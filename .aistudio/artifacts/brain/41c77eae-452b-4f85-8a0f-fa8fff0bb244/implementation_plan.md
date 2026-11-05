# Kế hoạch Cải tiến Hệ thống Tự chọn Máy chủ & Vùng phát (Mục 1–8 — Bản hiệu đính theo Code thật)

## 1. Báo cáo hiện trạng trước khi sửa (Đối chiếu Code thật)

### (a) Máy chủ/vùng mặc định lần mở app đầu tiên được chọn bằng cách nào?
- Trong `VpnState.kt` (dòng 902), `VpnConnectionState` khởi tạo cứng:
  `val currentServer: ServerLocation = DEFAULT_SERVERS[0]` (máy chủ `jp_tsukuba_softether_7` tại Nhật Bản).
- Trong `VpnManager.init(context)` (`VpnManager.kt` dòng 85–96), hàm `loadPersistedServers(context)` đọc khóa `"selected_server_id"` từ `SharedPreferences("cloudplay_servers")`. Ở lần mở app đầu tiên, khóa này là `null`, nên `currentServer` giữ nguyên `DEFAULT_SERVERS[0]`.
- Tiếp đó, khi mở tab Máy chủ (`ServersScreen.kt` dòng 150–154), `LaunchedEffect(Unit)` gọi `VpnManager.runV130SmartRoutingUpgradeStep(context = context, force = false)` **trước khi** `VpnManager.testAllServers` kịp đo ping (`serverPings` lúc này đang rỗng). Do `serverPings` rỗng, `sortServersByUnlockAndPing` (`VpnManager.kt` dòng 563–588) rơi vào nhánh sắp xếp theo thứ hạng khoảng cách địa lý (`RegionalProbeTester.getExactCountryDistanceRank`) và gán thẳng sang `kr_seoul_kt_1` mà chưa có bất kỳ mẫu đo ping thật nào.

### (b) Chuỗi gọi hiện tại khi tự chọn máy chủ & hàm quyết định kết quả cuối cùng
Có 4 luồng gọi chính đều hội tụ về **hàm quyết định cuối cùng là `VpnManager.sortServersByUnlockAndPing`** (`VpnManager.kt` dòng 533–671):
1. **Luồng kiểm tra GeoIP khi khởi động (`checkCurrentIp` → `updateSmartDecision`)**:
   - `VpnManager.checkCurrentIp()` (`VpnManager.kt` dòng 1482) gọi `GeoIpDetector.checkCurrentIp()` rồi truyền vào `updateSmartDecision(result)` (dòng 1504).
   - Nếu người dùng ở vùng chưa hỗ trợ (VD: `VN`), `updateSmartDecision` lọc máy chủ bằng `RegionalProbeTester.isWithinAutoSelectDistanceLimit` (dòng 351, giới hạn `MAX_AUTO_SELECT_DISTANCE_KM = 5500.0` và `MAX_AUTO_SELECT_DELTA_KM = 2500.0`) và `RegionalProbeTester.getNearestSupportedCountryTier` (dòng 415), sau đó gọi `sortServersByUnlockAndPing(...)` ở dòng 1580 để lấy phần tử đầu tiên.
2. **Luồng quét toàn bộ máy chủ (`VpnManager.testAllServers`)**:
   - `testAllServers(autoPickBest, ...)` (`VpnManager.kt` dòng 839–1114) chọn tối đa 2 máy chủ đại diện mỗi nước ở Bước 1 (`selectStep1RepresentativeServers`, dòng 710), đo bằng `PingTester.measureServerEndpoint` (`PingTester.kt` dòng 191), sau đó đo Bước 2 cho các máy chủ còn lại.
   - Ở dòng 978, hàm đọc `val autoBypassMode = com.example.data.SettingsRepository.config.value.autoBypassEnabled`.
   - Ở dòng 1007–1028, nếu `shouldAutoSwitchToOptimal` là `true`, hàm gọi `filterServersWithinDistanceLimit` rồi gọi `sortServersByUnlockAndPing(..., prioritizeDistanceForAutoBypass = autoBypassMode)` (dòng 1020) và lấy `sortedCandidates.firstOrNull { isReachable }` làm `bestServer`.
