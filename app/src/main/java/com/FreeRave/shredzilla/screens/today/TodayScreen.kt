package com.FreeRave.shredzilla.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.auth.FirebaseGoogleAuth
import com.FreeRave.shredzilla.models.RecordedSet
import com.FreeRave.shredzilla.screens.calendar.FullScreenCalendarScreen
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


private fun formatDateAsRelative(selectedDate: Date): String {
    val today = Calendar.getInstance()
    val selectedCal = Calendar.getInstance().apply { time = selectedDate }

    today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
    selectedCal.set(Calendar.HOUR_OF_DAY, 0); selectedCal.set(Calendar.MINUTE, 0); selectedCal.set(Calendar.SECOND, 0); selectedCal.set(Calendar.MILLISECOND, 0)

    val diffInMillis = selectedCal.timeInMillis - today.timeInMillis
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when (diffInDays) {
        0L -> "Today"
        -1L -> "Yesterday"
        1L -> "Tomorrow"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(selectedDate)
    }
}

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    mainNavController: NavHostController?,
    firebaseEmailAuthManager: FirebaseEmailPasswordAuth?,
    firebaseGoogleAuthManager: FirebaseGoogleAuth?,
    totalReps: Int,
    totalSets: Int,
    uniqueExercises: Int,
    recordedSetsToday: List<RecordedSet>,
    unitSystem: UnitSystem
) {
    val initialCalendar = Calendar.getInstance()
    var displayMonth by remember { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }
    var displayYear by remember { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }

    fun getDaysForDisplayedMonth(month: Int, year: Int): List<DayItem> {
        val list = ArrayList<DayItem>()
        val sdfDayOfMonth = SimpleDateFormat("d", Locale.getDefault())
        val sdfDayOfWeek = SimpleDateFormat("E", Locale.getDefault())
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, month); set(Calendar.YEAR, year) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val mockHasActivity = java.util.Random().nextInt(10) < 3
            list.add(DayItem(sdfDayOfMonth.format(cal.time), sdfDayOfWeek.format(cal.time), cal.time, hasActivity = mockHasActivity))
        }
        return list
    }

    var daysInCurrentMonth by remember(displayMonth, displayYear) { mutableStateOf(getDaysForDisplayedMonth(displayMonth, displayYear)) }
    val todayDayOfMonthStr = SimpleDateFormat("d", Locale.getDefault()).format(Date())
    var selectedDay by remember {
        mutableStateOf(
            daysInCurrentMonth.find { it.dayOfMonth == todayDayOfMonthStr && Calendar.getInstance().apply { time = it.fullDate }.get(Calendar.MONTH) == displayMonth }
                ?: daysInCurrentMonth.firstOrNull()
                ?: DayItem(todayDayOfMonthStr, "", Date(), true)
        )
    }
    daysInCurrentMonth = daysInCurrentMonth.map { it.copy(isSelected = (it.fullDate == selectedDay.fullDate)) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    if (showCalendarDialog) {
        Dialog(onDismissRequest = { showCalendarDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                FullScreenCalendarScreen(
                    initialDate = selectedDay.fullDate,
                    onDateSelected = { newDate ->
                        val calNew = Calendar.getInstance().apply { time = newDate }
                        displayMonth = calNew.get(Calendar.MONTH); displayYear = calNew.get(Calendar.YEAR)
                        val newDaysList = getDaysForDisplayedMonth(displayMonth, displayYear)
                        selectedDay = newDaysList.find { val calExisting = Calendar.getInstance().apply { time = it.fullDate }; calExisting.get(Calendar.YEAR) == displayYear && calExisting.get(Calendar.DAY_OF_YEAR) == calNew.get(Calendar.DAY_OF_YEAR) } ?: newDaysList.first()
                        daysInCurrentMonth = newDaysList.map { it.copy(isSelected = (it.fullDate == selectedDay.fullDate)) }
                        showCalendarDialog = false
                    },
                    onDismiss = { showCalendarDialog = false }
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp).clickable { showCalendarDialog = true })
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = formatDateAsRelative(selectedDay.fullDate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            itemsIndexed(daysInCurrentMonth) { _, dayItem -> DayPill(dayItem = dayItem, isSelected = dayItem.fullDate == selectedDay.fullDate, onDaySelected = { selectedDay = dayItem; daysInCurrentMonth = daysInCurrentMonth.map { it.copy(isSelected = (it.fullDate == dayItem.fullDate)) } }) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SummaryCard("REPS", totalReps.toString(), MaterialTheme.colorScheme.primary)
            SummaryCard("SETS", totalSets.toString(), MaterialTheme.colorScheme.secondary)
            SummaryCard("EXERCISES", uniqueExercises.toString(), MaterialTheme.colorScheme.tertiary)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (recordedSetsToday.isNotEmpty()) {
            val groupedSets = recordedSetsToday.groupBy { it.exerciseName }
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                groupedSets.forEach { (exerciseName, sets) ->
                    item(key = "header_$exerciseName") { Text(text = exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp), color = MaterialTheme.colorScheme.onBackground) }
                    items(items = sets, key = { set -> "set_${set.timestamp.seconds}_${set.timestamp.nanoseconds}_${set.reps}_${set.weight}" }) { setItem ->
                        val displayWeight = if (unitSystem == UnitSystem.IMPERIAL) setItem.weight * 2.20462 else setItem.weight.toDouble()
                        val weightUnitString = if (unitSystem == UnitSystem.IMPERIAL) "lbs" else "kg"
                        val formattedWeight = displayWeight.let { if (it.rem(1.0) == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it) }
                        Text(
                            text = "  $formattedWeight $weightUnitString, rep ${setItem.reps}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) { Text("No sets recorded for today yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, valueColor: Color) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.width(100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TodayScreenDarkPreview() {
    ShredzillaTheme {
        Surface {
            TodayScreen(
                mainNavController = null,
                firebaseEmailAuthManager = null,
                firebaseGoogleAuthManager = null,
                totalReps = 53, totalSets = 9, uniqueExercises = 3,
                recordedSetsToday = listOf(
                    RecordedSet(exerciseName= "Bench Press", setNumber = 1, reps = 5, weight = 35.0f, notes = null, timestamp = Timestamp(System.currentTimeMillis()/1000, 0), exerciseId = "bench_press", firestoreDocId = null),
                    RecordedSet(exerciseName ="Bench Press", setNumber = 2, reps = 7, weight = 35.0f, notes = null, timestamp = Timestamp(System.currentTimeMillis()/1000, 100), exerciseId = "bench_press", firestoreDocId = null)
                ),
                unitSystem = UnitSystem.METRIC
            )
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun TodayScreenLightPreview() {
    ShredzillaTheme {
        Surface {
            TodayScreen(
                mainNavController = null,
                firebaseEmailAuthManager = null,
                firebaseGoogleAuthManager = null,
                totalReps = 53, totalSets = 9, uniqueExercises = 3,
                recordedSetsToday = listOf(
                    RecordedSet(exerciseName = "Incline Bench Press", setNumber = 1, reps = 3, weight = 10.0f, notes = null, timestamp = Timestamp(System.currentTimeMillis()/1000, 200), exerciseId = "incline_bench_press", firestoreDocId = null)
                ),
                unitSystem = UnitSystem.IMPERIAL
            )
        }
    }
}
