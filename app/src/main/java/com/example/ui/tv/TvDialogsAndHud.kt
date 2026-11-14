package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cloudplay.ScreenRecorderState
import com.example.cloudplay.StreamTelemetry
import com.example.data.AppConfig
import com.example.data.AppLogger
import com.example.data.XboxWebUiMode
import com.example.ui.components.GamepadSurfaceController
import com.example.ui.components.GamepadUiNavigation
import com.example.ui.components.gamepadClickable
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState

/**
 * Hộp thoại xác nhận chuẩn TV (#s-dlg -> .dlg trong tv-mockup.html):
 * - Nền phẳng TvSurface2, viền 1dp TvLine, bo góc 22dp.
 * - Focus bị giữ trong hộp thoại, mặc định nằm ở nút an toàn (Hủy / Ở lại).
 * - Bấm A để chọn, B để đóng/hủy, kèm gợi ý phím ở dưới.
 */
@Composable
internal fun TvConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    cancelText: String,
    confirmVariant: TvButtonVariant = TvButtonVariant.DANGER,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    testTagPrefix: String = "tv_confirm_dialog"
) {
    val defaultCancelFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99040806))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val scale = computeTvScale(maxWidth, maxHeight, fontScale)
            val dlgWidth = (480.dp * scale).coerceIn(320.dp, 560.dp)
            val corner = (22.dp * scale).coerceAtLeast(16.dp)

            GamepadSurfaceController(
                enabled = true,
                defaultFocusRequester = defaultCancelFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = onDismiss
            ) {
                Column(
                    modifier = Modifier
                        .width(dlgWidth)
                        .clip(RoundedCornerShape(corner))
                        .background(TvSurface2)
                        .border(1.dp, TvLine, RoundedCornerShape(corner))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding((28.dp * scale).coerceAtLeast(18.dp))
                        .testTag(testTagPrefix),
                    verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
                ) {
                    Text(
                        text = title,
                        color = TvInk,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = (22f * scale).coerceAtLeast(17f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = description,
                        color = TvMuted,
                        fontSize = (16f * scale).coerceAtLeast(13f).sp,
                        lineHeight = (22f * scale).coerceAtLeast(18f).sp
                    )

                    Spacer(modifier = Modifier.height((8.dp * scale).coerceAtLeast(4.dp)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp), Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvButton(
                            text = cancelText,
                            variant = TvButtonVariant.DEFAULT,
                            scale = scale,
                            focusRequester = defaultCancelFocusRequester,
                            onClick = onDismiss,
                            modifier = Modifier.testTag("${testTagPrefix}_cancel_btn")
                        )
                        TvButton(
                            text = confirmText,
                            variant = confirmVariant,
                            scale = scale,
                            onClick = onConfirm,
                            modifier = Modifier.testTag("${testTagPrefix}_confirm_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height((4.dp * scale).coerceAtLeast(2.dp)))

                    TvBottomKeyHintBar(
                        hints = listOf(
                            TvKeyHint(listOf("A" to TvKeyABg), "Chọn"),
                            TvKeyHint(listOf("B" to TvKeyBBg), cancelText)
                        ),
                        scale = scale * 0.9f
                    )
                }
            }
        }
    }
}

/**
 * Menu nổi trong game bản TV (#s-game -> .hud trong tv-mockup.html):
 * - Mở/đóng bằng giữ đồng thời View + Menu ~0,6 giây (theo kiểu nghe lén, không chặn phím của game)
 *   hoặc bấm vào viên thuốc nhỏ ở góc trên trái.
 * - Khi mở: Bảng dọc phẳng bên trái (.hud, rộng ~390dp), gồm 3 ô chỉ số thực (Ping, FPS, Mạng)
 *   và danh sách các hàng điều khiển lớn chuẩn TV.
 */
@Composable
internal fun TvInGameFloatingHud(
    config: AppConfig,
    vpnState: VpnConnectionState,
    telemetry: StreamTelemetry,
    clarityBoostActive: Boolean,
    translationActive: Boolean,
    hideStreamStats: Boolean,
    recorderState: ScreenRecorderState,
    xboxWebUiMode: XboxWebUiMode,
    onToggleClarityBoost: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onToggleHideStreamStats: (Boolean) -> Unit,
    onToggleRecording: () -> Unit,
    onToggleNetwork: () -> Unit,
    onSelectXboxWebUiMode: (XboxWebUiMode) -> Unit,
    onReloadPage: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onNavigateHome: () -> Unit,
    onHudOpenStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHudExpanded by rememberSaveable { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    val firstRowFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isHudExpanded, showExitConfirmDialog) {
        onHudOpenStateChanged(isHudExpanded || showExitConfirmDialog)
    }

    val comboTick = GamepadUiNavigation.hudToggleComboTick
    var lastHandledComboTick by remember { mutableIntStateOf(comboTick) }
    LaunchedEffect(comboTick) {
        if (comboTick != lastHandledComboTick) {
            lastHandledComboTick = comboTick
            if (showExitConfirmDialog) {
                showExitConfirmDialog = false
            } else {
                isHudExpanded = !isHudExpanded
                AppLogger.info(
                    "UI",
                    "Menu nổi TV (View + Menu 0,6s): isHudExpanded=$isHudExpanded"
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val scale = computeTvScale(maxWidth, maxHeight, fontScale)

        val pingText = if (telemetry.rttMs > 0) "${telemetry.rttMs} ms" else "--"
        val fpsText = if (telemetry.fps > 0) "${telemetry.fps}" else "--"
        val netLabel = when (vpnState.mode) {
            NetworkMode.VPN_JAPAN -> "VPN"
            NetworkMode.DIRECT_NETWORK -> config.tr("Trực tiếp", "Direct")
            NetworkMode.DISCONNECTED -> config.tr("Trực tiếp", "Direct")
        }

        if (!isHudExpanded) {
            // Viên thuốc nhỏ gọn ở góc trên trái khi đang chơi trên TV
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = (24.dp * scale).coerceAtLeast(14.dp),
                        top = (18.dp * scale).coerceAtLeast(10.dp)
                    )
                    .clip(CircleShape)
                    .background(TvSurface.copy(alpha = 0.86f))
                    .border(1.dp, TvLine, CircleShape)
                    .clickable { isHudExpanded = true }
                    .padding(
                        horizontal = (14.dp * scale).coerceAtLeast(10.dp),
                        vertical = (7.dp * scale).coerceAtLeast(5.dp)
                    )
                    .testTag("tv_hud_minimized_pill"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size((8.dp * scale).coerceAtLeast(6.dp))
                        .clip(CircleShape)
                        .background(if (recorderState.isRecording) TvDanger else TvAccent)
                )
                Text(
                    text = if (telemetry.rttMs > 0) {
                        "$pingText · $fpsText FPS"
                    } else {
                        config.tr("Menu (View+Menu)", "Menu (View+Menu)")
                    },
                    color = TvInk,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = (13f * scale).coerceAtLeast(11f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Lớp nền mờ + Bảng Menu nhanh chuẩn TV (#s-game -> .hud trong tv-mockup.html)
            val hudWidth = (390.dp * scale).coerceIn(290.dp, 460.dp)
            val hudCorner = (20.dp * scale).coerceAtLeast(14.dp)
            val rowCorner = (14.dp * scale).coerceAtLeast(10.dp)

            GamepadSurfaceController(
                enabled = !showExitConfirmDialog,
                defaultFocusRequester = firstRowFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = { isHudExpanded = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x78050A07))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isHudExpanded = false }
                        )
                        .padding(
                            horizontal = (32.dp * scale).coerceIn(16.dp, 48.dp),
                            vertical = (22.dp * scale).coerceIn(12.dp, 36.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .width(hudWidth)
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(hudCorner))
                                .background(TvSurface)
                                .border(1.dp, TvLine, RoundedCornerShape(hudCorner))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                                .padding((20.dp * scale).coerceAtLeast(14.dp))
                                .testTag("tv_hud_expanded_panel"),
                            verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
                        ) {
                            // 1. Header (.hud-top)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = config.tr("Menu nhanh", "Quick Menu"),
                                    color = TvInk,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = (20f * scale).coerceAtLeast(16f).sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = config.tr("Giữ View + Menu 0,6 giây để đóng", "Hold View + Menu 0.6s to close"),
                                    color = TvMuted,
                                    fontSize = (13f * scale).coerceAtLeast(10.5f).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 2. 3 Metric Boxes (.hud-metrics -> .m)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
                            ) {
                                TvHudMetricBox(
                                    value = pingText,
                                    label = "Ping",
                                    valueColor = TvAccent,
                                    scale = scale,
                                    modifier = Modifier.weight(1f)
                                )
                                TvHudMetricBox(
                                    value = fpsText,
                                    label = "FPS",
                                    valueColor = TvInk,
                                    scale = scale,
                                    modifier = Modifier.weight(1f)
                                )
                                TvHudMetricBox(
                                    value = netLabel,
                                    label = config.tr("Mạng", "Network"),
                                    valueColor = TvCyan,
                                    scale = scale,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. Scrollable list of TV HUD action rows
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
                            ) {
                                // Hàng 1: Làm nét hình ảnh
                                TvHudToggleRow(
                                    title = config.tr("Làm nét hình ảnh", "Sharpen Image"),
                                    checked = clarityBoostActive,
                                    scale = scale,
                                    corner = rowCorner,
                                    focusRequester = firstRowFocusRequester,
                                    onToggle = { onToggleClarityBoost(!clarityBoostActive) },
                                    testTag = "tv_hud_clarity_row"
                                )

                                // Hàng 2: Tự dịch tiếng Việt
                                TvHudToggleRow(
                                    title = config.tr("Tự dịch tiếng Việt", "Auto-translate Xbox UI"),
                                    checked = translationActive,
                                    scale = scale,
                                    corner = rowCorner,
                                    onToggle = { onToggleTranslation(!translationActive) },
                                    testTag = "tv_hud_translate_row"
                                )

                                // Hàng 3: Hiện thông số luồng
                                TvHudToggleRow(
                                    title = config.tr("Hiện thông số luồng", "Show Stream Stats"),
                                    checked = !hideStreamStats,
                                    scale = scale,
                                    corner = rowCorner,
                                    onToggle = { onToggleHideStreamStats(!hideStreamStats) },
                                    testTag = "tv_hud_stats_row"
                                )

                                // Hàng 4: Quay màn hình (.MP4)
                                val recordSubtitle = if (recorderState.isRecording) {
                                    val mins = recorderState.elapsedSeconds / 60
                                    val secs = recorderState.elapsedSeconds % 60
                                    "● %02d:%02d".format(mins, secs)
                                } else {
                                    config.tr("Tắt", "Off")
                                }
                                TvHudActionRow(
                                    title = config.tr("Quay màn hình (.MP4)", "Screen Recording (.MP4)"),
                                    trailingText = recordSubtitle,
                                    trailingColor = if (recorderState.isRecording) TvDanger else TvMuted,
                                    scale = scale,
                                    corner = rowCorner,
                                    onClick = onToggleRecording,
                                    testTag = "tv_hud_record_row"
                                )

                                // Hàng 5: Chuyển mạng VPN / Trực tiếp
                                TvHudActionRow(
                                    title = if (vpnState.mode == NetworkMode.VPN_JAPAN) {
                                        config.tr("Tắt VPN (Chuyển sang Trực tiếp)", "Disconnect VPN (Use Direct)")
                                    } else {
                                        config.tr("Bật VPN mở khóa vùng", "Connect Region Unlock VPN")
                                    },
                                    trailingText = netLabel,
                                    trailingColor = TvCyan,
                                    scale = scale,
                                    corner = rowCorner,
                                    onClick = onToggleNetwork,
                                    testTag = "tv_hud_network_row"
                                )

                                // Hàng 6: Giao diện web Xbox (Cũ / Mới)
                                TvHudActionRow(
                                    title = config.tr("Giao diện web Xbox", "Xbox Web UI"),
                                    trailingText = if (xboxWebUiMode == XboxWebUiMode.CLASSIC) {
                                        config.tr("Cũ", "Classic")
                                    } else {
                                        config.tr("Mới", "New")
                                    },
                                    trailingColor = TvAccent,
                                    scale = scale,
                                    corner = rowCorner,
                                    onClick = {
                                        val nextMode = if (xboxWebUiMode == XboxWebUiMode.CLASSIC) {
                                            XboxWebUiMode.NEW_UI
                                        } else {
                                            XboxWebUiMode.CLASSIC
                                        }
                                        onSelectXboxWebUiMode(nextMode)
                                    },
                                    testTag = "tv_hud_webui_row"
                                )

                                // Hàng 7: Cài đặt nhanh đầy đủ
                                TvHudActionRow(
                                    title = config.tr("Tùy chỉnh nhanh Hiển thị & Tay cầm", "Display & Controller Quick Settings"),
                                    trailingText = "›",
                                    trailingColor = TvMuted,
                                    scale = scale,
                                    corner = rowCorner,
                                    onClick = {
                                        isHudExpanded = false
                                        onOpenQuickSettings()
                                    },
                                    testTag = "tv_hud_quick_settings_row"
                                )

                                // Hàng 8: Tải lại trang
                                TvHudActionRow(
                                    title = config.tr("Tải lại trang", "Reload Page"),
                                    trailingText = "↻",
                                    trailingColor = TvMuted,
                                    scale = scale,
                                    corner = rowCorner,
                                    onClick = {
                                        isHudExpanded = false
                                        onReloadPage()
                                    },
                                    testTag = "tv_hud_reload_row"
                                )

                                // Hàng 9: Thoát về Trang chủ
                                TvHudActionRow(
                                    title = config.tr("Thoát về Trang chủ", "Exit to Home"),
                                    titleColor = TvDanger,
                                    trailingText = "›",
                                    trailingColor = TvDanger,
                                    scale = scale,
                                    corner = rowCorner,
                                    onClick = { showExitConfirmDialog = true },
                                    testTag = "tv_hud_exit_home_row"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height((8.dp * scale).coerceAtLeast(4.dp)))

                        TvBottomKeyHintBar(
                            hints = listOf(
                                TvKeyHint(listOf("A" to TvKeyABg), config.tr("Chọn / Bật tắt", "Select / Toggle")),
                                TvKeyHint(listOf("B" to TvKeyBBg), config.tr("Đóng menu", "Close Menu")),
                                TvKeyHint(listOf("View" to TvSurface2, "Menu" to TvSurface2), config.tr("Đóng", "Close"))
                            ),
                            scale = scale
                        )
                    }
                }
            }
        }
    }

    if (showExitConfirmDialog) {
        TvConfirmDialog(
            title = config.tr("Thoát phiên chơi?", "Exit Game Session?"),
            description = config.tr(
                "Phiên truyền phát hiện tại sẽ đóng và quay về Trang chủ. Tiến trình đã lưu trên đám mây Xbox được giữ nguyên.",
                "The current cloud gaming stream will close and return to Home. Your Xbox Cloud saved progress is preserved."
            ),
            confirmText = config.tr("Thoát game", "Exit Game"),
            cancelText = config.tr("Ở lại", "Stay"),
            confirmVariant = TvButtonVariant.DANGER,
            onConfirm = {
                showExitConfirmDialog = false
                isHudExpanded = false
                onNavigateHome()
            },
            onDismiss = {
                showExitConfirmDialog = false
            },
            testTagPrefix = "tv_exit_game_dialog"
        )
    }
}

@Composable
private fun TvHudMetricBox(
    value: String,
    label: String,
    valueColor: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val corner = (12.dp * scale).coerceAtLeast(9.dp)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(TvSurface2)
            .padding((10.dp * scale).coerceAtLeast(7.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = (19f * scale).coerceAtLeast(14.5f).sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = label,
            color = TvMuted,
            fontSize = (12f * scale).coerceAtLeast(10f).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TvHudToggleRow(
    title: String,
    checked: Boolean,
    scale: Float,
    corner: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester? = null,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (54.dp * scale).coerceAtLeast(46.dp))
            .clip(RoundedCornerShape(corner))
            .background(TvSurface2)
            .gamepadClickable(
                cornerRadius = corner,
                focusRequester = focusRequester,
                onStepHorizontal = { dir ->
                    val wantOn = dir > 0
                    if (wantOn != checked) onToggle()
                },
                onClick = onToggle
            )
            .padding(
                horizontal = (16.dp * scale).coerceAtLeast(12.dp),
                vertical = (10.dp * scale).coerceAtLeast(8.dp)
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TvInk,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (16.5f * scale).coerceAtLeast(13.5f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))
        TvSwitch(checked = checked, scale = scale * 0.92f)
    }
}

@Composable
private fun TvHudActionRow(
    title: String,
    trailingText: String,
    trailingColor: Color,
    scale: Float,
    corner: androidx.compose.ui.unit.Dp,
    titleColor: Color = TvInk,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (54.dp * scale).coerceAtLeast(46.dp))
            .clip(RoundedCornerShape(corner))
            .background(TvSurface2)
            .gamepadClickable(
                cornerRadius = corner,
                onClick = onClick
            )
            .padding(
                horizontal = (16.dp * scale).coerceAtLeast(12.dp),
                vertical = (10.dp * scale).coerceAtLeast(8.dp)
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = titleColor,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (16.5f * scale).coerceAtLeast(13.5f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))
        Text(
            text = trailingText,
            color = trailingColor,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = (16f * scale).coerceAtLeast(13f).sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
