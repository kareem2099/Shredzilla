package com.FreeRave.shredzilla.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Timer // For the timer switch icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions // For numeric input
import androidx.compose.ui.text.input.KeyboardType // For numeric input
import androidx.compose.ui.unit.sp // For font size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.onboarding.RestTimeOptionCard // Assuming this is reusable
import com.FreeRave.shredzilla.onboarding.restOptions // Assuming this is accessible
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultRestTimeSettingsScreen(
    currentPreference: String, // e.g., "1min", "2min", "5min"
    onSave: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var timerEnabled by remember { mutableStateOf(true) } // For the Timer switch
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var selectedPresetId by remember { mutableStateOf(currentPreference) }

    // Initialize minutes and seconds based on currentPreference
    LaunchedEffect(currentPreference) {
        when (currentPreference) {
            "1min" -> { minutes = "1"; seconds = "0" }
            "2min" -> { minutes = "2"; seconds = "0" }
            "5min" -> { minutes = "5"; seconds = "0" }
            // Handle custom or other values if necessary
            else -> {
                // Attempt to parse if it's a custom format like "custom_MM_SSs" or just clear
                minutes = "" // Or parse from a custom format
                seconds = ""
            }
        }
    }
    
    // Update selectedPresetId if custom inputs match a preset
    LaunchedEffect(minutes, seconds) {
        val totalSeconds = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
        selectedPresetId = when (totalSeconds) {
            60 -> "1min"
            120 -> "2min"
            300 -> "5min"
            else -> "custom" // Indicate a custom time if no preset matches
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Default Interset Rest") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val finalPreference = if (selectedPresetId != "custom") {
                            selectedPresetId
                        } else {
                            // Construct a preference string for custom time, e.g., "custom_MM_SS"
                            // For now, let's try to map to existing "Xmin" if possible, or save as total seconds
                            val totalSec = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                            if (totalSec % 60 == 0 && totalSec / 60 in listOf(1,2,5)) {
                                "${totalSec/60}min"
                            } else {
                                // Fallback or new custom format, e.g. "custom_${totalSec}s"
                                // For simplicity, if not a preset, it won't save as a preset string.
                                // The logic in MainAppContainer for `startRestTimer` needs to handle this.
                                // For now, we only save if it matches a preset or is a new "Xmin" format.
                                // This part needs more robust handling for custom times.
                                 if (totalSec > 0) "${totalSec/60}min${if (totalSec%60 != 0) " ${totalSec%60}s" else ""}" else "1min" // Default if invalid
                            }
                        }
                        onSave(finalPreference)
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Timer", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = timerEnabled,
                    onCheckedChange = { timerEnabled = it },
                    thumbContent = if (timerEnabled) {
                        { Icon(imageVector = Icons.Filled.Timer, contentDescription = "Timer On", tint = MaterialTheme.colorScheme.primary) }
                    } else { null }
                )
            }
            HorizontalDivider()

            // Custom Time Inputs
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround // Or SpaceBetween
            ) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.length <= 2) minutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 20.sp),
                    enabled = timerEnabled
                )
                Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedTextField(
                    value = seconds,
                    onValueChange = { if (it.length <= 2) seconds = it.filter { c -> c.isDigit() } },
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 20.sp),
                    enabled = timerEnabled
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
             Text(
                text = "Select the interset rest duration you'd like to use the majority of the time.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall, // Made smaller
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp)) // Adjusted spacer

            Text(
                "DEFAULT RECOMMENDATIONS",
                style = MaterialTheme.typography.labelMedium, // Made slightly larger
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            restOptions.forEach { option ->
                RestTimeOptionCard(
                    option = option,
                    isSelected = selectedPresetId == option.id,
                    onOptionSelected = {
                        selectedPresetId = option.id
                        when (option.id) {
                            "1min" -> { minutes = "1"; seconds = "0" }
                            "2min" -> { minutes = "2"; seconds = "0" }
                            "5min" -> { minutes = "5"; seconds = "0" }
                        }
                        timerEnabled = true // Ensure timer is enabled when a preset is picked
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.weight(1f)) // Removed heightIn for flexible spacing
        }
    }
}

@Preview(showBackground = true, name = "Default Rest Time Settings")
@Composable
fun DefaultRestTimeSettingsScreenPreview() {
    ShredzillaTheme {
        DefaultRestTimeSettingsScreen(
            currentPreference = "2min",
            onSave = {},
            onNavigateBack = {}
        )
    }
}