3. **Luồng chọn trong một quốc gia (`VpnManager.autoSelectBestServer`)**:
   - `autoSelectBestServer(countryCode, context)` (`VpnManager.kt` dòng 1869–1949) lọc các máy chủ của `countryCode`, đo ping nếu chưa có, rồi gọi `sortServersByUnlockAndPing` (dòng 1916) để lấy máy chủ đứng đầu.
4. **Luồng nâng cấp định tuyến (`VpnManager.runV130SmartRoutingUpgradeStep`)**:
   - `runV130SmartRoutingUpgradeStep(context, force)` (`VpnManager.kt` dòng 1233–1289) đọc `val autoBypass = com.example.data.SettingsRepository.config.value.autoBypassEnabled` (dòng 1244) rồi gọi `filterServersWithinDistanceLimit` + `sortServersByUnlockAndPing(..., prioritizeDistanceForAutoBypass = autoBypass)` (dòng 1255).

### (c) Những chỗ dùng `speedMbps` cố định hoặc số ước lượng giả định
1. **Số ước lượng ping giả định (`estimateDirectInGamePingMs`)**:
   - Hàm `RegionalProbeTester.estimateDirectInGamePingMs(countryCode: String, geo: GeoIpResult? = VpnManager.state.value.detectedGeoIp, allowTimezoneFallback: Boolean = true): Int?` (`RegionalProbeTester.kt` dòng 487–509) trả về số cứng `70` cho `KR` (khi `distKm in 1500.0..4200.0`), `80` cho `JP` (khi `distKm in 2500.0..5200.0`), `35` nếu `distKm <= 1200.0`, `70` nếu `distKm <= 3600.0`, `80` nếu `distKm <= 5000.0`, và `(35 + (distKm * 0.014)).toInt()` cho các trường hợp khác.
   - `ServersScreen.kt` (dòng 1420–1427) gọi `RegionalProbeTester.estimateDirectInGamePingMs(server.countryCode, vpnState.detectedGeoIp, allowTimezoneFallback = true)` khi `config.autoBypassEnabled` bật và hiển thị con số giả định này dưới dạng `"~${estDirectPing}ms khi tắt VPN"`.
   - Chuỗi văn bản ghi cứng `"KR ~70ms"` xuất hiện tại `VpnManager.kt` dòng 1279 và `ServersScreen.kt` dòng 833.
2. **Trường `speedMbps` cố định**:
   - Toàn bộ 60 máy chủ trong `DEFAULT_SERVERS` (`VpnState.kt` dòng 122–848) đều khai báo cứng `speedMbps` từ `310` đến `700`. Dù `sortServersByUnlockAndPing` hiện không dùng `speedMbps` để sắp xếp, trường này vẫn tồn tại trong model `ServerLocation` (`VpnState.kt` dòng 59), lưu JSON (`VpnManager.kt` dòng 1326, 1415, 1448) và hộp thoại thêm proxy (`ServersScreen.kt` dòng 1575, 1697, 1774, 1792).

---

## 2. Phương án kỹ thuật chi tiết cho từng mục (Mục 2–8)

### Mục 2: Bỏ số ping bịa, thay bằng thống kê phiên chơi thật theo vùng/máy chủ
- **Hiện trạng**:
  - `SessionHistoryRepository.getAverageRttForServerOrRegion(serverName: String, countryCode: String, sessionsList: List<PlaySessionRecord>)` (`SessionHistoryRepository.kt` dòng 196–211) hiện chỉ so khớp `it.serverName.trim().equals(cleanTarget, ignoreCase = true)` mà bỏ qua tham số `countryCode`.
  - Model `PlaySessionRecord` (`SessionHistoryRepository.kt` dòng 13–33) dùng trường `val avgPingMs: Int` (dòng 20), `val pingSamples: List<Int>` (dòng 25), `val framesDropped: Int` (dòng 26), `val freezeCount: Int` (dòng 27), `val durationSeconds: Long` (dòng 17), `val framesDecoded: Int` (dòng 32).
