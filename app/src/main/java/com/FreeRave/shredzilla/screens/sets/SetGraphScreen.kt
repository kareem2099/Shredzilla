package com.FreeRave.shredzilla.screens.sets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.CircularProgressIndicator
import com.FreeRave.shredzilla.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetGraphScreen(
    analyticsGraphData: Map<String, List<MainViewModel.ChartDataPoint>>,
    isAnalyticsLoading: Boolean,
    exerciseCount: Int,
    userExerciseLists: List<UserExerciseList>, // New parameter for user's lists
    onNavigateToSettings: () -> Unit,
    onNavigateToExerciseList: () -> Unit,
    onNavigateToCreateNewList: () -> Unit,
    onNavigateToExerciseListDetail: (listId: String, listName: String) -> Unit, // New callback
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit
) {
    // Add other navigation callbacks as needed, e.g., for settings or new list
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Shredzilla", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSettings) { // Use the new callback
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onPrimary)
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
                onClick = onNavigateToCreateNewList,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create New List")
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues) // This padding is from the Scaffold (includes TopAppBar height)
                    .fillMaxSize()
            ) {
                if (isTimerRunning) {
                    RestTimerBar(
                        remainingSeconds = timerRemainingSeconds,
                        totalSeconds = timerTotalSeconds,
                        onClose = onCloseTimer,
                        // Modifier.padding(horizontal = 16.dp) // Already padded by Column's parent
                    )
                }
                Column( // Inner column for the rest of the content with its own padding
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    if (isAnalyticsLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                    } else { // It's either empty or has data
                        Text(
                            "Overall Progress",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val primaryExerciseId = analyticsGraphData.entries.maxByOrNull { it.value.size }?.key
                        if (analyticsGraphData.isNotEmpty() && primaryExerciseId != null && analyticsGraphData[primaryExerciseId]!!.size > 1) {
                            ProgressChartCanvas(
                                dataPoints = analyticsGraphData[primaryExerciseId]!!, 
                                modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp)
                            )
                        } else {
                            Text("Complete more workouts to unlock your Progress Chart!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SetGraphCard(
                        count = exerciseCount.toString(),
                        title = "Exercises",
                        showExerciseIcon = true,
                        onClick = onNavigateToExerciseList
                    )
                    // Display user-created lists
                    if (userExerciseLists.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Your Lists",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Reverting to basic items without key to see if it resolves compiler issue
                            items(userExerciseLists) { exerciseList -> 
                                SetGraphCard(
                                    count = exerciseList.exerciseIds.size.toString(),
                                    title = exerciseList.name,
                                    showExerciseIcon = true, 
                                    onClick = { onNavigateToExerciseListDetail(exerciseList.id, exerciseList.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SetGraphCard(
    modifier: Modifier = Modifier,
    count: String? = null,
    title: String,
    showPlusIcon: Boolean = false,
    showExerciseIcon: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (count != null) {
                    Text(
                        text = count,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = title,
                    color = if (title == "New List") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showPlusIcon) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New List",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            if (showExerciseIcon) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = "Exercises",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Pro-Tip: Advanced Canvas Drawing for dynamic Y-Axis bounds.
// Mathematical clamping prevents outliers from aggressively compressing the graph.
@Composable
fun ProgressChartCanvas(dataPoints: List<MainViewModel.ChartDataPoint>, modifier: Modifier = Modifier) {
    if (dataPoints.isEmpty() || dataPoints.size < 2) return
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pro-Tip: Adding Gradient Fill for Premium Look
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.4f), 
            Color.Transparent
        )
    )

    // Dynamic Bounds Evaluation
    val maxVolume = dataPoints.maxOf { it.totalVolume }.toFloat()
    val minVolume = dataPoints.minOf { it.totalVolume }.toFloat()
    
    // Pro-Tip: Safe Division & Clamping
    val volumeRange = if (maxVolume > minVolume) (maxVolume - minVolume) else 1f // Prevent division by zero entirely

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pointSpacing = width / (dataPoints.size - 1).toFloat()

        val path = Path()
        val fillPath = Path() // New path for the gradient fill

        dataPoints.forEachIndexed { index, point ->
            val safeVolume = point.totalVolume.toFloat()
            // If all values are equal, draw the straight line exactly centered at 0.5f
            val normalizedY = if (maxVolume == minVolume) 0.5f else 1f - ((safeVolume - minVolume) / volumeRange)
            val yPos = (normalizedY * height * 0.8f) + (height * 0.1f)
            val xPos = index * pointSpacing

            if (index == 0) {
                path.moveTo(xPos, yPos)
                fillPath.moveTo(xPos, yPos)
            } else {
                path.lineTo(xPos, yPos)
                fillPath.lineTo(xPos, yPos)
            }
        }

        // Close the path to allow bottom contour gradient fill
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        // Draw the gradient beneath the main stroke line
        drawPath(
            path = fillPath,
            brush = gradientBrush
        )

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SetGraphScreenPreview() {
    ShredzillaTheme {
        SetGraphScreen(
            analyticsGraphData = emptyMap(),
            isAnalyticsLoading = false,
            exerciseCount = 10,
            userExerciseLists = listOf(
                UserExerciseList("1", "Upper Body A", listOf("ex1", "ex2")),
                UserExerciseList("2", "Leg Day", listOf("ex3", "ex4", "ex5"))
            ),
            onNavigateToSettings = {}, 
            onNavigateToExerciseList = {},
            onNavigateToCreateNewList = {},
            onNavigateToExerciseListDetail = { _, _ -> }, // Add to preview
            isTimerRunning = true,
            timerRemainingSeconds = 45,
            timerTotalSeconds = 60,
            onCloseTimer = {}
        )
    }
}
