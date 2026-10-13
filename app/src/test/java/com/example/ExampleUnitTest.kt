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
}
