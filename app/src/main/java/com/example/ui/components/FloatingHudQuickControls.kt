package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.GameOptimizationProfile
import com.example.data.SettingsRepository
import com.example.data.XboxWebUiMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

/**
 * Tab "Điều khiển" trong Menu nổi mở rộng (chuẩn hud-mockup.html).
 */
@Composable
internal fun HudControlsTabContent(
    config: AppConfig,
    virtualControllerEnabled: Boolean,
    translationActive: Boolean,
    translatedCount: Int,
    gameProfile: GameOptimizationProfile?,
    xboxWebUiMode: XboxWebUiMode,
    onInteraction: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onSelectFortniteControlMode: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onOpenTranslator: () -> Unit,
    onSelectXboxWebUiMode: (XboxWebUiMode) -> Unit,
    onToggleOrientation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HudSectionLabel(config.tr("Tay cầm ảo", "Virtual gamepad"))

        HudSegmentedControl(
            options = listOf(config.tr("Bật", "Show"), config.tr("Ẩn", "Hide")),
            selectedIndex = if (virtualControllerEnabled) 0 else 1,
            onSelect = { idx ->
                onInteraction()
                val wantOn = idx == 0
                if (wantOn != virtualControllerEnabled) {
                    onToggleVirtualController()
                }
            },
            tagPrefix = "hud_ctrl_gamepad_seg"
        )

        HudSliderRow(
            label = config.tr("Cỡ nút", "Button size"),
            valueText = "${(config.gamepadScale * 100).roundToInt()}%",
            value = config.gamepadScale,
            valueRange = 0.75f..1.35f,
            onValueChange = {
                onInteraction()
                SettingsRepository.updateGamepadScale(it)
            }
        )

        HudSliderRow(
            label = config.tr("Độ mờ", "Opacity"),
            valueText = "${(config.gamepadOpacity * 100).roundToInt()}%",
            value = config.gamepadOpacity,
            valueRange = 0.25f..0.95f,
            onValueChange = {
                onInteraction()
                SettingsRepository.updateGamepadOpacity(it)
            }
        )

        if (gameProfile?.gameId == "fortnite") {
            HudSectionLabel(config.tr("Điều khiển Fortnite", "Fortnite controls"))
            HudSegmentedControl(
                options = listOf(config.tr("Cảm ứng", "Touch"), config.tr("Tay cầm", "Gamepad")),
                selectedIndex = if (!virtualControllerEnabled) 0 else 1,
                onSelect = { idx ->
                    onInteraction()
                    onSelectFortniteControlMode(idx == 1)
                },
                tagPrefix = "hud_fortnite_mode_seg"
            )
        }

        // Dịch giao diện Xbox · N mục + nút Chi tiết + công tắc
        HudSurfaceRow(minHeight = 42.dp) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = config.tr("Dịch giao diện Xbox", "Translate Xbox UI"),
                    color = HudInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (translatedCount > 0) {
                    Text(
                        text = config.tr("Đã dịch $translatedCount mục", "$translatedCount items"),
                        color = HudFaint,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudButton(
                    text = config.tr("Chi tiết", "Open"),
                    onClick = {
                        onInteraction()
                        onOpenTranslator()
                    },
                    minHeight = 30.dp,
                    fontSize = 11.5.sp,
                    modifier = Modifier.testTag("hud_expanded_translator_card")
                )
                HudSwitch(
                    checked = translationActive,
                    onCheckedChange = {
                        onInteraction()
                        onToggleTranslation(it)
                    },
                    contentDescription = config.tr("Bật dịch giao diện Xbox", "Toggle Xbox UI translation")
                )
            }
        }

        HudSectionLabel(config.tr("Giao diện web Xbox", "Xbox Web UI"))
        HudSegmentedControl(
            options = listOf(
                config.tr("Giao diện cũ", "Classic UI"),
                config.tr("Giao diện mới", "New UI")
            ),
            selectedIndex = if (xboxWebUiMode == XboxWebUiMode.CLASSIC) 0 else 1,
            onSelect = { idx ->
                onInteraction()
                val next = if (idx == 0) XboxWebUiMode.CLASSIC else XboxWebUiMode.NEW_UI
                if (next != xboxWebUiMode) {
                    onSelectXboxWebUiMode(next)
                }
            },
            tagPrefix = "hud_ui_mode"
        )

        HudSurfaceRow(
            minHeight = 40.dp,
            onClick = {
                onInteraction()
                onToggleOrientation()
            },
            modifier = Modifier.testTag("hud_expanded_rotate_button")
        ) {
            Text(
                text = config.tr("Xoay màn hình", "Rotate screen"),
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            HudPanelIconButton(
                icon = Icons.Default.ScreenRotation,
                contentDescription = config.tr("Xoay màn hình", "Rotate screen"),
                onClick = {
                    onInteraction()
                    onToggleOrientation()
                },
                size = 32.dp,
                tint = HudInk
            )
        }
    }
}

/**
 * Tab "Máy" trong Menu nổi mở rộng (chuẩn hud-mockup.html).
 */
