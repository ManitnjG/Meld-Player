package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MeldDarkColorScheme = darkColorScheme(
    primary = FlacCyan,
    onPrimary = DeepBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = FlacCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = NeonPurple,
    tertiary = HiResGold,
    onTertiary = DeepBlack,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StrokeColor
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MeldDarkColorScheme,
        typography = Typography,
        content = content
    )
}
