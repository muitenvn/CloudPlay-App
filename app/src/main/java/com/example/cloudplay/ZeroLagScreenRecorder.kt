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
import java.nio.ByteBuffer
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
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScreenRecorderState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val modeLabel: String = "Hardware Zero-Lag",
    val activeQuality: ScreenRecordQuality = ScreenRecordQuality.BALANCED_720P_30,
    val autoLowLoadActive: Boolean = false,
    val lastSavedFileName: String? = null,
    val recordedClipsCount: Int = 0
)

/**
 * ZeroLagScreenRecorder:
 * Hệ thống quay màn hình không lag dành riêng cho Xbox Cloud Gaming:
 * 1. Ưu tiên thu trực tiếp luồng WebRTC (Direct Stream Capture) ngay từ phần tử <video> mà không cần chụp lại khung hình UI,
 *    giúp giảm tải CPU/GPU xuống gần 0% và không cần quyền hệ thống phức tạp.
 * 2. Tự động chuyển sang bộ mã hóa phần cứng H.264 (MediaCodec Surface + Async PixelCopy trên luồng nền riêng)
 *    nếu đang ở ngoài sảnh chờ chưa có luồng video WebRTC.
 * 3. Khi bật "Tự giảm tải đồ họa khi quay", tự động tắt các hiệu ứng làm mờ, bóng đổ và tạm dừng bộ quét DOM nền
 *    để giữ trọn vẹn 60 FPS khi chiến game.
 */
object ZeroLagScreenRecorder {
    private const val TAG = "ZeroLagScreenRecorder"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(ScreenRecorderState())
    val state: StateFlow<ScreenRecorderState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var webViewRef: WeakReference<WebView>? = null
    private var appContext: Context? = null

    // Direct WebRTC stream chunk file
    private var streamTempFile: File? = null
    private var streamOutputStream: BufferedOutputStream? = null
    private var streamMimeType: String = "video/mp4"
    private var usingNativeHardwareFallback: Boolean = false

    // Native Hardware MediaCodec + PixelCopy fallback state (runs 100% off main thread)
    private var encoderThread: HandlerThread? = null
    private var encoderHandler: Handler? = null
    @Volatile
    private var isNativeEncoding = false
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var videoTrackIndex: Int = -1
    private var muxerStarted: Boolean = false
    private var nativeTempFile: File? = null
    private var encodingStartTimeNs: Long = 0L

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

        _state.value = _state.value.copy(
            isRecording = true,
            elapsedSeconds = 0,
            activeQuality = quality,
            autoLowLoadActive = autoReduceLoad,
            modeLabel = if (captureEngine == RecordingCaptureEngine.DIRECT_STREAM) {
                if (isEn) "Direct Stream 0% Lag" else "Luồng Stream Trực Tiếp 0% Lag"
            } else {
                if (isEn) "Hardware Locked 16:9" else "Phần Cứng (Khóa 16:9)"
            }
        )

        startElapsedTimer()

        // Prepare temp file on IO thread
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

