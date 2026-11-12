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
private val BADGE_SHAPE = RoundedCornerShape(999.dp)
private val SELECTED_CARD_BG = Color(0xFF0F2C1D)
private val DEFAULT_CARD_BG = XboxSurface
private val UNTESTED_BADGE_COLOR = Color(0xFFA5B8AE)

@Composable
internal fun ServerCard(
    server: ServerLocation,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    pingMs: Int,
    measurement: EndpointPingMeasurement? = null,
    @Suppress("UNUSED_PARAMETER") hasTriedPing: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isSmartBypass: Boolean = false,
    isGameActiveNow: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isTestingPings: Boolean = false,
    unlockStatus: RegionUnlockStatus,
    distanceKm: Int? = null,
    isTooFarForAutoSelect: Boolean = false,
    @Suppress("UNUSED_PARAMETER") precomputedSubtitle: String? = null,
    isEn: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val displayPingMs = if (pingMs > 0) pingMs else 0
    val isFallbackPortOnly = measurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY
    val isServerCurrentlyTesting = measurement?.status == ServerPingStatus.TESTING
    val isUnstablePing = measurement?.isUnstable == true || pingMs >= 1000
    val pingColor = when {
        pingMs <= 0 -> TextSecondary
        isFallbackPortOnly || isUnstablePing -> WarningAmber
        pingMs <= 95 -> XboxNeonGreen
        pingMs <= 150 -> WarningAmber
        else -> JapanRed
    }

    val activeBars = when {
        pingMs <= 0 -> 0
        isFallbackPortOnly -> 2
        pingMs <= 70 -> 4
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
    val unlockBadgeTextColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
        RegionUnlockStatus.FAILED -> JapanRed
        RegionUnlockStatus.UNTESTED -> UNTESTED_BADGE_COLOR
        RegionUnlockStatus.WRONG_REGION -> WarningAmber
    }
    val unlockBadgeBgColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> Color(0xFF0F2C1D)
        RegionUnlockStatus.FAILED -> Color(0xFF331A19)
        RegionUnlockStatus.UNTESTED -> Color(0xFF132019)
        RegionUnlockStatus.WRONG_REGION -> Color(0xFF2A2208)
    }
    val unlockBadgeBorderColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> Color(0xFF1E5238)
        RegionUnlockStatus.FAILED -> Color(0xFF5A2A28)
        RegionUnlockStatus.UNTESTED -> XboxCardBorder
        RegionUnlockStatus.WRONG_REGION -> Color(0xFF4A3B0E)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(SERVER_CARD_SHAPE)
            .background(if (isSelected) SELECTED_CARD_BG else DEFAULT_CARD_BG, SERVER_CARD_SHAPE)
            .border(
                width = 1.dp,
                color = if (isSelected) XboxNeonGreen else XboxCardBorder,
                shape = SERVER_CARD_SHAPE
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("server_card_${server.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(text = server.flag, fontSize = 19.sp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = server.name,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.testTag("server_card_subtitle_${server.id}")
                ) {
                    // Protocol pill (HTTP / SOCKS5)
                    Text(
                        text = server.protocol.uppercase(),
                        color = TextSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier
                            .background(Color(0xFF132019), BADGE_SHAPE)
                            .border(1.dp, XboxCardBorder, BADGE_SHAPE)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )

                    // Unlock status pill
                    Text(
                        text = unlockLabel,
                        color = unlockBadgeTextColor,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier
                            .background(unlockBadgeBgColor, BADGE_SHAPE)
                            .border(1.dp, unlockBadgeBorderColor, BADGE_SHAPE)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )

                    if (isRecommended) {
                        Text(
                            text = if (isEn) "★ Best" else "★ Tối ưu",
                            color = XboxNeonGreen,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            softWrap = false,
                            maxLines = 1
                        )
                    } else if (isTooFarForAutoSelect) {
                        Text(
                            text = if (isEn) "Far" else "Xa",
                            color = WarningAmber,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            softWrap = false,
                            maxLines = 1
                        )
                    } else if (distanceKm != null) {
                        Text(
                            text = "~${distanceKm}km",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .width(15.dp)
                    .height(13.dp)
            ) {
                val barWidthPx = 2.6.dp.toPx()
                val gapPx = 1.5.dp.toPx()
                val cornerPx = 1.dp.toPx()
                val cornerRadius = CornerRadius(cornerPx, cornerPx)
                for (bar in 1..4) {
                    val barHeightPx = (3.5f + bar * 2.2f).dp.toPx()
                    val x = (bar - 1) * (barWidthPx + gapPx)
                    val y = size.height - barHeightPx
                    drawRoundRect(
                        color = if (bar <= activeBars) pingColor else Color(0xFF284237),
                        topLeft = Offset(x, y),
                        size = Size(barWidthPx, barHeightPx),
                        cornerRadius = cornerRadius
                    )
                }
            }

            val mainPingValueText = when {
                isGameActiveNow && pingMs > 0 -> "$displayPingMs ms"
                isServerCurrentlyTesting -> "..."
                pingMs > 0 -> "$displayPingMs ms"
                else -> "--"
            }

            Text(
                text = mainPingValueText,
                color = if (pingMs > 0) pingColor else if (isServerCurrentlyTesting) DirectCyan else TextSecondary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
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
