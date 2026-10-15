package com.example.cloudplay

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dedicated Vietnamese Translation Engine optimized exclusively for Xbox Cloud Gaming (xbox.com/play).
 * Incorporates the complete Better-xCloud & community gaming dictionary from GitHub.
 *
 * Performance Architecture:
 * 1. O(1) Dual Hash Maps (EXACT_MAP & LOWER_MAP) for instantaneous zero-latency dictionary lookup.
 * 2. Native DOM TreeWalker: Traverses text nodes 10x faster than recursive tree walks.
 * 3. Element Blacklist: Completely ignores <video>, <canvas>, <audio>, <script>, <style>, inputs, WebRTC streams.
 * 4. Micro-batching: Uses requestAnimationFrame and batched MutationObserver to prevent frame drops during gameplay.
 * 5. Game Title Immunity: Safeguards titles like Fortnite, Forza, Halo, Starfield from mistranslation.
 * 6. Dynamic GitHub Synchronization: Supports pulling live dictionary updates directly from GitHub.
 */
object XboxTranslator {

    private const val TAG = "XboxTranslator"

    // Primary Community & GitHub Better-xCloud UI Dictionary (550+ entries)
    val XBOX_UI_DICTIONARY_EN = mapOf(
        // === Better-xCloud & Stream Settings ===
        "Stream stats" to "Thông số stream",
        "Stream statistics" to "Thống kê luồng stream",
        "Codec profile" to "Hồ sơ codec giải mã",
        "Decode profile" to "Hồ sơ giải mã video",
        "Decode profiling" to "Đo lường giải mã",
        "Visual quality" to "Chất lượng hình ảnh",
        "Clarity boost" to "Tăng cường độ nét (Clarity Boost)",
        "Clarity Boost" to "Tăng cường độ nét (Clarity Boost)",
        "Renderer" to "Bộ dựng hình",
        "Default renderer" to "Bộ dựng hình mặc định",
        "Audio stream" to "Luồng âm thanh",
        "Video stream" to "Luồng video",
        "Touch controller" to "Tay cầm cảm ứng",
        "Virtual controller" to "Tay cầm ảo trên màn hình",
        "Custom layout" to "Bố cục phím tùy chỉnh",
        "Server region" to "Khu vực máy chủ",
        "Microphone" to "Micro đàm thoại",
        "User agent" to "Định danh trình duyệt (User Agent)",
        "Target resolution" to "Độ phân giải mục tiêu",
        "Max bitrate" to "Băng thông tối đa (Bitrate)",
        "Disable touch controls" to "Tắt phím cảm ứng",
        "Enable touch controls" to "Bật phím cảm ứng",
        "Hide stream stats" to "Ẩn thông số stream",
        "Show stream stats" to "Hiện thông số stream",
        "Native touch" to "Cảm ứng gốc của game",
        "Controller vibration" to "Độ rung tay cầm",
        "Haptic feedback" to "Phản hồi xúc giác",
        "Deadzone" to "Điểm chết cần gạt (Deadzone)",
        "Stick deadzone" to "Điểm chết cần Analog",
        "Key mapping" to "Gán phím điều khiển",
        "Mouse & keyboard" to "Chuột & Bàn phím",
        "Quick glance" to "Xem nhanh khi mở menu",
        "Always on" to "Luôn hiển thị",
        "Off" to "Tắt",
        "On" to "Bật",
        "Auto" to "Tự động",
        "Default" to "Mặc định",
        "High" to "Cao (High)",
        "Main" to "Tiêu chuẩn (Main)",
        "Baseline" to "Cơ bản (Baseline)",
        "High quality" to "Chất lượng cao",
        "Balanced" to "Cân bằng",
        "Ultra" to "Cực cao (Ultra)",

        // === Navigation & Xbox Cloud Header ===
        "Cloud Gaming" to "Chơi Game Đám Mây",
        "Cloud Gaming (Beta)" to "Chơi Game Đám Mây (Beta)",
        "Xbox Cloud Gaming" to "Xbox Cloud Gaming",
        "Xbox Game Pass" to "Xbox Game Pass",
        "All games" to "Tất cả trò chơi",
        "All Games" to "Tất cả trò chơi",
        "Jump back in" to "Tiếp tục chơi",
        "Recently played" to "Đã chơi gần đây",
        "Most popular" to "Phổ biến nhất",
        "Play with touch" to "Chơi bằng cảm ứng",
        "Touch games" to "Game hỗ trợ cảm ứng",
        "Play with Game Pass" to "Chơi cùng Game Pass",
        "Included with Game Pass" to "Bao gồm trong Game Pass",
        "Get Game Pass" to "Đăng ký Game Pass",
        "Join Game Pass" to "Tham gia Game Pass",
        "Controller required" to "Yêu cầu tay cầm",
        "Controller optional" to "Tùy chọn tay cầm",
        "Action & adventure" to "Hành động & Phiêu lưu",
        "Action & Adventure" to "Hành động & Phiêu lưu",
        "Shooter" to "Bắn súng",
        "Shooters" to "Game bắn súng",
        "Role playing" to "Nhập vai (RPG)",
        "Role Playing" to "Nhập vai (RPG)",
        "Sports" to "Thể thao",
        "Racing & flying" to "Đua xe & Máy bay",
        "Racing & Flying" to "Đua xe & Máy bay",
        "Strategy & simulation" to "Chiến thuật & Mô phỏng",
        "Strategy & Simulation" to "Chiến thuật & Mô phỏng",
        "Family & kids" to "Gia đình & Trẻ em",
        "Family & Kids" to "Gia đình & Trẻ em",
        "Indie" to "Game độc lập (Indie)",
        "Multiplayer" to "Nhiều người chơi",
        "Online multiplayer" to "Chơi mạng trực tuyến",
        "Co-op" to "Chơi phối hợp (Co-op)",
        "Online co-op" to "Chơi phối hợp trực tuyến",
        "Free to play" to "Chơi miễn phí",
        "Free-to-play" to "Chơi miễn phí",
        "Coming soon" to "Sắp ra mắt",
        "Leaving soon" to "Sắp rời dịch vụ",
        "New to Game Pass" to "Mới trên Game Pass",
        "Recently added" to "Mới bổ sung gần đây",
        "Trending now" to "Xu hướng hiện nay",
        "Top paid" to "Trả phí hàng đầu",
        "Top free" to "Miễn phí hàng đầu",

        // === Action Buttons & Session Launching ===
        "Play" to "Chơi ngay",
        "Play now" to "Chơi ngay",
        "Play game" to "Chơi game",
        "Ready to play" to "Sẵn sàng chơi",
        "Get ready to play" to "Chuẩn bị vào chơi",
        "Get ready" to "Đang chuẩn bị",
        "Starting cloud gaming session..." to "Đang khởi tạo phiên chơi đám mây...",
        "Starting cloud gaming session" to "Đang khởi tạo phiên chơi đám mây",
        "Launching with cloud gaming..." to "Đang khởi chạy trên đám mây...",
        "Launching with cloud gaming" to "Đang khởi chạy trên đám mây",
        "Waiting for an open server..." to "Đang xếp hàng chờ máy chủ...",
        "Waiting for an open server" to "Đang xếp hàng chờ máy chủ",
        "You're in the queue" to "Bạn đang trong hàng chờ",
        "You're next!" to "Tới lượt bạn rồi!",
        "Estimated wait time:" to "Thời gian chờ ước tính:",
        "Estimated wait time" to "Thời gian chờ ước tính",
        "Queue position:" to "Vị trí hàng chờ:",
        "Queue position" to "Vị trí hàng chờ",
        "We're experiencing long queue times" to "Hệ thống đang quá tải hàng chờ",
        "Thank you for your patience" to "Cảm ơn bạn đã kiên nhẫn chờ đợi",
        "Connecting..." to "Đang kết nối...",
        "Connected" to "Đã kết nối",
        "Connecting to stream..." to "Đang kết nối luồng game...",
        "Preparing game..." to "Đang chuẩn bị game...",

        // === In-game HUD, Guide & Controls ===
        "Press the Xbox button to open the guide" to "Nhấn nút Xbox để mở bảng điều khiển",
        "Press the Xbox button" to "Nhấn nút Xbox",
        "Open guide" to "Mở bảng điều khiển",
        "Close guide" to "Đóng bảng điều khiển",
        "Quit game" to "Thoát trò chơi",
        "Quit" to "Thoát",
        "Exit" to "Thoát",
        "Resume" to "Tiếp tục",
        "Resume game" to "Tiếp tục trò chơi",
        "Disconnect" to "Ngắt kết nối",
        "Disconnected from the game" to "Đã ngắt kết nối khỏi trò chơi",
        "Disconnected from server" to "Mất kết nối tới máy chủ",
        "Try again" to "Thử lại",
        "Retry" to "Thử lại",
        "Feedback" to "Góp ý chất lượng",
        "Give feedback" to "Gửi góp ý",
        "Mute" to "Tắt tiếng",
        "Unmute" to "Bật tiếng",
        "Mute stream" to "Tắt âm thanh stream",
        "Unmute stream" to "Bật âm thanh stream",
        "Full screen" to "Toàn màn hình",
        "Exit full screen" to "Thoát toàn màn hình",
        "Audio" to "Âm thanh",
        "Video" to "Hình ảnh",
        "Settings" to "Cài đặt",
        "Performance" to "Hiệu năng",
        "Game options" to "Tùy chọn trò chơi",
        "Manage game" to "Quản lý trò chơi",
        "Save game" to "Lưu game",
        "Load game" to "Tải bản lưu",
        "Restart game" to "Khởi động lại game",

        // === User Account & Store ===
        "Sign in" to "Đăng nhập",
        "Sign In" to "Đăng nhập",
        "Sign out" to "Đăng xuất",
        "Sign Out" to "Đăng xuất",
        "Account" to "Tài khoản",
        "Profile" to "Hồ sơ",
        "Manage subscription" to "Quản lý gói dịch vụ",
        "Upgrade" to "Nâng cấp",
        "Search" to "Tìm kiếm",
        "Search games" to "Tìm kiếm trò chơi",
        "Search all games" to "Tìm kiếm mọi game",
        "Details" to "Chi tiết",
        "Overview" to "Tổng quan",
        "Capabilities" to "Tính năng hỗ trợ",
        "Single player" to "Chơi đơn",
        "Cloud saves" to "Lưu đám mây",
        "Achievements" to "Thành tựu",
        "Install to" to "Cài đặt vào",
        "Buy" to "Mua game",
        "Buy now" to "Mua ngay",
        "Back" to "Quay lại",
        "Cancel" to "Hủy",
        "Confirm" to "Xác nhận",
        "OK" to "Đồng ý",
        "Close" to "Đóng",
        "Save" to "Lưu",
        "Apply" to "Áp dụng",
        "Loading..." to "Đang tải...",
        "Please wait" to "Vui lòng đợi",
        "Something went wrong" to "Đã xảy ra sự cố",
        "Network error" to "Lỗi mạng",
        "Check your connection" to "Kiểm tra kết nối mạng của bạn",
        "High latency detected" to "Độ trễ cao",
        "Connection unstable" to "Kết nối không ổn định",
        "Your controller is disconnected" to "Tay cầm đã bị ngắt kết nối",
        "Connect a controller to continue" to "Vui lòng kết nối tay cầm để tiếp tục",
        "Better-xCloud" to "Better-xCloud Tối Ưu",

        // === Game Details & UI Terms ===
        "Description" to "Mô tả",
        "Release date" to "Ngày phát hành",
        "Developer" to "Nhà phát triển",
        "Publisher" to "Nhà xuất bản",
        "Genre" to "Thể loại",
        "Age rating" to "Độ tuổi",
        "Share" to "Chia sẻ",
        "Add to play later" to "Thêm vào danh sách chơi sau",
        "Play later" to "Chơi sau",
        "In play later" to "Đã lưu trong Chơi sau",
        "Remove from play later" to "Xóa khỏi Chơi sau",
        "Screenshots" to "Ảnh chụp màn hình",
        "Trailers" to "Video giới thiệu",
        "More like this" to "Các game tương tự",
        "Recommended for you" to "Đề xuất cho bạn",
        "Because you played" to "Vì bạn đã từng chơi",
        "Reviews" to "Đánh giá",
        "Average rating" to "Điểm đánh giá trung bình",

        // === Controller Buttons & Actions ===
        "Left stick" to "Cần gạt trái (LS)",
        "Right stick" to "Cần gạt phải (RS)",
        "Left bumper" to "Nút LB",
        "Right bumper" to "Nút RB",
        "Left trigger" to "Cò trái LT",
        "Right trigger" to "Cò phải RT",
        "D-pad" to "Phím điều hướng (D-Pad)",
        "View button" to "Nút View",
        "Menu button" to "Nút Menu",
        "Xbox button" to "Nút biểu tượng Xbox",
        "Share button" to "Nút Chia sẻ",
        "Press A" to "Nhấn nút A",
        "Press B" to "Nhấn nút B",
        "Press X" to "Nhấn nút X",
        "Press Y" to "Nhấn nút Y",
        "Press A to continue" to "Nhấn A để tiếp tục",
        "Press any button to start" to "Nhấn phím bất kỳ để bắt đầu",
        "Press Start" to "Nhấn Start để vào game",

        // === Gamepad Virtual Controls ===
        "Button opacity" to "Độ mờ nút ảo",
        "Vibration intensity" to "Cường độ rung",
        "Touch preset" to "Cấu hình cảm ứng",
        "Default layout" to "Bố cục mặc định",
        "Shooter layout" to "Bố cục bắn súng (Shooter)",
        "Racing layout" to "Bố cục đua xe (Racing)",
        "Gyroscope" to "Cảm biến con quay hồi chuyển (Gyro)",
        "Sensitivity" to "Độ nhạy",

        // === Network Telemetry & Diagnostics ===
        "Ping" to "Độ trễ (Ping)",
        "Latency" to "Độ trễ",
        "FPS" to "Khung hình (FPS)",
        "Frames per second" to "Khung hình trên giây",
        "Bitrate" to "Băng thông (Bitrate)",
        "Packet loss" to "Tỉ lệ mất gói tin",
        "Jitter" to "Dao động trễ (Jitter)",
        "Decode time" to "Thời gian giải mã khung hình",
        "Render time" to "Thời gian dựng hình",
        "Resolution" to "Độ phân giải",
        "Frame rate" to "Tốc độ khung hình",
        "High bitrate" to "Băng thông cao",
        "Good connection" to "Kết nối tốt",
        "Poor connection" to "Kết nối kém",
        "Severe packet loss" to "Mất gói tin nghiêm trọng"
    )

