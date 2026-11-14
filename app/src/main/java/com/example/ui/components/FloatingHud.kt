package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.AppConfig
import com.example.data.AppLogger
import com.example.data.SettingsRepository
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxPressScale
import com.example.vpn.VpnConnectionState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class HudMode {
    MINIMIZED,
    COMPACT,
    EXPANDED
}

enum class HudDockedEdge {
    NONE,
    LEFT,
    RIGHT
}

private enum class HudExpandedTab {
    NETWORK,
    CONTROLS,
    IMAGE,
    DEVICE
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
    onOpenTranslator: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    videoTopRatio: Float = 0f,
    lowEndDeviceMode: Boolean = false,
    onSelectFortniteControlMode: (Boolean) -> Unit = {},
    recorderState: com.example.cloudplay.ScreenRecorderState = com.example.cloudplay.ScreenRecorderState(),
    onToggleRecording: () -> Unit = {},
    xboxWebUiMode: com.example.data.XboxWebUiMode = com.example.data.XboxWebUiMode.CLASSIC,
    onSelectXboxWebUiMode: (com.example.data.XboxWebUiMode) -> Unit = {},
    onHudOpenStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hudMode by rememberSaveable { mutableStateOf(HudMode.MINIMIZED) }
    var activeTab by rememberSaveable { mutableStateOf(HudExpandedTab.NETWORK) }
    var dockedEdge by rememberSaveable { mutableStateOf(HudDockedEdge.NONE) }
    var offsetX by rememberSaveable { mutableFloatStateOf(24f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(24f) }
    var hudWidthPx by remember { mutableIntStateOf(160) }
    var hudHeightPx by remember { mutableIntStateOf(80) }
    var isDragging by remember { mutableStateOf(false) }
    var lastOrientationPortrait by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isInteracting by remember { mutableStateOf(true) }
    var prevLoggedHudMode by remember { mutableStateOf(hudMode) }

    val compactExpandFocusRequester = remember { FocusRequester() }
    val expandedFirstTabFocusRequester = remember { FocusRequester() }
    val cancelExitFocusRequester = remember { FocusRequester() }

    LaunchedEffect(hudMode, showExitConfirmDialog) {
        onHudOpenStateChanged(hudMode != HudMode.MINIMIZED || showExitConfirmDialog)
        if (prevLoggedHudMode != hudMode) {
            AppLogger.info("UI", "Menu nhanh trong game (HUD): ${prevLoggedHudMode.name} -> ${hudMode.name}")
            prevLoggedHudMode = hudMode
        }
    }

    val comboTick = GamepadUiNavigation.hudToggleComboTick
    var lastHandledComboTick by remember { mutableIntStateOf(comboTick) }
    LaunchedEffect(comboTick) {
        if (comboTick != lastHandledComboTick) {
            lastHandledComboTick = comboTick
            lastInteractionTime = System.currentTimeMillis()
            isInteracting = true
            if (showExitConfirmDialog) {
                showExitConfirmDialog = false
            } else if (hudMode == HudMode.MINIMIZED) {
                dockedEdge = HudDockedEdge.NONE
                hudMode = HudMode.EXPANDED
            } else {
                hudMode = HudMode.MINIMIZED
            }
        }
    }

    // Theo dõi nhiệt độ máy (API 29+) & pin
    val isThermalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var thermalStatus by remember { mutableIntStateOf(0) }
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
                thermalStatus = newStatus
            }
        } else {
            null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            try {
                thermalStatus = powerManager.currentThermalStatus
                powerManager.addThermalStatusListener(context.mainExecutor, thermalListener)
            } catch (e: Exception) {
                AppLogger.error("SYSTEM", "Lỗi đăng ký theo dõi nhiệt độ máy (${AppLogger.shortThrowable(e)})")
            }
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
        } catch (e: Exception) {
            AppLogger.error("SYSTEM", "Lỗi đăng ký theo dõi pin (${AppLogger.shortThrowable(e)})")
        }

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

    val config by SettingsRepository.config.collectAsState()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = lowEndDeviceMode || config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )

    // Cảnh báo nhỏ tự ẩn sau vài giây (nhiệt độ / pin yếu / đổi mạng)
    var hasShownSevereThermalAlert by rememberSaveable(isGameActive) { mutableStateOf(false) }
    var hasShownLowBatteryAlert by rememberSaveable(isGameActive) { mutableStateOf(false) }
    var lastStreamNetworkIsWifi by rememberSaveable(isGameActive) { mutableStateOf<Boolean?>(null) }
    var autoHudBannerText by remember { mutableStateOf<String?>(null) }
    var autoHudBannerTag by remember { mutableStateOf("hud_transient_alert_banner") }

    LaunchedEffect(isGameActive, isThermalSupported, thermalStatus, batteryPercent, isBatteryCharging, config.appLanguage) {
        if (!isGameActive) {
            hasShownSevereThermalAlert = false
            hasShownLowBatteryAlert = false
            return@LaunchedEffect
        }
        val severeHot = isThermalSupported &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE
        if (severeHot && !hasShownSevereThermalAlert) {
            hasShownSevereThermalAlert = true
            autoHudBannerTag = "hud_thermal_battery_alert_banner"
            autoHudBannerText = config.tr(
                "Máy nóng, nên giảm FPS hoặc độ phân giải",
                "Device is hot, consider lowering FPS or resolution"
            )
        } else if (batteryPercent in 1..15 && !isBatteryCharging && !hasShownLowBatteryAlert) {
            hasShownLowBatteryAlert = true
            autoHudBannerTag = "hud_thermal_battery_alert_banner"
            autoHudBannerText = config.tr(
                "Pin yếu (${batteryPercent}%), nên cắm sạc hoặc giảm FPS",
                "Low battery (${batteryPercent}%), consider charging or lowering FPS"
            )
        }
    }

    LaunchedEffect(isGameActive, networkStatus.isWifi, networkStatus.isCellular, config.appLanguage) {
        val currentIsWifi: Boolean? = when {
            networkStatus.isWifi -> true
            networkStatus.isCellular -> false
            else -> null
        }
        if (!isGameActive) {
            lastStreamNetworkIsWifi = currentIsWifi
            return@LaunchedEffect
        }
        if (currentIsWifi != null) {
            val previous = lastStreamNetworkIsWifi
            if (previous != null && previous != currentIsWifi) {
                autoHudBannerTag = "hud_network_switch_alert_banner"
                autoHudBannerText = config.tr(
                    "Mạng vừa đổi. Nếu giật hãy thử lại.",
                    "Network switched. If laggy please retry."
                )
            }
            lastStreamNetworkIsWifi = currentIsWifi
        }
    }

    LaunchedEffect(autoHudBannerText) {
        if (autoHudBannerText != null) {
            delay(4500)
            autoHudBannerText = null
        }
    }

    fun markUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        isInteracting = true
    }

    // Tự mờ theo config.hudIdleOpacity sau 3s không chạm
    LaunchedEffect(lastInteractionTime, hudMode) {
        if (hudMode != HudMode.EXPANDED) {
            delay(3000)
            isInteracting = false
        } else {
            isInteracting = true
        }
    }

    val idleAlpha = config.hudIdleOpacity.coerceIn(0.15f, 0.90f)
    val targetAlpha = if (isInteracting || isDragging || hudMode == HudMode.EXPANDED) 1.0f else idleAlpha
    val hudAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(if (lowEndDeviceMode) 0 else 280),
        label = "hud_alpha"
    )

    val safeInsets = WindowInsets.safeDrawing
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
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

        val rightScreenEdgeX = (maxWidthPx - hudWidthPx.toFloat()).coerceAtLeast(0f)
        val maxBoundY = (maxHeightPx - safeBottomPx - hudHeightPx.toFloat()).coerceAtLeast(safeTopPx)
        val edgeDockThresholdPx = with(density) { 10.dp.toPx() }
        val restorePaddingPx = with(density) { 12.dp.toPx() }

        val actualVideoTopPx = if (isPortrait) {
            if (videoTopRatio > 0.04f) {
                maxHeightPx * videoTopRatio
            } else {
                ((maxHeightPx - (maxWidthPx * 9f / 16f)) / 2f).coerceAtLeast(safeTopPx)
            }
        } else {
            0f
        }
        val portraitDefaultY = if (isPortrait) {
            (actualVideoTopPx - hudHeightPx.toFloat() - with(density) { 6.dp.toPx() }).coerceIn(safeTopPx, maxBoundY)
        } else {
            safeTopPx
        }

        LaunchedEffect(maxWidthPx, maxHeightPx, hudWidthPx, hudHeightPx, isPortrait) {
            if (maxWidthPx > 0f && maxHeightPx > 0f) {
                val orientationChanged = lastOrientationPortrait != isPortrait
                if (orientationChanged) {
                    val isFirstInit = lastOrientationPortrait == null
                    lastOrientationPortrait = isPortrait
                    if (isFirstInit) {
                        offsetX = safeLeftPx
                        offsetY = if (isPortrait) portraitDefaultY else safeTopPx
                    } else {
                        offsetX = offsetX.coerceIn(0f, rightScreenEdgeX)
                        offsetY = offsetY.coerceIn(safeTopPx, maxBoundY)
                    }
                } else {
                    offsetX = offsetX.coerceIn(0f, rightScreenEdgeX)
                    offsetY = offsetY.coerceIn(safeTopPx, maxBoundY)
                }
            }
        }

        val animatedOffsetX by animateFloatAsState(
            targetValue = offsetX.coerceIn(0f, rightScreenEdgeX),
            animationSpec = if (isDragging || lowEndDeviceMode) tween(0) else tween(180),
            label = "hud_offset_x"
        )
        val animatedOffsetY by animateFloatAsState(
            targetValue = offsetY.coerceIn(safeTopPx, maxBoundY),
            animationSpec = if (isDragging || lowEndDeviceMode) tween(0) else tween(180),
            label = "hud_offset_y"
        )
        val isAnchoredRight = (offsetX + hudWidthPx / 2f) > (maxWidthPx * 0.5f)
        val isNearEdgeForDock = isDragging && hudMode != HudMode.EXPANDED &&
            (offsetX <= edgeDockThresholdPx || offsetX >= (rightScreenEdgeX - edgeDockThresholdPx))

        if (dockedEdge != HudDockedEdge.NONE) {
            // Thanh gờ khi dính sát mép trái / phải
            val handleWidthDp = 30.dp
            val handleHeightDp = 56.dp
            val handleWidthPx = with(density) { handleWidthDp.toPx() }
            val handleHeightPx = with(density) { handleHeightDp.toPx() }
            val swipeUnlockThresholdPx = with(density) { 10.dp.toPx() }
            val handleX = if (dockedEdge == HudDockedEdge.LEFT) {
                0
            } else {
                (maxWidthPx - handleWidthPx).roundToInt().coerceAtLeast(0)
            }
            val handleMaxY = (maxHeightPx - safeBottomPx - handleHeightPx).coerceAtLeast(safeTopPx)
            val handleY = animatedOffsetY.coerceIn(safeTopPx, handleMaxY).roundToInt()

            val handleShape = if (dockedEdge == HudDockedEdge.LEFT) {
                RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)
            } else {
                RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
            }

            fun restoreFromEdge() {
                markUserInteraction()
                val restoredX = if (dockedEdge == HudDockedEdge.LEFT) {
                    (safeLeftPx + restorePaddingPx).coerceAtMost(rightScreenEdgeX)
                } else {
                    (rightScreenEdgeX - safeRightPx - restorePaddingPx).coerceAtLeast(safeLeftPx)
                }
                offsetX = restoredX
                dockedEdge = HudDockedEdge.NONE
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(handleX, handleY) }
                    .size(width = handleWidthDp, height = handleHeightDp)
                    .graphicsLayer { alpha = if (isInteracting || isDragging) 0.96f else idleAlpha.coerceAtLeast(0.45f) }
                    .pointerInput(dockedEdge, maxWidthPx, maxHeightPx, rightScreenEdgeX, safeLeftPx, safeRightPx, safeTopPx, handleMaxY) {
                        var accumulatedSwipeX = 0f
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                accumulatedSwipeX = 0f
                                markUserInteraction()
                            },
                            onDragEnd = {
                                isDragging = false
                                markUserInteraction()
                            },
                            onDragCancel = {
                                isDragging = false
                                markUserInteraction()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                markUserInteraction()
                                accumulatedSwipeX += dragAmount.x
                                offsetY = (offsetY + dragAmount.y).coerceIn(safeTopPx, handleMaxY)
                                val swipedInward = (dockedEdge == HudDockedEdge.LEFT && accumulatedSwipeX > swipeUnlockThresholdPx) ||
                                    (dockedEdge == HudDockedEdge.RIGHT && accumulatedSwipeX < -swipeUnlockThresholdPx)
                                if (swipedInward) {
                                    isDragging = false
                                    restoreFromEdge()
                                }
                            }
                        )
                    }
                    .clip(handleShape)
                    .clickable { restoreFromEdge() }
                    .border(1.dp, HudLine, handleShape)
                    .testTag("hud_edge_dock_handle"),
                shape = handleShape,
                color = HudPanelBg,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (dockedEdge == HudDockedEdge.LEFT) {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft
                        },
                        contentDescription = config.tr("Mở menu nổi", "Open floating menu"),
                        tint = HudAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            val containerShape = when (hudMode) {
                HudMode.MINIMIZED -> RoundedCornerShape(20.dp)
                HudMode.COMPACT -> RoundedCornerShape(18.dp)
                HudMode.EXPANDED -> RoundedCornerShape(20.dp)
            }

            Column(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                    .onGloballyPositioned { coords ->
                        if (coords.size.width > 0 && coords.size.height > 0) {
                            hudWidthPx = coords.size.width
                            hudHeightPx = coords.size.height
                        }
                    }
                    .graphicsLayer { alpha = if (isNearEdgeForDock) 0.65f else hudAlpha }
                    .pointerInput(maxWidthPx, maxHeightPx, rightScreenEdgeX, safeLeftPx, safeTopPx, maxBoundY, edgeDockThresholdPx, hudMode) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                markUserInteraction()
                            },
                            onDragEnd = {
                                isDragging = false
                                markUserInteraction()
                                if (hudMode != HudMode.EXPANDED && offsetX <= edgeDockThresholdPx) {
                                    offsetX = 0f
                                    dockedEdge = HudDockedEdge.LEFT
                                } else if (hudMode != HudMode.EXPANDED && offsetX >= (rightScreenEdgeX - edgeDockThresholdPx)) {
                                    offsetX = rightScreenEdgeX
                                    dockedEdge = HudDockedEdge.RIGHT
                                } else {
                                    offsetX = offsetX.coerceIn(0f, rightScreenEdgeX)
                                    offsetY = offsetY.coerceIn(safeTopPx, maxBoundY)
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                markUserInteraction()
                                offsetX = offsetX.coerceIn(0f, rightScreenEdgeX)
                                offsetY = offsetY.coerceIn(safeTopPx, maxBoundY)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                markUserInteraction()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, rightScreenEdgeX)
                                offsetY = (offsetY + dragAmount.y).coerceIn(safeTopPx, maxBoundY)
                            }
                        )
                    },
                horizontalAlignment = if (isAnchoredRight) Alignment.End else Alignment.Start
            ) {
                Surface(
                    modifier = Modifier
                        .clip(containerShape)
                        .border(
                            1.dp,
                            if (isNearEdgeForDock) HudAccent else HudLine,
                            containerShape
                        )
                        .testTag("floating_hud_container"),
                    shape = containerShape,
                    color = HudPanelBg,
                    shadowElevation = 0.dp
                ) {
                    when (hudMode) {
                        HudMode.MINIMIZED -> {
                            HudMinimizedPill(
                                config = config,
                                telemetry = telemetry,
                                isAnchoredRight = isAnchoredRight,
                                motionEnabled = motionEnabled,
                                onExpandToCompact = {
                                    markUserInteraction()
                                    hudMode = HudMode.COMPACT
                                }
                            )
                        }

                        HudMode.COMPACT -> {
                            GamepadSurfaceController(
                                enabled = !showExitConfirmDialog,
                                defaultFocusRequester = compactExpandFocusRequester,
                                autoRequestDefaultFocus = true,
                                onBackPress = {
                                    markUserInteraction()
                                    hudMode = HudMode.MINIMIZED
                                }
                            ) {
                                HudCompactRail(
                                    config = config,
                                    telemetry = telemetry,
                                    isAnchoredRight = isAnchoredRight,
                                    maxHeight = (screenMaxHeight - 16.dp).coerceAtLeast(220.dp),
                                    gamepadActive = virtualControllerEnabled || physicalControllerConnected,
                                    recorderState = recorderState,
                                    expandFocusRequester = compactExpandFocusRequester,
                                    onExpand = {
                                        markUserInteraction()
                                        hudMode = HudMode.EXPANDED
                                    },
                                    onToggleGamepad = {
                                        markUserInteraction()
                                        onToggleVirtualController()
                                    },
                                    onToggleRecording = {
                                        markUserInteraction()
                                        onToggleRecording()
                                    },
                                    onReload = {
                                        markUserInteraction()
                                        onReloadPage()
                                    },
                                    onMinimize = {
                                        markUserInteraction()
                                        hudMode = HudMode.MINIMIZED
                                    },
                                    onRequestExit = {
                                        markUserInteraction()
                                        showExitConfirmDialog = true
                                    }
                                )
                            }
                        }

                        HudMode.EXPANDED -> {
                            val panelWidth = (screenMaxWidth - 16.dp).coerceIn(260.dp, 388.dp)
                            val panelMaxHeight = (screenMaxHeight - 16.dp).coerceAtLeast(240.dp)
                            val bodyScrollState = rememberScrollState()
                            val isThermalWarmOrHot = isThermalSupported && thermalStatus >= 1

                            GamepadSurfaceController(
                                enabled = !showExitConfirmDialog,
                                defaultFocusRequester = expandedFirstTabFocusRequester,
                                autoRequestDefaultFocus = true,
                                onPreviousTab = {
                                    markUserInteraction()
                                    val allTabs = HudExpandedTab.entries
                                    val curIdx = allTabs.indexOf(activeTab).coerceAtLeast(0)
                                    activeTab = allTabs[(curIdx - 1 + allTabs.size) % allTabs.size]
                                },
                                onNextTab = {
                                    markUserInteraction()
                                    val allTabs = HudExpandedTab.entries
                                    val curIdx = allTabs.indexOf(activeTab).coerceAtLeast(0)
                                    activeTab = allTabs[(curIdx + 1) % allTabs.size]
                                },
                                onBackPress = {
                                    markUserInteraction()
                                    hudMode = HudMode.MINIMIZED
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(panelWidth)
                                        .heightIn(max = panelMaxHeight)
                                ) {
                                    // Header (.p-head)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HudPingDotIndicator(
                                            pingMs = if (telemetry.rttMs > 0) telemetry.rttMs else null,
                                            hasSample = telemetry.hasStatsSample,
                                            fontSize = 15.sp,
                                            showMsSuffix = true
                                        )
                                        if (recorderState.isRecording) {
                                            val mins = recorderState.elapsedSeconds / 60
                                            val secs = recorderState.elapsedSeconds % 60
                                            Text(
                                                text = "● %02d:%02d".format(mins, secs),
                                                color = HudDanger,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        HudPanelIconButton(
                                            icon = if (isAnchoredRight) {
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight
                                            } else {
                                                Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                            },
                                            contentDescription = config.tr("Thu gọn", "Collapse"),
                                            onClick = {
                                                markUserInteraction()
                                                hudMode = HudMode.COMPACT
                                            },
                                            modifier = Modifier.testTag("hud_collapse_48dp_button")
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(HudSurface2)
                                                .gamepadClickable(cornerRadius = 12.dp) {
                                                    markUserInteraction()
                                                    hudMode = HudMode.MINIMIZED
                                                }
                                                .testTag("hud_minimize_48dp_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "─",
                                                color = HudMuted,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // 4 Tabs (.tabs)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val tabs = listOf(
                                            HudExpandedTab.NETWORK to config.tr("Mạng", "Net"),
                                            HudExpandedTab.CONTROLS to config.tr("Điều khiển", "Controls"),
                                            HudExpandedTab.IMAGE to config.tr("Hình ảnh", "Image"),
                                            HudExpandedTab.DEVICE to config.tr("Máy", "Device")
                                        )
                                        tabs.forEach { (tab, title) ->
                                            val selected = activeTab == tab
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 34.dp)
                                                    .clip(RoundedCornerShape(11.dp))
                                                    .background(if (selected) HudSelBg else HudSurface)
                                                    .gamepadClickable(
                                                        cornerRadius = 11.dp,
                                                        focusRequester = if (selected) expandedFirstTabFocusRequester else null,
                                                        onStepHorizontal = { dir ->
                                                            markUserInteraction()
                                                            val curIdx = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
                                                            val nextIdx = (curIdx + dir).coerceIn(0, tabs.lastIndex)
                                                            if (nextIdx != curIdx) {
                                                                activeTab = tabs[nextIdx].first
                                                            }
                                                        }
                                                    ) {
                                                        markUserInteraction()
                                                        activeTab = tab
                                                    }
                                                    .testTag("hud_tab_${tab.name.lowercase()}")
                                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = title,
                                                    color = if (selected) HudAccent else HudMuted,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                // Body (.p-body)
                                Column(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .fillMaxWidth()
                                        .verticalScroll(bodyScrollState)
                                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (activeTab) {
                                        HudExpandedTab.NETWORK -> HudNetworkTabContent(
                                            config = config,
                                            vpnState = vpnState,
                                            networkStatus = networkStatus,
                                            telemetry = telemetry,
                                            adaptiveQualityState = adaptiveQualityState,
                                            isThermalWarmOrHot = isThermalWarmOrHot,
                                            onInteraction = ::markUserInteraction,
                                            onToggleNetwork = onToggleNetwork
                                        )

                                        HudExpandedTab.CONTROLS -> HudControlsTabContent(
                                            config = config,
                                            virtualControllerEnabled = virtualControllerEnabled,
                                            translationActive = translationActive,
                                            translatedCount = translatedCount,
                                            gameProfile = gameProfile,
                                            xboxWebUiMode = xboxWebUiMode,
                                            onInteraction = ::markUserInteraction,
                                            onToggleVirtualController = onToggleVirtualController,
                                            onSelectFortniteControlMode = onSelectFortniteControlMode,
                                            onToggleTranslation = onToggleTranslation,
                                            onOpenTranslator = onOpenTranslator,
                                            onSelectXboxWebUiMode = onSelectXboxWebUiMode,
                                            onToggleOrientation = onToggleOrientation
                                        )

                                        HudExpandedTab.IMAGE -> HudImageTabContent(
                                            config = config,
                                            clarityBoostActive = clarityBoostActive,
                                            hideStreamStats = hideStreamStats,
                                            networkStatus = networkStatus,
                                            adaptiveQualityState = adaptiveQualityState,
                                            isThermalWarmOrHot = isThermalWarmOrHot,
                                            onInteraction = ::markUserInteraction,
                                            onSelectCellularMode = onSelectCellularMode,
                                            onToggleClarityBoost = onToggleClarityBoost,
                                            onToggleHideStreamStats = onToggleHideStreamStats,
                                            onSelectLadderStep = onSelectLadderStep,
                                            onToggleAutoAdapting = onToggleAutoAdapting
                                        )

                                        HudExpandedTab.DEVICE -> HudDeviceTabContent(
                                            config = config,
                                            batteryPercent = batteryPercent,
                                            isBatteryCharging = isBatteryCharging,
                                            isThermalSupported = isThermalSupported,
                                            thermalStatus = thermalStatus,
                                            networkStatus = networkStatus,
                                            recorderState = recorderState,
                                            onInteraction = ::markUserInteraction,
                                            onToggleWifiSpeedFirst = onToggleWifiSpeedFirst,
                                            onToggleRecording = onToggleRecording
                                        )
                                    }
                                }

                                // Sticky Footer (.p-foot): Tải lại | Trang chủ | Cài đặt | Thoát
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(HudFootBorder)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HudFooterActionButton(
                                        icon = Icons.Default.Refresh,
                                        label = config.tr("Tải lại", "Reload"),
                                        onClick = {
                                            markUserInteraction()
                                            onReloadPage()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hud_expanded_reload_button")
                                    )
                                    HudFooterActionButton(
                                        icon = Icons.Default.Home,
                                        label = config.tr("Trang chủ", "Home"),
                                        onClick = {
                                            markUserInteraction()
                                            showExitConfirmDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hud_expanded_go_home_card")
                                    )
                                    HudFooterActionButton(
                                        icon = Icons.Default.Tune,
                                        label = config.tr("Cài đặt", "Settings"),
                                        onClick = {
                                            markUserInteraction()
                                            onOpenSettings()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hud_expanded_video_settings_card")
                                    )
                                    HudFooterActionButton(
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        label = config.tr("Thoát", "Exit"),
                                        isDanger = true,
                                        onClick = {
                                            markUserInteraction()
                                            AppLogger.info("UI", "Mở hộp thoại xác nhận Thoát game từ HUD")
                                            showExitConfirmDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hud_expanded_home_button")
                                    )
                                }
                            }
                            }
                        }
                    }
                }

                // Banner cảnh báo nhỏ tự ẩn (nhiệt độ / pin yếu / chuyển mạng)
                val currentBanner = autoHudBannerText
                if (currentBanner != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HudNoteCard(
                        text = currentBanner,
                        isWarning = true,
                        onClick = { autoHudBannerText = null },
                        modifier = Modifier
                            .widthIn(max = 270.dp)
                            .testTag(autoHudBannerTag)
                    )
                }
            }
        }
    }

    // Hộp thoại Xác nhận thoát theo khuôn .dlg của hud-mockup.html
    if (showExitConfirmDialog) {
        Dialog(
            onDismissRequest = {
                AppLogger.info("UI", "Hủy hộp thoại Thoát game từ HUD")
                showExitConfirmDialog = false
            }
        ) {
            GamepadSurfaceController(
                enabled = true,
                defaultFocusRequester = cancelExitFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = {
                    AppLogger.info("UI", "Hủy hộp thoại Thoát game từ HUD")
                    showExitConfirmDialog = false
                }
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .testTag("hud_exit_confirm_dialog"),
                    shape = RoundedCornerShape(22.dp),
                    color = HudSurface2,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_exit_title),
                            color = HudInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = stringResource(R.string.dialog_exit_message),
                            color = HudDialogBody,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    AppLogger.info("UI", "Hủy hộp thoại Thoát game từ HUD")
                                    showExitConfirmDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HudSurface,
                                    contentColor = HudInk
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .focusRequester(cancelExitFocusRequester)
                                    .gamepadActionButton(cornerRadius = 12.dp) {
                                        AppLogger.info("UI", "Hủy hộp thoại Thoát game từ HUD")
                                        showExitConfirmDialog = false
                                    }
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_cancel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                            Button(
                                onClick = {
                                    AppLogger.info("UI", "Xác nhận Thoát game từ HUD -> về Trang chủ")
                                    showExitConfirmDialog = false
                                    onNavigateHome()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HudDanger,
                                    contentColor = HudDangerOn
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .gamepadActionButton(cornerRadius = 12.dp) {
                                        AppLogger.info("UI", "Xác nhận Thoát game từ HUD -> về Trang chủ")
                                        showExitConfirmDialog = false
                                        onNavigateHome()
                                    }
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_confirm_exit),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMinimizedPill(
    config: AppConfig,
    telemetry: com.example.cloudplay.StreamTelemetry,
    isAnchoredRight: Boolean,
    motionEnabled: Boolean,
    onExpandToCompact: () -> Unit
) {
    val chevIcon = if (isAnchoredRight) {
        Icons.AutoMirrored.Filled.KeyboardArrowLeft
    } else {
        Icons.AutoMirrored.Filled.KeyboardArrowRight
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .height(40.dp)
            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
            .clickable(onClick = onExpandToCompact)
            .padding(
                start = if (isAnchoredRight) 10.dp else 8.dp,
                end = if (isAnchoredRight) 8.dp else 10.dp
            )
            .testTag("hud_minimized_pill")
    ) {
        if (!isAnchoredRight) {
            Icon(
                imageVector = chevIcon,
                contentDescription = config.tr("Mở menu nhanh", "Open quick menu"),
                tint = HudInk,
                modifier = Modifier.size(18.dp)
            )
        }
        HudPingDotIndicator(
            pingMs = if (telemetry.rttMs > 0) telemetry.rttMs else null,
            hasSample = telemetry.hasStatsSample,
            fontSize = 13.sp,
            showMsSuffix = false
        )
        if (isAnchoredRight) {
            Icon(
                imageVector = chevIcon,
                contentDescription = config.tr("Mở menu nhanh", "Open quick menu"),
                tint = HudInk,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HudCompactRail(
    config: AppConfig,
    telemetry: com.example.cloudplay.StreamTelemetry,
    isAnchoredRight: Boolean,
    maxHeight: androidx.compose.ui.unit.Dp,
    gamepadActive: Boolean,
    recorderState: com.example.cloudplay.ScreenRecorderState,
    expandFocusRequester: FocusRequester,
    onExpand: () -> Unit,
    onToggleGamepad: () -> Unit,
    onToggleRecording: () -> Unit,
    onReload: () -> Unit,
    onMinimize: () -> Unit,
    onRequestExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HudPingDotIndicator(
            pingMs = if (telemetry.rttMs > 0) telemetry.rttMs else null,
            hasSample = telemetry.hasStatsSample,
            fontSize = 12.sp,
            showMsSuffix = false,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        // Mở rộng (.on)
        HudRailButton(
            icon = Icons.Default.OpenInFull,
            contentDescription = config.tr("Mở rộng", "Expand"),
            bgColor = HudSelBg,
            tint = HudAccent,
            onClick = onExpand,
            modifier = Modifier
                .focusRequester(expandFocusRequester)
                .testTag("hud_expand_button")
        )

        // Tay cầm ảo
        HudRailButton(
            icon = Icons.Default.SportsEsports,
            contentDescription = config.tr("Tay cầm ảo", "Virtual gamepad"),
            bgColor = if (gamepadActive) HudSelBg else Color.Transparent,
            tint = if (gamepadActive) HudAccent else HudInk,
            onClick = onToggleGamepad,
            modifier = Modifier.testTag("hud_toggle_controller_button")
        )

        // Quay màn hình (hiển thị MM:SS khi đang quay)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (recorderState.isRecording) HudDangerBg else Color.Transparent)
                .gamepadClickable(cornerRadius = 13.dp, onClick = onToggleRecording)
                .testTag("hud_record_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = config.tr("Quay màn hình", "Record screen"),
                    tint = if (recorderState.isRecording) HudDanger else HudInk,
                    modifier = Modifier.size(if (recorderState.isRecording) 16.dp else 20.dp)
                )
                if (recorderState.isRecording) {
                    val m = recorderState.elapsedSeconds / 60
                    val s = recorderState.elapsedSeconds % 60
                    Text(
                        text = "%02d:%02d".format(m, s),
                        color = HudDanger,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 1
                    )
                }
            }
        }

        // Tải lại
        HudRailButton(
            icon = Icons.Default.Refresh,
            contentDescription = config.tr("Tải lại", "Reload"),
            bgColor = Color.Transparent,
            tint = HudInk,
            onClick = onReload
        )

        Box(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .size(width = 28.dp, height = 1.dp)
                .background(HudLine)
        )

        // Thu nhỏ
        HudRailButton(
            icon = if (isAnchoredRight) {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowLeft
            },
            contentDescription = config.tr("Thu nhỏ", "Minimize"),
            bgColor = Color.Transparent,
            tint = HudInk,
            onClick = onMinimize,
            modifier = Modifier.testTag("hud_minimize_button")
        )

        // Thoát
        HudRailButton(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = config.tr("Thoát game", "Exit game"),
            bgColor = Color.Transparent,
            tint = HudDanger,
            onClick = onRequestExit,
            modifier = Modifier.testTag("hud_home_button")
        )
    }
}

@Composable
private fun HudRailButton(
    icon: ImageVector,
    contentDescription: String,
    bgColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .gamepadClickable(cornerRadius = 13.dp, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun HudFooterActionButton(
    icon: ImageVector,
    label: String,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDanger) HudDangerBg else HudSurface2
    val fg = if (isDanger) HudDanger else HudInk
    Column(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .gamepadClickable(cornerRadius = 13.dp, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = fg,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
