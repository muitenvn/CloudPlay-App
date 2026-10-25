package com.example.data

import com.example.R

data class OptimizationCheckItem(
    val title: String,
    val description: String,
    val isReady: Boolean = true,
    val titleEn: String = title,
    val descriptionEn: String = description
)

data class GameOptimizationProfile(
    val gameId: String,
    val title: String,
    val subtitle: String,
    val bannerResId: Int,
    val primaryAccentHex: Long,
    val secondaryAccentHex: Long,
    val badge: String,
    val isFreeToPlay: Boolean,
    val recommendedFps: String = "60 FPS",
    val nativeTouchSupport: Boolean = true,
    val loadingQuote: String,
    val tips: List<String>,
    val checklist: List<OptimizationCheckItem>,
    val subtitleEn: String = subtitle,
    val badgeEn: String = badge,
    val loadingQuoteEn: String = loadingQuote,
    val tipsEn: List<String> = tips
) {
    fun localizedSubtitle(isEn: Boolean): String = if (isEn) subtitleEn else subtitle
    fun localizedBadge(isEn: Boolean): String = if (isEn) badgeEn else badge
    fun localizedTips(isEn: Boolean): List<String> = if (isEn) tipsEn else tips
}

object GameOptimizationRepository {

    val FORTNITE_PROFILE = GameOptimizationProfile(
        gameId = "fortnite",
        title = "FORTNITE BATTLE ROYALE",
        subtitle = "Hỗ trợ Cảm ứng Epic & Tay cầm ảo • Âm thanh bước chân rõ • Thao tác nhạy",
        subtitleEn = "Epic Touch & Virtual Pad • Clear Footsteps Audio • Responsive Controls",
        bannerResId = R.drawable.cyber_gaming_banner_1791438050320,
        primaryAccentHex = 0xFF0074E4, // Fortnite Electric Blue
        secondaryAccentHex = 0xFF9C27B0, // Epic Purple
        badge = "⚡ HỖ TRỢ CẢM ỨNG & TAY CẦM",
        badgeEn = "⚡ TOUCH & GAMEPAD READY",
        isFreeToPlay = true,
        recommendedFps = "60 FPS",
        nativeTouchSupport = true,
        loadingQuote = "Nhảy dù Battle Bus • Điều khiển nhạy • Giành Victory Royale! 👑",
        loadingQuoteEn = "Drop from the Battle Bus • Responsive controls • Claim Victory Royale! 👑",
        tips = listOf(
            "Bật phản hồi cảm ứng nhanh",
            "Mở thanh điều khiển để chuyển 1 chạm giữa [👆 Cảm ứng Epic] (dùng nút cảm ứng gốc của game) và [🎮 Tay cầm ảo].",
            "Tối ưu âm thanh nổi giúp nghe rõ tiếng bước chân và hướng tiếng súng.",
            "Hỗ trợ chạm nhiều ngón cùng lúc để vừa di chuyển, vừa xây dựng và ngắm bắn mượt mà."
        ),
        tipsEn = listOf(
            "Enable fast touch response",
            "Open the control bar to switch in 1 tap between [👆 Epic Touch] (game's native touch buttons) and [🎮 Virtual Pad].",
            "Stereo audio is tuned for clear footsteps, chest cues, and directional gunfire.",
            "Multi-touch support is enabled for smooth building, editing, and aiming."
        ),
        checklist = listOf(
            OptimizationCheckItem("Phản hồi thao tác", "Bật phản hồi cảm ứng nhanh", true, "Fast Response", "Enable fast touch response"),
            OptimizationCheckItem("Chuyển đổi điều khiển", "Đổi nhanh giữa Cảm ứng Epic và Tay cầm ảo", true, "Control Switch", "1-tap switch between Epic Touch and Virtual Pad"),
            OptimizationCheckItem("Âm thanh trong trận", "Nghe rõ tiếng bước chân và hướng súng", true, "Clear Audio", "Clear footsteps and directional sound"),
            OptimizationCheckItem("Cảm ứng đa điểm", "Hỗ trợ bấm nhiều ngón cùng lúc", true, "Multi-Touch", "Smooth multi-finger touch support")
        )
    )

