# Kế hoạch nâng cấp Hệ thống Điều khiển CloudPlay (v1.3.0 — Bản hiệu chỉnh)

Dựa trên yêu cầu hiệu chỉnh kế hoạch và đối chiếu trực tiếp với mã nguồn thực tế (`XboxBridge.kt`, `VirtualGamepad.kt`, `CloudBrowserScreen.kt`, `FloatingHud.kt`, `InGameLoadingOverlay.kt`, `XboxTranslator.kt`, `AppSettings.kt`, `SettingsScreen.kt`), dưới đây là kế hoạch triển khai đã cập nhật:

---

## 1. Rung của game (Game Rumble qua Gamepad API)
- **Hiện trạng trong code**:
  - `XboxBridge.kt` (dòng 1600) đang gán `vibrationActuator: null` trên `virtualGamepadState`.
  - Khi gộp tay cầm thật và tay cầm ảo trong `navigator.getGamepads()` (`XboxBridge.kt` dòng 1753–1761), mã hiện tại tạo một object mới chỉ gồm `{ id, index, connected, timestamp, mapping, axes, buttons }` nên **làm mất thuộc tính `physPad.vibrationActuator` (và `hapticActuators`) gốc của tay cầm thật**.
- **Phương án triển khai**:
  - **Với tay cầm thật (`physPad` có kết nối)**: Giữ nguyên và chuyển tiếp trực tiếp `vibrationActuator: physPad.vibrationActuator || null` (cùng `hapticActuators` nếu có) của tay cầm thật sang đối tượng trả về trong `navigator.getGamepads()`. **Tuyệt đối không thay thế actuator của tay cầm thật bằng rung điện thoại.**
  - **Chỉ khi chơi bằng tay cầm ảo (`!physPad` và `shouldExposeVirtual`)**: Cung cấp đối tượng `virtualVibrationActuator` (`type: "dual-rumble"`, `playEffect(type, params)`, `reset()`) trên `virtualGamepadState` để chuyển tiếp lệnh rung của game về `Vibrator` của điện thoại qua `@JavascriptInterface fun onGameRumble(durationMs: Int, strongMag: Float, weakMag: Float)` (giữ nguyên tên mọi hàm `@JavascriptInterface` hiện có).
  - Thêm tùy chọn **Độ rung của game trên tay cầm ảo (`gameRumbleLevel`: Tắt / Nhẹ / Vừa / Mạnh)** trong `AppSettings.kt` và `SettingsScreen.kt`, kèm giới hạn tần suất (throttle `65ms`, thời lượng tối đa `500ms`/lần, ngắt khi máy quá nhiệt `window.__deviceThermalThrottled` hoặc xuống nền).

---

## 2. Con quay hồi chuyển (Gyroscope) — Gỡ bỏ toàn bộ trên `app/src`
- **Kết quả `grep -ri "gyro" app/src/`**:
  1. `VirtualGamepad.kt` (dòng 363–432, 838, 845–846, 1350, 1513, 1725–1735): Mã đăng ký cảm biến `Sensor.TYPE_GYROSCOPE`, biến `gyroRightX`/`gyroRightY`, nhãn `"R•🎯"`, và nút bật nhanh `🎯 Ngắm Gyro`.
  2. `SettingsScreen.kt` (dòng 714–730): Công tắc `🎯 Nghiêng máy để ngắm` và thanh trượt `Độ nhạy nghiêng máy`.
  3. `AppSettings.kt` (dòng 624–625, 848–849, 971–972, 1411–1418): Trường `gamepadGyroEnabled`, `gamepadGyroSensitivity` và các hàm cập nhật.
  4. `app/src/main/assets/web/terms.html` (dòng 632): Cụm từ `"gyroscope aiming"` trong mô tả điều khoản mục 2.3.
  5. `XboxTranslator.kt` (dòng 285): `"Gyroscope" to "Cảm biến con quay hồi chuyển (Gyro)"` — đây là từ điển dịch giao diện web Xbox Cloud (dùng để dịch chữ "Gyroscope" trên trang web Xbox nếu xuất hiện, không phải mã cảm biến của app).
- **Phương án triển khai**:
  - Gỡ bỏ toàn bộ mã cảm biến, giao diện và tham chiếu gyro tại `VirtualGamepad.kt`, `SettingsScreen.kt`, `AppSettings.kt`, `XboxTranslator.kt` (dòng 285) và `terms.html` (dòng 632).
  - Trong `AppSettings.kt`, bỏ hoàn toàn các thuộc tính gyro khỏi `AppConfig` và không ghi xuống `SharedPreferences` nữa (dữ liệu `SharedPreferences` cũ có sẵn khóa `gamepad_gyro_enabled` trên máy người dùng cũ sẽ tự động được bỏ qua an toàn khi đọc mà không gây lỗi).

---

## 3. Đổi nút (Remap) linh hoạt cho tay cầm thật
- **Phương án triển khai**:
  - Lưu bảng ánh xạ nút tay cầm thật `physicalGamepadButtonMap: String` trong `AppSettings.kt` (ánh xạ từng nút bất kỳ trong 17 nút chuẩn W3C Gamepad).
  - Trong `navigator.getGamepads()` (`XboxBridge.kt`), áp dụng bảng ánh xạ `window.__physGamepadRemap` riêng trên mảng `physPad.buttons` của tay cầm thật trước khi trả về hoặc gộp. Không tác động đến `virtualGamepadState.buttons`.
  - Trong `SettingsScreen.kt`, thêm giao diện cho phép đổi từng nút bất kỳ của tay cầm thật kèm nút **Đặt lại (Reset)** có `contentDescription` và chuỗi song ngữ VI/EN.

