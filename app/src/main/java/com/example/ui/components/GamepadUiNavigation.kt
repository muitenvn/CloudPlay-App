package com.example.ui.components

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.XboxNeonGreen
import kotlinx.coroutines.delay

/**
 * Quản lý trạng thái điều khiển giao diện bằng tay cầm thật (USB / Bluetooth) hoặc bàn phím.
 * - Viền chọn phẳng chỉ hiển thị khi người dùng đang thao tác bằng tay cầm/bàn phím ([isGamepadModeActive] == true).
 * - Khi người dùng chạm màn hình cảm ứng, [isGamepadModeActive] tắt ngay lập tức mà không gây recompose toàn màn hình
 *   nhờ chỉ đọc state này trong pha vẽ ([drawWithContent]).
 * - Không chặn sự kiện tay cầm khi đang stream game trong WebView (trừ khi menu nổi hoặc hộp thoại của app đang mở).
 */
object GamepadUiNavigation {

    /**
     * Chỉ đọc trong pha vẽ (drawWithContent) để tránh làm cả màn hình recompose khi đổi giữa chạm và tay cầm.
     */
    var isGamepadModeActive by mutableStateOf(false)
        private set

    /**
     * Cho biết giao diện app hiện có đang nhận điều khiển tay cầm hay không.
     * Khi đang ở màn WebView chơi game (và không mở menu nổi / hộp thoại), cờ này là false để mọi phím/cần
     * đi thẳng vào WebView của game.
     */
    @Volatile
    var shouldRouteToAppUi: Boolean = true

    var activeAnalogDirection by mutableStateOf<FocusDirection?>(null)
        private set

    var activeHorizontalStepper: ((Int) -> Unit)? = null

    /**
     * Tăng lên mỗi khi người dùng giữ đồng thời View (SELECT) + Menu (START) khoảng 0,6 giây.
     * Nhận tổ hợp phím theo kiểu nghe lén (passive): không chặn hay tiêu thụ phím của game.
     */
    var hudToggleComboTick by mutableIntStateOf(0)
        private set

    private var hatOrStickDirection: FocusDirection? = null
    private var lastKeyDownCode: Int = KeyEvent.KEYCODE_UNKNOWN
    private var lastKeyDownTimeMs: Long = 0L

    private var isSelectHeld: Boolean = false
    private var isStartHeld: Boolean = false
    private var comboFiredForCurrentHold: Boolean = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val comboHoldRunnable = Runnable {
        if (isSelectHeld && isStartHeld && !comboFiredForCurrentHold) {
            comboFiredForCurrentHold = true
            onGamepadInteraction()
            hudToggleComboTick++
        }
    }

    fun triggerHudToggle() {
        hudToggleComboTick++
    }

    /**
     * Nghe lén tổ hợp giữ View (nút 8) + Menu (nút 9) trên tay cầm ảo trong ~0,6 giây (600ms).
     */
    fun observeVirtualSelectStartPassively(buttonIndex: Int, pressed: Boolean) {
        if (buttonIndex != 8 && buttonIndex != 9) return
        if (pressed) {
            val wasBothHeld = isSelectHeld && isStartHeld
            if (buttonIndex == 8) isSelectHeld = true
            if (buttonIndex == 9) isStartHeld = true
            val nowBothHeld = isSelectHeld && isStartHeld
            if (!wasBothHeld && nowBothHeld && !comboFiredForCurrentHold) {
                mainHandler.removeCallbacks(comboHoldRunnable)
                mainHandler.postDelayed(comboHoldRunnable, 600L)
            }
        } else {
            if (buttonIndex == 8) isSelectHeld = false
            if (buttonIndex == 9) isStartHeld = false
            if (!isSelectHeld || !isStartHeld) {
                comboFiredForCurrentHold = false
                mainHandler.removeCallbacks(comboHoldRunnable)
            }
        }
    }

