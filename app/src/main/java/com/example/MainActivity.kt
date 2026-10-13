package com.example

import android.app.Activity
import android.net.VpnService
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.cloudplay.StreamController
import com.example.data.SettingsRepository
import com.example.ui.CloudBrowserScreen
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.vpn.VpnManager

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.auth.AuthManager
import com.example.ui.auth.SignInScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class AppScreen {
    data object Dashboard : AppScreen()
    data class Browser(val url: String) : AppScreen()
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {

    private lateinit var streamController: StreamController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake while streaming Xbox games
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        com.example.cloudplay.NetworkOptimizer.init(applicationContext)
        streamController = StreamController(applicationContext)

        setContent {
            MyApplicationTheme {
                val auth = FirebaseAuth.getInstance()
                val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

                val context = androidx.compose.ui.platform.LocalContext.current
                val credentialManager = remember { androidx.credentials.CredentialManager.create(context) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    AuthManager.attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {},
                        onUnauthenticated = {},
                        scope = scope
                    )
                }

                if (currentUser == null) {
                    SignInScreen(
                        onAuthSuccess = {}
                    )
                } else {
                    MainContent(
                        streamController = streamController,
                        onConnectVpnRequested = { handleVpnConnect() }
                    )
                }
            }
        }
    }

    private fun handleVpnConnect() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            // Permission needed, handled by compose launcher in MainContent
        } else {
            VpnManager.connect(this)
        }
    }
}

@Composable
fun MainContent(
    streamController: StreamController,
    onConnectVpnRequested: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }

    // Launcher for Android system VPN preparation intent
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnManager.connect(context)
        }
    }

    // Fullscreen Immersive Mode controller for Cloud Gaming
    LaunchedEffect(currentScreen) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            when (currentScreen) {
                is AppScreen.Browser -> {
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
                is AppScreen.Dashboard -> {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
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
