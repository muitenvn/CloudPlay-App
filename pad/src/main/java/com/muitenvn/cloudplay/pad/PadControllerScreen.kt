package com.muitenvn.cloudplay.pad

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muitenvn.cloudplay.protocol.RemotePadButtons
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Bảng màu Ravno Pad mới (phẳng, tối xanh lục hiện đại, độ tương phản cao)
private val OfficialHudSurface = Color(0xFF0D1511)
private val OfficialHudSurface2 = Color(0xFF142019)
private val OfficialHudSelBg = Color(0xFF16352A)
private val OfficialHudBorder = Color(0xFF284235)
private val OfficialHudAccent = Color(0xFF27D884)
private val OfficialHudCyan = Color(0xFF3FD8EE)
private val OfficialPadBtnText = Color(0xFFEEF6F1)
private val OfficialPadAColor = Color(0xFF27D884)
private val OfficialPadBColor = Color(0xFFF0706B)
private val OfficialPadXColor = Color(0xFF3FD8EE)
private val OfficialPadYColor = Color(0xFFE3B341)

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
internal fun PadLandscapeControllerScreen(
    state: PadClientUiState,
    onDisconnect: () -> Unit,
    onForgetDevice: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lang = state.language

    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences("cloudplay_pad_layout_prefs", Context.MODE_PRIVATE)
    }

    var globalSettings by remember {
        mutableStateOf(PadControllerStorage.loadGlobalSettings(prefs))
    }

    // Đồng bộ cường độ rung từ trò chơi với PadNetworkClient
    LaunchedEffect(globalSettings.gameRumbleLevel) {
        PadNetworkClient.gameRumbleScale = globalSettings.gameRumbleLevel.scale
    }

    fun updateGlobalSettings(next: PadGlobalControllerSettings) {
        globalSettings = next
        PadControllerStorage.saveGlobalSettings(prefs, next)
    }

    var currentPreset by remember {
        val savedId = prefs.getString("active_preset_id", PadLayoutPreset.STANDARD.id)
        mutableStateOf(PadLayoutPreset.entries.firstOrNull { it.id == savedId } ?: PadLayoutPreset.STANDARD)
    }
    LaunchedEffect(state.suggestedPresetId) {
        val suggested = state.suggestedPresetId.trim().lowercase()
        if (suggested.isNotBlank()) {
            val matched = PadLayoutPreset.entries.firstOrNull { it.id == suggested }
            if (matched != null && matched != currentPreset) {
                currentPreset = matched
                prefs.edit().putString("active_preset_id", matched.id).apply()
            }
        }
    }

    var isCustomizeMode by remember { mutableStateOf(false) }
    var showGlobalSettingsSheet by remember { mutableStateOf(false) }
    var showManageButtonsModal by remember { mutableStateOf(false) }
    var selectedNodeKey by remember { mutableStateOf("abxy") }
    var activePopoverNodeKey by remember { mutableStateOf<String?>(null) }

    var rootSizePx by remember { mutableStateOf(IntSize.Zero) }
    var rootPosInWindow by remember { mutableStateOf(Offset.Zero) }
    val nodeBoundsMap = remember { mutableStateMapOf<String, ComposeRect>() }

    val activeGameKey = remember(state.activeGameTitle) {
        state.activeGameTitle.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
    }
    val storageKey = remember(currentPreset, activeGameKey, globalSettings.layoutScope) {
        if (globalSettings.layoutScope == PadLayoutScope.PER_GAME &&
            activeGameKey.isNotEmpty() &&
            activeGameKey != "generic"
        ) {
            "${currentPreset.id}_$activeGameKey"
        } else {
            currentPreset.id
        }
    }

    val nodeTunings = remember(storageKey) {
        mutableStateMapOf<String, PadNodeTuning>().apply {
            val loaded = PadControllerStorage.loadPresetTunings(prefs, storageKey)
            if (loaded.isNotEmpty()) {
                putAll(loaded)
            } else {
                putAll(PadControllerStorage.loadPresetTunings(prefs, currentPreset.id))
            }
        }
    }

    val customButtons = remember(storageKey) {
        mutableStateListOf<PadCustomButtonSpec>().apply {
            val loaded = PadControllerStorage.loadCustomButtons(prefs, storageKey)
            if (loaded.isNotEmpty()) {
                addAll(loaded)
            } else {
                addAll(PadControllerStorage.loadCustomButtons(prefs, currentPreset.id))
            }
        }
    }

    fun persistCurrentTunings() {
        PadControllerStorage.savePresetTunings(prefs, storageKey, nodeTunings)
    }

    fun persistCustomButtons() {
        PadControllerStorage.saveCustomButtons(prefs, storageKey, customButtons)
    }

    fun triggerLocalTouchTick(strong: Boolean = false, overrideLevel: Int = -1) {
        val effectiveLevel = if (overrideLevel in 0..3) {
            PadHapticLevel.fromId(overrideLevel)
        } else {
            globalSettings.buttonHapticLevel
        }
        if (effectiveLevel == PadHapticLevel.OFF) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            val durMs = when (effectiveLevel) {
                PadHapticLevel.LIGHT -> if (strong) 10L else 6L
                PadHapticLevel.MEDIUM -> if (strong) 16L else 10L
                PadHapticLevel.STRONG -> if (strong) 24L else 15L
                PadHapticLevel.OFF -> return
            }
            val amp = when (effectiveLevel) {
                PadHapticLevel.LIGHT -> if (strong) 110 else 65
                PadHapticLevel.MEDIUM -> if (strong) 175 else 115
                PadHapticLevel.STRONG -> if (strong) 255 else 185
                PadHapticLevel.OFF -> return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durMs, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durMs)
            }
        } catch (_: Exception) {
        }
    }

    // 1. Khóa màn hình ngang, giữ màn hình sáng, ẩn thanh điều hướng hệ thống
    DisposableEffect(context) {
        val activity = context.findActivity()
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else {
            null
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            PadNetworkClient.releaseAllInputsImmediate()
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { view.systemGestureExclusionRects = emptyList() }
            }
            activity?.requestedOrientation = prevOrientation
        }
    }

    // 2. Khi ứng dụng vào nền (ON_PAUSE / ON_STOP) -> lập tức nhả toàn bộ nút
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                PadNetworkClient.releaseAllInputsImmediate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 2B. Con quay hồi chuyển (Gyroscope) trên Ravno Pad - Mặc định TẮT hoàn toàn nếu người dùng không bật
    var gyroRecenterTick by remember { mutableIntStateOf(0) }
    val currentGyroSettings by rememberUpdatedState(globalSettings)
    val shouldRunPadGyro = globalSettings.gyroEnabled && !isCustomizeMode && !showGlobalSettingsSheet && !showManageButtonsModal && activePopoverNodeKey == null
    DisposableEffect(context, shouldRunPadGyro) {
        if (!shouldRunPadGyro) {
            PadNetworkClient.clearGyroOffset()
            onDispose { PadNetworkClient.clearGyroOffset() }
        } else {
            val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            val rotMatrix = FloatArray(9)
            val remapMatrix = FloatArray(9)
            val orientAngles = FloatArray(3)
            var hasRef = false
            var refYaw = 0f
            var refPitch = 0f
            var refRoll = 0f
            var lastRecenterSeen = gyroRecenterTick

            fun normAngle(a: Float): Float {
                var v = a
                val pi = Math.PI.toFloat()
                while (v > pi) v -= 2f * pi
                while (v < -pi) v += 2f * pi
                return v
            }

            fun shapeGyroAxis(raw: Float, sensPct: Int, dzPct: Int, invert: Boolean): Float {
                val dz = (dzPct.coerceIn(0, 20) / 100f)
                val absV = abs(raw)
                if (absV <= dz) return 0f
                val rescaled = ((absV - dz) / (1f - dz).coerceAtLeast(0.1f)) * (if (raw < 0f) -1f else 1f)
                val scaled = (rescaled * (sensPct.coerceIn(10, 300) / 100f)).coerceIn(-1f, 1f)
                return if (invert) -scaled else scaled
            }

            val listener = object : android.hardware.SensorEventListener {
                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit
                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    val gs = currentGyroSettings
                    if (!gs.gyroEnabled || event == null) return
                    if (gyroRecenterTick != lastRecenterSeen) {
                        lastRecenterSeen = gyroRecenterTick
                        hasRef = false
                    }
                    when (event.sensor.type) {
                        android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR,
                        android.hardware.Sensor.TYPE_ROTATION_VECTOR -> {
                            android.hardware.SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                            @Suppress("DEPRECATION")
                            val rot = wm?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_90
                            when (rot) {
                                android.view.Surface.ROTATION_90 -> android.hardware.SensorManager.remapCoordinateSystem(
                                    rotMatrix,
                                    android.hardware.SensorManager.AXIS_Y,
                                    android.hardware.SensorManager.AXIS_MINUS_X,
                                    remapMatrix
                                )
                                android.view.Surface.ROTATION_270 -> android.hardware.SensorManager.remapCoordinateSystem(
                                    rotMatrix,
                                    android.hardware.SensorManager.AXIS_MINUS_Y,
                                    android.hardware.SensorManager.AXIS_X,
                                    remapMatrix
                                )
                                else -> System.arraycopy(rotMatrix, 0, remapMatrix, 0, 9)
                            }
                            android.hardware.SensorManager.getOrientation(remapMatrix, orientAngles)
                            val yaw = orientAngles[0]
                            val pitch = orientAngles[1]
                            val roll = orientAngles[2]
                            if (!hasRef) {
                                refYaw = yaw
                                refPitch = pitch
                                refRoll = roll
                                hasRef = true
                            }
                            val dYaw = normAngle(yaw - refYaw)
                            val dPitch = normAngle(pitch - refPitch)
                            val dRoll = normAngle(roll - refRoll)
                            val fullRad = 0.56f
                            val rawH = when (gs.gyroMode) {
                                PadGyroMode.RACING_STEER -> dRoll / fullRad
                                PadGyroMode.FPS_VR_360,
                                PadGyroMode.LEFT_STICK_360 -> (dYaw * 0.85f + dRoll * 0.65f) / fullRad
                            }
                            val rawV = if (gs.gyroMode == PadGyroMode.RACING_STEER) 0f else (dPitch / fullRad)
                            val outX = shapeGyroAxis(rawH, gs.gyroSensX, gs.gyroDeadzone, gs.gyroInvertX)
                            val outY = if (gs.gyroMode == PadGyroMode.RACING_STEER) 0f else shapeGyroAxis(rawV, gs.gyroSensY, gs.gyroDeadzone, gs.gyroInvertY)
                            PadNetworkClient.updateGyroOffset(
                                rightStick = (gs.gyroMode == PadGyroMode.FPS_VR_360),
                                gx = outX,
                                gy = outY,
                                holdLtOnly = gs.gyroHoldLtOnly
                            )
                        }
                        android.hardware.Sensor.TYPE_ACCELEROMETER,
                        android.hardware.Sensor.TYPE_GRAVITY -> {
                            val gx = event.values[0]
                            val gy = event.values[1]
                            val gz = event.values[2]
                            val norm = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1f)
                            val steerAngle = atan2(gy / norm, gx / norm)
                            val pitchAngle = atan2(-gz / norm, gx / norm)
                            if (!hasRef) {
                                refRoll = steerAngle
                                refPitch = pitchAngle
                                hasRef = true
                            }
                            val dRoll = normAngle(steerAngle - refRoll) / 0.56f
                            val dPitch = normAngle(pitchAngle - refPitch) / 0.56f
                            val outX = shapeGyroAxis(dRoll, gs.gyroSensX, gs.gyroDeadzone, gs.gyroInvertX)
                            val outY = if (gs.gyroMode == PadGyroMode.RACING_STEER) 0f else shapeGyroAxis(dPitch, gs.gyroSensY, gs.gyroDeadzone, gs.gyroInvertY)
                            PadNetworkClient.updateGyroOffset(
                                rightStick = (gs.gyroMode == PadGyroMode.FPS_VR_360),
                                gx = outX,
                                gy = outY,
                                holdLtOnly = gs.gyroHoldLtOnly
                            )
                        }
                    }
                }
            }

            val rotSensor = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR)
                ?: sm?.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
                ?: sm?.getDefaultSensor(android.hardware.Sensor.TYPE_GRAVITY)
                ?: sm?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            if (rotSensor != null) {
                sm?.registerListener(listener, rotSensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)
            }
            onDispose {
                runCatching { sm?.unregisterListener(listener) }
                PadNetworkClient.clearGyroOffset()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PadBg)
            .onGloballyPositioned { coords ->
                rootSizePx = coords.size
                rootPosInWindow = coords.positionInWindow()
            }
            .onSizeChanged { sz ->
                rootSizePx = sz
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sz.width > 0 && sz.height > 0) {
                    val edgeW = (sz.width * 0.08f).roundToInt().coerceAtLeast(1)
                    runCatching {
                        view.systemGestureExclusionRects = listOf(
                            Rect(0, 0, edgeW, sz.height),
                            Rect(sz.width - edgeW, 0, sz.width, sz.height)
                        )
                    }
                }
            }
            .testTag("pad_landscape_controller_screen")
    ) {
        val density = LocalDensity.current
        val w = maxWidth
        val h = maxHeight
        val unit = minOf(w, h) * 0.01f

        val outerPadH = w * 0.022f
        val outerPadV = h * 0.022f
        val pillCorner = unit * 2.4f
        val strokeThin = unit * 0.26f
        val strokeActive = unit * 0.42f
        val fontBtn: TextUnit = with(density) { (unit * 3.5f).toSp() }
        val fontSmall: TextUnit = with(density) { (unit * 2.55f).toSp() }
        val fontTiny: TextUnit = with(density) { (unit * 2.10f).toSp() }

        val gScale = globalSettings.globalScale.coerceIn(0.60f, 1.65f)
        val gOpacity = globalSettings.globalOpacity.coerceIn(0.05f, 1.0f)

        fun tuningFor(nodeKey: String): PadNodeTuning = nodeTunings[nodeKey] ?: PadNodeTuning()

        fun effectiveOpacity(nodeTuning: PadNodeTuning): Float =
            (nodeTuning.opacity * gOpacity).coerceIn(0.05f, 1.0f)

        fun shouldRenderNode(nodeKey: String): Boolean {
            val t = tuningFor(nodeKey)
            return !t.hidden || isCustomizeMode
        }

        fun Modifier.trackNodeBounds(key: String): Modifier = this.onGloballyPositioned { coords ->
            val p = coords.positionInWindow() - rootPosInWindow
            val s = coords.size
            nodeBoundsMap[key] = ComposeRect(
                left = p.x,
                top = p.y,
                right = p.x + s.width,
                bottom = p.y + s.height
            )
        }

        fun Modifier.draggableOfficialNode(nodeKey: String): Modifier {
            val t = tuningFor(nodeKey)
            val dxPx = with(density) { (w * t.dxNorm).roundToPx() }
            val dyPx = with(density) { (h * t.dyNorm).roundToPx() }
            val baseMod = this
                .offset { IntOffset(dxPx, dyPx) }
                .trackNodeBounds(nodeKey)
            if (!isCustomizeMode) return baseMod
            val isSelected = selectedNodeKey == nodeKey || activePopoverNodeKey == nodeKey
            val outlineMod = if (isSelected) {
                baseMod
                    .border(strokeActive, OfficialHudAccent, RoundedCornerShape(pillCorner))
                    .background(OfficialHudAccent.copy(alpha = 0.14f), RoundedCornerShape(pillCorner))
            } else {
                baseMod.drawBehind {
                    val strokePx = strokeThin.toPx()
                    val dashPx = strokePx * 4.5f
                    val gapPx = strokePx * 3.0f
                    val radiusPx = pillCorner.toPx()
                    drawRoundRect(
                        color = if (t.hidden) PadDanger.copy(alpha = 0.78f) else OfficialHudAccent.copy(alpha = 0.78f),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(
                            width = strokePx,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
                        )
                    )
                }
            }
            return outlineMod
                .pointerInput(nodeKey, isCustomizeMode) {
                    detectTapGestures(
                        onTap = {
                            triggerLocalTouchTick(false)
                            selectedNodeKey = nodeKey
                        },
                        onDoubleTap = {
                            triggerLocalTouchTick(true)
                            selectedNodeKey = nodeKey
                            activePopoverNodeKey = nodeKey
                            showManageButtonsModal = false
                        }
                    )
                }
                .then(
                    if (globalSettings.isPositionLocked) {
                        Modifier
                    } else {
                        Modifier.pointerInput(nodeKey, isCustomizeMode, globalSettings.isPositionLocked) {
                            detectDragGestures(
                                onDragStart = { selectedNodeKey = nodeKey },
                                onDragEnd = { persistCurrentTunings() },
                                onDragCancel = { persistCurrentTunings() }
                            ) { change, dragAmount ->
                                change.consume()
                                selectedNodeKey = nodeKey
                                val curT = nodeTunings[nodeKey] ?: PadNodeTuning()
                                val rw = rootSizePx.width.coerceAtLeast(1).toFloat()
                                val rh = rootSizePx.height.coerceAtLeast(1).toFloat()
                                val nextDx = (curT.dxNorm + dragAmount.x / rw)
                                    .coerceIn(-0.45f, 0.45f)
                                val nextDy = (curT.dyNorm + dragAmount.y / rh)
                                    .coerceIn(-0.45f, 0.45f)
                                nodeTunings[nodeKey] = curT.copy(dxNorm = nextDx, dyNorm = nextDy)
                            }
                        }
                    }
                )
        }

        // Lưới căn chỉnh & Tâm ngắm khi bật chế độ Sửa nút
        if (isCustomizeMode && globalSettings.showEditGrid) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("virtual_gamepad_alignment_grid")
            ) {
                drawRect(color = Color(0x40070B09))
                val stepPx = (size.minDimension * 0.08f).coerceAtLeast(18f)
                val gridColor = Color.White.copy(alpha = 0.07f)
                val centerColor = OfficialHudAccent.copy(alpha = 0.35f)

                var x = stepPx
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += stepPx
                }
                var y = stepPx
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += stepPx
                }

                val midX = size.width / 2f
                val midY = size.height / 2f
                drawLine(centerColor, Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 1.5f)
                drawLine(centerColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.5f)
            }
        }

        // === 1. HÀNG NÚT VAI TRÊN CÙNG (Trái: LT, LB, L3 | Phải: R3, RB, RT) ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = outerPadH, vertical = outerPadV)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cụm vai trái: LT, LB (vuốt 4 hướng), L3
            Row(
                horizontalArrangement = Arrangement.spacedBy(unit * 1.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (shouldRenderNode("btn_lt")) {
                    val ltTuning = tuningFor("btn_lt")
                    val ltScale = currentPreset.shoulderBonusScale * ltTuning.scaleMult * gScale
                    val ltAnalogDrag = when (ltTuning.analogTriggerDrag) {
                        1 -> true
                        0 -> false
                        else -> currentPreset == PadLayoutPreset.RACING
                    }
                    Box(modifier = Modifier.draggableOfficialNode("btn_lt")) {
                        OfficialGamepadTriggerButton(
                            label = "LT",
                            width = w * 0.086f * ltScale * ltTuning.widthMult,
                            height = h * 0.115f * ltScale * ltTuning.heightMult,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            opacity = effectiveOpacity(ltTuning),
                            pressMode = ltTuning.pressMode,
                            turboHz = ltTuning.turboHz,
                            comboMask = ltTuning.comboMask,
                            analogDragMode = ltAnalogDrag,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_lt" },
                            onTriggerValueChanged = { v, justPressed ->
                                if (justPressed) triggerLocalTouchTick(strong = true, overrideLevel = ltTuning.hapticOverride)
                                PadNetworkClient.updateLeftTrigger(v)
                            },
                            modifier = Modifier.testTag("pad_btn_lt")
                        )
                    }
                }

                if (shouldRenderNode("btn_lb")) {
                    val lbTuning = tuningFor("btn_lb")
                    val lbScale = currentPreset.shoulderBonusScale * lbTuning.scaleMult * gScale
                    val defaultLbSwipe = lbTuning.swipeBit.takeIf { it != 0 } ?: RemotePadButtons.L3
                    Box(modifier = Modifier.draggableOfficialNode("btn_lb")) {
                        OfficialGamepadShoulderButton(
                            label = "LB",
                            primaryBit = RemotePadButtons.LB,
                            comboMask = lbTuning.comboMask,
                            directionalSwipeEnabled = lbTuning.directionalSwipeEnabled,
                            swipeUpBit = lbTuning.swipeUpBit,
                            swipeDownBit = lbTuning.swipeDownBit.takeIf { it != 0 } ?: defaultLbSwipe,
                            swipeLeftBit = lbTuning.swipeLeftBit,
                            swipeRightBit = lbTuning.swipeRightBit,
                            swipeIncludeShoulder = lbTuning.swipeIncludeShoulder,
                            width = w * 0.080f * lbScale * lbTuning.widthMult,
                            height = h * 0.115f * lbScale * lbTuning.heightMult,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            badgeFontSize = fontTiny,
                            opacity = effectiveOpacity(lbTuning),
                            pressMode = lbTuning.pressMode,
                            turboHz = lbTuning.turboHz,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_lb" },
                            onHaptic = { triggerLocalTouchTick(it, lbTuning.hapticOverride) },
                            modifier = Modifier.testTag("pad_btn_lb")
                        )
                    }
                }

                if (shouldRenderNode("btn_l3")) {
                    val l3Tuning = tuningFor("btn_l3")
                    val l3Scale = l3Tuning.scaleMult * gScale
                    Box(modifier = Modifier.draggableOfficialNode("btn_l3")) {
                        OfficialGamepadPillButton(
                            label = "L3",
                            bit = RemotePadButtons.L3,
                            comboMask = l3Tuning.comboMask,
                            width = w * 0.060f * l3Scale * l3Tuning.widthMult,
                            height = h * 0.098f * l3Scale * l3Tuning.heightMult,
                            corner = pillCorner * 0.85f,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            opacity = effectiveOpacity(l3Tuning),
                            pressMode = l3Tuning.pressMode,
                            turboHz = l3Tuning.turboHz,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_l3" },
                            onHaptic = { triggerLocalTouchTick(true, l3Tuning.hapticOverride) },
                            testTag = "pad_btn_l3"
                        )
                    }
                }
            }

            // Cụm vai phải: R3, RB (vuốt 4 hướng), RT
            Row(
                horizontalArrangement = Arrangement.spacedBy(unit * 1.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (shouldRenderNode("btn_r3")) {
                    val r3Tuning = tuningFor("btn_r3")
                    val r3Scale = r3Tuning.scaleMult * gScale
                    Box(modifier = Modifier.draggableOfficialNode("btn_r3")) {
                        OfficialGamepadPillButton(
                            label = "R3",
                            bit = RemotePadButtons.R3,
                            comboMask = r3Tuning.comboMask,
                            width = w * 0.060f * r3Scale * r3Tuning.widthMult,
                            height = h * 0.098f * r3Scale * r3Tuning.heightMult,
                            corner = pillCorner * 0.85f,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            opacity = effectiveOpacity(r3Tuning),
                            pressMode = r3Tuning.pressMode,
                            turboHz = r3Tuning.turboHz,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_r3" },
                            onHaptic = { triggerLocalTouchTick(true, r3Tuning.hapticOverride) },
                            testTag = "pad_btn_r3"
                        )
                    }
                }

                if (shouldRenderNode("btn_rb")) {
                    val rbTuning = tuningFor("btn_rb")
                    val rbScale = currentPreset.shoulderBonusScale * rbTuning.scaleMult * gScale
                    val defaultRbSwipe = rbTuning.swipeBit.takeIf { it != 0 } ?: RemotePadButtons.R3
                    Box(modifier = Modifier.draggableOfficialNode("btn_rb")) {
                        OfficialGamepadShoulderButton(
                            label = "RB",
                            primaryBit = RemotePadButtons.RB,
                            comboMask = rbTuning.comboMask,
                            directionalSwipeEnabled = rbTuning.directionalSwipeEnabled,
                            swipeUpBit = rbTuning.swipeUpBit,
                            swipeDownBit = rbTuning.swipeDownBit.takeIf { it != 0 } ?: defaultRbSwipe,
                            swipeLeftBit = rbTuning.swipeLeftBit,
                            swipeRightBit = rbTuning.swipeRightBit,
                            swipeIncludeShoulder = rbTuning.swipeIncludeShoulder,
                            width = w * 0.080f * rbScale * rbTuning.widthMult,
                            height = h * 0.115f * rbScale * rbTuning.heightMult,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            badgeFontSize = fontTiny,
                            opacity = effectiveOpacity(rbTuning),
                            pressMode = rbTuning.pressMode,
                            turboHz = rbTuning.turboHz,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_rb" },
                            onHaptic = { triggerLocalTouchTick(it, rbTuning.hapticOverride) },
                            modifier = Modifier.testTag("pad_btn_rb")
                        )
                    }
                }

                if (shouldRenderNode("btn_rt")) {
                    val rtTuning = tuningFor("btn_rt")
                    val rtScale = currentPreset.shoulderBonusScale * rtTuning.scaleMult * gScale
                    val rtAnalogDrag = when (rtTuning.analogTriggerDrag) {
                        1 -> true
                        0 -> false
                        else -> currentPreset == PadLayoutPreset.RACING
                    }
                    Box(modifier = Modifier.draggableOfficialNode("btn_rt")) {
                        OfficialGamepadTriggerButton(
                            label = "RT",
                            width = w * 0.086f * rtScale * rtTuning.widthMult,
                            height = h * 0.115f * rtScale * rtTuning.heightMult,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            opacity = effectiveOpacity(rtTuning),
                            pressMode = rtTuning.pressMode,
                            turboHz = rtTuning.turboHz,
                            comboMask = rtTuning.comboMask,
                            analogDragMode = rtAnalogDrag,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_rt" },
                            onTriggerValueChanged = { v, justPressed ->
                                if (justPressed) triggerLocalTouchTick(strong = true, overrideLevel = rtTuning.hapticOverride)
                                PadNetworkClient.updateRightTrigger(v)
                            },
                            modifier = Modifier.testTag("pad_btn_rt")
                        )
                    }
                }
            }
        }

        // === 2. CỤM TRUNG TÂM TRÊN CÙNG GỌN NHẸ (đồng bộ y hệt app chính): ⧉ View · ◉ Xbox Guide · ☰ Menu · ⚙ ===
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = outerPadV),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(unit * 0.85f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(unit * 1.3f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (shouldRenderNode("btn_view")) {
                    val viewTuning = tuningFor("btn_view")
                    val vScale = viewTuning.scaleMult * gScale
                    Box(modifier = Modifier.draggableOfficialNode("btn_view")) {
                        OfficialGamepadPillButton(
                            label = "⧉ View",
                            bit = RemotePadButtons.VIEW,
                            comboMask = viewTuning.comboMask,
                            width = w * 0.080f * vScale * viewTuning.widthMult,
                            height = h * 0.086f * vScale * viewTuning.heightMult,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            opacity = effectiveOpacity(viewTuning),
                            pressMode = viewTuning.pressMode,
                            turboHz = viewTuning.turboHz,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_view" },
                            onHaptic = { triggerLocalTouchTick(false, viewTuning.hapticOverride) },
                            testTag = "pad_btn_view"
                        )
                    }
                }

                if (shouldRenderNode("btn_xbox")) {
                    val xboxTuning = tuningFor("btn_xbox")
                    val xScale = xboxTuning.scaleMult * gScale
                    Box(modifier = Modifier.draggableOfficialNode("btn_xbox")) {
                        OfficialXboxNexusButton(
                            diameter = h * 0.095f * xScale,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            opacity = effectiveOpacity(xboxTuning),
                            pressMode = xboxTuning.pressMode,
                            turboHz = xboxTuning.turboHz,
                            comboMask = xboxTuning.comboMask,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_xbox" },
                            onHaptic = { triggerLocalTouchTick(true, xboxTuning.hapticOverride) },
                            testTag = "pad_btn_xbox"
                        )
                    }
                }

                if (shouldRenderNode("btn_menu")) {
                    val menuTuning = tuningFor("btn_menu")
                    val mScale = menuTuning.scaleMult * gScale
                    Box(modifier = Modifier.draggableOfficialNode("btn_menu")) {
                        OfficialGamepadPillButton(
                            label = "☰ Menu",
                            bit = RemotePadButtons.MENU,
                            comboMask = menuTuning.comboMask,
                            width = w * 0.080f * mScale * menuTuning.widthMult,
                            height = h * 0.086f * mScale * menuTuning.heightMult,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            opacity = effectiveOpacity(menuTuning),
                            pressMode = menuTuning.pressMode,
                            turboHz = menuTuning.turboHz,
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "btn_menu" },
                            onHaptic = { triggerLocalTouchTick(false, menuTuning.hapticOverride) },
                            testTag = "pad_btn_menu"
                        )
                    }
                }

                // Nút ⚙ Duy nhất (như app chính): mở popover gọn nhẹ (Mẫu bố cục, Cỡ chung, Độ mờ, Sửa vị trí, Thêm/Ẩn nút, Cài đặt nâng cao)
                val isTuneOpen = activePopoverNodeKey == "all" || isCustomizeMode
                Box(
                    modifier = Modifier
                        .trackNodeBounds("all")
                        .size(width = w * 0.048f, height = h * 0.086f)
                        .clip(RoundedCornerShape(pillCorner * 0.85f))
                        .background(if (isTuneOpen) OfficialHudSelBg else OfficialHudSurface2.copy(alpha = (gOpacity * 0.85f).coerceIn(0.18f, 0.94f)))
                        .border(
                            if (isTuneOpen) strokeActive else strokeThin,
                            if (isTuneOpen) OfficialHudAccent else OfficialHudBorder,
                            RoundedCornerShape(pillCorner * 0.85f)
                        )
                        .clickable {
                            PadNetworkClient.releaseAllInputsImmediate()
                            activePopoverNodeKey = if (activePopoverNodeKey == "all") null else "all"
                            triggerLocalTouchTick(false)
                        }
                        .testTag("pad_global_settings_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = PadStrings.tr(lang, "Tùy chỉnh tay cầm", "Customize controller"),
                        tint = if (isTuneOpen) OfficialHudAccent else OfficialPadBtnText,
                        modifier = Modifier.size(unit * 4.0f)
                    )
                }
            }

            // Viên thuốc trạng thái siêu gọn (chỉ hiện Tên TV + Ping ms thật khi không ở chế độ sửa, bấm vào cũng mở bảng ⚙)
            if (!isCustomizeMode) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(pillCorner))
                        .background(OfficialHudSurface.copy(alpha = 0.72f))
                        .border(strokeThin, OfficialHudBorder.copy(alpha = 0.55f), RoundedCornerShape(pillCorner))
                        .clickable {
                            PadNetworkClient.releaseAllInputsImmediate()
                            activePopoverNodeKey = if (activePopoverNodeKey == "all") null else "all"
                            triggerLocalTouchTick(false)
                        }
                        .padding(horizontal = unit * 1.4f, vertical = unit * 0.35f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(unit * 0.9f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(unit * 1.3f)
                            .clip(CircleShape)
                            .background(OfficialHudAccent)
                    )
                    val tvTitle = state.selectedTv?.tvName.orEmpty().ifBlank { "Ravno TV" }
                    Text(
                        text = tvTitle,
                        color = OfficialPadBtnText.copy(alpha = 0.85f),
                        fontSize = fontTiny,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pingStr = state.measuredPingMs?.let { "$it ms" } ?: "-- ms"
                    Text(
                        text = "· $pingStr",
                        color = OfficialHudAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontTiny,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("pad_live_ping_text")
                    )
                }
            }
        }

        // === 3. CỤM DƯỚI TRÁI: Cần Analog Trái (L) + D-Pad (chữ thập 8 hướng hoặc tách 4 nút độc lập) ===
        val effectiveAutoSprintL3 = when (globalSettings.autoSprintL3Override) {
            0 -> false
            1 -> true
            else -> currentPreset.autoSprintL3
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = outerPadH, bottom = outerPadV),
            horizontalArrangement = Arrangement.spacedBy(unit * 3.0f),
            verticalAlignment = Alignment.Bottom
        ) {
            if (shouldRenderNode("stick_left")) {
                val leftStickTuning = tuningFor("stick_left")
                val leftStickSize = minOf(w * 0.175f, h * 0.42f) *
                    currentPreset.stickBonusScale *
                    leftStickTuning.scaleMult *
                    gScale
                Box(modifier = Modifier.draggableOfficialNode("stick_left")) {
                    OfficialVirtualAnalogStick(
                        label = "L",
                        size = leftStickSize,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        tinyFontSize = fontTiny,
                        opacity = effectiveOpacity(leftStickTuning),
                        sensitivity = globalSettings.leftStickSens,
                        responseCurve = globalSettings.leftStickCurve,
                        innerDeadzone = globalSettings.innerDeadzone,
                        outerDeadzone = globalSettings.outerDeadzone,
                        autoSprintL3 = effectiveAutoSprintL3,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "stick_left" },
                        onStickChanged = { x, y, sprintBit ->
                            PadNetworkClient.updateLeftStick(x, y, sprintBit)
                        },
                        onDoubleTapStickClick = { pressed ->
                            if (pressed) triggerLocalTouchTick(true, leftStickTuning.hapticOverride)
                            PadNetworkClient.updateButtonBit(RemotePadButtons.L3, pressed)
                        },
                        modifier = Modifier.testTag("pad_left_stick")
                    )
                }
            }

            if (!globalSettings.dpadSplitMode) {
                if (shouldRenderNode("dpad")) {
                    val dpadTuning = tuningFor("dpad")
                    val dpadSize = minOf(w * 0.155f, h * 0.37f) * dpadTuning.scaleMult * gScale
                    Box(modifier = Modifier.draggableOfficialNode("dpad")) {
                        OfficialVirtualDpadCross(
                            size = dpadSize,
                            corner = unit * 2.6f,
                            keyCorner = unit * 1.6f,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            opacity = effectiveOpacity(dpadTuning),
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "dpad" },
                            onHaptic = { triggerLocalTouchTick(false, dpadTuning.hapticOverride) },
                            modifier = Modifier.testTag("pad_dpad")
                        )
                    }
                }
            } else {
                // Tách 4 nút D-Pad: mỗi phím hướng có thể kéo, chỉnh cỡ/mờ/Turbo/Combo riêng!
                val baseDpadSize = minOf(w * 0.155f, h * 0.37f) * gScale
                val baseKeySize = baseDpadSize * 0.35f
                Box(modifier = Modifier.size(baseDpadSize).testTag("pad_dpad")) {
                    if (shouldRenderNode("dpad_up")) {
                        val tUp = tuningFor("dpad_up")
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .draggableOfficialNode("dpad_up")
                        ) {
                            OfficialGamepadPillButton(
                                label = "▲",
                                bit = RemotePadButtons.DPAD_UP,
                                comboMask = tUp.comboMask,
                                width = baseKeySize * tUp.scaleMult * tUp.widthMult,
                                height = baseKeySize * tUp.scaleMult * tUp.heightMult,
                                corner = unit * 1.8f,
                                strokeThin = strokeThin,
                                strokeActive = strokeActive,
                                fontSize = fontSmall,
                                opacity = effectiveOpacity(tUp),
                                pressMode = tUp.pressMode,
                                turboHz = tUp.turboHz,
                                isEditMode = isCustomizeMode,
                                onSelectInEdit = { selectedNodeKey = "dpad_up" },
                                onHaptic = { triggerLocalTouchTick(false, tUp.hapticOverride) },
                                testTag = "pad_dpad_up"
                            )
                        }
                    }
                    if (shouldRenderNode("dpad_down")) {
                        val tDown = tuningFor("dpad_down")
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .draggableOfficialNode("dpad_down")
                        ) {
                            OfficialGamepadPillButton(
                                label = "▼",
                                bit = RemotePadButtons.DPAD_DOWN,
                                comboMask = tDown.comboMask,
                                width = baseKeySize * tDown.scaleMult * tDown.widthMult,
                                height = baseKeySize * tDown.scaleMult * tDown.heightMult,
                                corner = unit * 1.8f,
                                strokeThin = strokeThin,
                                strokeActive = strokeActive,
                                fontSize = fontSmall,
                                opacity = effectiveOpacity(tDown),
                                pressMode = tDown.pressMode,
                                turboHz = tDown.turboHz,
                                isEditMode = isCustomizeMode,
                                onSelectInEdit = { selectedNodeKey = "dpad_down" },
                                onHaptic = { triggerLocalTouchTick(false, tDown.hapticOverride) },
                                testTag = "pad_dpad_down"
                            )
                        }
                    }
                    if (shouldRenderNode("dpad_left")) {
                        val tLeft = tuningFor("dpad_left")
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .draggableOfficialNode("dpad_left")
                        ) {
                            OfficialGamepadPillButton(
                                label = "◀",
                                bit = RemotePadButtons.DPAD_LEFT,
                                comboMask = tLeft.comboMask,
                                width = baseKeySize * tLeft.scaleMult * tLeft.widthMult,
                                height = baseKeySize * tLeft.scaleMult * tLeft.heightMult,
                                corner = unit * 1.8f,
                                strokeThin = strokeThin,
                                strokeActive = strokeActive,
                                fontSize = fontSmall,
                                opacity = effectiveOpacity(tLeft),
                                pressMode = tLeft.pressMode,
                                turboHz = tLeft.turboHz,
                                isEditMode = isCustomizeMode,
                                onSelectInEdit = { selectedNodeKey = "dpad_left" },
                                onHaptic = { triggerLocalTouchTick(false, tLeft.hapticOverride) },
                                testTag = "pad_dpad_left"
                            )
                        }
                    }
                    if (shouldRenderNode("dpad_right")) {
                        val tRight = tuningFor("dpad_right")
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .draggableOfficialNode("dpad_right")
                        ) {
                            OfficialGamepadPillButton(
                                label = "▶",
                                bit = RemotePadButtons.DPAD_RIGHT,
                                comboMask = tRight.comboMask,
                                width = baseKeySize * tRight.scaleMult * tRight.widthMult,
                                height = baseKeySize * tRight.scaleMult * tRight.heightMult,
                                corner = unit * 1.8f,
                                strokeThin = strokeThin,
                                strokeActive = strokeActive,
                                fontSize = fontSmall,
                                opacity = effectiveOpacity(tRight),
                                pressMode = tRight.pressMode,
                                turboHz = tRight.turboHz,
                                isEditMode = isCustomizeMode,
                                onSelectInEdit = { selectedNodeKey = "dpad_right" },
                                onHaptic = { triggerLocalTouchTick(false, tRight.hapticOverride) },
                                testTag = "pad_dpad_right"
                            )
                        }
                    }
                }
            }
        }

        // === 4. CỤM DƯỚI PHẢI: Cần Analog Phải (R) + Cụm ABXY (trượt tổ hợp hoặc tách 4 nút độc lập) ===
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = outerPadH, bottom = outerPadV),
            horizontalArrangement = Arrangement.spacedBy(unit * 3.0f),
            verticalAlignment = Alignment.Bottom
        ) {
            if (shouldRenderNode("stick_right")) {
                val rightStickTuning = tuningFor("stick_right")
                val rightStickSize = minOf(w * 0.165f, h * 0.40f) *
                    currentPreset.rightStickBonusScale *
                    rightStickTuning.scaleMult *
                    gScale
                Box(modifier = Modifier.draggableOfficialNode("stick_right")) {
                    OfficialVirtualAnalogStick(
                        label = "R",
                        size = rightStickSize,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        tinyFontSize = fontTiny,
                        opacity = effectiveOpacity(rightStickTuning),
                        sensitivity = globalSettings.rightStickSens,
                        responseCurve = globalSettings.rightStickCurve,
                        innerDeadzone = globalSettings.rightInnerDeadzone,
                        outerDeadzone = globalSettings.rightOuterDeadzone,
                        autoSprintL3 = false,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "stick_right" },
                        onStickChanged = { x, y, _ ->
                            PadNetworkClient.updateRightStick(x, y)
                        },
                        onDoubleTapStickClick = { pressed ->
                            if (pressed) triggerLocalTouchTick(true, rightStickTuning.hapticOverride)
                            PadNetworkClient.updateButtonBit(RemotePadButtons.R3, pressed)
                        },
                        modifier = Modifier.testTag("pad_right_stick")
                    )
                }
            }

            if (!globalSettings.abxySplitMode) {
                if (shouldRenderNode("abxy")) {
                    val abxyTuning = tuningFor("abxy")
                    val abxySize = minOf(w * 0.185f, h * 0.44f) *
                        currentPreset.abxyBonusScale *
                        abxyTuning.scaleMult *
                        gScale
                    Box(modifier = Modifier.draggableOfficialNode("abxy")) {
                        OfficialVirtualAbxyPad(
                            size = abxySize,
                            swapSwitchAbxy = currentPreset.swapSwitchAbxy,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            opacity = effectiveOpacity(abxyTuning),
                            isEditMode = isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "abxy" },
                            onHaptic = { triggerLocalTouchTick(false, abxyTuning.hapticOverride) },
                            modifier = Modifier.testTag("pad_abxy_cluster")
                        )
                    }
                }
            } else {
                // Tách 4 nút A, B, X, Y độc lập
                val baseAbxySize = minOf(w * 0.185f, h * 0.44f) * currentPreset.abxyBonusScale * gScale
                val baseBtnDiam = baseAbxySize * 0.35f
                val swap = currentPreset.swapSwitchAbxy

                val topNode = if (swap) Triple("btn_x", "X", OfficialPadXColor to RemotePadButtons.X)
                else Triple("btn_y", "Y", OfficialPadYColor to RemotePadButtons.Y)
                val bottomNode = if (swap) Triple("btn_b", "B", OfficialPadBColor to RemotePadButtons.B)
                else Triple("btn_a", "A", OfficialPadAColor to RemotePadButtons.A)
                val leftNode = if (swap) Triple("btn_y", "Y", OfficialPadYColor to RemotePadButtons.Y)
                else Triple("btn_x", "X", OfficialPadXColor to RemotePadButtons.X)
                val rightNode = if (swap) Triple("btn_a", "A", OfficialPadAColor to RemotePadButtons.A)
                else Triple("btn_b", "B", OfficialPadBColor to RemotePadButtons.B)

                Box(modifier = Modifier.size(baseAbxySize).testTag("pad_abxy_cluster")) {
                    listOf(
                        Alignment.TopCenter to topNode,
                        Alignment.BottomCenter to bottomNode,
                        Alignment.CenterStart to leftNode,
                        Alignment.CenterEnd to rightNode
                    ).forEach { (align, info) ->
                        val (nodeKey, lbl, colorAndBit) = info
                        if (shouldRenderNode(nodeKey)) {
                            val tNode = tuningFor(nodeKey)
                            Box(
                                modifier = Modifier
                                    .align(align)
                                    .draggableOfficialNode(nodeKey)
                            ) {
                                OfficialStandaloneColoredAbxyButton(
                                    label = lbl,
                                    accentColor = colorAndBit.first,
                                    bit = colorAndBit.second,
                                    comboMask = tNode.comboMask,
                                    diameter = baseBtnDiam * tNode.scaleMult,
                                    strokeThin = strokeThin,
                                    strokeActive = strokeActive,
                                    fontSize = fontBtn,
                                    opacity = effectiveOpacity(tNode),
                                    pressMode = tNode.pressMode,
                                    turboHz = tNode.turboHz,
                                    isEditMode = isCustomizeMode,
                                    onSelectInEdit = { selectedNodeKey = nodeKey },
                                    onHaptic = { triggerLocalTouchTick(false, tNode.hapticOverride) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // === 5. CÁC NÚT TRỐNG TÙY CHỈNH DO NGƯỜI DÙNG TẠO (Custom Blank Buttons) ===
        customButtons.forEachIndexed { idx, spec ->
            if (shouldRenderNode(spec.id)) {
                val cTuning = nodeTunings[spec.id] ?: PadNodeTuning(
                    dxNorm = (-0.12f + (idx % 4) * 0.08f).coerceIn(-0.35f, 0.35f),
                    dyNorm = (-0.08f + (idx / 4) * 0.10f).coerceIn(-0.35f, 0.35f)
                )
                val cScale = cTuning.scaleMult * gScale
                val cWidth = when (spec.shape) {
                    PadCustomBtnShape.ROUND -> h * 0.11f * cScale
                    PadCustomBtnShape.PILL -> w * 0.082f * cScale * cTuning.widthMult
                    PadCustomBtnShape.CUSTOM_RECT -> w * 0.092f * cScale * cTuning.widthMult
                }
                val cHeight = when (spec.shape) {
                    PadCustomBtnShape.ROUND -> h * 0.11f * cScale
                    PadCustomBtnShape.PILL -> h * 0.095f * cScale * cTuning.heightMult
                    PadCustomBtnShape.CUSTOM_RECT -> h * 0.105f * cScale * cTuning.heightMult
                }
                val cCorner = when (spec.shape) {
                    PadCustomBtnShape.ROUND -> cHeight * 0.5f
                    PadCustomBtnShape.PILL -> pillCorner
                    PadCustomBtnShape.CUSTOM_RECT -> unit * 1.6f
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .draggableOfficialNode(spec.id)
                ) {
                    OfficialGamepadPillButton(
                        label = spec.label,
                        bit = spec.assignedMask,
                        comboMask = cTuning.comboMask,
                        width = cWidth,
                        height = cHeight,
                        corner = cCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = effectiveOpacity(cTuning),
                        pressMode = cTuning.pressMode,
                        turboHz = cTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = spec.id },
                        onHaptic = { triggerLocalTouchTick(false, cTuning.hapticOverride) },
                        testTag = "pad_custom_btn_${spec.id}"
                    )
                }
            }
        }

        // === 6. THANH CÔNG CỤ SỬA GỌN NHẸ & POPOVER NỔI CẠNH NÚT KHI CHẠM 2 LẦN (đồng bộ y hệt app chính) ===
        if (isCustomizeMode) {
            val hiddenCountInEdit = nodeTunings.values.count { it.hidden }
            PadCompactEditToolbar(
                lang = lang,
                hiddenCount = hiddenCountInEdit,
                showAddButtonModal = showManageButtonsModal,
                showEditGrid = globalSettings.showEditGrid,
                isPositionLocked = globalSettings.isPositionLocked,
                onToggleAddButtonModal = {
                    activePopoverNodeKey = null
                    showManageButtonsModal = !showManageButtonsModal
                },
                onToggleGrid = {
                    updateGlobalSettings(globalSettings.copy(showEditGrid = !globalSettings.showEditGrid))
                },
                onToggleLock = { nextLocked ->
                    updateGlobalSettings(globalSettings.copy(isPositionLocked = nextLocked))
                },
                onDone = {
                    persistCurrentTunings()
                    activePopoverNodeKey = null
                    isCustomizeMode = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = outerPadV + h * 0.095f)
            )

            if (activePopoverNodeKey == null && !showManageButtonsModal) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(OfficialHudSurface.copy(alpha = 0.88f))
                        .border(1.dp, OfficialHudBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = PadStrings.tr(
                            lang,
                            "Chạm 2 lần vào một nút để chỉnh riêng nút đó",
                            "Double-tap any button to customize it"
                        ),
                        color = OfficialPadBtnText.copy(alpha = 0.72f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // === 6B. POPOVER NỔI CẠNH NÚT (mở khi bấm ⚙ hoặc chạm 2 lần vào một nút bất kỳ) ===
        val popNodeKey = activePopoverNodeKey
        if (popNodeKey != null) {
            // Lớp nền trong suốt: chạm ra ngoài để đóng popover
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(popNodeKey) {
                        detectTapGestures(
                            onTap = {
                                persistCurrentTunings()
                                activePopoverNodeKey = null
                            }
                        )
                    }
            )

            val popWidthDp = 262.dp
            val popEstHeightDp = if (popNodeKey == "all") 335.dp else 315.dp
            val popWpx = with(density) { popWidthDp.toPx() }
            val popHpx = with(density) { popEstHeightDp.toPx() }
            val padPx = with(density) { 10.dp.toPx() }
            val gapPx = with(density) { 10.dp.toPx() }

            val anchorRect = nodeBoundsMap[popNodeKey] ?: nodeBoundsMap["all"]
            val rootW = rootSizePx.width.coerceAtLeast(1).toFloat()
            val rootH = rootSizePx.height.coerceAtLeast(1).toFloat()

            val offsetPx = if (anchorRect == null || popNodeKey == "all") {
                val ax = anchorRect?.center?.x ?: (rootW / 2f)
                val ay = anchorRect?.bottom ?: with(density) { 54.dp.toPx() }
                val x = (ax - popWpx / 2f).coerceIn(padPx, (rootW - popWpx - padPx).coerceAtLeast(padPx))
                val y = (ay + gapPx).coerceIn(padPx, (rootH - popHpx - padPx).coerceAtLeast(padPx))
                IntOffset(x.roundToInt(), y.roundToInt())
            } else {
                val placeRight = anchorRect.center.x < rootW * 0.5f
                val rawX = if (placeRight) {
                    anchorRect.right + gapPx
                } else {
                    anchorRect.left - popWpx - gapPx
                }
                val x = rawX.coerceIn(padPx, (rootW - popWpx - padPx).coerceAtLeast(padPx))
                val rawY = anchorRect.center.y - popHpx * 0.45f
                val y = rawY.coerceIn(padPx, (rootH - popHpx - padPx).coerceAtLeast(padPx))
                IntOffset(x.roundToInt(), y.roundToInt())
            }

            val curNodeTuning = tuningFor(popNodeKey)
            val curCustomSpec = customButtons.firstOrNull { it.id == popNodeKey }
            val hasActiveGame = activeGameKey.isNotEmpty() && activeGameKey != "generic"
            val hasPerGameCustom = hasActiveGame && globalSettings.layoutScope == PadLayoutScope.PER_GAME
            PadNodePopover(
                nodeKey = popNodeKey,
                lang = lang,
                tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "Ravno TV" },
                pingMs = state.measuredPingMs,
                activeGameTitle = state.activeGameTitle,
                hasActiveGameKey = hasActiveGame,
                hasPerGameCustomLayout = hasPerGameCustom,
                isEditPositionMode = isCustomizeMode,
                currentPreset = currentPreset,
                globalSettings = globalSettings,
                nodeTuning = curNodeTuning,
                customButtonSpec = curCustomSpec,
                screenW = rootW,
                screenH = rootH,
                onSelectPreset = { preset ->
                    currentPreset = preset
                    prefs.edit().putString("active_preset_id", preset.id).apply()
                    triggerLocalTouchTick()
                },
                onUpdateGlobalSettings = { next ->
                    updateGlobalSettings(next)
                },
                onUpdateNodeTuning = { nextTuning ->
                    nodeTunings[popNodeKey] = nextTuning
                    persistCurrentTunings()
                },
                onUpdateCustomButtonSpec = { updatedSpec ->
                    val idx = customButtons.indexOfFirst { it.id == updatedSpec.id }
                    if (idx >= 0) {
                        customButtons[idx] = updatedSpec
                        persistCustomButtons()
                    }
                },
                onNudgePx = { dxPxDelta, dyPxDelta ->
                    val cur = nodeTunings[popNodeKey] ?: PadNodeTuning()
                    val nextDx = (cur.dxNorm + dxPxDelta / rootW).coerceIn(-0.45f, 0.45f)
                    val nextDy = (cur.dyNorm + dyPxDelta / rootH).coerceIn(-0.45f, 0.45f)
                    nodeTunings[popNodeKey] = cur.copy(dxNorm = nextDx, dyNorm = nextDy)
                    persistCurrentTunings()
                },
                onHideOrDeleteNode = {
                    if (curCustomSpec != null) {
                        customButtons.removeAll { it.id == popNodeKey }
                        nodeTunings.remove(popNodeKey)
                        persistCustomButtons()
                        persistCurrentTunings()
                        activePopoverNodeKey = null
                        selectedNodeKey = "abxy"
                    } else {
                        val cur = nodeTunings[popNodeKey] ?: PadNodeTuning()
                        nodeTunings[popNodeKey] = cur.copy(hidden = !cur.hidden)
                        persistCurrentTunings()
                        if (!isCustomizeMode && !cur.hidden) {
                            activePopoverNodeKey = null
                        }
                    }
                },
                onResetSingleNode = {
                    nodeTunings.remove(popNodeKey)
                    persistCurrentTunings()
                },
                onToggleAbxySplit = {
                    val nextSplit = !globalSettings.abxySplitMode
                    updateGlobalSettings(globalSettings.copy(abxySplitMode = nextSplit))
                    val nextKey = if (nextSplit) "btn_a" else "abxy"
                    selectedNodeKey = nextKey
                    activePopoverNodeKey = nextKey
                },
                onToggleDpadSplit = {
                    val nextSplit = !globalSettings.dpadSplitMode
                    updateGlobalSettings(globalSettings.copy(dpadSplitMode = nextSplit))
                    val nextKey = if (nextSplit) "dpad_up" else "dpad"
                    selectedNodeKey = nextKey
                    activePopoverNodeKey = nextKey
                },
                onEnterOrExitEditMode = {
                    isCustomizeMode = !isCustomizeMode
                    activePopoverNodeKey = null
                },
                onUseGlobalLayout = {
                    updateGlobalSettings(
                        globalSettings.copy(
                            layoutScope = if (globalSettings.layoutScope == PadLayoutScope.PER_GAME) {
                                PadLayoutScope.GLOBAL
                            } else {
                                PadLayoutScope.PER_GAME
                            }
                        )
                    )
                },
                onShowAllNodes = {
                    val keys = nodeTunings.keys.toList()
                    for (k in keys) {
                        val cur = nodeTunings[k] ?: continue
                        if (cur.hidden) nodeTunings[k] = cur.copy(hidden = false)
                    }
                    persistCurrentTunings()
                },
                onResetAllNodes = {
                    nodeTunings.clear()
                    persistCurrentTunings()
                    updateGlobalSettings(
                        globalSettings.copy(
                            globalScale = 1.0f,
                            globalOpacity = 0.85f
                        )
                    )
                },
                onRecenterGyro = {
                    gyroRecenterTick++
                    triggerLocalTouchTick()
                },
                onOpenFullSettingsSheet = {
                    activePopoverNodeKey = null
                    showGlobalSettingsSheet = true
                },
                onDisconnect = {
                    activePopoverNodeKey = null
                    onDisconnect()
                },
                onClose = {
                    persistCurrentTunings()
                    activePopoverNodeKey = null
                },
                modifier = Modifier.offset { offsetPx }
            )
        }

        // === 7. BẢNG CÀI ĐẶT TAY CẦM CHUNG (Con quay hồi chuyển, Rung, Deadzone, Bố cục chung/riêng) ===
        if (showGlobalSettingsSheet) {
            val hiddenCount = nodeTunings.values.count { it.hidden }
            PadGlobalSettingsSheet(
                lang = lang,
                tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "Ravno TV" },
                tvHost = state.selectedTv?.hostAddress.orEmpty().ifBlank { "--" },
                activeGameTitle = state.activeGameTitle,
                pingMs = state.measuredPingMs,
                currentPreset = currentPreset,
                settings = globalSettings,
                hiddenNodeCount = hiddenCount,
                customButtonCount = customButtons.size,
                onSelectPreset = { preset ->
                    currentPreset = preset
                    prefs.edit().putString("active_preset_id", preset.id).apply()
                    triggerLocalTouchTick()
                },
                onUpdateSettings = { next ->
                    updateGlobalSettings(next)
                },
                onOpenEditLayoutMode = {
                    showGlobalSettingsSheet = false
                    isCustomizeMode = true
                },
                onOpenManageButtonsModal = {
                    showGlobalSettingsSheet = false
                    showManageButtonsModal = true
                },
                onShowAllHiddenNodes = {
                    val keys = nodeTunings.keys.toList()
                    for (k in keys) {
                        val cur = nodeTunings[k] ?: continue
                        if (cur.hidden) nodeTunings[k] = cur.copy(hidden = false)
                    }
                    persistCurrentTunings()
                },
                onResetCurrentLayout = {
                    nodeTunings.clear()
                    persistCurrentTunings()
                },
                onTestVibration = {
                    triggerLocalTouchTick(strong = true, overrideLevel = PadHapticLevel.STRONG.id)
                },
                onRecenterGyro = {
                    gyroRecenterTick++
                    triggerLocalTouchTick()
                },
                onDisconnect = {
                    showGlobalSettingsSheet = false
                    onDisconnect()
                },
                onForgetDevice = {
                    showGlobalSettingsSheet = false
                    onForgetDevice()
                },
                onClose = {
                    showGlobalSettingsSheet = false
                }
            )
        }

        // === 8. BẢNG THÊM NÚT TRỐNG (1 CHẠM) & ẨN / HIỆN NÚT ===
        if (showManageButtonsModal) {
            PadManageButtonsModal(
                lang = lang,
                dpadSplitMode = globalSettings.dpadSplitMode,
                abxySplitMode = globalSettings.abxySplitMode,
                nodeTunings = nodeTunings,
                customButtons = customButtons,
                onToggleNodeHidden = { key ->
                    val cur = nodeTunings[key] ?: PadNodeTuning()
                    nodeTunings[key] = cur.copy(hidden = !cur.hidden)
                    persistCurrentTunings()
                },
                onShowAllNodes = {
                    val keys = nodeTunings.keys.toList()
                    for (k in keys) {
                        val cur = nodeTunings[k] ?: continue
                        if (cur.hidden) nodeTunings[k] = cur.copy(hidden = false)
                    }
                    persistCurrentTunings()
                },
                onAddBlankCustomButton = {
                    val nextNum = customButtons.size + 1
                    val newId = "custom_${System.currentTimeMillis()}"
                    val spec = PadCustomButtonSpec(
                        id = newId,
                        label = "M$nextNum",
                        shape = PadCustomBtnShape.ROUND,
                        assignedMask = 0
                    )
                    customButtons.add(spec)
                    val idx = customButtons.size - 1
                    nodeTunings[newId] = PadNodeTuning(
                        dxNorm = (-0.10f + (idx % 4) * 0.07f).coerceIn(-0.35f, 0.35f),
                        dyNorm = (-0.06f + (idx / 4) * 0.09f).coerceIn(-0.35f, 0.35f)
                    )
                    persistCustomButtons()
                    persistCurrentTunings()
                    isCustomizeMode = true
                    selectedNodeKey = newId
                    showManageButtonsModal = false
                    activePopoverNodeKey = newId
                },
                onSelectCustomButton = { customId ->
                    isCustomizeMode = true
                    selectedNodeKey = customId
                    showManageButtonsModal = false
                    activePopoverNodeKey = customId
                },
                onDeleteCustomButton = { id ->
                    customButtons.removeAll { it.id == id }
                    nodeTunings.remove(id)
                    persistCustomButtons()
                    persistCurrentTunings()
                },
                onClose = {
                    showManageButtonsModal = false
                }
            )
        }
    }
}

@Composable
private fun PadTopBarButton(
    label: String,
    corner: Dp,
    stroke: Dp,
    fontSize: TextUnit,
    isActive: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = when {
        isDanger -> PadDanger
        isActive -> OfficialHudAccent
        else -> OfficialHudBorder
    }
    val textColor = when {
        isDanger -> PadDanger
        isActive -> OfficialHudAccent
        else -> OfficialPadBtnText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(corner))
            .background(if (isActive) OfficialHudSelBg else OfficialHudSurface2)
            .border(stroke, borderColor, RoundedCornerShape(corner))
            .clickable(onClick = onClick)
            .padding(horizontal = corner * 1.15f, vertical = corner * 0.50f)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Nút cò LT / RT (hỗ trợ Bấm thả / Chạm khóa / Nhấp nhanh Turbo + kéo dọc điều tiết cò 0–100% + Combo).
 */
@Composable
private fun OfficialGamepadTriggerButton(
    label: String,
    width: Dp,
    height: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
    comboMask: Int,
    analogDragMode: Boolean,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onTriggerValueChanged: (Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    var activeVal by remember { mutableFloatStateOf(0f) }
    val currentOnTrigger by rememberUpdatedState(onTriggerValueChanged)

    fun updateTriggerAndCombo(v: Float, justPressed: Boolean) {
        currentOnTrigger(v, justPressed)
        if (comboMask != 0) {
            PadNetworkClient.updateButtonsMask(comboMask, if (v > 0.1f) comboMask else 0)
        }
    }

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            activeVal = 0f
            updateTriggerAndCombo(0f, false)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) {
                activeVal = 0f
                updateTriggerAndCombo(0f, false)
            }
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                activeVal = 1.0f
                updateTriggerAndCombo(1.0f, false)
                delay(halfPeriodMs)
                activeVal = 0f
                updateTriggerAndCombo(0f, false)
                delay(halfPeriodMs)
            }
            activeVal = 0f
            updateTriggerAndCombo(0f, false)
        }
    }

    val isPressedVisual = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown || activeVal > 0.05f
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown || activeVal > 0.05f
    }

    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val borderAlpha = (opacity * 0.40f).coerceIn(0.05f, 0.65f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isPressedVisual) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.75f))
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isPressedVisual) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isPressedVisual -> OfficialHudAccent
                    else -> OfficialHudBorder.copy(alpha = borderAlpha)
                },
                RoundedCornerShape(corner)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(pressMode, analogDragMode, comboMask) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            fingerDown = true
                            val startY = down.position.y

                            when (pressMode) {
                                PadPressMode.NORMAL -> {
                                    val initV = if (analogDragMode) 0.35f else 1.0f
                                    activeVal = initV
                                    updateTriggerAndCombo(initV, true)
                                }
                                PadPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    activeVal = if (latched) 1.0f else 0f
                                    updateTriggerAndCombo(activeVal, latched)
                                }
                                PadPressMode.TURBO -> {
                                    updateTriggerAndCombo(1.0f, true)
                                }
                            }

                            do {
                                val event = awaitPointerEvent()
                                val activeChange = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (activeChange != null && activeChange.pressed) {
                                    activeChange.consume()
                                    val dy = activeChange.position.y - startY
                                    val hPx = size.height.coerceAtLeast(1).toFloat()
                                    if (analogDragMode || abs(dy) >= hPx * 0.18f) {
                                        val v = ((dy / hPx) + 0.65f).coerceIn(0.15f, 1.0f)
                                        activeVal = v
                                        if (pressMode == PadPressMode.NORMAL || (pressMode == PadPressMode.TOGGLE_HOLD && latched)) {
                                            updateTriggerAndCombo(v, false)
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode == PadPressMode.NORMAL) {
                                activeVal = 0f
                                updateTriggerAndCombo(0f, false)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (analogDragMode && isPressedVisual) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(activeVal.coerceIn(0f, 1f))
                    .background(OfficialHudAccent.copy(alpha = 0.25f))
            )
        }
        val textLabel = when {
            analogDragMode && isPressedVisual -> "$label ${(activeVal * 100).roundToInt()}%"
            latched -> "$label •"
            else -> label
        }
        Text(
            text = textLabel,
            color = when {
                latched -> OfficialHudCyan
                isPressedVisual -> OfficialHudAccent
                else -> OfficialPadBtnText.copy(alpha = textAlpha)
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Nút vai LB / RB hỗ trợ bật/tắt vuốt 4 hướng (↑ ↓ ← →) + gửi kèm hoặc thay thế nút vai + Combo.
 */
@Composable
private fun OfficialGamepadShoulderButton(
    label: String,
    primaryBit: Int,
    comboMask: Int,
    directionalSwipeEnabled: Boolean,
    swipeUpBit: Int,
    swipeDownBit: Int,
    swipeLeftBit: Int,
    swipeRightBit: Int,
    swipeIncludeShoulder: Boolean,
    width: Dp,
    height: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    badgeFontSize: TextUnit,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    var activeSwipeBit by remember { mutableIntStateOf(0) }
    var activeSwipeDir by remember { mutableIntStateOf(0) } // 1: Up, 2: Down, 3: Left, 4: Right
    var tapPulseToken by remember { mutableIntStateOf(0) }

    val fullPrimaryMask = primaryBit or comboMask

    LaunchedEffect(tapPulseToken) {
        if (tapPulseToken > 0) {
            PadNetworkClient.updateButtonsMask(fullPrimaryMask, fullPrimaryMask)
            delay(65L)
            if (!fingerDown && !latched) {
                PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
            }
        }
    }

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            if (activeSwipeBit != 0) {
                PadNetworkClient.updateButtonsMask(activeSwipeBit, 0)
                activeSwipeBit = 0
                activeSwipeDir = 0
            }
            PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                PadNetworkClient.updateButtonsMask(fullPrimaryMask, fullPrimaryMask)
                delay(halfPeriodMs)
                PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
                delay(halfPeriodMs)
            }
            PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
        }
    }

    val isSwiped = activeSwipeBit != 0
    val isVisualActive = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown || isSwiped
        PadPressMode.TOGGLE_HOLD -> latched || isSwiped
        PadPressMode.TURBO -> fingerDown || isSwiped
    }

    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val borderAlpha = (opacity * 0.40f).coerceIn(0.05f, 0.65f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    val activeSwipeBadge = if (isSwiped) formatMaskBadge(activeSwipeBit) else ""
    val activeDirArrow = when (activeSwipeDir) {
        1 -> "↑"
        2 -> "↓"
        3 -> "←"
        4 -> "→"
        else -> ""
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isVisualActive) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.75f))
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isVisualActive) strokeActive else strokeThin,
                when {
                    isSwiped || latched -> OfficialHudCyan
                    isVisualActive -> OfficialHudAccent
                    else -> OfficialHudBorder.copy(alpha = borderAlpha)
                },
                RoundedCornerShape(corner)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(
                        fullPrimaryMask,
                        directionalSwipeEnabled,
                        swipeUpBit,
                        swipeDownBit,
                        swipeLeftBit,
                        swipeRightBit,
                        swipeIncludeShoulder,
                        pressMode
                    ) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            val startPos = down.position
                            var triggeredSwipeBit = 0
                            var triggeredDir = 0
                            fingerDown = true
                            onHaptic(false)

                            val holdPrimaryImmediately = !directionalSwipeEnabled || swipeIncludeShoulder
                            when (pressMode) {
                                PadPressMode.NORMAL -> {
                                    if (holdPrimaryImmediately) {
                                        PadNetworkClient.updateButtonsMask(fullPrimaryMask, fullPrimaryMask)
                                    }
                                }
                                PadPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    PadNetworkClient.updateButtonsMask(fullPrimaryMask, if (latched) fullPrimaryMask else 0)
                                }
                                PadPressMode.TURBO -> {}
                            }

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (change != null && change.pressed) {
                                    change.consume()
                                    if (directionalSwipeEnabled) {
                                        val dx = change.position.x - startPos.x
                                        val dy = change.position.y - startPos.y
                                        val dist = hypot(dx, dy)
                                        val threshold = minOf(size.width, size.height).coerceAtLeast(24) * 0.34f
                                        if (dist >= threshold) {
                                            val (candDir, candBit) = if (abs(dx) > abs(dy)) {
                                                if (dx < 0) 3 to swipeLeftBit else 4 to swipeRightBit
                                            } else {
                                                if (dy < 0) 1 to swipeUpBit else 2 to swipeDownBit
                                            }
                                            if (candBit != 0 && candBit != triggeredSwipeBit) {
                                                if (triggeredSwipeBit != 0) {
                                                    PadNetworkClient.updateButtonsMask(triggeredSwipeBit, 0)
                                                }
                                                triggeredSwipeBit = candBit
                                                triggeredDir = candDir
                                                activeSwipeBit = candBit
                                                activeSwipeDir = candDir
                                                onHaptic(true)
                                                if (!swipeIncludeShoulder && pressMode == PadPressMode.NORMAL) {
                                                    PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
                                                }
                                                PadNetworkClient.updateButtonsMask(candBit, candBit)
                                            }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode == PadPressMode.NORMAL) {
                                if (holdPrimaryImmediately) {
                                    PadNetworkClient.updateButtonsMask(fullPrimaryMask, 0)
                                } else if (triggeredSwipeBit == 0 && triggeredDir == 0) {
                                    tapPulseToken++
                                }
                            }
                            if (triggeredSwipeBit != 0) {
                                activeSwipeBit = 0
                                activeSwipeDir = 0
                                PadNetworkClient.updateButtonsMask(triggeredSwipeBit, 0)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (directionalSwipeEnabled) {
            val hintColor = OfficialPadBtnText.copy(alpha = (textAlpha * 0.62f).coerceIn(0.05f, 0.82f))
            if (swipeUpBit != 0) {
                Text(
                    text = "↑${formatMaskBadge(swipeUpBit)}",
                    color = if (activeSwipeDir == 1) OfficialHudCyan else hintColor,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = corner * 0.08f)
                )
            }
            if (swipeDownBit != 0) {
                Text(
                    text = "↓${formatMaskBadge(swipeDownBit)}",
                    color = if (activeSwipeDir == 2) OfficialHudCyan else hintColor,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = corner * 0.08f)
                )
            }
            if (swipeLeftBit != 0) {
                Text(
                    text = "←${formatMaskBadge(swipeLeftBit)}",
                    color = if (activeSwipeDir == 3) OfficialHudCyan else hintColor,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = corner * 0.22f)
                )
            }
            if (swipeRightBit != 0) {
                Text(
                    text = "→${formatMaskBadge(swipeRightBit)}",
                    color = if (activeSwipeDir == 4) OfficialHudCyan else hintColor,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = corner * 0.22f)
                )
            }
        }
        val textLabel = when {
            isSwiped && activeSwipeBadge.isNotEmpty() ->
                if (swipeIncludeShoulder) "$label+$activeSwipeBadge" else "$activeDirArrow$activeSwipeBadge"
            latched -> "$label •"
            else -> label
        }
        Text(
            text = textLabel,
            color = when {
                isSwiped || latched -> OfficialHudCyan
                isVisualActive -> OfficialHudAccent
                else -> OfficialPadBtnText.copy(alpha = textAlpha)
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun OfficialGamepadPillButton(
    label: String,
    bit: Int,
    comboMask: Int = 0,
    width: Dp,
    height: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    testTag: String
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    val fullMask = bit or comboMask

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            PadNetworkClient.updateButtonsMask(fullMask, 0)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonsMask(fullMask, 0)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                PadNetworkClient.updateButtonsMask(fullMask, fullMask)
                delay(halfPeriodMs)
                PadNetworkClient.updateButtonsMask(fullMask, 0)
                delay(halfPeriodMs)
            }
            PadNetworkClient.updateButtonsMask(fullMask, 0)
        }
    }

    val isVisualActive = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown
    }

    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val borderAlpha = (opacity * 0.40f).coerceIn(0.05f, 0.65f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isVisualActive) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.75f))
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isVisualActive) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isVisualActive -> OfficialHudAccent
                    else -> OfficialHudBorder.copy(alpha = borderAlpha)
                },
                RoundedCornerShape(corner)
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(fullMask, pressMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            fingerDown = true
                            onHaptic()
                            when (pressMode) {
                                PadPressMode.NORMAL -> PadNetworkClient.updateButtonsMask(fullMask, fullMask)
                                PadPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    PadNetworkClient.updateButtonsMask(fullMask, if (latched) fullMask else 0)
                                }
                                PadPressMode.TURBO -> {}
                            }
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { if (it.pressed) it.consume() }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode == PadPressMode.NORMAL) {
                                PadNetworkClient.updateButtonsMask(fullMask, 0)
                            }
                        }
                    }
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (latched) "$label •" else label,
            color = when {
                latched -> OfficialHudCyan
                isVisualActive -> OfficialHudAccent
                else -> OfficialPadBtnText.copy(alpha = textAlpha)
            },
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Nút tròn Xbox Guide (vòng tròn kèm chấm ngọc lục bảo ở tâm theo phong cách giao diện Ravno Pad mới).
 */
