# Tối Ưu Hiệu Năng, Khắc Phục Độ Trễ Cao Trên Mạng 4G & Nâng Cấp Trải Nghiệm Người Dùng

Bản kế hoạch này giải quyết tận gốc hiện tượng **trễ điều khiển (input lag), nhảy ping (jitter) và trễ hình ảnh tích lũy** khi chơi Xbox Cloud Gaming qua mạng di động 4G/5G, đồng thời nâng cấp trải nghiệm điều khiển trực quan ngay trong màn hình chơi game.

---

## Quyết Định Quan Trọng & Lựa Chọn Mặc Định

> [!IMPORTANT]
> **Các cơ chế tối ưu 4G dưới đây hoạt động tự động ngầm 100% ngay khi phát hiện mạng di động 4G/5G, đồng thời cung cấp thêm nút "⚡ Xả Trễ" 1 chạm trên bảng HUD để người chơi chủ động làm mới luồng bất cứ lúc nào:**
> - **Quyết định đã xác nhận 1 — Tự động chống nghẽn sóng 4G (LTE Anti-Bufferbloat):** Tự động giữ dải băng thông tối ưu **4.0 – 8.0 Mbps (720p60 không khung B-frame)** trên 4G để gói tin đi qua trạm thu phát LTE tức thì, không bị xếp hàng chờ tại trạm.
> - **Quyết định đã xác nhận 2 — Giới hạn bộ đệm WebRTC ở mức siêu nhạy:** Giảm trần bộ đệm thu trên 4G từ `45ms` xuống **`10ms – 22ms`** (mặc định `12ms`) và rút ngắn thời gian thu hồi đệm từ `9 giây` xuống **`4.5 giây`**.
> - **Quyết định đã xác nhận 3 — Tự động bám sát khung hình thực tế (Live-Edge Sync) & Nút "⚡ Xả Trễ" trên HUD:** Tự động xả độ trễ hình ảnh bị dồn cục sau mỗi nhịp sóng 4G chập chờn và bổ sung nút bấm 1 chạm ngay trên HUD để làm mới bộ đệm mà không cần tải lại trang game.

---

## 1. Tổng Quan & Giá Trị Cốt Lõi

- **Mục tiêu giải quyết:** Khi chơi Cloud Gaming trên mạng 4G, độ trễ cao không chỉ đến từ khoảng cách máy chủ mà chủ yếu do **3 nguyên nhân đặc thù của sóng di động**:
  1. **Nghẽn hàng đợi tại trạm phát 4G (LTE Bufferbloat) và bộ đệm thu phình to (`45ms`):** Cộng với ping 4G thực tế (`70–110ms`), tổng độ trễ từ lúc bấm nút đến lúc nhân vật phản hồi vọt lên `140–180ms`.
  2. **Modem 4G rơi vào trạng thái ngủ ngắn (LTE RRC Connected DRX):** Khi luồng dữ liệu chiều lên (uplink) nghỉ vài chục mili-giây, chip modem 4G tự chuyển sang chế độ tiết kiệm năng lượng ngắn hạn, làm chậm thao tác bấm nút thêm `25–45ms`.
  3. **Trễ hình ảnh tích lũy (Catch-up Lag):** Sau một nhịp sóng 4G khựng nhẹ, các khung hình cũ ứ đọng trong hàng đợi trình phát `<video>`, khiến hình ảnh hiển thị chậm hơn thực tế hàng trăm mili-giây.
- **Đối tượng phục vụ:** Người chơi Xbox Cloud Gaming trên điện thoại Android qua mạng di động 4G/5G hoặc mạng Wi-Fi có độ dao động cao, cần phản hồi tay cầm tức thì và khung hình mượt mà.
- **Giá trị mang lại:** Giảm **35ms – 65ms** tổng độ trễ đầu-cuối (end-to-end input lag) trên mạng 4G, loại bỏ hiện tượng trễ hình kéo dài sau khi rớt sóng nhẹ, và giữ giao diện đạt chuẩn 60/120fps không giật khựng.

---

## 2. Trải Nghiệm Người Dùng & Thiết Kế Giao Diện (UX & Visual Design)

- **Luồng trải nghiệm người dùng (Key User Flows):**
  1. **Tự động kích hoạt khi vào game bằng 4G/5G:** Ngay khi vào màn hình chơi game, hệ thống tự động nhận diện kết nối di động, kích hoạt cấu hình **4G Siêu Nhạy** (ghim đường truyền trực tiếp sau khi mở khóa vùng, khóa codec phần cứng 0-B-frame và giữ thức modem 4G).
  2. **Theo dõi độ trễ trực quan trên bảng HUD:** Khi mở bảng điều khiển nổi (Floating HUD), người chơi thấy ngay trạng thái mạng hiện tại (ví dụ: `📶 4G Siêu Nhạy • Đệm 12ms`) cùng phân tách độ trễ rõ ràng: **Mạng (Ping)** + **Giải mã (Decode)** + **Bộ đệm (Buffer)**.
  3. **Thao tác 1 chạm "⚡ Xả Trễ" (Instant Lag Flush):** Khi người chơi đi vào vùng sóng 4G yếu khiến cảm giác bấm nút hơi nặng tay, chỉ cần bấm nút **"⚡ Xả Trễ"** trên bảng HUD. Hệ thống lập tức xả toàn bộ khung hình ứ đọng, đưa bộ đệm về `10ms`, đồng bộ lại nhịp tay cầm và hiện thông báo xác nhận ngay lập tức mà **không làm gián đoạn hay tải lại ván game**.

