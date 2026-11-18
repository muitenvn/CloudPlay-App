package com.muitenvn.cloudplay.pad

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

// Bảng màu phẳng mới theo thiết kế Ravno Pad (#070B09 / #0D1511 / #142019 / #16352A / #27D884 / #3FD8EE)
internal val PadBg = Color(0xFF070B09)
internal val PadSurface = Color(0xFF0D1511)
internal val PadSurface2 = Color(0xFF142019)
internal val PadSelected = Color(0xFF16352A)
internal val PadBorder = Color(0xFF1B2B23)
internal val PadInk = Color(0xFFE8F1EC)
internal val PadMuted = Color(0xFF8AA196)
internal val PadAccent = Color(0xFF27D884)
internal val PadOnAccent = Color(0xFF04130B)
internal val PadAccentFill = Color(0xFF16352A)
internal val PadCyan = Color(0xFF3FD8EE)
internal val PadDanger = Color(0xFFF0706B)
internal val PadWarn = Color(0xFFE3B341)

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
                outerPad = unit * 4.4f,
                sectionGap = unit * 3.0f,
                itemGap = unit * 2.2f,
                cardPad = unit * 3.6f,
                cardRadius = unit * 3.8f,
                buttonRadius = unit * 2.8f,
                strokeThin = unit * 0.30f,
                strokeFocus = unit * 0.52f,
                dotSize = unit * 2.2f,
                pinBoxW = unit * 15.0f,
                pinBoxH = unit * 18.2f,
                keyHeight = unit * 12.8f,
                titleFont = (unit * 4.8f).toSp(),
                headingFont = (unit * 4.2f).toSp(),
                bodyFont = (unit * 3.4f).toSp(),
                captionFont = (unit * 2.9f).toSp(),
                pinDigitFont = (unit * 7.6f).toSp(),
                keyFont = (unit * 4.6f).toSp()
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
                PadLandscapeControllerScreen(
                    state = clientState,
                    onDisconnect = { PadNetworkClient.disconnectActiveSession(navigateToList = true) },
                    onForgetDevice = { PadNetworkClient.forgetActiveTvAndDisconnect() }
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
                        PadNetworkClient.reconnectSelectedTv()
                    },
                    onBackToList = { PadNetworkClient.backToTvList() }
                )
            }
        }
    }
}