- **Giải pháp**:
  1. Trong `SessionHistoryRepository.kt`, tạo **data class mới** `RegionSessionStats`:
     ```kotlin
     data class RegionSessionStats(
         val avgRttMs: Int,
         val sampleCount: Int,
         val weightedFreezePerMin: Double,
         val weightedFrameDropPercent: Double
     )
     ```
     và **hàm mới** `getRegionSessionStats(serverName: String, countryCode: String, sameCountryServerNames: Set<String> = emptySet(), maxRecentSessions: Int = 5, sessionsList: List<PlaySessionRecord> = _sessions.value): RegionSessionStats?`.
  2. Hàm lọc các phiên chơi gần nhất có `avgPingMs > 0` khớp với `serverName` hoặc thuộc cùng vùng (`sameCountryServerNames` / nhãn quốc gia trong `serverName`), lấy tối đa `maxRecentSessions` (mặc định 5 phiên gần nhất) để tính trung bình có trọng số thời gian của `avgPingMs`, số mẫu `sampleCount`, tỷ lệ `freezeCount` trên phút và tỷ lệ phần trăm `framesDropped / framesDecoded`.
  3. Xóa hàm `estimateDirectInGamePingMs` khỏi `RegionalProbeTester.kt` (dòng 487–509) và gỡ bỏ chuỗi ghi cứng `"KR ~70ms"` tại `VpnManager.kt` (dòng 1279) & `ServersScreen.kt` (dòng 833).
  4. Tại `ServersScreen.kt` (dòng 1420–1458), thay biến `estDirectPing` và nhãn `"~${estDirectPing}ms khi tắt VPN"` bằng dữ liệu thật từ `SessionHistoryRepository.getRegionSessionStats`:
     - Nếu có dữ liệu (`stats != null && stats.sampleCount > 0`): hiển thị `"~${stats.avgRttMs}ms • ${stats.sampleCount} phiên gần nhất"` (EN: `"~${stats.avgRttMs}ms • last ${stats.sampleCount} sessions"`).
     - Nếu chưa có dữ liệu (`stats == null`): hiển thị `"Chưa có dữ liệu"` (EN: `"No data yet"`), tuyệt đối không hiện số ước lượng.

### Mục 3: Chọn vùng mặc định có thuật toán (Probe thật ngắn, `DEFAULT_SERVERS[0]` chỉ là dự phòng cuối)
- **Giải pháp (dùng hàm có sẵn + sửa hàm nâng cấp khởi động)**:
  1. Sử dụng các hàm có sẵn:
     - `RegionalProbeTester.getNearestSupportedCountriesForUser(servers: List<ServerLocation>, geo: GeoIpResult?): List<String>` (`RegionalProbeTester.kt` dòng 277) để xếp thứ tự ban đầu các nước thuộc `GeoIpDetector.SUPPORTED_COUNTRIES` (`KR`, `JP`, `US`) theo khoảng cách địa lý từ tọa độ người dùng (`haversineDistanceKm`).
     - `VpnManager.selectStep1RepresentativeServers(candidates: List<ServerLocation>): List<ServerLocation>` (`VpnManager.kt` dòng 710) để lấy các máy chủ đại diện của những nước đứng đầu.
     - `PingTester.measureServerEndpoint(server: ServerLocation, timeoutMs: Int, sampleCount: Int, forceRefresh: Boolean): EndpointPingMeasurement` (`PingTester.kt` dòng 191) để đo RTT thật với timeout ngắn (`1200ms`, `sampleCount = 2`).
  2. Trong `VpnManager.kt`, tạo **hàm suspend mới** `ensureInitialOrGeoUpdatedOptimalServer(context: Context, force: Boolean = false): ServerLocation?` (thay cho cách chọn mù khi `serverPings` còn rỗng trong `runV130SmartRoutingUpgradeStep` ở `VpnManager.kt` dòng 1233):
     - Khi mở app lần đầu (chưa có `"selected_server_id"` hoặc kết quả tự chọn đã quá 6 giờ), khi quốc gia trong `detectedGeoIp` thay đổi, hoặc khi chưa có dữ liệu đo:
       - Lấy danh sách nước gần nhất từ `RegionalProbeTester.getNearestSupportedCountriesForUser`.
       - Nếu chưa có kết quả đo trong `serverMeasurements`, chạy `PingTester.measureServerEndpoint` ngắn (`timeoutMs = 1200`, `sampleCount = 2`) tới các máy chủ đại diện của 2 nước gần nhất.
       - Chấm điểm từng máy chủ bằng `calculateServerSelectionScore` (Mục 4) và chọn máy chủ có điểm số tốt nhất.
       - `DEFAULT_SERVERS[0]` chỉ còn là giá trị khởi tạo tạm trong bộ nhớ hoặc phương án dự phòng cuối cùng khi mọi phép đo mạng đều trả về `ServerPingStatus.UNREACHABLE` và không có lịch sử phiên chơi.