- **Định hướng thẩm mỹ & Hệ màu (Visual Identity & Theme):**
  - *Phong cách thiết kế:* **Xbox Dashboard Utilitarian Dark** — tối giản, sắc nét, độ tương phản cao, ưu tiên tốc độ phản hồi dưới `200ms`.
  - *Bảng màu ngữ nghĩa:*
    - Nền bề mặt HUD (`--background` / Surface): Đen bán trong suốt `#CC0E1318` kết hợp viền mảnh `#26FFFFFF` giúp nổi bật trên nền video game mà không gây chói.
    - Màu nhấn chủ đạo (`--primary` / Xbox Emerald): Xanh lá `#107C10` và `#22C55E` cho trạng thái mạng ổn định / tối ưu 4G đang bật.
    - Màu nhấn hành động nhanh (`--accent` / Instant Flush): Xanh lục lam / Vàng tia chớp `#00E5FF` / `#F59E0B` cho nút **"⚡ Xả Trễ"** và chỉ báo tự động thu hồi trễ.
    - Cảnh báo độ trễ (`--warning` / `--danger`): Vàng hổ phách `#F59E0B` khi ping 4G dao động nhẹ và Đỏ `#EF4444` khi nghẽn trạm phát.
  - *Bố cục & Tương tác:* Mọi nút bấm nhanh trên HUD tuân thủ kích thước chạm tối thiểu `48dp`, có phản hồi xúc giác (haptic feedback) nhẹ và hiệu ứng chuyển trạng thái mượt mà.

---

## 3. Quyết Định Sản Phẩm & Đánh Đổi Kỹ Thuật (Trade-Offs)

- **Quyết định 1: Ưu tiên hạ nhanh Bitrate (4.0 – 8.0 Mbps) thay vì tăng bộ đệm thu trên mạng 4G**
  - *Giải pháp lựa chọn:* Giữ trần bộ đệm thu (`jitterBufferTarget`) ở mức `10ms – 22ms` trên 4G và chủ động hạ bitrate xuống `4.0 – 6.0 Mbps` ngay khi phát hiện xu hướng ping tăng (`rttTrendDelta >= 12ms`).
  - *Lý do:* Trên mạng 4G, khi sóng yếu, trạm phát LTE (eNodeB) tự động xếp hàng các gói tin lớn gây ra hiện tượng **Bufferbloat** (ping vọt lên 250–400ms). Việc tăng bộ đệm thu trên máy khách chỉ làm hình ảnh trễ thêm mà không giải quyết được tắc nghẽn tại trạm phát. Hạ nhẹ kích thước gói tin video giúp thông rỗng hàng đợi trạm 4G trong 1–2 giây, giữ thao tác điều khiển luôn nhạy.
  - *Phương án đã loại bỏ:* Giữ bitrate cao (12–15 Mbps) và tăng bộ đệm lên 45–60ms (gây trễ nút bấm nặng nề, không thể chơi các game hành động/bắn súng).

- **Quyết định 2: Cơ chế giữ thức Modem 4G (Anti-DRX Uplink Pacer) chỉ chạy khi đang trong luồng Stream trên mạng di động**
  - *Giải pháp lựa chọn:* Phát xung nhịp đánh thức siêu nhẹ (chu kỳ `28ms` khi tay cầm đang nghỉ) trên kênh kết nối WebRTC khi phát hiện mạng 4G/5G và tự tắt ngay khi thoát game hoặc chuyển sang Wi-Fi.
  - *Lý do:* Giữ chip modem 4G luôn ở trạng thái thức liên tục (Active Continuous Reception) trong suốt ván chơi để triệt tiêu `25–45ms` độ trễ đánh thức modem, nhưng không gây hao pin khi người dùng ở ngoài màn hình chính.

- **Quyết định 3: Khóa Codec H.264 Constrained Baseline (`42e01f`) không khung B-frame khi dùng 4G**
  - *Giải pháp lựa chọn:* Tự động sắp xếp ưu tiên SDP sang `42e01f` cho cả chế độ mạng 4G bên cạnh chế độ máy yếu.
  - *Lý do:* Khung hình B-frame đòi hỏi bộ giải mã phải chờ các khung hình phía sau mới hiển thị được; nếu mạng 4G rớt 1 gói tin, cả cụm khung hình sẽ bị khựng. Loại bỏ B-frame giúp giải mã tức thì từng khung hình (`decodeTime < 4.5ms`) và chống khựng hình khi sóng 4G chập chờn.

