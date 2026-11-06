package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.models.SetEntry
import com.FreeRave.shredzilla.models.SetType
import com.FreeRave.shredzilla.models.Side
import com.FreeRave.shredzilla.models.Tempo
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassBottomSheet
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatWeight
import com.FreeRave.shredzilla.utils.formatWeightNumber
import com.FreeRave.shredzilla.utils.SetSuggestion
import com.FreeRave.shredzilla.utils.formatEntry
import com.FreeRave.shredzilla.utils.platesLabel
import com.FreeRave.shredzilla.utils.bodyweightLabel
import com.FreeRave.shredzilla.utils.weightUnit
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val MIN_REPS = 1

/** How a set's load is counted: weight in the user's unit, plates on a stack, or bodyweight (plus any added weight). */
private enum class LoadMode { WEIGHT, PLATES, BODYWEIGHT }

private val SetEntry.loadMode: LoadMode
    get() = when {
        bodyweight -> LoadMode.BODYWEIGHT
        plates != null -> LoadMode.PLATES
        else -> LoadMode.WEIGHT
    }
private const val MIN_PLATES = 1
private val WeightQuickSteps = listOf(-5.0, -2.5, 2.5, 5.0)
private val EccentricOptions = listOf(null, 3, 5, 8)
private val PauseOptions = listOf(null, 1, 2, 3)

/**
 * Body of the "record a set" sheet, shared by the library, list detail and exercise detail.
 * Reps and load use steppers so logging mid-workout needs no keyboard. The load is either a
 * weight in the user's unit or, for weight-stack machines, a plate count.
 */
