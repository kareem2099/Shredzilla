package com.FreeRave.shredzilla.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.FreeRave.shredzilla.models.ExerciseItem
import com.FreeRave.shredzilla.models.ExerciseSetPerformance
import com.FreeRave.shredzilla.models.initialGlobalExerciseList
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
import java.util.Locale

// Data classes that might be moved here or kept in a common 'models' package
// For now, assuming UserExerciseList and RecordedSet are accessible (e.g. from navigation or models package)

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
    var themePreferenceMale by mutableStateOf(ThemeSetting.SYSTEM)
    var themePreferenceFemale by mutableStateOf(ThemeSetting.SYSTEM)
    var userExerciseLists by mutableStateOf<List<UserExerciseList>>(emptyList())
    var todayRecordedSetsList by mutableStateOf<List<RecordedSet>>(emptyList())
    var todayTotalReps by mutableIntStateOf(0)
    var todayTotalSets by mutableIntStateOf(0)
    var todayUniqueExercises by mutableIntStateOf(0)

    // --- Firestore Listeners ---
    fun setupFirestoreListeners(currentUser: FirebaseUser?) {
        // Listener for commonExercises
        db.collection("commonExercises").orderBy("name")
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

        currentUser?.uid?.let { userId ->
            // Listener for user document
            db.collection("users").document(userId)
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

            // Listener for user's exercise lists
            db.collection("users").document(userId).collection("exerciseLists")
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
                                exerciseIds = exIds
                            )
                        )
                    }
                    userExerciseLists = currentLists
                }

            // Listener for today's recorded sets
            val todayDateStr =
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Timestamp.now().toDate())
            db.collection("users").document(userId).collection("dailyActivity")
                .document(todayDateStr).collection("recordedSets")
                .orderBy(
                    "timestamp",
                    com.google.firebase.firestore.Query.Direction.ASCENDING
                ) // Order by time
                .addSnapshotListener { setsSnapshot, e ->
                    if (e != null) {
                        Log.w(
                            "Firestore",
                            "Today's sets listen failed for $userId on $todayDateStr.",
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
        }
    }

    // --- Data Modification Functions ---

    fun addExerciseToCommonExercises(exerciseName: String) {
        val trimmedName = exerciseName.trim()
        if (trimmedName.isNotBlank()) {
            val exerciseDocId = trimmedName.lowercase().replace(" ", "_")
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
            val exerciseDocId = trimmedName.lowercase().replace(" ", "_")
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
        currentUser?.uid?.let { userId ->
            val userDocRef = db.collection("users").document(userId)
            val updateData = hashMapOf<String, Any?>("profileImageLocalPath" to newPath)
            userDocRef.set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(
                        "Firestore",
                        "Profile image path updated to: $newPath"
                    ); userProfileImageLocalPath = newPath
                }
                .addOnFailureListener { e ->
                    Log.e(
                        "Firestore",
                        "Error updating profile image path",
                        e
                    )
                }
        }
    }

    fun recordSet(
        exerciseName: String,
        reps: Int,
        weightInput: Double,
        notes: String,
        currentUser: FirebaseUser?
    ) {
        val exId = exerciseName.lowercase().replace(" ", "_")
        val ts = Timestamp.now()
        val weightInKg =
            if (userUnitSystem == UnitSystem.IMPERIAL) weightInput * 0.45359237 else weightInput

        currentUser?.uid?.let { userId ->
            val userDocRef = db.collection("users").document(userId)
            userDocRef.update("lastPerformedTimestamps.$exId", ts)
                .addOnSuccessListener { lastPerformedTimestampsMap[exId] = ts }
                .addOnFailureListener {
                    // If field doesn't exist, set it.
                    userDocRef.set(
                        mapOf("lastPerformedTimestamps" to mapOf(exId to ts)),
                        SetOptions.merge()
                    )
                        .addOnSuccessListener { lastPerformedTimestampsMap[exId] = ts }
                }

            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(ts.toDate())
            val setData = hashMapOf(
                "exerciseName" to exerciseName,
                "exerciseId" to exId,
                "reps" to reps,
                "weight" to weightInKg, // Always store in kg
                "notes" to notes,
                "timestamp" to ts
            )

            userDocRef.collection("dailyActivity").document(todayDate).collection("recordedSets")
                .add(setData)
                .addOnSuccessListener { documentReference ->
                    val firestoreDocumentId = documentReference.id
                    updateLocalExerciseSetInList(
                        exerciseName,
                        reps,
                        weightInput,
                        notes,
                        ts,
                        firestoreDocumentId
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding set to dailyActivity for $exerciseName", e)
                }
        }
    }

    private fun updateLocalExerciseSetInList(
        exName: String,
        reps: Int,
        weightInput: Double,
        notes: String,
        ts: Timestamp,
        firestoreDocId: String?
    ) {
        val exerciseIdForLocalUpdate = exName.lowercase().replace(" ", "_")
        val exerciseToUpdate = initialGlobalExerciseList.find { it.id == exerciseIdForLocalUpdate }
        exerciseToUpdate?.let { item ->
            val newSetNumber = (item.recordedSets.maxOfOrNull { set -> set.setNumber } ?: 0) + 1
            val newSetPerformance = ExerciseSetPerformance(
                setNumber = newSetNumber,
                reps = reps.toString(),
                weight = weightInput.toString(), // Keep in user's unit for local display consistency
                notes = notes,
                timestamp = ts,
                firestoreDocId = firestoreDocId
            )
            item.recordedSets.add(newSetPerformance)
            Log.d(
                "LocalUpdate",
                "Locally added recordedSet for '$exName' with docID '$firestoreDocId'. New count: ${item.recordedSets.size}"
            )
        } ?: Log.w(
            "LocalUpdate",
            "Exercise '$exName' (ID: '$exerciseIdForLocalUpdate') not found in local initialGlobalExerciseList for adding set."
        )
    }


    fun deleteSet(exerciseName: String, firestoreDocId: String, currentUser: FirebaseUser?) {
        currentUser?.uid?.let { userId ->
            val exerciseItem = initialGlobalExerciseList.find { it.name == exerciseName }
            val setToRemove =
                exerciseItem?.recordedSets?.find { it.firestoreDocId == firestoreDocId }

            if (setToRemove != null) {
                val setDateStr =
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(setToRemove.timestamp.toDate())
                val setDocRef = db.collection("users").document(userId)
                    .collection("dailyActivity").document(setDateStr)
                    .collection("recordedSets").document(firestoreDocId)

                setDocRef.delete()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Set $firestoreDocId deleted successfully.")
                        exerciseItem.recordedSets.remove(setToRemove)
                        Log.d(
                            "LocalUpdate",
                            "Set $firestoreDocId removed locally for $exerciseName."
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "Firestore",
                            "Error deleting set $firestoreDocId",
                            e
                        )
                    }
            } else {
                Log.w(
                    "DeleteSet",
                    "Set with docID $firestoreDocId not found locally for $exerciseName."
                )
            }
        }
    }

    fun updateSet(
        exerciseName: String,
        firestoreDocId: String,
        newReps: Int,
        newWeight: Double,
        newNotes: String,
        currentUser: FirebaseUser?
    ) {
        currentUser?.uid?.let { userId ->
            val exerciseItem = initialGlobalExerciseList.find { it.name == exerciseName }
            val setToUpdate =
                exerciseItem?.recordedSets?.find { it.firestoreDocId == firestoreDocId }

            if (setToUpdate != null) {
                val setDateStr =
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(setToUpdate.timestamp.toDate())
                val setDocRef = db.collection("users").document(userId)
                    .collection("dailyActivity").document(setDateStr)
                    .collection("recordedSets").document(firestoreDocId)

                val weightInKgForUpdate =
                    if (userUnitSystem == UnitSystem.IMPERIAL) newWeight * 0.45359237 else newWeight
                val updatedSetData = mapOf( // Use mapOf for type safety with update
                    "reps" to newReps.toLong(), // Firestore expects Long for integer numbers
                    "weight" to weightInKgForUpdate,
                    "notes" to newNotes
                    // timestamp, exerciseName, exerciseId are not changed on update
                )

                setDocRef.update(updatedSetData)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Set $firestoreDocId updated successfully.")
                        val index = exerciseItem.recordedSets.indexOf(setToUpdate)
                        if (index != -1) {
                            exerciseItem.recordedSets[index] = setToUpdate.copy(
                                reps = newReps.toString(),
                                weight = newWeight.toString(), // Keep in user's unit for local display
                                notes = newNotes
                            )
                            Log.d(
                                "LocalUpdate",
                                "Set $firestoreDocId updated locally for $exerciseName."
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "Firestore",
                            "Error updating set $firestoreDocId",
                            e
                        )
                    }
            }
        }
    }
}
