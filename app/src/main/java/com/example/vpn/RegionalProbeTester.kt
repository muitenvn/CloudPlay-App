package com.example.vpn

import com.example.data.AppLogger
import com.example.data.SettingsRepository
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
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
 * Kết quả ước lượng độ trễ tham khảo từ trạm Globalping bên thứ ba (tách biệt với số đo TCP trực tiếp từ điện thoại).
 */
data class GlobalpingProbeEstimate(
    val rttMs: Int,
    val probeId: String,
    val probeCity: String,
    val probeDisplayName: String,
    val timestampMs: Long
) {
    fun formatLabel(isEn: Boolean): String {
        return if (isEn) {
            "Estimated from $probeCity probe (excludes phone-to-probe leg): ${rttMs} ms"
        } else {
            "Ước lượng từ trạm $probeCity (không gồm đoạn từ điện thoại tới trạm): ${rttMs} ms"
        }
    }
}

/**
 * Hệ thống định vị Quốc gia hỗ trợ Xbox gần người dùng nhất & Tùy chọn ước lượng độ trễ qua trạm Globalping:
 * - Mặc định TẮT (enableGlobalpingProbe = false); ưu tiên đo TCP trực tiếp từ thiết bị.
 * - Không giả định người dùng ở Việt Nam khi chưa có dữ liệu vị trí GeoIP thực tế.
 * - Chỉ đo đơn lẻ theo yêu cầu cho máy chủ đang chọn, không dùng để quét hàng loạt.
 * - Hạn mức miễn phí chính thức của Globalping API (api.globalping.io): 250 lượt đo/giờ/IP.
 *   Giới hạn cục bộ trên thiết bị: tối đa 200 lượt/giờ, bộ nhớ đệm 10 phút.
 */
object RegionalProbeTester {

    /** Hạn mức miễn phí phía dịch vụ Globalping API (unauthenticated): 250 measurements / giờ / IP */
    const val GLOBALPING_SERVICE_FREE_LIMIT_PER_HOUR = 250

    /** Trần bảo vệ cục bộ trên thiết bị để không chạm ngưỡng 429 của dịch vụ */
    const val LOCAL_DEVICE_HOURLY_CAP = 200

    /** Thời gian lưu cache kết quả ước lượng từ trạm (10 phút) */
    const val CACHE_TTL_MS = 10 * 60 * 1000L

    private const val ONE_HOUR_MS = 60 * 60 * 1000L

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val probeHttpClient = OkHttpClient.Builder()
        .connectTimeout(1600, TimeUnit.MILLISECONDS)
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .writeTimeout(1600, TimeUnit.MILLISECONDS)
        .build()

    private val remoteProbeSemaphore = Semaphore(2)

    private val probeCache = ConcurrentHashMap<String, GlobalpingProbeEstimate>()
    private val latestServerEstimate = ConcurrentHashMap<String, GlobalpingProbeEstimate>()
    private val hourlyRequestTimestamps = ConcurrentLinkedQueue<Long>()

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

        // Đông Bắc Á
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

    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Trả về tọa độ thực của người dùng nếu đã có GeoIP hợp lệ.
     * Nếu chưa có GeoIP thực tế -> trả về null (KHÔNG giả định người dùng ở Việt Nam).
     */
    fun resolveUserCoordinatesOrNull(geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp): Pair<Double, Double>? {
        val realGeo = if (geo != null && !geo.isFromWebView) {
            geo
        } else {
            GeoIpDetector.currentGeoIp.value?.takeIf { !it.isFromWebView } ?: geo?.takeIf { !it.isFromWebView }
        } ?: return null

        val country = realGeo.countryCode.trim().uppercase()
        if (country.isBlank() || country == "UNKNOWN") return null
        val cityLower = realGeo.city.trim().lowercase()

        if (realGeo.latitude != null && realGeo.longitude != null) {
            return Pair(realGeo.latitude, realGeo.longitude)
        }

        if (country == "VN") {
            return when {
                cityLower.contains("hanoi") || cityLower.contains("ha noi") ||
                    cityLower.contains("hà nội") || cityLower.contains("hai phong") ||
                    cityLower.contains("hải phòng") || cityLower.contains("quang ninh") ||
                    cityLower.contains("bac ninh") || cityLower.contains("nam dinh") ||
                    cityLower.contains("thai nguyen") || cityLower.contains("vinh") -> {
                    Pair(21.0285, 105.8542)
                }
                cityLower.contains("da nang") || cityLower.contains("đà nẵng") ||
                    cityLower.contains("hue") || cityLower.contains("huế") ||
                    cityLower.contains("quang nam") || cityLower.contains("quy nhon") -> {
                    Pair(16.0544, 108.2022)
                }
                else -> Pair(10.8231, 106.6297)
            }
        }

        return COUNTRY_REFERENCE_COORDS[country]
    }

