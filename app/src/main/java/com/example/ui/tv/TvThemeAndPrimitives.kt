package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.ui.DashboardTab
import com.example.ui.components.gamepadClickable
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import kotlin.math.min

// Bảng màu phẳng theo tv-mockup.html & giao diện hiện tại
internal val TvBg = Color(0xFF070B09)
internal val TvSurface = Color(0xFF0D1511)
internal val TvSurface2 = Color(0xFF142019)
internal val TvSel = Color(0xFF16352A)
internal val TvAccent = Color(0xFF27D884)
internal val TvCyan = Color(0xFF3FD8EE)
internal val TvWarn = Color(0xFFE3B341)
internal val TvDanger = Color(0xFFF0706B)
internal val TvDangerBg = Color(0xFF3A1A19)
internal val TvInk = Color(0xFFEAF3EE)
internal val TvMuted = Color(0xFF8FA79B)
internal val TvLine = Color(0xFF1D2B24)
internal val TvOnAccent = Color(0xFF04130B)

internal val TvKeyABg = Color(0xFF1D5E3F)
internal val TvKeyBBg = Color(0xFF6A2D2B)
internal val TvKeyXBg = Color(0xFF1F4F63)
internal val TvKeyYBg = Color(0xFF6B5618)

enum class TvButtonVariant {
    DEFAULT,
    PRIMARY,
    DANGER
}

enum class TvPillTone {
    NEUTRAL,
    GREEN,
    CYAN,
    WARN
}

data class TvKeyHint(
    val badges: List<Pair<String, Color>>,
    val label: String
)

internal fun computeTvScale(maxWidth: Dp, maxHeight: Dp, fontScale: Float): Float {
    val wRatio = maxWidth.value / 960f
    val hRatio = maxHeight.value / 540f
    val rawScale = min(wRatio, hRatio).coerceIn(0.62f, 1.35f)
    val safeFontDiv = fontScale.coerceIn(0.9f, 1.25f)
    return (rawScale / safeFontDiv).coerceIn(0.60f, 1.30f)
}

