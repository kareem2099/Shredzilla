package com.FreeRave.shredzilla.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassBottomSheet
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.daysOfMonth
import com.FreeRave.shredzilla.utils.startOfDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun shiftMonth(anyDayInMonth: Long, months: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = anyDayInMonth
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, months)
    }.timeInMillis

/**
 * Month picker shown over the Today tab. Days with workouts get a dot; picking a day selects it
 * and closes the sheet. The week starts on the locale's first day (Monday in most of the world).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSheet(
    selectedDay: Long,
    activeDays: Set<Long>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = MaterialTheme.glass
    val today = remember { startOfDay() }
    var shownMonth by remember { mutableLongStateOf(selectedDay) }
    val days = remember(shownMonth) { daysOfMonth(shownMonth) }
    val workoutsThisMonth = days.count { it in activeDays }

    GlassBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", onClick = { shownMonth = shiftMonth(shownMonth, -1) })
            AnimatedContent(
                targetState = shownMonth,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * direction } + fadeIn()) togetherWith (slideOutHorizontally { -it * direction } + fadeOut())
                },
                label = "calendarMonth",
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) { month ->
                Text(
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(month)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            GlassIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", onClick = { shownMonth = shiftMonth(shownMonth, 1) })
        }
        Spacer(Modifier.size(16.dp))
        MonthGrid(days = days, selectedDay = selectedDay, today = today, activeDays = activeDays, onSelect = onSelect)
        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$workoutsThisMonth ${if (workoutsThisMonth == 1) "workout" else "workouts"} this month",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            TextButton(onClick = { onSelect(today) }) {
                Text("Jump to today", color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<Long>,
    selectedDay: Long,
    today: Long,
    activeDays: Set<Long>,
    onSelect: (Long) -> Unit
) {
    val palette = MaterialTheme.glass
    val firstDayOfWeek = remember { Calendar.getInstance().firstDayOfWeek }
    // Weekday initials in locale order, e.g. M T W T F S S
    val weekdayLabels = remember {
        val cal = Calendar.getInstance()
        (0 until 7).map { offset ->
            cal.set(Calendar.DAY_OF_WEEK, (firstDayOfWeek - 1 + offset) % 7 + 1)
            SimpleDateFormat("EEEEE", Locale.getDefault()).format(cal.time)
        }
    }
    val leadingBlanks = remember(days) {
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = days.first() }.get(Calendar.DAY_OF_WEEK)
        (dayOfWeek - firstDayOfWeek + 7) % 7
    }
    val cells: List<Long?> = List(leadingBlanks) { null } + days

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            weekdayLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.contentMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                isSelected = day == selectedDay,
                                isToday = day == today,
                                hasWorkout = day in activeDays,
                                onClick = { onSelect(day) }
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) } // pad the last week
            }
        }
    }
}

@Composable
private fun DayCell(day: Long, isSelected: Boolean, isToday: Boolean, hasWorkout: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    val dayNumber = remember(day) { Calendar.getInstance().apply { timeInMillis = day }.get(Calendar.DAY_OF_MONTH) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(Brush.linearGradient(palette.accentGradient)) else Modifier)
            .border(1.5.dp, if (isToday && !isSelected) palette.accent else Color.Transparent, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Text(
            dayNumber.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) palette.onAccent else palette.content
        )
        Box(
            Modifier
                .size(5.dp)
                .background(
                    when {
                        !hasWorkout -> Color.Transparent
                        isSelected -> palette.onAccent
                        else -> palette.accent
                    },
                    CircleShape
                )
        )
    }
}
