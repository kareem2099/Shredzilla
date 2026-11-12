package com.FreeRave.shredzilla

import com.FreeRave.shredzilla.badges.BadgeEvent
import com.FreeRave.shredzilla.badges.BadgeInput
import com.FreeRave.shredzilla.badges.BadgeStats
import com.FreeRave.shredzilla.badges.evaluateBadges
import com.FreeRave.shredzilla.badges.fullSplitInAWeek
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.PersonalRecord
import com.FreeRave.shredzilla.models.RecordSet
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_800_000_000_000L

    private fun input(
        history: Map<String, List<LoggedSet>> = emptyMap(),
        records: Map<String, PersonalRecord> = emptyMap(),
        stats: BadgeStats = BadgeStats(),
        unit: UnitSystem = UnitSystem.METRIC,
        events: List<BadgeEvent> = emptyList(),
        founding: Int? = null,
        earned: Map<String, Long> = emptyMap(),
    ) = BadgeInput(
        now = now, unitSystem = unit, history = history, workoutLogs = emptyList(), records = records,
        measurements = emptyList(), streakWeeks = 0, weeklyGoalMet = false, deloadWeeks = emptySet(), lists = emptyList(),
        ownsLifetime = false, supporterTips = 0, foundingNumber = founding,
        stats = stats, events = events, earned = earned,
    )

    private fun set(day: String, at: Long, kg: Double = 50.0, side: Side? = null) = LoggedSet("d$at", day, "row", 10, kg, "", at, side = side)
    private fun status(id: String, i: BadgeInput) = evaluateBadges(i).first { it.badge.id == id }

    @Test fun `nothing logged earns nothing but founding`() {
        val all = evaluateBadges(input(founding = 7))
        assertEquals(listOf("founding"), all.filter { it.isEarned }.map { it.badge.id })
        assertEquals("Founding member #7", status("founding", input(founding = 7)).badge.title)
    }

    @Test fun `founding needs the server number, a saved badge alone doesn't count`() {
        val saved = input(earned = mapOf("founding" to now - day))
        assertFalse(status("founding", saved).isEarned)
        assertEquals("Founding member", status("founding", saved).badge.title)
    }

    @Test fun `first set and one side`() {
        val i = input(history = mapOf("row" to listOf(set("2026-11-08", now, side = Side.LEFT))))
        assertTrue(status("first_set", i).isEarned)
        assertTrue(status("one_side", i).isEarned)
        assertFalse(status("tempo", i).isEarned)
    }

    @Test fun `training-day badges use the larger of counter and history`() {
        val i = input(stats = BadgeStats(trainingDays = 49))
        assertEquals(49, status("days_50", i).current)
        assertTrue(status("days_10", i).isEarned)
        assertFalse(status("days_50", i).isEarned)
    }

    @Test fun `strength clubs follow the unit`() {
        val records = mapOf("bench" to PersonalRecord("bench", "Bench", RecordSet(102.5, 5, now), null))
        assertTrue(status("club_2", input(records = records)).isEarned) // 100 kg club
        assertEquals("225 lb club", status("club_2", input(records = records, unit = UnitSystem.IMPERIAL)).badge.title)
        assertTrue(status("club_2", input(records = records, unit = UnitSystem.IMPERIAL)).isEarned) // 225 lb = 102.1 kg
        assertFalse(status("club_3", input(records = records)).isEarned)
        // 315 lb logged in pounds is stored as 142.88 kg and still makes the 315 lb club
        val lb315 = mapOf("squat" to PersonalRecord("squat", "Squat", RecordSet(315 * 0.45359237, 3, now), null))
        assertTrue(status("club_3", input(records = lb315, unit = UnitSystem.IMPERIAL)).isEarned)
    }

    @Test fun `events count only inside their window`() {
        val event = BadgeEvent("ramadan", "Ramadan Warrior", "🌙", "Train 2 days", now - 5 * day, now + 5 * day, "workouts", 2)
        val history = mapOf("row" to listOf(set("a", now - 10 * day), set("b", now - 2 * day), set("c", now - day)))
        val s = status("event_ramadan", input(history = history, events = listOf(event)))
        assertEquals(2, s.current)
        assertTrue(s.isEarned)
    }

    @Test fun `earned badges stay earned`() =
        assertTrue(status("first_set", input(earned = mapOf("first_set" to now - day))).isEarned)

    @Test fun `full split within a week`() {
        val lists = listOf("Split · Chest", "Split · Back", "Split · Legs").mapIndexed { i, n -> UserExerciseList("$i", n, emptyList()) }
        fun log(name: String, at: Long) = WorkoutLog(listName = name, startedAtMillis = at, durationMillis = 0, setCount = 1, volumeKg = 0.0, exercises = emptyList(), records = emptyList())
        assertTrue(fullSplitInAWeek(listOf(log("Split · Chest", 0), log("Split · Back", 2 * day), log("Split · Legs", 6 * day)), lists))
        assertFalse(fullSplitInAWeek(listOf(log("Split · Chest", 0), log("Split · Back", 2 * day), log("Split · Legs", 8 * day)), lists))
    }
}
