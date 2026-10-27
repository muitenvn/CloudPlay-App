package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppConfig
import com.example.data.AppLanguage
import com.example.data.CellularOptimizationMode
import com.example.data.GamepadHapticLevel
import com.example.data.GamepadLayoutPreset
import com.example.data.POPULAR_XBOX_LANGUAGES
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine
import com.example.vpn.VpnManager

@Composable
fun SettingsScreen(
    config: AppConfig,
    vpnState: VpnConnectionState,
    onOpenGuide: (com.example.ui.components.GuideSection) -> Unit = {},
    onClearCookies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customInputText by remember(config.customLocale) {
        mutableStateOf(if (config.useSystemLanguage) "xbox.com/en-US" else "xbox.com/${config.customLocale}")
    }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showAppLogDialog by remember { mutableStateOf(false) }

    // 5 nhóm gập/mở (mặc định gập, chỉ nhóm đồ họa chính mở)
    var expandGraphics by rememberSaveable { mutableStateOf(true) }
    var expandNetwork by rememberSaveable { mutableStateOf(false) }
    var expandControls by rememberSaveable { mutableStateOf(false) }
    var expandAdvanced by rememberSaveable { mutableStateOf(false) }
    var expandRecording by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val resolvedSystemLocale = remember { SettingsRepository.resolveSystemXboxLocale() }
    val isEn = config.isEnglishUI()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // =========================================================================
        // NHÓM 1: ĐỒ HỌA & KHUNG HÌNH (FPS) (Mặc định mở)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Resolution, Graphics & FPS" else "Độ phân giải, Đồ họa & FPS",
                subtitle = "${config.cellularMode.localizedTitle(isEn)} • ${config.streamFpsCap.fps} FPS",
                icon = Icons.Default.Speed,
                expanded = expandGraphics,
                onToggle = { expandGraphics = !expandGraphics },
                testTag = "settings_group_graphics"
            ) {
                // 1.1 Độ phân giải & Đồ họa (gộp 1) + Nhịp FPS
                SettingsCard(title = if (isEn) "Resolution & Graphics (4G/5G/Wi-Fi Low Lag)" else "Độ phân giải & Đồ họa (Giảm trễ 4G/5G/Wi-Fi)") {
                    val selectedMode = config.cellularMode
                    val isAutoSelected = selectedMode == CellularOptimizationMode.AUTO

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.2.dp,
                                if (isAutoSelected) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { SettingsRepository.updateCellularMode(CellularOptimizationMode.AUTO) }
                            .testTag("settings_graphics_mode_auto"),
                        color = if (isAutoSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isAutoSelected) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isEn) "Auto • 720p ⇄ 1080p (Recommended)" else "Tự động • 720p ⇄ 1080p (Khuyên dùng)",
                                        color = if (isAutoSelected) XboxNeonGreen else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isEn) "Auto-lowers resolution to cut lag on 4G, 5G & Wi-Fi" else "Tự hạ độ phân giải để giảm độ trễ trên cả 4G, 5G và Wi-Fi",
                                        color = TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                            if (isAutoSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    val manualModes = listOf(
                        CellularOptimizationMode.DATA_SAVER_HIGH_SPEED,
                        CellularOptimizationMode.MEDIUM,
                        CellularOptimizationMode.HIGH,
                        CellularOptimizationMode.MAX_SPEED_UNCAPPED
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        manualModes.chunked(2).forEach { rowModes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowModes.forEach { mode ->
                                    val isSelected = selectedMode == mode
                                    val isLowOrMed = mode == CellularOptimizationMode.DATA_SAVER_HIGH_SPEED ||
                                        mode == CellularOptimizationMode.MEDIUM
                                    val accent = if (isLowOrMed) XboxNeonGreen else DirectCyan

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                1.dp,
                                                if (isSelected) accent else XboxCardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { SettingsRepository.updateCellularMode(mode) }
                                            .testTag("settings_graphics_mode_${mode.id}"),
                                        color = if (isSelected) {
                                            if (isLowOrMed) Color(0xFF132219) else Color(0xFF0F2327)
                                        } else {
                                            Color(0xFF0F1813)
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = mode.localizedTitle(isEn),
                                                    color = if (isSelected) accent else TextPrimary,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = accent,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = mode.localizedSubtitle(isEn),
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isEn) "Frame Rate (FPS)" else "Nhịp khung hình (FPS)",
                        color = XboxNeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.data.StreamFpsCap.entries.forEach { fpsCap ->
                            val isSelected = config.streamFpsCap == fpsCap
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateStreamFpsCap(fpsCap) }
                                    .testTag("settings_fps_cap_${fpsCap.fps}"),
                                color = if (isSelected) Color(0xFF13241A) else Color(0xFF0F1813)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${fpsCap.fps} FPS",
                                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = fpsCap.localizedDesc(isEn),
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                    SettingToggleRow(
                        title = if (isEn) "Low-End & Battery Saver Mode" else "Chế độ máy yếu & Tiết kiệm pin",
                        subtitle = if (isEn) "Light 720p stream & reduced effects to keep phone cool" else "Dùng mức 720p nhẹ và tắt hiệu ứng thừa giúp máy mát, đỡ hao pin",
                        checked = config.lowEndDeviceMode && config.gpuLoadReductionEnabled,
                        onCheckedChange = { enabled ->
                            SettingsRepository.updateLowEndDeviceMode(enabled)
                            SettingsRepository.updateGpuLoadReductionEnabled(enabled)
                        },
                        switchTestTag = "settings_low_end_device_mode_switch"
                    )
                }

                // 1.2 Độ nét & Màu sắc hình ảnh
                SettingsCard(title = if (isEn) "Sharpness & Image Colors" else "Độ nét & Màu sắc hình ảnh") {
                    SettingToggleRow(
                        title = if (isEn) "Sharpen Image" else "Tăng độ nét hình ảnh",
                        subtitle = if (isEn) "Makes characters and scenery clearer" else "Làm rõ chi tiết nhân vật và khung cảnh",
                        checked = config.clarityBoostEnabled,
                        onCheckedChange = { SettingsRepository.updateClarityBoost(it) }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LabeledSlider(
                            label = if (isEn) "Contrast" else "Độ tương phản",
                            valueText = "${config.videoContrast}%",
                            value = config.videoContrast.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoContrast(it.toInt()) },
                            valueRange = 80f..140f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Color Saturation" else "Độ tươi màu",
                            valueText = "${config.videoSaturation}%",
                            value = config.videoSaturation.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoSaturation(it.toInt()) },
                            valueRange = 80f..150f,
                            accentColor = DirectCyan
                        )
                        LabeledSlider(
                            label = if (isEn) "Brightness" else "Độ sáng",
                            valueText = "${config.videoBrightness}%",
                            value = config.videoBrightness.toFloat(),
                            onValueChange = { SettingsRepository.updateVideoBrightness(it.toInt()) },
                            valueRange = 80f..130f,
                            accentColor = XboxNeonGreen
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            SettingsRepository.resetVideoDisplayDefaults()
                            Toast.makeText(
                                context,
                                if (isEn) "Reset colors to default" else "Đã đặt lại màu sắc mặc định",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advanced_reset_video_defaults_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEn) "Reset Colors to Default" else "Đặt lại màu sắc mặc định",
                            color = DirectCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 2: MẠNG & MỞ KHÓA VÙNG (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Network & Region Unlock" else "Mạng & Mở khóa vùng",
                subtitle = if (isEn) "Smart VPN, Low Ping" else "VPN thông minh, vào game mượt",
                icon = Icons.Default.Public,
                expanded = expandNetwork,
                onToggle = { expandNetwork = !expandNetwork },
                testTag = "settings_group_network"
            ) {
                // 2.1 VPN thông minh
                SettingsCard(title = if (isEn) "Smart VPN" else "VPN thông minh") {
                    SettingToggleRow(
                        title = if (isEn) "Smart VPN (Auto Unlock ➔ Auto Direct)" else "VPN thông minh (Tự mở khóa ➔ Tự tắt khi vào trận)",
                        subtitle = if (isEn) "Auto connects to open game, then switches to direct network for lowest ping" else "Tự bật khi mở game, tự chuyển sang mạng trực tiếp khi vào trận để ping thấp nhất",
                        checked = config.smartVpnEnabled && config.autoBypassEnabled,
                        onCheckedChange = { enabled ->
                            SettingsRepository.updateSmartVpnEnabled(enabled)
                            SettingsRepository.updateAutoBypass(enabled)
                        },
                        switchTestTag = "settings_switch_smart_vpn"
                    )

                    if (config.autoBypassEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isEn) "VPN disconnect delay" else "Thời gian chờ tắt VPN",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isEn) "${config.autoBypassDelaySeconds}s" else "${config.autoBypassDelaySeconds} giây",
                                    color = DirectCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Slider(
                                value = config.autoBypassDelaySeconds.toFloat(),
                                onValueChange = { SettingsRepository.updateAutoBypassDelay(it.toInt()) },
                                valueRange = 1f..8f,
                                steps = 6,
                                colors = SliderDefaults.colors(
                                    thumbColor = DirectCyan,
                                    activeTrackColor = DirectCyan,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }

                // 2.2 Chế độ mở khóa vùng
                SettingsCard(title = if (isEn) "Region Unlock Mode" else "Chế độ mở khóa vùng") {
                    listOf(
                        Triple(
                            VpnEngine.SMART_BYPASS,
                            if (isEn) "Smart Unlock 🇯🇵 (Recommended)" else "Mở khóa thông minh 🇯🇵 (Khuyên dùng)",
                            if (isEn) "Direct unlock without extra ping" else "Vào thẳng nhanh, không làm tăng ping"
                        ),
                        Triple(
                            VpnEngine.PROXY_TUNNEL,
                            if (isEn) "Intermediate Server 🌐" else "Máy chủ trung gian 🌐",
                            if (isEn) "Connects via JP/KR/US servers" else "Kết nối qua máy chủ Nhật / Hàn / Mỹ"
                        ),
                        Triple(
                            VpnEngine.EXTERNAL_VPN,
                            if (isEn) "External VPN App" else "Ứng dụng VPN ngoài",
                            if (isEn) "Use with your own VPN app" else "Dùng kèm app VPN riêng trên máy"
                        )
                    ).forEach { (engine, title, desc) ->
                        val isSelected = vpnState.engine == engine
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) XboxNeonGreen else XboxCardBorder, RoundedCornerShape(10.dp))
                                .clickable { VpnManager.setEngine(engine) },
                            color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(text = desc, color = TextSecondary, fontSize = 10.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 3: TAY CẦM & CẢM ỨNG (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Gamepad & Touch Controls" else "Tay cầm & Cảm ứng",
                subtitle = if (config.showVirtualController) {
                    if (isEn) "Virtual Gamepad: ON" else "Tay cầm ảo: Đang bật"
                } else {
                    if (isEn) "Virtual Gamepad: OFF" else "Tay cầm ảo: Đang tắt"
                },
                icon = Icons.Default.Gamepad,
                expanded = expandControls,
                onToggle = { expandControls = !expandControls },
                testTag = "settings_group_controls"
            ) {
                SettingsCard(title = if (isEn) "Touch Gamepad" else "Tay cầm ảo trên màn hình") {
                    SettingToggleRow(
                        title = if (isEn) "Show Virtual Gamepad" else "Bật tay cầm ảo",
                        subtitle = if (isEn) "On-screen buttons with custom drag layout" else "Hiện nút bấm ảo, kéo thả vị trí tự do",
                        checked = config.showVirtualController,
                        onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                        switchTestTag = "settings_switch_virtual_controller"
                    )

                    Text(
                        text = if (isEn) "Button Layout" else "Bố cục nút bấm",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GamepadLayoutPreset.entries.forEach { preset ->
                            val isSelected = config.gamepadLayoutPreset == preset
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateGamepadLayoutPreset(preset) },
                                color = if (isSelected) XboxSurfaceVariant else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.localizedTitle(isEn),
                                            color = if (isSelected) XboxNeonGreen else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = preset.localizedDesc(isEn),
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LabeledSlider(
                            label = if (isEn) "Button Size" else "Kích thước nút",
                            valueText = "${(config.gamepadScale * 100).toInt()}%",
                            value = config.gamepadScale,
                            onValueChange = { SettingsRepository.updateGamepadScale(it) },
                            valueRange = 0.75f..1.35f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Opacity" else "Độ đậm mờ",
                            valueText = "${(config.gamepadOpacity * 100).toInt()}%",
                            value = config.gamepadOpacity,
                            onValueChange = { SettingsRepository.updateGamepadOpacity(it) },
                            valueRange = 0.25f..0.95f,
                            accentColor = DirectCyan
                        )
                        LabeledSlider(
                            label = if (isEn) "Stick Sensitivity" else "Độ nhạy cần xoay",
                            valueText = "${(config.gamepadSensitivity * 100).toInt()}%",
                            value = config.gamepadSensitivity,
                            onValueChange = { SettingsRepository.updateGamepadSensitivity(it) },
                            valueRange = 0.60f..1.60f,
                            accentColor = XboxNeonGreen
                        )
                        LabeledSlider(
                            label = if (isEn) "Center Stick Response" else "Độ trễ tâm cần xoay",
                            valueText = "${(config.gamepadDeadzone * 100).toInt()}%",
                            value = config.gamepadDeadzone,
                            onValueChange = { SettingsRepository.updateGamepadDeadzone(it) },
                            valueRange = 0.02f..0.25f,
                            accentColor = DirectCyan
                        )
                    }

                    Text(
                        text = if (isEn) "Vibration" else "Độ rung khi bấm",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GamepadHapticLevel.entries.forEach { level ->
                            val selected = config.gamepadHapticLevel == level
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { SettingsRepository.updateGamepadHapticLevel(level) },
                                color = if (selected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF0E1813),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) XboxNeonGreen else XboxCardBorder
                                )
                            ) {
                                Text(
                                    text = level.localizedTitle(isEn),
                                    color = if (selected) XboxNeonGreen else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    SettingToggleRow(
                        title = if (isEn) "🎯 Tilt to Aim (Gyro)" else "🎯 Nghiêng máy để ngắm",
                        subtitle = if (isEn) "Tilt phone to adjust aim" else "Nghiêng điện thoại để chỉnh tâm ngắm",
                        checked = config.gamepadGyroEnabled,
                        onCheckedChange = { SettingsRepository.updateGamepadGyroEnabled(it) },
                        activeColor = DirectCyan
                    )

                    if (config.gamepadGyroEnabled) {
                        LabeledSlider(
                            label = if (isEn) "Tilt Sensitivity" else "Độ nhạy nghiêng máy",
                            valueText = "${(config.gamepadGyroSensitivity * 100).toInt()}%",
                            value = config.gamepadGyroSensitivity,
                            onValueChange = { SettingsRepository.updateGamepadGyroSensitivity(it) },
                            valueRange = 0.40f..2.50f,
                            accentColor = DirectCyan
                        )
                    }

                    SettingToggleRow(
                        title = if (isEn) "🏃 Auto-Sprint" else "🏃 Đẩy kịch cần trái để chạy nhanh",
                        subtitle = if (isEn) "Push left stick forward to sprint" else "Tự chạy nhanh khi đẩy cần hết cỡ",
                        checked = config.gamepadSprintLockEnabled,
                        onCheckedChange = { SettingsRepository.updateGamepadSprintLockEnabled(it) }
                    )

                    if (config.gamepadCustomOffsets.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                SettingsRepository.resetGamepadCustomOffsets()
                                Toast.makeText(
                                    context,
                                    if (isEn) "Reset button positions" else "Đã đặt lại vị trí nút",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isEn) "Reset Button Positions" else "Đặt lại vị trí nút mặc định",
                                color = DirectCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 4: DỊCH TIẾNG VIỆT & HỆ THỐNG (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Translation, Language & System" else "Dịch Tiếng Việt & Hệ thống",
                subtitle = if (isEn) "Translator, App Language, Account" else "Bộ dịch, Ngôn ngữ & Tài khoản",
                icon = Icons.Default.Tune,
                expanded = expandAdvanced,
                onToggle = { expandAdvanced = !expandAdvanced },
                testTag = "settings_group_advanced"
            ) {
                // 4.1 Bộ dịch & hiển thị
                SettingsCard(title = if (isEn) "Vietnamese Translator" else "Bộ dịch Tiếng Việt") {
                    SettingToggleRow(
                        title = if (isEn) "Auto-translate to Vietnamese" else "Tự động dịch Tiếng Việt",
                        subtitle = if (isEn) "Translates Xbox menus and notifications" else "Dịch giao diện và thông báo trên trang Xbox",
                        checked = config.xcloudTranslateEnabled,
                        onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                        switchTestTag = "settings_switch_xcloud_translate"
                    )

                    SettingToggleRow(
                        title = if (isEn) "Bilingual mode (VI - EN)" else "Chế độ song ngữ (Việt - Anh)",
                        subtitle = if (isEn) "Shows original English in parentheses" else "Hiện kèm tiếng Anh gốc trong ngoặc",
                        checked = config.bilingualMode,
                        onCheckedChange = { SettingsRepository.updateBilingualMode(it) }
                    )
                }

                // 4.2 Ngôn ngữ ứng dụng (Gộp 1 hàng 3 nút gọn gàng)
                SettingsCard(title = if (isEn) "App Language" else "Ngôn ngữ ứng dụng") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(AppLanguage.SYSTEM, if (isEn) "🌐 System" else "🌐 Hệ thống", "app_lang_system_button"),
                            Triple(AppLanguage.VIETNAMESE, "🇻🇳 Tiếng Việt", "app_lang_vi_button"),
                            Triple(AppLanguage.ENGLISH, "🇺🇸 English", "app_lang_en_button")
                        ).forEach { (lang, label, tag) ->
                            val isSelected = config.appLanguage == lang
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateAppLanguage(lang) }
                                    .testTag(tag),
                                color = if (isSelected) XboxNeonGreen.copy(alpha = 0.14f) else XboxSurfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) XboxNeonGreen else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 4.3 Ngôn ngữ trang Xbox (chỉ giữ danh sách chọn nhanh 1 chạm, ẩn ô gõ URL thủ công)
                SettingsCard(title = if (isEn) "Xbox Page Language" else "Ngôn ngữ trang Xbox") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (config.useSystemLanguage) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { SettingsRepository.updateUseSystemLanguage(true) },
                        color = if (config.useSystemLanguage) XboxSurfaceVariant else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Auto Default (English - US)" else "Mặc định (Tiếng Anh - Mỹ)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isEn) "Keeps Xbox menus in English instead of Japanese" else "Giữ trang Xbox hiển thị tiếng Anh dễ đọc",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (config.useSystemLanguage) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        POPULAR_XBOX_LANGUAGES.take(5).forEach { lang ->
                            val isSelected = !config.useSystemLanguage && config.customLocale.equals(lang.code, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { SettingsRepository.updateCustomLocale(lang.code) },
                                color = if (isSelected) XboxSurfaceVariant else Color.Black.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = lang.flag, fontSize = 15.sp)
                                        Text(
                                            text = lang.localizedName(isEn),
                                            color = TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 4.4 Tài khoản & Bản quyền
                SettingsCard(title = if (isEn) "Account & Terms" else "Tài khoản & Điều khoản") {
                    Button(
                        onClick = onClearCookies,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "Sign Out & Clear Login Session" else "Xóa phiên & Đăng nhập lại Xbox",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "Terms of Use & Risk Notice" else "Điều khoản sử dụng & Lưu ý rủi ro",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { showAppLogDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_app_log_advanced_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isEn) "System Activity Log (Track Probe & App)" else "Nhật ký hệ thống (Log theo dõi App & Trạm đo)",
                                color = XboxNeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // NHÓM 5: QUAY MÀN HÌNH (TÙY CHỌN PHỤ) (Mặc định gập)
        // =========================================================================
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Screen Recording" else "Quay màn hình",
                subtitle = config.screenRecordQuality.localizedShort(isEn),
                icon = Icons.Default.Videocam,
                expanded = expandRecording,
                onToggle = { expandRecording = !expandRecording },
                testTag = "settings_group_recording"
            ) {
                SettingsCard(title = if (isEn) "Video Quality (.MP4)" else "Độ nét video (.MP4)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.data.ScreenRecordQuality.entries.forEach { recQuality ->
                            val isSelected = config.screenRecordQuality == recQuality
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) DirectCyan else XboxCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { SettingsRepository.updateScreenRecordQuality(recQuality) }
                                    .testTag("settings_record_quality_${recQuality.id}"),
                                color = if (isSelected) Color(0xFF0F2327) else Color(0xFF0F1813)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = recQuality.localizedTitle(isEn),
                                        color = if (isSelected) DirectCyan else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = recQuality.localizedDesc(isEn),
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isEn) {
                            "⚡ Hardware VBR encoding & background load reduction run automatically while recording."
                        } else {
                            "⚡ Tự động mã hóa phần cứng siêu nhẹ (VBR) & tắt hiệu ứng nền khi đang quay để chống nóng máy."
                        },
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        // =========================================================================
        // NHÓM 5.5: NHẠC NỀN (Tự phát, Phát thủ công & Phát khi đang chơi)
        // =========================================================================
        item {
            val isMusicPlaying by com.example.audio.BackgroundMusicManager.isPlaying.collectAsState()

            SettingsCard(title = if (isEn) "Background Music" else "Nhạc nền") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = if (isEn) "Manual Playback" else "Phát / Dừng nhạc nền",
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isMusicPlaying) {
                                if (isEn) "Currently playing (${(config.backgroundMusicVolume * 100).toInt()}%)" else "Đang phát (${(config.backgroundMusicVolume * 100).toInt()}%)"
                            } else {
                                if (isEn) "Stopped • Tap to play manually" else "Đang dừng • Bấm để bật tay khi cần"
                            },
                            color = if (isMusicPlaying) XboxNeonGreen else TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (config.lowEndDeviceMode && !isMusicPlaying) {
                                SettingsRepository.updateLowEndDeviceMode(false)
                                SettingsRepository.updateGpuLoadReductionEnabled(false)
                            }
                            com.example.audio.BackgroundMusicManager.toggleManualPlayback()
                        },
                        modifier = Modifier.testTag("settings_background_music_manual_play_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMusicPlaying) DirectCyan else XboxNeonGreen.copy(alpha = 0.7f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isMusicPlaying) DirectCyan.copy(alpha = 0.14f) else XboxNeonGreen.copy(alpha = 0.12f)
                        )
                    ) {
                        Text(
                            text = if (isMusicPlaying) {
                                if (isEn) "⏹ Stop" else "⏹ Dừng"
                            } else {
                                if (isEn) "▶ Play" else "▶ Phát"
                            },
                            color = if (isMusicPlaying) DirectCyan else XboxNeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                }

                SettingToggleRow(
                    title = if (isEn) "Auto-start music on app launch" else "Tự động bật nhạc khi mở app",
                    subtitle = if (config.lowEndDeviceMode) {
                        if (isEn) {
                            "Auto-muted while Low-End & Battery Saver Mode is active"
                        } else {
                            "Đang tự tắt do bật Chế độ máy yếu"
                        }
                    } else {
                        if (isEn) {
                            "Default OFF • Plays automatically in Home, Games, Servers & Settings"
                        } else {
                            "Mặc định tắt • Tự phát khi mở ứng dụng ở Trang chủ, Game, Máy chủ, Cài đặt"
                        }
                    },
                    checked = config.backgroundMusicEnabled && !config.lowEndDeviceMode,
                    onCheckedChange = { enabled ->
                        if (config.lowEndDeviceMode && enabled) {
                            SettingsRepository.updateLowEndDeviceMode(false)
                            SettingsRepository.updateGpuLoadReductionEnabled(false)
                        }
                        SettingsRepository.updateBackgroundMusicEnabled(enabled)
                    },
                    activeColor = XboxNeonGreen,
                    switchTestTag = "settings_background_music_switch"
                )

                SettingToggleRow(
                    title = if (isEn) "Play music while in game" else "Phát nhạc khi đang chơi",
                    subtitle = if (config.lowEndDeviceMode) {
                        if (isEn) {
                            "Auto-stops when entering game in Low-End Mode"
                        } else {
                            "Chế độ máy yếu: vẫn tự tắt nhạc khi vào game"
                        }
                    } else {
                        if (isEn) {
                            "Keeps playing at the same volume during gameplay if music is already on"
                        } else {
                            "Nếu nhạc đang phát sẽ tiếp tục phát cùng âm lượng khi vào game; tắt sẽ dừng (fade 800ms)"
                        }
                    },
                    checked = config.backgroundMusicPlayInGame && !config.lowEndDeviceMode,
                    onCheckedChange = { enabled ->
                        SettingsRepository.updateBackgroundMusicPlayInGame(enabled)
                    },
                    activeColor = DirectCyan,
                    switchTestTag = "settings_background_music_play_in_game_switch"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LabeledSlider(
                        label = if (isEn) "Background Music Volume" else "Âm lượng nhạc nền",
                        valueText = "${(config.backgroundMusicVolume * 100).toInt()}%",
                        value = config.backgroundMusicVolume,
                        onValueChange = { SettingsRepository.updateBackgroundMusicVolume(it) },
                        valueRange = 0f..1f,
                        accentColor = XboxNeonGreen
                    )
                }

                Text(
                    text = if (isEn) {
                        "Excluded from screen recording"
                    } else {
                        "Không thu vào video quay màn hình"
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("settings_music_capture_notice_text")
                )
            }
        }

        // =========================================================================
        // NHÓM 6: CẬP NHẬT ỨNG DỤNG, GIỚI THIỆU & CHIA SẺ MÃ QR
        // =========================================================================
        item {
            val updateState by com.example.data.UpdateChecker.state.collectAsState()
            val currentVersionName = remember { com.example.data.UpdateChecker.getCurrentVersionName(context) }

            SettingsCard(title = if (isEn) "App Updates, About & Share QR" else "Cập nhật, Giới thiệu & Chia sẻ QR") {
                SettingToggleRow(
                    title = if (isEn) "Check for updates on launch" else "Kiểm tra bản mới khi mở app",
                    subtitle = if (isEn) {
                        "Checks GitHub at most once every 24 hours (v$currentVersionName)"
                    } else {
                        "Tự kiểm tra ngầm tối đa 1 lần mỗi 24 giờ (Đang dùng v$currentVersionName)"
                    },
                    checked = config.autoCheckUpdatesOnLaunch,
                    onCheckedChange = { SettingsRepository.updateAutoCheckUpdatesOnLaunch(it) },
                    activeColor = XboxNeonGreen,
                    switchTestTag = "auto_check_updates_switch"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { com.example.data.UpdateChecker.checkNowManual(context) },
                        enabled = updateState !is com.example.data.UpdateCheckState.Checking,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("check_update_now_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxNeonGreen.copy(alpha = 0.65f))
                    ) {
                        Text(
                            text = when (updateState) {
                                is com.example.data.UpdateCheckState.Checking ->
                                    if (isEn) "Checking..." else "Đang kiểm tra..."
                                else ->
                                    if (isEn) "Check now" else "Kiểm tra ngay"
                            },
                            color = XboxNeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = { showWebPortalDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_web_portal_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isEn) "Share & QR" else "Mã QR & Chia sẻ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                when (val st = updateState) {
                    is com.example.data.UpdateCheckState.UpToDate -> {
                        Text(
                            text = if (isEn) {
                                "✓ Up to date (v${st.currentVersion})"
                            } else {
                                "✓ Đã là bản mới nhất (v${st.currentVersion})"
                            },
                            color = XboxNeonGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("update_check_status_text")
                        )
                    }
                    is com.example.data.UpdateCheckState.UpdateAvailable -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(XboxNeonGreen.copy(alpha = 0.14f))
                                .clickable {
                                    com.example.data.UpdateChecker.openUpdateDialog()
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("update_check_status_text"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEn) {
                                    "New version ${st.latestVersion} available · Tap to download"
                                } else {
                                    "Có bản ${st.latestVersion} · Bấm để mở trang tải"
                                },
                                color = XboxNeonGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "↗",
                                color = XboxNeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is com.example.data.UpdateCheckState.CheckFailed -> {
                        if (st.isManual) {
                            Text(
                                text = if (isEn) {
                                    "Could not check right now. Current version: v${st.currentVersion}"
                                } else {
                                    "Chưa kết nối được máy chủ kiểm tra. Bản hiện tại: v${st.currentVersion}"
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.testTag("update_check_status_text")
                            )
                        }
                    }
                    else -> {}
                }
                OutlinedButton(
                    onClick = { showAppLogDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_app_log_settings_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DirectCyan.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(15.dp))
                        Text(
                            text = if (isEn) "📋 View App Log (Track Probe & Ping)" else "📋 Xem Log Hệ Thống (Theo dõi App & Trạm đo)",
                            color = DirectCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showTermsDialog) {
        com.example.ui.components.UserTermsAgreementDialog(
            onAccept = { showTermsDialog = false },
            onDecline = { showTermsDialog = false },
            isDismissible = true,
            onDismissRequest = { showTermsDialog = false }
        )
    }

    if (showWebPortalDialog) {
        com.example.ui.components.WebDownloadPortalDialog(
            onDismiss = { showWebPortalDialog = false }
        )
    }

    if (showAppLogDialog) {
        com.example.ui.components.AppLogDialog(
            isEn = isEn,
            onDismiss = { showAppLogDialog = false }
        )
    }
}

@Composable
private fun CollapsibleSettingsGroup(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (expanded) XboxNeonGreen.copy(alpha = 0.55f) else XboxCardBorder,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onToggle() }
                .testTag(testTag),
            color = XboxSurface.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (expanded) XboxNeonGreen.copy(alpha = 0.18f) else XboxSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (expanded) XboxNeonGreen else DirectCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (expanded) XboxNeonGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = XboxNeonGreen,
    switchTestTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = activeColor),
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = valueText,
                color = accentColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 15.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(listOf(XboxNeonGreen, DirectCyan)))
                )
                Text(
                    text = title,
                    color = XboxNeonGreen,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            content()
        }
    }
}
