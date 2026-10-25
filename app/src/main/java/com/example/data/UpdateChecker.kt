package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseUrl: String,
        val releaseNotes: String,
        val apkDirectUrl: String = "",
        val fastCdnApkUrl: String = "",
        val officialSha256: String = "",
        val sha256AssetUrl: String = ""
    ) : UpdateCheckState()
    data class CheckFailed(val currentVersion: String, val isManual: Boolean) : UpdateCheckState()
}

sealed class ApkDownloadState {
    data object Idle : ApkDownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val usingFallbackGitHub: Boolean
    ) : ApkDownloadState()
    data object Verifying : ApkDownloadState()
    data class VerifiedReadyToInstall(val apkFile: File) : ApkDownloadState()
    data class VerificationFailed(
        val messageVi: String = "File không khớp hoặc không kiểm tra được, vui lòng tải từ GitHub",
        val messageEn: String = "File mismatch or could not be verified, please download from GitHub"
    ) : ApkDownloadState()
    data class DownloadError(val messageVi: String, val messageEn: String) : ApkDownloadState()
}

object UpdateChecker {
    private const val PREFS_NAME = "cloudplay_update_checker_store"
    private const val KEY_LAST_CHECK_MS = "last_update_check_epoch_ms"
    private const val KEY_CACHED_LATEST_VER = "cached_latest_version"
    private const val KEY_CACHED_RELEASE_URL = "cached_release_url"
    private const val KEY_CACHED_RELEASE_NOTES = "cached_release_notes_v2"
    private const val KEY_CACHED_APK_DIRECT_URL = "cached_apk_direct_url"
    private const val KEY_CACHED_OFFICIAL_SHA256 = "cached_official_sha256"
    private const val KEY_CACHED_SHA256_ASSET_URL = "cached_sha256_asset_url"

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val TIMEOUT_MS = 8000
    private const val GHFAST_TIMEOUT_MS = 15000 // 15 giây cho ghfast.top trước khi tự chuyển sang GitHub
    private const val GITHUB_DOWNLOAD_TIMEOUT_MS = 20000
    private const val MAX_APK_SIZE_BYTES = 150L * 1024L * 1024L // Tối đa 150 MB
    private const val MAX_REDIRECT_HOPS = 5

    private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/muitenvn/CloudPlay-App/releases/latest"
    const val FALLBACK_RELEASES_URL = "https://github.com/muitenvn/CloudPlay-App/releases"
    private const val FAST_CDN_PREFIX = "https://ghfast.top/"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _downloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val downloadState: StateFlow<ApkDownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    @Volatile
    private var hasAutoCheckedInSession = false

    fun getCurrentVersionName(context: Context): String = try {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        normalizeVersionString(pkgInfo.versionName ?: "1.2.1")
    } catch (_: Exception) {
        "1.2.1"
    }

