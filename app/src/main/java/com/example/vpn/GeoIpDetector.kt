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
        return when (countryCode.uppercase()) {
            "JP" -> "🇯🇵"
            "KR" -> "🇰🇷"
            "US" -> "🇺🇸"
            "SG" -> "🇸🇬"
            "VN" -> "🇻🇳"
            "GB", "UK" -> "🇬🇧"
            "DE" -> "🇩🇪"
            "FR" -> "🇫🇷"
            "ES" -> "🇪🇸"
            "IT" -> "🇮🇹"
            "PT" -> "🇵🇹"
            "NL" -> "🇳🇱"
            "AU" -> "🇦🇺"
            "NZ" -> "🇳🇿"
            "CA" -> "🇨🇦"
            "BR" -> "🇧🇷"
            "MX" -> "🇲🇽"
            "AR" -> "🇦🇷"
            "CL" -> "🇨🇱"
            "CO" -> "🇨🇴"
            "PL" -> "🇵🇱"
            "SE" -> "🇸🇪"
            "NO" -> "🇳🇴"
            "DK" -> "🇩🇰"
            "FI" -> "🇫🇮"
            "TR" -> "🇹🇷"
            "RU" -> "🇷🇺"
            "IN" -> "🇮🇳"
            "ID" -> "🇮🇩"
            "TH" -> "🇹🇭"
            "MY" -> "🇲🇾"
            "PH" -> "🇵🇭"
            "TW" -> "🇹🇼"
            "HK" -> "🇭🇰"
            "CN" -> "🇨🇳"
            "AE" -> "🇦🇪"
            "SA" -> "🇸🇦"
            else -> "🌐"
        }
    }

    fun getCountryDisplayName(countryCode: String): String {
        val isEn = try {
            com.example.data.SettingsRepository.config.value.isEnglishUI()
        } catch (_: Exception) {
            true
        }
        return when (countryCode.uppercase()) {
            "JP" -> if (isEn) "Japan" else "Nhật Bản (Japan)"
            "KR" -> if (isEn) "South Korea" else "Hàn Quốc (South Korea)"
            "US" -> if (isEn) "United States" else "Hoa Kỳ (United States)"
            "SG" -> "Singapore"
            "VN" -> if (isEn) "Vietnam" else "Việt Nam (Vietnam)"
            "GB", "UK" -> if (isEn) "United Kingdom" else "Vương Quốc Anh (UK)"
            "DE" -> if (isEn) "Germany" else "Đức (Germany)"
            "FR" -> if (isEn) "France" else "Pháp (France)"
            "ES" -> if (isEn) "Spain" else "Tây Ban Nha (Spain)"
            "IT" -> if (isEn) "Italy" else "Ý (Italy)"
            "PT" -> if (isEn) "Portugal" else "Bồ Đào Nha (Portugal)"
            "NL" -> if (isEn) "Netherlands" else "Hà Lan (Netherlands)"
            "AU" -> if (isEn) "Australia" else "Úc (Australia)"
            "NZ" -> "New Zealand"
            "CA" -> "Canada"
            "BR" -> "Brazil"
            "MX" -> "Mexico"
            "AR" -> "Argentina"
            "CL" -> "Chile"
            "CO" -> "Colombia"
            "PL" -> if (isEn) "Poland" else "Ba Lan (Poland)"
            "TR" -> if (isEn) "Turkey" else "Thổ Nhĩ Kỳ (Turkey)"
            "RU" -> if (isEn) "Russia" else "Nga (Russia)"
            "IN" -> if (isEn) "India" else "Ấn Độ (India)"
            "ID" -> "Indonesia"
            "TH" -> if (isEn) "Thailand" else "Thái Lan (Thailand)"
            "MY" -> "Malaysia"
            "PH" -> "Philippines"
            "TW" -> if (isEn) "Taiwan" else "Đài Loan (Taiwan)"
            "HK" -> "Hong Kong"
            "CN" -> if (isEn) "China" else "Trung Quốc (China)"
            else -> countryCode.ifBlank { "Global" }
        }
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
