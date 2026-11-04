package com.FreeRave.shredzilla.screens.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.models.TrainingLevel
import com.FreeRave.shredzilla.models.WorkoutProgram
import com.FreeRave.shredzilla.models.workoutPrograms
import com.FreeRave.shredzilla.ui.glass.GlassBottomSheet
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

/**
 * Ready-made splits. Pick a level (it only sets the suggested sets per exercise), preview a split,
 * and add it: each training day becomes one of the user's lists.
 */
@Composable
fun ProgramsScreen(
    recommendedProgramId: String?,
    addedProgramIds: Set<String>,
    onAddProgram: (WorkoutProgram, TrainingLevel) -> Unit,
    onNavigateBack: () -> Unit
) {
    var level by rememberSaveable { mutableStateOf(TrainingLevel.BEGINNER) }
    var previewing by remember { mutableStateOf<WorkoutProgram?>(null) }
    val palette = MaterialTheme.glass
    // Recommended split first, the rest in their usual order
    val programs = remember(recommendedProgramId) { workoutPrograms.sortedByDescending { it.id == recommendedProgramId } }

    GlassSubpageScaffold(title = "Programs", eyebrow = "Ready-made splits", onBack = onNavigateBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingLevel.entries.forEach { option ->
                    GlassFilterChip(text = option.label, selected = level == option, onClick = { level = option })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassChip(text = "${level.setsLabel} per exercise", highlighted = true)
                Spacer(Modifier.width(10.dp))
                Text(level.description, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
        }
        GlassFootnote("Sets are a suggestion. Log as many or as few as you like, and edit any list after adding it.")

        programs.forEach { program ->
            ProgramCard(
                program = program,
                isRecommended = program.id == recommendedProgramId,
                isAdded = program.id in addedProgramIds,
                onClick = { previewing = program }
            )
        }
    }

    previewing?.let { program ->
        ProgramPreviewSheet(
            program = program,
            level = level,
            onAdd = {
                onAddProgram(program, level)
                previewing = null
            },
            onDismiss = { previewing = null }
        )
    }
}

@Composable
private fun ProgramCard(program: WorkoutProgram, isRecommended: Boolean, isAdded: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                program.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.content,
                modifier = Modifier.weight(1f)
            )
            GlassChip(text = program.daysPerWeek)
        }
        if (isRecommended || isAdded) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isRecommended) GlassChip(text = "Recommended for you", highlighted = true)
                if (isAdded) GlassChip(text = "In your lists")
            }
        }
        Text(program.tagline, style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            program.days.forEach { GlassChip(text = it.name) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramPreviewSheet(program: WorkoutProgram, level: TrainingLevel, onAdd: () -> Unit, onDismiss: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassBottomSheet(title = program.name, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${program.daysPerWeek} · ${level.label} · ${level.setsLabel} per exercise",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            program.days.forEach { day ->
                GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${program.shortName} · ${day.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                    day.exercises.forEach { exercise ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exercise, style = MaterialTheme.typography.bodyLarge, color = palette.content, modifier = Modifier.weight(1f))
                            Text(level.setsLabel, style = MaterialTheme.typography.labelLarge, color = palette.accent)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            GlassPrimaryButton(
                text = "Add ${program.days.size} ${if (program.days.size == 1) "list" else "lists"}",
                onClick = onAdd
            )
        }
    }
}

@Preview
@Composable
private fun ProgramsScreenPreview() {
    ShredzillaTheme {
        ProgramsScreen(recommendedProgramId = "ppl", addedProgramIds = setOf("full_body"), onAddProgram = { _, _ -> }, onNavigateBack = {})
    }
}
