package com.FreeRave.shredzilla.screens.exercises.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ExerciseFilterOption(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (enabled) (if (selected) Color.White else Color.Gray) else Color.DarkGray
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        RadioButton(
            selected = selected && enabled, // RadioButton should appear unselected if disabled
            onClick = if(enabled) onClick else null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.Green,
                unselectedColor = Color.Gray,
                disabledSelectedColor = Color.DarkGray, // Technically won't be selected if disabled
                disabledUnselectedColor = Color.DarkGray
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = textColor)
    }
}
