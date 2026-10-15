# 🎮 CloudPlay - Xbox Cloud Gaming Booster & Client

<p align="center">
  <img src="https://img.shields.io/github/v/release/muitenvn/CloudPlay-App?color=00c853&label=Latest%20Release&style=for-the-badge" alt="Release Badge" />
  <img src="https://img.shields.io/github/actions/workflow/status/muitenvn/CloudPlay-App/build-and-release-apk.yml?branch=main&label=Build%20Status&style=for-the-badge" alt="Build Badge" />
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-blue?style=for-the-badge&logo=android" alt="Android Badge" />
  <img src="https://img.shields.io/badge/Ping%20Vietnam-25%E2%80%9335ms-brightgreen?style=for-the-badge" alt="Ping Badge" />
</p>

---

## Tiếng Việt / Vietnamese

### 📖 Giới thiệu

CloudPlay là một ứng dụng Android mã nguồn mở được phát triển để tối ưu hóa trải nghiệm chơi game trên Xbox Cloud Gaming (xCloud) cho người dùng ở Việt Nam và khu vực Đông Nam Á. Ứng dụng cung cấp các công cụ tối ưu hóa mạng và trải nghiệm hiển thị; kết quả có thể khác nhau tùy vào nhà mạng, thiết bị và vị trí địa lý.

Lưu ý pháp lý: Bạn có trách nhiệm tuân thủ Điều khoản Dịch vụ (Terms of Service) của Microsoft và các luật địa phương. Ứng dụng này không khuyến khích hoặc hướng dẫn hành vi vi phạm bản quyền, lách luật hay can thiệp trái phép vào các dịch vụ của bên thứ ba.

### ✨ Tính năng chính

- Tối ưu hóa kết nối mạng để giảm độ trễ và cải thiện chất lượng luồng (không đảm bảo với mọi trường hợp).
- Thanh HUD nổi tiện lợi hiển thị trạng thái mạng và các phím tắt thao tác nhanh.
- Dịch nội dung giao diện / mô tả game (Google Translate + từ điển nội bộ) với chế độ song ngữ (Tiếng Việt ↔ English).
- Giao diện hiện đại xây dựng bằng Jetpack Compose (Material 3);
- Tôn trọng quyền riêng tư: không lưu trữ mật khẩu hay thông tin đăng nhập của bạn trên máy chủ của project.

### 📥 Tải & Cài đặt

- Releases (Khuyên dùng): https://github.com/muitenvn/CloudPlay-App/releases/latest
- Hoặc tải artifact từ Actions nếu cần bản build mới nhất.

### ⚙️ Dành cho lập trình viên

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

### 📄 Bản quyền & Tuyên bố từ chối trách nhiệm

- CloudPlay là dự án độc lập, không liên kết chính thức với Microsoft hoặc dịch vụ Xbox.
- Tất cả thương hiệu (Xbox, Xbox Cloud Gaming, Xbox Game Pass...) thuộc sở hữu của chủ sở hữu tương ứng.
- Tác giả không chịu trách nhiệm cho việc sử dụng trái phép hoặc vi phạm Điều khoản Dịch vụ bởi người dùng.

---

## English / Tiếng Anh

### 📖 Overview

CloudPlay is an open-source Android application developed to improve the Xbox Cloud Gaming (xCloud) experience for users in Vietnam and Southeast Asia. The app provides network optimization tools and interface enhancements; results may vary depending on ISP, device, and geographic location.

Legal notice: You are responsible for complying with Microsoft's Terms of Service and local laws. This project does not encourage or instruct actions that violate copyrights, bypass protections, or interfere with third-party services.

### ✨ Key Features

- Network optimization to reduce latency and improve stream quality (no guarantee in all scenarios).
- Floating HUD shows network status and quick controls.
- Smart translation for UI/game descriptions (Google Translate + local dictionary) with bilingual mode (Vietnamese ↔ English).
- Modern UI built with Jetpack Compose (Material 3).
- Privacy-conscious: does not collect or store user login credentials.

### 📥 Download & Install

- Recommended: download from Releases: https://github.com/muitenvn/CloudPlay-App/releases/latest
- Alternatively, download the latest build artifact from the Actions tab.

### ⚙️ For Developers

Requirements: Android SDK 36 (or 35+), JDK 21.

```bash
# Clone repository
git clone https://github.com/muitenvn/CloudPlay-App.git
cd CloudPlay-App

# Make gradlew executable
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug
```

### 📄 Copyright & Disclaimer

- CloudPlay is an independent project and is not affiliated with Microsoft or Xbox services.
- All trademarks (Xbox, Xbox Cloud Gaming, Xbox Game Pass, etc.) are the property of their respective owners.
- The author is not responsible for any misuse of this software that violates third-party terms or local regulations.

---

### ☕ Support / Ủng hộ

If you enjoy the app and want to support development, you may donate. Use at your own discretion and be mindful of sharing sensitive payment information publicly.

