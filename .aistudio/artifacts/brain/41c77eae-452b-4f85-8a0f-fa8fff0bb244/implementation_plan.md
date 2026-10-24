# Kế Hoạch Nâng Cấp Chuyên Sâu Hệ Thống Mạng Wi-Fi & 4G/5G (Dựa Trên Số Liệu Phiên Chơi Fortnite Thực Tế)

Nâng cấp toàn diện lõi xử lý mạng **Wi-Fi (2.4GHz / 5GHz / 6GHz)** và **4G/5G Di Động** kết hợp xử lý trực tiếp hiện tượng **Giải mã TB vọt lên `32.8ms` và Ping dao động `81ms ➔ 127ms`** trong ảnh phiên chơi Fortnite vừa gửi, đồng thời gộp bớt các nút thừa và sửa các chỗ bị tràn/cắt chữ trên giao diện.

---

## Các Quyết Định Đã Chốt Với Bạn

> [!IMPORTANT]
> Toàn bộ kế hoạch dưới đây tuân thủ nghiêm ngặt các lựa chọn bạn đã xác nhận:

- **Không thêm nút mới, gộp gọn các nút thừa:** Mọi thuật toán tối ưu sâu cho Wi-Fi và 4G/5G chạy tự động ngầm 100%. Gộp bớt các công tắc mạng trùng lặp trong bảng điều khiển nổi và màn hình Cài đặt.
- **Trọng tâm xử lý mạng thực tế:**
  1. **Mạng 4G/5G & Đường truyền quốc tế:** Chống rớt mạng, khựng hình khi sóng 4G yếu, đổi trạm thu phát hoặc khi đường truyền quốc tế đi Nhật/Hàn dao động (như đoạn Ping nhấp nhô `81ms ➔ 127ms` trên biểu đồ Fortnite của bạn).
  2. **Mạng Wi-Fi 2.4GHz & 5GHz:** Chống giật lag khi đứng xa cục phát Wi-Fi hoặc nhiễu sóng.
  3. **Xử lý dứt điểm lỗi `Giải mã chậm (32.8ms)`:** Ngăn hiện tượng gói tin mạng dồn cục làm nghẽn bộ giải mã video phần cứng của điện thoại.
- **Chiến lược phản ứng khi mạng suy giảm:** Ưu tiên tuyệt đối **độ trễ thấp và thao tác điều khiển nhạy nhất**, chấp nhận hạ nhẹ độ nét khi mạng yếu hoặc khi thời gian giải mã vượt ngưỡng `9ms`.

---

## 1. Phân Tích Từ Ảnh Chụp Phiên Chơi Fortnite Thực Tế Của Bạn (`15p 36s`)

Nhìn vào bảng thống kê phiên chơi **Fortnite** bạn vừa gửi:
- **Ping TB: `87ms` (Thấp `81ms` • Cao `127ms`):** Ở nửa sau phiên chơi, biểu đồ Ping xuất hiện các đỉnh nhọn vọt từ `81ms` lên `127ms` (chênh lệch Jitter tới `46ms`).
- **Giải mã TB: `32.8ms` (`Trạng thái máy: Giải mã chậm >12ms`) dù FPS vẫn đạt `56 FPS`:**
  - Để chơi mượt không trễ tay cầm ở 60 FPS, mỗi khung hình chỉ được phép giải mã trong **dưới `8–10ms`**. Con số **`32.8ms`** có nghĩa là thao tác bấm nút của bạn bị trễ thêm hơn 2 khung hình!
  - **Nguyên nhân gốc rễ:** Khi mạng Wi-Fi / 4G bị khựng nhẹ (Ping vọt từ `81ms` lên `127ms`), hàng chục gói tin hình ảnh bị giữ lại rồi **đổ ập về cùng một mili-giây**. Bộ giải mã phần cứng MediaCodec của máy bị dồn ứ 3–4 khung hình cùng lúc trong hàng đợi, khiến tổng thời gian chờ giải mã (`totalDecodeTime`) vọt lên `32.8ms`!
