package com.FreeRave.shredzilla.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.DELOAD_EVERY_WEEKS
import com.FreeRave.shredzilla.utils.WeeklyProgress

/** This week's workouts against the weekly goal, the streak, and whether this is a deload week. */
@Composable
fun WeeklyGoalCard(progress: WeeklyProgress, onToggleDeload: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    val goal = progress.goal
    GlassCard(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                val fraction = if (goal == null || goal == 0) 0f else (progress.workoutsThisWeek.toFloat() / goal).coerceAtMost(1f)
                Canvas(Modifier.size(72.dp)) {
                    val stroke = 8.dp.toPx()
                    val inset = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(palette.border.copy(alpha = 0.3f), 0f, 360f, false, inset, arcSize, style = Stroke(stroke))
                    drawArc(
                        Brush.sweepGradient(palette.accentGradient),
                        -90f, 360f * fraction, false, inset, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    if (goal != null) "${progress.workoutsThisWeek}/$goal" else "${progress.workoutsThisWeek}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = palette.content
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        progress.isDeloadWeek -> "Deload week"
                        goal != null && progress.workoutsThisWeek >= goal -> "Weekly goal done!"
                        else -> "Workouts this week"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.content
                )
                Text(
                    if (progress.isDeloadWeek) "Lighter loads, about half the sets. Your streak is paused, not broken."
                    else if (progress.streakWeeks > 0) "🔥 ${progress.streakWeeks}-week streak"
                    else "Hit your goal to start a streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.contentMuted
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Short enough to never wrap next to the deload button
            GlassChip(text = "Week ${progress.weeksSinceDeload + 1}/$DELOAD_EVERY_WEEKS")
            Spacer(Modifier.weight(1f))
            GlassFilterChip(
                text = if (progress.isDeloadWeek) "Deload week ✓" else "Mark as deload",
                selected = progress.isDeloadWeek,
                onClick = onToggleDeload
            )
        }
    }
}

/**
 * After [DELOAD_EVERY_WEEKS] hard weeks: a friendly nudge to take it lighter, never enforced.
 * "Not now" hides it until next week.
 */
@Composable
fun DeloadReminderCard(weeks: Int, onMarkDeload: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(10.dp))
            Text("Time for a deload week?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
        }
        Text(
            "You've trained hard for $weeks weeks in a row. A lighter week, around half the sets at about 60% of your usual " +
                "weight, lets your joints, muscles and nervous system recover, so you come back stronger. It's your call.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.contentMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassFilterChip(text = "Make this a deload week", selected = true, onClick = onMarkDeload)
            TextButton(onClick = onDismiss) { Text("Not now", color = palette.contentMuted) }
        }
    }
}
