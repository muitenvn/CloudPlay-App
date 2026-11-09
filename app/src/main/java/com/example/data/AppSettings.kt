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
    val flag: String,
    val displayNameEn: String = nativeName
) {
    fun localizedName(isEn: Boolean): String = if (isEn) displayNameEn else displayName
}

val POPULAR_XBOX_LANGUAGES = listOf(
    XboxLanguage("en-US", "Tiếng Anh (Mỹ)", "English (United States)", "🇺🇸", "English (United States)"),
    XboxLanguage("en-GB", "Tiếng Anh (Anh)", "English (United Kingdom)", "🇬🇧", "English (United Kingdom)"),
    XboxLanguage("es-ES", "Tiếng Tây Ban Nha", "Español (España)", "🇪🇸", "Spanish (Español)"),
    XboxLanguage("es-MX", "Tiếng Tây Ban Nha (Mỹ Latin)", "Español (México)", "🇲🇽", "Spanish (Latinoamérica)"),
    XboxLanguage("pt-BR", "Tiếng Bồ Đào Nha (Brazil)", "Português (Brasil)", "🇧🇷", "Portuguese (Português)"),
    XboxLanguage("fr-FR", "Tiếng Pháp", "Français", "🇫🇷", "French (Français)"),
    XboxLanguage("de-DE", "Tiếng Đức", "Deutsch", "🇩🇪", "German (Deutsch)"),
    XboxLanguage("ja-JP", "Tiếng Nhật", "日本語", "🇯🇵", "Japanese (日本語)"),
    XboxLanguage("ko-KR", "Tiếng Hàn", "한국어", "🇰🇷", "Korean (한국어)"),
    XboxLanguage("zh-TW", "Tiếng Trung (Phồn thể)", "繁體中文", "🇹🇼", "Chinese (繁體中文)"),
    XboxLanguage("it-IT", "Tiếng Ý", "Italiano", "🇮🇹", "Italian (Italiano)"),
    XboxLanguage("pl-PL", "Tiếng Ba Lan", "Polski", "🇵🇱", "Polish (Polski)"),
    XboxLanguage("tr-TR", "Tiếng Thổ Nhĩ Kỳ", "Türkçe", "🇹🇷", "Turkish (Türkçe)")
)

/**
 * Danh sách trắng thị trường Xbox đã xác nhận chơi được ban đầu: chỉ giữ en-US.
 */
val INITIAL_VERIFIED_XBOX_MARKETS: Set<String> = setOf("en-US")

data class TranslateLanguageOption(
    val code: String,
    val titleVi: String,
    val titleEn: String,
    val flag: String
) {
    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
}

val TRANSLATE_LANGUAGE_OPTIONS = listOf(
    TranslateLanguageOption("OFF", "Tắt dịch giao diện Xbox", "Off (Disable Xbox UI translation)", "🚫"),
    TranslateLanguageOption("vi", "Tiếng Việt", "Vietnamese (Tiếng Việt)", "🇻🇳"),
    TranslateLanguageOption("en", "English", "English", "🇺🇸"),
    TranslateLanguageOption("es", "Español", "Spanish (Español)", "🇪🇸"),
    TranslateLanguageOption("pt", "Português", "Portuguese (Português)", "🇧🇷"),
    TranslateLanguageOption("fr", "Français", "French (Français)", "🇫🇷"),
    TranslateLanguageOption("de", "Deutsch", "German (Deutsch)", "🇩🇪"),
    TranslateLanguageOption("ja", "日本語", "Japanese (日本語)", "🇯🇵"),
    TranslateLanguageOption("ko", "한국어", "Korean (한국어)", "🇰🇷"),
    TranslateLanguageOption("zh-CN", "中文", "Chinese (中文)", "🇨🇳"),
    TranslateLanguageOption("id", "Bahasa Indonesia", "Indonesian (Bahasa Indonesia)", "🇮🇩"),
    TranslateLanguageOption("th", "ไทย", "Thai (ไทย)", "🇹🇭"),
    TranslateLanguageOption("ru", "Русский", "Russian (Русский)", "🇷🇺")
)

enum class ClarityPreset(
    val displayName: String,
    val description: String,
    val displayNameEn: String,
    val descriptionEn: String
) {
    OFF("Tắt", "Hình ảnh nguyên bản không can thiệp", "Off", "Original unmodified video stream"),
    BALANCED("Cân Bằng", "Tăng nét nhẹ + 8% tương phản (Khuyên dùng)", "Balanced", "Gentle sharpening + 8% contrast (Recommended)"),
    ULTRA_SHARP("Sắc Nét Cao", "Làm nét cạnh tối đa cho game 3D/FPS", "High Sharpness", "High edge clarity for 3D/FPS games"),
    OLED_PUNCH("OLED Punch", "Màu sắc rực rỡ & độ sâu màu cho màn AMOLED/OLED", "OLED Punch", "Vivid colors & deep contrast for AMOLED/OLED");

    fun localizedName(isEn: Boolean): String = if (isEn) displayNameEn else displayName
    fun localizedDesc(isEn: Boolean): String = if (isEn) descriptionEn else description
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
        subtitle = "Tự chỉnh theo mạng Wi-Fi / 4G",
        targetBitrateMbps = 10,
        deviceLoadLabel = "⚡ Tự điều chỉnh theo mạng",
        dataSavingNotice = "Tự đổi giữa 720p và 1080p",
        description = "Tự động hạ độ phân giải khi mạng yếu và tăng lại khi mạng ổn định."
    ),
    DATA_SAVER(
        id = "saver",
        title = "Tiết Kiệm Dung Lượng (Thấp)",
        shortLabel = "Tiết Kiệm (5M)",
        subtitle = "Đỡ tốn data, mát máy, ít hao pin",
        targetBitrateMbps = 5,
        deviceLoadLabel = "❄️ Nhẹ máy • Đỡ hao pin",
        dataSavingNotice = "Giới hạn băng thông ~5 Mbps",
        description = "Giảm băng thông truyền tải xuống mức thấp, phù hợp khi dùng dữ liệu di động hoặc máy cấu hình thấp."
    ),
    MEDIUM(
        id = "medium",
        title = "Trung Bình",
        shortLabel = "Trung Bình (9M)",
        subtitle = "Cân bằng độ nét và nhiệt độ máy",
        targetBitrateMbps = 9,
        deviceLoadLabel = "🌿 Mức trung bình • ~9 Mbps",
        dataSavingNotice = "Giới hạn băng thông ~9 Mbps",
        description = "Mức cân bằng cho các dòng máy tầm trung, giữ nhiệt độ máy ổn định khi chơi lâu."
    ),
    HIGH(
        id = "high",
        title = "Cao",
        shortLabel = "Cao (14M)",
        subtitle = "Độ phân giải 1080p • 14 Mbps",
        targetBitrateMbps = 14,
        deviceLoadLabel = "⚡ Độ phân giải 1080p",
        dataSavingNotice = "Băng thông ~14 Mbps (~4.5GB/giờ)",
        description = "Độ phân giải 1080p cho mạng Wi-Fi ổn định và thiết bị tầm trung trở lên."
    ),
    ULTRA(
        id = "ultra",
        title = "Rất Cao (Tối Đa)",
        shortLabel = "Rất Cao (20M)",
        subtitle = "Mức băng thông cao nhất • 20 Mbps",
        targetBitrateMbps = 20,
        deviceLoadLabel = "🔥 Băng thông 20 Mbps",
        dataSavingNotice = "Băng thông ~20 Mbps (~7GB/giờ)",
        description = "Mức bitrate cao nhất của Xbox Cloud Gaming, phù hợp khi dùng mạng Wi-Fi 5GHz ổn định."
    )
}

enum class CellularOptimizationMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val resolutionBadge: String,
    val bitrateMbps: Int,
    val deviceLoadBadge: String,
    val description: String,
    val titleEn: String,
    val shortLabelEn: String,
    val subtitleEn: String,
    val resolutionBadgeEn: String,
    val deviceLoadBadgeEn: String,
    val descriptionEn: String,
    val request1080p: Boolean
) {
    AUTO(
        id = "auto",
        title = "Tự động (720p ⇄ 1080p)",
        shortLabel = "Tự động (720p⇄1080p)",
        subtitle = "Tự chỉnh độ nét theo sóng 4G/5G & Wi-Fi",
        resolutionBadge = "Auto 720p⇄1080p",
        bitrateMbps = 12,
        deviceLoadBadge = "⚡ Tự động theo mạng",
        description = "Tự động hạ độ nét khi mạng yếu để giữ game mượt, và nâng lại khi mạng ổn định.",
        titleEn = "Auto (720p ⇄ 1080p)",
        shortLabelEn = "Auto (720p⇄1080p)",
        subtitleEn = "Auto-adjusts clarity for 4G/5G & Wi-Fi",
        resolutionBadgeEn = "Auto 720p⇄1080p",
        deviceLoadBadgeEn = "⚡ Auto Network Fit",
        descriptionEn = "Automatically lowers resolution when signal is weak to keep gameplay smooth, and restores clarity when stable.",
        request1080p = true
    ),
    DATA_SAVER_HIGH_SPEED(
        id = "saver",
        title = "720p • Thấp",
        shortLabel = "720p • Thấp (5M)",
        subtitle = "Nhẹ mạng, tiết kiệm 4G/5G & mát máy • 5 Mbps",
        resolutionBadge = "720p • Thấp",
        bitrateMbps = 5,
        deviceLoadBadge = "❄️ 720p Nhẹ & Mát Máy",
        description = "Phù hợp khi sóng 4G yếu hoặc muốn tiết kiệm dữ liệu di động và giữ máy mát.",
        titleEn = "720p • Low",
        shortLabelEn = "720p • Low (5M)",
        subtitleEn = "Saves mobile data & keeps phone cool • 5 Mbps",
        resolutionBadgeEn = "720p • Low",
        deviceLoadBadgeEn = "❄️ 720p Cool & Light",
        descriptionEn = "Best for weak 4G signal, saving mobile data, and keeping your phone cool.",
        request1080p = true
    ),
    MEDIUM(
        id = "medium",
        title = "900p • Vừa",
        shortLabel = "900p • Vừa (9M)",
        subtitle = "Cân bằng độ nét và độ mượt • 9 Mbps",
        resolutionBadge = "900p • Vừa",
        bitrateMbps = 9,
        deviceLoadBadge = "🌿 900p Cân Bằng",
        description = "Hình ảnh rõ vừa phải, chơi ổn định trên cả 4G/5G và Wi-Fi.",
        titleEn = "900p • Med",
        shortLabelEn = "900p • Med (9M)",
        subtitleEn = "Balanced clarity and smoothness • 9 Mbps",
        resolutionBadgeEn = "900p • Med",
        deviceLoadBadgeEn = "🌿 900p Balanced",
        descriptionEn = "Balanced clarity and smooth response on 4G/5G and Wi-Fi.",
        request1080p = true
    ),
    HIGH(
        id = "high",
        title = "1080p • Cao",
        shortLabel = "1080p • Cao (14M)",
        subtitle = "Hình ảnh Full HD sắc nét • 14 Mbps",
        resolutionBadge = "1080p • Cao",
        bitrateMbps = 14,
        deviceLoadBadge = "✨ 1080p Full HD",
        description = "Hình ảnh Full HD sắc nét cho mạng Wi-Fi ổn định hoặc 5G mạnh.",
        titleEn = "1080p • High",
        shortLabelEn = "1080p • High (14M)",
        subtitleEn = "Crisp Full HD image • 14 Mbps",
        resolutionBadgeEn = "1080p • High",
        deviceLoadBadgeEn = "✨ 1080p Full HD",
        descriptionEn = "Sharp Full HD visuals for stable Wi-Fi or strong 5G.",
        request1080p = true
    ),
    MAX_SPEED_UNCAPPED(
        id = "max",
        title = "1080p+ • Rất cao",
        shortLabel = "1080p+ • Rất cao (20M)",
        subtitle = "Độ nét tối đa cho Wi-Fi mạnh • 20 Mbps",
        resolutionBadge = "1080p+ • Rất cao",
        bitrateMbps = 20,
        deviceLoadBadge = "🔥 1080p+ Tối Đa",
        description = "Độ nét cao nhất dành cho mạng Wi-Fi 5GHz hoặc 5G tốc độ cao.",
        titleEn = "1080p+ • Very High",
        shortLabelEn = "1080p+ • Very High (20M)",
        subtitleEn = "Maximum clarity for fast Wi-Fi • 20 Mbps",
        resolutionBadgeEn = "1080p+ • Very High",
        deviceLoadBadgeEn = "🔥 1080p+ Max",
        descriptionEn = "Highest visual clarity for fast 5GHz Wi-Fi or strong 5G.",
        request1080p = true
    );

    val autoRenderScale: StreamRenderScale
        get() = when (this) {
            AUTO -> StreamRenderScale.NATIVE_100
            DATA_SAVER_HIGH_SPEED -> StreamRenderScale.PERFORMANCE_75
            MEDIUM -> StreamRenderScale.BALANCED_85
            HIGH -> StreamRenderScale.NATIVE_100
            MAX_SPEED_UNCAPPED -> StreamRenderScale.NATIVE_100
        }

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else title
    fun localizedShortLabel(isEn: Boolean): String = if (isEn) shortLabelEn else shortLabel
    fun localizedSubtitle(isEn: Boolean): String = if (isEn) subtitleEn else subtitle
    fun localizedResolutionBadge(isEn: Boolean): String = if (isEn) resolutionBadgeEn else resolutionBadge
    fun localizedBadge(isEn: Boolean): String = if (isEn) deviceLoadBadgeEn else deviceLoadBadge
    fun localizedDesc(isEn: Boolean): String = if (isEn) descriptionEn else description
    fun unifiedChipLabel(isEn: Boolean): String = when (this) {
        AUTO -> if (isEn) "Auto (720p⇄1080p)" else "Tự động (720p⇄1080p)"
        DATA_SAVER_HIGH_SPEED -> if (isEn) "720p • Low" else "720p • Thấp"
        MEDIUM -> if (isEn) "900p • Med" else "900p • Vừa"
        HIGH -> if (isEn) "1080p • High" else "1080p • Cao"
        MAX_SPEED_UNCAPPED -> if (isEn) "1080p+ • Very High" else "1080p+ • Rất cao"
    }
}

