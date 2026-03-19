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

        timerJob = coroutineScope.launch {
            while (timerRemainingSeconds > 0 && isTimerRunning) {
                delay(1000L)
                if (isTimerRunning) { // Check again in case it was stopped externally
                    timerRemainingSeconds--
                    NotificationUtils.showTimerRunningNotification(context, timerRemainingSeconds, timerTotalSeconds)
                }
            }
            if (isTimerRunning) { // If timer completed naturally
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