    fun normalizeVersionString(raw: String): String = raw.trim().removePrefix("v").removePrefix("V").trim()

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
        return clean.split(".").mapNotNull { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull()
        }
    }

    fun dismissUpdateDialog() {
        cancelApkDownload()
        _showUpdateDialog.value = false
    }

    fun openUpdateDialog() {
        if (_state.value is UpdateCheckState.UpdateAvailable) {
            _downloadState.value = ApkDownloadState.Idle
            _showUpdateDialog.value = true
        }
    }

    private fun buildFastCdnUrl(apkDirectUrl: String, latestVersion: String): String {
        val resolvedDirect = apkDirectUrl.ifBlank {
            "https://github.com/muitenvn/CloudPlay-App/releases/download/v$latestVersion/CloudPlay-Xbox-v$latestVersion.apk"
        }
        return if (resolvedDirect.startsWith("https://github.com/")) "$FAST_CDN_PREFIX$resolvedDirect" else resolvedDirect
    }

    fun isValidSha256Hex(raw: String): Boolean {
        val clean = raw.trim().lowercase()
        return clean.length == 64 && clean.all { it in '0'..'9' || it in 'a'..'f' }
    }

    private fun extractDigestSha256(digestField: String): String {
        val trimmed = digestField.trim()
        if (trimmed.startsWith("sha256:", ignoreCase = true)) {
            val hex = trimmed.substringAfter(":").trim().lowercase()
            if (isValidSha256Hex(hex)) return hex
        }
        return ""
    }

    /**
     * Chỉ cho phép HTTPS tới ghfast.top (chỉ khi tải APK), github.com, api.github.com và *.githubusercontent.com.
     */
    fun isAllowedHttpsUrl(url: URL, allowGhFast: Boolean): Boolean {
        if (!url.protocol.equals("https", ignoreCase = true)) return false
        val host = url.host?.lowercase()?.trim() ?: return false
        if (host == "github.com" || host == "api.github.com") return true
        if (host == "objects.githubusercontent.com" ||
            host == "release-assets.githubusercontent.com" ||
            host.endsWith(".githubusercontent.com")
        ) return true
        if (allowGhFast && (host == "ghfast.top" || host.endsWith(".ghfast.top"))) return true
        return false
    }

    /**
     * Mở kết nối trực tiếp (Proxy.NO_PROXY, không đi qua VPN/Proxy của app) và kiểm tra mọi hop chuyển hướng.
     */
    private fun openVerifiedDirectConnection(
        initialUrlString: String,
        allowGhFast: Boolean,
        timeoutMs: Int,
        acceptHeader: String = "*/*"
    ): HttpURLConnection {
        var currentUrl = URL(initialUrlString)
        if (!isAllowedHttpsUrl(currentUrl, allowGhFast)) {
            throw SecurityException("Blocked disallowed URL: $initialUrlString")
        }
        var redirects = 0
        while (redirects <= MAX_REDIRECT_HOPS) {
            val conn = (currentUrl.openConnection(Proxy.NO_PROXY) as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = false
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("User-Agent", "CloudPlay-Android")
            }
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")?.trim().orEmpty()
                conn.disconnect()
                if (location.isBlank()) throw SecurityException("Redirect missing Location")
                val nextUrl = URL(currentUrl, location)
                if (!isAllowedHttpsUrl(nextUrl, allowGhFast)) {
                    throw SecurityException("Blocked redirect host: ${nextUrl.host}")
                }
                currentUrl = nextUrl
                redirects++
                continue
            }
            return conn
        }
        throw SecurityException("Too many redirects")
    }

    private fun fetchSha256FromGitHubAsset(sha256AssetUrl: String): String {
        if (sha256AssetUrl.isBlank() || !sha256AssetUrl.startsWith("https://github.com/")) return ""
        var conn: HttpURLConnection? = null
        return try {
            conn = openVerifiedDirectConnection(sha256AssetUrl, allowGhFast = false, timeoutMs = TIMEOUT_MS)
            if (conn.responseCode !in 200..299) return ""
            val content = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                val buf = CharArray(256)
                val read = reader.read(buf)
                if (read > 0) String(buf, 0, read) else ""
            }
            val hex = Regex("\\b([0-9a-fA-F]{64})\\b").find(content)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
            if (isValidSha256Hex(hex)) hex else ""
        } catch (_: Throwable) {
            ""
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    fun checkOnAppLaunchIfNeeded(context: Context) {
        if (hasAutoCheckedInSession) return
        if (!SettingsRepository.config.value.autoCheckUpdatesOnLaunch) return

        hasAutoCheckedInSession = true
        val appContext = context.applicationContext
        val currentVersion = getCurrentVersionName(appContext)
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckMs = sp.getLong(KEY_LAST_CHECK_MS, 0L)
        val now = System.currentTimeMillis()

        val cachedNotes = sp.getString(KEY_CACHED_RELEASE_NOTES, "") ?: ""
        val cachedApkDirect = sp.getString(KEY_CACHED_APK_DIRECT_URL, "") ?: ""
        val cachedOfficialSha = sp.getString(KEY_CACHED_OFFICIAL_SHA256, "") ?: ""
        val cachedShaAssetUrl = sp.getString(KEY_CACHED_SHA256_ASSET_URL, "") ?: ""
        if (now - lastCheckMs in 1 until CHECK_INTERVAL_MS && cachedNotes.isNotBlank() && cachedApkDirect.isNotBlank()) {
            val cachedLatest = sp.getString(KEY_CACHED_LATEST_VER, "") ?: ""
            val cachedUrl = sp.getString(KEY_CACHED_RELEASE_URL, FALLBACK_RELEASES_URL) ?: FALLBACK_RELEASES_URL
            if (cachedLatest.isNotBlank() && isNewerVersion(currentVersion, cachedLatest)) {
                _state.value = UpdateCheckState.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = cachedLatest,
                    releaseUrl = cachedUrl,
                    releaseNotes = cachedNotes,
                    apkDirectUrl = cachedApkDirect,
                    fastCdnApkUrl = buildFastCdnUrl(cachedApkDirect, cachedLatest),
                    officialSha256 = cachedOfficialSha,
                    sha256AssetUrl = cachedShaAssetUrl
                )
                _showUpdateDialog.value = true
            }
            return
        }
        scope.launch { performCheck(appContext, isManual = false) }
    }

    fun checkNowManual(context: Context) {
        if (_state.value is UpdateCheckState.Checking) return
        val appContext = context.applicationContext
        scope.launch { performCheck(appContext, isManual = true) }
    }

    private fun performCheck(context: Context, isManual: Boolean) {
        val currentVersion = getCurrentVersionName(context)
        if (isManual) _state.value = UpdateCheckState.Checking

        var connection: HttpURLConnection? = null
        try {
            connection = openVerifiedDirectConnection(
                initialUrlString = LATEST_RELEASE_API_URL,
                allowGhFast = false,
                timeoutMs = TIMEOUT_MS,
                acceptHeader = "application/vnd.github+json"
            )
            if (connection.responseCode !in 200..299) {
                handleCheckFailure(currentVersion, isManual)
                return
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(responseBody)
            val latestVersion = normalizeVersionString(json.optString("tag_name", ""))
            if (latestVersion.isBlank() || parseNumericParts(latestVersion).isEmpty()) {
                handleCheckFailure(currentVersion, isManual)
                return
            }

            val rawHtmlUrl = json.optString("html_url", "").trim()
            val releaseUrl = if (rawHtmlUrl.startsWith("https://github.com/")) rawHtmlUrl else FALLBACK_RELEASES_URL

            var apkDirectUrl = ""
            var apkAssetName = ""
            var officialSha256 = ""
            var sha256AssetUrl = ""

            val assetsArr = json.optJSONArray("assets")
            if (assetsArr != null) {
                for (i in 0 until assetsArr.length()) {
                    val assetObj = assetsArr.optJSONObject(i) ?: continue
                    val name = assetObj.optString("name", "").trim()
                    val dlUrl = assetObj.optString("browser_download_url", "").trim()
                    val digest = assetObj.optString("digest", "").trim()
                    if (name.endsWith(".apk", ignoreCase = true) && dlUrl.startsWith("https://github.com/")) {
                        if (name.contains("CloudPlay-Xbox", ignoreCase = true) || apkDirectUrl.isBlank()) {
                            apkDirectUrl = dlUrl
                            apkAssetName = name
                            val extracted = extractDigestSha256(digest)
                            if (extracted.isNotBlank()) officialSha256 = extracted
                        }
                    }
                }
                for (i in 0 until assetsArr.length()) {
                    val assetObj = assetsArr.optJSONObject(i) ?: continue
                    val name = assetObj.optString("name", "").trim()
                    val dlUrl = assetObj.optString("browser_download_url", "").trim()
                    if (name.endsWith(".sha256", ignoreCase = true) && dlUrl.startsWith("https://github.com/")) {
                        if (apkAssetName.isNotBlank() && name.equals("$apkAssetName.sha256", ignoreCase = true)) {
                            sha256AssetUrl = dlUrl
                            break
                        } else if (sha256AssetUrl.isBlank()) {
                            sha256AssetUrl = dlUrl
                        }
                    }
                }
            }

            if (apkDirectUrl.isBlank()) {
                apkDirectUrl = "https://github.com/muitenvn/CloudPlay-App/releases/download/v$latestVersion/CloudPlay-Xbox-v$latestVersion.apk"
            }
            if (sha256AssetUrl.isBlank()) sha256AssetUrl = "$apkDirectUrl.sha256"
            if (!isValidSha256Hex(officialSha256)) {
                officialSha256 = fetchSha256FromGitHubAsset(sha256AssetUrl)
            }

            val fastCdnUrl = buildFastCdnUrl(apkDirectUrl, latestVersion)
            val cleanNotes = sanitizeReleaseNotes(json.optString("body", "").trim())

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
                .putString(KEY_CACHED_LATEST_VER, latestVersion)
                .putString(KEY_CACHED_RELEASE_URL, releaseUrl)
                .putString(KEY_CACHED_RELEASE_NOTES, cleanNotes)
                .putString(KEY_CACHED_APK_DIRECT_URL, apkDirectUrl)
                .putString(KEY_CACHED_OFFICIAL_SHA256, officialSha256)
                .putString(KEY_CACHED_SHA256_ASSET_URL, sha256AssetUrl)
                .apply()

            if (isNewerVersion(currentVersion, latestVersion)) {
                _state.value = UpdateCheckState.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    releaseUrl = releaseUrl,
                    releaseNotes = cleanNotes,
                    apkDirectUrl = apkDirectUrl,
                    fastCdnApkUrl = fastCdnUrl,
                    officialSha256 = officialSha256,
                    sha256AssetUrl = sha256AssetUrl
                )
                _showUpdateDialog.value = true
            } else {
                _state.value = UpdateCheckState.UpToDate(currentVersion = currentVersion)
            }
        } catch (_: Throwable) {
            handleCheckFailure(currentVersion, isManual)
        } finally {
            try { connection?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun ensureOfficialSha256FromGitHub(updateInfo: UpdateCheckState.UpdateAvailable): String {
        if (isValidSha256Hex(updateInfo.officialSha256)) return updateInfo.officialSha256.lowercase()
        var conn: HttpURLConnection? = null
        try {
            conn = openVerifiedDirectConnection(
                initialUrlString = LATEST_RELEASE_API_URL,
                allowGhFast = false,
                timeoutMs = TIMEOUT_MS,
                acceptHeader = "application/vnd.github+json"
            )
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                val assetsArr = JSONObject(body).optJSONArray("assets")
                var fallbackShaUrl = updateInfo.sha256AssetUrl
                if (assetsArr != null) {
                    for (i in 0 until assetsArr.length()) {
                        val assetObj = assetsArr.optJSONObject(i) ?: continue
                        val name = assetObj.optString("name", "").trim()
                        val dlUrl = assetObj.optString("browser_download_url", "").trim()
                        val digest = assetObj.optString("digest", "").trim()
                        if (name.endsWith(".apk", ignoreCase = true) && dlUrl.startsWith("https://github.com/")) {
                            val extracted = extractDigestSha256(digest)
                            if (isValidSha256Hex(extracted)) return extracted
                        } else if (name.endsWith(".sha256", ignoreCase = true) && dlUrl.startsWith("https://github.com/")) {
                            fallbackShaUrl = dlUrl
                        }
                    }
                }
                if (fallbackShaUrl.isNotBlank()) {
                    val fromAsset = fetchSha256FromGitHubAsset(fallbackShaUrl)
                    if (isValidSha256Hex(fromAsset)) return fromAsset
                }
            }
        } catch (_: Throwable) {
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
        if (updateInfo.sha256AssetUrl.isNotBlank()) {
            val fromAsset = fetchSha256FromGitHubAsset(updateInfo.sha256AssetUrl)
            if (isValidSha256Hex(fromAsset)) return fromAsset
        }
        return ""
    }

    fun startVerifiedApkDownload(context: Context, updateInfo: UpdateCheckState.UpdateAvailable) {
        if (_downloadState.value is ApkDownloadState.Downloading || _downloadState.value is ApkDownloadState.Verifying) return
        val appContext = context.applicationContext
        val updatesDir = File(appContext.cacheDir, "updates").apply { if (!exists()) mkdirs() }
        val targetApkFile = File(updatesDir, "CloudPlay-Xbox-v${updateInfo.latestVersion}.apk")

        downloadJob?.cancel()
        downloadJob = scope.launch {
            try {
                _downloadState.value = ApkDownloadState.Downloading(0f, 0L, -1L, usingFallbackGitHub = false)
                val fastUrl = updateInfo.fastCdnApkUrl.ifBlank { buildFastCdnUrl(updateInfo.apkDirectUrl, updateInfo.latestVersion) }
                val directGitHubUrl = updateInfo.apkDirectUrl.ifBlank {
                    "https://github.com/muitenvn/CloudPlay-App/releases/download/v${updateInfo.latestVersion}/CloudPlay-Xbox-v${updateInfo.latestVersion}.apk"
                }

                var downloadedOk = false
                try {
                    downloadStreamToFile(fastUrl, targetApkFile, GHFAST_TIMEOUT_MS, usingFallbackGitHub = false)
                    downloadedOk = true
                } catch (ce: CancellationException) {
                    targetApkFile.delete()
                    throw ce
                } catch (_: Throwable) {
                    targetApkFile.delete()
                }

                if (!downloadedOk) {
                    _downloadState.value = ApkDownloadState.Downloading(0f, 0L, -1L, usingFallbackGitHub = true)
                    downloadStreamToFile(directGitHubUrl, targetApkFile, GITHUB_DOWNLOAD_TIMEOUT_MS, usingFallbackGitHub = true)
                }

                _downloadState.value = ApkDownloadState.Verifying
                val officialSha256 = ensureOfficialSha256FromGitHub(updateInfo)
                val computedSha256 = computeFileSha256(targetApkFile)

                if (!isExactSha256Match(computedSha256, officialSha256)) {
                    targetApkFile.delete()
                    _downloadState.value = ApkDownloadState.VerificationFailed()
                    return@launch
                }

                _downloadState.value = ApkDownloadState.VerifiedReadyToInstall(targetApkFile)
                launchApkInstaller(appContext, targetApkFile)
            } catch (_: CancellationException) {
                targetApkFile.delete()
                _downloadState.value = ApkDownloadState.Idle
            } catch (_: SecurityException) {
                targetApkFile.delete()
                _downloadState.value = ApkDownloadState.VerificationFailed()
            } catch (_: Throwable) {
                targetApkFile.delete()
                _downloadState.value = ApkDownloadState.DownloadError(
                    messageVi = "Không thể tải bản cập nhật lúc này, vui lòng tải từ GitHub.",
                    messageEn = "Could not download update right now, please download from GitHub."
                )
            }
        }
    }

    fun cancelApkDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = ApkDownloadState.Idle
    }

    private suspend fun downloadStreamToFile(
        downloadUrl: String,
        destFile: File,
        timeoutMs: Int,
        usingFallbackGitHub: Boolean
    ) {
        var conn: HttpURLConnection? = null
        try {
            conn = openVerifiedDirectConnection(downloadUrl, allowGhFast = true, timeoutMs = timeoutMs)
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            val contentLength = conn.contentLengthLong
            if (contentLength > MAX_APK_SIZE_BYTES) throw SecurityException("APK exceeds 150 MB")

            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var totalRead = 0L
                    while (coroutineContext.isActive) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        if (read == 0) continue
                        totalRead += read
                        if (totalRead > MAX_APK_SIZE_BYTES) throw SecurityException("Stream exceeds 150 MB")
                        output.write(buffer, 0, read)
                        val progress = if (contentLength > 0L) (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else -1f
                        _downloadState.value = ApkDownloadState.Downloading(progress, totalRead, contentLength, usingFallbackGitHub)
                    }
                    if (!coroutineContext.isActive) throw CancellationException("Cancelled")
                    output.flush()
                    if (totalRead <= 0L) throw IllegalStateException("Empty APK")
                }
            }
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun computeFileSha256(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = fis.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { String.format("%02x", it) }.lowercase()
    }

    fun isExactSha256Match(computed: String, official: String): Boolean {
        val c = computed.trim().lowercase()
        val o = official.trim().lowercase()
        if (!isValidSha256Hex(c) || !isValidSha256Hex(o) || c.length != o.length) return false
        for (i in c.indices) {
            if (c[i] != o[i]) return false
        }
        return true
    }

    fun launchApkInstaller(context: Context, apkFile: File) {
        if (!apkFile.exists()) return
        try {
            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (_: Throwable) {}
    }

    private fun handleCheckFailure(currentVersion: String, isManual: Boolean) {
        if (isManual) {
            _state.value = UpdateCheckState.CheckFailed(currentVersion = currentVersion, isManual = true)
        } else if (_state.value is UpdateCheckState.Checking) {
            _state.value = UpdateCheckState.Idle
        }
    }

    private fun sanitizeReleaseNotes(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.lineSequence()
            .map { line ->
                line.trim().removePrefix("###").removePrefix("##").removePrefix("#").removePrefix(">")
                    .replace("**", "").replace("`", "").trim()
            }
            .filter { it.isNotBlank() && !it.startsWith("---") }
            .joinToString("\n")
    }
}
