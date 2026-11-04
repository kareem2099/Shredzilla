package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassIconBadge
import com.FreeRave.shredzilla.ui.glass.GlassSelectableCard
import com.FreeRave.shredzilla.ui.glass.GlassSelectionIndicator
import com.FreeRave.shredzilla.ui.theme.glass

/** One rest-time preset; shared by onboarding and the default-rest-time settings. */
@Composable
fun RestTimeOptionCard(
    option: RestOption,
    isSelected: Boolean,
    onOptionSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.glass
    GlassSelectableCard(
        selected = isSelected,
        onClick = onOptionSelected,
        modifier = modifier.fillMaxWidth()
    ) {
        GlassIconBadge(painter = rememberVectorPainter(option.icon), size = 52.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.duration,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.content
                )
                if (option.isPopular) {
                    Spacer(Modifier.width(8.dp))
                    GlassChip(text = "Popular", highlighted = true)
                }
            }
            Text(
                text = option.goal,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent
            )
            Text(text = option.description, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        Spacer(Modifier.width(12.dp))
        GlassSelectionIndicator(selected = isSelected)
    }
}
