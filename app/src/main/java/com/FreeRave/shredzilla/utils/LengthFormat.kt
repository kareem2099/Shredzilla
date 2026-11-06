package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.screens.settings.UnitSystem

/** Measurements are stored in centimetres; like weights, the unit only affects input and display. */
private const val CM_PER_INCH = 2.54

fun UnitSystem.lengthUnit(): String = if (this == UnitSystem.IMPERIAL) "in" else "cm"

fun cmToDisplay(cm: Double, unitSystem: UnitSystem): Double = if (unitSystem == UnitSystem.IMPERIAL) cm / CM_PER_INCH else cm

fun displayToCm(length: Double, unitSystem: UnitSystem): Double = if (unitSystem == UnitSystem.IMPERIAL) length * CM_PER_INCH else length

/** A stored length in the user's unit, e.g. "40.5 cm". */
fun formatLength(cm: Double, unitSystem: UnitSystem): String =
    "${formatWeightNumber(cmToDisplay(cm, unitSystem))} ${unitSystem.lengthUnit()}"

/**
 * Typed number to a Double, accepting Arabic-Indic digits and a comma or Arabic decimal mark,
 * since an Arabic keyboard types "٤٠٫٥" for 40.5.
 */
fun parseDecimalInput(text: String): Double? {
    val western = text.trim().map { char ->
        when (char) {
            in '٠'..'٩' -> '0' + (char - '٠')
            in '۰'..'۹' -> '0' + (char - '۰')
            ',', '٫' -> '.'
            else -> char
        }
    }.joinToString("")
    return western.toDoubleOrNull()?.takeIf { it > 0 }
}
