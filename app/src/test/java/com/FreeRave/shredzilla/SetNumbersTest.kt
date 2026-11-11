package com.FreeRave.shredzilla

import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.models.completedSetCount
import com.FreeRave.shredzilla.models.workingSetCount
import com.FreeRave.shredzilla.models.movedKgOf
import com.FreeRave.shredzilla.models.setNumbers
import org.junit.Assert.assertEquals
import org.junit.Test

class SetNumbersTest {
    private data class S(val side: Side? = null, val warmup: Boolean = false)

    private fun numbers(vararg sets: S) = setNumbers(sets.toList(), { it.side }, { it.warmup })

    @Test fun `plain sets count up`() = assertEquals(listOf(1, 2, 3), numbers(S(), S(), S()))

    @Test fun `warm-ups are not numbered`() = assertEquals(listOf(0, 1, 2), numbers(S(warmup = true), S(), S()))

    @Test fun `left and right share a number`() =
        assertEquals(listOf(1, 1, 2, 2, 3), numbers(S(Side.LEFT), S(Side.RIGHT), S(Side.LEFT), S(Side.RIGHT), S(Side.LEFT)))

    @Test fun `both-sided sets continue after a pair`() =
        assertEquals(listOf(1, 1, 2, 3), numbers(S(Side.LEFT), S(Side.RIGHT), S(), S()))

    @Test fun `one-sided per-side set moves one side's load`() {
        assertEquals(30.0, movedKgOf(60.0, perSide = true, side = Side.LEFT), 0.0)
        assertEquals(60.0, movedKgOf(60.0, perSide = true, side = null), 0.0)
        assertEquals(15.0, movedKgOf(15.0, perSide = false, side = Side.RIGHT), 0.0)
    }

    private fun logged(side: Side?, warmup: Boolean = false) =
        LoggedSet("id", "2026-11-08", "wide_grip_row", 10, 60.0, "", 0L, side = side, setType = if (warmup) SetType.WARMUP else SetType.NORMAL)

    @Test fun `a set is finished only when both sides are in`() {
        val lrlrl = listOf(Side.LEFT, Side.RIGHT, Side.LEFT, Side.RIGHT, Side.LEFT).map { logged(it) }
        assertEquals(2, lrlrl.completedSetCount())
        assertEquals(3, lrlrl.workingSetCount())
        assertEquals(3, (lrlrl + logged(Side.RIGHT)).completedSetCount())
        assertEquals(2, listOf(logged(null), logged(null), logged(null, warmup = true)).completedSetCount())
    }
}
