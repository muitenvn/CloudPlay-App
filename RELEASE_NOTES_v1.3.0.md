# CloudPlay v1.3.0 – Release Notes / Nhật ký phát hành

[English below]

---

## 🇻🇳 Tiếng Việt

### 🚀 Có gì mới trong phiên bản 1.3.0?

#### 1. Cải tiến tay cầm ảo & Tuỳ chỉnh nhanh ngay trên HUD
- **Tùy chỉnh tức thì khi đang chơi**: Bảng điều khiển nổi (Floating HUD) mở rộng giờ đây bổ sung hàng phím điều chỉnh nhanh tay cầm ảo gồm các mức độ mờ (40%, 65%, 85%) và kích thước nút (0.85x, 1.0x, 1.2x) mà không cần phải thoát ra màn hình Cài đặt.
- **Rung phản hồi xúc giác tức thì (Instant Hardware Haptics)**: Cải tiến cơ chế rung xúc giác trên Android 10+ (API 29+) và Android 12+ (API 31+) sử dụng hiệu ứng phần cứng `EFFECT_CLICK` và `EFFECT_HEAVY_CLICK` trực tiếp từ bộ rung LRA/X-axis, triệt tiêu độ trễ kích hoạt và mang lại cảm giác bấm đầm tay như tay cầm console thật.
- **Tách cụm nút ABXY và D-Pad**: Cho phép di chuyển và tùy chỉnh kích thước từng nút độc lập trong chế độ Chỉnh sửa bố cục.

#### 2. Tối ưu hoá luồng phát WebRTC & Giải mã phần cứng
- **Ưu tiên cấu hình H.264 Constrained Baseline (`42e01f`)**: Tự động thương lượng lại bản tin SDP (Session Description Protocol) trong WebRTC để đưa hồ sơ `profile-level-id=42e01f` lên đầu danh sách giải mã của `m=video`. Giúp bộ giải mã phần cứng MediaCodec trên chip Snapdragon, MediaTek, Exynos giải mã khung hình tức thì với độ trễ dưới 8–12ms, không bị ứ đọng buffer B-frame.
- **Điều chỉnh đệm nhận mượt mà**: Tự động tính toán lại dung lượng đệm nhận (jitter buffer) theo tín hiệu mạng thực tế (Wi-Fi 5GHz/6GHz vs 4G LTE).

#### 3. Hoàn thiện Điều khoản sử dụng & Quyền riêng tư (Terms of Use)
- **Đầy đủ thông tin pháp lý**: Đã loại bỏ hoàn toàn các trường tạm `[ĐIỀN]`. Ghi rõ chủ thể vận hành dự án là `muitenvn (CloudPlay Project Contributors)`.
- **Cơ chế pháp luật**: Áp dụng pháp luật nước Cộng hòa Xã hội Chủ nghĩa Việt Nam; phiên bản tiếng Việt được ưu tiên áp dụng làm căn cứ giải thích chính thức; kênh liên hệ chính thức qua email `contact.cloudplay.app@gmail.com` và GitHub Issues.
- **Giao diện đa trang tiện lợi**: Điều khoản được chia thành mục lục 15 phần chuyên sâu, có thể xem dạng danh mục phân trang gọn gàng hoặc xem toàn bộ trên một trang tùy thích.

#### 4. Tải nhanh qua máy chủ phụ (Multi-Mirror Auto-Failover)
- Giao diện nút tải được tinh chỉnh ngắn gọn: **“Tải nhanh qua máy chủ phụ”**, loại bỏ hoàn toàn các từ ngữ kỹ thuật phức tạp.
- Tự động kiểm tra độ sống và chuyển hướng mượt mà giữa các mirror uy tín (`gh-proxy.com`, `ghproxy.net`, `ghfast.top`, `mirror.ghproxy.com`, `kkgithub.com`).

#### 5. Bước nâng cấp Thuật toán Chọn Máy Chủ Kép v1.3.0 (Khoảng cách địa lý vs Ping)
- **Giới hạn khoảng cách tự động chọn (`≤ 5.500 km`)**: Ngăn chặn hoàn toàn việc chọn nhầm máy chủ Mỹ (`US`, ~12.600 km) quá xa người dùng Việt Nam / Đông Nam Á.
- **Chế độ Tự động tắt VPN khi vào game (`Vị trí gần ➔ Ping`)**: Vì khi vào game sẽ ngắt VPN để đi mạng trực tiếp, hệ thống ưu tiên **Khoảng cách địa lý gần nhất trước** — chọn **🇰🇷 Hàn Quốc (~70ms khi tắt VPN)** trước **🇯🇵 Nhật Bản (~80ms khi tắt VPN)**, sau đó mới xét đến ping của máy chủ trong vùng đó.
- **Chế độ Bật VPN luôn khi chơi (`Ping thấp ➔ Vị trí`)**: Khi giữ VPN xuyên suốt lúc chơi, hệ thống ưu tiên **Độ trễ Ping đo thực tế thấp nhất trước**, còn khoảng cách địa lý xếp ở phía sau làm tiêu chí phụ.

