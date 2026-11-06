# Kế Hoạch Tối Ưu Giảm Giật / Lắc Khi Duyệt Trang Xbox Và Khi Đang Chơi (Stream)

Kế hoạch này giải quyết tình trạng giật/khựng giao diện trong lúc duyệt danh mục Xbox và khi đang chơi game trên mây, dựa hoàn toàn trên bằng chứng mã nguồn thực tế mà không làm thay đổi giao diện hiển thị, logic chọn máy chủ hay các hàm cầu nối JavaScript (`@JavascriptInterface`).

## Quyết Định Đã Xác Nhận Từ Chủ Dự Án

> [!IMPORTANT]
> Các nguyên tắc và phạm vi đã được chủ dự án chốt:
> - **Phạm vi tối ưu**: Áp dụng đồng bộ cả 3 nhóm giải pháp: (1) Tách và đẩy việc đọc dữ liệu trạng thái (state) xuống đúng thành phần con thay vì đọc tất cả ở gốc màn hình; (2) Chống ghi trùng trạng thái khi giá trị không đổi; (3) Tạm dừng hoàn toàn vòng lặp đo độ trễ nền khi đang trong phiên phát luồng (stream).
> - **Ràng buộc an toàn**: Giữ nguyên 100% logic chọn máy chủ, các hàm cầu nối JavaScript (`@JavascriptInterface`) và giao diện nhìn thấy của người dùng.

---

## 1. Tổng Quan & Phân Tích Nguyên Nhân Gây Giật (Theo Thứ Tự Ảnh Hưởng)

Dưới đây là danh sách các nguyên nhân gây vẽ lại giao diện (recomposition) và chiếm dụng luồng chính (Main Thread) đã được xác minh trực tiếp trong mã nguồn:

### Hạng 1 (Ảnh hưởng lớn nhất khi đang chơi và duyệt web): Gốc màn hình trình duyệt thu thập toàn bộ trạng thái tổng (`VpnConnectionState`) thay đổi mỗi 5–7,5 giây
- **Bằng chứng trong mã nguồn**:
  - Gốc màn hình chơi game thu thập toàn bộ trạng thái VPN tại đầu hàm giao diện (dòng 97 của màn hình trình duyệt đám mây) và gốc màn hình chính cũng thu thập toàn bộ trạng thái này (dòng 119 của màn hình bảng điều khiển).
  - Trong khi đó, vòng lặp giám sát độ trễ nền (dòng 2620–2655 của bộ quản lý VPN) chạy liên tục mỗi **5 giây** (khi duyệt web) và **7,5 giây** (khi đang chơi).
  - Ngay cả khi đang chơi game (`liveWebRtcRtt > 0`), vòng lặp này vẫn tạo một danh sách lịch sử ping mới (`pingHistory`) và ghi đè vào trạng thái tổng mỗi 7,5 giây (dòng 2630–2635). Vì danh sách `pingHistory` luôn là đối tượng mới, toàn bộ gốc màn hình trình duyệt (bao gồm khung WebView, lớp phủ tay cầm ảo và các khối hiệu ứng) bị buộc phải kiểm tra và vẽ lại định kỳ.
  - Thực tế, gốc màn hình trình duyệt chỉ cần biết **máy chủ đang chọn**, **chế độ mạng** và **cờ nhắc đổi thị trường**, chứ không cần danh sách máy chủ hay lịch sử ping từng giây.

### Hạng 2 (Ảnh hưởng lớn khi cuộn/duyệt trang Xbox): Số câu đã dịch (`translationInfo`) cập nhật liên tục ở gốc màn hình trình duyệt
- **Bằng chứng trong mã nguồn**:
  - Gốc màn hình trình duyệt thu thập trực tiếp trạng thái dịch thuật (dòng 291), nhưng biến `translatedCount` chỉ được dùng duy nhất ở một chỗ là truyền vào thanh công cụ nổi (`IsolatedFloatingHud` tại dòng 1031).
  - Khi người dùng cuộn trang Xbox có bật dịch Tiếng Việt/quốc tế, bộ quan sát DOM trong trang web gọi về hàm cầu nối `onTranslationStatus` (dòng 2618–2621 của cầu nối Xbox, kích hoạt từ dòng 836 của bộ dịch). Mỗi đợt dịch văn bản mới đều gán lại đối tượng `TranslationInfo` mới (kể cả khi số câu dịch không đổi) và kích hoạt vẽ lại toàn bộ gốc màn hình trình duyệt ngay trong lúc người dùng đang vuốt cuộn trang.

