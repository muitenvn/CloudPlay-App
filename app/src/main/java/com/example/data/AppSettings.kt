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
    XboxLanguage("ja-JP", "Tiếng Nhật", "日本語", "🇯🇵", "Japanese (日本語)"),
    XboxLanguage("ko-KR", "Tiếng Hàn", "한국어", "🇰🇷", "Korean (한국어)"),
    XboxLanguage("zh-TW", "Tiếng Trung (Phồn thể)", "繁體中文", "🇹🇼", "Traditional Chinese (繁體中文)"),
    XboxLanguage("fr-FR", "Tiếng Pháp", "Français", "🇫🇷", "French (Français)"),
    XboxLanguage("de-DE", "Tiếng Đức", "Deutsch", "🇩🇪", "German (Deutsch)"),
    XboxLanguage("es-ES", "Tiếng Tây Ban Nha", "Español", "🇪🇸", "Spanish (Español)"),
    XboxLanguage("pt-BR", "Tiếng Bồ Đào Nha", "Português (Brasil)", "🇧🇷", "Portuguese (Português)"),
    XboxLanguage("it-IT", "Tiếng Ý", "Italiano", "🇮🇹", "Italian (Italiano)"),
    XboxLanguage("pl-PL", "Tiếng Ba Lan", "Polski", "🇵🇱", "Polish (Polski)")
)

