package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cloudplay.StreamController
import com.example.data.SettingsRepository
import com.example.vpn.NetworkMode
import com.example.vpn.VpnManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @org.junit.Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        VpnManager.updateSmartDecision(
            com.example.vpn.GeoIpResult(
                ip = "14.225.204.10",
                countryCode = "VN",
                countryName = "Việt Nam",
                city = "Hanoi",
                flagEmoji = "🇻🇳",
                isXboxSupported = false
            )
        )
        VpnManager.disconnect(context)
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CloudPlay", appName)
    }

    @Test
    fun `verify default vpn state and settings`() {
        val vpnState = VpnManager.state.value
        assertEquals(NetworkMode.DISCONNECTED, vpnState.mode)
        assertNotNull(vpnState.currentServer)
        assertTrue(vpnState.currentServer.name.contains("Japan"))

        val config = SettingsRepository.config.value
        assertTrue(config.autoBypassEnabled)
        assertTrue(config.smartVpnEnabled)
        assertEquals(2, config.autoBypassDelaySeconds)
    }

    @Test
    fun `verify stream controller auto bypass trigger`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = StreamController(context)

        controller.onGameLaunchInitiated("Fortnite")
        assertEquals(true, controller.uiState.value.isGameActive)
        assertEquals("Fortnite", controller.uiState.value.gameTitle)
    }

    @Test
    fun `verify smart vpn ip decision for unsupported and supported countries`() {
        val vnGeo = com.example.vpn.GeoIpResult(
            ip = "14.225.204.10",
            countryCode = "VN",
            countryName = "Việt Nam",
            city = "Hanoi",
            flagEmoji = "🇻🇳",
            isXboxSupported = false
        )
        VpnManager.updateSmartDecision(vnGeo)
        assertTrue("Vietnam player must need VPN", VpnManager.needsVpnToPlay())
        assertEquals(true, VpnManager.state.value.smartDecision.needsVpn)

        val usGeo = com.example.vpn.GeoIpResult(
            ip = "20.150.10.5",
            countryCode = "US",
            countryName = "Hoa Kỳ (United States)",
            city = "Seattle",
            flagEmoji = "🇺🇸",
            isXboxSupported = true
        )
        VpnManager.updateSmartDecision(usGeo)
        assertEquals(false, VpnManager.needsVpnToPlay())
        assertEquals(false, VpnManager.state.value.smartDecision.needsVpn)
        assertEquals(com.example.vpn.SmartVpnPhase.DIRECT_SUPPORTED_REGION, VpnManager.state.value.smartVpnPhase)
    }

    @Test
    fun `verify smart vpn lifecycle switch to direct network on stream connect`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vnGeo = com.example.vpn.GeoIpResult(
            ip = "14.225.204.10",
            countryCode = "VN",
            countryName = "Việt Nam",
            city = "Hanoi",
            flagEmoji = "🇻🇳",
            isXboxSupported = false
        )
        VpnManager.updateSmartDecision(vnGeo)
        VpnManager.autoTurnOnForLaunch(context)
        assertEquals(NetworkMode.VPN_JAPAN, VpnManager.state.value.mode)
        assertEquals(com.example.vpn.SmartVpnPhase.BYPASS_ACTIVE, VpnManager.state.value.smartVpnPhase)

        // Stream connection switches to Direct ISP
        VpnManager.switchToDirectNetwork(context, "WebRTC game stream connected")
        assertEquals(NetworkMode.DIRECT_NETWORK, VpnManager.state.value.mode)
        assertEquals(com.example.vpn.SmartVpnPhase.DIRECT_GAMING, VpnManager.state.value.smartVpnPhase)
    }

    @Test
    fun `verify xbox locale parsing and fallback to en-US`() {
        assertEquals("en-US", SettingsRepository.parseLocaleFromInput("xbox.com/en-US"))
        assertEquals("en-US", SettingsRepository.parseLocaleFromInput("https://www.xbox.com/en-US/play"))
        assertEquals("en-GB", SettingsRepository.parseLocaleFromInput("en-GB"))
        assertEquals("ko-KR", SettingsRepository.parseLocaleFromInput("ko-KR"))
        assertEquals("ja-JP", SettingsRepository.parseLocaleFromInput("ja-JP"))

        // Format Xbox URLs with locale
        val originalUrl = "https://www.xbox.com/ja-JP/play/games/forza"
        val adaptedEn = SettingsRepository.formatXboxUrlWithLocale(originalUrl, "en-US")
        assertEquals("https://www.xbox.com/en-US/play/games/forza", adaptedEn)

        val noLocaleUrl = "https://www.xbox.com/play"
        val adaptedKo = SettingsRepository.formatXboxUrlWithLocale(noLocaleUrl, "ko-KR")
        assertEquals("https://www.xbox.com/ko-KR/play", adaptedKo)

        // Custom locale configuration
        SettingsRepository.updateCustomLocale("en-US")
        assertEquals("en-US", SettingsRepository.config.value.getEffectiveLocale())
        assertEquals("https://www.xbox.com/en-US/play", SettingsRepository.config.value.targetRegionUrl)
    }

    @Test
    fun `verify system revolution 1 0 optimization`() {
        SettingsRepository.optimizeEntireSystem()
        val config = SettingsRepository.config.value
        assertTrue(config.systemRevolutionEnabled)
        assertTrue(config.stealthHeadersEnabled)
        assertTrue(config.autoHealingProxyMesh)
        assertTrue(config.sdpBitrateBoostEnabled)
        assertTrue(config.zeroDelayDirectHandshake)
        assertTrue(config.antiJapaneseEnforcer)
        assertTrue(config.smartVpnEnabled)
        assertTrue(config.autoBypassEnabled)
        assertEquals(15, config.maxBitrateMbps)
    }

    @Test
    fun `test launch MainActivity`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @Test
    fun `test GoogleGamingTranslator tokenization preserves game titles and buttons`() {
        val input = "Press A button to play Forza Horizon 5 at 60fps on https://xbox.com with @GamerOne"
        val (tokenized, placeholderMap) = com.example.cloudplay.GoogleGamingTranslator.tokenizeProtectedContent(input)

        // Assert placeholders replaced protected terms
        assertTrue(placeholderMap.values.contains("Forza Horizon 5"))
        assertTrue(placeholderMap.values.contains("A button"))
        assertTrue(placeholderMap.values.contains("60fps"))
        assertTrue(placeholderMap.values.contains("https://xbox.com"))
        assertTrue(placeholderMap.values.contains("@GamerOne"))

        // Restoring must give back the protected terms
        val fakeTranslated = tokenized.replace("Press", "Nhấn").replace("to play", "để chơi")
        val restored = com.example.cloudplay.GoogleGamingTranslator.restoreProtectedContent(fakeTranslated, placeholderMap)
        assertTrue(restored.contains("Forza Horizon 5"))
        assertTrue(restored.contains("A button"))
        assertTrue(restored.contains("60fps"))
        assertTrue(restored.contains("https://xbox.com"))
        assertTrue(restored.contains("@GamerOne"))
        assertTrue(restored.contains("Nhấn"))
    }

    @Test
    fun `test isTrustedHost security and domain whitelist`() {
        // Trusted Xbox and Microsoft domains
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://www.xbox.com/play"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://xbox.com/en-US/play"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://login.live.com/oauth20_authorize.srf"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://account.microsoft.com"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://login.microsoftonline.com/common/oauth2"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://auth.msauth.net"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://client.xboxservices.com/status"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://api.xboxlive.com"))
        assertTrue(com.example.cloudplay.XboxBridge.isTrustedHost("https://sub.live.net"))

        // Untrusted / non-https / external domains
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost("http://www.xbox.com/play")) // Insecure http rejected
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost("https://google.com"))
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost("https://facebook.com"))
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost("https://evil-xbox.com.attacker.com"))
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost("https://fake-microsoft.com"))
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost(null))
        org.junit.Assert.assertFalse(com.example.cloudplay.XboxBridge.isTrustedHost(""))
    }

    @Test
    fun `verify server sorting orders measured servers before unmeasured without geographic bias`() {
        val servers = VpnManager.getAllServers().take(3)
        // Without measured pings, default list order is preserved
        val unmeasuredSorted = VpnManager.sortServersByUnlockAndPing(
            servers = servers,
            pingMap = emptyMap(),
            records = emptyMap(),
            engine = com.example.vpn.VpnEngine.PROXY_TUNNEL,
            sessions = emptyList()
        )
        assertEquals(servers.map { it.id }, unmeasuredSorted.map { it.id })

        // With measured pings, server with lower real ping is ranked first
        val measuredPings = mapOf(
            servers[0].id to -1,
            servers[1].id to 120,
            servers[2].id to 45
        )
        val measuredSorted = VpnManager.sortServersByUnlockAndPing(
            servers = servers,
            pingMap = measuredPings,
            records = emptyMap(),
            engine = com.example.vpn.VpnEngine.PROXY_TUNNEL,
            sessions = emptyList()
        )
        assertEquals(servers[2].id, measuredSorted[0].id)
        assertEquals(servers[1].id, measuredSorted[1].id)
        assertEquals(servers[0].id, measuredSorted[2].id)
    }
}

