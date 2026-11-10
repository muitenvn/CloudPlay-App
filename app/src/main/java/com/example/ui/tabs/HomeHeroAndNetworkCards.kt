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

private val HERO_CARD_SHAPE = RoundedCornerShape(14.dp)
private val HERO_BTN_SHAPE = RoundedCornerShape(10.dp)
private val COCKPIT_CARD_SHAPE = RoundedCornerShape(14.dp)
private val COCKPIT_PILL_SHAPE = RoundedCornerShape(8.dp)
private val COCKPIT_BTN_SHAPE = RoundedCornerShape(10.dp)

@Composable
internal fun HeroConsolePlayDeck(
    vpnState: VpnConnectionState,
    @Suppress("UNUSED_PARAMETER") gamepadStatus: GamepadStatus,
    onOpenXboxCloud: () -> Unit,
    onLaunchFortnite: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val borderColor = if (isConnected) XboxNeonGreen.copy(alpha = 0.5f) else XboxCardBorder

    val context = androidx.compose.ui.platform.LocalContext.current
    val heroBannerRequest = remember(context) {
        coil.request.ImageRequest.Builder(context)
            .data(R.drawable.gamepass_hub_banner_1791394609676)
            .size(640, 360)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HERO_CARD_SHAPE)
            .background(XboxSurface, HERO_CARD_SHAPE)
            .border(1.dp, borderColor, HERO_CARD_SHAPE)
            .testTag("hero_console_play_deck")
    ) {
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hero_title),
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(XboxSurfaceVariant, CircleShape)
                        .border(1.dp, XboxCardBorder, CircleShape)
                        .clickable { onOpenGuide() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.guide_button),
                        tint = DirectCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenXboxCloud,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("hero_launch_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XboxGreen,
                        contentColor = Color.White
                    ),
                    shape = HERO_BTN_SHAPE,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.btn_launch_xcloud),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onLaunchFortnite,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("hero_launch_fortnite_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EpicBlue,
                        contentColor = Color.White
                    ),
                    shape = HERO_BTN_SHAPE,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GoldStar,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.btn_play_fortnite),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
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
    val borderColor = if (isConnected) XboxNeonGreen.copy(alpha = 0.45f) else XboxCardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(COCKPIT_CARD_SHAPE)
            .background(XboxSurface, COCKPIT_CARD_SHAPE)
            .border(1.dp, borderColor, COCKPIT_CARD_SHAPE)
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .testTag("network_cockpit_card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(COCKPIT_PILL_SHAPE)
                .clickable { onNavigateToServers() }
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = vpnState.currentServer.flag, fontSize = 18.sp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = vpnState.currentServer.name,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isStreaming) {
                        config.tr("WebRTC • Chạm để đổi", "WebRTC • Tap to switch")
                    } else {
                        config.tr("Chạm để đổi máy chủ", "Tap to switch server")
                    },
                    color = TextSecondary,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .background(signalTint.copy(alpha = 0.12f), COCKPIT_PILL_SHAPE)
                    .border(0.8.dp, signalTint.copy(alpha = 0.45f), COCKPIT_PILL_SHAPE)
                    .padding(horizontal = 6.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(14.dp)
                        .height(11.dp)
                ) {
                    val barW = 2.2.dp.toPx()
                    val gap = 1.5.dp.toPx()
                    val cr = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    for (bar in 1..4) {
                        val barH = (3 + bar * 2).dp.toPx()
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Button(
            onClick = { if (isConnected) onDisconnectVpn() else onConnectVpn() },
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .height(36.dp)
                .testTag("quick_vpn_toggle_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) XboxSurfaceElevated else XboxGreen
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            shape = COCKPIT_BTN_SHAPE
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                contentDescription = null,
                tint = if (isConnected) XboxNeonGreen else Color.White,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConnected) stringResource(R.string.btn_switch_direct) else stringResource(R.string.btn_enable_vpn),
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
