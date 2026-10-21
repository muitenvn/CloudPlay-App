package com.example.cloudplay

import android.util.Log
import android.webkit.JavascriptInterface
import com.example.vpn.VpnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.ref.WeakReference

data class TranslationInfo(
    val isEnabled: Boolean = true,
    val translatedCount: Int = 0
)

class XboxBridge(
    private val controller: StreamController,
    private var webViewRef: WeakReference<android.webkit.WebView>? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile
    private var currentNavigatedUrl: String? = null

    fun setWebView(webView: android.webkit.WebView) {
        webViewRef = WeakReference(webView)
    }

    fun updateCurrentUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            currentNavigatedUrl = url
        }
    }

    /**
     * Cancels the CoroutineScope and clears WebView reference when WebView or Activity is destroyed.
     */
    fun release() {
        webViewRef?.clear()
        webViewRef = null
        scope.cancel()
    }

    fun cancel() {
        release()
    }

    companion object {
        private const val TAG = "XboxBridge"
        private val _translationInfo = MutableStateFlow(TranslationInfo())
        val translationInfo: StateFlow<TranslationInfo> = _translationInfo.asStateFlow()

        val TRUSTED_DOMAINS = listOf(
            "xbox.com",
            "xboxlive.com",
            "xboxservices.com",
            "live.com",
            "live.net",
            "microsoft.com",
            "microsoftonline.com",
            "msauth.net"
        )

        fun isTrustedDomainHost(host: String?): Boolean {
            if (host.isNullOrBlank()) return false
            val cleanHost = host.lowercase()
            return TRUSTED_DOMAINS.any { domain -> cleanHost == domain || cleanHost.endsWith(".$domain") }
        }

        fun isXboxHost(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val host: String = try {
                android.net.Uri.parse(url)?.host?.lowercase()
            } catch (_: Exception) {
                null
            } ?: try {
                java.net.URI(url).host?.lowercase()
            } catch (_: Exception) {
                null
            } ?: return false

            return host == "xbox.com" || host.endsWith(".xbox.com")
        }

        fun isTrustedHost(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val parsedScheme: String? = try {
                android.net.Uri.parse(url)?.scheme?.lowercase()
            } catch (_: Exception) {
                null
            } ?: try {
                java.net.URI(url).scheme?.lowercase()
            } catch (_: Exception) {
                null
            }
            if (parsedScheme != "https") return false

            val host: String = try {
                android.net.Uri.parse(url)?.host?.lowercase()
            } catch (_: Exception) {
                null
            } ?: try {
                java.net.URI(url).host?.lowercase()
            } catch (_: Exception) {
                null
            } ?: return false

            return isTrustedDomainHost(host)
        }

        fun getInjectionScript(
            clarityBoost: Boolean,
            clarityPreset: String = "BALANCED",
            maxBitrateMbps: Int = 15,
            antiAfk: Boolean = true,
            targetCountry: String = "JP",
            targetIp: String = "138.199.21.239",
            userLocale: String = "en-US",
            xcloudTranslate: Boolean = true,
            preserveTitles: Boolean = true,
            bilingualMode: Boolean = false,
            hideStreamStats: Boolean = true,
            cellularMode: String = "saver",
            networkTuningMode: String = "auto",
            speedFirstPriority: Boolean = true,
            effectiveBitrateMbps: Int = 8,
            lowEndDeviceMode: Boolean = false
        ): String {
            val translatorScript = XboxTranslator.getTranslatorScript(
                enabled = xcloudTranslate,
                preserveGameTitles = preserveTitles,
                bilingualMode = bilingualMode,
                hideStreamStats = hideStreamStats
            )
            return """
                (function() {
                    if (window.__cloudPlayInjected) {
                        if (window.__updateRegionAndLocale) window.__updateRegionAndLocale({ country: "$targetCountry", ip: "$targetIp", locale: "$userLocale" });
                        if (window.__toggleClarityBoost) window.__toggleClarityBoost($clarityBoost);
                        if (window.__setClarityPreset) window.__setClarityPreset("$clarityPreset");
                        if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation($xcloudTranslate);
                        if (window.__setXCloudTranslationConfig) window.__setXCloudTranslationConfig({ preserveTitles: $preserveTitles, bilingual: $bilingualMode, hideStats: $hideStreamStats });
                        if (window.__toggleStreamStatsOverlay) window.__toggleStreamStatsOverlay($hideStreamStats);
                        if (window.__updateNetworkProfile) window.__updateNetworkProfile({ cellularMode: "$cellularMode", networkTuningMode: "$networkTuningMode", speedFirst: $speedFirstPriority, targetBitrate: $effectiveBitrateMbps, lowEndDeviceMode: $lowEndDeviceMode });
                        if ($lowEndDeviceMode && window.__pauseTranslator) window.__pauseTranslator();
                        return;
                    }
                    window.__cloudPlayInjected = true;

                    window.__networkProfile = {
                        cellularMode: "$cellularMode",
                        networkTuningMode: "$networkTuningMode",
                        speedFirst: $speedFirstPriority,
                        targetBitrate: $effectiveBitrateMbps,
                        lowEndDeviceMode: $lowEndDeviceMode
                    };

                    window.__updateNetworkProfile = function(conf) {
                        if (!conf) return;
                        window.__networkProfile = Object.assign(window.__networkProfile || {}, conf);
                        if (window.__networkProfile.lowEndDeviceMode && window.__pauseTranslator) {
                            window.__pauseTranslator();
                        }
                        console.log("[xCloud Optimizer] Network Profile Active:", window.__networkProfile);
                    };

                    const initialBufSec = ($speedFirstPriority || $lowEndDeviceMode) ? 0.012 : 0.016;
                    window.__currentBufferSec = initialBufSec;
                    window.__targetBufferSec = initialBufSec;
                    window.__bufferGlideTimer = null;

                    window.__setAdaptiveBitrate = function(targetMbps, bufferHintMs, smoothGlide) {
                        const mbps = Math.max(1.0, Math.min(20.0, parseFloat(targetMbps) || 10.0));
                        const bufMs = Math.max(8.0, Math.min(24.0, parseFloat(bufferHintMs) || 12.0));
                        const targetSec = bufMs / 1000.0;

                        if (window.__networkProfile) {
                            window.__networkProfile.targetBitrate = mbps;
                        }

                        window.__targetBufferSec = targetSec;
                        if (typeof window.__currentBufferSec !== 'number') {
                            window.__currentBufferSec = targetSec;
                        }

                        function applyBufferToReceivers(secVal) {
                            if (!window.__activePeerConnection) return;
                            try {
                                const receivers = window.__activePeerConnection.getReceivers ? window.__activePeerConnection.getReceivers() : [];
                                for (let i = 0; i < receivers.length; i++) {
                                    const r = receivers[i];
                                    if (r && r.track) {
                                        if (r.track.kind === 'video') {
                                            if ('playoutDelayHint' in r) r.playoutDelayHint = secVal;
                                            if ('jitterBufferTarget' in r) r.jitterBufferTarget = secVal;
                                        } else if (r.track.kind === 'audio') {
                                            const audioSec = Math.max(0.008, secVal - 0.002);
                                            if ('playoutDelayHint' in r) r.playoutDelayHint = audioSec;
                                        }
                                    }
                                }
                            } catch(e) {}
                        }

                        // Smooth micro-interpolation (0.35ms per 90ms tick) so WebRTC hardware decoder never stutters when changing quality
                        if (smoothGlide === false) {
                            window.__currentBufferSec = targetSec;
                            applyBufferToReceivers(targetSec);
                            return;
                        }

                        if (!window.__bufferGlideTimer) {
                            window.__bufferGlideTimer = setInterval(function() {
                                const diff = window.__targetBufferSec - window.__currentBufferSec;
                                if (Math.abs(diff) <= 0.0004) {
                                    window.__currentBufferSec = window.__targetBufferSec;
                                    applyBufferToReceivers(window.__currentBufferSec);
                                    clearInterval(window.__bufferGlideTimer);
                                    window.__bufferGlideTimer = null;
                                } else {
                                    const step = diff > 0 ? Math.min(diff, 0.00035) : Math.max(diff, -0.00035);
                                    window.__currentBufferSec = parseFloat((window.__currentBufferSec + step).toFixed(5));
                                    applyBufferToReceivers(window.__currentBufferSec);
                                }
                            }, 90);
                        }
                    };

                    function notify(method, arg) {
                        try {
                            if (method === 'onStreamConnected') {
                                if (window.__pauseTranslator) window.__pauseTranslator();
                            } else if (method === 'onStreamDisconnected') {
                                const isLowEnd = !!(window.__networkProfile && window.__networkProfile.lowEndDeviceMode);
                                if (!isLowEnd && window.__resumeTranslator) window.__resumeTranslator();
                            }
                            if (window.CloudPlayBridge && window.CloudPlayBridge[method]) {
                                window.CloudPlayBridge[method](arg);
                            }
                        } catch(e) {
                            console.error("CloudPlayBridge notification error: " + e);
                        }
                    }

                    let targetCountry = "$targetCountry";
                    let bypassIp = "$targetIp";
                    let preferredLocale = "$userLocale";

                    window.__updateRegionAndLocale = function(conf) {
                        if (!conf) return;
                        if (conf.country) targetCountry = conf.country;
                        if (conf.ip) bypassIp = conf.ip;
                        if (conf.locale) {
                            preferredLocale = conf.locale;
                            try {
                                document.cookie = "MSPC-LOCALE=" + preferredLocale + ";domain=.xbox.com;path=/;max-age=31536000";
                            } catch(e) {}
                        }
                    };

                    // Set locale cookie to enforce user's selected language
                    try {
                        document.cookie = "MSPC-LOCALE=" + preferredLocale + ";domain=.xbox.com;path=/;max-age=31536000";
                    } catch(e) {}

                    // ==========================================
                    // 1. Better-xCloud Smart Region Interceptor
                    // ==========================================
                    try {
                        const NATIVE_FETCH = window.fetch;
                        window.fetch = async function(request, init) {
                            let url = typeof request === 'string' ? request : (request ? request.url : '');
                            let isXboxApi = url.includes('/v2/login/user') || 
                                           url.includes('/sessions/cloud/play') || 
                                           url.includes('/titles') ||
                                           url.includes('emerald.xboxservices.com') ||
                                           url.includes('xboxlive.com');

                            if (isXboxApi && !url.includes('xhome')) {
                                if (typeof request === 'string') {
                                    init = init || {};
                                    let headers = new Headers(init.headers || {});
                                    headers.set('X-Forwarded-For', bypassIp);
                                    headers.set('X-Client-IP', bypassIp);
                                    headers.set('X-Real-IP', bypassIp);
                                    headers.set('Accept-Language', preferredLocale + ',en;q=0.9');
                                    init.headers = headers;
                                } else if (request instanceof Request) {
                                    let headers = new Headers(request.headers);
                                    headers.set('X-Forwarded-For', bypassIp);
                                    headers.set('X-Client-IP', bypassIp);
                                    headers.set('X-Real-IP', bypassIp);
                                    headers.set('Accept-Language', preferredLocale + ',en;q=0.9');
                                    request = new Request(request, { headers: headers });
                                }
                            }

                            let response;
                            try {
                                response = await NATIVE_FETCH(request, init);
                            } catch(err) {
                                throw err;
                            }

                            // Intercept /v2/login/user to spoof allowed regions to Japan East / Korea / US
                            if (url.includes('/v2/login/user') && response && response.ok) {
                                try {
                                    const clone = response.clone();
                                    const data = await clone.json();
                                    if (data) {
                                        let targetName = targetCountry === 'KR' ? 'KOREACENTRAL' : 
                                                         (targetCountry === 'US' ? 'WESTUS' : 'JAPANEAST');

                                        let foundTargetRegion = false;
                                        if (data.offeringSettings && Array.isArray(data.offeringSettings.regions)) {
                                            let found = data.offeringSettings.regions.find(r => r && r.name && r.name.toUpperCase() === targetName);
                                            if (found) {
                                                found.isDefault = true;
                                                data.offeringSettings.regions.forEach(r => { if (r !== found) r.isDefault = false; });
                                                foundTargetRegion = true;
                                            } else if (data.offeringSettings.regions.length > 0) {
                                                data.offeringSettings.regions[0].isDefault = true;
                                                foundTargetRegion = true;
                                            }
                                        }

                                        if (foundTargetRegion) {
                                            window.__lastUnlockSignal = 'UNLOCK_OK';
                                            notify('onRegionUnlockSignal', 'UNLOCK_OK');
                                            notify('onRegionBypassed', JSON.stringify({
                                                country: targetCountry,
                                                ip: bypassIp,
                                                targetRegion: targetName
                                            }));
                                        }

                                        const modifiedBody = JSON.stringify(data);
                                        try {
                                            return new Response(modifiedBody, {
                                                status: response.status,
                                                statusText: response.statusText,
                                                headers: response.headers
                                            });
                                        } catch (respErr) {
                                            Object.defineProperty(response, 'json', { value: () => Promise.resolve(data) });
                                            Object.defineProperty(response, 'text', { value: () => Promise.resolve(modifiedBody) });
                                        }
                                    }
                                } catch(e) {
                                    // Ignore non-JSON login responses cleanly
                                }
                            }

                            return response;
                        };

                        // Patch XMLHttpRequest for older components
                        const origXhrOpen = XMLHttpRequest.prototype.open;
                        const origXhrSend = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.open = function(method, url) {
                            this._url = url;
                            return origXhrOpen.apply(this, arguments);
                        };
                        XMLHttpRequest.prototype.send = function(data) {
                            if (this._url && (this._url.includes('/v2/login/user') || this._url.includes('xboxlive.com'))) {
                                try {
                                    this.setRequestHeader('X-Forwarded-For', bypassIp);
                                    this.setRequestHeader('X-Client-IP', bypassIp);
                                    this.setRequestHeader('Accept-Language', preferredLocale + ',en;q=0.9');
                                } catch(e) {}
                            }
                            return origXhrSend.apply(this, arguments);
                        };
                    } catch(e) {
                        console.error("Failed to setup fetch/xhr interceptor", e);
                    }

                    // ==========================================
                    // 2. Spoof Geolocation to Tokyo, Japan
                    // ==========================================
                    try {
                        const fakePosition = {
                            coords: {
                                latitude: 35.6762,
                                longitude: 139.6503,
                                accuracy: 15,
                                altitude: null,
                                altitudeAccuracy: null,
                                heading: null,
                                speed: null
                            },
                            timestamp: Date.now()
                        };
                        navigator.geolocation.getCurrentPosition = function(success) {
                            if (success) success(fakePosition);
                        };
                        navigator.geolocation.watchPosition = function(success) {
                            if (success) success(fakePosition);
                            return 1;
                        };
                    } catch(e) {}

                    // ==========================================
                    // 3. In-WebView Live IP & Region probe
                    // ==========================================
                    setTimeout(async function() {
                        if (location.hostname && (location.hostname.indexOf('xbox.com') !== -1 || location.hostname.indexOf('live.com') !== -1 || location.hostname.indexOf('microsoft') !== -1)) {
                            return;
                        }
                        try {
                            const res = await window.fetch('https://api.country.is/');
                            if (res.ok) {
                                const geo = await res.json();
                                notify('onWebViewIpDetected', JSON.stringify(geo));
                            }
                        } catch(e) {
                            try {
                                const res2 = await window.fetch('https://ipwho.is/');
                                if (res2.ok) {
                                    const geo2 = await res2.json();
                                    notify('onWebViewIpDetected', JSON.stringify(geo2));
                                }
                            } catch(err) {}
                        }
                    }, 1500);

                    // ==========================================
                    // 4. Hook RTCPeerConnection for Stream Detect, SDP Munging & Realtime Telemetry
                    // ==========================================
                    try {
                        const OrigPeerConnection = window.RTCPeerConnection;
                        if (OrigPeerConnection) {
                            window.RTCPeerConnection = function(...args) {
                                const pc = new OrigPeerConnection(...args);
                                if (window.__activeStatsIntervalId) {
                                    clearInterval(window.__activeStatsIntervalId);
                                    window.__activeStatsIntervalId = null;
                                }
                                window.__activePeerConnection = pc;

                                // SDP Munging optimized for Android Hardware MediaCodec Decoder (Better-xCloud method)
                                const origSetRemote = pc.setRemoteDescription.bind(pc);
                                pc.setRemoteDescription = function(desc) {
                                    if (desc && desc.sdp) {
                                        try {
                                            let sdp = desc.sdp;
                                            const isLowEndMode = !!(window.__networkProfile && window.__networkProfile.lowEndDeviceMode);
                                            const activeMbps = isLowEndMode ? 5 : ((window.__networkProfile && window.__networkProfile.targetBitrate) ? window.__networkProfile.targetBitrate : $effectiveBitrateMbps);
                                            const isLowSaver = isLowEndMode || activeMbps <= 6;
                                            const isMedium = !isLowEndMode && activeMbps > 6 && activeMbps <= 10;
                                            const isHigh = !isLowEndMode && activeMbps > 10 && activeMbps <= 15;
                                            const targetKbps = isLowEndMode ? 5000 : Math.min(Math.max(activeMbps * 1000, 3500), 20000);

                                            // Video bandwidth limit (+ prioritize 60 FPS in lowEndDeviceMode)
                                            if (isLowEndMode) {
                                                sdp = sdp.replace(/m=video (.*)\r\n/g, 'm=video $1\r\nb=AS:5000\r\na=framerate:60\r\n');
                                                if (window.__pauseTranslator) window.__pauseTranslator();
                                            } else {
                                                sdp = sdp.replace(/m=video (.*)\r\n/g, 'm=video $1\r\nb=AS:' + targetKbps + '\r\n');
                                            }

                                            if (isLowSaver) {
                                                // MỨC THẤP / CHẾ ĐỘ MÁY YẾU (5 Mbps cố định): H.264 Baseline Level 3.1 nhẹ trong SDP, ưu tiên 60 FPS, giải mã <4ms
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e01f;level-asymmetry-allowed=1;packetization-mode=1;x-google-max-bitrate=5000;x-google-min-bitrate=2500;x-google-start-bitrate=4500\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=48000\r\n');
                                            } else if (isMedium) {
                                                // MỨC TRUNG BÌNH (CÂN BẰNG): H.264 Constrained/Main nhẹ nhàng cho GPU tầm trung, chống nóng máy khi chơi lâu
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e020;x-google-max-bitrate=9000;x-google-min-bitrate=4000;x-google-start-bitrate=7000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=64000\r\n');
                                            } else if (isHigh) {
                                                // MỨC CAO (SẮC NÉT 1080P): 14 Mbps chuẩn Full HD 60/120fps
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 x-google-max-bitrate=14000;x-google-min-bitrate=5000;x-google-start-bitrate=10000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=96000\r\n');
                                            } else {
                                                // MỨC RẤT CAO (ĐỒ HỌA TỐI ĐA): 20 Mbps max visual quality (hỗ trợ 120 FPS)
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 x-google-max-bitrate=20000;x-google-min-bitrate=8000;x-google-start-bitrate=14000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=128000\r\n');
                                            }
                                            desc = new RTCSessionDescription({ type: desc.type, sdp: sdp });
                                        } catch(sdpErr) {}
                                    }
                                    return origSetRemote(desc);
                                };

                                // Hardware Decoder Stabilization: Ultra-low latency jitter buffer (10-12ms)
                                pc.addEventListener('track', function(evt) {
                                    try {
                                        if (evt.receiver) {
                                            const isFast = !!(window.__networkProfile && (window.__networkProfile.speedFirst || window.__networkProfile.lowEndDeviceMode));
                                            const delaySec = isFast ? 0.012 : 0.016;
                                            if ('playoutDelayHint' in evt.receiver) {
                                                evt.receiver.playoutDelayHint = delaySec;
                                            }
                                            if ('jitterBufferTarget' in evt.receiver) {
                                                evt.receiver.jitterBufferTarget = delaySec;
                                            }
                                        }
                                    } catch(trackErr) {}
                                });

                                let statsIntervalId = null;
                                function stopStatsPolling() {
                                    if (statsIntervalId !== null) {
                                        clearInterval(statsIntervalId);
                                        if (window.__activeStatsIntervalId === statsIntervalId) {
                                            window.__activeStatsIntervalId = null;
                                        }
                                        statsIntervalId = null;
                                    }
                                }

                                pc.addEventListener('iceconnectionstatechange', function() {
                                    const state = pc.iceConnectionState;
                                    if (state === 'connected' || state === 'completed') {
                                        notify('onStreamConnected', 'ice_connected');
                                    } else if (state === 'disconnected' || state === 'closed' || state === 'failed') {
                                        stopStatsPolling();
                                        notify('onStreamDisconnected', 'ice_closed');
                                    }
                                });
                                pc.addEventListener('connectionstatechange', function() {
                                    const state = pc.connectionState;
                                    if (state === 'connected') {
                                        notify('onStreamConnected', 'pc_connected');
                                    } else if (state === 'disconnected' || state === 'closed' || state === 'failed') {
                                        stopStatsPolling();
                                        notify('onStreamDisconnected', 'pc_closed');
                                    }
                                });

                                const origClose = pc.close ? pc.close.bind(pc) : null;
                                if (origClose) {
                                    pc.close = function() {
                                        stopStatsPolling();
                                        return origClose();
                                    };
                                }

                                // WebRTC getStats() Real-Time Telemetry Extractor (Real decodeTimeMs, supports up to 120 FPS)
                                let lastBytes = 0;
                                let lastTimestamp = Date.now();
                                let lastFrames = 0;
                                let lastTotalDecodeTime = 0;
                                let lastFramesDecodedForDecode = 0;

                                statsIntervalId = setInterval(async function() {
                                    if (pc.connectionState === 'disconnected' || pc.connectionState === 'closed' || pc.connectionState === 'failed' ||
                                        pc.iceConnectionState === 'disconnected' || pc.iceConnectionState === 'closed' || pc.iceConnectionState === 'failed') {
                                        stopStatsPolling();
                                        return;
                                    }
                                    if (pc.connectionState !== 'connected' && pc.iceConnectionState !== 'connected' && pc.iceConnectionState !== 'completed') return;
                                    try {
                                        const stats = await pc.getStats();
                                        let currentBytes = 0;
                                        let rtt = 0;
                                        let jitter = 0;
                                        let packetsLost = 0;
                                        let packetsReceived = 0;
                                        let framesDecoded = 0;
                                        let framesDropped = 0;
                                        let currentTotalDecodeTime = 0;
                                        let nativeFps = 0;

                                        stats.forEach(report => {
                                            if (report.type === 'inbound-rtp' && report.kind === 'video') {
                                                currentBytes = report.bytesReceived || currentBytes;
                                                packetsLost = report.packetsLost || packetsLost;
                                                packetsReceived = report.packetsReceived || packetsReceived;
                                                if (report.jitter !== undefined) {
                                                    jitter = Math.round(report.jitter * 1000);
                                                }
                                                framesDecoded = report.framesDecoded || framesDecoded;
                                                framesDropped = report.framesDropped || framesDropped;
                                                if (report.framesPerSecond !== undefined && report.framesPerSecond > 0) {
                                                    nativeFps = Math.round(report.framesPerSecond);
                                                }
                                                if (report.totalDecodeTime !== undefined) {
                                                    currentTotalDecodeTime = report.totalDecodeTime;
                                                }
                                            } else if (report.type === 'candidate-pair' && (report.state === 'succeeded' || report.nominated)) {
                                                if (report.currentRoundTripTime !== undefined) {
                                                    rtt = Math.round(report.currentRoundTripTime * 1000);
                                                } else if (report.roundTripTime !== undefined) {
                                                    rtt = Math.round(report.roundTripTime * 1000);
                                                }
                                            } else if (report.type === 'remote-inbound-rtp' && report.roundTripTime !== undefined) {
                                                if (rtt === 0) {
                                                    rtt = Math.round(report.roundTripTime * 1000);
                                                }
                                            }
                                        });

                                        const now = Date.now();
                                        const timeDiff = (now - lastTimestamp) / 1000;
                                        let bitrateMbps = 0.0;
                                        let fps = nativeFps;
                                        if (timeDiff > 0 && lastBytes > 0 && currentBytes >= lastBytes) {
                                            bitrateMbps = parseFloat((((currentBytes - lastBytes) * 8) / (timeDiff * 1000000)).toFixed(2));
                                            if (fps <= 0 && lastFrames > 0 && framesDecoded >= lastFrames) {
                                                fps = Math.round((framesDecoded - lastFrames) / timeDiff);
                                            }
                                            // Clamp up to 120 FPS (Fortnite & Xbox Series X 120FPS cloud streams)
                                            if (fps > 120) fps = 120;
                                        }
                                        lastBytes = currentBytes;
                                        lastTimestamp = now;
                                        lastFrames = framesDecoded;

                                        // Real hardware/software decode time per frame in milliseconds
                                        let decodeTimeMs = 0.0;
                                        if (lastTotalDecodeTime > 0 && currentTotalDecodeTime >= lastTotalDecodeTime &&
                                            lastFramesDecodedForDecode > 0 && framesDecoded > lastFramesDecodedForDecode) {
                                            const deltaDecodeSec = currentTotalDecodeTime - lastTotalDecodeTime;
                                            const deltaFrames = framesDecoded - lastFramesDecodedForDecode;
                                            if (deltaFrames > 0 && deltaDecodeSec >= 0) {
                                                decodeTimeMs = parseFloat(((deltaDecodeSec / deltaFrames) * 1000).toFixed(1));
                                            }
                                        }
                                        lastTotalDecodeTime = currentTotalDecodeTime;
                                        lastFramesDecodedForDecode = framesDecoded;

                                        const totalPackets = packetsReceived + packetsLost;
                                        const lostPercent = totalPackets > 0 ? parseFloat(((packetsLost / totalPackets) * 100).toFixed(1)) : 0.0;

                                        if ((framesDecoded > 0 || currentBytes > 15000) && !notifiedStreamConnection) {
                                            notifiedStreamConnection = true;
                                            notify('onStreamConnected', 'webrtc_frames_active');
                                        }

                                        notify('onStreamTelemetry', JSON.stringify({
                                            fps: fps,
                                            bitrateMbps: bitrateMbps,
                                            decodeTimeMs: decodeTimeMs,
                                            rttMs: rtt,
                                            jitterMs: jitter,
                                            packetsLostPercent: lostPercent,
                                            framesDropped: framesDropped
                                        }));
                                    } catch(err) {}
                                }, 1500);
                                window.__activeStatsIntervalId = statsIntervalId;

                                return pc;
                            };
                            window.RTCPeerConnection.prototype = OrigPeerConnection.prototype;
                        }
                    } catch(e) {
                        console.error("Failed to hook RTCPeerConnection: " + e);
                    }

                    // ==========================================
                    // 5. Event-Driven SPA Navigation & Video Media Stream Detection
                    // ==========================================
                    let lastUrl = location.href;
                    let notifiedStreamConnection = false;
                    let regionCheckCountForUrl = 0;

                    if (location.href.indexOf('/play/launch/') !== -1) {
                        notify('onGameLaunchInitiated', document.title || 'Xbox Game');
                    }

                    function handleNavigationChange() {
                        if (location.href !== lastUrl) {
                            const wasInLaunch = lastUrl.indexOf('/play/launch/') !== -1;
                            const nowInLaunch = location.href.indexOf('/play/launch/') !== -1;
                            lastUrl = location.href;
                            notifiedStreamConnection = false;
                            regionCheckCountForUrl = 0;
                            notify('onUrlChanged', location.href);
                            if (nowInLaunch) {
                                notify('onGameLaunchInitiated', document.title || 'Xbox Game');
                            } else if (wasInLaunch && !nowInLaunch) {
                                const isLowEnd = !!(window.__networkProfile && window.__networkProfile.lowEndDeviceMode);
                                if (!isLowEnd && window.__resumeTranslator) {
                                    window.__resumeTranslator();
                                }
                            }
                        }
                    }

                    // Hook History API (pushState / replaceState) + popstate / hashchange for zero-overhead SPA tracking
                    try {
                        const origPushState = history.pushState;
                        const origReplaceState = history.replaceState;
                        if (origPushState) {
                            history.pushState = function() {
                                const ret = origPushState.apply(this, arguments);
                                handleNavigationChange();
                                return ret;
                            };
                        }
                        if (origReplaceState) {
                            history.replaceState = function() {
                                const ret = origReplaceState.apply(this, arguments);
                                handleNavigationChange();
                                return ret;
                            };
                        }
                        window.addEventListener('popstate', handleNavigationChange);
                        window.addEventListener('hashchange', handleNavigationChange);
                    } catch(e) {}

                    // Listen for video media events (playing / loadeddata / timeupdate) in capture phase
                    function handleVideoMediaEvent(e) {
                        const target = e ? e.target : null;
                        const isLaunchPage = location.href.indexOf('/play/launch/') !== -1 || !!window.__activePeerConnection;
                        if (target && target.tagName === 'VIDEO' && isLaunchPage && !notifiedStreamConnection) {
                            if ((target.currentTime > 0 || target.readyState >= 2) && !target.paused) {
                                notifiedStreamConnection = true;
                                notify('onStreamConnected', 'video_active');
                            }
                        }
                    }
                    document.addEventListener('playing', handleVideoMediaEvent, true);
                    document.addEventListener('loadeddata', handleVideoMediaEvent, true);
                    document.addEventListener('canplay', handleVideoMediaEvent, true);
                    document.addEventListener('ended', function(e) {
                        const target = e ? e.target : null;
                        if (target && target.tagName === 'VIDEO' && notifiedStreamConnection) {
                            notifiedStreamConnection = false;
                            notify('onStreamDisconnected', 'video_ended');
                        }
                    }, true);

                    function hasPlayButtonOnPage() {
                        const els = document.querySelectorAll('button, a[role="button"], [data-testid*="play" i], [data-testid*="launch" i]');
                        for (let i = 0; i < els.length; i++) {
                            const el = els[i];
                            const txt = ((el.innerText || el.textContent || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('data-testid') || '')).trim().toLowerCase();
                            if (/\bplay\b/.test(txt) || txt.includes('play with cloud') || txt.includes('chơi ngay') || txt.includes('chơi trên đám mây') || txt.includes('playbutton')) {
                                return true;
                            }
                        }
                        return false;
                    }

                    function showUnlockFailBannerDom() {
                        if (document.getElementById('__cloudplay_unlock_fail_banner')) return;
                        const bar = document.createElement('div');
                        bar.id = '__cloudplay_unlock_fail_banner';
                        bar.style.cssText = 'position:fixed;bottom:20px;left:50%;transform:translateX(-50%);z-index:2147483647;background:rgba(12,20,15,0.96);border:1.5px solid #ef4444;border-radius:14px;padding:10px 14px;display:flex;align-items:center;gap:10px;box-shadow:0 8px 24px rgba(0,0,0,0.65);font-family:sans-serif;max-width:92vw;color:#fff;';
                        const isViUi = !!window.__xcloudTranslateEnabled || (preferredLocale || '').toLowerCase().startsWith('vi');
                        const msg = document.createElement('span');
                        msg.style.cssText = 'font-size:12.5px;font-weight:700;line-height:1.35;';
                        msg.textContent = isViUi
                            ? 'Máy chủ này không mở khóa được, đổi sang máy chủ khác?'
                            : 'This server cannot unlock the region, switch to another server?';
                        const btnOk = document.createElement('button');
                        btnOk.style.cssText = 'background:#107C10;color:#fff;border:1px solid #22c55e;border-radius:9px;padding:6px 12px;font-size:12px;font-weight:800;cursor:pointer;white-space:nowrap;';
                        btnOk.textContent = isViUi ? 'Xác nhận' : 'Switch';
                        btnOk.onclick = function() {
                            bar.remove();
                            if (window.CloudPlayBridge && window.CloudPlayBridge.confirmSwitchBestServer) {
                                window.CloudPlayBridge.confirmSwitchBestServer();
                            }
                        };
                        const btnClose = document.createElement('button');
                        btnClose.style.cssText = 'background:transparent;color:#9ca3af;border:none;padding:4px 6px;font-size:12px;cursor:pointer;';
                        btnClose.textContent = '✕';
                        btnClose.onclick = function() {
                            bar.remove();
                            if (window.CloudPlayBridge && window.CloudPlayBridge.dismissUnlockFailPrompt) {
                                window.CloudPlayBridge.dismissUnlockFailPrompt();
                            }
                        };
                        bar.appendChild(msg);
                        bar.appendChild(btnOk);
                        bar.appendChild(btnClose);
                        if (document.body) document.body.appendChild(bar);
                    }

                    function checkRegionLockDomState() {
                        // Never read document.body.innerText during active stream, on /launch/ page, or more than 4 times per URL (avoids layout reflow)
                        if (notifiedStreamConnection || !document.body || location.href.indexOf('/launch/') !== -1 || regionCheckCountForUrl >= 4) return;
                        regionCheckCountForUrl++;
                        const bodyText = (document.body.textContent || '').toLowerCase();
                        const hasRegionUnsupportedText =
                            bodyText.includes('unavailable in your region') ||
                            bodyText.includes('not available in your region') ||
                            bodyText.includes('view supported regions') ||
                            bodyText.includes('không khả dụng tại khu vực') ||
                            bodyText.includes('xem các khu vực được hỗ trợ');
                        if (hasRegionUnsupportedText && !hasPlayButtonOnPage()) {
                            if (window.__lastUnlockSignal !== 'UNLOCK_FAIL') {
                                window.__lastUnlockSignal = 'UNLOCK_FAIL';
                                notify('onRegionUnlockSignal', 'UNLOCK_FAIL');
                                showUnlockFailBannerDom();
                            }
                        }
                    }

                    function optimizeVideoElementLowLatency(video) {
                        if (!video || video.__cpLowLatencyTuned) return;
                        video.__cpLowLatencyTuned = true;
                        try {
                            video.playsInline = true;
                            if ('disablePictureInPicture' in video) video.disablePictureInPicture = true;
                            if ('disableRemotePlayback' in video) video.disableRemotePlayback = true;
                        } catch(e) {}
                    }

                    // Lightweight fallback check (2500ms) for edge cases where video mounts silently & region lock check
                    setInterval(function() {
                        handleNavigationChange();
                        if (!notifiedStreamConnection && regionCheckCountForUrl < 4) {
                            checkRegionLockDomState();
                        }
                        const isLaunchPage = location.href.indexOf('/play/launch/') !== -1 || !!window.__activePeerConnection;
                        if (isLaunchPage && !notifiedStreamConnection) {
                            const video = document.querySelector('video#stream-video, video[data-testid="stream-video"], #segmented-video video, video');
                            if (video) {
                                optimizeVideoElementLowLatency(video);
                                if ((video.currentTime > 0 || video.readyState >= 2 || video.videoWidth > 0) && !video.paused) {
                                    notifiedStreamConnection = true;
                                    notify('onStreamConnected', 'video_active');
                                }
                            }
                        }
                    }, 2500);

                    document.addEventListener('click', function(e) {
                        const target = e.target;
                        if (target) {
                            const btn = target.closest('button, a');
                            if (btn) {
                                const txt = (btn.innerText || btn.getAttribute('aria-label') || '').toLowerCase();
                                const testId = (btn.getAttribute('data-testid') || '').toLowerCase();
                                if (txt.includes('play') || txt.includes('chơi') || testId.includes('play') || testId.includes('launch')) {
                                    notifiedStreamConnection = false;
                                    notify('onGameLaunchInitiated', document.title || 'Xbox Game');
                                }
                            }
                        }
                    }, true);

                    // ==========================================
                    // 6. Synthetic Gamepad Hook & Anti-AFK
                    // ==========================================
                    const virtualGamepadState = {
                        id: "Xbox 360 Controller (CloudPlay Gamepad)",
                        index: 0,
                        connected: true,
                        timestamp: Date.now(),
                        mapping: "standard",
                        axes: [0, 0, 0, 0],
                        buttons: Array.from({ length: 17 }, () => ({ pressed: false, touched: false, value: 0 }))
                    };

                    window.addEventListener('virtual_gamepad_button', function(e) {
                        const idx = e.detail.index;
                        const pressed = e.detail.pressed;
                        if (idx >= 0 && idx < 17) {
                            virtualGamepadState.buttons[idx].pressed = pressed;
                            virtualGamepadState.buttons[idx].touched = pressed;
                            virtualGamepadState.buttons[idx].value = pressed ? 1.0 : 0.0;
                            virtualGamepadState.timestamp = Date.now();
                        }
                    });

                    window.addEventListener('virtual_gamepad_axis', function(e) {
                        const axis = e.detail.axis;
                        const val = e.detail.value;
                        if (axis >= 0 && axis < 4) {
                            virtualGamepadState.axes[axis] = val;
                            virtualGamepadState.timestamp = Date.now();
                        }
                    });

                    window.addEventListener('virtual_gamepad_axes_pair', function(e) {
                        const d = e.detail;
                        if (d) {
                            if (d.xAxis >= 0 && d.xAxis < 4) virtualGamepadState.axes[d.xAxis] = d.xVal;
                            if (d.yAxis >= 0 && d.yAxis < 4) virtualGamepadState.axes[d.yAxis] = d.yVal;
                            virtualGamepadState.timestamp = Date.now();
                        }
                    });

                    // Anti-AFK Keep-Alive trigger
                    if ($antiAfk) {
                        setInterval(function() {
                            if (location.href.indexOf('/play/launch/') !== -1) {
                                virtualGamepadState.timestamp = Date.now();
                            }
                        }, 180000);
                    }

                    try {
                        const origGetGamepads = navigator.getGamepads ? navigator.getGamepads.bind(navigator) : null;
                        navigator.getGamepads = function() {
                            const real = origGetGamepads ? origGetGamepads() : [];
                            const list = Array.from(real || []);
                            let hasConnected = false;
                            for (let i = 0; i < list.length; i++) {
                                if (list[i] && list[i].connected) {
                                    hasConnected = true;
                                    break;
                                }
                            }
                            if (!hasConnected) {
                                list[0] = virtualGamepadState;
                            }
                            return list;
                        };
                    } catch(e) {}

                    // ==========================================
                    // 7. Dynamic Clarity Boost Ultra Controller (Zero-Lag GPU Color Matrix, No Drop-Shadow Blur Pass)
                    // ==========================================
                    let clarityStyleEl = null;
                    function getPresetCss(preset) {
                        switch(preset) {
                            case 'ULTRA_SHARP':
                                return 'filter: contrast(1.12) saturate(1.14) brightness(1.02) !important; image-rendering: -webkit-optimize-contrast !important; transform: translateZ(0); backface-visibility: hidden;';
                            case 'OLED_PUNCH':
                                return 'filter: contrast(1.16) saturate(1.20) !important; transform: translateZ(0); backface-visibility: hidden;';
                            case 'OFF':
                                return 'filter: none !important; transform: translateZ(0); backface-visibility: hidden;';
                            case 'BALANCED':
                            default:
                                return 'filter: contrast(1.06) saturate(1.08) !important; image-rendering: -webkit-optimize-contrast !important; transform: translateZ(0); backface-visibility: hidden;';
                        }
                    }

                    window.__setClarityPreset = function(preset) {
                        if (!clarityStyleEl) {
                            clarityStyleEl = document.createElement('style');
                            clarityStyleEl.id = 'cloudplay-clarity-boost';
                            document.head.appendChild(clarityStyleEl);
                        }
                        const css = getPresetCss(preset);
                        clarityStyleEl.textContent = `
                            video#stream-video, video[data-testid="stream-video"], #segmented-video video, video {
                                ` + css + `
                            }
                        `;
                    };

                    window.__toggleClarityBoost = function(active) {
                        if (active) {
                            window.__setClarityPreset("$clarityPreset");
                        } else {
                            window.__setClarityPreset("OFF");
                        }
                    };

                    window.__toggleClarityBoost($clarityBoost);
                    if ($lowEndDeviceMode && window.__pauseTranslator) {
                        window.__pauseTranslator();
                    }
                })();

                // ==========================================
                // 8. Dedicated Xbox Cloud Vietnamese Translator
                // ==========================================
                $translatorScript
            """.trimIndent()
        }
    }

    private fun isCallerTrusted(): Boolean {
        val url = currentNavigatedUrl
        if (url.isNullOrBlank()) return true
        return isTrustedHost(url) || isXboxHost(url)
    }

    @JavascriptInterface
    fun onTranslationStatus(enabled: Boolean, count: Int) {
        if (!isCallerTrusted()) return
        _translationInfo.value = TranslationInfo(isEnabled = enabled, translatedCount = count)
    }

    @JavascriptInterface
    fun onGameLaunchInitiated(gameTitle: String?) {
        if (!isCallerTrusted()) return
        Log.d(TAG, "Game launch detected: $gameTitle")
        controller.onGameLaunchInitiated(gameTitle ?: "Xbox Game")
    }

    @JavascriptInterface
    fun onStreamConnected(detail: String?) {
        if (!isCallerTrusted()) return
        Log.d(TAG, "Stream established: $detail — pausing DOM translator")
        controller.onStreamConnected()
        scope.launch(Dispatchers.Main) {
            webViewRef?.get()?.evaluateJavascript(
                "if (window.__pauseTranslator) window.__pauseTranslator();",
                null
            )
        }
    }

    @JavascriptInterface
    fun onStreamDisconnected(detail: String?) {
        if (!isCallerTrusted()) return
        Log.d(TAG, "Stream ended: $detail — resuming DOM translator if allowed")
        controller.onStreamDisconnected()
        scope.launch(Dispatchers.Main) {
            val cfg = com.example.data.SettingsRepository.config.value
            if (!cfg.lowEndDeviceMode && cfg.isEffectiveTranslateEnabled()) {
                webViewRef?.get()?.evaluateJavascript(
                    "if (window.__resumeTranslator) window.__resumeTranslator();",
                    null
                )
            }
        }
    }

    @JavascriptInterface
    fun onUrlChanged(url: String?) {
        if (!isCallerTrusted()) return
        url?.let { targetUrl ->
            currentNavigatedUrl = targetUrl
            controller.onUrlChanged(targetUrl)
            if (!targetUrl.contains("/launch/") && isXboxHost(targetUrl)) {
                scope.launch(Dispatchers.Main) {
                    val cfg = com.example.data.SettingsRepository.config.value
                    if (!cfg.lowEndDeviceMode && cfg.isEffectiveTranslateEnabled()) {
                        webViewRef?.get()?.evaluateJavascript(
                            "if (window.__resumeTranslator) window.__resumeTranslator();",
                            null
                        )
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun onRegionBypassed(info: String?) {
        if (!isCallerTrusted()) return
        Log.d(TAG, "Region bypassed: $info")
        VpnManager.onRegionBypassed(info ?: "")
    }

    @JavascriptInterface
    fun onRegionUnlockSignal(signal: String?) {
        if (!isCallerTrusted()) return
        val cleanSignal = signal?.trim()?.uppercase() ?: return
        Log.d(TAG, "Region unlock signal: $cleanSignal")
        VpnManager.onUnlockSignalReceived(cleanSignal)
    }

    @JavascriptInterface
    fun confirmSwitchBestServer() {
        if (!isCallerTrusted()) return
        scope.launch(Dispatchers.Main) {
            val wv = webViewRef?.get()
            val nextServer = VpnManager.confirmSwitchToNextBestServer(wv?.context)
            if (nextServer != null && wv != null) {
                val locale = com.example.data.SettingsRepository.config.value.getEffectiveLocale()
                wv.evaluateJavascript(
                    "window.__lastUnlockSignal = null; if (window.__updateRegionAndLocale) window.__updateRegionAndLocale({ country: '${nextServer.countryCode}', ip: '${nextServer.ip}', locale: '$locale' }); location.reload();",
                    null
                )
            }
        }
    }

    @JavascriptInterface
    fun dismissUnlockFailPrompt() {
        if (!isCallerTrusted()) return
        VpnManager.dismissUnlockFailPrompt()
    }

    @JavascriptInterface
    fun onWebViewIpDetected(jsonStr: String?) {
        if (!isCallerTrusted()) return
        if (jsonStr.isNullOrBlank()) return
        try {
            val json = JSONObject(jsonStr)
            val ip = json.optString("ip", "")
            val countryCode = json.optString("country", json.optString("country_code", "JP")).uppercase()
            val countryName = json.optString("country_name", json.optString("country", "Japan"))
            val city = json.optString("city", "")
            if (ip.isNotBlank()) {
                VpnManager.onWebViewIpDetected(ip, countryCode, countryName, city)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse onWebViewIpDetected JSON: ${e.message}")
        }
    }

    @JavascriptInterface
    fun onStreamTelemetry(telemetryJson: String?) {
        if (!isCallerTrusted()) return
        NetworkOptimizer.onTelemetryReceived(telemetryJson)
    }

    @JavascriptInterface
    fun requestSmartTranslate(rawText: String?, callbackId: String?, isBilingual: Boolean) {
        if (!isCallerTrusted()) return
        if (rawText.isNullOrBlank() || callbackId.isNullOrBlank()) return
        if (!com.example.data.SettingsRepository.config.value.isEffectiveTranslateEnabled()) return
        scope.launch(Dispatchers.IO) {
            val translated = GoogleGamingTranslator.translate(rawText, "vi", isBilingual)
            withContext(Dispatchers.Main) {
                val escapedText = JSONObject.quote(translated)
                webViewRef?.get()?.evaluateJavascript(
                    "if (window.__onSmartTranslateResult) window.__onSmartTranslateResult('$callbackId', $escapedText);",
                    null
                )
            }
        }
    }

    @JavascriptInterface
    fun requestBatchTranslate(jsonTexts: String?, batchId: String?, isBilingual: Boolean) {
        if (!isCallerTrusted()) return
        if (jsonTexts.isNullOrBlank() || batchId.isNullOrBlank()) return
        if (!com.example.data.SettingsRepository.config.value.isEffectiveTranslateEnabled()) return
        scope.launch(Dispatchers.IO) {
            try {
                val array = org.json.JSONArray(jsonTexts)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                // Use controlled parallel batch translation instead of sequential loop
                val translatedMap = GoogleGamingTranslator.translateBatch(list, "vi", isBilingual)
                val resultMap = org.json.JSONObject()
                for ((orig, translated) in translatedMap) {
                    resultMap.put(orig, translated)
                }
                withContext(Dispatchers.Main) {
                    webViewRef?.get()?.evaluateJavascript(
                        "if (window.__onBatchTranslateResult) window.__onBatchTranslateResult('$batchId', ${resultMap.toString()});",
                        null
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in requestBatchTranslate", e)
            }
        }
    }

    @JavascriptInterface
    fun log(msg: String?) {
        if (!isCallerTrusted()) return
        Log.d("XboxBridgeJS", msg ?: "")
    }
}


