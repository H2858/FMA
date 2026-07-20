package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BurgundyDarkColorScheme = darkColorScheme(
    primary = BurgundyPrimary,
    secondary = BurgundyGlow,
    tertiary = BurgundyLight,
    background = CosmicBlack,
    surface = SlateCard,
    onPrimary = Color(0xFFFBF5F7), // Literal TextPrimary
    onSecondary = Color(0xFFFBF5F7),
    onTertiary = Color(0xFFFBF5F7),
    onBackground = Color(0xFFFBF5F7),
    onSurface = Color(0xFFFBF5F7),
    surfaceVariant = SlateCard,
    onSurfaceVariant = Color(0xFFBCA6AF), // Literal TextSecondary
    outline = SlateCardBorder
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldGlow,
    tertiary = EmeraldLight,
    background = ObsidianBlack,
    surface = ObsidianCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = ObsidianBlack,
    onBackground = Color(0xFFFBF5F7), // Literal TextPrimary
    onSurface = Color(0xFFFBF5F7),
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = Color(0xFF8FA89B), // Literal TextSecondaryGreen
    outline = ObsidianCardBorder
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF021B10), // Professional deep dark black-green (high contrast)
    secondary = Color(0xFF032215), // Dark green-black forest
    tertiary = Color(0xFF064E3B), // Dark emerald forest accent
    background = Color(0xFFFAFAFA), // Clean pristine background
    surface = Color(0xFFFFFFFF), // Crisp white cards
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF01140B),
    onSurface = Color(0xFF01140B),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF374151),
    outline = Color(0xFFE2E8F0)
)

private val BurgundyLightColorScheme = lightColorScheme(
    primary = Color(0xFF800020), // Rich royal burgundy (high contrast)
    secondary = Color(0xFF9E1B32), // Deep crimson
    tertiary = Color(0xFF5C061D), // Dark plum
    background = Color(0xFFFAFAFA), // Clean background
    surface = Color(0xFFFFFFFF), // Pure white
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F0507),
    onSurface = Color(0xFF0F0507),
    surfaceVariant = Color(0xFFFBF5F7),
    onSurfaceVariant = Color(0xFF4A373D),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    isEmerald: Boolean = true, // Default to true to match user's screenshot exactly
    darkTheme: Boolean = true, // Force dark theme by default for premium feel
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic binding of global theme state
    isDarkThemeGlobal = darkTheme

    val colorScheme = if (darkTheme) {
        if (isEmerald) EmeraldDarkColorScheme else BurgundyDarkColorScheme
    } else {
        if (isEmerald) EmeraldLightColorScheme else BurgundyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
