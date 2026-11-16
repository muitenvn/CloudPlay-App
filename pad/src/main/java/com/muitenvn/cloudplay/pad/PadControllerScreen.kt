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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muitenvn.cloudplay.protocol.RemotePadButtons
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Bảng màu chính thức đồng nhất với VirtualGamepadOverlay của CloudPlay (:app)
private val OfficialHudSurface = Color(0xFF0E1411)
private val OfficialHudSurface2 = Color(0xFF151E19)
private val OfficialHudSelBg = Color(0xFF143322)
private val OfficialHudAccent = Color(0xFF2EE56B)
private val OfficialHudCyan = Color(0xFF00E5FF)
private val OfficialPadBtnText = Color(0xFFEAF4EC)
private val OfficialPadAColor = Color(0xFF34D058)
private val OfficialPadBColor = Color(0xFFFF5252)
private val OfficialPadXColor = Color(0xFF38BDF8)
private val OfficialPadYColor = Color(0xFFFACC15)

enum class PadPressMode(val id: String) {
    NORMAL("normal"),
    TOGGLE_HOLD("toggle"),
    TURBO("turbo");

    fun next(): PadPressMode = when (this) {
        NORMAL -> TOGGLE_HOLD
        TOGGLE_HOLD -> TURBO
        TURBO -> NORMAL
    }

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        NORMAL -> PadStrings.tr(lang, "Bấm thả", "Normal")
        TOGGLE_HOLD -> PadStrings.tr(lang, "Chạm khóa", "Toggle Hold")
        TURBO -> PadStrings.tr(lang, "Nhấp nhanh (Turbo)", "Turbo")
    }
}

/**
 * Các mẫu bố cục tay cầm theo game (đồng bộ với VirtualGamepadOverlay chính thức của CloudPlay):
 * - Chuẩn (Standard Xbox)
 * - Bắn súng (FPS Pro: cò/nút vai và cần phải lớn hơn + tự chạy nhanh L3 khi đẩy kịch cần trái)
 * - Hành động (Action: cụm ABXY và cần phải rộng rãi)
 * - Đua xe (Racing: cò LT/RT nhạy tuyến tính theo chiều dọc, cần trái lớn)
 * - Đảo nút (Switch A↔B, X↔Y)
 */
enum class PadLayoutPreset(
    val id: String,
    val shoulderBonusScale: Float,
    val abxyBonusScale: Float,
    val stickBonusScale: Float,
    val rightStickBonusScale: Float,
    val autoSprintL3: Boolean,
    val swapSwitchAbxy: Boolean
) {
    STANDARD("standard", 1.0f, 1.0f, 1.0f, 1.0f, true, false),
    FPS_PRO("fps", 1.08f, 1.04f, 1.05f, 1.15f, true, false),
    ACTION("action", 1.04f, 1.12f, 1.04f, 1.06f, true, false),
    RACING("racing", 1.14f, 0.96f, 1.12f, 1.0f, false, false),
    SWITCH_SWAP("switch", 1.0f, 1.0f, 1.0f, 1.0f, true, true);

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        STANDARD -> PadStrings.tr(lang, "Bố cục: Chuẩn", "Layout: Standard")
        FPS_PRO -> PadStrings.tr(lang, "Bố cục: Bắn súng", "Layout: FPS Pro")
        ACTION -> PadStrings.tr(lang, "Bố cục: Hành động", "Layout: Action")
        RACING -> PadStrings.tr(lang, "Bố cục: Đua xe", "Layout: Racing")
        SWITCH_SWAP -> PadStrings.tr(lang, "Bố cục: Đảo nút", "Layout: Swapped")
    }
}

