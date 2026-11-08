package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

data class ApkMirrorNode(
    val id: String,
    val name: String,
    val host: String,
    val isDirectGitHub: Boolean = false
) {
    fun buildDownloadUrl(directGitHubUrl: String): String {
        val cleanDirect = directGitHubUrl.trim()
        if (!cleanDirect.startsWith("https://github.com/")) return cleanDirect
        return when (id) {
            "gh_proxy_com" -> "https://gh-proxy.com/$cleanDirect"
            "ghproxy_net" -> "https://ghproxy.net/$cleanDirect"
            "gh_llkk_cc" -> "https://gh.llkk.cc/$cleanDirect"
            "ghproxy_cc" -> "https://ghproxy.cc/$cleanDirect"
            "gh_ddlc_top" -> "https://gh.ddlc.top/$cleanDirect"
            "ghfast_top" -> "https://ghfast.top/$cleanDirect"
            "mirror_ghproxy" -> "https://mirror.ghproxy.com/$cleanDirect"
            "kkgithub" -> cleanDirect.replaceFirst("https://github.com/", "https://kkgithub.com/")
            "gh_con_sh" -> "https://gh.con.sh/$cleanDirect"
            "github_moeyy" -> "https://github.moeyy.xyz/$cleanDirect"
            else -> cleanDirect
        }
    }
}

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
        val usingFallbackGitHub: Boolean,
        val activeMirrorName: String = "",
        val mirrorIndex: Int = 1,
        val totalMirrors: Int = 1,
        val failedMirrors: List<String> = emptyList()
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
    private const val KEY_CACHED_ETAG = "cached_github_api_etag"
    private const val KEY_LAST_WORKING_MIRROR_ID = "last_working_mirror_id"

    private const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val MANUAL_RAPID_TAP_REUSE_MS = 15_000L
    private const val TIMEOUT_MS = 8000
    private const val MIRROR_CONNECT_TIMEOUT_MS = 6000 // Chuyển sang máy chủ phụ kế tiếp nhanh nếu máy chủ hiện tại lỗi/treo
    private const val MIRROR_READ_TIMEOUT_MS = 12000
    private const val GITHUB_DOWNLOAD_TIMEOUT_MS = 20000
    private const val MAX_APK_SIZE_BYTES = 150L * 1024L * 1024L // Tối đa 150 MB
    private const val MIN_VALID_APK_BYTES = 64L * 1024L // Chặn trang báo lỗi HTML giả dạng 200 OK
    private const val MAX_REDIRECT_HOPS = 8
    private const val BROWSER_COMPAT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/muitenvn/CloudPlay-App/releases/latest"
    private const val LATEST_RELEASE_WEB_URL = "https://github.com/muitenvn/CloudPlay-App/releases/latest"
    const val FALLBACK_RELEASES_URL = "https://github.com/muitenvn/CloudPlay-App/releases"
    private const val FAST_CDN_PREFIX = "https://gh-proxy.com/"

    /**
     * Danh sách 10 cổng máy chủ phụ (GitHub Release Proxy/CDN) + 1 cổng GitHub gốc trực tiếp.
     * Khi một cổng bị lỗi kết nối, quá thời gian chờ, bị chặn chuyển hướng, trả mã lỗi HTTP,
     * trả trang HTML thay vì file APK, hoặc sai mã kiểm tra, hệ thống sẽ tự động chuyển sang máy chủ kế tiếp.
     */
    val FAST_MIRROR_NODES: List<ApkMirrorNode> = listOf(
        ApkMirrorNode(id = "gh_proxy_com", name = "gh-proxy.com", host = "gh-proxy.com"),
        ApkMirrorNode(id = "ghproxy_net", name = "ghproxy.net", host = "ghproxy.net"),
        ApkMirrorNode(id = "gh_llkk_cc", name = "gh.llkk.cc", host = "gh.llkk.cc"),
        ApkMirrorNode(id = "ghproxy_cc", name = "ghproxy.cc", host = "ghproxy.cc"),
        ApkMirrorNode(id = "gh_ddlc_top", name = "gh.ddlc.top", host = "gh.ddlc.top"),
        ApkMirrorNode(id = "ghfast_top", name = "ghfast.top", host = "ghfast.top"),
        ApkMirrorNode(id = "mirror_ghproxy", name = "mirror.ghproxy.com", host = "mirror.ghproxy.com"),
        ApkMirrorNode(id = "kkgithub", name = "kkgithub.com", host = "kkgithub.com"),
        ApkMirrorNode(id = "gh_con_sh", name = "gh.con.sh", host = "gh.con.sh"),
        ApkMirrorNode(id = "github_moeyy", name = "github.moeyy.xyz", host = "github.moeyy.xyz"),
        ApkMirrorNode(id = "github_direct", name = "GitHub Direct", host = "github.com", isDirectGitHub = true)
    )

    val ALLOWED_MIRROR_HOSTS: Set<String> = FAST_MIRROR_NODES
        .filter { !it.isDirectGitHub }
        .map { it.host.lowercase() }
        .toSet()

    private val ALLOWED_CDN_REDIRECT_SUFFIXES: List<String> = listOf(
        ".githubusercontent.com",
        ".amazonaws.com",
        ".r2.cloudflarestorage.com",
        ".cloudflare.com",
        ".workers.dev",
        ".pages.dev",
        ".fastly.net",
        ".akamaized.net",
        ".jsdelivr.net",
        ".gh-proxy.com",
        ".ghproxy.net",
        ".ghproxy.com",
        ".ghproxy.cc",
        ".llkk.cc",
        ".ddlc.top",
        ".ghfast.top",
        ".kkgithub.com",
        ".con.sh",
        ".moeyy.xyz"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isChecking = AtomicBoolean(false)

    @Volatile
    private var lastSuccessfulCheckRealtimeMs: Long = 0L

    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _downloadState = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Idle)
    val downloadState: StateFlow<ApkDownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    @Volatile
    private var skipCurrentMirrorRequested = false

    @Volatile
    private var activeDownloadConnection: HttpURLConnection? = null

    @Volatile
    private var hasAutoCheckedInSession = false

    fun getCurrentVersionName(context: Context): String = try {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        normalizeVersionString(pkgInfo.versionName ?: "1.3.0")
    } catch (_: Exception) {
        "1.3.0"
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

    /**
     * Xây dựng danh sách thứ tự các cổng tải sẽ thử:
     * - Nếu người dùng chọn 1 cổng cụ thể (preferredMirrorId != "auto"), thử cổng đó đầu tiên (kể cả github_direct khi bấm "Tải thường")
     *   rồi mới tự động xoay sang các cổng còn lại.
     * - Nếu để "auto" (bấm "Tải qua máy chủ phụ"), ưu tiên cổng máy chủ phụ vừa tải thành công lần gần nhất (nếu có),
     *   sau đó lần lượt thử toàn bộ chuỗi FAST_MIRROR_NODES và cuối cùng là GitHub Direct.
     */
    fun buildOrderedMirrorChain(
        directGitHubUrl: String,
        preferredMirrorId: String = "auto",
        lastWorkingMirrorId: String = ""
    ): List<Pair<ApkMirrorNode, String>> {
        val orderedNodes = mutableListOf<ApkMirrorNode>()
        if (preferredMirrorId.isNotBlank() && !preferredMirrorId.equals("auto", ignoreCase = true)) {
            FAST_MIRROR_NODES.firstOrNull { it.id == preferredMirrorId }?.let { orderedNodes.add(it) }
            if (lastWorkingMirrorId.isNotBlank() && lastWorkingMirrorId != preferredMirrorId) {
                FAST_MIRROR_NODES.firstOrNull { it.id == lastWorkingMirrorId && !it.isDirectGitHub }?.let { orderedNodes.add(it) }
            }
        } else if (lastWorkingMirrorId.isNotBlank()) {
            FAST_MIRROR_NODES.firstOrNull { it.id == lastWorkingMirrorId && !it.isDirectGitHub }?.let { orderedNodes.add(it) }
        }
        FAST_MIRROR_NODES.forEach { node ->
            if (orderedNodes.none { it.id == node.id }) {
                orderedNodes.add(node)
            }
        }
        return orderedNodes.map { node -> node to node.buildDownloadUrl(directGitHubUrl) }
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
     * Chỉ cho phép HTTPS tới các cổng mirror trong ALLOWED_MIRROR_HOSTS / CDN chuyển hướng (chỉ khi tải APK),
     * github.com, api.github.com, *.githubusercontent.com và *.amazonaws.com (S3 release assets của GitHub).
     */
    fun isAllowedHttpsUrl(url: URL, allowGhFast: Boolean): Boolean {
        if (!url.protocol.equals("https", ignoreCase = true)) return false
        val host = url.host?.lowercase()?.trim() ?: return false
        if (host == "github.com" || host == "api.github.com") return true
        if (host == "objects.githubusercontent.com" ||
            host == "release-assets.githubusercontent.com" ||
            host.endsWith(".githubusercontent.com") ||
            host.endsWith(".amazonaws.com")
        ) return true
        if (allowGhFast) {
            if (ALLOWED_MIRROR_HOSTS.any { mirrorHost -> host == mirrorHost || host.endsWith(".$mirrorHost") }) {
                return true
            }
            if (ALLOWED_CDN_REDIRECT_SUFFIXES.any { suffix -> host.endsWith(suffix) }) {
                return true
            }
        }
        return false
    }

    private fun isSafeRedirectHttpsTarget(url: URL, allowGhFast: Boolean): Boolean {
        if (isAllowedHttpsUrl(url, allowGhFast)) return true
        if (!allowGhFast) return false
        if (!url.protocol.equals("https", ignoreCase = true)) return false
        val host = url.host?.lowercase()?.trim() ?: return false
        if (host.isBlank() || host == "localhost" || host.endsWith(".local") || host.endsWith(".internal")) return false
        if (host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")) return false
        return host.contains(".")
    }

    /**
     * Mở kết nối trực tiếp (Proxy.NO_PROXY, không đi qua VPN/Proxy của app) và kiểm tra mọi hop chuyển hướng.
     */
    private fun openVerifiedDirectConnection(
        initialUrlString: String,
        allowGhFast: Boolean,
        timeoutMs: Int,
        readTimeoutMs: Int = timeoutMs,
        acceptHeader: String = "*/*",
        ifNoneMatchEtag: String = ""
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
                readTimeout = readTimeoutMs
                useCaches = false
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty(
                    "User-Agent",
                    if (allowGhFast) BROWSER_COMPAT_USER_AGENT else "CloudPlay-Android"
                )
                if (ifNoneMatchEtag.isNotBlank() && redirects == 0) {
                    setRequestProperty("If-None-Match", ifNoneMatchEtag)
                }
            }
            activeDownloadConnection = conn
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")?.trim().orEmpty()
                conn.disconnect()
                if (location.isBlank()) throw IllegalStateException("Redirect missing Location from ${currentUrl.host}")
                val nextUrl = URL(currentUrl, location)
                if (!isSafeRedirectHttpsTarget(nextUrl, allowGhFast)) {
                    throw IllegalStateException("Blocked unsafe redirect host: ${nextUrl.host}")
                }
                currentUrl = nextUrl
                redirects++
                continue
            }
            return conn
        }
        throw IllegalStateException("Too many redirects (> $MAX_REDIRECT_HOPS)")
    }

    /**
     * Dự phòng khi api.github.com bị giới hạn tần suất (HTTP 403/429 khi bấm liên tục):
     * kiểm tra qua URL web github.com/.../releases/latest (chuyển hướng 302 tới /releases/tag/vX.Y.Z,
     * không bị giới hạn 60 request/giờ của REST API).
     */
    private fun fetchLatestVersionViaWebRedirect(): String {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(LATEST_RELEASE_WEB_URL)
            conn = (url.openConnection(Proxy.NO_PROXY) as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "HEAD"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("User-Agent", "CloudPlay-Android")
            }
            val code = conn.responseCode
            val location = conn.getHeaderField("Location")?.trim().orEmpty()
            if (code in listOf(301, 302, 303, 307, 308) && location.contains("/releases/tag/")) {
                val rawTag = location.substringAfter("/releases/tag/").substringBefore("?").substringBefore("#").trim()
                val clean = normalizeVersionString(rawTag)
                if (parseNumericParts(clean).isNotEmpty()) clean else ""
            } else {
                ""
            }
        } catch (_: Throwable) {
            ""
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
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

    private fun applyCachedOrResolvedVersionState(
        context: Context,
        currentVersion: String,
        resolvedLatestVersion: String,
        openDialogIfNewer: Boolean
    ): Boolean {
        val cleanLatest = normalizeVersionString(resolvedLatestVersion)
        if (cleanLatest.isBlank() || parseNumericParts(cleanLatest).isEmpty()) return false
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedLatest = sp.getString(KEY_CACHED_LATEST_VER, "") ?: ""
        val sameAsCached = cachedLatest.equals(cleanLatest, ignoreCase = true)

        val releaseUrl = if (sameAsCached) {
            sp.getString(KEY_CACHED_RELEASE_URL, FALLBACK_RELEASES_URL) ?: FALLBACK_RELEASES_URL
        } else {
            "https://github.com/muitenvn/CloudPlay-App/releases/tag/v$cleanLatest"
        }
        val releaseNotes = if (sameAsCached) sp.getString(KEY_CACHED_RELEASE_NOTES, "") ?: "" else ""
        val apkDirectUrl = if (sameAsCached) {
            (sp.getString(KEY_CACHED_APK_DIRECT_URL, "") ?: "").ifBlank {
                "https://github.com/muitenvn/CloudPlay-App/releases/download/v$cleanLatest/CloudPlay-Xbox-v$cleanLatest.apk"
            }
        } else {
            "https://github.com/muitenvn/CloudPlay-App/releases/download/v$cleanLatest/CloudPlay-Xbox-v$cleanLatest.apk"
        }
        val officialSha = if (sameAsCached) sp.getString(KEY_CACHED_OFFICIAL_SHA256, "") ?: "" else ""
        val shaAssetUrl = if (sameAsCached) {
            (sp.getString(KEY_CACHED_SHA256_ASSET_URL, "") ?: "").ifBlank { "$apkDirectUrl.sha256" }
        } else {
            "$apkDirectUrl.sha256"
        }

        lastSuccessfulCheckRealtimeMs = System.currentTimeMillis()
        if (isNewerVersion(currentVersion, cleanLatest)) {
            _state.value = UpdateCheckState.UpdateAvailable(
                currentVersion = currentVersion,
                latestVersion = cleanLatest,
                releaseUrl = releaseUrl,
                releaseNotes = releaseNotes,
                apkDirectUrl = apkDirectUrl,
                fastCdnApkUrl = buildFastCdnUrl(apkDirectUrl, cleanLatest),
                officialSha256 = officialSha,
                sha256AssetUrl = shaAssetUrl
            )
            if (openDialogIfNewer) _showUpdateDialog.value = true
        } else {
            _state.value = UpdateCheckState.UpToDate(currentVersion = currentVersion)
        }
        return true
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
        if (!isChecking.compareAndSet(false, true)) return
        scope.launch {
            try {
                performCheck(appContext, isManual = false)
            } finally {
                isChecking.set(false)
            }
        }
    }

    fun checkNowManual(context: Context) {
        if (!isChecking.compareAndSet(false, true)) return
        AppLogger.info("UI", "Bấm kiểm tra cập nhật ứng dụng thủ công")
        _state.value = UpdateCheckState.Checking
        val appContext = context.applicationContext
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val cachedLatest = sp.getString(KEY_CACHED_LATEST_VER, "") ?: ""
                if (now - lastSuccessfulCheckRealtimeMs in 1..MANUAL_RAPID_TAP_REUSE_MS && cachedLatest.isNotBlank()) {
                    delay(220L)
                    val currentVersion = getCurrentVersionName(appContext)
                    if (applyCachedOrResolvedVersionState(appContext, currentVersion, cachedLatest, openDialogIfNewer = true)) {
                        return@launch
                    }
                }
                performCheck(appContext, isManual = true)
            } finally {
                isChecking.set(false)
            }
        }
    }

    private fun performCheck(context: Context, isManual: Boolean) {
        val currentVersion = getCurrentVersionName(context)
        if (isManual) _state.value = UpdateCheckState.Checking
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedEtag = sp.getString(KEY_CACHED_ETAG, "") ?: ""
        val cachedLatest = sp.getString(KEY_CACHED_LATEST_VER, "") ?: ""

        var connection: HttpURLConnection? = null
        try {
            connection = openVerifiedDirectConnection(
                initialUrlString = LATEST_RELEASE_API_URL,
                allowGhFast = false,
                timeoutMs = TIMEOUT_MS,
                acceptHeader = "application/vnd.github+json",
                ifNoneMatchEtag = if (cachedLatest.isNotBlank()) cachedEtag else ""
            )
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED && cachedLatest.isNotBlank()) {
                sp.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply()
                if (applyCachedOrResolvedVersionState(context, currentVersion, cachedLatest, openDialogIfNewer = true)) {
                    return
                }
            }
            if (responseCode !in 200..299) {
                val redirectVer = fetchLatestVersionViaWebRedirect().ifBlank { cachedLatest }
                if (!applyCachedOrResolvedVersionState(context, currentVersion, redirectVer, openDialogIfNewer = true)) {
                    handleCheckFailure(currentVersion, isManual)
                }
                return
            }

            val newEtag = connection.getHeaderField("ETag")?.trim().orEmpty()
            val responseBody = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(responseBody)
            val latestVersion = normalizeVersionString(json.optString("tag_name", ""))
            if (latestVersion.isBlank() || parseNumericParts(latestVersion).isEmpty()) {
                val redirectVer = fetchLatestVersionViaWebRedirect().ifBlank { cachedLatest }
                if (!applyCachedOrResolvedVersionState(context, currentVersion, redirectVer, openDialogIfNewer = true)) {
                    handleCheckFailure(currentVersion, isManual)
                }
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
            val newerAvailable = isNewerVersion(currentVersion, latestVersion)
            if (newerAvailable && !isValidSha256Hex(officialSha256)) {
                officialSha256 = fetchSha256FromGitHubAsset(sha256AssetUrl)
            }

            val fastCdnUrl = buildFastCdnUrl(apkDirectUrl, latestVersion)
            val cleanNotes = sanitizeReleaseNotes(json.optString("body", "").trim())

            sp.edit()
                .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
                .putString(KEY_CACHED_LATEST_VER, latestVersion)
                .putString(KEY_CACHED_RELEASE_URL, releaseUrl)
                .putString(KEY_CACHED_RELEASE_NOTES, cleanNotes)
                .putString(KEY_CACHED_APK_DIRECT_URL, apkDirectUrl)
                .putString(KEY_CACHED_OFFICIAL_SHA256, officialSha256)
                .putString(KEY_CACHED_SHA256_ASSET_URL, sha256AssetUrl)
                .apply {
                    if (newEtag.isNotBlank()) putString(KEY_CACHED_ETAG, newEtag)
                }
                .apply()

            lastSuccessfulCheckRealtimeMs = System.currentTimeMillis()
            if (newerAvailable) {
                AppLogger.info("UPDATE", "Phát hiện bản cập nhật mới: v$currentVersion -> v$latestVersion")
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
                AppLogger.success("UPDATE", "Ứng dụng đang ở phiên bản mới nhất (v$currentVersion)")
                _state.value = UpdateCheckState.UpToDate(currentVersion = currentVersion)
            }
        } catch (e: Throwable) {
            val redirectVer = fetchLatestVersionViaWebRedirect().ifBlank { cachedLatest }
            if (!applyCachedOrResolvedVersionState(context, currentVersion, redirectVer, openDialogIfNewer = true)) {
                AppLogger.error("UPDATE", "Lỗi kiểm tra cập nhật (${AppLogger.shortThrowable(e)})")
                handleCheckFailure(currentVersion, isManual)
            }
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

    /**
     * Cho phép người dùng bấm chuyển ngay sang máy chủ phụ kế tiếp nếu thấy máy chủ hiện tại tải chậm.
     */
    fun skipToNextMirror() {
        if (_downloadState.value is ApkDownloadState.Downloading) {
            skipCurrentMirrorRequested = true
            AppLogger.info("UI", "Bấm chuyển sang máy chủ tải cập nhật kế tiếp")
            try {
                activeDownloadConnection?.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun isValidDownloadedApkArchive(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() < MIN_VALID_APK_BYTES) return false
        return try {
            val pm = context.packageManager
            val archiveInfo = pm?.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (archiveInfo != null) {
                archiveInfo.packageName == context.packageName
            } else {
                // Nếu PackageManager trả null trên một số ROM khi chưa cấp quyền đọc ngoài, vẫn chấp nhận nếu có magic ZIP chuẩn
                true
            }
        } catch (_: Throwable) {
            true
        }
    }

    fun startVerifiedApkDownload(
        context: Context,
        updateInfo: UpdateCheckState.UpdateAvailable,
        preferredMirrorId: String = "auto"
    ) {
        if (_downloadState.value is ApkDownloadState.Downloading || _downloadState.value is ApkDownloadState.Verifying) return
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastWorkingMirrorId = sp.getString(KEY_LAST_WORKING_MIRROR_ID, "") ?: ""
        val updatesDir = File(appContext.cacheDir, "updates").apply { if (!exists()) mkdirs() }
        val targetApkFile = File(updatesDir, "CloudPlay-Xbox-v${updateInfo.latestVersion}.apk")

        val modeDesc = when {
            preferredMirrorId == "github_direct" -> "Tải thường (Trực tiếp GitHub)"
            preferredMirrorId == "auto" -> "Tải nhanh qua máy chủ phụ (Tự động đổi khi lỗi)"
            else -> "Tải qua máy chủ phụ ($preferredMirrorId)"
        }
        AppLogger.info("UI", "Bắt đầu tải bản cập nhật v${updateInfo.latestVersion}: $modeDesc")

        downloadJob?.cancel()
        downloadJob = scope.launch {
            try {
                val directGitHubUrl = updateInfo.apkDirectUrl.ifBlank {
                    "https://github.com/muitenvn/CloudPlay-App/releases/download/v${updateInfo.latestVersion}/CloudPlay-Xbox-v${updateInfo.latestVersion}.apk"
                }
                val candidates = buildOrderedMirrorChain(
                    directGitHubUrl = directGitHubUrl,
                    preferredMirrorId = preferredMirrorId,
                    lastWorkingMirrorId = lastWorkingMirrorId
                )

                val failedMirrorNames = mutableListOf<String>()
                var officialSha256: String? = null
                var downloadedAndVerifiedOk = false

                for ((idx, pair) in candidates.withIndex()) {
                    val (node, candidateUrl) = pair
                    val attemptNum = idx + 1
                    val connectTimeoutForNode = if (node.isDirectGitHub) GITHUB_DOWNLOAD_TIMEOUT_MS else MIRROR_CONNECT_TIMEOUT_MS
                    val readTimeoutForNode = if (node.isDirectGitHub) GITHUB_DOWNLOAD_TIMEOUT_MS else MIRROR_READ_TIMEOUT_MS
                    skipCurrentMirrorRequested = false
                    try {
                        _downloadState.value = ApkDownloadState.Downloading(
                            progress = 0f,
                            downloadedBytes = 0L,
                            totalBytes = -1L,
                            usingFallbackGitHub = node.isDirectGitHub,
                            activeMirrorName = node.name,
                            mirrorIndex = attemptNum,
                            totalMirrors = candidates.size,
                            failedMirrors = failedMirrorNames.toList()
                        )
                        downloadStreamToFile(
                            downloadUrl = candidateUrl,
                            destFile = targetApkFile,
                            connectTimeoutMs = connectTimeoutForNode,
                            readTimeoutMs = readTimeoutForNode,
                            usingFallbackGitHub = node.isDirectGitHub,
                            activeMirrorName = node.name,
                            mirrorIndex = attemptNum,
                            totalMirrors = candidates.size,
                            failedMirrors = failedMirrorNames.toList()
                        )

                        // Kiểm tra cấu trúc gói APK và chữ ký SHA-256 ngay trong vòng lặp để nếu máy chủ phụ trả file hỏng thì tự chuyển sang máy chủ kế tiếp
                        _downloadState.value = ApkDownloadState.Verifying
                        if (!isValidDownloadedApkArchive(appContext, targetApkFile)) {
                            throw IllegalStateException("File tải từ ${node.name} không phải gói APK CloudPlay hợp lệ")
                        }

                        val resolvedSha = officialSha256 ?: ensureOfficialSha256FromGitHub(updateInfo).also {
                            officialSha256 = it
                        }
                        if (isValidSha256Hex(resolvedSha)) {
                            val computedSha256 = computeFileSha256(targetApkFile)
                            if (!isExactSha256Match(computedSha256, resolvedSha)) {
                                throw IllegalStateException("Mã SHA-256 từ ${node.name} không khớp với bản chính thức")
                            }
                        }

                        downloadedAndVerifiedOk = true
                        if (!node.isDirectGitHub) {
                            sp.edit().putString(KEY_LAST_WORKING_MIRROR_ID, node.id).apply()
                        }
                        AppLogger.success("UPDATE", "Tải và xác thực thành công APK qua ${node.name} ($attemptNum/${candidates.size})")
                        break
                    } catch (ce: CancellationException) {
                        targetApkFile.delete()
                        throw ce
                    } catch (err: Throwable) {
                        targetApkFile.delete()
                        failedMirrorNames.add(node.name)
                        val nextNodeName = candidates.getOrNull(idx + 1)?.first?.name
                        if (nextNodeName != null) {
                            AppLogger.warn(
                                "UPDATE",
                                "Máy chủ tải ${node.name} lỗi (${err.message ?: err.javaClass.simpleName}) -> tự động chuyển sang $nextNodeName (${idx + 2}/${candidates.size})"
                            )
                        } else {
                            AppLogger.error(
                                "UPDATE",
                                "Máy chủ tải cuối cùng ${node.name} lỗi (${err.message ?: err.javaClass.simpleName})"
                            )
                        }
                    }
                }

                if (!downloadedAndVerifiedOk) {
                    throw IllegalStateException("All download mirrors failed")
                }

                _downloadState.value = ApkDownloadState.VerifiedReadyToInstall(targetApkFile)
                launchApkInstaller(appContext, targetApkFile)
            } catch (_: CancellationException) {
                targetApkFile.delete()
                _downloadState.value = ApkDownloadState.Idle
            } catch (e: Throwable) {
                targetApkFile.delete()
                AppLogger.error("UPDATE", "Không thể tải bản cập nhật qua tất cả các cổng (${AppLogger.shortThrowable(e)})")
                _downloadState.value = ApkDownloadState.DownloadError(
                    messageVi = "Tất cả máy chủ phụ và cổng tải trực tiếp đều đang bận hoặc lỗi mạng. Bạn hãy thử chọn một máy chủ phụ khác hoặc bấm 'Mở trang GitHub'.",
                    messageEn = "All mirrors and direct download nodes are busy or unreachable. Please pick another mirror or tap 'Open GitHub Page'."
                )
            } finally {
                activeDownloadConnection = null
            }
        }
    }

    fun cancelApkDownload() {
        skipCurrentMirrorRequested = false
        try {
            activeDownloadConnection?.disconnect()
        } catch (_: Throwable) {}
        activeDownloadConnection = null
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = ApkDownloadState.Idle
    }

    /**
     * Kiểm tra 4 byte đầu tiên của luồng tải có phải là chữ ký chuẩn của file APK/ZIP ('P', 'K', 0x03, 0x04) hay không.
     * Giúp phát hiện tức thì trường hợp mirror bị lỗi (như ghfast.top bảo trì) trả về trang HTML lỗi với HTTP 200 OK.
     */
    fun hasValidApkZipMagicHeader(headerBytes: ByteArray, length: Int): Boolean {
        if (length < 4) return false
        return headerBytes[0] == 0x50.toByte() &&
            headerBytes[1] == 0x4B.toByte() &&
            headerBytes[2] == 0x03.toByte() &&
            headerBytes[3] == 0x04.toByte()
    }

    private suspend fun downloadStreamToFile(
        downloadUrl: String,
        destFile: File,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        usingFallbackGitHub: Boolean,
        activeMirrorName: String,
        mirrorIndex: Int,
        totalMirrors: Int,
        failedMirrors: List<String>
    ) {
        var conn: HttpURLConnection? = null
        try {
            conn = openVerifiedDirectConnection(
                initialUrlString = downloadUrl,
                allowGhFast = true,
                timeoutMs = connectTimeoutMs,
                readTimeoutMs = readTimeoutMs
            )
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            val contentType = conn.contentType?.lowercase().orEmpty()
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                throw IllegalStateException("Mirror returned $contentType error page instead of APK")
            }
            val contentLength = conn.contentLengthLong
            if (contentLength > MAX_APK_SIZE_BYTES) throw SecurityException("APK exceeds 150 MB")
            if (contentLength in 1 until MIN_VALID_APK_BYTES) {
                throw IllegalStateException("Response too small ($contentLength B) to be a valid APK")
            }

            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    val magicHeader = ByteArray(4)
                    var magicFilled = 0
                    var totalRead = 0L
                    while (coroutineContext.isActive) {
                        if (skipCurrentMirrorRequested) {
                            throw IllegalStateException("Người dùng chuyển sang máy chủ tiếp theo")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        if (read == 0) continue

                        if (magicFilled < 4) {
                            val toCopy = minOf(4 - magicFilled, read)
                            System.arraycopy(buffer, 0, magicHeader, magicFilled, toCopy)
                            magicFilled += toCopy
                            if (magicFilled == 4 && !hasValidApkZipMagicHeader(magicHeader, 4)) {
                                throw IllegalStateException("Invalid APK/ZIP header from $activeMirrorName")
                            }
                        }

                        totalRead += read
                        if (totalRead > MAX_APK_SIZE_BYTES) throw SecurityException("Stream exceeds 150 MB")
                        output.write(buffer, 0, read)
                        val progress = if (contentLength > 0L) (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else -1f
                        _downloadState.value = ApkDownloadState.Downloading(
                            progress = progress,
                            downloadedBytes = totalRead,
                            totalBytes = contentLength,
                            usingFallbackGitHub = usingFallbackGitHub,
                            activeMirrorName = activeMirrorName,
                            mirrorIndex = mirrorIndex,
                            totalMirrors = totalMirrors,
                            failedMirrors = failedMirrors
                        )
                    }
                    if (!coroutineContext.isActive) throw CancellationException("Cancelled")
                    if (skipCurrentMirrorRequested) {
                        throw IllegalStateException("Người dùng chuyển sang máy chủ tiếp theo")
                    }
                    output.flush()
                    if (totalRead < MIN_VALID_APK_BYTES) {
                        throw IllegalStateException("Downloaded file too small ($totalRead B)")
                    }
                    if (contentLength > 0L && totalRead != contentLength) {
                        throw IllegalStateException("Truncated download ($totalRead / $contentLength B)")
                    }
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
        } catch (e: Throwable) {
            AppLogger.error("UPDATE", "Lỗi mở trình cài đặt APK (${AppLogger.shortThrowable(e)})")
        }
    }

    private fun handleCheckFailure(currentVersion: String, isManual: Boolean) {
        if (isManual) {
            _state.value = UpdateCheckState.CheckFailed(currentVersion = currentVersion, isManual = true)
        } else if (_state.value is UpdateCheckState.Checking) {
            _state.value = UpdateCheckState.Idle
        }
    }

    internal fun sanitizeReleaseNotes(raw: String): String {
        if (raw.isBlank()) return ""
        val markdownLinkRegex = Regex("""\[([^\]]+)]\([^)]+\)""")
        return raw.lineSequence()
            .map { line ->
                line.trim()
                    .replace(Regex("""^#{1,6}\s*"""), "")
                    .removePrefix(">")
                    .replace(markdownLinkRegex, "$1")
                    .replace("**", "")
                    .replace("__", "")
                    .replace("`", "")
                    .trim()
            }
            .filter { it.isNotBlank() && !it.startsWith("---") && !it.startsWith("***") }
            .take(10)
            .joinToString("\n")
    }
}
