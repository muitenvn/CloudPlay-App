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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    videoTopRatio: Float = 0f,
    lowEndDeviceMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hudMode by rememberSaveable { mutableStateOf(HudMode.MINIMIZED) }
    var offsetX by rememberSaveable { mutableFloatStateOf(24f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(24f) }
    var hudWidthPx by remember { mutableStateOf(220) }
    var hudHeightPx by remember { mutableStateOf(96) }
    var isDragging by remember { mutableStateOf(false) }
    var lastOrientationPortrait by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isInteracting by remember { mutableStateOf(true) }

    // 2. THEO DÕI NHIỆT ĐỘ MÁY (API 29+, bỏ qua trên máy thấp hơn) & PIN
    val isThermalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var thermalStatus by remember { mutableIntStateOf(0) }
    var showThermalWarningBanner by remember { mutableStateOf(false) }
    var batteryPercent by remember { mutableIntStateOf(-1) }
    var isBatteryCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val powerManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        } else {
            null
        }

        val thermalListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager.OnThermalStatusChangedListener { newStatus ->
                val wasHot = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
                thermalStatus = newStatus
                val isNowHot = newStatus >= PowerManager.THERMAL_STATUS_MODERATE
                showThermalWarningBanner = isNowHot
                if (isNowHot && !wasHot) {
                    try {
                        Toast.makeText(
                            context,
                            "Máy đang nóng, nên nghỉ hoặc hạ chất lượng",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) {}
                }
            }
        } else {
            null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            try {
                val initialThermal = powerManager.currentThermalStatus
                thermalStatus = initialThermal
                showThermalWarningBanner = initialThermal >= PowerManager.THERMAL_STATUS_MODERATE
                powerManager.addThermalStatusListener(context.mainExecutor, thermalListener)
            } catch (_: Exception) {}
        }

        fun updateBatteryFromIntent(intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryPercent = ((level * 100f) / scale.toFloat()).roundToInt().coerceIn(0, 100)
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                updateBatteryFromIntent(intent)
            }
        }

        try {
            val stickyIntent = context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            updateBatteryFromIntent(stickyIntent)
        } catch (_: Exception) {}

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
                try {
                    powerManager.removeThermalStatusListener(thermalListener)
                } catch (_: Exception) {}
            }
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }
    }

    // Phân loại biểu tượng nhiệt trên HUD: mát / ấm / nóng
    val thermalIcon = when {
        !isThermalSupported -> ""
        thermalStatus >= 2 -> "🔥"
        thermalStatus == 1 -> "🌡️"
        else -> "❄️"
    }
    val thermalLabel = when {
        !isThermalSupported -> ""
        thermalStatus >= 2 -> "Nóng"
        thermalStatus == 1 -> "Ấm"
        else -> "Mát"
    }
    val thermalColor = when {
        thermalStatus >= 2 -> JapanRed
        thermalStatus == 1 -> Color(0xFFFFB300)
        else -> DirectCyan
    }

    fun markUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        isInteracting = true
    }

    // A.4: Tự mờ còn khoảng 35% độ đục sau 3 giây không chạm, chạm vào thì hiện rõ lại
    LaunchedEffect(lastInteractionTime, hudMode) {
        if (hudMode != HudMode.EXPANDED) {
            delay(3000)
            isInteracting = false
        } else {
            isInteracting = true
        }
    }

    val targetAlpha = if (isInteracting || isDragging || hudMode == HudMode.EXPANDED) 1.0f else 0.35f
    val hudAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(if (lowEndDeviceMode) 0 else 350),
        label = "hud_alpha"
    )

    val safeInsets = WindowInsets.safeDrawing
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenMaxWidth = maxWidth
        val screenMaxHeight = maxHeight
        val maxWidthPx = with(density) { screenMaxWidth.toPx() }
        val maxHeightPx = with(density) { screenMaxHeight.toPx() }
        val isPortrait = maxHeightPx > maxWidthPx

        val edgeMarginPx = with(density) { 8.dp.toPx() }
        val safeLeftPx = (safeInsets.getLeft(density, layoutDirection).toFloat() + edgeMarginPx).coerceAtLeast(edgeMarginPx)
        val safeTopPx = (safeInsets.getTop(density).toFloat() + edgeMarginPx).coerceAtLeast(edgeMarginPx)
        val safeRightPx = (safeInsets.getRight(density, layoutDirection).toFloat() + edgeMarginPx).coerceAtLeast(edgeMarginPx)
        val safeBottomPx = (safeInsets.getBottom(density).toFloat() + edgeMarginPx).coerceAtLeast(edgeMarginPx)

        val maxBoundX = (maxWidthPx - safeRightPx - hudWidthPx.toFloat()).coerceAtLeast(safeLeftPx)
        val maxBoundY = (maxHeightPx - safeBottomPx - hudHeightPx.toFloat()).coerceAtLeast(safeTopPx)

        // Tính vị trí dải đen phía trên vùng video thực tế khi ở màn DỌC (Portrait)
        val actualVideoTopPx = if (isPortrait) {
            if (videoTopRatio > 0.04f) {
                maxHeightPx * videoTopRatio
            } else {
                ((maxHeightPx - (maxWidthPx * 9f / 16f)) / 2f).coerceAtLeast(safeTopPx)
            }
        } else {
            0f
        }
        val portraitBlackBarMaxY = if (isPortrait) {
            (actualVideoTopPx - hudHeightPx.toFloat() - with(density) { 6.dp.toPx() }).coerceIn(safeTopPx, maxBoundY)
        } else {
            maxBoundY
        }

        // A.1, A.2, A.3: Đặt vị trí mặc định chuẩn khi xoay màn DỌC / NGANG và luôn kẹp trong vùng an toàn (trừ tai thỏ)
        LaunchedEffect(maxWidthPx, maxHeightPx, hudWidthPx, hudHeightPx, isPortrait, videoTopRatio, hudMode) {
            if (maxWidthPx > 0f && maxHeightPx > 0f) {
                val orientationChanged = lastOrientationPortrait != isPortrait
                if (orientationChanged) {
                    lastOrientationPortrait = isPortrait
                    if (isPortrait) {
                        // Màn DỌC: đặt HUD vào dải đen phía trên vùng video (không đè lên hình game)
                        offsetX = safeLeftPx
                        offsetY = portraitBlackBarMaxY
                    } else {
                        // Màn NGANG: mặc định ở góc trên trái hoặc trên phải
                        val preferRight = (offsetX + hudWidthPx / 2f) > (maxWidthPx * 0.5f)
                        offsetX = if (preferRight) maxBoundX else safeLeftPx
                        offsetY = safeTopPx
                    }
                } else {
                    offsetX = offsetX.coerceIn(safeLeftPx, maxBoundX)
                    if (isPortrait && hudMode != HudMode.EXPANDED) {
                        // Giữ HUD nằm trong dải đen phía trên video ở màn dọc để không đè lên hình game
                        offsetY = offsetY.coerceIn(safeTopPx, portraitBlackBarMaxY)
                    } else {
                        offsetY = offsetY.coerceIn(safeTopPx, maxBoundY)
                    }
                }
            }
        }

        val animatedOffsetX by animateFloatAsState(
            targetValue = offsetX.coerceIn(safeLeftPx, maxBoundX),
            animationSpec = if (isDragging || lowEndDeviceMode) tween(0) else tween(220),
            label = "hud_offset_x"
        )
        val animatedOffsetY by animateFloatAsState(
            targetValue = offsetY.coerceIn(safeTopPx, maxBoundY),
            animationSpec = if (isDragging || lowEndDeviceMode) tween(0) else tween(220),
            label = "hud_offset_y"
        )
        val isAnchoredRight = (offsetX + hudWidthPx / 2f) > (maxWidthPx * 0.5f)

        Surface(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .onGloballyPositioned { coords ->
                    if (coords.size.width > 0 && coords.size.height > 0) {
                        hudWidthPx = coords.size.width
                        hudHeightPx = coords.size.height
                    }
                }
                .graphicsLayer {
                    alpha = hudAlpha
                }
                .pointerInput(maxWidthPx, maxHeightPx, safeLeftPx, safeTopPx, maxBoundX, maxBoundY, isPortrait, portraitBlackBarMaxY) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            markUserInteraction()
                        },
                        onDragEnd = {
                            isDragging = false
                            markUserInteraction()
                            if (!isPortrait) {
                                // A.3 Màn NGANG: nếu kéo HUD vào vùng giữa phía trên (30%–70% chiều ngang) thì tự bám về mép gần nhất khi thả tay
                                val hudCenterX = offsetX + (hudWidthPx / 2f)
                                val inTopZone = offsetY <= (maxHeightPx * 0.45f)
                                if (inTopZone && hudCenterX in (maxWidthPx * 0.30f)..(maxWidthPx * 0.70f)) {
                                    offsetX = if (hudCenterX < maxWidthPx * 0.50f) safeLeftPx else maxBoundX
                                } else {
                                    offsetX = offsetX.coerceIn(safeLeftPx, maxBoundX)
                                }
                                offsetY = offsetY.coerceIn(safeTopPx, maxBoundY)
                            } else {
                                // A.2 Màn DỌC: giữ trong vùng nhìn thấy và ưu tiên dải đen phía trên vùng video
                                offsetX = offsetX.coerceIn(safeLeftPx, maxBoundX)
                                offsetY = offsetY.coerceIn(safeTopPx, portraitBlackBarMaxY)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            markUserInteraction()
                            offsetX = offsetX.coerceIn(safeLeftPx, maxBoundX)
                            offsetY = offsetY.coerceIn(safeTopPx, if (isPortrait) portraitBlackBarMaxY else maxBoundY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            markUserInteraction()
                            offsetX = (offsetX + dragAmount.x).coerceIn(safeLeftPx, maxBoundX)
                            val yLimit = if (isPortrait && hudMode != HudMode.EXPANDED) portraitBlackBarMaxY else maxBoundY
                            offsetY = (offsetY + dragAmount.y).coerceIn(safeTopPx, yLimit)
                        }
                    )
                }
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan.copy(alpha = 0.55f) else XboxNeonGreen.copy(alpha = 0.55f),
                    RoundedCornerShape(14.dp)
                )
                .testTag("floating_hud_container"),
            color = Color(0xF20B130E),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalAlignment = if (isAnchoredRight) Alignment.End else Alignment.Start
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
                                markUserInteraction()
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
                            fontSize = 11.sp,
                            maxLines = 1
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

                    // ==========================================
                    // B. BẢNG MỞ RỘNG (NHỎ GỌN, DÀY THÔNG TIN, CUỘN BÊN TRONG)
                    // - Chiều rộng tối đa 38% màn hình, chiều cao tối đa 60% màn hình
                    // - Mở sát cạnh nơi HUD đang đứng (trái/phải)
                    // - Nút thu gọn rõ ràng, vùng bấm chuẩn 48dp
                    // - Bỏ ô lớn "Chất lượng đồ họa & hiệu năng máy", chỉ giữ:
                    //   Độ trễ, FPS, Thời gian giải mã, Tay cầm, Pin & Sạc, Nhiệt độ (chỉ hiện trong mở rộng), Chế độ chất lượng + nút chức năng
                    // ==========================================
                    val panelMaxWidth = (screenMaxWidth * 0.38f).coerceAtLeast(172.dp)
                    val panelMaxHeight = (screenMaxHeight * 0.60f).coerceAtLeast(180.dp)
                    val scrollState = rememberScrollState()

                    AnimatedVisibility(
                        visible = hudMode == HudMode.EXPANDED,
                        enter = if (lowEndDeviceMode) EnterTransition.None else expandVertically(
                            expandFrom = Alignment.Top
                        ) + fadeIn(),
                        exit = if (lowEndDeviceMode) ExitTransition.None else shrinkVertically(
                            shrinkTowards = Alignment.Top
                        ) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 4.dp, start = 2.dp, end = 2.dp, bottom = 2.dp)
                                .width(panelMaxWidth)
                                .widthIn(max = screenMaxWidth * 0.38f)
                                .heightIn(max = panelMaxHeight),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = if (isAnchoredRight) Alignment.End else Alignment.Start
                        ) {
                            // Thanh tiêu đề + Nút THU GỌN rõ ràng, đủ lớn để bấm (48dp)
                            ExpandedHudCollapseHeader(
                                isAnchoredRight = isAnchoredRight,
                                gameProfile = gameProfile,
                                onCollapseToCompact = {
                                    markUserInteraction()
                                    hudMode = HudMode.COMPACT
                                },
                                onCollapseToMinimized = {
                                    markUserInteraction()
                                    hudMode = HudMode.MINIMIZED
                                }
                            )

                            // Cảnh báo nhiệt độ khi từ MODERATE trở lên (chỉ hiển thị trong bảng cài đặt mở rộng)
                            if (isThermalSupported && showThermalWarningBanner) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showThermalWarningBanner = false }
                                        .testTag("hud_thermal_warning_banner"),
                                    shape = RoundedCornerShape(6.dp),
                                    color = JapanRed.copy(alpha = 0.24f),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, JapanRed.copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        text = "🔥 Máy đang nóng, nên nghỉ hoặc hạ chất lượng",
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Nội dung bên trong cuộn dọc nếu màn hình thấp (chiều cao tối đa 60%)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // B.6: Bảng thông số cốt lõi xếp dày (Độ trễ, FPS, Giải mã, Tay cầm, Pin & Nhiệt độ)
                                DenseCoreTelemetrySection(
                                    vpnState = vpnState,
                                    telemetry = telemetry,
                                    compactRawPing = compactRawPing,
                                    compactHasValidPing = compactHasValidPing,
                                    physicalControllerConnected = physicalControllerConnected,
                                    virtualControllerEnabled = virtualControllerEnabled,
                                    controllerName = controllerName,
                                    batteryPercent = batteryPercent,
                                    isBatteryCharging = isBatteryCharging,
                                    isThermalSupported = isThermalSupported,
                                    thermalIcon = thermalIcon,
                                    thermalLabel = thermalLabel,
                                    thermalColor = thermalColor,
                                    onToggleNetwork = {
                                        markUserInteraction()
                                        onToggleNetwork()
                                    },
                                    onToggleVirtualController = {
                                        markUserInteraction()
                                        onToggleVirtualController()
                                    }
                                )

                                // B.6: Chế độ chất lượng gọn & dày (không còn ô lớn chiếm hết chiều cao)
                                DenseQualityModeSection(
                                    networkStatus = networkStatus,
                                    adaptiveQualityState = adaptiveQualityState,
                                    telemetry = telemetry,
                                    onSelectCellularMode = { mode ->
                                        markUserInteraction()
                                        onSelectCellularMode(mode)
                                    },
                                    onToggleAutoAdapting = { enabled ->
                                        markUserInteraction()
                                        onToggleAutoAdapting(enabled)
                                    },
                                    onToggleWifiSpeedFirst = { enabled ->
                                        markUserInteraction()
                                        onToggleWifiSpeedFirst(enabled)
                                    }
                                )

                                // Hàng nút bật/tắt nhanh: Làm nét, Ẩn bảng kỹ thuật, Dịch tự động, Công cụ dịch
                                DenseFeatureTogglesSection(
                                    clarityBoostActive = clarityBoostActive,
                                    hideStreamStats = hideStreamStats,
                                    translationActive = translationActive,
                                    translatedCount = translatedCount,
                                    onToggleClarityBoost = { enabled ->
                                        markUserInteraction()
                                        onToggleClarityBoost(enabled)
                                    },
                                    onToggleHideStreamStats = { enabled ->
                                        markUserInteraction()
                                        onToggleHideStreamStats(enabled)
                                    },
                                    onToggleTranslation = { enabled ->
                                        markUserInteraction()
                                        onToggleTranslation(enabled)
                                    },
                                    onOpenTranslator = {
                                        markUserInteraction()
                                        onOpenTranslator()
                                    }
                                )

                                // Hàng tác vụ nhanh: Tải lại trang, Xoay màn hình, Cài đặt, Trang chủ
                                DenseBottomActionsSection(
                                    onReloadPage = {
                                        markUserInteraction()
                                        onReloadPage()
                                    },
                                    onToggleOrientation = {
                                        markUserInteraction()
                                        onToggleOrientation()
                                    },
                                    onOpenSettings = {
                                        markUserInteraction()
                                        onOpenSettings()
                                    },
                                    onRequestExitHome = {
                                        markUserInteraction()
                                        showExitConfirmDialog = true
                                    }
                                )
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

