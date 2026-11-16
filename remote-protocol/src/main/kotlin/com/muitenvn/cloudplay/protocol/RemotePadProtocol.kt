package com.muitenvn.cloudplay.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.roundToInt

/**
 * Giao thức điều khiển từ xa CloudPlay Pad v1 (Kotlin thuần, không phụ thuộc Android UI).
 * Nguồn duy nhất cho cả CloudPlay (:app) và CloudPlay Pad (:pad).
 */
object RemotePadProtocol {
    const val PROTOCOL_VERSION: Int = 1
    const val NSD_SERVICE_TYPE: String = "_cloudplaypad._tcp"

    const val DEFAULT_TCP_CONTROL_PORT: Int = 47991
    const val DEFAULT_UDP_INPUT_PORT: Int = 47992

    /** Điện thoại gửi định kỳ trạng thái mỗi 100ms khi không đổi để TV biết kết nối còn sống */
    const val UDP_HEARTBEAT_INTERVAL_MS: Long = 100L

    /** TV không nhận gói UDP hoặc đứt TCP quá 450ms -> nhả toàn bộ nút và đưa cần/cò về 0 ngay */
    const val WATCHDOG_RELEASE_TIMEOUT_MS: Long = 450L

    /** Nhập sai mã 4 số quá 3 lần thì TV khóa ghép đôi tạm thời (3 phút) */
    const val MAX_PIN_ATTEMPTS: Int = 3
    const val PIN_LOCKOUT_DURATION_MS: Long = 3 * 60 * 1000L

    const val UDP_PACKET_BYTES: Int = 48
    const val SESSION_TOKEN_BYTES: Int = 16
    const val MAGIC_BYTE_0: Byte = 0x43 // 'C'
    const val MAGIC_BYTE_1: Byte = 0x50 // 'P'

    // Khóa thuộc tính TXT cho NSD/mDNS
    const val TXT_KEY_VERSION: String = "v"
    const val TXT_KEY_STATUS: String = "st"
    const val TXT_KEY_UDP_PORT: String = "up"
    const val TXT_KEY_TV_ID: String = "id"

    fun checkVersionCompatibility(localVersion: Int, remoteVersion: Int, isLocalTv: Boolean): VersionCheckResult {
        if (localVersion == remoteVersion) return VersionCheckResult.COMPATIBLE
        return if (isLocalTv) {
            if (localVersion < remoteVersion) VersionCheckResult.NEED_UPDATE_CLOUDPLAY else VersionCheckResult.NEED_UPDATE_PAD
        } else {
            if (localVersion < remoteVersion) VersionCheckResult.NEED_UPDATE_PAD else VersionCheckResult.NEED_UPDATE_CLOUDPLAY
        }
    }
}

enum class TvServiceStatus(val wireCode: String) {
    WAITING_FOR_PAIR("WAITING"),
    HAS_CONTROLLER("CONNECTED"),
    BUSY("BUSY");

    companion object {
        fun fromWireCode(code: String?): TvServiceStatus {
            return entries.firstOrNull { it.wireCode.equals(code?.trim(), ignoreCase = true) }
                ?: WAITING_FOR_PAIR
        }
    }
}

enum class VersionCheckResult {
    COMPATIBLE,
    NEED_UPDATE_CLOUDPLAY,
    NEED_UPDATE_PAD
}

/**
 * Bitmask các nút bấm theo đúng Giao thức v1:
 * A, B, X, Y, LB, RB, View, Menu, L3, R3, D-Pad 4 hướng (+ nút Xbox Guide tùy chọn).
 */
object RemotePadButtons {
    const val A: Int = 1 shl 0
    const val B: Int = 1 shl 1
    const val X: Int = 1 shl 2
    const val Y: Int = 1 shl 3
    const val LB: Int = 1 shl 4
    const val RB: Int = 1 shl 5
    const val VIEW: Int = 1 shl 6
    const val MENU: Int = 1 shl 7
    const val L3: Int = 1 shl 8
    const val R3: Int = 1 shl 9
    const val DPAD_UP: Int = 1 shl 10
    const val DPAD_DOWN: Int = 1 shl 11
    const val DPAD_LEFT: Int = 1 shl 12
    const val DPAD_RIGHT: Int = 1 shl 13
    const val XBOX: Int = 1 shl 14