---

## 4. Kiến Trúc Kỹ Thuật & Chiến Lược Dữ Liệu

### Sơ Đồ Kiến Trúc Hệ Thống Chống Trễ 4G

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   BỘ QUẢN LÝ MẠNG 4G/5G (Network Layer)                    │
│   • Phát hiện mạng di động (Cellular 4G/5G), đo băng thông & cường độ sóng │
│   • Ghim thẳng tiến trình vào mạng di động trực tiếp sau khi mở khóa vùng  │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
┌───────────────────┐       ┌───────────────────┐       ┌────────────────────┐
│ 1. ĐIỀU PHỐI ĐỆM  │       │ 2. GIỮ THỨC MODEM │       │ 3. ĐỒNG BỘ THỰC TẾ │
│ & BĂNG THÔNG 4G   │       │ 4G (ANTI-DRX)     │       │ (LIVE-EDGE SYNC)   │
├───────────────────┤       ├───────────────────┤       ├────────────────────┤
│ • Đệm thu: 10-22ms│       │ • Nhịp giữ thức   │       │ • Tự xả khung hình │
│ • Cắt nhanh nghẽn │       │   uplink 28ms khi │       │   cũ tồn đọng khi  │
│   trạm phát (4-8M)│       │   tay cầm nghỉ    │       │   trễ đệm > 26ms   │
│ • Thu hồi đệm sau │       │ • Triệt tiêu 35ms │       │ • Ưu tiên phần cứng│
│   3 nhịp (4.5s)   │       │   trễ ngủ modem   │       │   H.264 0-B-frame  │
└─────────┬─────────┘       └─────────┬─────────┘       └─────────┬──────────┘
          └───────────────────────────┼───────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│               GIAO DIỆN ĐIỀU KHIỂN TRONG GAME (In-Game HUD)                │
│   • Gom nhóm cập nhật Telemetry (1.5s/lần) chống giật luồng giao diện      │
│   • Hiển thị trạng thái 4G Siêu Nhạy & độ trễ từng phần (Ping/Decode/Đệm)  │
│   • Nút [⚡ Xả Trễ] 1 chạm: làm rỗng bộ đệm & đồng bộ khung hình tức thì    │
└────────────────────────────────────────────────────────────────────────────┘
```

### Mô Hình Trạng Thái & Ánh Xạ Tương Tác (State & Interactive Mapping)

- **Trạng thái điều phối chất lượng & độ trễ 4G:**
  - `isCellular`: Cờ nhận diện kết nối 4G/5G thời gian thực từ hệ thống mạng Android, tự động chuyển dải bộ đệm mục tiêu sang `10.0ms – 22.0ms` (thay vì `12.0ms – 45.0ms`) và hạ ngưỡng thu hồi bộ đệm từ `6` chu kỳ (`9.0s`) xuống `3` chu kỳ ổn định (`4.5s`).
  - `rttTrendDelta` & `jitterMs`: Khi `isCellular == true` và phát hiện `rttTrendDelta >= 12ms` hoặc `jitterMs >= 10ms`, lập tức giảm trần bitrate xuống `4.0 – 6.5 Mbps` để giải phóng hàng đợi gói tin tại trạm thu phát 4G trước khi ping kịp tăng vọt.
- **Cơ chế Đồng bộ khung hình thời gian thực (Live-Edge Catch-up) & Xả trễ chủ động:**
  - **Tự động (`Live-Edge Sync`):** Trong vòng lặp kiểm tra luồng phát, nếu phát hiện độ trễ bộ đệm (`jitterBufferDelayMs`) vượt `26ms` hoặc thời gian phát `<video>` bị tụt lại sau điểm thời gian thực (`buffered.end - currentTime > 0.045s`), hệ thống tự động kích hoạt nhịp bắt kịp `playbackRate = 1.05` trong `250ms` kết hợp ép `playoutDelayHint = 0.010` để đốt sạch khung hình trễ tồn đọng.
  - **Chủ động qua nút `"⚡ Xả Trễ"` trên HUD:** Khi người chơi chạm nút **"⚡ Xả Trễ"**, sự kiện sẽ gọi đồng thời:
    1. Đặt lại bộ đệm mục tiêu về mức thấp nhất (`10.0ms`) và xóa lịch sử phạt trễ.
    2. Thực thi lệnh xả bộ đệm trực tiếp trên luồng WebRTC (`playoutDelayHint = 0.008`, `jitterBufferTarget = 10`, nhảy `currentTime` sát mép `buffered.end` và đồng bộ lại kênh tay cầm).
    3. Hiển thị thông báo nhanh xác nhận đã xả độ trễ thành công trên màn hình.