    /**
     * Nghe lén tổ hợp phím giữ View (KEYCODE_BUTTON_SELECT) + Menu (KEYCODE_BUTTON_START) trong ~0,6 giây (600ms).
     * Hàm này KHÔNG bao giờ tiêu thụ sự kiện phím để mọi nút vẫn đi thẳng vào WebView game bình thường.
     */
    fun observeKeyEventPassively(event: KeyEvent) {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_BUTTON_SELECT && keyCode != KeyEvent.KEYCODE_BUTTON_START) {
            return
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    val wasBothHeld = isSelectHeld && isStartHeld
                    if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) isSelectHeld = true
                    if (keyCode == KeyEvent.KEYCODE_BUTTON_START) isStartHeld = true
                    val nowBothHeld = isSelectHeld && isStartHeld
                    if (!wasBothHeld && nowBothHeld && !comboFiredForCurrentHold) {
                        mainHandler.removeCallbacks(comboHoldRunnable)
                        mainHandler.postDelayed(comboHoldRunnable, 600L)
                    }
                }
            }
            KeyEvent.ACTION_UP -> {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) isSelectHeld = false
                if (keyCode == KeyEvent.KEYCODE_BUTTON_START) isStartHeld = false
                if (!isSelectHeld || !isStartHeld) {
                    comboFiredForCurrentHold = false
                    mainHandler.removeCallbacks(comboHoldRunnable)
                }
            }
        }
    }

    fun onTouchInteraction() {
        if (isGamepadModeActive) {
            isGamepadModeActive = false
        }
        if (activeAnalogDirection != null) {
            activeAnalogDirection = null
            hatOrStickDirection = null
        }
    }

    fun onGamepadInteraction() {
        if (!isGamepadModeActive) {
            isGamepadModeActive = true
        }
    }

    fun isFromGamepadOrKeyboard(event: KeyEvent): Boolean {
        val src = event.source
        if ((src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (src and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        ) {
            return true
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> true
            else -> false
        }
    }

    /**
     * Xử lý cần analog trái (AXIS_X, AXIS_Y) và D-Pad dạng trục (AXIS_HAT_X, AXIS_HAT_Y)
     * chỉ khi đang ở giao diện app ([shouldRouteToAppUi] == true).
     */
    fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (!shouldRouteToAppUi) {
            if (activeAnalogDirection != null) {
                activeAnalogDirection = null
                hatOrStickDirection = null
            }
            return false
        }
        val src = event.source
        val isJoystick = (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (src and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        if (!isJoystick || event.actionMasked != MotionEvent.ACTION_MOVE) {
            return false
        }

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val stickX = event.getAxisValue(MotionEvent.AXIS_X)
        val stickY = event.getAxisValue(MotionEvent.AXIS_Y)

        val rawX = if (kotlin.math.abs(hatX) > 0.5f) hatX else stickX
        val rawY = if (kotlin.math.abs(hatY) > 0.5f) hatY else stickY

        val deadzone = 0.48f
        val releaseZone = 0.30f
        val currentDir = hatOrStickDirection
        val threshold = if (currentDir != null) releaseZone else deadzone

        val nextDir: FocusDirection? = when {
            kotlin.math.abs(rawX) < threshold && kotlin.math.abs(rawY) < threshold -> null
            kotlin.math.abs(rawY) >= kotlin.math.abs(rawX) -> {
                if (rawY < -threshold) FocusDirection.Up else FocusDirection.Down
            }
            else -> {
                if (rawX < -threshold) FocusDirection.Left else FocusDirection.Right
            }
        }

        if (nextDir != hatOrStickDirection) {
            hatOrStickDirection = nextDir
            activeAnalogDirection = nextDir
            if (nextDir != null) {
                onGamepadInteraction()
            }
        }
        return nextDir != null
    }

    fun shouldIgnoreDuplicateDown(keyCode: Int): Boolean {
        val now = SystemClock.uptimeMillis()
        if (keyCode == lastKeyDownCode && now - lastKeyDownTimeMs < 35L) {
            return true
        }
        lastKeyDownCode = keyCode
        lastKeyDownTimeMs = now
        return false
    }
}

/**
 * Modifier gắn điều khiển tay cầm cho một vùng giao diện hoặc hộp thoại (kể cả AlertDialog tạo cửa sổ riêng):
 * - Tự động lặp khi giữ cần trái / D-Pad (initial delay 300ms, bước lặp 105ms).
 * - Tự động cấp focus vào [defaultFocusRequester] khi mở hộp thoại/bảng ([autoRequestDefaultFocus] == true)
 *   hoặc khi chưa có phần tử nào đang được chọn.
 * - Gắn OnGenericMotionListener cho cửa sổ Dialog để cần analog trái hoạt động trong mọi hộp thoại.
 * - Phím LB / RB để chuyển tab, phím B để đóng hộp thoại / quay lại, phím X / Y cho thao tác phụ.
 */
fun Modifier.gamepadControllerScope(
    enabled: Boolean = true,
    defaultFocusRequester: FocusRequester? = null,
    autoRequestDefaultFocus: Boolean = false,
    attachWindowMotionListener: Boolean = false,
    onPreviousTab: (() -> Unit)? = null,
    onNextTab: (() -> Unit)? = null,
    onBackPress: (() -> Unit)? = null,
    onSecondaryX: (() -> Unit)? = null,
    onSecondaryY: (() -> Unit)? = null
): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val view = androidx.compose.ui.platform.LocalView.current
    val currentOnPrevTab by rememberUpdatedState(onPreviousTab)
    val currentOnNextTab by rememberUpdatedState(onNextTab)
    val currentOnBack by rememberUpdatedState(onBackPress)
    val currentOnX by rememberUpdatedState(onSecondaryX)
    val currentOnY by rememberUpdatedState(onSecondaryY)
    val currentDefaultRequester by rememberUpdatedState(defaultFocusRequester)

    var heldDpadDirection by remember { mutableStateOf<FocusDirection?>(null) }
    var dpadPressSeq by remember { mutableIntStateOf(0) }

    DisposableEffect(enabled) {
        if (!enabled) {
            heldDpadDirection = null
        }
        onDispose {
            heldDpadDirection = null
        }
    }

    DisposableEffect(view, enabled, attachWindowMotionListener) {
        if (!enabled || !attachWindowMotionListener) {
            onDispose { }
        } else {
            val listener = android.view.View.OnGenericMotionListener { _, event ->
                if (event != null && GamepadUiNavigation.shouldRouteToAppUi) {
                    GamepadUiNavigation.handleGenericMotionEvent(event)
                } else {
                    false
                }
            }
            view.setOnGenericMotionListener(listener)
            onDispose {
                view.setOnGenericMotionListener(null)
            }
        }
    }

    LaunchedEffect(enabled, autoRequestDefaultFocus, defaultFocusRequester) {
        if (enabled && autoRequestDefaultFocus && defaultFocusRequester != null) {
            delay(40L)
            runCatching { defaultFocusRequester.requestFocus() }
        }
    }

    fun stepFocus(direction: FocusDirection) {
        GamepadUiNavigation.onGamepadInteraction()
        val horizontalStepper = GamepadUiNavigation.activeHorizontalStepper
        if (horizontalStepper != null && (direction == FocusDirection.Left || direction == FocusDirection.Right)) {
            horizontalStepper(if (direction == FocusDirection.Left) -1 else 1)
            return
        }
        val moved = runCatching { focusManager.moveFocus(direction) }.getOrDefault(false)
        if (!moved) {
            runCatching { currentDefaultRequester?.requestFocus() }
        }
    }

    val effectiveHeldDirection = if (enabled) {
        heldDpadDirection ?: GamepadUiNavigation.activeAnalogDirection
    } else {
        null
    }

    LaunchedEffect(effectiveHeldDirection, dpadPressSeq, enabled) {
        val dir = effectiveHeldDirection ?: return@LaunchedEffect
        if (!enabled) return@LaunchedEffect
        stepFocus(dir)
        delay(300L)
        while (enabled && (heldDpadDirection == dir || GamepadUiNavigation.activeAnalogDirection == dir)) {
            stepFocus(dir)
            delay(105L)
        }
    }

    this.onKeyEvent { keyEvent ->
        val native = keyEvent.nativeKeyEvent
        GamepadUiNavigation.observeKeyEventPassively(native)
        if (!enabled) return@onKeyEvent false
        if (!GamepadUiNavigation.isFromGamepadOrKeyboard(native)) {
            return@onKeyEvent false
        }

        val keyCode = native.keyCode
        val dir = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
            KeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
            KeyEvent.KEYCODE_DPAD_LEFT -> FocusDirection.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> FocusDirection.Right
            else -> null
        }

        if (dir != null) {
            if (keyEvent.type == KeyEventType.KeyDown) {
                GamepadUiNavigation.onGamepadInteraction()
                if (native.repeatCount == 0) {
                    heldDpadDirection = dir
                    dpadPressSeq++
                }
                return@onKeyEvent true
            } else if (keyEvent.type == KeyEventType.KeyUp) {
                if (heldDpadDirection == dir) {
                    heldDpadDirection = null
                }
                return@onKeyEvent true
            }
        }

        if (keyEvent.type == KeyEventType.KeyDown && native.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    GamepadUiNavigation.onGamepadInteraction()
                    runCatching { currentDefaultRequester?.requestFocus() }
                    return@onKeyEvent true
                }
                KeyEvent.KEYCODE_BUTTON_L1 -> {
                    val prev = currentOnPrevTab
                    if (prev != null) {
                        GamepadUiNavigation.onGamepadInteraction()
                        prev()
                        return@onKeyEvent true
                    }
                }
                KeyEvent.KEYCODE_BUTTON_R1 -> {
                    val next = currentOnNextTab
                    if (next != null) {
                        GamepadUiNavigation.onGamepadInteraction()
                        next()
                        return@onKeyEvent true
                    }
                }
                KeyEvent.KEYCODE_BUTTON_B -> {
                    val back = currentOnBack
                    if (back != null) {
                        GamepadUiNavigation.onGamepadInteraction()
                        back()
                        return@onKeyEvent true
                    }
                }
                KeyEvent.KEYCODE_BUTTON_X -> {
                    val actX = currentOnX
                    if (actX != null) {
                        GamepadUiNavigation.onGamepadInteraction()
                        actX()
                        return@onKeyEvent true
                    }
                }
                KeyEvent.KEYCODE_BUTTON_Y -> {
                    val actY = currentOnY
                    if (actY != null) {
                        GamepadUiNavigation.onGamepadInteraction()
                        actY()
                        return@onKeyEvent true
                    }
                }
            }
        }
        false
    }
}

