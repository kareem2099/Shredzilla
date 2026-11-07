package com.FreeRave.shredzilla.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass

/** Pro's own gold, the same on every profile and accent so "Pro" always reads as Pro. */
val ProGold = listOf(Color(0xFFFFE27A), Color(0xFFFFB300), Color(0xFFFF7A00))
val ProGoldSolid = Color(0xFFFFB300)
private val OnGold = Color(0xFF2A1A00)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Small gold "PRO" pill, used next to titles of Pro features. */
@Composable
fun ProPill(modifier: Modifier = Modifier, text: String = "PRO") {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = OnGold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(ProGold))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** A gold crown in a slowly turning halo. */
@Composable
fun ProCrown(modifier: Modifier = Modifier, size: Dp = 84.dp) {
    val transition = rememberInfiniteTransition(label = "crown")
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "turn")
    val glow by transition.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(ProGoldSolid.copy(alpha = glow), Color.Transparent), radius = this.size.minDimension))
            }
    ) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { rotationZ = turn }
                .border(2.5.dp, Brush.sweepGradient(ProGold + Color.Transparent + ProGold.first()), CircleShape)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size - 14.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(ProGold, start = Offset.Zero, end = Offset.Infinite))
        ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = OnGold, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/**
 * Stand-in for a Pro feature the user doesn't have: says what it would show and opens the
 * Pro page. Free features never show this.
 */
@Composable
fun ProLockedCard(title: String, description: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    val pro = LocalProAccess.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(palette)
            .border(1.dp, Brush.horizontalGradient(ProGold.map { it.copy(alpha = 0.6f) }), com.FreeRave.shredzilla.ui.glass.GlassDefaults.CardShape)
            .clickable(onClick = pro.openPro)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(ProGold))
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = OnGold, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.content)
                ProPill()
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
    }
}
