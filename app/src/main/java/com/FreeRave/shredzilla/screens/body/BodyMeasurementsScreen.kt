package com.FreeRave.shredzilla.screens.body

import com.FreeRave.shredzilla.models.MeasurementCadence
import com.FreeRave.shredzilla.utils.BodyChange
import com.FreeRave.shredzilla.utils.BodyInsights
import com.FreeRave.shredzilla.utils.InsightTone
import com.FreeRave.shredzilla.utils.bodyInsights
import com.FreeRave.shredzilla.utils.lengthUnit
import com.FreeRave.shredzilla.utils.weightUnit
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.ProgressChartCanvas
import com.FreeRave.shredzilla.models.BodyMeasurement
import com.FreeRave.shredzilla.models.MeasurementSite
import com.FreeRave.shredzilla.models.MeasurementSlot
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassFab
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.cmToDisplay
import com.FreeRave.shredzilla.utils.formatLength
import com.FreeRave.shredzilla.utils.formatStoredWeight
import com.FreeRave.shredzilla.utils.formatWeightNumber
import com.FreeRave.shredzilla.utils.kgToDisplay
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val SHOWN_DIFFERENCE = 0.05 // smaller changes are tape-measure noise
private const val BODY_WEIGHT_TREND = "BODY_WEIGHT"

fun measurementDateLabel(millis: Long): String = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(millis))

/** "+1.5" / "−3", or null when there's no real change. */
private fun signed(value: Double): String? = when {
    abs(value) < SHOWN_DIFFERENCE -> null
    value > 0 -> "+${formatWeightNumber(value)}"
    else -> "−${formatWeightNumber(-value)}"
}

/**
 * Body measurements: the latest reading of each site with the change since the one before and the
 * right-vs-left difference, a trend chart for any site, and every measuring session.
 */
@Composable
fun BodyMeasurementsScreen(
    measurements: List<BodyMeasurement>,
    unitSystem: UnitSystem,
    cadence: MeasurementCadence,
    onCadenceChange: (MeasurementCadence) -> Unit,
    onNew: () -> Unit,
    onOpen: (id: String) -> Unit,
    onBack: () -> Unit
) {
    val latest = measurements.firstOrNull()
    // The value before the latest one, from whichever older session measured that slot
    fun previousOf(slot: MeasurementSlot): Double? = measurements.drop(1).firstNotNullOfOrNull { it.valueOf(slot) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = "Body", eyebrow = "Measurements", onBack = onBack)
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { CadenceCard(cadence, latest?.takenAtMillis, onCadenceChange) }
                if (latest == null) {
                    item {
                        GlassSettingsGroup {
                            GlassSettingsRow(
                                icon = Icons.Filled.Straighten,
                                title = "No measurements yet",
                                subtitle = "Track arms, thighs, calves, chest and waist, right and left, over time",
                                onClick = onNew
                            )
                        }
                    }
                } else {
                    item { LatestCard(latest, measurements.drop(1).firstNotNullOfOrNull { it.bodyWeightKg }, ::previousOf, unitSystem) }
                    bodyInsights(measurements, unitSystem)?.let { insights ->
                        if (insights.verdict != null || insights.changes.isNotEmpty() || insights.sinceFirst.isNotEmpty() || insights.imbalance != null) {
                            item { InsightsCard(insights, unitSystem) }
                        }
                    }
                    item { TrendCard(measurements, unitSystem) }
                    item {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.glass.content,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(measurements, key = { it.id }) { measurement ->
                        EntryCard(measurement, unitSystem, onClick = { onOpen(measurement.id) })
                    }
                }
            }
        }
        GlassFab(
            icon = Icons.Filled.Add,
            text = "Measure",
            onClick = onNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
        )
    }
}

