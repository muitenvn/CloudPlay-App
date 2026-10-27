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
 * Hệ thống đo ping qua Trạm Test (Probe) khu vực gần người dùng nhất.
 * - Chỉ sử dụng trong mục Máy chủ (Servers tab) để đo ping tới các máy chủ VPN/Proxy.
 * - KHÔNG sử dụng để đo ping khi đang chơi game (ping trong game dùng 100% WebRTC RTT).
 * - Thứ tự ưu tiên ngầm (Silent Fallback):
 *   1. Trạm Test gần nhất theo vị trí người dùng (VD: TP. Hồ Chí Minh hoặc Hà Nội tại VN, hoặc trạm gần nhất theo Haversine).
 *   2. Trạm Test lân cận kế tiếp nếu trạm số 1 đang bận / không có node trực tuyến.
 *   3. Đo TCP trực tiếp từ thiết bị (với giới hạn 3 luồng đồng thời để chống nghẽn hàng đợi 100ms).
 * - Mọi hoạt động chọn trạm, chuyển trạm dự phòng và kết quả đo được ghi vào AppLogger (xem tại Cài đặt -> Nhật ký).
 */
object RegionalProbeTester {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val probeHttpClient = OkHttpClient.Builder()
        .connectTimeout(1800, TimeUnit.SECONDS.let { TimeUnit.MILLISECONDS })
        .readTimeout(2200, TimeUnit.MILLISECONDS)
        .writeTimeout(1800, TimeUnit.MILLISECONDS)
        .build()

    // Giới hạn số yêu cầu đo qua API trạm test đồng thời để tránh vượt Rate Limit
    private val remoteProbeSemaphore = Semaphore(4)

    // Bộ nhớ đệm ngắn hạn (45 giây) cho kết quả đo từ Trạm Test tới IP:Port
    private data class CachedProbePing(val rttMs: Int, val timestampMs: Long, val probeName: String)
    private val probeCache = ConcurrentHashMap<String, CachedProbePing>()
    private const val CACHE_TTL_MS = 45_000L

    // Ngắt mạch tạm thời (25 giây) nếu dịch vụ API trạm đo toàn cầu bị chặn mạng hoặc hết hạn mức (HTTP 429)
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

