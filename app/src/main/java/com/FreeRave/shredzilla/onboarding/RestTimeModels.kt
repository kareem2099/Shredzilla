package com.FreeRave.shredzilla.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

data class RestOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

val restOptions = listOf(
    RestOption("1min", "minute 1", "More sets, less joint stress\nQuick Workouts", Icons.Filled.LocalFireDepartment),
    RestOption("2min", "minute 2", "Get toned/bigger\nBuild Muscle", Icons.Filled.FitnessCenter),
    RestOption("5min", "minute 5", "Maximize muscular power\nIncrease Strength", Icons.Filled.Timer)
)
