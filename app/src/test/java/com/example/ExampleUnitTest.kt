package com.example

import com.example.cloudplay.XboxTranslator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun xboxTranslator_hasCrucialTranslations() {
    val dict = XboxTranslator.XBOX_UI_DICTIONARY_EN
    assertEquals("Chơi ngay", dict["Play"])
    assertEquals("Bạn đang trong hàng chờ", dict["You're in the queue"])
    assertEquals("Đang khởi tạo phiên chơi đám mây...", dict["Starting cloud gaming session..."])
    assertEquals("Nhấn nút Xbox để mở bảng điều khiển", dict["Press the Xbox button to open the guide"])
    assertEquals("Yêu cầu tay cầm", dict["Controller required"])
    assertEquals("Chơi bằng cảm ứng", dict["Play with touch"])
  }

  @Test
  fun xboxTranslator_protectsMajorGameTitles() {
    val protected = XboxTranslator.PROTECTED_GAME_TITLES
    assertTrue(protected.contains("Fortnite"))
    assertTrue(protected.contains("Halo Infinite"))
    assertTrue(protected.contains("Forza Horizon"))
    assertTrue(protected.contains("Starfield"))
  }

  @Test
  fun xboxTranslator_generatesValidScript() {
    val script = XboxTranslator.getTranslatorScript(enabled = true, preserveGameTitles = true, bilingualMode = false)
    assertTrue(script.contains("__xcloudTranslatorInstalled"))
    assertTrue(script.contains("__toggleXCloudTranslation"))
    assertTrue(script.contains("CloudPlayBridge"))
  }

  @Test
  fun streamTelemetry_hasAccurateDecodeTimeAndZeroDefaults() {
    val telemetry = com.example.cloudplay.StreamTelemetry()
    assertEquals(0, telemetry.fps)
    assertEquals(0.0, telemetry.bitrateMbps, 0.001)
    assertEquals(0.0, telemetry.decodeTimeMs, 0.001)
    assertEquals(0, telemetry.rttMs)
    assertEquals(0, telemetry.jitterMs)

    val activeTelemetry = com.example.cloudplay.StreamTelemetry(
      fps = 60,
      bitrateMbps = 15.4,
      decodeTimeMs = 4.8,
      rttMs = 32,
      jitterMs = 2,
      packetsLostPercent = 0.1
    )
    assertEquals(4.8, activeTelemetry.decodeTimeMs, 0.001)
    assertEquals(15.4, activeTelemetry.bitrateMbps, 0.001)
  }

  @Test
  fun adaptiveQualityManager_hasValidLaddersForWifiAndCellular() {
    val wifi5g = com.example.cloudplay.AdaptiveQualityManager.WIFI_5GHZ_LADDER
    assertTrue(wifi5g.isNotEmpty())
    assertTrue(wifi5g.any { it.targetBitrateMbps >= 16.0 })

    val wifi24g = com.example.cloudplay.AdaptiveQualityManager.WIFI_24GHZ_LADDER
    assertTrue(wifi24g.isNotEmpty())
    assertTrue(wifi24g.all { it.targetBitrateMbps <= 14.0 })

    val cellSaver = com.example.cloudplay.AdaptiveQualityManager.CELLULAR_SAVER_LADDER
    assertTrue(cellSaver.isNotEmpty())
    assertTrue(cellSaver.all { it.targetBitrateMbps <= 8.0 })

    val cellMax = com.example.cloudplay.AdaptiveQualityManager.CELLULAR_MAX_LADDER
    assertTrue(cellMax.isNotEmpty())
    assertTrue(cellMax.any { it.targetBitrateMbps >= 20.0 })
  }

  @Test
  fun adaptiveQualityManager_evaluatesTelemetryAndAdapts() {
    val manager = com.example.cloudplay.AdaptiveQualityManager
    manager.toggleAutoAdapting(true)

    // Simulate high decode time (>16ms)
    val badDecode = com.example.cloudplay.StreamTelemetry(
      fps = 55,
      bitrateMbps = 12.0,
      decodeTimeMs = 18.5,
      rttMs = 45,
      packetsLostPercent = 0.0
    )
    manager.evaluateTelemetry(badDecode)
    manager.evaluateTelemetry(badDecode)

    val state = manager.state.value
    assertNotNull(state.qualityGrade)
    assertTrue(state.qualityNotice.isNotBlank())
  }

  @Test
  fun updateChecker_mirrorFailoverChainAndRedirectValidation() {
    val directUrl = "https://github.com/muitenvn/CloudPlay-App/releases/download/v1.3.0/CloudPlay-Xbox-v1.3.0.apk"
    val totalNodes = com.example.data.UpdateChecker.FAST_MIRROR_NODES.size
    assertEquals(11, totalNodes)

    // 1. Auto mirror chain puts gh_proxy_com first and github_direct last
    val autoChain = com.example.data.UpdateChecker.buildOrderedMirrorChain(
      directGitHubUrl = directUrl,
      preferredMirrorId = "auto",
      lastWorkingMirrorId = ""
    )
    assertEquals(11, autoChain.size)
    assertEquals("gh_proxy_com", autoChain.first().first.id)
    assertEquals("github_direct", autoChain.last().first.id)

    // 2. Normal direct download puts github_direct first and retains all 10 mirrors as fallback
    val normalChain = com.example.data.UpdateChecker.buildOrderedMirrorChain(
      directGitHubUrl = directUrl,
      preferredMirrorId = "github_direct",
      lastWorkingMirrorId = "gh_llkk_cc"
    )
    assertEquals(11, normalChain.size)
    assertEquals("github_direct", normalChain[0].first.id)
    assertEquals(directUrl, normalChain[0].second)
    assertEquals("gh_llkk_cc", normalChain[1].first.id)

    // 3. AWS S3 and Cloudflare R2 redirect URLs from GitHub/mirrors are allowed during download
    assertTrue(
      com.example.data.UpdateChecker.isAllowedHttpsUrl(
        java.net.URL("https://github-production-release-asset-2e65be.s3.amazonaws.com/12345"),
        allowGhFast = false
      )
    )
    assertTrue(
      com.example.data.UpdateChecker.isAllowedHttpsUrl(
        java.net.URL("https://cdn.gh-proxy.com/release.apk"),
        allowGhFast = true
      )
    )
  }
}

