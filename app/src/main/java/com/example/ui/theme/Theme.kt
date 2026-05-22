package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),       // Electric Sky Blue (Miami night)
    secondary = Color(0xFF1E293B),     // Carbon Slate Navy
    tertiary = AccentCoral,            // Track Rose Red
    background = Color(0xFF0B0F19),    // Deep Evening Indigo Space Obsidian
    surface = Color(0xFF151D2F),       // Polished Titanium Card Plate
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFFFAF7F2),
    onTertiary = Color.White,
    onBackground = Color(0xFFFAF7F2),  // Alabaster Cream Text
    onSurface = Color(0xFFFAF7F2)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,               // Miami Riviera Sky Blue (Primary)
    secondary = OceanLight,            // Soft Sky-tinted Cream (Secondary elements)
    tertiary = AccentCoral,            // Track Rose Red
    background = Color(0xFFFAF7F2),    // Premium Alabaster Cream App Canvas Background (Chalk Luxury)
    surface = Color(0xFFFFFFFF),       // Pristine Ceramic Pearlescent White Surface
    onPrimary = Color.White,
    onSecondary = SlateDark,           // Slate Dark Indigo Text
    onTertiary = Color.White,
    onBackground = SlateDark,          // Executive Midnight Indigo Text
    onSurface = SlateDark              // Clean Contrast Surfaces Text
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Elegant luxury theme matching the selected user preference
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
