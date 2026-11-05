package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.vpn.VpnConnectionState

@Composable
fun SettingsScreen(
    config: AppConfig,
    vpnState: VpnConnectionState,
    @Suppress("UNUSED_PARAMETER") onOpenGuide: (com.example.ui.components.GuideSection) -> Unit = {},
    onClearCookies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWebPortalDialog by remember { mutableStateOf(false) }
    var showAppLogDialog by remember { mutableStateOf(false) }
    var showFullTermsDialog by remember { mutableStateOf(false) }
    var showResetGamepadConfirmDialog by remember { mutableStateOf(false) }

    // 6 nhóm gập/mở (mặc định gập, chỉ nhóm đồ họa chính mở)
    var expandGraphics by rememberSaveable { mutableStateOf(true) }
    var expandNetwork by rememberSaveable { mutableStateOf(false) }
    var expandControls by rememberSaveable { mutableStateOf(false) }
    var expandAdvanced by rememberSaveable { mutableStateOf(false) }
    var expandRecording by rememberSaveable { mutableStateOf(false) }
    var expandTermsPrivacy by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val isEn = config.isSettingsEnglishUI()
    val termsWebUrl = "https://muitenvn.github.io/CloudPlay-App/terms.html"

    fun openExternalTermsPage() {
        showFullTermsDialog = true
    }

    fun launchExternalBrowserTerms() {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(termsWebUrl)
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                if (isEn) "Could not open browser: $termsWebUrl" else "Không mở được trình duyệt: $termsWebUrl",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // NHÓM 1: ĐỒ HỌA & KHUNG HÌNH (FPS) (Mặc định mở)
        item {
            SettingsGraphicsFpsSection(
                config = config,
                isEn = isEn,
                expandGraphics = expandGraphics,
                onToggleExpand = { expandGraphics = !expandGraphics }
            )
        }

        // NHÓM 2: MẠNG & MỞ KHÓA VÙNG (Mặc định gập)
        item {
            SettingsNetworkVpnSection(
                config = config,
                vpnState = vpnState,
                isEn = isEn,
                expandNetwork = expandNetwork,
                onToggleExpand = { expandNetwork = !expandNetwork }
            )
        }

        // NHÓM 3: TAY CẦM & CẢM ỨNG (Mặc định gập)
        item {
            SettingsGamepadTouchSection(
                config = config,
                isEn = isEn,
                expandControls = expandControls,
                onToggleExpand = { expandControls = !expandControls },
                onRequestResetGamepadConfirm = { showResetGamepadConfirmDialog = true }
            )
        }

        // NHÓM 4: NGÔN NGỮ, DỊCH TỰ ĐỘNG & HỆ THỐNG (Mặc định gập)
        item {
            SettingsLanguageMarketAccountSection(
                config = config,
                vpnState = vpnState,
                isEn = isEn,
                expandAdvanced = expandAdvanced,
                onToggleExpand = { expandAdvanced = !expandAdvanced },
                onClearCookies = onClearCookies,
                onOpenExternalTerms = { openExternalTermsPage() },
                onOpenAppLog = { showAppLogDialog = true }
            )
        }

        // NHÓM 5: QUAY MÀN HÌNH (TÙY CHỌN PHỤ) (Mặc định gập)
        item {
            SettingsScreenRecordingSection(
                config = config,
                isEn = isEn,
                expandRecording = expandRecording,
                onToggleExpand = { expandRecording = !expandRecording }
            )
        }

        // NHÓM 5.5: NHẠC NỀN (Tự phát, Phát thủ công & Phát khi đang chơi)
        item {
            SettingsBackgroundMusicSection(
                config = config,
                isEn = isEn
            )
        }

        // NHÓM 6: CẬP NHẬT ỨNG DỤNG, GIỚI THIỆU & CHIA SẺ MÃ QR
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
                            .semantics {
                                contentDescription = if (isEn) "Check for app updates now" else "Kiểm tra bản cập nhật ngay"
                            }
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
                            .semantics {
                                contentDescription = if (isEn) "Open Share and QR code dialog" else "Mở hộp thoại Mã QR và Chia sẻ"
                            }
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
                        .semantics {
                            contentDescription = if (isEn) "View System Activity Log" else "Xem nhật ký hệ thống"
                        }
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

        // NHÓM 7: ĐIỀU KHOẢN & QUYỀN RIÊNG TƯ
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Terms & Privacy" else "Điều khoản & Quyền riêng tư",
                subtitle = if (isEn) {
                    "Quick summary & link to full terms"
                } else {
                    "Tóm tắt nhanh & liên kết điều khoản đầy đủ"
                },
                icon = Icons.Default.Shield,
                expanded = expandTermsPrivacy,
                onToggle = { expandTermsPrivacy = !expandTermsPrivacy },
                testTag = "settings_group_terms_privacy"
            ) {
                SettingsCard(title = if (isEn) "Quick Summary" else "Tóm tắt nhanh") {
                    val summaryLines = if (isEn) {
                        listOf(
                            "• Independent app, not affiliated with Microsoft, Xbox, or Epic Games.",
                            "• Using VPN/region switching may violate third-party terms; you are responsible for your own use.",
                            "• Public servers are operated by independent third parties.",
                            "• The app does not collect your account credentials or passwords.",
                            "• IP geolocation by DB-IP.com (CC BY 4.0)."
                        )
                    } else {
                        listOf(
                            "• Ứng dụng độc lập, không liên kết với Microsoft, Xbox hay Epic Games.",
                            "• Dùng VPN/đổi vùng có thể vi phạm điều khoản bên thứ ba; người dùng tự chịu trách nhiệm.",
                            "• Máy chủ công cộng do bên thứ ba độc lập vận hành.",
                            "• Ứng dụng không thu thập tài khoản hay mật khẩu của bạn.",
                            "• Dữ liệu định vị IP: IP geolocation by DB-IP.com (CC BY 4.0)."
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        summaryLines.forEach { line ->
                            Text(
                                text = line,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { openExternalTermsPage() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (isEn) {
                                    "View Full Terms in external browser"
                                } else {
                                    "Xem điều khoản đầy đủ trên trình duyệt ngoài"
                                }
                            }
                            .testTag("settings_view_full_terms_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxNeonGreen.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = XboxNeonGreen.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (isEn) "View Full Terms (Web)" else "Xem điều khoản đầy đủ (Web)",
                                color = XboxNeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { SettingsRepository.updateUserAcceptedTerms(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (isEn) {
                                    "Review or revoke Terms of Use agreement"
                                } else {
                                    "Mở lại hộp thoại đồng ý Điều khoản sử dụng"
                                }
                            }
                            .testTag("settings_reopen_terms_consent_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DirectCyan.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (isEn) "Re-open Terms Consent Dialog" else "Mở lại hộp thoại Đồng ý Điều khoản",
                                color = DirectCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
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

    if (showFullTermsDialog) {
        FullTermsInAppDialog(
            isEn = config.isEnglishUI(),
            onOpenExternalBrowser = { launchExternalBrowserTerms() },
            onDismiss = { showFullTermsDialog = false }
        )
    }

    if (showResetGamepadConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetGamepadConfirmDialog = false },
            containerColor = XboxSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = if (isEn) "Reset Default Layout?" else "Đặt lại bố cục mặc định?",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "Restore custom button coordinates to their default positions? Split modes and hidden buttons will be kept."
                    } else {
                        "Khôi phục tọa độ vị trí các nút về mặc định? Chế độ tách nút và danh sách nút ẩn sẽ được giữ nguyên."
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SettingsRepository.resetGamepadCustomOffsets()
                        showResetGamepadConfirmDialog = false
                        Toast.makeText(
                            context,
                            if (isEn) "Reset default button positions" else "Đã đặt lại vị trí nút mặc định",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .semantics {
                            contentDescription = if (isEn) "Confirm reset default layout" else "Xác nhận đặt lại bố cục mặc định"
                        }
                        .testTag("confirm_reset_gamepad_btn")
                ) {
                    Text(
                        text = if (isEn) "Reset" else "Đặt lại",
                        color = XboxNeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetGamepadConfirmDialog = false },
                    modifier = Modifier
                        .semantics {
                            contentDescription = if (isEn) "Cancel reset default layout" else "Hủy đặt lại bố cục mặc định"
                        }
                        .testTag("cancel_reset_gamepad_btn")
                ) {
                    Text(
                        text = if (isEn) "Cancel" else "Hủy",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FullTermsInAppDialog(
    isEn: Boolean,
    onOpenExternalBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val syncTermsThemeAndLangJs = remember(isEn) {
        val targetLang = if (isEn) "en" else "vi"
        """
        (function() {
            try {
                document.documentElement.style.setProperty('--bg', '#0b1210');
                document.documentElement.style.setProperty('--surface', '#121b17');
                document.documentElement.style.setProperty('--sum-bg', '#17231e');
                document.documentElement.style.setProperty('--sum-hover', '#1d2c26');
                document.documentElement.style.setProperty('--line', '#283931');
                document.documentElement.style.setProperty('--text', '#eaf1ee');
                document.documentElement.style.setProperty('--muted', '#b2c5bd');
                document.documentElement.style.setProperty('--accent', '#3fb950');
                document.documentElement.style.setProperty('--accent-d', '#2f9a40');
                document.documentElement.style.setProperty('--accent-soft', '#132a1c');
                document.documentElement.style.setProperty('--on-accent', '#06130a');
                document.documentElement.style.setProperty('--warn-bg', '#241c0b');
                document.documentElement.style.setProperty('--warn-line', '#524014');
                document.documentElement.style.setProperty('--warn-text', '#f3dca2');
                document.documentElement.style.setProperty('--bar', 'rgba(11,18,16,0.95)');
                localStorage.setItem('cp_lang', '$targetLang');
                var btn = document.querySelector('.lang button[data-lang="$targetLang"]');
                if (btn) btn.click();
            } catch (e) {}
        })();
        """.trimIndent()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(580.dp)
                .clip(RoundedCornerShape(22.dp))
                .testTag("full_terms_in_app_dialog"),
            color = com.example.ui.theme.XboxDark,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, XboxNeonGreen.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(XboxSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isEn) "Terms of Use & Privacy" else "Điều khoản & Quyền riêng tư",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(onClick = onOpenExternalBrowser) {
                            Text(
                                text = if (isEn) "↗ Browser" else "↗ Trình duyệt",
                                color = DirectCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = if (isEn) "✕ Close" else "✕ Đóng",
                                color = XboxNeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#0B1210"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(syncTermsThemeAndLangJs, null)
                                }
                            }
                            loadUrl("file:///android_asset/web/terms.html")
                        }
                    },
                    update = { webView ->
                        webView.evaluateJavascript(syncTermsThemeAndLangJs, null)
                    }
                )
            }
        }
    }
}

