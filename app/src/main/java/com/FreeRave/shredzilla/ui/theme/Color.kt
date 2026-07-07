package com.FreeRave.shredzilla.ui.theme

import androidx.compose.ui.graphics.Color

// --- Default/Neutral Theme Colors (Current ones, can be fallback) ---
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8) // Used as DarkAccentPink before

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260) // Used as LightAccentPink before

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val TextFieldGreen = Color(0xFF2ECC71)

// --- Male Theme Colors (Green Centric) ---
// Light Male Theme
val MaleLightPrimary = Color(0xFF006D3D) // Darker Green for primary elements
val MaleLightOnPrimary = Color.White
val MaleLightSecondary = Color(0xFF4CAF50) // Lighter Green for secondary
val MaleLightOnSecondary = Color.Black
val MaleLightBackground = Color(0xFFF0FFF0) // Very light green/mint white
val MaleLightOnBackground = Color(0xFF00210F) // Very dark green for text
val MaleLightSurface = Color(0xFFF0FFF0)
val MaleLightOnSurface = Color(0xFF00210F)
val MaleLightError = Color(0xFFB00020) // Standard Error for Light
val MaleLightOnError = Color.White
val MaleLightErrorContainer = Color(0xFFFCD8DF) // Example, adjust as needed
val MaleLightOnErrorContainer = Color(0xFF410E0B) // Example, adjust as needed


// Dark Male Theme
val MaleDarkPrimary = Color(0xFF50D892) // Bright Green for primary
val MaleDarkOnPrimary = Color.Black
val MaleDarkSecondary = Color(0xFF008748) // Medium Green for secondary
val MaleDarkOnSecondary = Color.White
val MaleDarkBackground = Color(0xFF001F10) // Very dark green/black
val MaleDarkOnBackground = Color(0xFF9EF8C4) // Light green for text
val MaleDarkSurface = Color(0xFF001F10)
val MaleDarkOnSurface = Color(0xFF9EF8C4)
val MaleDarkError = Color(0xFFCF6679) // Standard Error for Dark
val MaleDarkOnError = Color.Black
val MaleDarkErrorContainer = Color(0xFFB00020) // Example, adjust as needed
val MaleDarkOnErrorContainer = Color(0xFFFCD8DF) // Example, adjust as needed


// --- Female Theme Colors (Pink/Purple Centric) ---
// Light Female Theme
val FemaleLightPrimary = Color(0xFFC9007A) // Strong Pink/Magenta
val FemaleLightOnPrimary = Color.White
val FemaleLightSecondary = Color(0xFFE91E63) // Lighter Pink
val FemaleLightOnSecondary = Color.White
val FemaleLightBackground = Color(0xFFFFF0F5) // Lavender Blush / Very light pink
val FemaleLightOnBackground = Color(0xFF3E0024) // Dark Magenta/Purple for text
val FemaleLightSurface = Color(0xFFFFF0F5)
val FemaleLightOnSurface = Color(0xFF3E0024)
val FemaleLightError = Color(0xFFB00020) // Standard Error for Light (can be themed pinker if desired)
val FemaleLightOnError = Color.White
val FemaleLightErrorContainer = Color(0xFFFCD8DF)
val FemaleLightOnErrorContainer = Color(0xFF410E0B)


// Dark Female Theme
val FemaleDarkPrimary = Color(0xFFFF80AB) // Bright Pink (Pink80 was EFB8C8)
val FemaleDarkOnPrimary = Color.Black
val FemaleDarkSecondary = Color(0xFFF48FB1) // Lighter Pink
val FemaleDarkOnSecondary = Color.Black
val FemaleDarkBackground = Color(0xFF300018) // Dark Magenta/Purple
val FemaleDarkOnBackground = Color(0xFFFFD9E5) // Light Pink for text
val FemaleDarkSurface = Color(0xFF300018)
val FemaleDarkOnSurface = Color(0xFFFFD9E5)
val FemaleDarkError = Color(0xFFCF6679) // Standard Error for Dark (can be themed pinker if desired)
val FemaleDarkOnError = Color.Black
val FemaleDarkErrorContainer = Color(0xFFB00020)
val FemaleDarkOnErrorContainer = Color(0xFFFCD8DF)


