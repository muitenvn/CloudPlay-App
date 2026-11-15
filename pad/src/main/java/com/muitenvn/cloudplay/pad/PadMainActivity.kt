package com.muitenvn.cloudplay.pad

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muitenvn.cloudplay.protocol.RemotePadProtocol
import java.util.Locale

class PadMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            CloudPlayPadRootScreen()
        }
    }
}

internal val PadBg = Color(0xFF0B0F0E)
internal val PadSurface = Color(0xFF131A17)
internal val PadSurface2 = Color(0xFF1A2420)
internal val PadBorder = Color(0xFF26352E)
internal val PadInk = Color(0xFFEAF2EE)
internal val PadMuted = Color(0xFF8EA399)
internal val PadAccent = Color(0xFF2EE56B)
internal val PadDanger = Color(0xFFFF5B63)

internal fun padTr(vi: String, en: String): String {
    val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
    return if (lang == "vi") vi else en
}

@Composable
fun CloudPlayPadRootScreen() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PadBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("cloudplay_pad_root")
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        val scale = (minDim / 380.dp).coerceIn(0.80f, 1.35f)
        val padOuter = (20.dp * scale).coerceAtLeast(14.dp)
        val cardRadius = (16.dp * scale).coerceAtLeast(12.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padOuter),
            verticalArrangement = Arrangement.spacedBy((14.dp * scale).coerceAtLeast(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CloudPlay Pad",
                        color = PadInk,
                        fontSize = (22f * scale).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = padTr(
                            "Tay cầm không dây qua mạng nội bộ cho CloudPlay TV",
                            "LAN Wireless Controller for CloudPlay TV"
                        ),
                        color = PadMuted,
                        fontSize = (13f * scale).sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((10.dp * scale).coerceAtLeast(8.dp)))
                        .background(PadSurface2)
                        .border(1.dp, PadBorder, RoundedCornerShape((10.dp * scale).coerceAtLeast(8.dp)))
                        .padding(
                            horizontal = (10.dp * scale).coerceAtLeast(8.dp),
                            vertical = (6.dp * scale).coerceAtLeast(4.dp)
                        )
                ) {
                    Text(
                        text = "Protocol v${RemotePadProtocol.PROTOCOL_VERSION}",
                        color = PadAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (12f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cardRadius))
                    .border(1.dp, PadBorder, RoundedCornerShape(cardRadius))
                    .testTag("pad_discovery_status_card"),
                color = PadSurface,
                shape = RoundedCornerShape(cardRadius)
            ) {
                Column(
                    modifier = Modifier.padding((16.dp * scale).coerceAtLeast(12.dp)),
                    verticalArrangement = Arrangement.spacedBy((8.dp * scale).coerceAtLeast(6.dp))
                ) {
                    Text(
                        text = padTr("Đang chờ tìm TV trong mạng Wi-Fi / LAN", "Ready to discover TV on Wi-Fi / LAN"),
                        color = PadInk,
                        fontSize = (16f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = padTr(
                            "Dịch vụ: ${RemotePadProtocol.NSD_SERVICE_TYPE} • Không đăng nhập Xbox • Không dùng VPN",
                            "Service: ${RemotePadProtocol.NSD_SERVICE_TYPE} • No Xbox sign-in • No VPN"
                        ),
                        color = PadMuted,
                        fontSize = (13f * scale).sp
                    )
                    Spacer(modifier = Modifier.height((4.dp * scale).coerceAtLeast(2.dp)))
                    Text(
                        text = padTr(
                            "Mở CloudPlay trên TV -> Cài đặt -> Dùng điện thoại làm tay cầm để lấy mã ghép đôi 4 số.",
                            "Open CloudPlay on TV -> Settings -> Use Phone as Controller to get the 4-digit pairing code."
                        ),
                        color = PadAccent,
                        fontSize = (13f * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
