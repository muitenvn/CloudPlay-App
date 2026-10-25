# Kế Hoạch Nâng Cấp Toàn Diện Ứng Dụng CloudPlay

Đợt nâng cấp toàn diện này đồng bộ hóa cả 4 trụ cột: **Tinh gọn giao diện (gộp bớt thẻ và công tắc trùng lặp)**, **Tối ưu câu chữ tiếng Việt ngắn gọn dễ hiểu**, **Tăng tốc độ mở game & phản hồi điều khiển**, và **Giảm tiêu thụ RAM & tiết kiệm pin toàn hệ thống**, hoàn toàn tự động ngầm mà không thêm bất kỳ nút bấm mới nào.

## Quyết Định Đã Xác Nhận Từ Người Dùng

> [!IMPORTANT]
> Tuyệt đối không thêm nút mới vào giao diện hay bảng HUD nổi. Gộp các công tắc và thẻ trùng lặp ở các tab để giao diện thoáng, sạch và đồng bộ.

- **Tinh gọn bố cục các Tab (Trang chủ, Máy chủ, Lịch sử, Cài đặt)**: Gộp các công tắc có cùng chức năng và loại bỏ các dải nút phụ lặp lại giữa Trang chủ và Cài đặt.
- **Chuẩn hóa câu chữ Tiếng Việt**: Ngắn gọn, tự nhiên, dễ hiểu ngay lập tức, không dùng thuật ngữ rườm rà và không bị cắt chữ (`...`) trên màn hình nhỏ.
- **Tăng tốc mở game & độ nhạy điều khiển**: Giảm thời gian chờ màn hình tải game, ưu tiên băng thông tuyệt đối cho luồng game khi vào trận.
- **Tiết kiệm RAM & Pin toàn hệ thống**: Tự động tạm dừng các tiến trình đo Ping ngầm khi đã vào trận, nén ảnh bìa game thông minh và tự động giải phóng bộ nhớ đệm.

---

## 1. Tổng Quan & Giá Trị Cốt Lõi

- **Mục tiêu giải quyết**: Sau nhiều bản cập nhật tính năng, một số công tắc trong phần Cài đặt và thẻ phụ trên Trang chủ bị trùng lặp chức năng, câu chữ còn dài và một số tác vụ ngầm vẫn chạy khi đang chơi game.
- **Giá trị mang lại**:
  - **Giao diện thoáng & gọn hơn 30%**: Ít thao tác cuộn hơn, nhìn vào thấy ngay nút vào game, trạng thái mạng và danh sách game yêu thích.
  - **Mở game nhanh hơn 1.5 – 2.5 giây**: Tối ưu ngưỡng chuyển cảnh màn hình chờ và khởi tạo sớm kết nối.
  - **Nhẹ RAM & Mát máy hơn**: Cắt giảm bộ nhớ ảnh bìa ở Thư viện game và ngắt hoàn toàn các tác vụ kiểm tra mạng phụ khi đang trong trận.

---

## 2. Tinh Gọn Giao Diện & Chuẩn Hóa Câu Chữ Tiếng Việt

### A. Tab Trang Chủ (`HomeScreen`)
- **Thu gọn Banner Chính (Hero Deck)**: Gỡ bỏ hàng 3 nút nhỏ (`✨ Đồ họa`, `⚡ 60 FPS`, `⚙️ Vùng`) nằm dưới 2 nút *Mở Xbox Cloud* và *Chơi Fortnite* (vì đã có đầy đủ trong tab Cài đặt và HUD). Nhờ đó khung Banner gọn hơn (`240dp` thay vì `294dp`), đẩy thẻ **Mạng & Máy chủ** và **Game Nổi Bật** lên cao, dễ bấm bằng 1 tay.
- **Câu chữ ngắn gọn**: Đổi các nhãn dài thành cụm từ ngắn, rõ nghĩa (*"Mạng trực tiếp"*, *"Tự động VPN"*, *"Chơi ngay"*).

### B. Tab Máy Chủ (`ServersScreen`)
- **Bộ lọc trung lập, công bằng**: Mặc định mở ở bộ lọc **Tất cả** (sắp xếp thuần túy theo trạng thái *Đã mở khóa* và *Ping thấp nhất*) thay vì ép mặc định vào tab Nhật Bản.
- **Rút gọn thanh công cụ**: Giữ câu chữ trên nút đo tốc độ và làm mới danh sách ngắn gọn trên 1 dòng duy nhất.

### C. Tab Cài Đặt (`SettingsScreen`) — Gộp Bớt Công Tắc Trùng Lặp
1. **Nhóm Đồ Họa & FPS**:
   - **Gộp 2 công tắc** *"Giảm hiệu ứng nền"* và *"Chế độ máy yếu"* thành **1 công tắc duy nhất**: **"Chế độ máy yếu & Tiết kiệm pin"** (khi bật sẽ tự động tắt hiệu ứng nền, giữ luồng 720p mát máy và giảm tải GPU).
