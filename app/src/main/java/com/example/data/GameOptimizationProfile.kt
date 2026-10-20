package com.example.data

import com.example.R

data class OptimizationCheckItem(
    val title: String,
    val description: String,
    val isReady: Boolean = true
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
    val checklist: List<OptimizationCheckItem>
)

object GameOptimizationRepository {

    val FORTNITE_PROFILE = GameOptimizationProfile(
        gameId = "fortnite",
        title = "FORTNITE BATTLE ROYALE",
        subtitle = "Tối ưu độc quyền: Hỗ trợ 60–120 FPS • Free 100% • Epic Touch Cảm Ứng • Zero-Delay",
        bannerResId = R.drawable.fortnite_cloud_poster_1791719536404,
        primaryAccentHex = 0xFF0074E4, // Fortnite Electric Blue
        secondaryAccentHex = 0xFF9C27B0, // Epic Purple
        badge = "TỐI ƯU ĐỘC QUYỀN FORTNITE • 120 FPS",
        isFreeToPlay = true,
        recommendedFps = "60–120 FPS",
        nativeTouchSupport = true,
        loadingQuote = "Nhảy dù Battle Bus • Loot súng huyền thoại • Giành Victory Royale! 👑",
        tips = listOf(
            "Fortnite trên Xbox Cloud hỗ trợ chế độ 120 FPS (trên phần cứng máy chủ Xbox Series X)! Bạn có thể bật 120 FPS Mode trong phần Cài đặt Video của Fortnite.",
            "Fortnite là game MIỄN PHÍ 100% trên Xbox Cloud, không cần mua gói Game Pass Ultimate!",
            "Game hỗ trợ phím cảm ứng riêng của Epic Games. Bạn có thể bấm biểu tượng 🎮 trên Game Bar để ẩn tay cầm ảo nếu dùng cảm ứng của game.",
            "Chế độ Direct Network Bypass giữ ping dưới 30ms, giúp bắn shotgun và xây nhà không bị delay phản xạ."
        ),
        checklist = listOf(
            OptimizationCheckItem("Máy chủ thi đấu", "Định tuyến xCloud Tokyo/Singapore (<30ms)"),
            OptimizationCheckItem("Tần số khung hình", "Đồng bộ màn hình 60–120Hz (Hỗ trợ 120 FPS Mode)"),
            OptimizationCheckItem("Chế độ điều khiển", "Hỗ trợ kép: Touch Epic Controls + Gamepad"),
            OptimizationCheckItem("Tối ưu phản xạ", "Kích hoạt Low-Latency Shooter Preset 60–120FPS")
        )
    )

    val FORZA_PROFILE = GameOptimizationProfile(
        gameId = "forza_horizon_5",
        title = "FORZA HORIZON 5",
        subtitle = "Đua xe Mexico 60FPS • Tối ưu phản hồi vô lăng & Trigger rung",
        bannerResId = R.drawable.forza_cover_art_1791438033687,
        primaryAccentHex = 0xFFFF007A,
        secondaryAccentHex = 0xFFFF9E00,
        badge = "RACING 60FPS PRESET",
        isFreeToPlay = false,
        recommendedFps = "60 FPS Ultra",
        nativeTouchSupport = true,
        loadingQuote = "Lái xe tốc độ cao qua những cung đường hoang dã tuyệt đẹp tại Mexico! 🏎️",
        tips = listOf(
            "Bật độ rung phản hồi xúc giác trong Game Bar để cảm nhận tiếng bánh xe trượt chân thực.",
            "Giữ đường truyền ổn định để hình ảnh đổ bóng xe hơi và cảnh vật luôn sắc nét chuẩn 1080p."
        ),
        checklist = listOf(
            OptimizationCheckItem("Tần số khung hình", "Khóa 60 FPS mượt mà cho tốc độ cao"),
            OptimizationCheckItem("Phản hồi xúc giác", "Kích hoạt mô phỏng rung Trigger LT/RT"),
            OptimizationCheckItem("Độ sắc nét cảnh quan", "Clarity Boost khử nhòe chuyển động")
        )
    )

