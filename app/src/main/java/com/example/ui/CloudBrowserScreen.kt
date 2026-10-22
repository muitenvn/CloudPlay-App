package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cloudplay.BannerType
import com.example.cloudplay.GamepadDetector
import com.example.cloudplay.StreamController
import com.example.cloudplay.XboxBridge
import com.example.data.SettingsRepository
import com.example.ui.components.FloatingHud
import com.example.ui.components.VirtualGamepadOverlay
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.vpn.NetworkMode
import com.example.vpn.VpnManager

private const val EDGE_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudBrowserScreen(
    initialUrl: String,
    streamController: StreamController,
    onNavigateBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vpnState by VpnManager.state.collectAsState()
    val config by SettingsRepository.config.collectAsState()
    val streamUiState by streamController.uiState.collectAsState()

    val gamepadDetector = remember { GamepadDetector(context) }
    val gamepadStatus by gamepadDetector.status.collectAsState()

    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var bridgeInstance by remember { mutableStateOf<XboxBridge?>(null) }
    var pageLoadingProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var showQuickSettingsDialog by remember { mutableStateOf(false) }
    var showTranslatorDialog by remember { mutableStateOf(false) }
    var lastXboxUrl by remember { mutableStateOf(initialUrl) }
    var currentMainFrameUrl by remember { mutableStateOf(initialUrl) }
    var showReturnButton by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        (activity as? com.example.MainActivity)?.applyImmersiveFullscreen(true)
        onDispose {
            gamepadDetector.release()
            bridgeInstance?.release()
            bridgeInstance = null
            streamController.release()
            com.example.cloudplay.QualityAdapter.setWebView(null)
            webViewInstance?.apply {
                stopLoading()
                destroy()
            }
            webViewInstance = null
            (activity as? com.example.MainActivity)?.let { mainAct ->
                mainAct.applyImmersiveFullscreen(false)
                mainAct.resetOrientation()
            }
        }
    }

    val returnToGame: () -> Unit = {
        val webView = webViewInstance
        if (webView != null) {
            val history = webView.copyBackForwardList()
            val currentIndex = history.currentIndex
            var targetOffset: Int? = null

            // Tìm mục gần nhất trong lịch sử duyệt có host thuộc xbox.com
            for (i in (currentIndex - 1) downTo 0) {
                val item = history.getItemAtIndex(i)
                val itemUrl = item?.url
                if (XboxBridge.isXboxHost(itemUrl)) {
                    targetOffset = i - currentIndex
                    break
                }
            }

            if (targetOffset != null && webView.canGoBackOrForward(targetOffset)) {
                webView.goBackOrForward(targetOffset)
            } else {
                val targetUrl = if (lastXboxUrl.isNotBlank()) lastXboxUrl else initialUrl
                webView.loadUrl(targetUrl)
            }
        }
        showReturnButton = false
        VpnManager.restoreProxyForGaming()
    }

    val handleMainFrameNavigation: (String) -> Unit = { targetUrl ->
        val oldUrl = currentMainFrameUrl
        val oldUri = try { android.net.Uri.parse(oldUrl) } catch (_: Exception) { null }
        val newUri = try { android.net.Uri.parse(targetUrl) } catch (_: Exception) { null }
        val oldHost = oldUri?.host?.lowercase()
        val newHost = newUri?.host?.lowercase()

        // Bỏ qua việc hiện nút khi chuyển hướng http -> https cùng tên miền
        val isHttpToHttpsSameDomain = oldHost != null && oldHost == newHost &&
            oldUri?.scheme?.lowercase() == "http" && newUri?.scheme?.lowercase() == "https"

        val isTrustedDomainHost = XboxBridge.isTrustedDomainHost(newHost)
        val isTrusted = XboxBridge.isTrustedHost(targetUrl)

        // Chỉ lưu URL có host thuộc xbox.com (bỏ login.live.com, microsoftonline.com...)
        if (XboxBridge.isXboxHost(targetUrl)) {
            lastXboxUrl = targetUrl
            if (targetUrl.contains("/play/games/") || targetUrl.contains("/play/launch/")) {
                com.example.data.GameLibraryRepository.onWebGamePlayedOrDiscovered(
                    rawTitle = webViewInstance?.title,
                    rawUrl = targetUrl,
                    markAsLastPlayed = true
                )
            }
        }

        if (isTrusted || isTrustedDomainHost) {
            showReturnButton = false
            VpnManager.restoreProxyForGaming()
            // Khi người dùng thoát về trang danh mục Xbox (ngoài /launch/), nối lại bộ dịch nếu đang bật tiếng Việt và không bật Chế độ máy yếu
            if (XboxBridge.isXboxHost(targetUrl) && !targetUrl.contains("/launch/") && !config.lowEndDeviceMode && config.isEffectiveTranslateEnabled()) {
                webViewInstance?.evaluateJavascript(
                    "if (window.__resumeTranslator) window.__resumeTranslator();",
                    null
                )
            }
        } else if (!isHttpToHttpsSameDomain) {
            showReturnButton = true
            VpnManager.pauseProxyForExternalNavigation()
        }

        currentMainFrameUrl = targetUrl
    }

    val translationInfo by XboxBridge.translationInfo.collectAsState()
    val networkStatus by com.example.cloudplay.NetworkOptimizer.status.collectAsState()
    val telemetry by com.example.cloudplay.NetworkOptimizer.telemetry.collectAsState()
    val adaptiveQualityState by com.example.cloudplay.AdaptiveQualityManager.state.collectAsState()

    val effectiveLocale = config.getEffectiveLocale()
    val effectiveTranslate = config.isEffectiveTranslateEnabled()

    // Dynamically recompute injectionScript only when structural settings change (bitrate updates go through __updateNetworkProfile)
    val injectionScript = remember(
        config.clarityBoostEnabled,
        config.clarityPreset,
        vpnState.currentServer,
        effectiveLocale,
        effectiveTranslate,
        config.preserveGameTitles,
        config.bilingualMode,
        config.hideStreamStatsOverlay,
        config.cellularMode,
        config.networkTuningMode,
        config.wifiSpeedFirstPriority,
        config.lowEndDeviceMode,
        config.showVirtualController
    ) {
        XboxBridge.getInjectionScript(
            clarityBoost = config.clarityBoostEnabled,
            clarityPreset = config.clarityPreset.name,
            targetCountry = vpnState.currentServer.countryCode,
            targetIp = vpnState.currentServer.ip,
            userLocale = effectiveLocale,
            xcloudTranslate = effectiveTranslate,
            preserveTitles = config.preserveGameTitles,
            bilingualMode = config.bilingualMode,
            hideStreamStats = config.hideStreamStatsOverlay,
            cellularMode = config.cellularMode.id,
            networkTuningMode = config.networkTuningMode.id,
            speedFirstPriority = config.wifiSpeedFirstPriority,
            effectiveBitrateMbps = if (config.lowEndDeviceMode) 5 else networkStatus.effectiveBitrateMbps,
            lowEndDeviceMode = config.lowEndDeviceMode,
            virtualGamepadEnabled = config.showVirtualController
        )
    }

    val currentInjectionScript by rememberUpdatedState(injectionScript)

    // Dynamically update active WebView when user changes server or locale
    LaunchedEffect(vpnState.currentServer, effectiveLocale) {
        CookieManager.getInstance().setCookie(
            "https://www.xbox.com",
            "MSPC-LOCALE=$effectiveLocale; domain=.xbox.com; path=/"
        )
        val safeCountry = org.json.JSONObject.quote(vpnState.currentServer.countryCode)
        val safeIp = org.json.JSONObject.quote(vpnState.currentServer.ip)
        val safeLocale = org.json.JSONObject.quote(effectiveLocale)
        webViewInstance?.evaluateJavascript(
            "if (window.__updateRegionAndLocale) window.__updateRegionAndLocale({ country: $safeCountry, ip: $safeIp, locale: $safeLocale });",
            null
        )
    }

    LaunchedEffect(effectiveTranslate) {
        webViewInstance?.evaluateJavascript(
            "if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation($effectiveTranslate); if (!$effectiveTranslate && window.__pauseTranslator) window.__pauseTranslator();",
            null
        )
    }

    // 1. DỪNG BỘ DỊCH KHI ĐANG STREAM (hoặc khi bật Chế độ máy yếu / ngôn ngữ tiếng Anh), NỐI LẠI KHI NGẮT STREAM / THOÁT VỀ TRANG XBOX
    LaunchedEffect(streamUiState.isStreamConnected, streamUiState.isGameActive, currentMainFrameUrl, config.lowEndDeviceMode, effectiveTranslate) {
        val wv = webViewInstance ?: return@LaunchedEffect
        val isOnLaunchPage = currentMainFrameUrl.contains("/launch/")
        if (streamUiState.isStreamConnected || config.lowEndDeviceMode || !effectiveTranslate) {
            wv.evaluateJavascript("if (window.__pauseTranslator) window.__pauseTranslator();", null)
        } else if (!streamUiState.isGameActive && !isOnLaunchPage && XboxBridge.isXboxHost(currentMainFrameUrl)) {
            wv.evaluateJavascript("if (window.__resumeTranslator) window.__resumeTranslator();", null)
        }
    }

    LaunchedEffect(config.cellularMode, config.networkTuningMode, config.wifiSpeedFirstPriority, networkStatus.effectiveBitrateMbps, config.lowEndDeviceMode) {
        val targetMbps = if (config.lowEndDeviceMode) 5 else networkStatus.effectiveBitrateMbps
        webViewInstance?.evaluateJavascript(
            "if (window.__updateNetworkProfile) window.__updateNetworkProfile({ cellularMode: '${config.cellularMode.id}', networkTuningMode: '${config.networkTuningMode.id}', speedFirst: ${config.wifiSpeedFirstPriority}, targetBitrate: $targetMbps, lowEndDeviceMode: ${config.lowEndDeviceMode} });",
            null
        )
    }

    LaunchedEffect(config.preserveGameTitles, config.bilingualMode, config.hideStreamStatsOverlay) {
        webViewInstance?.evaluateJavascript(
            "if (window.__setXCloudTranslationConfig) window.__setXCloudTranslationConfig({ preserveTitles: ${config.preserveGameTitles}, bilingual: ${config.bilingualMode}, hideStats: ${config.hideStreamStatsOverlay} });",
            null
        )
        webViewInstance?.evaluateJavascript(
            "if (window.__toggleStreamStatsOverlay) window.__toggleStreamStatsOverlay(${config.hideStreamStatsOverlay});",
            null
        )
    }

    // Đồng bộ tức thì trạng thái Tay cầm ảo với WebView (Tắt tay cầm ảo -> ngắt kết nối Gamepad giả lập để Fortnite hiện HUD Cảm ứng Epic gốc)
    LaunchedEffect(config.showVirtualController, webViewInstance) {
        webViewInstance?.evaluateJavascript(
            "if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled(${config.showVirtualController});",
            null
        )
    }

    BackHandler {
        if (showReturnButton) {
            returnToGame()
        } else {
            onNavigateBackToDashboard()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cloud_browser_screen")
    ) {
        // Main Web View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Hardware Acceleration for Zero-Lag WebRTC Video Decoding
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        allowFileAccess = false
                        allowContentAccess = false
                        setSupportZoom(false)

                        userAgentString = if (config.forceDesktopUserAgent) {
                            EDGE_DESKTOP_UA
                        } else {
                            settings.userAgentString
                        }
                    }

                    CookieManager.getInstance().let { cookieManager ->
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        cookieManager.setCookie("https://www.xbox.com", "MSPC-LOCALE=$effectiveLocale; domain=.xbox.com; path=/")
                    }

                    // Add JavaScript bridge for stream / launch detection & smart translation
                    val bridge = XboxBridge(streamController)
                    bridge.setWebView(this)
                    bridgeInstance = bridge
                    com.example.cloudplay.QualityAdapter.setWebView(this)
                    bridge.updateCurrentUrl(initialUrl)
                    addJavascriptInterface(bridge, "CloudPlayBridge")

                    var hasDocumentStartScript = false
                    // Inject document start script exclusively on authorized Xbox & Microsoft domains
                    if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        try {
                            androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                                this,
                                currentInjectionScript,
                                setOf("https://www.xbox.com", "https://xbox.com", "https://login.live.com", "https://login.microsoftonline.com")
                            )
                            hasDocumentStartScript = true
                        } catch (e: Exception) {
                            android.util.Log.w("CloudBrowser", "Document start script not supported", e)
                        }
                    }

                    var injectedForCurrentPage = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isPageLoading = true
                            injectedForCurrentPage = false
                            bridge.updateCurrentUrl(url)
                            url?.let {
                                streamController.onUrlChanged(it)
                                handleMainFrameNavigation(it)
                            }
                            // Early injection to intercept Xbox login and region check using latest dynamic script
                            if (!hasDocumentStartScript && XboxBridge.isTrustedHost(url)) {
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                            }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            if (!injectedForCurrentPage && XboxBridge.isTrustedHost(url)) {
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                            bridge.updateCurrentUrl(url)
                            url?.let {
                                handleMainFrameNavigation(it)
                            }
                            if (XboxBridge.isTrustedHost(url)) {
                                // Ensure hooks are active on finished page
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val isMainFrame = request?.isForMainFrame == true
                            val targetUrl = request?.url?.toString()
                            if (isMainFrame && targetUrl != null) {
                                bridge.updateCurrentUrl(targetUrl)
                                streamController.onUrlChanged(targetUrl)
                                handleMainFrameNavigation(targetUrl)
                            }
                            return false
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            bridge.updateCurrentUrl(url)
                            url?.let {
                                streamController.onUrlChanged(it)
                                handleMainFrameNavigation(it)
                            }
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                            android.util.Log.w("CloudBrowser", "Render process fallback handled gracefully: didCrash=${detail?.didCrash()}")
                            return true
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                android.util.Log.w("CloudBrowser", "Page error: ${error?.description}")
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            pageLoadingProgress = newProgress / 100f
                            if (!injectedForCurrentPage && newProgress >= 30 && XboxBridge.isTrustedHost(view?.url)) {
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                            }
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            if (request == null) return
                            val originScheme = request.origin?.scheme?.lowercase()
                            val originHost = request.origin?.host?.lowercase() ?: ""
                            val isTrustedOrigin = originScheme == "https" && XboxBridge.isTrustedDomainHost(originHost)
                            if (isTrustedOrigin) {
                                val audioResources = request.resources.filter { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }.toTypedArray()
                                if (audioResources.isNotEmpty()) {
                                    request.grant(audioResources)
                                } else {
                                    request.deny()
                                }
                            } else {
                                request.deny()
                            }
                        }
                    }

                    val formattedInitialUrl = SettingsRepository.formatXboxUrlWithLocale(initialUrl, effectiveLocale)
                    val customHeaders = mapOf(
                        "Accept-Language" to "$effectiveLocale,en;q=0.9",
                        "X-Edge-Shopping-Flag" to "0"
                    )
                    loadUrl(formattedInitialUrl, customHeaders)
                    webViewInstance = this
                }
            },
            update = { view ->
                val desiredUa = if (config.forceDesktopUserAgent) EDGE_DESKTOP_UA else WebSettings.getDefaultUserAgent(context)
                if (view.settings.userAgentString != desiredUa) {
                    view.settings.userAgentString = desiredUa
                }
            }
        )

        val gameProfile = remember(initialUrl, currentMainFrameUrl) {
            val currentProfile = com.example.data.GameOptimizationRepository.getProfileForUrl(currentMainFrameUrl)
            if (currentProfile.gameId != "generic") currentProfile
            else com.example.data.GameOptimizationRepository.getProfileForUrl(initialUrl)
        }

        // Kích hoạt chế độ Fortnite Arena (Đệm 8ms + tự động chọn bố cục FPS Pro khi bật tay cầm ảo lần đầu trong Fortnite)
        LaunchedEffect(gameProfile.gameId, webViewInstance) {
            val isFn = gameProfile.gameId == "fortnite"
            webViewInstance?.evaluateJavascript(
                "window.__fortniteArenaMode = $isFn; if ($isFn && window.__setAdaptiveBitrate) window.__setAdaptiveBitrate(${if (config.lowEndDeviceMode) 5 else networkStatus.effectiveBitrateMbps}, 8.0, false);",
                null
            )
            if (isFn && config.gamepadLayoutPreset == com.example.data.GamepadLayoutPreset.STANDARD) {
                SettingsRepository.updateGamepadLayoutPreset(com.example.data.GamepadLayoutPreset.FPS_PRO)
                SettingsRepository.updateGamepadSprintLockEnabled(true)
            }
        }

        // Dedicated Game Loading Screen (Optimized for Portrait & Landscape)
        com.example.ui.components.InGameLoadingOverlay(
            isLoading = isPageLoading && pageLoadingProgress < 0.92f,
            progress = pageLoadingProgress,
            targetServerName = "${vpnState.currentServer.flag} ${vpnState.currentServer.name}",
            hasGamepad = gamepadStatus.hasPhysicalController,
            gameProfile = gameProfile,
            realLatencyMs = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) vpnState.directLatencyMs else vpnState.latencyMs,
            lowEndDeviceMode = config.lowEndDeviceMode,
            onNavigateHome = onNavigateBackToDashboard,
            onSkip = { isPageLoading = false },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle Top Progress Bar when loading dynamically
        if (isPageLoading && pageLoadingProgress in 0.05f..0.99f) {
            LinearProgressIndicator(
                progress = { pageLoadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = XboxNeonGreen,
                trackColor = Color.Transparent
            )
        }

        // Heads-up Notification Banner (Auto-Switch Countdown / Direct Active)
        AnimatedVisibility(
            visible = streamUiState.showBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp))
                    .testTag("status_banner"),
                color = XboxSurface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (streamUiState.bannerType) {
                            BannerType.COUNTDOWN -> Icons.Default.FlashOn
                            BannerType.SUCCESS -> Icons.Default.CheckCircle
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (streamUiState.bannerType) {
                            BannerType.COUNTDOWN -> DirectCyan
                            BannerType.SUCCESS -> XboxNeonGreen
                            else -> TextPrimary
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = streamUiState.bannerMessage,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    if (streamUiState.bannerType == BannerType.COUNTDOWN) {
                        Button(
                            onClick = { streamController.cancelAutoBypass() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.testTag("cancel_bypass_button")
                        ) {
                            Text(
                                text = if (config.isEnglishUI()) "Cancel" else "Hủy",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Floating "Return to Game" Pill when navigating away to external untrusted websites (e.g. 3D Secure / Payment / External Links)
        AnimatedVisibility(
            visible = showReturnButton,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            val returnLabel = if (config.isEnglishUI()) "Return to Game" else "Quay trở lại trò chơi"
            Surface(
                onClick = { returnToGame() },
                shape = RoundedCornerShape(20.dp),
                color = XboxSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, XboxNeonGreen),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("return_to_game_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = returnLabel,
                        tint = XboxNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = returnLabel,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Virtual Gamepad Overlay (if toggled on)
        if (config.showVirtualController) {
            VirtualGamepadOverlay(
                webView = webViewInstance,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Measure actual <video> top position in WebView ONLY in Portrait mode (0 JS polling overhead in Landscape)
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isPortraitMode = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        var videoTopRatio by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(webViewInstance, streamUiState.isGameActive, isPortraitMode) {
            val wv = webViewInstance ?: return@LaunchedEffect
            if (!isPortraitMode) {
                videoTopRatio = 0f
                return@LaunchedEffect
            }
            var sampleCount = 0
            while (sampleCount < 6) {
                try {
                    wv.evaluateJavascript(
                        "(function(){var v=document.querySelector('video#stream-video,video[data-testid=\"stream-video\"],#segmented-video video,video');var vh=window.innerHeight||1;var vw=window.innerWidth||1;if(!v){return (vh>vw)?(Math.max(0,(vh-(vw*9/16))/2)/vh).toFixed(4):'0';}var r=v.getBoundingClientRect();var aspect=(v.videoWidth>0&&v.videoHeight>0)?(v.videoWidth/v.videoHeight):(16/9);var rh=r.width/aspect;var topPx=r.top;if(r.height>rh+4){topPx=r.top+(r.height-rh)/2;}return Math.max(0,Math.min(0.48,topPx/vh)).toFixed(4);})()"
                    ) { res ->
                        val parsed = res?.trim()?.removeSurrounding("\"")?.toFloatOrNull()
                        if (parsed != null && parsed in 0f..0.48f) {
                            videoTopRatio = parsed
                        }
                    }
                } catch (_: Exception) {}
                sampleCount++
                kotlinx.coroutines.delay(if (videoTopRatio > 0.04f) 5000L else 2000L)
            }
        }

        // In-game Floating HUD Pill (draggable)
        FloatingHud(
            vpnState = vpnState,
            isGameActive = streamUiState.isGameActive,
            virtualControllerEnabled = config.showVirtualController,
            physicalControllerConnected = gamepadStatus.hasPhysicalController,
            controllerName = gamepadStatus.controllerName,
            onToggleNetwork = {
                if (vpnState.mode == NetworkMode.VPN_JAPAN) {
                    streamController.forceSwitchToDirectNetwork()
                } else {
                    VpnManager.connect(context)
                }
            },
            onToggleVirtualController = {
                SettingsRepository.updateShowVirtualController(!config.showVirtualController)
            },
            onOpenSettings = {
                showQuickSettingsDialog = true
            },
            onNavigateHome = onNavigateBackToDashboard,
            onReloadPage = { webViewInstance?.reload() },
            onToggleClarityBoost = { enabled ->
                SettingsRepository.updateClarityBoost(enabled)
                webViewInstance?.evaluateJavascript("if (window.__toggleClarityBoost) window.__toggleClarityBoost($enabled);", null)
            },
            clarityBoostActive = config.clarityBoostEnabled,
            translationActive = effectiveTranslate,
            translatedCount = translationInfo.translatedCount,
            onToggleTranslation = { enabled ->
                if (config.isEnglishUI() && enabled) {
                    SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.VIETNAMESE)
                }
                SettingsRepository.updateXcloudTranslate(enabled)
                webViewInstance?.evaluateJavascript(
                    "if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation($enabled);",
                    null
                )
            },
            hideStreamStats = config.hideStreamStatsOverlay,
            onToggleHideStreamStats = { hide ->
                SettingsRepository.updateHideStreamStatsOverlay(hide)
                webViewInstance?.evaluateJavascript(
                    "if (window.__toggleStreamStatsOverlay) window.__toggleStreamStatsOverlay($hide);",
                    null
                )
            },
            networkStatus = networkStatus,
            telemetry = telemetry,
            adaptiveQualityState = adaptiveQualityState,
            onSelectLadderStep = { stepIdx ->
                com.example.cloudplay.AdaptiveQualityManager.setStepManually(stepIdx)
            },
            onToggleAutoAdapting = { enabled ->
                com.example.cloudplay.AdaptiveQualityManager.toggleAutoAdapting(enabled)
            },
            onSelectCellularMode = { mode -> SettingsRepository.updateCellularMode(mode) },
            onToggleWifiSpeedFirst = { enabled -> SettingsRepository.updateWifiSpeedFirstPriority(enabled) },
            gameProfile = gameProfile,
            onOpenTranslator = {
                showTranslatorDialog = true
            },
            onToggleOrientation = {
                (activity as? com.example.MainActivity)?.toggleOrientation()
            },
            videoTopRatio = videoTopRatio,
            lowEndDeviceMode = config.lowEndDeviceMode,
            onSelectFortniteControlMode = { useVirtualFpsPro ->
                if (useVirtualFpsPro) {
                    SettingsRepository.updateGamepadLayoutPreset(com.example.data.GamepadLayoutPreset.FPS_PRO)
                    SettingsRepository.updateGamepadSprintLockEnabled(true)
                    SettingsRepository.updateShowVirtualController(true)
                    webViewInstance?.evaluateJavascript("if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled(true);", null)
                } else {
                    SettingsRepository.updateShowVirtualController(false)
                    webViewInstance?.evaluateJavascript("if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled(false);", null)
                }
            }
        )

        // Compact In-Game Quick Settings Dialog (Quality Level, Low-End Mode, Translation Toggle)
        if (showQuickSettingsDialog) {
            val isEn = config.isEnglishUI()
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showQuickSettingsDialog = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, XboxCardBorder, RoundedCornerShape(18.dp))
                        .testTag("in_game_quick_settings_dialog"),
                    color = XboxSurface.copy(alpha = 0.98f),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEn) "Quick Settings" else "Cài đặt nhanh",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showQuickSettingsDialog = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isEn) "Close" else "Đóng",
                                    tint = TextPrimary,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                        }

                        // 1. Mức chất lượng
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isEn) "Quality Level" else "Mức chất lượng",
                                color = XboxNeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                com.example.data.CellularOptimizationMode.entries.forEach { mode ->
                                    val selected = config.cellularMode == mode
                                    val shortTitle = when (mode) {
                                        com.example.data.CellularOptimizationMode.AUTO -> if (isEn) "Auto" else "Tự động"
                                        com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> if (isEn) "Low" else "Thấp"
                                        com.example.data.CellularOptimizationMode.MEDIUM -> if (isEn) "Med" else "Vừa"
                                        com.example.data.CellularOptimizationMode.HIGH -> if (isEn) "High" else "Cao"
                                        com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED -> if (isEn) "Ultra" else "Rất cao"
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { SettingsRepository.updateCellularMode(mode) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF101A14),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) XboxNeonGreen else XboxCardBorder
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortTitle,
                                                color = if (selected) XboxNeonGreen else TextPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (isEn) "New level takes effect next session" else "Mức mới có tác dụng ở lần chơi sau",
                                color = DirectCyan,
                                fontSize = 10.sp
                            )
                        }

                        // 2. Chế độ máy yếu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Low-End Device Mode" else "Chế độ máy yếu",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isEn) "Light graphics & cool 60 FPS" else "Đồ họa nhẹ, giữ máy mát 60 FPS",
                                    color = Color(0xFFB0C4B8),
                                    fontSize = 10.sp
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = config.lowEndDeviceMode,
                                onCheckedChange = { SettingsRepository.updateLowEndDeviceMode(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = XboxNeonGreen
                                )
                            )
                        }

                        // 3. Bật/tắt bộ dịch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Auto Translate (Vietnamese)" else "Dịch tự động tiếng Việt",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isEn) "Translate Xbox web menus" else "Dịch menu và thông báo trang Xbox",
                                    color = Color(0xFFB0C4B8),
                                    fontSize = 10.sp
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = effectiveTranslate,
                                onCheckedChange = { enabled ->
                                    if (config.isEnglishUI() && enabled) {
                                        SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.VIETNAMESE)
                                    }
                                    SettingsRepository.updateXcloudTranslate(enabled)
                                    webViewInstance?.evaluateJavascript(
                                        "if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation($enabled);",
                                        null
                                    )
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = DirectCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        // Smart Translator Dialog
        if (showTranslatorDialog) {
            com.example.ui.components.SmartTranslatorDialog(
                onDismissRequest = { showTranslatorDialog = false },
                onForceTranslatePage = {
                    webViewInstance?.evaluateJavascript("if (window.__forceTranslateEntirePage) window.__forceTranslateEntirePage();", null)
                }
            )
        }
    }
}

