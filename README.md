# 🎮 CloudPlay - Xbox Cloud Gaming Booster & Client

<p align="center">
  <img src="https://img.shields.io/github/v/release/muitenvn/CloudPlay-App?color=00c853&label=Latest%20Release&style=for-the-badge" alt="Release Badge" />
  <img src="https://img.shields.io/github/actions/workflow/status/muitenvn/CloudPlay-App/build-and-release-apk.yml?branch=main&label=Build%20Status&style=for-the-badge" alt="Build Badge" />
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-blue?style=for-the-badge&logo=android" alt="Android Badge" />
  <img src="https://img.shields.io/badge/Ping%20Vietnam-25%E2%80%9335ms-brightgreen?style=for-the-badge" alt="Ping Badge" />
</p>

---

## 📖 Giới thiệu (Overview)

**CloudPlay** là ứng dụng Android chuyên dụng tối ưu hóa trải nghiệm chơi game đám mây **Xbox Cloud Gaming (xCloud)** dành cho game thủ Việt Nam và Đông Nam Á. 

Ứng dụng tích hợp công nghệ **Direct Handshake Bypass** độc quyền, cho phép vượt qua rào cản khu vực (Region Lock) để truy cập máy chủ xCloud Nhật Bản (Japan), Hàn Quốc (Korea), Mỹ (US)... sau đó tự động chuyển về luồng truyền mạng trực tiếp của thiết bị. Nhờ đó, bạn đạt được độ trễ cực thấp (**chỉ từ ~25 - 35ms**) và hình ảnh sắc nét chuẩn 60 FPS mà **không cần duy trì kết nối VPN liên tục hay cài đặt app VPN thứ 3**.

---

## ✨ Tính năng nổi bật (Key Features)

- ⚡ **Direct Handshake Bypass (Không cần VPN rườm rà)**:
  - Tự động bắt tay và khởi tạo phiên chơi qua máy chủ tối ưu (Nhật Bản, Hàn Quốc, Mỹ...).
  - Tự động chuyển về mạng trực tiếp nội địa (Direct Stream) khi luồng video bắt đầu, giúp đường truyền không bị bóp băng thông và ping thấp nhất có thể (~25 - 35ms).
- 🎮 **Floating Gaming HUD (Thanh công cụ nổi thông minh)**:
  - **Chế độ thu nhỏ (Minimized Pill):** Thiết kế dạng viên thuốc siêu gọn gàng, hiển thị ping mạng thời gian thực (`⚡29ms`) và icon tay cầm (`🎮`), hoàn toàn không che khuất màn hình chơi game (đã loại bỏ các nút thừa). Chạm vào viên thuốc để mở rộng menu.
  - **Chế độ mở rộng (Expanded / Compact):** Đầy đủ các nút điều hướng tiện lợi như **Home** ("Về Trang Chủ"), xoay màn hình (Landscape/Portrait), xem chi tiết tình trạng kết nối mạng và đổi server nhanh.
- 🚀 **Hiệu năng cao & Tiết kiệm pin (High Performance)**:
  - Giao diện xây dựng hoàn toàn bằng **Jetpack Compose (Material 3)** mượt mà, hiện đại.
  - Tối ưu bộ tăng tốc phần cứng WebKit WebView, tối thiểu hóa độ trễ cảm ứng và giật khung hình.
- 🛡️ **Bảo mật & Riêng tư tuyệt đối**:
  - Đăng nhập trực tiếp trên cổng xác thực chính thức của Microsoft Xbox.
  - Không thu thập thông tin đăng nhập, mật khẩu hay dữ liệu cá nhân của người dùng.

---

## 📥 Hướng dẫn Tải và Cài đặt APK (Download APK)

Bạn có thể tải file APK cài đặt bằng một trong hai cách dưới đây:

### 🌟 Cách 1: Tải trực tiếp từ mục Releases (Khuyên dùng - Nhanh nhất)