### Hạng 3 (Ảnh hưởng khi sóng Wi-Fi/4G dao động): Bộ tối ưu mạng phát 2 lần cập nhật liên tiếp trên luồng chính mỗi khi cường độ sóng thay đổi nhẹ
- **Bằng chứng trong mã nguồn**:
  - Bộ tối ưu mạng khởi tạo trên luồng chính (`Dispatchers.Main`, dòng 75) và đăng ký lắng nghe thay đổi mạng từ hệ điều hành (`onCapabilitiesChanged`, dòng 132–139).
  - Trên Android, mỗi khi cường độ sóng Wi-Fi (`wifiRssiDbm`) hoặc băng thông ước tính nhích nhẹ (ví dụ từ `-54 dBm` sang `-55 dBm`), hàm đánh giá mạng (dòng 252–310) ghi vào trạng thái `_status.value` **2 lần liên tiếp**: lần 1 ở dòng 296 và lần 2 ngay sau đó ở dòng 421 trong hàm tính cấu hình.
  - Vì gốc màn hình trình duyệt thu thập toàn bộ trạng thái mạng (dòng 292) dù chỉ dùng 2 trường (`isCellular` và `effectiveBitrateMbps`), mỗi lần sóng Wi-Fi nhích 1 dBm nhưng chưa đổi ngưỡng đều làm gốc màn hình nhận 2 đợt cập nhật thừa.

### Hạng 4 (Ảnh hưởng khi bật quay màn hình và khi đếm ngược tắt VPN): Bộ đếm giây quay màn hình và băng rôn đếm ngược nằm ở gốc màn hình
- **Bằng chứng trong mã nguồn**:
  - Gốc màn hình trình duyệt thu thập toàn bộ trạng thái quay màn hình (dòng 124). Khi đang quay video `.MP4`, số giây đã quay (`elapsedSeconds`) tăng mỗi **1 giây**, làm toàn bộ gốc màn hình trình duyệt vẽ lại mỗi giây thay vì chỉ cập nhật viên thuốc HUD và hộp thoại cài đặt nhanh.
  - Khi vào game và đếm ngược tự tắt VPN (dòng 522–543 của bộ điều khiển luồng), mỗi giây đếm ngược cập nhật `StreamUiState`, trong khi gốc màn hình trình duyệt đang đọc cả `StreamUiState` (dòng 99).

### Hạng 5 (Kiểm tra ảnh bìa danh mục và hiệu ứng động vô hạn):
- **Đã kiểm chứng an toàn trong mã nguồn**:
  - Thẻ game trong Thư viện (dòng 174–188) và Trang chủ (dòng 472–486) đã giới hạn kích thước giải mã ảnh ở mức `320x440`, bật `allowRgb565(true)` và tắt `crossfade(false)`.
  - Hiệu ứng nhịp thở `rememberInfiniteTransition` trong màn hình chờ vào game (dòng 157) nằm bên trong khối `AnimatedVisibility(visible = isLoading)` nên tự động ngừng hoàn toàn ngay khi trang game tải đạt 80%.

---

## 2. Trải Nghiệm Người Dùng & Giao Diện

- **Giữ nguyên 100% thiết kế và tính năng**: Mọi thành phần giao diện (khung chơi game, tay cầm ảo, viên thuốc HUD, bảng thông số mạng, danh mục game) giữ nguyên hình thức và cách hoạt động.
- **Cải thiện độ mượt thực tế**:
  - Khi vuốt cuộn trang Xbox Cloud Gaming: không còn hiện tượng khựng nhẹ mỗi khi bộ dịch dịch xong một đoạn văn bản mới trên trang.
  - Khi đang chơi game (stream): loại bỏ hoàn toàn các đợt vẽ lại ngầm mỗi 5–7,5 giây từ bộ đo ping nền và các đợt vẽ lại thừa khi sóng Wi-Fi dao động nhẹ.

---

## 3. Quyết Định Kỹ Thuật & Đánh Đổi

- **Quyết định 1: Cô lập việc đọc trạng thái theo đúng thành phần tiêu thụ (State Scoping & `derivedStateOf`)**
  - *Cách làm*: Ở gốc màn hình trình duyệt đám mây, chỉ trích xuất các trường cấu trúc thật sự cần thiết thông qua `distinctUntilChanged` / `derivedStateOf` (như `currentServer`, `mode`, `unlockFailPromptServer`, `isCellular`, `effectiveBitrateMbps`). Đẩy việc thu thập chi tiết `vpnState`, `translationInfo`, `networkStatus` và `recorderState` vào thẳng bên trong `IsolatedFloatingHud` và các thành phần con riêng biệt.
  - *Lý do*: Ngăn chặn triệt để việc toàn bộ hàm giao diện gốc phải chạy lại khi các chỉ số thời gian thực (ping, số từ đã dịch, số giây quay video, cường độ dBm Wi-Fi) thay đổi.
