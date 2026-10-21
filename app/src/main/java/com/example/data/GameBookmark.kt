package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

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
    val rating: String = "4.8",
    val categoryEn: String = category,
    val badgeEn: String? = badge
) {
    fun localizedCategory(isEn: Boolean): String = if (isEn) {
        if (category == "Game tùy chỉnh") "Custom Game" else categoryEn
    } else category

    fun localizedBadge(isEn: Boolean): String? = if (isEn) {
        if (badge == "Tự thêm") "Custom" else badgeEn
    } else badge
}

val INITIAL_XBOX_GAMES = listOf(
    GameBookmark(
        id = "forza_horizon_5",
        title = "Forza Horizon 5",
        category = "Đua xe thế giới mở",
        categoryEn = "Open-World Racing",
        playUrl = "https://www.xbox.com/en-US/play/games/forza-horizon-5-standard-edition/9NKX70BBC2HN",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFFFF007A,
        badge = "Siêu Phẩm 60FPS",
        badgeEn = "60FPS Masterpiece",
        drawableResId = R.drawable.forza_cover_art_1791438033687,
        rating = "4.9"
    ),
    GameBookmark(
        id = "starfield",
        title = "Starfield",
        category = "Nhập vai viễn tưởng",
        categoryEn = "Sci-Fi Space RPG",
        playUrl = "https://www.xbox.com/en-US/play/games/starfield/9NCJSX2VD4B0",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF1B4965,
        badge = "Khám Phá Vũ Trụ",
        badgeEn = "Space Exploration",
        drawableResId = R.drawable.cyber_gaming_banner_1791438050320,
        rating = "4.7"
    ),
    GameBookmark(
        id = "fortnite",
        title = "Fortnite",
        category = "Sinh tồn / Battle Royale (Hỗ trợ 120FPS)",
        categoryEn = "Battle Royale (120FPS Ready)",
        playUrl = "https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2",
        freeToPlay = true,
        supportsTouch = true,
        accentColorHex = 0xFF0074E4,
        badge = "Hỗ Trợ 120FPS • Free 100%",
        badgeEn = "120FPS Ready • 100% Free",
        drawableResId = R.drawable.fortnite_cloud_poster_1791719536404,
        rating = "4.9"
    ),
    GameBookmark(
        id = "halo_infinite",
        title = "Halo Infinite",
        category = "Hành động / Bắn súng",
        categoryEn = "Action / Arena Shooter",
        playUrl = "https://www.xbox.com/en-US/play/games/halo-infinite/9PP5G1F0C2B6",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFF2D6A4F,
        badge = "Chiến Binh Master Chief",
        badgeEn = "Master Chief Saga",
        drawableResId = R.drawable.cloudplay_hero_banner_1791392804712,
        rating = "4.6"
    ),
    GameBookmark(
        id = "persona_3_reload",
        title = "Persona 3 Reload",
        category = "JRPG / Anime",
        categoryEn = "JRPG / Anime",
        playUrl = "https://www.xbox.com/en-US/play/games/persona-3-reload/9NZ8X7D58R0R",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFF0A2472,
        badge = "Cốt truyện đỉnh",
        badgeEn = "Top Story",
        rating = "4.9"
    ),
    GameBookmark(
        id = "palworld",
        title = "Palworld",
        category = "Sinh tồn / Bắt thú",
        categoryEn = "Survival / Creature Collector",
        playUrl = "https://www.xbox.com/en-US/play/games/palworld/9NKVB39XZ8QR",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF38B000,
        badge = "Rất Hot",
        badgeEn = "Trending",
        rating = "4.8"
    ),
    GameBookmark(
        id = "flight_simulator",
        title = "Microsoft Flight Simulator",
        category = "Mô phỏng phi công",
        categoryEn = "Flight Simulation",
        playUrl = "https://www.xbox.com/en-US/play/games/microsoft-flight-simulator-standard-game-of-the-year-edition/9PMQDM08SNK9",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF0077B6,
        badge = "Bản đồ toàn cầu",
        badgeEn = "World Map",
        rating = "4.9"
    ),
    GameBookmark(
        id = "gta_v",
        title = "Grand Theft Auto V",
        category = "Hành động thế giới mở",
        categoryEn = "Open-World Action",
        playUrl = "https://www.xbox.com/en-US/play/games/grand-theft-auto-v/9NXHG24D2H10",
        freeToPlay = false,
        supportsTouch = false,
        accentColorHex = 0xFF386641,
        badge = "Game Pass",
        badgeEn = "Game Pass",
        rating = "4.9"
    ),
    GameBookmark(
        id = "minecraft_dungeons",
        title = "Minecraft Dungeons",
        category = "Phiêu lưu hầm ngục",
        categoryEn = "Dungeon Crawler",
        playUrl = "https://www.xbox.com/en-US/play/games/minecraft-dungeons/9N8NJLR6B392",
        freeToPlay = false,
        supportsTouch = true,
        accentColorHex = 0xFFB07D62,
        badge = "Touch Controls",
        badgeEn = "Touch Controls",
        rating = "4.7"
    )
)

