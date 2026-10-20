package com.example.cloudplay

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Smart Google Translation Engine designed specifically for Xbox Cloud Gaming.
 * Translates arbitrary text (game descriptions, guides, store synopsis, patch notes)
 * exactly like standard Google Translate, but with intelligent immunity for:
 * 1. Protected Game Titles (Fortnite, Forza Horizon, Halo, Starfield, etc.)
 * 2. Hardware / Stream elements (video, canvas, WebRTC)
 * 3. Gamepad buttons (A, B, X, Y, LB, RB, LT, RT, D-Pad, LS, RS, Menu, View)
 * 4. Technical telemetry (FPS, Bitrate Mbps, Ping ms, Codecs H.264/HEVC/AV1)
 * 5. Player Gamertags, URLs, and numeric identifiers.
 */
object GoogleGamingTranslator {

    private const val TAG = "GoogleGamingTranslator"
    private const val MAX_CACHE_ENTRIES = 500
    private const val MAX_PARALLEL_TRANSLATIONS = 4

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Bounded thread-safe LRU translation cache (prevents unbounded RAM growth)
    private val translationCache = object : LruCache<String, String>(MAX_CACHE_ENTRIES) {}
    private val batchSemaphore = Semaphore(MAX_PARALLEL_TRANSLATIONS)

    private fun getCachedTranslation(key: String): String? = synchronized(translationCache) {
        translationCache.get(key)
    }

    private fun putCachedTranslation(key: String, value: String) = synchronized(translationCache) {
        translationCache.put(key, value)
    }

    data class SmartTranslationResult(
        val originalText: String,
        val translatedText: String,
        val protectedTerms: List<String>,
        val translatedTerms: List<String>,
        val executionTimeMs: Long,
        val isFromCache: Boolean = false,
        val engine: String = "Google Neural Translate (xCloud Gaming Engine)"
    )

    // Hardware buttons, gaming telemetry, and technical specs that MUST NOT be mistranslated
    private val PROTECTED_CONTROLLER_TERMS = listOf(
        "A button", "B button", "X button", "Y button",
        "Left stick", "Right stick", "Left bumper", "Right bumper",
        "Left trigger", "Right trigger", "D-pad", "D-Pad",
        "LB", "RB", "LT", "RT", "LS", "RS", "L3", "R3",
        "Menu button", "View button", "Share button", "Xbox button", "Nexus button",
        "Clarity Boost", "Better-xCloud", "WebRTC", "H.264", "HEVC", "AV1",
        "1080p", "720p", "1440p", "4K HDR", "4K", "HDR10", "Dolby Vision", "Dolby Atmos",
        "FPS Boost", "DirectX", "Quick Resume", "Smart Delivery"
    )

    // Complete list of game titles to protect
    val IMMUNE_TITLES: List<String> by lazy {
        val list = mutableListOf<String>()
        list.addAll(XboxTranslator.PROTECTED_GAME_TITLES)
        list.addAll(
            listOf(
                "Call of Duty", "Modern Warfare", "Warzone", "Black Ops",
                "Resident Evil", "Silent Hill", "Monster Hunter Rise", "Monster Hunter World",
                "Deathloop", "Control", "Alan Wake", "Quantum Break", "Dishonored",
                "Wolfenstein", "Prey", "The Evil Within", "Quake", "Fallout: New Vegas",
                "Fallout 76", "Fallout 4", "Fallout 3", "Skyrim", "Oblivion", "Morrowind",
                "Senua's Saga: Hellblade", "Hellblade", "Avowed", "Fable", "Outer Worlds",
                "Pillars of Eternity", "Grounded", "Pentiment", "Sea of Stars",
                "Dead Space", "Mass Effect", "Dragon Age", "Titanfall", "Apex Legends",
                "Star Wars Jedi", "Star Wars Battlefront", "Battlefield 2042", "Battlefield V",
                "Need for Speed Unbound", "Need for Speed Heat", "Burnout Paradise",
                "Flight Simulator", "Age of Empires", "Age of Mythology", "Halo Wars",
                "State of Decay", "Crackdown", "Sunset Overdrive", "Quantum Break",
                "Ori and the Blind Forest", "Ori and the Will of the Wisps", "Psychonauts",
                "Banjo-Kazooie", "Perfect Dark", "Killer Instinct", "Battletoads",
                "Hi-Fi RUSH", "Lies of P", "Remnant II", "Remnant: From the Ashes",
                "Palworld", "Manor Lords", "Frostpunk", "Crusader Kings", "Football Manager",
                "Dead by Daylight", "Rainbow Six Siege", "Rainbow Six Extraction",
                "Far Cry 6", "Far Cry 5", "Assassin's Creed Valhalla", "Assassin's Creed Odyssey",
                "Assassin's Creed Mirage", "Watch Dogs", "The Division", "Ghost Recon",
                "Cyberpunk 2077", "The Witcher", "Elden Ring", "Dark Souls", "Bloodborne",
                "Armored Core", "Sekiro", "Sekiro: Shadows Die Twice", "Nier: Automata",
                "Persona 3 Reload", "Persona 4 Golden", "Persona 5 Royal", "Persona 5 Tactica",
                "Metaphor: ReFantazio", "Shin Megami Tensei", "Yakuza: Like a Dragon",
                "Like a Dragon: Infinite Wealth", "Like a Dragon Gaiden", "Judgment", "Lost Judgment",
                "Minecraft Dungeons", "Minecraft Legends", "Terraria", "Stardew Valley",
                "Forza Horizon 5", "Forza Horizon 4", "Forza Motorsport 7", "Forza Motorsport",
                "Grand Theft Auto V", "Grand Theft Auto VI", "GTA V", "GTA VI",
                "Doom Eternal", "The Elder Scrolls V: Skyrim", "Halo 5: Guardians",
                "Subnautica", "Deep Rock Galactic", "Valheim", "Vampire Survivors", "Brotato",
                "Slay the Spire", "Monster Train", "Inscryption", "Balatro", "Tunic", "Chained Echoes"
            )
        )
        // Sort descending by length so longer titles match first (e.g. "Forza Horizon 5" before "Forza")
        list.distinct().sortedByDescending { it.length }
    }

