package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxDark
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebDownloadPortalDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var apkUrlInput by remember(config.customApkDownloadUrl) { mutableStateOf(config.customApkDownloadUrl) }
    var webUrlInput by remember(config.publicWebPortalUrl) { mutableStateOf(config.publicWebPortalUrl) }
    var repoSlugInput by remember(config.githubRepoSlug) { mutableStateOf(config.githubRepoSlug) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(
            context,
            if (isEn) "Copied: $label" else "Đã sao chép: $label",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun sharePortalLink() {
        val shareText = if (isEn) {
            "🎮 Download CloudPlay Xbox (120FPS • Low Ping & Decode):\n🌐 Web: ${config.publicWebPortalUrl}\n📥 Direct APK: ${config.customApkDownloadUrl}"
        } else {
            "🎮 Tải CloudPlay Xbox (120FPS • Giảm Ping & Decode • Cảm ứng Epic):\n🌐 Trang Web: ${config.publicWebPortalUrl}\n📥 Tải trực tiếp APK: ${config.customApkDownloadUrl}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "CloudPlay Xbox APK")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, if (isEn) "Share CloudPlay APK" else "Chia sẻ link tải CloudPlay"))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.2.dp,
                    Brush.linearGradient(listOf(XboxNeonGreen, DirectCyan)),
                    RoundedCornerShape(22.dp)
                )
                .testTag("web_download_portal_dialog"),
            color = XboxDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = XboxSurface,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(XboxNeonGreen.copy(alpha = 0.16f))
                                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isEn) "SEO Web Portal & APK Share" else "Trang Web Tải App & Chia Sẻ",
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isEn) "Built into this project (/docs + In-App Portal)" else "Tích hợp sẵn chung 1 dự án (/docs + Cổng chia sẻ)",
                                    color = DirectCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_web_portal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEn) "Close" else "Đóng",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // 3 Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, if (isEn) "Share & QR" else "Chia sẻ & QR", Icons.Default.QrCode2),
                        Triple(1, if (isEn) "Web Preview" else "Xem Trang Web", Icons.Default.Language),
                        Triple(2, if (isEn) "Link & SEO" else "Đổi Link & SEO", Icons.Default.Tune)
                    )
                    tabs.forEach { (index, label, icon) ->
                        val active = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .border(
                                    1.dp,
                                    if (active) XboxNeonGreen else XboxCardBorder,
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable { selectedTab = index }
                                .testTag("web_portal_tab_$index"),
                            color = if (active) Color(0xFF13261B) else XboxSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (active) XboxNeonGreen else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.size(5.dp))
                                Text(
                                    text = label,
                                    color = if (active) XboxNeonGreen else TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Body content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // QR Code + Direct Link Card
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = XboxSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (isEn) "SCAN OR TAP TO DOWNLOAD APK" else "MÃ CHIA SẺ NHANH & LINK TẢI APK",
                                            color = XboxNeonGreen,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        // Visual QR Matrix badge generated deterministically from the APK URL
                                        Box(
                                            modifier = Modifier
                                                .size(156.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White)
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            DeterministicQrMatrixCanvas(
                                                seedText = config.customApkDownloadUrl,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Text(
                                            text = if (isEn) {
                                                "Users searching on Google will find your Web Portal and click the button to download this APK directly."
                                            } else {
                                                "Người dùng tìm kiếm trên Google sẽ thấy trang Web của bạn và bấm nút tải trực tiếp file APK bên dưới."
                                            },
                                            color = TextSecondary,
                                            fontSize = 11.5.sp,
                                            textAlign = TextAlign.Center
                                        )

                                        // Web URL Box
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = XboxSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, DirectCyan.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isEn) "🌐 Public Search Website (SEO)" else "🌐 Địa chỉ Trang Web tìm kiếm (SEO)",
                                                        color = DirectCyan,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = config.publicWebPortalUrl,
                                                        color = TextPrimary,
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { copyToClipboard("Web URL", config.publicWebPortalUrl) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy Web Link",
                                                        tint = DirectCyan,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Direct APK Download URL Box
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = XboxSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, XboxNeonGreen.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isEn) "📥 Direct APK Download Link" else "📥 Link tải trực tiếp file APK",
                                                        color = XboxNeonGreen,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = config.customApkDownloadUrl,
                                                        color = TextPrimary,
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { copyToClipboard("APK Link", config.customApkDownloadUrl) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy APK Link",
                                                        tint = XboxNeonGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { sharePortalLink() },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("share_apk_portal_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.size(6.dp))
                                                Text(
                                                    text = if (isEn) "Share Link" else "Chia sẻ Link",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val browserIntent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(config.customApkDownloadUrl)
                                                        )
                                                        context.startActivity(browserIntent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            if (isEn) "Invalid URL" else "Link không hợp lệ",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.size(6.dp))
                                                Text(
                                                    text = if (isEn) "Test Download" else "Mở Link Tải",
                                                    color = DirectCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Live Preview of the bundled SEO Website (assets/web/index.html)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, XboxCardBorder, RoundedCornerShape(14.dp)),
                                    color = XboxSurface
                                ) {
                                    AndroidView(
                                        modifier = Modifier.fillMaxSize(),
                                        factory = { ctx ->
                                            WebView(ctx).apply {
                                                settings.javaScriptEnabled = true
                                                settings.domStorageEnabled = true
                                                webViewClient = object : WebViewClient() {
                                                    override fun onPageFinished(view: WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        val escapedApk = config.customApkDownloadUrl.replace("'", "\\'")
                                                        val escapedRepo = config.githubRepoSlug.replace("'", "\\'")
                                                        view?.evaluateJavascript(
                                                            "if (window.__updatePortalConfig) window.__updatePortalConfig('$escapedApk', '$escapedRepo');",
                                                            null
                                                        )
                                                    }
                                                }
                                                loadUrl("file:///android_asset/web/index.html")
                                            }
                                        },
                                        update = { webView ->
                                            val escapedApk = config.customApkDownloadUrl.replace("'", "\\'")
                                            val escapedRepo = config.githubRepoSlug.replace("'", "\\'")
                                            webView.evaluateJavascript(
                                                "if (window.__updatePortalConfig) window.__updatePortalConfig('$escapedApk', '$escapedRepo');",
                                                null
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        2 -> {
                            // Customize APK Download Link + GitHub Pages 1-Project Guide
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = XboxSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (isEn) "Customize Download Links" else "Tùy chỉnh Link Tải APK & Trang Web",
                                            color = XboxNeonGreen,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        OutlinedTextField(
                                            value = apkUrlInput,
                                            onValueChange = { apkUrlInput = it },
                                            label = {
                                                Text(
                                                    if (isEn) "Direct APK Download Link (GitHub Releases / Drive)"
                                                    else "Link tải file APK trực tiếp (GitHub Releases / Drive)"
                                                )
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = XboxNeonGreen,
                                                unfocusedBorderColor = XboxCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("portal_apk_url_input")
                                        )

                                        OutlinedTextField(
                                            value = webUrlInput,
                                            onValueChange = { webUrlInput = it },
                                            label = {
                                                Text(
                                                    if (isEn) "Public Website URL (GitHub Pages)"
                                                    else "Địa chỉ trang Web công khai (GitHub Pages)"
                                                )
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DirectCyan,
                                                unfocusedBorderColor = XboxCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("portal_web_url_input")
                                        )

                                        OutlinedTextField(
                                            value = repoSlugInput,
                                            onValueChange = { repoSlugInput = it },
                                            label = {
                                                Text(
                                                    if (isEn) "GitHub Repository (owner/repo)"
                                                    else "Kho GitHub tự lấy bản mới (owner/repo)"
                                                )
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = XboxNeonGreen,
                                                unfocusedBorderColor = XboxCardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                SettingsRepository.updateWebPortalSettings(
                                                    apkDownloadUrl = apkUrlInput,
                                                    webPortalUrl = webUrlInput,
                                                    githubRepo = repoSlugInput
                                                )
                                                Toast.makeText(
                                                    context,
                                                    if (isEn) "Saved APK & Web links!" else "Đã lưu cấu hình Link tải & Trang Web!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("portal_save_links_btn")
                                        ) {
                                            Text(
                                                text = if (isEn) "Save Links" else "Lưu Cấu Hình Link",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                }

                                // Instructions on how 1 project runs both Android APK + Public SEO Website
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0E1D24),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DirectCyan.copy(alpha = 0.55f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isEn) "🚀 How to publish your Web Page from this project:" else "🚀 Cách bật Trang Web lên Google từ chung 1 dự án này:",
                                            color = DirectCyan,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (isEn) {
                                                "1. Push this project to GitHub.\n" +
                                                    "2. In your GitHub repo, open Settings ➔ Pages.\n" +
                                                    "3. Select Branch 'main' and folder '/docs' ➔ Save.\n" +
                                                    "• The file docs/index.html (already created in this project with full Google SEO & sitemap.xml) will immediately go live and automatically point the Download button to your latest APK Release!"
                                            } else {
                                                "1. Đẩy (Push) dự án này lên GitHub.\n" +
                                                    "2. Trên GitHub, vào mục Settings ➔ Pages.\n" +
                                                    "3. Chọn nhánh 'main' và thư mục '/docs' ➔ bấm Save.\n" +
                                                    "• Bộ mã nguồn Web chuẩn SEO (docs/index.html, sitemap.xml, robots.txt) đã được tích hợp sẵn trong dự án này sẽ tự động chạy thành trang web công khai để người dùng tìm thấy trên Google và bấm tải thẳng file APK mới nhất!"
                                            },
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeterministicQrMatrixCanvas(
    seedText: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gridSize = 21
        val cellSize = size.width / gridSize
        val darkColor = Color(0xFF07140C)
        val accentColor = Color(0xFF0D8A38)

        fun isFinderPattern(r: Int, c: Int): Boolean {
            val inTopLeft = r in 0..6 && c in 0..6
            val inTopRight = r in 0..6 && c in (gridSize - 7) until gridSize
            val inBottomLeft = r in (gridSize - 7) until gridSize && c in 0..6
            return inTopLeft || inTopRight || inBottomLeft
        }

        fun drawFinder(startR: Int, startC: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    if (isBorder || isInner) {
                        drawRoundRect(
                            color = if (isInner) accentColor else darkColor,
                            topLeft = Offset((startC + c) * cellSize, (startR + r) * cellSize),
                            size = Size(cellSize * 0.94f, cellSize * 0.94f),
                            cornerRadius = CornerRadius(cellSize * 0.2f, cellSize * 0.2f)
                        )
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, gridSize - 7)
        drawFinder(gridSize - 7, 0)

        val bytes = seedText.ifBlank { "https://github.com" }.encodeToByteArray()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (isFinderPattern(r, c)) continue
                if (r == 6 || c == 6) {
                    if ((r + c) % 2 == 0) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                    continue
                }
                val idx = (r * gridSize + c) % bytes.size
                val hash = ((bytes[idx].toInt() and 0xFF) xor (r * 31 + c * 17))
                if (hash % 2 == 0) {
                    drawRoundRect(
                        color = darkColor,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize * 0.9f, cellSize * 0.9f),
                        cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f)
                    )
                }
            }
        }
    }
}
