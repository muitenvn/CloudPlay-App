package com.example.cloudplay

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.PixelCopy
import android.view.Surface
import android.webkit.WebView
import android.widget.Toast
import com.example.data.RecordingCaptureEngine
import com.example.data.ScreenRecordQuality
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

data class ScreenRecorderState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val modeLabel: String = "Full Screen MP4 + Audio",
    val activeQuality: ScreenRecordQuality = ScreenRecordQuality.BALANCED_720P_30,
    val autoLowLoadActive: Boolean = true,
    val lastSavedFileName: String? = null,
    val recordedClipsCount: Int = 0
)

/**
 * ZeroLagScreenRecorder (Zero-Allocation Hardware VBR Edition):
 * - Tái sử dụng toàn bộ vùng nhớ đệm (MediaCodec.BufferInfo, mảng PCM tĩnh, Bitmap/Rect cố định) để 0% GC Pause khi quay.
 * - Chống chồng chéo PixelCopy (isPixelCopyInFlight guard) & tự động giãn nhịp nhẹ khi GPU đang bận giải mã luồng game.
 * - Mã hóa phần cứng H.264 VBR (Priority = 1 để luôn nhường ưu tiên số 0 cho bộ giải mã WebRTC của game) + AAC-LC Stereo 48kHz.
 */
object ZeroLagScreenRecorder {
    private const val TAG = "ZeroLagScreenRecorder"
    private const val AUDIO_SAMPLE_RATE = 48000
    private const val AUDIO_CHANNEL_COUNT = 2
    private const val AUDIO_BITRATE_BPS = 128_000
    private const val BYTES_PER_STEREO_SAMPLE = 4 // 16-bit Left + 16-bit Right

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(ScreenRecorderState())
    val state: StateFlow<ScreenRecorderState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var webViewRef: WeakReference<WebView>? = null
    private var appContext: Context? = null

    private var activeCaptureEngine: RecordingCaptureEngine = RecordingCaptureEngine.HARDWARE_SCREEN
    private val hasSavedCurrentSession = AtomicBoolean(false)
    private val isPixelCopyInFlight = AtomicBoolean(false)

    // Native Hardware MediaCodec (H.264 Video + AAC Stereo Audio) + MediaMuxer (.mp4) state
    private var encoderThread: HandlerThread? = null
    private var encoderHandler: Handler? = null
    @Volatile
    private var isNativeEncoding = false
    private var mediaCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var pendingVideoFormat: MediaFormat? = null
    private var pendingAudioFormat: MediaFormat? = null
    private var videoTrackIndex: Int = -1
    private var audioTrackIndex: Int = -1
    private var muxerStarted: Boolean = false
    private var nativeTempFile: File? = null
    private var encodingStartTimeNs: Long = 0L
    private var firstMuxedVideoPtsNs: Long = 0L
    private var encodedVideoFrameCount: Int = 0
    private var lastVideoPtsUs: Long = -1L
    private var lastAudioPtsUs: Long = -1L
    private var submittedAudioFrames: Long = 0L
    private val pcmQueue = ConcurrentLinkedQueue<ByteArray>()

    // Pre-allocated reusable buffers (Zero-Allocation per frame/tick)
    private val videoBufferInfo = MediaCodec.BufferInfo()
    private val audioBufferInfo = MediaCodec.BufferInfo()
    private val silentPcmBuffer = ByteArray(1024 * BYTES_PER_STEREO_SAMPLE)
    private val reusableDstRect = Rect()

    fun toggleRecording(context: Context, webView: WebView?) {
        if (_state.value.isRecording) {
            stopRecording(context)
        } else {
            startRecording(context, webView)
        }
    }

