package com.FreeRave.shredzilla.screens.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.pro.SupportTip
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass

/**
 * Tips only: a pre-workout shot, a protein scoop or a creatine tub. They thank the developer and
 * add a Supporter badge; Pro has its own page.
 */
@Composable
fun SupportScreen(
    supporterTips: Int,
    prices: Map<String, String>,
    onBuy: (productId: String) -> Unit,
    onOpenPro: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = MaterialTheme.glass
    GlassSubpageScaffold(title = "Support the developer", onBack = onBack) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(palette)
                .background(Brush.verticalGradient(listOf(SupporterPink.copy(alpha = 0.16f), Color.Transparent)))
                .padding(vertical = 26.dp, horizontal = 20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(SupporterPink.copy(alpha = 0.18f))
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = SupporterPink, modifier = Modifier.size(38.dp))
            }
            Text(
                if (supporterTips > 0) "Thank you for the fuel!" else "Fuel the developer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = palette.content,
                textAlign = TextAlign.Center
            )
            Text(
                if (supporterTips > 0) "You've sent ${if (supporterTips == 1) "1 tip" else "$supporterTips tips"}. Every one goes into the next update."
                else "DotShredzilla is built by one person, with no forced ads. A tip keeps the updates coming.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.contentMuted,
                textAlign = TextAlign.Center
            )
            if (supporterTips > 0) SupporterBadge()
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Pick a tip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            SupportTip.entries.forEach { tip ->
                TipRow(tip, price = prices[tip.productId] ?: tip.fallbackPrice, onClick = { onBuy(tip.productId) })
            }
            Text(
                "Tips can be sent as often as you like. Each one adds to your Supporter badge.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.contentMuted
            )
        }

        Text(
            "Looking for Pro features? See DotShredzilla Pro",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenPro)
                .padding(12.dp)
        )
        GlassFootnote("Payments are handled by Google Play. Tips are optional thank-yous: they don't unlock Pro or any other feature.")
        Spacer(Modifier.height(8.dp))
    }
}