// Existing colors for reference or if needed as tertiary, etc.
// val DarkBackground = Color(0xFF000000) // Black - Now part of specific themes
// val DarkText = Color(0xFF00FF00)       // Green - Now part of specific themes
// val LightBackground = Color(0xFFFFFFFF) // White - Now part of specific themes
// val LightText = Color(0xFF006400)     // Dark Green - Now part of specific themes

// Highlight colors for RestTimeScreen selections
val MaleThemeHighlight = Color(0xFFFF4444) // Bright Red
val FemaleThemeHighlight = Color(0xFFE91E63) // Vibrant Pink (using FemaleLightSecondary for consistency)

// Specific colors for RestTimeOption titles/icons

// -- Male Theme Option Colors --
val MaleOption1RedLight = Color(0xFFD32F2F) // Darker Red for light theme
val MaleOption1RedDark = Color(0xFFFF5252)  // Brighter Red for dark theme

val MaleOption2OrangeLight = Color(0xFFF57C00) // Darker Orange for light
val MaleOption2OrangeDark = Color(0xFFFFAB40)   // Brighter Orange for dark

val MaleOption3GreenLight = Color(0xFF388E3C) // Darker Green for light
val MaleOption3GreenDark = Color(0xFF66BB6A)  // Brighter Green for dark

// -- Female Theme Option Colors --
// (Example: Using shades of Pink, Purple, Teal for female options)
val FemaleOption1PinkLight = Color(0xFFC2185B)   // Deep Pink for light
val FemaleOption1PinkDark = Color(0xFFF48FB1)    // Lighter Pink for dark (matches FemaleDarkSecondary)

val FemaleOption2PurpleLight = Color(0xFF7B1FA2) // Deep Purple for light
val FemaleOption2PurpleDark = Color(0xFFCE93D8)  // Lighter Purple for dark

val FemaleOption3TealLight = Color(0xFF00796B)   // Deep Teal for light
val FemaleOption3TealDark = Color(0xFF4DB6AC)    // Lighter Teal for dark

// Colors for DayPill in TodayScreen's horizontal week view
val WorkoutDayBlueLight = Color(0xFF1976D2) // Standard Blue 700
val WorkoutDayBlueDark = Color(0xFF64B5F6)  // Standard Blue 300
val FemaleWorkoutDayPinkLight = Color(0xFFAD1457) // Pink 700 for female theme
val FemaleWorkoutDayPinkDark = Color(0xFFF06292)   // Pink 300 for female theme

val NoActivityDayGrayLight = Color(0xFFE0E0E0) // Gray 300
val NoActivityDayGrayDark = Color(0xFF424242)   // Gray 700 (adjust alpha for card background)
// For female theme, "no activity" can still be gray or a very desaturated version of a theme color.
// Using grays for now for simplicity, can be themed further.
val FemaleNoActivityDayGrayLight = NoActivityDayGrayLight
val FemaleNoActivityDayGrayDark = NoActivityDayGrayDark

// Today's date highlight (if no activity, otherwise WorkoutDay color takes precedence if today had activity)
val TodayHighlightGreenLight = Color(0xFF388E3C) // Green 700
val TodayHighlightGreenDark = Color(0xFF81C784)  // Green 300
val FemaleTodayHighlightPinkLight = Color(0xFFD81B60) // Pink 600
val FemaleTodayHighlightPinkDark = Color(0xFFF48FB1) // Pink 200 (matches FemaleDarkSecondary)

// ── Auth Screen Colors ────────────────────────────────────────────────────────
// Shared background gradient for Login & Register (dark purple → near-black)
val AuthBgTop    = Color(0xFF1B1040)
val AuthBgBottom = Color(0xFF0C0918)

// Brand gradient for Login / Register primary buttons (mono-purple, same hue palette)
val AuthBtnGradientStart = Color(0xFF6650A4) // = Purple40 alias — explicit for gradient use
val AuthBtnGradientEnd   = Color(0xFF9A82DB) // lighter tint of Purple40

// Google Sign-In button — must stay white per Google branding guidelines
val GoogleButtonBg      = Color(0xFFFFFFFF)
val GoogleButtonContent = Color(0xFF1F1F1F)
