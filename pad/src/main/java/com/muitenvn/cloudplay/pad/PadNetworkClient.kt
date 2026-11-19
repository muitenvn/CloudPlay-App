package com.muitenvn.cloudplay.pad

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.muitenvn.cloudplay.protocol.AuthFailReason
import com.muitenvn.cloudplay.protocol.RemoteControlCodec
import com.muitenvn.cloudplay.protocol.RemoteControlFrame
import com.muitenvn.cloudplay.protocol.RemotePadCrypto
import com.muitenvn.cloudplay.protocol.RemotePadProtocol
import com.muitenvn.cloudplay.protocol.RemotePadState
import com.muitenvn.cloudplay.protocol.RemotePadUdpCodec
import com.muitenvn.cloudplay.protocol.TvServiceStatus
import com.muitenvn.cloudplay.protocol.VersionCheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PadScreenRoute {
    FIND_TV,
    ENTER_PIN,
    GAMEPAD,
    DISCONNECTED
}

data class DiscoveredTvDevice(
    val tvId: String,
    val tvName: String,
    val hostAddress: String,
    val tcpPort: Int,
    val udpPort: Int,
    val protocolVersion: Int,
    val status: TvServiceStatus,
    val hasRememberToken: Boolean = false
)

data class PadClientUiState(
    val route: PadScreenRoute = PadScreenRoute.FIND_TV,
    val language: PadLanguage = PadLanguage.SYSTEM,
    val clientDeviceId: String = "",
    val clientDeviceName: String = "",
    val localIpAddress: String = "",
    val isScanning: Boolean = false,
    val discoveredTvs: List<DiscoveredTvDevice> = emptyList(),
    val selectedTv: DiscoveredTvDevice? = null,
    val enteredPin: String = "",
    val isConnecting: Boolean = false,
    val pinErrorReason: AuthFailReason? = null,
    val remainingPinAttempts: Int = RemotePadProtocol.MAX_PIN_ATTEMPTS,
    val lockoutRemainingSec: Int = 0,
    val versionMismatchResult: VersionCheckResult = VersionCheckResult.COMPATIBLE,
    val connectedPlayerIndex: Int = 1,
    val measuredPingMs: Int? = null,
    val disconnectReasonCode: String = "",
    val activeGameKey: String = "",
    val activeGameTitle: String = "",
    val suggestedPresetId: String = "",
    val isAutoReconnecting: Boolean = false
)

/**
 * Bộ quản lý mạng nội bộ của CloudPlay Pad (:pad):
 * - Quét tìm TV qua NSD/mDNS (`_cloudplaypad._tcp`) + quét nhanh cổng TCP nội bộ khi cần.
 * - Ghép đôi mã 4 số bằng chữ ký HMAC-SHA256 (không bao giờ gửi mã 4 số dạng rõ).
 * - Lưu mã nhớ riêng theo từng TV để tự kết nối lại không cần nhập mã.
 * - Gửi TOÀN BỘ trạng thái tay cầm qua gói UDP 48 byte kèm mã phiên 16 byte + số thứ tự tăng dần
 *   mỗi khi có thay đổi và gửi nhịp tim mỗi 100ms khi giữ nguyên.
 * - Nhả toàn bộ nút ngay lập tức khi vào nền hoặc ngắt kết nối.
 * - Đo Ping thật qua gói PING/PONG và nhận lệnh rung từ TV.
 */
object PadNetworkClient {
    private const val PREFS_NAME = "cloudplay_pad_client_prefs"
    private const val KEY_DEVICE_ID = "client_device_id"
    private const val KEY_LANG_CODE = "pad_lang_code"
    private const val KEY_TOKENS_JSON = "remember_tokens_by_tv_json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(PadClientUiState())
    val state: StateFlow<PadClientUiState> = _state.asStateFlow()

    private val discoveredMap = ConcurrentHashMap<String, DiscoveredTvDevice>()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var nsdManager: NsdManager? = null

    @Volatile
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    @Volatile
    private var multicastLock: WifiManager.MulticastLock? = null

    @Volatile
    private var subnetScanJob: Job? = null

    @Volatile
    private var lockoutCountdownJob: Job? = null

    @Volatile
    private var autoReconnectJob: Job? = null

    @Volatile
    private var lockoutUntilElapsedMs: Long = 0L

    private val activeControlSocketRef = AtomicReference<Socket?>(null)
    private val activeControlWriterRef = AtomicReference<BufferedWriter?>(null)
    private val activeUdpSocketRef = AtomicReference<DatagramSocket?>(null)

    @Volatile
    private var activeUdpSessionToken: ByteArray? = null

    @Volatile
    private var activeUdpTargetAddress: InetAddress? = null

    @Volatile
    private var activeUdpTargetPort: Int = RemotePadProtocol.DEFAULT_UDP_INPUT_PORT

    private val udpSequenceCounter = AtomicLong(0L)
    private val currentButtonsBitmask = AtomicInteger(0)

    @Volatile
    private var currentLx: Float = 0f

    @Volatile
    private var currentLy: Float = 0f

    @Volatile
    private var currentRx: Float = 0f

    @Volatile
    private var currentRy: Float = 0f

    @Volatile
    private var currentLt: Float = 0f

    @Volatile
    private var currentRt: Float = 0f