    val FORZA_PROFILE = GameOptimizationProfile(
        gameId = "forza_horizon_5",
        title = "FORZA HORIZON 5",
        subtitle = "Đua xe Mexico 60FPS • Tối ưu phản hồi vô lăng & Trigger rung",
        subtitleEn = "60FPS Mexico Racing • Optimized steering response & haptic triggers",
        bannerResId = R.drawable.forza_cover_art_1791438033687,
        primaryAccentHex = 0xFFFF007A,
        secondaryAccentHex = 0xFFFF9E00,
        badge = "RACING 60FPS PRESET",
        badgeEn = "RACING 60FPS PRESET",
        isFreeToPlay = false,
        recommendedFps = "60 FPS Ultra",
        nativeTouchSupport = true,
        loadingQuote = "Lái xe tốc độ cao qua những cung đường hoang dã tuyệt đẹp tại Mexico! 🏎️",
        loadingQuoteEn = "Race at high speed across the vibrant open roads of Mexico! 🏎️",
        tips = listOf(
            "Bật độ rung phản hồi xúc giác trong Game Bar để cảm nhận tiếng bánh xe trượt chân thực.",
            "Giữ đường truyền ổn định để hình ảnh đổ bóng xe hơi và cảnh vật luôn sắc nét chuẩn 1080p."
        ),
        tipsEn = listOf(
            "Use a physical controller or virtual triggers for precise throttle and drift control.",
            "Enable Clarity Boost in the HUD to keep road textures and car reflections razor-sharp at 1080p."
        ),
        checklist = listOf(
            OptimizationCheckItem("Tần số khung hình", "Khóa 60 FPS mượt mà cho tốc độ cao", true, "Frame Rate", "Locked 60 FPS for high-speed racing"),
            OptimizationCheckItem("Phản hồi xúc giác", "Kích hoạt mô phỏng rung Trigger LT/RT", true, "Haptic Feedback", "LT/RT trigger vibration active"),
            OptimizationCheckItem("Độ sắc nét cảnh quan", "Clarity Boost khử nhòe chuyển động", true, "Visual Clarity", "Clarity Boost motion sharpening")
        )
    )

    val STARFIELD_PROFILE = GameOptimizationProfile(
        gameId = "starfield",
        title = "STARFIELD",
        subtitle = "Khám phá vũ trụ Constellation • Tối ưu HDR Clarity Boost",
        subtitleEn = "Constellation Space Exploration • HDR Clarity Boost tuned",
        bannerResId = R.drawable.cyber_gaming_banner_1791438050320,
        primaryAccentHex = 0xFF00B4D8,
        secondaryAccentHex = 0xFF0077B6,
        badge = "SPACE EXPLORATION PRESET",
        badgeEn = "SPACE EXPLORATION PRESET",
        isFreeToPlay = false,
        recommendedFps = "30-60 FPS",
        nativeTouchSupport = false,
        loadingQuote = "Du hành giữa các vì sao, khám phá hơn 1000 hành tinh bí ẩn! 🚀",
        loadingQuoteEn = "Journey among the stars and explore over 1,000 uncharted planets! 🚀",
        tips = listOf(
            "Sử dụng cần Analog ảo hoặc tay cầm Bluetooth để điều khiển phi thuyền linh hoạt hơn.",
            "Bật chế độ Clarity Boost trong Game Bar để các vì sao và bề mặt hành tinh rõ nét hơn."
        ),
        tipsEn = listOf(
            "Use Bluetooth gamepad or virtual dual-sticks for smooth starship dogfighting.",
            "Turn on Clarity Boost in the HUD for deeper space contrast and sharper planetary detail."
        ),
        checklist = listOf(
            OptimizationCheckItem("Đồ họa không gian", "Tối ưu độ tương phản tối sâu OLED", true, "Space Visuals", "Deep OLED contrast tuning"),
            OptimizationCheckItem("Cần điều khiển", "Dual-stick layout cho lái tàu vũ trụ", true, "Flight Controls", "Dual-stick starship layout")
        )
    )

