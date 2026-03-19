package com.FreeRave.shredzilla.screens.exercises

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.shredzilla.screens.exercises.composables.AddExerciseContent
import com.FreeRave.shredzilla.screens.exercises.composables.SelectableExercise
import com.FreeRave.shredzilla.screens.exercises.composables.SelectableExerciseItem
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    currentExercises: List<String>,
    onAddNewExerciseToList: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onAddSelectedExercises: (List<SelectableExercise>) -> Unit // For adding selected popular exercises
) {
    val searchQueryState = remember { mutableStateOf(TextFieldValue("")) }

    val popularExercisesList = remember {
        mutableStateListOf(
            SelectableExercise("Bench Press", "bench_press_popular", isSelected = false),
            SelectableExercise("Squat", "squat_popular", isSelected = false),
            SelectableExercise("Deadlift", "deadlift_popular", isSelected = false),
            SelectableExercise("Lat Pulldown", "lat_pulldown_popular"),
            SelectableExercise("Incline Dumbbell Press", "incline_dumbbell_press_popular"),
            SelectableExercise("Leg Extension", "leg_extension_popular"),
            SelectableExercise("Incline Bench Press", "incline_bench_press_popular"),
            SelectableExercise("Pull-up", "pull_up_popular")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    // "ADD" button removed from TopAppBar.
                    // If functionality to add selected popular items is needed,
                    // it could be a separate button within AddExerciseContent or triggered on navigateBack.
                    // For now, onAddSelectedExercises is called by AddExerciseContent if it were to have an ADD button for popular items.
                    // The current AddExerciseContent doesn't have such a button, it relies on onSelectPopularExercise for individual toggles.
                    // The TopAppBar "ADD" button in the screenshot is not present.
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = { paddingValues ->
            AddExerciseContent(
                modifier = Modifier.padding(paddingValues),
                searchQuery = searchQueryState,
                popularExercisesList = popularExercisesList,
                currentExercises = currentExercises,
                onAddNewExerciseToList = onAddNewExerciseToList,
                onNavigateBack = onNavigateBack,
                onSelectPopularExercise = { selectedExercise ->
                    val index = popularExercisesList.indexOfFirst { it.id == selectedExercise.id }
                    if (index != -1) {
                        val updatedExercise = selectedExercise.copy(isSelected = !selectedExercise.isSelected)
                        popularExercisesList[index] = updatedExercise
                        
                        // If the exercise is being selected (not deselected), add it to commonExercises
                        if (updatedExercise.isSelected) {
                            onAddNewExerciseToList(updatedExercise.name)
                        }
                        // If you also want to add to a temporary workout list for the current session:
                        // onAddSelectedExercises(listOf(updatedExercise)) 
                        // However, the primary goal here is to add to commonExercises via onAddNewExerciseToList.
                    }
                }
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun AddExerciseScreenPreview() {
    ShredzillaTheme { // Removed darkTheme = true
        AddExerciseScreen(
            currentExercises = listOf("Bench Press", "Existing Exercise"),
            onAddNewExerciseToList = {},
            onNavigateBack = {},
            onAddSelectedExercises = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SelectableExerciseItemPreviewSelectedInScreen() {
    ShredzillaTheme { // Removed darkTheme = true
        SelectableExerciseItem(
            name = "Bench Press",
            isSelected = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SelectableExerciseItemPreviewUnselectedInScreen() {
    ShredzillaTheme { // Removed darkTheme = true
        SelectableExerciseItem(
            name = "Squat",
            isSelected = false,
            onClick = {}
        )
    }
}
