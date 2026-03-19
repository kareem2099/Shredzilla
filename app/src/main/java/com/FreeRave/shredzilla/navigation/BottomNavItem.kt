package com.FreeRave.shredzilla.navigation

import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.ListAlt // No longer using ListAlt
import androidx.compose.material.icons.filled.CalendarToday // Placeholder for "Today"
import androidx.compose.material.icons.filled.FitnessCenter // Icon for "Sets"
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Sets : BottomNavItem( // Renamed from Exercises to Sets
        route = AppRoutes.SETS, // Will need to define AppRoutes.SETS
        label = "Sets",         // Renamed label
        icon = Icons.Filled.FitnessCenter // Using FitnessCenter icon
    )

    object Today : BottomNavItem(
        route = "today_screen", // Keeping this distinct for now, can be AppRoutes.TODAY if unified
        label = "Today",
        icon = Icons.Filled.CalendarToday
    )
    // Add other items like "Profile", "Settings" later if needed
}

val bottomNavItems = listOf(
    BottomNavItem.Sets, // Updated to Sets
    BottomNavItem.Today
)
