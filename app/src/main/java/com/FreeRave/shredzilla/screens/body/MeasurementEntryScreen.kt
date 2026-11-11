package com.FreeRave.shredzilla.screens.body

import com.FreeRave.shredzilla.models.MeasurementCadence
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.models.BodyMeasurement
import com.FreeRave.shredzilla.models.MeasurementSite
import com.FreeRave.shredzilla.models.MeasurementSlot
import com.FreeRave.shredzilla.screens.settings.UnitSystem
import com.FreeRave.shredzilla.screens.settings.LengthUnit
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassDialog
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSettingsGroup
import com.FreeRave.shredzilla.ui.glass.GlassSettingsRow
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.glass.glassTextFieldColors
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.cmToDisplay
import com.FreeRave.shredzilla.utils.displayToCm
import com.FreeRave.shredzilla.utils.displayToKg
import com.FreeRave.shredzilla.utils.formatWeightNumber
import com.FreeRave.shredzilla.utils.kgToDisplay
import com.FreeRave.shredzilla.utils.parseDecimalInput
import com.FreeRave.shredzilla.utils.weightUnit
import java.util.Calendar
import java.util.TimeZone

private const val BODY_WEIGHT_KEY = "body_weight"

/** The date picker works in UTC midnights; entries are stored at local noon so no timezone moves the day. */
private fun localNoonToUtcMidnight(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcMidnightToLocalNoon(millis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0)
    }.timeInMillis
}

private fun todayAtNoon(): Long = utcMidnightToLocalNoon(localNoonToUtcMidnight(System.currentTimeMillis()))

/**
 * New or edited measuring session. Every field is optional; each one shows the last value so the
 * tape measure numbers are easy to compare while typing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementEntryScreen(
    existing: BodyMeasurement?,
    previous: BodyMeasurement?,
    unitSystem: UnitSystem,
    lengthUnit: LengthUnit,
    cadence: MeasurementCadence,
    onSave: (BodyMeasurement) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    val palette = MaterialTheme.glass
    val unit = lengthUnit.symbol
    var takenAt by rememberSaveable { mutableLongStateOf(existing?.takenAtMillis ?: todayAtNoon()) }
    var texts by rememberSaveable {
        mutableStateOf(
            buildMap {
                existing?.valuesCm?.forEach { (key, cm) -> put(key, formatWeightNumber(cmToDisplay(cm, lengthUnit))) }
                existing?.bodyWeightKg?.let { put(BODY_WEIGHT_KEY, formatWeightNumber(kgToDisplay(it, unitSystem))) }
            }
        )
    }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val values = MeasurementSite.entries.flatMap { it.slots }
        .mapNotNull { slot -> texts[slot.key]?.let(::parseDecimalInput)?.let { slot.key to displayToCm(it, lengthUnit) } }
        .toMap()
    val bodyWeightKg = texts[BODY_WEIGHT_KEY]?.let(::parseDecimalInput)?.let { displayToKg(it, unitSystem) }
    val canSave = values.isNotEmpty() || bodyWeightKg != null

    fun lastLabel(cm: Double?) = cm?.let { " · last ${formatWeightNumber(cmToDisplay(it, lengthUnit))}" }.orEmpty()

    @Composable
    fun NumberField(key: String, label: String, modifier: Modifier = Modifier) {
        TextFormField(
            value = texts[key].orEmpty(),
            onValueChange = { typed -> texts = texts + (key to typed) },
            label = label,
            keyboardType = KeyboardType.Decimal,
            modifier = modifier
        )
    }

    GlassSubpageScaffold(
        title = if (existing == null) "New measurement" else "Edit measurement",
        eyebrow = "Body",
        onBack = onBack,
        bottomBar = {
            GlassPrimaryButton(
                text = if (canSave) "Save" else "Enter at least one value",
                enabled = canSave,
                onClick = {
                    onSave(BodyMeasurement(existing?.id.orEmpty(), takenAt, values, bodyWeightKg, note.trim()))
                }
            )
        }
    ) {
        // A gentle note, not a block: measuring too often mostly records water and food
        val daysSincePrevious = previous?.let { TimeUnit.MILLISECONDS.toDays(takenAt - it.takenAtMillis).toInt() }
        if (existing == null && daysSincePrevious != null && daysSincePrevious < cadence.days - cadence.days / 7) {
            GlassFootnote(
                "Your last measurement was ${if (daysSincePrevious <= 0) "today" else "$daysSincePrevious days ago"}. " +
                    "You planned to measure ${cadence.label.lowercase()}; numbers taken closer together mostly show water and food, not progress."
            )
        }
        GlassSettingsGroup {
            GlassSettingsRow(
                icon = Icons.Filled.CalendarMonth,
                title = "Date",
                value = measurementDateLabel(takenAt),
                onClick = { showDatePicker = true }
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = palette.accent)
                Text("Body weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
            }
            NumberField(
                BODY_WEIGHT_KEY,
                unitSystem.weightUnit() + previous?.bodyWeightKg?.let { " · last ${formatWeightNumber(kgToDisplay(it, unitSystem))}" }.orEmpty()
            )
        }

        MeasurementSite.entries.forEach { site ->
            GlassCard(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${site.label} ($unit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.content)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    site.slots.forEach { slot: MeasurementSlot ->
                        NumberField(
                            slot.key,
                            (slot.side?.label ?: site.label) + lastLabel(previous?.valueOf(slot)),
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note, e.g. after deload week") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = glassTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (onDelete != null) {
            GlassSettingsGroup {
                GlassSettingsRow(
                    icon = Icons.Filled.DeleteOutline,
                    title = "Delete this measurement",
                    onClick = { confirmDelete = true },
                    tint = palette.error,
                    showChevron = false
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localNoonToUtcMidnight(takenAt),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { takenAt = utcMidnightToLocalNoon(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete && onDelete != null) {
        GlassDialog(
            title = "Delete this measurement?",
            message = "Measurements from ${measurementDateLabel(takenAt)} are removed. This can't be undone.",
            confirmText = "Delete",
            destructive = true,
            icon = rememberVectorPainter(Icons.Filled.DeleteOutline),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
