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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Các mẫu bố cục tay cầm theo game (thừa hưởng từ tay cầm ảo của CloudPlay):
 * - Chuẩn (Standard Xbox)
 * - Bắn súng (FPS Pro: cò/nút vai lớn hơn + tự chạy nhanh L3 khi đẩy kịch cần trái)
 * - Hành động (Action: cụm ABXY và cần phải rộng rãi)
 * - Đua xe (Racing: cò LT/RT nhạy tuyến tính theo chiều dọc, cần trái lớn)
 * - Đảo nút (Switch A↔B, X↔Y)
 */
enum class PadLayoutPreset(
    val id: String,
    val shoulderBonusScale: Float,
    val abxyBonusScale: Float,
    val stickBonusScale: Float,
    val autoSprintL3: Boolean,
    val swapSwitchAbxy: Boolean
) {
    STANDARD("standard", 1.0f, 1.0f, 1.0f, true, false),
    FPS_PRO("fps", 1.15f, 1.05f, 1.08f, true, false),
    ACTION("action", 1.05f, 1.14f, 1.04f, true, false),
    RACING("racing", 1.18f, 0.96f, 1.12f, false, false),
    SWITCH_SWAP("switch", 1.0f, 1.0f, 1.0f, true, true);

    fun localizedTitle(lang: PadLanguage): String = when (this) {
        STANDARD -> PadStrings.tr(lang, "Bố cục: Chuẩn", "Layout: Standard")
        FPS_PRO -> PadStrings.tr(lang, "Bố cục: Bắn súng", "Layout: Shooter")
        ACTION -> PadStrings.tr(lang, "Bố cục: Hành động", "Layout: Action")
        RACING -> PadStrings.tr(lang, "Bố cục: Đua xe", "Layout: Racing")
        SWITCH_SWAP -> PadStrings.tr(lang, "Bố cục: Đảo nút", "Layout: Swapped")
    }
}

