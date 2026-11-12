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

private val HERO_CARD_SHAPE = RoundedCornerShape(16.dp)
private val HERO_BTN_SHAPE = RoundedCornerShape(12.dp)
private val HERO_BADGE_SHAPE = RoundedCornerShape(999.dp)
private val COCKPIT_CARD_SHAPE = RoundedCornerShape(12.dp)
private val COCKPIT_PILL_SHAPE = RoundedCornerShape(8.dp)
private val COCKPIT_BTN_SHAPE = RoundedCornerShape(8.dp)

@Composable
internal fun HeroConsolePlayDeck(
    vpnState: VpnConnectionState,
    @Suppress("UNUSED_PARAMETER") gamepadStatus: GamepadStatus,
    onOpenXboxCloud: () -> Unit,
    onLaunchFortnite: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val config by SettingsRepository.config.collectAsState()
    val recentSessions by SessionHistoryRepository.sessions.collectAsState()
    val streamTelemetry by com.example.cloudplay.NetworkOptimizer.telemetry.collectAsState()

    val heroBannerRequest = remember(context) {
        coil.request.ImageRequest.Builder(context)
            .data(R.drawable.gamepass_hub_banner_1791394609676)
            .size(640, 360)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    val lastSessionPing = com.example.vpn.VpnManager.getLastSessionPingForServer(
        server = vpnState.currentServer,
        sessions = recentSessions
    ).takeIf { it > 0 }
    val pingMs = streamTelemetry.rttMs.takeIf { it > 0 }
        ?: vpnState.directLatencyMs?.takeIf { it > 0 }
        ?: lastSessionPing
    val pingSuffix = if (pingMs != null && pingMs > 0) " · $pingMs ms" else ""
    val statusText = "${config.tr("Sẵn sàng", "Ready")} · ${vpnState.currentServer.name}$pingSuffix"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 196.dp)
            .clip(HERO_CARD_SHAPE)
            .background(Color(0xFF0A1D14), HERO_CARD_SHAPE)
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
            alpha = 0.25f,
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Status Pill + Help Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(HERO_BADGE_SHAPE)
                        .background(Color(0xD9060A08), HERO_BADGE_SHAPE)
                        .border(1.dp, XboxCardBorder, HERO_BADGE_SHAPE)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(XboxNeonGreen)
                    )
                    Text(
                        text = statusText,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9060A08), CircleShape)
                        .border(1.dp, XboxCardBorder, CircleShape)
                        .clickable { onOpenGuide() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.guide_button),
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Headline & Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = config.tr("Chơi Xbox Cloud", "Play Xbox Cloud"),
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = if (isNarrow) 20.sp else 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = config.tr(
                        "Hàng trăm game, ngay trên điện thoại.",
                        "Hundreds of console games, right on your phone."
                    ),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Chơi ngay (Green) + Fortnite (Dark bordered)
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
                        .testTag("hero_launch_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XboxNeonGreen,
                        contentColor = Color(0xFF03140B)
                    ),
                    shape = HERO_BTN_SHAPE,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (!isNarrow) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF03140B),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = config.tr("Chơi ngay", "Play Now"),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03140B),
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
                        .border(1.dp, XboxCardBorder, HERO_BTN_SHAPE)
                        .testTag("hero_launch_fortnite_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xD9060A08),
                        contentColor = TextPrimary
                    ),
                    shape = HERO_BTN_SHAPE,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (!isNarrow) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = "Fortnite",
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
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
        vpnState.directLatencyMs?.takeIf { it > 0 } ?: lastSessionPing
    }
    val hasPing = directPing != null && directPing > 0
    val activeBars = when {
        !hasPing -> 3
        directPing < 65 -> 4
        directPing < 120 -> 3
        directPing < 200 -> 2
        else -> 1
    }
    val signalTint = when {
        !hasPing -> XboxNeonGreen
        activeBars >= 3 -> XboxNeonGreen
        activeBars == 2 -> WarningAmber
        else -> JapanRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(COCKPIT_CARD_SHAPE)
            .background(XboxSurface, COCKPIT_CARD_SHAPE)
            .border(1.dp, XboxCardBorder, COCKPIT_CARD_SHAPE)
            .clickable { onNavigateToServers() }
            .padding(12.dp)
            .testTag("network_cockpit_card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Flag
        Text(
            text = vpnState.currentServer.flag,
            fontSize = 20.sp
        )

        // Server Name + Change Server Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = vpnState.currentServer.name,
                color = TextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = config.tr("Đổi máy chủ", "Change server"),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Signal Bars + Ping
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .width(15.dp)
                    .height(13.dp)
            ) {
                val barW = 2.6.dp.toPx()
                val gap = 1.5.dp.toPx()
                val cr = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                for (bar in 1..4) {
                    val barH = (3.5f + bar * 2.2f).dp.toPx()
                    val x = (bar - 1) * (barW + gap)
                    val y = size.height - barH
                    drawRoundRect(
                        color = if (bar <= activeBars) signalTint else Color(0xFF284237),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = cr
                    )
                }
            }
            if (hasPing) {
                Text(
                    text = "$directPing ms",
                    color = signalTint,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Compact VPN Toggle Button
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 38.dp)
                .clip(COCKPIT_BTN_SHAPE)
                .background(
                    if (isConnected) Color(0xFF0F2C1D) else XboxSurfaceVariant,
                    COCKPIT_BTN_SHAPE
                )
                .border(
                    width = 1.dp,
                    color = if (isConnected) XboxNeonGreen else XboxCardBorder,
                    shape = COCKPIT_BTN_SHAPE
                )
                .clickable { if (isConnected) onDisconnectVpn() else onConnectVpn() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .testTag("quick_vpn_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isConnected) config.tr("Tắt VPN", "VPN Off") else "VPN",
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) XboxNeonGreen else TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun SignalBarsIndicator(
    activeBars: Int,
    activeColor: Color = XboxNeonGreen
) {
    Canvas(
        modifier = Modifier
            .width(15.dp)
            .height(13.dp)
    ) {
        val barW = 2.6.dp.toPx()
        val gap = 1.5.dp.toPx()
        val cr = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        for (bar in 1..4) {
            val barH = (3.5f + bar * 2.2f).dp.toPx()
            val x = (bar - 1) * (barW + gap)
            val y = size.height - barH
            drawRoundRect(
                color = if (bar <= activeBars) activeColor else Color(0xFF284237),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = cr
            )
        }
    }
}

