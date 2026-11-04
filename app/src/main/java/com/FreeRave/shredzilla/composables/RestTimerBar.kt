package com.FreeRave.shredzilla.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass
import java.util.Locale

private val TimerShape = RoundedCornerShape(24.dp)

/** Frosted rest-timer strip shown while a rest countdown is running. */
@Composable
fun RestTimerBar(
    remainingSeconds: Int,
    totalSeconds: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalSeconds <= 0) return // Nothing meaningful to show

    val palette = MaterialTheme.glass
    val progress by animateFloatAsState(
        targetValue = (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "timerProgress"
    )
    val timeFormatted = String.format(Locale.US, "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassSurface(palette, TimerShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = palette.accent,
                    trackColor = palette.accent.copy(alpha = 0.15f),
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
                Icon(Icons.Filled.Timer, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(timeFormatted, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = palette.content)
                Text("Rest · next set soon", style = MaterialTheme.typography.labelMedium, color = palette.contentMuted)
            }
            GlassIconButton(Icons.Filled.Close, contentDescription = "Stop rest timer", onClick = onClose)
        }
        // Thin bar along the bottom edge mirrors the ring for at-a-glance progress
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(palette.accent.copy(alpha = 0.12f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(palette.accentGradient))
            )
        }
    }
}
