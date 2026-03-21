package com.FreeRave.shredzilla.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.isSystemInDarkTheme // Added for theme check
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
// Import purple colors
import com.FreeRave.shredzilla.ui.theme.Purple40
import com.FreeRave.shredzilla.ui.theme.Purple80
import com.FreeRave.shredzilla.ui.theme.PurpleGrey40
import com.FreeRave.shredzilla.ui.theme.PurpleGrey80
import com.FreeRave.shredzilla.ui.theme.ThemeManager // To check overall theme mode
import com.FreeRave.shredzilla.screens.settings.ThemeSetting // For ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    isLoading: Boolean = false
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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

    val secondaryPurpleButtonContainer = if (useDarkTheme) PurpleGrey40 else PurpleGrey80
    val secondaryPurpleButtonContent = if (useDarkTheme) Purple80 else Purple40
    
    val textButtonPurpleColor = if (useDarkTheme) Purple80 else Purple40


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = "Login Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)))

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Shredzilla",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Removed ElevatedCard, form elements are now directly in the Column
                Text(
                    "Welcome Back!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White // Adjusted for direct overlay
                )
                Spacer(modifier = Modifier.height(24.dp))

                TextFormField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                PasswordFormField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    imeAction = ImeAction.Done,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") }
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                } else {
                    Button(
                        onClick = { 
                            focusManager.clearFocus()
                            if (email.isNotBlank() && password.isNotBlank()) {
                                onLoginClick(email.trim(), password) 
                            } else {
                                Toast.makeText(context, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryPurpleButtonContainer,
                            contentColor = primaryPurpleButtonContent
                        )
                    ) {
                        Text("Login", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onGoogleSignInClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = secondaryPurpleButtonContainer,
                        contentColor = secondaryPurpleButtonContent
                    )
                ) {
                    // Consider adding Google logo
                    Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Don't have an account? Register",
                        color = textButtonPurpleColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Light")
@Composable
fun LoginScreenLightPreview() {
    ShredzillaTheme(dynamicColor = false) {
        LoginScreen(onLoginClick = { _, _ -> }, onGoogleSignInClick = {}, onNavigateToRegister = {}, isLoading = false)
    }
}

@Preview(showBackground = true, name = "Login Dark")
@Composable
fun LoginScreenDarkPreview() {
    ShredzillaTheme(dynamicColor = false) {
         LoginScreen(onLoginClick = { _, _ -> }, onGoogleSignInClick = {}, onNavigateToRegister = {}, isLoading = false)
    }
}

@Preview(showBackground = true, name = "Login Loading")
@Composable
fun LoginScreenLoadingPreview() {
    ShredzillaTheme(dynamicColor = false) {
        LoginScreen(onLoginClick = { _, _ -> }, onGoogleSignInClick = {}, onNavigateToRegister = {}, isLoading = true)
    }
}
