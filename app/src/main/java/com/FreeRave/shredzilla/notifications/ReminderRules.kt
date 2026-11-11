package com.FreeRave.shredzilla.notifications

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** What the background check knows about the user's training, mirrored from the app (see [ReminderStore]). */
data class TrainingSnapshot(
    /** Date keys ("yyyy-MM-dd") of recent days with at least one working set. */
    val trainingDays: Set<String> = emptySet(),
    val weeklyGoal: Int? = null,
    /** Week keys (first day of the week) marked as deload weeks. */
    val deloadWeeks: Set<String> = emptySet(),
    val lastMeasurementMillis: Long? = null,
    val measurementCadenceDays: Int = 7,
    /** The list to suggest next, e.g. the day after the last workout's day of a split. */
    val nextListId: String? = null,
    val nextListName: String? = null,
)

/** The user's notification choices. [reminderDays] null means the inactivity reminder is off. */
data class NotificationChoices(
    val reminderDays: Int? = null,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val weeklyGoal: Boolean = true,
    val measurements: Boolean = true,
    val proEnding: Boolean = true,
    /** The live notification during a workout, and how it looks. */
    val workoutNotification: Boolean = true,
    val workoutStyle: WorkoutNotificationStyle = WorkoutNotificationStyle.DETAILED,
) {
    companion object {
        const val DEFAULT_REMINDER_HOUR = 18
        const val DEFAULT_REMINDER_DAYS = 3
    }
}

/** When each kind was last sent, so none of them repeats every day. */
data class SentLog(
    val inactivityDay: String? = null,
    val goalWeek: String? = null,
    val measurementDay: String? = null,
)

sealed interface Reminder {
    data class Inactive(val daysSince: Int, val nextListId: String?, val nextListName: String?) : Reminder
    data class GoalAtRisk(val missing: Int, val daysLeft: Int) : Reminder
    data class MeasurementDue(val daysSince: Int) : Reminder
}

/**
 * Decides the day's reminders. Kept free of Android so it can be unit tested; [firstDayOfWeek]
 * follows the device locale like the rest of the app.
 */
object ReminderRules {

    fun dueToday(
        snapshot: TrainingSnapshot,
        choices: NotificationChoices,
        sent: SentLog,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
    ): List<Reminder> = listOfNotNull(
        inactivity(snapshot, choices, sent, today),
        goalAtRisk(snapshot, choices, sent, today, firstDayOfWeek),
        measurementDue(snapshot, choices, sent, today),
    )

    /** After N days without training, then again every N days while the break lasts. */
    fun inactivity(snapshot: TrainingSnapshot, choices: NotificationChoices, sent: SentLog, today: LocalDate): Reminder.Inactive? {
        val every = choices.reminderDays ?: return null
        val lastDay = snapshot.trainingDays.mapNotNull(::parse).maxOrNull() ?: return null
        val daysSince = days(lastDay, today)
        if (daysSince < every) return null
        val lastSent = sent.inactivityDay?.let(::parse)
        if (lastSent != null && lastSent >= lastDay && days(lastSent, today) < every) return null
        return Reminder.Inactive(daysSince.toInt(), snapshot.nextListId, snapshot.nextListName)
    }

    /**
     * Once a week, on the day the goal needs a workout every remaining day. Not when today already
     * has a workout, in a deload week, or when the goal can't be reached any more.
     */
    fun goalAtRisk(
        snapshot: TrainingSnapshot,
        choices: NotificationChoices,
        sent: SentLog,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
    ): Reminder.GoalAtRisk? {
        if (!choices.weeklyGoal) return null
        val goal = snapshot.weeklyGoal ?: return null
        val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val weekKey = weekStart.toString()
        if (weekKey in snapshot.deloadWeeks || sent.goalWeek == weekKey) return null
        if (today.toString() in snapshot.trainingDays) return null
        val done = snapshot.trainingDays.mapNotNull(::parse).count { it >= weekStart && it < weekStart.plusDays(7) }
        val missing = goal - done
        val daysLeft = (7 - days(weekStart, today)).toInt() // today included
        return if (missing in 1..daysLeft && missing == daysLeft) Reminder.GoalAtRisk(missing, daysLeft) else null
    }

    /** When the chosen weekly or monthly rhythm has passed since the last measurement; once per rhythm. */
    fun measurementDue(snapshot: TrainingSnapshot, choices: NotificationChoices, sent: SentLog, today: LocalDate): Reminder.MeasurementDue? {
        if (!choices.measurements) return null
        val last = snapshot.lastMeasurementMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            ?: return null
        val daysSince = days(last, today)
        if (daysSince < snapshot.measurementCadenceDays) return null
        val lastSent = sent.measurementDay?.let(::parse)
        if (lastSent != null && lastSent >= last && days(lastSent, today) < snapshot.measurementCadenceDays) return null
        return Reminder.MeasurementDue(daysSince.toInt())
    }

    private fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key) }.getOrNull()
    private fun days(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)
}
