package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder // Placeholder icon
import androidx.compose.material.icons.filled.Check // For Done action
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.models.ExerciseItem // Assuming a similar structure for selectable exercises
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

data class SelectableExerciseForList(
    val id: String, // From commonExercises
    val name: String,
    var isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewListScreen(
    allExercises: List<ExerciseItem>,
    onNavigateBack: () -> Unit,
    onSaveList: (listId: String?, listName: String, selectedExerciseIds: List<String>) -> Unit, // Added listId for updates
    initialListId: String? = null, // For editing
    initialListName: String? = null, // For editing
    initialSelectedExerciseIds: List<String>? = null // For editing
) {
    var folderName by remember { mutableStateOf(initialListName ?: "") }
    val isEditMode = initialListId != null

    val selectableExercises = remember {
        mutableStateListOf(
            *allExercises.map { exercise ->
                SelectableExerciseForList(
                    id = exercise.id, 
                    name = exercise.name, 
                    isSelected = initialSelectedExerciseIds?.contains(exercise.id) ?: false
                )
            }.toTypedArray()
        )
    }
    
    // Update selections if initialSelectedExerciseIds changes (e.g., when screen recomposes for editing)
    LaunchedEffect(initialSelectedExerciseIds) {
        if (isEditMode) {
            selectableExercises.forEachIndexed { index, item ->
                if (initialSelectedExerciseIds?.contains(item.id) == true && !item.isSelected) {
                    selectableExercises[index] = item.copy(isSelected = true)
                } else if (initialSelectedExerciseIds?.contains(item.id) == false && item.isSelected) {
                     selectableExercises[index] = item.copy(isSelected = false)
                }
            }
        }
    }
     LaunchedEffect(initialListName) {
        if (isEditMode) {
            folderName = initialListName ?: ""
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Folder" else "New Folder") },
                navigationIcon = {
                     // Standard back navigation
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // "Done" button on the right for saving
                    TextButton(onClick = {
                        val selectedIds = selectableExercises.filter { it.isSelected }.map { it.id }
                        if (folderName.isNotBlank() && (selectedIds.isNotEmpty() || isEditMode)) { // Allow saving empty list if editing
                            onSaveList(initialListId, folderName, selectedIds)
                        } else {
                            // TODO: Show a toast or error message (e.g., name required, at least one exercise for new list)
                        }
                    }) {
                        Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                imageVector = Icons.Filled.Folder, // Placeholder
                contentDescription = "New Folder Icon",
                modifier = Modifier.size(60.dp).padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("NAME") },
                placeholder = { Text("...Upper Body, Monday, Triceps") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = "Common naming conventions include: Workout Name, Muscle Group, Day of the Week, etc",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Text(
                "SELECTIONS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(selectableExercises) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val index = selectableExercises.indexOf(exercise)
                                if (index != -1) {
                                    selectableExercises[index] = exercise.copy(isSelected = !exercise.isSelected)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = exercise.isSelected,
                            onClick = {
                                val index = selectableExercises.indexOf(exercise)
                                if (index != -1) {
                                    selectableExercises[index] = exercise.copy(isSelected = !exercise.isSelected)
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateNewListScreenPreview() {
    ShredzillaTheme {
        CreateNewListScreen(
            allExercises = listOf(
                ExerciseItem(id = "bench_press", name = "Bench Press", description = "", videoUrl = null, targetMuscles = emptyList(), equipmentNeeded = emptyList(), difficulty = ""),
                ExerciseItem(id = "squat", name = "Squat", description = "", videoUrl = null, targetMuscles = emptyList(), equipmentNeeded = emptyList(), difficulty = ""),
                ExerciseItem(id = "deadlift", name = "Deadlift", description = "", videoUrl = null, targetMuscles = emptyList(), equipmentNeeded = emptyList(), difficulty = ""),
            ),
            onNavigateBack = {},
            onSaveList = { _, _, _ -> },
            initialListName = "My Old Workout",
            initialListId = "someListId",
            initialSelectedExerciseIds = listOf("bench_press", "deadlift")
        )
    }
}
