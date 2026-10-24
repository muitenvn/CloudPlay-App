# Kế hoạch cải thiện ứng dụng CloudPlay

Dựa trên các hạng mục bạn đã chọn, kế hoạch dưới đây tập trung nâng cấp toàn diện cả 5 khu vực của ứng dụng theo hướng thực chất, gọn gàng, giảm độ trễ thực tế và đồng bộ ngôn ngữ trung tính với trang web chính thức.

---

## 1. Tối ưu Stream WebView & Tự động hạ giải khi mạng 4G/5G yếu
- **Tự động hạ độ phân giải & Bitrate thông minh trên 4G/5G (`AdaptiveQualityManager` & `QualityAdapter`):**
  - Khi phát hiện mạng di động 4G/5G có độ trễ cao (RTT > 95ms), dao động sóng (Jitter > 14ms), mất gói (> 1%) hoặc thời gian giải mã khung hình cao (Decode > 10ms), hệ thống không chỉ nới nhẹ bộ đệm chống giật mà còn chủ động hạ mượt mức Bitrate mục tiêu và độ phân giải luồng (1080p ➔ 900p ➔ 720p) ngay trong phiên hoặc khóa sẵn mức tối ưu cho luồng WebRTC.
  - Khi sóng 4G/5G hoặc Wi-Fi ổn định trở lại trong thời gian đủ dài, tự động nâng dần về mức sắc nét ban đầu mà không gây khựng hình (stutter).
- **Hiển thị rõ trạng thái Độ phân giải + Bitrate thực tế:**
  - Đồng bộ nhãn trạng thái theo chuẩn gộp chung (ví dụ: `720p Thấp • 5M`, `900p Vừa • 9M`, `1080p Cao • 14M`, `Tự động 720p ⇄ 1080p`) để người dùng thấy ngay hiệu quả giảm trễ khi bật 4G/5G hoặc Wi-Fi.

## 2. Thu gọn HUD trong game, Tay cầm ảo & Menu điều khiển nhanh
- **Thanh HUD thông số mạng gọn nhẹ (`FloatingHud`):**
  - Thiết kế lại chế độ thu gọn (Compact Pill) hiển thị trực quan trên 1 hàng duy nhất: `FPS • Ping (ms) • Độ phân giải/Bitrate`, không che khuất tầm nhìn trong game.
  - Cho phép chạm nhẹ để mở rộng bảng điều khiển nhanh hoặc thu nhỏ tức thì.
- **Menu điều khiển nhanh trong game (`CloudBrowserScreen`):**
  - Gom các nút thao tác nhanh (Đổi nhanh mức Độ phân giải & Đồ họa, Bật/tắt Tay cầm ảo, Bàn phím & Chuột KBM, Dịch màn hình, Quay màn hình) vào một bảng điều khiển gọn gàng, dễ bấm bằng ngón cái khi cầm ngang máy.

## 3. Cải thiện Trang chủ & Kho Game (Xbox Vault)
- **Trang chủ (`HomeScreen`):**
  - Làm gọn thẻ khởi chạy nhanh và thanh công cụ trạng thái ở đầu trang.
  - Bổ sung danh sách **"Chơi gần đây / Tiếp tục chơi"** ngay trên Trang chủ để vào thẳng game đang chơi dở chỉ với 1 chạm.
- **Kho Game (`GameLibraryScreen`):**
  - Cải thiện thanh tìm kiếm và bộ lọc thể loại nhanh (Hành động, Bắn súng, Đua xe, Thể thao, Nhập vai, Đám mây 60FPS) phản hồi tức thì, thêm nút xóa nhanh từ khóa tìm kiếm và hiển thị số lượng kết quả rõ ràng.

## 4. Nâng cấp Tab Mạng, Đo Ping máy chủ & Cài đặt
- **Tab Mạng (`ServersScreen`):**
  - Hiển thị rõ độ ổn định (Jitter) bên cạnh chỉ số Ping của từng cụm máy chủ châu Á (Nhật Bản, Hàn Quốc, Singapore...) và tự động gợi ý máy chủ có đường truyền tốt nhất hiện tại.
- **Tab Cài đặt (`SettingsScreen`):**
  - Sắp xếp lại các nhóm cài đặt (Chất lượng hình ảnh & Độ phân giải gộp chung, Điều khiển, Mạng & Máy chủ, Thông tin ứng dụng) rõ ràng, dễ hiểu, loại bỏ các mục trùng lặp.

## 5. Đồng bộ câu chữ trung tính & Thông tin với trang Web
- **Loại bỏ các cụm từ quảng cáo phóng đại trong toàn bộ ứng dụng:**
  - Rà soát và thay thế các câu chữ như *"120FPS thực"*, *"Zero-Delay 8ms"*, *"Bơm xung phần cứng"*, hay tên công cụ của bên thứ ba bằng ngôn ngữ kỹ thuật trung tính, rõ ràng (song ngữ Việt/Anh), khớp hoàn toàn với nội dung trên trang web `docs/index.html` và `assets/web/index.html`.
- **Cập nhật hộp thoại Hướng dẫn, Điều khoản & Cổng tải Web (`WebDownloadPortalDialog`, `UserTermsAgreementDialog`):**
  - Hiển thị thống nhất liên kết `muitenvn/CloudPlay-App`, cảnh báo rủi ro minh bạch (ứng dụng độc lập, không liên kết Microsoft/Xbox/Epic Games) và mô tả ngắn gọn.
