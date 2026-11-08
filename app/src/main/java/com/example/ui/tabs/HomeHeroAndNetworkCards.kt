package com.example.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.theme.animateXboxIntAsState
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.ui.theme.xboxSlideFadeIn
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun HeroConsolePlayDeck(
    vpnState: VpnConnectionState,
    @Suppress("UNUSED_PARAMETER") gamepadStatus: GamepadStatus,
    onOpenXboxCloud: () -> Unit,
    onLaunchFortnite: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED
    val heroBorderBrush = remember(isConnected) {
        val glowAlpha = if (isConnected) 0.88f else 0.58f
        Brush.linearGradient(
            listOf(
                XboxNeonGreen.copy(alpha = glowAlpha),
                DirectCyan.copy(alpha = 0.65f),
                XboxNeonGreen.copy(alpha = glowAlpha)
            )
        )
    }
    val heroScrimBrush = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xDE040806),
                0.32f to Color(0xC8050A07),
                0.64f to Color(0xEB060B08),
                1.0f to XboxSurface
            )
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val heroBannerRequest = remember(context) {
        coil.request.ImageRequest.Builder(context)
            .data(R.drawable.gamepass_hub_banner_1791394609676)
            .size(640, 360)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .xboxSlideFadeIn(enabled = motionEnabled, key = "hero_deck", durationMillis = 160)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, heroBorderBrush, RoundedCornerShape(24.dp))
            .testTag("hero_console_play_deck"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            coil.compose.AsyncImage(
                model = heroBannerRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(heroScrimBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hàng trạng thái trên cùng của Hero Deck (FlowRow tự xuống dòng khi màn hình hẹp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xF207100B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (vpnState.mode) {
                                    NetworkMode.DIRECT_NETWORK -> DirectCyan.copy(alpha = 0.65f)
                                    NetworkMode.VPN_JAPAN -> XboxNeonGreen.copy(alpha = 0.65f)
                                    NetworkMode.DISCONNECTED -> XboxCardBorder
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (vpnState.mode) {
                                                NetworkMode.DIRECT_NETWORK -> DirectCyan
                                                NetworkMode.VPN_JAPAN -> XboxNeonGreen
                                                NetworkMode.DISCONNECTED -> WarningAmber
                                            }
                                        )
                                )
                                Text(
                                    text = when (vpnState.mode) {
                                        NetworkMode.DIRECT_NETWORK -> config.tr("Mạng trực tiếp", "Direct Net")
                                        NetworkMode.VPN_JAPAN -> "${config.tr("Mở vùng", "Unlocked")} ${vpnState.currentServer.flag}"
                                        NetworkMode.DISCONNECTED -> config.tr("Tự tắt VPN", "Auto VPN")
                                    },
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Nhãn hiển thị công nghệ giảm trễ 4G/5G & Wi-Fi
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xF206110C),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                DirectCyan.copy(alpha = 0.55f)
                            )
                        ) {
                            Text(
                                text = config.tr("Hỗ trợ 4G/5G & Wi-Fi", "4G/5G & Wi-Fi"),
                                color = DirectCyan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xEB0B150F),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onOpenGuide() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = stringResource(R.string.guide_button),
                                tint = DirectCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        color = Color(0xFFD6EAE0),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Hai nút khởi chạy chính với phụ đề thông số Console
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenXboxCloud,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.96f)
                            .testTag("hero_launch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XboxGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = stringResource(R.string.btn_launch_xcloud),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            Text(
                                text = config.tr("Full HD • Mượt 60FPS", "Full HD • Smooth 60FPS"),
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = onLaunchFortnite,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.96f)
                            .testTag("hero_launch_fortnite_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EpicBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = stringResource(R.string.btn_play_fortnite),
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            Text(
                                text = config.tr("Miễn phí • Vào ngay", "Free • Instant Play"),
                                color = GoldStar.copy(alpha = 0.95f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
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
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )
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

    // Khi đang có stream thực sự trên máy chủ này mới hiển thị RTT WebRTC;
    // Khi không có stream, chỉ lấy "Ping lần chơi gần nhất" của đúng máy chủ đang chọn từ SessionHistory (hoặc "--").
    val directPing: Int? = if (isStreaming) {
        streamTelemetry.rttMs.takeIf { it > 0 }
    } else {
        lastSessionPing
    }
    val hasPing = directPing != null && directPing > 0
    val animatedDirectPing by animateXboxIntAsState(
        targetValue = directPing ?: 0,
        motionEnabled = motionEnabled,
        label = "cockpit_ping_counter"
    )
    val activeBars = when {
        !hasPing -> 0
        directPing < 65 -> 4
        directPing < 120 -> 3
        directPing < 200 -> 2
        else -> 1
    }
    val targetSignalTint = when {
        !hasPing -> TextSecondary
        activeBars == 4 -> XboxNeonGreen
        activeBars == 3 -> DirectCyan
        activeBars == 2 -> WarningAmber
        else -> JapanRed
    }
    val signalTint by animateColorAsState(
        targetValue = targetSignalTint,
        animationSpec = tween(durationMillis = if (motionEnabled) 175 else 0),
        label = "cockpit_signal_tint"
    )

    val cockpitBorderBrush = remember(isConnected) {
        Brush.horizontalGradient(
            listOf(
                if (isConnected) XboxNeonGreen.copy(alpha = 0.55f) else XboxCardBorder,
                DirectCyan.copy(alpha = 0.35f)
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .xboxSlideFadeIn(enabled = motionEnabled, key = "network_cockpit", delayMillis = 20, durationMillis = 160)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, cockpitBorderBrush, RoundedCornerShape(18.dp))
            .testTag("network_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.97f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigateToServers() }
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = vpnState.currentServer.flag, fontSize = 19.sp)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = vpnState.currentServer.name,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isStreaming -> config.tr("RTT WebRTC • Chạm để đổi", "WebRTC RTT • Tap to switch")
                            lastSessionPing != null -> "${config.tr("Ping gần nhất", "Last ping")}: $lastSessionPing ms"
                            else -> "${config.tr("Ping gần nhất", "Last ping")}: --"
                        },
                        color = TextSecondary,
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cột sóng 4 vạch trực quan + Ping
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = signalTint.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        signalTint.copy(alpha = 0.48f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 4-bar micro signal meter
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier.height(11.dp)
                        ) {
                            for (bar in 1..4) {
                                val barHeight = (3 + bar * 2).dp
                                Box(
                                    modifier = Modifier
                                        .width(2.2.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (bar <= activeBars) signalTint
                                            else TextSecondary.copy(alpha = 0.28f)
                                        )
                                )
                            }
                        }
                        Text(
                            text = if (hasPing) "${animatedDirectPing}ms" else "--",
                            color = signalTint,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Button(
                onClick = { if (isConnected) onDisconnectVpn() else onConnectVpn() },
                modifier = Modifier
                    .height(38.dp)
                    .xboxPressScale(enabled = motionEnabled, pressedScale = 0.95f)
                    .testTag("quick_vpn_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) XboxSurfaceElevated else XboxGreen
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(11.dp)
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
}
