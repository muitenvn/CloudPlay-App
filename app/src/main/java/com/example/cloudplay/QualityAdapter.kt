package com.example.cloudplay

import android.util.Log
import android.webkit.WebView
import java.lang.ref.WeakReference

/**
 * QualityAdapter: Bridge between AdaptiveQualityManager and the Xbox WebRTC stream.
 * Dynamically adjusts receiver playout delay, jitter buffer target, and bitrate constraints.
 */
object QualityAdapter {
    private const val TAG = "QualityAdapter"
    private var webViewRef: WeakReference<WebView>? = null

    fun setWebView(webView: WebView?) {
        webViewRef = if (webView != null) WeakReference(webView) else null
    }

    /**
     * Applies target bitrate and buffer tuning hint into the active WebRTC stream in WebView.
     */
    fun applyAdaptiveBitrate(targetBitrateMbps: Double, bufferHintMs: Int) {
        val webView = webViewRef?.get() ?: return
        val js = "if (window.__setAdaptiveBitrate) window.__setAdaptiveBitrate($targetBitrateMbps, $bufferHintMs);"
        webView.post {
            try {
                webView.evaluateJavascript(js) { res ->
                    Log.d(TAG, "Applied adaptive bitrate: ${targetBitrateMbps}Mbps, buffer: ${bufferHintMs}ms (res=$res)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to evaluate adaptive bitrate javascript", e)
            }
        }
    }
}