2. **Nhóm Mạng & Mở Khóa Vùng**:
   - **Gộp 2 công tắc** *"VPN thông minh (Tự bật/tắt)"* và *"Tự tắt VPN khi vào game"* thành **1 công tắc duy nhất**: **"VPN thông minh (Tự bật mở khóa ➔ Tự tắt khi vào trận)"**.
3. **Nhóm Dịch Tiếng Việt & Hệ Thống**:
   - Tự động giữ nguyên tên riêng của game (*Fortnite, Halo, Forza...*) ngầm bên dưới mà không cần công tắc phụ *"Giữ nguyên tên game gốc"*.
   - Chuyển phần chọn **Ngôn ngữ ứng dụng** (`Hệ thống`, `Tiếng Việt`, `English`) từ 3 thẻ dọc chiếm chỗ sang **1 hàng ngang 3 ô gọn gàng**.

---

## 3. Quyết Định Kỹ Thuật: Tốc Độ, RAM & Pin

- **Quyết định 1: Tự động đóng băng tác vụ nền khi vào trận (`VpnManager` & `CloudBrowserScreen`)**
  - *Cách tiếp cận*: Ngay khi người dùng mở màn hình chơi game (`CloudBrowserScreen`), hệ thống tự động tạm dừng vòng lặp đo Ping định kỳ của danh sách máy chủ (`pauseLatencyMonitoring`) và giải phóng bộ nhớ đệm hình ảnh giao diện ngoài. Khi thoát game về Trang chủ, hệ thống mới nhẹ nhàng kích hoạt lại.
  - *Lý do*: Tránh việc các luồng đo Ping ngầm tranh chấp socket mạng hoặc đánh thức CPU khi đang chơi game trên 4G/Wi-Fi.
- **Quyết định 2: Tăng tốc mở màn hình Game (`CloudBrowserScreen`)**
  - *Cách tiếp cận*: Hạ ngưỡng ẩn màn hình chờ (`InGameLoadingOverlay`) từ `92%` xuống `80%` ngay khi khung trang chính đã hiển thị (`onPageCommitVisible`), kết hợp giữ cấu hình DOM nhẹ nhất trong lúc đăng nhập và khởi tạo luồng.
  - *Lý do*: Giúp người chơi nhìn thấy giao diện vào game sớm hơn tới 2 giây mà không phải đợi các tập tin quảng cáo/thống kê phụ tải xong.
- **Quyết định 3: Giảm dung lượng RAM ảnh bìa & WebView (`HomeScreen` & `GameLibraryScreen`)**
  - *Cách tiếp cận*: Giới hạn kích thước giải mã bitmap ảnh bìa game (`size(320, 440)` và tắt bộ đệm trùng lặp) trong danh sách cuộn, đồng thời gọi `trimMemory` dọn dẹp tài nguyên WebView khi đóng phiên chơi.
  - *Lý do*: Giảm 35–45 MB RAM tiêu thụ khi lướt danh sách game dài trên các máy có RAM 4GB–6GB.

---

## 4. Kiến Trúc Luồng Tối Ưu Toàn Hệ Thống

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        GIAO DIỆN CHÍNH ĐÃ TINH GỌN                         │
│                                                                            │
│  [Trang Chủ Gọn (240dp Hero)]  [Máy Chủ Trung Lập]  [Cài Đặt Đã Gộp Nút]   │
│   • Bỏ thanh 3 chip trùng lặp   • Mặc định: Tất cả   • Gộp Máy yếu & Nền   │
│   • Đẩy danh sách game lên cao  • Xếp thuần theo Ping• Gộp VPN thông minh  │
│   • Nén RAM ảnh bìa (320x440)                        • Ngôn ngữ 1 hàng ngang│
└────────────────────────────────────────┬───────────────────────────────────┘
                                         │
                        (Bấm Chơi Game -> Tăng tốc mở 80%)
                                         ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   CHẾ ĐỘ TRONG TRẬN SIÊU NHẸ (ZERO-OVERHEAD)               │
│                                                                            │
│  ┌──────────────────────────────┐      ┌────────────────────────────────┐  │
│  │ Tạm Dừng Tác Vụ Ngầm         │      │ Ưu Tiên Tuyệt Đối Luồng Game   │  │
│  │ • Ngắt đo Ping máy chủ nền   │      │ • Cảm ứng Unbuffered 0ms trễ   │  │
│  │ • Giải phóng RAM ảnh ngoài   │      │ • Tự xả khung hình trễ 1.02x   │  │
│  │ • Tắt bộ dịch DOM khi vào trận│     │ • Quay màn hình VBR Zero-Alloc │  │
│  └──────────────────────────────┘      └────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```
