package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.cloudplay.StreamController
import com.example.data.GameLibraryRepository
import com.example.data.SettingsRepository
import com.example.ui.CloudBrowserScreen
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.vpn.VpnManager


sealed class AppScreen {
    data object Dashboard : AppScreen()
    data class Browser(val url: String) : AppScreen()

    companion object {
        val Saver: Saver<AppScreen, String> = Saver(
            save = { screen ->
                when (screen) {
                    is Dashboard -> "DASHBOARD"
                    is Browser -> "BROWSER|${screen.url}"
                }
            },
            restore = { saved ->
                if (saved.startsWith("BROWSER|")) {
                    val url = saved.removePrefix("BROWSER|").ifBlank { "https://www.xbox.com/en-US/play" }
                    Browser(url)
                } else {
                    Dashboard
                }
            }
        )
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var streamController: StreamController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake while streaming Xbox games
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        com.example.data.AppLogger.installCrashHandler(applicationContext)
        VpnManager.init(applicationContext)
        SettingsRepository.init(applicationContext)
        GameLibraryRepository.init(applicationContext)
        com.example.data.SessionHistoryRepository.init(applicationContext)
        com.example.cloudplay.NetworkOptimizer.init(applicationContext)
        com.example.audio.BackgroundMusicManager.init(applicationContext)
        streamController = StreamController(applicationContext)
        lockDisplayRefreshRateTo60Hz(false)

        setContent {
            val config by SettingsRepository.config.collectAsState()
            val targetLocale: java.util.Locale = remember(config.appLanguage) {
                val langCode = config.appLanguage.effectiveLanguageCode()
                java.util.Locale.forLanguageTag(langCode)
            }

            val baseContext = androidx.compose.ui.platform.LocalContext.current
            val currentOrientation = baseContext.resources.configuration.orientation
            val currentUiMode = baseContext.resources.configuration.uiMode
            val (localizedConfiguration, localizedWrapper) = remember(targetLocale, currentOrientation, currentUiMode) {
                val conf = android.content.res.Configuration(baseContext.resources.configuration)
                conf.setLocale(targetLocale)
                conf.setLayoutDirection(targetLocale)
                val localizedCtx = baseContext.createConfigurationContext(conf)
                val wrapper = object : android.content.ContextWrapper(this@MainActivity), androidx.activity.result.ActivityResultRegistryOwner {
                    override fun getResources(): android.content.res.Resources = localizedCtx.resources
                    override fun getAssets(): android.content.res.AssetManager = localizedCtx.assets
                    override fun getTheme(): android.content.res.Resources.Theme = localizedCtx.theme
                    override val activityResultRegistry: androidx.activity.result.ActivityResultRegistry
                        get() = this@MainActivity.activityResultRegistry
                }
                Pair(localizedCtx.resources.configuration, wrapper)
            }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
                androidx.compose.ui.platform.LocalContext provides localizedWrapper,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                MyApplicationTheme {
                    MainContent(
                        activity = this@MainActivity,
                        streamController = streamController,
                        onConnectVpnRequested = { handleVpnConnect() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.audio.BackgroundMusicManager.onAppForeground()
    }

    override fun onPause() {
        super.onPause()
        com.example.audio.BackgroundMusicManager.onAppBackground()
    }

    override fun onStop() {
        super.onStop()
        com.example.audio.BackgroundMusicManager.onAppBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.audio.BackgroundMusicManager.stopAndReleaseImmediately()
        if (::streamController.isInitialized) {
            streamController.release()
        }
    }

    private var isBrowserScreenActive = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isBrowserScreenActive) {
            applyImmersiveFullscreen(true)
        }
    }

    fun applyImmersiveFullscreen(enable: Boolean) {
        isBrowserScreenActive = enable
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            lockDisplayRefreshRateTo60Hz(true)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            lockDisplayRefreshRateTo60Hz(false)
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            resetOrientation()
        }
    }

    /**
     * Khóa tần số quét màn hình ở 60Hz khi đang trong phiên chơi Cloud Gaming để khớp nhịp 60fps của WebRTC,
     * triệt tiêu hiện tượng lệch pha VSync (micro-stuttering trên màn 90Hz/120Hz) và giảm 3-4°C nhiệt độ máy.
     * Khi thoát game (lock60Hz = false), khôi phục về tần số quét mặc định của hệ thống.
     */
    private fun lockDisplayRefreshRateTo60Hz(lock60Hz: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this.display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val supportedModes = display?.supportedModes ?: return
                val lp = window.attributes
                if (lock60Hz) {
                    val currentMode = display.mode
                    val sameResModes = supportedModes.filter {
                        it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
                    }.ifEmpty { supportedModes.toList() }
                    val mode60Hz = sameResModes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
                    if (mode60Hz != null) {
                        lp.preferredDisplayModeId = mode60Hz.modeId
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            lp.preferredRefreshRate = 60f
                        }
                        window.attributes = lp
                    }
                } else {
                    lp.preferredDisplayModeId = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        lp.preferredRefreshRate = 0f
                    }
                    window.attributes = lp
                }
            }
        } catch (_: Exception) {
        }
    }

    fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun resetOrientation() {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun handleVpnConnect() {
        VpnManager.connect(this)
    }
}

