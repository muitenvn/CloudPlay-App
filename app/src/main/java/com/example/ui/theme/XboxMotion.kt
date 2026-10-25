package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hệ thống hiệu ứng chuyển động chuẩn Xbox Dashboard (< 220ms, Không Lag).
 * - Chỉ biến đổi ở tầng GPU (Modifier.graphicsLayer) trong pha Vẽ (Draw phase),
 *   không gây tái đo đạc (Measure/Layout) làm rớt khung hình khi cuộn danh sách.
 * - Tự động tắt hoàn toàn khi bật Chế độ máy yếu (lowEndDeviceMode),
 *   Giảm tải GPU (gpuLoadReductionEnabled), hoặc khi hệ thống tắt hiệu ứng.
 */
object XboxMotion {
    const val PRESS_DOWN_MS = 105
    const val PRESS_RELEASE_MS = 165
    const val TAB_ENTER_MS = 180
    const val TAB_EXIT_MS = 125
    const val ITEM_ENTER_MS = 175
    const val HUD_EXPAND_MS = 180
    const val HUD_COLLAPSE_MS = 135
    const val COUNTER_TWEEN_MS = 180
}

/**
 * Kiểm tra xem thiết bị có cho phép chạy hiệu ứng mượt hay không.
 * Tự động tắt nếu người dùng bật Chế độ máy yếu, Giảm tải GPU, hoặc tắt ANIMATOR_DURATION_SCALE.
 */
@Composable
fun rememberXboxMotionEnabled(
    lowEndDeviceMode: Boolean = false,
    gpuLoadReductionEnabled: Boolean = false
): Boolean {
    val context = LocalContext.current
    val systemAnimationsEnabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
    return !lowEndDeviceMode && !gpuLoadReductionEnabled && systemAnimationsEnabled
}

/**
 * Hiệu ứng phản hồi nhấn kiểu Xbox Dashboard (< 170ms) trên tầng GPU (graphicsLayer).
 * Sử dụng pointerInput không chặn sự kiện click/scroll của thẻ hoặc nút bên trong,
 * giúp thu nhẹ về pressedScale (mặc định 0.965f) khi chạm và nảy mượt về 1.0f khi nhả.
 */
fun Modifier.xboxPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f
): Modifier = composed {
    if (!enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = if (isPressed) XboxMotion.PRESS_DOWN_MS else XboxMotion.PRESS_RELEASE_MS,
            easing = if (isPressed) LinearOutSlowInEasing else FastOutSlowInEasing
        ),
        label = "xboxPressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Hiệu ứng xuất hiện nhanh trên tầng GPU (trượt nhẹ từ dưới lên + mờ dần trong 175ms).
 * Không làm thay đổi kích thước phần tử nên không gây giật LazyColumn / LazyVerticalGrid.
 */
fun Modifier.xboxSlideFadeIn(
    enabled: Boolean = true,
    key: Any? = Unit,
    delayMillis: Int = 0,
    durationMillis: Int = XboxMotion.ITEM_ENTER_MS,
    initialOffsetY: Dp = 10.dp,
    initialScale: Float = 0.985f
): Modifier = composed {
    if (!enabled) return@composed this

    val density = LocalDensity.current
    val offsetPx = remember(density, initialOffsetY) {
        with(density) { initialOffsetY.toPx() }
    }
    val progress = remember(key) { Animatable(0f) }

    LaunchedEffect(key, enabled) {
        if (enabled) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis.coerceAtMost(220),
                    delayMillis = delayMillis.coerceIn(0, 120),
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            progress.snapTo(1f)
        }
    }

    this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * offsetPx
        val currentScale = initialScale + (1f - initialScale) * p
        scaleX = currentScale
        scaleY = currentScale
    }
}

/**
 * Bộ đếm số nhảy mượt dưới 180ms cho chỉ số Ping, FPS hoặc thống kê.
 */
@Composable
fun animateXboxIntAsState(
    targetValue: Int,
    motionEnabled: Boolean = true,
    label: String = "xboxCounter"
): State<Int> {
    if (!motionEnabled || targetValue < 0) {
        return remember(targetValue) { mutableStateOf(targetValue) }
    }
    return animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = XboxMotion.COUNTER_TWEEN_MS,
            easing = FastOutSlowInEasing
        ),
        label = label
    )
}

/**
 * Chuyển cảnh ngang nhanh chuẩn Xbox Dashboard (180ms vào / 125ms ra) khi đổi tab.
 */
fun xboxTabEnterTransition(direction: Int): EnterTransition {
    val slideSign = if (direction >= 0) 1 else -1
    return fadeIn(
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_ENTER_MS,
            easing = LinearOutSlowInEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_ENTER_MS,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { fullWidth -> (fullWidth * 0.04f * slideSign).toInt() }
    )
}

fun xboxTabExitTransition(direction: Int): ExitTransition {
    val slideSign = if (direction >= 0) -1 else 1
    return fadeOut(
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_EXIT_MS,
            easing = FastOutLinearInEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_EXIT_MS,
            easing = FastOutLinearInEasing
        ),
        targetOffsetX = { fullWidth -> (fullWidth * 0.03f * slideSign).toInt() }
    )
}

/**
 * Hiệu ứng bung/thu gọn nhanh cho bảng HUD nổi (< 180ms) không làm khựng WebRTC.
 */
fun xboxHudExpandEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = XboxMotion.HUD_EXPAND_MS,
            easing = LinearOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 0.15f),
        animationSpec = tween(
            durationMillis = XboxMotion.HUD_EXPAND_MS,
            easing = FastOutSlowInEasing
        )
    )
}

fun xboxHudCollapseExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = XboxMotion.HUD_COLLAPSE_MS,
            easing = FastOutLinearInEasing
        )
    ) + scaleOut(
        targetScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 0.15f),
        animationSpec = tween(
            durationMillis = XboxMotion.HUD_COLLAPSE_MS,
            easing = FastOutLinearInEasing
        )
    )
}
