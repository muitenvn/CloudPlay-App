# Kế Hoạch Triển Khai Chi Tiết (Đối Chiếu Trực Tiếp Codebase Thực Tế)

Bản kế hoạch này giữ nguyên toàn bộ các phần đã đạt yêu cầu và cập nhật chính xác **5 điểm** theo kết quả đối chiếu trực tiếp với mã nguồn thực tế trong dự án.

- **Nguyên tắc thực thi**:
  1. Không đổi tên khóa JSON telemetry hiện có; chỉ thêm khóa `hasStatsSample`.
  2. Dùng đúng 100% tên file, lớp, hàm, enum, biến có thật trong codebase (đã chạy `grep` và dán kết quả bên dưới).
  3. Không rút gọn hay viết lại giao diện/file ngoài phạm vi từng mục.
  4. Ở lượt thực thi code: chỉ viết thêm test vào `ExampleRobolectricTest.kt`, **chưa chạy build/test**.

---

## Bằng Chứng Đối Chiếu Tên Thật Trong Codebase (Kết Quả `grep` & `wc -l` Thực Tế)

### 1. Kết quả `grep -rnI` các hàm và biểu tượng thật trong `app/src/main/java/`:
```text
app/src/main/java/com/example/data/AppSettings.kt:37:enum class ClarityPreset(
app/src/main/java/com/example/data/AppSettings.kt:43:    OFF("Tắt", "Hình ảnh nguyên bản không can thiệp", "Off", "Original unmodified video stream"),
app/src/main/java/com/example/data/AppSettings.kt:44:    BALANCED("Cân Bằng", "Tăng nét nhẹ + 8% tương phản (Khuyên dùng)", "Balanced", "Gentle sharpening + 8% contrast (Recommended)"),
app/src/main/java/com/example/data/AppSettings.kt:45:    ULTRA_SHARP("Sắc Nét Cao", "Làm nét cạnh tối đa cho game 3D/FPS", "High Sharpness", "High edge clarity for 3D/FPS games"),
app/src/main/java/com/example/data/AppSettings.kt:46:    OLED_PUNCH("OLED Punch", "Màu sắc rực rỡ & độ sâu màu cho màn AMOLED/OLED", "OLED Punch", "Vivid colors & deep contrast for AMOLED/OLED");
app/src/main/java/com/example/data/AppSettings.kt:581:    val clarityBoostEnabled: Boolean = false,
app/src/main/java/com/example/data/AppSettings.kt:582:    val clarityPreset: ClarityPreset = ClarityPreset.BALANCED,
app/src/main/java/com/example/data/AppSettings.kt:636:    val targetRegionUrl: String
app/src/main/java/com/example/data/AppSettings.kt:639:    fun getEffectiveLocale(): String {
app/src/main/java/com/example/data/AppSettings.kt:655:    fun getTargetTranslateLanguageCode(): String {
app/src/main/java/com/example/data/AppSettings.kt:659:    fun isEffectiveTranslateEnabled(): Boolean {
app/src/main/java/com/example/data/AppSettings.kt:669:    fun init(context: Context) {
app/src/main/java/com/example/data/AppSettings.kt:962:    fun formatXboxUrlWithLocale(url: String, targetLocale: String): String {
app/src/main/java/com/example/data/AppSettings.kt:984:    fun getUrlForCountry(countryCode: String): String {
app/src/main/java/com/example/data/AppSettings.kt:989:    fun adaptPlayUrl(url: String, countryCode: String? = null): String {
app/src/main/java/com/example/data/AppSettings.kt:999:    fun updateAppLanguage(language: AppLanguage) {
app/src/main/java/com/example/data/AppSettings.kt:1015:    fun updateCustomLocale(locale: String) {
app/src/main/java/com/example/data/AppSettings.kt:1046:    fun updateClarityBoost(enabled: Boolean) {
app/src/main/java/com/example/data/AppSettings.kt:1051:    fun updateClarityPreset(preset: ClarityPreset) {
app/src/main/java/com/example/data/AppSettings.kt:1106:    fun updateXcloudTranslate(enabled: Boolean) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:129:        fun getInjectionScript(
app/src/main/java/com/example/cloudplay/XboxBridge.kt:153:            val translatorScript = XboxTranslator.getTranslatorScript(
app/src/main/java/com/example/cloudplay/XboxBridge.kt:232:                    window.__setAdaptiveBitrate = function(targetMbps, bufferHintMs, smoothGlide) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:252:                    function notify(method, arg) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:260:                            if (window.CloudPlayBridge && window.CloudPlayBridge[method]) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:802:                                    if (decodeTimeMs > 12.0 && window.__clarityBoostActive && window.__setClarityPreset) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:803:                                        window.__setClarityPreset('OFF');
app/src/main/java/com/example/cloudplay/XboxBridge.kt:833:                                        notify('onStreamTelemetry', JSON.stringify(payloadObj));
app/src/main/java/com/example/cloudplay/XboxBridge.kt:1263:                                        notify('onStreamTelemetry', JSON.stringify({
app/src/main/java/com/example/cloudplay/XboxBridge.kt:1505:                    window.__setClarityPreset = function(preset) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:2302:    fun onStreamTelemetry(telemetryJson: String?) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:2304:        NetworkOptimizer.onTelemetryReceived(telemetryJson)
app/src/main/java/com/example/cloudplay/XboxBridge.kt:2308:    fun requestSmartTranslate(rawText: String?, callbackId: String?, isBilingual: Boolean) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:2313:            val translated = GoogleGamingTranslator.translate(rawText, "vi", isBilingual)
app/src/main/java/com/example/cloudplay/XboxBridge.kt:2325:    fun requestBatchTranslate(jsonTexts: String?, batchId: String?, isBilingual: Boolean) {
app/src/main/java/com/example/cloudplay/XboxBridge.kt:2337:                val translatedMap = GoogleGamingTranslator.translateBatch(list, "vi", isBilingual)
app/src/main/java/com/example/cloudplay/QualityAdapter.kt:30:    fun applyAdaptiveBitrate(targetBitrateMbps: Double, bufferHintMs: Int) {
app/src/main/java/com/example/cloudplay/QualityAdapter.kt:38:    fun applyAdaptiveBitrate(
app/src/main/java/com/example/cloudplay/QualityAdapter.kt:54:        val js = "if (window.__setAdaptiveBitrate) window.__setAdaptiveBitrate($targetBitrateMbps, $bufferHintMs, $smoothGlide);"
app/src/main/java/com/example/cloudplay/NetworkOptimizer.kt:434:    fun onTelemetryReceived(telemetryJson: String?) {
app/src/main/java/com/example/cloudplay/AdaptiveQualityManager.kt:290:    fun evaluateTelemetry(telemetry: StreamTelemetry) {
app/src/main/java/com/example/cloudplay/XboxTranslator.kt:450:    fun getTranslatorScript(
app/src/main/java/com/example/ui/tabs/ServersScreen.kt:966:        CustomProxyDialog(
app/src/main/java/com/example/ui/tabs/ServersScreen.kt:1298:private fun CustomProxyDialog(
```

