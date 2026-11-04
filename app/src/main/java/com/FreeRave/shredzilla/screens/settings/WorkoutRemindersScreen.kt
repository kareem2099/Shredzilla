package com.FreeRave.shredzilla.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassNumberRow
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.glass.glassSwitchColors
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

private const val REMINDER_OFF = "Never"
private val ReminderDayRange = 1..8
private const val DEFAULT_REMINDER_DAYS = 3

/** Stored as "Never" or "N day(s) inactive" in Firestore ("workoutReminder"). */
private fun parseReminderDays(setting: String): Int? = setting.substringBefore(' ').toIntOrNull()
private fun reminderSettingOf(days: Int?): String =
    days?.let { "$it ${if (it == 1) "day" else "days"} inactive" } ?: REMINDER_OFF

/** Inactivity reminder: on/off plus how many idle days before the nudge. Saves on every change. */
@Composable
fun WorkoutRemindersScreen(
    onNavigateBack: () -> Unit,
    currentReminderSetting: String,
    onReminderSettingChange: (String) -> Unit
) {
    var days by remember { mutableStateOf(parseReminderDays(currentReminderSetting)) }
    val palette = MaterialTheme.glass

    fun update(newDays: Int?) {
        days = newDays
        onReminderSettingChange(reminderSettingOf(newDays))
    }

    GlassSubpageScaffold(title = "Workout reminders", eyebrow = "Settings", onBack = onNavigateBack) {
        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.NotificationsActive), size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remind me", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                    Text(
                        "Muscle growth slows after long breaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.contentMuted
                    )
                }
                Switch(
                    checked = days != null,
                    onCheckedChange = { on -> update(if (on) DEFAULT_REMINDER_DAYS else null) },
                    colors = glassSwitchColors()
                )
            }
            AnimatedVisibility(visible = days != null) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "After ${days ?: DEFAULT_REMINDER_DAYS} ${if (days == 1) "day" else "days"} without a workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.content
                    )
                    GlassNumberRow(range = ReminderDayRange, selected = days, onSelect = ::update)
                }
            }
        }
    }
}

@Preview
@Composable
private fun WorkoutRemindersScreenPreview() {
    ShredzillaTheme {
        WorkoutRemindersScreen(onNavigateBack = {}, currentReminderSetting = "3 days inactive", onReminderSettingChange = {})
    }
}