- **Chọn nhầm máy chủ chưa tối ưu:** Phiên chơi đang kết nối vào `Japan Tokyo CloudDC #11` (`87ms`), trong khi máy chủ `Japan Tsukuba #6` chỉ có **`~68ms`** (thấp hơn gần `20ms`).
- **Lỗi hiển thị giao diện nhìn thấy ngay trên ảnh:**
  1. Nút đỏ **`🗑️ Xóa phiên này`** ở góc dưới bên trái bị rớt chữ `"này"` xuống dòng 2 và bị cắt mất nửa chữ.
  2. Dòng tiêu đề **`BIỂU ĐỒ PING THEO THỜI GIAN`** và **`Thấp 81ms • Cao 127ms`** chèn ép nhau làm chữ `"127ms"` bị rớt dòng lẻ loi.
  3. Thanh tiêu đề trên cùng bị ép hẹp khiến tên ứng dụng bị cắt thành **`CLOUDP...`**.

---

## 2. Giải Pháp Kỹ Thuật Chuyên Sâu Cho Wi-Fi, 4G/5G & Bộ Giải Mã (`< 8ms`)

### 2.1. Chống Dồn Ứ Hàng Đợi Giải Mã (`32.8ms ➔ < 8ms`) Khi Mạng Dao Động
- **Cơ chế xả khung hình trễ (Frame-Queue Drain) & Hạ áp lực tức thì trong 1.5 giây:**
  - Ngay khi phát hiện thời gian giải mã vượt ngưỡng **`9.0ms`** hoặc Ping tăng đột ngột trên **`15ms`** (như đợt sóng từ `81ms ➔ 127ms`), hệ thống lập tức hạ nhẹ băng thông mục tiêu ngay trong **1 nhịp đo (1.5 giây)** thay vì chờ 6 giây như cũ.
  - Khi kích thước mỗi khung hình nhỏ đi ngay lập tức, chip giải mã của điện thoại xử lý xong từng khung hình trong **3–6ms**, xóa sạch hàng đợi bị ứ đọng và đưa `Giải mã TB` từ `32.8ms` về mức xanh mát (`< 8ms`).
- **Tự động gạn lọc nhiễu số liệu WebRTC:**
  - Khi trình duyệt vừa chuyển tab hoặc tạm dừng khung hình, `totalDecodeTime` của Chromium đôi khi cộng dồn cả thời gian chờ dẫn đến số ảo vọt lên trên `30ms`. Bộ đo mới sẽ lọc bỏ các nhịp nghỉ ngoại lệ để số liệu `Giải mã TB` phản ánh đúng 100% tốc độ phần cứng thực.

### 2.2. Đo Sóng Thực Tế Trên Wi-Fi (2.4GHz / 5GHz) & 4G/5G
- **Trên Wi-Fi 5GHz & 2.4GHz:**
  - Đọc trực tiếp **cường độ sóng Wi-Fi (`RSSI dBm`)** và **tốc độ liên kết vật lý (`Link Speed Mbps`)**:
    - Nếu bạn dùng **Wi-Fi 5GHz nhưng đứng xa cục phát / cách tường** (cường độ sóng yếu hơn `-70 dBm`), hệ thống tự động hạ trần băng thông xuống mức an toàn (7–9 Mbps) để gói tin không bị rớt và không gây nhấp nhô Ping.
    - Nếu dùng **Wi-Fi 2.4GHz**, tự động kích hoạt chế độ chống nhiễu tay cầm Bluetooth và giữ gói tin gọn nhẹ.
- **Trên mạng 4G/5G Di Động:**
  - Theo dõi **cường độ vạch sóng (`signalStrength`)** và **băng thông thực tế (`linkDownstreamBandwidthKbps`)** để chủ động thu gọn luồng hình ảnh ngay khi sóng 4G yếu hoặc chuyển trạm phát sóng.