### 2. Bảng tổng hợp trạng thái từng mục (Đã làm / Phản biện & Lý do)
| Mục | Nội dung yêu cầu | Quyết định | Lý do & Bằng chứng kỹ thuật |
| :--- | :--- | :--- | :--- |
| **Mục 1** | Thay `setTag(android.R.id.summary, ...)` ở `CloudBrowserScreen.kt:517, 659, 661` bằng ID riêng trong `res/values/ids.xml` | **Thực hiện 100%** | `View.setTag(int, Object)` trong AOSP kiểm tra `(key >>> 24) < 2` và ném `IllegalArgumentException` vì `android.R.id.summary` thuộc package `0x01`. Thay bằng `R.id.webview_desktop_ua_tag` (`0x7f...`). |
| **Mục 2 (Điểm 1)** | Bỏ `estDecode` 4.1/2.7, `vFps = 60`, `bitrateMbps` giả ở `XboxBridge.kt:1244-1274`; giữ nguyên tên khóa JSON (`rttMs`, `jitterMs`, `decodeTimeMs`, `bitrateMbps`, `framesDropped`...) và thêm `hasStatsSample: Boolean` | **Thực hiện 100% (Phản biện `jitterMs = -1` đã được chấp thuận)** | Nếu đẩy `jitterMs = -1` vào luồng chung sẽ làm sai `emaJitterMs` trong `AdaptiveQualityManager.evaluateTelemetry` (`AdaptiveQualityManager.kt:313-328`). Dùng cờ `hasStatsSample = false` khi `getStats()` chưa có mẫu để `NetworkOptimizer.onTelemetryReceived`, `AdaptiveQualityManager.evaluateTelemetry`, và `StreamController` bỏ qua mẫu chưa đo. |
| **Mục 3** | Khai báo `navigator.mediaCapabilities.decodingInfo` (`XboxBridge.kt:456-472`) theo phần cứng thật qua `MediaCodecList`; chỉ tắt VP9/AV1 khi máy không có giải mã phần cứng | **Thực hiện 100%** | Kiểm tra `MediaCodecList(MediaCodecList.REGULAR_CODECS)` cho `video/avc` (Baseline/Main/High), `video/x-vnd.on2.vp9`, `video/av01` và truyền cờ thật vào `XboxBridge.Companion.getInjectionScript`. |
| **Mục 4 (Điểm 2)** | Tự tắt làm nét ở `XboxBridge.kt:802` khi `decodeTimeMs > 12.0` cần `>= 3` mẫu liên tiếp, cập nhật trạng thái UI qua `SettingsRepository.updateClarityPreset(ClarityPreset.OFF)` + thông báo ngắn | **Thực hiện 100%** | Enum thật là `ClarityPreset { OFF, BALANCED, ULTRA_SHARP, OLED_PUNCH }` (`AppSettings.kt:37`). Gọi `notify('onClarityAutoDisabled', ...)` qua `CloudPlayBridge` -> gọi `SettingsRepository.updateClarityPreset(ClarityPreset.OFF)` (đồng thời đặt `clarityBoostEnabled = false`) và hiện Toast. |
| **Mục 5** | Đảm bảo nhịp gửi mẫu định kỳ tối đa mỗi 2 giây (`>= 2000 ms`) ở `XboxBridge.kt:808-834` khi `decodeTimeMs`/`jitterMs`/`framesDropped` vượt ngưỡng dù giá trị không đổi | **Thực hiện 100%** | Khi `decodeTimeMs > 12.0` hoặc `jitterMs > 15` đứng yên ở một số cao cố định, `hasMeaningfulDelta` hiện đang đợi tới `4500 ms`; rút xuống `>= 2000 ms` khi vượt ngưỡng để `AdaptiveQualityManager.evaluateTelemetry` nhận đủ mẫu. |
| **Mục 6 (Điểm 2)** | Xóa tham số `bufferHintMs` không còn tác dụng ở `QualityAdapter.applyAdaptiveBitrate` (`QualityAdapter.kt:30-55`), `AdaptiveQualityManager.kt`, và JS `window.__setAdaptiveBitrate` (`XboxBridge.kt:232`); sửa comment `"zero-copy"` ở `CloudBrowserScreen.kt:490-491` | **Thực hiện 100%** | `window.__setAdaptiveBitrate` ở `XboxBridge.kt:232` không còn sử dụng `bufferHintMs`. Sửa `QualityAdapter.applyAdaptiveBitrate(targetBitrateMbps: Double, smoothGlide: Boolean = true)` và comment `setLayerType(View.LAYER_TYPE_NONE, null)`. |
| **Mục 7 (Điểm 4)** | Tách `appLanguage` khỏi thị trường Xbox: (a) đổi ngôn ngữ app không đổi link Xbox; (b) mục Nâng cao ẩn cho chọn thị trường thủ công kèm cảnh báo + nút `"Quay về en-US"`; (c) whitelist ban đầu chỉ `en-US`; (d) migration 1 lần thị trường cũ khác `en-US` → `en-US`; (e) vào game thất bại sau khi đổi thị trường thì hiện gợi ý `"Quay về en-US"`, không tự đổi ngầm | **Thực hiện 100% (Bỏ giả `navigator.language`/`Accept-Language` & tự đánh dấu thị trường sang mục Đề xuất thêm)** | Thực hiện đúng các mục 7a, 7b, 7c, 7d, 7e trong `AppSettings.kt`, `SettingsScreen.kt`, `CloudBrowserScreen.kt`, `XboxBridge.kt`. |
| **Mục 8 (Điểm 3)** | Tách ngôn ngữ dịch (`xcloudTranslateEnabled` + `translateLanguage` có lựa chọn `"OFF"`/`"Tắt"`), sửa truyền cứng `"vi"` ở `XboxBridge.kt:2313, 2337`, `getTargetTranslateLanguageCode()` không trả chuỗi rỗng, chỉ dùng từ điển tĩnh tiếng Việt khi `targetLang == "vi"` | **Thực hiện 100%** | Nơi gọi kiểm tra `config.isEffectiveTranslateEnabled()` trước; khi ngôn ngữ đích khác `"vi"`, `XboxTranslator.getTranslatorScript` bỏ qua `XBOX_UI_DICTIONARY_EN/JP/KR` để dịch qua `GoogleGamingTranslator`. |
| **Mục 9 & 10 (Điểm 5 & D.1–D.7)** | HTTPS cho VPNGate, tách `http`/`socks5` trên ProxyScrape, không trả `DEFAULT_SERVERS` khi rỗng; bộ tra DB-IP Lite Country CSV + tìm nhị phân; kết quả tra thật thắng tham số nguồn; cờ emoji động; chỉ giữ `azureEndpoint` cho `JP` và `KR` (còn lại để rỗng `""`); lọc/nhóm quốc gia + tự tra trong `CustomProxyDialog`; chặn IP riêng/loopback; ghi công DB-IP | **Thực hiện 100%** | Trong `LiveProxyFetcher.kt:161-165`, chỉ giữ `"JP" -> "japaneast.cloudapp.azure.com"` và `"KR" -> "koreacentral.cloudapp.azure.com"`, các quốc gia khác để `""`. |

