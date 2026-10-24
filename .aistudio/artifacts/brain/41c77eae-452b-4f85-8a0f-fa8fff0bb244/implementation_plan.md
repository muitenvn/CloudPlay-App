# Kế hoạch Triển khai: Trang Web Tải App Chuẩn SEO (Chung 1 Dự Án) & Gộp Tự Động Độ Phân Giải Vào Chất Lượng Đồ Họa

## 1. Mục tiêu tổng quan
Giải quyết trọn vẹn 2 yêu cầu của bạn ngay trong cùng 1 dự án **CloudPlay**:
1. **Tích hợp Trang Web Tải App chuẩn SEO + Cổng Chia sẻ trong App:**
   - Tạo bộ mã nguồn Web tĩnh chuẩn SEO Google (`docs/index.html`, `docs/sitemap.xml`, `docs/robots.txt`) nằm ngay trong dự án để khi đẩy lên GitHub sẽ tự động chạy thành website công khai qua **GitHub Pages**, giúp người dùng tìm kiếm trên Google thấy ngay trang giới thiệu và bấm nút tải APK.
   - Đồng bộ bản sao trang Web vào `app/src/main/assets/web/index.html` và bổ sung **Cổng Web & Chia Sẻ APK (QR Code + Link tải + Xem trước trang Web)** ngay trong ứng dụng để bạn có thể xem trực tiếp giao diện web, đổi link tải APK tùy chỉnh hoặc chia sẻ mã QR cho người khác quét tải ngay.
2. **Tự động hóa Độ phân giải & Gộp thẳng vào Chất lượng Đồ họa:**
   - Loại bỏ mục chọn **Độ phân giải dựng hình (`StreamRenderScale`)** và nút gạt **Độ phân giải 1080p Full HD** rời rạc.
   - Tích hợp sẵn độ phân giải tự động tương ứng vào từng mức **Chất lượng đồ họa** (`Tự động`, `Tiết kiệm`, `Trung bình`, `Cao`, `Tối đa`).

---

## 2. Chi tiết Thiết kế & Thay đổi

### Phần A: Gộp tự động Độ phân giải vào Chất lượng đồ họa
- **Cập nhật `AppSettings.kt` (`CellularOptimizationMode` & `SettingsRepository`):**
  - Mỗi chế độ Chất lượng đồ họa sẽ tự động điều khiển luôn cả Bitrate, Độ phân giải dựng hình (`StreamRenderScale`) và cấu hình luồng 1080p (`forceDesktopUserAgent`):
    - **Tự động (Khuyên dùng):** Tự động co giãn độ phân giải (`720p ⇄ 1080p`) và tốc độ mạng theo sóng Wi-Fi/4G và nhiệt độ GPU để giữ máy mát, chống trễ giải mã (`decode`).
    - **Tiết kiệm (5 Mbps):** Tự động khóa độ phân giải nhẹ `720p (Siêu mượt 75%)`, giảm tải GPU tối đa, siêu mát máy.
    - **Trung bình (9 Mbps):** Tự động dùng độ phân giải `900p HD+ (Cân bằng 85%)`, giữ 60 FPS ổn định và tiết kiệm pin.
    - **Cao (14 Mbps):** Tự động kích hoạt `1080p Full HD (100% sắc nét)`.
    - **Tối đa (20 Mbps):** Tự động kích hoạt `1080p Full HD+ (Bung tối đa bitrate & chi tiết)`.
- **Cập nhật `SettingsScreen.kt` & `FloatingHud.kt`:**
  - Xóa bỏ danh sách chọn **Độ phân giải dựng hình** (`StreamRenderScale`) thủ công ở mục `1.2` và xóa công tắc **Độ phân giải 1080p Full HD** ở mục `1.3`.
  - Gom gọn các công tắc hỗ trợ phần cứng (`Ưu tiên phản hồi mượt`, `Giảm tải GPU`, `Chế độ máy yếu`) ngay bên dưới thẻ **Chất lượng đồ họa & FPS**.
  - Hiển thị rõ độ phân giải đi kèm ngay trên nhãn của từng mức Chất lượng đồ họa (ví dụ: `Tiết kiệm • 720p`, `Trung bình • 900p`, `Cao • 1080p`, `Tối đa • 1080p Max`).

---

### Phần B: Bộ Trang Web Tải Ứng Dụng Chuẩn SEO (`docs/` & `assets/web/`)
- **Tạo trang Landing Page `docs/index.html` (đồng bộ vào `app/src/main/assets/web/index.html`):**
  - **Tối ưu SEO Google Search & Chia sẻ mạng xã hội:**
    - Đầy đủ thẻ `<title>`, `<meta name="description">`, `<meta name="keywords">`, OpenGraph (`og:title`, `og:description`, `og:type`), và dữ liệu có cấu trúc `JSON-LD (SoftwareApplication)` để Google hiển thị kết quả tìm kiếm kèm thông tin phiên bản, dung lượng và nút tải.
    - Kèm sẵn file `docs/robots.txt` và `docs/sitemap.xml`.
  - **Giao diện & Nội dung trên trang Web (Phong cách Xbox Neon Dark):**
    - **Banner chính & Nút Tải APK thông minh:** Tự động lấy bản phát hành `.apk` mới nhất từ GitHub Releases API (hoặc dẫn thẳng tới link APK tùy chỉnh do bạn cấu hình), hiển thị rõ tên file, phiên bản, dung lượng và mã kiểm tra SHA-256.
    - **Điểm nhấn công nghệ:** Luồng 120 FPS & Đệm khung hình 8ms Zero-Delay, Tối ưu giải mã phần cứng giảm Ping/Decode, Chuyển đổi 1 chạm Cảm ứng Epic / Tay cầm FPS Pro cho Fortnite, Âm thanh định hướng 128kbps Opus + FEC.
    - **Nhật ký bản cập nhật (Changelog) & Hướng dẫn cài đặt đè không mất dữ liệu.**

---

### Phần C: Cổng Web & Chia Sẻ Nhanh Ngay Trong Ứng Dụng
- **Tạo hộp thoại / màn hình `WebDownloadPortalDialog.kt` truy cập nhanh từ thanh trên cùng của `DashboardScreen` và trong `SettingsScreen`:**
  - **Cấu hình Link tải APK tùy chỉnh & GitHub Repo:** Cho phép bạn nhập link tải APK trực tiếp (GitHub Releases, Google Drive, hoặc link riêng) để cập nhật ngay cho mã QR và nút Chia sẻ.
  - **Mã QR Tải App Trực Tiếp (Vẽ bằng Compose Canvas):** Tự động sinh mã QR sắc nét từ link trang Web / link tải APK để bạn bè hoặc người dùng quét tải ngay lập tức.
  - **Xem trước Trang Web Chuẩn SEO ngay trong App:** Tích hợp chế độ xem trực tiếp trang Web tải ứng dụng (`assets/web/index.html`) ngay trong ứng dụng và nút **Sao chép Link / Chia sẻ nhanh**.
