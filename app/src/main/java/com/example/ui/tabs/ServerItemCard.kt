package com.example.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.vpn.EndpointPingMeasurement
import com.example.vpn.RegionUnlockStatus
import com.example.vpn.ServerLocation
import com.example.vpn.ServerPingStatus

private val SERVER_CARD_SHAPE = RoundedCornerShape(12.dp)
private val BADGE_SHAPE = RoundedCornerShape(4.dp)
private val SELECTED_CARD_BG = Color(0xFF122118)
private val DEFAULT_CARD_BG = XboxSurface
private val UNTESTED_BADGE_COLOR = Color(0xFF9CA3AF)

@Composable
internal fun ServerCard(
    server: ServerLocation,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    pingMs: Int,
    measurement: EndpointPingMeasurement? = null,
    hasTriedPing: Boolean = false,
    isSmartBypass: Boolean = false,
    isGameActiveNow: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isTestingPings: Boolean = false,
    unlockStatus: RegionUnlockStatus,
    distanceKm: Int? = null,
    isTooFarForAutoSelect: Boolean = false,
    precomputedSubtitle: String? = null,
    isEn: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val displayPingMs = if (pingMs > 0) pingMs else 0
    val isFallbackPortOnly = measurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY
    val isServerCurrentlyTesting = measurement?.status == ServerPingStatus.TESTING
    val isUnstablePing = measurement?.isUnstable == true || pingMs >= 1000
    val pingColor = when {
        pingMs <= 0 -> Color.Gray
        isFallbackPortOnly || isUnstablePing -> WarningAmber
        pingMs <= 80 -> XboxNeonGreen
        pingMs <= 120 -> WarningAmber
        else -> JapanRed
    }

    val activeBars = when {
        pingMs <= 0 -> 0
        isFallbackPortOnly -> 2
        pingMs <= 65 -> 4
        pingMs <= 120 -> 3
        pingMs <= 200 -> 2
        else -> 1
    }

    val unlockLabel = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> if (isEn) "Unlocked" else "Đã mở khóa"
        RegionUnlockStatus.FAILED -> if (isEn) "Unlock Failed" else "Không mở khóa"
        RegionUnlockStatus.UNTESTED -> if (isEn) "Untested" else "Chưa thử"
        RegionUnlockStatus.WRONG_REGION -> if (isEn) "Wrong Region" else "Sai vùng"
    }
    val unlockBadgeColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
        RegionUnlockStatus.FAILED -> JapanRed
        RegionUnlockStatus.UNTESTED -> UNTESTED_BADGE_COLOR
        RegionUnlockStatus.WRONG_REGION -> WarningAmber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(SERVER_CARD_SHAPE)
            .background(if (isSelected) SELECTED_CARD_BG else DEFAULT_CARD_BG, SERVER_CARD_SHAPE)
            .border(
                width = if (isSelected) 1.4.dp else 0.8.dp,
                color = if (isSelected) XboxNeonGreen else XboxCardBorder,
                shape = SERVER_CARD_SHAPE
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("server_card_${server.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        ) {
            Text(text = server.flag, fontSize = 17.sp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = server.name,
                    color = if (isSelected) XboxNeonGreen else TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isRecommended) {
                        Text(
                            text = if (isEn) "★ BEST" else "★ ĐỀ XUẤT",
                            color = DirectCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier
                                .background(DirectCyan.copy(alpha = 0.16f), BADGE_SHAPE)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = unlockLabel,
                        color = unlockBadgeColor,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier
                            .background(unlockBadgeColor.copy(alpha = 0.14f), BADGE_SHAPE)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    if (isTooFarForAutoSelect) {
                        Text(
                            text = if (isEn) "Too far" else "Quá xa",
                            color = WarningAmber,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier
                                .background(WarningAmber.copy(alpha = 0.14f), BADGE_SHAPE)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                val baseSubtitle = precomputedSubtitle ?: remember(server.id, distanceKm, isEn) {
                    val distLabel = if (distanceKm != null) " • ~${distanceKm}km" else ""
                    "${server.localizedDescription(isEn)}$distLabel • ${server.ip}"
                }
                val subtitleText = "${server.protocol.uppercase()} • $baseSubtitle"
                Text(
                    text = subtitleText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("server_card_subtitle_${server.id}")
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(14.dp)
                            .height(11.dp)
                    ) {
                        val barWidthPx = 2.3.dp.toPx()
                        val gapPx = 1.5.dp.toPx()
                        val cornerPx = 1.dp.toPx()
                        val cornerRadius = CornerRadius(cornerPx, cornerPx)
                        for (bar in 1..4) {
                            val barHeightPx = (3f + bar * 1.8f).dp.toPx()
                            val x = (bar - 1) * (barWidthPx + gapPx)
                            val y = size.height - barHeightPx
                            drawRoundRect(
                                color = if (bar <= activeBars) pingColor else XboxCardBorder,
                                topLeft = Offset(x, y),
                                size = Size(barWidthPx, barHeightPx),
                                cornerRadius = cornerRadius
                            )
                        }
                    }

                    val mainPingValueText = when {
                        isGameActiveNow && pingMs > 0 -> "${displayPingMs}ms"
                        isServerCurrentlyTesting -> "..."
                        pingMs > 0 -> "${displayPingMs}ms"
                        else -> "--"
                    }

                    Text(
                        text = mainPingValueText,
                        color = if (pingMs > 0) pingColor else if (isServerCurrentlyTesting) DirectCyan else TextSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                val secondaryStatusText: String? = when {
                    isGameActiveNow -> "WebRTC"
                    isFallbackPortOnly -> if (isEn) "Fallback" else "Cổng phụ"
                    pingMs > 0 && isUnstablePing -> if (isEn) "Unstable" else "Chập chờn"
                    hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> {
                        if (isEn) "Offline" else "Không đo được"
                    }
                    isSmartBypass && !hasTriedPing && pingMs > 0 -> if (isEn) "Last" else "Phiên trước"
                    isSelected -> if (isEn) "Selected" else "Đang chọn"
                    else -> if (isEn) "Untested" else "Chưa đo"
                }

                if (secondaryStatusText != null) {
                    if (isSelected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = secondaryStatusText,
                                color = if (isFallbackPortOnly) WarningAmber else XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = secondaryStatusText,
                            color = if (isFallbackPortOnly || isUnstablePing) WarningAmber else TextSecondary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_server_${server.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = if (isEn) "Delete custom proxy" else "Xóa proxy tùy chỉnh",
                        tint = JapanRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
