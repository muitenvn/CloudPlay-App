package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GameLibraryRepository
import com.example.data.PlaySessionRecord
import com.example.data.SettingsRepository
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

@Composable
internal fun WeeklySummaryCard(
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
internal fun SummaryStatTile(
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )
                if (!statusBadgeText.isNullOrBlank()) {
                    Text(
                        text = statusBadgeText,
                        color = valueColor,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
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
internal fun SmartSuggestionBanner(text: String) {
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
internal fun SessionHistoryItemCard(
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
internal fun MetricChip(
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
internal fun WarningBadgePill(
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
