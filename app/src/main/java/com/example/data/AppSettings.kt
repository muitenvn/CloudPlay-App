package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

enum class StreamGraphicsQuality(
    val id: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val targetBitrateMbps: Int,
    val deviceLoadLabel: String,
    val dataSavingNotice: String,
    val description: String
) {
    AUTO(
        id = "auto",
        title = "Tự Động (Khuyên dùng)",
        shortLabel = "Tự Động",
        subtitle = "Tự tối ưu theo mạng & giảm tải máy",
        targetBitrateMbps = 10,
        deviceLoadLabel = "⚡ Tự động giảm tải chống nóng máy",
        dataSavingNotice = "Tự động co giãn theo Wifi / 4G",
        description = "Hệ thống tự động theo dõi điều kiện mạng Wifi / 4G và độ trễ giải mã GPU của máy để tự điều chỉnh đồ họa, giữ 60 FPS mượt mà và chống nóng máy."
    ),
    DATA_SAVER(
        id = "saver",
        title = "Tiết Kiệm Dung Lượng (Thấp)",
        shortLabel = "Tiết Kiệm (5M)",
        subtitle = "Tiết kiệm data, siêu mát máy, tốn ít pin",
        targetBitrateMbps = 5,
        deviceLoadLabel = "❄️ Siêu mát máy • Tiết kiệm pin tối đa",
        dataSavingNotice = "Tiết kiệm đến 60% dung lượng data di động",
        description = "Giảm tải tối đa cho GPU và CPU điện thoại, thời gian giải mã cực nhanh dưới 4ms, máy chạy mát rượi và tốn rất ít pin."
    ),
    MEDIUM(
        id = "medium",
        title = "Trung Bình",
        shortLabel = "Trung Bình (9M)",
        subtitle = "Cân bằng độ nét và nhiệt độ máy",
        targetBitrateMbps = 9,
        deviceLoadLabel = "🌿 Máy chạy êm ái • Ít hao pin",
        dataSavingNotice = "Tiết kiệm ~40% dung lượng data",
        description = "Mức cân bằng lý tưởng cho các dòng máy tầm trung, đảm bảo 60 FPS mượt mà và giữ điện thoại không bị quá nhiệt khi chơi lâu."
    ),
    HIGH(
        id = "high",
        title = "Cao",
        shortLabel = "Cao (14M)",
        subtitle = "1080p sắc nét, chi tiết cao",
        targetBitrateMbps = 14,
        deviceLoadLabel = "⚡ Tiêu chuẩn đồ họa 1080p60",
        dataSavingNotice = "Tiêu chuẩn ~4.5GB/giờ",
        description = "Hình ảnh sắc nét rõ từng chi tiết, đổ bóng và bề mặt vật thể tuyệt đẹp cho máy có chip xử lý đồ họa khá trở lên."
    ),
    ULTRA(
        id = "ultra",
        title = "Rất Cao (Tối Đa)",
        shortLabel = "Rất Cao (20M)",
        subtitle = "Bung trọn vẹn đồ họa xCloud tối đa",
        targetBitrateMbps = 20,
        deviceLoadLabel = "🔥 Tối đa công suất GPU",
        dataSavingNotice = "Băng thông tối đa ~7GB/giờ",
        description = "Chất lượng hình ảnh đỉnh cao nhất của Xbox Cloud Gaming, tối ưu cho máy cấu hình mạnh và mạng Wifi 5GHz / 5G cực khỏe."
    )
}

