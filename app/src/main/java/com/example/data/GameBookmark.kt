package com.example.data

import android.content.Context
import android.content.SharedPreferences
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
    POPULAR,
    RECENTLY_ADDED,
    FREE_TO_PLAY,
    TOUCH_FRIENDLY,
    GAME_PASS
}

data class XboxCloudSyncStatus(
    val gamertag: String? = null,
    val avatarUrl: String? = null,
    val subscriptionTier: String? = null,
    val cloudServiceOnline: Boolean = true,
    val cloudLatencyMs: Int? = null,
    val totalCloudCatalogCount: Int = 0,
    val syncedCatalogCount: Int = 0,
    val jumpBackInIds: List<String> = emptyList(),
    val lastSyncTimeMs: Long = 0L
)

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
    val imageUrl: String? = null,
    val heroImageUrl: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val isPopular: Boolean = false,
    val isRecentlyAdded: Boolean = false
) {
    fun localizedCategory(isEn: Boolean): String = if (isEn) {
        if (category == "Game tùy chỉnh") "Custom Game" else categoryEn
    } else category

    fun localizedBadge(isEn: Boolean): String? = if (isEn) {
        if (badge == "Tự thêm") "Custom" else badgeEn
    } else badge
}

// Kho game để trống hoàn toàn trước khi quét; chỉ tải thông tin (metadata) từ Xbox Cloud, không tải ảnh để tiết kiệm dữ liệu
val INITIAL_XBOX_GAMES: List<GameBookmark> = emptyList()
private const val FORTNITE_PRODUCT_ID = "BT5P2X999VH2"

object GameLibraryRepository {
    private const val PREFS_NAME = "cloudplay_custom_games_prefs"
    private const val RECENT_PREFS_NAME = "cloudplay_recent_game_prefs"
    private const val KEY_CUSTOM_GAMES_JSON = "custom_games_json"
    private const val KEY_SYNCED_GAMES_JSON = "synced_xbox_games_json_v4"
    private const val KEY_LAST_CATALOG_SYNC_MS = "last_catalog_sync_ms"
    private const val KEY_FAVORITE_IDS = "favorite_game_ids"
    private const val KEY_LAST_PLAYED_ID = "last_played_game_id"
    private const val KEY_XBOX_GAMERTAG = "xbox_gamertag"
    private const val KEY_XBOX_AVATAR_URL = "xbox_avatar_url"
    private const val KEY_XBOX_SUB_TIER = "xbox_sub_tier"
    private const val KEY_XBOX_TOTAL_CLOUD = "xbox_total_cloud_count"
    private const val KEY_XBOX_JUMP_BACK_IDS = "xbox_jump_back_ids"

    private const val CLOUD_CATALOG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=29a81209-df6f-41fd-a528-2ae6b91f719c&language=en-us&market=US"
    private const val TOUCH_CATALOG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=7d86c25b-b99e-4090-90b3-54a7514b0240&language=en-us&market=US"
    private const val POPULAR_CLOUD_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=f6f1f99f-9b49-4ccd-b3bf-4d9767a77f5e&language=en-us&market=US"
    private const val RECENTLY_ADDED_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=f13cf6b4-57e6-4459-89df-6aec18cf0538&language=en-us&market=US"

    private val ACCENT_PALETTE = longArrayOf(
        0xFF107C10L, 0xFF0074E4L, 0xFFFF007AL, 0xFF00B4D8L,
        0xFF38B000L, 0xFF7209B7L, 0xFFF77F00L, 0xFF2D6A4FL
    )

    private var prefs: SharedPreferences? = null
    private var recentPrefs: SharedPreferences? = null
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cachedAllCloudProductIds: List<String> = emptyList()
    @Volatile
    private var cachedTouchIds: Set<String> = emptySet()
    @Volatile
    private var cachedPopularIds: Set<String> = emptySet()
    @Volatile
    private var cachedRecentIds: Set<String> = emptySet()

