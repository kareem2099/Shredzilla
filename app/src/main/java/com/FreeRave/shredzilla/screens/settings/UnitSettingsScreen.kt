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
// TODO: Import a way to save/load this preference (e.g., SharedPreferences helper or ViewModel)

enum class UnitSystem { METRIC, IMPERIAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSettingsScreen(
    onNavigateBack: () -> Unit,
    // TODO: Pass current preference and a lambda to save it
    currentUnitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(currentUnitSystem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Units of Measurement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Select your preferred units:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            UnitOptionRow(
                text = "Metric (kg, km)",
                selected = selectedUnit == UnitSystem.METRIC,
                onClick = {
                    selectedUnit = UnitSystem.METRIC
                    onUnitSystemChange(UnitSystem.METRIC)
                }
            )
            HorizontalDivider()
            UnitOptionRow(
                text = "Imperial (lbs, miles)",
                selected = selectedUnit == UnitSystem.IMPERIAL,
                onClick = {
                    selectedUnit = UnitSystem.IMPERIAL
                    onUnitSystemChange(UnitSystem.IMPERIAL)
                }
            )
        }
    }
}

@Composable
private fun UnitOptionRow(
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

@Preview(showBackground = true)
@Composable
fun UnitSettingsScreenPreviewMetric() {
    ShredzillaTheme {
        UnitSettingsScreen(onNavigateBack = {}, currentUnitSystem = UnitSystem.METRIC, onUnitSystemChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun UnitSettingsScreenPreviewImperial() {
    ShredzillaTheme {
        UnitSettingsScreen(onNavigateBack = {}, currentUnitSystem = UnitSystem.IMPERIAL, onUnitSystemChange = {})
    }
}
