package com.FreeRave.shredzilla.screens.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.WorkoutExerciseSummary
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.models.WorkoutPr
import com.FreeRave.shredzilla.models.setLoad
import com.FreeRave.shredzilla.screens.history.WorkoutLogContent
import com.FreeRave.shredzilla.models.setsRangeLabel
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheet
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.SetPrefill
import com.FreeRave.shredzilla.utils.formatEntry
import com.FreeRave.shredzilla.utils.formatDuration
import com.FreeRave.shredzilla.utils.formatLoad
import com.FreeRave.shredzilla.utils.personalRecordIds
import kotlinx.coroutines.delay

/** The session as it's saved and shown in history; PRs use the trophy rule from the exercise page. */
private fun buildWorkoutLog(
    listName: String,
    startedAt: Long,
    finishedAt: Long,
    exercises: List<ExerciseDisplayInfo>,
    sessionSets: Map<String, List<LoggedSet>>,
    recordIds: Set<String>
): WorkoutLog {
    val allSets = sessionSets.values.flatten()
    return WorkoutLog(
        listName = listName,
        startedAtMillis = startedAt,
        durationMillis = finishedAt - startedAt,
        setCount = allSets.size,
        volumeKg = allSets.sumOf { it.reps * it.weightKg }, // plate sets are stored with 0 kg
        exercises = exercises.map { exercise ->
            val sets = sessionSets[exercise.id].orEmpty()
            val best = sets.maxWithOrNull(compareBy<LoggedSet> { it.plates?.toDouble() ?: it.weightKg }.thenBy { it.reps })
            WorkoutExerciseSummary(exercise.id, exercise.name, sets.size, best?.setLoad)
        },
        records = exercises.flatMap { exercise ->
            sessionSets[exercise.id].orEmpty().filter { it.docId in recordIds }.map { WorkoutPr(exercise.name, it.setLoad) }
        }
    )
}

/**
 * Guided session through one list: the current exercise with this session's sets, a progression
 * hint, one big button that logs a set (starting the rest timer) and moves on once the target is
 * met, then a summary. The session is every set logged since it started.
 *
 * Two neighbouring exercises can be linked as a superset: a set of the first jumps straight to the
 * second without rest, and a set of the second starts the rest and goes back to the first.
 */