@Composable
private fun OfficialXboxNexusButton(
    diameter: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
    comboMask: Int,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    testTag: String
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    val fullMask = RemotePadButtons.XBOX or comboMask

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            PadNetworkClient.updateButtonsMask(fullMask, 0)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonsMask(fullMask, 0)
        }
    }

    val isVisualActive = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown
    }

    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val ringAlpha = opacity.coerceIn(0.08f, 1.0f)

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                if (isVisualActive) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.75f))
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isVisualActive) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isVisualActive -> OfficialHudAccent
                    else -> OfficialHudAccent.copy(alpha = ringAlpha * 0.75f)
                },
                CircleShape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(pressMode, turboHz, fullMask) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            fingerDown = true
                            onHaptic()
                            when (pressMode) {
                                PadPressMode.NORMAL, PadPressMode.TURBO -> PadNetworkClient.updateButtonsMask(fullMask, fullMask)
                                PadPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    PadNetworkClient.updateButtonsMask(fullMask, if (latched) fullMask else 0)
                                }
                            }
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { if (it.pressed) it.consume() }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode != PadPressMode.TOGGLE_HOLD) {
                                PadNetworkClient.updateButtonsMask(fullMask, 0)
                            }
                        }
                    }
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter * 0.48f)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val tint = when {
                latched -> OfficialHudCyan
                isVisualActive -> Color.White
                else -> OfficialHudAccent.copy(alpha = ringAlpha)
            }
            drawCircle(
                color = tint,
                radius = size.minDimension * 0.44f,
                center = c,
                style = Stroke(width = strokeActive.toPx())
            )
            drawCircle(
                color = tint,
                radius = size.minDimension * 0.16f,
                center = c
            )
        }
    }
}

