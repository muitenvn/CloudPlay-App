package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PlaySessionRecord
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
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxSlideFadeIn
import java.util.Locale

@Composable
internal fun SessionDetailDialog(
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
                .testTag("session_detail_dialog"),
            color = XboxSurfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sticky Header (always visible even when scrolling down the session report)
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
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatSessionDateTime(session.startTimeMillis)} • ${session.formattedNetworkLabel(isEn)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(XboxSurface)
                            .testTag("session_detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Đóng",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {

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
                            "Rớt khung hình %.1f%% • Đứng hình %.1f lần/phút • Độ trễ %dms • Biến động %dms",
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
                            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                                lineHeight = 13.5.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
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
                    color = XboxSurface
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

                // Nút Chơi lại, Xóa phiên và Đóng theo khuôn .dlg-btns
                if (onPlayAgain != null) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XboxNeonGreen,
                            contentColor = Color(0xFF03140B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("detail_play_again_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF03140B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Play Again: ${session.gameTitle}" else "Chơi lại: ${session.gameTitle}",
                            color = Color(0xFF03140B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF361B1A)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
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
                            text = if (isEn) "Delete Session" else "Xóa phiên",
                            color = JapanRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurface),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (isEn) "Close" else "Đóng",
                            color = TextPrimary,
                            fontSize = 12.5.sp,
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
}
