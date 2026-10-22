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
    private var sessionSampleJob: Job? = null

    private var sessionStartTimeMillis: Long = 0L
    private var sessionPingSum: Long = 0L
    private var sessionPingCount: Int = 0
    private var sessionFpsSum: Long = 0L
    private var sessionFpsCount: Int = 0
    private var sessionDecodeSum: Double = 0.0
    private var sessionDecodeCount: Int = 0
    private var sessionThermalHot: Boolean = false
    private val sessionPingSamples = mutableListOf<Int>()

    init {
        com.example.data.SessionHistoryRepository.init(context)
        com.example.data.GameLibraryRepository.init(context)
    }

    /**
     * Cancels active coroutines when WebView or Activity is destroyed to prevent memory leaks.
     */
    fun release() {
        finalizeActiveSessionIfNeeded()
        countdownJob?.cancel()
        countdownJob = null
        bannerDismissJob?.cancel()
        bannerDismissJob = null
        launchFallbackJob?.cancel()
        launchFallbackJob = null
        sessionSampleJob?.cancel()
        sessionSampleJob = null
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    fun cancel() {
        release()
    }

    fun onWebGameMetadataDetected(
        title: String?,
        url: String,
        imageUrl: String?,
        supportsTouch: Boolean?
    ) {
        scope.launch {
            val bookmark = com.example.data.GameLibraryRepository.onWebGamePlayedOrDiscovered(
                rawTitle = title,
                rawUrl = url.ifBlank { _uiState.value.currentUrl },
                imageUrl = imageUrl,
                supportsTouch = supportsTouch,
                markAsLastPlayed = true
            )
            if (bookmark != null && !bookmark.title.equals("Xbox Cloud Game", ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(gameTitle = bookmark.title)
            }
        }
    }

    fun onGameLaunchInitiated(title: String) {
        scope.launch {
            launchFallbackJob?.cancel()
            countdownJob?.cancel()

            val existingTitle = _uiState.value.gameTitle
            val isIncomingGeneric = title.isBlank() ||
                title.equals("Xbox Cloud Game", ignoreCase = true) ||
                title.equals("Xbox Game", ignoreCase = true)
            val resolvedTitle = if (isIncomingGeneric && !existingTitle.isNullOrBlank() &&
                !existingTitle.equals("Xbox Cloud Game", ignoreCase = true) &&
                !existingTitle.equals("Xbox Game", ignoreCase = true)
            ) {
                existingTitle
            } else {
                com.example.data.SessionHistoryRepository.resolveCleanGameTitle(title, _uiState.value.currentUrl)
            }

            val syncedBookmark = com.example.data.GameLibraryRepository.onWebGamePlayedOrDiscovered(
                rawTitle = resolvedTitle,
                rawUrl = _uiState.value.currentUrl,
                markAsLastPlayed = true
            )
            val finalTitle = syncedBookmark?.title ?: resolvedTitle

            _uiState.value = _uiState.value.copy(
                isGameActive = true,
                gameTitle = finalTitle,
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

            if (sessionStartTimeMillis == 0L) {
                startSessionTracking()
            }

            if (_uiState.value.isStreamConnected && _uiState.value.autoBypassTriggered) return@launch

            val updatedTitle = com.example.data.SessionHistoryRepository.resolveCleanGameTitle(
                _uiState.value.gameTitle,
                _uiState.value.currentUrl
            )
            val syncedBookmark = com.example.data.GameLibraryRepository.onWebGamePlayedOrDiscovered(
                rawTitle = updatedTitle,
                rawUrl = _uiState.value.currentUrl,
                markAsLastPlayed = true
            )
            _uiState.value = _uiState.value.copy(
                isStreamConnected = true,
                isGameActive = true,
                gameTitle = syncedBookmark?.title ?: updatedTitle
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
            finalizeActiveSessionIfNeeded()
            _uiState.value = _uiState.value.copy(
                isStreamConnected = false,
                isGameActive = false
            )
        }
    }

    private fun startSessionTracking() {
        sessionSampleJob?.cancel()
        sessionStartTimeMillis = System.currentTimeMillis()
        sessionPingSum = 0L
        sessionPingCount = 0
        sessionFpsSum = 0L
        sessionFpsCount = 0
        sessionDecodeSum = 0.0
        sessionDecodeCount = 0
        sessionThermalHot = checkDeviceThermalModerateOrHigher()
        sessionPingSamples.clear()

        // Take initial sample and then sample every 5 seconds (up to 60 samples)
        sessionSampleJob = scope.launch {
            while (true) {
                captureTelemetrySample()
                delay(5000)
            }
        }
    }

    private fun captureTelemetrySample() {
        val t = NetworkOptimizer.telemetry.value
        val vpn = VpnManager.state.value
        val ping = when {
            t.rttMs > 0 -> t.rttMs
            vpn.mode == NetworkMode.DIRECT_NETWORK -> vpn.directLatencyMs ?: vpn.latencyMs ?: 0
            else -> vpn.latencyMs ?: 0
        }
        if (ping > 0) {
            sessionPingSum += ping
            sessionPingCount += 1
            if (sessionPingSamples.size >= 60) {
                sessionPingSamples.removeAt(0)
            }
            sessionPingSamples.add(ping)
        }
        if (t.fps > 0) {
            sessionFpsSum += t.fps
            sessionFpsCount += 1
        }
        if (t.decodeTimeMs > 0.0) {
            sessionDecodeSum += t.decodeTimeMs
            sessionDecodeCount += 1
        }
        if (!sessionThermalHot && checkDeviceThermalModerateOrHigher()) {
            sessionThermalHot = true
        }
    }

    private fun checkDeviceThermalModerateOrHigher(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            (pm?.currentThermalStatus ?: 0) >= android.os.PowerManager.THERMAL_STATUS_MODERATE
        } catch (_: Exception) {
            false
        }
    }

    private fun finalizeActiveSessionIfNeeded() {
        val startMs = sessionStartTimeMillis
        if (startMs <= 0L) return
        sessionStartTimeMillis = 0L
        sessionSampleJob?.cancel()
        sessionSampleJob = null

        val nowMs = System.currentTimeMillis()
        val durationSec = ((nowMs - startMs) / 1000L).coerceAtLeast(0L)
        if (durationSec < 30L) {
            sessionPingSamples.clear()
            return
        }

        // Capture one last sample if none were recorded yet
        if (sessionPingCount == 0 && sessionFpsCount == 0 && sessionDecodeCount == 0) {
            captureTelemetrySample()
        }

        val vpn = VpnManager.state.value
        val fallbackPing = if (vpn.mode == NetworkMode.DIRECT_NETWORK) {
            vpn.directLatencyMs ?: vpn.latencyMs ?: 0
        } else {
            vpn.latencyMs ?: 0
        }
        val avgPing = if (sessionPingCount > 0) {
            (sessionPingSum / sessionPingCount).toInt()
        } else {
            fallbackPing.coerceAtLeast(0)
        }
        val avgFps = if (sessionFpsCount > 0) {
            (sessionFpsSum / sessionFpsCount).toInt()
        } else {
            NetworkOptimizer.telemetry.value.fps.let { if (it > 0) it else 60 }
        }
        val rawAvgDecode = if (sessionDecodeCount > 0) {
            sessionDecodeSum / sessionDecodeCount
        } else {
            NetworkOptimizer.telemetry.value.decodeTimeMs
        }
        val avgDecode = ((rawAvgDecode * 10.0).toInt()) / 10.0
        val samplesSnapshot = if (sessionPingSamples.isNotEmpty()) {
            sessionPingSamples.takeLast(60)
        } else if (avgPing > 0) {
            listOf(avgPing)
        } else {
            emptyList()
        }
        sessionPingSamples.clear()

        val resolvedGameTitle = com.example.data.SessionHistoryRepository.resolveCleanGameTitle(
            _uiState.value.gameTitle,
            _uiState.value.currentUrl
        )

        val record = com.example.data.PlaySessionRecord(
            id = "sess_${startMs}",
            gameTitle = resolvedGameTitle,
            startTimeMillis = startMs,
            durationSeconds = durationSec,
            serverName = vpn.currentServer.name,
            isDirectNetwork = vpn.mode == NetworkMode.DIRECT_NETWORK,
            avgPingMs = avgPing,
            avgFps = avgFps,
            avgDecodeMs = avgDecode,
            isSlowDecode = avgDecode > 12.0,
            isThermalHot = sessionThermalHot,
            pingSamples = samplesSnapshot
        )
        com.example.data.SessionHistoryRepository.recordSession(record)
    }

    fun onUrlChanged(url: String) {
        scope.launch {
            val prevUrl = _uiState.value.currentUrl
            val wasInLaunch = prevUrl.contains("/launch/")
            val nowInLaunch = url.contains("/launch/")
            val isGamePage = url.contains("/play/games/") || nowInLaunch

            val detectedBookmark = if (isGamePage) {
                com.example.data.GameLibraryRepository.onWebGamePlayedOrDiscovered(
                    rawTitle = null,
                    rawUrl = url,
                    markAsLastPlayed = true
                )
            } else null

            _uiState.value = _uiState.value.copy(
                currentUrl = url,
                gameTitle = detectedBookmark?.title ?: _uiState.value.gameTitle
            )

            if (!wasInLaunch && nowInLaunch) {
                val resolvedTitle = detectedBookmark?.title ?: com.example.data.SessionHistoryRepository.resolveCleanGameTitle(
                    _uiState.value.gameTitle,
                    url
                )
                onGameLaunchInitiated(resolvedTitle)
            } else if (wasInLaunch && !nowInLaunch) {
                // Player exited running game stream back to catalog/home
                launchFallbackJob?.cancel()
                countdownJob?.cancel()
                finalizeActiveSessionIfNeeded()
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

