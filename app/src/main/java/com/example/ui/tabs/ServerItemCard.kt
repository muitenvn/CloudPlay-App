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
    isEn: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val animatedPingMs by animateXboxIntAsState(
        targetValue = if (pingMs > 0) pingMs else 0,
        motionEnabled = motionEnabled,
        label = "server_ping_${server.id}"
    )
    val isFallbackPortOnly = measurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY
    val isServerCurrentlyTesting = measurement?.status == ServerPingStatus.TESTING
    val isUnstablePing = measurement?.isUnstable == true || pingMs >= 1000
    val targetPingColor = when {
        pingMs <= 0 -> Color.Gray
        isFallbackPortOnly || isUnstablePing -> WarningAmber
        pingMs <= 80 -> XboxNeonGreen
        pingMs <= 120 -> WarningAmber
        else -> JapanRed
    }
    val pingColor by animateColorAsState(
        targetValue = targetPingColor,
        animationSpec = tween(durationMillis = if (motionEnabled) 170 else 0),
        label = "server_ping_color"
    )
    val cardContainerColor by animateColorAsState(
        targetValue = if (isSelected) XboxSurfaceVariant.copy(alpha = 0.96f) else XboxSurface.copy(alpha = 0.94f),
        animationSpec = tween(durationMillis = if (motionEnabled) 170 else 0),
        label = "server_card_bg"
    )

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
        RegionUnlockStatus.UNLOCKED -> config.tr("Đã mở khóa", "Unlocked")
        RegionUnlockStatus.FAILED -> config.tr("Không mở khóa", "Unlock Failed")
        RegionUnlockStatus.UNTESTED -> config.tr("Chưa thử mở khóa", "Untested unlock")
        RegionUnlockStatus.WRONG_REGION -> config.tr("Sai vùng", "Wrong Region")
    }
    val unlockBadgeColor = when (unlockStatus) {
        RegionUnlockStatus.UNLOCKED -> XboxNeonGreen
        RegionUnlockStatus.FAILED -> JapanRed
        RegionUnlockStatus.UNTESTED -> Color(0xFF9CA3AF)
        RegionUnlockStatus.WRONG_REGION -> WarningAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.97f)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 1.6.dp else 1.dp,
                brush = if (isSelected) {
                    Brush.horizontalGradient(listOf(XboxNeonGreen, DirectCyan))
                } else {
                    Brush.horizontalGradient(listOf(XboxCardBorder, XboxCardBorder))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("server_card_${server.id}"),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 3.dp)
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
                                    text = config.tr("★ ĐỀ XUẤT", "★ BEST"),
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
                                    text = config.tr("Quá xa", "Too far"),
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

                    val distLabel = if (distanceKm != null) " • ~${distanceKm}km" else ""
                    val recentSessionsForCard by com.example.data.SessionHistoryRepository.sessions.collectAsState()
                    val regionSessionStats = remember(server.name, server.countryCode, recentSessionsForCard) {
                        com.example.data.SessionHistoryRepository.getRegionSessionStats(
                            serverName = server.name,
                            countryCode = server.countryCode,
                            sessionsList = recentSessionsForCard
                        )
                    }
                    val directPingNote = if (regionSessionStats != null && regionSessionStats.sampleCount > 0) {
                        if (isEn) {
                            " • ~${regionSessionStats.avgRttMs}ms • last ${regionSessionStats.sampleCount} sessions"
                        } else {
                            " • ~${regionSessionStats.avgRttMs}ms • ${regionSessionStats.sampleCount} phiên gần nhất"
                        }
                    } else {
                        if (isEn) " • No data yet" else " • Chưa có dữ liệu"
                    }
                    Text(
                        text = "${server.localizedDescription(isEn)}$distLabel$directPingNote • ${server.ip}",
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
                            isGameActiveNow && pingMs > 0 -> "${animatedPingMs}ms"
                            isGameActiveNow -> "--"
                            isServerCurrentlyTesting -> "..."
                            isFallbackPortOnly && pingMs > 0 -> "${animatedPingMs}ms"
                            pingMs > 0 && isUnstablePing -> "${animatedPingMs}ms • ${config.tr("Mạng chập chờn", "Unstable")}"
                            pingMs > 0 -> "${animatedPingMs}ms"
                            hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> config.tr("Không đo được", "Unreachable")
                            else -> config.tr("Chưa đo", "Not tested")
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
                        isServerCurrentlyTesting -> config.tr("Đang đo...", "Testing...")
                        isFallbackPortOnly -> {
                            config.tr("Cổng phụ • Cổng chính đóng", "Fallback port • Primary closed")
                        }
                        measurement?.status == ServerPingStatus.MEASURED_PRIMARY && pingMs > 0 -> {
                            when {
                                isUnstablePing -> config.tr("Độ trễ mạng (Mạng chập chờn)", "Latency (Unstable)")
                                pingMs <= 80 -> config.tr("Độ trễ mạng (Tốt)", "Latency (Good)")
                                pingMs <= 120 -> config.tr("Độ trễ mạng (Trung bình)", "Latency (Fair)")
                                else -> config.tr("Độ trễ mạng (Cao)", "Latency (High)")
                            }
                        }
                        hasTriedPing && pingMs > 0 -> config.tr("Độ trễ mạng", "Network latency")
                        hasTriedPing || measurement?.status == ServerPingStatus.UNREACHABLE -> config.tr("Không đo được", "Unreachable")
                        isSmartBypass && pingMs > 0 -> config.tr("Độ trễ phiên gần nhất", "Last session latency")
                        else -> config.tr("Chưa đo", "Not tested")
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