### Mục 4: Công thức điểm số thống nhất trong một hàm duy nhất (`calculateServerSelectionScore`)
- **Giải pháp**:
  Tạo **hàm mới** `internal fun calculateServerSelectionScore(...)` trong `VpnManager.kt` (được gọi bởi `sortServersByUnlockAndPing` tại `VpnManager.kt` dòng 533) với chữ ký và các hằng số rõ ràng:
  ```kotlin
  internal fun calculateServerSelectionScore(
      server: ServerLocation,
      measurement: EndpointPingMeasurement?,
      fallbackPingMs: Int?,
      unlockStatus: RegionUnlockStatus,
      sessionStats: SessionHistoryRepository.RegionSessionStats?,
      autoBypassEnabled: Boolean,
      distanceKm: Int?
  ): Double
  ```
  - **Trọng số RTT theo đúng lựa chọn của chủ dự án (`config.autoBypassEnabled` — cờ "Tự động tắt VPN khi vào game" tại `AppSettings.kt` dòng 616)**:
    - Khi `autoBypassEnabled == true` (Tự động tắt VPN khi vào game):
      - `WEIGHT_SESSION_RTT_AUTO_BYPASS = 0.75` (75% RTT lịch sử phiên chơi trực tiếp `sessionStats.avgRttMs`).
      - `WEIGHT_UNLOCK_RTT_AUTO_BYPASS = 0.25` (25% RTT mở khóa qua VPN `measuredRttMs`).
      - Nếu vùng đó chưa có lịch sử phiên chơi (`sessionStats == null`), dùng 100% `measuredRttMs` làm thành phần RTT chính; nếu chưa đo được VPN RTT nhưng đã có lịch sử phiên chơi thì dùng 100% `sessionStats.avgRttMs`.
    - Khi `autoBypassEnabled == false` (Giữ VPN trong suốt lúc chơi):
      - `WEIGHT_VPN_RTT_KEEP_TUNNEL = 0.75` (75% RTT qua VPN `measuredRttMs`).
      - `WEIGHT_SESSION_RTT_KEEP_TUNNEL = 0.25` (25% RTT lịch sử phiên chơi, hoặc 100% `measuredRttMs` nếu chưa có lịch sử).
  - **Trọng số thời gian cho các phiên chơi (`SessionHistoryRepository.getRegionSessionStats`)**:
    - `RECENCY_DECAY_FACTOR = 0.75`: Phiên mới nhất có trọng số $1.0$, các phiên cũ hơn giảm dần theo $0.75^i$ ($i = 0, 1, 2, \dots$).
  - **Điểm phạt từ lịch sử phiên chơi (`PlaySessionRecord`)**:
    - `PENALTY_PER_FREEZE_PER_MIN = 35.0`: Điểm phạt cho mỗi lần đứng hình/phút (`freezeCount / max(durationSeconds / 60.0, 0.5)`).
    - `PENALTY_PER_PERCENT_FRAME_DROP = 8.0`: Điểm phạt cho mỗi 1% khung hình bị rớt (`(framesDropped.toDouble() / framesDecoded.toDouble()) * 100.0` khi `framesDecoded > 0`).
  - **Điểm phạt trạng thái mở khóa (`RegionUnlockStatus` tại `VpnState.kt` dòng 17–21) và kết nối (`ServerPingStatus` tại `PingTester.kt` dòng 21–28)**:
    - `PENALTY_UNLOCK_FAILED_OR_WRONG_REGION = 10_000.0`: Áp dụng khi `unlockStatus == RegionUnlockStatus.FAILED` hoặc `unlockStatus == RegionUnlockStatus.WRONG_REGION`.
    - `PENALTY_PING_UNREACHABLE = 15_000.0`: Áp dụng khi `measurement?.status == ServerPingStatus.UNREACHABLE`.
    - `PENALTY_PING_FALLBACK_PORT_ONLY = 1_500.0`: Áp dụng khi `measurement?.status == ServerPingStatus.MEASURED_FALLBACK_ONLY`.
    - `PENALTY_UNLOCK_UNTESTED = 40.0`: Điểm phạt nhẹ khi `unlockStatus == RegionUnlockStatus.UNTESTED` so với máy chủ đã `RegionUnlockStatus.UNLOCKED` (`0.0`).
  - **Khoảng cách địa lý (`RegionalProbeTester.getServerDistanceKm` tại `RegionalProbeTester.kt` dòng 330)**:
    - Chỉ dùng làm điểm ước tính ban đầu (`distanceKm * 0.025`) khi **cả** RTT đo thật lẫn RTT lịch sử phiên đều chưa có, hoặc làm số phân định rất nhỏ (`distanceKm * 0.001`) khi hai máy chủ có điểm bằng nhau.
  - **Không dùng `speedMbps`**: Không đọc `server.speedMbps` ở bất kỳ bước tính điểm hay sắp xếp nào.

