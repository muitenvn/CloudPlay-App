package com.example.data

import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GameFilterTab {
    ALL,
    FREE_TO_PLAY,
    TOUCH_FRIENDLY,
    GAME_PASS
}

data class GameBookmark(
    val id: String,
    val title: String,
    val category: String,
    val playUrl: String,
    val freeToPlay: Boolean = false,
    val supportsTouch: Boolean = true,
    val accentColorHex: Long = 0xFF107C10,
    val badge: String? = null,
    val drawableResId: Int? = null,
    val rating: String = "4.8"
)

val INITIAL_XBOX_GAMES = listOf(
    GameBookmark(
        id = "forza_horizon_5",
        title = "Forza Horizon 5",
        category = "Đua xe thế giới mở",
        playUrl = "https://www.xbox.com/en-US/play/games/forza-horizon-5-standard-edition/9NKX70BBC2HN",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFFFF007A,
        badge = "Siêu Phẩm 60FPS",
        drawableResId = R.drawable.forza_cover_art_1791438033687,
        rating = "4.9"
    ),
    GameBookmark(
        id = "starfield",
        title = "Starfield",
        category = "Nhập vai viễn tưởng",
        playUrl = "https://www.xbox.com/en-US/play/games/starfield/9NCJSX2VD4B0",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF1B4965,
        badge = "Khám Phá Vũ Trụ",
        drawableResId = R.drawable.cyber_gaming_banner_1791438050320,
        rating = "4.7"
    ),
    GameBookmark(
        id = "fortnite",
        title = "Fortnite",
        category = "Sinh tồn / Battle Royale",
        playUrl = "https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2",
        freeToPlay = true,
        supportsTouch = true,
        accentColorHex = 0xFF0074E4,
        badge = "Tối Ưu Riêng • Free 100%",
        drawableResId = R.drawable.fortnite_cloud_poster_1791719536404,
        rating = "4.9"
    ),
    GameBookmark(
        id = "halo_infinite",
        title = "Halo Infinite",
        category = "Hành động / Bắn súng",
        playUrl = "https://www.xbox.com/en-US/play/games/halo-infinite/9PP5G1F0C2B6",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFF2D6A4F,
        badge = "Chiến Binh Master Chief",
        drawableResId = R.drawable.cloudplay_hero_banner_1791392804712,
        rating = "4.6"
    ),
    GameBookmark(
        id = "persona_3_reload",
        title = "Persona 3 Reload",
        category = "JRPG / Anime",
        playUrl = "https://www.xbox.com/en-US/play/games/persona-3-reload/9NZ8X7D58R0R",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFF0A2472,
        badge = "Cốt truyện đỉnh",
        rating = "4.9"
    ),
    GameBookmark(
        id = "palworld",
        title = "Palworld",
        category = "Sinh tồn / Bắt thú",
        playUrl = "https://www.xbox.com/en-US/play/games/palworld/9NKVB39XZ8QR",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF38B000,
        badge = "Rất Hot",
        rating = "4.8"
    ),
    GameBookmark(
        id = "flight_simulator",
        title = "Microsoft Flight Simulator",
        category = "Mô phỏng phi công",
        playUrl = "https://www.xbox.com/en-US/play/games/microsoft-flight-simulator-standard-game-of-the-year-edition/9PMQDM08SNK9",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF0077B6,
        badge = "Bản đồ toàn cầu",
        rating = "4.9"
    ),
    GameBookmark(
        id = "gta_v",
        title = "Grand Theft Auto V",
        category = "Hành động thế giới mở",
        playUrl = "https://www.xbox.com/en-US/play/games/grand-theft-auto-v/9NXHG24D2H10",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF386641,
        badge = "Game Pass",
        rating = "4.9"
    ),
    GameBookmark(
        id = "minecraft_dungeons",
        title = "Minecraft Dungeons",
        category = "Phiêu lưu hầm ngục",
        playUrl = "https://www.xbox.com/en-US/play/games/minecraft-dungeons/9N8NJLR6B392",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFFB07D62,
        badge = "Touch Controls",
        rating = "4.7"
    )
)

object GameLibraryRepository {
    private val _games = MutableStateFlow(INITIAL_XBOX_GAMES)
    val games: StateFlow<List<GameBookmark>> = _games.asStateFlow()

    fun addCustomGame(title: String, url: String) {
        if (title.isBlank() || url.isBlank()) return
        val newGame = GameBookmark(
            id = "custom_${System.currentTimeMillis()}",
            title = title.trim(),
            category = "Game tùy chỉnh",
            playUrl = url.trim(),
            freeToPlay = false,
            supportsTouch = true,
            accentColorHex = 0xFF00E676,
            badge = "Tự thêm",
            rating = "5.0"
        )
        _games.value = listOf(newGame) + _games.value
    }
}
