package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.theme.*
import com.FreeRave.shredzilla.models.ExerciseItem
import com.FreeRave.shredzilla.models.initialGlobalExerciseList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialExercisesScreen(onExercisesSelected: (List<String>) -> Unit) {
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
                    isSelected = true // Default to selected
                )
            }.toTypedArray()
        )
    }

    // Button scale animation
    var btnPressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue = if (btnPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "btnScale"
    )

    // Button gradient & accent
    val btnGradient = Brush.horizontalGradient(
        listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
    )
    val screenAccent = AuthBtnGradientEnd

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuthBgTop, AuthBgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Glowing Icon ───────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Purple40.copy(alpha = 0.22f))
                    .border(1.dp, screenAccent.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Checklist,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Set Initial Exercises",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select exercises you plan on doing.\nYou can easily add more later.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.60f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Exercise Selection List ────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(exerciseScreenItems) { exercise ->
                    ExerciseSelectionCard(
                        exercise = exercise,
                        accentColor = screenAccent,
                        onExerciseSelected = {
                            val index = exerciseScreenItems.indexOf(exercise)
                            if (index != -1) {
                                exerciseScreenItems[index] = exerciseScreenItems[index].copy(
                                    isSelected = !exerciseScreenItems[index].isSelected
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Next Button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .scale(btnScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(btnGradient)
            ) {
                Button(
                    onClick = {
                        val selectedIds = exerciseScreenItems.filter { it.isSelected }.map { it.id }
                        onExercisesSelected(selectedIds)
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = null
                ) {
                    Text(
                        text = "Next →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseSelectionCard(
    exercise: ExerciseItem,
    accentColor: Color,
    onExerciseSelected: () -> Unit
) {
    val cardBgColor = if (exercise.isSelected) {
        accentColor.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }

    val cardBorderColor = if (exercise.isSelected) {
        accentColor.copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onExerciseSelected),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (exercise.isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = if (exercise.isSelected) "Selected" else "Not selected",
                tint = if (exercise.isSelected) accentColor else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = exercise.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InitialExercisesScreenPreview() {
    ShredzillaTheme {
        InitialExercisesScreen {}
    }
}
