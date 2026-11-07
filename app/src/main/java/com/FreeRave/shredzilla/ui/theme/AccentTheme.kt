package com.FreeRave.shredzilla.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Pro accent themes: they recolor the accent, its gradient and the background orbs on top of
 * the profile's light/dark palette, so contrast rules of the base palette stay intact.
 */
enum class AccentTheme(
    val storedValue: String,
    val title: String,
    val darkAccent: Color,
    val lightAccent: Color,
    val gradient: List<Color>,
    val orbs: Pair<Color, Color>,
    val lightOrbs: Pair<Color, Color>,
    val darkBackground: Pair<Color, Color>,
    val lightBackground: Pair<Color, Color>,
) {
    INFERNO(
        "inferno", "Inferno",
        darkAccent = Color(0xFFFF7A45), lightAccent = Color(0xFFD9480F),
        gradient = listOf(Color(0xFFFF512F), Color(0xFFF09819)),
        orbs = Color(0xFFFF5722) to Color(0xFFFFB300),
        lightOrbs = Color(0xFFFFB199) to Color(0xFFFFD98A),
        darkBackground = Color(0xFF2A0F06) to Color(0xFF0D0402),
        lightBackground = Color(0xFFFFF4EC) to Color(0xFFFFE1CC),
    ),
    OCEAN(
        "ocean", "Ocean",
        darkAccent = Color(0xFF4FC3F7), lightAccent = Color(0xFF0277BD),
        gradient = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
        orbs = Color(0xFF00B4DB) to Color(0xFF3F51B5),
        lightOrbs = Color(0xFF8FDBFF) to Color(0xFFA8B8FF),
        darkBackground = Color(0xFF04172A) to Color(0xFF02080F),
        lightBackground = Color(0xFFEEF8FF) to Color(0xFFD6ECFF),
    ),
    GOLD(
        "gold", "Gold",
        darkAccent = Color(0xFFFFD54F), lightAccent = Color(0xFF9A6B00),
        gradient = listOf(Color(0xFFF7B733), Color(0xFFC79100)),
        orbs = Color(0xFFFFC107) to Color(0xFFB8860B),
        lightOrbs = Color(0xFFFFE08A) to Color(0xFFF2C46D),
        darkBackground = Color(0xFF241A04) to Color(0xFF0B0802),
        lightBackground = Color(0xFFFFFAEA) to Color(0xFFFCEFC7),
    ),
    VOLT(
        "volt", "Volt",
        darkAccent = Color(0xFFC6FF3D), lightAccent = Color(0xFF4C7A00),
        gradient = listOf(Color(0xFFA8E10C), Color(0xFF3BB78F)),
        orbs = Color(0xFF9EFF00) to Color(0xFF00E5A0),
        lightOrbs = Color(0xFFCFF58A) to Color(0xFF9DEFCF),
        darkBackground = Color(0xFF121E04) to Color(0xFF050A01),
        lightBackground = Color(0xFFF6FDEB) to Color(0xFFE3F6C8),
    );

    /** Text/icons on the accent gradient: dark on the bright gradients, white on the deep ones. */
    private val onGradient: Color get() = if (this == OCEAN || this == INFERNO) Color.White else Color(0xFF1A1300)

    fun applyTo(base: GlassPalette): GlassPalette = base.copy(
        accent = if (base.isDark) darkAccent else lightAccent,
        accentGradient = gradient,
        onAccent = onGradient,
        orbPrimary = if (base.isDark) orbs.first else lightOrbs.first,
        orbSecondary = if (base.isDark) orbs.second else lightOrbs.second,
        backgroundTop = if (base.isDark) darkBackground.first else lightBackground.first,
        backgroundBottom = if (base.isDark) darkBackground.second else lightBackground.second,
    )

    fun applyTo(scheme: ColorScheme, isDark: Boolean): ColorScheme {
        val accent = if (isDark) darkAccent else lightAccent
        val onAccent = if (isDark) Color(0xFF1A1300) else Color.White
        return scheme.copy(primary = accent, onPrimary = onAccent, secondary = accent, tertiary = gradient.last())
    }

    companion object {
        fun fromStored(value: String?): AccentTheme? = entries.find { it.storedValue == value }
    }
}
