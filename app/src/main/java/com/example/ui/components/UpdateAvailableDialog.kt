package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.NewReleases
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val mirrorChipsScrollState = rememberScrollState()
    val downloadState by UpdateChecker.downloadState.collectAsState()
    val githubReleasesUrl = UpdateChecker.FALLBACK_RELEASES_URL
    var selectedMirrorId by remember { mutableStateOf("auto") }
    val mirrorNodes = remember { UpdateChecker.FAST_MIRROR_NODES.filter { !it.isDirectGitHub } }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .heightIn(max = 160.dp)
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

                // Hiển thị tiến độ tải / trạng thái chuyển máy chủ phụ / kiểm tra SHA-256 / thông báo lỗi
                when (val dl = downloadState) {
                    is ApkDownloadState.Downloading -> {
                        val mbDone = dl.downloadedBytes / (1024f * 1024f)
                        val mbTotal = if (dl.totalBytes > 0) dl.totalBytes / (1024f * 1024f) else -1f
                        val activeNodeLabel = dl.activeMirrorName.ifBlank {
                            if (dl.usingFallbackGitHub) "GitHub Direct" else "Mirror"
                        }
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
                                        if (isEn) {
                                            "📥 Direct GitHub (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        } else {
                                            "📥 Đang tải thường: GitHub gốc (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        }
                                    } else {
                                        if (isEn) {
                                            "⚡ Mirror: $activeNodeLabel (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        } else {
                                            "⚡ Máy chủ phụ: $activeNodeLabel (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        }
                                    },
                                    color = DirectCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
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
                            if (dl.failedMirrors.isNotEmpty()) {
                                Text(
                                    text = if (isEn) {
                                        "🔄 Auto-switched mirror (skipped: ${dl.failedMirrors.takeLast(3).joinToString(", ")})"
                                    } else {
                                        "🔄 Đã tự chuyển máy chủ (bỏ qua: ${dl.failedMirrors.takeLast(3).joinToString(", ")})"
                                    },
                                    color = WarningAmber,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("update_dialog_failover_notice")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (dl.mirrorIndex < dl.totalMirrors) {
                                    OutlinedButton(
                                        onClick = { UpdateChecker.skipToNextMirror() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("update_dialog_skip_mirror_btn"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.75f))
                                    ) {
                                        Text(
                                            text = if (isEn) "⏭ Next Mirror" else "⏭ Đổi máy chủ khác",
                                            color = DirectCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { UpdateChecker.cancelApkDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("update_dialog_cancel_download_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        text = if (isEn) "Cancel" else "Hủy tải",
                                        color = WarningAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
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
                                text = if (isEn) "Verifying APK & official SHA-256 signature..." else "Đang kiểm tra gói APK & mã SHA-256...",
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
                                text = if (isEn) "✓ Verified · Install Update" else "✓ Đã xác thực · Mở trình cài đặt",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ApkDownloadState.Idle -> {}
                }

                // Bộ chọn máy chủ phụ + 2 nút tải trong app: (1) Tải qua máy chủ phụ & (2) Tải thường trực tiếp GitHub
                if (downloadState !is ApkDownloadState.Downloading && downloadState !is ApkDownloadState.Verifying) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isEn) {
                                "Select mirror server (${mirrorNodes.size} backup mirrors, auto-switches on error):"
                            } else {
                                "Chọn máy chủ phụ (${mirrorNodes.size} cổng dự phòng, tự đổi khi lỗi):"
                            },
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(mirrorChipsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAutoSelected = selectedMirrorId == "auto"
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMirrorId = "auto" }
                                    .testTag("update_mirror_chip_auto"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAutoSelected) XboxNeonGreen.copy(alpha = 0.20f) else XboxSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAutoSelected) XboxNeonGreen else XboxCardBorder
                                )
                            ) {
                                Text(
                                    text = if (isEn) "⚡ Auto (${mirrorNodes.size})" else "⚡ Tự động (${mirrorNodes.size})",
                                    color = if (isAutoSelected) XboxNeonGreen else TextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isAutoSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }

                            mirrorNodes.forEachIndexed { idx, node ->
                                val isSelected = selectedMirrorId == node.id
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedMirrorId = node.id }
                                        .testTag("update_mirror_chip_${node.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) DirectCyan.copy(alpha = 0.20f) else XboxSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) DirectCyan else XboxCardBorder
                                    )
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${node.name}",
                                        color = if (isSelected) DirectCyan else TextSecondary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Nút 1: Tải nhanh qua máy chủ phụ (tự động chuyển sang máy chủ phụ tiếp theo nếu lỗi)
                    val selectedNodeName = mirrorNodes.firstOrNull { it.id == selectedMirrorId }?.name
                    Button(
                        onClick = {
                            UpdateChecker.startVerifiedApkDownload(
                                context = context,
                                updateInfo = updateInfo,
                                preferredMirrorId = selectedMirrorId
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_dialog_fast_cdn_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedNodeName != null) {
                                if (isEn) "⚡ Download via $selectedNodeName (Auto-failover)" else "⚡ Tải qua máy chủ phụ: $selectedNodeName"
                            } else {
                                if (isEn) "⚡ Fast download via mirror (Auto-failover)" else "⚡ Tải nhanh qua máy chủ phụ (Tự đổi khi lỗi)"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Nút 2: Tải thường trực tiếp từ GitHub ngay trong ứng dụng (nếu GitHub lỗi vẫn có máy chủ phụ đỡ)
                    OutlinedButton(
                        onClick = {
                            UpdateChecker.startVerifiedApkDownload(
                                context = context,
                                updateInfo = updateInfo,
                                preferredMirrorId = "github_direct"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_dialog_normal_direct_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, DirectCyan)
                    ) {
                        Text(
                            text = if (isEn) {
                                "📥 Normal Download (Direct from GitHub)"
                            } else {
                                "📥 Tải thường (Trực tiếp từ GitHub gốc)"
                            },
                            color = DirectCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Hàng nút phụ: "Để sau" và "Mở trang GitHub"
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
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val targetUrl = updateInfo.releaseUrl.takeIf {
                                    it.startsWith("https://github.com/muitenvn/CloudPlay-App/releases")
                                } ?: githubReleasesUrl
                                com.example.data.AppLogger.info("UI", "Mở trang bản phát hành GitHub trên trình duyệt ngoài")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                com.example.data.AppLogger.error(
                                    "UI",
                                    "Lỗi mở trình duyệt tới trang GitHub Releases (${com.example.data.AppLogger.shortThrowable(e)})"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("update_dialog_open_browser_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.65f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = DirectCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEn) "Open GitHub Web" else "Mở web GitHub",
                                color = DirectCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
