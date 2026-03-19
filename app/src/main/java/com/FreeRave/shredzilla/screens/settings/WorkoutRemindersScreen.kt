package com.FreeRave.shredzilla.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive // Or a more fitting icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

// TODO: Import a way to save/load this preference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRemindersScreen(
    onNavigateBack: () -> Unit,
    // TODO: Pass current preference and a lambda to save it
    currentReminderSetting: String, // e.g., "Never", "1 day inactive"
    onReminderSettingChange: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentReminderSetting) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    val reminderOptions = listOf(
        "Never", 
        "1 day inactive", 
        "2 days inactive", 
        "3 days inactive", 
        "4 days inactive", 
        "5 days inactive", 
        "6 days inactive", 
        "7 days inactive",
        "8 days inactive" 
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Reminders") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive, // Placeholder icon
                contentDescription = "Workout Reminder Icon",
                modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Muscle growth slows down after long periods of inactivity.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "Get a notification when it's been too long since your last workout.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text("Remind after:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = showDropdownMenu,
                onExpandedChange = { showDropdownMenu = !showDropdownMenu }
            ) {
                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Inactive Period") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdownMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false }
                ) {
                    reminderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedOption = option
                                onReminderSettingChange(option)
                                showDropdownMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutRemindersScreenPreview() {
    ShredzillaTheme {
        WorkoutRemindersScreen(
            onNavigateBack = {},
            currentReminderSetting = "3 days inactive",
            onReminderSettingChange = {}
        )
    }
}