@Composable
internal fun HudDeviceTabContent(
    config: AppConfig,
    batteryPercent: Int,
    isBatteryCharging: Boolean,
    isThermalSupported: Boolean,
    thermalStatus: Int,
    networkStatus: com.example.cloudplay.NetworkOptimizerStatus,
    recorderState: com.example.cloudplay.ScreenRecorderState,
    onInteraction: () -> Unit,
    onToggleWifiSpeedFirst: (Boolean) -> Unit,
    onToggleRecording: () -> Unit
) {
    val batteryVal = if (batteryPercent >= 0) {
        if (isBatteryCharging) {
            config.tr("$batteryPercent% · đang sạc ⚡", "$batteryPercent% · charging ⚡")
        } else {
            "$batteryPercent%"
        }
    } else {
        "—%"
    }
    val batteryColor = when {
        isBatteryCharging -> HudAccent
        batteryPercent in 0..15 -> HudDanger
        batteryPercent in 16..30 -> HudWarn
        else -> HudInk
    }

    val (thermalText, thermalTier) = when {
        !isThermalSupported -> config.tr("Chưa hỗ trợ", "N/A") to HudTagTier.NEUTRAL
        thermalStatus >= 2 -> config.tr("Nóng", "Hot") to HudTagTier.BAD
        thermalStatus == 1 -> config.tr("Ấm", "Warm") to HudTagTier.MID
        else -> config.tr("Mát", "Cool") to HudTagTier.GOOD
    }
    val (thermalBadge, _) = when {
        !isThermalSupported -> "—" to HudTagTier.NEUTRAL
        thermalStatus >= 2 -> config.tr("Nên hạ", "Lower") to HudTagTier.BAD
        thermalStatus == 1 -> config.tr("Chú ý", "Watch") to HudTagTier.MID
        else -> config.tr("Tốt", "Good") to HudTagTier.GOOD
    }

    val netSummary = when {
        networkStatus.isWifi -> {
            val band = when (networkStatus.physicalType) {
                com.example.cloudplay.PhysicalNetworkType.WIFI_6GHZ -> "Wi-Fi 6GHz"
                com.example.cloudplay.PhysicalNetworkType.WIFI_5GHZ -> "Wi-Fi 5GHz"
                com.example.cloudplay.PhysicalNetworkType.WIFI_2_4GHZ -> "Wi-Fi 2.4GHz"
                else -> "Wi-Fi"
            }
            if (networkStatus.isWeakWifiSignal) config.tr("$band · sóng yếu", "$band · weak")
            else config.tr("$band · sóng tốt", "$band · good")
        }
        networkStatus.isCellular -> {
            if (networkStatus.isWeakCellularSignal) config.tr("4G/5G · sóng yếu", "4G/5G · weak")
            else config.tr("4G/5G · ổn định", "4G/5G · stable")
        }
        else -> config.tr("Đang kết nối", "Active")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HudSurfaceRow {
            Text(
                text = config.tr("Pin", "Battery"),
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = batteryVal,
                color = batteryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }

        HudSurfaceRow {
            Text(
                text = config.tr("Nhiệt độ máy", "Device temperature"),
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = thermalText,
                    color = HudMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isThermalSupported) {
                    HudTagChip(text = thermalBadge, tier = thermalTier)
                }
            }
        }

        HudSurfaceRow {
            Text(
                text = config.tr("Mạng", "Network"),
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = netSummary,
                color = if (networkStatus.isWeakWifiSignal || networkStatus.isWeakCellularSignal) HudWarn else HudMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HudSwitchRow(
            title = config.tr("Ưu tiên phản hồi mượt", "Low-latency Wi-Fi lock"),
            checked = config.wifiSpeedFirstPriority,
            onCheckedChange = {
                onInteraction()
                onToggleWifiSpeedFirst(it)
            }
        )

        HudSurfaceRow(minHeight = 42.dp) {
            Text(
                text = config.tr("Quay màn hình", "Screen recording"),
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (recorderState.isRecording) {
                    val m = recorderState.elapsedSeconds / 60
                    val s = recorderState.elapsedSeconds % 60
                    Text(
                        text = "● %02d:%02d".format(m, s),
                        color = HudDanger,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                HudButton(
                    text = if (recorderState.isRecording) {
                        config.tr("Dừng", "Stop")
                    } else {
                        config.tr("Quay", "Record")
                    },
                    onClick = {
                        onInteraction()
                        onToggleRecording()
                    },
                    style = if (recorderState.isRecording) HudButtonStyle.DANGER else HudButtonStyle.DEFAULT,
                    minHeight = 32.dp,
                    fontSize = 12.sp
                )
            }
        }

        HudSectionLabel(config.tr("Menu nổi", "Floating HUD"))

        HudSliderRow(
            label = config.tr("Độ mờ khi không chạm", "Idle opacity"),
            valueText = "${(config.hudIdleOpacity * 100).roundToInt()}%",
            value = config.hudIdleOpacity,
            valueRange = 0.15f..0.90f,
            onValueChange = {
                onInteraction()
                SettingsRepository.updateHudIdleOpacity(it)
            }
        )

        HudSurfaceRow(minHeight = 42.dp) {
            Text(
                text = config.tr("Vị trí", "Position"),
                color = HudInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = config.tr("Kéo để di chuyển, thả gần mép để dính", "Drag anywhere, snap near edge"),
                color = HudFaint,
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
