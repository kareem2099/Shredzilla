package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Correct import for LazyColumn items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.models.ExerciseItem // Import from models
import com.FreeRave.shredzilla.models.initialGlobalExerciseList // Import from models

// ExerciseItem data class and initialExerciseList are now in models.ExerciseModels.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialExercisesScreen(onExercisesSelected: (List<String>) -> Unit) {
    // Create a mutable list of ExerciseItem with selection state for this screen
    val exerciseScreenItems = remember {
        mutableStateListOf(
            *initialGlobalExerciseList.map { model ->
                ExerciseItem(
                    id = model.id,
                    name = model.name,
                    description = model.description,
                    videoUrl = model.videoUrl,
                    targetMuscles = model.targetMuscles,
                    equipmentNeeded = model.equipmentNeeded,
                    difficulty = model.difficulty,
                    isSelected = true // Default to selected for this screen
                )
            }.toTypedArray()
        )
    }

    val gender = ThemeManager.currentGenderTheme
    val backgroundImageRes = if (gender == "Female") {
        R.drawable.forth_page_female
    } else {
        R.drawable.forth_page_male
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundImageRes),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Filled.Checklist,
                contentDescription = "Set Initial Exercises Icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Set Initial Exercises",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // Ensure text is readable
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select exercises you plan on doing.\nYou will add more later",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Ensure readability
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(exerciseScreenItems) { exercise -> // Use the local mutable state list
                    ExerciseSelectionCard(
                        exercise = exercise, // This is now the local ExerciseItem with isSelected state
                        onExerciseSelected = {
                            val index = exerciseScreenItems.indexOf(exercise)
                            if (index != -1) {
                                exerciseScreenItems[index] = exerciseScreenItems[index].copy(isSelected = !exerciseScreenItems[index].isSelected)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val selectedIds = exerciseScreenItems.filter { it.isSelected }.map { it.id }
                    onExercisesSelected(selectedIds)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Next")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ExerciseSelectionCard(
    exercise: ExerciseItem,
    onExerciseSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExerciseSelected),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (exercise.isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = if (exercise.isSelected) "Selected" else "Not selected",
                tint = if (exercise.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Preview(showBackground = true, name = "Initial Exercises Light Male")
@Composable
fun InitialExercisesScreenMaleLightPreview() {
    ThemeManager.currentGenderTheme = "Male"
    // ThemeManager.themePreferenceMale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        Surface { InitialExercisesScreen {} }
    }
}

@Preview(showBackground = true, name = "Initial Exercises Dark Male")
@Composable
fun InitialExercisesScreenMaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Male"
    // ThemeManager.themePreferenceMale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        Surface { InitialExercisesScreen {} }
    }
}

@Preview(showBackground = true, name = "Initial Exercises Light Female")
@Composable
fun InitialExercisesScreenFemaleLightPreview() {
    ThemeManager.currentGenderTheme = "Female"
    // ThemeManager.themePreferenceFemale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        Surface { InitialExercisesScreen {} }
    }
}

@Preview(showBackground = true, name = "Initial Exercises Dark Female")
@Composable
fun InitialExercisesScreenFemaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Female"
    // ThemeManager.themePreferenceFemale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        Surface { InitialExercisesScreen {} }
    }
}
