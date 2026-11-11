package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.screens.settings.LengthUnit

/** Measurements are stored in centimetres; like weights, the unit only affects input and display. */
private const val CM_PER_INCH = 2.54

fun cmToDisplay(cm: Double, unit: LengthUnit): Double = if (unit == LengthUnit.INCHES) cm / CM_PER_INCH else cm

fun displayToCm(length: Double, unit: LengthUnit): Double = if (unit == LengthUnit.INCHES) length * CM_PER_INCH else length

/** A stored length in the user's unit, e.g. "40.5 cm". */
fun formatLength(cm: Double, unit: LengthUnit): String = "${formatWeightNumber(cmToDisplay(cm, unit))} ${unit.symbol}"

/** Height for onboarding: "176 cm", or feet and inches like "5′9″". */
fun formatHeight(cm: Int, unit: LengthUnit): String {
    if (unit == LengthUnit.CENTIMETRES) return "$cm cm"
    val inches = Math.round(cm / CM_PER_INCH).toInt()
    return "${inches / 12}′${inches % 12}″"
}

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
