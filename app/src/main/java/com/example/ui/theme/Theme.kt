package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = XboxNeonGreen,
    onPrimary = Color.Black,
    primaryContainer = XboxGreen,
    onPrimaryContainer = Color.White,
    secondary = DirectCyan,
    onSecondary = Color.Black,
    secondaryContainer = XboxSurfaceVariant,
    onSecondaryContainer = DirectCyan,
    tertiary = WarningAmber,
    background = XboxDark,
    onBackground = TextPrimary,
    surface = XboxSurface,
    onSurface = TextPrimary,
    surfaceVariant = XboxSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = XboxCardBorder
)

private val LightColorScheme = darkColorScheme(
    primary = XboxGreen,
    onPrimary = Color.White,
    primaryContainer = XboxSurfaceVariant,
    onPrimaryContainer = XboxNeonGreen,
    secondary = DirectCyan,
    onSecondary = Color.Black,
    background = XboxDark,
    onBackground = TextPrimary,
    surface = XboxSurface,
    onSurface = TextPrimary,
    surfaceVariant = XboxSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = XboxCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Gaming app defaults to sleek immersive dark mode
    dynamicColor: Boolean = false, // Keep distinct Xbox aesthetic
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
