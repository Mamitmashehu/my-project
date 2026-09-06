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
    background = Cloud,
    onBackground = Ink,
    surface = BoxGray,
    onSurface = Ink,
    surfaceVariant = BoxGray,
    onSurfaceVariant = Slate,
    outline = BoxBorder,
    // Prevent Material3 from tinting elevated surfaces with `primary` -- keeps the gray boxes a
    // clean neutral gray instead of picking up a teal wash at higher elevations.
    surfaceTint = Color.Transparent,
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
 * The app defaults to this light theme regardless of the system setting -- pass
 * `darkTheme = true` explicitly if you ever want to offer a dark mode toggle.
 */
@Composable
fun ScarGuardTheme(
    darkTheme: Boolean = false,
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