    fun startRecording(context: Context, webView: WebView?) {
        if (_state.value.isRecording) return
        val appCtx = context.applicationContext
        appContext = appCtx
        webViewRef = if (webView != null) WeakReference(webView) else null

        val config = SettingsRepository.config.value
        val isEn = config.isEnglishUI()
        val quality = config.screenRecordQuality
        val captureEngine = config.recordingCaptureEngine
        activeCaptureEngine = captureEngine
        hasSavedCurrentSession.set(false)
        isPixelCopyInFlight.set(false)
        pcmQueue.clear()

        val isFullScreenCapture = (captureEngine == RecordingCaptureEngine.HARDWARE_SCREEN)

        _state.value = _state.value.copy(
            isRecording = true,
            elapsedSeconds = 0,
            activeQuality = quality,
            autoLowLoadActive = true,
            modeLabel = if (isFullScreenCapture) {
                if (isEn) "Full Screen (As Played • .MP4)" else "Full màn hình (Giống lúc chơi • .MP4)"
            } else {
                if (isEn) "Standard 16:9 Frame (.MP4)" else "Khung chuẩn 16:9 (.MP4)"
            }
        )

        startElapsedTimer()
        startNativeHardwareSurfaceRecorder(context, webView, quality, isFullScreenCapture)

        if (webView != null) {
            webView.post {
                try {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            if (window.__setRecordingPerformanceMode) {
                                window.__setRecordingPerformanceMode(true);
                            }
                            if (window.__startLiveAudioPcmTap) {
                                window.__startLiveAudioPcmTap();
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not start WebAudio PCM tap", e)
                }
            }
        }

        val toastMsg = if (isEn) {
            "🔴 Recording (${quality.localizedShort(true)} • Low-CPU .MP4)"
        } else {
            "🔴 Đang quay (${quality.localizedShort(false)} • Nhẹ .MP4)"
        }
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    fun stopRecording(context: Context? = null) {
        if (!_state.value.isRecording) return
        val ctx = context ?: appContext
        val isEn = SettingsRepository.config.value.isEnglishUI()

        timerJob?.cancel()
        timerJob = null

        val wv = webViewRef?.get()
        if (wv != null) {
            wv.post {
                try {
                    wv.evaluateJavascript(
                        """
                        (function() {
                            if (window.__setRecordingPerformanceMode) window.__setRecordingPerformanceMode(false);
                            if (window.__stopLiveAudioPcmTap) window.__stopLiveAudioPcmTap();
                        })();
                        """.trimIndent(),
                        null
                    )
                } catch (_: Exception) {}
            }
        }

        stopNativeHardwareSurfaceRecorder(ctx, saveDirectly = true)

        _state.value = _state.value.copy(
            isRecording = false,
            autoLowLoadActive = false
        )

        if (ctx != null) {
            val stoppingMsg = if (isEn) {
                "⏹️ Saving .MP4 video to Gallery (Movies/XboxCloudPlay)..."
            } else {
                "⏹️ Đang lưu video .MP4 vào Thư viện (Movies/XboxCloudPlay)..."
            }
            Toast.makeText(ctx, stoppingMsg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Receives live 16-bit Little-Endian stereo PCM audio chunks from WebRTC via XboxBridge.onScreenRecordAudioPcm.
     */
    fun onWebAudioPcmReceived(base64Pcm: String, @Suppress("UNUSED_PARAMETER") sampleRate: Int, @Suppress("UNUSED_PARAMETER") channels: Int) {
        if (!_state.value.isRecording || base64Pcm.isBlank()) return
        try {
            // Bounded queue prevents memory growth if encoder thread is briefly busy
            if (pcmQueue.size >= 48) return
            val pcmBytes = Base64.decode(base64Pcm, Base64.DEFAULT)
            if (pcmBytes.isNotEmpty()) {
                pcmQueue.offer(pcmBytes)
                encoderHandler?.post {
                    if (isNativeEncoding) {
                        feedAndDrainAudioEncoder(false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error decoding web audio PCM chunk", e)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onWebStreamChunkReceived(base64Data: String, isLastChunk: Boolean, mimeType: String?) {
        // Retained for JavascriptInterface binary compatibility; all recording uses Hardware Surface MediaCodec.
    }

    private fun isGenuineMp4File(file: File?): Boolean {
        if (file == null || !file.exists() || file.length() < 1024L) return false
        return try {
            val header = ByteArray(12)
            val read = FileInputStream(file).use { it.read(header) }
            read >= 8 &&
                header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
        } catch (_: Exception) {
            false
        }
    }

    private fun startNativeHardwareSurfaceRecorder(
        context: Context,
        webView: WebView?,
        quality: ScreenRecordQuality,
        isFullScreenMode: Boolean
    ) {
        val activity = findActivity(context) ?: findActivity(webView?.context)
        val appCtx = context.applicationContext
        val displayMetrics = appCtx.resources.displayMetrics
        val fallbackScreenW = maxOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
        val fallbackScreenH = minOf(displayMetrics.widthPixels, displayMetrics.heightPixels)

        // Run encoder thread at background/favorable priority so it never starves UI or WebRTC Decoder threads
        val thread = HandlerThread("ZeroLagHardwareEncoder", Process.THREAD_PRIORITY_MORE_FAVORABLE).apply { start() }
        val handler = Handler(thread.looper)
        encoderThread = thread
        encoderHandler = handler
        isNativeEncoding = true

        handler.post {
            try {
                val dir = File(appCtx.cacheDir, "recordings").apply { mkdirs() }
                val outFile = File(dir, "hw_clip_${System.currentTimeMillis()}.mp4")
                nativeTempFile = outFile

                val decor = activity?.window?.peekDecorView()
                val rawWinW = decor?.width?.takeIf { it > 128 } ?: webView?.width?.takeIf { it > 128 } ?: fallbackScreenW
                val rawWinH = decor?.height?.takeIf { it > 128 } ?: webView?.height?.takeIf { it > 128 } ?: fallbackScreenH
                val winW = maxOf(rawWinW, rawWinH)
                val winH = minOf(rawWinW, rawWinH)

                val (width, height) = if (isFullScreenMode && winW > 128 && winH > 128) {
                    val winAspect = winW.toFloat() / winH.toFloat()
                    val baseShortSide = quality.heightPx.coerceAtLeast(540)
                    val calcW = (((baseShortSide * winAspect).toInt() + 8) / 16) * 16
                    val calcH = ((baseShortSide + 8) / 16) * 16
                    Pair(calcW.coerceIn(640, 1920), calcH.coerceIn(360, 1080))
                } else {
                    Pair((quality.widthPx / 16) * 16, (quality.heightPx / 16) * 16)
                }

                reusableDstRect.set(0, 0, width, height)

                // 1. Configure Hardware H.264 Video Encoder with VBR & non-blocking priority (Priority = 1 yields to Game Decoder)
                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, quality.bitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, quality.fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                    setInteger(
                        MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setInteger(MediaFormat.KEY_OPERATING_RATE, quality.fps)
                        // Priority 1 ensures the live game WebRTC decoder (Priority 0) always gets hardware VPU slices first
                        setInteger(MediaFormat.KEY_PRIORITY, 1)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
                    }
                }

                val vCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                try {
                    vCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                } catch (_: Exception) {
                    // Fallback if device codec rejects VBR or optional keys
                    val basicFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                        setInteger(MediaFormat.KEY_BIT_RATE, quality.bitrateBps)
                        setInteger(MediaFormat.KEY_FRAME_RATE, quality.fps)
                        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                    }
                    vCodec.configure(basicFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }
                val surface = vCodec.createInputSurface()
                vCodec.start()

                // 2. Configure Hardware AAC-LC Stereo Audio Encoder (48kHz, 2 channels, 128 kbps)
                var aCodec: MediaCodec? = null
                try {
                    val audioFormat = MediaFormat.createAudioFormat(
                        MediaFormat.MIMETYPE_AUDIO_AAC,
                        AUDIO_SAMPLE_RATE,
                        AUDIO_CHANNEL_COUNT
                    ).apply {
                        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                        setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE_BPS)
                        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32768)
                    }
                    aCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                        configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                        start()
                    }
                } catch (audioInitErr: Exception) {
                    Log.w(TAG, "AAC audio encoder init fallback", audioInitErr)
                    aCodec = null
                }

                val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                mediaCodec = vCodec
                audioCodec = aCodec
                inputSurface = surface
                mediaMuxer = muxer
                pendingVideoFormat = null
                pendingAudioFormat = null
                videoTrackIndex = -1
                audioTrackIndex = -1
                muxerStarted = false
                encodingStartTimeNs = System.nanoTime()
                firstMuxedVideoPtsNs = 0L
                encodedVideoFrameCount = 0
                lastVideoPtsUs = -1L
                lastAudioPtsUs = -1L
                submittedAudioFrames = 0L

                // Prime AAC encoder so csd-0 is ready before the first H.264 IDR keyframe
                for (primeStep in 0 until 10) {
                    if (pendingAudioFormat != null || audioCodec == null) break
                    feedSilentAudioFrames(1024)
                    drainAudioCodecInternal()
                    if (pendingAudioFormat == null) {
                        try { Thread.sleep(4) } catch (_: Exception) {}
                    }
                }

                val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val paint = Paint() // No expensive bilinear filter needed since PixelCopy already scales in hardware
                val baseFrameIntervalMs = (1000L / quality.fps.coerceIn(24, 60)).coerceAtLeast(16L)
                val targetAspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
                val reusableCropRect = Rect()

                val captureRunnable = object : Runnable {
                    override fun run() {
                        if (!isNativeEncoding) return
                        val tickStart = System.currentTimeMillis()
                        val currentActivity = activity ?: findActivity(context) ?: findActivity(webView?.context)
                        val window = currentActivity?.window
                        val decorView = window?.peekDecorView()

                        // Adaptive thermal & decode pacing: give WebRTC video decoder extra breathing room if stream is degrading
                        val qualityState = AdaptiveQualityManager.state.value
                        val isDecoderBusy = qualityState.lastDegradeReason != null
                        val effectiveIntervalMs = if (isDecoderBusy) baseFrameIntervalMs + 6L else baseFrameIntervalMs

                        fun scheduleNextCaptureTick() {
                            if (!isNativeEncoding) return
                            val elapsed = System.currentTimeMillis() - tickStart
                            val nextDelay = (effectiveIntervalMs - elapsed).coerceAtLeast(10L)
                            handler.postDelayed(this, nextDelay)
                        }

                        // Skip launching a new PixelCopy if previous GPU readback is still in flight
                        if (isPixelCopyInFlight.get()) {
                            feedAndDrainAudioEncoder(false)
                            drainVideoEncoder(false)
                            scheduleNextCaptureTick()
                            return
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window != null && decorView != null && decorView.width > 0 && decorView.height > 0) {
                            try {
                                val curWinW = decorView.width
                                val curWinH = decorView.height
                                val srcRect: Rect? = if (!isFullScreenMode && curWinW > 64 && curWinH > 64) {
                                    val winAspect = curWinW.toFloat() / curWinH.toFloat()
                                    if (winAspect > targetAspect + 0.02f) {
                                        val cropW = (curWinH * targetAspect).toInt().coerceIn(64, curWinW)
                                        val left = ((curWinW - cropW) / 2).coerceAtLeast(0)
                                        reusableCropRect.set(left, 0, left + cropW, curWinH)
                                        reusableCropRect
                                    } else if (winAspect < targetAspect - 0.02f) {
                                        val cropH = (curWinW / targetAspect).toInt().coerceIn(64, curWinH)
                                        val top = ((curWinH - cropH) / 2).coerceAtLeast(0)
                                        reusableCropRect.set(0, top, curWinW, top + cropH)
                                        reusableCropRect
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }

                                isPixelCopyInFlight.set(true)
                                val copyListener = PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                                    isPixelCopyInFlight.set(false)
                                    try {
                                        if (isNativeEncoding) {
                                            if (copyResult == PixelCopy.SUCCESS || encodedVideoFrameCount > 0) {
                                                drawBitmapToEncoderSurface(frameBitmap, paint)
                                            }
                                            feedAndDrainAudioEncoder(false)
                                            drainVideoEncoder(false)
                                        }
                                    } finally {
                                        scheduleNextCaptureTick()
                                    }
                                }

                                if (srcRect != null) {
                                    PixelCopy.request(window, srcRect, frameBitmap, copyListener, handler)
                                } else {
                                    PixelCopy.request(window, frameBitmap, copyListener, handler)
                                }
                            } catch (_: Exception) {
                                isPixelCopyInFlight.set(false)
                                if (encodedVideoFrameCount > 0) {
                                    drawBitmapToEncoderSurface(frameBitmap, paint)
                                }
                                feedAndDrainAudioEncoder(false)
                                drainVideoEncoder(false)
                                scheduleNextCaptureTick()
                            }
                        } else {
                            if (encodedVideoFrameCount > 0) {
                                drawBitmapToEncoderSurface(frameBitmap, paint)
                            }
                            feedAndDrainAudioEncoder(false)
                            drainVideoEncoder(false)
                            scheduleNextCaptureTick()
                        }
                    }
                }
                handler.post(captureRunnable)
            } catch (e: Exception) {
                Log.w(TAG, "Hardware encoder init error", e)
                isNativeEncoding = false
            }
        }
    }

    private fun requestImmediateVideoKeyframe() {
        val vCodec = mediaCodec ?: return
        try {
            val params = android.os.Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            }
            vCodec.setParameters(params)
        } catch (_: Exception) {}
    }

    private fun tryStartMuxerIfReady(forceStartIfVideoReady: Boolean = false) {
        if (muxerStarted) return
        val muxer = mediaMuxer ?: return
        val vFmt = pendingVideoFormat ?: return
        var aFmt = pendingAudioFormat

        if (audioCodec != null && aFmt == null) {
            feedSilentAudioFrames(1024)
            drainAudioCodecInternal()
            aFmt = pendingAudioFormat
        }
        if (audioCodec != null && aFmt == null && !forceStartIfVideoReady) return

        try {
            videoTrackIndex = muxer.addTrack(vFmt)
            if (aFmt != null) {
                audioTrackIndex = muxer.addTrack(aFmt)
            }
            muxer.start()
            muxerStarted = true
            encodingStartTimeNs = System.nanoTime()
            firstMuxedVideoPtsNs = encodingStartTimeNs
            requestImmediateVideoKeyframe()
        } catch (e: Exception) {
            Log.w(TAG, "Error starting MediaMuxer", e)
        }
    }

    private fun feedSilentAudioFrames(frameCount: Int) {
        val aCodec = audioCodec ?: return
        try {
            val inIdx = aCodec.dequeueInputBuffer(1_500L)
            if (inIdx >= 0) {
                val inBuf = aCodec.getInputBuffer(inIdx) ?: return
                inBuf.clear()
                val byteCount = (frameCount * BYTES_PER_STEREO_SAMPLE)
                    .coerceAtMost(silentPcmBuffer.size)
                    .coerceAtMost(inBuf.capacity())
                inBuf.put(silentPcmBuffer, 0, byteCount)
                val ptsUs = (submittedAudioFrames * 1_000_000L) / AUDIO_SAMPLE_RATE
                submittedAudioFrames += (byteCount / BYTES_PER_STEREO_SAMPLE)
                aCodec.queueInputBuffer(inIdx, 0, byteCount, ptsUs, 0)
            }
        } catch (_: Exception) {}
    }

    private fun feedAndDrainAudioEncoder(endOfStream: Boolean) {
        val aCodec = audioCodec ?: return
        try {
            var fedAny = false
            while (pcmQueue.isNotEmpty()) {
                val chunk = pcmQueue.peek() ?: break
                val inIdx = aCodec.dequeueInputBuffer(0L)
                if (inIdx < 0) break
                pcmQueue.poll()
                val inBuf = aCodec.getInputBuffer(inIdx)
                if (inBuf != null) {
                    inBuf.clear()
                    val len = chunk.size.coerceAtMost(inBuf.capacity())
                    inBuf.put(chunk, 0, len)
                    val ptsUs = (submittedAudioFrames * 1_000_000L) / AUDIO_SAMPLE_RATE
                    submittedAudioFrames += (len / BYTES_PER_STEREO_SAMPLE)
                    aCodec.queueInputBuffer(inIdx, 0, len, ptsUs, 0)
                    fedAny = true
                }
            }

            val elapsedUs = ((System.nanoTime() - encodingStartTimeNs) / 1000L).coerceAtLeast(0L)
            val audioPtsUs = (submittedAudioFrames * 1_000_000L) / AUDIO_SAMPLE_RATE
            if (!fedAny && !endOfStream && elapsedUs - audioPtsUs > 90_000L) {
                feedSilentAudioFrames(1024)
            }

            if (endOfStream) {
                val inIdx = aCodec.dequeueInputBuffer(4_000L)
                if (inIdx >= 0) {
                    val ptsUs = (submittedAudioFrames * 1_000_000L) / AUDIO_SAMPLE_RATE
                    aCodec.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            drainAudioCodec(endOfStream)
        } catch (e: Exception) {
            Log.w(TAG, "Error feeding/draining AAC audio encoder", e)
        }
    }

    private fun drainAudioCodecInternal() {
        val aCodec = audioCodec ?: return
        try {
            for (i in 0 until 6) {
                val outIdx = aCodec.dequeueOutputBuffer(audioBufferInfo, 1_500L)
                if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pendingAudioFormat = aCodec.outputFormat
                    break
                } else if (outIdx >= 0) {
                    aCodec.releaseOutputBuffer(outIdx, false)
                }
            }
        } catch (_: Exception) {}
    }

    private fun drainAudioCodec(endOfStream: Boolean) {
        val aCodec = audioCodec ?: return
        val muxer = mediaMuxer ?: return
        try {
            var loops = 0
            while (loops < 20) {
                loops++
                val outIdx = aCodec.dequeueOutputBuffer(audioBufferInfo, if (endOfStream) 4_000L else 0L)
                if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pendingAudioFormat = aCodec.outputFormat
                    tryStartMuxerIfReady(forceStartIfVideoReady = false)
                } else if (outIdx >= 0) {
                    val encodedData = aCodec.getOutputBuffer(outIdx)
                    if (encodedData != null) {
                        if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            audioBufferInfo.size = 0
                        }
                        if (audioBufferInfo.size > 0 && muxerStarted && audioTrackIndex >= 0) {
                            encodedData.position(audioBufferInfo.offset)
                            encodedData.limit(audioBufferInfo.offset + audioBufferInfo.size)
                            val rawPts = audioBufferInfo.presentationTimeUs.coerceAtLeast(0L)
                            val safePts = if (lastAudioPtsUs < 0L) {
                                0L
                            } else if (rawPts <= lastAudioPtsUs) {
                                lastAudioPtsUs + 21_333L
                            } else {
                                rawPts
                            }
                            lastAudioPtsUs = safePts
                            audioBufferInfo.presentationTimeUs = safePts
                            muxer.writeSampleData(audioTrackIndex, encodedData, audioBufferInfo)
                        }
                    }
                    aCodec.releaseOutputBuffer(outIdx, false)
                    if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error draining AAC output", e)
        }
    }

    private fun drawBitmapToEncoderSurface(
        bitmap: Bitmap,
        paint: Paint
    ) {
        val surface = inputSurface ?: return
        try {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                surface.lockCanvas(null)
            }
            try {
                // Draw opaque full-frame bitmap directly without redundant full-screen drawColor(BLACK) GPU fill
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
            encodedVideoFrameCount++
            if (encodedVideoFrameCount == 1 || encodedVideoFrameCount == 15) {
                requestImmediateVideoKeyframe()
            }
        } catch (_: Exception) {}
    }

    private fun drainVideoEncoder(endOfStream: Boolean) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return
        try {
            if (endOfStream) {
                codec.signalEndOfInputStream()
            }
            var loops = 0
            while (loops < 20) {
                loops++
                val outputIndex = codec.dequeueOutputBuffer(videoBufferInfo, if (endOfStream) 8_000L else 0L)
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pendingVideoFormat = codec.outputFormat
                    tryStartMuxerIfReady(forceStartIfVideoReady = true)
                } else if (outputIndex >= 0) {
                    if (!muxerStarted && pendingVideoFormat != null) {
                        tryStartMuxerIfReady(forceStartIfVideoReady = true)
                    }
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (encodedData != null) {
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            videoBufferInfo.size = 0
                        }
                        if (videoBufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                            encodedData.position(videoBufferInfo.offset)
                            encodedData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            val nowNs = System.nanoTime()
                            if (firstMuxedVideoPtsNs <= 0L) {
                                firstMuxedVideoPtsNs = nowNs
                            }
                            val rawPts = ((nowNs - firstMuxedVideoPtsNs) / 1000L).coerceAtLeast(0L)
                            val safePts = if (lastVideoPtsUs < 0L) {
                                0L
                            } else if (rawPts <= lastVideoPtsUs) {
                                lastVideoPtsUs + 16_666L
                            } else {
                                rawPts
                            }
                            lastVideoPtsUs = safePts
                            videoBufferInfo.presentationTimeUs = safePts
                            muxer.writeSampleData(videoTrackIndex, encodedData, videoBufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error draining hardware video encoder", e)
        }
    }

    private fun stopNativeHardwareSurfaceRecorder(ctx: Context?, saveDirectly: Boolean) {
        isNativeEncoding = false
        isPixelCopyInFlight.set(false)
        val handler = encoderHandler
        val thread = encoderThread
        encoderHandler = null
        encoderThread = null

        if (handler != null && thread != null) {
            handler.post {
                try {
                    feedAndDrainAudioEncoder(true)
                } catch (_: Exception) {}
                try {
                    drainVideoEncoder(true)
                } catch (_: Exception) {}
                try {
                    mediaCodec?.stop()
                    mediaCodec?.release()
                } catch (_: Exception) {}
                mediaCodec = null
                try {
                    audioCodec?.stop()
                    audioCodec?.release()
                } catch (_: Exception) {}
                audioCodec = null
                try {
                    inputSurface?.release()
                } catch (_: Exception) {}
                inputSurface = null
                try {
                    if (muxerStarted) {
                        mediaMuxer?.stop()
                    }
                    mediaMuxer?.release()
                } catch (_: Exception) {}
                mediaMuxer = null
                muxerStarted = false
                pcmQueue.clear()

                if (saveDirectly && hasSavedCurrentSession.compareAndSet(false, true)) {
                    val outFile = nativeTempFile
                    nativeTempFile = null
                    if (outFile != null && isGenuineMp4File(outFile)) {
                        val savedName = saveVideoFileToMediaStore(ctx, outFile, "mp4", "video/mp4")
                        outFile.delete()
                        if (savedName != null) {
                            notifyClipSavedOnMain(ctx, savedName)
                        }
                    } else {
                        outFile?.delete()
                    }
                }
                try {
                    thread.quitSafely()
                } catch (_: Exception) {}
            }
        }
    }

    private fun saveVideoFileToMediaStore(
        ctx: Context?,
        sourceFile: File,
        extension: String,
        mimeType: String
    ): String? {
        val context = ctx ?: appContext ?: return null
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "XboxClip_$timestamp.$extension"

        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, "XboxClip_$timestamp")
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
                put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/XboxCloudPlay")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val itemUri = resolver.insert(collection, values)
            if (itemUri != null) {
                resolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(outStream, 128 * 1024)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readyValues = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    resolver.update(itemUri, readyValues, null, null)
                }
                fileName
            } else {
                val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "XboxCloudPlay").apply { mkdirs() }
                val dest = File(fallbackDir, fileName)
                sourceFile.copyTo(dest, overwrite = true)
                fileName
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore save fallback", e)
            try {
                val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "XboxCloudPlay").apply { mkdirs() }
                val dest = File(fallbackDir, fileName)
                sourceFile.copyTo(dest, overwrite = true)
                fileName
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun notifyClipSavedOnMain(ctx: Context?, fileName: String) {
        scope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(
                lastSavedFileName = fileName,
                recordedClipsCount = _state.value.recordedClipsCount + 1
            )
            val context = ctx ?: appContext ?: return@launch
            val isEn = SettingsRepository.config.value.isEnglishUI()
            val msg = if (isEn) {
                "✅ Saved MP4 with audio: Movies/XboxCloudPlay/$fileName"
            } else {
                "✅ Đã lưu video MP4 có tiếng: Movies/XboxCloudPlay/$fileName"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_state.value.isRecording) {
                delay(1000)
                if (_state.value.isRecording) {
                    _state.value = _state.value.copy(
                        elapsedSeconds = _state.value.elapsedSeconds + 1
                    )
                }
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    private fun findActivity(context: Context?): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
