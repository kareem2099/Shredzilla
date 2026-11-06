package com.FreeRave.shredzilla.screens.exercises

import com.FreeRave.shredzilla.models.SetEntry
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.ProgressChartCanvas
import com.FreeRave.shredzilla.models.ExerciseItem
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheet
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassFab
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassStatTile
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatStoredWeight
import com.FreeRave.shredzilla.utils.formatWeightNumber
import com.FreeRave.shredzilla.utils.formatSetLoad
import com.FreeRave.shredzilla.utils.platesLabel
import com.FreeRave.shredzilla.utils.toEntry
import com.FreeRave.shredzilla.utils.nextEntry
import com.FreeRave.shredzilla.utils.sidePrefix
import com.FreeRave.shredzilla.utils.setDetailsLabel
import com.FreeRave.shredzilla.utils.suggestNextSet
import com.FreeRave.shredzilla.utils.personalRecordIds
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Epley estimate of a one-rep max: load × (1 + reps / 30). Works for kg and for plate counts alike. */
private fun estimatedOneRepMax(load: Double, reps: Int): Double = if (reps <= 1) load else load * (1 + reps / 30.0)

private fun dayLabel(dateKey: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) ?: return dateKey
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
    val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .format(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
    return when (dateKey) {
        todayKey -> "Today"
        yesterdayKey -> "Yesterday"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)
    }
}