    /**
     * Bảng ánh xạ từ chỉ số nút chuẩn W3C Standard Gamepad (0..16 trong XboxBridge/VirtualGamepad)
     * sang bitmask của Giao thức v1.
     */
    fun bitForXboxButtonIndex(xboxIndex: Int): Int = when (xboxIndex) {
        0 -> A
        1 -> B
        2 -> X
        3 -> Y
        4 -> LB
        5 -> RB
        8 -> VIEW
        9 -> MENU
        10 -> L3
        11 -> R3
        12 -> DPAD_UP
        13 -> DPAD_DOWN
        14 -> DPAD_LEFT
        15 -> DPAD_RIGHT
        16 -> XBOX
        else -> 0
    }

    fun isXboxButtonPressed(mask: Int, xboxIndex: Int): Boolean {
        val bit = bitForXboxButtonIndex(xboxIndex)
        return bit != 0 && (mask and bit) != 0
    }
}

/**
 * Toàn bộ trạng thái tay cầm gửi trong mỗi gói UDP.
 */
data class RemotePadState(
    val sequenceNumber: Long = 0L,
    val playerIndex: Int = 1,
    val buttonsBitmask: Int = 0,
    val lx: Float = 0f,
    val ly: Float = 0f,
    val rx: Float = 0f,
    val ry: Float = 0f,
    val lt: Float = 0f,
    val rt: Float = 0f
) {
    fun isNeutral(): Boolean {
        return buttonsBitmask == 0 &&
            lx == 0f && ly == 0f &&
            rx == 0f && ry == 0f &&
            lt == 0f && rt == 0f
    }

    companion object {
        val NEUTRAL = RemotePadState(
            sequenceNumber = 0L,
            playerIndex = 1,
            buttonsBitmask = 0,
            lx = 0f,
            ly = 0f,
            rx = 0f,
            ry = 0f,
            lt = 0f,
            rt = 0f
        )
    }
}

/**
 * Bộ mã hóa / giải mã gói tin UDP nhị phân 48 byte (không cấp phát rác, kiểm tra mã phiên & số thứ tự).
 */
object RemotePadUdpCodec {
    private const val AXIS_SCALE = 32767f

    fun encode(
        state: RemotePadState,
        sessionToken: ByteArray,
        outBuffer: ByteArray = ByteArray(RemotePadProtocol.UDP_PACKET_BYTES)
    ): ByteArray {
        require(sessionToken.size == RemotePadProtocol.SESSION_TOKEN_BYTES) {
            "Session token must be ${RemotePadProtocol.SESSION_TOKEN_BYTES} bytes"
        }
        require(outBuffer.size >= RemotePadProtocol.UDP_PACKET_BYTES) {
            "Output buffer must be at least ${RemotePadProtocol.UDP_PACKET_BYTES} bytes"
        }

        val bb = ByteBuffer.wrap(outBuffer, 0, RemotePadProtocol.UDP_PACKET_BYTES).order(ByteOrder.BIG_ENDIAN)
        bb.put(RemotePadProtocol.MAGIC_BYTE_0)
        bb.put(RemotePadProtocol.MAGIC_BYTE_1)
        bb.put(RemotePadProtocol.PROTOCOL_VERSION.toByte())
        bb.put(state.playerIndex.coerceIn(1, 4).toByte())
        bb.put(sessionToken, 0, RemotePadProtocol.SESSION_TOKEN_BYTES)
        bb.putLong(state.sequenceNumber)
        bb.putInt(state.buttonsBitmask)
        bb.putShort(quantizeSignedAxis(state.lx))
        bb.putShort(quantizeSignedAxis(state.ly))
        bb.putShort(quantizeSignedAxis(state.rx))
        bb.putShort(quantizeSignedAxis(state.ry))
        bb.putShort(quantizeTrigger(state.lt))
        bb.putShort(quantizeTrigger(state.rt))
        bb.putInt(0) // 4 bytes reserved
        return outBuffer
    }