@Composable
fun RecordSetSheetContent(
    exerciseName: String,
    onRecordSet: (SetEntry) -> Unit,
    onCancel: () -> Unit,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    initial: SetEntry = SetEntry(),
    suggestion: SetSuggestion? = null,
    isEditing: Boolean = false
) {
    var reps by remember(initial) { mutableIntStateOf(initial.reps.coerceAtLeast(MIN_REPS)) }
    // Bar weight and added bodyweight load are kept apart, so switching modes never turns 60 kg into "BW + 60"
    var weight by remember(initial) { mutableDoubleStateOf(if (initial.bodyweight) 0.0 else initial.weight) }
    var addedWeight by remember(initial) { mutableDoubleStateOf(if (initial.bodyweight) initial.weight else 0.0) }
    var plates by remember(initial) { mutableIntStateOf(initial.plates ?: MIN_PLATES) }
    var mode by remember(initial) { mutableStateOf(initial.loadMode) }
    var notes by remember(initial) { mutableStateOf(initial.notes) }
    var side by remember(initial) { mutableStateOf(initial.side) }
    var setType by remember(initial) { mutableStateOf(initial.setType) }
    var eccentric by remember(initial) { mutableStateOf(initial.tempo.eccentricSeconds) }
    var pause by remember(initial) { mutableStateOf(initial.tempo.pauseSeconds) }
    // Plate-loaded on both sides: [weight] stays the total, the user steps and reads one side
    var perSide by remember(initial) { mutableStateOf(initial.perSide) }
    val sideFactor = if (perSide) 2 else 1
    val palette = MaterialTheme.glass

    // Scrolls once the plate calculator is open on a short screen
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                if (isEditing) "Edit set" else "Log a set",
                style = MaterialTheme.typography.labelLarge,
                color = palette.contentMuted
            )
            Text(exerciseName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.content)
        }

        if (suggestion != null && !isEditing) {
            val next = suggestion.next
            SuggestionCard(
                suggestion = suggestion,
                unitSystem = unitSystem,
                applied = reps == next.reps && mode == next.loadMode && when (mode) {
                    LoadMode.WEIGHT -> weight == next.weight && perSide == next.perSide
                    LoadMode.PLATES -> plates == next.plates
                    LoadMode.BODYWEIGHT -> addedWeight == next.weight
                },
                onUse = {
                    reps = next.reps
                    mode = next.loadMode
                    when (mode) {
                        LoadMode.WEIGHT -> {
                            weight = next.weight
                            perSide = next.perSide
                        }
                        LoadMode.PLATES -> plates = next.plates ?: MIN_PLATES
                        LoadMode.BODYWEIGHT -> addedWeight = next.weight
                    }
                }
            )
        }

        StepperCard(
            label = "Reps",
            valueText = reps.toString(),
            onMinus = { reps = (reps - 1).coerceAtLeast(MIN_REPS) },
            onPlus = { reps++ },
            // One leg / arm at a time: the next set starts on the other side by itself
            headerTrailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassFilterChip(text = "Both", selected = side == null, onClick = { side = null })
                    Side.entries.forEach { option ->
                        GlassFilterChip(text = option.label, selected = side == option, onClick = { side = option })
                    }
                }
            }
        )
        StepperCard(
            label = "Load",
            valueText = when (mode) {
                LoadMode.WEIGHT -> if (perSide) "${formatWeight(weight / 2, unitSystem)}/side" else formatWeight(weight, unitSystem)
                LoadMode.PLATES -> platesLabel(plates)
                LoadMode.BODYWEIGHT -> bodyweightLabel(addedWeight, unitSystem, stored = false)
            },
            onMinus = {
                when (mode) {
                    LoadMode.WEIGHT -> weight = (weight - sideFactor).coerceAtLeast(0.0)
                    LoadMode.PLATES -> plates = (plates - 1).coerceAtLeast(MIN_PLATES)
                    LoadMode.BODYWEIGHT -> addedWeight = (addedWeight - 1).coerceAtLeast(0.0)
                }
            },
            onPlus = {
                when (mode) {
                    LoadMode.WEIGHT -> weight += sideFactor
                    LoadMode.PLATES -> plates++
                    LoadMode.BODYWEIGHT -> addedWeight += 1
                }
            },
            headerTrailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassFilterChip(text = unitSystem.weightUnit(), selected = mode == LoadMode.WEIGHT, onClick = { mode = LoadMode.WEIGHT })
                    GlassFilterChip(text = "Plates", selected = mode == LoadMode.PLATES, onClick = { mode = LoadMode.PLATES })
                    GlassFilterChip(text = "Bodyweight", selected = mode == LoadMode.BODYWEIGHT, onClick = { mode = LoadMode.BODYWEIGHT })
                }
            }
        ) {
            if (mode == LoadMode.BODYWEIGHT) {
                Text(
                    "+ / − adds weight on top, e.g. a plate held during back extensions",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.contentMuted
                )
            }
            if (mode != LoadMode.PLATES) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    WeightQuickSteps.forEach { step ->
                        GlassFilterChip(
                            text = "${if (step > 0) "+" else "−"}${formatWeightNumber(abs(step))}",
                            selected = false,
                            onClick = {
                                if (mode == LoadMode.BODYWEIGHT) addedWeight = (addedWeight + step).coerceAtLeast(0.0)
                                else weight = (weight + step * sideFactor).coerceAtLeast(0.0)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (mode == LoadMode.WEIGHT) {
                    GlassFilterChip(
                        text = if (perSide) "Per side · total ${formatWeight(weight, unitSystem)}" else "Loaded per side?",
                        selected = perSide,
                        // Keeps the number on screen: 30 becomes 30 per side, not 15
                        onClick = {
                            weight = if (perSide) weight / 2 else weight * 2
                            perSide = !perSide
                        }
                    )
                    if (!perSide) PlateCalculator(weight = weight, unitSystem = unitSystem)
                }
            }
        }

        SetDetailsCard(
            setType = setType,
            onSetType = { setType = it },
            eccentric = eccentric,
            onEccentric = { eccentric = it },
            pause = pause,
            onPause = { pause = it }
        )

        TextFormField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes (optional)",
            imeAction = ImeAction.Done,
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
        )

        GlassPrimaryButton(
            text = if (isEditing) "Update set" else "Record set",
            onClick = {
                onRecordSet(
                    SetEntry(
                        reps = reps,
                        weight = when (mode) {
                            LoadMode.WEIGHT -> weight
                            LoadMode.PLATES -> 0.0
                            LoadMode.BODYWEIGHT -> addedWeight
                        },
                        plates = plates.takeIf { mode == LoadMode.PLATES },
                        notes = notes,
                        side = side,
                        bodyweight = mode == LoadMode.BODYWEIGHT,
                        setType = setType,
                        tempo = Tempo(eccentric, pause),
                        perSide = perSide && mode == LoadMode.WEIGHT
                    )
                )
            }
        )
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel", color = palette.contentMuted)
        }
    }
}

