package com.example.data

import java.util.Locale

/**
 * Hệ thống từ điển đa ngôn ngữ cho giao diện ứng dụng (12 ngôn ngữ quốc tế)
 * và từ điển dịch nhanh O(1) cho Xbox Cloud Gaming trên mọi ngôn ngữ đích.
 *
 * Lưu ý thiết kế theo yêu cầu Chủ dự án:
 * - Khi chọn ngôn ngữ ứng dụng (ví dụ: Tiếng Trung `zh`, Tiếng Nhật `ja`, Tiếng Hàn `ko`, ...),
 *   toàn bộ giao diện ứng dụng (Trang chủ, Thư viện, Lịch sử, Máy chủ, Thanh điều hướng, HUD trong game)
 *   chuyển sang ngôn ngữ đó, TRỪ màn hình Cài đặt (`SettingsScreen`) giữ Tiếng Việt/Anh để người dùng
 *   luôn đọc và chỉnh lại cài đặt dễ dàng.
 */
object AppUiTranslator {

    fun normalizeLangCode(rawCode: String): String {
        val lower = rawCode.trim().lowercase(Locale.ROOT)
        return when {
            lower.startsWith("zh") -> "zh"
            lower.startsWith("ja") -> "ja"
            lower.startsWith("ko") -> "ko"
            lower.startsWith("es") -> "es"
            lower.startsWith("pt") -> "pt"
            lower.startsWith("fr") -> "fr"
            lower.startsWith("de") -> "de"
            lower.startsWith("ru") -> "ru"
            lower.startsWith("th") -> "th"
            lower.startsWith("id") || lower.startsWith("in") -> "id"
            lower.startsWith("vi") -> "vi"
            else -> "en"
        }
    }

