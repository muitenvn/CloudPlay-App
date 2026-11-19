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
     * Khi đang ở chế độ giao diện TV (TvDashboardScreen), luôn giữ khung xanh hiển thị kể cả khi người dùng
     * chạm vào màn hình điện thoại hoặc cửa sổ nổi Ravno Pad.
     */
    var keepFocusVisibleInTvMode by mutableStateOf(false)

    /**
     * Cho biết giao diện app hiện có đang nhận điều khiển tay cầm hay không.
     * Khi đang ở màn WebView chơi game (và không mở menu nổi / hộp thoại), cờ này là false để mọi phím/cần
     * đi thẳng vào WebView của game.
     */
    @Volatile
    var shouldRouteToAppUi: Boolean = true

    var activeAnalogDirection by mutableStateOf<FocusDirection?>(null)
        private set

    var remotePadPressSeq by mutableIntStateOf(0)
        private set

    @Volatile
    internal var lastConsumedRemotePadSeq: Int = 0

    var activeHorizontalStepper: ((Int) -> Unit)? = null
    var activeHorizontalStepperConsumed: ((Int) -> Boolean)? = null
    var activeFocusedClickHandler: (() -> Unit)? = null
    var activeFocusedKeyYHandler: (() -> Unit)? = null
    var activeFocusedLongClickHandler: (() -> Unit)? = null

    internal class ControllerScopeCallbacks(
        val stepFocus: (FocusDirection) -> Unit,
        val onConfirmA: () -> Unit,
        val onBackB: (() -> Unit)?,
        val onPrevTabLB: (() -> Unit)?,
        val onNextTabRB: (() -> Unit)?,
        val onSecondaryX: (() -> Unit)?,
        val onSecondaryY: (() -> Unit)?
    )

    private val activeScopes = java.util.concurrent.CopyOnWriteArrayList<ControllerScopeCallbacks>()

    internal fun registerScope(scope: ControllerScopeCallbacks) {
        activeScopes.remove(scope)
        activeScopes.add(scope)
        activeAnalogDirection = null
        hatOrStickDirection = null
    }

    internal fun unregisterScope(scope: ControllerScopeCallbacks) {
        activeScopes.remove(scope)
        activeAnalogDirection = null
        hatOrStickDirection = null
    }

    /**
     * Nhả toàn bộ trạng thái giữ phím/cần điều hướng giao diện từ Ravno Pad (gọi khi trạng thái UDP về trung tính,
     * mất gói quá hạn watchdog hoặc ngắt kết nối Pad).
     */
    fun releaseAllRemotePadInputs() {
        val pendingDir = activeAnalogDirection
        val unconsumedSeq = remotePadPressSeq > lastConsumedRemotePadSeq
        activeAnalogDirection = null
        hatOrStickDirection = null
        if (pendingDir != null && unconsumedSeq && shouldRouteToAppUi) {
            lastConsumedRemotePadSeq = remotePadPressSeq
            activeScopes.lastOrNull()?.stepFocus?.invoke(pendingDir)
        }
        if (isSelectHeld || isStartHeld) {
            isSelectHeld = false
            isStartHeld = false
            comboFiredForCurrentHold = false
            mainHandler.removeCallbacks(comboHoldRunnable)
        }
    }

    /**
     * Điều khiển giao diện trực tiếp từ CloudPlay Pad (UDP) tới scope/hộp thoại đang mở trên cùng,
     * hoạt động kể cả khi Dialog tạo cửa sổ riêng hoặc khi WebView đang giữ focus gốc.
     */
    fun dispatchRemotePadButton(buttonIndex: Int, pressed: Boolean): Boolean {
        val dir = when (buttonIndex) {
            12 -> FocusDirection.Up
            13 -> FocusDirection.Down
            14 -> FocusDirection.Left
            15 -> FocusDirection.Right
            else -> null
        }
        if (dir != null) {
            if (!pressed) {
                val unconsumedSeq = remotePadPressSeq > lastConsumedRemotePadSeq
                if (activeAnalogDirection == dir) {
                    activeAnalogDirection = null
                }
                if (hatOrStickDirection == dir) {
                    hatOrStickDirection = null
                }
                if (unconsumedSeq && shouldRouteToAppUi) {
                    lastConsumedRemotePadSeq = remotePadPressSeq
                    activeScopes.lastOrNull()?.stepFocus?.invoke(dir)
                }
                return true
            }
            if (!shouldRouteToAppUi) return false
            if (activeScopes.isEmpty()) return false
            onGamepadInteraction()
            activeAnalogDirection = dir
            remotePadPressSeq++
            return true
        }
        if (!shouldRouteToAppUi) return false
        val topScope = activeScopes.lastOrNull() ?: return false
        if (!pressed) return true
        onGamepadInteraction()
        when (buttonIndex) {
            0 -> {
                topScope.onConfirmA()
                return true
            }
            1 -> {
                val back = topScope.onBackB
                if (back != null) {
                    back()
                    return true
                }
            }
            2 -> {
                val actX = topScope.onSecondaryX
                if (actX != null) {
                    actX()
                    return true
                }
            }
            3 -> {
                val keyY = activeFocusedKeyYHandler ?: topScope.onSecondaryY
                if (keyY != null) {
                    keyY()
                    return true
                }
            }
            4 -> {
                val prev = topScope.onPrevTabLB
                if (prev != null) {
                    prev()
                    return true
                }
            }
            5 -> {
                val next = topScope.onNextTabRB
                if (next != null) {
                    next()
                    return true
                }
            }
            9 -> {
                val longClick = activeFocusedLongClickHandler
                if (longClick != null) {
                    longClick()
                    return true
                }
            }
        }
        return false
    }

    /**
     * Điều khiển giao diện trực tiếp bằng cần gạt trái (Left Stick) từ CloudPlay Pad (UDP).
     */
    fun dispatchRemotePadStick(lx: Float, ly: Float): Boolean {
        if (!shouldRouteToAppUi) {
            if (activeAnalogDirection != null) {
                activeAnalogDirection = null
                hatOrStickDirection = null
            }
            return false
        }
        val deadzone = 0.48f
        val releaseZone = 0.30f
        val currentDir = hatOrStickDirection
        val threshold = if (currentDir != null) releaseZone else deadzone

        val nextDir: FocusDirection? = when {
            kotlin.math.abs(lx) < threshold && kotlin.math.abs(ly) < threshold -> null
            kotlin.math.abs(ly) >= kotlin.math.abs(lx) -> {
                if (ly < -threshold) FocusDirection.Up else FocusDirection.Down
            }
            else -> {
                if (lx < -threshold) FocusDirection.Left else FocusDirection.Right
            }
        }

        if (nextDir != hatOrStickDirection) {
            val prevDir = hatOrStickDirection
            val unconsumedSeq = remotePadPressSeq > lastConsumedRemotePadSeq
            hatOrStickDirection = nextDir
            activeAnalogDirection = nextDir
            if (nextDir != null) {
                onGamepadInteraction()
                remotePadPressSeq++
            } else if (prevDir != null && unconsumedSeq && shouldRouteToAppUi) {
                lastConsumedRemotePadSeq = remotePadPressSeq
                activeScopes.lastOrNull()?.stepFocus?.invoke(prevDir)
            }
        }
        return nextDir != null
    }

    /**
     * Dự phòng cấp Activity: khi không có node Compose nào đang giữ focus (ví dụ vừa mở app trên máy cảm ứng,
     * vừa cuộn mất phần tử đang chọn hoặc vừa đóng hộp thoại), chuyển sự kiện phím tay cầm trực tiếp vào
     * scope trên cùng để khôi phục focus ngay lập tức thay vì làm đơ tay cầm.
     */
    fun handleActivityKeyEvent(event: KeyEvent): Boolean {
        if (!shouldRouteToAppUi) return false
        if (!isFromGamepadOrKeyboard(event)) return false
        val pressed = event.action == KeyEvent.ACTION_DOWN
        if (!pressed && event.action != KeyEvent.ACTION_UP) return false
        if (pressed && event.repeatCount > 0) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> true
                else -> false
            }
        }
        val mappedButton = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> 12
            KeyEvent.KEYCODE_DPAD_DOWN -> 13
            KeyEvent.KEYCODE_DPAD_LEFT -> 14
            KeyEvent.KEYCODE_DPAD_RIGHT -> 15
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> 0
            KeyEvent.KEYCODE_BUTTON_B -> 1
            KeyEvent.KEYCODE_BUTTON_X -> 2
            KeyEvent.KEYCODE_BUTTON_Y -> 3
            KeyEvent.KEYCODE_BUTTON_L1 -> 4
            KeyEvent.KEYCODE_BUTTON_R1 -> 5
            KeyEvent.KEYCODE_BUTTON_START -> 9
            else -> return false
        }
        return dispatchRemotePadButton(mappedButton, pressed)
    }

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
        if (isGamepadModeActive && !keepFocusVisibleInTvMode) {
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
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
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
    val inputModeManager = androidx.compose.ui.platform.LocalInputModeManager.current
    val view = androidx.compose.ui.platform.LocalView.current
    val currentOnPrevTab by rememberUpdatedState(onPreviousTab)
    val currentOnNextTab by rememberUpdatedState(onNextTab)
    val currentOnBack by rememberUpdatedState(onBackPress)
    val currentOnX by rememberUpdatedState(onSecondaryX)
    val currentOnY by rememberUpdatedState(onSecondaryY)
    val currentDefaultRequester by rememberUpdatedState(defaultFocusRequester)

    var heldDpadDirection by remember { mutableStateOf<FocusDirection?>(null) }
    var dpadPressSeq by remember { mutableIntStateOf(0) }
    var hasFocusInsideScope by remember { mutableStateOf(false) }

    fun ensureComposeViewFocused() {
        GamepadUiNavigation.onGamepadInteraction()
        runCatching {
            if (!view.isFocusable) view.isFocusable = true
            if (!view.isFocusableInTouchMode) view.isFocusableInTouchMode = true
            inputModeManager.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
            if (!view.hasFocus()) {
                val ok = view.requestFocus()
                if (!ok) {
                    view.requestFocusFromTouch()
                }
            }
        }
    }

    fun stepFocus(direction: FocusDirection) {
        ensureComposeViewFocused()
        val consumedStepper = GamepadUiNavigation.activeHorizontalStepperConsumed
        if (consumedStepper != null && (direction == FocusDirection.Left || direction == FocusDirection.Right)) {
            val consumed = consumedStepper(if (direction == FocusDirection.Left) -1 else 1)
            if (consumed) return
        } else {
            val horizontalStepper = GamepadUiNavigation.activeHorizontalStepper
            if (horizontalStepper != null && (direction == FocusDirection.Left || direction == FocusDirection.Right)) {
                horizontalStepper(if (direction == FocusDirection.Left) -1 else 1)
                return
            }
        }
        if (!hasFocusInsideScope && GamepadUiNavigation.activeFocusedClickHandler == null) {
            val req = currentDefaultRequester
            if (req != null) {
                runCatching { req.requestFocus() }
                return
            }
        }
        val moved = runCatching { focusManager.moveFocus(direction) }.getOrDefault(false)
        if (!moved && GamepadUiNavigation.activeFocusedClickHandler == null) {
            runCatching { currentDefaultRequester?.requestFocus() }
        }
    }

    val scopeCallbacks = remember {
        GamepadUiNavigation.ControllerScopeCallbacks(
            stepFocus = { dir -> stepFocus(dir) },
            onConfirmA = {
                ensureComposeViewFocused()
                val clickHandler = GamepadUiNavigation.activeFocusedClickHandler
                if (clickHandler != null) {
                    clickHandler()
                } else {
                    runCatching { currentDefaultRequester?.requestFocus() }
                }
            },
            onBackB = { currentOnBack?.invoke() },
            onPrevTabLB = { currentOnPrevTab?.invoke() },
            onNextTabRB = { currentOnNextTab?.invoke() },
            onSecondaryX = { currentOnX?.invoke() },
            onSecondaryY = { currentOnY?.invoke() }
        )
    }

    DisposableEffect(enabled) {
        if (!enabled) {
            heldDpadDirection = null
            GamepadUiNavigation.unregisterScope(scopeCallbacks)
        } else {
            GamepadUiNavigation.registerScope(scopeCallbacks)
        }
        onDispose {
            heldDpadDirection = null
            GamepadUiNavigation.unregisterScope(scopeCallbacks)
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
            ensureComposeViewFocused()
            delay(45L)
            ensureComposeViewFocused()
            runCatching { defaultFocusRequester.requestFocus() }
        }
    }

    val effectiveHeldDirection = if (enabled) {
        heldDpadDirection ?: GamepadUiNavigation.activeAnalogDirection
    } else {
        null
    }

    val currentRemoteSeq = GamepadUiNavigation.remotePadPressSeq

    LaunchedEffect(effectiveHeldDirection, dpadPressSeq, currentRemoteSeq, enabled) {
        val dir = effectiveHeldDirection ?: return@LaunchedEffect
        if (!enabled) return@LaunchedEffect
        GamepadUiNavigation.lastConsumedRemotePadSeq = currentRemoteSeq
        stepFocus(dir)
        delay(300L)
        while (enabled && (heldDpadDirection == dir || GamepadUiNavigation.activeAnalogDirection == dir)) {
            stepFocus(dir)
            delay(105L)
        }
    }

    this
        .onFocusChanged { state ->
            hasFocusInsideScope = state.hasFocus || state.isFocused
        }
        .onKeyEvent { keyEvent ->
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
                        val clickHandler = GamepadUiNavigation.activeFocusedClickHandler
                        if (clickHandler != null && !GamepadUiNavigation.shouldIgnoreDuplicateDown(keyCode)) {
                            clickHandler()
                        } else if (!hasFocusInsideScope) {
                            runCatching { currentDefaultRequester?.requestFocus() }
                        }
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
    onStepHorizontalConsumed: ((Int) -> Boolean)? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var isFocused by remember { mutableStateOf(false) }
    var aButtonDown by remember { mutableStateOf(false) }
    val internalFocusRequester = remember { FocusRequester() }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnStepHorizontal by rememberUpdatedState(onStepHorizontal)
    val currentOnStepHorizontalConsumed by rememberUpdatedState(onStepHorizontalConsumed)
    val currentOnFocused by rememberUpdatedState(onFocused)
    val interactionSource = remember { MutableInteractionSource() }
    val selfClickDispatcher = remember { { currentOnClick() } }

    DisposableEffect(Unit) {
        onDispose {
            if (isFocused) {
                if (currentOnStepHorizontal != null) {
                    GamepadUiNavigation.activeHorizontalStepper = null
                }
                if (currentOnStepHorizontalConsumed != null) {
                    GamepadUiNavigation.activeHorizontalStepperConsumed = null
                }
                if (GamepadUiNavigation.activeFocusedClickHandler === selfClickDispatcher) {
                    GamepadUiNavigation.activeFocusedClickHandler = null
                }
            }
        }
    }

    this
        .focusRequester(internalFocusRequester)
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state ->
            val next = state.isFocused
            if (isFocused != next) {
                isFocused = next
                if (next) {
                    GamepadUiNavigation.activeFocusedClickHandler = selfClickDispatcher
                    GamepadUiNavigation.activeFocusedKeyYHandler = null
                    GamepadUiNavigation.activeFocusedLongClickHandler = null
                    if (currentOnStepHorizontalConsumed != null) {
                        GamepadUiNavigation.activeHorizontalStepperConsumed = { dir ->
                            currentOnStepHorizontalConsumed?.invoke(dir) ?: false
                        }
                        GamepadUiNavigation.activeHorizontalStepper = null
                    } else if (currentOnStepHorizontal != null) {
                        GamepadUiNavigation.activeHorizontalStepper = { dir ->
                            currentOnStepHorizontal?.invoke(dir)
                        }
                        GamepadUiNavigation.activeHorizontalStepperConsumed = null
                    } else {
                        GamepadUiNavigation.activeHorizontalStepper = null
                        GamepadUiNavigation.activeHorizontalStepperConsumed = null
                    }
                    currentOnFocused?.invoke()
                } else {
                    if (GamepadUiNavigation.activeFocusedClickHandler === selfClickDispatcher) {
                        GamepadUiNavigation.activeFocusedClickHandler = null
                    }
                    if (currentOnStepHorizontal != null) {
                        GamepadUiNavigation.activeHorizontalStepper = null
                    }
                    if (currentOnStepHorizontalConsumed != null) {
                        GamepadUiNavigation.activeHorizontalStepperConsumed = null
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
                if (GamepadUiNavigation.keepFocusVisibleInTvMode || GamepadUiNavigation.isGamepadModeActive) {
                    runCatching { internalFocusRequester.requestFocus() }
                }
                currentOnClick()
            }
        )
        .drawWithContent {
            drawContent()
            if (isFocused && (GamepadUiNavigation.isGamepadModeActive || GamepadUiNavigation.keepFocusVisibleInTvMode)) {
                val strokeWidthPx = 2.5.dp.toPx()
                val halfStroke = strokeWidthPx / 2f
                val radiusPx = cornerRadius.toPx()
                drawRoundRect(
                    color = focusBorderColor.copy(alpha = 0.12f),
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
    val selfClickDispatcher = remember { { currentOnClick() } }

    DisposableEffect(Unit) {
        onDispose {
            if (isFocused) {
                if (GamepadUiNavigation.activeFocusedClickHandler === selfClickDispatcher) {
                    GamepadUiNavigation.activeFocusedClickHandler = null
                }
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
                    GamepadUiNavigation.activeFocusedClickHandler = selfClickDispatcher
                    GamepadUiNavigation.activeFocusedKeyYHandler = null
                    GamepadUiNavigation.activeFocusedLongClickHandler = null
                    GamepadUiNavigation.activeHorizontalStepper = null
                    currentOnFocused?.invoke()
                } else {
                    if (GamepadUiNavigation.activeFocusedClickHandler === selfClickDispatcher) {
                        GamepadUiNavigation.activeFocusedClickHandler = null
                    }
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
    val selfClickDispatcher = remember { { currentOnClick() } }
    val selfKeyYDispatcher = remember { { currentOnKeyY?.invoke() ?: Unit } }
    val selfLongClickDispatcher = remember { { currentOnLongClick?.invoke() ?: Unit } }

    DisposableEffect(Unit) {
        onDispose {
            if (isFocused) {
                if (GamepadUiNavigation.activeFocusedClickHandler === selfClickDispatcher) {
                    GamepadUiNavigation.activeFocusedClickHandler = null
                }
                if (GamepadUiNavigation.activeFocusedKeyYHandler === selfKeyYDispatcher) {
                    GamepadUiNavigation.activeFocusedKeyYHandler = null
                }
                if (GamepadUiNavigation.activeFocusedLongClickHandler === selfLongClickDispatcher) {
                    GamepadUiNavigation.activeFocusedLongClickHandler = null
                }
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
                    GamepadUiNavigation.activeFocusedClickHandler = selfClickDispatcher
                    GamepadUiNavigation.activeFocusedKeyYHandler = if (currentOnKeyY != null) selfKeyYDispatcher else null
                    GamepadUiNavigation.activeFocusedLongClickHandler = if (currentOnLongClick != null) selfLongClickDispatcher else null
                    GamepadUiNavigation.activeHorizontalStepper = null
                    currentOnFocused?.invoke()
                } else {
                    if (GamepadUiNavigation.activeFocusedClickHandler === selfClickDispatcher) {
                        GamepadUiNavigation.activeFocusedClickHandler = null
                    }
                    if (GamepadUiNavigation.activeFocusedKeyYHandler === selfKeyYDispatcher) {
                        GamepadUiNavigation.activeFocusedKeyYHandler = null
                    }
                    if (GamepadUiNavigation.activeFocusedLongClickHandler === selfLongClickDispatcher) {
                        GamepadUiNavigation.activeFocusedLongClickHandler = null
                    }
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


