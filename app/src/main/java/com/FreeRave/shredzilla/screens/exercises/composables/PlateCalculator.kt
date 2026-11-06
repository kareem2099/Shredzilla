package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.barWeights
import com.FreeRave.shredzilla.utils.formatWeight
import com.FreeRave.shredzilla.utils.formatWeightNumber
import com.FreeRave.shredzilla.utils.platesPerSide

/** Collapsed by default: "Plate calculator" opens which plates go on each side of the bar for [weight]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculator(weight: Double, unitSystem: UnitSystem, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var barWeight by rememberSaveable(unitSystem) { mutableDoubleStateOf(barWeights(unitSystem).first()) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassFilterChip(
            text = if (isOpen) "Hide plate calculator" else "Plate calculator",
            selected = isOpen,
            onClick = { isOpen = !isOpen }
        )
        AnimatedVisibility(visible = isOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bar", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
                    barWeights(unitSystem).forEach { bar ->
                        GlassFilterChip(text = formatWeight(bar, unitSystem), selected = bar == barWeight, onClick = { barWeight = bar })
                    }
                }
                val load = platesPerSide(weight, barWeight, unitSystem)
                when {
                    load == null -> Text("Lighter than the bar", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
                    load.perSide.isEmpty() && load.leftover == 0.0 ->
                        Text("Just the empty bar", style = MaterialTheme.typography.bodyMedium, color = palette.content)
                    else -> {
                        Text("Each side", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = palette.contentMuted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            load.perSide.forEach { plate -> GlassChip(text = formatWeightNumber(plate), highlighted = true) }
                        }
                        if (load.leftover > 0) {
                            Text(
                                "${formatWeight(load.leftover, unitSystem)} short: standard plates can't make this exact weight",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.contentMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
