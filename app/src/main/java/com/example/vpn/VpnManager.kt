package com.example.vpn

import android.content.Context
import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

object VpnManager {

    private const val TAG = "VpnManager"
    private val _state = MutableStateFlow(VpnConnectionState())
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var pingJob: Job? = null
    private var batchPingJob: Job? = null
    private var lazyPingJob: Job? = null
    private var ipCheckJob: Job? = null
    private var autoSyncLiveJob: Job? = null
    const val AUTO_SYNC_LIVE_INTERVAL_MS = 60L * 60L * 1000L
    private val proxyExecutor = Executors.newSingleThreadExecutor()
    private var appContext: Context? = null

    /** Bộ đếm đợt đo (Generation ID) ngăn đợt đo cũ ghi đè kết quả của đợt đo mới */
    private val measurementGeneration = java.util.concurrent.atomic.AtomicLong(1L)

    fun getCurrentMeasurementGeneration(): Long = measurementGeneration.get()

    fun incrementMeasurementGeneration(): Long = measurementGeneration.incrementAndGet()

    fun isDynamicLiveProxy(server: ServerLocation): Boolean {
        return server.id.startsWith("live_", ignoreCase = true)
    }

    /**
     * Chỉ ghi nhận kết quả đo nếu `generationId` khớp với đợt đo hiện hành (chống race condition từ tác vụ cũ).
     */
    @Synchronized
    fun recordMeasurementIfCurrentGeneration(
        generationId: Long,
        serverId: String,
        measurement: EndpointPingMeasurement
    ): Boolean {
        if (generationId != measurementGeneration.get()) {
            return false
        }
        val nextMeas = _state.value.serverMeasurements.toMutableMap()
        nextMeas[serverId] = measurement

        val nextPings = _state.value.serverPings.toMutableMap()
        // Chỉ ghi vào serverPings khi cổng chính phản hồi (MEASURED_PRIMARY) hoặc đánh dấu -1 khi thất bại;
        // tuyệt đối không lấy kết quả cổng phụ (:443/:80) giả làm ping cổng chính.
        when (measurement.status) {
            ServerPingStatus.MEASURED_PRIMARY -> nextPings[serverId] = measurement.rttMs
            ServerPingStatus.MEASURED_FALLBACK_ONLY, ServerPingStatus.UNREACHABLE -> nextPings[serverId] = -1
            ServerPingStatus.UNTESTED, ServerPingStatus.TESTING, ServerPingStatus.EXPIRED -> nextPings.remove(serverId)
        }

        _state.value = _state.value.copy(
            serverMeasurements = nextMeas,
            serverPings = nextPings
        )
        return true
    }

    const val IP_FOREGROUND_COOLDOWN_MS = 10_000L
    const val IP_NETWORK_CHANGE_DEBOUNCE_MS = 1_500L
    const val IP_PRE_LAUNCH_MAX_AGE_MS = 60_000L
    const val IP_PRE_LAUNCH_TIMEOUT_MS = 1_800L

    @Volatile
    private var lastRealGeoIp: GeoIpResult? = null

    @Volatile
    private var lastIpCheckTimestampMs: Long = 0L

    @Volatile
    private var isIpInvalidatedByNetworkChange: Boolean = false

    private var networkChangeMonitorJob: Job? = null
    private var networkChangeDebounceJob: Job? = null
    private var lastObservedTransportTag: String = ""
    private var lastObservedNetworkKey: String = ""
    private var lastObservedHasVpnTransport: Boolean? = null

    init {
        // Initial location check at launch
        checkCurrentIp(reason = "khởi động", force = true)
    }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        scope.launch(Dispatchers.IO) {
            GeoIpDetector.downloadDbIpLiteIfPermitted(appCtx, isCellularConfirmedByUser = false)
        }
        loadPersistedServers(appCtx)
        loadPersistedUnlockRecords(appCtx)
        lastObservedTransportTag = resolveCurrentNetworkTransportTag(appCtx)
        val initialNetStatus = com.example.cloudplay.NetworkOptimizer.status.value
        lastObservedNetworkKey = initialNetStatus.activeNetworkKey
        lastObservedHasVpnTransport = initialNetStatus.hasVpnTransport

        // Khi mở app lần đầu, khi vị trí GeoIP thay đổi hoặc khi kết quả tự chọn đã quá 6 giờ: đo probe ngắn và chọn theo điểm số
        scope.launch(Dispatchers.IO) {
            ensureInitialOrGeoUpdatedOptimalServer(context = appCtx, force = false)
            if (_state.value.serverPings.isEmpty() && _state.value.serverMeasurements.isEmpty()) {
                testAllServers(autoPickBest = false, context = appCtx, forceRefresh = false)
            }
        }
        autoSyncPublicProxiesIfNeeded(context = appCtx, force = false)

