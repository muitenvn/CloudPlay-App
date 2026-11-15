package com.muitenvn.cloudplay.pad

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.muitenvn.cloudplay.protocol.AuthFailReason
import com.muitenvn.cloudplay.protocol.TvServiceStatus
import com.muitenvn.cloudplay.protocol.VersionCheckResult

class PadMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        PadNetworkClient.ensureInitialized(this)
        setContent {
            CloudPlayPadRootScreen()
        }
    }
}

// Bảng màu phẳng đồng bộ với remote-pad-mockup.html (Flat Dark Gaming UI, không phát sáng, không làm mờ)
internal val PadBg = Color(0xFF0B0F0E)
internal val PadSurface = Color(0xFF131A17)
internal val PadSurface2 = Color(0xFF1A2420)
internal val PadBorder = Color(0xFF26352E)
internal val PadInk = Color(0xFFEAF2EE)
internal val PadMuted = Color(0xFF8EA399)
internal val PadAccent = Color(0xFF2EE56B)
internal val PadAccentFill = Color(0xFF163824)
internal val PadDanger = Color(0xFFFF5B63)
internal val PadWarn = Color(0xFFF5B841)

internal data class PadResponsiveMetrics(
    val outerPad: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val cardPad: Dp,
    val cardRadius: Dp,
    val buttonRadius: Dp,
    val strokeThin: Dp,
    val strokeFocus: Dp,
    val dotSize: Dp,
    val pinBoxW: Dp,
    val pinBoxH: Dp,
    val keyHeight: Dp,
    val titleFont: TextUnit,
    val headingFont: TextUnit,
    val bodyFont: TextUnit,
    val captionFont: TextUnit,
    val pinDigitFont: TextUnit,
    val keyFont: TextUnit
)

@Composable
internal fun rememberPadMetrics(maxWidth: Dp, maxHeight: Dp): PadResponsiveMetrics {
    val density = LocalDensity.current
    return remember(maxWidth, maxHeight, density.density, density.fontScale) {
        val minSide = minOf(maxWidth, maxHeight)
        val unit = minSide * 0.01f
        with(density) {
            PadResponsiveMetrics(
                outerPad = unit * 4.8f,
                sectionGap = unit * 3.4f,
                itemGap = unit * 2.4f,
                cardPad = unit * 3.8f,
                cardRadius = unit * 3.8f,
                buttonRadius = unit * 3.0f,
                strokeThin = unit * 0.32f,
                strokeFocus = unit * 0.55f,
                dotSize = unit * 2.2f,
                pinBoxW = unit * 14.5f,
                pinBoxH = unit * 17.5f,
                keyHeight = unit * 13.5f,
                titleFont = (unit * 5.0f).toSp(),
                headingFont = (unit * 4.3f).toSp(),
                bodyFont = (unit * 3.5f).toSp(),
                captionFont = (unit * 3.0f).toSp(),
                pinDigitFont = (unit * 7.2f).toSp(),
                keyFont = (unit * 4.8f).toSp()
            )
        }
    }
}

