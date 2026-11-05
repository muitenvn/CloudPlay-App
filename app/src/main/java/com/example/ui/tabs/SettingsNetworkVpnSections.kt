package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.AppLanguage
import com.example.data.POPULAR_XBOX_LANGUAGES
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine
import com.example.vpn.VpnManager

@Composable
internal fun SettingsNetworkVpnSection(
    config: AppConfig,
    vpnState: VpnConnectionState,
    isEn: Boolean,
    expandNetwork: Boolean,
    onToggleExpand: () -> Unit
) {
    CollapsibleSettingsGroup(
        title = if (isEn) "Network & Region Unlock" else "Mạng & Mở khóa vùng",
        subtitle = if (isEn) "Auto VPN & Server Selection" else "Tự bật/tắt VPN & Chọn máy chủ",
        icon = Icons.Default.Public,
        expanded = expandNetwork,
        onToggle = onToggleExpand,
        testTag = "settings_group_network"
    ) {
        // 2.1 Tự động bật/tắt VPN
        SettingsCard(title = if (isEn) "Auto VPN" else "Tự động bật/tắt VPN") {
            SettingToggleRow(
                title = if (isEn) "Auto VPN (Unlock on launch ➔ Turn off in-game)" else "Tự bật VPN khi mở game ➔ Tự tắt khi vào chơi",
                subtitle = if (isEn) "Connects VPN to open the game, then switches back to direct network" else "Bật VPN lúc mở game để qua bước kiểm tra vùng, sau đó tắt VPN để dùng mạng trực tiếp",
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

            SettingToggleRow(
                title = if (isEn) "Allow public server auto-sync on mobile data" else "Cho phép tự cập nhật máy chủ công cộng bằng 4G/5G",
                subtitle = if (isEn) {
                    "Default OFF (auto-sync runs on Wi-Fi every 60 min; manual refresh always works)"
                } else {
                    "Mặc định TẮT (chỉ tự đồng bộ trên Wi-Fi mỗi 60 phút; nút cập nhật thủ công luôn hoạt động)"
                },
                checked = config.allowCellularLiveProxySync,
                onCheckedChange = { SettingsRepository.updateAllowCellularLiveProxySync(it) },
                switchTestTag = "settings_switch_cellular_live_proxy_sync"
            )
        }

        // 2.2 Chế độ mở khóa vùng
        SettingsCard(title = if (isEn) "Region Unlock Mode" else "Chế độ mở khóa vùng") {
            listOf(
                Triple(
                    VpnEngine.SMART_BYPASS,
                    if (isEn) "Unlock on Launch Only 🇯🇵 (Recommended)" else "Chỉ mở khóa lúc vào game 🇯🇵 (Khuyên dùng)",
                    if (isEn) "Uses proxy only during game launch" else "Chỉ đi qua máy chủ trung gian lúc mới mở game"
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

            SettingToggleRow(
                title = if (isEn) "Estimate Ping via Globalping Probe" else "Ước lượng độ trễ qua trạm Globalping",
                subtitle = if (isEn) {
                    "Sends server IP to third-party Globalping service for reference (free tier: 250 tests/hr, local cap: 200/hr)"
                } else {
                    "Gửi IP máy chủ tới dịch vụ bên thứ ba Globalping để đo tham khảo cho máy chủ đang chọn (miễn phí 250 lượt/giờ, giới hạn app 200 lượt/giờ)"
                },
                checked = config.enableGlobalpingProbe,
                onCheckedChange = { SettingsRepository.updateEnableGlobalpingProbe(it) },
                switchTestTag = "settings_switch_globalping_probe"
            )
        }
    }
}

@Composable
internal fun SettingsLanguageMarketAccountSection(
    config: AppConfig,
    vpnState: VpnConnectionState,
    isEn: Boolean,
    expandAdvanced: Boolean,
    onToggleExpand: () -> Unit,
    onClearCookies: () -> Unit,
    onOpenExternalTerms: () -> Unit,
    onOpenAppLog: () -> Unit
) {
    val context = LocalContext.current
    CollapsibleSettingsGroup(
        title = if (isEn) "Language, Auto-Translate & System" else "Ngôn ngữ, Dịch tự động & Hệ thống",
        subtitle = if (isEn) "12 World Languages, Translator, Account" else "12 ngôn ngữ quốc tế, Bộ dịch & Tài khoản",
        icon = Icons.Default.Tune,
        expanded = expandAdvanced,
        onToggle = onToggleExpand,
        testTag = "settings_group_advanced"
    ) {
        // 4.1 Bộ dịch đa ngôn ngữ & hiển thị
        SettingsCard(title = if (isEn) "Xbox Menu Translator" else "Dịch giao diện trang Xbox") {
            val activeTargetLangCode = if (config.isEffectiveTranslateEnabled()) config.getTargetTranslateLanguageCode() else SettingsRepository.TRANSLATE_LANG_OFF
            SettingToggleRow(
                title = if (isEn) "Auto-translate Xbox interface" else "Tự động dịch giao diện Xbox",
                subtitle = if (isEn) {
                    "Translates Xbox Cloud menus into your selected target language ($activeTargetLangCode)"
                } else {
                    "Dịch giao diện và thông báo Xbox sang ngôn ngữ đích đã chọn ($activeTargetLangCode)"
                },
                checked = config.isEffectiveTranslateEnabled(),
                onCheckedChange = { SettingsRepository.updateXcloudTranslate(it) },
                switchTestTag = "settings_switch_xcloud_translate"
            )

            Text(
                text = if (isEn) "Translation Target Language:" else "Ngôn ngữ dịch giao diện Xbox:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            val translateOptions = listOf(
                Triple(SettingsRepository.TRANSLATE_LANG_OFF, if (isEn) "🚫 Off" else "🚫 Tắt", "translate_lang_off_btn"),
                Triple("vi", "🇻🇳 Tiếng Việt", "translate_lang_vi_btn"),
                Triple("es", "🇪🇸 Español", "translate_lang_es_btn"),
                Triple("pt", "🇧🇷 Português", "translate_lang_pt_btn"),
                Triple("fr", "🇫🇷 Français", "translate_lang_fr_btn"),
                Triple("de", "🇩🇪 Deutsch", "translate_lang_de_btn"),
                Triple("ja", "🇯🇵 日本語", "translate_lang_ja_btn"),
                Triple("ko", "🇰🇷 한국어", "translate_lang_ko_btn"),
                Triple("zh-CN", "🇨🇳 中文", "translate_lang_zh_btn"),
                Triple("id", "🇮🇩 Indonesia", "translate_lang_id_btn"),
                Triple("th", "🇹🇭 ไทย", "translate_lang_th_btn"),
                Triple("ru", "🇷🇺 Русский", "translate_lang_ru_btn")
            )
            translateOptions.chunked(3).forEach { rowOpts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowOpts.forEach { (code, label, tag) ->
                        val isSelected = if (code == SettingsRepository.TRANSLATE_LANG_OFF) {
                            !config.isEffectiveTranslateEnabled()
                        } else {
                            config.isEffectiveTranslateEnabled() && config.getTargetTranslateLanguageCode().equals(code, ignoreCase = true)
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) DirectCyan else XboxCardBorder.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { SettingsRepository.updateTranslateLanguage(code) }
                                .testTag(tag),
                            color = if (isSelected) DirectCyan.copy(alpha = 0.16f) else XboxSurfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) DirectCyan else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            SettingToggleRow(
                title = if (isEn) "Bilingual mode (Translated + Original)" else "Chế độ song ngữ (Bản dịch + Gốc)",
                subtitle = if (isEn) "Shows original text in parentheses" else "Hiện kèm ngôn ngữ gốc trong ngoặc",
                checked = config.bilingualMode,
                onCheckedChange = { SettingsRepository.updateBilingualMode(it) }
            )
        }

        // 4.2 Ngôn ngữ ứng dụng
        SettingsCard(title = if (isEn) "App Language (Global)" else "Ngôn ngữ ứng dụng (Quốc tế)") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DirectCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isEn) {
                                "✓ Active: ${config.appLanguage.effectiveFlag()} ${config.appLanguage.titleEn} (${config.appLanguage.effectiveShortBadge()})"
                            } else {
                                "✓ Đang dùng: ${config.appLanguage.effectiveFlag()} ${config.appLanguage.title} (${config.appLanguage.effectiveShortBadge()})"
                            },
                            color = DirectCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (config.appLanguage != AppLanguage.VIETNAMESE &&
                            config.appLanguage != AppLanguage.ENGLISH &&
                            config.appLanguage != AppLanguage.SYSTEM
                        ) {
                            Text(
                                text = "Trang chủ, Thư viện, Máy chủ & HUD đã chuyển sang ${config.appLanguage.title}. Mục Cài đặt giữ Tiếng Việt để bạn dễ điều chỉnh.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

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
                                .clickable {
                                    SettingsRepository.updateAppLanguage(lang)
                                    Toast.makeText(
                                        context,
                                        "✓ ${lang.effectiveFlag()} ${lang.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
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

                val worldLanguages = listOf(
                    AppLanguage.SPANISH,
                    AppLanguage.PORTUGUESE,
                    AppLanguage.FRENCH,
                    AppLanguage.GERMAN,
                    AppLanguage.JAPANESE,
                    AppLanguage.KOREAN,
                    AppLanguage.CHINESE,
                    AppLanguage.INDONESIAN,
                    AppLanguage.THAI,
                    AppLanguage.RUSSIAN
                )
                worldLanguages.chunked(2).forEach { rowLangs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLangs.forEach { lang ->
                            val isSelected = config.appLanguage == lang
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) XboxNeonGreen else XboxCardBorder.copy(alpha = 0.7f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        SettingsRepository.updateAppLanguage(lang)
                                        Toast.makeText(
                                            context,
                                            "✓ ${lang.flag} ${lang.title} (${lang.shortBadge})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .testTag("app_lang_${lang.shortBadge.lowercase()}_button"),
                                color = if (isSelected) XboxNeonGreen.copy(alpha = 0.14f) else XboxSurfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = lang.flag, fontSize = 13.sp)
                                        Text(
                                            text = lang.title,
                                            color = if (isSelected) XboxNeonGreen else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = lang.shortBadge,
                                        color = if (isSelected) XboxNeonGreen else TextSecondary,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4.3 Thị trường trang Xbox Cloud
        SettingsCard(title = if (isEn) "Xbox Cloud Storefront Market" else "Thị trường trang Xbox Cloud") {
            var expandManualXboxMarket by rememberSaveable { mutableStateOf(false) }
            val effectiveMarket = config.getEffectiveLocale()
            val isDefaultEnUs = effectiveMarket.equals("en-US", ignoreCase = true)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (isDefaultEnUs) XboxNeonGreen else XboxCardBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { SettingsRepository.resetXboxMarketToDefault() }
                    .testTag("xbox_market_default_en_us_card"),
                color = if (isDefaultEnUs) XboxSurfaceVariant else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "🇺🇸 Default Market (en-US • Recommended)" else "🇺🇸 Thị trường mặc định (en-US • Khuyên dùng)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isEn) {
                                "Separated from app UI language to prevent unsupported region redirects"
                            } else {
                                "Tách biệt với ngôn ngữ ứng dụng để tránh bị chuyển hướng sang vùng không hỗ trợ xCloud"
                            },
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    if (isDefaultEnUs) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (!isDefaultEnUs) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                    color = Color(0x26F59E0B)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) {
                                "⚠️ Custom market active ($effectiveMarket). Non-en-US markets may cause region errors or fail to launch games."
                            } else {
                                "⚠️ Đang chọn thị trường thủ công ($effectiveMarket). Thị trường khác en-US có thể báo lỗi vùng hoặc không vào được game."
                            },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { SettingsRepository.resetXboxMarketToDefault() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = if (isEn) "Return to en-US market" else "Quay về thị trường en-US"
                                }
                                .testTag("settings_reset_xbox_market_en_us_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxNeonGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isEn) "Return to en-US" else "Quay về en-US",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, XboxCardBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable { expandManualXboxMarket = !expandManualXboxMarket }
                    .testTag("toggle_advanced_manual_xbox_market_btn"),
                color = Color.Black.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEn) "⚙️ Advanced: Manual Xbox Market Override" else "⚙️ Nâng cao: Tự chọn thị trường Xbox thủ công",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (expandManualXboxMarket) "▲" else "▼",
                        color = DirectCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expandManualXboxMarket) {
                Text(
                    text = if (isEn) {
                        "Warning: Only en-US is in the initial verified market whitelist. If a game fails to launch after switching, use 'Return to en-US'."
                    } else {
                        "Lưu ý: Danh sách trắng ban đầu chỉ gồm en-US. Nếu đổi thị trường làm không vào được game, hãy bấm 'Quay về en-US'."
                    },
                    color = Color(0xFFFBBF24),
                    fontSize = 10.5.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    POPULAR_XBOX_LANGUAGES.forEach { lang ->
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
        }

        // 4.4 Tài khoản, Định vị IP (DB-IP) & Điều khoản
        SettingsCard(title = if (isEn) "Account, IP Geolocation & Terms" else "Tài khoản, Tra cứu IP & Điều khoản") {
            SettingToggleRow(
                title = if (isEn) "Fallback HTTPS IP lookup (api.country.is)" else "Dịch vụ tra IP dự phòng qua HTTPS",
                subtitle = if (isEn) {
                    "Used only when an IP is not yet in the local DB-IP Lite database"
                } else {
                    "Chỉ gọi khi IP chưa có trong cơ sở dữ liệu DB-IP Lite trên máy"
                },
                checked = config.enableGeoIpHttpsFallback,
                onCheckedChange = { SettingsRepository.updateEnableGeoIpHttpsFallback(it) },
                switchTestTag = "settings_switch_geoip_https_fallback"
            )

            SettingToggleRow(
                title = if (isEn) "Allow DB-IP database update on mobile data" else "Cho phép tải dữ liệu DB-IP qua mạng di động",
                subtitle = if (isEn) {
                    "Default OFF (updates automatically on Wi-Fi; asks permission on 4G/5G)"
                } else {
                    "Mặc định TẮT (tự tải trên Wi-Fi; dùng mạng di động phải được bạn cho phép)"
                },
                checked = config.allowDbIpCellularDownload,
                onCheckedChange = { SettingsRepository.updateAllowDbIpCellularDownload(it) },
                switchTestTag = "settings_switch_dbip_cellular"
            )

            Text(
                text = "IP geolocation by DB-IP.com (CC-BY 4.0 • https://db-ip.com)",
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.testTag("dbip_attribution_text")
            )

            Button(
                onClick = onClearCookies,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEn) {
                            "Sign Out and Clear Login Session"
                        } else {
                            "Xóa phiên và Đăng nhập lại Xbox"
                        }
                    },
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
                onClick = onOpenExternalTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEn) {
                            "View Full Terms of Use and Privacy in external browser"
                        } else {
                            "Xem điều khoản đầy đủ và quyền riêng tư trên trình duyệt ngoài"
                        }
                    },
                colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isEn) "View Full Terms" else "Xem điều khoản đầy đủ",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onOpenAppLog,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEn) {
                            "System Activity Log"
                        } else {
                            "Nhật ký hệ thống"
                        }
                    }
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

            Button(
                onClick = {
                    val reportText = com.example.data.AppLogger.buildDiagnosticReport(
                        context = context,
                        config = config,
                        vpnState = vpnState
                    )
                    try {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(
                            android.content.ClipData.newPlainText("CloudPlay Diagnostic Report", reportText)
                        )
                        Toast.makeText(
                            context,
                            if (isEn) "Copied diagnostic report to clipboard" else "Đã sao chép báo cáo chẩn đoán vào bộ nhớ tạm",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {}
                    try {
                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_SUBJECT,
                                if (isEn) "CloudPlay Diagnostic Report" else "Báo cáo chẩn đoán CloudPlay"
                            )
                            putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                        }
                        val chooser = android.content.Intent.createChooser(
                            sendIntent,
                            if (isEn) "Share Diagnostic Report" else "Chia sẻ báo cáo chẩn đoán"
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEn) {
                            "Copy Diagnostic Report"
                        } else {
                            "Sao chép báo cáo chẩn đoán"
                        }
                    }
                    .testTag("copy_diagnostic_report_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = XboxSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isEn) "Copy Diagnostic Report" else "Sao chép báo cáo chẩn đoán",
                        color = DirectCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
