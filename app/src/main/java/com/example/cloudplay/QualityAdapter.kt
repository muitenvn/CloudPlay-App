package com.example.cloudplay

import android.util.Log
import android.webkit.WebView
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * QualityAdapter: Bridge between AdaptiveQualityManager and the Xbox WebRTC stream.
 * Smoothly interpolates receiver playout delay, jitter buffer target, and bitrate constraints
 * without causing decoder stutters or frame drops during transitions.
 */
object QualityAdapter {
    private const val TAG = "QualityAdapter"
    private var webViewRef: WeakReference<WebView>? = null
    private var lastAppliedBitrateMbps: Double = -1.0
    private var lastAppliedBufferMs: Double = -1.0

    fun setWebView(webView: WebView?) {
        webViewRef = if (webView != null) WeakReference(webView) else null
        if (webView == null) {
            lastAppliedBitrateMbps = -1.0
            lastAppliedBufferMs = -1.0
        }
    }

    /**
     * Compatibility overload accepting integer bufferHintMs.
     */
    fun applyAdaptiveBitrate(targetBitrateMbps: Double, bufferHintMs: Int) {
        applyAdaptiveBitrate(targetBitrateMbps, bufferHintMs.toDouble(), smoothGlide = true)
    }

    /**
     * Applies continuous target bitrate and float buffer tuning hint into the active WebRTC stream
     * using zero-hitch micro-interpolation in JavaScript.
     */
    fun applyAdaptiveBitrate(
        targetBitrateMbps: Double,
        bufferHintMs: Double,
        smoothGlide: Boolean = true
    ) {
        val webView = webViewRef?.get() ?: return
        // Avoid redundant JS evaluations if change is negligible
        if (lastAppliedBitrateMbps > 0 &&
            abs(targetBitrateMbps - lastAppliedBitrateMbps) < 0.08 &&
            abs(bufferHintMs - lastAppliedBufferMs) < 0.15
        ) {
            return
        }
        lastAppliedBitrateMbps = targetBitrateMbps
        lastAppliedBufferMs = bufferHintMs

        val js = "if (window.__setAdaptiveBitrate) window.__setAdaptiveBitrate($targetBitrateMbps, $bufferHintMs, $smoothGlide);"
        webView.post {
            try {
                webView.evaluateJavascript(js) { res ->
                    Log.d(TAG, "Glided adaptive bitrate: ${targetBitrateMbps}Mbps, buffer: ${bufferHintMs}ms (smooth=$smoothGlide, res=$res)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to evaluate adaptive bitrate javascript", e)
            }
        }
    }

    /**
     * Immediately flushes accumulated WebRTC jitter buffer delay and fast-forwards queued video frames
     * to the live edge without reloading the stream.
     */
    fun flushStreamLagNow(targetBitrateMbps: Double = -1.0, bufferHintMs: Double = 10.0) {
        val webView = webViewRef?.get() ?: return
        if (targetBitrateMbps > 0) {
            lastAppliedBitrateMbps = targetBitrateMbps
        }
        lastAppliedBufferMs = bufferHintMs
        val safeMbps = if (targetBitrateMbps > 0) targetBitrateMbps else lastAppliedBitrateMbps.coerceAtLeast(5.0)
        val js = "if (window.__flushStreamLagNow) window.__flushStreamLagNow($safeMbps, $bufferHintMs);"
        webView.post {
            try {
                webView.evaluateJavascript(js) { res ->
                    Log.d(TAG, "Flushed stream lag: bitrate=${safeMbps}Mbps, buffer=${bufferHintMs}ms (res=$res)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to evaluate flushStreamLagNow javascript", e)
            }
        }
    }
}
