package com.FreeRave.shredzilla.screens.exercises

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSearchField
import com.FreeRave.shredzilla.ui.glass.GlassSelectableCard
import com.FreeRave.shredzilla.ui.glass.GlassSelectionIndicator
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

private val PopularExercises = listOf(
    "Bench Press", "Squat", "Deadlift", "Lat Pulldown", "Incline Dumbbell Press",
    "Leg Extension", "Incline Bench Press", "Pull-up", "Overhead Press", "Barbell Row"
)

/**
 * Grow the library: tick popular exercises and add them together, or type a new name and create it.
 * Nothing is saved until the user confirms, and exercises already in the library can't be re-added.
 */
@Composable
fun AddExerciseScreen(
    libraryExercises: List<String>,
    onCreateExercise: (String) -> Unit,
    onAddExercises: (List<String>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    val palette = MaterialTheme.glass

    fun inLibrary(name: String) = libraryExercises.any { it.equals(name, ignoreCase = true) }
    val typedName = query.trim()
    val canCreate = typedName.isNotEmpty() && !inLibrary(typedName)
    val suggestions = PopularExercises.filter { it.contains(typedName, ignoreCase = true) }

    GlassSubpageScaffold(
        title = "Add exercise",
        eyebrow = "Library",
        onBack = onNavigateBack,
        bottomBar = if (selected.isEmpty()) null else {
            {
                GlassPrimaryButton(
                    text = "Add ${selected.size} ${if (selected.size == 1) "exercise" else "exercises"}",
                    onClick = { onAddExercises(selected) }
                )
            }
        }
    ) {
        GlassSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search or type a new name")

        when {
            canCreate -> GlassSettingsGroup {
                GlassSettingsRow(
                    icon = Icons.Filled.AddCircle,
                    title = "Create \"$typedName\"",
                    subtitle = "Add it to your library as a new exercise",
                    onClick = { onCreateExercise(typedName) }
                )
            }
            typedName.isNotEmpty() -> GlassFootnote("\"$typedName\" is already in your library.")
        }

        if (suggestions.isNotEmpty()) {
            Text(
                "POPULAR",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.contentMuted,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp)
            )
        }
        suggestions.forEach { name ->
            val alreadyAdded = inLibrary(name)
            val isSelected = name in selected
            GlassSelectableCard(
                selected = isSelected,
                role = Role.Checkbox,
                onClick = { if (!alreadyAdded) selected = if (isSelected) selected - name else selected + name },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (alreadyAdded) palette.contentMuted else palette.content
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (alreadyAdded) GlassChip(text = "In library") else GlassSelectionIndicator(selected = isSelected)
            }
        }
    }
}

@Preview
@Composable
private fun AddExerciseScreenPreview() {
    ShredzillaTheme {
        AddExerciseScreen(
            libraryExercises = listOf("Bench Press", "Squat"),
            onCreateExercise = {},
            onAddExercises = {},
            onNavigateBack = {}
        )
    }
}