private data class PadNodeTuning(
    val dxNorm: Float = 0f,
    val dyNorm: Float = 0f,
    val scaleMult: Float = 1.0f,
    val opacity: Float = 0.85f,
    val swipeBit: Int = 0,
    val pressMode: PadPressMode = PadPressMode.NORMAL,
    val turboHz: Int = 10
)

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Màn hình Tay cầm chính thức của CloudPlay Pad (đồng bộ 100% bố cục, màu sắc và tính năng với
 * VirtualGamepadOverlay chính thức trong CloudPlay):
 * - Hàng vai trên cùng: Trái (LT, LB vuốt 4 hướng, L3) · Giữa (⧉ View, ✕ Xbox Nexus, ☰ Menu, ⚙ Chỉnh nút) · Phải (R3, RB vuốt 4 hướng, RT)
 * - Cụm dưới trái: Cần Analog Trái (L - có vòng vùng chết trong/ngoài, tia hướng và đẩy kịch tự chạy nhanh 🏃 L3) + Cụm D-Pad chữ thập 8 hướng (hoặc tách rời 4 nút hướng)
 * - Cụm dưới phải: Cần Analog Phải (R - vòng vùng chết trong/ngoài, tia hướng) + Cụm ABXY màu chuẩn Xbox (Y vàng, A xanh lá, X xanh dương, B đỏ - hỗ trợ trượt ngón cái bấm tổ hợp hoặc tách rời 4 nút)
 * - Chế độ tinh chỉnh (⚙ / Chỉnh nút): Lưới căn chỉnh (# Lưới) + tâm ngắm, Khóa vị trí, Tách/Gộp D-Pad & ABXY, chỉnh riêng từng nút (Kích thước, Độ mờ, Chế độ bấm Bấm thả/Chạm khóa/Turbo, Vuốt nút vai LB/RB).
 */
@Composable
internal fun PadLandscapeControllerScreen(
    state: PadClientUiState,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lang = state.language

    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences("cloudplay_pad_layout_prefs", Context.MODE_PRIVATE)
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
    var isPositionLocked by remember {
        mutableStateOf(prefs.getBoolean("pad_position_locked", false))
    }
    var showEditGrid by remember {
        mutableStateOf(prefs.getBoolean("pad_show_edit_grid", true))
    }
    var dpadSplitMode by remember {
        mutableStateOf(prefs.getBoolean("pad_dpad_split_mode", false))
    }
    var abxySplitMode by remember {
        mutableStateOf(prefs.getBoolean("pad_abxy_split_mode", false))
    }
    var selectedNodeKey by remember { mutableStateOf("abxy") }
    var hapticEnabled by remember {
        mutableStateOf(prefs.getBoolean("pad_touch_haptic", true))
    }

    val activeGameKey = remember(state.activeGameTitle) {
        state.activeGameTitle.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
    }
    val storageKey = remember(currentPreset, activeGameKey) {
        if (activeGameKey.isNotEmpty() && activeGameKey != "generic") {
            "${currentPreset.id}_$activeGameKey"
        } else {
            currentPreset.id
        }
    }

    val nodeTunings = remember(storageKey) {
        mutableStateMapOf<String, PadNodeTuning>().apply {
            val loaded = loadPresetTunings(prefs, storageKey)
            if (loaded.isNotEmpty()) {
                putAll(loaded)
            } else {
                putAll(loadPresetTunings(prefs, currentPreset.id))
            }
        }
    }

    fun persistCurrentTunings() {
        savePresetTunings(prefs, storageKey, nodeTunings)
    }

    fun triggerLocalTouchTick(strong: Boolean = false) {
        if (!hapticEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(
                    VibrationEffect.createPredefined(
                        if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK
                    )
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 18L else 10L, if (strong) 150 else 85))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12L)
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PadBg)
            .onSizeChanged { sz ->
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
        val barCorner = unit * 2.8f
        val pillCorner = unit * 2.4f
        val strokeThin = unit * 0.26f
        val strokeActive = unit * 0.42f
        val fontBar: TextUnit = with(density) { (unit * 2.8f).toSp() }
        val fontBtn: TextUnit = with(density) { (unit * 3.5f).toSp() }
        val fontSmall: TextUnit = with(density) { (unit * 2.55f).toSp() }
        val fontTiny: TextUnit = with(density) { (unit * 2.15f).toSp() }

        fun tuningFor(nodeKey: String): PadNodeTuning = nodeTunings[nodeKey] ?: PadNodeTuning()

        fun Modifier.draggableOfficialNode(nodeKey: String): Modifier {
            val t = tuningFor(nodeKey)
            val dxPx = with(density) { (w * t.dxNorm).roundToPx() }
            val dyPx = with(density) { (h * t.dyNorm).roundToPx() }
            val baseMod = this.offset { IntOffset(dxPx, dyPx) }
            if (!isCustomizeMode) return baseMod
            val isSelected = selectedNodeKey == nodeKey
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
                        color = OfficialHudAccent.copy(alpha = 0.78f),
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
                        }
                    )
                }
                .then(
                    if (isPositionLocked) {
                        Modifier
                    } else {
                        Modifier.pointerInput(nodeKey, isCustomizeMode, isPositionLocked) {
                            detectDragGestures(
                                onDragStart = { selectedNodeKey = nodeKey },
                                onDragEnd = { persistCurrentTunings() },
                                onDragCancel = { persistCurrentTunings() }
                            ) { change, dragAmount ->
                                change.consume()
                                selectedNodeKey = nodeKey
                                val curT = nodeTunings[nodeKey] ?: PadNodeTuning()
                                val nextDx = (curT.dxNorm + dragAmount.x / size.width.coerceAtLeast(1).toFloat() * 0.25f)
                                    .coerceIn(-0.30f, 0.30f)
                                val nextDy = (curT.dyNorm + dragAmount.y / size.height.coerceAtLeast(1).toFloat() * 0.25f)
                                    .coerceIn(-0.30f, 0.30f)
                                nodeTunings[nodeKey] = curT.copy(dxNorm = nextDx, dyNorm = nextDy)
                            }
                        }
                    }
                )
        }

        // Lưới căn chỉnh & Tâm ngắm chính thức của VirtualGamepadOverlay khi bật chế độ tinh chỉnh
        if (isCustomizeMode && showEditGrid) {
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

        // === 1. HÀNG NÚT VAI TRÊN CÙNG (Trái: LT, LB, L3 | Phải: R3, RB, RT) chuẩn VirtualGamepadOverlay ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = outerPadH, vertical = outerPadV)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cụm vai trái: LT, LB (vuốt hướng), L3
            Row(
                horizontalArrangement = Arrangement.spacedBy(unit * 1.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ltTuning = tuningFor("btn_lt")
                val ltScale = currentPreset.shoulderBonusScale * ltTuning.scaleMult
                Box(modifier = Modifier.draggableOfficialNode("btn_lt")) {
                    OfficialGamepadTriggerButton(
                        label = "LT",
                        width = w * 0.086f * ltScale,
                        height = h * 0.115f * ltScale,
                        corner = pillCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        opacity = ltTuning.opacity,
                        pressMode = ltTuning.pressMode,
                        turboHz = ltTuning.turboHz,
                        analogDragMode = currentPreset == PadLayoutPreset.RACING,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_lt" },
                        onTriggerValueChanged = { v, justPressed ->
                            if (justPressed) triggerLocalTouchTick(strong = true)
                            PadNetworkClient.updateLeftTrigger(v)
                        },
                        modifier = Modifier.testTag("pad_btn_lt")
                    )
                }

                val lbTuning = tuningFor("btn_lb")
                val lbScale = currentPreset.shoulderBonusScale * lbTuning.scaleMult
                val lbSwipeBit = lbTuning.swipeBit.takeIf { it != 0 } ?: RemotePadButtons.L3
                Box(modifier = Modifier.draggableOfficialNode("btn_lb")) {
                    OfficialGamepadShoulderButton(
                        label = "LB",
                        primaryBit = RemotePadButtons.LB,
                        swipeTargetBit = lbSwipeBit,
                        width = w * 0.080f * lbScale,
                        height = h * 0.115f * lbScale,
                        corner = pillCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        badgeFontSize = fontTiny,
                        opacity = lbTuning.opacity,
                        pressMode = lbTuning.pressMode,
                        turboHz = lbTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_lb" },
                        onHaptic = { triggerLocalTouchTick(it) },
                        modifier = Modifier.testTag("pad_btn_lb")
                    )
                }

                val l3Tuning = tuningFor("btn_l3")
                val l3Scale = l3Tuning.scaleMult
                Box(modifier = Modifier.draggableOfficialNode("btn_l3")) {
                    OfficialGamepadPillButton(
                        label = "L3",
                        bit = RemotePadButtons.L3,
                        width = w * 0.060f * l3Scale,
                        height = h * 0.098f * l3Scale,
                        corner = pillCorner * 0.85f,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = l3Tuning.opacity,
                        pressMode = l3Tuning.pressMode,
                        turboHz = l3Tuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_l3" },
                        onHaptic = { triggerLocalTouchTick(true) },
                        testTag = "pad_btn_l3"
                    )
                }
            }

            // Cụm vai phải: R3, RB (vuốt hướng), RT
            Row(
                horizontalArrangement = Arrangement.spacedBy(unit * 1.6f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val r3Tuning = tuningFor("btn_r3")
                val r3Scale = r3Tuning.scaleMult
                Box(modifier = Modifier.draggableOfficialNode("btn_r3")) {
                    OfficialGamepadPillButton(
                        label = "R3",
                        bit = RemotePadButtons.R3,
                        width = w * 0.060f * r3Scale,
                        height = h * 0.098f * r3Scale,
                        corner = pillCorner * 0.85f,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = r3Tuning.opacity,
                        pressMode = r3Tuning.pressMode,
                        turboHz = r3Tuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_r3" },
                        onHaptic = { triggerLocalTouchTick(true) },
                        testTag = "pad_btn_r3"
                    )
                }

                val rbTuning = tuningFor("btn_rb")
                val rbScale = currentPreset.shoulderBonusScale * rbTuning.scaleMult
                val rbSwipeBit = rbTuning.swipeBit.takeIf { it != 0 } ?: RemotePadButtons.R3
                Box(modifier = Modifier.draggableOfficialNode("btn_rb")) {
                    OfficialGamepadShoulderButton(
                        label = "RB",
                        primaryBit = RemotePadButtons.RB,
                        swipeTargetBit = rbSwipeBit,
                        width = w * 0.080f * rbScale,
                        height = h * 0.115f * rbScale,
                        corner = pillCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        badgeFontSize = fontTiny,
                        opacity = rbTuning.opacity,
                        pressMode = rbTuning.pressMode,
                        turboHz = rbTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_rb" },
                        onHaptic = { triggerLocalTouchTick(it) },
                        modifier = Modifier.testTag("pad_btn_rb")
                    )
                }

                val rtTuning = tuningFor("btn_rt")
                val rtScale = currentPreset.shoulderBonusScale * rtTuning.scaleMult
                Box(modifier = Modifier.draggableOfficialNode("btn_rt")) {
                    OfficialGamepadTriggerButton(
                        label = "RT",
                        width = w * 0.086f * rtScale,
                        height = h * 0.115f * rtScale,
                        corner = pillCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        opacity = rtTuning.opacity,
                        pressMode = rtTuning.pressMode,
                        turboHz = rtTuning.turboHz,
                        analogDragMode = currentPreset == PadLayoutPreset.RACING,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_rt" },
                        onTriggerValueChanged = { v, justPressed ->
                            if (justPressed) triggerLocalTouchTick(strong = true)
                            PadNetworkClient.updateRightTrigger(v)
                        },
                        modifier = Modifier.testTag("pad_btn_rt")
                    )
                }
            }
        }

        // === 2. CỤM TRUNG TÂM TRÊN CÙNG: ⧉ View · ✕ Xbox Nexus · ☰ Menu · ⚙ Tune + Trạng thái TV/Ping ===
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = outerPadV),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(unit * 1.2f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(unit * 1.4f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val viewTuning = tuningFor("btn_view")
                Box(modifier = Modifier.draggableOfficialNode("btn_view")) {
                    OfficialGamepadPillButton(
                        label = "⧉ View",
                        bit = RemotePadButtons.VIEW,
                        width = w * 0.082f * viewTuning.scaleMult,
                        height = h * 0.088f * viewTuning.scaleMult,
                        corner = pillCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = viewTuning.opacity,
                        pressMode = viewTuning.pressMode,
                        turboHz = viewTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_view" },
                        onHaptic = { triggerLocalTouchTick(false) },
                        testTag = "pad_btn_view"
                    )
                }

                val xboxTuning = tuningFor("btn_xbox")
                Box(modifier = Modifier.draggableOfficialNode("btn_xbox")) {
                    OfficialXboxNexusButton(
                        diameter = h * 0.095f * xboxTuning.scaleMult,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        opacity = xboxTuning.opacity,
                        pressMode = xboxTuning.pressMode,
                        turboHz = xboxTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_xbox" },
                        onHaptic = { triggerLocalTouchTick(true) },
                        testTag = "pad_btn_xbox"
                    )
                }

                val menuTuning = tuningFor("btn_menu")
                Box(modifier = Modifier.draggableOfficialNode("btn_menu")) {
                    OfficialGamepadPillButton(
                        label = "☰ Menu",
                        bit = RemotePadButtons.MENU,
                        width = w * 0.082f * menuTuning.scaleMult,
                        height = h * 0.088f * menuTuning.scaleMult,
                        corner = pillCorner,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = menuTuning.opacity,
                        pressMode = menuTuning.pressMode,
                        turboHz = menuTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "btn_menu" },
                        onHaptic = { triggerLocalTouchTick(false) },
                        testTag = "pad_btn_menu"
                    )
                }

                // Nút ⚙ Tinh chỉnh tay cầm chính thức (đồng bộ với virtual_gamepad_tune_button của CloudPlay)
                Box(
                    modifier = Modifier
                        .size(width = w * 0.052f, height = h * 0.088f)
                        .clip(RoundedCornerShape(pillCorner * 0.85f))
                        .background(if (isCustomizeMode) OfficialHudSelBg else OfficialHudSurface2)
                        .border(
                            if (isCustomizeMode) strokeActive else strokeThin,
                            if (isCustomizeMode) OfficialHudAccent else Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(pillCorner * 0.85f)
                        )
                        .clickable {
                            if (!isCustomizeMode) {
                                PadNetworkClient.releaseAllInputsImmediate()
                            } else {
                                persistCurrentTunings()
                            }
                            isCustomizeMode = !isCustomizeMode
                            triggerLocalTouchTick(false)
                        }
                        .testTag("pad_customize_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = PadStrings.tr(lang, "Tinh chỉnh tay cầm", "Customize controller"),
                        tint = if (isCustomizeMode) OfficialHudAccent else OfficialPadBtnText,
                        modifier = Modifier.size(unit * 4.2f)
                    )
                }
            }

            // Thanh trạng thái nhỏ gọn dưới cụm View/Xbox/Menu: Tên TV · Ping thật · Đổi bố cục · Ngắt kết nối
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(pillCorner))
                    .background(OfficialHudSurface.copy(alpha = 0.88f))
                    .border(strokeThin, Color.White.copy(alpha = 0.14f), RoundedCornerShape(pillCorner))
                    .padding(horizontal = unit * 2.0f, vertical = unit * 0.8f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(unit * 1.4f)
            ) {
                Box(
                    modifier = Modifier
                        .size(unit * 1.7f)
                        .clip(CircleShape)
                        .background(OfficialHudAccent)
                )
                val tvTitle = state.selectedTv?.tvName.orEmpty().ifBlank { "Ravno TV" }
                val gameBadge = state.activeGameTitle.trim().takeIf {
                    it.isNotEmpty() && !it.equals("generic", ignoreCase = true)
                }
                Text(
                    text = if (gameBadge != null) "$tvTitle · $gameBadge" else tvTitle,
                    color = OfficialPadBtnText,
                    fontSize = fontTiny,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val pingStr = state.measuredPingMs?.let { "${it} ms" } ?: "-- ms"
                Text(
                    text = "· $pingStr",
                    color = OfficialHudAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontTiny,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("pad_live_ping_text")
                )

                PadTopBarButton(
                    label = currentPreset.localizedTitle(lang),
                    corner = pillCorner * 0.75f,
                    stroke = strokeThin,
                    fontSize = fontTiny,
                    isActive = false,
                    onClick = {
                        val list = PadLayoutPreset.entries
                        val next = list[(list.indexOf(currentPreset) + 1) % list.size]
                        currentPreset = next
                        prefs.edit().putString("active_preset_id", next.id).apply()
                        triggerLocalTouchTick()
                    },
                    testTag = "pad_preset_cycle_btn"
                )

                PadTopBarButton(
                    label = PadStrings.tr(lang, "Ngắt", "Disconnect"),
                    corner = pillCorner * 0.75f,
                    stroke = strokeThin,
                    fontSize = fontTiny,
                    isDanger = true,
                    onClick = onDisconnect,
                    testTag = "pad_controller_disconnect_btn"
                )
            }
        }

        // === 3. CỤM DƯỚI TRÁI: Cần Analog Trái (L) + D-Pad (chữ thập 8 hướng hoặc tách 4 nút) chuẩn VirtualGamepadOverlay ===
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = outerPadH, bottom = outerPadV),
            horizontalArrangement = Arrangement.spacedBy(unit * 3.0f),
            verticalAlignment = Alignment.Bottom
        ) {
            val leftStickTuning = tuningFor("stick_left")
            val leftStickSize = minOf(w * 0.175f, h * 0.42f) *
                currentPreset.stickBonusScale *
                leftStickTuning.scaleMult
            Box(modifier = Modifier.draggableOfficialNode("stick_left")) {
                OfficialVirtualAnalogStick(
                    label = "L",
                    size = leftStickSize,
                    strokeThin = strokeThin,
                    strokeActive = strokeActive,
                    fontSize = fontSmall,
                    tinyFontSize = fontTiny,
                    opacity = leftStickTuning.opacity,
                    autoSprintL3 = currentPreset.autoSprintL3,
                    isEditMode = isCustomizeMode,
                    onSelectInEdit = { selectedNodeKey = "stick_left" },
                    onStickChanged = { x, y, sprintBit ->
                        PadNetworkClient.updateLeftStick(x, y, sprintBit)
                    },
                    onDoubleTapStickClick = { pressed ->
                        if (pressed) triggerLocalTouchTick(true)
                        PadNetworkClient.updateButtonBit(RemotePadButtons.L3, pressed)
                    },
                    modifier = Modifier.testTag("pad_left_stick")
                )
            }

            val dpadTuning = tuningFor("dpad")
            val dpadSize = minOf(w * 0.155f, h * 0.37f) * dpadTuning.scaleMult
            Box(modifier = Modifier.draggableOfficialNode("dpad")) {
                if (!dpadSplitMode) {
                    OfficialVirtualDpadCross(
                        size = dpadSize,
                        corner = unit * 2.6f,
                        keyCorner = unit * 1.6f,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = dpadTuning.opacity,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "dpad" },
                        onHaptic = { triggerLocalTouchTick(false) },
                        modifier = Modifier.testTag("pad_dpad")
                    )
                } else {
                    OfficialSplitDpadCluster(
                        size = dpadSize,
                        keyCorner = unit * 1.8f,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontSmall,
                        opacity = dpadTuning.opacity,
                        pressMode = dpadTuning.pressMode,
                        turboHz = dpadTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "dpad" },
                        onHaptic = { triggerLocalTouchTick(false) },
                        modifier = Modifier.testTag("pad_dpad")
                    )
                }
            }
        }

        // === 4. CỤM DƯỚI PHẢI: Cần Analog Phải (R) + Cụm ABXY màu chuẩn Xbox (trượt tổ hợp hoặc tách 4 nút) ===
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = outerPadH, bottom = outerPadV),
            horizontalArrangement = Arrangement.spacedBy(unit * 3.0f),
            verticalAlignment = Alignment.Bottom
        ) {
            val rightStickTuning = tuningFor("stick_right")
            val rightStickSize = minOf(w * 0.165f, h * 0.40f) *
                currentPreset.rightStickBonusScale *
                rightStickTuning.scaleMult
            Box(modifier = Modifier.draggableOfficialNode("stick_right")) {
                OfficialVirtualAnalogStick(
                    label = "R",
                    size = rightStickSize,
                    strokeThin = strokeThin,
                    strokeActive = strokeActive,
                    fontSize = fontSmall,
                    tinyFontSize = fontTiny,
                    opacity = (rightStickTuning.opacity * 0.94f).coerceIn(0.25f, 1.0f),
                    autoSprintL3 = false,
                    isEditMode = isCustomizeMode,
                    onSelectInEdit = { selectedNodeKey = "stick_right" },
                    onStickChanged = { x, y, _ ->
                        PadNetworkClient.updateRightStick(x, y)
                    },
                    onDoubleTapStickClick = { pressed ->
                        if (pressed) triggerLocalTouchTick(true)
                        PadNetworkClient.updateButtonBit(RemotePadButtons.R3, pressed)
                    },
                    modifier = Modifier.testTag("pad_right_stick")
                )
            }

            val abxyTuning = tuningFor("abxy")
            val abxySize = minOf(w * 0.185f, h * 0.44f) *
                currentPreset.abxyBonusScale *
                abxyTuning.scaleMult
            Box(modifier = Modifier.draggableOfficialNode("abxy")) {
                if (!abxySplitMode) {
                    OfficialVirtualAbxyPad(
                        size = abxySize,
                        swapSwitchAbxy = currentPreset.swapSwitchAbxy,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        opacity = abxyTuning.opacity,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "abxy" },
                        onHaptic = { triggerLocalTouchTick(false) },
                        modifier = Modifier.testTag("pad_abxy_cluster")
                    )
                } else {
                    OfficialSplitAbxyCluster(
                        size = abxySize,
                        swapSwitchAbxy = currentPreset.swapSwitchAbxy,
                        strokeThin = strokeThin,
                        strokeActive = strokeActive,
                        fontSize = fontBtn,
                        opacity = abxyTuning.opacity,
                        pressMode = abxyTuning.pressMode,
                        turboHz = abxyTuning.turboHz,
                        isEditMode = isCustomizeMode,
                        onSelectInEdit = { selectedNodeKey = "abxy" },
                        onHaptic = { triggerLocalTouchTick(false) },
                        modifier = Modifier.testTag("pad_abxy_cluster")
                    )
                }
            }
        }

        // === 5. HỘP CÔNG CỤ TINH CHỈNH TAY CẦM CHÍNH THỨC KHI BẬT ⚙ (Chỉnh nút) ===
        if (isCustomizeMode) {
            val curTuning = tuningFor(selectedNodeKey)
            val swipeLabel = when (curTuning.swipeBit) {
                RemotePadButtons.L3 -> "Vuốt → L3"
                RemotePadButtons.R3 -> "Vuốt → R3"
                RemotePadButtons.Y -> "Vuốt → Y"
                RemotePadButtons.A -> "Vuốt → A"
                RemotePadButtons.X -> "Vuốt → X"
                RemotePadButtons.B -> "Vuốt → B"
                else -> PadStrings.tr(lang, "Vuốt vai: Mặc định", "Swipe: Default")
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(w * 0.68f)
                    .clip(RoundedCornerShape(barCorner))
                    .background(OfficialHudSurface.copy(alpha = 0.96f))
                    .border(strokeThin, OfficialHudAccent, RoundedCornerShape(barCorner))
                    .padding(horizontal = unit * 2.6f, vertical = unit * 1.8f)
                    .testTag("pad_customize_toolbar"),
                verticalArrangement = Arrangement.spacedBy(unit * 1.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PadStrings.tr(
                            lang = lang,
                            vi = "Đang chọn [$selectedNodeKey] · Cỡ ${(curTuning.scaleMult * 100).roundToInt()}% · Mờ ${(curTuning.opacity * 100).roundToInt()}%",
                            en = "Selected [$selectedNodeKey] · Size ${(curTuning.scaleMult * 100).roundToInt()}% · Opacity ${(curTuning.opacity * 100).roundToInt()}%"
                        ),
                        color = OfficialPadBtnText,
                        fontSize = fontSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(unit * 1.2f)) {
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "# Lưới", "# Grid"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            isActive = showEditGrid,
                            onClick = {
                                showEditGrid = !showEditGrid
                                prefs.edit().putBoolean("pad_show_edit_grid", showEditGrid).apply()
                            },
                            testTag = "virtual_gamepad_grid_toggle"
                        )
                        PadTopBarButton(
                            label = if (isPositionLocked) {
                                PadStrings.tr(lang, "🔒 Đã khóa vị trí", "🔒 Locked")
                            } else {
                                PadStrings.tr(lang, "Khóa vị trí", "Lock Pos")
                            },
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            isActive = isPositionLocked,
                            onClick = {
                                isPositionLocked = !isPositionLocked
                                prefs.edit().putBoolean("pad_position_locked", isPositionLocked).apply()
                            },
                            testTag = "pad_lock_position_btn"
                        )
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "✓ Xong", "✓ Done"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            isActive = true,
                            onClick = {
                                persistCurrentTunings()
                                isCustomizeMode = false
                            },
                            testTag = "pad_customize_done_btn"
                        )
                    }
                }

                // Hàng nút chỉnh Cỡ, Độ mờ, Chế độ bấm (Bấm thả/Chạm khóa/Turbo), Vuốt vai, Tách cụm, Đặt lại
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(unit * 1.0f)) {
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "Cỡ −", "Size −"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            onClick = {
                                val next = (curTuning.scaleMult - 0.08f).coerceIn(0.60f, 1.65f)
                                nodeTunings[selectedNodeKey] = curTuning.copy(scaleMult = next)
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_scale_down_btn"
                        )
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "Cỡ +", "Size +"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            onClick = {
                                val next = (curTuning.scaleMult + 0.08f).coerceIn(0.60f, 1.65f)
                                nodeTunings[selectedNodeKey] = curTuning.copy(scaleMult = next)
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_scale_up_btn"
                        )
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "Mờ −", "Alpha −"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            onClick = {
                                val next = (curTuning.opacity - 0.10f).coerceIn(0.25f, 1.0f)
                                nodeTunings[selectedNodeKey] = curTuning.copy(opacity = next)
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_opacity_down_btn"
                        )
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "Mờ +", "Alpha +"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            onClick = {
                                val next = (curTuning.opacity + 0.10f).coerceIn(0.25f, 1.0f)
                                nodeTunings[selectedNodeKey] = curTuning.copy(opacity = next)
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_opacity_up_btn"
                        )
                        PadTopBarButton(
                            label = curTuning.pressMode.localizedTitle(lang),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            isActive = curTuning.pressMode != PadPressMode.NORMAL,
                            onClick = {
                                nodeTunings[selectedNodeKey] = curTuning.copy(pressMode = curTuning.pressMode.next())
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_press_mode_btn"
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(unit * 1.0f)) {
                        if (selectedNodeKey == "btn_lb" || selectedNodeKey == "btn_rb") {
                            PadTopBarButton(
                                label = swipeLabel,
                                corner = pillCorner,
                                stroke = strokeThin,
                                fontSize = fontTiny,
                                onClick = {
                                    val cycle = listOf(
                                        0,
                                        RemotePadButtons.L3,
                                        RemotePadButtons.R3,
                                        RemotePadButtons.Y,
                                        RemotePadButtons.A,
                                        RemotePadButtons.X,
                                        RemotePadButtons.B
                                    )
                                    val nextSwipe = cycle[(cycle.indexOf(curTuning.swipeBit) + 1) % cycle.size]
                                    nodeTunings[selectedNodeKey] = curTuning.copy(swipeBit = nextSwipe)
                                    persistCurrentTunings()
                                },
                                testTag = "pad_customize_swipe_btn"
                            )
                        }
                        if (selectedNodeKey == "dpad") {
                            PadTopBarButton(
                                label = if (dpadSplitMode) {
                                    PadStrings.tr(lang, "Gộp D-Pad", "Merge D-Pad")
                                } else {
                                    PadStrings.tr(lang, "Tách 4 nút D-Pad", "Split D-Pad")
                                },
                                corner = pillCorner,
                                stroke = strokeThin,
                                fontSize = fontTiny,
                                isActive = dpadSplitMode,
                                onClick = {
                                    dpadSplitMode = !dpadSplitMode
                                    prefs.edit().putBoolean("pad_dpad_split_mode", dpadSplitMode).apply()
                                },
                                testTag = "pad_customize_split_dpad_btn"
                            )
                        }
                        if (selectedNodeKey == "abxy") {
                            PadTopBarButton(
                                label = if (abxySplitMode) {
                                    PadStrings.tr(lang, "Gộp cụm ABXY", "Merge ABXY")
                                } else {
                                    PadStrings.tr(lang, "Tách rời A/B/X/Y", "Split ABXY")
                                },
                                corner = pillCorner,
                                stroke = strokeThin,
                                fontSize = fontTiny,
                                isActive = abxySplitMode,
                                onClick = {
                                    abxySplitMode = !abxySplitMode
                                    prefs.edit().putBoolean("pad_abxy_split_mode", abxySplitMode).apply()
                                },
                                testTag = "pad_customize_split_abxy_btn"
                            )
                        }
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "Đặt lại", "Reset"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontTiny,
                            isDanger = true,
                            onClick = {
                                nodeTunings.clear()
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_reset_btn"
                        )
                    }
                }
            }
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
        else -> Color.White.copy(alpha = 0.22f)
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
 * Nút cò LT / RT chính thức của CloudPlay (hỗ trợ Bấm thả / Chạm khóa / Nhấp nhanh Turbo + kéo dọc điều tiết cò).
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

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            activeVal = 0f
            currentOnTrigger(0f, false)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) {
                activeVal = 0f
                currentOnTrigger(0f, false)
            }
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                activeVal = 1.0f
                currentOnTrigger(1.0f, false)
                delay(halfPeriodMs)
                activeVal = 0f
                currentOnTrigger(0f, false)
                delay(halfPeriodMs)
            }
            activeVal = 0f
            currentOnTrigger(0f, false)
        }
    }

    val isPressedVisual = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown || activeVal > 0.05f
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown || activeVal > 0.05f
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isPressedVisual) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.85f))
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isPressedVisual) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isPressedVisual -> OfficialHudAccent
                    else -> Color.White.copy(alpha = borderAlpha)
                },
                RoundedCornerShape(corner)
            )
            .pointerInput(isEditMode, pressMode, analogDragMode) {
                if (isEditMode) {
                    awaitEachGesture {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    fingerDown = true
                    val startY = down.position.y

                    when (pressMode) {
                        PadPressMode.NORMAL -> {
                            val initV = if (analogDragMode) 0.35f else 1.0f
                            activeVal = initV
                            currentOnTrigger(initV, true)
                        }
                        PadPressMode.TOGGLE_HOLD -> {
                            latched = !latched
                            activeVal = if (latched) 1.0f else 0f
                            currentOnTrigger(activeVal, latched)
                        }
                        PadPressMode.TURBO -> {
                            currentOnTrigger(1.0f, true)
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
                                val v = ((dy / hPx) + 0.65f).coerceIn(0.20f, 1.0f)
                                activeVal = v
                                if (pressMode == PadPressMode.NORMAL || (pressMode == PadPressMode.TOGGLE_HOLD && latched)) {
                                    currentOnTrigger(v, false)
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    fingerDown = false
                    if (pressMode == PadPressMode.NORMAL) {
                        activeVal = 0f
                        currentOnTrigger(0f, false)
                    }
                }
            },
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
                else -> OfficialPadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun badgeForSwipeBit(bit: Int): String = when (bit) {
    RemotePadButtons.L3 -> "L3"
    RemotePadButtons.R3 -> "R3"
    RemotePadButtons.Y -> "Y"
    RemotePadButtons.A -> "A"
    RemotePadButtons.X -> "X"
    RemotePadButtons.B -> "B"
    else -> ""
}

/**
 * Nút vai LB / RB chính thức của CloudPlay (hỗ trợ vuốt hướng kích hoạt thêm nút phụ + hiển thị nhãn góc như VirtualGamepadOverlay).
 */
@Composable
private fun OfficialGamepadShoulderButton(
    label: String,
    primaryBit: Int,
    swipeTargetBit: Int,
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
    var isSwiped by remember { mutableStateOf(false) }

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            isSwiped = false
            PadNetworkClient.updateButtonBit(primaryBit, false)
            if (swipeTargetBit != 0) PadNetworkClient.updateButtonBit(swipeTargetBit, false)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonBit(primaryBit, false)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                PadNetworkClient.updateButtonBit(primaryBit, true)
                delay(halfPeriodMs)
                PadNetworkClient.updateButtonBit(primaryBit, false)
                delay(halfPeriodMs)
            }
            PadNetworkClient.updateButtonBit(primaryBit, false)
        }
    }

    val isVisualActive = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown || isSwiped
        PadPressMode.TOGGLE_HOLD -> latched || isSwiped
        PadPressMode.TURBO -> fingerDown || isSwiped
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)
    val swipeBadge = badgeForSwipeBit(swipeTargetBit)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isVisualActive) {
                    OfficialHudAccent.copy(alpha = 0.26f)
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isVisualActive) strokeActive else strokeThin,
                when {
                    isSwiped || latched -> OfficialHudCyan
                    isVisualActive -> OfficialHudAccent
                    else -> Color.White.copy(alpha = borderAlpha)
                },
                RoundedCornerShape(corner)
            )
            .pointerInput(isEditMode, primaryBit, swipeTargetBit, pressMode) {
                if (isEditMode) {
                    awaitEachGesture {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    val startPos = down.position
                    var swipeLatched = false
                    fingerDown = true
                    onHaptic(false)

                    when (pressMode) {
                        PadPressMode.NORMAL -> PadNetworkClient.updateButtonBit(primaryBit, true)
                        PadPressMode.TOGGLE_HOLD -> {
                            latched = !latched
                            PadNetworkClient.updateButtonBit(primaryBit, latched)
                        }
                        PadPressMode.TURBO -> {}
                    }

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            change.consume()
                            val dist = hypot(
                                change.position.x - startPos.x,
                                change.position.y - startPos.y
                            )
                            val threshold = size.height.coerceAtLeast(24) * 0.42f
                            if (!swipeLatched && dist >= threshold && swipeTargetBit != 0) {
                                swipeLatched = true
                                isSwiped = true
                                onHaptic(true)
                                PadNetworkClient.updateButtonBit(swipeTargetBit, true)
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    fingerDown = false
                    if (pressMode == PadPressMode.NORMAL) {
                        PadNetworkClient.updateButtonBit(primaryBit, false)
                    }
                    if (swipeLatched) {
                        isSwiped = false
                        PadNetworkClient.updateButtonBit(swipeTargetBit, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (swipeBadge.isNotEmpty()) {
            Text(
                text = "▾$swipeBadge",
                color = if (isSwiped) OfficialHudCyan else OfficialPadBtnText.copy(alpha = (opacity * 0.55f).coerceIn(0.20f, 0.75f)),
                fontSize = badgeFontSize,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = corner * 0.35f, top = corner * 0.12f)
            )
        }
        val textLabel = when {
            isSwiped && swipeBadge.isNotEmpty() -> "$label+$swipeBadge"
            latched -> "$label •"
            else -> label
        }
        Text(
            text = textLabel,
            color = when {
                isSwiped || latched -> OfficialHudCyan
                isVisualActive -> OfficialHudAccent
                else -> OfficialPadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
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

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            PadNetworkClient.updateButtonBit(bit, false)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonBit(bit, false)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                PadNetworkClient.updateButtonBit(bit, true)
                delay(halfPeriodMs)
                PadNetworkClient.updateButtonBit(bit, false)
                delay(halfPeriodMs)
            }
            PadNetworkClient.updateButtonBit(bit, false)
        }
    }

    val isVisualActive = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isVisualActive) {
                    OfficialHudAccent.copy(alpha = 0.26f)
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isVisualActive) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isVisualActive -> OfficialHudAccent
                    else -> Color.White.copy(alpha = borderAlpha)
                },
                RoundedCornerShape(corner)
            )
            .pointerInput(isEditMode, bit, pressMode) {
                if (isEditMode) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    fingerDown = true
                    onHaptic()
                    when (pressMode) {
                        PadPressMode.NORMAL -> PadNetworkClient.updateButtonBit(bit, true)
                        PadPressMode.TOGGLE_HOLD -> {
                            latched = !latched
                            PadNetworkClient.updateButtonBit(bit, latched)
                        }
                        PadPressMode.TURBO -> {}
                    }
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { if (it.pressed) it.consume() }
                    } while (event.changes.any { it.pressed })

                    fingerDown = false
                    if (pressMode == PadPressMode.NORMAL) {
                        PadNetworkClient.updateButtonBit(bit, false)
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (latched) "$label •" else label,
            color = when {
                latched -> OfficialHudCyan
                isVisualActive -> OfficialHudAccent
                else -> OfficialPadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f))
            },
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Nút tròn ✕ Xbox Nexus chính thức (đồng bộ với GamepadXboxNexusButton trong VirtualGamepadControls.kt).
 */
