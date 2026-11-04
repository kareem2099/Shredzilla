package com.FreeRave.shredzilla.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color 
import com.FreeRave.shredzilla.screens.settings.ThemeSetting // Moved import to the top

// Default Color Schemes (using existing Purple/Pink)
private val DefaultDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Black, // Example: Using generic Black for default dark
    surface = Black,    // Example
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = White, // Example
    onSurface = White,     // Example
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color(0xFFFCD8DF)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = White, // Example
    surface = White,    // Example
    onPrimary = White,
    onSecondary = Black, // Example
    onTertiary = Black,  // Example
    onBackground = Black, // Example
    onSurface = Black,     // Example
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF410E0B)
)

// Male Theme Color Schemes
private val MaleDarkColorScheme = darkColorScheme(
    primary = MaleDarkPrimary,
    onPrimary = MaleDarkOnPrimary,
    primaryContainer = MaleDarkPrimaryContainer,
    onPrimaryContainer = MaleDarkOnPrimaryContainer,
    secondary = MaleDarkSecondary,
    onSecondary = MaleDarkOnSecondary,
    secondaryContainer = MaleDarkSecondaryContainer,
    onSecondaryContainer = MaleDarkOnSecondaryContainer,
    tertiary = MaleDarkTertiary,
    onTertiary = MaleDarkOnTertiary,
    tertiaryContainer = MaleDarkTertiaryContainer,
    onTertiaryContainer = MaleDarkOnTertiaryContainer,
    background = MaleDarkBackground,
    onBackground = MaleDarkOnBackground,
    surface = MaleDarkSurface,
    onSurface = MaleDarkOnSurface,
    surfaceVariant = MaleDarkSurfaceVariant,
    onSurfaceVariant = MaleDarkOnSurfaceVariant,
    outline = MaleDarkOutline,
    outlineVariant = MaleDarkOutlineVariant,
    error = MaleDarkError,
    onError = MaleDarkOnError,
    errorContainer = MaleDarkErrorContainer,
    onErrorContainer = MaleDarkOnErrorContainer
)

private val MaleLightColorScheme = lightColorScheme(
    primary = MaleLightPrimary,
    onPrimary = MaleLightOnPrimary,
    primaryContainer = MaleLightPrimaryContainer,
    onPrimaryContainer = MaleLightOnPrimaryContainer,
    secondary = MaleLightSecondary,
    onSecondary = MaleLightOnSecondary,
    secondaryContainer = MaleLightSecondaryContainer,
    onSecondaryContainer = MaleLightOnSecondaryContainer,
    tertiary = MaleLightTertiary,
    onTertiary = MaleLightOnTertiary,
    tertiaryContainer = MaleLightTertiaryContainer,
    onTertiaryContainer = MaleLightOnTertiaryContainer,
    background = MaleLightBackground,
    onBackground = MaleLightOnBackground,
    surface = MaleLightSurface,
    onSurface = MaleLightOnSurface,
    surfaceVariant = MaleLightSurfaceVariant,
    onSurfaceVariant = MaleLightOnSurfaceVariant,
    outline = MaleLightOutline,
    outlineVariant = MaleLightOutlineVariant,
    error = MaleLightError,
    onError = MaleLightOnError,
    errorContainer = MaleLightErrorContainer,
    onErrorContainer = MaleLightOnErrorContainer
)

// Female Theme Color Schemes
private val FemaleDarkColorScheme = darkColorScheme(
    primary = FemaleDarkPrimary,
    onPrimary = FemaleDarkOnPrimary,
    primaryContainer = FemaleDarkPrimaryContainer,
    onPrimaryContainer = FemaleDarkOnPrimaryContainer,
    secondary = FemaleDarkSecondary,
    onSecondary = FemaleDarkOnSecondary,
    secondaryContainer = FemaleDarkSecondaryContainer,
    onSecondaryContainer = FemaleDarkOnSecondaryContainer,
    tertiary = FemaleDarkTertiary,
    onTertiary = FemaleDarkOnTertiary,
    tertiaryContainer = FemaleDarkTertiaryContainer,
    onTertiaryContainer = FemaleDarkOnTertiaryContainer,
    background = FemaleDarkBackground,
    onBackground = FemaleDarkOnBackground,
    surface = FemaleDarkSurface,
    onSurface = FemaleDarkOnSurface,
    surfaceVariant = FemaleDarkSurfaceVariant,
    onSurfaceVariant = FemaleDarkOnSurfaceVariant,
    outline = FemaleDarkOutline,
    outlineVariant = FemaleDarkOutlineVariant,
    error = FemaleDarkError,
    onError = FemaleDarkOnError,
    errorContainer = FemaleDarkErrorContainer,
    onErrorContainer = FemaleDarkOnErrorContainer
)

private val FemaleLightColorScheme = lightColorScheme(
    primary = FemaleLightPrimary,
    onPrimary = FemaleLightOnPrimary,
    primaryContainer = FemaleLightPrimaryContainer,
    onPrimaryContainer = FemaleLightOnPrimaryContainer,
    secondary = FemaleLightSecondary,
    onSecondary = FemaleLightOnSecondary,
    secondaryContainer = FemaleLightSecondaryContainer,
    onSecondaryContainer = FemaleLightOnSecondaryContainer,
    tertiary = FemaleLightTertiary,
    onTertiary = FemaleLightOnTertiary,
    tertiaryContainer = FemaleLightTertiaryContainer,
    onTertiaryContainer = FemaleLightOnTertiaryContainer,
    background = FemaleLightBackground,
    onBackground = FemaleLightOnBackground,
    surface = FemaleLightSurface,
    onSurface = FemaleLightOnSurface,
    surfaceVariant = FemaleLightSurfaceVariant,
    onSurfaceVariant = FemaleLightOnSurfaceVariant,
    outline = FemaleLightOutline,
    outlineVariant = FemaleLightOutlineVariant,
    error = FemaleLightError,
    onError = FemaleLightOnError,
    errorContainer = FemaleLightErrorContainer,
    onErrorContainer = FemaleLightOnErrorContainer
)


@Composable
fun ShredzillaTheme(
    // darkTheme: Boolean = isSystemInDarkTheme(), // This will be determined by ThemeManager
    // genderTheme: String? = null, // This is now ThemeManager.currentGenderTheme
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentGender = ThemeManager.currentGenderTheme
    val useDarkTheme = isAppInDarkTheme()

    val baseColorScheme = when (currentGender) {
        "Male" -> if (useDarkTheme) MaleDarkColorScheme else MaleLightColorScheme
        "Female" -> if (useDarkTheme) FemaleDarkColorScheme else FemaleLightColorScheme
        else -> if (useDarkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
    }

    val colorScheme = when {
        // If a specific gender theme is active, use it.
        currentGender != null -> baseColorScheme
        // Otherwise, if dynamic color is enabled and available, use it.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback to the base (default) scheme if no gender theme and no dynamic color.
        else -> baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Bars are transparent (edge-to-edge); only the icon contrast follows the app theme
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalGlassPalette provides glassPaletteFor(currentGender, useDarkTheme)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Dark/light decision for the current gender's saved theme preference (single source of truth). */
@Composable
fun isAppInDarkTheme(): Boolean {
    // These preferences are reactive states in ThemeManager, updated by MainAppContainer
    val effectiveThemeSetting = ThemeManager.getEffectiveThemeSetting(
        ThemeManager.currentGenderTheme,
        ThemeManager.themePreferenceMale,
        ThemeManager.themePreferenceFemale
    )
    return when (effectiveThemeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
    }
}