    private val _games = MutableStateFlow(INITIAL_XBOX_GAMES)
    val games: StateFlow<List<GameBookmark>> = _games.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _lastPlayedId = MutableStateFlow<String?>(null)
    val lastPlayedId: StateFlow<String?> = _lastPlayedId.asStateFlow()

    private val _isSyncingCatalog = MutableStateFlow(false)
    val isSyncingCatalog: StateFlow<Boolean> = _isSyncingCatalog.asStateFlow()

    private val _isSearchingLiveXbox = MutableStateFlow(false)
    val isSearchingLiveXbox: StateFlow<Boolean> = _isSearchingLiveXbox.asStateFlow()

    private val _xboxStatus = MutableStateFlow(XboxCloudSyncStatus())
    val xboxStatus: StateFlow<XboxCloudSyncStatus> = _xboxStatus.asStateFlow()

    /**
     * Initializes persistent storage for custom user-added games, auto-synced Xbox Cloud catalog games,
     * favorites, Xbox profile status, and last played game via SharedPreferences.
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
            val savedGamertag = sharedPrefs.getString(KEY_XBOX_GAMERTAG, null)?.takeIf { it.isNotBlank() }
            val savedTier = sharedPrefs.getString(KEY_XBOX_SUB_TIER, null)?.takeIf { it.isNotBlank() }
            val savedTotalCloud = sharedPrefs.getInt(KEY_XBOX_TOTAL_CLOUD, 0)
            val savedJumpBackRaw = sharedPrefs.getString(KEY_XBOX_JUMP_BACK_IDS, "") ?: ""
            val savedJumpBack = savedJumpBackRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }

            val savedAvatar = sharedPrefs.getString(KEY_XBOX_AVATAR_URL, null)?.takeIf { it.isNotBlank() }

            _xboxStatus.value = XboxCloudSyncStatus(
                gamertag = savedGamertag,
                avatarUrl = savedAvatar,
                subscriptionTier = savedTier,
                cloudServiceOnline = true,
                totalCloudCatalogCount = savedTotalCloud.coerceAtLeast(_games.value.size),
                syncedCatalogCount = _games.value.size,
                jumpBackInIds = savedJumpBack,
                lastSyncTimeMs = lastSyncMs
            )

            val now = System.currentTimeMillis()
            val missingCoverArt = syncedList.any { it.imageUrl.isNullOrBlank() }
            val needsAutoFullScan = syncedList.isEmpty() ||
                missingCoverArt ||
                (savedTotalCloud > 0 && syncedList.size < (savedTotalCloud - 20)) ||
                syncedList.size < 120 ||
                (now - lastSyncMs) > 6 * 3600 * 1000L
            if (needsAutoFullScan) {
                syncLiveXboxCatalog(force = missingCoverArt)
            }
        }
    }

    private fun rebuildMergedGames(
        customList: List<GameBookmark>,
        syncedList: List<GameBookmark>
    ) {
        val existingIds = HashSet<String>()
        val existingProductIds = HashSet<String>()
        val merged = ArrayList<GameBookmark>(customList.size + syncedList.size)

        fun addUnique(item: GameBookmark) {
            val idKey = item.id.lowercase(Locale.ROOT)
            val pid = extractProductId(item.playUrl)
            val isFortnite = idKey == "fortnite" || pid.equals(FORTNITE_PRODUCT_ID, ignoreCase = true)
            if (existingIds.contains(idKey)) return
            if (pid.isNotBlank() && existingProductIds.contains(pid)) return
            existingIds.add(idKey)
            if (pid.isNotBlank()) existingProductIds.add(pid)
            val finalItem = if (isFortnite) {
                item.copy(id = "fortnite", drawableResId = null)
            } else {
                item.copy(drawableResId = null)
            }
            merged.add(finalItem)
        }

        customList.forEach { addUnique(it) }
        syncedList.forEach { addUnique(it) }

        _games.value = merged
        _xboxStatus.value = _xboxStatus.value.copy(
            syncedCatalogCount = merged.size,
            totalCloudCatalogCount = _xboxStatus.value.totalCloudCatalogCount.coerceAtLeast(merged.size)
        )
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
     * Updates the user's live Xbox Live profile detected from the signed-in Xbox Cloud WebView.
     */
    fun onXboxProfileSynced(gamertag: String?, avatarUrl: String?, subscriptionTier: String?) {
        val cleanTag = gamertag?.trim()?.takeIf {
            it.isNotBlank() &&
                !it.equals("Sign in", ignoreCase = true) &&
                !it.equals("Đăng nhập", ignoreCase = true) &&
                !it.equals("Xbox", ignoreCase = true) &&
                it.length in 2..28
        } ?: return
        val cleanAvatar = normalizeImageUrl(avatarUrl, widthPx = 120, quality = 70)
        val cleanTier = subscriptionTier?.trim()?.takeIf { it.isNotBlank() } ?: "Xbox Cloud Gaming"
        val current = _xboxStatus.value
        if (current.gamertag == cleanTag && current.avatarUrl == cleanAvatar && current.subscriptionTier == cleanTier) return

        val updated = current.copy(
            gamertag = cleanTag,
            avatarUrl = cleanAvatar ?: current.avatarUrl,
            subscriptionTier = cleanTier
        )
        _xboxStatus.value = updated
        prefs?.edit()
            ?.putString(KEY_XBOX_GAMERTAG, updated.gamertag)
            ?.putString(KEY_XBOX_AVATAR_URL, updated.avatarUrl ?: "")
            ?.putString(KEY_XBOX_SUB_TIER, updated.subscriptionTier ?: "")
            ?.apply()
    }

