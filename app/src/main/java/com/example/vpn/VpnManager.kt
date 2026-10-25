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
    private var ipCheckJob: Job? = null
    private val proxyExecutor = Executors.newSingleThreadExecutor()
    private var appContext: Context? = null

    init {
        // Initial location check at launch
        checkCurrentIp()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        loadPersistedServers(context.applicationContext)
        loadPersistedUnlockRecords(context.applicationContext)
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
                if (record.isExpired(now)) {
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

    /**
     * 4. SẮP XẾP: xếp "Đã mở khóa" lên đầu, rồi "Chưa thử" theo ping,
     * "Không mở khóa" và "Sai vùng" xuống cuối.
     */
    fun sortServersByUnlockAndPing(
        servers: List<ServerLocation>,
        pingMap: Map<String, Int> = emptyMap(),
        records: Map<String, ServerUnlockRecord> = _state.value.unlockRecords
    ): List<ServerLocation> {
        return servers.sortedWith(
            Comparator { a, b ->
                val statusA = getServerUnlockStatus(a, records)
                val statusB = getServerUnlockStatus(b, records)
                val rankA = unlockSortRank(statusA)
                val rankB = unlockSortRank(statusB)
                if (rankA != rankB) {
                    return@Comparator rankA.compareTo(rankB)
                }
                val pingA = pingMap[a.id] ?: a.lastPingMs ?: -1
                val pingB = pingMap[b.id] ?: b.lastPingMs ?: -1
                val validPingA = if (pingA > 0) pingA else Int.MAX_VALUE
                val validPingB = if (pingB > 0) pingB else Int.MAX_VALUE
                if (validPingA != validPingB) {
                    validPingA.compareTo(validPingB)
                } else {
                    b.speedMbps.compareTo(a.speedMbps)
                }
            }
        )
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
            emptyMap(),
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
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ServerLocation(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        flag = obj.optString("flag", "🌐"),
                        ip = obj.getString("ip"),
                        port = obj.getInt("port"),
                        protocol = obj.optString("protocol", "http"),
                        countryCode = obj.optString("countryCode", "JP"),
                        azureEndpoint = obj.optString("azureEndpoint", "japaneast.cloudapp.azure.com"),
                        description = obj.optString("description", "Proxy cá nhân"),
                        speedMbps = obj.optInt("speedMbps", 200),
                        lastPingMs = null,
                        isCustom = true
                    )
                )
            }
            if (list.isNotEmpty()) {
                _state.value = _state.value.copy(customServers = list)
            }
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
                if (isEn) "Auto: Keep Direct ISP (0ms VPN lag)" else "Tự động: Giữ Direct ISP (0ms lag VPN)"
            }
        )

        val nextPhase = when {
            isSupported -> SmartVpnPhase.DIRECT_SUPPORTED_REGION
            _state.value.mode == NetworkMode.VPN_JAPAN -> SmartVpnPhase.BYPASS_ACTIVE
            _state.value.mode == NetworkMode.DIRECT_NETWORK -> SmartVpnPhase.DIRECT_GAMING
            else -> SmartVpnPhase.IDLE
        }

        _state.value = _state.value.copy(
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
        return true
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
            mode = NetworkMode.DISCONNECTED,
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
     * Finds and connects to the fastest responding server in the selected country.
     * Không tự chọn máy chủ "Không mở khóa" (FAILED) hoặc "Sai vùng" (WRONG_REGION).
     */
    fun autoSelectBestServer(countryCode: String, context: Context) {
        scope.launch {
            val isEn = com.example.data.SettingsRepository.config.value.isEnglishUI()
            _state.value = _state.value.copy(
                isConnecting = true,
                statusMessage = if (isEn) "Testing and finding fastest $countryCode server..." else "Đang kiểm tra và tìm máy chủ $countryCode có tốc độ tốt nhất..."
            )

            val records = _state.value.unlockRecords
            val allInCountry = getAllServers().filter { it.countryCode.equals(countryCode, ignoreCase = true) }
            val eligible = allInCountry.filter {
                val st = getServerUnlockStatus(it, records)
                st != RegionUnlockStatus.FAILED && st != RegionUnlockStatus.WRONG_REGION
            }.ifEmpty { allInCountry.filter { getServerUnlockStatus(it, records) != RegionUnlockStatus.FAILED } }

            val tested: List<Pair<ServerLocation, Int>> = kotlinx.coroutines.coroutineScope {
                eligible.map { s ->
                    async(Dispatchers.IO) {
                        val ping = PingTester.testServerPing(s, 1200)
                        Pair(s, ping)
                    }
                }.awaitAll()
            }

            val pingMap = tested.toMap().mapKeys { it.key.id }
            val responsive = tested.filter { it.second > 0 }.map { it.first }
            val sortedCandidates = sortServersByUnlockAndPing(
                if (responsive.isNotEmpty()) responsive else eligible,
                pingMap,
                records
            )
            val best: ServerLocation? = sortedCandidates.firstOrNull()
            if (best != null) {
                val bestPing = pingMap[best.id]?.takeIf { it > 0 } ?: best.lastPingMs ?: 65
                _state.value = _state.value.copy(
                    currentServer = best,
                    statusMessage = if (isEn) "⚡ Selected best server: ${best.name} (${bestPing}ms)" else "⚡ Đã chọn máy chủ tốt nhất: ${best.name} (${bestPing}ms)"
                )
                connect(context, best)
            } else {
                _state.value = _state.value.copy(
                    isConnecting = false,
                    statusMessage = if (isEn) "⚠️ No valid unlock server found in $countryCode" else "⚠️ Không tìm thấy máy chủ mở khóa hợp lệ trong vùng $countryCode"
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
        val candidates = sortServersByUnlockAndPing(rawCandidates, emptyMap(), records)
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
            if (directPing > 0) {
                val updatedHistory = (_state.value.pingHistory + directPing).takeLast(16)
                _state.value = _state.value.copy(
                    directLatencyMs = directPing,
                    pingHistory = updatedHistory
                )
            } else {
                _state.value = _state.value.copy(directLatencyMs = null)
            }
            startLatencyMonitoring()
        }
    }

    fun selectServer(server: ServerLocation, context: Context? = null) {
        val wasConnected = _state.value.mode == NetworkMode.VPN_JAPAN
        _state.value = _state.value.copy(currentServer = server, unlockFailPromptServer = null)
        persistCustomServers(context, _state.value.customServers, server)
        preCheckProxyRegion(server)
        if (wasConnected && context != null) {
            connect(context, server)
        }
    }

    fun addCustomServer(server: ServerLocation) {
        val updated = _state.value.customServers + server
        _state.value = _state.value.copy(customServers = updated, currentServer = server, unlockFailPromptServer = null)
        persistCustomServers(appContext, updated, server)
        preCheckProxyRegion(server)
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
                    emptyMap(),
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
    }

    private fun startLatencyMonitoring() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val isDirect = _state.value.mode == NetworkMode.DIRECT_NETWORK
                // When WebRTC stream is active or user is inside CloudBrowserScreen, reuse WebRTC RTT instead of opening background TCP sockets
                val liveWebRtcRtt = com.example.cloudplay.NetworkOptimizer.telemetry.value.rttMs
                val ping = if (liveWebRtcRtt > 0) {
                    liveWebRtcRtt
                } else if (isInGameStreamActive) {
                    // Avoid opening competing TCP ping sockets while inside game session; wait for WebRTC getStats() RTT
                    -1
                } else if (isDirect) {
                    PingTester.measureDirectLatency()
                } else {
                    PingTester.measureServerLatency(_state.value.currentServer)
                }
                if (ping > 0) {
                    val newHistory = (_state.value.pingHistory + ping).takeLast(16)
                    if (isDirect) {
                        _state.value = _state.value.copy(directLatencyMs = ping, pingHistory = newHistory)
                    } else {
                        _state.value = _state.value.copy(latencyMs = ping, pingHistory = newHistory)
                    }
                }
                delay(if (isInGameStreamActive || liveWebRtcRtt > 0) 7500L else 5000L)
            }
        }
    }
}
