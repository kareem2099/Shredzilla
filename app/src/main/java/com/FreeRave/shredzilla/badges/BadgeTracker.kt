package com.FreeRave.shredzilla.badges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.FreeRave.shredzilla.composables.PrCelebration
import com.FreeRave.shredzilla.data.TrainingHistoryStore
import com.FreeRave.shredzilla.data.UserDataManager
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.pro.BillingRepository
import com.FreeRave.shredzilla.utils.dateKeyOf
import com.FreeRave.shredzilla.utils.personalRecordIds
import com.FreeRave.shredzilla.utils.weeklyProgress
import com.FreeRave.shredzilla.workout.WorkoutController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.ZoneId

/**
 * Judges the badges whenever the training data changes, saves newly earned ones to the account and
 * queues a banner for each. Badges already earned before this ran (or before the data finished
 * loading) are saved quietly, so opening the app never shows a burst of old badges.
 */
class BadgeTracker(
    private val scope: CoroutineScope,
    private val userDataManager: UserDataManager,
    private val trainingHistory: TrainingHistoryStore,
    private val billing: BillingRepository,
    private val workout: WorkoutController,
    private val history: StateFlow<Map<String, List<LoggedSet>>>,
) {
    /** The latest judgement, for the badges page. */
    var statuses by mutableStateOf<List<BadgeStatus>>(emptyList())
        private set
    private val queue = ArrayDeque<BadgeStatus>()
    /** The badge being celebrated, shown as a banner by the app. */
    var celebration by mutableStateOf<PrCelebration?>(null)
        private set

    private var quietUntil = 0L
    private var quietUser: String? = null

    init {
        active = WeakReference(this)
        @OptIn(FlowPreview::class)
        scope.launch {
            combine(snapshotFlow { inputsKey() }, history) { _, h -> h }
                .debounce(800)
                .collect { h -> judge(h) }
        }
        // Linking a superset is an action, not something the data shows later
        scope.launch {
            snapshotFlow { workout.session?.supersetStarts?.isNotEmpty() == true }
                .collect { linked -> if (linked) award(BadgeIds.SUPERSET) }
        }
        workout.onSetLogged = { entryIsWorking, beatRecord ->
            userDataManager.countForBadges(dateKeyOf(System.currentTimeMillis()), entryIsWorking, beatRecord, FirebaseAuth.getInstance().currentUser)
        }
    }

    /** Gives a badge earned by doing something (not visible in the data), with a banner. */
    fun award(id: String) {
        if (!userDataManager.badgesLoaded || id in userDataManager.earnedBadges) return
        val now = System.currentTimeMillis()
        userDataManager.saveEarnedBadges(mapOf(id to now), emptyMap(), FirebaseAuth.getInstance().currentUser)
        statuses.find { it.badge.id == id }?.let { celebrate(it) }
    }

    fun dismissCelebration() {
        celebration = queue.removeFirstOrNull()?.let(::bannerFor)
    }

    private fun inputsKey(): List<Any?> = listOf(
        userDataManager.badgesLoaded, userDataManager.earnedBadges, userDataManager.badgeStats, userDataManager.userUnitSystem,
        userDataManager.userWeeklyGoalDays, userDataManager.deloadWeeks, userDataManager.userExerciseLists, userDataManager.supporterTips,
        userDataManager.foundingNumber, trainingHistory.workoutLogs, trainingHistory.personalRecords, trainingHistory.measurements, billing.ownsProLifetime,
    )

    private fun judge(h: Map<String, List<LoggedSet>>) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!userDataManager.badgesLoaded) return
        val now = System.currentTimeMillis()
        if (quietUser != user.uid) {
            quietUser = user.uid
            quietUntil = now + QUIET_START_MS
        }

        // Accounts from before the counters: start them from what the loaded history shows
        val working = h.values.flatten().filter { it.setType != SetType.WARMUP }
        userDataManager.raiseBadgeStats(
            trainingDays = working.map { it.dateKey }.toSet().size,
            sets = working.size,
            records = h.values.sumOf { personalRecordIds(it).size },
            currentUser = user,
        )

        val trainingDays = working.map { it.dateKey }.toSet()
        val progress = weeklyProgress(trainingDays, userDataManager.userWeeklyGoalDays, userDataManager.deloadWeeks)
        val events = BadgeEventsConfig.events()
        val input = BadgeInput(
            now = now,
            unitSystem = userDataManager.userUnitSystem,
            history = h,
            workoutLogs = trainingHistory.workoutLogs,
            records = trainingHistory.personalRecords,
            measurements = trainingHistory.measurements,
            streakWeeks = progress.streakWeeks,
            weeklyGoalMet = progress.goal != null && progress.workoutsThisWeek >= progress.goal,
            deloadWeeks = userDataManager.deloadWeeks,
            lists = userDataManager.userExerciseLists,
            ownsLifetime = billing.ownsProLifetime,
            supporterTips = userDataManager.supporterTips,
            foundingNumber = userDataManager.foundingNumber,
            stats = userDataManager.badgeStats,
            events = events,
            earned = userDataManager.earnedBadges,
        )
        // Past events that left Remote Config still show from the details saved with the badge
        val shownEvents = events.map { "event_${it.id}" }.toSet()
        val pastEvents = userDataManager.eventBadgeDetails.filterKeys { it !in shownEvents && it in userDataManager.earnedBadges }
            .map { (id, e) ->
                BadgeStatus(
                    Badge(id, e.title, e.description, e.emoji, BadgeCategory.EVENTS, BadgeTier.SPECIAL),
                    current = 1, earnedAtMillis = userDataManager.earnedBadges[id]
                )
            }
        val judged = evaluateBadges(input) + pastEvents
        statuses = judged

        val newlyEarned = judged.filter { it.isEarned && it.badge.id !in userDataManager.earnedBadges }
        if (newlyEarned.isEmpty()) return
        val eventDetails = newlyEarned.mapNotNull { status ->
            events.find { "event_${it.id}" == status.badge.id }?.let { status.badge.id to it }
        }.toMap()
        userDataManager.saveEarnedBadges(newlyEarned.associate { it.badge.id to now }, eventDetails, user)
        if (now >= quietUntil) newlyEarned.forEach(::celebrate)
    }

    private fun celebrate(status: BadgeStatus) {
        if (celebration == null) celebration = bannerFor(status) else queue.addLast(status)
    }

    private fun bannerFor(status: BadgeStatus) = PrCelebration(
        exerciseName = status.badge.title,
        setLabel = status.badge.description,
        headline = "Badge unlocked!",
        emoji = status.badge.emoji,
    )

    companion object {
        /** Long enough for history, records and lists to load after sign-in. */
        private const val QUIET_START_MS = 20_000L

        @Volatile var active: WeakReference<BadgeTracker> = WeakReference(null)
    }
}