enum class ClarityPreset(
    val displayName: String,
    val description: String,
    val displayNameEn: String,
    val descriptionEn: String
) {
    OFF("Tắt", "Hình ảnh nguyên bản không can thiệp", "Off", "Original unmodified video stream"),
    BALANCED("Cân Bằng", "Tăng nét nhẹ + 8% tương phản (Khuyên dùng)", "Balanced", "Gentle sharpening + 8% contrast (Recommended)"),
    ULTRA_SHARP("Ultra Sharp", "Làm nét cạnh tối đa cho game 3D/FPS", "Ultra Sharp", "Maximum edge clarity for 3D/FPS games"),
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
    val description: String,
    val titleEn: String,
    val shortLabelEn: String,
    val subtitleEn: String,
    val deviceLoadBadgeEn: String,
    val descriptionEn: String
) {
    AUTO(
        id = "auto",
        title = "Tự Động",
        shortLabel = "Tự Động",
        subtitle = "Tự tối ưu theo điều kiện mạng & máy",
        bitrateMbps = 10,
        deviceLoadBadge = "⚡ Tự Động Tối Ưu",
        description = "Tự động nhận diện chất lượng Wi-Fi / 4G / 5G và theo dõi tải giải mã GPU của điện thoại để điều chỉnh đồ họa, giữ 60 FPS mượt mà và chống nóng máy.",
        titleEn = "Auto",
        shortLabelEn = "Auto",
        subtitleEn = "Auto-optimizes for network & device load",
        deviceLoadBadgeEn = "⚡ Smart Auto-Tune",
        descriptionEn = "Automatically detects Wi-Fi / 4G / 5G quality and GPU decode load to adjust bitrate smoothly, keeping 60 FPS and preventing overheating."
    ),
    DATA_SAVER_HIGH_SPEED(
        id = "saver",
        title = "Thấp",
        shortLabel = "Tiết Kiệm (5M)",
        subtitle = "Tiết kiệm dung lượng • 5 Mbps",
        bitrateMbps = 5,
        deviceLoadBadge = "❄️ Siêu Nhẹ • Mát Máy",
        description = "Giảm tải tối đa cho CPU/GPU điện thoại (giải mã phần cứng dưới 4ms), tiết kiệm đến 60% dung lượng data (~2.2GB/giờ) và đỡ hao pin.",
        titleEn = "Low",
        shortLabelEn = "Saver (5M)",
        subtitleEn = "Data Saver • 5 Mbps",
        deviceLoadBadgeEn = "❄️ Ultra Light • Cool",
        descriptionEn = "Minimizes phone CPU/GPU load (<4ms hardware decode), saves up to 60% mobile data (~2.2GB/hr), and extends battery life."
    ),
    MEDIUM(
        id = "medium",
        title = "Trung Bình",
        shortLabel = "Trung Bình (9M)",
        subtitle = "Cân bằng & mát máy • 9 Mbps",
        bitrateMbps = 9,
        deviceLoadBadge = "🌿 Êm Ái • Chống Nóng",
        description = "Mức cân bằng lý tưởng cho điện thoại tầm trung. Đảm bảo khung hình 60 FPS ổn định, tiết kiệm ~40% dung lượng và giữ máy mát khi chơi lâu.",
        titleEn = "Medium",
        shortLabelEn = "Medium (9M)",
        subtitleEn = "Balanced & cool • 9 Mbps",
        deviceLoadBadgeEn = "🌿 Smooth • Cool",
        descriptionEn = "Ideal balance for mid-range phones. Delivers stable 60 FPS, saves ~40% data, and keeps your device cool during long sessions."
    ),
    HIGH(
        id = "high",
        title = "Cao",
        shortLabel = "Cao (14M)",
        subtitle = "Sắc nét 1080p • 14 Mbps",
        bitrateMbps = 14,
        deviceLoadBadge = "✨ Chuẩn 1080p60",
        description = "Tái tạo hình ảnh Full HD 1080p60 sắc nét với chi tiết bề mặt rõ ràng, phù hợp khi mạng ổn định và điện thoại có chip xử lý khá trở lên.",
        titleEn = "High",
        shortLabelEn = "High (14M)",
        subtitleEn = "Crisp 1080p • 14 Mbps",
        deviceLoadBadgeEn = "✨ 1080p60 Standard",
        descriptionEn = "Delivers sharp Full HD 1080p60 visuals with crisp textures, ideal for stable connections and capable mobile processors."
    ),
    MAX_SPEED_UNCAPPED(
        id = "max",
        title = "Rất Cao",
        shortLabel = "Rất Cao (20M)",
        subtitle = "Đồ họa tối đa • 20 Mbps",
        bitrateMbps = 20,
        deviceLoadBadge = "🔥 Đồ Họa Tối Đa",
        description = "Bung trọn vẹn 20 Mbps băng thông cho chất lượng đồ họa cao nhất, phản hồi tay cầm tức thì cho máy cấu hình mạnh và mạng tốc độ cao.",
        titleEn = "Ultra",
        shortLabelEn = "Ultra (20M)",
        subtitleEn = "Max Graphics • 20 Mbps",
        deviceLoadBadgeEn = "🔥 Max Graphics",
        descriptionEn = "Unlocks the full 20 Mbps bandwidth for maximum visual fidelity and instant input response on high-speed networks."
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else title
    fun localizedShortLabel(isEn: Boolean): String = if (isEn) shortLabelEn else shortLabel
    fun localizedSubtitle(isEn: Boolean): String = if (isEn) subtitleEn else subtitle
    fun localizedBadge(isEn: Boolean): String = if (isEn) deviceLoadBadgeEn else deviceLoadBadge
    fun localizedDesc(isEn: Boolean): String = if (isEn) descriptionEn else description
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
        subtitle = "Triệt tiêu trễ đệm khi mạng yếu, giảm tải GPU tức thì",
        titleEn = "Prioritize Low Latency & Smooth 60 FPS",
        subtitleEn = "Eliminates buffer bloat on weak links, reduces GPU load"
    ),
    CELLULAR_DATA_SAVER(
        id = "cell_saver",
        title = "Thấp (Tiết Kiệm Dung Lượng)",
        subtitle = "Tiết kiệm 60% dung lượng, siêu mát máy & nhẹ GPU",
        titleEn = "Low (Mobile Data Saver)",
        subtitleEn = "Saves up to 60% data, ultra-cool & lightweight GPU decode"
    ),
    CELLULAR_MAX_SPEED(
        id = "cell_max",
        title = "Rất Cao (Đồ Họa Tối Đa)",
        subtitle = "Bung tối đa 20 Mbps cho hình ảnh sắc nét nhất",
        titleEn = "Ultra (Maximum Graphics)",
        subtitleEn = "Unlocks full 20 Mbps for the sharpest visual quality"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else title
    fun localizedSubtitle(isEn: Boolean): String = if (isEn) subtitleEn else subtitle
}

enum class AppLanguage(val code: String, val title: String, val titleEn: String) {
    SYSTEM("system", "Theo hệ thống (Tự động)", "System Default (Auto)"),
    VIETNAMESE("vi", "Tiếng Việt", "Tiếng Việt (Vietnamese)"),
    ENGLISH("en", "English", "English");

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else title
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
        "Bố cục công thái học cân đối cho mọi thể loại game",
        "Balanced ergonomic layout for all game genres"
    ),
    FPS_PRO(
        "Bắn Súng (FPS Pro)",
        "FPS / Shooter Pro",
        "Tăng diện tích cần ngắm R (+18%), ưu tiên cò LT/RT & L3/R3",
        "Larger Right Stick (+18%), prioritized LT/RT triggers & L3/R3"
    ),
    RACING(
        "Đua Xe (Forza / Racing)",
        "Racing / Driving",
        "Tối ưu cò ga/phanh LT/RT & cần lái L độ chính xác cao",
        "Optimized LT/RT throttle/brake triggers & high-precision Left Stick"
    ),
    COMPACT(
        "Gọn Nhẹ (RPG / Turn-Based)",
        "Compact / RPG",
        "Thu gọn kích thước cụm nút để tối đa tầm nhìn màn hình",
        "Compact footprint to maximize visible game screen area"
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
        titleVi = "100% Chuẩn Gốc (1080p)",
        titleEn = "100% Native (1080p)",
        shortVi = "100%",
        shortEn = "100%",
        descVi = "Độ nét tối đa theo luồng gốc của máy chủ",
        descEn = "Full native resolution from cloud server"
    ),
    BALANCED_85(
        id = "85",
        scaleFactor = 0.85f,
        targetHeightPx = 900,
        titleVi = "85% Cân Bằng (900p • -25% tải GPU)",
        titleEn = "85% Balanced (900p • -25% GPU load)",
        shortVi = "85%",
        shortEn = "85%",
        descVi = "Giảm 25% gánh nặng dựng hình mà vẫn giữ chữ & tâm ngắm sắc nét",
        descEn = "Cuts GPU rendering load by 25% while keeping text & crosshair sharp"
    ),
    PERFORMANCE_75(
        id = "75",
        scaleFactor = 0.75f,
        targetHeightPx = 720,
        titleVi = "75% Siêu Mượt (720p • -45% tải GPU)",
        titleEn = "75% Ultra Smooth (720p • -45% GPU load)",
        shortVi = "75%",
        shortEn = "75%",
        descVi = "Giảm gần một nửa tải GPU/CPU, chống tụt khung hình khi combat",
        descEn = "Halves GPU/CPU load to eliminate frame drops in heavy combat"
    ),
    ULTRA_LIGHT_60(
        id = "60",
        scaleFactor = 0.60f,
        targetHeightPx = 540,
        titleVi = "60% Siêu Nhẹ (540p • Máy yếu / Quay mượt)",
        titleEn = "60% Ultra Light (540p • Low-End / Recording)",
        shortVi = "60%",
        shortEn = "60%",
        descVi = "Nhẹ máy tối đa, lý tưởng cho máy yếu hoặc khi vừa chơi vừa quay màn hình",
        descEn = "Minimum hardware load, ideal for low-end phones or lag-free recording"
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
        titleVi = "30 FPS (Siêu mát • Giảm 50% tải máy)",
        titleEn = "30 FPS (Ultra Cool • -50% Device Load)",
        shortVi = "30 FPS",
        shortEn = "30 FPS",
        descVi = "Khóa 30 khung hình ổn định, máy cực mát và tiết kiệm pin gấp đôi",
        descEn = "Locks stable 30 FPS to keep device cool and double battery life"
    ),
    FPS_60(
        fps = 60,
        titleVi = "60 FPS (Mượt tiêu chuẩn)",
        titleEn = "60 FPS (Standard Smooth)",
        shortVi = "60 FPS",
        shortEn = "60 FPS",
        descVi = "Chuẩn 60 FPS mượt mà cho mọi tựa game hành động, đua xe, bắn súng",
        descEn = "Smooth 60 FPS standard for action, racing, and shooter titles"
    ),
    FPS_120(
        fps = 120,
        titleVi = "120 FPS (Màn hình tần số cao)",
        titleEn = "120 FPS (High Refresh Display)",
        shortVi = "120 FPS",
        shortEn = "120 FPS",
        descVi = "Mở khóa thương lượng 120Hz cho màn hình 90Hz/120Hz (Fortnite / Series X)",
        descEn = "Unlocks 120Hz stream negotiation for 90Hz/120Hz displays"
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
        titleVi = "Siêu Nhẹ 540p • 30FPS (0% Lag)",
        titleEn = "Ultra Light 540p • 30FPS (Zero Lag)",
        shortVi = "540p • 30F",
        shortEn = "540p • 30F",
        descVi = "Thu trực tiếp luồng hình ảnh với tải CPU/GPU gần bằng 0, không tụt FPS",
        descEn = "Direct stream capture with near-zero CPU/GPU overhead and zero frame drops"
    ),
    BALANCED_720P_30(
        id = "720p30",
        widthPx = 1280,
        heightPx = 720,
        fps = 30,
        bitrateBps = 4_500_000,
        titleVi = "Chuẩn HD 720p • 30FPS (Khuyên dùng)",
        titleEn = "Balanced HD 720p • 30FPS (Recommended)",
        shortVi = "720p • 30F",
        shortEn = "720p • 30F",
        descVi = "Video HD rõ nét, dung lượng gọn nhẹ và giữ game mượt ổn định",
        descEn = "Crisp HD video with compact file size and smooth gameplay"
    ),
    SMOOTH_720P_60(
        id = "720p60",
        widthPx = 1280,
        heightPx = 720,
        fps = 60,
        bitrateBps = 7_000_000,
        titleVi = "Mượt Cao 720p • 60FPS (Máy khỏe)",
        titleEn = "High Smooth 720p • 60FPS (Fast Device)",
        shortVi = "720p • 60F",
        shortEn = "720p • 60F",
        descVi = "Quay 60 khung hình/giây siêu mượt cho các pha highlight bắn súng/đua xe",
        descEn = "Silky-smooth 60 FPS recording for shooter and racing highlights"
    );

    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
}