@Composable
private fun LatestCard(
    latest: BodyMeasurement,
    previousBodyWeightKg: Double?,
    previousOf: (MeasurementSlot) -> Double?,
    unitSystem: UnitSystem
) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text("Latest · ${measurementDateLabel(latest.takenAtMillis)}", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
            if (latest.note.isNotBlank()) {
                Text(latest.note, style = MaterialTheme.typography.bodyMedium, color = palette.content)
            }
        }
        latest.bodyWeightKg?.let { kg ->
            SiteBlock(label = "Body weight", imbalance = null) {
                SlotValue(
                    caption = null,
                    value = formatStoredWeight(kg, unitSystem),
                    change = previousBodyWeightKg?.let { signed(kgToDisplay(kg - it, unitSystem)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        MeasurementSite.entries.filter { site -> site.slots.any { latest.valueOf(it) != null } }.forEach { site ->
            val right = latest.valueOf(MeasurementSlot(site, Side.RIGHT))
            val left = latest.valueOf(MeasurementSlot(site, Side.LEFT))
            // Which side is bigger and by how much, e.g. "Right +5"
            val imbalance = if (site.sided && right != null && left != null) {
                signed(cmToDisplay(right - left, unitSystem))?.let { "Right $it" }
            } else null
            SiteBlock(label = site.label, imbalance = imbalance) {
                site.slots.forEach { slot ->
                    val value = latest.valueOf(slot)
                    SlotValue(
                        caption = slot.side?.label,
                        value = value?.let { formatLength(it, unitSystem) } ?: "—",
                        change = value?.let { v -> previousOf(slot)?.let { signed(cmToDisplay(v - it, unitSystem)) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SiteBlock(label: String, imbalance: String?, values: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val palette = MaterialTheme.glass
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.content, modifier = Modifier.weight(1f))
            if (imbalance != null) GlassChip(text = imbalance)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), content = values)
    }
}

@Composable
private fun SlotValue(caption: String?, value: String, change: String?, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    Column(modifier = modifier) {
        if (caption != null) Text(caption, style = MaterialTheme.typography.labelSmall, color = palette.contentMuted)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = palette.content)
            if (change != null) {
                Text(change, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.accent, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

/** Trend of one site (one side at a time) across every session that measured it. */
@Composable
private fun TrendCard(measurements: List<BodyMeasurement>, unitSystem: UnitSystem) {
    val palette = MaterialTheme.glass
    val oldestFirst = remember(measurements) { measurements.sortedBy { it.takenAtMillis } }
    // Only what was measured at least twice has a trend to draw; body weight comes first
    val sites = remember(measurements) {
        MeasurementSite.entries.filter { site -> site.slots.any { slot -> oldestFirst.count { it.valueOf(slot) != null } >= 2 } }
    }
    val hasWeightTrend = oldestFirst.count { it.bodyWeightKg != null } >= 2
    val options = listOfNotNull(BODY_WEIGHT_TREND.takeIf { hasWeightTrend }) + sites.map { it.name }
    if (options.isEmpty()) return
    var selected by rememberSaveable { mutableStateOf(options.first()) }
    var side by rememberSaveable { mutableStateOf(Side.RIGHT) }
    val choice = selected.takeIf { it in options } ?: options.first()
    val site = sites.find { it.name == choice }
    val slot = site?.let { MeasurementSlot(it, if (it.sided) side else null) }
    // Values in the user's unit, oldest first
    val series = oldestFirst.mapNotNull { m ->
        val value = if (slot == null) m.bodyWeightKg?.let { kgToDisplay(it, unitSystem) } else m.valueOf(slot)?.let { cmToDisplay(it, unitSystem) }
        value?.let { m.takenAtMillis to it }
    }
    val points = series.map { (at, value) -> MainViewModel.ChartDataPoint(at, value) }
    val unitLabel = if (slot == null) unitSystem.weightUnit() else unitSystem.lengthUnit()

    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                GlassFilterChip(
                    text = if (option == BODY_WEIGHT_TREND) "Body weight" else MeasurementSite.valueOf(option).label,
                    selected = option == choice,
                    onClick = { selected = option }
                )
            }
        }
        if (site?.sided == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Side.entries.forEach { option -> GlassFilterChip(text = option.label, selected = option == side, onClick = { side = option }) }
            }
        }
        if (points.size >= 2) {
            ProgressChartCanvas(points, Modifier.fillMaxWidth().height(140.dp))
            Text(
                "${formatWeightNumber(series.first().second)} → ${formatWeightNumber(series.last().second)} $unitLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted
            )
        } else {
            Text("Measure this side twice to see a trend.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
        }
    }
}

/** Measure weekly or monthly, never daily; shows when the next one is due. */
@Composable
private fun CadenceCard(cadence: MeasurementCadence, lastMillis: Long?, onChange: (MeasurementCadence) -> Unit) {
    val palette = MaterialTheme.glass
    val daysSince = lastMillis?.let { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it).toInt() }
    val dueIn = daysSince?.let { cadence.days - it }
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Measure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MeasurementCadence.entries.forEach { option ->
                    GlassFilterChip(text = option.label, selected = option == cadence, onClick = { onChange(option) })
                }
            }
        }
        Text(
            when {
                dueIn == null -> "Once a week or once a month is plenty. Day-to-day numbers mostly move with water and food."
                dueIn <= 0 -> "Due now. Your last measurement was $daysSince days ago."
                dueIn == 1 -> "Next measurement tomorrow."
                else -> "Next measurement in $dueIn days."
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (dueIn != null && dueIn <= 0) FontWeight.Bold else FontWeight.Normal,
            color = if (dueIn != null && dueIn <= 0) palette.accent else palette.content
        )
        Text(
            "Same conditions every time: in the morning, before eating, with the tape on the same spot.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.contentMuted
        )
    }
}

/** Plain-language read of the latest changes, the change since the first session, and any side gap. */
@Composable
private fun InsightsCard(insights: BodyInsights, unitSystem: UnitSystem) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Insights, contentDescription = null, tint = palette.accent)
            Text(
                "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        insights.verdict?.let { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = palette.content) }
        if (insights.changes.isNotEmpty()) ChangeGroup("Since last time", insights.changes)
        if (insights.sinceFirst.isNotEmpty() && insights.firstDateMillis != null) {
            ChangeGroup("Since ${measurementDateLabel(insights.firstDateMillis)}", insights.sinceFirst)
        }
        insights.imbalance?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangeGroup(title: String, changes: List<BodyChange>) {
    val palette = MaterialTheme.glass
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            changes.forEach { change ->
                val arrow = if (change.change.startsWith("+")) "▲" else "▼"
                GlassChip(text = "$arrow ${change.label} ${change.change}", highlighted = change.tone == InsightTone.GOOD)
            }
        }
    }
}

@Composable
private fun EntryCard(measurement: BodyMeasurement, unitSystem: UnitSystem, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(measurementDateLabel(measurement.takenAtMillis), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            if (measurement.note.isNotBlank()) {
                Text(measurement.note, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            measurement.bodyWeightKg?.let { GlassChip(text = formatStoredWeight(it, unitSystem)) }
            val count = measurement.valuesCm.size
            GlassChip(text = "$count ${if (count == 1) "value" else "values"}")
        }
    }
}
