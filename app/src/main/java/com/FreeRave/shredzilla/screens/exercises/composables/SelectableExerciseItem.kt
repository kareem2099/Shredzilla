package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// No longer importing from parent package, defined below.

// Data class defined here, co-located with its primary composable.
// If used more broadly, consider moving to a common models package.
data class SelectableExercise(
    val name: String,
    val id: String, // Or some unique identifier
    var isSelected: Boolean = false
)

@Composable
internal fun SelectableExerciseItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit // Changed from onExerciseSelected to simple onClick
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Use the new onClick
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent) // Use isSelected
                .border(
                    BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), // Use isSelected
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Optional: inner content for selected state
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name, // Use name
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground, // Assuming item is directly on background
            modifier = Modifier.weight(1f)
        )
        if (isSelected) { // Use isSelected
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
