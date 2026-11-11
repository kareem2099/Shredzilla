package com.FreeRave.shredzilla

import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.workout.parseSetInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetInputTest {
    private val plates = SetEntry(reps = 7, plates = 9)
    private val perSide = SetEntry(reps = 8, weight = 70.0, perSide = true)

    @Test fun `one number changes only the reps`() = assertEquals(plates.copy(reps = 8), parseSetInput("8", plates))

    @Test fun `two numbers set load and reps`() {
        assertEquals(plates.copy(plates = 10, reps = 6), parseSetInput("10 6", plates))
        assertEquals(plates.copy(plates = 10, reps = 6), parseSetInput("10x6", plates))
        assertEquals(plates.copy(plates = 10, reps = 6), parseSetInput("10 × 6", plates))
    }

    @Test fun `per-side load is typed per side`() = assertEquals(perSide.copy(weight = 80.0, reps = 10), parseSetInput("40 10", perSide))

    @Test fun `arabic digits and decimals`() {
        assertEquals(plates.copy(reps = 12), parseSetInput("١٢", plates))
        assertEquals(SetEntry(reps = 8, weight = 62.5), parseSetInput("62.5 8", SetEntry(reps = 5, weight = 60.0)))
    }

    @Test fun `first set ever needs load and reps`() {
        assertNull(parseSetInput("8", null))
        assertEquals(SetEntry(reps = 10, weight = 60.0), parseSetInput("60 10", null))
    }

    @Test fun `nonsense is rejected`() {
        assertNull(parseSetInput("hello", plates))
        assertNull(parseSetInput("0", plates))
    }
}
