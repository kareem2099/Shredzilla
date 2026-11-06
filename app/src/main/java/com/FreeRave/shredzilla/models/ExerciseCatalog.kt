package com.FreeRave.shredzilla.models

/** One muscle group's exercises in the "Add exercise" catalog. */
data class CatalogGroup(val muscle: String, val exercises: List<String>)

/**
 * Common gym exercises to add with a tap. Names shared with [workoutPrograms] are spelled the
 * same, so they map to the same exercise id. Anything missing can still be created by name.
 */
val exerciseCatalog = listOf(
    CatalogGroup(
        "Legs",
        listOf(
            "Squat", "Leg Press", "Hack Squat", "Leg Extension", "Leg Curl", "Seated Leg Curl", "Romanian Deadlift",
            "Bulgarian Split Squat", "Lunge", "Hip Thrust", "Glute Bridge", "Glute Kickback", "Hip Abduction",
            "Hip Adduction", "Calf Raise", "Seated Calf Raise", "Single-Leg Press", "Single-Leg Extension", "Single-Leg Curl"
        )
    ),
    CatalogGroup(
        "Chest",
        listOf(
            "Bench Press", "Incline Bench Press", "Dumbbell Bench Press", "Incline Dumbbell Press", "Machine Chest Press",
            "Incline Machine Press", "Chest Fly", "Cable Crossover", "Dips", "Push-Up"
        )
    ),
    CatalogGroup(
        "Back",
        listOf(
            "Lat Pulldown", "Close-Grip Lat Pulldown", "Pull-Up", "Seated Row", "Close-Grip Row", "Wide-Grip Row",
            "T-Bar Row", "Barbell Row", "One-Arm Dumbbell Row", "Straight-Arm Pulldown", "Deadlift", "Back Extension"
        )
    ),
    CatalogGroup(
        "Shoulders",
        listOf(
            "Overhead Press", "Machine Shoulder Press", "Dumbbell Shoulder Press", "Lateral Raise", "Seated Lateral Raise",
            "Cable Lateral Raise", "Rear Delt Fly", "Face Pull", "Front Raise", "Shrug", "Dumbbell Shrug"
        )
    ),
    CatalogGroup(
        "Biceps",
        listOf("Bicep Curl", "Preacher Curl", "Incline Dumbbell Curl", "Hammer Curl", "Cable Curl", "Concentration Curl")
    ),
    CatalogGroup(
        "Triceps",
        listOf("Tricep Pushdown", "Overhead Tricep Extension", "Skull Crusher", "Close-Grip Bench Press", "Single-Arm Pushdown")
    ),
    CatalogGroup("Forearms", listOf("Wrist Curl", "Reverse Wrist Curl", "Farmer's Walk")),
    CatalogGroup(
        "Core",
        listOf("Cable Crunch", "Pallof Press", "Plank", "Hanging Leg Raise", "Crunch", "Ab Wheel Rollout", "Russian Twist")
    )
)