    /**
     * Syncs the user's "Jump back in" (Recently played on Xbox Cloud) carousel detected from xbox.com/play.
     */
    fun onXboxJumpBackInSynced(jsonArrayStr: String) {
        if (jsonArrayStr.isBlank()) return
        ioScope.launch {
            try {
                val arr = JSONArray(jsonArrayStr)
                if (arr.length() == 0) return@launch
                val jumpIds = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawUrl = obj.optString("url", "").trim()
                    val rawTitle = obj.optString("title", "").trim()
                    val rawImg = obj.optString("imageUrl", "").trim().takeIf { it.isNotBlank() }
                    if (rawUrl.isBlank()) continue
                    val bookmark = onWebGamePlayedOrDiscovered(
                        rawTitle = rawTitle,
                        rawUrl = rawUrl,
                        imageUrl = rawImg,
                        markAsLastPlayed = (i == 0 && _lastPlayedId.value == null)
                    )
                    if (bookmark != null && bookmark.id !in jumpIds) {
                        jumpIds.add(bookmark.id)
                    }
                }
                if (jumpIds.isNotEmpty()) {
                    val mergedJumpIds = (jumpIds + _xboxStatus.value.jumpBackInIds).distinct().take(10)
                    _xboxStatus.value = _xboxStatus.value.copy(jumpBackInIds = mergedJumpIds)
                    prefs?.edit()?.putString(KEY_XBOX_JUMP_BACK_IDS, mergedJumpIds.joinToString(","))?.apply()
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Automatically detects any game opened or launched inside the Xbox Cloud WebView,
     * matches or adds its metadata to GameLibraryRepository, and records it as the last played game
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

        val validImg = normalizeImageUrl(imageUrl, widthPx = 260, quality = 70)
        val productId = extractProductId(cleanUrl)
        val slug = extractSlug(cleanUrl)

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
            if (validImg != null && matched.imageUrl.isNullOrBlank()) {
                updatedItem = updatedItem.copy(imageUrl = validImg)
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
            if ((updatedItem.imageUrl.isNullOrBlank() || updatedItem.description.isNullOrBlank()) && productId.isNotBlank()) {
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
            freeToPlay = isFreeToPlayTitle(resolvedTitle),
            supportsTouch = supportsTouch ?: true,
            accentColorHex = accent,
            badge = "Xbox Cloud",
            badgeEn = "Xbox Cloud",
            rating = "4.8",
            imageUrl = validImg,
            heroImageUrl = validImg
        )

        val updatedList = currentList + newBookmark
        _games.value = updatedList
        saveSyncedGames(updatedList)

        if (markAsLastPlayed) {
            recordLastPlayed(newBookmark.id)
        }
        if (productId.isNotBlank()) {
            fetchSingleProductMetadataAsync(productId)
        }
        return newBookmark
    }

    private fun isFreeToPlayTitle(title: String): Boolean {
        return title.contains("Fortnite", ignoreCase = true) ||
            title.contains("Roblox", ignoreCase = true) ||
            title.contains("Overwatch", ignoreCase = true) ||
            title.contains("Apex Legends", ignoreCase = true) ||
            title.contains("Warzone", ignoreCase = true) ||
            title.contains("Halo Infinite", ignoreCase = true) ||
            title.contains("Rocket League", ignoreCase = true) ||
            title.contains("Destiny 2", ignoreCase = true) ||
            title.contains("The Finals", ignoreCase = true) ||
            title.contains("Valorant", ignoreCase = true) ||
            title.contains("Genshin", ignoreCase = true)
    }

    /**
     * Ingests a batch of game cards scraped live from the Xbox Cloud web DOM (metadata only, no image downloads).
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
                    val rawImg = normalizeImageUrl(obj.optString("imageUrl", ""), widthPx = 260, quality = 70)
                    val touch = obj.optBoolean("supportsTouch", false)
                    if (rawUrl.isBlank()) continue

                    val pid = extractProductId(rawUrl)
                    val slug = extractSlug(rawUrl)
                    if (pid.isBlank() && slug.isBlank()) continue

                    val existingIdx = currentList.indexOfFirst { item ->
                        val itemPid = extractProductId(item.playUrl)
                        val itemSlug = extractSlug(item.playUrl)
                        (pid.isNotBlank() && itemPid.equals(pid, ignoreCase = true)) ||
                            (slug.length > 2 && itemSlug.equals(slug, ignoreCase = true))
                    }

                    if (existingIdx >= 0) {
                        val existing = currentList[existingIdx]
                        if (rawImg != null && existing.imageUrl.isNullOrBlank()) {
                            currentList[existingIdx] = existing.copy(imageUrl = rawImg)
                            changed = true
                        }
                    } else if (rawImg != null) {
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
                                freeToPlay = isFreeToPlayTitle(cleanTitle),
                                supportsTouch = touch,
                                accentColorHex = accent,
                                badge = if (touch) "Cảm ứng • Cloud" else "Game Pass",
                                badgeEn = if (touch) "Touch • Cloud" else "Game Pass",
                                rating = "4.8",
                                imageUrl = rawImg,
                                heroImageUrl = rawImg
                            )
                        )
                        changed = true
                    }
                }

                if (changed) {
                    _games.value = currentList
                    saveSyncedGames(currentList)
                    _xboxStatus.value = _xboxStatus.value.copy(syncedCatalogCount = currentList.size)
                }
            } catch (_: Exception) {
            }
        }
    }

    @Volatile
    private var isStreamActive: Boolean = false

    fun setStreamActive(active: Boolean) {
        isStreamActive = active
    }

    /**
     * Tự động quét toàn bộ kho game Xbox Cloud theo từng lô (streaming batches)
     * và chỉ lấy thông tin game (tên, thể loại, nhà phát hành, mô tả, link chơi),
     * hoàn toàn không tải ảnh bìa để tiết kiệm dữ liệu mạng.
     * Tự động tạm dừng ngay khi người dùng đang trong luồng chơi game để nhường 100% băng thông cho 4G/Wi-Fi.
     */
    fun syncLiveXboxCatalog(force: Boolean = false, maxCatalogGames: Int = 500) {
        if ((_isSyncingCatalog.value && !force) || isStreamActive) return
        ioScope.launch {
            _isSyncingCatalog.value = true
            val startPingMs = System.currentTimeMillis()
            try {
                val allCloudIds = fetchSiglProductIds(CLOUD_CATALOG_SIGL_URL, maxCount = 500)
                val catalogRtt = (System.currentTimeMillis() - startPingMs).toInt().coerceIn(15, 2500)
                val touchIds = fetchSiglProductIds(TOUCH_CATALOG_SIGL_URL, maxCount = 300).toSet()
                val popularIds = fetchSiglProductIds(POPULAR_CLOUD_SIGL_URL, maxCount = 100).toSet()
                val recentIds = fetchSiglProductIds(RECENTLY_ADDED_SIGL_URL, maxCount = 80).toSet()

                if (allCloudIds.isNotEmpty()) {
                    cachedAllCloudProductIds = allCloudIds
                }
                if (touchIds.isNotEmpty()) cachedTouchIds = touchIds
                if (popularIds.isNotEmpty()) cachedPopularIds = popularIds
                if (recentIds.isNotEmpty()) cachedRecentIds = recentIds

                val prioritizedCloudIds = (listOf(FORTNITE_PRODUCT_ID) + popularIds.toList() + recentIds.toList() + allCloudIds)
                    .map { it.uppercase(Locale.ROOT) }
                    .distinct()
                    .take(maxCatalogGames)

                if (prioritizedCloudIds.isEmpty()) {
                    _xboxStatus.value = _xboxStatus.value.copy(cloudServiceOnline = false)
                    return@launch
                }

                val totalCloud = prioritizedCloudIds.size.coerceAtLeast(allCloudIds.size)
                _xboxStatus.value = _xboxStatus.value.copy(
                    cloudServiceOnline = true,
                    cloudLatencyMs = catalogRtt,
                    totalCloudCatalogCount = totalCloud
                )

                val sharedPrefs = prefs
                val customList = if (sharedPrefs != null) {
                    loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
                } else {
                    _games.value.filter { it.id.startsWith("custom_") }
                }
                val existingSynced = if (sharedPrefs != null) {
                    loadGamesArrayFromPrefs(sharedPrefs, KEY_SYNCED_GAMES_JSON)
                } else emptyList()

                val existingWithImages = existingSynced.filter { !it.imageUrl.isNullOrBlank() }
                val existingPids = existingWithImages
                    .mapNotNull { extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }?.uppercase(Locale.ROOT) }
                    .toSet()

                val idsToFetch = if (force) {
                    prioritizedCloudIds
                } else {
                    prioritizedCloudIds.filter { it !in existingPids }
                }

                val runningSynced = existingWithImages.toMutableList()
                for (batch in idsToFetch.chunked(40)) {
                    if (isStreamActive) break
                    val batchResults = fetchDisplayCatalogProducts(
                        bigIds = batch,
                        touchIds = cachedTouchIds,
                        popularIds = cachedPopularIds,
                        recentIds = cachedRecentIds
                    )
                    if (batchResults.isNotEmpty()) {
                        runningSynced.addAll(batchResults)
                        val deduped = runningSynced
                            .filter { !it.imageUrl.isNullOrBlank() }
                            .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                        rebuildMergedGames(customList, deduped)
                        _xboxStatus.value = _xboxStatus.value.copy(
                            cloudServiceOnline = true,
                            cloudLatencyMs = catalogRtt,
                            totalCloudCatalogCount = totalCloud.coerceAtLeast(_games.value.size),
                            syncedCatalogCount = _games.value.size
                        )
                    }
                }

                saveSyncedGames(_games.value)
                val nowMs = System.currentTimeMillis()
                _xboxStatus.value = _xboxStatus.value.copy(
                    cloudServiceOnline = true,
                    cloudLatencyMs = catalogRtt,
                    totalCloudCatalogCount = totalCloud.coerceAtLeast(_games.value.size),
                    syncedCatalogCount = _games.value.size,
                    lastSyncTimeMs = nowMs
                )
                sharedPrefs?.edit()
                    ?.putLong(KEY_LAST_CATALOG_SYNC_MS, nowMs)
                    ?.putInt(KEY_XBOX_TOTAL_CLOUD, totalCloud.coerceAtLeast(_games.value.size))
                    ?.apply()
            } catch (_: Exception) {
                _xboxStatus.value = _xboxStatus.value.copy(cloudServiceOnline = false)
            } finally {
                _isSyncingCatalog.value = false
            }
        }
    }

    /**
     * Loads the next batch of unsynced Xbox Cloud games from Microsoft's full Cloud SIGL feed.
     */
    fun loadMoreXboxCatalogGames(batchSize: Int = 40) {
        if (_isSyncingCatalog.value) return
        ioScope.launch {
            _isSyncingCatalog.value = true
            try {
                if (cachedAllCloudProductIds.isEmpty()) {
                    cachedAllCloudProductIds = fetchSiglProductIds(CLOUD_CATALOG_SIGL_URL, maxCount = 450)
                }
                if (cachedTouchIds.isEmpty()) {
                    cachedTouchIds = fetchSiglProductIds(TOUCH_CATALOG_SIGL_URL, maxCount = 250).toSet()
                }
                val existingPids = _games.value
                    .mapNotNull { extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }?.uppercase(Locale.ROOT) }
                    .toSet()

                val nextBatchIds = cachedAllCloudProductIds
                    .filter { it.uppercase(Locale.ROOT) !in existingPids }
                    .take(batchSize)

                if (nextBatchIds.isEmpty()) {
                    syncLiveXboxCatalog(force = true, maxCatalogGames = (_games.value.size + batchSize).coerceAtMost(240))
                    return@launch
                }

                val newBookmarks = mutableListOf<GameBookmark>()
                nextBatchIds.chunked(20).forEach { chunk ->
                    newBookmarks.addAll(
                        fetchDisplayCatalogProducts(
                            bigIds = chunk,
                            touchIds = cachedTouchIds,
                            popularIds = cachedPopularIds,
                            recentIds = cachedRecentIds
                        )
                    )
                }

                if (newBookmarks.isNotEmpty()) {
                    val current = _games.value
                    val merged = (current + newBookmarks).distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    _games.value = merged
                    saveSyncedGames(merged)
                    _xboxStatus.value = _xboxStatus.value.copy(
                        syncedCatalogCount = merged.size,
                        totalCloudCatalogCount = cachedAllCloudProductIds.size.coerceAtLeast(merged.size)
                    )
                }
            } catch (_: Exception) {
            } finally {
                _isSyncingCatalog.value = false
            }
        }
    }

    /**
     * Searches Microsoft's live DisplayCatalog API by keyword, resolves lightweight game metadata (no images),
     * adds discovered Xbox games to the local library, and returns matching results via callback.
     */
    fun searchMicrosoftXboxCatalog(
        query: String,
        onResult: (List<GameBookmark>) -> Unit = {}
    ) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            onResult(emptyList())
            return
        }
        ioScope.launch {
            _isSearchingLiveXbox.value = true
            try {
                val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
                val suggestUrl =
                    "https://displaycatalog.mp.microsoft.com/v7.0/productFamilies/autosuggest?market=US&languages=en-us&productFamilyNames=Games&query=$encoded"
                val conn = (URL(suggestUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                val discoveredIds = mutableListOf<String>()
                try {
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(body)
                        val resultsArr = root.optJSONArray("Results")
                        if (resultsArr != null) {
                            for (i in 0 until resultsArr.length()) {
                                val group = resultsArr.optJSONObject(i) ?: continue
                                val prods = group.optJSONArray("Products") ?: continue
                                for (j in 0 until prods.length()) {
                                    val p = prods.optJSONObject(j) ?: continue
                                    val pid = p.optString("ProductId", "").trim().uppercase(Locale.ROOT)
                                    if (pid.length == 12 && pid.all { it.isLetterOrDigit() } && pid !in discoveredIds) {
                                        discoveredIds.add(pid)
                                        if (discoveredIds.size >= 16) break
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }

                if (discoveredIds.isNotEmpty()) {
                    val fetched = fetchDisplayCatalogProducts(
                        bigIds = discoveredIds,
                        touchIds = cachedTouchIds,
                        popularIds = cachedPopularIds,
                        recentIds = cachedRecentIds
                    )
                    if (fetched.isNotEmpty()) {
                        val current = _games.value
                        val merged = (current + fetched).distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                        _games.value = merged
                        saveSyncedGames(merged)
                        _xboxStatus.value = _xboxStatus.value.copy(syncedCatalogCount = merged.size)
                        onResult(fetched)
                        return@launch
                    }
                }
                onResult(emptyList())
            } catch (_: Exception) {
                onResult(emptyList())
            } finally {
                _isSearchingLiveXbox.value = false
            }
        }
    }

    private fun fetchSingleProductMetadataAsync(productId: String) {
        ioScope.launch {
            try {
                val results = fetchDisplayCatalogProducts(
                    bigIds = listOf(productId.uppercase(Locale.ROOT)),
                    touchIds = cachedTouchIds,
                    popularIds = cachedPopularIds,
                    recentIds = cachedRecentIds
                )
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
                            heroImageUrl = item.heroImageUrl ?: fetched.heroImageUrl,
                            description = item.description ?: fetched.description,
                            publisher = item.publisher ?: fetched.publisher,
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
        touchIds: Set<String>,
        popularIds: Set<String> = emptySet(),
        recentIds: Set<String> = emptySet()
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

                val shortDesc = locProps.optString("ShortDescription", "").trim()
                    .ifBlank { locProps.optString("ProductDescription", "").trim() }
                    .replace(Regex("\\s+"), " ")
                    .take(280)
                    .takeIf { it.isNotBlank() }

                val publisherName = locProps.optString("PublisherName", "").trim()
                    .ifBlank { locProps.optString("DeveloperName", "").trim() }
                    .takeIf { it.isNotBlank() }

                val imagesArr = locProps.optJSONArray("Images")
                var posterUrl: String? = null
                var superHeroUrl: String? = null
                var boxArtUrl: String? = null
                var fallbackImgUrl: String? = null

                if (imagesArr != null) {
                    for (j in 0 until imagesArr.length()) {
                        val imgObj = imagesArr.optJSONObject(j) ?: continue
                        val purpose = imgObj.optString("ImagePurpose", "")
                        val uri = normalizeImageUrl(
                            imgObj.optString("Uri", ""),
                            widthPx = if (purpose.equals("SuperHeroArt", ignoreCase = true)) 520 else 260,
                            quality = 70
                        ) ?: continue
                        when (purpose) {
                            "Poster" -> if (posterUrl == null) posterUrl = uri
                            "BoxArt" -> if (boxArtUrl == null) boxArtUrl = uri
                            "SuperHeroArt", "TitledHeroArt" -> if (superHeroUrl == null) superHeroUrl = uri
                            "Tile", "FeaturePromotionalSquareArt" -> if (fallbackImgUrl == null) fallbackImgUrl = uri
                        }
                    }
                }

                val bestCoverUrl = posterUrl ?: boxArtUrl ?: superHeroUrl ?: fallbackImgUrl
                val bestHeroUrl = superHeroUrl ?: posterUrl ?: boxArtUrl ?: fallbackImgUrl
                if (bestCoverUrl.isNullOrBlank()) continue

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

                val isFortnite = productId.equals(FORTNITE_PRODUCT_ID, ignoreCase = true) ||
                    cleanTitle.equals("Fortnite", ignoreCase = true)
                val isTouch = productId in touchIds || isFortnite
                val isPop = productId in popularIds || isFortnite
                val isRecent = productId in recentIds
                val slug = if (isFortnite) "fortnite" else slugifyTitle(cleanTitle)
                val playUrl = "https://www.xbox.com/en-US/play/games/$slug/$productId"
                val accent = ACCENT_PALETTE[(cleanTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]

                val badgeVi = when {
                    isFortnite -> "Hỗ Trợ 120FPS • Free 100%"
                    isRecent -> "Mới lên kệ • Cloud"
                    isPop -> "Top Hot • Xbox"
                    isTouch -> "Cảm ứng • Cloud"
                    else -> "Game Pass"
                }
                val badgeEn = when {
                    isFortnite -> "120FPS Ready • 100% Free"
                    isRecent -> "New on Cloud"
                    isPop -> "Popular • Xbox"
                    isTouch -> "Touch • Cloud"
                    else -> "Game Pass"
                }

                results.add(
                    GameBookmark(
                        id = if (isFortnite) "fortnite" else "xbox_${productId.lowercase(Locale.ROOT)}",
                        title = cleanTitle,
                        category = catVi,
                        categoryEn = catEn,
                        playUrl = playUrl,
                        freeToPlay = isFreeToPlayTitle(cleanTitle),
                        supportsTouch = isTouch,
                        accentColorHex = accent,
                        badge = badgeVi,
                        badgeEn = badgeEn,
                        drawableResId = null,
                        rating = ratingStr,
                        imageUrl = bestCoverUrl,
                        heroImageUrl = bestHeroUrl,
                        description = shortDesc,
                        publisher = publisherName,
                        isPopular = isPop,
                        isRecentlyAdded = isRecent
                    )
                )
            }
            results
        } finally {
            conn.disconnect()
        }
    }

    private fun normalizeImageUrl(raw: String?, widthPx: Int = 260, quality: Int = 70): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val httpsUrl = when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.removePrefix("http://")
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> return null
        }
        return if (httpsUrl.contains("store-images.s-microsoft.com", ignoreCase = true) && !httpsUrl.contains("?")) {
            "$httpsUrl?w=$widthPx&q=$quality"
        } else {
            httpsUrl
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
                val id = obj.optString("id", "custom_$i")
                val isFortnite = id == "fortnite" || extractProductId(playUrl).equals(FORTNITE_PRODUCT_ID, ignoreCase = true)
                val cat = obj.optString("category", "Xbox Cloud Game Pass")
                val badge = obj.optString("badge", "").takeIf { it.isNotBlank() }
                val imgUrl = normalizeImageUrl(obj.optString("imageUrl", ""), widthPx = 260, quality = 70)
                val heroUrl = normalizeImageUrl(obj.optString("heroImageUrl", ""), widthPx = 520, quality = 70) ?: imgUrl
                list.add(
                    GameBookmark(
                        id = if (isFortnite) "fortnite" else id,
                        title = title,
                        category = cat,
                        categoryEn = obj.optString("categoryEn", cat),
                        playUrl = playUrl,
                        freeToPlay = obj.optBoolean("freeToPlay", false),
                        supportsTouch = obj.optBoolean("supportsTouch", true),
                        accentColorHex = obj.optLong("accentColorHex", 0xFF107C10L),
                        badge = badge,
                        badgeEn = obj.optString("badgeEn", badge ?: "").takeIf { it.isNotBlank() },
                        drawableResId = null,
                        rating = obj.optString("rating", "4.8"),
                        imageUrl = imgUrl,
                        heroImageUrl = heroUrl,
                        description = obj.optString("description", "").takeIf { it.isNotBlank() },
                        publisher = obj.optString("publisher", "").takeIf { it.isNotBlank() },
                        isPopular = obj.optBoolean("isPopular", false),
                        isRecentlyAdded = obj.optBoolean("isRecentlyAdded", false)
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
        val syncedGames = allGames
            .filter { !it.id.startsWith("custom_") && !it.imageUrl.isNullOrBlank() }
            .take(550)
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
                    put("heroImageUrl", game.heroImageUrl ?: "")
                    put("description", game.description ?: "")
                    put("publisher", game.publisher ?: "")
                    put("isPopular", game.isPopular)
                    put("isRecentlyAdded", game.isRecentlyAdded)
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



