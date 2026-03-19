package com.FreeRave.shredzilla.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background // Added import
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets // Added import
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme // Added for theme check
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults // Added import
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.auth.composables.PasswordFormField
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
// Import purple colors & ThemeManager
import com.FreeRave.shredzilla.ui.theme.Purple40
import com.FreeRave.shredzilla.ui.theme.Purple80
// import com.FreeRave.shredzilla.ui.theme.PurpleGrey40 // Not needed for buttons directly
// import com.FreeRave.shredzilla.ui.theme.PurpleGrey80 // Not needed for buttons directly
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.screens.settings.ThemeSetting


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMismatchError by remember { mutableStateOf(false) }

    // Determine if overall app is in dark mode (respecting user's theme choice)
    val currentGender = ThemeManager.currentGenderTheme
    val effectiveThemeSetting = ThemeManager.getEffectiveThemeSetting(
        currentGender,
        ThemeManager.themePreferenceMale,
        ThemeManager.themePreferenceFemale
    )
    val useDarkTheme = when (effectiveThemeSetting) {
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
    }

    // Define purple colors for buttons based on light/dark mode
    val primaryPurpleButtonContainer = if (useDarkTheme) Purple80 else Purple40
    val primaryPurpleButtonContent = if (useDarkTheme) Color.Black else Color.White
    val textButtonPurpleColor = if (useDarkTheme) Purple80 else Purple40

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.register_background),
            contentDescription = "Register Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)))

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Shredzilla",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Removed ElevatedCard
                Text(
                    "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White // Adjusted for direct overlay
                )
                Spacer(modifier = Modifier.height(24.dp))

                TextFormField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name Icon") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextFormField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                PasswordFormField(
                    value = password,
                    onValueChange = { password = it; passwordMismatchError = false },
                    label = "Password",
                    imeAction = ImeAction.Next,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                    isError = passwordMismatchError
                )
                Spacer(modifier = Modifier.height(16.dp))

                PasswordFormField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordMismatchError = false },
                    label = "Confirm Password",
                    imeAction = ImeAction.Done,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Confirm Password Icon") },
                    isError = passwordMismatchError,
                    errorMessage = if (passwordMismatchError) "Passwords do not match" else null
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                } else {
                    Button(
                        onClick = {
                            if (password == confirmPassword) {
                                onRegisterClick(name, email, password, confirmPassword)
                            } else {
                                passwordMismatchError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryPurpleButtonContainer,
                            contentColor = primaryPurpleButtonContent
                        )
                    ) {
                        Text("Register", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Already have an account? Login",
                        color = textButtonPurpleColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Register Light")
@Composable
fun RegisterScreenLightPreview() {
    ShredzillaTheme(dynamicColor = false) {
        RegisterScreen(onRegisterClick = { _, _, _, _ -> }, onNavigateToLogin = {}, isLoading = false)
    }
}
@Preview(showBackground = true, name = "Register Dark")
@Composable
fun RegisterScreenDarkPreview() {
    ShredzillaTheme(dynamicColor = false) {
        RegisterScreen(onRegisterClick = { _, _, _, _ -> }, onNavigateToLogin = {}, isLoading = false)
    }
}
@Preview(showBackground = true, name = "Register Loading")
@Composable
fun RegisterScreenLoadingPreview() {
    ShredzillaTheme(dynamicColor = false) {
        RegisterScreen(onRegisterClick = { _, _, _, _ -> }, onNavigateToLogin = {}, isLoading = true)
    }
}
