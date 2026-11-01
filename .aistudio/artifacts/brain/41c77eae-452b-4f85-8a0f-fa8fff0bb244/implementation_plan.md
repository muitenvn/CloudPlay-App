# Kế hoạch Cập nhật CloudPlay v1.3.0 (Từng bước)

Kế hoạch chi tiết từng bước cho đợt cập nhật tính năng mới **v1.3.0**, tập trung vào 3 trọng tâm chính: hoàn thiện văn bản Điều khoản sử dụng, cải thiện trải nghiệm điều khiển tay cầm ảo & luồng phát Xbox, và chuẩn bị quy trình phát hành chính thức lên GitHub Releases.

---

## Ý kiến người dùng & Quyết định cốt lõi

> [!IMPORTANT]
> Dưới đây là các quyết định đã xác nhận cùng các mục cần thông tin thực tế từ bạn trước khi thực hiện chỉnh sửa:

- **Phiên bản mục tiêu:** Nâng cấp từ `v1.2.x` lên `v1.3.0` (Bản cập nhật tính năng mới).
- **Trọng tâm 1 - Điều khoản sử dụng:** Thay thế toàn bộ các placeholder `[ĐIỀN: ...]` bằng thông tin thực tế:
  - *Tên nhà phát triển/nhóm:* Mặc định đề xuất: `muitenvn (CloudPlay Project Contributors)`.
  - *Email liên hệ:* Mặc định đề xuất: `https://github.com/muitenvn/CloudPlay-App/issues` hoặc email do bạn cung cấp.
  - *Độ tuổi tối thiểu:* Mặc định: `13 tuổi` (phù hợp chính sách tài khoản Microsoft & ESRB).
  - *Luật áp dụng & Tòa án:* Mặc định: `Pháp luật Việt Nam` / Tòa án có thẩm quyền tại Việt Nam.
  - *Ngôn ngữ ưu tiên:* `Tiếng Việt` (bản gốc giải thích).
- **Trọng tâm 2 - Tay cầm ảo & Luồng phát Xbox:**
  - Bổ sung thanh trượt tùy chỉnh **Độ mờ / Độ trong suốt (Opacity 10% - 100%)** và **Kích thước nút bấm (Scale 80% - 130%)** cho tay cầm ảo ngay trên HUD điều khiển nhanh.
  - Tối ưu rung phản hồi haptic khi chạm nút ảo (giảm delay rung 15ms xuống mức tức thì).
  - Cải tiến thông số đàm phán luồng WebRTC H.264 constrained baseline giúp giảm thời gian giải mã khung hình (decode time) và chống giật hình khi chuyển cảnh nhanh.
- **Trọng tâm 3 - Quy trình phát hành GitHub Releases:**
  - Cập nhật số phiên bản `1.3.0` trong `build.gradle.kts`.
  - Tự động đồng bộ tạo tệp chữ ký băm `CloudPlay-v1.3.0.apk.sha256`.
  - Cập nhật mẫu ghi chú phát hành (Release Notes) song ngữ tóm tắt các tính năng mới cho GitHub Releases.

---

## 1. Tổng quan & Mục tiêu

- **Mục đích:** Cung cấp bản phát hành `v1.3.0` ổn định, hoàn thiện đầy đủ tính pháp lý minh bạch, đem lại trải nghiệm vuốt chạm tay cầm ảo mượt mà không che khuất màn hình game và tối ưu đường truyền hình ảnh Cloud Gaming.
- **Đối tượng sử dụng:** Game thủ Android chơi Xbox Cloud Gaming (Fortnite, Game Pass Ultimate) bằng màn hình cảm ứng điện thoại hoặc máy tính bảng.

---

## 2. Trải nghiệm người dùng & Giao diện (UX/UI)

### 2.1. Tùy biến tay cầm ảo (Virtual Gamepad Overlay)
- **Menu cài đặt nhanh (HUD Trong Game):** Khi mở HUD góc màn hình, người chơi có thể:
  - Điều chỉnh thanh trượt **Độ trong suốt nút bấm (Opacity)** từ 20% (mờ nhẹ không che tầm nhìn) đến 100% (nét rõ).
  - Điều chỉnh **Kích thước nút bấm (Scale)** nhỏ/vừa/lớn phù hợp cho cả màn hình nhỏ 5.5 inch và máy tính bảng 11 inch.
  - Bật/tắt chế độ **Haptic Feedback** (rung nhẹ khi chạm nút ảo A/B/X/Y, D-Pad và Triggers L2/R2).