private data class PadNodeTuning(
    val dxNorm: Float = 0f,
    val dyNorm: Float = 0f,
    val scaleMult: Float = 1.0f,
    val swipeBit: Int = 0
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
 * Màn hình Điện thoại 3 (#s5 trong remote-pad-mockup.html):
 * - Luôn xoay ngang cố định (SCREEN_ORIENTATION_SENSOR_LANDSCAPE), giữ màn hình luôn sáng,
 *   ẩn thanh điều hướng/trạng thái hệ thống và chặn vuốt cạnh thoát nhầm trong lúc chơi.
 * - Đa điểm độc lập cho từng cụm: 2 cò LT/RT (hỗ trợ kéo analog), 2 nút vai LB/RB (hỗ trợ vuốt hướng kích hoạt nhanh L3/R3),
 *   D-Pad 8 hướng, 2 cần Analog (cần trái hỗ trợ đẩy kịch tự chạy nhanh L3), View/Xbox/Menu, L3/R3, và cụm Y/X/B/A (hỗ trợ trượt ngón cái bấm tổ hợp).
 * - Chế độ "Chỉnh nút": kéo vị trí từng cụm/nút, tăng giảm kích thước riêng, đổi nút vuốt LB/RB, hoặc đặt lại mặc định.
 * - Mất kết nối hoặc vào nền (ON_PAUSE / ON_STOP) lập tức nhả toàn bộ nút.
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
    var isCustomizeMode by remember { mutableStateOf(false) }
    var selectedNodeKey by remember { mutableStateOf("abxy") }
    var hapticEnabled by remember {
        mutableStateOf(prefs.getBoolean("pad_touch_haptic", true))
    }

    val nodeTunings = remember(currentPreset) {
        mutableStateMapOf<String, PadNodeTuning>().apply {
            putAll(loadPresetTunings(prefs, currentPreset.id))
        }
    }

    fun persistCurrentTunings() {
        savePresetTunings(prefs, currentPreset.id, nodeTunings)
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
                        if (strong) VibrationEffect.EFFECT_CLICK else VibrationEffect.EFFECT_TICK
                    )
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 18L else 10L, if (strong) 140 else 80))
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
        val outerPadV = h * 0.028f
        val barCorner = unit * 2.8f
        val pillCorner = unit * 2.4f
        val strokeThin = unit * 0.30f
        val strokeActive = unit * 0.52f
        val fontBar: TextUnit = with(density) { (unit * 3.1f).toSp() }
        val fontBtn: TextUnit = with(density) { (unit * 3.8f).toSp() }
        val fontSmall: TextUnit = with(density) { (unit * 2.7f).toSp() }

        fun tuningFor(nodeKey: String): PadNodeTuning = nodeTunings[nodeKey] ?: PadNodeTuning()

        fun Modifier.customizableNode(nodeKey: String): Modifier {
            val t = tuningFor(nodeKey)
            val dxPx = with(density) { (w * t.dxNorm).roundToPx() }
            val dyPx = with(density) { (h * t.dyNorm).roundToPx() }
            val baseMod = this.offset { IntOffset(dxPx, dyPx) }
            if (!isCustomizeMode) return baseMod
            val isSelected = selectedNodeKey == nodeKey
            return baseMod
                .border(
                    width = if (isSelected) strokeActive else strokeThin,
                    color = if (isSelected) PadAccent else PadWarn,
                    shape = RoundedCornerShape(pillCorner)
                )
                .pointerInput(nodeKey, isCustomizeMode) {
                    detectDragGestures(
                        onDragStart = {
                            selectedNodeKey = nodeKey
                        },
                        onDragEnd = {
                            persistCurrentTunings()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val curT = nodeTunings[nodeKey] ?: PadNodeTuning()
                        val nextDx = (curT.dxNorm + dragAmount.x / size.width.coerceAtLeast(1).toFloat() * 0.25f)
                            .coerceIn(-0.22f, 0.22f)
                        val nextDy = (curT.dyNorm + dragAmount.y / size.height.coerceAtLeast(1).toFloat() * 0.25f)
                            .coerceIn(-0.22f, 0.22f)
                        nodeTunings[nodeKey] = curT.copy(dxNorm = nextDx, dyNorm = nextDy)
                    }
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = outerPadH, vertical = outerPadV),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Thanh trạng thái trên cùng (#s5 .pad-bar trong remote-pad-mockup.html)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.11f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tên TV + Ping thực tế
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(unit * 1.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(unit * 2.0f)
                            .clip(CircleShape)
                            .background(PadAccent)
                    )
                    Text(
                        text = state.selectedTv?.tvName.orEmpty().ifBlank { "CloudPlay TV" },
                        color = PadInk,
                        fontSize = fontBar,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pingStr = state.measuredPingMs?.let { "${it} ms" } ?: "-- ms"
                    Text(
                        text = "· $pingStr",
                        color = PadAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontBar,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("pad_live_ping_text")
                    )
                }

                // Các nút điều khiển thanh trên: Bố cục theo game, Rung chạm, Chỉnh nút, Ngắt kết nối
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(unit * 1.4f)
                ) {
                    PadTopBarButton(
                        label = currentPreset.localizedTitle(lang),
                        corner = pillCorner,
                        stroke = strokeThin,
                        fontSize = fontSmall,
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
                        label = if (isCustomizeMode) {
                            PadStrings.tr(lang, "Xong chỉnh nút", "Done Editing")
                        } else {
                            PadStrings.tr(lang, "Chỉnh nút", "Edit Buttons")
                        },
                        corner = pillCorner,
                        stroke = strokeThin,
                        fontSize = fontSmall,
                        isActive = isCustomizeMode,
                        onClick = {
                            if (!isCustomizeMode) {
                                PadNetworkClient.releaseAllInputsImmediate()
                            } else {
                                persistCurrentTunings()
                            }
                            isCustomizeMode = !isCustomizeMode
                            triggerLocalTouchTick()
                        },
                        testTag = "pad_customize_toggle_btn"
                    )

                    PadTopBarButton(
                        label = PadStrings.tr(lang, "Ngắt", "Disconnect"),
                        corner = pillCorner,
                        stroke = strokeThin,
                        fontSize = fontSmall,
                        isDanger = true,
                        onClick = onDisconnect,
                        testTag = "pad_controller_disconnect_btn"
                    )
                }
            }

            // Thanh công cụ chỉnh riêng từng nút khi bật "Chỉnh nút"
            if (isCustomizeMode) {
                val curTuning = tuningFor(selectedNodeKey)
                val swipeLabel = when (curTuning.swipeBit) {
                    RemotePadButtons.L3 -> "Vuốt → L3"
                    RemotePadButtons.R3 -> "Vuốt → R3"
                    RemotePadButtons.Y -> "Vuốt → Y"
                    else -> PadStrings.tr(lang, "Vuốt vai: Mặc định", "Swipe: Default")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(barCorner))
                        .background(PadSurface)
                        .border(strokeThin, PadAccent, RoundedCornerShape(barCorner))
                        .padding(horizontal = unit * 2.4f, vertical = unit * 1.0f)
                        .testTag("pad_customize_toolbar"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PadStrings.tr(
                            lang = lang,
                            vi = "Kéo cụm [$selectedNodeKey] để dời · Cỡ ${(curTuning.scaleMult * 100).roundToInt()}%",
                            en = "Drag [$selectedNodeKey] to move · Size ${(curTuning.scaleMult * 100).roundToInt()}%"
                        ),
                        color = PadInk,
                        fontSize = fontSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(unit * 1.2f)) {
                        PadTopBarButton(
                            label = "−",
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontSmall,
                            onClick = {
                                val next = (curTuning.scaleMult - 0.08f).coerceIn(0.72f, 1.40f)
                                nodeTunings[selectedNodeKey] = curTuning.copy(scaleMult = next)
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_scale_down_btn"
                        )
                        PadTopBarButton(
                            label = "+",
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontSmall,
                            onClick = {
                                val next = (curTuning.scaleMult + 0.08f).coerceIn(0.72f, 1.40f)
                                nodeTunings[selectedNodeKey] = curTuning.copy(scaleMult = next)
                                persistCurrentTunings()
                            },
                            testTag = "pad_customize_scale_up_btn"
                        )
                        if (selectedNodeKey == "shoulders_left" || selectedNodeKey == "shoulders_right") {
                            PadTopBarButton(
                                label = swipeLabel,
                                corner = pillCorner,
                                stroke = strokeThin,
                                fontSize = fontSmall,
                                onClick = {
                                    val cycle = listOf(0, RemotePadButtons.L3, RemotePadButtons.R3, RemotePadButtons.Y)
                                    val nextSwipe = cycle[(cycle.indexOf(curTuning.swipeBit) + 1) % cycle.size]
                                    nodeTunings[selectedNodeKey] = curTuning.copy(swipeBit = nextSwipe)
                                    persistCurrentTunings()
                                },
                                testTag = "pad_customize_swipe_btn"
                            )
                        }
                        PadTopBarButton(
                            label = PadStrings.tr(lang, "Đặt lại", "Reset"),
                            corner = pillCorner,
                            stroke = strokeThin,
                            fontSize = fontSmall,
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

            // 2. Bố cục tay cầm 3 cột (#s5 .pad-grid trong remote-pad-mockup.html)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // === CỘT TRÁI: LT + LB (vuốt nút vai) + D-Pad + Cần trái + L3 ===
                Column(
                    modifier = Modifier
                        .width(w * 0.34f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    val leftShoulderScale = currentPreset.shoulderBonusScale * tuningFor("shoulders_left").scaleMult
                    val leftSwipeTargetBit = tuningFor("shoulders_left").swipeBit.takeIf { it != 0 } ?: RemotePadButtons.L3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .customizableNode("shoulders_left"),
                        horizontalArrangement = Arrangement.spacedBy(unit * 2.0f)
                    ) {
                        PadTriggerButton(
                            label = "LT",
                            height = h * 0.135f * leftShoulderScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "shoulders_left" },
                            onTriggerValueChanged = { v, justPressed ->
                                if (justPressed) triggerLocalTouchTick(strong = true)
                                PadNetworkClient.updateLeftTrigger(v)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pad_btn_lt")
                        )

                        PadShoulderSwipeButton(
                            label = "LB",
                            primaryBit = RemotePadButtons.LB,
                            swipeTargetBit = leftSwipeTargetBit,
                            height = h * 0.135f * leftShoulderScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "shoulders_left" },
                            onHaptic = { triggerLocalTouchTick(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pad_btn_lb")
                        )
                    }

                    // D-Pad + Cần trái (song song để ngón cái trái chuyển nhanh)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val dpadSize = minOf(w * 0.155f, h * 0.36f) * tuningFor("dpad").scaleMult
                        PadEightWayDpad(
                            size = dpadSize,
                            corner = unit * 2.2f,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "dpad" },
                            onHaptic = { triggerLocalTouchTick(false) },
                            modifier = Modifier
                                .customizableNode("dpad")
                                .testTag("pad_dpad")
                        )

                        val leftStickSize = minOf(w * 0.165f, h * 0.39f) *
                            currentPreset.stickBonusScale *
                            tuningFor("stick_left").scaleMult
                        PadAnalogStickZone(
                            label = "L",
                            size = leftStickSize,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            autoSprintL3 = currentPreset.autoSprintL3,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "stick_left" },
                            onStickChanged = { x, y, sprintBit ->
                                PadNetworkClient.updateLeftStick(x, y, sprintBit)
                            },
                            onDoubleTapStickClick = { pressed ->
                                if (pressed) triggerLocalTouchTick(true)
                                PadNetworkClient.updateButtonBit(RemotePadButtons.L3, pressed)
                            },
                            modifier = Modifier
                                .customizableNode("stick_left")
                                .testTag("pad_left_stick")
                        )
                    }
                }

                // === CỘT GIỮA: View / Xbox / Menu + L3 / R3 ===
                Column(
                    modifier = Modifier
                        .width(w * 0.24f)
                        .fillMaxHeight()
                        .customizableNode("center_cluster"),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val centerScale = tuningFor("center_cluster").scaleMult
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(unit * 1.6f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PadMomentaryPillButton(
                            label = "View",
                            bit = RemotePadButtons.VIEW,
                            width = w * 0.068f * centerScale,
                            height = h * 0.095f * centerScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "center_cluster" },
                            onHaptic = { triggerLocalTouchTick(false) },
                            testTag = "pad_btn_view"
                        )
                        PadMomentaryPillButton(
                            label = "Xbox",
                            bit = RemotePadButtons.XBOX,
                            width = w * 0.068f * centerScale,
                            height = h * 0.095f * centerScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "center_cluster" },
                            onHaptic = { triggerLocalTouchTick(true) },
                            testTag = "pad_btn_xbox"
                        )
                        PadMomentaryPillButton(
                            label = "Menu",
                            bit = RemotePadButtons.MENU,
                            width = w * 0.068f * centerScale,
                            height = h * 0.095f * centerScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "center_cluster" },
                            onHaptic = { triggerLocalTouchTick(false) },
                            testTag = "pad_btn_menu"
                        )
                    }

                    Spacer(modifier = Modifier.height(h * 0.04f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(unit * 2.4f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PadMomentaryPillButton(
                            label = "L3",
                            bit = RemotePadButtons.L3,
                            width = w * 0.075f * centerScale,
                            height = h * 0.09f * centerScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "center_cluster" },
                            onHaptic = { triggerLocalTouchTick(true) },
                            testTag = "pad_btn_l3"
                        )
                        PadMomentaryPillButton(
                            label = "R3",
                            bit = RemotePadButtons.R3,
                            width = w * 0.075f * centerScale,
                            height = h * 0.09f * centerScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "center_cluster" },
                            onHaptic = { triggerLocalTouchTick(true) },
                            testTag = "pad_btn_r3"
                        )
                    }

                    Spacer(modifier = Modifier.height(h * 0.03f))
                }

                // === CỘT PHẢI: RB (vuốt nút vai) + RT + Cần phải + Cụm kim cương Y/X/B/A ===
                Column(
                    modifier = Modifier
                        .width(w * 0.34f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val rightShoulderScale = currentPreset.shoulderBonusScale * tuningFor("shoulders_right").scaleMult
                    val rightSwipeTargetBit = tuningFor("shoulders_right").swipeBit.takeIf { it != 0 } ?: RemotePadButtons.R3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .customizableNode("shoulders_right"),
                        horizontalArrangement = Arrangement.spacedBy(unit * 2.0f)
                    ) {
                        PadShoulderSwipeButton(
                            label = "RB",
                            primaryBit = RemotePadButtons.RB,
                            swipeTargetBit = rightSwipeTargetBit,
                            height = h * 0.135f * rightShoulderScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "shoulders_right" },
                            onHaptic = { triggerLocalTouchTick(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pad_btn_rb")
                        )

                        PadTriggerButton(
                            label = "RT",
                            height = h * 0.135f * rightShoulderScale,
                            corner = pillCorner,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "shoulders_right" },
                            onTriggerValueChanged = { v, justPressed ->
                                if (justPressed) triggerLocalTouchTick(strong = true)
                                PadNetworkClient.updateRightTrigger(v)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pad_btn_rt")
                        )
                    }

                    // Cần phải + Cụm kim cương Y/X/B/A
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val rightStickSize = minOf(w * 0.155f, h * 0.37f) *
                            currentPreset.stickBonusScale *
                            tuningFor("stick_right").scaleMult
                        PadAnalogStickZone(
                            label = "R",
                            size = rightStickSize,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontSmall,
                            autoSprintL3 = false,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "stick_right" },
                            onStickChanged = { x, y, _ ->
                                PadNetworkClient.updateRightStick(x, y)
                            },
                            onDoubleTapStickClick = { pressed ->
                                if (pressed) triggerLocalTouchTick(true)
                                PadNetworkClient.updateButtonBit(RemotePadButtons.R3, pressed)
                            },
                            modifier = Modifier
                                .customizableNode("stick_right")
                                .testTag("pad_right_stick")
                        )

                        val abxySize = minOf(w * 0.165f, h * 0.40f) *
                            currentPreset.abxyBonusScale *
                            tuningFor("abxy").scaleMult
                        PadAbxyDiamondCluster(
                            size = abxySize,
                            swapSwitchAbxy = currentPreset.swapSwitchAbxy,
                            strokeThin = strokeThin,
                            strokeActive = strokeActive,
                            fontSize = fontBtn,
                            enabled = !isCustomizeMode,
                            onSelectInEdit = { selectedNodeKey = "abxy" },
                            onHaptic = { triggerLocalTouchTick(false) },
                            modifier = Modifier
                                .customizableNode("abxy")
                                .testTag("pad_abxy_cluster")
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
        isActive -> PadAccent
        else -> PadBorder
    }
    val textColor = when {
        isDanger -> PadDanger
        isActive -> PadAccent
        else -> PadInk
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(corner))
            .background(if (isActive) PadAccentFill else PadSurface2)
            .border(stroke, borderColor, RoundedCornerShape(corner))
            .clickable(onClick = onClick)
            .padding(horizontal = corner * 1.15f, vertical = corner * 0.55f)
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
 * Nút cò LT / RT hỗ trợ cả nhấn nhanh (1.0) và kéo dọc để điều tiết độ sâu cò (0.15 .. 1.0) cho game đua xe.
 */
@Composable
private fun PadTriggerButton(
    label: String,
    height: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    enabled: Boolean,
    onSelectInEdit: () -> Unit,
    onTriggerValueChanged: (Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeVal by remember { mutableFloatStateOf(0f) }
    val isPressed = activeVal > 0.05f

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(if (isPressed) PadAccentFill else PadSurface)
            .border(
                if (isPressed) strokeActive else strokeThin,
                if (isPressed) PadAccent else PadBorder,
                RoundedCornerShape(corner)
            )
            .pointerInput(enabled) {
                if (!enabled) {
                    awaitEachGesture {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) {
                            onSelectInEdit()
                        }
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    var wasDown = false
                    var startY = 0f
                    do {
                        val event = awaitPointerEvent()
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        if (activeChange != null) {
                            activeChange.consume()
                            if (!wasDown) {
                                wasDown = true
                                startY = activeChange.position.y
                                activeVal = 1.0f
                                onTriggerValueChanged(1.0f, true)
                            } else {
                                val dy = activeChange.position.y - startY
                                val hPx = size.height.coerceAtLeast(1).toFloat()
                                val v = if (abs(dy) < hPx * 0.18f) {
                                    1.0f
                                } else {
                                    ((dy / hPx) + 0.65f).coerceIn(0.20f, 1.0f)
                                }
                                activeVal = v
                                onTriggerValueChanged(v, false)
                            }
                        } else if (wasDown) {
                            wasDown = false
                            activeVal = 0f
                            onTriggerValueChanged(0f, false)
                        }
                    } while (event.changes.any { it.pressed })

                    if (wasDown) {
                        activeVal = 0f
                        onTriggerValueChanged(0f, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) PadAccent else PadInk,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Nút vai LB / RB hỗ trợ vuốt hướng (vuốt ngang/dọc vượt ngưỡng kích hoạt thêm nút phụ như L3/R3/Y).
 */
@Composable
private fun PadShoulderSwipeButton(
    label: String,
    primaryBit: Int,
    swipeTargetBit: Int,
    height: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    enabled: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var isSwiped by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(if (isPressed) PadAccentFill else PadSurface)
            .border(
                if (isPressed) strokeActive else strokeThin,
                if (isPressed) PadAccent else PadBorder,
                RoundedCornerShape(corner)
            )
            .pointerInput(enabled, primaryBit, swipeTargetBit) {
                if (!enabled) {
                    awaitEachGesture {
                        val down = awaitPointerEvent()
                        if (down.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    var downPos: Offset? = null
                    var swipeLatched = false
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed }
                        if (change != null) {
                            change.consume()
                            if (downPos == null) {
                                downPos = change.position
                                isPressed = true
                                onHaptic(false)
                                PadNetworkClient.updateButtonBit(primaryBit, true)
                            } else {
                                val dist = hypot(
                                    change.position.x - downPos.x,
                                    change.position.y - downPos.y
                                )
                                val threshold = size.height.coerceAtLeast(24) * 0.48f
                                if (!swipeLatched && dist >= threshold && swipeTargetBit != 0) {
                                    swipeLatched = true
                                    isSwiped = true
                                    onHaptic(true)
                                    PadNetworkClient.updateButtonBit(swipeTargetBit, true)
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isPressed) {
                        isPressed = false
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
        Text(
            text = if (isSwiped) "$label+" else label,
            color = if (isPressed) PadAccent else PadInk,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PadMomentaryPillButton(
    label: String,
    bit: Int,
    width: Dp,
    height: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    enabled: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    testTag: String
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(corner))
            .background(if (pressed) PadAccentFill else PadSurface)
            .border(
                if (pressed) strokeActive else strokeThin,
                if (pressed) PadAccent else PadBorder,
                RoundedCornerShape(corner)
            )
            .pointerInput(enabled, bit) {
                if (!enabled) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    var active = false
                    do {
                        val event = awaitPointerEvent()
                        val anyDown = event.changes.any { it.pressed }
                        event.changes.forEach { it.consume() }
                        if (anyDown && !active) {
                            active = true
                            pressed = true
                            onHaptic()
                            PadNetworkClient.updateButtonBit(bit, true)
                        }
                    } while (event.changes.any { it.pressed })

                    if (active) {
                        pressed = false
                        PadNetworkClient.updateButtonBit(bit, false)
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (pressed) PadAccent else PadMuted,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * D-Pad 4 hướng hỗ trợ bấm chéo 8 hướng mượt mà bằng một ngón cái.
 */
@Composable
private fun PadEightWayDpad(
    size: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    enabled: Boolean,
    onSelectInEdit: () -> Unit,
    onHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDpadMask = RemotePadButtons.DPAD_UP or
        RemotePadButtons.DPAD_DOWN or
        RemotePadButtons.DPAD_LEFT or
        RemotePadButtons.DPAD_RIGHT
    var activeMask by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(enabled) {
                if (!enabled) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed }
                        if (change != null) {
                            change.consume()
                            val cx = this.size.width / 2f
                            val cy = this.size.height / 2f
                            val dx = (change.position.x - cx) / cx.coerceAtLeast(1f)
                            val dy = (change.position.y - cy) / cy.coerceAtLeast(1f)
                            var mask = 0
                            if (hypot(dx, dy) > 0.20f) {
                                if (dy < -0.32f) mask = mask or RemotePadButtons.DPAD_UP
                                if (dy > 0.32f) mask = mask or RemotePadButtons.DPAD_DOWN
                                if (dx < -0.32f) mask = mask or RemotePadButtons.DPAD_LEFT
                                if (dx > 0.32f) mask = mask or RemotePadButtons.DPAD_RIGHT
                            }
                            if (mask != activeMask) {
                                if (mask != 0) onHaptic()
                                activeMask = mask
                                PadNetworkClient.updateButtonsMask(allDpadMask, mask)
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (activeMask != 0) {
                        activeMask = 0
                        PadNetworkClient.updateButtonsMask(allDpadMask, 0)
                    }
                }
            }
    ) {
        val arm = size * 0.32f
        // Up
        DpadArmCell(
            symbol = "▲",
            active = (activeMask and RemotePadButtons.DPAD_UP) != 0,
            armSize = arm,
            corner = corner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        // Down
        DpadArmCell(
            symbol = "▼",
            active = (activeMask and RemotePadButtons.DPAD_DOWN) != 0,
            armSize = arm,
            corner = corner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        // Left
        DpadArmCell(
            symbol = "◀",
            active = (activeMask and RemotePadButtons.DPAD_LEFT) != 0,
            armSize = arm,
            corner = corner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        // Right
        DpadArmCell(
            symbol = "▶",
            active = (activeMask and RemotePadButtons.DPAD_RIGHT) != 0,
            armSize = arm,
            corner = corner,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun DpadArmCell(
    symbol: String,
    active: Boolean,
    armSize: Dp,
    corner: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(armSize)
            .clip(RoundedCornerShape(corner))
            .background(if (active) PadAccentFill else PadSurface)
            .border(
                if (active) strokeActive else strokeThin,
                if (active) PadAccent else PadBorder,
                RoundedCornerShape(corner)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (active) PadAccent else PadInk,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Cụm 4 nút kim cương Y / X / B / A hỗ trợ cả nhấn từng nút và trượt ngón cái giữ tổ hợp 2 nút.
 */
@Composable
private fun PadAbxyDiamondCluster(
    size: Dp,
    swapSwitchAbxy: Boolean,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    enabled: Boolean,
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

    val allAbxyMask = RemotePadButtons.A or RemotePadButtons.B or RemotePadButtons.X or RemotePadButtons.Y
    var activeMask by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(enabled, swapSwitchAbxy) {
                if (!enabled) {
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

                        val topCenter = Offset(wPx * 0.50f, hPx * 0.17f)
                        val bottomCenter = Offset(wPx * 0.50f, hPx * 0.83f)
                        val leftCenter = Offset(wPx * 0.17f, hPx * 0.50f)
                        val rightCenter = Offset(wPx * 0.83f, hPx * 0.50f)

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
    ) {
        val btnDiam = size * 0.34f
        AbxyCircleCell(
            label = topLabel,
            active = (activeMask and topBit) != 0,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        AbxyCircleCell(
            label = leftLabel,
            active = (activeMask and leftBit) != 0,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        AbxyCircleCell(
            label = rightLabel,
            active = (activeMask and rightBit) != 0,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        AbxyCircleCell(
            label = bottomLabel,
            active = (activeMask and bottomBit) != 0,
            diameter = btnDiam,
            strokeThin = strokeThin,
            strokeActive = strokeActive,
            fontSize = fontSize,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AbxyCircleCell(
    label: String,
    active: Boolean,
    diameter: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(if (active) PadAccentFill else PadSurface)
            .border(
                if (active) strokeActive else strokeThin,
                if (active) PadAccent else PadBorder,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) PadAccent else PadInk,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Cần gạt Analog (LX/LY hoặc RX/RY) với tâm di chuyển mượt theo ngón tay và tự trả về 0 khi nhả.
 */
@Composable
private fun PadAnalogStickZone(
    label: String,
    size: Dp,
    strokeThin: Dp,
    strokeActive: Dp,
    fontSize: TextUnit,
    autoSprintL3: Boolean,
    enabled: Boolean,
    onSelectInEdit: () -> Unit,
    onStickChanged: (Float, Float, Int) -> Unit,
    onDoubleTapStickClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(enabled, autoSprintL3) {
                if (!enabled) {
                    awaitEachGesture {
                        val ev = awaitPointerEvent()
                        if (ev.changes.any { it.pressed }) onSelectInEdit()
                    }
                    return@pointerInput
                }
                var lastDownTimeMs = 0L
                awaitEachGesture {
                    var doubleTapHeld = false
                    var started = false
                    do {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull { it.pressed }
                        if (ch != null) {
                            ch.consume()
                            if (!started) {
                                started = true
                                isTouching = true
                                val now = System.currentTimeMillis()
                                if (now - lastDownTimeMs <= 260L) {
                                    doubleTapHeld = true
                                    onDoubleTapStickClick(true)
                                }
                                lastDownTimeMs = now
                            }
                            val cx = this.size.width / 2f
                            val cy = this.size.height / 2f
                            val maxRadius = minOf(cx, cy) * 0.72f
                            val rawDx = (ch.position.x - cx) / maxRadius.coerceAtLeast(1f)
                            val rawDy = (ch.position.y - cy) / maxRadius.coerceAtLeast(1f)
                            val mag = hypot(rawDx, rawDy)
                            val (nx, ny) = if (mag > 1f) {
                                (rawDx / mag) to (rawDy / mag)
                            } else if (mag < 0.06f) {
                                0f to 0f
                            } else {
                                rawDx to rawDy
                            }
                            knobX = nx
                            knobY = ny
                            val sprintBit = if (autoSprintL3 && ny <= -0.94f) RemotePadButtons.L3 else 0
                            onStickChanged(nx, ny, sprintBit)
                        }
                    } while (event.changes.any { it.pressed })

                    if (started) {
                        isTouching = false
                        knobX = 0f
                        knobY = 0f
                        onStickChanged(0f, 0f, 0)
                    }
                    if (doubleTapHeld) {
                        onDoubleTapStickClick(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val strokePx = if (isTouching) strokeActive.toPx() else strokeThin.toPx()
            drawCircle(
                color = PadSurface,
                radius = radius - strokePx / 2f
            )
            drawCircle(
                color = if (isTouching) PadAccent else PadBorder,
                radius = radius - strokePx / 2f,
                style = Stroke(width = strokePx)
            )

            val travel = radius * 0.46f
            val nubCenter = Offset(
                x = center.x + knobX * travel,
                y = center.y + knobY * travel
            )
            val nubRadius = radius * 0.42f
            drawCircle(
                color = if (isTouching) PadAccentFill else PadSurface2,
                radius = nubRadius,
                center = nubCenter
            )
            drawCircle(
                color = if (isTouching) PadAccent else PadBorder,
                radius = nubRadius,
                center = nubCenter,
                style = Stroke(width = strokePx)
            )
        }

        Text(
            text = label,
            color = if (isTouching) PadAccent else PadMuted,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
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
            out[k] = PadNodeTuning(
                dxNorm = obj.optDouble("dx", 0.0).toFloat().coerceIn(-0.25f, 0.25f),
                dyNorm = obj.optDouble("dy", 0.0).toFloat().coerceIn(-0.25f, 0.25f),
                scaleMult = obj.optDouble("sc", 1.0).toFloat().coerceIn(0.72f, 1.40f),
                swipeBit = obj.optInt("sw", 0)
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
                    put("sw", v.swipeBit)
                }
            )
        }
        prefs.edit().putString("tunings_$presetId", root.toString()).apply()
    } catch (_: Exception) {
    }
}
