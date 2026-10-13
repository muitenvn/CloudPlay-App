package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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

        // Try api.country.is first (fastest, lightweight)
        try {
            val url = URL("https://api.country.is/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(response)
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
            conn.disconnect()
        } catch (e: Exception) {
            // Fallback 1: ipwho.is
            try {
                val url = URL("http://ipwho.is/")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(response)
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
                conn.disconnect()
            } catch (ignored: Exception) {
                // Fallback 2: ipapi.co
                try {
                    val url = URL("https://ipapi.co/json/")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    if (conn.responseCode == 200) {
                        val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        val json = JSONObject(response)
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
                    conn.disconnect()
                } catch (ignored2: Exception) {
                }
            }
        }

        // If completely offline or DNS block, default to device locale country so user is never stuck
        if (result == null) {
            val localCountry = java.util.Locale.getDefault().country.ifBlank { "VN" }.uppercase()
            val isSupported = SUPPORTED_COUNTRIES.contains(localCountry)
            result = GeoIpResult(
                ip = "127.0.0.1 (Mạng nội bộ)",
                countryCode = localCountry,
                countryName = getCountryDisplayName(localCountry),
                city = "Thiết bị",
                flagEmoji = getFlagEmoji(localCountry),
                isXboxSupported = isSupported
            )
        }

        _isChecking.value = false
        if (result != null) {
            _currentGeoIp.value = result
        }
        result
    }
}