enum class VideoAspectRatioMode(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val shortVi: String,
    val shortEn: String,
    val descVi: String,
    val descEn: String
) {
    FIT_16_9(
        id = "fit_16_9",
        titleVi = "Chuẩn 16:9 Gốc (Không bóp hình)",
        titleEn = "Native 16:9 Fit (No Squish)",
        shortVi = "16:9 Chuẩn",
        shortEn = "16:9 Fit",
        descVi = "Giữ đúng tỉ lệ 16:9 chuẩn của Xbox Cloud, chống bóp méo hình trên mọi màn hình 20:9 / 21:9",
        descEn = "Preserves exact 16:9 Xbox Cloud aspect ratio without squishing on 20:9 / 21:9 displays"
    ),
    CINEMA_18_9(
        id = "cinema_18_9",
        titleVi = "Mở Rộng 18:9 (Cân đối • Giảm viền đen)",
        titleEn = "Balanced 18:9 (Reduced Black Bars)",
        shortVi = "18:9 Cân đối",
        shortEn = "18:9 Wide",
        descVi = "Mở rộng nhẹ khung hình sang 18:9, giảm viền đen hai bên mà nhân vật vẫn cân đối",
        descEn = "Expands frame to 18:9 to reduce side bars while keeping characters natural"
    ),
    ZOOM_COVER(
        id = "zoom_cover",
        titleVi = "Tràn Viền Tự Nhiên (Zoom Cover • Không méo)",
        titleEn = "Full Screen Zoom Cover (No Distortion)",
        shortVi = "Tràn viền Zoom",
        shortEn = "Zoom Cover",
        descVi = "Phóng lấp đầy màn hình giữ nguyên tỉ lệ gốc (không bóp dẹt hình, cắt nhẹ mép trên/dưới)",
        descEn = "Fills the entire screen while keeping native proportions (crops top/bottom slightly)"
    ),
    STRETCH_FULL(
        id = "stretch_full",
        titleVi = "Kéo Giãn Toàn Màn (Full Stretch)",
        titleEn = "Full Screen Stretch",
        shortVi = "Kéo giãn Full",
        shortEn = "Full Stretch",
        descVi = "Kéo giãn lấp kín 100% diện tích màn hình thiết bị mà không cắt bất kỳ chi tiết nào",
        descEn = "Stretches video to fill 100% of device screen without cropping any HUD elements"
    ),
    CUSTOM_SCALE(
        id = "custom_scale",
        titleVi = "Tùy Chỉnh Tỉ Lệ Ngang / Dọc (Scale X/Y)",
        titleEn = "Custom Horizontal / Vertical Scale (X/Y)",
        shortVi = "Tùy chỉnh X/Y",
        shortEn = "Custom X/Y",
        descVi = "Tự tay kéo thanh trượt độ rộng ngang (Scale X) và độ cao dọc (Scale Y) theo đúng mắt nhìn",
        descEn = "Manually adjust horizontal width (Scale X) and vertical height (Scale Y) sliders"
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
        titleVi = "Quay Full Screen Toàn Màn Hình (Mặc định • Chuẩn .MP4 + Âm thanh)",
        titleEn = "Full Screen Recording (Default • Entire Display .MP4 + Audio)",
        shortVi = "Full Screen (Mặc định)",
        shortEn = "Full Screen (Default)",
        descVi = "Quay đúng 100% toàn bộ màn hình thực tế của máy (không bó hẹp 16:9), thu đầy đủ âm thanh Stereo vào file .MP4 và tuyệt đối không đổi tỉ lệ màn hình khi đang chơi",
        descEn = "Records 100% of the device's full screen with stereo game audio to .MP4 without altering live gameplay display"
    ),
    DIRECT_STREAM(
        id = "DIRECT_STREAM",
        titleVi = "Quay 16:9 Chuẩn Quốc Tế (Xuất .MP4 16:9 + Âm thanh • 0% Lag)",
        titleEn = "16:9 International Standard (.MP4 + Audio • Zero-Lag)",
        shortVi = "Chuẩn Quốc Tế 16:9",
        shortEn = "Standard 16:9",
        descVi = "Xuất video .MP4 theo đúng chuẩn tỉ lệ quốc tế 16:9 (1280x720) kèm âm thanh Stereo của game, xử lý ngầm hoàn toàn không làm thay đổi màn hình đang chơi",
        descEn = "Outputs standard 16:9 (1280x720) .MP4 video with stereo game audio in the background without changing live screen ratio"
    );


    fun localizedTitle(isEn: Boolean): String = if (isEn) titleEn else titleVi
    fun localizedShort(isEn: Boolean): String = if (isEn) shortEn else shortVi
    fun localizedDesc(isEn: Boolean): String = if (isEn) descEn else descVi
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
    val gamepadScale: Float = 1.0f, // 0.75f .. 1.35f
    val gamepadOpacity: Float = 0.70f, // 0.25f .. 0.95f
    val gamepadDeadzone: Float = 0.08f, // 0.02f .. 0.25f
    val gamepadSensitivity: Float = 1.0f, // 0.60f .. 1.60f
    val gamepadLayoutPreset: GamepadLayoutPreset = GamepadLayoutPreset.STANDARD,
    val gamepadHapticLevel: GamepadHapticLevel = GamepadHapticLevel.MEDIUM,
    val gamepadGyroEnabled: Boolean = false,
    val gamepadGyroSensitivity: Float = 1.0f,
    val gamepadSprintLockEnabled: Boolean = true,
    val gamepadCustomOffsets: String = "",
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
    // Điều chỉnh đồ họa chuyên sâu & Quay màn hình không lag
    val streamRenderScale: StreamRenderScale = StreamRenderScale.NATIVE_100,
    val streamFpsCap: StreamFpsCap = StreamFpsCap.FPS_60,
    val gpuLoadReductionEnabled: Boolean = true,
    val screenRecordQuality: ScreenRecordQuality = ScreenRecordQuality.BALANCED_720P_30,
    val autoReduceLoadWhenRecording: Boolean = true,
    val recordingCaptureEngine: RecordingCaptureEngine = RecordingCaptureEngine.HARDWARE_SCREEN,
    // Cài đặt Tỉ lệ khung hình & Hiển thị Video (Chống bóp méo hình)
    val videoAspectRatioMode: VideoAspectRatioMode = VideoAspectRatioMode.FIT_16_9,
    val videoScaleX: Float = 1.0f, // 0.70f .. 1.40f
    val videoScaleY: Float = 1.0f, // 0.70f .. 1.40f
    val videoContrast: Int = 105, // 80 .. 140 (%)
    val videoSaturation: Int = 108, // 80 .. 150 (%)
    val videoBrightness: Int = 100, // 80 .. 130 (%)
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

    fun isEnglishUI(): Boolean {
        return when (appLanguage) {
            AppLanguage.ENGLISH -> true
            AppLanguage.VIETNAMESE -> false
            AppLanguage.SYSTEM -> !Locale.getDefault().language.lowercase().startsWith("vi")
        }
    }

    fun isEffectiveTranslateEnabled(): Boolean {
        return xcloudTranslateEnabled && !isEnglishUI()
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
            gamepadScale = sp.getFloat("gamepad_scale", 1.0f).coerceIn(0.75f, 1.35f),
            gamepadOpacity = sp.getFloat("gamepad_opacity", 0.70f).coerceIn(0.25f, 0.95f),
            gamepadDeadzone = sp.getFloat("gamepad_deadzone", 0.08f).coerceIn(0.02f, 0.25f),
            gamepadSensitivity = sp.getFloat("gamepad_sensitivity", 1.0f).coerceIn(0.60f, 1.60f),
            gamepadLayoutPreset = try {
                GamepadLayoutPreset.valueOf(sp.getString("gamepad_layout_preset", GamepadLayoutPreset.STANDARD.name) ?: GamepadLayoutPreset.STANDARD.name)
            } catch (_: Exception) { GamepadLayoutPreset.STANDARD },
            gamepadHapticLevel = try {
                GamepadHapticLevel.valueOf(sp.getString("gamepad_haptic_level", GamepadHapticLevel.MEDIUM.name) ?: GamepadHapticLevel.MEDIUM.name)
            } catch (_: Exception) { GamepadHapticLevel.MEDIUM },
            gamepadGyroEnabled = sp.getBoolean("gamepad_gyro_enabled", false),
            gamepadGyroSensitivity = sp.getFloat("gamepad_gyro_sensitivity", 1.0f).coerceIn(0.40f, 2.50f),
            gamepadSprintLockEnabled = sp.getBoolean("gamepad_sprint_lock", true),
            gamepadCustomOffsets = sp.getString("gamepad_custom_offsets", "") ?: "",
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
            gpuLoadReductionEnabled = sp.getBoolean("gpu_load_reduction_enabled", true),
            screenRecordQuality = try {
                ScreenRecordQuality.valueOf(
                    sp.getString("screen_record_quality", ScreenRecordQuality.BALANCED_720P_30.name)
                        ?: ScreenRecordQuality.BALANCED_720P_30.name
                )
            } catch (_: Exception) { ScreenRecordQuality.BALANCED_720P_30 },
            autoReduceLoadWhenRecording = sp.getBoolean("auto_reduce_load_recording", true),
            recordingCaptureEngine = try {
                RecordingCaptureEngine.valueOf(
                    sp.getString("recording_capture_engine", RecordingCaptureEngine.HARDWARE_SCREEN.name)
                        ?: RecordingCaptureEngine.HARDWARE_SCREEN.name
                )
            } catch (_: Exception) { RecordingCaptureEngine.HARDWARE_SCREEN },
            videoAspectRatioMode = try {
                VideoAspectRatioMode.valueOf(
                    sp.getString("video_aspect_ratio_mode", VideoAspectRatioMode.FIT_16_9.name)
                        ?: VideoAspectRatioMode.FIT_16_9.name
                )
            } catch (_: Exception) { VideoAspectRatioMode.FIT_16_9 },
            videoScaleX = sp.getFloat("video_scale_x", 1.0f).coerceIn(0.70f, 1.40f),
            videoScaleY = sp.getFloat("video_scale_y", 1.0f).coerceIn(0.70f, 1.40f),
            videoContrast = sp.getInt("video_contrast", 105).coerceIn(80, 140),
            videoSaturation = sp.getInt("video_saturation", 108).coerceIn(80, 150),
            videoBrightness = sp.getInt("video_brightness", 100).coerceIn(80, 130),
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
            putFloat("gamepad_scale", c.gamepadScale)
            putFloat("gamepad_opacity", c.gamepadOpacity)
            putFloat("gamepad_deadzone", c.gamepadDeadzone)
            putFloat("gamepad_sensitivity", c.gamepadSensitivity)
            putString("gamepad_layout_preset", c.gamepadLayoutPreset.name)
            putString("gamepad_haptic_level", c.gamepadHapticLevel.name)
            putBoolean("gamepad_gyro_enabled", c.gamepadGyroEnabled)
            putFloat("gamepad_gyro_sensitivity", c.gamepadGyroSensitivity)
            putBoolean("gamepad_sprint_lock", c.gamepadSprintLockEnabled)
            putString("gamepad_custom_offsets", c.gamepadCustomOffsets)
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
            putString("stream_render_scale", c.streamRenderScale.name)
            putString("stream_fps_cap", c.streamFpsCap.name)
            putBoolean("gpu_load_reduction_enabled", c.gpuLoadReductionEnabled)
            putString("screen_record_quality", c.screenRecordQuality.name)
            putBoolean("auto_reduce_load_recording", c.autoReduceLoadWhenRecording)
            putString("recording_capture_engine", c.recordingCaptureEngine.name)
            putString("video_aspect_ratio_mode", c.videoAspectRatioMode.name)
            putFloat("video_scale_x", c.videoScaleX)
            putFloat("video_scale_y", c.videoScaleY)
            putInt("video_contrast", c.videoContrast)
            putInt("video_saturation", c.videoSaturation)
            putInt("video_brightness", c.videoBrightness)
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
        val autoTranslate = when (language) {
            AppLanguage.ENGLISH -> false
            AppLanguage.VIETNAMESE -> true
            AppLanguage.SYSTEM -> Locale.getDefault().language.lowercase().startsWith("vi")
        }
        _config.update {
            it.copy(
                appLanguage = language,
                xcloudTranslateEnabled = autoTranslate
            )
        }
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

    fun updateGamepadScale(scale: Float) {
        _config.update { it.copy(gamepadScale = scale.coerceIn(0.75f, 1.35f)) }
        persist(_config.value)
    }

    fun updateGamepadOpacity(opacity: Float) {
        _config.update { it.copy(gamepadOpacity = opacity.coerceIn(0.25f, 0.95f)) }
        persist(_config.value)
    }

    fun updateGamepadDeadzone(deadzone: Float) {
        _config.update { it.copy(gamepadDeadzone = deadzone.coerceIn(0.02f, 0.25f)) }
        persist(_config.value)
    }

    fun updateGamepadSensitivity(sensitivity: Float) {
        _config.update { it.copy(gamepadSensitivity = sensitivity.coerceIn(0.60f, 1.60f)) }
        persist(_config.value)
    }

    fun updateGamepadLayoutPreset(preset: GamepadLayoutPreset) {
        _config.update { it.copy(gamepadLayoutPreset = preset) }
        persist(_config.value)
    }

    fun updateGamepadHapticLevel(level: GamepadHapticLevel) {
        _config.update { it.copy(gamepadHapticLevel = level) }
        persist(_config.value)
    }

    fun updateGamepadGyroEnabled(enabled: Boolean) {
        _config.update { it.copy(gamepadGyroEnabled = enabled) }
        persist(_config.value)
    }

    fun updateGamepadGyroSensitivity(sensitivity: Float) {
        _config.update { it.copy(gamepadGyroSensitivity = sensitivity.coerceIn(0.40f, 2.50f)) }
        persist(_config.value)
    }

    fun updateGamepadSprintLockEnabled(enabled: Boolean) {
        _config.update { it.copy(gamepadSprintLockEnabled = enabled) }
        persist(_config.value)
    }

    fun updateGamepadCustomOffsets(offsets: String) {
        _config.update { it.copy(gamepadCustomOffsets = offsets) }
        persist(_config.value)
    }

    fun resetGamepadCustomOffsets() {
        _config.update { it.copy(gamepadCustomOffsets = "") }
        persist(_config.value)
    }

    fun updateStreamRenderScale(scale: StreamRenderScale) {
        _config.update { it.copy(streamRenderScale = scale) }
        persist(_config.value)
    }

    fun updateStreamFpsCap(cap: StreamFpsCap) {
        _config.update { it.copy(streamFpsCap = cap) }
        persist(_config.value)
    }

    fun updateGpuLoadReductionEnabled(enabled: Boolean) {
        _config.update { it.copy(gpuLoadReductionEnabled = enabled) }
        persist(_config.value)
    }

    fun updateScreenRecordQuality(quality: ScreenRecordQuality) {
        _config.update { it.copy(screenRecordQuality = quality) }
        persist(_config.value)
    }

    fun updateAutoReduceLoadWhenRecording(enabled: Boolean) {
        _config.update { it.copy(autoReduceLoadWhenRecording = enabled) }
        persist(_config.value)
    }

    fun updateRecordingCaptureEngine(engine: RecordingCaptureEngine) {
        _config.update { it.copy(recordingCaptureEngine = engine) }
        persist(_config.value)
    }

    fun updateVideoAspectRatioMode(mode: VideoAspectRatioMode) {
        _config.update { it.copy(videoAspectRatioMode = mode) }
        persist(_config.value)
    }

    fun updateVideoScaleX(scaleX: Float) {
        _config.update { it.copy(videoScaleX = scaleX.coerceIn(0.70f, 1.40f)) }
        persist(_config.value)
    }

    fun updateVideoScaleY(scaleY: Float) {
        _config.update { it.copy(videoScaleY = scaleY.coerceIn(0.70f, 1.40f)) }
        persist(_config.value)
    }

    fun updateVideoContrast(contrast: Int) {
        _config.update { it.copy(videoContrast = contrast.coerceIn(80, 140)) }
        persist(_config.value)
    }

    fun updateVideoSaturation(saturation: Int) {
        _config.update { it.copy(videoSaturation = saturation.coerceIn(80, 150)) }
        persist(_config.value)
    }

    fun updateVideoBrightness(brightness: Int) {
        _config.update { it.copy(videoBrightness = brightness.coerceIn(80, 130)) }
        persist(_config.value)
    }

    fun resetVideoDisplayDefaults() {
        _config.update {
            it.copy(
                videoAspectRatioMode = VideoAspectRatioMode.FIT_16_9,
                videoScaleX = 1.0f,
                videoScaleY = 1.0f,
                videoContrast = 105,
                videoSaturation = 108,
                videoBrightness = 100
            )
        }
        persist(_config.value)
    }
}
