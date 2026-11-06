package com.FreeRave.shredzilla.screens.sets

import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.ProgressChartCanvas
import com.FreeRave.shredzilla.composables.RestTimerBar
import com.FreeRave.shredzilla.models.UserExerciseList
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import java.util.Calendar
import kotlin.math.roundToInt

/** Home of the Sets tab: progress chart, exercise library and the user's workout lists. */
@Composable
fun SetGraphScreen(
    analyticsGraphData: Map<String, List<MainViewModel.ChartDataPoint>>,
    isAnalyticsLoading: Boolean,
    exerciseCount: Int,
    userExerciseLists: List<UserExerciseList>,
    userName: String?,
    onNavigateToExerciseList: () -> Unit,
    onOpenPrograms: () -> Unit,
    recommendedProgramName: String?,
    workoutCount: Int,
    recordCount: Int,
    onOpenHistory: () -> Unit,
    onOpenRecords: () -> Unit,
    measurementCount: Int,
    onOpenBody: () -> Unit,
    onNavigateToCreateNewList: () -> Unit,
    onNavigateToExerciseListDetail: (listId: String, listName: String) -> Unit,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    timerTotalSeconds: Int,
    onCloseTimer: () -> Unit
) {
    // The exercise with the most data points makes the most meaningful trend line
    val featured = remember(analyticsGraphData) {
        analyticsGraphData.entries.maxByOrNull { it.value.size }?.takeIf { it.value.size > 1 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(
            eyebrow = greeting(),
            title = userName?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "Shredzilla"
        )
        if (isTimerRunning) {
            RestTimerBar(remainingSeconds = timerRemainingSeconds, totalSeconds = timerTotalSeconds, onClose = onCloseTimer)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ProgressCard(
                    exerciseName = featured?.key?.toDisplayName(),
                    dataPoints = featured?.value,
                    isLoading = isAnalyticsLoading
                )
            }
            item {
                NavigationCard(
                    icon = Icons.Filled.FitnessCenter,
                    title = "Exercise library",
                    subtitle = "$exerciseCount ${if (exerciseCount == 1) "exercise" else "exercises"}",
                    onClick = onNavigateToExerciseList
                )
            }
            item {
                NavigationCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Start a program",
                    subtitle = recommendedProgramName?.let { "Recommended for you: $it" } ?: "Ready-made splits for every level",
                    onClick = onOpenPrograms
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShortcutTile(
                        icon = Icons.Filled.History,
                        title = "History",
                        subtitle = "$workoutCount ${if (workoutCount == 1) "workout" else "workouts"}",
                        onClick = onOpenHistory,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutTile(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Records",
                        subtitle = "$recordCount ${if (recordCount == 1) "exercise" else "exercises"}",
                        onClick = onOpenRecords,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutTile(
                        icon = Icons.Filled.Straighten,
                        title = "Body",
                        subtitle = "$measurementCount ${if (measurementCount == 1) "entry" else "entries"}",
                        onClick = onOpenBody,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { ListsHeader(onCreateList = onNavigateToCreateNewList) }
            if (userExerciseLists.isEmpty()) {
                item {
                    NavigationCard(
                        icon = Icons.Filled.Add,
                        title = "Create your first list",
                        subtitle = "Group exercises into a workout, like Push Day",
                        onClick = onNavigateToCreateNewList
                    )
                }
            } else {
                items(userExerciseLists, key = { it.id }) { list ->
                    val count = list.exerciseIds.size
                    NavigationCard(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = list.name,
                        subtitle = "$count ${if (count == 1) "exercise" else "exercises"}",
                        onClick = { onNavigateToExerciseListDetail(list.id, list.name) }
                    )
                }
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** Analytics keys are exercise ids like "bench_press". */
private fun String.toDisplayName() = split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

@Composable
private fun ProgressCard(
    exerciseName: String?,
    dataPoints: List<MainViewModel.ChartDataPoint>?,
    isLoading: Boolean
) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.content)
                Text(
                    text = exerciseName?.let { "$it · volume per session" } ?: "Your training volume over time",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.contentMuted
                )
            }
            dataPoints?.let { trendPercent(it) }?.let { change ->
                GlassChip(text = "${if (change >= 0) "+" else ""}$change%", highlighted = change >= 0)
            }
        }
        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
            dataPoints != null -> ProgressChartCanvas(dataPoints, Modifier.fillMaxWidth().height(160.dp))
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.Insights), size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Log a couple of workouts and your progress chart shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.contentMuted
                )
            }
        }
    }
}

/** Change from the first to the latest session, or null when there's no baseline. */
private fun trendPercent(points: List<MainViewModel.ChartDataPoint>): Int? {
    val first = points.first().totalVolume
    if (first <= 0.0) return null
    return ((points.last().totalVolume - first) / first * 100).roundToInt()
}

@Composable
private fun ListsHeader(onCreateList: () -> Unit) {
    val palette = MaterialTheme.glass
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Your lists",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = palette.content,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCreateList) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(4.dp))
            Text("New list", color = palette.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NavigationCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconBadge(painter = rememberVectorPainter(icon), size = 52.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.contentMuted)
        }
    }
}

@Composable
private fun ShortcutTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = modifier
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassIconBadge(painter = rememberVectorPainter(icon), size = 44.dp)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
    }
}

@Preview
@Composable
private fun SetGraphScreenPreview() {
    ShredzillaTheme {
        SetGraphScreen(
            analyticsGraphData = emptyMap(),
            isAnalyticsLoading = false,
            exerciseCount = 10,
            userExerciseLists = listOf(
                UserExerciseList("1", "Upper Body A", listOf("ex1", "ex2")),
                UserExerciseList("2", "Leg Day", listOf("ex3", "ex4", "ex5"))
            ),
            userName = "Alex",
            onNavigateToExerciseList = {},
            onOpenPrograms = {},
            recommendedProgramName = "Push / Pull / Legs",
            workoutCount = 12,
            recordCount = 8,
            onOpenHistory = {},
            onOpenRecords = {},
            measurementCount = 3,
            onOpenBody = {},
            onNavigateToCreateNewList = {},
            onNavigateToExerciseListDetail = { _, _ -> },
            isTimerRunning = true,
            timerRemainingSeconds = 45,
            timerTotalSeconds = 60,
            onCloseTimer = {}
        )
    }
}