- **Tự động ưu tiên máy chủ có Ping thực tế thấp nhất (ví dụ `Tsukuba #6 ~68ms`):**
  - Khi tự động chọn máy chủ, hệ thống ưu tiên cụm máy chủ có độ trễ thấp nhất và ổn định nhất cho tựa game đang mở.

### 2.3. Gộp Nút Thừa & Sửa Lỗi Cắt Chữ Giao Diện (Không Thêm Nút Mới)
- **Gộp gọn các nút mạng trùng lặp:**
  - Bật mặc định 100% chế độ **Ưu tiên giảm trễ** ở tầng hệ thống; loại bỏ nút phụ *"Giảm trễ: Bật/Tắt"* và nhãn phụ *"Tự động/Cố định"* trùng lặp trong bảng nút nổi.
  - Rút gọn các công tắc mạng chồng chéo trong màn hình Cài đặt.
- **Sửa lỗi cắt chữ trong Hộp thoại Lịch sử phiên chơi & Thanh tiêu đề:**
  - Sửa nút **`🗑️ Xóa phiên`** (gọn 1 dòng, căn chỉnh khoảng đệm chuẩn để không bao giờ bị cắt mất chữ như trong ảnh).
  - Căn chỉnh lại hàng tiêu đề **`BIỂU ĐỒ PING`** và **`Thấp 81ms • Cao 127ms`** nằm gọn trên 1 dòng duy nhất.
  - Thu gọn khoảng cách các nút trên thanh tiêu đề trên cùng để tên **`CLOUDPLAY`** hiển thị rõ ràng, không bị cắt thành `CLOUDP...`.

---

## 3. Sơ Đồ Luồng Xử Lý Mạng & Chống Nghẽn Giải Mã

```
┌─────────────────────────────────────────────────────────────────────────┐
│             1. CẢM BIẾN SÓNG WI-FI & 4G/5G THỜI GIAN THỰC               │
│  • Wi-Fi: Băng tần (2.4/5/6GHz) + Cường độ sóng (RSSI dBm) + LinkSpeed  │
│  • 4G/5G: Vạch sóng (SignalStrength) + Băng thông + Phát hiện đổi trạm  │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│        2. BỘ CHỐNG NGHẼN GIẢI MÃ & ƯU TIÊN ĐỘ NHẠY (PHẢN ỨNG 1.5S)      │
│  • Phát hiện Ping nhảy (81➔127ms) hoặc Giải mã > 9ms                    │
│  • Hạ nhẹ băng thông tức thì trong 1.5s ➔ Xả sạch hàng đợi giải mã      │
│  • Giữ thời gian giải mã luôn ở mức Xanh (< 8ms), điều khiển nhạy 100%  │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│           3. GIAO DIỆN GỌN NHẸ (KHÔNG THÊM NÚT, SỬA CẮT CHỮ)            │
│  • Gộp bỏ nút "Giảm trễ: Bật/Tắt" thừa (luôn bật tự động ngầm)          │
│  • Sửa nút "Xóa phiên", biểu đồ Ping 1 dòng & thanh tiêu đề CLOUDPLAY   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Các Hạng Mục Sẽ Thực Hiện Ngay Khi Bạn Bấm Proceed

1. **Nâng cấp lõi mạng Wi-Fi (đo RSSI/LinkSpeed khi đứng xa cục phát) & 4G/5G (đo vạch sóng & chống khựng khi đổi trạm).**
2. **Xử lý dứt điểm lỗi `Giải mã TB 32.8ms`:** Phản ứng tức thì trong 1.5 giây khi Ping nhấp nhô hoặc giải mã vượt `9ms`, kết hợp xả hàng đợi khung hình trễ để thời gian giải mã luôn dưới `8ms`.
3. **Gộp bớt nút thừa & Sửa lỗi tràn chữ trong ảnh chụp màn hình:** Gộp bỏ công tắc giảm trễ phụ trong bảng nút nổi, sửa nút `"Xóa phiên"`, dòng `"Thấp 81ms • Cao 127ms"` và thanh tiêu đề `"CLOUDPLAY"`.
