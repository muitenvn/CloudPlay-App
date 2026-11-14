package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

// Exact flat dark tokens matching hud-mockup.html
internal val HudBg = Color(0xFF070B09)
internal val HudSurface = Color(0xFF0D1511)
internal val HudSurface2 = Color(0xFF142019)
internal val HudSelBg = Color(0xFF16352A)
internal val HudLine = Color(0xFF1C2C24)
internal val HudInk = Color(0xFFF4F9F6)
internal val HudMuted = Color(0xFFA5B8AE)
internal val HudFaint = Color(0xFF6F8780)
internal val HudAccent = Color(0xFF27D884)
internal val HudOnAccent = Color(0xFF03140B)
internal val HudCyan = Color(0xFF3FD8EE)
internal val HudWarn = Color(0xFFE3B341)
internal val HudWarnTagBg = Color(0xFF2A2208)
internal val HudWarnNoteBg = Color(0xFF1D1808)
internal val HudWarnNoteText = Color(0xFFEFD9A0)
internal val HudDanger = Color(0xFFF0706B)
internal val HudDangerBg = Color(0xFF331A19)
internal val HudDangerOn = Color(0xFF2A0A09)
internal val HudPanelBg = Color(0xF50A100D) // rgba(10,16,13,.96)
internal val HudDialogBody = Color(0xFFC5D6D0)
internal val HudFootBorder = Color(0x0FFFFFFF) // rgba(255,255,255,.06)
internal val HudSwitchKnobOff = Color(0xFFCFE0DA)
internal val HudScrimColor = Color(0x8C000000) // rgba(0,0,0,.55)

// Virtual gamepad mockup palette
internal val PadBtnBg = Color(0x8C0D1511) // rgba(13,21,17,.55)
internal val PadBtnBorder = Color(0x33FFFFFF) // rgba(255,255,255,.2)
internal val PadBtnText = Color(0xFFE8F1EE)
internal val PadStickBg = Color(0x660D1511) // rgba(13,21,17,.4)
internal val PadKnobBg = Color(0x38FFFFFF) // rgba(255,255,255,.22)
internal val PadKnobBorder = Color(0x4DFFFFFF) // rgba(255,255,255,.3)
internal val PadYColor = Color(0xFFE0B44C)
internal val PadAColor = HudAccent
internal val PadXColor = HudCyan
internal val PadBColor = Color(0xFFEA6D6A)

internal enum class HudTagTier { GOOD, MID, BAD, NEUTRAL }

internal enum class HudButtonStyle { DEFAULT, MAIN, DANGER }

@Composable
internal fun HudPingDotIndicator(
    pingMs: Int?,
    hasSample: Boolean,
    fontSize: TextUnit = 13.sp,
    showMsSuffix: Boolean = true,
    modifier: Modifier = Modifier
) {
    val valid = hasSample && pingMs != null && pingMs > 0
    val dotColor = when {
        !valid -> HudFaint
        pingMs < 80 -> HudCyan
        pingMs <= 120 -> HudWarn
        else -> HudDanger
    }
    val text = when {
        !valid -> "—"
        showMsSuffix -> "$pingMs ms"
        else -> "$pingMs"
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            color = HudInk,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1
        )
    }
}

@Composable
internal fun HudTagChip(
    text: String,
    tier: HudTagTier,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tier) {
        HudTagTier.GOOD -> HudSelBg to HudAccent
        HudTagTier.MID -> HudWarnTagBg to HudWarn
        HudTagTier.BAD -> HudDangerBg to HudDanger
        HudTagTier.NEUTRAL -> HudSurface2 to HudMuted
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun HudSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = HudMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 2.dp, top = 2.dp)
    )
}

@Composable
internal fun HudSurfaceRow(
    modifier: Modifier = Modifier,
    minHeight: Dp = 38.dp,
    bgColor: Color = HudSurface,
    onStepHorizontal: ((Int) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (onClick != null) {
                    Modifier.gamepadClickable(
                        cornerRadius = 12.dp,
                        onStepHorizontal = onStepHorizontal,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
internal fun HudSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    enableGamepadFocus: Boolean = true,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) HudAccent else HudLine,
        animationSpec = tween(120),
        label = "hud_switch_track"
    )
    val thumbColor = if (checked) HudOnAccent else HudSwitchKnobOff
    val thumbOffsetX = if (checked) 16.dp else 0.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thumbOffsetPx = with(density) { thumbOffsetX.toPx().roundToInt() }

    Box(
        modifier = modifier
            .size(width = 42.dp, height = 26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .then(
                if (enableGamepadFocus) {
                    Modifier.gamepadClickable(
                        cornerRadius = 999.dp,
                        onStepHorizontal = { dir ->
                            val next = dir > 0
                            if (next != checked) onCheckedChange(next)
                        }
                    ) { onCheckedChange(!checked) }
                } else {
                    Modifier.clickable(
                        role = Role.Switch,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!checked) }
                }
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx, 0) }
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
internal fun HudSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    bgColor: Color = HudSurface,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    HudSurfaceRow(
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        minHeight = if (subtitle != null) 46.dp else 40.dp,
        bgColor = bgColor,
        onStepHorizontal = { dir ->
            val next = dir > 0
            if (next != checked) onCheckedChange(next)
        },
        onClick = { onCheckedChange(!checked) }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = HudFaint,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HudSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = title,
            enableGamepadFocus = false
        )
    }
}

