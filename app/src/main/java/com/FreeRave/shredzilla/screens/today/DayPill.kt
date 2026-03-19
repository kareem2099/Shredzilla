package com.FreeRave.shredzilla.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.*
import java.util.Calendar

@Composable
fun DayPill(dayItem: DayItem, isSelected: Boolean, onDaySelected: () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val genderTheme = ThemeManager.currentGenderTheme
    val todayCalendar = Calendar.getInstance()
    val itemCalendar = Calendar.getInstance().apply { time = dayItem.fullDate }
    val isToday = todayCalendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR) &&
                  todayCalendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR)

    val backgroundColor = when {
        isToday && dayItem.hasActivity -> if (genderTheme == "Female") (if (darkTheme) FemaleWorkoutDayPinkDark else FemaleWorkoutDayPinkLight) else (if (darkTheme) WorkoutDayBlueDark else WorkoutDayBlueLight)
        isToday -> if (genderTheme == "Female") (if (darkTheme) FemaleTodayHighlightPinkDark else FemaleTodayHighlightPinkLight) else (if (darkTheme) TodayHighlightGreenDark else TodayHighlightGreenLight)
        dayItem.hasActivity -> if (genderTheme == "Female") (if (darkTheme) FemaleWorkoutDayPinkDark else FemaleWorkoutDayPinkLight) else (if (darkTheme) WorkoutDayBlueDark else WorkoutDayBlueLight)
        else -> if (genderTheme == "Female") (if (darkTheme) FemaleNoActivityDayGrayDark else FemaleNoActivityDayGrayLight) else (if (darkTheme) NoActivityDayGrayDark else NoActivityDayGrayLight)
    }.copy(alpha = if (isSelected) 1f else 0.7f)

    val textColor = when {
         isToday && dayItem.hasActivity -> if (darkTheme) Color.Black else Color.White
         isToday -> if (darkTheme) Color.Black else Color.White
         dayItem.hasActivity -> if (darkTheme) Color.Black else Color.White
         isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
     val pillModifier = Modifier
        .clip(RoundedCornerShape(8.dp)) // Slightly smaller rounding
        .clickable(onClick = onDaySelected)
        .background(backgroundColor)
        .padding(vertical = 6.dp, horizontal = 8.dp) // Reduced horizontal padding

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = pillModifier
    ) {
        Text(
            text = dayItem.dayOfWeekShort.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dayItem.dayOfMonth,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
