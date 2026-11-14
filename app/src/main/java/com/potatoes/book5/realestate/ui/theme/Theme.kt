package com.potatoes.book5.realestate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = WireframeColors.Accent,
    onPrimary = Color.White,
    primaryContainer = WireframeColors.AccentLight,
    onPrimaryContainer = WireframeColors.Accent,
    background = WireframeColors.Background,
    onBackground = WireframeColors.Ink,
    surface = WireframeColors.Background,
    onSurface = WireframeColors.Ink,
    surfaceVariant = WireframeColors.Card,
    onSurfaceVariant = WireframeColors.Sub,
    outline = WireframeColors.Line,
    error = WireframeColors.Up,
    errorContainer = WireframeColors.UpLight,
)

private val DarkColors = darkColorScheme(
    primary = WireframeColors.DarkAccent,
    onPrimary = WireframeColors.DarkBackground,
    primaryContainer = WireframeColors.DarkAccentLight,
    onPrimaryContainer = WireframeColors.DarkAccent,
    background = WireframeColors.DarkBackground,
    onBackground = WireframeColors.DarkInk,
    surface = WireframeColors.DarkBackground,
    onSurface = WireframeColors.DarkInk,
    surfaceVariant = WireframeColors.DarkCard,
    onSurfaceVariant = WireframeColors.DarkSub,
    outline = WireframeColors.DarkLine,
    error = WireframeColors.Up,
    errorContainer = WireframeColors.DarkUpLight,
)

@Composable
fun KingOfRealEstateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