### 3. Bảng số dòng trước và dự kiến sau khi sửa của từng file (Đo bằng `wc -l` / `view_file`)
| File | Số dòng trước khi sửa (`view_file` / `wc -l`) | Số dòng dự kiến sau khi sửa |
| :--- | ---: | ---: |
| `/app/src/main/res/values/ids.xml` | *(Tạo mới — 0 dòng)* | ~4 dòng |
| `/app/src/main/java/com/example/ui/CloudBrowserScreen.kt` | **1.499 dòng** (`1498` dòng newline) | ~1.535 dòng |
| `/app/src/main/java/com/example/cloudplay/XboxBridge.kt` | **2.362 dòng** (`2361` dòng newline) | ~2.435 dòng |
| `/app/src/main/java/com/example/cloudplay/QualityAdapter.kt` | **64 dòng** (`63` dòng newline) | ~54 dòng |
| `/app/src/main/java/com/example/cloudplay/AdaptiveQualityManager.kt` | **578 dòng** (`577` dòng newline) | ~585 dòng |
| `/app/src/main/java/com/example/cloudplay/NetworkOptimizer.kt` | **496 dòng** (`495` dòng newline) | ~512 dòng |
| `/app/src/main/java/com/example/cloudplay/StreamController.kt` | **605 dòng** (`604` dòng newline) | ~615 dòng |
| `/app/src/main/java/com/example/cloudplay/XboxTranslator.kt` | **963 dòng** (`962` dòng newline) | ~978 dòng |
| `/app/src/main/java/com/example/ui/components/FloatingHud.kt` | **2.340 dòng** (`2339` dòng newline) | ~2.352 dòng |
| `/app/src/main/java/com/example/data/AppSettings.kt` | **1.378 dòng** (`1377` dòng newline) | ~1.465 dòng |
| `/app/src/main/java/com/example/ui/tabs/SettingsScreen.kt` | **1.581 dòng** (`1580` dòng newline) | ~1.675 dòng |
| `/app/src/main/java/com/example/vpn/GeoIpDetector.kt` | **274 dòng** (`273` dòng newline) | ~480 dòng |
| `/app/src/main/java/com/example/vpn/LiveProxyFetcher.kt` | **216 dòng** (`215` dòng newline) | ~245 dòng |
| `/app/src/main/java/com/example/vpn/VpnManager.kt` | **1.724 dòng** (`1723` dòng newline) | ~1.745 dòng |
| `/app/src/main/java/com/example/ui/tabs/ServersScreen.kt` | **1.523 dòng** (`1522` dòng newline) | ~1.605 dòng |
| `/app/src/test/java/com/example/ExampleRobolectricTest.kt` | **667 dòng** (`666` dòng newline) | ~960 dòng |

---

## Chi Tiết Sửa Đổi Từng Mục (Kèm Tên File, Hàm Thật & Dòng Code)

