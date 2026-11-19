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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    val currentServer by remember {
        VpnManager.state.map { it.currentServer }.distinctUntilChanged()
    }.collectAsState(initial = VpnManager.state.value.currentServer)
    val hasUnlockFailPrompt by remember {
        VpnManager.state.map { it.unlockFailPromptServer != null }.distinctUntilChanged()
    }.collectAsState(initial = VpnManager.state.value.unlockFailPromptServer != null)
    val config by SettingsRepository.config.collectAsState()
    val streamUiState by remember(streamController) {
        streamController.uiState.distinctUntilChanged { old, new ->
            old.isGameActive == new.isGameActive &&
                old.isStreamConnected == new.isStreamConnected &&
                old.gameTitle == new.gameTitle &&
                old.currentUrl == new.currentUrl &&
                old.shortcutVirtualPadOverrideTick == new.shortcutVirtualPadOverrideTick &&
                old.shortcutToggleRecordingTick == new.shortcutToggleRecordingTick
        }
    }.collectAsState(initial = streamController.uiState.value)

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
    var webViewSessionKey by remember { mutableStateOf(0) }
    var activeSessionInitialUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var exitPreviewArmedAtMs by remember { mutableStateOf(0L) }
    var exitPreviewOptOutStartedAtMs by remember { mutableStateOf(0L) }
    var exitPreviewSavedGameUrl by remember { mutableStateOf("") }
    var exitPreviewSavedGameTitle by remember { mutableStateOf("") }
    var lastVisitedGameUrl by remember(initialUrl) {
        mutableStateOf(
            if (initialUrl.contains("/play/games/", ignoreCase = true) ||
                initialUrl.contains("/products/", ignoreCase = true) ||
                initialUrl.contains("/games/", ignoreCase = true) ||
                initialUrl.contains("/launch/", ignoreCase = true)
            ) initialUrl else ""
        )
    }
    var suppressLoadingOverlayAfterExitPreview by remember { mutableStateOf(false) }
    var bounceTrackedGameKey by remember { mutableStateOf("") }
    var consecutiveClassicBounceCount by remember { mutableStateOf(0) }
    var classicBounceLoopStopped by remember { mutableStateOf(false) }
    var isClassicBounceBannerDismissed by remember { mutableStateOf(false) }
    var lastSeenWebMarketLocale by remember { mutableStateOf("") }
    var dismissedAtWebMarket by remember { mutableStateOf("") }
    var isAwaitingOrOnClassicSurface by remember { mutableStateOf(false) }
    var lastClassicSwitchElapsedMs by remember { mutableStateOf(0L) }

    val extractXboxUrlMarketLocale: (String) -> String = { rawUrl ->
        try {
            val u = android.net.Uri.parse(rawUrl)
            val seg0 = u.pathSegments?.firstOrNull().orEmpty()
            val localeRegex = Regex("^[a-zA-Z]{2}-[a-zA-Z]{2}$")
            when {
                localeRegex.matches(seg0) -> seg0.lowercase()
                else -> {
                    val qMkt = (u.getQueryParameter("mkt") ?: u.getQueryParameter("locale") ?: u.getQueryParameter("hl")).orEmpty()
                    if (Regex("^[a-zA-Z]{2}(-[a-zA-Z]{2})?$").matches(qMkt)) qMkt.lowercase() else ""
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    val resolveBounceGameIdentity: (List<String>) -> String = { candidates ->
        var found = ""
        for (raw in candidates) {
            if (raw.isBlank()) continue
            val pid = com.example.data.GameLibraryRepository.extractProductId(raw)
            if (pid.isNotBlank()) {
                found = pid.lowercase()
                break
            }
            val slug = Regex("/play/(?:games|launch)/([^/?#]+)", RegexOption.IGNORE_CASE)
                .find(raw)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
            if (slug.isNotBlank()) {
                found = slug
                break
            }
            val prodSlug = Regex("/products/([^/?#]+)", RegexOption.IGNORE_CASE)
                .find(raw)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
            if (prodSlug.isNotBlank()) {
                found = prodSlug
                break
            }
        }
        if (found.isBlank()) {
            found = streamController.uiState.value.gameTitle?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "xbox_catalog"
        }
        found
    }

    LaunchedEffect(streamUiState.currentUrl) {
        val u = streamUiState.currentUrl
        if (u.contains("/play/games/", ignoreCase = true) ||
            u.contains("/products/", ignoreCase = true) ||
            u.contains("/games/", ignoreCase = true) ||
            u.contains("/launch/", ignoreCase = true)
        ) {
            lastVisitedGameUrl = u
        }
    }

    DisposableEffect(showQuickSettingsDialog, showTranslatorDialog) {
        com.example.ui.components.GamepadUiNavigation.shouldRouteToAppUi =
            showQuickSettingsDialog || showTranslatorDialog
        onDispose {
            com.example.ui.components.GamepadUiNavigation.shouldRouteToAppUi = true
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewInstance, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    try {
                        webViewInstance?.evaluateJavascript(
                            "if (window.__setAppInBackground) window.__setAppInBackground(true);",
                            null
                        )
                        webViewInstance?.onPause()
                        webViewInstance?.pauseTimers()
                    } catch (_: Exception) {}
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    try {
                        webViewInstance?.onResume()
                        webViewInstance?.resumeTimers()
                        webViewInstance?.evaluateJavascript(
                            "if (window.__setAppInBackground) window.__setAppInBackground(false);",
                            null
                        )
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        val thermalListener = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && powerManager != null) {
            val l = android.os.PowerManager.OnThermalStatusChangedListener { status ->
                val isHot = status >= android.os.PowerManager.THERMAL_STATUS_MODERATE
                try {
                    webViewInstance?.evaluateJavascript(
                        "if (window.__setDeviceThermalThrottled) window.__setDeviceThermalThrottled($isHot);",
                        null
                    )
                } catch (_: Exception) {}
            }
            try {
                powerManager.addThermalStatusListener(l)
            } catch (_: Exception) {}
            l
        } else null

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
                try {
                    powerManager.removeThermalStatusListener(thermalListener)
                } catch (_: Exception) {}
            }
        }
    }

    DisposableEffect(activity) {
        VpnManager.setInGameStreamActive(true)
        (activity as? com.example.MainActivity)?.applyImmersiveFullscreen(true)
        onDispose {
            VpnManager.setInGameStreamActive(false)
            com.example.cloudplay.ZeroLagScreenRecorder.stopRecording(context)
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

    var pendingAppNavReason by remember { mutableStateOf<String?>(null) }

    val returnToGame: () -> Unit = {
        val webView = webViewInstance
        if (webView != null) {
            val history = webView.copyBackForwardList()
            val currentIndex = history.currentIndex
            var targetOffset: Int? = null
            var historyTargetUrl: String? = null

            // Tìm mục gần nhất trong lịch sử duyệt có host thuộc xbox.com
            for (i in (currentIndex - 1) downTo 0) {
                val item = history.getItemAtIndex(i)
                val itemUrl = item?.url
                if (XboxBridge.isXboxHost(itemUrl)) {
                    targetOffset = i - currentIndex
                    historyTargetUrl = itemUrl
                    break
                }
            }

            if (targetOffset != null && webView.canGoBackOrForward(targetOffset)) {
                pendingAppNavReason = "bấm Quay trở lại trò chơi (lùi lịch sử $targetOffset bước về ${com.example.data.AppLogger.safeUrlHostPath(historyTargetUrl)})"
                com.example.data.AppLogger.info(
                    "RESET",
                    "Quay trở lại trò chơi qua lịch sử WebView: offset=$targetOffset, đích=${com.example.data.AppLogger.safeUrlHostPath(historyTargetUrl)} | chế độ=${config.xboxWebUiMode.name}"
                )
                webView.goBackOrForward(targetOffset)
            } else {
                val targetUrl = if (lastXboxUrl.isNotBlank()) lastXboxUrl else initialUrl
                pendingAppNavReason = "bấm Quay trở lại trò chơi (tải lại lastXboxUrl/initialUrl)"
                com.example.data.AppLogger.info(
                    "RESET",
                    "Quay trở lại trò chơi bằng loadUrl: đích=${com.example.data.AppLogger.safeUrlHostPath(targetUrl)} | chế độ=${config.xboxWebUiMode.name} | nguồn=${if (lastXboxUrl.isNotBlank()) "lastXboxUrl" else "initialUrl"}"
                )
                webView.loadUrl(targetUrl)
            }
        }
        showReturnButton = false
        VpnManager.restoreProxyForGaming()
    }

    val isCellular by remember {
        com.example.cloudplay.NetworkOptimizer.status.map { it.isCellular }.distinctUntilChanged()
    }.collectAsState(initial = com.example.cloudplay.NetworkOptimizer.status.value.isCellular)
    val effectiveBitrateMbps by remember {
        com.example.cloudplay.NetworkOptimizer.status.map { it.effectiveBitrateMbps }.distinctUntilChanged()
    }.collectAsState(initial = com.example.cloudplay.NetworkOptimizer.status.value.effectiveBitrateMbps)

    val effectiveLocale = config.getEffectiveLocale()
    val effectiveTranslate = config.isEffectiveTranslateEnabled()
    var lastAppliedUiMode by remember { mutableStateOf(config.xboxWebUiMode) }

    val resolveTargetGameClassicUrl: () -> String = {
        val curUrl = webViewInstance?.url ?: currentMainFrameUrl
        val liveStreamUrl = streamController.uiState.value.currentUrl
        val bestTitleHint = exitPreviewSavedGameTitle
            .ifBlank { streamController.uiState.value.gameTitle.orEmpty() }
            .ifBlank { webViewInstance?.title.orEmpty() }

        val candidateUrls = listOf(
            exitPreviewSavedGameUrl,
            lastVisitedGameUrl,
            liveStreamUrl,
            curUrl,
            lastXboxUrl,
            initialUrl
        )
        var resolvedGameClassicUrl: String? = null
        for (candidate in candidateUrls) {
            if (candidate.isBlank()) continue
            val directResolved = com.example.data.GameLibraryRepository.resolveClassicGameUrl(
                rawUrl = candidate,
                fallbackTitle = bestTitleHint,
                locale = effectiveLocale
            )
            if (directResolved != null && directResolved.contains("/play/games/", ignoreCase = true)) {
                resolvedGameClassicUrl = directResolved
                break
            }
            val formatted = SettingsRepository.formatXboxUrlWithLocale(
                candidate,
                effectiveLocale,
                com.example.data.XboxWebUiMode.CLASSIC
            )
            if (formatted.contains("/play/games/", ignoreCase = true)) {
                resolvedGameClassicUrl = formatted
                break
            }
        }
        if (resolvedGameClassicUrl == null && bestTitleHint.isNotBlank()) {
            resolvedGameClassicUrl = com.example.data.GameLibraryRepository.resolveClassicGameUrl(
                rawUrl = "",
                fallbackTitle = bestTitleHint,
                locale = effectiveLocale
            )
        }
        if (resolvedGameClassicUrl == null) {
            val lastPlayedId = com.example.data.GameLibraryRepository.lastPlayedId.value
            val lastPlayedBookmark = if (!lastPlayedId.isNullOrBlank()) {
                com.example.data.GameLibraryRepository.games.value.firstOrNull {
                    it.id.equals(lastPlayedId, ignoreCase = true)
                }
            } else null
            if (lastPlayedBookmark != null) {
                val formattedLastPlayed = SettingsRepository.formatXboxUrlWithLocale(
                    lastPlayedBookmark.playUrl,
                    effectiveLocale,
                    com.example.data.XboxWebUiMode.CLASSIC
                )
                if (formattedLastPlayed.contains("/play/games/", ignoreCase = true)) {
                    resolvedGameClassicUrl = formattedLastPlayed
                }
            }
        }
        resolvedGameClassicUrl ?: "https://www.xbox.com/$effectiveLocale/play/games/fortnite/BT5P2X999VH2"
    }

    val applyClassicPreviewOptOutCookies: () -> Unit = {
        try {
            val cm = CookieManager.getInstance()
            val domains = listOf("https://www.xbox.com", "https://play.xbox.com", "https://xbox.com")
            for (domainUrl in domains) {
                cm.setCookie(domainUrl, "TakahashiOptOut=true; domain=.xbox.com; path=/; max-age=31536000; SameSite=Lax")
                cm.setCookie(domainUrl, "xcloud_preview_optout=true; domain=.xbox.com; path=/; max-age=31536000; SameSite=Lax")
            }
            cm.flush()
        } catch (_: Exception) {}
    }

    val handleMainFrameNavigation: (String, String) -> Unit = nav@{ targetUrl, callbackSource ->
        val oldUrl = currentMainFrameUrl
        val oldSafePath = com.example.data.AppLogger.safeUrlHostPath(oldUrl)
        val newSafePath = com.example.data.AppLogger.safeUrlHostPath(targetUrl)
        val oldUri = try { android.net.Uri.parse(oldUrl) } catch (_: Exception) { null }
        val newUri = try { android.net.Uri.parse(targetUrl) } catch (_: Exception) { null }
        val oldHost = oldUri?.host?.lowercase()
        val newHost = newUri?.host?.lowercase()

        if (newSafePath.isNotBlank() && oldSafePath != newSafePath) {
            val actualSurface = when (newHost) {
                "play.xbox.com" -> "NEW_UI (play.xbox.com)"
                "www.xbox.com", "xbox.com" -> "CLASSIC (www.xbox.com)"
                else -> "OTHER (${newHost ?: "unknown"})"
            }
            val uaMode = if (config.forceDesktopUserAgent) "desktop" else "mobile"
            val appReason = pendingAppNavReason
            val decisionSource = when {
                oldHost in listOf("www.xbox.com", "xbox.com") && newHost == "play.xbox.com" &&
                    config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC -> {
                    "Xbox tự chuyển hướng máy chủ/JS (www.xbox.com -> play.xbox.com dù setting=CLASSIC; sự kiện=$callbackSource)"
                }
                oldHost == "play.xbox.com" && newHost in listOf("www.xbox.com", "xbox.com") -> {
                    if (config.xboxWebUiMode != com.example.data.XboxWebUiMode.CLASSIC) {
                        SettingsRepository.updateXboxWebUiMode(com.example.data.XboxWebUiMode.CLASSIC)
                    }
                    "Chuyển từ play.xbox.com về www.xbox.com (${appReason ?: "Xbox Exit preview / chuyển hướng"}; sự kiện=$callbackSource)"
                }
                appReason != null -> {
                    "App điều hướng ($appReason; sự kiện=$callbackSource)"
                }
                else -> {
                    "Điều hướng web ($callbackSource)"
                }
            }
            pendingAppNavReason = null
            com.example.data.AppLogger.info(
                "WEB-UI",
                "Điều hướng chính: ${oldSafePath.ifBlank { "(khởi tạo)" }} -> $newSafePath | cài đặt=${config.xboxWebUiMode.name} | ua=$uaMode | thực tế=$actualSurface | nguồn=$decisionSource"
            )
        }

        // Bỏ qua việc hiện nút khi chuyển hướng http -> https cùng tên miền
        val isHttpToHttpsSameDomain = oldHost != null && oldHost == newHost &&
            oldUri?.scheme?.lowercase() == "http" && newUri?.scheme?.lowercase() == "https"

        val isTrustedDomainHost = XboxBridge.isTrustedDomainHost(newHost)
        val isTrusted = XboxBridge.isTrustedHost(targetUrl)

        // Chỉ lưu URL có host thuộc xbox.com (bỏ login.live.com, microsoftonline.com...)
        if (XboxBridge.isXboxHost(targetUrl)) {
            val detectedMkt = extractXboxUrlMarketLocale(targetUrl)
            if (detectedMkt.isNotBlank() && detectedMkt != lastSeenWebMarketLocale) {
                val prevMkt = lastSeenWebMarketLocale
                lastSeenWebMarketLocale = detectedMkt
                if (classicBounceLoopStopped && isClassicBounceBannerDismissed &&
                    prevMkt.isNotBlank() && !detectedMkt.equals(dismissedAtWebMarket, ignoreCase = true)
                ) {
                    isClassicBounceBannerDismissed = false
                    dismissedAtWebMarket = detectedMkt
                    com.example.data.AppLogger.info(
                        "WEB-UI",
                        "Xbox đổi sang thị trường khác ($prevMkt -> $detectedMkt) sau khi tắt nhắc nhở -> hiện lại thanh nhắc giao diện mới"
                    )
                }
            }

            if (newHost == "www.xbox.com" || newHost == "xbox.com") {
                isAwaitingOrOnClassicSurface = true
                if (targetUrl.contains("/launch/", ignoreCase = true)) {
                    consecutiveClassicBounceCount = 0
                    classicBounceLoopStopped = false
                    isClassicBounceBannerDismissed = false
                }
            }

            val prevLastSafe = com.example.data.AppLogger.safeUrlHostPath(lastXboxUrl)
            val isUnwantedPreviewRedirect = newHost == "play.xbox.com" &&
                config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
                !targetUrl.contains("/launch/")
            if (!isUnwantedPreviewRedirect) {
                lastXboxUrl = targetUrl
            } else if (prevLastSafe != newSafePath) {
                val uaMode = if (config.forceDesktopUserAgent) "desktop" else "mobile"
                com.example.data.AppLogger.info(
                    "WEB-UI",
                    "Giữ nguyên lastXboxUrl ở CLASSIC ($prevLastSafe), bỏ qua ghi đè từ redirect giao diện mới: $newSafePath (ua=$uaMode)"
                )
            }
            val isLaunchUrl = targetUrl.contains("/play/launch/", ignoreCase = true) ||
                targetUrl.contains("/launch/", ignoreCase = true)
            val isGameInfoUrl = targetUrl.contains("/play/games/", ignoreCase = true) ||
                targetUrl.contains("/products/", ignoreCase = true) ||
                targetUrl.contains("/games/", ignoreCase = true)
            if (isLaunchUrl || isGameInfoUrl) {
                lastVisitedGameUrl = targetUrl
                com.example.data.GameLibraryRepository.onWebGamePlayedOrDiscovered(
                    rawTitle = webViewInstance?.title,
                    rawUrl = targetUrl,
                    markAsLastPlayed = isLaunchUrl
                )
            }

            val isPushbackToNewUi = newHost == "play.xbox.com" &&
                config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
                !isLaunchUrl &&
                (oldHost == "www.xbox.com" || oldHost == "xbox.com" || isAwaitingOrOnClassicSurface)
            if (isPushbackToNewUi) {
                isAwaitingOrOnClassicSurface = false
                val nowElapsed = android.os.SystemClock.elapsedRealtime()
                val gameKey = resolveBounceGameIdentity(
                    listOf(oldUrl, targetUrl, lastVisitedGameUrl, exitPreviewSavedGameUrl, initialUrl)
                )
                if (gameKey == bounceTrackedGameKey) {
                    consecutiveClassicBounceCount += 1
                } else {
                    bounceTrackedGameKey = gameKey
                    consecutiveClassicBounceCount = 1
                    classicBounceLoopStopped = false
                }
                if (consecutiveClassicBounceCount >= 2) {
                    classicBounceLoopStopped = true
                    isClassicBounceBannerDismissed = false
                    dismissedAtWebMarket = lastSeenWebMarketLocale
                    exitPreviewArmedAtMs = 0L
                    exitPreviewOptOutStartedAtMs = 0L
                    webViewInstance?.evaluateJavascript(
                        "if (window.__setAutoExitPreviewEnabled) window.__setAutoExitPreviewEnabled(false);",
                        null
                    )
                    com.example.data.AppLogger.warn(
                        "WEB-UI",
                        "Xbox đẩy ngược về play.xbox.com lần thứ $consecutiveClassicBounceCount liên tiếp (game=$gameKey) -> DỪNG tự thử lại và hiện nhắc nhở"
                    )
                } else if (config.autoExitPreviewHandlerEnabled &&
                    !classicBounceLoopStopped &&
                    lastClassicSwitchElapsedMs > 0L &&
                    (nowElapsed - lastClassicSwitchElapsedMs) <= 6_000L
                ) {
                    applyClassicPreviewOptOutCookies()
                    val retryClassicUrl = resolveTargetGameClassicUrl()
                    isAwaitingOrOnClassicSurface = true
                    lastClassicSwitchElapsedMs = nowElapsed
                    pendingAppNavReason = "thử lại lần 2 sau khi Xbox đẩy ngược về play.xbox.com (game=$gameKey)"
                    lastXboxUrl = retryClassicUrl
                    currentMainFrameUrl = retryClassicUrl
                    com.example.data.AppLogger.info(
                        "WEB-UI",
                        "Xbox đẩy ngược về play.xbox.com lần 1 (game=$gameKey) -> thử chuyển lại Giao diện cũ thêm 1 lần: ${com.example.data.AppLogger.safeUrlHostPath(retryClassicUrl)}"
                    )
                    webViewInstance?.post {
                        if (!classicBounceLoopStopped) {
                            webViewInstance?.loadUrl(retryClassicUrl)
                        }
                    }
                    return@nav
                }
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

    val switchWebViewToClassicUi: (String) -> Unit = { reason ->
        if (config.xboxWebUiMode != com.example.data.XboxWebUiMode.CLASSIC) {
            lastAppliedUiMode = com.example.data.XboxWebUiMode.CLASSIC
            SettingsRepository.updateXboxWebUiMode(com.example.data.XboxWebUiMode.CLASSIC)
        }
        applyClassicPreviewOptOutCookies()
        val targetClassicUrl = resolveTargetGameClassicUrl()
        val gameKey = resolveBounceGameIdentity(
            listOf(targetClassicUrl, exitPreviewSavedGameUrl, lastVisitedGameUrl, currentMainFrameUrl, initialUrl)
        )
        if (gameKey != bounceTrackedGameKey) {
            bounceTrackedGameKey = gameKey
            consecutiveClassicBounceCount = 0
            classicBounceLoopStopped = false
            isClassicBounceBannerDismissed = false
        }
        isAwaitingOrOnClassicSurface = true
        lastClassicSwitchElapsedMs = android.os.SystemClock.elapsedRealtime()
        exitPreviewArmedAtMs = 0L
        exitPreviewOptOutStartedAtMs = 0L
        exitPreviewSavedGameUrl = ""
        exitPreviewSavedGameTitle = ""
        pendingAppNavReason = reason
        lastXboxUrl = targetClassicUrl
        currentMainFrameUrl = targetClassicUrl
        com.example.data.AppLogger.info(
            "WEB-UI",
            "Chuyển từ Giao diện mới sang Giao diện cũ ($reason): đích=${com.example.data.AppLogger.safeUrlHostPath(targetClassicUrl)}"
        )
        val wv = webViewInstance
        if (wv != null) {
            val safeTargetJs = org.json.JSONObject.quote(targetClassicUrl)
            wv.evaluateJavascript(
                "if (window.__prepareSwitchToClassicUi) { window.__prepareSwitchToClassicUi($safeTargetJs); 'JS_OK'; } else { 'FALLBACK'; }"
            ) { res ->
                if (res == null || !res.contains("JS_OK")) {
                    wv.loadUrl(targetClassicUrl)
                }
            }
        }
    }

    val commitExitPreviewToClassicUi: (String) -> Unit = commit@{ triggerReason ->
        if (!config.autoExitPreviewHandlerEnabled ||
            config.xboxWebUiMode != com.example.data.XboxWebUiMode.CLASSIC ||
            classicBounceLoopStopped
        ) {
            exitPreviewArmedAtMs = 0L
            exitPreviewOptOutStartedAtMs = 0L
            return@commit
        }
        if (exitPreviewArmedAtMs == 0L && exitPreviewOptOutStartedAtMs == 0L) return@commit
        exitPreviewArmedAtMs = 0L
        exitPreviewOptOutStartedAtMs = 0L

        applyClassicPreviewOptOutCookies()

        // Mở thẳng đúng trang chi tiết của tựa game người dùng đang xem ở Giao diện cũ sau khi opt-out
        val targetClassicUrl = resolveTargetGameClassicUrl()
        val gameKey = resolveBounceGameIdentity(
            listOf(targetClassicUrl, exitPreviewSavedGameUrl, lastVisitedGameUrl, currentMainFrameUrl, initialUrl)
        )
        if (gameKey != bounceTrackedGameKey) {
            bounceTrackedGameKey = gameKey
            consecutiveClassicBounceCount = 0
            classicBounceLoopStopped = false
            isClassicBounceBannerDismissed = false
        }
        isAwaitingOrOnClassicSurface = true
        lastClassicSwitchElapsedMs = android.os.SystemClock.elapsedRealtime()
        exitPreviewSavedGameUrl = ""
        exitPreviewSavedGameTitle = ""

        com.example.data.AppLogger.info(
            "WEB-UI",
            "Tự động cắt phiên play.xbox.com & chuyển sang Giao diện cũ: đích=${com.example.data.AppLogger.safeUrlHostPath(targetClassicUrl)} | nguồn=$triggerReason | autoExit=${config.autoExitPreviewHandlerEnabled}"
        )

        try {
            bridgeInstance?.release()
            bridgeInstance = null
            com.example.cloudplay.QualityAdapter.setWebView(null)
            webViewInstance?.apply {
                stopLoading()
                destroy()
            }
            webViewInstance = null
        } catch (_: Exception) {}

        lastXboxUrl = targetClassicUrl
        currentMainFrameUrl = targetClassicUrl
        activeSessionInitialUrl = targetClassicUrl
        suppressLoadingOverlayAfterExitPreview = true
        pendingAppNavReason = "chuyển sang Giao diện cũ sau Exit preview ($triggerReason)"
        webViewSessionKey += 1
    }

    LaunchedEffect(config.xboxWebUiMode) {
        if (config.xboxWebUiMode == lastAppliedUiMode) return@LaunchedEffect
        val prevMode = lastAppliedUiMode
        lastAppliedUiMode = config.xboxWebUiMode
        val wv = webViewInstance ?: return@LaunchedEffect
        val curUrl = wv.url ?: currentMainFrameUrl
        val uaMode = if (config.forceDesktopUserAgent) "desktop" else "mobile"
        if (!streamUiState.isStreamConnected && !curUrl.contains("/launch/") && XboxBridge.isXboxHost(curUrl)) {
            if (config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC) {
                switchWebViewToClassicUi("LaunchedEffect đổi cài đặt giao diện (${prevMode.name} -> CLASSIC)")
            } else {
                val adapted = SettingsRepository.formatXboxUrlWithLocale(
                    curUrl,
                    effectiveLocale,
                    config.xboxWebUiMode
                )
                com.example.data.AppLogger.info(
                    "WEB-UI",
                    "LaunchedEffect(xboxWebUiMode): ${prevMode.name} -> ${config.xboxWebUiMode.name} | ua=$uaMode | cur=${com.example.data.AppLogger.safeUrlHostPath(curUrl)} -> adapted=${com.example.data.AppLogger.safeUrlHostPath(adapted)}"
                )
                if (adapted.isNotBlank() && !adapted.equals(curUrl, ignoreCase = true)) {
                    pendingAppNavReason = "LaunchedEffect đổi cài đặt giao diện (${prevMode.name} -> ${config.xboxWebUiMode.name})"
                    wv.loadUrl(adapted)
                }
            }
        }
    }

    var manualVirtualPadOverrideWithPhys by remember { mutableStateOf(false) }
    LaunchedEffect(gamepadStatus.hasPhysicalController) {
        manualVirtualPadOverrideWithPhys = false
    }
    LaunchedEffect(streamUiState.shortcutVirtualPadOverrideTick) {
        if (streamUiState.shortcutVirtualPadOverrideTick > 0L && gamepadStatus.hasPhysicalController) {
            manualVirtualPadOverrideWithPhys = !manualVirtualPadOverrideWithPhys
        }
    }
    LaunchedEffect(streamUiState.shortcutToggleRecordingTick) {
        if (streamUiState.shortcutToggleRecordingTick > 0L) {
            com.example.cloudplay.ZeroLagScreenRecorder.toggleRecording(context, webViewInstance)
        }
    }

    val isTvMode = remember(config.appUiMode, context) { config.isEffectiveTvMode(context) }
    val remotePadServerState by com.example.cloudplay.RemotePadTvServer.state.collectAsState()
    val isRemotePadConnected = remotePadServerState.status == com.muitenvn.cloudplay.protocol.TvServiceStatus.HAS_CONTROLLER
    val effectiveShowVirtualController = !isTvMode && config.showVirtualController &&
        (!config.autoHideVirtualOnPhysicalController || !gamepadStatus.hasPhysicalController || manualVirtualPadOverrideWithPhys)
    val effectiveSyntheticGamepadEnabled = effectiveShowVirtualController || isRemotePadConnected

    // Dynamically recompute injectionScript only when structural settings change (bitrate updates go through __updateNetworkProfile)
    val injectionScript = remember(
        config.clarityBoostEnabled,
        config.clarityPreset,
        currentServer,
        effectiveLocale,
        effectiveTranslate,
        config.translateLanguage,
        config.preserveGameTitles,
        config.bilingualMode,
        config.hideStreamStatsOverlay,
        config.cellularMode,
        config.networkTuningMode,
        config.wifiSpeedFirstPriority,
        config.lowEndDeviceMode,
        effectiveSyntheticGamepadEnabled,
        config.streamRenderScale,
        config.streamFpsCap,
        config.gpuLoadReductionEnabled,
        isCellular,
        config.physicalGamepadButtonMap,
        config.physLeftStickInnerDeadzone,
        config.physLeftStickOuterDeadzone,
        config.physRightStickInnerDeadzone,
        config.physRightStickOuterDeadzone,
        config.controllerShortcutsEnabled,
        config.shortcutToggleStatsButton,
        config.shortcutToggleVirtualPadButton,
        config.shortcutToggleRecordingButton,
        config.autoExitPreviewHandlerEnabled,
        config.xboxWebUiMode
    ) {
        XboxBridge.getInjectionScript(
            clarityBoost = config.clarityBoostEnabled,
            clarityPreset = config.clarityPreset.name,
            targetCountry = currentServer.countryCode,
            targetIp = currentServer.ip,
            userLocale = effectiveLocale,
            xcloudTranslate = effectiveTranslate,
            preserveTitles = config.preserveGameTitles,
            bilingualMode = config.bilingualMode,
            hideStreamStats = config.hideStreamStatsOverlay,
            cellularMode = config.cellularMode.id,
            networkTuningMode = config.networkTuningMode.id,
            speedFirstPriority = config.wifiSpeedFirstPriority,
            effectiveBitrateMbps = if (config.lowEndDeviceMode) 5 else effectiveBitrateMbps,
            lowEndDeviceMode = config.lowEndDeviceMode,
            virtualGamepadEnabled = effectiveSyntheticGamepadEnabled,
            renderScaleFactor = config.streamRenderScale.scaleFactor,
            fpsCap = config.streamFpsCap.fps,
            gpuLoadReduction = config.gpuLoadReductionEnabled,
            isCellular = isCellular
        )
    }

    val currentInjectionScript by rememberUpdatedState(injectionScript)

    // Dynamically update active WebView when user changes server or locale
    LaunchedEffect(currentServer, effectiveLocale) {
        CookieManager.getInstance().setCookie(
            "https://www.xbox.com",
            "MSPC-LOCALE=$effectiveLocale; domain=.xbox.com; path=/"
        )
        val safeCountry = org.json.JSONObject.quote(currentServer.countryCode)
        val safeIp = org.json.JSONObject.quote(currentServer.ip)
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

    val activeBrowserPerfScenario = if (streamUiState.isStreamConnected) {
        com.example.data.PerfScenario.STREAM_ACTIVE
    } else {
        com.example.data.PerfScenario.WEBVIEW_BROWSE
    }
    LaunchedEffect(activeBrowserPerfScenario) {
        com.example.data.PerformanceMonitor.setScenario(activeBrowserPerfScenario)
    }
    androidx.compose.runtime.SideEffect {
        com.example.data.PerformanceMonitor.recordRecomposition(activeBrowserPerfScenario)
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

    LaunchedEffect(config.cellularMode, config.networkTuningMode, config.wifiSpeedFirstPriority, effectiveBitrateMbps, config.lowEndDeviceMode, isCellular) {
        val targetMbps = if (config.lowEndDeviceMode) 5 else effectiveBitrateMbps
        webViewInstance?.evaluateJavascript(
            "if (window.__updateNetworkProfile) window.__updateNetworkProfile({ cellularMode: '${config.cellularMode.id}', networkTuningMode: '${config.networkTuningMode.id}', speedFirst: ${config.wifiSpeedFirstPriority}, targetBitrate: $targetMbps, lowEndDeviceMode: ${config.lowEndDeviceMode}, isCellular: $isCellular });",
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

    // Đồng bộ tức thì trạng thái Tay cầm ảo / Tay cầm từ xa với WebView
    LaunchedEffect(effectiveSyntheticGamepadEnabled, webViewInstance) {
        webViewInstance?.evaluateJavascript(
            "if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled($effectiveSyntheticGamepadEnabled);",
            null
        )
    }

    // Đưa trạng thái Tay cầm từ xa (UDP) vào đúng đường chuẩn của XboxBridge (__cpBtn, __cpStick, __cpReleaseAll)
    DisposableEffect(webViewInstance, activity) {
        com.example.cloudplay.RemotePadTvServer.onRemotePadButton = { buttonIndex, pressed, analogValue ->
            com.example.ui.components.GamepadUiNavigation.observeVirtualSelectStartPassively(buttonIndex, pressed)
            if (com.example.ui.components.GamepadUiNavigation.shouldRouteToAppUi) {
                com.example.ui.components.GamepadUiNavigation.dispatchRemotePadButton(buttonIndex, pressed)
            }
            val rv = (analogValue.coerceIn(0f, 1f) * 100f).let { kotlin.math.round(it) / 100f }
            webViewInstance?.evaluateJavascript(
                "if(window.__cpBtn){window.__cpBtn($buttonIndex,$pressed,$rv);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_button',{detail:{index:$buttonIndex,pressed:$pressed,value:$rv}}));}",
                null
            )
        }
        com.example.cloudplay.RemotePadTvServer.onRemotePadStick = { xAxisIndex, xVal, yAxisIndex, yVal ->
            if (xAxisIndex == 0 && yAxisIndex == 1 && com.example.ui.components.GamepadUiNavigation.shouldRouteToAppUi) {
                com.example.ui.components.GamepadUiNavigation.dispatchRemotePadStick(xVal, yVal)
            }
            val rx = (xVal.coerceIn(-1f, 1f) * 100f).let { kotlin.math.round(it) / 100f }
            val ry = (yVal.coerceIn(-1f, 1f) * 100f).let { kotlin.math.round(it) / 100f }
            webViewInstance?.evaluateJavascript(
                "if(window.__cpStick){window.__cpStick($xAxisIndex,$rx,$yAxisIndex,$ry);}else if(window.dispatchEvent){window.dispatchEvent(new CustomEvent('virtual_gamepad_axes_pair',{detail:{xAxis:$xAxisIndex,xVal:$rx,yAxis:$yAxisIndex,yVal:$ry}}));}",
                null
            )
        }
        com.example.cloudplay.RemotePadTvServer.onRemotePadReleaseAll = {
            com.example.ui.components.GamepadUiNavigation.releaseAllRemotePadInputs()
            webViewInstance?.evaluateJavascript(
                "if(window.__cpReleaseAll){window.__cpReleaseAll();}",
                null
            )
        }
        onDispose {
            com.example.ui.components.GamepadUiNavigation.releaseAllRemotePadInputs()
            webViewInstance?.evaluateJavascript(
                "if(window.__cpReleaseAll){window.__cpReleaseAll();}",
                null
            )
            com.example.cloudplay.RemotePadTvServer.onRemotePadButton = { buttonIndex, pressed, _ ->
                com.example.ui.components.GamepadUiNavigation.dispatchRemotePadButton(buttonIndex, pressed)
            }
            com.example.cloudplay.RemotePadTvServer.onRemotePadStick = { xAxisIndex, xVal, yAxisIndex, yVal ->
                if (xAxisIndex == 0 && yAxisIndex == 1) {
                    com.example.ui.components.GamepadUiNavigation.dispatchRemotePadStick(xVal, yVal)
                }
            }
            com.example.cloudplay.RemotePadTvServer.onRemotePadReleaseAll = {
                com.example.ui.components.GamepadUiNavigation.releaseAllRemotePadInputs()
            }
        }
    }

    // Bộ điều khiển Con quay hồi chuyển (Gyroscope) - Mặc định TẮT hoàn toàn nếu người dùng không bật
    val currentHoldLtOnly by androidx.compose.runtime.rememberUpdatedState(config.gyroHoldLtOnly)
    val currentWebViewForGyro by androidx.compose.runtime.rememberUpdatedState(webViewInstance)
    val gyroController = remember(context) {
        com.example.cloudplay.GyroscopeController(context) { targetRightStick, outX, outY ->
            val wv = currentWebViewForGyro ?: return@GyroscopeController
            val rx = (outX.coerceIn(-1f, 1f) * 100f).let { kotlin.math.round(it) / 100f }
            val ry = (outY.coerceIn(-1f, 1f) * 100f).let { kotlin.math.round(it) / 100f }
            wv.post {
                if (kotlin.math.abs(rx) < 0.001f && kotlin.math.abs(ry) < 0.001f && !SettingsRepository.config.value.gyroEnabled) {
                    wv.evaluateJavascript("if(window.__cpGyroClear)window.__cpGyroClear();", null)
                } else {
                    wv.evaluateJavascript(
                        "if(window.__cpGyro)window.__cpGyro($targetRightStick,$rx,$ry,$currentHoldLtOnly);",
                        null
                    )
                }
            }
        }
    }

    LaunchedEffect(
        config.gyroEnabled,
        config.gyroMode,
        config.gyroSensitivityX,
        config.gyroSensitivityY,
        config.gyroDeadzone,
        config.gyroInvertX,
        config.gyroInvertY,
        config.gyroHoldLtOnly,
        webViewInstance
    ) {
        gyroController.updateConfiguration(
            enabled = config.gyroEnabled,
            mode = config.gyroMode,
            sensitivityXPercent = kotlin.math.round(config.gyroSensitivityX * 100f).toInt(),
            sensitivityYPercent = kotlin.math.round(config.gyroSensitivityY * 100f).toInt(),
            deadzonePercent = kotlin.math.round(config.gyroDeadzone * 100f).toInt(),
            invertX = config.gyroInvertX,
            invertY = config.gyroInvertY
        )
        if (!config.gyroEnabled) {
            webViewInstance?.evaluateJavascript("if(window.__cpGyroClear)window.__cpGyroClear();", null)
        }
    }

    DisposableEffect(gyroController) {
        onDispose {
            gyroController.stopListening()
            currentWebViewForGyro?.evaluateJavascript("if(window.__cpGyroClear)window.__cpGyroClear();", null)
        }
    }

    // Đồng bộ tức thì cấu hình Tay cầm thật (đổi nút, vùng chết trong/ngoài từng cần, tổ hợp phím tắt View + nút)
    LaunchedEffect(
        config.physicalGamepadButtonMap,
        config.physLeftStickInnerDeadzone,
        config.physLeftStickOuterDeadzone,
        config.physRightStickInnerDeadzone,
        config.physRightStickOuterDeadzone,
        config.controllerShortcutsEnabled,
        config.shortcutToggleStatsButton,
        config.shortcutToggleVirtualPadButton,
        config.shortcutToggleRecordingButton,
        webViewInstance
    ) {
        val remapObj = org.json.JSONObject()
        SettingsRepository.parsePhysicalButtonMap(config.physicalGamepadButtonMap).forEach { (src, dst) ->
            remapObj.put(src.toString(), dst)
        }
        webViewInstance?.evaluateJavascript(
            "if (window.__updatePhysicalControllerConfig) window.__updatePhysicalControllerConfig({ remap: ${remapObj}, lInner: ${config.physLeftStickInnerDeadzone}, lOuter: ${config.physLeftStickOuterDeadzone}, rInner: ${config.physRightStickInnerDeadzone}, rOuter: ${config.physRightStickOuterDeadzone}, shortcutsEnabled: ${config.controllerShortcutsEnabled}, shortcutStats: ${config.shortcutToggleStatsButton}, shortcutVirtualPad: ${config.shortcutToggleVirtualPadButton}, shortcutRecord: ${config.shortcutToggleRecordingButton} });",
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

    // Đồng bộ tức thì trạng thái Bộ lọc làm nét (Clarity Boost) với WebView đang chạy
    LaunchedEffect(config.clarityBoostEnabled, config.clarityPreset, webViewInstance) {
        val presetName = if (config.clarityBoostEnabled && config.clarityPreset != com.example.data.ClarityPreset.OFF) {
            config.clarityPreset.name
        } else {
            "OFF"
        }
        webViewInstance?.evaluateJavascript(
            "if (window.__setClarityPreset) window.__setClarityPreset('$presetName'); if (window.__toggleClarityBoost) window.__toggleClarityBoost(${config.clarityBoostEnabled});",
            null
        )
    }

    // Đồng bộ tức thì trạng thái Bộ xử lý tự động thoát Giao diện mới (Exit preview) với WebView đang chạy
    LaunchedEffect(config.autoExitPreviewHandlerEnabled, config.xboxWebUiMode, classicBounceLoopStopped, webViewInstance) {
        val effectiveAutoExit = config.autoExitPreviewHandlerEnabled &&
            config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
            !classicBounceLoopStopped
        if (!effectiveAutoExit) {
            exitPreviewArmedAtMs = 0L
            exitPreviewOptOutStartedAtMs = 0L
        }
        webViewInstance?.evaluateJavascript(
            "if (window.__setAutoExitPreviewEnabled) window.__setAutoExitPreviewEnabled($effectiveAutoExit);",
            null
        )
    }

    BackHandler {
        if (showReturnButton) {
            returnToGame()
        } else {
            com.example.ui.components.GamepadUiNavigation.triggerHudToggle()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cloud_browser_screen")
    ) {
        // Main Web View
        androidx.compose.runtime.key(webViewSessionKey) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                object : WebView(ctx) {
                    private var lastUnbufferedMotionDeviceId = -1
                    private var lastHeaderTouchLogElapsedMs = 0L

                    override fun dispatchTouchEvent(event: android.view.MotionEvent?): Boolean {
                        val handled = super.dispatchTouchEvent(event)
                        if (event != null &&
                            event.actionMasked == android.view.MotionEvent.ACTION_UP &&
                            !streamController.uiState.value.isStreamConnected
                        ) {
                            val w = width.coerceAtLeast(1)
                            val h = height.coerceAtLeast(1)
                            val pxX = event.x.toInt()
                            val pxY = event.y.toInt()
                            val isHeaderOrTopRightZone = (pxY <= (h * 0.22f).toInt()) ||
                                (pxX >= (w * 0.65f).toInt() && pxY <= (h * 0.32f).toInt())
                            val nowElapsed = android.os.SystemClock.elapsedRealtime()
                            if (isHeaderOrTopRightZone && nowElapsed - lastHeaderTouchLogElapsedMs >= 250L) {
                                lastHeaderTouchLogElapsedMs = nowElapsed
                                val curUrlSafe = com.example.data.AppLogger.safeUrlHostPath(this.url ?: currentMainFrameUrl)
                                val isDesktopUa = (this.getTag(com.example.R.id.webview_desktop_ua_tag) as? Boolean)
                                    ?: SettingsRepository.config.value.forceDesktopUserAgent
                                val uaLabel = if (isDesktopUa) "desktop" else "mobile"
                                com.example.data.AppLogger.info(
                                    "TOUCH",
                                    "WebView nhận chạm vùng đầu trang: px=($pxX,$pxY)/${w}x${h} | handled=$handled | ua=$uaLabel | url=$curUrlSafe"
                                )
                                val relX = (event.x / w.toFloat()).coerceIn(0f, 1f)
                                val relY = (event.y / h.toFloat()).coerceIn(0f, 1f)
                                this.postDelayed({
                                    try {
                                        val probeJs = """
                                            (function(){
                                                try {
                                                    var vw = window.innerWidth || 1;
                                                    var vh = window.innerHeight || 1;
                                                    var cx = Math.round($relX * vw);
                                                    var cy = Math.round($relY * vh);
                                                    var hit = document.elementFromPoint(cx, cy);
                                                    function safeDesc(el) {
                                                        if (!el || !el.tagName) return 'null';
                                                        var tag = el.tagName.toLowerCase();
                                                        var role = el.getAttribute('role') || '';
                                                        var tid = el.getAttribute('data-testid') || '';
                                                        var exp = el.getAttribute('aria-expanded') || '';
                                                        var pop = el.getAttribute('aria-haspopup') || '';
                                                        var dis = el.disabled || el.getAttribute('aria-disabled') || '';
                                                        var rawAria = (el.getAttribute('aria-label') || '').toLowerCase();
                                                        var ariaKind = '';
                                                        if (rawAria.indexOf('profile') !== -1 || rawAria.indexOf('hồ sơ') !== -1 || rawAria.indexOf('account') !== -1 || rawAria.indexOf('tài khoản') !== -1) ariaKind = 'profile';
                                                        else if (rawAria.indexOf('setting') !== -1 || rawAria.indexOf('cài đặt') !== -1) ariaKind = 'settings';
                                                        else if (rawAria.indexOf('preview') !== -1 || rawAria.indexOf('xem trước') !== -1) ariaKind = 'preview';
                                                        else if (rawAria.indexOf('menu') !== -1 || rawAria.indexOf('guide') !== -1) ariaKind = 'menu';
                                                        else if (rawAria.length > 0) ariaKind = 'other(' + rawAria.length + ')';
                                                        var hasGamerpic = !!(el.querySelector && el.querySelector('img[src*="xboxlive.com"], img[src*="ssl-images-microsoft.com"], img[alt*="gamerpic" i], [data-testid*="gamerpic" i]'));
                                                        var cs = window.getComputedStyle ? window.getComputedStyle(el) : null;
                                                        var pe = cs ? cs.pointerEvents : '';
                                                        var r = el.getBoundingClientRect ? el.getBoundingClientRect() : null;
                                                        var rectStr = r ? (Math.round(r.left) + ',' + Math.round(r.top) + ' ' + Math.round(r.width) + 'x' + Math.round(r.height)) : '';
                                                        return tag + (tid ? '#' + tid : '') + (role ? '[role=' + role + ']' : '') + (ariaKind ? '[aria=' + ariaKind + ']' : '') + (exp ? '[expanded=' + exp + ']' : '') + (pop ? '[haspopup=' + pop + ']' : '') + (dis ? '[disabled=' + dis + ']' : '') + (hasGamerpic ? '[hasGamerpic=true]' : '') + (pe && pe !== 'auto' ? '[pe=' + pe + ']' : '') + (rectStr ? '@(' + rectStr + ')' : '');
                                                    }
                                                    var btn = hit && hit.closest ? hit.closest('button, a, [role="button"], [role="menuitem"], [data-testid]') : null;
                                                    var menus = document.querySelectorAll('[role="menu"], [role="dialog"], [aria-modal="true"], [popover], [data-state="open"], [class*="Menu" i], [class*="Drawer" i], [class*="Popover" i], [class*="Sheet" i]');
                                                    var openList = [];
                                                    for (var i = 0; i < menus.length && openList.length < 4; i++) {
                                                        var m = menus[i];
                                                        var mcs = window.getComputedStyle ? window.getComputedStyle(m) : null;
                                                        if (mcs && mcs.display !== 'none' && mcs.visibility !== 'hidden' && parseFloat(mcs.opacity || '1') > 0.01) {
                                                            var mr = m.getBoundingClientRect ? m.getBoundingClientRect() : null;
                                                            if (mr && mr.width > 20 && mr.height > 20) {
                                                                openList.push((m.tagName || '').toLowerCase() + '[role=' + (m.getAttribute('role') || '') + ',tid=' + (m.getAttribute('data-testid') || '') + ']@(' + Math.round(mr.left) + ',' + Math.round(mr.top) + ' ' + Math.round(mr.width) + 'x' + Math.round(mr.height) + ',z=' + (mcs.zIndex || '') + ')');
                                                            }
                                                        }
                                                    }
                                                    return 'css=(' + cx + ',' + cy + ')/' + vw + 'x' + vh + ' | hit=' + safeDesc(hit) + ' | interactive=' + safeDesc(btn) + ' | openPopups=' + (openList.length > 0 ? openList.join(';') : 'none');
                                                } catch (e) {
                                                    return 'err=' + (e && e.message ? e.message : String(e));
                                                }
                                            })()
                                        """.trimIndent()
                                        this.evaluateJavascript(probeJs) { jsRes ->
                                            val cleanRes = jsRes?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "null"
                                            com.example.data.AppLogger.info("TOUCH", "Kiểm tra DOM vùng chạm (+180ms): $cleanRes")
                                        }
                                    } catch (_: Exception) {}
                                }, 180L)
                            }
                        }
                        return handled
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

                    // Giữ LAYER_TYPE_NONE để tránh ép WebView vẽ qua bộ đệm phần mềm/lớp trung gian phụ.
                    setLayerType(android.view.View.LAYER_TYPE_NONE, null)
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

                        val initialUa = if (config.forceDesktopUserAgent) {
                            EDGE_DESKTOP_UA
                        } else {
                            settings.userAgentString
                        }
                        userAgentString = initialUa
                    }
                    setTag(com.example.R.id.webview_desktop_ua_tag, config.forceDesktopUserAgent)

                    CookieManager.getInstance().let { cookieManager ->
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        cookieManager.setCookie("https://www.xbox.com", "MSPC-LOCALE=$effectiveLocale; domain=.xbox.com; path=/")
                    }

                    // Add JavaScript bridge for stream / launch detection & smart translation
                    val bridge = XboxBridge(streamController)
                    bridge.setWebView(this)
                    bridge.setExitPreviewSignalListener { rawSig ->
                        val nowElapsed = android.os.SystemClock.elapsedRealtime()
                        val curWebUrl = this.url ?: currentMainFrameUrl
                        val sig = rawSig.substringBefore("|").trim().uppercase()
                        val payload = rawSig.substringAfter("|", "")
                        val payloadUrl = payload.substringBefore("||").trim()
                        val payloadTitle = payload.substringAfter("||", "").trim()

                        fun captureActiveGameContext() {
                            if (payloadUrl.isNotBlank()) {
                                exitPreviewSavedGameUrl = payloadUrl
                                lastVisitedGameUrl = payloadUrl
                            } else if (curWebUrl.contains("/products/", ignoreCase = true) ||
                                curWebUrl.contains("/play/games/", ignoreCase = true) ||
                                curWebUrl.contains("/games/", ignoreCase = true)
                            ) {
                                exitPreviewSavedGameUrl = curWebUrl
                                lastVisitedGameUrl = curWebUrl
                            } else if (lastVisitedGameUrl.isNotBlank()) {
                                exitPreviewSavedGameUrl = lastVisitedGameUrl
                            } else if (lastXboxUrl.contains("/products/", ignoreCase = true) ||
                                lastXboxUrl.contains("/play/games/", ignoreCase = true)
                            ) {
                                exitPreviewSavedGameUrl = lastXboxUrl
                            }
                            if (payloadTitle.isNotBlank()) {
                                exitPreviewSavedGameTitle = payloadTitle
                            }
                        }

                        if (sig == "EXIT_PREVIEW_MANUAL_CLICK") {
                            captureActiveGameContext()
                            if (!SettingsRepository.config.value.autoExitPreviewHandlerEnabled) {
                                com.example.data.AppLogger.info(
                                    "WEB-UI",
                                    "Ghi nhận game đang xem trước khi thoát xem trước thủ công (autoExitPreview=OFF): savedGame=${com.example.data.AppLogger.safeUrlHostPath(exitPreviewSavedGameUrl)}, title=$exitPreviewSavedGameTitle"
                                )
                            }
                            return@setExitPreviewSignalListener
                        }

                        val curCfg = SettingsRepository.config.value
                        if (!curCfg.autoExitPreviewHandlerEnabled ||
                            curCfg.xboxWebUiMode != com.example.data.XboxWebUiMode.CLASSIC ||
                            classicBounceLoopStopped
                        ) {
                            exitPreviewArmedAtMs = 0L
                            exitPreviewOptOutStartedAtMs = 0L
                            return@setExitPreviewSignalListener
                        }

                        when (sig) {
                            "EXIT_PREVIEW_ARMED" -> {
                                exitPreviewArmedAtMs = nowElapsed
                                exitPreviewOptOutStartedAtMs = 0L
                                captureActiveGameContext()
                                com.example.data.AppLogger.info(
                                    "WEB-UI",
                                    "Người dùng bấm Thoát khỏi chế độ xem trước (Exit preview) trên play.xbox.com -> chờ đóng hộp thoại & hoàn tất opt-out (autoExit=${SettingsRepository.config.value.autoExitPreviewHandlerEnabled}, savedGame=${com.example.data.AppLogger.safeUrlHostPath(exitPreviewSavedGameUrl)}, title=$exitPreviewSavedGameTitle)"
                                )
                            }
                            "EXIT_PREVIEW_OPT_OUT_STARTED" -> {
                                if (exitPreviewArmedAtMs == 0L) exitPreviewArmedAtMs = nowElapsed
                                exitPreviewOptOutStartedAtMs = nowElapsed
                                if (exitPreviewSavedGameUrl.isBlank() || payloadUrl.isNotBlank()) {
                                    captureActiveGameContext()
                                }
                                com.example.data.AppLogger.info(
                                    "WEB-UI",
                                    "Đã đóng/xác nhận hộp thoại Thoát xem trước -> đang chờ vòng xoay Xbox lưu xong trạng thái opt-out (~3.6s)"
                                )
                            }
                            "EXIT_PREVIEW_COMMIT" -> {
                                if (exitPreviewSavedGameUrl.isBlank() || payloadUrl.isNotBlank()) {
                                    captureActiveGameContext()
                                }
                                commitExitPreviewToClassicUi("EXIT_PREVIEW_COMMIT từ XboxBridge (Giai đoạn 2 Skeleton)")
                            }
                        }
                    }
                    bridgeInstance = bridge
                    com.example.cloudplay.QualityAdapter.setWebView(this)
                    bridge.updateCurrentUrl(activeSessionInitialUrl)
                    addJavascriptInterface(bridge, "CloudPlayBridge")

                    var hasDocumentStartScript = false
                    // Inject document start script exclusively on authorized Xbox & Microsoft domains
                    if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        try {
                            androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                                this,
                                currentInjectionScript,
                                setOf(
                                    "https://www.xbox.com",
                                    "https://xbox.com",
                                    "https://play.xbox.com",
                                    "https://login.live.com",
                                    "https://login.microsoftonline.com"
                                )
                            )
                            hasDocumentStartScript = true
                            com.example.data.AppLogger.info("WEBVIEW", "Đăng ký script DOCUMENT_START cho tên miền Xbox")
                        } catch (e: Exception) {
                            android.util.Log.w("CloudBrowser", "Document start script not supported", e)
                            com.example.data.AppLogger.warn(
                                "WEBVIEW",
                                "Thiết bị không hỗ trợ DOCUMENT_START_SCRIPT, dùng dự phòng tiêm ở onPageStarted (${com.example.data.AppLogger.shortThrowable(e)})"
                            )
                        }
                    }

                    var injectedForCurrentPage = false
                    var lastLoggedScriptInjectedHostPath = ""

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (url != null &&
                                exitPreviewOptOutStartedAtMs > 0L &&
                                config.autoExitPreviewHandlerEnabled &&
                                config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
                                !classicBounceLoopStopped
                            ) {
                                val optElapsed = android.os.SystemClock.elapsedRealtime() - exitPreviewOptOutStartedAtMs
                                val isMigrateStep = url.contains("/migrate", ignoreCase = true)
                                // Chỉ cho phép cắt dự phòng sau >= 3800ms để không làm đứt Giai đoạn 1 (vòng xoay lưu opt-out ~3.2s)
                                if (optElapsed in 3800L..16000L && !isMigrateStep) {
                                    commitExitPreviewToClassicUi("onPageStarted sau opt-out (${com.example.data.AppLogger.safeUrlHostPath(url)})")
                                    return
                                }
                            }
                            isPageLoading = true
                            injectedForCurrentPage = hasDocumentStartScript
                            bridge.updateCurrentUrl(url)
                            url?.let {
                                streamController.onUrlChanged(it)
                                handleMainFrameNavigation(it, "onPageStarted")
                            }
                            // Luôn đồng bộ cấu hình vùng/máy chủ mới nhất (currentInjectionScript tự chống chạy trùng nhờ window.__cloudPlayInjected)
                            if (XboxBridge.isTrustedHost(url)) {
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                                val safeHp = com.example.data.AppLogger.safeUrlHostPath(url)
                                if (safeHp != lastLoggedScriptInjectedHostPath) {
                                    lastLoggedScriptInjectedHostPath = safeHp
                                    com.example.data.AppLogger.info("WEBVIEW", "Tiêm script tối ưu vào trang: $safeHp")
                                }
                            }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            if (suppressLoadingOverlayAfterExitPreview) {
                                isPageLoading = false
                            }
                            if (!injectedForCurrentPage && XboxBridge.isTrustedHost(url)) {
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                                val safeHp = com.example.data.AppLogger.safeUrlHostPath(url)
                                if (safeHp != lastLoggedScriptInjectedHostPath) {
                                    lastLoggedScriptInjectedHostPath = safeHp
                                    com.example.data.AppLogger.info("WEBVIEW", "Tiêm script tối ưu (onPageCommitVisible): $safeHp")
                                }
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                            suppressLoadingOverlayAfterExitPreview = false
                            bridge.updateCurrentUrl(url)
                            url?.let {
                                handleMainFrameNavigation(it, "onPageFinished")
                            }
                            if (!injectedForCurrentPage && XboxBridge.isTrustedHost(url)) {
                                // Ensure hooks are active on finished page if not already injected at document start
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                                val safeHp = com.example.data.AppLogger.safeUrlHostPath(url)
                                if (safeHp != lastLoggedScriptInjectedHostPath) {
                                    lastLoggedScriptInjectedHostPath = safeHp
                                    com.example.data.AppLogger.info("WEBVIEW", "Tiêm script tối ưu (onPageFinished): $safeHp")
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val isMainFrame = request?.isForMainFrame == true
                            val targetUrl = request?.url?.toString()
                            if (isMainFrame && targetUrl != null) {
                                val isClassicAutoExitActive = config.autoExitPreviewHandlerEnabled &&
                                    config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
                                    !classicBounceLoopStopped
                                if (exitPreviewOptOutStartedAtMs > 0L && isClassicAutoExitActive) {
                                    val optElapsed = android.os.SystemClock.elapsedRealtime() - exitPreviewOptOutStartedAtMs
                                    val isMigrateStep = targetUrl.contains("/migrate", ignoreCase = true)
                                    if (optElapsed in 3800L..16000L && !isMigrateStep) {
                                        commitExitPreviewToClassicUi("shouldOverride sau opt-out (${com.example.data.AppLogger.safeUrlHostPath(targetUrl)})")
                                        return true
                                    }
                                }
                                if (isClassicAutoExitActive) {
                                    val prevHost = try { android.net.Uri.parse(currentMainFrameUrl).host?.lowercase() } catch (_: Exception) { null }
                                    val nextUri = try { android.net.Uri.parse(targetUrl) } catch (_: Exception) { null }
                                    val nextHost = nextUri?.host?.lowercase()
                                    val nextPath = nextUri?.path?.trimEnd('/') ?: ""
                                    val isBareClassicRoot = (nextHost == "www.xbox.com" || nextHost == "xbox.com") &&
                                        (nextPath.equals("/play", ignoreCase = true) || Regex("^/[a-zA-Z]{2}-[a-zA-Z]{2}/play$", RegexOption.IGNORE_CASE).matches(nextPath))
                                    if (prevHost == "play.xbox.com" && isBareClassicRoot) {
                                        applyClassicPreviewOptOutCookies()
                                        val directClassicGameUrl = resolveTargetGameClassicUrl()
                                        val gameKey = resolveBounceGameIdentity(
                                            listOf(directClassicGameUrl, exitPreviewSavedGameUrl, lastVisitedGameUrl, currentMainFrameUrl, initialUrl)
                                        )
                                        if (gameKey != bounceTrackedGameKey) {
                                            bounceTrackedGameKey = gameKey
                                            consecutiveClassicBounceCount = 0
                                            classicBounceLoopStopped = false
                                            isClassicBounceBannerDismissed = false
                                        }
                                        isAwaitingOrOnClassicSurface = true
                                        lastClassicSwitchElapsedMs = android.os.SystemClock.elapsedRealtime()
                                        exitPreviewArmedAtMs = 0L
                                        exitPreviewOptOutStartedAtMs = 0L
                                        exitPreviewSavedGameUrl = ""
                                        exitPreviewSavedGameTitle = ""
                                        pendingAppNavReason = "chuyển thẳng vào trang game Giao diện cũ khi rời play.xbox.com"
                                        lastXboxUrl = directClassicGameUrl
                                        currentMainFrameUrl = directClassicGameUrl
                                        view?.loadUrl(directClassicGameUrl)
                                        return true
                                    }
                                }
                                val isRedirect = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    request.isRedirect
                                } else {
                                    false
                                }
                                bridge.updateCurrentUrl(targetUrl)
                                streamController.onUrlChanged(targetUrl)
                                handleMainFrameNavigation(targetUrl, "shouldOverride(redirect=$isRedirect)")
                            }
                            return false
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            if (url != null &&
                                exitPreviewOptOutStartedAtMs > 0L &&
                                config.autoExitPreviewHandlerEnabled &&
                                config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
                                !classicBounceLoopStopped
                            ) {
                                val optElapsed = android.os.SystemClock.elapsedRealtime() - exitPreviewOptOutStartedAtMs
                                val isMigrateStep = url.contains("/migrate", ignoreCase = true)
                                if (optElapsed in 3800L..16000L && !isMigrateStep) {
                                    commitExitPreviewToClassicUi("historyUpdate sau opt-out (${com.example.data.AppLogger.safeUrlHostPath(url)})")
                                    return
                                }
                            }
                            bridge.updateCurrentUrl(url)
                            url?.let {
                                streamController.onUrlChanged(it)
                                handleMainFrameNavigation(it, "historyUpdate(reload=$isReload)")
                            }
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                            val didCrash = detail?.didCrash() == true
                            android.util.Log.w("CloudBrowser", "Render process fallback handled gracefully: didCrash=$didCrash")
                            com.example.data.AppLogger.error(
                                "WEBVIEW",
                                "Tiến trình dựng hình WebView bị dừng bất ngờ (didCrash=$didCrash)"
                            )
                            return true
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                val safeHp = com.example.data.AppLogger.safeUrlHostPath(request.url?.toString())
                                val code = error?.errorCode ?: -1
                                val desc = error?.description?.toString() ?: "Unknown"
                                android.util.Log.w("CloudBrowser", "Page error: $desc")
                                com.example.data.AppLogger.error(
                                    "WEBVIEW",
                                    "Lỗi tải trang chính ($safeHp): mã=$code, lý do=$desc"
                                )
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            val statusCode = errorResponse?.statusCode ?: return
                            val isMainFrame = request?.isForMainFrame == true
                            val reqUrl = request?.url?.toString() ?: ""
                            val isCriticalApi = reqUrl.contains("/sessions/cloud/", ignoreCase = true) ||
                                reqUrl.contains("xhome.gssv-play", ignoreCase = true) ||
                                reqUrl.contains("uks.gssv-play", ignoreCase = true)
                            if (statusCode >= 400 && (isMainFrame || isCriticalApi)) {
                                val safeHp = com.example.data.AppLogger.safeUrlHostPath(reqUrl)
                                com.example.data.AppLogger.error(
                                    "WEBVIEW",
                                    "Lỗi HTTP $statusCode khi tải ${if (isMainFrame) "trang chính" else "API phiên chơi"} ($safeHp)"
                                )
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        private var lastConsoleErrLogMs = 0L

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            val nextProgress = newProgress / 100f
                            val curProgress = pageLoadingProgress
                            val crossedThreshold = (curProgress < 0.80f && nextProgress >= 0.80f) ||
                                (curProgress < 0.05f && nextProgress >= 0.05f)
                            if (newProgress >= 100 || newProgress <= 5 || crossedThreshold || kotlin.math.abs(nextProgress - curProgress) >= 0.05f) {
                                pageLoadingProgress = nextProgress
                            }
                            if (!injectedForCurrentPage && newProgress >= 30 && XboxBridge.isTrustedHost(view?.url)) {
                                injectedForCurrentPage = true
                                view?.evaluateJavascript(currentInjectionScript, null)
                            }
                        }

                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            if (consoleMessage != null &&
                                consoleMessage.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR &&
                                !streamController.uiState.value.isStreamConnected
                            ) {
                                val nowMs = android.os.SystemClock.elapsedRealtime()
                                if (nowMs - lastConsoleErrLogMs >= 400L) {
                                    lastConsoleErrLogMs = nowMs
                                    val srcSafe = com.example.data.AppLogger.safeUrlHostPath(consoleMessage.sourceId())
                                    val rawMsg = consoleMessage.message() ?: ""
                                    val cleanMsg = com.example.data.AppLogger.sanitizeMessage(rawMsg).take(220)
                                    com.example.data.AppLogger.warn(
                                        "WEB-UI",
                                        "Lỗi JS trên trang ($srcSafe:${consoleMessage.lineNumber()}): $cleanMsg"
                                    )
                                }
                            }
                            return super.onConsoleMessage(consoleMessage)
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

                    if (config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC) {
                        applyClassicPreviewOptOutCookies()
                    }
                    val formattedInitialUrl = SettingsRepository.formatXboxUrlWithLocale(activeSessionInitialUrl, effectiveLocale)
                    val customHeaders = mapOf(
                        "Accept-Language" to "$effectiveLocale,en;q=0.9",
                        "X-Edge-Shopping-Flag" to "0"
                    )
                    val initUaMode = if (config.forceDesktopUserAgent) "desktop" else "mobile"
                    if (pendingAppNavReason == null) {
                        pendingAppNavReason = "khởi tạo WebView (initialUrl=${com.example.data.AppLogger.safeUrlHostPath(activeSessionInitialUrl)}, chế độ=${config.xboxWebUiMode.name}, ua=$initUaMode)"
                    }
                    com.example.data.AppLogger.info(
                        "WEB-UI",
                        "Khởi tạo WebView (phiên #$webViewSessionKey): initialUrl=${com.example.data.AppLogger.safeUrlHostPath(activeSessionInitialUrl)} -> formatted=${com.example.data.AppLogger.safeUrlHostPath(formattedInitialUrl)} | chế độ=${config.xboxWebUiMode.name} | ua=$initUaMode | thị trường=$effectiveLocale"
                    )
                    loadUrl(formattedInitialUrl, customHeaders)
                    webViewInstance = this
                }
            },
            update = { view ->
                // Tránh gọi IPC sang luồng Chromium (WebSettings.getDefaultUserAgent / view.settings.userAgentString)
                // mỗi khi Compose vẽ lại theo nhịp telemetry trong trận.
                val wantDesktopUa = config.forceDesktopUserAgent
                val lastDesktopFlag = view.getTag(com.example.R.id.webview_desktop_ua_tag) as? Boolean
                if (lastDesktopFlag != wantDesktopUa) {
                    view.setTag(com.example.R.id.webview_desktop_ua_tag, wantDesktopUa)
                    val desiredUa = if (wantDesktopUa) EDGE_DESKTOP_UA else WebSettings.getDefaultUserAgent(context)
                    if (view.settings.userAgentString != desiredUa) {
                        view.settings.userAgentString = desiredUa
                        com.example.data.AppLogger.info(
                            "WEB-UI",
                            "Cập nhật User-Agent của WebView đang chạy: ua=${if (wantDesktopUa) "desktop" else "mobile"} | chế độ=${config.xboxWebUiMode.name}"
                        )
                    }
                }
            }
        )
        }

        val gameProfile = remember(activeSessionInitialUrl, currentMainFrameUrl) {
            val currentProfile = com.example.data.GameOptimizationRepository.getProfileForUrl(currentMainFrameUrl)
            if (currentProfile.gameId != "generic") currentProfile
            else com.example.data.GameOptimizationRepository.getProfileForUrl(activeSessionInitialUrl)
        }

        // Kích hoạt chế độ Fortnite Arena (tự động chọn bố cục FPS Pro khi bật tay cầm ảo lần đầu trong Fortnite, không ép bộ đệm 8ms gây sụt frame)
        LaunchedEffect(gameProfile.gameId, webViewInstance) {
            val isFn = gameProfile.gameId == "fortnite"
            webViewInstance?.evaluateJavascript(
                "window.__fortniteArenaMode = $isFn;",
                null
            )
            if (isFn && config.gamepadLayoutPreset == com.example.data.GamepadLayoutPreset.STANDARD) {
                SettingsRepository.updateGamepadLayoutPreset(com.example.data.GamepadLayoutPreset.FPS_PRO)
                SettingsRepository.updateGamepadSprintLockEnabled(true)
            }
        }

        val isLoadingOverlayVisible = !suppressLoadingOverlayAfterExitPreview && isPageLoading && pageLoadingProgress < 0.80f
        var isHudOpen by remember { mutableStateOf(false) }
        val shouldRouteGamepadToAppUi = isHudOpen || showQuickSettingsDialog || showTranslatorDialog || isLoadingOverlayVisible

        LaunchedEffect(shouldRouteGamepadToAppUi, webViewInstance) {
            com.example.ui.components.GamepadUiNavigation.shouldRouteToAppUi = shouldRouteGamepadToAppUi
            if (!shouldRouteGamepadToAppUi) {
                webViewInstance?.requestFocus()
            }
        }

        // Dedicated Game Loading Screen (Optimized for Portrait & Landscape, isolated state collection)
        IsolatedInGameLoadingOverlay(
            isLoading = isLoadingOverlayVisible,
            progress = pageLoadingProgress,
            hasGamepad = gamepadStatus.hasPhysicalController,
            gameProfile = gameProfile,
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

        // Heads-up Notification Banner (Auto-Switch Countdown / Direct Active, isolated state collection)
        IsolatedStatusBanner(
            streamController = streamController,
            isEnglishUI = config.isEnglishUI(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )

        // Mục 7e: Khi người dùng đã tự đổi thị trường Xbox khác en-US và vào game thất bại (unlock fail),
        // hiện gợi ý "Quay về en-US" để người dùng bấm (không tự ý đổi ngầm).
        AnimatedVisibility(
            visible = hasUnlockFailPrompt && !effectiveLocale.equals("en-US", ignoreCase = true),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                    .testTag("market_fallback_en_us_banner"),
                color = XboxSurface.copy(alpha = 0.96f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (config.isEnglishUI()) {
                            "Market $effectiveLocale failed to launch. Return to verified en-US market?"
                        } else {
                            "Thị trường $effectiveLocale không vào được game. Bạn có muốn quay về en-US?"
                        },
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            SettingsRepository.resetXboxMarketToDefault()
                            VpnManager.dismissUnlockFailPrompt()
                            val baseForReset = currentMainFrameUrl.ifBlank { initialUrl }
                            val enUsUrl = SettingsRepository.formatXboxUrlWithLocale(baseForReset, "en-US")
                            pendingAppNavReason = "bấm Quay về thị trường en-US trên biểu ngữ"
                            com.example.data.AppLogger.info(
                                "RESET",
                                "Đặt lại thị trường Xbox về en-US: từ=${com.example.data.AppLogger.safeUrlHostPath(baseForReset)} -> đích=${com.example.data.AppLogger.safeUrlHostPath(enUsUrl)} | chế độ=${config.xboxWebUiMode.name}"
                            )
                            webViewInstance?.loadUrl(enUsUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XboxNeonGreen),
                        modifier = Modifier.testTag("reset_market_to_en_us_banner_btn")
                    ) {
                        Text(
                            text = if (config.isEnglishUI()) "Return to en-US" else "Quay về en-US",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // A2: Nhắc nhở nhỏ không chặn khi máy chủ lặp lại lỗi không mở khóa được vùng (kèm gợi ý máy chủ đã đo thật / báo đã tự đổi ở chế độ tự chọn)
        if (!streamUiState.isStreamConnected) {
            IsolatedServerUnlockFailReminderChip(
                config = config,
                effectiveLocale = effectiveLocale,
                webViewInstance = webViewInstance,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // A1: Nhắc nhở nhỏ không chặn khi Xbox liên tục đẩy ngược từ Giao diện cũ về play.xbox.com (>= 2 lần trong cùng một game)
        if (classicBounceLoopStopped &&
            !isClassicBounceBannerDismissed &&
            config.xboxWebUiMode == com.example.data.XboxWebUiMode.CLASSIC &&
            !streamUiState.isStreamConnected
        ) {
            ClassicUiBounceReminderChip(
                config = config,
                onRetryClassic = {
                    isClassicBounceBannerDismissed = true
                    classicBounceLoopStopped = false
                    consecutiveClassicBounceCount = 1
                    switchWebViewToClassicUi("người dùng bấm Thử lại trên thanh nhắc giao diện cũ")
                },
                onContinueNewUi = {
                    isClassicBounceBannerDismissed = true
                    classicBounceLoopStopped = false
                    consecutiveClassicBounceCount = 0
                    lastAppliedUiMode = com.example.data.XboxWebUiMode.NEW_UI
                    SettingsRepository.updateXboxWebUiMode(com.example.data.XboxWebUiMode.NEW_UI)
                    com.example.data.AppLogger.info(
                        "WEB-UI",
                        "Người dùng chọn tiếp tục dùng Giao diện mới (play.xbox.com) từ thanh nhắc"
                    )
                },
                onDismiss = {
                    isClassicBounceBannerDismissed = true
                    dismissedAtWebMarket = lastSeenWebMarketLocale
                    com.example.data.AppLogger.info(
                        "WEB-UI",
                        "Người dùng tắt thanh nhắc giao diện mới cho lần chuyển hướng hiện tại (thị trường=${lastSeenWebMarketLocale.ifBlank { "default" }})"
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
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

        val activeGameKey = remember(currentMainFrameUrl, initialUrl, gameProfile.gameId, streamUiState.gameTitle) {
            when {
                gameProfile.gameId.isNotBlank() && gameProfile.gameId != "generic" -> gameProfile.gameId.lowercase()
                else -> {
                    val urlSlug = Regex("/play/(?:launch|games)/([^/?#]+)", RegexOption.IGNORE_CASE)
                        .find(currentMainFrameUrl.ifBlank { initialUrl })
                        ?.groupValues?.getOrNull(1)
                        ?.lowercase()
                        ?.replace(Regex("[^a-z0-9_-]+"), "_")
                        ?.trim('_')
                        ?.takeIf { it.isNotBlank() }
                    urlSlug ?: streamUiState.gameTitle
                        ?.lowercase()
                        ?.replace(Regex("[^a-z0-9]+"), "_")
                        ?.trim('_')
                        ?.takeIf { it.isNotBlank() }
                }
            }
        }
        val activeGameDisplayName = remember(streamUiState.gameTitle, gameProfile.title, gameProfile.gameId, activeGameKey) {
            streamUiState.gameTitle?.takeIf { it.isNotBlank() }
                ?: gameProfile.title.takeIf { gameProfile.gameId != "generic" && it.isNotBlank() }
                ?: activeGameKey
        }

        LaunchedEffect(activeGameKey, activeGameDisplayName, gameProfile.gameId, config.gamepadLayoutPreset, isRemotePadConnected) {
            val keyLower = (activeGameKey ?: gameProfile.gameId).lowercase()
            val presetId = when {
                keyLower.contains("fortnite") || keyLower.contains("halo") || keyLower.contains("call_of_duty") || keyLower.contains("cod") || keyLower.contains("apex") || keyLower.contains("battlefield") -> "fps"
                keyLower.contains("forza") || keyLower.contains("f1") || keyLower.contains("dirt") || keyLower.contains("need_for_speed") || keyLower.contains("asphalt") -> "racing"
                keyLower.contains("starfield") || keyLower.contains("gta") || keyLower.contains("elden") || keyLower.contains("assassin") || keyLower.contains("mortal_kombat") -> "action"
                else -> when (config.gamepadLayoutPreset) {
                    com.example.data.GamepadLayoutPreset.FPS_PRO -> "fps"
                    com.example.data.GamepadLayoutPreset.COMPACT -> "action"
                    com.example.data.GamepadLayoutPreset.RACING -> "racing"
                    com.example.data.GamepadLayoutPreset.STANDARD -> "standard"
                }
            }
            com.example.cloudplay.RemotePadTvServer.notifyActiveGameChanged(
                gameKey = activeGameKey.orEmpty(),
                gameTitle = activeGameDisplayName.orEmpty(),
                presetName = presetId
            )
        }

        // Virtual Gamepad Overlay (if toggled on and not auto-hidden by physical controller)
        if (effectiveShowVirtualController) {
            VirtualGamepadOverlay(
                webView = webViewInstance,
                gameKey = activeGameKey,
                gameDisplayName = activeGameDisplayName,
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

        // In-game Floating HUD Pill (isolated state collection so WebView never recomposes on telemetry ticks)
        IsolatedFloatingHud(
            isGameActive = streamUiState.isGameActive,
            virtualControllerEnabled = effectiveShowVirtualController,
            physicalControllerConnected = gamepadStatus.hasPhysicalController,
            controllerName = gamepadStatus.controllerName,
            onToggleNetwork = {
                if (VpnManager.state.value.mode == NetworkMode.VPN_JAPAN) {
                    streamController.forceSwitchToDirectNetwork()
                } else {
                    VpnManager.connect(context)
                }
            },
            onToggleVirtualController = {
                if (gamepadStatus.hasPhysicalController && config.autoHideVirtualOnPhysicalController) {
                    if (!effectiveShowVirtualController) {
                        manualVirtualPadOverrideWithPhys = true
                        if (!config.showVirtualController) {
                            SettingsRepository.updateShowVirtualController(true)
                        }
                    } else {
                        manualVirtualPadOverrideWithPhys = false
                    }
                } else {
                    SettingsRepository.updateShowVirtualController(!config.showVirtualController)
                }
            },
            onOpenSettings = {
                com.example.data.AppLogger.info("UI", "Mở bảng Tùy chỉnh nhanh Hiển thị & Điều khiển trong game")
                showQuickSettingsDialog = true
            },
            onNavigateHome = onNavigateBackToDashboard,
            onReloadPage = {
                val curWvUrl = webViewInstance?.url ?: currentMainFrameUrl
                pendingAppNavReason = "bấm Tải lại trang từ HUD (reload trực tiếp URL hiện tại)"
                com.example.data.AppLogger.info(
                    "RESET",
                    "Tải lại trang WebView từ HUD: urlHiệnTại=${com.example.data.AppLogger.safeUrlHostPath(curWvUrl)} | chế độ=${config.xboxWebUiMode.name} | nguồn=webView.reload()"
                )
                webViewInstance?.reload()
            },
            onToggleClarityBoost = { enabled ->
                SettingsRepository.updateClarityBoost(enabled)
                webViewInstance?.evaluateJavascript("if (window.__toggleClarityBoost) window.__toggleClarityBoost($enabled);", null)
            },
            clarityBoostActive = config.clarityBoostEnabled,
            translationActive = effectiveTranslate,
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
                com.example.data.AppLogger.info("UI", "Mở hộp thoại Dịch tự động từ HUD trong game")
                showTranslatorDialog = true
            },
            onToggleOrientation = {
                com.example.data.AppLogger.info("UI", "Bấm Xoay ngang/dọc màn hình từ HUD")
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
            onToggleRecording = {
                com.example.cloudplay.ZeroLagScreenRecorder.toggleRecording(context, webViewInstance)
            },
            xboxWebUiMode = config.xboxWebUiMode,
            onSelectXboxWebUiMode = { uiMode ->
                val rawCur = webViewInstance?.url ?: currentMainFrameUrl.ifBlank { lastXboxUrl }
                SettingsRepository.updateXboxWebUiMode(uiMode)
                if (uiMode == com.example.data.XboxWebUiMode.CLASSIC) {
                    switchWebViewToClassicUi("chuyển nhanh sang Giao diện cũ (CLASSIC) từ HUD")
                } else {
                    val target = SettingsRepository.formatXboxUrlWithLocale(
                        rawCur,
                        effectiveLocale,
                        uiMode
                    )
                    pendingAppNavReason = "chuyển nhanh sang Giao diện mới (NEW_UI) từ HUD"
                    com.example.data.AppLogger.info(
                        "WEB-UI",
                        "Chuyển nhanh Giao diện web Xbox: chế độ=${uiMode.name} | từ=${com.example.data.AppLogger.safeUrlHostPath(rawCur)} -> đích=${com.example.data.AppLogger.safeUrlHostPath(target)}"
                    )
                    webViewInstance?.loadUrl(target)
                }
                val toastMsg = if (uiMode == com.example.data.XboxWebUiMode.CLASSIC) {
                    config.tr("Đã chuyển sang Giao diện cũ (xbox.com/play)", "Switched to Classic UI (xbox.com/play)")
                } else {
                    config.tr("Đã chuyển sang Giao diện mới (play.xbox.com)", "Switched to New UI (play.xbox.com)")
                }
                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
            },
            onHudOpenStateChanged = { open -> isHudOpen = open }
        )

        // In-Game Quick Settings Sheet (Opened from "Cài đặt" in Expanded HUD)
        if (showQuickSettingsDialog) {
            com.example.ui.components.QuickSettingsSheetDialog(
                config = config,
                onToggleVirtualController = { enabled ->
                    SettingsRepository.updateShowVirtualController(enabled)
                    webViewInstance?.evaluateJavascript(
                        "if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled($enabled);",
                        null
                    )
                },
                onSelectXboxWebUiMode = { uiMode ->
                    val rawCur = webViewInstance?.url ?: currentMainFrameUrl.ifBlank { lastXboxUrl }
                    SettingsRepository.updateXboxWebUiMode(uiMode)
                    if (uiMode == com.example.data.XboxWebUiMode.CLASSIC) {
                        switchWebViewToClassicUi("bấm chọn Giao diện cũ (CLASSIC) trong Cài đặt nhanh")
                    } else {
                        val target = SettingsRepository.formatXboxUrlWithLocale(
                            rawCur,
                            effectiveLocale,
                            uiMode
                        )
                        pendingAppNavReason = "bấm chọn Giao diện mới (NEW_UI) trong Cài đặt nhanh"
                        com.example.data.AppLogger.info(
                            "WEB-UI",
                            "Chọn nhanh Giao diện web Xbox trong HUD: chế độ=${uiMode.name} | từ=${com.example.data.AppLogger.safeUrlHostPath(rawCur)} -> đích=${com.example.data.AppLogger.safeUrlHostPath(target)}"
                        )
                        webViewInstance?.loadUrl(target)
                    }
                },
                onToggleRecording = {
                    val wasRecording = com.example.cloudplay.ZeroLagScreenRecorder.state.value.isRecording
                    if (!wasRecording) {
                        showQuickSettingsDialog = false
                    }
                    com.example.cloudplay.ZeroLagScreenRecorder.toggleRecording(context, webViewInstance)
                },
                onDismiss = {
                    com.example.data.AppLogger.info("UI", "Đóng bảng Tùy chỉnh nhanh Hiển thị & Điều khiển trong game")
                    showQuickSettingsDialog = false
                }
            )
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

@Composable
private fun IsolatedInGameLoadingOverlay(
    isLoading: Boolean,
    progress: Float,
    hasGamepad: Boolean,
    gameProfile: com.example.data.GameOptimizationProfile,
    lowEndDeviceMode: Boolean,
    onNavigateHome: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vpnSnapshot = if (isLoading) {
        val liveVpn by VpnManager.state.collectAsState()
        liveVpn
    } else {
        remember { VpnManager.state.value }
    }
    com.example.ui.components.InGameLoadingOverlay(
        isLoading = isLoading,
        progress = progress,
        targetServerName = "${vpnSnapshot.currentServer.flag} ${vpnSnapshot.currentServer.name}",
        hasGamepad = hasGamepad,
        gameProfile = gameProfile,
        realLatencyMs = if (vpnSnapshot.mode == NetworkMode.DIRECT_NETWORK) vpnSnapshot.directLatencyMs else vpnSnapshot.latencyMs,
        lowEndDeviceMode = lowEndDeviceMode,
        onNavigateHome = onNavigateHome,
        onSkip = onSkip,
        modifier = modifier
    )
}

@Composable
private fun IsolatedStatusBanner(
    streamController: StreamController,
    isEnglishUI: Boolean,
    modifier: Modifier = Modifier
) {
    val streamUiState by streamController.uiState.collectAsState()
    val vpnMode by remember {
        VpnManager.state.map { it.mode }.distinctUntilChanged()
    }.collectAsState(initial = VpnManager.state.value.mode)

    AnimatedVisibility(
        visible = streamUiState.showBanner,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
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
                            text = if (isEnglishUI) "Cancel" else "Hủy",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                } else if (streamUiState.bannerType == BannerType.WARNING && vpnMode == NetworkMode.VPN_JAPAN) {
                    Button(
                        onClick = { streamController.forceSwitchToDirectNetwork() },
                        colors = ButtonDefaults.buttonColors(containerColor = XboxNeonGreen),
                        modifier = Modifier.testTag("disconnect_vpn_banner_button")
                    ) {
                        Text(
                            text = if (isEnglishUI) "Disconnect VPN" else "Ngắt VPN ngay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IsolatedFloatingHud(
    isGameActive: Boolean,
    virtualControllerEnabled: Boolean,
    physicalControllerConnected: Boolean,
    controllerName: String,
    onToggleNetwork: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateHome: () -> Unit,
    onReloadPage: () -> Unit,
    onToggleClarityBoost: (Boolean) -> Unit,
    clarityBoostActive: Boolean,
    translationActive: Boolean,
    onToggleTranslation: (Boolean) -> Unit,
    hideStreamStats: Boolean,
    onToggleHideStreamStats: (Boolean) -> Unit,
    onSelectLadderStep: (Int) -> Unit,
    onToggleAutoAdapting: (Boolean) -> Unit,
    onSelectCellularMode: (com.example.data.CellularOptimizationMode) -> Unit,
    onToggleWifiSpeedFirst: (Boolean) -> Unit,
    gameProfile: com.example.data.GameOptimizationProfile?,
    onOpenTranslator: () -> Unit,
    onToggleOrientation: () -> Unit,
    videoTopRatio: Float,
    lowEndDeviceMode: Boolean,
    onSelectFortniteControlMode: (Boolean) -> Unit,
    onToggleRecording: () -> Unit,
    xboxWebUiMode: com.example.data.XboxWebUiMode = com.example.data.XboxWebUiMode.CLASSIC,
    onSelectXboxWebUiMode: (com.example.data.XboxWebUiMode) -> Unit = {},
    onHudOpenStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val config by SettingsRepository.config.collectAsState()
    val isTvMode = remember(config.appUiMode, context) {
        config.isEffectiveTvMode(context)
    }

    var isHudOpenInternal by remember { mutableStateOf(false) }
    val needFullLiveHudState = isHudOpenInternal || (isTvMode && !hideStreamStats)

    val networkStatus by remember {
        com.example.cloudplay.NetworkOptimizer.status.distinctUntilChanged { old, new ->
            old.isWifi == new.isWifi &&
                old.isCellular == new.isCellular &&
                old.hasVpnTransport == new.hasVpnTransport &&
                old.wifiRssiDbm == new.wifiRssiDbm &&
                old.wifiFrequencyMhz == new.wifiFrequencyMhz
        }
    }.collectAsState(initial = com.example.cloudplay.NetworkOptimizer.status.value)

    val recorderState by com.example.cloudplay.ZeroLagScreenRecorder.state.collectAsState()

    val vpnState = if (needFullLiveHudState) {
        val liveVpn by VpnManager.state.collectAsState()
        liveVpn
    } else {
        remember { VpnManager.state.value }
    }

    val translatedCount = if (needFullLiveHudState) {
        val liveTrans by XboxBridge.translationInfo.collectAsState()
        liveTrans.translatedCount
    } else {
        0
    }

    val adaptiveQualityState = if (needFullLiveHudState) {
        val liveAdaptive by com.example.cloudplay.AdaptiveQualityManager.state.collectAsState()
        liveAdaptive
    } else {
        remember { com.example.cloudplay.AdaptiveQualityManager.state.value }
    }

    val telemetry = if (needFullLiveHudState) {
        val liveTelemetry by com.example.cloudplay.NetworkOptimizer.telemetry.collectAsState()
        liveTelemetry
    } else {
        val minimalPingPair by remember {
            com.example.cloudplay.NetworkOptimizer.telemetry
                .map { it.rttMs to it.hasStatsSample }
                .distinctUntilChanged()
        }.collectAsState(
            initial = com.example.cloudplay.NetworkOptimizer.telemetry.value.let { it.rttMs to it.hasStatsSample }
        )
        remember(minimalPingPair.first, minimalPingPair.second) {
            com.example.cloudplay.StreamTelemetry(
                rttMs = minimalPingPair.first,
                hasStatsSample = minimalPingPair.second
            )
        }
    }

    val handleHudOpenChanged: (Boolean) -> Unit = { open ->
        isHudOpenInternal = open
        onHudOpenStateChanged(open)
    }

    if (isTvMode) {
        com.example.ui.tv.TvInGameFloatingHud(
            config = config,
            vpnState = vpnState,
            telemetry = telemetry,
            clarityBoostActive = clarityBoostActive,
            translationActive = translationActive,
            hideStreamStats = hideStreamStats,
            recorderState = recorderState,
            xboxWebUiMode = xboxWebUiMode,
            onToggleClarityBoost = onToggleClarityBoost,
            onToggleTranslation = onToggleTranslation,
            onToggleHideStreamStats = onToggleHideStreamStats,
            onToggleRecording = onToggleRecording,
            onToggleNetwork = onToggleNetwork,
            onSelectXboxWebUiMode = onSelectXboxWebUiMode,
            onReloadPage = onReloadPage,
            onOpenQuickSettings = onOpenSettings,
            onNavigateHome = onNavigateHome,
            onHudOpenStateChanged = handleHudOpenChanged
        )
    } else {
        FloatingHud(
            vpnState = vpnState,
            isGameActive = isGameActive,
            virtualControllerEnabled = virtualControllerEnabled,
            physicalControllerConnected = physicalControllerConnected,
            controllerName = controllerName,
            onToggleNetwork = onToggleNetwork,
            onToggleVirtualController = onToggleVirtualController,
            onOpenSettings = onOpenSettings,
            onNavigateHome = onNavigateHome,
            onReloadPage = onReloadPage,
            onToggleClarityBoost = onToggleClarityBoost,
            clarityBoostActive = clarityBoostActive,
            translationActive = translationActive,
            translatedCount = translatedCount,
            onToggleTranslation = onToggleTranslation,
            hideStreamStats = hideStreamStats,
            onToggleHideStreamStats = onToggleHideStreamStats,
            networkStatus = networkStatus,
            telemetry = telemetry,
            adaptiveQualityState = adaptiveQualityState,
            onSelectLadderStep = onSelectLadderStep,
            onToggleAutoAdapting = onToggleAutoAdapting,
            onSelectCellularMode = onSelectCellularMode,
            onToggleWifiSpeedFirst = onToggleWifiSpeedFirst,
            gameProfile = gameProfile,
            onOpenTranslator = onOpenTranslator,
            onToggleOrientation = onToggleOrientation,
            videoTopRatio = videoTopRatio,
            lowEndDeviceMode = lowEndDeviceMode,
            onSelectFortniteControlMode = onSelectFortniteControlMode,
            recorderState = recorderState,
            onToggleRecording = onToggleRecording,
            xboxWebUiMode = xboxWebUiMode,
            onSelectXboxWebUiMode = onSelectXboxWebUiMode,
            onHudOpenStateChanged = handleHudOpenChanged
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ClassicUiBounceReminderChip(
    config: com.example.data.AppConfig,
    onRetryClassic: () -> Unit,
    onContinueNewUi: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val minSide = minOf(maxWidth, maxHeight)
        val unit = minSide * 0.01f
        val outerMarginH = unit * 3.5f
        val outerMarginBottom = unit * 11.0f
        val padH = unit * 3.2f
        val padV = unit * 2.2f
        val gap = unit * 2.0f
        val cornerRadius = unit * 3.0f
        val btnRadius = unit * 2.2f
        val borderStroke = unit * 0.28f
        val chipPadH = unit * 2.6f
        val chipPadV = unit * 1.4f
        val textFontSp = with(density) { (unit * 3.1f).toSp() }
        val actionFontSp = with(density) { (unit * 2.9f).toSp() }

        val messageText = config.tr(
            "Xbox đang tự chuyển về giao diện mới (play.xbox.com). Đã dừng tự thử lại.",
            "Xbox is redirecting to the new UI (play.xbox.com). Auto-retry paused."
        )
        val retryLabel = config.tr("Thử lại", "Retry")
        val continueNewUiLabel = config.tr("Dùng giao diện mới", "Use New UI")
        val dismissLabel = config.tr("Đóng", "Dismiss")

        Box(
            modifier = Modifier
                .padding(start = outerMarginH, end = outerMarginH, bottom = outerMarginBottom)
                .clip(RoundedCornerShape(cornerRadius))
                .background(XboxSurface)
                .border(borderStroke, XboxCardBorder, RoundedCornerShape(cornerRadius))
                .padding(horizontal = padH, vertical = padV)
                .testTag("classic_bounce_reminder_banner")
        ) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = messageText,
                    color = TextPrimary,
                    fontSize = textFontSp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .align(Alignment.CenterVertically)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(btnRadius))
                            .background(XboxNeonGreen)
                            .clickable(onClick = onRetryClassic)
                            .padding(horizontal = chipPadH, vertical = chipPadV)
                            .testTag("classic_bounce_retry_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = retryLabel,
                            color = Color.Black,
                            fontSize = actionFontSp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(btnRadius))
                            .background(XboxCardBorder)
                            .clickable(onClick = onContinueNewUi)
                            .padding(horizontal = chipPadH, vertical = chipPadV)
                            .testTag("classic_bounce_continue_new_ui_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = continueNewUiLabel,
                            color = TextPrimary,
                            fontSize = actionFontSp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(btnRadius))
                            .border(borderStroke, XboxCardBorder, RoundedCornerShape(btnRadius))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = chipPadH, vertical = chipPadV)
                            .testTag("classic_bounce_dismiss_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissLabel,
                            color = TextSecondary,
                            fontSize = actionFontSp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private data class UnlockFailBannerSlice(
    val promptServer: com.example.vpn.ServerLocation?,
    val suggestedServer: com.example.vpn.ServerLocation?,
    val suggestedPingMs: Int?,
    val consecutiveCount: Int,
    val autoSwitchedFromServer: com.example.vpn.ServerLocation?,
    val currentServer: com.example.vpn.ServerLocation
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun IsolatedServerUnlockFailReminderChip(
    config: com.example.data.AppConfig,
    effectiveLocale: String,
    webViewInstance: WebView?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val slice by remember {
        VpnManager.state.map { s ->
            UnlockFailBannerSlice(
                promptServer = s.unlockFailPromptServer,
                suggestedServer = s.unlockFailSuggestedServer,
                suggestedPingMs = s.unlockFailSuggestedPingMs,
                consecutiveCount = s.unlockFailConsecutiveCount,
                autoSwitchedFromServer = s.unlockFailAutoSwitchedFromServer,
                currentServer = s.currentServer
            )
        }.distinctUntilChanged()
    }.collectAsState(
        initial = VpnManager.state.value.let { s ->
            UnlockFailBannerSlice(
                promptServer = s.unlockFailPromptServer,
                suggestedServer = s.unlockFailSuggestedServer,
                suggestedPingMs = s.unlockFailSuggestedPingMs,
                consecutiveCount = s.unlockFailConsecutiveCount,
                autoSwitchedFromServer = s.unlockFailAutoSwitchedFromServer,
                currentServer = s.currentServer
            )
        }
    )

    val autoSwitchedFrom = slice.autoSwitchedFromServer
    val promptServer = slice.promptServer

    // Khi đang ở chế độ tự chọn và VpnManager đã tự đổi sang máy chủ khác có số đo thật,
    // cập nhật vùng cho WebView và tải lại trang danh mục để mở khóa ngay.
    LaunchedEffect(autoSwitchedFrom?.id, slice.currentServer.id, webViewInstance) {
        if (autoSwitchedFrom != null && webViewInstance != null && autoSwitchedFrom.id != slice.currentServer.id) {
            val safeCountry = org.json.JSONObject.quote(slice.currentServer.countryCode)
            val safeIp = org.json.JSONObject.quote(slice.currentServer.ip)
            val safeLocale = org.json.JSONObject.quote(effectiveLocale)
            webViewInstance.evaluateJavascript(
                "window.__lastUnlockSignal = null; if (window.__updateRegionAndLocale) window.__updateRegionAndLocale({ country: $safeCountry, ip: $safeIp, locale: $safeLocale }); location.reload();",
                null
            )
        }
    }

    if (promptServer == null && autoSwitchedFrom == null) return

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val minSide = minOf(maxWidth, maxHeight)
        val unit = minSide * 0.01f
        val outerMarginH = unit * 3.5f
        val outerMarginBottom = unit * 11.0f
        val padH = unit * 3.2f
        val padV = unit * 2.2f
        val gap = unit * 2.0f
        val cornerRadius = unit * 3.0f
        val btnRadius = unit * 2.2f
        val borderStroke = unit * 0.28f
        val chipPadH = unit * 2.6f
        val chipPadV = unit * 1.4f
        val textFontSp = with(density) { (unit * 3.1f).toSp() }
        val actionFontSp = with(density) { (unit * 2.9f).toSp() }

        val pingSuffix = slice.suggestedPingMs?.takeIf { it > 0 }?.let { " · $it ms" } ?: ""
        val messageText = if (autoSwitchedFrom != null) {
            val failedLabel = "${autoSwitchedFrom.flag} ${autoSwitchedFrom.name}".trim()
            val targetSrv = slice.suggestedServer ?: slice.currentServer
            val targetLabel = "${targetSrv.flag} ${targetSrv.name}".trim()
            "${config.tr("Máy chủ không mở khóa được vùng:", "Server cannot unlock region:")} $failedLabel. " +
                "${config.tr("Đã tự chuyển sang máy chủ đo thật:", "Auto-switched to measured server:")} $targetLabel$pingSuffix"
        } else {
            val failed = promptServer ?: return@BoxWithConstraints
            val failedLabel = "${failed.flag} ${failed.name}".trim()
            val suggested = slice.suggestedServer
            val baseMsg = "${config.tr("Máy chủ không mở khóa được vùng:", "Server cannot unlock region:")} $failedLabel."
            if (suggested != null) {
                val suggestedLabel = "${suggested.flag} ${suggested.name}".trim()
                "$baseMsg ${config.tr("Gợi ý máy chủ đã đo thật:", "Suggested measured server:")} $suggestedLabel$pingSuffix"
            } else {
                "$baseMsg ${config.tr("Máy chủ này không mở khóa được vùng, đổi sang máy chủ khác?", "This server cannot unlock the region, switch to another server?")}"
            }
        }

        val switchLabel = config.tr("Chuyển máy chủ", "Switch server")
        val dismissLabel = config.tr("Đóng", "Dismiss")

        Box(
            modifier = Modifier
                .padding(start = outerMarginH, end = outerMarginH, bottom = outerMarginBottom)
                .clip(RoundedCornerShape(cornerRadius))
                .background(XboxSurface)
                .border(borderStroke, XboxCardBorder, RoundedCornerShape(cornerRadius))
                .padding(horizontal = padH, vertical = padV)
                .testTag("webview_unlock_fail_reminder_banner")
        ) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = messageText,
                    color = TextPrimary,
                    fontSize = textFontSp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .align(Alignment.CenterVertically)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    if (autoSwitchedFrom == null && promptServer != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(btnRadius))
                                .background(XboxNeonGreen)
                                .clickable {
                                    val nextServer = VpnManager.confirmSwitchToNextBestServer(context)
                                    if (nextServer != null && webViewInstance != null) {
                                        val safeCountry = org.json.JSONObject.quote(nextServer.countryCode)
                                        val safeIp = org.json.JSONObject.quote(nextServer.ip)
                                        val safeLocale = org.json.JSONObject.quote(effectiveLocale)
                                        webViewInstance.evaluateJavascript(
                                            "window.__lastUnlockSignal = null; if (window.__updateRegionAndLocale) window.__updateRegionAndLocale({ country: $safeCountry, ip: $safeIp, locale: $safeLocale }); location.reload();",
                                            null
                                        )
                                    }
                                }
                                .padding(horizontal = chipPadH, vertical = chipPadV)
                                .testTag("webview_unlock_fail_switch_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = switchLabel,
                                color = Color.Black,
                                fontSize = actionFontSp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(btnRadius))
                            .border(borderStroke, XboxCardBorder, RoundedCornerShape(btnRadius))
                            .clickable { VpnManager.dismissUnlockFailPrompt() }
                            .padding(horizontal = chipPadH, vertical = chipPadV)
                            .testTag("webview_unlock_fail_dismiss_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissLabel,
                            color = TextSecondary,
                            fontSize = actionFontSp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}




