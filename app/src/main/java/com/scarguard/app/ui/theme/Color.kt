package com.scarguard.app.ui.theme

import androidx.compose.ui.graphics.Color

// True-black, flat, hairline-bordered palette -- closer to a modern dashboard/SaaS product
// than Material Design's default tonal-elevation dark theme. Surfaces stay near-black with a
// thin low-opacity border for separation, rather than lightening with elevation.

val TealPrimary = Color(0xFF2DD4BF)       // vivid teal accent, used sparingly (buttons, active states)
val TealPrimaryDark = TealPrimary
val TealOnPrimary = Color(0xFF00201C)     // dark text on the teal accent
val TealContainerDark = Color(0xFF0E2C29) // muted teal for subtle accent backgrounds
val TealContainerLight = Color(0xFFDCF4F2)

val Ink = Color(0xFF0A0A0A)
val Slate = Color(0xFF8B8B93)             // secondary/muted text
val Mist = Color(0xFFF4F8F8)
val Cloud = Color(0xFFFFFFFF)
val Line = Color(0xFFE1E9E9)

val PureBlack = Color(0xFF000000)         // app background
val NearBlackSurface = Color(0xFF0C0C0E)  // card/surface background, one step above pure black
val HairlineBorder = Color(0xFF232327)    // subtle 1dp borders separating surfaces
val TextPrimary = Color(0xFFF5F5F7)       // near-white, easier on the eyes than pure white
val TextSecondary = Color(0xFF98989F)     // muted gray for secondary text

val RiskNormal = Color(0xFF34D399)
val RiskNormalContainer = Color(0xFF0E2A20)
val RiskWatch = Color(0xFFFBBF24)
val RiskWatchContainer = Color(0xFF2E2408)
val RiskAlert = Color(0xFFF87171)
val RiskAlertContainer = Color(0xFF2E1212)

val DarkSurface = PureBlack
val DarkSurfaceVariant = NearBlackSurface