### Mục 1: Thay `setTag(android.R.id.summary, ...)` bằng ID riêng của ứng dụng
- **File tạo mới**: `/app/src/main/res/values/ids.xml`
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <item name="webview_desktop_ua_tag" type="id" />
  </resources>
  ```
- **File sửa**: `/app/src/main/java/com/example/ui/CloudBrowserScreen.kt`
  - Trong hàm `@Composable fun CloudBrowserScreen(...)`:
    - **Dòng 517** (trong `AndroidView` `factory`): đổi `setTag(android.R.id.summary, config.forceDesktopUserAgent)` thành `setTag(com.example.R.id.webview_desktop_ua_tag, config.forceDesktopUserAgent)`.
    - **Dòng 659 & 661** (trong `AndroidView` `update`): đổi `view.getTag(android.R.id.summary) as? Boolean` và `view.setTag(android.R.id.summary, wantDesktopUa)` thành `view.getTag(com.example.R.id.webview_desktop_ua_tag) as? Boolean` và `view.setTag(com.example.R.id.webview_desktop_ua_tag, wantDesktopUa)`.

---

### Mục 2 (Điểm 1): Xóa số đo giả trong Telemetry & dùng khóa `hasStatsSample` với đúng các khóa JSON thật
- **Các khóa JSON thật được giữ nguyên 100%**: `fps`, `bitrateMbps`, `decodeTimeMs`, `rttMs`, `jitterMs`, `packetsLostPercent`, `framesDropped`, `framesDecoded`, `freezeCount`, `totalFreezesDurationSec`, `jitterBufferDelayMs`, và chỉ bổ sung thêm khóa **`hasStatsSample`** (`true` / `false`). Tuyệt đối không đổi sang `"rtt"` hay `"jitter"`.
- **File và hàm thật sẽ sửa**:
  1. **`/app/src/main/java/com/example/cloudplay/XboxBridge.kt`** — trong `XboxBridge.Companion.getInjectionScript` (dòng 129):
     - **Tại khối `statsIntervalId = setInterval(async function() { ... }, 2000)` (dòng 620–837)**:
       - Ở dòng 819–833 (`const payloadObj = { ... }; notify('onStreamTelemetry', JSON.stringify(payloadObj));`): giữ nguyên tất cả khóa (`fps`, `bitrateMbps`, `decodeTimeMs`, `rttMs`, `jitterMs`, `packetsLostPercent`, `framesDropped`, `framesDecoded`, `freezeCount`, `totalFreezesDurationSec`, `jitterBufferDelayMs`) và thêm **`hasStatsSample: true`**. Khi chưa có khung hình giải mã mới (`framesDecoded <= lastFramesDecodedForDecode`), `decodeTimeMs` giữ nguyên `0.0`.
     - **Tại khối fallback `setInterval(function() { ... }, 2500)` (dòng 1240–1276)**:
       - Xóa bỏ các số bịa: xóa `let vFps = 60;` (khởi tạo `let vFps = 0;`, chỉ lấy `vFps` khi `video.getVideoPlaybackQuality()` có chênh lệch khung hình thực tế), xóa `const profMbps = ...`, xóa `const estDecode = video.videoHeight >= 1080 ? 4.1 : 2.7;`.
       - Gửi `notify('onStreamTelemetry', JSON.stringify({ ... }))` với đúng các khóa thật và **`hasStatsSample: false`**:
         ```javascript
         notify('onStreamTelemetry', JSON.stringify({
             hasStatsSample: false,
             fps: vFps,
             bitrateMbps: 0.0,
             decodeTimeMs: 0.0,
             rttMs: 0,
             jitterMs: 0,
             packetsLostPercent: 0.0,
             framesDropped: vDropped,
             framesDecoded: 0,
             freezeCount: 0,
             totalFreezesDurationSec: 0.0,
             jitterBufferDelayMs: 0.0
         }));
         ```
  2. **`/app/src/main/java/com/example/cloudplay/NetworkOptimizer.kt`**:
     - Trong `data class StreamTelemetry` (dòng 55–70): thêm trường `val hasStatsSample: Boolean = true`.
     - Trong `fun onTelemetryReceived(telemetryJson: String?)` (dòng 434–493):
       - Giữ nguyên việc đọc các khóa thật: `fps`, `bitrateMbps`, `decodeTimeMs`, `rttMs`, `jitterMs`, `packetsLostPercent`, `framesDropped`, `framesDecoded`, `freezeCount`, `totalFreezesDurationSec`, `jitterBufferDelayMs`.
       - Đọc thêm `val hasStatsSample = json.optBoolean("hasStatsSample", true)`.
       - Đảm bảo `val jitter = json.optInt("jitterMs", 0).coerceAtLeast(0)` và `val decodeTime = json.optDouble("decodeTimeMs", 0.0).coerceAtLeast(0.0)` để không bao giờ có số âm.
       - Chỉ cộng dồn `streamSecondsTracked` / `totalDataSavedMegabytes` và chỉ gọi `AdaptiveQualityManager.evaluateTelemetry(newTelemetry)` khi **`hasStatsSample == true`**.
  3. **`/app/src/main/java/com/example/cloudplay/AdaptiveQualityManager.kt`**:
     - Trong `fun evaluateTelemetry(telemetry: StreamTelemetry)` (dòng 290):
       - Thêm kiểm tra chặn ngay đầu hàm: `if (!telemetry.hasStatsSample) return`.
       - Nhờ đó, mẫu `hasStatsSample = false` không bao giờ đi vào `emaDecodeMs`, `emaRttMs`, `emaJitterMs`, `emaLossPercent`, `prevJitterMs` hay kích hoạt hạ bậc chất lượng.
  4. **`/app/src/main/java/com/example/cloudplay/StreamController.kt`**:
     - Trong luồng thu thập `NetworkOptimizer.telemetry.collect { tele -> ... }`: nếu `!tele.hasStatsSample`, không đưa `rttMs`, `jitterMs`, `decodeTimeMs` vào thống kê trung bình của phiên chơi (`PlaySessionRecord`).
  5. **`/app/src/main/java/com/example/ui/components/FloatingHud.kt`**:
     - Khi `!telemetry.hasStatsSample` hoặc `telemetry.decodeTimeMs <= 0.0`: hiển thị Decode là `"—"`.
     - Khi `!telemetry.hasStatsSample`: hiển thị Ping (`rttMs`), Jitter (`jitterMs`), Bitrate (`bitrateMbps`) là `"—"`.

---

### Mục 3: Khai báo `MediaCapabilities` theo phần cứng thật (`MediaCodecList`)
- **File và hàm thật sẽ sửa**: `/app/src/main/java/com/example/cloudplay/XboxBridge.kt`
  - Trong `XboxBridge.Companion`:
    - Thêm `data class HardwareCodecSupport(val h264Baseline: Boolean, val h264Main: Boolean, val h264High: Boolean, val vp9: Boolean, val av1: Boolean)` và hàm `fun queryHardwareCodecSupport(): HardwareCodecSupport` dùng `android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos` để kiểm tra decoder phần cứng thật của máy cho `video/avc` (các profile `AVCProfileBaseline`, `AVCProfileMain`, `AVCProfileHigh`), `video/x-vnd.on2.vp9`, và `video/av01`.
  - Trong `XboxBridge.Companion.getInjectionScript` (dòng 129, đoạn `navigator.mediaCapabilities.decodingInfo` ở dòng 454–473 và `prioritizeHardwareH264Codecs` ở dòng 476–509):
    - Truyền các cờ phần cứng (`hwH264Baseline`, `hwH264Main`, `hwH264High`, `hwVp9`, `hwAv1`) vào script JS.
    - Với `avc1.64` (High), `avc1.4d` (Main), `avc1.42` (Baseline): trả `smooth` và `powerEfficient` theo đúng khả năng giải mã phần cứng thật của profile đó trên máy (nếu profile không được phần cứng hỗ trợ thì để `origDecodingInfo(config)` xử lý hoặc trả `smooth: false, powerEfficient: false`).
    - Với `vp9` / `vp09` và `av01` / `av1`: chỉ trả `{ supported: false, smooth: false, powerEfficient: false }` khi máy **không** có giải mã phần cứng cho codec tương ứng (`!hwVp9` hoặc `!hwAv1`).

---

### Mục 4 (Điểm 2): Tự tắt làm nét sau >= 3 mẫu liên tiếp `decodeTimeMs > 12 ms` & cập nhật đúng `ClarityPreset.OFF`
- **File và hàm thật sẽ sửa**:
  1. **`/app/src/main/java/com/example/data/AppSettings.kt`**:
     - Enum có thật là `ClarityPreset { OFF, BALANCED, ULTRA_SHARP, OLED_PUNCH }` (dòng 37–50).
     - Trong `SettingsRepository.updateClarityPreset(preset: ClarityPreset)` (dòng 1051): khi `preset == ClarityPreset.OFF`, cập nhật cả `clarityPreset = ClarityPreset.OFF` và `clarityBoostEnabled = false`; khi `preset != ClarityPreset.OFF`, cập nhật `clarityPreset = preset`.
  2. **`/app/src/main/java/com/example/cloudplay/XboxBridge.kt`**:
     - Trong `XboxBridge.Companion.getInjectionScript` (xung quanh dòng 800–805):
       - Thêm biến đếm `let highDecodeStreakForClarity = 0;` và `let clarityAutoDisabledSent = false;`.
       - Trong `statsIntervalId` (mẫu đo thật từ `pc.getStats()`):
         ```javascript
         if (decodeTimeMs > 12.0) {
             highDecodeStreakForClarity++;
         } else if (decodeTimeMs > 0.0) {
             highDecodeStreakForClarity = 0;
         }
         if (highDecodeStreakForClarity >= 3 && window.__clarityBoostActive && window.__setClarityPreset) {
             window.__clarityBoostActive = false;
             window.__setClarityPreset('OFF');
             if (!clarityAutoDisabledSent) {
                 clarityAutoDisabledSent = true;
                 notify('onClarityAutoDisabled', String(decodeTimeMs));
             }
         }
         ```
     - Thêm hàm `@JavascriptInterface fun onClarityAutoDisabled(decodeTimeStr: String?)` trong `class XboxBridge` (gần dòng 2305):
       - Kiểm tra `if (!isCallerTrusted()) return`.
       - Gọi `com.example.data.SettingsRepository.updateClarityPreset(com.example.data.ClarityPreset.OFF)` và `com.example.data.SettingsRepository.updateClarityBoost(false)`.
       - Chuyển lên `scope.launch(Dispatchers.Main)` để hiển thị `android.widget.Toast` ngắn thông báo đã tự tắt bộ lọc làm nét (`ClarityPreset.OFF`) do thời gian giải mã vượt 12 ms trong 3 mẫu liên tiếp.

---

### Mục 5: Đảm bảo gửi mẫu Telemetry định kỳ mỗi 2 giây khi vượt ngưỡng dù giá trị không đổi
- **File và hàm thật sẽ sửa**: `/app/src/main/java/com/example/cloudplay/XboxBridge.kt`
  - Trong `XboxBridge.Companion.getInjectionScript` tại dòng 806–818:
    - Thêm kiểm tra vượt ngưỡng:
      ```javascript
      const isOverThreshold = (decodeTimeMs > 12.0 || jitter > 15 || ( prev && framesDropped > prev.framesDropped ) || lostPercent > 2.0);
      const hasMeaningfulDelta = !prev ||
          elapsedSinceSent >= 4500 ||
          (isOverThreshold && elapsedSinceSent >= 2000) ||
          Math.abs(fps - prev.fps) >= 3 ||
          Math.abs(rtt - prev.rttMs) >= Math.max(4, Math.round(prev.rttMs * 0.05)) ||
          Math.abs(jitter - prev.jitterMs) >= 3 ||
          Math.abs(decodeTimeMs - prev.decodeTimeMs) >= 1.0 ||
          framesDropped !== prev.framesDropped ||
          freezeCount !== prev.freezeCount;
      ```

---

### Mục 6 (Điểm 2): Xóa tham số `bufferHintMs` không còn tác dụng & sửa comment `"zero-copy"`
- **File và hàm thật sẽ sửa**:
  1. **`/app/src/main/java/com/example/cloudplay/QualityAdapter.kt`** (dòng 17–62):
     - Xóa biến `private var lastAppliedBufferMs: Double = -1.0` và xóa overload `fun applyAdaptiveBitrate(targetBitrateMbps: Double, bufferHintMs: Int)`.
     - Đổi chữ ký `fun applyAdaptiveBitrate(targetBitrateMbps: Double, smoothGlide: Boolean = true)` (xóa tham số `bufferHintMs: Double`).
     - Tại dòng 54: đổi chuỗi JS thành `"if (window.__setAdaptiveBitrate) window.__setAdaptiveBitrate($targetBitrateMbps, $smoothGlide);"`.
  2. **`/app/src/main/java/com/example/cloudplay/AdaptiveQualityManager.kt`** (dòng 177, 239, 276, 507, 545):
     - Cập nhật cả 5 lời gọi `QualityAdapter.applyAdaptiveBitrate(...)` để bỏ đối số `bufferHintMs = ...`.
  3. **`/app/src/main/java/com/example/cloudplay/XboxBridge.kt`** (dòng 232):
     - Đổi `window.__setAdaptiveBitrate = function(targetMbps, bufferHintMs, smoothGlide)` thành `window.__setAdaptiveBitrate = function(targetMbps, smoothGlide)`.
  4. **`/app/src/main/java/com/example/ui/CloudBrowserScreen.kt`** (dòng 490–492):
     - Sửa comment trên `setLayerType(android.view.View.LAYER_TYPE_NONE, null)` thành:
       `// Giữ LAYER_TYPE_NONE (mặc định của View) để WebView vẽ trực tiếp trên cửa sổ Hardware Accelerated mà không tạo bộ đệm offscreen layer phụ.`

