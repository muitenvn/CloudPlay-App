package com.example.ui.tv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloudplay.GamepadDetector
import com.example.data.AppConfig
import com.example.data.AppLanguage
import com.example.data.AppLogger
import com.example.data.AppUiMode
import com.example.data.CellularOptimizationMode
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.POPULAR_XBOX_LANGUAGES
import com.example.data.ScreenRecordQuality
import com.example.data.SettingsRepository
import com.example.data.StreamFpsCap
import com.example.data.UpdateCheckState
import com.example.data.UpdateChecker
import com.example.data.XboxWebUiMode
import com.example.ui.components.AppLogDialog
import com.example.ui.components.GuideSection
import com.example.ui.components.WebDownloadPortalDialog
import com.example.ui.components.gamepadActionButton
import com.example.ui.components.gamepadClickable
import com.example.ui.components.gamepadControllerScope
import com.example.ui.tabs.FullTermsInAppDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine
import com.example.vpn.VpnManager
import kotlin.math.roundToInt

private enum class TvSettingsCategory(val titleVi: String, val titleEn: String) {
    GRAPHICS("Hình ảnh & FPS", "Display & FPS"),
    NETWORK("Mạng & VPN", "Network & VPN"),
    CONTROLS("Tay cầm & Cảm ứng", "Controller & Touch"),
    LANGUAGE("Ngôn ngữ & Dịch", "Language & Translate"),
    RECORDING("Quay màn hình", "Recording"),
    ADVANCED("Chế độ nâng cao", "Advanced Mode"),
    INFO("Thông tin & Tài khoản", "Info & Account")
}

