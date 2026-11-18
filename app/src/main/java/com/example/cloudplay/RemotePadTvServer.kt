package com.example.cloudplay

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.muitenvn.cloudplay.protocol.AuthFailReason
import com.muitenvn.cloudplay.protocol.RemoteControlCodec
import com.muitenvn.cloudplay.protocol.RemoteControlFrame
import com.muitenvn.cloudplay.protocol.RemotePadButtons
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

data class PairedPhoneDevice(
    val deviceId: String,
    val deviceName: String,
    val rememberTokenHex: String,
    val pairedAtEpochMs: Long
)

data class TvRemotePadServerState(
    val isRunning: Boolean = false,
    val isNsdRegistered: Boolean = false,
    val tvId: String = "",
    val tvDisplayName: String = "",
    val localIpAddress: String = "",
    val tcpPort: Int = RemotePadProtocol.DEFAULT_TCP_CONTROL_PORT,
    val udpPort: Int = RemotePadProtocol.DEFAULT_UDP_INPUT_PORT,
    val currentPin: String = "0000",
    val failedPinAttempts: Int = 0,
    val lockoutRemainingSec: Int = 0,
    val status: TvServiceStatus = TvServiceStatus.WAITING_FOR_PAIR,
    val connectedDeviceId: String = "",
    val connectedDeviceName: String = "",
    val connectedPlayerIndex: Int = 1,
    val rememberedDevices: List<PairedPhoneDevice> = emptyList(),
    val versionMismatchNoticeVi: String = "",
    val versionMismatchNoticeEn: String = "",
    val pairPromptTick: Long = 0L
)

/**
 * Máy chủ nhận tay cầm từ xa trên TV (CloudPlay :app).
 * - Quảng bá dịch vụ qua NSD/mDNS kiểu `_cloudplaypad._tcp`.
 * - Kênh điều khiển TCP: sinh mã 4 số mới mỗi lần mở màn ghép đôi, xác thực HMAC-SHA256,
 *   khóa tạm 3 phút khi nhập sai 3 lần, cấp mã nhớ riêng và hỗ trợ "Quên thiết bị".
 * - Kênh nút bấm UDP: nhận gói nhị phân 48 byte có mã phiên 16 byte và số thứ tự tăng dần;
 *   tự nhả toàn bộ nút nếu đứt TCP hoặc không nhận gói UDP quá 450ms (Watchdog).
 * - Đưa trạng thái nút/cần vào đúng đường của XboxBridge (__cpBtn / __cpStick / __cpReleaseAll).
 * - Chỉ chạy trong mạng nội bộ (LAN), không gửi dữ liệu ra internet, không ghi nhật ký nút bấm.
 */
object RemotePadTvServer {
    private const val PREFS_NAME = "cloudplay_remote_pad_tv_prefs"
    private const val KEY_TV_ID = "tv_unique_id"
    private const val KEY_TV_NAME = "tv_display_name"
    private const val KEY_PAIRED_DEVICES_JSON = "paired_devices_json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(TvRemotePadServerState())
    val state: StateFlow<TvRemotePadServerState> = _state.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var udpSocket: DatagramSocket? = null

    @Volatile
    private var tcpAcceptJob: Job? = null

    @Volatile
    private var udpReceiveJob: Job? = null

    @Volatile
    private var watchdogJob: Job? = null

    @Volatile
    private var lockoutTickerJob: Job? = null

    @Volatile
    private var lockoutUntilElapsedMs: Long = 0L

    private val activeClientSocketRef = AtomicReference<Socket?>(null)
    private val activeClientWriterRef = AtomicReference<BufferedWriter?>(null)

    @Volatile
    private var activeUdpSessionToken: ByteArray? = null

    private val lastReceivedUdpSequence = AtomicLong(-1L)
    private val lastUdpPacketElapsedMs = AtomicLong(0L)

    @Volatile
    private var lastAppliedPadState: RemotePadState = RemotePadState.NEUTRAL

    @Volatile
    private var nsdManager: NsdManager? = null

    @Volatile
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null

    @Volatile
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Đường điều khiển chuẩn đưa thẳng vào XboxBridge (và điều hướng UI khi cần):
     * - [onRemotePadButton]: (buttonIndex 0..16, pressed, analogValue 0f..1f) -> window.__cpBtn
     * - [onRemotePadStick]: (xAxisIndex, xVal, yAxisIndex, yVal) -> window.__cpStick
     * - [onRemotePadReleaseAll]: () -> window.__cpReleaseAll
     */
    @Volatile
    var onRemotePadButton: ((Int, Boolean, Float) -> Unit)? = null