@Composable
fun WorkoutScreen(
    listName: String,
    exercises: List<ExerciseDisplayInfo>,
    /** Suggested sets per exercise (from a program), or null for hand-made lists. */
    targetSets: IntRange?,
    history: Map<String, List<LoggedSet>>,
    unitSystem: UnitSystem,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit,
    prefillFor: (exerciseName: String) -> SetPrefill,
    onRecordSet: (exerciseName: String, entry: SetEntry, startRest: Boolean) -> Unit,
    onSaveWorkout: (WorkoutLog) -> Unit,
    onFinish: () -> Unit
) {
    // History timestamps are whole seconds, so start on a whole second too
    val startedAt = rememberSaveable { System.currentTimeMillis() / 1000 * 1000 }
    var finishedAt by rememberSaveable { mutableLongStateOf(0L) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isSheetOpen by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    // Index of each superset's first exercise; it's paired with the one right after it
    var supersetStarts by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    fun partnerOf(index: Int): Int? = when {
        index in supersetStarts -> index + 1
        index - 1 in supersetStarts -> index - 1
        else -> null
    }

    val sessionSets = remember(history, exercises) {
        exercises.associate { exercise ->
            exercise.id to history[exercise.id].orEmpty().filter { it.timestampMillis >= startedAt }.sortedBy { it.timestampMillis }
        }
    }
    val recordIds = remember(history, exercises) { exercises.flatMap { personalRecordIds(history[it.id].orEmpty()) }.toSet() }
    val totalSessionSets = sessionSets.values.sumOf { it.size }

    fun requestFinish() {
        when {
            finishedAt > 0 || totalSessionSets == 0 -> onFinish() // nothing logged: just leave
            else -> confirmFinish = true
        }
    }
    BackHandler(onBack = ::requestFinish)

    val exercise = exercises.getOrNull(currentIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)))
    if (finishedAt > 0 || exercise == null) {
        val log = buildWorkoutLog(listName, startedAt, finishedAt.takeIf { it > 0 } ?: startedAt, exercises, sessionSets, recordIds)
        WorkoutSummary(log, unitSystem, onDone = onFinish)
        return
    }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val doneSets = sessionSets[exercise.id].orEmpty()
    val partner = partnerOf(currentIndex)?.let(exercises::getOrNull)
    // A superset moves on as one block
    val blockEnd = maxOf(currentIndex, partnerOf(currentIndex) ?: currentIndex)
    val isLast = blockEnd >= exercises.lastIndex
    val nextExercise = exercises.getOrNull(blockEnd + 1)
    val targetMet = targetSets != null && doneSets.size >= targetSets.first &&
        (partner == null || sessionSets[partner.id].orEmpty().size >= targetSets.first)
    val canLinkNext = partner == null && currentIndex < exercises.lastIndex && partnerOf(currentIndex + 1) == null
    val prefill = prefillFor(exercise.name)

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            title = listName,
            eyebrow = "Workout · ${formatDuration(now - startedAt)}",
            onBack = ::requestFinish,
            actions = { GlassFilterChip(text = "Finish", selected = true, onClick = ::requestFinish) }
        )
        if (isTimerRunning) {
            RestTimerBar(remainingSeconds = timerRemainingSeconds, totalSeconds = timerTotalSeconds, onClose = onCloseTimer)
        }
        ExerciseStrip(
            exercises = exercises,
            currentIndex = currentIndex,
            setsDone = { sessionSets[it].orEmpty().size },
            isInSuperset = { partnerOf(it) != null },
            onSelect = { currentIndex = it }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            CurrentExerciseCard(
                position = "Exercise ${currentIndex + 1} of ${exercises.size}",
                name = exercise.name,
                targetSets = targetSets,
                doneSets = doneSets,
                recordIds = recordIds,
                suggestion = prefill.suggestion?.let { "Try ${formatEntry(it.next, unitSystem)} · last session ${formatEntry(it.basedOn, unitSystem)}" },
                unitSystem = unitSystem,
                superset = {
                    when {
                        partner != null -> GlassFilterChip(
                            text = "Superset with ${partner.name} ✕",
                            selected = true,
                            onClick = { supersetStarts = supersetStarts - minOf(currentIndex, partnerOf(currentIndex)!!) }
                        )
                        canLinkNext -> GlassFilterChip(
                            text = "Superset with ${exercises[currentIndex + 1].name}",
                            selected = false,
                            onClick = { supersetStarts = supersetStarts + currentIndex }
                        )
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Once the target is met the main button moves on; logging more stays one tap away
            when {
                targetMet && isLast -> GlassPrimaryButton(text = "Finish workout", onClick = ::requestFinish)
                targetMet -> GlassPrimaryButton(text = "Next: ${nextExercise?.name}", onClick = { currentIndex = blockEnd + 1 })
                else -> GlassPrimaryButton(text = "Log set ${doneSets.size + 1}", onClick = { isSheetOpen = true })
            }
            val secondary: Pair<String, () -> Unit>? = when {
                targetMet -> "Log another set" to { isSheetOpen = true }
                !isLast -> "Skip to ${nextExercise?.name}" to { currentIndex = blockEnd + 1 }
                totalSessionSets > 0 -> "Finish workout" to ::requestFinish
                else -> null
            }
            secondary?.let { (text, action) ->
                TextButton(onClick = action) { Text(text, color = MaterialTheme.glass.contentMuted) }
            }
        }
    }

    RecordSetSheet(
        exerciseName = exercise.name.takeIf { isSheetOpen },
        unitSystem = unitSystem,
        initial = prefill.initial,
        suggestion = prefill.suggestion,
        onRecordSet = { name, entry ->
            val partnerIndex = partnerOf(currentIndex)
            // First half of a superset: straight to the partner, the rest comes after it
            val isFirstHalf = partnerIndex != null && partnerIndex > currentIndex
            onRecordSet(name, entry, !isFirstHalf)
            if (partnerIndex != null) currentIndex = partnerIndex
        },
        onDismiss = { isSheetOpen = false }
    )

    if (confirmFinish) {
        GlassDialog(
            title = "Finish workout?",
            message = "$totalSessionSets ${if (totalSessionSets == 1) "set" else "sets"} logged in ${formatDuration(now - startedAt)}.",
            confirmText = "Finish",
            dismissText = "Keep going",
            icon = rememberVectorPainter(Icons.Filled.EmojiEvents),
            onConfirm = {
                confirmFinish = false
                finishedAt = System.currentTimeMillis()
                onSaveWorkout(buildWorkoutLog(listName, startedAt, finishedAt, exercises, sessionSets, recordIds))
            },
            onDismiss = { confirmFinish = false }
        )
    }
}

@Composable
private fun ExerciseStrip(
    exercises: List<ExerciseDisplayInfo>,
    currentIndex: Int,
    setsDone: (exerciseId: String) -> Int,
    isInSuperset: (index: Int) -> Boolean,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) { listState.animateScrollToItem(currentIndex) }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(exercises, key = { _, it -> it.id }) { index, exercise ->
            val done = setsDone(exercise.id)
            val name = if (isInSuperset(index)) "⇄ ${exercise.name}" else exercise.name
            GlassFilterChip(
                text = if (done > 0) "$name · $done" else name,
                selected = index == currentIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun CurrentExerciseCard(
    position: String,
    name: String,
    targetSets: IntRange?,
    doneSets: List<LoggedSet>,
    recordIds: Set<String>,
    suggestion: String?,
    unitSystem: UnitSystem,
    superset: @Composable () -> Unit
) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text(position, style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
            Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = palette.content)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (targetSets != null) {
                GlassChip(text = "Target ${setsRangeLabel(targetSets.first, targetSets.last)}", highlighted = doneSets.size >= targetSets.first)
            }
            GlassChip(text = "${doneSets.size} done")
        }
        superset()
        if (suggestion != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = palette.content)
            }
        }
        if (doneSets.isEmpty()) {
            Text("No sets yet. Log your first one when you're ready.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                doneSets.forEachIndexed { index, set ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Set ${index + 1}", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted, modifier = Modifier.weight(0.3f))
                        Text(
                            formatLoad(set.setLoad, unitSystem),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.content,
                            modifier = Modifier.weight(0.7f)
                        )
                        if (set.docId in recordIds) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Personal record", tint = palette.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSummary(log: WorkoutLog, unitSystem: UnitSystem, onDone: () -> Unit) {
    val palette = MaterialTheme.glass
    BackHandler(onBack = onDone)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.EmojiEvents), size = 72.dp)
        Text("Nice work!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = palette.content)
        WorkoutLogContent(log = log, unitSystem = unitSystem, onDone = onDone)
    }
}
