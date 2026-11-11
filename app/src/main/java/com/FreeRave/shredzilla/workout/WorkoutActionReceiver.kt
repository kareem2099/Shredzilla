package com.FreeRave.shredzilla.workout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput

/** Buttons on the workout notification. Without a running app (process restarted) they open the workout instead. */
class WorkoutActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = WorkoutController.active.get()
        val session = controller?.session
        if (controller == null || session == null || session.isFinished) {
            // The app was closed by the system; a tap on the notification itself reopens the workout
            Toast.makeText(context, "Open DotShredzilla to continue your workout", Toast.LENGTH_SHORT).show()
            return
        }
        when (intent.action) {
            LOG -> {
                controller.quickEntry()?.let(controller::logInWorkout)
                com.FreeRave.shredzilla.badges.BadgeTracker.active.get()?.award(com.FreeRave.shredzilla.badges.BadgeIds.POCKET_LOGGER)
            }
            PLUS_REP -> controller.adjustReps(+1)
            NEXT -> controller.next()
            SKIP_REST -> controller.skipRest()
            REPLY -> {
                val typed = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(WorkoutNotifier.REPLY_KEY)?.toString()
                val entry = typed?.let { parseSetInput(it, controller.quickEntry()) }
                if (entry != null) {
                    controller.logInWorkout(entry)
                    com.FreeRave.shredzilla.badges.BadgeTracker.active.get()?.award(com.FreeRave.shredzilla.badges.BadgeIds.POCKET_LOGGER)
                }
                else Toast.makeText(context, "Couldn't read that. Try 8, or 9 8 for load and reps.", Toast.LENGTH_LONG).show()
                WorkoutNotifier.repost(context) // clears the reply spinner right away
            }
        }
    }

    companion object {
        const val LOG = "com.FreeRave.shredzilla.workout.LOG"
        const val PLUS_REP = "com.FreeRave.shredzilla.workout.PLUS_REP"
        const val NEXT = "com.FreeRave.shredzilla.workout.NEXT"
        const val SKIP_REST = "com.FreeRave.shredzilla.workout.SKIP_REST"
        const val REPLY = "com.FreeRave.shredzilla.workout.REPLY"
    }
}
