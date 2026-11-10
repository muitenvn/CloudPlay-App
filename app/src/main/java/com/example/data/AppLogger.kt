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

    private const val BATCH_FLUSH_INTERVAL_MS = 250L

    private class RawLogRecord(
        val id: Long,
        val timestamp: String,
        val tag: String,
        val rawMessage: String?,
        val lazyMessage: (() -> String)?,
        val level: AppLogLevel
    )

    private val idGenerator = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileLock = Any()
    private val bufferLock = Any()

    private val pendingQueue = java.util.concurrent.ConcurrentLinkedQueue<RawLogRecord>()
    private val ringBuffer = java.util.ArrayDeque<AppLogEntry>(MAX_LOGS + 16)
    private var ringBufferBytes: Int = 0
    private var ringBufferPerfCount: Int = 0
    private val pendingDiskEntries = java.util.ArrayList<AppLogEntry>(64)
    private val workerScheduled = java.util.concurrent.atomic.AtomicBoolean(false)
    private val delayedFlushScheduled = java.util.concurrent.atomic.AtomicBoolean(false)

    @Volatile
    private var lastUiPublishAtMs: Long = 0L

    @Volatile
    private var lastDiskFlushAtMs: Long = 0L

    @Volatile
    private var unPublishedUiChanges: Boolean = false

    @Volatile
    private var persistentLogFile: java.io.File? = null

    @Volatile
    private var processInitialized = false

    private val diskExecutor: java.util.concurrent.ScheduledExecutorService =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor { runnable ->
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
    val logs: StateFlow<List<AppLogEntry>>
        get() {
            if (pendingQueue.isNotEmpty() || unPublishedUiChanges) {
                val isRealMainThread = android.os.Looper.getMainLooper()?.thread === Thread.currentThread() &&
                    !android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
                if (isRealMainThread) {
                    scheduleWorkerDrain(flushImmediately = true)
                } else {
                    processPendingLogsLocked(publishUiNow = true, flushDiskNow = false)
                }
            }
            return _logs.asStateFlow()
        }

    private fun entryByteCost(item: AppLogEntry): Int {
        return (item.timestamp.length + item.tag.length + item.message.length + 16) * 2
    }

    private fun rebuildRingBufferFromListLocked(entries: List<AppLogEntry>) {
        ringBuffer.clear()
        ringBufferBytes = 0
        ringBufferPerfCount = 0
        val cappedPerf = capPerfEntries(entries).takeLast(MAX_LOGS)
        val trimmed = trimEntriesByBytes(cappedPerf)
        for (e in trimmed) {
            ringBuffer.addLast(e)
            ringBufferBytes += entryByteCost(e)
            if (e.tag == "PERF") ringBufferPerfCount++
        }
    }

    private fun appendToRingBufferLocked(entry: AppLogEntry) {
        if (entry.tag == "PERF") {
            while (ringBufferPerfCount >= MAX_PERF_LOGS) {
                val iter = ringBuffer.iterator()
                var removed = false
                while (iter.hasNext()) {
                    val old = iter.next()
                    if (old.tag == "PERF") {
                        iter.remove()
                        ringBufferBytes = (ringBufferBytes - entryByteCost(old)).coerceAtLeast(0)
                        ringBufferPerfCount = (ringBufferPerfCount - 1).coerceAtLeast(0)
                        removed = true
                        break
                    }
                }
                if (!removed) break
            }
        }

        while (ringBuffer.size >= MAX_LOGS && ringBuffer.isNotEmpty()) {
            // Giữ lại các dòng [PERF-INIT] (chỉ sinh tối đa ~10 dòng mỗi tiến trình) khỏi bị đẩy mất khi log chung đầy 500 dòng
            val iter = ringBuffer.iterator()
            var removedEntry: AppLogEntry? = null
            while (iter.hasNext()) {
                val candidate = iter.next()
                if (candidate.tag != "PERF-INIT") {
                    iter.remove()
                    removedEntry = candidate
                    break
                }
            }
            val removed = removedEntry ?: ringBuffer.removeFirst()
            ringBufferBytes = (ringBufferBytes - entryByteCost(removed)).coerceAtLeast(0)
            if (removed.tag == "PERF") {
                ringBufferPerfCount = (ringBufferPerfCount - 1).coerceAtLeast(0)
            }
        }

        val cost = entryByteCost(entry)
        ringBuffer.addLast(entry)
        ringBufferBytes += cost
        if (entry.tag == "PERF") {
            ringBufferPerfCount++
        }

        while (ringBufferBytes > MAX_TOTAL_BYTES && ringBuffer.size > 1) {
            val iter = ringBuffer.iterator()
            var removedEntry: AppLogEntry? = null
            while (iter.hasNext()) {
                val candidate = iter.next()
                if (candidate.tag != "PERF-INIT") {
                    iter.remove()
                    removedEntry = candidate
                    break
                }
            }
            val removed = removedEntry ?: ringBuffer.removeFirst()
            ringBufferBytes = (ringBufferBytes - entryByteCost(removed)).coerceAtLeast(0)
            if (removed.tag == "PERF") {
                ringBufferPerfCount = (ringBufferPerfCount - 1).coerceAtLeast(0)
            }
        }
    }

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
    private val asyncInitScheduled = java.util.concurrent.atomic.AtomicBoolean(false)

    fun init(context: android.content.Context) {
        if (processInitialized || !asyncInitScheduled.compareAndSet(false, true)) return
        val filesDir = context.applicationContext.filesDir
        val isRobolectric = android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
        if (isRobolectric) {
            initWithDirectory(filesDir)
        } else {
            try {
                diskExecutor.execute {
                    initWithDirectory(filesDir)
                }
            } catch (_: Throwable) {
                initWithDirectory(filesDir)
            }
        }
    }

    fun initWithDirectory(
        filesDir: java.io.File,
        pidOverride: Int? = null,
        forceReinit: Boolean = false
    ) {
        if (processInitialized && !forceReinit) return
        processPendingLogsLocked(publishUiNow = true, flushDiskNow = false)
        synchronized(fileLock) {
            if (processInitialized && !forceReinit) return
            try {
                if (!filesDir.exists()) {
                    filesDir.mkdirs()
                }
                val targetFile = java.io.File(filesDir, PERSISTENT_LOG_FILE_NAME)
                persistentLogFile = targetFile

                val loadedEntries = loadEntriesFromDiskLocked(targetFile)
                var unsavedEarly: List<AppLogEntry> = emptyList()
                synchronized(bufferLock) {
                    val current = if (forceReinit) emptyList() else ringBuffer.toList()
                    if (forceReinit) {
                        pendingDiskEntries.clear()
                    }
                    rebuildRingBufferFromListLocked(loadedEntries + current)
                    val snapshot = ArrayList(ringBuffer)
                    _logs.value = snapshot
                    unPublishedUiChanges = false
                    lastUiPublishAtMs = System.currentTimeMillis()
                    if (!forceReinit) {
                        val maxLoadedId = loadedEntries.lastOrNull()?.id ?: 0L
                        unsavedEarly = snapshot.filter { it.id > maxLoadedId }
                        pendingDiskEntries.clear()
                    }
                }

                // Nếu có các dòng log phát sinh sớm trước khi init() được gọi, ghi bù xuống file
                if (!forceReinit && unsavedEarly.isNotEmpty()) {
                    writeEntriesToFileLocked(targetFile, unsavedEarly)
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
                            message = rawMsg,
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

    /**
     * `entry.message` đã được làm sạch (`sanitizeMessage`) đúng 1 lần khi tạo `AppLogEntry`,
     * không gọi lại `sanitizeMessage` lần thứ 2 khi ghi đĩa.
     */
    private fun formatEntryForDisk(entry: AppLogEntry): String {
        val singleLineMsg = entry.message
            .replace("\r\n", " ⏎ ")
            .replace('\n', ' ')
        return "[${entry.timestamp}] [${entry.tag}] [${entry.level.name}] $singleLineMsg\n"
    }

    private fun writeEntriesToFileLocked(file: java.io.File, entries: List<AppLogEntry>) {
        if (entries.isEmpty()) return
        synchronized(fileLock) {
            try {
                if (persistentLogFile != file) return
                val sb = StringBuilder(entries.size * 96)
                for (entry in entries) {
                    sb.append(formatEntryForDisk(entry))
                }
                val lineBytes = sb.toString().toByteArray(Charsets.UTF_8)
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
            val itemBytes = entryByteCost(item)
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

    private fun scheduleWorkerDrain(flushImmediately: Boolean) {
        if (flushImmediately) {
            try {
                diskExecutor.execute {
                    processPendingLogsLocked(publishUiNow = true, flushDiskNow = true)
                }
            } catch (_: Throwable) {
            }
            return
        }
        if (workerScheduled.compareAndSet(false, true)) {
            try {
                diskExecutor.execute {
                    try {
                        processPendingLogsLocked(publishUiNow = false, flushDiskNow = false)
                    } finally {
                        workerScheduled.set(false)
                        if (pendingQueue.isNotEmpty()) {
                            scheduleWorkerDrain(flushImmediately = false)
                        }
                    }
                }
            } catch (_: Throwable) {
                workerScheduled.set(false)
            }
        }
    }

    private fun scheduleDelayedBatchFlushIfNeeded() {
        if (delayedFlushScheduled.compareAndSet(false, true)) {
            try {
                diskExecutor.schedule(
                    {
                        try {
                            processPendingLogsLocked(publishUiNow = true, flushDiskNow = true)
                        } finally {
                            delayedFlushScheduled.set(false)
                        }
                    },
                    BATCH_FLUSH_INTERVAL_MS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
            } catch (_: Throwable) {
                delayedFlushScheduled.set(false)
            }
        }
    }

    private fun processPendingLogsLocked(publishUiNow: Boolean, flushDiskNow: Boolean) {
        var diskBatchToWrite: List<AppLogEntry>? = null
        var targetFile: java.io.File? = null
        var needsDelayedFlush = false

        synchronized(bufferLock) {
            var hasError = false
            var drainedAny = false
            while (true) {
                val record = pendingQueue.poll() ?: break
                drainedAny = true
                val rawMsg = record.rawMessage
                    ?: runCatching { record.lazyMessage?.invoke().orEmpty() }.getOrDefault("")
                val safeMessage = sanitizeMessage(rawMsg)
                val entry = AppLogEntry(
                    id = record.id,
                    timestamp = record.timestamp,
                    tag = record.tag,
                    message = safeMessage,
                    level = record.level
                )
                appendToRingBufferLocked(entry)
                unPublishedUiChanges = true
                if (persistentLogFile != null) {
                    pendingDiskEntries.add(entry)
                }
                if (record.level == AppLogLevel.ERROR) {
                    hasError = true
                }
                try {
                    when (record.level) {
                        AppLogLevel.ERROR, AppLogLevel.WARN -> Log.w("CloudPlay-${record.tag}", safeMessage)
                        else -> Log.d("CloudPlay-${record.tag}", safeMessage)
                    }
                } catch (_: Exception) {
                    // Ignore android.util.Log exceptions in pure unit test environments
                }
            }

            val nowMs = System.currentTimeMillis()
            val shouldPublishUi = unPublishedUiChanges &&
                (publishUiNow || hasError || (nowMs - lastUiPublishAtMs) >= BATCH_FLUSH_INTERVAL_MS)
            if (shouldPublishUi) {
                _logs.value = ArrayList(ringBuffer)
                unPublishedUiChanges = false
                lastUiPublishAtMs = nowMs
            }

            val shouldFlushDisk = pendingDiskEntries.isNotEmpty() &&
                (flushDiskNow || hasError || (nowMs - lastDiskFlushAtMs) >= BATCH_FLUSH_INTERVAL_MS)
            if (shouldFlushDisk) {
                targetFile = persistentLogFile
                if (targetFile != null) {
                    diskBatchToWrite = ArrayList(pendingDiskEntries)
                }
                pendingDiskEntries.clear()
                lastDiskFlushAtMs = nowMs
            }

            if (unPublishedUiChanges || pendingDiskEntries.isNotEmpty()) {
                needsDelayedFlush = true
            }
            if (!drainedAny && !shouldPublishUi && !shouldFlushDisk) {
                return
            }
        }

        if (diskBatchToWrite != null && targetFile != null) {
            writeEntriesToFileLocked(targetFile!!, diskBatchToWrite!!)
        }
        if (needsDelayedFlush) {
            scheduleDelayedBatchFlushIfNeeded()
        }
    }

    fun flushDiskWritesForTest() {
        processPendingLogsLocked(publishUiNow = true, flushDiskNow = true)
        try {
            diskExecutor.submit {
                processPendingLogsLocked(publishUiNow = true, flushDiskNow = true)
            }.get(3, java.util.concurrent.TimeUnit.SECONDS)
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

    /**
     * Đẩy bản ghi thô vào hàng đợi không khóa (lock-free queue) và trả về ngay lập tức trên luồng gọi.
     * Việc lọc nhạy cảm (`sanitizeMessage`), cắt giới hạn vòng đệm 500 dòng / 256 KB / 40 dòng [PERF],
     * phát `_logs.value` (~4 lần/giây) và gom ghi đĩa được thực hiện trên luồng nền `CloudPlay-AppLogger-IO`.
     */
    fun log(tag: String, message: String, level: AppLogLevel = AppLogLevel.INFO) {
        val cleanTag = tag.trim().uppercase(Locale.ROOT)
        val record = RawLogRecord(
            id = idGenerator.getAndIncrement(),
            timestamp = nowString(),
            tag = cleanTag,
            rawMessage = message,
            lazyMessage = null,
            level = level
        )
        pendingQueue.offer(record)
        scheduleWorkerDrain(flushImmediately = (level == AppLogLevel.ERROR))
    }

    fun logLazy(tag: String, level: AppLogLevel = AppLogLevel.INFO, messageBuilder: () -> String) {
        val cleanTag = tag.trim().uppercase(Locale.ROOT)
        val record = RawLogRecord(
            id = idGenerator.getAndIncrement(),
            timestamp = nowString(),
            tag = cleanTag,
            rawMessage = null,
            lazyMessage = messageBuilder,
            level = level
        )
        pendingQueue.offer(record)
        scheduleWorkerDrain(flushImmediately = (level == AppLogLevel.ERROR))
    }

    fun info(tag: String, message: String) = log(tag, message, AppLogLevel.INFO)

    fun success(tag: String, message: String) = log(tag, message, AppLogLevel.SUCCESS)

    fun warn(tag: String, message: String) = log(tag, message, AppLogLevel.WARN)

    fun error(tag: String, message: String) = log(tag, message, AppLogLevel.ERROR)

    fun clear() {
        synchronized(bufferLock) {
            pendingQueue.clear()
            ringBuffer.clear()
            ringBufferBytes = 0
            ringBufferPerfCount = 0
            pendingDiskEntries.clear()
            unPublishedUiChanges = false
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
        processPendingLogsLocked(publishUiNow = true, flushDiskNow = false)
        val snapshot = _logs.value
        if (snapshot.isEmpty()) return "Chưa có dữ liệu nhật ký (No logs recorded)."
        return snapshot.joinToString(separator = "\n") { entry ->
            "[${entry.timestamp}] [${entry.tag}] [${entry.level.name}] ${entry.message}"
        }
    }

    /**
     * Chỉ giữ lại scheme + host + path của URL (tuyệt đối bỏ query string ?..., fragment #... và userinfo)
     * để ghi nhật ký điều hướng WebView an toàn, không lộ mã xác thực.
     */
    fun safeUrlHostPath(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return "(trống)"
        val trimmed = rawUrl.trim()
        return try {
            val noQuery = trimmed.substringBefore('?').substringBefore('#')
            val uri = java.net.URI(noQuery)
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: "https"
            val host = uri.host?.lowercase(Locale.ROOT)
            val path = uri.path?.ifBlank { "/" } ?: "/"
            if (!host.isNullOrBlank()) {
                "$scheme://$host$path"
            } else {
                noQuery.replace(URL_USERINFO_REGEX, "$1[REDACTED]@").take(120)
            }
        } catch (_: Throwable) {
            trimmed.substringBefore('?').substringBefore('#')
                .replace(URL_USERINFO_REGEX, "$1[REDACTED]@")
                .take(120)
        }
    }

    /**
     * Tóm tắt ngắn gọn tên lớp ngoại lệ + thông điệp (đã lọc dữ liệu nhạy cảm) cho khối catch.
     */
    fun shortThrowable(t: Throwable?): String {
        if (t == null) return "UnknownError"
        val cls = t.javaClass.simpleName.ifBlank { "Exception" }
        val msg = t.message?.replace("\r\n", " ")?.replace('\n', ' ')?.trim().orEmpty()
        return if (msg.isNotBlank()) {
            "$cls: ${sanitizeMessage(msg).take(140)}"
        } else {
            cls
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
            processPendingLogsLocked(publishUiNow = true, flushDiskNow = true)
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
