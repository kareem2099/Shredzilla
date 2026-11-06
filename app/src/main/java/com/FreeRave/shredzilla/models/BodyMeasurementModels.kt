package com.FreeRave.shredzilla.models

/** A body site the user can measure; [sided] sites take a right and a left value. */
enum class MeasurementSite(val key: String, val label: String, val sided: Boolean) {
    ARM_FLEXED("arm_flexed", "Arm (flexed)", true),
    ARM_RELAXED("arm_relaxed", "Arm (relaxed)", true),
    FOREARM("forearm", "Forearm", true),
    FOREARM_FLEXED("forearm_flexed", "Forearm (flexed)", true),
    THIGH("thigh", "Thigh", true),
    CALF("calf", "Calf", true),
    CHEST("chest", "Chest", false),
    WAIST("waist", "Waist", false),
    WAIST_VACUUM("waist_vacuum", "Waist (vacuum)", false);

    /** Right then left for sided sites, a single slot otherwise. */
    val slots: List<MeasurementSlot>
        get() = if (sided) listOf(MeasurementSlot(this, Side.RIGHT), MeasurementSlot(this, Side.LEFT)) else listOf(MeasurementSlot(this, null))
}

/** One value of a measurement: a site, plus the side for sided sites. [key] is how it's stored, e.g. "thigh_right". */
data class MeasurementSlot(val site: MeasurementSite, val side: Side?) {
    val key: String get() = side?.let { "${site.key}_${it.storedValue}" } ?: site.key
}

/** One measuring session, saved under users/{uid}/measurements. Lengths in cm, weight in kg; missing = not measured. */
data class BodyMeasurement(
    val id: String = "",
    val takenAtMillis: Long,
    val valuesCm: Map<String, Double>,
    val bodyWeightKg: Double? = null,
    val note: String = ""
) {
    fun valueOf(slot: MeasurementSlot): Double? = valuesCm[slot.key]
}

/** How often the user plans to measure; daily numbers mostly show water and food, not progress. */
enum class MeasurementCadence(val label: String, val days: Int, private val stored: String) {
    WEEKLY("Weekly", 7, "weekly"),
    MONTHLY("Monthly", 30, "monthly");

    val storedValue: String get() = stored

    companion object {
        fun fromStored(value: String?): MeasurementCadence = entries.find { it.stored == value } ?: WEEKLY
    }
}
