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

    init {
        // Initial location check at launch
        checkCurrentIp()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            GeoIpDetector.downloadDbIpLiteIfPermitted(context.applicationContext, isCellularConfirmedByUser = false)
        }
        loadPersistedServers(context.applicationContext)
        loadPersistedUnlockRecords(context.applicationContext)
        if (_state.value.serverPings.isEmpty() && _state.value.serverMeasurements.isEmpty()) {
            testAllServers(autoPickBest = false, context = context.applicationContext, forceRefresh = false)
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
     * 1. KIỂM TRA TRƯỚC (nhẹ): khi chọn hoặc cập nhật proxy, lấy quốc gia của IP thoát
     * (dùng GeoIpDetector sẵn có). Nếu quốc gia không thuộc danh sách vùng hỗ trợ thì gắn nhãn "Sai vùng".
     */
    fun preCheckProxyRegion(server: ServerLocation) {
        val declaredCode = server.countryCode.trim().uppercase()
        if (declaredCode.isNotBlank() && !GeoIpDetector.SUPPORTED_COUNTRIES.contains(declaredCode)) {
            saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, declaredCode)
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                // Kiểm tra quốc gia của IP máy chủ/proxy kết hợp GeoIpDetector.SUPPORTED_COUNTRIES
                val detectedCountry = queryIpCountryLight(server.ip)
                if (!detectedCountry.isNullOrBlank()) {
                    val cleanCode = detectedCountry.trim().uppercase()
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
        servers.forEach { server ->
            val declaredCode = server.countryCode.trim().uppercase()
            if (declaredCode.isNotBlank() && !GeoIpDetector.SUPPORTED_COUNTRIES.contains(declaredCode)) {
                saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, declaredCode)
            }
        }
        scope.launch(Dispatchers.IO) {
            servers.take(15).forEach { server ->
                val currentStatus = getServerUnlockStatus(server)
                if (currentStatus == RegionUnlockStatus.UNTESTED) {
                    val detectedCountry = queryIpCountryLight(server.ip)
                    if (!detectedCountry.isNullOrBlank()) {
                        val cleanCode = detectedCountry.trim().uppercase()
                        if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(cleanCode)) {
                            saveServerUnlockRecord(server, RegionUnlockStatus.WRONG_REGION, cleanCode)
                        }
                    }
                }
            }
        }
    }

    private fun queryIpCountryLight(ip: String): String? {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank() || cleanIp == "127.0.0.1" || cleanIp == "localhost") return null
        return try {
            val url = java.net.URL("https://api.country.is/$cleanIp")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = org.json.JSONObject(body)
                json.optString("country", "").ifBlank { null }
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
                _state.value = _state.value.copy(
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

    fun getLastSessionPingForServer(
        server: ServerLocation,
        sessions: List<com.example.data.PlaySessionRecord> = com.example.data.SessionHistoryRepository.sessions.value
    ): Int {
        return com.example.data.SessionHistoryRepository.getAverageRttForServerOrRegion(
            serverName = server.name,
            countryCode = server.countryCode,
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
     * 4. SẮP XẾP ĐA TẦNG MINH BẠCH (A3, A5, A8):
     * - Tách riêng trạng thái hợp lệ (UNLOCKED / UNTESTED) trước trạng thái lỗi mở khóa (FAILED / WRONG_REGION).
     * - Phân tầng trạng thái đo kết nối (pingReachabilityTier):
     *   + Tầng 0: Cổng chính phản hồi (`MEASURED_PRIMARY` hoặc `ping > 0`).
     *   + Tầng 1: Chỉ cổng phụ `:443` / `:80` phản hồi (`MEASURED_FALLBACK_ONLY`) — không bao giờ xếp trên cổng chính sống.
     *   + Tầng 2: Chưa đo (`UNTESTED` / `TESTING` / `EXPIRED`).
     *   + Tầng 3: Không đo được (`UNREACHABLE`) — máy chủ tĩnh/tự thêm.
     *   + Tầng 4: Không đo được (`UNREACHABLE`) — máy chủ động từ `LiveProxyFetcher` (`live_*`) xếp xuống cuối cùng.
     * - Trong cùng tầng kết nối:
     *   + Ưu tiên `UNLOCKED` trước `UNTESTED`.
     *   + Khi đã có số đo thực tế (Tầng 0 hoặc Tầng 1): ưu tiên đường truyền ổn định (`!isUnstable`) rồi xếp trực tiếp theo **số ping đo thật từ thấp đến cao**. Khoảng cách địa lý KHÔNG được ghi đè số đo thật.
     *   + Khi chưa có số đo (Tầng 2): mới dùng khoảng cách địa lý sơ bộ (`getNearestSupportedCountryTier` — trả về 0 nếu thiếu GeoIP).
     */
    fun sortServersByUnlockAndPing(
        servers: List<ServerLocation>,
        pingMap: Map<String, Int> = _state.value.serverPings,
        records: Map<String, ServerUnlockRecord> = _state.value.unlockRecords,
        engine: VpnEngine = _state.value.engine,
        sessions: List<com.example.data.PlaySessionRecord> = com.example.data.SessionHistoryRepository.sessions.value,
        measurementsMap: Map<String, EndpointPingMeasurement> = _state.value.serverMeasurements
    ): List<ServerLocation> {
        val currentGeo = _state.value.detectedGeoIp
        return servers.sortedWith(
            Comparator { a, b ->
                val statusA = getServerUnlockStatus(a, records)
                val statusB = getServerUnlockStatus(b, records)
                val tierA = unlockEligibilityTier(statusA)
                val tierB = unlockEligibilityTier(statusB)
                if (tierA != tierB) {
                    return@Comparator tierA.compareTo(tierB)
                }

                if (engine == VpnEngine.SMART_BYPASS && pingMap.isEmpty() && measurementsMap.isEmpty()) {
                    val rankA = unlockSortRank(statusA)
                    val rankB = unlockSortRank(statusB)
                    if (rankA != rankB) {
                        return@Comparator rankA.compareTo(rankB)
                    }
                    val lastPlayA = getLastSessionPingForServer(a, sessions)
                    val lastPlayB = getLastSessionPingForServer(b, sessions)
                    val hasPlayA = lastPlayA > 0
                    val hasPlayB = lastPlayB > 0
                    if (hasPlayA != hasPlayB) {
                        return@Comparator if (hasPlayA) -1 else 1
                    }
                    if (hasPlayA && hasPlayB && lastPlayA != lastPlayB) {
                        return@Comparator lastPlayA.compareTo(lastPlayB)
                    }
                    val countryTierA = RegionalProbeTester.getNearestSupportedCountryTier(a.countryCode, servers, currentGeo)
                    val countryTierB = RegionalProbeTester.getNearestSupportedCountryTier(b.countryCode, servers, currentGeo)
                    if (countryTierA != countryTierB) {
                        return@Comparator countryTierA.compareTo(countryTierB)
                    }
                    return@Comparator 0
                }

                val measA = measurementsMap[a.id]
                val measB = measurementsMap[b.id]
                val pingA = if (measA?.isPrimaryPortAlive == true) measA.rttMs else (pingMap[a.id] ?: 0)
                val pingB = if (measB?.isPrimaryPortAlive == true) measB.rttMs else (pingMap[b.id] ?: 0)

                val reachTierA = computePingReachabilityTier(a, measA, pingMap)
                val reachTierB = computePingReachabilityTier(b, measB, pingMap)
                if (reachTierA != reachTierB) {
                    return@Comparator reachTierA.compareTo(reachTierB)
                }

                // Ưu tiên UNLOCKED trước UNTESTED trong cùng tầng kết nối
                val rankA = unlockSortRank(statusA)
                val rankB = unlockSortRank(statusB)
                if (rankA != rankB) {
                    return@Comparator rankA.compareTo(rankB)
                }

                // Tầng 0 (Cổng chính sống): xếp trực tiếp theo độ ổn định và số ping đo thật (KHÔNG để địa lý ghi đè ping thật)
                if (reachTierA == 0) {
                    val unstableA = measA?.isUnstable == true
                    val unstableB = measB?.isUnstable == true
                    if (unstableA != unstableB) {
                        return@Comparator if (!unstableA) -1 else 1
                    }
                    if (pingA != pingB) {
                        return@Comparator pingA.compareTo(pingB)
                    }
                    val countryTierA = RegionalProbeTester.getNearestSupportedCountryTier(a.countryCode, servers, currentGeo)
                    val countryTierB = RegionalProbeTester.getNearestSupportedCountryTier(b.countryCode, servers, currentGeo)
                    if (countryTierA != countryTierB) {
                        return@Comparator countryTierA.compareTo(countryTierB)
                    }
                    return@Comparator 0
                }

                // Tầng 1 (Chỉ cổng phụ :443/:80 sống): xếp theo RTT cổng phụ
                if (reachTierA == 1) {
                    val fbPingA = measA?.rttMs ?: Int.MAX_VALUE
                    val fbPingB = measB?.rttMs ?: Int.MAX_VALUE
                    if (fbPingA != fbPingB) {
                        return@Comparator fbPingA.compareTo(fbPingB)
                    }
                }

                // Tầng 2 (Chưa đo): dùng khoảng cách địa lý sơ bộ nếu có GeoIP hợp lệ, ưu tiên máy chủ tĩnh trước máy chủ động
                if (reachTierA == 2) {
                    val countryTierA = RegionalProbeTester.getNearestSupportedCountryTier(a.countryCode, servers, currentGeo)
                    val countryTierB = RegionalProbeTester.getNearestSupportedCountryTier(b.countryCode, servers, currentGeo)
                    if (countryTierA != countryTierB) {
                        return@Comparator countryTierA.compareTo(countryTierB)
                    }
                    val dynA = isDynamicLiveProxy(a)
                    val dynB = isDynamicLiveProxy(b)
                    if (dynA != dynB) {
                        return@Comparator if (!dynA) -1 else 1
                    }
                }

                0
            }
        )
    }

    private fun computePingReachabilityTier(
        server: ServerLocation,
        measurement: EndpointPingMeasurement?,
        pingMap: Map<String, Int>
    ): Int {
        if (measurement != null) {
            return when (measurement.status) {
                ServerPingStatus.MEASURED_PRIMARY -> if (measurement.rttMs > 0) 0 else 3
                ServerPingStatus.MEASURED_FALLBACK_ONLY -> if (measurement.rttMs > 0) 1 else 3
                ServerPingStatus.UNTESTED, ServerPingStatus.TESTING, ServerPingStatus.EXPIRED -> 2
                ServerPingStatus.UNREACHABLE -> if (isDynamicLiveProxy(server)) 4 else 3
            }
        }
        val rawPing = pingMap[server.id] ?: return 2 // Chưa có trong pingMap -> Chưa đo (Tầng 2)
        return if (rawPing > 0) {
            0
        } else {
            if (isDynamicLiveProxy(server)) 4 else 3
        }
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
     * Chọn tập máy chủ cho Bước 1 (2–3 đại diện mỗi quốc gia/vùng) để thăm dò sơ bộ.
     */
    fun selectStep1RepresentativeServers(
        allServers: List<ServerLocation>,
        records: Map<String, ServerUnlockRecord> = _state.value.unlockRecords
    ): List<ServerLocation> {
        val valid = allServers.filter {
            val st = getServerUnlockStatus(it, records)
            st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
        }
        return valid
            .groupBy { it.countryCode.trim().uppercase() }
            .flatMap { (_, listInCountry) ->
                listInCountry
                    .sortedWith(
                        compareBy<ServerLocation> { unlockSortRank(getServerUnlockStatus(it, records)) }
                            .thenBy { isDynamicLiveProxy(it) }
                    )
                    .take(3)
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
        if (visibleServers.isEmpty()) return
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
        batchPingJob?.cancel()
        lazyPingJob?.cancel()

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

                // BƯỚC 1 (Thời hạn <= 5 giây): Đo 2–3 đại diện mỗi vùng/quốc gia
                val step1Targets = (selectStep1RepresentativeServers(allServers, records) + currentSrv)
                    .distinctBy { it.id }

                com.example.data.AppLogger.info(
                    "PING",
                    "Đợt đo #$myGeneration • Bước 1: Thăm dò ${step1Targets.size}/${allServers.size} đại diện vùng (thời hạn ≤5s, tối đa ${PingTester.MAX_CONCURRENT_SOCKETS} socket mở đồng thời)"
                )

                runMeasurementStepWithDeadline(
                    generationId = myGeneration,
                    targets = step1Targets,
                    stepDeadlineMs = 5000L,
                    useCache = !forceRefresh
                )

                if (myGeneration != measurementGeneration.get()) return@launch

                // Đánh giá sơ bộ các vùng có phản hồi từ Bước 1 (giữ lại ứng viên từ nhiều vùng, không loại bỏ cứng vùng khác)
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
                    .sortedBy { it.value }
                    .map { it.key }

                // BƯỚC 2 (Thời hạn <= 5 giây): Đo các ứng viên tốt từ các vùng + các mục đang hiển thị trên màn hình
                val testedIds = step1Targets.map { it.id }.toSet()
                val visibleTargets = allServers.filter { it.id in visibleServerIds && it.id !in testedIds }
                val regionalCandidates = orderedCountries.flatMap { code ->
                    allServers
                        .filter {
                            it.countryCode.equals(code, ignoreCase = true) &&
                                it.id !in testedIds &&
                                !isDynamicLiveProxy(it)
                        }
                        .take(10)
                }
                val step2Targets = (visibleTargets + regionalCandidates)
                    .distinctBy { it.id }
                    .take(24)

                if (step2Targets.isNotEmpty() && myGeneration == measurementGeneration.get()) {
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

                val currentSrvStatus = getServerUnlockStatus(currentSrv, records)
                val currentSrvPing = finalPings[currentSrv.id] ?: -1
                val currentSrvCountryTier = RegionalProbeTester.getNearestSupportedCountryTier(
                    currentSrv.countryCode,
                    allServers,
                    _state.value.detectedGeoIp
                )

                // Chỉ tự động đổi máy chủ khi người dùng bấm "Chọn tối ưu" hoặc máy chủ hiện tại bị lỗi/chưa mở khóa và chết cổng chính.
                // Không bao giờ thay máy chủ UNLOCKED đang chạy bằng máy chủ UNTESTED hoặc bằng máy chủ chỉ mở cổng phụ (:443/:80).
                val shouldAutoSwitchToOptimal = autoPickBest ||
                    (!currentSrv.isCustom && (
                        currentSrvStatus == RegionUnlockStatus.FAILED ||
                            currentSrvStatus == RegionUnlockStatus.WRONG_REGION ||
                            (currentSrvStatus != RegionUnlockStatus.UNLOCKED && (currentSrvCountryTier > 0 || currentSrvPing <= 0))
                        ))

                if (shouldAutoSwitchToOptimal) {
                    val validPool = allServers.filter {
                        val st = getServerUnlockStatus(it, records)
                        st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
                    }
                    val candidates = sortServersByUnlockAndPing(
                        servers = validPool,
                        pingMap = finalPings,
                        records = records,
                        engine = currentEngine,
                        measurementsMap = finalMeas
                    )
                    val best = candidates.firstOrNull { cand ->
                        val candMeas = finalMeas[cand.id]
                        val candPrimaryPing = if (candMeas != null) {
                            if (candMeas.isPrimaryPortAlive) candMeas.rttMs else -1
                        } else {
                            finalPings[cand.id] ?: -1
                        }
                        val candStatus = getServerUnlockStatus(cand, records)
                        val allowedByUnlockRule = if (currentSrvStatus == RegionUnlockStatus.UNLOCKED && currentSrvPing > 0) {
                            candStatus == RegionUnlockStatus.UNLOCKED
                        } else {
                            true
                        }
                        candPrimaryPing > 0 && allowedByUnlockRule
                    } ?: if (currentEngine == VpnEngine.SMART_BYPASS && currentSrvStatus != RegionUnlockStatus.UNLOCKED) {
                        candidates.firstOrNull { getLastSessionPingForServer(it) > 0 }
                            ?: candidates.firstOrNull()
                    } else {
                        null
                    }

                    if (best != null) {
                        val measuredBest = finalPings[best.id] ?: -1
                        val bestPing = if (measuredBest > 0) {
                            measuredBest
                        } else if (currentEngine == VpnEngine.SMART_BYPASS) {
                            getLastSessionPingForServer(best)
                        } else {
                            -1
                        }
                        if (best.id != currentSrv.id || autoPickBest) {
                            com.example.data.AppLogger.success(
                                "VPN",
                                "Chọn tối ưu (${best.countryCode} • ${getServerUnlockStatus(best, records)}): ${best.name} (${if (bestPing > 0) "${bestPing}ms" else "Sẵn sàng"})"
                            )
                            withContext(Dispatchers.Main) {
                                selectServer(best, context ?: appContext)
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
                }
            } catch (e: Exception) {
                Log.w(TAG, "testAllServers interrupted", e)
                if (myGeneration == measurementGeneration.get()) {
                    _state.value = _state.value.copy(isTestingPings = false)
                }
            }
        }
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

        // Lọc bỏ máy chủ đang lỗi và các máy chủ "Không mở khóa" / "Sai vùng", sắp xếp thuần theo trạng thái mở khóa & Ping thấp nhất
        val eligibleAnySupported = all.filter {
            it.addressPortKey != current.addressPortKey &&
                getServerUnlockStatus(it, records) != RegionUnlockStatus.FAILED &&
                getServerUnlockStatus(it, records) != RegionUnlockStatus.WRONG_REGION
        }

        val sortedPool = sortServersByUnlockAndPing(
            eligibleAnySupported,
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
        selectServer(bestNext, context ?: appContext)
        return bestNext
    }

    private fun loadPersistedServers(context: Context) {
        try {
            val sp = context.getSharedPreferences("cloudplay_servers", Context.MODE_PRIVATE)
            val jsonStr = sp.getString("custom_servers_json", null) ?: return
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<ServerLocation>()
            val defaultKeys = DEFAULT_SERVERS.map { "${it.ip.trim().lowercase()}:${it.port}" }.toSet()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val cc = obj.optString("countryCode", "JP")
                val ip = obj.getString("ip")
                val port = obj.getInt("port")
                val key = "${ip.trim().lowercase()}:$port"
                // Bỏ qua các bản ghi lỗi cũ có countryCode = "ALL" hoặc trùng với DEFAULT_SERVERS
                if (id.startsWith("live_all_", ignoreCase = true) ||
                    cc.equals("ALL", ignoreCase = true) ||
                    cc.equals("CUSTOM", ignoreCase = true) ||
                    key in defaultKeys
                ) {
                    continue
                }
                list.add(
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
                        speedMbps = obj.optInt("speedMbps", 200),
                        lastPingMs = null,
                        isCustom = true
                    )
                )
            }
            _state.value = _state.value.copy(customServers = list)
            val lastServerId = sp.getString("selected_server_id", null)
            if (lastServerId != null) {
                val all = list + DEFAULT_SERVERS
                val found = all.find { it.id == lastServerId }
                if (found != null) {
                    _state.value = _state.value.copy(currentServer = found)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load persisted servers", e)
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

    fun checkCurrentIp() {
        ipCheckJob?.cancel()
        ipCheckJob = scope.launch(Dispatchers.IO) {
            val result = GeoIpDetector.checkCurrentIp()
            val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
            if (result != null) {
                updateSmartDecision(result)
            } else {
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

        // Giai đoạn 1: Tự động chuyển sang quốc gia hỗ trợ Xbox gần người dùng nhất nếu server hiện tại
        // chưa xác nhận mở khóa (UNTESTED / FAILED) và đang nằm ở quốc gia xa (ví dụ người dùng ở VN nhưng server cũ là US).
        var updatedCurrentServer = _state.value.currentServer
        val records = _state.value.unlockRecords
        val currentServerStatus = getServerUnlockStatus(updatedCurrentServer, records)
        if (!currentGeo.isFromWebView && !updatedCurrentServer.isCustom && currentServerStatus != RegionUnlockStatus.UNLOCKED) {
            val allServers = getAllServers()
            val currentCountryTier = RegionalProbeTester.getNearestSupportedCountryTier(
                serverCountryCode = updatedCurrentServer.countryCode,
                servers = allServers,
                geo = currentGeo
            )
            if (currentCountryTier > 0) {
                val nearestCountryCandidates = sortServersByUnlockAndPing(
                    servers = allServers.filter {
                        val st = getServerUnlockStatus(it, records)
                        RegionalProbeTester.getNearestSupportedCountryTier(it.countryCode, allServers, currentGeo) == 0 &&
                            st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
                    }
                )
                val bestInNearestCountry = nearestCountryCandidates.firstOrNull()
                if (bestInNearestCountry != null && bestInNearestCountry.id != updatedCurrentServer.id) {
                    updatedCurrentServer = bestInNearestCountry
                    persistCustomServers(null, _state.value.customServers, bestInNearestCountry)
                    com.example.data.AppLogger.info(
                        "SmartSelect",
                        "Tự động đổi máy chủ từ ${_state.value.currentServer.countryCode} sang quốc gia hỗ trợ Xbox gần nhất (${bestInNearestCountry.countryCode}): ${bestInNearestCountry.name}"
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
        val geo = _state.value.detectedGeoIp
        if (geo != null && !geo.isFromWebView) {
            return !geo.isXboxSupported
        }
        return _state.value.smartDecision.needsVpn
    }

    /**
     * Smart VPN Lifecycle: Auto Turn ON when launching game or opening catalog
     */
    fun autoTurnOnForLaunch(context: Context) {
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
                    checkCurrentIp()
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
        checkCurrentIp()
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

    fun selectServer(server: ServerLocation, context: Context? = null) {
        val wasConnected = _state.value.mode == NetworkMode.VPN_JAPAN
        val cachedProbe = RegionalProbeTester.getCachedEstimateForServer(server)
        _state.value = _state.value.copy(
            currentServer = server,
            unlockFailPromptServer = null,
            selectedServerProbeEstimate = cachedProbe
        )
        com.example.data.AppLogger.info("VPN", "Chọn máy chủ: ${server.name} (${server.ip}:${server.port})")
        persistCustomServers(context, _state.value.customServers, server)
        preCheckProxyRegion(server)
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
        return _state.value.customServers + DEFAULT_SERVERS
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

    fun setInGameStreamActive(active: Boolean) {
        isInGameStreamActive = active
        _state.value = _state.value.copy(
            activeStreamServerId = if (active) _state.value.currentServer.id else null
        )
        com.example.data.AppLogger.info(
            "STREAM",
            if (active) "Bắt đầu phiên stream trên máy chủ: ${_state.value.currentServer.name}" else "Kết thúc phiên stream"
        )
    }

    private fun startLatencyMonitoring() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val isDirect = _state.value.mode == NetworkMode.DIRECT_NETWORK
                val currentEngine = _state.value.engine
                val liveWebRtcRtt = com.example.cloudplay.NetworkOptimizer.telemetry.value.rttMs

                if (liveWebRtcRtt > 0) {
                    // pingHistory chỉ lấy từ WebRTC khi đang chơi
                    val newHistory = (_state.value.pingHistory + liveWebRtcRtt).takeLast(16)
                    if (isDirect) {
                        _state.value = _state.value.copy(directLatencyMs = liveWebRtcRtt, pingHistory = newHistory)
                    } else {
                        _state.value = _state.value.copy(latencyMs = liveWebRtcRtt, pingHistory = newHistory)
                    }
                } else if (!isInGameStreamActive) {
                    if (isDirect) {
                        val tcpPing = PingTester.measureDirectLatency()
                        if (tcpPing > 0) {
                            _state.value = _state.value.copy(directLatencyMs = tcpPing)
                        }
                    } else if (currentEngine == VpnEngine.PROXY_TUNNEL) {
                        val srv = _state.value.currentServer
                        val tcpPing = PingTester.measureServerLatency(srv)
                        if (tcpPing > 0) {
                            val updatedMap = _state.value.serverPings.toMutableMap()
                            updatedMap[srv.id] = tcpPing
                            _state.value = _state.value.copy(latencyMs = tcpPing, serverPings = updatedMap)
                        }
                    }
                }
                delay(if (isInGameStreamActive || liveWebRtcRtt > 0) 7500L else 5000L)
            }
        }
    }
}
