package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val TermsBg = Color(0xFF0D1511)
private val TermsSurface = Color(0xFF142019)
private val TermsCardBg = Color(0xFF142019)
private val TermsLine = Color(0xFF1C2C24)
private val TermsInk = Color(0xFFF3F8F5)
private val TermsMuted = Color(0xFFA2B5AC)
private val TermsAccent = Color(0xFF27D884)
private val TermsOnAccent = Color(0xFF03140B)
private val TermsSoft = Color(0xFF16352A)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UserTermsAgreementDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isDismissible: Boolean = false,
    onDismissRequest: () -> Unit = onDecline
) {
    val context = LocalContext.current
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var agreedToTerms by remember(isDismissible) { mutableStateOf(isDismissible) }
    var showFullInAppReader by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val fullTermsUrl = "https://muitenvn.github.io/CloudPlay-App/terms.html"

    fun openFullTermsInBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullTermsUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                if (isEn) "Could not open browser: $fullTermsUrl" else "Không mở được trình duyệt: $fullTermsUrl",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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

    Dialog(
        onDismissRequest = {
            if (isDismissible) onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .fillMaxWidth(0.96f)
                .heightIn(max = 690.dp)
                .border(1.dp, TermsLine, RoundedCornerShape(20.dp))
                .testTag("user_terms_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = TermsSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with Shield & Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(TermsSoft, RoundedCornerShape(12.dp))
                            .border(1.dp, TermsLine, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TermsAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Terms of Use & Privacy Policy" else "Điều khoản & Quyền riêng tư",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TermsInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEn) {
                                "Version 1.1 • Independent Android Project"
                            } else {
                                "Phiên bản 1.1 • Dự án Android độc lập"
                            },
                            fontSize = 12.sp,
                            color = TermsMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Switcher: Summary (5 points) vs Full 15-Section In-App Reader + External Web Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Quick Summary
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFullInAppReader = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!showFullInAppReader) TermsSoft else TermsBg,
                        border = BorderStroke(1.dp, if (!showFullInAppReader) TermsAccent else TermsLine)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEn) "Quick Summary" else "Tóm tắt chính",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!showFullInAppReader) TermsAccent else TermsMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Tab 2: Read all 15 sections right in the dialog
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFullInAppReader = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (showFullInAppReader) TermsSoft else TermsBg,
                        border = BorderStroke(1.dp, if (showFullInAppReader) TermsAccent else TermsLine)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEn) "Full 15 Sections" else "Đầy đủ 15 mục",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showFullInAppReader) TermsAccent else TermsMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Button to open in external browser (preserves testTag)
                    OutlinedButton(
                        onClick = { openFullTermsInBrowser() },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("open_full_terms_web_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, TermsLine),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TermsBg,
                            contentColor = TermsInk
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = TermsAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "Web" else "Web",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Content Area: Either Summary Cards or Full 15-Section WebView with TOC
                if (showFullInAppReader) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TermsBg)
                            .border(1.dp, TermsLine, RoundedCornerShape(14.dp))
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
                                            if (scheme == "http" || scheme == "https" || scheme == "mailto") {
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
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .background(TermsBg, RoundedCornerShape(14.dp))
                            .border(1.dp, TermsLine, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LegalSection(
                            icon = Icons.Default.Gavel,
                            title = if (isEn) "1. Independent Application" else "1. Ứng dụng độc lập",
                            content = if (isEn) {
                                "CloudPlay is an independent Android browser for xbox.com/play and is NOT affiliated with, owned by, or endorsed by Microsoft, Xbox, or Epic Games."
                            } else {
                                "CloudPlay là trình duyệt chuyên biệt mở xbox.com/play trên Android; KHÔNG liên kết, không thuộc sở hữu và không được bảo trợ bởi Microsoft, Xbox hay Epic Games."
                            }
                        )

                        LegalSection(
                            icon = Icons.Default.Public,
                            title = if (isEn) "2. Optional Proxy & Public Servers" else "2. Proxy tùy chọn & Máy chủ công cộng",
                            content = if (isEn) {
                                "• Optional VPN/proxy routing is disabled by default; you choose when to enable it and are responsible for complying with Microsoft/Xbox terms.\n• Public servers are run by third parties and may vary in speed."
                            } else {
                                "• Tính năng máy chủ trung gian (VPN/Proxy) mặc định tắt; bạn tự quyết định bật và tự chịu trách nhiệm tuân thủ điều khoản của Microsoft/Xbox.\n• Máy chủ công cộng do bên thứ ba vận hành nên tốc độ có thể thay đổi."
                            }
                        )

                        LegalSection(
                            icon = Icons.Default.Lock,
                            title = if (isEn) "3. Account Safety & Local Data" else "3. An toàn tài khoản & Dữ liệu trên máy",
                            content = if (isEn) {
                                "• No password collection: You sign in directly on Microsoft's official pages (login.live.com, xbox.com).\n• Safe checkout: Proxies automatically bypass Microsoft sign-in and billing pages.\n• Settings and play history stay locally on your device. IP geolocation by DB-IP.com (CC BY 4.0)."
                            } else {
                                "• Không thu thập mật khẩu: Bạn đăng nhập trực tiếp trên trang của Microsoft (login.live.com, xbox.com).\n• Bảo vệ thanh toán: Tự động ngắt proxy khi vào trang đăng nhập hoặc thanh toán Microsoft.\n• Cài đặt và lịch sử chơi chỉ lưu trên điện thoại của bạn. Định vị IP bởi DB-IP.com (CC BY 4.0)."
                            }
                        )

                        LegalSection(
                            icon = Icons.Default.SportsEsports,
                            title = if (isEn) "4. Fair Play & Acceptable Use" else "4. Chơi công bằng & Sử dụng đúng mục đích",
                            content = if (isEn) {
                                "CloudPlay only provides touch controls, controller support, and stream stats — NO cheats, NO auto-fire macros, and NO game modification."
                            } else {
                                "CloudPlay chỉ cung cấp tay cầm cảm ứng, hỗ trợ tay cầm rời và thông số truyền phát thực tế — KHÔNG gian lận, KHÔNG macro tự bắn và KHÔNG can thiệp trò chơi."
                            }
                        )

                        LegalSection(
                            icon = Icons.Default.Code,
                            title = if (isEn) "5. Provided As-Is & Developer Contact" else "5. Miễn trừ bảo đảm & Liên hệ",
                            content = if (isEn) {
                                "Provided free of charge 'as is'. The developer (Nguyễn Thế Ngọc — cloudplayxbox@gmail.com) is not liable for network interruptions, account restrictions, or mobile data charges."
                            } else {
                                "Ứng dụng cung cấp miễn phí theo nguyên trạng ('as-is'). Nhà phát triển (Nguyễn Thế Ngọc — cloudplayxbox@gmail.com) không chịu trách nhiệm về gián đoạn mạng, hạn chế tài khoản hay cước dữ liệu di động (4G/5G)."
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mandatory Agreement Checkbox Row (tappable across whole row)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (agreedToTerms) TermsSoft else TermsBg)
                        .border(1.dp, if (agreedToTerms) TermsAccent.copy(alpha = 0.5f) else TermsLine, RoundedCornerShape(10.dp))
                        .clickable { agreedToTerms = !agreedToTerms }
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                        .testTag("terms_checkbox_row")
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TermsAccent,
                            uncheckedColor = TermsMuted,
                            checkmarkColor = TermsOnAccent
                        ),
                        modifier = Modifier.testTag("terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) {
                            "I have read and agree to the Terms of Use & Privacy Policy. (Declining closes the app)."
                        } else {
                            "Tôi đã đọc và đồng ý với Điều khoản sử dụng & Chính sách quyền riêng tư. (Từ chối sẽ đóng ứng dụng)."
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = if (agreedToTerms) TermsInk else TermsMuted,
                        fontWeight = if (agreedToTerms) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("decline_terms_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDismissible) TermsLine else Color(0xFFE05D5D).copy(alpha = 0.65f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TermsBg,
                            contentColor = if (isDismissible) TermsMuted else Color(0xFFFF7A7A)
                        )
                    ) {
                        Text(
                            text = if (isDismissible) {
                                if (isEn) "Close" else "Đóng"
                            } else {
                                if (isEn) "Decline & Exit" else "Từ chối (Thoát)"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onAccept,
                        enabled = agreedToTerms,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("accept_terms_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TermsAccent,
                            contentColor = TermsOnAccent,
                            disabledContainerColor = TermsLine,
                            disabledContentColor = TermsMuted
                        )
                    ) {
                        Text(
                            text = if (isEn) "I Agree & Continue" else "Tôi đồng ý",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TermsCardBg, RoundedCornerShape(10.dp))
            .border(1.dp, TermsLine, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TermsAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TermsInk
            )
        }
        Text(
            text = content,
            fontSize = 12.sp,
            color = TermsMuted,
            lineHeight = 17.sp,
            fontFamily = FontFamily.Default
        )
    }
}
