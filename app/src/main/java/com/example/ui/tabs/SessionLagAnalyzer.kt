package com.example.ui.tabs

import androidx.compose.ui.graphics.Color
import com.example.data.PlaySessionRecord
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxNeonGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cấu hình ngưỡng chẩn đoán nguyên nhân giật/trễ (Nhóm B2 & C3).
 */
object LagDiagnosticConfig {
    const val MIN_DURATION_SECONDS = 30L

    // Độ trễ mạng (ms)
    const val GOOD_PING_MAX_MS = 80
    const val FAIR_PING_MAX_MS = 120

    // Độ biến động mạng - Jitter (ms)
    const val GOOD_JITTER_MAX_MS = 12
    const val FAIR_JITTER_MAX_MS = 20

    // Tỉ lệ khung hình bị rớt (%)
    const val GOOD_DROP_MAX_PCT = 0.5
    const val FAIR_DROP_MAX_PCT = 2.0

    // Thời gian giải mã mỗi khung hình (ms)
    const val GOOD_DECODE_MAX_MS = 10.0
    const val SLOW_DECODE_MIN_MS = 12.0
}

data class SessionLagDiagnosis(
    val isInsufficientData: Boolean,
    val evidenceSentence: String,
    val suggestionSentence: String,
    val dropRatioPct: Double,
    val usedEstimatedDenominator: Boolean
)

fun computeSessionDropRatio(session: PlaySessionRecord): Pair<Double, Boolean> {
    val dropped = session.framesDropped.coerceAtLeast(0)
    return if (session.framesDecoded > 0) {
        val total = (session.framesDecoded + dropped).coerceAtLeast(1)
        val pct = (dropped.toDouble() * 100.0) / total.toDouble()
        pct to false
    } else {
        val estimatedRendered = (session.durationSeconds.coerceAtLeast(1L) * session.avgFps.coerceAtLeast(30)).coerceAtLeast(1L)
        val total = (estimatedRendered + dropped).coerceAtLeast(1L)
        val pct = (dropped.toDouble() * 100.0) / total.toDouble()
        pct to true
    }
}

/**
 * B2 & B3: Phân tích "Vì sao có lag?" chỉ dựa trên số liệu thật đã lưu trong PlaySessionRecord.
 * Không phát sinh bất kỳ truy vấn mạng mới nào; không đổ lỗi cho mạng chỉ vì freezeCount > 0 khi ping và jitter bình thường;
 * chỉ kết luận giải mã chậm khi avgDecodeMs > 12.0ms kèm tỉ lệ khung hình bị rớt > 2.0%.
 */