    // Tọa độ tâm tham chiếu cho các quốc gia (bao gồm các nước nhỏ không có trạm test nội địa)
    private val COUNTRY_REFERENCE_COORDS = mapOf(
        "VN" to Pair(10.8231, 106.6297), // Mặc định ưu tiên cụm phía Nam (TP.HCM) nếu chưa rõ tỉnh/thành
        "LA" to Pair(17.9757, 102.6331), // Lào (Vientiane -> gần Hà Nội / Đà Nẵng / Bangkok)
        "KH" to Pair(11.5564, 104.9282), // Campuchia (Phnom Penh -> gần TP.HCM nhất)
        "MM" to Pair(16.8409, 96.1735),  // Myanmar (Yangon -> gần Bangkok nhất)
        "BN" to Pair(4.9031, 114.9398),  // Brunei (-> gần Singapore / Kuala Lumpur)
        "SG" to Pair(1.3521, 103.8198),
        "TH" to Pair(13.7563, 100.5018),
        "MY" to Pair(3.1390, 101.6869),
        "ID" to Pair(-6.2088, 106.8456),
        "PH" to Pair(14.5995, 120.9842),
        "HK" to Pair(22.3193, 114.1694),
        "MO" to Pair(22.1987, 113.5439), // Macau (-> gần Hong Kong nhất)
        "TW" to Pair(25.0330, 121.5654),
        "JP" to Pair(35.6762, 139.6503),
        "KR" to Pair(37.5665, 126.9780),
        "IN" to Pair(19.0760, 72.8777),
        "AU" to Pair(-33.8688, 151.2093),
        "NZ" to Pair(-36.8485, 174.7633), // New Zealand (-> gần Sydney nhất)
        "DE" to Pair(50.1109, 8.6821),
        "GB" to Pair(51.5074, -0.1278),
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
     * Xác định danh sách Trạm Test được sắp xếp từ gần người dùng nhất tới xa hơn
     * dựa trên GeoIP thực tế (tọa độ vĩ độ/kinh độ, tên thành phố, hoặc mã quốc gia).
     */
    fun selectOrderedProbes(geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp): List<ProbeStation> {
        val realGeo = if (geo != null && !geo.isFromWebView) geo else GeoIpDetector.currentGeoIp.value
        val country = (realGeo?.countryCode ?: "VN").trim().uppercase()
        val cityLower = (realGeo?.city ?: "").trim().lowercase()

        val (userLat, userLon) = when {
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
                        Pair(21.0285, 105.8542) // Khu vực miền Bắc -> Hà Nội
                    }
                    cityLower.contains("da nang") || cityLower.contains("đà nẵng") ||
                        cityLower.contains("hue") || cityLower.contains("huế") ||
                        cityLower.contains("quang nam") || cityLower.contains("quy nhon") -> {
                        Pair(16.0544, 108.2022) // Khu vực miền Trung -> Đà Nẵng
                    }
                    else -> {
                        Pair(10.8231, 106.6297) // Khu vực miền Nam / mặc định VN -> TP. Hồ Chí Minh
                    }
                }
            }
            else -> {
                COUNTRY_REFERENCE_COORDS[country] ?: Pair(10.8231, 106.6297)
            }
        }

        val sorted = PROBE_STATIONS.sortedWith(
            Comparator { a, b ->
                val distA = haversineDistanceKm(userLat, userLon, a.latitude, a.longitude)
                val distB = haversineDistanceKm(userLat, userLon, b.latitude, b.longitude)
                // Ưu tiên nhẹ cho trạm nằm cùng quốc gia nếu khoảng cách tương đương
                val effectiveDistA = if (a.countryCode.equals(country, ignoreCase = true)) distA * 0.85 else distA
                val effectiveDistB = if (b.countryCode.equals(country, ignoreCase = true)) distB * 0.85 else distB
                effectiveDistA.compareTo(effectiveDistB)
            }
        )

        val primary = sorted.firstOrNull()
        if (primary != null && primary.id != lastLoggedPrimaryProbeId) {
            lastLoggedPrimaryProbeId = primary.id
            val backupNames = sorted.drop(1).take(2).joinToString(" -> ") { it.displayName }
            AppLogger.info(
                "PROBE",
                "Vị trí người dùng: $country ${if (cityLower.isNotBlank()) "($cityLower)" else ""} • " +
                    "Trạm đo gần nhất: ${primary.displayName} (Dự phòng lân cận: $backupNames)"
            )
        }
        return sorted
    }

    /**
     * Đo ping cho máy chủ trong mục Máy chủ theo đúng thứ tự:
     * 1. Trạm Test gần nhất (chỉ lấy nguyên số ping từ trạm test tới server).
     * 2. Nếu trạm gần nhất bận/lỗi -> tự động thử trạm lân cận kế tiếp (không hiện thông báo trên UI, chỉ ghi Log).
     * 3. Nếu các trạm từ xa đều không phản hồi -> trả về null để PingTester chuyển sang đo TCP trực tiếp từ máy.
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

            val resultRtt = remoteProbeSemaphore.withPermit {
                requestGlobalpingTcpRtt(probe, server, timeoutMs)
            }

            if (resultRtt != null && resultRtt > 0) {
                probeCache[cacheKey] = CachedProbePing(
                    rttMs = resultRtt,
                    timestampMs = System.currentTimeMillis(),
                    probeName = probe.displayName
                )
                AppLogger.success(
                    "PROBE",
                    "Trạm ${probe.displayName} -> ${server.name} (${server.ip}:${server.port}): ${resultRtt}ms"
                )
                return resultRtt
            } else if (resultRtt == -2) {
                // -2: Đích đến (server.ip:port) đã được trạm đo xác nhận là không phản hồi (Packet Loss 100%)
                AppLogger.warn(
                    "PROBE",
                    "Trạm ${probe.displayName} xác nhận máy chủ ${server.name} (${server.ip}:${server.port}) không phản hồi"
                )
                return -1
            } else {
                // null: Trạm đo hiện tại bận hoặc chưa có node rảnh -> đánh dấu tạm nghỉ 20s và thử trạm lân cận
                failedProbeIdsUntilMs[probe.id] = System.currentTimeMillis() + 20_000L
                val nextProbe = candidateProbes.getOrNull(index + 1)
                if (nextProbe != null) {
                    AppLogger.warn(
                        "PROBE",
                        "Trạm ${probe.displayName} đang bận -> Chuyển ngầm sang trạm lân cận: ${nextProbe.displayName}"
                    )
                }
            }
        }

        return null
    }

    /**
     * Gọi Globalping API để đo TCP RTT trực tiếp từ [probe] tới [server].
     * Trả về:
     * - `> 0`: Số ping thực tế (ms) từ trạm test tới server.
     * - `-2`: Trạm test hoạt động bình thường nhưng server đích chết / đóng cổng.
     * - `null`: Trạm test bận, lỗi mạng hoặc bị giới hạn tần suất (cần chuyển sang trạm lân cận / đo từ máy).
     */
    private suspend fun requestGlobalpingTcpRtt(
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
                        put("protocol", "TCP")
                        put("port", server.port)
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
                remoteApiCooldownUntilMs.set(System.currentTimeMillis() + 25_000L)
                AppLogger.warn("PROBE", "API trạm đo báo mã $code -> Chuyển ngầm sang đo trực tiếp trên máy trong 25s")
                return null
            }
            if (code == 422 || !postResp.isSuccessful) {
                return null
            }

            val postJson = JSONObject(bodyStr)
            val measurementId = postJson.optString("id", "")
            if (measurementId.isBlank()) return null

            val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(1200, 3000)
            while (System.currentTimeMillis() < deadline) {
                delay(320)
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
                    val rtt = when {
                        avg > 0.0 -> avg.toInt().coerceAtLeast(1)
                        min > 0.0 -> min.toInt().coerceAtLeast(1)
                        else -> -2
                    }
                    return rtt
                }
            }
            null
        } catch (_: Exception) {
            remoteApiCooldownUntilMs.set(System.currentTimeMillis() + 25_000L)
            null
        }
    }
}
