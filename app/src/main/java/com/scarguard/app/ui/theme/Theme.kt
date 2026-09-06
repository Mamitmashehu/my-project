package com.scarguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Cloud,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealPrimary,
    background = Mist,
    onBackground = Ink,
    surface = Cloud,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = Line,
    error = RiskAlert,
    onError = Cloud,
    errorContainer = RiskAlertContainer,
    onErrorContainer = RiskAlert,
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF00201F),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealPrimaryDark,
    background = DarkSurface,
    onBackground = Cloud,
    surface = DarkSurfaceVariant,
    onSurface = Cloud,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB6C4C4),
    outline = Color(0xFF3A4649),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * The app defaults to a dark theme regardless of the system setting -- pass `darkTheme = false`
 * explicitly if you ever want to offer a light mode toggle.
 */
@Composable
fun ScarGuardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ScarGuardTypography,
        shapes = ScarGuardShapes,
        content = content
    )
}