/**
 * One exercise: what it is (for built-in exercises), how it's trending, and every set from the
 * last 90 days, which can be edited or deleted. Works for any exercise in the user's library.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    info: ExerciseItem?,
    history: List<LoggedSet>,
    volumeTrend: List<MainViewModel.ChartDataPoint>,
    isLoading: Boolean,
    unitSystem: UnitSystem,
    onBack: () -> Unit,
    onRecordSet: (SetEntry) -> Unit,
    onUpdateSet: (set: LoggedSet, entry: SetEntry) -> Unit,
    onDeleteSet: (LoggedSet) -> Unit
) {
    // The set being edited while the sheet is open; null means logging a new one
    var sheetFor by remember { mutableStateOf<LoggedSet?>(null) }
    var isSheetOpen by remember { mutableStateOf(false) }
    var setToDelete by remember { mutableStateOf<LoggedSet?>(null) }

    val sessions = remember(history) { history.groupBy { it.dateKey } } // newest day first
    // Plate counts aren't kilograms, so kg stats only look at weighted sets
    val weightedSets = remember(history) { history.filter { it.plates == null && !it.bodyweight } }
    val bestSet = remember(history) { weightedSets.maxByOrNull { it.weightKg } }
    val mostPlates = remember(history) { history.mapNotNull { it.plates }.maxOrNull() }
    val bestOneRepMaxKg = remember(history) { weightedSets.maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) } }
    val bestOneRepMaxPlates = remember(history) {
        history.mapNotNull { set -> set.plates?.let { estimatedOneRepMax(it.toDouble(), set.reps) } }.maxOrNull()
    }
    val recordIds = remember(history) { personalRecordIds(history) }

    fun openSheet(editing: LoggedSet?) {
        sheetFor = editing
        isSheetOpen = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(title = exerciseName, eyebrow = "Exercise", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (info != null) item { AboutCard(info) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassStatTile(Icons.Filled.EventAvailable, "Sessions", sessions.size.toString(), Modifier.weight(1f))
                        // Bodyweight-only exercises have no heaviest load; their best is the most reps
                        val heaviest = bestSet?.let { formatStoredWeight(it.weightKg, unitSystem) } ?: mostPlates?.let(::platesLabel)
                        val mostReps = history.filter { it.bodyweight }.maxOfOrNull { it.reps }
                        GlassStatTile(
                            Icons.Filled.FitnessCenter,
                            if (heaviest == null && mostReps != null) "Most reps" else "Heaviest",
                            heaviest ?: mostReps?.toString() ?: "—",
                            Modifier.weight(1f)
                        )
                        GlassStatTile(
                            Icons.Filled.EmojiEvents, "Est. 1RM",
                            bestOneRepMaxKg?.let { formatStoredWeight(it, unitSystem) }
                                ?: bestOneRepMaxPlates?.let { "${formatWeightNumber(it)} plates" }
                                ?: "—",
                            Modifier.weight(1f)
                        )
                    }
                }
                if (volumeTrend.size > 1) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Volume per session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.glass.content)
                            ProgressChartCanvas(volumeTrend, Modifier.fillMaxWidth().height(150.dp))
                        }
                    }
                }
                item {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.glass.content,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                when {
                    isLoading && history.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.glass.accent)
                        }
                    }
                    history.isEmpty() -> item { EmptyHistoryCard() }
                    else -> items(sessions.entries.toList(), key = { it.key }) { (dateKey, sets) ->
                        SessionCard(
                            dateKey = dateKey,
                            sets = sets.sortedBy { it.timestampMillis },
                            unitSystem = unitSystem,
                            recordIds = recordIds,
                            onEdit = ::openSheet,
                            onDelete = { setToDelete = it }
                        )
                    }
                }
            }
        }
        GlassFab(
            icon = Icons.Filled.Add,
            text = "Log set",
            onClick = { openSheet(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
        )
    }

    val editing = sheetFor
    RecordSetSheet(
        exerciseName = exerciseName.takeIf { isSheetOpen },
        unitSystem = unitSystem,
        isEditing = editing != null,
        // New sets start from the latest one (other side for one-sided work), so the load is ready to go
        initial = editing?.toEntry(unitSystem) ?: history.firstOrNull()?.nextEntry(unitSystem) ?: SetEntry(),
        suggestion = remember(history) { suggestNextSet(history, unitSystem) },
        onRecordSet = { _, entry ->
            if (editing != null) onUpdateSet(editing, entry) else onRecordSet(entry)
        },
        onDismiss = { isSheetOpen = false; sheetFor = null }
    )

    setToDelete?.let { set ->
        GlassDialog(
            title = "Delete this set?",
            message = "${set.reps} reps × ${formatSetLoad(set.weightKg, set.plates, unitSystem, set.bodyweight, set.perSide)} on ${dayLabel(set.dateKey)}. This can't be undone.",
            confirmText = "Delete",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                onDeleteSet(set)
                setToDelete = null
            },
            onDismiss = { setToDelete = null }
        )
    }
}

@Composable
private fun AboutCard(info: ExerciseItem) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content, modifier = Modifier.weight(1f))
            GlassChip(text = info.difficulty, highlighted = true)
        }
        Text(info.description, style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            info.targetMuscles.take(3).forEach { GlassChip(text = it) }
        }
        if (info.equipmentNeeded.isNotEmpty()) {
            Text(
                "Equipment: ${info.equipmentNeeded.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted
            )
        }
    }
}

@Composable
private fun SessionCard(
    dateKey: String,
    sets: List<LoggedSet>,
    unitSystem: UnitSystem,
    recordIds: Set<String>,
    onEdit: (LoggedSet) -> Unit,
    onDelete: (LoggedSet) -> Unit
) {
    val palette = MaterialTheme.glass
    val volumeKg = sets.sumOf { it.volumeKg }
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(dayLabel(dateKey), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.content, modifier = Modifier.weight(1f))
            Text(
                if (volumeKg > 0) "Volume ${formatStoredWeight(volumeKg, unitSystem)}" else "${sets.size} sets",
                style = MaterialTheme.typography.labelMedium,
                color = palette.contentMuted
            )
        }
        sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(set) }
                    .padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).glassSurface(palette, CircleShape)) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.accent)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${sidePrefix(set.side)}${formatSetLoad(set.weightKg, set.plates, unitSystem, set.bodyweight, set.perSide)}  ×  ${set.reps} reps",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.content
                        )
                        if (set.docId in recordIds) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Personal record", tint = palette.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    setDetailsLabel(set.setType, set.tempo)?.let { details ->
                        Text(details, style = MaterialTheme.typography.labelMedium, color = palette.accent)
                    }
                    if (set.notes.isNotBlank()) {
                        Text(set.notes, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = { onDelete(set) }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete set", tint = palette.contentMuted)
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.History), size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                "No sets in the last 90 days. Tap Log set to record your first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted
            )
        }
    }
}

@Preview
@Composable
private fun ExerciseDetailScreenPreview() {
    ShredzillaTheme {
        ExerciseDetailScreen(
            exerciseName = "Bench Press",
            info = null,
            history = listOf(
                LoggedSet("a", "2026-11-04", "bench_press", 8, 60.0, "", 2),
                LoggedSet("b", "2026-11-04", "bench_press", 6, 65.0, "Felt strong", 3)
            ),
            volumeTrend = emptyList(),
            isLoading = false,
            unitSystem = UnitSystem.METRIC,
            onBack = {},
            onRecordSet = {},
            onUpdateSet = { _, _ -> },
            onDeleteSet = {}
        )
    }
}
