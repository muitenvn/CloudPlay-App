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
    val timestamp: Long = System.currentTimeMillis()
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

    // Xbox Cloud officially supported countries
    val SUPPORTED_COUNTRIES = setOf("JP", "US", "KR", "SG", "DE", "GB", "FR", "NL", "AU", "CA", "SE", "NO", "DK", "FI", "PL", "BR", "MX")

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
        return when (countryCode.uppercase()) {
            "JP" -> "Nhật Bản (Japan)"
            "KR" -> "Hàn Quốc (South Korea)"
            "US" -> "Hoa Kỳ (United States)"
            "SG" -> "Singapore"
            "VN" -> "Việt Nam (Vietnam)"
            "GB" -> "Vương Quốc Anh (United Kingdom)"
            "DE" -> "Đức (Germany)"
            "FR" -> "Pháp (France)"
            "AU" -> "Úc (Australia)"
            "CA" -> "Canada"
            "BR" -> "Brazil"
            "MX" -> "Mexico"
            "PL" -> "Ba Lan (Poland)"
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
     * Checks current IP from device network
     */
    suspend fun checkCurrentIp(): GeoIpResult? = withContext(Dispatchers.IO) {
        _isChecking.value = true
        var result: GeoIpResult? = null

        // 1. Try api.country.is first (fastest, lightweight)
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
        } catch (_: Exception) {
            // Fallback 1: ipwho.is
            try {
                val req = Request.Builder()
                    .url("http://ipwho.is/")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val ip = json.optString("ip", "")
                    val countryCode = json.optString("country_code", "VN").uppercase()
                    val countryName = json.optString("country", getCountryDisplayName(countryCode))
                    val city = json.optString("city", "")
                    val isSupported = SUPPORTED_COUNTRIES.contains(countryCode)

                    result = GeoIpResult(
                        ip = ip,
                        countryCode = countryCode,
                        countryName = countryName,
                        city = city,
                        flagEmoji = getFlagEmoji(countryCode),
                        isXboxSupported = isSupported
                    )
                }
            } catch (_: Exception) {
                // Fallback 2: ipapi.co
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
                        val isSupported = SUPPORTED_COUNTRIES.contains(countryCode)

                        result = GeoIpResult(
                            ip = ip,
                            countryCode = countryCode,
                            countryName = countryName,
                            city = city,
                            flagEmoji = getFlagEmoji(countryCode),
                            isXboxSupported = isSupported
                        )
                    }
                } catch (_: Exception) {
                }
            }
        }

        // If completely offline or DNS block, default to device locale country so user is never stuck
        val finalResult = result ?: run {
            val localCountry = java.util.Locale.getDefault().country.ifBlank { "VN" }.uppercase()
            val isSupported = SUPPORTED_COUNTRIES.contains(localCountry)
            GeoIpResult(
                ip = "127.0.0.1 (Mạng nội bộ)",
                countryCode = localCountry,
                countryName = getCountryDisplayName(localCountry),
                city = "Thiết bị",
                flagEmoji = getFlagEmoji(localCountry),
                isXboxSupported = isSupported
            )
        }

        _isChecking.value = false
        _currentGeoIp.value = finalResult
        finalResult
    }
}
