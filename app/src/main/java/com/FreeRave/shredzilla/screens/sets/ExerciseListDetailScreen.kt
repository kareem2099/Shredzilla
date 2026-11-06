package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.Alignment
import com.FreeRave.shredzilla.ui.glass.GlassFab
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.padding
import com.FreeRave.shredzilla.ui.theme.glass
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.utils.SetPrefill
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

/**
 * A workout list: log sets straight from it, open an exercise, drag exercises into the order
 * they're done in, edit the list or delete it.
 */
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
    onRecordExercise: (exerciseName: String, entry: SetEntry) -> Unit,
    /** Where a new set starts (the last one logged) and what to try next. */
    prefillFor: (exerciseName: String) -> SetPrefill,
    onRemoveExercise: (exerciseName: String) -> Unit,
    /** The whole new order (exercise ids), once a drag ends. */
    onReorderExercises: (exerciseIds: List<String>) -> Unit,
    onStartWorkout: () -> Unit
) {
    // Moved live while dragging, saved on release; a fresh list from Firestore replaces it
    var order by remember(exercises) { mutableStateOf(exercises) }
    val lazyListState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = order.indexOfFirst { it.id == from.key }
        val toIndex = order.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            order = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    var exerciseToRecord by remember { mutableStateOf<String?>(null) }
    var exerciseToRemove by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf(ListDialog.NONE) }
    Box(modifier = Modifier.fillMaxSize()) {
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
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
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
                items(order, key = { it.id }) { exercise ->
                    ReorderableItem(reorderState, key = exercise.id) { isDragging ->
                        val lift by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "dragLift")
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { scaleX = lift; scaleY = lift },
                            dragHandle = if (order.size > 1) {
                                {
                                    Icon(
                                        Icons.Filled.DragIndicator,
                                        contentDescription = "Reorder ${exercise.name}",
                                        tint = MaterialTheme.glass.contentMuted,
                                        modifier = Modifier
                                            .draggableHandle(
                                                onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                                onDragStopped = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                                    if (order.map { it.id } != exercises.map { it.id }) onReorderExercises(order.map { it.id })
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 10.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
        if (exercises.isNotEmpty()) {
            GlassFab(
                icon = Icons.Filled.PlayArrow,
                text = "Start workout",
                onClick = onStartWorkout,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            )
        }
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
            onRecordExercise = { _, _ -> },
            prefillFor = { SetPrefill() },
            onRemoveExercise = {},
            onReorderExercises = {},
            onStartWorkout = {}
        )
    }
}
