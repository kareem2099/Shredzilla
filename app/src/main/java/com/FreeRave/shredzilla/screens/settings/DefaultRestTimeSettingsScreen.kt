package com.FreeRave.shredzilla.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.onboarding.RestTimeOptionCard
import com.FreeRave.shredzilla.onboarding.restOptions
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatRestDuration
import com.FreeRave.shredzilla.utils.parseRestPreference
import com.FreeRave.shredzilla.utils.restPreferenceOf

private const val STEP_SECONDS = 15
private val RestRangeSeconds = 15..600

/** Default rest between sets: fine-tune in 15s steps or tap a preset, then save. */
@Composable
fun DefaultRestTimeSettingsScreen(
    currentPreference: String,
    onSave: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var seconds by remember { mutableIntStateOf(parseRestPreference(currentPreference)) }
    val palette = MaterialTheme.glass

    GlassSubpageScaffold(
        title = "Rest between sets",
        eyebrow = "Settings",
        onBack = onNavigateBack,
        bottomBar = { GlassPrimaryButton(text = "Save", onClick = { onSave(restPreferenceOf(seconds)) }) }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassIconButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = "15 seconds less",
                    onClick = { seconds = (seconds - STEP_SECONDS).coerceIn(RestRangeSeconds) }
                )
                AnimatedContent(
                    targetState = seconds,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "restSeconds"
                ) { shown ->
                    Text(formatRestDuration(shown), fontSize = 56.sp, fontWeight = FontWeight.Black, color = palette.content)
                }
                GlassIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "15 seconds more",
                    onClick = { seconds = (seconds + STEP_SECONDS).coerceIn(RestRangeSeconds) }
                )
            }
        }
        restOptions.forEach { option ->
            val presetSeconds = parseRestPreference(option.id)
            RestTimeOptionCard(
                option = option,
                isSelected = seconds == presetSeconds,
                onOptionSelected = { seconds = presetSeconds }
            )
        }
        GlassFootnote("The timer starts after every set you record and resets for the next one.")
    }
}

@Preview
@Composable
private fun DefaultRestTimeSettingsScreenPreview() {
    ShredzillaTheme {
        DefaultRestTimeSettingsScreen(currentPreference = "2min", onSave = {}, onNavigateBack = {})
    }
}
