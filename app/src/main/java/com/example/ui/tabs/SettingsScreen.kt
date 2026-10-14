package com.example.ui.tabs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.POPULAR_XBOX_LANGUAGES
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
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
import java.util.Locale

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val systemLocale = remember { Locale.getDefault() }
    val resolvedSystemLocale = remember { SettingsRepository.resolveSystemXboxLocale() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        // Section 1: Language Settings (CRITICAL for user request)
        item {
            SettingsCard(title = "NGÔN NGỮ HIỂN THỊ XBOX CLOUD") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Chọn tiếng Anh hoặc ngôn ngữ hệ thống cho giao diện Xbox. Bạn không còn bị ép đọc tiếng Nhật khi máy chủ kết nối qua Tokyo/Nhật Bản.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    // Option A: Use System Language (with fallback to en-US)
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
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (config.useSystemLanguage) XboxNeonGreen else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Theo ngôn ngữ hệ thống (Tự động fallback en-US)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Thiết bị: ${systemLocale.displayName} (${systemLocale.toLanguageTag()}) ➔ Áp dụng Xbox: $resolvedSystemLocale",
                                    color = DirectCyan,
                                    fontSize = 10.sp
                                )
                                if (systemLocale.language.lowercase() == "vi") {
                                    Text(
                                        text = "*(Tiếng Việt chưa có trên xCloud ➔ tự động chuyển tiếng Anh en-US để dễ đọc)",
                                        color = TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            if (config.useSystemLanguage) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HOẶC CHỌN NGÔN NGỮ CỤ THỂ",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // Popular Language Chips / Items
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        POPULAR_XBOX_LANGUAGES.take(6).forEach { lang ->
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = lang.flag, fontSize = 16.sp)
                                        Column {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = lang.displayName,
                                                    color = TextPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                                if (lang.code == "en-US") {
                                                    Text(
                                                        text = "KHUYÊN DÙNG",
                                                        color = Color.Black,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier
                                                            .background(XboxNeonGreen, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${lang.nativeName} • Mã: ${lang.code}",
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = XboxNeonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option C: Manual Language input / Paste link (e.g. "xbox.com/en-US")
                    Text(
                        text = "NHẬP TÙY CHỌN MÃ NGÔN NGỮ HOẶC LINK XBOX",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        label = { Text("Ví dụ: xbox.com/en-US hoặc en-US") },
                        placeholder = { Text("xbox.com/en-US") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XboxNeonGreen,
                            unfocusedBorderColor = XboxCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_language_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                SettingsRepository.updateCustomLocale(customInputText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Áp dụng mã này", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                customInputText = "xbox.com/en-US"
                                SettingsRepository.updateCustomLocale("en-US")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Đặt en-US 🇺🇸", color = DirectCyan, fontSize = 11.sp)
                        }
                    }

                    // Display Current Active Target URL
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Link Xbox Cloud hiện tại: ${config.targetRegionUrl}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 1.5: BỘ DỊCH TIẾNG VIỆT XBOX CLOUD (TỐI ƯU ĐỘC QUYỀN)
        item {
            SettingsCard(title = "BỘ DỊCH TIẾNG VIỆT XBOX CLOUD (TỐI ƯU ĐỘC QUYỀN)") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Được tối ưu riêng cho Xbox Cloud, không phải Google Translate đại trà",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ Không làm đơ lag stream WebRTC video hay hỏng phím tay cầm.\n✓ Dịch chuẩn 250+ thuật ngữ gaming, nút bấm, hàng chờ và hướng dẫn.\n✓ Bảo vệ tên các tựa game nổi tiếng (Fortnite, Forza, Halo...) không bị dịch sai nghĩa ngớ ngẩn.\n✓ Tốc độ 0ms chạy ngay trên thiết bị mà không cần gửi dữ liệu ra máy chủ ngoài.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Toggle 1: Auto Translate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Tự động dịch sang Tiếng Việt", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Dịch toàn bộ menu, danh mục game, nút bấm và thông báo", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.xcloudTranslateEnabled,
                            onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen),
                            modifier = Modifier.testTag("settings_switch_xcloud_translate")
                        )
                    }

                    // Toggle 2: Preserve Game Titles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Bảo vệ tên tựa game gốc", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Giữ nguyên tên gốc các game (Fortnite, Forza Horizon, Halo Infinite...)", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.preserveGameTitles,
                            onCheckedChange = { SettingsRepository.updatePreserveGameTitles(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                        )
                    }

                    // Toggle 3: Bilingual mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Chế độ song ngữ (Việt - Anh)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Kèm từ gốc Tiếng Anh trong ngoặc bên cạnh bản dịch tiếng Việt", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.bilingualMode,
                            onCheckedChange = { SettingsRepository.updateBilingualMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                        )
                    }

                    // Toggle 4: Hide Decode Profile / Stream Stats overlay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Ẩn bảng Decode Profile / Stream Stats đè màn hình", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Tự động ẩn bảng thông số kỹ thuật (Decode profile, FPS, Bitrate) che tầm nhìn khi chơi", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.hideStreamStatsOverlay,
                            onCheckedChange = { SettingsRepository.updateHideStreamStatsOverlay(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = DirectCyan),
                            modifier = Modifier.testTag("settings_switch_hide_decode_profile")
                        )
                    }

                    // Toggle 5: GitHub Better-xCloud Dictionary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Từ điển Gaming đầy đủ từ GitHub (550+ mục)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Trọn bộ từ điển Better-xCloud GitHub với thuật toán tra cứu O(1) tức thì", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.githubDictionaryEnabled,
                            onCheckedChange = { SettingsRepository.updateGithubDictionaryEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                        )
                    }
                }
            }
        }

        // Section: 4G & Wi-Fi Network Optimization
        item {
            SettingsCard(title = "TỐI ƯU MẠNG 4G & WIFI (TỐC ĐỘ HÀNG ĐẦU)") {
                Text(
                    text = "Tối ưu hóa đường truyền cho tất cả các loại mạng 4G/LTE/5G di động (Viettel, Vina, Mobi...) và tự động tinh chỉnh mạng Wifi ưu tiên 'Tốc độ #1, Đồ họa #2'.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4G Options Title
                Text(
                    text = "CHẾ ĐỘ MẠNG DI ĐỘNG 4G/5G:",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 4G Option 1: Data Saver High Speed
                val isDataSaver = config.cellularMode == com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isDataSaver) XboxNeonGreen else XboxCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { SettingsRepository.updateCellularMode(com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED) },
                    color = if (isDataSaver) Color(0xFF132219) else Color(0xFF0F1813)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "4G Tiết Kiệm Data (Tốc Độ Cao)",
                                    color = if (isDataSaver) XboxNeonGreen else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = XboxNeonGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "KHUYÊN DÙNG",
                                        color = XboxNeonGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tiết kiệm 50% data di động (~3.2GB/giờ thay vì 7GB). Khóa 60 FPS mượt mà, độ trễ đệm 0ms, bộ giải mã H.264 Baseline chống nóng máy cho mọi chip 4G.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (isDataSaver) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4G Option 2: Max Speed Uncapped ("Có nhiêu sài hết")
                val isMaxSpeed = config.cellularMode == com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isMaxSpeed) DirectCyan else XboxCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { SettingsRepository.updateCellularMode(com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED) },
                    color = if (isMaxSpeed) Color(0xFF0F2327) else Color(0xFF0F1813)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "4G Có Nhiêu Sài Hết (Max Speed 20M)",
                                    color = if (isMaxSpeed) DirectCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DirectCyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "MAX BĂNG THÔNG",
                                        color = DirectCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bung trọn vẹn 20 Mbps băng thông 4G/5G tốc độ cao. Ưu tiên hình ảnh sắc nét tối đa, hàng đợi đệm WebRTC 0ms phản hồi tay cầm tức thì.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (isMaxSpeed) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wifi Auto-Tuning Policy Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.wifiSpeedFirstPriority) Color(0xFF0D211F) else Color(0xFF141D17))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Wifi: Tự Động Tinh Chỉnh (Tốc độ #1, Đồ họa #2)",
                                color = if (config.wifiSpeedFirstPriority) DirectCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tự động phân tích chất lượng sóng Wifi (2.4GHz / 5GHz) & độ trễ RTT. Luôn đặt TỐC ĐỘ LÊN HÀNG ĐẦU, khi có dao động sóng sẽ lập tức triệt tiêu độ trễ đệm để không bao giờ bị delay thao tác, đồ họa tự thích ứng theo sau.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.wifiSpeedFirstPriority,
                        onCheckedChange = { SettingsRepository.updateWifiSpeedFirstPriority(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = DirectCyan
                        )
                    )
                }
            }
        }

        // Section 2: Smart VPN & Auto Bypass Logic
        item {
            SettingsCard(title = "VPN THÔNG MINH (SMART VPN AUTO ON/OFF) & ZERO RELOAD") {
                // Smart VPN Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Kích hoạt VPN Thông Minh (Auto On/Off)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Tự động phân tích IP người chơi: Tự bật VPN khi mở game nếu chưa hỗ trợ, tự tắt khi vào trận", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.smartVpnEnabled,
                        onCheckedChange = { SettingsRepository.updateSmartVpnEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen),
                        modifier = Modifier.testTag("settings_switch_smart_vpn")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tự động ngắt VPN khi vào game", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Chuyển về mạng thường (Direct ISP) ngay khi luồng WebRTC kết nối, không reload trang", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.autoBypassEnabled,
                        onCheckedChange = { SettingsRepository.updateAutoBypass(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen),
                        modifier = Modifier.testTag("settings_switch_auto_bypass")
                    )
                }

                if (config.autoBypassEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Thời gian đếm ngược trước khi ngắt VPN", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "${config.autoBypassDelaySeconds} giây", color = DirectCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = config.autoBypassDelaySeconds.toFloat(),
                            onValueChange = { SettingsRepository.updateAutoBypassDelay(it.toInt()) },
                            valueRange = 1f..8f,
                            steps = 6,
                            colors = SliderDefaults.colors(thumbColor = DirectCyan, activeTrackColor = DirectCyan, inactiveTrackColor = Color.DarkGray)
                        )
                    }
                }
            }
        }

        // Section 3: Engine Selection
        item {
            SettingsCard(title = "CHẾ ĐỘ VƯỢT RÀO (VPN ENGINE)") {
                listOf(
                    Triple(VpnEngine.SMART_BYPASS, "Smart Japan Bypass 🇯🇵 (Khuyên dùng)", "Vượt rào trực tiếp Better-xCloud, 0 độ trễ, không sợ chết proxy"),
                    Triple(VpnEngine.PROXY_TUNNEL, "Proxy Route 🌐 (20+ Máy chủ)", "Định tuyến qua máy chủ Proxy Nhật Bản với tự động đổi máy chủ dự phòng"),
                    Triple(VpnEngine.EXTERNAL_VPN, "VPN Ngoài (1.1.1.1 / WARP / Kiwi)", "Dành cho người chơi dùng app VPN riêng")
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
                            modifier = Modifier.padding(12.dp),
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

        // Section 4: Stream Quality & Visuals
        item {
            SettingsCard(title = "CHẤT LƯỢNG HÌNH ẢNH & ĐIỀU KHIỂN") {
                // 1080p Edge UA
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Độ phân giải 1080p Full HD (Edge UA)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Giả lập Edge Desktop để Xbox stream ở 1080p thay vì 720p", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.forceDesktopUserAgent,
                        onCheckedChange = { SettingsRepository.updateDesktopUserAgent(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Clarity Boost
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Bộ lọc Clarity Boost (Làm nét)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Tăng độ sắc nét và tương phản video stream", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.clarityBoostEnabled,
                        onCheckedChange = { SettingsRepository.updateClarityBoost(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Virtual Touch Gamepad
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tay cầm ảo trên màn hình", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Hiện D-Pad, cần Analog và nút ABXY khi không có tay cầm Bluetooth", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.showVirtualController,
                        onCheckedChange = { SettingsRepository.updateShowVirtualController(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = XboxNeonGreen)
                    )
                }
            }
        }

        // Section 5: Storage & Reset
        item {
            SettingsCard(title = "BỘ NHỚ ĐỆM & TÀI KHOẢN") {
                Button(
                    onClick = onClearCookies,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text(text = "Xóa Cookie & Đăng Nhập Lại Xbox", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
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
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(XboxNeonGreen)
                )
                Text(
                    text = title,
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            content()
        }
    }
}