/**
 * B.7: Thanh tiêu đề bảng mở rộng kèm nút Thu gọn lớn (chuẩn 48dp) rõ ràng, dễ bấm,
 * tự căn theo phía trái hoặc phải nơi HUD đang đứng.
 */
@Composable
private fun ExpandedHudCollapseHeader(
    isAnchoredRight: Boolean,
    gameProfile: com.example.data.GameOptimizationProfile?,
    onCollapseToCompact: () -> Unit,
    onCollapseToMinimized: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF101D16),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isAnchoredRight) {
                CollapseTouchButton48dp(
                    onCollapse = onCollapseToCompact,
                    onMinimize = onCollapseToMinimized
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isAnchoredRight) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = gameProfile?.title ?: "BẢNG ĐIỀU KHIỂN NHANH",
                    color = TextPrimary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = if (gameProfile != null) {
                        "${gameProfile.badge} • ${gameProfile.recommendedFps}"
                    } else {
                        "Chạm Thu gọn để đóng bảng"
                    },
                    color = XboxNeonGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            if (isAnchoredRight) {
                Spacer(modifier = Modifier.width(6.dp))
                CollapseTouchButton48dp(
                    onCollapse = onCollapseToCompact,
                    onMinimize = onCollapseToMinimized
                )
            }
        }
    }
}

