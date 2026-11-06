package com.FreeRave.shredzilla.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Hard training weeks in a row after which a deload week is suggested. */
const val DELOAD_EVERY_WEEKS = 6

/** How far back the streak can see: the loaded history covers about 90 days. */
private const val MAX_WEEKS_BACK = 12

/** Midnight of the first day of the week containing [millis], by the device's locale (Saturday in Egypt). */
fun weekStartOf(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(Date(millis))
    while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) add(Calendar.DAY_OF_MONTH, -1)
}.timeInMillis

/** Week id stored for deload weeks: the date key of its first day. */
fun weekKeyOf(millis: Long): String = dateKeyOf(weekStartOf(millis))

data class WeeklyProgress(
    val thisWeekKey: String,
    val workoutsThisWeek: Int,
    val goal: Int?,
    /** Weeks in a row that met the goal; deload weeks pause it without breaking it. */
    val streakWeeks: Int,
    /** Full training weeks in a row before this one, since the last deload or week off. */
    val weeksSinceDeload: Int,
    val isDeloadWeek: Boolean
) {
    val deloadDue: Boolean get() = !isDeloadWeek && weeksSinceDeload >= DELOAD_EVERY_WEEKS
}

/**
 * [trainingDayKeys] are the day keys ("yyyy-MM-dd") with at least one working set. Without a
 * [goal], any week with a workout counts towards the streak.
 */
fun weeklyProgress(trainingDayKeys: Set<String>, goal: Int?, deloadWeeks: Set<String>, now: Long = System.currentTimeMillis()): WeeklyProgress {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val workoutsPerWeek = trainingDayKeys
        .mapNotNull { key -> parser.parse(key)?.time?.let(::weekKeyOf) }
        .groupingBy { it }.eachCount()
    val weekKeys = (0..MAX_WEEKS_BACK).map { weeksAgo ->
        weekKeyOf(Calendar.getInstance().apply { timeInMillis = now; add(Calendar.WEEK_OF_YEAR, -weeksAgo) }.timeInMillis)
    }
    val thisWeek = weekKeys.first()
    val needed = goal ?: 1

    // The current week is still in progress: it adds to the streak once met, but never breaks it
    var streak = if ((workoutsPerWeek[thisWeek] ?: 0) >= needed) 1 else 0
    for (week in weekKeys.drop(1)) {
        when {
            week in deloadWeeks -> continue
            (workoutsPerWeek[week] ?: 0) >= needed -> streak++
            else -> break
        }
    }
    // A deload or a week without training both reset the count
    val weeksSinceDeload = weekKeys.drop(1)
        .takeWhile { week -> week !in deloadWeeks && (workoutsPerWeek[week] ?: 0) > 0 }
        .size

    return WeeklyProgress(
        thisWeekKey = thisWeek,
        workoutsThisWeek = workoutsPerWeek[thisWeek] ?: 0,
        goal = goal,
        streakWeeks = streak,
        weeksSinceDeload = weeksSinceDeload,
        isDeloadWeek = thisWeek in deloadWeeks
    )
}
