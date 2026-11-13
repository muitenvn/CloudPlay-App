package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddOrManageButtonsModal(
    config: AppConfig,
    hiddenButtons: Set<String>,
    abxySplitMode: Boolean,
    dpadSplitMode: Boolean,
    onToggleButtonVisibility: (String) -> Unit,
    onRestoreAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNodes = remember(abxySplitMode, dpadSplitMode) {
        ALL_GAMEPAD_NODES.filter { node ->
            when (node.id) {
                "abxy" -> !abxySplitMode
                "btn_a", "btn_b", "btn_x", "btn_y" -> abxySplitMode
                "dpad" -> !dpadSplitMode
                "dpad_up", "dpad_down", "dpad_left", "dpad_right" -> dpadSplitMode
                else -> true
            }
        }
    }

    Surface(
        modifier = modifier
            .widthIn(max = 440.dp)
            .testTag("virtual_gamepad_add_button_modal"),
        shape = RoundedCornerShape(22.dp),
        color = HudSurface2,
        border = BorderStroke(1.dp, HudLine),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.tr("＋ Thêm / Ẩn nút", "＋ Add / Hide Buttons"),
                    color = HudInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hiddenButtons.isNotEmpty()) {
                        HudButton(
                            text = config.tr("Hiện tất cả", "Show All"),
                            onClick = onRestoreAll,
                            minHeight = 30.dp,
                            fontSize = 11.5.sp,
                            containerOverride = HudSelBg,
                            contentOverride = HudAccent
                        )
                    }
                    HudButton(
                        text = "✕",
                        onClick = onClose,
                        minHeight = 30.dp,
                        fontSize = 11.5.sp,
                        containerOverride = HudSurface,
                        contentOverride = HudMuted
                    )
                }
            }

            Text(
                text = config.tr(
                    "Chạm vào nút bên dưới để hiện lại (+) lên màn hình hoặc ẩn (✓):",
                    "Tap a button below to add (+) it back to screen or hide (✓):"
                ),
                color = HudMuted,
                fontSize = 12.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeNodes.forEach { node ->
                    val isHidden = node.id in hiddenButtons
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHidden) HudDangerBg else HudSelBg,
                        border = BorderStroke(1.dp, if (isHidden) HudDanger else HudAccent),
                        modifier = Modifier.clickable { onToggleButtonVisibility(node.id) },
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = if (isHidden) "＋ ${node.shortBadge}" else "✓ ${node.shortBadge}",
                            color = if (isHidden) HudDanger else HudAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
