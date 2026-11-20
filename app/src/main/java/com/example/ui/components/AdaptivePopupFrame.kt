package com.example.ui.components

import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cloudplay.RemotePadTvServer
import com.example.data.AppLanguage
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import kotlinx.coroutines.delay

/**
 * Kiểm tra xem có tay cầm thật (USB / Bluetooth) đang cắm vào máy hay không.
 */
fun isPhysicalGamepadConnectedNow(): Boolean {
    return runCatching {
        val ids = InputDevice.getDeviceIds()
        for (id in ids) {
            val dev = InputDevice.getDevice(id) ?: continue
            if (dev.isVirtual) continue
            val src = dev.sources
            val isGamepad = (src and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val isJoystick = (src and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isGamepad || isJoystick) return@runCatching true
        }
        false
    }.getOrDefault(false)
}

/**
 * Ngữ cảnh kích thước tỉ lệ theo cửa sổ (Window-relative sizing) cho khung popup dùng chung.
 * Không dùng số dp/sp cứng: mọi khoảng cách, bo góc, cỡ chữ đều tính từ kích thước cửa sổ thực tế
 * và tự bù cỡ chữ hệ thống lớn (fontScale) để không bao giờ bị cắt chữ hoặc che nút.
 */
@Immutable
class AdaptivePopupScope(
    val winW: Dp,
    val winH: Dp,
    val isTvUiMode: Boolean,
    val isGamepadDetected: Boolean,
    val useTwoColumnLayout: Boolean,
    private val density: Density
) {
    private val shortEdge: Dp = if (winW < winH) winW else winH
    private val baseUnit: Dp = shortEdge / 360f
    private val minDpUnit: Dp = shortEdge / 1800f
    private val fontScaleCompensation: Float =
        if (density.fontScale > 1.12f) (1.12f / density.fontScale).coerceIn(0.72f, 1f) else 1f
    private val rawScaleFactor: Float =
        (shortEdge.value / 360f).coerceIn(0.76f, 1.16f) * fontScaleCompensation

    fun rDp(refPx: Float): Dp {
        val candidate = baseUnit * refPx
        val minBound = minDpUnit * refPx
        return if (candidate > minBound) candidate else minBound
    }

    fun rSp(refSp: Float): TextUnit = with(density) {
        (refSp * rawScaleFactor).sp
    }

    val borderThin: Dp get() = rDp(1.1f)
    val borderFocus: Dp get() = rDp(2.2f)
    val cardCorner: Dp get() = rDp(18f)
    val sectionCorner: Dp get() = rDp(12f)
    val btnCorner: Dp get() = rDp(10f)
    val chipCorner: Dp get() = rDp(8f)
    val padOuter: Dp get() = rDp(if (useTwoColumnLayout) 13f else 15f)
    val padInner: Dp get() = rDp(10f)
    val gapLarge: Dp get() = rDp(10f)
    val gapMed: Dp get() = rDp(7f)
    val gapSmall: Dp get() = rDp(4.5f)
    val iconBoxSize: Dp get() = rDp(34f)
    val iconSize: Dp get() = rDp(18f)
    val btnMinHeight: Dp get() = rDp(38f)
}

/**
 * Biểu tượng nhỏ gọn gợi ý nút tay cầm (A / B / X / Y) gắn trực tiếp trên nút popup
 * khi phát hiện có tay cầm (ở cả giao diện Điện thoại lẫn giao diện TV).
 */
@Composable
fun AdaptivePopupScope.GamepadKeyHintBadge(
    keyLabel: String,
    isOnBrightPrimary: Boolean = false
) {
    val badgeBg = when {
        isOnBrightPrimary -> Color(0xFF04150B).copy(alpha = 0.85f)
        keyLabel.equals("B", ignoreCase = true) -> JapanRed.copy(alpha = 0.24f)
        keyLabel.equals("A", ignoreCase = true) -> XboxNeonGreen.copy(alpha = 0.24f)
        else -> DirectCyan.copy(alpha = 0.22f)
    }
    val badgeBorder = when {
        isOnBrightPrimary -> Color(0xFF04150B)
        keyLabel.equals("B", ignoreCase = true) -> JapanRed
        keyLabel.equals("A", ignoreCase = true) -> XboxNeonGreen
        else -> DirectCyan
    }
    val badgeText = when {
        isOnBrightPrimary -> XboxNeonGreen
        keyLabel.equals("B", ignoreCase = true) -> Color(0xFFFFB4AB)
        keyLabel.equals("A", ignoreCase = true) -> XboxNeonGreen
        else -> DirectCyan
    }
    Box(
        modifier = Modifier
            .size(rDp(18f))
            .clip(CircleShape)
            .background(badgeBg)
            .border(rDp(1f), badgeBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = keyLabel,
            color = badgeText,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = rSp(9.5f),
            maxLines = 1
        )
    }
}

/**
 * Khung popup dùng chung cho toàn bộ ứng dụng:
 * - Bề rộng lớn khi ở giao diện TV (AppUiMode TV hoặc tự nhận diện) hoặc màn hình ngang/rộng và thấp.
 * - Tự động cung cấp cờ `useTwoColumnLayout` để chuyển sang bố cục 2 cột khi đủ rộng/TV,
 *   và giữ nguyên giao diện 1 cột dọc trên điện thoại dọc.
 * - Tự động nhận diện tay cầm (tay cầm thật USB/Bluetooth, tay cầm phụ Ravno Pad hoặc chế độ TV)
 *   để hiển thị huy hiệu gợi ý nút `[A]` (Xác nhận/Chọn) và `[B]` (Quay lại/Đóng) trên cả Điện thoại và TV.
 * - Giao diện phẳng hoàn toàn (không phát sáng, không làm mờ, không hoạt ảnh lặp).
 */
@Composable
fun AdaptivePopupFrame(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dialogTestTag: String? = null,
    defaultFocusRequester: FocusRequester? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    wideWidthFraction: Float = 0.94f,
    portraitWidthFraction: Float = 0.92f,
    wideMaxHeightFraction: Float = 0.92f,
    portraitMaxHeightFraction: Float = 0.90f,
    containerColor: Color = XboxSurfaceVariant,
    borderColor: Color = XboxCardBorder,
    onPreviousTab: (() -> Unit)? = null,
    onNextTab: (() -> Unit)? = null,
    onBackPress: (() -> Unit)? = onDismissRequest,
    onSecondaryY: (() -> Unit)? = null,
    content: @Composable AdaptivePopupScope.() -> Unit
) {
    val context = LocalContext.current
    val config by SettingsRepository.config.collectAsState()
    val remotePadServerState by RemotePadTvServer.state.collectAsState()
    val hasRemotePadConnected = remotePadServerState.connectedDeviceId.isNotBlank()
    val isTvUiMode = remember(config.appUiMode, context) {
        config.isEffectiveTvMode(context)
    }
    val hasHardwarePad = remember { isPhysicalGamepadConnectedNow() }
    val isGamepadDetected = isTvUiMode ||
        GamepadUiNavigation.isGamepadModeActive ||
        hasHardwarePad ||
        hasRemotePadConnected

    val internalDefaultFocus = remember { FocusRequester() }
    val activeDefaultFocus = defaultFocusRequester ?: internalDefaultFocus

    LaunchedEffect(isTvUiMode, isGamepadDetected, activeDefaultFocus) {
        if (isTvUiMode || hasHardwarePad || hasRemotePadConnected) {
            GamepadUiNavigation.onGamepadInteraction()
        }
        delay(60)
        runCatching { activeDefaultFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        GamepadSurfaceController(
            enabled = true,
            defaultFocusRequester = activeDefaultFocus,
            autoRequestDefaultFocus = true,
            attachWindowMotionListener = true,
            onPreviousTab = onPreviousTab,
            onNextTab = onNextTab,
            onBackPress = onBackPress,
            onSecondaryY = onSecondaryY
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val winW = maxWidth
                val winH = maxHeight
                val useTwoColumnLayout = isTvUiMode || (winW > winH)

                val scope = remember(winW, winH, isTvUiMode, isGamepadDetected, useTwoColumnLayout, density) {
                    AdaptivePopupScope(
                        winW = winW,
                        winH = winH,
                        isTvUiMode = isTvUiMode,
                        isGamepadDetected = isGamepadDetected,
                        useTwoColumnLayout = useTwoColumnLayout,
                        density = density
                    )
                }

                val cardWidthFraction = if (useTwoColumnLayout) wideWidthFraction else portraitWidthFraction
                val cardMaxHeight = winH * (if (useTwoColumnLayout) wideMaxHeightFraction else portraitMaxHeightFraction)

                Card(
                    modifier = modifier
                        .fillMaxWidth(cardWidthFraction)
                        .heightIn(max = cardMaxHeight)
                        .border(scope.borderThin, borderColor, RoundedCornerShape(scope.cardCorner))
                        .then(
                            if (!dialogTestTag.isNullOrBlank()) Modifier.testTag(dialogTestTag)
                            else Modifier
                        ),
                    shape = RoundedCornerShape(scope.cardCorner),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = scope.rDp(0f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(scope.padOuter)
                    ) {
                        scope.content()
                    }
                }
            }
        }
    }
}

/**
 * Bố cục 2 cột tự động cho `AdaptivePopupFrame`:
 * - Khi `useTwoColumnLayout == true` (TV hoặc màn ngang): xếp 2 cột trái/phải với vách ngăn phẳng mảnh ở giữa.
 * - Khi `useTwoColumnLayout == false` (Điện thoại dọc): giữ nguyên bố cục 1 cột dọc.
 */
@Composable
fun AdaptivePopupScope.AdaptiveTwoColumnLayout(
    leftWeight: Float = 1.08f,
    rightWeight: Float = 0.92f,
    dividerColor: Color = XboxCardBorder,
    leftColumn: @Composable AdaptivePopupScope.() -> Unit,
    rightColumn: @Composable AdaptivePopupScope.() -> Unit,
    portraitColumn: @Composable AdaptivePopupScope.() -> Unit
) {
    if (useTwoColumnLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gapLarge),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(leftWeight),
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                leftColumn()
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .size(width = borderThin, height = rDp(180f))
                    .background(dividerColor)
            )

            Column(
                modifier = Modifier.weight(rightWeight),
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                rightColumn()
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gapMed)
        ) {
            portraitColumn()
        }
    }
}