        if (webView != null && captureEngine == RecordingCaptureEngine.DIRECT_STREAM) {
            val js = """
                (function() {
                    if (window.__setRecordingPerformanceMode) {
                        window.__setRecordingPerformanceMode(${autoReduceLoad});
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
                                modeLabel = if (isEn) "Direct Stream 0% Lag (16:9)" else "Luồng Stream 0% Lag (Chuẩn 16:9)"
                            )
                        } else {
                            usingNativeHardwareFallback = true
                            _state.value = _state.value.copy(
                                modeLabel = if (isEn) "Hardware Locked 16:9" else "Phần Cứng (Khóa 16:9)"
                            )
                            startNativeHardwareSurfaceRecorder(context, webView, quality)
                        }
                    }
                } catch (e: Exception) {
                    usingNativeHardwareFallback = true
                    startNativeHardwareSurfaceRecorder(context, webView, quality)
                }
            }
        } else {
            if (webView != null) {
                webView.post {
                    try {
                        webView.evaluateJavascript(
                            "if (window.__setRecordingPerformanceMode) window.__setRecordingPerformanceMode(${autoReduceLoad});",
                            null
                        )
                    } catch (_: Exception) {}
                }
            }
            usingNativeHardwareFallback = true
            startNativeHardwareSurfaceRecorder(context, webView, quality)
        }

        val toastMsg = if (isEn) {
            "🔴 Recording started (${quality.localizedShort(true)} • Zero-Lag Mode)"
        } else {
            "🔴 Đã bắt đầu quay (${quality.localizedShort(false)} • Chế độ Không Lag)"
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
                        "if (window.__setRecordingPerformanceMode) window.__setRecordingPerformanceMode(false); if (window.__stopZeroLagStreamRecorder) window.__stopZeroLagStreamRecorder();",
                        null
                    )
                } catch (_: Exception) {}
            }
        }

        if (usingNativeHardwareFallback) {
            stopNativeHardwareSurfaceRecorder(ctx)
        } else {
            // Give JS MediaRecorder 450ms to flush its final chunk if it hasn't already
            scope.launch(Dispatchers.IO) {
                delay(450)
                finalizeDirectStreamFile(ctx)
            }
        }

        _state.value = _state.value.copy(
            isRecording = false,
            autoLowLoadActive = false
        )

        if (ctx != null) {
            val stoppingMsg = if (isEn) "⏹️ Saving video to Gallery (Movies/XboxCloudPlay)..." else "⏹️ Đang lưu video vào Thư viện ảnh (Movies/XboxCloudPlay)..."
            Toast.makeText(ctx, stoppingMsg, Toast.LENGTH_SHORT).show()
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
                if (isLastChunk) {
                    finalizeDirectStreamFile(appContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error writing web stream chunk", e)
            }
        }
    }

    @Synchronized
    private fun finalizeDirectStreamFile(ctx: Context?) {
        val tmpFile = streamTempFile ?: return
        streamTempFile = null
        try {
            streamOutputStream?.flush()
            streamOutputStream?.close()
        } catch (_: Exception) {}
        streamOutputStream = null

        if (!tmpFile.exists() || tmpFile.length() < 512L) {
            tmpFile.delete()
            return
        }

        // Remux into a standard, clean MP4 container with full video + audio track preservation
        val parentDir = tmpFile.parentFile ?: File(ctx?.cacheDir ?: appContext?.cacheDir, "recordings")
        val cleanMp4File = File(parentDir, "clean_clip_${System.currentTimeMillis()}.mp4")
        val remuxSuccess = remuxOrConvertToStandardMp4(tmpFile, cleanMp4File)
        val fileToSave = if (remuxSuccess && cleanMp4File.exists() && cleanMp4File.length() > 512L) {
            tmpFile.delete()
            cleanMp4File
        } else {
            cleanMp4File.delete()
            tmpFile
        }

        // Always save as standard .mp4 with video/mp4 MIME type so Android Gallery, players and social apps play it seamlessly
        val savedName = saveVideoFileToMediaStore(ctx, fileToSave, "mp4", "video/mp4")
        fileToSave.delete()

        if (savedName != null) {
            notifyClipSavedOnMain(ctx, savedName)
        }
    }

    /**
     * Remuxes any incoming video stream (WebM or fragmented MP4) into a standard, clean MPEG-4 (.mp4)
     * container preserving video (H.264/AVC) and audio tracks using native Android MediaExtractor + MediaMuxer.
     */
    private fun remuxOrConvertToStandardMp4(inputFile: File, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(inputFile.absolutePath) }
            val trackCount = extractor.trackCount
            if (trackCount == 0) return false

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()
            var maxBufSize = 512 * 1024

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val dstIndex = try {
                        muxer.addTrack(format)
                    } catch (e: Exception) {
                        Log.w(TAG, "Muxer could not directly accept track format: $mime", e)
                        -1
                    }
                    if (dstIndex >= 0) {
                        extractor.selectTrack(i)
                        trackMap[i] = dstIndex
                        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                            if (size > maxBufSize) maxBufSize = size
                        }
                    }
                }
            }

            if (trackMap.isEmpty()) return false

            muxer.start()
            val buffer = ByteBuffer.allocateDirect(maxBufSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val srcIndex = extractor.sampleTrackIndex
                if (srcIndex < 0) break
                val dstIndex = trackMap[srcIndex]
                if (dstIndex != null) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size > 0) {
                        bufferInfo.presentationTimeUs = extractor.sampleTime
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(dstIndex, buffer, bufferInfo)
                    }
                }
                extractor.advance()
            }

            muxer.stop()
            return outputFile.exists() && outputFile.length() > 512L
        } catch (e: Exception) {
            Log.w(TAG, "Remux to MP4 note: fallback to direct stream", e)
            return false
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }


    private fun startNativeHardwareSurfaceRecorder(
        context: Context,
        webView: WebView?,
        quality: ScreenRecordQuality
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

                // Align dimensions to multiples of 16 for hardware H.264 encoders
                val width = (quality.widthPx / 16) * 16
                val height = (quality.heightPx / 16) * 16
                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, quality.bitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, quality.fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                }

                val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surface = codec.createInputSurface()
                codec.start()

                val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                mediaCodec = codec
                inputSurface = surface
                mediaMuxer = muxer
                videoTrackIndex = -1
                muxerStarted = false
                encodingStartTimeNs = System.nanoTime()

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
                                val winW = decorView.width
                                val winH = decorView.height
                                // Compute aspect-ratio-preserving crop rect so 20:9 / 21:9 or portrait screens are never squished into 16:9
                                val srcRect: Rect? = if (winW > 64 && winH > 64) {
                                    val winAspect = winW.toFloat() / winH.toFloat()
                                    if (winAspect > targetAspect + 0.02f) {
                                        // Ultrawide phone screen (e.g. 20:9 / 21:9 landscape): crop centered 16:9 active stream region
                                        val cropW = (winH * targetAspect).toInt().coerceIn(64, winW)
                                        val left = ((winW - cropW) / 2).coerceAtLeast(0)
                                        Rect(left, 0, left + cropW, winH)
                                    } else if (winAspect < targetAspect - 0.02f) {
                                        // Taller screen or portrait orientation: crop centered 16:9 horizontal band
                                        val cropH = (winW / targetAspect).toInt().coerceIn(64, winH)
                                        val top = ((winH - cropH) / 2).coerceAtLeast(0)
                                        Rect(0, top, winW, top + cropH)
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
                                                drainEncoder(false)
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
                                                drainEncoder(false)
                                            }
                                        },
                                        handler
                                    )
                                }
                            } catch (_: Exception) {
                                drainEncoder(false)
                            }
                        } else {
                            drainEncoder(false)
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

    private fun drainEncoder(endOfStream: Boolean) {
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
                    if (!muxerStarted) {
                        videoTrackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outputIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            bufferInfo.presentationTimeUs = (System.nanoTime() - encodingStartTimeNs) / 1000L
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
            Log.w(TAG, "Error draining hardware encoder", e)
        }
    }

    private fun stopNativeHardwareSurfaceRecorder(ctx: Context?) {
        isNativeEncoding = false
        val handler = encoderHandler
        val thread = encoderThread
        encoderHandler = null
        encoderThread = null

        if (handler != null && thread != null) {
            handler.post {
                try {
                    drainEncoder(true)
                } catch (_: Exception) {}
                try {
                    mediaCodec?.stop()
                    mediaCodec?.release()
                } catch (_: Exception) {}
                mediaCodec = null
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

                val outFile = nativeTempFile
                nativeTempFile = null
                if (outFile != null && outFile.exists() && outFile.length() > 512L) {
                    val savedName = saveVideoFileToMediaStore(ctx, outFile, "mp4", "video/mp4")
                    outFile.delete()
                    if (savedName != null) {
                        notifyClipSavedOnMain(ctx, savedName)
                    }
                } else {
                    outFile?.delete()
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
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
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
                "✅ Saved clip: Movies/XboxCloudPlay/$fileName"
            } else {
                "✅ Đã lưu video: Movies/XboxCloudPlay/$fileName"
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
