package com.FreeRave.shredzilla.screens.stats

import androidx.compose.material.icons.filled.Timer
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.utils.formatTrainingTime
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.composables.ProgressChartCanvas
import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.pro.LocalProAccess
import com.FreeRave.shredzilla.pro.ProCrown
import com.FreeRave.shredzilla.pro.ProGold
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassStatTile
import com.FreeRave.shredzilla.ui.glass.GlassTopBar
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.RECOMMENDED_WEEKLY_SETS
import com.FreeRave.shredzilla.utils.StrengthTrend
import com.FreeRave.shredzilla.utils.formatStoredWeight
import com.FreeRave.shredzilla.utils.formatWeightNumber
import com.FreeRave.shredzilla.utils.kgToDisplay
import com.FreeRave.shredzilla.utils.muscleWeeks
import com.FreeRave.shredzilla.utils.strengthTrends
import com.FreeRave.shredzilla.utils.weeklyTotals
import com.FreeRave.shredzilla.utils.weightUnit
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pro stats over the loaded 90 days: hard sets per muscle against the usual 10–20 a week,
 * estimated 1RM per exercise and an 8-week view of sets and volume. Without Pro the real
 * numbers are shown blurred behind the unlock card.
 */
@Composable
fun StatsScreen(
    history: Map<String, List<LoggedSet>>,
    unitSystem: UnitSystem,
    exerciseName: (exerciseId: String) -> String,
    onBack: () -> Unit,
    workouts: List<WorkoutLog> = emptyList(),
) {
    val pro = LocalProAccess.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Training stats", eyebrow = "Pro", onBack = onBack)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (pro.isPro) Modifier.verticalScroll(rememberScrollState()) else Modifier.blur(14.dp))
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 32.dp)
            ) {
                StatsContent(history, workouts, unitSystem, exerciseName)
            }
        }
        if (!pro.isPro) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
                    .glassSurface(MaterialTheme.glass.copy(fill = MaterialTheme.glass.backgroundBottom.copy(alpha = 0.85f)))
                    .padding(24.dp)
            ) {
                ProCrown(size = 64.dp)
                Text("Your stats are ready", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.glass.content)
                Text(
                    "See sets per muscle, your strength trend and weekly volume with Pro. One ad unlocks an hour.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.glass.contentMuted,
                    textAlign = TextAlign.Center
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GlassDefaults.ButtonHeight)
                        .clip(GlassDefaults.ControlShape)
                        .background(Brush.horizontalGradient(ProGold))
                        .clickable(onClick = pro.openPro)
                ) {
                    Text("Unlock with Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2A1A00))
                }
            }
        }
    }
}

@Composable
private fun StatsContent(history: Map<String, List<LoggedSet>>, workouts: List<WorkoutLog>, unitSystem: UnitSystem, exerciseName: (String) -> String) {
    val muscles = remember(history) { muscleWeeks(history) }
    val weeks = remember(history, workouts) { weeklyTotals(history, workouts) }
    val trends = remember(history) { strengthTrends(history) }
    val thisWeek = weeks.last()
    val lastWeek = weeks.getOrNull(weeks.size - 2)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassStatTile(Icons.Filled.FitnessCenter, "Sets this week" + delta(thisWeek.sets, lastWeek?.sets), thisWeek.sets.toString(), Modifier.weight(1f))
        GlassStatTile(Icons.Filled.Timer, "Time this week", formatTrainingTime(thisWeek.trainingMillis), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassStatTile(Icons.Filled.CalendarMonth, "Training days", thisWeek.trainingDays.toString(), Modifier.weight(1f))
        GlassStatTile(
            Icons.Filled.Scale,
            "Volume (${unitSystem.weightUnit()})",
            if (thisWeek.volumeKg > 0) compact(kgToDisplay(thisWeek.volumeKg, unitSystem)) else "—",
            Modifier.weight(1f)
        )
    }

    MuscleCard(muscles)
    StrengthCard(trends, unitSystem, exerciseName)
    WeeksCard(weeks, unitSystem)
}

private fun delta(now: Int, before: Int?): String {
    if (before == null || now == before) return ""
    return if (now > before) " ▲${now - before}" else " ▼${before - now}"
}

private fun compact(value: Double): String =
    if (value >= 10_000) "${formatWeightNumber(value / 1000)}k" else value.roundToInt().toString()

