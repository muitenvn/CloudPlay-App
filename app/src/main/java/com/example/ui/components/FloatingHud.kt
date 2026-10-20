package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import android.widget.Toast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import kotlin.math.roundToInt

enum class HudMode {
    MINIMIZED,
    COMPACT,
    EXPANDED
}

@Composable
fun FloatingHud(
    vpnState: VpnConnectionState,
    isGameActive: Boolean,
    virtualControllerEnabled: Boolean,
    physicalControllerConnected: Boolean,
    controllerName: String,
    onToggleNetwork: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateHome: () -> Unit,
    onReloadPage: () -> Unit,
    onToggleClarityBoost: (Boolean) -> Unit,
    clarityBoostActive: Boolean,
    translationActive: Boolean = true,
    translatedCount: Int = 0,
    onToggleTranslation: (Boolean) -> Unit = {},
    hideStreamStats: Boolean = true,
    onToggleHideStreamStats: (Boolean) -> Unit = {},
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus = com.example.cloudplay.NetworkOptimizerStatus(),
    telemetry: com.example.cloudplay.StreamTelemetry = com.example.cloudplay.StreamTelemetry(),
    adaptiveQualityState: com.example.cloudplay.AdaptiveQualityState = com.example.cloudplay.AdaptiveQualityState(),
    onSelectLadderStep: (Int) -> Unit = {},
    onToggleAutoAdapting: (Boolean) -> Unit = {},
    onSelectCellularMode: (com.example.data.CellularOptimizationMode) -> Unit = {},
    onToggleWifiSpeedFirst: (Boolean) -> Unit = {},
    gameProfile: com.example.data.GameOptimizationProfile? = null,
    onOpenGuide: (GuideSection) -> Unit = {},
    onOpenTranslator: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hudMode by rememberSaveable { mutableStateOf(HudMode.MINIMIZED) }
    var offsetX by rememberSaveable { mutableFloatStateOf(16f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(48f) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(lastInteractionTime) {
        delay(3500)
        isInteracting = false
    }

    val hudAlpha by animateFloatAsState(
        targetValue = if (isInteracting || hudMode == HudMode.EXPANDED) 0.95f else 0.52f,
        animationSpec = tween(400),
        label = "hud_alpha"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(maxWidthPx, maxHeightPx) {
            if (maxWidthPx > 0f && maxHeightPx > 0f) {
                val boundX = (maxWidthPx - 90f).coerceAtLeast(8f)
                val boundY = (maxHeightPx - 50f).coerceAtLeast(8f)
                offsetX = offsetX.coerceIn(8f, boundX)
                offsetY = offsetY.coerceIn(8f, boundY)
            }
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(maxWidthPx, maxHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        isInteracting = true
                        val boundX = (maxWidthPx - 90f).coerceAtLeast(8f)
                        val boundY = (maxHeightPx - 50f).coerceAtLeast(8f)
                        offsetX = (offsetX + dragAmount.x).coerceIn(8f, boundX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(8f, boundY)
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan.copy(alpha = 0.5f) else XboxNeonGreen.copy(alpha = 0.5f),
                    RoundedCornerShape(14.dp)
                )
                .testTag("floating_hud_container"),
            color = Color(0xF20B130E).copy(alpha = hudAlpha),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // MINIMIZED STATE: Ultra-compact Capsule Pill (tap to expand)
                if (hudMode == HudMode.MINIMIZED) {
                    val rawPing = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) vpnState.directLatencyMs else vpnState.latencyMs
                    val hasValidPing = rawPing != null && rawPing > 0
                    val pingColor = when {
                        !hasValidPing -> Color.Gray
                        rawPing < 50 -> DirectCyan
                        rawPing < 90 -> XboxNeonGreen
                        rawPing < 130 -> Color(0xFFFFB300)
                        else -> JapanRed
                    }
                    val pingText = if (!hasValidPing) {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡-- ms" else "${vpnState.currentServer.flag}-- ms"
                    } else {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡${rawPing}ms" else "${vpnState.currentServer.flag}${rawPing}ms"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                lastInteractionTime = System.currentTimeMillis()
                                isInteracting = true
                                hudMode = HudMode.COMPACT
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("hud_minimized_pill")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Text(
                            text = pingText,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = if (physicalControllerConnected || virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                } else {
                    // ==========================================
                    // COMPACT STATE: ULTRA-SLIM ROW WITH HOME BUTTON (~145dp)
                    // ==========================================
                    val compactRawPing = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) vpnState.directLatencyMs else vpnState.latencyMs
                    val compactHasValidPing = compactRawPing != null && compactRawPing > 0
                    val pingColor = when {
                        !compactHasValidPing -> Color.Gray
                        compactRawPing < 50 -> DirectCyan
                        compactRawPing < 90 -> XboxNeonGreen
                        compactRawPing < 130 -> Color(0xFFFFB300)
                        else -> JapanRed
                    }
                    val compactPingText = if (!compactHasValidPing) {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡-- ms" else "${vpnState.currentServer.flag}-- ms"
                    } else {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡${compactRawPing}ms" else "${vpnState.currentServer.flag}${compactRawPing}ms"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                    ) {
                        // 0. Dedicated Visible Home Button with Label
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DirectCyan.copy(alpha = 0.28f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isInteracting = true
                                    showExitConfirmDialog = true
                                }
                                .testTag("hud_home_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Trang chủ",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Home",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 1. Unified 1-Tap Network Pill: Ping + Mode
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = when (vpnState.mode) {
                                NetworkMode.DIRECT_NETWORK -> DirectCyan.copy(alpha = 0.22f)
                                NetworkMode.VPN_JAPAN -> JapanRed.copy(alpha = 0.22f)
                                NetworkMode.DISCONNECTED -> Color(0x33233A2D)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isInteracting = true
                                    onToggleNetwork()
                                }
                                .testTag("hud_toggle_network_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(pingColor)
                                )
                                Text(
                                    text = compactPingText,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "Direct" else "VPN",
                                    color = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan else JapanRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 2. Controller Toggle (Icon only)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (physicalControllerConnected) XboxGreen.copy(alpha = 0.35f)
                                    else if (virtualControllerEnabled) DirectCyan.copy(alpha = 0.25f)
                                    else Color(0x33233A2D),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isInteracting = true
                                    onToggleVirtualController()
                                }
                                .testTag("hud_toggle_controller_button")
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.5.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = "Tay cầm",
                                    tint = if (physicalControllerConnected) XboxNeonGreen else if (virtualControllerEnabled) DirectCyan else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // 3. Translation Toggle (Flag)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (translationActive) XboxNeonGreen.copy(alpha = 0.25f) else Color(0x33233A2D),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isInteracting = true
                                    onToggleTranslation(!translationActive)
                                }
                                .testTag("hud_translate_button")
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 2.5.dp)) {
                                Text(
                                    text = if (translationActive) "🇻🇳" else "🌐",
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // 4. Clarity Boost Toggle (Icon only)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (clarityBoostActive) XboxNeonGreen.copy(alpha = 0.25f) else Color(0x33233A2D),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isInteracting = true
                                    onToggleClarityBoost(!clarityBoostActive)
                                }
                                .testTag("hud_clarity_button")
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.5.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Làm nét",
                                    tint = if (clarityBoostActive) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // 5. Expand Game Bar Menu Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hudMode == HudMode.EXPANDED) XboxNeonGreen.copy(alpha = 0.3f) else Color(0x33233A2D),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isInteracting = true
                                    hudMode = if (hudMode == HudMode.EXPANDED) HudMode.COMPACT else HudMode.EXPANDED
                                }
                                .testTag("hud_expand_button")
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.5.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Menu tùy chỉnh",
                                    tint = if (hudMode == HudMode.EXPANDED) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // 6. Minimize to Corner Dot
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .clickable {
                                    lastInteractionTime = System.currentTimeMillis()
                                    hudMode = HudMode.MINIMIZED
                                }
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                                .testTag("hud_minimize_button")
                        ) {
                            Text(
                                text = "─",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // EXPANDED XBOX GAME BAR PANEL
                    AnimatedVisibility(
                        visible = hudMode == HudMode.EXPANDED,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                                .width(250.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Game Specific Optimization Card
                            if (gameProfile != null) {
                                val accent = Color(gameProfile.primaryAccentHex)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = accent.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = gameProfile.badge,
                                                color = accent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = gameProfile.recommendedFps,
                                                color = TextPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = gameProfile.title,
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Section: Telemetry Info
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF14221A)
                            ) {
                                val effectivePing = when {
                                    telemetry.rttMs > 0 -> telemetry.rttMs
                                    compactHasValidPing -> compactRawPing
                                    else -> null
                                }
                                val latencyDisplayText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
                                    if (effectivePing != null) "Direct (${effectivePing}ms)" else "Direct (-- ms)"
                                } else {
                                    if (effectivePing != null) "VPN ${vpnState.currentServer.flag} (${effectivePing}ms)" else "VPN ${vpnState.currentServer.flag} (-- ms)"
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.hud_stream_latency), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = latencyDisplayText,
                                            color = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan else JapanRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.hud_controller), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (physicalControllerConnected) controllerName else if (virtualControllerEnabled) stringResource(R.string.hud_virtual_gamepad) else stringResource(R.string.controller_disconnected),
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Dedicated 4G & Wi-Fi Optimization Engine Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                color = Color(0xFF101C16)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (networkStatus.isWifi) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                                                contentDescription = null,
                                                tint = XboxNeonGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "CHẤT LƯỢNG ĐỒ HỌA & HIỆU NĂNG MÁY",
                                                color = XboxNeonGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DirectCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "${networkStatus.effectiveBitrateMbps} Mbps",
                                                color = DirectCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${networkStatus.physicalType.icon} ${networkStatus.activeProfileName}",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // Real-time WebRTC telemetry metrics strip (Real decodeTimeMs, No fake fallback)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0A120E))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "FPS: ${if (telemetry.fps > 0) telemetry.fps else "--"}", color = XboxNeonGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Ping: ${if (telemetry.rttMs > 0) "${telemetry.rttMs}ms" else "--"}", color = if (telemetry.rttMs in 1..49) DirectCyan else if (telemetry.rttMs >= 100) JapanRed else TextPrimary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Bitrate: ${if (telemetry.bitrateMbps > 0) String.format("%.1f", telemetry.bitrateMbps) + "M" else "--"}", color = TextPrimary, fontSize = 8.5.sp)
                                        Text(text = "Giải mã: ${if (telemetry.decodeTimeMs > 0) String.format("%.1f", telemetry.decodeTimeMs) + "ms" else "--"}", color = if (telemetry.decodeTimeMs in 0.1..8.5) XboxNeonGreen else if (telemetry.decodeTimeMs > 12.0) JapanRed else DirectCyan, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Tự Động Biến Thiên Tự Do (1..20 Mbps) vs Cố Định
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(0.8.dp, DirectCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                        color = Color(0xFF0B1610)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(7.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bolt,
                                                        contentDescription = null,
                                                        tint = DirectCyan,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = if (adaptiveQualityState.isAutoAdapting) {
                                                            "TỰ ĐỘNG BIẾN THIÊN (1 ➔ 20 Mbps)"
                                                        } else {
                                                            "CHẾ ĐỘ CỐ ĐỊNH (KHÔNG TỰ ĐỔI)"
                                                        },
                                                        color = DirectCyan,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (adaptiveQualityState.isAutoAdapting) XboxNeonGreen.copy(alpha = 0.25f) else Color.DarkGray,
                                                    modifier = Modifier.clickable { onToggleAutoAdapting(!adaptiveQualityState.isAutoAdapting) }
                                                ) {
                                                    Text(
                                                        text = if (adaptiveQualityState.isAutoAdapting) "TỰ ĐỘNG: BẬT" else "ĐANG CỐ ĐỊNH",
                                                        color = if (adaptiveQualityState.isAutoAdapting) XboxNeonGreen else TextSecondary,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "🎯 ${adaptiveQualityState.currentStep.title}",
                                                color = TextPrimary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = adaptiveQualityState.qualityNotice,
                                                color = TextSecondary,
                                                fontSize = 8.sp,
                                                lineHeight = 11.sp
                                            )

                                            // Continuous 1..20 Mbps progress spectrum (No fixed 4-bar jump!)
                                            val liveMbps = adaptiveQualityState.smoothBitrateMbps.coerceIn(1.0, 20.0)
                                            val fillFraction = ((liveMbps - 1.0) / 19.0).toFloat().coerceIn(0.05f, 1f)
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(7.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF14241B))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fillFraction)
                                                            .height(7.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                                    colors = listOf(XboxNeonGreen, DirectCyan)
                                                                )
                                                            )
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (adaptiveQualityState.isAutoAdapting) {
                                                            "1M • 2 • 3 • 4 • 5 ... 10 ... 15 ... 20M (Tự do từng nấc)"
                                                        } else {
                                                            "Đang khóa cố định tại ${liveMbps.toInt()} Mbps"
                                                        },
                                                        color = TextSecondary,
                                                        fontSize = 7.2.sp
                                                    )
                                                    Text(
                                                        text = "${liveMbps} Mbps",
                                                        color = XboxNeonGreen,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (telemetry.estimatedDataSavedMb > 1.0) {
                                        Text(
                                            text = "🌱 Đã tiết kiệm ước tính: ~${String.format("%.1f", telemetry.estimatedDataSavedMb)} MB dung lượng data",
                                            color = XboxNeonGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Graphics & Data Mode Quick Selectors (2 hàng gọn gàng, không bị ép chữ)
                                    Text(
                                        text = "CHẾ ĐỘ ĐỒ HỌA & DUNG LƯỢNG:",
                                        color = TextSecondary,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val hudRows = listOf(
                                        listOf(
                                            Triple(com.example.data.CellularOptimizationMode.AUTO, "⚡ Tự Động", "Theo mạng & máy"),
                                            Triple(com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED, "❄️ Thấp (5M)", "Tiết kiệm data")
                                        ),
                                        listOf(
                                            Triple(com.example.data.CellularOptimizationMode.MEDIUM, "Trung Bình", "9M • Êm máy"),
                                            Triple(com.example.data.CellularOptimizationMode.HIGH, "Cao", "14M • 1080p"),
                                            Triple(com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED, "Rất Cao", "20M • Tối đa")
                                        )
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        hudRows.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                rowItems.forEach { (mode, label, sub) ->
                                                    val isSelected = networkStatus.activeProfileName.contains(
                                                        when (mode) {
                                                            com.example.data.CellularOptimizationMode.AUTO -> "Tự Động"
                                                            com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> "Thấp"
                                                            com.example.data.CellularOptimizationMode.MEDIUM -> "Trung Bình"
                                                            com.example.data.CellularOptimizationMode.HIGH -> "Cao ("
                                                            com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED -> "Rất Cao"
                                                        }
                                                    )
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .clickable { onSelectCellularMode(mode) },
                                                        color = if (isSelected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF14241B),
                                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(0.9.dp, XboxNeonGreen) else null
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) XboxNeonGreen else TextPrimary,
                                                                fontSize = 8.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = sub,
                                                                color = TextSecondary,
                                                                fontSize = 7.sp,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Low-Latency & Thermal Protection Row
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onToggleWifiSpeedFirst(!networkStatus.speedFirstPriority) },
                                        color = if (networkStatus.speedFirstPriority) DirectCyan.copy(alpha = 0.15f) else Color(0xFF14241B)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Ưu Tiên Độ Trễ Thấp & Giảm Tải Khi Mạng Yếu",
                                                    color = if (networkStatus.speedFirstPriority) DirectCyan else TextPrimary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Tự động giảm trễ đệm và hạ tải GPU khi sóng yếu hoặc máy nóng",
                                                    color = TextSecondary,
                                                    fontSize = 8.sp
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (networkStatus.speedFirstPriority) DirectCyan.copy(alpha = 0.3f) else Color.DarkGray
                                            ) {
                                                Text(
                                                    text = if (networkStatus.speedFirstPriority) "BẬT" else "TẮT",
                                                    color = if (networkStatus.speedFirstPriority) DirectCyan else TextSecondary,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Clarity Boost Quick Toggle
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleClarityBoost(!clarityBoostActive) },
                                color = Color(0xFF14221A)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "CLARITY BOOST (LÀM NÉT)", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (clarityBoostActive) "Đang bật (Tăng độ nét 1080p)" else "Đang tắt",
                                            color = if (clarityBoostActive) XboxNeonGreen else TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (clarityBoostActive) XboxNeonGreen.copy(alpha = 0.2f) else Color.DarkGray
                                    ) {
                                        Text(
                                            text = if (clarityBoostActive) "BẬT" else "TẮT",
                                            color = if (clarityBoostActive) XboxNeonGreen else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Stream Stats & Decode Profile Overlay Scrubber Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleHideStreamStats(!hideStreamStats) },
                                color = Color(0xFF131D1A)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = DirectCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "HỒ SƠ GIẢI MÃ & STREAM STATS",
                                                color = TextPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (hideStreamStats) "Đang ẩn bảng đè màn hình (Màn hình game sạch 100%)"
                                            else "Đang hiện bảng kỹ thuật web (Decode profile / Stream stats)",
                                            color = if (hideStreamStats) DirectCyan else TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Tự động chặn thông số Decode Profile che tầm nhìn khi chơi",
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (hideStreamStats) DirectCyan.copy(alpha = 0.25f) else Color.DarkGray
                                    ) {
                                        Text(
                                            text = if (hideStreamStats) "ẨN BẢNG" else "HIỆN BẢNG",
                                            color = if (hideStreamStats) DirectCyan else TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Dedicated Xbox Cloud Native Translator Card (GitHub Engine)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleTranslation(!translationActive) },
                                color = Color(0xFF14241B)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = XboxNeonGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "GOOGLE DỊCH THÔNG MINH (xCLOUD EDITION)",
                                                color = TextPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (translationActive) {
                                                if (translatedCount > 0) "Đang bật: Đã dịch $translatedCount mục (Google Dịch + 0ms UI)"
                                                else "Đang bật: Tự động dịch mô tả game & giao diện"
                                            } else "Đang tắt (Hiển thị ngôn ngữ gốc)",
                                            color = if (translationActive) XboxNeonGreen else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "✓ Dịch y hệt Google Dịch  ✓ Đã né tên game gốc  ✓ Không chạm stream",
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (translationActive) XboxNeonGreen.copy(alpha = 0.25f) else Color.DarkGray,
                                            modifier = Modifier.clickable { onToggleTranslation(!translationActive) }
                                        ) {
                                            Text(
                                                text = if (translationActive) "BẬT" else "TẮT",
                                                color = if (translationActive) XboxNeonGreen else TextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DirectCyan.copy(alpha = 0.2f),
                                            modifier = Modifier.clickable { onOpenTranslator() }
                                        ) {
                                            Text(
                                                text = "CÔNG CỤ DỊCH",
                                                color = DirectCyan,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom Actions: Home & Full Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Home / Exit Button
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showExitConfirmDialog = true }
                                        .testTag("hud_expanded_home_button"),
                                    color = JapanRed.copy(alpha = 0.22f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, JapanRed.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = JapanRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.hud_return_home), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }
                                }

                                // Rotate Screen Button (Landscape/Portrait)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleOrientation() }
                                        .testTag("hud_expanded_rotate_button"),
                                    color = Color.Black.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.ScreenRotation, contentDescription = "Xoay màn hình", tint = DirectCyan, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Xoay", color = DirectCyan, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }
                                }

                                // Full Settings Button
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenSettings() }
                                        .testTag("hud_expanded_settings_button"),
                                    color = Color.Black.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.tab_settings), color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Exit Game Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = {
                Text(text = stringResource(R.string.dialog_exit_title), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_exit_message),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onNavigateHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JapanRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.btn_confirm_exit), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary, fontSize = 12.sp)
                }
            },
            containerColor = XboxSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
