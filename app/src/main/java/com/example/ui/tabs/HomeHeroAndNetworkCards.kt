package com.example.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloudplay.GamepadStatus
import com.example.data.SessionHistoryRepository
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.EpicBlue
import com.example.ui.theme.GoldStar
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
import com.example.ui.theme.XboxSurfaceElevated
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity

private val HERO_CARD_SHAPE = RoundedCornerShape(14.dp)
private val HERO_BTN_SHAPE = RoundedCornerShape(12.dp)
private val HERO_BADGE_SHAPE = RoundedCornerShape(6.dp)
private val COCKPIT_CARD_SHAPE = RoundedCornerShape(12.dp)
private val COCKPIT_PILL_SHAPE = RoundedCornerShape(8.dp)
private val COCKPIT_BTN_SHAPE = RoundedCornerShape(9.dp)

@Composable
internal fun HeroConsolePlayDeck(
    @Suppress("UNUSED_PARAMETER") vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    onOpenXboxCloud: () -> Unit,
    onLaunchFortnite: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val config by SettingsRepository.config.collectAsState()
    val heroBannerRequest = remember(context) {
        coil.request.ImageRequest.Builder(context)
            .data(R.drawable.gamepass_hub_banner_1791394609676)
            .size(640, 360)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HERO_CARD_SHAPE)
            .background(XboxSurface, HERO_CARD_SHAPE)
            .border(1.dp, XboxCardBorder, HERO_CARD_SHAPE)
            .testTag("hero_console_play_deck")
    ) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val effectiveWidthDp = maxWidth.value / fontScale
        val isNarrow = maxWidth < 340.dp || effectiveWidthDp < 300f

        coil.compose.AsyncImage(
            model = heroBannerRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.32f,
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎮 XBOX CLOUD",
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier
                                .background(XboxNeonGreen.copy(alpha = 0.14f), HERO_BADGE_SHAPE)
                                .border(1.dp, XboxNeonGreen.copy(alpha = 0.35f), HERO_BADGE_SHAPE)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = if (gamepadStatus.hasPhysicalController) {
                                config.tr("🕹️ Tay cầm rời", "🕹️ Controller")
                            } else {
                                config.tr("📱 Cảm ứng • ⚡ 60FPS", "📱 Touch • ⚡ 60FPS")
                            },
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = stringResource(R.string.hero_title),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = if (isNarrow) 14.sp else 15.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onOpenGuide() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(XboxSurfaceVariant, CircleShape)
                            .border(1.dp, XboxCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.guide_button),
                            tint = DirectCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenXboxCloud,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 46.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.22f), HERO_BTN_SHAPE)
                        .testTag("hero_launch_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XboxGreen,
                        contentColor = Color.White
                    ),
                    shape = HERO_BTN_SHAPE,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (!isNarrow) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = stringResource(R.string.btn_launch_xcloud),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onLaunchFortnite,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 46.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.22f), HERO_BTN_SHAPE)
                        .testTag("hero_launch_fortnite_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EpicBlue,
                        contentColor = Color.White
                    ),
                    shape = HERO_BTN_SHAPE,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (!isNarrow) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = stringResource(R.string.btn_play_fortnite),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuickNetworkCockpitCard(
    vpnState: VpnConnectionState,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onNavigateToServers: () -> Unit = {}
) {
    val config by SettingsRepository.config.collectAsState()
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val optimizerStatus by com.example.cloudplay.NetworkOptimizer.status.collectAsState()
    val streamTelemetry by com.example.cloudplay.NetworkOptimizer.telemetry.collectAsState()
    val recentSessions by SessionHistoryRepository.sessions.collectAsState()
    val isStreaming = optimizerStatus.isLowLatencyStreamActive &&
        vpnState.activeStreamServerId == vpnState.currentServer.id &&
        streamTelemetry.rttMs > 0
    val lastSessionPing: Int? = com.example.vpn.VpnManager.getLastSessionPingForServer(
        server = vpnState.currentServer,
        sessions = recentSessions
    ).takeIf { it > 0 }

    val directPing: Int? = if (isStreaming) {
        streamTelemetry.rttMs.takeIf { it > 0 }
    } else {
        lastSessionPing
    }
    val hasPing = directPing != null && directPing > 0
    val activeBars = when {
        !hasPing -> 0
        directPing < 65 -> 4
        directPing < 120 -> 3
        directPing < 200 -> 2
        else -> 1
    }
    val signalTint = when {
        !hasPing -> TextSecondary
        activeBars == 4 -> XboxNeonGreen
        activeBars == 3 -> DirectCyan
        activeBars == 2 -> WarningAmber
        else -> JapanRed
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(COCKPIT_CARD_SHAPE)
            .background(XboxSurface, COCKPIT_CARD_SHAPE)
            .border(1.dp, XboxCardBorder, COCKPIT_CARD_SHAPE)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("network_cockpit_card")
    ) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val effectiveWidthDp = maxWidth.value / fontScale
        val isCompactRow = maxWidth < 340.dp || effectiveWidthDp < 310f

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cột trái: Dòng 1 hiển thị trọn vẹn cờ + tên máy chủ; Dòng 2 hiển thị vạch sóng + ping + Đổi máy chủ
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .clip(COCKPIT_PILL_SHAPE)
                    .clickable { onNavigateToServers() }
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = vpnState.currentServer.flag,
                        fontSize = 15.sp
                    )
                    Text(
                        text = vpnState.currentServer.name,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(13.dp)
                            .height(10.dp)
                    ) {
                        val barW = 2.1.dp.toPx()
                        val gap = 1.4.dp.toPx()
                        val cr = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        for (bar in 1..4) {
                            val barH = (2.5f + bar * 1.8f).dp.toPx()
                            val x = (bar - 1) * (barW + gap)
                            val y = size.height - barH
                            drawRoundRect(
                                color = if (bar <= activeBars) signalTint else XboxCardBorder,
                                topLeft = Offset(x, y),
                                size = Size(barW, barH),
                                cornerRadius = cr
                            )
                        }
                    }
                    Text(
                        text = if (hasPing) "${directPing}ms" else "--",
                        color = signalTint,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "•",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = when {
                            isStreaming -> config.tr("WebRTC • Đổi ▸", "WebRTC • Switch ▸")
                            isCompactRow -> config.tr("Đổi ▸", "Switch ▸")
                            else -> config.tr("Đổi máy chủ ▸", "Switch server ▸")
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Nút phải: Bật/Tắt VPN luôn được đo trước (không bị che hay cắt chữ ở màn hình hẹp)
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(COCKPIT_BTN_SHAPE)
                    .background(
                        if (isConnected) XboxSurfaceElevated else XboxGreen,
                        COCKPIT_BTN_SHAPE
                    )
                    .border(
                        width = if (isConnected) 1.dp else 0.dp,
                        color = if (isConnected) XboxNeonGreen.copy(alpha = 0.6f) else Color.Transparent,
                        shape = COCKPIT_BTN_SHAPE
                    )
                    .clickable { if (isConnected) onDisconnectVpn() else onConnectVpn() }
                    .padding(horizontal = if (isCompactRow) 9.dp else 11.dp, vertical = 7.dp)
                    .testTag("quick_vpn_toggle_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isCompactRow) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isConnected) XboxNeonGreen else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = if (isConnected) stringResource(R.string.btn_switch_direct) else stringResource(R.string.btn_enable_vpn),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isConnected) XboxNeonGreen else Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

