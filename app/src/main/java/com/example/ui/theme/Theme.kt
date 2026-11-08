package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AdaptiveSleepColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = PrimaryCyan,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = AccentAmber,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun AdaptiveSleepTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AdaptiveSleepColorScheme,
        typography = Typography,
        content = content
    )
}
