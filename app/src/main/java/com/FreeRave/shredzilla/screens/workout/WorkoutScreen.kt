package com.FreeRave.shredzilla.screens.workout

import com.FreeRave.shredzilla.pro.LocalProAccess
import com.FreeRave.shredzilla.pro.ProLockedCard
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.utils.changeSinceLast
import com.FreeRave.shredzilla.utils.sidePrefix
import java.text.SimpleDateFormat
import java.util.Locale
import com.FreeRave.shredzilla.models.workingSetCount
import com.FreeRave.shredzilla.models.completedSetCount
import com.FreeRave.shredzilla.models.setNumbers
import com.FreeRave.shredzilla.workout.WorkoutSession
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
internal fun buildWorkoutLog(
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
        setCount = sessionSets.values.sumOf { it.workingSetCount() },
        volumeKg = allSets.sumOf { it.volumeKg },
        exercises = exercises.map { exercise ->
            val sets = sessionSets[exercise.id].orEmpty()
            val best = sets.maxWithOrNull(compareBy<LoggedSet> { it.plates?.toDouble() ?: it.weightKg }.thenBy { it.reps })
            WorkoutExerciseSummary(exercise.id, exercise.name, sets.workingSetCount(), best?.setLoad)
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
    /** The running workout; it outlives this screen so the notification can keep logging. */
    session: WorkoutSession,
    history: Map<String, List<LoggedSet>>,
    unitSystem: UnitSystem,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit,
    prefillFor: (exerciseName: String) -> SetPrefill,
    /** Logs a set for the current exercise; the session handles supersets and the rest. */
    onRecordSet: (entry: SetEntry) -> Unit,
    /** Saves the workout; the screen then shows the summary. */
    onFinishWorkout: () -> Unit,
    onLeave: () -> Unit
) {
    val listName = session.listName
    val exercises = session.exercises
    val targetSets = session.targetSets
    val startedAt = session.startedAt
    val finishedAt = session.finishedAt
    var isSheetOpen by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    fun partnerOf(index: Int): Int? = session.partnerOf(index)

    val sessionSets = remember(history, exercises) { session.sessionSets(history) }
    val recordIds = remember(history, exercises) { exercises.flatMap { personalRecordIds(history[it.id].orEmpty()) }.toSet() }
    val totalSessionSets = sessionSets.values.sumOf { it.workingSetCount() }

    fun requestFinish() {
        when {
            finishedAt > 0 || totalSessionSets == 0 -> onLeave() // nothing logged: just leave
            else -> confirmFinish = true
        }
    }
    BackHandler(onBack = ::requestFinish)

    val exercise = exercises.getOrNull(session.currentIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)))
    if (finishedAt > 0 || exercise == null) {
        val log = buildWorkoutLog(listName, startedAt, finishedAt.takeIf { it > 0 } ?: startedAt, exercises, sessionSets, recordIds)
        WorkoutSummary(log, unitSystem, onDone = onLeave)
        return
    }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val doneSets = sessionSets[exercise.id].orEmpty()
    // The latest earlier session of this exercise, warm-ups left out, to beat set by set
    val lastSession = remember(history, exercise.id) {
        history[exercise.id].orEmpty()
            .filter { it.timestampMillis < startedAt && it.setType != SetType.WARMUP }
            .groupBy { it.dateKey }
            .maxByOrNull { it.key }?.value
            ?.sortedBy { it.timestampMillis }
            .orEmpty()
    }
    val partner = partnerOf(session.currentIndex)?.let(exercises::getOrNull)
    // A superset moves on as one block
    val blockEnd = maxOf(session.currentIndex, partnerOf(session.currentIndex) ?: session.currentIndex)
    val isLast = blockEnd >= exercises.lastIndex
    val nextExercise = exercises.getOrNull(blockEnd + 1)
    // Toward the target a left/right set only counts once both sides are in, so the last right side isn't skipped
    val doneCount = doneSets.completedSetCount()
    val targetMet = targetSets != null && doneCount >= targetSets.first &&
        (partner == null || sessionSets[partner.id].orEmpty().completedSetCount() >= targetSets.first)
    val canLinkNext = partner == null && session.currentIndex < exercises.lastIndex && partnerOf(session.currentIndex + 1) == null
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
            currentIndex = session.currentIndex,
            setsDone = { sessionSets[it].orEmpty().completedSetCount() },
            isInSuperset = { partnerOf(it) != null },
            onSelect = { session.currentIndex = it }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            CurrentExerciseCard(
                position = "Exercise ${session.currentIndex + 1} of ${exercises.size}",
                name = exercise.name,
                targetSets = targetSets,
                doneSets = doneSets,
                recordIds = recordIds,
                suggestion = prefill.suggestion?.let { "Try ${formatEntry(it.next, unitSystem)} · last session ${formatEntry(it.basedOn, unitSystem)}" },
                unitSystem = unitSystem,
                lastSession = lastSession,
                superset = {
                    when {
                        partner != null -> GlassFilterChip(
                            text = "Superset with ${partner.name} ✕",
                            selected = true,
                            onClick = { session.supersetStarts = session.supersetStarts - minOf(session.currentIndex, partnerOf(session.currentIndex)!!) }
                        )
                        canLinkNext -> GlassFilterChip(
                            text = "Superset with ${exercises[session.currentIndex + 1].name}",
                            selected = false,
                            onClick = { session.supersetStarts = session.supersetStarts + session.currentIndex }
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
                targetMet -> GlassPrimaryButton(text = "Next: ${nextExercise?.name}", onClick = { session.currentIndex = blockEnd + 1 })
                else -> GlassPrimaryButton(
                    // Mid-pair on a one-sided exercise, the next tap is the other side of the same set
                    text = prefill.initial.side?.let { "Log ${it.label.lowercase()} side" } ?: "Log set ${doneCount + 1}",
                    onClick = { isSheetOpen = true }
                )
            }
            val secondary: Pair<String, () -> Unit>? = when {
                targetMet -> "Log another set" to { isSheetOpen = true }
                !isLast -> "Skip to ${nextExercise?.name}" to { session.currentIndex = blockEnd + 1 }
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
        onRecordSet = { _, entry -> onRecordSet(entry) },
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
                onFinishWorkout()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentExerciseCard(
    position: String,
    name: String,
    targetSets: IntRange?,
    doneSets: List<LoggedSet>,
    recordIds: Set<String>,
    suggestion: String?,
    unitSystem: UnitSystem,
    lastSession: List<LoggedSet>,
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
                GlassChip(text = "Target ${setsRangeLabel(targetSets.first, targetSets.last)}", highlighted = doneSets.completedSetCount() >= targetSets.first)
            }
            GlassChip(text = "${doneSets.completedSetCount()} done")
        }
        superset()
        val isPro = LocalProAccess.current.isPro
        if (!isPro && (suggestion != null || lastSession.isNotEmpty())) {
            ProLockedCard(title = "Coach & last time", description = "Your suggested next set, last session's sets and \"+2 reps\" on every set")
        }
        if (isPro && suggestion != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = palette.content)
            }
        }
        if (isPro && lastSession.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Last time · ${lastSessionLabel(lastSession.first().dateKey)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.contentMuted
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lastSession.forEach { set -> GlassChip(text = sidePrefix(set.side) + formatLoad(set.setLoad, unitSystem)) }
                }
            }
        }
        if (doneSets.isEmpty()) {
            Text("No sets yet. Log your first one when you're ready.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val numbers = setNumbers(doneSets, { it.side }, { it.setType == SetType.WARMUP })
                doneSets.forEachIndexed { index, set ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // A left/right pair shares its number (Set 1 · Left, Set 1 · Right); warm-ups aren't numbered.
                        // The comparison is with the same side's matching set from last time.
                        val isWarmup = set.setType == SetType.WARMUP
                        val sideIndex = doneSets.take(index + 1).count { it.side == set.side && it.setType != SetType.WARMUP }
                        val previous = if (isWarmup) null else lastSession.filter { it.side == set.side }.getOrNull(sideIndex - 1)
                        Text(
                            (if (isWarmup) "Warm-up" else "Set ${numbers[index]}") + set.side?.let { " · ${it.label}" }.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.contentMuted,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            formatLoad(set.setLoad, unitSystem),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.content,
                            modifier = Modifier.weight(0.6f)
                        )
                        changeSinceLast(set, previous, unitSystem)?.takeIf { isPro }?.let { change ->
                            Text(
                                change,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (change.startsWith("+")) palette.accent else palette.contentMuted,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        if (set.docId in recordIds) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Personal record", tint = palette.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun lastSessionLabel(dateKey: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) ?: return dateKey
    return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)
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
