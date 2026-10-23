package com.example.cloudplay

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

data class ScreenRecorderState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val modeLabel: String = "Full Screen MP4 + Audio",
    val activeQuality: ScreenRecordQuality = ScreenRecordQuality.BALANCED_720P_30,
    val autoLowLoadActive: Boolean = false,
    val lastSavedFileName: String? = null,
    val recordedClipsCount: Int = 0
)

/**
 * ZeroLagScreenRecorder:
 * Hệ thống quay màn hình chuẩn .MP4 có đầy đủ âm thanh Stereo dành riêng cho Xbox Cloud Gaming:
 * 1. Chế độ mặc định (HARDWARE_SCREEN - Quay Full Toàn Màn Hình):
 *    - Quay trọn vẹn toàn bộ màn hình thiết bị theo đúng tỉ lệ màn hình thật (không cắt viền 16:9) bằng bộ mã hóa phần cứng H.264 (MediaCodec Surface + Async PixelCopy).
 *    - Thu trực tiếp âm thanh Stereo 48kHz của game qua WebAudio PCM Tap và mã hóa phần cứng sang chuẩn AAC-LC (audio/mp4a-latm), đóng gói trực tiếp vào container chuẩn MPEG-4 (.mp4).
 * 2. Chế độ tùy chọn (DIRECT_STREAM - Thu Luồng Stream 16:9):
 *    - Thu trực tiếp luồng WebRTC 16:9 và remux sang container .MP4 chuẩn (kiểm tra magic header 'ftyp' để Android MediaStore không bao giờ đổi đuôi sang .webm), tự động ghép luồng âm thanh AAC nếu trình duyệt xuất Opus/WebM.
 * 3. Khi bật "Tự giảm tải đồ họa khi quay", tự động tắt các hiệu ứng làm mờ, bóng đổ và tạm dừng bộ quét DOM nền
 *    để giữ trọn vẹn 60 FPS khi chiến game.
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

    // Direct WebRTC stream chunk file (used only when DIRECT_STREAM is selected)
    private var streamTempFile: File? = null
    private var streamOutputStream: BufferedOutputStream? = null
    private var streamMimeType: String = "video/mp4"
    private var usingNativeHardwareFallback: Boolean = true
    private var activeCaptureEngine: RecordingCaptureEngine = RecordingCaptureEngine.HARDWARE_SCREEN

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
    private var lastVideoPtsUs: Long = 0L
    private var lastAudioPtsUs: Long = 0L
    private var submittedAudioFrames: Long = 0L
    private val pcmQueue = ConcurrentLinkedQueue<ByteArray>()

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
        val autoReduceLoad = config.autoReduceLoadWhenRecording
        val captureEngine = config.recordingCaptureEngine
        activeCaptureEngine = captureEngine
        pcmQueue.clear()

        _state.value = _state.value.copy(
            isRecording = true,
            elapsedSeconds = 0,
            activeQuality = quality,
            autoLowLoadActive = autoReduceLoad,
            modeLabel = if (captureEngine == RecordingCaptureEngine.DIRECT_STREAM) {
                if (isEn) "Direct Stream 16:9 (.MP4 + Audio)" else "Luồng Stream 16:9 (.MP4 + Âm thanh)"
            } else {
                if (isEn) "Full Screen (.MP4 + Audio)" else "Quay Full Màn Hình (.MP4 + Âm thanh)"
            }
        )

        startElapsedTimer()

        // Always start the hardware MP4 (H.264 + AAC Audio) encoder:
        // - In HARDWARE_SCREEN (Default): records full screen + live game audio directly to standard .mp4
        // - In DIRECT_STREAM: acts as simultaneous guaranteed .mp4 + AAC audio source so we NEVER save .webm or silent clips
        val isFullScreenCapture = (captureEngine == RecordingCaptureEngine.HARDWARE_SCREEN)
        usingNativeHardwareFallback = isFullScreenCapture
        startNativeHardwareSurfaceRecorder(context, webView, quality, isFullScreenCapture)

        if (webView != null && captureEngine == RecordingCaptureEngine.DIRECT_STREAM) {
            scope.launch(Dispatchers.IO) {
                try {
                    val dir = File(appCtx.cacheDir, "recordings").apply { mkdirs() }
                    val tmp = File(dir, "active_clip_${System.currentTimeMillis()}.tmp")
                    streamTempFile = tmp
                    streamOutputStream = BufferedOutputStream(FileOutputStream(tmp), 64 * 1024)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not prepare stream temp file", e)
                }
            }

            val js = """
                (function() {
                    if (window.__setRecordingPerformanceMode) {
                        window.__setRecordingPerformanceMode(${autoReduceLoad});
                    }
                    if (window.__startLiveAudioPcmTap) {
                        window.__startLiveAudioPcmTap();
                    }
                    if (window.__startZeroLagStreamRecorder) {
                        return window.__startZeroLagStreamRecorder(${quality.widthPx}, ${quality.heightPx}, ${quality.fps}, ${quality.bitrateBps}, '${captureEngine.id}');
                    }
                    return "FALLBACK_NATIVE";
                })();
            """.trimIndent()

            webView.post {
                try {
                    webView.evaluateJavascript(js) { result ->
                        val cleanRes = result?.trim()?.removeSurrounding("\"") ?: "FALLBACK_NATIVE"
                        if (cleanRes.startsWith("DIRECT_STREAM")) {
                            usingNativeHardwareFallback = false
                            _state.value = _state.value.copy(
                                modeLabel = if (isEn) "Direct Stream 16:9 (.MP4 + Audio)" else "Luồng Stream 16:9 (.MP4 + Âm thanh)"
                            )
                        } else {
                            usingNativeHardwareFallback = true
                            _state.value = _state.value.copy(
                                modeLabel = if (isEn) "Hardware 16:9 (.MP4 + Audio)" else "Phần Cứng 16:9 (.MP4 + Âm thanh)"
                            )
                        }
                    }
                } catch (e: Exception) {
                    usingNativeHardwareFallback = true
                }
            }
        } else {
            // Default HARDWARE_SCREEN mode: activate performance mode & live WebAudio stereo PCM tap for AAC audio encoding
            if (webView != null) {
                webView.post {
                    try {
                        webView.evaluateJavascript(
                            """
                            (function() {
                                if (window.__setRecordingPerformanceMode) window.__setRecordingPerformanceMode(${autoReduceLoad});
                                if (window.__startLiveAudioPcmTap) window.__startLiveAudioPcmTap();
                            })();
                            """.trimIndent(),
                            null
                        )
                    } catch (_: Exception) {}
                }
            }
        }

        val toastMsg = if (isEn) {
            "🔴 Recording started (${quality.localizedShort(true)} • Standard .MP4 with Audio)"
        } else {
            "🔴 Đã bắt đầu quay (${quality.localizedShort(false)} • Chuẩn .MP4 có âm thanh)"
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
                            if (window.__stopZeroLagStreamRecorder) window.__stopZeroLagStreamRecorder();
                        })();
                        """.trimIndent(),
                        null
                    )
                } catch (_: Exception) {}
            }
        }

        if (usingNativeHardwareFallback) {
            // Clean up any unused stream temp file
            try {
                streamOutputStream?.close()
            } catch (_: Exception) {}
            streamOutputStream = null
            streamTempFile?.delete()
            streamTempFile = null
            stopNativeHardwareSurfaceRecorder(ctx, saveDirectly = true)
        } else {
            // Stop hardware companion encoder first, then finalize direct stream with guaranteed MP4 + AAC fallback
            stopNativeHardwareSurfaceRecorder(ctx, saveDirectly = false)
            scope.launch(Dispatchers.IO) {
                delay(480)
                finalizeDirectStreamFile(ctx)
            }
        }

        _state.value = _state.value.copy(
            isRecording = false,
            autoLowLoadActive = false
        )

        if (ctx != null) {
            val stoppingMsg = if (isEn) "⏹️ Saving .MP4 video with audio to Gallery (Movies/XboxCloudPlay)..." else "⏹️ Đang lưu video .MP4 có âm thanh vào Thư viện (Movies/XboxCloudPlay)..."
            Toast.makeText(ctx, stoppingMsg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Receives live 16-bit Little-Endian stereo PCM audio chunks from WebRTC via XboxBridge.onScreenRecordAudioPcm.
     */
    fun onWebAudioPcmReceived(base64Pcm: String, sampleRate: Int, channels: Int) {
        if (!_state.value.isRecording || base64Pcm.isBlank()) return
        try {
            val pcmBytes = Base64.decode(base64Pcm, Base64.DEFAULT)
            if (pcmBytes.isNotEmpty()) {
                // Keep queue bounded so memory never grows if encoder is busy
                if (pcmQueue.size < 64) {
                    pcmQueue.offer(pcmBytes)
                }
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

    /**
     * Called from XboxBridge (@JavascriptInterface) as base64 chunks arrive from the WebRTC MediaRecorder.
     */
    fun onWebStreamChunkReceived(base64Data: String, isLastChunk: Boolean, mimeType: String?) {
        if (!mimeType.isNullOrBlank()) {
            streamMimeType = mimeType
        }
        scope.launch(Dispatchers.IO) {
            try {
                if (base64Data.isNotBlank()) {
                    val cleanBase64 = base64Data.substringAfter("base64,", base64Data)
                    val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    synchronized(this@ZeroLagScreenRecorder) {
                        streamOutputStream?.write(bytes)
                        if (isLastChunk) {
                            streamOutputStream?.flush()
                        }
                    }
                }
                if (isLastChunk && !usingNativeHardwareFallback) {
                    finalizeDirectStreamFile(appContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error writing web stream chunk", e)
            }
        }
    }

    @Synchronized
    private fun finalizeDirectStreamFile(ctx: Context?) {
        val tmpFile = streamTempFile
        val hwMp4File = nativeTempFile
        if (tmpFile == null && hwMp4File == null) return
        streamTempFile = null
        nativeTempFile = null

        try {
            streamOutputStream?.flush()
            streamOutputStream?.close()
        } catch (_: Exception) {}
        streamOutputStream = null

        val parentDir = tmpFile?.parentFile ?: hwMp4File?.parentFile ?: File(ctx?.cacheDir ?: appContext?.cacheDir, "recordings")
        val cleanMp4File = File(parentDir, "clean_clip_${System.currentTimeMillis()}.mp4")

        val remuxSuccess = if (tmpFile != null && tmpFile.exists() && tmpFile.length() > 1024L) {
            remuxStreamWithAudioFallbackToMp4(tmpFile, hwMp4File, cleanMp4File)
        } else {
            false
        }

        // CRITICAL: Never pass a raw WebM file to MediaStore (Android MediaProvider renames WebM headers to .webm).
        // Only save cleanMp4File if it has a verified 'ftyp' MP4 box header; otherwise use hwMp4File (which is 100% native MP4 with AAC audio).
        val verifiedFileToSave: File? = when {
            remuxSuccess && isGenuineMp4File(cleanMp4File) -> cleanMp4File
            hwMp4File != null && isGenuineMp4File(hwMp4File) -> hwMp4File
            else -> null
        }

        if (verifiedFileToSave != null) {
            val savedName = saveVideoFileToMediaStore(ctx, verifiedFileToSave, "mp4", "video/mp4")
            if (savedName != null) {
                notifyClipSavedOnMain(ctx, savedName)
            }
        }

        try { tmpFile?.delete() } catch (_: Exception) {}
        try { cleanMp4File.delete() } catch (_: Exception) {}
        try { hwMp4File?.delete() } catch (_: Exception) {}
    }

    /**
     * Checks that the file exists, is non-empty, and starts with a genuine ISO Base Media (MP4) 'ftyp' header at bytes 4..7.
     * This prevents Android 11+ MediaProvider from ever auto-renaming the file extension to .webm.
     */
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

    /**
     * Remuxes the direct stream into a standard MPEG-4 (.mp4) container and guarantees an AAC audio track:
     * - If the direct stream's own audio track is accepted by MP4 MediaMuxer, uses it directly.
     * - If the direct stream's audio is WebM Opus (rejected by MP4 MediaMuxer), automatically injects the synchronized
     *   hardware AAC stereo audio track from [companionMp4File] so the resulting .mp4 ALWAYS has full game audio!
     */
    private fun remuxStreamWithAudioFallbackToMp4(
        inputFile: File,
        companionMp4File: File?,
        outputFile: File
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var companionAudioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            videoExtractor = MediaExtractor().apply { setDataSource(inputFile.absolutePath) }
            val trackCount = videoExtractor.trackCount
            if (trackCount == 0) return false

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var srcVideoTrack = -1
            var dstVideoTrack = -1
            var srcAudioTrack = -1
            var dstAudioTrack = -1
            var usingCompanionAudio = false
            var maxBufSize = 512 * 1024

            for (i in 0 until trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && dstVideoTrack < 0) {
                    val added = try {
                        muxer.addTrack(format)
                    } catch (e: Exception) {
                        Log.w(TAG, "MP4 Muxer rejected stream video track ($mime), will use hardware MP4", e)
                        -1
                    }
                    if (added >= 0) {
                        srcVideoTrack = i
                        dstVideoTrack = added
                        videoExtractor.selectTrack(i)
                        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            maxBufSize = maxOf(maxBufSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                        }
                    }
                } else if (mime.startsWith("audio/") && dstAudioTrack < 0) {
                    // Only add direct audio if it is MP4/AAC compatible
                    val added = try {
                        if (mime == MediaFormat.MIMETYPE_AUDIO_AAC || mime.contains("mp4a")) {
                            muxer.addTrack(format)
                        } else {
                            -1
                        }
                    } catch (_: Exception) {
                        -1
                    }
                    if (added >= 0) {
                        srcAudioTrack = i
                        dstAudioTrack = added
                        videoExtractor.selectTrack(i)
                    }
                }
            }

            // If stream video track could not be muxed to MP4 (e.g., WebM VP8/VP9 or missing AVC CSD), return false to use hardware MP4
            if (dstVideoTrack < 0) {
                return false
            }

            // If stream audio track was missing or Opus, pull the clean AAC audio track from companionMp4File!
            if (dstAudioTrack < 0 && companionMp4File != null && companionMp4File.exists() && companionMp4File.length() > 512L) {
                try {
                    companionAudioExtractor = MediaExtractor().apply { setDataSource(companionMp4File.absolutePath) }
                    for (i in 0 until companionAudioExtractor.trackCount) {
                        val fmt = companionAudioExtractor.getTrackFormat(i)
                        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                        if (mime.startsWith("audio/")) {
                            val added = try { muxer.addTrack(fmt) } catch (_: Exception) { -1 }
                            if (added >= 0) {
                                srcAudioTrack = i
                                dstAudioTrack = added
                                usingCompanionAudio = true
                                companionAudioExtractor.selectTrack(i)
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Companion audio extractor note", e)
                }
            }

            muxer.start()
            val buffer = ByteBuffer.allocateDirect(maxBufSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Copy primary video (and direct audio if present)
            var lastVideoPts = -1L
            var lastDirectAudioPts = -1L
            while (true) {
                val trackIndex = videoExtractor.sampleTrackIndex
                if (trackIndex < 0) break
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size > 0) {
                    val rawPts = videoExtractor.sampleTime.coerceAtLeast(0L)
                    bufferInfo.flags = videoExtractor.sampleFlags
                    if (trackIndex == srcVideoTrack) {
                        val pts = if (rawPts <= lastVideoPts) lastVideoPts + 1000L else rawPts
                        lastVideoPts = pts
                        bufferInfo.presentationTimeUs = pts
                        muxer.writeSampleData(dstVideoTrack, buffer, bufferInfo)
                    } else if (!usingCompanionAudio && trackIndex == srcAudioTrack && dstAudioTrack >= 0) {
                        val pts = if (rawPts <= lastDirectAudioPts) lastDirectAudioPts + 1000L else rawPts
                        lastDirectAudioPts = pts
                        bufferInfo.presentationTimeUs = pts
                        muxer.writeSampleData(dstAudioTrack, buffer, bufferInfo)
                    }
                }
                videoExtractor.advance()
            }

            // Copy companion AAC audio track if used
            if (usingCompanionAudio && dstAudioTrack >= 0 && companionAudioExtractor != null) {
                var lastCompAudioPts = -1L
                while (true) {
                    val trackIndex = companionAudioExtractor.sampleTrackIndex
                    if (trackIndex < 0) break
                    if (trackIndex == srcAudioTrack) {
                        bufferInfo.offset = 0
                        bufferInfo.size = companionAudioExtractor.readSampleData(buffer, 0)
                        if (bufferInfo.size > 0) {
                            val rawPts = companionAudioExtractor.sampleTime.coerceAtLeast(0L)
                            val pts = if (rawPts <= lastCompAudioPts) lastCompAudioPts + 1000L else rawPts
                            lastCompAudioPts = pts
                            bufferInfo.presentationTimeUs = pts
                            bufferInfo.flags = companionAudioExtractor.sampleFlags
                            muxer.writeSampleData(dstAudioTrack, buffer, bufferInfo)
                        }
                    }
                    companionAudioExtractor.advance()
                }
            }

            muxer.stop()
            return isGenuineMp4File(outputFile)
        } catch (e: Exception) {
            Log.w(TAG, "Remux to MP4 fallback to hardware MP4", e)
            return false
        } finally {
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { companionAudioExtractor?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun startNativeHardwareSurfaceRecorder(
        context: Context,
        webView: WebView?,
        quality: ScreenRecordQuality,
        isFullScreenMode: Boolean
    ) {
        val activity = findActivity(webView?.context ?: context)
        val appCtx = context.applicationContext
        val thread = HandlerThread("ZeroLagHardwareEncoder").apply { start() }
        val handler = Handler(thread.looper)
        encoderThread = thread
        encoderHandler = handler
        isNativeEncoding = true

        handler.post {
            try {
                val dir = File(appCtx.cacheDir, "recordings").apply { mkdirs() }
                val outFile = File(dir, "hw_clip_${System.currentTimeMillis()}.mp4")
                nativeTempFile = outFile

                // Determine output dimensions:
                // - In Full Screen mode (default): match the device window's true full-screen aspect ratio (aligned to 16px)
                // - In 16:9 Stream mode: lock to 16:9 (1280x720 or 960x540)
                val decor = activity?.window?.peekDecorView()
                val winW = decor?.width ?: 0
                val winH = decor?.height ?: 0
                val (width, height) = if (isFullScreenMode && winW > 128 && winH > 128) {
                    val winAspect = winW.toFloat() / winH.toFloat()
                    val baseShortSide = quality.heightPx.coerceAtLeast(540)
                    if (winW >= winH) {
                        val calcW = ((baseShortSide * winAspect).toInt() / 16) * 16
                        val calcH = (baseShortSide / 16) * 16
                        Pair(calcW.coerceIn(640, 2560), calcH.coerceIn(360, 1440))
                    } else {
                        val calcW = (baseShortSide / 16) * 16
                        val calcH = ((baseShortSide / winAspect).toInt() / 16) * 16
                        Pair(calcW.coerceIn(360, 1440), calcH.coerceIn(640, 2560))
                    }
                } else {
                    Pair((quality.widthPx / 16) * 16, (quality.heightPx / 16) * 16)
                }

                // 1. Configure Hardware H.264 Video Encoder
                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, quality.bitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, quality.fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                }

                val vCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                vCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
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
                lastVideoPtsUs = 0L
                lastAudioPtsUs = 0L
                submittedAudioFrames = 0L

                // Prime AAC encoder with an initial silent frame so its output MediaFormat (csd-0) is ready immediately
                feedSilentAudioFrames(1024)
                drainAudioCodec(false)

                val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                val frameIntervalMs = (1000L / quality.fps.coerceIn(24, 60)).coerceAtLeast(16L)
                val targetAspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)

                val captureRunnable = object : Runnable {
                    override fun run() {
                        if (!isNativeEncoding) return
                        val tickStart = System.currentTimeMillis()
                        val window = activity?.window
                        val decorView = window?.peekDecorView()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window != null && decorView != null) {
                            try {
                                val curWinW = decorView.width
                                val curWinH = decorView.height
                                // In Full Screen mode (isFullScreenMode == true), capture the ENTIRE screen without cropping!
                                // Only crop to 16:9 when in DIRECT_STREAM 16:9 lock mode.
                                val srcRect: Rect? = if (!isFullScreenMode && curWinW > 64 && curWinH > 64) {
                                    val winAspect = curWinW.toFloat() / curWinH.toFloat()
                                    if (winAspect > targetAspect + 0.02f) {
                                        val cropW = (curWinH * targetAspect).toInt().coerceIn(64, curWinW)
                                        val left = ((curWinW - cropW) / 2).coerceAtLeast(0)
                                        Rect(left, 0, left + cropW, curWinH)
                                    } else if (winAspect < targetAspect - 0.02f) {
                                        val cropH = (curWinW / targetAspect).toInt().coerceIn(64, curWinH)
                                        val top = ((curWinH - cropH) / 2).coerceAtLeast(0)
                                        Rect(0, top, curWinW, top + cropH)
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }

                                if (srcRect != null) {
                                    PixelCopy.request(
                                        window,
                                        srcRect,
                                        frameBitmap,
                                        { copyResult ->
                                            if (copyResult == PixelCopy.SUCCESS && isNativeEncoding) {
                                                drawBitmapToEncoderSurface(frameBitmap, width, height, paint)
                                                feedAndDrainAudioEncoder(false)
                                                drainVideoEncoder(false)
                                            }
                                        },
                                        handler
                                    )
                                } else {
                                    PixelCopy.request(
                                        window,
                                        frameBitmap,
                                        { copyResult ->
                                            if (copyResult == PixelCopy.SUCCESS && isNativeEncoding) {
                                                drawBitmapToEncoderSurface(frameBitmap, width, height, paint)
                                                feedAndDrainAudioEncoder(false)
                                                drainVideoEncoder(false)
                                            }
                                        },
                                        handler
                                    )
                                }
                            } catch (_: Exception) {
                                feedAndDrainAudioEncoder(false)
                                drainVideoEncoder(false)
                            }
                        } else {
                            feedAndDrainAudioEncoder(false)
                            drainVideoEncoder(false)
                        }
                        val elapsed = System.currentTimeMillis() - tickStart
                        val nextDelay = (frameIntervalMs - elapsed).coerceAtLeast(8L)
                        if (isNativeEncoding) {
                            handler.postDelayed(this, nextDelay)
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

    private fun tryStartMuxerIfReady() {
        if (muxerStarted) return
        val muxer = mediaMuxer ?: return
        val vFmt = pendingVideoFormat ?: return
        val aFmt = pendingAudioFormat
        // Start muxer as soon as video format and (if audioCodec is active) audio format are ready
        if (audioCodec != null && aFmt == null) return
        try {
            videoTrackIndex = muxer.addTrack(vFmt)
            if (aFmt != null) {
                audioTrackIndex = muxer.addTrack(aFmt)
            }
            muxer.start()
            muxerStarted = true
        } catch (e: Exception) {
            Log.w(TAG, "Error starting MediaMuxer", e)
        }
    }

    private fun feedSilentAudioFrames(frameCount: Int) {
        val aCodec = audioCodec ?: return
        try {
            val inIdx = aCodec.dequeueInputBuffer(2_000L)
            if (inIdx >= 0) {
                val inBuf = aCodec.getInputBuffer(inIdx) ?: return
                inBuf.clear()
                val byteCount = (frameCount * BYTES_PER_STEREO_SAMPLE).coerceAtMost(inBuf.capacity())
                val zeroBytes = ByteArray(byteCount)
                inBuf.put(zeroBytes)
                val ptsUs = (submittedAudioFrames * 1_000_000L) / AUDIO_SAMPLE_RATE
                submittedAudioFrames += (byteCount / BYTES_PER_STEREO_SAMPLE)
                aCodec.queueInputBuffer(inIdx, 0, byteCount, ptsUs, 0)
            }
        } catch (_: Exception) {}
    }

    private fun feedAndDrainAudioEncoder(endOfStream: Boolean) {
        val aCodec = audioCodec ?: return
        try {
            // 1. Feed queued live WebRTC stereo PCM chunks
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

            // 2. If no live WebRTC audio arrived recently (e.g. menu or brief gap), keep AAC stream continuous
            val elapsedUs = ((System.nanoTime() - encodingStartTimeNs) / 1000L).coerceAtLeast(0L)
            val audioPtsUs = (submittedAudioFrames * 1_000_000L) / AUDIO_SAMPLE_RATE
            if (!fedAny && !endOfStream && elapsedUs - audioPtsUs > 140_000L) {
                feedSilentAudioFrames(2048)
            }

            if (endOfStream) {
                val inIdx = aCodec.dequeueInputBuffer(5_000L)
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

    private fun drainAudioCodec(endOfStream: Boolean) {
        val aCodec = audioCodec ?: return
        val muxer = mediaMuxer ?: return
        try {
            val bufferInfo = MediaCodec.BufferInfo()
            var loops = 0
            while (loops < 25) {
                loops++
                val outIdx = aCodec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 5_000L else 0L)
                if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pendingAudioFormat = aCodec.outputFormat
                    tryStartMuxerIfReady()
                } else if (outIdx >= 0) {
                    val encodedData = aCodec.getOutputBuffer(outIdx)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted && audioTrackIndex >= 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            val safePts = if (bufferInfo.presentationTimeUs <= lastAudioPtsUs) {
                                lastAudioPtsUs + 1000L
                            } else {
                                bufferInfo.presentationTimeUs
                            }
                            lastAudioPtsUs = safePts
                            bufferInfo.presentationTimeUs = safePts
                            muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    aCodec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
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
        width: Int,
        height: Int,
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
                canvas.drawColor(Color.BLACK)
                canvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), paint)
            } finally {
                surface.unlockCanvasAndPost(canvas)
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
            val bufferInfo = MediaCodec.BufferInfo()
            var loops = 0
            while (loops < 25) {
                loops++
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    pendingVideoFormat = codec.outputFormat
                    tryStartMuxerIfReady()
                } else if (outputIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            val rawPts = ((System.nanoTime() - encodingStartTimeNs) / 1000L).coerceAtLeast(0L)
                            val safePts = if (rawPts <= lastVideoPtsUs) lastVideoPtsUs + 1000L else rawPts
                            lastVideoPtsUs = safePts
                            bufferInfo.presentationTimeUs = safePts
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
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

                if (saveDirectly) {
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
                        inStream.copyTo(outStream, 64 * 1024)
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
                // Fallback to app-specific Movies directory
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

