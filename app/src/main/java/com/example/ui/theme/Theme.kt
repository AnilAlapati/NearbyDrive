package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF86A789),       // Muted Sage Green for Dark Mode
    secondary = Color(0xFF4A634E),     // Deeper Olive/Forest Container
    tertiary = Color(0xFFB2A59B),      // Earthy Sand Accent
    background = Color(0xFF141A16),    // Pure Deep Midnight Forest Pine
    surface = Color(0xFF1D2620),       // Warm Muted Moss Card Surface
    onPrimary = Color(0xFF141A16),
    onSecondary = Color(0xFFFDFBF7),
    onTertiary = Color.White,
    onBackground = Color(0xFFFDFBF7),
    onSurface = Color(0xFFFDFBF7)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,               // Forest Sage Green (0xFF4A634E)
    secondary = OceanLight,           // Light Sage Container (0xFFDDE8DB)
    tertiary = AccentCoral,            // Soil Brown Accent (0xFF7D7767)
    background = Color(0xFFFDFBF7),    // Organic Warm Cream/Beige (from Natural Tones spec)
    surface = Color(0xFFFCFAF4),       // Warm Alabaster White Surface
    onPrimary = Color.White,
    onSecondary = SlateBlueText,
    onTertiary = Color.White,
    onBackground = SlateDark,
    onSurface = SlateDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to enforce our extremely polished dedicated color palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