enum class NetworkTuningMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val titleEn: String,
    val subtitleEn: String
) {
    AUTO(
        id = "auto",
        title = "Tự Động Theo Điều Kiện Mạng (Khuyên dùng)",
        subtitle = "Tự động điều chỉnh đồ họa & hiệu năng theo Wi-Fi / 4G",
        titleEn = "Auto Network Adaptation (Recommended)",
        subtitleEn = "Automatically tunes graphics & latency for Wi-Fi / 4G"
    ),
    WIFI_SPEED_FIRST(
        id = "wifi_speed",
        title = "Ưu Tiên Độ Trễ Thấp & Mượt 60 FPS",
        subtitle = "Giảm trễ đệm khi mạng yếu và giảm tải GPU",
        titleEn = "Prioritize Low Latency & Smooth 60 FPS",
        subtitleEn = "Reduces buffer bloat on weak links and lowers GPU load"
    ),
    CELLULAR_DATA_SAVER(
        id = "cell_saver",
        title = "Thấp (Tiết Kiệm Dung Lượng)",
        subtitle = "Giới hạn ~5 Mbps để đỡ tốn dữ liệu và mát máy",
        titleEn = "Low (Mobile Data Saver)",
        subtitleEn = "Limits bitrate to ~5 Mbps to save data and keep phone cool"
    ),
    CELLULAR_MAX_SPEED(
        id = "cell_max",
        title = "Rất Cao (20 Mbps)",
        subtitle = "Cho phép mức băng thông tối đa 20 Mbps khi mạng mạnh",
        titleEn = "Very High (20 Mbps)",
        subtitleEn = "Allows up to 20 Mbps bitrate on fast connections"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else title
    fun localizedSubtitle(isEn: Boolean): String = if (isEn) subtitleEn else subtitle
}

enum class AppLanguage(
    val code: String,
    val title: String,
    val titleEn: String,
    val flag: String,
    val shortBadge: String,
    val translateTargetCode: String
) {
    SYSTEM("system", "Theo hệ thống (Tự động)", "System Default (Auto)", "🌐", "AUTO", ""),
    ENGLISH("en", "English", "English", "🇺🇸", "EN", "en"),
    VIETNAMESE("vi", "Tiếng Việt", "Tiếng Việt (Vietnamese)", "🇻🇳", "VI", "vi"),
    SPANISH("es", "Español", "Español (Spanish)", "🇪🇸", "ES", "es"),
    PORTUGUESE("pt", "Português", "Português (Portuguese)", "🇧🇷", "PT", "pt"),
    FRENCH("fr", "Français", "Français (French)", "🇫🇷", "FR", "fr"),
    GERMAN("de", "Deutsch", "Deutsch (German)", "🇩🇪", "DE", "de"),
    JAPANESE("ja", "日本語", "日本語 (Japanese)", "🇯🇵", "JA", "ja"),
    KOREAN("ko", "한국어", "한국어 (Korean)", "🇰🇷", "KO", "ko"),
    CHINESE("zh", "中文", "中文 (Chinese)", "🇨🇳", "ZH", "zh-CN"),
    INDONESIAN("id", "Bahasa Indonesia", "Bahasa Indonesia", "🇮🇩", "ID", "id"),
    THAI("th", "ไทย", "ไทย (Thai)", "🇹🇭", "TH", "th"),
    RUSSIAN("ru", "Русский", "Русский (Russian)", "🇷🇺", "RU", "ru");

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else title

    fun effectiveLanguageCode(): String {
        if (this != SYSTEM) return code
        val rawSys = resolveOsSystemLanguage()
        val normalized = if (rawSys == "in") "id" else rawSys
        return entries.firstOrNull { it != SYSTEM && normalized.startsWith(it.code) }?.code ?: "en"
    }

    companion object {
        fun resolveOsSystemLanguage(): String {
            return try {
                val sysConf = android.content.res.Resources.getSystem().configuration
                val sysLoc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && !sysConf.locales.isEmpty) {
                    sysConf.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    sysConf.locale
                }
                val lang = sysLoc?.language?.lowercase(Locale.ROOT).orEmpty()
                if (lang.isNotBlank()) lang else Locale.getDefault().language.lowercase(Locale.ROOT)
            } catch (_: Exception) {
                Locale.getDefault().language.lowercase(Locale.ROOT)
            }
        }
    }

    fun effectiveShortBadge(): String {
        if (this != SYSTEM) return shortBadge
        val code = effectiveLanguageCode()
        return entries.firstOrNull { it.code == code }?.shortBadge ?: "EN"
    }

    fun effectiveFlag(): String {
        if (this != SYSTEM) return flag
        val code = effectiveLanguageCode()
        return entries.firstOrNull { it.code == code }?.flag ?: "🌐"
    }

    fun resolveTranslateTargetCode(): String {
        val eff = effectiveLanguageCode()
        return entries.firstOrNull { it.code == eff }?.translateTargetCode?.ifBlank { "en" } ?: "en"
    }
}