    // Từ điển câu tĩnh giao diện App theo mã ngôn ngữ (key = chuỗi tiếng Anh chuẩn `en`)
    private val UI_PHRASES_BY_LANG: Map<String, Map<String, String>> = mapOf(
        "zh" to mapOf(
            "Auto Translate" to "自动翻译",
            "More options" to "更多选项",
            "Share & QR Code" to "分享与二维码",
            "Cleared saved login & web data!" to "已清除保存的登录和网页数据！",
            "🎮 Controller Ready" to "🎮 手柄已就绪",
            "👆 Touch Controls" to "👆 触控操作",
            "Direct Net" to "直连网络",
            "Auto VPN" to "智能 VPN",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ 4G/5G 与 Wi-Fi 低延迟优化",
            "Full HD • Smooth 60FPS" to "全高清 1080p • 流畅 60FPS",
            "Free • Instant Play" to "免费畅玩 • 秒开即玩",
            "WebRTC RTT • Tap to switch" to "WebRTC 实时延迟 • 点击切换",
            "Last session ping: --" to "上次游玩延迟: --",
            "QUICK RESUME" to "快速恢复 (Quick Resume)",
            "• 60FPS STREAM" to "• 60FPS 云串流",
            "Loading games..." to "正在加载游戏...",
            "No games yet" to "暂无游戏",
            "Favorite" to "收藏",
            "History" to "游玩记录",
            "Refresh Catalog" to "刷新游戏目录",
            "Refresh" to "刷新",
            "All Genres" to "全部类型",
            "Shooter / FPS" to "射击 / FPS",
            "Action & Adv" to "动作与冒险",
            "Racing & Sim" to "赛车与模拟",
            "RPG & Survival" to "角色扮演 / 生存",
            "Sports & Fight" to "体育与格斗",
            "Syncing..." to "同步中...",
            "Loading games from Xbox Cloud..." to "正在从 Xbox 云游戏加载目录...",
            "No matching games found" to "未找到匹配的游戏",
            "Searching Xbox..." to "正在搜索 Xbox...",
            " • Untested unlock" to " • 未测试解锁",
            "Select" to "选择",
            "This server cannot unlock the region, switch to another server?" to "当前节点无法解锁该地区，是否切换到其他节点？",
            "Switch server" to "切换节点",
            "Dismiss" to "关闭",
            "Unlocked" to "已解锁",
            "Unlock Failed" to "解锁失败",
            "Ready" to "就绪",
            "Wrong Region" to "地区不符",
            "Testing network latency..." to "正在测量网络延迟...",
            "Network latency: Unreachable" to "网络延迟: 无法连接",
            "Network latency: Not tested" to "网络延迟: 未测试",
            "Auto Best" to "自动择优",
            "Pinging..." to "测速中...",
            "Ping Test" to "延迟测速",
            "Updating..." to "更新中...",
            "Update" to "更新节点",
            "Return to en-US" to "返回 en-US 标准服",
            "Return to Game" to "返回游戏",
            "Free Cloud" to "免费云游",
            "Game Pass" to "Game Pass",
            "Shooter" to "射击游戏",
            "Action & Adventure" to "动作冒险",
            "Racing" to "竞速赛车",
            "RPG" to "角色扮演",
            "Sports" to "体育竞技"
        ),
        "ja" to mapOf(
            "Auto Translate" to "自動翻訳",
            "More options" to "その他のオプション",
            "Share & QR Code" to "共有 & QRコード",
            "Cleared saved login & web data!" to "ログイン情報とWebキャッシュを消去しました！",
            "🎮 Controller Ready" to "🎮 コントローラー準備完了",
            "👆 Touch Controls" to "👆 タッチ操作",
            "Direct Net" to "ダイレクト通信",
            "Auto VPN" to "自動 VPN",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ 4G/5G & Wi-Fi 低遅延モード",
            "Full HD • Smooth 60FPS" to "Full HD • なめらか 60FPS",
            "Free • Instant Play" to "無料 • すぐにプレイ",
            "WebRTC RTT • Tap to switch" to "WebRTC 遅延 • タップで切替",
            "Last session ping: --" to "前回のPing: --",
            "QUICK RESUME" to "クイック再開",
            "• 60FPS STREAM" to "• 60FPS ストリーム",
            "Loading games..." to "ゲームを読み込み中...",
            "No games yet" to "ゲームがありません",
            "Favorite" to "お気に入り",
            "History" to "プレイ履歴",
            "Refresh Catalog" to "カタログを更新",
            "Refresh" to "更新",
            "All Genres" to "全ジャンル",
            "Shooter / FPS" to "シューティング / FPS",
            "Action & Adv" to "アクション & 冒険",
            "Racing & Sim" to "レース & シミュレーション",
            "RPG & Survival" to "RPG & サバイバル",
            "Sports & Fight" to "スポーツ & 格闘",
            "Syncing..." to "同期中...",
            "Loading games from Xbox Cloud..." to "Xbox Cloud からゲームを取得中...",
            "No matching games found" to "一致するゲームが見つかりません",
            "Searching Xbox..." to "Xbox Store を検索中...",
            " • Untested unlock" to " • 未確認サーバー",
            "Select" to "選択",
            "This server cannot unlock the region, switch to another server?" to "このサーバーでは地域ロックを解除できません。別のサーバーに切り替えますか？",
            "Switch server" to "サーバー切替",
            "Dismiss" to "閉じる",
            "Unlocked" to "解除済み",
            "Unlock Failed" to "解除失敗",
            "Ready" to "準備完了",
            "Wrong Region" to "非対応地域",
            "Testing network latency..." to "ネットワーク遅延を測定中...",
            "Network latency: Unreachable" to "ネットワーク遅延: 接続不可",
            "Network latency: Not tested" to "ネットワーク遅延: 未測定",
            "Auto Best" to "最適を選択",
            "Pinging..." to "Ping測定中...",
            "Ping Test" to "Ping測定",
            "Updating..." to "更新中...",
            "Update" to "サーバー更新",
            "Return to en-US" to "en-US に戻す",
            "Return to Game" to "ゲームに戻る"
        ),
        "ko" to mapOf(
            "Auto Translate" to "자동 번역",
            "More options" to "더 보기",
            "Share & QR Code" to "공유 및 QR 코드",
            "Cleared saved login & web data!" to "저장된 로그인 및 웹 데이터를 초기화했습니다!",
            "🎮 Controller Ready" to "🎮 컨트롤러 준비됨",
            "👆 Touch Controls" to "👆 터치 컨트롤",
            "Direct Net" to "직접 연결",
            "Auto VPN" to "스마트 VPN",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ 4G/5G 및 Wi-Fi 저지연 최적화",
            "Full HD • Smooth 60FPS" to "Full HD • 부드러운 60FPS",
            "Free • Instant Play" to "무료 • 즉시 플레이",
            "WebRTC RTT • Tap to switch" to "WebRTC 지연 • 탭하여 서버 변경",
            "Last session ping: --" to "최근 플레이 핑: --",
            "QUICK RESUME" to "빠른 이어하기",
            "• 60FPS STREAM" to "• 60FPS 스트리밍",
            "Loading games..." to "게임 불러오는 중...",
            "No games yet" to "등록된 게임 없음",
            "Favorite" to "즐겨찾기",
            "History" to "플레이 기록",
            "Refresh Catalog" to "목록 새로고침",
            "Refresh" to "새로고침",
            "All Genres" to "모든 장르",
            "Shooter / FPS" to "슈팅 / FPS",
            "Action & Adv" to "액션 & 어드벤처",
            "Racing & Sim" to "레이싱 & 시뮬레이션",
            "RPG & Survival" to "RPG & 생존",
            "Sports & Fight" to "스포츠 & 격투",
            "Syncing..." to "동기화 중...",
            "Loading games from Xbox Cloud..." to "Xbox Cloud에서 게임을 불러오는 중...",
            "No matching games found" to "일치하는 게임을 찾을 수 없습니다",
            "Searching Xbox..." to "Xbox 스토어 검색 중...",
            " • Untested unlock" to " • 미확인 서버",
            "Select" to "선택",
            "This server cannot unlock the region, switch to another server?" to "이 서버는 지역 잠금을 해제할 수 없습니다. 다른 서버로 전환할까요?",
            "Switch server" to "서버 전환",
            "Dismiss" to "닫기",
            "Unlocked" to "잠금 해제됨",
            "Unlock Failed" to "해제 실패",
            "Ready" to "준비됨",
            "Wrong Region" to "지원되지 않는 지역",
            "Testing network latency..." to "네트워크 지연 시간 측정 중...",
            "Network latency: Unreachable" to "네트워크 지연: 연결 불가",
            "Network latency: Not tested" to "네트워크 지연: 미측정",
            "Auto Best" to "최적 서버 선택",
            "Pinging..." to "핑 측정 중...",
            "Ping Test" to "핑 측정",
            "Updating..." to "업데이트 중...",
            "Update" to "업데이트",
            "Return to en-US" to "en-US로 돌아가기",
            "Return to Game" to "게임으로 돌아가기"
        ),
        "es" to mapOf(
            "Auto Translate" to "Traducción automática",
            "More options" to "Más opciones",
            "Share & QR Code" to "Compartir y código QR",
            "🎮 Controller Ready" to "🎮 Mando listo",
            "👆 Touch Controls" to "👆 Controles táctiles",
            "Direct Net" to "Red directa",
            "Auto VPN" to "VPN inteligente",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Baja latencia 4G/5G y Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS fluidos",
            "Free • Instant Play" to "Gratis • Juego instantáneo",
            "Last session ping: --" to "Ping última sesión: --",
            "QUICK RESUME" to "CONTINUAR RÁPIDO",
            "History" to "Historial",
            "Refresh" to "Actualizar",
            "All Genres" to "Todos los géneros",
            "Shooter / FPS" to "Disparos / FPS",
            "Action & Adv" to "Acción y Aventura",
            "Racing & Sim" to "Carreras y Sim",
            "RPG & Survival" to "RPG y Supervivencia",
            "Sports & Fight" to "Deportes y Lucha",
            "Select" to "Elegir",
            "Switch server" to "Cambiar servidor",
            "Dismiss" to "Cerrar",
            "Unlocked" to "Desbloqueado",
            "Ready" to "Listo",
            "Auto Best" to "Mejor servidor",
            "Ping Test" to "Medir Ping",
            "Update" to "Actualizar",
            "Return to Game" to "Volver al juego"
        ),
        "pt" to mapOf(
            "Auto Translate" to "Tradução automática",
            "More options" to "Mais opções",
            "Share & QR Code" to "Compartilhar e QR Code",
            "🎮 Controller Ready" to "🎮 Controle pronto",
            "👆 Touch Controls" to "👆 Controles de toque",
            "Direct Net" to "Rede direta",
            "Auto VPN" to "VPN inteligente",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Baixa latência 4G/5G e Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS suaves",
            "Free • Instant Play" to "Grátis • Jogar agora",
            "Last session ping: --" to "Ping da última sessão: --",
            "QUICK RESUME" to "RETOMADA RÁPIDA",
            "History" to "Histórico",
            "Refresh" to "Atualizar",
            "All Genres" to "Todos os gêneros",
            "Select" to "Selecionar",
            "Switch server" to "Trocar servidor",
            "Unlocked" to "Desbloqueado",
            "Ready" to "Pronto",
            "Auto Best" to "Melhor servidor",
            "Ping Test" to "Testar Ping",
            "Update" to "Atualizar",
            "Return to Game" to "Voltar ao jogo"
        ),
        "fr" to mapOf(
            "Auto Translate" to "Traduction auto",
            "More options" to "Plus d'options",
            "Share & QR Code" to "Partager & QR Code",
            "🎮 Controller Ready" to "🎮 Manette prête",
            "👆 Touch Controls" to "👆 Commandes tactiles",
            "Direct Net" to "Réseau direct",
            "Auto VPN" to "VPN auto",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Faible latence 4G/5G & Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS fluides",
            "Free • Instant Play" to "Gratuit • Jeu instantané",
            "Last session ping: --" to "Ping dernière session : --",
            "QUICK RESUME" to "REPRISE RAPIDE",
            "History" to "Historique",
            "Refresh" to "Actualiser",
            "All Genres" to "Tous les genres",
            "Select" to "Choisir",
            "Switch server" to "Changer de serveur",
            "Unlocked" to "Débloqué",
            "Ready" to "Prêt",
            "Auto Best" to "Meilleur serveur",
            "Ping Test" to "Tester Ping",
            "Update" to "Actualiser",
            "Return to Game" to "Retour au jeu"
        ),
        "de" to mapOf(
            "Auto Translate" to "Auto-Übersetzung",
            "More options" to "Weitere Optionen",
            "Share & QR Code" to "Teilen & QR-Code",
            "🎮 Controller Ready" to "🎮 Controller bereit",
            "👆 Touch Controls" to "👆 Touch-Steuerung",
            "Direct Net" to "Direktnetz",
            "Auto VPN" to "Smart-VPN",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Niedrige Latenz 4G/5G & WLAN",
            "Full HD • Smooth 60FPS" to "Full HD • Flüssige 60FPS",
            "Free • Instant Play" to "Kostenlos • Sofort spielen",
            "Last session ping: --" to "Ping letzte Sitzung: --",
            "QUICK RESUME" to "SCHNELL FORTSETZEN",
            "History" to "Verlauf",
            "Refresh" to "Aktualisieren",
            "All Genres" to "Alle Genres",
            "Select" to "Wählen",
            "Switch server" to "Server wechseln",
            "Unlocked" to "Freigeschaltet",
            "Ready" to "Bereit",
            "Auto Best" to "Bester Server",
            "Ping Test" to "Ping testen",
            "Update" to "Aktualisieren",
            "Return to Game" to "Zurück zum Spiel"
        ),
        "ru" to mapOf(
            "Auto Translate" to "Автоперевод",
            "More options" to "Ещё",
            "Share & QR Code" to "Поделиться и QR-код",
            "🎮 Controller Ready" to "🎮 Геймпад готов",
            "👆 Touch Controls" to "👆 Сенсорное управление",
            "Direct Net" to "Прямая сеть",
            "Auto VPN" to "Умный VPN",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Низкий пинг 4G/5G и Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • Плавные 60 FPS",
            "Free • Instant Play" to "Бесплатно • Быстрый старт",
            "Last session ping: --" to "Пинг прошлой сессии: --",
            "QUICK RESUME" to "БЫСТРЫЙ ЗАПУСК",
            "History" to "История",
            "Refresh" to "Обновить",
            "All Genres" to "Все жанры",
            "Select" to "Выбрать",
            "Switch server" to "Сменить сервер",
            "Unlocked" to "Разблокировано",
            "Ready" to "Готов",
            "Auto Best" to "Лучший сервер",
            "Ping Test" to "Тест пинга",
            "Update" to "Обновить",
            "Return to Game" to "Вернуться в игру"
        ),
        "th" to mapOf(
            "Auto Translate" to "แปลภาษาอัตโนมัติ",
            "More options" to "ตัวเลือกเพิ่มเติม",
            "Share & QR Code" to "แชร์ & คิวอาร์โค้ด",
            "🎮 Controller Ready" to "🎮 จอยพร้อมใช้งาน",
            "👆 Touch Controls" to "👆 ปุ่มสัมผัส",
            "Direct Net" to "เน็ตตรง",
            "Auto VPN" to "VPN อัจฉริยะ",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ ลดแล็ก 4G/5G และ Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • ลื่นไหล 60FPS",
            "Free • Instant Play" to "เล่นฟรี • เข้าเกมทันที",
            "Last session ping: --" to "ปิงครั้งล่าสุด: --",
            "QUICK RESUME" to "เล่นต่อทันที",
            "History" to "ประวัติการเล่น",
            "Refresh" to "รีเฟรช",
            "All Genres" to "ทุกแนวเกม",
            "Select" to "เลือก",
            "Switch server" to "เปลี่ยนเซิร์ฟเวอร์",
            "Unlocked" to "ปลดล็อกแล้ว",
            "Ready" to "พร้อมใช้",
            "Auto Best" to "เลือกเซิร์ฟดีสุด",
            "Ping Test" to "วัดค่าปิง",
            "Update" to "อัปเดต",
            "Return to Game" to "กลับเข้าสู่เกม"
        ),
        "id" to mapOf(
            "Auto Translate" to "Terjemahan Otomatis",
            "More options" to "Opsi lainnya",
            "Share & QR Code" to "Bagikan & Kode QR",
            "🎮 Controller Ready" to "🎮 Stik Siap",
            "👆 Touch Controls" to "👆 Kontrol Sentuh",
            "Direct Net" to "Jaringan Langsung",
            "Auto VPN" to "VPN Pintar",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Latensi Rendah 4G/5G & Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS Mulus",
            "Free • Instant Play" to "Gratis • Main Instan",
            "Last session ping: --" to "Ping sesi terakhir: --",
            "QUICK RESUME" to "LANJUT CEPAT",
            "History" to "Riwayat",
            "Refresh" to "Segarkan",
            "All Genres" to "Semua Genre",
            "Select" to "Pilih",
            "Switch server" to "Ganti server",
            "Unlocked" to "Terbuka",
            "Ready" to "Siap",
            "Auto Best" to "Server Terbaik",
            "Ping Test" to "Cek Ping",
            "Update" to "Perbarui",
            "Return to Game" to "Kembali ke Game"
        )
    )

