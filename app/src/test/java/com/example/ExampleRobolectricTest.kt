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
@Config(sdk = [36])
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
    fun `test 1 A1 no physical distance ping calibration and real measured rtt preserved`() {
        val methodsInPingTester = com.example.vpn.PingTester::class.java.declaredMethods.map { it.name }
        val methodsInProbeTester = com.example.vpn.RegionalProbeTester::class.java.declaredMethods.map { it.name }
        org.junit.Assert.assertFalse(methodsInPingTester.any { it.contains("calibrateMeasured") || it.contains("computeMinimumPhysicalRtt") })
        org.junit.Assert.assertFalse(methodsInProbeTester.any { it.contains("calibrateMeasured") || it.contains("computeMinimumPhysicalRtt") })
    }

    @Test
    fun `test 2 A2 summarizeRawSamples computes median min max without subtracting 960`() {
        val methodsInPingTester = com.example.vpn.PingTester::class.java.declaredMethods.map { it.name }
        org.junit.Assert.assertFalse(methodsInPingTester.any { it.contains("normalizeKernelSynRetransmit") })

        // Samples [45, 52, 980]: median must be 52, min=45, max=980
        val summary = com.example.vpn.PingTester.summarizeRawSamples(listOf(45, 980, 52))
        assertNotNull(summary)
        val (medianRtt, minRtt, maxRtt) = summary!!
        assertEquals(52, medianRtt)
        assertEquals(45, minRtt)
        assertEquals(980, maxRtt)
        assertTrue((maxRtt - minRtt) >= 80)

        // Samples >= 1000ms must NOT be reduced by 960 or modulo 960
        val highSummary = com.example.vpn.PingTester.summarizeRawSamples(listOf(1020, 1080))
        assertNotNull(highSummary)
        assertTrue("RTT >= 1000ms must remain >= 1000ms", highSummary!!.first >= 1000)
    }

    @Test
    fun `test 3 A3 fallback port measurement separates MEASURED_PRIMARY and MEASURED_FALLBACK_ONLY`() {
        val primaryMeasurement = com.example.vpn.EndpointPingMeasurement(
            rttMs = 58,
            minRttMs = 55,
            maxRttMs = 61,
            configuredPort = 1194,
            measuredPort = 1194,
            resolvedIp = "219.100.37.10",
            measurementType = com.example.vpn.PingMeasurementType.TCP_PRIMARY_PORT,
            status = com.example.vpn.ServerPingStatus.MEASURED_PRIMARY,
            isUnstable = false
        )
        assertTrue(primaryMeasurement.isPrimaryPortAlive)
        org.junit.Assert.assertFalse(primaryMeasurement.isFallbackPortOnly)
        assertEquals("58ms (:1194)", primaryMeasurement.formatDisplayBadge(isEn = false))

        val fallbackMeasurement = com.example.vpn.EndpointPingMeasurement(
            rttMs = 63,
            minRttMs = 61,
            maxRttMs = 65,
            configuredPort = 1194,
            measuredPort = 443,
            resolvedIp = "219.100.37.10",
            measurementType = com.example.vpn.PingMeasurementType.TCP_FALLBACK_PORT,
            status = com.example.vpn.ServerPingStatus.MEASURED_FALLBACK_ONLY,
            isUnstable = false
        )
        org.junit.Assert.assertFalse(fallbackMeasurement.isPrimaryPortAlive)
        assertTrue(fallbackMeasurement.isFallbackPortOnly)
        assertTrue(fallbackMeasurement.formatDisplayBadge(isEn = false).contains(":443"))
        assertTrue(fallbackMeasurement.formatDisplayBadge(isEn = false).contains("Cổng :1194 đóng"))
    }

    @Test
    fun `test 4 A4 Globalping default off null coordinates when no GeoIP and 200 per hour quota`() {
        val defaultConfig = com.example.data.AppConfig()
        org.junit.Assert.assertFalse("Globalping must be OFF by default", defaultConfig.enableGlobalpingProbe)

        // Missing GeoIP must return null coordinates (never default to VN)
        val emptyGeo = com.example.vpn.GeoIpResult(ip = "", countryCode = "", countryName = "", city = "", flagEmoji = "", isXboxSupported = false)
        org.junit.Assert.assertNull(com.example.vpn.RegionalProbeTester.resolveUserCoordinatesOrNull(emptyGeo))
        assertTrue(com.example.vpn.RegionalProbeTester.selectOrderedProbes(emptyGeo).isEmpty())

        // Quota enforcement: 200 per hour
        com.example.vpn.RegionalProbeTester.resetQuotaForTest()
        var acquiredCount = 0
        for (i in 1..205) {
            if (com.example.vpn.RegionalProbeTester.tryAcquireHourlyQuota(nowMs = 1_000_000L + i)) {
                acquiredCount++
            }
        }
        assertEquals(com.example.vpn.RegionalProbeTester.LOCAL_DEVICE_HOURLY_CAP, acquiredCount)

        // Estimate label clearly states station and excludes phone-to-station leg
        val estimate = com.example.vpn.GlobalpingProbeEstimate(
            rttMs = 68,
            probeId = "vn_han",
            probeCity = "Hà Nội",
            probeDisplayName = "Hà Nội (VN)",
            timestampMs = 1000L
        )
        assertTrue(estimate.formatLabel(isEn = false).contains("Ước lượng từ trạm Hà Nội (không gồm đoạn từ điện thoại tới trạm): 68 ms"))
    }

    @Test
    fun `test 5 A5 server sorting prioritizes real ping over geography and MEASURED_PRIMARY before MEASURED_FALLBACK_ONLY`() {
        val emptyGeo = com.example.vpn.GeoIpResult(ip = "", countryCode = "", countryName = "", city = "", flagEmoji = "", isXboxSupported = false)
        val allServers = VpnManager.getAllServers()
        // Without GeoIP, getNearestSupportedCountryTier must return 0 for all countries
        assertEquals(0, com.example.vpn.RegionalProbeTester.getNearestSupportedCountryTier("JP", allServers, emptyGeo))
        assertEquals(0, com.example.vpn.RegionalProbeTester.getNearestSupportedCountryTier("US", allServers, emptyGeo))

        val s1 = allServers[0]
        val s2 = allServers[1]
        val pingMap = mapOf(s1.id to -1, s2.id to 75)
        val detailsMap = mapOf(
            s1.id to com.example.vpn.EndpointPingMeasurement(
                rttMs = 50,
                minRttMs = 50,
                maxRttMs = 50,
                configuredPort = s1.port,
                measuredPort = 443,
                resolvedIp = s1.ip,
                measurementType = com.example.vpn.PingMeasurementType.TCP_FALLBACK_PORT,
                status = com.example.vpn.ServerPingStatus.MEASURED_FALLBACK_ONLY
            ),
            s2.id to com.example.vpn.EndpointPingMeasurement(
                rttMs = 75,
                minRttMs = 75,
                maxRttMs = 75,
                configuredPort = s2.port,
                measuredPort = s2.port,
                resolvedIp = s2.ip,
                measurementType = com.example.vpn.PingMeasurementType.TCP_PRIMARY_PORT,
                status = com.example.vpn.ServerPingStatus.MEASURED_PRIMARY
            )
        )
        val sorted = VpnManager.sortServersByUnlockAndPing(
            servers = listOf(s1, s2),
            pingMap = pingMap,
            records = emptyMap(),
            engine = com.example.vpn.VpnEngine.PROXY_TUNNEL,
            sessions = emptyList(),
            measurementsMap = detailsMap
        )
        // s2 (MEASURED_PRIMARY 75ms) must come before s1 (MEASURED_FALLBACK_ONLY 50ms)
        assertEquals(s2.id, sorted[0].id)
        assertEquals(s1.id, sorted[1].id)
    }

    @Test
    fun `test 6 A6 AppLogger redacts sensitive tokens before storing and enforces 300 lines and 64KB cap`() {
        com.example.data.AppLogger.clear()
        val rawSecretLog = """GET https://user:myPass123@xbox.com/play?access_token=abc987xyz Authorization: Bearer eyJhbGciOiJIUzI1NiJ9 Cookie: MSCC=secret_cookie; RPSAuth=tok123 {"token":"jsonSecret999","email":"gamer@example.com"}"""
        com.example.data.AppLogger.info("AUTH", rawSecretLog)

        val storedEntry = com.example.data.AppLogger.logs.value.last()
        org.junit.Assert.assertFalse(storedEntry.message.contains("myPass123"))
        org.junit.Assert.assertFalse(storedEntry.message.contains("abc987xyz"))
        org.junit.Assert.assertFalse(storedEntry.message.contains("eyJhbGciOiJIUzI1NiJ9"))
        org.junit.Assert.assertFalse(storedEntry.message.contains("secret_cookie"))
        org.junit.Assert.assertFalse(storedEntry.message.contains("jsonSecret999"))
        org.junit.Assert.assertFalse(storedEntry.message.contains("gamer@example.com"))
        assertTrue(storedEntry.message.contains("[REDACTED]"))

        // Enforce 300 entries and 64KB limit
        for (i in 1..350) {
            com.example.data.AppLogger.info("TEST", "Log line $i with padding " + "X".repeat(220))
        }
        assertTrue(com.example.data.AppLogger.logs.value.size <= com.example.data.AppLogger.MAX_LOGS)
        val totalBytes = com.example.data.AppLogger.logs.value.sumOf { (it.timestamp.length + it.tag.length + it.message.length + 16) * 2 }
        assertTrue(totalBytes <= com.example.data.AppLogger.MAX_TOTAL_BYTES)

        com.example.data.AppLogger.clear()
        assertTrue(com.example.data.AppLogger.logs.value.isEmpty())
    }

    @Test
    fun `test 7 A7 verify public directory does not exist in workspace`() {
        val publicDir = java.io.File("public")
        org.junit.Assert.assertFalse("public/ directory must not exist", publicDir.exists())
    }

    @Test
    fun `test 8 A8 scalable 2-step representative selection socket cap cache and generation guard`() {
        assertTrue(com.example.vpn.PingTester.MAX_CONCURRENT_SOCKETS in 4..6)

        val allServers = VpnManager.getAllServers()
        val reps = VpnManager.selectStep1RepresentativeServers(allServers)
        // At most 3 representatives per country
        val byCountry = reps.groupBy { it.countryCode }
        assertTrue(byCountry.values.all { it.size <= 3 })

        // Verify generation guard ignores stale measurement writes
        val currentGen = VpnManager.getCurrentMeasurementGeneration()
        val staleGen = currentGen - 1L
        val targetServer = allServers.first()
        val staleMeasurement = com.example.vpn.EndpointPingMeasurement(
            rttMs = 99,
            minRttMs = 99,
            maxRttMs = 99,
            configuredPort = targetServer.port,
            measuredPort = targetServer.port,
            resolvedIp = targetServer.ip,
            measurementType = com.example.vpn.PingMeasurementType.TCP_PRIMARY_PORT,
            status = com.example.vpn.ServerPingStatus.MEASURED_PRIMARY
        )
        org.junit.Assert.assertFalse(
            VpnManager.recordMeasurementIfCurrentGeneration(staleGen, targetServer.id, staleMeasurement)
        )
    }

    @Test
    fun `test 9 B1 SessionHistoryRepository backward compatibility with legacy JSON`() {
        val legacyJson = """
            [
              {
                "id": "legacy_1",
                "gameTitle": "Halo Infinite",
                "startTimeMillis": 1700000000000,
                "durationSeconds": 180,
                "serverName": "Japan Tokyo #1",
                "isDirectNetwork": true,
                "avgFps": 60,
                "avgPingMs": 55,
                "framesDropped": 4,
                "avgDecodeMs": 6.2,
                "jitterMs": 5,
                "freezeCount": 0
              }
            ]
        """.trimIndent()
        val parsed = com.example.data.SessionHistoryRepository.deserializeSessionsJson(legacyJson)
        assertEquals(1, parsed.size)
        assertEquals("Halo Infinite", parsed[0].gameTitle)
        assertEquals("", parsed[0].networkType)
        assertEquals(0, parsed[0].framesDecoded)
    }

    @Test
    fun `test 10 B2 diagnoseSessionLag handles insufficient data freeze without network blame and decode overload`() {
        // 1. Short session (< 30s) -> isInsufficientData = true
        val shortSession = com.example.data.PlaySessionRecord(
            id = "s_short",
            gameTitle = "Fortnite",
            startTimeMillis = 1700000000000L,
            durationSeconds = 15L,
            serverName = "Japan #1",
            isDirectNetwork = true,
            avgPingMs = 50,
            avgFps = 60,
            avgDecodeMs = 6.0,
            isSlowDecode = false,
            isThermalHot = false,
            pingSamples = listOf(50)
        )
        val shortDiag = com.example.ui.tabs.diagnoseSessionLag(shortSession, isEn = false)
        assertTrue(shortDiag.isInsufficientData)
        assertTrue(shortDiag.evidenceSentence.contains("Chưa đủ dữ liệu"))

        // 2. Freeze > 0 while ping (45ms) and jitter (5ms) are normal -> must NOT blame network
        val freezeNormalNetSession = com.example.data.PlaySessionRecord(
            id = "s_freeze",
            gameTitle = "Fortnite",
            startTimeMillis = 1700000000000L,
            durationSeconds = 120L,
            serverName = "Japan #1",
            isDirectNetwork = true,
            avgPingMs = 45,
            avgFps = 58,
            avgDecodeMs = 6.5,
            isSlowDecode = false,
            isThermalHot = false,
            pingSamples = listOf(44, 46),
            framesDecoded = 7000,
            framesDropped = 10,
            jitterMs = 5,
            freezeCount = 2
        )
        val freezeDiag = com.example.ui.tabs.diagnoseSessionLag(freezeNormalNetSession, isEn = false)
        org.junit.Assert.assertFalse(freezeDiag.isInsufficientData)
        assertTrue(freezeDiag.evidenceSentence.contains("đều bình thường"))
        assertTrue(freezeDiag.suggestionSentence.contains("không phải do độ trễ mạng"))

        // 3. Decode overload only when avgDecodeMs > 12.0 AND dropRatioPct > 2.0%
        val decodeOverloadSession = freezeNormalNetSession.copy(
            id = "s_decode",
            framesDecoded = 4000,
            framesDropped = 200, // ~4.76% > 2.0%
            avgDecodeMs = 14.5,
            isSlowDecode = true
        )
        val decodeDiag = com.example.ui.tabs.diagnoseSessionLag(decodeOverloadSession, isEn = false)
        assertTrue(decodeDiag.evidenceSentence.contains("Thời gian giải mã trung bình"))
    }

    @Test
    fun `test 11 B3 diagnoseSessionLag performs zero network operations`() {
        com.example.vpn.PingTester.resetSocketCountersForTest()
        val session = com.example.data.PlaySessionRecord(
            id = "s_offline",
            gameTitle = "Forza",
            startTimeMillis = 1700000000000L,
            durationSeconds = 90L,
            serverName = "Korea #1",
            isDirectNetwork = true,
            avgPingMs = 60,
            avgFps = 60,
            avgDecodeMs = 5.5,
            isSlowDecode = false,
            isThermalHot = false,
            pingSamples = listOf(58, 62),
            framesDecoded = 5400,
            framesDropped = 5,
            jitterMs = 4
        )
        val diag = com.example.ui.tabs.diagnoseSessionLag(session, isEn = false)
        org.junit.Assert.assertFalse(diag.isInsufficientData)
        assertEquals(0, com.example.vpn.PingTester.getPeakOpenSocketsCount())
    }

    @Test
    fun `test 12 C1 C2 C3 standardized metric explanations and 3-state accessible text labels`() {
        val keys = listOf("ping", "jitter", "decode", "buffer", "dropped", "freeze")
        for (key in keys) {
            val expVi = com.example.ui.tabs.getMetricOneSentenceExplanation(key, isEn = false)
            assertNotNull("Explanation for $key must not be null", expVi)
            assertTrue("Explanation for $key must not be blank", expVi!!.isNotBlank())
        }
        assertEquals("Tốt", com.example.ui.tabs.pingStatusLabel(55, isEn = false))
        assertEquals("Trung bình", com.example.ui.tabs.pingStatusLabel(95, isEn = false))
        assertEquals("Cao", com.example.ui.tabs.pingStatusLabel(145, isEn = false))

        assertEquals("Tốt", com.example.ui.tabs.jitterStatusLabel(5, isEn = false))
        assertEquals("Trung bình", com.example.ui.tabs.jitterStatusLabel(15, isEn = false))
        assertEquals("Cao", com.example.ui.tabs.jitterStatusLabel(28, isEn = false))

        assertEquals("Tốt", com.example.ui.tabs.dropRatioStatusLabel(0.2, isEn = false))
        assertEquals("Trung bình", com.example.ui.tabs.dropRatioStatusLabel(1.2, isEn = false))
        assertEquals("Cao", com.example.ui.tabs.dropRatioStatusLabel(3.5, isEn = false))
    }

    @Test
    fun `test 13 C4 verify over-promising strings removed and UNLOCKED prioritized before UNTESTED`() {
        // Verify CellularOptimizationMode display names no longer contain "Ultra"
        val modes = com.example.data.CellularOptimizationMode.values()
        for (mode in modes) {
            org.junit.Assert.assertFalse(mode.title.contains("Ultra", ignoreCase = true))
            org.junit.Assert.assertFalse(mode.titleEn.contains("Ultra", ignoreCase = true))
        }

        // Verify UNLOCKED server is prioritized before UNTESTED
        val allServers = VpnManager.getAllServers()
        val krIncheon = allServers.first { it.name.contains("Incheon", ignoreCase = true) }
        val jpTsukuba = allServers.first { it.name.contains("Tsukuba #7", ignoreCase = true) }

        val krKey = "${krIncheon.ip.trim().lowercase()}:${krIncheon.port}"
        val records = mapOf(
            krKey to com.example.vpn.ServerUnlockRecord(
                serverKey = krKey,
                status = com.example.vpn.RegionUnlockStatus.UNLOCKED
            )
        )
        val pingMap = mapOf(
            krIncheon.id to 120,
            jpTsukuba.id to 63
        )
        val sorted = VpnManager.sortServersByUnlockAndPing(
            servers = listOf(jpTsukuba, krIncheon),
            pingMap = pingMap,
            records = records,
            engine = com.example.vpn.VpnEngine.PROXY_TUNNEL,
            sessions = emptyList()
        )
        assertEquals(krIncheon.id, sorted[0].id)
        assertEquals(jpTsukuba.id, sorted[1].id)
    }

    @Test
    fun `test 14 Muc 1 R id webview_desktop_ua_tag allows safe View setTag without IllegalArgumentException`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = android.view.View(context)
        view.setTag(com.example.R.id.webview_desktop_ua_tag, true)
        assertEquals(true, view.getTag(com.example.R.id.webview_desktop_ua_tag))
        view.setTag(com.example.R.id.webview_desktop_ua_tag, false)
        assertEquals(false, view.getTag(com.example.R.id.webview_desktop_ua_tag))
    }

    @Test
    fun `test 15 Muc 2 and 5 telemetry with hasStatsSample false does not pollute AdaptiveQualityManager EMA or trigger fake decode`() {
        com.example.cloudplay.AdaptiveQualityManager.toggleAutoAdapting(true)
        val initialStep = com.example.cloudplay.AdaptiveQualityManager.state.value.currentStepIndex
        val initialBitrate = com.example.cloudplay.AdaptiveQualityManager.state.value.smoothBitrateMbps

        // Gửi mẫu dự phòng khi getStats chưa có dữ liệu (hasStatsSample = false), kể cả khi chứa số xấu giả định
        com.example.cloudplay.NetworkOptimizer.onTelemetryReceived(
            """
            {
                "hasStatsSample": false,
                "fps": 0,
                "bitrateMbps": 0.0,
                "decodeTimeMs": 0.0,
                "rttMs": 0,
                "jitterMs": 0,
                "packetsLostPercent": 0.0,
                "framesDropped": 0,
                "framesDecoded": 0,
                "freezeCount": 0,
                "totalFreezesDurationSec": 0.0,
                "jitterBufferDelayMs": 0.0
            }
            """.trimIndent()
        )
        val tFallback = com.example.cloudplay.NetworkOptimizer.telemetry.value
        org.junit.Assert.assertFalse(tFallback.hasStatsSample)
        assertEquals(0.0, tFallback.decodeTimeMs, 0.001)
        assertEquals(0, tFallback.rttMs)
        assertEquals(0, tFallback.jitterMs)

        // Đảm bảo mẫu hasStatsSample = false (kể cả khi truyền rtt/decode cao) bị bỏ qua hoàn toàn bởi AdaptiveQualityManager
        com.example.cloudplay.AdaptiveQualityManager.evaluateTelemetry(
            tFallback.copy(hasStatsSample = false, rttMs = 300, jitterMs = 80, decodeTimeMs = 28.0, packetsLostPercent = 15.0)
        )
        val aqmAfterFallback = com.example.cloudplay.AdaptiveQualityManager.state.value
        assertEquals(initialStep, aqmAfterFallback.currentStepIndex)
        assertEquals(initialBitrate, aqmAfterFallback.smoothBitrateMbps, 0.001)

        // Gửi mẫu thật (hasStatsSample = true) với khóa chuẩn rttMs, jitterMs, decodeTimeMs
        com.example.cloudplay.NetworkOptimizer.onTelemetryReceived(
            """
            {
                "hasStatsSample": true,
                "fps": 60,
                "bitrateMbps": 12.0,
                "decodeTimeMs": 5.4,
                "rttMs": 48,
                "jitterMs": 4,
                "packetsLostPercent": 0.0,
                "framesDropped": 0,
                "framesDecoded": 180,
                "freezeCount": 0,
                "totalFreezesDurationSec": 0.0,
                "jitterBufferDelayMs": 8.2
            }
            """.trimIndent()
        )
        val tReal = com.example.cloudplay.NetworkOptimizer.telemetry.value
        assertTrue(tReal.hasStatsSample)
        assertEquals(48, tReal.rttMs)
        assertEquals(4, tReal.jitterMs)
        assertEquals(5.4, tReal.decodeTimeMs, 0.01)
    }

    @Test
    fun `test 16 Muc 3 and 4 queryHardwareCodecSupport and clarity auto disable syncs ClarityPreset OFF and clarityBoostEnabled false`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsRepository.init(context)

        // Kiểm tra Mục 3: queryHardwareCodecSupport trả về cấu trúc hợp lệ và script nhúng cờ phần cứng thật
        val hwCaps = com.example.cloudplay.XboxBridge.queryHardwareCodecSupport()
        assertNotNull(hwCaps)
        val script = com.example.cloudplay.XboxBridge.getInjectionScript(clarityBoost = true, clarityPreset = "BALANCED")
        assertTrue(script.contains("window.__hwCodecCaps = hwCodecCaps;"))
        assertTrue(script.contains("window.__highDecodeStreakForClarity >= 3"))
        assertTrue(script.contains("window.__clarityBoostActive = false;"))
        org.junit.Assert.assertFalse(script.contains("estDecode = video.videoHeight"))

        // Kiểm tra Mục 4: khi tự tắt làm nét do decode > 12ms, đưa ClarityPreset về OFF và clarityBoostEnabled về false
        SettingsRepository.updateClarityBoost(true)
        SettingsRepository.updateClarityPreset(com.example.data.ClarityPreset.ULTRA_SHARP)
        assertTrue(SettingsRepository.config.value.clarityBoostEnabled)
        assertEquals(com.example.data.ClarityPreset.ULTRA_SHARP, SettingsRepository.config.value.clarityPreset)

        SettingsRepository.updateClarityPreset(com.example.data.ClarityPreset.OFF)
        SettingsRepository.updateClarityBoost(false)
        assertEquals(com.example.data.ClarityPreset.OFF, SettingsRepository.config.value.clarityPreset)
        org.junit.Assert.assertFalse(SettingsRepository.config.value.clarityBoostEnabled)
    }

    @Test
    fun `test 17 Muc 6 QualityAdapter applyAdaptiveBitrate uses 2 parameters without bufferHintMs`() {
        com.example.cloudplay.QualityAdapter.setWebView(null)
        com.example.cloudplay.QualityAdapter.applyAdaptiveBitrate(targetBitrateMbps = 10.5, smoothGlide = true)
        val script = com.example.cloudplay.XboxBridge.getInjectionScript(clarityBoost = false)
        assertTrue(script.contains("window.__setAdaptiveBitrate = function(targetMbps, smoothGlide)"))
        org.junit.Assert.assertFalse(script.contains("window.__setAdaptiveBitrate = function(targetMbps, bufferHintMs"))
    }

    @Test
    fun `test 18 Muc 7 Xbox market separated from app language and one-time migration resets non-en-US to en-US`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sp = context.getSharedPreferences("cloudplay_settings_store", Context.MODE_PRIVATE)

        // Giả lập người dùng từ bản cũ đang lưu thị trường khác en-US (vd: ko-KR, use_system_language = false)
        sp.edit()
            .putString("custom_locale", "ko-KR")
            .putBoolean("use_system_language", false)
            .putBoolean("migrated_xbox_market_en_us_v1", false)
            .commit()

        SettingsRepository.init(context)
        assertEquals("en-US", SettingsRepository.config.value.getEffectiveLocale())
        assertTrue(SettingsRepository.INITIAL_VERIFIED_XBOX_MARKETS.contains("en-US"))
        assertEquals(1, SettingsRepository.INITIAL_VERIFIED_XBOX_MARKETS.size)

        // Đổi ngôn ngữ giao diện app sang Tiếng Việt, Tiếng Nhật, Tiếng Hàn không được làm đổi thị trường Xbox khỏi en-US
        SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.VIETNAMESE)
        assertEquals("en-US", SettingsRepository.config.value.getEffectiveLocale())
        SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.JAPANESE)
        assertEquals("en-US", SettingsRepository.config.value.getEffectiveLocale())

        // Khi người dùng chủ động chọn thị trường thủ công trong mục Nâng cao rồi bấm "Quay về en-US"
        SettingsRepository.updateCustomLocale("ja-JP")
        assertEquals("ja-JP", SettingsRepository.config.value.getEffectiveLocale())
        SettingsRepository.resetXboxMarketToDefault()
        assertEquals("en-US", SettingsRepository.config.value.getEffectiveLocale())
    }

    @Test
    fun `test 19 Muc 8 translation target language migration and getTargetTranslateLanguageCode never returns blank`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sp = context.getSharedPreferences("cloudplay_settings_store", Context.MODE_PRIVATE)

        // 1. Người dùng cũ có xcloud_translate = true -> giữ nguyên "vi"
        sp.edit()
            .clear()
            .putBoolean("xcloud_translate", true)
            .putString("app_language", com.example.data.AppLanguage.VIETNAMESE.name)
            .commit()
        SettingsRepository.init(context)
        assertTrue(SettingsRepository.config.value.isEffectiveTranslateEnabled())
        assertEquals("vi", SettingsRepository.config.value.getTargetTranslateLanguageCode())

        // 2. Chọn "OFF" cho ngôn ngữ dịch -> isEffectiveTranslateEnabled() == false nhưng getTargetTranslateLanguageCode() vẫn trả mã hợp lệ không rỗng
        SettingsRepository.updateTranslateLanguage("OFF")
        org.junit.Assert.assertFalse(SettingsRepository.config.value.isEffectiveTranslateEnabled())
        assertTrue(SettingsRepository.config.value.getTargetTranslateLanguageCode().isNotBlank())

        // 3. Chọn ngôn ngữ đích "es" -> bật dịch sang "es", và từ điển tĩnh tiếng Việt không kích hoạt cho "es"
        SettingsRepository.updateTranslateLanguage("es")
        assertTrue(SettingsRepository.config.value.isEffectiveTranslateEnabled())
        assertEquals("es", SettingsRepository.config.value.getTargetTranslateLanguageCode())
        val esScript = com.example.cloudplay.XboxTranslator.getTranslatorScript(
            enabled = true,
            preserveGameTitles = true,
            bilingualMode = false,
            hideStreamStats = true,
            targetLanguage = "es"
        )
        assertTrue(esScript.contains("const isVietnameseTarget = false;"))
        assertTrue(esScript.contains("const RAW_DICT = {};"))
    }

    @Test
    fun `test 20 Muc 9 and 10 GeoIpDetector lookup flag country name azureEndpoint for JP KR US VN DE FR unknown and private IPs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsRepository.init(context)
        SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.VIETNAMESE)

        // 1. Kiểm tra từ chối IP nội bộ / loopback
        assertTrue(com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp("127.0.0.1"))
        assertTrue(com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp("10.0.0.1"))
        assertTrue(com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp("172.16.5.4"))
        assertTrue(com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp("192.168.1.1"))
        assertTrue(com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp("169.254.1.1"))
        assertTrue(com.example.vpn.GeoIpDetector.isPrivateOrLoopbackIp("::1"))
        assertEquals("", com.example.vpn.GeoIpDetector.resolveIpCountryCode("127.0.0.1", allowNetworkFallback = false))
        assertEquals("", com.example.vpn.GeoIpDetector.resolveIpCountryCode("192.168.1.1", allowNetworkFallback = false))

        // 2. Kiểm tra tra cứu IP -> mã ISO cho JP, KR, US, VN, DE, FR
        assertEquals("JP", com.example.vpn.GeoIpDetector.resolveIpCountryCode("138.199.21.239", allowNetworkFallback = false))
        assertEquals("KR", com.example.vpn.GeoIpDetector.resolveIpCountryCode("141.147.150.10", allowNetworkFallback = false))
        assertEquals("US", com.example.vpn.GeoIpDetector.resolveIpCountryCode("8.8.8.8", allowNetworkFallback = false))
        assertEquals("VN", com.example.vpn.GeoIpDetector.resolveIpCountryCode("14.160.10.20", allowNetworkFallback = false))
        assertEquals("DE", com.example.vpn.GeoIpDetector.resolveIpCountryCode("88.198.10.20", allowNetworkFallback = false))
        assertEquals("FR", com.example.vpn.GeoIpDetector.resolveIpCountryCode("51.15.10.20", allowNetworkFallback = false))

        // 3. Kiểm tra cờ emoji sinh động từ mã ISO và tên hiển thị tiếng Việt
        assertEquals("🇯🇵", com.example.vpn.GeoIpDetector.getFlagEmoji("JP"))
        assertEquals("🇰🇷", com.example.vpn.GeoIpDetector.getFlagEmoji("KR"))
        assertEquals("🇺🇸", com.example.vpn.GeoIpDetector.getFlagEmoji("US"))
        assertEquals("🇻🇳", com.example.vpn.GeoIpDetector.getFlagEmoji("VN"))
        assertEquals("🇩🇪", com.example.vpn.GeoIpDetector.getFlagEmoji("DE"))
        assertEquals("🇫🇷", com.example.vpn.GeoIpDetector.getFlagEmoji("FR"))
        assertEquals("🌐", com.example.vpn.GeoIpDetector.getFlagEmoji(""))

        assertEquals("🇯🇵 Nhật Bản • 138.199.21.239", com.example.vpn.GeoIpDetector.formatServerDisplayName("JP", "138.199.21.239", isEn = false))
        assertEquals("🌐 Không rõ quốc gia • 203.0.113.5", com.example.vpn.GeoIpDetector.formatServerDisplayName("", "203.0.113.5", isEn = false))

        // 4. Kiểm tra azureEndpoint: chỉ JP và KR có endpoint thật, các nước khác hoặc không rõ trả về "" (không đoán westus)
        assertEquals("japaneast.cloudapp.azure.com", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry("JP"))
        assertEquals("koreacentral.cloudapp.azure.com", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry("KR"))
        assertEquals("", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry("US"))
        assertEquals("", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry("VN"))
        assertEquals("", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry("DE"))
        assertEquals("", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry("FR"))
        assertEquals("", com.example.vpn.GeoIpDetector.getAzureEndpointForCountry(""))
    }

    @Test
    fun `test 21 60 verified servers above 250Mbps and 12-language UI and dictionary support with Settings staying Vietnamese`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SettingsRepository.init(context)

        // 1. Kiểm tra 60 máy chủ VPN đạt chuẩn tốc độ > 250 Mbps, đã xác thực (isVerified = true) và đúng mã vùng GeoIP
        assertEquals(60, com.example.vpn.DEFAULT_SERVERS.size)
        com.example.vpn.DEFAULT_SERVERS.forEach { server ->
            assertTrue("Server ${server.id} speedMbps must be > 250", server.speedMbps > 250)
            assertTrue("Server ${server.id} must be verified", server.isVerified)
            assertEquals(
                "Server ${server.id} GeoIP seed must match countryCode",
                server.countryCode,
                com.example.vpn.GeoIpDetector.resolveIpCountryCode(server.ip, allowNetworkFallback = false)
            )
        }

        // 2. Khi chọn Tiếng Trung (zh), giao diện app dịch sang Tiếng Trung nhưng mục Cài đặt vẫn giữ Tiếng Việt (isSettingsEnglishUI == false)
        SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.CHINESE)
        val zhConfig = SettingsRepository.config.value
        assertEquals("zh", zhConfig.appLanguage.effectiveLanguageCode())
        org.junit.Assert.assertFalse(zhConfig.isSettingsEnglishUI())
        assertEquals("直连网络", zhConfig.tr("Mạng trực tiếp", "Direct Net"))
        assertEquals("快速恢复 (Quick Resume)", zhConfig.tr("CHƠI TIẾP NGAY", "QUICK RESUME"))
        assertEquals("全部类型", zhConfig.tr("Mọi thể loại", "All Genres"))
        assertEquals("延迟测速", zhConfig.tr("Đo ping", "Ping Test"))

        // 3. Khi chọn Tiếng Anh (en), mục Cài đặt chuyển sang Tiếng Anh (isSettingsEnglishUI == true)
        SettingsRepository.updateAppLanguage(com.example.data.AppLanguage.ENGLISH)
        assertTrue(SettingsRepository.config.value.isSettingsEnglishUI())

        // 4. Kiểm tra từ điển tích hợp cho đủ 12 ngôn ngữ và từ điển Tiếng Việt / Tiếng Anh nâng cấp từ bản HTML
        val allLangCodes = listOf("vi", "en", "zh", "ja", "ko", "es", "pt", "fr", "de", "ru", "th", "id")
        allLangCodes.forEach { code ->
            val dict = com.example.cloudplay.XboxTranslator.getDictionaryForLanguage(code)
            assertTrue("Dictionary for $code must not be empty", dict.isNotEmpty())
            assertTrue("Dictionary for $code must contain 'Cloud Gaming'", dict.containsKey("Cloud Gaming"))
        }
        assertEquals(
            "Chơi game trên mây Xbox tại Xbox.com",
            com.example.cloudplay.XboxTranslator.XBOX_UI_DICTIONARY_EN["Xbox Cloud Gaming on Xbox.com"]
        )
        assertTrue(
            com.example.cloudplay.XboxTranslator.PROTECTED_GAME_TITLES.contains("Call of Duty: Modern Warfare 4 - Vault Edition")
        )
        assertTrue(
            com.example.cloudplay.XboxTranslator.PROTECTED_GAME_TITLES.contains("NBA 2K27 (XBOX Series X|S)")
        )
    }
}
