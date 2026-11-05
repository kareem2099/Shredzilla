package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.SetLoad
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import java.util.Locale

/** Firestore always stores weights in kilograms; the user's unit only affects input and display. */
const val KG_PER_LB = 0.45359237

fun UnitSystem.weightUnit(): String = if (this == UnitSystem.IMPERIAL) "lbs" else "kg"

/** Stored kilograms → the number the user expects to see in their unit. */
fun kgToDisplay(weightKg: Double, unitSystem: UnitSystem): Double =
    if (unitSystem == UnitSystem.IMPERIAL) weightKg / KG_PER_LB else weightKg

/** Inverse of [kgToDisplay], for saving what the user typed. */
fun displayToKg(weight: Double, unitSystem: UnitSystem): Double =
    if (unitSystem == UnitSystem.IMPERIAL) weight * KG_PER_LB else weight

/** A weight already in the user's unit, e.g. a value they're typing: 35.0 → "35 kg". */
fun formatWeight(weight: Double, unitSystem: UnitSystem): String = "${formatWeightNumber(weight)} ${unitSystem.weightUnit()}"

/** A weight as stored in Firestore (kg), converted and labelled for the user's unit. */
fun formatStoredWeight(weightKg: Double, unitSystem: UnitSystem): String =
    formatWeight(kgToDisplay(weightKg, unitSystem), unitSystem)

/** Number part only: whole values without decimals, otherwise one decimal ("2.5"). */
fun formatWeightNumber(weight: Double): String {
    // Round first: kg↔lbs round-trips leave noise like 99.99999 that should read "100"
    val rounded = Math.round(weight * 10) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else String.format(Locale.US, "%.1f", rounded)
}

fun platesLabel(plates: Int): String = if (plates == 1) "1 plate" else "$plates plates"

/** The load of a stored set: "6 plates" on a weight stack, otherwise its weight in the user's unit. */
fun formatSetLoad(weightKg: Double, plates: Int?, unitSystem: UnitSystem): String =
    plates?.let(::platesLabel) ?: formatStoredWeight(weightKg, unitSystem)

/** A logged set as sheet input, e.g. to edit it or to start the next set from it. */
fun LoggedSet.toEntry(unitSystem: UnitSystem): SetEntry =
    SetEntry(reps = reps, weight = kgToDisplay(weightKg, unitSystem), plates = plates, notes = notes)

/** A set as typed (user's unit), e.g. "60 kg × 10", "6 plates × 12", or "15 reps" for bodyweight. */
fun formatEntry(entry: SetEntry, unitSystem: UnitSystem): String = when {
    entry.plates != null -> "${platesLabel(entry.plates)} × ${entry.reps}"
    entry.weight > 0 -> "${formatWeight(entry.weight, unitSystem)} × ${entry.reps}"
    else -> "${entry.reps} reps"
}

/** A stored set's load and reps, e.g. "60 kg × 10" or "6 plates × 12". */
fun formatLoad(load: SetLoad, unitSystem: UnitSystem): String =
    "${formatSetLoad(load.weightKg, load.plates, unitSystem)} × ${load.reps}"
