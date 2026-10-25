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
    const val PRESS_DOWN_MS = 95
    const val PRESS_RELEASE_MS = 150
    const val TAB_ENTER_MS = 145
    const val TAB_EXIT_MS = 65
    const val ITEM_ENTER_MS = 155
    const val HUD_EXPAND_MS = 165
    const val HUD_COLLAPSE_MS = 120
    const val COUNTER_TWEEN_MS = 160
}

/**
 * Kiểm tra xem thiết bị có cho phép chạy hiệu ứng mượt hay không.
 * Tự động tắt nếu người dùng bật Chế độ máy yếu (lowEndDeviceMode) hoặc tắt ANIMATOR_DURATION_SCALE.
 * Lưu ý: gpuLoadReductionEnabled mặc định bật cho luồng WebRTC trong game nên không dùng để khóa hiệu ứng UI ngoài sảnh.
 */
@Composable
fun rememberXboxMotionEnabled(
    lowEndDeviceMode: Boolean = false,
    @Suppress("UNUSED_PARAMETER") gpuLoadReductionEnabled: Boolean = false
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
    return !lowEndDeviceMode && systemAnimationsEnabled
}

/**
 * Hiệu ứng phản hồi nhấn kiểu Xbox Dashboard (< 160ms) trên tầng GPU (graphicsLayer).
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

    val scaleMod = if (scale != 1f) {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    } else {
        Modifier
    }

    this
        .then(scaleMod)
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // Chờ ngắn 45ms để phân biệt vuốt cuộn màn hình (scroll) với nhấn thật (tap),
                // tránh việc chạm ngón tay để cuộn làm kích hoạt recomposition co giãn trên các thẻ.
                val releasedOrMovedEarly = withTimeoutOrNull(45L) {
                    waitForUpOrCancellation()
                }
                if (releasedOrMovedEarly == null) {
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Hiệu ứng xuất hiện nhanh trên tầng GPU (dành cho các khối tiêu đề cố định, chỉ chạy 1 lần).
 * Khi hoàn tất (hasAnimated == true), gỡ hoàn toàn graphicsLayer để cuộn màn hình chính đạt 120 FPS.
 */
fun Modifier.xboxSlideFadeIn(
    enabled: Boolean = true,
    key: Any? = Unit,
    delayMillis: Int = 0,
    durationMillis: Int = XboxMotion.ITEM_ENTER_MS,
    initialOffsetY: Dp = 8.dp,
    initialScale: Float = 0.988f
): Modifier = composed {
    if (!enabled) return@composed this

    var hasAnimated by remember(key) { mutableStateOf(false) }
    if (hasAnimated) return@composed this

    val density = LocalDensity.current
    val offsetPx = remember(density, initialOffsetY) {
        with(density) { initialOffsetY.toPx() }
    }
    val progress = remember(key) { Animatable(0f) }

    LaunchedEffect(key, enabled) {
        if (enabled && !hasAnimated) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis.coerceAtMost(180),
                    delayMillis = delayMillis.coerceIn(0, 80),
                    easing = FastOutSlowInEasing
                )
            )
            hasAnimated = true
        } else {
            progress.snapTo(1f)
            hasAnimated = true
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
 * Bộ đếm số nhảy mượt dưới 160ms cho chỉ số Ping, FPS hoặc thống kê.
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
 * Chuyển cảnh thuần GPU (fadeIn + scaleIn trên RenderThread, KHÔNG dùng slideInHorizontally/slideOutHorizontally
 * vốn ép hệ thống chạy lại pha Layout trên từng khung hình gây khựng khi đổi tab nặng).
 */
fun xboxTabEnterTransition(@Suppress("UNUSED_PARAMETER") direction: Int): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_ENTER_MS,
            delayMillis = 25,
            easing = LinearOutSlowInEasing
        )
    ) + scaleIn(
        initialScale = 0.986f,
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_ENTER_MS,
            delayMillis = 25,
            easing = FastOutSlowInEasing
        )
    )
}

fun xboxTabExitTransition(@Suppress("UNUSED_PARAMETER") direction: Int): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = XboxMotion.TAB_EXIT_MS,
            easing = FastOutLinearInEasing
        )
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
