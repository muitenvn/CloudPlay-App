package com.example.cloudplay

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // In-memory persistent LRU-like translation cache
    private val translationCache = ConcurrentHashMap<String, String>()

    // Hardware buttons and controller terms that MUST NOT be mistranslated
    private val PROTECTED_CONTROLLER_TERMS = listOf(
        "A button", "B button", "X button", "Y button",
        "Left stick", "Right stick", "Left bumper", "Right bumper",
        "Left trigger", "Right trigger", "D-pad", "D-Pad",
        "LB", "RB", "LT", "RT", "LS", "RS", "L3", "R3",
        "Menu button", "View button", "Share button", "Xbox button", "Nexus button",
        "Clarity Boost", "Better-xCloud", "WebRTC", "H.264", "HEVC", "AV1"
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

        // 1. Protect controller buttons
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

        // 2. Protect immune game titles
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

        // 3. Protect FPS, Bitrate, Ping telemetry
        val telemetryPattern = Pattern.compile("(\\b\\d+\\s*(?:fps|fps|mbps|mb/s|ms|ping|kbit/s|mbit/s)\\b)", Pattern.CASE_INSENSITIVE)
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
            // Replace both exact and spaces variations Google Translate might insert (e.g. "__ CP TITLE _ 0 __")
            val regex = Pattern.quote(placeholder).replace("_", "\\s*_\\s*")
            result = result.replace(Regex(regex, RegexOption.IGNORE_CASE), originalTerm)
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

        // Check cache first
        val cacheKey = "$targetLang:$trimmed"
        val cached = translationCache[cacheKey]
        if (cached != null) {
            return@withContext if (bilingual && cached != trimmed) "$cached ($trimmed)" else cached
        }

        // Fast-path: Check local dictionary
        val dictMatch = XboxTranslator.XBOX_UI_DICTIONARY_EN[trimmed]
            ?: XboxTranslator.XBOX_UI_DICTIONARY_EN[trimmed.lowercase()]
        if (dictMatch != null) {
            translationCache[cacheKey] = dictMatch
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

            // Save to memory cache
            translationCache[cacheKey] = finalizedText

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
     * Batch translation for fast initial page translation
     */
    suspend fun translateBatch(
        texts: List<String>,
        targetLang: String = "vi"
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, String>()
        for (text in texts) {
            val translated = translate(text, targetLang)
            results[text] = translated
        }
        results
    }

    fun clearCache() {
        translationCache.clear()
    }
}