/**
 * Màn hình 1: Tìm TV trong mạng nội bộ.
 * - Hiển thị danh sách TV phát hiện qua mDNS / LAN kèm địa chỉ IP và chip trạng thái ("Sẵn sàng", "Đã nhớ").
 * - Tên TV tự động xuống dòng, không bao giờ bị cắt chữ.
 * - Hỗ trợ Quét lại và Nhập IP thủ công.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        // 1. Thanh trên cùng: Biểu tượng + Ravno Pad + IP LAN + Nút ngôn ngữ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.85f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ravno_pad_launcher_icon),
                    contentDescription = "Ravno Pad Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(m.cardPad * 1.95f)
                        .clip(RoundedCornerShape(m.buttonRadius * 0.8f))
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius * 0.8f))
                )
                Text(
                    text = "Ravno Pad",
                    color = PadInk,
                    fontSize = m.titleFont,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PadSurface2)
                        .padding(horizontal = m.cardPad * 0.65f, vertical = m.cardPad * 0.3f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.5f)
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

        Spacer(modifier = Modifier.height(m.sectionGap * 0.8f))

        // 2. Danh sách TV tìm thấy
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(m.itemGap)
        ) {
            Text(
                text = PadStrings.tr(
                    lang = lang,
                    vi = if (state.isScanning) "Đang tìm TV trên mạng nội bộ…" else PadStrings.selectTvTitle(lang),
                    en = if (state.isScanning) "Searching for TV on local network…" else PadStrings.selectTvTitle(lang)
                ),
                color = PadMuted,
                fontSize = m.captionFont,
                fontWeight = FontWeight.SemiBold
            )

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(m.cardRadius))
                        .background(PadSurface)
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.cardRadius))
                        .padding(m.cardPad * 1.4f)
                        .testTag("pad_empty_tv_card"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(m.cardPad * 2.8f)
                            .clip(RoundedCornerShape(m.buttonRadius))
                            .background(PadSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📺",
                            fontSize = m.headingFont
                        )
                    }
                    Spacer(modifier = Modifier.height(m.itemGap))
                    Text(
                        text = PadStrings.tr(
                            lang = lang,
                            vi = if (state.isScanning) "Đang tìm TV…" else "Không thấy TV",
                            en = if (state.isScanning) "Searching for TV…" else "No TV Found"
                        ),
                        color = PadInk,
                        fontSize = m.headingFont,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(m.itemGap * 0.4f))
                    Text(
                        text = PadStrings.tr(
                            lang = lang,
                            vi = "Mở Ravno trên TV (cùng mạng Wi-Fi), vào Cài đặt → Dùng điện thoại làm tay cầm.",
                            en = "Open Ravno on your TV (same Wi-Fi), go to Settings → Use Phone as Controller."
                        ),
                        color = PadMuted,
                        fontSize = m.bodyFont,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("pad_tv_list"),
                    verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.85f)
                ) {
                    itemsIndexed(state.discoveredTvs, key = { _, item -> item.tvId }) { idx, tv ->
                        val isBusy = tv.status == TvServiceStatus.BUSY
                        val chipLabel = when {
                            tv.hasRememberToken -> PadStrings.tr(lang, "Đã nhớ", "Remembered")
                            tv.status == TvServiceStatus.HAS_CONTROLLER -> PadStrings.tr(lang, "Đang có tay cầm", "Connected")
                            tv.status == TvServiceStatus.BUSY -> PadStrings.tr(lang, "Đang bận", "Busy")
                            else -> PadStrings.tr(lang, "Sẵn sàng", "Ready")
                        }
                        val chipColor = when {
                            tv.hasRememberToken -> PadCyan
                            tv.status == TvServiceStatus.WAITING_FOR_PAIR -> PadAccent
                            tv.status == TvServiceStatus.HAS_CONTROLLER -> PadWarn
                            else -> PadMuted
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(m.cardRadius))
                                .background(if (tv.hasRememberToken) PadSelected else PadSurface)
                                .border(
                                    if (tv.hasRememberToken) m.strokeFocus else m.strokeThin,
                                    if (tv.hasRememberToken) PadAccent else PadBorder,
                                    RoundedCornerShape(m.cardRadius)
                                )
                                .clickable(enabled = !isBusy) { onSelectTv(tv) }
                                .padding(m.cardPad)
                                .testTag("pad_tv_card_$idx"),
                            horizontalArrangement = Arrangement.spacedBy(m.itemGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon TV
                            Box(
                                modifier = Modifier
                                    .size(m.cardPad * 2.2f)
                                    .clip(RoundedCornerShape(m.buttonRadius))
                                    .background(PadSurface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📺",
                                    fontSize = m.bodyFont
                                )
                            }

                            // Tên TV (tự xuống dòng, không cắt) + IP + Chip trạng thái
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.35f)
                            ) {
                                Text(
                                    text = tv.tvName,
                                    color = if (isBusy) PadMuted else PadInk,
                                    fontSize = m.bodyFont,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.6f),
                                    verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.3f)
                                ) {
                                    Text(
                                        text = tv.hostAddress,
                                        color = PadMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = m.captionFont
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(PadSurface2)
                                            .padding(
                                                horizontal = m.cardPad * 0.55f,
                                                vertical = m.cardPad * 0.16f
                                            )
                                    ) {
                                        Text(
                                            text = chipLabel,
                                            color = chipColor,
                                            fontSize = m.captionFont,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

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
                            .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.75f)
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
                            .background(PadAccent)
                            .clickable { onProbeManualIp(manualIp) }
                            .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.75f)
                            .testTag("pad_manual_ip_btn")
                    ) {
                        Text(
                            text = PadStrings.tr(lang, "Tìm IP", "Connect IP"),
                            color = PadOnAccent,
                            fontSize = m.bodyFont,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(m.sectionGap * 0.8f))

        // 3. Thanh nút hành động dưới cùng: Quét lại + Nhập IP
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.7f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.itemGap)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadAccent)
                        .clickable(onClick = onRescan)
                        .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.8f)
                        .testTag("pad_rescan_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↻ ${PadStrings.rescanButton(lang)}",
                        color = PadOnAccent,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(if (showManualIpRow) PadSelected else PadSurface2)
                        .border(
                            m.strokeThin,
                            if (showManualIpRow) PadAccent else PadBorder,
                            RoundedCornerShape(m.buttonRadius)
                        )
                        .clickable { showManualIpRow = !showManualIpRow }
                        .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.8f)
                        .testTag("pad_toggle_manual_ip_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PadStrings.tr(lang, "⌨ Nhập IP", "⌨ Enter IP"),
                        color = if (showManualIpRow) PadAccent else PadInk,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = PadStrings.localOnlyNotice(lang),
                color = PadMuted,
                fontSize = m.captionFont,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Màn hình 2: Nhập mã 4 chữ số trên TV.
 * - 4 ô số tỉ lệ chuẩn, tự tô sáng ô đang nhập.
 * - Hiển thị đếm ngược 3:00 khi nhập sai 3 lần, hoặc số lần thử còn lại.
 * - Nút "Kết nối" nổi bật + bàn phím số 3x4.
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
    val tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "Ravno TV" }
    val isLocked = state.lockoutRemainingSec > 0 || state.pinErrorReason == AuthFailReason.LOCKED_OUT
    val canSubmit = !isLocked && !state.isConnecting && state.enteredPin.length == 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(m.outerPad)
            .testTag("pad_enter_pin_screen"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Thanh trên cùng: ← Quay lại + Tiêu đề "Nhập mã" + Tên TV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(m.itemGap)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadSurface2)
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                        .clickable(onClick = onBack)
                        .padding(horizontal = m.cardPad * 0.85f, vertical = m.cardPad * 0.5f)
                        .testTag("pad_back_to_tv_list_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PadStrings.changeTvBack(lang),
                        color = PadInk,
                        fontSize = m.captionFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = PadStrings.tr(lang, "Nhập mã", "Enter Code"),
                    color = PadInk,
                    fontSize = m.headingFont,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PadSurface2)
                    .padding(horizontal = m.cardPad * 0.7f, vertical = m.cardPad * 0.3f)
            ) {
                Text(
                    text = tvName,
                    color = PadAccent,
                    fontSize = m.captionFont,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Giữa: Dòng phụ + 4 ô số PIN + Dòng trạng thái lỗi/khóa + Nút Kết nối
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.85f)
        ) {
            Text(
                text = PadStrings.enterPinSubtitle(lang),
                color = PadMuted,
                fontSize = m.bodyFont,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.85f),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("pad_pin_boxes_row")
            ) {
                for (i in 0 until 4) {
                    val charStr = state.enteredPin.getOrNull(i)?.toString().orEmpty()
                    val isCurrentSlot = !isLocked && (state.enteredPin.length == i || (i == 3 && state.enteredPin.length == 4))
                    val isError = state.pinErrorReason != null && !isLocked
                    val borderColor = when {
                        isLocked -> PadBorder
                        isError -> PadDanger
                        isCurrentSlot -> PadAccent
                        else -> PadBorder
                    }
                    val boxBg = when {
                        isCurrentSlot -> PadSelected
                        else -> PadSurface2
                    }
                    val digitColor = when {
                        isLocked -> Color(0xFF4D6156)
                        isError -> PadDanger
                        else -> PadInk
                    }
                    Box(
                        modifier = Modifier
                            .size(width = m.pinBoxW, height = m.pinBoxH)
                            .clip(RoundedCornerShape(m.cardRadius))
                            .background(boxBg)
                            .border(
                                if (isCurrentSlot || isError) m.strokeFocus else m.strokeThin,
                                borderColor,
                                RoundedCornerShape(m.cardRadius)
                            )
                            .testTag("pad_pin_box_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLocked) "–" else charStr,
                            color = digitColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = m.pinDigitFont,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Thông báo lỗi / đếm ngược khóa tạm 3 phút / đang kết nối
            val feedbackText = when {
                state.isConnecting -> PadStrings.tr(
                    lang = lang,
                    vi = "Đang xác thực kết nối với TV…",
                    en = "Verifying connection with TV…"
                )
                isLocked -> {
                    val mins = state.lockoutRemainingSec / 60
                    val secs = state.lockoutRemainingSec % 60
                    val timeStr = "%d:%02d".format(mins, secs)
                    PadStrings.tr(
                        lang = lang,
                        vi = "Sai 3 lần. Chờ $timeStr rồi nhập lại.",
                        en = "3 wrong tries. Wait $timeStr to retry."
                    )
                }
                state.pinErrorReason == AuthFailReason.WRONG_PIN -> PadStrings.tr(
                    lang = lang,
                    vi = "Mã chưa đúng. Còn ${state.remainingPinAttempts} lần thử.",
                    en = "Incorrect code. ${state.remainingPinAttempts} attempts left."
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(m.keyHeight * 0.45f),
                contentAlignment = Alignment.Center
            ) {
                if (feedbackText.isNotBlank()) {
                    Text(
                        text = feedbackText,
                        color = when {
                            state.isConnecting -> PadAccent
                            isLocked -> PadWarn
                            else -> PadDanger
                        },
                        fontSize = m.captionFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("pad_pin_feedback_text")
                    )
                }
            }

            // Nút Kết nối chính
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(m.buttonRadius))
                    .background(if (canSubmit) PadAccent else PadSurface)
                    .border(
                        m.strokeThin,
                        if (canSubmit) PadAccent else PadBorder,
                        RoundedCornerShape(m.buttonRadius)
                    )
                    .clickable(enabled = canSubmit, onClick = onSubmit)
                    .padding(vertical = m.cardPad * 0.75f)
                    .testTag("pad_submit_pin_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PadStrings.keypadConnect(lang),
                    color = if (canSubmit) PadOnAccent else Color(0xFF4D6156),
                    fontSize = m.bodyFont,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 3. Bàn phím số 3x4
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(m.itemGap * 0.7f)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )
            rows.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.7f)
                ) {
                    r.forEach { d ->
                        PadKeypadButton(
                            label = d,
                            m = m,
                            isPrimary = false,
                            onClick = { if (!isLocked) onDigit(d[0]) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pad_key_$d")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.7f)
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
                    onClick = { if (!isLocked) onDigit('0') },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pad_key_0")
                )
                PadKeypadButton(
                    label = PadStrings.keypadConnect(lang),
                    m = m,
                    isPrimary = canSubmit,
                    useCaptionFont = true,
                    onClick = { if (canSubmit) onSubmit() },
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

/**
 * Màn hình 4: Mất kết nối với TV.
 * - Hiển thị rõ trạng thái tự nhả nút an toàn và tự kết nối lại.
 * - Hai nút hành động: "Thử kết nối lại" (Chính) và "Chọn TV khác" (Phụ).
 */