    @Volatile
    private var gyroActive: Boolean = false

    @Volatile
    private var gyroRightStick: Boolean = true

    @Volatile
    private var gyroX: Float = 0f

    @Volatile
    private var gyroY: Float = 0f

    @Volatile
    private var gyroHoldLtOnly: Boolean = false

    private val udpSendLock = Any()
    private val reusableUdpBytes = ByteArray(RemotePadProtocol.UDP_PACKET_BYTES)

    fun ensureInitialized(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var devId = prefs.getString(KEY_DEVICE_ID, null).orEmpty()
        if (devId.isBlank()) {
            devId = UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_DEVICE_ID, devId).apply()
        }
        val savedLangCode = prefs.getString(KEY_LANG_CODE, PadLanguage.SYSTEM.code).orEmpty()
        val lang = PadLanguage.entries.firstOrNull { it.code == savedLangCode } ?: PadLanguage.SYSTEM
        val model = Build.MODEL?.trim()?.takeIf { it.isNotEmpty() } ?: "Android Phone"
        val ip = resolveLocalLanIpv4()

        _state.update { cur ->
            cur.copy(
                language = lang,
                clientDeviceId = devId,
                clientDeviceName = model,
                localIpAddress = ip
            )
        }
    }

    fun cycleLanguage(context: Context) {
        ensureInitialized(context)
        val all = PadLanguage.entries
        val curIdx = all.indexOf(_state.value.language).coerceAtLeast(0)
        val next = all[(curIdx + 1) % all.size]
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG_CODE, next.code)
            .apply()
        _state.update { it.copy(language = next) }
    }

    fun updateButtonBit(bitMask: Int, pressed: Boolean) {
        if (bitMask == 0) return
        while (true) {
            val prev = currentButtonsBitmask.get()
            val next = if (pressed) (prev or bitMask) else (prev and bitMask.inv())
            if (prev == next) return
            if (currentButtonsBitmask.compareAndSet(prev, next)) {
                dispatchCurrentPadStateOverUdp()
                return
            }
        }
    }

    fun updateButtonsMask(maskToClear: Int, maskToSet: Int) {
        while (true) {
            val prev = currentButtonsBitmask.get()
            val next = (prev and maskToClear.inv()) or maskToSet
            if (prev == next) return
            if (currentButtonsBitmask.compareAndSet(prev, next)) {
                dispatchCurrentPadStateOverUdp()
                return
            }
        }
    }

    fun updateLeftStick(x: Float, y: Float, sprintL3Bit: Int = 0) {
        val cx = x.coerceIn(-1f, 1f)
        val cy = y.coerceIn(-1f, 1f)
        val axisChanged = abs(currentLx - cx) >= 0.004f || abs(currentLy - cy) >= 0.004f
        currentLx = cx
        currentLy = cy
        var maskChanged = false
        if (sprintL3Bit != 0) {
            val prev = currentButtonsBitmask.get()
            val next = prev or sprintL3Bit
            if (prev != next && currentButtonsBitmask.compareAndSet(prev, next)) {
                maskChanged = true
            }
        }
        if (axisChanged || maskChanged) {
            dispatchCurrentPadStateOverUdp()
        }
    }

    fun updateRightStick(x: Float, y: Float) {
        val cx = x.coerceIn(-1f, 1f)
        val cy = y.coerceIn(-1f, 1f)
        if (abs(currentRx - cx) < 0.004f && abs(currentRy - cy) < 0.004f) return
        currentRx = cx
        currentRy = cy
        dispatchCurrentPadStateOverUdp()
    }

    fun updateLeftTrigger(value: Float) {
        val cv = value.coerceIn(0f, 1f)
        if (abs(currentLt - cv) < 0.005f) return
        currentLt = cv
        dispatchCurrentPadStateOverUdp()
    }

    fun updateRightTrigger(value: Float) {
        val cv = value.coerceIn(0f, 1f)
        if (abs(currentRt - cv) < 0.005f) return
        currentRt = cv
        dispatchCurrentPadStateOverUdp()
    }

    fun updateGyroOffset(rightStick: Boolean, gx: Float, gy: Float, holdLtOnly: Boolean) {
        val cx = gx.coerceIn(-1f, 1f)
        val cy = gy.coerceIn(-1f, 1f)
        val changed = !gyroActive || gyroRightStick != rightStick || gyroHoldLtOnly != holdLtOnly ||
            abs(gyroX - cx) >= 0.006f || abs(gyroY - cy) >= 0.006f
        gyroActive = true
        gyroRightStick = rightStick
        gyroX = cx
        gyroY = cy
        gyroHoldLtOnly = holdLtOnly
        if (changed) {
            dispatchCurrentPadStateOverUdp()
        }
    }

    fun clearGyroOffset() {
        val wasActive = gyroActive && (abs(gyroX) > 0.001f || abs(gyroY) > 0.001f)
        gyroActive = false
        gyroX = 0f
        gyroY = 0f
        if (wasActive) {
            dispatchCurrentPadStateOverUdp()
        }
    }

    /**
     * An toàn nút bấm (Giao thức v1 mục 6):
     * Khi vào nền, mất kết nối hoặc thoát màn tay cầm -> đưa toàn bộ nút/cần/cò về 0 và bắn gói UDP NEUTRAL ngay.
     */
    fun releaseAllInputsImmediate() {
        currentButtonsBitmask.set(0)
        currentLx = 0f
        currentLy = 0f
        currentRx = 0f
        currentRy = 0f
        currentLt = 0f
        currentRt = 0f
        gyroActive = false
        gyroX = 0f
        gyroY = 0f
        scope.launch {
            repeat(3) {
                sendUdpSnapshotNow()
                if (it < 2) delay(12L)
            }
        }
    }

    private fun dispatchCurrentPadStateOverUdp() {
        scope.launch {
            sendUdpSnapshotNow()
        }
    }

    private fun sendUdpSnapshotNow() {
        val token = activeUdpSessionToken ?: return
        val dSocket = activeUdpSocketRef.get() ?: return
        val addr = activeUdpTargetAddress ?: return
        val port = activeUdpTargetPort
        if (dSocket.isClosed || token.size != RemotePadProtocol.SESSION_TOKEN_BYTES) return

        val rawBtns = currentButtonsBitmask.get()
        val btns = rawBtns and 0xFFFF
        val synthLt = if ((rawBtns and PAD_SYNTH_BIT_LT) != 0) 1f else 0f
        val synthRt = if ((rawBtns and PAD_SYNTH_BIT_RT) != 0) 1f else 0f
        val effLt = maxOf(currentLt, synthLt)
        val effRt = maxOf(currentRt, synthRt)
        val applyGyroNow = gyroActive && (!gyroHoldLtOnly || effLt > 0.15f)
        val effLx = if (applyGyroNow && !gyroRightStick) (currentLx + gyroX).coerceIn(-1f, 1f) else currentLx
        val effLy = if (applyGyroNow && !gyroRightStick) (currentLy + gyroY).coerceIn(-1f, 1f) else currentLy
        val effRx = if (applyGyroNow && gyroRightStick) (currentRx + gyroX).coerceIn(-1f, 1f) else currentRx
        val effRy = if (applyGyroNow && gyroRightStick) (currentRy + gyroY).coerceIn(-1f, 1f) else currentRy

        val seq = udpSequenceCounter.incrementAndGet()
        val stateSnapshot = RemotePadState(
            sequenceNumber = seq,
            playerIndex = _state.value.connectedPlayerIndex.coerceIn(1, 4),
            buttonsBitmask = btns,
            lx = effLx,
            ly = effLy,
            rx = effRx,
            ry = effRy,
            lt = effLt,
            rt = effRt
        )

        synchronized(udpSendLock) {
            try {
                RemotePadUdpCodec.encode(
                    state = stateSnapshot,
                    sessionToken = token,
                    outBuffer = reusableUdpBytes
                )
                val packet = DatagramPacket(
                    reusableUdpBytes,
                    RemotePadProtocol.UDP_PACKET_BYTES,
                    addr,
                    port
                )
                dSocket.send(packet)
            } catch (_: Exception) {
            }
        }
    }

    fun startDiscovery(context: Context) {
        ensureInitialized(context)
        stopDiscovery()
        acquireMulticastLock(context.applicationContext)

        _state.update {
            it.copy(
                isScanning = true,
                localIpAddress = resolveLocalLanIpv4()
            )
        }

        try {
            val manager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
            nsdManager = manager
            if (manager != null) {
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(regType: String) {
                        _state.update { it.copy(isScanning = true) }
                    }

                    override fun onServiceFound(service: NsdServiceInfo) {
                        val type = service.serviceType.orEmpty()
                        if (type.contains("_cloudplaypad._tcp", ignoreCase = true)) {
                            resolveDiscoveredService(manager, service)
                        }
                    }

                    override fun onServiceLost(service: NsdServiceInfo) {
                        val lostName = service.serviceName.orEmpty()
                        val toRemove = discoveredMap.values.filter { it.tvName == lostName }.map { it.tvId }
                        toRemove.forEach { discoveredMap.remove(it) }
                        publishDiscoveredList()
                    }

                    override fun onDiscoveryStopped(serviceType: String) {
                        _state.update { it.copy(isScanning = false) }
                    }

                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        _state.update { it.copy(isScanning = false) }
                    }

                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    }
                }
                discoveryListener = listener
                manager.discoverServices(
                    RemotePadProtocol.NSD_SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    listener
                )
            }
        } catch (_: Exception) {
            _state.update { it.copy(isScanning = false) }
        }

        triggerSubnetQuickProbe()
    }

    fun rescan(context: Context) {
        discoveredMap.clear()
        publishDiscoveredList()
        startDiscovery(context)
    }

    fun stopDiscovery() {
        val manager = nsdManager
        val listener = discoveryListener
        discoveryListener = null
        if (manager != null && listener != null) {
            try {
                manager.stopServiceDiscovery(listener)
            } catch (_: Exception) {
            }
        }
        subnetScanJob?.cancel()
        _state.update { it.copy(isScanning = false) }
    }

    private fun resolveDiscoveredService(manager: NsdManager, service: NsdServiceInfo) {
        try {
            @Suppress("DEPRECATION")
            manager.resolveService(
                service,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    }

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        val host = resolved.host?.hostAddress ?: return
                        val tcpPort = resolved.port.takeIf { it > 0 } ?: RemotePadProtocol.DEFAULT_TCP_CONTROL_PORT
                        val attrs = resolved.attributes ?: emptyMap()
                        val verStr = attrs[RemotePadProtocol.TXT_KEY_VERSION]?.toString(StandardCharsets.UTF_8)
                        val stStr = attrs[RemotePadProtocol.TXT_KEY_STATUS]?.toString(StandardCharsets.UTF_8)
                        val upStr = attrs[RemotePadProtocol.TXT_KEY_UDP_PORT]?.toString(StandardCharsets.UTF_8)
                        val idStr = attrs[RemotePadProtocol.TXT_KEY_TV_ID]?.toString(StandardCharsets.UTF_8)

                        val ver = verStr?.trim()?.toIntOrNull() ?: RemotePadProtocol.PROTOCOL_VERSION
                        val udpPort = upStr?.trim()?.toIntOrNull() ?: RemotePadProtocol.DEFAULT_UDP_INPUT_PORT
                        val status = TvServiceStatus.fromWireCode(stStr)
                        val tvId = idStr?.trim()?.ifBlank { "$host:$tcpPort" } ?: "$host:$tcpPort"
                        val tvName = resolved.serviceName?.trim()?.ifBlank { "Ravno TV ($host)" } ?: "Ravno TV ($host)"
                        val hasToken = getRememberToken(tvId).isNotBlank()

                        discoveredMap[tvId] = DiscoveredTvDevice(
                            tvId = tvId,
                            tvName = tvName,
                            hostAddress = host,
                            tcpPort = tcpPort,
                            udpPort = udpPort,
                            protocolVersion = ver,
                            status = status,
                            hasRememberToken = hasToken
                        )
                        publishDiscoveredList()
                    }
                }
            )
        } catch (_: Exception) {
        }
    }

    private fun triggerSubnetQuickProbe() {
        subnetScanJob?.cancel()
        subnetScanJob = scope.launch {
            val myIp = resolveLocalLanIpv4()
            val dotIdx = myIp.lastIndexOf('.')
            if (dotIdx <= 0) return@launch
            val prefix = myIp.substring(0, dotIdx + 1)
            val myLastOctet = myIp.substring(dotIdx + 1).toIntOrNull() ?: -1

            val octets = (1..254).filter { it != myLastOctet }
            for (batch in octets.chunked(32)) {
                if (!isActive) break
                val jobs = batch.map { octet ->
                    launch {
                        probeHostForTv("$prefix$octet", RemotePadProtocol.DEFAULT_TCP_CONTROL_PORT)
                    }
                }
                jobs.forEach { it.join() }
            }
        }
    }

    fun probeManualIp(hostIp: String) {
        val cleanIp = hostIp.trim()
        if (cleanIp.isEmpty()) return
        scope.launch {
            probeHostForTv(cleanIp, RemotePadProtocol.DEFAULT_TCP_CONTROL_PORT, timeoutMs = 1500)
        }
    }

    private fun probeHostForTv(host: String, tcpPort: Int, timeoutMs: Int = 260) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, tcpPort), timeoutMs)
            socket.soTimeout = 800
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val firstLine = reader.readLine() ?: return
            val hello = RemoteControlCodec.parse(firstLine) as? RemoteControlFrame.Hello ?: return
            val tvId = hello.tvId.ifBlank { "$host:$tcpPort" }
            val hasToken = getRememberToken(tvId).isNotBlank()
            discoveredMap[tvId] = DiscoveredTvDevice(
                tvId = tvId,
                tvName = hello.tvName.ifBlank { "Ravno TV ($host)" },
                hostAddress = host,
                tcpPort = tcpPort,
                udpPort = hello.udpPort,
                protocolVersion = hello.protocolVersion,
                status = hello.status,
                hasRememberToken = hasToken
            )
            publishDiscoveredList()
        } catch (_: Exception) {
        } finally {
            runCatching { socket?.close() }
        }
    }

    private fun publishDiscoveredList() {
        val sorted = discoveredMap.values.sortedWith(
            compareByDescending<DiscoveredTvDevice> { it.hasRememberToken }
                .thenBy { it.status != TvServiceStatus.WAITING_FOR_PAIR }
                .thenBy { it.tvName }
        )
        _state.update { cur ->
            val updatedSelected = cur.selectedTv?.let { sel ->
                sorted.firstOrNull { it.tvId == sel.tvId } ?: sel
            }
            cur.copy(
                discoveredTvs = sorted,
                selectedTv = updatedSelected
            )
        }
    }

    fun onSelectTv(tv: DiscoveredTvDevice) {
        val verCheck = RemotePadProtocol.checkVersionCompatibility(
            localVersion = RemotePadProtocol.PROTOCOL_VERSION,
            remoteVersion = tv.protocolVersion,
            isLocalTv = false
        )
        if (verCheck != VersionCheckResult.COMPATIBLE) {
            _state.update {
                it.copy(
                    selectedTv = tv,
                    versionMismatchResult = verCheck,
                    pinErrorReason = AuthFailReason.VERSION_MISMATCH
                )
            }
            return
        }

        val savedToken = getRememberToken(tv.tvId)
        if (savedToken.isNotBlank()) {
            _state.update {
                it.copy(
                    selectedTv = tv,
                    enteredPin = "",
                    pinErrorReason = null,
                    versionMismatchResult = VersionCheckResult.COMPATIBLE
                )
            }
            connectWithRememberToken(tv, savedToken)
        } else {
            _state.update {
                it.copy(
                    route = PadScreenRoute.ENTER_PIN,
                    selectedTv = tv,
                    enteredPin = "",
                    pinErrorReason = null,
                    remainingPinAttempts = RemotePadProtocol.MAX_PIN_ATTEMPTS,
                    versionMismatchResult = VersionCheckResult.COMPATIBLE
                )
            }
            notifyTvShowPairPinPrompt(tv)
        }
    }

    private fun notifyTvShowPairPinPrompt(tv: DiscoveredTvDevice) {
        scope.launch {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(tv.hostAddress, tv.tcpPort), 1_500)
                socket.soTimeout = 1_500
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                reader.readLine()
                writer.write("PAIR_PROMPT\n")
                writer.flush()
            } catch (_: Exception) {
            } finally {
                runCatching { socket?.close() }
            }
        }
    }

    fun backToTvList() {
        disconnectActiveSession(navigateToList = true)
    }

    fun appendPinDigit(digit: Char) {
        if (!digit.isDigit()) return
        val cur = _state.value
        if (cur.isConnecting || cur.lockoutRemainingSec > 0) return
        if (cur.enteredPin.length < 4) {
            val nextPin = cur.enteredPin + digit
            _state.update {
                it.copy(
                    enteredPin = nextPin,
                    pinErrorReason = null
                )
            }
        }
    }

    fun backspacePinDigit() {
        val cur = _state.value
        if (cur.isConnecting) return
        if (cur.enteredPin.isNotEmpty()) {
            _state.update {
                it.copy(
                    enteredPin = cur.enteredPin.dropLast(1),
                    pinErrorReason = null
                )
            }
        }
    }

    fun submitFourDigitPin() {
        val cur = _state.value
        val tv = cur.selectedTv ?: return
        val pin = cur.enteredPin.trim()
        if (pin.length != 4 || cur.isConnecting || cur.lockoutRemainingSec > 0) return

        _state.update {
            it.copy(
                isConnecting = true,
                pinErrorReason = null
            )
        }

        scope.launch {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(tv.hostAddress, tv.tcpPort), 4_000)
                socket.soTimeout = 8_000

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

                val helloLine = reader.readLine() ?: throw IllegalStateException("No HELLO")
                val hello = RemoteControlCodec.parse(helloLine) as? RemoteControlFrame.Hello
                    ?: throw IllegalStateException("Invalid HELLO")

                val verCheck = RemotePadProtocol.checkVersionCompatibility(
                    localVersion = RemotePadProtocol.PROTOCOL_VERSION,
                    remoteVersion = hello.protocolVersion,
                    isLocalTv = false
                )
                if (verCheck != VersionCheckResult.COMPATIBLE) {
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            versionMismatchResult = verCheck,
                            pinErrorReason = AuthFailReason.VERSION_MISMATCH
                        )
                    }
                    runCatching { socket.close() }
                    return@launch
                }

                if (hello.lockoutRemainingSec > 0) {
                    applyLockoutCountdown(hello.lockoutRemainingSec)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            enteredPin = "",
                            pinErrorReason = AuthFailReason.LOCKED_OUT
                        )
                    }
                    runCatching { socket.close() }
                    return@launch
                }

                val pinHmac = RemotePadCrypto.computePinProofHmac(
                    pin4Digits = pin,
                    tvNonceHex = hello.nonceHex,
                    clientDeviceId = cur.clientDeviceId
                )

                sendFrame(
                    writer,
                    RemoteControlFrame.PairWithPin(
                        protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                        deviceId = cur.clientDeviceId,
                        deviceName = cur.clientDeviceName,
                        pinHmacHex = pinHmac
                    )
                )

                val replyLine = reader.readLine() ?: throw IllegalStateException("No auth response")
                when (val reply = RemoteControlCodec.parse(replyLine)) {
                    is RemoteControlFrame.AuthOk -> {
                        val tvKey = hello.tvId.ifBlank { tv.tvId }
                        saveRememberToken(tvKey, reply.rememberTokenHex)
                        discoveredMap[tvKey] = tv.copy(tvId = tvKey, hasRememberToken = true)
                        publishDiscoveredList()

                        runConnectedControlLoop(
                            socket = socket,
                            reader = reader,
                            writer = writer,
                            tv = tv.copy(tvId = tvKey, tvName = hello.tvName.ifBlank { tv.tvName }),
                            authOk = reply
                        )
                    }
                    is RemoteControlFrame.AuthFail -> {
                        handleAuthFail(reply)
                        runCatching { socket.close() }
                    }
                    else -> {
                        _state.update { it.copy(isConnecting = false, pinErrorReason = AuthFailReason.WRONG_PIN) }
                        runCatching { socket.close() }
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isConnecting = false) }
                runCatching { socket?.close() }
            }
        }
    }

    fun reconnectSelectedTv() {
        autoReconnectJob?.cancel()
        _state.update { it.copy(isAutoReconnecting = false) }
        val tv = _state.value.selectedTv ?: return
        onSelectTv(tv)
    }

    private fun connectWithRememberToken(
        tv: DiscoveredTvDevice,
        rememberTokenHex: String,
        fromAutoReconnect: Boolean = false
    ) {
        val cur = _state.value
        if (cur.isConnecting) return
        _state.update {
            it.copy(
                isConnecting = true,
                isAutoReconnecting = fromAutoReconnect,
                pinErrorReason = null
            )
        }

        scope.launch {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(tv.hostAddress, tv.tcpPort), 3_500)
                socket.soTimeout = 6_000

                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

                val helloLine = reader.readLine() ?: throw IllegalStateException("No HELLO")
                val hello = RemoteControlCodec.parse(helloLine) as? RemoteControlFrame.Hello
                    ?: throw IllegalStateException("Invalid HELLO")

                val verCheck = RemotePadProtocol.checkVersionCompatibility(
                    localVersion = RemotePadProtocol.PROTOCOL_VERSION,
                    remoteVersion = hello.protocolVersion,
                    isLocalTv = false
                )
                if (verCheck != VersionCheckResult.COMPATIBLE) {
                    autoReconnectJob?.cancel()
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isAutoReconnecting = false,
                            versionMismatchResult = verCheck,
                            pinErrorReason = AuthFailReason.VERSION_MISMATCH
                        )
                    }
                    runCatching { socket.close() }
                    return@launch
                }

                val tokenHmac = RemotePadCrypto.computeTokenProofHmac(
                    rememberTokenHex = rememberTokenHex,
                    tvNonceHex = hello.nonceHex,
                    clientDeviceId = cur.clientDeviceId
                )

                sendFrame(
                    writer,
                    RemoteControlFrame.AuthWithToken(
                        protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                        deviceId = cur.clientDeviceId,
                        deviceName = cur.clientDeviceName,
                        tokenHmacHex = tokenHmac
                    )
                )

                val replyLine = reader.readLine() ?: throw IllegalStateException("No auth response")
                when (val reply = RemoteControlCodec.parse(replyLine)) {
                    is RemoteControlFrame.AuthOk -> {
                        autoReconnectJob?.cancel()
                        runConnectedControlLoop(
                            socket = socket,
                            reader = reader,
                            writer = writer,
                            tv = tv.copy(tvName = hello.tvName.ifBlank { tv.tvName }),
                            authOk = reply
                        )
                    }
                    is RemoteControlFrame.AuthFail -> {
                        autoReconnectJob?.cancel()
                        if (reply.reason == AuthFailReason.TOKEN_REVOKED) {
                            removeRememberToken(tv.tvId)
                            discoveredMap[tv.tvId] = tv.copy(hasRememberToken = false)
                            publishDiscoveredList()
                            _state.update {
                                it.copy(
                                    isConnecting = false,
                                    isAutoReconnecting = false,
                                    route = PadScreenRoute.ENTER_PIN,
                                    enteredPin = "",
                                    pinErrorReason = AuthFailReason.TOKEN_REVOKED
                                )
                            }
                        } else {
                            handleAuthFail(reply)
                        }
                        runCatching { socket.close() }
                    }
                    else -> {
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isAutoReconnecting = false,
                                route = if (fromAutoReconnect) PadScreenRoute.DISCONNECTED else PadScreenRoute.ENTER_PIN,
                                enteredPin = ""
                            )
                        }
                        runCatching { socket.close() }
                    }
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        route = if (fromAutoReconnect || it.route == PadScreenRoute.DISCONNECTED) {
                            PadScreenRoute.DISCONNECTED
                        } else {
                            PadScreenRoute.ENTER_PIN
                        },
                        enteredPin = ""
                    )
                }
                runCatching { socket?.close() }
            }
        }
    }

    private fun handleAuthFail(fail: RemoteControlFrame.AuthFail) {
        when (fail.reason) {
            AuthFailReason.LOCKED_OUT -> {
                applyLockoutCountdown(fail.lockoutRemainingSec.coerceAtLeast(30))
                _state.update {
                    it.copy(
                        isConnecting = false,
                        enteredPin = "",
                        pinErrorReason = AuthFailReason.LOCKED_OUT,
                        remainingPinAttempts = 0
                    )
                }
            }
            AuthFailReason.WRONG_PIN -> {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        enteredPin = "",
                        pinErrorReason = AuthFailReason.WRONG_PIN,
                        remainingPinAttempts = fail.remainingAttempts.coerceAtLeast(1)
                    )
                }
            }
            AuthFailReason.VERSION_MISMATCH -> {
                val verCheck = RemotePadProtocol.checkVersionCompatibility(
                    localVersion = RemotePadProtocol.PROTOCOL_VERSION,
                    remoteVersion = fail.protocolVersion,
                    isLocalTv = false
                )
                _state.update {
                    it.copy(
                        isConnecting = false,
                        enteredPin = "",
                        pinErrorReason = AuthFailReason.VERSION_MISMATCH,
                        versionMismatchResult = verCheck
                    )
                }
            }
            else -> {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        enteredPin = "",
                        pinErrorReason = fail.reason
                    )
                }
            }
        }
    }

    private suspend fun runConnectedControlLoop(
        socket: Socket,
        reader: BufferedReader,
        writer: BufferedWriter,
        tv: DiscoveredTvDevice,
        authOk: RemoteControlFrame.AuthOk
    ) {
        activeControlSocketRef.getAndSet(socket)?.let { old ->
            if (old !== socket) runCatching { old.close() }
        }
        activeControlWriterRef.set(writer)

        val dSocket = runCatching { DatagramSocket() }.getOrNull()
        activeUdpSocketRef.getAndSet(dSocket)?.close()
        activeUdpSessionToken = RemotePadCrypto.hexToBytes(authOk.udpSessionTokenHex)
        activeUdpTargetAddress = runCatching { InetAddress.getByName(tv.hostAddress) }.getOrNull()
        activeUdpTargetPort = authOk.udpPort
        udpSequenceCounter.set(0L)
        currentButtonsBitmask.set(0)
        currentLx = 0f
        currentLy = 0f
        currentRx = 0f
        currentRy = 0f
        currentLt = 0f
        currentRt = 0f

        stopDiscovery()
        autoReconnectJob?.cancel()

        _state.update {
            it.copy(
                isConnecting = false,
                isAutoReconnecting = false,
                route = PadScreenRoute.GAMEPAD,
                selectedTv = tv,
                enteredPin = "",
                pinErrorReason = null,
                connectedPlayerIndex = authOk.playerIndex,
                measuredPingMs = null,
                disconnectReasonCode = "",
                activeGameKey = authOk.activeGameKey,
                activeGameTitle = authOk.activeGameTitle
            )
        }

        // 1. Luồng nhịp tim UDP mỗi 100ms để TV biết tay cầm còn sống (Giao thức v1 mục 2)
        val udpHeartbeatJob = scope.launch {
            while (isActive && !socket.isClosed) {
                sendUdpSnapshotNow()
                delay(RemotePadProtocol.UDP_HEARTBEAT_INTERVAL_MS)
            }
        }

        // 2. Luồng PING TCP mỗi 2 giây để đo Ping thực tế tới TV
        val pingJob = scope.launch {
            while (isActive && !socket.isClosed) {
                sendFrame(writer, RemoteControlFrame.Ping(SystemClock.elapsedRealtime()))
                delay(2_000L)
            }
        }

        socket.soTimeout = 12_000
        var disconnectReason = "CONNECTION_LOST"
        try {
            while (!socket.isClosed) {
                val line = reader.readLine() ?: break
                when (val frame = RemoteControlCodec.parse(line)) {
                    is RemoteControlFrame.Pong -> {
                        val rtt = (SystemClock.elapsedRealtime() - frame.clientSentRealtimeMs)
                            .coerceIn(1L, 9_999L)
                            .toInt()
                        _state.update { cur ->
                            cur.copy(
                                measuredPingMs = rtt,
                                activeGameKey = frame.activeGameKey.ifBlank { cur.activeGameKey }
                            )
                        }
                    }
                    is RemoteControlFrame.Rumble -> {
                        triggerPhoneVibration(frame.durationMs, frame.strongMagnitude, frame.weakMagnitude)
                    }
                    is RemoteControlFrame.LayoutSync -> {
                        _state.update { cur ->
                            cur.copy(
                                suggestedPresetId = frame.presetName.trim().lowercase(),
                                activeGameKey = frame.gameKey.ifBlank { cur.activeGameKey },
                                activeGameTitle = frame.gameTitle.ifBlank { cur.activeGameTitle }
                            )
                        }
                    }
                    is RemoteControlFrame.Disconnect -> {
                        disconnectReason = frame.reason
                        if (frame.reason == "FORGOTTEN") {
                            removeRememberToken(tv.tvId)
                        }
                        break
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) {
        } finally {
            udpHeartbeatJob.cancel()
            pingJob.cancel()
            releaseAllInputsImmediate()
            if (activeControlSocketRef.compareAndSet(socket, null)) {
                activeControlWriterRef.set(null)
                activeUdpSessionToken = null
                activeUdpSocketRef.getAndSet(null)?.close()
                var transitionedToDisconnected = false
                _state.update { cur ->
                    if (cur.route == PadScreenRoute.GAMEPAD) {
                        transitionedToDisconnected = true
                        cur.copy(
                            route = PadScreenRoute.DISCONNECTED,
                            measuredPingMs = null,
                            disconnectReasonCode = disconnectReason
                        )
                    } else {
                        cur.copy(measuredPingMs = null)
                    }
                }
                if (transitionedToDisconnected && disconnectReason == "CONNECTION_LOST") {
                    startAutoReconnectIfEligible(tv)
                }
            }
            runCatching { socket.close() }
        }
    }

    private fun startAutoReconnectIfEligible(tv: DiscoveredTvDevice) {
        val savedToken = getRememberToken(tv.tvId)
        if (savedToken.isBlank()) return
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch {
            _state.update { it.copy(isAutoReconnecting = true) }
            try {
                for (attempt in 1..6) {
                    delay(2_200L)
                    val cur = _state.value
                    if (cur.route != PadScreenRoute.DISCONNECTED) break
                    if (!cur.isConnecting) {
                        connectWithRememberToken(tv, savedToken, fromAutoReconnect = true)
                    }
                }
            } finally {
                _state.update { it.copy(isAutoReconnecting = false) }
            }
        }
    }

    @Volatile
    var gameRumbleScale: Float = 1.0f

    fun disconnectActiveSession(navigateToList: Boolean = true) {
        autoReconnectJob?.cancel()
        releaseAllInputsImmediate()
        val writer = activeControlWriterRef.getAndSet(null)
        val socket = activeControlSocketRef.getAndSet(null)
        scope.launch {
            delay(35L)
            activeUdpSessionToken = null
            activeUdpSocketRef.getAndSet(null)?.close()
            if (writer != null) {
                sendFrame(writer, RemoteControlFrame.Disconnect("PHONE_DISCONNECT"))
            }
            runCatching { socket?.close() }
        }
        _state.update { cur ->
            cur.copy(
                route = if (navigateToList) PadScreenRoute.FIND_TV else PadScreenRoute.DISCONNECTED,
                isConnecting = false,
                isAutoReconnecting = false,
                enteredPin = "",
                measuredPingMs = null
            )
        }
        val ctx = appContext
        if (navigateToList && ctx != null) {
            startDiscovery(ctx)
        }
    }

    fun forgetActiveTvAndDisconnect() {
        val curTv = _state.value.selectedTv
        if (curTv != null) {
            removeRememberToken(curTv.tvId)
            discoveredMap[curTv.tvId] = curTv.copy(hasRememberToken = false)
            publishDiscoveredList()
        }
        autoReconnectJob?.cancel()
        releaseAllInputsImmediate()
        val writer = activeControlWriterRef.getAndSet(null)
        val socket = activeControlSocketRef.getAndSet(null)
        scope.launch {
            delay(35L)
            activeUdpSessionToken = null
            activeUdpSocketRef.getAndSet(null)?.close()
            if (writer != null) {
                sendFrame(writer, RemoteControlFrame.Disconnect("FORGOTTEN"))
            }
            runCatching { socket?.close() }
        }
        _state.update { cur ->
            cur.copy(
                route = PadScreenRoute.FIND_TV,
                isConnecting = false,
                isAutoReconnecting = false,
                enteredPin = "",
                measuredPingMs = null
            )
        }
        val ctx = appContext
        if (ctx != null) {
            startDiscovery(ctx)
        }
    }

    fun triggerPhoneVibration(durationMs: Int, strongMag: Float, weakMag: Float) {
        val ctx = appContext ?: return
        val scale = gameRumbleScale.coerceIn(0f, 1.5f)
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return

            if (scale <= 0.02f || durationMs <= 0 || (strongMag <= 0.01f && weakMag <= 0.01f)) {
                vibrator.cancel()
                return
            }

            val dur = durationMs.coerceIn(12, 1500).toLong()
            val combined = ((strongMag * 0.7f + weakMag * 0.3f) * scale).coerceIn(0.08f, 1f)
            val amp = (combined * 255f).roundToInt().coerceIn(1, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(dur)
            }
        } catch (_: Exception) {
        }
    }

    private fun applyLockoutCountdown(seconds: Int) {
        lockoutUntilElapsedMs = SystemClock.elapsedRealtime() + seconds.coerceAtLeast(1) * 1_000L
        _state.update { it.copy(lockoutRemainingSec = seconds.coerceAtLeast(1)) }
        if (lockoutCountdownJob?.isActive == true) return
        lockoutCountdownJob = scope.launch {
            while (isActive) {
                val remMs = lockoutUntilElapsedMs - SystemClock.elapsedRealtime()
                if (remMs <= 0L) {
                    lockoutUntilElapsedMs = 0L
                    _state.update {
                        it.copy(
                            lockoutRemainingSec = 0,
                            pinErrorReason = if (it.pinErrorReason == AuthFailReason.LOCKED_OUT) null else it.pinErrorReason,
                            remainingPinAttempts = RemotePadProtocol.MAX_PIN_ATTEMPTS
                        )
                    }
                    break
                }
                val sec = ((remMs + 999L) / 1000L).toInt()
                _state.update { it.copy(lockoutRemainingSec = sec) }
                delay(1_000L)
            }
        }
    }

    @Synchronized
    private fun sendFrame(writer: BufferedWriter, frame: RemoteControlFrame) {
        try {
            writer.write(RemoteControlCodec.serialize(frame))
            writer.write("\n")
            writer.flush()
        } catch (_: Exception) {
        }
    }

    private fun acquireMulticastLock(context: Context) {
        try {
            if (multicastLock?.isHeld == true) return
            val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            multicastLock = wifi.createMulticastLock("CloudPlayPadClientNsdLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun getRememberToken(tvId: String): String {
        val ctx = appContext ?: return ""
        return try {
            val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TOKENS_JSON, "{}")
                .orEmpty()
            JSONObject(raw).optString(tvId, "")
        } catch (_: Exception) {
            ""
        }
    }

    private fun saveRememberToken(tvId: String, tokenHex: String) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val obj = JSONObject(prefs.getString(KEY_TOKENS_JSON, "{}").orEmpty())
            obj.put(tvId, tokenHex)
            prefs.edit().putString(KEY_TOKENS_JSON, obj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun removeRememberToken(tvId: String) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val obj = JSONObject(prefs.getString(KEY_TOKENS_JSON, "{}").orEmpty())
            obj.remove(tvId)
            prefs.edit().putString(KEY_TOKENS_JSON, obj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun resolveLocalLanIpv4(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                val name = nif.name.lowercase()
                if (name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("dummy")) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress.orEmpty()
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }
}