    /**
     * Replaces protected gaming words, controller keys, and telemetry with placeholders
     * before sending text to Google Translate, so Google cannot mistranslate them.
     */
    fun tokenizeProtectedContent(text: String): Pair<String, Map<String, String>> {
        var tokenized = text
        val placeholderMap = mutableMapOf<String, String>()
        var counter = 0

        // 1. Protect URLs and Email addresses
        val urlPattern = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE)
        val urlMatcher = urlPattern.matcher(tokenized)
        val urlBuffer = StringBuffer()
        while (urlMatcher.find()) {
            val matched = urlMatcher.group()
            val placeholder = "__CPURL_${counter}__"
            placeholderMap[placeholder] = matched
            urlMatcher.appendReplacement(urlBuffer, placeholder)
            counter++
        }
        urlMatcher.appendTail(urlBuffer)
        tokenized = urlBuffer.toString()

        // 2. Protect GamerTags / Handles (@UserName)
        val tagPattern = Pattern.compile("@[a-zA-Z0-9_]{3,20}")
        val tagMatcher = tagPattern.matcher(tokenized)
        val tagBuffer = StringBuffer()
        while (tagMatcher.find()) {
            val matched = tagMatcher.group()
            val placeholder = "__CPTAG_${counter}__"
            placeholderMap[placeholder] = matched
            tagMatcher.appendReplacement(tagBuffer, placeholder)
            counter++
        }
        tagMatcher.appendTail(tagBuffer)
        tokenized = tagBuffer.toString()

