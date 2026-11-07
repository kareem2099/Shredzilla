package com.FreeRave.shredzilla.screens.today

import androidx.compose.material.icons.filled.Timer
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.utils.TrainingSession
import com.FreeRave.shredzilla.utils.formatTrainingTime
import com.FreeRave.shredzilla.utils.trainingSessions
import com.FreeRave.shredzilla.utils.WeeklyProgress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.models.RecordedSet
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.screens.calendar.CalendarSheet
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassStatTile
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatSetLoad
import com.FreeRave.shredzilla.utils.sidePrefix
import com.FreeRave.shredzilla.utils.setDetailsLabel
import com.FreeRave.shredzilla.utils.formatStoredWeight
import com.FreeRave.shredzilla.utils.daysOfMonth
import com.FreeRave.shredzilla.utils.startOfDay
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** "Today", "Yesterday", "Tomorrow", otherwise "Nov 2". */
private fun relativeDayLabel(startOfDay: Long): String =
    when (TimeUnit.MILLISECONDS.toDays(startOfDay - startOfDay())) {
        0L -> "Today"
        -1L -> "Yesterday"
        1L -> "Tomorrow"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(startOfDay))
    }

/**
 * Daily log: pick a day from the month strip (or the full calendar) and see that day's totals
 * and every set, grouped by exercise. [recordedSetsToday] holds the sets of the selected day.
 */
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    activeDays: Set<Long>,
    totalReps: Int,
    totalSets: Int,
    uniqueExercises: Int,
    recordedSetsToday: List<RecordedSet>,
    unitSystem: UnitSystem,
    onDateSelected: (Date) -> Unit,
    onNavigateToExerciseDetail: (String) -> Unit,
    weeklyProgress: WeeklyProgress,
    showDeloadReminder: Boolean,
    onSetDeloadWeek: (Boolean) -> Unit,
    onDismissDeloadReminder: () -> Unit,
    workouts: List<WorkoutLog> = emptyList()
) {
    val today = remember { startOfDay() }
    var selectedDay by rememberSaveable { mutableLongStateOf(today) }
    var showCalendar by remember { mutableStateOf(false) }
    val days = remember(selectedDay) { daysOfMonth(selectedDay) }

    fun select(day: Long) {
        selectedDay = day
        onDateSelected(Date(day))
    }

    if (showCalendar) {
        CalendarSheet(
            selectedDay = selectedDay,
            activeDays = activeDays,
            onSelect = { day ->
                select(day)
                showCalendar = false
            },
            onDismiss = { showCalendar = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        GlassTopBar(
            eyebrow = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(selectedDay)),
            title = relativeDayLabel(selectedDay),
            actions = { GlassIconButton(Icons.Filled.CalendarMonth, contentDescription = "Open calendar", onClick = { showCalendar = true }) }
        )
        DayStrip(
            days = days,
            selectedDay = selectedDay,
            today = today,
            // The selected day's own sets count too, in case analytics haven't caught up yet
            hasActivity = { it in activeDays || (it == selectedDay && recordedSetsToday.isNotEmpty()) },
            onSelect = ::select
        )

        val setsByExercise = remember(recordedSetsToday) {
            recordedSetsToday.groupBy { it.exerciseName }.mapValues { (_, sets) -> sets.sortedBy { it.timestamp } }
        }
        // Workouts measure their own time; sets logged outside one are grouped into sessions by their gaps
        val sessions = remember(recordedSetsToday, workouts, selectedDay) {
            val dayEnd = selectedDay + TimeUnit.DAYS.toMillis(1)
            trainingSessions(
                setTimes = recordedSetsToday.map { it.timestamp.toDate().time },
                workouts = workouts.filter { it.startedAtMillis in selectedDay until dayEnd }
                    .map { TrainingSession(it.startedAtMillis, it.startedAtMillis + it.durationMillis) }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // The week view belongs to today; browsing an old day shows just that day
            if (selectedDay == today) {
                if (showDeloadReminder) {
                    item {
                        DeloadReminderCard(
                            weeks = weeklyProgress.weeksSinceDeload,
                            onMarkDeload = { onSetDeloadWeek(true) },
                            onDismiss = onDismissDeloadReminder
                        )
                    }
                }
                item { WeeklyGoalCard(weeklyProgress, onToggleDeload = { onSetDeloadWeek(!weeklyProgress.isDeloadWeek) }) }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassStatTile(
                            Icons.Filled.Timer,
                            if (sessions.size > 1) "Training time · ${sessions.size} sessions" else "Training time",
                            formatTrainingTime(sessions.sumOf { it.durationMillis }),
                            Modifier.weight(1f)
                        )
                        GlassStatTile(Icons.Filled.Layers, "Sets", totalSets.toString(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassStatTile(Icons.Filled.Repeat, "Reps", totalReps.toString(), Modifier.weight(1f))
                        GlassStatTile(Icons.Filled.FitnessCenter, "Exercises", uniqueExercises.toString(), Modifier.weight(1f))
                    }
                    if (sessions.isNotEmpty()) {
                        val clock = remember { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT) }
                        Text(
                            sessions.joinToString("   ·   ") { "${clock.format(Date(it.startMillis))} – ${clock.format(Date(it.endMillis))}" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.glass.contentMuted,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
            if (setsByExercise.isEmpty()) {
                item { EmptyDayCard(isToday = selectedDay == today) }
            } else {
                items(setsByExercise.entries.toList(), key = { it.key }) { (exerciseName, sets) ->
                    ExerciseLogCard(
                        exerciseName = exerciseName,
                        sets = sets,
                        unitSystem = unitSystem,
                        onClick = { onNavigateToExerciseDetail(exerciseName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayStrip(
    days: List<Long>,
    selectedDay: Long,
    today: Long,
    hasActivity: (Long) -> Boolean,
    onSelect: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    // Keep the selected day in view (with two days of context to its left), e.g. the 25th on open
    LaunchedEffect(selectedDay, days) {
        val index = days.indexOf(selectedDay)
        if (index >= 0) listState.animateScrollToItem((index - 2).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days, key = { it }) { day ->
            DayPill(
                dayStart = day,
                isSelected = day == selectedDay,
                isToday = day == today,
                hasActivity = hasActivity(day),
                onClick = { onSelect(day) }
            )
        }
    }
}

@Composable
private fun ExerciseLogCard(
    exerciseName: String,
    sets: List<RecordedSet>,
    unitSystem: UnitSystem,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.glass
    val volume = remember(sets) {
        sets.filter { it.setType != SetType.WARMUP }.sumOf { it.weight.toDouble() * it.reps }.roundToLong().toDouble()
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                Text(
                    // Plate-counted sets are stored with 0 kg, so a machine-only card shows reps instead
                    if (volume > 0) "Volume ${formatStoredWeight(volume, unitSystem)}" else "${sets.sumOf { it.reps }} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.contentMuted
                )
            }
            val working = sets.filter { it.setType != SetType.WARMUP }
            val count = working.count { it.side == null } + maxOf(working.count { it.side == Side.LEFT }, working.count { it.side == Side.RIGHT })
            GlassChip(text = "$count ${if (count == 1) "set" else "sets"}")
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.contentMuted)
        }
        sets.forEach { set -> SetRow(set, unitSystem) }
    }
}

@Composable
private fun SetRow(set: RecordedSet, unitSystem: UnitSystem) {
    val palette = MaterialTheme.glass
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).glassSurface(palette, CircleShape)
        ) {
            Text(if (set.setNumber == 0) "W" else set.setNumber.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.accent)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = sidePrefix(set.side) + formatSetLoad(set.weight.toDouble(), set.plates, unitSystem, set.bodyweight, set.perSide),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.content
        )
        Text("  ×  ${set.reps} reps", style = MaterialTheme.typography.bodyLarge, color = palette.contentMuted)
        // Type and tempo first, then the user's own notes
        listOfNotNull(setDetailsLabel(set.setType, set.tempo), set.notes?.takeIf { it.isNotBlank() }).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun EmptyDayCard(isToday: Boolean) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassIconBadge(
            painter = rememberVectorPainter(Icons.Filled.SelfImprovement),
            size = 64.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = if (isToday) "Nothing logged yet today" else "Rest day",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.content,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = if (isToday) "Record a set from your exercise library and it shows up here."
            else "No sets were recorded on this day.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.contentMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun TodayScreenPreview() {
    ShredzillaTheme {
        TodayScreen(
            activeDays = emptySet(),
            totalReps = 12, totalSets = 2, uniqueExercises = 1,
            recordedSetsToday = listOf(
                RecordedSet("Bench Press", 1, 5, 35f, timestamp = Timestamp(Date()), exerciseId = "bench_press"),
                RecordedSet("Bench Press", 2, 7, 35f, timestamp = Timestamp(Date()), exerciseId = "bench_press")
            ),
            unitSystem = UnitSystem.METRIC,
            onDateSelected = {},
            onNavigateToExerciseDetail = {},
            weeklyProgress = WeeklyProgress("2026-11-01", 2, 4, 3, 6, false),
            showDeloadReminder = true,
            onSetDeloadWeek = {},
            onDismissDeloadReminder = {}
        )
    }
}