/**
 * Nút hành động phẳng dùng chung cho các popup, có vòng chọn D-Pad phẳng rõ nét (không phát sáng, không đổ bóng).
 * Khi phát hiện tay cầm (trên cả giao diện Điện thoại lẫn TV), tự động hiện biểu tượng gợi ý nút nhỏ gọn:
 * - Nút đang focus hoặc nút xác nhận chính hiện huy hiệu `(A)`.
 * - Nút hủy / đóng / quay lại (`gamepadHint = "B"`) hiện huy hiệu `(B)`.
 */
@Composable
fun AdaptivePopupScope.AdaptiveFlatActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    gamepadHint: String? = null,
    containerColor: Color = if (isPrimary) XboxNeonGreen else XboxSurface,
    contentColor: Color = if (isPrimary) Color(0xFF04150B) else TextPrimary,
    borderColor: Color = if (isPrimary) XboxNeonGreen else XboxCardBorder,
    focusedBorderColor: Color = if (isPrimary) Color.White else XboxNeonGreen,
    focusedContainerColor: Color = if (isPrimary) Color(0xFF39F296) else Color(0xFF193226),
    focusRequester: FocusRequester? = null,
    content: @Composable RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val resolvedBg = when {
        !enabled -> XboxSurface.copy(alpha = 0.45f)
        isFocused -> focusedContainerColor
        else -> containerColor
    }
    val resolvedBorderColor = when {
        !enabled -> XboxCardBorder.copy(alpha = 0.4f)
        isFocused -> focusedBorderColor
        else -> borderColor
    }
    val resolvedBorderWidth = if (isFocused) borderFocus else borderThin

    val effectiveHintBadge: String? = when {
        !enabled || !isGamepadDetected -> null
        !gamepadHint.isNullOrBlank() -> gamepadHint
        isFocused || isPrimary -> "A"
        else -> null
    }

    Surface(
        modifier = modifier
            .heightIn(min = btnMinHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    GamepadUiNavigation.onGamepadInteraction()
                }
            }
            .onKeyEvent { event ->
                if (!enabled) return@onKeyEvent false
                val code = event.nativeKeyEvent.keyCode
                val isConfirm = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_BUTTON_A ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER
                if (isConfirm && event.type == KeyEventType.KeyUp) {
                    GamepadUiNavigation.onGamepadInteraction()
                    onClick()
                    true
                } else if (isConfirm && event.type == KeyEventType.KeyDown) {
                    GamepadUiNavigation.onGamepadInteraction()
                    true
                } else {
                    false
                }
            }
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clip(RoundedCornerShape(btnCorner))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(btnCorner),
        color = resolvedBg,
        contentColor = if (enabled) contentColor else TextSecondary,
        border = BorderStroke(resolvedBorderWidth, resolvedBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = padInner, vertical = gapMed),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            if (effectiveHintBadge != null) {
                Spacer(modifier = Modifier.width(rDp(5f)))
                GamepadKeyHintBadge(
                    keyLabel = effectiveHintBadge,
                    isOnBrightPrimary = isPrimary && effectiveHintBadge.equals("A", ignoreCase = true)
                )
            }
        }
    }
}

