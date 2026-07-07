package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

// ── Gender card brand colours ────────────────────────────────────────────────
// Female: rose → magenta diagonal half
private val FemBgTop     = Color(0xFF3B0A2A)
private val FemBgBottom  = Color(0xFF7B1458)
private val FemAccent    = Color(0xFFFF80AB)

// Male: deep-teal → green diagonal half
private val MaleBgTop    = Color(0xFF00210F)
private val MaleBgBottom = Color(0xFF006D3D)
private val MaleAccent   = Color(0xFF50D892)

@Composable
fun GenderSelectionScreen(onGenderSelected: (String) -> Unit) {
    var selectedGender    by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Pressed-scale animations
    var femalePressed by remember { mutableStateOf(false) }
    var malePressed   by remember { mutableStateOf(false) }
    val femaleScale by animateFloatAsState(
        targetValue    = if (femalePressed) 0.97f else 1f,
        animationSpec  = tween(120),
        label          = "femaleScale"
    )
    val maleScale by animateFloatAsState(
        targetValue    = if (malePressed) 0.97f else 1f,
        animationSpec  = tween(120),
        label          = "maleScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen Canvas: two gradient triangles + dashed divider ───────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Female triangle — top-left
            val femalePath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path  = femalePath,
                brush = Brush.linearGradient(
                    colors = listOf(FemBgTop, FemBgBottom),
                    start  = Offset(0f, 0f),
                    end    = Offset(w * 0.5f, h)
                )
            )

            // Male triangle — bottom-right
            val malePath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path  = malePath,
                brush = Brush.linearGradient(
                    colors = listOf(MaleBgTop, MaleBgBottom),
                    start  = Offset(w * 0.5f, 0f),
                    end    = Offset(w, h)
                )
            )

            // Dashed diagonal divider
            drawLine(
                color       = Color.White.copy(alpha = 0.20f),
                start       = Offset(w, 0f),
                end         = Offset(0f, h),
                strokeWidth = 2.dp.toPx(),
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
            )
        }

        // ── Female tap zone (top-left triangle) ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(femaleScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            femalePressed = true
                            tryAwaitRelease()
                            femalePressed = false
                        },
                        onTap = { offset ->
                            if (offset.x / size.width + offset.y / size.height <= 1f) {
                                selectedGender    = "Female"
                                showConfirmDialog = true
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 44.dp, top = 72.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Glowing circle icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.size(72.dp).clip(CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(FemAccent.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        drawCircle(
                            color  = FemAccent.copy(alpha = 0.30f),
                            radius = size.minDimension / 2f,
                            style  = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    Icon(
                        imageVector        = Icons.Filled.Female,
                        contentDescription = "Female",
                        tint               = FemAccent,
                        modifier           = Modifier.size(40.dp)
                    )
                }
                Text(
                    text          = "FEMALE",
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color         = Color.White
                )
                Text(
                    text     = "For be cutie \uD83D\uDC95",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color    = FemAccent.copy(alpha = 0.80f)
                )
                Text(
                    text     = "Tap to select",
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.40f)
                )
            }
        }

        // ── Male tap zone (bottom-right triangle) ─────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(maleScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            malePressed = true
                            tryAwaitRelease()
                            malePressed = false
                        },
                        onTap = { offset ->
                            if (offset.x / size.width + offset.y / size.height >= 1f) {
                                selectedGender    = "Male"
                                showConfirmDialog = true
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 44.dp, bottom = 80.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text     = "Tap to select",
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.40f)
                )
                Text(
                    text       = "For be beast \uD83D\uDCAA",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaleAccent.copy(alpha = 0.80f)
                )
                Text(
                    text          = "MALE",
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color         = Color.White
                )
                // Glowing circle icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.size(72.dp).clip(CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(MaleAccent.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        drawCircle(
                            color  = MaleAccent.copy(alpha = 0.30f),
                            radius = size.minDimension / 2f,
                            style  = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    Icon(
                        imageVector        = Icons.Filled.Male,
                        contentDescription = "Male",
                        tint               = MaleAccent,
                        modifier           = Modifier.size(40.dp)
                    )
                }
            }
        }

        // ── Centre label ──────────────────────────────────────────────────────
        Text(
            text       = "Who are you?",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White.copy(alpha = 0.65f),
            modifier   = Modifier.align(Alignment.Center)
        )
    }

    // ── Confirmation dialog ───────────────────────────────────────────────────
    if (showConfirmDialog && selectedGender != null) {
        val accent = if (selectedGender == "Female") FemAccent else MaleAccent
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedGender   = null
            },
            title = {
                Text(
                    text       = "${selectedGender!!} Selected",
                    fontWeight = FontWeight.Bold,
                    color      = accent
                )
            },
            text = { Text("You've selected ${selectedGender!!}. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        val gender        = selectedGender!!
                        showConfirmDialog = false
                        selectedGender    = null
                        onGenderSelected(gender)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Yes, Continue", color = Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    selectedGender   = null
                }) { Text("Cancel", color = accent) }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GenderSelectionPreview() {
    ShredzillaTheme {
        GenderSelectionScreen(onGenderSelected = {})
    }
}
