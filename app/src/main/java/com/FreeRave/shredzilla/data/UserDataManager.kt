package com.FreeRave.shredzilla.data

import com.FreeRave.shredzilla.utils.displayToKg
import com.FreeRave.shredzilla.models.exerciseIdOf
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import com.FreeRave.shredzilla.pro.AdTimeBank
import com.FreeRave.shredzilla.badges.BadgeEvent
import com.FreeRave.shredzilla.badges.BadgeStats
import com.FreeRave.shredzilla.notifications.NotificationChoices
import com.FreeRave.shredzilla.notifications.WorkoutNotificationStyle
import com.FreeRave.shredzilla.screens.settings.parseReminderDays
import com.FreeRave.shredzilla.screens.settings.reminderSettingOf
import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.models.RecordedSet
import com.FreeRave.shredzilla.models.MeasurementCadence
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.models.setNumbers
import com.FreeRave.shredzilla.screens.settings.ThemeSetting
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.screens.settings.LengthUnit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.FreeRave.shredzilla.models.exerciseIdOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import com.google.firebase.firestore.CollectionReference
import com.FreeRave.shredzilla.utils.dateKeysBetween
import com.FreeRave.shredzilla.onboarding.workoutFrequencyOptions
import com.FreeRave.shredzilla.models.WorkoutProgram
import com.FreeRave.shredzilla.models.TrainingLevel
import com.FreeRave.shredzilla.models.initialGlobalExerciseList

// Data classes that might be moved here or kept in a common 'models' package
// For now, assuming UserExerciseList and RecordedSet are accessible (e.g. from navigation or models package)

private const val FIRESTORE_BATCH_LIMIT = 500
private const val DELETE_PARALLELISM = 20