---

### Mục 7 (Điểm 4): Tách Ngôn Ngữ Giao Diện App Khỏi Thị Trường Xbox (Khôi Phục Đầy Đủ 7a, 7b, 7c, 7d, 7e)
- **Lưu ý phạm vi**: Đã **bỏ** việc giả `navigator.language` / `Accept-Language` và việc tự động đánh dấu thị trường `"đã xác minh"` khỏi phần thực hiện, chuyển sang mục **"Đề xuất thêm — chờ chủ dự án duyệt"** ở cuối bản kế hoạch.
- **File và hàm thật sẽ sửa**:
  1. **`/app/src/main/java/com/example/data/AppSettings.kt`**:
     - **(c) Danh sách trắng thị trường ban đầu**: Thêm `val INITIAL_VERIFIED_XBOX_MARKETS = setOf("en-US")` (chỉ giữ `"en-US"`).
     - **(a) Tách `appLanguage` khỏi thị trường Xbox**:
       - Sửa `SettingsRepository.resolveSystemXboxLocale()` (dòng 893): luôn trả về `"en-US"` (không suy ra `ja-JP`, `ko-KR`, `de-DE`... từ ngôn ngữ ứng dụng hay ngôn ngữ hệ thống).
       - Sửa `AppConfig.getEffectiveLocale()` (dòng 639): trả về `customLocale.ifBlank { "en-US" }`.
       - Sửa `SettingsRepository.updateAppLanguage(language: AppLanguage)` (dòng 999): chỉ cập nhật `appLanguage`, không chạm tới `customLocale` hay `targetRegionUrl`.
       - Thêm hàm `fun resetXboxMarketToDefault()` trong `SettingsRepository` để đưa `customLocale = "en-US"`, `useSystemLanguage = false`.
     - **(d) Migration một lần trong `SettingsRepository.init(context: Context)` (dòng 669)**:
       - Kiểm tra khóa `"migrated_xbox_market_en_us_v1"` trong `SharedPreferences`.
       - Nếu chưa migrate (`!sp.getBoolean("migrated_xbox_market_en_us_v1", false)`): nếu `custom_locale` đã lưu trước đó khác `"en-US"` hoặc `use_system_language` đang bật làm thị trường khác `"en-US"`, đặt lại `custom_locale = "en-US"` và `use_system_language = false`, sau đó ghi `putBoolean("migrated_xbox_market_en_us_v1", true)`.
  2. **`/app/src/main/java/com/example/ui/tabs/SettingsScreen.kt`**:
     - **(b) Mục Nâng cao ẩn cho chọn thị trường Xbox thủ công**:
       - Đặt bộ chọn thị trường Xbox (`customLocale`) bên trong một mục **"Tùy chọn thị trường Xbox nâng cao (Thủ công)"** mặc định thu gọn (ẩn).
       - Khi mở ra và chọn thị trường ngoài `INITIAL_VERIFIED_XBOX_MARKETS` (`!= "en-US"`), hiển thị hộp thoại/thẻ cảnh báo rõ ràng rằng thị trường này chưa được xác nhận và có thể gây lỗi không vào được game, kèm nút **"Quay về en-US"** (`SettingsRepository.resetXboxMarketToDefault()`).
  3. **`/app/src/main/java/com/example/ui/CloudBrowserScreen.kt` & `/app/src/main/java/com/example/cloudplay/XboxBridge.kt`**:
     - **(e) Gợi ý `"Quay về en-US"` khi vào game thất bại sau khi đổi thị trường**:
       - Khi `config.getEffectiveLocale() != "en-US"` và phát hiện tín hiệu lỗi vùng / không vào được game (`onRegionUnlockSignal("UNLOCK_FAIL")` hoặc trang báo `Unsupported market` / lỗi khởi chạy phiên), hiển thị thanh gợi ý rõ ràng trên màn hình trình duyệt với nút **"Quay về en-US"** (khi người dùng bấm mới gọi `SettingsRepository.resetXboxMarketToDefault()` và tải lại URL `en-US`; **tuyệt đối không tự đổi ngầm**).

