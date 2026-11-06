package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.PersonalRecord
import com.FreeRave.shredzilla.models.RecordSet
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.screens.settings.UnitSystem

// Double progression: earn reps at a load up to the top of the range, then add load and start over
private const val REP_RANGE_BOTTOM = 8
private const val REP_RANGE_TOP = 12
private const val LOAD_EPSILON = 1e-6

/** What a new set starts from: the last set logged, plus a progression hint when there's history. */
data class SetPrefill(val initial: SetEntry = SetEntry(), val suggestion: SetSuggestion? = null)

/** A suggested next set and the last-session set it builds on. */
data class SetSuggestion(val next: SetEntry, val basedOn: SetEntry)

/** Plate-counted and weighted sets aren't comparable, so records are tracked per kind. */
private val LoggedSet.isPlates: Boolean get() = plates != null
private val LoggedSet.load: Double get() = plates?.toDouble() ?: weightKg
/** Bodyweight sets have no comparable load (added weight isn't the whole load), so they hold no records. */
private val LoggedSet.countsForRecords: Boolean get() = !bodyweight && setType != SetType.WARMUP && load > 0

/**
 * Doc ids of sets that beat every earlier set of the same exercise on load (heavier, or more
 * plates). The first loaded set of each kind isn't a record, there was nothing to beat yet.
 * [history] must be one exercise's sets, in any order.
 */
fun personalRecordIds(history: List<LoggedSet>): Set<String> {
    val records = mutableSetOf<String>()
    val best = mutableMapOf<Boolean, Double>()
    history.filter { it.countsForRecords }.sortedBy { it.timestampMillis }.forEach { set ->
        val previous = best[set.isPlates]
        if (previous != null && set.load > previous + LOAD_EPSILON) records += set.docId
        if (previous == null || set.load > previous) best[set.isPlates] = set.load
    }
    return records
}

enum class RecordCheck { NONE, FIRST, BEATEN }

/**
 * How logging [entry] (user's unit) compares with the exercise's best of the same kind, from the
 * all-time [stored] record and the loaded [history]. FIRST means there's nothing to beat yet:
 * worth storing, not worth a celebration.
 */
fun checkRecord(history: List<LoggedSet>, stored: PersonalRecord?, entry: SetEntry, unitSystem: UnitSystem): RecordCheck {
    val isPlates = entry.plates != null
    val load = entry.plates?.toDouble() ?: displayToKg(entry.weight, unitSystem)
    if (load <= 0 || entry.bodyweight || entry.setType == SetType.WARMUP) return RecordCheck.NONE
    val storedBest = (if (isPlates) stored?.mostPlates else stored?.heaviest)?.load
    val historyBest = history.filter { it.isPlates == isPlates && it.countsForRecords }.maxOfOrNull { it.load }
    val previousBest = listOfNotNull(storedBest, historyBest).maxOrNull() ?: return RecordCheck.FIRST
    return if (load > previousBest + LOAD_EPSILON) RecordCheck.BEATEN else RecordCheck.NONE
}

/** Bests in [history] (per kind: true = plates) that the [stored] record doesn't cover yet. */
fun recordsToBackfill(history: List<LoggedSet>, stored: PersonalRecord?): List<Pair<Boolean, RecordSet>> =
    listOf(false, true).mapNotNull { isPlates ->
        val best = history.filter { it.isPlates == isPlates && it.countsForRecords }
            .maxWithOrNull(compareBy<LoggedSet> { it.load }.thenBy { it.reps }) ?: return@mapNotNull null
        val storedBest = if (isPlates) stored?.mostPlates else stored?.heaviest
        if (storedBest != null && best.load <= storedBest.load + LOAD_EPSILON) null
        else isPlates to RecordSet(best.load, best.reps, best.timestampMillis)
    }

/**
 * Next step from the last session before today: one more rep on its top set, or, once that set
 * reached [REP_RANGE_TOP] reps, a bit more load back at [REP_RANGE_BOTTOM]. Null without history.
 */
fun suggestNextSet(history: List<LoggedSet>, unitSystem: UnitSystem): SetSuggestion? {
    val todayKey = dateKeyOf(System.currentTimeMillis())
    // Date keys are yyyy-MM-dd, so the largest one is the latest day
    val lastSession = history.filter { it.dateKey != todayKey && it.setType != SetType.WARMUP }.groupBy { it.dateKey }.maxByOrNull { it.key }?.value
        ?: return null
    val top = lastSession.maxWith(compareBy<LoggedSet> { it.load }.thenBy { it.reps })
    val base = top.toEntry(unitSystem).copy(notes = "")
    val next = when {
        top.reps < REP_RANGE_TOP -> base.copy(reps = top.reps + 1)
        top.plates != null -> base.copy(plates = top.plates + 1, reps = REP_RANGE_BOTTOM)
        // Plate-loaded on both sides: the smallest jump is one small plate per side
        top.weightKg > 0 -> base.copy(weight = base.weight + weightStep(unitSystem) * (if (top.perSide) 2 else 1), reps = REP_RANGE_BOTTOM)
        else -> base.copy(reps = top.reps + 1) // bodyweight: just keep adding reps
    }
    return SetSuggestion(next = next, basedOn = base)
}

/** Smallest usual jump on a barbell: a pair of 1.25 kg or 2.5 lb plates. */
private fun weightStep(unitSystem: UnitSystem): Double = if (unitSystem == UnitSystem.IMPERIAL) 5.0 else 2.5

/**
 * How [set] compares with the matching set of the last session: more load wins over reps, so
 * "+1 plate" or "+2.5 kg" when the load changed, otherwise "+2 reps" / "−1 rep", or "Same".
 * Null when there's no comparable set (none last time, or a different kind of load).
 */
fun changeSinceLast(set: LoggedSet, previous: LoggedSet?, unitSystem: UnitSystem): String? {
    if (previous == null || set.isPlates != previous.isPlates || set.bodyweight != previous.bodyweight) return null
    val loadChange = set.load - previous.load
    val repChange = set.reps - previous.reps
    fun signed(value: String, positive: Boolean) = if (positive) "+$value" else "−$value"
    return when {
        loadChange > LOAD_EPSILON || loadChange < -LOAD_EPSILON -> {
            val amount = kotlin.math.abs(loadChange)
            val label = if (set.isPlates) platesLabel(amount.toInt()) else formatWeight(kgToDisplay(amount, unitSystem), unitSystem)
            signed(label, loadChange > 0)
        }
        repChange != 0 -> signed("${kotlin.math.abs(repChange)} ${if (kotlin.math.abs(repChange) == 1) "rep" else "reps"}", repChange > 0)
        else -> "Same"
    }
}
