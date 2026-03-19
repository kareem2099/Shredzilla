package com.FreeRave.shredzilla.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.auth.composables.TextFormField 
import com.FreeRave.shredzilla.ui.theme.ThemeManager // Import ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalDetailsScreen(onDetailsSubmitted: (age: String, height: String, weight: String) -> Unit) {
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    var ageError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current // For Toasts

    val gender = ThemeManager.currentGenderTheme
    val backgroundImageRes = if (gender == "Female") {
        R.drawable.sec_page_female 
    } else {
        R.drawable.sec_page 
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Details", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface) // Ensure text is readable on background
            Spacer(modifier = Modifier.height(24.dp))

            TextFormField(
                value = age,
                onValueChange = {
                    age = it
                    ageError = null // Clear error when user types
                },
                label = "Age (years)",
                keyboardType = KeyboardType.Number,
                isError = ageError != null,
                errorMessage = ageError
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextFormField(
                value = height,
                onValueChange = {
                    height = it
                    heightError = null // Clear error when user types
                },
                label = "Height (cm)",
                keyboardType = KeyboardType.Number,
                isError = heightError != null,
                errorMessage = heightError
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextFormField(
                value = weight,
                onValueChange = {
                    weight = it
                    weightError = null // Clear error when user types
                },
                label = "Weight (kg)",
                keyboardType = KeyboardType.Decimal,
                isError = weightError != null,
                errorMessage = weightError
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    var isValid = true
                    if (age.isBlank()) {
                        ageError = "Age cannot be empty"
                        isValid = false
                    } else if (age.toIntOrNull() == null || age.toInt() <= 0) {
                        ageError = "Please enter a valid age"
                        isValid = false
                    }
                    if (height.isBlank()) {
                        heightError = "Height cannot be empty"
                        isValid = false
                    } else if (height.toDoubleOrNull() == null || height.toDouble() <= 0) {
                        heightError = "Please enter a valid height"
                        isValid = false
                    }
                    if (weight.isBlank()) {
                        weightError = "Weight cannot be empty"
                        isValid = false
                    } else if (weight.toDoubleOrNull() == null || weight.toDouble() <= 0) {
                        weightError = "Please enter a valid weight"
                        isValid = false
                    }

                    if (isValid) {
                        onDetailsSubmitted(age, height, weight)
                    } else {
                        // Optionally show a general Toast message
                        // Toast.makeText(context, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
    }
}

@Preview(showBackground = true, name = "Physical Details Light Male")
@Composable
fun PhysicalDetailsScreenMaleLightPreview() {
    ThemeManager.currentGenderTheme = "Male" 
    // ThemeManager.themePreferenceMale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        PhysicalDetailsScreen { _, _, _ -> }
    }
}

@Preview(showBackground = true, name = "Physical Details Dark Male")
@Composable
fun PhysicalDetailsScreenMaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Male" 
    // ThemeManager.themePreferenceMale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        PhysicalDetailsScreen { _, _, _ -> }
    }
}

@Preview(showBackground = true, name = "Physical Details Light Female")
@Composable
fun PhysicalDetailsScreenFemaleLightPreview() {
    ThemeManager.currentGenderTheme = "Female" 
    // ThemeManager.themePreferenceFemale = ThemeSetting.LIGHT // To force light for this preview
    ShredzillaTheme {
        PhysicalDetailsScreen { _, _, _ -> }
    }
}

@Preview(showBackground = true, name = "Physical Details Dark Female")
@Composable
fun PhysicalDetailsScreenFemaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Female" 
    // ThemeManager.themePreferenceFemale = ThemeSetting.DARK // To force dark for this preview
    ShredzillaTheme {
        PhysicalDetailsScreen { _, _, _ -> }
    }
}
