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
}


