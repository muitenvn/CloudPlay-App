package com.example.data

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class AppLogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

@Immutable
data class AppLogEntry(
    val id: Long,
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: AppLogLevel = AppLogLevel.INFO
)

/**
 * Bộ ghi nhật ký hệ thống (AppLogger) lưu bền vững vào bộ nhớ riêng của ứng dụng (filesDir):
 * - Tự động che (redact) mọi dữ liệu nhạy cảm (token, cookie, authorization, bearer, mật khẩu, email, URL auth, JSON lồng nhau) TRƯỚC khi lưu vào RAM và ghi xuống file.
 * - Giới hạn tối đa 500 dòng trong RAM và <= 256 KB trên đĩa (tự động xoay vòng giữ phần mới nhất).
 * - Ghi đĩa bất đồng bộ trên luồng nền riêng biệt, không chặn luồng chính.
 */
object AppLogger {

    const val MAX_LOGS = 500
    const val MAX_PERF_LOGS = 40 // Giới hạn riêng cho tag [PERF] để không đẩy mất log mạng/VPN quan trọng
    const val MAX_TOTAL_BYTES = 256 * 1024 // 256 KB
    const val PERSISTENT_LOG_FILE_NAME = "cloudplay_activity_log.txt"
    const val MAX_PERSISTENT_FILE_BYTES = 256 * 1024 // 256 KB
    private const val ROTATED_KEEP_FILE_BYTES = 192 * 1024 // Giữ lại ~192 KB mới nhất khi vượt ngưỡng 256 KB
    const val STORAGE_LOCATION_DESC_VI = "Lưu bền vững trong bộ nhớ nội bộ ứng dụng (filesDir, tối đa 256 KB, giữ qua các lần mở lại app, tự xóa khi bấm Xóa log, không ghi cookie/token/tài khoản)"
    const val STORAGE_LOCATION_DESC_EN = "Persisted in internal app storage (filesDir, max 256 KB, retained across app restarts, deleted on Clear Log, no cookies/tokens/account data)"

    private val idGenerator = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileLock = Any()

    @Volatile
    private var persistentLogFile: java.io.File? = null

    @Volatile
    private var processInitialized = false

