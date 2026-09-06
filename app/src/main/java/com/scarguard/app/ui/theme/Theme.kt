package com.scarguard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
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
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealPrimary,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = NearBlackSurface,
    onSurface = TextPrimary,
    surfaceVariant = NearBlackSurface,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    // Material3 normally tints elevated surfaces with `primary`; on a true-black canvas that
    // reads as a muddy wash, so it's switched off in favor of flat surfaces + hairline borders.
    surfaceTint = Color.Transparent,
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
