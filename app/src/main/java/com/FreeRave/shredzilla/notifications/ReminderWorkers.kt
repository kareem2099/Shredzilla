package com.FreeRave.shredzilla.notifications

import android.content.Context
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Once a day at the chosen hour: inactivity, weekly goal and measurement reminders. */
class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = ReminderStore(applicationContext)
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()
        // A run the system deferred into the night waits for the next day instead of waking anyone
        if (LocalDateTime.now().hour !in QUIET_HOURS_END..QUIET_HOURS_START) return Result.success()

        val today = LocalDate.now()
        val weekKey = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek)).toString()
        ReminderRules.dueToday(store.snapshot(), store.choices(), store.sent(), today).forEach { reminder ->
            post(applicationContext, reminder)
            store.markSent(reminder, today.toString(), weekKey)
        }
        return Result.success()
    }

    private fun post(context: Context, reminder: Reminder) = when (reminder) {
        is Reminder.Inactive -> AppNotifications.show(
            context, AppNotifications.Kind.INACTIVE,
            title = "${reminder.daysSince} days since your last workout",
            text = reminder.nextListName?.let { "Next up: $it. Tap to open it." }
                ?: "Your lists are ready when you are. Even a short session keeps the habit.",
            route = reminder.nextListId?.let { id ->
                AppRoutes.EXERCISE_LIST_DETAIL.replace("{listId}", id).replace("{listName}", Uri.encode(reminder.nextListName.orEmpty()))
            } ?: AppRoutes.SETS,
        )
        is Reminder.GoalAtRisk -> AppNotifications.show(
            context, AppNotifications.Kind.WEEKLY_GOAL,
            title = if (reminder.missing == 1) "One workout left for your weekly goal" else "${reminder.missing} workouts left this week",
            text = if (reminder.daysLeft == 1) "Today is the last day of the week. Train today to hit your goal."
                else "There are ${reminder.daysLeft} days left, so every one counts to hit your goal.",
            route = AppRoutes.SETS,
        )
        is Reminder.MeasurementDue -> AppNotifications.show(
            context, AppNotifications.Kind.MEASUREMENTS,
            title = "Time to measure",
            text = "It's been ${reminder.daysSince} days since your last measurements. Same time of day, same spot, for a fair comparison.",
            route = AppRoutes.MEASUREMENT_ENTRY.replace("{measurementId}", "new"),
        )
    }

    private companion object {
        const val QUIET_HOURS_END = 8
        const val QUIET_HOURS_START = 22
    }
}

/** Ten minutes before ad-earned Pro time runs out. */
class ProEndingWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = ReminderStore(applicationContext)
        val left = store.proUntilMillis - System.currentTimeMillis()
        // Rescheduled whenever the time changes; this still checks it wasn't extended or bought outright
        if (store.ownsLifetime || !store.choices().proEnding || left <= 0 || left > ReminderScheduler.PRO_WARNING_MS + 60_000) return Result.success()
        AppNotifications.show(
            applicationContext, AppNotifications.Kind.PRO_TIME,
            title = "Pro ends in ${TimeUnit.MILLISECONDS.toMinutes(left).coerceAtLeast(1)} minutes",
            text = "Watch an ad for another hour, or keep Pro for life.",
            route = AppRoutes.PRO,
        )
        return Result.success()
    }
}

object ReminderScheduler {
    private const val DAILY = "daily_reminders"
    private const val PRO_ENDING = "pro_ending"
    val PRO_WARNING_MS = TimeUnit.MINUTES.toMillis(10)

    /** Schedules (or stops) the daily check at the chosen hour; only re-times it when the hour changes. */
    fun syncDaily(context: Context) {
        val store = ReminderStore(context)
        val choices = store.choices()
        val work = WorkManager.getInstance(context)
        if (choices.reminderDays == null && !choices.weeklyGoal && !choices.measurements) {
            work.cancelUniqueWork(DAILY)
            store.scheduledHour = -1
            return
        }
        val hourChanged = store.scheduledHour != choices.reminderHour
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(choices.reminderHour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = java.time.Duration.between(now, next).toMillis()
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        work.enqueueUniquePeriodicWork(
            DAILY,
            if (hourChanged) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        store.scheduledHour = choices.reminderHour
    }

    /** Re-times the "Pro ends soon" alert for the current ad time, or cancels it. */
    fun syncProEnding(context: Context) {
        val store = ReminderStore(context)
        val work = WorkManager.getInstance(context)
        val fireIn = store.proUntilMillis - PRO_WARNING_MS - System.currentTimeMillis()
        if (store.ownsLifetime || !store.choices().proEnding || fireIn <= 0) {
            work.cancelUniqueWork(PRO_ENDING)
            return
        }
        work.enqueueUniqueWork(
            PRO_ENDING,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ProEndingWorker>().setInitialDelay(fireIn, TimeUnit.MILLISECONDS).build()
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY)
        WorkManager.getInstance(context).cancelUniqueWork(PRO_ENDING)
    }
}