@Composable
internal fun HudSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = HudSurface,
    minButtonHeight: Dp = 34.dp,
    fontSize: TextUnit = 12.sp,
    tagPrefix: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minButtonHeight)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) HudSelBg else Color.Transparent)
                    .gamepadClickable(
                        cornerRadius = 9.dp,
                        onStepHorizontal = { dir ->
                            val next = (selectedIndex + dir).coerceIn(0, options.lastIndex)
                            if (next != selectedIndex) onSelect(next)
                        }
                    ) { onSelect(idx) }
                    .then(if (tagPrefix != null) Modifier.testTag("${tagPrefix}_$idx") else Modifier)
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) HudAccent else HudMuted,
                    fontSize = fontSize,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun HudSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    bgColor: Color = HudSurface,
    accentColor: Color = HudAccent,
    modifier: Modifier = Modifier
) {
    val stepSize = ((valueRange.endInclusive - valueRange.start) / 20f).coerceAtLeast(0.01f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .gamepadClickable(
                cornerRadius = 12.dp,
                focusBorderColor = accentColor,
                onStepHorizontal = { dir ->
                    val next = (value + dir * stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(next)
                },
                onClick = {
                    val next = if (value + stepSize > valueRange.endInclusive) {
                        valueRange.start
                    } else {
                        (value + stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
                    }
                    onValueChange(next)
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = HudInk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valueText,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .then(
                    androidx.compose.ui.Modifier.focusProperties { canFocus = false }
                ),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = HudLine
            )
        )
    }
}

@Composable
internal fun HudButton(
    text: String,
    onClick: () -> Unit,
    style: HudButtonStyle = HudButtonStyle.DEFAULT,
    minHeight: Dp = 38.dp,
    fontSize: TextUnit = 12.5.sp,
    containerOverride: Color? = null,
    contentOverride: Color? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (style) {
        HudButtonStyle.MAIN -> HudAccent to HudOnAccent
        HudButtonStyle.DANGER -> HudDangerBg to HudDanger
        HudButtonStyle.DEFAULT -> HudSurface2 to HudInk
    }
    val finalBg = containerOverride ?: bg
    val finalFg = contentOverride ?: fg

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(finalBg)
            .gamepadClickable(cornerRadius = 12.dp, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = finalFg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = finalFg,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HudNoteCard(
    text: String,
    isWarning: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bg = if (isWarning) HudWarnNoteBg else HudSurface
    val fg = if (isWarning) HudWarnNoteText else HudMuted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.gamepadClickable(cornerRadius = 12.dp, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isWarning) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = HudWarn,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun HudStepperControl(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    btnBg: Color = HudSurface2,
    modifier: Modifier = Modifier,
    decTag: String? = null,
    incTag: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .gamepadClickable(
                    cornerRadius = 9.dp,
                    onStepHorizontal = { dir -> if (dir < 0) onDecrement() else onIncrement() },
                    onClick = onDecrement
                )
                .then(if (decTag != null) Modifier.testTag(decTag) else Modifier),
            shape = RoundedCornerShape(9.dp),
            color = btnBg,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("−", color = HudInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Text(
            text = valueText,
            color = HudInk,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .gamepadClickable(
                    cornerRadius = 9.dp,
                    onStepHorizontal = { dir -> if (dir < 0) onDecrement() else onIncrement() },
                    onClick = onIncrement
                )
                .then(if (incTag != null) Modifier.testTag(incTag) else Modifier),
            shape = RoundedCornerShape(9.dp),
            color = btnBg,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("+", color = HudInk, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun HudPanelIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 34.dp,
    bgColor: Color = HudSurface2,
    tint: Color = HudMuted,
    border: BorderStroke? = null,
    enableGamepadFocus: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enableGamepadFocus) {
                    Modifier.gamepadClickable(cornerRadius = 12.dp, onClick = onClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = border,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
