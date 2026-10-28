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
 * Bộ ghi nhật ký hệ thống (AppLogger) chỉ lưu trong bộ nhớ RAM cục bộ trên thiết bị:
 * - Tự động che (redact) mọi dữ liệu nhạy cảm (token, cookie, authorization, bearer, mật khẩu, email, URL auth, JSON lồng nhau) TRƯỚC khi lưu.
 * - Giới hạn cứng tối đa 300 dòng và <= 64 KB trong RAM; tự giải phóng khi đóng ứng dụng hoặc bấm Xóa log.
 */
object AppLogger {

    const val MAX_LOGS = 300
    const val MAX_TOTAL_BYTES = 64 * 1024 // 64 KB
    const val STORAGE_LOCATION_DESC_VI = "Lưu tạm trong bộ nhớ RAM cục bộ trên máy (tối đa 300 dòng / 64 KB, tự xóa khi tắt app, không ghi cookie/token/tài khoản)"
    const val STORAGE_LOCATION_DESC_EN = "Stored only in local device RAM (max 300 lines / 64 KB, cleared on exit, no cookies/tokens/account data)"

    private val idGenerator = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

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
            val combined = (_logs.value + entry).takeLast(MAX_LOGS)
            // Giới hạn tổng dung lượng không vượt quá 64 KB
            val trimmedByBytes = mutableListOf<AppLogEntry>()
            var runningBytes = 0
            for (i in combined.indices.reversed()) {
                val item = combined[i]
                val itemBytes = (item.timestamp.length + item.tag.length + item.message.length + 16) * 2
                if (runningBytes + itemBytes > MAX_TOTAL_BYTES && trimmedByBytes.isNotEmpty()) {
                    break
                }
                runningBytes += itemBytes
                trimmedByBytes.add(item)
            }
            _logs.value = trimmedByBytes.asReversed()
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
    }

    fun exportAsText(): String {
        val snapshot = _logs.value
        if (snapshot.isEmpty()) return "Chưa có dữ liệu nhật ký (No logs recorded)."
        return snapshot.joinToString(separator = "\n") { entry ->
            "[${entry.timestamp}] [${entry.tag}] ${entry.message}"
        }
    }
}
