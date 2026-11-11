package com.FreeRave.shredzilla.notifications

import android.content.Context
import androidx.core.content.edit

/**
 * The app's latest training snapshot and notification choices, kept on the device so the
 * background check works without the app open, offline and without reading Firestore.
 */
class ReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun saveSnapshot(s: TrainingSnapshot) = prefs.edit {
        putStringSet(K_DAYS, s.trainingDays)
        putInt(K_GOAL, s.weeklyGoal ?: 0)
        putStringSet(K_DELOAD, s.deloadWeeks)
        putLong(K_LAST_MEASURE, s.lastMeasurementMillis ?: 0L)
        putInt(K_CADENCE, s.measurementCadenceDays)
        putString(K_NEXT_ID, s.nextListId)
        putString(K_NEXT_NAME, s.nextListName)
    }

    fun snapshot() = TrainingSnapshot(
        trainingDays = prefs.getStringSet(K_DAYS, emptySet()).orEmpty(),
        weeklyGoal = prefs.getInt(K_GOAL, 0).takeIf { it > 0 },
        deloadWeeks = prefs.getStringSet(K_DELOAD, emptySet()).orEmpty(),
        lastMeasurementMillis = prefs.getLong(K_LAST_MEASURE, 0L).takeIf { it > 0 },
        measurementCadenceDays = prefs.getInt(K_CADENCE, 7),
        nextListId = prefs.getString(K_NEXT_ID, null),
        nextListName = prefs.getString(K_NEXT_NAME, null),
    )

    fun saveChoices(c: NotificationChoices) = prefs.edit {
        putInt(K_REMINDER_DAYS, c.reminderDays ?: 0)
        putInt(K_HOUR, c.reminderHour)
        putBoolean(K_NOTIFY_GOAL, c.weeklyGoal)
        putBoolean(K_NOTIFY_MEASURE, c.measurements)
        putBoolean(K_NOTIFY_PRO, c.proEnding)
    }

    fun choices() = NotificationChoices(
        reminderDays = prefs.getInt(K_REMINDER_DAYS, 0).takeIf { it > 0 },
        reminderHour = prefs.getInt(K_HOUR, NotificationChoices.DEFAULT_REMINDER_HOUR),
        weeklyGoal = prefs.getBoolean(K_NOTIFY_GOAL, true),
        measurements = prefs.getBoolean(K_NOTIFY_MEASURE, true),
        proEnding = prefs.getBoolean(K_NOTIFY_PRO, true),
    )

    fun sent() = SentLog(
        inactivityDay = prefs.getString(K_SENT_INACTIVE, null),
        goalWeek = prefs.getString(K_SENT_GOAL, null),
        measurementDay = prefs.getString(K_SENT_MEASURE, null),
    )

    fun markSent(reminder: Reminder, todayKey: String, weekKey: String) = prefs.edit {
        when (reminder) {
            is Reminder.Inactive -> putString(K_SENT_INACTIVE, todayKey)
            is Reminder.GoalAtRisk -> putString(K_SENT_GOAL, weekKey)
            is Reminder.MeasurementDue -> putString(K_SENT_MEASURE, todayKey)
        }
    }

    /** Ad-earned Pro end time (0 when none) and lifetime ownership, for the "Pro ends soon" alert. */
    fun saveProTime(untilMillis: Long, ownsLifetime: Boolean) = prefs.edit {
        putLong(K_PRO_UNTIL, untilMillis)
        putBoolean(K_LIFETIME, ownsLifetime)
    }
    val proUntilMillis: Long get() = prefs.getLong(K_PRO_UNTIL, 0L)
    val ownsLifetime: Boolean get() = prefs.getBoolean(K_LIFETIME, false)

    /** Hour the daily check was scheduled for, to reschedule only when it changes. */
    var scheduledHour: Int
        get() = prefs.getInt(K_SCHEDULED_HOUR, -1)
        set(value) = prefs.edit { putInt(K_SCHEDULED_HOUR, value) }

    /** Asked once automatically; after that only from the notification settings. */
    var permissionPrompted: Boolean
        get() = prefs.getBoolean(K_PROMPTED, false)
        set(value) = prefs.edit { putBoolean(K_PROMPTED, value) }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val K_DAYS = "training_days"
        const val K_GOAL = "weekly_goal"
        const val K_DELOAD = "deload_weeks"
        const val K_LAST_MEASURE = "last_measurement"
        const val K_CADENCE = "measurement_cadence"
        const val K_NEXT_ID = "next_list_id"
        const val K_NEXT_NAME = "next_list_name"
        const val K_REMINDER_DAYS = "reminder_days"
        const val K_HOUR = "reminder_hour"
        const val K_NOTIFY_GOAL = "notify_goal"
        const val K_NOTIFY_MEASURE = "notify_measure"
        const val K_NOTIFY_PRO = "notify_pro"
        const val K_SENT_INACTIVE = "sent_inactive"
        const val K_SENT_GOAL = "sent_goal"
        const val K_SENT_MEASURE = "sent_measure"
        const val K_PROMPTED = "permission_prompted"
        const val K_PRO_UNTIL = "pro_until"
        const val K_LIFETIME = "owns_lifetime"
        const val K_SCHEDULED_HOUR = "scheduled_hour"
    }
}