    /**
     * Dịch chuỗi giao diện ứng dụng (ngoài màn hình Cài đặt) sang ngôn ngữ đang chọn.
     */
    fun translateUi(vi: String, en: String, rawLangCode: String): String {
        val lang = normalizeLangCode(rawLangCode)
        if (lang == "vi") return vi
        if (lang == "en") return en

        val table = UI_PHRASES_BY_LANG[lang]
        if (table != null) {
            table[en]?.let { return it }
        }

        // Xử lý các mẫu câu động có số liệu / tham số (Ping, tên máy chủ, số lượng game...)
        if (en.startsWith("Unlocked ")) {
            val flag = en.removePrefix("Unlocked ")
            return when (lang) {
                "zh" -> "已解锁 $flag"
                "ja" -> "地域解除 $flag"
                "ko" -> "지역 해제 $flag"
                "es" -> "Desbloqueado $flag"
                "pt" -> "Desbloqueado $flag"
                "fr" -> "Débloqué $flag"
                "de" -> "Freigeschaltet $flag"
                "ru" -> "Разблокировано $flag"
                "th" -> "ปลดล็อก $flag"
                "id" -> "Terbuka $flag"
                else -> en
            }
        }
        if (en.startsWith("Last session ping: ")) {
            val v = en.removePrefix("Last session ping: ")
            return when (lang) {
                "zh" -> "上次游玩延迟: $v"
                "ja" -> "前回のPing: $v"
                "ko" -> "최근 플레이 핑: $v"
                "es" -> "Ping última sesión: $v"
                "pt" -> "Ping da última sessão: $v"
                "fr" -> "Ping dernière session : $v"
                "de" -> "Ping letzte Sitzung: $v"
                "ru" -> "Пинг прошлой сессии: $v"
                "th" -> "ปิงครั้งล่าสุด: $v"
                "id" -> "Ping sesi terakhir: $v"
                else -> en
            }
        }
        if (en.startsWith("Selected: ")) {
            val srv = en.removePrefix("Selected: ")
            return when (lang) {
                "zh" -> "当前节点: $srv"
                "ja" -> "選択中: $srv"
                "ko" -> "선택됨: $srv"
                "es" -> "Seleccionado: $srv"
                "pt" -> "Selecionado: $srv"
                "fr" -> "Sélectionné : $srv"
                "de" -> "Ausgewählt: $srv"
                "ru" -> "Выбрано: $srv"
                "th" -> "กำลังเลือก: $srv"
                "id" -> "Dipilih: $srv"
                else -> en
            }
        }
        if (en.startsWith("Faster server: ")) {
            val srv = en.removePrefix("Faster server: ")
            return when (lang) {
                "zh" -> "发现更快节点: $srv"
                "ja" -> "より高速なサーバー: $srv"
                "ko" -> "더 빠른 서버: $srv"
                "es" -> "Servidor más rápido: $srv"
                "pt" -> "Servidor mais rápido: $srv"
                "fr" -> "Serveur plus rapide : $srv"
                "de" -> "Schnellerer Server: $srv"
                "ru" -> "Более быстрый сервер: $srv"
                "th" -> "เซิร์ฟเวอร์ที่เร็วกว่า: $srv"
                "id" -> "Server lebih cepat: $srv"
                else -> en
            }
        }
        if (en.startsWith("Network latency: ")) {
            val v = en.removePrefix("Network latency: ")
            return when (lang) {
                "zh" -> "网络延迟: $v"
                "ja" -> "ネットワーク遅延: $v"
                "ko" -> "네트워크 지연: $v"
                "es" -> "Latencia de red: $v"
                "pt" -> "Latência de rede: $v"
                "fr" -> "Latence réseau : $v"
                "de" -> "Netzwerklatenz: $v"
                "ru" -> "Задержка сети: $v"
                "th" -> "ความหน่วงเครือข่าย: $v"
                "id" -> "Latensi jaringan: $v"
                else -> en
            }
        }
        if (en.endsWith(" games")) {
            val num = en.removeSuffix(" games")
            return when (lang) {
                "zh" -> "$num 款游戏"
                "ja" -> "$num 本のゲーム"
                "ko" -> "${num}개 게임"
                "es" -> "$num juegos"
                "pt" -> "$num jogos"
                "fr" -> "$num jeux"
                "de" -> "$num Spiele"
                "ru" -> "$num игр"
                "th" -> "$num เกม"
                "id" -> "$num game"
                else -> en
            }
        }
        if (en.startsWith("❤️ Favorites (")) {
            val count = en.removePrefix("❤️ Favorites ")
            return when (lang) {
                "zh" -> "❤️ 收藏 $count"
                "ja" -> "❤️ お気に入り $count"
                "ko" -> "❤️ 즐겨찾기 $count"
                "es" -> "❤️ Favoritos $count"
                "pt" -> "❤️ Favoritos $count"
                "fr" -> "❤️ Favoris $count"
                "de" -> "❤️ Favoriten $count"
                "ru" -> "❤️ Избранное $count"
                "th" -> "❤️ รายการโปรด $count"
                "id" -> "❤️ Favorit $count"
                else -> en
            }
        }
        if (en.startsWith("🔥 Xbox Popular (")) {
            val count = en.removePrefix("🔥 Xbox Popular ")
            return when (lang) {
                "zh" -> "🔥 Xbox 热门 $count"
                "ja" -> "🔥 Xbox 人気 $count"
                "ko" -> "🔥 Xbox 인기 $count"
                "es" -> "🔥 Populares Xbox $count"
                "pt" -> "🔥 Populares Xbox $count"
                "fr" -> "🔥 Populaires Xbox $count"
                "de" -> "🔥 Xbox Beliebt $count"
                "ru" -> "🔥 Популярное $count"
                "th" -> "🔥 ยอดนิยม $count"
                "id" -> "🔥 Populer $count"
                else -> en
            }
        }
        if (en.startsWith("🆕 New on Cloud (")) {
            val count = en.removePrefix("🆕 New on Cloud ")
            return when (lang) {
                "zh" -> "🆕 最新上架 $count"
                "ja" -> "🆕 新着クラウド $count"
                "ko" -> "🆕 신규 추가 $count"
                "es" -> "🆕 Nuevos $count"
                "pt" -> "🆕 Novos $count"
                "fr" -> "🆕 Nouveautés $count"
                "de" -> "🆕 Neu in der Cloud $count"
                "ru" -> "🆕 Новинки $count"
                "th" -> "🆕 มาใหม่ $count"
                "id" -> "🆕 Baru di Cloud $count"
                else -> en
            }
        }
        return en
    }

