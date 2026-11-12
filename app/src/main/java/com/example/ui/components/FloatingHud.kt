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
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.example.ui.theme.rememberXboxMotionEnabled
import com.example.ui.theme.xboxHudCollapseExitTransition
import com.example.ui.theme.xboxHudExpandEnterTransition
import com.example.ui.theme.xboxPressScale
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hudMode by rememberSaveable { mutableStateOf(HudMode.MINIMIZED) }
    var dockedEdge by rememberSaveable { mutableStateOf(HudDockedEdge.NONE) }
    var offsetX by rememberSaveable { mutableFloatStateOf(24f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(24f) }
    var hudWidthPx by remember { mutableStateOf(220) }
    var hudHeightPx by remember { mutableStateOf(96) }
    var isDragging by remember { mutableStateOf(false) }
    var lastOrientationPortrait by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isInteracting by remember { mutableStateOf(true) }
    var prevLoggedHudMode by remember { mutableStateOf(hudMode) }

    LaunchedEffect(hudMode) {
        if (prevLoggedHudMode != hudMode) {
            com.example.data.AppLogger.info("UI", "Menu nhanh trong game (HUD): ${prevLoggedHudMode.name} -> ${hudMode.name}")
            prevLoggedHudMode = hudMode
        }
    }

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
                thermalStatus = newStatus
                showThermalWarningBanner = newStatus >= PowerManager.THERMAL_STATUS_MODERATE
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
            } catch (e: Exception) {
                com.example.data.AppLogger.error("SYSTEM", "Lỗi đăng ký theo dõi nhiệt độ máy (${com.example.data.AppLogger.shortThrowable(e)})")
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
            com.example.data.AppLogger.error("SYSTEM", "Lỗi đăng ký theo dõi pin (${com.example.data.AppLogger.shortThrowable(e)})")
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

    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val motionEnabled = rememberXboxMotionEnabled(
        lowEndDeviceMode = lowEndDeviceMode || config.lowEndDeviceMode,
        gpuLoadReductionEnabled = config.gpuLoadReductionEnabled
    )

    // Mục 6 & 7: Cảnh báo nhỏ không chặn trên FloatingHud, tự ẩn sau vài giây
    // Mục 6: Mỗi phiên tối đa 1 lần cho cảnh báo nhiệt độ (THERMAL_STATUS_SEVERE) và 1 lần cho cảnh báo pin yếu (<= 15%, không sạc)
    var hasShownSevereThermalAlert by rememberSaveable(isGameActive) { mutableStateOf(false) }
    var hasShownLowBatteryAlert by rememberSaveable(isGameActive) { mutableStateOf(false) }
    var lastStreamNetworkIsWifi by rememberSaveable(isGameActive) { mutableStateOf<Boolean?>(null) }
    var autoHudBannerText by remember { mutableStateOf<String?>(null) }
    var autoHudBannerTag by remember { mutableStateOf("hud_transient_alert_banner") }

    LaunchedEffect(isGameActive, isThermalSupported, thermalStatus, batteryPercent, isBatteryCharging, isEn) {
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
            autoHudBannerText = if (isEn) {
                "Device hot/low battery, consider lowering FPS or resolution"
            } else {
                "Máy nóng/pin yếu, nên giảm FPS hoặc độ phân giải"
            }
        } else if (batteryPercent in 1..15 && !isBatteryCharging && !hasShownLowBatteryAlert) {
            hasShownLowBatteryAlert = true
            autoHudBannerTag = "hud_thermal_battery_alert_banner"
            autoHudBannerText = if (isEn) {
                "Device hot/low battery, consider lowering FPS or resolution"
            } else {
                "Máy nóng/pin yếu, nên giảm FPS hoặc độ phân giải"
            }
        }
    }

    // Mục 7: Khi mạng đổi giữa Wi-Fi và di động lúc đang stream
    LaunchedEffect(isGameActive, networkStatus.isWifi, networkStatus.isCellular, isEn) {
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
                autoHudBannerText = if (isEn) {
                    "Network switched, if laggy please retry"
                } else {
                    "Mạng vừa đổi, nếu giật hãy thử lại"
                }
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

    // Phân loại biểu tượng nhiệt trên HUD: mát / ấm / nóng
    val thermalIcon = when {
        !isThermalSupported -> ""
        thermalStatus >= 2 -> "🔥"
        thermalStatus == 1 -> "🌡️"
        else -> "❄️"
    }
    val thermalLabel = when {
        !isThermalSupported -> ""
        thermalStatus >= 2 -> if (isEn) "Hot" else "Nóng"
        thermalStatus == 1 -> if (isEn) "Warm" else "Ấm"
        else -> if (isEn) "Cool" else "Mát"
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

        val rightScreenEdgeX = (maxWidthPx - hudWidthPx.toFloat()).coerceAtLeast(0f)
        val maxBoundX = (maxWidthPx - safeRightPx - hudWidthPx.toFloat()).coerceAtLeast(safeLeftPx)
        val maxBoundY = (maxHeightPx - safeBottomPx - hudHeightPx.toFloat()).coerceAtLeast(safeTopPx)
        // Ngưỡng sát rìa trái/phải (10dp): chỉ khi kéo sát mép màn hình mới ẩn thành thanh gờ
        val edgeDockThresholdPx = with(density) { 10.dp.toPx() }
        val restorePaddingPx = with(density) { 14.dp.toPx() }

        // Tính vị trí dải đen phía trên vùng video thực tế khi khởi tạo ở màn DỌC (Portrait)
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

        // Đặt vị trí mặc định khi xoay màn DỌC / NGANG và chỉ giới hạn trong phạm vi màn hình (di chuyển tự do không giới hạn vùng)
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
            animationSpec = if (isDragging || lowEndDeviceMode) tween(0) else tween(200),
            label = "hud_offset_x"
        )
        val animatedOffsetY by animateFloatAsState(
            targetValue = offsetY.coerceIn(safeTopPx, maxBoundY),
            animationSpec = if (isDragging || lowEndDeviceMode) tween(0) else tween(200),
            label = "hud_offset_y"
        )
        val isAnchoredRight = (offsetX + hudWidthPx / 2f) > (maxWidthPx * 0.5f)
        val isNearEdgeForDock = isDragging && hudMode != HudMode.EXPANDED &&
            (offsetX <= edgeDockThresholdPx || offsetX >= (rightScreenEdgeX - edgeDockThresholdPx))

        if (dockedEdge != HudDockedEdge.NONE) {
            // THANH GỜ SÁT RÌA MÀN HÌNH (30x56dp như bản mẫu .hud-handle): Chạm hoặc kéo để mở
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
                RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
            } else {
                RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
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
                    .graphicsLayer {
                        alpha = if (isInteracting || isDragging) 0.98f else 0.55f
                    }
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
                    .clickable {
                        restoreFromEdge()
                    }
                    .border(
                        1.dp,
                        XboxCardBorder,
                        handleShape
                    )
                    .testTag("hud_edge_dock_handle"),
                color = XboxSurface,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (dockedEdge == HudDockedEdge.LEFT) "›" else "‹",
                        color = XboxNeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
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
                    alpha = if (isNearEdgeForDock) 0.65f else hudAlpha
                }
                .pointerInput(maxWidthPx, maxHeightPx, rightScreenEdgeX, safeLeftPx, safeTopPx, maxBoundX, maxBoundY, edgeDockThresholdPx, hudMode) {
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
                }
                .clip(RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (isNearEdgeForDock) XboxNeonGreen else XboxCardBorder,
                    RoundedCornerShape(14.dp)
                )
                .testTag("floating_hud_container"),
            color = XboxSurface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(if (hudMode == HudMode.EXPANDED) 12.dp else 6.dp),
                horizontalAlignment = if (isAnchoredRight) Alignment.End else Alignment.Start
            ) {
                // 1. TRẠNG THÁI THU NHỎ (MINIMIZED): Thanh gọn kèm mũi tên › và ping
                if (hudMode == HudMode.MINIMIZED) {
                    val rawPing: Int? = if (telemetry.hasStatsSample && telemetry.rttMs > 0) telemetry.rttMs else null
                    val hasValidPing = rawPing != null && rawPing > 0
                    val pingColor = when {
                        !hasValidPing -> TextSecondary
                        rawPing < 80 -> XboxNeonGreen
                        rawPing <= 120 -> Color(0xFFE3B341)
                        else -> JapanRed
                    }
                    val pingText = if (!hasValidPing) "—" else "${rawPing}ms"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                markUserInteraction()
                                hudMode = HudMode.COMPACT
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("hud_minimized_pill")
                    ) {
                        Text(
                            text = "›",
                            color = XboxNeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
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
                    }
                } else if (hudMode == HudMode.COMPACT) {
                    // ==========================================
                    // 2. TRẠNG THÁI GỌN (COMPACT): Cột dọc các nút 44dp phẳng theo bản mẫu .hud-compact
                    // ==========================================
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Nút Mở rộng sang bảng Menu nhanh (›)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = XboxSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    markUserInteraction()
                                    hudMode = HudMode.EXPANDED
                                }
                                .testTag("hud_expand_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "›",
                                    color = XboxNeonGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Nút Mạng / Thông số (chuyển nhanh VPN / Direct)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (vpnState.mode != NetworkMode.DIRECT_NETWORK) Color(0xFF0F2C1D) else XboxSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (vpnState.mode != NetworkMode.DIRECT_NETWORK) XboxNeonGreen else XboxCardBorder
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    markUserInteraction()
                                    onToggleNetwork()
                                }
                                .testTag("hud_toggle_network_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = if (isEn) "Network & stats" else "Thông số mạng",
                                    tint = if (vpnState.mode != NetworkMode.DIRECT_NETWORK) XboxNeonGreen else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Nút Tay cầm ảo
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (virtualControllerEnabled || physicalControllerConnected) Color(0xFF0F2C1D) else XboxSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (virtualControllerEnabled || physicalControllerConnected) XboxNeonGreen else XboxCardBorder
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    markUserInteraction()
                                    onToggleVirtualController()
                                }
                                .testTag("hud_toggle_controller_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = if (isEn) "Gamepad" else "Tay cầm",
                                    tint = if (virtualControllerEnabled || physicalControllerConnected) XboxNeonGreen else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Nút Quay màn hình
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (recorderState.isRecording) Color(0xFF331A19) else XboxSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (recorderState.isRecording) Color(0xFF612826) else XboxCardBorder
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    markUserInteraction()
                                    onToggleRecording()
                                }
                                .testTag("hud_record_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (recorderState.isRecording) JapanRed else TextPrimary)
                                )
                            }
                        }

                        // Nút Thoát (đỏ cảnh báo)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF331A19),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF612826)),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    markUserInteraction()
                                    showExitConfirmDialog = true
                                }
                                .testTag("hud_home_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isEn) "Exit game" else "Thoát game",
                                    tint = JapanRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Nút thu nhỏ về viên thuốc gọn
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    markUserInteraction()
                                    hudMode = HudMode.MINIMIZED
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("hud_minimize_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "─",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // ==========================================
                    // 3. TRẠNG THÁI MỞ RỘNG (EXPANDED): Bảng Menu nhanh chuẩn bản mẫu .hud-panel
                    // ==========================================
                    val panelMaxWidth = 272.dp
                    val panelMaxHeight = (screenMaxHeight * 0.78f).coerceAtLeast(240.dp)
                    val scrollState = rememberScrollState()
                    var showMoreHudOptions by rememberSaveable { mutableStateOf(false) }

                    val hasRealSample = telemetry.hasStatsSample
                    val effectivePing: Int? = if (hasRealSample && telemetry.rttMs > 0) telemetry.rttMs else null
                    val pingValStr = effectivePing?.toString() ?: "—"
                    val fpsValStr = if (hasRealSample && telemetry.fps > 0) "${telemetry.fps}" else "—"
                    val jitterValStr = if (hasRealSample && (telemetry.jitterMs > 0 || effectivePing != null)) "${telemetry.jitterMs}" else "—"
                    val mbpsValStr = if (hasRealSample && telemetry.bitrateMbps > 0) String.format("%.1f", telemetry.bitrateMbps) else "—"

                    Column(
                        modifier = Modifier
                            .width(panelMaxWidth)
                            .heightIn(max = panelMaxHeight)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Hàng đầu: "Menu nhanh" + nút thu gọn 36dp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gameProfile?.title ?: if (isEn) "Quick Menu" else "Menu nhanh",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            markUserInteraction()
                                            hudMode = HudMode.COMPACT
                                        }
                                        .testTag("hud_collapse_48dp_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "‹",
                                            color = TextSecondary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            markUserInteraction()
                                            hudMode = HudMode.MINIMIZED
                                        }
                                        .testTag("hud_minimize_48dp_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "─",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Cảnh báo nhiệt độ khi từ MODERATE trở lên
                        if (isThermalSupported && showThermalWarningBanner) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showThermalWarningBanner = false }
                                    .testTag("hud_thermal_warning_banner"),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF331A19),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF612826))
                            ) {
                                Text(
                                    text = if (isEn) "Device is warm, consider lowering resolution" else "Máy đang nóng, nên hạ chất lượng",
                                    color = JapanRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    maxLines = 2
                                )
                            }
                        }

                        // Lưới 4 cột thông số (.hud-stats): ms | FPS | jitter | Mbps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val statsList = listOf(
                                Triple(pingValStr, "ms", true),
                                Triple(fpsValStr, "FPS", false),
                                Triple(jitterValStr, "jitter", false),
                                Triple(mbpsValStr, "Mbps", false)
                            )
                            statsList.forEach { (numStr, unitStr, highlightGreen) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = numStr,
                                            color = if (highlightGreen) XboxNeonGreen else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = unitStr,
                                            color = TextSecondary,
                                            fontSize = 10.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Lưới 3x2 nút hành động (.hud-actions): Tay cầm, Đồ họa, Xoay, Quay, Tải lại, Thoát
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Tay cầm
                                val padOn = virtualControllerEnabled || physicalControllerConnected
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            markUserInteraction()
                                            onToggleVirtualController()
                                        }
                                        .testTag("hud_toggle_controller_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (padOn) Color(0xFF0F2C1D) else XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (padOn) XboxNeonGreen else XboxCardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gamepad,
                                            contentDescription = null,
                                            tint = if (padOn) XboxNeonGreen else TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isEn) "Gamepad" else "Tay cầm",
                                            color = if (padOn) XboxNeonGreen else TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 2. Đồ họa (mở rộng tùy chỉnh chất lượng/làm nét/màu sắc ngay trong HUD)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            markUserInteraction()
                                            showMoreHudOptions = !showMoreHudOptions
                                        }
                                        .testTag("hud_more_options_toggle_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (showMoreHudOptions) Color(0xFF0F2C1D) else XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (showMoreHudOptions) XboxNeonGreen else XboxCardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = if (showMoreHudOptions) XboxNeonGreen else TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isEn) "Graphics" else "Đồ họa",
                                            color = if (showMoreHudOptions) XboxNeonGreen else TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 3. Xoay
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            markUserInteraction()
                                            onToggleOrientation()
                                        }
                                        .testTag("hud_expanded_rotate_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenRotation,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isEn) "Rotate" else "Xoay",
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 4. Quay
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            markUserInteraction()
                                            if (!recorderState.isRecording) {
                                                hudMode = HudMode.COMPACT
                                            }
                                            onToggleRecording()
                                        }
                                        .testTag("hud_expanded_record_card"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (recorderState.isRecording) Color(0xFF331A19) else XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (recorderState.isRecording) Color(0xFF612826) else XboxCardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(if (recorderState.isRecording) JapanRed else TextPrimary)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (recorderState.isRecording) {
                                                val mins = recorderState.elapsedSeconds / 60
                                                val secs = recorderState.elapsedSeconds % 60
                                                "%02d:%02d".format(mins, secs)
                                            } else {
                                                if (isEn) "Record" else "Quay"
                                            },
                                            color = if (recorderState.isRecording) JapanRed else TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 5. Tải lại
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            markUserInteraction()
                                            onReloadPage()
                                        }
                                        .testTag("hud_expanded_reload_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isEn) "Reload" else "Tải lại",
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 6. Thoát (màu đỏ cảnh báo)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            markUserInteraction()
                                            com.example.data.AppLogger.info("UI", "Mở hộp thoại xác nhận Thoát game từ HUD")
                                            showExitConfirmDialog = true
                                        }
                                        .testTag("hud_expanded_home_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF331A19),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF612826))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = JapanRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isEn) "Exit" else "Thoát",
                                            color = JapanRed,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Bảng mở rộng chi tiết khi người dùng bấm "Đồ họa"
                        if (showMoreHudOptions) {
                            DenseQualityModeSection(
                                networkStatus = networkStatus,
                                adaptiveQualityState = adaptiveQualityState,
                                telemetry = telemetry,
                                isVpnActive = vpnState.mode != NetworkMode.DIRECT_NETWORK,
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

                            if (gameProfile?.gameId == "fortnite") {
                                FortniteExclusiveHudCard(
                                    isEn = isEn,
                                    virtualControllerEnabled = virtualControllerEnabled,
                                    onSelectFortniteControlMode = { useVirtualFpsPro ->
                                        markUserInteraction()
                                        onSelectFortniteControlMode(useVirtualFpsPro)
                                    }
                                )
                            }

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
                                onToggleGpuLoadReduction = {
                                    markUserInteraction()
                                    val nextState = !config.gpuLoadReductionEnabled
                                    com.example.data.SettingsRepository.updateGpuLoadReductionEnabled(nextState)
                                }
                            )

                            if (virtualControllerEnabled) {
                                DenseQuickGamepadSection(
                                    virtualControllerEnabled = true,
                                    onInteraction = { markUserInteraction() }
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        markUserInteraction()
                                        onOpenSettings()
                                    }
                                    .testTag("hud_expanded_video_settings_card"),
                                shape = RoundedCornerShape(8.dp),
                                color = XboxSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isEn) "Full Settings" else "Mở Cài đặt đầy đủ",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "›",
                                        color = XboxNeonGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Banner cảnh báo nhỏ tự ẩn sau vài giây (Mục 6: Máy nóng/pin yếu; Mục 7: Đổi mạng Wi-Fi/di động)
                val currentBanner = autoHudBannerText
                if (currentBanner != null) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .widthIn(max = 250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { autoHudBannerText = null }
                            .testTag(autoHudBannerTag),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xEB18241D),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFFB300).copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = currentBanner,
                            color = Color(0xFFFFE082),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 13.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        }
        }
    }

    // Exit Game Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                com.example.data.AppLogger.info("UI", "Hủy hộp thoại Thoát game từ HUD")
                showExitConfirmDialog = false
            },
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
                        com.example.data.AppLogger.info("UI", "Xác nhận Thoát game từ HUD -> về Trang chủ")
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
                TextButton(onClick = {
                    com.example.data.AppLogger.info("UI", "Hủy hộp thoại Thoát game từ HUD")
                    showExitConfirmDialog = false
                }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary, fontSize = 12.sp)
                }
            },
            containerColor = XboxSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}


