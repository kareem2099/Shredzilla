package com.FreeRave.shredzilla.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun DayPill(
    dayItem: DayItem,
    isSelected: Boolean,
    onDaySelected: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onDaySelected() }.padding(4.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = 45.dp, height = 60.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dayItem.dayOfWeekShort.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = dayItem.dayOfMonth, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }

        if (dayItem.hasActivity) {
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        } else {
            // Invisible spacer to maintain height even without a dot
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