class UserDataManager(
    private val context: Context, // If needed for any utility functions
    private val onThemePreferenceChanged: (ThemeSetting, ThemeSetting) -> Unit // Callback for theme updates
) {
    private val db = Firebase.firestore

    // States previously in MainAppContainer
    val masterExerciseList = mutableStateListOf<String>()
    var userRestTimePreference by mutableStateOf("1min")
    val lastPerformedTimestampsMap = mutableStateMapOf<String, Timestamp>()
    var userProfileImageLocalPath by mutableStateOf<String?>(null)
    var userUnitSystem by mutableStateOf(UnitSystem.METRIC)
    /** Body measurements and height; separate from weights since 1.6.1. */
    var userLengthUnit by mutableStateOf(LengthUnit.CENTIMETRES)
    var userWorkoutReminderSetting by mutableStateOf("Never")
    var userWeeklyGoalDays by mutableStateOf<Int?>(null)
    /** Week keys (first day of the week) the user marked as deload weeks. */
    var deloadWeeks by mutableStateOf<Set<String>>(emptySet())
        private set
    /** The week the deload reminder was dismissed in, so it stays hidden for the rest of that week. */
    var deloadReminderDismissedWeek by mutableStateOf<String?>(null)
        private set
    var measurementCadence by mutableStateOf(MeasurementCadence.WEEKLY)
        private set
    /** Pro time earned from rewarded ads (wall-clock end, the day it was counted for and ads watched that day). */
    var adTimeBank by mutableStateOf(AdTimeBank())
        private set
    /** How many tips (consumable purchases) this account has sent; any tip shows the supporter badge. */
    var supporterTips by mutableIntStateOf(0)
        private set
    /** Which reminders are on and when they arrive; the inactivity part lives in [userWorkoutReminderSetting]. */
    var notificationChoices by mutableStateOf(NotificationChoices())
        private set
    /** Badges on the account (id to earned time), event badges' details, and the all-time counters behind them. */
    var earnedBadges by mutableStateOf<Map<String, Long>>(emptyMap())
        private set
    var eventBadgeDetails by mutableStateOf<Map<String, BadgeEvent>>(emptyMap())
        private set
    var badgeStats by mutableStateOf(BadgeStats())
        private set
    /** False until the user document arrived, so badges aren't judged before what's already earned is known. */
    var badgesLoaded by mutableStateOf(false)
        private set
    /** Stored name of the chosen Pro accent theme, null for the profile's default colors. */
    var accentThemeKey by mutableStateOf<String?>(null)
        private set
    var themePreferenceMale by mutableStateOf(ThemeSetting.SYSTEM)
    var themePreferenceFemale by mutableStateOf(ThemeSetting.SYSTEM)
    var userExerciseLists by mutableStateOf<List<UserExerciseList>>(emptyList())
    var todayRecordedSetsList by mutableStateOf<List<RecordedSet>>(emptyList())
    var todayTotalReps by mutableIntStateOf(0)
    var todayTotalSets by mutableIntStateOf(0)
    var todayUniqueExercises by mutableIntStateOf(0)

    // --- Listener Management ---
    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    // Signed-in user whose library (users/{uid}/exercises) add/delete operate on
    private var libraryUserId: String? = null

    fun clearAllListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        todaySetsListener = null
        libraryUserId = null
        masterExerciseList.clear()
    }

    private fun userExercisesCollection(userId: String) =
        db.collection("users").document(userId).collection("exercises")

    /** Sign-out: stop listening and forget everything about the previous user, so the next one starts clean. */
    fun resetForSignOut() {
        clearAllListeners()
        masterExerciseList.clear()
        lastPerformedTimestampsMap.clear()
        userRestTimePreference = "1min"
        userProfileImageLocalPath = null
        userUnitSystem = UnitSystem.METRIC
        userLengthUnit = LengthUnit.CENTIMETRES
        userWorkoutReminderSetting = "Never"
        userWeeklyGoalDays = null
        deloadWeeks = emptySet()
        deloadReminderDismissedWeek = null
        measurementCadence = MeasurementCadence.WEEKLY
        adTimeBank = AdTimeBank()
        supporterTips = 0
        notificationChoices = NotificationChoices()
        earnedBadges = emptyMap()
        eventBadgeDetails = emptyMap()
        badgeStats = BadgeStats()
        badgesLoaded = false
        accentThemeKey = null
        themePreferenceMale = ThemeSetting.SYSTEM
        themePreferenceFemale = ThemeSetting.SYSTEM
        userExerciseLists = emptyList()
        todayRecordedSetsList = emptyList()
        todayTotalReps = 0
        todayTotalSets = 0
        todayUniqueExercises = 0
    }

    /**
     * Deletes everything stored under users/{uid}. Firestore doesn't delete subcollections with their
     * parent, so each one is emptied explicitly. Old day documents were never written, so days are
     * also enumerated by key from [accountCreatedMillis] to today to reach every logged set.
     */
    suspend fun deleteAllUserData(userId: String, accountCreatedMillis: Long) = coroutineScope {
        val userDoc = db.collection("users").document(userId)
        val days = userDoc.collection("dailyActivity")
        val listedDayKeys = days.get().await().documents.map { it.id }
        val allDayKeys = (listedDayKeys + dateKeysBetween(accountCreatedMillis, System.currentTimeMillis())).distinct()

        allDayKeys.chunked(DELETE_PARALLELISM).forEach { chunk ->
            chunk.map { key -> async { deleteCollection(days.document(key).collection("recordedSets")) } }.awaitAll()
        }
        listOf(
            days,
            userDoc.collection("exerciseLists"),
            userDoc.collection("exercises"),
            userDoc.collection(TrainingHistoryStore.WORKOUTS),
            userDoc.collection(TrainingHistoryStore.RECORDS),
            userDoc.collection(TrainingHistoryStore.MEASUREMENTS)
        ).forEach { deleteCollection(it) }
        userDoc.delete().await()
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        val docs = collection.get().await().documents
        docs.chunked(FIRESTORE_BATCH_LIMIT).forEach { chunk ->
            db.runBatch { batch -> chunk.forEach { batch.delete(it.reference) } }.await()
        }
    }

    // --- Firestore Listeners ---
    fun setupFirestoreListeners(currentUser: FirebaseUser?) {
        libraryUserId = currentUser?.uid
        masterExerciseList.clear()

        currentUser?.uid?.let { userId ->
            // Listener for the user's own exercise library
            val exercisesRegistration = userExercisesCollection(userId).orderBy("name")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w("Firestore", "Listen failed for exercises of $userId.", e)
                        return@addSnapshotListener
                    }
                    val exercises = mutableListOf<String>()
                    snapshots?.forEach { doc ->
                        doc.getString("name")?.let { exercises.add(it) }
                    }
                    masterExerciseList.clear()
                    masterExerciseList.addAll(exercises.distinct())
                }
            listeners.add(exercisesRegistration)

            ensureExerciseLibrarySeeded(userId)

            // Listener for user document
            val userDocRegistration = db.collection("users").document(userId)
                .addSnapshotListener { userDocSnapshot, e ->
                    if (e != null) {
                        Log.w("Firestore", "User doc listen failed for $userId.", e)
                        return@addSnapshotListener
                    }
                    if (userDocSnapshot != null && userDocSnapshot.exists()) {
                        userDocSnapshot.getString("restTimePreference")
                            ?.let { userRestTimePreference = it }
                        @Suppress("UNCHECKED_CAST")
                        (userDocSnapshot.get("lastPerformedTimestamps") as? Map<String, Timestamp>)?.let {
                            lastPerformedTimestampsMap.clear()
                            lastPerformedTimestampsMap.putAll(it)
                        }
                        userProfileImageLocalPath =
                            userDocSnapshot.getString("profileImageLocalPath")
                        userDocSnapshot.getString("unitSystem")?.let {
                            try {
                                userUnitSystem = UnitSystem.valueOf(it.uppercase(Locale.ROOT))
                            } catch (e: IllegalArgumentException) {
                                Log.w("Firestore", "Invalid unit system: $it")
                            }
                        }
                        userLengthUnit = LengthUnit.fromStored(userDocSnapshot.getString("lengthUnit"), userUnitSystem)
                        // Stored as the onboarding label ("3 times per week"); its position is days - 1
                        userWeeklyGoalDays = workoutFrequencyOptions
                            .indexOf(userDocSnapshot.getString("weeklyGoalFrequency"))
                            .takeIf { it >= 0 }?.plus(1)
                        userDocSnapshot.getString("workoutReminder")
                            ?.let { userWorkoutReminderSetting = it }
                        @Suppress("UNCHECKED_CAST")
                        deloadWeeks = (userDocSnapshot.get("deloadWeeks") as? List<String>).orEmpty().toSet()
                        deloadReminderDismissedWeek = userDocSnapshot.getString("deloadReminderDismissedWeek")
                        measurementCadence = MeasurementCadence.fromStored(userDocSnapshot.getString("measurementCadence"))
                        adTimeBank = AdTimeBank(
                            untilMillis = userDocSnapshot.getTimestamp("proUntil")?.toDate()?.time ?: 0L,
                            adTimes = (userDocSnapshot.get("proAdTimes") as? List<*>).orEmpty()
                                .mapNotNull { (it as? Timestamp)?.toDate()?.time },
                        )
                        supporterTips = userDocSnapshot.getLong("supporterTips")?.toInt() ?: 0
                        accentThemeKey = userDocSnapshot.getString("accentTheme")
                        earnedBadges = (userDocSnapshot.get("badges") as? Map<*, *>).orEmpty()
                            .mapNotNull { (k, v) -> (k as? String)?.let { id -> (v as? Timestamp)?.toDate()?.time?.let { id to it } } }.toMap()
                        eventBadgeDetails = (userDocSnapshot.get("eventBadges") as? Map<*, *>).orEmpty().mapNotNull { (k, v) ->
                            val id = k as? String ?: return@mapNotNull null
                            val m = v as? Map<*, *> ?: return@mapNotNull null
                            id to BadgeEvent(
                                id = id.removePrefix("event_"), title = m["title"] as? String ?: "Event",
                                emoji = m["emoji"] as? String ?: "🎖️", description = m["description"] as? String ?: "",
                                startsAt = 0L, endsAt = 0L, goalType = "open", goal = 1,
                            )
                        }.toMap()
                        (userDocSnapshot.get("stats") as? Map<*, *>)?.let { st ->
                            badgeStats = BadgeStats(
                                trainingDays = (st["trainingDays"] as? Number)?.toInt() ?: 0,
                                sets = (st["sets"] as? Number)?.toInt() ?: 0,
                                records = (st["records"] as? Number)?.toInt() ?: 0,
                                lastTrainingDay = st["lastTrainingDay"] as? String,
                            )
                        }
                        badgesLoaded = true
                        notificationChoices = NotificationChoices(
                            reminderDays = parseReminderDays(userDocSnapshot.getString("workoutReminder") ?: "Never"),
                            reminderHour = userDocSnapshot.getLong("reminderHour")?.toInt() ?: NotificationChoices.DEFAULT_REMINDER_HOUR,
                            weeklyGoal = userDocSnapshot.getBoolean("notifyWeeklyGoal") ?: true,
                            measurements = userDocSnapshot.getBoolean("notifyMeasurements") ?: true,
                            proEnding = userDocSnapshot.getBoolean("notifyProEnding") ?: true,
                            workoutNotification = userDocSnapshot.getBoolean("workoutNotification") ?: true,
                            workoutStyle = WorkoutNotificationStyle.fromStored(userDocSnapshot.getString("workoutNotificationStyle")),
                        )

                        val oldThemeMale = themePreferenceMale
                        val oldThemeFemale = themePreferenceFemale
                        userDocSnapshot.getString("themePreferenceMale")?.let { themeStr ->
                            try {
                                themePreferenceMale = ThemeSetting.valueOf(themeStr)
                            } catch (e: Exception) {
                                Log.w("Firestore", "Invalid themeMale: $themeStr")
                            }
                        }
                        userDocSnapshot.getString("themePreferenceFemale")?.let { themeStr ->
                            try {
                                themePreferenceFemale = ThemeSetting.valueOf(themeStr)
                            } catch (e: Exception) {
                                Log.w("Firestore", "Invalid themeFemale: $themeStr")
                            }
                        }
                        if (oldThemeMale != themePreferenceMale || oldThemeFemale != themePreferenceFemale) {
                            onThemePreferenceChanged(themePreferenceMale, themePreferenceFemale)
                        }

                    } else {
                        Log.d("Firestore", "User document $userId not found.")
                    }
                }
            listeners.add(userDocRegistration)

            // Listener for user's exercise lists
            val exerciseListsRegistration = db.collection("users").document(userId).collection("exerciseLists")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { listsSnapshot, e ->
                    if (e != null) {
                        Log.w("Firestore", "Exercise lists listen failed for $userId.", e)
                        userExerciseLists = emptyList()
                        return@addSnapshotListener
                    }
                    val currentLists = mutableListOf<UserExerciseList>()
                    listsSnapshot?.documents?.forEach { doc ->
                        val listName = doc.getString("name") ?: "Unnamed List"

                        @Suppress("UNCHECKED_CAST")
                        val exIds = doc.get("exerciseIds") as? List<String> ?: emptyList()
                        currentLists.add(
                            UserExerciseList(
                                id = doc.id,
                                name = listName,
                                exerciseIds = exIds,
                                targetSetsMin = doc.getLong("targetSetsMin")?.toInt(),
                                targetSetsMax = doc.getLong("targetSetsMax")?.toInt()
                            )
                        )
                    }
                    userExerciseLists = currentLists
                }
            listeners.add(exerciseListsRegistration)

            // Listener for today's recorded sets
            loadSetsForDate(userId)
        }
    }

    var selectedDateStr by mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        private set

    private var todaySetsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadSetsForDate(userId: String, targetDate: Date = Date()) {
        val newDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(targetDate)
        if (newDateStr == selectedDateStr && todaySetsListener != null) return
        
        selectedDateStr = newDateStr

        val oldListener = todaySetsListener
        if (oldListener != null) {
            oldListener.remove()
            listeners.remove(oldListener)
        }
        
        todaySetsListener = db.collection("users").document(userId).collection("dailyActivity")
            .document(selectedDateStr).collection("recordedSets")
            .orderBy(
                "timestamp",
                com.google.firebase.firestore.Query.Direction.ASCENDING
            ) // Order by time
                .addSnapshotListener { setsSnapshot, e ->
                    if (e != null) {
                        Log.w(
                            "Firestore",
                            "Today's sets listen failed for $userId on $selectedDateStr.",
                            e
                        )
                        todayRecordedSetsList = emptyList(); todayTotalReps = 0; todayTotalSets =
                            0; todayUniqueExercises = 0
                        return@addSnapshotListener
                    }
                    val currentSets = mutableListOf<RecordedSet>()
                    var repsCount = 0;
                    var setsCount = 0;
                    val uniqueExNames = mutableSetOf<String>()
                    setsSnapshot?.documents?.forEach { doc ->
                        val exName = doc.getString("exerciseName") ?: ""
                        val exId = doc.getString("exerciseId") ?: ""
                        val reps = doc.getLong("reps")?.toInt() ?: 0
                        val weight = doc.getDouble("weight")?.toFloat() ?: 0.0f
                        val notes = doc.getString("notes")
                        val ts = doc.getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: Timestamp.now()
                        currentSets.add(
                            RecordedSet(
                                exerciseName = exName,
                                setNumber = 0, // numbered below, once the day's order is known
                                reps = reps,
                                weight = weight,
                                notes = notes,
                                timestamp = ts,
                                exerciseId = exId,
                                firestoreDocId = doc.id,
                                plates = doc.getLong("plates")?.toInt(),
                                side = Side.fromStored(doc.getString("side")),
                                bodyweight = doc.getBoolean("bodyweight") ?: false,
                                setType = SetDocFields.setTypeOf(doc),
                                tempo = SetDocFields.tempoOf(doc),
                                perSide = SetDocFields.perSideOf(doc)
                            )
                        )
                        // Warm-ups are logged but don't count towards the day's totals
                        if (SetDocFields.setTypeOf(doc) != SetType.WARMUP) {
                            repsCount += reps
                        }
                        if (exName.isNotBlank()) uniqueExNames.add(exName)
                    }
                    // Number each exercise's working sets in the order they were logged: a left + right
                    // pair shares a number (1, 1, 2, 2) and warm-ups get 0, shown as "W"
                    val numberByDocId = currentSets.groupBy { it.exerciseName }.values.flatMap { sets ->
                        sets.zip(setNumbers(sets, { it.side }, { it.setType == SetType.WARMUP }))
                    }.associate { (set, number) -> set.firestoreDocId to number }
                    todayRecordedSetsList = currentSets.map { it.copy(setNumber = numberByDocId[it.firestoreDocId] ?: 0) }
                    setsCount = currentSets.filter { it.setType != SetType.WARMUP }.groupBy { it.exerciseName }.values.sumOf { sets ->
                        sets.count { it.side == null } + maxOf(sets.count { it.side == Side.LEFT }, sets.count { it.side == Side.RIGHT })
                    }
                    todayTotalReps = repsCount; todayTotalSets = setsCount; todayUniqueExercises =
                    uniqueExNames.size
                }
        todaySetsListener?.let { listeners.add(it) }
    }

    // --- Data Modification Functions ---

    // Adds to the signed-in user's library (users/{uid}/exercises). The name is kept for existing call sites;
    // the shared commonExercises collection is no longer written.
    fun addExerciseToCommonExercises(exerciseName: String) {
        val trimmedName = exerciseName.trim()
        val userId = libraryUserId
        if (trimmedName.isNotBlank() && userId != null) {
            val exerciseDocRef = userExercisesCollection(userId).document(exerciseIdOf(trimmedName))
            // A merge write instead of a transaction: transactions need the server, merge writes are
            // queued offline. The id is derived from the name, so adding twice just rewrites the name.
            exerciseDocRef.set(mapOf("name" to trimmedName), SetOptions.merge())
                .addOnFailureListener { e -> Log.w("Firestore", "Error adding exercise '$trimmedName' to library of $userId", e) }
        } else if (userId == null) {
            Log.w("Firestore", "No signed-in user; not adding exercise '$trimmedName'.")
        }
    }

    // Deletes from the signed-in user's library only.
    fun deleteExerciseFromFirestore(exerciseName: String) {
        val trimmedName = exerciseName.trim()
        val userId = libraryUserId
        if (trimmedName.isNotBlank() && userId != null) {
            userExercisesCollection(userId).document(exerciseIdOf(trimmedName)).delete()
                .addOnSuccessListener {
                    Log.d(
                        "Firestore",
                        "Exercise $trimmedName deleted from library of $userId."
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "Firestore",
                        "Error deleting $trimmedName from library of $userId",
                        e
                    )
                }
        } else if (userId == null) {
            Log.w("Firestore", "No signed-in user; not deleting exercise '$trimmedName'.")
        }
    }

    // --- Exercise library seeding / one-time migration ---

    private val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val seedAttemptedUserIds = mutableSetOf<String>()

    /**
     * Makes sure users/{uid}/exercises is populated once. New users get their onboarding picks
     * (initialExercises); users from before the library was per-user get the exercises they actually
     * use (lastPerformedTimestamps, exercise lists), falling back to the old shared commonExercises
     * contents and finally the built-in list. Idempotent: skipped when the exerciseLibrarySeeded flag
     * is set or the collection already has docs, and doc ids are deterministic (exerciseIdOf(name)).
     */
    private fun ensureExerciseLibrarySeeded(userId: String) {
        if (!seedAttemptedUserIds.add(userId)) return
        seedScope.launch {
            try {
                seedExerciseLibrary(userId)
            } catch (e: Exception) {
                // Allow a retry on the next listener setup (e.g. when we were offline).
                seedAttemptedUserIds.remove(userId)
                Log.w("Firestore", "Exercise library seeding failed for $userId", e)
            }
        }
    }

    private suspend fun seedExerciseLibrary(userId: String) {
        val userDocRef = db.collection("users").document(userId)
        // Server reads only: a cached "empty" result while offline must not trigger seeding.
        val userDoc = userDocRef.get(Source.SERVER).await()
        if (!userDoc.exists() || userDoc.getBoolean(LIBRARY_SEEDED_FIELD) == true) return

        val markSeeded = mapOf(LIBRARY_SEEDED_FIELD to true)
        if (!userExercisesCollection(userId).limit(1).get(Source.SERVER).await().isEmpty) {
            userDocRef.set(markSeeded, SetOptions.merge()).await()
            return
        }

        // id -> display name, from the built-in list (by both its id and exerciseIdOf(name)) and commonExercises
        val knownNames = mutableMapOf<String, String>()
        val commonExercises = try {
            db.collection("commonExercises").get(Source.SERVER).await().documents
        } catch (e: Exception) {
            Log.w("Firestore", "Could not read commonExercises during migration", e)
            emptyList()
        }
        commonExercises.forEach { doc ->
            // Older builds wrote ids as names (e.g. "bench_press"); those carry no extra information.
            doc.getString("name")?.trim()?.takeIf { it.isNotBlank() && it != doc.id }
                ?.let { knownNames[doc.id] = it }
        }
        initialGlobalExerciseList.forEach { item ->
            knownNames[item.id] = item.name
            knownNames[exerciseIdOf(item.name)] = item.name
        }
        fun nameForId(id: String): String = knownNames[id]
            ?: id.split('_').filter { it.isNotBlank() }
                .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ROOT) } }

        val usedIds = linkedSetOf<String>()
        @Suppress("UNCHECKED_CAST")
        (userDoc.get("initialExercises") as? List<String>)?.let { usedIds.addAll(it) }
        @Suppress("UNCHECKED_CAST")
        (userDoc.get("lastPerformedTimestamps") as? Map<String, Any>)?.keys?.let { usedIds.addAll(it) }
        userDocRef.collection("exerciseLists").get(Source.SERVER).await().documents.forEach { doc ->
            @Suppress("UNCHECKED_CAST")
            (doc.get("exerciseIds") as? List<String>)?.let { usedIds.addAll(it) }
        }

        var names = usedIds.filter { it.isNotBlank() }.map { nameForId(it) }
        if (names.isEmpty()) names = commonExercises.map { nameForId(it.id) }
        if (names.isEmpty()) names = initialGlobalExerciseList.map { it.name }
        val namesById = names.map { it.trim() }.filter { it.isNotBlank() }
            .associateBy { exerciseIdOf(it) }

        // Batched writes are capped at 500 operations; the last chunk also sets the flag.
        val entries = namesById.entries.toList()
        val chunks = entries.chunked(400).ifEmpty { listOf(emptyList()) }
        chunks.forEachIndexed { index, chunk ->
            val batch = db.batch()
            chunk.forEach { (docId, name) ->
                batch.set(
                    userExercisesCollection(userId).document(docId),
                    mapOf("name" to name, "createdAt" to FieldValue.serverTimestamp())
                )
            }
            if (index == chunks.lastIndex) batch.set(userDocRef, markSeeded, SetOptions.merge())
            batch.commit().await()
        }
        Log.d("Firestore", "Seeded exercise library for $userId with ${entries.size} exercises.")
    }

    fun updateProfileImagePathInFirestore(newPath: String?, currentUser: FirebaseUser?) {
        updateUserSetting(currentUser?.uid, "profileImageLocalPath", newPath)
        userProfileImageLocalPath = newPath
    }

    private fun setsCollection(userId: String, dateKey: String) =
        db.collection("users").document(userId)
            .collection("dailyActivity").document(dateKey)
            .collection("recordedSets")

    /** [onSaved] fires as soon as the set is in the local cache (online or not). */
    fun recordSet(
        exerciseName: String,
        entry: SetEntry,
        currentUser: FirebaseUser?,
        onSaved: () -> Unit = {}
    ) {
        val userId = currentUser?.uid ?: return
        val exId = exerciseIdOf(exerciseName)
        val ts = Timestamp.now()
        val userDocRef = db.collection("users").document(userId)

        // Local first so "last done" updates instantly; merge creates the map field if it's missing
        lastPerformedTimestampsMap[exId] = ts
        userDocRef.set(mapOf("lastPerformedTimestamps" to mapOf(exId to ts)), SetOptions.merge())

        val setData = hashMapOf<String, Any>(
            "exerciseName" to exerciseName,
            "exerciseId" to exId,
            "timestamp" to ts
        ) + storedFieldsOf(entry)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(ts.toDate())
        // The day document must exist too: Firestore can't list days that only have a subcollection
        userDocRef.collection("dailyActivity").document(dateKey).set(mapOf("date" to dateKey), SetOptions.merge())
        // Offline, Firestore keeps the write and syncs later; the today listener shows it immediately
        setsCollection(userId, dateKey).add(setData)
            .addOnFailureListener { e -> Log.e("Firestore", "Error recording set for $exerciseName", e) }
        // Writes land in the local cache synchronously; waiting for the server would hang offline
        onSaved()
    }

    fun deleteSet(dateKey: String, docId: String, currentUser: FirebaseUser?, onDone: () -> Unit = {}) {
        val userId = currentUser?.uid ?: return
        setsCollection(userId, dateKey).document(docId).delete()
            .addOnFailureListener { e -> Log.e("Firestore", "Error deleting set $docId", e) }
        onDone() // already applied locally, see recordSet
    }

    fun updateSet(
        dateKey: String,
        docId: String,
        entry: SetEntry,
        currentUser: FirebaseUser?,
        onDone: () -> Unit = {}
    ) {
        val userId = currentUser?.uid ?: return
        // Switching a set back from plates to kg (or to both sides) must drop the old field
        val changes = storedFieldsOf(entry) +
            ("plates" to (entry.plates ?: FieldValue.delete())) +
            ("side" to (entry.side?.storedValue ?: FieldValue.delete())) +
            ("bodyweight" to (if (entry.bodyweight) true else FieldValue.delete())) +
            SetDocFields.CLEARABLE.associateWith { FieldValue.delete() } +
            SetDocFields.extrasOf(entry)
        setsCollection(userId, dateKey).document(docId).update(changes)
            .addOnFailureListener { e -> Log.e("Firestore", "Error updating set $docId", e) }
        onDone() // already applied locally, see recordSet
    }

    /** Set fields as stored: weight always in kg, and 0 for plate-counted sets. */
    private fun storedFieldsOf(entry: SetEntry): Map<String, Any> = buildMap {
        put("reps", entry.reps.toLong())
        put("weight", if (entry.plates != null) 0.0 else convertToStorageWeight(entry.weight))
        put("notes", entry.notes)
        entry.plates?.let { put("plates", it.toLong()) }
        entry.side?.let { put("side", it.storedValue) }
        if (entry.bodyweight) put("bodyweight", true)
        putAll(SetDocFields.extrasOf(entry))
    }

    // DRY Helper for settings updates
    fun updateUserSetting(userId: String?, key: String, value: Any?) {
        if (userId.isNullOrEmpty()) return
        db.collection("users").document(userId).set(hashMapOf(key to value), SetOptions.merge())
            .addOnFailureListener { e -> Log.e("Firestore", "Failed to update setting $key", e) }
    }

    fun setDeloadWeek(weekKey: String, isDeload: Boolean, currentUser: FirebaseUser?) {
        updateUserSetting(
            currentUser?.uid,
            "deloadWeeks",
            if (isDeload) FieldValue.arrayUnion(weekKey) else FieldValue.arrayRemove(weekKey)
        )
    }

    fun dismissDeloadReminder(weekKey: String, currentUser: FirebaseUser?) =
        updateUserSetting(currentUser?.uid, "deloadReminderDismissedWeek", weekKey)

    fun recordSupporterTip(currentUser: FirebaseUser?) {
        supporterTips += 1
        updateUserSetting(currentUser?.uid, "supporterTips", FieldValue.increment(1))
    }

    /** Saves every notification choice at once; the inactivity reminder keeps its old string format. */
    fun updateNotificationChoices(choices: NotificationChoices, currentUser: FirebaseUser?) {
        notificationChoices = choices
        userWorkoutReminderSetting = reminderSettingOf(choices.reminderDays)
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).set(
            hashMapOf(
                "workoutReminder" to userWorkoutReminderSetting,
                "reminderHour" to choices.reminderHour,
                "notifyWeeklyGoal" to choices.weeklyGoal,
                "notifyMeasurements" to choices.measurements,
                "notifyProEnding" to choices.proEnding,
                "workoutNotification" to choices.workoutNotification,
                "workoutNotificationStyle" to choices.workoutStyle.storedValue,
            ),
            SetOptions.merge()
        ).addOnFailureListener { e -> Log.e("Firestore", "Failed to save notification settings", e) }
    }

    /** Adds newly earned badges; event badges also keep their title and emoji for when the event is gone. */
    fun saveEarnedBadges(earned: Map<String, Long>, events: Map<String, BadgeEvent>, currentUser: FirebaseUser?) {
        if (earned.isEmpty()) return
        earnedBadges = earnedBadges + earned
        val uid = currentUser?.uid ?: return
        val data = hashMapOf<String, Any>("badges" to earned.mapValues { Timestamp(Date(it.value)) })
        if (events.isNotEmpty()) {
            data["eventBadges"] = events.mapValues { (_, e) -> mapOf("title" to e.title, "emoji" to e.emoji, "description" to e.description) }
            eventBadgeDetails = eventBadgeDetails + events
        }
        db.collection("users").document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.e("Firestore", "Failed to save badges", e) }
    }

    /** Counts a logged set toward the all-time counters (a new training day, a working set, a beaten record). */
    fun countForBadges(todayKey: String, workingSet: Boolean, beatRecord: Boolean, currentUser: FirebaseUser?) {
        val uid = currentUser?.uid ?: return
        val newDay = badgeStats.lastTrainingDay != todayKey && workingSet
        badgeStats = badgeStats.copy(
            trainingDays = badgeStats.trainingDays + if (newDay) 1 else 0,
            sets = badgeStats.sets + if (workingSet) 1 else 0,
            records = badgeStats.records + if (beatRecord) 1 else 0,
            lastTrainingDay = if (workingSet) todayKey else badgeStats.lastTrainingDay,
        )
        val stats = buildMap<String, Any> {
            if (workingSet) put("sets", FieldValue.increment(1))
            if (newDay) {
                put("trainingDays", FieldValue.increment(1))
                put("lastTrainingDay", todayKey)
            }
            if (beatRecord) put("records", FieldValue.increment(1))
        }
        if (stats.isNotEmpty()) {
            db.collection("users").document(uid).set(mapOf("stats" to stats), SetOptions.merge())
                .addOnFailureListener { e -> Log.e("Firestore", "Failed to count for badges", e) }
        }
    }

    /** Lifts the counters to at least what the loaded history shows, for accounts from before badges. */
    fun raiseBadgeStats(trainingDays: Int, sets: Int, records: Int, currentUser: FirebaseUser?) {
        val raised = badgeStats.copy(
            trainingDays = maxOf(badgeStats.trainingDays, trainingDays),
            sets = maxOf(badgeStats.sets, sets),
            records = maxOf(badgeStats.records, records),
        )
        if (raised == badgeStats) return
        badgeStats = raised
        val uid = currentUser?.uid ?: return
        db.collection("users").document(uid).set(
            mapOf("stats" to mapOf("trainingDays" to raised.trainingDays, "sets" to raised.sets, "records" to raised.records)),
            SetOptions.merge()
        )
    }

    fun updateAccentTheme(key: String?, currentUser: FirebaseUser?) {
        accentThemeKey = key
        updateUserSetting(currentUser?.uid, "accentTheme", key ?: FieldValue.delete())
    }

    fun updateMeasurementCadence(cadence: MeasurementCadence, currentUser: FirebaseUser?) =
        updateUserSetting(currentUser?.uid, "measurementCadence", cadence.storedValue)

    // DRY Helper for unit conversion
    private fun convertToStorageWeight(displayWeight: Double): Double = displayToKg(displayWeight, userUnitSystem)

    fun createNewList(listName: String, selectedExerciseIds: List<String>, currentUser: FirebaseUser?) {
        val newListData = hashMapOf(
            "name" to listName,
            "exerciseIds" to selectedExerciseIds,
            "createdAt" to FieldValue.serverTimestamp()
        )
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("exerciseLists").add(newListData)
                .addOnSuccessListener { Log.d("Firestore", "New list '$listName' saved.") }
                .addOnFailureListener { e -> Log.e("Firestore", "Error saving list '$listName'", e) }
        }
    }

    /**
     * Adds a ready-made split: every exercise goes into the library (existing ones are left alone)
     * and each day becomes a list like "PPL · Push" carrying the level's suggested sets.
     */
    fun addProgram(program: WorkoutProgram, level: TrainingLevel, currentUser: FirebaseUser?) {
        val userId = currentUser?.uid ?: return
        program.allExercises.forEach(::addExerciseToCommonExercises)
        val lists = db.collection("users").document(userId).collection("exerciseLists")
        val now = System.currentTimeMillis()
        program.days.forEachIndexed { index, day ->
            val listData = hashMapOf(
                "name" to "${program.shortName} · ${day.name}",
                "exerciseIds" to day.exercises.map(::exerciseIdOf),
                "targetSetsMin" to level.minSets,
                "targetSetsMax" to level.maxSets,
                // Lists are shown newest first, so give earlier days a later time to keep A before B
                "createdAt" to Timestamp(Date(now + (program.days.size - index)))
            )
            lists.add(listData).addOnFailureListener { e -> Log.e("Firestore", "Error adding ${program.name} list", e) }
        }
    }

    fun removeExerciseFromList(listId: String, exerciseIdToDelete: String, currentUser: FirebaseUser?) {
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("exerciseLists").document(listId)
                .update("exerciseIds", FieldValue.arrayRemove(exerciseIdToDelete))
        }
    }

    /** Saves the order the user dragged the list's exercises into. */
    fun updateListOrder(listId: String, exerciseIds: List<String>, currentUser: FirebaseUser?) {
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("exerciseLists").document(listId)
                .update("exerciseIds", exerciseIds)
                .addOnFailureListener { e -> Log.e("Firestore", "Error reordering list $listId", e) }
        }
    }

    /** Deletes the list only; the exercises stay in the library and logged sets are untouched. */
    fun deleteList(listId: String, currentUser: FirebaseUser?) {
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("exerciseLists").document(listId).delete()
                .addOnFailureListener { e -> Log.e("Firestore", "Error deleting list $listId", e) }
        }
    }

    fun updateList(listId: String, updatedListName: String, updatedExerciseIds: List<String>, currentUser: FirebaseUser?) {
        val listUpdateData = hashMapOf(
            "name" to updatedListName,
            "exerciseIds" to updatedExerciseIds
        )
        currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).collection("exerciseLists").document(listId)
                .set(listUpdateData, SetOptions.merge())
        }
    }

    companion object {
        private const val LIBRARY_SEEDED_FIELD = "exerciseLibrarySeeded"
    }
}
