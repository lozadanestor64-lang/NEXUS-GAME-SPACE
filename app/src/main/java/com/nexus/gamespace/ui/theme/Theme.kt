package com.nexus.gamespace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexusColorScheme = darkColorScheme(
    primary = NexusRed,
    onPrimary = NexusTextPrimary,
    primaryContainer = NexusRedDark,
    onPrimaryContainer = NexusTextPrimary,
    background = NexusBlack,
    onBackground = NexusTextPrimary,
    surface = NexusSurface,
    onSurface = NexusTextPrimary,
    surfaceVariant = NexusSurfaceHigh,
    onSurfaceVariant = NexusTextSecondary,
    outline = NexusBorder,
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexusColorScheme,
        typography = NexusTypography,
        content = content,
    )
}
