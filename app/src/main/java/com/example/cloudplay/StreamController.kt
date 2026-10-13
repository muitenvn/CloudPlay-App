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

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onGameLaunchInitiated(title: String) {
        _uiState.value = _uiState.value.copy(
            isGameActive = true,
            gameTitle = title
        )

        val config = SettingsRepository.config.value
        val vpnState = VpnManager.state.value

        if (config.smartVpnEnabled) {
            if (VpnManager.needsVpnToPlay() && vpnState.mode != NetworkMode.VPN_JAPAN) {
                VpnManager.autoTurnOnForLaunch(context)
                showTemporaryBanner("🤖 Smart VPN: Tự động BẬT VPN Nhật Bản để vượt rào xác thực game...", BannerType.INFO, 3000)
            } else if (!VpnManager.needsVpnToPlay()) {
                VpnManager.ensureDirectNetwork()
            }
        }
    }

    fun onStreamConnected() {
        if (_uiState.value.isStreamConnected) return

        _uiState.value = _uiState.value.copy(
            isStreamConnected = true,
            isGameActive = true
        )

        val config = SettingsRepository.config.value
        val vpnState = VpnManager.state.value

        if ((config.smartVpnEnabled || config.autoBypassEnabled) && vpnState.mode == NetworkMode.VPN_JAPAN && !_uiState.value.autoBypassTriggered) {
            scheduleAutoBypass(config.autoBypassDelaySeconds, "WebRTC stream kết nối thành công")
        }
    }

    fun onStreamDisconnected() {
        _uiState.value = _uiState.value.copy(
            isStreamConnected = false,
            isGameActive = false
        )
    }

    fun onUrlChanged(url: String) {
        val wasInGame = _uiState.value.currentUrl.contains("/launch/")
        val nowInGame = url.contains("/launch/")

        _uiState.value = _uiState.value.copy(currentUrl = url)

        if (wasInGame && !nowInGame) {
            // Player returned to catalog/home
            _uiState.value = _uiState.value.copy(
                isGameActive = false,
                isStreamConnected = false,
                autoBypassTriggered = false
            )

            val config = SettingsRepository.config.value
            if ((config.smartVpnEnabled || config.autoReconnectOnMenu) && VpnManager.needsVpnToPlay()) {
                showTemporaryBanner("🎮 Đã thoát trận: Smart VPN tự động BẬT lại VPN để duyệt thư viện...", BannerType.INFO, 3500)
                VpnManager.autoTurnOnForLaunch(context)
            }
        }
    }

    fun cancelAutoBypass() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.value = _uiState.value.copy(
            countdownRemainingSeconds = null,
            showBanner = false
        )
    }

    fun forceSwitchToDirectNetwork() {
        cancelAutoBypass()
        executeBypass("Chuyển mạng thủ công")
    }

    private fun scheduleAutoBypass(delaySeconds: Int, triggerReason: String) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in delaySeconds downTo 1) {
                _uiState.value = _uiState.value.copy(
                    countdownRemainingSeconds = sec,
                    showBanner = true,
                    bannerMessage = "⚡ Smart VPN: Tự động TẮT VPN chuyển Mạng Nhà sau ${sec}s... (Chạm để Hủy)",
                    bannerType = BannerType.COUNTDOWN
                )
                delay(1000)
            }

            executeBypass(triggerReason)
        }
    }

    private fun executeBypass(reason: String) {
        _uiState.value = _uiState.value.copy(
            countdownRemainingSeconds = null,
            autoBypassTriggered = true,
            showBanner = true,
            bannerMessage = "⚡ Smart VPN: Đã TỰ ĐỘNG TẮT VPN khi vào game! Mạng Trực Tiếp Active (Ping thấp nhất).",
            bannerType = BannerType.SUCCESS
        )

        vibrateFeedback()
        VpnManager.switchToDirectNetwork(context, reason)

        // Dismiss banner after 4.5 seconds
        scope.launch {
            delay(4500)
            _uiState.value = _uiState.value.copy(showBanner = false)
        }
    }

    private fun showTemporaryBanner(message: String, type: BannerType, durationMs: Long = 3000) {
        _uiState.value = _uiState.value.copy(
            showBanner = true,
            bannerMessage = message,
            bannerType = type
        )
        scope.launch {
            delay(durationMs)
            _uiState.value = _uiState.value.copy(showBanner = false)
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