enum class CellularOptimizationMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val bitrateMbps: Int,
    val deviceLoadBadge: String,
    val description: String
) {
    AUTO(
        id = "auto",
        title = "Tự Động",
        shortLabel = "Tự Động",
        subtitle = "Tự tối ưu theo điều kiện mạng & máy",
        bitrateMbps = 10,
        deviceLoadBadge = "⚡ Tự Động Tối Ưu",
        description = "Tự động nhận diện chất lượng Wi-Fi / 4G / 5G và theo dõi tải giải mã GPU của điện thoại để điều chỉnh đồ họa, giữ 60 FPS mượt mà và chống nóng máy."
    ),
    DATA_SAVER_HIGH_SPEED(
        id = "saver",
        title = "Thấp",
        shortLabel = "Tiết Kiệm (5M)",
        subtitle = "Tiết kiệm dung lượng • 5 Mbps",
        bitrateMbps = 5,
        deviceLoadBadge = "❄️ Siêu Nhẹ • Mát Máy",
        description = "Giảm tải tối đa cho CPU/GPU điện thoại (giải mã phần cứng dưới 4ms), tiết kiệm đến 60% dung lượng data (~2.2GB/giờ) và đỡ hao pin."
    ),
    MEDIUM(
        id = "medium",
        title = "Trung Bình",
        shortLabel = "Trung Bình (9M)",
        subtitle = "Cân bằng & mát máy • 9 Mbps",
        bitrateMbps = 9,
        deviceLoadBadge = "🌿 Êm Ái • Chống Nóng",
        description = "Mức cân bằng lý tưởng cho điện thoại tầm trung. Đảm bảo khung hình 60 FPS ổn định, tiết kiệm ~40% dung lượng và giữ máy mát khi chơi lâu."
    ),
    HIGH(
        id = "high",
        title = "Cao",
        shortLabel = "Cao (14M)",
        subtitle = "Sắc nét 1080p • 14 Mbps",
        bitrateMbps = 14,
        deviceLoadBadge = "✨ Chuẩn 1080p60",
        description = "Tái tạo hình ảnh Full HD 1080p60 sắc nét với chi tiết bề mặt rõ ràng, phù hợp khi mạng ổn định và điện thoại có chip xử lý khá trở lên."
    ),
    MAX_SPEED_UNCAPPED(
        id = "max",
        title = "Rất Cao",
        shortLabel = "Rất Cao (20M)",
        subtitle = "Đồ họa tối đa • 20 Mbps",
        bitrateMbps = 20,
        deviceLoadBadge = "🔥 Đồ Họa Tối Đa",
        description = "Bung trọn vẹn 20 Mbps băng thông cho chất lượng đồ họa cao nhất, phản hồi tay cầm tức thì cho máy cấu hình mạnh và mạng tốc độ cao."
    )
}