    @Volatile
    var onRemotePadStick: ((Int, Float, Int, Float) -> Unit)? = null

    @Volatile
    var onRemotePadReleaseAll: (() -> Unit)? = null

    private val DIGITAL_BUTTON_INDICES = intArrayOf(
        0,  // A
        1,  // B
        2,  // X
        3,  // Y
        4,  // LB
        5,  // RB
        8,  // View
        9,  // Menu
        10, // L3
        11, // R3
        12, // D-Pad Up
        13, // D-Pad Down
        14, // D-Pad Left
        15, // D-Pad Right
        16  // Xbox Guide
    )

    fun ensureInitialized(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var tvId = prefs.getString(KEY_TV_ID, null).orEmpty()
        if (tvId.isBlank()) {
            tvId = UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_TV_ID, tvId).apply()
        }
        val modelName = Build.MODEL?.trim()?.takeIf { it.isNotEmpty() } ?: "Android TV"
        val defaultTvName = if (modelName.contains("TV", ignoreCase = true)) {
            modelName
        } else {
            "TV — $modelName"
        }
        val savedName = prefs.getString(KEY_TV_NAME, null)?.takeIf { it.isNotBlank() } ?: defaultTvName
        val remembered = loadRememberedDevices(context.applicationContext)
        val localIp = resolveLocalLanIpv4()

