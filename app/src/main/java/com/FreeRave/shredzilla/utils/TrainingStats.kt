package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.models.exerciseCatalog
import com.FreeRave.shredzilla.models.exerciseIdOf
import com.FreeRave.shredzilla.models.workingSetCount
import java.util.Calendar

/** Weekly hard sets per muscle that most hypertrophy guidelines land on. */
val RECOMMENDED_WEEKLY_SETS = 10..20

private const val OTHER_MUSCLE = "Other"

private val muscleByExerciseId: Map<String, String> =
    exerciseCatalog.flatMap { group -> group.exercises.map { exerciseIdOf(it) to group.muscle } }.toMap()

fun muscleOf(exerciseId: String): String = muscleByExerciseId[exerciseId] ?: OTHER_MUSCLE

data class MuscleWeek(val muscle: String, val thisWeek: Int, val lastWeek: Int)

data class WeekTotals(val weekStartMillis: Long, val sets: Int, val volumeKg: Double, val trainingDays: Int, val trainingMillis: Long)

data class StrengthPoint(val dayMillis: Long, val estimatedMax: Double)

data class StrengthTrend(val exerciseId: String, val isPlates: Boolean, val points: List<StrengthPoint>) {
    val first get() = points.first().estimatedMax
    val latest get() = points.last().estimatedMax
    val best get() = points.maxOf { it.estimatedMax }
}

/** Epley estimate of a one-rep max: load × (1 + reps / 30). Works for kg and for plate counts alike. */
fun estimatedOneRepMax(load: Double, reps: Int): Double = if (reps <= 1) load else load * (1 + reps / 30.0)

/** Working sets counted the app's way (warm-ups out, a left + right pair is one set), per exercise per day. */
private fun Map<String, List<LoggedSet>>.workingSetsIn(fromMillis: Long, toMillis: Long): Map<String, Int> =
    mapValues { (_, sets) ->
        sets.filter { it.timestampMillis in fromMillis until toMillis }
            .groupBy { it.dateKey }
            .values.sumOf { it.workingSetCount() }
    }

/** Sets per muscle this week and last week; muscles trained in neither week are left out. */
fun muscleWeeks(history: Map<String, List<LoggedSet>>, now: Long = System.currentTimeMillis()): List<MuscleWeek> {
    val thisStart = weekStartOf(now)
    val lastStart = weekStartOf(thisStart - 1)
    fun byMuscle(counts: Map<String, Int>) = counts.entries.groupBy({ muscleOf(it.key) }, { it.value }).mapValues { it.value.sum() }
    val thisWeek = byMuscle(history.workingSetsIn(thisStart, Long.MAX_VALUE))
    val lastWeek = byMuscle(history.workingSetsIn(lastStart, thisStart))
    val order = exerciseCatalog.map { it.muscle } + OTHER_MUSCLE
    return order.mapNotNull { muscle ->
        val a = thisWeek[muscle] ?: 0
        val b = lastWeek[muscle] ?: 0
        if (a == 0 && b == 0) null else MuscleWeek(muscle, a, b)
    }
}

/** The last [weeks] weeks, oldest first, including the current one. */
fun weeklyTotals(history: Map<String, List<LoggedSet>>, workouts: List<WorkoutLog> = emptyList(), weeks: Int = 8, now: Long = System.currentTimeMillis()): List<WeekTotals> {
    val starts = generateSequence(weekStartOf(now)) { weekStartOf(it - 1) }.take(weeks).toList().reversed()
    val all = history.values.flatten()
    return starts.mapIndexed { i, start ->
        val end = starts.getOrNull(i + 1) ?: Long.MAX_VALUE
        val inWeek = all.filter { it.timestampMillis in start until end }
        WeekTotals(
            weekStartMillis = start,
            sets = inWeek.groupBy { it.exerciseId to it.dateKey }.values.sumOf { it.workingSetCount() },
            volumeKg = inWeek.sumOf { it.volumeKg },
            trainingDays = inWeek.filter { it.setType != SetType.WARMUP }.map { it.dateKey }.distinct().size,
            trainingMillis = trainingMillisByDay(inWeek, workouts.filter { it.startedAtMillis in start until end }).values.sum(),
        )
    }
}

/**
 * Best estimated 1RM per training day, for exercises with a load on at least two days.
 * Plates and kilograms aren't comparable, so each exercise uses whichever it was logged in most.
 */
fun strengthTrends(history: Map<String, List<LoggedSet>>): List<StrengthTrend> =
    history.mapNotNull { (exerciseId, sets) ->
        val working = sets.filter { it.setType != SetType.WARMUP && !it.bodyweight && it.reps > 0 && (it.plates != null || it.weightKg > 0) }
        if (working.isEmpty()) return@mapNotNull null
        val isPlates = working.count { it.plates != null } > working.size / 2
        val points = working.filter { (it.plates != null) == isPlates }
            .groupBy { it.dateKey }
            .map { (_, day) ->
                StrengthPoint(
                    dayMillis = startOfDayMillis(day.first().timestampMillis),
                    estimatedMax = day.maxOf { estimatedOneRepMax(it.plates?.toDouble() ?: it.weightKg, it.reps) }
                )
            }
            .sortedBy { it.dayMillis }
        if (points.size < 2) null else StrengthTrend(exerciseId, isPlates, points)
    }.sortedByDescending { it.points.size }

private fun startOfDayMillis(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Training time per date key: workouts with their measured length, other sets grouped into sessions. */
fun trainingMillisByDay(sets: List<LoggedSet>, workouts: List<WorkoutLog>): Map<String, Long> {
    val setsByDay = sets.groupBy { it.dateKey }
    val workoutsByDay = workouts.groupBy { dateKeyOf(it.startedAtMillis) }
    return (setsByDay.keys + workoutsByDay.keys).associateWith { day ->
        trainingMillis(
            setTimes = setsByDay[day].orEmpty().map { it.timestampMillis },
            workouts = workoutsByDay[day].orEmpty().map { TrainingSession(it.startedAtMillis, it.startedAtMillis + it.durationMillis) }
        )
    }
}
