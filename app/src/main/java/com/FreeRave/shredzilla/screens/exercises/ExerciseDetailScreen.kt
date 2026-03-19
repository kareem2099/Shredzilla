package com.FreeRave.shredzilla.screens.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add // For FAB
import androidx.compose.material.icons.filled.Delete // For delete action
import androidx.compose.material.icons.filled.Edit // For edit action
import androidx.compose.material.icons.filled.EmojiEvents // For PRs
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert // For options menu
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable // For clickable items
import com.FreeRave.shredzilla.models.ExerciseSetPerformance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.models.ExerciseItem
import com.FreeRave.shredzilla.models.initialGlobalExerciseList
import com.FreeRave.shredzilla.screens.exercises.composables.RecordSetSheetContent // Import for bottom sheet
import com.FreeRave.shredzilla.screens.settings.UnitSystem // Import UnitSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // For coroutine scope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    navController: NavController,
    exerciseId: String?,
    onRecordSet: (exerciseName: String, reps: Int, weight: Double, notes: String) -> Unit,
    unitSystem: UnitSystem,
    onDeleteSet: (exerciseName: String, firestoreDocId: String) -> Unit, // New callback for deleting
    onUpdateSet: (exerciseName: String, firestoreDocId: String, reps: Int, weight: Double, notes: String) -> Unit // New callback for updating
) {
    val context = LocalContext.current
    val exercise = initialGlobalExerciseList.find { it.id == exerciseId }

    // Bottom sheet state for adding/editing sets
    val recordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRecordSheet by remember { mutableStateOf(false) }
    var setToEdit by remember { mutableStateOf<ExerciseSetPerformance?>(null) } // To hold set being edited

    val scope = rememberCoroutineScope()

    // Delete confirmation dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var setToDelete by remember { mutableStateOf<ExerciseSetPerformance?>(null) }


    // Animation states
    var showVideo by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }
    var showTargetMuscles by remember { mutableStateOf(false) }
    var showEquipment by remember { mutableStateOf(false) }
    var showDifficulty by remember { mutableStateOf(false) }
    var showPerformance by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100) // Stagger animations
        showVideo = true
        delay(150)
        showDescription = true
        delay(150)
        showTargetMuscles = true
        delay(150)
        showEquipment = true
        delay(150)
        showDifficulty = true
        delay(150)
        showPerformance = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise Details", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (exercise != null) { // Only show FAB if exercise is loaded
                FloatingActionButton(
                    onClick = { showRecordSheet = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Filled.Add, "Record New Set")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (exercise == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Exercise not found.", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp) // Additional padding for content
        ) {
            // Video Player Section
            AnimatedVisibility(
                visible = showVideo,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                VideoSection(exercise)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Details Sections
            AnimatedInfoCard(visible = showDescription, icon = Icons.Filled.Description, title = "Description", content = exercise.description)
            AnimatedInfoCard(visible = showTargetMuscles, icon = Icons.Filled.FitnessCenter, title = "Target Muscles", content = exercise.targetMuscles.joinToString())
            AnimatedInfoCard(visible = showEquipment, icon = Icons.AutoMirrored.Filled.ListAlt, title = "Equipment Needed", content = exercise.equipmentNeeded.joinToString()) // Corrected Icon
            AnimatedInfoCard(visible = showDifficulty, icon = Icons.Filled.Info, title = "Difficulty", content = exercise.difficulty)

            // Personal Bests Section
            val personalBests = remember(exercise.recordedSets) {
                calculatePersonalBests(exercise.recordedSets)
            }
            AnimatedVisibility(
                visible = showPerformance, // Reuse showPerformance for animation timing
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 450)), // Adjust delay
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                PersonalBestsSection(
                    oneRepMax = personalBests.oneRepMax,
                    fiveRepMax = personalBests.fiveRepMax,
                    unitSystem = unitSystem
                )
            }

            // User Performance Log Section
            AnimatedVisibility(
                visible = showPerformance,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 600)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                PerformanceLogSection(
                    exercise = exercise,
                    onEditSetClicked = { set ->
                        setToEdit = set
                        showRecordSheet = true
                    },
                    onDeleteSetClicked = { set ->
                        setToDelete = set
                        showDeleteConfirmDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showRecordSheet && exercise != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showRecordSheet = false
                    setToEdit = null // Reset setToEdit when sheet is dismissed
                },
                sheetState = recordSheetState, // Corrected: use recordSheetState
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                RecordSetSheetContent(
                    exerciseName = exercise.name,
                    initialReps = setToEdit?.reps?.toIntOrNull() ?: 1,
                    initialWeight = setToEdit?.weight?.toDoubleOrNull() ?: 0.0,
                    initialNotes = setToEdit?.notes ?: "", // Now correctly uses the new 'notes' field
                    onRecordSet = { reps, weight, notes ->
                        if (setToEdit != null && setToEdit!!.firestoreDocId != null) {
                            onUpdateSet(exercise.name, setToEdit!!.firestoreDocId!!, reps, weight, notes)
                        } else {
                            onRecordSet(exercise.name, reps, weight, notes)
                        }
                        scope.launch { recordSheetState.hide() }.invokeOnCompletion {
                            if (!recordSheetState.isVisible) {
                                showRecordSheet = false
                                setToEdit = null // Reset after sheet closes
                            }
                        }
                    },
                    onCancel = {
                        scope.launch { recordSheetState.hide() }.invokeOnCompletion {
                            if (!recordSheetState.isVisible) {
                                showRecordSheet = false
                                setToEdit = null // Reset after sheet closes
                            }
                        }
                    },
                    unitSystem = unitSystem,
                    isEditing = setToEdit != null
                )
            }
        }

        if (showDeleteConfirmDialog && setToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    setToDelete = null
                },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this set? (Set ${setToDelete?.setNumber}: ${setToDelete?.reps} reps, ${setToDelete?.weight} ${if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"})") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            setToDelete?.firestoreDocId?.let { docId ->
                                onDeleteSet(exercise.name, docId)
                            }
                            showDeleteConfirmDialog = false
                            setToDelete = null
                        }
                    ) { Text("Yes, Delete") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog = false
                        setToDelete = null
                    }) { Text("No") }
                }
            )
        }
    }
}

