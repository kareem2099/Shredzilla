package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable // Added for onClick
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit // Or a more specific record icon like FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection // Import LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection // Import LayoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipableExerciseListItem(
    exerciseName: String,
    lastPerformed: String?,
    onDelete: () -> Unit,
    onRecord: () -> Unit,
    onClick: () -> Unit, // New parameter for item click
    swipeEnabled: Boolean = true, // New parameter to control swipeability
    modifier: Modifier = Modifier
) {
    val currentItemName by rememberUpdatedState(exerciseName)
    val currentLastPerformed by rememberUpdatedState(lastPerformed)
    val layoutDirection = LocalLayoutDirection.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (!swipeEnabled) return@rememberSwipeToDismissBoxState false

            val isRtl = layoutDirection == LayoutDirection.Rtl

            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> { // Physical swipe from right-to-left in LTR, or left-to-right in RTL
                    if (isRtl) onDelete() else onRecord()
                    if (isRtl) true else false // Dismiss for delete, not for record
                }
                SwipeToDismissBoxValue.StartToEnd -> { // Physical swipe from left-to-right in LTR, or right-to-left in RTL
                    if (isRtl) onRecord() else onDelete()
                    if (isRtl) false else true // Dismiss for delete, not for record
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(vertical = 1.dp),
        enableDismissFromStartToEnd = swipeEnabled, // Controlled by new parameter
        enableDismissFromEndToStart = swipeEnabled, // Controlled by new parameter
        backgroundContent = {
            DismissBehindContent(dismissState = dismissState)
        }
    ) {
        ExerciseCardItem(
            name = currentItemName,
            lastPerformed = currentLastPerformed,
            modifier = Modifier.clickable(onClick = onClick, enabled = swipeEnabled) // Apply onClick here, consider if swipeEnabled should also control clickability
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBehindContent(dismissState: SwipeToDismissBoxState) {
    val physicalDirection = dismissState.dismissDirection // This is StartToEnd or EndToStart based on physical swipe start
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    // Determine logical action based on physical swipe and layout direction
    val isRemoveAction = (physicalDirection == SwipeToDismissBoxValue.StartToEnd && !isRtl) || (physicalDirection == SwipeToDismissBoxValue.EndToStart && isRtl)
    val isRecordAction = (physicalDirection == SwipeToDismissBoxValue.EndToStart && !isRtl) || (physicalDirection == SwipeToDismissBoxValue.StartToEnd && isRtl)

    val color = when {
        isRemoveAction -> MaterialTheme.colorScheme.errorContainer
        isRecordAction -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val icon = when {
        isRemoveAction -> Icons.Filled.Delete
        isRecordAction -> Icons.Filled.Edit
        else -> null
    }
    val text = when {
        isRemoveAction -> "Remove"
        isRecordAction -> "Record"
        else -> ""
    }
    val textColor = when {
        isRemoveAction -> MaterialTheme.colorScheme.onErrorContainer
        isRecordAction -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> Color.Transparent
    }
    // Alignment of the icon and text within the background
    val alignment = when (physicalDirection) { // Align based on the physical edge revealed
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = text, tint = textColor)
                Spacer(Modifier.width(8.dp))
                Text(text, color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// This is the original ExerciseItem, renamed to avoid conflict if ExercisesScreen still has one.
// It will be the content of the SwipeToDismissBox.
@Composable
fun ExerciseCardItem(name: String, lastPerformed: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) { // Use a column to stack name and lastPerformed
                Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (lastPerformed != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastPerformed,
                        style = MaterialTheme.typography.labelSmall, // Smaller text for the date
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            // You could add other icons or indicators on the right if needed in the future
        }
    }
}
