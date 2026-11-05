package com.FreeRave.shredzilla.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

import com.google.firebase.Timestamp // Import Firebase Timestamp

/**
 * One set as stored in Firestore under users/{uid}/dailyActivity/{dateKey}/recordedSets/{docId}.
 * [dateKey] ("yyyy-MM-dd") plus [docId] is everything needed to edit or delete it.
 */
data class LoggedSet(
    val docId: String,
    val dateKey: String,
    val exerciseId: String,
    val reps: Int,
    val weightKg: Double,
    val notes: String,
    val timestampMillis: Long,
    /** Set on weight-stack machines logged by plate count; [weightKg] is 0 then. */
    val plates: Int? = null
) {
    /** What the chart sums: kg for free weights, plates for stack machines. */
    val volume: Double get() = reps * (plates?.toDouble() ?: weightKg)
}

/**
 * What the user enters for one set. [weight] is in the user's unit; weight-stack machines are
 * logged as a [plates] count instead, and [weight] is ignored then.
 */
data class SetEntry(
    val reps: Int = 1,
    val weight: Double = 0.0,
    val plates: Int? = null,
    val notes: String = ""
)

// Data class for an exercise item
data class ExerciseItem(
    val id: String,
    val name: String,
    val description: String,
    val videoUrl: String?, // Nullable if not all exercises have videos
    val targetMuscles: List<String>,
    val equipmentNeeded: List<String>,
    val difficulty: String, // e.g., "Beginner", "Intermediate", "Advanced"
    var isSelected: Boolean = false // isSelected might be more of a UI concern for selection screens
)

// List of initial exercises - this can be the master list
val initialGlobalExerciseList: SnapshotStateList<ExerciseItem> = mutableStateListOf(
    ExerciseItem(
        id = "bench_press",
        name = "Bench Press",
        description = "A compound exercise that targets the pectoralis major, anterior deltoids, and triceps. Lie on a bench with feet flat on the floor. Grip the barbell slightly wider than shoulder-width. Lower the bar to your mid-chest and press it back up.",
        videoUrl = "https://cdnl.iconscout.com/lottie/premium/thumb/man-doing-barbell-bench-press-exercise-for-chest-animation-download-in-lottie-json-gif-static-svg-file-formats--front-men-workout-male-gym-exercises-pack-fitness-animations-8971869.mp4", // Example video URL
        targetMuscles = listOf("Chest", "Shoulders", "Triceps"),
        equipmentNeeded = listOf("Barbell", "Bench"),
        difficulty = "Intermediate"
    ),
    ExerciseItem(
        id = "squat",
        name = "Squat",
        description = "A compound exercise that primarily targets the quadriceps, hamstrings, and glutes. Stand with feet shoulder-width apart, barbell resting on your upper back. Lower your hips as if sitting in a chair, keeping your back straight and chest up. Go as low as comfortable and return to the starting position.",
        videoUrl = "https://www.youtube.com/watch?v=ultWZbUMPL8", // Example video URL
        targetMuscles = listOf("Quadriceps", "Hamstrings", "Glutes", "Core"),
        equipmentNeeded = listOf("Barbell", "Squat Rack (optional)"),
        difficulty = "Intermediate"
    ),
    ExerciseItem(
        id = "deadlift",
        name = "Deadlift",
        description = "A compound exercise that works multiple muscle groups, including the back, legs, and core. Stand with mid-foot under the barbell. Bend at the hips and knees to grip the bar, keeping your back straight. Lift the bar by extending your hips and knees, keeping the bar close to your body.",
        videoUrl = "https://www.youtube.com/watch?v=ytGaGIn3SjE", // Example video URL
        targetMuscles = listOf("Back", "Hamstrings", "Glutes", "Traps", "Forearms"),
        equipmentNeeded = listOf("Barbell"),
        difficulty = "Advanced"
    ),
    ExerciseItem(
        id = "pull_up",
        name = "Pull-Up",
        description = "An upper-body compound exercise that targets the latissimus dorsi, biceps, and forearms. Hang from a pull-up bar with an overhand grip, slightly wider than shoulder-width. Pull your body up until your chin is over the bar. Lower yourself slowly.",
        videoUrl = "https://www.youtube.com/watch?v=eGo4IYlbE5g", // Example video URL
        targetMuscles = listOf("Lats", "Biceps", "Forearms", "Upper Back"),
        equipmentNeeded = listOf("Pull-up Bar"),
        difficulty = "Intermediate"
    ),
    ExerciseItem(
        id = "push_up",
        name = "Push-Up",
        description = "A bodyweight exercise that targets the chest, shoulders, and triceps. Start in a plank position with hands shoulder-width apart. Lower your body until your chest nearly touches the floor, then push back up.",
        videoUrl = "https://www.youtube.com/watch?v=IODxDxX7oi4", // Example video URL
        targetMuscles = listOf("Chest", "Shoulders", "Triceps", "Core"),
        equipmentNeeded = listOf("None"),
        difficulty = "Beginner"
    )
// Add more exercises here if the app supports a larger predefined list
)

// Data class for a user's exercise list
data class UserExerciseList(
    val id: String,
    val name: String,
    val exerciseIds: List<String>,
    val lastPerformed: Timestamp? = null,
    /** Suggested sets per exercise when the list came from a program; null for hand-made lists. */
    val targetSetsMin: Int? = null,
    val targetSetsMax: Int? = null
)

// Data class for a recorded set
data class RecordedSet(
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weight: Float,
    val notes: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val exerciseId: String,
    val firestoreDocId: String? = null,
    val plates: Int? = null
)

/** Firestore document id / analytics key for an exercise name, e.g. "Bench Press" -> "bench_press". */
fun exerciseIdOf(name: String): String = name.lowercase().replace(" ", "_")

// Data class for exercise display information
data class ExerciseDisplayInfo(
    val id: String,
    val name: String,
    val lastPerformed: String? = null,
    val lastPerformedMillis: Long? = null, // for sorting; lastPerformed is the human label
    val recordedSets: List<RecordedSet> = emptyList()
)
