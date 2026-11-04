package com.FreeRave.shredzilla.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Glass design tokens. One palette per gender (plus the brand palette used before a
 * gender is chosen, e.g. auth screens) and per light/dark mode.
 * Read it anywhere through [MaterialTheme.glass].
 */
@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val accent: Color,
    val accentGradient: List<Color>,
    val onAccent: Color,
    val fill: Color,
    val border: Color,
    val content: Color,
    val contentMuted: Color,
    val error: Color
)

private fun darkGlass(
    backgroundTop: Color,
    backgroundBottom: Color,
    orbPrimary: Color,
    orbSecondary: Color,
    accent: Color,
    accentGradient: List<Color>,
    onAccent: Color = Color.White
) = GlassPalette(
    isDark = true,
    backgroundTop = backgroundTop,
    backgroundBottom = backgroundBottom,
    orbPrimary = orbPrimary,
    orbSecondary = orbSecondary,
    accent = accent,
    accentGradient = accentGradient,
    onAccent = onAccent,
    fill = Color.White.copy(alpha = 0.08f),
    border = Color.White.copy(alpha = 0.20f),
    content = Color.White,
    contentMuted = Color.White.copy(alpha = 0.65f),
    error = Color(0xFFFF8A9A)
)

private fun lightGlass(
    backgroundTop: Color,
    backgroundBottom: Color,
    orbPrimary: Color,
    orbSecondary: Color,
    accent: Color,
    accentGradient: List<Color>,
    content: Color,
    onAccent: Color = Color.White
) = GlassPalette(
    isDark = false,
    backgroundTop = backgroundTop,
    backgroundBottom = backgroundBottom,
    orbPrimary = orbPrimary,
    orbSecondary = orbSecondary,
    accent = accent,
    accentGradient = accentGradient,
    onAccent = onAccent,
    fill = Color.White.copy(alpha = 0.55f),
    border = Color.White.copy(alpha = 0.90f),
    content = content,
    contentMuted = content.copy(alpha = 0.62f),
    error = Color(0xFFB3261E)
)

// ── Brand (no gender yet) — violet / electric blue ──────────────────────────
private val BrandDarkGlass = darkGlass(
    backgroundTop = AuthBgTop,
    backgroundBottom = AuthBgBottom,
    orbPrimary = Color(0xFF7C4DFF),
    orbSecondary = Color(0xFF2F80FF),
    accent = Purple80,
    accentGradient = listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
)

private val BrandLightGlass = lightGlass(
    backgroundTop = Color(0xFFF4F0FF),
    backgroundBottom = Color(0xFFE4DBFF),
    orbPrimary = Color(0xFFB59CFF),
    orbSecondary = Color(0xFF8FC8FF),
    accent = Purple40,
    accentGradient = listOf(AuthBtnGradientStart, AuthBtnGradientEnd),
    content = Color(0xFF1D1530)
)

// ── Male — emerald / teal ───────────────────────────────────────────────────
private val MaleDarkGlass = darkGlass(
    backgroundTop = Color(0xFF0A2016),
    backgroundBottom = Color(0xFF030A06),
    orbPrimary = Color(0xFF10B981),
    orbSecondary = Color(0xFF0EA5A0),
    accent = MaleDarkPrimary,
    accentGradient = listOf(Color(0xFF00A86B), MaleDarkPrimary),
    onAccent = MaleDarkOnPrimary
)

private val MaleLightGlass = lightGlass(
    backgroundTop = Color(0xFFEFFAF3),
    backgroundBottom = Color(0xFFD9F2E3),
    orbPrimary = Color(0xFF7EE2AE),
    orbSecondary = Color(0xFF8EDFD6),
    accent = MaleLightPrimary,
    accentGradient = listOf(MaleLightPrimary, Color(0xFF2E9E66)),
    content = MaleLightOnBackground
)

// ── Female — rose / orchid ──────────────────────────────────────────────────
private val FemaleDarkGlass = darkGlass(
    backgroundTop = Color(0xFF2A0A1E),
    backgroundBottom = Color(0xFF0E040A),
    orbPrimary = Color(0xFFFF4FA3),
    orbSecondary = Color(0xFFA855F7),
    accent = FemaleDarkPrimary,
    accentGradient = listOf(FemaleLightPrimary, Color(0xFFE85AA6))
)

private val FemaleLightGlass = lightGlass(
    backgroundTop = Color(0xFFFFF0F6),
    backgroundBottom = Color(0xFFF8DCEB),
    orbPrimary = Color(0xFFFF9CC8),
    orbSecondary = Color(0xFFD7A8FF),
    accent = FemaleLightPrimary,
    accentGradient = listOf(FemaleLightPrimary, Color(0xFFE85AA6)),
    content = FemaleLightOnBackground
)

fun glassPaletteFor(gender: String?, darkTheme: Boolean): GlassPalette = when (gender) {
    "Male" -> if (darkTheme) MaleDarkGlass else MaleLightGlass
    "Female" -> if (darkTheme) FemaleDarkGlass else FemaleLightGlass
    else -> if (darkTheme) BrandDarkGlass else BrandLightGlass
}

val LocalGlassPalette = staticCompositionLocalOf { BrandDarkGlass }

/** Smoothly cross-fades every colour of the palette, e.g. to preview a gender theme before it is chosen. */
@Composable
fun animateGlassPaletteAsState(target: GlassPalette, durationMillis: Int = 600): GlassPalette {
    val spec = tween<Color>(durationMillis)
    @Composable
    fun Color.animated(label: String) = animateColorAsState(this, spec, label = label).value

    return target.copy(
        backgroundTop = target.backgroundTop.animated("backgroundTop"),
        backgroundBottom = target.backgroundBottom.animated("backgroundBottom"),
        orbPrimary = target.orbPrimary.animated("orbPrimary"),
        orbSecondary = target.orbSecondary.animated("orbSecondary"),
        accent = target.accent.animated("accent"),
        accentGradient = target.accentGradient.mapIndexed { i, color -> color.animated("accentGradient$i") },
        onAccent = target.onAccent.animated("onAccent"),
        content = target.content.animated("content"),
        contentMuted = target.contentMuted.animated("contentMuted")
    )
}

val MaterialTheme.glass: GlassPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalGlassPalette.current