### Mục 5: Chống nhảy máy chủ (Hysteresis) & Bảo vệ lựa chọn thủ công của người dùng
- **Giải pháp**:
  1. **Cờ bảo vệ lựa chọn thủ công**:
     - Thêm trường mới `val userManuallySelectedServer: Boolean = false` vào `VpnConnectionState` (`VpnState.kt` dòng 898) và lưu trong `SharedPreferences("cloudplay_servers")` với khóa `"user_manually_selected_server"`.
     - Sửa hàm có sẵn `VpnManager.selectServer(server: ServerLocation, context: Context? = null, isManualSelection: Boolean = true)` (`VpnManager.kt` dòng 179):
       - Khi người dùng chạm chọn máy chủ trên UI (`ServersScreen.kt`), truyền `isManualSelection = true` để lưu `userManuallySelectedServer = true`.
       - Khi hệ thống tự động chọn hoặc khi người dùng bấm nút **"Chọn tối ưu"** (`testAllServers(autoPickBest = true)` tại `ServersScreen.kt` dòng 167), đặt `userManuallySelectedServer = false`.
     - Khi `userManuallySelectedServer == true`: các luồng quét ngầm/đổi mạng không tự động đổi máy chủ, trừ khi máy chủ đang chọn bị `ServerPingStatus.UNREACHABLE` hoặc `RegionUnlockStatus.FAILED` / `RegionUnlockStatus.WRONG_REGION`.
  2. **Hàm kiểm tra ngưỡng chống nhảy máy chủ (Hysteresis)**:
     - Tạo **hàm mới** `internal fun shouldAutoSwitchServer(currentServer: ServerLocation, candidateServer: ServerLocation, currentScore: Double, candidateScore: Double, currentMeasurement: EndpointPingMeasurement?, currentUnlockStatus: RegionUnlockStatus, userManuallySelected: Boolean, forcePickBest: Boolean): Boolean` trong `VpnManager.kt`:
     - Nếu `currentServer.id == candidateServer.id` → `false`.
     - Nếu máy chủ hiện tại đang sống (`currentMeasurement?.status == ServerPingStatus.MEASURED_PRIMARY`) và không bị `RegionUnlockStatus.FAILED` / `WRONG_REGION`, chỉ tự động chuyển sang `candidateServer` khi:
       - Điểm số cải thiện **≥ 15%**: `(currentScore - candidateScore) / currentScore >= 0.15`
       - **VÀ** mức chênh lệch điểm số/RTT cải thiện **≥ 10ms**: `(currentScore - candidateScore) >= 10.0`.

