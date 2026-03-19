package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border // Added border import
import androidx.compose.foundation.clickable // Added for clickable modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.FreeRave.shredzilla.ui.theme.MaleLightPrimary // Added import for dark green
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExerciseContent(
    modifier: Modifier = Modifier,
    searchQuery: MutableState<TextFieldValue>,
    popularExercisesList: SnapshotStateList<SelectableExercise>, // For displaying popular exercises
    currentExercises: List<String>, // Master list for existence checks
    onAddNewExerciseToList: (String) -> Unit, // Callback to add to master list
    onNavigateBack: () -> Unit, // To navigate back after creation or selection
    onSelectPopularExercise: (SelectableExercise) -> Unit // Callback when a popular exercise is selected/deselected
) {
    var showExerciseExistsDialog by remember { mutableStateOf(false) }
    val exerciseSearchText = searchQuery.value.text.trim()

    val canCreateNewExercise = exerciseSearchText.isNotBlank() &&
            currentExercises.none { it.equals(exerciseSearchText, ignoreCase = true) }

    val searchedExerciseExactlyExistsInCurrentList = exerciseSearchText.isNotBlank() &&
            currentExercises.any { it.equals(exerciseSearchText, ignoreCase = true) }

    // Filtered list for displaying popular exercises based on search
    val filteredPopularExercises = remember(searchQuery.value, popularExercisesList) {
        derivedStateOf {
            if (searchQuery.value.text.isBlank()) {
                popularExercisesList
            } else {
                popularExercisesList.filter {
                    it.name.contains(searchQuery.value.text, ignoreCase = true)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxSize()
    ) {
        // Row for "Create" text and Search Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create",
                color = if (canCreateNewExercise) MaleLightPrimary else MaterialTheme.colorScheme.onBackground, // Use MaleLightPrimary when active
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable {
                        if (canCreateNewExercise) {
                            onAddNewExerciseToList(exerciseSearchText)
                            searchQuery.value = TextFieldValue("") // Clear search
                            onNavigateBack() // Navigate back after creating
                        }
                    }
            )
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = {
                    searchQuery.value = it
                    if (it.text.isBlank()) showExerciseExistsDialog = false
                     // Check if exact match exists when text changes
                    if (it.text.isNotBlank() && currentExercises.any { ex -> ex.equals(it.text.trim(), ignoreCase = true) }) {
                        showExerciseExistsDialog = true
                    } else {
                        showExerciseExistsDialog = false
                    }
                },
                placeholder = { Text("Search or enter e...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .weight(1f) // Search bar takes remaining space
                    .clip(RoundedCornerShape(50)),
                // .background(MaterialTheme.colorScheme.surfaceVariant), // Let TextField colors handle container
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // Added for consistency
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // Added for consistency
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // Added for consistency
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
        }

        // Row for "Popularity"
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Popularity Icon (Placeholder - replace with actual progress-like icon if available)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder color
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape) // Placeholder border
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Popularity", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
        }


        if (showExerciseExistsDialog) {
            AlertDialog(
                onDismissRequest = { showExerciseExistsDialog = false },
                title = { Text("Exercise Exists", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("The exercise \"${exerciseSearchText}\" already exists.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = { showExerciseExistsDialog = false }) { Text("OK") } // TextButton colors adapt by default
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface, // Explicitly set for clarity, though often inherited
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Explicitly set for clarity
            )
        }

        // Conditional "Create Exercise" item
        if (canCreateNewExercise) {
            SelectableExerciseItem(
                name = exerciseSearchText,
                isSelected = false, // A "create" item is initially not "selected" in the context of a list
                onClick = {
                    onAddNewExerciseToList(exerciseSearchText)
                    searchQuery.value = TextFieldValue("") // Clear search
                    onNavigateBack() // Navigate back after creating
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }


        // List of popular exercises
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (filteredPopularExercises.value.isEmpty() && searchQuery.value.text.isNotBlank() && !canCreateNewExercise && !searchedExerciseExactlyExistsInCurrentList) {
                 item {
                     Text(
                        "No popular exercises match \"${searchQuery.value.text}\".",
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.padding(8.dp).fillMaxWidth(),
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center
                     )
                 }
            }

            items(filteredPopularExercises.value, key = { it.id }) { exerciseItem -> // Renamed to avoid confusion
                SelectableExerciseItem(
                    name = exerciseItem.name,
                    isSelected = exerciseItem.isSelected,
                    onClick = { onSelectPopularExercise(exerciseItem) } // Pass the full exerciseItem to the callback
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp) // Restoring HorizontalDivider
            }
        }
    }
}
