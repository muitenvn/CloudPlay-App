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

    private val UI_PHRASES_BY_LANG: Map<String, Map<String, String>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        mapOf(
            "zh" to mapOf(
            "Auto Translate" to "自动翻译",
            "More options" to "更多选项",
            "Share & QR Code" to "分享与二维码",
            "Cleared saved login & web data!" to "已清除保存的登录和网页数据！",
            "🎮 Controller Ready" to "🎮 手柄已就绪",
            "👆 Touch Controls" to "👆 触控操作",
            "Direct Net" to "直连网络",
            "Auto VPN" to "自动关闭 VPN",
            "4G/5G & Wi-Fi" to "支持 4G/5G 与 Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ 4G/5G 与 Wi-Fi 低延迟优化",
            "Full HD • Smooth 60FPS" to "全高清 1080p • 流畅 60FPS",
            "Free • Instant Play" to "免费畅玩 • 秒开即玩",
            "WebRTC RTT • Tap to switch" to "WebRTC 实时延迟 • 点击切换",
            "Last ping" to "上次延迟",
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
            "Close" to "关闭",
            "Unlocked" to "已解锁",
            "Unlock Failed" to "解锁失败",
            "Ready" to "就绪",
            "Wrong Region" to "地区不符",
            "Selected" to "当前节点",
            "Custom" to "自定义",
            "Add Custom" to "添加代理",
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
            "Sports" to "体育竞技",
            "HOW TO USE" to "基本使用指南",
            "Sign in, unlock region, and set up controls" to "登录账号、跨区解锁与手柄控制设置",
            "Start" to "快速开始",
            "Quality" to "网络与画质",
            "Controls" to "手柄控制",
            "Help" to "常见问题",
            "Optimize" to "优化设置",
            "Account" to "账号",
            "Region" to "跨区",
            "Got It • Start Playing 🎮" to "明白了 • 开始游戏 🎮",
            "Share CloudPlay App" to "分享 CloudPlay 应用",
            "Scan QR code or send download link" to "扫描二维码或发送下载链接",
            "QR Code & Link" to "二维码与下载链接",
            "Web Page" to "介绍网页",
            "SCAN QR TO DOWNLOAD" to "扫码下载应用",
            "🌐 Web Download" to "🌐 网页下载",
            "📥 Direct APK" to "📥 直接下载 APK",
            "🌐 Download Web Page" to "🌐 应用介绍与下载网页",
            "📥 Direct APK File Link" to "📥 APK 安装包直链",
            "Share Link" to "分享链接",
            "Download APK" to "下载 APK",
            "New Update Available" to "发现新版本",
            "🤖 AI Update Summary" to "🤖 AI 更新内容摘要",
            "What's new" to "更新内容"
        ),
        "ja" to mapOf(
            "Auto Translate" to "自動翻訳",
            "More options" to "その他のオプション",
            "Share & QR Code" to "共有 & QRコード",
            "Cleared saved login & web data!" to "ログイン情報とWebキャッシュを消去しました！",
            "🎮 Controller Ready" to "🎮 コントローラー準備完了",
            "👆 Touch Controls" to "👆 タッチ操作",
            "Direct Net" to "ダイレクト接続",
            "Auto VPN" to "自動VPNオフ",
            "4G/5G & Wi-Fi" to "4G/5G & Wi-Fi 対応",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ 4G/5G & Wi-Fi 低遅延モード",
            "Full HD • Smooth 60FPS" to "Full HD • なめらか 60FPS",
            "Free • Instant Play" to "無料 • すぐにプレイ",
            "WebRTC RTT • Tap to switch" to "WebRTC 遅延 • タップで切替",
            "Last ping" to "前回のPing",
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
            "Close" to "閉じる",
            "Unlocked" to "解除済み",
            "Unlock Failed" to "解除失敗",
            "Ready" to "準備完了",
            "Wrong Region" to "非対応地域",
            "Selected" to "選択中",
            "Custom" to "カスタム",
            "Add Custom" to "カスタム追加",
            "Testing network latency..." to "ネットワーク遅延を測定中...",
            "Network latency: Unreachable" to "ネットワーク遅延: 接続不可",
            "Network latency: Not tested" to "ネットワーク遅延: 未測定",
            "Auto Best" to "最適を選択",
            "Pinging..." to "Ping測定中...",
            "Ping Test" to "Ping測定",
            "Updating..." to "更新中...",
            "Update" to "サーバー更新",
            "Return to en-US" to "en-US に戻す",
            "Return to Game" to "ゲームに戻る",
            "HOW TO USE" to "基本的な使い方",
            "Sign in, unlock region, and set up controls" to "サインイン・地域ロック解除・操作設定",
            "Start" to "はじめに",
            "Quality" to "通信と画質",
            "Controls" to "コントローラー",
            "Help" to "トラブル対処",
            "Optimize" to "最適化",
            "Account" to "アカウント",
            "Region" to "地域解除",
            "Got It • Start Playing 🎮" to "了解 • ゲームを始める 🎮",
            "Share CloudPlay App" to "CloudPlay アプリを共有",
            "Scan QR code or send download link" to "QRコード読取またはリンクを送信",
            "QR Code & Link" to "QRコード & リンク",
            "Web Page" to "紹介ページ",
            "SCAN QR TO DOWNLOAD" to "QRコードでダウンロード",
            "🌐 Web Download" to "🌐 Webページ",
            "📥 Direct APK" to "📥 APK直リンク",
            "Share Link" to "リンクを共有",
            "Download APK" to "APKをダウンロード",
            "New Update Available" to "新しいアップデートがあります",
            "🤖 AI Update Summary" to "🤖 AI アップデート要約",
            "What's new" to "更新内容"
        ),
        "ko" to mapOf(
            "Auto Translate" to "자동 번역",
            "More options" to "더 보기",
            "Share & QR Code" to "공유 및 QR 코드",
            "Cleared saved login & web data!" to "저장된 로그인 및 웹 데이터를 초기화했습니다!",
            "🎮 Controller Ready" to "🎮 컨트롤러 준비됨",
            "👆 Touch Controls" to "👆 터치 컨트롤",
            "Direct Net" to "직접 연결",
            "Auto VPN" to "자동 VPN 끄기",
            "4G/5G & Wi-Fi" to "4G/5G 및 Wi-Fi 지원",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ 4G/5G 및 Wi-Fi 저지연 최적화",
            "Full HD • Smooth 60FPS" to "Full HD • 부드러운 60FPS",
            "Free • Instant Play" to "무료 • 즉시 플레이",
            "WebRTC RTT • Tap to switch" to "WebRTC 지연 • 탭하여 서버 변경",
            "Last ping" to "최근 핑",
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
            "Close" to "닫기",
            "Unlocked" to "잠금 해제됨",
            "Unlock Failed" to "해제 실패",
            "Ready" to "준비됨",
            "Wrong Region" to "지원되지 않는 지역",
            "Selected" to "선택됨",
            "Custom" to "사용자 지정",
            "Add Custom" to "프록시 추가",
            "Testing network latency..." to "네트워크 지연 시간 측정 중...",
            "Network latency: Unreachable" to "네트워크 지연: 연결 불가",
            "Network latency: Not tested" to "네트워크 지연: 미측정",
            "Auto Best" to "최적 서버 선택",
            "Pinging..." to "핑 측정 중...",
            "Ping Test" to "핑 측정",
            "Updating..." to "업데이트 중...",
            "Update" to "업데이트",
            "Return to en-US" to "en-US로 돌아가기",
            "Return to Game" to "게임으로 돌아가기",
            "HOW TO USE" to "기본 사용법",
            "Sign in, unlock region, and set up controls" to "로그인, 지역 잠금 해제 및 컨트롤러 설정",
            "Start" to "시작하기",
            "Quality" to "네트워크/화질",
            "Controls" to "컨트롤러",
            "Help" to "문제 해결",
            "Optimize" to "최적화",
            "Account" to "계정",
            "Region" to "지역 해제",
            "Got It • Start Playing 🎮" to "확인 • 게임 시작 🎮",
            "Share CloudPlay App" to "CloudPlay 앱 공유",
            "Scan QR code or send download link" to "QR 코드 스캔 또는 다운로드 링크 전송",
            "QR Code & Link" to "QR 코드 및 링크",
            "Web Page" to "소개 웹페이지",
            "SCAN QR TO DOWNLOAD" to "QR 스캔하여 다운로드",
            "🌐 Web Download" to "🌐 웹 다운로드",
            "📥 Direct APK" to "📥 APK 직접 링크",
            "Share Link" to "링크 공유",
            "Download APK" to "APK 다운로드",
            "New Update Available" to "새 업데이트 사용 가능",
            "🤖 AI Update Summary" to "🤖 AI 업데이트 요약",
            "What's new" to "새로운 기능"
        ),
        "es" to mapOf(
            "Auto Translate" to "Traducción automática",
            "More options" to "Más opciones",
            "Share & QR Code" to "Compartir y código QR",
            "Cleared saved login & web data!" to "¡Datos de inicio de sesión y caché borrados!",
            "🎮 Controller Ready" to "🎮 Mando listo",
            "👆 Touch Controls" to "👆 Controles táctiles",
            "Direct Net" to "Red directa",
            "Auto VPN" to "Apagar VPN auto",
            "4G/5G & Wi-Fi" to "4G/5G y Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Baja latencia 4G/5G y Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS fluidos",
            "Free • Instant Play" to "Gratis • Juego instantáneo",
            "WebRTC RTT • Tap to switch" to "RTT WebRTC • Toca para cambiar",
            "Last ping" to "Último ping",
            "Last session ping: --" to "Ping última sesión: --",
            "QUICK RESUME" to "CONTINUAR RÁPIDO",
            "• 60FPS STREAM" to "• STREAM 60FPS",
            "Loading games..." to "Cargando juegos...",
            "No games yet" to "Aún no hay juegos",
            "Favorite" to "Favorito",
            "History" to "Historial",
            "Refresh Catalog" to "Actualizar catálogo",
            "Refresh" to "Actualizar",
            "All Genres" to "Todos los géneros",
            "Shooter / FPS" to "Disparos / FPS",
            "Action & Adv" to "Acción y Aventura",
            "Racing & Sim" to "Carreras y Sim",
            "RPG & Survival" to "RPG y Supervivencia",
            "Sports & Fight" to "Deportes y Lucha",
            "Syncing..." to "Sincronizando...",
            "Loading games from Xbox Cloud..." to "Cargando juegos desde Xbox Cloud...",
            "No matching games found" to "No se encontraron juegos",
            "Searching Xbox..." to "Buscando en Xbox...",
            "Select" to "Elegir",
            "Switch server" to "Cambiar servidor",
            "Dismiss" to "Cerrar",
            "Close" to "Cerrar",
            "Unlocked" to "Desbloqueado",
            "Unlock Failed" to "Error al desbloquear",
            "Ready" to "Listo",
            "Wrong Region" to "Región incorrecta",
            "Selected" to "Seleccionado",
            "Custom" to "Personalizado",
            "Add Custom" to "Añadir proxy",
            "Auto Best" to "Mejor servidor",
            "Pinging..." to "Midiendo ping...",
            "Ping Test" to "Medir Ping",
            "Update" to "Actualizar",
            "Return to Game" to "Volver al juego",
            "HOW TO USE" to "CÓMO USAR",
            "Sign in, unlock region, and set up controls" to "Inicia sesión, desbloquea región y configura controles",
            "Start" to "Inicio",
            "Quality" to "Calidad",
            "Controls" to "Controles",
            "Help" to "Ayuda",
            "Account" to "Cuenta",
            "Region" to "Región",
            "Got It • Start Playing 🎮" to "Entendido • Jugar 🎮",
            "Share CloudPlay App" to "Compartir aplicación CloudPlay",
            "Scan QR code or send download link" to "Escanea el código QR o envía el enlace",
            "QR Code & Link" to "Código QR y Enlace",
            "Web Page" to "Página Web",
            "SCAN QR TO DOWNLOAD" to "ESCANEA EL QR PARA DESCARGAR",
            "Share Link" to "Compartir enlace",
            "Download APK" to "Descargar APK",
            "New Update Available" to "Nueva actualización disponible",
            "🤖 AI Update Summary" to "🤖 Resumen de actualización (IA)",
            "What's new" to "Novedades"
        ),
        "pt" to mapOf(
            "Auto Translate" to "Tradução automática",
            "More options" to "Mais opções",
            "Share & QR Code" to "Compartilhar e QR Code",
            "Cleared saved login & web data!" to "Dados de login e cache limpos!",
            "🎮 Controller Ready" to "🎮 Controle pronto",
            "👆 Touch Controls" to "👆 Controles de toque",
            "Direct Net" to "Rede direta",
            "Auto VPN" to "Desligar VPN auto",
            "4G/5G & Wi-Fi" to "4G/5G e Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Baixa latência 4G/5G e Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS suaves",
            "Free • Instant Play" to "Grátis • Jogar agora",
            "WebRTC RTT • Tap to switch" to "RTT WebRTC • Toque para trocar",
            "Last ping" to "Último ping",
            "Last session ping: --" to "Ping da última sessão: --",
            "QUICK RESUME" to "RETOMADA RÁPIDA",
            "• 60FPS STREAM" to "• STREAM 60FPS",
            "Loading games..." to "Carregando jogos...",
            "No games yet" to "Nenhum jogo ainda",
            "Favorite" to "Favorito",
            "History" to "Histórico",
            "Refresh Catalog" to "Atualizar catálogo",
            "Refresh" to "Atualizar",
            "All Genres" to "Todos os gêneros",
            "Shooter / FPS" to "Tiro / FPS",
            "Action & Adv" to "Ação e Aventura",
            "Racing & Sim" to "Corrida e Sim",
            "RPG & Survival" to "RPG e Sobrevivência",
            "Sports & Fight" to "Esportes e Luta",
            "Select" to "Selecionar",
            "Switch server" to "Trocar servidor",
            "Dismiss" to "Fechar",
            "Close" to "Fechar",
            "Unlocked" to "Desbloqueado",
            "Unlock Failed" to "Falha no desbloqueio",
            "Ready" to "Pronto",
            "Wrong Region" to "Região incorreta",
            "Selected" to "Selecionado",
            "Custom" to "Personalizado",
            "Add Custom" to "Adicionar proxy",
            "Auto Best" to "Melhor servidor",
            "Pinging..." to "Testando ping...",
            "Ping Test" to "Testar Ping",
            "Update" to "Atualizar",
            "Return to Game" to "Voltar ao jogo",
            "HOW TO USE" to "COMO USAR",
            "Sign in, unlock region, and set up controls" to "Entre na conta, desbloqueie a região e configure controles",
            "Start" to "Início",
            "Quality" to "Qualidade",
            "Controls" to "Controles",
            "Help" to "Ajuda",
            "Account" to "Conta",
            "Region" to "Região",
            "Got It • Start Playing 🎮" to "Entendi • Começar a Jogar 🎮",
            "Share CloudPlay App" to "Compartilhar aplicativo CloudPlay",
            "Scan QR code or send download link" to "Escaneie o QR Code ou envie o link",
            "QR Code & Link" to "QR Code e Link",
            "Web Page" to "Página Web",
            "SCAN QR TO DOWNLOAD" to "ESCANEIE O QR PARA BAIXAR",
            "Share Link" to "Compartilhar Link",
            "Download APK" to "Baixar APK",
            "New Update Available" to "Nova atualização disponível",
            "🤖 AI Update Summary" to "🤖 Resumo da atualização (IA)",
            "What's new" to "Novidades"
        ),
        "fr" to mapOf(
            "Auto Translate" to "Traduction auto",
            "More options" to "Plus d'options",
            "Share & QR Code" to "Partager & QR Code",
            "Cleared saved login & web data!" to "Données de connexion et cache effacés !",
            "🎮 Controller Ready" to "🎮 Manette prête",
            "👆 Touch Controls" to "👆 Commandes tactiles",
            "Direct Net" to "Réseau direct",
            "Auto VPN" to "Arrêt VPN auto",
            "4G/5G & Wi-Fi" to "4G/5G & Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Faible latence 4G/5G & Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS fluides",
            "Free • Instant Play" to "Gratuit • Jeu instantané",
            "WebRTC RTT • Tap to switch" to "RTT WebRTC • Appuyer pour changer",
            "Last ping" to "Dernier ping",
            "Last session ping: --" to "Ping dernière session : --",
            "QUICK RESUME" to "REPRISE RAPIDE",
            "• 60FPS STREAM" to "• STREAM 60FPS",
            "Loading games..." to "Chargement des jeux...",
            "No games yet" to "Aucun jeu",
            "Favorite" to "Favori",
            "History" to "Historique",
            "Refresh Catalog" to "Actualiser le catalogue",
            "Refresh" to "Actualiser",
            "All Genres" to "Tous les genres",
            "Shooter / FPS" to "Tir / FPS",
            "Action & Adv" to "Action & Aventure",
            "Racing & Sim" to "Course & Sim",
            "RPG & Survival" to "RPG & Survie",
            "Sports & Fight" to "Sports & Combat",
            "Select" to "Choisir",
            "Switch server" to "Changer de serveur",
            "Dismiss" to "Fermer",
            "Close" to "Fermer",
            "Unlocked" to "Débloqué",
            "Unlock Failed" to "Échec du déblocage",
            "Ready" to "Prêt",
            "Wrong Region" to "Mauvaise région",
            "Selected" to "Sélectionné",
            "Custom" to "Personnalisé",
            "Add Custom" to "Ajouter proxy",
            "Auto Best" to "Meilleur serveur",
            "Pinging..." to "Mesure du ping...",
            "Ping Test" to "Tester Ping",
            "Update" to "Actualiser",
            "Return to Game" to "Retour au jeu",
            "HOW TO USE" to "MODE D'EMPLOI",
            "Sign in, unlock region, and set up controls" to "Connexion, déblocage régional et configuration des commandes",
            "Start" to "Début",
            "Quality" to "Qualité",
            "Controls" to "Manette",
            "Help" to "Aide",
            "Account" to "Compte",
            "Region" to "Région",
            "Got It • Start Playing 🎮" to "Compris • Jouer 🎮",
            "Share CloudPlay App" to "Partager l'application CloudPlay",
            "Scan QR code or send download link" to "Scannez le QR code ou envoyez le lien",
            "QR Code & Link" to "QR Code & Lien",
            "Web Page" to "Page Web",
            "SCAN QR TO DOWNLOAD" to "SCANNEZ POUR TÉLÉCHARGER",
            "Share Link" to "Partager le lien",
            "Download APK" to "Télécharger APK",
            "New Update Available" to "Nouvelle mise à jour disponible",
            "🤖 AI Update Summary" to "🤖 Résumé de la mise à jour (IA)",
            "What's new" to "Nouveautés"
        ),
        "de" to mapOf(
            "Auto Translate" to "Auto-Übersetzung",
            "More options" to "Weitere Optionen",
            "Share & QR Code" to "Teilen & QR-Code",
            "Cleared saved login & web data!" to "Anmeldedaten & Web-Cache gelöscht!",
            "🎮 Controller Ready" to "🎮 Controller bereit",
            "👆 Touch Controls" to "👆 Touch-Steuerung",
            "Direct Net" to "Direktnetz",
            "Auto VPN" to "Auto-VPN Aus",
            "4G/5G & Wi-Fi" to "4G/5G & WLAN",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Niedrige Latenz 4G/5G & WLAN",
            "Full HD • Smooth 60FPS" to "Full HD • Flüssige 60FPS",
            "Free • Instant Play" to "Kostenlos • Sofort spielen",
            "WebRTC RTT • Tap to switch" to "WebRTC RTT • Tippen zum Wechseln",
            "Last ping" to "Letzter Ping",
            "Last session ping: --" to "Ping letzte Sitzung: --",
            "QUICK RESUME" to "SCHNELL FORTSETZEN",
            "• 60FPS STREAM" to "• 60FPS STREAM",
            "Loading games..." to "Spiele werden geladen...",
            "No games yet" to "Noch keine Spiele",
            "Favorite" to "Favorit",
            "History" to "Verlauf",
            "Refresh Catalog" to "Katalog aktualisieren",
            "Refresh" to "Aktualisieren",
            "All Genres" to "Alle Genres",
            "Shooter / FPS" to "Shooter / FPS",
            "Action & Adv" to "Action & Abenteuer",
            "Racing & Sim" to "Rennen & Sim",
            "RPG & Survival" to "RPG & Survival",
            "Sports & Fight" to "Sport & Kampf",
            "Select" to "Wählen",
            "Switch server" to "Server wechseln",
            "Dismiss" to "Schließen",
            "Close" to "Schließen",
            "Unlocked" to "Freigeschaltet",
            "Unlock Failed" to "Freischaltung fehlgeschlagen",
            "Ready" to "Bereit",
            "Wrong Region" to "Falsche Region",
            "Selected" to "Ausgewählt",
            "Custom" to "Benutzerdefiniert",
            "Add Custom" to "Proxy hinzufügen",
            "Auto Best" to "Bester Server",
            "Pinging..." to "Ping läuft...",
            "Ping Test" to "Ping testen",
            "Update" to "Aktualisieren",
            "Return to Game" to "Zurück zum Spiel",
            "HOW TO USE" to "ANLEITUNG",
            "Sign in, unlock region, and set up controls" to "Anmelden, Region freischalten und Steuerung einrichten",
            "Start" to "Start",
            "Quality" to "Qualität",
            "Controls" to "Steuerung",
            "Help" to "Hilfe",
            "Account" to "Konto",
            "Region" to "Region",
            "Got It • Start Playing 🎮" to "Verstanden • Spielen 🎮",
            "Share CloudPlay App" to "CloudPlay App teilen",
            "Scan QR code or send download link" to "QR-Code scannen oder Download-Link senden",
            "QR Code & Link" to "QR-Code & Link",
            "Web Page" to "Webseite",
            "SCAN QR TO DOWNLOAD" to "QR-CODE ZUM DOWNLOAD SCANNEN",
            "Share Link" to "Link teilen",
            "Download APK" to "APK herunterladen",
            "New Update Available" to "Neues Update verfügbar",
            "🤖 AI Update Summary" to "🤖 KI-Update-Zusammenfassung",
            "What's new" to "Neuigkeiten"
        ),
        "ru" to mapOf(
            "Auto Translate" to "Автоперевод",
            "More options" to "Ещё",
            "Share & QR Code" to "Поделиться и QR-код",
            "Cleared saved login & web data!" to "Данные входа и кэш очищены!",
            "🎮 Controller Ready" to "🎮 Геймпад готов",
            "👆 Touch Controls" to "👆 Сенсорное управление",
            "Direct Net" to "Прямая сеть",
            "Auto VPN" to "Авто-откл. VPN",
            "4G/5G & Wi-Fi" to "4G/5G и Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Низкий пинг 4G/5G и Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • Плавные 60 FPS",
            "Free • Instant Play" to "Бесплатно • Быстрый старт",
            "WebRTC RTT • Tap to switch" to "RTT WebRTC • Нажмите для смены",
            "Last ping" to "Последний пинг",
            "Last session ping: --" to "Пинг прошлой сессии: --",
            "QUICK RESUME" to "БЫСТРЫЙ ЗАПУСК",
            "• 60FPS STREAM" to "• 60FPS СТРИМ",
            "Loading games..." to "Загрузка игр...",
            "No games yet" to "Пока нет игр",
            "Favorite" to "Избранное",
            "History" to "История",
            "Refresh Catalog" to "Обновить каталог",
            "Refresh" to "Обновить",
            "All Genres" to "Все жанры",
            "Shooter / FPS" to "Шутеры / FPS",
            "Action & Adv" to "Экшен и Приключения",
            "Racing & Sim" to "Гонки и Симуляторы",
            "RPG & Survival" to "RPG и Выживание",
            "Sports & Fight" to "Спорт и Файтинги",
            "Select" to "Выбрать",
            "Switch server" to "Сменить сервер",
            "Dismiss" to "Закрыть",
            "Close" to "Закрыть",
            "Unlocked" to "Разблокировано",
            "Unlock Failed" to "Ошибка разблокировки",
            "Ready" to "Готов",
            "Wrong Region" to "Неверный регион",
            "Selected" to "Выбрано",
            "Custom" to "Свой прокси",
            "Add Custom" to "Добавить прокси",
            "Auto Best" to "Лучший сервер",
            "Pinging..." to "Проверка пинга...",
            "Ping Test" to "Тест пинга",
            "Update" to "Обновить",
            "Return to Game" to "Вернуться в игру",
            "HOW TO USE" to "КАК ПОЛЬЗОВАТЬСЯ",
            "Sign in, unlock region, and set up controls" to "Вход в аккаунт, обход региона и настройка управления",
            "Start" to "Старт",
            "Quality" to "Качество",
            "Controls" to "Геймпад",
            "Help" to "Помощь",
            "Account" to "Аккаунт",
            "Region" to "Регион",
            "Got It • Start Playing 🎮" to "Понятно • Играть 🎮",
            "Share CloudPlay App" to "Поделиться CloudPlay",
            "Scan QR code or send download link" to "Отсканируйте QR-код или отправьте ссылку",
            "QR Code & Link" to "QR-код и ссылка",
            "Web Page" to "Веб-страница",
            "SCAN QR TO DOWNLOAD" to "СКАНИРУЙТЕ QR ДЛЯ СКАЧИВАНИЯ",
            "Share Link" to "Поделиться ссылкой",
            "Download APK" to "Скачать APK",
            "New Update Available" to "Доступно обновление",
            "🤖 AI Update Summary" to "🤖 ИИ-описание обновления",
            "What's new" to "Что нового"
        ),
        "th" to mapOf(
            "Auto Translate" to "แปลภาษาอัตโนมัติ",
            "More options" to "ตัวเลือกเพิ่มเติม",
            "Share & QR Code" to "แชร์ & คิวอาร์โค้ด",
            "Cleared saved login & web data!" to "ล้างข้อมูลการเข้าสู่ระบบและแคชเรียบร้อย!",
            "🎮 Controller Ready" to "🎮 จอยพร้อมใช้งาน",
            "👆 Touch Controls" to "👆 ปุ่มสัมผัส",
            "Direct Net" to "เน็ตตรง",
            "Auto VPN" to "ปิด VPN อัตโนมัติ",
            "4G/5G & Wi-Fi" to "รองรับ 4G/5G & Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ ลดแล็ก 4G/5G และ Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • ลื่นไหล 60FPS",
            "Free • Instant Play" to "เล่นฟรี • เข้าเกมทันที",
            "WebRTC RTT • Tap to switch" to "WebRTC RTT • แตะเพื่อเปลี่ยน",
            "Last ping" to "ปิงล่าสุด",
            "Last session ping: --" to "ปิงครั้งล่าสุด: --",
            "QUICK RESUME" to "เล่นต่อทันที",
            "• 60FPS STREAM" to "• สตรีม 60FPS",
            "Loading games..." to "กำลังโหลดเกม...",
            "No games yet" to "ยังไม่มีเกม",
            "Favorite" to "รายการโปรด",
            "History" to "ประวัติการเล่น",
            "Refresh Catalog" to "รีเฟรชรายชื่อเกม",
            "Refresh" to "รีเฟรช",
            "All Genres" to "ทุกแนวเกม",
            "Shooter / FPS" to "ยิงปืน / FPS",
            "Action & Adv" to "แอ็กชัน & ผจญภัย",
            "Racing & Sim" to "แข่งรถ & จำลอง",
            "RPG & Survival" to "RPG & เอาชีวิตรอด",
            "Sports & Fight" to "กีฬา & ต่อสู้",
            "Select" to "เลือก",
            "Switch server" to "เปลี่ยนเซิร์ฟเวอร์",
            "Dismiss" to "ปิด",
            "Close" to "ปิด",
            "Unlocked" to "ปลดล็อกแล้ว",
            "Unlock Failed" to "ปลดล็อกไม่สำเร็จ",
            "Ready" to "พร้อมใช้",
            "Wrong Region" to "โซนไม่ถูกต้อง",
            "Selected" to "กำลังเลือก",
            "Custom" to "กำหนดเอง",
            "Add Custom" to "เพิ่มพร็อกซี",
            "Auto Best" to "เลือกเซิร์ฟดีสุด",
            "Pinging..." to "กำลังวัดปิง...",
            "Ping Test" to "วัดค่าปิง",
            "Update" to "อัปเดต",
            "Return to Game" to "กลับเข้าสู่เกม",
            "HOW TO USE" to "วิธีใช้งานเบื้องต้น",
            "Sign in, unlock region, and set up controls" to "เข้าสู่ระบบ ปลดล็อกโซน และตั้งค่าปุ่มควบคุม",
            "Start" to "เริ่มต้น",
            "Quality" to "เครือข่าย/ภาพ",
            "Controls" to "จอยควบคุม",
            "Help" to "แก้ไขปัญหา",
            "Account" to "บัญชี",
            "Region" to "ปลดล็อกโซน",
            "Got It • Start Playing 🎮" to "เข้าใจแล้ว • เริ่มเล่นเกม 🎮",
            "Share CloudPlay App" to "แชร์แอป CloudPlay",
            "Scan QR code or send download link" to "สแกน QR Code หรือส่งลิงก์ดาวน์โหลด",
            "QR Code & Link" to "คิวอาร์โค้ด & ลิงก์",
            "Web Page" to "หน้าเว็บ",
            "SCAN QR TO DOWNLOAD" to "สแกน QR เพื่อดาวน์โหลด",
            "Share Link" to "แชร์ลิงก์",
            "Download APK" to "ดาวน์โหลด APK",
            "New Update Available" to "มีอัปเดตเวอร์ชันใหม่",
            "🤖 AI Update Summary" to "🤖 สรุปการอัปเดตโดย AI",
            "What's new" to "มีอะไรใหม่"
        ),
        "id" to mapOf(
            "Auto Translate" to "Terjemahan Otomatis",
            "More options" to "Opsi lainnya",
            "Share & QR Code" to "Bagikan & Kode QR",
            "Cleared saved login & web data!" to "Data login & cache web telah dihapus!",
            "🎮 Controller Ready" to "🎮 Stik Siap",
            "👆 Touch Controls" to "👆 Kontrol Sentuh",
            "Direct Net" to "Jaringan Langsung",
            "Auto VPN" to "Matikan VPN Otomatis",
            "4G/5G & Wi-Fi" to "4G/5G & Wi-Fi",
            "⚡ 4G/5G & Wi-Fi Low Lag" to "⚡ Latensi Rendah 4G/5G & Wi-Fi",
            "Full HD • Smooth 60FPS" to "Full HD • 60FPS Mulus",
            "Free • Instant Play" to "Gratis • Main Instan",
            "WebRTC RTT • Tap to switch" to "WebRTC RTT • Ketuk untuk ganti",
            "Last ping" to "Ping terakhir",
            "Last session ping: --" to "Ping sesi terakhir: --",
            "QUICK RESUME" to "LANJUT CEPAT",
            "• 60FPS STREAM" to "• STREAM 60FPS",
            "Loading games..." to "Memuat game...",
            "No games yet" to "Belum ada game",
            "Favorite" to "Favorit",
            "History" to "Riwayat",
            "Refresh Catalog" to "Segarkan Katalog",
            "Refresh" to "Segarkan",
            "All Genres" to "Semua Genre",
            "Shooter / FPS" to "Tembakan / FPS",
            "Action & Adv" to "Aksi & Petualangan",
            "Racing & Sim" to "Balapan & Simulasi",
            "RPG & Survival" to "RPG & Bertahan Hidup",
            "Sports & Fight" to "Olahraga & Pertarungan",
            "Select" to "Pilih",
            "Switch server" to "Ganti server",
            "Dismiss" to "Tutup",
            "Close" to "Tutup",
            "Unlocked" to "Terbuka",
            "Unlock Failed" to "Gagal membuka wilayah",
            "Ready" to "Siap",
            "Wrong Region" to "Wilayah salah",
            "Selected" to "Dipilih",
            "Custom" to "Kustom",
            "Add Custom" to "Tambah Proxy",
            "Auto Best" to "Server Terbaik",
            "Pinging..." to "Mengukur ping...",
            "Ping Test" to "Cek Ping",
            "Update" to "Perbarui",
            "Return to Game" to "Kembali ke Game",
            "HOW TO USE" to "CARA PENGGUNAAN",
            "Sign in, unlock region, and set up controls" to "Masuk akun, buka kunci wilayah, dan atur kontrol",
            "Start" to "Mulai",
            "Quality" to "Kualitas",
            "Controls" to "Kontrol",
            "Help" to "Bantuan",
            "Account" to "Akun",
            "Region" to "Wilayah",
            "Got It • Start Playing 🎮" to "Mengerti • Mulai Main 🎮",
            "Share CloudPlay App" to "Bagikan Aplikasi CloudPlay",
            "Scan QR code or send download link" to "Pindai kode QR atau kirim tautan unduhan",
            "QR Code & Link" to "Kode QR & Tautan",
            "Web Page" to "Halaman Web",
            "SCAN QR TO DOWNLOAD" to "PINDAI QR UNTUK MENGUNDUH",
            "Share Link" to "Bagikan Tautan",
            "Download APK" to "Unduh APK",
            "New Update Available" to "Pembaruan Baru Tersedia",
            "🤖 AI Update Summary" to "🤖 Ringkasan Pembaruan (AI)",
            "What's new" to "Apa yang baru"
        )
    )
    }

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
        if (en.startsWith("Last session ping: ") || en.startsWith("Last session: ")) {
            val v = en.removePrefix("Last session ping: ").removePrefix("Last session: ")
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
                "pt" -> "Latencia de rede: $v"
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
        when (en) {
            "Home" -> return when (lang) {
                "zh" -> "主页"; "ja" -> "ホーム"; "ko" -> "홈"; "es" -> "Inicio"; "pt" -> "Início"
                "fr" -> "Accueil"; "de" -> "Start"; "ru" -> "Главная"; "th" -> "หน้าหลัก"; "id" -> "Beranda"
                else -> en
            }
            "Library" -> return when (lang) {
                "zh" -> "游戏库"; "ja" -> "ライブラリ"; "ko" -> "라이브러리"; "es" -> "Biblioteca"; "pt" -> "Biblioteca"
                "fr" -> "Bibliothèque"; "de" -> "Bibliothek"; "ru" -> "Библиотека"; "th" -> "คลังเกม"; "id" -> "Pustaka"
                else -> en
            }
            "Servers" -> return when (lang) {
                "zh" -> "服务器"; "ja" -> "サーバー"; "ko" -> "서버"; "es" -> "Servidores"; "pt" -> "Servidores"
                "fr" -> "Serveurs"; "de" -> "Server"; "ru" -> "Серверы"; "th" -> "เซิร์ฟเวอร์"; "id" -> "Server"
                else -> en
            }
            "Settings" -> return when (lang) {
                "zh" -> "设置"; "ja" -> "設定"; "ko" -> "설정"; "es" -> "Ajustes"; "pt" -> "Configurações"
                "fr" -> "Paramètres"; "de" -> "Einstellungen"; "ru" -> "Настройки"; "th" -> "ตั้งค่า"; "id" -> "Pengaturan"
                else -> en
            }
            "Guide", "How to Use" -> return when (lang) {
                "zh" -> "使用指南"; "ja" -> "使い方"; "ko" -> "사용법"; "es" -> "Guía"; "pt" -> "Guia"
                "fr" -> "Guide"; "de" -> "Anleitung"; "ru" -> "Инструкция"; "th" -> "คู่มือ"; "id" -> "Panduan"
                else -> en
            }
            "Direct Network" -> return when (lang) {
                "zh" -> "直连网络"; "ja" -> "ダイレクト接続"; "ko" -> "직접 연결"; "es" -> "Red directa"; "pt" -> "Rede direta"
                "fr" -> "Réseau direct"; "de" -> "Direktnetz"; "ru" -> "Прямая сеть"; "th" -> "เครือข่ายตรง"; "id" -> "Jaringan Langsung"
                else -> en
            }
            "Tap to switch server" -> return when (lang) {
                "zh" -> "点击切换节点"; "ja" -> "タップしてサーバー切替"; "ko" -> "탭하여 서버 변경"; "es" -> "Toca para cambiar"; "pt" -> "Toque para trocar"
                "fr" -> "Toucher pour changer"; "de" -> "Tippen zum Wechseln"; "ru" -> "Нажмите для смены"; "th" -> "แตะเพื่อเปลี่ยนเซิร์ฟเวอร์"; "id" -> "Ketuk untuk ganti server"
                else -> en
            }
            "WebRTC • Tap to switch" -> return when (lang) {
                "zh" -> "WebRTC • 点击切换"; "ja" -> "WebRTC • タップで切替"; "ko" -> "WebRTC • 탭하여 변경"; "es" -> "WebRTC • Toca para cambiar"; "pt" -> "WebRTC • Toque para trocar"
                "fr" -> "WebRTC • Changer"; "de" -> "WebRTC • Wechseln"; "ru" -> "WebRTC • Сменить"; "th" -> "WebRTC • แตะเพื่อเปลี่ยน"; "id" -> "WebRTC • Ketuk untuk ganti"
                else -> en
            }
            "Japan" -> return when (lang) {
                "zh" -> "日本"; "ja" -> "日本"; "ko" -> "일본"; "es" -> "Japón"; "pt" -> "Japão"
                "fr" -> "Japon"; "de" -> "Japan"; "ru" -> "Япония"; "th" -> "ญี่ปุ่น"; "id" -> "Jepang"
                else -> en
            }
            "Korea" -> return when (lang) {
                "zh" -> "韩国"; "ja" -> "韓国"; "ko" -> "한국"; "es" -> "Corea"; "pt" -> "Coreia"
                "fr" -> "Corée"; "de" -> "Korea"; "ru" -> "Корея"; "th" -> "เกาหลี"; "id" -> "Korea"
                else -> en
            }
            "USA" -> return when (lang) {
                "zh" -> "美国"; "ja" -> "米国"; "ko" -> "미국"; "es" -> "EE. UU."; "pt" -> "EUA"
                "fr" -> "États-Unis"; "de" -> "USA"; "ru" -> "США"; "th" -> "สหรัฐฯ"; "id" -> "AS"
                else -> en
            }
            "Third-party public servers • Use for Xbox region unlock only, not for sensitive accounts/data.",
            "⚠️ Public proxies: use for Xbox unlock only, not sensitive data." -> return when (lang) {
                "zh" -> "⚠️ 公共节点仅用于解锁 Xbox 地区，请勿传输敏感数据。"; "ja" -> "⚠️ 公開サーバーはXboxの地域解除のみに使用し、機密通信は避けてください。"; "ko" -> "⚠️ 공개 서버는 Xbox 지역 해제용으로만 사용하고 민감한 정보는 피하세요."
                "es" -> "⚠️ Proxies públicos: solo para desbloquear Xbox, no para datos sensibles."; "pt" -> "⚠️ Proxies públicos: use apenas para desbloquear o Xbox, não dados sensíveis."
                "fr" -> "⚠️ Proxys publics : uniquement pour débloquer Xbox, pas pour données sensibles."; "de" -> "⚠️ Öffentliche Proxys: nur zur Xbox-Regionsfreischaltung nutzen."
                "ru" -> "⚠️ Публичные прокси: только для разблокировки Xbox, не для важных данных."; "th" -> "⚠️ พร็อกซีสาธารณะ: ใช้ปลดล็อกโซน Xbox เท่านั้น อย่าใช้กับข้อมูลสำคัญ"; "id" -> "⚠️ Proxy publik: gunakan hanya untuk buka wilayah Xbox, bukan data sensitif."
                else -> en
            }
            "Untested unlock", "Untested" -> return when (lang) {
                "zh" -> "未测试解锁"; "ja" -> "未確認"; "ko" -> "미확인 서버"; "es" -> "Sin probar"; "pt" -> "Não testado"
                "fr" -> "Non testé"; "de" -> "Ungetestet"; "ru" -> "Не проверен"; "th" -> "ยังไม่ทดสอบ"; "id" -> "Belum diuji"
                else -> en
            }
            "★ BEST" -> return when (lang) {
                "zh" -> "★ 推荐"; "ja" -> "★ 最適"; "ko" -> "★ 추천"; "es" -> "★ MEJOR"; "pt" -> "★ MELHOR"
                "fr" -> "★ TOP"; "de" -> "★ BESTE"; "ru" -> "★ ЛУЧШИЙ"; "th" -> "★ แนะนำ"; "id" -> "★ TERBAIK"
                else -> en
            }
            "Too far" -> return when (lang) {
                "zh" -> "距离过远"; "ja" -> "遠距離"; "ko" -> "거리 멈"; "es" -> "Muy lejos"; "pt" -> "Muito longe"
                "fr" -> "Trop loin"; "de" -> "Zu weit"; "ru" -> "Далеко"; "th" -> "ไกลเกินไป"; "id" -> "Terlalu jauh"
                else -> en
            }
            "Unreachable" -> return when (lang) {
                "zh" -> "无法连接"; "ja" -> "接続不可"; "ko" -> "연결 불가"; "es" -> "Inaccesible"; "pt" -> "Inacessível"
                "fr" -> "Injoignable"; "de" -> "Nicht erreichbar"; "ru" -> "Недоступен"; "th" -> "เชื่อมต่อไม่ได้"; "id" -> "Tidak terjangkau"
                else -> en
            }
            "Not tested" -> return when (lang) {
                "zh" -> "未测速"; "ja" -> "未測定"; "ko" -> "미측정"; "es" -> "Sin medir"; "pt" -> "Não medido"
                "fr" -> "Non mesuré"; "de" -> "Nicht gemessen"; "ru" -> "Не измерено"; "th" -> "ยังไม่วัด"; "id" -> "Belum diukur"
                else -> en
            }
            "Testing..." -> return when (lang) {
                "zh" -> "测速中..."; "ja" -> "測定中..."; "ko" -> "측정 중..."; "es" -> "Midiendo..."; "pt" -> "Testando..."
                "fr" -> "Test..."; "de" -> "Teste..."; "ru" -> "Тест..."; "th" -> "กำลังวัด..."; "id" -> "Menguji..."
                else -> en
            }
            "Selecting..." -> return when (lang) {
                "zh" -> "择优中..."; "ja" -> "選択中..."; "ko" -> "선택 중..."; "es" -> "Eligiendo..."; "pt" -> "Selecionando..."
                "fr" -> "Sélection..."; "de" -> "Wähle..."; "ru" -> "Выбор..."; "th" -> "กำลังเลือก..."; "id" -> "Memilih..."
                else -> en
            }
            "Network latency" -> return when (lang) {
                "zh" -> "网络延迟"; "ja" -> "ネットワーク遅延"; "ko" -> "네트워크 지연"; "es" -> "Latencia de red"; "pt" -> "Latência de rede"
                "fr" -> "Latence réseau"; "de" -> "Netzwerklatenz"; "ru" -> "Задержка сети"; "th" -> "ความหน่วงเครือข่าย"; "id" -> "Latensi jaringan"
                else -> en
            }
            "Last session latency" -> return when (lang) {
                "zh" -> "上次游玩延迟"; "ja" -> "前回の遅延"; "ko" -> "최근 세션 지연"; "es" -> "Latencia última sesión"; "pt" -> "Latência última sessão"
                "fr" -> "Latence dernière session"; "de" -> "Latenz letzte Sitzung"; "ru" -> "Задержка прошлой сессии"; "th" -> "ความหน่วงรอบล่าสุด"; "id" -> "Latensi sesi terakhir"
                else -> en
            }
            "Latency (Good)" -> return when (lang) {
                "zh" -> "网络延迟 (极佳)"; "ja" -> "ネットワーク遅延 (良好)"; "ko" -> "네트워크 지연 (양호)"; "es" -> "Latencia (Buena)"; "pt" -> "Latência (Boa)"
                "fr" -> "Latence (Bonne)"; "de" -> "Latenz (Gut)"; "ru" -> "Задержка (Хорошо)"; "th" -> "ความหน่วง (ดี)"; "id" -> "Latensi (Baik)"
                else -> en
            }
            "Latency (Fair)" -> return when (lang) {
                "zh" -> "网络延迟 (一般)"; "ja" -> "ネットワーク遅延 (普通)"; "ko" -> "네트워크 지연 (보통)"; "es" -> "Latencia (Media)"; "pt" -> "Latência (Média)"
                "fr" -> "Latence (Moyenne)"; "de" -> "Latenz (Mittel)"; "ru" -> "Задержка (Средне)"; "th" -> "ความหน่วง (ปานกลาง)"; "id" -> "Latensi (Sedang)"
                else -> en
            }
            "Latency (High)" -> return when (lang) {
                "zh" -> "网络延迟 (偏高)"; "ja" -> "ネットワーク遅延 (高め)"; "ko" -> "네트워크 지연 (높음)"; "es" -> "Latencia (Alta)"; "pt" -> "Latência (Alta)"
                "fr" -> "Latence (Élevée)"; "de" -> "Latenz (Hoch)"; "ru" -> "Задержка (Высокая)"; "th" -> "ความหน่วง (สูง)"; "id" -> "Latensi (Tinggi)"
                else -> en
            }
            "Latency (Unstable)", "Unstable" -> return when (lang) {
                "zh" -> "网络波动"; "ja" -> "不安定"; "ko" -> "네트워크 불안정"; "es" -> "Inestable"; "pt" -> "Instável"
                "fr" -> "Instable"; "de" -> "Instabil"; "ru" -> "Нестабильно"; "th" -> "ไม่เสถียร"; "id" -> "Tidak stabil"
                else -> en
            }
            "⚡ Smart Server Selection" -> return when (lang) {
                "zh" -> "⚡ 智能节点选择算法"; "ja" -> "⚡ スマートサーバー選択"; "ko" -> "⚡ 스마트 서버 자동 선택"; "es" -> "⚡ Selección inteligente"; "pt" -> "⚡ Seleção inteligente"
                "fr" -> "⚡ Sélection intelligente"; "de" -> "⚡ Intelligente Serverwahl"; "ru" -> "⚡ Умный выбор сервера"; "th" -> "⚡ เลือกเซิร์ฟเวอร์อัจฉริยะ"; "id" -> "⚡ Pilihan Server Pintar"
                else -> en
            }
            "Session ➔ Unlock" -> return when (lang) {
                "zh" -> "游玩延迟 ➔ 解锁"; "ja" -> "プレイ遅延 ➔ 解除"; "ko" -> "세션 지연 ➔ 잠금 해제"; "es" -> "Sesión ➔ Desbloqueo"; "pt" -> "Sessão ➔ Desbloqueio"
                "fr" -> "Session ➔ Déblocage"; "de" -> "Sitzung ➔ Freischaltung"; "ru" -> "Сессия ➔ Разблок."; "th" -> "ปิงในเกม ➔ ปลดล็อก"; "id" -> "Sesi ➔ Buka Kunci"
                else -> en
            }
            "VPN Ping ➔ Session" -> return when (lang) {
                "zh" -> "VPN 延迟 ➔ 游玩"; "ja" -> "VPN遅延 ➔ プレイ"; "ko" -> "VPN 핑 ➔ 세션 지연"; "es" -> "Ping VPN ➔ Sesión"; "pt" -> "Ping VPN ➔ Sessão"
                "fr" -> "Ping VPN ➔ Session"; "de" -> "VPN-Ping ➔ Sitzung"; "ru" -> "Пинг VPN ➔ Сессия"; "th" -> "ปิง VPN ➔ ในเกม"; "id" -> "Ping VPN ➔ Sesi"
                else -> en
            }
            "Auto-Off VPN in game", "Auto-Off VPN" -> return when (lang) {
                "zh" -> "自动关 VPN"; "ja" -> "VPN自動オフ"; "ko" -> "VPN 자동 끄기"; "es" -> "Apagar VPN"; "pt" -> "Desligar VPN"
                "fr" -> "Couper VPN"; "de" -> "VPN auto-aus"; "ru" -> "Авто-откл. VPN"; "th" -> "ปิด VPN อัตโนมัติ"; "id" -> "Matikan VPN"
                else -> en
            }
            "75% Session • 25% Unlock" -> return when (lang) {
                "zh" -> "75% 游玩延迟 • 25% 解锁"; "ja" -> "75% プレイ遅延 • 25% 解除"; "ko" -> "75% 세션 지연 • 25% 잠금 해제"; "es" -> "75% Sesión • 25% Desbloqueo"; "pt" -> "75% Sessão • 25% Desbloqueio"
                "fr" -> "75% Session • 25% Déblocage"; "de" -> "75% Sitzung • 25% Freisch."; "ru" -> "75% Сессия • 25% Разблок."; "th" -> "75% ปิงในเกม • 25% ปลดล็อก"; "id" -> "75% Sesi • 25% Buka Kunci"
                else -> en
            }
            "Keep VPN on in game", "Keep VPN on" -> return when (lang) {
                "zh" -> "保持开启 VPN"; "ja" -> "VPNを維持"; "ko" -> "VPN 계속 유지"; "es" -> "Mantener VPN"; "pt" -> "Manter VPN"
                "fr" -> "Garder VPN"; "de" -> "VPN anlassen"; "ru" -> "Держать VPN"; "th" -> "เปิด VPN ตลอด"; "id" -> "Tetap VPN"
                else -> en
            }
            "75% VPN Ping • 25% Session" -> return when (lang) {
                "zh" -> "75% VPN 延迟 • 25% 游玩"; "ja" -> "75% VPN遅延 • 25% プレイ"; "ko" -> "75% VPN 핑 • 25% 세션 지연"; "es" -> "75% Ping VPN • 25% Sesión"; "pt" -> "75% Ping VPN • 25% Sessão"
                "fr" -> "75% Ping VPN • 25% Session"; "de" -> "75% VPN-Ping • 25% Sitzung"; "ru" -> "75% Пинг VPN • 25% Сессия"; "th" -> "75% ปิง VPN • 25% ในเกม"; "id" -> "75% Ping VPN • 25% Sesi"
                else -> en
            }
            "✓ Hiding dead proxies", "Hiding dead dynamic" -> return when (lang) {
                "zh" -> "✓ 已隐藏失效节点"; "ja" -> "✓ 応答なしを非表示中"; "ko" -> "✓ 응답 없는 프록시 숨김"; "es" -> "✓ Ocultando inactivos"; "pt" -> "✓ Ocultando inativos"
                "fr" -> "✓ Inactifs masqués"; "de" -> "✓ Tote Proxys ausgeblendet"; "ru" -> "✓ Недоступные скрыты"; "th" -> "✓ ซ่อนพร็อกซีที่เสียแล้ว"; "id" -> "✓ Sembunyikan proxy mati"
                else -> en
            }
            "Hide dead proxies", "Hide dead dynamic" -> return when (lang) {
                "zh" -> "隐藏失效节点"; "ja" -> "応答なしを非表示"; "ko" -> "응답 없는 프록시 숨기기"; "es" -> "Ocultar inactivos"; "pt" -> "Ocultar inativos"
                "fr" -> "Masquer inactifs"; "de" -> "Tote Proxys ausblenden"; "ru" -> "Скрыть недоступные"; "th" -> "ซ่อนพร็อกซีที่เสีย"; "id" -> "Sembunyikan proxy mati"
                else -> en
            }
            "Refresh Public List" -> return when (lang) {
                "zh" -> "刷新公共列表"; "ja" -> "公開リスト更新"; "ko" -> "공개 목록 새로고침"; "es" -> "Actualizar lista"; "pt" -> "Atualizar lista"
                "fr" -> "Actualiser la liste"; "de" -> "Liste aktualisieren"; "ru" -> "Обновить список"; "th" -> "อัปเดตรายชื่อ"; "id" -> "Segarkan Daftar"
                else -> en
            }
            "Add Custom Proxy" -> return when (lang) {
                "zh" -> "添加自定义代理"; "ja" -> "カスタムプロキシ追加"; "ko" -> "사용자 지정 프록시 추가"; "es" -> "Añadir proxy"; "pt" -> "Adicionar proxy"
                "fr" -> "Ajouter un proxy"; "de" -> "Eigenen Proxy hinzufügen"; "ru" -> "Добавить прокси"; "th" -> "เพิ่มพร็อกซีส่วนตัว"; "id" -> "Tambah Proxy Kustom"
                else -> en
            }
            // --- MÀN HÌNH CÀI ĐẶT (SETTINGS SCREEN) ---
            "Resolution, Graphics & FPS" -> return when (lang) {
                "zh" -> "分辨率、画质与帧率 (FPS)"; "ja" -> "解像度・画質・FPS"; "ko" -> "해상도, 그래픽 및 FPS"; "es" -> "Resolución, Gráficos y FPS"; "pt" -> "Resolução, Gráficos e FPS"
                "fr" -> "Résolution, Graphismes et FPS"; "de" -> "Auflösung, Grafik & FPS"; "ru" -> "Разрешение, Графика и FPS"; "th" -> "ความละเอียด กราฟิก และ FPS"; "id" -> "Resolusi, Grafik & FPS"
                else -> en
            }
            "Resolution & Bitrate (Wi-Fi / 4G / 5G)" -> return when (lang) {
                "zh" -> "分辨率与码率 (Wi-Fi / 4G / 5G)"; "ja" -> "解像度とビットレート (Wi-Fi / 4G / 5G)"; "ko" -> "해상도 및 비트레이트 (Wi-Fi / 4G / 5G)"; "es" -> "Resolución y Bitrate (Wi-Fi / 4G / 5G)"; "pt" -> "Resolução e Bitrate (Wi-Fi / 4G / 5G)"
                "fr" -> "Résolution et Débit (Wi-Fi / 4G / 5G)"; "de" -> "Auflösung & Bitrate (Wi-Fi / 4G / 5G)"; "ru" -> "Разрешение и битрейт (Wi-Fi / 4G / 5G)"; "th" -> "ความละเอียดและบิตเรต (Wi-Fi / 4G / 5G)"; "id" -> "Resolusi & Bitrate (Wi-Fi / 4G / 5G)"
                else -> en
            }
            "Auto • 720p ⇄ 1080p (Recommended)" -> return when (lang) {
                "zh" -> "自动 • 720p ⇄ 1080p (推荐)"; "ja" -> "自動 • 720p ⇄ 1080p (推奨)"; "ko" -> "자동 • 720p ⇄ 1080p (권장)"; "es" -> "Auto • 720p ⇄ 1080p (Recomendado)"; "pt" -> "Auto • 720p ⇄ 1080p (Recomendado)"
                "fr" -> "Auto • 720p ⇄ 1080p (Recommandé)"; "de" -> "Auto • 720p ⇄ 1080p (Empfohlen)"; "ru" -> "Авто • 720p ⇄ 1080p (Рекомендуется)"; "th" -> "อัตโนมัติ • 720p ⇄ 1080p (แนะนำ)"; "id" -> "Otomatis • 720p ⇄ 1080p (Disarankan)"
                else -> en
            }
            "Auto-lowers resolution to cut lag on 4G, 5G & Wi-Fi" -> return when (lang) {
                "zh" -> "在 4G、5G 和 Wi-Fi 下自动降低分辨率以减少卡顿"; "ja" -> "4G/5G/Wi-Fiで遅延を減らすため解像度を自動調整"; "ko" -> "4G, 5G 및 Wi-Fi에서 렉을 줄이기 위해 해상도 자동 조절"; "es" -> "Baja la resolución automáticamente para reducir lag"; "pt" -> "Reduz a resolução automaticamente para cortar lag"
                "fr" -> "Réduit automatiquement la résolution pour éviter le lag"; "de" -> "Senkt Auflösung automatisch gegen Lag bei 4G/5G & Wi-Fi"; "ru" -> "Авто-снижение разрешения для уменьшения лагов"; "th" -> "ลดความละเอียดอัตโนมัติเพื่อลดอาการแล็ก"; "id" -> "Otomatis turunkan resolusi untuk kurangi lag"
                else -> en
            }
            "Frame Rate (FPS)" -> return when (lang) {
                "zh" -> "帧率上限 (FPS)"; "ja" -> "フレームレート (FPS)"; "ko" -> "프레임 속도 (FPS)"; "es" -> "Cuadros por segundo (FPS)"; "pt" -> "Taxa de Quadros (FPS)"
                "fr" -> "Fréquence d'images (FPS)"; "de" -> "Bildrate (FPS)"; "ru" -> "Частота кадров (FPS)"; "th" -> "เฟรมเรต (FPS)"; "id" -> "Frame Rate (FPS)"
                else -> en
            }
            "Low-End & Battery Saver Mode" -> return when (lang) {
                "zh" -> "低配设备与省电模式"; "ja" -> "軽量・省電力モード"; "ko" -> "저사양 및 배터리 절약 모드"; "es" -> "Modo ahorro de batería y gama baja"; "pt" -> "Modo Economia de Bateria e Leve"
                "fr" -> "Mode Économie d'énergie"; "de" -> "Energiespar- & Leichtmodus"; "ru" -> "Режим экономии заряда и слабых ПК"; "th" -> "โหมดเครื่องสเปกต่ำและประหยัดแบต"; "id" -> "Mode Hemat Baterai & Perangkat Ringan"
                else -> en
            }
            "Light 720p stream & reduced effects to keep phone cool" -> return when (lang) {
                "zh" -> "使用轻量 720p 串流并减少特效，保持手机清凉省电"; "ja" -> "軽量720pとエフェクト軽減で発熱と電池消費を抑えます"; "ko" -> "가벼운 720p 스트리밍 및 효과 감소로 발열과 배터리 절약"; "es" -> "Stream 720p ligero y menos efectos para enfriar el móvil"; "pt" -> "Stream 720p leve e menos efeitos para manter o celular frio"
                "fr" -> "Flux 720p léger et effets réduits contre la chauffe"; "de" -> "Leichter 720p-Stream & weniger Effekte gegen Überhitzung"; "ru" -> "Лёгкий поток 720p и меньше эффектов от нагрева"; "th" -> "ใช้สตรีม 720p เบาๆ และลดเอฟเฟกต์ช่วยให้เครื่องเย็น"; "id" -> "Stream 720p ringan & kurangi efek agar ponsel tetap dingin"
                else -> en
            }
            "Sharpness & Image Colors" -> return when (lang) {
                "zh" -> "画面锐度与色彩调节"; "ja" -> "シャープネスと色彩調整"; "ko" -> "선명도 및 화면 색상"; "es" -> "Nitidez y Colores de Imagen"; "pt" -> "Nitidez e Cores da Imagem"
                "fr" -> "Netteté et Couleurs de l'image"; "de" -> "Schärfe & Bildfarben"; "ru" -> "Резкость и цвет изображения"; "th" -> "ความคมชัดและสีสันภาพ"; "id" -> "Ketajaman & Warna Gambar"
                else -> en
            }
            "Sharpen Image" -> return when (lang) {
                "zh" -> "增强画面锐度"; "ja" -> "映像をくっきり表示"; "ko" -> "화면 선명도 향상"; "es" -> "Aumentar nitidez"; "pt" -> "Aumentar nitidez"
                "fr" -> "Améliorer la netteté"; "de" -> "Bild schärfen"; "ru" -> "Повысить резкость"; "th" -> "เพิ่มความคมชัดของภาพ"; "id" -> "Pertajam Gambar"
                else -> en
            }
            "Makes characters and scenery clearer" -> return when (lang) {
                "zh" -> "让角色细节与远景画面更清晰"; "ja" -> "キャラクターや背景の輪郭をより鮮明にします"; "ko" -> "캐릭터와 배경 디테일을 더 뚜렷하게 표시합니다"; "es" -> "Hace más nítidos los personajes y escenarios"; "pt" -> "Torna personagens e cenários mais nítidos"
                "fr" -> "Rend les personnages et décors plus nets"; "de" -> "Macht Charaktere und Landschaften schärfer"; "ru" -> "Делает персонажей и окружение чётче"; "th" -> "ทำให้ตัวละครและฉากคมชัดขึ้น"; "id" -> "Membuat karakter dan pemandangan lebih jelas"
                else -> en
            }
            "Contrast" -> return when (lang) {
                "zh" -> "对比度"; "ja" -> "コントラスト"; "ko" -> "대비 (Contrast)"; "es" -> "Contraste"; "pt" -> "Contraste"
                "fr" -> "Contraste"; "de" -> "Kontrast"; "ru" -> "Контрастность"; "th" -> "ความคมชัดลึก (Contrast)"; "id" -> "Kontras"
                else -> en
            }
            "Color Saturation" -> return when (lang) {
                "zh" -> "色彩饱和度"; "ja" -> "彩度 (色の鮮やかさ)"; "ko" -> "채도 (색감)"; "es" -> "Saturación de color"; "pt" -> "Saturação de cor"
                "fr" -> "Saturation des couleurs"; "de" -> "Farbsättigung"; "ru" -> "Насыщенность цвета"; "th" -> "ความสดของสี"; "id" -> "Saturasi Warna"
                else -> en
            }
            "Brightness" -> return when (lang) {
                "zh" -> "画面亮度"; "ja" -> "明るさ"; "ko" -> "밝기"; "es" -> "Brillo"; "pt" -> "Brilho"
                "fr" -> "Luminosité"; "de" -> "Helligkeit"; "ru" -> "Яркость"; "th" -> "ความสว่าง"; "id" -> "Kecerahan"
                else -> en
            }
            "Reset Colors to Default" -> return when (lang) {
                "zh" -> "恢复默认色彩"; "ja" -> "色をデフォルトに戻す"; "ko" -> "색상 기본값으로 초기화"; "es" -> "Restablecer colores"; "pt" -> "Restaurar cores padrão"
                "fr" -> "Réinitialiser les couleurs"; "de" -> "Farben zurücksetzen"; "ru" -> "Сбросить цвета по умолчанию"; "th" -> "รีเซ็ตสีเป็นค่าเริ่มต้น"; "id" -> "Reset Warna ke Default"
                else -> en
            }
            "Network & Region Unlock" -> return when (lang) {
                "zh" -> "网络与跨区解锁"; "ja" -> "ネットワークと地域ロック解除"; "ko" -> "네트워크 및 지역 잠금 해제"; "es" -> "Red y Desbloqueo de Región"; "pt" -> "Rede e Desbloqueio de Região"
                "fr" -> "Réseau et Déblocage de Région"; "de" -> "Netzwerk & Regionsfreischaltung"; "ru" -> "Сеть и разблокировка региона"; "th" -> "เครือข่ายและปลดล็อกโซน"; "id" -> "Jaringan & Buka Kunci Wilayah"
                else -> en
            }
            "Auto VPN & Server Selection" -> return when (lang) {
                "zh" -> "自动 VPN 与服务器选择"; "ja" -> "自動VPNとサーバー選択"; "ko" -> "자동 VPN 및 서버 선택"; "es" -> "VPN Automático y Servidor"; "pt" -> "VPN Automático e Servidor"
                "fr" -> "VPN Auto et Choix du Serveur"; "de" -> "Auto-VPN & Serverauswahl"; "ru" -> "Авто-VPN и выбор сервера"; "th" -> "VPN อัตโนมัติและเลือกเซิร์ฟเวอร์"; "id" -> "VPN Otomatis & Pilihan Server"
                else -> en
            }
            "Auto VPN (Unlock on launch ➔ Turn off in-game)" -> return when (lang) {
                "zh" -> "自动 VPN (启动游戏时解锁 ➔ 进入后自动关闭)"; "ja" -> "自動VPN (起動時に解除 ➔ ゲーム開始後にオフ)"; "ko" -> "자동 VPN (실행 시 잠금 해제 ➔ 게임 진입 후 자동 끄기)"; "es" -> "VPN Auto (Desbloquear al abrir ➔ Apagar en juego)"; "pt" -> "VPN Auto (Desbloquear ao abrir ➔ Desligar no jogo)"
                "fr" -> "VPN Auto (Débloquer au lancement ➔ Couper en jeu)"; "de" -> "Auto-VPN (Beim Start entsperren ➔ Im Spiel ausschalten)"; "ru" -> "Авто-VPN (Разблок. при запуске ➔ Выкл. в игре)"; "th" -> "VPN อัตโนมัติ (เปิดตอนเข้าเกม ➔ ปิดเองเมื่อเล่น)"; "id" -> "VPN Otomatis (Buka saat mulai ➔ Matikan di game)"
                else -> en
            }
            "Connects VPN to open the game, then switches back to direct network" -> return when (lang) {
                "zh" -> "启动游戏时连接 VPN 跨区验证，进入画面后切回直连网络以降低延迟"; "ja" -> "ゲーム起動時のみVPNで地域認証し、開始後は直連通信に切り替えます"; "ko" -> "게임 실행 시 VPN으로 지역 인증 후 직접 연결로 전환하여 핑을 낮춥니다"; "es" -> "Conecta VPN para abrir el juego y luego vuelve a red directa"; "pt" -> "Conecta a VPN para abrir o jogo e volta para a rede direta"
                "fr" -> "Active le VPN au lancement puis repasse en connexion directe"; "de" -> "Verbindet VPN zum Spielstart und wechselt dann auf Direktverbindung"; "ru" -> "Включает VPN для запуска игры, затем переключает на прямую сеть"; "th" -> "ต่อ VPN เพื่อผ่านการตรวจโซนแล้วสลับกลับเป็นเน็ตตรง"; "id" -> "Hubungkan VPN saat membuka game lalu kembali ke jaringan langsung"
                else -> en
            }
            "VPN disconnect delay" -> return when (lang) {
                "zh" -> "关闭 VPN 延迟时间"; "ja" -> "VPN切断までの待機時間"; "ko" -> "VPN 자동 해제 대기 시간"; "es" -> "Retraso al desconectar VPN"; "pt" -> "Atraso para desligar VPN"
                "fr" -> "Délai de coupure VPN"; "de" -> "VPN-Trennverzögerung"; "ru" -> "Задержка отключения VPN"; "th" -> "เวลาหน่วงก่อนปิด VPN"; "id" -> "Jeda putus VPN"
                else -> en
            }
            "Allow public server auto-sync on mobile data" -> return when (lang) {
                "zh" -> "允许使用移动数据自动同步公共节点"; "ja" -> "モバイル通信での公開サーバー自動同期を許可"; "ko" -> "모바일 데이터(4G/5G)에서 공개 서버 자동 동기화 허용"; "es" -> "Sincronizar servidores públicos con datos móviles"; "pt" -> "Sincronizar servidores públicos nos dados móveis"
                "fr" -> "Synchroniser les serveurs publics sur données mobiles"; "de" -> "Server-Synchronisierung über mobile Daten erlauben"; "ru" -> "Авто-синхронизация серверов по мобильной сети"; "th" -> "อนุญาตซิงค์เซิร์ฟเวอร์สาธารณะผ่านเน็ตมือถือ"; "id" -> "Izinkan sinkronisasi server publik via data seluler"
                else -> en
            }
            "Region Unlock Mode" -> return when (lang) {
                "zh" -> "地区解锁模式"; "ja" -> "地域ロック解除モード"; "ko" -> "지역 잠금 해제 모드"; "es" -> "Modo de Desbloqueo Regional"; "pt" -> "Modo de Desbloqueio Regional"
                "fr" -> "Mode de Déblocage Régional"; "de" -> "Regionsfreischaltungs-Modus"; "ru" -> "Режим разблокировки региона"; "th" -> "โหมดปลดล็อกโซน"; "id" -> "Mode Buka Kunci Wilayah"
                else -> en
            }
            "Unlock on Launch Only 🇯🇵 (Recommended)" -> return when (lang) {
                "zh" -> "仅在启动游戏时解锁 🇯🇵 (推荐)"; "ja" -> "ゲーム起動時のみ解除 🇯🇵 (推奨)"; "ko" -> "게임 시작 시에만 잠금 해제 🇯🇵 (권장)"; "es" -> "Desbloquear solo al iniciar 🇯🇵 (Recomendado)"; "pt" -> "Desbloquear só ao iniciar 🇯🇵 (Recomendado)"
                "fr" -> "Débloquer au lancement uniquement 🇯🇵 (Recommandé)"; "de" -> "Nur beim Spielstart entsperren 🇯🇵 (Empfohlen)"; "ru" -> "Разблокировка только при запуске 🇯🇵 (Рекомендуется)"; "th" -> "ปลดล็อกเฉพาะตอนเริ่มเกม 🇯🇵 (แนะนำ)"; "id" -> "Buka Kunci Saat Mulai Saja 🇯🇵 (Disarankan)"
                else -> en
            }
            "Uses proxy only during game launch" -> return when (lang) {
                "zh" -> "仅在启动游戏时经过代理节点"; "ja" -> "ゲーム起動時のみプロキシサーバーを経由します"; "ko" -> "게임을 처음 열 때만 프록시 서버를 거칩니다"; "es" -> "Usa proxy solo durante el inicio del juego"; "pt" -> "Usa proxy apenas durante a abertura do jogo"
                "fr" -> "Utilise le proxy uniquement à l'ouverture du jeu"; "de" -> "Nutzt Proxy nur beim Starten des Spiels"; "ru" -> "Использует прокси только при запуске игры"; "th" -> "ผ่านเซิร์ฟเวอร์กลางเฉพาะตอนเริ่มเปิดเกม"; "id" -> "Gunakan proxy hanya saat membuka game"
                else -> en
            }
            "Intermediate Server 🌐" -> return when (lang) {
                "zh" -> "中转代理服务器 🌐"; "ja" -> "中継サーバー経由 🌐"; "ko" -> "중계 서버 경유 🌐"; "es" -> "Servidor Intermedio 🌐"; "pt" -> "Servidor Intermediário 🌐"
                "fr" -> "Serveur Intermédiaire 🌐"; "de" -> "Zwischenserver 🌐"; "ru" -> "Промежуточный сервер 🌐"; "th" -> "เซิร์ฟเวอร์ตัวกลาง 🌐"; "id" -> "Server Perantara 🌐"
                else -> en
            }
            "Connects via JP/KR/US servers" -> return when (lang) {
                "zh" -> "通过日本 / 韩国 / 美国节点连接"; "ja" -> "日本・韓国・米国サーバーを経由して接続"; "ko" -> "일본 / 한국 / 미국 서버를 통해 연결"; "es" -> "Conecta mediante servidores JP/KR/US"; "pt" -> "Conecta via servidores JP/KR/US"
                "fr" -> "Connexion via serveurs JP/KR/US"; "de" -> "Verbindet über JP/KR/US-Server"; "ru" -> "Подключение через серверы JP/KR/US"; "th" -> "เชื่อมต่อผ่านเซิร์ฟเวอร์ ญี่ปุ่น / เกาหลี / สหรัฐฯ"; "id" -> "Terhubung lewat server JP/KR/US"
                else -> en
            }
            "External VPN App" -> return when (lang) {
                "zh" -> "外部 VPN 应用"; "ja" -> "外部VPNアプリを使用"; "ko" -> "외부 VPN 앱 사용"; "es" -> "App VPN Externa"; "pt" -> "App VPN Externo"
                "fr" -> "Application VPN Externe"; "de" -> "Externe VPN-App"; "ru" -> "Внешнее VPN-приложение"; "th" -> "แอป VPN ภายนอก"; "id" -> "Aplikasi VPN Eksternal"
                else -> en
            }
            "Use with your own VPN app" -> return when (lang) {
                "zh" -> "配合手机上的第三方 VPN 使用"; "ja" -> "端末内の別VPNアプリと併用します"; "ko" -> "기기에 설치된 별도 VPN 앱과 함께 사용"; "es" -> "Usar con tu propia aplicación VPN"; "pt" -> "Usar com seu próprio app de VPN"
                "fr" -> "Utiliser avec votre propre application VPN"; "de" -> "Mit eigener VPN-App auf dem Gerät nutzen"; "ru" -> "Использовать со своим VPN-приложением"; "th" -> "ใช้ร่วมกับแอป VPN แยกในเครื่อง"; "id" -> "Gunakan bersama aplikasi VPN Anda sendiri"
                else -> en
            }
            "Gamepad & Touch Controls" -> return when (lang) {
                "zh" -> "手柄与触控设置"; "ja" -> "コントローラーとタッチ操作"; "ko" -> "게임패드 및 터치 컨트롤"; "es" -> "Mando y Controles Táctiles"; "pt" -> "Controle e Comandos de Toque"
                "fr" -> "Manette et Commandes Tactiles"; "de" -> "Gamepad & Touch-Steuerung"; "ru" -> "Геймпад и сенсорное управление"; "th" -> "จอยเกมและปุ่มสัมผัส"; "id" -> "Gamepad & Kontrol Sentuh"
                else -> en
            }
            "Virtual Gamepad: ON" -> return when (lang) {
                "zh" -> "虚拟手柄: 已开启"; "ja" -> "仮想パッド: オン"; "ko" -> "가상 패드: 켜짐"; "es" -> "Mando virtual: ACTIVADO"; "pt" -> "Controle virtual: LIGADO"
                "fr" -> "Manette virtuelle : ACTIVÉE"; "de" -> "Virtuelles Gamepad: EIN"; "ru" -> "Виртуальный геймпад: ВКЛ"; "th" -> "จอยจำลอง: เปิดอยู่"; "id" -> "Gamepad Virtual: AKTIF"
                else -> en
            }
            "Virtual Gamepad: OFF" -> return when (lang) {
                "zh" -> "虚拟手柄: 已关闭"; "ja" -> "仮想パッド: オフ"; "ko" -> "가상 패드: 꺼짐"; "es" -> "Mando virtual: DESACTIVADO"; "pt" -> "Controle virtual: DESLIGADO"
                "fr" -> "Manette virtuelle : DÉSACTIVÉE"; "de" -> "Virtuelles Gamepad: AUS"; "ru" -> "Виртуальный геймпад: ВЫКЛ"; "th" -> "จอยจำลอง: ปิดอยู่"; "id" -> "Gamepad Virtual: MATI"
                else -> en
            }
            "Touch Gamepad" -> return when (lang) {
                "zh" -> "屏幕虚拟手柄"; "ja" -> "画面上の仮想コントローラー"; "ko" -> "화면 가상 게임패드"; "es" -> "Mando Táctil en Pantalla"; "pt" -> "Controle Virtual na Tela"
                "fr" -> "Manette Tactile à l'écran"; "de" -> "Touch-Gamepad auf dem Bildschirm"; "ru" -> "Экранный сенсорный геймпад"; "th" -> "จอยจำลองบนหน้าจอ"; "id" -> "Gamepad Sentuh di Layar"
                else -> en
            }
            "Show Virtual Gamepad" -> return when (lang) {
                "zh" -> "显示虚拟手柄"; "ja" -> "仮想コントローラーを表示"; "ko" -> "가상 게임패드 표시"; "es" -> "Mostrar mando virtual"; "pt" -> "Mostrar controle virtual"
                "fr" -> "Afficher la manette virtuelle"; "de" -> "Virtuelles Gamepad anzeigen"; "ru" -> "Показать виртуальный геймпад"; "th" -> "เปิดจอยจำลองบนหน้าจอ"; "id" -> "Tampilkan Gamepad Virtual"
                else -> en
            }
            "Auto-Hide Virtual Gamepad on Physical Controller" -> return when (lang) {
                "zh" -> "连接实体手柄时自动隐藏虚拟按键"; "ja" -> "物理コントローラー接続時に自動非表示"; "ko" -> "실제 컨트롤러 연결 시 가상 패드 자동 숨김"; "es" -> "Ocultar mando virtual al conectar mando físico"; "pt" -> "Ocultar controle virtual ao conectar controle físico"
                "fr" -> "Masquer la manette virtuelle si manette physique"; "de" -> "Virtuelles Gamepad bei physischem Controller ausblenden"; "ru" -> "Скрывать виртуальный геймпад при подключении физического"; "th" -> "ซ่อนจอยจำลองอัตโนมัติเมื่อต่อจอยจริง"; "id" -> "Sembunyikan Gamepad Virtual saat Kontroler Fisik Terhubung"
                else -> en
            }
            "Save Layout Per Game" -> return when (lang) {
                "zh" -> "按游戏分别保存按键布局"; "ja" -> "ゲームごとに配置を保存"; "ko" -> "게임별 버튼 배치 개별 저장"; "es" -> "Guardar diseño por cada juego"; "pt" -> "Salvar layout por jogo"
                "fr" -> "Enregistrer la disposition par jeu"; "de" -> "Layout pro Spiel speichern"; "ru" -> "Сохранять раскладку для каждой игры"; "th" -> "บันทึกผังปุ่มแยกตามแต่ละเกม"; "id" -> "Simpan Tata Letak per Game"
                else -> en
            }
            "✓ Using Global Layout" -> return when (lang) {
                "zh" -> "✓ 正在使用全局布局"; "ja" -> "✓ 共通配置を使用中"; "ko" -> "✓ 공통 배치 사용 중"; "es" -> "✓ Usando diseño global"; "pt" -> "✓ Usando layout global"
                "fr" -> "✓ Disposition globale active"; "de" -> "✓ Globales Layout aktiv"; "ru" -> "✓ Общая раскладка активна"; "th" -> "✓ กำลังใช้ผังปุ่มร่วมกัน"; "id" -> "✓ Menggunakan Tata Letak Global"
                else -> en
            }
            "Use Global Layout" -> return when (lang) {
                "zh" -> "使用全局通用布局"; "ja" -> "共通の配置を使用"; "ko" -> "공통 배치 사용"; "es" -> "Usar diseño global"; "pt" -> "Usar layout global"
                "fr" -> "Utiliser la disposition globale"; "de" -> "Globales Layout nutzen"; "ru" -> "Использовать общую раскладку"; "th" -> "ใช้ผังปุ่มร่วมกัน"; "id" -> "Gunakan Tata Letak Global"
                else -> en
            }
            "Reset" -> return when (lang) {
                "zh" -> "重置"; "ja" -> "リセット"; "ko" -> "초기화"; "es" -> "Restablecer"; "pt" -> "Redefinir"
                "fr" -> "Réinitialiser"; "de" -> "Zurücksetzen"; "ru" -> "Сброс"; "th" -> "รีเซ็ต"; "id" -> "Reset"
                else -> en
            }
            "Button Layout" -> return when (lang) {
                "zh" -> "按键预设布局"; "ja" -> "ボタン配置プリセット"; "ko" -> "버튼 배치 프리셋"; "es" -> "Diseño de botones"; "pt" -> "Layout dos botões"
                "fr" -> "Disposition des boutons"; "de" -> "Tasten-Layout"; "ru" -> "Раскладка кнопок"; "th" -> "รูปแบบการจัดปุ่ม"; "id" -> "Tata Letak Tombol"
                else -> en
            }
            "Button Size" -> return when (lang) {
                "zh" -> "按键大小"; "ja" -> "ボタンサイズ"; "ko" -> "버튼 크기"; "es" -> "Tamaño de botones"; "pt" -> "Tamanho dos botões"
                "fr" -> "Taille des boutons"; "de" -> "Tastengröße"; "ru" -> "Размер кнопок"; "th" -> "ขนาดปุ่ม"; "id" -> "Ukuran Tombol"
                else -> en
            }
            "Opacity" -> return when (lang) {
                "zh" -> "按键透明度"; "ja" -> "不透明度"; "ko" -> "투명도"; "es" -> "Opacidad"; "pt" -> "Opacidade"
                "fr" -> "Opacité"; "de" -> "Deckkraft"; "ru" -> "Прозрачность"; "th" -> "ความทึบแสง"; "id" -> "Transparansi"
                else -> en
            }
            "Stick Sensitivity" -> return when (lang) {
                "zh" -> "摇杆灵敏度"; "ja" -> "スティック感度"; "ko" -> "스틱 감도"; "es" -> "Sensibilidad del stick"; "pt" -> "Sensibilidade do analógico"
                "fr" -> "Sensibilité du stick"; "de" -> "Stick-Empfindlichkeit"; "ru" -> "Чувствительность стиков"; "th" -> "ความไวก้านอนาล็อก"; "id" -> "Sensitivitas Analog"
                else -> en
            }
            "Language, Auto-Translate & System" -> return when (lang) {
                "zh" -> "语言、自动翻译与系统"; "ja" -> "言語・自動翻訳・システム"; "ko" -> "언어, 자동 번역 및 시스템"; "es" -> "Idioma, Traducción y Sistema"; "pt" -> "Idioma, Tradução e Sistema"
                "fr" -> "Langue, Traduction et Système"; "de" -> "Sprache, Auto-Übersetzung & System"; "ru" -> "Язык, автоперевод и система"; "th" -> "ภาษา แปลอัตโนมัติ และระบบ"; "id" -> "Bahasa, Terjemahan & Sistem"
                else -> en
            }
            "12 World Languages, Translator, Account" -> return when (lang) {
                "zh" -> "12 种国际语言、翻译器与账号"; "ja" -> "12言語対応・翻訳・アカウント"; "ko" -> "12개국 언어, 번역기 및 계정"; "es" -> "12 idiomas, Traductor y Cuenta"; "pt" -> "12 idiomas, Tradutor e Conta"
                "fr" -> "12 langues, Traducteur et Compte"; "de" -> "12 Weltsprachen, Übersetzer & Konto"; "ru" -> "12 языков, переводчик и аккаунт"; "th" -> "12 ภาษา ตัวแปล และบัญชี"; "id" -> "12 Bahasa, Penerjemah & Akun"
                else -> en
            }
            "Xbox Menu Translator" -> return when (lang) {
                "zh" -> "Xbox 界面自动翻译"; "ja" -> "Xbox メニュー翻訳"; "ko" -> "Xbox 메뉴 자동 번역"; "es" -> "Traductor de Menú Xbox"; "pt" -> "Tradutor do Menu Xbox"
                "fr" -> "Traducteur du Menu Xbox"; "de" -> "Xbox-Menü-Übersetzer"; "ru" -> "Переводчик меню Xbox"; "th" -> "แปลเมนูหน้าเว็บ Xbox"; "id" -> "Penerjemah Menu Xbox"
                else -> en
            }
            "Auto-translate Xbox interface" -> return when (lang) {
                "zh" -> "自动翻译 Xbox 云游戏界面"; "ja" -> "Xbox インターフェースを自動翻訳"; "ko" -> "Xbox 인터페이스 자동 번역"; "es" -> "Traducir interfaz de Xbox automáticamente"; "pt" -> "Traduzir interface do Xbox automaticamente"
                "fr" -> "Traduire automatiquement l'interface Xbox"; "de" -> "Xbox-Oberfläche automatisch übersetzen"; "ru" -> "Автоматически переводить интерфейс Xbox"; "th" -> "แปลหน้าเว็บ Xbox อัตโนมัติ"; "id" -> "Terjemahkan antarmuka Xbox otomatis"
                else -> en
            }
            "Translation Target Language:" -> return when (lang) {
                "zh" -> "Xbox 界面翻译目标语言:"; "ja" -> "翻訳先の言語:"; "ko" -> "Xbox 번역 대상 언어:"; "es" -> "Idioma de destino:"; "pt" -> "Idioma de destino:"
                "fr" -> "Langue cible de traduction :"; "de" -> "Zielsprache der Übersetzung:"; "ru" -> "Язык перевода:"; "th" -> "ภาษาที่ต้องการแปล:"; "id" -> "Bahasa Target Terjemahan:"
                else -> en
            }
            "🚫 Off" -> return when (lang) {
                "zh" -> "🚫 关闭"; "ja" -> "🚫 オフ"; "ko" -> "🚫 끄기"; "es" -> "🚫 Desactivado"; "pt" -> "🚫 Desligado"
                "fr" -> "🚫 Désactivé"; "de" -> "🚫 Aus"; "ru" -> "🚫 Выкл."; "th" -> "🚫 ปิด"; "id" -> "🚫 Mati"
                else -> en
            }
            "Bilingual mode (Translated + Original)" -> return when (lang) {
                "zh" -> "双语对照模式 (译文 + 原文)"; "ja" -> "二ヶ国語表示モード (翻訳 + 原文)"; "ko" -> "이중 언어 모드 (번역 + 원문)"; "es" -> "Modo bilingüe (Traducción + Original)"; "pt" -> "Modo bilíngue (Tradução + Original)"
                "fr" -> "Mode bilingue (Traduction + Original)"; "de" -> "Zweisprachiger Modus (Übersetzung + Original)"; "ru" -> "Двуязычный режим (Перевод + Оригинал)"; "th" -> "โหมดสองภาษา (คำแปล + ต้นฉบับ)"; "id" -> "Mode Dwibahasa (Terjemahan + Asli)"
                else -> en
            }
            "Shows original text in parentheses" -> return when (lang) {
                "zh" -> "在括号内同时显示原始语言文本"; "ja" -> "括弧内に元のテキストを併記します"; "ko" -> "괄호 안에 원문을 함께 표시합니다"; "es" -> "Muestra el texto original entre paréntesis"; "pt" -> "Mostra o texto original entre parênteses"
                "fr" -> "Affiche le texte original entre parenthèses"; "de" -> "Zeigt den Originaltext in Klammern an"; "ru" -> "Показывает оригинал в скобках"; "th" -> "แสดงข้อความภาษาต้นฉบับในวงเล็บ"; "id" -> "Tampilkan teks asli di dalam kurung"
                else -> en
            }
            "App Language (Global)" -> return when (lang) {
                "zh" -> "应用界面语言 (全球)"; "ja" -> "アプリの表示言語 (グローバル)"; "ko" -> "앱 인터페이스 언어 (글로벌)"; "es" -> "Idioma de la Aplicación (Global)"; "pt" -> "Idioma do Aplicativo (Global)"
                "fr" -> "Langue de l'Application (Global)"; "de" -> "App-Sprache (Global)"; "ru" -> "Язык приложения (Глобальный)"; "th" -> "ภาษาของแอปพลิเคชัน (สากล)"; "id" -> "Bahasa Aplikasi (Global)"
                else -> en
            }
            "✓ Active" -> return when (lang) {
                "zh" -> "✓ 当前使用"; "ja" -> "✓ 使用中"; "ko" -> "✓ 현재 사용 중"; "es" -> "✓ Activo"; "pt" -> "✓ Ativo"
                "fr" -> "✓ Actif"; "de" -> "✓ Aktiv"; "ru" -> "✓ Активно"; "th" -> "✓ กำลังใช้"; "id" -> "✓ Aktif"
                else -> en
            }
            "🌐 System" -> return when (lang) {
                "zh" -> "🌐 跟随系统"; "ja" -> "🌐 システム"; "ko" -> "🌐 시스템"; "es" -> "🌐 Sistema"; "pt" -> "🌐 Sistema"
                "fr" -> "🌐 Système"; "de" -> "🌐 System"; "ru" -> "🌐 Система"; "th" -> "🌐 ตามระบบ"; "id" -> "🌐 Sistem"
                else -> en
            }
            "Xbox Cloud Storefront Market" -> return when (lang) {
                "zh" -> "Xbox 云游戏商店区域"; "ja" -> "Xbox Cloud ストア地域"; "ko" -> "Xbox 클라우드 스토어 지역"; "es" -> "Región de la Tienda Xbox Cloud"; "pt" -> "Região da Loja Xbox Cloud"
                "fr" -> "Marché de la Boutique Xbox Cloud"; "de" -> "Xbox Cloud Store-Region"; "ru" -> "Регион магазина Xbox Cloud"; "th" -> "โซนร้านค้าหน้าเว็บ Xbox Cloud"; "id" -> "Wilayah Toko Xbox Cloud"
                else -> en
            }
            "🇺🇸 Default Market (en-US • Recommended)" -> return when (lang) {
                "zh" -> "🇺🇸 默认美区市场 (en-US • 推荐)"; "ja" -> "🇺🇸 デフォルト地域 (en-US • 推奨)"; "ko" -> "🇺🇸 기본 마켓 (en-US • 권장)"; "es" -> "🇺🇸 Mercado predeterminado (en-US • Recomendado)"; "pt" -> "🇺🇸 Mercado padrão (en-US • Recomendado)"
                "fr" -> "🇺🇸 Marché par défaut (en-US • Recommandé)"; "de" -> "🇺🇸 Standard-Markt (en-US • Empfohlen)"; "ru" -> "🇺🇸 Регион по умолчанию (en-US • Рекомендуется)"; "th" -> "🇺🇸 โซนมาตรฐาน (en-US • แนะนำ)"; "id" -> "🇺🇸 Pasar Default (en-US • Disarankan)"
                else -> en
            }
            "Account, IP Geolocation & Terms" -> return when (lang) {
                "zh" -> "账号、IP 定位与使用条款"; "ja" -> "アカウント・IP位置情報・利用規約"; "ko" -> "계정, IP 위치 확인 및 이용약관"; "es" -> "Cuenta, Geolocalización IP y Términos"; "pt" -> "Conta, Geolocalização IP e Termos"
                "fr" -> "Compte, Géolocalisation IP et Conditions"; "de" -> "Konto, IP-Geolokalisierung & Bedingungen"; "ru" -> "Аккаунт, геолокация IP и условия"; "th" -> "บัญชี ระบุตำแหน่ง IP และข้อกำหนด"; "id" -> "Akun, Geolokasi IP & Ketentuan"
                else -> en
            }
            "Sign Out & Clear Login Session" -> return when (lang) {
                "zh" -> "退出登录并清除 Xbox 会话"; "ja" -> "サインアウトとログイン情報の消去"; "ko" -> "로그아웃 및 Xbox 로그인 세션 초기화"; "es" -> "Cerrar sesión y borrar datos de acceso"; "pt" -> "Sair e limpar sessão de login"
                "fr" -> "Se déconnecter et effacer la session"; "de" -> "Abmelden & Xbox-Sitzung löschen"; "ru" -> "Выйти и очистить сессию входа"; "th" -> "ออกจากระบบและล้างเซสชัน Xbox"; "id" -> "Keluar & Hapus Sesi Login Xbox"
                else -> en
            }
            "View Full Terms", "View Full Terms (Web)" -> return when (lang) {
                "zh" -> "查看完整使用条款"; "ja" -> "利用規約の全文を表示"; "ko" -> "전체 이용약관 보기"; "es" -> "Ver Términos Completos"; "pt" -> "Ver Termos Completos"
                "fr" -> "Voir les Conditions Complètes"; "de" -> "Vollständige Bedingungen anzeigen"; "ru" -> "Полные условия использования"; "th" -> "ดูข้อกำหนดการใช้งานฉบับเต็ม"; "id" -> "Lihat Ketentuan Lengkap"
                else -> en
            }
            "System Activity Log (Track Probe & App)", "📋 View App Log (Track Probe & Ping)" -> return when (lang) {
                "zh" -> "📋 查看系统日志 (追踪节点与延迟)"; "ja" -> "📋 システムログを表示 (Ping・通信履歴)"; "ko" -> "📋 시스템 로그 보기 (프로브 및 핑 추적)"; "es" -> "📋 Ver Registro del Sistema (Ping y Red)"; "pt" -> "📋 Ver Log do Sistema (Ping e Rede)"
                "fr" -> "📋 Journal d'activité système"; "de" -> "📋 Systemprotokoll anzeigen (Probe & Ping)"; "ru" -> "📋 Журнал активности системы"; "th" -> "📋 ดูบันทึกระบบ (ติดตามแอปและปิง)"; "id" -> "📋 Lihat Log Sistem (Lacak Probe & Ping)"
                else -> en
            }
            "Copy Diagnostic Report" -> return when (lang) {
                "zh" -> "复制诊断报告"; "ja" -> "診断レポートをコピー"; "ko" -> "진단 보고서 복사"; "es" -> "Copiar informe de diagnóstico"; "pt" -> "Copiar relatório de diagnóstico"
                "fr" -> "Copier le rapport de diagnostic"; "de" -> "Diagnosebericht kopieren"; "ru" -> "Скопировать отчёт диагностики"; "th" -> "คัดลอกรายงานการวินิจฉัย"; "id" -> "Salin Laporan Diagnostik"
                else -> en
            }
            "Screen Recording" -> return when (lang) {
                "zh" -> "屏幕录制"; "ja" -> "画面録画"; "ko" -> "화면 녹화"; "es" -> "Grabación de Pantalla"; "pt" -> "Gravação de Tela"
                "fr" -> "Enregistrement d'écran"; "de" -> "Bildschirmaufnahme"; "ru" -> "Запись экрана"; "th" -> "บันทึกหน้าจอ"; "id" -> "Perekaman Layar"
                else -> en
            }
            "Video Quality (.MP4)" -> return when (lang) {
                "zh" -> "视频录制画质 (.MP4)"; "ja" -> "録画品質 (.MP4)"; "ko" -> "비디오 화질 (.MP4)"; "es" -> "Calidad de Video (.MP4)"; "pt" -> "Qualidade de Vídeo (.MP4)"
                "fr" -> "Qualité Vidéo (.MP4)"; "de" -> "Videoqualität (.MP4)"; "ru" -> "Качество видео (.MP4)"; "th" -> "คุณภาพวิดีโอ (.MP4)"; "id" -> "Kualitas Video (.MP4)"
                else -> en
            }
            "Background Music" -> return when (lang) {
                "zh" -> "背景音乐"; "ja" -> "BGM (背景音楽)"; "ko" -> "배경 음악 (BGM)"; "es" -> "Música de Fondo"; "pt" -> "Música de Fundo"
                "fr" -> "Musique de Fond"; "de" -> "Hintergrundmusik"; "ru" -> "Фоновая музыка"; "th" -> "เพลงประกอบพื้นหลัง"; "id" -> "Musik Latar"
                else -> en
            }
            "Manual Playback" -> return when (lang) {
                "zh" -> "手动播放 / 停止"; "ja" -> "手動再生 / 停止"; "ko" -> "수동 재생 / 정지"; "es" -> "Reproducción manual"; "pt" -> "Reprodução manual"
                "fr" -> "Lecture manuelle"; "de" -> "Manuelle Wiedergabe"; "ru" -> "Ручное воспроизведение"; "th" -> "เล่น / หยุดเพลงด้วยตนเอง"; "id" -> "Putar / Hentikan Manual"
                else -> en
            }
            "Stopped • Tap to play manually" -> return when (lang) {
                "zh" -> "已停止 • 点击手动播放"; "ja" -> "停止中 • タップで再生"; "ko" -> "정지됨 • 탭하여 재생"; "es" -> "Detenido • Toca para reproducir"; "pt" -> "Parado • Toque para tocar"
                "fr" -> "Arrêté • Appuyez pour lire"; "de" -> "Gestoppt • Zum Abspielen tippen"; "ru" -> "Остановлено • Нажмите для запуска"; "th" -> "หยุดอยู่ • แตะเพื่อเปิดเพลง"; "id" -> "Berhenti • Ketuk untuk memutar"
                else -> en
            }
            "▶ Play" -> return when (lang) {
                "zh" -> "▶ 播放"; "ja" -> "▶ 再生"; "ko" -> "▶ 재생"; "es" -> "▶ Reproducir"; "pt" -> "▶ Tocar"
                "fr" -> "▶ Lire"; "de" -> "▶ Start"; "ru" -> "▶ Пуск"; "th" -> "▶ เล่น"; "id" -> "▶ Putar"
                else -> en
            }
            "⏹ Stop" -> return when (lang) {
                "zh" -> "⏹ 停止"; "ja" -> "⏹ 停止"; "ko" -> "⏹ 정지"; "es" -> "⏹ Detener"; "pt" -> "⏹ Parar"
                "fr" -> "⏹ Arrêter"; "de" -> "⏹ Stopp"; "ru" -> "⏹ Стоп"; "th" -> "⏹ หยุด"; "id" -> "⏹ Hentikan"
                else -> en
            }
            "Auto-start music on app launch" -> return when (lang) {
                "zh" -> "打开应用时自动播放背景音乐"; "ja" -> "アプリ起動時にBGMを自動再生"; "ko" -> "앱 실행 시 배경 음악 자동 재생"; "es" -> "Iniciar música al abrir la app"; "pt" -> "Iniciar música ao abrir o app"
                "fr" -> "Lancer la musique à l'ouverture"; "de" -> "Musik beim App-Start automatisch abspielen"; "ru" -> "Автозапуск музыки при входе"; "th" -> "เปิดเพลงอัตโนมัติเมื่อเข้าแอป"; "id" -> "Putar musik otomatis saat buka aplikasi"
                else -> en
            }
            "Play music while in game" -> return when (lang) {
                "zh" -> "游玩时继续播放背景音乐"; "ja" -> "ゲームプレイ中もBGMを再生"; "ko" -> "게임 플레이 중에도 배경 음악 유지"; "es" -> "Reproducir música durante el juego"; "pt" -> "Tocar música durante o jogo"
                "fr" -> "Jouer la musique pendant le jeu"; "de" -> "Musik während des Spiels abspielen"; "ru" -> "Играть музыку во время игры"; "th" -> "เล่นเพลงต่อเนื่องขณะอยู่ในเกม"; "id" -> "Putar musik saat sedang bermain"
                else -> en
            }
            "Background Music Volume" -> return when (lang) {
                "zh" -> "背景音乐音量"; "ja" -> "BGM 音量"; "ko" -> "배경 음악 볼륨"; "es" -> "Volumen de música de fondo"; "pt" -> "Volume da música de fundo"
                "fr" -> "Volume de la musique"; "de" -> "Hintergrundmusik-Lautstärke"; "ru" -> "Громкость фоновой музыки"; "th" -> "ระดับเสียงเพลงพื้นหลัง"; "id" -> "Volume Musik Latar"
                else -> en
            }
            "App Updates, About & Share QR" -> return when (lang) {
                "zh" -> "应用更新、关于与二维码分享"; "ja" -> "アップデート・情報・QR共有"; "ko" -> "앱 업데이트, 정보 및 QR 공유"; "es" -> "Actualizaciones, Info y Compartir QR"; "pt" -> "Atualizações, Sobre e QR Code"
                "fr" -> "Mises à jour, À propos et Partage QR"; "de" -> "App-Updates, Info & QR teilen"; "ru" -> "Обновления, о приложении и QR-код"; "th" -> "อัปเดตแอป เกี่ยวกับ และแชร์ QR"; "id" -> "Pembaruan, Tentang & Bagikan QR"
                else -> en
            }
            "Check for updates on launch" -> return when (lang) {
                "zh" -> "启动时自动检查新版本"; "ja" -> "起動時にアップデートを確認"; "ko" -> "앱 실행 시 새 버전 자동 확인"; "es" -> "Buscar actualizaciones al iniciar"; "pt" -> "Verificar atualizações ao iniciar"
                "fr" -> "Vérifier les mises à jour au lancement"; "de" -> "Beim Start nach Updates suchen"; "ru" -> "Проверять обновления при запуске"; "th" -> "ตรวจหาอัปเดตใหม่เมื่อเปิดแอป"; "id" -> "Periksa pembaruan saat aplikasi dibuka"
                else -> en
            }
            "Check now" -> return when (lang) {
                "zh" -> "立即检查"; "ja" -> "今すぐ確認"; "ko" -> "지금 확인"; "es" -> "Comprobar ahora"; "pt" -> "Verificar agora"
                "fr" -> "Vérifier"; "de" -> "Jetzt prüfen"; "ru" -> "Проверить"; "th" -> "ตรวจสอบเลย"; "id" -> "Periksa sekarang"
                else -> en
            }
            "Checking..." -> return when (lang) {
                "zh" -> "检查中..."; "ja" -> "確認中..."; "ko" -> "확인 중..."; "es" -> "Comprobando..."; "pt" -> "Verificando..."
                "fr" -> "Vérification..."; "de" -> "Prüfe..."; "ru" -> "Проверка..."; "th" -> "กำลังตรวจ..."; "id" -> "Memeriksa..."
                else -> en
            }
            "Share & QR" -> return when (lang) {
                "zh" -> "分享与二维码"; "ja" -> "共有 & QR"; "ko" -> "QR 및 공유"; "es" -> "Compartir y QR"; "pt" -> "Compartilhar e QR"
                "fr" -> "Partager & QR"; "de" -> "Teilen & QR"; "ru" -> "QR и Поделиться"; "th" -> "แชร์ & QR"; "id" -> "Bagikan & QR"
                else -> en
            }
            "Terms & Privacy" -> return when (lang) {
                "zh" -> "使用条款与隐私说明"; "ja" -> "利用規約とプライバシー"; "ko" -> "이용약관 및 개인정보 보호"; "es" -> "Términos y Privacidad"; "pt" -> "Termos e Privacidade"
                "fr" -> "Conditions et Confidentialité"; "de" -> "Bedingungen & Datenschutz"; "ru" -> "Условия и конфиденциальность"; "th" -> "ข้อกำหนดและความเป็นส่วนตัว"; "id" -> "Ketentuan & Privasi"
                else -> en
            }
            "Quick summary & link to full terms" -> return when (lang) {
                "zh" -> "快速摘要与完整条款链接"; "ja" -> "概要と全文へのリンク"; "ko" -> "요약 및 전체 이용약관 링크"; "es" -> "Resumen rápido y enlace a términos"; "pt" -> "Resumo rápido e link para termos"
                "fr" -> "Résumé rapide et conditions complètes"; "de" -> "Kurzzusammenfassung & vollständige Bedingungen"; "ru" -> "Краткая сводка и полные условия"; "th" -> "สรุปย่อและลิงก์ข้อกำหนดฉบับเต็ม"; "id" -> "Ringkasan cepat & tautan ketentuan lengkap"
                else -> en
            }
            "Quick Summary" -> return when (lang) {
                "zh" -> "核心条款摘要"; "ja" -> "規約の要約"; "ko" -> "핵심 요약"; "es" -> "Resumen Rápido"; "pt" -> "Resumo Rápido"
                "fr" -> "Résumé Rapide"; "de" -> "Kurzzusammenfassung"; "ru" -> "Краткая сводка"; "th" -> "สรุปสาระสำคัญ"; "id" -> "Ringkasan Cepat"
                else -> en
            }
            "Re-open Terms Consent Dialog" -> return when (lang) {
                "zh" -> "重新打开条款同意弹窗"; "ja" -> "規約同意ダイアログを再表示"; "ko" -> "약관 동의 대화상자 다시 열기"; "es" -> "Reabrir diálogo de aceptación"; "pt" -> "Reabrir diálogo de consentimento"
                "fr" -> "Rouvrir la boîte d'acceptation"; "de" -> "Zustimmungsdialog erneut öffnen"; "ru" -> "Открыть окно принятия условий"; "th" -> "เปิดหน้าต่างยอมรับข้อกำหนดอีกครั้ง"; "id" -> "Buka Ulang Dialog Persetujuan"
                else -> en
            }
            "Sign in, check network, and set up controls" -> return when (lang) {
                "zh" -> "登录账号、检查网络连接并设置操控方式"; "ja" -> "サインイン・通信確認・操作設定のガイド"; "ko" -> "로그인, 네트워크 확인 및 컨트롤 설정"; "es" -> "Inicia sesión, revisa la red y configura controles"; "pt" -> "Entre na conta, verifique a rede e configure controles"
                "fr" -> "Connexion, vérification réseau et configuration des commandes"; "de" -> "Anmelden, Netzwerk prüfen & Steuerung einrichten"; "ru" -> "Вход, проверка сети и настройка управления"; "th" -> "ลงชื่อเข้าใช้ ตรวจสอบเครือข่าย และตั้งค่าปุ่มควบคุม"; "id" -> "Masuk, periksa jaringan, dan atur kontrol"
                else -> en
            }
            "Network" -> return when (lang) {
                "zh" -> "网络"; "ja" -> "通信"; "ko" -> "네트워크"; "es" -> "Red"; "pt" -> "Rede"
                "fr" -> "Réseau"; "de" -> "Netzwerk"; "ru" -> "Сеть"; "th" -> "เครือข่าย"; "id" -> "Jaringan"
                else -> en
            }
            "Choose a stable network connection" -> return when (lang) {
                "zh" -> "在开始游戏前选择稳定的网络连接"; "ja" -> "プレイ前に安定した通信接続を選択"; "ko" -> "플레이 전 안정적인 네트워크 연결 선택"; "es" -> "Elige una conexión de red estable antes de jugar"; "pt" -> "Escolha uma conexão de rede estável antes de jogar"
                "fr" -> "Choisissez une connexion réseau stable avant de jouer"; "de" -> "Wähle vor dem Spielen eine stabile Netzwerkverbindung"; "ru" -> "Выберите стабильное подключение к сети перед игрой"; "th" -> "เลือกการเชื่อมต่อเครือข่ายที่เสถียรก่อนเริ่มเล่น"; "id" -> "Pilih koneksi jaringan yang stabil sebelum bermain"
                else -> en
            }
            "Tips for smooth connection and low latency" -> return when (lang) {
                "zh" -> "保持流畅连接与降低延迟的技巧"; "ja" -> "快適な通信と低遅延を保つコツ"; "ko" -> "원활한 연결 및 지연 시간 감소 팁"; "es" -> "Consejos para una conexión fluida y baja latencia"; "pt" -> "Dicas para conexão estável e baixa latência"
                "fr" -> "Astuces pour une connexion fluide et une faible latence"; "de" -> "Tipps für stabile Verbindung und niedrige Latenz"; "ru" -> "Советы для плавного соединения и низкого пинга"; "th" -> "เคล็ดลับการเชื่อมต่อที่ลื่นไหลและลดความหน่วง"; "id" -> "Tips koneksi lancar dan latensi rendah"
                else -> en
            }
            "Connection error or slow loading?" -> return when (lang) {
                "zh" -> "无法连接或页面加载缓慢？"; "ja" -> "接続エラーや読み込みが遅い場合は？"; "ko" -> "연결 오류 또는 페이지 로딩이 느린가요?"; "es" -> "¿Error de conexión o carga lenta?"; "pt" -> "Erro de conexão ou carregamento lento?"
                "fr" -> "Erreur de connexion ou chargement lent ?"; "de" -> "Verbindungsfehler oder langsames Laden?"; "ru" -> "Ошибка подключения или долгая загрузка?"; "th" -> "เชื่อมต่อไม่ได้หรือโหลดหน้าเว็บช้า?"; "id" -> "Gagal terhubung atau pemuatan lambat?"
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
    val XBOX_MULTILANG_DICTIONARIES: Map<String, Map<String, String>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        mapOf(
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
    }

    fun getXboxDictForLanguage(rawLangCode: String): Map<String, String> {
        val code = normalizeLangCode(rawLangCode)
        return XBOX_MULTILANG_DICTIONARIES[code] ?: emptyMap()
    }
}