### Mục 6: Vòng đời làm mới hợp lý (6 giờ, đổi mạng Wi-Fi ↔ Di động, không đo khi đang stream, tiết kiệm trên mạng di động)
- **Giải pháp (dùng `com.example.cloudplay.NetworkOptimizer` có sẵn tại `NetworkOptimizer.kt` dòng 73)**:
  1. Lưu dấu thời gian `"last_auto_select_timestamp_ms"`, loại mạng `"last_auto_select_network_type"` (lấy từ `NetworkOptimizer.status.value.isWifi` / `isCellular` tại `NetworkOptimizer.kt` dòng 38–39) và mã quốc gia `"last_auto_select_user_country"` trong `SharedPreferences("cloudplay_servers")`.
  2. Hằng số `AUTO_SELECT_CACHE_TTL_MS = 6L * 60L * 60L * 1000L` (6 giờ). Nếu chưa quá 6 giờ, cùng loại mạng (`WIFI` vs `CELLULAR`) và cùng quốc gia `detectedGeoIp?.countryCode`, giữ nguyên kết quả tự chọn.
  3. Theo dõi thay đổi giữa Wi-Fi và dữ liệu di động từ `NetworkOptimizer.status` (`NetworkOptimizer.kt` dòng 78): khi chuyển đổi `isWifi` ↔ `isCellular` (hoặc khi người dùng bấm nút làm mới/đo ping trên `ServersScreen.kt`), kích hoạt đánh giá lại máy chủ tối ưu.
  4. **Chặn tuyệt đối khi đang stream game**:
     - Kiểm tra `NetworkOptimizer.status.value.isLowLatencyStreamActive` (`NetworkOptimizer.kt` dòng 50) hoặc `_state.value.activeStreamServerId != null` (`VpnState.kt` dòng 927) ở đầu `VpnManager.testAllServers`, `VpnManager.autoSelectBestServer`, và `VpnManager.ensureInitialOrGeoUpdatedOptimalServer`. Nếu đang trong phiên stream, hủy ngay việc đo ping/probe để không làm giật luồng chơi.
  5. **Chế độ nhẹ trên mạng di động**:
     - Khi `NetworkOptimizer.status.value.isCellular == true` (`NetworkOptimizer.kt` dòng 39) và không phải người dùng bấm quét toàn bộ thủ công (`!forceRefresh`), Bước 1 chỉ lấy tối đa 1 máy chủ đại diện cho 2 quốc gia gần nhất và gọi `PingTester.measureServerEndpoint(..., timeoutMs = 1200, sampleCount = 2)`.

