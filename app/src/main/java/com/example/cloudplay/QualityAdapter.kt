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
        // Chỉ gửi lệnh cập nhật vào WebView khi mức bitrate mục tiêu thay đổi đáng kể (>= 1.0 Mbps)
        // để tránh làm phiền luồng chính của Chromium trong lúc đang giải mã khung hình 60FPS.
        if (lastAppliedBitrateMbps > 0 &&
            abs(targetBitrateMbps - lastAppliedBitrateMbps) < 1.0
        ) {
            return
        }
        lastAppliedBitrateMbps = targetBitrateMbps
        lastAppliedBufferMs = bufferHintMs

        val js = "if (window.__setAdaptiveBitrate) window.__setAdaptiveBitrate($targetBitrateMbps, $bufferHintMs, $smoothGlide);"
        webView.post {
            try {
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to evaluate adaptive bitrate javascript", e)
            }
        }
    }
}