enum class GamepadLayoutPreset(
    val titleVi: String,
    val titleEn: String,
    val descVi: String,
    val descEn: String
) {
    STANDARD(
        "Chuẩn Xbox (Standard)",
        "Xbox Standard",
        "Bố cục nút bấm mặc định cho các thể loại game",
        "Default button layout for general games"
    ),
    FPS_PRO(
        "Bắn Súng (FPS)",
        "FPS / Shooter",
        "Cần ngắm phải R lớn hơn (+18%), ưu tiên cò LT/RT & L3/R3",
        "Larger Right Stick (+18%), prioritized LT/RT triggers & L3/R3"
    ),
    RACING(
        "Đua Xe (Racing)",
        "Racing / Driving",
        "Ưu tiên cò ga/phanh LT/RT và cần lái bên trái L",
        "Prioritized LT/RT throttle/brake triggers & Left Stick"
    ),
    COMPACT(
        "Gọn Nhẹ (Compact)",
        "Compact / RPG",
        "Thu nhỏ cụm nút để đỡ che màn hình",
        "Smaller button clusters to leave more screen visible"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

enum class GamepadHapticLevel(
    val titleVi: String,
    val titleEn: String,
    val durationMs: Long,
    val amplitude: Int
) {
    OFF("Tắt", "Off", 0L, 0),
    LIGHT("Nhẹ", "Light", 10L, 65),
    MEDIUM("Vừa", "Medium", 18L, 140),
    STRONG("Mạnh", "Strong", 28L, 255);

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
}

enum class StreamRenderScale(
    val id: String,
    val scaleFactor: Float,
    val targetHeightPx: Int,
    val titleVi: String,
    val titleEn: String,
    val shortVi: String,
    val shortEn: String,
    val descVi: String,
    val descEn: String
) {
    NATIVE_100(
        id = "100",
        scaleFactor = 1.0f,
        targetHeightPx = 1080,
        titleVi = "100% (1080p Chuẩn)",
        titleEn = "100% (1080p Native)",
        shortVi = "100%",
        shortEn = "100%",
        descVi = "Sắc nét tối đa",
        descEn = "Maximum sharpness"
    ),
    BALANCED_85(
        id = "85",
        scaleFactor = 0.85f,
        targetHeightPx = 900,
        titleVi = "85% (900p Cân bằng)",
        titleEn = "85% (900p Balanced)",
        shortVi = "85%",
        shortEn = "85%",
        descVi = "Giảm 25% tải GPU, vẫn rõ nét",
        descEn = "-25% GPU load, stays sharp"
    ),
    PERFORMANCE_75(
        id = "75",
        scaleFactor = 0.75f,
        targetHeightPx = 720,
        titleVi = "75% (720p Mượt)",
        titleEn = "75% (720p Smooth)",
        shortVi = "75%",
        shortEn = "75%",
        descVi = "Giảm 45% tải GPU, chống tụt FPS",
        descEn = "-45% GPU load, prevents frame drops"
    ),
    ULTRA_LIGHT_60(
        id = "60",
        scaleFactor = 0.60f,
        targetHeightPx = 540,
        titleVi = "60% (540p Nhẹ)",
        titleEn = "60% (540p Light)",
        shortVi = "60%",
        shortEn = "60%",
        descVi = "Dành cho máy yếu, mát máy",
        descEn = "For low-end devices, runs cool"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

enum class StreamFpsCap(
    val fps: Int,
    val titleVi: String,
    val titleEn: String,
    val shortVi: String,
    val shortEn: String,
    val descVi: String,
    val descEn: String
) {
    FPS_30(
        fps = 30,
        titleVi = "30 FPS",
        titleEn = "30 FPS",
        shortVi = "30 FPS",
        shortEn = "30 FPS",
        descVi = "Mát máy, tiết kiệm pin",
        descEn = "Cool & battery saving"
    ),
    FPS_60(
        fps = 60,
        titleVi = "60 FPS",
        titleEn = "60 FPS",
        shortVi = "60 FPS",
        shortEn = "60 FPS",
        descVi = "Mượt tiêu chuẩn",
        descEn = "Standard smooth"
    ),
    FPS_120(
        fps = 120,
        titleVi = "120 FPS",
        titleEn = "120 FPS",
        shortVi = "120 FPS",
        shortEn = "120 FPS",
        descVi = "Màn hình 90Hz/120Hz",
        descEn = "90Hz/120Hz displays"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

enum class ScreenRecordQuality(
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
    val fps: Int,
    val bitrateBps: Int,
    val titleVi: String,
    val titleEn: String,
    val shortVi: String,
    val shortEn: String,
    val descVi: String,
    val descEn: String
) {
    LIGHT_540P_30(
        id = "540p30",
        widthPx = 960,
        heightPx = 540,
        fps = 30,
        bitrateBps = 2_800_000,
        titleVi = "540p • 30 FPS",
        titleEn = "540p • 30 FPS",
        shortVi = "540p • 30 FPS",
        shortEn = "540p • 30 FPS",
        descVi = "Nhẹ máy, tốn ít bộ nhớ",
        descEn = "Light & compact"
    ),
    BALANCED_720P_30(
        id = "720p30",
        widthPx = 1280,
        heightPx = 720,
        fps = 30,
        bitrateBps = 3_800_000,
        titleVi = "720p • 30 FPS",
        titleEn = "720p • 30 FPS",
        shortVi = "720p • 30 FPS",
        shortEn = "720p • 30 FPS",
        descVi = "Mượt, mát máy & nhẹ dung lượng",
        descEn = "Smooth, cool & compact"
    ),
    SMOOTH_720P_60(
        id = "720p60",
        widthPx = 1280,
        heightPx = 720,
        fps = 60,
        bitrateBps = 7_000_000,
        titleVi = "720p • 60 FPS",
        titleEn = "720p • 60 FPS",
        shortVi = "720p • 60 FPS",
        shortEn = "720p • 60 FPS",
        descVi = "60 FPS cho máy cấu hình khá",
        descEn = "60 FPS for fast devices"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

enum class RecordingCaptureEngine(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val shortVi: String,
    val shortEn: String,
    val descVi: String,
    val descEn: String
) {
    HARDWARE_SCREEN(
        id = "HARDWARE_SCREEN",
        titleVi = "Quay theo màn hình đang chơi (Mặc định)",
        titleEn = "Record Screen Ratio as Played (Default)",
        shortVi = "Theo màn hình",
        shortEn = "Screen Ratio",
        descVi = "Ghi hình .MP4 đúng tỉ lệ màn hình máy kèm tiếng game",
        descEn = "Records .MP4 matching your display ratio with game audio"
    ),
    DIRECT_STREAM(
        id = "DIRECT_STREAM",
        titleVi = "Quay luồng 16:9 gốc",
        titleEn = "Direct 16:9 Stream",
        shortVi = "Luồng 16:9",
        shortEn = "16:9 Stream",
        descVi = "Chỉ ghi khung hình 16:9 của game kèm tiếng",
        descEn = "Records pure 16:9 game stream with audio"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

enum class XboxWebUiMode(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val shortVi: String,
    val shortEn: String,
    val descVi: String,
    val descEn: String
) {
    CLASSIC(
        id = "CLASSIC",
        titleVi = "Giao diện cũ (Ổn định • Khuyên dùng ở VN)",
        titleEn = "Classic UI (Stable • Best for Region Unlock)",
        shortVi = "Giao diện cũ",
        shortEn = "Classic UI",
        descVi = "Dùng trang xbox.com/play cổ điển, bấm nút nhạy và tương thích tốt nhất khi mở khóa vùng ở Việt Nam",
        descEn = "Uses classic xbox.com/play layout, responsive touch & best compatibility with Smart Region Unlock"
    ),
    NEW_UI(
        id = "NEW_UI",
        titleVi = "Giao diện mới (Dành cho Mỹ/Nhật hoặc VPN ngoài)",
        titleEn = "New UI (For US/JP or External VPN)",
        shortVi = "Giao diện mới",
        shortEn = "New UI",
        descVi = "Dùng trang play.xbox.com mới khi bạn đang ở nước được hỗ trợ (Mỹ, Nhật...) hoặc đã bật VPN ngoài sang đó",
        descEn = "Uses new play.xbox.com dashboard when you are in a supported country or using a full external VPN"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

@androidx.compose.runtime.Immutable
data class PerGameGamepadLayout(
    val offsets: String = "",
    val scales: String = "",
    val hiddenButtons: String = "",
    val abxySplit: Boolean = false,
    val dpadSplit: Boolean = false
)

@androidx.compose.runtime.Immutable
data class AppConfig(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val smartVpnEnabled: Boolean = true,
    val autoBypassEnabled: Boolean = true,
    val autoBypassDelaySeconds: Int = 2,
    val autoReconnectOnMenu: Boolean = true,
    val forceDesktopUserAgent: Boolean = true,
    val clarityBoostEnabled: Boolean = false,
    val clarityPreset: ClarityPreset = ClarityPreset.BALANCED,
    val maxBitrateMbps: Int = 15,
    val antiAfkKeepAlive: Boolean = true,
    val inputPollingBoost: Boolean = true,
    val showFloatingHud: Boolean = true,
    val showVirtualController: Boolean = false,
    val autoHideVirtualOnPhysicalController: Boolean = true,
    val gamepadScale: Float = 1.0f, // 0.75f .. 1.35f
    val gamepadOpacity: Float = 0.70f, // 0.25f .. 0.95f
    val gamepadDeadzone: Float = 0.08f, // 0.02f .. 0.25f
    val leftStickInnerDeadzone: Float = 0.08f, // 0.00f .. 0.30f
    val leftStickOuterDeadzone: Float = 0.95f, // 0.70f .. 1.00f
    val rightStickInnerDeadzone: Float = 0.08f, // 0.00f .. 0.30f
    val rightStickOuterDeadzone: Float = 0.95f, // 0.70f .. 1.00f
    val physLeftStickInnerDeadzone: Float = 0.0f, // 0.00f .. 0.30f (default 0.0 = untouched)
    val physLeftStickOuterDeadzone: Float = 1.0f, // 0.70f .. 1.00f (default 1.0 = untouched)
    val physRightStickInnerDeadzone: Float = 0.0f, // 0.00f .. 0.30f (default 0.0 = untouched)
    val physRightStickOuterDeadzone: Float = 1.0f, // 0.70f .. 1.00f (default 1.0 = untouched)
    val gamepadSensitivity: Float = 1.0f, // 0.60f .. 1.60f
    val gamepadLayoutPreset: GamepadLayoutPreset = GamepadLayoutPreset.STANDARD,
    val gamepadHapticLevel: GamepadHapticLevel = GamepadHapticLevel.MEDIUM,
    val gameRumbleLevel: GamepadHapticLevel = GamepadHapticLevel.MEDIUM,
    val gamepadSprintLockEnabled: Boolean = true,
    val gamepadCustomOffsets: String = "",
    val gamepadButtonScales: String = "",
    val gamepadHiddenButtons: String = "",
    val gamepadAbxySplitMode: Boolean = false,
    val gamepadDpadSplitMode: Boolean = false,
    val usePerGameGamepadLayout: Boolean = true,
    val perGameLayoutsJson: String = "{}",
    val physicalGamepadButtonMap: String = "",
    val controllerShortcutsEnabled: Boolean = true,
    val shortcutToggleStatsButton: Int = 3, // Y
    val shortcutToggleVirtualPadButton: Int = 1, // B
    val shortcutToggleRecordingButton: Int = 5, // RB
    val useSystemLanguage: Boolean = true,
    val customLocale: String = "en-US",
    val preferredServerId: String = "jp_tokyo_linode_1",
    val systemRevolutionEnabled: Boolean = true,
    val stealthHeadersEnabled: Boolean = true,
    val autoHealingProxyMesh: Boolean = true,
    val sdpBitrateBoostEnabled: Boolean = true,
    val zeroDelayDirectHandshake: Boolean = true,
    val antiJapaneseEnforcer: Boolean = true,
    val xcloudTranslateEnabled: Boolean = true,
    val translateLanguage: String = "vi",
    val preserveGameTitles: Boolean = true,
    val bilingualMode: Boolean = false,
    val githubDictionaryEnabled: Boolean = true,
    val hideStreamStatsOverlay: Boolean = true,
    val cellularMode: CellularOptimizationMode = CellularOptimizationMode.AUTO,
    val networkTuningMode: NetworkTuningMode = NetworkTuningMode.AUTO,
    val wifiSpeedFirstPriority: Boolean = true,
    val lowEndDeviceMode: Boolean = false,
    val streamRenderScale: StreamRenderScale = StreamRenderScale.NATIVE_100,
    val streamFpsCap: StreamFpsCap = StreamFpsCap.FPS_60,
    val gpuLoadReductionEnabled: Boolean = true,
    val screenRecordQuality: ScreenRecordQuality = ScreenRecordQuality.BALANCED_720P_30,
    val autoReduceLoadWhenRecording: Boolean = true,
    val recordingCaptureEngine: RecordingCaptureEngine = RecordingCaptureEngine.HARDWARE_SCREEN,
    val videoContrast: Int = 100, // 80 .. 140 (%)
    val videoSaturation: Int = 100, // 80 .. 150 (%)
    val videoBrightness: Int = 100, // 80 .. 130 (%)
    val customApkDownloadUrl: String = "https://github.com/muitenvn/CloudPlay-App/releases/latest",
    val publicWebPortalUrl: String = "https://muitenvn.github.io/CloudPlay-App/",
    val githubRepoSlug: String = "muitenvn/CloudPlay-App",
    val userAcceptedTerms: Boolean = false,
    val autoCheckUpdatesOnLaunch: Boolean = true,
    val enableGlobalpingProbe: Boolean = false, // A4: Mặc định TẮT đo qua trạm Globalping bên thứ ba
    val hideUnreachableDynamicServers: Boolean = false, // A8: Tùy chọn ẩn máy chủ động không đo được
    val enableGeoIpHttpsFallback: Boolean = true,
    val allowDbIpCellularDownload: Boolean = false,
    val allowCellularLiveProxySync: Boolean = false,
    val xboxWebUiMode: XboxWebUiMode = XboxWebUiMode.CLASSIC
) {
    val targetRegionUrl: String
        get() = if (xboxWebUiMode == XboxWebUiMode.NEW_UI) {
            "https://play.xbox.com/"
        } else {
            "https://www.xbox.com/${getEffectiveLocale()}/play"
        }

    fun getEffectiveLocale(): String {
        return if (useSystemLanguage) {
            SettingsRepository.resolveSystemXboxLocale()
        } else {
            customLocale.ifBlank { "en-US" }
        }
    }

    fun isEnglishUI(): Boolean {
        return appLanguage.effectiveLanguageCode() != "vi"
    }

    /**
     * Dùng cho màn hình Cài đặt (`SettingsScreen`):
     * Trả về `true` cho mọi ngôn ngữ khác Tiếng Việt (`en`, `de`, `ko`, `ja`, `zh`, `es`, `pt`, `fr`, `id`, `th`, `ru`)
     * để kết hợp cùng `AppUiTranslator` dịch sang ngôn ngữ đã chọn và không bao giờ bị rơi về Tiếng Việt.
     */
    fun isSettingsEnglishUI(): Boolean {
        return appLanguage.effectiveLanguageCode() != "vi"
    }

    /**
     * Dịch nhanh chuỗi giao diện ứng dụng (ngoài màn hình Cài đặt) sang bất kỳ ngôn ngữ nào trong 12 ngôn ngữ.
     */
    fun tr(vi: String, en: String): String {
        return AppUiTranslator.translateUi(vi = vi, en = en, rawLangCode = appLanguage.effectiveLanguageCode())
    }

    fun isVietnameseUI(): Boolean {
        return appLanguage.effectiveLanguageCode() == "vi"
    }

    /**
     * Trả về mã ngôn ngữ đích cho bộ dịch giao diện Xbox.
     * Đảm bảo KHÔNG BAO GIỜ trả về chuỗi rỗng ("") để tránh lỗi API dịch;
     * nơi gọi phải kiểm tra [isEffectiveTranslateEnabled] trước khi gọi bộ dịch.
     */
    fun getTargetTranslateLanguageCode(): String {
        val trimmed = translateLanguage.trim()
        if (trimmed.isNotEmpty() && !trimmed.equals("OFF", ignoreCase = true)) {
            return trimmed
        }
        return appLanguage.resolveTranslateTargetCode().ifBlank { "vi" }
    }

    fun isEffectiveTranslateEnabled(): Boolean {
        if (lowEndDeviceMode) return false
        val trimmed = translateLanguage.trim()
        if (trimmed.equals("OFF", ignoreCase = true)) return false
        return xcloudTranslateEnabled && getTargetTranslateLanguageCode().isNotBlank()
    }
}

object SettingsRepository {
    const val TRANSLATE_LANG_OFF = "OFF"
    val INITIAL_VERIFIED_XBOX_MARKETS: Set<String> = setOf("en-US")

    private var prefs: SharedPreferences? = null
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences("cloudplay_settings_store", Context.MODE_PRIVATE)
        prefs = sp

        val hasMigratedUnifiedSwitches = sp.getBoolean("migrated_unified_switches_v1", false)
        val rawSmartVpn = sp.getBoolean("smart_vpn_enabled", true)
        val rawAutoBypass = sp.getBoolean("auto_bypass_enabled", true)
        val rawLowEnd = sp.getBoolean("low_end_device_mode", false)
        val rawGpuReduction = sp.getBoolean("gpu_load_reduction_enabled", true)

        val effectiveSmartVpn = if (!hasMigratedUnifiedSwitches) (rawSmartVpn || rawAutoBypass) else rawSmartVpn
        val effectiveAutoBypass = rawAutoBypass
        // Phản biện mục 1: gpu_load_reduction_enabled mặc định là true cho mọi máy, nếu dùng phép HOẶC sẽ ép mọi thiết bị bật lowEndDeviceMode (khóa 5 Mbps, 720p, tắt dịch). Do đó lấy theo rawLowEnd.
        val effectiveLowEnd = rawLowEnd
        val effectiveGpuReduction = if (!hasMigratedUnifiedSwitches) rawLowEnd else rawGpuReduction

        if (!hasMigratedUnifiedSwitches) {
            sp.edit()
                .putBoolean("smart_vpn_enabled", effectiveSmartVpn)
                .putBoolean("auto_bypass_enabled", effectiveAutoBypass)
                .putBoolean("low_end_device_mode", effectiveLowEnd)
                .putBoolean("gpu_load_reduction_enabled", effectiveGpuReduction)
                .putBoolean("migrated_unified_switches_v1", true)
                .apply()
        }

        // Mục 7d: Migration 1 lần đưa thị trường đã lưu khác en-US từ bản cũ về en-US
        val hasMigratedXboxMarket = sp.getBoolean("migrated_xbox_market_en_us_v1", false)
        if (!hasMigratedXboxMarket) {
            val savedLocale = sp.getString("custom_locale", "en-US") ?: "en-US"
            val savedUseSystem = sp.getBoolean("use_system_language", true)
            if (!savedUseSystem || !savedLocale.equals("en-US", ignoreCase = true)) {
                sp.edit()
                    .putBoolean("use_system_language", true)
                    .putString("custom_locale", "en-US")
                    .putBoolean("migrated_xbox_market_en_us_v1", true)
                    .apply()
            } else {
                sp.edit().putBoolean("migrated_xbox_market_en_us_v1", true).apply()
            }
        }

        val resolvedAppLanguage = try {
            AppLanguage.valueOf(sp.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name)
        } catch (_: Exception) { AppLanguage.SYSTEM }

        // Mục 8: Migration ngôn ngữ dịch giao diện Xbox:
        // Người dùng cũ giữ "vi" (nếu xcloudTranslateEnabled đang tắt thì giữ tắt);
        // Người dùng mới mặc định theo ngôn ngữ app, nếu app tiếng Anh thì tắt.
        val hasMigratedTranslateLang = sp.getBoolean("migrated_translate_lang_v1", false)
        if (!hasMigratedTranslateLang) {
            val isExistingUser = sp.contains("xcloud_translate") || sp.contains("app_language") || sp.contains("smart_vpn_enabled") && hasMigratedUnifiedSwitches
            if (isExistingUser && (sp.contains("xcloud_translate") || sp.contains("app_language"))) {
                val oldEnabled = sp.getBoolean("xcloud_translate", true)
                val targetLang = sp.getString("translate_language", null)?.ifBlank { "vi" } ?: "vi"
                sp.edit()
                    .putBoolean("xcloud_translate", oldEnabled)
                    .putString("translate_language", targetLang)
                    .putBoolean("migrated_translate_lang_v1", true)
                    .apply()
            } else {
                val effAppCode = resolvedAppLanguage.effectiveLanguageCode()
                if (effAppCode == "en") {
                    sp.edit()
                        .putBoolean("xcloud_translate", false)
                        .putString("translate_language", "OFF")
                        .putBoolean("migrated_translate_lang_v1", true)
                        .apply()
                } else {
                    val defaultTarget = resolvedAppLanguage.resolveTranslateTargetCode().ifBlank { "vi" }
                    sp.edit()
                        .putBoolean("xcloud_translate", true)
                        .putString("translate_language", defaultTarget)
                        .putBoolean("migrated_translate_lang_v1", true)
                        .apply()
                }
            }
        }

        val loaded = AppConfig(
            appLanguage = resolvedAppLanguage,
            useSystemLanguage = sp.getBoolean("use_system_language", true),
            customLocale = sp.getString("custom_locale", "en-US") ?: "en-US",
            smartVpnEnabled = effectiveSmartVpn,
            autoBypassEnabled = effectiveAutoBypass,
            autoBypassDelaySeconds = sp.getInt("auto_bypass_delay", 2),
            forceDesktopUserAgent = sp.getBoolean("force_desktop_ua", true),
            clarityBoostEnabled = sp.getBoolean("clarity_boost_enabled_v2", false),
            clarityPreset = try {
                ClarityPreset.valueOf(sp.getString("clarity_preset", ClarityPreset.BALANCED.name) ?: ClarityPreset.BALANCED.name)
            } catch (_: Exception) { ClarityPreset.BALANCED },
            maxBitrateMbps = sp.getInt("max_bitrate", 15),
            antiAfkKeepAlive = sp.getBoolean("anti_afk", true),
            inputPollingBoost = sp.getBoolean("input_polling_boost", true),
            showVirtualController = sp.getBoolean("show_virtual_controller", false),
            autoHideVirtualOnPhysicalController = sp.getBoolean("auto_hide_virtual_on_phys", true),
            gamepadScale = sp.getFloat("gamepad_scale", 1.0f).coerceIn(0.75f, 1.35f),
            gamepadOpacity = sp.getFloat("gamepad_opacity", 0.70f).coerceIn(0.25f, 0.95f),
            gamepadDeadzone = sp.getFloat("gamepad_deadzone", 0.08f).coerceIn(0.02f, 0.25f),
            leftStickInnerDeadzone = sp.getFloat("left_stick_inner_deadzone", sp.getFloat("gamepad_deadzone", 0.08f)).coerceIn(0.00f, 0.30f),
            leftStickOuterDeadzone = sp.getFloat("left_stick_outer_deadzone", 0.95f).coerceIn(0.70f, 1.00f),
            rightStickInnerDeadzone = sp.getFloat("right_stick_inner_deadzone", sp.getFloat("gamepad_deadzone", 0.08f)).coerceIn(0.00f, 0.30f),
            rightStickOuterDeadzone = sp.getFloat("right_stick_outer_deadzone", 0.95f).coerceIn(0.70f, 1.00f),
            physLeftStickInnerDeadzone = sp.getFloat("phys_left_stick_inner_deadzone", 0.0f).coerceIn(0.00f, 0.30f),
            physLeftStickOuterDeadzone = sp.getFloat("phys_left_stick_outer_deadzone", 1.0f).coerceIn(0.70f, 1.00f),
            physRightStickInnerDeadzone = sp.getFloat("phys_right_stick_inner_deadzone", 0.0f).coerceIn(0.00f, 0.30f),
            physRightStickOuterDeadzone = sp.getFloat("phys_right_stick_outer_deadzone", 1.0f).coerceIn(0.70f, 1.00f),
            gamepadSensitivity = sp.getFloat("gamepad_sensitivity", 1.0f).coerceIn(0.60f, 1.60f),
            gamepadLayoutPreset = try {
                GamepadLayoutPreset.valueOf(sp.getString("gamepad_layout_preset", GamepadLayoutPreset.STANDARD.name) ?: GamepadLayoutPreset.STANDARD.name)
            } catch (_: Exception) { GamepadLayoutPreset.STANDARD },
            gamepadHapticLevel = try {
                GamepadHapticLevel.valueOf(sp.getString("gamepad_haptic_level", GamepadHapticLevel.MEDIUM.name) ?: GamepadHapticLevel.MEDIUM.name)
            } catch (_: Exception) { GamepadHapticLevel.MEDIUM },
            gameRumbleLevel = try {
                GamepadHapticLevel.valueOf(sp.getString("game_rumble_level", GamepadHapticLevel.MEDIUM.name) ?: GamepadHapticLevel.MEDIUM.name)
            } catch (_: Exception) { GamepadHapticLevel.MEDIUM },
            gamepadSprintLockEnabled = sp.getBoolean("gamepad_sprint_lock", true),
            gamepadCustomOffsets = sp.getString("gamepad_custom_offsets", "") ?: "",
            gamepadButtonScales = sp.getString("gamepad_button_scales", "") ?: "",
            gamepadHiddenButtons = sp.getString("gamepad_hidden_buttons", "") ?: "",
            gamepadAbxySplitMode = sp.getBoolean("gamepad_abxy_split_mode", false),
            gamepadDpadSplitMode = sp.getBoolean("gamepad_dpad_split_mode", false),
            usePerGameGamepadLayout = sp.getBoolean("use_per_game_gamepad_layout", true),
            perGameLayoutsJson = sp.getString("per_game_layouts_json", "{}") ?: "{}",
            physicalGamepadButtonMap = sp.getString("physical_gamepad_button_map", "") ?: "",
            controllerShortcutsEnabled = sp.getBoolean("controller_shortcuts_enabled", true),
            shortcutToggleStatsButton = sp.getInt("shortcut_toggle_stats_btn", 3).coerceIn(-1, 16),
            shortcutToggleVirtualPadButton = sp.getInt("shortcut_toggle_vpad_btn", 1).coerceIn(-1, 16),
            shortcutToggleRecordingButton = sp.getInt("shortcut_toggle_rec_btn", 5).coerceIn(-1, 16),
            systemRevolutionEnabled = sp.getBoolean("system_revolution", true),
            stealthHeadersEnabled = sp.getBoolean("stealth_headers", true),
            autoHealingProxyMesh = sp.getBoolean("auto_healing_mesh", true),
            sdpBitrateBoostEnabled = sp.getBoolean("sdp_bitrate_boost", true),
            zeroDelayDirectHandshake = sp.getBoolean("zero_delay_handshake", true),
            antiJapaneseEnforcer = sp.getBoolean("anti_jp_enforcer", true),
            xcloudTranslateEnabled = sp.getBoolean("xcloud_translate", true),
            translateLanguage = sp.getString("translate_language", "vi") ?: "vi",
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
            lowEndDeviceMode = effectiveLowEnd,
            streamRenderScale = try {
                StreamRenderScale.valueOf(
                    sp.getString("stream_render_scale", StreamRenderScale.NATIVE_100.name)
                        ?: StreamRenderScale.NATIVE_100.name
                )
            } catch (_: Exception) { StreamRenderScale.NATIVE_100 },
            streamFpsCap = try {
                StreamFpsCap.valueOf(
                    sp.getString("stream_fps_cap", StreamFpsCap.FPS_60.name)
                        ?: StreamFpsCap.FPS_60.name
                )
            } catch (_: Exception) { StreamFpsCap.FPS_60 },
            gpuLoadReductionEnabled = effectiveGpuReduction,
            screenRecordQuality = try {
                ScreenRecordQuality.valueOf(
                    sp.getString("screen_record_quality", ScreenRecordQuality.BALANCED_720P_30.name)
                        ?: ScreenRecordQuality.BALANCED_720P_30.name
                )
            } catch (_: Exception) { ScreenRecordQuality.BALANCED_720P_30 },
            autoReduceLoadWhenRecording = sp.getBoolean("auto_reduce_load_recording", true),
            recordingCaptureEngine = try {
                RecordingCaptureEngine.valueOf(
                    sp.getString("recording_capture_engine_v3", RecordingCaptureEngine.HARDWARE_SCREEN.name)
                        ?: RecordingCaptureEngine.HARDWARE_SCREEN.name
                )
            } catch (_: Exception) { RecordingCaptureEngine.HARDWARE_SCREEN },
            videoContrast = sp.getInt("video_contrast_v2", 100).coerceIn(80, 140),
            videoSaturation = sp.getInt("video_saturation_v2", 100).coerceIn(80, 150),
            videoBrightness = sp.getInt("video_brightness", 100).coerceIn(80, 130),
            customApkDownloadUrl = sp.getString(
                "custom_apk_download_url_v2",
                "https://github.com/muitenvn/CloudPlay-App/releases/latest"
            ) ?: "https://github.com/muitenvn/CloudPlay-App/releases/latest",
            publicWebPortalUrl = sp.getString(
                "public_web_portal_url_v2",
                "https://muitenvn.github.io/CloudPlay-App/"
            ) ?: "https://muitenvn.github.io/CloudPlay-App/",
            githubRepoSlug = sp.getString(
                "github_repo_slug_v2",
                "muitenvn/CloudPlay-App"
            ) ?: "muitenvn/CloudPlay-App",
            userAcceptedTerms = sp.getBoolean("user_accepted_terms_v2", false),
            autoCheckUpdatesOnLaunch = sp.getBoolean("auto_check_updates_on_launch", true),
            enableGlobalpingProbe = sp.getBoolean("enable_globalping_probe", false),
            hideUnreachableDynamicServers = sp.getBoolean("hide_unreachable_dynamic_servers", false),
            enableGeoIpHttpsFallback = sp.getBoolean("enable_geoip_https_fallback", true),
            allowDbIpCellularDownload = sp.getBoolean("allow_dbip_cellular_download", false),
            allowCellularLiveProxySync = sp.getBoolean("allow_cellular_live_proxy_sync", false),
            xboxWebUiMode = try {
                XboxWebUiMode.valueOf(
                    sp.getString("xbox_web_ui_mode", XboxWebUiMode.CLASSIC.name)
                        ?: XboxWebUiMode.CLASSIC.name
                )
            } catch (_: Exception) { XboxWebUiMode.CLASSIC }
        )
        // Đồng bộ tự động độ phân giải theo mức chất lượng đồ họa đã chọn; luôn giữ Desktop UA để Xbox không ép chuyển sang play.xbox.com
        val synced = loaded.copy(
            streamRenderScale = if (loaded.lowEndDeviceMode) StreamRenderScale.PERFORMANCE_75 else loaded.cellularMode.autoRenderScale,
            forceDesktopUserAgent = true
        )
        _config.value = synced
        AppLogger.info(
            "WEB-UI",
            "Khởi tạo cấu hình giao diện Xbox: chế độ=${synced.xboxWebUiMode.name}, ua=${if (synced.forceDesktopUserAgent) "desktop" else "mobile"}, thị trường=${synced.getEffectiveLocale()}, urlMặcĐịnh=${AppLogger.safeUrlHostPath(synced.targetRegionUrl)}"
        )
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
            putBoolean("clarity_boost_enabled_v2", c.clarityBoostEnabled)
            putBoolean("clarity_boost_enabled", c.clarityBoostEnabled)
            putString("clarity_preset", c.clarityPreset.name)
            putInt("max_bitrate", c.maxBitrateMbps)
            putBoolean("anti_afk", c.antiAfkKeepAlive)
            putBoolean("input_polling_boost", c.inputPollingBoost)
            putBoolean("show_virtual_controller", c.showVirtualController)
            putBoolean("auto_hide_virtual_on_phys", c.autoHideVirtualOnPhysicalController)
            putFloat("gamepad_scale", c.gamepadScale)
            putFloat("gamepad_opacity", c.gamepadOpacity)
            putFloat("gamepad_deadzone", c.gamepadDeadzone)
            putFloat("left_stick_inner_deadzone", c.leftStickInnerDeadzone)
            putFloat("left_stick_outer_deadzone", c.leftStickOuterDeadzone)
            putFloat("right_stick_inner_deadzone", c.rightStickInnerDeadzone)
            putFloat("right_stick_outer_deadzone", c.rightStickOuterDeadzone)
            putFloat("phys_left_stick_inner_deadzone", c.physLeftStickInnerDeadzone)
            putFloat("phys_left_stick_outer_deadzone", c.physLeftStickOuterDeadzone)
            putFloat("phys_right_stick_inner_deadzone", c.physRightStickInnerDeadzone)
            putFloat("phys_right_stick_outer_deadzone", c.physRightStickOuterDeadzone)
            putFloat("gamepad_sensitivity", c.gamepadSensitivity)
            putString("gamepad_layout_preset", c.gamepadLayoutPreset.name)
            putString("gamepad_haptic_level", c.gamepadHapticLevel.name)
            putString("game_rumble_level", c.gameRumbleLevel.name)
            putBoolean("gamepad_sprint_lock", c.gamepadSprintLockEnabled)
            putString("gamepad_custom_offsets", c.gamepadCustomOffsets)
            putString("gamepad_button_scales", c.gamepadButtonScales)
            putString("gamepad_hidden_buttons", c.gamepadHiddenButtons)
            putBoolean("gamepad_abxy_split_mode", c.gamepadAbxySplitMode)
            putBoolean("gamepad_dpad_split_mode", c.gamepadDpadSplitMode)
            putBoolean("use_per_game_gamepad_layout", c.usePerGameGamepadLayout)
            putString("per_game_layouts_json", c.perGameLayoutsJson)
            putString("physical_gamepad_button_map", c.physicalGamepadButtonMap)
            putBoolean("controller_shortcuts_enabled", c.controllerShortcutsEnabled)
            putInt("shortcut_toggle_stats_btn", c.shortcutToggleStatsButton)
            putInt("shortcut_toggle_vpad_btn", c.shortcutToggleVirtualPadButton)
            putInt("shortcut_toggle_rec_btn", c.shortcutToggleRecordingButton)
            putBoolean("system_revolution", c.systemRevolutionEnabled)
            putBoolean("stealth_headers", c.stealthHeadersEnabled)
            putBoolean("auto_healing_mesh", c.autoHealingProxyMesh)
            putBoolean("sdp_bitrate_boost", c.sdpBitrateBoostEnabled)
            putBoolean("zero_delay_handshake", c.zeroDelayDirectHandshake)
            putBoolean("anti_jp_enforcer", c.antiJapaneseEnforcer)
            putBoolean("xcloud_translate", c.xcloudTranslateEnabled)
            putString("translate_language", c.translateLanguage)
            putBoolean("preserve_titles", c.preserveGameTitles)
            putBoolean("bilingual_mode", c.bilingualMode)
            putBoolean("github_dict", c.githubDictionaryEnabled)
            putBoolean("hide_stream_stats", c.hideStreamStatsOverlay)
            putString("cellular_mode", c.cellularMode.name)
            putString("graphics_quality_mode_v2", c.cellularMode.name)
            putString("network_tuning_mode", c.networkTuningMode.name)
            putBoolean("wifi_speed_first", c.wifiSpeedFirstPriority)
            putBoolean("low_end_device_mode", c.lowEndDeviceMode)
            putString("stream_render_scale", c.streamRenderScale.name)
            putString("stream_fps_cap", c.streamFpsCap.name)
            putBoolean("gpu_load_reduction_enabled", c.gpuLoadReductionEnabled)
            putString("screen_record_quality", c.screenRecordQuality.name)
            putBoolean("auto_reduce_load_recording", c.autoReduceLoadWhenRecording)
            putString("recording_capture_engine_v3", c.recordingCaptureEngine.name)
            putString("recording_capture_engine", c.recordingCaptureEngine.name)
            putInt("video_contrast_v2", c.videoContrast)
            putInt("video_saturation_v2", c.videoSaturation)
            putInt("video_contrast", c.videoContrast)
            putInt("video_saturation", c.videoSaturation)
            putInt("video_brightness", c.videoBrightness)
            putString("custom_apk_download_url_v2", c.customApkDownloadUrl)
            putString("public_web_portal_url_v2", c.publicWebPortalUrl)
            putString("github_repo_slug_v2", c.githubRepoSlug)
            putBoolean("user_accepted_terms", c.userAcceptedTerms)
            putBoolean("user_accepted_terms_v2", c.userAcceptedTerms)
            putBoolean("auto_check_updates_on_launch", c.autoCheckUpdatesOnLaunch)
            putBoolean("enable_globalping_probe", c.enableGlobalpingProbe)
            putBoolean("hide_unreachable_dynamic_servers", c.hideUnreachableDynamicServers)
            putBoolean("enable_geoip_https_fallback", c.enableGeoIpHttpsFallback)
            putBoolean("allow_dbip_cellular_download", c.allowDbIpCellularDownload)
            putBoolean("allow_cellular_live_proxy_sync", c.allowCellularLiveProxySync)
            putString("xbox_web_ui_mode", c.xboxWebUiMode.name)
            apply()
        }
    }

    /**
     * Mục 7a & 7c: Tách ngôn ngữ giao diện app khỏi thị trường Xbox.
     * Mặc định luôn trả về "en-US" (danh sách trắng ban đầu chỉ gồm en-US) để tránh chuyển hướng
     * sang thị trường chưa hỗ trợ xCloud hoặc chưa xác minh.
     */
    fun resolveSystemXboxLocale(): String {
        return "en-US"
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

    fun formatXboxUrlWithLocale(
        url: String,
        targetLocale: String,
        uiMode: XboxWebUiMode = _config.value.xboxWebUiMode
    ): String {
        if (!url.contains("xbox.com", ignoreCase = true)) return url

        // Nếu đang ở chế độ Giao diện cũ (CLASSIC) mà URL là play.xbox.com, chuyển về www.xbox.com/<locale>/play...
        val playSubdomainPattern = Regex("""https?://play\.xbox\.com(?:/([a-zA-Z]{2}-[a-zA-Z]{2}))?(/.*)?""", RegexOption.IGNORE_CASE)
        val playSubMatch = playSubdomainPattern.find(url)
        if (playSubMatch != null) {
            val rawRest = playSubMatch.groupValues[2].ifBlank { "" }
            return if (uiMode == XboxWebUiMode.CLASSIC) {
                val normalizedPath = when {
                    rawRest.isBlank() || rawRest == "/" -> "/play"
                    rawRest.startsWith("/play") -> rawRest
                    else -> "/play$rawRest"
                }
                "https://www.xbox.com/$targetLocale$normalizedPath"
            } else {
                val cleanRest = if (rawRest.isBlank() || rawRest == "/") "/" else rawRest
                "https://play.xbox.com$cleanRest"
            }
        }

        val localePattern = Regex("""(https?://(?:www\.)?xbox\.com)/([a-zA-Z]{2}-[a-zA-Z]{2})(/.*)?""")
        val match = localePattern.find(url)
        if (match != null) {
            val domain = match.groupValues[1]
            val rest = match.groupValues[3] ?: ""
            if (uiMode == XboxWebUiMode.NEW_UI && (rest.isBlank() || rest == "/play" || rest == "/play/")) {
                return "https://play.xbox.com/"
            }
            return "$domain/$targetLocale$rest"
        }

        val noLocalePattern = Regex("""(https?://(?:www\.)?xbox\.com)(/play.*)?""")
        val noLocaleMatch = noLocalePattern.find(url)
        if (noLocaleMatch != null) {
            val domain = noLocaleMatch.groupValues[1]
            val rest = noLocaleMatch.groupValues[2] ?: "/play"
            if (uiMode == XboxWebUiMode.NEW_UI && (rest == "/play" || rest == "/play/")) {
                return "https://play.xbox.com/"
            }
            return "$domain/$targetLocale$rest"
        }

        return url
    }

    fun getUrlForCountry(countryCode: String): String {
        return _config.value.targetRegionUrl
    }

    fun adaptPlayUrl(url: String, countryCode: String? = null): String {
        val effectiveLocale = _config.value.getEffectiveLocale()
        return formatXboxUrlWithLocale(url, effectiveLocale, _config.value.xboxWebUiMode)
    }

    private val lastSliderLogTsBySetting = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun logSettingChange(name: String, oldVal: Any?, newVal: Any?, throttleMs: Long = 0L) {
        if (oldVal == newVal) return
        if (throttleMs > 0L) {
            val now = System.currentTimeMillis()
            val prev = lastSliderLogTsBySetting[name] ?: 0L
            if (now - prev < throttleMs) return
            lastSliderLogTsBySetting[name] = now
        }
        AppLogger.info("UI", "Đổi cài đặt $name: $oldVal -> $newVal")
    }

    fun updateXboxWebUiMode(mode: XboxWebUiMode) {
        val cur = _config.value
        val old = cur.xboxWebUiMode
        val uaMode = if (cur.forceDesktopUserAgent) "desktop" else "mobile"
        if (old == mode) {
            AppLogger.info(
                "WEB-UI",
                "Người dùng bấm chọn lại Giao diện web Xbox đang bật: ${mode.name} | ua=$uaMode (giữ nguyên cài đặt)"
            )
            return
        }
        _config.update { it.copy(xboxWebUiMode = mode) }
        persist(_config.value)
        logSettingChange("Giao diện web Xbox", old.name, mode.name)
        AppLogger.info(
            "WEB-UI",
            "Đổi chế độ Giao diện web Xbox: ${old.name} -> ${mode.name} | ua=$uaMode (nguồn: cài đặt người dùng)"
        )
    }

    fun update(newConfig: AppConfig) {
        _config.update { newConfig }
        persist(newConfig)
    }

    fun updateAppLanguage(language: AppLanguage) {
        val old = _config.value.appLanguage
        // Mục 7a: Không đụng tới thị trường Xbox (customLocale / useSystemLanguage) khi đổi ngôn ngữ giao diện app.
        _config.update {
            it.copy(
                appLanguage = language
            )
        }
        persist(_config.value)
        logSettingChange("Ngôn ngữ ứng dụng", old.code, language.code)
    }

    fun updateTranslateLanguage(langCode: String) {
        val old = _config.value.translateLanguage
        val normalized = langCode.trim().ifBlank { "OFF" }
        val isOff = normalized.equals("OFF", ignoreCase = true)
        val nextVal = if (isOff) "OFF" else normalized
        _config.update {
            it.copy(
                translateLanguage = nextVal,
                xcloudTranslateEnabled = !isOff
            )
        }
        persist(_config.value)
        logSettingChange("Ngôn ngữ dịch trang Xbox", old, nextVal)
    }

    fun resetXboxMarketToDefault() {
        val old = _config.value.getEffectiveLocale()
        _config.update {
            it.copy(
                customLocale = "en-US",
                useSystemLanguage = true
            )
        }
        persist(_config.value)
        AppLogger.info("UI", "Khôi phục mặc định thị trường Xbox: $old -> en-US")
    }

    fun updateUseSystemLanguage(useSystem: Boolean) {
        val old = _config.value.useSystemLanguage
        _config.update { it.copy(useSystemLanguage = useSystem) }
        persist(_config.value)
        logSettingChange("Dùng ngôn ngữ hệ thống cho Xbox", old, useSystem)
    }

    fun updateCustomLocale(locale: String) {
        val old = _config.value.customLocale
        val parsed = parseLocaleFromInput(locale)
        _config.update {
            it.copy(
                customLocale = parsed,
                useSystemLanguage = false
            )
        }
        persist(_config.value)
        logSettingChange("Thị trường Xbox (customLocale)", old, parsed)
    }

    fun updateSmartVpnEnabled(enabled: Boolean) {
        val old = _config.value.smartVpnEnabled
        _config.update { it.copy(smartVpnEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Smart VPN tự bật khi mở game", old, enabled)
    }

    fun updateAutoBypass(enabled: Boolean) {
        val old = _config.value.autoBypassEnabled
        _config.update { it.copy(autoBypassEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Tự động tắt VPN khi vào trận", old, enabled)
    }

    fun updateAutoBypassDelay(seconds: Int) {
        val old = _config.value.autoBypassDelaySeconds
        _config.update { it.copy(autoBypassDelaySeconds = seconds) }
        persist(_config.value)
        logSettingChange("Độ trễ tự tắt VPN", "${old}s", "${seconds}s")
    }

    fun updateDesktopUserAgent(enabled: Boolean) {
        val old = _config.value.forceDesktopUserAgent
        _config.update { it.copy(forceDesktopUserAgent = enabled) }
        persist(_config.value)
        logSettingChange("Giả lập trình duyệt máy tính (1080p UA)", old, enabled)
        if (old != enabled) {
            AppLogger.info(
                "WEB-UI",
                "Đổi kiểu User-Agent trực tiếp: ${if (old) "desktop" else "mobile"} -> ${if (enabled) "desktop" else "mobile"} | chế độ=${_config.value.xboxWebUiMode.name}"
            )
        }
    }

    fun updateClarityBoost(enabled: Boolean) {
        val old = _config.value.clarityBoostEnabled
        _config.update {
            it.copy(
                clarityBoostEnabled = enabled,
                clarityPreset = if (enabled && it.clarityPreset == ClarityPreset.OFF) ClarityPreset.BALANCED else it.clarityPreset
            )
        }
        persist(_config.value)
        logSettingChange("Làm nét hình ảnh (Clarity Boost)", old, enabled)
    }

    fun updateClarityPreset(preset: ClarityPreset) {
        val old = _config.value.clarityPreset
        _config.update {
            it.copy(
                clarityPreset = preset,
                clarityBoostEnabled = if (preset == ClarityPreset.OFF) false else it.clarityBoostEnabled
            )
        }
        persist(_config.value)
        logSettingChange("Mức làm nét (Clarity Preset)", old.name, preset.name)
    }

    fun updateMaxBitrate(mbps: Int) {
        val old = _config.value.maxBitrateMbps
        _config.update { it.copy(maxBitrateMbps = mbps) }
        persist(_config.value)
        logSettingChange("Giới hạn Bitrate", "${old}Mbps", "${mbps}Mbps")
    }

    fun updateAntiAfk(enabled: Boolean) {
        val old = _config.value.antiAfkKeepAlive
        _config.update { it.copy(antiAfkKeepAlive = enabled) }
        persist(_config.value)
        logSettingChange("Chống văng AFK", old, enabled)
    }

    fun updateInputPollingBoost(enabled: Boolean) {
        val old = _config.value.inputPollingBoost
        _config.update { it.copy(inputPollingBoost = enabled) }
        persist(_config.value)
        logSettingChange("Tăng tốc độ đọc tay cầm (Polling Boost)", old, enabled)
    }

    fun updateShowVirtualController(enabled: Boolean) {
        val old = _config.value.showVirtualController
        _config.update { it.copy(showVirtualController = enabled) }
        persist(_config.value)
        logSettingChange("Tay cầm ảo trên màn hình", old, enabled)
    }

    fun updateSystemRevolution(enabled: Boolean) {
        val old = _config.value.systemRevolutionEnabled
        _config.update { it.copy(systemRevolutionEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Tối ưu hóa hệ thống", old, enabled)
    }

    fun updateStealthHeaders(enabled: Boolean) {
        val old = _config.value.stealthHeadersEnabled
        _config.update { it.copy(stealthHeadersEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Stealth Headers", old, enabled)
    }

    fun updateAutoHealingProxyMesh(enabled: Boolean) {
        val old = _config.value.autoHealingProxyMesh
        _config.update { it.copy(autoHealingProxyMesh = enabled) }
        persist(_config.value)
        logSettingChange("Tự chuyển máy chủ dự phòng", old, enabled)
    }

    fun updateSdpBitrateBoost(enabled: Boolean) {
        val old = _config.value.sdpBitrateBoostEnabled
        _config.update { it.copy(sdpBitrateBoostEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Tối ưu SDP Bitrate", old, enabled)
    }

    fun updateZeroDelayDirectHandshake(enabled: Boolean) {
        val old = _config.value.zeroDelayDirectHandshake
        _config.update { it.copy(zeroDelayDirectHandshake = enabled) }
        persist(_config.value)
        logSettingChange("Bắt tay trực tiếp không trễ", old, enabled)
    }

    fun updateAntiJapaneseEnforcer(enabled: Boolean) {
        val old = _config.value.antiJapaneseEnforcer
        _config.update { it.copy(antiJapaneseEnforcer = enabled) }
        persist(_config.value)
        logSettingChange("Chống chuyển vùng tiếng Nhật", old, enabled)
    }

    fun updateXcloudTranslate(enabled: Boolean) {
        val old = _config.value.xcloudTranslateEnabled
        _config.update {
            val nextLang = if (enabled && it.translateLanguage.equals("OFF", ignoreCase = true)) {
                it.appLanguage.resolveTranslateTargetCode().ifBlank { "vi" }
            } else {
                it.translateLanguage
            }
            it.copy(
                xcloudTranslateEnabled = enabled,
                translateLanguage = nextLang
            )
        }
        persist(_config.value)
        logSettingChange("Dịch tự động giao diện Xbox", old, enabled)
    }

    fun updatePreserveGameTitles(enabled: Boolean) {
        val old = _config.value.preserveGameTitles
        _config.update { it.copy(preserveGameTitles = enabled) }
        persist(_config.value)
        logSettingChange("Giữ nguyên tên gốc của game", old, enabled)
    }

    fun updateBilingualMode(enabled: Boolean) {
        val old = _config.value.bilingualMode
        _config.update { it.copy(bilingualMode = enabled) }
        persist(_config.value)
        logSettingChange("Hiển thị song ngữ", old, enabled)
    }

    fun updateGithubDictionaryEnabled(enabled: Boolean) {
        val old = _config.value.githubDictionaryEnabled
        _config.update { it.copy(githubDictionaryEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Từ điển thuật ngữ game", old, enabled)
    }

    fun updateHideStreamStatsOverlay(hide: Boolean) {
        val old = _config.value.hideStreamStatsOverlay
        _config.update { it.copy(hideStreamStatsOverlay = hide) }
        persist(_config.value)
        logSettingChange("Ẩn bảng thông số kỹ thuật Xbox", old, hide)
    }

    fun updateCellularMode(mode: CellularOptimizationMode) {
        val prev = _config.value
        val old = prev.cellularMode
        val oldUa = prev.forceDesktopUserAgent
        _config.update {
            val nextLowEnd = if (mode != CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) false else it.lowEndDeviceMode
            it.copy(
                cellularMode = mode,
                lowEndDeviceMode = nextLowEnd,
                streamRenderScale = if (nextLowEnd) StreamRenderScale.PERFORMANCE_75 else mode.autoRenderScale,
                forceDesktopUserAgent = true,
                maxBitrateMbps = mode.bitrateMbps
            )
        }
        persist(_config.value)
        logSettingChange("Chế độ chất lượng đồ họa", old.name, mode.name)
        val newUa = _config.value.forceDesktopUserAgent
        if (old != mode || oldUa != newUa) {
            AppLogger.info(
                "WEB-UI",
                "Đổi chế độ chất lượng ${old.name} -> ${mode.name}: ua=${if (oldUa) "desktop" else "mobile"} -> ${if (newUa) "desktop" else "mobile"} (request1080p=${mode.request1080p})"
            )
        }
    }

    fun updateNetworkTuningMode(mode: NetworkTuningMode) {
        val old = _config.value.networkTuningMode
        _config.update { it.copy(networkTuningMode = mode) }
        persist(_config.value)
        logSettingChange("Chế độ thích ứng mạng", old.name, mode.name)
    }

    fun updateWifiSpeedFirstPriority(enabled: Boolean) {
        val old = _config.value.wifiSpeedFirstPriority
        _config.update { it.copy(wifiSpeedFirstPriority = enabled) }
        persist(_config.value)
        logSettingChange("Ưu tiên độ trễ thấp trên Wi-Fi", old, enabled)
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
        AppLogger.info("UI", "Bấm tối ưu hóa toàn diện hệ thống (1 chạm)")
    }

    fun updateTargetRegionUrl(url: String) {
        val match = Regex("""xbox\.com/([a-zA-Z]{2}-[a-zA-Z]{2})""").find(url)
        if (match != null) {
            updateCustomLocale(match.groupValues[1])
        }
    }

    fun updateUserAcceptedTerms(accepted: Boolean) {
        val old = _config.value.userAcceptedTerms
        _config.update { it.copy(userAcceptedTerms = accepted) }
        persist(_config.value)
        logSettingChange("Đồng ý Điều khoản sử dụng", old, accepted)
    }

    fun updateLowEndDeviceMode(enabled: Boolean) {
        val old = _config.value.lowEndDeviceMode
        _config.update {
            it.copy(
                lowEndDeviceMode = enabled,
                streamRenderScale = if (enabled) StreamRenderScale.PERFORMANCE_75 else it.cellularMode.autoRenderScale
            )
        }
        persist(_config.value)
        logSettingChange("Chế độ máy yếu (720p nhẹ)", old, enabled)
    }

    fun updateWebPortalSettings(
        apkDownloadUrl: String,
        webPortalUrl: String,
        githubRepo: String
    ) {
        _config.update {
            it.copy(
                customApkDownloadUrl = apkDownloadUrl.trim().ifBlank {
                    "https://github.com/muitenvn/CloudPlay-App/releases/latest"
                },
                publicWebPortalUrl = webPortalUrl.trim().ifBlank {
                    "https://muitenvn.github.io/CloudPlay-App/"
                },
                githubRepoSlug = githubRepo.trim().ifBlank {
                    "muitenvn/CloudPlay-App"
                }
            )
        }
        persist(_config.value)
        AppLogger.info("UI", "Cập nhật liên kết cổng tải xuống & kho mã nguồn: ${_config.value.githubRepoSlug}")
    }

    fun updateAutoHideVirtualOnPhysicalController(enabled: Boolean) {
        val old = _config.value.autoHideVirtualOnPhysicalController
        _config.update { it.copy(autoHideVirtualOnPhysicalController = enabled) }
        persist(_config.value)
        logSettingChange("Tự ẩn tay cầm ảo khi cắm tay cầm thật", old, enabled)
    }

    fun updateGamepadScale(scale: Float) {
        val old = _config.value.gamepadScale
        val next = scale.coerceIn(0.75f, 1.35f)
        _config.update { it.copy(gamepadScale = next) }
        persist(_config.value)
        logSettingChange("Kích thước nút tay cầm ảo", "${(old * 100).toInt()}%", "${(next * 100).toInt()}%", throttleMs = 400L)
    }

    fun updateGamepadOpacity(opacity: Float) {
        val old = _config.value.gamepadOpacity
        val next = opacity.coerceIn(0.25f, 0.95f)
        _config.update { it.copy(gamepadOpacity = next) }
        persist(_config.value)
        logSettingChange("Độ đậm tay cầm ảo", "${(old * 100).toInt()}%", "${(next * 100).toInt()}%", throttleMs = 400L)
    }

    fun updateGamepadDeadzone(deadzone: Float) {
        val old = _config.value.gamepadDeadzone
        val clamped = deadzone.coerceIn(0.02f, 0.25f)
        _config.update {
            it.copy(
                gamepadDeadzone = clamped,
                leftStickInnerDeadzone = clamped,
                rightStickInnerDeadzone = clamped
            )
        }
        persist(_config.value)
        logSettingChange("Vùng chết cần analog ảo", "${(old * 100).toInt()}%", "${(clamped * 100).toInt()}%", throttleMs = 400L)
    }

    fun updateVirtualStickDeadzones(
        leftInner: Float = _config.value.leftStickInnerDeadzone,
        leftOuter: Float = _config.value.leftStickOuterDeadzone,
        rightInner: Float = _config.value.rightStickInnerDeadzone,
        rightOuter: Float = _config.value.rightStickOuterDeadzone
    ) {
        val prev = _config.value
        val li = leftInner.coerceIn(0.00f, 0.30f)
        val lo = leftOuter.coerceIn(0.70f, 1.00f)
        val ri = rightInner.coerceIn(0.00f, 0.30f)
        val ro = rightOuter.coerceIn(0.70f, 1.00f)
        _config.update {
            it.copy(
                gamepadDeadzone = li.coerceIn(0.02f, 0.25f),
                leftStickInnerDeadzone = li,
                leftStickOuterDeadzone = lo,
                rightStickInnerDeadzone = ri,
                rightStickOuterDeadzone = ro
            )
        }
        persist(_config.value)
        logSettingChange(
            "Vùng chết cần ảo (L/R)",
            "L(${(prev.leftStickInnerDeadzone * 100).toInt()}-${(prev.leftStickOuterDeadzone * 100).toInt()}%) R(${(prev.rightStickInnerDeadzone * 100).toInt()}-${(prev.rightStickOuterDeadzone * 100).toInt()}%)",
            "L(${(li * 100).toInt()}-${(lo * 100).toInt()}%) R(${(ri * 100).toInt()}-${(ro * 100).toInt()}%)",
            throttleMs = 400L
        )
    }

    fun updatePhysicalStickDeadzones(
        leftInner: Float = _config.value.physLeftStickInnerDeadzone,
        leftOuter: Float = _config.value.physLeftStickOuterDeadzone,
        rightInner: Float = _config.value.physRightStickInnerDeadzone,
        rightOuter: Float = _config.value.physRightStickOuterDeadzone
    ) {
        val prev = _config.value
        val li = leftInner.coerceIn(0.00f, 0.30f)
        val lo = leftOuter.coerceIn(0.70f, 1.00f)
        val ri = rightInner.coerceIn(0.00f, 0.30f)
        val ro = rightOuter.coerceIn(0.70f, 1.00f)
        _config.update {
            it.copy(
                physLeftStickInnerDeadzone = li,
                physLeftStickOuterDeadzone = lo,
                physRightStickInnerDeadzone = ri,
                physRightStickOuterDeadzone = ro
            )
        }
        persist(_config.value)
        logSettingChange(
            "Vùng chết cần tay cầm thật (L/R)",
            "L(${(prev.physLeftStickInnerDeadzone * 100).toInt()}-${(prev.physLeftStickOuterDeadzone * 100).toInt()}%) R(${(prev.physRightStickInnerDeadzone * 100).toInt()}-${(prev.physRightStickOuterDeadzone * 100).toInt()}%)",
            "L(${(li * 100).toInt()}-${(lo * 100).toInt()}%) R(${(ri * 100).toInt()}-${(ro * 100).toInt()}%)",
            throttleMs = 400L
        )
    }

    fun resetPhysicalStickDeadzones() {
        updatePhysicalStickDeadzones(0.0f, 1.0f, 0.0f, 1.0f)
        AppLogger.info("UI", "Khôi phục mặc định: Vùng chết cần tay cầm thật (0% - 100%)")
    }

    fun updateGamepadSensitivity(sensitivity: Float) {
        val old = _config.value.gamepadSensitivity
        val next = sensitivity.coerceIn(0.60f, 1.60f)
        _config.update { it.copy(gamepadSensitivity = next) }
        persist(_config.value)
        logSettingChange("Độ nhạy cần tay cầm ảo", "${(old * 100).toInt()}%", "${(next * 100).toInt()}%", throttleMs = 400L)
    }

    fun updateGamepadLayoutPreset(preset: GamepadLayoutPreset) {
        val old = _config.value.gamepadLayoutPreset
        _config.update { it.copy(gamepadLayoutPreset = preset) }
        persist(_config.value)
        logSettingChange("Bố cục tay cầm ảo", old.name, preset.name)
    }

    fun updateGamepadHapticLevel(level: GamepadHapticLevel) {
        val old = _config.value.gamepadHapticLevel
        _config.update { it.copy(gamepadHapticLevel = level) }
        persist(_config.value)
        logSettingChange("Độ rung khi chạm nút ảo", old.name, level.name)
    }

    fun updateGameRumbleLevel(level: GamepadHapticLevel) {
        val old = _config.value.gameRumbleLevel
        _config.update { it.copy(gameRumbleLevel = level) }
        persist(_config.value)
        logSettingChange("Độ rung phản hồi từ game", old.name, level.name)
    }

    fun updateGamepadSprintLockEnabled(enabled: Boolean) {
        val old = _config.value.gamepadSprintLockEnabled
        _config.update { it.copy(gamepadSprintLockEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Khóa chạy nhanh cần trái (Sprint Lock)", old, enabled)
    }

    fun updateGamepadCustomOffsets(offsets: String, gameKey: String? = null) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (_config.value.usePerGameGamepadLayout && normKey.isNotEmpty()) {
            val current = getPerGameLayout(normKey) ?: PerGameGamepadLayout(
                offsets = _config.value.gamepadCustomOffsets,
                scales = _config.value.gamepadButtonScales,
                hiddenButtons = _config.value.gamepadHiddenButtons,
                abxySplit = _config.value.gamepadAbxySplitMode,
                dpadSplit = _config.value.gamepadDpadSplitMode
            )
            savePerGameLayout(normKey, current.copy(offsets = offsets))
            logSettingChange("Vị trí nút tay cầm ảo ($normKey)", current.offsets.length, offsets.length, throttleMs = 600L)
            return
        }
        val old = _config.value.gamepadCustomOffsets
        _config.update { it.copy(gamepadCustomOffsets = offsets) }
        persist(_config.value)
        logSettingChange("Vị trí nút tay cầm ảo", "${old.length} ký tự", "${offsets.length} ký tự", throttleMs = 600L)
    }

    fun updateGamepadButtonScales(scales: String, gameKey: String? = null) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (_config.value.usePerGameGamepadLayout && normKey.isNotEmpty()) {
            val current = getPerGameLayout(normKey) ?: PerGameGamepadLayout(
                offsets = _config.value.gamepadCustomOffsets,
                scales = _config.value.gamepadButtonScales,
                hiddenButtons = _config.value.gamepadHiddenButtons,
                abxySplit = _config.value.gamepadAbxySplitMode,
                dpadSplit = _config.value.gamepadDpadSplitMode
            )
            savePerGameLayout(normKey, current.copy(scales = scales))
            logSettingChange("Cỡ từng nút tay cầm ảo ($normKey)", current.scales, scales, throttleMs = 500L)
            return
        }
        val old = _config.value.gamepadButtonScales
        _config.update { it.copy(gamepadButtonScales = scales) }
        persist(_config.value)
        logSettingChange("Cỡ từng nút tay cầm ảo", old, scales, throttleMs = 500L)
    }

    fun updateGamepadHiddenButtons(hidden: String, gameKey: String? = null) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (_config.value.usePerGameGamepadLayout && normKey.isNotEmpty()) {
            val current = getPerGameLayout(normKey) ?: PerGameGamepadLayout(
                offsets = _config.value.gamepadCustomOffsets,
                scales = _config.value.gamepadButtonScales,
                hiddenButtons = _config.value.gamepadHiddenButtons,
                abxySplit = _config.value.gamepadAbxySplitMode,
                dpadSplit = _config.value.gamepadDpadSplitMode
            )
            savePerGameLayout(normKey, current.copy(hiddenButtons = hidden))
            logSettingChange("Nút ẩn tay cầm ảo ($normKey)", current.hiddenButtons.ifBlank { "không" }, hidden.ifBlank { "không" })
            return
        }
        val old = _config.value.gamepadHiddenButtons
        _config.update { it.copy(gamepadHiddenButtons = hidden) }
        persist(_config.value)
        logSettingChange("Nút ẩn tay cầm ảo", old.ifBlank { "không" }, hidden.ifBlank { "không" })
    }

    fun updateGamepadAbxySplitMode(split: Boolean, gameKey: String? = null) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (_config.value.usePerGameGamepadLayout && normKey.isNotEmpty()) {
            val current = getPerGameLayout(normKey) ?: PerGameGamepadLayout(
                offsets = _config.value.gamepadCustomOffsets,
                scales = _config.value.gamepadButtonScales,
                hiddenButtons = _config.value.gamepadHiddenButtons,
                abxySplit = _config.value.gamepadAbxySplitMode,
                dpadSplit = _config.value.gamepadDpadSplitMode
            )
            savePerGameLayout(normKey, current.copy(abxySplit = split))
            logSettingChange("Tách rời nút A/B/X/Y ($normKey)", current.abxySplit, split)
            return
        }
        val old = _config.value.gamepadAbxySplitMode
        _config.update { it.copy(gamepadAbxySplitMode = split) }
        persist(_config.value)
        logSettingChange("Tách rời nút A/B/X/Y", old, split)
    }

    fun updateGamepadDpadSplitMode(split: Boolean, gameKey: String? = null) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (_config.value.usePerGameGamepadLayout && normKey.isNotEmpty()) {
            val current = getPerGameLayout(normKey) ?: PerGameGamepadLayout(
                offsets = _config.value.gamepadCustomOffsets,
                scales = _config.value.gamepadButtonScales,
                hiddenButtons = _config.value.gamepadHiddenButtons,
                abxySplit = _config.value.gamepadAbxySplitMode,
                dpadSplit = _config.value.gamepadDpadSplitMode
            )
            savePerGameLayout(normKey, current.copy(dpadSplit = split))
            logSettingChange("Tách rời cụm phím D-Pad ($normKey)", current.dpadSplit, split)
            return
        }
        val old = _config.value.gamepadDpadSplitMode
        _config.update { it.copy(gamepadDpadSplitMode = split) }
        persist(_config.value)
        logSettingChange("Tách rời cụm phím D-Pad", old, split)
    }

    fun resetGamepadCustomOffsets(gameKey: String? = null) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (_config.value.usePerGameGamepadLayout && normKey.isNotEmpty()) {
            savePerGameLayout(normKey, PerGameGamepadLayout())
            AppLogger.info("UI", "Khôi phục mặc định: Bố cục tay cầm ảo cho game $normKey")
            return
        }
        _config.update {
            it.copy(
                gamepadCustomOffsets = "",
                gamepadButtonScales = "",
                gamepadHiddenButtons = "",
                gamepadAbxySplitMode = false,
                gamepadDpadSplitMode = false
            )
        }
        persist(_config.value)
        AppLogger.info("UI", "Khôi phục mặc định: Bố cục và vị trí nút tay cầm ảo")
    }

    fun updateUsePerGameGamepadLayout(enabled: Boolean) {
        val old = _config.value.usePerGameGamepadLayout
        _config.update { it.copy(usePerGameGamepadLayout = enabled) }
        persist(_config.value)
        logSettingChange("Lưu bố cục tay cầm riêng theo từng game", old, enabled)
    }

    fun normalizeGameLayoutKey(rawKey: String?): String {
        if (rawKey.isNullOrBlank()) return ""
        val trimmed = rawKey.trim().lowercase(Locale.ROOT)
        if (trimmed == "generic" || trimmed == "xbox cloud game" || trimmed == "xbox game") return ""
        return trimmed.replace(Regex("[^a-z0-9_-]"), "_").trim('_').take(64)
    }

    fun getPerGameLayout(gameKey: String?, cfg: AppConfig = _config.value): PerGameGamepadLayout? {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (normKey.isEmpty() || cfg.perGameLayoutsJson.isBlank()) return null
        return try {
            val root = org.json.JSONObject(cfg.perGameLayoutsJson)
            val obj = root.optJSONObject(normKey) ?: return null
            PerGameGamepadLayout(
                offsets = obj.optString("offsets", ""),
                scales = obj.optString("scales", ""),
                hiddenButtons = obj.optString("hidden", ""),
                abxySplit = obj.optBoolean("abxySplit", false),
                dpadSplit = obj.optBoolean("dpadSplit", false)
            )
        } catch (e: Exception) {
            AppLogger.error("UI", "Lỗi đọc bố cục tay cầm riêng ($normKey) — ${AppLogger.shortThrowable(e)}")
            null
        }
    }

    fun savePerGameLayout(gameKey: String, layout: PerGameGamepadLayout) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (normKey.isEmpty()) return
        try {
            val root = try {
                org.json.JSONObject(_config.value.perGameLayoutsJson)
            } catch (_: Exception) {
                org.json.JSONObject()
            }
            val obj = org.json.JSONObject().apply {
                put("offsets", layout.offsets)
                put("scales", layout.scales)
                put("hidden", layout.hiddenButtons)
                put("abxySplit", layout.abxySplit)
                put("dpadSplit", layout.dpadSplit)
            }
            root.put(normKey, obj)
            val nextJson = root.toString()
            _config.update { it.copy(perGameLayoutsJson = nextJson) }
            persist(_config.value)
        } catch (e: Exception) {
            AppLogger.error("UI", "Lỗi lưu bố cục tay cầm riêng ($normKey) — ${AppLogger.shortThrowable(e)}")
        }
    }

    fun useGlobalLayoutForGame(gameKey: String?) {
        val normKey = normalizeGameLayoutKey(gameKey)
        if (normKey.isEmpty()) return
        try {
            val root = org.json.JSONObject(_config.value.perGameLayoutsJson)
            if (root.has(normKey)) {
                root.remove(normKey)
                val nextJson = root.toString()
                _config.update { it.copy(perGameLayoutsJson = nextJson) }
                persist(_config.value)
                AppLogger.info("UI", "Chuyển game $normKey về dùng bố cục tay cầm chung")
            }
        } catch (e: Exception) {
            AppLogger.error("UI", "Lỗi xóa bố cục riêng ($normKey) — ${AppLogger.shortThrowable(e)}")
        }
    }

    fun resetAllPerGameLayouts() {
        _config.update { it.copy(perGameLayoutsJson = "{}") }
        persist(_config.value)
        AppLogger.info("UI", "Khôi phục mặc định: Xóa toàn bộ bố cục tay cầm riêng từng game")
    }

    fun parsePhysicalButtonMap(raw: String = _config.value.physicalGamepadButtonMap): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<Int, Int>()
        raw.split(",").forEach { pair ->
            val parts = pair.trim().split(":")
            if (parts.size == 2) {
                val src = parts[0].toIntOrNull()
                val dst = parts[1].toIntOrNull()
                if (src != null && dst != null && src in 0..16 && dst in 0..16 && src != dst) {
                    map[src] = dst
                }
            }
        }
        return map
    }

    fun serializePhysicalButtonMap(map: Map<Int, Int>): String {
        return map.entries
            .filter { it.key in 0..16 && it.value in 0..16 && it.key != it.value }
            .sortedBy { it.key }
            .joinToString(",") { "${it.key}:${it.value}" }
    }

    fun updatePhysicalButtonMapping(sourceButton: Int, targetButton: Int) {
        if (sourceButton !in 0..16 || targetButton !in 0..16) return
        val current = parsePhysicalButtonMap().toMutableMap()
        val prevTarget = current[sourceButton] ?: sourceButton
        if (sourceButton == targetButton) {
            current.remove(sourceButton)
        } else {
            current[sourceButton] = targetButton
        }
        val serialized = serializePhysicalButtonMap(current)
        _config.update { it.copy(physicalGamepadButtonMap = serialized) }
        persist(_config.value)
        logSettingChange(
            "Gán lại nút tay cầm thật (${getGamepadButtonShortLabel(sourceButton)})",
            getGamepadButtonShortLabel(prevTarget),
            getGamepadButtonShortLabel(targetButton)
        )
    }

    fun resetPhysicalButtonMap() {
        _config.update { it.copy(physicalGamepadButtonMap = "") }
        persist(_config.value)
        AppLogger.info("UI", "Khôi phục mặc định: Sơ đồ nút tay cầm vật lý")
    }

    fun updateControllerShortcutsEnabled(enabled: Boolean) {
        val old = _config.value.controllerShortcutsEnabled
        _config.update { it.copy(controllerShortcutsEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Phím tắt tay cầm vật lý (View + nút)", old, enabled)
    }

    fun updateControllerShortcutButtons(
        toggleStatsBtn: Int = _config.value.shortcutToggleStatsButton,
        toggleVirtualPadBtn: Int = _config.value.shortcutToggleVirtualPadButton,
        toggleRecordingBtn: Int = _config.value.shortcutToggleRecordingButton
    ) {
        val prev = _config.value
        val sStats = toggleStatsBtn.coerceIn(-1, 16)
        val sPad = toggleVirtualPadBtn.coerceIn(-1, 16)
        val sRec = toggleRecordingBtn.coerceIn(-1, 16)
        _config.update {
            it.copy(
                shortcutToggleStatsButton = sStats,
                shortcutToggleVirtualPadButton = sPad,
                shortcutToggleRecordingButton = sRec
            )
        }
        persist(_config.value)
        logSettingChange(
            "Tổ hợp phím tắt tay cầm",
            "Stats=${prev.shortcutToggleStatsButton},Pad=${prev.shortcutToggleVirtualPadButton},Rec=${prev.shortcutToggleRecordingButton}",
            "Stats=$sStats,Pad=$sPad,Rec=$sRec"
        )
    }

    val STANDARD_GAMEPAD_BUTTONS: List<Triple<Int, String, String>> = listOf(
        Triple(0, "A", "A (Cross)"),
        Triple(1, "B", "B (Circle)"),
        Triple(2, "X", "X (Square)"),
        Triple(3, "Y", "Y (Triangle)"),
        Triple(4, "LB", "LB (L1)"),
        Triple(5, "RB", "RB (R1)"),
        Triple(6, "LT", "LT (L2)"),
        Triple(7, "RT", "RT (R2)"),
        Triple(8, "View", "View (Back)"),
        Triple(9, "Menu", "Menu (Start)"),
        Triple(10, "L3", "L3 (Left Stick)"),
        Triple(11, "R3", "R3 (Right Stick)"),
        Triple(12, "D-Up", "D-Pad Up"),
        Triple(13, "D-Down", "D-Pad Down"),
        Triple(14, "D-Left", "D-Pad Left"),
        Triple(15, "D-Right", "D-Pad Right"),
        Triple(16, "Nexus", "Xbox Guide")
    )

    fun getGamepadButtonShortLabel(btnIndex: Int): String {
        return STANDARD_GAMEPAD_BUTTONS.firstOrNull { it.first == btnIndex }?.second ?: "Btn $btnIndex"
    }

    fun getGamepadButtonLabel(btnIndex: Int, isEn: Boolean): String {
        return STANDARD_GAMEPAD_BUTTONS.firstOrNull { it.first == btnIndex }?.third ?: if (isEn) "Button $btnIndex" else "Nút $btnIndex"
    }

    fun updateLeftStickInnerDeadzone(value: Float) {
        updateVirtualStickDeadzones(leftInner = value)
    }

    fun updateLeftStickOuterDeadzone(value: Float) {
        updateVirtualStickDeadzones(leftOuter = value)
    }

    fun updateRightStickInnerDeadzone(value: Float) {
        updateVirtualStickDeadzones(rightInner = value)
    }

    fun updateRightStickOuterDeadzone(value: Float) {
        updateVirtualStickDeadzones(rightOuter = value)
    }

    fun resetVirtualStickDeadzones() {
        updateVirtualStickDeadzones(0.08f, 0.95f, 0.08f, 0.95f)
        AppLogger.info("UI", "Khôi phục mặc định: Vùng chết cần ảo (8% - 95%)")
    }

    fun updatePhysLeftStickInnerDeadzone(value: Float) {
        updatePhysicalStickDeadzones(leftInner = value)
    }

    fun updatePhysLeftStickOuterDeadzone(value: Float) {
        updatePhysicalStickDeadzones(leftOuter = value)
    }

    fun updatePhysRightStickInnerDeadzone(value: Float) {
        updatePhysicalStickDeadzones(rightInner = value)
    }

    fun updatePhysRightStickOuterDeadzone(value: Float) {
        updatePhysicalStickDeadzones(rightOuter = value)
    }

    fun parseAllPerGameLayouts(rawJson: String = _config.value.perGameLayoutsJson): Map<String, PerGameGamepadLayout> {
        if (rawJson.isBlank()) return emptyMap()
        val result = mutableMapOf<String, PerGameGamepadLayout>()
        try {
            val root = org.json.JSONObject(rawJson)
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val obj = root.optJSONObject(k) ?: continue
                result[k] = PerGameGamepadLayout(
                    offsets = obj.optString("offsets", ""),
                    scales = obj.optString("scales", ""),
                    hiddenButtons = obj.optString("hidden", ""),
                    abxySplit = obj.optBoolean("abxySplit", false),
                    dpadSplit = obj.optBoolean("dpadSplit", false)
                )
            }
        } catch (e: Exception) {
            AppLogger.error("UI", "Lỗi đọc danh sách bố cục từng game — ${AppLogger.shortThrowable(e)}")
        }
        return result
    }

    fun clearAllPerGameLayouts() {
        resetAllPerGameLayouts()
    }

    fun updatePhysicalGamepadSingleButtonMap(sourceButton: Int, targetButton: Int) {
        updatePhysicalButtonMapping(sourceButton, targetButton)
    }

    fun resetPhysicalGamepadButtonMap() {
        resetPhysicalButtonMap()
    }

    fun updateShortcutToggleStatsButton(btnIdx: Int) {
        updateControllerShortcutButtons(toggleStatsBtn = btnIdx)
    }

    fun updateShortcutToggleVirtualPadButton(btnIdx: Int) {
        updateControllerShortcutButtons(toggleVirtualPadBtn = btnIdx)
    }

    fun updateShortcutToggleRecordingButton(btnIdx: Int) {
        updateControllerShortcutButtons(toggleRecordingBtn = btnIdx)
    }

    fun resetControllerShortcuts() {
        _config.update {
            it.copy(
                controllerShortcutsEnabled = true,
                shortcutToggleStatsButton = 3, // Y
                shortcutToggleVirtualPadButton = 1, // B
                shortcutToggleRecordingButton = 5 // RB
            )
        }
        persist(_config.value)
        AppLogger.info("UI", "Khôi phục mặc định: Phím tắt tay cầm vật lý (View + Y/B/RB)")
    }

    fun updateStreamRenderScale(scale: StreamRenderScale) {
        val old = _config.value.streamRenderScale
        _config.update { it.copy(streamRenderScale = scale) }
        persist(_config.value)
        logSettingChange("Tỉ lệ dựng hình (Render Scale)", old.name, scale.name)
    }

    fun updateStreamFpsCap(cap: StreamFpsCap) {
        val old = _config.value.streamFpsCap
        _config.update { it.copy(streamFpsCap = cap) }
        persist(_config.value)
        logSettingChange("Giới hạn khung hình (FPS Cap)", "${old.fps} FPS", "${cap.fps} FPS")
    }

    fun updateGpuLoadReductionEnabled(enabled: Boolean) {
        val old = _config.value.gpuLoadReductionEnabled
        _config.update { it.copy(gpuLoadReductionEnabled = enabled) }
        persist(_config.value)
        logSettingChange("Giảm tải GPU", old, enabled)
    }

    fun updateScreenRecordQuality(quality: ScreenRecordQuality) {
        val old = _config.value.screenRecordQuality
        _config.update { it.copy(screenRecordQuality = quality) }
        persist(_config.value)
        logSettingChange("Chất lượng quay màn hình", old.name, quality.name)
    }

    fun updateAutoReduceLoadWhenRecording(enabled: Boolean) {
        val old = _config.value.autoReduceLoadWhenRecording
        _config.update { it.copy(autoReduceLoadWhenRecording = enabled) }
        persist(_config.value)
        logSettingChange("Tự giảm tải khi đang quay màn hình", old, enabled)
    }

    fun updateRecordingCaptureEngine(engine: RecordingCaptureEngine) {
        val old = _config.value.recordingCaptureEngine
        _config.update { it.copy(recordingCaptureEngine = engine) }
        persist(_config.value)
        logSettingChange("Công nghệ thu hình (Capture Engine)", old.name, engine.name)
    }

    fun updateVideoContrast(contrast: Int) {
        val old = _config.value.videoContrast
        val next = contrast.coerceIn(80, 140)
        _config.update { it.copy(videoContrast = next) }
        persist(_config.value)
        logSettingChange("Độ tương phản video", "${old}%", "${next}%", throttleMs = 400L)
    }

    fun updateVideoSaturation(saturation: Int) {
        val old = _config.value.videoSaturation
        val next = saturation.coerceIn(80, 150)
        _config.update { it.copy(videoSaturation = next) }
        persist(_config.value)
        logSettingChange("Độ bão hòa màu video", "${old}%", "${next}%", throttleMs = 400L)
    }

    fun updateVideoBrightness(brightness: Int) {
        val old = _config.value.videoBrightness
        val next = brightness.coerceIn(80, 130)
        _config.update { it.copy(videoBrightness = next) }
        persist(_config.value)
        logSettingChange("Độ sáng video", "${old}%", "${next}%", throttleMs = 400L)
    }

    fun resetVideoDisplayDefaults() {
        _config.update {
            it.copy(
                videoContrast = 100,
                videoSaturation = 100,
                videoBrightness = 100
            )
        }
        persist(_config.value)
        AppLogger.info("UI", "Khôi phục mặc định: Màu sắc & Độ sáng video (100%)")
    }

    fun updateAutoCheckUpdatesOnLaunch(enabled: Boolean) {
        val old = _config.value.autoCheckUpdatesOnLaunch
        _config.update { it.copy(autoCheckUpdatesOnLaunch = enabled) }
        persist(_config.value)
        logSettingChange("Tự kiểm tra cập nhật khi mở app", old, enabled)
    }

    fun updateEnableGlobalpingProbe(enabled: Boolean) {
        val old = _config.value.enableGlobalpingProbe
        _config.update { it.copy(enableGlobalpingProbe = enabled) }
        persist(_config.value)
        logSettingChange("Trạm đo tham khảo Globalping", old, enabled)
    }

    fun updateHideUnreachableDynamicServers(hide: Boolean) {
        val old = _config.value.hideUnreachableDynamicServers
        _config.update { it.copy(hideUnreachableDynamicServers = hide) }
        persist(_config.value)
        logSettingChange("Ẩn máy chủ công cộng không phản hồi", old, hide)
    }

    fun updateEnableGeoIpHttpsFallback(enabled: Boolean) {
        val old = _config.value.enableGeoIpHttpsFallback
        _config.update { it.copy(enableGeoIpHttpsFallback = enabled) }
        persist(_config.value)
        logSettingChange("Dự phòng kiểm tra IP qua HTTPS", old, enabled)
    }

    fun updateAllowDbIpCellularDownload(allowed: Boolean) {
        val old = _config.value.allowDbIpCellularDownload
        _config.update { it.copy(allowDbIpCellularDownload = allowed) }
        persist(_config.value)
        logSettingChange("Cho phép tải cơ sở dữ liệu IP trên 4G/5G", old, allowed)
    }

    fun updateAllowCellularLiveProxySync(allowed: Boolean) {
        val old = _config.value.allowCellularLiveProxySync
        _config.update { it.copy(allowCellularLiveProxySync = allowed) }
        persist(_config.value)
        logSettingChange("Cho phép đồng bộ máy chủ công cộng trên 4G/5G", old, allowed)
    }

    /**
     * Chỉ khôi phục mặc định các cài đặt nằm trong mục "Nâng cao".
     * KHÔNG xóa phiên đăng nhập Xbox, lịch sử phiên, danh sách game, điều khoản hay các cài đặt Cơ bản.
     */
    fun resetAdvancedSettingsOnly() {
        _config.update { current ->
            current.copy(
                videoContrast = 100,
                videoSaturation = 100,
                videoBrightness = 100,
                lowEndDeviceMode = false,
                gpuLoadReductionEnabled = true,
                streamRenderScale = current.cellularMode.autoRenderScale,
                leftStickInnerDeadzone = 0.08f,
                leftStickOuterDeadzone = 0.95f,
                rightStickInnerDeadzone = 0.08f,
                rightStickOuterDeadzone = 0.95f,
                physLeftStickInnerDeadzone = 0.0f,
                physLeftStickOuterDeadzone = 1.0f,
                physRightStickInnerDeadzone = 0.0f,
                physRightStickOuterDeadzone = 1.0f,
                gamepadLayoutPreset = GamepadLayoutPreset.STANDARD,
                physicalGamepadButtonMap = "",
                controllerShortcutsEnabled = true,
                shortcutToggleStatsButton = 3, // Y
                shortcutToggleVirtualPadButton = 1, // B
                shortcutToggleRecordingButton = 5, // RB
                bilingualMode = false,
                useSystemLanguage = true,
                customLocale = "en-US",
                autoBypassDelaySeconds = 2,
                allowCellularLiveProxySync = false,
                enableGlobalpingProbe = false,
                enableGeoIpHttpsFallback = true,
                allowDbIpCellularDownload = false
            )
        }
        persist(_config.value)
        AppLogger.info(
            "RESET",
            "Khôi phục mặc định: Toàn bộ cài đặt Nâng cao (lý do: người dùng bấm trong Cài đặt | xboxWebUiMode hiện tại=${_config.value.xboxWebUiMode.name}, thị trường=${_config.value.getEffectiveLocale()})"
        )
    }
}