object GameLibraryRepository {
    private const val PREFS_NAME = "cloudplay_custom_games_prefs"
    private const val KEY_CUSTOM_GAMES_JSON = "custom_games_json"

    private var prefs: SharedPreferences? = null

    private val _games = MutableStateFlow(INITIAL_XBOX_GAMES)
    val games: StateFlow<List<GameBookmark>> = _games.asStateFlow()

    /**
     * Initializes persistent storage for custom user-added games via SharedPreferences.
     */
    fun init(context: Context) {
        if (prefs != null) return
        val sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sharedPrefs
        loadCustomGames(sharedPrefs)
    }

    private fun loadCustomGames(sharedPrefs: SharedPreferences) {
        try {
            val rawJson = sharedPrefs.getString(KEY_CUSTOM_GAMES_JSON, null) ?: return
            val arr = JSONArray(rawJson)
            val customList = mutableListOf<GameBookmark>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val playUrl = obj.optString("playUrl", "").trim()
                if (title.isBlank() || playUrl.isBlank()) continue
                customList.add(
                    GameBookmark(
                        id = obj.optString("id", "custom_$i"),
                        title = title,
                        category = obj.optString("category", "Game tùy chỉnh"),
                        playUrl = playUrl,
                        freeToPlay = obj.optBoolean("freeToPlay", false),
                        supportsTouch = obj.optBoolean("supportsTouch", true),
                        accentColorHex = obj.optLong("accentColorHex", 0xFF00E676),
                        badge = obj.optString("badge", "Tự thêm"),
                        rating = obj.optString("rating", "5.0")
                    )
                )
            }
            if (customList.isNotEmpty()) {
                _games.value = customList + INITIAL_XBOX_GAMES
            }
        } catch (_: Exception) {
        }
    }

    private fun saveCustomGames(allGames: List<GameBookmark>) {
        val sharedPrefs = prefs ?: return
        try {
            val customGames = allGames.filter { it.id.startsWith("custom_") }
            val arr = JSONArray()
            for (game in customGames) {
                val obj = JSONObject().apply {
                    put("id", game.id)
                    put("title", game.title)
                    put("category", game.category)
                    put("playUrl", game.playUrl)
                    put("freeToPlay", game.freeToPlay)
                    put("supportsTouch", game.supportsTouch)
                    put("accentColorHex", game.accentColorHex)
                    put("badge", game.badge ?: "Tự thêm")
                    put("rating", game.rating)
                }
                arr.put(obj)
            }
            sharedPrefs.edit().putString(KEY_CUSTOM_GAMES_JSON, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

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
        val updated = listOf(newGame) + _games.value
        _games.value = updated
        saveCustomGames(updated)
    }

    fun removeCustomGame(gameId: String) {
        val updated = _games.value.filterNot { it.id == gameId }
        _games.value = updated
        saveCustomGames(updated)
    }
}


