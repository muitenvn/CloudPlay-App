package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class XboxLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
)

val POPULAR_XBOX_LANGUAGES = listOf(
    XboxLanguage("en-US", "Tiếng Anh (Mỹ)", "English (United States)", "🇺🇸"),
    XboxLanguage("en-GB", "Tiếng Anh (Anh)", "English (United Kingdom)", "🇬🇧"),
    XboxLanguage("ja-JP", "Tiếng Nhật", "日本語", "🇯🇵"),
    XboxLanguage("ko-KR", "Tiếng Hàn", "한국어", "🇰🇷"),
    XboxLanguage("zh-TW", "Tiếng Trung (Phồn thể)", "繁體中文", "🇹🇼"),
    XboxLanguage("fr-FR", "Tiếng Pháp", "Français", "🇫🇷"),
    XboxLanguage("de-DE", "Tiếng Đức", "Deutsch", "🇩🇪"),
    XboxLanguage("es-ES", "Tiếng Tây Ban Nha", "Español", "🇪🇸"),
    XboxLanguage("pt-BR", "Tiếng Bồ Đào Nha", "Português (Brasil)", "🇧🇷"),
    XboxLanguage("it-IT", "Tiếng Ý", "Italiano", "🇮🇹"),
    XboxLanguage("pl-PL", "Tiếng Ba Lan", "Polski", "🇵🇱")
)

enum class ClarityPreset(val displayName: String, val description: String) {
    OFF("Tắt", "Hình ảnh nguyên bản không can thiệp"),
    BALANCED("Cân Bằng", "Tăng nét nhẹ + 8% tương phản (Khuyên dùng)"),
    ULTRA_SHARP("Ultra Sharp", "Làm nét cạnh tối đa cho game 3D/FPS"),
    OLED_PUNCH("OLED Punch", "Màu sắc rực rỡ & độ sâu màu cho màn AMOLED/OLED")
}

enum class CellularOptimizationMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val bitrateMbps: Int,
    val description: String
) {
    DATA_SAVER_HIGH_SPEED(
        id = "saver",
        title = "4G Tiết Kiệm (Tốc Độ Cao)",
        subtitle = "Tiết kiệm 50% data, 60 FPS, độ trễ 0ms",
        bitrateMbps = 8,
        description = "Giảm tiêu hao dung lượng còn ~3.2GB/h nhưng vẫn giữ 60 FPS phản hồi tức thì, H.264 Baseline chống nóng máy cho mọi chip 4G."
    ),
    MAX_SPEED_UNCAPPED(
        id = "max",
        title = "4G Có Nhiêu Sài Hết (Max Speed)",
        subtitle = "Bung hết băng thông 20 Mbps, mượt mà tối đa",
        bitrateMbps = 20,
        description = "Bung trọn vẹn băng thông 4G/5G tốc độ cao, hình ảnh sắc nét và giảm thiểu đệm WebRTC để phản hồi tay cầm tức thì."
    )
}

enum class NetworkTuningMode(
    val id: String,
    val title: String,
    val subtitle: String
) {
    AUTO(
        id = "auto",
        title = "Tự Động Thông Minh (Khuyên dùng)",
        subtitle = "Tự nhận diện Wifi & 4G để tinh chỉnh tốc độ tối ưu nhất"
    ),
    WIFI_SPEED_FIRST(
        id = "wifi_speed",
        title = "Wifi Tự Động (Tốc độ #1, Đồ họa #2)",
        subtitle = "Ưu tiên độ trễ cực thấp, đồ họa tự co giãn theo chất lượng sóng"
    ),
    CELLULAR_DATA_SAVER(
        id = "cell_saver",
        title = "Cố định 4G Tiết Kiệm (Tốc độ cao)",
        subtitle = "Tiết kiệm 50% data, giữ 60 FPS mượt mà"
    ),
    CELLULAR_MAX_SPEED(
        id = "cell_max",
        title = "Cố định 4G Có Nhiêu Sài Hết",
        subtitle = "Bung tối đa 20 Mbps không giới hạn"
    )
}

