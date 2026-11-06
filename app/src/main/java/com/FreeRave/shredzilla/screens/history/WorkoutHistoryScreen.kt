package com.FreeRave.shredzilla.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatDuration
import com.FreeRave.shredzilla.utils.formatStoredWeight

/** Every finished workout, newest first. */
@Composable
fun WorkoutHistoryScreen(logs: List<WorkoutLog>, unitSystem: UnitSystem, onOpenLog: (String) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = "History", eyebrow = "${logs.size} ${if (logs.size == 1) "workout" else "workouts"}", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    GlassSettingsGroup {
                        GlassSettingsRow(
                            icon = Icons.Filled.History,
                            title = "No workouts yet",
                            subtitle = "Open a list and tap Start workout. Finished workouts land here.",
                            onClick = onBack,
                            showChevron = false
                        )
                    }
                }
            }
            items(logs, key = { it.id }) { log -> WorkoutLogCard(log, unitSystem, onClick = { onOpenLog(log.id) }) }
        }
    }
}

@Composable
private fun WorkoutLogCard(log: WorkoutLog, unitSystem: UnitSystem, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(log.listName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            Text(workoutDateLabel(log.startedAtMillis), style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassChip(text = formatDuration(log.durationMillis))
            GlassChip(text = "${log.setCount} sets")
            if (log.volumeKg > 0) GlassChip(text = formatStoredWeight(log.volumeKg, unitSystem))
            if (log.records.isNotEmpty()) GlassChip(text = "🏆 ${log.records.size}", highlighted = true)
        }
    }
}

/** One saved workout as its share card; can be shared again or deleted. */
@Composable
fun WorkoutLogScreen(log: WorkoutLog?, unitSystem: UnitSystem, onDelete: () -> Unit, onBack: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            title = log?.listName ?: "Workout",
            eyebrow = log?.let { workoutDateLabel(it.startedAtMillis) },
            onBack = onBack,
            actions = {
                if (log != null) GlassIconButton(Icons.Filled.DeleteOutline, contentDescription = "Delete workout", onClick = { confirmDelete = true })
            }
        )
        if (log != null) {
            WorkoutLogContent(
                log = log,
                unitSystem = unitSystem,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
    if (confirmDelete) {
        GlassDialog(
            title = "Delete this workout?",
            message = "It's removed from your history. The sets you logged stay in each exercise's history.",
            confirmText = "Delete",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
