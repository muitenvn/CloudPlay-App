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
    val SUPPORTED_COUNTRIES = setOf("JP", "US", "KR", "DE", "GB", "FR", "NL", "AU", "CA", "SE", "NO", "DK", "FI", "PL", "BR", "MX")

    fun getFlagEmoji(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "JP" -> "🇯🇵"
            "KR" -> "🇰🇷"
            "US" -> "🇺🇸"
            "SG" -> "🇸🇬"
            "VN" -> "🇻🇳"
            "GB" -> "🇬🇧"
            "DE" -> "🇩🇪"
            "FR" -> "🇫🇷"
            "AU" -> "🇦🇺"
            "CA" -> "🇨🇦"
            "BR" -> "🇧🇷"
            "MX" -> "🇲🇽"
            "PL" -> "🇵🇱"
            else -> "🌐"
        }
    }

    fun getCountryDisplayName(countryCode: String): String {
        val isEn = try {
            com.example.data.SettingsRepository.config.value.isEnglishUI()
        } catch (_: Exception) {
            false
        }
        return when (countryCode.uppercase()) {
            "JP" -> if (isEn) "Japan" else "Nhật Bản (Japan)"
            "KR" -> if (isEn) "South Korea" else "Hàn Quốc (South Korea)"
            "US" -> if (isEn) "United States" else "Hoa Kỳ (United States)"
            "SG" -> "Singapore"
            "VN" -> if (isEn) "Vietnam" else "Việt Nam (Vietnam)"
            "GB" -> if (isEn) "United Kingdom" else "Vương Quốc Anh (United Kingdom)"
            "DE" -> if (isEn) "Germany" else "Đức (Germany)"
            "FR" -> if (isEn) "France" else "Pháp (France)"
            "AU" -> if (isEn) "Australia" else "Úc (Australia)"
            "CA" -> "Canada"
            "BR" -> "Brazil"
            "MX" -> "Mexico"
            "PL" -> if (isEn) "Poland" else "Ba Lan (Poland)"
            else -> countryCode
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

        // 1. Try ipwho.is over HTTPS first to obtain accurate city + coordinates (e.g. Ho Chi Minh City vs Hanoi)
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
                    val countryCode = json.optString("country_code", "VN").uppercase()
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
                    val countryCode = json.optString("country", "VN").uppercase()
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
                    val countryCode = json.optString("country_code", "VN").uppercase()
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
