package com.jay.jaygame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FantasyColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = LightText,
    primaryContainer = DarkNavy,
    onPrimaryContainer = LightText,
    secondary = NeonCyan,
    onSecondary = DeepDark,
    secondaryContainer = HoverNavy,
    onSecondaryContainer = LightText,
    tertiary = Gold,
    background = DeepDark,
    onBackground = LightText,
    surface = DarkNavy,
    onSurface = LightText,
    surfaceVariant = HoverNavy,
    onSurfaceVariant = SubText,
    outline = Divider,
    error = NegativeRed,
)

@Composable
fun JayGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FantasyColorScheme,
        typography = GameTypography,
        content = content,
    )
}