/**
 * Nút thu gọn rõ ràng, chiều cao & vùng chạm chuẩn 48dp để người chơi dễ bấm ngay khi đang chơi game.
 */
@Composable
private fun CollapseTouchButton48dp(
    onCollapse: () -> Unit,
    onMinimize: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .height(48.dp)
                .width(86.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCollapse() }
                .testTag("hud_collapse_48dp_button"),
            shape = RoundedCornerShape(8.dp),
            color = XboxNeonGreen.copy(alpha = 0.22f),
            border = androidx.compose.foundation.BorderStroke(1.dp, XboxNeonGreen.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Thu gọn bảng HUD",
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Thu gọn",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onMinimize() }
                .testTag("hud_minimize_48dp_button"),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF16261E),
            border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "─",
                    color = DirectCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * B.6 & B.7: Bảng thông số rút gọn, xếp dày, chỉ giữ 4 dòng thông tin cần xem:
 * - Độ trễ (kèm nút đổi nhanh Direct / VPN)
 * - FPS (kèm tốc độ truyền thực tế)
 * - Thời gian giải mã (Decode time ms)
 * - Tay cầm (kèm nút bật/tắt tay cầm ảo)
 */
@Composable
private fun DenseCoreTelemetrySection(
    vpnState: VpnConnectionState,
    telemetry: com.example.cloudplay.StreamTelemetry,
    compactRawPing: Int?,
    compactHasValidPing: Boolean,
    physicalControllerConnected: Boolean,
    virtualControllerEnabled: Boolean,
    controllerName: String,
    batteryPercent: Int,
    isBatteryCharging: Boolean,
    isThermalSupported: Boolean,
    thermalIcon: String,
    thermalLabel: String,
    thermalColor: Color,
    onToggleNetwork: () -> Unit,
    onToggleVirtualController: () -> Unit
) {
    val effectivePing = when {
        telemetry.rttMs > 0 -> telemetry.rttMs
        compactHasValidPing -> compactRawPing
        else -> null
    }
    val latencyDisplayText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
        if (effectivePing != null) "Direct • ${effectivePing}ms" else "Direct • -- ms"
    } else {
        if (effectivePing != null) "VPN ${vpnState.currentServer.flag} • ${effectivePing}ms" else "VPN ${vpnState.currentServer.flag} • -- ms"
    }

    val fpsValueText = if (telemetry.fps > 0) "${telemetry.fps} FPS" else "-- FPS"
    val bitrateValueText = if (telemetry.bitrateMbps > 0) {
        String.format("%.1f Mbps", telemetry.bitrateMbps)
    } else {
        "-- Mbps"
    }

    val decodeTimeText = if (telemetry.decodeTimeMs > 0) {
        String.format("%.1f ms", telemetry.decodeTimeMs)
    } else {
        "-- ms"
    }
    val decodeColor = when {
        telemetry.decodeTimeMs <= 0.0 -> TextSecondary
        telemetry.decodeTimeMs <= 8.5 -> XboxNeonGreen
        telemetry.decodeTimeMs <= 12.0 -> DirectCyan
        else -> JapanRed
    }

    val controllerStatusText = when {
        physicalControllerConnected -> controllerName
        virtualControllerEnabled -> stringResource(R.string.hud_virtual_gamepad)
        else -> stringResource(R.string.controller_disconnected)
    }

    val batteryStatusText = if (batteryPercent >= 0) {
        val chargeStr = if (isBatteryCharging) "Đang sạc ⚡" else "Không sạc"
        "$batteryPercent% • $chargeStr"
    } else {
        "--% • Không rõ"
    }
    val batteryColor = when {
        isBatteryCharging -> XboxNeonGreen
        batteryPercent in 0..20 -> JapanRed
        batteryPercent in 21..35 -> Color(0xFFFFB300)
        else -> TextPrimary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF121F18),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // 1. Dòng Độ trễ (chạm để chuyển nhanh Direct / VPN)
            DenseMetricRowItem(
                label = stringResource(R.string.hud_stream_latency),
                value = latencyDisplayText,
                valueColor = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan else JapanRed,
                badgeText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "Đổi VPN" else "Ngắt VPN",
                onRowClick = onToggleNetwork
            )

            // 2. Dòng FPS & tốc độ luồng
            DenseMetricRowItem(
                label = "FPS",
                value = "$fpsValueText ($bitrateValueText)",
                valueColor = if (telemetry.fps >= 55) XboxNeonGreen else TextPrimary
            )

            // 3. Dòng Thời gian giải mã
            DenseMetricRowItem(
                label = "Giải mã",
                value = decodeTimeText,
                valueColor = decodeColor
            )

            // 4. Dòng Tay cầm (chạm để bật/tắt tay cầm ảo)
            DenseMetricRowItem(
                label = stringResource(R.string.hud_controller),
                value = controllerStatusText,
                valueColor = if (physicalControllerConnected || virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                badgeText = if (virtualControllerEnabled) "Ẩn ảo" else "Bật ảo",
                onRowClick = onToggleVirtualController
            )

            // 5. Dòng % Pin & trạng thái sạc (+ Nhiệt độ máy trên API 29+)
            DenseMetricRowItem(
                label = "Pin & Sạc",
                value = batteryStatusText,
                valueColor = batteryColor,
                badgeText = if (isThermalSupported) "$thermalIcon $thermalLabel" else null
            )
        }
    }
}

