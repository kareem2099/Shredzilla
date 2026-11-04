package com.FreeRave.shredzilla.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Sets : BottomNavItem(AppRoutes.SETS, "Sets", Icons.Filled.FitnessCenter)
    object Today : BottomNavItem("today_screen", "Today", Icons.Filled.CalendarToday)
    object Settings : BottomNavItem(AppRoutes.SETTINGS, "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(BottomNavItem.Sets, BottomNavItem.Today, BottomNavItem.Settings)
