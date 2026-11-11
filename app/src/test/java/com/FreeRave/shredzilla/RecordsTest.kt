package com.FreeRave.shredzilla

import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.utils.RecordCheck
import com.FreeRave.shredzilla.utils.checkRecord
import com.FreeRave.shredzilla.utils.personalRecordIds
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordsTest {
    private var t = 0L
    private fun plates(id: String, day: String, plates: Int, reps: Int, warmup: Boolean = false) =
        LoggedSet(id, day, "lat_pulldown", reps, 0.0, "", ++t, plates = plates, setType = if (warmup) SetType.WARMUP else SetType.NORMAL)

    // Today's Lat Pulldown after an earlier best of 8 plates × 12
    private val history = listOf(
        plates("old", "2026-11-01", 8, 12),
        plates("w", "2026-11-08", 6, 12, warmup = true),
        plates("s1", "2026-11-08", 8, 14),
        plates("s2", "2026-11-08", 9, 7),
        plates("s3", "2026-11-08", 9, 8),
    )

    @Test fun `the trophy goes on the day's best set`() = assertEquals(setOf("s3"), personalRecordIds(history))

    @Test fun `more reps at the same load is a record`() {
        val before = history.dropLast(1)
        assertEquals(RecordCheck.BEATEN, checkRecord(before, null, SetEntry(reps = 8, plates = 9), UnitSystem.METRIC))
    }

    @Test fun `fewer reps at the same load is not`() =
        assertEquals(RecordCheck.NONE, checkRecord(history, null, SetEntry(reps = 6, plates = 9), UnitSystem.METRIC))

    @Test fun `a lighter set with more reps is not`() =
        assertEquals(RecordCheck.NONE, checkRecord(history, null, SetEntry(reps = 20, plates = 8), UnitSystem.METRIC))
}
