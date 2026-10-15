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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

private const val EDGE_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0"

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

    DisposableEffect(Unit) {
        onDispose {
            gamepadDetector.release()
        }
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageLoadingProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var guideInitialSection by remember { mutableStateOf(com.example.ui.components.GuideSection.ZERO_LAG) }

    val translationInfo by XboxBridge.translationInfo.collectAsState()
    val networkStatus by com.example.cloudplay.NetworkOptimizer.status.collectAsState()
    val telemetry by com.example.cloudplay.NetworkOptimizer.telemetry.collectAsState()

    LaunchedEffect(config.xcloudTranslateEnabled) {
        webViewInstance?.evaluateJavascript(
            "if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation(${config.xcloudTranslateEnabled});",
            null
        )
    }

    LaunchedEffect(config.cellularMode, config.networkTuningMode, config.wifiSpeedFirstPriority, networkStatus.effectiveBitrateMbps) {
        webViewInstance?.evaluateJavascript(
            "if (window.__updateNetworkProfile) window.__updateNetworkProfile({ cellularMode: '${config.cellularMode.id}', networkTuningMode: '${config.networkTuningMode.id}', speedFirst: ${config.wifiSpeedFirstPriority}, targetBitrate: ${networkStatus.effectiveBitrateMbps} });",
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

    BackHandler {
        onNavigateBackToDashboard()
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

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        allowFileAccess = true
                        allowContentAccess = true
                        setSupportZoom(false)

                        userAgentString = if (config.forceDesktopUserAgent) {
                            EDGE_DESKTOP_UA
                        } else {
                            settings.userAgentString
                        }
                    }

                    val effectiveLocale = config.getEffectiveLocale()

                    CookieManager.getInstance().let { cookieManager ->
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        cookieManager.setCookie("https://www.xbox.com", "MSPC-LOCALE=$effectiveLocale; domain=.xbox.com; path=/")
                    }

                    // Add JavaScript bridge for stream / launch detection & smart translation
                    val bridge = XboxBridge(streamController)
                    bridge.setWebView(this)
                    addJavascriptInterface(bridge, "CloudPlayBridge")

                    val injectionScript = XboxBridge.getInjectionScript(
                        clarityBoost = config.clarityBoostEnabled,
                        clarityPreset = config.clarityPreset.name,
                        targetCountry = vpnState.currentServer.countryCode,
                        targetIp = vpnState.currentServer.ip,
                        userLocale = effectiveLocale,
                        xcloudTranslate = config.xcloudTranslateEnabled,
                        preserveTitles = config.preserveGameTitles,
                        bilingualMode = config.bilingualMode,
                        hideStreamStats = config.hideStreamStatsOverlay,
                        cellularMode = config.cellularMode.id,
                        networkTuningMode = config.networkTuningMode.id,
                        speedFirstPriority = config.wifiSpeedFirstPriority,
                        effectiveBitrateMbps = networkStatus.effectiveBitrateMbps
                    )

                    // Inject document start script if WebKit supports it
                    if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        try {
                            androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                                this,
                                injectionScript,
                                setOf("https://www.xbox.com", "https://xbox.com", "*")
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("CloudBrowser", "Document start script not supported", e)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isPageLoading = true
                            url?.let { streamController.onUrlChanged(it) }
                            // Early injection to intercept Xbox login and region check
                            view?.evaluateJavascript(injectionScript, null)
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            view?.evaluateJavascript(injectionScript, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                            // Re-apply hooks for dynamic single-page-app transitions
                            view?.evaluateJavascript(injectionScript, null)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            request?.url?.toString()?.let { streamController.onUrlChanged(it) }
                            return false
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
                            if (newProgress in 25..80) {
                                view?.evaluateJavascript(injectionScript, null)
                            }
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.grant(request.resources)
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

        val gameProfile = remember(initialUrl) {
            com.example.data.GameOptimizationRepository.getProfileForUrl(initialUrl)
        }

        // Dedicated Game Loading Screen (Optimized for Fortnite & Xbox Cloud with Jet Cruise Animation)
        com.example.ui.components.InGameLoadingOverlay(
            isLoading = isPageLoading && pageLoadingProgress < 0.92f,
            progress = pageLoadingProgress,
            targetServerName = "${vpnState.currentServer.flag} ${vpnState.currentServer.name}",
            hasGamepad = gamepadStatus.hasPhysicalController,
            gameProfile = gameProfile,
            realLatencyMs = if (vpnState.mode == NetworkMode.DIRECT_NETWORK) vpnState.directLatencyMs else vpnState.latencyMs,
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
                            Text("Hủy", fontSize = 11.sp, color = Color.White)
                        }
                    }
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
            onOpenSettings = { showSettingsDialog = true },
            onNavigateHome = onNavigateBackToDashboard,
            onReloadPage = { webViewInstance?.reload() },
            onToggleClarityBoost = { enabled ->
                SettingsRepository.updateClarityBoost(enabled)
                webViewInstance?.evaluateJavascript("if (window.__toggleClarityBoost) window.__toggleClarityBoost($enabled);", null)
            },
            clarityBoostActive = config.clarityBoostEnabled,
            translationActive = config.xcloudTranslateEnabled,
            translatedCount = translationInfo.translatedCount,
            onToggleTranslation = { enabled ->
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
            onSelectCellularMode = { mode -> SettingsRepository.updateCellularMode(mode) },
            onToggleWifiSpeedFirst = { enabled -> SettingsRepository.updateWifiSpeedFirstPriority(enabled) },
            gameProfile = gameProfile,
            onOpenGuide = { section ->
                guideInitialSection = section
                showGuideDialog = true
            }
        )

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onClearCookies = {
                    CookieManager.getInstance().removeAllCookies(null)
                    webViewInstance?.clearCache(true)
                    webViewInstance?.reload()
                    showSettingsDialog = false
                }
            )
        }

        // How To Use Guide Dialog
        if (showGuideDialog) {
            com.example.ui.components.HowToUseGuideDialog(
                initialSection = guideInitialSection,
                onDismiss = { showGuideDialog = false }
            )
        }
    }
}
