package com.vel.secure.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = VelPrimaryBlue,
    secondary = VelAccentCyan,
    background = VelBackgroundMain,
    surface = VelSurface,
    surfaceVariant = VelSurfaceElevated,
    onPrimary = VelTextPrimary,
    onSecondary = VelBackgroundMain,
    onBackground = VelTextPrimary,
    onSurface = VelTextPrimary,
    outline = VelBorder,
    error = VelStatusError
)

@Composable
fun VelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
