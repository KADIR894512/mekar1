package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DefaultDarkColorScheme = darkColorScheme(
    primary = RoyalBlue600,
    onPrimary = Color.White,
    primaryContainer = Navy800,
    onPrimaryContainer = Color.White,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Slate700,
    onSecondaryContainer = Color.White,
    tertiary = AccentGreen,
    background = Navy900,
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = RoyalBlue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0ECFF),
    onPrimaryContainer = RoyalBlue700,
    secondary = Navy900,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Navy900,
    tertiary = AccentCyan,
    background = Color(0xFFF8FAFC),
    onBackground = Navy900,
    surface = Color.White,
    onSurface = Navy900,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Slate700,
    outline = Slate200
)

fun parseHexColor(hex: String, default: Color): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        val colorInt = if (clean.length == 6) {
            (0xFF000000 or clean.toLong(16)).toInt()
        } else if (clean.length == 8) {
            clean.toLong(16).toInt()
        } else {
            return default
        }
        Color(colorInt)
    } catch (_: Exception) {
        default
    }
}

@Composable
fun MasterPrinterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme

    val colorScheme = if (!customPrimaryHex.isNullOrBlank() || !customSecondaryHex.isNullOrBlank()) {
        val primary = customPrimaryHex?.let { parseHexColor(it, baseScheme.primary) } ?: baseScheme.primary
        val secondary = customSecondaryHex?.let { parseHexColor(it, baseScheme.secondary) } ?: baseScheme.secondary
        baseScheme.copy(
            primary = primary,
            secondary = secondary
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