enum class AppLanguage(val code: String, val title: String) {
    SYSTEM("system", "Theo hệ thống (Tự động)"),
    VIETNAMESE("vi", "Tiếng Việt"),
    ENGLISH("en", "English")
}

data class AppConfig(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM, // Tự động chọn tiếng Việt / tiếng Anh theo hệ thống
    val smartVpnEnabled: Boolean = true, // Tự động kiểm tra IP & điều khiển bật/tắt thông minh
    val autoBypassEnabled: Boolean = true,
    val autoBypassDelaySeconds: Int = 2,
    val autoReconnectOnMenu: Boolean = true,
    val forceDesktopUserAgent: Boolean = true,
    val clarityBoostEnabled: Boolean = true,
    val clarityPreset: ClarityPreset = ClarityPreset.BALANCED,
    val maxBitrateMbps: Int = 15, // 1080p high bitrate stream
    val antiAfkKeepAlive: Boolean = true, // Chống bị ngắt phiên AFK 15 phút
    val inputPollingBoost: Boolean = true, // Tăng tần số lấy mẫu tay cầm 120Hz/240Hz
    val showFloatingHud: Boolean = true,
    val showVirtualController: Boolean = false,
    val useSystemLanguage: Boolean = true,
    val customLocale: String = "en-US",
    val preferredServerId: String = "jp_tokyo_linode_1",
    // Cuộc Cách Mạng Hệ Thống 1.0 (System Revolution 1.0)
    val systemRevolutionEnabled: Boolean = true,
    val stealthHeadersEnabled: Boolean = true,
    val autoHealingProxyMesh: Boolean = true,
    val sdpBitrateBoostEnabled: Boolean = true,
    val zeroDelayDirectHandshake: Boolean = true,
    val antiJapaneseEnforcer: Boolean = true,
    // Bộ Dịch Tiếng Việt Tối Ưu Riêng Cho Xbox Cloud (Native xCloud Translator)
    val xcloudTranslateEnabled: Boolean = true,
    val preserveGameTitles: Boolean = true,
    val bilingualMode: Boolean = false,
    val githubDictionaryEnabled: Boolean = true,
    // Ẩn lớp phủ bảng thông số kỹ thuật đè màn hình (Decode Profile / Stream Stats HUD)
    val hideStreamStatsOverlay: Boolean = true,
    // Tối Ưu Mạng 4G (Tiết kiệm vs Có nhiêu sài hết) & Wifi Tốc Độ Hàng Đầu
    val cellularMode: CellularOptimizationMode = CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
    val networkTuningMode: NetworkTuningMode = NetworkTuningMode.AUTO,
    val wifiSpeedFirstPriority: Boolean = true
) {
    val targetRegionUrl: String
        get() = "https://www.xbox.com/${getEffectiveLocale()}/play"

    fun getEffectiveLocale(): String {
        return if (useSystemLanguage) {
            SettingsRepository.resolveSystemXboxLocale()
        } else {
            customLocale.ifBlank { "en-US" }
        }
    }
}

object SettingsRepository {
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    /**
     * Resolves system language into an officially supported Xbox Cloud Gaming locale.
     * If the system language is unsupported (e.g. Vietnamese 'vi'), automatically falls back to 'en-US'.
     */
    fun resolveSystemXboxLocale(): String {
        val sysLocale = Locale.getDefault()
        val lang = sysLocale.language.lowercase()
        val country = sysLocale.country.uppercase()

        return when (lang) {
            "vi" -> "en-US" // Tiếng Việt chưa hỗ trợ xCloud -> Fallback tiếng Anh (en-US)
            "en" -> when (country) {
                "GB" -> "en-GB"
                "CA" -> "en-CA"
                "AU" -> "en-AU"
                "NZ" -> "en-NZ"
                else -> "en-US"
            }
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            "zh" -> if (country in setOf("CN", "SG")) "zh-CN" else "zh-TW"
            "fr" -> if (country == "CA") "fr-CA" else "fr-FR"
            "de" -> "de-DE"
            "es" -> if (country in setOf("MX", "AR", "CO", "CL")) "es-MX" else "es-ES"
            "pt" -> if (country == "PT") "pt-PT" else "pt-BR"
            "it" -> "it-IT"
            "pl" -> "pl-PL"
            "nl" -> "nl-NL"
            "sv" -> "sv-SE"
            "da" -> "da-DK"
            "nb", "no" -> "nb-NO"
            "fi" -> "fi-FI"
            "cs" -> "cs-CZ"
            "hu" -> "hu-HU"
            "tr" -> "tr-TR"
            else -> "en-US" // Ngôn ngữ khác -> Fallback sang tiếng Anh (en-US)
        }
    }

