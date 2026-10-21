# Nhật ký cập nhật / Update Changelog (CloudPlay Xbox)

Tài liệu này ghi nhận chi tiết các thay đổi thực tế của từng bản cập nhật và được hệ thống xuất APK tự động trích xuất vào trang phát hành (GitHub Releases).

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