@Composable
private fun OfficialXboxNexusButton(
    diameter: Dp,
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

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            PadNetworkClient.updateButtonBit(RemotePadButtons.XBOX, false)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonBit(RemotePadButtons.XBOX, false)
        }
    }

    val isVisualActive = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                if (isVisualActive) {
                    OfficialHudAccent.copy(alpha = 0.28f)
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isVisualActive) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isVisualActive -> OfficialHudAccent
                    else -> OfficialHudAccent.copy(alpha = opacity.coerceIn(0.35f, 0.95f))
                },
                CircleShape
            )
            .pointerInput(isEditMode, pressMode, turboHz) {
                if (isEditMode) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    fingerDown = true
                    onHaptic()
                    when (pressMode) {
                        PadPressMode.NORMAL, PadPressMode.TURBO -> PadNetworkClient.updateButtonBit(RemotePadButtons.XBOX, true)
                        PadPressMode.TOGGLE_HOLD -> {
                            latched = !latched
                            PadNetworkClient.updateButtonBit(RemotePadButtons.XBOX, latched)
                        }
                    }
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { if (it.pressed) it.consume() }
                    } while (event.changes.any { it.pressed })

                    fingerDown = false
                    if (pressMode != PadPressMode.TOGGLE_HOLD) {
                        PadNetworkClient.updateButtonBit(RemotePadButtons.XBOX, false)
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕",
            color = when {
                latched -> OfficialHudCyan
                isVisualActive -> Color.White
                else -> OfficialHudAccent.copy(alpha = opacity.coerceIn(0.40f, 1.0f))
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Cụm D-Pad chữ thập 8 hướng chính thức (VirtualDpadCross của CloudPlay: khung bo góc, 4 phím ▲ ▼ ◀ ▶ và chấm tròn trung tâm).
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

    val containerBgAlpha = (opacity * 0.48f).coerceIn(0.10f, 0.65f)
    val borderAlpha = (opacity * 0.24f).coerceIn(0.06f, 0.40f)
    val btnSize = size * 0.33f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(OfficialHudSurface.copy(alpha = containerBgAlpha))
            .border(strokeThin, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(corner))
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
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
            },
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
                        Color.White.copy(alpha = (opacity * 0.25f).coerceIn(0.08f, 0.40f))
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
    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .padding(sizeDp * 0.05f)
            .clip(RoundedCornerShape(corner))
            .background(
                if (isPressed) {
                    OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.85f))
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressed) strokeActive else strokeThin,
                color = if (isPressed) OfficialHudAccent else Color.White.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(corner)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isPressed) OfficialHudAccent else OfficialPadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f)),
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun OfficialSplitDpadCluster(
    size: Dp,
    keyCorner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
    isEditMode: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keySize = size * 0.35f
    Box(modifier = modifier.size(size)) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            OfficialGamepadPillButton(
                label = "▲",
                bit = RemotePadButtons.DPAD_UP,
                width = keySize,
                height = keySize,
                corner = keyCorner,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic,
                testTag = "pad_dpad_up"
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            OfficialGamepadPillButton(
                label = "▼",
                bit = RemotePadButtons.DPAD_DOWN,
                width = keySize,
                height = keySize,
                corner = keyCorner,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic,
                testTag = "pad_dpad_down"
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            OfficialGamepadPillButton(
                label = "◀",
                bit = RemotePadButtons.DPAD_LEFT,
                width = keySize,
                height = keySize,
                corner = keyCorner,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic,
                testTag = "pad_dpad_left"
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            OfficialGamepadPillButton(
                label = "▶",
                bit = RemotePadButtons.DPAD_RIGHT,
                width = keySize,
                height = keySize,
                corner = keyCorner,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic,
                testTag = "pad_dpad_right"
            )
        }
    }
}

/**
 * Cụm 4 nút kim cương Y / X / B / A chính thức (VirtualAbxyPad của CloudPlay: khung tròn mờ, 4 nút màu Xbox chuẩn, trượt ngón cái bấm tổ hợp).
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

    val containerBgAlpha = (opacity * 0.42f).coerceIn(0.08f, 0.60f)
    val borderAlpha = (opacity * 0.22f).coerceIn(0.06f, 0.36f)
    val btnDiam = size * 0.35f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(OfficialHudSurface.copy(alpha = containerBgAlpha))
            .border(strokeThin, Color.White.copy(alpha = borderAlpha), CircleShape)
            .pointerInput(isEditMode, swapSwitchAbxy) {
                if (isEditMode) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
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
            },
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
    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = modifier
            .size(diameter)
            .padding(diameter * 0.04f)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.28f)
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                width = if (isPressed) strokeActive else strokeThin,
                color = if (isPressed) accentColor else Color.White.copy(alpha = borderAlpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor.copy(alpha = opacity.coerceIn(0.30f, 1.0f)),
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun OfficialSplitAbxyCluster(
    size: Dp,
    swapSwitchAbxy: Boolean,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    opacity: Float,
    pressMode: PadPressMode,
    turboHz: Int,
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

    val btnDiam = size * 0.35f
    Box(modifier = modifier.size(size)) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            OfficialStandaloneColoredAbxyButton(
                label = topLabel,
                accentColor = topColor,
                bit = topBit,
                diameter = btnDiam,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            OfficialStandaloneColoredAbxyButton(
                label = bottomLabel,
                accentColor = bottomColor,
                bit = bottomBit,
                diameter = btnDiam,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            OfficialStandaloneColoredAbxyButton(
                label = leftLabel,
                accentColor = leftColor,
                bit = leftBit,
                diameter = btnDiam,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            OfficialStandaloneColoredAbxyButton(
                label = rightLabel,
                accentColor = rightColor,
                bit = rightBit,
                diameter = btnDiam,
                strokeThin = strokeThin,
                strokeActive = strokeActive,
                fontSize = fontSize,
                opacity = opacity,
                pressMode = pressMode,
                turboHz = turboHz,
                isEditMode = isEditMode,
                onSelectInEdit = onSelectInEdit,
                onHaptic = onHaptic
            )
        }
    }
}

@Composable
private fun OfficialStandaloneColoredAbxyButton(
    label: String,
    accentColor: Color,
    bit: Int,
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

    LaunchedEffect(isEditMode, pressMode) {
        if (isEditMode) {
            fingerDown = false
            latched = false
            PadNetworkClient.updateButtonBit(bit, false)
        } else if (pressMode != PadPressMode.TOGGLE_HOLD && latched) {
            latched = false
            if (!fingerDown) PadNetworkClient.updateButtonBit(bit, false)
        }
    }

    LaunchedEffect(fingerDown, pressMode, turboHz, isEditMode) {
        if (isEditMode) return@LaunchedEffect
        if (pressMode == PadPressMode.TURBO && fingerDown) {
            val halfPeriodMs = (500L / turboHz.coerceIn(5, 20)).coerceAtLeast(22L)
            while (fingerDown) {
                PadNetworkClient.updateButtonBit(bit, true)
                delay(halfPeriodMs)
                PadNetworkClient.updateButtonBit(bit, false)
                delay(halfPeriodMs)
            }
            PadNetworkClient.updateButtonBit(bit, false)
        }
    }

    val isPressed = when (pressMode) {
        PadPressMode.NORMAL -> fingerDown
        PadPressMode.TOGGLE_HOLD -> latched
        PadPressMode.TURBO -> fingerDown
    }

    val bgAlpha = (opacity * 0.72f).coerceIn(0.16f, 0.90f)
    val borderAlpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f)

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    accentColor.copy(alpha = 0.28f)
                } else {
                    OfficialHudSurface.copy(alpha = bgAlpha)
                }
            )
            .border(
                if (isPressed) strokeActive else strokeThin,
                when {
                    latched -> OfficialHudCyan
                    isPressed -> accentColor
                    else -> Color.White.copy(alpha = borderAlpha)
                },
                CircleShape
            )
            .pointerInput(isEditMode, bit, pressMode) {
                if (isEditMode) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    down.consume()
                    fingerDown = true
                    onHaptic()
                    when (pressMode) {
                        PadPressMode.NORMAL -> PadNetworkClient.updateButtonBit(bit, true)
                        PadPressMode.TOGGLE_HOLD -> {
                            latched = !latched
                            PadNetworkClient.updateButtonBit(bit, latched)
                        }
                        PadPressMode.TURBO -> {}
                    }
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { if (it.pressed) it.consume() }
                    } while (event.changes.any { it.pressed })

                    fingerDown = false
                    if (pressMode == PadPressMode.NORMAL) {
                        PadNetworkClient.updateButtonBit(bit, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (latched) "$label•" else label,
            color = when {
                latched -> OfficialHudCyan
                isPressed -> Color.White
                else -> accentColor.copy(alpha = opacity.coerceIn(0.30f, 1.0f))
            },
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Cần gạt Analog chính thức (VirtualAnalogStick của CloudPlay: vòng vùng chết trong/ngoài, tia vector nối tâm,
 * núm trắng mờ và khóa tự động chạy nhanh 🏃 L3 khi vuốt vượt vành trên).
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

    val idleBorder = Color.White.copy(alpha = (opacity * 0.28f).coerceIn(0.08f, 0.45f))
    val outerBgAlpha = (opacity * 0.54f).coerceIn(0.12f, 0.72f)
    val knobBgAlpha = (opacity * 0.30f).coerceIn(0.08f, 0.48f)
    val knobBorderAlpha = (opacity * 0.40f).coerceIn(0.10f, 0.56f)

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
            .pointerInput(isEditMode, autoSprintL3, maxRadiusPx) {
                if (isEditMode) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
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
                        val nx = if (abs(rawX) < 0.06f) 0f else rawX
                        val ny = if (abs(rawY) < 0.06f) 0f else rawY

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
            },
        contentAlignment = Alignment.Center
    ) {
        // Vòng vùng chết ngoài & trong + Tia vector nối tâm chuẩn VirtualAnalogStick
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = maxRadiusPx * 0.95f,
                center = c,
                style = Stroke(width = strokeThin.toPx())
            )
            drawCircle(
                color = OfficialHudCyan.copy(alpha = 0.22f),
                radius = maxRadiusPx * 0.12f,
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

        // Núm gạt tròn trắng mờ chuẩn VirtualAnalogStick
        Box(
            modifier = Modifier
                .offset { IntOffset(stickPxX.roundToInt(), stickPxY.roundToInt()) }
                .size(knobSizeDp)
                .clip(CircleShape)
                .background(
                    when {
                        isAutoSprinting -> Color(0xFF10313A).copy(alpha = opacity.coerceAtLeast(0.85f))
                        isTouching -> OfficialHudSelBg.copy(alpha = opacity.coerceAtLeast(0.85f))
                        else -> Color.White.copy(alpha = knobBgAlpha)
                    }
                )
                .border(
                    width = if (isTouching) strokeActive else strokeThin,
                    color = when {
                        isAutoSprinting -> OfficialHudCyan
                        isTouching -> OfficialHudAccent
                        else -> Color.White.copy(alpha = knobBorderAlpha)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = OfficialPadBtnText.copy(alpha = opacity.coerceIn(0.25f, 1.0f)),
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize
            )
        }
    }
}

private fun loadPresetTunings(
    prefs: android.content.SharedPreferences,
    presetId: String
): Map<String, PadNodeTuning> {
    return try {
        val raw = prefs.getString("tunings_$presetId", "{}").orEmpty()
        val root = JSONObject(raw)
        val out = LinkedHashMap<String, PadNodeTuning>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val obj = root.optJSONObject(k) ?: continue
            val modeId = obj.optString("pm", PadPressMode.NORMAL.id)
            val mode = PadPressMode.entries.firstOrNull { it.id == modeId } ?: PadPressMode.NORMAL
            out[k] = PadNodeTuning(
                dxNorm = obj.optDouble("dx", 0.0).toFloat().coerceIn(-0.30f, 0.30f),
                dyNorm = obj.optDouble("dy", 0.0).toFloat().coerceIn(-0.30f, 0.30f),
                scaleMult = obj.optDouble("sc", 1.0).toFloat().coerceIn(0.60f, 1.65f),
                opacity = obj.optDouble("op", 0.85).toFloat().coerceIn(0.25f, 1.0f),
                swipeBit = obj.optInt("sw", 0),
                pressMode = mode,
                turboHz = obj.optInt("hz", 10).coerceIn(5, 20)
            )
        }
        out
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun savePresetTunings(
    prefs: android.content.SharedPreferences,
    presetId: String,
    map: Map<String, PadNodeTuning>
) {
    try {
        val root = JSONObject()
        for ((k, v) in map) {
            root.put(
                k,
                JSONObject().apply {
                    put("dx", v.dxNorm.toDouble())
                    put("dy", v.dyNorm.toDouble())
                    put("sc", v.scaleMult.toDouble())
                    put("op", v.opacity.toDouble())
                    put("sw", v.swipeBit)
                    put("pm", v.pressMode.id)
                    put("hz", v.turboHz)
                }
            )
        }
        prefs.edit().putString("tunings_$presetId", root.toString()).apply()
    } catch (_: Exception) {
    }
}
