package com.FreeRave.shredzilla.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
// Ensure all specific color names are accessible, if not covered by wildcard:
import com.FreeRave.shredzilla.ui.theme.MaleDarkPrimary
import com.FreeRave.shredzilla.ui.theme.MaleLightPrimary
import com.FreeRave.shredzilla.ui.theme.FemaleDarkPrimary
import com.FreeRave.shredzilla.ui.theme.FemaleLightPrimary
// Wildcard import should cover MaterialTheme and other theme elements
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // AutoMirrored
import androidx.compose.material.icons.automirrored.filled.ArrowForward // AutoMirrored
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.theme.* // Import all from theme for colors
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenCalendarScreen(
    initialDate: Date, // The date to initially show/select
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance().apply { time = initialDate }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val todayCalendar = Calendar.getInstance()

    fun getDaysInMonth(month: Int, year: Int): List<Date?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7

        val daysList = MutableList<Date?>(42) { null } // Max 6 weeks * 7 days

        // Add days of current month
        for (i in 0 until daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i + 1)
            daysList[firstDayOfWeek - 1 + i] = cal.time
        }
        return daysList
    }

    var daysForGrid by remember(currentMonth, currentYear) {
        mutableStateOf(getDaysInMonth(currentMonth, currentYear))
    }

    val genderTheme = ThemeManager.currentGenderTheme
    val isDarkTheme = isSystemInDarkTheme()

    val todayButtonColor = when (genderTheme) {
        "Male" -> if (isDarkTheme) MaleDarkPrimary else MaleLightPrimary
        "Female" -> if (isDarkTheme) FemaleDarkPrimary else FemaleLightPrimary
        else -> MaterialTheme.colorScheme.primary
    }
    val cancelButtonColor = todayButtonColor // Same as "Today" button per image

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(monthYearFormat.format(Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }.time), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply { time = Date() } // Today
                        currentMonth = cal.get(Calendar.MONTH)
                        currentYear = cal.get(Calendar.YEAR)
                        selectedDate = cal.time
                        daysForGrid = getDaysInMonth(currentMonth, currentYear)
                        onDateSelected(selectedDate) // Optionally select today immediately
                    }) { Text("Today", color = todayButtonColor) }
                },
                actions = {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = cancelButtonColor) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 8.dp)) {
            // Days of week header (S, M, T, W, T, F, S)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { day ->
                    Text(text = day, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            HorizontalDivider() // Replaced Divider

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(daysForGrid) { date ->
                    if (date != null) {
                        val cal = Calendar.getInstance().apply { time = date }
                        val day = dayFormat.format(date)
                        val isCurrentMonth = cal.get(Calendar.MONTH) == currentMonth
                        val isSelectedDay = selectedDate.let { Calendar.getInstance().apply { time = it }.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) && Calendar.getInstance().apply { time = it }.get(Calendar.YEAR) == cal.get(Calendar.YEAR) }
                        val isToday = todayCalendar.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) && todayCalendar.get(Calendar.YEAR) == cal.get(Calendar.YEAR)

                        val cellColor = when {
                            isSelectedDay -> MaterialTheme.colorScheme.primaryContainer
                            isToday -> todayButtonColor.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }
                        val textColor = when {
                            isSelectedDay -> MaterialTheme.colorScheme.onPrimaryContainer
                            isToday -> todayButtonColor
                            !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Dimmed for other months
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val cellBorder = if (isSelectedDay) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null


                        Box(
                            modifier = Modifier
                                .aspectRatio(1f) // Make cells square
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellColor)
                                .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable(enabled = isCurrentMonth) {
                                    selectedDate = date
                                    onDateSelected(date)
                                    // onDismiss() // Optionally dismiss after selection
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = day, color = textColor, fontSize = 14.sp)
                        }
                    } else {
                        Spacer(Modifier.aspectRatio(1f).padding(2.dp)) // Empty cell
                    }
                }
            }
             // Month navigation (optional, not in screenshot but common)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply { set(currentYear, currentMonth, 1); add(Calendar.MONTH, -1) }
                    currentMonth = cal.get(Calendar.MONTH)
                    currentYear = cal.get(Calendar.YEAR)
                    daysForGrid = getDaysInMonth(currentMonth, currentYear)
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Month") }

                IconButton(onClick = {
                     val cal = Calendar.getInstance().apply { set(currentYear, currentMonth, 1); add(Calendar.MONTH, 1) }
                    currentMonth = cal.get(Calendar.MONTH)
                    currentYear = cal.get(Calendar.YEAR)
                    daysForGrid = getDaysInMonth(currentMonth, currentYear)
                }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Month") }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FullScreenCalendarScreenDarkPreview() {
    ShredzillaTheme { // Removed darkTheme = true
        Surface {
            FullScreenCalendarScreen(initialDate = Date(), onDateSelected = {}, onDismiss = {})
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun FullScreenCalendarScreenLightPreview() {
    ShredzillaTheme { // Removed darkTheme = false
        Surface {
            FullScreenCalendarScreen(initialDate = Date(), onDateSelected = {}, onDismiss = {})
        }
    }
}