    /**
     * Extracts and validates locale codes from user input such as "xbox.com/en-US", "en-US", "ko-KR", etc.
     */
    fun parseLocaleFromInput(input: String): String {
        val trimmed = input.trim()
        val match = Regex("""([a-zA-Z]{2})[-_]([a-zA-Z]{2})""").find(trimmed)
        if (match != null) {
            val l = match.groupValues[1].lowercase()
            val r = match.groupValues[2].uppercase()
            return "$l-$r"
        }
        if (trimmed.length == 2 && trimmed.all { it.isLetter() }) {
            return when (trimmed.lowercase()) {
                "en" -> "en-US"
                "ja" -> "ja-JP"
                "ko" -> "ko-KR"
                "fr" -> "fr-FR"
                "de" -> "de-DE"
                "es" -> "es-ES"
                "pt" -> "pt-BR"
                "it" -> "it-IT"
                "zh" -> "zh-TW"
                else -> "en-US"
            }
        }
        return if (trimmed.isNotBlank()) trimmed else "en-US"
    }

    fun formatXboxUrlWithLocale(url: String, targetLocale: String): String {
        if (!url.contains("xbox.com", ignoreCase = true)) return url

        val localePattern = Regex("""(https?://(?:www\.)?xbox\.com)/([a-zA-Z]{2}-[a-zA-Z]{2})(/.*)?""")
        val match = localePattern.find(url)
        if (match != null) {
            val domain = match.groupValues[1]
            val rest = match.groupValues[3] ?: ""
            return "$domain/$targetLocale$rest"
        }

        val noLocalePattern = Regex("""(https?://(?:www\.)?xbox\.com)(/play.*)?""")
        val noLocaleMatch = noLocalePattern.find(url)
        if (noLocaleMatch != null) {
            val domain = noLocaleMatch.groupValues[1]
            val rest = noLocaleMatch.groupValues[2] ?: "/play"
            return "$domain/$targetLocale$rest"
        }

        return url
    }

    fun getUrlForCountry(countryCode: String): String {
        val effectiveLocale = _config.value.getEffectiveLocale()
        return "https://www.xbox.com/$effectiveLocale/play"
    }

    fun adaptPlayUrl(url: String, countryCode: String? = null): String {
        val effectiveLocale = _config.value.getEffectiveLocale()
        return formatXboxUrlWithLocale(url, effectiveLocale)
    }

    fun updateAppLanguage(language: AppLanguage) {
        _config.value = _config.value.copy(appLanguage = language)
    }

    fun updateUseSystemLanguage(useSystem: Boolean) {
        _config.value = _config.value.copy(useSystemLanguage = useSystem)
    }

    fun updateCustomLocale(locale: String) {
        val parsed = parseLocaleFromInput(locale)
        _config.value = _config.value.copy(
            customLocale = parsed,
            useSystemLanguage = false
        )
    }

