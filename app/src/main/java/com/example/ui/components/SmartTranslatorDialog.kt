package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cloudplay.GoogleGamingTranslator
import com.example.cloudplay.XboxBridge
import com.example.data.SettingsRepository
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxGlassBorder
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartTranslatorDialog(
    onDismissRequest: () -> Unit,
    onForceTranslatePage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config by SettingsRepository.config.collectAsState()
    val translationInfo by XboxBridge.translationInfo.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dịch Nhanh, 1: Quy Tắc Né & Dịch, 2: Tùy Chọn
    var inputText by remember { mutableStateOf("Press A button to play Forza Horizon 5 at 60fps on Xbox Cloud Gaming.") }
    var translationResult by remember { mutableStateOf<GoogleGamingTranslator.SmartTranslationResult?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(XboxNeonGreen.copy(alpha = 0.8f), DirectCyan.copy(alpha = 0.5f), Color(0xFF14221A))
                    ),
                    RoundedCornerShape(22.dp)
                )
                .testTag("smart_translator_dialog"),
            color = XboxSurface.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxHeight()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = XboxNeonGreen.copy(alpha = 0.18f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GTranslate,
                                    contentDescription = null,
                                    tint = XboxNeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "BỘ DỊCH THÔNG MINH (GOOGLE TRANSLATE)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = "Dịch mượt như Google Dịch • Tự động né tên game & stream",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F1813),
                    contentColor = XboxNeonGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = XboxNeonGreen
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Dịch Nhanh",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Quy Tắc Né & Sửa",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Cài Đặt xCloud",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content View
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> LiveTranslateSection(
                            inputText = inputText,
                            onInputTextChange = { inputText = it },
                            translationResult = translationResult,
                            isTranslating = isTranslating,
                            copiedFeedback = copiedFeedback,
                            bilingualMode = config.bilingualMode,
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
                        1 -> SmartRulesSection()
                        2 -> SettingsSection(
                            translationActive = config.xcloudTranslateEnabled,
                            preserveTitles = config.preserveGameTitles,
                            bilingualMode = config.bilingualMode,
                            hideStats = config.hideStreamStatsOverlay,
                            translatedCount = translationInfo.translatedCount,
                            onToggleTranslation = { SettingsRepository.updateXcloudTranslate(it) },
                            onTogglePreserveTitles = { SettingsRepository.updatePreserveGameTitles(it) },
                            onToggleBilingual = { SettingsRepository.updateBilingualMode(it) },
                            onToggleHideStats = { SettingsRepository.updateHideStreamStatsOverlay(it) },
                            onForceTranslatePage = onForceTranslatePage,
                            onClearCache = {
                                GoogleGamingTranslator.clearCache()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveTranslateSection(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    translationResult: GoogleGamingTranslator.SmartTranslationResult?,
    isTranslating: Boolean,
    copiedFeedback: Boolean,
    bilingualMode: Boolean,
    onTranslate: () -> Unit,
    onCopy: (String) -> Unit,
    onSelectSample: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sample chips
        Text(
            text = "MẪU CÂU THỬ NGHIỆM ĐỂ KIỂM TRA NÉ & DỊCH:",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val samples = listOf(
                "Press A button to play Forza Horizon 5 at 60fps",
                "Starfield is an epic RPG from Bethesda Game Studios",
                "Wait time: 5 minutes left. Join Xbox Game Pass to play",
                "Use Left stick to aim and RT to fire in Halo Infinite"
            )
            samples.forEach { sample ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF14241B),
                    modifier = Modifier.clickable { onSelectSample(sample) }
                ) {
                    Text(
                        text = sample,
                        color = DirectCyan,
                        fontSize = 10.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Field
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp),
            label = { Text("Nhập hoặc dán văn bản tiếng Anh (Game, Menu, Cốt truyện...)") },
            placeholder = { Text("VD: Press RT to boost speed in Forza Horizon...") },
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
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onTranslate() })
        )

        // Action Button
        Button(
            onClick = onTranslate,
            enabled = !isTranslating && inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = XboxNeonGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đang dịch thông minh...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            } else {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DỊCH BẰNG GOOGLE TRANSLATE (CÓ BẢO VỆ)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Translation Result Display
        if (translationResult != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, XboxNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                color = Color(0xFF0D1812)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(15.dp))
                            Text(
                                text = "KẾT QUẢ DỊCH TIẾNG VIỆT",
                                color = XboxNeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { onCopy(translationResult.translatedText) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Sao chép",
                                tint = if (copiedFeedback) XboxNeonGreen else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Translated text
                    Text(
                        text = translationResult.translatedText,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Protected Terms Analysis Badge
                    if (translationResult.protectedTerms.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF14251B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "🛡️ ĐÃ NÉ THÀNH CÔNG (GIỮ NGUYÊN GỐC):",
                                    color = DirectCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    translationResult.protectedTerms.forEach { term ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DirectCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = term,
                                                color = DirectCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Telemetry row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (translationResult.isFromCache) "⚡ Bộ nhớ đệm tức thì (0ms)" else "⏱️ Thời gian phản hồi: ${translationResult.executionTimeMs}ms",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                        Text(
                            text = translationResult.engine,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartRulesSection() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "HỆ THỐNG PHÂN BIỆT THÔNG MINH (NÉ GÌ & DỊCH GÌ)",
            color = XboxNeonGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // Card 1: Chỗ NÉ (Tuyệt đối không sửa)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, JapanRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            color = Color(0xFF1B1012)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = JapanRed, modifier = Modifier.size(16.dp))
                    Text(
                        text = "1. CHỖ NÉ (TUYỆT ĐỐI KHÔNG DỊCH BỪA)",
                        color = JapanRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "• Tên Game & Thương hiệu gốc: Halo, Forza Horizon, Starfield, Minecraft, Palworld, Call of Duty, Cyberpunk 2077, EA Play, Ubisoft, Bethesda Softworks...",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Nút bấm tay cầm console: A, B, X, Y, LB, RB, LT, RT, D-Pad, Left Stick, Right Stick, Menu, View, Xbox Nexus Button.",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Luồng hình ảnh & âm thanh: Video stream WebRTC, thẻ <video>, <canvas>, audio (giữ nguyên để stream 60fps mượt mà).",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Thông số kỹ thuật: FPS, Bitrate Mbps, Ping ms, 1080p, 60fps, 4K HDR, Clarity Boost, Codec H.264 / AV1.",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Tài khoản người dùng: GamerTag, ID người chơi, URLs liên kết, Tokens.",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
            }
        }

        // Card 2: Chỗ NÊN DỊCH
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, XboxNeonGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            color = Color(0xFF101C15)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.GTranslate, contentDescription = null, tint = XboxNeonGreen, modifier = Modifier.size(16.dp))
                    Text(
                        text = "2. CHỖ NÊN DỊCH (DỊCH Y HỆT GOOGLE DỊCH)",
                        color = XboxNeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "• Tóm tắt cốt truyện game (Game Synopsis, Overview, Description).",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Menu điều hướng giao diện xCloud: Tất cả trò chơi, Thư viện, Tham gia Game Pass, Đăng nhập, Cài đặt.",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Thành tựu (Achievements), mục tiêu nhiệm vụ, mẹo chơi game.",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Thông báo hàng chờ: Thời gian chờ còn lại, vị trí xếp hàng, lỗi mất kết nối máy chủ.",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "• Đánh giá cộng đồng, bài nhận xét, ghi chú cập nhật bản vá (Patch notes).",
                    color = TextPrimary,
                    fontSize = 10.sp
                )
            }
        }

        // Card 3: Công nghệ hoạt động
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DirectCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            color = Color(0xFF0F1A1B)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = DirectCyan, modifier = Modifier.size(15.dp))
                    Text(
                        text = "CƠ CHẾ HOẠT ĐỘNG THÔNG MINH",
                        color = DirectCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "1. Quét DOM bằng Native TreeWalker siêu nhẹ (<2ms).\n2. Đối chiếu từ điển UI 0ms của cộng đồng GitHub Better-xCloud.\n3. Nhận diện tokenized game titles và nút bấm để đóng gói bảo vệ.\n4. Gửi đoạn văn bản cần dịch tới Google Neural Translate qua mạng.\n5. Tự động giải mã placeholder đưa tên game nguyên bản về vị trí cũ.\n6. Lưu vào RAM Cache để lần truy cập sau hiển thị ngay lập tức không tốn data.",
                    color = TextSecondary,
                    fontSize = 9.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    translationActive: Boolean,
    preserveTitles: Boolean,
    bilingualMode: Boolean,
    hideStats: Boolean,
    translatedCount: Int,
    onToggleTranslation: (Boolean) -> Unit,
    onTogglePreserveTitles: (Boolean) -> Unit,
    onToggleBilingual: (Boolean) -> Unit,
    onToggleHideStats: (Boolean) -> Unit,
    onForceTranslatePage: (() -> Unit)?,
    onClearCache: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Toggle 1: Kích hoạt dịch tự động
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF121F17),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tự Động Dịch Trang Web xCloud", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (translationActive) "Đang bật: Đã dịch $translatedCount mục trên trang" else "Đang tắt: Hiển thị ngôn ngữ gốc",
                        color = if (translationActive) XboxNeonGreen else TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = translationActive,
                    onCheckedChange = onToggleTranslation,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = XboxNeonGreen,
                        checkedTrackColor = XboxNeonGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Toggle 2: Giữ nguyên tên game
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF121F17),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Giữ Nguyên Tên Game & Thương Hiệu", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Không dịch 'Halo' thành 'Hào quang', 'Forza' thành 'Sức mạnh'",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = preserveTitles,
                    onCheckedChange = onTogglePreserveTitles,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DirectCyan,
                        checkedTrackColor = DirectCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Toggle 3: Chế độ song ngữ
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF121F17),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chế Độ Hiển Thị Song Ngữ", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Hiển thị Tiếng Việt kèm từ gốc tiếng Anh trong ngoặc đơn",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = bilingualMode,
                    onCheckedChange = onToggleBilingual,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = XboxNeonGreen,
                        checkedTrackColor = XboxNeonGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Toggle 4: Ẩn thông số đè màn hình
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF121F17),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ẩn Bảng Decode Profile & Stream Stats", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Tự động xóa bảng thông số web gây rối mắt khi chơi game",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = hideStats,
                    onCheckedChange = onToggleHideStats,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DirectCyan,
                        checkedTrackColor = DirectCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Force Translate Entire Page Action
        if (onForceTranslatePage != null) {
            Button(
                onClick = onForceTranslatePage,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF163222),
                    contentColor = XboxNeonGreen
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DỊCH TOÀN BỘ TRANG WEB HIỆN TẠI NGAY LẬP TỨC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Clear Cache Action
        Button(
            onClick = onClearCache,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF241618),
                contentColor = JapanRed
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("XÓA BỘ NHỚ ĐỆM BẢN DỊCH (LÀM MỚI)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