/**
 * Cụm D-Pad chữ thập 8 hướng.
 */
@Composable
private fun OfficialVirtualDpadCross(
    size: Dp,
    corner: Dp,
    keyCorner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDpadMask = RemotePadButtons.DPAD_UP or
        RemotePadButtons.DPAD_DOWN or
        RemotePadButtons.DPAD_LEFT or
        RemotePadButtons.DPAD_RIGHT
    var activeMask by remember { mutableIntStateOf(0) }

    val containerBgAlpha = (opacity * 0.52f).coerceIn(0.03f, 0.72f)
    val borderAlpha = (opacity * 0.35f).coerceIn(0.04f, 0.55f)
    val btnSize = size * 0.33f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(OfficialHudSurface.copy(alpha = containerBgAlpha))
            .border(strokeThin, OfficialHudBorder.copy(alpha = borderAlpha), RoundedCornerShape(corner))
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            val cx = this.size.width / 2f
                            val cy = this.size.height / 2f
                            val minDeadRadius = this.size.width * 0.12f

                            fun evaluatePos(pos: Offset) {
                                val dx = pos.x - cx
                                val dy = pos.y - cy
                                val dist = sqrt(dx * dx + dy * dy)
                                var mask = 0
                                if (dist >= minDeadRadius) {
                                    val deg = atan2(dy, dx) * (180f / PI.toFloat())
                                    if (deg in -157.5f..-22.5f) mask = mask or RemotePadButtons.DPAD_UP
                                    if (deg in 22.5f..157.5f) mask = mask or RemotePadButtons.DPAD_DOWN
                                    if (deg in -67.5f..67.5f) mask = mask or RemotePadButtons.DPAD_RIGHT
                                    if (deg <= -112.5f || deg >= 112.5f) mask = mask or RemotePadButtons.DPAD_LEFT
                                }
                                if (mask != activeMask) {
                                    if (mask != 0) onHaptic()
                                    activeMask = mask
                                    PadNetworkClient.updateButtonsMask(allDpadMask, mask)
                                }
                            }

                            evaluatePos(down.position)
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (change != null && change.pressed) {
                                    change.consume()
                                    evaluatePos(change.position)
                                }
                            } while (event.changes.any { it.pressed })

                            if (activeMask != 0) {
                                activeMask = 0
                                PadNetworkClient.updateButtonsMask(allDpadMask, 0)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        OfficialDpadVisualKey(
            modifier = Modifier.align(Alignment.TopCenter),
            symbol = "▲",
            sizeDp = btnSize,
            corner = keyCorner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and RemotePadButtons.DPAD_UP) != 0,
            opacity = opacity
        )
        OfficialDpadVisualKey(
            modifier = Modifier.align(Alignment.BottomCenter),
            symbol = "▼",
            sizeDp = btnSize,
            corner = keyCorner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and RemotePadButtons.DPAD_DOWN) != 0,
            opacity = opacity
        )
        OfficialDpadVisualKey(
            modifier = Modifier.align(Alignment.CenterStart),
            symbol = "◀",
            sizeDp = btnSize,
            corner = keyCorner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and RemotePadButtons.DPAD_LEFT) != 0,
            opacity = opacity
        )
        OfficialDpadVisualKey(
            modifier = Modifier.align(Alignment.CenterEnd),
            symbol = "▶",
            sizeDp = btnSize,
            corner = keyCorner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and RemotePadButtons.DPAD_RIGHT) != 0,
            opacity = opacity
        )

        Box(
            modifier = Modifier
                .size(size * 0.135f)
                .clip(CircleShape)
                .background(
                    if (activeMask != 0) {
                        OfficialHudAccent
                    } else {
                        OfficialHudBorder.copy(alpha = (opacity * 0.45f).coerceIn(0.05f, 0.65f))
                    }
                )
        )
    }
}