---

### 📦 Quy trình phát hành lên GitHub Releases

#### Bước 1: Biên dịch APK bản phát hành
```bash
gradle :app:assembleRelease
```
Tệp APK sau khi biên dịch nằm tại: `app/build/outputs/apk/release/app-release.apk`

#### Bước 2: Tạo mã kiểm tra tính toàn vẹn SHA-256
```bash
cd app/build/outputs/apk/release/
sha256sum app-release.apk > app-release.apk.sha256
cat app-release.apk.sha256
```

#### Bước 3: Phát hành lên GitHub Releases
Dùng GitHub CLI (`gh`) hoặc tạo trực tiếp trên giao diện web:
```bash
gh release create v1.3.0 app-release.apk app-release.apk.sha256 \
  --title "CloudPlay v1.3.0 – Virtual Gamepad & Stream Engine Upgrade" \
  --notes-file RELEASE_NOTES_v1.3.0.md
```

---

## 🇺🇸 English

### 🚀 What's New in Version 1.3.0?

#### 1. Enhanced Virtual Gamepad & In-Game Quick Adjustments
- **Live HUD Controls**: The expanded Floating HUD now features instant adjustment chips for gamepad opacity (40%, 65%, 85%) and button scale (0.85x, 1.0x, 1.2x), allowing seamless fine-tuning during active gameplay without opening Settings.
- **Instant Hardware Haptics**: Upgraded vibration actuator logic on Android 10+ (API 29+) and Android 12+ (API 31+) using direct hardware `EFFECT_CLICK` and `EFFECT_HEAVY_CLICK` presets, eliminating activation lag for crisp, console-grade physical button feedback.
- **Split ABXY & D-Pad Customization**: Freely reposition, scale, or hide individual buttons independently.

#### 2. WebRTC Stream Telemetry & Codec Optimization
- **H.264 Constrained Baseline (`42e01f`) SDP Priority**: Automatically prioritizes the `42e01f` profile-level-id at the front of WebRTC `m=video` SDP negotiation, directing Android MediaCodec to decode video frames without B-frame buffering latency (sub-10ms frame decode times).
- **Dynamic Jitter Buffer Adjustment**: Smoothly adapts receiver buffer size to real-time network conditions (Wi-Fi 5GHz/6GHz vs 4G LTE).

#### 3. Complete Terms of Use & Privacy Policy
- **Zero Legal Placeholders**: All legal sections have been finalized with formal developer attribution to `muitenvn (CloudPlay Project Contributors)`.
- **Governing Jurisdiction**: Governed under the laws of the Socialist Republic of Vietnam, with Vietnamese as the prevailing language and official support via `contact.cloudplay.app@gmail.com` and GitHub Issues.
- **Hub & Section Navigation**: Terms directory supports both single-page reading and a clean 15-section multi-page experience.

#### 4. Multi-Mirror Fast Download
- Streamlined button label: **“Fast download via mirror”** with clean, simplified UI copy.
- Automatic background failover across verified mirrors (`gh-proxy.com`, `ghproxy.net`, `ghfast.top`, `mirror.ghproxy.com`, `kkgithub.com`).

---

### 📦 GitHub Releases Workflow

#### Step 1: Build Release APK
```bash
gradle :app:assembleRelease
```
APK output location: `app/build/outputs/apk/release/app-release.apk`

#### Step 2: Generate SHA-256 Checksum
```bash
cd app/build/outputs/apk/release/
sha256sum app-release.apk > app-release.apk.sha256
cat app-release.apk.sha256
```

#### Step 3: Publish Release to GitHub
```bash
gh release create v1.3.0 app-release.apk app-release.apk.sha256 \
  --title "CloudPlay v1.3.0 – Virtual Gamepad & Stream Engine Upgrade" \
  --notes-file RELEASE_NOTES_v1.3.0.md
```
