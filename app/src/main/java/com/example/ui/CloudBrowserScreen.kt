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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.theme.TextSecondary
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
    var expandInGameRecording by rememberSaveable { mutableStateOf(false) }
    var lastXboxUrl by remember { mutableStateOf(initialUrl) }
    var currentMainFrameUrl by remember { mutableStateOf(initialUrl) }
    var showReturnButton by remember { mutableStateOf(false) }

    val recorderState by com.example.cloudplay.ZeroLagScreenRecorder.state.collectAsState()

    LaunchedEffect(recorderState.isRecording) {
        com.example.audio.BackgroundMusicManager.onScreenRecordingStateChanged(recorderState.isRecording)
    }

    LaunchedEffect(streamUiState.isGameActive, streamUiState.isStreamConnected) {
        com.example.audio.BackgroundMusicManager.onGameStreamStateChanged(
            isGameActive = streamUiState.isGameActive,
            isStreamConnected = streamUiState.isStreamConnected
        )
    }

    DisposableEffect(activity) {
        com.example.audio.BackgroundMusicManager.onScreenChanged(isDashboard = false)
        VpnManager.setInGameStreamActive(true)
        (activity as? com.example.MainActivity)?.applyImmersiveFullscreen(true)
        onDispose {
            VpnManager.setInGameStreamActive(false)
            com.example.cloudplay.ZeroLagScreenRecorder.stopRecording(context)
            com.example.audio.BackgroundMusicManager.onScreenRecordingStateChanged(false)
            com.example.audio.BackgroundMusicManager.onScreenChanged(isDashboard = true)
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
        config.showVirtualController,
        config.streamRenderScale,
        config.streamFpsCap,
        config.gpuLoadReductionEnabled
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
            virtualGamepadEnabled = config.showVirtualController,
            renderScaleFactor = config.streamRenderScale.scaleFactor,
            fpsCap = config.streamFpsCap.fps,
            gpuLoadReduction = config.gpuLoadReductionEnabled
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

    // Đồng bộ tức thì tinh chỉnh đồ họa & giảm tải phần cứng GPU với WebView đang chạy
    LaunchedEffect(
        config.streamRenderScale,
        config.streamFpsCap,
        config.gpuLoadReductionEnabled,
        config.lowEndDeviceMode,
        webViewInstance
    ) {
        webViewInstance?.evaluateJavascript(
            "if (window.__updateGraphicsTuning) window.__updateGraphicsTuning({ renderScale: ${config.streamRenderScale.scaleFactor}, fpsCap: ${config.streamFpsCap.fps}, gpuLoadReduction: ${config.gpuLoadReductionEnabled}, lowEndDeviceMode: ${config.lowEndDeviceMode} });",
            null
        )
    }

    // Đồng bộ tức thì Màu sắc & Độ sáng với WebView đang chạy
    LaunchedEffect(
        config.videoContrast,
        config.videoSaturation,
        config.videoBrightness,
        streamUiState.isStreamConnected,
        webViewInstance
    ) {
        webViewInstance?.evaluateJavascript(
            "if (window.__updateVideoDisplayConfig) window.__updateVideoDisplayConfig({ contrast: ${config.videoContrast}, saturation: ${config.videoSaturation}, brightness: ${config.videoBrightness} });",
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
                object : WebView(ctx) {
                    private var touchEventCountInSec = 0
                    private var lastTouchCountLogMs = 0L
                    private var lastUnbufferedMotionDeviceId = -1

                    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
                        if (event != null) {
                            touchEventCountInSec++
                            val now = android.os.SystemClock.uptimeMillis()
                            if (lastTouchCountLogMs == 0L) {
                                lastTouchCountLogMs = now
                            } else if (now - lastTouchCountLogMs >= 1000L) {
                                android.util.Log.d("CloudBrowserTouch", "onTouchEvent/s: $touchEventCountInSec")
                                touchEventCountInSec = 0
                                lastTouchCountLogMs = now
                            }
                        }
                        return super.onTouchEvent(event)
                    }

                    override fun onGenericMotionEvent(event: android.view.MotionEvent?): Boolean {
                        if (event != null &&
                            ((event.source and android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK ||
                                (event.source and android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD)
                        ) {
                            if (event.deviceId != lastUnbufferedMotionDeviceId) {
                                lastUnbufferedMotionDeviceId = event.deviceId
                                try {
                                    requestUnbufferedDispatch(event)
                                } catch (_: Exception) {}
                            }
                        }
                        return super.onGenericMotionEvent(event)
                    }
                }.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Hardware Acceleration & High Renderer Priority for Zero-Lag WebRTC Video Decoding
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    keepScreenOn = true
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                    }

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
            isLoading = isPageLoading && pageLoadingProgress < 0.80f,
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
            },
            recorderState = recorderState,
            onToggleRecording = {
                com.example.cloudplay.ZeroLagScreenRecorder.toggleRecording(context, webViewInstance)
            }
        )

        // In-Game Video Settings & Stream Capture Dialog (Opened from "🎬 Cài đặt Video" in Expanded HUD)
        if (showQuickSettingsDialog) {
            val isEn = config.isEnglishUI()
            val dialogScrollState = rememberScrollState()
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
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(dialogScrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Quick Stream & Display Settings" else "Tùy chỉnh nhanh Hiển thị & Điều khiển",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isEn) "Adjust resolution, frame rate, controls & image colors" else "Chỉnh nhanh độ phân giải, khung hình, tay cầm và màu sắc",
                                    color = DirectCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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

                        // Hàng thao tác nhanh: Tay cầm ảo & Dịch giao diện
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        val next = !config.showVirtualController
                                        SettingsRepository.updateShowVirtualController(next)
                                        webViewInstance?.evaluateJavascript("if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled($next);", null)
                                    },
                                shape = RoundedCornerShape(9.dp),
                                color = if (config.showVirtualController) XboxNeonGreen.copy(alpha = 0.20f) else Color(0xFF101A14),
                                border = BorderStroke(1.dp, if (config.showVirtualController) XboxNeonGreen else XboxCardBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (config.showVirtualController) {
                                            if (isEn) "🎮 Touch Pad: ON" else "🎮 Tay cầm ảo: Bật"
                                        } else {
                                            if (isEn) "🎮 Touch Pad: OFF" else "🎮 Tay cầm ảo: Tắt"
                                        },
                                        color = if (config.showVirtualController) XboxNeonGreen else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        SettingsRepository.updateXcloudTranslate(!config.xcloudTranslateEnabled)
                                    },
                                shape = RoundedCornerShape(9.dp),
                                color = if (config.xcloudTranslateEnabled) DirectCyan.copy(alpha = 0.20f) else Color(0xFF101A14),
                                border = BorderStroke(1.dp, if (config.xcloudTranslateEnabled) DirectCyan else XboxCardBorder)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (config.xcloudTranslateEnabled) {
                                            if (isEn) "🌐 VI Translate: ON" else "🌐 Dịch Tiếng Việt: Bật"
                                        } else {
                                            if (isEn) "🌐 VI Translate: OFF" else "🌐 Dịch Tiếng Việt: Tắt"
                                        },
                                        color = if (config.xcloudTranslateEnabled) DirectCyan else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // ========================================================
                        // PHẦN 1 (ƯU TIÊN SỐ 1): ĐỘ PHÂN GIẢI, ĐỒ HỌA & KHUNG HÌNH
                        // ========================================================
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E1B14),
                            border = BorderStroke(0.9.dp, XboxNeonGreen.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = if (isEn) "1. Resolution & Graphics (4G/5G/Wi-Fi)" else "1. Độ phân giải & Đồ họa (4G/5G/Wi-Fi)",
                                        color = XboxNeonGreen,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = config.cellularMode.localizedSubtitle(isEn),
                                        color = DirectCyan,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        com.example.data.CellularOptimizationMode.entries.forEach { mode ->
                                            val selected = config.cellularMode == mode
                                            val shortTitle = when (mode) {
                                                com.example.data.CellularOptimizationMode.AUTO -> if (isEn) "Auto" else "Tự động"
                                                com.example.data.CellularOptimizationMode.DATA_SAVER_HIGH_SPEED -> if (isEn) "720p•Low" else "720p•Thấp"
                                                com.example.data.CellularOptimizationMode.MEDIUM -> if (isEn) "900p•Med" else "900p•Vừa"
                                                com.example.data.CellularOptimizationMode.HIGH -> if (isEn) "1080p•High" else "1080p•Cao"
                                                com.example.data.CellularOptimizationMode.MAX_SPEED_UNCAPPED -> if (isEn) "1080p+•Max" else "1080p+•Cao+"
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
                                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = shortTitle,
                                                        color = if (selected) XboxNeonGreen else TextPrimary,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = if (isEn) "Frame Rate (FPS)" else "Khung hình (FPS)",
                                        color = XboxNeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        com.example.data.StreamFpsCap.entries.forEach { cap ->
                                            val selected = config.streamFpsCap == cap
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .clickable { SettingsRepository.updateStreamFpsCap(cap) },
                                                shape = RoundedCornerShape(7.dp),
                                                color = if (selected) XboxNeonGreen.copy(alpha = 0.22f) else Color(0xFF101A14),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) XboxNeonGreen else XboxCardBorder
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${cap.fps} FPS",
                                                        color = if (selected) XboxNeonGreen else TextPrimary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) "Light Background Effects" else "Giảm hiệu ứng nền",
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isEn) "Helps phone run cooler and smoother" else "Giúp máy đỡ nóng và đỡ giật hơn",
                                            color = Color(0xFFB0C4B8),
                                            fontSize = 9.sp
                                        )
                                    }
                                    androidx.compose.material3.Switch(
                                        checked = config.gpuLoadReductionEnabled,
                                        onCheckedChange = { SettingsRepository.updateGpuLoadReductionEnabled(it) },
                                        colors = androidx.compose.material3.SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = DirectCyan
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) "Low-End Device Mode" else "Chế độ máy yếu",
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isEn) "5 Mbps light stream, cool 60 FPS" else "Đồ họa nhẹ 5 Mbps, giữ máy mát",
                                            color = Color(0xFFB0C4B8),
                                            fontSize = 9.sp
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
                            }
                        }

                        // ========================================================
                        // PHẦN 2 (ƯU TIÊN SỐ 2): MÀU SẮC & ĐỘ SÁNG HÌNH ẢNH
                        // ========================================================
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0E1B14),
                            border = BorderStroke(0.9.dp, DirectCyan.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEn) "🎨 2. Image Colors & Brightness" else "🎨 2. Màu sắc & Độ sáng",
                                        color = DirectCyan,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = XboxNeonGreen.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { SettingsRepository.resetVideoDisplayDefaults() }
                                    ) {
                                        Text(
                                            text = if (isEn) "Reset" else "Mặc định",
                                            color = XboxNeonGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isEn) "Contrast" else "Độ tương phản",
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${config.videoContrast}%",
                                            color = XboxNeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    androidx.compose.material3.Slider(
                                        value = config.videoContrast.toFloat(),
                                        onValueChange = { SettingsRepository.updateVideoContrast(it.toInt()) },
                                        valueRange = 80f..140f,
                                        colors = androidx.compose.material3.SliderDefaults.colors(
                                            thumbColor = XboxNeonGreen,
                                            activeTrackColor = XboxNeonGreen
                                        )
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isEn) "Color Saturation" else "Độ tươi màu",
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${config.videoSaturation}%",
                                            color = DirectCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    androidx.compose.material3.Slider(
                                        value = config.videoSaturation.toFloat(),
                                        onValueChange = { SettingsRepository.updateVideoSaturation(it.toInt()) },
                                        valueRange = 80f..150f,
                                        colors = androidx.compose.material3.SliderDefaults.colors(
                                            thumbColor = DirectCyan,
                                            activeTrackColor = DirectCyan
                                        )
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isEn) "Brightness" else "Độ sáng",
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${config.videoBrightness}%",
                                            color = XboxNeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    androidx.compose.material3.Slider(
                                        value = config.videoBrightness.toFloat(),
                                        onValueChange = { SettingsRepository.updateVideoBrightness(it.toInt()) },
                                        valueRange = 80f..130f,
                                        colors = androidx.compose.material3.SliderDefaults.colors(
                                            thumbColor = XboxNeonGreen,
                                            activeTrackColor = XboxNeonGreen
                                        )
                                    )
                                }
                            }
                        }

                        // ========================================================
                        // PHẦN 3 (TÙY CHỌN PHỤ - THU GỌN): QUAY MÀN HÌNH (.MP4)
                        // ========================================================
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0D1611),
                            border = BorderStroke(0.9.dp, if (recorderState.isRecording) Color(0xFFFF3B30) else Color(0x33808080))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandInGameRecording = !expandInGameRecording },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isEn) "🎥 3. Record Screen (.MP4)" else "🎥 3. Quay màn hình (.MP4)",
                                        color = if (recorderState.isRecording) Color(0xFFFF8A80) else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (expandInGameRecording || recorderState.isRecording) "▲ Thu gọn" else "▼ Mở",
                                        color = if (recorderState.isRecording) Color(0xFFFF5252) else DirectCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (expandInGameRecording || recorderState.isRecording) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        com.example.data.ScreenRecordQuality.entries.forEach { q ->
                                            val selected = config.screenRecordQuality == q
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .clickable { SettingsRepository.updateScreenRecordQuality(q) },
                                                shape = RoundedCornerShape(7.dp),
                                                color = if (selected) DirectCyan.copy(alpha = 0.22f) else Color(0xFF0C1510),
                                                border = BorderStroke(1.dp, if (selected) DirectCyan else XboxCardBorder)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = q.localizedShort(isEn),
                                                        color = if (selected) DirectCyan else TextPrimary,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable {
                                                val wasRecording = recorderState.isRecording
                                                if (!wasRecording) {
                                                    showQuickSettingsDialog = false
                                                }
                                                com.example.cloudplay.ZeroLagScreenRecorder.toggleRecording(context, webViewInstance)
                                            }
                                            .testTag("quick_settings_record_button"),
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (recorderState.isRecording) Color(0x33FF3B30) else Color(0x2200E676),
                                        border = BorderStroke(
                                            1.dp,
                                            if (recorderState.isRecording) Color(0xFFFF3B30) else XboxNeonGreen.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (recorderState.isRecording) {
                                                    val mins = recorderState.elapsedSeconds / 60
                                                    val secs = recorderState.elapsedSeconds % 60
                                                    if (isEn) "🔴 Recording %02d:%02d — Tap to Save".format(mins, secs)
                                                    else "🔴 Đang quay %02d:%02d — Chạm để Lưu".format(mins, secs)
                                                } else {
                                                    if (isEn) "🎬 Start Recording"
                                                    else "🎬 Bắt đầu quay"
                                                },
                                                color = if (recorderState.isRecording) Color(0xFFFF6B6B) else Color.White,
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

