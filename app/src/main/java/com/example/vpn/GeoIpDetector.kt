package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeoIpResult(
    val ip: String,
    val countryCode: String,
    val countryName: String,
    val city: String,
    val flagEmoji: String,
    val isXboxSupported: Boolean,
    val isFromWebView: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
)

object GeoIpDetector {

    private val _currentGeoIp = MutableStateFlow<GeoIpResult?>(null)
    val currentGeoIp: StateFlow<GeoIpResult?> = _currentGeoIp.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Xbox Cloud officially supported countries (Singapore 'SG' is NOT supported by Xbox Cloud Gaming)
    val SUPPORTED_COUNTRIES = setOf(
        "JP", "US", "KR", "DE", "GB", "FR", "ES", "IT", "PT", "NL", "BE", "AT", "CH", "IE",
        "AU", "NZ", "CA", "SE", "NO", "DK", "FI", "PL", "CZ", "HU", "SK", "BR", "MX", "AR", "CL", "CO"
    )

    fun getFlagEmoji(countryCode: String): String {
        val clean = countryCode.trim().uppercase(java.util.Locale.ROOT).let {
            if (it == "UK") "GB" else it
        }
        if (clean.length != 2 || clean == "XX" || clean == "ZZ") return "🌐"
        val c0 = clean[0]
        val c1 = clean[1]
        if (c0 !in 'A'..'Z' || c1 !in 'A'..'Z') return "🌐"
        val firstCodePoint = 0x1F1E6 + (c0 - 'A')
        val secondCodePoint = 0x1F1E6 + (c1 - 'A')
        return String(Character.toChars(firstCodePoint)) + String(Character.toChars(secondCodePoint))
    }

    fun getCountryDisplayName(
        countryCode: String,
        isEn: Boolean = try {
            com.example.data.SettingsRepository.config.value.isEnglishUI()
        } catch (_: Exception) {
            false
        }
    ): String {
        val clean = countryCode.trim().uppercase(java.util.Locale.ROOT).let {
            if (it == "UK") "GB" else it
        }
        if (clean.length != 2 || clean == "XX" || clean == "ZZ" || !clean.all { it in 'A'..'Z' }) {
            return if (isEn) "Unknown country" else "Không rõ quốc gia"
        }
        return when (clean) {
            "JP" -> if (isEn) "Japan" else "Nhật Bản"
            "KR" -> if (isEn) "South Korea" else "Hàn Quốc"
            "US" -> if (isEn) "United States" else "Hoa Kỳ"
            "SG" -> "Singapore"
            "VN" -> if (isEn) "Vietnam" else "Việt Nam"
            "GB" -> if (isEn) "United Kingdom" else "Vương Quốc Anh"
            "DE" -> if (isEn) "Germany" else "Đức"
            "FR" -> if (isEn) "France" else "Pháp"
            "ES" -> if (isEn) "Spain" else "Tây Ban Nha"
            "IT" -> if (isEn) "Italy" else "Ý"
            "PT" -> if (isEn) "Portugal" else "Bồ Đào Nha"
            "NL" -> if (isEn) "Netherlands" else "Hà Lan"
            "AU" -> if (isEn) "Australia" else "Úc"
            "NZ" -> "New Zealand"
            "CA" -> "Canada"
            "BR" -> "Brazil"
            "MX" -> "Mexico"
            "AR" -> "Argentina"
            "CL" -> "Chile"
            "CO" -> "Colombia"
            "PL" -> if (isEn) "Poland" else "Ba Lan"
            "TR" -> if (isEn) "Turkey" else "Thổ Nhĩ Kỳ"
            "RU" -> if (isEn) "Russia" else "Nga"
            "IN" -> if (isEn) "India" else "Ấn Độ"
            "ID" -> "Indonesia"
            "TH" -> if (isEn) "Thailand" else "Thái Lan"
            "MY" -> "Malaysia"
            "PH" -> "Philippines"
            "TW" -> if (isEn) "Taiwan" else "Đài Loan"
            "HK" -> "Hong Kong"
            "CN" -> if (isEn) "China" else "Trung Quốc"
            else -> {
                val targetLocale = if (isEn) java.util.Locale.ENGLISH else java.util.Locale("vi", "VN")
                val localized = java.util.Locale("", clean).getDisplayCountry(targetLocale)
                if (localized.isNotBlank() && !localized.equals(clean, ignoreCase = true)) {
                    localized
                } else {
                    val enFallback = java.util.Locale("", clean).getDisplayCountry(java.util.Locale.ENGLISH)
                    if (enFallback.isNotBlank() && !enFallback.equals(clean, ignoreCase = true)) {
                        enFallback
                    } else {
                        if (isEn) "Unknown country" else "Không rõ quốc gia"
                    }
                }
            }
        }
    }

