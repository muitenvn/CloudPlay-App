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
            if (result != null) {
                updateSmartDecision(result)
            } else {
                val offlineDecision = SmartVpnDecision(
                    checked = true,
                    needsVpn = false,
                    userCountryCode = "--",
                    userCountryName = "Không xác định / Ngoại tuyến",
                    isSupportedRegion = false,
                    message = "🌐 Không xác định được địa chỉ IP mạng (Mất kết nối hoặc chặn DNS). Bạn có thể bật Bypass hoặc chọn Server thủ công.",
                    actionSummary = "Thủ công: Chọn máy chủ theo ý muốn"
                )
                _state.value = _state.value.copy(
                    detectedGeoIp = null,
                    smartDecision = offlineDecision,
                    smartVpnPhase = SmartVpnPhase.IDLE,
                    statusMessage = "Chưa kết nối mạng / Không rõ IP"
                )
            }
        }
    }

    fun updateSmartDecision(geo: GeoIpResult?) {
        val currentGeo = geo ?: _state.value.detectedGeoIp
        if (currentGeo == null) {
            val offlineDecision = SmartVpnDecision(
                checked = true,
                needsVpn = false,
                userCountryCode = "--",
                userCountryName = "Không xác định / Ngoại tuyến",
                isSupportedRegion = false,
                message = "🌐 Chưa xác định được IP mạng. Bạn có thể bật Bypass hoặc chọn Server thủ công.",
                actionSummary = "Thủ công: Chọn máy chủ theo ý muốn"
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
                "⚠️ Phát hiện IP ${currentGeo.ip} tại ${currentGeo.countryName} ${currentGeo.flagEmoji} (Chưa hỗ trợ xCloud). Smart VPN sẽ TỰ ĐỘNG BẬT khi mở game & TỰ ĐỘNG TẮT khi vào trận!"
            } else {
                "✅ Phát hiện IP ${currentGeo.ip} tại ${currentGeo.countryName} ${currentGeo.flagEmoji} ĐÃ HỖ TRỢ xCloud. KHÔNG CẦN BẬT VPN! Luôn giữ Direct ISP để đạt Ping thấp nhất."
            },
            actionSummary = if (needsVpn) {
                "Tự động: BẬT lúc mở game ➔ TẮT khi vào trận"
            } else {
                "Tự động: Giữ Direct ISP (0ms lag VPN)"
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
                "✅ IP hợp lệ: ${currentGeo.countryName} ${currentGeo.flagEmoji} • Không cần VPN (Mạng Trực Tiếp)"
            } else {
                "🤖 Smart VPN sẵn sàng: Tự động Bật/Tắt cho IP ${currentGeo.countryName} ${currentGeo.flagEmoji}"
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
        if (_state.value.mode != NetworkMode.VPN_JAPAN) {
            connect(context, _state.value.currentServer)
            _state.value = _state.value.copy(
                mode = NetworkMode.VPN_JAPAN,
                autoBypassTriggered = false,
                smartVpnPhase = SmartVpnPhase.BYPASS_ACTIVE,
                statusMessage = "🤖 Smart VPN: Đã TỰ ĐỘNG BẬT để mở khóa vùng vào game..."
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
        _state.value = _state.value.copy(
            mode = NetworkMode.DIRECT_NETWORK,
            smartVpnPhase = SmartVpnPhase.DIRECT_SUPPORTED_REGION,
            statusMessage = "⚡ Smart VPN: Vị trí hợp lệ, giữ Mạng Trực Tiếp"
        )
    }

    fun setSmartVpnPhase(phase: SmartVpnPhase) {
        _state.value = _state.value.copy(smartVpnPhase = phase)
    }

    fun onWebViewIpDetected(ip: String, countryCode: String, countryName: String? = null, city: String? = null) {
        GeoIpDetector.onWebViewReported(ip, countryCode, countryName, city)
        val currentGeo = GeoIpDetector.currentGeoIp.value
        updateSmartDecision(currentGeo)
    }

    fun onRegionBypassed(info: String) {
        Log.d(TAG, "Xbox Region bypassed via In-WebView Interceptor: $info")
        val currentServer = _state.value.currentServer
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
            detectedGeoIp = spoofGeo,
            smartVpnPhase = SmartVpnPhase.BYPASS_ACTIVE,
            statusMessage = "✅ Đã vượt rào Xbox thành công! Khu vực: ${currentServer.name} ${currentServer.flag}"
        )
    }

    fun connect(context: Context, server: ServerLocation = _state.value.currentServer) {
        _state.value = _state.value.copy(
            isConnecting = true,
            currentServer = server,
            lastError = null,
            statusMessage = "Đang kết nối tới ${server.name} ${server.flag}..."
        )

        scope.launch {
            when (_state.value.engine) {
                VpnEngine.PROXY_TUNNEL -> {
                    // 1. Verify reachability of selected server with fast timeout
                    val isAlive = PingTester.isHostReachable(server.ip, server.port, 1000)
                    var targetServer = server

                    if (!isAlive && !server.isCustom) {
                        Log.w(TAG, "Server ${server.name} unreachable, finding best working backup...")
                        _state.value = _state.value.copy(
                            statusMessage = "🔄 Đang kiểm tra đường truyền proxy..."
                        )

                        val workingBackup = findFirstResponsiveServer(server.countryCode)
                        if (workingBackup != null) {
                            targetServer = workingBackup
                            _state.value = _state.value.copy(
                                currentServer = targetServer,
                                autoSwitchedBackupCount = _state.value.autoSwitchedBackupCount + 1,
                                statusMessage = "✅ Đã chọn máy chủ: ${targetServer.name}"
                            )
                        } else {
                            // If proxy port is unreachable on real device / carrier firewall, fallback to Smart Bypass immediately
                            Log.w(TAG, "Proxy ports blocked or unreachable. Auto-falling back to Smart Bypass mode.")
                            _state.value = _state.value.copy(
                                engine = VpnEngine.SMART_BYPASS,
                                statusMessage = "⚡ Tự động chuyển Smart Bypass (chống nghẽn mạng)"
                            )
                            clearWebViewProxy()
                        }
                    }

                    // 2. Apply multi-server proxy rules with automatic fallbacks
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
                        statusMessage = "✅ Đã kích hoạt Proxy: ${targetServer.name} (${targetServer.ip}:${targetServer.port})"
                    )
                }

                VpnEngine.SMART_BYPASS -> {
                    // Smart Bypass relies on Better-xCloud header and region spoofing inside WebView
                    // Clear proxy to ensure fastest possible loading without dead proxy hangs
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

                    // Start foreground VpnService so Android system tray displays active status
                    XboxCloudVpnService.startVpn(context, server)

                    _state.value = _state.value.copy(
                        mode = NetworkMode.VPN_JAPAN,
                        isConnecting = false,
                        autoBypassTriggered = false,
                        isBypassActiveInBrowser = true,
                        detectedGeoIp = targetGeo,
                        statusMessage = "✅ Đã kích hoạt Smart Bypass • ${server.name} (${server.ip})"
                    )
                }

                VpnEngine.EXTERNAL_VPN -> {
                    clearWebViewProxy()
                    XboxCloudVpnService.stopVpn(context)
                    _state.value = _state.value.copy(
                        mode = NetworkMode.VPN_JAPAN,
                        isConnecting = false,
                        autoBypassTriggered = false,
                        statusMessage = "Chế độ VPN ngoài: Sẵn sàng tự động chuyển mạng khi vào game"
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
        _state.value = _state.value.copy(
            mode = NetworkMode.DISCONNECTED,
            isConnecting = false,
            isBypassActiveInBrowser = false,
            smartVpnPhase = if (needsVpnToPlay()) SmartVpnPhase.IDLE else SmartVpnPhase.DIRECT_SUPPORTED_REGION,
            statusMessage = "Đã ngắt kết nối"
        )
        checkCurrentIp()
    }

    /**
     * Cycles to the next backup server in the same country if current server fails or loads slowly
     */
    fun switchNextBackupServer(context: Context) {
        val allServers = getAllServers()
        val sameCountryServers = allServers.filter { it.countryCode.equals(_state.value.currentServer.countryCode, ignoreCase = true) }
        val pool = if (sameCountryServers.isNotEmpty()) sameCountryServers else allServers

        val currentIndex = pool.indexOfFirst { it.id == _state.value.currentServer.id }
        val nextIndex = if (currentIndex in 0 until pool.size - 1) currentIndex + 1 else 0
        val nextServer = pool[nextIndex]

        _state.value = _state.value.copy(
            currentServer = nextServer,
            statusMessage = "🔄 Chuyển sang máy chủ dự phòng #${nextIndex + 1}: ${nextServer.name}"
        )
        selectServer(nextServer, context)
    }

    /**
     * Finds and connects to the fastest responding server in the selected country
     */
    fun autoSelectBestServer(countryCode: String, context: Context) {
        scope.launch {
            _state.value = _state.value.copy(
                isConnecting = true,
                statusMessage = "Đang kiểm tra và tìm máy chủ $countryCode có tốc độ tốt nhất..."
            )

            val servers = getAllServers().filter { it.countryCode.equals(countryCode, ignoreCase = true) }
            val tested: List<Pair<ServerLocation, Int>> = kotlinx.coroutines.coroutineScope {
                servers.map { s ->
                    async(Dispatchers.IO) {
                        val ping = PingTester.testServerPing(s, 1200)
                        Pair(s, ping)
                    }
                }.awaitAll()
            }

            val best: ServerLocation? = tested.filter { it.second > 0 }.minByOrNull { it.second }?.first ?: servers.firstOrNull()
            if (best != null) {
                _state.value = _state.value.copy(
                    currentServer = best,
                    statusMessage = "⚡ Đã chọn máy chủ tốt nhất: ${best.name} (${best.lastPingMs ?: 65}ms)"
                )
                connect(context, best)
            } else {
                _state.value = _state.value.copy(
                    isConnecting = false,
                    statusMessage = "⚠️ Không tìm thấy máy chủ phản hồi trong vùng $countryCode"
                )
            }
        }
    }

    private suspend fun findFirstResponsiveServer(countryCode: String): ServerLocation? = withContext(Dispatchers.IO) {
        val candidates = getAllServers().filter { it.countryCode.equals(countryCode, ignoreCase = true) }
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

        clearWebViewProxy()
        XboxCloudVpnService.stopVpn(context)

        _state.value = _state.value.copy(
            mode = NetworkMode.DIRECT_NETWORK,
            isConnecting = false,
            autoBypassTriggered = true,
            smartVpnPhase = SmartVpnPhase.DIRECT_GAMING,
            statusMessage = "⚡ Smart VPN: Đã TỰ ĐỘNG TẮT VPN ($triggerSource)! Đang truyền trực tiếp qua mạng ISP (Ping thấp nhất)."
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
        _state.value = _state.value.copy(currentServer = server)
        persistCustomServers(context, _state.value.customServers, server)
        if (wasConnected && context != null) {
            connect(context, server)
        }
    }

    fun addCustomServer(server: ServerLocation) {
        val updated = _state.value.customServers + server
        _state.value = _state.value.copy(customServers = updated, currentServer = server)
        persistCustomServers(appContext, updated, server)
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

                // Add up to 3 backup servers from the same region for auto-fallback in Chromium
                val backups = getAllServers()
                    .filter { it.id != primaryServer.id && it.countryCode == primaryServer.countryCode }
                    .take(3)

                backups.forEach { backup ->
                    builder.addProxyRule(ruleFor(backup))
                }

                val proxyConfig = builder.build()
                ProxyController.getInstance().setProxyOverride(proxyConfig, proxyExecutor) {
                    Log.d(TAG, "WebView proxy applied: ${ruleFor(primaryServer)} with ${backups.size} fallbacks")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply WebView proxy", e)
            }
        }
    }

    private fun clearWebViewProxy() {
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

    private fun startLatencyMonitoring() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val isDirect = _state.value.mode == NetworkMode.DIRECT_NETWORK
                val ping = if (isDirect) {
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
                delay(4000)
            }
        }
    }
}
