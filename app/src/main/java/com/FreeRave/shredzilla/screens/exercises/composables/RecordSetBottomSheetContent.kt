package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.ui.glass.GlassBottomSheet
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassFilterChip
import com.FreeRave.shredzilla.ui.glass.GlassIconButton
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.formatWeight
import com.FreeRave.shredzilla.utils.formatWeightNumber
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val MIN_REPS = 1
private val WeightQuickSteps = listOf(-5.0, -2.5, 2.5, 5.0)

/**
 * Body of the "record a set" sheet, shared by the library, list detail and exercise detail.
 * Reps and weight use steppers so logging mid-workout needs no keyboard.
 */
@Composable
fun RecordSetSheetContent(
    exerciseName: String,
    onRecordSet: (reps: Int, weight: Double, notes: String) -> Unit,
    onCancel: () -> Unit,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    initialReps: Int = MIN_REPS,
    initialWeight: Double = 0.0,
    initialNotes: String = "",
    isEditing: Boolean = false
) {
    var reps by remember(initialReps) { mutableIntStateOf(initialReps) }
    var weight by remember(initialWeight) { mutableDoubleStateOf(initialWeight) }
    var notes by remember(initialNotes) { mutableStateOf(initialNotes) }
    val palette = MaterialTheme.glass

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                if (isEditing) "Edit set" else "Log a set",
                style = MaterialTheme.typography.labelLarge,
                color = palette.contentMuted
            )
            Text(exerciseName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.content)
        }

        StepperCard(
            label = "Reps",
            valueText = reps.toString(),
            onMinus = { reps = (reps - 1).coerceAtLeast(MIN_REPS) },
            onPlus = { reps++ }
        )
        StepperCard(
            label = "Weight",
            valueText = formatWeight(weight, unitSystem),
            onMinus = { weight = (weight - 1).coerceAtLeast(0.0) },
            onPlus = { weight += 1 }
        ) {
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
        }

        TextFormField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes (optional)",
            imeAction = ImeAction.Done,
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
        )

        GlassPrimaryButton(text = if (isEditing) "Update set" else "Record set", onClick = { onRecordSet(reps, weight, notes) })
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel", color = palette.contentMuted)
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val palette = MaterialTheme.glass
    GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = palette.contentMuted)
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
    onRecordSet: (exerciseName: String, reps: Int, weight: Double, notes: String) -> Unit,
    onDismiss: () -> Unit,
    initialReps: Int = MIN_REPS,
    initialWeight: Double = 0.0,
    initialNotes: String = "",
    isEditing: Boolean = false
) {
    val name = exerciseName ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    GlassBottomSheet(onDismiss = onDismiss, sheetState = sheetState) {
        RecordSetSheetContent(
            exerciseName = name,
            unitSystem = unitSystem,
            initialReps = initialReps,
            initialWeight = initialWeight,
            initialNotes = initialNotes,
            isEditing = isEditing,
            onRecordSet = { reps, weight, notes ->
                onRecordSet(name, reps, weight, notes)
                close()
            },
            onCancel = ::close
        )
    }
}
