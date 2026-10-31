package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ApkDownloadState
import com.example.data.UpdateCheckState
import com.example.data.UpdateChecker
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateCheckState.UpdateAvailable,
    isEn: Boolean,
    onDismiss: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val context = LocalContext.current
    val notesScrollState = rememberScrollState()
    val downloadState by UpdateChecker.downloadState.collectAsState()
    val githubReleasesUrl = UpdateChecker.FALLBACK_RELEASES_URL

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, XboxNeonGreen.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("update_available_dialog"),
            colors = CardDefaults.cardColors(containerColor = XboxSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(XboxNeonGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.tr("Đã có phiên bản mới", "New Update Available"),
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isEn) {
                                "v${updateInfo.currentVersion} ➔ v${updateInfo.latestVersion}"
                            } else {
                                "Phiên bản hiện tại v${updateInfo.currentVersion} ➔ v${updateInfo.latestVersion}"
                            },
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (updateInfo.releaseNotes.isNotBlank()) {
                    Surface(
                        color = XboxSurfaceVariant.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp)
                            .testTag("update_dialog_whats_new_section")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(notesScrollState)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = config.tr("🤖 Mô tả bản cập nhật (AI)", "🤖 AI Update Summary"),
                                color = XboxNeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = updateInfo.releaseNotes,
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Hiển thị tiến độ tải / kiểm tra SHA-256 / thông báo lỗi xác thực
                when (val dl = downloadState) {
                    is ApkDownloadState.Downloading -> {
                        val mbDone = dl.downloadedBytes / (1024f * 1024f)
                        val mbTotal = if (dl.totalBytes > 0) dl.totalBytes / (1024f * 1024f) else -1f
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(XboxSurfaceVariant.copy(alpha = 0.85f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (dl.usingFallbackGitHub) {
                                        if (isEn) "Downloading from GitHub..." else "Đang tải trực tiếp từ GitHub..."
                                    } else {
                                        if (isEn) "Fast downloading APK..." else "Đang tải nhanh APK..."
                                    },
                                    color = DirectCyan,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (mbTotal > 0f && dl.progress >= 0f) {
                                        String.format("%.1f / %.1f MB (%.0f%%)", mbDone, mbTotal, dl.progress * 100f)
                                    } else {
                                        String.format("%.1f MB", mbDone)
                                    },
                                    color = XboxNeonGreen,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (dl.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { dl.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = XboxNeonGreen,
                                    trackColor = XboxCardBorder
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = XboxNeonGreen,
                                    trackColor = XboxCardBorder
                                )
                            }
                            OutlinedButton(
                                onClick = { UpdateChecker.cancelApkDownload() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("update_dialog_cancel_download_btn"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = if (isEn) "Cancel Download" else "Hủy tải",
                                    color = WarningAmber,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is ApkDownloadState.Verifying -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(XboxSurfaceVariant.copy(alpha = 0.85f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isEn) "Verifying official SHA-256 signature..." else "Đang kiểm tra mã SHA-256 chính thức...",
                                color = XboxNeonGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = XboxNeonGreen,
                                trackColor = XboxCardBorder
                            )
                        }
                    }

                    is ApkDownloadState.VerificationFailed -> {
                        Surface(
                            color = Color(0x33FF5252),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_dialog_sha_error_box")
                        ) {
                            Text(
                                text = if (isEn) dl.messageEn else dl.messageVi,
                                color = WarningAmber,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    is ApkDownloadState.DownloadError -> {
                        Surface(
                            color = Color(0x33FF5252),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEn) dl.messageEn else dl.messageVi,
                                color = WarningAmber,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    is ApkDownloadState.VerifiedReadyToInstall -> {
                        Button(
                            onClick = { UpdateChecker.launchApkInstaller(context, dl.apkFile) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_dialog_install_verified_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isEn) "✓ SHA-256 Verified · Install Update" else "✓ Đã xác thực SHA-256 · Mở trình cài đặt",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ApkDownloadState.Idle -> {}
                }

                // Nút tải nhanh qua ghfast.top có kiểm tra SHA-256 trước khi cài
                if (downloadState !is ApkDownloadState.Downloading && downloadState !is ApkDownloadState.Verifying) {
                    Button(
                        onClick = {
                            UpdateChecker.startVerifiedApkDownload(context, updateInfo)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_dialog_fast_cdn_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEn) "⚡ Fast Download (Verified SHA-256)" else "⚡ Tải nhanh (Kiểm tra SHA-256 tự động)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dòng ghi chú minh bạch theo Mục 10 báo cáo kỹ thuật
                Text(
                    text = if (isEn) {
                        "Download from secondary server (SHA-256 verification recommended & auto-checked)."
                    } else {
                        "Tải từ máy chủ phụ (Khuyến nghị đối chiếu mã SHA-256; ứng dụng tự kiểm tra trước khi cài)."
                    },
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(1.dp))

                // Hàng nút phụ: "Để sau" và luôn có nút "Tải từ GitHub" (github.com/muitenvn/CloudPlay-App/releases)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("update_dialog_later_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                    ) {
                        Text(
                            text = if (isEn) "Later" else "Để sau",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val targetUrl = updateInfo.releaseUrl.takeIf {
                                    it.startsWith("https://github.com/muitenvn/CloudPlay-App/releases")
                                } ?: githubReleasesUrl
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("update_dialog_open_browser_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.75f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = DirectCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEn) "Download from GitHub" else "Tải từ GitHub",
                                color = DirectCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