    /**
     * Giải mã và xác thực gói UDP.
     * - Bỏ gói nếu sai kích thước, sai Magic, sai phiên bản, hoặc sai mã phiên 16 byte.
     * - Bỏ gói nếu sequenceNumber <= lastReceivedSequence (chống gói trễ / lặp).
     */
    fun decodeAndVerify(
        packetBytes: ByteArray,
        length: Int = packetBytes.size,
        expectedSessionToken: ByteArray,
        lastReceivedSequence: Long = -1L
    ): RemotePadState? {
        if (length < RemotePadProtocol.UDP_PACKET_BYTES) return null
        if (expectedSessionToken.size != RemotePadProtocol.SESSION_TOKEN_BYTES) return null
        if (packetBytes[0] != RemotePadProtocol.MAGIC_BYTE_0 || packetBytes[1] != RemotePadProtocol.MAGIC_BYTE_1) {
            return null
        }
        val version = packetBytes[2].toInt() and 0xFF
        if (version != RemotePadProtocol.PROTOCOL_VERSION) return null

        // Kiểm tra hằng thời gian (constant-time) cho 16 byte session token
        var tokenDiff = 0
        for (i in 0 until RemotePadProtocol.SESSION_TOKEN_BYTES) {
            tokenDiff = tokenDiff or (packetBytes[4 + i].toInt() xor expectedSessionToken[i].toInt())
        }
        if (tokenDiff != 0) return null

        val bb = ByteBuffer.wrap(packetBytes, 0, RemotePadProtocol.UDP_PACKET_BYTES).order(ByteOrder.BIG_ENDIAN)
        val playerIdx = (packetBytes[3].toInt() and 0xFF).coerceIn(1, 4)
        bb.position(20)
        val seq = bb.long
        if (lastReceivedSequence >= 0L && seq <= lastReceivedSequence) {
            return null
        }
        val buttonsMask = bb.int
        val lx = dequantizeSignedAxis(bb.short)
        val ly = dequantizeSignedAxis(bb.short)
        val rx = dequantizeSignedAxis(bb.short)
        val ry = dequantizeSignedAxis(bb.short)
        val lt = dequantizeTrigger(bb.short)
        val rt = dequantizeTrigger(bb.short)

        return RemotePadState(
            sequenceNumber = seq,
            playerIndex = playerIdx,
            buttonsBitmask = buttonsMask,
            lx = lx,
            ly = ly,
            rx = rx,
            ry = ry,
            lt = lt,
            rt = rt
        )
    }

    private fun quantizeSignedAxis(value: Float): Short {
        val clamped = value.coerceIn(-1f, 1f)
        return (clamped * AXIS_SCALE).roundToInt().coerceIn(-32767, 32767).toShort()
    }

    private fun dequantizeSignedAxis(raw: Short): Float {
        val v = raw.toFloat() / AXIS_SCALE
        return if (kotlin.math.abs(v) < 0.0005f) 0f else v.coerceIn(-1f, 1f)
    }

    private fun quantizeTrigger(value: Float): Short {
        val clamped = value.coerceIn(0f, 1f)
        return (clamped * AXIS_SCALE).roundToInt().coerceIn(0, 32767).toShort()
    }

    private fun dequantizeTrigger(raw: Short): Float {
        val v = raw.coerceAtLeast(0).toFloat() / AXIS_SCALE
        return if (v < 0.0005f) 0f else v.coerceIn(0f, 1f)
    }
}

/**
 * Mật mã học cho ghép đôi HMAC-SHA256 và sinh mã phiên UDP.
 * Điện thoại KHÔNG bao giờ gửi mã 4 số dạng rõ qua mạng.
 */
object RemotePadCrypto {
    private const val HMAC_ALGO = "HmacSHA256"
    private val secureRandom = SecureRandom()

    fun generateFourDigitPin(): String {
        val n = secureRandom.nextInt(10_000)
        return "%04d".format(n)
    }

