package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.XboxDark

/**
 * Flat solid backdrop for maximum scroll and tab-switch performance on mid-range devices.
 */
@Composable
fun CyberBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XboxDark)
    ) {
        content()
    }
}
