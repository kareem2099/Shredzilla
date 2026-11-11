package com.FreeRave.shredzilla.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.FreeRave.shredzilla.notifications.WorkoutNotificationStyle
import com.FreeRave.shredzilla.MainActivity
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.notifications.AppNotifications

/**
 * The live workout notification: current exercise and set, the rest countdown, this session's
 * sets, and buttons to log, adjust and move on, all from the lock screen.
 */
object WorkoutNotifier {
    private const val CHANNEL_ID = "workout_live"
    private const val NOTIFICATION_ID = 105
    private const val BRAND_COLOR = 0xFF7C4DFF.toInt()
    const val REPLY_KEY = "set_input"

    @Volatile private var lastNotice: WorkoutNotice? = null

    private fun ensureChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workout in progress", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Your current exercise, rest countdown and quick logging during a workout"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        )
    }

    fun show(context: Context, notice: WorkoutNotice) {
        lastNotice = notice
        ensureChannel(context)
        val resting = notice.restEndsAt > System.currentTimeMillis()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dumbbell)
            .setColor(BRAND_COLOR)
            .setSubText("${notice.listName} · ${notice.position}")
            // Counts down the rest, otherwise counts up the workout time
            .setWhen(if (resting) notice.restEndsAt else notice.startedAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(resting)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openWorkout(context, notice.listId))

        when (notice.style) {
            WorkoutNotificationStyle.DETAILED -> detailed(context, builder, notice, resting)
            WorkoutNotificationStyle.COMPACT -> compact(context, builder, notice, resting)
            WorkoutNotificationStyle.FOCUS -> focus(context, builder, notice, resting)
            WorkoutNotificationStyle.PROGRESS -> progress(context, builder, notice, resting)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w("WorkoutNotifier", "Notification permission missing", e)
        }
    }

    private fun statusLine(notice: WorkoutNotice, resting: Boolean): String = when {
        notice.targetMet && notice.isLastBlock -> "Target done. Finish the workout or log another set."
        notice.targetMet -> "Target done. Next: ${notice.nextExerciseName}"
        resting -> "Resting${notice.quickSet?.let { " · next $it" }.orEmpty()}"
        notice.quickSet != null -> "Ready: ${notice.quickSet}"
        else -> "Type your first set, e.g. 60 10"
    }

    /** Everything: next set, today's sets, Pro lines, what's next and totals; three buttons. */
    private fun detailed(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice, resting: Boolean) {
        val lines = buildList {
            notice.quickSet?.let { add("Next set: $it") }
            notice.doneToday?.let { add("Today: $it") }
            notice.lastTime?.let { add("Last time: $it") }
            notice.coach?.let { add("Coach: $it") }
            if (notice.proLocked) add("Last time and coach: Pro")
            add(notice.upNext)
            add(notice.totals)
        }
        builder.setContentTitle("${notice.exerciseName} · ${notice.setLabel}")
            .setContentText(statusLine(notice, resting))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
        addMainAction(context, builder, notice)
        addSecondAction(context, builder, notice)
        addEditAction(context, builder, notice)
    }

    /** One line and two buttons. */
    private fun compact(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice, resting: Boolean) {
        builder.setContentTitle("${notice.exerciseName} · ${notice.setLabel}")
            .setContentText(statusLine(notice, resting))
        addMainAction(context, builder, notice)
        addEditAction(context, builder, notice)
    }

    /** The rest comes first: "Rest" with the countdown, then "Go" with the set to do. */
    private fun focus(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice, resting: Boolean) {
        builder.setContentTitle(if (resting) "Rest · ${notice.exerciseName}" else "Go · ${notice.setLabel}")
            .setContentText(
                if (resting) "Next: ${notice.quickSet ?: notice.exerciseName}"
                else "${notice.exerciseName}${notice.quickSet?.let { " · $it" }.orEmpty()}"
            )
        if (resting) {
            notice.quickSet?.let { builder.addAction(0, "Log $it", action(context, WorkoutActionReceiver.LOG)) }
            builder.addAction(0, "+1 rep", action(context, WorkoutActionReceiver.PLUS_REP))
            builder.addAction(0, "Skip rest", action(context, WorkoutActionReceiver.SKIP_REST))
        } else {
            addMainAction(context, builder, notice)
            addSecondAction(context, builder, notice)
            addEditAction(context, builder, notice)
        }
    }

    /**
     * A bar across the whole workout, one segment per exercise. Android 16 can show it as a Live
     * Update with a chip in the status bar; older versions show a regular progress bar.
     */
    private fun progress(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice, resting: Boolean) {
        val style = NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_dumbbell))
            .setProgress(notice.progress)
        notice.segments.forEach { length -> style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(length.coerceAtLeast(1)).setColor(BRAND_COLOR)) }
        builder.setContentTitle("${notice.exerciseName} · ${notice.setLabel}")
            .setContentText("${notice.progress} of ${notice.totalSets} sets · ${statusLine(notice, resting)}")
            .setStyle(style)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(if (resting) "Rest" else "${notice.progress}/${notice.totalSets}")
        addMainAction(context, builder, notice)
        addSecondAction(context, builder, notice)
        addEditAction(context, builder, notice)
    }

    /** Log while working toward the target, then Next, and Finish on the last exercise. */
    private fun addMainAction(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice) {
        when {
            // Finishing opens the app on the summary; a receiver isn't allowed to open screens
            notice.targetMet && notice.isLastBlock -> builder.addAction(0, "Finish", openWorkout(context, notice.listId, finish = true))
            notice.targetMet -> builder.addAction(0, "Next: ${notice.nextExerciseName}", action(context, WorkoutActionReceiver.NEXT))
            notice.quickSet != null -> builder.addAction(0, "Log ${notice.quickSet}", action(context, WorkoutActionReceiver.LOG))
        }
    }

    private fun addSecondAction(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice) {
        if (notice.quickSet != null) {
            if (notice.targetMet) builder.addAction(0, "Log another", action(context, WorkoutActionReceiver.LOG))
            else builder.addAction(0, "+1 rep", action(context, WorkoutActionReceiver.PLUS_REP))
        } else if (!notice.isLastBlock) {
            builder.addAction(0, "Skip", action(context, WorkoutActionReceiver.NEXT))
        }
    }

    /** Typing a different set: "8" for reps, "9 8" for load and reps. */
    private fun addEditAction(context: Context, builder: NotificationCompat.Builder, notice: WorkoutNotice) {
        builder.addAction(
            NotificationCompat.Action.Builder(0, if (notice.quickSet != null) "Edit" else "Enter set", action(context, WorkoutActionReceiver.REPLY, mutable = true))
                .addRemoteInput(
                    RemoteInput.Builder(REPLY_KEY)
                        .setLabel(if (notice.quickSet != null) "Reps, or load and reps (8 or 9 8)" else "Load and reps, e.g. 60 10")
                        .build()
                )
                .setAllowGeneratedReplies(false)
                .build()
        )
    }

    /** Posts the last notice again, e.g. to clear the spinner after an inline reply. */
    fun repost(context: Context) {
        lastNotice?.let { show(context, it) }
    }

    fun cancel(context: Context) {
        lastNotice = null
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Opens workout mode on [listId]; with [finish], the app saves the workout and shows its summary. */
    fun openWorkout(context: Context, listId: String, finish: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AppNotifications.EXTRA_OPEN_ROUTE, AppRoutes.WORKOUT.replace("{listId}", listId))
            .putExtra(EXTRA_FINISH, finish)
        return PendingIntent.getActivity(
            context, NOTIFICATION_ID + if (finish) 1 else 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    const val EXTRA_FINISH = "finishWorkout"

    private fun action(context: Context, name: String, mutable: Boolean = false): PendingIntent {
        val intent = Intent(context, WorkoutActionReceiver::class.java).setAction(name)
        // An inline reply needs a mutable intent so the system can add the typed text
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, name.hashCode(), intent, flags)
    }
}
