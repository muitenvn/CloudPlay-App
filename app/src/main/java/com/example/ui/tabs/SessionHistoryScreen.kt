package com.example.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GameLibraryRepository
import com.example.data.PlaySessionRecord
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.ui.components.PingSparklineChart
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.animateXboxIntAsState
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.ui.theme.xboxSlideFadeIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class HistoryFilterMode {
    ALL,
    SMOOTH,
    ALERTS
}

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
        val evidence = if (isEn) {
            "Network latency (${session.avgPingMs}ms), network jitter (${session.jitterMs}ms), and decode time (${session.avgDecodeMs}ms) were normal, but ${session.freezeCount} temporary freezes ($dropStr% dropped frames$estSuffix) occurred."
        } else {
            "Độ trễ mạng (${session.avgPingMs}ms), độ biến động mạng (${session.jitterMs}ms) và thời gian giải mã (${session.avgDecodeMs}ms) đều bình thường, nhưng có ${session.freezeCount} lần đứng hình tạm thời (rớt $dropStr%$estSuffix)."
        }
        val suggestion = if (isEn) {
            "Suggestion: This pattern may come from remote cloud encoder spikes or background tasks on the device rather than network latency."
        } else {
            "Gợi ý: Hiện tượng này có thể đến từ phía luồng máy chủ đám mây hoặc tác vụ chạy nền trên thiết bị, không phải do độ trễ mạng."
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

private enum class SessionVerdictLevel {
    INSUFFICIENT_DATA,
    SMOOTH,
    SLIGHT_STUTTER,
    HEAVY_STUTTER
}

private data class SessionVerdictEvaluation(
    val level: SessionVerdictLevel,
    val dropRatioPct: Double,
    val freezesPerMinute: Double
)

private fun evaluateSessionVerdict(session: PlaySessionRecord): SessionVerdictEvaluation {
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

@Composable
fun SessionHistoryScreen(
    onLaunchGame: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sessions by SessionHistoryRepository.sessions.collectAsState()
    val allGames by GameLibraryRepository.games.collectAsState()
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )

    var selectedSession by remember { mutableStateOf<PlaySessionRecord?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(HistoryFilterMode.ALL) }

    val filteredSessions = remember(sessions, filterMode) {
        when (filterMode) {
            HistoryFilterMode.ALL -> sessions
            HistoryFilterMode.SMOOTH -> sessions.filter { it.avgFps >= 55 && !it.isSlowDecode && !it.isThermalHot }
            HistoryFilterMode.ALERTS -> sessions.filter { it.isSlowDecode || it.isThermalHot || it.avgPingMs > 110 }
        }
    }

    fun resolvePlayUrlForSession(session: PlaySessionRecord): String {
        val trimmed = session.gameTitle.trim()
        val matched = allGames.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
            ?: allGames.firstOrNull {
                it.title.contains(trimmed, ignoreCase = true) ||
                    trimmed.contains(it.title, ignoreCase = true)
            }
        return when {
            matched != null -> matched.playUrl
            trimmed.contains("fortnite", ignoreCase = true) -> "https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2"
            else -> config.targetRegionUrl
        }
    }

    val oneWeekAgoMs = remember(sessions) {
        System.currentTimeMillis() - 7L * 24L * 3600L * 1000L
    }
    val weeklySessions = remember(sessions, oneWeekAgoMs) {
        sessions.filter { it.startTimeMillis >= oneWeekAgoMs }
    }
    val weeklyTotalSeconds = remember(weeklySessions) {
        weeklySessions.sumOf { it.durationSeconds }
    }
    val weeklyAvgPing = remember(weeklySessions) {
        val valid = weeklySessions.filter { it.avgPingMs > 0 }
        if (valid.isNotEmpty()) valid.sumOf { it.avgPingMs } / valid.size else 0
    }

    val globalSmartTip = remember(sessions, isEn) {
        buildGlobalSmartTip(sessions, isEn)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Thẻ tổng quan "Tuần này"
        WeeklySummaryCard(
            totalDurationSec = weeklyTotalSeconds,
            sessionCount = weeklySessions.size,
            avgPingMs = weeklyAvgPing,
            hasAnyHistory = sessions.isNotEmpty(),
            isEn = isEn,
            onClearAllClick = { showClearConfirmDialog = true }
        )

        // Bộ lọc nhanh khi có lịch sử
        if (sessions.isNotEmpty()) {
            val smoothCount = remember(sessions) {
                sessions.count { it.avgFps >= 55 && !it.isSlowDecode && !it.isThermalHot }
            }
            val alertCount = remember(sessions) {
                sessions.count { it.isSlowDecode || it.isThermalHot || it.avgPingMs > 110 }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterItems = listOf(
                    HistoryFilterMode.ALL to (if (isEn) "All (${sessions.size})" else "Tất cả (${sessions.size})"),
                    HistoryFilterMode.SMOOTH to (if (isEn) "Smooth ($smoothCount)" else "Mượt ($smoothCount)"),
                    HistoryFilterMode.ALERTS to (if (isEn) "Alerts ($alertCount)" else "Cần lưu ý ($alertCount)")
                )
                filterItems.forEach { (mode, label) ->
                    val isSelected = filterMode == mode
                    val filterChipBg by animateColorAsState(
                        targetValue = if (isSelected) XboxGreen.copy(alpha = 0.28f) else XboxSurfaceVariant.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = if (motionEnabled) 150 else 0),
                        label = "history_filter_bg"
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.96f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { filterMode = mode }
                            .testTag("history_filter_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        color = filterChipBg,
                        border = BorderStroke(
                            0.9.dp,
                            if (isSelected) XboxNeonGreen else XboxCardBorder
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) XboxNeonGreen else TextSecondary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Gợi ý thông minh ngắn ở đầu trang (nếu có)
        if (globalSmartTip != null) {
            SmartSuggestionBanner(text = globalSmartTip)
        }

        // 2. Danh sách phiên chơi hoặc trạng thái trống
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("session_history_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(XboxSurfaceVariant.copy(alpha = 0.85f))
                            .border(1.dp, XboxCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = if (isEn) {
                            "No session history"
                        } else {
                            "Chưa có lịch sử"
                        },
                        color = TextSecondary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("session_history_list"),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val matchedBookmark = remember(session.gameTitle, allGames) {
                        val trimmed = session.gameTitle.trim()
                        allGames.firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
                            ?: allGames.firstOrNull {
                                it.title.contains(trimmed, ignoreCase = true) ||
                                    trimmed.contains(it.title, ignoreCase = true)
                            }
                    }
                    SessionHistoryItemCard(
                        session = session,
                        coverDrawableResId = matchedBookmark?.drawableResId,
                        coverImageUrl = matchedBookmark?.imageUrl,
                        accentColorHex = matchedBookmark?.accentColorHex ?: 0xFF107C10L,
                        isEn = isEn,
                        onClick = { selectedSession = session },
                        onPlayAgain = if (onLaunchGame != null) {
                            { onLaunchGame(resolvePlayUrlForSession(session)) }
                        } else null
                    )
                }
            }
        }
    }

    // Hộp thoại chi tiết phiên chơi (chỉ vẽ biểu đồ khi mở)
    selectedSession?.let { activeSession ->
        val bestServerTip = remember(activeSession, sessions, isEn) {
            buildSessionSmartTip(activeSession, sessions, isEn)
        }
        SessionDetailDialog(
            session = activeSession,
            smartTip = bestServerTip,
            isEn = isEn,
            onDismiss = { selectedSession = null },
            onPlayAgain = if (onLaunchGame != null) {
                {
                    val url = resolvePlayUrlForSession(activeSession)
                    selectedSession = null
                    onLaunchGame(url)
                }
            } else null,
            onDelete = {
                SessionHistoryRepository.deleteSession(activeSession.id)
                selectedSession = null
            }
        )
    }

    // Hộp xác nhận xóa tất cả lịch sử
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = if (isEn) "Clear all history?" else "Xóa tất cả lịch sử?",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "All recorded play sessions will be permanently removed."
                    } else {
                        "Toàn bộ lịch sử các phiên chơi đã lưu sẽ bị xóa."
                    },
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SessionHistoryRepository.clearAllSessions()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JapanRed),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text(
                        text = if (isEn) "Delete All" else "Xóa tất cả",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(
                        text = if (isEn) "Cancel" else "Hủy",
                        color = TextSecondary
                    )
                }
            },
            containerColor = XboxSurface
        )
    }
}