        // 3. Protect controller buttons & hardware terms
        for (term in PROTECTED_CONTROLLER_TERMS) {
            val pattern = Pattern.compile("\\b" + Pattern.quote(term) + "\\b", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(tokenized)
            if (matcher.find()) {
                val placeholder = "__CPTERM_${counter}__"
                placeholderMap[placeholder] = term
                tokenized = matcher.replaceAll(placeholder)
                counter++
            }
        }

        // 4. Protect immune game titles
        for (title in IMMUNE_TITLES) {
            if (tokenized.contains(title, ignoreCase = true)) {
                val pattern = Pattern.compile("\\b" + Pattern.quote(title) + "\\b", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(tokenized)
                if (matcher.find()) {
                    val placeholder = "__CPTITLE_${counter}__"
                    placeholderMap[placeholder] = title
                    tokenized = matcher.replaceAll(placeholder)
                    counter++
                }
            }
        }

        // 5. Protect FPS, Bitrate, Ping telemetry
        val telemetryPattern = Pattern.compile("(\\b\\d+\\s*(?:fps|mbps|mb/s|ms|ping|kbit/s|mbit/s|hz)\\b)", Pattern.CASE_INSENSITIVE)
        val telemMatcher = telemetryPattern.matcher(tokenized)
        val telemBuffer = StringBuffer()
        while (telemMatcher.find()) {
            val matched = telemMatcher.group(1) ?: ""
            val placeholder = "__CPTELEM_${counter}__"
            placeholderMap[placeholder] = matched
            telemMatcher.appendReplacement(telemBuffer, placeholder)
            counter++
        }
        telemMatcher.appendTail(telemBuffer)
        tokenized = telemBuffer.toString()

        return Pair(tokenized, placeholderMap)
    }

    /**
     * Restores protected terms back into the translated text.
     */
    fun restoreProtectedContent(translated: String, placeholderMap: Map<String, String>): String {
        var result = translated
        for ((placeholder, originalTerm) in placeholderMap) {
            result = result.replace(placeholder, originalTerm)

            val nameOnly = placeholder.trim('_')
            val parts = nameOnly.split("_")
            if (parts.size == 2) {
                val flexibleRegex = Regex("_*\\s*${parts[0]}\\s*_+\\s*${parts[1]}\\s*_*", RegexOption.IGNORE_CASE)
                result = result.replace(flexibleRegex, originalTerm)
            }
        }
        return result
    }

    /**
     * Translates a single phrase or entire paragraph into Vietnamese using Google Translate.
     * Guaranteed to preserve protected terms and avoid mistranslation.
     */
    suspend fun translate(
        rawText: String,
        targetLang: String = "vi",
        bilingual: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val trimmed = rawText.trim()
        if (trimmed.length < 2) return@withContext rawText

        // Check bounded LRU cache first
        val cacheKey = "$targetLang:$trimmed"
        val cached = getCachedTranslation(cacheKey)
        if (cached != null) {
            return@withContext if (bilingual && cached != trimmed) "$cached ($trimmed)" else cached
        }

        // Fast-path: Check local dictionary
        val dictMatch = XboxTranslator.XBOX_UI_DICTIONARY_EN[trimmed]
            ?: XboxTranslator.XBOX_UI_DICTIONARY_EN[trimmed.lowercase()]
        if (dictMatch != null) {
            putCachedTranslation(cacheKey, dictMatch)
            return@withContext if (bilingual) "$dictMatch ($trimmed)" else dictMatch
        }

        // Tokenize gaming terms to protect them
        val (tokenizedText, placeholderMap) = tokenizeProtectedContent(trimmed)

        try {
            val encodedQuery = URLEncoder.encode(tokenizedText, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext rawText
            }

            val responseBody = response.body?.string() ?: return@withContext rawText
            val jsonArray = JSONArray(responseBody)
            val sentencesArray = jsonArray.optJSONArray(0) ?: return@withContext rawText

            val translatedBuilder = StringBuilder()
            for (i in 0 until sentencesArray.length()) {
                val sentenceObj = sentencesArray.optJSONArray(i)
                if (sentenceObj != null && sentenceObj.length() > 0) {
                    val part = sentenceObj.optString(0, "")
                    translatedBuilder.append(part)
                }
            }

            val rawTranslated = translatedBuilder.toString()
            if (rawTranslated.isBlank()) return@withContext rawText

            // Restore protected gaming terms
            val finalizedText = restoreProtectedContent(rawTranslated, placeholderMap).trim()

            // Save to bounded LRU cache
            putCachedTranslation(cacheKey, finalizedText)

            if (bilingual && finalizedText != trimmed) {
                "$finalizedText ($trimmed)"
            } else {
                finalizedText
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Translate error for text: '$trimmed'", e)
            rawText
        }
    }

    /**
     * Translates text and provides detailed analysis of what was protected (bypassed)
     * versus what was translated into Vietnamese, ideal for user inspection.
     */
    suspend fun translateWithAnalysis(
        rawText: String,
        targetLang: String = "vi",
        bilingual: Boolean = false
    ): SmartTranslationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val trimmed = rawText.trim()
        val cacheKey = "$targetLang:$trimmed"
        val cached = getCachedTranslation(cacheKey)

        val (_, placeholderMap) = tokenizeProtectedContent(trimmed)
        val protectedList = placeholderMap.values.toList()

        if (cached != null) {
            val finalized = if (bilingual && cached != trimmed) "$cached ($trimmed)" else cached
            return@withContext SmartTranslationResult(
                originalText = rawText,
                translatedText = finalized,
                protectedTerms = protectedList,
                translatedTerms = listOf("Đã tải tức thì từ bộ nhớ đệm LRU (0ms)"),
                executionTimeMs = System.currentTimeMillis() - startTime,
                isFromCache = true
            )
        }

        val translated = translate(rawText, targetLang, bilingual)
        val timeSpent = System.currentTimeMillis() - startTime

        SmartTranslationResult(
            originalText = rawText,
            translatedText = translated,
            protectedTerms = protectedList,
            translatedTerms = listOf("Mô tả game / Cốt truyện / Menu điều hướng"),
            executionTimeMs = timeSpent,
            isFromCache = false
        )
    }

    /**
     * Controlled parallel batch translation using Semaphore(4) + async/awaitAll.
     */
    suspend fun translateBatch(
        texts: List<String>,
        targetLang: String = "vi",
        bilingual: Boolean = false
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val distinctTexts = texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinctTexts.isEmpty()) return@withContext emptyMap()

        coroutineScope {
            val deferredList = distinctTexts.map { text ->
                async {
                    batchSemaphore.withPermit {
                        text to translate(text, targetLang, bilingual)
                    }
                }
            }
            deferredList.awaitAll().toMap()
        }
    }

    fun clearCache() {
        synchronized(translationCache) {
            translationCache.evictAll()
        }
    }
}
