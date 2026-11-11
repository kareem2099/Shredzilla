package com.FreeRave.shredzilla.screens.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.FreeRave.shredzilla.badges.BadgeCategory
import com.FreeRave.shredzilla.badges.BadgeStatus
import com.FreeRave.shredzilla.badges.BadgeTier
import com.FreeRave.shredzilla.badges.ProgressFormat
import com.FreeRave.shredzilla.badges.progressToKg
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassChip
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatStoredWeight
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/** Ring colors per tier: bronze, silver, gold, and a pink-to-violet gradient for special badges. */
fun BadgeTier.colors(): List<Color> = when (this) {
    BadgeTier.BRONZE -> listOf(Color(0xFFE0A06A), Color(0xFFA86232))
    BadgeTier.SILVER -> listOf(Color(0xFFE3E9F2), Color(0xFF8E9BB0))
    BadgeTier.GOLD -> listOf(Color(0xFFFFE27A), Color(0xFFFF9F00))
    BadgeTier.SPECIAL -> listOf(Color(0xFFFF4F7B), Color(0xFF7C4DFF))
}

private val BadgeTier.label: String get() = when (this) {
    BadgeTier.BRONZE -> "Bronze"
    BadgeTier.SILVER -> "Silver"
    BadgeTier.GOLD -> "Gold"
    BadgeTier.SPECIAL -> "Special"
}

/** Every badge, earned or not: events first, then each category, with progress toward the locked ones. */
@Composable
fun BadgesScreen(statuses: List<BadgeStatus>, unitSystem: UnitSystem, onBack: () -> Unit) {
    val palette = MaterialTheme.glass
    var open by remember { mutableStateOf<BadgeStatus?>(null) }
    val now = System.currentTimeMillis()
    val earned = statuses.filter { it.isEarned }
    // Upcoming and running events are shown on their own card; past ones only once earned
    val liveEvents = statuses.filter { it.badge.category == BadgeCategory.EVENTS && (it.badge.endsAt ?: 0L) >= now }
    val visible = statuses.filter { it.badge.category != BadgeCategory.EVENTS || it.isEarned }

    GlassSubpageScaffold(title = "Badges", eyebrow = "Your progress", onBack = onBack) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${earned.size} of ${visible.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = palette.content)
                    Text("badges earned", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
                }
                Text(
                    earned.sortedByDescending { it.earnedAtMillis ?: 0L }.take(4).joinToString(" ") { it.badge.emoji },
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            ProgressBar(if (visible.isEmpty()) 0f else earned.size.toFloat() / visible.size, palette.accentGradient)
        }

        liveEvents.forEach { EventCard(it, now, onClick = { open = it }) }

        BadgeCategory.entries.forEach { category ->
            val inCategory = visible.filter { it.badge.category == category }
            if (inCategory.isEmpty()) return@forEach
            Text(
                "${category.title.uppercase()} · ${inCategory.count { it.isEarned }}/${inCategory.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.contentMuted,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp)
            )
            inCategory.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { status ->
                        BadgeTile(status, unitSystem, Modifier.weight(1f), onClick = { open = status })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        GlassFootnote("Badges are free for everyone. Events come and go, so watch this page for limited-time badges.")
        Spacer(Modifier.height(8.dp))
    }

    open?.let { status -> BadgeDetail(status, unitSystem, now, onDismiss = { open = null }) }
}

@Composable
private fun BadgeMedal(status: BadgeStatus, size: Int) {
    val palette = MaterialTheme.glass
    val colors = status.badge.tier.colors()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size.dp)) {
        Box(
            Modifier
                .size(size.dp)
                .then(
                    if (status.isEarned) Modifier
                        .drawBehind { drawCircle(Brush.radialGradient(listOf(colors.first().copy(alpha = 0.45f), Color.Transparent))) }
                        .border(3.dp, Brush.linearGradient(colors), CircleShape)
                    else Modifier.border(1.5.dp, palette.border.copy(alpha = 0.5f), CircleShape)
                )
                .padding(5.dp)
                .clip(CircleShape)
                .background(if (status.isEarned) Brush.linearGradient(colors.map { it.copy(alpha = 0.22f) }) else Brush.linearGradient(listOf(palette.fill, palette.fill)))
        )
        Text(status.badge.emoji, fontSize = (size * 0.42f).sp, modifier = Modifier.alpha(if (status.isEarned) 1f else 0.28f))
        if (!status.isEarned) {
            Icon(
                Icons.Filled.Lock, contentDescription = "Locked", tint = palette.contentMuted,
                modifier = Modifier.align(Alignment.BottomEnd).size((size * 0.26f).dp)
            )
        }
    }
}

