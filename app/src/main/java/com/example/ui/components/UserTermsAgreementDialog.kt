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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.SettingsRepository

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
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    var agreedToTerms by remember(isDismissible) { mutableStateOf(isDismissible) }
    var showFullInAppReader by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val fullTermsUrl = "https://muitenvn.github.io/Ravno-App/terms.html"

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

    val defaultTermsFocusRequester = remember { FocusRequester() }
    val acceptFocusRequester = remember { FocusRequester() }
    val declineFocusRequester = remember { FocusRequester() }
    val openWebFocusRequester = remember { FocusRequester() }

    AdaptivePopupFrame(
        onDismissRequest = {
            if (isDismissible) onDismissRequest()
        },
        dialogTestTag = "user_terms_dialog",
        defaultFocusRequester = if (isDismissible) acceptFocusRequester else defaultTermsFocusRequester,
        dismissOnBackPress = isDismissible,
        dismissOnClickOutside = isDismissible,
        wideWidthFraction = 0.95f,
        portraitWidthFraction = 0.94f,
        containerColor = TermsSurface,
        borderColor = TermsLine,
        onPreviousTab = { showFullInAppReader = false },
        onNextTab = { showFullInAppReader = true },
        onBackPress = {
            if (isDismissible) onDismissRequest() else onDecline()
        }
    ) {
        @Composable
        fun HeaderSection() {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gapMed)
            ) {
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .background(TermsSoft, RoundedCornerShape(btnCorner))
                        .border(borderThin, TermsLine, RoundedCornerShape(btnCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TermsAccent,
                        modifier = Modifier.size(iconSize)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) "Terms of Use & Privacy Policy" else "Điều khoản & Quyền riêng tư",
                        fontSize = rSp(15f),
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
                        fontSize = rSp(11f),
                        color = TermsMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        @Composable
        fun ModeSwitcherRow() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = btnMinHeight)
                        .clip(RoundedCornerShape(btnCorner))
                        .gamepadClickable(
                            cornerRadius = btnCorner,
                            onStepHorizontal = { dir -> if (dir > 0) showFullInAppReader = true }
                        ) { showFullInAppReader = false },
                    shape = RoundedCornerShape(btnCorner),
                    color = if (!showFullInAppReader) TermsSoft else TermsBg,
                    border = BorderStroke(borderThin, if (!showFullInAppReader) TermsAccent else TermsLine)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = gapSmall, vertical = gapSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "Quick Summary" else "Tóm tắt chính",
                            fontSize = rSp(11f),
                            fontWeight = FontWeight.Bold,
                            color = if (!showFullInAppReader) TermsAccent else TermsMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = btnMinHeight)
                        .clip(RoundedCornerShape(btnCorner))
                        .gamepadClickable(
                            cornerRadius = btnCorner,
                            onStepHorizontal = { dir -> if (dir < 0) showFullInAppReader = false }
                        ) { showFullInAppReader = true },
                    shape = RoundedCornerShape(btnCorner),
                    color = if (showFullInAppReader) TermsSoft else TermsBg,
                    border = BorderStroke(borderThin, if (showFullInAppReader) TermsAccent else TermsLine)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = gapSmall, vertical = gapSmall),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEn) "Full 15 Sections" else "Đầy đủ 15 mục",
                            fontSize = rSp(11f),
                            fontWeight = FontWeight.Bold,
                            color = if (showFullInAppReader) TermsAccent else TermsMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AdaptiveFlatActionButton(
                    onClick = { openFullTermsInBrowser() },
                    isPrimary = false,
                    containerColor = TermsBg,
                    contentColor = TermsInk,
                    borderColor = TermsLine,
                    focusedBorderColor = TermsAccent,
                    focusRequester = openWebFocusRequester,
                    modifier = Modifier.testTag("open_full_terms_web_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = TermsAccent,
                        modifier = Modifier.size(rDp(13f))
                    )
                    Spacer(modifier = Modifier.width(rDp(3f)))
                    Text(
                        text = "Web",
                        fontSize = rSp(11f),
                        fontWeight = FontWeight.SemiBold,
                        color = TermsInk,
                        maxLines = 1
                    )
                }
            }
        }

        @Composable
        fun TermsBodyContent(maxBodyHeight: Dp) {
            if (showFullInAppReader) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxBodyHeight)
                        .clip(RoundedCornerShape(sectionCorner))
                        .background(TermsBg)
                        .border(borderThin, TermsLine, RoundedCornerShape(sectionCorner))
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
                        .fillMaxWidth()
                        .heightIn(max = maxBodyHeight)
                        .background(TermsBg, RoundedCornerShape(sectionCorner))
                        .border(borderThin, TermsLine, RoundedCornerShape(sectionCorner))
                        .padding(padInner)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    LegalSection(
                        icon = Icons.Default.Gavel,
                        title = if (isEn) "1. Independent Application" else "1. Ứng dụng độc lập",
                        content = if (isEn) {
                            "Ravno App is an independent Android browser for xbox.com/play and is NOT affiliated with, owned by, or endorsed by Microsoft, Xbox, or Epic Games."
                        } else {
                            "Ravno App là trình duyệt chuyên biệt mở xbox.com/play trên Android; KHÔNG liên kết, không thuộc sở hữu và không được bảo trợ bởi Microsoft, Xbox hay Epic Games."
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
                            "Ravno only provides touch controls, controller support, and stream stats — NO cheats, NO auto-fire macros, and NO game modification."
                        } else {
                            "Ravno chỉ cung cấp tay cầm cảm ứng, hỗ trợ tay cầm rời và thông số truyền phát thực tế — KHÔNG gian lận, KHÔNG macro tự bắn và KHÔNG can thiệp trò chơi."
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
        }

        @Composable
        fun AgreementAndActionsSection() {
            Column(verticalArrangement = Arrangement.spacedBy(gapMed)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = btnMinHeight)
                        .clip(RoundedCornerShape(btnCorner))
                        .background(if (agreedToTerms) TermsSoft else TermsBg)
                        .border(
                            borderThin,
                            if (agreedToTerms) TermsAccent.copy(alpha = 0.5f) else TermsLine,
                            RoundedCornerShape(btnCorner)
                        )
                        .gamepadClickable(
                            cornerRadius = btnCorner,
                            focusRequester = if (!isDismissible) defaultTermsFocusRequester else null
                        ) { agreedToTerms = !agreedToTerms }
                        .padding(vertical = rDp(5f), horizontal = rDp(7f))
                        .testTag("terms_checkbox_row")
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = TermsAccent,
                            uncheckedColor = TermsMuted,
                            checkmarkColor = TermsOnAccent
                        ),
                        modifier = Modifier.testTag("terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(gapSmall))
                    Text(
                        text = if (isEn) {
                            "I have read and agree to the Terms of Use & Privacy Policy. (Declining closes the app)."
                        } else {
                            "Tôi đã đọc và đồng ý với Điều khoản sử dụng & Chính sách quyền riêng tư. (Từ chối sẽ đóng ứng dụng)."
                        },
                        fontSize = rSp(11f),
                        lineHeight = rSp(15f),
                        color = if (agreedToTerms) TermsInk else TermsMuted,
                        fontWeight = if (agreedToTerms) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    AdaptiveFlatActionButton(
                        onClick = onDecline,
                        isPrimary = false,
                        gamepadHint = "B",
                        containerColor = TermsBg,
                        contentColor = if (isDismissible) TermsMuted else Color(0xFFFF7A7A),
                        borderColor = if (isDismissible) TermsLine else Color(0xFFE05D5D).copy(alpha = 0.65f),
                        focusedBorderColor = if (isDismissible) TermsAccent else Color(0xFFFF7A7A),
                        focusRequester = declineFocusRequester,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("decline_terms_button")
                    ) {
                        Text(
                            text = if (isDismissible) {
                                if (isEn) "Close" else "Đóng"
                            } else {
                                if (isEn) "Decline & Exit" else "Từ chối (Thoát)"
                            },
                            fontSize = rSp(11.5f),
                            fontWeight = FontWeight.Bold,
                            color = if (isDismissible) TermsMuted else Color(0xFFFF7A7A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AdaptiveFlatActionButton(
                        onClick = onAccept,
                        enabled = agreedToTerms,
                        isPrimary = true,
                        gamepadHint = "A",
                        containerColor = TermsAccent,
                        contentColor = TermsOnAccent,
                        borderColor = TermsAccent,
                        focusedBorderColor = Color.White,
                        focusRequester = acceptFocusRequester,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("accept_terms_button")
                    ) {
                        Text(
                            text = if (isEn) "I Agree & Continue" else "Tôi đồng ý",
                            fontSize = rSp(12f),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (agreedToTerms) TermsOnAccent else TermsMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 1.15f,
            rightWeight = 0.85f,
            dividerColor = TermsLine,
            leftColumn = {
                TermsBodyContent(maxBodyHeight = winH * 0.78f)
            },
            rightColumn = {
                HeaderSection()
                ModeSwitcherRow()
                AgreementAndActionsSection()
            },
            portraitColumn = {
                HeaderSection()
                ModeSwitcherRow()
                TermsBodyContent(maxBodyHeight = winH * 0.48f)
                AgreementAndActionsSection()
            }
        )
    }
}

@Composable
private fun AdaptivePopupScope.LegalSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(btnCorner))
            .background(TermsCardBg, RoundedCornerShape(btnCorner))
            .border(borderThin, TermsLine, RoundedCornerShape(btnCorner))
            .gamepadClickable(cornerRadius = btnCorner) {}
            .padding(padInner),
        verticalArrangement = Arrangement.spacedBy(rDp(3.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gapSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TermsAccent,
                modifier = Modifier.size(rDp(15f))
            )
            Text(
                text = title,
                fontSize = rSp(12f),
                fontWeight = FontWeight.Bold,
                color = TermsInk
            )
        }
        Text(
            text = content,
            fontSize = rSp(11f),
            color = TermsMuted,
            lineHeight = rSp(15.5f),
            fontFamily = FontFamily.Default
        )
    }
}
