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
            // THANH GỜ SÁT RÌA MÀN HÌNH: Vuốt qua (hoặc chạm) để bật lại nút nổi
            val handleWidthDp = 20.dp
            val handleHeightDp = 62.dp
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

            val handleAccent = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) DirectCyan else XboxNeonGreen
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
                    .graphicsLayer {
                        alpha = if (isInteracting || isDragging) 0.96f else 0.45f
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
                                // Cho phép trượt dọc thanh gờ dọc theo rìa màn hình
                                offsetY = (offsetY + dragAmount.y).coerceIn(safeTopPx, handleMaxY)
                                // Nếu vuốt qua vào trong màn hình -> bật lại nút nổi ngay lập tức
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
                        1.2.dp,
                        handleAccent.copy(alpha = 0.85f),
                        handleShape
                    )
                    .testTag("hud_edge_dock_handle"),
                color = Color(0xF209120D),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (dockedEdge == HudDockedEdge.LEFT) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(handleAccent)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "›",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        Text(
                            text = "‹",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(26.dp)
                                .clip(CircleShape)
                                .background(handleAccent)
                        )
                    }
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
                            // Chỉ khi kéo SÁT rìa màn hình bên trái hoặc bên phải mới ẩn nút nổi thành thanh gờ
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
                            // Di chuyển tự do không giới hạn vùng (chỉ giới hạn trong khung màn hình)
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, rightScreenEdgeX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(safeTopPx, maxBoundY)
                        }
                    )
                }
                .clip(RoundedCornerShape(12.dp))
                .border(
                    if (isNearEdgeForDock) 1.5.dp else 1.dp,
                    if (isNearEdgeForDock) XboxNeonGreen else Color(0xFF24382C),
                    RoundedCornerShape(12.dp)
                )
                .testTag("floating_hud_container"),
            color = Color(0xF20D1511),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalAlignment = if (isAnchoredRight) Alignment.End else Alignment.Start
            ) {
                // MINIMIZED STATE: Ultra-compact 1-line Capsule Pill (Ping + Controller icon)
                if (hudMode == HudMode.MINIMIZED) {
                    val rawPing: Int? = if (telemetry.hasStatsSample && telemetry.rttMs > 0) telemetry.rttMs else null
                    val hasValidPing = rawPing != null && rawPing > 0
                    val pingColor = when {
                        !hasValidPing -> Color.Gray
                        rawPing < 80 -> XboxNeonGreen
                        rawPing <= 120 -> Color(0xFFFFB300)
                        else -> JapanRed
                    }
                    val pingText = if (!hasValidPing) {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡—" else "${vpnState.currentServer.flag}—"
                    } else {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡${rawPing}ms" else "${vpnState.currentServer.flag}${rawPing}ms"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .xboxPressScale(enabled = motionEnabled, pressedScale = 0.94f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                markUserInteraction()
                                hudMode = HudMode.COMPACT
                            }
                            .padding(horizontal = 7.dp, vertical = 3.5.dp)
                            .testTag("hud_minimized_pill")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.5.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Text(
                            text = pingText,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = if (isEn) "Expand quick controls" else "Mở điều khiển nhanh",
                            tint = if (physicalControllerConnected || virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                            modifier = Modifier.size(11.5.dp)
                        )
                    }
                } else {
                    // ==========================================
                    // COMPACT STATE: ULTRA-SLIM ROW WITH HOME BUTTON (~145dp)
                    // ==========================================
                    val compactRawPing: Int? = if (telemetry.hasStatsSample && telemetry.rttMs > 0) telemetry.rttMs else null
                    val compactHasValidPing = compactRawPing != null && compactRawPing > 0
                    val pingColor = when {
                        !compactHasValidPing -> Color.Gray
                        compactRawPing < 80 -> XboxNeonGreen
                        compactRawPing <= 120 -> Color(0xFFFFB300)
                        else -> JapanRed
                    }
                    val compactPingText = if (!compactHasValidPing) {
                        if (vpnState.mode == NetworkMode.DIRECT_NETWORK) "⚡—" else "${vpnState.currentServer.flag}—"
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
                                    contentDescription = if (isEn) "Home" else "Trang chủ",
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
                                    contentDescription = if (isEn) "Gamepad" else "Tay cầm",
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
                                    contentDescription = if (isEn) "Clarity Boost" else "Làm nét",
                                    tint = if (clarityBoostActive) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // 4.5. Recording Status Pill (Only displayed when actively recording so secondary feature does not clutter primary gameplay bar)
                        if (recorderState.isRecording) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x44FF3B30),
                                border = androidx.compose.foundation.BorderStroke(0.9.dp, Color(0xFFFF3B30)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        lastInteractionTime = System.currentTimeMillis()
                                        isInteracting = true
                                        onToggleRecording()
                                    }
                                    .testTag("hud_record_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30))
                                    )
                                    val mins = recorderState.elapsedSeconds / 60
                                    val secs = recorderState.elapsedSeconds % 60
                                    Text(
                                        text = "%02d:%02d".format(mins, secs),
                                        color = Color(0xFFFF8A80),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }


                        // 5. Expand Game Bar Menu Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hudMode == HudMode.EXPANDED) XboxNeonGreen.copy(alpha = 0.3f) else Color(0x33233A2D),
                            modifier = Modifier
                                .xboxPressScale(enabled = motionEnabled, pressedScale = 0.92f)
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
                                    contentDescription = if (isEn) "Settings menu" else "Menu tùy chỉnh",
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
                    val panelMaxWidth = (screenMaxWidth * 0.48f).coerceIn(248.dp, 312.dp)
                    val panelMaxHeight = (screenMaxHeight * 0.68f).coerceAtLeast(220.dp)
                    val scrollState = rememberScrollState()

                    AnimatedVisibility(
                        visible = hudMode == HudMode.EXPANDED,
                        enter = if (!motionEnabled) EnterTransition.None else xboxHudExpandEnterTransition(),
                        exit = if (!motionEnabled) ExitTransition.None else xboxHudCollapseExitTransition()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 4.dp, start = 2.dp, end = 2.dp, bottom = 2.dp)
                                .width(panelMaxWidth)
                                .widthIn(min = 248.dp, max = 312.dp)
                                .heightIn(max = panelMaxHeight),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
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
                                        text = if (isEn) "🔥 Device is hot, take a break or lower quality" else "🔥 Máy đang nóng, nên nghỉ hoặc hạ chất lượng",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 2
                                    )
                                }
                            }

                            // Nội dung bên trong cuộn dọc nếu màn hình thấp
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // ========================================================
                                // BẢNG THÔNG SỐ CỐT LÕI (Ping, FPS, Giải mã, Tay cầm, Nhiệt độ & Pin)
                                // ========================================================
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
                                    },
                                    adaptiveBufferMs = adaptiveQualityState.smoothBufferMs.roundToInt()
                                )

                                // ========================================================
                                // NHÓM 1: ĐỘ PHÂN GIẢI (Tự động / 720p / 900p / 1080p / 1080p+)
                                // ========================================================
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

                                // ========================================================
                                // NHÓM 2: ĐIỀU KHIỂN (Chế độ điều khiển & Công tắc nhanh)
                                // ========================================================
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

                                // ========================================================
                                // NHÓM 3: MÀU SẮC (Độ nét, FPS & Màu sắc hình ảnh)
                                // ========================================================
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
                                    color = Color(0xFF121D17),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF24382C))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = if (isEn) "Colors & Graphics" else "Màu sắc & Đồ họa",
                                                color = TextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (isEn) {
                                                    "${config.cellularMode.localizedTitle(true)} • ${config.streamFpsCap.fps} FPS • Contrast ${config.videoContrast}%"
                                                } else {
                                                    "${config.cellularMode.localizedTitle(false)} • ${config.streamFpsCap.fps} FPS • Tương phản ${config.videoContrast}%"
                                                },
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 2
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1C2E24)
                                        ) {
                                            Text(
                                                text = if (isEn) "Open" else "Mở",
                                                color = XboxNeonGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // ========================================================
                                // NHÓM 4: QUAY MÀN HÌNH (.MP4)
                                // ========================================================
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            markUserInteraction()
                                            if (!recorderState.isRecording) {
                                                hudMode = HudMode.COMPACT
                                            }
                                            onToggleRecording()
                                        }
                                        .testTag("hud_expanded_record_card"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (recorderState.isRecording) Color(0x33FF3B30) else Color(0xFF0F1813),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.8.dp,
                                        if (recorderState.isRecording) Color(0xFFFF3B30) else Color(0x40808080)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (recorderState.isRecording) Color(0xFFFF3B30) else Color(0xFF888888))
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = if (recorderState.isRecording) {
                                                        val mins = recorderState.elapsedSeconds / 60
                                                        val secs = recorderState.elapsedSeconds % 60
                                                        if (isEn) "Recording %02d:%02d — Tap to Save".format(mins, secs)
                                                        else "Đang quay %02d:%02d — Chạm để Lưu".format(mins, secs)
                                                    } else {
                                                        if (isEn) "Record Screen (.MP4)" else "Quay màn hình (.MP4)"
                                                    },
                                                    color = if (recorderState.isRecording) Color(0xFFFF8A80) else TextPrimary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (isEn) {
                                                        "${config.screenRecordQuality.shortEn} • Game Audio"
                                                    } else {
                                                        "${config.screenRecordQuality.shortVi} • Có tiếng game"
                                                    },
                                                    color = Color(0xFF90A89B),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (recorderState.isRecording) {
                                                if (isEn) "SAVE" else "LƯU"
                                            } else {
                                                if (isEn) "REC" else "QUAY"
                                            },
                                            color = if (recorderState.isRecording) Color(0xFFFF5252) else DirectCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                // ========================================================
                                // HÀNG NÚT TÁC VỤ DƯỚI CÙNG (Tải lại, Xoay, Đồ họa, Thoát)
                                // ========================================================
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

@Composable
private fun FortniteExclusiveHudCard(
    isEn: Boolean,
    virtualControllerEnabled: Boolean,
    onSelectFortniteControlMode: (Boolean) -> Unit
) {
    val fortniteBlue = Color(0xFF0095FF)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hud_fortnite_arena_card"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0A1929).copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, fortniteBlue.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "Fortnite Controls" else "Điều khiển Fortnite",
                    color = fortniteBlue,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = if (isEn) "1-Tap Switch" else "Đổi 1 chạm",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val isEpicTouchActive = !virtualControllerEnabled
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isEpicTouchActive) fortniteBlue.copy(alpha = 0.28f) else Color(0x26FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isEpicTouchActive) fortniteBlue else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectFortniteControlMode(false) }
                        .testTag("hud_fortnite_epic_touch_btn")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "👆 Epic Touch" else "👆 Cảm ứng Epic",
                            color = if (isEpicTouchActive) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (virtualControllerEnabled) XboxNeonGreen.copy(alpha = 0.25f) else Color(0x26FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (virtualControllerEnabled) XboxNeonGreen else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectFortniteControlMode(true) }
                        .testTag("hud_fortnite_fps_pad_btn")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "🎮 Virtual Pad" else "🎮 Tay cầm ảo",
                            color = if (virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
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
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF101D16),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isAnchoredRight) {
                CollapseTouchButton48dp(
                    isEn = isEn,
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
                    text = gameProfile?.title ?: if (isEn) "Quick Controls" else "Điều khiển nhanh",
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (gameProfile != null) {
                        gameProfile.recommendedFps
                    } else {
                        if (isEn) "Tap Collapse to close" else "Chạm Thu gọn để đóng"
                    },
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (isAnchoredRight) {
                Spacer(modifier = Modifier.width(6.dp))
                CollapseTouchButton48dp(
                    isEn = isEn,
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
    isEn: Boolean = false,
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
                .width(92.dp)
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
                    contentDescription = if (isEn) "Collapse HUD" else "Thu gọn bảng HUD",
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isEn) "Collapse" else "Thu gọn",
                    color = Color.White,
                    fontSize = 11.sp,
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
 * B.6 & B.7: Bảng thông số cốt lõi trong bảng mở rộng (cỡ chữ >= 11sp).
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
    onToggleVirtualController: () -> Unit,
    adaptiveBufferMs: Int = 12
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

    // Trong khi chơi chỉ hiển thị RTT từ WebRTC candidate-pair; nếu không có thì hiện "—", không rơi về ping TCP/VPN
    val hasRealSample = telemetry.hasStatsSample
    val effectivePing: Int? = if (hasRealSample && telemetry.rttMs > 0) telemetry.rttMs else null
    val pingTierLabel = when {
        effectivePing == null || effectivePing <= 0 -> if (isEn) "Unmeasured" else "Chưa đo"
        effectivePing < 80 -> if (isEn) "Good" else "Tốt"
        effectivePing <= 120 -> if (isEn) "Moderate" else "Trung bình"
        else -> if (isEn) "High" else "Cao"
    }
    val latencyDisplayText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
        if (effectivePing != null) "Direct • ${effectivePing}ms ($pingTierLabel)" else "Direct • — ($pingTierLabel)"
    } else {
        if (effectivePing != null) "VPN ${vpnState.currentServer.flag} • ${effectivePing}ms ($pingTierLabel)" else "VPN ${vpnState.currentServer.flag} • — ($pingTierLabel)"
    }

    val jitterVal = telemetry.jitterMs
    val hasValidJitter = hasRealSample && (jitterVal > 0 || effectivePing != null)
    val jitterTierLabel = when {
        !hasValidJitter -> if (isEn) "Unmeasured" else "Chưa đo"
        jitterVal < 8 -> if (isEn) "Good" else "Tốt"
        jitterVal <= 20 -> if (isEn) "Moderate" else "Trung bình"
        else -> if (isEn) "High" else "Cao"
    }
    val jitterColor = when {
        !hasValidJitter -> TextSecondary
        jitterVal < 8 -> XboxNeonGreen
        jitterVal <= 20 -> Color(0xFFFFB300)
        else -> JapanRed
    }
    val jitterDisplayText = if (hasValidJitter) "${jitterVal}ms ($jitterTierLabel)" else "— ($jitterTierLabel)"

    val fpsValueText = if (hasRealSample && telemetry.fps > 0) "${telemetry.fps} FPS" else "— FPS"
    val bitrateValueText = if (hasRealSample && telemetry.bitrateMbps > 0) {
        String.format("%.1f Mbps", telemetry.bitrateMbps)
    } else {
        "— Mbps"
    }

    val liveBufMs = if (hasRealSample && telemetry.jitterBufferDelayMs > 1.0) {
        telemetry.jitterBufferDelayMs.roundToInt().coerceIn(6, 60)
    } else {
        adaptiveBufferMs.coerceIn(8, 45)
    }
    val bufferLabel = if (isEn) "Data buffer" else "Bộ đệm dữ liệu"
    val decodeTimeText = if (hasRealSample && telemetry.decodeTimeMs > 0.0) {
        String.format("%.1fms • %s %dms", telemetry.decodeTimeMs, bufferLabel, liveBufMs)
    } else {
        "— • $bufferLabel ${liveBufMs}ms"
    }
    // Chuẩn ITU-T G.1072 / 60Hz (16.6ms): < 8ms Xanh, 8-16ms Vàng, > 16ms Đỏ
    val decodeColor = when {
        !hasRealSample || telemetry.decodeTimeMs <= 0.0 -> TextSecondary
        telemetry.decodeTimeMs < 8.0 -> XboxNeonGreen
        telemetry.decodeTimeMs <= 16.0 -> Color(0xFFFFB300)
        else -> JapanRed
    }

    val droppedCount = telemetry.framesDropped
    val freezeCount = telemetry.freezeCount
    val dropRatioPct = if (telemetry.framesDecoded > 0) {
        (droppedCount.toDouble() * 100.0) / (telemetry.framesDecoded + droppedCount).toDouble()
    } else 0.0
    val dropTierLabel = when {
        telemetry.framesDecoded <= 0 && droppedCount == 0 -> if (isEn) "Good" else "Tốt"
        dropRatioPct < 0.5 -> if (isEn) "Good" else "Tốt"
        dropRatioPct <= 2.0 -> if (isEn) "Moderate" else "Trung bình"
        else -> if (isEn) "High" else "Cao"
    }
    val dropColor = when {
        dropRatioPct < 0.5 -> XboxNeonGreen
        dropRatioPct <= 2.0 -> Color(0xFFFFB300)
        else -> JapanRed
    }
    val droppedAndFreezeText = if (isEn) {
        "$droppedCount ($dropTierLabel) • Temporary freeze: $freezeCount"
    } else {
        "$droppedCount ($dropTierLabel) • Đứng hình tạm thời: $freezeCount"
    }

    val pingMetricColor = when {
        effectivePing == null || effectivePing <= 0 -> TextSecondary
        effectivePing < 80 -> XboxNeonGreen
        effectivePing <= 120 -> Color(0xFFFFB300)
        else -> JapanRed
    }

    val controllerStatusText = when {
        physicalControllerConnected -> controllerName
        virtualControllerEnabled -> stringResource(R.string.hud_virtual_gamepad)
        else -> stringResource(R.string.controller_disconnected)
    }

    val batteryStatusText = if (batteryPercent >= 0) {
        val chargeStr = if (isBatteryCharging) {
            if (isEn) "Charging ⚡" else "Đang sạc ⚡"
        } else {
            if (isEn) "Battery" else "Pin"
        }
        "$batteryPercent% • $chargeStr"
    } else {
        if (isEn) "--%" else "--%"
    }
    val batteryColor = when {
        isBatteryCharging -> XboxNeonGreen
        batteryPercent in 0..20 -> JapanRed
        batteryPercent in 21..35 -> Color(0xFFFFB300)
        else -> TextPrimary
    }

    // Phân loại tự động 3 trạng thái theo chuẩn ITU-T G.1072: [1 câu nguyên nhân] + [1 câu gợi ý]
    val pVal = effectivePing ?: 0
    val jVal = telemetry.jitterMs
    val dVal = telemetry.decodeTimeMs
    val isRedState = pVal > 120 || jVal > 20 || dVal > 16.0
    val isYellowState = !isRedState && (pVal in 80..120 || jVal in 8..20 || dVal in 8.0..16.0)
    val statusAssessmentColor = when {
        pVal <= 0 && dVal <= 0.0 -> TextSecondary
        isRedState -> JapanRed
        isYellowState -> Color(0xFFFFB300)
        else -> XboxNeonGreen
    }
    val statusAssessmentText = when {
        pVal <= 0 && dVal <= 0.0 -> if (isEn) {
            "Waiting for WebRTC stream metrics..."
        } else {
            "Đang đồng bộ thông số luồng WebRTC..."
        }
        isRedState -> when {
            vpnState.mode != NetworkMode.DIRECT_NETWORK && pVal > 120 -> if (isEn) {
                "High network latency (${pVal}ms). Tip: Turn off VPN now to route directly."
            } else {
                "Độ trễ mạng tăng cao (${pVal}ms). Gợi ý: Tắt VPN ngay sau khi vào game để giảm trễ."
            }
            dVal > 16.0 -> if (isEn) {
                "Slow hardware decode (${String.format("%.1f", dVal)}ms > 16.6ms). Tip: Switch to 720p or cool down device."
            } else {
                "Thời gian giải mã cao (${String.format("%.1f", dVal)}ms > 16.6ms). Gợi ý: Chuyển về 720p hoặc để máy hạ nhiệt."
            }
            jVal > 20 -> if (isEn) {
                "High network jitter (${jVal}ms). Tip: Move closer to Wi-Fi or stronger 4G area."
            } else {
                "Độ biến động mạng cao (${jVal}ms). Gợi ý: Di chuyển tới nơi sóng 4G mạnh hơn hoặc dùng Wi-Fi 5GHz."
            }
            else -> if (isEn) {
                "High network latency (${pVal}ms). Tip: Pause background downloads or check VPN."
            } else {
                "Độ trễ mạng tăng cao (${pVal}ms). Gợi ý: Kiểm tra xem VPN đã tắt chưa hoặc tạm dừng tải ngầm."
            }
        }
        isYellowState -> if (isEn) {
            "Moderate connection (Network latency: ${pVal}ms | Network jitter: ${jVal}ms). Tip: Keep Auto quality enabled."
        } else {
            "Đường truyền biến động nhẹ (Độ trễ mạng: ${pVal}ms | Độ biến động mạng: ${jVal}ms). Gợi ý: Giữ chế độ Tự động."
        }
        else -> if (isEn) {
            "Stable stream (Network latency: ${pVal}ms | Decode time: ${String.format("%.1f", dVal)}ms)."
        } else {
            "Ổn định (Độ trễ mạng: ${pVal}ms | Thời gian giải mã: ${String.format("%.1f", dVal)}ms)."
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF121F18),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Dòng Độ trễ mạng (chạm để chuyển nhanh Direct / VPN)
            DenseMetricRowItem(
                label = if (isEn) "Network latency" else "Độ trễ mạng",
                value = latencyDisplayText,
                valueColor = pingMetricColor,
                badgeText = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) {
                    if (isEn) "VPN" else "Đổi VPN"
                } else {
                    if (isEn) "Off" else "Tắt VPN"
                },
                onRowClick = onToggleNetwork
            )

            // 2. Dòng Độ biến động mạng (C1 & C3)
            DenseMetricRowItem(
                label = if (isEn) "Network jitter" else "Độ biến động mạng",
                value = jitterDisplayText,
                valueColor = jitterColor
            )

            // 3. Dòng Chất lượng truyền tải & FPS (C1)
            DenseMetricRowItem(
                label = if (isEn) "Stream bitrate" else "Chất lượng truyền tải",
                value = "$bitrateValueText ($fpsValueText)",
                valueColor = if (telemetry.fps >= 55) XboxNeonGreen else TextPrimary
            )

            // 4. Dòng Thời gian giải mã & Bộ đệm dữ liệu (C1)
            DenseMetricRowItem(
                label = if (isEn) "Decode time" else "Thời gian giải mã",
                value = decodeTimeText,
                valueColor = decodeColor
            )

            // 5. Dòng Khung hình bị rớt & Đứng hình tạm thời (C1 & C3)
            DenseMetricRowItem(
                label = if (isEn) "Dropped frames" else "Khung hình bị rớt",
                value = droppedAndFreezeText,
                valueColor = dropColor
            )

            // 6. Dòng Tay cầm (chạm để bật/tắt tay cầm ảo)
            DenseMetricRowItem(
                label = stringResource(R.string.hud_controller),
                value = controllerStatusText,
                valueColor = if (physicalControllerConnected || virtualControllerEnabled) XboxNeonGreen else TextSecondary,
                badgeText = if (virtualControllerEnabled) {
                    if (isEn) "Hide" else "Ẩn ảo"
                } else {
                    if (isEn) "Show" else "Bật ảo"
                },
                onRowClick = onToggleVirtualController
            )

            // 7. Dòng % Pin & trạng thái sạc (+ Nhiệt độ máy trên API 29+)
            DenseMetricRowItem(
                label = if (isEn) "Battery" else "Pin & Máy",
                value = batteryStatusText,
                valueColor = batteryColor,
                badgeText = if (isThermalSupported) "$thermalIcon $thermalLabel" else null
            )

            // 8. Hộp đánh giá tự động theo chuẩn ITU-T G.1072 ([Nguyên nhân] + [Gợi ý])
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = statusAssessmentColor.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, statusAssessmentColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = statusAssessmentText,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    lineHeight = 13.5.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
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
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
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
                fontSize = 11.sp,
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Nhóm 1: Trần SDP (phiên sau), Đệm nhận tự động & Chẩn đoán 4 lớp "Vì sao tôi bị lag?" (cỡ chữ >= 11sp).
 */
@Composable
private fun DenseQualityModeSection(
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus,
    adaptiveQualityState: com.example.cloudplay.AdaptiveQualityState,
    telemetry: com.example.cloudplay.StreamTelemetry,
    isVpnActive: Boolean = false,
    onSelectCellularMode: (com.example.data.CellularOptimizationMode) -> Unit,
    onToggleAutoAdapting: (Boolean) -> Unit,
    onToggleWifiSpeedFirst: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val liveMbps = adaptiveQualityState.smoothBitrateMbps.coerceIn(1.0, 20.0)
    val liveBufMs = adaptiveQualityState.smoothBufferMs.roundToInt().coerceIn(8, 45)
    val selectedMode = config.cellularMode
    var showLagDiagnostic by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C16),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxNeonGreen.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "SDP Cap (next) & Buffer" else "Trần SDP (phiên sau) & Đệm",
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                val wifiBandText = when (networkStatus.physicalType) {
                    com.example.cloudplay.PhysicalNetworkType.WIFI_6GHZ -> "Wi-Fi 6GHz"
                    com.example.cloudplay.PhysicalNetworkType.WIFI_5GHZ -> "Wi-Fi 5GHz"
                    com.example.cloudplay.PhysicalNetworkType.WIFI_2_4GHZ -> "Wi-Fi 2.4GHz"
                    else -> "Wi-Fi"
                }
                val signalInfo = when {
                    networkStatus.isWifi && networkStatus.wifiRssiDbm < 0 ->
                        "$wifiBandText • Đệm ${liveBufMs}ms • SDP ${String.format("%.1f", liveMbps)}M"
                    networkStatus.isCellular ->
                        "📶 4G • Đệm ${liveBufMs}ms • SDP ${String.format("%.1f", liveMbps)}M"
                    else ->
                        "Đệm ${liveBufMs}ms • SDP ${String.format("%.1f", liveMbps)}M"
                }
                val warnAmber = Color(0xFFF59E0B)
                Text(
                    text = signalInfo,
                    color = if (networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal) warnAmber else DirectCyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Hàng 1: Tự động, 720p, 900p
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQualityPresetChip(
                    label = if (isEn) "Auto" else "Tự động",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.AUTO,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.AUTO) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "720p",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = "900p",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.MEDIUM,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.MEDIUM) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Hàng 2: 1080p & 1080p+
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQualityPresetChip(
                    label = if (isEn) "1080p High" else "1080p Cao",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.HIGH,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.HIGH) },
                    modifier = Modifier.weight(1f)
                )
                DenseQualityPresetChip(
                    label = if (isEn) "1080p+ Max" else "1080p+ Tối đa",
                    isSelected = selectedMode == com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED,
                    onClick = { onSelectCellularMode(com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Nút Chẩn đoán 4 lớp "🩺 Vì sao tôi bị lag?" (đã bỏ nút "Đồng bộ lại khung hình" theo mục 5)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showLagDiagnostic = !showLagDiagnostic }
                    .testTag("hud_why_lag_button"),
                shape = RoundedCornerShape(6.dp),
                color = if (showLagDiagnostic) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF182A20),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (showLagDiagnostic) XboxNeonGreen else XboxCardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "🩺 Why am I lagging? (4-Layer Check)" else "🩺 Vì sao tôi bị lag? (Chẩn đoán 4 lớp)",
                        color = if (showLagDiagnostic) XboxNeonGreen else DirectCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Bảng Chẩn đoán nhanh 4 lớp "Vì sao tôi bị lag?" (Mục 15 báo cáo kỹ thuật)
            if (showLagDiagnostic) {
                val powerManager = remember(context) {
                    context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                }
                val isPowerSave = remember(showLagDiagnostic, telemetry.rttMs) {
                    runCatching { powerManager?.isPowerSaveMode == true }.getOrDefault(false)
                }
                val currentHour = remember(showLagDiagnostic, telemetry.rttMs) {
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                }
                val isPeakHour = currentHour in 20..23

                // Lớp 1: Môi trường thiết bị (Tiết kiệm pin / Giải mã phần cứng)
                val layer1Bad = isPowerSave || (telemetry.hasStatsSample && telemetry.decodeTimeMs > 16.0)
                val layer1Text = when {
                    isPowerSave -> if (isEn) "1. Device: Battery Saver ON (throttles decoder)" else "1. Thiết bị: Đang bật Tiết kiệm pin (bóp xung giải mã)"
                    !telemetry.hasStatsSample || telemetry.decodeTimeMs <= 0.0 -> if (isEn) "1. Device: 60Hz VSync locked • Decode —" else "1. Thiết bị: Đã khóa 60Hz • Giải mã —"
                    telemetry.decodeTimeMs > 16.0 -> if (isEn) "1. Device: Slow decode (${String.format("%.1f", telemetry.decodeTimeMs)}ms > 16.6ms)" else "1. Thiết bị: Giải mã quá tải (${String.format("%.1f", telemetry.decodeTimeMs)}ms > 16.6ms)"
                    else -> if (isEn) "1. Device: 60Hz VSync locked • Decode OK (${String.format("%.1f", telemetry.decodeTimeMs)}ms)" else "1. Thiết bị: Đã khóa 60Hz • Giải mã tốt (${String.format("%.1f", telemetry.decodeTimeMs)}ms)"
                }

                // Lớp 2: Kết nối cục bộ (Wi-Fi RSSI / 4G Jitter)
                val layer2Bad = networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal || telemetry.jitterMs > 20
                val layer2Text = when {
                    networkStatus.isWifi -> {
                        val band = if (networkStatus.physicalType == com.example.cloudplay.PhysicalNetworkType.WIFI_2_4GHZ) "2.4GHz" else "5GHz"
                        if (networkStatus.isWeakWifiSignal) {
                            if (isEn) "2. Local Net: Weak Wi-Fi $band (${networkStatus.wifiRssiDbm}dBm)" else "2. Mạng cục bộ: Sóng Wi-Fi $band yếu (${networkStatus.wifiRssiDbm}dBm)"
                        } else {
                            if (isEn) "2. Local Net: Wi-Fi $band Low-Latency OK (${networkStatus.wifiRssiDbm}dBm)" else "2. Mạng cục bộ: Wi-Fi $band ổn định (${networkStatus.wifiRssiDbm}dBm)"
                        }
                    }
                    networkStatus.isCellular -> {
                        if (telemetry.jitterMs > 18 || networkStatus.isWeakCellularSignal) {
                            if (isEn) "2. Local Net: 4G queue fluctuation (Jitter ${telemetry.jitterMs}ms)" else "2. Mạng cục bộ: Sóng 4G dao động (Biến động ${telemetry.jitterMs}ms)"
                        } else {
                            if (isEn) "2. Local Net: 4G Direct active (Jitter ${telemetry.jitterMs}ms)" else "2. Mạng cục bộ: 4G ổn định (Biến động ${telemetry.jitterMs}ms)"
                        }
                    }
                    else -> if (isEn) "2. Local Net: Active" else "2. Mạng cục bộ: Đang hoạt động"
                }

                // Lớp 3: Tuyến đường quốc tế tới máy chủ luồng
                val layer3Bad = telemetry.rttMs > 150
                val layer3Text = when {
                    telemetry.rttMs > 160 && !layer2Bad -> if (isEn) {
                        "3. Intl Route: Congested (${telemetry.rttMs}ms)${if (isPeakHour) " • Peak hours 20h-23h" else ""}"
                    } else {
                        "3. Tuyến quốc tế: Nghẽn (${telemetry.rttMs}ms)${if (isPeakHour) " • Giờ cao điểm 20h-23h" else ""}"
                    }
                    telemetry.rttMs > 0 -> if (isEn) {
                        "3. Intl Route: Normal (${telemetry.rttMs}ms)"
                    } else {
                        "3. Tuyến quốc tế: Bình thường (${telemetry.rttMs}ms)"
                    }
                    else -> if (isEn) "3. Intl Route: Measuring WebRTC RTT..." else "3. Tuyến quốc tế: Đang đo độ trễ WebRTC..."
                }

                // Lớp 4: Trạng thái giao diện VPN (tun0 / TRANSPORT_VPN)
                val layer4Bad = isVpnActive
                val layer4Text = if (layer4Bad) {
                    if (isEn) "4. VPN: Active (Adds detour routing latency — turn off after game loads!)" else "4. VPN: Đang bật (Tăng độ trễ đường vòng — hãy tắt khi đã vào game!)"
                } else {
                    if (isEn) "4. VPN: Off (Direct routing)" else "4. VPN: Đã tắt (Truyền trực tiếp không đi vòng)"
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hud_why_lag_panel"),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0B1410),
                    border = androidx.compose.foundation.BorderStroke(0.7.dp, DirectCyan.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEn) "4-LAYER LATENCY DIAGNOSTICS" else "CHẨN ĐOÁN 4 LỚP: VÌ SAO TÔI BỊ LAG?",
                            color = DirectCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = layer1Text,
                            color = if (layer1Bad) JapanRed else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = layer2Text,
                            color = if (layer2Bad) Color(0xFFFFB300) else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = layer3Text,
                            color = if (layer3Bad) Color(0xFFFFB300) else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = layer4Text,
                            color = if (layer4Bad) JapanRed else XboxNeonGreen,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            if (networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal) {
                Text(
                    text = if (networkStatus.isWeakWifiSignal) {
                        if (isEn) "⚠️ Weak Wi-Fi: Adjusting receiver buffer (${liveBufMs}ms); SDP cap (${String.format("%.1f", liveMbps)}M) applies next session"
                        else "⚠️ Sóng Wi-Fi yếu: Tự chỉnh bộ đệm nhận (${liveBufMs}ms); trần SDP (${String.format("%.1f", liveMbps)}M) áp dụng phiên sau"
                    } else {
                        if (isEn) "⚠️ Weak 4G: Adjusting receiver buffer (${liveBufMs}ms); SDP cap (${String.format("%.1f", liveMbps)}M) applies next session"
                        else "⚠️ Sóng 4G yếu: Tự chỉnh bộ đệm nhận (${liveBufMs}ms); trần SDP (${String.format("%.1f", liveMbps)}M) áp dụng phiên sau"
                    },
                    color = Color(0xFFF59E0B),
                    fontSize = 10.5.sp,
                    maxLines = 2
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
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF15251D),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(0.9.dp, accentColor) else null
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) accentColor else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Nhóm 2: Điều khiển & Tiện ích nhanh (Làm nét, Giảm nền, Máy yếu, Dịch VI) với cỡ chữ >= 11sp.
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
    onToggleGpuLoadReduction: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101C16),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, XboxCardBorder.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = if (isEn) "Controls & Quick Toggles" else "Điều khiển & Tối ưu nhanh",
                color = DirectCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQuickTogglePill(
                    label = if (clarityBoostActive) {
                        if (isEn) "Sharpen: ON" else "Làm nét: Bật"
                    } else {
                        if (isEn) "Sharpen: OFF" else "Làm nét: Tắt"
                    },
                    active = clarityBoostActive,
                    activeColor = XboxNeonGreen,
                    onClick = { onToggleClarityBoost(!clarityBoostActive) },
                    modifier = Modifier.weight(1f)
                )
                DenseQuickTogglePill(
                    label = if (config.gpuLoadReductionEnabled) {
                        if (isEn) "Light BG: ON" else "Giảm nền: Bật"
                    } else {
                        if (isEn) "Light BG: OFF" else "Giảm nền: Tắt"
                    },
                    active = config.gpuLoadReductionEnabled,
                    activeColor = DirectCyan,
                    onClick = onToggleGpuLoadReduction,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DenseQuickTogglePill(
                    label = if (config.lowEndDeviceMode) {
                        if (isEn) "Low-End: ON" else "Máy yếu: Bật"
                    } else {
                        if (isEn) "Low-End: OFF" else "Máy yếu: Tắt"
                    },
                    active = config.lowEndDeviceMode,
                    activeColor = DirectCyan,
                    onClick = { com.example.data.SettingsRepository.updateLowEndDeviceMode(!config.lowEndDeviceMode) },
                    modifier = Modifier.weight(1f)
                )
                DenseQuickTogglePill(
                    label = if (translationActive) {
                        if (isEn) {
                            if (translatedCount > 0) "VI ($translatedCount)" else "VI Trans: ON"
                        } else {
                            if (translatedCount > 0) "Dịch VI ($translatedCount)" else "Dịch VI: Bật"
                        }
                    } else {
                        if (isEn) "VI Trans: OFF" else "Dịch VI: Tắt"
                    },
                    active = translationActive,
                    activeColor = XboxNeonGreen,
                    onClick = { onToggleTranslation(!translationActive) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (active) activeColor else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Hàng nút tác vụ dưới cùng: Tải lại, Xoay, Đồ họa, Thoát (xếp 1 hàng ngang, biểu tượng ở trên và nhãn >= 11sp ở dưới).
 */
@Composable
private fun DenseBottomActionsSection(
    onReloadPage: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestExitHome: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DenseBottomActionChip(
            icon = Icons.Default.Refresh,
            label = if (isEn) "Reload" else "Tải lại",
            tint = XboxNeonGreen,
            onClick = onReloadPage,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_reload_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.ScreenRotation,
            label = if (isEn) "Rotate" else "Xoay",
            tint = DirectCyan,
            onClick = onToggleOrientation,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_rotate_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.Settings,
            label = if (isEn) "Graphics" else "Đồ họa",
            tint = DirectCyan,
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f),
            testTag = "hud_expanded_settings_button"
        )
        DenseBottomActionChip(
            icon = Icons.Default.Home,
            label = if (isEn) "Exit" else "Thoát",
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
            .clip(RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(7.dp),
        color = Color(0xFF101915),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, tint.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