👉 **Truy cập ngay:** **[Tải CloudPlay APK mới nhất (Releases)](https://github.com/muitenvn/CloudPlay-App/releases/latest)**

1. Nhấp vào đường link trên (hoặc bấm vào mục **Releases** ở cột bên phải trang GitHub).
2. Cuộn xuống phần **Assets** ở cuối bản phát hành mới nhất.
3. Nhấp vào file **`CloudPlay-Xbox-v1.0.apk`** để tải trực tiếp về điện thoại Android.
4. Mở file vừa tải về và chọn **Cài đặt** (Cho phép cài đặt ứng dụng không rõ nguồn gốc nếu điện thoại yêu cầu).

---

### 🛠️ Cách 2: Tải từ mục GitHub Actions (Bản build tự động mới nhất)

Nếu muốn trải nghiệm bản build mới nhất theo từng commit mới:
1. Truy cập tab **[Actions](https://github.com/muitenvn/CloudPlay-App/actions)** ở menu phía trên repository.
2. Chọn workflow chạy gần nhất có dấu tích xanh ✅ (ví dụ: *"Build & Release Android APK"*).
3. Cuộn xuống mục **Artifacts** ở cuối trang.
4. Nhấp vào **`CloudPlay-Xbox-APK`** để tải file nén chứa APK đã biên dịch sẵn.

---

## 💡 Mẹo chơi game xCloud mượt nhất tại Việt Nam

1. **Kết nối mạng:**
   - Khuyến khích sử dụng băng tần Wi-Fi **5GHz** hoặc mạng di động **4G/5G** có tín hiệu ổn định.
   - Hạn chế chơi trên Wi-Fi 2.4GHz vì dễ bị nhiễu sóng và tăng jitter.
2. **Chọn máy chủ:**
   - Ưu tiên chọn cụm server **Nhật Bản (Japan)** hoặc **Hàn Quốc (Korea)** trong cài đặt ứng dụng để có khoảng cách địa lý gần nhất và ping tối ưu nhất (~25ms đến 40ms).
3. **Tay cầm điều khiển (Controller):**
   - Ứng dụng hỗ trợ hoàn hảo tất cả các tay cầm Bluetooth tiêu chuẩn: Xbox Wireless Controller, PlayStation DualSense / DualShock 4, Gamesir, Razer Kishi, v.v.

---

## ⚙️ Dành cho lập trình viên & Tự Build (For Developers)

### 1. Tự biên dịch cục bộ (Local Build)
Yêu cầu: Android SDK 36 (hoặc 35+), JDK 21.

```bash
# Clone repository
git clone https://github.com/muitenvn/CloudPlay-App.git
cd CloudPlay-App

# Cấp quyền thực thi cho gradlew
chmod +x gradlew

# Biên dịch file APK Debug
./gradlew assembleDebug
```
File APK sau khi build nằm tại: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Cấu hình GitHub Actions khi Fork repository
Nếu bạn Fork repo này về tài khoản cá nhân và muốn GitHub Actions tự động build ra file APK trong mục Releases:
1. Vào **Settings** của repository trên GitHub của bạn.
2. Chọn menu **Actions** -> **General**.
3. Cuộn xuống mục **Workflow permissions**:
   - Chọn **Read and write permissions**.
   - Tích chọn **Allow GitHub Actions to create and approve pull requests**.
4. Nhấn **Save**.
5. Vào lại tab **Actions** -> bấm vào workflow và chọn **Run workflow**.

---

## ☕ Ủng hộ tác giả (Donate)

Nếu ứng dụng giúp bạn có những giờ phút giải trí chơi game mượt mà hơn, bạn có thể mời tác giả một ly cà phê nhé! Cảm ơn sự đồng hành của bạn!

* **Ngân hàng:** MB Bank (Ngân hàng TMCP Quân Đội)
* **Chủ tài khoản:** NGUYEN THE NGOC
* **Số tài khoản:** `0385351613`

<p align="center">
  <img src="https://raw.githubusercontent.com/muitenvn0990/bombanana/main/Screenshot_20261012_133457.jpg" alt="MB Bank QR Code" width="280px" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
</p>

---

## 📄 Bản quyền & Tuyên bố từ chối trách nhiệm (Disclaimer)

- **CloudPlay** là ứng dụng mã nguồn mở độc lập được phát triển vì mục đích học tập và tối ưu hóa trải nghiệm cộng đồng game thủ.
- **Xbox**, **Xbox Cloud Gaming**, **Xbox Game Pass** và các thương hiệu liên quan là tài sản thuộc bản quyền của **Microsoft Corporation**. Ứng dụng này không có liên kết chính thức hay được ủy quyền bởi Microsoft.
