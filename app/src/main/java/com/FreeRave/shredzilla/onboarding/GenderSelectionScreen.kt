package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.onboarding.components.OnboardingScaffold
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassSelectionIndicator
import com.FreeRave.shredzilla.ui.glass.drawOrb
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.LocalGlassPalette
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.ui.theme.animateGlassPaletteAsState
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.ui.theme.glassPaletteFor

private enum class GenderOption(val value: String, val icon: ImageVector, val tagline: String) {
    Female("Female", Icons.Filled.Female, "Built to be cute 💕"),
    Male("Male", Icons.Filled.Male, "Built to be a beast 💪")
}

/** Step 1 — picking a card previews that gender's whole theme live, before anything is saved. */
@Composable
fun GenderSelectionScreen(
    onGenderSelected: (String) -> Unit,
    isLoading: Boolean = false
) {
    var selected by rememberSaveable {
        mutableStateOf(GenderOption.entries.firstOrNull { it.value == ThemeManager.currentGenderTheme })
    }
    val isDark = MaterialTheme.glass.isDark
    val previewPalette = animateGlassPaletteAsState(
        selected?.let { glassPaletteFor(it.value, isDark) } ?: MaterialTheme.glass
    )

    CompositionLocalProvider(LocalGlassPalette provides previewPalette) {
        OnboardingScaffold(
            step = 1,
            title = "Who are you?",
            subtitle = "Your colours, theme and defaults will be tailored to you.",
            ctaText = selected?.let { "Continue as ${it.value}" } ?: "Choose one to continue",
            ctaEnabled = selected != null,
            ctaLoading = isLoading,
            onCta = { selected?.let { onGenderSelected(it.value) } }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(380.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GenderOption.entries.forEach { option ->
                    GenderCard(
                        option = option,
                        selected = selected == option,
                        dimmed = selected != null && selected != option,
                        onClick = { selected = option },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Tall card painted in its own gender palette, so each option is a mini preview of its theme. */
@Composable
private fun GenderCard(
    option: GenderOption,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = glassPaletteFor(option.value, MaterialTheme.glass.isDark)
    val shape = RoundedCornerShape(28.dp)
    val scale by animateFloatAsState(if (dimmed) 0.95f else 1f, label = "genderCardScale")
    val alpha by animateFloatAsState(if (dimmed) 0.55f else 1f, label = "genderCardAlpha")

    CompositionLocalProvider(LocalGlassPalette provides palette) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
                .clip(shape)
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
                    drawOrb(palette.orbPrimary.copy(alpha = 0.6f), 0.3f, 0.25f, 0.9f)
                    drawOrb(palette.orbSecondary.copy(alpha = 0.4f), 0.9f, 0.85f, 0.7f)
                }
                .glassSurface(palette, shape)
                .border(2.dp, if (selected) palette.accent else Color.Transparent, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(18.dp)
        ) {
            GlassSelectionIndicator(selected = selected, modifier = Modifier.align(Alignment.TopEnd))
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassIconBadge(painter = rememberVectorPainter(option.icon), size = 84.dp)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = option.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = palette.content
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = option.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = palette.accent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
private fun GenderSelectionPreview() {
    ShredzillaTheme {
        GenderSelectionScreen(onGenderSelected = {})
    }
}
