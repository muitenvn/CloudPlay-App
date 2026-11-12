package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    CONTINUE_PLAYING,
    FAVORITES,
    POPULAR,
    RECENTLY_ADDED,
    FREE_TO_PLAY,
    TOUCH_FRIENDLY,
    GAME_PASS
}

@androidx.compose.runtime.Immutable
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

@androidx.compose.runtime.Immutable
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
    val monogram: String = run {
        var firstChar: Char? = null
        var secondChar: Char? = null
        var inWord = false
        for (i in 0 until title.length) {
            val c = title[i]
            if (c.isLetterOrDigit()) {
                if (!inWord) {
                    inWord = true
                    if (firstChar == null) {
                        firstChar = c.uppercaseChar()
                    } else if (secondChar == null) {
                        secondChar = c.uppercaseChar()
                        break
                    }
                }
            } else if (c.isWhitespace()) {
                inWord = false
            }
        }
        when {
            firstChar != null && secondChar != null -> "$firstChar$secondChar"
            firstChar != null -> "$firstChar"
            else -> "XB"
        }
    }

    val secondaryColorHex: Long = when ((title.hashCode() and 0x7FFFFFFF) % 4) {
        0 -> 0xFF00E5FFL
        1 -> 0xFF107C10L
        2 -> 0xFF7C4DFFL
        else -> 0xFF00B4D8L
    }

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
    private const val SYNCED_GAMES_CACHE_FILE_NAME = "xbox_synced_games_v4.json"
    private const val KEY_LAST_CATALOG_SYNC_MS = "last_catalog_sync_ms"
    private const val KEY_FAVORITE_IDS = "favorite_game_ids"
    private const val KEY_LAST_PLAYED_ID = "last_played_game_id"
    private const val KEY_PLAYED_GAME_IDS_LIST = "played_game_ids_ordered_v1"
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
    private val SLUG_NON_ALNUM_REGEX = Regex("[^a-z0-9]+")
    private val XBOX_CLOUD_SUFFIX_REGEX = Regex("\\s*[•|\\-–—]\\s*Xbox Cloud Gaming.*", RegexOption.IGNORE_CASE)
    private val XBOX_PIPE_SUFFIX_REGEX = Regex("\\s*\\|?\\s*Xbox.*", RegexOption.IGNORE_CASE)
    private val XBOX_BULLET_SUFFIX_REGEX = Regex("\\s*[•|\\-–—]\\s*Xbox.*", RegexOption.IGNORE_CASE)
    private val WHITESPACE_COLLAPSE_REGEX = Regex("\\s+")

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var recentPrefs: SharedPreferences? = null
    @Volatile
    private var syncedGamesCacheFile: java.io.File? = null
    private val cacheFileLock = Any()
    @Volatile
    private var userModifiedFavoritesEarly = false
    @Volatile
    private var userModifiedLastPlayedEarly = false
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

    private val _playedGameIds = MutableStateFlow<List<String>>(emptyList())
    val playedGameIds: StateFlow<List<String>> = _playedGameIds.asStateFlow()

    private val _isSyncingCatalog = MutableStateFlow(false)
    val isSyncingCatalog: StateFlow<Boolean> = _isSyncingCatalog.asStateFlow()

    private val _isLoadingInitialCatalog = MutableStateFlow(false)
    val isLoadingInitialCatalog: StateFlow<Boolean> = _isLoadingInitialCatalog.asStateFlow()

    private val _isSearchingLiveXbox = MutableStateFlow(false)
    val isSearchingLiveXbox: StateFlow<Boolean> = _isSearchingLiveXbox.asStateFlow()

    private val _xboxStatus = MutableStateFlow(XboxCloudSyncStatus())
    val xboxStatus: StateFlow<XboxCloudSyncStatus> = _xboxStatus.asStateFlow()

    /**
     * Initializes persistent storage for custom user-added games, auto-synced Xbox Cloud catalog games,
     * favorites, Xbox profile status, and last played game via SharedPreferences.
     */
    @Volatile
    private var initStarted = false
    @Volatile
    private var initialDiskLoadCompleted = false

    fun init(context: Context) {
        if (!initStarted) {
            initStarted = true
            val appCtx = context.applicationContext
            syncedGamesCacheFile = java.io.File(appCtx.filesDir, SYNCED_GAMES_CACHE_FILE_NAME)
            // Gán tham chiếu SharedPreferences ngay lập tức (<0.2ms, không chờ nạp XML) để các hàm gọi sớm
            // như toggleFavorite / recordLastPlayed luôn có đối tượng prefs hợp lệ.
            val sharedPrefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val legacyRecentPrefs = appCtx.getSharedPreferences(RECENT_PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sharedPrefs
            recentPrefs = legacyRecentPrefs
            _isLoadingInitialCatalog.value = true
            ioScope.launch {
                // Khởi tạo sẵn Coil ImageLoader trên luồng IO để lần gọi AsyncImage đầu tiên trên UI không bị chặn
                try {
                    coil.Coil.imageLoader(appCtx)
                } catch (_: Throwable) {
                }

                val t0 = android.os.SystemClock.elapsedRealtime()
                // Chờ SharedPreferencesImpl nạp xong file XML trên luồng IO thay vì chặn luồng chính
                val savedFavs = sharedPrefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
                val tAfterPrefsAwait = android.os.SystemClock.elapsedRealtime()
                if (!userModifiedFavoritesEarly) {
                    _favoriteIds.value = savedFavs.toSet()
                }

                val savedLast = sharedPrefs.getString(KEY_LAST_PLAYED_ID, null)
                    ?: legacyRecentPrefs.getString(KEY_LAST_PLAYED_ID, null)
                val savedPlayedRaw = sharedPrefs.getString(KEY_PLAYED_GAME_IDS_LIST, "") ?: ""
                val savedPlayedList = savedPlayedRaw
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (!userModifiedLastPlayedEarly) {
                    if (_lastPlayedId.value == null) {
                        _lastPlayedId.value = savedPlayedList.firstOrNull() ?: savedLast
                    }
                    if (_playedGameIds.value.isEmpty()) {
                        _playedGameIds.value = if (savedPlayedList.isNotEmpty()) {
                            savedPlayedList
                        } else if (!savedLast.isNullOrBlank()) {
                            listOf(savedLast)
                        } else {
                            emptyList()
                        }
                    }
                }

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

                val customList: List<GameBookmark>
                val syncedList: List<GameBookmark>
                try {
                    val tParseStart = android.os.SystemClock.elapsedRealtime()
                    val earlyDiscovered = _games.value.filter { !it.id.startsWith("custom_") }
                    customList = loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
                    val diskSynced = loadSyncedGamesFromFileOrMigratePrefs(sharedPrefs)
                    syncedList = (diskSynced + earlyDiscovered)
                        .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    val tParseEnd = android.os.SystemClock.elapsedRealtime()
                    rebuildMergedGames(customList, syncedList)
                    initialDiskLoadCompleted = true
                    val tMergeEnd = android.os.SystemClock.elapsedRealtime()
                    PerformanceMonitor.recordInitMilestone(
                        "GameLibraryRepository (IO): xmlLoad=${tAfterPrefsAwait - t0}ms, jsonParse=${tParseEnd - tParseStart}ms, merge=${tMergeEnd - tParseEnd}ms (tổng=${_games.value.size} game)"
                    )
                } finally {
                    initialDiskLoadCompleted = true
                    _isLoadingInitialCatalog.value = false
                }

                val now = System.currentTimeMillis()
                val missingCoverArt = syncedList.any { it.imageUrl.isNullOrBlank() }
                val targetMinSynced = (savedTotalCloud.coerceAtMost(550) - 20).coerceAtLeast(0)
                val isCatalogTooSmall = syncedList.size < 120
                val needsAutoFullScan = syncedList.isEmpty() ||
                    missingCoverArt ||
                    isCatalogTooSmall ||
                    (targetMinSynced > 0 && syncedList.size < targetMinSynced) ||
                    (now - lastSyncMs) > 6 * 3600 * 1000L
                if (needsAutoFullScan) {
                    kotlinx.coroutines.delay(if (isCatalogTooSmall) 300L else 3_500L)
                    if (!isStreamActive && !com.example.vpn.VpnManager.isStreamActive()) {
                        syncLiveXboxCatalog(
                            force = missingCoverArt || isCatalogTooSmall,
                            maxCatalogGames = 600
                        )
                    }
                }
            }
        }
    }

    private fun rebuildMergedGames(
        customList: List<GameBookmark>,
        syncedList: List<GameBookmark>
    ) {
        val existingIds = HashSet<String>((customList.size + syncedList.size) * 2)
        val existingProductIds = HashSet<String>((customList.size + syncedList.size) * 2)
        val merged = ArrayList<GameBookmark>(customList.size + syncedList.size)

        fun addUnique(item: GameBookmark) {
            val idKey = item.id.lowercase(Locale.ROOT)
            val pid = extractProductId(item.playUrl)
            val isFortnite = idKey == "fortnite" || pid.equals(FORTNITE_PRODUCT_ID, ignoreCase = true)
            val trulyFree = isFreeToPlayTitle(item.title, pid, item.playUrl)
            if (existingIds.contains(idKey)) return
            if (pid.isNotBlank() && existingProductIds.contains(pid)) return
            existingIds.add(idKey)
            if (pid.isNotBlank()) existingProductIds.add(pid)
            val finalItem = if (isFortnite && (item.id != "fortnite" || item.drawableResId != null || !item.freeToPlay)) {
                item.copy(id = "fortnite", drawableResId = null, freeToPlay = true)
            } else if (item.drawableResId != null || item.freeToPlay != trulyFree) {
                item.copy(drawableResId = null, freeToPlay = trulyFree)
            } else {
                item
            }
            merged.add(finalItem)
        }

        customList.forEach { addUnique(it) }
        syncedList.forEach { addUnique(it) }

        // Đảm bảo Fortnite (game miễn phí 100% thực sự trên Xbox Cloud không cần Game Pass) luôn có mặt trong danh mục
        if (!existingIds.contains("fortnite") && !existingProductIds.contains(FORTNITE_PRODUCT_ID)) {
            merged.add(
                0,
                GameBookmark(
                    id = "fortnite",
                    title = "Fortnite",
                    category = "Battle Royale • Miễn phí",
                    categoryEn = "Battle Royale • Free to Play",
                    playUrl = "https://www.xbox.com/en-US/play/games/fortnite/$FORTNITE_PRODUCT_ID",
                    freeToPlay = true,
                    supportsTouch = true,
                    accentColorHex = 0xFF0074E4L,
                    badge = "Cảm ứng Epic • Miễn phí",
                    badgeEn = "Epic Touch • Free",
                    drawableResId = null,
                    rating = "4.9",
                    imageUrl = "https://store-images.s-microsoft.com/image/apps.50670.70702278257994163.1052c4f6-c429-424b-9b56-0303b1420312.69b75902-e543-496f-9464-480b3795328d?w=260&q=70",
                    heroImageUrl = "https://store-images.s-microsoft.com/image/apps.50670.70702278257994163.1052c4f6-c429-424b-9b56-0303b1420312.69b75902-e543-496f-9464-480b3795328d?w=520&q=70",
                    description = "Chơi Fortnite miễn phí trên Xbox Cloud Gaming chỉ cần tài khoản Microsoft, không cần gói Game Pass.",
                    publisher = "Epic Games",
                    isPopular = true,
                    isRecentlyAdded = false
                )
            )
        }

        _games.value = merged
        _xboxStatus.value = _xboxStatus.value.copy(
            syncedCatalogCount = merged.size,
            totalCloudCatalogCount = _xboxStatus.value.totalCloudCatalogCount.coerceAtLeast(merged.size)
        )
    }

    fun extractProductId(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val clean = url.substringBefore("?").substringBefore("#").trimEnd('/')
        // Hỗ trợ cả Giao diện mới (play.xbox.com/products/<productId>/<slug>)
        val afterProducts = clean.substringAfter("/products/", "").trim('/')
        if (afterProducts.isNotBlank()) {
            val firstSeg = afterProducts.substringBefore("/").uppercase(Locale.ROOT)
            if (firstSeg.length in 10..14 && firstSeg.all { it.isLetterOrDigit() }) {
                return firstSeg
            }
        }
        val afterGames = clean.substringAfter("/play/games/", "")
            .ifBlank { clean.substringAfter("/play/launch/", "") }
            .ifBlank { clean.substringAfter("/games/", "") }
            .ifBlank { clean.substringAfter("/launch/", "") }
            .trim('/')
        if (afterGames.isBlank()) return ""
        val segments = afterGames.split("/").filter { it.isNotBlank() }
        for (seg in segments.asReversed()) {
            val candidate = seg.uppercase(Locale.ROOT)
            if (candidate.length == 12 && candidate.all { it.isLetterOrDigit() } && candidate.any { it.isDigit() }) {
                return candidate
            }
        }
        val lastSeg = segments.lastOrNull()?.uppercase(Locale.ROOT).orEmpty()
        if (lastSeg.length in 10..14 && lastSeg.all { it.isLetterOrDigit() } && lastSeg.any { it.isDigit() }) {
            return lastSeg
        }
        return ""
    }

    fun extractSlug(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val clean = url.substringBefore("?").substringBefore("#").trimEnd('/')
        val afterProducts = clean.substringAfter("/products/", "").trim('/')
        if (afterProducts.isNotBlank()) {
            val secondSeg = afterProducts.substringAfter("/", "").substringBefore("/").lowercase(Locale.ROOT).trim()
            if (secondSeg.isNotBlank()) return secondSeg
        }
        val afterGames = clean.substringAfter("/play/games/", "")
            .ifBlank { clean.substringAfter("/play/launch/", "") }
            .ifBlank { clean.substringAfter("/games/", "") }
            .ifBlank { clean.substringAfter("/launch/", "") }
            .trim('/')
        if (afterGames.isBlank()) return ""
        val firstSeg = afterGames.substringBefore("/").lowercase(Locale.ROOT).trim()
        // Nếu segment đầu tiên là ProductId 12 ký tự (ví dụ /launch/BT5P2X999VH2) thì không coi là slug
        if (firstSeg.length == 12 && firstSeg.all { it.isLetterOrDigit() } && firstSeg.any { it.isDigit() }) {
            return ""
        }
        return firstSeg
    }

    private fun slugifyTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(SLUG_NON_ALNUM_REGEX, "-")
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
        userModifiedFavoritesEarly = true
        val current = _favoriteIds.value.toMutableSet()
        if (!current.add(gameId)) {
            current.remove(gameId)
        }
        val updated = current.toSet()
        _favoriteIds.value = updated
        prefs?.edit()?.putStringSet(KEY_FAVORITE_IDS, updated)?.apply()
    }

    fun recordLastPlayed(gameId: String) {
        val cleanId = gameId.trim()
        if (cleanId.isBlank()) return
        userModifiedLastPlayedEarly = true
        _lastPlayedId.value = cleanId
        val updatedList = (listOf(cleanId) + _playedGameIds.value.filterNot { it.equals(cleanId, ignoreCase = true) })
            .take(24)
        _playedGameIds.value = updatedList
        prefs?.edit()
            ?.putString(KEY_LAST_PLAYED_ID, cleanId)
            ?.putString(KEY_PLAYED_GAME_IDS_LIST, updatedList.joinToString(","))
            ?.apply()
        recentPrefs?.edit()?.putString(KEY_LAST_PLAYED_ID, cleanId)?.apply()
    }

    fun removePlayedGame(gameId: String) {
        val cleanId = gameId.trim()
        if (cleanId.isBlank()) return
        userModifiedLastPlayedEarly = true
        val updatedList = _playedGameIds.value.filterNot { it.equals(cleanId, ignoreCase = true) }
        _playedGameIds.value = updatedList
        val nextLast = updatedList.firstOrNull()
        _lastPlayedId.value = nextLast
        prefs?.edit()
            ?.putString(KEY_LAST_PLAYED_ID, nextLast)
            ?.putString(KEY_PLAYED_GAME_IDS_LIST, updatedList.joinToString(","))
            ?.apply()
        recentPrefs?.edit()?.putString(KEY_LAST_PLAYED_ID, nextLast)?.apply()
    }

    fun clearPlayedGames() {
        userModifiedLastPlayedEarly = true
        _playedGameIds.value = emptyList()
        _lastPlayedId.value = null
        prefs?.edit()
            ?.remove(KEY_LAST_PLAYED_ID)
            ?.remove(KEY_PLAYED_GAME_IDS_LIST)
            ?.apply()
        recentPrefs?.edit()?.remove(KEY_LAST_PLAYED_ID)?.apply()
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
                        markAsLastPlayed = false
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
            cleanUrl.contains("/play/launch/", ignoreCase = true) ||
            cleanUrl.contains("/products/", ignoreCase = true) ||
            cleanUrl.contains("play.xbox.com", ignoreCase = true)

        val cleanedTitle = rawTitle
            ?.replace(XBOX_CLOUD_SUFFIX_REGEX, "")
            ?.replace(XBOX_PIPE_SUFFIX_REGEX, "")
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
        val isFreeCloud = isFreeToPlayTitle(resolvedTitle, productId, canonicalPlayUrl)
        val newBookmark = GameBookmark(
            id = if (isFreeCloud && productId.equals(FORTNITE_PRODUCT_ID, ignoreCase = true)) "fortnite" else newId,
            title = resolvedTitle,
            category = category ?: "Xbox Cloud Game Pass",
            categoryEn = category ?: "Xbox Cloud Game Pass",
            playUrl = canonicalPlayUrl,
            freeToPlay = isFreeCloud,
            supportsTouch = supportsTouch ?: true,
            accentColorHex = accent,
            badge = if (isFreeCloud) "Cảm ứng Epic • Miễn phí" else "Xbox Cloud",
            badgeEn = if (isFreeCloud) "Epic Touch • Free" else "Xbox Cloud",
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

    /**
     * Chỉ đánh dấu Miễn phí (freeToPlay = true) cho các tựa game thực sự được Microsoft mở miễn phí 100%
     * trên Xbox Cloud Gaming mà KHÔNG cần mua gói Game Pass Ultimate (hiện tại là Fortnite - BT5P2X999VH2).
     * Các game miễn phí trên PC/Console như Overwatch 2, Call of Duty: Warzone, Halo Infinite, Destiny 2,
     * Apex Legends, Roblox... trên Xbox Cloud đều yêu cầu Game Pass Ultimate (hoặc không có trên Cloud miễn phí)
     * nên KHÔNG được gắn cờ freeToPlay = true.
     */
    private fun isFreeToPlayTitle(
        title: String,
        productId: String = "",
        playUrl: String = ""
    ): Boolean {
        val cleanTitle = title.trim()
        val pid = productId.ifBlank { extractProductId(playUrl) }
        if (pid.equals(FORTNITE_PRODUCT_ID, ignoreCase = true)) return true
        return cleanTitle.equals("Fortnite", ignoreCase = true)
    }

    /**
     * Ingests a batch of game cards scraped live from the Xbox Cloud web DOM (metadata only, no image downloads).
     */
    fun ingestDiscoveredWebGames(jsonArrayStr: String) {
        if (jsonArrayStr.isBlank() || isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return
        ioScope.launch {
            try {
                if (isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return@launch
                val arr = JSONArray(jsonArrayStr)
                if (arr.length() == 0) return@launch
                val currentList = _games.value.toMutableList()
                var changed = false

                for (i in 0 until arr.length()) {
                    if (isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return@launch
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
                            .replace(XBOX_BULLET_SUFFIX_REGEX, "")
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
                                freeToPlay = isFreeToPlayTitle(cleanTitle, pid, canonicalUrl),
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

                if (changed && !isStreamActive && !com.example.vpn.VpnManager.isStreamActive()) {
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
    private var catalogSyncJob: kotlinx.coroutines.Job? = null

    fun setStreamActive(active: Boolean) {
        isStreamActive = active
        if (active) {
            catalogSyncJob?.cancel()
            _isSyncingCatalog.value = false
        }
    }

    /**
     * Tự động quét toàn bộ kho game Xbox Cloud theo từng lô (streaming batches)
     * và chỉ lấy thông tin game (tên, thể loại, nhà phát hành, mô tả, link chơi),
     * hoàn toàn không tải ảnh bìa để tiết kiệm dữ liệu mạng.
     * Tự động tạm dừng ngay khi người dùng đang trong luồng chơi game để nhường 100% băng thông và tốc độ giải mã video.
     */
    fun syncLiveXboxCatalog(force: Boolean = false, maxCatalogGames: Int = 600) {
        if (!force && (_isSyncingCatalog.value || isStreamActive || com.example.vpn.VpnManager.isStreamActive())) return
        if (force) {
            isStreamActive = false
        }
        catalogSyncJob?.cancel()
        catalogSyncJob = ioScope.launch {
            _isSyncingCatalog.value = true
            val startPingMs = System.currentTimeMillis()
            var anyBatchAdded = false
            try {
                if (!force && (isStreamActive || com.example.vpn.VpnManager.isStreamActive())) return@launch

                val (allCloudIds, touchIds, popularIds, recentIds) = coroutineScope {
                    val allDeferred = async {
                        fetchSiglProductIdsSafe(CLOUD_CATALOG_SIGL_URL, maxCount = 650)
                    }
                    val touchDeferred = async {
                        fetchSiglProductIdsSafe(TOUCH_CATALOG_SIGL_URL, maxCount = 350).toSet()
                    }
                    val popDeferred = async {
                        fetchSiglProductIdsSafe(POPULAR_CLOUD_SIGL_URL, maxCount = 150).toSet()
                    }
                    val recDeferred = async {
                        fetchSiglProductIdsSafe(RECENTLY_ADDED_SIGL_URL, maxCount = 100).toSet()
                    }
                    Quadruple(
                        allDeferred.await(),
                        touchDeferred.await(),
                        popDeferred.await(),
                        recDeferred.await()
                    )
                }

                if (!force && isStreamActive) return@launch
                val catalogRtt = (System.currentTimeMillis() - startPingMs).toInt().coerceIn(15, 2500)

                if (allCloudIds.isNotEmpty()) {
                    cachedAllCloudProductIds = allCloudIds
                }
                if (touchIds.isNotEmpty()) cachedTouchIds = touchIds
                if (popularIds.isNotEmpty()) cachedPopularIds = popularIds
                if (recentIds.isNotEmpty()) cachedRecentIds = recentIds

                val effectiveAllIds = if (allCloudIds.isNotEmpty()) allCloudIds else cachedAllCloudProductIds
                val prioritizedCloudIds = (
                    listOf(FORTNITE_PRODUCT_ID) +
                        popularIds.toList() +
                        recentIds.toList() +
                        effectiveAllIds +
                        touchIds.toList()
                    )
                    .map { it.uppercase(Locale.ROOT) }
                    .distinct()
                    .take(maxCatalogGames.coerceAtLeast(600))

                if (prioritizedCloudIds.isEmpty()) {
                    _xboxStatus.value = _xboxStatus.value.copy(cloudServiceOnline = false)
                    return@launch
                }

                val totalCloud = prioritizedCloudIds.size.coerceAtLeast(effectiveAllIds.size)
                _xboxStatus.value = _xboxStatus.value.copy(
                    cloudServiceOnline = true,
                    cloudLatencyMs = catalogRtt,
                    totalCloudCatalogCount = totalCloud.coerceAtLeast(_games.value.size)
                )

                val sharedPrefs = prefs
                val inMemoryGames = _games.value
                val customList = if (inMemoryGames.isNotEmpty()) {
                    inMemoryGames.filter { it.id.startsWith("custom_") }
                } else if (sharedPrefs != null) {
                    loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
                } else {
                    emptyList()
                }
                val diskSynced = if (sharedPrefs != null) {
                    loadSyncedGamesFromFileOrMigratePrefs(sharedPrefs)
                } else {
                    emptyList()
                }
                val memorySynced = inMemoryGames.filter { !it.id.startsWith("custom_") }
                val existingSynced = (diskSynced + memorySynced)
                    .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }

                val existingWithImages = existingSynced.filter { !it.imageUrl.isNullOrBlank() }
                if (existingWithImages.size > memorySynced.size) {
                    rebuildMergedGames(customList, existingWithImages)
                }

                val existingPids = existingWithImages
                    .mapNotNull { extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }?.uppercase(Locale.ROOT) }
                    .toSet()

                val missingIds = prioritizedCloudIds.filter { it !in existingPids }
                val idsToFetch = if (missingIds.isNotEmpty()) {
                    missingIds
                } else if (force) {
                    prioritizedCloudIds
                } else {
                    emptyList()
                }

                val runningSynced = existingWithImages.toMutableList()
                var completedBatches = 0
                for (batch in idsToFetch.chunked(40)) {
                    if (!force && isStreamActive) break
                    val batchResults = fetchDisplayCatalogProductsSafe(
                        bigIds = batch,
                        touchIds = cachedTouchIds,
                        popularIds = cachedPopularIds,
                        recentIds = cachedRecentIds
                    )
                    if (!force && isStreamActive) break
                    if (batchResults.isNotEmpty()) {
                        anyBatchAdded = true
                        runningSynced.addAll(batchResults)
                        val progressiveDeduped = runningSynced
                            .filter { !it.imageUrl.isNullOrBlank() }
                            .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                        rebuildMergedGames(customList, progressiveDeduped)
                        val currentCount = _games.value.size
                        _xboxStatus.value = _xboxStatus.value.copy(
                            cloudServiceOnline = true,
                            cloudLatencyMs = catalogRtt,
                            totalCloudCatalogCount = totalCloud.coerceAtLeast(currentCount),
                            syncedCatalogCount = currentCount
                        )
                        completedBatches++
                        if (completedBatches % 3 == 0) {
                            saveSyncedGames(_games.value)
                        }
                    }
                }

                if (runningSynced.isNotEmpty()) {
                    val finalDeduped = runningSynced
                        .filter { !it.imageUrl.isNullOrBlank() }
                        .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    rebuildMergedGames(customList, finalDeduped)
                }

                saveSyncedGames(_games.value)
                val nowMs = System.currentTimeMillis()
                val finalCount = _games.value.size
                _xboxStatus.value = _xboxStatus.value.copy(
                    cloudServiceOnline = true,
                    cloudLatencyMs = catalogRtt,
                    totalCloudCatalogCount = totalCloud.coerceAtLeast(finalCount),
                    syncedCatalogCount = finalCount,
                    lastSyncTimeMs = nowMs
                )
                sharedPrefs?.edit()
                    ?.putLong(KEY_LAST_CATALOG_SYNC_MS, nowMs)
                    ?.putInt(KEY_XBOX_TOTAL_CLOUD, totalCloud.coerceAtLeast(finalCount))
                    ?.apply()
            } catch (_: Exception) {
                if (anyBatchAdded) {
                    saveSyncedGames(_games.value)
                } else {
                    _xboxStatus.value = _xboxStatus.value.copy(cloudServiceOnline = _games.value.size > 2)
                }
            } finally {
                _isSyncingCatalog.value = false
            }
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    /**
     * Loads the next batch of unsynced Xbox Cloud games from Microsoft's full Cloud SIGL feed.
     */
    fun loadMoreXboxCatalogGames(batchSize: Int = 40) {
        if (_isSyncingCatalog.value || isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return
        ioScope.launch {
            _isSyncingCatalog.value = true
            try {
                if (cachedAllCloudProductIds.isEmpty()) {
                    cachedAllCloudProductIds = fetchSiglProductIdsSafe(CLOUD_CATALOG_SIGL_URL, maxCount = 650)
                }
                if (cachedTouchIds.isEmpty()) {
                    cachedTouchIds = fetchSiglProductIdsSafe(TOUCH_CATALOG_SIGL_URL, maxCount = 350).toSet()
                }
                val existingPids = _games.value
                    .mapNotNull { extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }?.uppercase(Locale.ROOT) }
                    .toSet()

                val nextBatchIds = cachedAllCloudProductIds
                    .filter { it.uppercase(Locale.ROOT) !in existingPids }
                    .take(batchSize)

                if (nextBatchIds.isEmpty()) {
                    syncLiveXboxCatalog(force = true, maxCatalogGames = 600)
                    return@launch
                }

                val newBookmarks = mutableListOf<GameBookmark>()
                nextBatchIds.chunked(20).forEach { chunk ->
                    newBookmarks.addAll(
                        fetchDisplayCatalogProductsSafe(
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

    private suspend fun fetchSiglProductIdsSafe(siglUrl: String, maxCount: Int): List<String> {
        for (attempt in 0..1) {
            try {
                val list = fetchSiglProductIds(siglUrl, maxCount)
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {
            }
            if (attempt == 0) {
                kotlinx.coroutines.delay(350L)
            }
        }
        return emptyList()
    }

    private fun fetchSiglProductIds(siglUrl: String, maxCount: Int): List<String> {
        val conn = (URL(siglUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
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

    private suspend fun fetchDisplayCatalogProductsSafe(
        bigIds: List<String>,
        touchIds: Set<String>,
        popularIds: Set<String> = emptySet(),
        recentIds: Set<String> = emptySet()
    ): List<GameBookmark> {
        if (bigIds.isEmpty()) return emptyList()
        for (attempt in 0..1) {
            try {
                val items = fetchDisplayCatalogProducts(
                    bigIds = bigIds,
                    touchIds = touchIds,
                    popularIds = popularIds,
                    recentIds = recentIds
                )
                if (items.isNotEmpty()) return items
            } catch (_: Exception) {
            }
            if (attempt == 0) {
                kotlinx.coroutines.delay(350L)
            }
        }
        return emptyList()
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
                    .replace(WHITESPACE_COLLAPSE_REGEX, " ")
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
                    isFortnite -> "Cảm ứng Epic • Miễn phí"
                    isRecent -> "Mới lên kệ • Cloud"
                    isPop -> "Top Hot • Xbox"
                    isTouch -> "Cảm ứng • Cloud"
                    else -> "Game Pass"
                }
                val badgeEn = when {
                    isFortnite -> "Epic Touch • Free"
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
                        freeToPlay = isFreeToPlayTitle(cleanTitle, productId, playUrl),
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
        val rawJson = try {
            sharedPrefs.getString(key, null)
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        return parseGamesJsonArray(rawJson)
    }

    private fun writeTextAtomically(targetFile: java.io.File, content: String) {
        synchronized(cacheFileLock) {
            val parent = targetFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tmpFile = java.io.File(parent ?: targetFile.parentFile, "${targetFile.name}.tmp")
            tmpFile.writeText(content, Charsets.UTF_8)
            if (!tmpFile.renameTo(targetFile)) {
                targetFile.writeText(content, Charsets.UTF_8)
                tmpFile.delete()
            }
        }
    }

    private fun loadSyncedGamesFromFileOrMigratePrefs(sharedPrefs: SharedPreferences): List<GameBookmark> {
        val cacheFile = syncedGamesCacheFile
        if (cacheFile != null && cacheFile.exists() && cacheFile.length() > 2L) {
            val fileJson = try {
                synchronized(cacheFileLock) {
                    cacheFile.readText(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            }
            if (!fileJson.isNullOrBlank()) {
                val parsed = parseGamesJsonArray(fileJson)
                if (parsed.isNotEmpty()) {
                    if (sharedPrefs.contains(KEY_SYNCED_GAMES_JSON)) {
                        sharedPrefs.edit().remove(KEY_SYNCED_GAMES_JSON).apply()
                    }
                    return parsed
                }
            }
        }
        val legacyJson = try {
            sharedPrefs.getString(KEY_SYNCED_GAMES_JSON, null)
        } catch (_: Exception) {
            null
        }
        if (!legacyJson.isNullOrBlank()) {
            try {
                if (cacheFile != null) {
                    writeTextAtomically(cacheFile, legacyJson)
                }
                sharedPrefs.edit().remove(KEY_SYNCED_GAMES_JSON).apply()
            } catch (_: Exception) {
            }
            return parseGamesJsonArray(legacyJson)
        }
        return emptyList()
    }

    private fun parseGamesJsonArray(rawJson: String): List<GameBookmark> {
        return try {
            val arr = JSONArray(rawJson)
            val list = ArrayList<GameBookmark>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val playUrl = obj.optString("playUrl", "").trim()
                if (title.isBlank() || playUrl.isBlank()) continue
                val id = obj.optString("id", "custom_$i")
                val pid = extractProductId(playUrl)
                val isFortnite = id == "fortnite" || pid.equals(FORTNITE_PRODUCT_ID, ignoreCase = true) || title.equals("Fortnite", ignoreCase = true)
                val cat = obj.optString("category", "Xbox Cloud Game Pass")
                val rawBadge = obj.optString("badge", "").takeIf { it.isNotBlank() }
                val badge = rawBadge?.let { b ->
                    if (b.contains("120", ignoreCase = true)) "Cảm ứng Epic • Miễn phí" else b
                }
                val rawBadgeEn = obj.optString("badgeEn", badge ?: "").takeIf { it.isNotBlank() }
                val badgeEn = rawBadgeEn?.let { b ->
                    if (b.contains("120", ignoreCase = true)) "Epic Touch • Free" else b
                }
                val imgUrl = normalizeImageUrl(obj.optString("imageUrl", ""), widthPx = 260, quality = 70)
                val heroUrl = normalizeImageUrl(obj.optString("heroImageUrl", ""), widthPx = 520, quality = 70) ?: imgUrl
                val isCustomFree = id.startsWith("custom_") && obj.optBoolean("freeToPlay", false)
                list.add(
                    GameBookmark(
                        id = if (isFortnite) "fortnite" else id,
                        title = title,
                        category = cat,
                        categoryEn = obj.optString("categoryEn", cat),
                        playUrl = playUrl,
                        freeToPlay = isFreeToPlayTitle(title, pid, playUrl) || isCustomFree,
                        supportsTouch = obj.optBoolean("supportsTouch", true),
                        accentColorHex = obj.optLong("accentColorHex", 0xFF107C10L),
                        badge = badge,
                        badgeEn = badgeEn,
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
        if (!initialDiskLoadCompleted) return
        val syncedGames = allGames
            .filter { !it.id.startsWith("custom_") && !it.imageUrl.isNullOrBlank() }
            .take(650)
        if (syncedGames.isEmpty()) return
        ioScope.launch {
            val cacheFile = syncedGamesCacheFile
            val finalToSave = if (syncedGames.size < 100 && cacheFile != null && cacheFile.exists() && cacheFile.length() > 2L) {
                val existingOnDisk = try {
                    val fileJson = synchronized(cacheFileLock) {
                        cacheFile.readText(Charsets.UTF_8)
                    }
                    parseGamesJsonArray(fileJson)
                } catch (_: Exception) {
                    emptyList()
                }
                (syncedGames + existingOnDisk)
                    .filter { !it.id.startsWith("custom_") && !it.imageUrl.isNullOrBlank() }
                    .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    .take(650)
            } else {
                syncedGames
            }
            val jsonStr = serializeGamesListToJson(finalToSave) ?: return@launch
            if (cacheFile != null) {
                try {
                    writeTextAtomically(cacheFile, jsonStr)
                    prefs?.let { sp ->
                        if (sp.contains(KEY_SYNCED_GAMES_JSON)) {
                            sp.edit().remove(KEY_SYNCED_GAMES_JSON).apply()
                        }
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
            prefs?.edit()?.putString(KEY_SYNCED_GAMES_JSON, jsonStr)?.apply()
        }
    }

    private fun saveGamesListToPrefs(sharedPrefs: SharedPreferences, key: String, gamesList: List<GameBookmark>) {
        val jsonStr = serializeGamesListToJson(gamesList) ?: return
        sharedPrefs.edit().putString(key, jsonStr).apply()
    }

    private fun serializeGamesListToJson(gamesList: List<GameBookmark>): String? {
        return try {
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
            arr.toString()
        } catch (_: Exception) {
            null
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



