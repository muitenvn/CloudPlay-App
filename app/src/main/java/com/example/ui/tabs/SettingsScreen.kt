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
import androidx.compose.material.icons.filled.Delete
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
import com.example.ui.theme.XboxSurfaceVariant
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

    // Bố cục 3 tầng:
    // - Tầng 1 (Cơ bản): Đồ họa & FPS, Mạng & Mở khóa vùng, Tay cầm, Ngôn ngữ & Dịch, Quay màn hình
    // - Tầng 2 (Nâng cao): Mặc định đóng sẵn (expandAdvanced = false)
    // - Tầng 3 (Thông tin & Điều khoản): Ở dưới cùng, mở sẵn để bấm vào dễ dàng
    var expandGraphics by rememberSaveable { mutableStateOf(true) }
    var expandNetwork by rememberSaveable { mutableStateOf(false) }
    var expandControls by rememberSaveable { mutableStateOf(false) }
    var expandLanguage by rememberSaveable { mutableStateOf(false) }
    var expandRecording by rememberSaveable { mutableStateOf(false) }
    var expandAdvanced by rememberSaveable { mutableStateOf(false) }
    var expandTermsPrivacy by rememberSaveable { mutableStateOf(true) }

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
        } catch (e: Exception) {
            com.example.data.AppLogger.error(
                "UI",
                "Không mở được trình duyệt ngoài xem Điều khoản (${com.example.data.AppLogger.shortThrowable(e)})"
            )
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
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = config.tr("HÌNH ẢNH", "DISPLAY & VIDEO"),
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )
        }

        // 1. ĐỒ HỌA & KHUNG HÌNH (FPS) (Mặc định mở)
        item {
            SettingsGraphicsFpsSection(
                config = config,
                isEn = isEn,
                expandGraphics = expandGraphics,
                onToggleExpand = { expandGraphics = !expandGraphics }
            )
        }

        // 2. QUAY MÀN HÌNH (.MP4)
        item {
            SettingsScreenRecordingSection(
                config = config,
                isEn = isEn,
                expandRecording = expandRecording,
                onToggleExpand = { expandRecording = !expandRecording }
            )
        }

        item {
            Text(
                text = config.tr("ĐIỀU KHIỂN", "CONTROLS"),
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }

        // 3. TAY CẦM & CẢM ỨNG
        item {
            SettingsGamepadTouchSection(
                config = config,
                isEn = isEn,
                expandControls = expandControls,
                onToggleExpand = { expandControls = !expandControls },
                onRequestResetGamepadConfirm = { showResetGamepadConfirmDialog = true }
            )
        }

        item {
            Text(
                text = config.tr("MẠNG VÀ ỨNG DỤNG", "NETWORK & APP"),
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }

        // 4. MẠNG & MỞ KHÓA VÙNG
        item {
            SettingsNetworkVpnSection(
                config = config,
                vpnState = vpnState,
                isEn = isEn,
                expandNetwork = expandNetwork,
                onToggleExpand = { expandNetwork = !expandNetwork }
            )
        }

        // 5. NGÔN NGỮ & DỊCH GIAO DIỆN XBOX
        item {
            SettingsLanguageBasicSection(
                config = config,
                isEn = isEn,
                expandLanguage = expandLanguage,
                onToggleExpand = { expandLanguage = !expandLanguage }
            )
        }

        // 6. CÀI ĐẶT NÂNG CAO (Mặc định ĐÓNG SẴN)
        item {
            SettingsAdvancedSection(
                config = config,
                vpnState = vpnState,
                isEn = isEn,
                expandAdvanced = expandAdvanced,
                onToggleExpand = { expandAdvanced = !expandAdvanced },
                onOpenAppLog = { showAppLogDialog = true }
            )
        }

        item {
            Text(
                text = config.tr("THÔNG TIN", "INFORMATION"),
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
            )
        }

        // 7. DANH SÁCH THÔNG TIN (.list gồm 5 mục: Nhật ký hệ thống, Kiểm tra bản cập nhật, Mã QR và chia sẻ, Điều khoản và quyền riêng tư, Xóa phiên và đăng nhập lại Xbox)
        item {
            val updateState by com.example.data.UpdateChecker.state.collectAsState()
            val currentVersionName = remember { com.example.data.UpdateChecker.getCurrentVersionName(context) }

            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = XboxSurface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mục 1: Nhật ký hệ thống
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                com.example.data.AppLogger.info("UI", "Mở hộp thoại Nhật ký hệ thống")
                                showAppLogDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .semantics {
                                contentDescription = if (isEn) "View System Activity Log" else "Xem nhật ký hệ thống"
                            }
                            .testTag("open_app_log_settings_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = config.tr("Nhật ký hệ thống", "System Activity Log"),
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = config.tr("Theo dõi ứng dụng và trạm đo", "Track app events and server probes"),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "›",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    androidx.compose.material3.HorizontalDivider(color = com.example.ui.theme.XboxCardBorder, thickness = 1.dp)

                    // Mục 2: Kiểm tra bản cập nhật
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = updateState !is com.example.data.UpdateCheckState.Checking) {
                                com.example.data.UpdateChecker.checkNowManual(context)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .semantics {
                                contentDescription = if (isEn) "Check for app updates now" else "Kiểm tra bản cập nhật ngay"
                            }
                            .testTag("check_update_now_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = config.tr("Kiểm tra bản cập nhật", "Check for Updates"),
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val updateSubtitle = when (val st = updateState) {
                                is com.example.data.UpdateCheckState.Checking ->
                                    config.tr("Đang kiểm tra bản cập nhật...", "Checking for updates...")
                                is com.example.data.UpdateCheckState.UpToDate ->
                                    config.tr("Đã là bản mới nhất (v${st.currentVersion})", "Up to date (v${st.currentVersion})")
                                is com.example.data.UpdateCheckState.UpdateAvailable ->
                                    config.tr("Có bản mới ${st.latestVersion} · Chạm để tải", "New version ${st.latestVersion} available")
                                is com.example.data.UpdateCheckState.CheckFailed ->
                                    config.tr("Đang dùng v$currentVersionName · Tự kiểm tra mỗi 24 giờ", "Current v$currentVersionName · Auto-checks every 24h")
                                else ->
                                    config.tr("Đang dùng v$currentVersionName · Tự kiểm tra mỗi 24 giờ", "Current v$currentVersionName · Auto-checks every 24h")
                            }
                            Text(
                                text = updateSubtitle,
                                color = if (updateState is com.example.data.UpdateCheckState.UpdateAvailable) XboxNeonGreen else TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.testTag("update_check_status_text")
                            )
                        }
                        Text(
                            text = "›",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    androidx.compose.material3.HorizontalDivider(color = com.example.ui.theme.XboxCardBorder, thickness = 1.dp)

                    // Mục 3: Mã QR và chia sẻ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWebPortalDialog = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .semantics {
                                contentDescription = if (isEn) "Open Share and QR code dialog" else "Mở hộp thoại Mã QR và Chia sẻ"
                            }
                            .testTag("open_web_portal_settings_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = config.tr("Mã QR và chia sẻ", "QR Code & Share"),
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = config.tr("Quét mã hoặc gửi liên kết tải cho bạn bè", "Scan QR code or send download link to friends"),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "›",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    androidx.compose.material3.HorizontalDivider(color = com.example.ui.theme.XboxCardBorder, thickness = 1.dp)

                    // Mục 4: Điều khoản và quyền riêng tư
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openExternalTermsPage() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .semantics {
                                contentDescription = if (isEn) "View Terms and Privacy Policy" else "Xem điều khoản và quyền riêng tư"
                            }
                            .testTag("settings_view_full_terms_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = config.tr("Điều khoản và quyền riêng tư", "Terms & Privacy Policy"),
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = config.tr("Đọc đầy đủ 15 mục trong ứng dụng hoặc trên web", "Read all 15 sections in-app or on the web"),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "›",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    androidx.compose.material3.HorizontalDivider(color = com.example.ui.theme.XboxCardBorder, thickness = 1.dp)

                    // Mục 5: Xóa phiên và đăng nhập lại Xbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClearCookies() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .semantics {
                                contentDescription = if (isEn) "Sign Out and Clear Login Session" else "Xóa phiên và đăng nhập lại Xbox"
                            }
                            .testTag("clear_xbox_cookies_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = config.tr("Xóa phiên và đăng nhập lại Xbox", "Sign Out & Clear Xbox Session"),
                                color = com.example.ui.theme.JapanRed,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = config.tr("Làm mới toàn bộ dữ liệu đăng nhập WebView", "Clear all saved WebView cookies and login data"),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "›",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 8. TÙY CHỌN KIỂM TRA BẢN MỚI & HỘP ĐỒNG Ý ĐIỀU KHOẢN (giữ đủ tính năng hiện có)
        item {
            CollapsibleSettingsGroup(
                title = if (isEn) "Update & Terms Options" else "Tùy chọn Cập nhật & Đồng ý Điều khoản",
                subtitle = if (isEn) {
                    "Auto-check toggle & re-open terms consent dialog"
                } else {
                    "Bật/tắt tự kiểm tra bản mới và mở lại hộp đồng ý điều khoản"
                },
                icon = Icons.Default.Shield,
                expanded = expandTermsPrivacy,
                onToggle = { expandTermsPrivacy = !expandTermsPrivacy },
                testTag = "settings_group_terms_privacy"
            ) {
                SettingToggleRow(
                    title = if (isEn) "Check for updates on launch" else "Kiểm tra bản mới khi mở app",
                    subtitle = if (isEn) {
                        "Checks GitHub at most once every 24 hours"
                    } else {
                        "Tự kiểm tra ngầm tối đa 1 lần mỗi 24 giờ"
                    },
                    checked = config.autoCheckUpdatesOnLaunch,
                    onCheckedChange = { SettingsRepository.updateAutoCheckUpdatesOnLaunch(it) },
                    activeColor = XboxNeonGreen,
                    switchTestTag = "auto_check_updates_switch"
                )

                Button(
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XboxSurfaceVariant,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        text = config.tr("Mở lại hộp thoại Đồng ý Điều khoản", "Re-open Terms Consent Dialog"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showWebPortalDialog) {
        com.example.ui.components.WebDownloadPortalDialog(
            onDismiss = {
                com.example.data.AppLogger.info("UI", "Đóng hộp thoại Mã QR & Chia sẻ (từ Cài đặt)")
                showWebPortalDialog = false
            }
        )
    }

    if (showAppLogDialog) {
        com.example.ui.components.AppLogDialog(
            isEn = isEn,
            onDismiss = {
                com.example.data.AppLogger.info("UI", "Đóng hộp thoại Nhật ký hệ thống")
                showAppLogDialog = false
            }
        )
    }

    if (showFullTermsDialog) {
        FullTermsInAppDialog(
            isEn = config.isEnglishUI(),
            onOpenExternalBrowser = { launchExternalBrowserTerms() },
            onDismiss = {
                com.example.data.AppLogger.info("UI", "Đóng hộp thoại Điều khoản sử dụng")
                showFullTermsDialog = false
            }
        )
    }

    if (showResetGamepadConfirmDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                com.example.data.AppLogger.info("UI", "Hủy hộp thoại Đặt lại bố cục tay cầm mặc định")
                showResetGamepadConfirmDialog = false
            }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = XboxSurfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEn) "Reset Default Layout?" else "Đặt lại bố cục mặc định?",
                        color = TextPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (isEn) {
                            "Restore custom button coordinates to their default positions? Split modes and hidden buttons will be kept."
                        } else {
                            "Khôi phục tọa độ vị trí các nút về mặc định? Chế độ tách nút và danh sách nút ẩn sẽ được giữ nguyên."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showResetGamepadConfirmDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XboxSurface,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .semantics {
                                    contentDescription = if (isEn) "Cancel reset default layout" else "Hủy đặt lại bố cục mặc định"
                                }
                                .testTag("cancel_reset_gamepad_btn")
                        ) {
                            Text(
                                text = if (isEn) "Cancel" else "Hủy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                SettingsRepository.resetGamepadCustomOffsets()
                                showResetGamepadConfirmDialog = false
                                Toast.makeText(
                                    context,
                                    if (isEn) "Reset default button positions" else "Đã đặt lại vị trí nút mặc định",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XboxNeonGreen,
                                contentColor = androidx.compose.ui.graphics.Color(0xFF03140B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .semantics {
                                    contentDescription = if (isEn) "Confirm reset default layout" else "Xác nhận đặt lại bố cục mặc định"
                                }
                                .testTag("confirm_reset_gamepad_btn")
                        ) {
                            Text(
                                text = if (isEn) "Reset" else "Đặt lại",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
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
                document.documentElement.setAttribute('data-theme', 'dark');
                document.documentElement.setAttribute('data-inapp', 'true');
                localStorage.setItem('cp_lang', '$targetLang');
                if (window.setTermsLang) window.setTermsLang('$targetLang');
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
                .height(620.dp)
                .clip(RoundedCornerShape(22.dp))
                .testTag("full_terms_in_app_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = XboxSurfaceVariant
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(XboxSurfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        Column {
                            Text(
                                text = if (isEn) "Terms of Use & Privacy (15 Sections)" else "Điều khoản & Quyền riêng tư (15 mục)",
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (isEn) "Tap '☰ Contents (15)' below to jump to any section" else "Bấm '☰ Mục lục (15)' bên dưới để nhảy nhanh từng mục",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = onOpenExternalBrowser) {
                            Text(
                                text = if (isEn) "↗ Web" else "↗ Mở Web",
                                color = DirectCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = if (isEn) "✕ Close" else "✕ Đóng",
                                color = XboxNeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
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
                            setBackgroundColor(android.graphics.Color.parseColor("#0D1511"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : android.webkit.WebViewClient() {
                                private fun handleExternalUri(uri: android.net.Uri?): Boolean {
                                    val scheme = uri?.scheme?.lowercase() ?: return false
                                    if (scheme == "http" || scheme == "https" || scheme == "mailto") {
                                        try {
                                            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                        } catch (_: Exception) {}
                                        return true
                                    }
                                    return false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean = handleExternalUri(request?.url)

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    url: String?
                                ): Boolean = handleExternalUri(url?.let { android.net.Uri.parse(it) })

                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(syncTermsThemeAndLangJs, null)
                                }
                            }
                            loadUrl("file:///android_asset/web/terms.html?inapp=1")
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