enum class NetworkTuningMode(
    val id: String,
    val title: String,
    val subtitle: String
) {
    AUTO(
        id = "auto",
        title = "Tự Động Theo Điều Kiện Mạng (Khuyên dùng)",
        subtitle = "Tự động điều chỉnh đồ họa & hiệu năng theo Wi-Fi / 4G"
    ),
    WIFI_SPEED_FIRST(
        id = "wifi_speed",
        title = "Ưu Tiên Độ Trễ Thấp & Mượt 60 FPS",
        subtitle = "Triệt tiêu trễ đệm khi mạng yếu, giảm tải GPU tức thì"
    ),
    CELLULAR_DATA_SAVER(
        id = "cell_saver",
        title = "Thấp (Tiết Kiệm Dung Lượng)",
        subtitle = "Tiết kiệm 60% dung lượng, siêu mát máy & nhẹ GPU"
    ),
    CELLULAR_MAX_SPEED(
        id = "cell_max",
        title = "Rất Cao (Đồ Họa Tối Đa)",
        subtitle = "Bung tối đa 20 Mbps cho hình ảnh sắc nét nhất"
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
    // Chế độ Đồ Họa & Hiệu Năng Máy (Mặc định: Tự Động theo điều kiện mạng & máy)
    val cellularMode: CellularOptimizationMode = CellularOptimizationMode.AUTO,
    val networkTuningMode: NetworkTuningMode = NetworkTuningMode.AUTO,
    val wifiSpeedFirstPriority: Boolean = true,
    // Chế độ máy yếu (tùy chọn do người dùng bật, không tự áp cho mọi máy)
    val lowEndDeviceMode: Boolean = false,
    // Điều khoản người dùng & Tuyên bố bản quyền nguồn mở GitHub (Better-xCloud MIT)
    val userAcceptedTerms: Boolean = false
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
    private var prefs: SharedPreferences? = null
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences("cloudplay_settings_store", Context.MODE_PRIVATE)
        prefs = sp

        val loaded = AppConfig(
            appLanguage = try {
                AppLanguage.valueOf(sp.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name)
            } catch (_: Exception) { AppLanguage.SYSTEM },
            useSystemLanguage = sp.getBoolean("use_system_language", true),
            customLocale = sp.getString("custom_locale", "en-US") ?: "en-US",
            smartVpnEnabled = sp.getBoolean("smart_vpn_enabled", true),
            autoBypassEnabled = sp.getBoolean("auto_bypass_enabled", true),
            autoBypassDelaySeconds = sp.getInt("auto_bypass_delay", 2),
            forceDesktopUserAgent = sp.getBoolean("force_desktop_ua", true),
            clarityBoostEnabled = sp.getBoolean("clarity_boost_enabled", true),
            clarityPreset = try {
                ClarityPreset.valueOf(sp.getString("clarity_preset", ClarityPreset.BALANCED.name) ?: ClarityPreset.BALANCED.name)
            } catch (_: Exception) { ClarityPreset.BALANCED },
            maxBitrateMbps = sp.getInt("max_bitrate", 15),
            antiAfkKeepAlive = sp.getBoolean("anti_afk", true),
            inputPollingBoost = sp.getBoolean("input_polling_boost", true),
            showVirtualController = sp.getBoolean("show_virtual_controller", false),
            systemRevolutionEnabled = sp.getBoolean("system_revolution", true),
            stealthHeadersEnabled = sp.getBoolean("stealth_headers", true),
            autoHealingProxyMesh = sp.getBoolean("auto_healing_mesh", true),
            sdpBitrateBoostEnabled = sp.getBoolean("sdp_bitrate_boost", true),
            zeroDelayDirectHandshake = sp.getBoolean("zero_delay_handshake", true),
            antiJapaneseEnforcer = sp.getBoolean("anti_jp_enforcer", true),
            xcloudTranslateEnabled = sp.getBoolean("xcloud_translate", true),
            preserveGameTitles = sp.getBoolean("preserve_titles", true),
            bilingualMode = sp.getBoolean("bilingual_mode", false),
            githubDictionaryEnabled = sp.getBoolean("github_dict", true),
            hideStreamStatsOverlay = sp.getBoolean("hide_stream_stats", true),
            cellularMode = try {
                CellularOptimizationMode.valueOf(
                    sp.getString("graphics_quality_mode_v2", CellularOptimizationMode.AUTO.name)
                        ?: CellularOptimizationMode.AUTO.name
                )
            } catch (_: Exception) { CellularOptimizationMode.AUTO },
            networkTuningMode = try {
                NetworkTuningMode.valueOf(sp.getString("network_tuning_mode", NetworkTuningMode.AUTO.name) ?: NetworkTuningMode.AUTO.name)
            } catch (_: Exception) { NetworkTuningMode.AUTO },
            wifiSpeedFirstPriority = sp.getBoolean("wifi_speed_first", true),
            lowEndDeviceMode = sp.getBoolean("low_end_device_mode", false),
            userAcceptedTerms = sp.getBoolean("user_accepted_terms", false)
        )
        _config.value = loaded
    }

    private fun persist(c: AppConfig) {
        prefs?.edit()?.apply {
            putString("app_language", c.appLanguage.name)
            putBoolean("use_system_language", c.useSystemLanguage)
            putString("custom_locale", c.customLocale)
            putBoolean("smart_vpn_enabled", c.smartVpnEnabled)
            putBoolean("auto_bypass_enabled", c.autoBypassEnabled)
            putInt("auto_bypass_delay", c.autoBypassDelaySeconds)
            putBoolean("force_desktop_ua", c.forceDesktopUserAgent)
            putBoolean("clarity_boost_enabled", c.clarityBoostEnabled)
            putString("clarity_preset", c.clarityPreset.name)
            putInt("max_bitrate", c.maxBitrateMbps)
            putBoolean("anti_afk", c.antiAfkKeepAlive)
            putBoolean("input_polling_boost", c.inputPollingBoost)
            putBoolean("show_virtual_controller", c.showVirtualController)
            putBoolean("system_revolution", c.systemRevolutionEnabled)
            putBoolean("stealth_headers", c.stealthHeadersEnabled)
            putBoolean("auto_healing_mesh", c.autoHealingProxyMesh)
            putBoolean("sdp_bitrate_boost", c.sdpBitrateBoostEnabled)
            putBoolean("zero_delay_handshake", c.zeroDelayDirectHandshake)
            putBoolean("anti_jp_enforcer", c.antiJapaneseEnforcer)
            putBoolean("xcloud_translate", c.xcloudTranslateEnabled)
            putBoolean("preserve_titles", c.preserveGameTitles)
            putBoolean("bilingual_mode", c.bilingualMode)
            putBoolean("github_dict", c.githubDictionaryEnabled)
            putBoolean("hide_stream_stats", c.hideStreamStatsOverlay)
            putString("cellular_mode", c.cellularMode.name)
            putString("graphics_quality_mode_v2", c.cellularMode.name)
            putString("network_tuning_mode", c.networkTuningMode.name)
            putBoolean("wifi_speed_first", c.wifiSpeedFirstPriority)
            putBoolean("low_end_device_mode", c.lowEndDeviceMode)
            putBoolean("user_accepted_terms", c.userAcceptedTerms)
            apply()
        }
    }

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

    fun update(newConfig: AppConfig) {
        _config.update { newConfig }
        persist(newConfig)
    }

    fun updateAppLanguage(language: AppLanguage) {
        _config.update { it.copy(appLanguage = language) }
        persist(_config.value)
    }

    fun updateUseSystemLanguage(useSystem: Boolean) {
        _config.update { it.copy(useSystemLanguage = useSystem) }
        persist(_config.value)
    }

    fun updateCustomLocale(locale: String) {
        val parsed = parseLocaleFromInput(locale)
        _config.update {
            it.copy(
                customLocale = parsed,
                useSystemLanguage = false
            )
        }
        persist(_config.value)
    }

    fun updateSmartVpnEnabled(enabled: Boolean) {
        _config.update { it.copy(smartVpnEnabled = enabled) }
        persist(_config.value)
    }

    fun updateAutoBypass(enabled: Boolean) {
        _config.update { it.copy(autoBypassEnabled = enabled) }
        persist(_config.value)
    }

    fun updateAutoBypassDelay(seconds: Int) {
        _config.update { it.copy(autoBypassDelaySeconds = seconds) }
        persist(_config.value)
    }

    fun updateDesktopUserAgent(enabled: Boolean) {
        _config.update { it.copy(forceDesktopUserAgent = enabled) }
        persist(_config.value)
    }

    fun updateClarityBoost(enabled: Boolean) {
        _config.update { it.copy(clarityBoostEnabled = enabled) }
        persist(_config.value)
    }

    fun updateClarityPreset(preset: ClarityPreset) {
        _config.update { it.copy(clarityPreset = preset) }
        persist(_config.value)
    }

    fun updateMaxBitrate(mbps: Int) {
        _config.update { it.copy(maxBitrateMbps = mbps) }
        persist(_config.value)
    }

    fun updateAntiAfk(enabled: Boolean) {
        _config.update { it.copy(antiAfkKeepAlive = enabled) }
        persist(_config.value)
    }

    fun updateInputPollingBoost(enabled: Boolean) {
        _config.update { it.copy(inputPollingBoost = enabled) }
        persist(_config.value)
    }

    fun updateShowVirtualController(enabled: Boolean) {
        _config.update { it.copy(showVirtualController = enabled) }
        persist(_config.value)
    }

    fun updateSystemRevolution(enabled: Boolean) {
        _config.update { it.copy(systemRevolutionEnabled = enabled) }
        persist(_config.value)
    }

    fun updateStealthHeaders(enabled: Boolean) {
        _config.update { it.copy(stealthHeadersEnabled = enabled) }
        persist(_config.value)
    }

    fun updateAutoHealingProxyMesh(enabled: Boolean) {
        _config.update { it.copy(autoHealingProxyMesh = enabled) }
        persist(_config.value)
    }

    fun updateSdpBitrateBoost(enabled: Boolean) {
        _config.update { it.copy(sdpBitrateBoostEnabled = enabled) }
        persist(_config.value)
    }

    fun updateZeroDelayDirectHandshake(enabled: Boolean) {
        _config.update { it.copy(zeroDelayDirectHandshake = enabled) }
        persist(_config.value)
    }

    fun updateAntiJapaneseEnforcer(enabled: Boolean) {
        _config.update { it.copy(antiJapaneseEnforcer = enabled) }
        persist(_config.value)
    }

    fun updateXcloudTranslate(enabled: Boolean) {
        _config.update { it.copy(xcloudTranslateEnabled = enabled) }
        persist(_config.value)
    }

    fun updatePreserveGameTitles(enabled: Boolean) {
        _config.update { it.copy(preserveGameTitles = enabled) }
        persist(_config.value)
    }

    fun updateBilingualMode(enabled: Boolean) {
        _config.update { it.copy(bilingualMode = enabled) }
        persist(_config.value)
    }

    fun updateGithubDictionaryEnabled(enabled: Boolean) {
        _config.update { it.copy(githubDictionaryEnabled = enabled) }
        persist(_config.value)
    }

    fun updateHideStreamStatsOverlay(hide: Boolean) {
        _config.update { it.copy(hideStreamStatsOverlay = hide) }
        persist(_config.value)
    }

    fun updateCellularMode(mode: CellularOptimizationMode) {
        _config.update { it.copy(cellularMode = mode) }
        persist(_config.value)
    }

    fun updateNetworkTuningMode(mode: NetworkTuningMode) {
        _config.update { it.copy(networkTuningMode = mode) }
        persist(_config.value)
    }

    fun updateWifiSpeedFirstPriority(enabled: Boolean) {
        _config.update { it.copy(wifiSpeedFirstPriority = enabled) }
        persist(_config.value)
    }

    /**
     * Cuộc Cách Mạng Hệ Thống 1.0: Tối ưu hóa toàn diện toàn bộ 5 tầng chỉ với 1 chạm.
     */
    fun optimizeEntireSystem() {
        val optimized = _config.value.copy(
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
        _config.update { optimized }
        persist(optimized)
    }

    fun updateTargetRegionUrl(url: String) {
        val match = Regex("""xbox\.com/([a-zA-Z]{2}-[a-zA-Z]{2})""").find(url)
        if (match != null) {
            updateCustomLocale(match.groupValues[1])
        }
    }

    fun updateUserAcceptedTerms(accepted: Boolean) {
        _config.update { it.copy(userAcceptedTerms = accepted) }
        persist(_config.value)
    }

    fun updateLowEndDeviceMode(enabled: Boolean) {
        _config.update { it.copy(lowEndDeviceMode = enabled) }
        persist(_config.value)
    }
}
