package com.example.vpn

import com.example.data.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class ProbeStation(
    val id: String,
    val displayName: String,
    val countryCode: String,
    val apiCity: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Hệ thống định vị Quốc gia hỗ trợ Xbox gần người dùng nhất & Đo ping qua Trạm Test (Probe) khu vực gần nhất.
 * - Tự động tìm quốc gia được hỗ trợ Xbox gần vị trí người dùng nhất (VD: ở Việt Nam -> Nhật Bản / Hàn Quốc, không chọn nhầm Mỹ).
 * - Chỉ sử dụng trong mục Máy chủ (Servers tab) để đo ping và xếp hạng máy chủ tối ưu.
 * - KHÔNG sử dụng để đo ping khi đang chơi game (ping trong game dùng 100% WebRTC RTT).
 */
object RegionalProbeTester {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val probeHttpClient = OkHttpClient.Builder()
        .connectTimeout(1600, TimeUnit.MILLISECONDS)
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .writeTimeout(1600, TimeUnit.MILLISECONDS)
        .build()

    // Giới hạn số yêu cầu đo qua API trạm test đồng thời để tránh vượt Rate Limit
    private val remoteProbeSemaphore = Semaphore(4)

    // Bộ nhớ đệm ngắn hạn (45 giây) cho kết quả đo từ Trạm Test tới IP:Port
    private data class CachedProbePing(val rttMs: Int, val timestampMs: Long, val probeName: String)
    private val probeCache = ConcurrentHashMap<String, CachedProbePing>()
    private const val CACHE_TTL_MS = 45_000L

    // Ngắt mạch tạm thời (30 giây) nếu dịch vụ API trạm đo toàn cầu bị chặn mạng hoặc hết hạn mức (HTTP 429)
    private val remoteApiCooldownUntilMs = AtomicLong(0L)
    private val failedProbeIdsUntilMs = ConcurrentHashMap<String, Long>()

    @Volatile
    private var lastLoggedPrimaryProbeId: String? = null

    val PROBE_STATIONS = listOf(
        // Việt Nam (3 miền: Nam - Bắc - Trung)
        ProbeStation("vn_hcm", "TP. Hồ Chí Minh (VN)", "VN", "Ho Chi Minh City", 10.8231, 106.6297),
        ProbeStation("vn_han", "Hà Nội (VN)", "VN", "Hanoi", 21.0285, 105.8542),
        ProbeStation("vn_dad", "Đà Nẵng (VN)", "VN", "Da Nang", 16.0544, 108.2022),

        // Đông Nam Á & Lân cận
        ProbeStation("sg_sin", "Singapore (SG)", "SG", "Singapore", 1.3521, 103.8198),
        ProbeStation("th_bkk", "Bangkok (TH)", "TH", "Bangkok", 13.7563, 100.5018),
        ProbeStation("hk_hkg", "Hong Kong (HK)", "HK", "Hong Kong", 22.3193, 114.1694),
        ProbeStation("tw_tpe", "Đài Bắc (TW)", "TW", "Taipei", 25.0330, 121.5654),
        ProbeStation("my_kul", "Kuala Lumpur (MY)", "MY", "Kuala Lumpur", 3.1390, 101.6869),
        ProbeStation("ph_mnl", "Manila (PH)", "PH", "Manila", 14.5995, 120.9842),
        ProbeStation("id_jkt", "Jakarta (ID)", "ID", "Jakarta", -6.2088, 106.8456),

        // Đông Bắc Á (Vùng Xbox Cloud chính cho Châu Á)
        ProbeStation("jp_tyo", "Tokyo (JP)", "JP", "Tokyo", 35.6762, 139.6503),
        ProbeStation("jp_osa", "Osaka (JP)", "JP", "Osaka", 34.6937, 135.5023),
        ProbeStation("kr_sel", "Seoul (KR)", "KR", "Seoul", 37.5665, 126.9780),

        // Nam Á & Châu Đại Dương
        ProbeStation("in_bom", "Mumbai (IN)", "IN", "Mumbai", 19.0760, 72.8777),
        ProbeStation("au_syd", "Sydney (AU)", "AU", "Sydney", -33.8688, 151.2093),

        // Châu Âu
        ProbeStation("de_fra", "Frankfurt (DE)", "DE", "Frankfurt", 50.1109, 8.6821),
        ProbeStation("gb_lon", "London (GB)", "GB", "London", 51.5074, -0.1278),
        ProbeStation("fr_par", "Paris (FR)", "FR", "Paris", 48.8566, 2.3522),
        ProbeStation("nl_ams", "Amsterdam (NL)", "NL", "Amsterdam", 52.3676, 4.9041),

        // Châu Mỹ
        ProbeStation("us_sjc", "Silicon Valley (US)", "US", "San Jose", 37.3382, -121.8863),
        ProbeStation("us_sea", "Seattle (US)", "US", "Seattle", 47.6062, -122.3321),
        ProbeStation("us_iad", "Virginia (US)", "US", "Ashburn", 39.0438, -77.4874),
        ProbeStation("br_gru", "São Paulo (BR)", "BR", "Sao Paulo", -23.5505, -46.6333)
    )

    // Tọa độ tâm tham chiếu cho các quốc gia
    private val COUNTRY_REFERENCE_COORDS = mapOf(
        "VN" to Pair(10.8231, 106.6297),
        "LA" to Pair(17.9757, 102.6331),
        "KH" to Pair(11.5564, 104.9282),
        "MM" to Pair(16.8409, 96.1735),
        "BN" to Pair(4.9031, 114.9398),
        "SG" to Pair(1.3521, 103.8198),
        "TH" to Pair(13.7563, 100.5018),
        "MY" to Pair(3.1390, 101.6869),
        "ID" to Pair(-6.2088, 106.8456),
        "PH" to Pair(14.5995, 120.9842),
        "HK" to Pair(22.3193, 114.1694),
        "MO" to Pair(22.1987, 113.5439),
        "TW" to Pair(25.0330, 121.5654),
        "CN" to Pair(31.2304, 121.4737),
        "JP" to Pair(35.6762, 139.6503),
        "KR" to Pair(37.5665, 126.9780),
        "IN" to Pair(19.0760, 72.8777),
        "AU" to Pair(-33.8688, 151.2093),
        "NZ" to Pair(-36.8485, 174.7633),
        "DE" to Pair(50.1109, 8.6821),
        "GB" to Pair(51.5074, -0.1278),
        "UK" to Pair(51.5074, -0.1278),
        "FR" to Pair(48.8566, 2.3522),
        "NL" to Pair(52.3676, 4.9041),
        "PL" to Pair(52.2297, 21.0122),
        "SE" to Pair(59.3293, 18.0686),
        "NO" to Pair(59.9139, 10.7522),
        "DK" to Pair(55.6761, 12.5683),
        "FI" to Pair(60.1699, 24.9384),
        "US" to Pair(37.3382, -121.8863),
        "CA" to Pair(49.2827, -123.1207),
        "MX" to Pair(19.4326, -99.1332),
        "BR" to Pair(-23.5505, -46.6333)
    )

    /**
     * Tính khoảng cách đường chim bay Haversine (km) giữa 2 tọa độ địa lý.
     */
    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Bán kính Trái Đất (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Trả về tọa độ thực tế (hoặc tọa độ tâm vùng) của người dùng.
     */
    fun resolveUserCoordinates(geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp): Pair<Double, Double> {
        val realGeo = if (geo != null && !geo.isFromWebView) {
            geo
        } else {
            GeoIpDetector.currentGeoIp.value?.takeIf { !it.isFromWebView } ?: geo
        }
        val country = (realGeo?.countryCode ?: "VN").trim().uppercase()
        val cityLower = (realGeo?.city ?: "").trim().lowercase()

        return when {
            realGeo?.latitude != null && realGeo.longitude != null -> {
                Pair(realGeo.latitude, realGeo.longitude)
            }
            country == "VN" -> {
                when {
                    cityLower.contains("hanoi") || cityLower.contains("ha noi") ||
                        cityLower.contains("hà nội") || cityLower.contains("hai phong") ||
                        cityLower.contains("hải phòng") || cityLower.contains("quang ninh") ||
                        cityLower.contains("bac ninh") || cityLower.contains("nam dinh") ||
                        cityLower.contains("thai nguyen") || cityLower.contains("vinh") -> {
                        Pair(21.0285, 105.8542) // Miền Bắc -> Hà Nội
                    }
                    cityLower.contains("da nang") || cityLower.contains("đà nẵng") ||
                        cityLower.contains("hue") || cityLower.contains("huế") ||
                        cityLower.contains("quang nam") || cityLower.contains("quy nhon") -> {
                        Pair(16.0544, 108.2022) // Miền Trung -> Đà Nẵng
                    }
                    else -> {
                        Pair(10.8231, 106.6297) // Miền Nam / mặc định VN -> TP. Hồ Chí Minh
                    }
                }
            }
            else -> {
                COUNTRY_REFERENCE_COORDS[country] ?: Pair(10.8231, 106.6297)
            }
        }
    }

    /**
     * Tính khoảng cách vật lý (km) từ vị trí người dùng tới một máy chủ hoặc quốc gia đích.
     */
    fun getDistanceToServerKm(
        server: ServerLocation,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): Double {
        val (userLat, userLon) = resolveUserCoordinates(geo)
        val (srvLat, srvLon) = resolveServerCoordinates(server)
        return haversineDistanceKm(userLat, userLon, srvLat, srvLon)
    }

    /**
     * Tọa độ trung tâm dữ liệu thực tế của từng máy chủ theo vùng / thành phố.
     */
    fun resolveServerCoordinates(server: ServerLocation): Pair<Double, Double> {
        val nameLower = (server.name + " " + server.description + " " + server.azureEndpoint).lowercase()
        val code = server.countryCode.trim().uppercase()
        return when {
            code == "JP" -> when {
                nameLower.contains("osaka") || nameLower.contains("japanwest") -> Pair(34.6937, 135.5023)
                nameLower.contains("gotemba") || nameLower.contains("shizuoka") -> Pair(35.3086, 138.9344)
                nameLower.contains("tsukuba") || nameLower.contains("chiba") -> Pair(36.0835, 140.0764)
                else -> Pair(35.6762, 139.6503) // Tokyo
            }
            code == "KR" -> when {
                nameLower.contains("busan") -> Pair(35.1796, 129.0756)
                nameLower.contains("incheon") -> Pair(37.4563, 126.7052)
                else -> Pair(37.5665, 126.9780) // Seoul
            }
            code == "US" -> when {
                nameLower.contains("virginia") || nameLower.contains("eastus") -> Pair(39.0438, -77.4874)
                nameLower.contains("iowa") || nameLower.contains("centralus") -> Pair(41.5868, -93.6250)
                nameLower.contains("oregon") || nameLower.contains("westus2") -> Pair(45.5152, -122.6784)
                nameLower.contains("nevada") || nameLower.contains("vegas") -> Pair(36.1699, -115.1398)
                else -> Pair(37.3382, -121.8863) // Silicon Valley / California
            }
            else -> COUNTRY_REFERENCE_COORDS[code] ?: Pair(35.6762, 139.6503)
        }
    }

    /**
     * Tự động tìm danh sách các Quốc gia được hỗ trợ Xbox (có trong pool máy chủ)
     * xếp theo khoảng cách địa lý gần người dùng nhất.
     * Ví dụ: Người dùng ở Việt Nam -> ["JP", "KR", "US"] (hoặc ["KR", "JP", "US"]).
     * Người dùng ở Mỹ -> ["US", "JP", "KR"].
     */
    fun getNearestSupportedCountriesForUser(
        servers: List<ServerLocation> = DEFAULT_SERVERS,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): List<String> {
        val (userLat, userLon) = resolveUserCoordinates(geo)
        val distinctCountries = servers
            .map { it.countryCode.trim().uppercase() }
            .filter { it.isNotBlank() && GeoIpDetector.SUPPORTED_COUNTRIES.contains(it) }
            .distinct()

        return distinctCountries.sortedBy { code ->
            val (cLat, cLon) = COUNTRY_REFERENCE_COORDS[code] ?: Pair(35.6762, 139.6503)
            haversineDistanceKm(userLat, userLon, cLat, cLon)
        }
    }

    /**
     * Phân tầng khoảng cách Quốc gia hỗ trợ Xbox so với người dùng (Nearest Country Tier):
     * - Tầng 0 (Khu vực Xbox gần người dùng nhất): cách quốc gia hỗ trợ gần nhất không quá 1.800 km
     *   (VD: khi ở Việt Nam, cả Nhật Bản JP và Hàn Quốc KR đều nằm ở Tầng 0 — khu vực Đông Á gần nhất).
     * - Tầng 1 (Khu vực tầm trung): cách quốc gia gần nhất từ 1.800 km đến 5.500 km.
     * - Tầng 2 (Khu vực xuyên lục địa / quá xa): cách quốc gia gần nhất > 5.500 km
     *   (VD: Mỹ US cách Việt Nam ~12.500 km nằm ở Tầng 2, đảm bảo app KHÔNG BAO GIỜ chọn nhầm Mỹ khi đang ở Việt Nam).
     */
    fun getNearestSupportedCountryTier(
        serverCountryCode: String,
        servers: List<ServerLocation> = DEFAULT_SERVERS,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): Int {
        val code = serverCountryCode.trim().uppercase()
        if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(code)) return 3

        val (userLat, userLon) = resolveUserCoordinates(geo)
        val supportedCodes = servers
            .map { it.countryCode.trim().uppercase() }
            .filter { GeoIpDetector.SUPPORTED_COUNTRIES.contains(it) }
            .distinct()
            .ifEmpty { listOf("JP", "KR", "US") }

        val minCountryDistKm = supportedCodes.minOfOrNull { c ->
            val (lat, lon) = COUNTRY_REFERENCE_COORDS[c] ?: Pair(35.6762, 139.6503)
            haversineDistanceKm(userLat, userLon, lat, lon)
        } ?: 0.0

        val (targetLat, targetLon) = COUNTRY_REFERENCE_COORDS[code] ?: Pair(35.6762, 139.6503)
        val targetDistKm = haversineDistanceKm(userLat, userLon, targetLat, targetLon)
        val deltaKm = (targetDistKm - minCountryDistKm).coerceAtLeast(0.0)

        return when {
            deltaKm <= 1800.0 -> 0 // Quốc gia hỗ trợ Xbox gần người dùng nhất (VD: JP & KR khi ở VN)
            deltaKm <= 5500.0 -> 1 // Quốc gia tầm trung
            else -> 2              // Quốc gia quá xa / xuyên đại dương (VD: US khi ở VN)
        }
    }

    /**
     * Tính ngưỡng Ping vật lý tối thiểu (Speed-of-Light Fiber RTT Floor) từ người dùng tới máy chủ.
     * Ngăn chặn triệt để hiện tượng IP máy chủ Mỹ đi qua Cloudflare/Anycast tại Singapore trả về ping ảo 25–40ms
     * khiến app chọn nhầm máy chủ Mỹ khi người dùng đang ở Việt Nam.
     */
    fun computeMinimumPhysicalRttMs(
        server: ServerLocation,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): Int {
        val distKm = getDistanceToServerKm(server, geo)
        // Tốc độ truyền dẫn thực tế trong cáp quang biển (kể cả độ vòng tuyến cáp ~1.35x): ~110 km / 1ms RTT khứ hồi + 12ms chặng nội hạt
        val fiberFloorMs = (distKm / 95.0).roundToInt() + 12
        return fiberFloorMs.coerceIn(8, 260)
    }

    /**
     * Hiệu chuẩn số ping đo được với khoảng cách vật lý thực tế:
     * Nếu một máy chủ ở xa (như Mỹ cách VN 12.500km) trả về TCP handshake < ngưỡng vật lý (do Anycast/Cloudflare trả lời hộ ở trạm gần),
     * cộng bù độ trễ đường truyền xuyên lục địa tới trung tâm dữ liệu thực tế của máy chủ đó.
     */
    fun calibrateMeasuredRttWithPhysicalDistance(
        rawRttMs: Int,
        server: ServerLocation,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): Int {
        if (rawRttMs <= 0) return -1
        val minPhysicalMs = computeMinimumPhysicalRttMs(server, geo)
        return if (rawRttMs < minPhysicalMs) {
            // Gói tin TCP bị Anycast kết thúc sớm ở trạm gần -> cộng độ trễ chặng cáp quang tới vùng thực của server
            val anycastLocalRtt = rawRttMs.coerceIn(5, 60)
            (minPhysicalMs + (anycastLocalRtt / 3)).coerceAtMost(320)
        } else {
            rawRttMs
        }
    }

    /**
     * Xác định danh sách Trạm Test được sắp xếp từ gần người dùng nhất tới xa hơn
     * dựa trên GeoIP thực tế (tọa độ vĩ độ/kinh độ, tên thành phố, hoặc mã quốc gia).
     */
    fun selectOrderedProbes(geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp): List<ProbeStation> {
        val realGeo = if (geo != null && !geo.isFromWebView) geo else GeoIpDetector.currentGeoIp.value
        val country = (realGeo?.countryCode ?: "VN").trim().uppercase()
        val cityLower = (realGeo?.city ?: "").trim().lowercase()
        val (userLat, userLon) = resolveUserCoordinates(geo)

        val sorted = PROBE_STATIONS.sortedWith(
            Comparator { a, b ->
                val distA = haversineDistanceKm(userLat, userLon, a.latitude, a.longitude)
                val distB = haversineDistanceKm(userLat, userLon, b.latitude, b.longitude)
                val effectiveDistA = if (a.countryCode.equals(country, ignoreCase = true)) distA * 0.85 else distA
                val effectiveDistB = if (b.countryCode.equals(country, ignoreCase = true)) distB * 0.85 else distB
                effectiveDistA.compareTo(effectiveDistB)
            }
        )

        val primary = sorted.firstOrNull()
        if (primary != null && primary.id != lastLoggedPrimaryProbeId) {
            lastLoggedPrimaryProbeId = primary.id
            val nearestXboxCountries = getNearestSupportedCountriesForUser(DEFAULT_SERVERS, geo).take(2).joinToString(", ")
            val backupNames = sorted.drop(1).take(2).joinToString(" -> ") { it.displayName }
            AppLogger.info(
                "PROBE",
                "Vị trí: $country ${if (cityLower.isNotBlank()) "($cityLower)" else ""} • " +
                    "Quốc gia Xbox gần nhất: [$nearestXboxCountries] • " +
                    "Trạm đo: ${primary.displayName} (Dự phòng: $backupNames)"
            )
        }
        return sorted
    }

    /**
     * Đo ping cho máy chủ trong mục Máy chủ theo đúng thứ tự:
     * 1. Trạm Test gần nhất (chỉ lấy nguyên số ping từ trạm test tới server, có kiểm tra ngưỡng vật lý chống Anycast ảo).
     * 2. Nếu trạm gần nhất bận/lỗi -> tự động thử trạm lân cận kế tiếp (không hiện thông báo trên UI, chỉ ghi Log).
     * 3. Nếu các trạm từ xa không đo được cổng riêng của server -> trả về null để PingTester đo qua fallback đa cổng / cụm vùng.
     */
    suspend fun measureServerViaNearestProbe(
        server: ServerLocation,
        timeoutMs: Int = 2500
    ): Int? {
        if (server.ip.isBlank() || server.port <= 0) return null
        val now = System.currentTimeMillis()
        if (now < remoteApiCooldownUntilMs.get()) {
            return null
        }

        val candidateProbes = selectOrderedProbes().take(2)
        if (candidateProbes.isEmpty()) return null

        for ((index, probe) in candidateProbes.withIndex()) {
            val probeCooldown = failedProbeIdsUntilMs[probe.id] ?: 0L
            if (System.currentTimeMillis() < probeCooldown) {
                continue
            }

            val cacheKey = "${probe.id}:${server.ip}:${server.port}"
            val cached = probeCache[cacheKey]
            if (cached != null && (System.currentTimeMillis() - cached.timestampMs) < CACHE_TTL_MS) {
                return cached.rttMs
            }

            val rawRtt = remoteProbeSemaphore.withPermit {
                requestGlobalpingRtt(probe, server, timeoutMs)
            }

            if (rawRtt != null && rawRtt > 0) {
                val calibratedRtt = calibrateMeasuredRttWithPhysicalDistance(rawRtt, server)
                probeCache[cacheKey] = CachedProbePing(
                    rttMs = calibratedRtt,
                    timestampMs = System.currentTimeMillis(),
                    probeName = probe.displayName
                )
                AppLogger.success(
                    "PROBE",
                    "Trạm ${probe.displayName} -> ${server.name} (${server.countryCode}): ${calibratedRtt}ms"
                )
                return calibratedRtt
            } else {
                // Nếu trạm đo hiện tại không lấy được RTT (do bận hoặc cổng TCP phụ bị chặn), thử trạm kế tiếp hoặc chuyển về PingTester đa cổng
                if (rawRtt == null) {
                    failedProbeIdsUntilMs[probe.id] = System.currentTimeMillis() + 20_000L
                    val nextProbe = candidateProbes.getOrNull(index + 1)
                    if (nextProbe != null) {
                        AppLogger.warn(
                            "PROBE",
                            "Trạm ${probe.displayName} bận -> Chuyển ngầm sang trạm lân cận: ${nextProbe.displayName}"
                        )
                    }
                } else {
                    // rawRtt == -2: cổng riêng bị chặn trên probe -> thoát về PingTester để thử cổng 443/80 hoặc hạ tầng vùng
                    return null
                }
            }
        }

        return null
    }

    /**
     * Gọi Globalping API để đo RTT từ [probe] tới [server] (sử dụng ICMP ping để không bị chặn bởi cổng proxy tùy chỉnh).
     */
    private suspend fun requestGlobalpingRtt(
        probe: ProbeStation,
        server: ServerLocation,
        timeoutMs: Int
    ): Int? {
        return try {
            val payload = JSONObject().apply {
                put("type", "ping")
                put("target", server.ip.trim())
                put("limit", 1)
                put(
                    "locations",
                    JSONArray().put(
                        JSONObject().apply {
                            put("country", probe.countryCode)
                            put("city", probe.apiCity)
                        }
                    )
                )
                put(
                    "measurementOptions",
                    JSONObject().apply {
                        put("packets", 2)
                    }
                )
            }

            val postReq = Request.Builder()
                .url("https://api.globalping.io/v1/measurements")
                .header("User-Agent", "CloudPlay-ProbeClient/1.0")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val postResp = probeHttpClient.newCall(postReq).execute()
            val code = postResp.code
            val bodyStr = postResp.body?.string() ?: ""
            postResp.close()

            if (code == 429 || code >= 500) {
                remoteApiCooldownUntilMs.set(System.currentTimeMillis() + 30_000L)
                AppLogger.warn("PROBE", "API trạm đo báo mã $code -> Chuyển ngầm sang đo trực tiếp trên máy")
                return null
            }
            if (code == 422 || !postResp.isSuccessful) {
                return null
            }

            val postJson = JSONObject(bodyStr)
            val measurementId = postJson.optString("id", "")
            if (measurementId.isBlank()) return null

            val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(1200, 2600)
            while (System.currentTimeMillis() < deadline) {
                delay(300)
                val getReq = Request.Builder()
                    .url("https://api.globalping.io/v1/measurements/$measurementId")
                    .header("User-Agent", "CloudPlay-ProbeClient/1.0")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val getResp = probeHttpClient.newCall(getReq).execute()
                val getBody = getResp.body?.string() ?: ""
                val ok = getResp.isSuccessful
                getResp.close()
                if (!ok) return null

                val resJson = JSONObject(getBody)
                val status = resJson.optString("status", "in-progress")
                if (status != "in-progress") {
                    val resultsArr = resJson.optJSONArray("results") ?: return null
                    if (resultsArr.length() == 0) return null
                    val firstResult = resultsArr.getJSONObject(0)
                    val resultObj = firstResult.optJSONObject("result") ?: return null
                    val resultStatus = resultObj.optString("status", "")
                    if (resultStatus == "failed" || resultStatus == "offline") {
                        return null
                    }
                    val stats = resultObj.optJSONObject("stats") ?: return -2
                    val avg = stats.optDouble("avg", -1.0)
                    val min = stats.optDouble("min", -1.0)
                    return when {
                        avg > 0.0 -> avg.toInt().coerceAtLeast(1)
                        min > 0.0 -> min.toInt().coerceAtLeast(1)
                        else -> -2
                    }
                }
            }
            null
        } catch (_: Exception) {
            remoteApiCooldownUntilMs.set(System.currentTimeMillis() + 30_000L)
            null
        }
    }
}
