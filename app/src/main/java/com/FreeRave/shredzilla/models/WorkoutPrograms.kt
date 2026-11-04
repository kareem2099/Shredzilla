package com.FreeRave.shredzilla.models

/**
 * Suggested sets per exercise for each experience level. Only a guide shown next to each exercise:
 * users log as many or as few sets as they like.
 */
enum class TrainingLevel(val label: String, val minSets: Int, val maxSets: Int, val description: String) {
    BEGINNER("Beginner", 4, 4, "More practice sets while you learn the movements"),
    INTERMEDIATE("Intermediate", 3, 4, "Solid volume with room to push harder"),
    ADVANCED("Advanced", 1, 2, "Fewer sets, every one taken close to failure");

    val setsLabel: String get() = setsRangeLabel(minSets, maxSets)
}

/** "4 sets", "3–4 sets". */
fun setsRangeLabel(min: Int, max: Int): String = if (min == max) "$min sets" else "$min–$max sets"

data class ProgramDay(val name: String, val exercises: List<String>)

/** A ready-made split. Adding it creates one workout list per [days] entry. */
data class WorkoutProgram(
    val id: String,
    val name: String,
    /** Prefix for the lists it creates, e.g. "PPL · Push". */
    val shortName: String,
    val tagline: String,
    val daysPerWeek: String,
    val days: List<ProgramDay>
) {
    val allExercises: List<String> get() = days.flatMap { it.exercises }.distinct()
}

val workoutPrograms = listOf(
    WorkoutProgram(
        id = "full_body", name = "Full Body", shortName = "Full Body",
        tagline = "Two alternating whole-body sessions. Great for starting out or busy weeks.",
        daysPerWeek = "3 days",
        days = listOf(
            ProgramDay("A", listOf("Squat", "Bench Press", "Barbell Row", "Overhead Press", "Bicep Curl")),
            ProgramDay("B", listOf("Deadlift", "Incline Dumbbell Press", "Lat Pulldown", "Leg Press", "Tricep Pushdown"))
        )
    ),
    WorkoutProgram(
        id = "upper_lower", name = "Upper / Lower", shortName = "Upper/Lower",
        tagline = "Each muscle twice a week with a simple upper and lower day.",
        daysPerWeek = "4 days",
        days = listOf(
            ProgramDay("Upper", listOf("Bench Press", "Barbell Row", "Overhead Press", "Lat Pulldown", "Bicep Curl", "Tricep Pushdown")),
            ProgramDay("Lower", listOf("Squat", "Romanian Deadlift", "Leg Press", "Leg Curl", "Calf Raise"))
        )
    ),
    WorkoutProgram(
        id = "ppl", name = "Push / Pull / Legs", shortName = "PPL",
        tagline = "Run it once (3 days) or twice (6 days) a week.",
        daysPerWeek = "3 or 6 days",
        days = listOf(
            ProgramDay("Push", listOf("Bench Press", "Incline Dumbbell Press", "Overhead Press", "Lateral Raise", "Tricep Pushdown")),
            ProgramDay("Pull", listOf("Deadlift", "Lat Pulldown", "Barbell Row", "Face Pull", "Bicep Curl")),
            ProgramDay("Legs", listOf("Squat", "Romanian Deadlift", "Leg Press", "Leg Curl", "Calf Raise"))
        )
    ),
    WorkoutProgram(
        id = "body_part", name = "Body Part Split", shortName = "Split",
        tagline = "One focus per day for maximum attention on each muscle.",
        daysPerWeek = "5 days",
        days = listOf(
            ProgramDay("Chest", listOf("Bench Press", "Incline Dumbbell Press", "Chest Fly", "Push-Up")),
            ProgramDay("Back", listOf("Deadlift", "Pull-Up", "Barbell Row", "Lat Pulldown")),
            ProgramDay("Shoulders", listOf("Overhead Press", "Lateral Raise", "Face Pull", "Shrug")),
            ProgramDay("Legs", listOf("Squat", "Leg Press", "Leg Extension", "Leg Curl", "Calf Raise")),
            ProgramDay("Arms", listOf("Bicep Curl", "Hammer Curl", "Tricep Pushdown", "Skull Crusher"))
        )
    ),
    WorkoutProgram(
        id = "glutes_lower", name = "Glutes & Lower Focus", shortName = "Glutes",
        tagline = "Two lower-body days built around the glutes, plus two upper days.",
        daysPerWeek = "4 days",
        days = listOf(
            ProgramDay("Lower A", listOf("Hip Thrust", "Squat", "Romanian Deadlift", "Glute Kickback")),
            ProgramDay("Upper A", listOf("Lat Pulldown", "Overhead Press", "Seated Row", "Lateral Raise")),
            ProgramDay("Lower B", listOf("Bulgarian Split Squat", "Leg Press", "Leg Curl", "Hip Abduction")),
            ProgramDay("Upper B", listOf("Incline Dumbbell Press", "Barbell Row", "Bicep Curl", "Tricep Pushdown"))
        )
    ),
    WorkoutProgram(
        id = "home", name = "Home, No Equipment", shortName = "Home",
        tagline = "Bodyweight full-body sessions you can do anywhere.",
        daysPerWeek = "3 days",
        days = listOf(
            ProgramDay("Full Body", listOf("Push-Up", "Bodyweight Squat", "Lunge", "Pike Push-Up", "Glute Bridge", "Plank"))
        )
    )
)

/** Best starting split for how many days a week the user said they'd train during onboarding. */
fun recommendedProgramId(daysPerWeek: Int?): String? = when (daysPerWeek) {
    null -> null
    in 1..3 -> "full_body"
    4 -> "upper_lower"
    5 -> "body_part"
    else -> "ppl"
}
