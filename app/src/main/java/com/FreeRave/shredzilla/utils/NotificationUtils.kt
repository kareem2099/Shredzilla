package com.FreeRave.shredzilla.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.FreeRave.shredzilla.MainActivity // Assuming MainActivity is your entry point
import com.FreeRave.shredzilla.R
import java.util.concurrent.TimeUnit

object NotificationUtils {

    const val CHANNEL_ID = "shredzilla_timer_channel"
    const val CHANNEL_NAME = "Rest Timer Notifications"
    const val CHANNEL_DESCRIPTION = "Notifications for rest timer status"
    const val TIMER_RUNNING_NOTIFICATION_ID = 101
    const val TIMER_FINISHED_NOTIFICATION_ID = 102 // Different ID for finished

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // Ensures sound and heads-up
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                // You can set vibration patterns, lights, etc. here if desired
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerRunningNotification(context: Context, remainingSeconds: Int, totalSeconds: Int) {
        createNotificationChannel(context) // Ensure channel exists

        val progress = if (totalSeconds > 0) ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat() * 100).toInt() else 0
        val timeFormatted = String.format(
            "%02d:%02d",
            TimeUnit.SECONDS.toMinutes(remainingSeconds.toLong()),
            remainingSeconds % 60
        )

        // Intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app's notification icon
            .setContentTitle("Rest Timer Active")
            .setContentText("Next set in: $timeFormatted")
            .setPriority(NotificationCompat.PRIORITY_LOW) // Lower priority for ongoing
            .setOngoing(true) // Makes the notification non-dismissable by swipe
            .setOnlyAlertOnce(true) // Important for progress updates
            .setProgress(totalSeconds, totalSeconds - remainingSeconds, false)
            .setContentIntent(pendingIntent) // Open app on tap

        try {
            NotificationManagerCompat.from(context).notify(TIMER_RUNNING_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationUtils", "Failed to show running notification: ${e.message}")
        }
    }

    fun showTimerFinishedNotification(context: Context) {
        createNotificationChannel(context) // Ensure channel exists

        // Cancel the running notification first
        NotificationManagerCompat.from(context).cancel(TIMER_RUNNING_NOTIFICATION_ID)

        // Intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Rest Over!")
            .setContentText("Time for your next set!")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to make sound/vibrate
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Defaults for sound/vibration will be used based on channel importance (HIGH)
            // To be explicit: .setDefaults(Notification.DEFAULT_ALL) or .setSound(defaultSoundUri).setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))

        try {
            NotificationManagerCompat.from(context).notify(TIMER_FINISHED_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationUtils", "Failed to show finished notification: ${e.message}")
        }
    }

    fun cancelTimerRunningNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(TIMER_RUNNING_NOTIFICATION_ID)
    }
}