@Composable
fun CloudPlayPadRootScreen() {
    val context = LocalContext.current
    val clientState by PadNetworkClient.state.collectAsState()

    DisposableEffect(Unit) {
        PadNetworkClient.startDiscovery(context)
        onDispose {
            PadNetworkClient.stopDiscovery()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PadBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("cloudplay_pad_root")
    ) {
        val m = rememberPadMetrics(maxWidth, maxHeight)

        when (clientState.route) {
            PadScreenRoute.FIND_TV -> {
                PadFindTvScreen(
                    state = clientState,
                    m = m,
                    onRescan = { PadNetworkClient.rescan(context) },
                    onCycleLanguage = { PadNetworkClient.cycleLanguage(context) },
                    onSelectTv = { PadNetworkClient.onSelectTv(it) },
                    onProbeManualIp = { PadNetworkClient.probeManualIp(it) }
                )
            }

            PadScreenRoute.ENTER_PIN -> {
                BackHandler {
                    PadNetworkClient.backToTvList()
                }
                PadEnterPinScreen(
                    state = clientState,
                    m = m,
                    onBack = { PadNetworkClient.backToTvList() },
                    onDigit = { PadNetworkClient.appendPinDigit(it) },
                    onBackspace = { PadNetworkClient.backspacePinDigit() },
                    onSubmit = { PadNetworkClient.submitFourDigitPin() }
                )
            }

            PadScreenRoute.GAMEPAD -> {
                BackHandler {
                    PadNetworkClient.disconnectActiveSession(navigateToList = true)
                }
                PadConnectedReadyScreen(
                    state = clientState,
                    m = m,
                    onDisconnect = { PadNetworkClient.disconnectActiveSession(navigateToList = true) }
                )
            }

            PadScreenRoute.DISCONNECTED -> {
                BackHandler {
                    PadNetworkClient.backToTvList()
                }
                PadDisconnectedSummaryScreen(
                    state = clientState,
                    m = m,
                    onReconnect = {
                        clientState.selectedTv?.let { PadNetworkClient.onSelectTv(it) }
                    },
                    onBackToList = { PadNetworkClient.backToTvList() }
                )
            }
        }
    }
}

/**
 * Màn hình Điện thoại 1 (#s3 trong remote-pad-mockup.html): Tìm TV trong mạng nội bộ.
 */
@Composable
private fun PadFindTvScreen(
    state: PadClientUiState,
    m: PadResponsiveMetrics,
    onRescan: () -> Unit,
    onCycleLanguage: () -> Unit,
    onSelectTv: (DiscoveredTvDevice) -> Unit,
    onProbeManualIp: (String) -> Unit
) {
    val lang = state.language
    var manualIp by remember { mutableStateOf("") }
    var showManualIpRow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(m.outerPad)
            .testTag("pad_find_tv_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Thanh trên cùng: CloudPlay Pad + mạng Wi-Fi + nút đổi ngôn ngữ (12 ngôn ngữ)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CloudPlay Pad",
                color = PadInk,
                fontSize = m.titleFont,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(m.itemGap)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.6f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(m.dotSize)
                            .clip(CircleShape)
                            .background(PadAccent)
                    )
                    Text(
                        text = state.localIpAddress.ifBlank { "Wi-Fi / LAN" },
                        color = PadMuted,
                        fontSize = m.captionFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadSurface2)
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                        .clickable(onClick = onCycleLanguage)
                        .padding(horizontal = m.cardPad * 0.7f, vertical = m.cardPad * 0.35f)
                        .testTag("pad_language_btn")
                ) {
                    Text(
                        text = lang.badge,
                        color = PadInk,
                        fontSize = m.captionFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(m.sectionGap))

        // 2. Danh sách TV tìm thấy trong mạng nội bộ (_cloudplaypad._tcp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(m.itemGap)
        ) {
            Text(
                text = PadStrings.selectTvTitle(lang),
                color = PadInk,
                fontSize = m.headingFont,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = PadStrings.selectTvGuide(lang),
                color = PadMuted,
                fontSize = m.bodyFont
            )

            // Hiển thị cảnh báo nếu phát hiện lệch phiên bản giao thức
            if (state.versionMismatchResult != VersionCheckResult.COMPATIBLE) {
                val verMsg = when (state.versionMismatchResult) {
                    VersionCheckResult.NEED_UPDATE_CLOUDPLAY -> PadStrings.needUpdateCloudPlay(lang)
                    VersionCheckResult.NEED_UPDATE_PAD -> PadStrings.needUpdateCloudPlayPad(lang)
                    VersionCheckResult.COMPATIBLE -> ""
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(m.cardRadius))
                        .background(PadSurface)
                        .border(m.strokeFocus, PadDanger, RoundedCornerShape(m.cardRadius))
                        .padding(m.cardPad)
                        .testTag("pad_version_mismatch_banner")
                ) {
                    Text(
                        text = verMsg,
                        color = PadDanger,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.discoveredTvs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(m.cardRadius))
                        .background(PadSurface)
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.cardRadius))
                        .padding(m.cardPad * 1.2f)
                        .testTag("pad_empty_tv_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.7f)) {
                        Text(
                            text = PadStrings.tr(
                                lang = lang,
                                vi = if (state.isScanning) "Đang tìm TV trên mạng nội bộ..." else "Chưa tìm thấy TV nào đang mở ghép đôi",
                                en = if (state.isScanning) "Scanning local network for TV..." else "No TV found waiting for pairing"
                            ),
                            color = PadInk,
                            fontSize = m.bodyFont,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = PadStrings.tr(
                                lang = lang,
                                vi = "Hãy mở mục Dùng điện thoại làm tay cầm trong Cài đặt của CloudPlay trên TV.",
                                en = "Open 'Use Phone as Controller' in CloudPlay Settings on your TV."
                            ),
                            color = PadMuted,
                            fontSize = m.captionFont
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("pad_tv_list"),
                    verticalArrangement = Arrangement.spacedBy(m.itemGap)
                ) {
                    itemsIndexed(state.discoveredTvs, key = { _, item -> item.tvId }) { idx, tv ->
                        val isBusy = tv.status == TvServiceStatus.BUSY
                        val statusLabel = when {
                            tv.hasRememberToken -> PadStrings.statusRemembered(lang)
                            tv.status == TvServiceStatus.HAS_CONTROLLER -> PadStrings.statusHasController(lang)
                            tv.status == TvServiceStatus.BUSY -> PadStrings.statusBusy(lang)
                            else -> PadStrings.statusReadyPair(lang)
                        }
                        val statusColor = when {
                            tv.hasRememberToken || tv.status == TvServiceStatus.WAITING_FOR_PAIR -> PadAccent
                            tv.status == TvServiceStatus.HAS_CONTROLLER -> PadWarn
                            else -> PadMuted
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(m.cardRadius))
                                .background(PadSurface)
                                .border(
                                    if (tv.hasRememberToken) m.strokeFocus else m.strokeThin,
                                    if (tv.hasRememberToken) PadAccent else PadBorder,
                                    RoundedCornerShape(m.cardRadius)
                                )
                                .clickable(enabled = !isBusy) { onSelectTv(tv) }
                                .padding(m.cardPad)
                                .testTag("pad_tv_card_$idx"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.35f)
                            ) {
                                Text(
                                    text = tv.tvName,
                                    color = if (isBusy) PadMuted else PadInk,
                                    fontSize = m.bodyFont,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = statusLabel,
                                    color = statusColor,
                                    fontSize = m.captionFont,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(m.itemGap))

                            Text(
                                text = if (isBusy) "—" else "→",
                                color = if (isBusy) PadMuted else PadAccent,
                                fontSize = m.headingFont,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Tùy chọn nhập IP LAN thủ công nếu router Wi-Fi chặn quảng bá mDNS
            if (showManualIpRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(m.buttonRadius))
                            .background(PadSurface)
                            .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                            .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.7f)
                            .testTag("pad_manual_ip_input")
                    ) {
                        if (manualIp.isEmpty()) {
                            Text(
                                text = "IP TV (192.168.1.x)",
                                color = PadMuted,
                                fontSize = m.captionFont
                            )
                        }
                        BasicTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = PadInk,
                                fontSize = m.bodyFont,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(PadAccent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(m.buttonRadius))
                            .background(PadSurface2)
                            .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                            .clickable { onProbeManualIp(manualIp) }
                            .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.7f)
                            .testTag("pad_manual_ip_btn")
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Tìm IP", "Find IP"),
                            color = PadAccent,
                            fontSize = m.captionFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = PadStrings.tr(
                        lang = lang,
                        vi = "Không thấy TV? Chạm để nhập địa chỉ IP mạng nội bộ",
                        en = "Don't see your TV? Tap to enter LAN IP manually"
                    ),
                    color = PadMuted,
                    fontSize = m.captionFont,
                    modifier = Modifier
                        .clickable { showManualIpRow = true }
                        .padding(vertical = m.itemGap * 0.3f)
                        .testTag("pad_toggle_manual_ip_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(m.sectionGap))

        // 3. Thanh chân trang: Chỉ hoạt động trong mạng nội bộ + nút Quét lại
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = PadStrings.localOnlyNotice(lang),
                color = PadMuted,
                fontSize = m.captionFont,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(m.itemGap))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(m.buttonRadius))
                    .background(PadSurface2)
                    .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                    .clickable(onClick = onRescan)
                    .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.65f)
                    .testTag("pad_rescan_btn")
            ) {
                Text(
                    text = PadStrings.rescanButton(lang),
                    color = PadInk,
                    fontSize = m.bodyFont,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Màn hình Điện thoại 2 (#s4 trong remote-pad-mockup.html): Nhập mã 4 chữ số trên TV.
 */
@Composable
private fun PadEnterPinScreen(
    state: PadClientUiState,
    m: PadResponsiveMetrics,
    onBack: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit
) {
    val lang = state.language
    val tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "CloudPlay TV" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(m.outerPad)
            .testTag("pad_enter_pin_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Thanh trên cùng: ← Đổi TV + Tên TV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(m.buttonRadius))
                    .background(PadSurface2)
                    .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                    .clickable(onClick = onBack)
                    .padding(horizontal = m.cardPad * 0.85f, vertical = m.cardPad * 0.5f)
                    .testTag("pad_back_to_tv_list_btn")
            ) {
                Text(
                    text = PadStrings.changeTvBack(lang),
                    color = PadInk,
                    fontSize = m.captionFont,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = tvName,
                color = PadMuted,
                fontSize = m.captionFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 2. Giữa: Tiêu đề + 4 ô số PIN + Thông báo lỗi / Khóa tạm
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(m.itemGap)
        ) {
            Text(
                text = PadStrings.enterPinTitle(lang),
                color = PadInk,
                fontSize = m.headingFont,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = PadStrings.enterPinSubtitle(lang),
                color = PadMuted,
                fontSize = m.bodyFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(m.itemGap * 0.4f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(m.itemGap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("pad_pin_boxes_row")
            ) {
                for (i in 0 until 4) {
                    val charStr = state.enteredPin.getOrNull(i)?.toString().orEmpty()
                    val isCurrentSlot = state.enteredPin.length == i || (i == 3 && state.enteredPin.length == 4)
                    val borderColor = when {
                        state.lockoutRemainingSec > 0 || state.pinErrorReason != null -> PadDanger
                        isCurrentSlot -> PadAccent
                        else -> PadBorder
                    }
                    Box(
                        modifier = Modifier
                            .size(width = m.pinBoxW, height = m.pinBoxH)
                            .clip(RoundedCornerShape(m.cardRadius))
                            .background(PadSurface)
                            .border(
                                if (isCurrentSlot) m.strokeFocus else m.strokeThin,
                                borderColor,
                                RoundedCornerShape(m.cardRadius)
                            )
                            .testTag("pad_pin_box_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = charStr,
                            color = PadInk,
                            fontFamily = FontFamily.Monospace,
                            fontSize = m.pinDigitFont,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Hiển thị thông báo lỗi / khóa tạm / đang xác thực HMAC
            val feedbackText = when {
                state.isConnecting -> PadStrings.tr(
                    lang = lang,
                    vi = "Đang xác thực chữ ký bảo mật với TV...",
                    en = "Verifying security signature with TV..."
                )
                state.lockoutRemainingSec > 0 || state.pinErrorReason == AuthFailReason.LOCKED_OUT -> PadStrings.tr(
                    lang = lang,
                    vi = "Nhập sai 3 lần · TV tạm khóa ghép đôi (${state.lockoutRemainingSec} giây)",
                    en = "3 wrong tries · TV locked pairing (${state.lockoutRemainingSec}s)"
                )
                state.pinErrorReason == AuthFailReason.WRONG_PIN -> PadStrings.tr(
                    lang = lang,
                    vi = "Mã ghép đôi chưa đúng · Còn ${state.remainingPinAttempts} lần thử",
                    en = "Incorrect pairing code · ${state.remainingPinAttempts} attempts remaining"
                )
                state.pinErrorReason == AuthFailReason.TOKEN_REVOKED -> PadStrings.tr(
                    lang = lang,
                    vi = "TV đã quên thiết bị này · Vui lòng nhập mã 4 số mới",
                    en = "TV forgot this device · Please enter the new 4-digit code"
                )
                state.pinErrorReason == AuthFailReason.VERSION_MISMATCH -> {
                    if (state.versionMismatchResult == VersionCheckResult.NEED_UPDATE_CLOUDPLAY) {
                        PadStrings.needUpdateCloudPlay(lang)
                    } else {
                        PadStrings.needUpdateCloudPlayPad(lang)
                    }
                }
                else -> ""
            }

            if (feedbackText.isNotBlank()) {
                Text(
                    text = feedbackText,
                    color = if (state.isConnecting) PadAccent else PadDanger,
                    fontSize = m.captionFont,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pad_pin_feedback_text")
                )
            }
        }

        // 3. Bàn phím số 3x4 (#s4 .kpad trong remote-pad-mockup.html)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.8f)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )
            rows.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.8f)
                ) {
                    r.forEach { d ->
                        PadKeypadButton(
                            label = d,
                            m = m,
                            isPrimary = false,
                            onClick = { onDigit(d[0]) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pad_key_$d")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.8f)
            ) {
                PadKeypadButton(
                    label = PadStrings.keypadDelete(lang),
                    m = m,
                    isPrimary = false,
                    useCaptionFont = true,
                    onClick = onBackspace,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pad_key_backspace")
                )
                PadKeypadButton(
                    label = "0",
                    m = m,
                    isPrimary = false,
                    onClick = { onDigit('0') },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pad_key_0")
                )
                PadKeypadButton(
                    label = PadStrings.keypadConnect(lang),
                    m = m,
                    isPrimary = true,
                    useCaptionFont = true,
                    onClick = onSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pad_key_connect")
                )
            }
        }
    }
}

@Composable
private fun PadKeypadButton(
    label: String,
    m: PadResponsiveMetrics,
    isPrimary: Boolean,
    useCaptionFont: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(m.keyHeight)
            .clip(RoundedCornerShape(m.buttonRadius))
            .background(if (isPrimary) PadAccentFill else PadSurface)
            .border(
                m.strokeThin,
                if (isPrimary) PadAccent else PadBorder,
                RoundedCornerShape(m.buttonRadius)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPrimary) PadAccent else PadInk,
            fontSize = if (useCaptionFont) m.bodyFont else m.keyFont,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PadConnectedReadyScreen(
    state: PadClientUiState,
    m: PadResponsiveMetrics,
    onDisconnect: () -> Unit
) {
    val lang = state.language
    val tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "CloudPlay TV" }
    val pingText = state.measuredPingMs?.let { "${it} ms" } ?: PadStrings.tr(lang, "Đang đo...", "Measuring...")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(m.outerPad)
            .testTag("pad_connected_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(m.cardRadius))
                .background(PadSurface)
                .border(m.strokeFocus, PadAccent, RoundedCornerShape(m.cardRadius))
                .padding(m.cardPad * 1.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(m.itemGap)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.6f)
            ) {
                Box(
                    modifier = Modifier
                        .size(m.dotSize)
                        .clip(CircleShape)
                        .background(PadAccent)
                )
                Text(
                    text = PadStrings.tr(
                        lang = lang,
                        vi = "Tay cầm ${state.connectedPlayerIndex} · Đã ghép đôi",
                        en = "Controller ${state.connectedPlayerIndex} · Paired"
                    ),
                    color = PadAccent,
                    fontSize = m.bodyFont,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = tvName,
                color = PadInk,
                fontSize = m.titleFont,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ping: $pingText",
                color = PadMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = m.bodyFont,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(m.buttonRadius))
                    .background(PadSurface2)
                    .border(m.strokeThin, PadDanger, RoundedCornerShape(m.buttonRadius))
                    .clickable(onClick = onDisconnect)
                    .padding(horizontal = m.cardPad * 1.2f, vertical = m.cardPad * 0.7f)
                    .testTag("pad_disconnect_btn")
            ) {
                Text(
                    text = PadStrings.tr(lang, "Ngắt kết nối", "Disconnect"),
                    color = PadDanger,
                    fontSize = m.bodyFont,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PadDisconnectedSummaryScreen(
    state: PadClientUiState,
    m: PadResponsiveMetrics,
    onReconnect: () -> Unit,
    onBackToList: () -> Unit
) {
    val lang = state.language
    val tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "CloudPlay TV" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(m.outerPad)
            .testTag("pad_disconnected_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(m.cardRadius))
                .background(PadSurface)
                .border(m.strokeThin, PadBorder, RoundedCornerShape(m.cardRadius))
                .padding(m.cardPad * 1.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(m.itemGap)
        ) {
            Text(
                text = PadStrings.tr(lang, "Đã mất kết nối với TV", "Disconnected from TV"),
                color = PadDanger,
                fontSize = m.headingFont,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = PadStrings.tr(
                    lang = lang,
                    vi = "TV đã tự nhả toàn bộ nút bấm để nhân vật trong game không bị kẹt nút.",
                    en = "TV automatically released all buttons to prevent stuck inputs in-game."
                ),
                color = PadMuted,
                fontSize = m.bodyFont,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(m.itemGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadAccentFill)
                        .border(m.strokeThin, PadAccent, RoundedCornerShape(m.buttonRadius))
                        .clickable(onClick = onReconnect)
                        .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.7f)
                        .testTag("pad_reconnect_btn")
                ) {
                    Text(
                        text = PadStrings.tr(lang, "Kết nối lại $tvName", "Reconnect"),
                        color = PadAccent,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadSurface2)
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                        .clickable(onClick = onBackToList)
                        .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.7f)
                        .testTag("pad_back_to_list_btn")
                ) {
                    Text(
                        text = PadStrings.tr(lang, "Đổi TV khác", "Change TV"),
                        color = PadInk,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
