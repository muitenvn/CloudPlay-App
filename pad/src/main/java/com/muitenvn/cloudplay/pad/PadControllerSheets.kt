package com.muitenvn.cloudplay.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muitenvn.cloudplay.protocol.RemotePadButtons
import kotlin.math.roundToInt

private val SheetBg = Color(0xFF0D1511)
private val SheetCard = Color(0xFF142019)
private val SheetSelected = Color(0xFF16352A)
private val SheetBorder = Color(0xFF284235)
private val SheetAccent = Color(0xFF27D884)
private val SheetCyan = Color(0xFF3FD8EE)
private val SheetAmber = Color(0xFFE3B341)
private val SheetDanger = Color(0xFFF0706B)
private val SheetText = Color(0xFFEEF6F1)
private val SheetMuted = Color(0xFF9EB4A7)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PadGlobalSettingsSheet(
    lang: PadLanguage,
    tvName: String,
    tvHost: String,
    activeGameTitle: String,
    pingMs: Int?,
    currentPreset: PadLayoutPreset,
    settings: PadGlobalControllerSettings,
    hiddenNodeCount: Int,
    customButtonCount: Int,
    onSelectPreset: (PadLayoutPreset) -> Unit,
    onUpdateSettings: (PadGlobalControllerSettings) -> Unit,
    onOpenEditLayoutMode: () -> Unit,
    onOpenManageButtonsModal: () -> Unit,
    onShowAllHiddenNodes: () -> Unit,
    onResetCurrentLayout: () -> Unit,
    onTestVibration: () -> Unit,
    onDisconnect: () -> Unit,
    onForgetDevice: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB8050807))
            .clickable(onClick = onClose)
            .testTag("pad_global_settings_sheet_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(18.dp))
                .background(SheetBg)
                .border(1.5.dp, SheetBorder, RoundedCornerShape(18.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("pad_global_settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PadStrings.tr(lang, "⚙ Cài đặt tay cầm Ravno Pad", "⚙ Ravno Pad Controller Settings"),
                        color = SheetText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SheetSelected)
                            .border(1.dp, SheetAccent.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        val pingLabel = pingMs?.let { "$it ms" } ?: "-- ms"
                        Text(
                            text = "$tvName · $pingLabel",
                            color = SheetAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                SheetPillButton(
                    label = PadStrings.tr(lang, "✓ Đóng", "✓ Close"),
                    isPrimary = true,
                    onClick = onClose,
                    testTag = "pad_settings_close_btn"
                )
            }

            // Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. BỐ CỤC & PHẠM VI LƯU
                SheetSectionCard(
                    title = PadStrings.tr(lang, "1. MẪU BỐ CỤC & HIỂN THỊ CHUNG", "1. LAYOUT PRESET & GLOBAL DISPLAY")
                ) {
                    Text(
                        text = PadStrings.tr(lang, "Mẫu bố cục theo thể loại game", "Game genre layout preset"),
                        color = SheetMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PadLayoutPreset.entries.forEach { preset ->
                            SheetChoiceChip(
                                label = preset.shortTitle(lang),
                                selected = currentPreset == preset,
                                onClick = { onSelectPreset(preset) },
                                testTag = "pad_preset_chip_${preset.id}"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PadStrings.tr(lang, "Phạm vi lưu bố cục", "Layout storage scope"),
                                color = SheetText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val gameDisplay = activeGameTitle.trim().takeIf {
                                it.isNotEmpty() && !it.equals("generic", ignoreCase = true)
                            } ?: PadStrings.tr(lang, "Mặc định", "Default")
                            Text(
                                text = PadStrings.tr(
                                    lang,
                                    "Game hiện tại trên TV: $gameDisplay",
                                    "Active TV game: $gameDisplay"
                                ),
                                color = SheetMuted,
                                fontSize = 11.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PadLayoutScope.entries.forEach { scope ->
                                SheetChoiceChip(
                                    label = scope.localizedTitle(lang),
                                    selected = settings.layoutScope == scope,
                                    onClick = { onUpdateSettings(settings.copy(layoutScope = scope)) },
                                    testTag = "pad_scope_chip_${scope.id}"
                                )
                            }
                        }
                    }

                    // Sliders: Kích thước chung & Độ hiển thị chung
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Kích thước chung", "Global scale"),
                                    color = SheetText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.globalScale * 100).roundToInt()}%",
                                    color = SheetAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.globalScale,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(globalScale = it.coerceIn(0.60f, 1.65f)))
                                },
                                valueRange = 0.60f..1.65f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetAccent,
                                    activeTrackColor = SheetAccent,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Độ hiển thị chung", "Global opacity"),
                                    color = SheetText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.globalOpacity * 100).roundToInt()}%",
                                    color = SheetCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.globalOpacity,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(globalOpacity = it.coerceIn(0.05f, 1.0f)))
                                },
                                valueRange = 0.05f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetCyan,
                                    activeTrackColor = SheetCyan,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                        }
                    }

                    // Hành động nhanh chỉnh sửa bố cục & thêm/ẩn nút
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetPillButton(
                            label = PadStrings.tr(
                                lang,
                                "✥ Sửa vị trí & tùy chỉnh từng nút",
                                "✥ Edit Layout & Per-Button Options"
                            ),
                            isPrimary = true,
                            onClick = onOpenEditLayoutMode,
                            testTag = "pad_open_edit_mode_btn"
                        )
                        SheetPillButton(
                            label = PadStrings.tr(
                                lang,
                                "＋ Thêm nút trống / Ẩn nút ($customButtonCount)",
                                "＋ Custom / Hide Buttons ($customButtonCount)"
                            ),
                            onClick = onOpenManageButtonsModal,
                            testTag = "pad_open_manage_buttons_btn"
                        )
                        if (hiddenNodeCount > 0) {
                            SheetPillButton(
                                label = PadStrings.tr(
                                    lang,
                                    "👁 Hiện tất cả nút ẩn ($hiddenNodeCount)",
                                    "👁 Show All Hidden ($hiddenNodeCount)"
                                ),
                                onClick = onShowAllHiddenNodes,
                                testTag = "pad_show_all_hidden_btn"
                            )
                        }
                        SheetPillButton(
                            label = PadStrings.tr(lang, "↺ Đặt lại mặc định", "↺ Reset Default"),
                            isDanger = true,
                            onClick = onResetCurrentLayout,
                            testTag = "pad_sheet_reset_layout_btn"
                        )
                    }
                }

                // 2. PHẢN HỒI RUNG (KHI BẤM NÚT & RUNG TỪ TRÒ CHƠI)
                SheetSectionCard(
                    title = PadStrings.tr(lang, "2. PHẢN HỒI RUNG (HAPTIC & GAME RUMBLE)", "2. VIBRATION & HAPTICS")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Rung khi chạm nút", "Button touch haptic"),
                            color = SheetText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PadHapticLevel.entries.forEach { lvl ->
                                SheetChoiceChip(
                                    label = lvl.localizedTitle(lang),
                                    selected = settings.buttonHapticLevel == lvl,
                                    onClick = { onUpdateSettings(settings.copy(buttonHapticLevel = lvl)) },
                                    testTag = "pad_btn_haptic_${lvl.id}"
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PadStrings.tr(lang, "Rung từ trò chơi trên TV", "In-game rumble from TV"),
                                color = SheetText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = PadStrings.tr(
                                    lang,
                                    "Nhận mô-tơ rung trái/phải trực tiếp từ Xbox Cloud Gaming",
                                    "Receives dual-motor rumble packets from Xbox Cloud Gaming"
                                ),
                                color = SheetMuted,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PadHapticLevel.entries.forEach { lvl ->
                                SheetChoiceChip(
                                    label = lvl.localizedTitle(lang),
                                    selected = settings.gameRumbleLevel == lvl,
                                    onClick = { onUpdateSettings(settings.copy(gameRumbleLevel = lvl)) },
                                    testTag = "pad_game_rumble_${lvl.id}"
                                )
                            }
                            SheetPillButton(
                                label = PadStrings.tr(lang, "Thử rung", "Test"),
                                onClick = onTestVibration,
                                testTag = "pad_test_vibration_btn"
                            )
                        }
                    }
                }

                // 3. CẦN ANALOG TRÁI / PHẢI, VÙNG CHẾT & TÁCH CỤM NÚT
                SheetSectionCard(
                    title = PadStrings.tr(lang, "3. CẦN ANALOG, VÙNG CHẾT & TÁCH CỤM NÚT", "3. ANALOG STICKS, DEADZONES & CLUSTERS")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cần Trái (L)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Độ nhạy cần Trái (L)", "Left Stick Sensitivity"),
                                    color = SheetText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.leftStickSens * 100).roundToInt()}%",
                                    color = SheetAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.leftStickSens,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(leftStickSens = it.coerceIn(0.60f, 1.60f)))
                                },
                                valueRange = 0.60f..1.60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetAccent,
                                    activeTrackColor = SheetAccent,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PadStickCurve.entries.forEach { curve ->
                                    SheetChoiceChip(
                                        label = curve.localizedTitle(lang),
                                        selected = settings.leftStickCurve == curve,
                                        onClick = { onUpdateSettings(settings.copy(leftStickCurve = curve)) },
                                        testTag = "pad_left_curve_${curve.id}"
                                    )
                                }
                            }
                        }

                        // Cần Phải (R)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Độ nhạy cần Phải (R)", "Right Stick Sensitivity"),
                                    color = SheetText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(settings.rightStickSens * 100).roundToInt()}%",
                                    color = SheetCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = settings.rightStickSens,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(rightStickSens = it.coerceIn(0.60f, 1.60f)))
                                },
                                valueRange = 0.60f..1.60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetCyan,
                                    activeTrackColor = SheetCyan,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PadStickCurve.entries.forEach { curve ->
                                    SheetChoiceChip(
                                        label = curve.localizedTitle(lang),
                                        selected = settings.rightStickCurve == curve,
                                        onClick = { onUpdateSettings(settings.copy(rightStickCurve = curve)) },
                                        testTag = "pad_right_curve_${curve.id}"
                                    )
                                }
                            }
                        }
                    }

                    // Vùng chết tâm & rìa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Vùng chết tâm (Inner Deadzone)", "Inner Deadzone"),
                                    color = SheetText,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${(settings.innerDeadzone * 100).roundToInt()}%",
                                    color = SheetMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = settings.innerDeadzone,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(innerDeadzone = it.coerceIn(0.02f, 0.25f)))
                                },
                                valueRange = 0.02f..0.25f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetAccent,
                                    activeTrackColor = SheetAccent,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = PadStrings.tr(lang, "Ngưỡng kịch rìa (Outer Deadzone)", "Outer Threshold"),
                                    color = SheetText,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${(settings.outerDeadzone * 100).roundToInt()}%",
                                    color = SheetMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                            Slider(
                                value = settings.outerDeadzone,
                                onValueChange = {
                                    onUpdateSettings(settings.copy(outerDeadzone = it.coerceIn(0.75f, 1.0f)))
                                },
                                valueRange = 0.75f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SheetCyan,
                                    activeTrackColor = SheetCyan,
                                    inactiveTrackColor = SheetBorder
                                )
                            )
                        }
                    }

                    // Hàng công tắc: Tự chạy nhanh L3, Tách D-Pad, Tách ABXY
                    val effectiveAutoSprint = when (settings.autoSprintL3Override) {
                        0 -> false
                        1 -> true
                        else -> currentPreset.autoSprintL3
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetChoiceChip(
                            label = if (effectiveAutoSprint) {
                                PadStrings.tr(lang, "🏃 Tự chạy nhanh L3: Bật", "🏃 Auto-Sprint L3: On")
                            } else {
                                PadStrings.tr(lang, "🏃 Tự chạy nhanh L3: Tắt", "🏃 Auto-Sprint L3: Off")
                            },
                            selected = effectiveAutoSprint,
                            onClick = {
                                val nextVal = if (effectiveAutoSprint) 0 else 1
                                onUpdateSettings(settings.copy(autoSprintL3Override = nextVal))
                            },
                            testTag = "pad_toggle_autosprint_btn"
                        )

                        SheetChoiceChip(
                            label = if (settings.dpadSplitMode) {
                                PadStrings.tr(lang, "✚ D-Pad: Đang tách 4 nút", "✚ D-Pad: Split 4 Keys")
                            } else {
                                PadStrings.tr(lang, "✚ D-Pad: Chữ thập 8 hướng", "✚ D-Pad: 8-Way Cross")
                            },
                            selected = settings.dpadSplitMode,
                            onClick = {
                                onUpdateSettings(settings.copy(dpadSplitMode = !settings.dpadSplitMode))
                            },
                            testTag = "pad_sheet_toggle_split_dpad"
                        )

                        SheetChoiceChip(
                            label = if (settings.abxySplitMode) {
                                PadStrings.tr(lang, "◆ ABXY: Đang tách rời 4 nút", "◆ ABXY: Split 4 Buttons")
                            } else {
                                PadStrings.tr(lang, "◆ ABXY: Cụm trượt tổ hợp", "◆ ABXY: Slide Cluster")
                            },
                            selected = settings.abxySplitMode,
                            onClick = {
                                onUpdateSettings(settings.copy(abxySplitMode = !settings.abxySplitMode))
                            },
                            testTag = "pad_sheet_toggle_split_abxy"
                        )
                    }
                }

                // 4. KẾT NỐI TV & THIẾT BỊ ĐÃ GHÉP ĐÔI
                SheetSectionCard(
                    title = PadStrings.tr(lang, "4. KẾT NỐI TV HIỆN TẠI", "4. ACTIVE TV CONNECTION")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tvName,
                                color = SheetText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "IP: $tvHost · Ping: ${pingMs?.let { "$it ms" } ?: "-- ms"}",
                                color = SheetMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SheetPillButton(
                                label = PadStrings.tr(lang, "Quên TV này", "Forget TV"),
                                isDanger = true,
                                onClick = onForgetDevice,
                                testTag = "pad_sheet_forget_tv_btn"
                            )
                            SheetPillButton(
                                label = PadStrings.tr(lang, "Ngắt kết nối", "Disconnect"),
                                isDanger = true,
                                onClick = onDisconnect,
                                testTag = "pad_sheet_disconnect_btn"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PadManageButtonsModal(
    lang: PadLanguage,
    dpadSplitMode: Boolean,
    abxySplitMode: Boolean,
    nodeTunings: Map<String, PadNodeTuning>,
    customButtons: List<PadCustomButtonSpec>,
    onToggleNodeHidden: (String) -> Unit,
    onShowAllNodes: () -> Unit,
    onAddCustomButton: (label: String, shape: PadCustomBtnShape, mask: Int) -> Unit,
    onDeleteCustomButton: (String) -> Unit,
    onClose: () -> Unit
) {
    var newLabel by remember { mutableStateOf("M${customButtons.size + 1}") }
    var newShape by remember { mutableStateOf(PadCustomBtnShape.PILL) }
    var newMask by remember { mutableIntStateOf(RemotePadButtons.A or RemotePadButtons.B) }

    val standardNodes = remember(dpadSplitMode, abxySplitMode) {
        buildList {
            add("btn_lt" to "Cò LT")
            add("btn_lb" to "Vai LB")
            add("btn_l3" to "Ấn cần L3")
            add("btn_r3" to "Ấn cần R3")
            add("btn_rb" to "Vai RB")
            add("btn_rt" to "Cò RT")
            add("btn_view" to "⧉ View")
            add("btn_xbox" to "Xbox Guide")
            add("btn_menu" to "☰ Menu")
            add("stick_left" to "Cần Trái (L)")
            add("stick_right" to "Cần Phải (R)")
            if (dpadSplitMode) {
                add("dpad_up" to "D-Pad ▲")
                add("dpad_down" to "D-Pad ▼")
                add("dpad_left" to "D-Pad ◀")
                add("dpad_right" to "D-Pad ▶")
            } else {
                add("dpad" to "Cụm D-Pad")
            }
            if (abxySplitMode) {
                add("btn_a" to "Nút A")
                add("btn_b" to "Nút B")
                add("btn_x" to "Nút X")
                add("btn_y" to "Nút Y")
            } else {
                add("abxy" to "Cụm ABXY")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC4050807))
            .clickable(onClick = onClose)
            .testTag("pad_manage_buttons_modal_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 740.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(18.dp))
                .background(SheetBg)
                .border(1.5.dp, SheetBorder, RoundedCornerShape(18.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .testTag("pad_manage_buttons_modal"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PadStrings.tr(
                        lang,
                        "＋ Tạo nút trống tùy chỉnh & Ẩn / Hiện nút",
                        "＋ Create Custom Blank Buttons & Hide / Show"
                    ),
                    color = SheetText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                SheetPillButton(
                    label = PadStrings.tr(lang, "✓ Đóng", "✓ Close"),
                    isPrimary = true,
                    onClick = onClose,
                    testTag = "pad_manage_buttons_close_btn"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tạo nút trống mới
                SheetSectionCard(
                    title = PadStrings.tr(lang, "TẠO NÚT TRỐNG TÙY CHỈNH (GÁN 1 HOẶC NHIỀU NÚT COMBO)", "ADD CUSTOM BLANK BUTTON")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newLabel,
                            onValueChange = { newLabel = it.take(8) },
                            label = {
                                Text(
                                    text = PadStrings.tr(lang, "Nhãn nút (tối đa 8 ký tự)", "Label"),
                                    fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SheetText,
                                unfocusedTextColor = SheetText,
                                focusedBorderColor = SheetAccent,
                                unfocusedBorderColor = SheetBorder,
                                focusedLabelColor = SheetAccent,
                                unfocusedLabelColor = SheetMuted
                            ),
                            modifier = Modifier.width(170.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = PadStrings.tr(lang, "Hình dạng nút", "Button shape"),
                                color = SheetMuted,
                                fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PadCustomBtnShape.entries.forEach { sh ->
                                    SheetChoiceChip(
                                        label = sh.localizedTitle(lang),
                                        selected = newShape == sh,
                                        onClick = { newShape = sh },
                                        testTag = "pad_custom_shape_${sh.id}"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        SheetPillButton(
                            label = PadStrings.tr(lang, "＋ Thêm vào màn hình", "＋ Add Button"),
                            isPrimary = true,
                            onClick = {
                                val mask = if (newMask == 0) RemotePadButtons.A else newMask
                                val cleanLabel = newLabel.trim().ifBlank {
                                    formatMaskBadge(mask).take(6).ifBlank { "M${customButtons.size + 1}" }
                                }
                                onAddCustomButton(cleanLabel, newShape, mask)
                                newLabel = "M${customButtons.size + 2}"
                            },
                            testTag = "pad_create_custom_btn_confirm"
                        )
                    }

                    Text(
                        text = PadStrings.tr(
                            lang,
                            "Chọn các nút Xbox sẽ kích hoạt cùng lúc khi bấm nút trống này:",
                            "Select Xbox buttons triggered simultaneously by this custom button:"
                        ),
                        color = SheetMuted,
                        fontSize = 11.sp
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AllPadAssignableButtons.forEach { opt ->
                            val active = (newMask and opt.bit) != 0
                            SheetChoiceChip(
                                label = opt.shortLabel,
                                selected = active,
                                onClick = {
                                    val toggled = if (active) (newMask and opt.bit.inv()) else (newMask or opt.bit)
                                    if (toggled != 0) newMask = toggled
                                },
                                testTag = "pad_custom_assign_${opt.shortLabel}"
                            )
                        }
                    }
                }

                // Danh sách nút trống đã tạo
                if (customButtons.isNotEmpty()) {
                    SheetSectionCard(
                        title = PadStrings.tr(
                            lang,
                            "NÚT TRỐNG ĐÃ TẠO (${customButtons.size})",
                            "CREATED CUSTOM BUTTONS (${customButtons.size})"
                        )
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customButtons.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SheetBg)
                                        .border(1.dp, SheetBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.label} (${formatMaskBadge(item.assignedMask)})",
                                        color = SheetText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "✕ Xóa",
                                        color = SheetDanger,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onDeleteCustomButton(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Ẩn / Hiện nút tiêu chuẩn
                SheetSectionCard(
                    title = PadStrings.tr(
                        lang,
                        "ẨN / HIỆN NÚT TIÊU CHUẨN TRÊN MÀN HÌNH",
                        "SHOW / HIDE STANDARD BUTTONS"
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(
                                lang,
                                "Bấm vào nút để Ẩn (ẩn hẳn, không nhận chạm) hoặc Hiện lại:",
                                "Tap any button below to toggle visibility:"
                            ),
                            color = SheetMuted,
                            fontSize = 11.sp
                        )
                        SheetPillButton(
                            label = PadStrings.tr(lang, "Hiện tất cả", "Show All"),
                            onClick = onShowAllNodes,
                            testTag = "pad_modal_show_all_btn"
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        standardNodes.forEach { (key, title) ->
                            val isHidden = nodeTunings[key]?.hidden == true
                            SheetChoiceChip(
                                label = if (isHidden) "👁‍🗨 $title (Đang ẩn)" else "✓ $title",
                                selected = !isHidden,
                                onClick = { onToggleNodeHidden(key) },
                                testTag = "pad_toggle_vis_$key"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PadEditNodeToolbar(
    lang: PadLanguage,
    selectedNodeKey: String,
    curTuning: PadNodeTuning,
    customButtons: List<PadCustomButtonSpec>,
    showEditGrid: Boolean,
    isPositionLocked: Boolean,
    dpadSplitMode: Boolean,
    abxySplitMode: Boolean,
    onUpdateTuning: (PadNodeTuning) -> Unit,
    onResetSingleNode: () -> Unit,
    onResetAllNodes: () -> Unit,
    onDeleteCustomNode: (String) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleSplitDpad: () -> Unit,
    onToggleSplitAbxy: () -> Unit,
    onOpenGlobalSettings: () -> Unit,
    onOpenManageButtons: () -> Unit,
    onDoneEditMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Vị trí & Cỡ, 1: Tác vụ & Nâng cao
    val nodeTitle = friendlyNodeTitle(selectedNodeKey, lang, customButtons)
    val isCustomNode = selectedNodeKey.startsWith("custom_")
    val isTriggerNode = selectedNodeKey == "btn_lt" || selectedNodeKey == "btn_rt"
    val isShoulderNode = selectedNodeKey == "btn_lb" || selectedNodeKey == "btn_rb"
    val isStickNode = selectedNodeKey == "stick_left" || selectedNodeKey == "stick_right"

    Column(
        modifier = modifier
            .fillMaxWidth(0.72f)
            .widthIn(max = 680.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SheetBg.copy(alpha = 0.96f))
            .border(1.5.dp, SheetAccent, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("pad_customize_toolbar"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dòng 1: Tên nút đang chọn + Tọa độ + Công cụ Lưới / Khóa / Cài đặt / Xong
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$nodeTitle · Cỡ ${(curTuning.scaleMult * 100).roundToInt()}% · Mờ ${(curTuning.opacity * 100).roundToInt()}%",
                    color = SheetText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tọa độ ΔX ${(curTuning.dxNorm * 100).roundToInt()}% · ΔY ${(curTuning.dyNorm * 100).roundToInt()}% · Ngang×Dọc ${(curTuning.widthMult * 100).roundToInt()}×${(curTuning.heightMult * 100).roundToInt()}%",
                    color = SheetMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SheetChoiceChip(
                    label = PadStrings.tr(lang, "# Lưới", "# Grid"),
                    selected = showEditGrid,
                    onClick = onToggleGrid,
                    testTag = "virtual_gamepad_grid_toggle"
                )
                SheetChoiceChip(
                    label = if (isPositionLocked) {
                        PadStrings.tr(lang, "🔒 Khóa kéo", "🔒 Locked")
                    } else {
                        PadStrings.tr(lang, "🔓 Kéo tự do", "🔓 Free Drag")
                    },
                    selected = isPositionLocked,
                    onClick = onToggleLock,
                    testTag = "pad_lock_position_btn"
                )
                SheetPillButton(
                    label = PadStrings.tr(lang, "＋ Nút", "＋ Btn"),
                    onClick = onOpenManageButtons,
                    testTag = "pad_toolbar_add_btn"
                )
                SheetPillButton(
                    label = PadStrings.tr(lang, "⚙ Chung", "⚙ Global"),
                    onClick = onOpenGlobalSettings,
                    testTag = "pad_toolbar_global_btn"
                )
                SheetPillButton(
                    label = PadStrings.tr(lang, "✓ Xong", "✓ Done"),
                    isPrimary = true,
                    onClick = onDoneEditMode,
                    testTag = "pad_customize_done_btn"
                )
            }
        }

        // Chuyển Tab trong hộp Sửa nút: [1. Cỡ, Độ mờ & Vị trí] | [2. Chế độ bấm, Vuốt 4 hướng & Combo]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SheetChoiceChip(
                    label = PadStrings.tr(lang, "1. Kích thước, Độ mờ & Dịch 2px", "1. Size, Opacity & Nudge"),
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    testTag = "pad_edit_tab_geometry"
                )
                SheetChoiceChip(
                    label = PadStrings.tr(lang, "2. Turbo, Vuốt 4 hướng & Combo", "2. Turbo, 4-Way Swipe & Combo"),
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    testTag = "pad_edit_tab_behavior"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCustomNode) {
                    SheetPillButton(
                        label = PadStrings.tr(lang, "🗑 Xóa nút này", "🗑 Delete"),
                        isDanger = true,
                        onClick = { onDeleteCustomNode(selectedNodeKey) },
                        testTag = "pad_delete_custom_node_btn"
                    )
                } else {
                    SheetChoiceChip(
                        label = if (curTuning.hidden) {
                            PadStrings.tr(lang, "👁 Đang ẩn (Bấm để hiện)", "👁 Hidden")
                        } else {
                            PadStrings.tr(lang, "👁 Ẩn nút này", "👁 Hide")
                        },
                        selected = curTuning.hidden,
                        onClick = { onUpdateTuning(curTuning.copy(hidden = !curTuning.hidden)) },
                        testTag = "pad_hide_single_node_btn"
                    )
                }
                SheetPillButton(
                    label = PadStrings.tr(lang, "↺ Nút này", "↺ Node"),
                    onClick = onResetSingleNode,
                    testTag = "pad_reset_single_node_btn"
                )
                SheetPillButton(
                    label = PadStrings.tr(lang, "Đặt lại tất cả", "Reset All"),
                    isDanger = true,
                    onClick = onResetAllNodes,
                    testTag = "pad_customize_reset_btn"
                )
            }
        }

        if (activeTab == 0) {
            // TAB 0: Thanh trượt Kích thước, Độ mờ (5%..100%), Bề ngang × Bề dọc & Dịch 2px
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Cỡ nút (55%–175%)", "Size (55%–175%)"),
                            color = SheetText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${(curTuning.scaleMult * 100).roundToInt()}%",
                            color = SheetAccent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = curTuning.scaleMult,
                        onValueChange = {
                            onUpdateTuning(curTuning.copy(scaleMult = it.coerceIn(0.55f, 1.75f)))
                        },
                        valueRange = 0.55f..1.75f,
                        colors = SliderDefaults.colors(
                            thumbColor = SheetAccent,
                            activeTrackColor = SheetAccent,
                            inactiveTrackColor = SheetBorder
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Độ hiển thị (5%–100%)", "Opacity (5%–100%)"),
                            color = SheetText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${(curTuning.opacity * 100).roundToInt()}%",
                            color = SheetCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = curTuning.opacity,
                        onValueChange = {
                            onUpdateTuning(curTuning.copy(opacity = it.coerceIn(0.05f, 1.0f)))
                        },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = SheetCyan,
                            activeTrackColor = SheetCyan,
                            inactiveTrackColor = SheetBorder
                        )
                    )
                }
            }

            // Hàng chỉnh Bề ngang × Bề dọc + Dịch từng nấc 2px + Tách D-Pad/ABXY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút +/- nhanh (giữ cả testTag cũ để tương thích kiểm thử)
                SheetPillButton(
                    label = "Cỡ −",
                    onClick = {
                        onUpdateTuning(curTuning.copy(scaleMult = (curTuning.scaleMult - 0.08f).coerceIn(0.55f, 1.75f)))
                    },
                    testTag = "pad_customize_scale_down_btn"
                )
                SheetPillButton(
                    label = "Cỡ +",
                    onClick = {
                        onUpdateTuning(curTuning.copy(scaleMult = (curTuning.scaleMult + 0.08f).coerceIn(0.55f, 1.75f)))
                    },
                    testTag = "pad_customize_scale_up_btn"
                )
                SheetPillButton(
                    label = "Mờ −",
                    onClick = {
                        onUpdateTuning(curTuning.copy(opacity = (curTuning.opacity - 0.08f).coerceIn(0.05f, 1.0f)))
                    },
                    testTag = "pad_customize_opacity_down_btn"
                )
                SheetPillButton(
                    label = "Mờ +",
                    onClick = {
                        onUpdateTuning(curTuning.copy(opacity = (curTuning.opacity + 0.08f).coerceIn(0.05f, 1.0f)))
                    },
                    testTag = "pad_customize_opacity_up_btn"
                )

                // Dịch vi chỉnh 2px (◀ ▲ ▼ ▶)
                val stepNorm = 0.004f
                SheetPillButton(
                    label = "◀",
                    onClick = {
                        onUpdateTuning(curTuning.copy(dxNorm = (curTuning.dxNorm - stepNorm).coerceIn(-0.42f, 0.42f)))
                    },
                    testTag = "pad_nudge_left_btn"
                )
                SheetPillButton(
                    label = "▲",
                    onClick = {
                        onUpdateTuning(curTuning.copy(dyNorm = (curTuning.dyNorm - stepNorm).coerceIn(-0.42f, 0.42f)))
                    },
                    testTag = "pad_nudge_up_btn"
                )
                SheetPillButton(
                    label = "▼",
                    onClick = {
                        onUpdateTuning(curTuning.copy(dyNorm = (curTuning.dyNorm + stepNorm).coerceIn(-0.42f, 0.42f)))
                    },
                    testTag = "pad_nudge_down_btn"
                )
                SheetPillButton(
                    label = "▶",
                    onClick = {
                        onUpdateTuning(curTuning.copy(dxNorm = (curTuning.dxNorm + stepNorm).coerceIn(-0.42f, 0.42f)))
                    },
                    testTag = "pad_nudge_right_btn"
                )

                // Bề ngang / Bề dọc riêng
                if (!isStickNode) {
                    SheetPillButton(
                        label = "Ngang −",
                        onClick = {
                            onUpdateTuning(curTuning.copy(widthMult = (curTuning.widthMult - 0.08f).coerceIn(0.60f, 1.80f)))
                        },
                        testTag = "pad_width_down_btn"
                    )
                    SheetPillButton(
                        label = "Ngang +",
                        onClick = {
                            onUpdateTuning(curTuning.copy(widthMult = (curTuning.widthMult + 0.08f).coerceIn(0.60f, 1.80f)))
                        },
                        testTag = "pad_width_up_btn"
                    )
                    SheetPillButton(
                        label = "Dọc −",
                        onClick = {
                            onUpdateTuning(curTuning.copy(heightMult = (curTuning.heightMult - 0.08f).coerceIn(0.60f, 1.80f)))
                        },
                        testTag = "pad_height_down_btn"
                    )
                    SheetPillButton(
                        label = "Dọc +",
                        onClick = {
                            onUpdateTuning(curTuning.copy(heightMult = (curTuning.heightMult + 0.08f).coerceIn(0.60f, 1.80f)))
                        },
                        testTag = "pad_height_up_btn"
                    )
                }

                if (selectedNodeKey.startsWith("dpad")) {
                    SheetChoiceChip(
                        label = if (dpadSplitMode) "Gộp D-Pad" else "Tách 4 nút D-Pad",
                        selected = dpadSplitMode,
                        onClick = onToggleSplitDpad,
                        testTag = "pad_customize_split_dpad_btn"
                    )
                }
                if (selectedNodeKey == "abxy" || selectedNodeKey in listOf("btn_a", "btn_b", "btn_x", "btn_y")) {
                    SheetChoiceChip(
                        label = if (abxySplitMode) "Gộp cụm ABXY" else "Tách rời A/B/X/Y",
                        selected = abxySplitMode,
                        onClick = onToggleSplitAbxy,
                        testTag = "pad_customize_split_abxy_btn"
                    )
                }
            }
        } else {
            // TAB 1: Chế độ bấm (Bấm thả / Chạm khóa / Turbo + tốc độ), Vuốt cò LT/RT, Vuốt 4 hướng LB/RB, Combo & Rung riêng
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isStickNode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Chế độ bấm:", "Press mode:"),
                            color = SheetMuted,
                            fontSize = 11.sp
                        )
                        PadPressMode.entries.forEach { pm ->
                            SheetChoiceChip(
                                label = pm.localizedTitle(lang),
                                selected = curTuning.pressMode == pm,
                                onClick = { onUpdateTuning(curTuning.copy(pressMode = pm)) },
                                testTag = if (curTuning.pressMode == pm) "pad_customize_press_mode_btn" else "pad_pm_${pm.id}"
                            )
                        }

                        if (curTuning.pressMode == PadPressMode.TURBO) {
                            Text(
                                text = "Tốc độ Turbo: ${curTuning.turboHz}/s",
                                color = SheetAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            listOf(6, 10, 15, 20).forEach { hz ->
                                SheetChoiceChip(
                                    label = "${hz}/s",
                                    selected = curTuning.turboHz == hz,
                                    onClick = { onUpdateTuning(curTuning.copy(turboHz = hz)) },
                                    testTag = "pad_turbo_hz_$hz"
                                )
                            }
                        }
                    }
                }

                // Tùy chọn riêng cho Cò LT / RT: Vuốt dọc chỉnh lực cò (0-100%)
                if (isTriggerNode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Vuốt dọc chỉnh lực cò (0–100%):", "Analog vertical trigger drag:"),
                            color = SheetMuted,
                            fontSize = 11.sp
                        )
                        listOf(
                            -1 to PadStrings.tr(lang, "Theo Bố cục", "Follow Preset"),
                            1 to PadStrings.tr(lang, "Luôn bật (0–100%)", "Always On"),
                            0 to PadStrings.tr(lang, "Tắt (Nhấn 100%)", "Off (Digital)")
                        ).forEach { (code, title) ->
                            SheetChoiceChip(
                                label = title,
                                selected = curTuning.analogTriggerDrag == code,
                                onClick = { onUpdateTuning(curTuning.copy(analogTriggerDrag = code)) },
                                testTag = "pad_trigger_drag_$code"
                            )
                        }
                    }
                }

                // Tùy chọn riêng cho Nút vai LB / RB: Vuốt 4 hướng độc lập + Gửi kèm nút vai
                if (isShoulderNode) {
                    val cycleBits = listOf(
                        0,
                        RemotePadButtons.L3,
                        RemotePadButtons.R3,
                        RemotePadButtons.Y,
                        RemotePadButtons.X,
                        RemotePadButtons.B,
                        RemotePadButtons.A,
                        RemotePadButtons.DPAD_UP,
                        RemotePadButtons.DPAD_DOWN,
                        RemotePadButtons.DPAD_LEFT,
                        RemotePadButtons.DPAD_RIGHT
                    )
                    fun nextInCycle(cur: Int): Int {
                        val idx = cycleBits.indexOf(cur)
                        return cycleBits[(idx + 1) % cycleBits.size]
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Vuốt 4 hướng:", "4-Way Swipe:"),
                            color = SheetMuted,
                            fontSize = 11.sp
                        )
                        SheetChoiceChip(
                            label = "↑ Lên: ${formatMaskBadge(curTuning.swipeUpBit).ifEmpty { "Tắt" }}",
                            selected = curTuning.swipeUpBit != 0,
                            onClick = { onUpdateTuning(curTuning.copy(swipeUpBit = nextInCycle(curTuning.swipeUpBit))) },
                            testTag = "pad_customize_swipe_btn"
                        )
                        SheetChoiceChip(
                            label = "↓ Xuống: ${formatMaskBadge(curTuning.swipeDownBit).ifEmpty { "Tắt" }}",
                            selected = curTuning.swipeDownBit != 0,
                            onClick = { onUpdateTuning(curTuning.copy(swipeDownBit = nextInCycle(curTuning.swipeDownBit))) },
                            testTag = "pad_swipe_down_btn"
                        )
                        SheetChoiceChip(
                            label = "← Trái: ${formatMaskBadge(curTuning.swipeLeftBit).ifEmpty { "Tắt" }}",
                            selected = curTuning.swipeLeftBit != 0,
                            onClick = { onUpdateTuning(curTuning.copy(swipeLeftBit = nextInCycle(curTuning.swipeLeftBit))) },
                            testTag = "pad_swipe_left_btn"
                        )
                        SheetChoiceChip(
                            label = "→ Phải: ${formatMaskBadge(curTuning.swipeRightBit).ifEmpty { "Tắt" }}",
                            selected = curTuning.swipeRightBit != 0,
                            onClick = { onUpdateTuning(curTuning.copy(swipeRightBit = nextInCycle(curTuning.swipeRightBit))) },
                            testTag = "pad_swipe_right_btn"
                        )
                        SheetChoiceChip(
                            label = if (curTuning.swipeIncludeShoulder) "Gửi kèm LB/RB" else "Chỉ gửi nút vuốt",
                            selected = curTuning.swipeIncludeShoulder,
                            onClick = {
                                onUpdateTuning(curTuning.copy(swipeIncludeShoulder = !curTuning.swipeIncludeShoulder))
                            },
                            testTag = "pad_swipe_include_shoulder_btn"
                        )
                    }
                }

                // Bấm kèm nút khác (Combo) cho mọi nút bấm đơn
                if (!isStickNode && selectedNodeKey != "dpad" && selectedNodeKey != "abxy") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "＋ Bấm kèm (Combo):", "＋ Combo with:"),
                            color = SheetMuted,
                            fontSize = 11.sp
                        )
                        AllPadAssignableButtons.forEach { opt ->
                            val active = (curTuning.comboMask and opt.bit) != 0
                            SheetChoiceChip(
                                label = opt.shortLabel,
                                selected = active,
                                onClick = {
                                    val nextMask = if (active) {
                                        curTuning.comboMask and opt.bit.inv()
                                    } else {
                                        curTuning.comboMask or opt.bit
                                    }
                                    onUpdateTuning(curTuning.copy(comboMask = nextMask))
                                },
                                testTag = "pad_combo_opt_${opt.shortLabel}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SheetCard)
            .border(1.dp, SheetBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = SheetAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
        content()
    }
}

@Composable
private fun SheetChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) SheetSelected else SheetBg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SheetAccent else SheetBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) SheetAccent else SheetText,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SheetPillButton(
    label: String,
    isPrimary: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = when {
        isPrimary -> SheetAccent
        isDanger -> SheetDanger.copy(alpha = 0.14f)
        else -> SheetCard
    }
    val borderColor = when {
        isPrimary -> SheetAccent
        isDanger -> SheetDanger
        else -> SheetBorder
    }
    val textColor = when {
        isPrimary -> Color(0xFF062012)
        isDanger -> SheetDanger
        else -> SheetText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