fun diagnoseSessionLag(session: PlaySessionRecord, isEn: Boolean): SessionLagDiagnosis {
    val (dropRatioPct, usedEstimate) = computeSessionDropRatio(session)
    val dropStr = String.format(Locale.US, "%.1f", dropRatioPct)
    val estSuffix = if (usedEstimate) {
        if (isEn) " (estimated from avg FPS)" else " (ước tính từ FPS trung bình)"
    } else {
        ""
    }

    if (session.durationSeconds < LagDiagnosticConfig.MIN_DURATION_SECONDS ||
        (session.avgPingMs <= 0 && session.avgFps <= 0 && session.avgDecodeMs <= 0.0)
    ) {
        return SessionLagDiagnosis(
            isInsufficientData = true,
            evidenceSentence = if (isEn) {
                "Insufficient data to analyze this session."
            } else {
                "Chưa đủ dữ liệu để phân tích phiên chơi này."
            },
            suggestionSentence = "",
            dropRatioPct = dropRatioPct,
            usedEstimatedDenominator = usedEstimate
        )
    }

    val isSlowDecodeWithDrops =
        (session.avgDecodeMs > LagDiagnosticConfig.SLOW_DECODE_MIN_MS || session.isSlowDecode) &&
            dropRatioPct > LagDiagnosticConfig.FAIR_DROP_MAX_PCT

    if (isSlowDecodeWithDrops) {
        val evidence = if (isEn) {
            "Decode time averaged ${session.avgDecodeMs}ms with $dropStr% dropped frames$estSuffix."
        } else {
            "Thời gian giải mã trung bình ${session.avgDecodeMs}ms kèm tỉ lệ khung hình bị rớt $dropStr%$estSuffix."
        }
        val suggestion = if (isEn) {
            "Suggestion: Consider lowering stream bitrate to 720p or enabling Low-End Device Mode to reduce hardware decode load."
        } else {
            "Gợi ý: Có thể thử giảm chất lượng truyền tải xuống 720p hoặc bật Chế độ máy yếu để giảm tải giải mã."
        }
        return SessionLagDiagnosis(
            isInsufficientData = false,
            evidenceSentence = evidence,
            suggestionSentence = suggestion,
            dropRatioPct = dropRatioPct,
            usedEstimatedDenominator = usedEstimate
        )
    }

    val isHighPing = session.avgPingMs > LagDiagnosticConfig.FAIR_PING_MAX_MS
    val isHighJitter = session.jitterMs > LagDiagnosticConfig.FAIR_JITTER_MAX_MS

    if (isHighPing || isHighJitter) {
        val evidence = if (isEn) {
            "Network latency averaged ${session.avgPingMs}ms with ${session.jitterMs}ms network jitter during this session."
        } else {
            "Độ trễ mạng trung bình ${session.avgPingMs}ms và độ biến động mạng ${session.jitterMs}ms vượt ngưỡng ổn định trong phiên."
        }
        val suggestion = when {
            !session.isDirectNetwork -> if (isEn) {
                "Suggestion: Try enabling auto VPN disconnect after game launch or selecting a lower-latency server."
            } else {
                "Gợi ý: Có thể thử bật Tự tắt VPN sau khi vào game hoặc chọn máy chủ có độ trễ mạng thấp hơn."
            }
            session.networkType.contains("CELLULAR", ignoreCase = true) ||
                session.networkType.contains("4G", ignoreCase = true) -> if (isEn) {
                "Suggestion: Mobile network jitter was elevated; switching to a stable Wi-Fi connection may help if available."
            } else {
                "Gợi ý: Mạng di động có độ biến động cao, có thể thử chuyển sang kết nối Wi-Fi ổn định nếu sẵn có."
            }
            else -> if (isEn) {
                "Suggestion: Check your current network signal or test server latency before launching the next session."
            } else {
                "Gợi ý: Có thể kiểm tra lại tín hiệu mạng hiện tại hoặc đo độ trễ máy chủ trước khi vào phiên tiếp theo."
            }
        }
        return SessionLagDiagnosis(
            isInsufficientData = false,
            evidenceSentence = evidence,
            suggestionSentence = suggestion,
            dropRatioPct = dropRatioPct,
            usedEstimatedDenominator = usedEstimate
        )
    }

    if (session.freezeCount > 0 || dropRatioPct > LagDiagnosticConfig.FAIR_DROP_MAX_PCT) {
        val isDecodeElevated = session.avgDecodeMs > LagDiagnosticConfig.SLOW_DECODE_MIN_MS || session.isSlowDecode
        val evidence = if (isDecodeElevated) {
            if (isEn) {
                "Network latency (${session.avgPingMs}ms) and network jitter (${session.jitterMs}ms) were normal, while decode time was elevated (${session.avgDecodeMs}ms, $dropStr% dropped frames$estSuffix) with ${session.freezeCount} temporary freezes."
            } else {
                "Độ trễ mạng (${session.avgPingMs}ms) và độ biến động mạng (${session.jitterMs}ms) đều bình thường, thời gian giải mã cao (${session.avgDecodeMs}ms, rớt $dropStr%$estSuffix) kèm ${session.freezeCount} lần đứng hình tạm thời."
            }
        } else {
            if (isEn) {
                "Network latency (${session.avgPingMs}ms), network jitter (${session.jitterMs}ms), and decode time (${session.avgDecodeMs}ms) were normal, but ${session.freezeCount} temporary freezes ($dropStr% dropped frames$estSuffix) occurred."
            } else {
                "Độ trễ mạng (${session.avgPingMs}ms), độ biến động mạng (${session.jitterMs}ms) và thời gian giải mã (${session.avgDecodeMs}ms) đều bình thường, nhưng có ${session.freezeCount} lần đứng hình tạm thời (rớt $dropStr%$estSuffix)."
            }
        }
        val suggestion = if (isDecodeElevated) {
            if (isEn) {
                "Suggestion: Network latency was stable; locking 60Hz refresh rate or lowering stream resolution to 720p can help reduce hardware decode spikes."
            } else {
                "Gợi ý: Mạng ổn định, hiện tượng đứng hình có thể do tải giải mã trên thiết bị hoặc luồng đám mây (không phải do độ trễ mạng)."
            }
        } else {
            if (isEn) {
                "Suggestion: This pattern may come from remote cloud encoder spikes or background tasks on the device rather than network latency."
            } else {
                "Gợi ý: Hiện tượng này có thể đến từ phía luồng máy chủ đám mây hoặc tác vụ chạy nền trên thiết bị, không phải do độ trễ mạng."
            }
        }
        return SessionLagDiagnosis(
            isInsufficientData = false,
            evidenceSentence = evidence,
            suggestionSentence = suggestion,
            dropRatioPct = dropRatioPct,
            usedEstimatedDenominator = usedEstimate
        )
    }

    val evidence = if (isEn) {
        "Network latency (${session.avgPingMs}ms), network jitter (${session.jitterMs}ms), and decode time (${session.avgDecodeMs}ms) remained within good thresholds."
    } else {
        "Độ trễ mạng (${session.avgPingMs}ms), độ biến động mạng (${session.jitterMs}ms) và thời gian giải mã (${session.avgDecodeMs}ms) đều nằm trong ngưỡng ổn định."
    }
    val suggestion = if (isEn) {
        "Suggestion: Your current network and stream bitrate settings are working well."
    } else {
        "Gợi ý: Có thể giữ nguyên cấu hình mạng và mức chất lượng truyền tải hiện tại."
    }
    return SessionLagDiagnosis(
        isInsufficientData = false,
        evidenceSentence = evidence,
        suggestionSentence = suggestion,
        dropRatioPct = dropRatioPct,
        usedEstimatedDenominator = usedEstimate
    )
}