/**
 * Bọc một vùng giao diện của App hoặc hộp thoại để hỗ trợ:
 * - Tự động lặp khi giữ cần trái / D-Pad (initial delay 300ms, bước lặp 105ms).
 * - Tự động cấp focus vào [defaultFocusRequester] nếu chưa có phần tử nào đang được chọn.
 * - Phím LB / RB để chuyển tab.
 * - Phím B để đóng hộp thoại / quay lại.
 */
@Composable
fun GamepadSurfaceController(
    enabled: Boolean,
    defaultFocusRequester: FocusRequester? = null,
    autoRequestDefaultFocus: Boolean = false,
    attachWindowMotionListener: Boolean = false,
    onPreviousTab: (() -> Unit)? = null,
    onNextTab: (() -> Unit)? = null,
    onBackPress: (() -> Unit)? = null,
    onSecondaryX: (() -> Unit)? = null,
    onSecondaryY: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.gamepadControllerScope(
            enabled = enabled,
            defaultFocusRequester = defaultFocusRequester,
            autoRequestDefaultFocus = autoRequestDefaultFocus,
            attachWindowMotionListener = attachWindowMotionListener,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            onBackPress = onBackPress,
            onSecondaryX = onSecondaryX,
            onSecondaryY = onSecondaryY
        )
    ) {
        content()
    }
}

