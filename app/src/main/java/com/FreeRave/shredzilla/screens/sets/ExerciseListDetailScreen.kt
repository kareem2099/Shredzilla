package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.models.setsRangeLabel
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheet
import com.FreeRave.shredzilla.screens.exercises.composables.SwipableExerciseListItem
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

private enum class ListDialog { NONE, DELETE_LIST }

/** A workout list: log sets straight from it, open an exercise, edit the list or delete it. */
@Composable
fun ExerciseListDetailScreen(
    listName: String,
    exercises: List<ExerciseDisplayInfo>,
    /** Suggested sets per exercise (from a program), or null for hand-made lists. */
    targetSets: IntRange?,
    setsDoneToday: Map<String, Int>,
    unitSystem: UnitSystem,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit,
    onNavigateBack: () -> Unit,
    onEditList: () -> Unit,
    onDeleteList: () -> Unit,
    onOpenExercise: (exerciseName: String) -> Unit,
    onRecordExercise: (exerciseName: String, reps: Int, weight: Double, notes: String) -> Unit,
    onRemoveExercise: (exerciseName: String) -> Unit
) {
    var exerciseToRecord by remember { mutableStateOf<String?>(null) }
    var exerciseToRemove by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf(ListDialog.NONE) }
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            title = listName,
            eyebrow = "${exercises.size} ${if (exercises.size == 1) "exercise" else "exercises"}",
            onBack = onNavigateBack,
            actions = {
                GlassIconButton(Icons.Filled.Edit, contentDescription = "Edit list", onClick = onEditList)
                GlassIconButton(Icons.Filled.DeleteOutline, contentDescription = "Delete list", onClick = { dialog = ListDialog.DELETE_LIST })
            }
        )
        if (isTimerRunning) {
            RestTimerBar(remainingSeconds = timerRemainingSeconds, totalSeconds = timerTotalSeconds, onClose = onCloseTimer)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (exercises.isEmpty()) {
                item {
                    GlassSettingsGroup {
                        GlassSettingsRow(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            title = "This list is empty",
                            subtitle = "Add exercises to start logging from here",
                            onClick = onEditList
                        )
                    }
                }
            }
            items(exercises, key = { it.id }) { exercise ->
                SwipableExerciseListItem(
                    exerciseName = exercise.name,
                    lastPerformed = exercise.lastPerformed,
                    onDelete = { exerciseToRemove = exercise.name },
                    onRecord = { exerciseToRecord = exercise.name },
                    onClick = { onOpenExercise(exercise.name) },
                    progress = targetSets?.let { target ->
                        "${setsDoneToday[exercise.id] ?: 0} / ${setsRangeLabel(target.first, target.last)}"
                    },
                    progressDone = targetSets != null && (setsDoneToday[exercise.id] ?: 0) >= targetSets.first,
                    modifier = Modifier.fillMaxWidth().animateItem()
                )
            }
        }
    }

    RecordSetSheet(
        exerciseName = exerciseToRecord,
        unitSystem = unitSystem,
        onRecordSet = onRecordExercise,
        onDismiss = { exerciseToRecord = null }
    )

    exerciseToRemove?.let { name ->
        GlassDialog(
            title = "Remove $name?",
            message = "It's only taken out of \"$listName\". It stays in your library with its history.",
            confirmText = "Remove",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.RemoveCircleOutline),
            onConfirm = {
                onRemoveExercise(name)
                exerciseToRemove = null
            },
            onDismiss = { exerciseToRemove = null }
        )
    }

    if (dialog == ListDialog.DELETE_LIST) {
        GlassDialog(
            title = "Delete \"$listName\"?",
            message = "The list is removed. Your exercises and logged sets aren't touched.",
            confirmText = "Delete list",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                dialog = ListDialog.NONE
                onDeleteList()
            },
            onDismiss = { dialog = ListDialog.NONE }
        )
    }
}

@Preview
@Composable
private fun ExerciseListDetailScreenPreview() {
    ShredzillaTheme {
        ExerciseListDetailScreen(
            listName = "Push day",
            exercises = listOf(
                ExerciseDisplayInfo(id = "bench_press", name = "Bench Press", lastPerformed = "Today"),
                ExerciseDisplayInfo(id = "tricep_pushdown", name = "Tricep Pushdown")
            ),
            targetSets = 3..4,
            setsDoneToday = mapOf("bench_press" to 2),
            unitSystem = UnitSystem.METRIC,
            isTimerRunning = false,
            timerRemainingSeconds = 0,
            timerTotalSeconds = 60,
            onCloseTimer = {},
            onNavigateBack = {},
            onEditList = {},
            onDeleteList = {},
            onOpenExercise = {},
            onRecordExercise = { _, _, _, _ -> },
            onRemoveExercise = {}
        )
    }
}