@Composable
private fun DenseMetricRowItem(
    label: String,
    value: String,
    valueColor: Color,
    badgeText: String? = null,
    onRowClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .then(
                if (onRowClick != null) {
                    Modifier.clickable { onRowClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1D3126)
                ) {
                    Text(
                        text = badgeText,
                        color = DirectCyan,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * B.6 & B.7: Mục "Chế độ chất lượng" thu gọn, bỏ ô lớn trống,
 * xếp các mức Tự động / Thấp / Vừa / Cao / Tối đa và nút giảm trễ trên các dòng mỏng.
 */
@Composable
private fun DenseQualityModeSection(
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus,
    adaptiveQualityState: com.example.cloudplay.AdaptiveQualityState,
    telemetry: com.example.cloudplay.StreamTelemetry,
    onSelectCellularMode: (com.example.data.CellularOptimizationMode) -> Unit,
    onToggleAutoAdapting: (Boolean) -> Unit,
    onToggleWifiSpeedFirst: (Boolean) -> Unit
) {
    val liveMbps = adaptiveQualityState.smoothBitrateMbps.coerceIn(1.0, 20.0)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C16),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chế độ chất lượng",
                    color = XboxNeonGreen,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", liveMbps)}M",
                        color = DirectCyan,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (adaptiveQualityState.isAutoAdapting) XboxNeonGreen.copy(alpha = 0.22f) else Color.DarkGray,
                        modifier = Modifier.clickable { onToggleAutoAdapting(!adaptiveQualityState.isAutoAdapting) }
                    ) {
                        Text(
                            text = if (adaptiveQualityState.isAutoAdapting) "Tự động" else "Cố định",
                            color = if (adaptiveQualityState.isAutoAdapting) XboxNeonGreen else TextSecondary,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            // Hàng 1: Tự động & Thấp & Vừa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                DenseQualityPresetChip(
                    label = "Tự động",
                    isSelected = networkStatus.activeProfileName.contains("Tự Động"),
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.AUTO) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "Thấp",
                    isSelected = networkStatus.activeProfileName.contains("Thấp"),
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "Vừa",
                    isSelected = networkStatus.activeProfileName.contains("Trung Bình"),
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.MEDIUM) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Hàng 2: Cao, Rất cao & Ưu tiên giảm trễ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                DenseQualityPresetChip(
                    label = "Cao",
                    isSelected = networkStatus.activeProfileName.contains("Cao ("),
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.HIGH) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "Tối đa",
                    isSelected = networkStatus.activeProfileName.contains("Rất Cao"),
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = if (networkStatus.speedFirstPriority) "Giảm trễ: Bật" else "Giảm trễ: Tắt",
                    isSelected = networkStatus.speedFirstPriority,
                    accentColor = DirectCyan,
                    onClick = { onToggleWifiSpeedFirst(!networkStatus.speedFirstPriority) },
                    modifier = Modifier.weight(1.2f)
                )
            }

            if (telemetry.estimatedDataSavedMb > 1.0) {
                Text(
                    text = "Tiết kiệm: ~${String.format("%.1f", telemetry.estimatedDataSavedMb)} MB",
                    color = TextSecondary,
                    fontSize = 7.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DenseQualityPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = XboxNeonGreen
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(5.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF15251D),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(0.8.dp, accentColor) else null
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) accentColor else TextPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * B.7: Hàng nút bật/tắt gọn cho Làm nét, Ẩn thông số web, Dịch tiếng Việt và Công cụ dịch.
 */
@Composable
private fun DenseFeatureTogglesSection(
    clarityBoostActive: Boolean,
    hideStreamStats: Boolean,
    translationActive: Boolean,
    translatedCount: Int,
    onToggleClarityBoost: (Boolean) -> Unit,
    onToggleHideStreamStats: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onOpenTranslator: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DenseQuickTogglePill(
            label = if (clarityBoostActive) "Làm nét: Bật" else "Làm nét: Tắt",
            active = clarityBoostActive,
            activeColor = XboxNeonGreen,
            onClick = { onToggleClarityBoost(!clarityBoostActive) },
            modifier = Modifier.weight(1f)
        )
        DenseQuickTogglePill(
            label = if (hideStreamStats) "Ẩn bảng: Bật" else "Ẩn bảng: Tắt",
            active = hideStreamStats,
            activeColor = DirectCyan,
            onClick = { onToggleHideStreamStats(!hideStreamStats) },
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DenseQuickTogglePill(
            label = if (translationActive) {
                if (translatedCount > 0) "Dịch VI ($translatedCount)" else "Dịch VI: Bật"
            } else {
                "Dịch VI: Tắt"
            },
            active = translationActive,
            activeColor = XboxNeonGreen,
            onClick = { onToggleTranslation(!translationActive) },
            modifier = Modifier.weight(1f)
        )
        DenseQuickTogglePill(
            label = "Bảng dịch",
            active = true,
            activeColor = DirectCyan,
            onClick = onOpenTranslator,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DenseQuickTogglePill(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (active) activeColor.copy(alpha = 0.18f) else Color(0xFF14221A),
        border = androidx.compose.foundation.BorderStroke(
            0.7.dp,
            if (active) activeColor.copy(alpha = 0.55f) else XboxCardBorder.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (active) activeColor else TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * B.7: Hàng nút tác vụ dưới cùng: Tải lại trang, Xoay màn hình, Cài đặt, Trang chủ.
 */
@Composable
private fun DenseBottomActionsSection(
    onReloadPage: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestExitHome: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DenseBottomActionChip(
            icon = Icons.Default.Refresh,
            label = "Tải lại",
            tint = XboxNeonGreen,
            onClick = onReloadPage,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_reload_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.ScreenRotation,
            label = "Xoay",
            tint = DirectCyan,
            onClick = onToggleOrientation,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_rotate_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.Settings,
            label = "Cài đặt",
            tint = TextPrimary,
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_settings_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.Home,
            label = "Thoát",
            tint = JapanRed,
            onClick = onRequestExitHome,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_home_button"
        )
    }
}

@Composable
private fun DenseBottomActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF101915),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(2.5.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

