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
import com.example.ui.components.gamepadClickable
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
    @Suppress("UNUSED_PARAMETER") avgPingMs: Int,
    @Suppress("UNUSED_PARAMETER") hasAnyHistory: Boolean,
    isEn: Boolean,
    @Suppress("UNUSED_PARAMETER") onClearAllClick: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .xboxSlideFadeIn(enabled = motionEnabled, key = "weekly_summary_card", durationMillis = 170)
            .testTag("weekly_summary_card"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = XboxSurface
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isEn) "Sessions" else "Số phiên",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "$sessionCount",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = XboxSurface
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isEn) "Play Time" else "Tổng thời gian",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = formatDurationCompact(totalDurationSec, isEn),
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        .gamepadClickable(cornerRadius = 12.dp) { onClick() }
                } else {
                    Modifier
                }
            )
            .then(
                if (testTagName != null) Modifier.testTag(testTagName) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = XboxSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
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
                    fontSize = 11.5.sp,
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
                        fontSize = 10.sp,
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
                fontSize = 14.sp,
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
        color = Color(0xFF16352A)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = XboxNeonGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                color = XboxNeonGreen,
                fontSize = 12.5.sp,
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .gamepadClickable(cornerRadius = 16.dp) { onClick() }
            .testTag("session_card_${session.id}"),
        shape = RoundedCornerShape(16.dp),
        color = XboxSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isImportingCatalog by GameLibraryRepository.isSyncingCatalog.collectAsState()
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(accentColorHex).copy(alpha = 0.85f), XboxDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                if (!isImportingCatalog && !coverImageUrl.isNullOrBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = remember(coverImageUrl) {
                            ImageRequest.Builder(context)
                                .data(coverImageUrl)
                                .size(144, 144)
                                .allowRgb565(true)
                                .crossfade(false)
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
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = session.gameTitle,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${formatSessionDateTime(session.startTimeMillis)} · ${formatDurationCompact(session.durationSeconds, isEn)}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pingTag = pingStatusLabel(session.avgPingMs, isEn)
                    val pingCol = pingMetricColor(session.avgPingMs)
                    Text(
                        text = if (isEn) {
                            "● Latency ${session.avgPingMs}ms · $pingTag"
                        } else {
                            "● Độ trễ ${session.avgPingMs}ms · $pingTag"
                        },
                        color = pingCol,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val secondTag = when {
                        session.isThermalHot -> if (isEn) "● Device Hot" else "● Máy nóng"
                        session.isSlowDecode -> if (isEn) "● Slow Decode (${session.avgDecodeMs}ms)" else "● Giải mã chậm (${session.avgDecodeMs}ms)"
                        else -> if (isEn) "● Decode ${session.avgDecodeMs}ms" else "● Giải mã ${session.avgDecodeMs}ms"
                    }
                    val secondCol = when {
                        session.isThermalHot -> JapanRed
                        session.isSlowDecode -> WarningAmber
                        else -> DirectCyan
                    }
                    Text(
                        text = secondTag,
                        color = secondCol,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onPlayAgain != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = XboxSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .gamepadClickable(cornerRadius = 10.dp) { onPlayAgain() }
                        .testTag("session_play_again_${session.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "Play" else "Chơi lại",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
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
