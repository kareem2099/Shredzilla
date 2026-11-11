package com.FreeRave.shredzilla.workout

import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.models.completedSetCount
import com.FreeRave.shredzilla.models.setLoad
import com.FreeRave.shredzilla.models.workingSetCount
import com.FreeRave.shredzilla.notifications.WorkoutNotificationStyle
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.utils.SetSuggestion
import com.FreeRave.shredzilla.utils.formatEntry
import com.FreeRave.shredzilla.utils.formatLoad
import com.FreeRave.shredzilla.utils.personalRecordIds

/** Everything the workout notification shows, as plain values so an unchanged notice isn't re-posted. */
data class WorkoutNotice(
    val listId: String,
    val listName: String,
    val startedAt: Long,
    val exerciseName: String,
    /** "Exercise 3 of 7". */
    val position: String,
    /** "Set 3 of 3 · Right side". */
    val setLabel: String,
    /** The set the Log button saves, e.g. "9 plates × 8"; null before the exercise's first set ever. */
    val quickSet: String?,
    val restEndsAt: Long,
    /** This session's sets of the exercise, e.g. "W 6 plates × 12 · 8 plates × 14". */
    val doneToday: String?,
    /** Pro: last session's sets and the coach's suggestion. */
    val lastTime: String?,
    val coach: String?,
    /** Free users with data for those lines see one line saying they're Pro. */
    val proLocked: Boolean,
    val upNext: String,
    val totals: String,
    val targetMet: Boolean,
    val isLastBlock: Boolean,
    val nextExerciseName: String?,
    val style: WorkoutNotificationStyle = WorkoutNotificationStyle.DETAILED,
    /** One bar segment per exercise (its target sets) and how many of them are done, for the progress style. */
    val segments: List<Int> = emptyList(),
    val progress: Int = 0,
) {
    val totalSets: Int get() = segments.sum()

    companion object {
        fun from(
            session: WorkoutSession,
            exercise: ExerciseDisplayInfo,
            history: Map<String, List<LoggedSet>>,
            unitSystem: UnitSystem,
            quickEntry: SetEntry?,
            suggestion: SetSuggestion?,
            restEndsAt: Long,
            isPro: Boolean,
            style: WorkoutNotificationStyle = WorkoutNotificationStyle.DETAILED,
        ): WorkoutNotice {
            val sessionSets = session.sessionSets(history)
            val done = sessionSets[exercise.id].orEmpty()
            val target = session.targetSets
            val side = quickEntry?.side
            val setNumber = done.completedSetCount() + 1
            val setLabel = buildString {
                append("Set $setNumber")
                target?.let { append(if (it.first == it.last) " of ${it.first}" else " of ${it.first}–${it.last}") }
                side?.let { append(" · ${it.label} side") }
            }
            val lastSession = history[exercise.id].orEmpty()
                .filter { it.timestampMillis < session.startedAt && it.setType != SetType.WARMUP }
                .groupBy { it.dateKey }
                .maxByOrNull { it.key }?.value?.sortedBy { it.timestampMillis }.orEmpty()
            val partner = session.partnerOf(session.currentIndex)?.let(session.exercises::getOrNull)
            val records = session.exercises.sumOf { ex ->
                val ids = personalRecordIds(history[ex.id].orEmpty())
                sessionSets[ex.id].orEmpty().count { it.docId in ids }
            }
            val totalSets = sessionSets.values.sumOf { it.workingSetCount() }
            // Lists without a target still get a bar: at least three sets an exercise, or what was done
            val segments = session.exercises.map { ex -> target?.first ?: maxOf(sessionSets[ex.id].orEmpty().completedSetCount(), DEFAULT_SEGMENT) }
            val progress = session.exercises.zip(segments).sumOf { (ex, length) -> minOf(sessionSets[ex.id].orEmpty().completedSetCount(), length) }
            val exercisesStarted = sessionSets.values.count { it.isNotEmpty() }

            return WorkoutNotice(
                listId = session.listId,
                listName = session.listName,
                startedAt = session.startedAt,
                exerciseName = exercise.name,
                position = "Exercise ${session.currentIndex + 1} of ${session.exercises.size}",
                setLabel = setLabel,
                quickSet = quickEntry?.let { formatEntry(it, unitSystem) },
                restEndsAt = restEndsAt,
                doneToday = done.takeIf { it.isNotEmpty() }?.takeLast(MAX_SETS_SHOWN)?.joinToString(" · ") { describe(it, unitSystem) },
                lastTime = lastSession.takeIf { isPro && it.isNotEmpty() }?.takeLast(MAX_SETS_SHOWN)?.joinToString(" · ") { describe(it, unitSystem) },
                coach = suggestion?.takeIf { isPro }?.let { "Try ${formatEntry(it.next, unitSystem)}" },
                proLocked = !isPro && (lastSession.isNotEmpty() || suggestion != null),
                upNext = when {
                    partner != null -> "Superset with ${partner.name}"
                    session.nextExercise != null -> "Up next: ${session.nextExercise!!.name}"
                    else -> "Last exercise"
                },
                totals = buildString {
                    append("$totalSets ${if (totalSets == 1) "set" else "sets"}")
                    append(" · $exercisesStarted of ${session.exercises.size} exercises")
                    if (records > 0) append(" · $records ${if (records == 1) "record" else "records"} 🏆")
                },
                targetMet = session.targetMet(sessionSets),
                isLastBlock = session.isLastBlock,
                nextExerciseName = session.nextExercise?.name,
                style = if (style.isPro && !isPro) WorkoutNotificationStyle.DETAILED else style,
                segments = segments,
                progress = progress,
            )
        }

        private const val MAX_SETS_SHOWN = 6
        private const val DEFAULT_SEGMENT = 3

        private fun describe(set: LoggedSet, unitSystem: UnitSystem): String = buildString {
            if (set.setType == SetType.WARMUP) append("W ")
            when (set.side) {
                Side.LEFT -> append("L ")
                Side.RIGHT -> append("R ")
                null -> Unit
            }
            append(formatLoad(set.setLoad, unitSystem))
        }
    }
}

/**
 * Reads what was typed into the notification: "8" changes only the reps of [template], "9 8" (or
 * "9x8", "9×8") sets load and reps. Load means plates on a stack machine, weight per side on a
 * per-side machine, added weight on bodyweight work, and the user's unit otherwise. Arabic digits
 * work too. Null when it can't be read.
 */
fun parseSetInput(text: String, template: SetEntry?): SetEntry? {
    val normalized = text.map { c ->
        when (c) {
            in '٠'..'٩' -> '0' + (c - '٠')
            in '۰'..'۹' -> '0' + (c - '۰')
            '٫', ',' -> '.'
            else -> c
        }
    }.joinToString("")
    val numbers = Regex("""\d+(?:\.\d+)?""").findAll(normalized).map { it.value.toDouble() }.toList()
    val base = template ?: SetEntry()
    return when (numbers.size) {
        1 -> template?.copy(reps = numbers[0].toInt())
        2 -> {
            val (load, reps) = numbers
            when {
                base.plates != null -> base.copy(plates = load.toInt(), reps = reps.toInt())
                base.perSide -> base.copy(weight = load * 2, reps = reps.toInt())
                else -> base.copy(weight = load, reps = reps.toInt())
            }
        }
        else -> null
    }?.takeIf { it.reps >= 1 }?.copy(notes = "")
}
