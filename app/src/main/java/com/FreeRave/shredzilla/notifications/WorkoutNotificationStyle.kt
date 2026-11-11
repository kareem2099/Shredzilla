package com.FreeRave.shredzilla.notifications

/** How the live workout notification looks. Detailed and Compact are free; the other two are Pro, like accent themes. */
enum class WorkoutNotificationStyle(
    val storedValue: String,
    val title: String,
    val description: String,
    val isPro: Boolean,
) {
    DETAILED("detailed", "Detailed", "Next set, today's sets, what's next and your totals, with three buttons", false),
    COMPACT("compact", "Compact", "One line and two buttons. Just log and go", false),
    FOCUS("focus", "Rest focus", "The rest countdown up front, with Log, +1 rep and Skip rest", true),
    PROGRESS("progress", "Progress bar", "A bar across the whole workout. On Android 16 it's a live chip in the status bar", true);

    companion object {
        fun fromStored(value: String?): WorkoutNotificationStyle = entries.find { it.storedValue == value } ?: DETAILED
    }
}
