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
                    var showMoreHudOptions by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(hudMode) {
                        if (hudMode != HudMode.EXPANDED) {
                            showMoreHudOptions = false
                        }
                    }

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
                                // 1. BẢNG THÔNG SỐ CỐT LÕI (Ping, FPS, Giải mã, Tay cầm, Nhiệt độ & Pin)
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
                                // 2. NÚT QUAY MÀN HÌNH (.MP4) — Luôn hiện ở chế độ thu gọn mặc định
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
                                // 3. NÚT MỞ RỘNG CÁC BẢNG TÙY CHỈNH PHỤ (Mặc định thu gọn)
                                // ========================================================
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            markUserInteraction()
                                            showMoreHudOptions = !showMoreHudOptions
                                        }
                                        .testTag("hud_more_options_toggle_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (showMoreHudOptions) DirectCyan.copy(alpha = 0.16f) else Color(0xFF101C16),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.8.dp,
                                        if (showMoreHudOptions) DirectCyan else XboxCardBorder.copy(alpha = 0.65f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (showMoreHudOptions) {
                                                if (isEn) "Hide Resolution, Gamepad & Color Options" else "Thu gọn Độ phân giải, Tay cầm & Màu sắc"
                                            } else {
                                                if (isEn) "Expand (Resolution, Quick Toggles, Gamepad, Colors)" else "Mở rộng (Độ phân giải, Công tắc, Tay cầm, Màu sắc)"
                                            },
                                            color = if (showMoreHudOptions) DirectCyan else XboxNeonGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (showMoreHudOptions) "▲" else "▼",
                                            color = if (showMoreHudOptions) DirectCyan else XboxNeonGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                // CÁC BẢNG PHỤ NẰM SAU NÚT "MỞ RỘNG":
                                // Độ phân giải, Công tắc nhanh, Chỉnh tay cầm ảo, Chỉnh màu
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
                                }

                                // ========================================================
                                // 4. HÀNG NÚT TÁC VỤ DƯỚI CÙNG (Tải lại, Xoay, Đồ họa, Thoát)
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


