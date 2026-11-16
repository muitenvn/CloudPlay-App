package com.muitenvn.cloudplay.pad

import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Hỗ trợ đầy đủ 12 ngôn ngữ như CloudPlay hiện có:
 * Tiếng Việt (vi), English (en), Español (es), Português (pt), Français (fr),
 * Deutsch (de), 日本語 (ja), 한국어 (ko), 中文 (zh), Bahasa Indonesia (id), ไทย (th), Русский (ru).
 */
enum class PadLanguage(
    val code: String,
    val badge: String,
    val displayName: String
) {
    SYSTEM("system", "AUTO", "System"),
    VIETNAMESE("vi", "VI", "Tiếng Việt"),
    ENGLISH("en", "EN", "English"),
    SPANISH("es", "ES", "Español"),
    PORTUGUESE("pt", "PT", "Português"),
    FRENCH("fr", "FR", "Français"),
    GERMAN("de", "DE", "Deutsch"),
    JAPANESE("ja", "JA", "日本語"),
    KOREAN("ko", "KO", "한국어"),
    CHINESE("zh", "ZH", "中文"),
    INDONESIAN("id", "ID", "Indonesia"),
    THAI("th", "TH", "ไทย"),
    RUSSIAN("ru", "RU", "Русский");

    fun effectiveCode(): String {
        if (this != SYSTEM) return code
        val sys = try {
            val conf = Resources.getSystem().configuration
            val loc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !conf.locales.isEmpty) {
                conf.locales[0]
            } else {
                @Suppress("DEPRECATION")
                conf.locale
            }
            loc?.language?.lowercase(Locale.ROOT).orEmpty()
        } catch (_: Exception) {
            Locale.getDefault().language.lowercase(Locale.ROOT)
        }
        val norm = if (sys == "in") "id" else sys
        return entries.firstOrNull { it != SYSTEM && norm.startsWith(it.code) }?.code ?: "en"
    }
}

object PadStrings {
    fun tr(
        lang: PadLanguage,
        vi: String,
        en: String,
        es: String = en,
        pt: String = en,
        fr: String = en,
        de: String = en,
        ja: String = en,
        ko: String = en,
        zh: String = en,
        id: String = en,
        th: String = en,
        ru: String = en
    ): String {
        return when (lang.effectiveCode()) {
            "vi" -> vi
            "es" -> es
            "pt" -> pt
            "fr" -> fr
            "de" -> de
            "ja" -> ja
            "ko" -> ko
            "zh" -> zh
            "id" -> id
            "th" -> th
            "ru" -> ru
            else -> en
        }
    }

    fun selectTvTitle(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Chọn TV để kết nối",
        en = "Select a TV to Connect",
        es = "Selecciona un TV para conectar",
        pt = "Selecione uma TV para conectar",
        fr = "Sélectionner un téléviseur",
        de = "Fernseher zum Verbinden wählen",
        ja = "接続するテレビを選択",
        ko = "연결할 TV 선택",
        zh = "选择要连接的电视",
        id = "Pilih TV untuk dihubungkan",
        th = "เลือกทีวีที่จะเชื่อมต่อ",
        ru = "Выберите ТВ для подключения"
    )

    fun selectTvGuide(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Mở Ravno trên TV → Cài đặt → Dùng điện thoại làm tay cầm để lấy mã 4 số.",
        en = "Open Ravno on TV → Settings → Use Phone as Controller to get the 4-digit code.",
        es = "Abre Ravno en el TV → Ajustes → Usar teléfono como mando para obtener el código de 4 dígitos.",
        pt = "Abra o Ravno na TV → Configurações → Usar celular como controle para ver o código de 4 dígitos.",
        fr = "Ouvrez Ravno sur la TV → Paramètres → Téléphone comme manette pour le code à 4 chiffres.",
        de = "Ravno auf TV öffnen → Einstellungen → Smartphone als Controller für den 4-stelligen Code.",
        ja = "テレビでRavno → 設定 → スマホをコントローラーとして使用を開き4桁のコードを確認。",
        ko = "TV에서 Ravno 열기 → 설정 → 휴대폰을 컨트롤러로 사용에서 4자리 코드 확인.",
        zh = "在电视上打开 Ravno → 设置 → 使用手机作为手柄，获取 4 位配对码。",
        id = "Buka Ravno di TV → Pengaturan → Gunakan Ponsel sebagai Stik untuk kode 4 digit.",
        th = "เปิด Ravno บนทีวี → การตั้งค่า → ใช้โทรศัพท์เป็นจอย เพื่อรับรหัส 4 หลัก",
        ru = "Откройте Ravno на ТВ → Настройки → Телефон как геймпад, чтобы получить 4-значный код."
    )

