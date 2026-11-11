package com.FreeRave.shredzilla.utils

import com.FreeRave.shredzilla.models.BodyMeasurement
import com.FreeRave.shredzilla.models.MeasurementSite
import com.FreeRave.shredzilla.models.MeasurementSlot
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.screens.settings.LengthUnit
import kotlin.math.abs

private const val WEIGHT_NOISE_KG = 0.5
private const val LENGTH_NOISE_CM = 0.5
private const val IMBALANCE_WORTH_MENTIONING_CM = 2.0

/** Good = the change people usually train for (bigger arms, smaller waist); null = neither. */
enum class InsightTone { GOOD, WATCH, NEUTRAL }

data class BodyChange(val label: String, val change: String, val tone: InsightTone)

/** What changed and what it likely means, from the user's own measurements. */
data class BodyInsights(
    val verdict: String?,
    val changes: List<BodyChange>,
    val sinceFirst: List<BodyChange>,
    val firstDateMillis: Long?,
    val imbalance: String?
)

/** Average of the sides measured for [site] in [m], e.g. both arms. */
private fun siteAverage(m: BodyMeasurement, site: MeasurementSite): Double? =
    site.slots.mapNotNull { m.valueOf(it) }.takeIf { it.isNotEmpty() }?.average()

/** Sites summarised in insights; growth is the goal everywhere except the waist. */
private val TrackedSites = listOf(
    MeasurementSite.WAIST, MeasurementSite.CHEST, MeasurementSite.ARM_FLEXED, MeasurementSite.FOREARM,
    MeasurementSite.THIGH, MeasurementSite.CALF
)

private fun changes(from: BodyMeasurement, to: BodyMeasurement, unitSystem: UnitSystem, lengthUnit: LengthUnit): List<BodyChange> = buildList {
    val fromKg = from.bodyWeightKg
    val toKg = to.bodyWeightKg
    if (fromKg != null && toKg != null && abs(toKg - fromKg) >= WEIGHT_NOISE_KG) {
        val diff = kgToDisplay(toKg - fromKg, unitSystem)
        add(BodyChange("Weight", "${if (diff > 0) "+" else "−"}${formatWeight(abs(diff), unitSystem)}", InsightTone.NEUTRAL))
    }
    TrackedSites.forEach { site ->
        val before = siteAverage(from, site) ?: return@forEach
        val after = siteAverage(to, site) ?: return@forEach
        val diffCm = after - before
        if (abs(diffCm) < LENGTH_NOISE_CM) return@forEach
        val grewIsGood = site != MeasurementSite.WAIST
        add(
            BodyChange(
                label = site.label.substringBefore(" ("),
                change = "${if (diffCm > 0) "+" else "−"}${formatLength(abs(diffCm), lengthUnit)}",
                tone = if ((diffCm > 0) == grewIsGood) InsightTone.GOOD else InsightTone.WATCH
            )
        )
    }
}

/** Reads weight against waist (and arms): the usual signs of lean gain, fat gain or fat loss. */
private fun verdictOf(from: BodyMeasurement, to: BodyMeasurement): String? {
    val weight = to.bodyWeightKg?.let { t -> from.bodyWeightKg?.let { t - it } }
    val waist = siteAverage(to, MeasurementSite.WAIST)?.let { t -> siteAverage(from, MeasurementSite.WAIST)?.let { t - it } }
    val arms = siteAverage(to, MeasurementSite.ARM_FLEXED)?.let { t -> siteAverage(from, MeasurementSite.ARM_FLEXED)?.let { t - it } }
    val weightUp = weight != null && weight >= WEIGHT_NOISE_KG
    val weightDown = weight != null && weight <= -WEIGHT_NOISE_KG
    val waistUp = waist != null && waist >= LENGTH_NOISE_CM
    val waistDown = waist != null && waist <= -LENGTH_NOISE_CM
    val armsUp = arms != null && arms >= LENGTH_NOISE_CM
    val armsDown = arms != null && arms <= -LENGTH_NOISE_CM
    return when {
        weightUp && waist != null && !waistUp -> "Lean gain: weight went up while the waist held or shrank. That's mostly muscle."
        weightUp && waistUp -> "Weight and waist both went up, so part of the gain is fat. If that's not the plan, trim calories a little."
        weightDown && waistDown -> "Fat loss: weight and waist are both down."
        weightDown && armsDown -> "Weight dropped along with arm size. Keep protein high and keep training heavy to hold on to muscle."
        !weightUp && !weightDown && waistDown && armsUp -> "Recomposition: about the same weight, a smaller waist and bigger arms."
        else -> null
    }
}

/** The biggest right-vs-left gap worth acting on, e.g. "Right thigh is 5 cm bigger than the left". */
private fun imbalanceOf(latest: BodyMeasurement, lengthUnit: LengthUnit): String? =
    MeasurementSite.entries.filter { it.sided }.mapNotNull { site ->
        val right = latest.valueOf(MeasurementSlot(site, Side.RIGHT)) ?: return@mapNotNull null
        val left = latest.valueOf(MeasurementSlot(site, Side.LEFT)) ?: return@mapNotNull null
        Triple(site, right - left, abs(right - left))
    }.filter { it.third >= IMBALANCE_WORTH_MENTIONING_CM }.maxByOrNull { it.third }?.let { (site, diff, gap) ->
        val bigger = if (diff > 0) "Right" else "Left"
        val smaller = if (diff > 0) "left" else "right"
        "$bigger ${site.label.lowercase()} is ${formatLength(gap, lengthUnit)} bigger than the $smaller. " +
            "Train it one side at a time, start with the $smaller side and match its reps on the other."
    }

/** [measurements] newest first. Needs two sessions for changes; the imbalance works from one. */
fun bodyInsights(measurements: List<BodyMeasurement>, unitSystem: UnitSystem, lengthUnit: LengthUnit): BodyInsights? {
    val latest = measurements.firstOrNull() ?: return null
    val previous = measurements.getOrNull(1)
    val first = measurements.last().takeIf { measurements.size > 2 }
    return BodyInsights(
        verdict = previous?.let { verdictOf(it, latest) },
        changes = previous?.let { changes(it, latest, unitSystem, lengthUnit) }.orEmpty(),
        sinceFirst = first?.let { changes(it, latest, unitSystem, lengthUnit) }.orEmpty(),
        firstDateMillis = first?.takenAtMillis,
        imbalance = imbalanceOf(latest, lengthUnit)
    )
}
