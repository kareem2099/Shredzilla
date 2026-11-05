package com.FreeRave.shredzilla.models

/** A load as stored for sets: kilograms, or a plate count on a weight stack ([weightKg] is 0 then). */
data class SetLoad(val weightKg: Double, val plates: Int?, val reps: Int)

val LoggedSet.setLoad: SetLoad get() = SetLoad(weightKg, plates, reps)

/** One exercise's part of a finished workout. */
data class WorkoutExerciseSummary(val exerciseId: String, val name: String, val sets: Int, val best: SetLoad?)

/** A personal record set during a workout. */
data class WorkoutPr(val exerciseName: String, val load: SetLoad)

/** A finished workout as saved under users/{uid}/workouts. */
data class WorkoutLog(
    val id: String = "",
    val listName: String,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val setCount: Int,
    val volumeKg: Double,
    val exercises: List<WorkoutExerciseSummary>,
    val records: List<WorkoutPr>
)

/** Best set of one kind: [load] is kg for weighted sets, a plate count for stack sets. */
data class RecordSet(val load: Double, val reps: Int, val atMillis: Long)

/** All-time bests for one exercise, kept under users/{uid}/records/{exerciseId}. */
data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val heaviest: RecordSet?,
    val mostPlates: RecordSet?
) {
    val latestMillis: Long get() = maxOf(heaviest?.atMillis ?: 0L, mostPlates?.atMillis ?: 0L)
}
