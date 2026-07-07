package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
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

val workoutFrequencyOptions = listOf(
    "Once a week",
    "2 times per week",
    "3 times per week",
    "4 times per week",
    "5 times per week",
    "6 times per week",
    "Every day"
)

val nudgeDaysOptions = listOf(7, 6, 5, 4, 3, 2, 1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalScreen(onWeeklyGoalSelected: (frequency: String, nudgeDays: Int) -> Unit) {
    var selectedFrequencyIndex by remember { mutableStateOf(0) }

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
                .verticalScroll(rememberScrollState())
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
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Set a Weekly Goal",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Staying active is the only way you'll progress.\nWe will nudge you if you ever miss your goal.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.60f)
            )

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "How often will you workout?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Frequency selector card ────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (selectedFrequencyIndex > 0) {
                                selectedFrequencyIndex--
                            }
                        },
                        enabled = selectedFrequencyIndex > 0,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = screenAccent,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease frequency",
                            tint = if (selectedFrequencyIndex > 0) Color.Black else Color.White.copy(alpha = 0.35f)
                        )
                    }

                    Text(
                        text = workoutFrequencyOptions[selectedFrequencyIndex],
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            if (selectedFrequencyIndex < workoutFrequencyOptions.size - 1) {
                                selectedFrequencyIndex++
                            }
                        },
                        enabled = selectedFrequencyIndex < workoutFrequencyOptions.size - 1,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = screenAccent,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase frequency",
                            tint = if (selectedFrequencyIndex < workoutFrequencyOptions.size - 1) Color.Black else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── Nudge Info ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Nudge notification",
                    tint = screenAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Get nudged after ${nudgeDaysOptions[selectedFrequencyIndex]} days of inactivity.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            }

            Spacer(modifier = Modifier.weight(1f).heightIn(min = 32.dp))

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
                        onWeeklyGoalSelected(
                            workoutFrequencyOptions[selectedFrequencyIndex],
                            nudgeDaysOptions[selectedFrequencyIndex]
                        )
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

@Preview(showBackground = true)
@Composable
fun WeeklyGoalScreenPreview() {
    ShredzillaTheme {
        WeeklyGoalScreen { _, _ -> }
    }
}
