package com.FreeRave.shredzilla.screens.exercises

// Grouped Standard Android/Compose imports
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

// Project-specific imports
import androidx.navigation.NavHostController // Added for navigation
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.models.ExerciseDisplayInfo
import com.FreeRave.shredzilla.navigation.AppRoutes // Added for navigation routes
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheetContent
import com.FreeRave.shredzilla.screens.exercises.composables.SwipableExerciseListItem
import com.FreeRave.shredzilla.screens.settings.UnitSystem // Import UnitSystem
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

enum class SortOption(val displayName: String) {
    MOST_RECENTLY_DONE("Most Recently Done"),
    ALPHABETICAL("Alphabetical")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    exercisesDisplayInfo: List<ExerciseDisplayInfo>, // Now globally debounced and pre-filtered
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToAddExercise: () -> Unit,
    onNavigateBack: () -> Unit,
    onDeleteExercise: (String) -> Unit,
    onRecordExercise: (exerciseName: String, reps: Int, weight: Double, notes: String) -> Unit, // Updated signature
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit,
    unitSystem: UnitSystem, // New parameter
    navController: NavHostController // Added NavController
) {
    // State Hoisting: Search query is now managed safely in the ViewModel via StateFlow
    // var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf<SortOption?>(null) }
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedExerciseForRecord by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<String?>(null) }

    val displayedExercises = remember(selectedSortOption, exercisesDisplayInfo) {
        derivedStateOf {
            when (selectedSortOption) {
                SortOption.ALPHABETICAL -> exercisesDisplayInfo.sortedBy { it.name }
                SortOption.MOST_RECENTLY_DONE -> exercisesDisplayInfo // Already naturally ordered or handled centrally
                null -> exercisesDisplayInfo
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Exercises", color = MaterialTheme.colorScheme.onPrimary) },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Sort options", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    SortOptionsMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        onSortOptionSelected = { option ->
                            selectedSortOption = option
                            showSortMenu = false
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExercise,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isTimerRunning) {
                // Using fully qualified name as a test, though import should work
                com.FreeRave.shredzilla.composables.RestTimerBar(
                    remainingSeconds = timerRemainingSeconds,
                    totalSeconds = timerTotalSeconds,
                    onClose = onCloseTimer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50)),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (displayedExercises.value.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No exercises found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectDragGestures { _, _ ->
                                    focusManager.clearFocus()
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Added spacing
                    ) {
                        // 60 FPS Optimization: Explicitly declaring `key = { it.id }` forces Compose to memorize list allocations accurately during Debounced filtering!
                        // `contentType` acts as a memory recycler, eliminating view recreation strain.
                        items(
                            items = displayedExercises.value,
                            key = { it.id },
                            contentType = { "ExerciseItem" }
                        ) { exerciseInfo ->
                            SwipableExerciseListItem(
                                exerciseName = exerciseInfo.name,
                                lastPerformed = exerciseInfo.lastPerformed,
                                onDelete = { // Updated onDelete to show dialog
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
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                },
                unitSystem = unitSystem // Pass to bottom sheet
            )
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to remove '${exerciseToDelete ?: "this exercise"}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exerciseToDelete?.let { onDeleteExercise(it) }
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

@Composable
fun SortOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        SortOption.values().forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    onSortOptionSelected(option)
                    onDismissRequest() // Ensure menu closes on item click
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ExercisesScreenPreview() {
    val previewNavController = androidx.navigation.compose.rememberNavController() // Dummy for preview
    ShredzillaTheme { // Removed darkTheme = true
        ExercisesScreen(
            exercisesDisplayInfo = listOf(
                ExerciseDisplayInfo(id = "bench", name = "Preview Bench", lastPerformed = "Today"),
                ExerciseDisplayInfo(id = "squat", name = "Preview Squat", lastPerformed = "Yesterday"),
                ExerciseDisplayInfo(id = "deadlift", name = "Preview Deadlift", lastPerformed = "3 days ago")
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onNavigateToAddExercise = {},
            onNavigateBack = {},
            onDeleteExercise = {},
            onRecordExercise = { _, _, _, _ -> }, // Match new signature
            isTimerRunning = true,
            timerRemainingSeconds = 30,
            timerTotalSeconds = 60,
            onCloseTimer = {},
            unitSystem = UnitSystem.METRIC, // Add to preview
            navController = previewNavController // Pass dummy NavController
        )
    }
}
