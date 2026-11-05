package com.FreeRave.shredzilla.screens.exercises.composables

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
import com.FreeRave.shredzilla.utils.weightUnit
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val MIN_REPS = 1
private const val MIN_PLATES = 1
private val WeightQuickSteps = listOf(-5.0, -2.5, 2.5, 5.0)

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
    var weight by remember(initial) { mutableDoubleStateOf(initial.weight) }
    var plates by remember(initial) { mutableIntStateOf(initial.plates ?: MIN_PLATES) }
    var usePlates by remember(initial) { mutableStateOf(initial.plates != null) }
    var notes by remember(initial) { mutableStateOf(initial.notes) }
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
                applied = reps == next.reps && usePlates == (next.plates != null) &&
                    (if (usePlates) plates == next.plates else weight == next.weight),
                onUse = {
                    reps = next.reps
                    usePlates = next.plates != null
                    next.plates?.let { plates = it } ?: run { weight = next.weight }
                }
            )
        }

        StepperCard(
            label = "Reps",
            valueText = reps.toString(),
            onMinus = { reps = (reps - 1).coerceAtLeast(MIN_REPS) },
            onPlus = { reps++ }
        )
        StepperCard(
            label = if (usePlates) "Plates" else "Weight",
            valueText = if (usePlates) platesLabel(plates) else formatWeight(weight, unitSystem),
            onMinus = { if (usePlates) plates = (plates - 1).coerceAtLeast(MIN_PLATES) else weight = (weight - 1).coerceAtLeast(0.0) },
            onPlus = { if (usePlates) plates++ else weight += 1 },
            headerTrailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassFilterChip(text = unitSystem.weightUnit(), selected = !usePlates, onClick = { usePlates = false })
                    GlassFilterChip(text = "Plates", selected = usePlates, onClick = { usePlates = true })
                }
            }
        ) {
            if (!usePlates) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    WeightQuickSteps.forEach { step ->
                        GlassFilterChip(
                            text = "${if (step > 0) "+" else "−"}${formatWeightNumber(abs(step))}",
                            selected = false,
                            onClick = { weight = (weight + step).coerceAtLeast(0.0) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                PlateCalculator(weight = weight, unitSystem = unitSystem)
            }
        }

        TextFormField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes (optional)",
            imeAction = ImeAction.Done,
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
        )

        GlassPrimaryButton(
            text = if (isEditing) "Update set" else "Record set",
            onClick = { onRecordSet(SetEntry(reps, if (usePlates) 0.0 else weight, plates.takeIf { usePlates }, notes)) }
        )
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel", color = palette.contentMuted)
        }
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