### Mục 7: Minh bạch trên giao diện (`ServersScreen.kt`)
- **Giải pháp**:
  - Tạo **hàm mới** `VpnManager.buildServerSelectionReasonText(server: ServerLocation, isEn: Boolean, geo: GeoIpResult? = _state.value.detectedGeoIp, measurementsMap: Map<String, EndpointPingMeasurement> = _state.value.serverMeasurements, pingMap: Map<String, Int> = _state.value.serverPings, sessions: List<PlaySessionRecord> = SessionHistoryRepository.sessions.value): String` trong `VpnManager.kt`.
  - Chỉ ghép từ các số liệu thật hiện có:
    - Khoảng cách từ `RegionalProbeTester.getServerDistanceKm(server, geo, allowTimezoneFallback = true)`: ví dụ `"Gần: ~3.000 km"` (EN: `"Near: ~3,000 km"`).
    - RTT đo thật từ `measurementsMap[server.id]?.rttMs` hoặc `pingMap[server.id]`: ví dụ `"đo 76ms"` (EN: `"measured 76ms"`).
    - Lịch sử phiên chơi từ `SessionHistoryRepository.getRegionSessionStats`: ví dụ `"5 phiên gần nhất"` hoặc `"TB 76ms • 5 phiên gần nhất"` (EN: `"last 5 sessions"`).
    - Nếu thành phần nào chưa có số đo thật thì bỏ qua thành phần đó; tuyệt đối không chèn số giả định.
  - Hiển thị dòng lý do này ngay bên dưới tên máy chủ đang chọn trên thẻ trạng thái chính và thẻ máy chủ trong `ServersScreen.kt` (kèm `contentDescription`).

### Mục 8: Kiểm thử đơn vị (`ExampleRobolectricTest.kt`)
- **Giải pháp**:
  - Trong `/app/src/test/java/com/example/ExampleRobolectricTest.kt`:
    1. Cập nhật bài test cũ đang gọi `estimateDirectInGamePingMs` sang kiểm tra `SessionHistoryRepository.getRegionSessionStats` (trả về `null` khi chưa có phiên chơi, và trả về đúng trung bình RTT + số mẫu khi có danh sách `PlaySessionRecord`).
    2. Thêm bài test `serverScoring_vietnamUser_krLowPingLowFreeze_beatsJp`:
       - Tạo dữ liệu giả lập bằng đúng model thật: `GeoIpResult(ip = "1.2.3.4", countryCode = "VN", latitude = 21.0285, longitude = 105.8542)`, `RegionUnlockStatus.UNLOCKED`, `EndpointPingMeasurement` với `ServerPingStatus.MEASURED_PRIMARY` (`KR` rttMs = 76, `JP` rttMs = 89), và danh sách `PlaySessionRecord` với các trường thật (`avgPingMs = 76`, `freezeCount = 0`, `framesDropped = 5`, `framesDecoded = 10000`, `durationSeconds = 600` cho `KR`; `avgPingMs = 89`, `freezeCount = 3`, `framesDropped = 250`, `framesDecoded = 10000`, `durationSeconds = 600` cho `JP`).
       - Kiểm tra `VpnManager.calculateServerSelectionScore` và `VpnManager.sortServersByUnlockAndPing(..., prioritizeDistanceForAutoBypass = true)` xếp `KR` lên đầu.
    3. Thêm bài test `serverScoring_krHighFreezeAndFrameDrops_switchesToJp`:
       - Giữ `KR` có ping đo 76ms nhưng tạo các bản ghi `PlaySessionRecord` của `KR` có `freezeCount = 6`, `durationSeconds = 120` (3 lần đứng hình/phút), `framesDropped = 600`, `framesDecoded = 10000` (rớt 6% khung hình), trong khi `JP` có ping 89ms nhưng `freezeCount = 0`, `framesDropped = 10`, `framesDecoded = 10000`.
       - Kiểm tra điểm số của `JP` thấp hơn (tốt hơn) `KR` và `sortServersByUnlockAndPing` chọn `JP` thay vì `KR`.
    4. Thêm bài test `shouldAutoSwitchServer_enforcesHysteresisAndManualSelection`:
       - Kiểm tra ngưỡng cải thiện `< 15%` hoặc `< 10ms` không gây nhảy máy chủ, và `userManuallySelected = true` giữ nguyên máy chủ khi máy chủ đó vẫn `ServerPingStatus.MEASURED_PRIMARY` và `RegionUnlockStatus.UNLOCKED`.