    private val diskExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "CloudPlay-AppLogger-IO").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    private val PERSISTED_LINE_REGEX = Regex("^\\[([^\\]]+)]\\s+\\[([^\\]]+)](?:\\s+\\[(INFO|SUCCESS|WARN|ERROR)])?\\s+(.*)$")

    private val SENSITIVE_JSON_KEYS = setOf(
        "token", "access_token", "refresh_token", "id_token", "auth", "authorization",
        "cookie", "set-cookie", "cookies", "password", "passwd", "pwd", "secret",
        "client_secret", "apikey", "api_key", "email", "user_email", "session",
        "session_id", "sessionid", "credential", "credentials", "bearer"
    )

    private val BEARER_REGEX = Regex("(?i)\\bBearer\\s+[A-Za-z0-9\\-._~+/]+=*")
    private val HEADER_KV_REGEX = Regex(
        "(?i)\\b(cookie|set-cookie|authorization|x-api-key|api_key|apikey|access_token|refresh_token|id_token|token|password|passwd|pwd|secret|client_secret)\\s*([:=])\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;\"'}]+)"
    )
    private val URL_USERINFO_REGEX = Regex("(?i)(https?://)([^/\\s:@]+:[^/\\s@]+@)")
    private val URL_QUERY_SECRET_REGEX = Regex(
        "(?i)([?&](?:token|access_token|refresh_token|id_token|code|key|api_key|apikey|sig|signature|password|passwd|secret|auth|cookie)=)[^&\\s#\"']+"
    )
    private val EMAIL_REGEX = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")
    private val JWT_REGEX = Regex("\\beyJ[A-Za-z0-9_-]{8,}\\.eyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b")

    private val _logs = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val logs: StateFlow<List<AppLogEntry>> = _logs.asStateFlow()

    private fun nowString(): String {
        return synchronized(timeFormat) {
            timeFormat.format(Date())
        }
    }

    private fun nowFullDateTimeString(): String {
        return synchronized(fullDateTimeFormat) {
            fullDateTimeFormat.format(Date())
        }
    }

    /**
     * Khởi tạo bộ lưu nhật ký bền vững trong `context.filesDir`.
     * Tự động nạp lại log cũ từ đĩa khi tiến trình mới khởi động và ghi dòng đánh dấu PID.
     */
    fun init(context: android.content.Context) {
        initWithDirectory(context.applicationContext.filesDir)
    }

    fun initWithDirectory(
        filesDir: java.io.File,
        pidOverride: Int? = null,
        forceReinit: Boolean = false
    ) {
        if (processInitialized && !forceReinit) return
        synchronized(fileLock) {
            if (processInitialized && !forceReinit) return
            try {
                if (!filesDir.exists()) {
                    filesDir.mkdirs()
                }
                val targetFile = java.io.File(filesDir, PERSISTENT_LOG_FILE_NAME)
                persistentLogFile = targetFile

                val loadedEntries = loadEntriesFromDiskLocked(targetFile)
                val earlyEntries = synchronized(_logs) {
                    val current = if (forceReinit) emptyList() else _logs.value
                    val combined = capPerfEntries(loadedEntries + current).takeLast(MAX_LOGS)
                    trimEntriesByBytes(combined)
                }
                synchronized(_logs) {
                    _logs.value = earlyEntries
                }

                // Nếu có các dòng log phát sinh sớm trước khi init() được gọi, ghi bù xuống file
                if (!forceReinit) {
                    val unsavedEarly = synchronized(_logs) {
                        _logs.value.filter { it.id > (loadedEntries.lastOrNull()?.id ?: 0L) }
                    }
                    for (entry in unsavedEarly) {
                        writeEntryToFileLocked(targetFile, entry)
                    }
                }
            } catch (_: Throwable) {
            }
            processInitialized = true
        }

        val pid = pidOverride ?: runCatching { android.os.Process.myPid() }.getOrDefault(0)
        val startTimestamp = nowFullDateTimeString()
        info("SYSTEM", "=== Khởi động tiến trình mới: $startTimestamp, PID $pid ===")
    }

    private fun loadEntriesFromDiskLocked(file: java.io.File): List<AppLogEntry> {
        if (!file.exists() || !file.isFile) return emptyList()
        return try {
            val rawBytes = file.readBytes()
            val cappedBytes = if (rawBytes.size > MAX_PERSISTENT_FILE_BYTES) {
                keepNewestLinesBytes(rawBytes, ROTATED_KEEP_FILE_BYTES)
            } else {
                rawBytes
            }
            val content = String(cappedBytes, Charsets.UTF_8)
            val parsed = mutableListOf<AppLogEntry>()
            for (rawLine in content.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                val match = PERSISTED_LINE_REGEX.matchEntire(line)
                if (match != null) {
                    val ts = match.groupValues[1]
                    val tag = match.groupValues[2].trim().uppercase(Locale.ROOT)
                    val levelStr = match.groupValues[3]
                    val rawMsg = match.groupValues[4].replace(" ⏎ ", "\n")
                    val level = runCatching {
                        if (levelStr.isNotBlank()) AppLogLevel.valueOf(levelStr) else AppLogLevel.INFO
                    }.getOrDefault(AppLogLevel.INFO)
                    parsed.add(
                        AppLogEntry(
                            id = idGenerator.getAndIncrement(),
                            timestamp = ts,
                            tag = tag,
                            message = sanitizeMessage(rawMsg),
                            level = level
                        )
                    )
                } else {
                    parsed.add(
                        AppLogEntry(
                            id = idGenerator.getAndIncrement(),
                            timestamp = nowString(),
                            tag = "LOG",
                            message = sanitizeMessage(line),
                            level = AppLogLevel.INFO
                        )
                    )
                }
            }
            parsed.takeLast(MAX_LOGS)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun formatEntryForDisk(entry: AppLogEntry): String {
        val singleLineMsg = sanitizeMessage(entry.message)
            .replace("\r\n", " ⏎ ")
            .replace('\n', ' ')
        return "[${entry.timestamp}] [${entry.tag}] [${entry.level.name}] $singleLineMsg\n"
    }

    private fun writeEntryToFileLocked(file: java.io.File, entry: AppLogEntry) {
        synchronized(fileLock) {
            try {
                // Nếu file đã bị thay đổi sang file khác hoặc chưa bật lưu đĩa thì bỏ qua
                if (persistentLogFile != file) return
                val lineBytes = formatEntryForDisk(entry).toByteArray(Charsets.UTF_8)
                java.io.FileOutputStream(file, true).use { fos ->
                    fos.write(lineBytes)
                }
                if (file.length() > MAX_PERSISTENT_FILE_BYTES) {
                    val allBytes = file.readBytes()
                    val rotated = keepNewestLinesBytes(allBytes, ROTATED_KEEP_FILE_BYTES)
                    file.writeBytes(rotated)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun keepNewestLinesBytes(bytes: ByteArray, targetMaxBytes: Int): ByteArray {
        if (bytes.size <= targetMaxBytes) return bytes
        var startIdx = bytes.size - targetMaxBytes
        while (startIdx < bytes.size && bytes[startIdx] != '\n'.code.toByte()) {
            startIdx++
        }
        if (startIdx < bytes.size && bytes[startIdx] == '\n'.code.toByte()) {
            startIdx++
        }
        return if (startIdx < bytes.size) {
            bytes.copyOfRange(startIdx, bytes.size)
        } else {
            bytes.copyOfRange(bytes.size - targetMaxBytes, bytes.size)
        }
    }

    private fun trimEntriesByBytes(entries: List<AppLogEntry>): List<AppLogEntry> {
        val trimmedByBytes = mutableListOf<AppLogEntry>()
        var runningBytes = 0
        for (i in entries.indices.reversed()) {
            val item = entries[i]
            val itemBytes = (item.timestamp.length + item.tag.length + item.message.length + 16) * 2
            if (runningBytes + itemBytes > MAX_TOTAL_BYTES && trimmedByBytes.isNotEmpty()) {
                break
            }
            runningBytes += itemBytes
            trimmedByBytes.add(item)
        }
        return trimmedByBytes.asReversed()
    }

    private fun capPerfEntries(entries: List<AppLogEntry>): List<AppLogEntry> {
        val perfCount = entries.count { it.tag == "PERF" }
        if (perfCount <= MAX_PERF_LOGS) return entries
        var toRemove = perfCount - MAX_PERF_LOGS
        val result = ArrayList<AppLogEntry>(entries.size - toRemove)
        for (entry in entries) {
            if (entry.tag == "PERF" && toRemove > 0) {
                toRemove--
            } else {
                result.add(entry)
            }
        }
        return result
    }

    fun flushDiskWritesForTest() {
        try {
            diskExecutor.submit {}.get(3, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Throwable) {
        }
    }

    /**
     * Che dữ liệu nhạy cảm theo cấu trúc JSON lồng nhau và biểu thức chính quy trước khi ghi vào bộ nhớ.
     */
    fun sanitizeMessage(raw: String): String {
        if (raw.isBlank()) return raw
        var text = raw.trim()

        // 1. Nếu chuỗi là JSON hoặc chứa khối JSON, lọc đệ quy các trường nhạy cảm
        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                val obj = JSONObject(text)
                sanitizeJsonObject(obj)
                text = obj.toString()
            } catch (_: Exception) {
            }
        } else if (text.startsWith("[") && text.endsWith("]")) {
            try {
                val arr = JSONArray(text)
                sanitizeJsonArray(arr)
                text = arr.toString()
            } catch (_: Exception) {
            }
        } else {
            val firstBrace = text.indexOf('{')
            val lastBrace = text.lastIndexOf('}')
            if (firstBrace >= 0 && lastBrace > firstBrace + 1) {
                val jsonCandidate = text.substring(firstBrace, lastBrace + 1)
                try {
                    val obj = JSONObject(jsonCandidate)
                    sanitizeJsonObject(obj)
                    text = text.substring(0, firstBrace) + obj.toString() + text.substring(lastBrace + 1)
                } catch (_: Exception) {
                }
            }
        }

        // 2. Che JWT, Bearer, URL user:pass, URL query token, Header/Key-Value nhạy cảm và Email
        text = JWT_REGEX.replace(text, "[REDACTED_JWT]")
        text = BEARER_REGEX.replace(text, "Bearer [REDACTED]")
        text = URL_USERINFO_REGEX.replace(text, "$1[REDACTED]@")
        text = URL_QUERY_SECRET_REGEX.replace(text, "$1[REDACTED]")
        text = HEADER_KV_REGEX.replace(text) { match ->
            val key = match.groupValues[1]
            val sep = match.groupValues[2]
            "$key$sep[REDACTED]"
        }
        text = EMAIL_REGEX.replace(text, "[REDACTED_EMAIL]")
        return text
    }

    private fun isSensitiveKey(key: String): Boolean {
        val lower = key.trim().lowercase(Locale.ROOT)
        return SENSITIVE_JSON_KEYS.any { lower == it || lower.contains(it) }
    }

    private fun sanitizeJsonObject(obj: JSONObject) {
        val keys = obj.keys().asSequence().toList()
        for (key in keys) {
            val value = obj.opt(key)
            if (isSensitiveKey(key)) {
                obj.put(key, "[REDACTED]")
            } else {
                when (value) {
                    is JSONObject -> sanitizeJsonObject(value)
                    is JSONArray -> sanitizeJsonArray(value)
                    is String -> obj.put(key, sanitizeMessage(value))
                }
            }
        }
    }

    private fun sanitizeJsonArray(arr: JSONArray) {
        for (i in 0 until arr.length()) {
            when (val value = arr.opt(i)) {
                is JSONObject -> sanitizeJsonObject(value)
                is JSONArray -> sanitizeJsonArray(value)
                is String -> arr.put(i, sanitizeMessage(value))
            }
        }
    }

    fun log(tag: String, message: String, level: AppLogLevel = AppLogLevel.INFO) {
        val cleanTag = tag.trim().uppercase(Locale.ROOT)
        val safeMessage = sanitizeMessage(message)
        val entry = AppLogEntry(
            id = idGenerator.getAndIncrement(),
            timestamp = nowString(),
            tag = cleanTag,
            message = safeMessage,
            level = level
        )
        synchronized(_logs) {
            val combined = capPerfEntries(_logs.value + entry).takeLast(MAX_LOGS)
            _logs.value = trimEntriesByBytes(combined)
        }
        val targetFile = persistentLogFile
        if (targetFile != null) {
            try {
                diskExecutor.execute {
                    writeEntryToFileLocked(targetFile, entry)
                }
            } catch (_: Throwable) {
            }
        }
        try {
            when (level) {
                AppLogLevel.ERROR -> Log.e("CloudPlay-$cleanTag", safeMessage)
                AppLogLevel.WARN -> Log.w("CloudPlay-$cleanTag", safeMessage)
                else -> Log.d("CloudPlay-$cleanTag", safeMessage)
            }
        } catch (_: Exception) {
            // Ignore android.util.Log exceptions in pure unit test environments
        }
    }

    fun info(tag: String, message: String) = log(tag, message, AppLogLevel.INFO)

    fun success(tag: String, message: String) = log(tag, message, AppLogLevel.SUCCESS)

    fun warn(tag: String, message: String) = log(tag, message, AppLogLevel.WARN)

    fun error(tag: String, message: String) = log(tag, message, AppLogLevel.ERROR)

    fun clear() {
        synchronized(_logs) {
            _logs.value = emptyList()
        }
        synchronized(fileLock) {
            try {
                persistentLogFile?.delete()
            } catch (_: Throwable) {
            }
        }
        try {
            diskExecutor.execute {
                synchronized(fileLock) {
                    try {
                        persistentLogFile?.delete()
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun exportAsText(): String {
        val snapshot = _logs.value
        if (snapshot.isEmpty()) return "Chưa có dữ liệu nhật ký (No logs recorded)."
        return snapshot.joinToString(separator = "\n") { entry ->
            "[${entry.timestamp}] [${entry.tag}] ${entry.message}"
        }
    }

    // =========================================================================
    // MỤC 4 & 5: LƯU CRASH GẦN NHẤT VÀO FILESDIR (1 FILE, <= 20 KB) & BÁO CÁO CHẨN ĐOÁN
    // =========================================================================
    const val LAST_CRASH_FILE_NAME = "last_crash_report.txt"
    const val MAX_CRASH_FILE_BYTES = 20 * 1024 // ~20 KB
    private val IPV4_REDACT_REGEX = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")

    @Volatile
    private var crashHandlerInstalled = false

    fun installCrashHandler(context: android.content.Context) {
        if (crashHandlerInstalled) return
        synchronized(this) {
            if (crashHandlerInstalled) return
            val appCtx = context.applicationContext
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    saveCrashToInternalFile(appCtx, thread.name, throwable)
                } catch (_: Throwable) {
                } finally {
                    previousHandler?.uncaughtException(thread, throwable)
                }
            }
            crashHandlerInstalled = true
        }
    }

    fun saveCrashToInternalFile(
        context: android.content.Context,
        threadName: String,
        throwable: Throwable
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val rawTrace = android.util.Log.getStackTraceString(throwable).ifBlank {
                "${throwable.javaClass.name}: ${throwable.message.orEmpty()}"
            }
            val header = "Time: $timestamp | Thread: $threadName\nException: ${throwable.javaClass.name}\n"
            val sanitized = IPV4_REDACT_REGEX.replace(sanitizeMessage(header + rawTrace), "[REDACTED_IP]")
            val bytes = sanitized.toByteArray(Charsets.UTF_8)
            val truncatedBytes = if (bytes.size > MAX_CRASH_FILE_BYTES) {
                bytes.copyOf(MAX_CRASH_FILE_BYTES)
            } else {
                bytes
            }
            val file = java.io.File(context.filesDir, LAST_CRASH_FILE_NAME)
            file.writeBytes(truncatedBytes)
        } catch (_: Throwable) {
        }
    }

    fun readLastCrashLog(context: android.content.Context): String {
        return try {
            val file = java.io.File(context.filesDir, LAST_CRASH_FILE_NAME)
            if (!file.exists() || !file.isFile) return ""
            val bytes = file.readBytes()
            val capped = if (bytes.size > MAX_CRASH_FILE_BYTES) {
                bytes.copyOf(MAX_CRASH_FILE_BYTES)
            } else {
                bytes
            }
            val content = String(capped, Charsets.UTF_8).trim()
            IPV4_REDACT_REGEX.replace(sanitizeMessage(content), "[REDACTED_IP]")
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Tạo báo cáo chẩn đoán văn bản thuần (Mục 5):
     * TUYỆT ĐỐI không đưa cookie, token, tài khoản hay địa chỉ IP của người dùng.
     */
    fun buildDiagnosticReport(
        context: android.content.Context,
        config: AppConfig = SettingsRepository.config.value,
        vpnState: com.example.vpn.VpnConnectionState = com.example.vpn.VpnManager.state.value,
        networkStatus: com.example.cloudplay.NetworkOptimizerStatus? = null,
        latestSession: PlaySessionRecord? = SessionHistoryRepository.sessions.value.firstOrNull()
    ): String {
        val isEn = config.isSettingsEnglishUI()
        val appVersion = UpdateChecker.getCurrentVersionName(context)
        val androidVer = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
        val netType = when {
            networkStatus?.isWifi == true -> "Wi-Fi"
            networkStatus?.isCellular == true -> if (isEn) "Mobile Data (4G/5G)" else "Dữ liệu di động (4G/5G)"
            else -> {
                val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
                when {
                    caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                    caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true ->
                        if (isEn) "Mobile Data (4G/5G)" else "Dữ liệu di động (4G/5G)"
                    caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                    else -> if (isEn) "Offline / Other" else "Ngoại tuyến / Khác"
                }
            }
        }
        val xboxMarket = config.getEffectiveLocale()
        val resolutionLabel = config.cellularMode.localizedTitle(isEn)
        val fpsLabel = "${config.streamFpsCap.fps} FPS"
        val vpnEnabledLabel = when (vpnState.mode) {
            com.example.vpn.NetworkMode.VPN_JAPAN -> if (isEn) "ON (${vpnState.engine.name} • ${vpnState.currentServer.countryCode})" else "BẬT (${vpnState.engine.name} • ${vpnState.currentServer.countryCode})"
            com.example.vpn.NetworkMode.DIRECT_NETWORK -> if (isEn) "OFF (Direct ISP)" else "TẮT (Mạng trực tiếp)"
            com.example.vpn.NetworkMode.DISCONNECTED -> if (isEn) "OFF (Idle)" else "TẮT (Chờ)"
        }

        val sessionSummary = if (latestSession != null) {
            if (isEn) {
                "Duration: ${latestSession.durationSeconds}s | Avg FPS: ${latestSession.avgFps} | Avg Ping: ${latestSession.avgPingMs}ms | Jitter: ${latestSession.jitterMs}ms | Decode: ${latestSession.avgDecodeMs}ms | Dropped: ${latestSession.framesDropped} | Freezes: ${latestSession.freezeCount}"
            } else {
                "Thời lượng: ${latestSession.durationSeconds}s | FPS TB: ${latestSession.avgFps} | Ping TB: ${latestSession.avgPingMs}ms | Jitter: ${latestSession.jitterMs}ms | Giải mã: ${latestSession.avgDecodeMs}ms | Rớt khung: ${latestSession.framesDropped} | Đứng hình: ${latestSession.freezeCount}"
            }
        } else {
            if (isEn) "No recorded session yet" else "Chưa có phiên chơi nào"
        }

        val lastCrash = readLastCrashLog(context).ifBlank {
            if (isEn) "None" else "Không có"
        }

        return buildString {
            appendLine("=== CLOUDPLAY DIAGNOSTIC REPORT ===")
            appendLine(if (isEn) "App Version: v$appVersion" else "Phiên bản ứng dụng: v$appVersion")
            appendLine(if (isEn) "Android Version: $androidVer" else "Phiên bản Android: $androidVer")
            appendLine(if (isEn) "Device Model: $deviceModel" else "Tên máy: $deviceModel")
            appendLine(if (isEn) "Network Type: $netType" else "Loại mạng: $netType")
            appendLine(if (isEn) "Xbox Market: $xboxMarket" else "Thị trường Xbox đang chọn: $xboxMarket")
            appendLine(if (isEn) "Main Settings: Resolution=$resolutionLabel | FPS=$fpsLabel | VPN=$vpnEnabledLabel" else "Cài đặt chính: Độ phân giải=$resolutionLabel | FPS=$fpsLabel | VPN=$vpnEnabledLabel")
            appendLine(if (isEn) "Latest Session Telemetry: $sessionSummary" else "Tóm tắt telemetry phiên gần nhất: $sessionSummary")
            appendLine(if (isEn) "--- Latest Crash Log ---" else "--- Crash gần nhất ---")
            append(lastCrash)
        }
    }
}
