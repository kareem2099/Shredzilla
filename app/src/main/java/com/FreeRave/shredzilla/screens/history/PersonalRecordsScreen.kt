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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.models.PersonalRecord
import com.FreeRave.shredzilla.models.RecordSet
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatStoredWeight
import com.FreeRave.shredzilla.utils.platesLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun recordDate(millis: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

/** All-time bests per exercise, most recently beaten first. */
@Composable
fun PersonalRecordsScreen(
    records: List<PersonalRecord>,
    unitSystem: UnitSystem,
    onOpenExercise: (exerciseName: String) -> Unit,
    onDeleteRecord: (exerciseId: String) -> Unit,
    onBack: () -> Unit
) {
    var recordToDelete by remember { mutableStateOf<PersonalRecord?>(null) }
    val sorted = remember(records) { records.sortedByDescending { it.latestMillis } }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = "Records", eyebrow = "Your all-time bests", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sorted.isEmpty()) {
                item {
                    GlassSettingsGroup {
                        GlassSettingsRow(
                            icon = Icons.Filled.EmojiEvents,
                            title = "No records yet",
                            subtitle = "Log weighted sets and your best for each exercise shows up here",
                            onClick = onBack,
                            showChevron = false
                        )
                    }
                }
            }
            items(sorted, key = { it.exerciseId }) { record ->
                RecordCard(
                    record = record,
                    unitSystem = unitSystem,
                    onClick = { onOpenExercise(record.exerciseName) },
                    onDelete = { recordToDelete = record }
                )
            }
        }
    }

    recordToDelete?.let { record ->
        GlassDialog(
            title = "Reset ${record.exerciseName}?",
            message = "For a set logged by mistake. The record is rebuilt from the sets still in your recent history.",
            confirmText = "Reset",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                onDeleteRecord(record.exerciseId)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }
}

@Composable
private fun RecordCard(record: PersonalRecord, unitSystem: UnitSystem, onClick: () -> Unit, onDelete: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = palette.accent)
            Text(
                record.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                modifier = Modifier.weight(1f).padding(start = 10.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Reset record", tint = palette.contentMuted)
            }
        }
        record.heaviest?.let { RecordLine("Heaviest", "${formatStoredWeight(it.load, unitSystem)} × ${it.reps}", it) }
        record.mostPlates?.let { RecordLine("Most plates", "${platesLabel(it.load.toInt())} × ${it.reps}", it) }
    }
}

@Composable
private fun RecordLine(label: String, value: String, record: RecordSet) {
    val palette = MaterialTheme.glass
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = palette.contentMuted)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = palette.content)
        }
        Text(recordDate(record.atMillis), style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
    }
}
