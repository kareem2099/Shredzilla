package com.FreeRave.shredzilla.utils

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TICK_MS = 250L
private const val WAKE_LOCK_SLACK_MS = 10_000L
private val REST_OVER_VIBRATION = longArrayOf(0, 400, 150, 400, 150, 700)

/**
 * Rest countdown between sets. Shows a lock-screen countdown notification, keeps the CPU awake
 * for the length of the rest (otherwise the end can be late with the screen off), then alerts
 * with a notification and a vibration.
 */
class TimerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val userRestTimePreferenceProvider: () -> String
) {
    var isTimerRunning by mutableStateOf(false)
        private set
    var timerRemainingSeconds by mutableIntStateOf(0)
        private set
    var timerTotalSeconds by mutableIntStateOf(60)
        private set

    private var timerJob: Job? = null
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Shredzilla:RestTimer")
        .apply { setReferenceCounted(false) }

    fun startRestTimer() {
        timerJob?.cancel()
        val duration = parseRestPreference(userRestTimePreferenceProvider())
        val endsAt = System.currentTimeMillis() + duration * 1000L
        timerTotalSeconds = duration
        timerRemainingSeconds = duration
        isTimerRunning = true
        wakeLock.acquire(duration * 1000L + WAKE_LOCK_SLACK_MS)
        NotificationUtils.showRestTimerNotification(context, endsAt)

        timerJob = coroutineScope.launch {
            // Anchored to the end time, so a late tick never makes the timer drift
            while (true) {
                val left = endsAt - System.currentTimeMillis()
                if (left <= 0) break
                timerRemainingSeconds = ((left + 999) / 1000).toInt()
                delay(minOf(left, TICK_MS))
            }
            timerRemainingSeconds = 0
            isTimerRunning = false
            NotificationUtils.showRestOverNotification(context)
            vibrateRestOver()
            releaseWakeLock()
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        isTimerRunning = false
        timerRemainingSeconds = 0
        NotificationUtils.cancelRestTimerNotification(context)
        releaseWakeLock()
    }

    fun updateTotalSecondsFromPreference() {
        if (!isTimerRunning) {
            timerTotalSeconds = parseRestPreference(userRestTimePreferenceProvider())
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    /** Alarm usage so it still buzzes with the phone on silent in a gym bag. */
    private fun vibrateRestOver() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        } ?: return
        val effect = VibrationEffect.createWaveform(REST_OVER_VIBRATION, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
