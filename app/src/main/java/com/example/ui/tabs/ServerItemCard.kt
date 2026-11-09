package com.example.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsRepository
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
import com.example.ui.theme.XboxSurfaceVariant
import com.example.ui.theme.animateXboxIntAsState
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.vpn.EndpointPingMeasurement
import com.example.vpn.RegionUnlockStatus
import com.example.vpn.ServerLocation
import com.example.vpn.ServerPingStatus

private val SELECTED_CARD_BORDER_BRUSH = Brush.horizontalGradient(listOf(XboxNeonGreen, DirectCyan))
private val DEFAULT_CARD_BORDER_BRUSH = Brush.horizontalGradient(listOf(XboxCardBorder, XboxCardBorder))
private val SELECTED_CARD_BG = XboxSurfaceVariant.copy(alpha = 0.96f)
private val DEFAULT_CARD_BG = XboxSurface.copy(alpha = 0.94f)
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
    isTestingPings: Boolean = false,
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
    val cardContainerColor = if (isSelected) SELECTED_CARD_BG else DEFAULT_CARD_BG

    val activeBars = when {
        pingMs <= 0 -> 0
        isFallbackPortOnly -> 2
        pingMs <= 65 -> 4
        pingMs <= 120 -> 3
        pingMs <= 200 -> 2
        else -> 1
    }

    // 3. HIỂN THỊ: mỗi thẻ máy chủ có nhãn nhỏ: "Đã mở khóa" (xanh), "Không mở khóa" (đỏ), "Chưa thử mở khóa" (xám), "Sai vùng" (vàng)
    val unlockLabel = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> if (isEn) "Unlocked" else "Đã mở khóa"
        RegionUnlockStatus.FAILED -> if (isEn) "Unlock Failed" else "Không mở khóa"
        RegionUnlockStatus.UNTESTED -> if (isEn) "Untested unlock" else "Chưa thử mở khóa"
        RegionUnlockStatus.WRONG_REGION -> if (isEn) "Wrong Region" else "Sai vùng"
    }
    val unlockBadgeColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
        RegionUnlockStatus.FAILED -> JapanRed
        RegionUnlockStatus.UNTESTED -> UNTESTED_BADGE_COLOR
        RegionUnlockStatus.WRONG_REGION -> WarningAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 1.6.dp else 1.dp,
                brush = if (isSelected) SELECTED_CARD_BORDER_BRUSH else DEFAULT_CARD_BORDER_BRUSH,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .testTag("server_card_${server.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Flag badge with neon ring when active
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (isSelected) XboxNeonGreen.copy(alpha = 0.16f) else XboxSurfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isSelected) XboxNeonGreen else XboxCardBorder,
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = server.flag, fontSize = 22.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = server.name,
                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isRecommended) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = DirectCyan.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, DirectCyan.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = if (isEn) "★ BEST" else "★ ĐỀ XUẤT",
                                    color = DirectCyan,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = XboxGreen.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, XboxNeonGreen.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = server.protocol.uppercase(),
                                color = XboxNeonGreen,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier.defaultMinSize(minWidth = 52.dp),
                            shape = RoundedCornerShape(5.dp),
                            color = unlockBadgeColor.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(0.7.dp, unlockBadgeColor.copy(alpha = 0.6f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unlockLabel,
                                    color = unlockBadgeColor,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        if (isTooFarForAutoSelect) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = WarningAmber.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, WarningAmber.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = if (isEn) "Too far" else "Quá xa",
                                    color = WarningAmber,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }

                    val subtitleText = precomputedSubtitle ?: remember(server.id, distanceKm, isEn) {
                        val distLabel = if (distanceKm != null) " • ~${distanceKm}km" else ""
                        val directPingNote = if (isEn) " • No data yet" else " • Chưa có dữ liệu"
                        "${server.localizedDescription(isEn)}$distLabel$directPingNote • ${server.ip}"
                    }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Signal Bars + Ping & Selection state
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 4-Bar Signal Quality Meter
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(13.dp)
                        ) {
                            for (bar in 1..4) {
                                val barHeight = (4 + bar * 2).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(
                                            if (bar <= activeBars) pingColor else XboxCardBorder
                                        )
                                )
                            }
                        }

                        val mainPingValueText = when {
                            isGameActiveNow && pingMs > 0 -> "${displayPingMs}ms"
                            isGameActiveNow -> "--"
                            isServerCurrentlyTesting -> "..."
                            isFallbackPortOnly && pingMs > 0 -> "${displayPingMs}ms"
                            pingMs > 0 && isUnstablePing -> "${displayPingMs}ms • ${if (isEn) "Unstable" else "Mạng chập chờn"}"
                            pingMs > 0 -> "${displayPingMs}ms"
                            hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> if (isEn) "Unreachable" else "Không đo được"
                            else -> if (isEn) "Not tested" else "Chưa đo"
                        }

                        Text(
                            text = mainPingValueText,
                            color = if (pingMs > 0) pingColor else if (isServerCurrentlyTesting) DirectCyan else pingColor,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (pingMs <= 0 || isFallbackPortOnly || isUnstablePing) 10.sp else 12.sp
                        )
                    }

                    val metricLabelText = when {
                        isGameActiveNow -> "RTT WebRTC"
                        isServerCurrentlyTesting -> if (isEn) "Testing..." else "Đang đo..."
                        isFallbackPortOnly -> {
                            if (isEn) "Fallback port • Primary closed" else "Cổng phụ • Cổng chính đóng"
                        }
                        measurement?.status == ServerPingStatus.MEASURED_PRIMARY && pingMs > 0 -> {
                            when {
                                isUnstablePing -> if (isEn) "Latency (Unstable)" else "Độ trễ mạng (Mạng chập chờn)"
                                pingMs <= 80 -> if (isEn) "Latency (Good)" else "Độ trễ mạng (Tốt)"
                                pingMs <= 120 -> if (isEn) "Latency (Fair)" else "Độ trễ mạng (Trung bình)"
                                else -> if (isEn) "Latency (High)" else "Độ trễ mạng (Cao)"
                            }
                        }
                        hasTriedPing && pingMs > 0 -> if (isEn) "Network latency" else "Độ trễ mạng"
                        hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> if (isEn) "Unreachable" else "Không đo được"
                        isSmartBypass && pingMs > 0 -> if (isEn) "Last session latency" else "Độ trễ phiên gần nhất"
                        else -> if (isEn) "Not tested" else "Chưa đo"
                    }

                    if (isSelected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = metricLabelText,
                                color = if (isFallbackPortOnly) WarningAmber else XboxNeonGreen,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = metricLabelText,
                            color = if (isFallbackPortOnly) WarningAmber else TextSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp
                        )
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