        _state.update { cur ->
            cur.copy(
                tvId = tvId,
                tvDisplayName = savedName,
                localIpAddress = localIp,
                rememberedDevices = remembered,
                currentPin = if (cur.currentPin == "0000") RemotePadCrypto.generateFourDigitPin() else cur.currentPin
            )
        }
    }

    /**
     * Gọi mỗi khi mở màn hình "Dùng điện thoại làm tay cầm" trong Cài đặt TV:
     * - Đổi mã 4 số mới theo đúng yêu cầu Giao thức v1 mục 4.
     * - Bật máy chủ TCP + UDP + quảng bá NSD `_cloudplaypad._tcp` nếu chưa chạy.
     */
    fun onPairingScreenOpened(context: Context) {
        ensureInitialized(context)
        val newPin = RemotePadCrypto.generateFourDigitPin()
        val localIp = resolveLocalLanIpv4()
        _state.update { cur ->
            cur.copy(
                currentPin = newPin,
                localIpAddress = localIp,
                versionMismatchNoticeVi = "",
                versionMismatchNoticeEn = ""
            )
        }
        startServerIfNeeded(context.applicationContext)
    }

    fun regeneratePin() {
        val newPin = RemotePadCrypto.generateFourDigitPin()
        lockoutUntilElapsedMs = 0L
        _state.update { cur ->
            cur.copy(
                currentPin = newPin,
                failedPinAttempts = 0,
                lockoutRemainingSec = 0,
                versionMismatchNoticeVi = "",
                versionMismatchNoticeEn = ""
            )
        }
    }

    fun startServerIfNeeded(context: Context) {
        ensureInitialized(context)
        startUdpReceiverIfNeeded()
        startWatchdogIfNeeded()

        if (tcpAcceptJob?.isActive == true && serverSocket?.isClosed == false) {
            refreshNsdRegistration()
            return
        }

        acquireMulticastLock(context)
        startLockoutTickerIfNeeded()

        tcpAcceptJob?.cancel()
        tcpAcceptJob = scope.launch {
            val socket = bindTcpServerSocket() ?: return@launch
            serverSocket = socket
            val boundPort = socket.localPort
            _state.update { cur ->
                cur.copy(
                    isRunning = true,
                    tcpPort = boundPort,
                    localIpAddress = resolveLocalLanIpv4()
                )
            }
            registerNsdService(context, boundPort)

            try {
                while (isActive && !socket.isClosed) {
                    val client = try {
                        socket.accept()
                    } catch (_: Exception) {
                        break
                    }
                    scope.launch {
                        handleIncomingClient(client)
                    }
                }
            } finally {
                _state.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun startUdpReceiverIfNeeded() {
        if (udpReceiveJob?.isActive == true && udpSocket?.isClosed == false) return
        udpReceiveJob?.cancel()
        udpReceiveJob = scope.launch {
            val dSocket = bindUdpServerSocket() ?: return@launch
            udpSocket = dSocket
            val boundUdpPort = dSocket.localPort
            _state.update { it.copy(udpPort = boundUdpPort) }

            val buf = ByteArray(128)
            val packet = DatagramPacket(buf, buf.size)
            try {
                while (isActive && !dSocket.isClosed) {
                    try {
                        dSocket.receive(packet)
                    } catch (_: Exception) {
                        break
                    }
                    val token = activeUdpSessionToken ?: continue
                    val prevSeq = lastReceivedUdpSequence.get()
                    val decoded = RemotePadUdpCodec.decodeAndVerify(
                        packetBytes = packet.data,
                        length = packet.length,
                        expectedSessionToken = token,
                        lastReceivedSequence = prevSeq
                    ) ?: continue

                    lastReceivedUdpSequence.set(decoded.sequenceNumber)
                    lastUdpPacketElapsedMs.set(SystemClock.elapsedRealtime())
                    applyIncomingPadState(decoded)
                }
            } finally {
                runCatching { dSocket.close() }
            }
        }
    }

    /**
     * Bộ canh gác an toàn nút bấm (Giao thức v1 mục 6):
     * Nếu không nhận gói UDP mới trong 450ms khi đang có nút/cần khác 0 -> nhả toàn bộ nút ngay lập tức.
     */
    private fun startWatchdogIfNeeded() {
        if (watchdogJob?.isActive == true) return
        watchdogJob = scope.launch {
            while (isActive) {
                delay(80L)
                val lastPktAt = lastUdpPacketElapsedMs.get()
                if (!lastAppliedPadState.isNeutral() && lastPktAt > 0L) {
                    val elapsed = SystemClock.elapsedRealtime() - lastPktAt
                    if (elapsed >= RemotePadProtocol.WATCHDOG_RELEASE_TIMEOUT_MS) {
                        forceReleaseAllInputsToNeutral()
                    }
                }
            }
        }
    }

    private fun applyIncomingPadState(next: RemotePadState) {
        val prev = lastAppliedPadState
        lastAppliedPadState = next

        val returnedToNeutral = next.isNeutral() && !prev.isNeutral()

        mainHandler.post {
            val btnCallback = onRemotePadButton
            val stickCallback = onRemotePadStick

            if (btnCallback != null) {
                // 1. Các nút bấm số (A, B, X, Y, LB, RB, View, Menu, L3, R3, D-Pad 4 hướng, Xbox)
                if (prev.buttonsBitmask != next.buttonsBitmask) {
                    for (xboxIdx in DIGITAL_BUTTON_INDICES) {
                        val wasPressed = RemotePadButtons.isXboxButtonPressed(prev.buttonsBitmask, xboxIdx)
                        val nowPressed = RemotePadButtons.isXboxButtonPressed(next.buttonsBitmask, xboxIdx)
                        if (wasPressed != nowPressed) {
                            btnCallback(xboxIdx, nowPressed, if (nowPressed) 1f else 0f)
                        }
                    }
                }

                // 2. Hai cò LT (index 6) và RT (index 7) từ 0.0 -> 1.0
                if (abs(prev.lt - next.lt) >= 0.005f || (next.lt == 0f && prev.lt != 0f)) {
                    val ltPressed = next.lt > 0.12f
                    btnCallback(6, ltPressed, next.lt)
                }
                if (abs(prev.rt - next.rt) >= 0.005f || (next.rt == 0f && prev.rt != 0f)) {
                    val rtPressed = next.rt > 0.12f
                    btnCallback(7, rtPressed, next.rt)
                }
            }

            if (stickCallback != null) {
                // 3. Cần trái (axes 0, 1)
                if (
                    abs(prev.lx - next.lx) >= 0.004f ||
                    abs(prev.ly - next.ly) >= 0.004f ||
                    ((next.lx == 0f && next.ly == 0f) && (prev.lx != 0f || prev.ly != 0f))
                ) {
                    stickCallback(0, next.lx, 1, next.ly)
                }
                // 4. Cần phải (axes 2, 3)
                if (
                    abs(prev.rx - next.rx) >= 0.004f ||
                    abs(prev.ry - next.ry) >= 0.004f ||
                    ((next.rx == 0f && next.ry == 0f) && (prev.rx != 0f || prev.ry != 0f))
                ) {
                    stickCallback(2, next.rx, 3, next.ry)
                }
            }

            if (returnedToNeutral) {
                onRemotePadReleaseAll?.invoke()
            }
        }
    }

    fun forceReleaseAllInputsToNeutral() {
        val hadActiveInputs = !lastAppliedPadState.isNeutral()
        if (hadActiveInputs) {
            applyIncomingPadState(RemotePadState.NEUTRAL)
        } else {
            lastAppliedPadState = RemotePadState.NEUTRAL
            mainHandler.post {
                onRemotePadReleaseAll?.invoke()
            }
        }
    }

    private fun bindTcpServerSocket(): ServerSocket? {
        return try {
            ServerSocket(RemotePadProtocol.DEFAULT_TCP_CONTROL_PORT).apply {
                reuseAddress = true
            }
        } catch (_: Exception) {
            try {
                ServerSocket(0).apply {
                    reuseAddress = true
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun bindUdpServerSocket(): DatagramSocket? {
        return try {
            DatagramSocket(RemotePadProtocol.DEFAULT_UDP_INPUT_PORT).apply {
                reuseAddress = true
            }
        } catch (_: Exception) {
            try {
                DatagramSocket(0).apply {
                    reuseAddress = true
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun handleIncomingClient(client: Socket) {
        try {
            client.tcpNoDelay = true
            client.soTimeout = 12_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))

            val curState = _state.value
            val nonceHex = RemotePadCrypto.generateNonceHex(16)
            val lockoutSec = currentLockoutRemainingSec()
            val currentStatus = if (activeClientSocketRef.get() != null) {
                TvServiceStatus.HAS_CONTROLLER
            } else {
                TvServiceStatus.WAITING_FOR_PAIR
            }

            // 1. Gửi gói HELLO chứa phiên bản giao thức, trạng thái TV và thử thách ngẫu nhiên nonce
            sendFrame(
                writer,
                RemoteControlFrame.Hello(
                    protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                    tvId = curState.tvId,
                    tvName = curState.tvDisplayName,
                    nonceHex = nonceHex,
                    udpPort = curState.udpPort,
                    status = currentStatus,
                    lockoutRemainingSec = lockoutSec
                )
            )

            // 2. Đọc yêu cầu xác thực (PAIR_PIN, AUTH_TOKEN, hoặc PAIR_PROMPT báo mở màn mã 4 số)
            val firstLine = reader.readLine() ?: run {
                runCatching { client.close() }
                return
            }
            if (firstLine.trim() == "PAIR_PROMPT") {
                _state.update { cur ->
                    cur.copy(pairPromptTick = SystemClock.elapsedRealtime())
                }
                runCatching { client.close() }
                return
            }
            val authFrame = RemoteControlCodec.parse(firstLine) ?: run {
                runCatching { client.close() }
                return
            }

            when (authFrame) {
                is RemoteControlFrame.PairWithPin -> {
                    if (!verifyClientProtocolVersion(authFrame.protocolVersion, writer)) {
                        runCatching { client.close() }
                        return
                    }
                    val activeLockSec = currentLockoutRemainingSec()
                    if (activeLockSec > 0) {
                        sendFrame(
                            writer,
                            RemoteControlFrame.AuthFail(
                                protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                                reason = AuthFailReason.LOCKED_OUT,
                                remainingAttempts = 0,
                                lockoutRemainingSec = activeLockSec
                            )
                        )
                        runCatching { client.close() }
                        return
                    }

                    val expectedPin = _state.value.currentPin
                    val pinValid = RemotePadCrypto.verifyPinProofHmac(
                        expectedPin4Digits = expectedPin,
                        tvNonceHex = nonceHex,
                        clientDeviceId = authFrame.deviceId,
                        candidateHmacHex = authFrame.pinHmacHex
                    )

                    if (!pinValid) {
                        val nextFailures = _state.value.failedPinAttempts + 1
                        if (nextFailures >= RemotePadProtocol.MAX_PIN_ATTEMPTS) {
                            lockoutUntilElapsedMs = SystemClock.elapsedRealtime() + RemotePadProtocol.PIN_LOCKOUT_DURATION_MS
                            val lockSec = (RemotePadProtocol.PIN_LOCKOUT_DURATION_MS / 1000L).toInt()
                            _state.update {
                                it.copy(
                                    failedPinAttempts = nextFailures,
                                    lockoutRemainingSec = lockSec
                                )
                            }
                            sendFrame(
                                writer,
                                RemoteControlFrame.AuthFail(
                                    protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                                    reason = AuthFailReason.LOCKED_OUT,
                                    remainingAttempts = 0,
                                    lockoutRemainingSec = lockSec
                                )
                            )
                        } else {
                            val rem = (RemotePadProtocol.MAX_PIN_ATTEMPTS - nextFailures).coerceAtLeast(0)
                            _state.update {
                                it.copy(failedPinAttempts = nextFailures)
                            }
                            sendFrame(
                                writer,
                                RemoteControlFrame.AuthFail(
                                    protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                                    reason = AuthFailReason.WRONG_PIN,
                                    remainingAttempts = rem,
                                    lockoutRemainingSec = 0
                                )
                            )
                        }
                        runCatching { client.close() }
                        return
                    }

                    // Ghép đôi thành công: cấp mã nhớ riêng cho điện thoại này và lưu lại
                    val rememberTokenHex = RemotePadCrypto.generateRememberTokenHex()
                    val cleanName = authFrame.deviceName.trim().ifBlank { "Ravno Pad" }
                    val cleanId = authFrame.deviceId.trim().ifBlank { UUID.randomUUID().toString() }
                    savePairedDevice(
                        PairedPhoneDevice(
                            deviceId = cleanId,
                            deviceName = cleanName,
                            rememberTokenHex = rememberTokenHex,
                            pairedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                    establishAuthenticatedSession(
                        client = client,
                        reader = reader,
                        writer = writer,
                        deviceId = cleanId,
                        deviceName = cleanName,
                        rememberTokenHex = rememberTokenHex,
                        tvNonceHex = nonceHex
                    )
                }

                is RemoteControlFrame.AuthWithToken -> {
                    if (!verifyClientProtocolVersion(authFrame.protocolVersion, writer)) {
                        runCatching { client.close() }
                        return
                    }
                    val saved = _state.value.rememberedDevices.firstOrNull { it.deviceId == authFrame.deviceId.trim() }
                    val tokenValid = saved != null && RemotePadCrypto.verifyTokenProofHmac(
                        expectedRememberTokenHex = saved.rememberTokenHex,
                        tvNonceHex = nonceHex,
                        clientDeviceId = authFrame.deviceId,
                        candidateHmacHex = authFrame.tokenHmacHex
                    )
                    if (!tokenValid || saved == null) {
                        sendFrame(
                            writer,
                            RemoteControlFrame.AuthFail(
                                protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                                reason = AuthFailReason.TOKEN_REVOKED,
                                remainingAttempts = RemotePadProtocol.MAX_PIN_ATTEMPTS,
                                lockoutRemainingSec = 0
                            )
                        )
                        runCatching { client.close() }
                        return
                    }

                    val updatedName = authFrame.deviceName.trim().ifBlank { saved.deviceName }
                    if (updatedName != saved.deviceName) {
                        savePairedDevice(saved.copy(deviceName = updatedName))
                    }
                    establishAuthenticatedSession(
                        client = client,
                        reader = reader,
                        writer = writer,
                        deviceId = saved.deviceId,
                        deviceName = updatedName,
                        rememberTokenHex = saved.rememberTokenHex,
                        tvNonceHex = nonceHex
                    )
                }

                else -> {
                    runCatching { client.close() }
                }
            }
        } catch (_: Exception) {
            runCatching { client.close() }
        }
    }

    private fun verifyClientProtocolVersion(clientVersion: Int, writer: BufferedWriter): Boolean {
        return when (
            RemotePadProtocol.checkVersionCompatibility(
                localVersion = RemotePadProtocol.PROTOCOL_VERSION,
                remoteVersion = clientVersion,
                isLocalTv = true
            )
        ) {
            VersionCheckResult.COMPATIBLE -> true
            VersionCheckResult.NEED_UPDATE_CLOUDPLAY -> {
                _state.update {
                    it.copy(
                        versionMismatchNoticeVi = "Cần cập nhật Ravno (TV đang dùng giao thức v${RemotePadProtocol.PROTOCOL_VERSION}, điện thoại dùng v$clientVersion).",
                        versionMismatchNoticeEn = "Need to update Ravno (TV uses protocol v${RemotePadProtocol.PROTOCOL_VERSION}, phone uses v$clientVersion)."
                    )
                }
                sendFrame(
                    writer,
                    RemoteControlFrame.AuthFail(
                        protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                        reason = AuthFailReason.VERSION_MISMATCH
                    )
                )
                false
            }
            VersionCheckResult.NEED_UPDATE_PAD -> {
                _state.update {
                    it.copy(
                        versionMismatchNoticeVi = "Cần cập nhật Ravno Pad trên điện thoại (TV dùng v${RemotePadProtocol.PROTOCOL_VERSION}, điện thoại dùng v$clientVersion).",
                        versionMismatchNoticeEn = "Need to update Ravno Pad on phone (TV uses v${RemotePadProtocol.PROTOCOL_VERSION}, phone uses v$clientVersion)."
                    )
                }
                sendFrame(
                    writer,
                    RemoteControlFrame.AuthFail(
                        protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                        reason = AuthFailReason.VERSION_MISMATCH
                    )
                )
                false
            }
        }
    }

    private fun establishAuthenticatedSession(
        client: Socket,
        reader: BufferedReader,
        writer: BufferedWriter,
        deviceId: String,
        deviceName: String,
        rememberTokenHex: String,
        tvNonceHex: String
    ) {
        activeClientSocketRef.getAndSet(client)?.let { oldSocket ->
            if (oldSocket !== client) {
                runCatching { oldSocket.close() }
            }
        }
        activeClientWriterRef.set(writer)

        val sessionTokenBytes = RemotePadCrypto.deriveUdpSessionToken(rememberTokenHex, tvNonceHex)
        lastReceivedUdpSequence.set(-1L)
        lastUdpPacketElapsedMs.set(SystemClock.elapsedRealtime())
        lastAppliedPadState = RemotePadState.NEUTRAL
        activeUdpSessionToken = sessionTokenBytes
        val sessionTokenHex = RemotePadCrypto.bytesToHex(sessionTokenBytes)

        lockoutUntilElapsedMs = 0L
        _state.update { cur ->
            cur.copy(
                failedPinAttempts = 0,
                lockoutRemainingSec = 0,
                status = TvServiceStatus.HAS_CONTROLLER,
                connectedDeviceId = deviceId,
                connectedDeviceName = deviceName,
                connectedPlayerIndex = 1,
                versionMismatchNoticeVi = "",
                versionMismatchNoticeEn = ""
            )
        }
        refreshNsdRegistration()

        sendFrame(
            writer,
            RemoteControlFrame.AuthOk(
                protocolVersion = RemotePadProtocol.PROTOCOL_VERSION,
                rememberTokenHex = rememberTokenHex,
                udpSessionTokenHex = sessionTokenHex,
                udpPort = _state.value.udpPort,
                playerIndex = 1,
                activeGameKey = currentActiveGameKey,
                activeGameTitle = currentActiveGameTitle
            )
        )
        if (currentActivePresetName.isNotBlank() || currentActiveGameTitle.isNotBlank()) {
            sendFrame(
                writer,
                RemoteControlFrame.LayoutSync(
                    presetName = currentActivePresetName.ifBlank { "standard" },
                    gameKey = currentActiveGameKey,
                    gameTitle = currentActiveGameTitle
                )
            )
        }

        client.soTimeout = 15_000
        try {
            while (!client.isClosed) {
                val line = reader.readLine() ?: break
                when (val frame = RemoteControlCodec.parse(line)) {
                    is RemoteControlFrame.Ping -> {
                        sendFrame(
                            writer,
                            RemoteControlFrame.Pong(
                                clientSentRealtimeMs = frame.clientSentRealtimeMs,
                                activeGameKey = currentActiveGameKey
                            )
                        )
                    }
                    is RemoteControlFrame.Disconnect -> {
                        break
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) {
        } finally {
            if (activeClientSocketRef.compareAndSet(client, null)) {
                activeClientWriterRef.set(null)
                activeUdpSessionToken = null
                lastReceivedUdpSequence.set(-1L)
                // An toàn nút bấm (Giao thức v1 mục 6): mất kênh điều khiển -> nhả toàn bộ nút ngay lập tức
                lastAppliedPadState = RemotePadState.NEUTRAL
                mainHandler.post {
                    onRemotePadReleaseAll?.invoke()
                }
                _state.update { cur ->
                    cur.copy(
                        status = TvServiceStatus.WAITING_FOR_PAIR,
                        connectedDeviceId = "",
                        connectedDeviceName = ""
                    )
                }
                refreshNsdRegistration()
            }
            runCatching { client.close() }
        }
    }

    @Volatile
    private var currentActiveGameKey: String = ""

    @Volatile
    private var currentActiveGameTitle: String = ""

    @Volatile
    private var currentActivePresetName: String = "standard"

    fun notifyActiveGameChanged(gameKey: String, gameTitle: String, presetName: String) {
        val cleanKey = gameKey.trim()
        val cleanTitle = gameTitle.trim()
        val cleanPreset = presetName.trim().ifBlank { "standard" }
        if (
            cleanKey == currentActiveGameKey &&
            cleanTitle == currentActiveGameTitle &&
            cleanPreset == currentActivePresetName
        ) {
            return
        }
        currentActiveGameKey = cleanKey
        currentActiveGameTitle = cleanTitle
        currentActivePresetName = cleanPreset
        val writer = activeClientWriterRef.get() ?: return
        scope.launch {
            sendFrame(
                writer,
                RemoteControlFrame.LayoutSync(
                    presetName = cleanPreset,
                    gameKey = cleanKey,
                    gameTitle = cleanTitle
                )
            )
        }
    }

    fun sendRumbleToPhone(durationMs: Int = 180, strongMagnitude: Float = 0.85f, weakMagnitude: Float = 0.65f): Boolean {
        val writer = activeClientWriterRef.get() ?: return false
        scope.launch {
            sendFrame(
                writer,
                RemoteControlFrame.Rumble(
                    durationMs = durationMs.coerceIn(0, 2000),
                    strongMagnitude = strongMagnitude.coerceIn(0f, 1f),
                    weakMagnitude = weakMagnitude.coerceIn(0f, 1f)
                )
            )
        }
        return true
    }

    fun disconnectActivePhone() {
        val writer = activeClientWriterRef.getAndSet(null)
        val socket = activeClientSocketRef.getAndSet(null)
        activeUdpSessionToken = null
        lastReceivedUdpSequence.set(-1L)
        lastAppliedPadState = RemotePadState.NEUTRAL
        mainHandler.post {
            onRemotePadReleaseAll?.invoke()
        }
        scope.launch {
            if (writer != null) {
                sendFrame(writer, RemoteControlFrame.Disconnect("TV_DISCONNECT"))
            }
            runCatching { socket?.close() }
        }
        _state.update { cur ->
            cur.copy(
                status = TvServiceStatus.WAITING_FOR_PAIR,
                connectedDeviceId = "",
                connectedDeviceName = "",
                currentPin = RemotePadCrypto.generateFourDigitPin()
            )
        }
        refreshNsdRegistration()
    }

    fun forgetDevice(deviceId: String? = null) {
        val ctx = appContext ?: return
        val currentRemembered = _state.value.rememberedDevices
        val updated = if (deviceId.isNullOrBlank()) {
            emptyList()
        } else {
            currentRemembered.filterNot { it.deviceId == deviceId }
        }
        persistRememberedDevices(ctx, updated)

        val shouldDisconnect = deviceId.isNullOrBlank() || _state.value.connectedDeviceId == deviceId
        if (shouldDisconnect) {
            val writer = activeClientWriterRef.getAndSet(null)
            val socket = activeClientSocketRef.getAndSet(null)
            activeUdpSessionToken = null
            lastReceivedUdpSequence.set(-1L)
            lastAppliedPadState = RemotePadState.NEUTRAL
            mainHandler.post {
                onRemotePadReleaseAll?.invoke()
            }
            scope.launch {
                if (writer != null) {
                    sendFrame(writer, RemoteControlFrame.Disconnect("FORGOTTEN"))
                }
                runCatching { socket?.close() }
            }
        }

        _state.update { cur ->
            cur.copy(
                rememberedDevices = updated,
                status = if (shouldDisconnect) TvServiceStatus.WAITING_FOR_PAIR else cur.status,
                connectedDeviceId = if (shouldDisconnect) "" else cur.connectedDeviceId,
                connectedDeviceName = if (shouldDisconnect) "" else cur.connectedDeviceName,
                currentPin = RemotePadCrypto.generateFourDigitPin()
            )
        }
        refreshNsdRegistration()
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

    private fun startLockoutTickerIfNeeded() {
        if (lockoutTickerJob?.isActive == true) return
        lockoutTickerJob = scope.launch {
            while (isActive) {
                val remSec = currentLockoutRemainingSec()
                if (_state.value.lockoutRemainingSec != remSec) {
                    _state.update { cur ->
                        cur.copy(
                            lockoutRemainingSec = remSec,
                            failedPinAttempts = if (remSec == 0 && cur.lockoutRemainingSec > 0) 0 else cur.failedPinAttempts
                        )
                    }
                }
                delay(1_000L)
            }
        }
    }

    private fun currentLockoutRemainingSec(): Int {
        val deadline = lockoutUntilElapsedMs
        if (deadline <= 0L) return 0
        val diffMs = deadline - SystemClock.elapsedRealtime()
        if (diffMs <= 0L) {
            lockoutUntilElapsedMs = 0L
            return 0
        }
        return ((diffMs + 999L) / 1000L).toInt()
    }

    private fun registerNsdService(context: Context, tcpPort: Int) {
        unregisterNsdService()
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = manager
            val cur = _state.value
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = cur.tvDisplayName.ifBlank { "Ravno TV" }
                serviceType = RemotePadProtocol.NSD_SERVICE_TYPE
                port = tcpPort
                setAttribute(RemotePadProtocol.TXT_KEY_VERSION, RemotePadProtocol.PROTOCOL_VERSION.toString())
                setAttribute(RemotePadProtocol.TXT_KEY_STATUS, cur.status.wireCode)
                setAttribute(RemotePadProtocol.TXT_KEY_UDP_PORT, cur.udpPort.toString())
                setAttribute(RemotePadProtocol.TXT_KEY_TV_ID, cur.tvId)
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    _state.update { it.copy(isNsdRegistered = true) }
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    _state.update { it.copy(isNsdRegistered = false) }
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    _state.update { it.copy(isNsdRegistered = false) }
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                }
            }
            nsdRegistrationListener = listener
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
            _state.update { it.copy(isNsdRegistered = false) }
        }
    }

    private fun refreshNsdRegistration() {
        val ctx = appContext ?: return
        val port = serverSocket?.localPort ?: return
        if (port > 0) {
            registerNsdService(ctx, port)
        }
    }

    private fun unregisterNsdService() {
        val manager = nsdManager ?: return
        val listener = nsdRegistrationListener ?: return
        nsdRegistrationListener = null
        try {
            manager.unregisterService(listener)
        } catch (_: Exception) {
        }
    }

    private fun acquireMulticastLock(context: Context) {
        try {
            if (multicastLock?.isHeld == true) return
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            multicastLock = wifi.createMulticastLock("CloudPlayPadTvNsdLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun savePairedDevice(device: PairedPhoneDevice) {
        val ctx = appContext ?: return
        val current = _state.value.rememberedDevices.filterNot { it.deviceId == device.deviceId }
        val updated = listOf(device) + current
        persistRememberedDevices(ctx, updated)
        _state.update { it.copy(rememberedDevices = updated) }
    }

    private fun loadRememberedDevices(context: Context): List<PairedPhoneDevice> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PAIRED_DEVICES_JSON, "[]").orEmpty()
            val arr = JSONArray(raw)
            val list = ArrayList<PairedPhoneDevice>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val name = obj.optString("name")
                val tok = obj.optString("token")
                val at = obj.optLong("at", 0L)
                if (id.isNotBlank() && tok.isNotBlank()) {
                    list.add(PairedPhoneDevice(id, name.ifBlank { "Ravno Pad" }, tok, at))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistRememberedDevices(context: Context, devices: List<PairedPhoneDevice>) {
        try {
            val arr = JSONArray()
            for (d in devices) {
                arr.put(
                    JSONObject().apply {
                        put("id", d.deviceId)
                        put("name", d.deviceName)
                        put("token", d.rememberTokenHex)
                        put("at", d.pairedAtEpochMs)
                    }
                )
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PAIRED_DEVICES_JSON, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun resolveLocalLanIpv4(): String {
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
