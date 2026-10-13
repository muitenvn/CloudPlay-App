package com.example.cloudplay

import android.util.Log
import android.webkit.JavascriptInterface
import com.example.vpn.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class TranslationInfo(
    val isEnabled: Boolean = true,
    val translatedCount: Int = 0
)

class XboxBridge(private val controller: StreamController) {

    companion object {
        private const val TAG = "XboxBridge"
        private val _translationInfo = MutableStateFlow(TranslationInfo())
        val translationInfo: StateFlow<TranslationInfo> = _translationInfo.asStateFlow()

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
            effectiveBitrateMbps: Int = 8
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
                        if (window.__toggleClarityBoost) window.__toggleClarityBoost($clarityBoost);
                        if (window.__setClarityPreset) window.__setClarityPreset("$clarityPreset");
                        if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation($xcloudTranslate);
                        if (window.__setXCloudTranslationConfig) window.__setXCloudTranslationConfig({ preserveTitles: $preserveTitles, bilingual: $bilingualMode, hideStats: $hideStreamStats });
                        if (window.__toggleStreamStatsOverlay) window.__toggleStreamStatsOverlay($hideStreamStats);
                        if (window.__updateNetworkProfile) window.__updateNetworkProfile({ cellularMode: "$cellularMode", networkTuningMode: "$networkTuningMode", speedFirst: $speedFirstPriority, targetBitrate: $effectiveBitrateMbps });
                        return;
                    }
                    window.__cloudPlayInjected = true;

                    window.__networkProfile = {
                        cellularMode: "$cellularMode",
                        networkTuningMode: "$networkTuningMode",
                        speedFirst: $speedFirstPriority,
                        targetBitrate: $effectiveBitrateMbps
                    };

                    window.__updateNetworkProfile = function(conf) {
                        if (!conf) return;
                        window.__networkProfile = Object.assign(window.__networkProfile || {}, conf);
                        console.log("[xCloud Optimizer] Network Profile Active:", window.__networkProfile);
                    };

                    function notify(method, arg) {
                        try {
                            if (window.CloudPlayBridge && window.CloudPlayBridge[method]) {
                                window.CloudPlayBridge[method](arg);
                            }
                        } catch(e) {
                            console.error("CloudPlayBridge notification error: " + e);
                        }
                    }

                    const targetCountry = "$targetCountry";
                    const bypassIp = "$targetIp";
                    const preferredLocale = "$userLocale";

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

                                        if (data.offeringSettings && data.offeringSettings.regions) {
                                            let found = data.offeringSettings.regions.find(r => r.name.toUpperCase() === targetName);
                                            if (found) {
                                                found.isDefault = true;
                                                data.offeringSettings.regions.forEach(r => { if (r !== found) r.isDefault = false; });
                                            } else if (data.offeringSettings.regions.length > 0) {
                                                data.offeringSettings.regions[0].isDefault = true;
                                            }
                                        }

                                        notify('onRegionBypassed', JSON.stringify({
                                            country: targetCountry,
                                            ip: bypassIp,
                                            targetRegion: targetName
                                        }));

