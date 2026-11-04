package com.FreeRave.shredzilla.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassOptionCard
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

enum class UnitSystem(val title: String, val units: String, val icon: ImageVector) {
    METRIC("Metric", "Kilograms · kilometres", Icons.Filled.Straighten),
    IMPERIAL("Imperial", "Pounds · miles", Icons.Filled.Public)
}

/** Unit picker; the choice is saved as soon as it's tapped. */
@Composable
fun UnitSettingsScreen(
    onNavigateBack: () -> Unit,
    currentUnitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit
) {
    var selected by remember { mutableStateOf(currentUnitSystem) }

    GlassSubpageScaffold(title = "Units", eyebrow = "Settings", onBack = onNavigateBack) {
        UnitSystem.entries.forEach { unit ->
            GlassOptionCard(
                icon = unit.icon,
                title = unit.title,
                subtitle = unit.units,
                selected = selected == unit,
                onClick = {
                    selected = unit
                    onUnitSystemChange(unit)
                }
            )
        }
        // Weights are stored in kg, so switching converts every logged set for display
        GlassFootnote("Your history is saved in kilograms, so switching converts every set you've logged.")
    }
}

@Preview
@Composable
private fun UnitSettingsScreenPreview() {
    ShredzillaTheme {
        UnitSettingsScreen(onNavigateBack = {}, currentUnitSystem = UnitSystem.METRIC, onUnitSystemChange = {})
    }
}