/**
 * Remote Config for badges: "badge_events" is a JSON list of events, added without an app update.
 * (Founding member isn't here: the server numbers the first 100 accounts.)
 *
 * Event example:
 * [{"id":"ramadan_2027","title":"Ramadan Warrior","emoji":"🌙","description":"Train on 10 days during Ramadan",
 *   "start":"2027-02-08","end":"2027-03-09","goalType":"workouts","goal":10}]
 */
object BadgeEventsConfig {
    const val EVENTS_KEY = "badge_events"

    fun events(): List<BadgeEvent> = parseEvents(FirebaseRemoteConfig.getInstance().getString(EVENTS_KEY))

    /** Bad entries are skipped rather than breaking the badges page. */
    fun parseEvents(json: String): List<BadgeEvent> {
        if (json.isBlank()) return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                val zone = ZoneId.systemDefault()
                BadgeEvent(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    emoji = o.optString("emoji", "🎖️"),
                    description = o.optString("description", ""),
                    startsAt = LocalDate.parse(o.getString("start")).atStartOfDay(zone).toInstant().toEpochMilli(),
                    endsAt = LocalDate.parse(o.getString("end")).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
                    goalType = o.optString("goalType", "workouts"),
                    goal = o.optInt("goal", 1),
                )
            }.getOrNull()
        }
    }
}
