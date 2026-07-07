package com.FreeRave.shredzilla.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.auth.authFieldColors
import com.FreeRave.shredzilla.ui.theme.*

// ── Shared gradient reusing the auth palette ─────────────────────────────────
private val PhysBgTop    = Color(0xFF1B1040)   // = AuthBgTop
private val PhysBgBottom = Color(0xFF0C0918)   // = AuthBgBottom
private val PhysAccent   = Color(0xFF9A82DB)   // = AuthBtnGradientEnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalDetailsScreen(onDetailsSubmitted: (age: String, height: String, weight: String) -> Unit) {
    var age    by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    var ageError    by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }

    val focusManager      = LocalFocusManager.current
    val heightFocus       = remember { FocusRequester() }
    val weightFocus       = remember { FocusRequester() }

    // Button press animation
    var btnPressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue   = if (btnPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label         = "btnScale"
    )

    // Button gradient — same as auth screens
    val btnGradient = Brush.horizontalGradient(
        listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
    )

    // Field colours for dark background — same WCAG-AA palette as Login
    val fieldColors = authFieldColors

    fun validate(): Boolean {
        var ok = true
        ageError = when {
            age.isBlank()                                     -> "Age is required".also { ok = false }
            age.toIntOrNull() == null || age.toInt() <= 0    -> "Enter a valid age".also { ok = false }
            age.toInt() > 120                                 -> "Age must be ≤ 120".also { ok = false }
            else -> null
        }
        heightError = when {
            height.isBlank()                                          -> "Height is required".also { ok = false }
            height.toDoubleOrNull() == null || height.toDouble() <= 0 -> "Enter a valid height".also { ok = false }
            else -> null
        }
        weightError = when {
            weight.isBlank()                                          -> "Weight is required".also { ok = false }
            weight.toDoubleOrNull() == null || weight.toDouble() <= 0 -> "Enter a valid weight".also { ok = false }
            else -> null
        }
        return ok
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PhysBgTop, PhysBgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Icon ────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Purple40.copy(alpha = 0.22f))
                    .border(1.dp, PhysAccent.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector        = Icons.Filled.Person,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text       = "Your Details",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text     = "Help us personalise your experience",
                fontSize = 14.sp,
                color    = Color.White.copy(alpha = 0.60f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Age field ────────────────────────────────────────────────────
            TextFormField(
                value         = age,
                onValueChange = { age = it; ageError = null },
                label         = "Age (years)",
                keyboardType  = KeyboardType.Number,
                imeAction     = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { heightFocus.requestFocus() }),
                isError       = ageError != null,
                errorMessage  = ageError,
                customColors  = fieldColors,
                leadingIcon   = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (ageError != null) Pink80 else PhysAccent
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Height field ────────────────────────────────────────────────
            TextFormField(
                value         = height,
                onValueChange = { height = it; heightError = null },
                label         = "Height (cm)",
                keyboardType  = KeyboardType.Number,
                imeAction     = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { weightFocus.requestFocus() }),
                isError       = heightError != null,
                errorMessage  = heightError,
                customColors  = fieldColors,
                modifier      = Modifier.focusRequester(heightFocus),
                leadingIcon   = {
                    Icon(
                        Icons.Filled.Accessible,
                        contentDescription = null,
                        tint = if (heightError != null) Pink80 else PhysAccent
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Weight field ────────────────────────────────────────────────
            TextFormField(
                value         = weight,
                onValueChange = { weight = it; weightError = null },
                label         = "Weight (kg)",
                keyboardType  = KeyboardType.Decimal,
                imeAction     = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (validate()) onDetailsSubmitted(age, height, weight)
                }),
                isError       = weightError != null,
                errorMessage  = weightError,
                customColors  = fieldColors,
                modifier      = Modifier.focusRequester(weightFocus),
                leadingIcon   = {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = if (weightError != null) Pink80 else PhysAccent
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Next button ─────────────────────────────────────────────────
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
                        focusManager.clearFocus()
                        if (validate()) onDetailsSubmitted(age, height, weight)
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = null
                ) {
                    Text(
                        text       = "Next →",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhysicalDetailsPreview() {
    ShredzillaTheme {
        PhysicalDetailsScreen { _, _, _ -> }
    }
}
