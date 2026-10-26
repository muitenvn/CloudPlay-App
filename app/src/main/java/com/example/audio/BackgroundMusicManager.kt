package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import com.example.R
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages ambient background music (window_seat_view.mp3) across Dashboard screens
 * (Home, Games, Servers, Settings) and optionally during gameplay.
 *
 * Key guarantees:
 * 1. Loops seamlessly using built-in Android MediaPlayer (no heavy external SDK).
 * 2. Auto-start on launch is OFF by default (volume 8% by default); users can start/stop manually
 *    via the Play/Stop button in Settings or enable auto-start on launch.
 * 3. Supports "Play music while in game" (default ON): if enabled and music is currently playing
 *    (and Low-End Mode is OFF), music continues playing at the same volume when entering game
 *    and when stream starts; if disabled (or Low-End Mode is ON), fades out over 800ms and releases.
 * 4. Uses AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK and AudioAttributes.ALLOW_CAPTURE_BY_NONE (API 29+)
 *    so background music never mutes/breaks WebRTC game audio and is NEVER recorded into screen
 *    recording videos.
 */
object BackgroundMusicManager {

    const val FADE_DURATION_MS = 800L
    private const val FADE_STEPS = 20
    private const val FADE_STEP_DELAY_MS = FADE_DURATION_MS / FADE_STEPS

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var mediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var isAppInForeground: Boolean = false
    private var isOnDashboardScreen: Boolean = true
    private var isInGameOrStreamActive: Boolean = false
    private var isScreenRecordingActive: Boolean = false
    private var hasAudioFocus: Boolean = false
    private var pausedByAudioFocusLoss: Boolean = false

    /**
     * Tracks whether music is desired in the current app session (either initialized from
     * auto-start setting `backgroundMusicEnabled`, or toggled manually via the Play/Stop button
     * in Settings).
     */
    private var isSessionPlaybackDesired: Boolean = false
    private var wasPlayingWhenEnteringGame: Boolean = false
    private var lastObservedAutoStartSetting: Boolean? = null

    private var currentAppliedVolume: Float = 0f

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val config = SettingsRepository.config.value
                val am = audioManager
                val inPhoneOrVoiceCall = am?.mode == AudioManager.MODE_IN_CALL ||
                    am?.mode == AudioManager.MODE_RINGTONE ||
                    am?.mode == AudioManager.MODE_IN_COMMUNICATION

                // When inside CloudBrowserScreen, Chromium WebView requests AUDIOFOCUS_GAIN for WebRTC.
                // If "Play music while in game" is enabled and there is no phone call, keep background
                // music playing at the same volume alongside the game audio.
                val shouldKeepPlayingWithWebRtc = isAppInForeground &&
                    (!isOnDashboardScreen || isInGameOrStreamActive) &&
                    config.backgroundMusicPlayInGame &&
                    !config.lowEndDeviceMode &&
                    !inPhoneOrVoiceCall &&
                    focusChange != AudioManager.AUDIOFOCUS_LOSS_TRANSIENT

