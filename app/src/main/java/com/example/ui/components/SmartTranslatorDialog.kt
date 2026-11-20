package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.cloudplay.GoogleGamingTranslator
import com.example.cloudplay.XboxBridge
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGlassBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartTranslatorDialog(
    onDismissRequest: () -> Unit,
    onForceTranslatePage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config by SettingsRepository.config.collectAsState()
    val isEn = config.isEnglishUI()
    val translationInfo by XboxBridge.translationInfo.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dịch Nhanh, 1: Quy Tắc Dịch, 2: Cài Đặt xCloud
    var inputText by remember { mutableStateOf("Press A button to play Forza Horizon 5 at 60fps on Xbox Cloud Gaming.") }
    var translationResult by remember { mutableStateOf<GoogleGamingTranslator.SmartTranslationResult?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val firstTabFocusRequester = remember { FocusRequester() }
    val closeBtnFocusRequester = remember { FocusRequester() }

    val tabLabels = remember(isEn) {
        listOf(
            0 to if (isEn) "Quick Translate" else "Dịch Nhanh",
            1 to if (isEn) "Rules" else "Quy Tắc Dịch",
            2 to if (isEn) "xCloud Options" else "Cài Đặt xCloud"
        )
    }

    AdaptivePopupFrame(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        dialogTestTag = "smart_translator_dialog",
        defaultFocusRequester = firstTabFocusRequester,
        wideWidthFraction = 0.95f,
        portraitWidthFraction = 0.95f,
        onPreviousTab = { selectedTab = (selectedTab - 1 + 3) % 3 },
        onNextTab = { selectedTab = (selectedTab + 1) % 3 },
        onBackPress = onDismissRequest
    ) {
        @Composable
        fun HeaderSection() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapMed),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(btnCorner),
                        color = XboxNeonGreen.copy(alpha = 0.18f),
                        border = BorderStroke(borderThin, XboxNeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.size(iconBoxSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GTranslate,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "XBOX MENU TRANSLATOR" else "DỊCH GIAO DIỆN XBOX",
                            color = TextPrimary,
                            fontSize = rSp(14f),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEn) "Translates web menus • Keeps game titles original" else "Dịch menu trang web Xbox • Giữ nguyên tên game",
                            color = XboxNeonGreen,
                            fontSize = rSp(10.5f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = XboxSurface,
                    border = BorderStroke(borderThin, XboxCardBorder),
                    modifier = Modifier
                        .size(rDp(30f))
                        .clip(CircleShape)
                        .gamepadClickable(
                            cornerRadius = rDp(15f),
                            focusRequester = closeBtnFocusRequester,
                            onClick = onDismissRequest
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEn) "Close" else "Đóng",
                            tint = TextSecondary,
                            modifier = Modifier.size(rDp(16f))
                        )
                    }
                }
            }
        }

        @Composable
        fun SelectedTabContent(maxSectionHeight: Dp) {
            when (selectedTab) {
                0 -> LiveTranslateSection(
                    isEn = isEn,
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    translationResult = translationResult,
                    isTranslating = isTranslating,
                    copiedFeedback = copiedFeedback,
                    maxHeight = maxSectionHeight,
                    onTranslate = {
                        if (inputText.isNotBlank()) {
                            isTranslating = true
                            scope.launch {
                                translationResult = GoogleGamingTranslator.translateWithAnalysis(
                                    rawText = inputText,
                                    targetLang = "vi",
                                    bilingual = config.bilingualMode
                                )
                                isTranslating = false
                            }
                        }
                    },
                    onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        copiedFeedback = true
                    },
                    onPasteAndTranslate = {
                        val clipText = clipboardManager.getText()?.text?.trim()
                        if (!clipText.isNullOrBlank()) {
                            inputText = clipText
                            isTranslating = true
                            scope.launch {
                                translationResult = GoogleGamingTranslator.translateWithAnalysis(
                                    rawText = clipText,
                                    targetLang = "vi",
                                    bilingual = config.bilingualMode
                                )
                                isTranslating = false
                            }
                        }
                    },
                    onClearInput = {
                        inputText = ""
                        translationResult = null
                    },
                    onSelectSample = { sample ->
                        inputText = sample
                        isTranslating = true
                        scope.launch {
                            translationResult = GoogleGamingTranslator.translateWithAnalysis(
                                rawText = sample,
                                targetLang = "vi",
                                bilingual = config.bilingualMode
                            )
                            isTranslating = false
                        }
                    }
                )
                1 -> SmartRulesSection(isEn = isEn, maxHeight = maxSectionHeight)
                2 -> SettingsSection(
                    isEn = isEn,
                    translationActive = config.isEffectiveTranslateEnabled(),
                    preserveTitles = config.preserveGameTitles,
                    bilingualMode = config.bilingualMode,
                    translatedCount = translationInfo.translatedCount,
                    maxHeight = maxSectionHeight,
                    onToggleTranslation = { SettingsRepository.updateXcloudTranslate(it) },
                    onTogglePreserveTitles = { SettingsRepository.updatePreserveGameTitles(it) },
                    onToggleBilingual = { SettingsRepository.updateBilingualMode(it) },
                    onForceTranslatePage = onForceTranslatePage,
                    onClearCache = {
                        GoogleGamingTranslator.clearCache()
                    }
                )
            }
        }

        AdaptiveTwoColumnLayout(
            leftWeight = 0.78f,
            rightWeight = 1.22f,
            leftColumn = {
                HeaderSection()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    tabLabels.forEach { (idx, label) ->
                        val isSelected = selectedTab == idx
                        AdaptiveFlatActionButton(
                            onClick = { selectedTab = idx },
                            isPrimary = isSelected,
                            containerColor = if (isSelected) Color(0xFF16352A) else XboxSurface,
                            contentColor = if (isSelected) XboxNeonGreen else TextPrimary,
                            borderColor = if (isSelected) XboxNeonGreen else XboxCardBorder,
                            focusedBorderColor = Color.White,
                            focusRequester = if (idx == 0) firstTabFocusRequester else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) XboxNeonGreen else TextPrimary,
                                fontSize = rSp(11.5f),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                AdaptiveFlatActionButton(
                    onClick = onDismissRequest,
                    isPrimary = false,
                    gamepadHint = "B",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "Close" else "Đóng",
                        color = TextSecondary,
                        fontSize = rSp(11.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            rightColumn = {
                SelectedTabContent(maxSectionHeight = winH * 0.78f)
            },
            portraitColumn = {
                HeaderSection()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(btnCorner))
                        .background(Color(0xFF0F1813))
                        .padding(rDp(3f)),
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    tabLabels.forEach { (idx, label) ->
                        val isSelected = selectedTab == idx
                        AdaptiveFlatActionButton(
                            onClick = { selectedTab = idx },
                            isPrimary = isSelected,
                            containerColor = if (isSelected) Color(0xFF16352A) else XboxSurface,
                            contentColor = if (isSelected) XboxNeonGreen else TextSecondary,
                            borderColor = if (isSelected) XboxNeonGreen else XboxCardBorder,
                            focusedBorderColor = Color.White,
                            focusRequester = if (idx == 0) firstTabFocusRequester else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) XboxNeonGreen else TextSecondary,
                                fontSize = rSp(11f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                SelectedTabContent(maxSectionHeight = winH * 0.66f)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdaptivePopupScope.LiveTranslateSection(
    isEn: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    translationResult: GoogleGamingTranslator.SmartTranslationResult?,
    isTranslating: Boolean,
    copiedFeedback: Boolean,
    maxHeight: Dp,
    onTranslate: () -> Unit,
    onCopy: (String) -> Unit,
    onPasteAndTranslate: () -> Unit,
    onClearInput: () -> Unit,
    onSelectSample: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(gapMed)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEn) "SAMPLE PHRASES:" else "MẪU CÂU THỬ NHANH:",
                color = TextSecondary,
                fontSize = rSp(10f),
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(gapSmall)) {
                Surface(
                    shape = RoundedCornerShape(chipCorner),
                    color = XboxNeonGreen.copy(alpha = 0.16f),
                    border = BorderStroke(borderThin, XboxNeonGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(chipCorner))
                        .gamepadClickable(cornerRadius = chipCorner) { onPasteAndTranslate() }
                        .testTag("translator_paste_button")
                ) {
                    Text(
                        text = if (isEn) "📋 Paste & Translate" else "📋 Dán & Dịch",
                        color = XboxNeonGreen,
                        fontSize = rSp(10f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = rDp(8f), vertical = rDp(4f))
                    )
                }
                if (inputText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(chipCorner),
                        color = Color(0xFF1E1416),
                        border = BorderStroke(borderThin, JapanRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(chipCorner))
                            .gamepadClickable(cornerRadius = chipCorner) { onClearInput() }
                            .testTag("translator_clear_button")
                    ) {
                        Text(
                            text = if (isEn) "✕ Clear" else "✕ Xóa",
                            color = JapanRed,
                            fontSize = rSp(10f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = rDp(8f), vertical = rDp(4f))
                        )
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gapSmall),
            verticalArrangement = Arrangement.spacedBy(gapSmall)
        ) {
            val samples = listOf(
                "Press A button to play Forza Horizon 5 at 60fps",
                "Starfield is an epic RPG from Bethesda Game Studios",
                "Wait time: 5 minutes left. Join Xbox Game Pass to play",
                "Use Left stick to aim and RT to fire in Halo Infinite"
            )
            samples.forEach { sample ->
                Surface(
                    shape = RoundedCornerShape(chipCorner),
                    color = Color(0xFF14241B),
                    border = BorderStroke(borderThin, XboxCardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(chipCorner))
                        .gamepadClickable(cornerRadius = chipCorner) { onSelectSample(sample) }
                ) {
                    Text(
                        text = sample,
                        color = DirectCyan,
                        fontSize = rSp(10f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = rDp(8f), vertical = rDp(4.5f))
                    )
                }
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = rDp(80f))
                .gamepadTextFieldFocus(cornerRadius = sectionCorner),
            label = {
                Text(
                    if (isEn) "Enter or paste English text (Game, Menu, Story...)" else "Nhập hoặc dán văn bản tiếng Anh (Game, Menu, Cốt truyện...)",
                    fontSize = rSp(10.5f)
                )
            },
            placeholder = {
                Text(
                    if (isEn) "e.g., Press RT to boost speed in Forza Horizon..." else "VD: Press RT to boost speed in Forza Horizon...",
                    fontSize = rSp(10.5f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = XboxNeonGreen,
                unfocusedBorderColor = XboxGlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = XboxNeonGreen,
                unfocusedLabelColor = TextSecondary,
                cursorColor = XboxNeonGreen,
                focusedContainerColor = Color(0xFF0F1712),
                unfocusedContainerColor = Color(0xFF0F1712)
            ),
            shape = RoundedCornerShape(sectionCorner),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onTranslate() })
        )

        val canTranslate = !isTranslating && inputText.isNotBlank()
        AdaptiveFlatActionButton(
            onClick = onTranslate,
            enabled = canTranslate,
            isPrimary = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(rDp(15f)),
                    color = Color.Black,
                    strokeWidth = rDp(2f)
                )
                Spacer(modifier = Modifier.width(gapSmall))
                Text(
                    if (isEn) "Translating..." else "Đang dịch...",
                    fontWeight = FontWeight.Bold,
                    fontSize = rSp(11.5f),
                    color = Color(0xFF04150B)
                )
            } else {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFF04150B),
                    modifier = Modifier.size(rDp(15f))
                )
                Spacer(modifier = Modifier.width(gapSmall))
                Text(
                    if (isEn) "TRANSLATE TEXT (KEEP GAME TITLES)" else "DỊCH VĂN BẢN (GIỮ NGUYÊN TÊN GAME)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rSp(11.5f),
                    color = Color(0xFF04150B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (translationResult != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(sectionCorner))
                    .border(borderThin, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(sectionCorner)),
                color = Color(0xFF0D1812)
            ) {
                Column(
                    modifier = Modifier.padding(padInner),
                    verticalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(gapSmall)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = XboxNeonGreen,
                                modifier = Modifier.size(rDp(14f))
                            )
                            Text(
                                text = if (isEn) "VIETNAMESE TRANSLATION RESULT" else "KẾT QUẢ DỊCH TIẾNG VIỆT",
                                color = XboxNeonGreen,
                                fontSize = rSp(10.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AdaptiveFlatActionButton(
                            onClick = { onCopy(translationResult.translatedText) },
                            isPrimary = false,
                            containerColor = XboxSurface,
                            contentColor = if (copiedFeedback) XboxNeonGreen else TextSecondary
                        ) {
                            Icon(
                                imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = if (isEn) "Copy" else "Sao chép",
                                tint = if (copiedFeedback) XboxNeonGreen else TextSecondary,
                                modifier = Modifier.size(rDp(14f))
                            )
                        }
                    }

                    Text(
                        text = translationResult.translatedText,
                        color = TextPrimary,
                        fontSize = rSp(12.5f),
                        fontWeight = FontWeight.Medium,
                        lineHeight = rSp(17.5f)
                    )

                    if (translationResult.protectedTerms.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(chipCorner),
                            color = Color(0xFF14251B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(rDp(8f)),
                                verticalArrangement = Arrangement.spacedBy(rDp(3f))
                            ) {
                                Text(
                                    text = if (isEn) "KEPT ORIGINAL (NOT TRANSLATED):" else "CÁC TỪ GIỮ NGUYÊN KHÔNG DỊCH:",
                                    color = DirectCyan,
                                    fontSize = rSp(9.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(rDp(4f)),
                                    verticalArrangement = Arrangement.spacedBy(rDp(4f))
                                ) {
                                    translationResult.protectedTerms.forEach { term ->
                                        Surface(
                                            shape = RoundedCornerShape(rDp(4f)),
                                            color = DirectCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = term,
                                                color = DirectCyan,
                                                fontSize = rSp(9f),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = rDp(6f), vertical = rDp(2f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (translationResult.isFromCache) {
                                if (isEn) "⚡ Translation Cache" else "⚡ Bộ nhớ đệm từ điển"
                            } else {
                                if (isEn) "⏱️ Response time: ${translationResult.executionTimeMs}ms" else "⏱️ Thời gian phản hồi: ${translationResult.executionTimeMs}ms"
                            },
                            color = TextSecondary,
                            fontSize = rSp(9f)
                        )
                        Text(
                            text = translationResult.engine,
                            color = TextSecondary,
                            fontSize = rSp(9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptivePopupScope.SmartRulesSection(
    isEn: Boolean,
    maxHeight: Dp
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(gapMed)
    ) {
        Text(
            text = if (isEn) "TRANSLATION RULES (WHAT IS KEPT & WHAT IS TRANSLATED)" else "QUY TẮC DỊCH (TỪ GIỮ NGUYÊN & TỪ ĐƯỢC DỊCH)",
            color = XboxNeonGreen,
            fontSize = rSp(11.5f),
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(sectionCorner))
                .border(borderThin, JapanRed.copy(alpha = 0.6f), RoundedCornerShape(sectionCorner))
                .gamepadClickable(cornerRadius = sectionCorner) {},
            color = Color(0xFF1B1012)
        ) {
            Column(
                modifier = Modifier.padding(padInner),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = JapanRed, modifier = Modifier.size(rDp(15f)))
                    Text(
                        text = if (isEn) "1. KEPT ORIGINAL (NOT TRANSLATED)" else "1. CÁC MỤC GIỮ NGUYÊN GỐC",
                        color = JapanRed,
                        fontSize = rSp(11f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isEn) "• Game Titles & Publishers: Halo, Forza Horizon, Starfield, Minecraft, Palworld, Call of Duty, Cyberpunk 2077, EA Play, Ubisoft, Bethesda Softworks..." else "• Tên Game & Thương hiệu gốc: Halo, Forza Horizon, Starfield, Minecraft, Palworld, Call of Duty, Cyberpunk 2077, EA Play, Ubisoft, Bethesda Softworks...",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• Console Controller Buttons: A, B, X, Y, LB, RB, LT, RT, D-Pad, Left Stick, Right Stick, Menu, View, Xbox Nexus Button." else "• Nút bấm tay cầm console: A, B, X, Y, LB, RB, LT, RT, D-Pad, Left Stick, Right Stick, Menu, View, Xbox Nexus Button.",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• Video & Audio Streams: WebRTC stream, <video>, <canvas>, audio tags (untouched for smooth 60fps streaming)." else "• Luồng hình ảnh & âm thanh: Video stream WebRTC, thẻ <video>, <canvas>, audio (giữ nguyên để stream 60fps mượt mà).",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• Technical Specs: FPS, Bitrate Mbps, Ping ms, 1080p, 60fps, 4K HDR, Clarity Boost, H.264 / AV1 Codec." else "• Thông số kỹ thuật: FPS, Bitrate Mbps, Ping ms, 1080p, 60fps, 4K HDR, Clarity Boost, Codec H.264 / AV1.",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• User Account Data: GamerTag, Player IDs, URLs, Auth Tokens." else "• Tài khoản người dùng: GamerTag, ID người chơi, URLs liên kết, Tokens.",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(sectionCorner))
                .border(borderThin, XboxNeonGreen.copy(alpha = 0.6f), RoundedCornerShape(sectionCorner))
                .gamepadClickable(cornerRadius = sectionCorner) {},
            color = Color(0xFF101C15)
        ) {
            Column(
                modifier = Modifier.padding(padInner),
                verticalArrangement = Arrangement.spacedBy(gapSmall)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    Icon(Icons.Default.GTranslate, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(rDp(15f)))
                    Text(
                        text = if (isEn) "2. TRANSLATED ITEMS" else "2. CÁC MỤC ĐƯỢC DỊCH",
                        color = XboxNeonGreen,
                        fontSize = rSp(11f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isEn) "• Game Synopsis, Story Overview & Descriptions." else "• Tóm tắt cốt truyện game (Game Synopsis, Overview, Description).",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• xCloud Navigation Menus: All Games, Library, Join Game Pass, Sign In, Settings." else "• Menu điều hướng giao diện xCloud: Tất cả trò chơi, Thư viện, Tham gia Game Pass, Đăng nhập, Cài đặt.",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• Achievements, Quest Objectives & Gameplay Tips." else "• Thành tựu (Achievements), mục tiêu nhiệm vụ, mẹo chơi game.",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• Queue Notifications: Remaining wait time, queue position, server disconnect alerts." else "• Thông báo hàng chờ: Thời gian chờ còn lại, vị trí xếp hàng, lỗi mất kết nối máy chủ.",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
                Text(
                    text = if (isEn) "• Community Reviews, Ratings & Patch Notes." else "• Đánh giá cộng đồng, bài nhận xét, ghi chú cập nhật bản vá (Patch notes).",
                    color = TextPrimary,
                    fontSize = rSp(10f)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(sectionCorner))
                .border(borderThin, DirectCyan.copy(alpha = 0.5f), RoundedCornerShape(sectionCorner))
                .gamepadClickable(cornerRadius = sectionCorner) {},
            color = Color(0xFF0F1A1B)
        ) {
            Column(
                modifier = Modifier.padding(padInner),
                verticalArrangement = Arrangement.spacedBy(rDp(4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapSmall)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(rDp(14f)))
                    Text(
                        text = if (isEn) "HOW IT WORKS" else "CÁCH HOẠT ĐỘNG",
                        color = DirectCyan,
                        fontSize = rSp(11f),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isEn) {
                        "1. Scans web menus quickly without slowing down your device.\n2. Translates common Xbox interface buttons using a built-in dictionary.\n3. Keeps game titles and controller button names in their original form.\n4. Translates longer descriptions and story summaries into natural Vietnamese.\n5. Saves translated phrases in memory so repeat visits open quickly."
                    } else {
                        "1. Quét nhanh các mục menu trên trang mà không làm nặng máy.\n2. Dịch nhanh các nút giao diện Xbox quen thuộc bằng từ điển có sẵn.\n3. Giữ nguyên tên riêng của trò chơi và ký hiệu nút tay cầm (A, B, X, Y, LT, RT).\n4. Dịch phần mô tả cốt truyện và thông báo sang tiếng Việt tự nhiên.\n5. Lưu tạm các câu đã dịch để lần mở sau hiển thị nhanh hơn."
                    },
                    color = TextSecondary,
                    fontSize = rSp(9.8f),
                    lineHeight = rSp(14.5f)
                )
            }
        }
    }
}

@Composable
private fun AdaptivePopupScope.SettingsSection(
    isEn: Boolean,
    translationActive: Boolean,
    preserveTitles: Boolean,
    bilingualMode: Boolean,
    translatedCount: Int,
    maxHeight: Dp,
    onToggleTranslation: (Boolean) -> Unit,
    onTogglePreserveTitles: (Boolean) -> Unit,
    onToggleBilingual: (Boolean) -> Unit,
    onForceTranslatePage: (() -> Unit)?,
    onClearCache: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(gapMed)
    ) {
        Surface(
            shape = RoundedCornerShape(btnCorner),
            color = Color(0xFF121F17),
            border = BorderStroke(borderThin, XboxCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(btnCorner))
                .gamepadClickable(enabled = !isEn, cornerRadius = btnCorner) {
                    if (!isEn) onToggleTranslation(!translationActive)
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(padInner)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEn) "Auto-Translate xCloud Web Page" else "Tự Động Dịch Trang Web xCloud",
                        color = TextPrimary,
                        fontSize = rSp(12f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isEn) {
                            "Disabled in English UI mode (Original English is kept)"
                        } else if (translationActive) {
                            "Đang bật: Đã dịch $translatedCount mục trên trang"
                        } else {
                            "Đang tắt: Hiển thị ngôn ngữ gốc"
                        },
                        color = if (translationActive) XboxNeonGreen else TextSecondary,
                        fontSize = rSp(10f)
                    )
                }
                Switch(
                    checked = translationActive,
                    onCheckedChange = null,
                    enabled = !isEn,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = XboxNeonGreen,
                        checkedTrackColor = XboxNeonGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(btnCorner),
            color = Color(0xFF121F17),
            border = BorderStroke(borderThin, XboxCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(btnCorner))
                .gamepadClickable(cornerRadius = btnCorner) {
                    onTogglePreserveTitles(!preserveTitles)
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(padInner)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEn) "Preserve Game Titles & Brands" else "Giữ Nguyên Tên Game & Thương Hiệu",
                        color = TextPrimary,
                        fontSize = rSp(12f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isEn) "Never translate 'Halo', 'Forza', 'Starfield', etc." else "Không dịch 'Halo' thành 'Hào quang', 'Forza' thành 'Sức mạnh'",
                        color = TextSecondary,
                        fontSize = rSp(10f)
                    )
                }
                Switch(
                    checked = preserveTitles,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DirectCyan,
                        checkedTrackColor = DirectCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(btnCorner),
            color = Color(0xFF121F17),
            border = BorderStroke(borderThin, XboxCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(btnCorner))
                .gamepadClickable(cornerRadius = btnCorner) {
                    onToggleBilingual(!bilingualMode)
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(padInner)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEn) "Bilingual Display Mode" else "Chế Độ Hiển Thị Song Ngữ",
                        color = TextPrimary,
                        fontSize = rSp(12f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isEn) "Show translated text with original English in parentheses" else "Hiển thị Tiếng Việt kèm từ gốc tiếng Anh trong ngoặc đơn",
                        color = TextSecondary,
                        fontSize = rSp(10f)
                    )
                }
                Switch(
                    checked = bilingualMode,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = XboxNeonGreen,
                        checkedTrackColor = XboxNeonGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }

        if (onForceTranslatePage != null && !isEn) {
            AdaptiveFlatActionButton(
                onClick = onForceTranslatePage,
                isPrimary = false,
                containerColor = Color(0xFF163222),
                contentColor = XboxNeonGreen,
                borderColor = XboxNeonGreen.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(rDp(15f)))
                Spacer(modifier = Modifier.width(gapSmall))
                Text(
                    "DỊCH TOÀN BỘ TRANG WEB HIỆN TẠI NGAY LẬP TỨC",
                    fontSize = rSp(11f),
                    fontWeight = FontWeight.Bold,
                    color = XboxNeonGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        AdaptiveFlatActionButton(
            onClick = onClearCache,
            isPrimary = false,
            containerColor = Color(0xFF241618),
            contentColor = JapanRed,
            borderColor = JapanRed.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isEn) "CLEAR TRANSLATION CACHE (REFRESH)" else "XÓA BỘ NHỚ ĐỆM BẢN DỊCH (LÀM MỚI)",
                fontSize = rSp(11f),
                fontWeight = FontWeight.Bold,
                color = JapanRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