@Composable
private fun WeeklySummaryCard(
    totalDurationSec: Long,
    sessionCount: Int,
    avgPingMs: Int,
    hasAnyHistory: Boolean,
    isEn: Boolean,
    onClearAllClick: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val animatedAvgPing by animateXboxIntAsState(
        targetValue = if (avgPingMs > 0) avgPingMs else 0,
        motionEnabled = motionEnabled,
        label = "weekly_avg_ping"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .xboxSlideFadeIn(enabled = motionEnabled, key = "weekly_summary_card", durationMillis = 170)
            .testTag("weekly_summary_card"),
        shape = RoundedCornerShape(16.dp),
        color = XboxSurface.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(XboxNeonGreen.copy(alpha = 0.45f), XboxCardBorder, DirectCyan.copy(alpha = 0.45f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "THIS WEEK" else "TUẦN NÀY",
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )

                if (hasAnyHistory) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = JapanRed.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, JapanRed.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearAllClick() }
                            .testTag("clear_all_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = JapanRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isEn) "Clear All" else "Xóa tất cả lịch sử",
                                color = JapanRed,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatTile(
                    label = if (isEn) "Play Time" else "Tổng thời gian",
                    value = formatDurationCompact(totalDurationSec, isEn),
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = if (isEn) "Sessions" else "Số phiên",
                    value = "$sessionCount",
                    valueColor = DirectCyan,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = if (isEn) "Network latency" else "Độ trễ mạng",
                    value = if (avgPingMs > 0) "${animatedAvgPing}ms" else "--",
                    valueColor = if (avgPingMs > 0) pingMetricColor(avgPingMs) else TextSecondary,
                    statusBadgeText = if (avgPingMs > 0) pingStatusLabel(avgPingMs, isEn) else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    valueColor: Color,
    statusBadgeText: String? = null,
    onClick: (() -> Unit)? = null,
    testTagName: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .then(
                if (testTagName != null) Modifier.testTag(testTagName) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = XboxSurfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, XboxCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!statusBadgeText.isNullOrBlank()) {
                    Text(
                        text = statusBadgeText,
                        color = valueColor,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = value,
                color = valueColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmartSuggestionBanner(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_smart_suggestion"),
        shape = RoundedCornerShape(12.dp),
        color = DirectCyan.copy(alpha = 0.10f),
        border = BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = DirectCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                color = TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SessionHistoryItemCard(
    session: PlaySessionRecord,
    coverDrawableResId: Int?,
    coverImageUrl: String? = null,
    accentColorHex: Long,
    isEn: Boolean,
    onClick: () -> Unit,
    onPlayAgain: (() -> Unit)? = null
) {
    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("session_card_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh bìa game (tạm dừng tải trong lúc đang nhập kho game để không nghẽn mạng)
            val isImportingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.8.dp, Color(accentColorHex).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(accentColorHex).copy(alpha = 0.7f), XboxDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                if (!isImportingCatalog && !coverImageUrl.isNullOrBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = remember(coverImageUrl) {
                            ImageRequest.Builder(context)
                                .data(coverImageUrl)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = session.gameTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (coverDrawableResId != null) {
                    Image(
                        painter = painterResource(id = coverDrawableResId),
                        contentDescription = session.gameTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hàng tiêu đề + thời lượng + nút Chơi lại gọn gàng (không bị ép thành vạch dọc bên phải)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.gameTitle,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDurationCompact(session.durationSeconds, isEn),
                        color = XboxNeonGreen,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (onPlayAgain != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = XboxGreen.copy(alpha = 0.25f),
                            border = BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { onPlayAgain() }
                                .testTag("session_play_again_${session.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = if (isEn) "Play Again" else "Chơi lại",
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isEn) "Play" else "Chơi lại",
                                    color = XboxNeonGreen,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Ngày giờ + máy chủ/mạng
                Text(
                    text = "${formatSessionDateTime(session.startTimeMillis)} • ${session.formattedNetworkLabel(isEn)}",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 3 chip số liệu (Độ trễ mạng / FPS / Thời gian giải mã) kèm nhãn chữ 3 mức (Tốt / Trung bình / Cao)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pingTag = pingStatusLabel(session.avgPingMs, isEn)
                    MetricChip(
                        text = if (isEn) "Latency ${session.avgPingMs}ms ($pingTag)" else "Độ trễ ${session.avgPingMs}ms ($pingTag)",
                        color = pingMetricColor(session.avgPingMs)
                    )
                    MetricChip(
                        text = "${session.avgFps} FPS",
                        color = fpsMetricColor(session.avgFps)
                    )
                    val decTag = decodeStatusLabel(session.avgDecodeMs, isEn)
                    MetricChip(
                        text = if (isEn) "Decode ${session.avgDecodeMs}ms ($decTag)" else "Giải mã ${session.avgDecodeMs}ms ($decTag)",
                        color = decodeMetricColor(session.avgDecodeMs)
                    )
                }

                if (session.isThermalHot || session.isSlowDecode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.isThermalHot) {
                            WarningBadgePill(
                                text = if (isEn) "Device Hot" else "Máy nóng",
                                color = JapanRed
                            )
                        }
                        if (session.isSlowDecode) {
                            WarningBadgePill(
                                text = if (isEn) "Slow Decode" else "Giải mã chậm",
                                color = WarningAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.7.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
private fun WarningBadgePill(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(0.6.dp, color.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Whatshot,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = text,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SessionDetailDialog(
    session: PlaySessionRecord,
    smartTip: String?,
    isEn: Boolean,
    onDismiss: () -> Unit,
    onPlayAgain: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val chartSamples = remember(session.pingSamples, session.avgPingMs) {
        if (session.pingSamples.isNotEmpty()) session.pingSamples else listOf(session.avgPingMs.coerceAtLeast(25))
    }
    val minPing = remember(chartSamples) { chartSamples.minOrNull() ?: session.avgPingMs }
    val maxPing = remember(chartSamples) { chartSamples.maxOrNull() ?: session.avgPingMs }
    val lagDiagnosis = remember(session, isEn) { diagnoseSessionLag(session, isEn) }
    var selectedMetricKey by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .xboxSlideFadeIn(
                    enabled = motionEnabled,
                    key = "detail_${session.id}",
                    durationMillis = 175,
                    initialOffsetY = 8.dp,
                    initialScale = 0.96f
                )
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.2.dp,
                    Brush.verticalGradient(listOf(XboxNeonGreen, DirectCyan, XboxCardBorder)),
                    RoundedCornerShape(22.dp)
                )
                .testTag("session_detail_dialog"),
            color = XboxDark.copy(alpha = 0.98f),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.gameTitle,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatSessionDateTime(session.startTimeMillis)} • ${session.formattedNetworkLabel(isEn)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Đóng",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Nhãn kết luận lớn ở đầu chi tiết phiên (Mượt / Hơi giật / Giật nhiều / Chưa đủ dữ liệu)
                val verdict = remember(session) { evaluateSessionVerdict(session) }
                val verdictColor = when (verdict.level) {
                    SessionVerdictLevel.SMOOTH -> XboxNeonGreen
                    SessionVerdictLevel.SLIGHT_STUTTER -> WarningAmber
                    SessionVerdictLevel.HEAVY_STUTTER -> JapanRed
                    SessionVerdictLevel.INSUFFICIENT_DATA -> DirectCyan
                }
                val verdictTitle = when (verdict.level) {
                    SessionVerdictLevel.SMOOTH -> if (isEn) "Smooth" else "Mượt"
                    SessionVerdictLevel.SLIGHT_STUTTER -> if (isEn) "Slightly Stuttery" else "Hơi giật"
                    SessionVerdictLevel.HEAVY_STUTTER -> if (isEn) "Heavy Stutter" else "Giật nhiều"
                    SessionVerdictLevel.INSUFFICIENT_DATA -> if (isEn) "Insufficient Data" else "Chưa đủ dữ liệu"
                }
                val verdictSummary = when (verdict.level) {
                    SessionVerdictLevel.INSUFFICIENT_DATA -> if (isEn) {
                        "Session under 2 minutes (${formatDurationCompact(session.durationSeconds, true)}) • Play ≥ 2m for full rating"
                    } else {
                        "Phiên dưới 2 phút (${formatDurationCompact(session.durationSeconds, false)}) • Cần chơi từ 2 phút để đánh giá chuẩn"
                    }
                    else -> if (isEn) {
                        String.format(
                            Locale.US,
                            "Dropped %.1f%% • Freezes %.1f/min • Latency %dms • Jitter %dms",
                            verdict.dropRatioPct,
                            verdict.freezesPerMinute,
                            session.avgPingMs,
                            session.jitterMs
                        )
                    } else {
                        String.format(
                            Locale.US,
                            "Khung hình bị rớt %.1f%% • Đứng hình tạm thời %.1f lần/phút • Độ trễ mạng %dms • Độ biến động mạng %dms",
                            verdict.dropRatioPct,
                            verdict.freezesPerMinute,
                            session.avgPingMs,
                            session.jitterMs
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .xboxSlideFadeIn(
                            enabled = motionEnabled,
                            key = "verdict_${session.id}",
                            delayMillis = 30,
                            durationMillis = 175
                        )
                        .testTag("session_verdict_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = verdictColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = verdictColor.copy(alpha = 0.24f),
                                    border = BorderStroke(0.8.dp, verdictColor)
                                ) {
                                    Text(
                                        text = verdictTitle,
                                        color = verdictColor,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = if (isEn) "Session Verdict" else "Đánh giá phiên chơi",
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = verdictSummary,
                                color = TextSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                maxLines = 2
                            )
                        }
                    }
                }

                // B2: Thẻ "Vì sao có lag?" chỉ sử dụng dữ liệu đã lưu trong PlaySessionRecord
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("why_lag_diagnostic_card"),
                    shape = RoundedCornerShape(14.dp),
                    color = XboxSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEn) "WHY WAS THERE LAG?" else "VÌ SAO CÓ LAG?",
                            color = DirectCyan,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = lagDiagnosis.evidenceSentence,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (lagDiagnosis.suggestionSentence.isNotBlank()) {
                            Text(
                                text = lagDiagnosis.suggestionSentence,
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                // Biểu đồ đường độ trễ mạng theo thời gian
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = XboxSurfaceVariant.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, XboxCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isEn) "NETWORK LATENCY CHART" else "BIỂU ĐỒ ĐỘ TRỄ MẠNG",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Min ${minPing}ms • Max ${maxPing}ms" else "Thấp ${minPing}ms • Cao ${maxPing}ms",
                                color = DirectCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        PingSparklineChart(
                            samples = chartSamples,
                            strokeColor = pingMetricColor(session.avgPingMs),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        )
                    }
                }

                // C1, C2, C3: Các ô chỉ số chuẩn hóa thuật ngữ, có nhãn chữ 3 mức (Tốt / Trung bình / Cao) và chạm để xem giải thích <= 1 câu
                val dropPctFormatted = String.format(Locale.US, "%.1f%%", lagDiagnosis.dropRatioPct)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatTile(
                        label = if (isEn) "Network latency" else "Độ trễ mạng",
                        value = "${session.avgPingMs}ms",
                        valueColor = pingMetricColor(session.avgPingMs),
                        statusBadgeText = pingStatusLabel(session.avgPingMs, isEn),
                        onClick = { selectedMetricKey = if (selectedMetricKey == "ping") null else "ping" },
                        testTagName = "metric_tile_ping",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        label = if (isEn) "Network jitter" else "Độ biến động mạng",
                        value = "${session.jitterMs}ms",
                        valueColor = jitterMetricColor(session.jitterMs),
                        statusBadgeText = jitterStatusLabel(session.jitterMs, isEn),
                        onClick = { selectedMetricKey = if (selectedMetricKey == "jitter") null else "jitter" },
                        testTagName = "metric_tile_jitter",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatTile(
                        label = if (isEn) "Decode time" else "Thời gian giải mã",
                        value = "${session.avgDecodeMs}ms",
                        valueColor = decodeMetricColor(session.avgDecodeMs),
                        statusBadgeText = decodeStatusLabel(session.avgDecodeMs, isEn),
                        onClick = { selectedMetricKey = if (selectedMetricKey == "decode") null else "decode" },
                        testTagName = "metric_tile_decode",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        label = if (isEn) "Data buffer" else "Bộ đệm dữ liệu",
                        value = "${session.jitterBufferDelayMs}ms",
                        valueColor = DirectCyan,
                        statusBadgeText = "${session.avgFps} FPS",
                        onClick = { selectedMetricKey = if (selectedMetricKey == "buffer") null else "buffer" },
                        testTagName = "metric_tile_buffer",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStatTile(
                        label = if (isEn) "Dropped frames" else "Khung hình bị rớt",
                        value = "${session.framesDropped} ($dropPctFormatted)",
                        valueColor = dropRatioMetricColor(lagDiagnosis.dropRatioPct),
                        statusBadgeText = dropRatioStatusLabel(lagDiagnosis.dropRatioPct, isEn),
                        onClick = { selectedMetricKey = if (selectedMetricKey == "dropped") null else "dropped" },
                        testTagName = "metric_tile_dropped",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        label = if (isEn) "Temporary freeze" else "Đứng hình tạm thời",
                        value = if (isEn) "${session.freezeCount} times" else "${session.freezeCount} lần",
                        valueColor = if (session.freezeCount == 0) XboxNeonGreen else if (session.freezeCount <= 2) WarningAmber else JapanRed,
                        statusBadgeText = formatDurationCompact(session.durationSeconds, isEn),
                        onClick = { selectedMetricKey = if (selectedMetricKey == "freeze") null else "freeze" },
                        testTagName = "metric_tile_freeze",
                        modifier = Modifier.weight(1f)
                    )
                }

                // C2: Hiển thị giải thích <= 1 câu khi người dùng chạm vào một chỉ số
                val metricExplanationText = remember(selectedMetricKey, isEn) {
                    getMetricOneSentenceExplanation(selectedMetricKey, isEn)
                }
                if (metricExplanationText != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("metric_explanation_banner"),
                        shape = RoundedCornerShape(10.dp),
                        color = XboxSurfaceVariant.copy(alpha = 0.92f),
                        border = BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = metricExplanationText,
                            color = TextPrimary,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Trạng thái thiết bị
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = XboxSurface.copy(alpha = 0.9f),
                    border = BorderStroke(0.8.dp, XboxCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) "Device Status" else "Trạng thái máy",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        val statusText = when {
                            session.isThermalHot && session.isSlowDecode ->
                                if (isEn) "Device Hot • Slow Decode" else "Máy nóng • Giải mã chậm"
                            session.isThermalHot ->
                                if (isEn) "Device Hot" else "Máy nóng"
                            session.isSlowDecode ->
                                if (isEn) "Slow Decode (>12ms)" else "Giải mã chậm (>12ms)"
                            else ->
                                if (isEn) "Cool & Smooth" else "Mát & Ổn định"
                        }
                        val statusColor = when {
                            session.isThermalHot -> JapanRed
                            session.isSlowDecode -> WarningAmber
                            else -> XboxNeonGreen
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Gợi ý thông minh ở cuối chi tiết
                if (smartTip != null) {
                    SmartSuggestionBanner(text = smartTip)
                }

                // Nút Chơi lại, Xóa phiên và Đóng
                if (onPlayAgain != null) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("detail_play_again_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Play Again: ${session.gameTitle}" else "Chơi lại: ${session.gameTitle}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = JapanRed.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, JapanRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("delete_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = JapanRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isEn) "Delete Session" else "Xóa phiên này",
                            color = JapanRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        border = BorderStroke(1.dp, XboxCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = if (isEn) "Close" else "Đóng",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
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

private fun fpsMetricColor(fps: Int): Color = when {
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

private fun formatDurationCompact(durationSec: Long, isEn: Boolean): String {
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

private fun formatSessionDateTime(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}

private fun buildGlobalSmartTip(sessions: List<PlaySessionRecord>, isEn: Boolean): String? {
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

private fun buildSessionSmartTip(
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