    /**
     * Mục 9.4 & 10: Chỉ gán azureEndpoint khi đã có ánh xạ thật theo quốc gia (JP, KR).
     * Các nước chưa có ánh xạ trong app (US, VN, DE, FR...) hoặc không rõ quốc gia thì trả về chuỗi rỗng "".
     */
    fun getAzureEndpointForCountry(countryCode: String): String {
        return when (countryCode.trim().uppercase(java.util.Locale.ROOT)) {
            "JP" -> "japaneast.cloudapp.azure.com"
            "KR" -> "koreacentral.cloudapp.azure.com"
            else -> ""
        }
    }

    fun formatServerDisplayName(countryCode: String, ip: String, isEn: Boolean = try {
        com.example.data.SettingsRepository.config.value.isEnglishUI()
    } catch (_: Exception) { false }): String {
        val cleanCc = countryCode.trim().uppercase(java.util.Locale.ROOT)
        val flag = getFlagEmoji(cleanCc)
        val countryName = getCountryDisplayName(cleanCc, isEn)
        return "$flag $countryName • ${ip.trim()}"
    }

    fun ipv4ToLong(ip: String): Long? {
        val trimmed = ip.trim()
        val parts = trimmed.split('.')
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            if (part.isEmpty() || part.length > 3) return null
            val num = part.toIntOrNull() ?: return null
            if (num !in 0..255) return null
            result = (result shl 8) or num.toLong()
        }
        return result
    }

    fun isPrivateOrLoopbackIp(ip: String): Boolean {
        val trimmed = ip.trim().lowercase(java.util.Locale.ROOT)
        if (trimmed.isEmpty() || trimmed == "localhost") return true
        if (trimmed.contains(':')) {
            if (trimmed == "::1" || trimmed == "::" || trimmed == "0:0:0:0:0:0:0:1") return true
            if (trimmed.startsWith("fe80:") || trimmed.startsWith("fc") || trimmed.startsWith("fd")) return true
            return false
        }
        val ipNum = ipv4ToLong(trimmed) ?: return true
        val b0 = ((ipNum ushr 24) and 0xFF).toInt()
        val b1 = ((ipNum ushr 16) and 0xFF).toInt()
        // 0.0.0.0/8, 10.0.0.0/8, 127.0.0.0/8
        if (b0 == 0 || b0 == 10 || b0 == 127) return true
        // 100.64.0.0/10 (CGNAT)
        if (b0 == 100 && (b1 in 64..127)) return true
        // 169.254.0.0/16 (Link-local)
        if (b0 == 169 && b1 == 254) return true
        // 172.16.0.0/12 (Private)
        if (b0 == 172 && (b1 in 16..31)) return true
        // 192.168.0.0/16 (Private)
        if (b0 == 192 && b1 == 168) return true
        // 198.18.0.0/15 (Benchmark)
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return true
        // 224.0.0.0/4 (Multicast) & 240.0.0.0/4 (Reserved)
        if (b0 >= 224) return true
        return false
    }

    private data class IpRangeTable(
        val startIps: LongArray,
        val endIps: LongArray,
        val countryCodes: Array<String>
    )

    // Dải IP hạt giống chuẩn từ DB-IP Lite Country (CC-BY 4.0 DB-IP.com) cho các dải máy chủ công cộng phổ biến
    // và phục vụ tra cứu ngoại tuyến / kiểm thử tức thì trước khi tệp CSV đầy đủ tải xong.
    private val defaultSeedTable: IpRangeTable by lazy {
        val rawRanges = listOf(
            Triple("1.0.16.0", "1.0.31.255", "JP"),
            Triple("1.1.64.0", "1.1.127.255", "JP"),
            Triple("1.52.0.0", "1.55.255.255", "VN"),
            Triple("8.8.8.0", "8.8.8.255", "US"),
            Triple("14.10.0.0", "14.10.255.255", "JP"),
            Triple("14.160.0.0", "14.191.255.255", "VN"),
            Triple("27.0.0.0", "27.35.255.255", "KR"),
            Triple("34.32.0.0", "34.63.255.255", "US"),
            Triple("39.112.0.0", "39.127.255.255", "KR"),
            Triple("45.32.0.0", "45.32.63.255", "JP"),
            Triple("49.160.0.0", "49.175.255.255", "KR"),
            Triple("51.15.0.0", "51.15.255.255", "FR"),
            Triple("51.158.0.0", "51.159.255.255", "FR"),
            Triple("61.192.0.0", "61.215.255.255", "JP"),
            Triple("67.192.0.0", "69.63.255.255", "US"),
            Triple("88.198.0.0", "88.198.255.255", "DE"),
            Triple("103.1.208.0", "103.1.211.255", "VN"),
            Triple("104.16.0.0", "104.31.255.255", "US"),
            Triple("106.128.0.0", "106.191.255.255", "JP"),
            Triple("113.160.0.0", "113.191.255.255", "VN"),
            Triple("116.120.0.0", "116.127.255.255", "KR"),
            Triple("118.32.0.0", "118.47.255.255", "KR"),
            Triple("121.128.0.0", "121.191.255.255", "KR"),
            Triple("122.252.0.0", "122.255.255.255", "KR"),
            Triple("123.140.0.0", "123.143.255.255", "KR"),
            Triple("126.0.0.0", "126.255.255.255", "JP"),
            Triple("130.127.0.0", "130.127.255.255", "US"),
            Triple("133.0.0.0", "133.255.255.255", "JP"),
            Triple("138.199.21.0", "138.199.21.255", "JP"),
            Triple("139.162.64.0", "139.162.127.255", "JP"),
            Triple("141.147.128.0", "141.147.191.255", "KR"),
            Triple("141.223.0.0", "141.223.255.255", "KR"),
            Triple("143.244.0.0", "143.247.255.255", "US"),
            Triple("158.247.192.0", "158.247.255.255", "KR"),
            Triple("159.65.0.0", "159.65.255.255", "US"),
            Triple("167.99.0.0", "167.99.255.255", "US"),
            Triple("172.104.64.0", "172.104.127.255", "JP"),
            Triple("175.192.0.0", "175.215.255.255", "KR"),
            Triple("183.96.0.0", "183.111.255.255", "KR"),
            Triple("185.220.100.0", "185.220.103.255", "DE"),
            Triple("211.168.0.0", "211.255.255.255", "KR"),
            Triple("218.144.0.0", "218.159.255.255", "KR"),
            Triple("219.100.32.0", "219.100.63.255", "JP"),
            Triple("220.64.0.0", "220.127.255.255", "KR"),
            Triple("222.96.0.0", "222.127.255.255", "KR")
        ).mapNotNull { (s, e, cc) ->
            val start = ipv4ToLong(s)
            val end = ipv4ToLong(e)
            if (start != null && end != null && start <= end) Triple(start, end, cc) else null
        }.sortedBy { it.first }

        IpRangeTable(
            startIps = LongArray(rawRanges.size) { rawRanges[it].first },
            endIps = LongArray(rawRanges.size) { rawRanges[it].second },
            countryCodes = Array(rawRanges.size) { rawRanges[it].third }
        )
    }

    @Volatile
    private var activeRangeTable: IpRangeTable = defaultSeedTable

    private val ipCountryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun loadCsvRangesFromStream(inputStream: java.io.InputStream): Boolean {
        return try {
            val starts = ArrayList<Long>(65536)
            val ends = ArrayList<Long>(65536)
            val codes = ArrayList<String>(65536)
            var lastStart = -1L

            java.io.BufferedReader(java.io.InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val parts = line.split(',')
                    if (parts.size < 3) continue
                    val sStr = parts[0].trim().removeSurrounding("\"")
                    val eStr = parts[1].trim().removeSurrounding("\"")
                    val cc = parts[2].trim().removeSurrounding("\"").uppercase(java.util.Locale.ROOT)
                    if (sStr.contains(':') || eStr.contains(':')) continue
                    if (cc.length != 2 || cc == "ZZ" || cc == "XX" || !cc.all { it in 'A'..'Z' }) continue
                    val startIp = ipv4ToLong(sStr) ?: continue
                    val endIp = ipv4ToLong(eStr) ?: continue
                    if (startIp > endIp || startIp < lastStart) continue
                    starts.add(startIp)
                    ends.add(endIp)
                    codes.add(cc)
                    lastStart = startIp
                }
            }

            if (starts.size >= 5) {
                activeRangeTable = IpRangeTable(
                    startIps = starts.toLongArray(),
                    endIps = ends.toLongArray(),
                    countryCodes = codes.toTypedArray()
                )
                ipCountryCache.clear()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun lookupInTable(table: IpRangeTable, ipNum: Long): String {
        val starts = table.startIps
        val ends = table.endIps
        val codes = table.countryCodes
        var low = 0
        var high = starts.size - 1
        var candidateIdx = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (starts[mid] <= ipNum) {
                candidateIdx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (candidateIdx >= 0 && ipNum <= ends[candidateIdx]) {
            return codes[candidateIdx]
        }
        return ""
    }

    /**
     * Tra cứu mã quốc gia ISO 2 ký tự từ địa chỉ IPv4 bằng bảng nhị phân DB-IP Lite Country.
     * - Từ chối ngay IP nội bộ/loopback (trả về "").
     * - Nếu không tìm thấy trong bảng hiện tại và [allowNetworkFallback] = true kèm công tắc
     *   `enableGeoIpHttpsFallback` đang bật, mới gọi dịch vụ dự phòng HTTPS `api.country.is`.
     */
    fun resolveIpCountryCode(ip: String, allowNetworkFallback: Boolean = false): String {
        val trimmed = ip.trim()
        if (isPrivateOrLoopbackIp(trimmed)) return ""
        val ipNum = ipv4ToLong(trimmed) ?: return ""

        ipCountryCache[trimmed]?.let { cached ->
            if (cached.isNotEmpty()) return cached
        }

        val fromActive = lookupInTable(activeRangeTable, ipNum)
        if (fromActive.isNotEmpty()) {
            ipCountryCache[trimmed] = fromActive
            return fromActive
        }

        if (activeRangeTable !== defaultSeedTable) {
            val fromSeed = lookupInTable(defaultSeedTable, ipNum)
            if (fromSeed.isNotEmpty()) {
                ipCountryCache[trimmed] = fromSeed
                return fromSeed
            }
        }

        val fallbackEnabled = try {
            com.example.data.SettingsRepository.config.value.enableGeoIpHttpsFallback
        } catch (_: Exception) {
            true
        }

        if (allowNetworkFallback && fallbackEnabled) {
            try {
                val req = Request.Builder()
                    .url("https://api.country.is/$trimmed")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val cc = json.optString("country", "").trim().uppercase(java.util.Locale.ROOT)
                        if (cc.length == 2 && cc.all { it in 'A'..'Z' } && cc != "XX" && cc != "ZZ") {
                            ipCountryCache[trimmed] = cc
                            return cc
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return ""
    }

    /**
     * Tải cơ sở dữ liệu DB-IP Lite Country theo tháng (`dbip-country-lite-YYYY-MM.csv.gz`).
     * - Nếu đã có tệp lưu cục bộ hợp lệ trên đĩa thì nạp ngay.
     * - Qua Wi-Fi: tự động tải ngầm về tệp `.tmp`, giải nén và kiểm tra tính hợp lệ trước khi thay thế bản cũ.
     * - Qua mạng di động: chỉ tải khi người dùng đã đồng ý (`allowDbIpCellularDownload` hoặc `isCellularConfirmedByUser`).
     */
    suspend fun downloadDbIpLiteIfPermitted(
        context: android.content.Context,
        isCellularConfirmedByUser: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val dbDir = java.io.File(context.filesDir, "geoip")
        if (!dbDir.exists()) dbDir.mkdirs()
        val csvFile = java.io.File(dbDir, "dbip-country-lite.csv")
        val tmpFile = java.io.File(dbDir, "dbip-country-lite.csv.tmp")

        if (csvFile.exists() && csvFile.length() > 1024L && activeRangeTable === defaultSeedTable) {
            try {
                java.io.FileInputStream(csvFile).use { fis ->
                    if (loadCsvRangesFromStream(fis)) {
                        return@withContext true
                    }
                }
            } catch (_: Exception) {}
        }

        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
        val isCellular = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true &&
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == false

        val config = com.example.data.SettingsRepository.config.value
        if (isCellular && !config.allowDbIpCellularDownload && !isCellularConfirmedByUser) {
            return@withContext false
        }

        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val ym = String.format(java.util.Locale.US, "%04d-%02d", year, month)
        val url = "https://download.db-ip.com/free/dbip-country-lite-$ym.csv.gz"

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val bodyStream = resp.body?.byteStream() ?: return@withContext false
                java.util.zip.GZIPInputStream(bodyStream).use { gzipIn ->
                    java.io.FileOutputStream(tmpFile).use { fos ->
                        gzipIn.copyTo(fos)
                    }
                }
            }
            if (tmpFile.exists() && tmpFile.length() > 1024L) {
                val loadedOk = java.io.FileInputStream(tmpFile).use { fis ->
                    loadCsvRangesFromStream(fis)
                }
                if (loadedOk) {
                    if (csvFile.exists()) csvFile.delete()
                    tmpFile.renameTo(csvFile)
                    return@withContext true
                } else {
                    tmpFile.delete()
                }
            }
        } catch (_: Exception) {
            if (tmpFile.exists()) tmpFile.delete()
        }
        false
    }

    /**
     * Called when the in-WebView JS probe detects the actual IP seen by the browser/Xbox
     */
    fun onWebViewReported(ip: String, countryCode: String, countryName: String? = null, city: String? = null) {
        val code = countryCode.uppercase()
        val isSupported = SUPPORTED_COUNTRIES.contains(code)
        val result = GeoIpResult(
            ip = ip,
            countryCode = code,
            countryName = countryName ?: getCountryDisplayName(code),
            city = city ?: "",
            flagEmoji = getFlagEmoji(code),
            isXboxSupported = isSupported,
            isFromWebView = true
        )
        _currentGeoIp.value = result
    }

    /**
     * Checks current IP from device network (including city & coordinates for nearest regional probe selection)
     */
    suspend fun checkCurrentIp(): GeoIpResult? = withContext(Dispatchers.IO) {
        _isChecking.value = true
        var result: GeoIpResult? = null

        // 1. Try ipwho.is over HTTPS first to obtain accurate city + coordinates globally
        try {
            val req = Request.Builder()
                .url("https://ipwho.is/")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                if (json.optBoolean("success", true)) {
                    val ip = json.optString("ip", "")
                    val countryCode = json.optString("country_code", "").trim().uppercase()
                    if (countryCode.length == 2) {
                        val countryName = json.optString("country", getCountryDisplayName(countryCode))
                        val city = json.optString("city", "")
                        val lat = json.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
                        val lon = json.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                        val isSupported = SUPPORTED_COUNTRIES.contains(countryCode)

                        result = GeoIpResult(
                            ip = ip,
                            countryCode = countryCode,
                            countryName = countryName,
                            city = city,
                            flagEmoji = getFlagEmoji(countryCode),
                            isXboxSupported = isSupported,
                            latitude = lat,
                            longitude = lon
                        )
                    }
                }
            }
            resp.close()
        } catch (_: Exception) {
        }

        // 2. Fallback 1: api.country.is (fast, lightweight)
        if (result == null) {
            try {
                val req = Request.Builder()
                    .url("https://api.country.is/")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val ip = json.optString("ip", "")
                    val countryCode = json.optString("country", "").trim().uppercase()
                    if (countryCode.length == 2) {
                        val isSupported = SUPPORTED_COUNTRIES.contains(countryCode)

                        result = GeoIpResult(
                            ip = ip,
                            countryCode = countryCode,
                            countryName = getCountryDisplayName(countryCode),
                            city = "",
                            flagEmoji = getFlagEmoji(countryCode),
                            isXboxSupported = isSupported
                        )
                    }
                }
                resp.close()
            } catch (_: Exception) {
            }
        }

        // 3. Fallback 2: ipapi.co
        if (result == null) {
            try {
                val req = Request.Builder()
                    .url("https://ipapi.co/json/")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val ip = json.optString("ip", "")
                    val countryCode = json.optString("country_code", "").trim().uppercase()
                    if (countryCode.length == 2) {
                        val countryName = json.optString("country_name", getCountryDisplayName(countryCode))
                        val city = json.optString("city", "")
                        val lat = json.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
                        val lon = json.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                        val isSupported = SUPPORTED_COUNTRIES.contains(countryCode)

                        result = GeoIpResult(
                            ip = ip,
                            countryCode = countryCode,
                            countryName = countryName,
                            city = city,
                            flagEmoji = getFlagEmoji(countryCode),
                            isXboxSupported = isSupported,
                            latitude = lat,
                            longitude = lon
                        )
                    }
                }
                resp.close()
            } catch (_: Exception) {
            }
        }

        if (result != null) {
            com.example.data.AppLogger.info(
                "VPN",
                "Phát hiện vị trí mạng: ${result.ip} • ${result.countryCode} ${result.city.ifBlank { "(Chưa rõ tỉnh/thành)" }}"
            )
        } else {
            com.example.data.AppLogger.warn("VPN", "Không lấy được thông tin GeoIP từ mạng hiện tại")
        }

        _isChecking.value = false
        _currentGeoIp.value = result
        result
    }
}
