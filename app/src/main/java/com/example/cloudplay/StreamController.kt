package com.example.cloudplay

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.SettingsRepository
import com.example.vpn.NetworkMode
import com.example.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StreamUiState(
    val isGameActive: Boolean = false,
    val gameTitle: String? = null,
    val isStreamConnected: Boolean = false,
    val countdownRemainingSeconds: Int? = null,
    val autoBypassTriggered: Boolean = false,
    val currentUrl: String = "https://www.xbox.com/en-US/play",
    val showBanner: Boolean = false,
    val bannerMessage: String = "",
    val bannerType: BannerType = BannerType.INFO
)

enum class BannerType {
    INFO,
    COUNTDOWN,
    SUCCESS,
    WARNING
}

class StreamController(private val context: Context) {

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var bannerDismissJob: Job? = null
    private var launchFallbackJob: Job? = null

    /**
     * Cancels active coroutines when WebView or Activity is destroyed to prevent memory leaks.
     */
    fun release() {
        countdownJob?.cancel()
        countdownJob = null
        bannerDismissJob?.cancel()
        bannerDismissJob = null
        launchFallbackJob?.cancel()
        launchFallbackJob = null
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    fun cancel() {
        release()
    }

    fun onGameLaunchInitiated(title: String) {
        scope.launch {
            launchFallbackJob?.cancel()
            countdownJob?.cancel()

            _uiState.value = _uiState.value.copy(
                isGameActive = true,
                gameTitle = title,
                isStreamConnected = false,
                autoBypassTriggered = false
            )

            val config = SettingsRepository.config.value
            val isEn = config.isEnglishUI()
            val vpnState = VpnManager.state.value

            if (config.smartVpnEnabled || config.autoBypassEnabled) {
                if (vpnState.mode != NetworkMode.VPN_JAPAN) {
                    VpnManager.autoTurnOnForLaunch(context)
                    showTemporaryBanner(
                        if (isEn) "🌐 Auto-enabled region unlock VPN for game launch..." else "🌐 Đã tự động bật VPN mở khóa vùng để vào game...",
                        BannerType.INFO,
                        3500
                    )
                } else {
                    showTemporaryBanner(
                        if (isEn) "🚀 Launching game: VPN will auto-disconnect once stream connects..." else "🚀 Đang vào game: Sẽ tự động tắt VPN khi luồng game kết nối...",
                        BannerType.INFO,
                        3500
                    )
                }
            }

            // Fallback: if WebRTC / video event inside WebView hasn't fired within 5.5s on /launch/,
            // automatically trigger onStreamConnected() so VPN auto-disconnect and notification always run.
            launchFallbackJob = scope.launch {
                delay(5500)
                if (_uiState.value.isGameActive && !_uiState.value.isStreamConnected) {
                    onStreamConnected()
                }
            }
        }
    }

    fun onStreamConnected() {
        scope.launch {
            launchFallbackJob?.cancel()
            launchFallbackJob = null

            if (_uiState.value.isStreamConnected && _uiState.value.autoBypassTriggered) return@launch

            _uiState.value = _uiState.value.copy(
                isStreamConnected = true,
                isGameActive = true
            )

            val config = SettingsRepository.config.value
            val isEn = config.isEnglishUI()

            if ((config.smartVpnEnabled || config.autoBypassEnabled) && !_uiState.value.autoBypassTriggered) {
                val delaySec = config.autoBypassDelaySeconds.coerceIn(1, 15)
                try {
                    android.widget.Toast.makeText(
                        context,
                        if (isEn) "⚡ In-game! Auto-disconnecting VPN in ${delaySec}s..." else "⚡ Đã vào game! Tự động tắt VPN sau ${delaySec}s...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {}
                scheduleAutoBypass(delaySec, if (isEn) "Game stream connected" else "Luồng game đã kết nối")
            } else {
                showTemporaryBanner(
                    if (isEn) "✅ In-game! Using Direct ISP connection." else "✅ Đã vào game! Đang dùng mạng trực tiếp.",
                    BannerType.SUCCESS,
                    5000
                )
            }
        }
    }

    fun onStreamDisconnected() {
        scope.launch {
            launchFallbackJob?.cancel()
            launchFallbackJob = null
            _uiState.value = _uiState.value.copy(
                isStreamConnected = false,
                isGameActive = false
            )
        }
    }

    fun onUrlChanged(url: String) {
        scope.launch {
            val prevUrl = _uiState.value.currentUrl
            val wasInLaunch = prevUrl.contains("/launch/")
            val nowInLaunch = url.contains("/launch/")

            _uiState.value = _uiState.value.copy(currentUrl = url)

            if (!wasInLaunch && nowInLaunch) {
                onGameLaunchInitiated("Xbox Cloud Game")
            } else if (wasInLaunch && !nowInLaunch) {
                // Player exited running game stream back to catalog/home
                launchFallbackJob?.cancel()
                countdownJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isGameActive = false,
                    isStreamConnected = false,
                    autoBypassTriggered = false,
                    countdownRemainingSeconds = null
                )

                val config = SettingsRepository.config.value
                val isEn = config.isEnglishUI()
                if ((config.smartVpnEnabled || config.autoReconnectOnMenu) && VpnManager.needsVpnToPlay()) {
                    showTemporaryBanner(
                        if (isEn) "🎮 Exited game: Re-enabling region unlock VPN..." else "🎮 Đã thoát game: Tự động bật lại VPN mở khóa vùng...",
                        BannerType.INFO,
                        4000
                    )
                    VpnManager.autoTurnOnForLaunch(context)
                    try {
                        android.widget.Toast.makeText(
                            context,
                            if (isEn) "🌐 Exited game: Auto-reenabled region unlock VPN" else "🌐 Đã thoát game: Tự động bật lại VPN mở khóa vùng",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun cancelAutoBypass() {
        countdownJob?.cancel()
        countdownJob = null
        bannerDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(
            countdownRemainingSeconds = null,
            showBanner = false
        )
        val isEn = SettingsRepository.config.value.isEnglishUI()
        showTemporaryBanner(
            if (isEn) "🛡️ Kept VPN connection active as requested." else "🛡️ Đã giữ nguyên kết nối VPN theo yêu cầu.",
            BannerType.INFO,
            3000
        )
    }

    fun forceSwitchToDirectNetwork() {
        cancelAutoBypass()
        val isEn = SettingsRepository.config.value.isEnglishUI()
        executeBypass(if (isEn) "Manual network switch" else "Chuyển mạng thủ công")
    }

    private fun scheduleAutoBypass(delaySeconds: Int, triggerReason: String) {
        countdownJob?.cancel()
        bannerDismissJob?.cancel()
        countdownJob = scope.launch {
            for (sec in delaySeconds downTo 1) {
                val isEn = SettingsRepository.config.value.isEnglishUI()
                _uiState.value = _uiState.value.copy(
                    countdownRemainingSeconds = sec,
                    showBanner = true,
                    bannerMessage = if (isEn) {
                        "⚡ In-game: Auto-disconnecting VPN in ${sec}s... (Tap Cancel to keep)"
                    } else {
                        "⚡ Đã vào game: Tự động TẮT VPN sau ${sec}s... (Chạm Hủy nếu muốn giữ)"
                    },
                    bannerType = BannerType.COUNTDOWN
                )
                delay(1000)
            }

            executeBypass(triggerReason)
        }
    }

    private fun executeBypass(reason: String) {
        bannerDismissJob?.cancel()
        val isEn = SettingsRepository.config.value.isEnglishUI()
        _uiState.value = _uiState.value.copy(
            countdownRemainingSeconds = null,
            autoBypassTriggered = true,
            showBanner = true,
            bannerMessage = if (isEn) {
                "✅ VPN auto-disconnected! Now streaming over ultra-low latency Direct ISP."
            } else {
                "✅ Đã TỰ ĐỘNG TẮT VPN khi vào game! Đang dùng mạng trực tiếp mượt nhất."
            },
            bannerType = BannerType.SUCCESS
        )

        vibrateFeedback()
        VpnManager.switchToDirectNetwork(context, reason)
        scope.launch {
            try {
                android.widget.Toast.makeText(
                    context,
                    if (isEn) "✅ VPN Disconnected — Switched to Direct ISP!" else "✅ Đã TỰ ĐỘNG TẮT VPN — Chuyển sang mạng trực tiếp!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {}
        }

        // Dismiss banner after 6.5 seconds
        bannerDismissJob = scope.launch {
            delay(6500)
            _uiState.value = _uiState.value.copy(showBanner = false)
        }
    }

    private fun showTemporaryBanner(message: String, type: BannerType, durationMs: Long = 3500) {
        bannerDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(
            showBanner = true,
            bannerMessage = message,
            bannerType = type
        )
        bannerDismissJob = scope.launch {
            delay(durationMs)
            if (_uiState.value.bannerType == type) {
                _uiState.value = _uiState.value.copy(showBanner = false)
            }
        }
    }

    private fun vibrateFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (ignored: Exception) {
        }
    }
}

