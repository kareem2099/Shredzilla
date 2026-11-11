package com.FreeRave.shredzilla.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.FreeRave.shredzilla.notifications.WorkoutNotificationStyle
import com.FreeRave.shredzilla.pro.LocalProAccess
import com.FreeRave.shredzilla.pro.ProPill
import com.FreeRave.shredzilla.ui.glass.glassSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.notifications.NotificationChoices
import com.FreeRave.shredzilla.notifications.rememberNotificationPermission
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassNumberRow
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.glass.glassSwitchColors
import com.FreeRave.shredzilla.ui.theme.glass
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val REMINDER_OFF = "Never"
private val ReminderDayRange = 1..8
private val ReminderHours = listOf(7, 9, 12, 15, 17, 18, 19, 20, 21)

/** The inactivity reminder is stored as "Never" or "N day(s) inactive" in Firestore ("workoutReminder"). */
fun parseReminderDays(setting: String): Int? = setting.substringBefore(' ').toIntOrNull()
fun reminderSettingOf(days: Int?): String =
    days?.let { "$it ${if (it == 1) "day" else "days"} inactive" } ?: REMINDER_OFF

private fun hourLabel(hour: Int): String = LocalTime.of(hour, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/**
 * Every notification in one place: the permission itself, the rest timer, and each reminder with
 * its own switch. Changes save as they're made.
 */
@Composable
fun NotificationSettingsScreen(
    choices: NotificationChoices,
    hasWeeklyGoal: Boolean,
    measurementRhythm: String,
    onChange: (NotificationChoices) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val palette = MaterialTheme.glass
    val permission = rememberNotificationPermission()
    val pro = LocalProAccess.current

    GlassSubpageScaffold(title = "Notifications", eyebrow = "Settings", onBack = onNavigateBack) {
        if (!permission.granted) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.error.copy(alpha = 0.5f), GlassDefaults.CardShape)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = palette.error, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications are off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                        Text(
                            "The rest timer can't show on your lock screen and no reminder below can reach you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.contentMuted
                        )
                    }
                }
                GlassPrimaryButton(text = "Turn on notifications", onClick = permission::request)
            }
        }

        InfoRow(
            icon = Icons.Filled.Timer,
            title = "Rest timer",
            subtitle = "A countdown on the lock screen while you rest, and an alert when it's time for the next set.",
            status = if (permission.granted) "On" else "Off"
        )

        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SwitchRow(
                icon = Icons.Filled.FitnessCenter,
                title = "Workout notification",
                subtitle = "Log sets, see your rest and move on from the lock screen during a workout",
                checked = choices.workoutNotification,
                onCheckedChange = { onChange(choices.copy(workoutNotification = it)) }
            )
            AnimatedVisibility(visible = choices.workoutNotification) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Look", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.content)
                    WorkoutNotificationStyle.entries.forEach { style ->
                        StylePreviewCard(
                            style = style,
                            selected = choices.workoutStyle == style && (!style.isPro || pro.isPro),
                            locked = style.isPro && !pro.isPro,
                            onClick = { if (style.isPro && !pro.isPro) pro.openPro() else onChange(choices.copy(workoutStyle = style)) }
                        )
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SwitchRow(
                icon = Icons.Filled.NotificationsActive,
                title = "Workout reminder",
                subtitle = "A nudge after a few days without training, with your next list ready to start",
                checked = choices.reminderDays != null,
                onCheckedChange = { on -> onChange(choices.copy(reminderDays = if (on) NotificationChoices.DEFAULT_REMINDER_DAYS else null)) }
            )
            AnimatedVisibility(visible = choices.reminderDays != null) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val days = choices.reminderDays ?: NotificationChoices.DEFAULT_REMINDER_DAYS
                    Text(
                        "After $days ${if (days == 1) "day" else "days"} without a workout",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.content
                    )
                    GlassNumberRow(range = ReminderDayRange, selected = choices.reminderDays, onSelect = { onChange(choices.copy(reminderDays = it)) })
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SwitchRow(
                icon = Icons.Filled.EmojiEvents,
                title = "Weekly goal",
                subtitle = if (hasWeeklyGoal) "When your goal needs a workout on every day that's left, so the streak survives"
                else "Set a weekly goal during setup to use this",
                checked = choices.weeklyGoal && hasWeeklyGoal,
                enabled = hasWeeklyGoal,
                onCheckedChange = { onChange(choices.copy(weeklyGoal = it)) }
            )
            SwitchRow(
                icon = Icons.Filled.Straighten,
                title = "Body measurements",
                subtitle = "When it's time to measure again (${measurementRhythm.lowercase()}, from the Body page)",
                checked = choices.measurements,
                onCheckedChange = { onChange(choices.copy(measurements = it)) }
            )
            SwitchRow(
                icon = Icons.Filled.WorkspacePremium,
                title = "Pro time ending",
                subtitle = "Ten minutes before your Pro hours from ads run out",
                checked = choices.proEnding,
                onCheckedChange = { onChange(choices.copy(proEnding = it)) }
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Remind me at", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ReminderHours.forEach { hour ->
                    GlassFilterChip(text = hourLabel(hour), selected = choices.reminderHour == hour, onClick = { onChange(choices.copy(reminderHour = hour)) })
                }
            }
            Text(
                "Workout, weekly goal and measurement reminders arrive around this time. Nothing is sent between 10 PM and 8 AM.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted
            )
        }

        GlassFootnote("Reminders are checked on your phone, also without internet. Each kind can be silenced in Android's settings too.")
        Spacer(Modifier.padding(4.dp))
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val palette = MaterialTheme.glass
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlassIconBadge(painter = rememberVectorPainter(icon), size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (enabled) palette.content else palette.contentMuted)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, colors = glassSwitchColors())
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String, status: String) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconBadge(painter = rememberVectorPainter(icon), size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.content)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
            Spacer(Modifier.width(8.dp))
            GlassChip(text = status, highlighted = status == "On")
        }
    }
}

