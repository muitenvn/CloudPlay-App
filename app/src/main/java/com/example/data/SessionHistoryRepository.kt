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
    val jitterBufferDelayMs: Double = 0.0,
    val networkType: String = "",
    val framesDecoded: Int = 0
) {
    fun formattedNetworkLabel(isEn: Boolean): String {
        val modeLabel = if (isDirectNetwork) {
            if (isEn) "Direct" else "Trực tiếp"
        } else {
            "VPN"
        }
        val netSuffix = when (networkType.uppercase()) {
            "WIFI" -> " • Wi-Fi"
            "CELLULAR_5G", "5G" -> " • 5G"
            "CELLULAR_4G", "4G", "LTE" -> " • 4G"
            "ETHERNET" -> " • LAN"
            else -> ""
        }
        return if (serverName.isNotBlank()) "$serverName • $modeLabel$netSuffix" else "$modeLabel$netSuffix"
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

    fun deserializeSessionsJson(raw: String): List<PlaySessionRecord> {
        val loaded = mutableListOf<PlaySessionRecord>()
        try {
            val arr = JSONArray(raw)
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
                        jitterBufferDelayMs = jbDelay,
                        networkType = obj.optString("networkType", "").trim(),
                        framesDecoded = obj.optInt("framesDecoded", 0).coerceAtLeast(0)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return loaded
    }

    fun serializeSessionsJson(list: List<PlaySessionRecord>): String {
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
                put("jitterBufferDelayMs", ((item.jitterBufferDelayMs * 10.0).roundToInt()) / 100.0 * 10.0)
                put("networkType", item.networkType)
                put("framesDecoded", item.framesDecoded)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun loadFromPrefs(sharedPrefs: SharedPreferences) {
        try {
            val raw = sharedPrefs.getString(KEY_SESSIONS_JSON, null) ?: return
            _sessions.value = deserializeSessionsJson(raw)
                .sortedByDescending { it.startTimeMillis }
                .take(MAX_SESSIONS)
        } catch (_: Exception) {
        }
    }

    private fun saveToPrefs(list: List<PlaySessionRecord>) {
        val sharedPrefs = prefs ?: return
        try {
            sharedPrefs.edit().putString(KEY_SESSIONS_JSON, serializeSessionsJson(list)).apply()
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

    /** Hệ số giảm trọng số theo thời gian cho các phiên chơi từ mới nhất về cũ hơn (phiên mới nhất = 1.0, tiếp theo = 0.75, 0.5625...) */
    const val RECENCY_DECAY_FACTOR = 0.75

    @androidx.compose.runtime.Immutable
    data class RegionSessionStats(
        val avgRttMs: Int,
        val sampleCount: Int,
        val weightedFreezePerMin: Double,
        val weightedFrameDropPercent: Double
    )

    private fun doesSessionMatchCountryCode(record: PlaySessionRecord, countryCode: String): Boolean {
        val code = countryCode.trim().uppercase()
        if (code.isBlank()) return false
        val nameLower = record.serverName.trim().lowercase()
        if (nameLower.isBlank()) return false
        return when (code) {
            "KR" -> nameLower.contains("seoul") ||
                nameLower.contains("korea") ||
                nameLower.contains("hàn quốc") ||
                nameLower.contains("busan") ||
                nameLower.contains("incheon") ||
                nameLower.contains("🇰🇷") ||
                nameLower.contains(" kt") ||
                nameLower.startsWith("kt ") ||
                nameLower.contains("sk broadband") ||
                nameLower.contains("uplus") ||
                nameLower.contains("dacom")
            "JP" -> nameLower.contains("tokyo") ||
                nameLower.contains("osaka") ||
                nameLower.contains("tsukuba") ||
                nameLower.contains("japan") ||
                nameLower.contains("nhật bản") ||
                nameLower.contains("🇯🇵") ||
                nameLower.contains("ntt") ||
                nameLower.contains("kddi") ||
                nameLower.contains("iij") ||
                nameLower.contains("sakura") ||
                nameLower.contains("softbank") ||
                nameLower.contains("nagoya") ||
                nameLower.contains("fukuoka") ||
                nameLower.contains("sapporo") ||
                nameLower.contains("yokohama")
            "US" -> nameLower.contains("usa") ||
                nameLower.contains("united states") ||
                nameLower.contains("hoa kỳ") ||
                nameLower.contains("🇺🇸") ||
                nameLower.contains("virginia") ||
                nameLower.contains("oregon") ||
                nameLower.contains("california") ||
                nameLower.contains("san jose") ||
                nameLower.contains("seattle") ||
                nameLower.contains("dallas") ||
                nameLower.contains("chicago") ||
                nameLower.contains("new york") ||
                nameLower.contains("ashburn") ||
                nameLower.contains("los angeles")
            else -> false
        }
    }

    /**
     * Tính thống kê thực tế từ lịch sử phiên chơi gần nhất của máy chủ hoặc vùng quốc gia (`countryCode`).
     * Trả về `null` nếu chưa có bất kỳ phiên chơi hợp lệ nào (không bao giờ trả về số giả định).
     */
    fun getRegionSessionStats(
        serverName: String = "",
        countryCode: String = "",
        sameCountryServerNames: Set<String> = emptySet(),
        maxRecentSessions: Int = 5,
        sessionsList: List<PlaySessionRecord> = _sessions.value
    ): RegionSessionStats? {
        if (sessionsList.isEmpty() || maxRecentSessions <= 0) return null
        val cleanTarget = serverName.trim()
        val normalizedPoolNames = sameCountryServerNames
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val sortedValid = sessionsList
            .filter { it.avgPingMs > 0 }
            .sortedByDescending { it.startTimeMillis }
        if (sortedValid.isEmpty()) return null

        val exactMatches = if (cleanTarget.isNotBlank()) {
            sortedValid.filter { it.serverName.trim().equals(cleanTarget, ignoreCase = true) }
        } else {
            emptyList()
        }

        val regionMatches = if (countryCode.isNotBlank() || normalizedPoolNames.isNotEmpty()) {
            sortedValid.filter { rec ->
                val recNameLower = rec.serverName.trim().lowercase()
                (cleanTarget.isNotBlank() && rec.serverName.trim().equals(cleanTarget, ignoreCase = true)) ||
                    (recNameLower.isNotBlank() && normalizedPoolNames.contains(recNameLower)) ||
                    doesSessionMatchCountryCode(rec, countryCode)
            }
        } else {
            emptyList()
        }

        // Ưu tiên kết hợp các phiên của chính máy chủ đó trước, sau đó bổ sung các phiên cùng vùng quốc gia cho đủ mẫu gần nhất
        val combinedRecent = (exactMatches + regionMatches)
            .distinctBy { it.id }
            .take(maxRecentSessions)

        if (combinedRecent.isEmpty()) return null

        var totalWeight = 0.0
        var weightedRttSum = 0.0
        var weightedFreezePerMinSum = 0.0
        var weightedDropPercentSum = 0.0

        combinedRecent.forEachIndexed { index, session ->
            val weight = Math.pow(RECENCY_DECAY_FACTOR, index.toDouble())
            val durationMinutes = (session.durationSeconds.coerceAtLeast(30L)).toDouble() / 60.0
            val freezePerMin = session.freezeCount.coerceAtLeast(0).toDouble() / durationMinutes
            val dropPercent = when {
                session.framesDecoded > 0 -> {
                    (session.framesDropped.coerceAtLeast(0).toDouble() / session.framesDecoded.toDouble()) * 100.0
                }
                session.framesDropped > 0 -> {
                    val estimatedTotalFrames = (session.durationSeconds.coerceAtLeast(30L) * session.avgFps.coerceAtLeast(30)).toDouble()
                    (session.framesDropped.toDouble() / estimatedTotalFrames.coerceAtLeast(100.0)) * 100.0
                }
                else -> 0.0
            }.coerceIn(0.0, 100.0)

            totalWeight += weight
            weightedRttSum += session.avgPingMs.toDouble() * weight
            weightedFreezePerMinSum += freezePerMin * weight
            weightedDropPercentSum += dropPercent * weight
        }

        if (totalWeight <= 0.0) return null

        return RegionSessionStats(
            avgRttMs = (weightedRttSum / totalWeight).roundToInt(),
            sampleCount = combinedRecent.size,
            weightedFreezePerMin = weightedFreezePerMinSum / totalWeight,
            weightedFrameDropPercent = weightedDropPercentSum / totalWeight
        )
    }

    /**
     * Returns the average WebRTC RTT (ms) of saved sessions for a specific server or its region.
     * Returns -1 if no valid session RTT exists yet.
     */
    fun getAverageRttForServerOrRegion(
        serverName: String,
        countryCode: String = "",
        sameRegionServerNames: Set<String> = emptySet(),
        sessionsList: List<PlaySessionRecord> = _sessions.value
    ): Int {
        val cleanTarget = serverName.trim()
        if (cleanTarget.isNotBlank()) {
            val exactServerMatches = sessionsList.filter {
                it.avgPingMs > 0 && it.serverName.trim().equals(cleanTarget, ignoreCase = true)
            }
            if (exactServerMatches.isNotEmpty()) {
                return exactServerMatches.take(5).map { it.avgPingMs }.average().roundToInt()
            }
        }
        val stats = getRegionSessionStats(
            serverName = cleanTarget,
            countryCode = countryCode,
            sameCountryServerNames = sameRegionServerNames,
            maxRecentSessions = 5,
            sessionsList = sessionsList
        )
        return stats?.avgRttMs ?: -1
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