    fun generateNonceHex(byteLength: Int = 16): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    fun generateRememberTokenHex(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    fun computePinProofHmac(pin4Digits: String, tvNonceHex: String, clientDeviceId: String): String {
        val normalizedPin = pin4Digits.trim()
        val message = "CLOUDPLAY_PIN_V1|$tvNonceHex|${clientDeviceId.trim()}"
        return hmacSha256Hex(key = normalizedPin, message = message)
    }

    fun computeTokenProofHmac(rememberTokenHex: String, tvNonceHex: String, clientDeviceId: String): String {
        val normalizedToken = rememberTokenHex.trim().lowercase()
        val message = "CLOUDPLAY_TOKEN_V1|$tvNonceHex|${clientDeviceId.trim()}"
        return hmacSha256Hex(key = normalizedToken, message = message)
    }

    fun verifyPinProofHmac(
        expectedPin4Digits: String,
        tvNonceHex: String,
        clientDeviceId: String,
        candidateHmacHex: String
    ): Boolean {
        val expected = computePinProofHmac(expectedPin4Digits, tvNonceHex, clientDeviceId)
        return constantTimeHexEquals(expected, candidateHmacHex)
    }

    fun verifyTokenProofHmac(
        expectedRememberTokenHex: String,
        tvNonceHex: String,
        clientDeviceId: String,
        candidateHmacHex: String
    ): Boolean {
        val expected = computeTokenProofHmac(expectedRememberTokenHex, tvNonceHex, clientDeviceId)
        return constantTimeHexEquals(expected, candidateHmacHex)
    }

    fun deriveUdpSessionToken(rememberTokenHex: String, tvNonceHex: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val raw = "CLOUDPLAY_UDP_SESSION_V1|${rememberTokenHex.trim().lowercase()}|${tvNonceHex.trim().lowercase()}"
            .toByteArray(StandardCharsets.UTF_8)
        val full = digest.digest(raw)
        return full.copyOfRange(0, RemotePadProtocol.SESSION_TOKEN_BYTES)
    }

    fun hmacSha256Hex(key: String, message: String): String {
        val mac = Mac.getInstance(HMAC_ALGO)
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        mac.init(SecretKeySpec(if (keyBytes.isNotEmpty()) keyBytes else ByteArray(1), HMAC_ALGO))
        val out = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(out)
    }

    fun constantTimeHexEquals(a: String, b: String): Boolean {
        val ca = a.trim().lowercase().toByteArray(StandardCharsets.UTF_8)
        val cb = b.trim().lowercase().toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(ca, cb)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(Character.forDigit(v ushr 4, 16))
            sb.append(Character.forDigit(v and 0x0F, 16))
        }
        return sb.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        if (clean.length % 2 != 0) return ByteArray(0)
        val out = ByteArray(clean.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(clean[i * 2], 16)
            val lo = Character.digit(clean[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) return ByteArray(0)
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}

/**
 * Thông điệp kênh điều khiển TCP (ghép đôi, giữ kết nối, đo Ping thật, rung ngược, đổi bố cục, ngắt kết nối).
 * Mã hóa dạng dòng văn bản `TYPE|key1=val1|key2=val2` thuần Kotlin, không phụ thuộc thư viện ngoài.
 */
sealed class RemoteControlFrame {
    data class Hello(
        val protocolVersion: Int = RemotePadProtocol.PROTOCOL_VERSION,
        val tvId: String,
        val tvName: String,
        val nonceHex: String,
        val udpPort: Int,
        val status: TvServiceStatus,
        val lockoutRemainingSec: Int = 0
    ) : RemoteControlFrame()

    data class PairWithPin(
        val protocolVersion: Int = RemotePadProtocol.PROTOCOL_VERSION,
        val deviceId: String,
        val deviceName: String,
        val pinHmacHex: String
    ) : RemoteControlFrame()

    data class AuthWithToken(
        val protocolVersion: Int = RemotePadProtocol.PROTOCOL_VERSION,
        val deviceId: String,
        val deviceName: String,
        val tokenHmacHex: String
    ) : RemoteControlFrame()

    data class AuthOk(
        val protocolVersion: Int = RemotePadProtocol.PROTOCOL_VERSION,
        val rememberTokenHex: String,
        val udpSessionTokenHex: String,
        val udpPort: Int,
        val playerIndex: Int = 1,
        val activeGameKey: String = "",
        val activeGameTitle: String = ""
    ) : RemoteControlFrame()

    data class AuthFail(
        val protocolVersion: Int = RemotePadProtocol.PROTOCOL_VERSION,
        val reason: AuthFailReason,
        val remainingAttempts: Int = 0,
        val lockoutRemainingSec: Int = 0
    ) : RemoteControlFrame()

    data class Ping(val clientSentRealtimeMs: Long) : RemoteControlFrame()
    data class Pong(val clientSentRealtimeMs: Long, val activeGameKey: String = "") : RemoteControlFrame()

    data class Rumble(
        val durationMs: Int,
        val strongMagnitude: Float,
        val weakMagnitude: Float
    ) : RemoteControlFrame()

    data class LayoutSync(
        val presetName: String,
        val gameKey: String = "",
        val gameTitle: String = ""
    ) : RemoteControlFrame()

    data class Disconnect(val reason: String = "USER") : RemoteControlFrame()
}

enum class AuthFailReason {
    WRONG_PIN,
    LOCKED_OUT,
    TOKEN_REVOKED,
    VERSION_MISMATCH,
    TV_BUSY
}

object RemoteControlCodec {
    private fun escape(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace("|", "\\p")
            .replace("=", "\\e")
            .replace("\n", " ")
            .replace("\r", " ")
    }

    private fun unescape(escaped: String): String {
        val sb = StringBuilder(escaped.length)
        var i = 0
        while (i < escaped.length) {
            val c = escaped[i]
            if (c == '\\' && i + 1 < escaped.length) {
                when (val next = escaped[i + 1]) {
                    '\\' -> sb.append('\\')
                    'p' -> sb.append('|')
                    'e' -> sb.append('=')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    fun serialize(frame: RemoteControlFrame): String {
        return when (frame) {
            is RemoteControlFrame.Hello ->
                "HELLO|v=${frame.protocolVersion}|id=${escape(frame.tvId)}|name=${escape(frame.tvName)}|nonce=${escape(frame.nonceHex)}|up=${frame.udpPort}|st=${frame.status.wireCode}|lock=${frame.lockoutRemainingSec}"
            is RemoteControlFrame.PairWithPin ->
                "PAIR_PIN|v=${frame.protocolVersion}|id=${escape(frame.deviceId)}|name=${escape(frame.deviceName)}|hmac=${escape(frame.pinHmacHex)}"
            is RemoteControlFrame.AuthWithToken ->
                "AUTH_TOKEN|v=${frame.protocolVersion}|id=${escape(frame.deviceId)}|name=${escape(frame.deviceName)}|hmac=${escape(frame.tokenHmacHex)}"
            is RemoteControlFrame.AuthOk ->
                "AUTH_OK|v=${frame.protocolVersion}|tok=${escape(frame.rememberTokenHex)}|ukey=${escape(frame.udpSessionTokenHex)}|up=${frame.udpPort}|p=${frame.playerIndex}|gk=${escape(frame.activeGameKey)}|gt=${escape(frame.activeGameTitle)}"
            is RemoteControlFrame.AuthFail ->
                "AUTH_FAIL|v=${frame.protocolVersion}|err=${frame.reason.name}|rem=${frame.remainingAttempts}|lock=${frame.lockoutRemainingSec}"
            is RemoteControlFrame.Ping ->
                "PING|t=${frame.clientSentRealtimeMs}"
            is RemoteControlFrame.Pong ->
                "PONG|t=${frame.clientSentRealtimeMs}|gk=${escape(frame.activeGameKey)}"
            is RemoteControlFrame.Rumble ->
                "RUMBLE|dur=${frame.durationMs}|s=${frame.strongMagnitude}|w=${frame.weakMagnitude}"
            is RemoteControlFrame.LayoutSync ->
                "LAYOUT|pre=${escape(frame.presetName)}|gk=${escape(frame.gameKey)}|gt=${escape(frame.gameTitle)}"
            is RemoteControlFrame.Disconnect ->
                "DISCONNECT|r=${escape(frame.reason)}"
        }
    }

    fun parse(line: String?): RemoteControlFrame? {
        val trimmed = line?.trim() ?: return null
        if (trimmed.isEmpty()) return null
        val parts = splitUnescaped(trimmed, '|')
        if (parts.isEmpty()) return null
        val type = parts[0].uppercase()
        val map = LinkedHashMap<String, String>()
        for (i in 1 until parts.size) {
            val kv = splitUnescaped(parts[i], '=', limit = 2)
            if (kv.size == 2) {
                map[unescape(kv[0])] = unescape(kv[1])
            }
        }
        return when (type) {
            "HELLO" -> RemoteControlFrame.Hello(
                protocolVersion = map["v"]?.toIntOrNull() ?: RemotePadProtocol.PROTOCOL_VERSION,
                tvId = map["id"].orEmpty(),
                tvName = map["name"].orEmpty().ifBlank { "Ravno TV" },
                nonceHex = map["nonce"].orEmpty(),
                udpPort = map["up"]?.toIntOrNull() ?: RemotePadProtocol.DEFAULT_UDP_INPUT_PORT,
                status = TvServiceStatus.fromWireCode(map["st"]),
                lockoutRemainingSec = map["lock"]?.toIntOrNull() ?: 0
            )
            "PAIR_PIN" -> RemoteControlFrame.PairWithPin(
                protocolVersion = map["v"]?.toIntOrNull() ?: RemotePadProtocol.PROTOCOL_VERSION,
                deviceId = map["id"].orEmpty(),
                deviceName = map["name"].orEmpty().ifBlank { "Ravno Pad" },
                pinHmacHex = map["hmac"].orEmpty()
            )
            "AUTH_TOKEN" -> RemoteControlFrame.AuthWithToken(
                protocolVersion = map["v"]?.toIntOrNull() ?: RemotePadProtocol.PROTOCOL_VERSION,
                deviceId = map["id"].orEmpty(),
                deviceName = map["name"].orEmpty().ifBlank { "Ravno Pad" },
                tokenHmacHex = map["hmac"].orEmpty()
            )
            "AUTH_OK" -> RemoteControlFrame.AuthOk(
                protocolVersion = map["v"]?.toIntOrNull() ?: RemotePadProtocol.PROTOCOL_VERSION,
                rememberTokenHex = map["tok"].orEmpty(),
                udpSessionTokenHex = map["ukey"].orEmpty(),
                udpPort = map["up"]?.toIntOrNull() ?: RemotePadProtocol.DEFAULT_UDP_INPUT_PORT,
                playerIndex = map["p"]?.toIntOrNull() ?: 1,
                activeGameKey = map["gk"].orEmpty(),
                activeGameTitle = map["gt"].orEmpty()
            )
            "AUTH_FAIL" -> RemoteControlFrame.AuthFail(
                protocolVersion = map["v"]?.toIntOrNull() ?: RemotePadProtocol.PROTOCOL_VERSION,
                reason = runCatching { AuthFailReason.valueOf(map["err"].orEmpty()) }
                    .getOrDefault(AuthFailReason.WRONG_PIN),
                remainingAttempts = map["rem"]?.toIntOrNull() ?: 0,
                lockoutRemainingSec = map["lock"]?.toIntOrNull() ?: 0
            )
            "PING" -> RemoteControlFrame.Ping(
                clientSentRealtimeMs = map["t"]?.toLongOrNull() ?: 0L
            )
            "PONG" -> RemoteControlFrame.Pong(
                clientSentRealtimeMs = map["t"]?.toLongOrNull() ?: 0L,
                activeGameKey = map["gk"].orEmpty()
            )
            "RUMBLE" -> RemoteControlFrame.Rumble(
                durationMs = map["dur"]?.toIntOrNull() ?: 0,
                strongMagnitude = map["s"]?.toFloatOrNull() ?: 0f,
                weakMagnitude = map["w"]?.toFloatOrNull() ?: 0f
            )
            "LAYOUT" -> RemoteControlFrame.LayoutSync(
                presetName = map["pre"].orEmpty(),
                gameKey = map["gk"].orEmpty(),
                gameTitle = map["gt"].orEmpty()
            )
            "DISCONNECT" -> RemoteControlFrame.Disconnect(
                reason = map["r"].orEmpty().ifBlank { "USER" }
            )
            else -> null
        }
    }

    private fun splitUnescaped(input: String, delimiter: Char, limit: Int = Int.MAX_VALUE): List<String> {
        val result = ArrayList<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                cur.append(c).append(input[i + 1])
                i += 2
                continue
            }
            if (c == delimiter && result.size + 1 < limit) {
                result.add(cur.toString())
                cur.setLength(0)
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString())
        return result
    }
}
