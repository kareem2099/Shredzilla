package com.FreeRave.shredzilla.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass
import kotlinx.coroutines.delay

private const val SHOW_MILLIS = 3500L

/** A set that beat the exercise's best so far. [id] makes back-to-back records re-trigger. */
data class PrCelebration(
    val exerciseName: String,
    val setLabel: String,
    val id: Long = System.nanoTime(),
    /** Badges reuse the banner with their own headline and emoji. */
    val headline: String = "New personal record!",
    val emoji: String? = null,
)

/** Slides in from the top over everything, buzzes, and leaves on its own (or on tap). */
@Composable
fun PrCelebrationBanner(celebration: PrCelebration?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    val haptics = LocalHapticFeedback.current
    // Keep the last record around so the exit animation still has something to draw
    var shown by remember { mutableStateOf(celebration) }
    LaunchedEffect(celebration?.id) {
        if (celebration == null) return@LaunchedEffect
        shown = celebration
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(SHOW_MILLIS)
        onDismiss()
    }

    AnimatedVisibility(
        visible = celebration != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val record = shown ?: return@AnimatedVisibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(GlassDefaults.CardShape)
                // Opaque base so the page underneath can't show through the frosted card
                .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
                .glassSurface(palette, GlassDefaults.CardShape)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(palette.accentGradient))
            ) {
                if (record.emoji != null) Text(record.emoji, style = MaterialTheme.typography.titleLarge)
                else Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = palette.onAccent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(record.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                Text(
                    listOf(record.exerciseName, record.setLabel).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.contentMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
