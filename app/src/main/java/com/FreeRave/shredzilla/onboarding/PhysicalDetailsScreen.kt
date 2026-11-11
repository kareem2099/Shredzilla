package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableStateOf
import com.FreeRave.shredzilla.screens.settings.LengthUnit
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.utils.formatHeight
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
private val HeightRangeIn = 47..91
private val WeightRangeKg = 30..250
private val WeightRangeLb = 66..550
private const val CM_PER_INCH = 2.54
private const val LB_PER_KG = 2.20462

/** Starting points near typical values so most people only nudge the rulers a little. */
private fun defaultHeightCm(gender: String?) = if (gender == "Female") 163 else 176
private fun defaultWeightKg(gender: String?) = if (gender == "Female") 60 else 75

/** Step 2 — age, height and weight on snapping rulers instead of free-text fields. */
@Composable
fun PhysicalDetailsScreen(
    onDetailsSubmitted: (age: Int, heightCm: Int, weightKg: Int, weightUnit: UnitSystem, lengthUnit: LengthUnit) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    val gender = ThemeManager.currentGenderTheme
    var age      by rememberSaveable { mutableIntStateOf(25) }
    // Kept in centimetres and kilograms; the rulers show the chosen units, starting from the phone's region
    var heightCm by rememberSaveable { mutableIntStateOf(defaultHeightCm(gender)) }
    var weightKg by rememberSaveable { mutableIntStateOf(defaultWeightKg(gender)) }
    var lengthUnit by rememberSaveable { mutableStateOf(LengthUnit.forLocale()) }
    var weightUnit by rememberSaveable { mutableStateOf(UnitSystem.forLocale()) }
    val inches = lengthUnit == LengthUnit.INCHES
    val pounds = weightUnit == UnitSystem.IMPERIAL

    OnboardingScaffold(
        step = 2,
        title = "Tell us about you",
        subtitle = "Swipe the rulers. We use this to personalise your progress and targets.",
        ctaText = "Continue",
        ctaLoading = isLoading,
        onBack = onBack,
        onCta = { onDetailsSubmitted(age, heightCm, weightKg, weightUnit, lengthUnit) }
    ) {
        MetricCard(Icons.Filled.Cake, "Age", age, "years", AgeRange) { age = it }
        MetricCard(
            icon = Icons.Filled.Height,
            label = "Height",
            value = if (inches) Math.round(heightCm / CM_PER_INCH).toInt() else heightCm,
            unit = if (inches) "" else "cm",
            range = if (inches) HeightRangeIn else HeightRangeCm,
            display = { if (inches) formatHeight(Math.round(it * CM_PER_INCH).toInt(), LengthUnit.INCHES) else it.toString() },
            units = listOf("cm", "ft/in"),
            selectedUnit = if (inches) 1 else 0,
            onUnitChange = { lengthUnit = if (it == 1) LengthUnit.INCHES else LengthUnit.CENTIMETRES },
        ) { heightCm = if (inches) Math.round(it * CM_PER_INCH).toInt() else it }
        MetricCard(
            icon = Icons.Filled.MonitorWeight,
            label = "Weight",
            value = if (pounds) Math.round(weightKg * LB_PER_KG).toInt() else weightKg,
            unit = if (pounds) "lb" else "kg",
            range = if (pounds) WeightRangeLb else WeightRangeKg,
            units = listOf("kg", "lb"),
            selectedUnit = if (pounds) 1 else 0,
            onUnitChange = { weightUnit = if (it == 1) UnitSystem.IMPERIAL else UnitSystem.METRIC },
        ) { weightKg = if (pounds) Math.round(it / LB_PER_KG).toInt() else it }
        Text(
            "You can change units any time in Settings, Units.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.glass.contentMuted,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    display: (Int) -> String = { it.toString() },
    units: List<String> = emptyList(),
    selectedUnit: Int = 0,
    onUnitChange: (Int) -> Unit = {},
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
                    text = display(shown),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.content
                )
            }
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(text = unit, style = MaterialTheme.typography.titleSmall, color = palette.accent)
            }
        }
        if (units.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 70.dp, top = 6.dp)
            ) {
                units.forEachIndexed { index, name ->
                    GlassFilterChip(text = name, selected = index == selectedUnit, onClick = { onUnitChange(index) })
                }
            }
        }
        // Keyed by range so switching units rebuilds the ruler at the converted value
        androidx.compose.runtime.key(range) {
            RulerPicker(value = value.coerceIn(range), range = range, onValueChange = onValueChange)
        }
    }
}

@Preview
@Composable
private fun PhysicalDetailsPreview() {
    ShredzillaTheme {
        PhysicalDetailsScreen(onDetailsSubmitted = { _, _, _, _, _ -> }, onBack = {})
    }
}
