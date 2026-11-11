package com.FreeRave.shredzilla.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.FreeRave.shredzilla.MainActivity
import com.FreeRave.shredzilla.R

/**
 * Reminder notifications, one Android channel per kind so each can be silenced on its own in
 * the system settings too. The rest timer keeps its own channels in NotificationUtils.
 */
object AppNotifications {
    /** Intent extra with an app route to open, read by MainActivity. */
    const val EXTRA_OPEN_ROUTE = "openRoute"
    private const val BRAND_COLOR = 0xFF7C4DFF.toInt()

    enum class Kind(val channelId: String, val channelName: String, val description: String, val notificationId: Int) {
        INACTIVE("workout_reminders", "Workout reminders", "A nudge after the days without training you chose", 201),
        WEEKLY_GOAL("weekly_goal", "Weekly goal", "When your weekly goal needs a workout every day that's left", 202),
        MEASUREMENTS("measurements", "Body measurements", "When it's time to measure again, weekly or monthly", 203),
        PRO_TIME("pro_time", "Pro time", "Shortly before your Pro hours from ads run out", 204),
    }

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        Kind.entries.forEach { kind ->
            manager.createNotificationChannel(
                NotificationChannel(kind.channelId, kind.channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = kind.description
                }
            )
        }
    }

    fun enabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, kind: Kind, title: String, text: String, route: String?) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { route?.let { putExtra(EXTRA_OPEN_ROUTE, it) } }
        val pending = PendingIntent.getActivity(
            context, kind.notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(kind.notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("AppNotifications", "Notification permission missing", e)
        }
    }
}