/** Set type (warm-up, rest-pause…) and tempo; the next set starts from the same tempo. */
@Composable
private fun SetDetailsCard(
    setType: SetType,
    onSetType: (SetType) -> Unit,
    eccentric: Int?,
    onEccentric: (Int?) -> Unit,
    pause: Int?,
    onPause: (Int?) -> Unit
) {
    val palette = MaterialTheme.glass
    @Composable
    fun Caption(text: String) =
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = palette.contentMuted)

    @Composable
    fun SecondsRow(label: String, options: List<Int?>, selected: Int?, onSelect: (Int?) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = palette.content, modifier = Modifier.width(48.dp))
            options.forEach { option ->
                GlassFilterChip(text = option?.let { "${it}s" } ?: "Off", selected = selected == option, onClick = { onSelect(option) })
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Caption("Set type")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SetType.entries.forEach { type -> GlassFilterChip(text = type.label, selected = type == setType, onClick = { onSetType(type) }) }
        }
        if (setType == SetType.WARMUP) {
            Text("Warm-ups don't count towards sets, volume or records", style = MaterialTheme.typography.bodySmall, color = palette.contentMuted)
        }
        Caption("Tempo")
        SecondsRow("Down", EccentricOptions, eccentric, onEccentric)
        SecondsRow("Hold", PauseOptions, pause, onPause)
    }
}

@Composable
private fun SuggestionCard(suggestion: SetSuggestion, unitSystem: UnitSystem, applied: Boolean, onUse: () -> Unit) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = palette.accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Try ${formatEntry(suggestion.next, unitSystem)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.content
                )
                Text(
                    "Last session ${formatEntry(suggestion.basedOn, unitSystem)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.contentMuted
                )
            }
            GlassFilterChip(text = if (applied) "Set" else "Use", selected = applied, onClick = onUse)
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    headerTrailing: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null
) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.contentMuted,
                modifier = Modifier.weight(1f)
            )
            headerTrailing?.invoke()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Filled.Remove, contentDescription = "Less $label", onClick = onMinus)
            AnimatedContent(
                targetState = valueText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "${label}Value",
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) { shown ->
                Text(shown, fontSize = 34.sp, fontWeight = FontWeight.Black, color = palette.content)
            }
            GlassIconButton(Icons.Filled.Add, contentDescription = "More $label", onClick = onPlus)
        }
        extra?.invoke()
    }
}

/**
 * The full "log / edit a set" sheet, shown while [exerciseName] is non-null. Handles the slide-out
 * animation itself and calls [onDismiss] once it's gone, so screens only track which exercise is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSetSheet(
    exerciseName: String?,
    unitSystem: UnitSystem,
    onRecordSet: (exerciseName: String, entry: SetEntry) -> Unit,
    onDismiss: () -> Unit,
    initial: SetEntry = SetEntry(),
    suggestion: SetSuggestion? = null,
    isEditing: Boolean = false
) {
    val name = exerciseName ?: return
    // Taken once per opening: history refreshing underneath must not reset what's being typed
    val startEntry = remember { initial }
    val startSuggestion = remember { suggestion }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    GlassBottomSheet(onDismiss = onDismiss, sheetState = sheetState) {
        RecordSetSheetContent(
            exerciseName = name,
            unitSystem = unitSystem,
            initial = startEntry,
            suggestion = startSuggestion,
            isEditing = isEditing,
            onRecordSet = { entry ->
                onRecordSet(name, entry)
                close()
            },
            onCancel = ::close
        )
    }
}