/** A small mock of the workout notification in [style], so the choice is visual. */
@Composable
private fun StylePreviewCard(style: WorkoutNotificationStyle, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    val shape = RoundedCornerShape(18.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .glassSurface(palette, shape)
            .border(if (selected) 2.dp else 0.dp, if (selected) palette.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(style.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.content)
                    if (style.isPro) ProPill()
                }
                Text(style.description, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
            }
            when {
                selected -> Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = palette.accent)
                locked -> Icon(Icons.Filled.Lock, contentDescription = "Pro", tint = palette.contentMuted)
            }
        }
        // The mock notification
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.content.copy(alpha = 0.06f))
                .padding(10.dp)
        ) {
            val title = MaterialTheme.typography.labelLarge
            val body = MaterialTheme.typography.labelMedium
            when (style) {
                WorkoutNotificationStyle.DETAILED -> {
                    MockLine("Lat Pulldown · Set 3 of 3", "0:45", title, palette.content)
                    Text("Resting · next 9 plates × 8", style = body, color = palette.contentMuted)
                    Text("Today: 8 plates × 14 · 9 plates × 7", style = body, color = palette.contentMuted)
                    Text("Up next: Seated Row · 12 sets · 2 records", style = body, color = palette.contentMuted)
                    MockButtons("Log 9 × 8", "+1 rep", "Edit")
                }
                WorkoutNotificationStyle.COMPACT -> {
                    MockLine("Lat Pulldown · Set 3 of 3", "0:45", title, palette.content)
                    Text("Ready: 9 plates × 8", style = body, color = palette.contentMuted)
                    MockButtons("Log 9 × 8", "Edit")
                }
                WorkoutNotificationStyle.FOCUS -> {
                    MockLine("Rest · Lat Pulldown", "0:45", MaterialTheme.typography.titleMedium, palette.content)
                    Text("Next: 9 plates × 8", style = body, color = palette.contentMuted)
                    MockButtons("Log 9 × 8", "+1 rep", "Skip rest")
                }
                WorkoutNotificationStyle.PROGRESS -> {
                    MockLine("Lat Pulldown · Set 3 of 3", "12/21", title, palette.content)
                    Text("12 of 21 sets · Ready: 9 plates × 8", style = body, color = palette.contentMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        listOf(1f, 1f, 1f, 0.33f, 0f, 0f, 0f).forEach { done ->
                            Row(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.content.copy(alpha = 0.12f))
                            ) {
                                if (done > 0f) Box(Modifier.fillMaxWidth(done).height(6.dp).background(palette.accent))
                            }
                        }
                    }
                    MockButtons("Log 9 × 8", "+1 rep", "Edit")
                }
            }
        }
    }
}

@Composable
private fun MockLine(text: String, trailing: String, style: androidx.compose.ui.text.TextStyle, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = style, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f), maxLines = 1)
        Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.glass.accent)
    }
}

@Composable
private fun MockButtons(vararg labels: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 2.dp)) {
        labels.forEach { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.glass.accent) }
    }
}
