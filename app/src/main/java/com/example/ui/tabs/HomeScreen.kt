package com.example.ui.tabs

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloudplay.GamepadStatus
import com.example.data.GameBookmark
import com.example.ui.components.GuideSection
import com.example.ui.theme.DirectCyan
import com.example.ui.theme.JapanRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XboxCardBorder
import com.example.ui.theme.XboxGreen
import com.example.ui.theme.XboxNeonGreen
import com.example.ui.theme.XboxSurface
import com.example.ui.theme.XboxSurfaceVariant
import com.example.vpn.NetworkMode
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnEngine

@Composable
fun HomeScreen(
    vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    featuredGames: List<GameBookmark>,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit,
    onSwitchEngine: (VpnEngine) -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenXboxCloud: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onOpenGuide: (GuideSection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Quick Play Deck (Direct Launch for Xbox Cloud & Fortnite)
        HeroConsolePlayDeck(
            vpnState = vpnState,
            gamepadStatus = gamepadStatus,
            onOpenXboxCloud = onOpenXboxCloud,
            onLaunchFortnite = {
                onLaunchGame("https://www.xbox.com/en-US/play/games/fortnite/BT5P2X999VH2")
            }
        )

        // 2. Unified Hardware & Network Cockpit (Ping, Bypass & Gamepad in 1 clean card)
        QuickNetworkCockpitCard(
            vpnState = vpnState,
            gamepadStatus = gamepadStatus,
            onConnectVpn = onConnectVpn,
            onDisconnectVpn = onDisconnectVpn
        )

        // 3. Featured Games Carousel (1-Tap Launch)
        FeaturedGamesSection(
            featuredGames = featuredGames,
            onLaunchGame = onLaunchGame,
            onNavigateToLibrary = onNavigateToLibrary
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeroConsolePlayDeck(
    vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    onOpenXboxCloud: () -> Unit,
    onLaunchFortnite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(XboxNeonGreen.copy(alpha = 0.6f), DirectCyan.copy(alpha = 0.5f), XboxNeonGreen.copy(alpha = 0.6f))
                ),
                RoundedCornerShape(22.dp)
            )
            .testTag("hero_console_play_deck"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Art: New AAA Xbox Cloud artwork
            Image(
                painter = painterResource(id = R.drawable.gamepass_hub_banner_1791394609676),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )

            // Dark Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color(0xD9080D0A),
                                Color(0xF7080D0A)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xD90D1C13),
                        border = androidx.compose.foundation.BorderStroke(1.dp, XboxNeonGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (vpnState.mode) {
                                            NetworkMode.DIRECT_NETWORK -> DirectCyan
                                            NetworkMode.VPN_JAPAN -> JapanRed
                                            NetworkMode.DISCONNECTED -> Color.Gray
                                        }
                                    )
                            )
                            Text(
                                text = when (vpnState.mode) {
                                    NetworkMode.DIRECT_NETWORK -> "Direct Handshake ⚡"
                                    NetworkMode.VPN_JAPAN -> "Tokyo Proxy 🇯🇵"
                                    NetworkMode.DISCONNECTED -> stringResource(R.string.server_testing)
                                },
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "XBOX CLOUD",
                            color = XboxNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }

                // Dual Launch Buttons (Xbox Cloud + Fortnite Free)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenXboxCloud,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("hero_launch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = XboxGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_launch_xcloud),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = onLaunchFortnite,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_launch_fortnite_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0074E4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_play_fortnite),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                // Features Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "🌐 " + stringResource(R.string.section_app_language),
                            color = XboxNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "⚡ 0ms Direct Lag Free",
                            color = DirectCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNetworkCockpitCard(
    vpnState: VpnConnectionState,
    gamepadStatus: GamepadStatus,
    onConnectVpn: () -> Unit,
    onDisconnectVpn: () -> Unit
) {
    val isConnected = vpnState.mode != NetworkMode.DISCONNECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(18.dp))
            .testTag("network_cockpit_card"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Network & Ping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF16291E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = XboxNeonGreen,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.cockpit_title),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${vpnState.currentServer.flag} ${vpnState.currentServer.name}",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Ping Meter Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isConnected) Color(0xFF14241B) else Color(0xFF202020),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (isConnected) XboxNeonGreen.copy(alpha = 0.5f) else Color.DarkGray
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isConnected) XboxNeonGreen else Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isConnected) "${vpnState.directLatencyMs ?: 28} ms" else "-- ms",
                            color = if (isConnected) XboxNeonGreen else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Row 2: Controller & Touch Hardware Status
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F1A13)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = if (gamepadStatus.hasPhysicalController) XboxNeonGreen else DirectCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (gamepadStatus.hasPhysicalController)
                                gamepadStatus.controllerName
                            else
                                stringResource(R.string.virtual_gamepad_title),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = if (gamepadStatus.hasPhysicalController) stringResource(R.string.controller_connected) else stringResource(R.string.controller_touch),
                        color = if (gamepadStatus.hasPhysicalController) XboxNeonGreen else DirectCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Row 3: Connect / Disconnect Toggle Button
            Button(
                onClick = {
                    if (isConnected) onDisconnectVpn() else onConnectVpn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("quick_vpn_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFF1E3526) else XboxGreen
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (isConnected) XboxNeonGreen else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) stringResource(R.string.net_mode_direct) else stringResource(R.string.btn_enable_vpn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedGamesSection(
    featuredGames: List<GameBookmark>,
    onLaunchGame: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.featured_games_title),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateToLibrary() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.btn_view_all),
                    color = XboxNeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = XboxNeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(featuredGames, key = { it.id }) { game ->
                FeaturedGameCard(game = game, onLaunch = { onLaunchGame(game.playUrl) })
            }
        }
    }
}

@Composable
private fun FeaturedGameCard(
    game: GameBookmark,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, XboxCardBorder, RoundedCornerShape(16.dp))
            .clickable { onLaunch() }
            .testTag("featured_game_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = XboxSurface)
    ) {
        Column {
            // Poster Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                if (game.drawableResId != null) {
                    Image(
                        painter = painterResource(id = game.drawableResId),
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(game.accentColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Badge Tag
                if (game.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = game.badge,
                            color = XboxNeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = game.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = game.category,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(text = game.rating, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = XboxGreen
                    ) {
                        Text(
                            text = stringResource(R.string.btn_play_now),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}





