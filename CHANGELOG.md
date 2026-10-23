# Nhật ký cập nhật / Update Changelog (CloudPlay Xbox)

Tài liệu này ghi nhận chi tiết các thay đổi thực tế của từng bản cập nhật và được hệ thống xuất APK tự động đối chiếu cùng `git diff` khi tạo trang phát hành (GitHub Releases).

## [v1.4.0] - Sửa lỗi Quay Video .MP4 Có Tiếng, Chống Giật Khung Hình & Tối Ưu Thứ Tự Ưu Tiên Toàn Hệ Thống
### 🛠️ Sửa lỗi hệ thống & Tối ưu độ mượt khi chơi (Core Stability & Anti-Stutter)
- **Giữ nguyên màn hình chơi game 100% chống giật khung hình:** Loại bỏ hoàn toàn việc can thiệp lại CSS `transform` / `contain` trên phần tử `<video>` khi luồng game đang phát hoặc khi bấm quay clip, chấm dứt hiện tượng khựng hình do trình duyệt tính toán lại bố cục.
- **Khắc phục lỗi Clarity Boost & Giảm tải luồng WebView:** Sửa lỗi bộ lọc làm nét tự bật lại khi chuyển trang, ngăn việc nạp lặp lại script 70KB trong `onPageFinished` và loại bỏ thao tác ghi đĩa giữa trận trong `AdaptiveQualityManager`.
- **Sắp xếp lại thứ tự ưu tiên hợp lý trên toàn ứng dụng:** Đưa các tính năng cốt lõi (Đồ họa, Mức chất lượng 4G/Wi-Fi, Khung hình 60/120 FPS, Giảm tải GPU) lên vị trí ưu tiên số 1; gom gọn cài đặt Tỉ lệ màn hình & Màu sắc vào nhóm Hình ảnh (xóa bỏ thẻ trùng lặp ở mục Nâng cao) và xếp Quay Video vào nhóm tính năng phụ thu gọn ở cuối.
- **Sửa lỗi lưu video `.webm` và mất tiếng:** Thu trực tiếp âm thanh nổi Stereo 48kHz từ WebRTC / AudioContext qua bộ mã hóa phần cứng AAC-LC & H.264, đóng gói chuẩn `.MP4` quốc tế (`ftyp` ISO) có đầy đủ tiếng ở cả chế độ Quay Full Screen toàn màn hình (Mặc định) và Quay 16:9 chuẩn quốc tế.
- **Nâng cấp AI tạo mô tả bản phát hành theo Git Diff thực tế:** Phân tích trực tiếp các dòng code thay đổi (`git diff prev_tag..HEAD`) để viết mô tả bản cập nhật chính xác, không lặp lại mô tả của các phiên bản cũ.

---

## [v1.3.0] - Tối ưu Độc quyền Fortnite Arena & Hệ thống Tay cầm Cảm ứng Pro
### ⚡ Tối ưu Độc quyền Fortnite (Fortnite Arena Mode)
- **Đệm khung hình 8ms Zero-Delay & Luồng 120 FPS:** Hạ bộ đệm WebRTC (`playoutDelayHint` & `jitterBufferTarget`) xuống `8ms` cho hình ảnh và `6ms` cho âm thanh, đồng thời thương lượng `a=framerate:120` trong SDP với máy chủ Xbox Series X cho Fortnite.
- **Chuyển đổi 1 chạm giữa Cảm ứng Epic gốc & Tay cầm FPS Pro:** Khi chọn `👆 Cảm ứng Epic`, hệ thống tự động ngắt kết nối tay cầm giả lập (`gamepaddisconnected`) để Fortnite hiển thị đầy đủ bộ nút cảm ứng xây nhà/bắn súng gốc của Epic Games mà không bị xung đột; khi chọn `🎮 Tay cầm FPS Pro`, tự động kích hoạt bố cục ngắm bắn (cần phải R lớn hơn +15% & Auto-Sprint L3).
- **Âm thanh Bước chân & Tiếng súng Định hướng (128kbps CBR Opus + FEC):** Nâng cấp luồng âm thanh Stereo Opus CBR 128kbps kèm chống mất gói In-Band FEC riêng cho Fortnite để nghe rõ tiếng bước chân và hướng giao tranh.
- **Cảm ứng đa điểm Claw 4 ngón chống trễ:** Vô hiệu hóa hoàn toàn cử chỉ trình duyệt gây trễ (`touch-action: none`) và tự động bỏ qua hộp thoại cảnh báo thiếu tay cầm khi khởi động Fortnite.

### 🎮 Cải tiến Hệ thống Điều khiển (Pro Control System - Chuẩn Fair-Play)
- **D-Pad trượt 8 hướng & ABXY trượt/combo đa điểm:** Hỗ trợ trượt ngón tay mượt mà qua 8 hướng (bao gồm 4 hướng chéo) trên D-Pad và vuốt/nhấn tổ hợp 2 nút liền kề trên cụm ABXY.
- **Bổ sung nút L3 / R3, Cò ga tuyến tính & Tự động chạy nhanh (Auto-Sprint L3):** Thêm nút L3/R3 chuyên dụng, cò ga/phanh tuyến tính (0–100%) trong chế độ Đua xe và tự kích hoạt chạy nhanh L3 khi đẩy kịch cần trái L.
- **Ngắm bắn bằng cảm biến Con quay hồi chuyển (Gyroscope):** Kết hợp nghiêng máy thật (`Sensor.TYPE_GYROSCOPE`) để tinh chỉnh tâm ngắm cần phải R mượt mà.
- **Bảng tùy chỉnh nhanh `⚙ Control` trong game & Kéo thả vị trí nút tự do:** Cho phép đổi nhanh 4 bố cục (`Xbox Standard`, `FPS Pro`, `Racing`, `Compact`), chỉnh kích thước (75%–135%), độ mờ, độ nhạy, vùng chết (Deadzone), cường độ rung và kéo thả từng cụm nút đến vị trí bất kỳ trên màn hình.