    fun statusReadyPair(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Sẵn sàng ghép đôi · Chờ mã 4 số",
        en = "Ready to pair · Waiting for 4-digit code",
        es = "Listo para vincular · Código de 4 dígitos",
        pt = "Pronto para parear · Código de 4 dígitos",
        fr = "Prêt à jumeler · Code à 4 chiffres",
        de = "Kopplungsbereit · 4-stelliger Code",
        ja = "ペアリング準備完了 · 4桁のコード待ち",
        ko = "페어링 준비 완료 · 4자리 코드 대기",
        zh = "准备配对 · 等待输入 4 位配对码",
        id = "Siap dipasangkan · Menunggu kode 4 digit",
        th = "พร้อมจับคู่ · รอรหัส 4 หลัก",
        ru = "Готов к сопряжению · Код из 4 цифр"
    )

    fun statusRemembered(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Đã nhớ thiết bị · Chạm để kết nối ngay",
        en = "Remembered TV · Tap to connect immediately",
        es = "TV recordado · Toca para conectar",
        pt = "TV lembrada · Toque para conectar",
        fr = "TV mémorisée · Appuyez pour connecter",
        de = "Gespeichert · Tippen zum Verbinden",
        ja = "登録済みテレビ · タップしてすぐ接続",
        ko = "기억된 TV · 탭하여 즉시 연결",
        zh = "已记住设备 · 点击立即连接",
        id = "Perangkat tersimpan · Ketuk untuk hubungkan",
        th = "จำอุปกรณ์แล้ว · แตะเพื่อเชื่อมต่อทันที",
        ru = "Устройство сохранено · Нажмите для входа"
    )

    fun statusHasController(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Đang có tay cầm kết nối",
        en = "Controller currently connected",
        es = "Mando conectado actualmente",
        pt = "Controle conectado no momento",
        fr = "Manette actuellement connectée",
        de = "Controller bereits verbunden",
        ja = "コントローラー接続中",
        ko = "현재 컨트롤러 연결됨",
        zh = "当前已有手柄连接",
        id = "Stik sedang terhubung",
        th = "มีจอยเชื่อมต่ออยู่แล้ว",
        ru = "Геймпад уже подключен"
    )

    fun statusBusy(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Đang bận · Không nhận thêm tay cầm",
        en = "Busy · Not accepting controllers",
        es = "Ocupado · No acepta más mandos",
        pt = "Ocupado · Não aceita controles",
        fr = "Occupé · N'accepte plus de manettes",
        de = "Beschäftigt · Keine weiteren Controller",
        ja = "ビジー · 接続できません",
        ko = "사용 중 · 추가 연결 불가",
        zh = "繁忙 · 暂不接受新连接",
        id = "Sibuk · Tidak menerima stik baru",
        th = "ไม่ว่าง · ไม่รับจอยเพิ่ม",
        ru = "Занят · Не принимает подключения"
    )

    fun rescanButton(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Quét lại",
        en = "Rescan",
        es = "Reescanear",
        pt = "Buscar novamente",
        fr = "Actualiser",
        de = "Neu suchen",
        ja = "再スキャン",
        ko = "다시 스캔",
        zh = "重新扫描",
        id = "Pindai ulang",
        th = "สแกนใหม่",
        ru = "Обновить"
    )

    fun localOnlyNotice(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Chỉ hoạt động trong mạng nội bộ (LAN)",
        en = "Works on local network (LAN) only",
        es = "Solo funciona en red local (LAN)",
        pt = "Funciona apenas na rede local (LAN)",
        fr = "Réseau local (LAN) uniquement",
        de = "Nur im lokalen Netzwerk (LAN)",
        ja = "ローカルネットワーク(LAN)専用",
        ko = "로컬 네트워크(LAN) 전용",
        zh = "仅在局域网 (LAN) 内运行",
        id = "Hanya bekerja di jaringan lokal (LAN)",
        th = "ทำงานเฉพาะในเครือข่ายภายใน (LAN)",
        ru = "Только в локальной сети (LAN)"
    )

