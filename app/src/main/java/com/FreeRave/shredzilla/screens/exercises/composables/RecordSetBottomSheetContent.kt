package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.screens.settings.UnitSystem // Import UnitSystem

@Composable
fun RecordSetSheetContent(
    exerciseName: String,
    onRecordSet: (reps: Int, weight: Double, notes: String) -> Unit,
    onCancel: () -> Unit,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier,
    initialReps: Int = 1, // Default for new set
    initialWeight: Double = 0.0, // Default for new set
    initialNotes: String = "", // Default for new set
    isEditing: Boolean = false // Flag to indicate if editing an existing set
) {
    var reps by remember(initialReps) { mutableStateOf(initialReps) }
    var weight by remember(initialWeight) { mutableStateOf(initialWeight) }
    var notes by remember(initialNotes) { mutableStateOf(initialNotes) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle (optional, ModalBottomSheet usually has one)
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "REPETITIONS & WEIGHT",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Reps Counter
        CounterRow(
            label = "rep",
            currentValue = reps,
            onIncrement = { reps++ },
            onDecrement = { if (reps > 1) reps-- },
            incrementValues = listOf(1),
            decrementValues = listOf(1)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Weight Counter
        val weightLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"
        CounterRow(
            label = weightLabel, // Dynamic label
            currentValue = weight,
            onIncrement = { value -> weight += value },
            onDecrement = { value -> if (weight - value >= 0) weight -= value else weight = 0.0 },
            incrementValues = listOf(5.0, 1.0), // Order matters for button display
            decrementValues = listOf(1.0, 5.0)  // Order matters for button display
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onRecordSet(reps, weight, notes)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isEditing) "Update Set" else "Record Set", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
         Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 16.sp)
        }
    }
}

@Composable
private fun <T : Number> CounterRow(
    label: String,
    currentValue: T,
    onIncrement: (T) -> Unit,
    onDecrement: (T) -> Unit,
    incrementValues: List<T>,
    decrementValues: List<T>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
        // horizontalArrangement = Arrangement.SpaceBetween // Replaced by weights and spacers
    ) {
        // Increment buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            incrementValues.forEachIndexed { index, value ->
                CounterButton(text = "+ $value") { onIncrement(value) }
                if (index < incrementValues.size - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f)) // Spacer to push text and decrement buttons apart

        // Value Text
        val displayText = if (currentValue is Double) {
            // Format double to 1 decimal place if it's not a whole number, else show as int
            if (currentValue.rem(1) == 0.0) currentValue.toInt().toString() else String.format("%.1f", currentValue)
        } else {
            currentValue.toString()
        }
        Text(
            text = "$label $displayText",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f) // Allow text to take space but also be constrained
        )

        Spacer(modifier = Modifier.weight(0.5f)) // Spacer to push text and decrement buttons apart

        // Decrement buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            decrementValues.reversed().forEachIndexed { index, value ->
                 if (index > 0) { // Add spacer before the second button onwards from the right
                    Spacer(modifier = Modifier.width(4.dp))
                }
                CounterButton(text = "- $value") { onDecrement(value) }
            }
        }
    }
}

@Composable
private fun CounterButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