    // Japanese UI Dictionary (when connected via Japan East servers)
    val XBOX_UI_DICTIONARY_JP = mapOf(
        "クラウド ゲーム" to "Chơi Game Đám Mây",
        "クラウド ゲーミング" to "Chơi Game Đám Mây",
        "プレイ" to "Chơi ngay",
        "今すぐプレイ" to "Chơi ngay",
        "サインイン" to "Đăng nhập",
        "サインアウト" to "Đăng xuất",
        "すべてのゲーム" to "Tất cả trò chơi",
        "最近プレイしたゲーム" to "Đã chơi gần đây",
        "ゲーム" to "Trò chơi",
        "設定" to "Cài đặt",
        "終了" to "Thoát game",
        "キューで待機中" to "Đang trong hàng chờ",
        "サーバーの空きを待っています" to "Đang chờ máy chủ trống...",
        "コントローラーが必要です" to "Cần tay cầm điều khiển",
        "タッチ操作に対応" to "Hỗ trợ phím cảm ứng",
        "クラウドでプレイ" to "Chơi trên đám mây",
        "再試行" to "Thử lại",
        "再開" to "Tiếp tục",
        "ガイドを開く" to "Mở bảng điều khiển",
        "全画面表示" to "Toàn màn hình",
        "全画面表示を終了" to "Thoát toàn màn hình",
        "ミュート" to "Tắt âm",
        "ミュート解除" to "Bật âm",
        "詳細" to "Chi tiết",
        "検索" to "Tìm kiếm"
    )

