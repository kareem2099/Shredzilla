package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.models.ExerciseItem
import com.FreeRave.shredzilla.models.initialGlobalExerciseList
import com.FreeRave.shredzilla.onboarding.components.OnboardingScaffold
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassSelectableCard
import com.FreeRave.shredzilla.ui.glass.GlassSelectionIndicator
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

/** Step 4 — which starter exercises go into the user's library. Everything is pre-selected. */
@Composable
fun InitialExercisesScreen(
    onExercisesSelected: (List<String>) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    val exercises = initialGlobalExerciseList
    var selectedIds by rememberSaveable { mutableStateOf(exercises.map { it.id }) }
    val allSelected = selectedIds.size == exercises.size
    val palette = MaterialTheme.glass

    OnboardingScaffold(
        step = 4,
        title = "Pick your lifts",
        subtitle = "Start with the exercises you'll actually do. You can add more anytime.",
        ctaText = if (selectedIds.isEmpty()) "Skip for now" else "Continue with ${selectedIds.size}",
        ctaLoading = isLoading,
        onBack = onBack,
        onCta = { onExercisesSelected(selectedIds) }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${selectedIds.size} of ${exercises.size} selected",
                style = MaterialTheme.typography.labelLarge,
                color = palette.contentMuted,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { selectedIds = if (allSelected) emptyList() else exercises.map { it.id } }) {
                Text(if (allSelected) "Clear all" else "Select all", color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        }
        exercises.forEach { exercise ->
            val selected = exercise.id in selectedIds
            ExerciseOptionCard(
                exercise = exercise,
                selected = selected,
                onToggle = { selectedIds = if (selected) selectedIds - exercise.id else selectedIds + exercise.id }
            )
        }
    }
}

@Composable
private fun ExerciseOptionCard(exercise: ExerciseItem, selected: Boolean, onToggle: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassSelectableCard(
        selected = selected,
        onClick = onToggle,
        role = Role.Checkbox,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.content
                )
                Spacer(Modifier.width(8.dp))
                GlassChip(text = exercise.difficulty)
            }
            Text(
                text = exercise.targetMuscles.take(3).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        GlassSelectionIndicator(selected = selected)
    }
}

@Preview
@Composable
private fun InitialExercisesPreview() {
    ShredzillaTheme {
        InitialExercisesScreen(onExercisesSelected = {}, onBack = {})
    }
}
