package com.FreeRave.shredzilla.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.FreeRave.shredzilla.R

/** Rest timer notifications: a silent lock-screen countdown, then a heads-up "rest over" alert. */
object NotificationUtils {

    private const val LEGACY_CHANNEL_ID = "shredzilla_timer_channel" // one HIGH channel for both, replaced
    private const val RUNNING_CHANNEL_ID = "rest_timer_running"
    private const val FINISHED_CHANNEL_ID = "rest_timer_finished"
    private const val TIMER_RUNNING_NOTIFICATION_ID = 101
    private const val TIMER_FINISHED_NOTIFICATION_ID = 102
    private const val FINISHED_TIMEOUT_MS = 2 * 60 * 1000L
    private const val BRAND_COLOR = 0xFF7C4DFF.toInt()

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel(RUNNING_CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Countdown on the lock screen while you rest"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(FINISHED_CHANNEL_ID, "Rest over", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alert when it's time for your next set"
                enableVibration(false) // TimerManager vibrates itself, also when notifications are off
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )
    }

    /** Like tapping the launcher icon: brings the running app forward instead of restarting it. */
    private fun openAppIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Posted once: the system counts down to [endsAtMillis] itself, also on the lock screen. */
    fun showRestTimerNotification(context: Context, endsAtMillis: Long) {
        val notification = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setColor(BRAND_COLOR)
            .setContentTitle("Resting")
            .setContentText("Next set when the timer hits zero")
            .setWhen(endsAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Removes itself even if the app is killed mid-rest
            .setTimeoutAfter(endsAtMillis - System.currentTimeMillis())
            .setContentIntent(openAppIntent(context))
            .build()
        notify(context, TIMER_RUNNING_NOTIFICATION_ID, notification)
        NotificationManagerCompat.from(context).cancel(TIMER_FINISHED_NOTIFICATION_ID)
    }

    fun showRestOverNotification(context: Context) {
        cancelRestTimerNotification(context)
        val notification = NotificationCompat.Builder(context, FINISHED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setColor(BRAND_COLOR)
            .setContentTitle("Rest over")
            .setContentText("Time for your next set 💪")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(FINISHED_TIMEOUT_MS)
            .setContentIntent(openAppIntent(context))
            .build()
        notify(context, TIMER_FINISHED_NOTIFICATION_ID, notification)
    }

    fun cancelRestTimerNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(TIMER_RUNNING_NOTIFICATION_ID)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w("NotificationUtils", "Notification permission missing", e) // user declined POST_NOTIFICATIONS
        }
    }
}
