package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AzamatDarkAccent,
    onPrimary = AzamatDarkBackground,
    primaryContainer = AzamatDarkSurface,
    onPrimaryContainer = AzamatDarkTextPrimary,
    secondary = AzamatSecondaryCyan,
    onSecondary = AzamatDarkBackground,
    background = AzamatDarkBackground,
    onBackground = AzamatDarkTextPrimary,
    surface = AzamatDarkSurface,
    onSurface = AzamatDarkTextPrimary,
    surfaceVariant = AzamatDarkSurfaceGlass,
    onSurfaceVariant = AzamatDarkTextSecondary,
    outline = AzamatDarkBorder,
    outlineVariant = AzamatDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AzamatPrimaryBlue,
    onPrimary = AzamatLightSurface,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = AzamatPrimaryDarkBlue,
    secondary = AzamatSecondaryCyan,
    onSecondary = AzamatLightSurface,
    background = AzamatLightBackground,
    onBackground = AzamatLightTextPrimary,
    surface = AzamatLightSurface,
    onSurface = AzamatLightTextPrimary,
    surfaceVariant = AzamatLightSurfaceGlass,
    onSurfaceVariant = AzamatLightTextSecondary,
    outline = AzamatLightBorder,
    outlineVariant = AzamatLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