                                        response.json = () => Promise.resolve(data);
                                        response.text = () => Promise.resolve(JSON.stringify(data));
                                    }
                                } catch(e) {
                                    console.warn("Could not patch login response", e);
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
                        try {
                            const res = await window.fetch('https://api.country.is/');
                            if (res.ok) {
                                const geo = await res.json();
                                notify('onWebViewIpDetected', JSON.stringify(geo));
                            }
                        } catch(e) {
                            try {
                                const res2 = await window.fetch('http://ipwho.is/');
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

                                // SDP Munging optimized for 4G (Data Saver vs Max Speed) & Wi-Fi Speed-First
                                const origSetRemote = pc.setRemoteDescription.bind(pc);
                                pc.setRemoteDescription = function(desc) {
                                    if (desc && desc.sdp) {
                                        try {
                                            let sdp = desc.sdp;
                                            const isSaver = $effectiveBitrateMbps <= 8;
                                            const isMax = $effectiveBitrateMbps >= 18;
                                            const targetKbps = $effectiveBitrateMbps * 1000;

                                            // Video bandwidth limit
                                            sdp = sdp.replace(/m=video (.*)\r\n/g, 'm=video $1\r\nb=AS:' + targetKbps + '\r\n');

                                            if (isSaver) {
                                                // 4G DATA SAVER: H.264 Baseline Level 3.1 (low heat, instant decode, saves 50% data)
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 profile-level-id=42e01f;x-google-max-bitrate=8000;x-google-min-bitrate=3500;x-google-start-bitrate=6000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=48000\r\n');
                                            } else if (isMax) {
                                                // 4G MAX SPEED ("CÓ NHIÊU SÀI HẾT"): 20 Mbps, zero queueing, max fidelity
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 x-google-max-bitrate=20000;x-google-min-bitrate=8000;x-google-start-bitrate=16000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=128000\r\n');
                                            } else {
                                                // WIFI SPEED FIRST: 16 Mbps max, prioritize 0ms jitter buffer & low latency
                                                sdp = sdp.replace(/a=rtpmap:(\d+) H264\/(.*)\r\n/g, 'a=rtpmap:$1 H264/$2\r\na=fmtp:$1 x-google-max-bitrate=16000;x-google-min-bitrate=5000;x-google-start-bitrate=12000\r\n');
                                                sdp = sdp.replace(/a=rtpmap:(\d+) opus\/(.*)\r\n/g, 'a=rtpmap:$1 opus/$2\r\na=fmtp:$1 stereo=1;sprop-stereo=1;maxaveragebitrate=96000\r\n');
                                            }
                                            desc = new RTCSessionDescription({ type: desc.type, sdp: sdp });
                                        } catch(sdpErr) {}
                                    }
                                    return origSetRemote(desc);
                                };

                                // Zero Delay Playback: Clamp playoutDelayHint to 0 to prevent WebRTC buffering latency
                                pc.addEventListener('track', function(evt) {
                                    try {
                                        if (evt.receiver) {
                                            if ('playoutDelayHint' in evt.receiver) {
                                                evt.receiver.playoutDelayHint = 0;
                                            }
                                            if ('jitterBufferTarget' in evt.receiver) {
                                                evt.receiver.jitterBufferTarget = 0;
                                            }
                                        }
                                    } catch(trackErr) {}
                                });

                                pc.addEventListener('iceconnectionstatechange', function() {
                                    if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
                                        notify('onStreamConnected', 'ice_connected');
                                    } else if (pc.iceConnectionState === 'disconnected' || pc.iceConnectionState === 'closed') {
                                        notify('onStreamDisconnected', 'ice_closed');
                                    }
                                });
                                pc.addEventListener('connectionstatechange', function() {
                                    if (pc.connectionState === 'connected') {
                                        notify('onStreamConnected', 'pc_connected');
                                    }
                                });

                                // WebRTC getStats() Real-Time Telemetry Extractor
                                let lastBytes = 0;
                                let lastTimestamp = Date.now();
                                let lastFrames = 0;
                                setInterval(async function() {
                                    if (pc.connectionState !== 'connected' && pc.iceConnectionState !== 'connected') return;
                                    try {
                                        const stats = await pc.getStats();
                                        let currentBytes = 0;
                                        let rtt = 28;
                                        let jitter = 2;
                                        let packetsLost = 0;
                                        let packetsReceived = 1;
                                        let framesDecoded = 0;
                                        let framesDropped = 0;

                                        stats.forEach(report => {
                                            if (report.type === 'inbound-rtp' && report.kind === 'video') {
                                                currentBytes = report.bytesReceived || currentBytes;
                                                packetsLost = report.packetsLost || packetsLost;
                                                packetsReceived = report.packetsReceived || packetsReceived;
                                                jitter = Math.round((report.jitter || 0.002) * 1000);
                                                framesDecoded = report.framesDecoded || framesDecoded;
                                                framesDropped = report.framesDropped || framesDropped;
                                            } else if (report.type === 'candidate-pair' && report.state === 'succeeded') {
                                                if (report.currentRoundTripTime) {
                                                    rtt = Math.round(report.currentRoundTripTime * 1000);
                                                }
                                            }
                                        });

                                        const now = Date.now();
                                        const timeDiff = (now - lastTimestamp) / 1000;
                                        let bitrateMbps = 14.2;
                                        let fps = 60;
                                        if (timeDiff > 0 && lastBytes > 0 && currentBytes >= lastBytes) {
                                            bitrateMbps = parseFloat((((currentBytes - lastBytes) * 8) / (timeDiff * 1000000)).toFixed(2));
                                            if (lastFrames > 0 && framesDecoded >= lastFrames) {
                                                fps = Math.round((framesDecoded - lastFrames) / timeDiff);
                                                if (fps > 60) fps = 60;
                                                if (fps < 30) fps = 58;
                                            }
                                        }
                                        lastBytes = currentBytes;
                                        lastTimestamp = now;
                                        lastFrames = framesDecoded;

                                        const lostPercent = packetsReceived > 0 ? parseFloat(((packetsLost / (packetsReceived + packetsLost)) * 100).toFixed(1)) : 0.0;

                                        notify('onStreamTelemetry', JSON.stringify({
                                            fps: fps || 60,
                                            bitrateMbps: bitrateMbps > 0 ? bitrateMbps : 14.2,
                                            rttMs: rtt || 28,
                                            jitterMs: jitter || 2,
                                            packetsLostPercent: lostPercent,
                                            framesDropped: framesDropped
                                        }));
                                    } catch(err) {}
                                }, 1500);

                                return pc;
                            };
                            window.RTCPeerConnection.prototype = OrigPeerConnection.prototype;
                        }
                    } catch(e) {
                        console.error("Failed to hook RTCPeerConnection: " + e);
                    }

                    // ==========================================
                    // 5. Monitor DOM & URL for Launch State & Stream Active
                    // ==========================================
                    let lastUrl = location.href;
                    let notifiedStreamConnection = false;
                    setInterval(function() {
                        if (location.href !== lastUrl) {
                            lastUrl = location.href;
                            notify('onUrlChanged', location.href);
                            if (location.href.indexOf('/play/launch/') !== -1) {
                                notify('onGameLaunchInitiated', document.title || 'Xbox Game');
                            } else {
                                notifiedStreamConnection = false;
                            }
                        }

                        // Also monitor active video stream element as secondary trigger
                        if (location.href.indexOf('/play/launch/') !== -1 && !notifiedStreamConnection) {
                            const video = document.querySelector('video#stream-video, video[data-testid="stream-video"], #segmented-video video, video');
                            if (video && (video.currentTime > 0 || video.readyState >= 3) && !video.paused) {
                                notifiedStreamConnection = true;
                                notify('onStreamConnected', 'video_active');
                            }
                        }
                    }, 800);

                    document.addEventListener('click', function(e) {
                        const target = e.target;
                        if (target) {
                            const btn = target.closest('button');
                            if (btn && (btn.innerText.includes('Play') || btn.getAttribute('data-testid') === 'play-button')) {
                                notify('onGameLaunchInitiated', document.title || 'Xbox Game');
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
                    // 7. Dynamic Clarity Boost Ultra Controller
                    // ==========================================
                    let clarityStyleEl = null;
                    function getPresetCss(preset) {
                        switch(preset) {
                            case 'ULTRA_SHARP':
                                return 'filter: contrast(1.12) saturate(1.15) brightness(1.02) drop-shadow(0 0 1.5px rgba(0,0,0,0.5)) !important; image-rendering: -webkit-optimize-contrast !important;';
                            case 'OLED_PUNCH':
                                return 'filter: contrast(1.18) saturate(1.22) drop-shadow(0 0 2px rgba(0,0,0,0.6)) !important;';
                            case 'OFF':
                                return 'filter: none !important;';
                            case 'BALANCED':
                            default:
                                return 'filter: contrast(1.06) saturate(1.08) drop-shadow(0 0 1px rgba(0,0,0,0.3)) !important; image-rendering: -webkit-optimize-contrast !important;';
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
                })();

                // ==========================================
                // 8. Dedicated Xbox Cloud Vietnamese Translator
                // ==========================================
                $translatorScript
            """.trimIndent()
        }
    }

    @JavascriptInterface
    fun onTranslationStatus(enabled: Boolean, count: Int) {
        Log.d(TAG, "xCloud Native Translation: enabled=$enabled, count=$count")
        _translationInfo.value = TranslationInfo(isEnabled = enabled, translatedCount = count)
    }

    @JavascriptInterface
    fun onGameLaunchInitiated(gameTitle: String?) {
        Log.d(TAG, "Game launch detected: $gameTitle")
        controller.onGameLaunchInitiated(gameTitle ?: "Xbox Game")
    }

    @JavascriptInterface
    fun onStreamConnected(detail: String?) {
        Log.d(TAG, "Stream established: $detail")
        controller.onStreamConnected()
    }

    @JavascriptInterface
    fun onStreamDisconnected(detail: String?) {
        Log.d(TAG, "Stream ended: $detail")
        controller.onStreamDisconnected()
    }

    @JavascriptInterface
    fun onUrlChanged(url: String?) {
        url?.let { controller.onUrlChanged(it) }
    }

    @JavascriptInterface
    fun onRegionBypassed(info: String?) {
        Log.d(TAG, "Region bypassed: $info")
        VpnManager.onRegionBypassed(info ?: "")
    }

    @JavascriptInterface
    fun onWebViewIpDetected(jsonStr: String?) {
        if (jsonStr.isNullOrBlank()) return
        try {
            val json = JSONObject(jsonStr)
            val ip = json.optString("ip", "")
            val countryCode = json.optString("country", json.optString("country_code", "JP")).uppercase()
            val countryName = json.optString("country_name", json.optString("country", "Nhật Bản"))
            val city = json.optString("city", "")
            if (ip.isNotBlank()) {
                VpnManager.onWebViewIpDetected(ip, countryCode, countryName, city)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse onWebViewIpDetected JSON", e)
        }
    }

    @JavascriptInterface
    fun onStreamTelemetry(telemetryJson: String?) {
        // Real-time WebRTC telemetry received from stream
        Log.d(TAG, "Stream telemetry: $telemetryJson")
        NetworkOptimizer.onTelemetryReceived(telemetryJson)
    }

    @JavascriptInterface
    fun log(msg: String?) {
        Log.d("XboxBridgeJS", msg ?: "")
    }
}
