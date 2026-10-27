package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EyeCareColorScheme = darkColorScheme(
    primary = BullishGreen,
    onPrimary = EyeCareBackground,
    secondary = BearishRed,
    onSecondary = TextPrimary,
    tertiary = AccentGold,
    background = EyeCareBackground,
    onBackground = TextPrimary,
    surface = EyeCareSurface,
    onSurface = TextPrimary,
    surfaceVariant = EyeCareSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = EyeCareBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EyeCareColorScheme,
        typography = Typography,
        content = content
    )
}
