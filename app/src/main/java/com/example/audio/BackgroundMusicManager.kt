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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages ambient background music (window_seat_view.mp3) across Dashboard screens
 * (Home, Games, Servers, Settings).
 *
 * Key guarantees:
 * 1. Loops seamlessly across Dashboard tabs using built-in Android MediaPlayer (no heavy external SDK).
 * 2. Fades in (800ms) when starting and fades out (800ms) before stopping/entering CloudBrowserScreen.
 * 3. Completely stops and releases MediaPlayer instance (null) when entering CloudBrowserScreen,
 *    when WebRTC game stream starts, when disabled in Settings, or when app goes to background.
 * 4. Sets AudioAttributes.ALLOW_CAPTURE_BY_NONE on Android 10+ (API 29+) so background music is
 *    NEVER recorded into screen recording videos, and never contends with WebRTC in-game audio latency.
 * 5. Handles AudioFocus (phone calls, other media apps) and lifecycle transitions automatically.
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

    private var isAppInForeground: Boolean = false
    private var isOnDashboardScreen: Boolean = true
    private var isInGameOrStreamActive: Boolean = false
    private var isScreenRecordingActive: Boolean = false
    private var hasAudioFocus: Boolean = false
    private var pausedByAudioFocusLoss: Boolean = false

    private var currentAppliedVolume: Float = 0f

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Stop when phone call arrives or another app takes audio focus; restore when focus returns
                hasAudioFocus = false
                pausedByAudioFocusLoss = true
                fadeOutAndRelease(keepFocus = true)
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
        }
    }

    fun onAppForeground() {
        isAppInForeground = true
        // If focus was lost while another app/call was active and user returned to app, re-evaluate
        pausedByAudioFocusLoss = false
        evaluatePlaybackState()
    }

    fun onAppBackground() {
        isAppInForeground = false
        fadeOutAndRelease(keepFocus = false)
    }

    fun onScreenChanged(isDashboard: Boolean) {
        isOnDashboardScreen = isDashboard
        if (!isDashboard) {
            isInGameOrStreamActive = true
            fadeOutAndRelease(keepFocus = false)
        } else {
            isInGameOrStreamActive = false
            evaluatePlaybackState()
        }
    }

    fun onGameStreamStateChanged(isGameActive: Boolean, isStreamConnected: Boolean) {
        val streamNowActive = isGameActive || isStreamConnected
        if (streamNowActive) {
            isInGameOrStreamActive = true
            // When actual WebRTC stream connects, cut any remaining fade and release immediately
            // so WebRTC audio track has zero latency contention
            if (isStreamConnected) {
                stopAndReleaseImmediately()
            } else {
                fadeOutAndRelease(keepFocus = false)
            }
        } else if (isOnDashboardScreen) {
            isInGameOrStreamActive = false
            evaluatePlaybackState()
        }
    }

    fun onScreenRecordingStateChanged(isRecording: Boolean) {
        isScreenRecordingActive = isRecording
        if (isRecording) {
            stopAndReleaseImmediately()
        } else {
            evaluatePlaybackState()
        }
    }

    fun onSettingsUpdated(enabled: Boolean, volume: Float, lowEndMode: Boolean) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        val shouldPlay = enabled && !lowEndMode && shouldPlayOnCurrentScreen()
        if (!shouldPlay) {
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
        return isAppInForeground &&
            isOnDashboardScreen &&
            !isInGameOrStreamActive &&
            !isScreenRecordingActive &&
            config.backgroundMusicEnabled &&
            !config.lowEndDeviceMode
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
            // Prevents screen recording (AudioPlaybackCapture) from recording background music
            builder.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
        }
        return builder.build()
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(buildAudioAttributes())
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
                .also { audioFocusRequest = it }
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
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
                    // Already playing or mid-fade; ramp smoothly to target volume
                    val targetVolume = SettingsRepository.config.value.backgroundMusicVolume.coerceIn(0f, 1f)
                    val startVolume = currentAppliedVolume.coerceIn(0f, targetVolume)
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

        if (!requestAudioFocus()) {
            return
        }

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
     * so no player instance is kept in memory when disabled, backgrounded, or in-game.
     */
    private fun fadeOutAndRelease(keepFocus: Boolean) {
        fadeJob?.cancel()
        val player = mediaPlayer
        if (player == null) {
            if (!keepFocus) abandonAudioFocus()
            return
        }

        val startVol = currentAppliedVolume.coerceIn(0f, 1f)
        if (startVol <= 0.01f) {
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
     * Used when WebRTC game stream connects, screen recording starts, or Activity is destroyed.
     */
    fun stopAndReleaseImmediately() {
        fadeJob?.cancel()
        fadeJob = null
        releasePlayerOnly()
        abandonAudioFocus()
    }

    private fun releasePlayerOnly() {
        currentAppliedVolume = 0f
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
