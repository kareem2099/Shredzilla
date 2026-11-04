package com.FreeRave.shredzilla.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.BuildConfig
import com.FreeRave.shredzilla.screens.account.composables.ProfileAvatar
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatRestDuration
import com.FreeRave.shredzilla.utils.parseRestPreference

/** Settings tab root: profile card on top, then grouped preferences. */
@Composable
fun SettingsScreen(
    userName: String?,
    userEmail: String?,
    profileImagePath: String?,
    unitSystem: UnitSystem,
    themeSetting: ThemeSetting,
    workoutReminderSetting: String,
    restTimePreference: String,
    onNavigateToAccount: () -> Unit,
    onNavigateToUnitSettings: () -> Unit,
    onNavigateToWorkoutReminders: () -> Unit,
    onNavigateToDefaultRestTimeSettings: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAdRewards: () -> Unit
) {
    val palette = MaterialTheme.glass
    Column(modifier = Modifier.fillMaxSize()) {
        GlassTopBar(title = "Settings")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProfileCard(userName, userEmail, profileImagePath, onClick = onNavigateToAccount)

            GlassSettingsGroup(title = "Support") {
                GlassSettingsRow(
                    icon = Icons.Filled.VolunteerActivism,
                    title = "Earn rewards",
                    subtitle = "Watch ads for an ad-free experience",
                    onClick = onNavigateToAdRewards
                )
                GlassSettingsRow(
                    icon = Icons.Filled.WorkspacePremium,
                    title = "Pro membership",
                    onClick = { /* TODO: Pro membership */ },
                    showChevron = false,
                    trailing = { GlassChip(text = "Soon") }
                )
            }

            GlassSettingsGroup(title = "Workout") {
                GlassSettingsRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Workout reminders",
                    value = reminderLabel(workoutReminderSetting),
                    onClick = onNavigateToWorkoutReminders
                )
                GlassSettingsRow(
                    icon = Icons.Filled.Timer,
                    title = "Rest between sets",
                    value = formatRestDuration(parseRestPreference(restTimePreference)),
                    onClick = onNavigateToDefaultRestTimeSettings
                )
            }

            GlassSettingsGroup(title = "Preferences") {
                GlassSettingsRow(
                    icon = Icons.Filled.Straighten,
                    title = "Units",
                    value = if (unitSystem == UnitSystem.METRIC) "Metric" else "Imperial",
                    onClick = onNavigateToUnitSettings
                )
                GlassSettingsRow(
                    icon = Icons.Filled.BrightnessAuto,
                    title = "Theme",
                    value = themeSetting.displayName,
                    onClick = onNavigateToThemeSettings
                )
            }

            GlassSettingsGroup(title = "About") {
                GlassSettingsRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "Help & feedback",
                    onClick = { /* TODO: Help & feedback */ }
                )
            }

            Text(
                text = "Shredzilla ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.contentMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** "Never" stays as is; "3 days inactive" reads better as "After 3 days". */
internal fun reminderLabel(setting: String): String =
    setting.substringBefore(" inactive").takeIf { it != setting }?.let { "After $it" } ?: setting

@Composable
private fun ProfileCard(userName: String?, userEmail: String?, imagePath: String?, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassDefaults.CardShape)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(imagePath = imagePath, name = userName ?: userEmail, size = 60.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName?.takeIf { it.isNotBlank() } ?: "Set your name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (userEmail != null) {
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted, maxLines = 1)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Account", tint = palette.contentMuted)
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ShredzillaTheme {
        SettingsScreen(
            userName = "Alex Doe",
            userEmail = "alex@example.com",
            profileImagePath = null,
            unitSystem = UnitSystem.METRIC,
            themeSetting = ThemeSetting.SYSTEM,
            workoutReminderSetting = "3 days inactive",
            restTimePreference = "2min",
            onNavigateToAccount = {},
            onNavigateToUnitSettings = {},
            onNavigateToWorkoutReminders = {},
            onNavigateToDefaultRestTimeSettings = {},
            onNavigateToThemeSettings = {},
            onNavigateToAdRewards = {}
        )
    }
}
