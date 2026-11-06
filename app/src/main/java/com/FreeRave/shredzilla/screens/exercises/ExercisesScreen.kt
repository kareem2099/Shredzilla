package com.FreeRave.shredzilla.screens.exercises

import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.utils.SetPrefill
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheet
import com.FreeRave.shredzilla.screens.exercises.composables.SwipableExerciseListItem
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassFab
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassSearchField
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

enum class SortOption(val label: String) {
    MOST_RECENTLY_DONE("Recent"),
    ALPHABETICAL("A–Z")
}

/** The user's exercise library: search, sort, record a set, open details or delete. */
@Composable
fun ExercisesScreen(
    exercisesDisplayInfo: List<ExerciseDisplayInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToAddExercise: () -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteExercise: (String) -> Unit,
    onRecordExercise: (exerciseName: String, entry: SetEntry) -> Unit,
    /** Where a new set starts (the last one logged) and what to try next. */
    prefillFor: (exerciseName: String) -> SetPrefill,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit,
    unitSystem: UnitSystem,
    onOpenExercise: (exerciseName: String) -> Unit
) {
    var sortOption by rememberSaveable { mutableStateOf(SortOption.MOST_RECENTLY_DONE) }
    var exerciseToRecord by remember { mutableStateOf<String?>(null) }
    var exerciseToDelete by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val sortedExercises = remember(sortOption, exercisesDisplayInfo) {
        when (sortOption) {
            // Never-done exercises sink to the bottom, then alphabetical among themselves
            SortOption.MOST_RECENTLY_DONE -> exercisesDisplayInfo.sortedWith(
                compareByDescending<ExerciseDisplayInfo> { it.lastPerformedMillis ?: Long.MIN_VALUE }.thenBy { it.name }
            )
            SortOption.ALPHABETICAL -> exercisesDisplayInfo.sortedBy { it.name.lowercase() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Exercise library",
                eyebrow = "${exercisesDisplayInfo.size} exercises",
                onBack = onNavigateBack
            )
            if (isTimerRunning) {
                RestTimerBar(remainingSeconds = timerRemainingSeconds, totalSeconds = timerTotalSeconds, onClose = onCloseTimer)
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSearchField(query = searchQuery, onQueryChange = onSearchQueryChange, placeholder = "Search exercises")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortOption.entries.forEach { option ->
                        GlassFilterChip(text = option.label, selected = sortOption == option, onClick = { sortOption = option })
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    // Dragging the list dismisses the keyboard, like most search UIs
                    .pointerInput(Unit) { detectDragGestures { _, _ -> focusManager.clearFocus() } },
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sortedExercises.isEmpty()) {
                    item { EmptyLibraryCard(isSearching = searchQuery.isNotBlank()) }
                }
                items(sortedExercises, key = { it.id }, contentType = { "exercise" }) { exercise ->
                    SwipableExerciseListItem(
                        exerciseName = exercise.name,
                        lastPerformed = exercise.lastPerformed,
                        onDelete = { exerciseToDelete = exercise.name },
                        onRecord = { exerciseToRecord = exercise.name },
                        onClick = { onOpenExercise(exercise.name) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        GlassFab(
            icon = Icons.Filled.Add,
            text = "Add exercise",
            onClick = onNavigateToAddExercise,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
        )
    }

    val prefill = exerciseToRecord?.let(prefillFor) ?: SetPrefill()
    RecordSetSheet(
        exerciseName = exerciseToRecord,
        unitSystem = unitSystem,
        onRecordSet = onRecordExercise,
        initial = prefill.initial,
        suggestion = prefill.suggestion,
        onDismiss = { exerciseToRecord = null }
    )

    exerciseToDelete?.let { name ->
        GlassDialog(
            title = "Delete $name?",
            message = "It's removed from the exercise library. Sets you've already logged stay in your history.",
            confirmText = "Delete",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                onDeleteExercise(name)
                exerciseToDelete = null
            },
            onDismiss = { exerciseToDelete = null }
        )
    }
}

@Composable
private fun EmptyLibraryCard(isSearching: Boolean) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassIconBadge(
            painter = rememberVectorPainter(Icons.Filled.SearchOff),
            size = 56.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = if (isSearching) "No matches" else "Your library is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.content,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = if (isSearching) "Try another name, or add it as a new exercise." else "Add the exercises you train to start logging sets.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.contentMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun ExercisesScreenPreview() {
    ShredzillaTheme {
        ExercisesScreen(
            exercisesDisplayInfo = listOf(
                ExerciseDisplayInfo(id = "bench", name = "Bench Press", lastPerformed = "Today"),
                ExerciseDisplayInfo(id = "squat", name = "Squat", lastPerformed = "Yesterday"),
                ExerciseDisplayInfo(id = "deadlift", name = "Deadlift")
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onNavigateToAddExercise = {},
            onNavigateBack = {},
            onDeleteExercise = {},
            onRecordExercise = { _, _ -> },
            prefillFor = { SetPrefill() },
            isTimerRunning = true,
            timerRemainingSeconds = 30,
            timerTotalSeconds = 60,
            onCloseTimer = {},
            unitSystem = UnitSystem.METRIC,
            onOpenExercise = {}
        )
    }
}
