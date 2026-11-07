package com.FreeRave.shredzilla.screens.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.FreeRave.shredzilla.pro.LocalProAccess
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.theme.GlassPalette
import com.FreeRave.shredzilla.ui.theme.LocalGlassPalette
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.models.WorkoutLog
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.glassSurface
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatDuration
import com.FreeRave.shredzilla.utils.formatLoad
import com.FreeRave.shredzilla.utils.formatStoredWeight
import com.FreeRave.shredzilla.utils.shareImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

fun workoutDateLabel(millis: Long): String = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(millis))

/**
 * A finished workout: the share card (exactly what gets shared as an image), a Share button and,
 * when [onDone] is given (right after a workout), a Done button.
 */
@Composable
fun WorkoutLogContent(log: WorkoutLog, unitSystem: UnitSystem, modifier: Modifier = Modifier, onDone: (() -> Unit)? = null) {
    val palette = MaterialTheme.glass
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardLayer = rememberGraphicsLayer()
    val pro = LocalProAccess.current
    var style by rememberSaveable { mutableStateOf(ShareCardStyle.GLASS) }
    // Pro ran out while a Pro style was picked: fall back to the free one
    val shownStyle = if (pro.isPro) style else ShareCardStyle.GLASS
    val cardPalette = shownStyle.paletteFrom(palette)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        WorkoutShareCard(
            log = log,
            unitSystem = unitSystem,
            style = shownStyle,
            // Records the card into a layer as it draws, so sharing can snapshot it
            modifier = Modifier.drawWithContent {
                cardLayer.record { this@drawWithContent.drawContent() }
                drawLayer(cardLayer)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ShareCardStyle.entries.forEach { option ->
                GlassFilterChip(
                    text = option.title + if (option.isPro && !pro.isPro) " 🔒" else "",
                    selected = shownStyle == option,
                    onClick = { if (option.isPro && !pro.isPro) pro.openPro() else style = option }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GlassDefaults.ButtonHeight)
                .glassSurface(palette, GlassDefaults.ControlShape)
                .clickable(role = Role.Button) {
                    scope.launch {
                        shareImage(context, cardLayer.toImageBitmap().asAndroidBitmap(), cardPalette.backgroundBottom.toArgb(), "Share workout")
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, tint = palette.content)
            Spacer(Modifier.width(10.dp))
            Text("Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.content)
        }
        if (onDone != null) GlassPrimaryButton(text = "Done", onClick = onDone)
    }
}

/** Opaque, self-contained card that still looks right once it's an image in someone's chat. */
@Composable
fun WorkoutShareCard(log: WorkoutLog, unitSystem: UnitSystem, modifier: Modifier = Modifier, style: ShareCardStyle = ShareCardStyle.GLASS) {
    val cardPalette = style.paletteFrom(MaterialTheme.glass)
    CompositionLocalProvider(LocalGlassPalette provides cardPalette) {
        ShareCardBody(log, unitSystem, style, modifier)
    }
}

/** Looks for the share card. Glass follows the app; Midnight and Accent are Pro. */
enum class ShareCardStyle(val title: String, val isPro: Boolean) {
    GLASS("Glass", false),
    MIDNIGHT("Midnight", true),
    ACCENT("Accent", true);

    fun paletteFrom(app: GlassPalette): GlassPalette = when (this) {
        GLASS -> app
        MIDNIGHT -> app.copy(
            isDark = true,
            backgroundTop = Color(0xFF15151C),
            backgroundBottom = Color(0xFF000000),
            orbPrimary = app.accent.copy(alpha = 0.6f),
            orbSecondary = Color(0xFF3A3A48),
            border = Color.White.copy(alpha = 0.18f),
            content = Color.White,
            contentMuted = Color.White.copy(alpha = 0.6f),
            accent = app.accentGradient.last().let { if (app.isDark) app.accent else it },
        )
        ACCENT -> app.copy(
            isDark = true,
            backgroundTop = app.accentGradient.first(),
            backgroundBottom = app.accentGradient.last().copy(red = app.accentGradient.last().red * 0.55f, green = app.accentGradient.last().green * 0.55f, blue = app.accentGradient.last().blue * 0.55f),
            orbPrimary = Color.White.copy(alpha = 0.45f),
            orbSecondary = Color.Black.copy(alpha = 0.35f),
            border = Color.White.copy(alpha = 0.35f),
            content = Color.White,
            contentMuted = Color.White.copy(alpha = 0.75f),
            accent = Color.White,
            accentGradient = listOf(Color.White, Color.White.copy(alpha = 0.85f)),
            onAccent = app.accentGradient.last().copy(red = app.accentGradient.last().red * 0.5f, green = app.accentGradient.last().green * 0.5f, blue = app.accentGradient.last().blue * 0.5f),
        )
    }
}

@Composable
private fun ShareCardBody(log: WorkoutLog, unitSystem: UnitSystem, style: ShareCardStyle, modifier: Modifier) {
    val palette = MaterialTheme.glass
    val shape = RoundedCornerShape(28.dp)
    val doneExercises = log.exercises.filter { it.sets > 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
            .drawBehind {
                val radius = size.width * 0.75f
                drawCircle(
                    Brush.radialGradient(listOf(palette.orbPrimary.copy(alpha = 0.55f), Color.Transparent), Offset(size.width, 0f), radius),
                    radius, Offset(size.width, 0f)
                )
                drawCircle(
                    Brush.radialGradient(listOf(palette.orbSecondary.copy(alpha = 0.4f), Color.Transparent), Offset(0f, size.height), radius),
                    radius, Offset(0f, size.height)
                )
            }
            .then(if (style == ShareCardStyle.MIDNIGHT) Modifier.border(1.5.dp, Brush.linearGradient(palette.accentGradient), shape) else Modifier.border(1.dp, palette.border.copy(alpha = 0.35f), shape))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(palette.accentGradient))
            ) {
                Icon(painterResource(R.drawable.ic_dumbbell), contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "SHREDZILLA",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = palette.content,
                modifier = Modifier.weight(1f)
            )
            Text(workoutDateLabel(log.startedAtMillis), style = MaterialTheme.typography.labelMedium, color = palette.contentMuted)
        }

        Column {
            Text("Workout complete", style = MaterialTheme.typography.labelLarge, color = palette.contentMuted)
            Text(log.listName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = palette.content)
        }

        Row {
            ShareStat("Time", formatDuration(log.durationMillis), Modifier.weight(1f))
            ShareStat("Sets", log.setCount.toString(), Modifier.weight(1f))
            ShareStat("Volume", if (log.volumeKg > 0) formatStoredWeight(log.volumeKg, unitSystem) else "—", Modifier.weight(1f))
        }

        if (log.records.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.accent.copy(alpha = 0.16f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    val count = log.records.size
                    Text(
                        "$count new personal ${if (count == 1) "record" else "records"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.content
                    )
                }
                log.records.forEach { record ->
                    ExerciseLine(record.exerciseName, formatLoad(record.load, unitSystem))
                }
            }
        }

        if (doneExercises.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.25f)))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                doneExercises.forEach { exercise ->
                    ExerciseLine(
                        exercise.name,
                        "${exercise.sets} ${if (exercise.sets == 1) "set" else "sets"}" + (exercise.best?.let { " · ${formatLoad(it, unitSystem)}" } ?: "")
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareStat(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = MaterialTheme.glass
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = palette.content, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.contentMuted)
    }
}

@Composable
private fun ExerciseLine(name: String, detail: String) {
    val palette = MaterialTheme.glass
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Text(detail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.content)
    }
}
