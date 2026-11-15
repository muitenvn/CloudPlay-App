package com.muitenvn.cloudplay.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePadProtocolTest {

    @Test
    fun udpCodec_encodesAndDecodesFullStateAndRejectsOldSequenceOrWrongToken() {
        val rememberToken = RemotePadCrypto.generateRememberTokenHex()
        val nonce = RemotePadCrypto.generateNonceHex()
        val sessionToken = RemotePadCrypto.deriveUdpSessionToken(rememberToken, nonce)

        val original = RemotePadState(
            sequenceNumber = 42L,
            playerIndex = 1,
            buttonsBitmask = RemotePadButtons.A or RemotePadButtons.RB or RemotePadButtons.DPAD_UP,
            lx = -0.75f,
            ly = 0.50f,
            rx = 0.25f,
            ry = -1.0f,
            lt = 0.35f,
            rt = 0.90f
        )

        val packet = RemotePadUdpCodec.encode(original, sessionToken)
        assertEquals(RemotePadProtocol.UDP_PACKET_BYTES, packet.size)

        val decoded = RemotePadUdpCodec.decodeAndVerify(
            packetBytes = packet,
            expectedSessionToken = sessionToken,
            lastReceivedSequence = 41L
        )
        assertNotNull(decoded)
        decoded!!
        assertEquals(42L, decoded.sequenceNumber)
        assertEquals(1, decoded.playerIndex)
        assertEquals(original.buttonsBitmask, decoded.buttonsBitmask)
        assertEquals(original.lx, decoded.lx, 0.002f)
        assertEquals(original.ly, decoded.ly, 0.002f)
        assertEquals(original.rx, decoded.rx, 0.002f)
        assertEquals(original.ry, decoded.ry, 0.002f)
        assertEquals(original.lt, decoded.lt, 0.002f)
        assertEquals(original.rt, decoded.rt, 0.002f)

        // Gói cũ hơn hoặc trùng sequenceNumber phải bị bỏ
        assertNull(
            RemotePadUdpCodec.decodeAndVerify(
                packetBytes = packet,
                expectedSessionToken = sessionToken,
                lastReceivedSequence = 42L
            )
        )

        // Gói sai session token phải bị bỏ
        val wrongToken = ByteArray(RemotePadProtocol.SESSION_TOKEN_BYTES) { 0x11 }
        assertNull(
            RemotePadUdpCodec.decodeAndVerify(
                packetBytes = packet,
                expectedSessionToken = wrongToken,
                lastReceivedSequence = 0L
            )
        )
    }

    @Test
    fun crypto_verifiesFourDigitPinAndRememberTokenHmac() {
        val pin = "4829"
        val nonce = RemotePadCrypto.generateNonceHex()
        val deviceId = "phone-pixel-8"

        val pinHmac = RemotePadCrypto.computePinProofHmac(pin, nonce, deviceId)
        assertTrue(RemotePadCrypto.verifyPinProofHmac(pin, nonce, deviceId, pinHmac))
        assertFalse(RemotePadCrypto.verifyPinProofHmac("9999", nonce, deviceId, pinHmac))
        assertFalse(RemotePadCrypto.verifyPinProofHmac(pin, nonce, "other-device", pinHmac))
    }

    @Test
    fun controlCodec_serializesAndParsesFrames() {
        val hello = RemoteControlFrame.Hello(
            tvId = "tv-living-room",
            tvName = "TV Phòng Khách | Sony",
            nonceHex = "0123456789abcdef0123456789abcdef",
            udpPort = 47992,
            status = TvServiceStatus.WAITING_FOR_PAIR,
            lockoutRemainingSec = 0
        )
        val wire = RemoteControlCodec.serialize(hello)
        val parsed = RemoteControlCodec.parse(wire) as? RemoteControlFrame.Hello
        assertNotNull(parsed)
        assertEquals("TV Phòng Khách | Sony", parsed?.tvName)
        assertEquals(47992, parsed?.udpPort)
        assertEquals(TvServiceStatus.WAITING_FOR_PAIR, parsed?.status)
    }
}
