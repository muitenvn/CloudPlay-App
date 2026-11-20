package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ApkDownloadState
import com.example.data.UpdateCheckState
import com.example.data.UpdateChecker
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tách dòng thông tin kỹ thuật (Ngày tạo, Dung lượng APK, Version Code, Commit) ra khỏi nội dung cập nhật
 * để gom thành 1 dòng nhỏ gọn mà KHÔNG bỏ bất kỳ thông tin nào.
 */
private data class ParsedReleaseNotes(
    val techMetaLine: String,
    val bodyNotes: String
)

private fun parseReleaseNotesPreservingAllInfo(rawNotes: String): ParsedReleaseNotes {
    if (rawNotes.isBlank()) return ParsedReleaseNotes("", "")
    val techParts = mutableListOf<String>()
    val bodyLines = mutableListOf<String>()

    for (rawLine in rawNotes.lines()) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue
        val lower = line.lowercase()
        val isTechLine = (lower.contains("dung lượng") || lower.contains("version code") || lower.contains("commit:")) &&
            (line.contains("|") || lower.contains("ngày tạo"))
        if (isTechLine) {
            val segments = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            techParts.addAll(segments)
        } else {
            bodyLines.add(line)
        }
    }

    val compactTechLine = techParts.joinToString(" · ")
    val joinedBody = bodyLines.joinToString("\n").ifBlank { rawNotes.trim() }
    return ParsedReleaseNotes(
        techMetaLine = compactTechLine,
        bodyNotes = joinedBody
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateCheckState.UpdateAvailable,
    isEn: Boolean,
    onDismiss: () -> Unit
) {
    val config by com.example.data.SettingsRepository.config.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notesScrollState = rememberScrollState()
    val mirrorChipsScrollState = rememberScrollState()
    val techMetaScrollState = rememberScrollState()
    val portraitOuterScrollState = rememberScrollState()
    val downloadState by UpdateChecker.downloadState.collectAsState()
    val githubReleasesUrl = UpdateChecker.FALLBACK_RELEASES_URL
    var selectedMirrorId by remember { mutableStateOf("auto") }
    val mirrorNodes = remember { UpdateChecker.FAST_MIRROR_NODES.filter { !it.isDirectGitHub } }

    val isTvUiMode = remember(config.appUiMode, context) {
        config.isEffectiveTvMode(context)
    }

    val parsedNotes = remember(updateInfo.releaseNotes) {
        parseReleaseNotesPreservingAllInfo(updateInfo.releaseNotes)
    }

    // Các FocusRequester cho điều hướng D-Pad mượt mà, không kẹt focus
    val fastBtnFocusRequester = remember { FocusRequester() }
    val normalBtnFocusRequester = remember { FocusRequester() }
    val laterBtnFocusRequester = remember { FocusRequester() }
    val openWebBtnFocusRequester = remember { FocusRequester() }
    val installBtnFocusRequester = remember { FocusRequester() }
    val skipMirrorBtnFocusRequester = remember { FocusRequester() }
    val cancelDownloadBtnFocusRequester = remember { FocusRequester() }
    val notesBoxFocusRequester = remember { FocusRequester() }

    val totalChipCount = mirrorNodes.size + 1
    val chipFocusRequesters = remember(totalChipCount) {
        List(totalChipCount) { FocusRequester() }
    }
    val selectedChipIndex = remember(selectedMirrorId, mirrorNodes) {
        if (selectedMirrorId == "auto") {
            0
        } else {
            val idx = mirrorNodes.indexOfFirst { it.id == selectedMirrorId }
            if (idx >= 0) idx + 1 else 0
        }
    }

    val defaultFocusRequester = when (downloadState) {
        is ApkDownloadState.VerifiedReadyToInstall -> installBtnFocusRequester
        is ApkDownloadState.Downloading -> cancelDownloadBtnFocusRequester
        is ApkDownloadState.Verifying -> laterBtnFocusRequester
        else -> fastBtnFocusRequester
    }

    LaunchedEffect(downloadState, isTvUiMode) {
        if (isTvUiMode) {
            GamepadUiNavigation.onGamepadInteraction()
        }
        for (attempt in 0..3) {
            delay(if (attempt == 0) 24L else 60L)
            val ok = runCatching {
                defaultFocusRequester.requestFocus()
                true
            }.getOrDefault(false)
            if (ok) break
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val winW = maxWidth
            val winH = maxHeight
            val isLandscapeOrWideShort = winW > winH
            val useTwoColumnLayout = isTvUiMode || isLandscapeOrWideShort

            // Mọi kích thước và cỡ chữ đều tính theo tỉ lệ cửa sổ & bù cỡ chữ hệ thống để không bao giờ bị cắt
            val shortSide = minOf(winW, winH)
            val unit = shortSide * 0.01f
            val fontScale = density.fontScale.coerceAtLeast(1f)
            val compactFactor = if (useTwoColumnLayout) 0.92f else 1.0f
            val textScale = (compactFactor / fontScale.coerceIn(1f, 1.75f))

            fun rDp(v: Float): Dp = unit * (v * compactFactor)
            fun rSp(v: Float): TextUnit = with(density) { (unit * v * textScale).toSp() }

            val cardWidthFraction = if (useTwoColumnLayout) 0.94f else 0.92f
            val cardMaxHeight = winH * (if (useTwoColumnLayout) 0.92f else 0.90f)
            val cardCorner = rDp(4.8f)
            val sectionCorner = rDp(2.8f)
            val btnCorner = rDp(2.6f)
            val chipCorner = rDp(1.9f)
            val padOuter = rDp(3.4f)
            val padInner = rDp(2.4f)
            val gapLarge = rDp(2.2f)
            val gapMed = rDp(1.6f)
            val gapSmall = rDp(1.1f)
            val borderThin = (unit * 0.28f).coerceAtLeast(winW * 0.0012f)
            val iconBoxSize = rDp(8.8f)
            val iconSize = rDp(5.0f)
            val smallIconSize = rDp(3.2f)

            val selectedNodeName = mirrorNodes.firstOrNull { it.id == selectedMirrorId }?.name
            val startFastDownload: () -> Unit = {
                UpdateChecker.startVerifiedApkDownload(
                    context = context,
                    updateInfo = updateInfo,
                    preferredMirrorId = selectedMirrorId
                )
            }
            val startNormalDownload: () -> Unit = {
                UpdateChecker.startVerifiedApkDownload(
                    context = context,
                    updateInfo = updateInfo,
                    preferredMirrorId = "github_direct"
                )
            }
            val openBrowserAction: () -> Unit = {
                try {
                    val targetUrl = updateInfo.releaseUrl.takeIf {
                        it.startsWith("https://github.com/muitenvn/Ravno-App/releases") ||
                            it.startsWith("https://github.com/muitenvn/CloudPlay-App/releases")
                    } ?: githubReleasesUrl
                    com.example.data.AppLogger.info("UI", "Mở trang bản phát hành GitHub trên trình duyệt ngoài")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    com.example.data.AppLogger.error(
                        "UI",
                        "Lỗi mở trình duyệt tới trang GitHub Releases (${com.example.data.AppLogger.shortThrowable(e)})"
                    )
                }
            }

            @Composable
            fun HeaderAndTechLineSection() {
                Column(verticalArrangement = Arrangement.spacedBy(gapSmall)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(gapMed)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconBoxSize)
                                .clip(RoundedCornerShape(sectionCorner))
                                .background(Color(0xFF16352A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = config.tr("Đã có bản cập nhật mới", "New Update Available"),
                                color = TextPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = rSp(3.8f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isEn) {
                                    "v${updateInfo.currentVersion} ➔ v${updateInfo.latestVersion}"
                                } else {
                                    "Phiên bản hiện tại v${updateInfo.currentVersion} ➔ v${updateInfo.latestVersion}"
                                },
                                color = XboxNeonGreen,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = rSp(2.7f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Dòng thông tin kỹ thuật (Ngày tạo, Dung lượng APK, Mã phiên bản, Commit) gom thành 1 dòng nhỏ gọn, giữ đủ 100% thông tin
                    if (parsedNotes.techMetaLine.isNotBlank()) {
                        Surface(
                            color = XboxSurface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(chipCorner),
                            border = BorderStroke(borderThin, XboxCardBorder.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_dialog_tech_meta_line")
                        ) {
                            Text(
                                text = parsedNotes.techMetaLine,
                                color = TextSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = rSp(2.15f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(techMetaScrollState)
                                    .padding(horizontal = padInner * 0.8f, vertical = gapSmall * 0.55f)
                            )
                        }
                    }
                }
            }

            @Composable
            fun ReleaseNotesSection(modifier: Modifier = Modifier) {
                if (parsedNotes.bodyNotes.isNotBlank()) {
                    var isNotesFocused by remember { mutableStateOf(false) }
                    val notesInteraction = remember { MutableInteractionSource() }
                    val scrollStepPx = with(density) { rDp(10f).toPx().toInt().coerceAtLeast(24) }

                    Surface(
                        color = XboxSurface,
                        shape = RoundedCornerShape(sectionCorner),
                        border = BorderStroke(
                            width = if (isNotesFocused) borderThin * 2f else borderThin,
                            color = if (isNotesFocused) XboxNeonGreen else XboxCardBorder.copy(alpha = 0.5f)
                        ),
                        modifier = modifier
                            .fillMaxWidth()
                            .focusRequester(notesBoxFocusRequester)
                            .focusProperties {
                                if (useTwoColumnLayout) {
                                    right = defaultFocusRequester
                                } else {
                                    down = chipFocusRequesters.getOrElse(selectedChipIndex) { defaultFocusRequester }
                                }
                            }
                            .onFocusChanged { isNotesFocused = it.isFocused }
                            .onKeyEvent { ev ->
                                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                                when (ev.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (notesScrollState.value < notesScrollState.maxValue) {
                                            scope.launch {
                                                notesScrollState.animateScrollTo(
                                                    (notesScrollState.value + scrollStepPx).coerceAtMost(notesScrollState.maxValue)
                                                )
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (notesScrollState.value > 0) {
                                            scope.launch {
                                                notesScrollState.animateScrollTo(
                                                    (notesScrollState.value - scrollStepPx).coerceAtLeast(0)
                                                )
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            }
                            .focusable(interactionSource = notesInteraction)
                            .testTag("update_dialog_whats_new_section")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(notesScrollState)
                                .padding(padInner),
                            verticalArrangement = Arrangement.spacedBy(gapSmall)
                        ) {
                            Text(
                                text = config.tr("Nội dung cập nhật", "What's New"),
                                color = XboxNeonGreen,
                                fontSize = rSp(2.65f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = parsedNotes.bodyNotes,
                                color = TextPrimary,
                                fontSize = rSp(2.55f),
                                lineHeight = rSp(3.55f)
                            )
                        }
                    }
                }
            }

            @Composable
            fun DownloadProgressOrStatusSection() {
                when (val dl = downloadState) {
                    is ApkDownloadState.Downloading -> {
                        val mbDone = dl.downloadedBytes / (1024f * 1024f)
                        val mbTotal = if (dl.totalBytes > 0) dl.totalBytes / (1024f * 1024f) else -1f
                        val activeNodeLabel = dl.activeMirrorName.ifBlank {
                            if (dl.usingFallbackGitHub) "GitHub Direct" else "Mirror"
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(sectionCorner))
                                .background(XboxSurface.copy(alpha = 0.9f))
                                .border(borderThin, XboxCardBorder, RoundedCornerShape(sectionCorner))
                                .padding(padInner),
                            verticalArrangement = Arrangement.spacedBy(gapSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (dl.usingFallbackGitHub) {
                                        if (isEn) {
                                            "📥 Direct GitHub (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        } else {
                                            "📥 Đang tải thường: GitHub gốc (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        }
                                    } else {
                                        if (isEn) {
                                            "⚡ Mirror: $activeNodeLabel (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        } else {
                                            "⚡ Máy chủ phụ: $activeNodeLabel (${dl.mirrorIndex}/${dl.totalMirrors})"
                                        }
                                    },
                                    color = DirectCyan,
                                    fontSize = rSp(2.4f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (mbTotal > 0f && dl.progress >= 0f) {
                                        String.format("%.1f / %.1f MB (%.0f%%)", mbDone, mbTotal, dl.progress * 100f)
                                    } else {
                                        String.format("%.1f MB", mbDone)
                                    },
                                    color = XboxNeonGreen,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = rSp(2.35f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            if (dl.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { dl.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = XboxNeonGreen,
                                    trackColor = XboxCardBorder
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = XboxNeonGreen,
                                    trackColor = XboxCardBorder
                                )
                            }
                            if (dl.failedMirrors.isNotEmpty()) {
                                Text(
                                    text = if (isEn) {
                                        "🔄 Auto-switched mirror (skipped: ${dl.failedMirrors.takeLast(3).joinToString(", ")})"
                                    } else {
                                        "🔄 Đã tự chuyển máy chủ (bỏ qua: ${dl.failedMirrors.takeLast(3).joinToString(", ")})"
                                    },
                                    color = WarningAmber,
                                    fontSize = rSp(2.25f),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("update_dialog_failover_notice")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gapSmall)
                            ) {
                                if (dl.mirrorIndex < dl.totalMirrors) {
                                    UpdateDialogFlatActionButton(
                                        text = if (isEn) "⏭ Next Mirror" else "⏭ Đổi máy chủ khác",
                                        containerColor = XboxSurfaceVariant,
                                        contentColor = DirectCyan,
                                        borderColor = DirectCyan.copy(alpha = 0.75f),
                                        focusBorderColor = DirectCyan,
                                        cornerRadius = btnCorner,
                                        borderWidth = borderThin,
                                        fontSize = rSp(2.4f),
                                        verticalPad = rDp(1.7f),
                                        horizontalPad = rDp(2.0f),
                                        focusRequester = skipMirrorBtnFocusRequester,
                                        onClick = { UpdateChecker.skipToNextMirror() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusProperties {
                                                right = cancelDownloadBtnFocusRequester
                                                down = laterBtnFocusRequester
                                                if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                                    left = notesBoxFocusRequester
                                                }
                                            }
                                            .testTag("update_dialog_skip_mirror_btn")
                                    )
                                }
                                UpdateDialogFlatActionButton(
                                    text = if (isEn) "Cancel" else "Hủy tải",
                                    containerColor = XboxSurfaceVariant,
                                    contentColor = WarningAmber,
                                    borderColor = WarningAmber.copy(alpha = 0.7f),
                                    focusBorderColor = WarningAmber,
                                    cornerRadius = btnCorner,
                                    borderWidth = borderThin,
                                    fontSize = rSp(2.4f),
                                    verticalPad = rDp(1.7f),
                                    horizontalPad = rDp(2.0f),
                                    focusRequester = cancelDownloadBtnFocusRequester,
                                    onClick = { UpdateChecker.cancelApkDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusProperties {
                                            if (dl.mirrorIndex < dl.totalMirrors) {
                                                left = skipMirrorBtnFocusRequester
                                            } else if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                                left = notesBoxFocusRequester
                                            }
                                            down = laterBtnFocusRequester
                                        }
                                        .testTag("update_dialog_cancel_download_btn")
                                )
                            }
                        }
                    }

                    is ApkDownloadState.Verifying -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(sectionCorner))
                                .background(XboxSurface.copy(alpha = 0.9f))
                                .border(borderThin, XboxCardBorder, RoundedCornerShape(sectionCorner))
                                .padding(padInner),
                            verticalArrangement = Arrangement.spacedBy(gapSmall)
                        ) {
                            Text(
                                text = if (isEn) "Verifying APK & official SHA-256 signature..." else "Đang kiểm tra gói APK & mã SHA-256...",
                                color = XboxNeonGreen,
                                fontSize = rSp(2.5f),
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = XboxNeonGreen,
                                trackColor = XboxCardBorder
                            )
                        }
                    }

                    is ApkDownloadState.VerificationFailed -> {
                        Surface(
                            color = Color(0x33FF5252),
                            shape = RoundedCornerShape(sectionCorner),
                            border = BorderStroke(borderThin, WarningAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_dialog_sha_error_box")
                        ) {
                            Text(
                                text = if (isEn) dl.messageEn else dl.messageVi,
                                color = WarningAmber,
                                fontSize = rSp(2.4f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(padInner * 0.85f)
                            )
                        }
                    }

                    is ApkDownloadState.DownloadError -> {
                        Surface(
                            color = Color(0x33FF5252),
                            shape = RoundedCornerShape(sectionCorner),
                            border = BorderStroke(borderThin, WarningAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEn) dl.messageEn else dl.messageVi,
                                color = WarningAmber,
                                fontSize = rSp(2.35f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(padInner * 0.85f)
                            )
                        }
                    }

                    is ApkDownloadState.VerifiedReadyToInstall -> {
                        UpdateDialogFlatActionButton(
                            text = if (isEn) "✓ Verified · Install Update" else "✓ Đã xác thực · Mở trình cài đặt",
                            containerColor = XboxGreen,
                            contentColor = Color.White,
                            borderColor = XboxNeonGreen,
                            focusBorderColor = XboxNeonGreen,
                            cornerRadius = btnCorner,
                            borderWidth = borderThin,
                            fontSize = rSp(2.65f),
                            verticalPad = rDp(2.0f),
                            horizontalPad = rDp(2.4f),
                            focusRequester = installBtnFocusRequester,
                            onClick = { UpdateChecker.launchApkInstaller(context, dl.apkFile) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties {
                                    down = chipFocusRequesters.getOrElse(selectedChipIndex) { fastBtnFocusRequester }
                                    if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                        left = notesBoxFocusRequester
                                    }
                                }
                                .testTag("update_dialog_install_verified_btn")
                        )
                    }

                    ApkDownloadState.Idle -> {}
                }
            }

            @Composable
            fun MirrorChipsAndDownloadActionsSection() {
                val isBusy = downloadState is ApkDownloadState.Downloading || downloadState is ApkDownloadState.Verifying

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(gapMed)
                ) {
                    if (!isBusy) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(gapSmall * 0.85f)
                        ) {
                            Text(
                                text = if (isEn) {
                                    "Select mirror server (${mirrorNodes.size} backup mirrors, auto-switches on error):"
                                } else {
                                    "Chọn máy chủ phụ (${mirrorNodes.size} cổng dự phòng, tự đổi khi lỗi):"
                                },
                                color = TextSecondary,
                                fontSize = rSp(2.3f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            @Composable
                            fun MirrorChipItem(
                                chipIdx: Int,
                                chipId: String,
                                label: String,
                                isAutoChip: Boolean,
                                testTag: String
                            ) {
                                val isSelected = selectedMirrorId == chipId
                                val accentColor = if (isAutoChip) XboxNeonGreen else DirectCyan
                                val prevReq = if (chipIdx > 0) {
                                    chipFocusRequesters[chipIdx - 1]
                                } else if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                    notesBoxFocusRequester
                                } else {
                                    chipFocusRequesters[0]
                                }
                                val nextReq = if (chipIdx + 1 < totalChipCount) {
                                    chipFocusRequesters[chipIdx + 1]
                                } else {
                                    fastBtnFocusRequester
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(chipCorner))
                                        .focusProperties {
                                            left = prevReq
                                            right = nextReq
                                            down = fastBtnFocusRequester
                                            if (!useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                                up = notesBoxFocusRequester
                                            }
                                        }
                                        .gamepadClickable(
                                            cornerRadius = chipCorner,
                                            focusBorderColor = accentColor,
                                            focusRequester = chipFocusRequesters[chipIdx],
                                            onClick = { selectedMirrorId = chipId }
                                        )
                                        .testTag(testTag),
                                    shape = RoundedCornerShape(chipCorner),
                                    color = if (isSelected) accentColor.copy(alpha = 0.20f) else XboxSurface,
                                    border = BorderStroke(
                                        borderThin,
                                        if (isSelected) accentColor else XboxCardBorder
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) accentColor else if (isAutoChip) TextPrimary else TextSecondary,
                                        fontFamily = if (isAutoChip) SpaceGroteskFontFamily else JetBrainsMonoFontFamily,
                                        fontSize = rSp(2.15f),
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(
                                            horizontal = rDp(1.75f),
                                            vertical = rDp(0.95f)
                                        )
                                    )
                                }
                            }

                            val hasExtraBannerInRightCol = downloadState != ApkDownloadState.Idle
                            val useCompactGridForChips = useTwoColumnLayout && !hasExtraBannerInRightCol && (winH / winW.value.coerceAtLeast(1f)).value >= 0.50f

                            if (useCompactGridForChips) {
                                // Trên TV / màn ngang đủ cao: xếp lưới gọn (FlowRow) để thấy toàn bộ chip và điều hướng D-Pad không kẹt
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(gapSmall * 0.7f),
                                    verticalArrangement = Arrangement.spacedBy(gapSmall * 0.65f)
                                ) {
                                    MirrorChipItem(
                                        chipIdx = 0,
                                        chipId = "auto",
                                        label = if (isEn) "⚡ Auto (${mirrorNodes.size})" else "⚡ Tự động (${mirrorNodes.size})",
                                        isAutoChip = true,
                                        testTag = "update_mirror_chip_auto"
                                    )
                                    mirrorNodes.forEachIndexed { idx, node ->
                                        MirrorChipItem(
                                            chipIdx = idx + 1,
                                            chipId = node.id,
                                            label = "#${idx + 1} ${node.name}",
                                            isAutoChip = false,
                                            testTag = "update_mirror_chip_${node.id}"
                                        )
                                    }
                                }
                            } else {
                                // Trên màn ngang thấp hoặc điện thoại dọc: cuộn ngang 1 dòng gọn gàng để nhường trọn chiều cao cho 4 nút hành động luôn hiện đủ ngay lập tức
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(mirrorChipsScrollState),
                                    horizontalArrangement = Arrangement.spacedBy(gapSmall * 0.85f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MirrorChipItem(
                                        chipIdx = 0,
                                        chipId = "auto",
                                        label = if (isEn) "⚡ Auto (${mirrorNodes.size})" else "⚡ Tự động (${mirrorNodes.size})",
                                        isAutoChip = true,
                                        testTag = "update_mirror_chip_auto"
                                    )
                                    mirrorNodes.forEachIndexed { idx, node ->
                                        MirrorChipItem(
                                            chipIdx = idx + 1,
                                            chipId = node.id,
                                            label = "#${idx + 1} ${node.name}",
                                            isAutoChip = false,
                                            testTag = "update_mirror_chip_${node.id}"
                                        )
                                    }
                                }
                            }
                        }

                        // Nút 1 (Chính - Focus mặc định): Tải nhanh qua máy chủ phụ
                        UpdateDialogFlatActionButton(
                            text = if (selectedNodeName != null) {
                                if (isEn) "⚡ Download via $selectedNodeName" else "⚡ Tải qua máy chủ phụ: $selectedNodeName"
                            } else {
                                if (isEn) "⚡ Fast download via mirror" else "⚡ Tải nhanh qua máy chủ phụ"
                            },
                            containerColor = XboxNeonGreen,
                            contentColor = Color(0xFF03140B),
                            borderColor = XboxNeonGreen,
                            focusBorderColor = Color.White,
                            cornerRadius = btnCorner,
                            borderWidth = borderThin,
                            fontSize = rSp(2.65f),
                            verticalPad = rDp(1.85f),
                            horizontalPad = rDp(2.2f),
                            focusRequester = fastBtnFocusRequester,
                            onClick = startFastDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties {
                                    up = chipFocusRequesters.getOrElse(selectedChipIndex) { chipFocusRequesters[0] }
                                    down = normalBtnFocusRequester
                                    if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                        left = notesBoxFocusRequester
                                    }
                                }
                                .testTag("update_dialog_fast_cdn_btn")
                        )

                        // Nút 2: Tải thường trực tiếp từ GitHub gốc
                        UpdateDialogFlatActionButton(
                            text = if (isEn) {
                                "📥 Normal Download (Direct from GitHub)"
                            } else {
                                "📥 Tải thường (Trực tiếp từ GitHub gốc)"
                            },
                            containerColor = XboxSurface,
                            contentColor = DirectCyan,
                            borderColor = DirectCyan,
                            focusBorderColor = DirectCyan,
                            cornerRadius = btnCorner,
                            borderWidth = borderThin,
                            fontSize = rSp(2.5f),
                            verticalPad = rDp(1.7f),
                            horizontalPad = rDp(2.0f),
                            focusRequester = normalBtnFocusRequester,
                            onClick = startNormalDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties {
                                    up = fastBtnFocusRequester
                                    down = laterBtnFocusRequester
                                    if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                        left = notesBoxFocusRequester
                                    }
                                }
                                .testTag("update_dialog_normal_direct_btn")
                        )
                    }

                    // Hàng nút phụ: "Để sau" và "Mở web GitHub"
                    val upFromBottomRow = when (downloadState) {
                        is ApkDownloadState.Downloading -> cancelDownloadBtnFocusRequester
                        is ApkDownloadState.Verifying -> laterBtnFocusRequester
                        else -> normalBtnFocusRequester
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gapMed)
                    ) {
                        UpdateDialogFlatActionButton(
                            text = if (isEn) "Later" else "Để sau",
                            containerColor = XboxSurface,
                            contentColor = TextSecondary,
                            borderColor = XboxCardBorder,
                            focusBorderColor = XboxNeonGreen,
                            cornerRadius = btnCorner,
                            borderWidth = borderThin,
                            fontSize = rSp(2.45f),
                            verticalPad = rDp(1.65f),
                            horizontalPad = rDp(1.9f),
                            gamepadHint = "B",
                            focusRequester = laterBtnFocusRequester,
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .focusProperties {
                                    up = upFromBottomRow
                                    right = openWebBtnFocusRequester
                                    if (useTwoColumnLayout && parsedNotes.bodyNotes.isNotBlank()) {
                                        left = notesBoxFocusRequester
                                    }
                                }
                                .testTag("update_dialog_later_btn")
                        )

                        UpdateDialogFlatActionButton(
                            text = if (isEn) "Open GitHub Web" else "Mở web GitHub",
                            containerColor = XboxSurface,
                            contentColor = DirectCyan,
                            borderColor = DirectCyan.copy(alpha = 0.65f),
                            focusBorderColor = DirectCyan,
                            cornerRadius = btnCorner,
                            borderWidth = borderThin,
                            fontSize = rSp(2.4f),
                            verticalPad = rDp(1.65f),
                            horizontalPad = rDp(1.9f),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = DirectCyan,
                                    modifier = Modifier.size(smallIconSize)
                                )
                            },
                            focusRequester = openWebBtnFocusRequester,
                            onClick = openBrowserAction,
                            modifier = Modifier
                                .weight(1.35f)
                                .focusProperties {
                                    up = upFromBottomRow
                                    left = laterBtnFocusRequester
                                }
                                .testTag("update_dialog_open_browser_btn")
                        )
                    }
                }
            }

            GamepadSurfaceController(
                enabled = true,
                defaultFocusRequester = defaultFocusRequester,
                autoRequestDefaultFocus = true,
                attachWindowMotionListener = true,
                onBackPress = onDismiss
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(cardWidthFraction)
                        .heightIn(max = cardMaxHeight)
                        .clip(RoundedCornerShape(cardCorner))
                        .border(borderThin, XboxCardBorder, RoundedCornerShape(cardCorner))
                        .testTag("update_available_dialog"),
                    shape = RoundedCornerShape(cardCorner),
                    colors = CardDefaults.cardColors(containerColor = XboxSurfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = rDp(0f))
                ) {
                    if (useTwoColumnLayout) {
                        // BỐ CỤC 2 CỘT CHO GIAO DIỆN TV & MÀN NGANG / RỘNG VÀ THẤP:
                        // Cột trái: Tiêu đề, Phiên bản hiện tại -> mới, Dòng kỹ thuật gọn, Nội dung cập nhật (cuộn được)
                        // Cột phải: Chọn máy chủ phụ + Các nút Tải nhanh, Tải thường, Để sau, Mở web GitHub (luôn thấy ngay)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(padOuter),
                            horizontalArrangement = Arrangement.spacedBy(gapLarge),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.08f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(gapMed)
                            ) {
                                HeaderAndTechLineSection()
                                ReleaseNotesSection(modifier = Modifier.weight(1f))
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1.0f),
                                verticalArrangement = Arrangement.spacedBy(gapMed)
                            ) {
                                DownloadProgressOrStatusSection()
                                MirrorChipsAndDownloadActionsSection()
                            }
                        }
                    } else {
                        // BỐ CỤC 1 CỘT DỌC TRÊN ĐIỆN THOẠI DỌC (giữ nguyên bố cục hiện tại)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(portraitOuterScrollState)
                                .padding(padOuter),
                            verticalArrangement = Arrangement.spacedBy(gapMed)
                        ) {
                            HeaderAndTechLineSection()
                            ReleaseNotesSection(
                                modifier = Modifier.heightIn(max = winH * 0.25f)
                            )
                            DownloadProgressOrStatusSection()
                            MirrorChipsAndDownloadActionsSection()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nút hành động phẳng hỗ trợ cả cảm ứng và D-Pad TV với vòng chọn phẳng nổi bật và gợi ý phím A/B khi nhận diện tay cầm.
 */
@Composable
private fun UpdateDialogFlatActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    focusBorderColor: Color,
    cornerRadius: Dp,
    borderWidth: Dp,
    fontSize: TextUnit,
    verticalPad: Dp,
    horizontalPad: Dp,
    gamepadHint: String? = null,
    focusRequester: FocusRequester? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val hasHardwarePad = remember { isPhysicalGamepadConnectedNow() }
    val remotePadServerState by com.example.cloudplay.RemotePadTvServer.state.collectAsState()
    val showGamepadHint = GamepadUiNavigation.isGamepadModeActive ||
        GamepadUiNavigation.keepFocusVisibleInTvMode ||
        hasHardwarePad ||
        remotePadServerState.connectedDeviceId.isNotBlank()
    val effectiveHint = when {
        !showGamepadHint -> null
        !gamepadHint.isNullOrBlank() -> gamepadHint
        isFocused -> "A"
        else -> null
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .onFocusChanged { isFocused = it.isFocused }
            .gamepadClickable(
                cornerRadius = cornerRadius,
                focusBorderColor = focusBorderColor,
                focusRequester = focusRequester,
                onClick = onClick
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(
            width = if (isFocused) borderWidth * 2.2f else borderWidth,
            color = if (isFocused) focusBorderColor else borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = verticalPad),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Box(modifier = Modifier.padding(end = horizontalPad * 0.3f))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (effectiveHint != null) {
                val isBrightPrimary = containerColor == XboxNeonGreen && effectiveHint == "A"
                val badgeBg = when {
                    isBrightPrimary -> Color(0xFF04150B).copy(alpha = 0.85f)
                    effectiveHint == "B" -> com.example.ui.theme.JapanRed.copy(alpha = 0.24f)
                    else -> XboxNeonGreen.copy(alpha = 0.24f)
                }
                val badgeBorder = when {
                    isBrightPrimary -> Color(0xFF04150B)
                    effectiveHint == "B" -> com.example.ui.theme.JapanRed
                    else -> XboxNeonGreen
                }
                val badgeText = when {
                    isBrightPrimary -> XboxNeonGreen
                    effectiveHint == "B" -> Color(0xFFFFB4AB)
                    else -> XboxNeonGreen
                }
                Box(modifier = Modifier.padding(start = horizontalPad * 0.35f))
                Box(
                    modifier = Modifier
                        .size(verticalPad * 2.1f)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(badgeBg)
                        .border(borderWidth, badgeBorder, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = effectiveHint,
                        color = badgeText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize * 0.8f,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