/**
 * Ngưỡng đánh giá chất lượng phiên chơi theo tỉ lệ thời gian (tập trung tại một nơi để dễ tinh chỉnh).
 */
object SessionVerdictThresholds {
    const val MIN_DURATION_SECONDS = 120L

    // Ngưỡng "Mượt" (tất cả điều kiện đều thỏa)
    const val SMOOTH_MAX_DROP_PCT = 3.0
    const val SMOOTH_MAX_FREEZES_PER_MIN = 3.0
    const val SMOOTH_MAX_PING_MS = 130
    const val SMOOTH_MAX_JITTER_MS = 20

    // Ngưỡng "Giật nhiều" (chỉ cần 1 điều kiện vượt ngưỡng)
    const val HEAVY_STUTTER_MIN_DROP_PCT = 8.0
    const val HEAVY_STUTTER_MIN_FREEZES_PER_MIN = 6.0
    const val HEAVY_STUTTER_MIN_PING_MS = 200
    const val HEAVY_STUTTER_MIN_JITTER_MS = 35
}

internal enum class SessionVerdictLevel {
    INSUFFICIENT_DATA,
    SMOOTH,
    SLIGHT_STUTTER,
    HEAVY_STUTTER
}

internal data class SessionVerdictEvaluation(
    val level: SessionVerdictLevel,
    val dropRatioPct: Double,
    val freezesPerMinute: Double
)

internal fun evaluateSessionVerdict(session: PlaySessionRecord): SessionVerdictEvaluation {
    val (dropRatioPct, _) = computeSessionDropRatio(session)
    val durationMinutes = (session.durationSeconds.coerceAtLeast(1L).toDouble() / 60.0).coerceAtLeast(0.1)
    val freezesPerMinute = session.freezeCount.coerceAtLeast(0).toDouble() / durationMinutes

    if (session.durationSeconds < SessionVerdictThresholds.MIN_DURATION_SECONDS) {
        return SessionVerdictEvaluation(
            level = SessionVerdictLevel.INSUFFICIENT_DATA,
            dropRatioPct = dropRatioPct,
            freezesPerMinute = freezesPerMinute
        )
    }

    val isHeavyStutter =
        dropRatioPct >= SessionVerdictThresholds.HEAVY_STUTTER_MIN_DROP_PCT ||
            freezesPerMinute >= SessionVerdictThresholds.HEAVY_STUTTER_MIN_FREEZES_PER_MIN ||
            session.avgPingMs >= SessionVerdictThresholds.HEAVY_STUTTER_MIN_PING_MS ||
            session.jitterMs >= SessionVerdictThresholds.HEAVY_STUTTER_MIN_JITTER_MS

    if (isHeavyStutter) {
        return SessionVerdictEvaluation(
            level = SessionVerdictLevel.HEAVY_STUTTER,
            dropRatioPct = dropRatioPct,
            freezesPerMinute = freezesPerMinute
        )
    }

    val isSmooth =
        dropRatioPct < SessionVerdictThresholds.SMOOTH_MAX_DROP_PCT &&
            freezesPerMinute < SessionVerdictThresholds.SMOOTH_MAX_FREEZES_PER_MIN &&
            (session.avgPingMs in 1 until SessionVerdictThresholds.SMOOTH_MAX_PING_MS) &&
            session.jitterMs < SessionVerdictThresholds.SMOOTH_MAX_JITTER_MS

    val level = if (isSmooth) SessionVerdictLevel.SMOOTH else SessionVerdictLevel.SLIGHT_STUTTER
    return SessionVerdictEvaluation(
        level = level,
        dropRatioPct = dropRatioPct,
        freezesPerMinute = freezesPerMinute
    )
}