@Composable
private fun OfficialDpadVisualKey(
    modifier: Modifier,
    symbol: String,
    sizeDp: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    isPressed: Boolean,
    opacity: Float
) {
    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val borderAlpha = (opacity * 0.40f).coerceIn(0.05f, 0.65f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(sizeDp * 0.05f)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isPressed) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.75f))
                } else {
                    OfficialHudSurface2.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressed) strokeActive else strokeThin,
                color = if (isPressed) OfficialHudAccent else OfficialHudBorder.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(corner)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isPressed) OfficialHudAccent else OfficialPadBtnText.copy(alpha = textAlpha),
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Cụm 4 nút kim cương Y / X / B / A (hỗ trợ trượt ngón cái bấm tổ hợp).
 */
@Composable
private fun OfficialVirtualAbxyPad(
    size: Dp,
    swapSwitchAbxy: Boolean,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBit = if (swapSwitchAbxy) RemotePadButtons.X else RemotePadButtons.Y
    val leftBit = if (swapSwitchAbxy) RemotePadButtons.Y else RemotePadButtons.X
    val rightBit = if (swapSwitchAbxy) RemotePadButtons.A else RemotePadButtons.B
    val bottomBit = if (swapSwitchAbxy) RemotePadButtons.B else RemotePadButtons.A

    val topLabel = if (swapSwitchAbxy) "X" else "Y"
    val leftLabel = if (swapSwitchAbxy) "Y" else "X"
    val rightLabel = if (swapSwitchAbxy) "A" else "B"
    val bottomLabel = if (swapSwitchAbxy) "B" else "A"

    val topColor = if (swapSwitchAbxy) OfficialPadXColor else OfficialPadYColor
    val leftColor = if (swapSwitchAbxy) OfficialPadYColor else OfficialPadXColor
    val rightColor = if (swapSwitchAbxy) OfficialPadAColor else OfficialPadBColor
    val bottomColor = if (swapSwitchAbxy) OfficialPadBColor else OfficialPadAColor

    val allAbxyMask = RemotePadButtons.A or RemotePadButtons.B or RemotePadButtons.X or RemotePadButtons.Y
    var activeMask by remember { mutableIntStateOf(0) }

    val containerBgAlpha = (opacity * 0.45f).coerceIn(0.03f, 0.65f)
    val borderAlpha = (opacity * 0.32f).coerceIn(0.04f, 0.50f)
    val btnDiam = size * 0.35f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(OfficialHudSurface.copy(alpha = containerBgAlpha))
            .border(strokeThin, OfficialHudBorder.copy(alpha = borderAlpha), CircleShape)
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(swapSwitchAbxy) {
                        awaitEachGesture {
                            do {
                                val event = awaitPointerEvent()
                                var mask = 0
                                val wPx = this.size.width.coerceAtLeast(1).toFloat()
                                val hPx = this.size.height.coerceAtLeast(1).toFloat()
                                val hitRadius = minOf(wPx, hPx) * 0.28f

                                val topCenter = Offset(wPx * 0.50f, hPx * 0.18f)
                                val bottomCenter = Offset(wPx * 0.50f, hPx * 0.82f)
                                val leftCenter = Offset(wPx * 0.18f, hPx * 0.50f)
                                val rightCenter = Offset(wPx * 0.82f, hPx * 0.50f)

                                for (ch in event.changes) {
                                    if (!ch.pressed) continue
                                    ch.consume()
                                    val p = ch.position
                                    if (hypot(p.x - topCenter.x, p.y - topCenter.y) <= hitRadius) mask = mask or topBit
                                    if (hypot(p.x - bottomCenter.x, p.y - bottomCenter.y) <= hitRadius) mask = mask or bottomBit
                                    if (hypot(p.x - leftCenter.x, p.y - leftCenter.y) <= hitRadius) mask = mask or leftBit
                                    if (hypot(p.x - rightCenter.x, p.y - rightCenter.y) <= hitRadius) mask = mask or rightBit
                                }

                                if (mask != activeMask) {
                                    if ((mask and activeMask.inv()) != 0) {
                                        onHaptic()
                                    }
                                    activeMask = mask
                                    PadNetworkClient.updateButtonsMask(allAbxyMask, mask)
                                }
                            } while (event.changes.any { it.pressed })

                            if (activeMask != 0) {
                                activeMask = 0
                                PadNetworkClient.updateButtonsMask(allAbxyMask, 0)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        OfficialAbxyVisualButton(
            modifier = Modifier.align(Alignment.TopCenter),
            label = topLabel,
            accentColor = topColor,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and topBit) != 0,
            opacity = opacity
        )
        OfficialAbxyVisualButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            label = bottomLabel,
            accentColor = bottomColor,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and bottomBit) != 0,
            opacity = opacity
        )
        OfficialAbxyVisualButton(
            modifier = Modifier.align(Alignment.CenterStart),
            label = leftLabel,
            accentColor = leftColor,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and leftBit) != 0,
            opacity = opacity
        )
        OfficialAbxyVisualButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            label = rightLabel,
            accentColor = rightColor,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            isPressed = (activeMask and rightBit) != 0,
            opacity = opacity
        )
    }
}