    fun resolveServerCoordinates(server: ServerLocation): Pair<Double, Double> {
        val nameLower = (server.name + " " + server.description + " " + server.azureEndpoint).lowercase()
        val code = server.countryCode.trim().uppercase()
        return when {
            code == "JP" -> when {
                nameLower.contains("osaka") || nameLower.contains("japanwest") -> Pair(34.6937, 135.5023)
                nameLower.contains("gotemba") || nameLower.contains("shizuoka") -> Pair(35.3086, 138.9344)
                nameLower.contains("tsukuba") || nameLower.contains("chiba") -> Pair(36.0835, 140.0764)
                else -> Pair(35.6762, 139.6503)
            }
            code == "KR" -> when {
                nameLower.contains("busan") -> Pair(35.1796, 129.0756)
                nameLower.contains("incheon") -> Pair(37.4563, 126.7052)
                else -> Pair(37.5665, 126.9780)
            }
            code == "US" -> when {
                nameLower.contains("virginia") || nameLower.contains("eastus") -> Pair(39.0438, -77.4874)
                nameLower.contains("iowa") || nameLower.contains("centralus") -> Pair(41.5868, -93.6250)
                nameLower.contains("oregon") || nameLower.contains("westus2") -> Pair(45.5152, -122.6784)
                nameLower.contains("nevada") || nameLower.contains("vegas") -> Pair(36.1699, -115.1398)
                else -> Pair(37.3382, -121.8863)
            }
            else -> COUNTRY_REFERENCE_COORDS[code] ?: Pair(35.6762, 139.6503)
        }
    }

    /**
     * Tự động tìm danh sách các Quốc gia được hỗ trợ Xbox xếp theo khoảng cách địa lý gần người dùng nhất.
     * Nếu chưa có GeoIP thực tế -> giữ nguyên thứ tự gốc của danh sách máy chủ, không giả định Việt Nam.
     */
    fun getNearestSupportedCountriesForUser(
        servers: List<ServerLocation> = DEFAULT_SERVERS,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): List<String> {
        val distinctCountries = servers
            .map { it.countryCode.trim().uppercase() }
            .filter { it.isNotBlank() && GeoIpDetector.SUPPORTED_COUNTRIES.contains(it) }
            .distinct()

        val userCoords = resolveUserCoordinatesOrNull(geo) ?: return distinctCountries
        val (userLat, userLon) = userCoords

        return distinctCountries.sortedBy { code ->
            val (cLat, cLon) = COUNTRY_REFERENCE_COORDS[code] ?: Pair(35.6762, 139.6503)
            haversineDistanceKm(userLat, userLon, cLat, cLon)
        }
    }

