package com.FreeRave.shredzilla.workout

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.FreeRave.shredzilla.composables.PrCelebration
import com.FreeRave.shredzilla.data.TrainingHistoryStore
import com.FreeRave.shredzilla.data.UserDataManager
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.RecordSet
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.models.exerciseIdOf
import com.FreeRave.shredzilla.pro.BillingRepository
import com.FreeRave.shredzilla.screens.workout.buildWorkoutLog
import com.FreeRave.shredzilla.utils.RecordCheck
import com.FreeRave.shredzilla.utils.SetPrefill
import com.FreeRave.shredzilla.utils.TimerManager
import com.FreeRave.shredzilla.utils.checkRecord
import com.FreeRave.shredzilla.utils.dateKeyOf
import com.FreeRave.shredzilla.utils.displayToKg
import com.FreeRave.shredzilla.utils.formatEntry
import com.FreeRave.shredzilla.utils.nextEntry
import com.FreeRave.shredzilla.utils.personalRecordIds
import com.FreeRave.shredzilla.utils.suggestNextSet
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Logging sets and the guided workout, outside any screen: the set sheet, workout mode and the
 * workout notification all go through here, so a set logged from the lock screen gets the same
 * records, rest timer and superset handling as one logged in the app.
 */
class WorkoutController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val userDataManager: UserDataManager,
    private val trainingHistory: TrainingHistoryStore,
    private val timerManager: TimerManager,
    private val billing: BillingRepository,
    private val history: StateFlow<Map<String, List<LoggedSet>>>,
    private val refreshHistory: () -> Unit,
) {
    private val sessionStore = WorkoutSessionStore(context)

    var session by mutableStateOf(sessionStore.restore())
        private set
    /** Called after every logged set (is it a working set, did it beat a record), for the badge counters. */
    var onSetLogged: (workingSet: Boolean, beatRecord: Boolean) -> Unit = { _, _ -> }
    /** A beaten personal record, shown as a banner by the app. */
    var celebration by mutableStateOf<PrCelebration?>(null)

    init {
        active = WeakReference(this)
        // While a workout runs, its notification shows the rest countdown instead of the plain one
        timerManager.showOwnNotification = { session?.isFinished != false || !userDataManager.notificationChoices.workoutNotification }
        scope.launch {
            snapshotFlow { session?.let { listOf(it.listId, it.currentIndex, it.supersetStarts, it.finishedAt, it.listName) } }
                .collect { sessionStore.save(session) }
        }
        scope.launch {
            combine(snapshotFlow { noticeInputs() }, history) { _, h -> buildNotice(h) }
                .distinctUntilChanged()
                .collect { notice -> if (notice == null) WorkoutNotifier.cancel(context) else WorkoutNotifier.show(context, notice) }
        }
    }

    // --- Logging ---

    /** A new set starts from the last one logged; the suggestion is the progression step from the last session. */
    fun prefillFor(exerciseName: String): SetPrefill {
        val sets = history.value[exerciseIdOf(exerciseName)].orEmpty()
        val unitSystem = userDataManager.userUnitSystem
        return SetPrefill(
            initial = sets.firstOrNull()?.nextEntry(unitSystem) ?: SetEntry(),
            suggestion = suggestNextSet(sets, unitSystem)
        )
    }

    /** Saves a set, checks it against the records and starts the rest. Returns whether a rest started. */
    fun logSet(exerciseName: String, entry: SetEntry, startRest: Boolean = true): Boolean {
        // Checked against the all-time record and the history before this set lands in it
        val unitSystem = userDataManager.userUnitSystem
        val exerciseId = exerciseIdOf(exerciseName)
        val sets = history.value[exerciseId].orEmpty()
        val check = checkRecord(sets, trainingHistory.personalRecords[exerciseId], entry, unitSystem)
        if (check != RecordCheck.NONE) {
            val load = entry.plates?.toDouble() ?: displayToKg(entry.weight, unitSystem)
            trainingHistory.saveRecord(exerciseId, exerciseName, entry.plates != null, RecordSet(load, entry.reps, System.currentTimeMillis()))
        }
        if (check == RecordCheck.BEATEN) celebration = PrCelebration(exerciseName, formatEntry(entry, unitSystem))
        // One side at a time: rest once both sides of the pair are done, not between them
        val todaySets = sets.filter { it.dateKey == dateKeyOf(System.currentTimeMillis()) }
        val pairComplete = entry.side == null ||
            todaySets.count { it.side == entry.side } + 1 <= todaySets.count { it.side == entry.side.opposite }
        userDataManager.recordSet(exerciseName, entry, FirebaseAuth.getInstance().currentUser, onSaved = refreshHistory)
        val rests = startRest && pairComplete
        if (rests) timerManager.startRestTimer()
        onSetLogged(entry.setType != com.FreeRave.shredzilla.models.SetType.WARMUP, check == RecordCheck.BEATEN)
        return rests
    }

    // --- The guided workout ---

    /** Starts a workout on [listId], or picks the running one back up, keeping its exercises current. */
    fun start(listId: String, listName: String, exercises: List<ExerciseDisplayInfo>, targetSets: IntRange?): WorkoutSession {
        val running = session?.takeIf { it.listId == listId }
        val current = running ?: WorkoutSession.startNow(listId, listName).also { session = it }
        current.listName = listName
        current.exercises = exercises
        current.targetSets = targetSets
        return current
    }

    /** Logs [entry] for the current exercise; in a superset the first half jumps to the partner without rest. */
    fun logInWorkout(entry: SetEntry): Boolean {
        val s = session ?: return false
        val exercise = s.current ?: return false
        val partnerIndex = s.partnerOf(s.currentIndex)
        val isFirstHalf = partnerIndex != null && partnerIndex > s.currentIndex
        val rested = logSet(exercise.name, entry, startRest = !isFirstHalf)
        if (partnerIndex != null) s.currentIndex = partnerIndex
        s.repsAdjust = 0
        return rested
    }

    /** The set the notification logs: the last one again with any +/- reps, or null when there's nothing to repeat. */
    fun quickEntry(): SetEntry? {
        val s = session ?: return null
        val exercise = s.current ?: return null
        if (history.value[exercise.id].isNullOrEmpty()) return null
        val initial = prefillFor(exercise.name).initial
        return initial.copy(reps = (initial.reps + s.repsAdjust).coerceAtLeast(1), notes = "")
    }

    fun adjustReps(by: Int) {
        val s = session ?: return
        val base = quickEntry()?.reps?.minus(s.repsAdjust) ?: return
        s.repsAdjust = (s.repsAdjust + by).coerceAtLeast(1 - base)
    }

    fun next() = session?.moveToNext()

    fun skipRest() = timerManager.stopRestTimer()

    /** Saves the workout; the screen then shows its summary. Null when nothing was logged. */
    fun finish(): WorkoutLog? {
        val s = session ?: return null
        val h = history.value
        val sets = s.sessionSets(h)
        if (sets.values.all { it.isEmpty() }) return null
        s.finishedAt = System.currentTimeMillis()
        timerManager.stopRestTimer()
        val recordIds = s.exercises.flatMap { personalRecordIds(h[it.id].orEmpty()) }.toSet()
        return buildWorkoutLog(s.listName, s.startedAt, s.finishedAt, s.exercises, sets, recordIds)
            .also(trainingHistory::saveWorkout)
    }

    /** Leaves the workout (after the summary, or without logging anything). */
    fun end() {
        session = null
        WorkoutNotifier.cancel(context)
    }

    fun clear() {
        session = null
        celebration = null
        sessionStore.save(null)
        WorkoutNotifier.cancel(context)
    }

    // --- The notification ---

    private fun isPro(now: Long) = billing.ownsProLifetime || userDataManager.adTimeBank.isActive(now)

    /** Everything the notification shows, read inside a snapshot so any change re-posts it. */
    private fun noticeInputs(): List<Any?> {
        val s = session ?: return emptyList()
        return listOf(
            s.currentIndex, s.supersetStarts, s.finishedAt, s.repsAdjust, s.exercises, s.targetSets, s.listName,
            timerManager.isTimerRunning, timerManager.restEndsAtMillis, userDataManager.userUnitSystem,
            billing.ownsProLifetime, userDataManager.adTimeBank, userDataManager.notificationChoices,
        )
    }

    private fun buildNotice(h: Map<String, List<LoggedSet>>): WorkoutNotice? {
        val s = session ?: return null
        if (s.isFinished || !userDataManager.notificationChoices.workoutNotification) return null
        val exercise = s.current ?: return null
        return WorkoutNotice.from(
            session = s,
            exercise = exercise,
            history = h,
            unitSystem = userDataManager.userUnitSystem,
            quickEntry = quickEntry(),
            suggestion = prefillFor(exercise.name).suggestion,
            restEndsAt = if (timerManager.isTimerRunning) timerManager.restEndsAtMillis else 0L,
            isPro = isPro(System.currentTimeMillis()),
            style = userDataManager.notificationChoices.workoutStyle,
        )
    }

    companion object {
        /** The live controller, for notification actions; empty when the app process was restarted. */
        @Volatile var active: WeakReference<WorkoutController> = WeakReference(null)
    }
}