---

## [v1.2.0] - Hoàn thiện 100% Tiếng Anh, Tối ưu Hiệu năng & Giảm Độ trễ, Sửa lỗi Log & Cải tiến Xuất APK
### ✨ Tính năng & Trải nghiệm người dùng (UX & Localization)
- **Hoàn thiện 100% ngôn ngữ Tiếng Anh (Full English UI):** Dịch đầy đủ toàn bộ các màn hình (`HomeScreen`, `GameLibraryScreen`, `ServersScreen`, `SettingsScreen`), hộp thoại (`CustomProxyDialog`, `HowToUseGuideDialog`, `SmartTranslatorDialog`, `UserTermsAgreementDialog`), màn hình chờ vào game (`InGameLoadingOverlay`), thanh `FloatingHud`, thông báo trạng thái (`StreamController`, `AdaptiveQualityManager`, `NetworkOptimizer`), và thông báo nền (`XboxCloudVpnService`).
- **Tự động tắt dịch Tiếng Việt khi chọn Tiếng Anh:** Khi chọn ngôn ngữ ứng dụng là Tiếng Anh, bộ dịch trang web xCloud sang Tiếng Việt tự động ngắt hoàn toàn và khôi phục văn bản gốc tiếng Anh trên trang.
- **Nút nổi (Floating HUD) tự do & ẩn sát rìa màn hình:** Cho phép kéo di chuyển nút nổi không giới hạn trên màn hình; khi kéo sát mép trái hoặc phải sẽ tự động thu gọn thành thanh gờ mỏng (vuốt nhẹ vào trong hoặc chạm để mở lại).
- **Ẩn nhiệt độ máy ở dạng thu nhỏ:** Chỉ hiển thị nhiệt độ thiết bị khi mở bảng Cài đặt mở rộng của HUD để giữ thanh thu nhỏ gọn gàng.
- **Nhãn "Mở khóa vùng" thực tế cho từng máy chủ/proxy:** Kiểm tra nhanh vùng IP thoát (`GeoIpDetector`), học kết quả mở khóa thực tế từ phản hồi `/v2/login/user` và DOM trang Xbox (`Đã mở khóa` / `Unlocked`, `Không mở khóa` / `Unlock Failed`, `Chưa thử` / `Untested`, `Sai vùng` / `Wrong Region`), tự động sắp xếp ưu tiên máy chủ đã mở khóa và gợi ý chuyển máy chủ khi gặp lỗi vùng.

### ⚡ Tối ưu hiệu năng & Giảm độ trễ (Performance & Low Latency)
- **Loại bỏ việc nạp lặp lại script 65KB khi tải trang:** Ngăn `onProgressChanged` gọi `evaluateJavascript` liên tục 55 lần trong quá trình tải trang; chỉ tiêm script đúng 1 lần khi khởi tạo trang và cập nhật cấu hình qua hàm nhẹ `__updateNetworkProfile`.
- **Giảm tải quét DOM & Layout Reflow:** Giới hạn số lần kiểm tra văn bản khóa vùng (`checkRegionLockDomState`) tối đa 4 lần trên mỗi URL bằng `textContent` (không gây reflow như `innerText`) và dừng hoàn toàn khi luồng game WebRTC đã kết nối.

### 🐛 Sửa lỗi hệ thống trong Log (Log & Stability Fixes)
- **Khắc phục lỗi WebView CSP Console Error:** Ngừng gọi `fetch('https://api.country.is/')` trực tiếp bên trong trang `xbox.com` (vốn bị chặn bởi Content-Security-Policy của Microsoft), chuyển hoàn toàn việc dò IP sang tầng mạng gốc của Android (`GeoIpDetector`).
- **Khắc phục lỗi vòng đời Foreground Service (`XboxCloudVpnService`):** Ngăn lỗi `ForegroundServiceDidNotStartInTimeException` khi bật/tắt VPN liên tục bằng cách đảm bảo gọi `startForeground` an toàn trước khi dừng dịch vụ và xóa bỏ các cảnh báo log khi không có mạng.
- **Tối ưu đồng bộ từ điển GitHub (`XboxTranslator`):** Bỏ qua việc tải từ điển tiếng Việt từ GitHub khi đang dùng giao diện Tiếng Anh và chuyển thông báo ngoại tuyến sang `Log.d`.

---

## [v1.1.0] - Chế độ Máy yếu (Low-End Device Mode) & Tối ưu Bộ dịch Web
- Thêm công tắc **Chế độ máy yếu (Low-End Device Mode)** trong Cài đặt: khóa bitrate ~5 Mbps, ưu tiên codec H.264 nhẹ trong SDP, tắt hiệu ứng chuyển động vô hạn và tạm dừng bộ dịch khi chơi.
- Tự động dừng bộ dịch DOM ngay khi luồng video WebRTC bắt đầu phát để bảo toàn 60 FPS.

---

## [v1.0.0] - Phiên bản Khởi đầu (Initial Release)
- Ra mắt trình duyệt đám mây chuyên dụng cho Xbox Cloud Gaming với Smart VPN Bypass, Tay cầm ảo cảm ứng (Virtual Gamepad), Clarity Boost và Kho phím tắt game.