@Composable
fun VideoSection(exercise: ExerciseItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!exercise.videoUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircleOutline,
                        contentDescription = "Play Video",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    // Placeholder for actual video thumbnail if available
                    // Image(painter = painterResource(id = R.drawable.your_thumbnail), contentDescription = null, contentScale = ContentScale.Crop)
                }
                Button(
                    onClick = { /* TODO: Implement video playback or open URL */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = "Watch Video Icon", modifier = Modifier.padding(end = 8.dp))
                    Text("Watch Video: ${exercise.name}")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No video available for this exercise.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedInfoCard(visible: Boolean, icon: ImageVector, title: String, content: String) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        InfoCard(icon = icon, title = title, content = content)
    }
}

data class CalculatedPRs(
    val oneRepMax: Double?,
    val fiveRepMax: Double?
)

fun calculatePersonalBests(recordedSets: List<ExerciseSetPerformance>): CalculatedPRs {
    var currentOneRepMax: Double? = null
    var currentFiveRepMax: Double? = null

    recordedSets.forEach { set ->
        val reps = set.reps.toIntOrNull()
        val weight = set.weight.toDoubleOrNull()

        if (reps != null && weight != null && weight > 0) {
            if (reps == 1) {
                if (currentOneRepMax == null || weight > currentOneRepMax!!) {
                    currentOneRepMax = weight
                }
            }
            if (reps == 5) {
                if (currentFiveRepMax == null || weight > currentFiveRepMax!!) {
                    currentFiveRepMax = weight
                }
            }
        }
    }
    return CalculatedPRs(oneRepMax = currentOneRepMax, fiveRepMax = currentFiveRepMax)
}

@Composable
fun PersonalBestsSection(oneRepMax: Double?, fiveRepMax: Double?, unitSystem: UnitSystem) {
    val weightUnit = if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Personal Bests",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Personal Bests",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val prTextStyle = MaterialTheme.typography.bodyLarge
            val noRecordColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("1-Rep Max", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = oneRepMax?.let { String.format("%.1f %s", it, weightUnit) } ?: "N/A",
                        style = prTextStyle,
                        color = if (oneRepMax != null) MaterialTheme.colorScheme.primary else noRecordColor,
                        fontWeight = if (oneRepMax != null) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("5-Rep Max", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fiveRepMax?.let { String.format("%.1f %s", it, weightUnit) } ?: "N/A",
                        style = prTextStyle,
                        color = if (fiveRepMax != null) MaterialTheme.colorScheme.primary else noRecordColor,
                        fontWeight = if (fiveRepMax != null) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}


@Composable
fun PerformanceLogSection(
    exercise: ExerciseItem,
    onEditSetClicked: (ExerciseSetPerformance) -> Unit,
    onDeleteSetClicked: (ExerciseSetPerformance) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Your Performance Log",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (exercise.recordedSets.isEmpty()) {
                Text(
                    "No sets recorded for this exercise yet. Swipe on the exercise in the list to log your performance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Text(
                    "Logged Sets:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp) // Limit height for scrollability
                ) {
                    itemsIndexed(
                        items = exercise.recordedSets,
                        key = { _, item -> item.firestoreDocId ?: "set-${item.setNumber}-${item.timestamp.seconds}" }
                    ) { index, setRecord ->
                        SetRecordItemView(
                            setRecord = setRecord,
                            onEditClick = { onEditSetClicked(setRecord) },
                            onDeleteClick = { onDeleteSetClicked(setRecord) }
                        )
                        if (index < exercise.recordedSets.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetRecordItemView(
    setRecord: ExerciseSetPerformance,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Set ${setRecord.setNumber}:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${setRecord.reps} reps",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Text(
            text = "Wt: ${setRecord.weight}", // Assuming weight already includes unit or unit is implied by context
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Set options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEditClick()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Edit Set") }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDeleteClick()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Delete Set") }
                )
            }
        }
    }
}