---

### Mục 8 (Điểm 3): Tách Ngôn Ngữ Dịch Web/Game (`xcloudTranslateEnabled` + `translateLanguage`) & Sửa `XboxBridge.kt` / `XboxTranslator.kt`
- **File và hàm thật sẽ sửa**:
  1. **`/app/src/main/java/com/example/data/AppSettings.kt`**:
     - Thêm hằng số `const val TRANSLATE_LANG_OFF = "OFF"` và trường `val translateLanguage: String = "vi"` vào `data class AppConfig` (dòng 607).
     - Sửa `AppConfig.isEffectiveTranslateEnabled()` (dòng 659):
       ```kotlin
       fun isEffectiveTranslateEnabled(): Boolean {
           return !lowEndDeviceMode &&
               xcloudTranslateEnabled &&
               translateLanguage.isNotBlank() &&
               !translateLanguage.equals(TRANSLATE_LANG_OFF, ignoreCase = true)
       }
       ```
     - Sửa `AppConfig.getTargetTranslateLanguageCode()` (dòng 655) — **Đảm bảo không bao giờ trả chuỗi rỗng vào bộ dịch**:
       ```kotlin
       fun getTargetTranslateLanguageCode(): String {
           val raw = translateLanguage.trim()
           if (raw.isNotBlank() && !raw.equals(TRANSLATE_LANG_OFF, ignoreCase = true)) {
               return raw
           }
           return appLanguage.resolveTranslateTargetCode().ifBlank { "vi" }
       }
       ```
     - Thêm hàm `fun updateTranslateLanguage(langCode: String)` trong `SettingsRepository`:
       - Nếu `langCode.equals("OFF", ignoreCase = true)`: cập nhật `translateLanguage = "OFF"`, `xcloudTranslateEnabled = false`.
       - Nếu `langCode` là mã ngôn ngữ hợp lệ (`"vi"`, `"es"`, `"pt"`, `"fr"`, `"de"`, `"ja"`, `"ko"`, `"zh-CN"`, `"id"`, `"th"`, `"ru"`...): cập nhật `translateLanguage = langCode`, `xcloudTranslateEnabled = true`.
     - **Migration trong `SettingsRepository.init(context: Context)` (dòng 669, khóa `"migrated_translate_lang_v1"`)**:
       - Kiểm tra người dùng cũ: `val isExistingUser = sp.contains("xcloud_translate") || sp.contains("app_language") || sp.contains("clarity_preset") || sp.contains("user_accepted_terms")`.
       - **Người dùng cũ (`isExistingUser == true`)**: giữ `translateLanguage = "vi"` (nếu `xcloud_translate` đang `false` thì giữ `xcloudTranslateEnabled = false`).
       - **Người dùng mới (`isExistingUser == false`)**: lấy `val effLang = loadedAppLanguage.effectiveLanguageCode()`. Nếu `effLang == "en"` thì đặt `translateLanguage = "OFF"`, `xcloudTranslateEnabled = false`; nếu `effLang != "en"` thì đặt `translateLanguage = loadedAppLanguage.resolveTranslateTargetCode().ifBlank { "vi" }` và `xcloudTranslateEnabled = true`.
     - Trong `SettingsRepository.updateAppLanguage(language: AppLanguage)` (dòng 999): chỉ cập nhật `appLanguage = language`, không tự ý ghi đè `xcloudTranslateEnabled` hay `translateLanguage` đã cấu hình riêng.
  2. **`/app/src/main/java/com/example/cloudplay/XboxBridge.kt`**:
     - **Dòng 2308–2322 (`requestSmartTranslate`)**:
       - Kiểm tra `val cfg = com.example.data.SettingsRepository.config.value; if (!cfg.isEffectiveTranslateEnabled()) return`.
       - Lấy `val targetLang = cfg.getTargetTranslateLanguageCode()`.
       - Sửa **dòng 2313** từ `GoogleGamingTranslator.translate(rawText, "vi", isBilingual)` thành:
         `val translated = GoogleGamingTranslator.translate(rawText, targetLang, isBilingual)`
     - **Dòng 2325–2352 (`requestBatchTranslate`)**:
       - Kiểm tra `val cfg = com.example.data.SettingsRepository.config.value; if (!cfg.isEffectiveTranslateEnabled()) return`.
       - Lấy `val targetLang = cfg.getTargetTranslateLanguageCode()`.
       - Sửa **dòng 2337** từ `GoogleGamingTranslator.translateBatch(list, "vi", isBilingual)` thành:
         `val translatedMap = GoogleGamingTranslator.translateBatch(list, targetLang, isBilingual)`
     - **Dòng 153 (`XboxBridge.Companion.getInjectionScript`)**:
       - Truyền thêm `targetLanguage = com.example.data.SettingsRepository.config.value.getTargetTranslateLanguageCode()` vào `XboxTranslator.getTranslatorScript(...)`.
  3. **`/app/src/main/java/com/example/cloudplay/XboxTranslator.kt`**:
     - Trong `fun getTranslatorScript(enabled: Boolean, preserveGameTitles: Boolean = true, bilingualMode: Boolean = false, hideStreamStats: Boolean = true, targetLanguage: String = "vi"): String` (dòng 450):
       - Kiểm tra `val useVietnameseStaticDict = targetLanguage.equals("vi", ignoreCase = true)`.
       - **Chỉ nạp từ điển tĩnh tiếng Việt (`XBOX_UI_DICTIONARY_EN`, `XBOX_UI_DICTIONARY_JP`, `XBOX_UI_DICTIONARY_KR`, `dynamicGithubDictionary`) và regex tiếng Việt trong `translateString(text)` (dòng 537–577) khi `useVietnameseStaticDict == true`**.
       - Khi `targetLanguage != "vi"` (ví dụ `"es"`, `"fr"`, `"de"`, `"ja"`, `"ko"`...), `RAW_DICT` để rỗng `{}` và `translateString(text)` trả về `null` để toàn bộ văn bản giao diện được gửi qua `requestBatchTranslate` / `requestSmartTranslate` tới `GoogleGamingTranslator` theo đúng `targetLanguage`.
       - Trong `syncDictionaryFromGithub` (dòng 362): chỉ đồng bộ `vi-VN.json` khi `config.isEffectiveTranslateEnabled() && config.getTargetTranslateLanguageCode().equals("vi", ignoreCase = true)`.
  4. **`/app/src/main/java/com/example/ui/tabs/SettingsScreen.kt`**:
     - Thêm bộ chọn **"Ngôn ngữ dịch xCloud"** gồm tùy chọn **"Tắt" (`"OFF"`)** và danh sách ngôn ngữ đích (`Tiếng Việt`, `Español`, `Português`, `Français`, `Deutsch`, `日本語`, `한국어`, `中文`...).

