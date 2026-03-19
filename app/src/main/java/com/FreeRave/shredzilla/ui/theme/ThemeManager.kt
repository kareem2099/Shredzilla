package com.FreeRave.shredzilla.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.screens.settings.ThemeSetting // Import ThemeSetting enum

object ThemeManager {
    var currentGenderTheme: String? by mutableStateOf(null) // "Male", "Female", or null

    // These will be updated by MainAppContainer when preferences are loaded/changed
    var themePreferenceMale: ThemeSetting by mutableStateOf(ThemeSetting.SYSTEM)
    var themePreferenceFemale: ThemeSetting by mutableStateOf(ThemeSetting.SYSTEM)

    fun updateThemePreferenceForGender(gender: String?, newSetting: ThemeSetting) {
        when (gender) {
            "Male" -> themePreferenceMale = newSetting
            "Female" -> themePreferenceFemale = newSetting
            // else -> // Handle null or other gender strings if necessary, though currentGenderTheme should be Male/Female
        }
        // The change in these state objects should trigger recomposition where they are used.
    }

    fun getEffectiveThemeSetting(
        gender: String?,
        prefMale: ThemeSetting, // Pass from MainAppContainer's state
        prefFemale: ThemeSetting // Pass from MainAppContainer's state
    ): ThemeSetting {
        return when (gender) {
            "Male" -> prefMale
            "Female" -> prefFemale
            else -> ThemeSetting.SYSTEM // Default if gender is somehow null
        }
    }
}
