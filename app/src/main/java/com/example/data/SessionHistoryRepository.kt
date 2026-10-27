package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

@androidx.compose.runtime.Immutable
data class PlaySessionRecord(
    val id: String,
    val gameTitle: String,
    val startTimeMillis: Long,
    val durationSeconds: Long,
    val serverName: String,
    val isDirectNetwork: Boolean,
    val avgPingMs: Int,
    val avgFps: Int,
    val avgDecodeMs: Double,
    val isSlowDecode: Boolean,
    val isThermalHot: Boolean,
    val pingSamples: List<Int>,
    val framesDropped: Int = 0,
    val freezeCount: Int = 0,
    val totalFreezesDuration: Double = 0.0,
    val jitterMs: Int = 0,
    val jitterBufferDelayMs: Double = 0.0
) {
    fun formattedNetworkLabel(isEn: Boolean): String {
        val modeLabel = if (isDirectNetwork) {
            if (isEn) "Direct" else "Trực tiếp"
        } else {
            "VPN"
        }
        return if (serverName.isNotBlank()) "$serverName • $modeLabel" else modeLabel
    }
}

object SessionHistoryRepository {
    private const val PREFS_NAME = "cloudplay_session_history_prefs"
    private const val KEY_SESSIONS_JSON = "session_history_json"
    private const val MAX_SESSIONS = 100
    private const val MAX_PING_SAMPLES = 60

    private var prefs: SharedPreferences? = null