/**
 * Hộp thoại xác nhận dùng chung (Confirm Dialog):
 * - Ở TV hoặc màn ngang: bố cục 2 cột rộng rãi (Cột trái: biểu tượng, tiêu đề, nội dung cuộn được; Cột phải: nút xác nhận và nút hủy luôn thấy ngay).
 * - Ở điện thoại dọc: bố cục 1 cột dọc truyền thống.
 * - Khi phát hiện tay cầm (trên cả giao diện Điện thoại lẫn TV): nút Xác nhận hiện huy hiệu `(A)` và nút Quay lại / Hủy hiện huy hiệu `(B)`.
 */
@Composable
fun AdaptiveConfirmPopupDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dialogTestTag: String? = null,
    confirmButtonTestTag: String? = null,
    dismissButtonTestTag: String? = null,
    confirmContentDescription: String? = null,
    dismissContentDescription: String? = null,
    confirmContainerColor: Color = XboxNeonGreen,
    confirmContentColor: Color = Color(0xFF04150B),
    defaultFocusOnDismiss: Boolean = false,
    iconContent: (@Composable AdaptivePopupScope.() -> Unit)? = null
) {
    val confirmFocusRequester = remember { FocusRequester() }
    val dismissFocusRequester = remember { FocusRequester() }
    val defaultRequester = if (defaultFocusOnDismiss) dismissFocusRequester else confirmFocusRequester

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = dialogTestTag,
        defaultFocusRequester = defaultRequester,
        wideWidthFraction = 0.78f,
        portraitWidthFraction = 0.90f
    ) {
        val msgScroll = rememberScrollState()

        @Composable
        fun HeaderAndMessage(modifier: Modifier = Modifier) {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    if (iconContent != null) {
                        iconContent()
                    }
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = rSp(15.5f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = winH * 0.48f)
                        .verticalScroll(msgScroll)
                ) {
                    Text(
                        text = message,
                        color = TextSecondary,
                        fontSize = rSp(12.5f),
                        lineHeight = rSp(17.5f)
                    )
                }
            }
        }

        val confirmModifierExtra = Modifier
            .then(
                if (!confirmContentDescription.isNullOrBlank()) {
                    Modifier.semantics { contentDescription = confirmContentDescription }
                } else Modifier
            )
            .then(
                if (!confirmButtonTestTag.isNullOrBlank()) Modifier.testTag(confirmButtonTestTag)
                else Modifier
            )

        val dismissModifierExtra = Modifier
            .then(
                if (!dismissContentDescription.isNullOrBlank()) {
                    Modifier.semantics { contentDescription = dismissContentDescription }
                } else Modifier
            )
            .then(
                if (!dismissButtonTestTag.isNullOrBlank()) Modifier.testTag(dismissButtonTestTag)
                else Modifier
            )

        if (useTwoColumnLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderAndMessage(modifier = Modifier.weight(1.25f))
                Box(
                    modifier = Modifier
                        .size(width = borderThin, height = rDp(96f))
                        .background(XboxCardBorder)
                )
                Column(
                    modifier = Modifier.weight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    AdaptiveFlatActionButton(
                        onClick = onConfirm,
                        isPrimary = true,
                        gamepadHint = "A",
                        containerColor = confirmContainerColor,
                        contentColor = confirmContentColor,
                        borderColor = confirmContainerColor,
                        focusedContainerColor = confirmContainerColor,
                        focusedBorderColor = Color.White,
                        focusRequester = confirmFocusRequester,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(confirmModifierExtra)
                    ) {
                        Text(
                            text = confirmText,
                            color = confirmContentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = rSp(12.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AdaptiveFlatActionButton(
                        onClick = onDismiss,
                        isPrimary = false,
                        gamepadHint = "B",
                        focusRequester = dismissFocusRequester,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(dismissModifierExtra)
                    ) {
                        Text(
                            text = dismissText,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = rSp(12.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gapLarge)
            ) {
                HeaderAndMessage(modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    AdaptiveFlatActionButton(
                        onClick = onDismiss,
                        isPrimary = false,
                        gamepadHint = "B",
                        focusRequester = dismissFocusRequester,
                        modifier = Modifier
                            .weight(1f)
                            .then(dismissModifierExtra)
                    ) {
                        Text(
                            text = dismissText,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = rSp(12.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AdaptiveFlatActionButton(
                        onClick = onConfirm,
                        isPrimary = true,
                        gamepadHint = "A",
                        containerColor = confirmContainerColor,
                        contentColor = confirmContentColor,
                        borderColor = confirmContainerColor,
                        focusedContainerColor = confirmContainerColor,
                        focusedBorderColor = Color.White,
                        focusRequester = confirmFocusRequester,
                        modifier = Modifier
                            .weight(1f)
                            .then(confirmModifierExtra)
                    ) {
                        Text(
                            text = confirmText,
                            color = confirmContentColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = rSp(12.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hộp thoại Đổi nút tay cầm thật dùng chung cho Cài đặt (Điện thoại & TV):
 * - Ở TV / màn ngang: Cột trái gồm Tiêu đề, hướng dẫn và nút Đóng; Cột phải là lưới nút 4 cột chọn bằng D-Pad.
 * - Ở điện thoại dọc: Bố cục 1 cột dọc với lưới nút 3 cột.
 */
@Composable
fun AdaptiveGamepadRemapDialog(
    srcIdx: Int,
    isEn: Boolean,
    physicalGamepadButtonMap: String,
    onSelectDestButton: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val srcName = SettingsRepository.getGamepadButtonLabel(srcIdx, isEn)
    val currentMap = remember(physicalGamepadButtonMap) {
        SettingsRepository.parsePhysicalButtonMap(physicalGamepadButtonMap)
    }
    val currentDest = currentMap[srcIdx] ?: srcIdx
    val closeFocusRequester = remember { FocusRequester() }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "gamepad_remap_dialog",
        defaultFocusRequester = closeFocusRequester,
        wideWidthFraction = 0.88f,
        portraitWidthFraction = 0.92f
    ) {
        val gridScroll = rememberScrollState()
        val columnsPerRow = if (useTwoColumnLayout) 4 else 3

        @Composable
        fun HeaderAndExplanation() {
            Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                Text(
                    text = if (isEn) "Remap Physical Button: $srcName" else "Đổi nút tay cầm thật: $srcName",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(15.5f)
                )
                Text(
                    text = if (isEn) {
                        "Select which Xbox button signal is sent when you press $srcName on your physical controller:"
                    } else {
                        "Chọn tín hiệu nút Xbox sẽ gửi vào game khi bạn bấm nút $srcName trên tay cầm thật:"
                    },
                    color = TextSecondary,
                    fontSize = rSp(11.5f),
                    lineHeight = rSp(16f)
                )
            }
        }

        @Composable
        fun CloseBtn() {
            AdaptiveFlatActionButton(
                onClick = onDismiss,
                isPrimary = false,
                gamepadHint = "B",
                focusRequester = closeFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEn) "Close button remap dialog" else "Đóng hộp thoại đổi nút"
                    }
            ) {
                Text(
                    text = if (isEn) "Close" else "Đóng",
                    color = XboxNeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = rSp(12f)
                )
            }
        }

        @Composable
        fun ButtonsGrid(maxGridHeight: Dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxGridHeight)
                    .verticalScroll(gridScroll),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                SettingsRepository.STANDARD_GAMEPAD_BUTTONS.chunked(columnsPerRow).forEach { rowBtns ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gapSmall)
                    ) {
                        rowBtns.forEach { (dstIdx, dstShort, _) ->
                            val selected = dstIdx == currentDest
                            AdaptiveFlatActionButton(
                                onClick = {
                                    onSelectDestButton(dstIdx)
                                    onDismiss()
                                },
                                isPrimary = selected,
                                containerColor = if (selected) Color(0xFF16352A) else XboxSurface,
                                contentColor = if (selected) XboxNeonGreen else TextPrimary,
                                borderColor = if (selected) XboxNeonGreen else XboxCardBorder,
                                focusedBorderColor = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = if (isEn) "Map $srcName to $dstShort" else "Gán $srcName thành $dstShort"
                                    }
                            ) {
                                Text(
                                    text = dstShort,
                                    color = if (selected) XboxNeonGreen else TextPrimary,
                                    fontSize = rSp(11f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(columnsPerRow - rowBtns.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 0.82f,
            rightWeight = 1.18f,
            leftColumn = {
                HeaderAndExplanation()
                CloseBtn()
            },
            rightColumn = {
                ButtonsGrid(maxGridHeight = winH * 0.72f)
            },
            portraitColumn = {
                HeaderAndExplanation()
                ButtonsGrid(maxGridHeight = winH * 0.56f)
                CloseBtn()
            }
        )
    }
}

/**
 * Hộp thoại Chọn nút kích hoạt phím tắt (View + Nút) dùng chung cho Cài đặt (Điện thoại & TV).
 */
@Composable
fun AdaptiveGamepadShortcutDialog(
    isEn: Boolean,
    onSelectButton: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val selectableButtons = remember {
        SettingsRepository.STANDARD_GAMEPAD_BUTTONS.filter { it.first != 8 && it.first != 16 }
    }
    val closeFocusRequester = remember { FocusRequester() }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "gamepad_shortcut_dialog",
        defaultFocusRequester = closeFocusRequester,
        wideWidthFraction = 0.88f,
        portraitWidthFraction = 0.92f
    ) {
        val gridScroll = rememberScrollState()
        val columnsPerRow = 3

        @Composable
        fun HeaderAndExplanation() {
            Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                Text(
                    text = if (isEn) "Select Trigger Button (View + Button)" else "Chọn nút kích hoạt (Giữ View + nút)",
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(15.5f)
                )
                Text(
                    text = if (isEn) {
                        "Choose the secondary button to press while holding View (Back) for 400ms:"
                    } else {
                        "Chọn nút thứ hai để bấm khi đang giữ nút View (Back) từ 400ms trở lên:"
                    },
                    color = TextSecondary,
                    fontSize = rSp(11.5f),
                    lineHeight = rSp(16f)
                )
            }
        }

        @Composable
        fun CloseBtn() {
            AdaptiveFlatActionButton(
                onClick = onDismiss,
                isPrimary = false,
                gamepadHint = "B",
                focusRequester = closeFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEn) "Close shortcut button dialog" else "Đóng hộp thoại chọn phím tắt"
                    }
            ) {
                Text(
                    text = if (isEn) "Close" else "Đóng",
                    color = XboxNeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = rSp(12f)
                )
            }
        }

        @Composable
        fun ShortcutGrid(maxGridHeight: Dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxGridHeight)
                    .verticalScroll(gridScroll),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                selectableButtons.chunked(columnsPerRow).forEach { rowBtns ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gapSmall)
                    ) {
                        rowBtns.forEach { (btnIdx, btnShort, _) ->
                            AdaptiveFlatActionButton(
                                onClick = {
                                    onSelectButton(btnIdx)
                                    onDismiss()
                                },
                                isPrimary = false,
                                containerColor = XboxSurface,
                                contentColor = DirectCyan,
                                borderColor = DirectCyan,
                                focusedBorderColor = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = if (isEn) "Set shortcut to View plus $btnShort" else "Đặt phím tắt là View cộng $btnShort"
                                    }
                            ) {
                                Text(
                                    text = "View + $btnShort",
                                    color = DirectCyan,
                                    fontSize = rSp(10.8f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(columnsPerRow - rowBtns.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 0.82f,
            rightWeight = 1.18f,
            leftColumn = {
                HeaderAndExplanation()
                CloseBtn()
            },
            rightColumn = {
                ShortcutGrid(maxGridHeight = winH * 0.72f)
            },
            portraitColumn = {
                HeaderAndExplanation()
                ShortcutGrid(maxGridHeight = winH * 0.56f)
                CloseBtn()
            }
        )
    }
}

/**
 * Hộp thoại chọn Ngôn ngữ ứng dụng (12 ngôn ngữ) dùng chung cho TV / màn ngang / dọc.
 */
@Composable
fun AdaptiveLanguagePickerDialog(
    currentLanguage: AppLanguage,
    isEnUi: Boolean,
    titleText: String,
    closeText: String,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "language_picker_dialog",
        defaultFocusRequester = selectedFocusRequester,
        wideWidthFraction = 0.88f,
        portraitWidthFraction = 0.90f
    ) {
        val listScroll = rememberScrollState()
        val columnsPerRow = if (useTwoColumnLayout) 2 else 1

        @Composable
        fun HeaderSection() {
            Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                Text(
                    text = titleText,
                    color = TextPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(16f)
                )
            }
        }

        @Composable
        fun CloseBtn() {
            AdaptiveFlatActionButton(
                onClick = onDismiss,
                isPrimary = false,
                gamepadHint = "B",
                focusRequester = closeFocusRequester,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = closeText,
                    color = XboxNeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = rSp(12f)
                )
            }
        }

        @Composable
        fun LanguageGrid(maxGridHeight: Dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxGridHeight)
                    .verticalScroll(listScroll),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                AppLanguage.entries.chunked(columnsPerRow).forEach { rowLangs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gapSmall)
                    ) {
                        rowLangs.forEach { langOption ->
                            val isSelected = currentLanguage == langOption
                            AdaptiveFlatActionButton(
                                onClick = { onSelectLanguage(langOption) },
                                isPrimary = isSelected,
                                containerColor = if (isSelected) Color(0xFF16352A) else XboxSurface,
                                contentColor = if (isSelected) XboxNeonGreen else TextPrimary,
                                borderColor = if (isSelected) XboxNeonGreen else XboxCardBorder,
                                focusedBorderColor = Color.White,
                                focusRequester = if (isSelected) selectedFocusRequester else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                                ) {
                                    Text(text = langOption.flag, fontSize = rSp(15f))
                                    Text(
                                        text = langOption.localizedTitle(isEnUi),
                                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = rSp(12f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        repeat(columnsPerRow - rowLangs.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 0.68f,
            rightWeight = 1.32f,
            leftColumn = {
                HeaderSection()
                CloseBtn()
            },
            rightColumn = {
                LanguageGrid(maxGridHeight = winH * 0.72f)
            },
            portraitColumn = {
                HeaderSection()
                LanguageGrid(maxGridHeight = winH * 0.60f)
                CloseBtn()
            }
        )
    }
}
