# Kế Hoạch Đơn Giản Hóa Ngôn Ngữ Người Dùng & Ẩn Các Tùy Chỉnh Kỹ Thuật

## 1. Mục tiêu
Chuyển toàn bộ câu chữ trong ứng dụng từ giọng văn kỹ thuật / lập trình viên sang **ngôn ngữ người dùng phổ thông, giải thích ngắn gọn**, đồng thời **ẩn các ô chỉnh sửa chỉ dành cho lập trình viên** theo đúng lựa chọn của bạn.

---

## 2. Ẩn & Rút gọn các tùy chỉnh dành cho lập trình viên

### 2.1. Hộp thoại Chia sẻ ứng dụng & Mã QR (`SettingsScreen.kt` & `WebDownloadPortalDialog.kt`)
- **Ẩn hoàn toàn Tab "Đổi Link & SEO"** (ô nhập GitHub Repo Slug, ô sửa link APK, ô sửa URL web, hướng dẫn cấu hình GitHub Pages `/docs/index.html`).
- **Đổi tên mục trong Cài đặt:** Từ *"Trang Web Tải App (SEO) & Chia Sẻ QR"* kèm mô tả `/docs/index.html` thành **"Chia sẻ ứng dụng & Mã QR"** (*"Quét mã QR hoặc gửi link tải ứng dụng cho bạn bè"*).
- **Rút gọn hộp thoại Chia sẻ:** Chỉ giữ mã QR, link trang web tải ứng dụng, nút Sao chép link và nút Chia sẻ nhanh (kèm tab xem trước trang giới thiệu nếu cần). Không nhắc đến SEO, `/docs`, hay GitHub Pages.

### 2.2. Mục Vùng ngôn ngữ trang Xbox (`SettingsScreen.kt`)
- **Ẩn ô nhập văn bản thủ công** (`Mã vùng hoặc link Xbox` / `xbox.com/en-US` và 2 nút *Áp dụng* / *Đặt en-US*).
- Chỉ giữ danh sách chọn nhanh bằng 1 chạm: **Mặc định (Tiếng Anh - Mỹ)** và các ngôn ngữ phổ biến (Anh, Nhật, Hàn, Trung...).

### 2.3. Mục Quay màn hình (`SettingsScreen.kt`)
- **Ẩn phần chọn công cụ ghi hình kỹ thuật** (`RecordingCaptureEngine` / WebView Canvas vs MediaProjection).
- Chỉ giữ lựa chọn **Độ nét video** (`720p Tiết kiệm` / `1080p Sắc nét`) và công tắc **Giữ game mượt khi đang quay**.

---

## 3. Viết lại câu chữ ngắn gọn, dễ hiểu cho người dùng phổ thông

### 3.1. Cài đặt & Cấu hình (`SettingsScreen.kt`, `AppSettings.kt`, `NetworkOptimizer.kt`)
- Thay các thuật ngữ kỹ thuật bằng giải thích ngắn gọn:
  - *"Giảm tải GPU / Tắt hiệu ứng nền nặng"* ➔ **"Giảm hiệu ứng nền"** (*"Giúp máy đỡ nóng và đỡ giật hơn"*).
  - *"Làm nét hình ảnh (Clarity Boost)"* ➔ **"Tăng độ nét hình ảnh"** (*"Làm rõ chi tiết nhân vật và khung cảnh"*).
  - *"Vùng chết tâm cần (Deadzone)"* ➔ **"Độ trễ tâm cần xoay"** (*"Chỉnh độ nhạy khi bắt đầu đẩy cần"*).
  - *"Giải mã phần cứng GPU dưới 4ms / WebRTC SDP / Playout Buffer"* ➔ **"Phản hồi nhanh, tiết kiệm pin và dữ liệu di động"**.

### 3.2. Kho Game & Trang chủ (`GameLibraryScreen.kt`, `HomeScreen.kt`, `GameOptimizationProfile.kt`)
- Đổi *"KHO GAME XBOX CLOUD VAULT"* ➔ **"KHO GAME XBOX CLOUD"**.
- Đổi các dòng nhắc kỹ thuật như *"Đang tự động tải thông tin game (Không tải ảnh, 0 tốn dung lượng ảnh / 0 MB image data)"* ➔ **"Đang cập nhật danh sách game..."** và **"Chạm để tìm thêm trên Xbox Store"**.
- Đổi nút *"Quét TT / Sync Info"* ➔ **"Làm mới"**.
- Trong hồ sơ game (`GameOptimizationProfile.kt`): Bỏ các từ kỹ thuật như *"WebRTC Playout"*, *"Opus 128kbps CBR + In-Band FEC"*, *"SDP"*; thay bằng mô tả ngắn gọn: **"Giảm độ trễ thao tác"**, **"Âm thanh bước chân rõ nét"**, **"Chuyển nhanh Cảm ứng & Tay cầm"**.

### 3.3. Màn hình Chơi game & Thanh HUD (`FloatingHud.kt`, `CloudBrowserScreen.kt`)
- Trong bảng điều khiển nhanh và thông số mở rộng:
  - Đổi *"Decode"* ➔ **"Xử lý hình"**; *"Packet Loss"* ➔ **"Mất gói"**; *"Jitter"* ➔ **"Độ ổn định"**.
  - Các thông báo trạng thái mạng ghi ngắn gọn: **"Mạng ổn định"**, **"Mạng yếu – Đã tự hạ độ nét để giữ mượt"**.

---

## 4. Kiểm tra & Xác nhận
- Giữ nguyên toàn bộ tính năng cốt lõi (tự động hạ giải 4G/5G, VPN thông minh, tay cầm ảo, dịch tiếng Việt, lọc game).
- Biên dịch ứng dụng (`compile_applet`) đảm bảo không phát sinh lỗi.