    /**
     * Từ điển dịch nhanh O(1) giao diện Xbox Cloud Gaming (xbox.com/play) cho TẤT CẢ ngôn ngữ đích.
     * Bổ sung đầy đủ từ vựng từ trang HTML Xbox Cloud Gaming (Call of Duty: Modern Warfare 4 - Vault Edition,
     * NBA 2K27 (XBOX Series X|S), Chơi game trên mây Xbox tại Xbox.com, v.v.).
     */
    val XBOX_MULTILANG_DICTIONARIES: Map<String, Map<String, String>> = mapOf(
        "en" to mapOf(
            // Dịch từ tiếng Nhật / Hàn / Việt trên trang Xbox sang Tiếng Anh chuẩn khi người dùng chọn English
            "クラウド ゲーム" to "Cloud Gaming",
            "クラウド ゲーミング" to "Cloud Gaming",
            "プレイ" to "Play",
            "今すぐプレイ" to "Play now",
            "サインイン" to "Sign in",
            "サインアウト" to "Sign out",
            "すべてのゲーム" to "All games",
            "最近プレイしたゲーム" to "Recently played",
            "設定" to "Settings",
            "終了" to "Quit game",
            "キューで待機中" to "You're in the queue",
            "コントローラーが必要です" to "Controller required",
            "タッチ操作に対応" to "Play with touch",
            "클라우드 게임" to "Cloud Gaming",
            "플레이" to "Play",
            "지금 플레이" to "Play now",
            "로그인" to "Sign in",
            "로그아웃" to "Sign out",
            "모든 게임" to "All games",
            "최근 플레이한 게임" to "Recently played",
            "설정" to "Settings",
            "종료" to "Quit game",
            "Chơi game trên mây Xbox tại Xbox.com" to "Xbox Cloud Gaming on Xbox.com",
            "Chơi Game Đám Mây" to "Cloud Gaming",
            "Cloud Gaming" to "Cloud Gaming",
            "Xbox Cloud Gaming on Xbox.com" to "Xbox Cloud Gaming on Xbox.com",
            "Call of Duty: Modern Warfare 4 - Vault Edition" to "Call of Duty: Modern Warfare 4 - Vault Edition",
            "NBA 2K27 (XBOX Series X|S)" to "NBA 2K27 (XBOX Series X|S)"
        ),
        "zh" to mapOf(
            "Cloud Gaming" to "云游戏",
            "Cloud Gaming (Beta)" to "云游戏 (测试版)",
            "Xbox Cloud Gaming" to "Xbox 云游戏",
            "Chơi game trên mây Xbox tại Xbox.com" to "在 Xbox.com 上畅玩 Xbox 云游戏",
            "Play" to "开始游戏",
            "Play now" to "立即开玩",
            "Play game" to "开始游戏",
            "Play with Cloud Gaming" to "通过云游戏畅玩",
            "Play with touch" to "支持触控游玩",
            "Touch controls" to "触控操作",
            "Controller required" to "需要连接手柄",
            "Jump back in" to "继续游玩",
            "Recently played" to "最近游玩",
            "Most popular" to "最受欢迎",
            "All games" to "全部游戏",
            "All Games" to "全部游戏",
            "Included with Game Pass" to "包含在 Game Pass 中",
            "Included with Game Pass Ultimate" to "包含在 Game Pass Ultimate 中",
            "Get Game Pass" to "获取 Game Pass",
            "Join Game Pass" to "加入 Game Pass",
            "Stream your own game" to "串流您拥有的游戏",
            "Sign in" to "登录",
            "Sign In" to "登录",
            "Sign out" to "退出登录",
            "Settings" to "设置",
            "Quit game" to "退出游戏",
            "Resume" to "继续游戏",
            "Resume game" to "继续游戏",
            "Disconnect" to "断开连接",
            "Try again" to "重试",
            "Waiting for an open server..." to "正在等待空闲服务器...",
            "You're in the queue" to "正在排队等待进入游戏",
            "Estimated wait time" to "预计等待时间",
            "Connecting..." to "正在连接...",
            "Launching with cloud gaming..." to "正在启动云游戏会话...",
            "Optimized for Xbox Series X|S" to "针对 Xbox Series X|S 优化",
            "Vault Edition" to "金库版 (Vault Edition)",
            "Standard Edition" to "标准版",
            "Deluxe Edition" to "豪华版",
            "Cross-Gen Bundle" to "跨世代同捆包",
            "Search" to "搜索",
            "Search games" to "搜索游戏",
            "Details" to "详情",
            "Capabilities" to "支持功能",
            "Achievements" to "成就",
            "Cloud saves" to "云存档",
            "Online multiplayer" to "在线多人游戏",
            "Online co-op" to "在线合作",
            "Single player" to "单人游戏"
        ),
        "ja" to mapOf(
            "Cloud Gaming" to "クラウド ゲーミング",
            "Xbox Cloud Gaming" to "Xbox クラウド ゲーミング",
            "Chơi game trên mây Xbox tại Xbox.com" to "Xbox.com で Xbox クラウド ゲーミングをプレイ",
            "Play" to "プレイ",
            "Play now" to "今すぐプレイ",
            "Play with Cloud Gaming" to "クラウド ゲーミングでプレイ",
            "Play with touch" to "タッチでプレイ",
            "Controller required" to "コントローラーが必要です",
            "Jump back in" to "再開する",
            "Recently played" to "最近プレイしたゲーム",
            "Most popular" to "人気ランキング",
            "All games" to "すべてのゲーム",
            "Included with Game Pass" to "Game Pass に含まれます",
            "Get Game Pass" to "Game Pass に入会",
            "Sign in" to "サインイン",
            "Sign out" to "サインアウト",
            "Settings" to "設定",
            "Quit game" to "ゲームを終了",
            "Resume game" to "ゲームを再開",
            "Try again" to "再試行",
            "Waiting for an open server..." to "サーバーの空きを待っています...",
            "You're in the queue" to "キューで待機中",
            "Estimated wait time" to "予想待ち時間",
            "Optimized for Xbox Series X|S" to "Xbox Series X|S 最適化",
            "Vault Edition" to "秘蔵版 (Vault Edition)",
            "Search games" to "ゲームを検索",
            "Achievements" to "実績",
            "Cloud saves" to "クラウド セーブ"
        ),
        "ko" to mapOf(
            "Cloud Gaming" to "클라우드 게임",
            "Xbox Cloud Gaming" to "Xbox 클라우드 게임",
            "Chơi game trên mây Xbox tại Xbox.com" to "Xbox.com에서 Xbox 클라우드 게임 플레이",
            "Play" to "플레이",
            "Play now" to "지금 플레이",
            "Play with Cloud Gaming" to "클라우드 게임으로 플레이",
            "Play with touch" to "터치로 플레이",
            "Controller required" to "컨트롤러 필요",
            "Jump back in" to "다시 시작하기",
            "Recently played" to "최근 플레이한 게임",
            "Most popular" to "인기 게임",
            "All games" to "모든 게임",
            "Included with Game Pass" to "Game Pass에 포함됨",
            "Get Game Pass" to "Game Pass 가입",
            "Sign in" to "로그인",
            "Sign out" to "로그아웃",
            "Settings" to "설정",
            "Quit game" to "게임 종료",
            "Resume game" to "게임 계속하기",
            "Try again" to "다시 시도",
            "Waiting for an open server..." to "빈 서버를 기다리는 중...",
            "You're in the queue" to "대기열에서 대기 중입니다",
            "Estimated wait time" to "예상 대기 시간",
            "Optimized for Xbox Series X|S" to "Xbox Series X|S 최적화",
            "Vault Edition" to "볼트 에디션 (Vault Edition)",
            "Search games" to "게임 검색",
            "Achievements" to "도전 과제",
            "Cloud saves" to "클라우드 저장"
        ),
        "es" to mapOf(
            "Cloud Gaming" to "Juego en la nube",
            "Play" to "Jugar",
            "Play now" to "Jugar ahora",
            "Play with touch" to "Jugar con controles táctiles",
            "Controller required" to "Se requiere mando",
            "Jump back in" to "Volver a jugar",
            "Recently played" to "Jugados recientemente",
            "Most popular" to "Más populares",
            "All games" to "Todos los juegos",
            "Included with Game Pass" to "Incluido con Game Pass",
            "Sign in" to "Iniciar sesión",
            "Sign out" to "Cerrar sesión",
            "Settings" to "Configuración",
            "Quit game" to "Salir del juego",
            "Resume game" to "Reanudar juego",
            "Waiting for an open server..." to "Esperando un servidor disponible...",
            "You're in the queue" to "Estás en la fila de espera",
            "Estimated wait time" to "Tiempo de espera estimado",
            "Search games" to "Buscar juegos",
            "Achievements" to "Logros"
        ),
        "pt" to mapOf(
            "Cloud Gaming" to "Jogos na Nuvem",
            "Play" to "Jogar",
            "Play now" to "Jogar agora",
            "Play with touch" to "Jogar com toque",
            "Controller required" to "Requer controle",
            "Jump back in" to "Voltar a jogar",
            "Recently played" to "Jogados recentemente",
            "Most popular" to "Mais populares",
            "All games" to "Todos os jogos",
            "Included with Game Pass" to "Incluído no Game Pass",
            "Sign in" to "Entrar",
            "Sign out" to "Sair",
            "Settings" to "Configurações",
            "Quit game" to "Sair do jogo",
            "Resume game" to "Retomar jogo",
            "Waiting for an open server..." to "Aguardando um servidor disponível...",
            "You're in the queue" to "Você está na fila",
            "Estimated wait time" to "Tempo estimado de espera",
            "Search games" to "Pesquisar jogos",
            "Achievements" to "Conquistas"
        ),
        "fr" to mapOf(
            "Cloud Gaming" to "Jeu dans le cloud",
            "Play" to "Jouer",
            "Play now" to "Jouer maintenant",
            "Play with touch" to "Jouer avec le tactile",
            "Controller required" to "Manette requise",
            "Jump back in" to "Reprendre",
            "Recently played" to "Joués récemment",
            "Most popular" to "Les plus populaires",
            "All games" to "Tous les jeux",
            "Included with Game Pass" to "Inclus avec le Game Pass",
            "Sign in" to "Se connecter",
            "Sign out" to "Se déconnecter",
            "Settings" to "Paramètres",
            "Quit game" to "Quitter le jeu",
            "Resume game" to "Reprendre le jeu",
            "Waiting for an open server..." to "En attente d'un serveur disponible...",
            "You're in the queue" to "Vous êtes dans la file d'attente",
            "Estimated wait time" to "Temps d'attente estimé",
            "Search games" to "Rechercher des jeux",
            "Achievements" to "Succès"
        ),
        "de" to mapOf(
            "Cloud Gaming" to "Cloud-Gaming",
            "Play" to "Spielen",
            "Play now" to "Jetzt spielen",
            "Play with touch" to "Mit Touch spielen",
            "Controller required" to "Controller erforderlich",
            "Jump back in" to "Weiterspielen",
            "Recently played" to "Kürzlich gespielt",
            "Most popular" to "Am beliebtesten",
            "All games" to "Alle Spiele",
            "Included with Game Pass" to "Im Game Pass enthalten",
            "Sign in" to "Anmelden",
            "Sign out" to "Abmelden",
            "Settings" to "Einstellungen",
            "Quit game" to "Spiel beenden",
            "Resume game" to "Spiel fortsetzen",
            "Waiting for an open server..." to "Warten auf freien Server...",
            "You're in the queue" to "Du bist in der Warteschlange",
            "Estimated wait time" to "Geschätzte Wartezeit",
            "Search games" to "Spiele suchen",
            "Achievements" to "Erfolge"
        ),
        "ru" to mapOf(
            "Cloud Gaming" to "Облачный гейминг",
            "Play" to "Играть",
            "Play now" to "Играть сейчас",
            "Play with touch" to "Сенсорное управление",
            "Controller required" to "Требуется геймпад",
            "Jump back in" to "Продолжить игру",
            "Recently played" to "Недавние игры",
            "Most popular" to "Популярные",
            "All games" to "Все игры",
            "Included with Game Pass" to "Входит в Game Pass",
            "Sign in" to "Войти",
            "Sign out" to "Выйти",
            "Settings" to "Настройки",
            "Quit game" to "Выйти из игры",
            "Resume game" to "Продолжить",
            "Waiting for an open server..." to "Ожидание свободного сервера...",
            "You're in the queue" to "Вы в очереди",
            "Estimated wait time" to "Расчетное время ожидания",
            "Search games" to "Поиск игр",
            "Achievements" to "Достижения"
        ),
        "th" to mapOf(
            "Cloud Gaming" to "เล่นเกมบนคลาวด์",
            "Play" to "เล่นเลย",
            "Play now" to "เล่นทันที",
            "Play with touch" to "เล่นด้วยปุ่มสัมผัส",
            "Controller required" to "ต้องใช้จอยคอนโทรลเลอร์",
            "Jump back in" to "กลับเข้าเล่นต่อ",
            "Recently played" to "เล่นล่าสุด",
            "Most popular" to "ยอดนิยม",
            "All games" to "เกมทั้งหมด",
            "Included with Game Pass" to "รวมอยู่ใน Game Pass",
            "Sign in" to "ลงชื่อเข้าใช้",
            "Sign out" to "ออกจากระบบ",
            "Settings" to "ตั้งค่า",
            "Quit game" to "ออกจากเกม",
            "Resume game" to "เล่นเกมต่อ",
            "Waiting for an open server..." to "กำลังรอเซิร์ฟเวอร์ว่าง...",
            "You're in the queue" to "คุณอยู่ในคิวรอเข้าเกม",
            "Estimated wait time" to "เวลารอโดยประมาณ",
            "Search games" to "ค้นหาเกม",
            "Achievements" to "ความสำเร็จ"
        ),
        "id" to mapOf(
            "Cloud Gaming" to "Cloud Gaming",
            "Play" to "Mainkan",
            "Play now" to "Main Sekarang",
            "Play with touch" to "Main dengan Sentuhan",
            "Controller required" to "Perlu Kontroler",
            "Jump back in" to "Lanjutkan Main",
            "Recently played" to "Baru Dimainkan",
            "Most popular" to "Paling Populer",
            "All games" to "Semua Game",
            "Included with Game Pass" to "Termasuk dalam Game Pass",
            "Sign in" to "Masuk",
            "Sign out" to "Keluar",
            "Settings" to "Pengaturan",
            "Quit game" to "Keluar dari Game",
            "Resume game" to "Lanjutkan Game",
            "Waiting for an open server..." to "Menunggu server tersedia...",
            "You're in the queue" to "Anda dalam antrean",
            "Estimated wait time" to "Perkiraan waktu tunggu",
            "Search games" to "Cari game",
            "Achievements" to "Pencapaian"
        )
    )

    fun getXboxDictForLanguage(rawLangCode: String): Map<String, String> {
        val code = normalizeLangCode(rawLangCode)
        return XBOX_MULTILANG_DICTIONARIES[code] ?: emptyMap()
    }
}
