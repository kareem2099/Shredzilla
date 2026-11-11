package com.FreeRave.shredzilla.badges

import com.FreeRave.shredzilla.models.BodyMeasurement
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.PersonalRecord
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class BadgeCategory(val title: String) {
    EVENTS("Events"),
    MILESTONES("Milestones"),
    TRAINING("Training"),
    STRENGTH("Strength"),
    CONSISTENCY("Consistency"),
    BODY("Body"),
    COMMUNITY("Community"),
}

/** Rarity, shown as the ring color: bronze for first steps up to special for events and community. */
enum class BadgeTier { BRONZE, SILVER, GOLD, SPECIAL }

/** How a badge's progress reads: a count, or tenths of a kilogram shown in the user's unit. */
enum class ProgressFormat { COUNT, WEIGHT_KG }

/** Weight badges count in tenths of a kilogram, so 225 lb logged in pounds still reaches a 225 lb goal. */
fun kgToProgress(kg: Double): Int = Math.round(kg * 10).toInt()
fun progressToKg(progress: Int): Double = progress / 10.0

data class Badge(
    val id: String,
    val title: String,
    /** How to earn it, shown while it's locked and as a reminder once earned. */
    val description: String,
    val emoji: String,
    val category: BadgeCategory,
    val tier: BadgeTier,
    val goal: Int = 1,
    val format: ProgressFormat = ProgressFormat.COUNT,
    /** Events show their window; badges outside an event have none. */
    val startsAt: Long? = null,
    val endsAt: Long? = null,
)

/**
 * A limited-time badge defined in Remote Config ("badge_events"), so an event can be launched
 * without an app update. [goalType] is "workouts" (training days in the window), "sets" (working
 * sets in the window) or "open" (use the app during the window).
 */
data class BadgeEvent(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val startsAt: Long,
    val endsAt: Long,
    val goalType: String,
    val goal: Int,
)

/** All-time counters kept on the account, since the app only loads 90 days of sets. */
data class BadgeStats(val trainingDays: Int = 0, val sets: Int = 0, val records: Int = 0, val lastTrainingDay: String? = null)

/** Everything the badges are judged on. */
data class BadgeInput(
    val now: Long,
    val unitSystem: UnitSystem,
    val history: Map<String, List<LoggedSet>>,
    val workoutLogs: List<WorkoutLog>,
    val records: Map<String, PersonalRecord>,
    val measurements: List<BodyMeasurement>,
    val streakWeeks: Int,
    val weeklyGoalMet: Boolean,
    val deloadWeeks: Set<String>,
    val lists: List<UserExerciseList>,
    val ownsLifetime: Boolean,
    val supporterTips: Int,
    val accountCreatedMillis: Long?,
    val foundingCutoffMillis: Long,
    val stats: BadgeStats,
    val events: List<BadgeEvent>,
    /** Badges already on the account; badges earned by an action (not by data) only live here. */
    val earned: Map<String, Long>,
)

data class BadgeStatus(val badge: Badge, val current: Int, val earnedAtMillis: Long?) {
    val isEarned: Boolean get() = earnedAtMillis != null || current >= badge.goal
    val progress: Float get() = (current.toFloat() / badge.goal).coerceIn(0f, 1f)
}

object BadgeIds {
    const val SUPERSET = "superset"
    const val POCKET_LOGGER = "pocket_logger"
}

private const val KG_PER_LB = 0.45359237
private val DAY = TimeUnit.DAYS.toMillis(1)