@Composable
private fun PadDisconnectedSummaryScreen(
    state: PadClientUiState,
    m: PadResponsiveMetrics,
    onReconnect: () -> Unit,
    onBackToList: () -> Unit
) {
    val lang = state.language
    val tvName = state.selectedTv?.tvName.orEmpty().ifBlank { "Ravno TV" }

    val subtitleText = when (state.disconnectReasonCode) {
        "FORGOTTEN" -> PadStrings.tr(
            lang = lang,
            vi = "TV ($tvName) đã quên thiết bị này. Mọi nút bấm đã tự động nhả về trạng thái nghỉ.",
            en = "TV ($tvName) forgot this device. All buttons were automatically released to neutral."
        )
        "TV_DISCONNECT" -> PadStrings.tr(
            lang = lang,
            vi = "TV ($tvName) đã ngắt kết nối tay cầm. Mọi nút bấm đã tự động nhả về trạng thái nghỉ.",
            en = "TV ($tvName) disconnected the controller. All buttons were automatically released to neutral."
        )
        else -> PadStrings.tr(
            lang = lang,
            vi = "Kiểm tra TV còn mở và cùng Wi-Fi. Mọi nút bấm đã tự động nhả để nhân vật không bị kẹt nút.",
            en = "Check that the TV is open on the same Wi-Fi. All buttons were automatically released."
        )
    }

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
            Box(
                modifier = Modifier
                    .size(m.cardPad * 2.5f)
                    .clip(RoundedCornerShape(m.buttonRadius))
                    .background(PadSurface2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️",
                    fontSize = m.headingFont
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(m.itemGap * 0.6f)
            ) {
                Box(
                    modifier = Modifier
                        .size(m.dotSize)
                        .clip(CircleShape)
                        .background(if (state.isAutoReconnecting || state.isConnecting) PadWarn else PadDanger)
                )
                Text(
                    text = tvName,
                    color = PadMuted,
                    fontSize = m.captionFont,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = PadStrings.tr(lang, "Không còn liên lạc được với TV", "Disconnected from TV"),
                color = PadInk,
                fontSize = m.headingFont,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitleText,
                color = PadMuted,
                fontSize = m.bodyFont,
                textAlign = TextAlign.Center
            )

            if (state.isAutoReconnecting || state.isConnecting) {
                Text(
                    text = PadStrings.tr(
                        lang = lang,
                        vi = "Đang tự kết nối lại với $tvName…",
                        en = "Automatically reconnecting to $tvName…"
                    ),
                    color = PadWarn,
                    fontSize = m.captionFont,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pad_auto_reconnecting_text")
                )
            }

            Spacer(modifier = Modifier.height(m.itemGap * 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(m.itemGap)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadAccent)
                        .clickable(onClick = onReconnect)
                        .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.85f)
                        .testTag("pad_reconnect_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PadStrings.tr(lang, "Thử kết nối lại", "Try reconnecting"),
                        color = PadOnAccent,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(m.buttonRadius))
                        .background(PadSurface2)
                        .border(m.strokeThin, PadBorder, RoundedCornerShape(m.buttonRadius))
                        .clickable(onClick = onBackToList)
                        .padding(horizontal = m.cardPad, vertical = m.cardPad * 0.85f)
                        .testTag("pad_back_to_list_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = PadStrings.tr(lang, "Chọn TV khác", "Choose another TV"),
                        color = PadInk,
                        fontSize = m.bodyFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