@Composable
private fun OfficialAbxyVisualButton(
    modifier: Modifier,
    label: String,
    accentColor: Color,
    diameter: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    isPressed: Boolean,
    opacity: Float
) {
    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val borderAlpha = (opacity * 0.45f).coerceIn(0.05f, 0.75f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    Box(
        modifier = modifier
            .size(diameter)
            .padding(diameter * 0.04f)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.28f)
                } else {
                    OfficialHudSurface2.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressed) strokeActive else strokeThin,
                color = if (isPressed) accentColor else accentColor.copy(alpha = borderAlpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor.copy(alpha = textAlpha),
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun OfficialStandaloneColoredAbxyButton(
    label: String,
    accentColor: Color,
    bit: Int,
    comboMask: Int,
    diameter: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit
) {
    var fingerDown by remember { mutableStateOf(false) }
    var latched by remember { mutableStateOf(false) }
    val fullMask = bit or comboMask

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            PadNetworkClient.updateButtonsMask(fullMask, 0)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonsMask(fullMask, 0)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                PadNetworkClient.updateButtonsMask(fullMask, fullMask)
                delay(halfPeriodMs)
                PadNetworkClient.updateButtonsMask(fullMask, 0)
                delay(halfPeriodMs)
            }
            PadNetworkClient.updateButtonsMask(fullMask, 0)
        }
    }

    val isPressed = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown
    }

    val bgAlpha = (opacity * 0.78f).coerceIn(0.04f, 0.94f)
    val borderAlpha = (opacity * 0.45f).coerceIn(0.05f, 0.75f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.28f)
                } else {
                    OfficialHudSurface2.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isPressed) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isPressed -> accentColor
                    else -> accentColor.copy(alpha = borderAlpha)
                },
                CircleShape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(fullMask, pressMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            fingerDown = true
                            onHaptic()
                            when (pressMode) {
                                PadPressMode.NORMAL -> PadNetworkClient.updateButtonsMask(fullMask, fullMask)
                                PadPressMode.TOGGLE_HOLD -> {
                                    latched = !latched
                                    PadNetworkClient.updateButtonsMask(fullMask, if (latched) fullMask else 0)
                                }
                                PadPressMode.TURBO -> {}
                            }
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { if (it.pressed) it.consume() }
                            } while (event.changes.any { it.pressed })

                            fingerDown = false
                            if (pressMode == PadPressMode.NORMAL) {
                                PadNetworkClient.updateButtonsMask(fullMask, 0)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (latched) "$label•" else label,
            color = when {
                latched -> OfficialHudCyan
                isPressed -> Color.White
                else -> accentColor.copy(alpha = textAlpha)
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Cần gạt Analog (hỗ trợ Độ nhạy, Đường cong phản hồi, Vùng chết tâm/rìa, tia vector nối tâm và Tự chạy nhanh 🏃 L3).
 */
@Composable
private fun OfficialVirtualAnalogStick(
    label: String,
    size: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    tinyFontSize: TextUnit,
    opacity: Float,
    sensitivity: Float,
    responseCurve: PadStickCurve,
    innerDeadzone: Float,
    outerDeadzone: Float,
    autoSprintL3: Boolean,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onStickChanged: (Float, Float, Int) -> Unit,
    onDoubleTapStickClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val knobSizeDp = size * 0.41f
    val maxRadiusPx = remember(density, size) {
        with(density) { (size * 0.36f).toPx() }.coerceAtLeast(20f)
    }

    var stickPxX by remember { mutableFloatStateOf(0f) }
    var stickPxY by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }
    var isAutoSprinting by remember { mutableStateOf(false) }

    val idleBorder = OfficialHudBorder.copy(alpha = (opacity * 0.45f).coerceIn(0.05f, 0.70f))
    val outerBgAlpha = (opacity * 0.58f).coerceIn(0.04f, 0.78f)
    val knobBgAlpha = (opacity * 0.75f).coerceIn(0.05f, 0.92f)
    val knobBorderAlpha = (opacity * 0.55f).coerceIn(0.06f, 0.80f)
    val textAlpha = opacity.coerceIn(0.05f, 1.0f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(OfficialHudSurface.copy(alpha = outerBgAlpha))
            .border(
                width = if (isTouching) strokeActive else strokeThin,
                color = when {
                    isAutoSprinting -> OfficialHudCyan
                    isTouching -> OfficialHudAccent
                    else -> idleBorder
                },
                shape = CircleShape
            )
            .then(
                if (isEditMode) {
                    Modifier
                } else {
                    Modifier.pointerInput(
                        autoSprintL3,
                        maxRadiusPx,
                        sensitivity,
                        responseCurve,
                        innerDeadzone,
                        outerDeadzone
                    ) {
                        var lastDownTimeMs = 0L
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            isTouching = true
                            var doubleTapHeld = false
                            val now = System.currentTimeMillis()
                            if (now - lastDownTimeMs <= 260L) {
                                doubleTapHeld = true
                                onDoubleTapStickClick(true)
                            }
                            lastDownTimeMs = now

                            val centerX = this.size.width / 2f
                            val centerY = this.size.height / 2f

                            fun applyAxisShaping(raw: Float): Float {
                                val mag = abs(raw)
                                val inDz = innerDeadzone.coerceIn(0.00f, 0.30f)
                                val outDz = outerDeadzone.coerceIn(0.70f, 1.0f)
                                if (mag < inDz) return 0f
                                val remapped = ((mag - inDz) / (outDz - inDz).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                                val signed = if (raw < 0f) -remapped else remapped
                                val curved = responseCurve.applyCurve(signed)
                                return (curved * sensitivity).coerceIn(-1f, 1f)
                            }

                            fun updateFromPos(pos: Offset) {
                                val dx = pos.x - centerX
                                val dy = pos.y - centerY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist <= maxRadiusPx) {
                                    stickPxX = dx
                                    stickPxY = dy
                                } else if (dist > 0.001f) {
                                    stickPxX = (dx / dist) * maxRadiusPx
                                    stickPxY = (dy / dist) * maxRadiusPx
                                } else {
                                    stickPxX = 0f
                                    stickPxY = 0f
                                }

                                val rawX = (stickPxX / maxRadiusPx).coerceIn(-1f, 1f)
                                val rawY = (stickPxY / maxRadiusPx).coerceIn(-1f, 1f)
                                val nx = applyAxisShaping(rawX)
                                val ny = applyAxisShaping(rawY)

                                if (autoSprintL3) {
                                    val rawUnclampedX = dx / maxRadiusPx
                                    val rawUnclampedY = dy / maxRadiusPx
                                    val shouldSprint = if (!isAutoSprinting) {
                                        rawUnclampedY < -1.18f && abs(rawUnclampedX) < 0.58f
                                    } else {
                                        rawY < -0.60f && abs(rawX) < 0.68f
                                    }
                                    isAutoSprinting = shouldSprint
                                } else {
                                    isAutoSprinting = false
                                }

                                val sprintBit = if (isAutoSprinting) RemotePadButtons.L3 else 0
                                onStickChanged(nx, ny, sprintBit)
                            }

                            updateFromPos(down.position)
                            do {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (ch != null && ch.pressed) {
                                    ch.consume()
                                    updateFromPos(ch.position)
                                }
                            } while (event.changes.any { it.pressed })

                            isTouching = false
                            stickPxX = 0f
                            stickPxY = 0f
                            isAutoSprinting = false
                            onStickChanged(0f, 0f, 0)
                            if (doubleTapHeld) {
                                onDoubleTapStickClick(false)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Vòng vùng chết ngoài & trong + Tia vector nối tâm
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(
                color = OfficialHudBorder.copy(alpha = (opacity * 0.35f).coerceIn(0.04f, 0.55f)),
                radius = maxRadiusPx * outerDeadzone.coerceIn(0.75f, 1.0f),
                center = c,
                style = Stroke(width = strokeThin.toPx())
            )
            drawCircle(
                color = OfficialHudCyan.copy(alpha = (opacity * 0.25f).coerceIn(0.04f, 0.45f)),
                radius = maxRadiusPx * innerDeadzone.coerceIn(0.02f, 0.25f),
                center = c,
                style = Stroke(width = strokeThin.toPx())
            )
            if (isTouching && (abs(stickPxX) > 2f || abs(stickPxY) > 2f)) {
                drawLine(
                    color = if (isAutoSprinting) OfficialHudCyan else OfficialHudAccent,
                    start = c,
                    end = Offset(c.x + stickPxX, c.y + stickPxY),
                    strokeWidth = strokeActive.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (isAutoSprinting || (autoSprintL3 && isTouching)) {
            Text(
                text = if (isAutoSprinting) "🏃 L3" else "▲ L3",
                color = if (isAutoSprinting) OfficialHudCyan else OfficialPadBtnText.copy(alpha = 0.55f),
                fontSize = tinyFontSize,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = size * 0.04f)
            )
        }

        // Núm gạt tròn
        Box(
            modifier = Modifier
                .offset { IntOffset(stickPxX.roundToInt(), stickPxY.roundToInt()) }
                .size(knobSizeDp)
                .clip(CircleShape)
                .background(
                    when {
                        isAutoSprinting -> Color(0xFF10313A).copy(alpha = opacity.coerceAtLeast(0.75f))
                        isTouching -> OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.75f))
                        else -> OfficialHudSurface2.copy(alpha = knobBgAlpha)
                    }
                )
                .border(
                    width = if (isTouching) strokeActive else strokeThin,
                    color = when {
                        isAutoSprinting -> OfficialHudCyan
                        isTouching -> OfficialHudAccent
                        else -> OfficialHudBorder.copy(alpha = knobBorderAlpha)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = OfficialPadBtnText.copy(alpha = textAlpha),
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize
            )
        }
    }
}
