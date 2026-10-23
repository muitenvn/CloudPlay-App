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
            lowEndDeviceMode: Boolean = false,
            virtualGamepadEnabled: Boolean = false,
            renderScaleFactor: Float = 1.0f,
            fpsCap: Int = 60,
            gpuLoadReduction: Boolean = true
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
                        if (window.__updateGraphicsTuning) window.__updateGraphicsTuning({ renderScale: $renderScaleFactor, fpsCap: $fpsCap, gpuLoadReduction: $gpuLoadReduction, lowEndDeviceMode: $lowEndDeviceMode });
                        if (window.__setVirtualGamepadEnabled) window.__setVirtualGamepadEnabled($virtualGamepadEnabled);
                        if ($lowEndDeviceMode && window.__pauseTranslator) window.__pauseTranslator();
                        return;
                    }
                    window.__cloudPlayInjected = true;
                    window.__virtualGamepadEnabled = $virtualGamepadEnabled;
                    window.__isRecordingPerformanceMode = false;

                    function isFortniteSession() {
                        const h = (location.href || '').toLowerCase();
                        return h.indexOf('fortnite') !== -1 || h.indexOf('bt5p2x999vh2') !== -1 || !!window.__fortniteArenaMode;
                    }
                    window.__isFortniteSession = isFortniteSession;

                    window.__networkProfile = {
                        cellularMode: "$cellularMode",
                        networkTuningMode: "$networkTuningMode",
                        speedFirst: $speedFirstPriority,
                        targetBitrate: $effectiveBitrateMbps,
                        lowEndDeviceMode: $lowEndDeviceMode
                    };

                    window.__graphicsTuning = {
                        renderScale: $renderScaleFactor,
                        fpsCap: $fpsCap,
                        gpuLoadReduction: $gpuLoadReduction,
                        lowEndDeviceMode: $lowEndDeviceMode
                    };

                    window.__updateNetworkProfile = function(conf) {
                        if (!conf) return;
                        window.__networkProfile = Object.assign(window.__networkProfile || {}, conf);
                        if (window.__networkProfile.lowEndDeviceMode && window.__pauseTranslator) {
                            window.__pauseTranslator();
                        }
                        if (window.__applyGpuAndRenderScaleCss) window.__applyGpuAndRenderScaleCss();
                    };

                    const initialBufSec = isFortniteSession() ? 0.008 : (($speedFirstPriority || $lowEndDeviceMode) ? 0.012 : 0.016);
                    window.__currentBufferSec = initialBufSec;
                    window.__targetBufferSec = initialBufSec;
                    window.__bufferGlideTimer = null;

                    window.__setAdaptiveBitrate = function(targetMbps, bufferHintMs, smoothGlide) {
                        const mbps = Math.max(1.0, Math.min(20.0, parseFloat(targetMbps) || 10.0));
                        const bufMs = Math.max(6.0, Math.min(45.0, parseFloat(bufferHintMs) || 14.0));
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
                                            if ('jitterBufferTarget' in r) r.jitterBufferTarget = secVal * 1000.0;
                                        } else if (r.track.kind === 'audio') {
                                            const audioSec = Math.max(0.008, secVal - 0.002);
                                            if ('playoutDelayHint' in r) r.playoutDelayHint = audioSec;
                                            if ('jitterBufferTarget' in r) r.jitterBufferTarget = audioSec * 1000.0;
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
                                            const gt = window.__graphicsTuning || {};
                                            const renderScale = parseFloat(gt.renderScale) || 1.0;
                                            const configuredFpsCap = parseInt(gt.fpsCap, 10) || 60;
                                            const isFortnite = isFortniteSession();
                                            const rawMbps = isLowEndMode ? 5 : ((window.__networkProfile && window.__networkProfile.targetBitrate) ? window.__networkProfile.targetBitrate : $effectiveBitrateMbps);
                                            // Scale target bitrate proportionally when user lowers render resolution (e.g. 60% / 75% / 85%) to save GPU decode overhead
                                            const scaleBitrateFactor = renderScale < 0.70 ? 0.65 : (renderScale < 0.80 ? 0.78 : (renderScale < 0.92 ? 0.88 : 1.0));
                                            const activeMbps = Math.max(3, Math.round(rawMbps * scaleBitrateFactor));
                                            const isLowSaver = isLowEndMode || activeMbps <= 6 || renderScale <= 0.65;
                                            const isMedium = !isLowSaver && activeMbps <= 10;
                                            const isHigh = !isLowSaver && activeMbps > 10 && activeMbps <= 15;
                                            const targetKbps = isLowEndMode ? 4500 : Math.min(Math.max(activeMbps * 1000, isFortnite ? 5500 : 3200), 20000);
                                            const negotiatedFps = configuredFpsCap <= 30 ? 30 : ((configuredFpsCap >= 120 || (isFortnite && activeMbps >= 14 && !isLowEndMode)) ? 120 : 60);

                                            // Video bandwidth & framerate negotiation based on user's Graphics & FPS Cap settings
                                            sdp = sdp.replace(/m=video (.*)\r\n/g, 'm=video $1\r\nb=AS:' + targetKbps + '\r\na=framerate:' + negotiatedFps + '\r\n');
                                            if (isLowEndMode && window.__pauseTranslator) {
                                                window.__pauseTranslator();
                                            }

                                            if (isLowSaver) {
                                                // MỨC THẤP / 4G ANTI-BUFFERBLOAT / MÁY YẾU: H.264 Constrained Baseline Zero-B-Frame (42e01f), start-bitrate 2200kbps tránh nghẽn 4G đầu phiên
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e01f;level-asymmetry-allowed=1;packetization-mode=1;x-google-max-bitrate=' + Math.max(3500, targetKbps) + ';x-google-min-bitrate=1200;x-google-start-bitrate=2200\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 minptime=10;ptime=10;maxptime=20;stereo=1;sprop-stereo=1;cbr=1;useinbandfec=1;usedtx=1;maxaveragebitrate=' + (isFortnite ? '64000' : '48000') + '\r\n');
                                            } else if (isMedium) {
                                                // MỨC TRUNG BÌNH (4G TỐC ĐỘ CAO / WI-FI CÂN BẰNG): Zero-B-Frame Constrained Baseline (42e020) khử độ trễ sắp xếp khung hình 16-33ms
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e020;level-asymmetry-allowed=1;packetization-mode=1;x-google-max-bitrate=' + (isFortnite ? '9500' : '8500') + ';x-google-min-bitrate=1800;x-google-start-bitrate=3000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 minptime=10;ptime=10;maxptime=20;stereo=1;sprop-stereo=1;cbr=1;useinbandfec=1;usedtx=1;maxaveragebitrate=' + (isFortnite ? '96000' : '64000') + '\r\n');
                                            } else if (isHigh) {
                                                // MỨC CAO (SẮC NÉT 1080P WI-FI): Zero-B-Frame + khởi động êm 3500kbps tránh dội bộ đệm router Wi-Fi
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e020;level-asymmetry-allowed=1;packetization-mode=1;x-google-max-bitrate=' + (isFortnite ? '15000' : '14000') + ';x-google-min-bitrate=2500;x-google-start-bitrate=3500\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 minptime=10;ptime=10;maxptime=20;stereo=1;sprop-stereo=1;cbr=1;useinbandfec=1;maxaveragebitrate=' + (isFortnite ? '128000;maxplaybackrate=48000' : '96000') + '\r\n');
                                            } else {
                                                // MỨC RẤT CAO (ĐỒ HỌA TỐI ĐA WI-FI 5GHz): 20 Mbps max visual quality (hỗ trợ 120 FPS)
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e020;level-asymmetry-allowed=1;packetization-mode=1;x-google-max-bitrate=20000;x-google-min-bitrate=3000;x-google-start-bitrate=4500\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 minptime=10;ptime=10;maxptime=20;stereo=1;sprop-stereo=1;cbr=1;useinbandfec=1;maxaveragebitrate=128000;maxplaybackrate=48000\r\n');
                                            }
                                            desc = new RTCSessionDescription({ type: desc.type, sdp: sdp });
                                        } catch(sdpErr) {}
                                    }
                                    return origSetRemote(desc);
                                };

                                // Hardware Decoder Stabilization: adaptive initial target (respects 4G HARQ burst cushion vs Wi-Fi arena zero-delay)
                                pc.addEventListener('track', function(evt) {
                                    try {
                                        if (evt.track) {
                                            window.__activeWebRtcTracks = window.__activeWebRtcTracks || {};
                                            window.__activeWebRtcTracks[evt.track.kind] = evt.track;
                                        }
                                        if (evt.streams && evt.streams[0]) {
                                            window.__activeWebRtcMediaStream = evt.streams[0];
                                        }
                                        if (evt.receiver) {
                                            const isFortnite = isFortniteSession();
                                            const isFast = !!(window.__networkProfile && (window.__networkProfile.speedFirst || window.__networkProfile.lowEndDeviceMode));
                                            const baseVideoSec = typeof window.__targetBufferSec === 'number'
                                                ? window.__targetBufferSec
                                                : (isFortnite ? 0.010 : (isFast ? 0.014 : 0.018));
                                            const isAudioTrack = evt.track && evt.track.kind === 'audio';
                                            const delaySec = isAudioTrack ? Math.max(0.008, baseVideoSec - 0.002) : baseVideoSec;
                                            if ('playoutDelayHint' in evt.receiver) {
                                                evt.receiver.playoutDelayHint = delaySec;
                                            }
                                            if ('jitterBufferTarget' in evt.receiver) {
                                                evt.receiver.jitterBufferTarget = delaySec * 1000.0;
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
                                    if (window.__bufferGlideTimer !== null && window.__bufferGlideTimer !== undefined) {
                                        clearInterval(window.__bufferGlideTimer);
                                        window.__bufferGlideTimer = null;
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

                                        // Live-Edge Video Catch-Up Pacer: eliminates accumulated input lag after 4G/Wi-Fi jitter spikes
                                        const vEl = findActiveStreamVideo();
                                        if (vEl && !vEl.paused && vEl.readyState >= 2) {
                                            try {
                                                if (vEl.buffered && vEl.buffered.length > 0) {
                                                    const bufEnd = vEl.buffered.end(vEl.buffered.length - 1);
                                                    const lagSec = bufEnd - vEl.currentTime;
                                                    if (lagSec > 0.22) {
                                                        vEl.currentTime = Math.max(vEl.currentTime, bufEnd - 0.035);
                                                        vEl.playbackRate = 1.0;
                                                    } else if (lagSec > 0.085) {
                                                        vEl.playbackRate = 1.04;
                                                    } else if (vEl.playbackRate !== 1.0) {
                                                        vEl.playbackRate = 1.0;
                                                    }
                                                }
                                            } catch(pacerErr) {}
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
                    const discoveredCardUrls = new Set();

                    function extractActiveGameDomMetadata(overrideUrl, clickedEl) {
                        try {
                            const href = overrideUrl || location.href;
                            if (!href || (href.indexOf('/play/games/') === -1 && href.indexOf('/play/launch/') === -1)) return null;
                            let title = '';
                            if (clickedEl) {
                                title = (clickedEl.getAttribute('aria-label') || clickedEl.getAttribute('title') || clickedEl.innerText || '').trim();
                            }
                            if (!title || title.length < 2 || /^(play|chơi|chơi ngay)$/i.test(title)) {
                                const h1 = document.querySelector('h1');
                                if (h1 && h1.textContent) title = h1.textContent.trim();
                            }
                            if (!title || title.length < 2) {
                                title = (document.title || '').replace(/\s*[•|\-–—]\s*Xbox.*$/i, '').trim();
                            }
                            let imgUrl = '';
                            if (clickedEl) {
                                const cardImg = clickedEl.querySelector('img[src]');
                                if (cardImg && cardImg.src) imgUrl = cardImg.src;
                            }
                            if (!imgUrl) {
                                const ogImg = document.querySelector('meta[property="og:image"]');
                                if (ogImg && ogImg.content) imgUrl = ogImg.content;
                            }
                            if (!imgUrl) {
                                const storeImg = document.querySelector('img[src*="store-images.s-microsoft.com"]');
                                if (storeImg && storeImg.src) imgUrl = storeImg.src;
                            }
                            const supportsTouch = !!document.querySelector('[aria-label*="touch" i], [data-testid*="touch" i]');
                            return {
                                title: title || '',
                                url: href,
                                imageUrl: imgUrl || '',
                                supportsTouch: supportsTouch
                            };
                        } catch(e) {
                            return null;
                        }
                    }

                    function reportActiveWebGame(overrideUrl, clickedEl) {
                        const meta = extractActiveGameDomMetadata(overrideUrl, clickedEl);
                        if (meta) {
                            notify('onWebGameDetected', JSON.stringify(meta));
                        }
                    }

                    let lastReportedProfileKey = '';
                    let lastJumpBackCount = 0;

                    function scanVisibleCatalogGameCards() {
                        if (notifiedStreamConnection || location.href.indexOf('/play/launch/') !== -1 || !!window.__activePeerConnection) return;
                        try {
                            // 1. Detect Xbox Gamertag, Gamerpic & Subscription Tier from xbox.com/play header
                            const profileBtn = document.querySelector(
                                'button[aria-label*="Profile" i], button[aria-label*="Hồ sơ" i], button[data-testid*="profile" i], [data-testid*="gamerpic" i], [class*="ProfileButton" i]'
                            );
                            if (profileBtn) {
                                const avatarImg = profileBtn.querySelector('img[src]') || document.querySelector('img[alt*="gamerpic" i], img[class*="gamerpic" i], img[src*="ssl-images-microsoft.com"], img[src*="xboxlive.com"]');
                                const avatarSrc = avatarImg ? (avatarImg.currentSrc || avatarImg.src || '') : '';
                                let rawLabel = (profileBtn.getAttribute('aria-label') || profileBtn.innerText || (avatarImg ? avatarImg.alt : '') || '').trim();
                                rawLabel = rawLabel.replace(/^(profile|hồ sơ|account|tài khoản|open profile menu for)\s*[:\-]?\s*/i, '').split('\n')[0].trim();
                                if (rawLabel && rawLabel.length >= 2 && rawLabel.length <= 28 && !/sign\s*in|đăng\s*nhập/i.test(rawLabel)) {
                                    const subBadgeEl = document.querySelector('[class*=" ultimate" i], [data-testid*="ultimate" i], [aria-label*="Ultimate" i]');
                                    const tier = subBadgeEl ? 'Game Pass Ultimate' : 'Xbox Cloud Account';
                                    const pKey = rawLabel + '|' + avatarSrc;
                                    if (pKey !== lastReportedProfileKey) {
                                        lastReportedProfileKey = pKey;
                                        notify('onXboxProfileDetected', JSON.stringify({
                                            gamertag: rawLabel,
                                            avatarUrl: avatarSrc,
                                            tier: tier
                                        }));
                                    }
                                }
                            }

                            // 2. Detect "Jump back in" (Chơi gần đây trên Xbox Cloud) carousel
                            const sections = document.querySelectorAll('section, [aria-label*="Jump back in" i], [aria-label*="Recently played" i], [data-testid*="jump" i]');
                            const jumpItems = [];
                            for (let s = 0; s < sections.length; s++) {
                                const sec = sections[s];
                                const heading = sec.querySelector('h2, h3, [role="heading"]');
                                const hText = ((heading ? heading.innerText : '') + ' ' + (sec.getAttribute('aria-label') || '')).toLowerCase();
                                if (hText.indexOf('jump back in') !== -1 || hText.indexOf('recently played') !== -1 || hText.indexOf('tiếp tục') !== -1 || hText.indexOf('gần đây') !== -1) {
                                    const jLinks = sec.querySelectorAll('a[href*="/play/games/"], a[href*="/play/launch/"]');
                                    for (let j = 0; j < jLinks.length && jumpItems.length < 8; j++) {
                                        const ja = jLinks[j];
                                        const jHref = (ja.href || ja.getAttribute('href') || '').split('?')[0];
                                        if (!jHref) continue;
                                        const jImg = ja.querySelector('img[src]');
                                        const jImgSrc = jImg ? (jImg.currentSrc || jImg.src || '') : '';
                                        let jTitle = (ja.getAttribute('aria-label') || (jImg ? jImg.alt : '') || ja.innerText || '').trim();
                                        jTitle = jTitle.split('\n')[0].replace(/^(play|chơi)\s+/i, '').trim();
                                        jumpItems.push({ title: jTitle, url: jHref, imageUrl: jImgSrc });
                                    }
                                }
                            }
                            if (jumpItems.length > 0 && jumpItems.length !== lastJumpBackCount) {
                                lastJumpBackCount = jumpItems.length;
                                notify('onXboxJumpBackInDetected', JSON.stringify(jumpItems));
                            }

                            // 3. Scrape visible Xbox Cloud catalog game cards
                            const links = document.querySelectorAll('a[href*="/play/games/"]');
                            if (!links || links.length === 0) return;
                            const batch = [];
                            for (let i = 0; i < links.length && batch.length < 24; i++) {
                                const a = links[i];
                                const rawHref = a.href || a.getAttribute('href') || '';
                                if (!rawHref || rawHref.indexOf('/play/games/') === -1) continue;
                                const cleanUrl = rawHref.split('?')[0];
                                if (discoveredCardUrls.has(cleanUrl)) continue;
                                const img = a.querySelector('img[src]');
                                const imgSrc = img ? (img.currentSrc || img.src || '') : '';
                                let title = (a.getAttribute('aria-label') || (img ? img.alt : '') || a.innerText || '').trim();
                                title = title.split('\n')[0].replace(/^(play|chơi)\s+/i, '').trim();
                                if (!title && !imgSrc) continue;
                                discoveredCardUrls.add(cleanUrl);
                                batch.push({
                                    title: title,
                                    url: cleanUrl,
                                    imageUrl: imgSrc,
                                    supportsTouch: !!(a.innerHTML && a.innerHTML.toLowerCase().indexOf('touch') !== -1)
                                });
                            }
                            if (batch.length > 0) {
                                notify('onCatalogCardsDiscovered', JSON.stringify(batch));
                            }
                        } catch(e) {}
                    }

                    if (location.href.indexOf('/play/launch/') !== -1) {
                        reportActiveWebGame(location.href, null);
                        notify('onGameLaunchInitiated', document.title || 'Xbox Game');
                    } else if (location.href.indexOf('/play/games/') !== -1) {
                        setTimeout(function() { reportActiveWebGame(location.href, null); }, 600);
                    }

                    function handleNavigationChange() {
                        if (location.href !== lastUrl) {
                            const wasInLaunch = lastUrl.indexOf('/play/launch/') !== -1;
                            const nowInLaunch = location.href.indexOf('/play/launch/') !== -1;
                            const nowInGamePage = location.href.indexOf('/play/games/') !== -1;
                            lastUrl = location.href;
                            notifiedStreamConnection = false;
                            regionCheckCountForUrl = 0;
                            notify('onUrlChanged', location.href);
                            if (nowInLaunch || nowInGamePage) {
                                reportActiveWebGame(location.href, null);
                                setTimeout(function() { reportActiveWebGame(location.href, null); }, 900);
                            }
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
                            optimizeVideoElementLowLatency(target);
                            if ((target.currentTime > 0 || target.readyState >= 2) && !target.paused) {
                                notifiedStreamConnection = true;
                                reportActiveWebGame(location.href, null);
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

                    function ensureFortniteMultiTouchClawCss() {
                        if (document.getElementById('__cloudplay_fortnite_claw_css')) return;
                        try {
                            const style = document.createElement('style');
                            style.id = '__cloudplay_fortnite_claw_css';
                            style.textContent = 'video, canvas, #game-stream, [data-testid*="stream" i], [data-testid*="media" i], [data-testid*="touch" i] { touch-action: none !important; overscroll-behavior: none !important; user-select: none !important; -webkit-user-select: none !important; -webkit-touch-callout: none !important; -webkit-user-drag: none !important; -webkit-tap-highlight-color: transparent !important; }';
                            (document.head || document.documentElement).appendChild(style);
                        } catch(e) {}
                    }

                    function tryAutoBypassFortniteControllerModal() {
                        if (!isFortniteSession() || location.href.indexOf('/launch/') === -1) return;
                        try {
                            const buttons = document.querySelectorAll('button, [role="button"]');
                            for (let i = 0; i < buttons.length; i++) {
                                const b = buttons[i];
                                const t = ((b.innerText || b.textContent || '') + ' ' + (b.getAttribute('aria-label') || '')).toLowerCase();
                                if (t.includes('continue anyway') || t.includes('tiếp tục') || t.includes('play with touch') || t.includes('chơi bằng cảm ứng')) {
                                    b.click();
                                    break;
                                }
                            }
                        } catch(e) {}
                    }

                    function optimizeVideoElementLowLatency(video) {
                        if (!video || video.__cpLowLatencyTuned) return;
                        video.__cpLowLatencyTuned = true;
                        try {
                            video.playsInline = true;
                            video.autoplay = true;
                            if ('disablePictureInPicture' in video) video.disablePictureInPicture = true;
                            if ('disableRemotePlayback' in video) video.disableRemotePlayback = true;
                            video.style.touchAction = 'none';
                            video.style.userSelect = 'none';
                            video.style.webkitUserSelect = 'none';
                            video.style.willChange = 'transform';
                            video.style.contain = 'layout style paint';
                            ensureFortniteMultiTouchClawCss();
                        } catch(e) {}
                    }

                    // Lightweight fallback check (2500ms) for edge cases where video mounts silently & region lock check
                    setInterval(function() {
                        handleNavigationChange();
                        if (!notifiedStreamConnection && regionCheckCountForUrl < 4) {
                            checkRegionLockDomState();
                        }
                        const isLaunchPage = location.href.indexOf('/play/launch/') !== -1 || !!window.__activePeerConnection;
                        if (isLaunchPage) {
                            ensureFortniteMultiTouchClawCss();
                            if (!notifiedStreamConnection) {
                                tryAutoBypassFortniteControllerModal();
                                const video = document.querySelector('video#stream-video, video[data-testid="stream-video"], #segmented-video video, video');
                                if (video) {
                                    optimizeVideoElementLowLatency(video);
                                    if ((video.currentTime > 0 || video.readyState >= 2 || video.videoWidth > 0) && !video.paused) {
                                        notifiedStreamConnection = true;
                                        reportActiveWebGame(location.href, null);
                                        notify('onStreamConnected', 'video_active');
                                    }
                                }
                            }
                        } else {
                            scanVisibleCatalogGameCards();
                        }
                    }, 2500);

                    document.addEventListener('click', function(e) {
                        const target = e.target;
                        if (target) {
                            const gameLink = target.closest('a[href*="/play/games/"], a[href*="/play/launch/"]');
                            if (gameLink && gameLink.href) {
                                reportActiveWebGame(gameLink.href, gameLink);
                            }
                            const btn = target.closest('button, a');
                            if (btn) {
                                const txt = (btn.innerText || btn.getAttribute('aria-label') || '').toLowerCase();
                                const testId = (btn.getAttribute('data-testid') || '').toLowerCase();
                                if (txt.includes('play') || txt.includes('chơi') || testId.includes('play') || testId.includes('launch')) {
                                    notifiedStreamConnection = false;
                                    reportActiveWebGame( (gameLink && gameLink.href) ? gameLink.href : location.href, gameLink || btn );
                                    notify('onGameLaunchInitiated', document.title || 'Xbox Game');
                                }
                            }
                        }
                    }, true);

                    // ==========================================
                    // 6. Synthetic Gamepad Hook & Anti-AFK (Ultra-Low-Latency Direct Bridge + Fortnite Native Touch Coexistence)
                    // ==========================================
                    const nowTs = () => (typeof performance !== 'undefined' && performance.now) ? performance.now() : Date.now();
                    let virtualGamepadActive = !!window.__virtualGamepadEnabled;
                    let gamepadConnectedDispatched = false;
                    const virtualGamepadState = {
                        id: "Xbox 360 Controller (XInput STANDARD GAMEPAD)",
                        index: 0,
                        connected: !!window.__virtualGamepadEnabled,
                        timestamp: nowTs(),
                        mapping: "standard",
                        axes: [0, 0, 0, 0],
                        buttons: Array.from({ length: 17 }, () => ({ pressed: false, touched: false, value: 0 })),
                        vibrationActuator: null
                    };

                    function ensureGamepadConnectedEvent() {
                        virtualGamepadActive = true;
                        virtualGamepadState.connected = true;
                        if (!gamepadConnectedDispatched) {
                            gamepadConnectedDispatched = true;
                            try {
                                const evt = new Event('gamepadconnected');
                                evt.gamepad = virtualGamepadState;
                                window.dispatchEvent(evt);
                            } catch (e) {}
                        }
                    }

                    window.__setVirtualGamepadEnabled = function(enabled) {
                        const isEn = !!enabled;
                        window.__virtualGamepadEnabled = isEn;
                        if (isEn) {
                            ensureGamepadConnectedEvent();
                        } else {
                            // Disconnect synthetic gamepad so Fortnite Native Epic Touch Controls activate cleanly without controller conflict
                            virtualGamepadActive = false;
                            virtualGamepadState.connected = false;
                            for (let i = 0; i < 4; i++) virtualGamepadState.axes[i] = 0;
                            for (let b = 0; b < 17; b++) {
                                virtualGamepadState.buttons[b].pressed = false;
                                virtualGamepadState.buttons[b].touched = false;
                                virtualGamepadState.buttons[b].value = 0;
                            }
                            virtualGamepadState.timestamp = nowTs();
                            if (gamepadConnectedDispatched) {
                                gamepadConnectedDispatched = false;
                                try {
                                    const evt = new Event('gamepaddisconnected');
                                    evt.gamepad = virtualGamepadState;
                                    window.dispatchEvent(evt);
                                } catch (e) {}
                            }
                        }
                    };

                    window.__cpBtn = function(idx, pressed, analogVal) {
                        if (idx >= 0 && idx < 17) {
                            ensureGamepadConnectedEvent();
                            const v = (typeof analogVal === 'number') ? Math.max(0, Math.min(1, analogVal)) : (pressed ? 1.0 : 0.0);
                            const isPressed = pressed || v > 0.12;
                            virtualGamepadState.buttons[idx].pressed = isPressed;
                            virtualGamepadState.buttons[idx].touched = isPressed || v > 0.01;
                            virtualGamepadState.buttons[idx].value = v;
                            virtualGamepadState.timestamp = nowTs();
                        }
                    };

                    window.__cpStick = function(xAxis, xVal, yAxis, yVal) {
                        ensureGamepadConnectedEvent();
                        if (xAxis >= 0 && xAxis < 4) virtualGamepadState.axes[xAxis] = Math.max(-1, Math.min(1, xVal));
                        if (yAxis >= 0 && yAxis < 4) virtualGamepadState.axes[yAxis] = Math.max(-1, Math.min(1, yVal));
                        virtualGamepadState.timestamp = nowTs();
                    };

                    window.addEventListener('virtual_gamepad_button', function(e) {
                        if (e && e.detail) {
                            window.__cpBtn(e.detail.index, e.detail.pressed, e.detail.value);
                        }
                    });

                    window.addEventListener('virtual_gamepad_axis', function(e) {
                        if (e && e.detail) {
                            const axis = e.detail.axis;
                            const val = e.detail.value;
                            if (axis >= 0 && axis < 4) {
                                ensureGamepadConnectedEvent();
                                virtualGamepadState.axes[axis] = val;
                                virtualGamepadState.timestamp = nowTs();
                            }
                        }
                    });

                    window.addEventListener('virtual_gamepad_axes_pair', function(e) {
                        if (e && e.detail) {
                            window.__cpStick(e.detail.xAxis, e.detail.xVal, e.detail.yAxis, e.detail.yVal);
                        }
                    });

                    // Anti-AFK Keep-Alive trigger
                    if ($antiAfk) {
                        setInterval(function() {
                            if (location.href.indexOf('/play/launch/') !== -1 && (window.__virtualGamepadEnabled || virtualGamepadActive)) {
                                virtualGamepadState.timestamp = nowTs();
                            }
                        }, 180000);
                    }

                    try {
                        const origGetGamepads = navigator.getGamepads ? navigator.getGamepads.bind(navigator) : null;
                        navigator.getGamepads = function() {
                            const real = origGetGamepads ? origGetGamepads() : [];
                            const list = Array.from(real || []);
                            let physPad = null;
                            for (let i = 0; i < list.length; i++) {
                                if (list[i] && list[i].connected) {
                                    physPad = list[i];
                                    break;
                                }
                            }
                            const shouldExposeVirtual = (!!window.__virtualGamepadEnabled || virtualGamepadActive) && virtualGamepadState.connected;
                            if (!physPad) {
                                if (shouldExposeVirtual) {
                                    list[0] = virtualGamepadState;
                                }
                            } else if (shouldExposeVirtual) {
                                // Hybrid merge: allow on-screen virtual buttons/sticks to work even while a physical pad is connected
                                const mergedButtons = [];
                                for (let b = 0; b < 17; b++) {
                                    const vb = virtualGamepadState.buttons[b] || { pressed: false, touched: false, value: 0 };
                                    const pb = (physPad.buttons && physPad.buttons[b]) ? physPad.buttons[b] : { pressed: false, touched: false, value: 0 };
                                    mergedButtons.push({
                                        pressed: !!(vb.pressed || pb.pressed),
                                        touched: !!(vb.touched || pb.touched),
                                        value: Math.max(vb.value || 0, pb.value || 0)
                                    });
                                }
                                const mergedAxes = [0, 0, 0, 0];
                                for (let a = 0; a < 4; a++) {
                                    const va = virtualGamepadState.axes[a] || 0;
                                    const pa = (physPad.axes && physPad.axes[a]) ? physPad.axes[a] : 0;
                                    mergedAxes[a] = Math.abs(va) >= Math.abs(pa) ? va : pa;
                                }
                                list[physPad.index || 0] = {
                                    id: physPad.id || virtualGamepadState.id,
                                    index: physPad.index || 0,
                                    connected: true,
                                    timestamp: Math.max(physPad.timestamp || 0, virtualGamepadState.timestamp),
                                    mapping: "standard",
                                    axes: mergedAxes,
                                    buttons: mergedButtons
                                };
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
                        window.__clarityBoostActive = !!active;
                        if (active && !window.__isRecordingPerformanceMode) {
                            window.__setClarityPreset("$clarityPreset");
                        } else {
                            window.__setClarityPreset("OFF");
                        }
                    };

                    // ==========================================
                    // 7B. Graphics Render Scale, Aspect Ratio Anti-Squish & Zero-Lag Direct Stream Recorder
                    // ==========================================
                    let gpuLoadStyleEl = null;
                    let videoAspectStyleEl = null;

                    window.__applyVideoAspectAndColorCss = function() {
                        try {
                            if (!videoAspectStyleEl) {
                                videoAspectStyleEl = document.createElement('style');
                                videoAspectStyleEl.id = 'cloudplay-video-aspect-controller';
                                (document.head || document.documentElement).appendChild(videoAspectStyleEl);
                            }
                            const vc = window.__videoDisplayConfig || {};
                            const mode = vc.aspectMode || 'FIT_16_9';
                            const userScaleX = (typeof vc.scaleX === 'number' && !isNaN(vc.scaleX)) ? vc.scaleX : 1.0;
                            const userScaleY = (typeof vc.scaleY === 'number' && !isNaN(vc.scaleY)) ? vc.scaleY : 1.0;
                            const contrast = (typeof vc.contrast === 'number') ? (vc.contrast / 100.0) : 1.05;
                            const saturation = (typeof vc.saturation === 'number') ? (vc.saturation / 100.0) : 1.08;
                            const brightness = (typeof vc.brightness === 'number') ? (vc.brightness / 100.0) : 1.00;

                            let objectFitRule = 'contain';
                            let effScaleX = userScaleX;
                            let effScaleY = userScaleY;

                            if (mode === 'FIT_16_9') {
                                objectFitRule = 'contain';
                            } else if (mode === 'CINEMA_18_9') {
                                // Expand 16:9 stream cleanly to 18:9 (18 / 16 = 1.125x horizontal width) while keeping contain boundaries
                                objectFitRule = 'contain';
                                effScaleX = parseFloat((userScaleX * 1.125).toFixed(4));
                            } else if (mode === 'ZOOM_COVER') {
                                objectFitRule = 'cover';
                            } else if (mode === 'STRETCH_FULL') {
                                objectFitRule = 'fill';
                            } else if (mode === 'CUSTOM_SCALE') {
                                objectFitRule = 'contain';
                            }

                            const hasColorTuning = !window.__isRecordingPerformanceMode &&
                                (Math.abs(contrast - 1.0) > 0.01 || Math.abs(saturation - 1.0) > 0.01 || Math.abs(brightness - 1.0) > 0.01);
                            const filterRule = hasColorTuning
                                ? ('filter: contrast(' + contrast.toFixed(2) + ') saturate(' + saturation.toFixed(2) + ') brightness(' + brightness.toFixed(2) + ') !important;')
                                : '';

                            videoAspectStyleEl.textContent = `
                                #game-stream, [data-testid="media-container"], #segmented-video {
                                    display: flex !important;
                                    align-items: center !important;
                                    justify-content: center !important;
                                    background-color: #000000 !important;
                                    overflow: hidden !important;
                                    width: 100vw !important;
                                    height: 100vh !important;
                                }
                                video#stream-video, video[data-testid="stream-video"], #segmented-video video, video {
                                    object-fit: ` + objectFitRule + ` !important;
                                    object-position: center center !important;
                                    width: 100% !important;
                                    height: 100% !important;
                                    max-width: 100vw !important;
                                    max-height: 100vh !important;
                                    margin: auto !important;
                                    transform-origin: center center !important;
                                    transform: scale(` + effScaleX + `, ` + effScaleY + `) translateZ(0) !important;
                                    backface-visibility: hidden !important;
                                    ` + filterRule + `
                                }
                            `;
                        } catch (e) {}
                    };

                    window.__updateVideoDisplayConfig = function(conf) {
                        if (!conf) return;
                        window.__videoDisplayConfig = Object.assign(window.__videoDisplayConfig || {}, conf);
                        window.__applyVideoAspectAndColorCss();
                    };

                    window.__applyGpuAndRenderScaleCss = function() {
                        try {
                            if (!gpuLoadStyleEl) {
                                gpuLoadStyleEl = document.createElement('style');
                                gpuLoadStyleEl.id = 'cloudplay-gpu-load-optimizer';
                                (document.head || document.documentElement).appendChild(gpuLoadStyleEl);
                            }
                            const gt = window.__graphicsTuning || {};
                            const isLowEnd = !!(gt.lowEndDeviceMode || (window.__networkProfile && window.__networkProfile.lowEndDeviceMode));
                            const reduceGpu = !!(gt.gpuLoadReduction || isLowEnd || window.__isRecordingPerformanceMode);
                            const scale = parseFloat(gt.renderScale) || 1.0;

                            let cssRules = '';
                            if (reduceGpu) {
                                // Strip heavy CSS blurs, shadows, and background animations so GPU focuses 100% on video decoding
                                cssRules += `
                                    *, *::before, *::after {
                                        backdrop-filter: none !important;
                                        -webkit-backdrop-filter: none !important;
                                        box-shadow: none !important;
                                        text-shadow: none !important;
                                    }
                                    [class*="background" i], [class*="ambient" i], [class*="blur" i], [class*="shimmer" i] {
                                        animation: none !important;
                                        transition: none !important;
                                        filter: none !important;
                                    }
                                `;
                            }
                            if (scale < 0.99) {
                                // Use crisp hardware upscaling hints when rendering at 85% / 75% / 60% scale
                                cssRules += `
                                    video#stream-video, video[data-testid="stream-video"], #segmented-video video, video {
                                        image-rendering: -webkit-optimize-contrast !important;
                                        contain: layout style paint !important;
                                        will-change: transform !important;
                                    }
                                `;
                            }
                            gpuLoadStyleEl.textContent = cssRules;
                        } catch (e) {}
                    };

                    window.__updateGraphicsTuning = function(conf) {
                        if (!conf) return;
                        window.__graphicsTuning = Object.assign(window.__graphicsTuning || {}, conf);
                        window.__applyGpuAndRenderScaleCss();
                    };

                    window.__setRecordingPerformanceMode = function(active) {
                        window.__isRecordingPerformanceMode = !!active;
                        if (active) {
                            if (window.__pauseTranslator) window.__pauseTranslator();
                            window.__setClarityPreset("OFF");
                        } else {
                            if (window.__clarityBoostActive) {
                                window.__setClarityPreset("$clarityPreset");
                            }
                            const isLowEnd = !!(window.__networkProfile && window.__networkProfile.lowEndDeviceMode);
                            if (!isLowEnd && location.href.indexOf('/launch/') === -1 && window.__resumeTranslator) {
                                window.__resumeTranslator();
                            }
                        }
                        window.__applyGpuAndRenderScaleCss();
                        window.__applyVideoAspectAndColorCss();
                    };

                    let activeMediaRecorder = null;
                    let activeClonedTracks = [];

                    function cleanupClonedRecordingTracks() {
                        try {
                            if (activeClonedTracks && activeClonedTracks.length > 0) {
                                for (let i = 0; i < activeClonedTracks.length; i++) {
                                    try { activeClonedTracks[i].stop(); } catch (e) {}
                                }
                            }
                        } catch (e) {}
                        activeClonedTracks = [];
                    }

                    window.__startZeroLagStreamRecorder = function(width, height, fps, bitrateBps, captureEngine) {
                        try {
                            if (captureEngine === 'HARDWARE_SCREEN') {
                                return "FALLBACK_NATIVE";
                            }
                            if (typeof MediaRecorder === 'undefined') return "FALLBACK_NATIVE";

                            if (activeMediaRecorder && activeMediaRecorder.state !== 'inactive') {
                                try { activeMediaRecorder.stop(); } catch (e) {}
                            }
                            cleanupClonedRecordingTracks();

                            const video = document.querySelector('video#stream-video, video[data-testid="stream-video"], #segmented-video video, video');
                            let rawSourceStream = null;
                            if (video && video.srcObject && typeof video.srcObject.getVideoTracks === 'function' && video.srcObject.getVideoTracks().length > 0) {
                                rawSourceStream = video.srcObject;
                            } else if (window.__activeWebRtcMediaStream && typeof window.__activeWebRtcMediaStream.getVideoTracks === 'function' && window.__activeWebRtcMediaStream.getVideoTracks().length > 0) {
                                rawSourceStream = window.__activeWebRtcMediaStream;
                            } else if (window.__activeWebRtcTracks && window.__activeWebRtcTracks.video) {
                                const tracksArr = [window.__activeWebRtcTracks.video];
                                if (window.__activeWebRtcTracks.audio) tracksArr.push(window.__activeWebRtcTracks.audio);
                                rawSourceStream = new MediaStream(tracksArr);
                            } else if (video && typeof video.captureStream === 'function') {
                                rawSourceStream = video.captureStream(fps || 30);
                            } else if (video && typeof video.mozCaptureStream === 'function') {
                                rawSourceStream = video.mozCaptureStream(fps || 30);
                            }

                            if (!rawSourceStream) return "FALLBACK_NATIVE";

                            // Clone tracks so recording reads passively from WebRTC receiver buffer with 0% side effect on live <video> playback
                            const vTracks = rawSourceStream.getVideoTracks ? rawSourceStream.getVideoTracks() : [];
                            if (!vTracks || vTracks.length === 0) return "FALLBACK_NATIVE";
                            const clonedVideoTrack = (typeof vTracks[0].clone === 'function') ? vTracks[0].clone() : vTracks[0];
                            if (clonedVideoTrack !== vTracks[0]) activeClonedTracks.push(clonedVideoTrack);

                            const aTracks = rawSourceStream.getAudioTracks ? rawSourceStream.getAudioTracks() : [];
                            let clonedAudioTrack = null;
                            if (aTracks && aTracks.length > 0) {
                                clonedAudioTrack = (typeof aTracks[0].clone === 'function') ? aTracks[0].clone() : aTracks[0];
                                if (clonedAudioTrack !== aTracks[0]) activeClonedTracks.push(clonedAudioTrack);
                            }

                            const streamWithAudio = clonedAudioTrack
                                ? new MediaStream([clonedVideoTrack, clonedAudioTrack])
                                : new MediaStream([clonedVideoTrack]);
                            const streamVideoOnly = new MediaStream([clonedVideoTrack]);

                            // Try combinations in priority order inside try/catch so Android 15 WebView never fails on avc1+opus mismatch
                            const mimeCandidates = [
                                'video/webm;codecs=h264,opus',
                                'video/webm;codecs=h264',
                                'video/mp4;codecs=avc1.42E01E,mp4a.40.2',
                                'video/mp4;codecs=avc1.42E01E',
                                'video/mp4;codecs=avc1',
                                'video/mp4',
                                'video/webm;codecs=vp8,opus',
                                'video/webm',
                                ''
                            ];

                            const streamCandidates = clonedAudioTrack ? [streamWithAudio, streamVideoOnly] : [streamVideoOnly];
                            let recorder = null;
                            let actualMime = 'video/webm';

                            outerLoop:
                            for (let sIdx = 0; sIdx < streamCandidates.length; sIdx++) {
                                const targetStream = streamCandidates[sIdx];
                                for (let mIdx = 0; mIdx < mimeCandidates.length; mIdx++) {
                                    const candidateMime = mimeCandidates[mIdx];
                                    if (candidateMime && MediaRecorder.isTypeSupported && !MediaRecorder.isTypeSupported(candidateMime)) {
                                        continue;
                                    }
                                    try {
                                        const opts = { videoBitsPerSecond: bitrateBps || 4500000 };
                                        if (candidateMime) opts.mimeType = candidateMime;
                                        const testRec = new MediaRecorder(targetStream, opts);
                                        actualMime = testRec.mimeType || candidateMime || 'video/webm';
                                        testRec.ondataavailable = function(evt) {
                                            if (!evt || !evt.data || evt.data.size === 0) return;
                                            const isFinal = (testRec.state === 'inactive');
                                            const reader = new FileReader();
                                            reader.onloadend = function() {
                                                if (reader.result && window.CloudPlayBridge && window.CloudPlayBridge.onScreenRecordChunk) {
                                                    window.CloudPlayBridge.onScreenRecordChunk(reader.result, isFinal, actualMime);
                                                }
                                            };
                                            reader.readAsDataURL(evt.data);
                                        };
                                        testRec.onstop = function() {
                                            cleanupClonedRecordingTracks();
                                            setTimeout(function() {
                                                if (window.CloudPlayBridge && window.CloudPlayBridge.onScreenRecordChunk) {
                                                    window.CloudPlayBridge.onScreenRecordChunk("", true, actualMime);
                                                }
                                            }, 180);
                                        };
                                        // 2000ms timeslice cuts IPC overhead in half for zero micro-stutter
                                        testRec.start(2000);
                                        recorder = testRec;
                                        break outerLoop;
                                    } catch (tryErr) {
                                        // Continue to next codec/stream candidate
                                    }
                                }
                            }

                            if (!recorder) {
                                cleanupClonedRecordingTracks();
                                return "FALLBACK_NATIVE";
                            }

                            activeMediaRecorder = recorder;
                            return "DIRECT_STREAM:" + actualMime;
                        } catch (err) {
                            cleanupClonedRecordingTracks();
                            return "FALLBACK_NATIVE";
                        }
                    };

                    window.__stopZeroLagStreamRecorder = function() {
                        try {
                            if (activeMediaRecorder && activeMediaRecorder.state !== 'inactive') {
                                activeMediaRecorder.stop();
                            } else {
                                cleanupClonedRecordingTracks();
                            }
                            activeMediaRecorder = null;
                        } catch (e) {
                            cleanupClonedRecordingTracks();
                        }
                    };

                    window.__toggleClarityBoost($clarityBoost);
                    window.__applyGpuAndRenderScaleCss();
                    window.__applyVideoAspectAndColorCss();
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
        if (url.isNullOrBlank()) return false
        return isTrustedHost(url)
    }

    @JavascriptInterface
    fun onScreenRecordChunk(base64Data: String?, isLastChunk: Boolean, mimeType: String?) {
        if (!isCallerTrusted()) return
        ZeroLagScreenRecorder.onWebStreamChunkReceived(base64Data ?: "", isLastChunk, mimeType)
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
    fun onWebGameDetected(jsonStr: String?) {
        if (!isCallerTrusted() || jsonStr.isNullOrBlank()) return
        try {
            val obj = JSONObject(jsonStr)
            val title = obj.optString("title", "").takeIf { it.isNotBlank() }
            val url = obj.optString("url", "").trim()
            val imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() }
            val supportsTouch = if (obj.has("supportsTouch")) obj.optBoolean("supportsTouch", true) else null
            controller.onWebGameMetadataDetected(
                title = title,
                url = url,
                imageUrl = imageUrl,
                supportsTouch = supportsTouch
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse onWebGameDetected: ${e.message}")
        }
    }

    @JavascriptInterface
    fun onCatalogCardsDiscovered(jsonArrayStr: String?) {
        if (!isCallerTrusted() || jsonArrayStr.isNullOrBlank()) return
        com.example.data.GameLibraryRepository.ingestDiscoveredWebGames(jsonArrayStr)
    }

    @JavascriptInterface
    fun onXboxProfileDetected(jsonStr: String?) {
        if (!isCallerTrusted() || jsonStr.isNullOrBlank()) return
        try {
            val obj = JSONObject(jsonStr)
            val gamertag = obj.optString("gamertag", "").takeIf { it.isNotBlank() }
            val avatarUrl = obj.optString("avatarUrl", "").takeIf { it.isNotBlank() }
            val tier = obj.optString("tier", "").takeIf { it.isNotBlank() }
            com.example.data.GameLibraryRepository.onXboxProfileSynced(gamertag, avatarUrl, tier)
        } catch (_: Exception) {
        }
    }

    @JavascriptInterface
    fun onXboxJumpBackInDetected(jsonArrayStr: String?) {
        if (!isCallerTrusted() || jsonArrayStr.isNullOrBlank()) return
        com.example.data.GameLibraryRepository.onXboxJumpBackInSynced(jsonArrayStr)
    }

    @JavascriptInterface
    fun onStreamConnected(detail: String?) {
        if (!isCallerTrusted()) return
        Log.d(TAG, "Stream established: $detail — activating low-latency lock & pausing DOM translator")
        NetworkOptimizer.setStreamActive(true)
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
        Log.d(TAG, "Stream ended: $detail — releasing low-latency lock & resuming DOM translator if allowed")
        NetworkOptimizer.setStreamActive(false)
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
            if (isTrustedHost(targetUrl)) {
                currentNavigatedUrl = targetUrl
            }
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
                val safeCountry = JSONObject.quote(nextServer.countryCode)
                val safeIp = JSONObject.quote(nextServer.ip)
                val safeLocale = JSONObject.quote(locale)
                wv.evaluateJavascript(
                    "window.__lastUnlockSignal = null; if (window.__updateRegionAndLocale) window.__updateRegionAndLocale({ country: $safeCountry, ip: $safeIp, locale: $safeLocale }); location.reload();",
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


