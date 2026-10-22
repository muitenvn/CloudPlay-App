package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class GameFilterTab {
    ALL,
    FAVORITES,
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
    val badgeEn: String? = badge,
    val imageUrl: String? = null
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
    private const val RECENT_PREFS_NAME = "cloudplay_recent_game_prefs"
    private const val KEY_CUSTOM_GAMES_JSON = "custom_games_json"
    private const val KEY_SYNCED_GAMES_JSON = "synced_xbox_games_json_v1"
    private const val KEY_LAST_CATALOG_SYNC_MS = "last_catalog_sync_ms"
    private const val KEY_FAVORITE_IDS = "favorite_game_ids"
    private const val KEY_LAST_PLAYED_ID = "last_played_game_id"

    private const val CLOUD_CATALOG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=29a81209-df6f-41fd-a528-2ae6b91f719c&language=en-us&market=US"
    private const val TOUCH_CATALOG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=7d86c25b-b99e-4090-90b3-54a7514b0240&language=en-us&market=US"

    private val ACCENT_PALETTE = longArrayOf(
        0xFF107C10L, 0xFF0074E4L, 0xFFFF007AL, 0xFF00B4D8L,
        0xFF38B000L, 0xFF7209B7L, 0xFFF77F00L, 0xFF2D6A4FL
    )

    private var prefs: SharedPreferences? = null
    private var recentPrefs: SharedPreferences? = null
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _games = MutableStateFlow(INITIAL_XBOX_GAMES)
    val games: StateFlow<List<GameBookmark>> = _games.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _lastPlayedId = MutableStateFlow<String?>(null)
    val lastPlayedId: StateFlow<String?> = _lastPlayedId.asStateFlow()

    private val _isSyncingCatalog = MutableStateFlow(false)
    val isSyncingCatalog: StateFlow<Boolean> = _isSyncingCatalog.asStateFlow()

    /**
     * Initializes persistent storage for custom user-added games, auto-synced Xbox Cloud catalog games,
     * favorites, and last played game via SharedPreferences.
     */
    fun init(context: Context) {
        if (prefs == null) {
            val appCtx = context.applicationContext
            val sharedPrefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val legacyRecentPrefs = appCtx.getSharedPreferences(RECENT_PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sharedPrefs
            recentPrefs = legacyRecentPrefs

            val customList = loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
            val syncedList = loadGamesArrayFromPrefs(sharedPrefs, KEY_SYNCED_GAMES_JSON)
            rebuildMergedGames(customList, syncedList)

            val savedFavs = sharedPrefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
            _favoriteIds.value = savedFavs.toSet()

            val savedLast = sharedPrefs.getString(KEY_LAST_PLAYED_ID, null)
                ?: legacyRecentPrefs.getString(KEY_LAST_PLAYED_ID, null)
            _lastPlayedId.value = savedLast

            val lastSyncMs = sharedPrefs.getLong(KEY_LAST_CATALOG_SYNC_MS, 0L)
            val now = System.currentTimeMillis()
            if (syncedList.size < 15 || (now - lastSyncMs) > 12 * 3600 * 1000L) {
                syncLiveXboxCatalog(force = false)
            }
        }
    }

    private fun rebuildMergedGames(
        customList: List<GameBookmark>,
        syncedList: List<GameBookmark>
    ) {
        val syncedByProductId = syncedList.associateBy { extractProductId(it.playUrl) }
        val syncedById = syncedList.associateBy { it.id.lowercase(Locale.ROOT) }

        val enrichedInitial = INITIAL_XBOX_GAMES.map { initGame ->
            val pid = extractProductId(initGame.playUrl)
            val match = (if (pid.isNotBlank()) syncedByProductId[pid] else null)
                ?: syncedById[initGame.id.lowercase(Locale.ROOT)]
            if (match != null && !match.imageUrl.isNullOrBlank()) {
                initGame.copy(imageUrl = match.imageUrl)
            } else {
                initGame
            }
        }

        val existingIds = HashSet<String>()
        val existingProductIds = HashSet<String>()
        val merged = ArrayList<GameBookmark>(customList.size + enrichedInitial.size + syncedList.size)

        fun addUnique(item: GameBookmark) {
            val idKey = item.id.lowercase(Locale.ROOT)
            val pid = extractProductId(item.playUrl)
            if (existingIds.contains(idKey)) return
            if (pid.isNotBlank() && existingProductIds.contains(pid)) return
            existingIds.add(idKey)
            if (pid.isNotBlank()) existingProductIds.add(pid)
            merged.add(item)
        }

        customList.forEach { addUnique(it) }
        enrichedInitial.forEach { addUnique(it) }
        syncedList.forEach { addUnique(it) }

        _games.value = merged
    }

    fun extractProductId(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val clean = url.substringBefore("?").substringBefore("#").trimEnd('/')
        val afterGames = clean.substringAfter("/play/games/", "")
            .ifBlank { clean.substringAfter("/play/launch/", "") }
        if (afterGames.isBlank()) return ""
        val segments = afterGames.split("/").filter { it.isNotBlank() }
        if (segments.size >= 2) {
            val candidate = segments.last().uppercase(Locale.ROOT)
            if (candidate.length in 10..14 && candidate.all { it.isLetterOrDigit() }) {
                return candidate
            }
        } else if (segments.size == 1) {
            val candidate = segments.first().uppercase(Locale.ROOT)
            if (candidate.length == 12 && candidate.all { it.isLetterOrDigit() && it !in 'a'..'z' }) {
                return candidate
            }
        }
        return ""
    }

    fun extractSlug(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val clean = url.substringBefore("?").substringBefore("#").trimEnd('/')
        val afterGames = clean.substringAfter("/play/games/", "")
            .ifBlank { clean.substringAfter("/play/launch/", "") }
        if (afterGames.isBlank()) return ""
        return afterGames.substringBefore("/").lowercase(Locale.ROOT).trim()
    }

    private fun slugifyTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "xbox-game" }
    }

    private fun formatSlugToTitle(slug: String): String {
        if (slug.isBlank()) return "Xbox Cloud Game"
        return slug.split("-")
            .filter { it.isNotBlank() && it != "standard" && it != "edition" && it != "xbox" && it != "series" }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
            .ifBlank { "Xbox Cloud Game" }
    }

    private fun normalizeImageUrl(rawImg: String?): String? {
        val trimmed = rawImg?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed.startsWith("data:")) return null
        val httpsUrl = when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> "https://${trimmed.removePrefix("http://")}"
            trimmed.startsWith("https://") -> trimmed
            else -> return null
        }
        return if (httpsUrl.contains("store-images.s-microsoft.com") && !httpsUrl.contains("?")) {
            "$httpsUrl?w=360&h=540&format=jpg"
        } else {
            httpsUrl
        }
    }

    fun toggleFavorite(gameId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (!current.add(gameId)) {
            current.remove(gameId)
        }
        val updated = current.toSet()
        _favoriteIds.value = updated
        prefs?.edit()?.putStringSet(KEY_FAVORITE_IDS, updated)?.apply()
    }

    fun recordLastPlayed(gameId: String) {
        if (gameId.isBlank()) return
        _lastPlayedId.value = gameId
        prefs?.edit()?.putString(KEY_LAST_PLAYED_ID, gameId)?.apply()
        recentPrefs?.edit()?.putString(KEY_LAST_PLAYED_ID, gameId)?.apply()
    }

    fun recordLastPlayedByUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val result = onWebGamePlayedOrDiscovered(
            rawTitle = null,
            rawUrl = trimmed,
            imageUrl = null,
            markAsLastPlayed = true
        )
        if (result == null && trimmed.contains("fortnite", ignoreCase = true)) {
            recordLastPlayed("fortnite")
        }
    }

    /**
     * Automatically detects any game opened or launched inside the Xbox Cloud WebView,
     * matches or adds it to GameLibraryRepository, and records it as the last played game
     * so "Chơi tiếp / Continue" on HomeScreen always shows the exact game played on the web.
     */
    fun onWebGamePlayedOrDiscovered(
        rawTitle: String?,
        rawUrl: String,
        imageUrl: String? = null,
        category: String? = null,
        supportsTouch: Boolean? = null,
        markAsLastPlayed: Boolean = true
    ): GameBookmark? {
        val cleanUrl = rawUrl.trim()
        val isXboxGameUrl = cleanUrl.contains("/play/games/", ignoreCase = true) ||
            cleanUrl.contains("/play/launch/", ignoreCase = true)

        val cleanedTitle = rawTitle
            ?.replace(Regex("\\s*[•|\\-–—]\\s*Xbox Cloud Gaming.*", RegexOption.IGNORE_CASE), "")
            ?.replace(Regex("\\s*\\|?\\s*Xbox.*", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()
        val hasValidTitle = cleanedTitle.isNotBlank() &&
            !cleanedTitle.equals("Xbox Cloud Gaming", ignoreCase = true) &&
            !cleanedTitle.equals("Xbox Cloud Game", ignoreCase = true) &&
            !cleanedTitle.equals("Xbox Game", ignoreCase = true) &&
            !cleanedTitle.equals("Fortnite | Xbox Cloud Gaming", ignoreCase = true)

        val productId = extractProductId(cleanUrl)
        val slug = extractSlug(cleanUrl)
        val normImage = normalizeImageUrl(imageUrl)

        val currentList = _games.value
        val matched = currentList.firstOrNull { item ->
            val itemPid = extractProductId(item.playUrl)
            val itemSlug = extractSlug(item.playUrl)
            (productId.isNotBlank() && itemPid.equals(productId, ignoreCase = true)) ||
                (slug.length > 2 && itemSlug.equals(slug, ignoreCase = true)) ||
                (slug.length > 2 && slug.contains(item.id.replace("_", "-"), ignoreCase = true)) ||
                (hasValidTitle && item.title.equals(cleanedTitle, ignoreCase = true))
        } ?: if (cleanUrl.contains("fortnite", ignoreCase = true) || cleanedTitle.contains("fortnite", ignoreCase = true)) {
            currentList.firstOrNull { it.id == "fortnite" }
        } else null

        if (matched != null) {
            var updatedItem = matched
            if (normImage != null && matched.imageUrl.isNullOrBlank()) {
                updatedItem = updatedItem.copy(imageUrl = normImage)
            }
            if (hasValidTitle && matched.title.startsWith("Xbox ", ignoreCase = true)) {
                updatedItem = updatedItem.copy(title = cleanedTitle)
            }
            if (updatedItem != matched) {
                val updatedList = currentList.map { if (it.id == matched.id) updatedItem else it }
                _games.value = updatedList
                saveSyncedGames(updatedList)
            }
            if (markAsLastPlayed) {
                recordLastPlayed(matched.id)
            }
            if (updatedItem.imageUrl.isNullOrBlank() && updatedItem.drawableResId == null && productId.isNotBlank()) {
                fetchSingleProductMetadataAsync(productId)
            }
            return updatedItem
        }

        if (!isXboxGameUrl && !hasValidTitle) return null
        if (productId.isBlank() && slug.isBlank() && !hasValidTitle) return null

        val resolvedTitle = if (hasValidTitle) cleanedTitle else formatSlugToTitle(slug)
        val resolvedSlug = if (slug.isNotBlank()) slug else slugifyTitle(resolvedTitle)
        val canonicalPlayUrl = if (productId.isNotBlank()) {
            "https://www.xbox.com/en-US/play/games/$resolvedSlug/$productId"
        } else if (isXboxGameUrl) {
            cleanUrl.replace("/play/launch/", "/play/games/").substringBefore("?")
        } else {
            return null
        }

        val newId = if (productId.isNotBlank()) {
            "xbox_${productId.lowercase(Locale.ROOT)}"
        } else {
            "xbox_${resolvedSlug.replace("-", "_")}"
        }

        val accent = ACCENT_PALETTE[(resolvedTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]
        val newBookmark = GameBookmark(
            id = newId,
            title = resolvedTitle,
            category = category ?: "Xbox Cloud Game Pass",
            categoryEn = category ?: "Xbox Cloud Game Pass",
            playUrl = canonicalPlayUrl,
            freeToPlay = resolvedTitle.contains("Fortnite", ignoreCase = true) ||
                resolvedTitle.contains("Roblox", ignoreCase = true) ||
                resolvedTitle.contains(" Overwatch", ignoreCase = true) ||
                resolvedTitle.contains("Apex Legends", ignoreCase = true) ||
                resolvedTitle.contains("Call of Duty: Warzone", ignoreCase = true),
            supportsTouch = supportsTouch ?: true,
            accentColorHex = accent,
            badge = "Xbox Cloud",
            badgeEn = "Xbox Cloud",
            rating = "4.8",
            imageUrl = normImage
        )

        val updatedList = currentList + newBookmark
        _games.value = updatedList
        saveSyncedGames(updatedList)

        if (markAsLastPlayed) {
            recordLastPlayed(newBookmark.id)
        }
        if (normImage == null && productId.isNotBlank()) {
            fetchSingleProductMetadataAsync(productId)
        }
        return newBookmark
    }

    /**
     * Ingests a batch of game cards scraped live from the Xbox Cloud web DOM.
     */
    fun ingestDiscoveredWebGames(jsonArrayStr: String) {
        if (jsonArrayStr.isBlank()) return
        ioScope.launch {
            try {
                val arr = JSONArray(jsonArrayStr)
                if (arr.length() == 0) return@launch
                val currentList = _games.value.toMutableList()
                var changed = false

                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawUrl = obj.optString("url", "").trim()
                    val rawTitle = obj.optString("title", "").trim()
                    val rawImg = obj.optString("imageUrl", "").trim()
                    val touch = obj.optBoolean("supportsTouch", false)
                    if (rawUrl.isBlank()) continue

                    val pid = extractProductId(rawUrl)
                    val slug = extractSlug(rawUrl)
                    if (pid.isBlank() && slug.isBlank()) continue

                    val normImg = normalizeImageUrl(rawImg)
                    val existingIdx = currentList.indexOfFirst { item ->
                        val itemPid = extractProductId(item.playUrl)
                        val itemSlug = extractSlug(item.playUrl)
                        (pid.isNotBlank() && itemPid.equals(pid, ignoreCase = true)) ||
                            (slug.length > 2 && itemSlug.equals(slug, ignoreCase = true))
                    }

                    if (existingIdx >= 0) {
                        val existing = currentList[existingIdx]
                        if (existing.imageUrl.isNullOrBlank() && normImg != null) {
                            currentList[existingIdx] = existing.copy(imageUrl = normImg)
                            changed = true
                        }
                    } else {
                        val cleanTitle = rawTitle
                            .replace(Regex("\\s*[•|\\-–—]\\s*Xbox.*", RegexOption.IGNORE_CASE), "")
                            .trim()
                            .ifBlank { formatSlugToTitle(slug) }
                        if (cleanTitle.equals("Xbox Cloud Game", ignoreCase = true)) continue
                        val canonicalUrl = if (pid.isNotBlank()) {
                            "https://www.xbox.com/en-US/play/games/${slug.ifBlank { slugifyTitle(cleanTitle) }}/$pid"
                        } else {
                            rawUrl.replace("/play/launch/", "/play/games/").substringBefore("?")
                        }
                        val id = if (pid.isNotBlank()) "xbox_${pid.lowercase(Locale.ROOT)}" else "xbox_${slug.replace("-", "_")}"
                        if (currentList.any { it.id == id }) continue
                        val accent = ACCENT_PALETTE[(cleanTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]
                        currentList.add(
                            GameBookmark(
                                id = id,
                                title = cleanTitle,
                                category = "Xbox Cloud Game Pass",
                                categoryEn = "Xbox Cloud Game Pass",
                                playUrl = canonicalUrl,
                                freeToPlay = cleanTitle.contains("Fortnite", ignoreCase = true),
                                supportsTouch = touch,
                                accentColorHex = accent,
                                badge = if (touch) "Cảm ứng • Cloud" else "Game Pass",
                                badgeEn = if (touch) "Touch • Cloud" else "Game Pass",
                                rating = "4.8",
                                imageUrl = normImg
                            )
                        )
                        changed = true
                    }
                }

                if (changed) {
                    _games.value = currentList
                    saveSyncedGames(currentList)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Dynamically fetches real Xbox Cloud Gaming titles, categories, ratings, touch support,
     * and official poster cover art from Microsoft's Game Pass & DisplayCatalog APIs.
     */
    fun syncLiveXboxCatalog(force: Boolean = false, maxCatalogGames: Int = 55) {
        if (_isSyncingCatalog.value && !force) return
        ioScope.launch {
            _isSyncingCatalog.value = true
            try {
                val touchIds = fetchSiglProductIds(TOUCH_CATALOG_SIGL_URL, maxCount = 200).toSet()
                val cloudIds = fetchSiglProductIds(CLOUD_CATALOG_SIGL_URL, maxCount = maxCatalogGames)

                val initialProductIds = INITIAL_XBOX_GAMES.mapNotNull {
                    extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }
                }

                val targetProductIds = (initialProductIds + cloudIds)
                    .map { it.uppercase(Locale.ROOT) }
                    .distinct()
                    .take(maxCatalogGames + initialProductIds.size)

                if (targetProductIds.isEmpty()) return@launch

                val fetchedBookmarks = mutableListOf<GameBookmark>()
                targetProductIds.chunked(20).forEach { batch ->
                    val batchResults = fetchDisplayCatalogProducts(batch, touchIds)
                    fetchedBookmarks.addAll(batchResults)
                }

                if (fetchedBookmarks.isNotEmpty()) {
                    val sharedPrefs = prefs
                    val customList = if (sharedPrefs != null) {
                        loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
                    } else {
                        _games.value.filter { it.id.startsWith("custom_") }
                    }
                    val existingSynced = if (sharedPrefs != null) {
                        loadGamesArrayFromPrefs(sharedPrefs, KEY_SYNCED_GAMES_JSON)
                    } else emptyList()

                    val combinedSynced = (fetchedBookmarks + existingSynced)
                        .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }

                    rebuildMergedGames(customList, combinedSynced)
                    saveSyncedGames(_games.value)
                    sharedPrefs?.edit()?.putLong(KEY_LAST_CATALOG_SYNC_MS, System.currentTimeMillis())?.apply()
                }
            } catch (_: Exception) {
            } finally {
                _isSyncingCatalog.value = false
            }
        }
    }

    private fun fetchSingleProductMetadataAsync(productId: String) {
        ioScope.launch {
            try {
                val results = fetchDisplayCatalogProducts(listOf(productId.uppercase(Locale.ROOT)), emptySet())
                val fetched = results.firstOrNull() ?: return@launch
                val current = _games.value
                var changed = false
                val updated = current.map { item ->
                    if (extractProductId(item.playUrl).equals(productId, ignoreCase = true)) {
                        changed = true
                        item.copy(
                            title = if (item.title.startsWith("Xbox ", ignoreCase = true)) fetched.title else item.title,
                            category = if (item.category == "Xbox Cloud Game Pass") fetched.category else item.category,
                            categoryEn = if (item.categoryEn == "Xbox Cloud Game Pass") fetched.categoryEn else item.categoryEn,
                            imageUrl = item.imageUrl ?: fetched.imageUrl,
                            rating = fetched.rating
                        )
                    } else item
                }
                if (changed) {
                    _games.value = updated
                    saveSyncedGames(updated)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun fetchSiglProductIds(siglUrl: String, maxCount: Int): List<String> {
        val conn = (URL(siglUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val ids = ArrayList<String>(maxCount)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim().uppercase(Locale.ROOT)
                if (id.length == 12 && id.all { it.isLetterOrDigit() }) {
                    ids.add(id)
                    if (ids.size >= maxCount) break
                }
            }
            ids
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchDisplayCatalogProducts(
        bigIds: List<String>,
        touchIds: Set<String>
    ): List<GameBookmark> {
        if (bigIds.isEmpty()) return emptyList()
        val joined = bigIds.joinToString(",")
        val apiUrl = "https://displaycatalog.mp.microsoft.com/v7.0/products?bigIds=$joined&market=US&languages=en-us&MS-CV=DGU1mcuYo0WMMp+F.1"
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val products = root.optJSONArray("Products") ?: return emptyList()
            val results = ArrayList<GameBookmark>(products.length())

            for (i in 0 until products.length()) {
                val prod = products.optJSONObject(i) ?: continue
                val productId = prod.optString("ProductId", "").trim().uppercase(Locale.ROOT)
                if (productId.isBlank()) continue

                val locProps = prod.optJSONArray("LocalizedProperties")?.optJSONObject(0) ?: continue
                val rawTitle = locProps.optString("ProductTitle", "").trim()
                if (rawTitle.isBlank()) continue

                val cleanTitle = rawTitle
                    .replace(" (Xbox Series X|S)", "", ignoreCase = true)
                    .replace(" - Standard Edition", "", ignoreCase = true)
                    .replace(" Standard Edition", "", ignoreCase = true)
                    .trim()

                // Pick best cover image: Poster -> BoxArt -> TitledHeroArt -> SuperHeroArt
                val imagesArr = locProps.optJSONArray("Images")
                var posterUri: String? = null
                var boxArtUri: String? = null
                var heroUri: String? = null
                if (imagesArr != null) {
                    for (j in 0 until imagesArr.length()) {
                        val imgObj = imagesArr.optJSONObject(j) ?: continue
                        val purpose = imgObj.optString("ImagePurpose", "")
                        val uri = imgObj.optString("Uri", "")
                        if (uri.isBlank()) continue
                        when (purpose) {
                            "Poster" -> if (posterUri == null) posterUri = uri
                            "BoxArt" -> if (boxArtUri == null) boxArtUri = uri
                            "TitledHeroArt", "SuperHeroArt", "FeaturePromotionalSquareArt" ->
                                if (heroUri == null) heroUri = uri
                        }
                    }
                }
                val bestImageUrl = normalizeImageUrl(posterUri ?: boxArtUri ?: heroUri)

                val props = prod.optJSONObject("Properties")
                val rawCat = props?.optString("Category", "")?.takeIf { it.isNotBlank() }
                    ?: props?.optJSONArray("Categories")?.optString(0, "")
                    ?: "Action & Adventure"
                val (catVi, catEn) = mapCategoryBilingual(rawCat)

                var ratingStr = "4.8"
                val usageData = prod.optJSONArray("MarketProperties")
                    ?.optJSONObject(0)
                    ?.optJSONArray("UsageData")
                if (usageData != null) {
                    for (u in 0 until usageData.length()) {
                        val uObj = usageData.optJSONObject(u) ?: continue
                        val avg = uObj.optDouble("AverageRating", 0.0)
                        if (avg >= 3.5) {
                            ratingStr = String.format(Locale.US, "%.1f", avg.coerceIn(4.2, 4.9))
                            break
                        }
                    }
                }

                val isTouch = productId in touchIds
                val slug = slugifyTitle(cleanTitle)
                val playUrl = "https://www.xbox.com/en-US/play/games/$slug/$productId"
                val accent = ACCENT_PALETTE[(cleanTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]

                results.add(
                    GameBookmark(
                        id = "xbox_${productId.lowercase(Locale.ROOT)}",
                        title = cleanTitle,
                        category = catVi,
                        categoryEn = catEn,
                        playUrl = playUrl,
                        freeToPlay = cleanTitle.contains("Fortnite", ignoreCase = true),
                        supportsTouch = isTouch,
                        accentColorHex = accent,
                        badge = if (isTouch) "Cảm ứng • Cloud" else "Game Pass",
                        badgeEn = if (isTouch) "Touch • Cloud" else "Game Pass",
                        rating = ratingStr,
                        imageUrl = bestImageUrl
                    )
                )
            }
            results
        } finally {
            conn.disconnect()
        }
    }

    private fun mapCategoryBilingual(rawCategory: String): Pair<String, String> {
        val c = rawCategory.lowercase(Locale.ROOT)
        return when {
            c.contains("shooter") -> "Hành động / Bắn súng" to "Shooter"
            c.contains("racing") || c.contains("flying") -> "Đua xe & tốc độ" to "Racing & Flying"
            c.contains("role playing") || c.contains("rpg") -> "Nhập vai (RPG)" to "Role Playing"
            c.contains("sport") -> "Thể thao" to "Sports"
            c.contains("strategy") -> "Chiến thuật" to "Strategy"
            c.contains("simulation") -> "Mô phỏng" to "Simulation"
            c.contains("fighting") -> "Đối kháng" to "Fighting"
            c.contains("family") || c.contains("platformer") -> "Phiêu lưu / Giải trí" to "Platformer & Family"
            else -> "Hành động & Phiêu lưu" to "Action & Adventure"
        }
    }

    private fun loadGamesArrayFromPrefs(sharedPrefs: SharedPreferences, key: String): List<GameBookmark> {
        return try {
            val rawJson = sharedPrefs.getString(key, null) ?: return emptyList()
            val arr = JSONArray(rawJson)
            val list = ArrayList<GameBookmark>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val playUrl = obj.optString("playUrl", "").trim()
                if (title.isBlank() || playUrl.isBlank()) continue
                val cat = obj.optString("category", "Xbox Cloud Game Pass")
                val badge = obj.optString("badge", "").takeIf { it.isNotBlank() }
                list.add(
                    GameBookmark(
                        id = obj.optString("id", "custom_$i"),
                        title = title,
                        category = cat,
                        categoryEn = obj.optString("categoryEn", cat),
                        playUrl = playUrl,
                        freeToPlay = obj.optBoolean("freeToPlay", false),
                        supportsTouch = obj.optBoolean("supportsTouch", true),
                        accentColorHex = obj.optLong("accentColorHex", 0xFF107C10L),
                        badge = badge,
                        badgeEn = obj.optString("badgeEn", badge ?: "").takeIf { it.isNotBlank() },
                        rating = obj.optString("rating", "4.8"),
                        imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomGames(allGames: List<GameBookmark>) {
        val sharedPrefs = prefs ?: return
        val customGames = allGames.filter { it.id.startsWith("custom_") }
        saveGamesListToPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON, customGames)
    }

    private fun saveSyncedGames(allGames: List<GameBookmark>) {
        val sharedPrefs = prefs ?: return
        val syncedGames = allGames.filter { !it.id.startsWith("custom_") && it.drawableResId == null || !it.imageUrl.isNullOrBlank() }
            .take(120)
        saveGamesListToPrefs(sharedPrefs, KEY_SYNCED_GAMES_JSON, syncedGames)
    }

    private fun saveGamesListToPrefs(sharedPrefs: SharedPreferences, key: String, gamesList: List<GameBookmark>) {
        try {
            val arr = JSONArray()
            for (game in gamesList) {
                val obj = JSONObject().apply {
                    put("id", game.id)
                    put("title", game.title)
                    put("category", game.category)
                    put("categoryEn", game.categoryEn)
                    put("playUrl", game.playUrl)
                    put("freeToPlay", game.freeToPlay)
                    put("supportsTouch", game.supportsTouch)
                    put("accentColorHex", game.accentColorHex)
                    put("badge", game.badge ?: "")
                    put("badgeEn", game.badgeEn ?: "")
                    put("rating", game.rating)
                    put("imageUrl", game.imageUrl ?: "")
                }
                arr.put(obj)
            }
            sharedPrefs.edit().putString(key, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun addCustomGame(title: String, url: String) {
        if (title.isBlank() || url.isBlank()) return
        val pid = extractProductId(url)
        val newGame = GameBookmark(
            id = "custom_${System.currentTimeMillis()}",
            title = title.trim(),
            category = "Game tùy chỉnh",
            categoryEn = "Custom Game",
            playUrl = url.trim(),
            freeToPlay = false,
            supportsTouch = true,
            accentColorHex = 0xFF00E676,
            badge = "Tự thêm",
            badgeEn = "Custom",
            rating = "5.0"
        )
        val updated = listOf(newGame) + _games.value
        _games.value = updated
        saveCustomGames(updated)
        if (pid.isNotBlank()) {
            fetchSingleProductMetadataAsync(pid)
        }
    }

    fun removeCustomGame(gameId: String) {
        val updated = _games.value.filterNot { it.id == gameId }
        _games.value = updated
        saveCustomGames(updated)
    }
}