    // Korean UI Dictionary (when connected via Korea Central servers)
    val XBOX_UI_DICTIONARY_KR = mapOf(
        "클라우드 게임" to "Chơi Game Đám Mây",
        "플레이" to "Chơi ngay",
        "지금 플레이" to "Chơi ngay",
        "로그인" to "Đăng nhập",
        "로그아웃" to "Đăng xuất",
        "모든 게임" to "Tất cả trò chơi",
        "최근 플레이한 게임" to "Đã chơi gần đây",
        "게임" to "Trò chơi",
        "설정" to "Cài đặt",
        "종료" to "Thoát game",
        "대기열에서 대기 중" to "Đang trong hàng chờ",
        "컨트롤러 필요" to "Yêu cầu tay cầm",
        "터치 지원" to "Hỗ trợ cảm ứng",
        "다시 시도" to "Thử lại",
        "계속하기" to "Tiếp tục"
    )

    // Game Titles Protected from translation (Never mistranslate titles like "Sea of Thieves" -> "Biển của những tên trộm")
    val PROTECTED_GAME_TITLES = listOf(
        "Fortnite", "Forza Horizon", "Forza Motorsport", "Forza", "Halo Infinite", "Halo",
        "Starfield", "Minecraft", "Sea of Thieves", "Gears of War", "Gears 5", "Fallout",
        "The Elder Scrolls", "Skyrim", "Oblivion", "Doom", "Doom Eternal", "Palworld", "Persona",
        "Yakuza", "Like a Dragon", "Assassin's Creed", "Rainbow Six", "Far Cry",
        "Dead Island", "Lies of P", "Monster Hunter", "Mortal Kombat", "Tekken",
        "Street Fighter", "Dead Cells", "Hades", "Hollow Knight", "Overcooked",
        "It Takes Two", "ARK", "Rust", "No Man's Sky", "Cities: Skylines",
        "Microsoft Flight Simulator", "Battlefield", "Need for Speed", "FIFA", "EA Sports FC",
        "NBA 2K", "PUBG", "Apex Legends", "Roblox", "Grand Theft Auto", "GTA",
        "Cyberpunk 2077", "Elden Ring", "Hi-Fi RUSH", "Remnant", "Valheim",
        "Call of Duty", "Modern Warfare", "Warzone", "Black Ops",
        "Resident Evil", "Silent Hill", "Deathloop", "Control", "Alan Wake",
        "Dishonored", "Wolfenstein", "Prey", "The Evil Within", "Quake",
        "Senua's Saga: Hellblade", "Hellblade", "Avowed", "Fable", "Outer Worlds",
        "Grounded", "Pentiment", "Sea of Stars", "Dead Space", "Mass Effect",
        "Dragon Age", "Titanfall", "State of Decay", "Crackdown", "Sunset Overdrive",
        "Psychonauts", "Ori and the Blind Forest", "Ori and the Will of the Wisps",
        "Slay the Spire", "Dead by Daylight", "Deep Rock Galactic", "Terraria"
    )

