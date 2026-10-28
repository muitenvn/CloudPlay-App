package com.example.vpn

import androidx.compose.runtime.Immutable

enum class NetworkMode {
    VPN_JAPAN,       // Traffic routing through Japan/Target region to pass Xbox region check
    DIRECT_NETWORK,  // Traffic routing directly through user's local ISP (low ping, zero-reload)
    DISCONNECTED     // Idle / normal connection
}

enum class VpnEngine {
    PROXY_TUNNEL,    // Proxy Route: Routes traffic through verified Japan/International proxy servers with multi-server backup
    SMART_BYPASS,    // Better-xCloud Smart Header & Region Spoof (Guaranteed Japan East, 0 lag, no dead proxy)
    EXTERNAL_VPN     // External VPN Assistant: Use with Warp, Proton, Kiwi, etc. with auto-detect and zero-reload handoff
}

enum class RegionUnlockStatus(val labelVi: String, val labelEn: String) {
    UNLOCKED("Đã mở khóa", "Unlocked"),
    FAILED("Không mở khóa", "Unlock Failed"),
    UNTESTED("Chưa thử", "Untested"),
    WRONG_REGION("Sai vùng", "Wrong Region");

    fun localizedLabel(isEn: Boolean): String = if (isEn) labelEn else labelVi
}

@Immutable
data class ServerUnlockRecord(
    val serverKey: String,
    val status: RegionUnlockStatus = RegionUnlockStatus.UNTESTED,
    val timestampMs: Long = System.currentTimeMillis(),
    val detectedCountryCode: String? = null
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        return status == RegionUnlockStatus.FAILED && (nowMs - timestampMs) >= UNLOCK_FAIL_EXPIRY_MS
    }

    fun effectiveStatus(nowMs: Long = System.currentTimeMillis()): RegionUnlockStatus {
        return if (isExpired(nowMs)) RegionUnlockStatus.UNTESTED else status
    }

    companion object {
        const val UNLOCK_FAIL_EXPIRY_MS: Long = 24L * 60L * 60L * 1000L // 24 giờ

        fun keyFor(ip: String, port: Int): String = "${ip.trim()}:$port"
        fun keyFor(server: ServerLocation): String = keyFor(server.ip, server.port)
    }
}

@Immutable
data class ServerLocation(
    val id: String,
    val name: String,
    val flag: String,
    val ip: String,
    val port: Int = 8080,
    val protocol: String = "http", // "http", "socks5"
    val azureEndpoint: String = "japaneast.cloudapp.azure.com",
    val description: String = "Tokyo, Japan",
    val speedMbps: Int = 0,
    val countryCode: String = "JP",
    val isCustom: Boolean = false,
    val lastPingMs: Int? = null,
    val isVerified: Boolean = true,
    val unlockStatus: RegionUnlockStatus = RegionUnlockStatus.UNTESTED
) {
    val addressPortKey: String
        get() = ServerUnlockRecord.keyFor(ip, port)

    fun localizedDescription(isEn: Boolean): String {
        if (!isEn) return description
        return description
            .replace("Đã xác minh IP Tokyo", "Verified Tokyo IP")
            .replace("Đã xác minh IP Nhật Bản", "Verified Japan IP")
            .replace("Đã xác minh IP Osaka", "Verified Osaka IP")
            .replace("Khuyên dùng", "Recommended")
            .replace("Dự phòng", "Backup")
            .replace("Đại học Tsukuba Academic", "Tsukuba Academic Node")
            .replace("Trục cáp quang", "Fiber Backbone")
            .replace("Cụm máy chủ", "Server Cluster")
            .replace("Độ trễ thấp cho VN", "Low Latency for SEA/VN")
            .replace("Kết nối khu vực Đông Nam Á", "Southeast Asia Regional Hub")
            .replace("Đầu mối quốc tế tốc độ cao", "High-Speed International Node")
            .replace("Mạng lưới toàn cầu", "Global Edge Network")
            .replace("Cổng kết nối ổn định", "Stable Gateway")
            .replace("Máy chủ SOCKS5 do người dùng tự thêm", "User-added custom SOCKS5 proxy")
            .replace("Máy chủ HTTP do người dùng tự thêm", "User-added custom HTTP proxy")
            .replace("do người dùng tự thêm", "User-added custom proxy")
    }
}