---

## 4. Vùng chết (Deadzone) Trong & Ngoài cho từng cần Analog (Trái / Phải)
- **Phương án triển khai**:
  - **Với tay cầm ảo**: Bổ sung vùng chết trong và ngoài cho Cần Trái (`leftStickInnerDeadzone` mặc định `0.08f`, `leftStickOuterDeadzone` mặc định `0.95f`) và Cần Phải (`rightStickInnerDeadzone` mặc định `0.08f`, `rightStickOuterDeadzone` mặc định `0.95f`), áp dụng trong `VirtualAnalogStick` (`VirtualGamepad.kt`).
  - **Với tay cầm thật**: Bổ sung vùng chết trong và ngoài riêng cho Cần Trái và Cần Phải của tay cầm thật với **giá trị mặc định `inner = 0.0f` và `outer = 1.0f`** (giữ nguyên giá trị trục gốc của tay cầm thật, không chồng lên vùng chết của game trừ khi người dùng chủ động chỉnh trong Cài đặt).
  - Trong `navigator.getGamepads()` (`XboxBridge.kt`), bước tính vùng chết của tay cầm thật chỉ chạy khi khác mặc định (`inner > 0` hoặc `outer < 1.0`) và được **áp dụng trên `physPad.axes` TRƯỚC bước gộp `mergedAxes`**.

---

## 5. Bố cục tay cầm ảo lưu riêng theo từng game
- **Hiện trạng nhận diện game**:
  - App đã nhận diện được game qua URL `/play/launch/<slug>/<productId>` và `streamUiState.gameTitle` (`StreamController.kt`).
- **Phương án triển khai**:
  - Lưu hồ sơ bố cục theo khóa game (`perGameLayoutsJson`) kèm cờ `usePerGameGamepadLayout: Boolean` trong `AppSettings.kt`.
  - Thêm nút **Dùng bố cục chung (Use Global Layout)** và **Đặt lại (Reset)** trong bảng chỉnh tay cầm ảo (`VirtualGamepad.kt`) và trong `SettingsScreen.kt`.

---

## 6. Tổ hợp nút tay cầm làm phím tắt (3 hành động, không làm trễ nút View)
- **Phương án triển khai**:
  - Chỉ hỗ trợ đúng **3 hành động có sẵn**:
    1. Ẩn / hiện bảng thông số luồng phát (`hideStreamStatsOverlay`).
    2. Bật / tắt tay cầm ảo trên màn hình (`showVirtualController`).
    3. Bật / tắt quay video màn hình (`ZeroLagScreenRecorder.toggleRecording`).
  - **Cơ chế xử lý nút View (Back) không trễ, không nuốt nút**:
    - Khi người chơi nhấn nút `View` (nút số 8), sự kiện nhấn `View` **vẫn được chuyển ngay lập tức vào game** (không giữ chờ hay làm trễ nút `View`).
    - Hệ thống chỉ ghi nhận thời điểm bắt đầu giữ nút `View` (`viewButtonDownAt`). Khi nút `View` đang được giữ đủ lâu (`>= 400ms`) và người chơi nhấn nút thứ hai trùng khớp với một trong 3 phím tắt đã gán, hệ thống mới chặn riêng xung của **nút thứ hai** đó để thực thi hành động phím tắt.
  - Cho phép bật/tắt tính năng tổ hợp phím tắt và đổi nút kích hoạt thứ hai cho từng hành động trong `SettingsScreen.kt`.

---

## 7. Tự ẩn tay cầm ảo khi cắm tay cầm thật
- **Kết quả kiểm tra `CloudBrowserScreen.kt` (dòng 688, 881, 920)**:
  - Tại dòng 688 (`hasGamepad = gamepadStatus.hasPhysicalController` truyền vào `InGameLoadingOverlay`) và dòng 920 (`physicalControllerConnected = gamepadStatus.hasPhysicalController` truyền vào `IsolatedFloatingHud`), hai tham số này hiện **chỉ dùng để đổi nhãn chữ và màu biểu tượng trạng thái tay cầm** trên màn hình chờ (`InGameLoadingOverlay.kt` dòng 194) và trên viên thuốc HUD (`FloatingHud.kt` dòng 772, 1681).
  - Tại dòng 881 của `CloudBrowserScreen.kt`:
    ```kotlin
    if (config.showVirtualController) {
        VirtualGamepadOverlay(...)
    }
    ```
    **Chưa có logic tự ẩn `VirtualGamepadOverlay` khi cắm tay cầm thật.**
- **Phương án triển khai**:
  - Thêm công tắc `autoHideVirtualOnPhysicalController: Boolean = true` trong `AppSettings.kt` và `SettingsScreen.kt`.
  - Cập nhật điều kiện hiển thị tại dòng 881 (và `LaunchedEffect` đồng bộ `__setVirtualGamepadEnabled` tại dòng 396) của `CloudBrowserScreen.kt` để tự động ẩn tay cầm ảo khi `gamepadStatus.hasPhysicalController && config.autoHideVirtualOnPhysicalController` (trừ khi người dùng chủ động bấm nút bật tay cầm ảo trên HUD trong lúc đang cắm tay cầm thật).

---

## 8. Nghiên cứu Bàn phím & Chuột Bluetooth/USB (Chỉ báo cáo nghiên cứu, KHÔNG viết code)
- Trình bày báo cáo nghiên cứu kỹ thuật trong phần tổng kết cuối cùng (cơ chế `KeyEvent`/`MotionEvent` trên WebView Android, rào cản `requestPointerLock` của Xbox Cloud Gaming trên WebView di động, và các điều kiện cần thiết nếu muốn hỗ trợ).