        // Theo dõi sự kiện đổi mạng mặc định hoặc bật/tắt cờ TRANSPORT_VPN từ NetworkOptimizer
        if (networkChangeMonitorJob == null) {
            networkChangeMonitorJob = scope.launch(Dispatchers.IO) {
                com.example.cloudplay.NetworkOptimizer.status.collect { netStatus ->
                    val newTag = when {
                        netStatus.isWifi -> "WIFI"
                        netStatus.isCellular -> "CELLULAR"
                        netStatus.physicalType == com.example.cloudplay.PhysicalNetworkType.ETHERNET -> "ETHERNET"
                        else -> ""
                    }
                    val newNetKey = netStatus.activeNetworkKey
                    val newHasVpn = netStatus.hasVpnTransport

                    val isInternalSmartBypassVpn = _state.value.engine == VpnEngine.SMART_BYPASS &&
                        (_state.value.mode == NetworkMode.VPN_JAPAN || _state.value.isConnecting)

                    val transportChanged = newTag.isNotBlank() &&
                        lastObservedTransportTag.isNotBlank() &&
                        newTag != lastObservedTransportTag
                    val networkKeyChanged = newNetKey.isNotBlank() &&
                        lastObservedNetworkKey.isNotBlank() &&
                        newNetKey != lastObservedNetworkKey
                    val vpnChanged = lastObservedHasVpnTransport != null &&
                        newHasVpn != lastObservedHasVpnTransport &&
                        !isInternalSmartBypassVpn

                    if (newTag.isNotBlank()) {
                        lastObservedTransportTag = newTag
                    }
                    if (newNetKey.isNotBlank()) {
                        lastObservedNetworkKey = newNetKey
                    }
                    lastObservedHasVpnTransport = newHasVpn

                    if (transportChanged || networkKeyChanged || vpnChanged) {
                        val reason = if (vpnChanged) "VPN bật-tắt" else "mạng đổi"
                        invalidateIpForNetworkChange(reason)
                        if (!isInGameStreamActive && !netStatus.isLowLatencyStreamActive) {
                            networkChangeDebounceJob?.cancel()
                            networkChangeDebounceJob = scope.launch(Dispatchers.IO) {
                                delay(IP_NETWORK_CHANGE_DEBOUNCE_MS)
                                if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
                                    return@launch
                                }
                                com.example.data.AppLogger.info(
                                    "VPN_NET",
                                    "Phát hiện thay đổi mạng ($reason: tag=$newTag, vpn=$newHasVpn) -> kiểm tra lại IP và đánh giá lại máy chủ tối ưu"
                                )
                                checkCurrentIp(reason = reason, force = true)
                                ensureInitialOrGeoUpdatedOptimalServer(context = appCtx, force = true)
                            }
                        }
                    }
                }
            }
        }
    }

    fun isWifiConnected(context: Context?): Boolean {
        val netStatus = com.example.cloudplay.NetworkOptimizer.status.value
        if (netStatus.isWifi) return true
        val ctx = context ?: appContext ?: return false
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    fun isCellularConnected(context: Context?): Boolean {
        val netStatus = com.example.cloudplay.NetworkOptimizer.status.value
        if (netStatus.isCellular) return true
        val ctx = context ?: appContext ?: return false
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveCurrentNetworkTransportTag(context: Context? = appContext): String {
        return when {
            isWifiConnected(context) -> "WIFI"
            isCellularConnected(context) -> "CELLULAR"
            else -> "UNKNOWN"
        }
    }

    /**
     * Tự động quét và kiểm tra kết nối các máy chủ từ danh sách công cộng (VPNGate, ProxyScrape):
     * - Khi chạy tự động (`force = false`): chỉ chạy khi đang kết nối Wi-Fi (hoặc khi người dùng bật cho phép dùng dữ liệu di động),
     *   với chu kỳ tối thiểu 60 phút (`AUTO_SYNC_LIVE_INTERVAL_MS`).
     * - Khi người dùng bấm nút cập nhật thủ công (`force = true`): luôn hoạt động mọi lúc.
     */
    fun autoSyncPublicProxiesIfNeeded(
        context: Context? = null,
        countryFilter: String = "ALL",
        force: Boolean = false
    ) {
        if (isInGameStreamActive) return
        val now = System.currentTimeMillis()
        val currentState = _state.value
        if (currentState.isAutoSyncingLiveProxies) return
        if (!force) {
            val allowCellular = try {
                com.example.data.SettingsRepository.config.value.allowCellularLiveProxySync
            } catch (_: Exception) {
                false
            }
            if (!isWifiConnected(context) && !allowCellular) {
                return
            }
            if (currentState.lastAutoSyncTimestampMs > 0L &&
                (now - currentState.lastAutoSyncTimestampMs) < AUTO_SYNC_LIVE_INTERVAL_MS
            ) {
                return
            }
        }

        autoSyncLiveJob?.cancel()
        _state.value = _state.value.copy(isAutoSyncingLiveProxies = true)

        autoSyncLiveJob = scope.launch(Dispatchers.IO) {
            try {
                val syncResult = LiveProxyFetcher.fetchAndCheckPublicProxies(countryFilter)
                val staticAndCustomKeys = (_state.value.customServers + DEFAULT_SERVERS)
                    .map { it.addressPortKey.lowercase() }
                    .toSet()
                val records = _state.value.unlockRecords
                val measurements = _state.value.serverMeasurements

                // Giữ lại các máy chủ công cộng cũ vẫn đang phản hồi cổng chính & hợp lệ
                val healthyExistingLive = _state.value.liveVerifiedServers.filter { srv ->
                    val unlockSt = getServerUnlockStatus(srv, records)
                    val meas = measurements[srv.id]
                    val isDead = meas?.status == ServerPingStatus.UNREACHABLE || meas?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY
                    unlockSt != RegionUnlockStatus.FAILED &&
                        unlockSt != RegionUnlockStatus.WRONG_REGION &&
                        !isDead
                }

                val mergedLive = (syncResult.verifiedServers + healthyExistingLive)
                    .filter { it.addressPortKey.lowercase() !in staticAndCustomKeys }
                    .distinctBy { it.addressPortKey.lowercase() }
                    .sortedWith(
                        compareByDescending<ServerLocation> { it.trustScore }
                            .thenBy { it.lastPingMs ?: Int.MAX_VALUE }
                    )

                if (mergedLive.isNotEmpty()) {
                    preCheckProxiesBatch(mergedLive)
                }

                val totalFilteredErrors = _state.value.autoFilteredOutErrorCount + syncResult.filteredOutErrorCount
                _state.value = _state.value.copy(
                    liveVerifiedServers = mergedLive,
                    isAutoSyncingLiveProxies = false,
                    lastAutoSyncTimestampMs = System.currentTimeMillis(),
                    autoFilteredOutErrorCount = totalFilteredErrors
                )
                persistLiveVerifiedServers(context ?: appContext, mergedLive)

                if (syncResult.verifiedServers.isNotEmpty()) {
                    com.example.data.AppLogger.success(
                        "VPN_AUTO",
                        "Đã lưu ${mergedLive.size} máy chủ công cộng đã kiểm tra kết nối (mới: +${syncResult.verifiedServers.size}, không phản hồi: ${syncResult.filteredOutErrorCount})"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "autoSyncPublicProxiesIfNeeded failed", e)
                _state.value = _state.value.copy(
                    isAutoSyncingLiveProxies = false,
                    lastAutoSyncTimestampMs = System.currentTimeMillis()
                )
            }
        }
    }

    private fun loadPersistedUnlockRecords(context: Context) {
        try {
            val sp = context.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val jsonStr = sp.getString("unlock_records_json", null) ?: return
            val array = org.json.JSONArray(jsonStr)
            val now = System.currentTimeMillis()
            val map = mutableMapOf<String, ServerUnlockRecord>()
            var hadExpired = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val key = obj.optString("serverKey", "")
                if (key.isBlank()) continue
                val statusStr = obj.optString("status", RegionUnlockStatus.UNTESTED.name)
                val status = try {
                    RegionUnlockStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    RegionUnlockStatus.UNTESTED
                }
                val ts = obj.optLong("timestampMs", now)
                val country = obj.optString("detectedCountryCode", "").ifBlank { null }
                val record = ServerUnlockRecord(
                    serverKey = key,
                    status = status,
                    timestampMs = ts,
                    detectedCountryCode = country
                )
                if (record.isExpired(now) ||
                    (status == RegionUnlockStatus.WRONG_REGION && (country == null || country.equals("ALL", ignoreCase = true) || country.equals("CUSTOM", ignoreCase = true)))
                ) {
                    hadExpired = true
                } else {
                    map[key] = record
                }
            }
            _state.value = _state.value.copy(unlockRecords = map)
            if (hadExpired) {
                persistUnlockRecords(context, map)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load persisted unlock records", e)
        }
    }

    private fun persistUnlockRecords(context: Context?, records: Map<String, ServerUnlockRecord>) {
        val ctx = context ?: appContext ?: return
        try {
            val sp = ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val array = org.json.JSONArray()
            val now = System.currentTimeMillis()
            for ((_, rec) in records) {
                if (rec.isExpired(now)) continue
                val obj = org.json.JSONObject().apply {
                    put("serverKey", rec.serverKey)
                    put("status", rec.status.name)
                    put("timestampMs", rec.timestampMs)
                    if (rec.detectedCountryCode != null) {
                        put("detectedCountryCode", rec.detectedCountryCode)
                    }
                }
                array.put(obj)
            }
            sp.edit().putString("unlock_records_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist unlock records", e)
        }
    }

    fun getServerUnlockStatus(
        server: ServerLocation,
        records: Map<String, ServerUnlockRecord> = _state.value.unlockRecords
    ): RegionUnlockStatus {
        val key = server.addressPortKey
        val rec = records[key]
        if (rec != null) {
            val eff = rec.effectiveStatus()
            if (eff != RegionUnlockStatus.UNTESTED) {
                return eff
            }
        }
        val code = server.countryCode.trim().uppercase()
        if (code.isNotBlank() && !GeoIpDetector.SUPPORTED_COUNTRIES.contains(code)) {
            return RegionUnlockStatus.WRONG_REGION
        }
        return RegionUnlockStatus.UNTESTED
    }

    private fun saveServerUnlockRecord(
        server: ServerLocation,
        status: RegionUnlockStatus,
        detectedCountryCode: String? = null
    ) {
        val key = server.addressPortKey
        val record = ServerUnlockRecord(
            serverKey = key,
            status = status,
            timestampMs = System.currentTimeMillis(),
            detectedCountryCode = detectedCountryCode
        )
        val updated = _state.value.unlockRecords.toMutableMap()
        updated[key] = record
        _state.value = _state.value.copy(unlockRecords = updated)
        persistUnlockRecords(appContext, updated)
    }

    /**
     * 1. KIỂM TRA TRƯỚC (siêu nhanh): khi chọn hoặc cập nhật proxy, kiểm tra ngay tính an toàn của IP
     * và tra cứu quốc gia qua bộ nhớ đệm / bảng nhị phân DB-IP (< 0.1ms) trước khi dùng truy vấn mạng nhẹ (≤ 1.2s).
     */
    fun preCheckProxyRegion(server: ServerLocation) {
        if (GeoIpDetector.isPrivateOrLoopbackIp(server.ip)) {
            saveServerUnlockRecord(server, RegionUnlockStatus.FAILED, server.countryCode)
            return
        }
        val declaredCode = server.countryCode.trim().uppercase()
        if (declaredCode.isNotBlank() && !GeoIpDetector.SUPPORTED_COUNTRIES.contains(declaredCode)) {
            saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, declaredCode)
            return
        }
        // Kiểm tra tức thì trong bảng nhị phân DB-IP / Seed Table (0ms, không tốn mạng)
        val fastLocalCode = GeoIpDetector.resolveIpCountryCode(server.ip, allowNetworkFallback = false)
        if (fastLocalCode.isNotBlank()) {
            if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(fastLocalCode)) {
                saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, fastLocalCode)
            } else if (getServerUnlockStatus(server) == RegionUnlockStatus.WRONG_REGION) {
                saveServerUnlockRecord(server, RegionUnlockStatus.UNTESTED, fastLocalCode)
            }
            return
        }
        if (isInGameStreamActive) return
        scope.launch(Dispatchers.IO) {
            try {
                val detectedCountry = queryIpCountryLight(server.ip)
                if (!detectedCountry.isNullOrBlank()) {
                    val cleanCode = detectedCountry.trim().uppercase()
                    GeoIpDetector.cacheResolvedIpCountry(server.ip, cleanCode)
                    if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(cleanCode)) {
                        saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, cleanCode)
                    } else {
                        val existing = getServerUnlockStatus(server)
                        if (existing == RegionUnlockStatus.WRONG_REGION) {
                            saveServerUnlockRecord(server, RegionUnlockStatus.UNTESTED, cleanCode)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun preCheckProxiesBatch(servers: List<ServerLocation>) {
        if (isInGameStreamActive || servers.isEmpty()) return
        val needsRemoteCheck = mutableListOf<ServerLocation>()
        servers.forEach { server ->
            if (GeoIpDetector.isPrivateOrLoopbackIp(server.ip)) {
                saveServerUnlockRecord(server, RegionUnlockStatus.FAILED, server.countryCode)
                return@forEach
            }
            val declaredCode = server.countryCode.trim().uppercase()
            if (declaredCode.isNotBlank() && !GeoIpDetector.SUPPORTED_COUNTRIES.contains(declaredCode)) {
                saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, declaredCode)
                return@forEach
            }
            val localCode = GeoIpDetector.resolveIpCountryCode(server.ip, allowNetworkFallback = false)
            if (localCode.isNotBlank()) {
                if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(localCode)) {
                    saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, localCode)
                }
            } else if (getServerUnlockStatus(server) == RegionUnlockStatus.UNTESTED) {
                needsRemoteCheck.add(server)
            }
        }
        if (needsRemoteCheck.isEmpty() || isInGameStreamActive) return
        scope.launch(Dispatchers.IO) {
            needsRemoteCheck.take(8).map { server ->
                async {
                    if (isInGameStreamActive) return@async
                    val detectedCountry = queryIpCountryLight(server.ip)
                    if (!detectedCountry.isNullOrBlank()) {
                        val cleanCode = detectedCountry.trim().uppercase()
                        GeoIpDetector.cacheResolvedIpCountry(server.ip, cleanCode)
                        if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(cleanCode)) {
                            saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, cleanCode)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun queryIpCountryLight(ip: String): String? {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank() || GeoIpDetector.isPrivateOrLoopbackIp(cleanIp)) return null
        val cachedOrLocal = GeoIpDetector.resolveIpCountryCode(cleanIp, allowNetworkFallback = false)
        if (cachedOrLocal.isNotBlank()) return cachedOrLocal
        return try {
            val url = java.net.URL("https://api.country.is/$cleanIp")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 1200
                readTimeout = 1200
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = org.json.JSONObject(body)
                json.optString("country", "").ifBlank { null }?.also { cc ->
                    GeoIpDetector.cacheResolvedIpCountry(cleanIp, cc)
                }
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 2. HỌC TỪ THỰC TẾ: nhận tín hiệu UNLOCK_OK hoặc UNLOCK_FAIL từ XboxBridge
     * Lưu kết quả theo từng máy chủ (khóa theo địa chỉ + cổng), kèm thời điểm.
     */
    fun onUnlockSignalReceived(signal: String) {
        val currentServer = _state.value.currentServer
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        when (signal.trim().uppercase()) {
            "UNLOCK_OK" -> {
                com.example.data.AppLogger.success("UNLOCK", "Mở khóa vùng thành công: ${currentServer.name} (${currentServer.countryCode})")
                saveServerUnlockRecord(currentServer, RegionUnlockStatus.UNLOCKED, currentServer.countryCode)
                _state.value = _state.value.copy(
                    unlockFailPromptServer = null,
                    isBypassActiveInBrowser = true,
                    statusMessage = if (isEn) {
                        "✅ Region unlocked successfully: ${currentServer.name} ${currentServer.flag}"
                    } else {
                        "✅ Đã mở khóa vùng thành công: ${currentServer.name} ${currentServer.flag}"
                    }
                )
            }
            "UNLOCK_FAIL" -> {
                com.example.data.AppLogger.warn("UNLOCK", "Máy chủ không mở khóa được vùng: ${currentServer.name} (${currentServer.ip}:${currentServer.port})")
                saveServerUnlockRecord(currentServer, RegionUnlockStatus.FAILED, currentServer.countryCode)
                val prunedLive = _state.value.liveVerifiedServers.filterNot {
                    it.addressPortKey.equals(currentServer.addressPortKey, ignoreCase = true)
                }
                if (prunedLive.size != _state.value.liveVerifiedServers.size) {
                    persistLiveVerifiedServers(appContext, prunedLive)
                }
                _state.value = _state.value.copy(
                    liveVerifiedServers = prunedLive,
                    unlockFailPromptServer = currentServer,
                    statusMessage = if (isEn) {
                        "⚠️ This server cannot unlock the region, switch to another server?"
                    } else {
                        "⚠️ Máy chủ này không mở khóa được, đổi sang máy chủ khác?"
                    }
                )
            }
        }
    }

    fun dismissUnlockFailPrompt() {
        _state.value = _state.value.copy(unlockFailPromptServer = null)
    }

    // ==================== HẰNG SỐ CHẤM ĐIỂM & CHỌN MÁY CHỦ (MỤC 4, 5, 6) ====================
    /** Khi bật Tự động tắt VPN (`autoBypassEnabled = true`): RTT lịch sử phiên chơi trực tiếp chiếm 75% */
    const val WEIGHT_SESSION_RTT_AUTO_BYPASS = 0.75
    /** Khi bật Tự động tắt VPN (`autoBypassEnabled = true`): RTT mở khóa qua VPN chiếm 25% (và là thành phần chính 100% khi vùng chưa có lịch sử) */
    const val WEIGHT_UNLOCK_RTT_AUTO_BYPASS = 0.25
    /** Khi giữ kết nối VPN suốt lúc chơi (`autoBypassEnabled = false`): RTT qua VPN chiếm 75% */
    const val WEIGHT_VPN_RTT_KEEP_TUNNEL = 0.75
    /** Khi giữ kết nối VPN suốt lúc chơi (`autoBypassEnabled = false`): RTT lịch sử phiên chơi chiếm 25% */
    const val WEIGHT_SESSION_RTT_KEEP_TUNNEL = 0.25

    /** Điểm phạt (tương đương ms) cho mỗi 1 lần đứng hình/phút (`freezeCount / durationMinutes`) */
    const val PENALTY_PER_FREEZE_PER_MIN = 35.0
    /** Điểm phạt (tương đương ms) cho mỗi 1% khung hình bị rớt (`(framesDropped / framesDecoded) * 100`) */
    const val PENALTY_PER_PERCENT_FRAME_DROP = 8.0
    /** Điểm phạt nặng khi máy chủ bị `RegionUnlockStatus.FAILED` hoặc `RegionUnlockStatus.WRONG_REGION` */
    const val PENALTY_UNLOCK_FAILED_OR_WRONG_REGION = 10000.0
    /** Điểm phạt nặng khi máy chủ không phản hồi (`ServerPingStatus.UNREACHABLE`) */
    const val PENALTY_PING_UNREACHABLE = 15000.0
    /** Điểm phạt khi máy chủ chỉ mở cổng phụ (:443/:80) mà đóng cổng proxy chính (`ServerPingStatus.MEASURED_FALLBACK_ONLY`) */
    const val PENALTY_PING_FALLBACK_PORT_ONLY = 1500.0
    /** Điểm phạt khi độ trễ cổng chính dao động mạnh (`measurement.isUnstable == true`) */
    const val PENALTY_PING_UNSTABLE = 45.0
    /** Điểm phạt nhẹ khi máy chủ chưa xác minh mở khóa (`RegionUnlockStatus.UNTESTED`) so với `RegionUnlockStatus.UNLOCKED` */
    const val PENALTY_UNLOCK_UNTESTED = 12.0

    /** Hệ số ước tính ban đầu theo khoảng cách địa lý (ms/km) — CHỈ dùng khi chưa có cả RTT đo thật lẫn lịch sử phiên chơi */
    const val DISTANCE_INITIAL_ESTIMATE_MS_PER_KM = 0.022
    /** Hệ số phân định cực nhỏ theo khoảng cách địa lý khi hai máy chủ có điểm số ngang nhau */
    const val DISTANCE_TIE_BREAKER_PER_KM = 0.001

    /** Ngưỡng chống nhảy máy chủ (Hysteresis): ứng viên mới phải cải thiện điểm số tối thiểu 15% */
    const val HYSTERESIS_MIN_IMPROVEMENT_RATIO = 0.15
    /** Ngưỡng chống nhảy máy chủ (Hysteresis): ứng viên mới phải cải thiện tối thiểu 10ms (điểm tương đương ms) */
    const val HYSTERESIS_MIN_DELTA_SCORE_MS = 10.0
    /** Thời hạn lưu kết quả tự chọn máy chủ: 6 giờ */
    const val AUTO_SELECT_CACHE_TTL_MS = 6L * 60L * 60L * 1000L

    private const val PREF_USER_MANUALLY_SELECTED = "user_manually_selected_server"
    private const val PREF_LAST_AUTO_SELECT_TS = "last_auto_select_timestamp_ms"
    private const val PREF_LAST_AUTO_SELECT_NET_TYPE = "last_auto_select_network_type"
    private const val PREF_LAST_AUTO_SELECT_COUNTRY = "last_auto_select_user_country"

    fun getLastSessionPingForServer(
        server: ServerLocation,
        sessions: List<com.example.data.PlaySessionRecord> = com.example.data.SessionHistoryRepository.sessions.value
    ): Int {
        val sameCountryNames = getAllServers()
            .filter { it.countryCode.equals(server.countryCode, ignoreCase = true) }
            .map { it.name }
            .toSet()
        return com.example.data.SessionHistoryRepository.getAverageRttForServerOrRegion(
            serverName = server.name,
            countryCode = server.countryCode,
            sameRegionServerNames = sameCountryNames,
            sessionsList = sessions
        )
    }

    fun getEffectiveServerPing(
        server: ServerLocation,
        engine: VpnEngine = _state.value.engine,
        pingMap: Map<String, Int> = _state.value.serverPings,
        sessions: List<com.example.data.PlaySessionRecord> = com.example.data.SessionHistoryRepository.sessions.value
    ): Int {
        val measured = pingMap[server.id]
        if (measured != null) {
            return measured
        }
        return if (engine == VpnEngine.SMART_BYPASS) {
            getLastSessionPingForServer(server, sessions)
        } else {
            -1
        }
    }

    /**
     * Hàm tính điểm số duy nhất cho máy chủ/vùng (Mục 4):
     * - Điểm càng THẤP càng TỐT (đơn vị tương đương ms).
     * - Thành phần chính là RTT đo thật (trung vị của các mẫu đo từ `EndpointPingMeasurement`) kết hợp với RTT lịch sử phiên chơi:
     *   + Khi bật Tự động tắt VPN (`autoBypassEnabled = true`): 75% RTT lịch sử phiên chơi trực tiếp + 25% RTT mở khóa qua VPN;
     *     nếu vùng chưa có lịch sử phiên chơi thì RTT mở khóa qua VPN là thành phần chính (100%).
     *   + Khi giữ VPN suốt lúc chơi (`autoBypassEnabled = false`): 75% RTT qua VPN + 25% RTT lịch sử phiên chơi (hoặc 100% RTT qua VPN nếu chưa có lịch sử).
     * - Cộng điểm phạt từ lịch sử phiên chơi gần đây (đã tính trọng số thời gian `RECENCY_DECAY_FACTOR = 0.75`):
     *   + Số lần đứng hình/phút (`weightedFreezePerMin * PENALTY_PER_FREEZE_PER_MIN`).
     *   + Tỉ lệ phần trăm rớt khung hình (`weightedFrameDropPercent * PENALTY_PER_PERCENT_FRAME_DROP`).
     * - Cộng điểm phạt trạng thái mở khóa (`RegionUnlockStatus`) và trạng thái cổng (`ServerPingStatus`).
     * - Khoảng cách địa lý CHỈ dùng khi chưa có bất kỳ số đo/lịch sử nào, hoặc làm hệ số phân định cực nhỏ khi điểm số bằng nhau.
     * - Tuyệt đối KHÔNG sử dụng `server.speedMbps`.
     */
    internal fun calculateServerSelectionScore(
        server: ServerLocation,
        measurement: EndpointPingMeasurement?,
        fallbackPingMs: Int?,
        unlockStatus: RegionUnlockStatus,
        sessionStats: com.example.data.SessionHistoryRepository.RegionSessionStats?,
        autoBypassEnabled: Boolean,
        distanceKm: Int?
    ): Double {
        val measuredRttMs = when {
            measurement != null &&
                (measurement.status == ServerPingStatus.MEASURED_PRIMARY ||
                    measurement.status == ServerPingStatus.MEASURED_FALLBACK_ONLY) &&
                measurement.rttMs > 0 -> measurement.rttMs
            fallbackPingMs != null && fallbackPingMs > 0 -> fallbackPingMs
            else -> -1
        }
        val sessionRttMs = if (sessionStats != null && sessionStats.sampleCount > 0 && sessionStats.avgRttMs > 0) {
            sessionStats.avgRttMs
        } else {
            -1
        }

        val hasMeasuredRtt = measuredRttMs > 0
        val hasSessionRtt = sessionRttMs > 0

        val baseRttScore = when {
            hasMeasuredRtt && hasSessionRtt -> {
                if (autoBypassEnabled) {
                    (WEIGHT_SESSION_RTT_AUTO_BYPASS * sessionRttMs.toDouble()) +
                        (WEIGHT_UNLOCK_RTT_AUTO_BYPASS * measuredRttMs.toDouble())
                } else {
                    (WEIGHT_VPN_RTT_KEEP_TUNNEL * measuredRttMs.toDouble()) +
                        (WEIGHT_SESSION_RTT_KEEP_TUNNEL * sessionRttMs.toDouble())
                }
            }
            hasMeasuredRtt -> measuredRttMs.toDouble()
            hasSessionRtt -> sessionRttMs.toDouble()
            else -> {
                // Chưa có cả số đo RTT lẫn lịch sử phiên chơi -> dùng khoảng cách địa lý làm ước tính ban đầu
                val safeDistKm = (distanceKm ?: 4000).coerceAtLeast(100)
                500.0 + (safeDistKm.toDouble() * DISTANCE_INITIAL_ESTIMATE_MS_PER_KM)
            }
        }

        val freezePenalty = if (sessionStats != null && sessionStats.sampleCount > 0) {
            sessionStats.weightedFreezePerMin.coerceAtLeast(0.0) * PENALTY_PER_FREEZE_PER_MIN
        } else {
            0.0
        }

        val frameDropPenalty = if (sessionStats != null && sessionStats.sampleCount > 0) {
            sessionStats.weightedFrameDropPercent.coerceAtLeast(0.0) * PENALTY_PER_PERCENT_FRAME_DROP
        } else {
            0.0
        }

        val unlockPenalty = when (unlockStatus) {
            RegionUnlockStatus.UNLOCKED -> 0.0
            RegionUnlockStatus.UNTESTED -> PENALTY_UNLOCK_UNTESTED
            RegionUnlockStatus.FAILED, RegionUnlockStatus.WRONG_REGION -> PENALTY_UNLOCK_FAILED_OR_WRONG_REGION
        }

        val isExplicitlyUnreachable = measurement?.status == ServerPingStatus.UNREACHABLE ||
            (measurement == null && fallbackPingMs != null && fallbackPingMs < 0)
        val reachabilityPenalty = when {
            isExplicitlyUnreachable -> PENALTY_PING_UNREACHABLE + (if (isDynamicLiveProxy(server)) 2000.0 else 0.0)
            measurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY -> PENALTY_PING_FALLBACK_PORT_ONLY
            measurement?.isUnstable == true -> PENALTY_PING_UNSTABLE
            else -> 0.0
        }

        val tieBreaker = ((distanceKm ?: 4000).coerceAtLeast(0).toDouble() * DISTANCE_TIE_BREAKER_PER_KM) +
            (if (isDynamicLiveProxy(server)) 0.5 else 0.0)

        return baseRttScore + freezePenalty + frameDropPenalty + unlockPenalty + reachabilityPenalty + tieBreaker
    }

    /**
     * Kiểm tra điều kiện tự động chuyển sang máy chủ ứng viên mới (Mục 5):
     * - Bảo vệ lựa chọn thủ công (`userManuallySelected == true`): không bao giờ tự động ghi đè trừ khi người dùng bấm "Chọn tối ưu" (`forcePickBest == true`)
     *   hoặc máy chủ đang chọn bị lỗi mở khóa (`FAILED` / `WRONG_REGION`) hay chết kết nối (`UNREACHABLE`).
     * - Ngưỡng chống nhảy máy chủ (Hysteresis): chỉ tự đổi khi ứng viên mới có điểm số tốt hơn ít nhất 15% VÀ cải thiện ít nhất 10ms.
     */
    internal fun shouldAutoSwitchServer(
        currentServer: ServerLocation,
        candidateServer: ServerLocation,
        currentScore: Double,
        candidateScore: Double,
        currentMeasurement: EndpointPingMeasurement?,
        currentUnlockStatus: RegionUnlockStatus,
        userManuallySelected: Boolean,
        forcePickBest: Boolean
    ): Boolean {
        if (currentServer.id == candidateServer.id) return false
        if (forcePickBest) return true

        val isCurrentFailedOrUnreachable =
            currentUnlockStatus == RegionUnlockStatus.FAILED ||
                currentUnlockStatus == RegionUnlockStatus.WRONG_REGION ||
                currentMeasurement?.status == ServerPingStatus.UNREACHABLE ||
                currentMeasurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY

        if (userManuallySelected && !isCurrentFailedOrUnreachable) {
            return false
        }

        if (isCurrentFailedOrUnreachable) {
            return candidateScore < currentScore
        }

        if (currentScore <= 0.0 || candidateScore >= currentScore) return false
        val deltaScoreMs = currentScore - candidateScore
        val improvementRatio = deltaScoreMs / currentScore
        return deltaScoreMs >= HYSTERESIS_MIN_DELTA_SCORE_MS &&
            improvementRatio >= HYSTERESIS_MIN_IMPROVEMENT_RATIO
    }

    /**
     * Tạo chuỗi giải thích lý do chọn máy chủ/vùng hoàn toàn từ số liệu thật (Mục 7):
     * Ví dụ: "Gần: ~3.000 km • đo 76ms • 5 phiên gần nhất" (hoặc bỏ bớt phần chưa có dữ liệu).
     */
    fun buildServerSelectionReasonText(
        server: ServerLocation = _state.value.currentServer,
        isEn: Boolean = com.example.data.SettingsRepository.config.value.isEnglishUI(),
        geo: GeoIpResult? = _state.value.detectedGeoIp,
        measurementsMap: Map<String, EndpointPingMeasurement> = _state.value.serverMeasurements,
        pingMap: Map<String, Int> = _state.value.serverPings,
        sessions: List<com.example.data.PlaySessionRecord> = com.example.data.SessionHistoryRepository.sessions.value
    ): String {
        val parts = mutableListOf<String>()

        val distKm = RegionalProbeTester.getServerDistanceKm(
            server = server,
            geo = geo,
            allowTimezoneFallback = true
        )
        if (distKm != null && distKm > 0) {
            val roundedKm = ((distKm + 50) / 100) * 100
            val formattedKm = if (roundedKm >= 1000) {
                val thousands = roundedKm / 1000
                val hundreds = roundedKm % 1000
                val sep = if (isEn) "," else "."
                String.format(java.util.Locale.US, "%d%s%03d", thousands, sep, hundreds)
            } else {
                roundedKm.toString()
            }
            parts.add(if (isEn) "Near: ~${formattedKm} km" else "Gần: ~${formattedKm} km")
        }

        val meas = measurementsMap[server.id]
        val measuredPing = when {
            meas != null && meas.isPrimaryPortAlive -> meas.rttMs
            else -> pingMap[server.id] ?: -1
        }
        if (measuredPing > 0) {
            parts.add(if (isEn) "measured ${measuredPing}ms" else "đo ${measuredPing}ms")
        }

        val sameCountryNames = getAllServers()
            .filter { it.countryCode.equals(server.countryCode, ignoreCase = true) }
            .map { it.name }
            .toSet()
        val sessionStats = com.example.data.SessionHistoryRepository.getRegionSessionStats(
            serverName = server.name,
            countryCode = server.countryCode,
            sameCountryServerNames = sameCountryNames,
            maxRecentSessions = 5,
            sessionsList = sessions
        )
        if (sessionStats != null && sessionStats.sampleCount > 0) {
            val sessionPart = if (measuredPing <= 0) {
                if (isEn) {
                    "~${sessionStats.avgRttMs}ms • last ${sessionStats.sampleCount} sessions"
                } else {
                    "~${sessionStats.avgRttMs}ms • ${sessionStats.sampleCount} phiên gần nhất"
                }
            } else {
                if (isEn) {
                    "last ${sessionStats.sampleCount} sessions (~${sessionStats.avgRttMs}ms)"
                } else {
                    "${sessionStats.sampleCount} phiên gần nhất (~${sessionStats.avgRttMs}ms)"
                }
            }
            parts.add(sessionPart)
        }

        if (parts.isEmpty()) {
            return if (isEn) "No data yet" else "Chưa có dữ liệu"
        }
        return parts.joinToString(" • ")
    }

    /**
     * Lọc danh sách máy chủ nằm trong giới hạn khoảng cách tự chọn (<= 5.500 km và chênh lệch <= 2.500 km so với vùng gần nhất).
     * Nếu tất cả máy chủ đều vượt giới hạn (ví dụ danh sách chỉ chứa máy chủ của vùng xa do người dùng lọc thủ công), giữ nguyên danh sách.
     */
    fun filterServersWithinDistanceLimit(
        servers: List<ServerLocation>,
        geo: GeoIpResult? = _state.value.detectedGeoIp,
        allowTimezoneFallback: Boolean = true
    ): List<ServerLocation> {
        val withinLimit = servers.filter { srv ->
            RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                server = srv,
                servers = servers,
                geo = geo,
                allowTimezoneFallback = allowTimezoneFallback
            )
        }
        return if (withinLimit.isNotEmpty()) withinLimit else servers
    }

    /**
     * Sắp xếp danh sách máy chủ dựa trên hàm tính điểm thống nhất `calculateServerSelectionScore` (Mục 4):
     * - Kết hợp RTT đo thật, lịch sử phiên chơi (RTT, freezeCount/phút, tỉ lệ framesDropped/framesDecoded có trọng số thời gian),
     *   và trạng thái mở khóa vùng.
     * - Khoảng cách địa lý chỉ đóng vai trò lọc vùng quá xa (> 5.500 km) và làm giá trị ước tính ban đầu khi chưa có số đo.
     */
    fun sortServersByUnlockAndPing(
        servers: List<ServerLocation>,
        pingMap: Map<String, Int> = _state.value.serverPings,
        records: Map<String, ServerUnlockRecord> = _state.value.unlockRecords,
        engine: VpnEngine = _state.value.engine,
        sessions: List<com.example.data.PlaySessionRecord> = com.example.data.SessionHistoryRepository.sessions.value,
        measurementsMap: Map<String, EndpointPingMeasurement> = if (pingMap === _state.value.serverPings) _state.value.serverMeasurements else emptyMap(),
        prioritizeDistanceForAutoBypass: Boolean = com.example.data.SettingsRepository.config.value.autoBypassEnabled,
        geo: GeoIpResult? = _state.value.detectedGeoIp
    ): List<ServerLocation> {
        if (servers.size <= 1) return servers
        val currentGeo = geo
        val namesByCountry = servers
            .groupBy { it.countryCode.trim().uppercase() }
            .mapValues { (_, list) -> list.map { it.name }.toSet() }

        val regionStatsByServerId = servers.associate { srv ->
            val cc = srv.countryCode.trim().uppercase()
            val stats = com.example.data.SessionHistoryRepository.getRegionSessionStats(
                serverName = srv.name,
                countryCode = cc,
                sameCountryServerNames = namesByCountry[cc] ?: emptySet(),
                maxRecentSessions = 5,
                sessionsList = sessions
            )
            srv.id to stats
        }

        val scoreByServerId = servers.associate { srv ->
            val unlockStatus = getServerUnlockStatus(srv, records)
            val meas = measurementsMap[srv.id]
            val fallbackPing = pingMap[srv.id]
            val distKm = RegionalProbeTester.getServerDistanceKm(srv, currentGeo, allowTimezoneFallback = false)
                ?: RegionalProbeTester.getServerDistanceKm(srv, currentGeo, allowTimezoneFallback = true)
            val score = calculateServerSelectionScore(
                server = srv,
                measurement = meas,
                fallbackPingMs = fallbackPing,
                unlockStatus = unlockStatus,
                sessionStats = regionStatsByServerId[srv.id],
                autoBypassEnabled = prioritizeDistanceForAutoBypass,
                distanceKm = distKm
            )
            srv.id to score
        }

        return servers.sortedWith(
            Comparator { a, b ->
                val statusA = getServerUnlockStatus(a, records)
                val statusB = getServerUnlockStatus(b, records)
                val tierA = unlockEligibilityTier(statusA)
                val tierB = unlockEligibilityTier(statusB)
                if (tierA != tierB) {
                    return@Comparator tierA.compareTo(tierB)
                }

                // Không để máy chủ quá xa (> 5.500 km như Mỹ từ Châu Á) vượt lên trên máy chủ trong phạm vi hợp lệ (KR/JP)
                val withinDistA = RegionalProbeTester.isWithinAutoSelectDistanceLimit(a, servers, currentGeo, allowTimezoneFallback = false)
                val withinDistB = RegionalProbeTester.isWithinAutoSelectDistanceLimit(b, servers, currentGeo, allowTimezoneFallback = false)
                if (withinDistA != withinDistB) {
                    return@Comparator if (withinDistA) -1 else 1
                }

                val scoreA = scoreByServerId[a.id] ?: Double.MAX_VALUE
                val scoreB = scoreByServerId[b.id] ?: Double.MAX_VALUE
                val scoreCmp = scoreA.compareTo(scoreB)
                if (scoreCmp != 0) {
                    return@Comparator scoreCmp
                }

                val exactDistRankA = RegionalProbeTester.getExactCountryDistanceRank(a.countryCode, servers, currentGeo, allowTimezoneFallback = false)
                val exactDistRankB = RegionalProbeTester.getExactCountryDistanceRank(b.countryCode, servers, currentGeo, allowTimezoneFallback = false)
                if (exactDistRankA != exactDistRankB) {
                    return@Comparator exactDistRankA.compareTo(exactDistRankB)
                }

                val countryTierA = RegionalProbeTester.getNearestSupportedCountryTier(a.countryCode, servers, currentGeo)
                val countryTierB = RegionalProbeTester.getNearestSupportedCountryTier(b.countryCode, servers, currentGeo)
                countryTierA.compareTo(countryTierB)
            }
        )
    }

    private fun unlockEligibilityTier(status: RegionUnlockStatus): Int {
        return when (status) {
            RegionUnlockStatus.UNLOCKED, RegionUnlockStatus.UNTESTED -> 0
            RegionUnlockStatus.FAILED -> 1
            RegionUnlockStatus.WRONG_REGION -> 2
        }
    }

    private fun unlockSortRank(status: RegionUnlockStatus): Int {
        return when (status) {
            RegionUnlockStatus.UNLOCKED -> 0
            RegionUnlockStatus.UNTESTED -> 1
            RegionUnlockStatus.FAILED -> 2
            RegionUnlockStatus.WRONG_REGION -> 3
        }
    }

    /**
     * Chọn tập máy chủ cho Bước 1 để thăm dò sơ bộ, ưu tiên các quốc gia gần người dùng nhất trước.
     * Trên mạng di động (`maxPerCountry = 1`, `maxCountries = 2`), chỉ lấy ít đại diện để tiết kiệm dữ liệu và pin.
     */
    fun selectStep1RepresentativeServers(
        allServers: List<ServerLocation>,
        records: Map<String, ServerUnlockRecord> = _state.value.unlockRecords,
        maxPerCountry: Int = 3,
        maxCountries: Int = Int.MAX_VALUE
    ): List<ServerLocation> {
        val valid = allServers.filter {
            val st = getServerUnlockStatus(it, records)
            st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
        }
        val orderedCountries = RegionalProbeTester.getNearestSupportedCountriesForUser(
            servers = valid.ifEmpty { allServers },
            geo = _state.value.detectedGeoIp
        ).ifEmpty { listOf("KR", "JP", "US") }

        val byCountry = valid.groupBy { it.countryCode.trim().uppercase() }
        val orderedKeys = (orderedCountries + byCountry.keys).distinct().take(maxCountries)
        return orderedKeys.flatMap { code ->
            val listInCountry = byCountry[code].orEmpty()
            listInCountry
                .sortedWith(
                    compareBy<ServerLocation> { unlockSortRank(getServerUnlockStatus(it, records)) }
                        .thenBy { isDynamicLiveProxy(it) }
                )
                .take(maxPerCountry)
        }
    }

    /**
     * Đo lười (Lazy Viewport Probing — A8):
     * Chỉ đo các máy chủ đang hiển thị trên màn hình (hoặc được truyền vào) mà chưa có kết quả hợp lệ trong cache 5 phút.
     * Các máy chủ ngoài danh sách hiển thị giữ nguyên trạng thái "Chưa đo" (UNTESTED).
     */
    fun probeVisibleServersLazily(
        visibleServers: List<ServerLocation>,
        customProbeExecutor: (suspend (String, Int, Int) -> Pair<Int, String>)? = null
    ) {
        if (visibleServers.isEmpty() || (isInGameStreamActive && customProbeExecutor == null)) return
        val currentGen = measurementGeneration.get()
        val now = System.currentTimeMillis()
        val currentMeas = _state.value.serverMeasurements.toMutableMap()
        val currentPings = _state.value.serverPings.toMutableMap()
        val needNetworkProbe = mutableListOf<ServerLocation>()

        for (srv in visibleServers) {
            val existing = currentMeas[srv.id]
            if (existing != null && !existing.isExpired(now) &&
                existing.status != ServerPingStatus.UNTESTED &&
                existing.status != ServerPingStatus.EXPIRED
            ) {
                continue
            }
            val cached = PingTester.getCachedMeasurement(srv, now)
            if (cached != null && cached.status != ServerPingStatus.EXPIRED) {
                currentMeas[srv.id] = cached
                if (cached.isPrimaryPortAlive) {
                    currentPings[srv.id] = cached.rttMs
                } else if (cached.status == ServerPingStatus.UNREACHABLE || cached.isFallbackPortOnly) {
                    currentPings[srv.id] = -1
                }
            } else {
                needNetworkProbe.add(srv)
            }
        }

        _state.value = _state.value.copy(
            serverMeasurements = currentMeas,
            serverPings = currentPings
        )

        if (needNetworkProbe.isEmpty() || _state.value.isTestingPings) return

        lazyPingJob?.cancel()
        lazyPingJob = scope.launch(Dispatchers.IO) {
            val batch = needNetworkProbe.take(8)
            // Đánh dấu trạng thái TESTING cho các mục đang hiển thị sắp đo
            synchronized(this@VpnManager) {
                if (currentGen == measurementGeneration.get()) {
                    val markTesting = _state.value.serverMeasurements.toMutableMap()
                    for (srv in batch) {
                        if (markTesting[srv.id] == null || markTesting[srv.id]?.status == ServerPingStatus.UNTESTED) {
                            markTesting[srv.id] = EndpointPingMeasurement(
                                rttMs = -1,
                                configuredPort = srv.port,
                                measuredPort = srv.port,
                                resolvedIp = srv.ip,
                                measurementType = PingMeasurementType.NONE,
                                status = ServerPingStatus.TESTING
                            )
                        }
                    }
                    _state.value = _state.value.copy(serverMeasurements = markTesting)
                }
            }

            // Thời hạn cho một bước đo lười <= 5 giây (giới hạn timeout socket được quản lý bên trong Semaphore của PingTester)
            kotlinx.coroutines.withTimeoutOrNull(5000L) {
                val jobs = batch.map { server ->
                    async {
                        val meas = PingTester.measureServerEndpoint(
                            server = server,
                            timeoutMs = 850,
                            useCache = true,
                            customProbeExecutor = customProbeExecutor
                        )
                        recordMeasurementIfCurrentGeneration(currentGen, server.id, meas)
                    }
                }
                jobs.awaitAll()
            }

            // Bất kỳ mục nào trong batch hết hạn 5s mà chưa tới lượt lấy giấy phép Semaphore (vẫn TESTING) -> trả về trạng thái Chưa đo (UNTESTED) để đo lại khi cuộn
            synchronized(this@VpnManager) {
                if (currentGen == measurementGeneration.get()) {
                    val cleanup = _state.value.serverMeasurements.toMutableMap()
                    for (srv in batch) {
                        if (cleanup[srv.id]?.status == ServerPingStatus.TESTING) {
                            cleanup.remove(srv.id)
                        }
                    }
                    _state.value = _state.value.copy(serverMeasurements = cleanup)
                }
            }
        }
    }

    /**
     * Đo ping theo 2 bước chịu tải danh sách lớn (A8):
     * - Bước 1 (thời hạn <= 5 giây): Đo 2–3 máy chủ đại diện mỗi vùng/quốc gia để tạo thứ tự ưu tiên sơ bộ.
     * - Bước 2 (thời hạn <= 5 giây): Đo các ứng viên tốt từ các vùng phản hồi + máy chủ đang chọn + đầu bảng.
     * - Các máy chủ còn lại trong danh sách lớn giữ nguyên trạng thái "Chưa đo" (UNTESTED) và được đo lười khi người dùng cuộn tới.
     * - Khi bấm "Đo ping" (`forceRefresh = true`), tăng `measurementGeneration`, xóa cache cần làm mới và chặn tác vụ cũ ghi đè.
     */
    fun testAllServers(
        autoPickBest: Boolean = false,
        context: Context? = null,
        forceRefresh: Boolean = true,
        visibleServerIds: Set<String> = emptySet(),
        onPickedBest: ((ServerLocation, Int) -> Unit)? = null
    ) {
        if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) return
        batchPingJob?.cancel()
        lazyPingJob?.cancel()

        if (autoPickBest) {
            setManualSelectionFlag(context ?: appContext, false)
        }

        val myGeneration = if (forceRefresh) {
            PingTester.clearCache()
            incrementMeasurementGeneration()
        } else {
            measurementGeneration.get()
        }

        _state.value = _state.value.copy(
            isTestingPings = true,
            serverPings = if (forceRefresh) emptyMap() else _state.value.serverPings,
            serverMeasurements = if (forceRefresh) emptyMap() else _state.value.serverMeasurements
        )

        batchPingJob = scope.launch(Dispatchers.IO) {
            try {
                val allServers = getAllServers()
                val currentEngine = _state.value.engine
                val records = _state.value.unlockRecords
                val currentSrv = _state.value.currentServer
                val isCellularLightMode = isCellularConnected(context) && !forceRefresh && !autoPickBest

                // BƯỚC 1 (Thời hạn <= 5 giây): Đo đại diện mỗi vùng/quốc gia (trên mạng di động chỉ đo 1 đại diện cho 2 nước gần nhất)
                val step1Reps = selectStep1RepresentativeServers(
                    allServers = allServers,
                    records = records,
                    maxPerCountry = if (isCellularLightMode) 1 else 3,
                    maxCountries = if (isCellularLightMode) 2 else Int.MAX_VALUE
                )
                val step1Targets = (step1Reps + currentSrv).distinctBy { it.id }

                com.example.data.AppLogger.info(
                    "PING",
                    "Đợt đo #$myGeneration • Bước 1: Thăm dò ${step1Targets.size}/${allServers.size} đại diện vùng (thời hạn ≤5s, tối đa ${PingTester.MAX_CONCURRENT_SOCKETS} socket mở đồng thời)"
                )

                runMeasurementStepWithDeadline(
                    generationId = myGeneration,
                    targets = step1Targets,
                    stepDeadlineMs = if (isCellularLightMode) 3000L else 5000L,
                    useCache = !forceRefresh
                )

                if (myGeneration != measurementGeneration.get() || isInGameStreamActive) return@launch

                // Đánh giá sơ bộ các vùng có phản hồi từ Bước 1
                val step1Meas = _state.value.serverMeasurements
                val countryBestRtt = allServers
                    .map { it.countryCode.trim().uppercase() }
                    .distinct()
                    .associateWith { code ->
                        step1Targets
                            .filter { it.countryCode.equals(code, ignoreCase = true) }
                            .mapNotNull { srv -> step1Meas[srv.id]?.takeIf { it.isReachableAnyPort }?.rttMs }
                            .minOrNull() ?: Int.MAX_VALUE
                    }

                val orderedCountries = countryBestRtt.entries
                    .sortedWith(
                        Comparator { a, b ->
                            val withinA = RegionalProbeTester.isCountryWithinAutoSelectDistanceLimit(
                                a.key,
                                allServers,
                                _state.value.detectedGeoIp,
                                allowTimezoneFallback = true
                            )
                            val withinB = RegionalProbeTester.isCountryWithinAutoSelectDistanceLimit(
                                b.key,
                                allServers,
                                _state.value.detectedGeoIp,
                                allowTimezoneFallback = true
                            )
                            if (withinA != withinB) {
                                if (withinA) -1 else 1
                            } else {
                                a.value.compareTo(b.value)
                            }
                        }
                    )
                    .map { it.key }

                // BƯỚC 2 (Thời hạn <= 5 giây): Đo các ứng viên tốt từ các vùng + các mục đang hiển thị trên màn hình (tiết kiệm trên mạng di động khi quét nền)
                val testedIds = step1Targets.map { it.id }.toSet()
                val visibleTargets = allServers.filter { it.id in visibleServerIds && it.id !in testedIds }
                val regionalCandidates = if (isCellularLightMode) {
                    emptyList()
                } else {
                    orderedCountries.flatMap { code ->
                        allServers
                            .filter {
                                it.countryCode.equals(code, ignoreCase = true) &&
                                    it.id !in testedIds &&
                                    !isDynamicLiveProxy(it)
                            }
                            .take(10)
                    }
                }
                val step2Targets = (visibleTargets + regionalCandidates)
                    .distinctBy { it.id }
                    .take(if (isCellularLightMode) 6 else 24)

                if (step2Targets.isNotEmpty() && myGeneration == measurementGeneration.get() && !isInGameStreamActive) {
                    com.example.data.AppLogger.info(
                        "PING",
                        "Đợt đo #$myGeneration • Bước 2: Đo kỹ ${step2Targets.size} máy chủ ứng viên & mục đang hiển thị (thời hạn ≤5s)"
                    )
                    runMeasurementStepWithDeadline(
                        generationId = myGeneration,
                        targets = step2Targets,
                        stepDeadlineMs = 5000L,
                        useCache = !forceRefresh
                    )
                }

                if (myGeneration != measurementGeneration.get()) return@launch

                // Nếu công tắc Globalping được bật và có GeoIP thật, đo tham khảo đơn lẻ cho máy chủ đang chọn
                val probeEst = if (com.example.data.SettingsRepository.config.value.enableGlobalpingProbe) {
                    RegionalProbeTester.measureSelectedServerEstimateViaProbe(_state.value.currentServer)
                } else {
                    null
                }

                val finalPings = _state.value.serverPings
                val finalMeas = _state.value.serverMeasurements
                val primaryAliveCount = finalMeas.values.count { it.isPrimaryPortAlive }
                val fallbackOnlyCount = finalMeas.values.count { it.isFallbackPortOnly }
                val untestedCount = (allServers.size - finalMeas.size).coerceAtLeast(0)

                com.example.data.AppLogger.success(
                    "PING",
                    "Hoàn tất đợt đo #$myGeneration: $primaryAliveCount cổng chính mở, $fallbackOnlyCount chỉ mở cổng phụ (:443/:80), $untestedCount chưa đo (đo lười khi cuộn)"
                )

                _state.value = _state.value.copy(
                    selectedServerProbeEstimate = probeEst,
                    isTestingPings = false
                )

                val autoBypassMode = com.example.data.SettingsRepository.config.value.autoBypassEnabled
                val currentSrvStatus = getServerUnlockStatus(currentSrv, records)
                val currentSrvPing = finalPings[currentSrv.id] ?: -1
                val isCurrentSrvTooFar = !currentSrv.isCustom &&
                    !RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                        server = currentSrv,
                        servers = allServers,
                        geo = _state.value.detectedGeoIp,
                        allowTimezoneFallback = true
                    )

                val validPool = allServers.filter {
                    val st = getServerUnlockStatus(it, records)
                    st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
                }
                val distanceLimitedPool = filterServersWithinDistanceLimit(
                    servers = validPool,
                    geo = _state.value.detectedGeoIp,
                    allowTimezoneFallback = true
                )
                val sessions = com.example.data.SessionHistoryRepository.sessions.value
                val candidates = sortServersByUnlockAndPing(
                    servers = distanceLimitedPool,
                    pingMap = finalPings,
                    records = records,
                    engine = currentEngine,
                    sessions = sessions,
                    measurementsMap = finalMeas,
                    prioritizeDistanceForAutoBypass = autoBypassMode
                )

                val best = candidates.firstOrNull { cand ->
                    val candMeas = finalMeas[cand.id]
                    val candPrimaryPing = if (candMeas != null) {
                        if (candMeas.isPrimaryPortAlive) candMeas.rttMs else -1
                    } else {
                        finalPings[cand.id] ?: -1
                    }
                    candPrimaryPing > 0
                } ?: if (currentEngine == VpnEngine.SMART_BYPASS) {
                    candidates.firstOrNull { getLastSessionPingForServer(it, sessions) > 0 }
                        ?: candidates.firstOrNull()
                } else {
                    candidates.firstOrNull()
                }

                if (best != null) {
                    val currentScore = computeSingleServerScore(currentSrv, allServers, finalMeas, finalPings, records, sessions, autoBypassMode)
                    val bestScore = computeSingleServerScore(best, allServers, finalMeas, finalPings, records, sessions, autoBypassMode)
                    val allowSwitch = shouldAutoSwitchServer(
                        currentServer = currentSrv,
                        candidateServer = best,
                        currentScore = currentScore,
                        candidateScore = bestScore,
                        currentMeasurement = finalMeas[currentSrv.id],
                        currentUnlockStatus = currentSrvStatus,
                        userManuallySelected = _state.value.userManuallySelectedServer,
                        forcePickBest = autoPickBest || isCurrentSrvTooFar
                    )

                    val measuredBest = finalPings[best.id] ?: -1
                    val bestPing = if (measuredBest > 0) {
                        measuredBest
                    } else if (currentEngine == VpnEngine.SMART_BYPASS) {
                        getLastSessionPingForServer(best, sessions)
                    } else {
                        -1
                    }

                    if (allowSwitch || autoPickBest) {
                        val reasonVi = buildServerSelectionReasonText(
                            server = best,
                            isEn = false,
                            geo = _state.value.detectedGeoIp,
                            measurementsMap = finalMeas,
                            pingMap = finalPings,
                            sessions = sessions
                        )
                        com.example.data.AppLogger.success(
                            "VPN",
                            "Chọn máy chủ tối ưu (${best.countryCode} • $reasonVi): ${best.name}"
                        )
                        withContext(Dispatchers.Main) {
                            selectServer(best, context ?: appContext, isManualSelection = false)
                            if (autoPickBest) {
                                onPickedBest?.invoke(best, bestPing)
                            }
                        }
                    }
                } else if (autoPickBest) {
                    withContext(Dispatchers.Main) {
                        onPickedBest?.invoke(_state.value.currentServer, currentSrvPing)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "testAllServers interrupted", e)
                if (myGeneration == measurementGeneration.get()) {
                    _state.value = _state.value.copy(isTestingPings = false)
                }
            }
        }
    }

    private fun computeSingleServerScore(
        server: ServerLocation,
        allServers: List<ServerLocation>,
        measurementsMap: Map<String, EndpointPingMeasurement>,
        pingMap: Map<String, Int>,
        records: Map<String, ServerUnlockRecord>,
        sessions: List<com.example.data.PlaySessionRecord>,
        autoBypassEnabled: Boolean
    ): Double {
        val currentGeo = _state.value.detectedGeoIp
        val sameCountryNames = allServers
            .filter { it.countryCode.equals(server.countryCode, ignoreCase = true) }
            .map { it.name }
            .toSet()
        val sessionStats = com.example.data.SessionHistoryRepository.getRegionSessionStats(
            serverName = server.name,
            countryCode = server.countryCode,
            sameCountryServerNames = sameCountryNames,
            maxRecentSessions = 5,
            sessionsList = sessions
        )
        val distKm = RegionalProbeTester.getServerDistanceKm(server, currentGeo, allowTimezoneFallback = false)
            ?: RegionalProbeTester.getServerDistanceKm(server, currentGeo, allowTimezoneFallback = true)
        return calculateServerSelectionScore(
            server = server,
            measurement = measurementsMap[server.id],
            fallbackPingMs = pingMap[server.id],
            unlockStatus = getServerUnlockStatus(server, records),
            sessionStats = sessionStats,
            autoBypassEnabled = autoBypassEnabled,
            distanceKm = distKm
        )
    }

    private suspend fun runMeasurementStepWithDeadline(
        generationId: Long,
        targets: List<ServerLocation>,
        stepDeadlineMs: Long,
        useCache: Boolean
    ) {
        if (targets.isEmpty() || generationId != measurementGeneration.get()) return

        // Đánh dấu TESTING cho các máy chủ trong bước này
        synchronized(this) {
            if (generationId == measurementGeneration.get()) {
                val markTesting = _state.value.serverMeasurements.toMutableMap()
                for (srv in targets) {
                    markTesting[srv.id] = EndpointPingMeasurement(
                        rttMs = -1,
                        configuredPort = srv.port,
                        measuredPort = srv.port,
                        resolvedIp = srv.ip,
                        measurementType = PingMeasurementType.NONE,
                        status = ServerPingStatus.TESTING
                    )
                }
                _state.value = _state.value.copy(serverMeasurements = markTesting)
            }
        }

        kotlinx.coroutines.withTimeoutOrNull(stepDeadlineMs) {
            val deferreds = targets.map { server ->
                scope.async(Dispatchers.IO) {
                    if (generationId != measurementGeneration.get()) return@async
                    val meas = PingTester.measureServerEndpoint(
                        server = server,
                        timeoutMs = 850,
                        useCache = useCache
                    )
                    recordMeasurementIfCurrentGeneration(generationId, server.id, meas)
                }
            }
            deferreds.awaitAll()
        }

        // Bất kỳ mục nào trong bước này chưa tới lượt chạy xong khi hết hạn stepDeadlineMs -> trả về UNTESTED để đo lười khi cuộn
        synchronized(this) {
            if (generationId == measurementGeneration.get()) {
                val nextMeas = _state.value.serverMeasurements.toMutableMap()
                for (srv in targets) {
                    val cur = nextMeas[srv.id]
                    if (cur != null && cur.status == ServerPingStatus.TESTING) {
                        nextMeas.remove(srv.id)
                    }
                }
                _state.value = _state.value.copy(
                    serverMeasurements = nextMeas
                )
            }
        }
    }

    /**
     * 5. TỰ CHUYỂN: khi người dùng xác nhận thông báo "Máy chủ này không mở khóa được, đổi sang máy chủ khác?",
     * chuyển sang máy chủ tốt nhất kế tiếp (không chọn máy chủ "Không mở khóa" hoặc "Sai vùng").
     */
    fun confirmSwitchToNextBestServer(context: Context? = null): ServerLocation? {
        val current = _state.value.currentServer
        val records = _state.value.unlockRecords
        val all = getAllServers()

        // Lọc bỏ máy chủ đang lỗi, các máy chủ "Không mở khóa" / "Sai vùng", và giới hạn khoảng cách (<= 5.500 km)
        val eligibleAnySupported = all.filter {
            it.addressPortKey != current.addressPortKey &&
                getServerUnlockStatus(it, records) != RegionUnlockStatus.FAILED &&
                getServerUnlockStatus(it, records) != RegionUnlockStatus.WRONG_REGION
        }
        val distanceLimitedEligible = filterServersWithinDistanceLimit(
            servers = eligibleAnySupported,
            geo = _state.value.detectedGeoIp,
            allowTimezoneFallback = true
        )

        val sortedPool = sortServersByUnlockAndPing(
            distanceLimitedEligible,
            _state.value.serverPings,
            records
        )
        val bestNext = sortedPool.firstOrNull() ?: return null

        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        _state.value = _state.value.copy(
            currentServer = bestNext,
            unlockFailPromptServer = null,
            statusMessage = if (isEn) {
                "🔄 Switched to next best server: ${bestNext.name} ${bestNext.flag}"
            } else {
                "🔄 Đã chuyển sang máy chủ tốt nhất kế tiếp: ${bestNext.name} ${bestNext.flag}"
            }
        )
        selectServer(bestNext, context ?: appContext, isManualSelection = false)
        return bestNext
    }

    private const val PREF_V130_SMART_ROUTING_UPGRADED = "upgraded_v1_3_0_smart_routing_step"

    fun isV130SmartRoutingUpgraded(context: Context? = appContext): Boolean {
        val ctx = context ?: appContext ?: return false
        return try {
            val sp = ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            sp.getBoolean(PREF_V130_SMART_ROUTING_UPGRADED, false)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Kiểm tra kết quả tự chọn máy chủ trong cache 6 giờ còn hợp lệ hay không (Mục 6):
     * - Hết hạn sau 6 giờ (`AUTO_SELECT_CACHE_TTL_MS`).
     * - Hết hạn khi đổi loại mạng (`WIFI` <-> `CELLULAR`).
     * - Hết hạn khi vị trí quốc gia `detectedGeoIp` thay đổi.
     */
    fun isAutoSelectionCacheFresh(
        context: Context? = appContext,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val ctx = context ?: appContext ?: return false
        return try {
            val sp = ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val savedServerId = sp.getString("selected_server_id", null)
            if (savedServerId.isNullOrBlank()) return false
            val lastTs = sp.getLong(PREF_LAST_AUTO_SELECT_TS, 0L)
            if (lastTs <= 0L || (nowMs - lastTs) >= AUTO_SELECT_CACHE_TTL_MS) return false

            val savedNet = sp.getString(PREF_LAST_AUTO_SELECT_NET_TYPE, "").orEmpty()
            val curNet = resolveCurrentNetworkTransportTag(ctx)
            if (savedNet.isNotBlank() && curNet != "UNKNOWN" && savedNet != curNet) return false

            val savedCountry = sp.getString(PREF_LAST_AUTO_SELECT_COUNTRY, "").orEmpty()
            val curCountry = _state.value.detectedGeoIp?.takeIf { !it.isFromWebView }?.countryCode.orEmpty()
            if (savedCountry.isNotBlank() && curCountry.isNotBlank() && !savedCountry.equals(curCountry, ignoreCase = true)) {
                return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun recordAutoSelectionMetadata(context: Context? = appContext) {
        val ctx = context ?: appContext ?: return
        val now = System.currentTimeMillis()
        _state.value = _state.value.copy(
            userManuallySelectedServer = false,
            lastAutoSelectTimestampMs = now
        )
        try {
            val sp = ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val curCountry = _state.value.detectedGeoIp?.takeIf { !it.isFromWebView }?.countryCode.orEmpty()
            sp.edit()
                .putBoolean(PREF_USER_MANUALLY_SELECTED, false)
                .putLong(PREF_LAST_AUTO_SELECT_TS, now)
                .putString(PREF_LAST_AUTO_SELECT_NET_TYPE, resolveCurrentNetworkTransportTag(ctx))
                .putString(PREF_LAST_AUTO_SELECT_COUNTRY, curCountry)
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun setManualSelectionFlag(context: Context? = appContext, isManual: Boolean) {
        _state.value = _state.value.copy(userManuallySelectedServer = isManual)
        val ctx = context ?: appContext ?: return
        try {
            ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_USER_MANUALLY_SELECTED, isManual)
                .apply()
        } catch (_: Exception) {
        }
    }

    /**
     * Chọn vùng/máy chủ mặc định có thuật toán (Mục 3 & Mục 6):
     * - Khi mở app lần đầu, khi vị trí GeoIP thay đổi, hoặc khi kết quả tự chọn đã quá 6 giờ:
     *   1. Xếp các nước thuộc `SUPPORTED_COUNTRIES` theo khoảng cách địa lý (chỉ làm thứ tự ưu tiên ban đầu).
     *   2. Đo ping thật ngắn (`PingTester.measureServerEndpoint`, timeout ngắn, ít mẫu) tới máy chủ đại diện của vài nước đứng đầu.
     *   3. Chấm điểm bằng `calculateServerSelectionScore` và chọn máy chủ tốt nhất.
     *   4. `DEFAULT_SERVERS[0]` chỉ là phương án dự phòng cuối cùng khi mọi phép đo đều thất bại và chưa có dữ liệu.
     */
    suspend fun ensureInitialOrGeoUpdatedOptimalServer(
        context: Context? = appContext,
        force: Boolean = false
    ): ServerLocation? = withContext(Dispatchers.IO) {
        if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
            return@withContext null
        }
        val ctx = context ?: appContext
        val currentSrv = _state.value.currentServer
        if (!force && (_state.value.userManuallySelectedServer || currentSrv.isCustom)) {
            return@withContext null
        }
        if (!force && isAutoSelectionCacheFresh(ctx)) {
            return@withContext null
        }

        val allServers = getAllServers()
        val records = _state.value.unlockRecords
        val validPool = allServers.filter {
            val st = getServerUnlockStatus(it, records)
            st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
        }
        val distanceLimitedPool = filterServersWithinDistanceLimit(
            servers = validPool,
            geo = _state.value.detectedGeoIp,
            allowTimezoneFallback = true
        )

        val isCellular = isCellularConnected(ctx)
        val probeTargets = selectStep1RepresentativeServers(
            allServers = distanceLimitedPool,
            records = records,
            maxPerCountry = if (isCellular) 1 else 2,
            maxCountries = 2
        )

        val currentGen = measurementGeneration.get()
        val unmeasuredTargets = probeTargets.filter { srv ->
            val existing = _state.value.serverMeasurements[srv.id]
            existing == null || existing.status == ServerPingStatus.UNTESTED || existing.status == ServerPingStatus.EXPIRED
        }

        if (unmeasuredTargets.isNotEmpty() && !isInGameStreamActive) {
            kotlinx.coroutines.withTimeoutOrNull(if (isCellular) 2200L else 3000L) {
                unmeasuredTargets.map { srv ->
                    async(Dispatchers.IO) {
                        val meas = PingTester.measureServerEndpoint(
                            server = srv,
                            timeoutMs = if (isCellular) 750 else 850,
                            useCache = !force
                        )
                        recordMeasurementIfCurrentGeneration(currentGen, srv.id, meas)
                    }
                }.awaitAll()
            }
        }

        return@withContext runV130SmartRoutingUpgradeStep(context = ctx, force = true)
    }

    /**
     * Bước chọn máy chủ theo thuật toán điểm số thống nhất (Mục 3, 4, 5):
     * - Không dùng số giả định ("~70ms"), không ghi đè lựa chọn thủ công của người dùng khi `force = false`.
     */
    fun runV130SmartRoutingUpgradeStep(
        context: Context? = appContext,
        force: Boolean = false
    ): ServerLocation? {
        val ctx = context ?: appContext
        val currentSrv = _state.value.currentServer
        if (!force) {
            if (_state.value.userManuallySelectedServer || currentSrv.isCustom) {
                return null
            }
            if (ctx != null && isV130SmartRoutingUpgraded(ctx) && isAutoSelectionCacheFresh(ctx)) {
                return null
            }
        }

        val autoBypass = com.example.data.SettingsRepository.config.value.autoBypassEnabled
        val allServers = getAllServers()
        val records = _state.value.unlockRecords
        val validPool = allServers.filter {
            val st = getServerUnlockStatus(it, records)
            st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
        }
        val distanceLimitedPool = filterServersWithinDistanceLimit(
            servers = validPool,
            geo = _state.value.detectedGeoIp,
            allowTimezoneFallback = true
        )
        val sessions = com.example.data.SessionHistoryRepository.sessions.value
        val measurements = _state.value.serverMeasurements
        val pings = _state.value.serverPings

        val sorted = sortServersByUnlockAndPing(
            servers = distanceLimitedPool,
            pingMap = pings,
            records = records,
            engine = _state.value.engine,
            sessions = sessions,
            measurementsMap = measurements,
            prioritizeDistanceForAutoBypass = autoBypass
        )
        val targetBest = sorted.firstOrNull() ?: DEFAULT_SERVERS.firstOrNull() ?: return null

        val isFirstSelection = ctx?.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            ?.getString("selected_server_id", null) == null
        val isCurrentTooFar = !currentSrv.isCustom &&
            !RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                server = currentSrv,
                servers = allServers,
                geo = _state.value.detectedGeoIp,
                allowTimezoneFallback = true
            )

        val currentScore = computeSingleServerScore(currentSrv, allServers, measurements, pings, records, sessions, autoBypass)
        val candidateScore = computeSingleServerScore(targetBest, allServers, measurements, pings, records, sessions, autoBypass)
        val shouldSwitch = shouldAutoSwitchServer(
            currentServer = currentSrv,
            candidateServer = targetBest,
            currentScore = currentScore,
            candidateScore = candidateScore,
            currentMeasurement = measurements[currentSrv.id],
            currentUnlockStatus = getServerUnlockStatus(currentSrv, records),
            userManuallySelected = _state.value.userManuallySelectedServer,
            forcePickBest = force || isFirstSelection || isCurrentTooFar
        )

        if (ctx != null) {
            try {
                ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_V130_SMART_ROUTING_UPGRADED, true)
                    .apply()
            } catch (_: Exception) {
            }
        }
        recordAutoSelectionMetadata(ctx)

        if (targetBest.id != currentSrv.id && shouldSwitch) {
            selectServer(targetBest, ctx, isManualSelection = false)
            val reasonText = buildServerSelectionReasonText(
                server = targetBest,
                isEn = false,
                geo = _state.value.detectedGeoIp,
                measurementsMap = measurements,
                pingMap = pings,
                sessions = sessions
            )
            com.example.data.AppLogger.success(
                "UPGRADE_1.3",
                "Tự động chọn máy chủ tối ưu (${targetBest.countryCode} • $reasonText): ${targetBest.flag} ${targetBest.name}"
            )
        }
        return if (shouldSwitch) targetBest else currentSrv
    }

    private fun loadPersistedServers(context: Context) {
        try {
            val sp = context.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val defaultKeys = DEFAULT_SERVERS.map { "${it.ip.trim().lowercase()}:${it.port}" }.toSet()

            // 1. Khôi phục danh sách máy chủ do người dùng tự thêm (customServers)
            val customList = mutableListOf<ServerLocation>()
            val customJsonStr = sp.getString("custom_servers_json", null)
            if (!customJsonStr.isNullOrBlank()) {
                val array = org.json.JSONArray(customJsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val cc = obj.optString("countryCode", "JP")
                    val ip = obj.getString("ip")
                    val port = obj.getInt("port")
                    val key = "${ip.trim().lowercase()}:$port"
                    if (id.startsWith("live_all_", ignoreCase = true) ||
                        cc.equals("ALL", ignoreCase = true) ||
                        cc.equals("CUSTOM", ignoreCase = true) ||
                        key in defaultKeys
                    ) {
                        continue
                    }
                    customList.add(
                        ServerLocation(
                            id = id,
                            name = obj.getString("name"),
                            flag = obj.optString("flag", "🌐"),
                            ip = ip,
                            port = port,
                            protocol = obj.optString("protocol", "http"),
                            countryCode = cc,
                            azureEndpoint = obj.optString("azureEndpoint", "japaneast.cloudapp.azure.com"),
                            description = obj.optString("description", "Proxy cá nhân"),
                            speedMbps = obj.optInt("speedMbps", 0),
                            lastPingMs = null,
                            isCustom = true
                        )
                    )
                }
            }

            // 2. Khôi phục danh sách các máy chủ VPN đã được cập nhật & lọc lỗi tự động (liveVerifiedServers)
            val customKeys = customList.map { "${it.ip.trim().lowercase()}:${it.port}" }.toSet()
            val liveList = mutableListOf<ServerLocation>()
            val liveJsonStr = sp.getString("auto_verified_live_servers_json", null)
            if (!liveJsonStr.isNullOrBlank()) {
                val liveArray = org.json.JSONArray(liveJsonStr)
                for (i in 0 until liveArray.length()) {
                    val obj = liveArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val cc = obj.optString("countryCode", "").trim().uppercase()
                    val ip = obj.optString("ip", "").trim()
                    val port = obj.optInt("port", 0)
                    val key = "${ip.lowercase()}:$port"
                    if (id.isBlank() ||
                        !LiveProxyFetcher.isValidPublicEndpoint(ip, port) ||
                        !GeoIpDetector.SUPPORTED_COUNTRIES.contains(cc) ||
                        key in defaultKeys ||
                        key in customKeys
                    ) {
                        continue
                    }
                    val rawDesc = obj.optString("description", "Đã kiểm tra kết nối")
                        .replace("Uy tín cao • Đã lọc lỗi", "Đã kiểm tra kết nối")
                        .replace("An toàn • Đã xác minh", "Đã kiểm tra kết nối")
                        .replace("An toàn • Uy tín", "Đã kiểm tra kết nối")
                    liveList.add(
                        ServerLocation(
                            id = id,
                            name = obj.optString("name", "${GeoIpDetector.getFlagEmoji(cc)} $ip"),
                            flag = obj.optString("flag", GeoIpDetector.getFlagEmoji(cc)),
                            ip = ip,
                            port = port,
                            protocol = obj.optString("protocol", "http"),
                            countryCode = cc,
                            azureEndpoint = obj.optString("azureEndpoint", GeoIpDetector.getAzureEndpointForCountry(cc)),
                            description = rawDesc,
                            speedMbps = 0,
                            lastPingMs = obj.optInt("lastPingMs", -1).takeIf { it > 0 },
                            isCustom = false,
                            isVerified = false,
                            trustScore = obj.optInt("trustScore", 90),
                            safetyBadge = "Đã kiểm tra kết nối"
                        )
                    )
                }
            }

            val savedManual = sp.getBoolean(PREF_USER_MANUALLY_SELECTED, false)
            val savedAutoTs = sp.getLong(PREF_LAST_AUTO_SELECT_TS, 0L)

            _state.value = _state.value.copy(
                customServers = customList,
                liveVerifiedServers = liveList,
                userManuallySelectedServer = savedManual,
                lastAutoSelectTimestampMs = savedAutoTs
            )

            val lastServerId = sp.getString("selected_server_id", null)
            if (lastServerId != null) {
                val all = customList + DEFAULT_SERVERS + liveList
                val found = all.find { it.id == lastServerId }
                if (found != null) {
                    _state.value = _state.value.copy(currentServer = found)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load persisted servers", e)
        }
    }

    private fun persistLiveVerifiedServers(context: Context?, liveServers: List<ServerLocation>) {
        val ctx = context ?: appContext ?: return
        try {
            val sp = ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val array = org.json.JSONArray()
            // Lưu tối đa 40 máy chủ tự động tốt nhất để bộ nhớ ứng dụng luôn gọn và nhanh
            for (s in liveServers.take(40)) {
                val obj = org.json.JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("flag", s.flag)
                    put("ip", s.ip)
                    put("port", s.port)
                    put("protocol", s.protocol)
                    put("countryCode", s.countryCode)
                    put("description", s.description)
                    put("speedMbps", s.speedMbps)
                    put("azureEndpoint", s.azureEndpoint)
                    put("trustScore", s.trustScore)
                    put("safetyBadge", s.safetyBadge)
                    if (s.lastPingMs != null && s.lastPingMs > 0) {
                        put("lastPingMs", s.lastPingMs)
                    }
                }
                array.put(obj)
            }
            sp.edit()
                .putString("auto_verified_live_servers_json", array.toString())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist live verified servers", e)
        }
    }

    private fun persistCustomServers(context: Context?, servers: List<ServerLocation>, selectedServer: ServerLocation?) {
        val ctx = context ?: appContext ?: return
        try {
            val sp = ctx.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val array = org.json.JSONArray()
            for (s in servers) {
                val obj = org.json.JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("flag", s.flag)
                    put("ip", s.ip)
                    put("port", s.port)
                    put("protocol", s.protocol)
                    put("countryCode", s.countryCode)
                    put("description", s.description)
                    put("speedMbps", s.speedMbps)
                    put("azureEndpoint", s.azureEndpoint)
                }
                array.put(obj)
            }
            sp.edit()
                .putString("custom_servers_json", array.toString())
                .putString("selected_server_id", selectedServer?.id)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist servers", e)
        }
    }

    fun setEngine(engine: VpnEngine, context: Context? = null) {
        val wasConnected = _state.value.mode == NetworkMode.VPN_JAPAN
        _state.value = _state.value.copy(engine = engine)
        if (engine == VpnEngine.PROXY_TUNNEL && _state.value.serverPings.isEmpty()) {
            testAllServers(autoPickBest = false, context = context ?: appContext)
        }
        if (wasConnected && context != null) {
            connect(context, _state.value.currentServer)
        }
    }

    /**
     * Vô hiệu hóa ngay kết quả IP cũ khi vừa đổi mạng hoặc bật/tắt VPN,
     * đảm bảo app không dùng lại `detectedGeoIp` cũ trong lúc đang xác định lại IP mới.
     */
    fun invalidateIpForNetworkChange(reason: String = "mạng đổi") {
        isIpInvalidatedByNetworkChange = true
        lastRealGeoIp = null
        lastIpCheckTimestampMs = 0L
        if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
            return
        }
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        val checkingDecision = SmartVpnDecision(
            checked = false,
            needsVpn = true,
            userCountryCode = "...",
            userCountryName = if (isEn) "Checking IP..." else "Đang xác định IP...",
            isSupportedRegion = false,
            message = if (isEn) {
                "🔄 Network/VPN changed ($reason). Re-checking IP location..."
            } else {
                "🔄 Phát hiện thay đổi mạng/VPN ($reason). Đang xác định lại vị trí IP..."
            },
            actionSummary = if (isEn) "Checking IP..." else "Đang xác định IP..."
        )
        _state.value = _state.value.copy(
            detectedGeoIp = null,
            smartDecision = checkingDecision,
            statusMessage = if (isEn) "🔄 Checking IP..." else "🔄 Đang xác định IP..."
        )
    }

    fun isIpInvalidatedOrChecking(): Boolean = isIpInvalidatedByNetworkChange

    fun isIpStaleForLaunch(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (isIpInvalidatedByNetworkChange) return true
        val realGeo = _state.value.detectedGeoIp?.takeIf { !it.isFromWebView } ?: lastRealGeoIp
        if (realGeo == null) return true
        val checkedAt = if (lastIpCheckTimestampMs > 0L) lastIpCheckTimestampMs else realGeo.timestamp
        return (nowMs - checkedAt) > IP_PRE_LAUNCH_MAX_AGE_MS
    }

    fun onAppForeground() {
        if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
            return
        }
        val now = System.currentTimeMillis()
        val hasValidIp = !isIpInvalidatedByNetworkChange && (_state.value.detectedGeoIp != null || lastRealGeoIp != null)
        if (hasValidIp && lastIpCheckTimestampMs > 0L && (now - lastIpCheckTimestampMs) < IP_FOREGROUND_COOLDOWN_MS) {
            return
        }
        checkCurrentIp(reason = "quay lại foreground", force = true)
    }

    fun checkCurrentIp(reason: String = "khởi động", force: Boolean = false) {
        if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
            return
        }
        val now = System.currentTimeMillis()
        if (!force &&
            !isIpInvalidatedByNetworkChange &&
            lastIpCheckTimestampMs > 0L &&
            (now - lastIpCheckTimestampMs) < IP_FOREGROUND_COOLDOWN_MS &&
            (_state.value.detectedGeoIp != null || lastRealGeoIp != null)
        ) {
            return
        }
        com.example.data.AppLogger.info("VPN", "[VPN] Kiểm tra IP (lý do: $reason)")
        ipCheckJob?.cancel()
        ipCheckJob = scope.launch(Dispatchers.IO) {
            if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
                return@launch
            }
            val result = GeoIpDetector.checkCurrentIp()
            if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
                return@launch
            }
            val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
            if (result != null) {
                isIpInvalidatedByNetworkChange = false
                lastRealGeoIp = result
                lastIpCheckTimestampMs = System.currentTimeMillis()
                updateSmartDecision(result)
            } else {
                isIpInvalidatedByNetworkChange = false
                val offlineDecision = SmartVpnDecision(
                    checked = true,
                    needsVpn = false,
                    userCountryCode = "--",
                    userCountryName = if (isEn) "Unknown / Offline" else "Không xác định / Ngoại tuyến",
                    isSupportedRegion = false,
                    message = if (isEn) {
                        "🌐 Could not detect network IP (Offline or DNS blocked). You can enable Bypass or select a Server manually."
                    } else {
                        "🌐 Không xác định được địa chỉ IP mạng (Mất kết nối hoặc chặn DNS). Bạn có thể bật Bypass hoặc chọn Server thủ công."
                    },
                    actionSummary = if (isEn) "Manual: Select preferred server" else "Thủ công: Chọn máy chủ theo ý muốn"
                )
                _state.value = _state.value.copy(
                    detectedGeoIp = null,
                    smartDecision = offlineDecision,
                    smartVpnPhase = SmartVpnPhase.IDLE,
                    statusMessage = if (isEn) "Offline / Unknown IP" else "Chưa kết nối mạng / Không rõ IP"
                )
            }
        }
    }

    fun updateSmartDecision(geo: GeoIpResult?) {
        val currentGeo = geo ?: _state.value.detectedGeoIp
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        if (currentGeo == null) {
            val offlineDecision = SmartVpnDecision(
                checked = true,
                needsVpn = false,
                userCountryCode = "--",
                userCountryName = if (isEn) "Unknown / Offline" else "Không xác định / Ngoại tuyến",
                isSupportedRegion = false,
                message = if (isEn) {
                    "🌐 Network IP not yet detected. You can enable Bypass or select a Server manually."
                } else {
                    "🌐 Chưa xác định được IP mạng. Bạn có thể bật Bypass hoặc chọn Server thủ công."
                },
                actionSummary = if (isEn) "Manual: Select preferred server" else "Thủ công: Chọn máy chủ theo ý muốn"
            )
            _state.value = _state.value.copy(smartDecision = offlineDecision)
            return
        }

        if (!currentGeo.isFromWebView) {
            isIpInvalidatedByNetworkChange = false
            lastRealGeoIp = currentGeo
            lastIpCheckTimestampMs = currentGeo.timestamp
        }

        val isSupported = currentGeo.isXboxSupported
        val needsVpn = !isSupported
        val decision = SmartVpnDecision(
            checked = true,
            needsVpn = needsVpn,
            userCountryCode = currentGeo.countryCode,
            userCountryName = currentGeo.countryName,
            isSupportedRegion = isSupported,
            message = if (needsVpn) {
                if (isEn) {
                    "⚠️ Detected IP ${currentGeo.ip} in ${currentGeo.countryName} ${currentGeo.flagEmoji} (xCloud unsupported). Smart VPN will AUTO TURN ON at launch & AUTO TURN OFF in-game!"
                } else {
                    "⚠️ Phát hiện IP ${currentGeo.ip} tại ${currentGeo.countryName} ${currentGeo.flagEmoji} (Chưa hỗ trợ xCloud). Smart VPN sẽ TỰ ĐỘNG BẬT khi mở game & TỰ ĐỘNG TẮT khi vào trận!"
                }
            } else {
                if (isEn) {
                    "✅ Detected IP ${currentGeo.ip} in ${currentGeo.countryName} ${currentGeo.flagEmoji} (xCloud supported). NO VPN NEEDED! Keeping Direct ISP for lowest ping."
                } else {
                    "✅ Phát hiện IP ${currentGeo.ip} tại ${currentGeo.countryName} ${currentGeo.flagEmoji} ĐÃ HỖ TRỢ xCloud. KHÔNG CẦN BẬT VPN! Luôn giữ Direct ISP để đạt Ping thấp nhất."
                }
            },
            actionSummary = if (needsVpn) {
                if (isEn) "Auto: ON at launch ➔ OFF in-game" else "Tự động: BẬT lúc mở game ➔ TẮT khi vào trận"
            } else {
                if (isEn) "Auto: Keep direct network" else "Tự động: Giữ mạng trực tiếp"
            }
        )

        // Khi vùng của người dùng đã được Xbox hỗ trợ (!currentGeo.isFromWebView && isSupported):
        // mặc định đi mạng trực tiếp, không tự bật VPN.
        val isRealSupportedRegion = isSupported && !currentGeo.isFromWebView
        if (isRealSupportedRegion && _state.value.mode != NetworkMode.VPN_JAPAN) {
            clearWebViewProxy()
        }

        // Cập nhật detectedGeoIp trước khi chấm điểm ứng viên để sortServersByUnlockAndPing dùng đúng vị trí mới
        _state.value = _state.value.copy(detectedGeoIp = currentGeo)

        // Tự động chuyển sang quốc gia hỗ trợ Xbox gần người dùng nhất nếu server hiện tại
        // vượt quá giới hạn khoảng cách (> 5.500 km như server Mỹ khi người dùng ở Việt Nam)
        // hoặc chưa xác nhận mở khóa và chưa bị khóa bởi lựa chọn thủ công.
        var updatedCurrentServer = _state.value.currentServer
        val records = _state.value.unlockRecords
        val currentServerStatus = getServerUnlockStatus(updatedCurrentServer, records)
        if (!currentGeo.isFromWebView && !updatedCurrentServer.isCustom) {
            val allServers = getAllServers()
            val isCurrentExceedingDistanceLimit = !RegionalProbeTester.isWithinAutoSelectDistanceLimit(
                server = updatedCurrentServer,
                servers = allServers,
                geo = currentGeo,
                allowTimezoneFallback = true
            )
            val currentCountryTier = RegionalProbeTester.getNearestSupportedCountryTier(
                serverCountryCode = updatedCurrentServer.countryCode,
                servers = allServers,
                geo = currentGeo
            )
            val canAutoSwitch = !_state.value.userManuallySelectedServer ||
                isCurrentExceedingDistanceLimit ||
                currentServerStatus == RegionUnlockStatus.FAILED ||
                currentServerStatus == RegionUnlockStatus.WRONG_REGION

            if (canAutoSwitch && (isCurrentExceedingDistanceLimit || (currentServerStatus != RegionUnlockStatus.UNLOCKED && currentCountryTier > 0))) {
                val nearestCountryCandidates = sortServersByUnlockAndPing(
                    servers = allServers.filter {
                        val st = getServerUnlockStatus(it, records)
                        RegionalProbeTester.isWithinAutoSelectDistanceLimit(it, allServers, currentGeo, allowTimezoneFallback = true) &&
                            RegionalProbeTester.getNearestSupportedCountryTier(it.countryCode, allServers, currentGeo) == 0 &&
                            st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
                    }
                )
                val bestInNearestCountry = nearestCountryCandidates.firstOrNull()
                if (bestInNearestCountry != null && bestInNearestCountry.id != updatedCurrentServer.id) {
                    val prevCode = updatedCurrentServer.countryCode
                    updatedCurrentServer = bestInNearestCountry
                    persistCustomServers(null, _state.value.customServers, bestInNearestCountry)
                    com.example.data.AppLogger.info(
                        "SmartSelect",
                        "Tự động đổi máy chủ từ $prevCode sang vùng trong giới hạn khoảng cách (${bestInNearestCountry.countryCode}): ${bestInNearestCountry.name}"
                    )
                }
            }
        }

        val nextPhase = when {
            isSupported -> SmartVpnPhase.DIRECT_SUPPORTED_REGION
            _state.value.mode == NetworkMode.VPN_JAPAN -> SmartVpnPhase.BYPASS_ACTIVE
            _state.value.mode == NetworkMode.DIRECT_NETWORK -> SmartVpnPhase.DIRECT_GAMING
            else -> SmartVpnPhase.IDLE
        }

        _state.value = _state.value.copy(
            currentServer = updatedCurrentServer,
            detectedGeoIp = currentGeo,
            smartDecision = decision,
            smartVpnPhase = nextPhase,
            statusMessage = if (isSupported) {
                if (isEn) {
                    "✅ Valid IP: ${currentGeo.countryName} ${currentGeo.flagEmoji} • No VPN needed (Direct ISP)"
                } else {
                    "✅ IP hợp lệ: ${currentGeo.countryName} ${currentGeo.flagEmoji} • Không cần VPN (Mạng Trực Tiếp)"
                }
            } else {
                if (isEn) {
                    "🤖 Smart VPN ready: Auto ON/OFF for IP ${currentGeo.countryName} ${currentGeo.flagEmoji}"
                } else {
                    "🤖 Smart VPN sẵn sàng: Tự động Bật/Tắt cho IP ${currentGeo.countryName} ${currentGeo.flagEmoji}"
                }
            }
        )
    }

    /**
     * Determines whether the player actually needs VPN to pass Xbox region check.
     * Ignores spoofed in-WebView GeoIP results so Smart VPN always activates for launch
     * and auto-disconnects once inside the game.
     */
    fun needsVpnToPlay(): Boolean {
        if (isIpInvalidatedByNetworkChange) {
            return true
        }
        val geo = _state.value.detectedGeoIp
        if (geo != null && !geo.isFromWebView) {
            return !geo.isXboxSupported
        }
        val realGeo = lastRealGeoIp
        if (realGeo != null && !realGeo.isFromWebView) {
            return !realGeo.isXboxSupported
        }
        return _state.value.smartDecision.needsVpn
    }

    /**
     * Smart VPN Lifecycle: Auto Turn ON when launching game or opening catalog.
     * Nếu kết quả IP đã cũ hơn ngưỡng `IP_PRE_LAUNCH_MAX_AGE_MS` hoặc vừa có sự kiện đổi mạng/VPN,
     * kiểm tra lại IP trước (với timeout ngắn `IP_PRE_LAUNCH_TIMEOUT_MS`) rồi mới quyết định bật VPN hay đi mạng trực tiếp.
     */
    fun autoTurnOnForLaunch(context: Context) {
        val appCtx = context.applicationContext
        if (isIpStaleForLaunch() && !com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
            if (!isIpInvalidatedByNetworkChange) {
                invalidateIpForNetworkChange("trước khi vào game")
            }
            com.example.data.AppLogger.info("VPN", "[VPN] Kiểm tra IP (lý do: trước khi vào game)")
            ipCheckJob?.cancel()
            ipCheckJob = scope.launch {
                val freshGeo = kotlinx.coroutines.withTimeoutOrNull(IP_PRE_LAUNCH_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        GeoIpDetector.checkCurrentIp()
                    }
                }
                if (freshGeo != null) {
                    isIpInvalidatedByNetworkChange = false
                    lastRealGeoIp = freshGeo
                    lastIpCheckTimestampMs = System.currentTimeMillis()
                    updateSmartDecision(freshGeo)
                } else {
                    isIpInvalidatedByNetworkChange = false
                }
                applyLaunchNetworkDecision(appCtx)
            }
            return
        }
        applyLaunchNetworkDecision(appCtx)
    }

    private fun applyLaunchNetworkDecision(context: Context) {
        if (!needsVpnToPlay()) {
            ensureDirectNetwork()
            return
        }
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        if (_state.value.mode != NetworkMode.VPN_JAPAN) {
            connect(context, _state.value.currentServer)
            _state.value = _state.value.copy(
                mode = NetworkMode.VPN_JAPAN,
                autoBypassTriggered = false,
                smartVpnPhase = SmartVpnPhase.BYPASS_ACTIVE,
                statusMessage = if (isEn) {
                    "🤖 Smart VPN: AUTO TURNED ON to unlock game region..."
                } else {
                    "🤖 Smart VPN: Đã TỰ ĐỘNG BẬT để mở khóa vùng vào game..."
                }
            )
        } else {
            _state.value = _state.value.copy(
                autoBypassTriggered = false,
                smartVpnPhase = SmartVpnPhase.BYPASS_ACTIVE
            )
        }
    }

    /**
     * Smart VPN Lifecycle: Keep Direct Network if user is in supported region
     */
    fun ensureDirectNetwork() {
        clearWebViewProxy()
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        _state.value = _state.value.copy(
            mode = NetworkMode.DIRECT_NETWORK,
            smartVpnPhase = SmartVpnPhase.DIRECT_SUPPORTED_REGION,
            statusMessage = if (isEn) {
                "⚡ Smart VPN: Supported region, keeping Direct ISP"
            } else {
                "⚡ Smart VPN: Vị trí hợp lệ, giữ Mạng Trực Tiếp"
            }
        )
    }

    fun setSmartVpnPhase(phase: SmartVpnPhase) {
        if (_state.value.smartVpnPhase == phase) return
        _state.value = _state.value.copy(smartVpnPhase = phase)
    }

    fun onWebViewIpDetected(ip: String, countryCode: String, countryName: String? = null, city: String? = null) {
        GeoIpDetector.onWebViewReported(ip, countryCode, countryName, city)
        val currentGeo = GeoIpDetector.currentGeoIp.value
        if (currentGeo != null && _state.value.engine == VpnEngine.PROXY_TUNNEL && _state.value.mode == NetworkMode.VPN_JAPAN) {
            if (!currentGeo.isXboxSupported) {
                saveServerUnlockRecord(_state.value.currentServer, RegionUnlockStatus.WRONG_REGION, currentGeo.countryCode)
            }
        }
        updateSmartDecision(currentGeo)
    }

    fun onRegionBypassed(info: String) {
        Log.d(TAG, "Xbox Region bypassed via In-WebView Interceptor: $info")
        val currentServer = _state.value.currentServer
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        saveServerUnlockRecord(currentServer, RegionUnlockStatus.UNLOCKED, currentServer.countryCode)
        val spoofGeo = GeoIpResult(
            ip = currentServer.ip,
            countryCode = currentServer.countryCode,
            countryName = GeoIpDetector.getCountryDisplayName(currentServer.countryCode),
            city = if (currentServer.countryCode == "JP") "Tokyo" else "Seoul",
            flagEmoji = currentServer.flag,
            isXboxSupported = true,
            isFromWebView = true
        )
        _state.value = _state.value.copy(
            isBypassActiveInBrowser = true,
            unlockFailPromptServer = null,
            detectedGeoIp = spoofGeo,
            smartVpnPhase = SmartVpnPhase.BYPASS_ACTIVE,
            statusMessage = if (isEn) {
                "✅ Xbox Region unlocked! Region: ${currentServer.name} ${currentServer.flag}"
            } else {
                "✅ Đã vượt rào Xbox thành công! Khu vực: ${currentServer.name} ${currentServer.flag}"
            }
        )
    }

    fun connect(context: Context, server: ServerLocation = _state.value.currentServer) {
        preCheckProxyRegion(server)
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        _state.value = _state.value.copy(
            isConnecting = true,
            currentServer = server,
            unlockFailPromptServer = null,
            lastError = null,
            statusMessage = if (isEn) "Connecting to ${server.name} ${server.flag}..." else "Đang kết nối tới ${server.name} ${server.flag}..."
        )

        scope.launch {
            val isEnNow = com.example.data.SettingsRepository.config.value.isEnglishUI()
            when (_state.value.engine) {
                VpnEngine.PROXY_TUNNEL -> {
                    val isAlive = withContext(Dispatchers.IO) {
                        PingTester.isHostReachable(server.ip, server.port, 1000)
                    }
                    var targetServer = server

                    if (!isAlive && !server.isCustom) {
                        Log.w(TAG, "Server ${server.name} unreachable, finding best working backup...")
                        _state.value = _state.value.copy(
                            statusMessage = if (isEnNow) "🔄 Checking proxy route..." else "🔄 Đang kiểm tra đường truyền proxy..."
                        )

                        val workingBackup = findFirstResponsiveServer(server.countryCode)
                        if (workingBackup != null) {
                            targetServer = workingBackup
                            _state.value = _state.value.copy(
                                currentServer = targetServer,
                                autoSwitchedBackupCount = _state.value.autoSwitchedBackupCount + 1,
                                statusMessage = if (isEnNow) "✅ Selected server: ${targetServer.name}" else "✅ Đã chọn máy chủ: ${targetServer.name}"
                            )
                        } else {
                            Log.w(TAG, "Proxy ports blocked or unreachable. Auto-falling back to Smart Bypass mode.")
                            _state.value = _state.value.copy(
                                engine = VpnEngine.SMART_BYPASS,
                                statusMessage = if (isEnNow) "⚡ Auto-switched to Smart Bypass (anti-throttle)" else "⚡ Tự động chuyển Smart Bypass (chống nghẽn mạng)"
                            )
                            clearWebViewProxy()
                        }
                    }

                    applyMultiServerProxy(targetServer)

                    val targetGeo = GeoIpResult(
                        ip = targetServer.ip,
                        countryCode = targetServer.countryCode,
                        countryName = GeoIpDetector.getCountryDisplayName(targetServer.countryCode),
                        city = if (targetServer.countryCode == "JP") "Tokyo" else "Seoul",
                        flagEmoji = targetServer.flag,
                        isXboxSupported = true,
                        isFromWebView = true
                    )

                    _state.value = _state.value.copy(
                        mode = NetworkMode.VPN_JAPAN,
                        isConnecting = false,
                        autoBypassTriggered = false,
                        detectedGeoIp = targetGeo,
                        statusMessage = if (isEnNow) "✅ Proxy Active: ${targetServer.name} (${targetServer.ip}:${targetServer.port})" else "✅ Đã kích hoạt Proxy: ${targetServer.name} (${targetServer.ip}:${targetServer.port})"
                    )
                }

                VpnEngine.SMART_BYPASS -> {
                    clearWebViewProxy()

                    val targetGeo = GeoIpResult(
                        ip = server.ip,
                        countryCode = server.countryCode,
                        countryName = GeoIpDetector.getCountryDisplayName(server.countryCode),
                        city = if (server.countryCode == "JP") "Tokyo" else "Seoul",
                        flagEmoji = server.flag,
                        isXboxSupported = true,
                        isFromWebView = true
                    )

                    XboxCloudVpnService.startVpn(context, server)

                    _state.value = _state.value.copy(
                        mode = NetworkMode.VPN_JAPAN,
                        isConnecting = false,
                        autoBypassTriggered = false,
                        isBypassActiveInBrowser = true,
                        detectedGeoIp = targetGeo,
                        statusMessage = if (isEnNow) "✅ Smart Bypass Active • ${server.name} (${server.ip})" else "✅ Đã kích hoạt Smart Bypass • ${server.name} (${server.ip})"
                    )
                }

                VpnEngine.EXTERNAL_VPN -> {
                    clearWebViewProxy()
                    XboxCloudVpnService.stopVpn(context)
                    _state.value = _state.value.copy(
                        mode = NetworkMode.VPN_JAPAN,
                        isConnecting = false,
                        autoBypassTriggered = false,
                        statusMessage = if (isEnNow) "External VPN mode: Ready to auto-switch on game start" else "Chế độ VPN ngoài: Sẵn sàng tự động chuyển mạng khi vào game"
                    )
                    checkCurrentIp(reason = "VPN bật-tắt", force = true)
                }
            }

            startLatencyMonitoring()
        }
    }

    fun disconnect(context: Context) {
        clearWebViewProxy()
        XboxCloudVpnService.stopVpn(context)
        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        _state.value = _state.value.copy(
            mode = if (needsVpnToPlay()) NetworkMode.DISCONNECTED else NetworkMode.DIRECT_NETWORK,
            isConnecting = false,
            isBypassActiveInBrowser = false,
            smartVpnPhase = if (needsVpnToPlay()) SmartVpnPhase.IDLE else SmartVpnPhase.DIRECT_SUPPORTED_REGION,
            statusMessage = if (isEn) "Disconnected" else "Đã ngắt kết nối"
        )
        checkCurrentIp(reason = "VPN bật-tắt", force = true)
    }

    /**
     * Cycles to the next backup server in the same country if current server fails or loads slowly.
     * Skips servers marked "Không mở khóa" (FAILED) or "Sai vùng" (WRONG_REGION).
     */
    fun switchNextBackupServer(context: Context) {
        val next = confirmSwitchToNextBestServer(context)
        if (next == null) {
            val allServers = getAllServers()
            val fallback = allServers.firstOrNull { it.id != _state.value.currentServer.id } ?: _state.value.currentServer
            selectServer(fallback, context)
        }
    }

    /**
     * Finds and connects to the best valid server in the selected country.
     * Không tự chọn máy chủ "Không mở khóa" (FAILED) hoặc "Sai vùng" (WRONG_REGION).
     * Nếu vùng người dùng đã hỗ trợ Xbox (needsVpnToPlay() == false), giữ mạng trực tiếp.
     */
    fun autoSelectBestServer(countryCode: String, context: Context) {
        if (isInGameStreamActive || com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) return
        if (!needsVpnToPlay()) {
            ensureDirectNetwork()
            return
        }
        scope.launch {
            val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
            val currentEngine = _state.value.engine
            _state.value = _state.value.copy(
                isConnecting = true,
                statusMessage = if (isEn) "Finding optimal $countryCode server..." else "Đang tìm máy chủ $countryCode tối ưu..."
            )

            val records = _state.value.unlockRecords
            val allInCountry = getAllServers().filter { it.countryCode.equals(countryCode, ignoreCase = true) }
            val eligible = allInCountry.filter {
                val st = getServerUnlockStatus(it, records)
                st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
            }

            if (currentEngine == VpnEngine.SMART_BYPASS) {
                val sortedCandidates = sortServersByUnlockAndPing(
                    servers = eligible,
                    pingMap = emptyMap(),
                    records = records,
                    engine = VpnEngine.SMART_BYPASS
                )
                val best = sortedCandidates.firstOrNull()
                if (best != null) {
                    val lastPlayPing = getLastSessionPingForServer(best)
                    val pingSuffix = if (lastPlayPing > 0) " (${lastPlayPing}ms)" else ""
                    _state.value = _state.value.copy(
                        currentServer = best,
                        statusMessage = if (isEn) "⚡ Selected server: ${best.name}$pingSuffix" else "⚡ Đã chọn máy chủ: ${best.name}$pingSuffix"
                    )
                    connect(context, best)
                } else {
                    _state.value = _state.value.copy(
                        isConnecting = false,
                        statusMessage = if (isEn) "⚠️ No valid unlock server found in $countryCode" else "⚠️ Không tìm thấy máy chủ mở khóa hợp lệ trong vùng $countryCode"
                    )
                }
                return@launch
            }

            val tested: List<Pair<ServerLocation, Int>> = kotlinx.coroutines.coroutineScope {
                eligible.map { s ->
                    async(Dispatchers.IO) {
                        val ping = PingTester.testServerPing(s, 1200)
                        Pair(s, ping)
                    }
                }.awaitAll()
            }

            val pingMap = tested.toMap().mapKeys { it.key.id }
            val updatedServerPings = _state.value.serverPings.toMutableMap().apply { putAll(pingMap) }
            _state.value = _state.value.copy(serverPings = updatedServerPings)

            val responsive = tested.filter { it.second > 0 }.map { it.first }
            val sortedCandidates = sortServersByUnlockAndPing(
                servers = responsive,
                pingMap = pingMap,
                records = records,
                engine = currentEngine
            )
            val best: ServerLocation? = sortedCandidates.firstOrNull()
            if (best != null) {
                val bestPing = pingMap[best.id] ?: -1
                _state.value = _state.value.copy(
                    currentServer = best,
                    statusMessage = if (isEn) "⚡ Selected best server: ${best.name} (${bestPing}ms)" else "⚡ Đã chọn máy chủ tốt nhất: ${best.name} (${bestPing}ms)"
                )
                connect(context, best)
            } else {
                _state.value = _state.value.copy(
                    isConnecting = false,
                    statusMessage = if (isEn) "⚠️ No reachable proxy server found in $countryCode" else "⚠️ Không đo được proxy phản hồi trong vùng $countryCode"
                )
            }
        }
    }

    private suspend fun findFirstResponsiveServer(countryCode: String): ServerLocation? = withContext(Dispatchers.IO) {
        val records = _state.value.unlockRecords
        val rawCandidates = getAllServers().filter {
            it.countryCode.equals(countryCode, ignoreCase = true) &&
                getServerUnlockStatus(it, records) != RegionUnlockStatus.FAILED &&
                getServerUnlockStatus(it, records) != RegionUnlockStatus.WRONG_REGION
        }
        val candidates = sortServersByUnlockAndPing(rawCandidates, _state.value.serverPings, records)
        for (server in candidates) {
            if (PingTester.isHostReachable(server.ip, server.port, 800)) {
                return@withContext server
            }
        }
        candidates.firstOrNull()
    }

    /**
     * Auto-Bypass Trigger:
     * Disconnects VPN / Clears Proxy to revert to user's direct ISP network
     * WITHOUT page reload, keeping the WebRTC stream uninterrupted!
     */
    fun switchToDirectNetwork(context: Context, triggerSource: String = "Game stream detected") {
        if (_state.value.mode == NetworkMode.DIRECT_NETWORK) return

        // Only clear WebView proxy if PROXY_TUNNEL actually applied a ProxyConfig override;
        // do NOT stop XboxCloudVpnService so Android maintains foreground CPU/Network scheduling priority during gameplay.
        clearWebViewProxy()

        val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
        _state.value = _state.value.copy(
            mode = NetworkMode.DIRECT_NETWORK,
            isConnecting = false,
            autoBypassTriggered = true,
            smartVpnPhase = SmartVpnPhase.DIRECT_GAMING,
            statusMessage = if (isEn) {
                "⚡ Smart VPN: AUTO DISCONNECTED VPN ($triggerSource)! Streaming directly via ISP (Lowest Ping)."
            } else {
                "⚡ Smart VPN: Đã TỰ ĐỘNG TẮT VPN ($triggerSource)! Đang truyền trực tiếp qua mạng ISP (Ping thấp nhất)."
            }
        )

        scope.launch(Dispatchers.IO) {
            val directPing = PingTester.measureDirectLatency()
            _state.value = _state.value.copy(
                directLatencyMs = if (directPing > 0) directPing else null
            )
            startLatencyMonitoring()
        }
    }

    fun selectServer(
        server: ServerLocation,
        context: Context? = null,
        isManualSelection: Boolean = true
    ) {
        val wasConnected = _state.value.mode == NetworkMode.VPN_JAPAN
        val cachedProbe = RegionalProbeTester.getCachedEstimateForServer(server)
        _state.value = _state.value.copy(
            currentServer = server,
            unlockFailPromptServer = null,
            selectedServerProbeEstimate = cachedProbe,
            userManuallySelectedServer = isManualSelection
        )
        if (isManualSelection) {
            setManualSelectionFlag(context ?: appContext, true)
        } else {
            recordAutoSelectionMetadata(context ?: appContext)
        }
        com.example.data.AppLogger.info(
            "VPN",
            "Chọn máy chủ (${if (isManualSelection) "Thủ công" else "Tự động"}): ${server.name} (${server.ip}:${server.port})"
        )
        persistCustomServers(context, _state.value.customServers, server)
        preCheckProxyRegion(server)
        if (!isInGameStreamActive && !com.example.cloudplay.NetworkOptimizer.status.value.isLowLatencyStreamActive) {
            val gen = measurementGeneration.get()
            scope.launch(Dispatchers.IO) {
                val meas = PingTester.measureServerEndpoint(server, timeoutMs = 950, useCache = true)
                recordMeasurementIfCurrentGeneration(gen, server.id, meas)
                if (com.example.data.SettingsRepository.config.value.enableGlobalpingProbe) {
                    val est = RegionalProbeTester.measureSelectedServerEstimateViaProbe(server)
                    if (est != null && _state.value.currentServer.id == server.id) {
                        _state.value = _state.value.copy(selectedServerProbeEstimate = est)
                    }
                }
            }
        }
        if (wasConnected && context != null) {
            connect(context, server)
        }
    }

    fun addCustomServer(server: ServerLocation) {
        if (GeoIpDetector.isPrivateOrLoopbackIp(server.ip)) return
        val resolvedCc = server.countryCode.trim().uppercase().ifBlank {
            GeoIpDetector.resolveIpCountryCode(server.ip, allowNetworkFallback = false)
        }
        val normalizedServer = server.copy(
            countryCode = resolvedCc,
            flag = GeoIpDetector.getFlagEmoji(resolvedCc),
            azureEndpoint = server.azureEndpoint.trim().ifBlank { GeoIpDetector.getAzureEndpointForCountry(resolvedCc) }
        )
        val updated = _state.value.customServers + normalizedServer
        _state.value = _state.value.copy(customServers = updated, currentServer = normalizedServer, unlockFailPromptServer = null)
        persistCustomServers(appContext, updated, normalizedServer)
        preCheckProxyRegion(normalizedServer)
    }

    fun removeCustomServer(serverId: String) {
        val updated = _state.value.customServers.filterNot { it.id == serverId }
        val newCurrent = if (_state.value.currentServer.id == serverId) {
            updated.firstOrNull() ?: DEFAULT_SERVERS[0]
        } else {
            _state.value.currentServer
        }
        _state.value = _state.value.copy(customServers = updated, currentServer = newCurrent)
        persistCustomServers(appContext, updated, newCurrent)
    }

    fun getAllServers(): List<ServerLocation> {
        return (_state.value.customServers + DEFAULT_SERVERS + _state.value.liveVerifiedServers)
            .distinctBy { it.addressPortKey.lowercase() }
    }

    private fun ruleFor(server: ServerLocation): String {
        val proto = server.protocol.lowercase()
        return if (proto == "socks5") {
            "socks5://${server.ip}:${server.port}"
        } else {
            "http://${server.ip}:${server.port}"
        }
    }

    /**
     * Configures Android WebView with primary proxy and up to 3 backup proxy rules.
     * Chromium will automatically failover to subsequent rules if primary connection fails!
     */
    private fun applyMultiServerProxy(primaryServer: ServerLocation) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                val builder = ProxyConfig.Builder()
                // Không đẩy trang vỏ www.xbox.com / Akamai (edgesuite.net) và trang đăng nhập Microsoft qua proxy công cộng
                // vì Akamai WAF sẽ chặn IP proxy với lỗi "Access Denied Reference #18 (errors.edgesuite.net)".
                // Chỉ các API kiểm tra vùng của Xbox (*.xboxlive.com / *.xboxservices.com) mới cần đi qua proxy.
                builder.addBypassRule("www.xbox.com")
                builder.addBypassRule("xbox.com")
                builder.addBypassRule("play.xbox.com")
                builder.addBypassRule("*.edgesuite.net")
                builder.addBypassRule("*.akamaiedge.net")
                builder.addBypassRule("*.akamai.net")
                builder.addBypassRule("login.live.com")
                builder.addBypassRule("login.microsoftonline.com")
                builder.addProxyRule(ruleFor(primaryServer))

                // Add up to 3 backup servers from the same region (excluding FAILED / WRONG_REGION) for auto-fallback in Chromium
                val records = _state.value.unlockRecords
                val backups = sortServersByUnlockAndPing(
                    getAllServers().filter {
                        it.id != primaryServer.id &&
                            it.countryCode == primaryServer.countryCode &&
                            getServerUnlockStatus(it, records) != RegionUnlockStatus.FAILED &&
                            getServerUnlockStatus(it, records) != RegionUnlockStatus.WRONG_REGION
                    },
                    _state.value.serverPings,
                    records
                ).take(3)

                backups.forEach { backup ->
                    builder.addProxyRule(ruleFor(backup))
                }

                val proxyConfig = builder.build()
                isWebViewProxyOverrideActive = true
                ProxyController.getInstance().setProxyOverride(proxyConfig, proxyExecutor) {
                    Log.d(TAG, "WebView proxy applied: ${ruleFor(primaryServer)} with ${backups.size} fallbacks")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply WebView proxy", e)
            }
        }
    }

    @Volatile
    private var isWebViewProxyOverrideActive: Boolean = false

    private fun clearWebViewProxy() {
        if (!isWebViewProxyOverrideActive) return
        isWebViewProxyOverrideActive = false
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                ProxyController.getInstance().clearProxyOverride(proxyExecutor) {
                    Log.d(TAG, "WebView proxy cleared")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear WebView proxy", e)
            }
        }
    }

    @Volatile
    private var isProxyPausedForExternal: Boolean = false

    fun pauseProxyForExternalNavigation() {
        if (_state.value.mode == NetworkMode.VPN_JAPAN && !isProxyPausedForExternal) {
            isProxyPausedForExternal = true
            clearWebViewProxy()
            Log.d(TAG, "Temporarily bypassed proxy for external navigation (direct ISP for banking/3DSecure/login)")
        }
    }

    fun restoreProxyForGaming() {
        if (isProxyPausedForExternal) {
            isProxyPausedForExternal = false
            if (_state.value.mode == NetworkMode.VPN_JAPAN) {
                applyMultiServerProxy(_state.value.currentServer)
                Log.d(TAG, "Restored proxy for Xbox Cloud gaming")
            }
        }
    }

    @Volatile
    private var isInGameStreamActive: Boolean = false

    fun isStreamActive(): Boolean = isInGameStreamActive

    /**
     * Khi bước vào game (`active = true`), lập tức hủy mọi tiến trình quét VPN, đo ping hàng loạt,
     * kiểm tra IP nền và đồng bộ thư viện để nhường 100% CPU + băng thông mạng cho bộ giải mã video phần cứng (Hardware Decoder).
     */
    fun setInGameStreamActive(active: Boolean) {
        isInGameStreamActive = active
        try {
            com.example.data.GameLibraryRepository.setStreamActive(active)
        } catch (_: Exception) {
        }
        if (active) {
            autoSyncLiveJob?.cancel()
            batchPingJob?.cancel()
            lazyPingJob?.cancel()
            ipCheckJob?.cancel()
            networkChangeDebounceJob?.cancel()
            _state.value = _state.value.copy(
                activeStreamServerId = _state.value.currentServer.id,
                isAutoSyncingLiveProxies = false,
                isTestingPings = false
            )
        } else {
            _state.value = _state.value.copy(
                activeStreamServerId = null
            )
            if (isIpInvalidatedByNetworkChange) {
                checkCurrentIp(reason = "mạng đổi", force = true)
            }
        }
        com.example.data.AppLogger.info(
            "STREAM",
            if (active) {
                "Bắt đầu phiên stream trên máy chủ: ${_state.value.currentServer.name} (Đã tạm dừng cập nhật nền để tối ưu tốc độ giải mã video)"
            } else {
                "Kết thúc phiên stream"
            }
        )
    }

    private fun startLatencyMonitoring() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val cur = _state.value
                val isDirect = cur.mode == NetworkMode.DIRECT_NETWORK
                val currentEngine = cur.engine
                val liveWebRtcRtt = com.example.cloudplay.NetworkOptimizer.telemetry.value.rttMs

                if (liveWebRtcRtt > 0) {
                    // Chỉ ghi lại state và cấp phát danh sách pingHistory mới khi độ trễ thực sự thay đổi
                    val prevRtt = if (isDirect) cur.directLatencyMs else cur.latencyMs
                    val lastSample = cur.pingHistory.lastOrNull()
                    if (prevRtt != liveWebRtcRtt || lastSample != liveWebRtcRtt) {
                        val newHistory = (cur.pingHistory + liveWebRtcRtt).takeLast(16)
                        if (isDirect) {
                            _state.value = cur.copy(directLatencyMs = liveWebRtcRtt, pingHistory = newHistory)
                        } else {
                            _state.value = cur.copy(latencyMs = liveWebRtcRtt, pingHistory = newHistory)
                        }
                    }
                } else if (!isInGameStreamActive) {
                    if (isDirect) {
                        val tcpPing = PingTester.measureDirectLatency()
                        if (tcpPing > 0 && _state.value.directLatencyMs != tcpPing) {
                            _state.value = _state.value.copy(directLatencyMs = tcpPing)
                        }
                    } else if (currentEngine == VpnEngine.PROXY_TUNNEL) {
                        val srv = _state.value.currentServer
                        val tcpPing = PingTester.measureServerLatency(srv)
                        if (tcpPing > 0 && (_state.value.latencyMs != tcpPing || _state.value.serverPings[srv.id] != tcpPing)) {
                            val updatedMap = _state.value.serverPings.toMutableMap()
                            updatedMap[srv.id] = tcpPing
                            _state.value = _state.value.copy(latencyMs = tcpPing, serverPings = updatedMap)
                        }
                    }
                }
                delay(if (isInGameStreamActive || liveWebRtcRtt > 0) 10000L else 5000L)
            }
        }
    }
}
