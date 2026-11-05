package com.FreeRave.shredzilla.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass

/** Slim strip under the status bar while offline, so nobody wonders whether their sets were saved. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassSurface(palette, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Text(
            "Offline · sets are saved and sync when you're back online",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.content
        )
    }
}
