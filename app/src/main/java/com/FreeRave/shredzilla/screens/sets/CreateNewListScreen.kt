package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.models.exerciseIdOf
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSearchField
import com.FreeRave.shredzilla.ui.glass.GlassSelectableCard
import com.FreeRave.shredzilla.ui.glass.GlassSelectionIndicator
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

private val NameSuggestions = listOf("Push day", "Pull day", "Leg day", "Upper body", "Full body")

/**
 * Create a workout list, or edit one when [isEditing]. Lists store exercise ids, so selection is
 * tracked by id while the UI shows names.
 */
@Composable
fun CreateNewListScreen(
    exerciseNames: List<String>,
    onNavigateBack: () -> Unit,
    onSave: (listName: String, exerciseIds: List<String>) -> Unit,
    isEditing: Boolean = false,
    initialName: String = "",
    initialSelectedIds: List<String> = emptyList()
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var selectedIds by rememberSaveable { mutableStateOf(initialSelectedIds) }
    var query by rememberSaveable { mutableStateOf("") }
    val palette = MaterialTheme.glass

    // A new list needs at least one exercise; an existing one may be emptied on purpose
    val missing = when {
        name.isBlank() -> "Give your list a name"
        selectedIds.isEmpty() && !isEditing -> "Pick at least one exercise"
        else -> null
    }
    val visibleExercises = exerciseNames.filter { it.contains(query.trim(), ignoreCase = true) }

    GlassSubpageScaffold(
        title = if (isEditing) "Edit list" else "New list",
        eyebrow = "Your lists",
        onBack = onNavigateBack,
        bottomBar = {
            GlassPrimaryButton(
                text = missing ?: if (isEditing) "Save changes" else "Create list",
                enabled = missing == null,
                onClick = { onSave(name.trim(), selectedIds) }
            )
        }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TextFormField(
                value = name,
                onValueChange = { name = it },
                label = "List name",
                imeAction = ImeAction.Done,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) }
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NameSuggestions.forEach { suggestion ->
                    GlassFilterChip(text = suggestion, selected = name == suggestion, onClick = { name = suggestion })
                }
            }
        }

        Row(modifier = Modifier.padding(start = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "EXERCISES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.contentMuted,
                modifier = Modifier.weight(1f)
            )
            Text("${selectedIds.size} selected", style = MaterialTheme.typography.labelMedium, color = palette.accent)
        }
        if (exerciseNames.size > 6) {
            GlassSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search your library")
        }
        if (exerciseNames.isEmpty()) {
            GlassFootnote("Your library is empty. Add exercises from the Sets tab first.")
        }
        visibleExercises.forEach { exerciseName ->
            val id = exerciseIdOf(exerciseName)
            val isSelected = id in selectedIds
            GlassSelectableCard(
                selected = isSelected,
                role = Role.Checkbox,
                onClick = { selectedIds = if (isSelected) selectedIds - id else selectedIds + id },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.content,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                GlassSelectionIndicator(selected = isSelected)
            }
        }
    }
}

@Preview
@Composable
private fun CreateNewListScreenPreview() {
    ShredzillaTheme {
        CreateNewListScreen(
            exerciseNames = listOf("Bench Press", "Squat", "Deadlift"),
            onNavigateBack = {},
            onSave = { _, _ -> },
            isEditing = true,
            initialName = "Push day",
            initialSelectedIds = listOf("bench_press")
        )
    }
}
