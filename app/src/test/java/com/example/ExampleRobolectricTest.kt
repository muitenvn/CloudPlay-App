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

    @Test
    fun `verify nearest regional probe selection for HCMC Hanoi and neighboring countries`() {
        val hcmGeo = com.example.vpn.GeoIpResult(
            ip = "113.161.72.10",
            countryCode = "VN",
            countryName = "Việt Nam",
            city = "Ho Chi Minh City",
            flagEmoji = "🇻🇳",
            isXboxSupported = false,
            latitude = 10.8231,
            longitude = 106.6297
        )
        val hcmProbes = com.example.vpn.RegionalProbeTester.selectOrderedProbes(hcmGeo)
        assertEquals("vn_hcm", hcmProbes.first().id)

        val hanoiGeo = com.example.vpn.GeoIpResult(
            ip = "14.225.204.10",
            countryCode = "VN",
            countryName = "Việt Nam",
            city = "Hanoi",
            flagEmoji = "🇻🇳",
            isXboxSupported = false,
            latitude = 21.0285,
            longitude = 105.8542
        )
        val hanoiProbes = com.example.vpn.RegionalProbeTester.selectOrderedProbes(hanoiGeo)
        assertEquals("vn_han", hanoiProbes.first().id)

        // Small country without domestic probe (e.g. Cambodia KH) picks nearest neighbor (Ho Chi Minh City)
        val khGeo = com.example.vpn.GeoIpResult(
            ip = "203.189.150.1",
            countryCode = "KH",
            countryName = "Cambodia",
            city = "Phnom Penh",
            flagEmoji = "🌐",
            isXboxSupported = false,
            latitude = 11.5564,
            longitude = 104.9282
        )
        val khProbes = com.example.vpn.RegionalProbeTester.selectOrderedProbes(khGeo)
        assertEquals("vn_hcm", khProbes.first().id)
    }

    @Test
    fun `verify measured server ping takes precedence in SMART_BYPASS and AppLogger records events`() {
        com.example.data.AppLogger.clear()
        com.example.data.AppLogger.info("PROBE", "Test probe log entry")
        assertTrue(com.example.data.AppLogger.logs.value.any { it.message.contains("Test probe log entry") })
        assertTrue(com.example.data.AppLogger.exportAsText().contains("[PROBE] Test probe log entry"))

        val server = VpnManager.getAllServers().first()
        val pingMap = mapOf(server.id to 64)
        val effPing = VpnManager.getEffectiveServerPing(
            server = server,
            engine = com.example.vpn.VpnEngine.SMART_BYPASS,
            pingMap = pingMap,
            sessions = emptyList()
        )
        assertEquals(64, effPing)
    }

    @Test
    fun `verify two stage nearest xbox country selection and physical ping calibration`() {
        val vnGeo = com.example.vpn.GeoIpResult(
            ip = "113.161.72.10",
            countryCode = "VN",
            countryName = "Việt Nam",
            city = "Ho Chi Minh City",
            flagEmoji = "🇻🇳",
            isXboxSupported = false,
            latitude = 10.8231,
            longitude = 106.6297
        )
        VpnManager.updateSmartDecision(vnGeo)

        val allServers = VpnManager.getAllServers()
        val jpServer = allServers.first { it.countryCode == "JP" }
        val krServer = allServers.first { it.countryCode == "KR" }
        val usServer = allServers.first { it.countryCode == "US" }

        // Stage 1: For Vietnam user, JP and KR are Tier 0 (nearest Xbox-supported countries), while US is Tier 2 (trans-oceanic)
        val jpTier = com.example.vpn.RegionalProbeTester.getNearestSupportedCountryTier("JP", allServers, vnGeo)
        val krTier = com.example.vpn.RegionalProbeTester.getNearestSupportedCountryTier("KR", allServers, vnGeo)
        val usTier = com.example.vpn.RegionalProbeTester.getNearestSupportedCountryTier("US", allServers, vnGeo)
        assertEquals(0, jpTier)
        assertEquals(0, krTier)
        assertTrue("US must be in a distant tier from Vietnam", usTier > 0)

        // Physical distance calibration prevents fake 47ms Anycast ping on US server from Vietnam
        val calibratedUsPing = com.example.vpn.RegionalProbeTester.calibrateMeasuredRttWithPhysicalDistance(
            rawRttMs = 47,
            server = usServer,
            geo = vnGeo
        )
        assertTrue("Calibrated US ping from Vietnam must be >= 140ms (got $calibratedUsPing)", calibratedUsPing >= 140)

        // Stage 2: Sorting prioritizes nearest Xbox-supported country and lowest ping within that region
        val pingMap = mapOf(
            usServer.id to calibratedUsPing,
            jpServer.id to 88,
            krServer.id to 72
        )
        val sorted = VpnManager.sortServersByUnlockAndPing(
            servers = listOf(usServer, jpServer, krServer),
            pingMap = pingMap,
            records = emptyMap(),
            engine = com.example.vpn.VpnEngine.PROXY_TUNNEL,
            sessions = emptyList()
        )
        assertEquals("Best server in nearest supported region (KR 72ms) must be first", krServer.id, sorted[0].id)
        assertEquals("Second server in nearest supported region (JP 88ms) must be second", jpServer.id, sorted[1].id)
        assertEquals("Distant US server must be ranked last", usServer.id, sorted[2].id)
    }
}