    val STARFIELD_PROFILE = GameOptimizationProfile(
        gameId = "starfield",
        title = "STARFIELD",
        subtitle = "Khám phá vũ trụ Constellation • Tối ưu HDR Clarity Boost",
        bannerResId = R.drawable.cyber_gaming_banner_1791438050320,
        primaryAccentHex = 0xFF00B4D8,
        secondaryAccentHex = 0xFF0077B6,
        badge = "SPACE EXPLORATION PRESET",
        isFreeToPlay = false,
        recommendedFps = "30-60 FPS",
        nativeTouchSupport = false,
        loadingQuote = "Du hành giữa các vì sao, khám phá hơn 1000 hành tinh bí ẩn! 🚀",
        tips = listOf(
            "Sử dụng cần Analog ảo hoặc tay cầm Bluetooth để điều khiển phi thuyền linh hoạt hơn.",
            "Bật chế độ Clarity Boost trong Game Bar để các vì sao và bề mặt hành tinh rõ nét hơn."
        ),
        checklist = listOf(
            OptimizationCheckItem("Đồ họa không gian", "Tối ưu độ tương phản tối sâu OLED"),
            OptimizationCheckItem("Cần điều khiển", "Dual-stick layout cho lái tàu vũ trụ")
        )
    )

    val HALO_PROFILE = GameOptimizationProfile(
        gameId = "halo_infinite",
        title = "HALO INFINITE",
        subtitle = "Huyền thoại Master Chief • Bắn súng Arena phản xạ cực nhanh",
        bannerResId = R.drawable.cloudplay_hero_banner_1791392804712,
        primaryAccentHex = 0xFF2D6A4F,
        secondaryAccentHex = 0xFF52B788,
        badge = "SPARTAN ARENA PRESET",
        isFreeToPlay = false,
        recommendedFps = "60 FPS Arena",
        nativeTouchSupport = true,
        loadingQuote = "Chiến binh Spartan trở lại bảo vệ loài người trước thế lực Banished! 🛡️",
        tips = listOf(
            "Cần độ trễ thấp để ngắm bắn Battle Rifle chuẩn từng viên đạn.",
            "Tay cầm Bluetooth hoặc phím ảo ABXY độ nhạy cao là lựa chọn tối ưu cho Halo."
        ),
        checklist = listOf(
            OptimizationCheckItem("Độ trễ đầu vào", "Zero Input Delay Mode kích hoạt"),
            OptimizationCheckItem("Âm thanh vòm", "Tối ưu tiếng bước chân địch trong Arena")
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
            bannerResId = R.drawable.gamepass_hub_banner_1791394609676,
            primaryAccentHex = 0xFF107C10, // Xbox Green
            secondaryAccentHex = 0xFF00E676,
            badge = "XBOX CLOUD PRESET",
            isFreeToPlay = false,
            recommendedFps = "60 FPS Stream",
            nativeTouchSupport = true,
            loadingQuote = "Tận hưởng kho game đồ sộ đỉnh cao mọi lúc mọi nơi! 🎮",
            tips = listOf(
                "Bạn có thể mở thanh Game Bar (nút 🎮 nổi) bất cứ lúc nào để kiểm tra ping hoặc bật tắt tay cầm ảo.",
                "Chế độ Direct Network Bypass tự động chuyển kết nối trực tiếp giúp giảm tối đa độ trễ."
            ),
            checklist = listOf(
                OptimizationCheckItem("Định tuyến mạng", "Smart Bypass xCloud Server"),
                OptimizationCheckItem("Bộ điều khiển", "Tích hợp Tay Cầm Ảo + Haptic Feedback"),
                OptimizationCheckItem("Chất lượng stream", "1080p 60FPS High Bitrate")
            )
        )
    }
}