@Composable
internal fun TvSettingsScreen(
    config: AppConfig,
    vpnState: VpnConnectionState,
    scale: Float,
    onOpenGuide: (GuideSection) -> Unit,
    onClearCookies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEn = config.isSettingsEnglishUI()
    var selectedCat by rememberSaveable { mutableStateOf(TvSettingsCategory.GRAPHICS) }

    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showAppLogDialog by remember { mutableStateOf(false) }
    var showFullTermsDialog by remember { mutableStateOf(false) }
    var showResetAdvancedConfirmDialog by remember { mutableStateOf(false) }
    var showResetGamepadConfirmDialog by remember { mutableStateOf(false) }
    var showClearCookiesConfirmDialog by remember { mutableStateOf(false) }
    var activeRemapSourceBtn by remember { mutableStateOf<Int?>(null) }
    var activeShortcutTargetAction by remember { mutableStateOf<String?>(null) }

    val gamepadDetector = remember(context) { GamepadDetector(context) }
    DisposableEffect(gamepadDetector) {
        onDispose { gamepadDetector.release() }
    }
    val gamepadStatus by gamepadDetector.status.collectAsState()
    val updateState by UpdateChecker.state.collectAsState()
    val currentVersionName = remember { UpdateChecker.getCurrentVersionName(context) }

    fun triggerControllerTestRumble() {
        try {
            var vibratedGamepad = false
            for (id in InputDevice.getDeviceIds()) {
                val dev = InputDevice.getDevice(id) ?: continue
                val isPad = (dev.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (dev.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                if (isPad && !dev.isVirtual) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = dev.vibratorManager
                        if (vm.vibratorIds.isNotEmpty()) {
                            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(140L, 220))
                            vibratedGamepad = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val v = dev.vibrator
                        if (v.hasVibrator()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                v.vibrate(VibrationEffect.createOneShot(140L, 220))
                            } else {
                                @Suppress("DEPRECATION")
                                v.vibrate(140L)
                            }
                            vibratedGamepad = true
                        }
                    }
                }
            }
            if (!vibratedGamepad) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    val vib = vm?.defaultVibrator
                    if (vib != null && vib.hasVibrator()) {
                        vib.vibrate(VibrationEffect.createOneShot(120L, 210))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (v != null && v.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(120L, 210))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(120L)
                        }
                    }
                }
            }
            Toast.makeText(
                context,
                config.tr("📳 Đã gửi tín hiệu thử rung tay cầm", "📳 Vibration test signal sent"),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tv_settings_screen"),
        verticalArrangement = Arrangement.spacedBy((12.dp * scale).coerceAtLeast(8.dp))
    ) {
        // 1. Header (#s-set .top in tv-mockup.html) + Quick UI Mode bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.tr("Cài đặt", "Settings"),
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (22f * scale).coerceAtLeast(16.5f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            val uiModes = AppUiMode.entries
            val selectedUiIdx = uiModes.indexOf(config.appUiMode).coerceAtLeast(0)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6.dp * scale).coerceAtLeast(4.dp))
            ) {
                Text(
                    text = config.tr("Giao diện:", "Interface:"),
                    color = TvMuted,
                    fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp,
                    fontWeight = FontWeight.SemiBold
                )
                TvSegmentedControl(
                    options = uiModes.map { config.tr(it.titleVi, it.titleEn) },
                    selectedIndex = selectedUiIdx,
                    onSelect = { idx ->
                        uiModes.getOrNull(idx)?.let { SettingsRepository.updateAppUiMode(it) }
                    },
                    scale = scale * 0.9f,
                    modifier = Modifier.testTag("tv_settings_header_ui_mode_segment")
                )
            }
        }

        // 2. Two-Column Layout (.set-wrap -> .set-cats + .set-pane in tv-mockup.html)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy((14.dp * scale).coerceAtLeast(9.dp))
        ) {
            // Left Category Column (.set-cats)
            val catWidth = (182.dp * scale).coerceIn(136.dp, 218.dp)
            Column(
                modifier = Modifier
                    .width(catWidth)
                    .fillMaxHeight()
                    .testTag("tv_settings_categories_column"),
                verticalArrangement = Arrangement.spacedBy((4.dp * scale).coerceAtLeast(3.dp))
            ) {
                TvSettingsCategory.entries.forEach { cat ->
                    val isCurrent = selectedCat == cat
                    val itemCorner = (10.dp * scale).coerceAtLeast(8.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = (36.dp * scale).coerceAtLeast(30.dp))
                            .clip(RoundedCornerShape(itemCorner))
                            .background(if (isCurrent) TvSurface2 else Color.Transparent)
                            .gamepadClickable(
                                cornerRadius = itemCorner,
                                onClick = { selectedCat = cat }
                            )
                            .padding(
                                horizontal = (11.dp * scale).coerceAtLeast(8.dp),
                                vertical = (6.dp * scale).coerceAtLeast(4.dp)
                            )
                            .testTag("tv_settings_cat_${cat.name.lowercase()}"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = config.tr(cat.titleVi, cat.titleEn),
                            color = if (isCurrent) TvAccent else TvMuted,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = (13.5f * scale).coerceAtLeast(11f).sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right Settings Pane (.set-pane)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("tv_settings_pane_${selectedCat.name.lowercase()}"),
                verticalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(5.dp)),
                contentPadding = PaddingValues(bottom = (10.dp * scale).coerceAtLeast(6.dp))
            ) {
                when (selectedCat) {
                    TvSettingsCategory.GRAPHICS -> {
                        item(key = "tv_set_res") {
                            val resModes = listOf(
                                CellularOptimizationMode.AUTO to config.tr("Tự động", "Auto"),
                                CellularOptimizationMode.DATA_SAVER_HIGH_SPEED to "720p",
                                CellularOptimizationMode.MEDIUM to "900p",
                                CellularOptimizationMode.HIGH to "1080p",
                                CellularOptimizationMode.MAX_SPEED_UNCAPPED to "1080p+"
                            )
                            val curIdx = resModes.indexOfFirst { it.first == config.cellularMode }.coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Độ phân giải mục tiêu", "Target Resolution"),
                                subtitle = config.tr(
                                    "Tự động giảm khi mạng yếu để tránh đứng hình.",
                                    "Auto lowers resolution on weak networks to prevent stutter."
                                ),
                                options = resModes.map { it.second },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    resModes.getOrNull(idx)?.let { (mode, _) ->
                                        SettingsRepository.updateCellularMode(mode)
                                    }
                                },
                                scale = scale,
                                testTag = "tv_setting_resolution_row"
                            )
                        }

                        item(key = "tv_set_fps") {
                            val fpsList = StreamFpsCap.entries
                            val curIdx = fpsList.indexOf(config.streamFpsCap).coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Giới hạn khung hình (FPS)", "Frame Rate Limit (FPS)"),
                                subtitle = config.tr(
                                    "60 FPS là mức chuẩn cho Xbox Cloud trên TV.",
                                    "60 FPS is standard for Xbox Cloud on TV."
                                ),
                                options = fpsList.map { "${it.fps}" },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    fpsList.getOrNull(idx)?.let { SettingsRepository.updateStreamFpsCap(it) }
                                },
                                scale = scale,
                                testTag = "tv_setting_fps_row"
                            )
                        }

                        item(key = "tv_set_clarity") {
                            TvSettingToggleRow(
                                title = config.tr("Làm nét hình ảnh", "Sharpen Image"),
                                subtitle = config.tr(
                                    "Tăng độ chi tiết trên màn hình lớn mà không tốn băng thông.",
                                    "Enhances detail on big screens without using extra bandwidth."
                                ),
                                checked = config.clarityBoostEnabled,
                                onCheckedChange = { SettingsRepository.updateClarityBoost(it) },
                                scale = scale,
                                testTag = "tv_setting_clarity_row"
                            )
                        }

                        item(key = "tv_set_smooth_resp") {
                            TvSettingToggleRow(
                                title = config.tr("Ưu tiên phản hồi mượt", "Prioritize Smooth Response"),
                                subtitle = config.tr(
                                    "Giảm độ trễ khung hình khi điều khiển bằng tay cầm.",
                                    "Reduces frame buffer latency for controller input."
                                ),
                                checked = config.wifiSpeedFirstPriority,
                                onCheckedChange = { SettingsRepository.updateWifiSpeedFirstPriority(it) },
                                scale = scale,
                                testTag = "tv_setting_smooth_response_row"
                            )
                        }

                        item(key = "tv_set_gpu_reduce") {
                            TvSettingToggleRow(
                                title = config.tr("Giảm tải GPU", "Reduce GPU Load"),
                                subtitle = config.tr(
                                    "Tắt hiệu ứng nền phụ để giữ tốc độ giải mã ổn định trên TV Box.",
                                    "Disables extra visual effects for stable decoding on TV boxes."
                                ),
                                checked = config.gpuLoadReductionEnabled,
                                onCheckedChange = { SettingsRepository.updateGpuLoadReductionEnabled(it) },
                                scale = scale,
                                testTag = "tv_setting_gpu_reduction_row"
                            )
                        }

                        item(key = "tv_set_xbox_webui") {
                            val uiModes = XboxWebUiMode.entries
                            val curIdx = uiModes.indexOf(config.xboxWebUiMode).coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Giao diện web Xbox", "Xbox Web Interface"),
                                subtitle = config.tr(
                                    "Chọn giao diện Cũ (xbox.com/play) hoặc Mới (play.xbox.com).",
                                    "Choose between Classic (xbox.com/play) or New (play.xbox.com)."
                                ),
                                options = uiModes.map { it.localizedTitle(isEn) },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    uiModes.getOrNull(idx)?.let { SettingsRepository.updateXboxWebUiMode(it) }
                                },
                                scale = scale,
                                testTag = "tv_setting_xbox_web_ui_row"
                            )
                        }

                        item(key = "tv_set_auto_exit_preview") {
                            TvSettingToggleRow(
                                title = config.tr("Tự động xử lý thoát Giao diện mới", "Auto-Exit New UI Handler"),
                                subtitle = config.tr(
                                    "Tự động đồng bộ về Giao diện cũ khi bấm Thoát xem trước trên trang Xbox.",
                                    "Automatically syncs back to Classic UI when opting out of preview."
                                ),
                                checked = config.autoExitPreviewHandlerEnabled,
                                onCheckedChange = { SettingsRepository.updateAutoExitPreviewHandlerEnabled(it) },
                                scale = scale,
                                testTag = "tv_setting_auto_exit_preview_row"
                            )
                        }
                    }

                    TvSettingsCategory.NETWORK -> {
                        item(key = "tv_set_auto_vpn") {
                            val autoVpnOn = config.smartVpnEnabled && config.autoBypassEnabled
                            TvSettingToggleRow(
                                title = config.tr("Tự bật VPN khi mở game ➔ Tự tắt khi vào chơi", "Auto VPN (Unlock on launch ➔ Off in-game)"),
                                subtitle = config.tr(
                                    "Bật VPN để qua kiểm tra vùng lúc mở game, sau đó chuyển sang mạng trực tiếp ping thấp.",
                                    "Connects VPN for region check, then switches to low-ping direct network."
                                ),
                                checked = autoVpnOn,
                                onCheckedChange = { enabled ->
                                    SettingsRepository.updateSmartVpnEnabled(enabled)
                                    SettingsRepository.updateAutoBypass(enabled)
                                },
                                scale = scale,
                                testTag = "tv_setting_auto_vpn_row"
                            )
                        }

                        item(key = "tv_set_vpn_engine") {
                            val engines = listOf(
                                VpnEngine.SMART_BYPASS to config.tr("Chỉ lúc vào game 🇯🇵", "Launch Only 🇯🇵"),
                                VpnEngine.PROXY_TUNNEL to config.tr("Máy chủ trung gian", "Proxy Tunnel"),
                                VpnEngine.EXTERNAL_VPN to config.tr("VPN ngoài", "External VPN")
                            )
                            val curIdx = engines.indexOfFirst { it.first == vpnState.engine }.coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Chế độ mở khóa vùng", "Region Unlock Mode"),
                                subtitle = config.tr(
                                    "Khuyên dùng 'Chỉ lúc vào game' để không làm tăng ping khi đang chơi.",
                                    "Recommended: 'Launch Only' so in-game ping stays direct."
                                ),
                                options = engines.map { it.second },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    engines.getOrNull(idx)?.let { (eng, _) -> VpnManager.setEngine(eng) }
                                },
                                scale = scale,
                                testTag = "tv_setting_vpn_engine_row"
                            )
                        }

                        item(key = "tv_set_vpn_delay") {
                            TvSettingSliderRow(
                                title = config.tr("Thời gian chờ tự tắt VPN", "Auto VPN Disconnect Delay"),
                                subtitle = config.tr(
                                    "Số giây giữ VPN sau khi luồng game bắt đầu (1 – 8 giây).",
                                    "Seconds to hold VPN after stream starts (1 – 8s)."
                                ),
                                valueText = config.tr("${config.autoBypassDelaySeconds} giây", "${config.autoBypassDelaySeconds}s"),
                                fraction = ((config.autoBypassDelaySeconds - 1) / 7f).coerceIn(0f, 1f),
                                onStep = { dir ->
                                    val next = (config.autoBypassDelaySeconds + dir).coerceIn(1, 8)
                                    SettingsRepository.updateAutoBypassDelay(next)
                                },
                                scale = scale,
                                testTag = "tv_setting_vpn_delay_row"
                            )
                        }
                    }

                    TvSettingsCategory.CONTROLS -> {
                        item(key = "tv_set_controller_status") {
                            val statusSub = if (gamepadStatus.hasPhysicalController) {
                                "${gamepadStatus.controllerBrand} • ${gamepadStatus.connectionTransport}"
                            } else {
                                config.tr(
                                    "Hỗ trợ Xbox, PlayStation, Nintendo, 8BitDo, GameSir, điều khiển TV",
                                    "Supports Xbox, PlayStation, Nintendo, 8BitDo, GameSir, TV remote"
                                )
                            }
                            TvSettingActionRow(
                                title = if (gamepadStatus.hasPhysicalController) {
                                    "🎮 ${gamepadStatus.controllerName}"
                                } else {
                                    config.tr("🎮 Chưa kết nối tay cầm Bluetooth / USB", "🎮 No physical controller detected")
                                },
                                subtitle = statusSub,
                                actionLabel = config.tr("📳 Thử rung", "📳 Test Rumble"),
                                scale = scale,
                                onClick = {
                                    gamepadDetector.checkControllers()
                                    triggerControllerTestRumble()
                                },
                                testTag = "tv_setting_test_rumble_row"
                            )
                        }

                        item(key = "tv_set_layout_preset") {
                            val presets = GamepadLayoutPreset.entries
                            val curIdx = presets.indexOf(config.gamepadLayoutPreset).coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Bố cục nút bấm (Chuẩn Xbox / Đảo nút)", "Button Layout Preset"),
                                subtitle = presets[curIdx].localizedDesc(isEn),
                                options = presets.map { it.localizedTitle(isEn) },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    presets.getOrNull(idx)?.let { SettingsRepository.updateGamepadLayoutPreset(it) }
                                },
                                scale = scale,
                                testTag = "tv_setting_layout_preset_row"
                            )
                        }

                        item(key = "tv_set_phys_l_inner") {
                            val pct = (config.physLeftStickInnerDeadzone * 100).roundToInt()
                            TvSettingSliderRow(
                                title = config.tr("Cần trái thật · Vùng chết trong", "Physical Left Stick · Inner Deadzone"),
                                subtitle = config.tr("Tăng nhẹ nếu cần analog tay cầm bị trôi (0% – 30%).", "Increase slightly if stick drifts (0% – 30%)."),
                                valueText = "$pct%",
                                fraction = (config.physLeftStickInnerDeadzone / 0.30f).coerceIn(0f, 1f),
                                onStep = { dir ->
                                    val next = (config.physLeftStickInnerDeadzone + dir * 0.02f).coerceIn(0f, 0.30f)
                                    SettingsRepository.updatePhysLeftStickInnerDeadzone(next)
                                },
                                scale = scale,
                                testTag = "tv_setting_phys_l_inner_row"
                            )
                        }

                        item(key = "tv_set_phys_r_inner") {
                            val pct = (config.physRightStickInnerDeadzone * 100).roundToInt()
                            TvSettingSliderRow(
                                title = config.tr("Cần phải thật · Vùng chết trong", "Physical Right Stick · Inner Deadzone"),
                                subtitle = config.tr("Chống trôi tâm ngắm cho cần phải (0% – 30%).", "Prevents camera drift on right stick (0% – 30%)."),
                                valueText = "$pct%",
                                fraction = (config.physRightStickInnerDeadzone / 0.30f).coerceIn(0f, 1f),
                                onStep = { dir ->
                                    val next = (config.physRightStickInnerDeadzone + dir * 0.02f).coerceIn(0f, 0.30f)
                                    SettingsRepository.updatePhysRightStickInnerDeadzone(next)
                                },
                                scale = scale,
                                testTag = "tv_setting_phys_r_inner_row"
                            )
                        }

                        item(key = "tv_set_virtual_pad") {
                            TvSettingToggleRow(
                                title = config.tr("Bật tay cầm ảo (Khi dùng trên màn hình cảm ứng)", "Show Virtual Gamepad (Touchscreen)"),
                                subtitle = config.tr(
                                    "Tự động tắt khi đang ở chế độ giao diện TV.",
                                    "Automatically hidden while in TV UI mode."
                                ),
                                checked = config.showVirtualController,
                                onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                                scale = scale,
                                testTag = "tv_setting_virtual_pad_row"
                            )
                        }

                        item(key = "tv_set_auto_hide_virtual") {
                            TvSettingToggleRow(
                                title = config.tr("Tự ẩn tay cầm ảo khi cắm tay cầm thật", "Auto-Hide Virtual Gamepad on Controller"),
                                subtitle = config.tr(
                                    "Ẩn nút cảm ứng ngay khi phát hiện tay cầm Bluetooth hoặc USB.",
                                    "Hides on-screen buttons when a Bluetooth/USB controller connects."
                                ),
                                checked = config.autoHideVirtualOnPhysicalController,
                                onCheckedChange = { SettingsRepository.updateAutoHideVirtualOnPhysicalController(it) },
                                scale = scale,
                                testTag = "tv_setting_auto_hide_virtual_row"
                            )
                        }

                        item(key = "tv_set_per_game_layout") {
                            TvSettingToggleRow(
                                title = config.tr("Lưu bố cục riêng theo từng game", "Save Layout Per Game"),
                                subtitle = config.tr(
                                    "Nhớ vị trí, kích thước và cấu hình nút vai riêng cho mỗi game.",
                                    "Remembers button layout and shoulder swipe settings per game."
                                ),
                                checked = config.usePerGameGamepadLayout,
                                onCheckedChange = { SettingsRepository.updateUsePerGameGamepadLayout(it) },
                                scale = scale,
                                testTag = "tv_setting_per_game_layout_row"
                            )
                        }

                        item(key = "tv_set_auto_sprint") {
                            TvSettingToggleRow(
                                title = config.tr("🏃 Đẩy kịch cần trái để chạy nhanh", "🏃 Push Left Stick to Auto-Sprint"),
                                subtitle = config.tr(
                                    "Tự kích hoạt L3 khi đẩy cần trái lên hết cỡ.",
                                    "Automatically triggers L3 when pushing left stick fully forward."
                                ),
                                checked = config.gamepadSprintLockEnabled,
                                onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) },
                                scale = scale,
                                testTag = "tv_setting_auto_sprint_row"
                            )
                        }

                        item(key = "tv_set_reset_pad_layout") {
                            TvSettingActionRow(
                                title = config.tr("Đặt lại bố cục tay cầm mặc định", "Reset Gamepad Layout to Default"),
                                subtitle = config.tr(
                                    "Khôi phục vị trí và kích thước nút cảm ứng về mặc định.",
                                    "Restores virtual button positions and sizes to default."
                                ),
                                actionLabel = config.tr("Đặt lại", "Reset"),
                                scale = scale,
                                onClick = { showResetGamepadConfirmDialog = true },
                                testTag = "tv_setting_reset_gamepad_row"
                            )
                        }
                    }

                    TvSettingsCategory.LANGUAGE -> {
                        item(key = "tv_set_auto_translate") {
                            TvSettingToggleRow(
                                title = config.tr("Tự động dịch giao diện trang Xbox", "Auto-translate Xbox Interface"),
                                subtitle = config.tr(
                                    "Dịch menu và thông báo trên trang Xbox Cloud sang ngôn ngữ đã chọn.",
                                    "Translates Xbox Cloud menus and prompts into your target language."
                                ),
                                checked = config.isEffectiveTranslateEnabled(),
                                onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                                scale = scale,
                                testTag = "tv_setting_auto_translate_row"
                            )
                        }

                        item(key = "tv_set_translate_target") {
                            val translateOptions = listOf(
                                SettingsRepository.TRANSLATE_LANG_OFF to config.tr("Tắt", "Off"),
                                "vi" to "🇻🇳 Tiếng Việt",
                                "es" to "🇪🇸 Español",
                                "pt" to "🇧🇷 Português",
                                "fr" to "🇫🇷 Français",
                                "de" to "🇩🇪 Deutsch",
                                "ja" to "🇯🇵 日本語",
                                "ko" to "🇰🇷 한국어",
                                "zh-CN" to "🇨🇳 中文"
                            )
                            val curCode = if (config.isEffectiveTranslateEnabled()) {
                                config.getTargetTranslateLanguageCode()
                            } else {
                                SettingsRepository.TRANSLATE_LANG_OFF
                            }
                            val curIdx = translateOptions.indexOfFirst { it.first.equals(curCode, ignoreCase = true) }.coerceAtLeast(0)
                            TvSettingSliderRow(
                                title = config.tr("Ngôn ngữ dịch giao diện Xbox", "Xbox Translation Language"),
                                subtitle = config.tr("Dùng ◀ ▶ hoặc bấm A để đổi ngôn ngữ dịch.", "Use ◀ ▶ or press A to cycle translation language."),
                                valueText = translateOptions[curIdx].second,
                                fraction = curIdx.toFloat() / translateOptions.lastIndex.coerceAtLeast(1).toFloat(),
                                onStep = { dir ->
                                    val next = (curIdx + dir + translateOptions.size) % translateOptions.size
                                    SettingsRepository.updateTranslateLanguage(translateOptions[next].first)
                                },
                                scale = scale,
                                testTag = "tv_setting_translate_lang_row"
                            )
                        }

                        item(key = "tv_set_app_language") {
                            val appLangs = AppLanguage.entries
                            val curIdx = appLangs.indexOf(config.appLanguage).coerceAtLeast(0)
                            TvSettingSliderRow(
                                title = config.tr("Ngôn ngữ ứng dụng CloudPlay", "CloudPlay App Language"),
                                subtitle = config.tr("Dùng ◀ ▶ hoặc bấm A để đổi ngôn ngữ hiển thị của ứng dụng.", "Use ◀ ▶ or press A to switch app language."),
                                valueText = "${config.appLanguage.effectiveFlag()} ${config.appLanguage.localizedTitle(isEn)}",
                                fraction = curIdx.toFloat() / appLangs.lastIndex.coerceAtLeast(1).toFloat(),
                                onStep = { dir ->
                                    val next = (curIdx + dir + appLangs.size) % appLangs.size
                                    SettingsRepository.updateAppLanguage(appLangs[next])
                                },
                                scale = scale,
                                testTag = "tv_setting_app_language_row"
                            )
                        }
                    }

                    TvSettingsCategory.RECORDING -> {
                        item(key = "tv_set_record_quality") {
                            val qualities = ScreenRecordQuality.entries
                            val curIdx = qualities.indexOf(config.screenRecordQuality).coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Độ nét quay màn hình (.MP4)", "Screen Recording Quality (.MP4)"),
                                subtitle = config.tr(
                                    "Quay trực tiếp luồng video WebRTC không làm tụt FPS khi chơi.",
                                    "Records the WebRTC video stream directly without dropping in-game FPS."
                                ),
                                options = qualities.map { it.localizedShort(isEn) },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    qualities.getOrNull(idx)?.let { SettingsRepository.updateScreenRecordQuality(it) }
                                },
                                scale = scale,
                                testTag = "tv_setting_record_quality_row"
                            )
                        }
                    }

                    TvSettingsCategory.ADVANCED -> {
                        // 1. Giao diện: Tự động / Điện thoại / TV (Yêu cầu Đợt 2 & Đợt 5)
                        item(key = "tv_adv_ui_mode") {
                            val modes = AppUiMode.entries
                            val curIdx = modes.indexOf(config.appUiMode).coerceAtLeast(0)
                            TvSettingSegmentRow(
                                title = config.tr("Giao diện", "Interface"),
                                subtitle = config.tr(
                                    "Tự động nhận biết TV hoặc chuyển thủ công ngay lập tức giữa Điện thoại và TV.",
                                    "Auto-detect TV or switch immediately between Phone and TV interface."
                                ),
                                options = modes.map { config.tr(it.titleVi, it.titleEn) },
                                selectedIndex = curIdx,
                                onSelect = { idx ->
                                    modes.getOrNull(idx)?.let { SettingsRepository.updateAppUiMode(it) }
                                },
                                scale = scale,
                                testTag = "tv_adv_setting_ui_mode_row"
                            )
                        }

                        // 2. Màu sắc hình ảnh (Độ tương phản, Độ tươi màu, Độ sáng) + Đặt lại màu
                        item(key = "tv_adv_contrast") {
                            TvSettingSliderRow(
                                title = config.tr("Độ tương phản hình ảnh", "Video Contrast"),
                                subtitle = config.tr("Chỉnh độ sâu vùng sáng/tối (80% – 140%).", "Adjust light/dark contrast (80% – 140%)."),
                                valueText = "${config.videoContrast}%",
                                fraction = ((config.videoContrast - 80) / 60f).coerceIn(0f, 1f),
                                onStep = { dir ->
                                    SettingsRepository.updateVideoContrast((config.videoContrast + dir * 5).coerceIn(80, 140))
                                },
                                scale = scale,
                                testTag = "tv_adv_contrast_row"
                            )
                        }

                        item(key = "tv_adv_saturation") {
                            TvSettingSliderRow(
                                title = config.tr("Độ tươi màu", "Color Saturation"),
                                subtitle = config.tr("Tăng độ rực rỡ màu sắc trên màn hình TV (80% – 150%).", "Boost color vibrancy on TV (80% – 150%)."),
                                valueText = "${config.videoSaturation}%",
                                fraction = ((config.videoSaturation - 80) / 70f).coerceIn(0f, 1f),
                                onStep = { dir ->
                                    SettingsRepository.updateVideoSaturation((config.videoSaturation + dir * 5).coerceIn(80, 150))
                                },
                                scale = scale,
                                testTag = "tv_adv_saturation_row"
                            )
                        }

                        item(key = "tv_adv_brightness") {
                            TvSettingSliderRow(
                                title = config.tr("Độ sáng hình ảnh", "Video Brightness"),
                                subtitle = config.tr("Làm sáng các cảnh tối trong game (80% – 130%).", "Brighten dark scenes in game (80% – 130%)."),
                                valueText = "${config.videoBrightness}%",
                                fraction = ((config.videoBrightness - 80) / 50f).coerceIn(0f, 1f),
                                onStep = { dir ->
                                    SettingsRepository.updateVideoBrightness((config.videoBrightness + dir * 5).coerceIn(80, 130))
                                },
                                scale = scale,
                                testTag = "tv_adv_brightness_row"
                            )
                        }

                        item(key = "tv_adv_reset_colors") {
                            TvSettingActionRow(
                                title = config.tr("Đặt lại màu sắc mặc định", "Reset Video Colors to Default"),
                                subtitle = config.tr("Đưa Tương phản, Tươi màu và Độ sáng về 100%.", "Restore Contrast, Saturation, and Brightness to 100%."),
                                actionLabel = config.tr("Đặt lại 100%", "Reset 100%"),
                                scale = scale,
                                onClick = {
                                    SettingsRepository.resetVideoDisplayDefaults()
                                    Toast.makeText(
                                        context,
                                        config.tr("Đã đặt lại màu sắc mặc định", "Reset video colors to default"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                testTag = "tv_adv_reset_colors_row"
                            )
                        }

                        item(key = "tv_adv_low_end") {
                            TvSettingToggleRow(
                                title = config.tr("Chế độ máy yếu & Tiết kiệm pin", "Low-End & Battery Saver Mode"),
                                subtitle = config.tr(
                                    "Dùng mức 720p nhẹ và tắt hiệu ứng phụ cho TV Box cấu hình thấp.",
                                    "Uses lightweight 720p stream and reduced effects for low-spec TV boxes."
                                ),
                                checked = config.lowEndDeviceMode && config.gpuLoadReductionEnabled,
                                onCheckedChange = { enabled ->
                                    SettingsRepository.updateLowEndDeviceMode(enabled)
                                    SettingsRepository.updateGpuLoadReductionEnabled(enabled)
                                },
                                scale = scale,
                                testTag = "tv_adv_low_end_row"
                            )
                        }

                        // 3. Đổi nút tay cầm thật (Remap) & Tổ hợp phím tắt View + nút
                        item(key = "tv_adv_remap_header") {
                            val activeButtonMap = remember(config.physicalGamepadButtonMap) {
                                SettingsRepository.parsePhysicalButtonMap(config.physicalGamepadButtonMap)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape((16.dp * scale).coerceAtLeast(12.dp)))
                                    .background(TvSurface)
                                    .padding((18.dp * scale).coerceAtLeast(12.dp)),
                                verticalArrangement = Arrangement.spacedBy((10.dp * scale).coerceAtLeast(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = config.tr("Đổi nút cho tay cầm thật (Remap)", "Physical Controller Button Remapping"),
                                            color = TvInk,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = (17.5f * scale).coerceAtLeast(14f).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = config.tr("Bấm A vào nút bất kỳ để gán sang tín hiệu nút Xbox khác.", "Press A on any button to map it to another Xbox button."),
                                            color = TvMuted,
                                            fontSize = (13.5f * scale).coerceAtLeast(11f).sp
                                        )
                                    }
                                    TvChip(
                                        text = config.tr("Đặt lại", "Reset"),
                                        selected = false,
                                        scale = scale * 0.85f,
                                        onClick = {
                                            SettingsRepository.resetPhysicalGamepadButtonMap()
                                            Toast.makeText(
                                                context,
                                                config.tr("Đã đặt lại ánh xạ nút tay cầm thật", "Reset button mapping"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }

                                SettingsRepository.STANDARD_GAMEPAD_BUTTONS.chunked(4).forEach { rowButtons ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
                                    ) {
                                        rowButtons.forEach { (btnIdx, shortName, _) ->
                                            val mappedTarget = activeButtonMap[btnIdx] ?: btnIdx
                                            val targetLabel = SettingsRepository.getGamepadButtonShortLabel(mappedTarget)
                                            val isRemapped = mappedTarget != btnIdx
                                            val btnCorner = (10.dp * scale).coerceAtLeast(8.dp)
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(btnCorner))
                                                    .background(if (isRemapped) TvSel else TvSurface2)
                                                    .gamepadClickable(
                                                        cornerRadius = btnCorner,
                                                        onClick = { activeRemapSourceBtn = btnIdx }
                                                    )
                                                    .padding(vertical = (8.dp * scale).coerceAtLeast(6.dp), horizontal = (6.dp * scale).coerceAtLeast(4.dp)),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = shortName,
                                                    color = TvMuted,
                                                    fontSize = (12f * scale).coerceAtLeast(10f).sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = if (isRemapped) "→ $targetLabel" else targetLabel,
                                                    color = if (isRemapped) TvAccent else TvInk,
                                                    fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        repeat(4 - rowButtons.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "tv_adv_shortcuts_toggle") {
                            TvSettingToggleRow(
                                title = config.tr("Tổ hợp phím tắt trên tay cầm (Giữ View + nút)", "Controller Combo Shortcuts (Hold View + Button)"),
                                subtitle = config.tr(
                                    "Giữ View 400ms + bấm nút phụ để bật/tắt thông số luồng hoặc quay màn hình.",
                                    "Hold View 400ms + press trigger button to toggle stream stats or recording."
                                ),
                                checked = config.controllerShortcutsEnabled,
                                onCheckedChange = { SettingsRepository.updateControllerShortcutsEnabled(it) },
                                scale = scale,
                                testTag = "tv_adv_controller_shortcuts_row"
                            )
                        }

                        if (config.controllerShortcutsEnabled) {
                            item(key = "tv_adv_shortcut_stats") {
                                val btnShort = SettingsRepository.getGamepadButtonShortLabel(config.shortcutToggleStatsButton)
                                TvSettingActionRow(
                                    title = config.tr("Phím tắt: Ẩn / hiện thông số luồng", "Shortcut: Show / Hide Stream Stats"),
                                    subtitle = config.tr("Bấm A để đổi nút kích hoạt.", "Press A to change trigger button."),
                                    actionLabel = "View + $btnShort",
                                    scale = scale,
                                    onClick = { activeShortcutTargetAction = "toggle_stats" },
                                    testTag = "tv_adv_shortcut_stats_row"
                                )
                            }
                            item(key = "tv_adv_shortcut_record") {
                                val btnShort = SettingsRepository.getGamepadButtonShortLabel(config.shortcutToggleRecordingButton)
                                TvSettingActionRow(
                                    title = config.tr("Phím tắt: Bật / tắt quay màn hình", "Shortcut: Start / Stop Recording"),
                                    subtitle = config.tr("Bấm A để đổi nút kích hoạt.", "Press A to change trigger button."),
                                    actionLabel = "View + $btnShort",
                                    scale = scale,
                                    onClick = { activeShortcutTargetAction = "toggle_recording" },
                                    testTag = "tv_adv_shortcut_record_row"
                                )
                            }
                        }

                        // 4. Song ngữ & Thị trường Xbox Cloud
                        item(key = "tv_adv_bilingual") {
                            TvSettingToggleRow(
                                title = config.tr("Chế độ dịch song ngữ (Bản dịch + Gốc)", "Bilingual Translation Mode"),
                                subtitle = config.tr(
                                    "Hiện kèm từ gốc tiếng Anh trong ngoặc đơn.",
                                    "Displays original text in parentheses alongside translation."
                                ),
                                checked = config.bilingualMode,
                                onCheckedChange = { SettingsRepository.updateBilingualMode(it) },
                                scale = scale,
                                testTag = "tv_adv_bilingual_row"
                            )
                        }

                        item(key = "tv_adv_market") {
                            val marketOptions = listOf("en-US" to "🇺🇸 en-US (Mặc định)") +
                                POPULAR_XBOX_LANGUAGES.filter { !it.code.equals("en-US", ignoreCase = true) }
                                    .map { it.code to "${it.flag} ${it.code}" }
                            val effectiveMarket = config.getEffectiveLocale()
                            val curIdx = marketOptions.indexOfFirst { it.first.equals(effectiveMarket, ignoreCase = true) }.coerceAtLeast(0)
                            TvSettingSliderRow(
                                title = config.tr("Thị trường trang Xbox Cloud", "Xbox Storefront Market"),
                                subtitle = config.tr(
                                    "Khuyên dùng en-US để tránh lỗi chuyển hướng vùng.",
                                    "Recommended: en-US to prevent unsupported region redirects."
                                ),
                                valueText = marketOptions[curIdx].second,
                                fraction = curIdx.toFloat() / marketOptions.lastIndex.coerceAtLeast(1).toFloat(),
                                onStep = { dir ->
                                    val next = (curIdx + dir + marketOptions.size) % marketOptions.size
                                    val chosen = marketOptions[next].first
                                    if (chosen.equals("en-US", ignoreCase = true)) {
                                        SettingsRepository.resetXboxMarketToDefault()
                                    } else {
                                        SettingsRepository.updateCustomLocale(chosen)
                                    }
                                },
                                scale = scale,
                                testTag = "tv_adv_market_row"
                            )
                        }

                        // 5. Công tắc mạng kỹ thuật
                        item(key = "tv_adv_globalping") {
                            TvSettingToggleRow(
                                title = config.tr("Ước lượng độ trễ qua trạm Globalping", "Estimate Ping via Globalping Probe"),
                                subtitle = config.tr(
                                    "Đo tham khảo từ trạm gần nhất cho máy chủ đang chọn.",
                                    "Reference probe from nearest station for selected server."
                                ),
                                checked = config.enableGlobalpingProbe,
                                onCheckedChange = { SettingsRepository.updateEnableGlobalpingProbe(it) },
                                scale = scale,
                                testTag = "tv_adv_globalping_row"
                            )
                        }

                        item(key = "tv_adv_geoip_https") {
                            TvSettingToggleRow(
                                title = config.tr("Dịch vụ tra IP dự phòng qua HTTPS", "Fallback HTTPS IP Lookup"),
                                subtitle = config.tr(
                                    "Chỉ gọi khi IP chưa có trong cơ sở dữ liệu DB-IP Lite trên máy.",
                                    "Used only when an IP is not in local DB-IP Lite database."
                                ),
                                checked = config.enableGeoIpHttpsFallback,
                                onCheckedChange = { SettingsRepository.updateEnableGeoIpHttpsFallback(it) },
                                scale = scale,
                                testTag = "tv_adv_geoip_https_row"
                            )
                        }

                        item(key = "tv_adv_copy_diag") {
                            TvSettingActionRow(
                                title = config.tr("Sao chép báo cáo chẩn đoán", "Copy Diagnostic Report"),
                                subtitle = config.tr(
                                    "Sao chép thông tin cấu hình & mạng vào bộ nhớ tạm.",
                                    "Copies configuration and network report to clipboard."
                                ),
                                actionLabel = config.tr("Sao chép", "Copy"),
                                scale = scale,
                                onClick = {
                                    val reportText = AppLogger.buildDiagnosticReport(
                                        context = context,
                                        config = config,
                                        vpnState = vpnState
                                    )
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("CloudPlay Diagnostic Report", reportText))
                                        Toast.makeText(
                                            context,
                                            config.tr("Đã sao chép báo cáo chẩn đoán", "Copied diagnostic report"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (_: Exception) {}
                                },
                                testTag = "tv_adv_copy_diag_row"
                            )
                        }

                        item(key = "tv_adv_restore_defaults") {
                            TvSettingActionRow(
                                title = config.tr("Khôi phục mặc định (Mục Nâng cao)", "Restore Defaults (Advanced Only)"),
                                subtitle = config.tr(
                                    "Đặt lại các tùy chỉnh nâng cao, giữ nguyên đăng nhập Xbox và lịch sử chơi.",
                                    "Resets advanced options while keeping Xbox login and play history."
                                ),
                                actionLabel = config.tr("Khôi phục", "Restore"),
                                scale = scale,
                                onClick = { showResetAdvancedConfirmDialog = true },
                                testTag = "tv_adv_restore_defaults_row"
                            )
                        }
                    }

                    TvSettingsCategory.INFO -> {
                        item(key = "tv_info_log") {
                            TvSettingActionRow(
                                title = config.tr("Nhật ký hệ thống", "System Activity Log"),
                                subtitle = config.tr("Theo dõi sự kiện ứng dụng, mạng và trạm đo.", "View app, network, and server probe logs."),
                                actionLabel = config.tr("Mở nhật ký", "Open Log"),
                                scale = scale,
                                onClick = { showAppLogDialog = true },
                                testTag = "tv_info_log_row"
                            )
                        }

                        item(key = "tv_info_update") {
                            val updateSub = when (val st = updateState) {
                                is UpdateCheckState.Checking -> config.tr("Đang kiểm tra bản cập nhật...", "Checking for updates...")
                                is UpdateCheckState.UpToDate -> config.tr("Đã là bản mới nhất (v${st.currentVersion})", "Up to date (v${st.currentVersion})")
                                is UpdateCheckState.UpdateAvailable -> config.tr("Có bản mới ${st.latestVersion} · Bấm A để xem", "Update ${st.latestVersion} available")
                                else -> config.tr("Đang dùng v$currentVersionName · Tự kiểm tra mỗi 24 giờ", "Current v$currentVersionName · Auto-checks every 24h")
                            }
                            TvSettingActionRow(
                                title = config.tr("Kiểm tra bản cập nhật", "Check for Updates"),
                                subtitle = updateSub,
                                actionLabel = config.tr("Kiểm tra", "Check Now"),
                                scale = scale,
                                onClick = { UpdateChecker.checkNowManual(context) },
                                testTag = "tv_info_update_row"
                            )
                        }

                        item(key = "tv_info_auto_update_toggle") {
                            TvSettingToggleRow(
                                title = config.tr("Tự kiểm tra bản mới khi mở app", "Auto-check for Updates on Launch"),
                                subtitle = config.tr("Kiểm tra ngầm tối đa 1 lần mỗi 24 giờ.", "Checks in the background at most once every 24 hours."),
                                checked = config.autoCheckUpdatesOnLaunch,
                                onCheckedChange = { SettingsRepository.updateAutoCheckUpdatesOnLaunch(it) },
                                scale = scale,
                                testTag = "tv_info_auto_update_row"
                            )
                        }

                        item(key = "tv_info_qr") {
                            TvSettingActionRow(
                                title = config.tr("Mã QR và chia sẻ", "QR Code & Share"),
                                subtitle = config.tr("Quét mã QR bằng điện thoại để mở trang tải xuống.", "Scan QR code with your phone to open download page."),
                                actionLabel = config.tr("Hiện mã QR", "Show QR"),
                                scale = scale,
                                onClick = { showWebPortalDialog = true },
                                testTag = "tv_info_qr_row"
                            )
                        }

                        item(key = "tv_info_guide") {
                            TvSettingActionRow(
                                title = config.tr("Hướng dẫn sử dụng", "User Guide"),
                                subtitle = config.tr("Cách dùng VPN mở khóa vùng, tay cầm và menu nổi.", "How to use region unlock VPN, controller, and quick menu."),
                                actionLabel = config.tr("Xem hướng dẫn", "Open Guide"),
                                scale = scale,
                                onClick = { onOpenGuide(GuideSection.QUICK_START) },
                                testTag = "tv_info_guide_row"
                            )
                        }

                        item(key = "tv_info_terms") {
                            TvSettingActionRow(
                                title = config.tr("Điều khoản và quyền riêng tư", "Terms & Privacy Policy"),
                                subtitle = config.tr("Đọc đầy đủ 15 mục điều khoản ngay trong ứng dụng.", "Read all 15 sections in-app."),
                                actionLabel = config.tr("Xem 15 mục", "Read Terms"),
                                scale = scale,
                                onClick = { showFullTermsDialog = true },
                                testTag = "tv_info_terms_row"
                            )
                        }

                        item(key = "tv_info_clear_cookies") {
                            TvSettingActionRow(
                                title = config.tr("Xóa phiên và đăng nhập lại Xbox", "Sign Out & Clear Xbox Session"),
                                subtitle = config.tr("Xóa cookie đăng nhập WebView để đổi tài khoản Microsoft/Xbox.", "Clear saved WebView cookies to switch Microsoft/Xbox account."),
                                actionLabel = config.tr("Xóa phiên", "Sign Out"),
                                titleColor = TvDanger,
                                scale = scale,
                                onClick = { showClearCookiesConfirmDialog = true },
                                testTag = "tv_info_clear_cookies_row"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWebPortalDialog) {
        WebDownloadPortalDialog(onDismiss = { showWebPortalDialog = false })
    }

    if (showAppLogDialog) {
        AppLogDialog(isEn = isEn, onDismiss = { showAppLogDialog = false })
    }

    if (showFullTermsDialog) {
        FullTermsInAppDialog(
            isEn = config.isEnglishUI(),
            onOpenExternalBrowser = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://muitenvn.github.io/CloudPlay-App/terms.html")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            onDismiss = { showFullTermsDialog = false }
        )
    }

    if (showResetAdvancedConfirmDialog) {
        TvConfirmDialog(
            title = config.tr("Khôi phục mặc định mục Nâng cao?", "Restore Advanced Defaults?"),
            description = config.tr(
                "Chỉ đặt lại các tùy chỉnh trong mục Nâng cao (giao diện về Tự động, màu sắc hình ảnh, vùng chết cần, đổi nút tay cầm, thị trường Xbox về en-US). Phiên đăng nhập Xbox và lịch sử chơi được giữ nguyên.",
                "Resets only Advanced settings (interface mode to Auto, image colors, stick deadzones, button remaps, Xbox market to en-US). Your Xbox login and play history are preserved."
            ),
            confirmText = config.tr("Khôi phục", "Restore"),
            cancelText = config.tr("Hủy", "Cancel"),
            confirmVariant = TvButtonVariant.PRIMARY,
            onConfirm = {
                SettingsRepository.resetAdvancedSettingsOnly()
                showResetAdvancedConfirmDialog = false
                Toast.makeText(
                    context,
                    config.tr("Đã khôi phục mặc định mục Nâng cao", "Restored Advanced settings to default"),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showResetAdvancedConfirmDialog = false },
            testTagPrefix = "tv_reset_advanced_dialog"
        )
    }

    if (showResetGamepadConfirmDialog) {
        TvConfirmDialog(
            title = config.tr("Đặt lại bố cục tay cầm?", "Reset Gamepad Layout?"),
            description = config.tr(
                "Khôi phục toàn bộ vị trí và kích thước nút tay cầm ảo về mặc định?",
                "Restore all virtual gamepad button positions and sizes to default?"
            ),
            confirmText = config.tr("Đặt lại", "Reset"),
            cancelText = config.tr("Hủy", "Cancel"),
            confirmVariant = TvButtonVariant.DANGER,
            onConfirm = {
                SettingsRepository.clearAllPerGameLayouts()
                SettingsRepository.resetGamepadCustomOffsets()
                showResetGamepadConfirmDialog = false
                Toast.makeText(
                    context,
                    config.tr("Đã đặt lại bố cục tay cầm mặc định", "Reset gamepad layout to default"),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showResetGamepadConfirmDialog = false },
            testTagPrefix = "tv_reset_gamepad_dialog"
        )
    }

    if (showClearCookiesConfirmDialog) {
        TvConfirmDialog(
            title = config.tr("Xóa phiên đăng nhập Xbox?", "Clear Xbox Login Session?"),
            description = config.tr(
                "Toàn bộ dữ liệu đăng nhập WebView hiện tại sẽ được làm mới để bạn đăng nhập lại tài khoản Microsoft/Xbox.",
                "All saved WebView cookies and login data will be cleared so you can sign in again."
            ),
            confirmText = config.tr("Xóa phiên", "Sign Out"),
            cancelText = config.tr("Hủy", "Cancel"),
            confirmVariant = TvButtonVariant.DANGER,
            onConfirm = {
                showClearCookiesConfirmDialog = false
                onClearCookies()
            },
            onDismiss = { showClearCookiesConfirmDialog = false },
            testTagPrefix = "tv_clear_cookies_dialog"
        )
    }

    if (activeRemapSourceBtn != null) {
        val srcIdx = activeRemapSourceBtn ?: 0
        val srcName = SettingsRepository.getGamepadButtonLabel(srcIdx, isEn)
        val currentMap = SettingsRepository.parsePhysicalButtonMap(config.physicalGamepadButtonMap)
        val currentDest = currentMap[srcIdx] ?: srcIdx
        val closeRemapFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { activeRemapSourceBtn = null },
            modifier = Modifier.gamepadControllerScope(
                enabled = true,
                defaultFocusRequester = closeRemapFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = { activeRemapSourceBtn = null }
            ),
            containerColor = TvSurface2,
            shape = RoundedCornerShape(22.dp),
            titleContentColor = TvInk,
            textContentColor = TvMuted,
            title = {
                Text(
                    text = config.tr("Đổi nút tay cầm thật: $srcName", "Remap Physical Button: $srcName"),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = config.tr(
                            "Chọn tín hiệu nút Xbox sẽ gửi vào game khi bạn bấm nút $srcName:",
                            "Select which Xbox button signal is sent when pressing $srcName:"
                        ),
                        fontSize = 14.sp
                    )
                    SettingsRepository.STANDARD_GAMEPAD_BUTTONS.chunked(3).forEach { rowBtns ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBtns.forEach { (dstIdx, dstShort, _) ->
                                val selected = dstIdx == currentDest
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .gamepadClickable(cornerRadius = 10.dp) {
                                            SettingsRepository.updatePhysicalGamepadSingleButtonMap(srcIdx, dstIdx)
                                            activeRemapSourceBtn = null
                                        },
                                    color = if (selected) TvSel else TvSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) TvAccent else TvLine
                                    )
                                ) {
                                    Text(
                                        text = dstShort,
                                        color = if (selected) TvAccent else TvInk,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    )
                                }
                            }
                            repeat(3 - rowBtns.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { activeRemapSourceBtn = null },
                    modifier = Modifier
                        .focusRequester(closeRemapFocusRequester)
                        .gamepadActionButton(cornerRadius = 10.dp) { activeRemapSourceBtn = null }
                ) {
                    Text(text = config.tr("Đóng", "Close"), color = TvAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (activeShortcutTargetAction != null) {
        val actionKey = activeShortcutTargetAction ?: ""
        val selectableButtons = remember {
            SettingsRepository.STANDARD_GAMEPAD_BUTTONS.filter { it.first != 8 && it.first != 16 }
        }
        val closeShortcutFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { activeShortcutTargetAction = null },
            modifier = Modifier.gamepadControllerScope(
                enabled = true,
                defaultFocusRequester = closeShortcutFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = { activeShortcutTargetAction = null }
            ),
            containerColor = TvSurface2,
            shape = RoundedCornerShape(22.dp),
            titleContentColor = TvInk,
            textContentColor = TvMuted,
            title = {
                Text(
                    text = config.tr("Chọn nút kích hoạt (Giữ View + nút)", "Select Trigger Button (View + Button)"),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableButtons.chunked(3).forEach { rowBtns ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBtns.forEach { (btnIdx, btnShort, _) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .gamepadClickable(cornerRadius = 10.dp) {
                                            when (actionKey) {
                                                "toggle_stats" -> SettingsRepository.updateShortcutToggleStatsButton(btnIdx)
                                                "toggle_virtual_pad" -> SettingsRepository.updateShortcutToggleVirtualPadButton(btnIdx)
                                                "toggle_recording" -> SettingsRepository.updateShortcutToggleRecordingButton(btnIdx)
                                            }
                                            activeShortcutTargetAction = null
                                        },
                                    color = TvSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TvCyan)
                                ) {
                                    Text(
                                        text = "View + $btnShort",
                                        color = TvCyan,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    )
                                }
                            }
                            repeat(3 - rowBtns.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { activeShortcutTargetAction = null },
                    modifier = Modifier
                        .focusRequester(closeShortcutFocusRequester)
                        .gamepadActionButton(cornerRadius = 10.dp) { activeShortcutTargetAction = null }
                ) {
                    Text(text = config.tr("Đóng", "Close"), color = TvAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TvSettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    scale: Float,
    testTag: String
) {
    val corner = (12.dp * scale).coerceAtLeast(9.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (48.dp * scale).coerceAtLeast(38.dp))
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .gamepadClickable(
                cornerRadius = corner,
                onStepHorizontal = { dir ->
                    val wantOn = dir > 0
                    if (wantOn != checked) onCheckedChange(wantOn)
                },
                onClick = { onCheckedChange(!checked) }
            )
            .padding(
                horizontal = (14.dp * scale).coerceAtLeast(10.dp),
                vertical = (8.dp * scale).coerceAtLeast(6.dp)
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
        ) {
            Text(
                text = title,
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = TvMuted,
                    fontSize = (11f * scale).coerceAtLeast(9.5f).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))
        TvSwitch(checked = checked, scale = scale)
    }
}

@Composable
private fun TvSettingSegmentRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    scale: Float,
    testTag: String
) {
    val corner = (12.dp * scale).coerceAtLeast(9.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (48.dp * scale).coerceAtLeast(38.dp))
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .gamepadClickable(
                cornerRadius = corner,
                onStepHorizontal = { dir ->
                    val next = (selectedIndex + dir).coerceIn(0, options.lastIndex)
                    if (next != selectedIndex) onSelect(next)
                },
                onClick = {
                    val next = (selectedIndex + 1) % options.size
                    onSelect(next)
                }
            )
            .padding(
                horizontal = (14.dp * scale).coerceAtLeast(10.dp),
                vertical = (8.dp * scale).coerceAtLeast(6.dp)
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
        ) {
            Text(
                text = title,
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = TvMuted,
                    fontSize = (11f * scale).coerceAtLeast(9.5f).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))
        TvSegmentedControl(
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            scale = scale,
            containerColor = TvSurface2
        )
    }
}

@Composable
private fun TvSettingSliderRow(
    title: String,
    subtitle: String,
    valueText: String,
    fraction: Float,
    onStep: (Int) -> Unit,
    scale: Float,
    testTag: String
) {
    val corner = (12.dp * scale).coerceAtLeast(9.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (48.dp * scale).coerceAtLeast(38.dp))
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .gamepadClickable(
                cornerRadius = corner,
                onStepHorizontal = { dir -> onStep(dir) },
                onClick = { onStep(1) }
            )
            .padding(
                horizontal = (14.dp * scale).coerceAtLeast(10.dp),
                vertical = (8.dp * scale).coerceAtLeast(6.dp)
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
        ) {
            Text(
                text = title,
                color = TvInk,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = TvMuted,
                    fontSize = (11f * scale).coerceAtLeast(9.5f).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .width((96.dp * scale).coerceIn(68.dp, 130.dp))
                    .height((8.dp * scale).coerceAtLeast(6.dp))
                    .clip(CircleShape)
                    .background(Color(0xFF233029))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                        .clip(CircleShape)
                        .background(TvAccent)
                )
            }
            Text(
                text = valueText,
                color = TvInk,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = (12.5f * scale).coerceAtLeast(10.5f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TvSettingActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    scale: Float,
    titleColor: Color = TvInk,
    onClick: () -> Unit,
    testTag: String
) {
    val corner = (12.dp * scale).coerceAtLeast(9.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = (48.dp * scale).coerceAtLeast(38.dp))
            .clip(RoundedCornerShape(corner))
            .background(TvSurface)
            .gamepadClickable(
                cornerRadius = corner,
                onClick = onClick
            )
            .padding(
                horizontal = (14.dp * scale).coerceAtLeast(10.dp),
                vertical = (8.dp * scale).coerceAtLeast(6.dp)
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2.dp * scale).coerceAtLeast(1.dp))
        ) {
            Text(
                text = title,
                color = titleColor,
                fontFamily = SpaceGroteskFontFamily,
                fontSize = (14f * scale).coerceAtLeast(11.5f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = TvMuted,
                    fontSize = (11f * scale).coerceAtLeast(9.5f).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width((10.dp * scale).coerceAtLeast(6.dp)))
        TvPill(
            text = actionLabel,
            scale = scale * 0.92f,
            tone = if (titleColor == TvDanger) TvPillTone.WARN else TvPillTone.GREEN
        )
    }
}
