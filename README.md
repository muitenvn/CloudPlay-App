# 🎮 CloudPlay - Xbox Cloud Gaming Booster & Client

<p align="center">
  <img src="https://img.shields.io/github/v/release/muitenvn/CloudPlay-App?color=00c853&label=Latest%20Release&style=for-the-badge" alt="Release Badge" />
  <img src="https://img.shields.io/github/actions/workflow/status/muitenvn/CloudPlay-App/build-and-release-apk.yml?branch=main&label=Build%20Status&style=for-the-badge" alt="Build Badge" />
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-blue?style=for-the-badge&logo=android" alt="Android Badge" />
  <img src="https://img.shields.io/badge/Ping%20Vietnam-25%E2%80%9335ms-brightgreen?style=for-the-badge" alt="Ping Badge" />
</p>

---

## 📖 Giới thiệu / Overview

CloudPlay là ứng dụng Android mã nguồn mở giúp cải thiện trải nghiệm chơi Xbox Cloud Gaming (xCloud) trên các thiết bị Android, đặc biệt phù hợp với người dùng ở Việt Nam và khu vực Đông Nam Á. Ứng dụng tập trung vào việc tối ưu hóa kết nối mạng, cải thiện trải nghiệm điều khiển và tăng khả năng sử dụng trên mạng di động / Wi‑Fi.

This project is an Android app for improving the Xbox Cloud Gaming (xCloud) experience on Android devices, especially for users in Vietnam and Southeast Asia. It focuses on network optimization, smoother controls, and better usability on mobile and Wi‑Fi connections.

Lưu ý quan trọng: người dùng cần tuân thủ Điều khoản sử dụng của Microsoft, Apple/Google (nếu có), và các quy định pháp luật địa phương. Dự án này không khuyến khích hoặc hỗ trợ bất kỳ hành vi vi phạm bản quyền, phá khóa, lách luật, hay can thiệp trái phép vào dịch vụ của bên thứ ba.

Important: users must comply with Microsoft's terms, relevant platform policies, and local laws. This project does not encourage or support copyright violations, bypassing restrictions, or unauthorized interference with third-party services.

---

## ✨ Tính năng / Features

- ⚡ Tối ưu hóa kết nối mạng / Network optimization
  - Giảm độ trễ và cải thiện chất lượng luồng trong điều kiện mạng thực tế.
  - Hỗ trợ tối ưu cho các kết nối Wi‑Fi 5GHz và mạng di động 4G/5G.
  - Reduces latency and improves streaming quality under real-world network conditions.

- 🎮 HUD nổi / Floating HUD
  - Hiển thị ping, trạng thái mạng và các nút điều khiển nhanh.
  - Helps monitor connection status and perform fast in-game actions.

- 🌐 Dịch nội dung và hỗ trợ đa ngôn ngữ / Translation & multilingual support
  - Hỗ trợ dịch giao diện, mô tả game và thông tin trực quan.
  - Bilingual mode with Vietnamese and English experience.

- 🧩 Giao diện hiện đại / Modern UI
  - Thiết kế mượt, dễ sử dụng, xây dựng bằng Jetpack Compose (Material 3).
  - Smooth and modern interface built with Jetpack Compose (Material 3).

- 🔒 Bảo mật và quyền riêng tư / Security & privacy
  - Không thu thập mật khẩu hay thông tin đăng nhập người dùng.
  - Does not collect user passwords or login credentials.

---

## 📥 Tải xuống / Download

Bạn có thể tải APK mới nhất từ tab Releases của repository:

- GitHub Releases: https://github.com/muitenvn/CloudPlay-App/releases/latest
- Actions: https://github.com/muitenvn/CloudPlay-App/actions

Download the latest APK from the Releases page or the Actions tab.

---

## ⚙️ Build / Tự build

Yêu cầu: Android SDK 36 (hoặc 35+), JDK 21.

Requirements: Android SDK 36 (or 35+), JDK 21.

```bash
# Clone repository
git clone https://github.com/muitenvn/CloudPlay-App.git
cd CloudPlay-App

# Cấp quyền thực thi cho gradlew
chmod +x gradlew

# Biên dịch APK Debug
./gradlew assembleDebug
```

---

## 🧾 Bản quyền & Tuyên bố từ chối trách nhiệm / Copyright & Disclaimer

- CloudPlay là dự án độc lập và không liên kết chính thức với Microsoft, Xbox hoặc các dịch vụ của họ.
- CloudPlay is an independent project and is not officially affiliated with Microsoft, Xbox, or their services.
- Tất cả thương hiệu như Xbox, Xbox Cloud Gaming, Game Pass, v.v. thuộc quyền sở hữu của các chủ sở hữu tương ứng.
- All trademarks such as Xbox, Xbox Cloud Gaming, Game Pass, etc. belong to their respective owners.
- Tác giả không chịu trách nhiệm cho việc người dùng lạm dụng phần mềm để vi phạm thỏa thuận, pháp luật hoặc quyền của bên thứ ba.
- The author is not responsible for any misuse of the software that violates agreements, laws, or third-party rights.

---

## ☕ Ủng hộ / Support

Nếu bạn thấy ứng dụng hữu ích và muốn hỗ trợ phát triển, bạn có thể đóng góp hoặc tài trợ theo hướng phù hợp với quy định hiện hành.

If you find the app useful and want to support development, you may contribute or sponsor in a way that complies with applicable regulations.
