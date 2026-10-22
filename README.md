# CloudPlay Optimizer (Xbox Cloud Gaming Helper)

**CloudPlay Optimizer** là ứng dụng hỗ trợ chơi **Xbox Cloud Gaming (xCloud)** trên Android mượt mà hơn, lấy cảm hứng từ dự án mã nguồn mở [Better-xCloud](https://github.com/redphx/better-xcloud) (Giấy phép MIT). Ứng dụng tập trung vào sự ổn định, dễ sử dụng, bảo mật tài khoản và nguyên tắc **Chơi công bằng (Fair Play)**.

**CloudPlay Optimizer** is an Android companion app designed to deliver a smoother **Xbox Cloud Gaming (xCloud)** experience, inspired by the open-source [Better-xCloud](https://github.com/redphx/better-xcloud) project (MIT License). It focuses on stability, simplicity, account security, and **Fair Play**.

---

## Tính năng chính / Key Features

### 1. Tối ưu Hình ảnh & Độ mượt / Stream & Performance Optimization
- **Chọn mức chất lượng linh hoạt:** Hỗ trợ chế độ Tự động hoặc chọn mức chất lượng hình ảnh phù hợp với tốc độ mạng (áp dụng khi bắt đầu phiên chơi; tự động điều chỉnh êm bộ đệm khi đang chơi).
- **Chế độ máy yếu & Tự giảm tải khi máy nóng:** Giữ ổn định 60 FPS trên thiết bị phổ thông, chỉ mở 120 FPS cho Fortnite khi chọn chất lượng Cao trên máy khỏe, và tự động hạ hiệu ứng khi thiết bị quá nhiệt.
- **Adaptive Stream & Thermal Protection:** Smoothly balances playback buffer during gameplay, keeps safe 60 FPS defaults for everyday devices (enabling 120 FPS in Fortnite only on high quality presets), and automatically scales down visual load when the phone overheats.

### 2. Kết nối & Máy chủ thông minh / Smart Network & Server Selection
- **Đo độ trễ (Ping) trực tiếp:** Kiểm tra nhanh máy chủ gần nhất (Nhật Bản, Hàn Quốc, Singapore, Úc, Mỹ, Châu Âu) để vào game nhanh và ít giật lag.
- **Hỗ trợ vượt giới hạn vùng:** Giúp truy cập trang Xbox Cloud Gaming tại các khu vực chưa hỗ trợ chính thức và tự động ngắt chuyển tiếp khi vào luồng video để giảm độ trễ.
- **Region Bypass & Fast Server Check:** Quickly tests latency across global regions and helps connect from unsupported regions while keeping video streaming direct and low-latency.

### 3. Tay cầm cảm ứng & Chơi công bằng / Touch Controls & Fair Play
- **Tay cầm ảo tùy chỉnh:** Hỗ trợ bố cục chuẩn Xbox, bố cục riêng cho **Fortnite**, tùy chỉnh vị trí, kích thước, độ trong suốt nút bấm và ngắm con quay hồi chuyển (Gyro).
- **Cam kết Chơi công bằng (Fair Play):** Hoàn toàn **không** có tính năng bắn liên thanh (Turbo/Macro), tự động khóa cò hay bất kỳ công cụ gian lận nào can thiệp vào tính công bằng của trò chơi.
- **Customizable Touch Controls (Fair Play):** Standard Xbox and Fortnite-tailored touch layouts with customizable button placement, size, opacity, and gyro aiming—strictly free of turbo-fire, macros, or unfair gameplay assists.

### 4. Tab Game: Thư viện & Lịch sử phiên chơi / Games Tab: Library & Session History
- **Thư viện nhanh:** Mở nhanh các tựa game phổ biến hoặc tiếp tục ngay trò chơi vừa chơi gần nhất từ Trang chủ.
- **Lịch sử phiên chơi trực quan:** Tự động ghi lại thời lượng chơi, FPS trung bình, độ trễ mạng (Ping) và trạng thái nhiệt độ máy của từng phiên chơi ngay trên thiết bị.
- **Quick Library & Session History:** Jump straight back into your most recently played game from the Home screen, and review past session duration, average FPS, ping, and thermal performance in the **Games** tab.

### 5. Bảng điều khiển nhanh (HUD) & Dịch màn hình / Quick HUD & Screen Translator
- **HUD gọn nhẹ trong lúc chơi:** Xem nhanh FPS, Ping, chỉnh tay cầm cảm ứng và mở hộp **Cài đặt nhanh** (chất lượng, chế độ máy yếu, bật/tắt dịch) ngay trong màn hình chơi mà không bị làm phiền.
- **Dịch phụ đề & menu trực tiếp trên máy:** Hỗ trợ dịch chữ trên màn hình sang tiếng Việt hoặc ngôn ngữ đã chọn bằng mô hình dịch ngoại tuyến trên thiết bị.
- **Compact In-Game HUD & On-Device Translator:** Check real-time FPS/ping, adjust touch controls, toggle **Quick Settings** on the fly, and translate in-game menus or subtitles directly on your device.

---

## Bảo mật & Quyền riêng tư / Security & Privacy

Ứng dụng được thiết kế theo nguyên tắc an toàn tối đa cho người dùng (xem chi tiết tại [SECURITY.md](SECURITY.md)):
The app is built with strict user safety guarantees (see [SECURITY.md](SECURITY.md) for full details):

1. **Đăng nhập an toàn qua HTTPS:** Bạn đăng nhập trực tiếp trên tên miền chính thức của Microsoft (`login.live.com`, `xbox.com`). Ứng dụng không thu thập hay lưu mật khẩu của bạn. / **Direct HTTPS Sign-In:** Authenticate directly on official Microsoft domains; credentials are never collected or stored.
2. **Cách ly trang thanh toán:** Tự động tắt chuyển tiếp mạng hoặc mở bằng trình duyệt ngoài khi bạn vào trang mua hàng/thanh toán của Microsoft. / **Payment Page Isolation:** Network relay automatically pauses on billing, cart, and checkout pages.
3. **Lưu trữ 100% trên thiết bị:** Cài đặt, danh sách game và lịch sử phiên chơi chỉ nằm trong bộ nhớ riêng tư của ứng dụng trên máy bạn, không gửi lên đám mây. / **100% On-Device Data:** Settings, saved games, and session history remain strictly on your device.

---

## Điều khoản sử dụng & Miễn trừ trách nhiệm / Terms of Use & Disclaimer

Khi tải và sử dụng ứng dụng này, bạn đồng ý với các điều khoản sau (cũng được hiển thị rõ trong ứng dụng):
By downloading and using this application, you agree to the following terms (also accessible anytime inside the app under **Settings**):

- **Không liên kết với Microsoft / Non-Affiliation:** Đây là dự án hỗ trợ độc lập của cộng đồng, **không** phải ứng dụng chính thức và không được tài trợ hay phê duyệt bởi Microsoft Corporation hay Xbox. Nhãn hiệu *Xbox*, *Xbox Cloud Gaming* và *Fortnite* thuộc về chủ sở hữu tương ứng. / This is an independent community companion app and is **not** affiliated with, endorsed, or sponsored by Microsoft Corporation or Xbox.
- **Trách nhiệm người dùng / User Responsibility:** Người dùng tự chịu trách nhiệm tuân thủ Điều khoản Dịch vụ của Microsoft (Microsoft Services Agreement) và quy tắc ứng xử của từng trò chơi khi sử dụng dịch vụ đám mây. / Users are solely responsible for complying with the Microsoft Services Agreement and each game's code of conduct.
- **Bản quyền mã nguồn mở / Open-Source License:** Dự án kế thừa ý tưởng tối ưu từ [Better-xCloud](https://github.com/redphx/better-xcloud) của tác giả **redphx** và được phát hành theo [Giấy phép MIT (MIT License)](https://opensource.org/licenses/MIT). Phần mềm được cung cấp "nguyên trạng" (AS-IS) không kèm bảo đảm thương mại.

---

## Hướng dẫn cài đặt & biên dịch / Build Instructions

1. Mở dự án bằng **Android Studio** (hỗ trợ Android SDK 34+). / Open the project in **Android Studio** (Android SDK 34+).
2. Đồng bộ Gradle và chạy mục tiêu `app` trên thiết bị Android thực tế (Android 8.0 trở lên). / Sync Gradle and run the `app` configuration on an Android device (Android 8.0+).
3. Trong lần mở đầu tiên, vui lòng đọc và xác nhận **Điều khoản sử dụng & Bảo mật** trước khi đăng nhập tài khoản Xbox của bạn. / On first launch, review and accept the **Terms of Use & Security Notice** before signing in to your Xbox account.
