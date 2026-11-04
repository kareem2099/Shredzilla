package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.onboarding.components.OnboardingScaffold
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassNumberRow
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

/** Stored as "weeklyGoalFrequency"; index = workouts per week - 1, so keep these strings stable. */
val workoutFrequencyOptions = listOf(
    "Once a week",
    "2 times per week",
    "3 times per week",
    "4 times per week",
    "5 times per week",
    "6 times per week",
    "Every day"
)

private const val DAYS_IN_WEEK = 7

/** Days without a workout before we nudge: the more often you train, the shorter the grace period. */
private fun nudgeDaysFor(daysPerWeek: Int) = DAYS_IN_WEEK + 1 - daysPerWeek

private fun intensityLabel(daysPerWeek: Int) = when (daysPerWeek) {
    1, 2 -> "Easy start"
    3, 4 -> "Balanced"
    5, 6 -> "Dedicated"
    else -> "All in"
}

/** Step 5 — weekly workout target; drives the inactivity nudges. */
@Composable
fun WeeklyGoalScreen(
    onWeeklyGoalSelected: (frequency: String, nudgeDays: Int) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    var daysPerWeek by rememberSaveable { mutableIntStateOf(3) }
    val palette = MaterialTheme.glass
    val nudgeDays = nudgeDaysFor(daysPerWeek)

    OnboardingScaffold(
        step = 5,
        title = "Set your weekly goal",
        subtitle = "Consistency beats intensity. Pick a pace you can keep.",
        ctaText = "Start training",
        ctaLoading = isLoading,
        onBack = onBack,
        onCta = { onWeeklyGoalSelected(workoutFrequencyOptions[daysPerWeek - 1], nudgeDays) }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            GlassChip(
                text = intensityLabel(daysPerWeek),
                highlighted = true,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            AnimatedContent(
                targetState = daysPerWeek,
                transitionSpec = { (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith fadeOut() },
                label = "daysPerWeek",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { days ->
                Text(text = days.toString(), fontSize = 88.sp, fontWeight = FontWeight.Black, color = palette.content)
            }
            Text(
                text = "days per week",
                style = MaterialTheme.typography.titleMedium,
                color = palette.contentMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            GlassNumberRow(
                range = 1..DAYS_IN_WEEK,
                selected = daysPerWeek,
                onSelect = { daysPerWeek = it },
                fillUpToSelected = true
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconBadge(painter = rememberVectorPainter(Icons.Filled.NotificationsActive), size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "We'll nudge you after $nudgeDays ${if (nudgeDays == 1) "day" else "days"} without a workout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.contentMuted
                )
            }
        }
    }
}

@Preview
@Composable
private fun WeeklyGoalPreview() {
    ShredzillaTheme {
        WeeklyGoalScreen(onWeeklyGoalSelected = { _, _ -> }, onBack = {})
    }
}