/**
 * C2: Giải thích <= 1 câu ngắn gọn khi chạm vào từng ô chỉ số.
 */
fun getMetricOneSentenceExplanation(metricKey: String?, isEn: Boolean): String? = when (metricKey) {
    "ping" -> if (isEn) {
        "Network latency is the round-trip time for a packet between your device and the cloud server."
    } else {
        "Độ trễ mạng là thời gian gói dữ liệu đi từ thiết bị tới máy chủ đám mây và quay về."
    }
    "jitter" -> if (isEn) {
        "Network jitter measures how much latency fluctuates between consecutive packets."
    } else {
        "Độ biến động mạng thể hiện mức chênh lệch thời gian đến giữa các gói dữ liệu liên tiếp."
    }
    "decode" -> if (isEn) {
        "Decode time is the time taken by your phone's hardware decoder to process one video frame."
    } else {
        "Thời gian giải mã là thời gian chip đồ họa của điện thoại xử lý xong một khung hình video."
    }
    "buffer" -> if (isEn) {
        "Data buffer holds incoming frames briefly to smooth out network jitter before display."
    } else {
        "Bộ đệm dữ liệu giữ tạm các khung hình nhận về để bù đắp biến động mạng trước khi hiển thị."
    }
    "dropped" -> if (isEn) {
        "Dropped frames are video frames skipped because they arrived or decoded too late."
    } else {
        "Khung hình bị rớt là số khung hình bị bỏ qua do đến trễ hoặc giải mã không kịp."
    }
    "freeze" -> if (isEn) {
        "Temporary freeze counts how many times the video stream paused noticeably during play."
    } else {
        "Đứng hình tạm thời là số lần hình ảnh luồng phát bị khựng lại rõ rệt trong phiên chơi."
    }
    else -> null
}

/**
 * C3: Thang 3 mức màu + chữ đi kèm để người mù màu vẫn phân biệt được.
 */
fun pingMetricColor(pingMs: Int): Color = when {
    pingMs <= 0 -> TextSecondary
    pingMs <= LagDiagnosticConfig.GOOD_PING_MAX_MS -> XboxNeonGreen
    pingMs <= LagDiagnosticConfig.FAIR_PING_MAX_MS -> WarningAmber
    else -> JapanRed
}

fun pingStatusLabel(pingMs: Int, isEn: Boolean): String = when {
    pingMs <= 0 -> "--"
    pingMs <= LagDiagnosticConfig.GOOD_PING_MAX_MS -> if (isEn) "Good" else "Tốt"
    pingMs <= LagDiagnosticConfig.FAIR_PING_MAX_MS -> if (isEn) "Fair" else "Trung bình"
    else -> if (isEn) "High" else "Cao"
}

fun jitterMetricColor(jitterMs: Int): Color = when {
    jitterMs <= LagDiagnosticConfig.GOOD_JITTER_MAX_MS -> XboxNeonGreen
    jitterMs <= LagDiagnosticConfig.FAIR_JITTER_MAX_MS -> WarningAmber
    else -> JapanRed
}

fun jitterStatusLabel(jitterMs: Int, isEn: Boolean): String = when {
    jitterMs <= LagDiagnosticConfig.GOOD_JITTER_MAX_MS -> if (isEn) "Good" else "Tốt"
    jitterMs <= LagDiagnosticConfig.FAIR_JITTER_MAX_MS -> if (isEn) "Fair" else "Trung bình"
    else -> if (isEn) "High" else "Cao"
}

fun dropRatioMetricColor(dropPct: Double): Color = when {
    dropPct <= LagDiagnosticConfig.GOOD_DROP_MAX_PCT -> XboxNeonGreen
    dropPct <= LagDiagnosticConfig.FAIR_DROP_MAX_PCT -> WarningAmber
    else -> JapanRed
}