- **Quyết định 2: Gộp cập nhật trạng thái mạng và chỉ phát trạng thái mới khi giá trị thực sự thay đổi**
  - *Cách làm*: Trong bộ tối ưu mạng, gộp 2 lần gán `_status.value` trong `evaluateCurrentNetwork` + `recomputeEffectiveProfile` thành **1 lần gán duy nhất**, đồng thời làm tròn/lọc biến động nhỏ của `wifiRssiDbm` khi chưa vượt ngưỡng sóng yếu để tránh phát trạng thái mới liên tục trên luồng chính. Trong cầu nối dịch thuật, chỉ cập nhật `_translationInfo.value` khi `enabled` hoặc `count` thực sự khác giá trị cũ.
  - *Lý do*: Giảm số lần phát sự kiện thừa trên `StateFlow` ngay từ gốc.
- **Quyết định 3: Tạm dừng hẳn vòng lặp đo độ trễ nền khi đang trong phiên chơi (`isInGameStreamActive`)**
  - *Cách làm*: Trong vòng lặp `startLatencyMonitoring()` của bộ quản lý VPN, khi đang trong phiên chơi thực tế (`isInGameStreamActive` và đã có luồng WebRTC), chỉ cập nhật `pingHistory` khi độ trễ chênh lệch đáng kể hoặc khi người dùng mở HUD, và không tạo danh sách mới nếu giá trị không đổi; tuyệt đối không chạy đo TCP/HTTP nền khi đang mở màn hình chơi game.
  - *Lý do*: Nhường tối đa chu kỳ CPU và luồng chính cho bộ giải mã video phần cứng WebRTC.

---

## 4. Kiến Trúc Kỹ Thuật & Luồng Dữ Liệu Sau Tối Ưu

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CloudBrowserScreen (Gốc màn hình)                    │
│  Chỉ lắng nghe các trường cấu trúc ít thay đổi (distinctUntilChanged):   │
│  • Server hiện tại & Chế độ mạng (mode)                                 │
│  • Cấu hình luồng (isCellular, effectiveBitrateMbps)                    │
│  • Trạng thái vào trận (isGameActive, isStreamConnected)                │
└───────────────┬─────────────────────────────────────────┬───────────────┘
                │                                         │
                ▼                                         ▼
┌───────────────────────────────┐         ┌───────────────────────────────┐
│   AndroidView (Xbox WebView)  │         │      IsolatedFloatingHud      │
│ • Không bị vẽ lại khi nhảy    │         │ Tự thu thập riêng bên trong:  │
│   ping, dịch chữ hay đếm giây │         │ • VpnManager.state (ping)     │
│ • Chỉ chạy evaluateJavascript │         │ • XboxBridge.translationInfo  │
│   khi cấu hình thực sự đổi    │         │ • NetworkOptimizer.telemetry  │
└───────────────────────────────┘         │ • ZeroLagScreenRecorder.state │
                                          └───────────────────────────────┘
```

### Tóm tắt các điểm cập nhật kỹ thuật:
1. **Tối ưu gốc màn hình trình duyệt (`CloudBrowserScreen`)**:
   - Đưa việc thu thập `XboxBridge.translationInfo`, `ZeroLagScreenRecorder.state`, và toàn bộ `VpnManager.state` / `NetworkOptimizer.status` vào bên trong `IsolatedFloatingHud` (và thành phần băng rôn/màn hình chờ con), còn ở gốc màn hình chỉ lắng nghe các trường cấu trúc bằng `remember` + `distinctUntilChanged`.
   - Kiểm tra các khối `LaunchedEffect` gọi `evaluateJavascript` để đảm bảo chỉ gọi khi `webViewInstance != null` và khi giá trị cấu hình thực sự thay đổi.
2. **Tối ưu bộ quản lý VPN (`VpnManager`)**:
   - Trong `startLatencyMonitoring()`: không tạo danh sách `pingHistory` mới hay ghi lại `_state.value` khi đang ở màn hình chơi game nếu giá trị độ trễ không thay đổi đáng kể; đảm bảo dừng mọi phép đo ping TCP nền khi `isInGameStreamActive == true`.
3. **Tối ưu bộ tối ưu mạng (`NetworkOptimizer`) và cầu nối dịch (`XboxBridge`)**:
   - Gộp 2 lần gán `_status.value` liên tiếp trong `evaluateCurrentNetwork()` và `recomputeEffectiveProfile()` thành 1 lần gán duy nhất.
   - Trong `XboxBridge.onTranslationStatus()`, kiểm tra nếu `enabled` và `count` bằng đúng giá trị hiện tại thì bỏ qua không phát lại sự kiện.
