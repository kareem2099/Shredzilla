package com.FreeRave.shredzilla

import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.navigation.nextListAfter
import com.FreeRave.shredzilla.notifications.NotificationChoices
import com.FreeRave.shredzilla.notifications.Reminder
import com.FreeRave.shredzilla.notifications.ReminderRules
import com.FreeRave.shredzilla.notifications.SentLog
import com.FreeRave.shredzilla.notifications.TrainingSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class ReminderRulesTest {
    private val sat = DayOfWeek.SATURDAY // week starts Saturday, like the user's phone
    private val sunday = LocalDate.of(2026, 11, 8)
    private val on = NotificationChoices(reminderDays = 3)

    @Test fun `inactivity waits for the chosen days`() {
        val snap = TrainingSnapshot(trainingDays = setOf("2026-11-06"))
        assertNull(ReminderRules.inactivity(snap, on, SentLog(), sunday)) // 2 days
        assertEquals(3, ReminderRules.inactivity(snap, on, SentLog(), sunday.plusDays(1))?.daysSince)
    }

    @Test fun `inactivity repeats every N days, not daily`() {
        val snap = TrainingSnapshot(trainingDays = setOf("2026-11-01"))
        val sent = SentLog(inactivityDay = "2026-11-04")
        assertNull(ReminderRules.inactivity(snap, on, sent, LocalDate.of(2026, 11, 6)))
        assertEquals(6, ReminderRules.inactivity(snap, on, sent, LocalDate.of(2026, 11, 7))?.daysSince)
    }

    @Test fun `inactivity is off when the reminder is off`() =
        assertNull(ReminderRules.inactivity(TrainingSnapshot(trainingDays = setOf("2026-10-01")), NotificationChoices(), SentLog(), sunday))

    @Test fun `goal alert on the day every remaining day is needed`() {
        // Week Sat 7 – Fri 13, goal 3, one workout so far. Thursday: 2 days left, 2 missing
        val snap = TrainingSnapshot(trainingDays = setOf("2026-11-07"), weeklyGoal = 3)
        assertNull(ReminderRules.goalAtRisk(snap, on, SentLog(), LocalDate.of(2026, 11, 11), sat))
        assertEquals(Reminder.GoalAtRisk(2, 2), ReminderRules.goalAtRisk(snap, on, SentLog(), LocalDate.of(2026, 11, 12), sat))
    }

    @Test fun `no goal alert after training today, in a deload week, or twice a week`() {
        val thursday = LocalDate.of(2026, 11, 12)
        val base = TrainingSnapshot(trainingDays = setOf("2026-11-07"), weeklyGoal = 3)
        assertNull(ReminderRules.goalAtRisk(base.copy(trainingDays = base.trainingDays + "2026-11-12"), on, SentLog(), thursday, sat))
        assertNull(ReminderRules.goalAtRisk(base.copy(deloadWeeks = setOf("2026-11-07")), on, SentLog(), thursday, sat))
        assertNull(ReminderRules.goalAtRisk(base, on, SentLog(goalWeek = "2026-11-07"), thursday, sat))
    }

    @Test fun `measurement reminder follows the rhythm`() {
        val taken = LocalDate.of(2026, 11, 1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val snap = TrainingSnapshot(lastMeasurementMillis = taken, measurementCadenceDays = 7)
        assertNull(ReminderRules.measurementDue(snap, on, SentLog(), LocalDate.of(2026, 11, 7)))
        assertEquals(7, ReminderRules.measurementDue(snap, on, SentLog(), sunday)?.daysSince)
        assertNull(ReminderRules.measurementDue(snap, on, SentLog(measurementDay = "2026-11-08"), LocalDate.of(2026, 11, 10)))
    }

    @Test fun `next list is the next day of the split`() {
        val lists = listOf("Split · Chest", "Split · Back", "Split · Legs", "Arms").mapIndexed { i, n -> UserExerciseList("$i", n, emptyList()) }
        assertEquals("Split · Legs", nextListAfter("Split · Back", lists)?.name)
        assertEquals("Split · Chest", nextListAfter("Split · Legs", lists)?.name)
        assertEquals("Arms", nextListAfter("Arms", lists)?.name)
        assertEquals("Split · Chest", nextListAfter(null, lists)?.name)
    }
}
