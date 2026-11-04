package com.FreeRave.shredzilla.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/** A rest-time preset. [id] is what gets stored in Firestore ("restTimePreference"), so keep it stable. */
data class RestOption(
    val id: String,
    val duration: String,
    val goal: String,
    val description: String,
    val icon: ImageVector,
    val isPopular: Boolean = false
)

val restOptions = listOf(
    RestOption("1min", "1 min", "Quick workouts", "More sets, less joint stress", Icons.Filled.LocalFireDepartment),
    RestOption("2min", "2 min", "Build muscle", "Get toned and bigger", Icons.Filled.FitnessCenter, isPopular = true),
    RestOption("5min", "5 min", "Increase strength", "Maximise muscular power", Icons.Filled.Timer)
)
