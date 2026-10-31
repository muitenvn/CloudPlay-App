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
            "Download CloudPlay for Xbox Cloud Gaming:\nWeb: ${config.publicWebPortalUrl}\nAPK: ${config.customApkDownloadUrl}"
        } else {
            "Link tải ứng dụng CloudPlay (hỗ trợ Xbox Cloud Gaming):\nTrang web: ${config.publicWebPortalUrl}\nFile APK: ${config.customApkDownloadUrl}"
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
                                    text = config.tr("Chia sẻ ứng dụng CloudPlay", "Share CloudPlay App"),
                                    color = TextPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = config.tr("Quét mã QR hoặc gửi link tải cho bạn bè", "Scan QR code or send download link"),
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
                                contentDescription = config.tr("Đóng", "Close"),
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // 2 User-facing Tabs (Share & QR | Web Preview)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, config.tr("Mã QR & Link tải", "QR Code & Link"), Icons.Default.QrCode2),
                        Triple(1, config.tr("Trang giới thiệu", "Web Page"), Icons.Default.Language)
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
                                        var qrUseDirectApk by rememberSaveable { mutableStateOf(false) }
                                        val activeQrUrl = if (qrUseDirectApk) {
                                            config.customApkDownloadUrl.ifBlank { "https://muitenvn.github.io/CloudPlay-App/" }
                                        } else {
                                            config.publicWebPortalUrl.ifBlank { "https://muitenvn.github.io/CloudPlay-App/" }
                                        }

                                        Text(
                                            text = config.tr("QUÉT MÃ QR ĐỂ TẢI ỨNG DỤNG", "SCAN QR TO DOWNLOAD"),
                                            color = XboxNeonGreen,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        // Toggle QR mode: Web Portal (faster scan, fewer modules) vs Direct APK file
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(
                                                false to config.tr("🌐 Trang Web tải", "🌐 Web Download"),
                                                true to config.tr("📥 Link file APK", "📥 Direct APK")
                                            ).forEach { (isDirectMode, modeLabel) ->
                                                val selected = qrUseDirectApk == isDirectMode
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .clickable { qrUseDirectApk = isDirectMode },
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = if (selected) XboxNeonGreen.copy(alpha = 0.18f) else XboxSurfaceVariant,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (selected) XboxNeonGreen else XboxCardBorder
                                                    )
                                                ) {
                                                    Text(
                                                        text = modeLabel,
                                                        color = if (selected) XboxNeonGreen else TextSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(212.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White)
                                                .border(2.dp, XboxNeonGreen.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                                .padding(10.dp)
                                                .testTag("qr_code_matrix_box"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            DeterministicQrMatrixCanvas(
                                                seedText = activeQrUrl,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Text(
                                            text = config.tr(
                                                "Dùng Camera hoặc ứng dụng quét mã trên điện thoại khác để mở trang tải.",
                                                "Open Camera or a QR scanner app on another phone to open the download link."
                                            ),
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
                                                        text = config.tr("🌐 Trang web giới thiệu & tải ứng dụng", "🌐 Download Web Page"),
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
                                                        text = config.tr("📥 Link tải trực tiếp file APK", "📥 Direct APK File Link"),
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
                                                    text = config.tr("Chia sẻ Link", "Share Link"),
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
                                                            config.tr("Link không hợp lệ", "Invalid URL"),
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
                                                    text = config.tr("Tải file APK", "Download APK"),
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

                        else -> {
                            // Live Preview of the bundled Web Landing Page (assets/web/index.html)
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
    val qrMatrix = remember(seedText) {
        QrCodeGenerator.encodeToMatrix(seedText)
    }
    Canvas(modifier = modifier) {
        val matrixSize = qrMatrix.size
        // Chuẩn ISO/IEC 18004: lề trắng tĩnh (Quiet Zone) xung quanh ma trận 29x29 giúp mọi camera bắt nét tức thì
        val quietZoneModules = 3
        val totalModules = matrixSize + quietZoneModules * 2
        val boardSide = minOf(size.width, size.height)
        val originX = (size.width - boardSide) / 2f
        val originY = (size.height - boardSide) / 2f
        val cellSize = boardSide / totalModules

        drawRect(color = Color.White, size = size)

        for (r in 0 until matrixSize) {
            val row = qrMatrix[r]
            val y0 = kotlin.math.floor(originY + (r + quietZoneModules) * cellSize)
            val y1 = kotlin.math.ceil(originY + (r + 1 + quietZoneModules) * cellSize)
            for (c in 0 until matrixSize) {
                if (row[c]) {
                    val x0 = kotlin.math.floor(originX + (c + quietZoneModules) * cellSize)
                    val x1 = kotlin.math.ceil(originX + (c + 1 + quietZoneModules) * cellSize)
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x = x0, y = y0),
                        size = Size(width = x1 - x0, height = y1 - y0)
                    )
                }
            }
        }
    }
}
