package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager

val workoutFrequencyOptions = listOf(
    "Once a week",
    "2 times per week",
    "3 times per week",
    "4 times per week",
    "5 times per week",
    "6 times per week",
    "Every day"
)

// Corresponding nudge days (example, can be configured)
val nudgeDaysOptions = listOf(7, 6, 5, 4, 3, 2, 1)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalScreen(onWeeklyGoalSelected: (frequency: String, nudgeDays: Int) -> Unit) {
    var selectedFrequencyIndex by remember { mutableStateOf(0) }

    val gender = ThemeManager.currentGenderTheme
    // Using placeholder names as per user confirmation.
    // Ensure these drawables exist or replace with actual names.
    val backgroundImageRes = if (gender == "Female") {
        R.drawable.fifth_page_female
    } else {
        R.drawable.fifth_page_male
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundImageRes),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Set Weekly Goal Icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Set a Weekly Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Staying active is the only way you'll\nprogress. We will nudge you if you\n.ever miss your goal",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "?How often will you workout",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (selectedFrequencyIndex > 0) {
                                selectedFrequencyIndex--
                            }
                        },
                        enabled = selectedFrequencyIndex > 0,
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease frequency", tint = MaterialTheme.colorScheme.onPrimary)
                    }

                    Text(
                        text = workoutFrequencyOptions[selectedFrequencyIndex],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            if (selectedFrequencyIndex < workoutFrequencyOptions.size - 1) {
                                selectedFrequencyIndex++
                            }
                        },
                        enabled = selectedFrequencyIndex < workoutFrequencyOptions.size - 1,
                         modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase frequency", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Get nudged after ${nudgeDaysOptions[selectedFrequencyIndex]} days of .inactivity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Nudge notification",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onWeeklyGoalSelected(
                        workoutFrequencyOptions[selectedFrequencyIndex],
                        nudgeDaysOptions[selectedFrequencyIndex]
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Next")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "Weekly Goal Light Male")
@Composable
fun WeeklyGoalScreenMaleLightPreview() {
    ThemeManager.currentGenderTheme = "Male"
    // ThemeManager.themePreferenceMale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        Surface { WeeklyGoalScreen { _, _ -> } }
    }
}

@Preview(showBackground = true, name = "Weekly Goal Dark Male")
@Composable
fun WeeklyGoalScreenMaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Male"
    // ThemeManager.themePreferenceMale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        Surface { WeeklyGoalScreen { _, _ -> } }
    }
}

@Preview(showBackground = true, name = "Weekly Goal Light Female")
@Composable
fun WeeklyGoalScreenFemaleLightPreview() {
    ThemeManager.currentGenderTheme = "Female"
    // ThemeManager.themePreferenceFemale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        Surface { WeeklyGoalScreen { _, _ -> } }
    }
}

@Preview(showBackground = true, name = "Weekly Goal Dark Female")
@Composable
fun WeeklyGoalScreenFemaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Female"
    // ThemeManager.themePreferenceFemale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        Surface { WeeklyGoalScreen { _, _ -> } }
    }
}