@Composable
fun MainContent(
    activity: MainActivity,
    streamController: StreamController,
    onConnectVpnRequested: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentScreen by rememberSaveable(stateSaver = AppScreen.Saver) {
        mutableStateOf<AppScreen>(AppScreen.Dashboard)
    }

    // Runtime POST_NOTIFICATIONS permission request for Android 13+ (TIRAMISU / API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission granted: $isGranted")
    }

    val config by SettingsRepository.config.collectAsState()
    val updateState by com.example.data.UpdateChecker.state.collectAsState()
    val showUpdateDialog by com.example.data.UpdateChecker.showUpdateDialog.collectAsState()

    LaunchedEffect(config.autoCheckUpdatesOnLaunch) {
        if (config.autoCheckUpdatesOnLaunch) {
            com.example.data.UpdateChecker.checkOnAppLaunchIfNeeded(context)
        }
    }
    val availableUpdate = updateState as? com.example.data.UpdateCheckState.UpdateAvailable
    if (showUpdateDialog && availableUpdate != null) {
        com.example.ui.components.UpdateAvailableDialog(
            updateInfo = availableUpdate,
            isEn = config.isEnglishUI(),
            onDismiss = { com.example.data.UpdateChecker.dismissUpdateDialog() }
        )
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val streamUiState by streamController.uiState.collectAsState()

    // Synchronize BackgroundMusicManager with settings (auto-start, volume, play-in-game, low-end mode)
    LaunchedEffect(
        config.backgroundMusicEnabled,
        config.backgroundMusicVolume,
        config.backgroundMusicPlayInGame,
        config.lowEndDeviceMode
    ) {
        com.example.audio.BackgroundMusicManager.onSettingsUpdated(
            autoStartEnabled = config.backgroundMusicEnabled,
            volume = config.backgroundMusicVolume,
            playInGameEnabled = config.backgroundMusicPlayInGame,
            lowEndMode = config.lowEndDeviceMode
        )
    }

    // Stop background music immediately when stream connects or game becomes active
    LaunchedEffect(streamUiState.isGameActive, streamUiState.isStreamConnected) {
        com.example.audio.BackgroundMusicManager.onGameStreamStateChanged(
            isGameActive = streamUiState.isGameActive,
            isStreamConnected = streamUiState.isStreamConnected
        )
    }

    // Fullscreen Immersive Mode & Background Music controller for Cloud Gaming vs Dashboard
    LaunchedEffect(currentScreen) {
        val isBrowser = currentScreen is AppScreen.Browser
        com.example.audio.BackgroundMusicManager.onScreenChanged(isDashboard = !isBrowser)
        activity.applyImmersiveFullscreen(isBrowser)
        if (!isBrowser) {
            activity.resetOrientation()
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_navigation"
    ) { screen ->
        when (screen) {
            is AppScreen.Dashboard -> {
                DashboardScreen(
                    onLaunchUrl = { url ->
                        val config = SettingsRepository.config.value
                        if (config.smartVpnEnabled) {
                            if (VpnManager.needsVpnToPlay()) {
                                VpnManager.autoTurnOnForLaunch(context)
                            } else {
                                VpnManager.ensureDirectNetwork()
                            }
                        } else {
                            if (VpnManager.state.value.mode == com.example.vpn.NetworkMode.DISCONNECTED) {
                                VpnManager.connect(context)
                            }
                        }
                        val currentLocale = config.getEffectiveLocale()
                        val finalUrl = SettingsRepository.formatXboxUrlWithLocale(url, currentLocale)
                        currentScreen = AppScreen.Browser(finalUrl)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AppScreen.Browser -> {
                CloudBrowserScreen(
                    initialUrl = screen.url,
                    streamController = streamController,
                    onNavigateBackToDashboard = {
                        currentScreen = AppScreen.Dashboard
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