// Danh sách 31 máy chủ chuẩn đã xác minh cổng chính mở & hoạt động ổn định (JP: 16, KR: 9, US: 6)
val DEFAULT_SERVERS = listOf(
    // === NHẬT BẢN (JAPAN) - 16 MÁY CHỦ TỐC ĐỘ CAO TOKYO / TSUKUBA / OSAKA ===
    ServerLocation(
        id = "jp_tsukuba_softether_7",
        name = "Japan Tsukuba #7",
        flag = "🇯🇵",
        ip = "219.100.37.197",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Node • AS4713 Tokyo (Khuyên dùng #1)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_softether_8",
        name = "Japan Tsukuba #8",
        flag = "🇯🇵",
        ip = "219.100.37.23",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Backbone • AS4713 Tokyo (Khuyên dùng #2)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_softether_9",
        name = "Japan Tsukuba #9",
        flag = "🇯🇵",
        ip = "219.100.37.11",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Transit • AS4713 Tokyo (Khuyên dùng #3)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tokyo_linode_1",
        name = "Japan Tokyo Fiber #1",
        flag = "🇯🇵",
        ip = "219.100.37.10",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Tokyo Academic Fiber • Đã xác minh IP Tokyo (Cổng :443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tokyo_bay_2",
        name = "Japan Tokyo Bay #2",
        flag = "🇯🇵",
        ip = "219.100.37.98",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Tokyo Bay Backbone • Đã xác minh IP Tokyo (Cổng :443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_gotemba_fiber_3",
        name = "Japan Tsukuba Core #3",
        flag = "🇯🇵",
        ip = "219.100.37.162",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Tsukuba High-Speed Core • Đã xác minh IP Nhật Bản (:443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tokyo_ali_4",
        name = "Japan Tokyo Express #4",
        flag = "🇯🇵",
        ip = "219.100.37.178",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Tokyo Express 500Mbps • Đã xác minh IP Tokyo (:443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_softether_5",
        name = "Japan Tsukuba #5",
        flag = "🇯🇵",
        ip = "219.100.37.179",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Academic • AS4713 Tokyo (Dự phòng #5)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_softether_6",
        name = "Japan Tsukuba #6",
        flag = "🇯🇵",
        ip = "219.100.37.206",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Fiber • AS4713 Tokyo (Dự phòng #6)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_osaka_west_10",
        name = "Japan Tsukuba Direct #10",
        flag = "🇯🇵",
        ip = "219.100.37.225",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Kanto Direct Relay • AS4713 Tokyo (Dự phòng #10)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tokyo_cloud_11",
        name = "Japan Tokyo CloudDC #11",
        flag = "🇯🇵",
        ip = "219.100.37.53",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Tokyo Central Cloud Backbone (Dự phòng #11)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_chiba_edge_12",
        name = "Japan Chiba Edge #12",
        flag = "🇯🇵",
        ip = "219.100.37.56",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Ibaraki / Chiba Research Gateway (Dự phòng #12)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_13",
        name = "Japan Tsukuba #13",
        flag = "🇯🇵",
        ip = "219.100.37.63",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Gateway #13 • AS4713 Tokyo (:443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_14",
        name = "Japan Tsukuba #14",
        flag = "🇯🇵",
        ip = "219.100.37.87",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Gateway #14 • AS4713 Tokyo (:443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_tsukuba_15",
        name = "Japan Tsukuba #15",
        flag = "🇯🇵",
        ip = "219.100.37.111",
        port = 443,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Đại học Tsukuba Gateway #15 • AS4713 Tokyo (:443)",
        countryCode = "JP"
    ),
    ServerLocation(
        id = "jp_softbank_16",
        name = "Japan SoftBank Fiber #16",
        flag = "🇯🇵",
        ip = "126.130.164.3",
        port = 1557,
        protocol = "http",
        azureEndpoint = "japaneast.cloudapp.azure.com",
        description = "Tokyo SoftBank BB High-Speed Relay (:1557)",
        countryCode = "JP"
    ),

    // === HÀN QUỐC (SOUTH KOREA) - 9 MÁY CHỦ TỐC ĐỘ CAO SEOUL / INCHEON / BUSAN ===
    ServerLocation(
        id = "kr_seoul_kt_1",
        name = "South Korea KT SSL #1",
        flag = "🇰🇷",
        ip = "218.149.226.154",
        port = 443,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Seoul Korea Telecom SSL Backbone • Cổng :443 (Khuyên dùng #1)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_incheon_5",
        name = "South Korea Incheon #5",
        flag = "🇰🇷",
        ip = "49.165.136.156",
        port = 1828,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Incheon Gyeonggi 700Mbps Fiber Gateway (Khuyên dùng #2)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_seoul_kt_2",
        name = "South Korea Seoul KT #2",
        flag = "🇰🇷",
        ip = "1.176.241.219",
        port = 995,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Seoul SK/KT Fiber 600Mbps Backbone (:995)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_seoul_hanaro_3",
        name = "South Korea Hanaro #3",
        flag = "🇰🇷",
        ip = "61.76.11.83",
        port = 1784,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Seoul Hanaro Telecom High-Speed Relay (:1784)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_seoul_hanaro_4",
        name = "South Korea Seoul #4",
        flag = "🇰🇷",
        ip = "222.102.95.219",
        port = 995,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Seoul Metro Optical Line (:995)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_busan_6",
        name = "South Korea Busan #6",
        flag = "🇰🇷",
        ip = "121.168.25.111",
        port = 995,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Busan South Coast Fiber Relay (:995)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_suwon_7",
        name = "South Korea Suwon #7",
        flag = "🇰🇷",
        ip = "211.192.121.223",
        port = 995,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Suwon Gyeonggi Fiber Node (:995)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_daegu_8",
        name = "South Korea Daegu #8",
        flag = "🇰🇷",
        ip = "121.142.148.62",
        port = 995,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Daegu KT Optical Relay (:995)",
        countryCode = "KR"
    ),
    ServerLocation(
        id = "kr_seoul_9",
        name = "South Korea Seoul #9",
        flag = "🇰🇷",
        ip = "118.34.210.174",
        port = 1819,
        protocol = "http",
        azureEndpoint = "koreacentral.cloudapp.azure.com",
        description = "Seoul LG U+ Direct Node (:1819)",
        countryCode = "KR"
    ),

    // === HOA KỲ (UNITED STATES) - 6 MÁY CHỦ DỰ PHÒNG ĐÃ XÁC MINH CỔNG MỞ ===
    ServerLocation(
        id = "us_california_1",
        name = "United States Silicon Valley #1",
        flag = "🇺🇸",
        ip = "156.239.242.10",
        port = 443,
        protocol = "http",
        azureEndpoint = "westus.cloudapp.azure.com",
        description = "California West Coast SSL • Cổng :443",
        countryCode = "US"
    ),
    ServerLocation(
        id = "us_west_oregon_2",
        name = "United States Oregon #2",
        flag = "🇺🇸",
        ip = "34.43.46.91",
        port = 443,
        protocol = "http",
        azureEndpoint = "westus2.cloudapp.azure.com",
        description = "Oregon Google Cloud Backbone • Cổng :443",
        countryCode = "US"
    ),
    ServerLocation(
        id = "us_central_iowa_3",
        name = "United States Central #3",
        flag = "🇺🇸",
        ip = "130.127.204.197",
        port = 8080,
        protocol = "http",
        azureEndpoint = "centralus.cloudapp.azure.com",
        description = "Clemson Research Backbone • Cổng :8080",
        countryCode = "US"
    ),
    ServerLocation(
        id = "us_virginia_4",
        name = "United States Virginia #4",
        flag = "🇺🇸",
        ip = "68.71.254.6",
        port = 4145,
        protocol = "socks5",
        azureEndpoint = "eastus.cloudapp.azure.com",
        description = "East Coast SOCKS5 Relay • Cổng :4145",
        countryCode = "US"
    ),
    ServerLocation(
        id = "us_nevada_5",
        name = "United States Nevada #5",
        flag = "🇺🇸",
        ip = "72.195.34.41",
        port = 4145,
        protocol = "socks5",
        azureEndpoint = "westus.cloudapp.azure.com",
        description = "Western Data Hub SOCKS5 • Cổng :4145",
        countryCode = "US"
    ),
    ServerLocation(
        id = "us_texas_6",
        name = "United States Texas #6",
        flag = "🇺🇸",
        ip = "72.207.113.97",
        port = 4145,
        protocol = "socks5",
        azureEndpoint = "southcentralus.cloudapp.azure.com",
        description = "South Central SOCKS5 Gateway • Cổng :4145",
        countryCode = "US"
    )
)

enum class SmartVpnPhase {
    IDLE,                      // Trạng thái chờ / kiểm tra IP
    BYPASS_ACTIVE,             // Giai đoạn 1: Đang BẬT VPN/Bypass để vượt rào vùng (duyệt catalog, mở game)
    DIRECT_GAMING,             // Giai đoạn 2: Đã TỰ ĐỘNG TẮT VPN khi vào trận (Mạng Direct ISP active - 0ms phụ trợ)
    DIRECT_SUPPORTED_REGION    // Người chơi ở sẵn khu vực hỗ trợ (Nhật, Hàn, Mỹ...), không cần bật VPN
}

@Immutable
data class SmartVpnDecision(
    val checked: Boolean = false,
    val needsVpn: Boolean = true,
    val userCountryCode: String = "VN",
    val userCountryName: String = "Việt Nam",
    val isSupportedRegion: Boolean = false,
    val message: String = "Đang kiểm tra IP người chơi...",
    val actionSummary: String = "BẬT lúc mở game ➔ TẮT khi vào trận"
)

@Immutable
data class StreamTelemetry(
    val isStreaming: Boolean = false,
    val fps: Int = 0,
    val bitrateMbps: Float = 0.0f,
    val rttMs: Int = 0,
    val jitterMs: Int = 0,
    val packetsLostPercent: Float = 0.0f,
    val framesDropped: Int = 0,
    val resolution: String = "1280x720",
    val codec: String = "H.264",
    val audioCodec: String = "Opus Stereo 48kHz",
    val qualityScore: Float = 0.0f
)

@Immutable
data class SystemBenchmarkResult(
    val isRunning: Boolean = false,
    val completed: Boolean = false,
    val dnsResolveMs: Int = 0,
    val azureTokyoPingMs: Int = 0,
    val directIspPingMs: Int = 0,
    val jitterMs: Int = 0,
    val packetLossPercent: Float = 0.0f,
    val hardwareDecoderOk: Boolean = true,
    val systemScore: Int = 0,
    val summary: String = "Chưa chạy đo kiểm đường truyền"
)

@Immutable
data class VpnConnectionState(
    val mode: NetworkMode = NetworkMode.DISCONNECTED,
    val engine: VpnEngine = VpnEngine.SMART_BYPASS,
    val isConnecting: Boolean = false,
    val currentServer: ServerLocation = DEFAULT_SERVERS[0],
    val latencyMs: Int? = null,
    val directLatencyMs: Int? = null,
    val pingHistory: List<Int> = emptyList(),
    val autoBypassTriggered: Boolean = false,
    val statusMessage: String = "Sẵn sàng kết nối",
    val lastError: String? = null,
    val detectedGeoIp: GeoIpResult? = null,
    val isBypassActiveInBrowser: Boolean = false,
    val customServers: List<ServerLocation> = emptyList(),
    val autoSwitchedBackupCount: Int = 0,
    val smartVpnPhase: SmartVpnPhase = SmartVpnPhase.IDLE,
    val smartDecision: SmartVpnDecision = SmartVpnDecision(),
    val telemetry: StreamTelemetry = StreamTelemetry(),
    val benchmark: SystemBenchmarkResult = SystemBenchmarkResult(),
    val unlockRecords: Map<String, ServerUnlockRecord> = emptyMap(),
    val unlockFailPromptServer: ServerLocation? = null,
    val serverPings: Map<String, Int> = emptyMap(),
    val serverMeasurements: Map<String, EndpointPingMeasurement> = emptyMap(),
    val selectedServerProbeEstimate: GlobalpingProbeEstimate? = null,
    val isTestingPings: Boolean = false,
    val activeStreamServerId: String? = null
)