fun dropRatioStatusLabel(dropPct: Double, isEn: Boolean): String = when {
    dropPct <= LagDiagnosticConfig.GOOD_DROP_MAX_PCT -> if (isEn) "Good" else "Tốt"
    dropPct <= LagDiagnosticConfig.FAIR_DROP_MAX_PCT -> if (isEn) "Fair" else "Trung bình"
    else -> if (isEn) "High" else "Cao"
}

internal fun fpsMetricColor(fps: Int): Color = when {
    fps >= 55 -> XboxNeonGreen
    fps >= 40 -> WarningAmber
    else -> JapanRed
}

fun decodeMetricColor(decodeMs: Double): Color = when {
    decodeMs <= LagDiagnosticConfig.GOOD_DECODE_MAX_MS -> XboxNeonGreen
    decodeMs <= LagDiagnosticConfig.SLOW_DECODE_MIN_MS -> WarningAmber
    else -> JapanRed
}

fun decodeStatusLabel(decodeMs: Double, isEn: Boolean): String = when {
    decodeMs <= LagDiagnosticConfig.GOOD_DECODE_MAX_MS -> if (isEn) "Good" else "Tốt"
    decodeMs <= LagDiagnosticConfig.SLOW_DECODE_MIN_MS -> if (isEn) "Fair" else "Trung bình"
    else -> if (isEn) "High" else "Cao"
}

internal fun formatDurationCompact(durationSec: Long, isEn: Boolean): String {
    if (durationSec <= 0L) return if (isEn) "0m" else "0 phút"
    val hours = durationSec / 3600L
    val minutes = (durationSec % 3600L) / 60L
    val seconds = durationSec % 60L
    return when {
        hours > 0L -> if (isEn) "${hours}h ${minutes}m" else "${hours}g ${minutes}p"
        minutes > 0L -> if (isEn) "${minutes}m ${seconds}s" else "${minutes}p ${seconds}s"
        else -> "${seconds}s"
    }
}

internal fun formatSessionDateTime(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}

internal fun buildGlobalSmartTip(sessions: List<PlaySessionRecord>, isEn: Boolean): String? {
    if (sessions.isEmpty()) return null
    val recent = sessions.take(10)
    val slowOrHotCount = recent.count { it.isSlowDecode || it.isThermalHot }
    if (slowOrHotCount >= 2 || (recent.size == 1 && slowOrHotCount == 1)) {
        return if (isEn) {
            "Tip: Keep Auto (720p ⇄ 1080p) quality mode enabled to help prevent frame drops when Wi-Fi or mobile signal fluctuates."
        } else {
            "Gợi ý: Giữ chế độ Tự động (720p ⇄ 1080p) để tự điều chỉnh chất lượng truyền tải khi sóng Wi-Fi hoặc di động dao động."
        }
    }
    return null
}

internal fun buildSessionSmartTip(
    session: PlaySessionRecord,
    @Suppress("UNUSED_PARAMETER") allSessions: List<PlaySessionRecord>,
    isEn: Boolean
): String? {
    val samples = session.pingSamples
    val pingSwing = if (samples.size >= 2) {
        (samples.maxOrNull() ?: session.avgPingMs) - (samples.minOrNull() ?: session.avgPingMs)
    } else {
        0
    }

    if (session.isSlowDecode || session.isThermalHot) {
        return if (isEn) {
            "Tip: 60Hz VSync lock & Auto (720p ⇄ 1080p) bitrate control help keep hardware decode time under 12ms."
        } else {
            "Gợi ý: Khóa màn hình 60Hz và chế độ Tự động (720p ⇄ 1080p) giúp giữ thời gian giải mã ổn định."
        }
    }

    if (pingSwing >= 35 || session.avgPingMs > LagDiagnosticConfig.FAIR_PING_MAX_MS) {
        return if (isEn) {
            "Tip: Network latency fluctuated by ${pingSwing}ms. Selecting a lower-latency server in the Servers tab may improve stability."
        } else {
            "Gợi ý: Độ trễ mạng dao động ${pingSwing}ms trong phiên này. Bạn có thể đo và chọn máy chủ có độ trễ thấp hơn ở mục Máy chủ."
        }
    }

    return if (isEn) {
        "Network latency and stream decoding remained stable throughout this session (${session.avgPingMs}ms)."
    } else {
        "Độ trễ mạng và giải mã hình ảnh duy trì ổn định trong suốt phiên chơi (${session.avgPingMs}ms)."
    }
}
