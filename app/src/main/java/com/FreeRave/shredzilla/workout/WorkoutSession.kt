package com.FreeRave.shredzilla.workout

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.completedSetCount
import java.util.concurrent.TimeUnit

/**
 * A guided workout through one list. It lives outside the screen so the notification can log sets
 * while the phone is locked; the sets themselves are simply everything logged since [startedAt].
 */
class WorkoutSession(
    val listId: String,
    listName: String,
    /** History timestamps are whole seconds, so the session starts on a whole second too. */
    val startedAt: Long,
) {
    var listName by mutableStateOf(listName)
    var exercises by mutableStateOf<List<ExerciseDisplayInfo>>(emptyList())
    /** Suggested sets per exercise (from a program), or null for hand-made lists. */
    var targetSets by mutableStateOf<IntRange?>(null)
    var currentIndex by mutableIntStateOf(0)
    /** Index of each superset's first exercise; it's paired with the one right after it. */
    var supersetStarts by mutableStateOf(emptyList<Int>())
    var finishedAt by mutableLongStateOf(0L)
    /** Reps added or taken away from the notification before logging the next set. */
    var repsAdjust by mutableIntStateOf(0)

    val isFinished: Boolean get() = finishedAt > 0

    fun partnerOf(index: Int): Int? = when {
        index in supersetStarts -> index + 1
        index - 1 in supersetStarts -> index - 1
        else -> null
    }

    val current: ExerciseDisplayInfo? get() = exercises.getOrNull(currentIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)))
    /** A superset moves on as one block. */
    val blockEnd: Int get() = maxOf(currentIndex, partnerOf(currentIndex) ?: currentIndex)
    val isLastBlock: Boolean get() = blockEnd >= exercises.lastIndex
    val nextExercise: ExerciseDisplayInfo? get() = exercises.getOrNull(blockEnd + 1)

    fun sessionSets(history: Map<String, List<LoggedSet>>): Map<String, List<LoggedSet>> =
        exercises.associate { exercise ->
            exercise.id to history[exercise.id].orEmpty().filter { it.timestampMillis >= startedAt }.sortedBy { it.timestampMillis }
        }

    /** The target is met for the whole block (both halves of a superset); a left/right set counts once both sides are in. */
    fun targetMet(sessionSets: Map<String, List<LoggedSet>>): Boolean {
        val target = targetSets ?: return false
        val exercise = current ?: return false
        val partner = partnerOf(currentIndex)?.let(exercises::getOrNull)
        return sessionSets[exercise.id].orEmpty().completedSetCount() >= target.first &&
            (partner == null || sessionSets[partner.id].orEmpty().completedSetCount() >= target.first)
    }

    fun moveToNext() {
        if (!isLastBlock) {
            currentIndex = blockEnd + 1
            repsAdjust = 0
        }
    }

    companion object {
        fun startNow(listId: String, listName: String) = WorkoutSession(listId, listName, System.currentTimeMillis() / 1000 * 1000)
    }
}

/** Keeps the running session across process death, like the screen's saved state used to. */
class WorkoutSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("workout_session", Context.MODE_PRIVATE)

    fun save(session: WorkoutSession?) = prefs.edit {
        if (session == null) {
            clear()
            return@edit
        }
        putString("listId", session.listId)
        putString("listName", session.listName)
        putLong("startedAt", session.startedAt)
        putInt("currentIndex", session.currentIndex)
        putString("supersets", session.supersetStarts.joinToString(","))
        putLong("finishedAt", session.finishedAt)
    }

    /** The saved session if it started within the last [MAX_AGE_HOURS] hours. */
    fun restore(): WorkoutSession? {
        val listId = prefs.getString("listId", null) ?: return null
        val startedAt = prefs.getLong("startedAt", 0L)
        if (System.currentTimeMillis() - startedAt > TimeUnit.HOURS.toMillis(MAX_AGE_HOURS)) {
            save(null)
            return null
        }
        return WorkoutSession(listId, prefs.getString("listName", null) ?: "Workout", startedAt).apply {
            currentIndex = prefs.getInt("currentIndex", 0)
            supersetStarts = prefs.getString("supersets", "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }
            finishedAt = prefs.getLong("finishedAt", 0L)
        }
    }

    private companion object {
        const val MAX_AGE_HOURS = 6L
    }
}