---

### Mục 9 & 10 (Điểm 5 & Điểm D 1–7): Bộ Tra DB-IP Lite Country, `LiveProxyFetcher.kt`, `GeoIpDetector.kt`, `ServersScreen.kt`
1. **Bộ tra IP → Mã ISO dùng DB-IP Lite Country (`GeoIpDetector.kt`)**:
   - **Điều khoản DB-IP**: DB-IP Lite Country phát hành theo giấy phép **CC BY 4.0** (`https://db-ip.com/db/lite.php`), cho phép sử dụng miễn phí trong ứng dụng với điều kiện ghi công `"IP geolocation by DB-IP.com"` kèm liên kết `https://db-ip.com`.
   - **Cách đọc & tìm kiếm nhị phân**:
     - Tải tệp `https://download.db-ip.com/free/dbip-country-lite-YYYY-MM.csv.gz` (nếu tháng hiện tại chưa có thì lùi về tháng trước) khi có Wi-Fi (hoặc hỏi người dùng nếu đang dùng mạng di động).
     - Đọc các dòng IPv4 (`startIp,endIp,countryCode`), biên dịch thành mảng nguyên thủy đã sắp xếp (`startIps: LongArray`, `endIps: LongArray`, `countryCodes: Array<String>`) lưu đệm tại `File(context.filesDir, "dbip_country_v4.bin")` và tra cứu bằng **tìm kiếm nhị phân `O(log N)`** trong `GeoIpDetector.resolveIpCountryCode(ip: String)`.
     - **Kiểm tra tệp tải về & giữ bản cũ nếu lỗi**: Tải vào file tạm `.tmp`, kiểm tra HTTP 200, GZIP magic bytes (`0x1f 0x8b`), số lượng dải hợp lệ và tra thử IP mẫu trước khi đổi tên nguyên tử sang file chính; nếu lỗi thì xóa file tạm và giữ nguyên bản DB-IP cũ.
     - Công tắc tắt dịch vụ tra IP dự phòng qua HTTPS: thêm `val enableFallbackIpLookup: Boolean = true` trong `AppConfig` (`SettingsRepository.updateEnableFallbackIpLookup(enabled: Boolean)`).
2. **Nối bộ tra vào `LiveProxyFetcher.kt` (`LiveProxyFetcher.kt:32-203`)**:
   - Đổi URL VPNGate ở dòng 43 từ `http://www.vpngate.net/api/iphone/` sang **`https://www.vpngate.net/api/iphone/`**.
   - Tách truy vấn ProxyScrape ở dòng 90 (`protocol=http,socks5`) thành 2 lượt riêng cho `protocol=http` và `protocol=socks5` để lưu đúng `protocol` vào `CandidateNode`.
   - Khi danh sách ứng viên hoặc máy chủ xác minh rỗng (dòng 135–137 và 198–202): trả về `emptyList()`, **không** trả về `DEFAULT_SERVERS`.
   - Với mỗi IP ứng viên: gọi `val resolvedCc = GeoIpDetector.resolveIpCountryCode(node.ip)`. **Kết quả tra thật từ IP (`resolvedCc`) luôn thắng mã quốc gia từ tham số nguồn proxy**.
3. **Cờ Emoji động từ mã ISO & Tên máy chủ (`GeoIpDetector.kt`, `LiveProxyFetcher.kt`)**:
   - Trong `GeoIpDetector.getFlagEmoji(countryCode: String)`: sinh cờ Unicode Regional Indicator (`0x1F1E6 + (char - 'A')`) cho mọi mã ISO 3166-1 alpha-2 hợp lệ (đổi `"UK"` -> `"GB"`), trả về `"🌐"` khi không rõ.
   - Trong `GeoIpDetector.getCountryDisplayName(countryCode: String, isEn: Boolean)`: trả về tên nước theo ngôn ngữ app (`Locale("", code).getDisplayCountry(...)`), hoặc **`"Không rõ quốc gia"`** (`"Unknown country"` khi `isEn == true`) nếu không xác định được.
   - Tên máy chủ trong `LiveProxyFetcher.kt` định dạng thống nhất: **`"<cờ> <Tên nước> • <IP>"`** (hoặc `"🌐 Không rõ quốc gia • <IP>"`).
4. **Sửa `azureEndpoint` (Điểm 5 — Chỉ giữ ánh xạ có sẵn cho `JP` và `KR`)**:
   - Trong `LiveProxyFetcher.kt` (dòng 161–165) và `GeoIpDetector.getAzureEndpointForCountry(countryCode: String)`:
     - **Bỏ 6 địa chỉ tự đặt (`SG`, `AU`, `DE`, `FR`, `GB`, `BR`) và bỏ mặc định `westus.cloudapp.azure.com`**.
     - Chỉ giữ đúng 2 ánh xạ có sẵn trong code cho `JP` và `KR`:
       ```kotlin
       val azureEndpoint = when (cc) {
           "JP" -> "japaneast.cloudapp.azure.com"
           "KR" -> "koreacentral.cloudapp.azure.com"
           else -> ""
       }
       ```
     - Trong `PingTester.kt`: nếu `server.azureEndpoint.isBlank()`, chỉ đo trực tiếp trên `server.ip:server.port` mà không đo fallback sang `westus.cloudapp.azure.com`.
