package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.screens.settings.UnitSystem

private const val EPSILON = 1e-6

/** Plates a normal gym has, heaviest first. */
private val KgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
private val LbPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

/** Olympic, women's/technique and short bars. */
fun barWeights(unitSystem: UnitSystem): List<Double> =
    if (unitSystem == UnitSystem.IMPERIAL) listOf(45.0, 35.0, 15.0) else listOf(20.0, 15.0, 10.0)

/** [perSide] heaviest first; [leftover] is what standard plates can't make up (both sides together). */
data class PlateLoad(val perSide: List<Double>, val leftover: Double)

/** Plates for each side of a [barWeight] bar to reach [total], greedily from the heaviest. Null below the bar. */
fun platesPerSide(total: Double, barWeight: Double, unitSystem: UnitSystem): PlateLoad? {
    if (total < barWeight - EPSILON) return null
    var side = (total - barWeight) / 2
    val plates = mutableListOf<Double>()
    for (plate in if (unitSystem == UnitSystem.IMPERIAL) LbPlates else KgPlates) {
        while (side >= plate - EPSILON) {
            plates += plate
            side -= plate
        }
    }
    return PlateLoad(plates, (side * 2).coerceAtLeast(0.0))
}
