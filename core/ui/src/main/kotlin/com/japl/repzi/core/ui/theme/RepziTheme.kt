package com.japl.repzi.core.ui.theme

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

// Palette taken from the launcher icon: navy background, teal plates and slate bar.
private val Navy = Color(0xFF0E1B2C)
private val Teal = Color(0xFF2DD4BF)
private val TealLight = Color(0xFF99F6E4)
private val TealDark = Color(0xFF0F766E)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF06231F),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = TealLight,
    secondary = Color(0xFF94A3B8),
    onSecondary = Navy,
    secondaryContainer = Color(0xFF1D4A4A),
    onSecondaryContainer = Color(0xFFE6EDF5),
    tertiary = TealLight,
    onTertiary = Color(0xFF06231F),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = TealLight,
    background = Navy,
    onBackground = Color(0xFFE6EDF5),
    surface = Navy,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = Color(0xFF15263B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF5B6F86),
    outlineVariant = Color(0xFF243952),
    surfaceDim = Color(0xFF0A1624),
    surfaceBright = Color(0xFF20364F),
    surfaceContainerLowest = Color(0xFF0A1624),
    surfaceContainerLow = Color(0xFF112133),
    surfaceContainer = Color(0xFF15263B),
    surfaceContainerHigh = Color(0xFF1A2E46),
    surfaceContainerHighest = Color(0xFF20364F),
)

private val LightColors = lightColorScheme(
    // The icon teal is too pale on white, so the light theme uses a darker step of it.
    primary = TealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF3EE),
    onPrimaryContainer = Color(0xFF04332E),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF3EE),
    onSecondaryContainer = Navy,
    tertiary = Teal,
    onTertiary = Color(0xFF06231F),
    tertiaryContainer = TealLight,
    onTertiaryContainer = Color(0xFF06231F),
    background = Color(0xFFF4F8FA),
    onBackground = Navy,
    surface = Color(0xFFF4F8FA),
    onSurface = Navy,
    surfaceVariant = Color(0xFFE3EAEF),
    onSurfaceVariant = Color(0xFF526173),
    outline = Color(0xFF7B8A9A),
    outlineVariant = Color(0xFFD8E1E8),
    surfaceDim = Color(0xFFDCE5EB),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FBFC),
    surfaceContainer = Color(0xFFEAF0F4),
    surfaceContainerHigh = Color(0xFFE3EAEF),
    surfaceContainerHighest = Color(0xFFDCE5EB),
)

/**
 * The app identity comes from its own palette. [dynamicColor] (Material You wallpaper colors) is opt-in and off by
 * default so the app looks the same on every device and in screenshots.
 */
@Composable
fun RepziTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
