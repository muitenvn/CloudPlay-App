package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant

@Composable
fun UpdateAvailableDialog(
    currentVersion: String,
    latestVersion: String,
    releaseUrl: String,
    releaseNotes: String,
    isEn: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notesScrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, XboxNeonGreen.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                .testTag("update_available_dialog"),
            colors = CardDefaults.cardColors(containerColor = XboxSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(XboxNeonGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "New Update Available" else "Đã có phiên bản mới",
                            color = TextPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isEn) {
                                "v$currentVersion ➔ v$latestVersion"
                            } else {
                                "Phiên bản hiện tại v$currentVersion ➔ v$latestVersion"
                            },
                            color = XboxNeonGreen,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = if (isEn) {
                        "Version $latestVersion is now available on GitHub with stability and performance improvements."
                    } else {
                        "Phiên bản $latestVersion đã phát hành trên GitHub với các cải tiến độ mượt và ổn định kết nối."
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (releaseNotes.isNotBlank()) {
                    Surface(
                        color = XboxSurfaceVariant.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(notesScrollState)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = releaseNotes,
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("update_dialog_later_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxCardBorder)
                    ) {
                        Text(
                            text = if (isEn) "Later" else "Để sau",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("update_dialog_open_browser_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = XboxGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isEn) "View & Download" else "Xem & Tải về",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
