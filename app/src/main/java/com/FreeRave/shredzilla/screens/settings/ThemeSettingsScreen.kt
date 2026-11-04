package com.FreeRave.shredzilla.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassOptionCard
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

/** Stored by [name] in Firestore ("themePreferenceMale"/"themePreferenceFemale"), so keep names stable. */
enum class ThemeSetting(val displayName: String, val description: String, val icon: ImageVector) {
    LIGHT("Light", "Bright glass, easy in daylight", Icons.Filled.LightMode),
    DARK("Dark", "Deep glass, easy on the eyes", Icons.Filled.DarkMode),
    SYSTEM("System default", "Follow your phone's setting", Icons.Filled.BrightnessAuto)
}

/** Theme picker; applies instantly so the user sees the result while choosing. */
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    currentThemeSetting: ThemeSetting,
    onThemeSettingChange: (ThemeSetting) -> Unit
) {
    var selected by remember { mutableStateOf(currentThemeSetting) }

    GlassSubpageScaffold(title = "Theme", eyebrow = "Settings", onBack = onNavigateBack) {
        ThemeSetting.entries.forEach { setting ->
            GlassOptionCard(
                icon = setting.icon,
                title = setting.displayName,
                subtitle = setting.description,
                selected = selected == setting,
                onClick = {
                    selected = setting
                    onThemeSettingChange(setting)
                }
            )
        }
        GlassFootnote("Your colours always follow the profile you picked during setup.")
    }
}

@Preview
@Composable
private fun ThemeSettingsScreenPreview() {
    ShredzillaTheme {
        ThemeSettingsScreen(onNavigateBack = {}, currentThemeSetting = ThemeSetting.SYSTEM, onThemeSettingChange = {})
    }
}
