package com.FreeRave.shredzilla.screens.settings

import androidx.compose.foundation.clickable // Added import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos // Updated import
import androidx.compose.material.icons.automirrored.filled.HelpOutline // Updated import
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.utils.RewardedAdManager
import android.app.Activity
import android.util.Log
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium // Added for new item
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToUnitSettings: () -> Unit,
    onNavigateToWorkoutReminders: () -> Unit,
    onNavigateToDefaultRestTimeSettings: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAdRewards: () -> Unit, // New navigation callback
    currentUnitSystemDisplay: String,
    currentWorkoutReminderDisplay: String,
    currentDefaultRestTimeDisplay: String,
    currentThemeDisplay: String
) {
    // Removed context, activity, and ad-related state variables as they are no longer needed here
    // var adsWatchedCount by remember { mutableStateOf(RewardedAdManager.getAdsWatchedCount(context)) }
    // var adFreeTimeRemaining by remember { mutableStateOf(RewardedAdManager.getRemainingAdFreeTimeFormatted(context)) }
    // var isAdFreeActive by remember { mutableStateOf(RewardedAdManager.isAdFree(context)) }
    // var isAdLoadingOrShowing by remember { mutableStateOf(false) }
    // val updateAdStatus = { ... }
    // LaunchedEffect to load ad is also removed. Ad loading will be handled by AdRewardsScreen.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Or primary, depending on theme
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Ad-Free Experience section removed. Will add a new item for navigation.
            
            SettingsSection(title = "Subscription & Support") { // Renamed section or can be a new one
                SettingsItem(
                    title = "Earn Rewards / Support Us",
                    subtitle = "Watch ads for an ad-free experience",
                    icon = Icons.Filled.WorkspacePremium, // Using a new icon
                    onClick = onNavigateToAdRewards
                )
                SettingsItem(title = "Pro Membership (Coming Soon)", icon = Icons.Filled.MonetizationOn) { /* TODO */ }
            }
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Account & Units") {
                SettingsItem(title = "Account", icon = Icons.Filled.AccountCircle, onClick = onNavigateToAccount)
                SettingsItem(
                    title = currentUnitSystemDisplay, // Display current selection
                    subtitle = "Unit", 
                    icon = Icons.Filled.Straighten, 
                    onClick = onNavigateToUnitSettings // Navigate to unit settings
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Workout") {
                SettingsItem(
                    title = "Workout Reminders", 
                    subtitle = currentWorkoutReminderDisplay, // Display current setting
                    icon = Icons.Filled.NotificationsActive, 
                    onClick = onNavigateToWorkoutReminders 
                )
                SettingsItem(
                    title = "Default Interset Rest", 
                    subtitle = currentDefaultRestTimeDisplay, // Display current setting
                    icon = Icons.Filled.Timer, 
                    onClick = onNavigateToDefaultRestTimeSettings // Navigate to default rest time settings
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Support & Appearance") {
                SettingsItem(title = "Help & Feedback", icon = Icons.AutoMirrored.Filled.HelpOutline) { /* TODO */ }
                SettingsItem(
                    title = "Theme", 
                    subtitle = currentThemeDisplay, // Display current theme
                    icon = Icons.Filled.BrightnessAuto, // Or a more specific theme icon
                    onClick = onNavigateToThemeSettings // Navigate to theme settings
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        // Text(
        //     text = title,
        //     style = MaterialTheme.typography.titleSmall,
        //     modifier = Modifier.padding(bottom = 8.dp),
        //     color = MaterialTheme.colorScheme.primary
        // )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkPreview() {
    ShredzillaTheme { // Removed darkTheme = true
        SettingsScreen(
            onNavigateBack = {}, 
            onNavigateToAccount = {}, 
            onNavigateToUnitSettings = {},
            onNavigateToWorkoutReminders = {},
            onNavigateToDefaultRestTimeSettings = {},
            onNavigateToThemeSettings = {},
            currentUnitSystemDisplay = "Metric (kg/km)",
            currentWorkoutReminderDisplay = "3 days inactive",
            currentDefaultRestTimeDisplay = "2min",
            currentThemeDisplay = "System Default",
            onNavigateToAdRewards = {} // Added for preview
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SettingsScreenLightPreview() {
    ShredzillaTheme { // Removed darkTheme = false
        SettingsScreen(
            onNavigateBack = {},
            onNavigateToAccount = {},
            onNavigateToUnitSettings = {},
            onNavigateToWorkoutReminders = {},
            onNavigateToDefaultRestTimeSettings = {},
            onNavigateToThemeSettings = {},
            onNavigateToAdRewards = {}, // Added for preview
            currentUnitSystemDisplay = "Imperial (lbs/miles)",
            currentWorkoutReminderDisplay = "Never",
            currentDefaultRestTimeDisplay = "1min",
            currentThemeDisplay = "Light"
        )
    }
}
