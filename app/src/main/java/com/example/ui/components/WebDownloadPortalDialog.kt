package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
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
    var qrUseDirectApk by rememberSaveable { mutableStateOf(false) }

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
            "Download Ravno App for Xbox Cloud Gaming:\nWeb: ${config.publicWebPortalUrl}\nAPK: ${config.customApkDownloadUrl}"
        } else {
            "Link tải ứng dụng Ravno App (hỗ trợ Xbox Cloud Gaming):\nTrang web: ${config.publicWebPortalUrl}\nFile APK: ${config.customApkDownloadUrl}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ravno Xbox APK")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, if (isEn) "Share Ravno APK" else "Chia sẻ link tải Ravno"))
    }

    val openApkLink: () -> Unit = {
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
    }

    val shareBtnFocusRequester = remember { FocusRequester() }
    val closeBtnFocusRequester = remember { FocusRequester() }

    val activeQrUrl = if (qrUseDirectApk) {
        config.customApkDownloadUrl.ifBlank { "https://muitenvn.github.io/Ravno-App/" }
    } else {
        config.publicWebPortalUrl.ifBlank { "https://muitenvn.github.io/Ravno-App/" }
    }

    val portalSyncJs = remember(config.customApkDownloadUrl, config.githubRepoSlug, isEn) {
        val escapedApk = config.customApkDownloadUrl.replace("'", "\\'")
        val escapedRepo = config.githubRepoSlug.replace("'", "\\'")
        val targetLang = if (isEn) "en" else "vi"
        """
        (function() {
            try {
                document.documentElement.setAttribute('data-theme', 'dark');
                localStorage.setItem('cp_lang', '$targetLang');
                if (window.setPortalLang) window.setPortalLang('$targetLang');
                if (window.__updatePortalConfig) window.__updatePortalConfig('$escapedApk', '$escapedRepo');
            } catch (e) {}
        })();
        """.trimIndent()
    }

    AdaptivePopupFrame(
        onDismissRequest = onDismiss,
        dialogTestTag = "web_download_portal_dialog",
        defaultFocusRequester = if (selectedTab == 0) shareBtnFocusRequester else closeBtnFocusRequester,
        wideWidthFraction = 0.95f,
        portraitWidthFraction = 0.95f,
        onPreviousTab = { selectedTab = (selectedTab - 1 + 2) % 2 },
        onNextTab = { selectedTab = (selectedTab + 1) % 2 },
        onBackPress = onDismiss
    ) {
        @Composable
        fun HeaderSection() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapMed),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(RoundedCornerShape(btnCorner))
                            .background(XboxNeonGreen.copy(alpha = 0.16f))
                            .border(borderThin, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(btnCorner)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.tr("Chia sẻ ứng dụng Ravno", "Share Ravno App"),
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = rSp(15f),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = config.tr("Quét mã QR hoặc gửi link tải cho bạn bè", "Scan QR code or send download link"),
                            color = DirectCyan,
                            fontSize = rSp(10.5f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder),
                    modifier = Modifier
                        .size(rDp(30f))
                        .clip(CircleShape)
                        .gamepadClickable(
                            cornerRadius = rDp(15f),
                            focusRequester = closeBtnFocusRequester,
                            onClick = onDismiss
                        )
                        .testTag("close_web_portal_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = config.tr("Đóng", "Close"),
                            tint = TextSecondary,
                            modifier = Modifier.size(rDp(16f))
                        )
                    }
                }
            }
        }

        @Composable
        fun TabsRow() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapSmall)
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
                            .clip(RoundedCornerShape(btnCorner))
                            .border(
                                borderThin,
                                if (active) XboxNeonGreen else XboxCardBorder,
                                RoundedCornerShape(btnCorner)
                            )
                            .gamepadClickable(
                                cornerRadius = btnCorner,
                                onStepHorizontal = { dir ->
                                    selectedTab = (selectedTab + dir).coerceIn(0, 1)
                                }
                            ) { selectedTab = index }
                            .testTag("web_portal_tab_$index"),
                        color = if (active) Color(0xFF13261B) else XboxSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = gapMed, horizontal = gapSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) XboxNeonGreen else TextSecondary,
                                modifier = Modifier.size(rDp(15f))
                            )
                            Spacer(modifier = Modifier.width(rDp(4f)))
                            Text(
                                text = label,
                                color = if (active) XboxNeonGreen else TextPrimary,
                                fontSize = rSp(11.5f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun QrMatrixPreviewCard(qrSize: androidx.compose.ui.unit.Dp) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(sectionCorner),
                color = XboxSurface,
                border = BorderStroke(borderThin, XboxCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(padInner),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    Text(
                        text = config.tr("QUÉT MÃ QR ĐỂ TẢI ỨNG DỤNG", "SCAN QR TO DOWNLOAD"),
                        color = XboxNeonGreen,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = rSp(11.5f),
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gapSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            false to config.tr("🌐 Trang Web tải", "🌐 Web Download"),
                            true to config.tr("📥 Link file APK", "📥 Direct APK")
                        ).forEach { (isDirectMode, modeLabel) ->
                            val selected = qrUseDirectApk == isDirectMode
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(cardCorner))
                                    .gamepadClickable(
                                        cornerRadius = cardCorner,
                                        onStepHorizontal = { dir -> qrUseDirectApk = dir > 0 }
                                    ) { qrUseDirectApk = isDirectMode },
                                shape = RoundedCornerShape(cardCorner),
                                color = if (selected) XboxNeonGreen.copy(alpha = 0.18f) else XboxSurfaceVariant,
                                border = BorderStroke(
                                    borderThin,
                                    if (selected) XboxNeonGreen else XboxCardBorder
                                )
                            ) {
                                Text(
                                    text = modeLabel,
                                    color = if (selected) XboxNeonGreen else TextSecondary,
                                    fontSize = rSp(10.5f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = padInner, vertical = rDp(4.5f))
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(qrSize)
                            .clip(RoundedCornerShape(sectionCorner))
                            .background(Color.White)
                            .border(borderFocus, XboxNeonGreen.copy(alpha = 0.65f), RoundedCornerShape(sectionCorner))
                            .padding(rDp(8f))
                            .testTag("qr_code_matrix_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        DeterministicQrMatrixCanvas(
                            seedText = activeQrUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = if (qrUseDirectApk) {
                            config.tr(
                                "✓ Mã QR đang trỏ tới: 📥 Link tải trực tiếp file APK",
                                "✓ QR encodes: 📥 Direct APK File Link"
                            )
                        } else {
                            config.tr(
                                "✓ Mã QR đang trỏ tới: 🌐 Trang web giới thiệu & tải ứng dụng",
                                "✓ QR encodes: 🌐 Download Web Page"
                            )
                        },
                        color = if (qrUseDirectApk) XboxNeonGreen else DirectCyan,
                        fontSize = rSp(10.5f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        @Composable
        fun LinksAndShareSection() {
            Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                Text(
                    text = config.tr(
                        "Dùng Camera hoặc ứng dụng quét mã trên điện thoại khác để mở trang tải.",
                        "Open Camera or a QR scanner app on another phone to open the download link."
                    ),
                    color = TextSecondary,
                    fontSize = rSp(10.5f)
                )

                // Web URL Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(btnCorner))
                        .gamepadClickable(cornerRadius = btnCorner) { qrUseDirectApk = false },
                    shape = RoundedCornerShape(btnCorner),
                    color = if (!qrUseDirectApk) DirectCyan.copy(alpha = 0.14f) else XboxSurface,
                    border = BorderStroke(
                        if (!qrUseDirectApk) borderFocus else borderThin,
                        if (!qrUseDirectApk) DirectCyan else DirectCyan.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = padInner, vertical = gapSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (if (!qrUseDirectApk) "✓ " else "") +
                                    config.tr("🌐 Trang web giới thiệu & tải ứng dụng", "🌐 Download Web Page"),
                                color = DirectCyan,
                                fontSize = rSp(10f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = config.publicWebPortalUrl,
                                color = TextPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = rSp(10.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AdaptiveFlatActionButton(
                            onClick = { copyToClipboard("Web URL", config.publicWebPortalUrl) },
                            isPrimary = false,
                            containerColor = XboxSurfaceVariant,
                            contentColor = DirectCyan,
                            borderColor = DirectCyan.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Web Link",
                                tint = DirectCyan,
                                modifier = Modifier.size(rDp(14f))
                            )
                        }
                    }
                }

                // Direct APK Download URL Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(btnCorner))
                        .gamepadClickable(cornerRadius = btnCorner) { qrUseDirectApk = true },
                    shape = RoundedCornerShape(btnCorner),
                    color = if (qrUseDirectApk) XboxNeonGreen.copy(alpha = 0.14f) else XboxSurface,
                    border = BorderStroke(
                        if (qrUseDirectApk) borderFocus else borderThin,
                        if (qrUseDirectApk) XboxNeonGreen else XboxNeonGreen.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = padInner, vertical = gapSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (if (qrUseDirectApk) "✓ " else "") +
                                    config.tr("📥 Link tải trực tiếp file APK", "📥 Direct APK File Link"),
                                color = XboxNeonGreen,
                                fontSize = rSp(10f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = config.customApkDownloadUrl,
                                color = TextPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = rSp(10.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AdaptiveFlatActionButton(
                            onClick = { copyToClipboard("APK Link", config.customApkDownloadUrl) },
                            isPrimary = false,
                            containerColor = XboxSurfaceVariant,
                            contentColor = XboxNeonGreen,
                            borderColor = XboxNeonGreen.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy APK Link",
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(rDp(14f))
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    AdaptiveFlatActionButton(
                        onClick = { sharePortalLink() },
                        isPrimary = true,
                        focusRequester = shareBtnFocusRequester,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_apk_portal_btn")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF04150B),
                            modifier = Modifier.size(rDp(15f))
                        )
                        Spacer(modifier = Modifier.width(rDp(4f)))
                        Text(
                            text = config.tr("Chia sẻ đường dẫn", "Share Link"),
                            color = Color(0xFF04150B),
                            fontSize = rSp(11.5f),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AdaptiveFlatActionButton(
                        onClick = openApkLink,
                        isPrimary = false,
                        containerColor = XboxSurface,
                        contentColor = DirectCyan,
                        borderColor = DirectCyan.copy(alpha = 0.65f),
                        focusedBorderColor = DirectCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = DirectCyan,
                            modifier = Modifier.size(rDp(15f))
                        )
                        Spacer(modifier = Modifier.width(rDp(4f)))
                        Text(
                            text = config.tr("Tải file APK", "Download APK"),
                            color = DirectCyan,
                            fontSize = rSp(11.5f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        @Composable
        fun WebPreviewBox(previewHeight: androidx.compose.ui.unit.Dp) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(RoundedCornerShape(sectionCorner))
                    .border(borderThin, XboxCardBorder, RoundedCornerShape(sectionCorner)),
                color = XboxSurface
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#0B1412"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                private fun handleExternalUri(uri: Uri?): Boolean {
                                    val scheme = uri?.scheme?.lowercase() ?: return false
                                    if (scheme == "http" || scheme == "https") {
                                        try {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                        } catch (_: Exception) {}
                                        return true
                                    }
                                    return false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean = handleExternalUri(request?.url)

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean = handleExternalUri(url?.let { Uri.parse(it) })

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(portalSyncJs, null)
                                }
                            }
                            loadUrl("file:///android_asset/web/index.html")
                        }
                    },
                    update = { webView ->
                        webView.evaluateJavascript(portalSyncJs, null)
                    }
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gapMed)
        ) {
            HeaderSection()
            TabsRow()

            if (selectedTab == 0) {
                val leftScroll = rememberScrollState()
                val rightScroll = rememberScrollState()
                val portraitScroll = rememberScrollState()

                AdaptiveTwoColumnLayout(
                    leftWeight = 0.90f,
                    rightWeight = 1.10f,
                    leftColumn = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = winH * 0.66f)
                                .verticalScroll(leftScroll)
                        ) {
                            QrMatrixPreviewCard(qrSize = (winH * 0.36f).coerceIn(rDp(130f), rDp(190f)))
                        }
                    },
                    rightColumn = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = winH * 0.66f)
                                .verticalScroll(rightScroll)
                        ) {
                            LinksAndShareSection()
                        }
                    },
                    portraitColumn = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = winH * 0.68f)
                                .verticalScroll(portraitScroll),
                            verticalArrangement = Arrangement.spacedBy(gapMed)
                        ) {
                            QrMatrixPreviewCard(qrSize = rDp(185f))
                            LinksAndShareSection()
                        }
                    }
                )
            } else {
                WebPreviewBox(previewHeight = winH * (if (useTwoColumnLayout) 0.66f else 0.68f))
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