    private val _sessions = MutableStateFlow<List<PlaySessionRecord>>(emptyList())
    val sessions: StateFlow<List<PlaySessionRecord>> = _sessions.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sharedPrefs
        loadFromPrefs(sharedPrefs)
    }

    private fun loadFromPrefs(sharedPrefs: SharedPreferences) {
        try {
            val raw = sharedPrefs.getString(KEY_SESSIONS_JSON, null) ?: return
            val arr = JSONArray(raw)
            val loaded = mutableListOf<PlaySessionRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val title = obj.optString("gameTitle", "").trim()
                if (id.isBlank() || title.isBlank()) continue

                val samplesArr = obj.optJSONArray("pingSamples")
                val samples = mutableListOf<Int>()
                if (samplesArr != null) {
                    val count = samplesArr.length().coerceAtMost(MAX_PING_SAMPLES)
                    for (j in 0 until count) {
                        val p = samplesArr.optInt(j, 0)
                        if (p > 0) samples.add(p)
                    }
                }

                val avgDecode = ((obj.optDouble("avgDecodeMs", 0.0) * 10.0).roundToInt()) / 10.0
                val totalFreezesDur = ((obj.optDouble("totalFreezesDuration", 0.0) * 100.0).roundToInt()) / 100.0
                val jbDelay = ((obj.optDouble("jitterBufferDelayMs", 0.0) * 10.0).roundToInt()) / 10.0
                loaded.add(
                    PlaySessionRecord(
                        id = id,
                        gameTitle = title,
                        startTimeMillis = obj.optLong("startTimeMillis", System.currentTimeMillis()),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        serverName = obj.optString("serverName", "Tokyo"),
                        isDirectNetwork = obj.optBoolean("isDirectNetwork", true),
                        avgPingMs = obj.optInt("avgPingMs", 0),
                        avgFps = obj.optInt("avgFps", 60),
                        avgDecodeMs = avgDecode,
                        isSlowDecode = obj.optBoolean("isSlowDecode", avgDecode > 12.0),
                        isThermalHot = obj.optBoolean("isThermalHot", false),
                        pingSamples = samples,
                        framesDropped = obj.optInt("framesDropped", 0),
                        freezeCount = obj.optInt("freezeCount", 0),
                        totalFreezesDuration = totalFreezesDur,
                        jitterMs = obj.optInt("jitterMs", 0),
                        jitterBufferDelayMs = jbDelay
                    )
                )
            }
            _sessions.value = loaded
                .sortedByDescending { it.startTimeMillis }
                .take(MAX_SESSIONS)
        } catch (_: Exception) {
        }
    }

    private fun saveToPrefs(list: List<PlaySessionRecord>) {
        val sharedPrefs = prefs ?: return
        try {
            val trimmed = list.sortedByDescending { it.startTimeMillis }.take(MAX_SESSIONS)
            val arr = JSONArray()
            for (item in trimmed) {
                val samplesJson = JSONArray()
                item.pingSamples.takeLast(MAX_PING_SAMPLES).forEach { samplesJson.put(it) }
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("gameTitle", item.gameTitle)
                    put("startTimeMillis", item.startTimeMillis)
                    put("durationSeconds", item.durationSeconds)
                    put("serverName", item.serverName)
                    put("isDirectNetwork", item.isDirectNetwork)
                    put("avgPingMs", item.avgPingMs)
                    put("avgFps", item.avgFps)
                    put("avgDecodeMs", ((item.avgDecodeMs * 10.0).roundToInt()) / 10.0)
                    put("isSlowDecode", item.isSlowDecode)
                    put("isThermalHot", item.isThermalHot)
                    put("pingSamples", samplesJson)
                    put("framesDropped", item.framesDropped)
                    put("freezeCount", item.freezeCount)
                    put("totalFreezesDuration", ((item.totalFreezesDuration * 100.0).roundToInt()) / 100.0)
                    put("jitterMs", item.jitterMs)
                    put("jitterBufferDelayMs", ((item.jitterBufferDelayMs * 10.0).roundToInt()) / 10.0)
                }
                arr.put(obj)
            }
            sharedPrefs.edit().putString(KEY_SESSIONS_JSON, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun recordSession(record: PlaySessionRecord) {
        if (record.durationSeconds < 30L) return
        val updated = (listOf(record) + _sessions.value.filterNot { it.id == record.id })
            .sortedByDescending { it.startTimeMillis }
            .take(MAX_SESSIONS)
        _sessions.value = updated
        saveToPrefs(updated)
    }

    fun deleteSession(sessionId: String) {
        val updated = _sessions.value.filterNot { it.id == sessionId }
        _sessions.value = updated
        saveToPrefs(updated)
    }

    fun clearAllSessions() {
        _sessions.value = emptyList()
        prefs?.edit()?.remove(KEY_SESSIONS_JSON)?.apply()
    }

    /**
     * Returns the average WebRTC RTT (ms) of saved sessions for a given server or its region/country.
     * Used in SMART_BYPASS mode to display "Ping lần chơi gần nhất" instead of TCP ping to server.ip.
     * Returns -1 if no valid session RTT exists yet ("Chưa có dữ liệu").
     */
    fun getAverageRttForServerOrRegion(
        serverName: String,
        countryCode: String,
        sameRegionServerNames: Set<String> = emptySet(),
        sessionsList: List<PlaySessionRecord> = _sessions.value
    ): Int {
        val validSessions = sessionsList.filter { it.avgPingMs > 0 }
        if (validSessions.isEmpty()) return -1

        val exactServerMatches = validSessions.filter {
            it.serverName.equals(serverName.trim(), ignoreCase = true)
        }
        if (exactServerMatches.isNotEmpty()) {
            return exactServerMatches.map { it.avgPingMs }.average().roundToInt()
        }

        val regionMatches = validSessions.filter { rec ->
            val recName = rec.serverName.trim()
            sameRegionServerNames.any { it.equals(recName, ignoreCase = true) } ||
                matchesCountryKeyword(recName, countryCode)
        }
        if (regionMatches.isNotEmpty()) {
            return regionMatches.map { it.avgPingMs }.average().roundToInt()
        }

        return -1
    }

    private fun matchesCountryKeyword(serverName: String, countryCode: String): Boolean {
        val lower = serverName.lowercase()
        return when (countryCode.trim().uppercase()) {
            "JP" -> lower.contains("japan") || lower.contains("tokyo") || lower.contains("osaka") || lower.contains("tsukuba") || lower.contains("chiba")
            "KR" -> lower.contains("korea") || lower.contains("seoul") || lower.contains("incheon") || lower.contains("busan")
            "US" -> lower.contains("united states") || lower.contains("silicon") || lower.contains("oregon") || lower.contains("iowa") || lower.contains("virginia") || lower.contains("nevada")
            "SG" -> lower.contains("singapore")
            "TW" -> lower.contains("taiwan") || lower.contains("taipei")
            "HK" -> lower.contains("hong kong")
            else -> false
        }
    }

    /**
     * Resolves a clean human-readable game title from onGameLaunchInitiated title or active URL.
     */
    fun resolveCleanGameTitle(rawTitle: String?, currentUrl: String?): String {
        val cleanedRaw = rawTitle
            ?.replace(Regex("\\s*[•|\\-–—]\\s*Xbox Cloud Gaming.*", RegexOption.IGNORE_CASE), "")
            ?.replace(Regex("\\s*\\|?\\s*Xbox.*", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()

        val isGenericTitle = cleanedRaw.isBlank() ||
            cleanedRaw.equals("Xbox Cloud Game", ignoreCase = true) ||
            cleanedRaw.equals("Xbox Game", ignoreCase = true) ||
            cleanedRaw.equals("Xbox Cloud Gaming", ignoreCase = true)

        if (!isGenericTitle) {
            val matchedLibrary = GameLibraryRepository.games.value.firstOrNull { bookmark ->
                bookmark.title.equals(cleanedRaw, ignoreCase = true) ||
                    cleanedRaw.contains(bookmark.title, ignoreCase = true)
            }
            return matchedLibrary?.title ?: cleanedRaw
        }

        val urlLower = currentUrl?.lowercase().orEmpty()
        if (urlLower.isNotBlank()) {
            val byLibraryUrl = GameLibraryRepository.games.value.firstOrNull { bookmark ->
                val slug = bookmark.playUrl.substringAfter("/games/", "").substringBefore("/").lowercase()
                val productId = bookmark.playUrl.substringAfterLast("/").lowercase()
                (slug.length > 2 && urlLower.contains(slug)) ||
                    (productId.length > 5 && urlLower.contains(productId))
            }
            if (byLibraryUrl != null) return byLibraryUrl.title

            val slugPart = urlLower
                .substringAfter("/launch/", "")
                .ifBlank { urlLower.substringAfter("/games/", "") }
                .substringBefore("/")
                .substringBefore("?")
            if (slugPart.isNotBlank() && slugPart.length > 2) {
                return slugPart.split("-")
                    .filter { it.isNotBlank() && it != "standard" && it != "edition" }
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { ch -> ch.uppercase() }
                    }
                    .ifBlank { "Xbox Cloud Game" }
            }
        }

        return if (cleanedRaw.isNotBlank()) cleanedRaw else "Xbox Cloud Game"
    }

    fun findMatchingBookmark(gameTitle: String): GameBookmark? {
        val games = GameLibraryRepository.games.value
        val trimmed = gameTitle.trim()
        return games.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
            ?: games.firstOrNull {
                it.title.contains(trimmed, ignoreCase = true) ||
                    trimmed.contains(it.title, ignoreCase = true)
            }
    }
}