@Composable
private fun MuscleCard(muscles: List<com.FreeRave.shredzilla.utils.MuscleWeek>) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Sets per muscle this week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
        Text(
            "Most guidelines aim for ${RECOMMENDED_WEEKLY_SETS.first}–${RECOMMENDED_WEEKLY_SETS.last} hard sets per muscle a week. The shaded band marks that range; the tick is last week.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.contentMuted
        )
        if (muscles.isEmpty()) {
            Text("Log a few sets this week to see the split.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
        }
        val scaleMax = maxOf(24, muscles.maxOfOrNull { maxOf(it.thisWeek, it.lastWeek) } ?: 0).toFloat()
        muscles.forEach { m ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.muscle, style = MaterialTheme.typography.labelLarge, color = palette.content, modifier = Modifier.weight(1f))
                    val status = when {
                        m.thisWeek >= RECOMMENDED_WEEKLY_SETS.last + 1 -> "High"
                        m.thisWeek >= RECOMMENDED_WEEKLY_SETS.first -> "On target"
                        else -> "${RECOMMENDED_WEEKLY_SETS.first - m.thisWeek} to go"
                    }
                    Text("${m.thisWeek} sets · $status", style = MaterialTheme.typography.labelMedium, color = palette.contentMuted)
                }
                val fill by animateFloatAsState(m.thisWeek / scaleMax, tween(800), label = "muscle${m.muscle}")
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.content.copy(alpha = 0.07f))
                ) {
                    val w = maxWidth
                    // Recommended band
                    Box(
                        Modifier
                            .offset(x = w * (RECOMMENDED_WEEKLY_SETS.first / scaleMax))
                            .width(w * ((RECOMMENDED_WEEKLY_SETS.last - RECOMMENDED_WEEKLY_SETS.first) / scaleMax))
                            .fillMaxHeight()
                            .background(palette.accent.copy(alpha = 0.14f))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(fill.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(palette.accentGradient))
                    )
                    if (m.lastWeek > 0) {
                        Box(
                            Modifier
                                .offset(x = w * (m.lastWeek / scaleMax).coerceAtMost(1f) - 1.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(palette.content.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthCard(trends: List<StrengthTrend>, unitSystem: UnitSystem, exerciseName: (String) -> String) {
    val palette = MaterialTheme.glass
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(trends) { if (trends.none { it.exerciseId == selectedId }) selectedId = trends.firstOrNull()?.exerciseId }
    val trend = trends.find { it.exerciseId == selectedId } ?: trends.firstOrNull()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text("Strength trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
        if (trend == null) {
            Text("Train an exercise with weight on two different days to see its estimated 1-rep max.", style = MaterialTheme.typography.bodyMedium, color = palette.contentMuted)
            return@GlassCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            trends.take(12).forEach { t ->
                GlassFilterChip(text = exerciseName(t.exerciseId), selected = t.exerciseId == trend.exerciseId, onClick = { selectedId = t.exerciseId })
            }
        }
        fun label(v: Double) = if (trend.isPlates) "${formatWeightNumber(v)} plates" else formatStoredWeight(v, unitSystem)
        val change = trend.latest - trend.first
        val percent = if (trend.first > 0) (change / trend.first * 100).roundToInt() else 0
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Estimated 1RM", style = MaterialTheme.typography.labelMedium, color = palette.contentMuted)
                Text(label(trend.latest), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = palette.content)
            }
            Text(
                when {
                    abs(change) < 0.05 -> "Holding steady"
                    change > 0 -> "▲ ${label(change)} (+$percent%)"
                    else -> "▼ ${label(-change)} ($percent%)"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (change > 0) palette.accent else palette.contentMuted
            )
        }
        ProgressChartCanvas(
            dataPoints = trend.points.map { MainViewModel.ChartDataPoint(it.dayMillis, it.estimatedMax) },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
        val fmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
        Row {
            Text(fmt.format(Date(trend.points.first().dayMillis)), style = MaterialTheme.typography.labelSmall, color = palette.contentMuted, modifier = Modifier.weight(1f))
            Text("Best ${label(trend.best)}", style = MaterialTheme.typography.labelSmall, color = palette.contentMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(fmt.format(Date(trend.points.last().dayMillis)), style = MaterialTheme.typography.labelSmall, color = palette.contentMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Text(
            "Estimated from your best set each day (load × (1 + reps ÷ 30)). A guide to your trend, not a max to test.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.contentMuted
        )
    }
}

private enum class WeekMetric(val title: String) { SETS("Sets"), TIME("Time"), VOLUME("Volume") }

@Composable
private fun WeeksCard(weeks: List<com.FreeRave.shredzilla.utils.WeekTotals>, unitSystem: UnitSystem) {
    val palette = MaterialTheme.glass
    var metric by rememberSaveable { mutableStateOf(WeekMetric.SETS) }
    val values = weeks.map {
        when (metric) {
            WeekMetric.SETS -> it.sets.toDouble()
            WeekMetric.TIME -> it.trainingMillis / 60_000.0
            WeekMetric.VOLUME -> kgToDisplay(it.volumeKg, unitSystem)
        }
    }
    fun valueLabel(v: Double) = if (metric == WeekMetric.TIME) formatTrainingTime((v * 60_000).toLong()) else compact(v)
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val fmt = remember { SimpleDateFormat("d/M", Locale.getDefault()) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last 8 weeks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content, modifier = Modifier.weight(1f))
            WeekMetric.entries.forEachIndexed { i, m ->
                if (i > 0) Spacer(Modifier.width(6.dp))
                GlassFilterChip(text = m.title, selected = metric == m, onClick = { metric = m })
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().height(150.dp)
        ) {
            weeks.forEachIndexed { i, week ->
                val current = i == weeks.lastIndex
                val target = (values[i] / max).toFloat()
                val h by animateFloatAsState(target, tween(700, delayMillis = i * 40), label = "week$i")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                    Text(
                        if (values[i] > 0) valueLabel(values[i]) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (current) palette.content else palette.contentMuted,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(h.coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(
                                    if (current) Brush.verticalGradient(palette.accentGradient)
                                    else Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.45f), palette.accent.copy(alpha = 0.2f)))
                                )
                        )
                    }
                }
            }
        }
        Row {
            weeks.forEachIndexed { i, week ->
                Text(
                    if (i == weeks.lastIndex) "Now" else fmt.format(Date(week.weekStartMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.contentMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
