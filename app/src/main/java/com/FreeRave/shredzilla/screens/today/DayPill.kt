package com.FreeRave.shredzilla.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PillShape = RoundedCornerShape(18.dp)

/** Day in the week strip: accent fill when selected, accent outline for today, dot when trained. */
@Composable
fun DayPill(
    dayStart: Long,
    isSelected: Boolean,
    isToday: Boolean,
    hasActivity: Boolean,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.glass
    val (weekday, dayOfMonth) = remember(dayStart) {
        val date = Date(dayStart)
        SimpleDateFormat("EEE", Locale.getDefault()).format(date) to SimpleDateFormat("d", Locale.getDefault()).format(date)
    }
    val textColor = if (isSelected) palette.onAccent else palette.content

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = Modifier
            .size(width = 52.dp, height = 76.dp)
            .clip(PillShape)
            .then(
                if (isSelected) Modifier.background(Brush.verticalGradient(palette.accentGradient))
                else Modifier.glassSurface(palette, PillShape)
            )
            .border(1.5.dp, if (isToday && !isSelected) palette.accent else Color.Transparent, PillShape)
            .clickable(role = Role.Tab, onClick = onClick)
    ) {
        Text(weekday, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.75f))
        Text(dayOfMonth, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
        Box(
            Modifier
                .size(5.dp)
                .background(
                    when {
                        !hasActivity -> Color.Transparent
                        isSelected -> palette.onAccent
                        else -> palette.accent
                    },
                    CircleShape
                )
        )
    }
}