    fun enterPinTitle(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Nhập mã 4 số trên TV",
        en = "Enter 4-Digit Code on TV",
        es = "Ingresa el código de 4 dígitos del TV",
        pt = "Digite o código de 4 dígitos da TV",
        fr = "Entrez le code à 4 chiffres affiché sur la TV",
        de = "4-stelligen Code vom TV eingeben",
        ja = "テレビの4桁のコードを入力",
        ko = "TV에 표시된 4자리 코드 입력",
        zh = "输入电视上的 4 位配对码",
        id = "Masukkan kode 4 digit di TV",
        th = "ป้อนรหัส 4 หลักบนทีวี",
        ru = "Введите 4-значный код с экрана ТВ"
    )

    fun enterPinSubtitle(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Nhìn lên màn hình TV và nhập 4 chữ số đang hiển thị.",
        en = "Look at your TV screen and enter the 4 digits shown.",
        es = "Mira la pantalla del TV e introduce los 4 dígitos.",
        pt = "Olhe para a tela da TV e digite os 4 números exibidos.",
        fr = "Regardez l'écran du téléviseur et saisissez les 4 chiffres.",
        de = "Gib die 4 Ziffern ein, die auf dem Fernseher angezeigt werden.",
        ja = "テレビ画面に表示されている4桁の数字を入力してください。",
        ko = "TV 화면에 표시된 4자리 숫자를 입력하세요.",
        zh = "请查看电视屏幕并输入显示的 4 位数字。",
        id = "Lihat layar TV Anda dan masukkan 4 angka yang tampil.",
        th = "ดูที่หน้าจอทีวีแล้วป้อนตัวเลข 4 หลักที่แสดงอยู่",
        ru = "Посмотрите на экран ТВ и введите 4 отображаемые цифры."
    )

    fun changeTvBack(lang: PadLanguage) = tr(
        lang = lang,
        vi = "← Đổi TV",
        en = "← Change TV",
        es = "← Cambiar TV",
        pt = "← Trocar TV",
        fr = "← Changer TV",
        de = "← TV wechseln",
        ja = "← テレビ変更",
        ko = "← TV 변경",
        zh = "← 更换电视",
        id = "← Ganti TV",
        th = "← เปลี่ยนทีวี",
        ru = "← Сменить ТВ"
    )

    fun keypadDelete(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Xóa",
        en = "Del",
        es = "Borrar",
        pt = "Apagar",
        fr = "Effacer",
        de = "Löschen",
        ja = "削除",
        ko = "지우기",
        zh = "删除",
        id = "Hapus",
        th = "ลบ",
        ru = "Стереть"
    )

    fun keypadConnect(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Kết nối",
        en = "Connect",
        es = "Conectar",
        pt = "Conectar",
        fr = "Connecter",
        de = "Verbinden",
        ja = "接続",
        ko = "연결",
        zh = "连接",
        id = "Hubungkan",
        th = "เชื่อมต่อ",
        ru = "Войти"
    )

    fun needUpdateCloudPlay(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Cần cập nhật Ravno",
        en = "Need to update Ravno",
        es = "Actualiza Ravno en el TV",
        pt = "Atualize o Ravno na TV",
        fr = "Mise à jour de Ravno requise",
        de = "Ravno muss aktualisiert werden",
        ja = "Ravnoの更新が必要です",
        ko = "Ravno 업데이트 필요",
        zh = "需要更新 Ravno",
        id = "Perlu memperbarui Ravno",
        th = "ต้องอัปเดต Ravno",
        ru = "Требуется обновить Ravno"
    )

    fun needUpdateCloudPlayPad(lang: PadLanguage) = tr(
        lang = lang,
        vi = "Cần cập nhật Ravno Pad",
        en = "Need to update Ravno Pad",
        es = "Actualiza Ravno Pad",
        pt = "Atualize o Ravno Pad",
        fr = "Mise à jour de Ravno Pad requise",
        de = "Ravno Pad muss aktualisiert werden",
        ja = "Ravno Padの更新が必要です",
        ko = "Ravno Pad 업데이트 필요",
        zh = "需要更新 Ravno Pad",
        id = "Perlu memperbarui Ravno Pad",
        th = "ต้องอัปเดต Ravno Pad",
        ru = "Требуется обновить Ravno Pad"
    )
}