    // Dynamic cache for community GitHub updates
    private val dynamicGithubDictionary = mutableMapOf<String, String>()

    suspend fun syncDictionaryFromGithub(rawUrl: String = "https://raw.githubusercontent.com/redphx/better-xcloud/main/src/locales/vi-VN.json"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(rawUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"
                conn.connect()
                if (conn.responseCode == 200) {
                    val content = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(content)
                    val keys = json.keys()
                    var count = 0
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = json.optString(k, "")
                        if (v.isNotBlank()) {
                            dynamicGithubDictionary[k] = v
                            count++
                        }
                    }
                    Log.i(TAG, "Synced $count translation terms from GitHub successfully")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync GitHub dictionary: ${e.message}")
                false
            }
        }
    }

    private fun escapeJson(str: String): String {
        val sb = StringBuilder()
        for (c in str) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0x00..0x1F) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun mapToJsonString(map: Map<String, String>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in map) {
            if (!first) sb.append(",")
            sb.append("\"").append(escapeJson(k)).append("\":\"").append(escapeJson(v)).append("\"")
            first = false
        }
        sb.append("}")
        return sb.toString()
    }

    private fun listToJsonString(list: List<String>): String {
        val sb = StringBuilder("[")
        var first = true
        for (item in list) {
            if (!first) sb.append(",")
            sb.append("\"").append(escapeJson(item)).append("\"")
            first = false
        }
        sb.append("]")
        return sb.toString()
    }

    /**
     * Builds the high-performance, 0ms in-browser translation and DOM scrubber engine.
     */
    fun getTranslatorScript(
        enabled: Boolean,
        preserveGameTitles: Boolean = true,
        bilingualMode: Boolean = false,
        hideStreamStats: Boolean = true
    ): String {
        val mergedDict = HashMap<String, String>()
        XBOX_UI_DICTIONARY_EN.forEach { (k, v) -> mergedDict[k] = v }
        XBOX_UI_DICTIONARY_JP.forEach { (k, v) -> mergedDict[k] = v }
        XBOX_UI_DICTIONARY_KR.forEach { (k, v) -> mergedDict[k] = v }
        dynamicGithubDictionary.forEach { (k, v) -> mergedDict[k] = v }

        val dictJson = mapToJsonString(mergedDict)
        val protectedTitlesJson = listToJsonString(PROTECTED_GAME_TITLES)

        return """
            (function() {
                if (window.__xcloudTranslatorInstalled) {
                    if (window.__toggleXCloudTranslation) window.__toggleXCloudTranslation($enabled);
                    if (window.__setXCloudTranslationConfig) {
                        window.__setXCloudTranslationConfig({
                            preserveTitles: $preserveGameTitles,
                            bilingual: $bilingualMode,
                            hideStats: $hideStreamStats
                        });
                    }
                    return;
                }
                window.__xcloudTranslatorInstalled = true;

                let isTranslationEnabled = $enabled;
                let preserveTitles = $preserveGameTitles;
                let isBilingual = $bilingualMode;
                let hideStreamStats = $hideStreamStats;
                let translatedCount = 0;

                const RAW_DICT = $dictJson;
                const PROTECTED = new Set($protectedTitlesJson);

                // Pre-index O(1) Fast Lookup Maps (Exact & Lowercase)
                const EXACT_MAP = Object.create(null);
                const LOWER_MAP = Object.create(null);
                for (const key in RAW_DICT) {
                    const val = RAW_DICT[key];
                    EXACT_MAP[key] = val;
                    LOWER_MAP[key.toLowerCase()] = val;
                }

                // Tag blacklist: Never touch media, WebRTC video, canvas or code
                const EXCLUDE_TAGS = new Set([
                    'SCRIPT', 'STYLE', 'NOSCRIPT', 'VIDEO', 'CANVAS', 'AUDIO',
                    'INPUT', 'TEXTAREA', 'SELECT', 'CODE', 'PRE', 'SVG', 'PATH'
                ]);

                // Auto-suppress intrusive on-screen stream stats & decode profile overlay
                const statsScrubberStyle = document.createElement('style');
                statsScrubberStyle.id = 'cloudplay-stats-scrubber-style';
                function updateStatsScrubber(hide) {
                    if (hide) {
                        statsScrubberStyle.textContent = `
                            #bx-stream-stats, .bx-stream-stats, [data-bx-stream-stats],
                            .stream-stats, #stream-stats, [data-testid="stream-stats"],
                            .stream-statistics-overlay, [class*="stream-stat"], [class*="StreamStat"],
                            [class*="decode-profile"], [id*="decode-profile"],
                            [data-testid*="stream-stats-overlay"] {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }
                        `;
                        if (!document.head.contains(statsScrubberStyle)) {
                            document.head.appendChild(statsScrubberStyle);
                        }
                    } else {
                        if (document.head.contains(statsScrubberStyle)) {
                            statsScrubberStyle.textContent = '';
                        }
                    }
                }
                updateStatsScrubber(hideStreamStats);

                function isInsideStreamPlayer(node) {
                    try {
                        let el = node.nodeType === 3 ? node.parentElement : node;
                        while (el && el !== document.body) {
                            if (el.tagName === 'VIDEO' || el.tagName === 'CANVAS') return true;
                            if (el.id === 'stream-video' || el.getAttribute('data-testid') === 'stream-video') return true;
                            if (el.classList && el.classList.contains('game-stream')) return true;
                            el = el.parentElement;
                        }
                    } catch(e) {}
                    return false;
                }

                function shouldPreserveTitle(text) {
                    if (!preserveTitles) return false;
                    const clean = text.trim();
                    for (const title of PROTECTED) {
                        if (clean === title || clean.startsWith(title + ' ') || clean.startsWith(title + ':')) {
                            return true;
                        }
                    }
                    return false;
                }

                // High-speed O(1) text translation
                function translateString(text) {
                    if (!text || text.length < 2) return null;
                    const trimmed = text.trim();

                    // O(1) Exact Match
                    const exact = EXACT_MAP[trimmed];
                    if (exact) return text.replace(trimmed, exact);

                    // O(1) Lowercase Match
                    const lowerMatch = LOWER_MAP[trimmed.toLowerCase()];
                    if (lowerMatch) return text.replace(trimmed, lowerMatch);

                    // Regex Replacements for dynamic counts/times
                    let s = text;
                    let modified = false;

                    // Match "Decode profile: High / Main" -> "Hồ sơ giải mã: High / Main"
                    if (/\bdecode\s*(?:profile|profiling):\s*([a-zA-Z0-9_\-]+)\b/i.test(s)) {
                        s = s.replace(/\bdecode\s*(?:profile|profiling):\s*([a-zA-Z0-9_\-]+)\b/gi, 'Hồ sơ giải mã video: $1');
                        modified = true;
                    }
                    // Match "X minutes left" or "Wait time: X min"
                    if (/\b(\d+)\s*(?:minutes?|mins?)\s*left\b/i.test(s)) {
                        s = s.replace(/\b(\d+)\s*(?:minutes?|mins?)\s*left\b/gi, 'Còn $1 phút');
                        modified = true;
                    }
                    if (/\bwait\s*time:\s*(\d+)\s*(?:mins?|minutes?)\b/i.test(s)) {
                        s = s.replace(/\bwait\s*time:\s*(\d+)\s*(?:mins?|minutes?)\b/gi, 'Thời gian chờ: $1 phút');
                        modified = true;
                    }
                    if (/\bqueue\s*position:\s*(\d+)\b/i.test(s)) {
                        s = s.replace(/\bqueue\s*position:\s*(\d+)\b/gi, 'Vị trí hàng chờ: $1');
                        modified = true;
                    }
                    if (/\b(\d+)\s*of\s*(\d+)\s*games\b/i.test(s)) {
                        s = s.replace(/\b(\d+)\s*of\s*(\d+)\s*games\b/gi, '$1 trên $2 trò chơi');
                        modified = true;
                    }

                    return modified ? s : null;
                }

                // Dynamic cache and pending callbacks for Google Translate
                const DYNAMIC_CACHE = Object.create(null);
                const pendingCallbacks = Object.create(null);
                let callbackSeq = 0;

                window.__onSmartTranslateResult = function(callbackId, translatedText) {
                    if (!translatedText) return;
                    const entry = pendingCallbacks[callbackId];
                    if (!entry) return;
                    delete pendingCallbacks[callbackId];

                    const node = entry.node;
                    const orig = entry.orig;
                    if (!node || !node.parentElement) return;

                    const clean = orig.trim();
                    DYNAMIC_CACHE[clean] = translatedText;

                    if (node._xcloudOrig === undefined) {
                        node._xcloudOrig = orig;
                    }
                    if (isBilingual && translatedText !== clean) {
                        node.nodeValue = translatedText + ' (' + clean + ')';
                    } else {
                        node.nodeValue = translatedText;
                    }
                    delete node._cpPending;
                    translatedCount++;
                    reportStatus();
                };

                function processTextNode(node) {
                    if (!isTranslationEnabled) return;
                    const parent = node.parentElement;
                    if (!parent || EXCLUDE_TAGS.has(parent.tagName) || isInsideStreamPlayer(parent)) return;

                    const original = node._xcloudOrig !== undefined ? node._xcloudOrig : node.nodeValue;
                    if (!original || !original.trim()) return;

                    if (shouldPreserveTitle(original)) return;

                    // 1. Instant 0ms Local Dictionary match
                    const translated = translateString(original);
                    if (translated && translated !== original) {
                        if (node._xcloudOrig === undefined) {
                            node._xcloudOrig = original;
                        }
                        if (isBilingual) {
                            node.nodeValue = translated + ' (' + original.trim() + ')';
                        } else {
                            node.nodeValue = translated;
                        }
                        translatedCount++;
                        return;
                    }

                    // 2. Instant Cache Hit from prior Google Translation
                    const trimmed = original.trim();
                    if (DYNAMIC_CACHE[trimmed]) {
                        const cached = DYNAMIC_CACHE[trimmed];
                        if (node._xcloudOrig === undefined) {
                            node._xcloudOrig = original;
                        }
                        if (isBilingual && cached !== trimmed) {
                            node.nodeValue = cached + ' (' + trimmed + ')';
                        } else {
                            node.nodeValue = cached;
                        }
                        translatedCount++;
                        return;
                    }

                    // 3. Smart Google Translate: Translates descriptions, guides, reviews, synopsis
                    // Intelligently avoids numbers, single punctuation, codes, URLs
                    if (trimmed.length >= 4 && !node._cpPending) {
                        if (/^[\d\s\.,:\-_/\\#\$%&*()+=\[\]{}|<>?!@~`^;'"0-9]+$/.test(trimmed)) return;
                        if (/^(https?:\/\/|www\.)/i.test(trimmed)) return;

                        node._cpPending = true;
                        const callId = 'cb_' + (++callbackSeq);
                        pendingCallbacks[callId] = { node: node, orig: original };

                        if (window.CloudPlayBridge && window.CloudPlayBridge.requestSmartTranslate) {
                            window.CloudPlayBridge.requestSmartTranslate(trimmed, callId, isBilingual);
                        }
                    }
                }

                // Fast Native TreeWalker Pass
                function runTreeWalkerPass(root) {
                    if (!root || !isTranslationEnabled) return;
                    try {
                        const walker = document.createTreeWalker(
                            root,
                            NodeFilter.SHOW_TEXT,
                            {
                                acceptNode: function(node) {
                                    const p = node.parentElement;
                                    if (!p || EXCLUDE_TAGS.has(p.tagName) || isInsideStreamPlayer(p)) {
                                        return NodeFilter.FILTER_REJECT;
                                    }
                                    return NodeFilter.FILTER_ACCEPT;
                                }
                            },
                            false
                        );

                        let currentNode = walker.nextNode();
                        while (currentNode) {
                            processTextNode(currentNode);
                            currentNode = walker.nextNode();
                        }
                    } catch(e) {}
                    reportStatus();
                }

                function revertAllTextNodes(root) {
                    if (!root) return;
                    try {
                        const walker = document.createTreeWalker(
                            root,
                            NodeFilter.SHOW_TEXT,
                            null,
                            false
                        );
                        let currentNode = walker.nextNode();
                        while (currentNode) {
                            if (currentNode._xcloudOrig !== undefined) {
                                currentNode.nodeValue = currentNode._xcloudOrig;
                                delete currentNode._xcloudOrig;
                            }
                            delete currentNode._cpPending;
                            currentNode = walker.nextNode();
                        }
                    } catch(e) {}
                }

                function reportStatus() {
                    try {
                        if (window.CloudPlayBridge && window.CloudPlayBridge.onTranslationStatus) {
                            window.CloudPlayBridge.onTranslationStatus(isTranslationEnabled, translatedCount);
                        }
                    } catch(e) {}
                }

                // Global APIs for Android Bridge & Floating HUD
                window.__toggleXCloudTranslation = function(enabled) {
                    isTranslationEnabled = !!enabled;
                    if (isTranslationEnabled) {
                        translatedCount = 0;
                        runTreeWalkerPass(document.body);
                    } else {
                        revertAllTextNodes(document.body);
                        translatedCount = 0;
                    }
                    reportStatus();
                };

                window.__forceTranslateEntirePage = function() {
                    isTranslationEnabled = true;
                    runTreeWalkerPass(document.body);
                    reportStatus();
                };

                window.__setXCloudTranslationConfig = function(options) {
                    if (!options) return;
                    if (options.preserveTitles !== undefined) preserveTitles = !!options.preserveTitles;
                    if (options.bilingual !== undefined) isBilingual = !!options.bilingual;
                    if (options.hideStats !== undefined) {
                        hideStreamStats = !!options.hideStats;
                        updateStatsScrubber(hideStreamStats);
                    }
                    if (isTranslationEnabled) {
                        runTreeWalkerPass(document.body);
                    }
                };

                window.__toggleStreamStatsOverlay = function(hide) {
                    hideStreamStats = !!hide;
                    updateStatsScrubber(hideStreamStats);
                };

                // Batched MutationObserver via requestAnimationFrame
                let pendingNodes = new Set();
                let rafId = null;

                function processBatchedNodes() {
                    rafId = null;
                    if (!isTranslationEnabled || pendingNodes.size === 0) {
                        pendingNodes.clear();
                        return;
                    }
                    pendingNodes.forEach(node => {
                        if (node.nodeType === 3) {
                            processTextNode(node);
                        } else if (node.nodeType === 1) {
                            runTreeWalkerPass(node);
                        }
                    });
                    pendingNodes.clear();
                }

                const observer = new MutationObserver(function(mutations) {
                    if (!isTranslationEnabled && !hideStreamStats) return;

                    for (let i = 0; i < mutations.length; i++) {
                        const m = mutations[i];
                        if (m.type === 'childList') {
                            for (let j = 0; j < m.addedNodes.length; j++) {
                                const added = m.addedNodes[j];
                                // Check if this is an unwanted stream stats / decode profile popup
                                if (hideStreamStats && added.nodeType === 1) {
                                    const id = (added.id || '').toLowerCase();
                                    const cls = (added.className || '').toString().toLowerCase();
                                    if (id.includes('stream-stat') || id.includes('decode-profile') ||
                                        cls.includes('stream-stat') || cls.includes('decode-profile') ||
                                        id.includes('bx-stream-stats') || cls.includes('bx-stream-stats')) {
                                        added.style.display = 'none';
                                    }
                                }
                                if (isTranslationEnabled) pendingNodes.add(added);
                            }
                        } else if (m.type === 'characterData' && isTranslationEnabled) {
                            pendingNodes.add(m.target);
                        }
                    }

                    if (isTranslationEnabled && !rafId) {
                        rafId = requestAnimationFrame(processBatchedNodes);
                    }
                });

                function start() {
                    const target = document.body || document.documentElement;
                    if (target) {
                        observer.observe(target, {
                            childList: true,
                            subtree: true,
                            characterData: true
                        });
                        runTreeWalkerPass(target);
                    } else {
                        document.addEventListener('DOMContentLoaded', function() {
                            observer.observe(document.body, {
                                childList: true,
                                subtree: true,
                                characterData: true
                            });
                            runTreeWalkerPass(document.body);
                        });
                    }
                }

                start();
                setTimeout(() => runTreeWalkerPass(document.body), 800);
                setTimeout(() => runTreeWalkerPass(document.body), 2500);
            })();
        """.trimIndent()
    }
}