/**
 * Modifier gắn khả năng nhận focus tay cầm + vẽ viền chọn phẳng (không phát sáng, không hoạt ảnh lặp,
 * không recompose màn hình khi đổi focus, tự ẩn khi chạm cảm ứng) và kích hoạt [onClick] khi bấm nút A.
 */
fun Modifier.gamepadClickable(
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    focusBorderColor: Color = XboxNeonGreen,
    focusRequester: FocusRequester? = null,
    onStepHorizontal: ((Int) -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    var aButtonDown by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnStepHorizontal by rememberUpdatedState(onStepHorizontal)
    val currentOnFocused by rememberUpdatedState(onFocused)
    val interactionSource = remember { MutableInteractionSource() }

    DisposableEffect(Unit) {
        onDispose {
            if (isFocused && currentOnStepHorizontal != null) {
                GamepadUiNavigation.activeHorizontalStepper = null
            }
        }
    }

    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state ->
            val next = state.isFocused
            if (isFocused != next) {
                isFocused = next
                if (next) {
                    if (currentOnStepHorizontal != null) {
                        GamepadUiNavigation.activeHorizontalStepper = { dir ->
                            currentOnStepHorizontal?.invoke(dir)
                        }
                    } else {
                        GamepadUiNavigation.activeHorizontalStepper = null
                    }
                    currentOnFocused?.invoke()
                } else {
                    if (currentOnStepHorizontal != null) {
                        GamepadUiNavigation.activeHorizontalStepper = null
                    }
                    aButtonDown = false
                }
            }
        }
        .onKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val keyCode = native.keyCode
            val isConfirmKey = keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE

            if (isConfirmKey) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    GamepadUiNavigation.onGamepadInteraction()
                    if (native.repeatCount == 0 && !GamepadUiNavigation.shouldIgnoreDuplicateDown(keyCode)) {
                        aButtonDown = true
                        currentOnClick()
                    }
                    return@onKeyEvent true
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    aButtonDown = false
                    return@onKeyEvent true
                }
            }
            false
        }
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                currentOnClick()
            }
        )
        .drawWithContent {
            drawContent()
            if (isFocused && GamepadUiNavigation.isGamepadModeActive) {
                val strokeWidthPx = 2.dp.toPx()
                val halfStroke = strokeWidthPx / 2f
                val radiusPx = cornerRadius.toPx()
                drawRoundRect(
                    color = focusBorderColor.copy(alpha = 0.08f),
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    color = focusBorderColor,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
}

/**
 * Dùng cho các thành phần Material 3 đã có sẵn clickable/focusTarget bên trong (như Button, TextButton, IconButton)
 * để không tạo 2 điểm focus lồng nhau mà vẫn vẽ viền chọn phẳng và nhận phím A chuẩn xác.
 */
fun Modifier.gamepadActionButton(
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    focusBorderColor: Color = XboxNeonGreen,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnFocused by rememberUpdatedState(onFocused)

    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state ->
            val next = state.isFocused
            if (isFocused != next) {
                isFocused = next
                if (next) {
                    currentOnFocused?.invoke()
                }
            }
        }
        .onKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val keyCode = native.keyCode
            val isConfirmKey = keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

            if (isConfirmKey) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    GamepadUiNavigation.onGamepadInteraction()
                    if (native.repeatCount == 0 && !GamepadUiNavigation.shouldIgnoreDuplicateDown(keyCode)) {
                        currentOnClick()
                    }
                    return@onKeyEvent true
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    return@onKeyEvent true
                }
            }
            false
        }
        .drawWithContent {
            drawContent()
            if (isFocused && GamepadUiNavigation.isGamepadModeActive) {
                val strokeWidthPx = 2.dp.toPx()
                val halfStroke = strokeWidthPx / 2f
                val radiusPx = cornerRadius.toPx()
                drawRoundRect(
                    color = focusBorderColor.copy(alpha = 0.10f),
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    color = focusBorderColor,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
}

/**
 * Dùng cho OutlinedTextField để hiển thị viền chọn phẳng đồng nhất khi focus bằng tay cầm.
 */
fun Modifier.gamepadTextFieldFocus(
    cornerRadius: Dp = 14.dp,
    focusBorderColor: Color = XboxNeonGreen,
    focusRequester: FocusRequester? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state ->
            isFocused = state.isFocused || state.hasFocus
        }
        .drawWithContent {
            drawContent()
            if (isFocused && GamepadUiNavigation.isGamepadModeActive) {
                val strokeWidthPx = 2.dp.toPx()
                val halfStroke = strokeWidthPx / 2f
                val radiusPx = cornerRadius.toPx()
                drawRoundRect(
                    color = focusBorderColor,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.gamepadCombinedClickable(
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    focusBorderColor: Color = XboxNeonGreen,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    onKeyY: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnKeyY by rememberUpdatedState(onKeyY)
    val currentOnFocused by rememberUpdatedState(onFocused)
    val interactionSource = remember { MutableInteractionSource() }

    this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state ->
            val next = state.isFocused
            if (isFocused != next) {
                isFocused = next
                if (next) {
                    currentOnFocused?.invoke()
                }
            }
        }
        .onKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val keyCode = native.keyCode
            val isConfirmKey = keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

            if (isConfirmKey) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    GamepadUiNavigation.onGamepadInteraction()
                    if (native.repeatCount == 0 && !GamepadUiNavigation.shouldIgnoreDuplicateDown(keyCode)) {
                        currentOnClick()
                    }
                    return@onKeyEvent true
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    return@onKeyEvent true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BUTTON_Y && currentOnKeyY != null) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    GamepadUiNavigation.onGamepadInteraction()
                    if (native.repeatCount == 0 && !GamepadUiNavigation.shouldIgnoreDuplicateDown(keyCode)) {
                        currentOnKeyY?.invoke()
                    }
                    return@onKeyEvent true
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    return@onKeyEvent true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BUTTON_START && currentOnLongClick != null) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    GamepadUiNavigation.onGamepadInteraction()
                    if (native.repeatCount == 0 && !GamepadUiNavigation.shouldIgnoreDuplicateDown(keyCode)) {
                        currentOnLongClick?.invoke()
                    }
                    return@onKeyEvent true
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    return@onKeyEvent true
                }
            }
            false
        }
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = { currentOnClick() },
            onLongClick = currentOnLongClick
        )
        .drawWithContent {
            drawContent()
            if (isFocused && GamepadUiNavigation.isGamepadModeActive) {
                val strokeWidthPx = 2.dp.toPx()
                val halfStroke = strokeWidthPx / 2f
                val radiusPx = cornerRadius.toPx()
                drawRoundRect(
                    color = focusBorderColor.copy(alpha = 0.08f),
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    color = focusBorderColor,
                    topLeft = Offset(halfStroke, halfStroke),
                    size = Size(
                        (size.width - strokeWidthPx).coerceAtLeast(0f),
                        (size.height - strokeWidthPx).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
}


