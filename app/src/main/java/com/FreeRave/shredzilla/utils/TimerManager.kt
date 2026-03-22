package com.FreeRave.shredzilla.utils

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val userRestTimePreferenceProvider: () -> String // To get current preference
) {
    var isTimerRunning by mutableStateOf(false)
        private set
    var timerRemainingSeconds by mutableIntStateOf(0)
        private set
    var timerTotalSeconds by mutableIntStateOf(60) // Default, will be updated
        private set

    private var timerJob: Job? = null

    fun startRestTimer() {
        timerJob?.cancel() // Cancel any existing timer job

        val durationPreference = userRestTimePreferenceProvider()
        // Basic parsing, assuming "Xmin" or "Xmin Ys" format.
        // A more robust parser might be needed for complex strings.
        var duration = 60 // Default to 60 seconds
        if (durationPreference.contains("min")) {
            duration = durationPreference.substringBefore("min").toIntOrNull()?.times(60) ?: 60
            if (durationPreference.contains("s")) {
                duration += durationPreference.substringAfter("min").substringBefore("s").trim().toIntOrNull() ?: 0
            }
        } else if (durationPreference.contains("s")) {
            duration = durationPreference.substringBefore("s").toIntOrNull() ?: 60
        }


        timerTotalSeconds = duration
        timerRemainingSeconds = duration
        isTimerRunning = true
        NotificationUtils.showTimerRunningNotification(context, timerRemainingSeconds, timerTotalSeconds)

        val targetEndTimeMillis = System.currentTimeMillis() + (duration * 1000L)

        timerJob = coroutineScope.launch {
            while (isTimerRunning) {
                val currentMillis = System.currentTimeMillis()
                if (currentMillis >= targetEndTimeMillis) {
                    timerRemainingSeconds = 0
                    break // Loop halts precisely on time
                }

                // Mathematical delta logic guarantees we never drift out of sync due to Coroutine pauses
                timerRemainingSeconds = ((targetEndTimeMillis - currentMillis) / 1000L).toInt()
                NotificationUtils.showTimerRunningNotification(context, timerRemainingSeconds, timerTotalSeconds)

                delay(100L) // Quick ticks, but the evaluation remains anchored to currentTimeMillis
            }

            if (isTimerRunning && timerRemainingSeconds <= 0) { // If timer completed naturally without user interruption
                isTimerRunning = false
                NotificationUtils.cancelTimerRunningNotification(context)
                NotificationUtils.showTimerFinishedNotification(context)
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        isTimerRunning = false
        if (timerRemainingSeconds > 0) { // Only cancel notification if it was running and not finished
            NotificationUtils.cancelTimerRunningNotification(context)
        }
        timerRemainingSeconds = 0 // Reset remaining time
    }

    // Optional: A method to update total seconds if preference changes while timer not running
    fun updateTotalSecondsFromPreference() {
        if (!isTimerRunning) {
            val durationPreference = userRestTimePreferenceProvider()
            var duration = 60
            if (durationPreference.contains("min")) {
                duration = durationPreference.substringBefore("min").toIntOrNull()?.times(60) ?: 60
                if (durationPreference.contains("s")) {
                    duration += durationPreference.substringAfter("min").substringBefore("s").trim().toIntOrNull() ?: 0
                }
            } else if (durationPreference.contains("s")) {
                duration = durationPreference.substringBefore("s").toIntOrNull() ?: 60
            }
            timerTotalSeconds = duration
        }
    }
}