/** The badges for [unit] (strength clubs use round numbers in each unit) plus the events. */
fun badgeCatalog(unit: UnitSystem, events: List<BadgeEvent>): List<Badge> {
    val clubs = if (unit == UnitSystem.IMPERIAL) listOf(135, 225, 315, 405) else listOf(60, 100, 140, 180)
    val clubSymbol = if (unit == UnitSystem.IMPERIAL) "lb" else "kg"
    val clubEmoji = listOf("🥉", "🥈", "🥇", "💎")
    val clubTier = listOf(BadgeTier.BRONZE, BadgeTier.SILVER, BadgeTier.GOLD, BadgeTier.SPECIAL)
    return buildList {
        events.forEach { e ->
            add(Badge("event_${e.id}", e.title, e.description, e.emoji, BadgeCategory.EVENTS, BadgeTier.SPECIAL, e.goal.coerceAtLeast(1), startsAt = e.startsAt, endsAt = e.endsAt))
        }
        add(Badge("first_set", "First rep", "Log your first set", "🏁", BadgeCategory.MILESTONES, BadgeTier.BRONZE))
        add(Badge("first_workout", "First workout", "Finish a workout in workout mode", "🎉", BadgeCategory.MILESTONES, BadgeTier.BRONZE))
        add(Badge("days_10", "Ten strong", "Train on 10 different days", "🔟", BadgeCategory.MILESTONES, BadgeTier.BRONZE, 10))
        add(Badge("days_50", "Fifty", "Train on 50 different days", "💪", BadgeCategory.MILESTONES, BadgeTier.SILVER, 50))
        add(Badge("days_100", "Centurion", "Train on 100 different days", "🏛️", BadgeCategory.MILESTONES, BadgeTier.GOLD, 100))
        add(Badge("sets_500", "Brick by brick", "Log 500 working sets", "🧱", BadgeCategory.MILESTONES, BadgeTier.SILVER, 500))
        add(Badge("sets_2500", "Built different", "Log 2,500 working sets", "🏗️", BadgeCategory.MILESTONES, BadgeTier.GOLD, 2500))

        add(Badge("split_starter", "Split starter", "Add a ready-made split from Programs", "🗂️", BadgeCategory.TRAINING, BadgeTier.BRONZE))
        add(Badge("split_week", "Full split", "Finish every day of a split in workout mode within a week", "🔁", BadgeCategory.TRAINING, BadgeTier.SILVER))
        add(Badge(BadgeIds.SUPERSET, "Superset", "Link two exercises into a superset", "🔗", BadgeCategory.TRAINING, BadgeTier.BRONZE))
        add(Badge("one_side", "Left, right", "Log a set one side at a time", "↔️", BadgeCategory.TRAINING, BadgeTier.BRONZE))
        add(Badge("tempo", "Slow and controlled", "Log a set with a tempo", "🐢", BadgeCategory.TRAINING, BadgeTier.BRONZE))
        add(Badge("weighted_bw", "Weighted", "Add weight to a bodyweight exercise", "🎒", BadgeCategory.TRAINING, BadgeTier.SILVER))
        add(Badge(BadgeIds.POCKET_LOGGER, "Pocket logger", "Log a set from the workout notification", "📲", BadgeCategory.TRAINING, BadgeTier.BRONZE))
        add(Badge("early_bird", "Early bird", "Log a set before 7 AM", "🌅", BadgeCategory.TRAINING, BadgeTier.SILVER))
        add(Badge("night_owl", "Night owl", "Log a set after 10 PM", "🦉", BadgeCategory.TRAINING, BadgeTier.SILVER))

        add(Badge("first_pr", "New best", "Beat a personal record", "🏆", BadgeCategory.STRENGTH, BadgeTier.BRONZE))
        add(Badge("prs_25", "Record breaker", "Beat 25 personal records", "📈", BadgeCategory.STRENGTH, BadgeTier.GOLD, 25))
        clubs.forEachIndexed { i, amount ->
            val kg = if (unit == UnitSystem.IMPERIAL) amount * KG_PER_LB else amount.toDouble()
            add(
                Badge(
                    "club_${i + 1}", "$amount $clubSymbol club", "Lift $amount $clubSymbol in a single set",
                    clubEmoji[i], BadgeCategory.STRENGTH, clubTier[i], Math.floor(kg * 10).toInt(), ProgressFormat.WEIGHT_KG
                )
            )
        }
        add(Badge("ten_tonnes", "Ten tonnes", "Move 10,000 kg in one day", "🚛", BadgeCategory.STRENGTH, BadgeTier.GOLD, kgToProgress(10_000.0), ProgressFormat.WEIGHT_KG))

        add(Badge("goal_week", "Goal getter", "Hit your weekly goal", "🎯", BadgeCategory.CONSISTENCY, BadgeTier.BRONZE))
        add(Badge("streak_4", "On a roll", "Hit your weekly goal 4 weeks in a row", "🔥", BadgeCategory.CONSISTENCY, BadgeTier.SILVER, 4))
        add(Badge("streak_12", "Unstoppable", "Hit your weekly goal 12 weeks in a row", "⚡", BadgeCategory.CONSISTENCY, BadgeTier.GOLD, 12))
        add(Badge("streak_26", "Half a year", "Hit your weekly goal 26 weeks in a row", "🌟", BadgeCategory.CONSISTENCY, BadgeTier.SPECIAL, 26))
        add(Badge("deload", "Smart recovery", "Take a deload week", "🧘", BadgeCategory.CONSISTENCY, BadgeTier.SILVER))

        add(Badge("first_measure", "Measured", "Log your first body measurements", "📏", BadgeCategory.BODY, BadgeTier.BRONZE))
        add(Badge("measure_10", "Tracker", "Log 10 measurement sessions", "📐", BadgeCategory.BODY, BadgeTier.SILVER, 10))
        add(Badge("measure_90", "Three months on record", "Keep measuring for 90 days", "🗓️", BadgeCategory.BODY, BadgeTier.GOLD, 90))

        add(Badge("founding", "Founding member", "Joined DotShredzilla in its first months", "⭐", BadgeCategory.COMMUNITY, BadgeTier.SPECIAL))
        add(Badge("pro_life", "Pro for life", "Own DotShredzilla Pro for life", "👑", BadgeCategory.COMMUNITY, BadgeTier.GOLD))
        add(Badge("supporter_1", "Supporter", "Send a tip", "❤️", BadgeCategory.COMMUNITY, BadgeTier.BRONZE))
        add(Badge("supporter_3", "Big supporter", "Send 3 tips", "💜", BadgeCategory.COMMUNITY, BadgeTier.SILVER, 3))
        add(Badge("supporter_10", "Patron", "Send 10 tips", "💖", BadgeCategory.COMMUNITY, BadgeTier.SPECIAL, 10))
    }
}