                if (!shouldKeepPlayingWithWebRtc) {
                    hasAudioFocus = false
                    pausedByAudioFocusLoss = true
                    fadeOutAndRelease(keepFocus = true)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (pausedByAudioFocusLoss) {
                    pausedByAudioFocusLoss = false
                    evaluatePlaybackState()
                }
            }
        }
    }

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            audioManager = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val config = SettingsRepository.config.value
            lastObservedAutoStartSetting = config.backgroundMusicEnabled
            isSessionPlaybackDesired = config.backgroundMusicEnabled && !config.lowEndDeviceMode
        }
    }

    /**
     * Manually toggles playback from the Play/Stop button in Settings without requiring
     * "Auto-start music on launch" to be enabled.
     */
    fun toggleManualPlayback() {
        val currentlyActive = _isPlaying.value || isSessionPlaybackDesired
        setManualPlayback(!currentlyActive)
    }

    fun setManualPlayback(play: Boolean) {
        isSessionPlaybackDesired = play
        if (play) {
            wasPlayingWhenEnteringGame = true
            pausedByAudioFocusLoss = false
            evaluatePlaybackState()
        } else {
            wasPlayingWhenEnteringGame = false
            fadeOutAndRelease(keepFocus = false)
        }
    }

    fun onAppForeground() {
        isAppInForeground = true
        pausedByAudioFocusLoss = false
        evaluatePlaybackState()
    }

    fun onAppBackground() {
        isAppInForeground = false
        fadeOutAndRelease(keepFocus = false)
    }

    fun onScreenChanged(isDashboard: Boolean) {
        val wasOnDashboard = isOnDashboardScreen
        isOnDashboardScreen = isDashboard
        if (!isDashboard) {
            if (wasOnDashboard) {
                wasPlayingWhenEnteringGame = _isPlaying.value || isSessionPlaybackDesired
            }
            isInGameOrStreamActive = true
            evaluatePlaybackState()
        } else {
            isInGameOrStreamActive = false
            evaluatePlaybackState()
        }
    }

    fun onGameStreamStateChanged(isGameActive: Boolean, isStreamConnected: Boolean) {
        val streamNowActive = isGameActive || isStreamConnected
        if (streamNowActive) {
            if (!isInGameOrStreamActive) {
                wasPlayingWhenEnteringGame = _isPlaying.value || isSessionPlaybackDesired
            }
            isInGameOrStreamActive = true
            evaluatePlaybackState()
        } else if (isOnDashboardScreen) {
            isInGameOrStreamActive = false
            evaluatePlaybackState()
        }
    }

    fun onScreenRecordingStateChanged(isRecording: Boolean) {
        isScreenRecordingActive = isRecording
        // On Android 10+ (API 29+), AudioAttributes.ALLOW_CAPTURE_BY_NONE prevents screen recording
        // from capturing background music. On older Android (< API 29) or in Low-End Mode, stop music.
        if (isRecording && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || SettingsRepository.config.value.lowEndDeviceMode)) {
            fadeOutAndRelease(keepFocus = false)
        } else {
            evaluatePlaybackState()
        }
    }

    fun onSettingsUpdated(
        autoStartEnabled: Boolean,
        volume: Float,
        playInGameEnabled: Boolean,
        lowEndMode: Boolean
    ) {
        val clampedVolume = volume.coerceIn(0f, 1f)

        // If user explicitly toggled the "Auto-start music on launch" switch in Settings,
        // sync the current session playback state with their toggle action.
        if (lastObservedAutoStartSetting != autoStartEnabled) {
            lastObservedAutoStartSetting = autoStartEnabled
            isSessionPlaybackDesired = autoStartEnabled && !lowEndMode
            if (isSessionPlaybackDesired) {
                wasPlayingWhenEnteringGame = true
            }
        }

        if (lowEndMode) {
            isSessionPlaybackDesired = false
            wasPlayingWhenEnteringGame = false
            fadeOutAndRelease(keepFocus = false)
            return
        }

        if (!shouldPlayOnCurrentScreen()) {
            fadeOutAndRelease(keepFocus = false)
            return
        }

        val player = mediaPlayer
        if (player != null && player.isPlaying && fadeJob?.isActive != true) {
            currentAppliedVolume = clampedVolume
            try {
                player.setVolume(clampedVolume, clampedVolume)
            } catch (_: Exception) {
            }
        } else {
            evaluatePlaybackState()
        }
    }

    private fun shouldPlayOnCurrentScreen(): Boolean {
        val config = SettingsRepository.config.value
        if (!isAppInForeground || config.lowEndDeviceMode) return false
        if (isScreenRecordingActive && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        val inGameContext = !isOnDashboardScreen || isInGameOrStreamActive
        return if (inGameContext) {
            // In game: keep playing only if "Play music while in game" is ON and music was already playing
            config.backgroundMusicPlayInGame && (isSessionPlaybackDesired || wasPlayingWhenEnteringGame)
        } else {
            // On Dashboard (Home, Games, Servers, Settings)
            isSessionPlaybackDesired
        }
    }

    fun evaluatePlaybackState() {
        if (!shouldPlayOnCurrentScreen()) {
            fadeOutAndRelease(keepFocus = false)
            return
        }
        startWithFadeIn()
    }

    private fun buildAudioAttributes(): AudioAttributes {
        val builder = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Strictly prevents screen recording (AudioPlaybackCapture) from recording background music
            builder.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
        }
        return builder.build()
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        if (hasAudioFocus) return true

        // Use AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK so background music never mutes or interrupts
        // WebRTC game stream audio in WebView or other media streams.
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(buildAudioAttributes())
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
                .also { audioFocusRequest = it }
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {
        }
        hasAudioFocus = false
    }

    private fun startWithFadeIn() {
        val ctx = appContext ?: return
        if (!shouldPlayOnCurrentScreen()) return

        fadeJob?.cancel()

        val existingPlayer = mediaPlayer
        if (existingPlayer != null) {
            try {
                if (existingPlayer.isPlaying) {
                    _isPlaying.value = true
                    // Already playing at target volume or mid-fade; ensure smooth transition to target volume
                    val targetVolume = SettingsRepository.config.value.backgroundMusicVolume.coerceIn(0f, 1f)
                    if (kotlin.math.abs(currentAppliedVolume - targetVolume) <= 0.005f) {
                        currentAppliedVolume = targetVolume
                        existingPlayer.setVolume(targetVolume, targetVolume)
                        return
                    }
                    val startVolume = currentAppliedVolume.coerceIn(0f, 1f)
                    fadeJob = scope.launch {
                        for (step in 1..FADE_STEPS) {
                            if (!isActive || !shouldPlayOnCurrentScreen()) break
                            val fraction = step.toFloat() / FADE_STEPS.toFloat()
                            val v = startVolume + (targetVolume - startVolume) * fraction
                            currentAppliedVolume = v
                            try {
                                mediaPlayer?.setVolume(v, v)
                            } catch (_: Exception) {
                            }
                            delay(FADE_STEP_DELAY_MS)
                        }
                    }
                    return
                }
            } catch (_: Exception) {
                releasePlayerOnly()
            }
        }

        requestAudioFocus()

        try {
            val afd = ctx.resources.openRawResourceFd(R.raw.window_seat_view) ?: return
            val player = MediaPlayer()
            player.setAudioAttributes(buildAudioAttributes())
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.isLooping = true
            player.prepare()
            currentAppliedVolume = 0f
            player.setVolume(0f, 0f)
            player.start()
            mediaPlayer = player
            _isPlaying.value = true

            fadeJob = scope.launch {
                for (step in 1..FADE_STEPS) {
                    if (!isActive || !shouldPlayOnCurrentScreen()) break
                    val targetVolume = SettingsRepository.config.value.backgroundMusicVolume.coerceIn(0f, 1f)
                    val v = targetVolume * (step.toFloat() / FADE_STEPS.toFloat())
                    currentAppliedVolume = v
                    try {
                        mediaPlayer?.setVolume(v, v)
                    } catch (_: Exception) {
                    }
                    delay(FADE_STEP_DELAY_MS)
                }
            }
        } catch (_: Exception) {
            releasePlayerOnly()
            abandonAudioFocus()
        }
    }

    /**
     * Fades volume down over 800ms and then completely stops and releases MediaPlayer
     * so no player instance is kept in memory when disabled or stopped.
     */
    private fun fadeOutAndRelease(keepFocus: Boolean) {
        fadeJob?.cancel()
        _isPlaying.value = false
        val player = mediaPlayer
        if (player == null) {
            if (!keepFocus) abandonAudioFocus()
            return
        }

        val startVol = currentAppliedVolume.coerceIn(0f, 1f)
        if (startVol <= 0.005f) {
            releasePlayerOnly()
            if (!keepFocus) abandonAudioFocus()
            return
        }

        fadeJob = scope.launch {
            try {
                for (step in (FADE_STEPS - 1) downTo 0) {
                    if (!isActive) break
                    val v = startVol * (step.toFloat() / FADE_STEPS.toFloat())
                    currentAppliedVolume = v
                    try {
                        mediaPlayer?.setVolume(v, v)
                    } catch (_: Exception) {
                        break
                    }
                    delay(FADE_STEP_DELAY_MS)
                }
            } finally {
                releasePlayerOnly()
                if (!keepFocus) {
                    abandonAudioFocus()
                }
            }
        }
    }

    /**
     * Immediately stops and releases MediaPlayer and abandons audio focus without waiting.
     * Used when Activity is destroyed.
     */
    fun stopAndReleaseImmediately() {
        fadeJob?.cancel()
        fadeJob = null
        _isPlaying.value = false
        releasePlayerOnly()
        abandonAudioFocus()
    }

    private fun releasePlayerOnly() {
        currentAppliedVolume = 0f
        _isPlaying.value = false
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: Exception) {
        }
        try {
            player.reset()
        } catch (_: Exception) {
        }
        try {
            player.release()
        } catch (_: Exception) {
        }
    }
}
