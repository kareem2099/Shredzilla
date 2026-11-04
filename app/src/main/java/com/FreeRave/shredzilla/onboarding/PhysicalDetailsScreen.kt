package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.onboarding.components.OnboardingScaffold
import com.FreeRave.shredzilla.onboarding.components.RulerPicker
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.ui.theme.glass

private val AgeRange = 13..100
private val HeightRangeCm = 120..230
private val WeightRangeKg = 30..250

/** Starting points near typical values so most people only nudge the rulers a little. */
private fun defaultHeightCm(gender: String?) = if (gender == "Female") 163 else 176
private fun defaultWeightKg(gender: String?) = if (gender == "Female") 60 else 75

/** Step 2 — age, height and weight on snapping rulers instead of free-text fields. */
@Composable
fun PhysicalDetailsScreen(
    onDetailsSubmitted: (age: Int, heightCm: Int, weightKg: Int) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    val gender = ThemeManager.currentGenderTheme
    var age      by rememberSaveable { mutableIntStateOf(25) }
    var heightCm by rememberSaveable { mutableIntStateOf(defaultHeightCm(gender)) }
    var weightKg by rememberSaveable { mutableIntStateOf(defaultWeightKg(gender)) }

    OnboardingScaffold(
        step = 2,
        title = "Tell us about you",
        subtitle = "Swipe the rulers. We use this to personalise your progress and targets.",
        ctaText = "Continue",
        ctaLoading = isLoading,
        onBack = onBack,
        onCta = { onDetailsSubmitted(age, heightCm, weightKg) }
    ) {
        MetricCard(Icons.Filled.Cake, "Age", age, "years", AgeRange) { age = it }
        MetricCard(Icons.Filled.Height, "Height", heightCm, "cm", HeightRangeCm) { heightCm = it }
        MetricCard(Icons.Filled.MonitorWeight, "Weight", weightKg, "kg", WeightRangeKg) { weightKg = it }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconBadge(painter = rememberVectorPainter(icon), size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = palette.contentMuted,
                modifier = Modifier.weight(1f)
            )
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInVertically { it * direction } + fadeIn()) togetherWith
                        (slideOutVertically { -it * direction } + fadeOut())
                },
                label = "${label}Value"
            ) { shown ->
                Text(
                    text = shown.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.content
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(text = unit, style = MaterialTheme.typography.titleSmall, color = palette.accent)
        }
        RulerPicker(value = value, range = range, onValueChange = onValueChange)
    }
}

@Preview
@Composable
private fun PhysicalDetailsPreview() {
    ShredzillaTheme {
        PhysicalDetailsScreen(onDetailsSubmitted = { _, _, _ -> }, onBack = {})
    }
}