@Composable
internal fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    variant: TvButtonVariant = TvButtonVariant.DEFAULT,
    leadingText: String? = null,
    focusRequester: FocusRequester? = null,
    heightDp: Dp = (50.dp * scale).coerceAtLeast(48.dp)
) {
    val bgColor = when (variant) {
        TvButtonVariant.PRIMARY -> TvAccent
        TvButtonVariant.DANGER -> TvDangerBg
        TvButtonVariant.DEFAULT -> TvSurface2
    }
    val fgColor = when (variant) {
        TvButtonVariant.PRIMARY -> TvOnAccent
        TvButtonVariant.DANGER -> TvDanger
        TvButtonVariant.DEFAULT -> TvInk
    }
    val corner = (14.dp * scale).coerceAtLeast(10.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = heightDp)
            .clip(RoundedCornerShape(corner))
            .background(bgColor)
            .gamepadClickable(
                cornerRadius = corner,
                focusRequester = focusRequester,
                onClick = onClick
            )
            .padding(horizontal = (22.dp * scale).coerceAtLeast(14.dp), vertical = (8.dp * scale).coerceAtLeast(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!leadingText.isNullOrBlank()) {
            Text(
                text = leadingText,
                color = fgColor,
                fontSize = (16f * scale).coerceAtLeast(13f).sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width((8.dp * scale).coerceAtLeast(5.dp)))
        }
        Text(
            text = text,
            color = fgColor,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (17f * scale).coerceAtLeast(13.5f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TvChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    focusRequester: FocusRequester? = null
) {
    val bgColor = if (selected) TvSel else TvSurface
    val fgColor = if (selected) TvAccent else TvMuted
    val heightDp = (40.dp * scale).coerceAtLeast(36.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = heightDp)
            .clip(CircleShape)
            .background(bgColor)
            .gamepadClickable(
                cornerRadius = 99.dp,
                focusRequester = focusRequester,
                onClick = onClick
            )
            .padding(horizontal = (16.dp * scale).coerceAtLeast(12.dp), vertical = (6.dp * scale).coerceAtLeast(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fgColor,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (15f * scale).coerceAtLeast(12.5f).sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TvPill(
    text: String,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    tone: TvPillTone = TvPillTone.NEUTRAL,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, fgColor) = when (tone) {
        TvPillTone.GREEN -> Color(0xFF10301F) to TvAccent
        TvPillTone.CYAN -> Color(0xFF0E2A31) to TvCyan
        TvPillTone.WARN -> Color(0xFF33290C) to TvWarn
        TvPillTone.NEUTRAL -> TvSurface2 to TvMuted
    }
    val minH = (30.dp * scale).coerceAtLeast(26.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minH)
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (onClick != null) {
                    Modifier.gamepadClickable(cornerRadius = 99.dp, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = (12.dp * scale).coerceAtLeast(9.dp), vertical = (4.dp * scale).coerceAtLeast(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fgColor,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (13.5f * scale).coerceAtLeast(11.5f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun TvSwitch(
    checked: Boolean,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val trackW = (56.dp * scale).coerceAtLeast(46.dp)
    val trackH = (32.dp * scale).coerceAtLeast(26.dp)
    val knobSize = (24.dp * scale).coerceAtLeast(18.dp)
    val pad = (4.dp * scale).coerceAtLeast(3.dp)

    Box(
        modifier = modifier
            .size(width = trackW, height = trackH)
            .clip(CircleShape)
            .background(if (checked) TvAccent else Color(0xFF243029))
            .padding(horizontal = pad),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(knobSize)
                .clip(CircleShape)
                .background(if (checked) TvOnAccent else Color(0xFF7D9087))
        )
    }
}

@Composable
internal fun TvSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    containerColor: Color = TvSurface
) {
    val corner = (13.dp * scale).coerceAtLeast(10.dp)
    val itemCorner = (10.dp * scale).coerceAtLeast(8.dp)
    val itemMinH = (38.dp * scale).coerceAtLeast(32.dp)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(containerColor)
            .padding((4.dp * scale).coerceAtLeast(3.dp)),
        horizontalArrangement = Arrangement.spacedBy((4.dp * scale).coerceAtLeast(3.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = itemMinH)
                    .clip(RoundedCornerShape(itemCorner))
                    .background(if (selected) TvSel else Color.Transparent)
                    .gamepadClickable(
                        cornerRadius = itemCorner,
                        onStepHorizontal = { dir ->
                            val next = (selectedIndex + dir).coerceIn(0, options.lastIndex)
                            if (next != selectedIndex) onSelect(next)
                        },
                        onClick = { onSelect(idx) }
                    )
                    .padding(horizontal = (15.dp * scale).coerceAtLeast(10.dp), vertical = (5.dp * scale).coerceAtLeast(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) TvAccent else TvMuted,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (15f * scale).coerceAtLeast(12f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TvLeftNavigationRail(
    currentTab: DashboardTab,
    config: AppConfig,
    vpnState: VpnConnectionState,
    measuredPingMs: Int?,
    scale: Float,
    activeRailFocusRequester: FocusRequester?,
    onSelectTab: (DashboardTab) -> Unit,
    onVpnChipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val railWidth = (196.dp * scale).coerceIn(148.dp, 240.dp)
    val navItems: List<Triple<DashboardTab, ImageVector, String>> = listOf(
        Triple(DashboardTab.HOME, Icons.Default.Home, config.tr("Trang chủ", "Home")),
        Triple(DashboardTab.LIBRARY, Icons.Default.SportsEsports, config.tr("Thư viện", "Library")),
        Triple(DashboardTab.SERVERS, Icons.Default.Dns, config.tr("Máy chủ", "Servers")),
        Triple(DashboardTab.SETTINGS, Icons.Default.Tune, config.tr("Cài đặt", "Settings"))
    )

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .testTag("tv_left_navigation_rail"),
        verticalArrangement = Arrangement.spacedBy((5.dp * scale).coerceAtLeast(4.dp))
    ) {
        // Brand Header (.brand)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (14.dp * scale).coerceAtLeast(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(7.dp))
        ) {
            Box(
                modifier = Modifier
                    .size((34.dp * scale).coerceAtLeast(28.dp))
                    .clip(RoundedCornerShape((10.dp * scale).coerceAtLeast(8.dp)))
                    .background(TvAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = TvOnAccent,
                    fontSize = (17f * scale).coerceAtLeast(13f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "CloudPlay",
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (21f * scale).coerceAtLeast(16f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 4 Navigation Items (.nav)
        navItems.forEach { (tab, icon, label) ->
            val isCurrent = currentTab == tab
            val itemShape = RoundedCornerShape((13.dp * scale).coerceAtLeast(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = (48.dp * scale).coerceAtLeast(42.dp))
                    .clip(itemShape)
                    .background(if (isCurrent) TvSurface2 else Color.Transparent)
                    .gamepadClickable(
                        cornerRadius = (13.dp * scale).coerceAtLeast(10.dp),
                        focusRequester = if (isCurrent) activeRailFocusRequester else null,
                        onClick = { onSelectTab(tab) }
                    )
                    .padding(horizontal = (13.dp * scale).coerceAtLeast(10.dp), vertical = (6.dp * scale).coerceAtLeast(4.dp))
                    .testTag("tv_nav_tab_${tab.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((11.dp * scale).coerceAtLeast(8.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isCurrent) TvAccent else TvMuted,
                    modifier = Modifier.size((22.dp * scale).coerceAtLeast(18.dp))
                )
                Text(
                    text = label,
                    color = if (isCurrent) TvInk else TvMuted,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (17.5f * scale).coerceAtLeast(13.5f).sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom VPN Status Chip (.vpnchip)
        val isVpnActive = vpnState.mode != NetworkMode.DISCONNECTED
        val dotColor = when (vpnState.mode) {
            NetworkMode.DIRECT_NETWORK -> TvCyan
            NetworkMode.VPN_JAPAN -> TvAccent
            NetworkMode.DISCONNECTED -> TvAccent
        }
        val statusText = when (vpnState.mode) {
            NetworkMode.DIRECT_NETWORK -> config.tr("Mạng trực tiếp", "Direct Network")
            NetworkMode.VPN_JAPAN -> config.tr("VPN bật", "VPN On")
            NetworkMode.DISCONNECTED -> config.tr("Sẵn sàng", "Ready")
        }
        val pingSuffix = if (measuredPingMs != null && measuredPingMs > 0) " · $measuredPingMs ms" else ""
        val chipCorner = (13.dp * scale).coerceAtLeast(10.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(chipCorner))
                .background(TvSurface)
                .gamepadClickable(
                    cornerRadius = chipCorner,
                    onClick = onVpnChipClick
                )
                .padding(horizontal = (12.dp * scale).coerceAtLeast(9.dp), vertical = (10.dp * scale).coerceAtLeast(7.dp))
                .testTag("tv_rail_vpn_chip"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((9.dp * scale).coerceAtLeast(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .size((9.dp * scale).coerceAtLeast(7.dp))
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vpnState.currentServer.name,
                    color = TvInk,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (14.5f * scale).coerceAtLeast(12f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$statusText$pingSuffix",
                    color = if (isVpnActive) TvAccent else TvMuted,
                    fontSize = (13f * scale).coerceAtLeast(11f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TvBottomKeyHintBar(
    hints: List<TvKeyHint>,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height((34.dp * scale).coerceAtLeast(26.dp))
            .testTag("tv_bottom_key_hint_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((20.dp * scale).coerceAtLeast(12.dp))
    ) {
        hints.forEach { hint ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((4.dp * scale).coerceAtLeast(3.dp))
                ) {
                    hint.badges.forEach { (badgeText, badgeBg) ->
                        Box(
                            modifier = Modifier
                                .defaultMinSize(
                                    minWidth = (24.dp * scale).coerceAtLeast(20.dp),
                                    minHeight = (24.dp * scale).coerceAtLeast(20.dp)
                                )
                                .clip(CircleShape)
                                .background(badgeBg)
                                .padding(horizontal = (6.dp * scale).coerceAtLeast(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                color = TvInk,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = (12.5f * scale).coerceAtLeast(10f).sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    text = hint.label,
                    color = TvMuted,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = (14.5f * scale).coerceAtLeast(11.5f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
