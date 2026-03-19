package com.FreeRave.shredzilla.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

enum class ThemeSetting(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    currentThemeSetting: ThemeSetting,
    onThemeSettingChange: (ThemeSetting) -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentThemeSetting) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Theme") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                // No Save button, selection is instant
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ThemeSettingOptionRow(
                text = ThemeSetting.LIGHT.displayName,
                selected = selectedOption == ThemeSetting.LIGHT,
                onClick = {
                    selectedOption = ThemeSetting.LIGHT
                    onThemeSettingChange(ThemeSetting.LIGHT)
                }
            )
            HorizontalDivider()
            ThemeSettingOptionRow(
                text = ThemeSetting.DARK.displayName,
                selected = selectedOption == ThemeSetting.DARK,
                onClick = {
                    selectedOption = ThemeSetting.DARK
                    onThemeSettingChange(ThemeSetting.DARK)
                }
            )
            HorizontalDivider()
            ThemeSettingOptionRow(
                text = ThemeSetting.SYSTEM.displayName,
                selected = selectedOption == ThemeSetting.SYSTEM,
                onClick = {
                    selectedOption = ThemeSetting.SYSTEM
                    onThemeSettingChange(ThemeSetting.SYSTEM)
                }
            )
        }
    }
}

@Composable
private fun ThemeSettingOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true, name = "Theme Settings - System")
@Composable
fun ThemeSettingsScreenPreviewSystem() {
    ShredzillaTheme {
        ThemeSettingsScreen(
            onNavigateBack = {},
            currentThemeSetting = ThemeSetting.SYSTEM,
            onThemeSettingChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Theme Settings - Dark")
@Composable
fun ThemeSettingsScreenPreviewDark() {
    ShredzillaTheme { // Removed darkTheme = true
        ThemeSettingsScreen(
            onNavigateBack = {},
            currentThemeSetting = ThemeSetting.DARK,
            onThemeSettingChange = {}
        )
    }
}
