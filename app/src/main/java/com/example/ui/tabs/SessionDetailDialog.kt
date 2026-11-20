package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.data.PlaySessionRecord
import com.example.ui.components.AdaptiveFlatActionButton
import com.example.ui.components.AdaptivePopupFrame
import com.example.ui.components.AdaptivePopupScope
import com.example.ui.components.AdaptiveTwoColumnLayout
import com.example.ui.components.PingSparklineChart
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
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
    val chartSamples = remember(session.pingSamples, session.avgPingMs) {
        if (session.pingSamples.isNotEmpty()) session.pingSamples else listOf(session.avgPingMs.coerceAtLeast(25))
    }
    val minPing = remember(chartSamples) { chartSamples.minOrNull() ?: session.avgPingMs }
    val maxPing = remember(chartSamples) { chartSamples.maxOrNull() ?: session.avgPingMs }
    val lagDiagnosis = remember(session, isEn) { diagnoseSessionLag(session, isEn) }
    var selectedMetricKey by remember { mutableStateOf<String?>(null) }
    val primaryFocusRequester = remember { FocusRequester() }

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

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "session_detail_dialog",
        defaultFocusRequester = primaryFocusRequester,
        wideWidthFraction = 0.94f,
        portraitWidthFraction = 0.94f
    ) {
        val leftScroll = rememberScrollState()
        val rightScroll = rememberScrollState()
        val portraitScroll = rememberScrollState()

        @Composable
        fun AdaptivePopupScope.HeaderRow() {
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
                        fontSize = rSp(16f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatSessionDateTime(session.startTimeMillis)} • ${session.formattedNetworkLabel(isEn)}",
                        color = TextSecondary,
                        fontSize = rSp(11.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(gapSmall))

                AdaptiveFlatActionButton(
                    onClick = onDismiss,
                    isPrimary = false,
                    gamepadHint = "B",
                    modifier = Modifier.testTag("session_detail_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isEn) "Close" else "Đóng",
                        tint = TextSecondary,
                        modifier = Modifier.size(rDp(16f))
                    )
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.VerdictAndLagDiagnostics() {
            Column(verticalArrangement = Arrangement.spacedBy(gapMed)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_verdict_banner"),
                    shape = RoundedCornerShape(sectionCorner),
                    color = verdictColor.copy(alpha = 0.14f),
                    border = BorderStroke(borderThin, verdictColor.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = padInner, vertical = gapMed),
                        verticalArrangement = Arrangement.spacedBy(rDp(3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(gapSmall)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(rDp(6f)),
                                color = verdictColor.copy(alpha = 0.24f),
                                border = BorderStroke(borderThin, verdictColor)
                            ) {
                                Text(
                                    text = verdictTitle,
                                    color = verdictColor,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = rSp(12f),
                                    modifier = Modifier.padding(horizontal = rDp(7f), vertical = rDp(2f))
                                )
                            }
                            Text(
                                text = if (isEn) "Session Verdict" else "Đánh giá phiên chơi",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = rSp(11.5f)
                            )
                        }
                        Text(
                            text = verdictSummary,
                            color = TextSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = rSp(9.5f),
                            lineHeight = rSp(13.5f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("why_lag_diagnostic_card"),
                    shape = RoundedCornerShape(sectionCorner),
                    color = XboxSurface,
                    border = BorderStroke(borderThin, DirectCyan.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = padInner, vertical = gapMed),
                        verticalArrangement = Arrangement.spacedBy(rDp(3f))
                    ) {
                        Text(
                            text = if (isEn) "WHY WAS THERE LAG?" else "VÌ SAO CÓ LAG?",
                            color = DirectCyan,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = rSp(10f),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = lagDiagnosis.evidenceSentence,
                            color = TextPrimary,
                            fontSize = rSp(10.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (lagDiagnosis.suggestionSentence.isNotBlank()) {
                            Text(
                                text = lagDiagnosis.suggestionSentence,
                                color = TextSecondary,
                                fontSize = rSp(10.2f)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(sectionCorner),
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(padInner),
                        verticalArrangement = Arrangement.spacedBy(gapSmall)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(rDp(4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(rDp(13f))
                                )
                                Text(
                                    text = if (isEn) "NETWORK LATENCY CHART" else "BIỂU ĐỒ ĐỘ TRỄ MẠNG",
                                    color = TextSecondary,
                                    fontSize = rSp(9.5f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(gapSmall))
                            Text(
                                text = if (isEn) "Min ${minPing}ms • Max ${maxPing}ms" else "Thấp ${minPing}ms • Cao ${maxPing}ms",
                                color = DirectCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = rSp(9.5f),
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
                                .height(rDp(48f))
                        )
                    }
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.MetricsAndStatusBlock() {
            val dropPctFormatted = String.format(Locale.US, "%.1f%%", lagDiagnosis.dropRatioPct)
            Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
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
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
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
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
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

                val metricExplanationText = remember(selectedMetricKey, isEn) {
                    getMetricOneSentenceExplanation(selectedMetricKey, isEn)
                }
                if (metricExplanationText != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("metric_explanation_banner"),
                        shape = RoundedCornerShape(chipCorner),
                        color = XboxSurfaceVariant,
                        border = BorderStroke(borderThin, DirectCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = metricExplanationText,
                            color = TextPrimary,
                            fontSize = rSp(10.2f),
                            modifier = Modifier.padding(horizontal = padInner, vertical = gapSmall)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(chipCorner),
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = padInner, vertical = gapMed),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEn) "Device Status" else "Trạng thái máy",
                            color = TextSecondary,
                            fontSize = rSp(10.8f)
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
                            fontSize = rSp(10.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (smartTip != null) {
                    SmartSuggestionBanner(text = smartTip)
                }
            }
        }

        @Composable
        fun AdaptivePopupScope.ActionButtonsBlock() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                if (onPlayAgain != null) {
                    AdaptiveFlatActionButton(
                        onClick = onPlayAgain,
                        isPrimary = true,
                        gamepadHint = "A",
                        focusRequester = primaryFocusRequester,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_play_again_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF03140B),
                            modifier = Modifier.size(rDp(15f))
                        )
                        Spacer(modifier = Modifier.width(rDp(5f)))
                        Text(
                            text = if (isEn) "Play Again: ${session.gameTitle}" else "Chơi lại: ${session.gameTitle}",
                            color = Color(0xFF03140B),
                            fontSize = rSp(12f),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    AdaptiveFlatActionButton(
                        onClick = onDelete,
                        isPrimary = false,
                        containerColor = Color(0xFF361B1A),
                        borderColor = JapanRed.copy(alpha = 0.65f),
                        contentColor = JapanRed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = JapanRed,
                            modifier = Modifier.size(rDp(14f))
                        )
                        Spacer(modifier = Modifier.width(rDp(4f)))
                        Text(
                            text = if (isEn) "Delete Session" else "Xóa phiên",
                            color = JapanRed,
                            fontSize = rSp(11.5f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AdaptiveFlatActionButton(
                        onClick = onDismiss,
                        isPrimary = false,
                        gamepadHint = "B",
                        focusRequester = if (onPlayAgain == null) primaryFocusRequester else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isEn) "Close" else "Đóng",
                            color = TextPrimary,
                            fontSize = rSp(11.5f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 1.04f,
            rightWeight = 0.96f,
            leftColumn = {
                HeaderRow()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = winH * 0.72f)
                        .verticalScroll(leftScroll),
                    verticalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    VerdictAndLagDiagnostics()
                }
            },
            rightColumn = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = winH * 0.60f)
                        .verticalScroll(rightScroll),
                    verticalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    MetricsAndStatusBlock()
                }
                ActionButtonsBlock()
            },
            portraitColumn = {
                HeaderRow()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = winH * 0.64f)
                        .verticalScroll(portraitScroll),
                    verticalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    VerdictAndLagDiagnostics()
                    MetricsAndStatusBlock()
                }
                ActionButtonsBlock()
            }
        )
    }
}
