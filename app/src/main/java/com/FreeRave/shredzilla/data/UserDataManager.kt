package com.FreeRave.shredzilla.data

import com.FreeRave.shredzilla.utils.displayToKg
import com.FreeRave.shredzilla.models.exerciseIdOf
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.models.RecordedSet
import com.FreeRave.shredzilla.screens.settings.ThemeSetting
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    var userWorkoutReminderSetting by mutableStateOf("Never")
    var userWeeklyGoalDays by mutableStateOf<Int?>(null)
    var themePreferenceMale by mutableStateOf(ThemeSetting.SYSTEM)
    var themePreferenceFemale by mutableStateOf(ThemeSetting.SYSTEM)
    var userExerciseLists by mutableStateOf<List<UserExerciseList>>(emptyList())
    var todayRecordedSetsList by mutableStateOf<List<RecordedSet>>(emptyList())
    var todayTotalReps by mutableIntStateOf(0)
    var todayTotalSets by mutableIntStateOf(0)
    var todayUniqueExercises by mutableIntStateOf(0)

    // --- Listener Management ---
    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun clearAllListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        todaySetsListener = null
    }

    /** Sign-out: stop listening and forget everything about the previous user, so the next one starts clean. */
    fun resetForSignOut() {
        clearAllListeners()
        masterExerciseList.clear()
        lastPerformedTimestampsMap.clear()
        userRestTimePreference = "1min"
        userProfileImageLocalPath = null
        userUnitSystem = UnitSystem.METRIC
        userWorkoutReminderSetting = "Never"
        userWeeklyGoalDays = null
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
        listOf(days, userDoc.collection("exerciseLists"), userDoc.collection("exercises")).forEach { deleteCollection(it) }
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
        // Listener for commonExercises
        val commonExercisesRegistration = db.collection("commonExercises").orderBy("name")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed for commonExercises.", e)
                    return@addSnapshotListener
                }
                val exercises = mutableListOf<String>()
                snapshots?.forEach { doc ->
                    doc.getString("name")?.let { exercises.add(it) }
                }
                masterExerciseList.clear()
                masterExerciseList.addAll(exercises.distinct())
            }
        listeners.add(commonExercisesRegistration)

        currentUser?.uid?.let { userId ->
            // Listener for user document
            val userDocRegistration = db.collection("users").document(userId)
                .addSnapshotListener { userDocSnapshot, e ->
                    if (e != null) {
                        Log.w("Firestore", "User doc listen failed for $userId.", e)
                        return@addSnapshotListener
                    }
                    if (userDocSnapshot != null && userDocSnapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        (userDocSnapshot.get("initialExercises") as? List<String>)?.forEach {
                            addExerciseToCommonExercises(
                                it
                            )
                        }
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
                        // Stored as the onboarding label ("3 times per week"); its position is days - 1
                        userWeeklyGoalDays = workoutFrequencyOptions
                            .indexOf(userDocSnapshot.getString("weeklyGoalFrequency"))
                            .takeIf { it >= 0 }?.plus(1)
                        userDocSnapshot.getString("workoutReminder")
                            ?.let { userWorkoutReminderSetting = it }

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
                        val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()
                        currentSets.add(
                            RecordedSet(
                                exerciseName = exName,
                                setNumber = 1,
                                reps = reps,
                                weight = weight,
                                notes = notes,
                                timestamp = ts,
                                exerciseId = exId,
                                firestoreDocId = doc.id
                            )
                        )
                        repsCount += reps; setsCount++; if (exName.isNotBlank()) uniqueExNames.add(
                        exName
                    )
                    }
                    todayRecordedSetsList = currentSets
                    todayTotalReps = repsCount; todayTotalSets = setsCount; todayUniqueExercises =
                    uniqueExNames.size
                }
        todaySetsListener?.let { listeners.add(it) }
    }

    // --- Data Modification Functions ---

    fun addExerciseToCommonExercises(exerciseName: String) {
        val trimmedName = exerciseName.trim()
        if (trimmedName.isNotBlank()) {
            val exerciseDocId = exerciseIdOf(trimmedName)
            val exerciseDocRef = db.collection("commonExercises").document(exerciseDocId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(exerciseDocRef)
                if (!snapshot.exists()) {
                    transaction.set(
                        exerciseDocRef,
                        mapOf("name" to trimmedName, "createdAt" to FieldValue.serverTimestamp())
                    )
                }
                null
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error adding exercise '$trimmedName' to commonExercises", e)
            }
        }
    }

    fun deleteExerciseFromFirestore(exerciseName: String) {
        val trimmedName = exerciseName.trim()
        if (trimmedName.isNotBlank()) {
            val exerciseDocId = exerciseIdOf(trimmedName)
            db.collection("commonExercises").document(exerciseDocId).delete()
                .addOnSuccessListener {
                    Log.d(
                        "Firestore",
                        "Exercise $trimmedName deleted from commonExercises."
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "Firestore",
                        "Error deleting $trimmedName from commonExercises",
                        e
                    )
                }
        }
    }

    fun updateProfileImagePathInFirestore(newPath: String?, currentUser: FirebaseUser?) {
        updateUserSetting(currentUser?.uid, "profileImageLocalPath", newPath)
        userProfileImageLocalPath = newPath
    }

    private fun setsCollection(userId: String, dateKey: String) =
        db.collection("users").document(userId)
            .collection("dailyActivity").document(dateKey)
            .collection("recordedSets")

    /** [weightInput] is in the user's unit; it's stored in kg. [onSaved] fires once Firestore confirms. */
    fun recordSet(
        exerciseName: String,
        reps: Int,
        weightInput: Double,
        notes: String,
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

        val setData = hashMapOf(
            "exerciseName" to exerciseName,
            "exerciseId" to exId,
            "reps" to reps,
            "weight" to convertToStorageWeight(weightInput), // always kg
            "notes" to notes,
            "timestamp" to ts
        )
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(ts.toDate())
        // The day document must exist too: Firestore can't list days that only have a subcollection
        userDocRef.collection("dailyActivity").document(dateKey).set(mapOf("date" to dateKey), SetOptions.merge())
        // Offline, Firestore keeps the write and syncs later; the today listener shows it immediately
        setsCollection(userId, dateKey).add(setData)
            .addOnSuccessListener { onSaved() }
            .addOnFailureListener { e -> Log.e("Firestore", "Error recording set for $exerciseName", e) }
    }

    fun deleteSet(dateKey: String, docId: String, currentUser: FirebaseUser?, onDone: () -> Unit = {}) {
        val userId = currentUser?.uid ?: return
        setsCollection(userId, dateKey).document(docId).delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { e -> Log.e("Firestore", "Error deleting set $docId", e) }
    }

    /** [weightInput] is in the user's unit; it's stored in kg. */
    fun updateSet(
        dateKey: String,
        docId: String,
        reps: Int,
        weightInput: Double,
        notes: String,
        currentUser: FirebaseUser?,
        onDone: () -> Unit = {}
    ) {
        val userId = currentUser?.uid ?: return
        val changes = mapOf(
            "reps" to reps.toLong(),
            "weight" to convertToStorageWeight(weightInput),
            "notes" to notes
        )
        setsCollection(userId, dateKey).document(docId).update(changes)
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { e -> Log.e("Firestore", "Error updating set $docId", e) }
    }

    // DRY Helper for settings updates
    fun updateUserSetting(userId: String?, key: String, value: Any?) {
        if (userId.isNullOrEmpty()) return
        db.collection("users").document(userId).set(hashMapOf(key to value), SetOptions.merge())
            .addOnFailureListener { e -> Log.e("Firestore", "Failed to update setting $key", e) }
    }

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
}
