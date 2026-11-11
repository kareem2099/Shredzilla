package com.FreeRave.shredzilla.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A route a tapped notification asked for; the main app navigates to it once it's ready, then clears it. */
object PendingNavigation {
    var route: String? by mutableStateOf(null)
    /** The workout notification's Finish: save the running workout once the app is up. */
    var finishWorkout: Boolean by mutableStateOf(false)
}
