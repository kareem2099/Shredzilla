package com.FreeRave.shredzilla.utils

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
