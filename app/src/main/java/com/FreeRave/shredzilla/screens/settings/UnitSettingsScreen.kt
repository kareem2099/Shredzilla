package com.FreeRave.shredzilla.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassOptionCard
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import java.util.Locale

/**
 * Weight unit for every load, volume and body weight. Stored by name in Firestore ("unitSystem"),
 * so the names stay METRIC/IMPERIAL even though they now only mean kilograms or pounds.
 */
enum class UnitSystem(val title: String, val symbol: String, val example: String, val icon: ImageVector) {
    METRIC("Kilograms", "kg", "A 20 kg plate · 60 kg × 10", Icons.Filled.FitnessCenter),
    IMPERIAL("Pounds", "lb", "A 45 lb plate · 135 lb × 10", Icons.Filled.Public);

    companion object {
        /** Pounds where people weigh in pounds; kilograms everywhere else. */
        fun forLocale(locale: Locale = Locale.getDefault()): UnitSystem =
            if (locale.country in POUND_COUNTRIES) IMPERIAL else METRIC
    }
}

/** Length unit for body measurements and height; measurements are stored in centimetres. */
enum class LengthUnit(val title: String, val symbol: String, val storedValue: String, val example: String, val icon: ImageVector) {
    CENTIMETRES("Centimetres", "cm", "cm", "Arm 38.5 cm · waist 82 cm", Icons.Filled.Straighten),
    INCHES("Inches", "in", "in", "Arm 15.2 in · waist 32.3 in", Icons.Filled.SquareFoot);

    companion object {
        /** Older accounts had one unit setting for everything, so they keep matching it. */
        fun fromStored(value: String?, weightUnit: UnitSystem): LengthUnit =
            entries.find { it.storedValue == value } ?: if (weightUnit == UnitSystem.IMPERIAL) INCHES else CENTIMETRES

        fun forLocale(locale: Locale = Locale.getDefault()): LengthUnit =
            if (locale.country in INCH_COUNTRIES) INCHES else CENTIMETRES
    }
}

private val POUND_COUNTRIES = setOf("US", "LR", "MM")
private val INCH_COUNTRIES = setOf("US", "LR", "MM")

/** Weights and body measurements are chosen separately; each change saves as soon as it's tapped. */
@Composable
fun UnitSettingsScreen(
    onNavigateBack: () -> Unit,
    weightUnit: UnitSystem,
    lengthUnit: LengthUnit,
    onWeightUnitChange: (UnitSystem) -> Unit,
    onLengthUnitChange: (LengthUnit) -> Unit,
) {
    val palette = MaterialTheme.glass
    GlassSubpageScaffold(title = "Units", eyebrow = "Settings", onBack = onNavigateBack) {
        SectionTitle("Weights")
        UnitSystem.entries.forEach { unit ->
            GlassOptionCard(
                icon = unit.icon,
                title = "${unit.title} (${unit.symbol})",
                subtitle = unit.example,
                selected = weightUnit == unit,
                onClick = { onWeightUnitChange(unit) }
            )
        }
        GlassFootnote("For loads, volume, plate math and body weight. Plate counts on machines stay plates.")

        Spacer(Modifier.height(8.dp))
        SectionTitle("Body measurements")
        LengthUnit.entries.forEach { unit ->
            GlassOptionCard(
                icon = unit.icon,
                title = "${unit.title} (${unit.symbol})",
                subtitle = unit.example,
                selected = lengthUnit == unit,
                onClick = { onLengthUnitChange(unit) }
            )
        }
        GlassFootnote(
            "Everything is saved in kilograms and centimetres, so switching just converts what you see. " +
                "Nothing you've logged changes."
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.glass.contentMuted,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Preview
@Composable
private fun UnitSettingsScreenPreview() {
    ShredzillaTheme {
        UnitSettingsScreen({}, UnitSystem.METRIC, LengthUnit.INCHES, {}, {})
    }
}