/** Where every badge stands. Earned badges stay earned even when the data that earned them is gone. */
fun evaluateBadges(input: BadgeInput): List<BadgeStatus> {
    val working = input.history.values.flatten().filter { it.setType != SetType.WARMUP }
    val trainingDays = working.map { it.dateKey }.toSet()
    val dayVolume = working.groupBy { it.dateKey }.values.maxOfOrNull { day -> day.sumOf { it.volumeKg } } ?: 0.0
    val logVolume = input.workoutLogs.maxOfOrNull { it.volumeKg } ?: 0.0
    val heaviestKg = maxOf(
        input.records.values.maxOfOrNull { it.heaviest?.load ?: 0.0 } ?: 0.0,
        working.filter { it.plates == null && !it.bodyweight }.maxOfOrNull { it.weightKg } ?: 0.0,
    )
    val hours = working.map { hourOf(it.timestampMillis) }
    val measureSpanDays = input.measurements.takeIf { it.size >= 2 }
        ?.let { ms -> ((ms.maxOf { it.takenAtMillis } - ms.minOf { it.takenAtMillis }) / DAY).toInt() } ?: 0

    fun has(condition: Boolean) = if (condition) 1 else 0

    val progress: Map<String, Int> = buildMap {
        put("first_set", has(working.isNotEmpty() || input.stats.sets > 0))
        put("first_workout", has(input.workoutLogs.isNotEmpty()))
        val days = maxOf(input.stats.trainingDays, trainingDays.size)
        put("days_10", days); put("days_50", days); put("days_100", days)
        val sets = maxOf(input.stats.sets, working.size)
        put("sets_500", sets); put("sets_2500", sets)
        put("split_starter", has(input.lists.any { " · " in it.name }))
        put("split_week", has(fullSplitInAWeek(input.workoutLogs, input.lists)))
        put("one_side", has(working.any { it.side != null }))
        put("tempo", has(working.any { it.tempo.isSet }))
        put("weighted_bw", has(working.any { it.bodyweight && it.weightKg > 0 }))
        put("early_bird", has(hours.any { it < 7 }))
        put("night_owl", has(hours.any { it >= 22 }))
        put("first_pr", has(input.stats.records > 0))
        put("prs_25", input.stats.records)
        (1..4).forEach { put("club_$it", kgToProgress(heaviestKg)) }
        put("ten_tonnes", kgToProgress(maxOf(dayVolume, logVolume)))
        put("goal_week", has(input.weeklyGoalMet || input.streakWeeks > 0))
        put("streak_4", input.streakWeeks); put("streak_12", input.streakWeeks); put("streak_26", input.streakWeeks)
        put("deload", has(input.deloadWeeks.isNotEmpty()))
        put("first_measure", has(input.measurements.isNotEmpty()))
        put("measure_10", input.measurements.size)
        put("measure_90", measureSpanDays)
        put("founding", has(input.accountCreatedMillis != null && input.accountCreatedMillis <= input.foundingCutoffMillis))
        put("pro_life", has(input.ownsLifetime))
        put("supporter_1", input.supporterTips); put("supporter_3", input.supporterTips); put("supporter_10", input.supporterTips)
        input.events.forEach { e -> put("event_${e.id}", eventProgress(e, working, input.now)) }
    }

    return badgeCatalog(input.unitSystem, input.events).map { badge ->
        BadgeStatus(badge, current = progress[badge.id] ?: 0, earnedAtMillis = input.earned[badge.id])
    }
}

/** Progress inside the event window only; "open" is earned by using the app during it. */
private fun eventProgress(e: BadgeEvent, working: List<LoggedSet>, now: Long): Int {
    val inWindow = working.filter { it.timestampMillis in e.startsAt..e.endsAt }
    return when (e.goalType) {
        "sets" -> inWindow.size
        "open" -> if (now in e.startsAt..e.endsAt) e.goal else 0
        else -> inWindow.map { it.dateKey }.toSet().size
    }
}

/** Every day of one split ("Split · Back", "Split · Legs"…) finished in workout mode within seven days. */
internal fun fullSplitInAWeek(logs: List<WorkoutLog>, lists: List<UserExerciseList>): Boolean {
    val splits = lists.map { it.name }.filter { " · " in it }.groupBy { it.substringBefore(" · ") }.filterValues { it.size >= 2 }
    return splits.any { (prefix, days) ->
        val runs = logs.filter { it.listName.startsWith("$prefix · ") }.sortedBy { it.startedAtMillis }
        runs.any { start ->
            runs.filter { it.startedAtMillis in start.startedAtMillis until start.startedAtMillis + 7 * DAY }
                .map { it.listName }.toSet().containsAll(days)
        }
    }
}

private fun hourOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
