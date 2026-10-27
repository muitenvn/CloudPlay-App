package com.example.data

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Bộ ghi nhật ký hệ thống (AppLogger) dùng để theo dõi ngầm hoạt động của ứng dụng:
 * - Chọn trạm đo ping gần nhất (TP.HCM, Hà Nội, Quốc tế...)
 * - Tự động chuyển trạm dự phòng lân cận hoặc đo trực tiếp từ máy khi trạm chính bận
 * - Kết quả đo ping từng máy chủ trong mục Máy chủ
 * - Sự kiện kết nối VPN, mở khóa vùng và luồng WebRTC
 */
object AppLogger {

    private const val MAX_LOGS = 350
    private val idGenerator = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val logs: StateFlow<List<AppLogEntry>> = _logs.asStateFlow()

    private fun nowString(): String {
        return synchronized(timeFormat) {
            timeFormat.format(Date())
        }
    }

    fun log(tag: String, message: String, level: AppLogLevel = AppLogLevel.INFO) {
        val cleanTag = tag.trim().uppercase()
        val entry = AppLogEntry(
            id = idGenerator.getAndIncrement(),
            timestamp = nowString(),
            tag = cleanTag,
            message = message,
            level = level
        )
        synchronized(_logs) {
            val updated = (_logs.value + entry).takeLast(MAX_LOGS)
            _logs.value = updated
        }
        try {
            when (level) {
                AppLogLevel.ERROR -> Log.e("CloudPlay-$cleanTag", message)
                AppLogLevel.WARN -> Log.w("CloudPlay-$cleanTag", message)
                else -> Log.d("CloudPlay-$cleanTag", message)
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
        info("SYSTEM", "Đã xóa nhật ký hệ thống (App Log cleared)")
    }

    fun exportAsText(): String {
        val snapshot = _logs.value
        if (snapshot.isEmpty()) return "Chưa có dữ liệu nhật ký (No logs recorded)."
        return snapshot.joinToString(separator = "\n") { entry ->
            "[${entry.timestamp}] [${entry.tag}] ${entry.message}"
        }
    }
}
