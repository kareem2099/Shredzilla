package com.FreeRave.shredzilla.utils

import java.util.concurrent.TimeUnit

/** A pause this long between sets starts a new session (morning and evening training stay apart). */
private val SESSION_GAP = TimeUnit.MINUTES.toMillis(45)
/** Time the last set of a session (or a lone set) still took. */
private val LAST_SET_ALLOWANCE = TimeUnit.MINUTES.toMillis(2)

data class TrainingSession(val startMillis: Long, val endMillis: Long) {
    val durationMillis: Long get() = endMillis - startMillis
}

/**
 * Sessions of one day. Workouts started with "Start workout" count with their measured duration;
 * sets logged outside a workout are grouped into sessions by the gaps between them. Overlaps merge,
 * so a set logged during a workout is never counted twice.
 */
fun trainingSessions(setTimes: List<Long>, workouts: List<TrainingSession>): List<TrainingSession> {
    val loose = setTimes.sorted().filter { t -> workouts.none { t in it.startMillis..it.endMillis } }
    val fromSets = mutableListOf<TrainingSession>()
    var start: Long? = null
    var last = 0L
    loose.forEach { t ->
        if (start == null) start = t
        else if (t - last > SESSION_GAP) {
            fromSets += TrainingSession(start!!, last + LAST_SET_ALLOWANCE)
            start = t
        }
        last = t
    }
    start?.let { fromSets += TrainingSession(it, last + LAST_SET_ALLOWANCE) }

    val merged = mutableListOf<TrainingSession>()
    (workouts + fromSets).sortedBy { it.startMillis }.forEach { s ->
        val prev = merged.lastOrNull()
        if (prev != null && s.startMillis <= prev.endMillis) merged[merged.lastIndex] = TrainingSession(prev.startMillis, maxOf(prev.endMillis, s.endMillis))
        else merged += s
    }
    return merged
}

fun trainingMillis(setTimes: List<Long>, workouts: List<TrainingSession>): Long =
    trainingSessions(setTimes, workouts).sumOf { it.durationMillis }

/** "1h 12m", "45m", or "0m". */
fun formatTrainingTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