@Composable
private fun BadgeTile(status: BadgeStatus, unitSystem: UnitSystem, modifier: Modifier, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .glassSurface(palette, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        BadgeMedal(status, 60)
        Text(
            status.badge.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = if (status.isEarned) palette.content else palette.contentMuted,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Text(
            subline(status, unitSystem), style = MaterialTheme.typography.labelSmall, color = palette.contentMuted,
            textAlign = TextAlign.Center, maxLines = 1
        )
    }
}

private fun subline(status: BadgeStatus, unitSystem: UnitSystem): String = when {
    status.earnedAtMillis != null -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(status.earnedAtMillis))
    status.isEarned -> "Earned"
    status.badge.format == ProgressFormat.WEIGHT_KG -> if (status.current > 0) "Best ${formatStoredWeight(progressToKg(status.current), unitSystem)}" else "Locked"
    status.badge.goal > 1 -> "${status.current.coerceAtMost(status.badge.goal)} / ${status.badge.goal}"
    else -> "Locked"
}

@Composable
private fun EventCard(status: BadgeStatus, now: Long, onClick: () -> Unit) {
    val palette = MaterialTheme.glass
    val starts = status.badge.startsAt ?: 0L
    val ends = status.badge.endsAt ?: 0L
    val days = { millis: Long -> TimeUnit.MILLISECONDS.toDays(millis).coerceAtLeast(0) }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .glassSurface(palette, RoundedCornerShape(24.dp))
            .border(1.5.dp, Brush.linearGradient(BadgeTier.SPECIAL.colors()), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BadgeMedal(status, 56)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status.badge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                    GlassChip(text = "Event", highlighted = true)
                }
                Text(status.badge.description, style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
                Text(
                    when {
                        status.isEarned -> "Earned. It's yours to keep."
                        now < starts -> "Starts in ${days(starts - now)} days"
                        else -> "Ends in ${days(ends - now)} days · ${status.current.coerceAtMost(status.badge.goal)} / ${status.badge.goal}"
                    },
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = palette.accent
                )
            }
        }
        if (!status.isEarned && now >= starts) ProgressBar(status.progress, BadgeTier.SPECIAL.colors())
    }
}

@Composable
private fun ProgressBar(fraction: Float, colors: List<Color>) {
    val palette = MaterialTheme.glass
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.content.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(colors))
        )
    }
}

@Composable
private fun BadgeDetail(status: BadgeStatus, unitSystem: UnitSystem, now: Long, onDismiss: () -> Unit) {
    val palette = MaterialTheme.glass
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
                .glassSurface(palette, RoundedCornerShape(28.dp))
                .clickable(onClick = onDismiss)
                .padding(24.dp)
        ) {
            BadgeMedal(status, 112)
            Text(status.badge.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = palette.content, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(text = status.badge.tier.label)
                GlassChip(text = status.badge.category.title)
            }
            Text(status.badge.description, style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted, textAlign = TextAlign.Center)
            if (status.isEarned) {
                Text(
                    status.earnedAtMillis?.let { "Earned ${DateFormat.getDateInstance(DateFormat.LONG).format(Date(it))}" } ?: "Earned",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = status.badge.tier.colors().last()
                )
            } else if (status.badge.goal > 1 && status.badge.format == ProgressFormat.COUNT) {
                ProgressBar(status.progress, status.badge.tier.colors())
                Text("${status.current} / ${status.badge.goal}", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
            } else if (status.badge.format == ProgressFormat.WEIGHT_KG && status.current > 0) {
                ProgressBar(status.progress, status.badge.tier.colors())
                Text("Your best: ${formatStoredWeight(progressToKg(status.current), unitSystem)}", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
            }
        }
    }
}