    val HALO_PROFILE = GameOptimizationProfile(
        gameId = "halo_infinite",
        title = "HALO INFINITE",
        subtitle = "Huyền thoại Master Chief • Bắn súng Arena phản xạ nhạy",
        subtitleEn = "Master Chief Campaign & Arena • Responsive input tuning",
        bannerResId = R.drawable.cloudplay_hero_banner_1791392804712,
        primaryAccentHex = 0xFF2D6A4F,
        secondaryAccentHex = 0xFF52B788,
        badge = "SPARTAN ARENA PRESET",
        badgeEn = "SPARTAN ARENA PRESET",
        isFreeToPlay = false,
        recommendedFps = "60 FPS Arena",
        nativeTouchSupport = true,
        loadingQuote = "Chiến binh Spartan trở lại bảo vệ loài người trước thế lực Banished! 🛡️",
        loadingQuoteEn = "Become the Master Chief and defend humanity against the Banished! 🛡️",
        tips = listOf(
            "Bật phản hồi cảm ứng nhanh để ngắm bắn Battle Rifle chính xác.",
            "Tay cầm Bluetooth hoặc phím ảo ABXY độ nhạy cao là lựa chọn tối ưu cho Halo."
        ),
        tipsEn = listOf(
            "Fast touch response keeps aim response crisp for Battle Rifle precision.",
            "Pair a Bluetooth controller or use responsive virtual sticks for Arena combat."
        ),
        checklist = listOf(
            OptimizationCheckItem("Độ trễ đầu vào", "Bật phản hồi cảm ứng nhanh", true, "Input Latency", "Enable fast touch response"),
            OptimizationCheckItem("Âm thanh vòm", "Tối ưu tiếng bước chân địch trong Arena", true, "Spatial Audio", "Stereo 128kbps footsteps clarity")
        )
    )

    fun getProfileForUrl(url: String): GameOptimizationProfile {
        val lower = url.lowercase()
        return when {
            lower.contains("fortnite") || lower.contains("bt5p2x999vh2") -> FORTNITE_PROFILE
            lower.contains("forza") || lower.contains("9nkx70bbc2hn") -> FORZA_PROFILE
            lower.contains("starfield") || lower.contains("9ncjsx2vd4b0") -> STARFIELD_PROFILE
            lower.contains("halo") || lower.contains("9pp5g1f0c2b6") -> HALO_PROFILE
            else -> buildGenericProfile(url)
        }
    }

    fun getProfileForGameId(gameId: String): GameOptimizationProfile {
        return when (gameId.lowercase()) {
            "fortnite" -> FORTNITE_PROFILE
            "forza_horizon_5" -> FORZA_PROFILE
            "starfield" -> STARFIELD_PROFILE
            "halo_infinite" -> HALO_PROFILE
            else -> FORTNITE_PROFILE // default fallback with rich experience
        }
    }

    private fun buildGenericProfile(url: String): GameOptimizationProfile {
        val title = when {
            url.contains("palworld") -> "PALWORLD"
            url.contains("minecraft") -> "MINECRAFT DUNGEONS"
            url.contains("flight") -> "MICROSOFT FLIGHT SIMULATOR"
            url.contains("persona") -> "PERSONA 3 RELOAD"
            url.contains("gta") -> "GRAND THEFT AUTO V"
            else -> "XBOX CLOUD GAMING"
        }

        return GameOptimizationProfile(
            gameId = "generic",
            title = title,
            subtitle = "Chơi game Xbox chuẩn Console trực tiếp qua Cloud Streaming",
            subtitleEn = "Console-grade Xbox Cloud Gaming with direct low-latency streaming",
            bannerResId = R.drawable.gamepass_hub_banner_1791394609676,
            primaryAccentHex = 0xFF107C10, // Xbox Green
            secondaryAccentHex = 0xFF00E676,
            badge = "XBOX CLOUD PRESET",
            badgeEn = "XBOX CLOUD PRESET",
            isFreeToPlay = false,
            recommendedFps = "60 FPS Stream",
            nativeTouchSupport = true,
            loadingQuote = "Tận hưởng kho game đồ sộ đỉnh cao mọi lúc mọi nơi! 🎮",
            loadingQuoteEn = "Enjoy console-quality cloud gaming anywhere, anytime! 🎮",
            tips = listOf(
                "Bạn có thể mở thanh Game Bar (nút 🎮 nổi) bất cứ lúc nào để kiểm tra ping hoặc bật tắt tay cầm ảo.",
                "Chế độ Direct Network Bypass tự động chuyển kết nối trực tiếp giúp giảm tối đa độ trễ."
            ),
            tipsEn = listOf(
                "Tap the floating HUD anytime to check live latency, switch quality, or toggle touch controls.",
                "Auto VPN Bypass switches to your direct ISP connection once the game starts for minimal lag."
            ),
            checklist = listOf(
                OptimizationCheckItem("Định tuyến mạng", "Smart Bypass xCloud Server", true, "Network Routing", "Smart Bypass xCloud Server"),
                OptimizationCheckItem("Bộ điều khiển", "Tích hợp Tay Cầm Ảo + Haptic Feedback", true, "Controller", "Virtual Gamepad + Haptic Feedback"),
                OptimizationCheckItem("Chất lượng stream", "1080p 60FPS High Bitrate", true, "Stream Quality", "1080p 60FPS High Bitrate")
            )
        )
    }
}
