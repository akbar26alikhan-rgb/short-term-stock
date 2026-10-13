package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = Color.White,
    secondary = AccentGold,
    onSecondary = Color.Black,
    secondaryContainer = AccentGoldContainer,
    onSecondaryContainer = AccentGold,
    tertiary = AccentCyan,
    background = BrandNavyDark,
    onBackground = TextPrimary,
    surface = BrandNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = BrandNavyElevated,
    onSurfaceVariant = TextSecondary,
    outline = BrandNavyBorder,
    error = BearRed,
    errorContainer = BearRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    outline = Color(0xFFD1D5DB),
    error = BearRedDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek pro dark mode for financial market charts
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
