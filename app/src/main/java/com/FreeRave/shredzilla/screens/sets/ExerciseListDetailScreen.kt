package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // For FAB
// import androidx.compose.material.icons.filled.Search // Removing search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
// import androidx.compose.ui.text.input.TextFieldValue // No longer needed for search
import androidx.compose.ui.text.style.TextAlign // Ensure this import is present
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController // Added for navigation
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.navigation.AppRoutes // Added for navigation routes
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheetContent
import com.FreeRave.shredzilla.screens.exercises.composables.SwipableExerciseListItem
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListDetailScreen(
    listName: String,
    exercisesDisplayInfo: List<ExerciseDisplayInfo>,
    onNavigateBack: () -> Unit,
    onDeleteExerciseFromList: (exerciseName: String, listId: String) -> Unit, // Might need listId
    onRecordExercise: (exerciseName: String, reps: Int, weight: Double, notes: String) -> Unit,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit,
    unitSystem: UnitSystem,
    listId: String,
    onNavigateToEditList: (listId: String, listName: String) -> Unit, // Callback for FAB
    navController: NavHostController // Added NavController
) {
    // var searchQuery by remember { mutableStateOf(TextFieldValue("")) } // Removing search
    // var selectedSortOption by remember { mutableStateOf<SortOption?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedExerciseForRecord by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<String?>(null) }

    // val displayedExercises = remember(searchQuery, exercisesDisplayInfo) { // Removing search
    val displayedExercises = remember(exercisesDisplayInfo) { 
        derivedStateOf {
            // if (searchQuery.text.isBlank()) { // Removing search
                exercisesDisplayInfo
            // } else {
            //     exercisesDisplayInfo.filter {
            //         it.name.contains(searchQuery.text, ignoreCase = true)
            //     }
            // }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(listName, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditList(listId, listName) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit List")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isTimerRunning) {
                RestTimerBar(
                    remainingSeconds = timerRemainingSeconds,
                    totalSeconds = timerTotalSeconds,
                    onClose = onCloseTimer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp) // Added top padding after removing search
                    .padding(bottom = 8.dp)
                    .fillMaxSize()
            ) {
                // Search Bar Removed
                // Spacer(modifier = Modifier.height(16.dp)) // No longer needed if search is gone

                if (displayedExercises.value.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This list is empty. Tap the edit button to add exercises.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedExercises.value, key = { it.name }) { exerciseInfo ->
                            SwipableExerciseListItem(
                                exerciseName = exerciseInfo.name,
                                lastPerformed = exerciseInfo.lastPerformed,
                                onDelete = {
                                    exerciseToDelete = exerciseInfo.name
                                    showDeleteConfirmDialog = true
                                },
                                onRecord = {
                                    selectedExerciseForRecord = exerciseInfo.name
                                    showBottomSheet = true
                                },
                                onClick = {
                                    val exerciseId = exerciseInfo.name.lowercase().replace(" ", "_")
                                    navController.navigate(AppRoutes.EXERCISE_DETAIL.replace("{exerciseId}", exerciseId))
                                },
                                swipeEnabled = !isTimerRunning && !showBottomSheet && !showDeleteConfirmDialog
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            RecordSetSheetContent(
                exerciseName = selectedExerciseForRecord ?: "Exercise",
                onRecordSet = { reps, weight, notes ->
                    selectedExerciseForRecord?.let { exerciseName ->
                        onRecordExercise(exerciseName, reps, weight, notes)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) { showBottomSheet = false }
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) { showBottomSheet = false }
                    }
                },
                unitSystem = unitSystem
            )
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Remove from List") }, // Title changed
            text = { Text("Are you sure you want to remove '${exerciseToDelete ?: "this exercise"}' from this list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exerciseToDelete?.let { onDeleteExerciseFromList(it, listId) }
                        showDeleteConfirmDialog = false
                        exerciseToDelete = null
                    }
                ) { Text("Yes, Remove") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    exerciseToDelete = null
                }) { Text("No") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseListDetailScreenPreview() {
    val previewNavController = androidx.navigation.compose.rememberNavController() // Dummy for preview
    ShredzillaTheme {
        ExerciseListDetailScreen(
            listName = "My Push Day",
            exercisesDisplayInfo = listOf(
                ExerciseDisplayInfo(id = "bench_press", name = "Bench Press", lastPerformed = "Today"),
                ExerciseDisplayInfo(id = "incline_dumbbell_press", name = "Incline Dumbbell Press", lastPerformed = "Yesterday"),
                ExerciseDisplayInfo(id = "tricep_pushdown", name = "Tricep Pushdown", lastPerformed = "3 days ago")
            ),
            onNavigateBack = {},
            onDeleteExerciseFromList = { _, _ -> },
            onRecordExercise = { _, _, _, _ -> },
            isTimerRunning = false,
            timerRemainingSeconds = 0,
            timerTotalSeconds = 60,
            onCloseTimer = {},
            unitSystem = UnitSystem.METRIC,
            listId = "previewListId",
            onNavigateToEditList = { _, _ -> },
            navController = previewNavController // Pass dummy NavController
        )
    }
}