5. **Danh sách máy chủ & `CustomProxyDialog` (`ServersScreen.kt:910-1521`)**:
   - Nhóm/lọc danh sách máy chủ theo quốc gia, hiển thị cờ + tên nước trên chip lọc và trên `ServerCard`.
   - Trong `CustomProxyDialog` (`ServersScreen.kt:1298`): khi người dùng nhập `host` (IP), tự động gọi `GeoIpDetector.resolveIpCountryCode(host)` để cập nhật `countryCode` (và cập nhật `azureEndpoint` tương ứng: `"JP" -> "japaneast.cloudapp.azure.com"`, `"KR" -> "koreacentral.cloudapp.azure.com"`, khác -> `""`), đồng thời vẫn giữ `OutlinedTextField` của `countryCode` (dòng 1418) để người dùng tự sửa tay nếu muốn. Dùng `GeoIpDetector.getFlagEmoji(cc)` ở dòng 1480 thay cho `when` cố định.
6. **IP riêng / Loopback không gán quốc gia (`GeoIpDetector.kt`)**:
   - Hàm `GeoIpDetector.isPrivateOrLoopbackIp(ip: String): Boolean` chặn toàn bộ dải `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`, `100.64.0.0/10`, `0.0.0.0/8`, `::1`, `fc00::/7`, `fe80::/10`, `localhost` — luôn trả về mã quốc gia rỗng `""` (`"Không rõ quốc gia"`, cờ `"🌐"`).
7. **Ghi công DB-IP (`SettingsScreen.kt`)**:
   - Thêm dòng `"IP geolocation by DB-IP.com"` kèm liên kết mở `https://db-ip.com` ở phần Cài đặt / Giới thiệu.

---

## NHÓM 4 – Kiểm Thử Tự Động (`ExampleRobolectricTest.kt` — Điểm E)

Chỉ viết thêm test trong `/app/src/test/java/com/example/ExampleRobolectricTest.kt` (**chưa chạy build/test**):
1. **Test Mục 1**: `R.id.webview_desktop_ua_tag` hợp lệ với `View.setTag(int, Object)` và không ném `IllegalArgumentException`.
2. **Test Mục 2 & Điểm 1**: `NetworkOptimizer.onTelemetryReceived` đọc đúng các khóa thật (`rttMs`, `jitterMs`, `decodeTimeMs`, `bitrateMbps`, `framesDropped`, `hasStatsSample`); mẫu `hasStatsSample = false` không làm thay đổi `emaJitterMs`, `emaRttMs`, `emaDecodeMs` trong `AdaptiveQualityManager.evaluateTelemetry` và không nạp số âm.
3. **Test Mục 3**: `XboxBridge.queryHardwareCodecSupport()` và `XboxBridge.getInjectionScript` truyền cờ phần cứng vào `navigator.mediaCapabilities.decodingInfo`.
4. **Test Mục 4 & Điểm 2**: Tự tắt làm nét sau đúng `>= 3` mẫu liên tiếp `decodeTimeMs > 12.0`, gọi `SettingsRepository.updateClarityPreset(ClarityPreset.OFF)` đưa `config.clarityPreset` về `ClarityPreset.OFF` và `config.clarityBoostEnabled` về `false`.
5. **Test Mục 5**: Nhịp gửi telemetry định kỳ mỗi `2000 ms` khi `decodeTimeMs > 12.0` hoặc `jitterMs > 15` không đổi.
6. **Test Mục 6**: `QualityAdapter.applyAdaptiveBitrate(targetBitrateMbps, smoothGlide)` và `window.__setAdaptiveBitrate(targetMbps, smoothGlide)` không còn tham số `bufferHintMs`.
7. **Test Mục 7 & Điểm 4**:
   - Đổi `appLanguage` không làm đổi `getEffectiveLocale()` / `targetRegionUrl`.
   - `INITIAL_VERIFIED_XBOX_MARKETS` chỉ chứa `"en-US"`.
   - Migration 1 lần trong `SettingsRepository.init` chuyển thị trường cũ khác `"en-US"` về `"en-US"`.
8. **Test Mục 8 & Điểm 3**:
   - Migration dịch cho người dùng cũ (giữ `"vi"`, nếu `xcloud_translate == false` thì giữ tắt) và người dùng mới (theo ngôn ngữ app, nếu app tiếng Anh `"en"` thì mặc định `"OFF"`).
   - `getTargetTranslateLanguageCode()` không bao giờ trả chuỗi rỗng.
   - `XboxTranslator.getTranslatorScript` chỉ nhúng từ điển tĩnh tiếng Việt khi `targetLanguage == "vi"`.
9. **Test Mục 9, 10 & Điểm 5, E**:
   - Các IP mẫu thuộc **`JP`, `KR`, `US`, `VN`, `DE`, `FR`** tra ra đúng mã ISO (`"JP"`, `"KR"`, `"US"`, `"VN"`, `"DE"`, `"FR"`), đúng cờ (`"🇯🇵"`, `"🇰🇷"`, `"🇺🇸"`, `"🇻🇳"`, `"🇩🇪"`, `"🇫🇷"`), và tên `"<cờ> <Tên nước> • <IP>"`.
   - IP không tra được trả về `""`, cờ `"🌐"`, tên `"Không rõ quốc gia"`.
   - IP riêng/loopback (`127.0.0.1`, `10.0.0.1`, `192.168.1.1`, `172.16.0.1`, `169.254.1.1`, `::1`) không bị gán quốc gia.
   - Kết quả tra IP thật thắng tham số quốc gia của nguồn proxy.
   - `azureEndpoint` chỉ nhận `"japaneast.cloudapp.azure.com"` cho `JP` và `"koreacentral.cloudapp.azure.com"` cho `KR`, còn lại trả về `""`.

---

## Đề Xuất Thêm — Chờ Chủ Dự Án Duyệt
*(Các mục dưới đây được tách riêng, **không** thực hiện trong lượt này nếu chủ dự án chưa duyệt)*

1. **Đồng bộ `navigator.language` / `navigator.languages` và header `Accept-Language` theo thị trường Xbox**: Giả lập `navigator.language` trong WebView khớp với thị trường Xbox đang chọn khi người dùng chủ động thử nghiệm thị trường ngoài `en-US`.
2. **Tự động ghi nhận thị trường Xbox "Đã xác minh" (`verifiedXboxMarkets`) khi vào stream thành công**: Tự động lưu các thị trường mà người dùng đã kết nối WebRTC thành công để không hiện lại cảnh báo ở các lần sau.
3. **Hiển thị phiên bản tháng của cơ sở dữ liệu DB-IP Lite trên màn hình Máy chủ (`ServersScreen`)**: Cho phép người dùng xem nhanh tháng phát hành của tệp DB-IP đang dùng và bấm cập nhật thủ công qua Wi-Fi.
