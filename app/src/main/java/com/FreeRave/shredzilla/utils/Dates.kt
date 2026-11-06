package com.FreeRave.shredzilla.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Midnight (local time) of the day containing [date]. */
fun startOfDay(date: Date = Date()): Long = Calendar.getInstance().apply {
    time = date
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Midnight of every day in the month that contains [anyDayInMonth]. */
fun daysOfMonth(anyDayInMonth: Long): List<Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = anyDayInMonth }
    return (1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        startOfDay(cal.time)
    }
}

/** Firestore day key used under users/{uid}/dailyActivity, e.g. "2026-11-04". Always US digits. */
fun dateKeyOf(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

/** Day keys from [fromMillis] to [toMillis] inclusive, newest first. */
fun dateKeysBetween(fromMillis: Long, toMillis: Long): List<String> {
    val keys = mutableListOf<String>()
    val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(Date(toMillis)) }
    val first = startOfDay(Date(fromMillis))
    while (cal.timeInMillis >= first) {
        keys += dateKeyOf(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }
    return keys
}

/** Stopwatch style: 75 s → "1:15", 3 725 s → "1:02:05". */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}