    /**
     * Phân tầng khoảng cách Quốc gia hỗ trợ Xbox so với người dùng (chỉ dùng làm tiêu chí sơ bộ khi chưa đo được ping thật).
     * Khi thiếu GeoIP thực tế -> trả về 0 cho mọi quốc gia được hỗ trợ (không giả định vị trí người dùng).
     */
    fun getNearestSupportedCountryTier(
        serverCountryCode: String,
        servers: List<ServerLocation> = DEFAULT_SERVERS,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): Int {
        val code = serverCountryCode.trim().uppercase()
        if (!GeoIpDetector.SUPPORTED_COUNTRIES.contains(code)) return 3

        val userCoords = resolveUserCoordinatesOrNull(geo) ?: return 0
        val (userLat, userLon) = userCoords

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
            deltaKm <= 1800.0 -> 0
            deltaKm <= 5500.0 -> 1
            else -> 2
        }
    }

    /**
     * Xác định danh sách Trạm Test được sắp xếp từ gần người dùng nhất tới xa hơn.
     * Nếu chưa có GeoIP thực tế -> trả về danh sách rỗng (bỏ qua trạm đo và đo TCP trực tiếp).
     */
    fun selectOrderedProbes(geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp): List<ProbeStation> {
        val realGeo = if (geo != null && !geo.isFromWebView) {
            geo
        } else {
            GeoIpDetector.currentGeoIp.value?.takeIf { !it.isFromWebView } ?: geo?.takeIf { !it.isFromWebView }
        } ?: return emptyList()

        val userCoords = resolveUserCoordinatesOrNull(realGeo) ?: return emptyList()
        val (userLat, userLon) = userCoords
        val country = realGeo.countryCode.trim().uppercase()
        val cityLower = realGeo.city.trim().lowercase()

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
            val nearestXboxCountries = getNearestSupportedCountriesForUser(DEFAULT_SERVERS, realGeo).take(2).joinToString(", ")
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
     * Kiểm tra và cấp phép hạn mức cục bộ (tối đa 200 lượt/giờ trên thiết bị < 250 lượt/giờ miễn phí của Globalping).
     */
    @Synchronized
    fun tryAcquireHourlyQuota(nowMs: Long = System.currentTimeMillis()): Boolean {
        while (true) {
            val head = hourlyRequestTimestamps.peek() ?: break
            if (nowMs - head > ONE_HOUR_MS) {
                hourlyRequestTimestamps.poll()
            } else {
                break
            }
        }
        if (hourlyRequestTimestamps.size >= LOCAL_DEVICE_HOURLY_CAP) {
            return false
        }
        hourlyRequestTimestamps.add(nowMs)
        return true
    }

    @Synchronized
    fun getRemainingHourlyQuota(nowMs: Long = System.currentTimeMillis()): Int {
        while (true) {
            val head = hourlyRequestTimestamps.peek() ?: break
            if (nowMs - head > ONE_HOUR_MS) {
                hourlyRequestTimestamps.poll()
            } else {
                break
            }
        }
        return (LOCAL_DEVICE_HOURLY_CAP - hourlyRequestTimestamps.size).coerceAtLeast(0)
    }

    @Synchronized
    fun resetQuotaForTest() {
        hourlyRequestTimestamps.clear()
        probeCache.clear()
        latestServerEstimate.clear()
        remoteApiCooldownUntilMs.set(0L)
    }

    fun getCachedEstimateForServer(server: ServerLocation): GlobalpingProbeEstimate? {
        val est = latestServerEstimate[server.id] ?: return null
        if (System.currentTimeMillis() - est.timestampMs > CACHE_TTL_MS) {
            latestServerEstimate.remove(server.id)
            return null
        }
        return est
    }

    /**
     * Ước lượng độ trễ theo yêu cầu cho 1 máy chủ đang chọn qua trạm Globalping gần nhất:
     * - Chỉ chạy khi người dùng bật công tắc `enableGlobalpingProbe` VÀ đã có GeoIP thực tế.
     * - Không bao giờ chỉnh sửa số đo theo khoảng cách vật lý.
     * - Kết quả tách biệt hoàn toàn với số đo TCP trực tiếp từ điện thoại.
     */
    suspend fun measureSelectedServerEstimateViaProbe(
        server: ServerLocation,
        timeoutMs: Int = 2200,
        geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp
    ): GlobalpingProbeEstimate? {
        if (!SettingsRepository.config.value.enableGlobalpingProbe) {
            return null
        }
        if (server.ip.isBlank() || server.port <= 0) return null
        val now = System.currentTimeMillis()
        if (now < remoteApiCooldownUntilMs.get()) {
            return null
        }

        val candidateProbes = selectOrderedProbes(geo).take(2)
        if (candidateProbes.isEmpty()) return null

        for ((index, probe) in candidateProbes.withIndex()) {
            val probeCooldown = failedProbeIdsUntilMs[probe.id] ?: 0L
            if (System.currentTimeMillis() < probeCooldown) {
                continue
            }

            val cacheKey = "${probe.id}:${server.ip.trim().lowercase()}"
            val cached = probeCache[cacheKey]
            if (cached != null && (System.currentTimeMillis() - cached.timestampMs) < CACHE_TTL_MS) {
                latestServerEstimate[server.id] = cached
                return cached
            }

            if (!tryAcquireHourlyQuota()) {
                AppLogger.warn(
                    "PROBE",
                    "Đã đạt trần cục bộ $LOCAL_DEVICE_HOURLY_CAP/$GLOBALPING_SERVICE_FREE_LIMIT_PER_HOUR lượt/giờ cho Globalping -> Bỏ qua gọi trạm"
                )
                return null
            }

            val rawRtt = remoteProbeSemaphore.withPermit {
                requestGlobalpingRtt(probe, server, timeoutMs)
            }

            if (rawRtt != null && rawRtt > 0) {
                val estimate = GlobalpingProbeEstimate(
                    rttMs = rawRtt,
                    probeId = probe.id,
                    probeCity = probe.apiCity,
                    probeDisplayName = probe.displayName,
                    timestampMs = System.currentTimeMillis()
                )
                probeCache[cacheKey] = estimate
                latestServerEstimate[server.id] = estimate
                AppLogger.success(
                    "PROBE",
                    "Ước lượng từ trạm ${probe.displayName} -> ${server.name}: ${rawRtt}ms (không gồm đoạn từ điện thoại tới trạm)"
                )
                return estimate
            } else {
                if (rawRtt == null) {
                    failedProbeIdsUntilMs[probe.id] = System.currentTimeMillis() + 20_000L
                    val nextProbe = candidateProbes.getOrNull(index + 1)
                    if (nextProbe != null) {
                        AppLogger.warn(
                            "PROBE",
                            "Trạm ${probe.displayName} bận -> Thử trạm lân cận: ${nextProbe.displayName}"
                        )
                    }
                } else {
                    return null
                }
            }
        }
        return null
    }

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
                remoteApiCooldownUntilMs.set(System.currentTimeMillis() + 60_000L)
                AppLogger.warn("PROBE", "API Globalping trả mã $code -> Tạm ngắt 60s, dùng số đo TCP trực tiếp")
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
                delay(500) // Tối đa 2 GET/giây theo đúng giới hạn của Globalping
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