### 2.2. Tối ưu chất lượng hiển thị luồng phát
- Giữ vững thiết kế tối giản Neon Green (`#107C10`, `#00FF5A`) đồng bộ Material 3.
- Bổ sung nút chuyển nhanh bộ lọc hình ảnh: **Sắc nét (Clarity boost)** / **Mặc định** / **Mượt mà (Smooth)** trực tiếp trên thanh điều khiển nhanh.

---

## 3. Quy trình thực hiện từng bước (Step-by-Step Execution Plan)

### Bước 1: Hoàn thiện các mục `[ĐIỀN]` trong Điều khoản sử dụng
1. Điền chính xác thông tin vận hành vào `docs/terms.html`:
   - Ngày cập nhật mới nhất.
   - Chủ thể: `muitenvn (CloudPlay Project)`.
   - Độ tuổi tối thiểu: `13 tuổi` (có sự giám sát của phụ huynh đối với game 16+/18+).
   - Luật áp dụng: Pháp luật Việt Nam, ngôn ngữ ưu tiên là Tiếng Việt.
   - Kênh liên hệ: GitHub Issues chính thức và email tiếp nhận phản hồi.
2. Cập nhật đồng bộ cả bản Tiếng Việt và Tiếng Anh trong tệp `terms.html`.

### Bước 2: Nâng cấp Tay cầm ảo & Trải nghiệm luồng phát (Web Script & Compose)
1. Trong `AppSettings.kt`: Bổ sung cấu hình lưu trữ `virtualControllerOpacity` (mặc định 65%) và `virtualControllerScale` (mặc định 100%).
2. Trong script ảo hóa tay cầm (`virtual gamepad script` trong `CloudBrowserScreen.kt`):
   - Nhận thông số opacity & scale động từ Compose để render layout CSS linh hoạt.
   - Thêm bộ đệm xử lý đa điểm cảm ứng (multi-touch handling) mượt mà cho cần analog và phím ngắm bắn đồng thời.
3. Trong `StreamController.kt` / SDP Filter: Tinh chỉnh ưu tiên codec video H.264 profile `42e01f` giúp tối ưu bộ giải mã phần cứng MediaCodec trên thiết bị Android, giảm hiện tượng tụt FPS.

### Bước 3: Nâng số phiên bản lên v1.3.0
1. Cập nhật `app/build.gradle.kts`:
   - Nâng `versionName = "1.3.0"`.
   - Đảm bảo `versionCode` tăng tự động không xung đột khi cài đè.
2. Cập nhật số phiên bản hiển thị trong `SettingsScreen.kt` và trang tải web `docs/index.html`.

### Bước 4: Chuẩn bị quy trình phát hành GitHub Releases v1.3.0
1. Biên dịch và kiểm thử toàn diện bằng lệnh `compile_applet` và kiểm thử tự động JVM Robolectric (`gradle testDebugUnitTest`).
2. Tạo mẫu tệp `RELEASE_NOTES_v1.3.0.md` chuẩn bị sẵn nội dung:
   - Tóm tắt tính năng mới (Tay cầm ảo chỉnh mờ/phóng to, luồng phát mượt hơn).
   - Khắc phục lỗi và hoàn thiện trang điều khoản minh bạch.
   - Hướng dẫn kiểm tra mã SHA-256 sau khi tải file APK.

---

## 4. Kiến trúc kỹ thuật & Sơ đồ luồng (Architecture Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│                     CloudPlay v1.3.0                        │
└─────────────────────────────────────────────────────────────┘
                               │
       ┌───────────────────────┼────────────────────────┐
       ▼                       ▼                        ▼
┌──────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   Bước 1:    │      │     Bước 2:     │      │   Bước 3 & 4:   │
│ docs/terms   │      │ Virtual Gamepad │      │ Version Bump &  │
│  Hoàn thiện  │      │ & Stream Engine │      │ Release Engine  │
└──────────────┘      └─────────────────┘      └─────────────────┘
       │                       │                        │
       ▼                       ▼                        ▼
• Xóa [ĐIỀN]           • Opacity (20-100%)      • versionName 1.3.0
• Tên & liên hệ        • Scale (80-130%)        • SHA-256 Checksum
• Luật áp dụng VN      • Haptic touch low-delay • GitHub Releases
• Song ngữ VI / EN     • H.264 WebRTC decode    • Multi-mirror APK
```