    fun updateSmartVpnEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(smartVpnEnabled = enabled)
    }

    fun updateAutoBypass(enabled: Boolean) {
        _config.value = _config.value.copy(autoBypassEnabled = enabled)
    }

    fun updateAutoBypassDelay(seconds: Int) {
        _config.value = _config.value.copy(autoBypassDelaySeconds = seconds)
    }

    fun updateDesktopUserAgent(enabled: Boolean) {
        _config.value = _config.value.copy(forceDesktopUserAgent = enabled)
    }

    fun updateClarityBoost(enabled: Boolean) {
        _config.value = _config.value.copy(clarityBoostEnabled = enabled)
    }

    fun updateClarityPreset(preset: ClarityPreset) {
        _config.value = _config.value.copy(clarityPreset = preset)
    }

    fun updateMaxBitrate(mbps: Int) {
        _config.value = _config.value.copy(maxBitrateMbps = mbps)
    }

    fun updateAntiAfk(enabled: Boolean) {
        _config.value = _config.value.copy(antiAfkKeepAlive = enabled)
    }

    fun updateInputPollingBoost(enabled: Boolean) {
        _config.value = _config.value.copy(inputPollingBoost = enabled)
    }

    fun updateShowVirtualController(enabled: Boolean) {
        _config.value = _config.value.copy(showVirtualController = enabled)
    }

    fun updateSystemRevolution(enabled: Boolean) {
        _config.value = _config.value.copy(systemRevolutionEnabled = enabled)
    }

    fun updateStealthHeaders(enabled: Boolean) {
        _config.value = _config.value.copy(stealthHeadersEnabled = enabled)
    }

    fun updateAutoHealingProxyMesh(enabled: Boolean) {
        _config.value = _config.value.copy(autoHealingProxyMesh = enabled)
    }

    fun updateSdpBitrateBoost(enabled: Boolean) {
        _config.value = _config.value.copy(sdpBitrateBoostEnabled = enabled)
    }

    fun updateZeroDelayDirectHandshake(enabled: Boolean) {
        _config.value = _config.value.copy(zeroDelayDirectHandshake = enabled)
    }

    fun updateAntiJapaneseEnforcer(enabled: Boolean) {
        _config.value = _config.value.copy(antiJapaneseEnforcer = enabled)
    }

    fun updateXcloudTranslate(enabled: Boolean) {
        _config.value = _config.value.copy(xcloudTranslateEnabled = enabled)
    }

    fun updatePreserveGameTitles(enabled: Boolean) {
        _config.value = _config.value.copy(preserveGameTitles = enabled)
    }

    fun updateBilingualMode(enabled: Boolean) {
        _config.value = _config.value.copy(bilingualMode = enabled)
    }

    fun updateGithubDictionaryEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(githubDictionaryEnabled = enabled)
    }

    fun updateHideStreamStatsOverlay(hide: Boolean) {
        _config.value = _config.value.copy(hideStreamStatsOverlay = hide)
    }

    fun updateCellularMode(mode: CellularOptimizationMode) {
        _config.value = _config.value.copy(cellularMode = mode)
    }

    fun updateNetworkTuningMode(mode: NetworkTuningMode) {
        _config.value = _config.value.copy(networkTuningMode = mode)
    }

    fun updateWifiSpeedFirstPriority(enabled: Boolean) {
        _config.value = _config.value.copy(wifiSpeedFirstPriority = enabled)
    }

    /**
     * Cuộc Cách Mạng Hệ Thống 1.0: Tối ưu hóa toàn diện toàn bộ 5 tầng chỉ với 1 chạm.
     */
    fun optimizeEntireSystem() {
        _config.value = _config.value.copy(
            systemRevolutionEnabled = true,
            stealthHeadersEnabled = true,
            autoHealingProxyMesh = true,
            sdpBitrateBoostEnabled = true,
            zeroDelayDirectHandshake = true,
            antiJapaneseEnforcer = true,
            xcloudTranslateEnabled = true,
            preserveGameTitles = true,
            smartVpnEnabled = true,
            autoBypassEnabled = true,
            autoBypassDelaySeconds = 2,
            forceDesktopUserAgent = true,
            clarityBoostEnabled = true,
            clarityPreset = ClarityPreset.BALANCED,
            maxBitrateMbps = 15,
            antiAfkKeepAlive = true,
            inputPollingBoost = true,
            cellularMode = CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
            networkTuningMode = NetworkTuningMode.AUTO,
            wifiSpeedFirstPriority = true
        )
    }

    fun updateTargetRegionUrl(url: String) {
        val match = Regex("""xbox\.com/([a-zA-Z]{2}-[a-zA-Z]{2})""").find(url)
        if (match != null) {
            updateCustomLocale(match.groupValues[1])
        }
    }
}
