package com.FreeRave.shredzilla.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.FreeRave.shredzilla.models.BodyMeasurement
import com.FreeRave.shredzilla.models.PersonalRecord
import com.FreeRave.shredzilla.models.RecordSet
import com.FreeRave.shredzilla.models.SetLoad
import com.FreeRave.shredzilla.models.WorkoutExerciseSummary
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.models.WorkoutPr
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Date

/**
 * Finished workouts, all-time personal records and body measurements. Both are live listeners, so they work from the
 * cache offline and writes sync later like everything else.
 */
class TrainingHistoryStore(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        const val WORKOUTS = "workouts"
        const val RECORDS = "records"
        const val MEASUREMENTS = "measurements"
        private const val WORKOUT_HISTORY_LIMIT = 200L
    }

    var workoutLogs by mutableStateOf<List<WorkoutLog>>(emptyList())
        private set
    var personalRecords by mutableStateOf<Map<String, PersonalRecord>>(emptyMap())
        private set
    /** Newest first. */
    var measurements by mutableStateOf<List<BodyMeasurement>>(emptyList())
        private set
    /** False until the first records snapshot, so nobody mistakes "not loaded" for "no records". */
    var recordsLoaded by mutableStateOf(false)
        private set

    private val listeners = mutableListOf<ListenerRegistration>()
    private var userId: String? = null

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun start(uid: String?) {
        if (uid == userId) return
        reset()
        userId = uid ?: return
        listeners += userDoc(uid).collection(WORKOUTS)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .limit(WORKOUT_HISTORY_LIMIT)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Workouts listen failed", e)
                    return@addSnapshotListener
                }
                workoutLogs = snapshot?.documents?.mapNotNull(::workoutLogOf).orEmpty()
            }
        listeners += userDoc(uid).collection(RECORDS).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Records listen failed", e)
                return@addSnapshotListener
            }
            personalRecords = snapshot?.documents?.mapNotNull(::personalRecordOf).orEmpty().associateBy { it.exerciseId }
            recordsLoaded = true
        }
        listeners += userDoc(uid).collection(MEASUREMENTS)
            .orderBy("takenAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Measurements listen failed", e)
                    return@addSnapshotListener
                }
                measurements = snapshot?.documents?.mapNotNull(::measurementOf).orEmpty()
            }
    }

    fun reset() {
        listeners.forEach { it.remove() }
        listeners.clear()
        userId = null
        workoutLogs = emptyList()
        personalRecords = emptyMap()
        measurements = emptyList()
        recordsLoaded = false
    }

    fun saveWorkout(log: WorkoutLog) {
        val uid = userId ?: return
        val data = mapOf(
            "listName" to log.listName,
            "startedAt" to Timestamp(Date(log.startedAtMillis)),
            "durationMillis" to log.durationMillis,
            "setCount" to log.setCount,
            "volumeKg" to log.volumeKg,
            "exercises" to log.exercises.map { mapOf("exerciseId" to it.exerciseId, "name" to it.name, "sets" to it.sets) + loadFields(it.best) },
            "records" to log.records.map { mapOf("exerciseName" to it.exerciseName) + loadFields(it.load) }
        )
        userDoc(uid).collection(WORKOUTS).add(data)
            .addOnFailureListener { e -> Log.e("Firestore", "Error saving workout", e) }
    }

    fun deleteWorkout(id: String) {
        val uid = userId ?: return
        userDoc(uid).collection(WORKOUTS).document(id).delete()
    }

    /** Stores a new best of one kind (weighted or plates); the other kind is left as it is. */
    fun saveRecord(exerciseId: String, exerciseName: String, isPlates: Boolean, record: RecordSet) {
        val uid = userId ?: return
        val prefix = if (isPlates) "plates" else "weight"
        val data = mapOf(
            "name" to exerciseName,
            prefix to record.load,
            "${prefix}Reps" to record.reps,
            "${prefix}At" to Timestamp(Date(record.atMillis))
        )
        userDoc(uid).collection(RECORDS).document(exerciseId).set(data, SetOptions.merge())
    }

    /** For mistakes (a typo'd 600 kg set): the next history load re-adds the real best it can see. */
    fun deleteRecord(exerciseId: String) {
        val uid = userId ?: return
        userDoc(uid).collection(RECORDS).document(exerciseId).delete()
    }

    /** New entries get an id; editing an existing one replaces it. */
    fun saveMeasurement(measurement: BodyMeasurement) {
        val uid = userId ?: return
        val data = mapOf(
            "takenAt" to Timestamp(Date(measurement.takenAtMillis)),
            "values" to measurement.valuesCm,
            "bodyWeightKg" to measurement.bodyWeightKg,
            "note" to measurement.note
        )
        val collection = userDoc(uid).collection(MEASUREMENTS)
        val doc = if (measurement.id.isEmpty()) collection.document() else collection.document(measurement.id)
        doc.set(data).addOnFailureListener { e -> Log.e("Firestore", "Error saving measurement", e) }
    }

    fun deleteMeasurement(id: String) {
        val uid = userId ?: return
        userDoc(uid).collection(MEASUREMENTS).document(id).delete()
    }

    private fun measurementOf(doc: DocumentSnapshot): BodyMeasurement? {
        val takenAt = doc.getTimestamp("takenAt") ?: return null
        @Suppress("UNCHECKED_CAST")
        val values = (doc.get("values") as? Map<String, Number>).orEmpty().mapValues { it.value.toDouble() }
        return BodyMeasurement(doc.id, takenAt.toDate().time, values, doc.getDouble("bodyWeightKg"), doc.getString("note").orEmpty())
    }

    private fun loadFields(load: SetLoad?): Map<String, Any?> =
        if (load == null) emptyMap()
        else mapOf(
            "weightKg" to load.weightKg,
            "plates" to load.plates,
            "reps" to load.reps,
            "bodyweight" to load.bodyweight,
            "perSide" to load.perSide
        )

    private fun Map<*, *>.toSetLoad(): SetLoad? {
        val reps = (this["reps"] as? Number)?.toInt() ?: return null
        return SetLoad(
            weightKg = (this["weightKg"] as? Number)?.toDouble() ?: 0.0,
            plates = (this["plates"] as? Number)?.toInt(),
            reps = reps,
            bodyweight = this["bodyweight"] as? Boolean ?: false,
            perSide = this["perSide"] as? Boolean ?: false
        )
    }

    private fun workoutLogOf(doc: DocumentSnapshot): WorkoutLog? {
        val startedAt = doc.getTimestamp("startedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
        @Suppress("UNCHECKED_CAST")
        fun list(field: String) = (doc.get(field) as? List<Map<*, *>>).orEmpty()
        return WorkoutLog(
            id = doc.id,
            listName = doc.getString("listName").orEmpty(),
            startedAtMillis = startedAt.toDate().time,
            durationMillis = doc.getLong("durationMillis") ?: 0L,
            setCount = doc.getLong("setCount")?.toInt() ?: 0,
            volumeKg = doc.getDouble("volumeKg") ?: 0.0,
            exercises = list("exercises").map {
                WorkoutExerciseSummary(
                    exerciseId = it["exerciseId"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    sets = (it["sets"] as? Number)?.toInt() ?: 0,
                    best = it.toSetLoad()
                )
            },
            records = list("records").mapNotNull { map -> map.toSetLoad()?.let { WorkoutPr(map["exerciseName"] as? String ?: "", it) } }
        )
    }

    private fun personalRecordOf(doc: DocumentSnapshot): PersonalRecord? {
        fun recordOf(prefix: String): RecordSet? {
            val load = doc.getDouble(prefix) ?: return null
            return RecordSet(load, doc.getLong("${prefix}Reps")?.toInt() ?: 0, doc.getTimestamp("${prefix}At")?.toDate()?.time ?: 0L)
        }
        val heaviest = recordOf("weight")
        val mostPlates = recordOf("plates")
        if (heaviest == null && mostPlates == null) return null
        return PersonalRecord(doc.id, doc.getString("name") ?: doc.id, heaviest, mostPlates)
    }
}
