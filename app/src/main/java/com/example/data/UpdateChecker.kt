package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseUrl: String,
        val releaseNotes: String
    ) : UpdateCheckState()
    data class CheckFailed(
        val currentVersion: String,
        val isManual: Boolean
    ) : UpdateCheckState()
}

object UpdateChecker {
    private const val PREFS_NAME = "cloudplay_update_checker_store"
    private const val KEY_LAST_CHECK_MS = "last_update_check_epoch_ms"
    private const val KEY_CACHED_LATEST_VER = "cached_latest_version"
    private const val KEY_CACHED_RELEASE_URL = "cached_release_url"
    private const val KEY_CACHED_RELEASE_NOTES = "cached_release_notes_v2"

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L // 24 giờ
    private const val TIMEOUT_MS = 8000 // 8 giây
    private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/muitenvn/CloudPlay-App/releases/latest"
    private const val FALLBACK_RELEASES_URL = "https://github.com/muitenvn/CloudPlay-App/releases/latest"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    @Volatile
    private var hasAutoCheckedInSession = false

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            normalizeVersionString(pkgInfo.versionName ?: "1.2.1")
        } catch (_: Exception) {
            "1.2.1"
        }
    }

    /**
     * Bỏ chữ "v" hoặc "V" ở đầu và khoảng trắng thừa.
     */
    fun normalizeVersionString(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V").trim()
    }

    /**
     * So sánh 2 chuỗi phiên bản theo từng cấp số học (ví dụ: 1.2.9 < 1.2.10).
     * Trả về true nếu [latest] lớn hơn [current].
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = parseNumericParts(current)
        val latestParts = parseNumericParts(latest)
        if (latestParts.isEmpty()) return false

        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val curVal = currentParts.getOrElse(i) { 0 }
            val latVal = latestParts.getOrElse(i) { 0 }
            if (latVal > curVal) return true
            if (latVal < curVal) return false
        }
        return false
    }

    private fun parseNumericParts(version: String): List<Int> {
        val clean = normalizeVersionString(version)
        if (clean.isBlank()) return emptyList()
        return clean.split(".")
            .mapNotNull { segment ->
                val digits = segment.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun openUpdateDialog() {
        if (_state.value is UpdateCheckState.UpdateAvailable) {
            _showUpdateDialog.value = true
        }
    }

    /**
     * Kiểm tra tự động khi mở ứng dụng (tối đa 1 lần mỗi 24 giờ).
     * Nếu chưa đủ 24 giờ kể từ lần kiểm tra trước nhưng có sẵn bản mới hơn trong bộ nhớ đệm,
     * vẫn hiển thị thông báo cho người dùng mà không gọi lại API.
     */
    fun checkOnAppLaunchIfNeeded(context: Context) {
        if (hasAutoCheckedInSession) return
        val config = SettingsRepository.config.value
        if (!config.autoCheckUpdatesOnLaunch) return

        hasAutoCheckedInSession = true
        val appContext = context.applicationContext
        val currentVersion = getCurrentVersionName(appContext)
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckMs = sp.getLong(KEY_LAST_CHECK_MS, 0L)
        val now = System.currentTimeMillis()

        val cachedNotes = sp.getString(KEY_CACHED_RELEASE_NOTES, "") ?: ""
        if (now - lastCheckMs in 1 until CHECK_INTERVAL_MS && cachedNotes.isNotBlank()) {
            val cachedLatest = sp.getString(KEY_CACHED_LATEST_VER, "") ?: ""
            val cachedUrl = sp.getString(KEY_CACHED_RELEASE_URL, FALLBACK_RELEASES_URL) ?: FALLBACK_RELEASES_URL
            if (cachedLatest.isNotBlank() && isNewerVersion(currentVersion, cachedLatest)) {
                _state.value = UpdateCheckState.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = cachedLatest,
                    releaseUrl = cachedUrl,
                    releaseNotes = cachedNotes
                )
                _showUpdateDialog.value = true
            }
            return
        }

        scope.launch {
            performCheck(appContext, isManual = false)
        }
    }

    /**
     * Kiểm tra ngay lập tức khi người dùng bấm nút "Kiểm tra ngay" trong Cài đặt.
     */
    fun checkNowManual(context: Context) {
        if (_state.value is UpdateCheckState.Checking) return
        val appContext = context.applicationContext
        scope.launch {
            performCheck(appContext, isManual = true)
        }
    }

    private fun performCheck(context: Context, isManual: Boolean) {
        val currentVersion = getCurrentVersionName(context)
        if (isManual) {
            _state.value = UpdateCheckState.Checking
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(LATEST_RELEASE_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "CloudPlay-Android")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                handleCheckFailure(currentVersion, isManual)
                return
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }

            val json = JSONObject(responseBody)
            val rawTag = json.optString("tag_name", "").trim()
            val latestVersion = normalizeVersionString(rawTag)
            if (latestVersion.isBlank() || parseNumericParts(latestVersion).isEmpty()) {
                handleCheckFailure(currentVersion, isManual)
                return
            }

            val rawHtmlUrl = json.optString("html_url", "").trim()
            val releaseUrl = if (rawHtmlUrl.startsWith("https://github.com/")) {
                rawHtmlUrl
            } else {
                FALLBACK_RELEASES_URL
            }

            val rawNotes = json.optString("body", "").trim()
            val cleanNotes = sanitizeReleaseNotes(rawNotes)

            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
                .putString(KEY_CACHED_LATEST_VER, latestVersion)
                .putString(KEY_CACHED_RELEASE_URL, releaseUrl)
                .putString(KEY_CACHED_RELEASE_NOTES, cleanNotes)
                .apply()

            if (isNewerVersion(currentVersion, latestVersion)) {
                _state.value = UpdateCheckState.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    releaseUrl = releaseUrl,
                    releaseNotes = cleanNotes
                )
                _showUpdateDialog.value = true
            } else {
                _state.value = UpdateCheckState.UpToDate(currentVersion = currentVersion)
            }
        } catch (_: Throwable) {
            handleCheckFailure(currentVersion, isManual)
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Throwable) {
            }
        }
    }

    private fun handleCheckFailure(currentVersion: String, isManual: Boolean) {
        if (isManual) {
            _state.value = UpdateCheckState.CheckFailed(
                currentVersion = currentVersion,
                isManual = true
            )
        } else {
            // Lỗi mạng hoặc giới hạn API khi kiểm tra ngầm: giữ im lặng hoàn toàn
            if (_state.value is UpdateCheckState.Checking) {
                _state.value = UpdateCheckState.Idle
            }
        }
    }

    private fun sanitizeReleaseNotes(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("###")
                    .removePrefix("##")
                    .removePrefix("#")
                    .removePrefix(">")
                    .replace("**", "")
                    .replace("`", "")
                    .trim()
            }
            .filter { it.isNotBlank() && !it.startsWith("---") }
            .joinToString("\n")
    }
}
