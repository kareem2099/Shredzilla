package com.FreeRave.shredzilla.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.FreeRave.shredzilla.ui.theme.Purple40
import com.FreeRave.shredzilla.ui.theme.Purple80
import com.FreeRave.shredzilla.ui.theme.Pink80
import com.FreeRave.shredzilla.ui.theme.AuthBgTop
import com.FreeRave.shredzilla.ui.theme.AuthBgBottom
import com.FreeRave.shredzilla.ui.theme.AuthBtnGradientStart
import com.FreeRave.shredzilla.ui.theme.AuthBtnGradientEnd
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.screens.settings.ThemeSetting

// Auth screen background — shared with LoginScreen constants
// Auth background colors are defined in Color.kt (AuthBgTop / AuthBgBottom)
// Register uses the same dark purple gradient as Login

// WCAG AA-compliant field colors for dark background (ratio > 4.5:1)
@OptIn(ExperimentalMaterial3Api::class)
private val regFieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedTextColor           = Color.White,
    unfocusedTextColor         = Color.White.copy(alpha = 0.85f),
    focusedLabelColor          = Color.White.copy(alpha = 0.9f),
    unfocusedLabelColor        = Color.White.copy(alpha = 0.6f),
    cursorColor                = Purple80,
    focusedBorderColor         = Purple80.copy(alpha = 0.8f),
    unfocusedBorderColor       = Color.White.copy(alpha = 0.25f),
    focusedContainerColor      = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor    = Color.White.copy(alpha = 0.06f),
    focusedLeadingIconColor    = Purple80,
    unfocusedLeadingIconColor  = Color.White.copy(alpha = 0.6f),
    focusedTrailingIconColor   = Purple80,
    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f),
    // Disabled state — consistent with dark bg, no jarring Material gray
    disabledTextColor          = Color.White.copy(alpha = 0.38f),
    disabledLabelColor         = Color.White.copy(alpha = 0.30f),
    disabledBorderColor        = Color.White.copy(alpha = 0.12f),
    disabledContainerColor     = Color.White.copy(alpha = 0.04f),
    disabledLeadingIconColor   = Color.White.copy(alpha = 0.30f),
    disabledTrailingIconColor  = Color.White.copy(alpha = 0.30f),
    errorTextColor             = Pink80,
    errorLabelColor            = Pink80,
    errorBorderColor           = Pink80,
    errorCursorColor           = Pink80,
    errorContainerColor        = Color.White.copy(alpha = 0.06f),
    errorLeadingIconColor      = Pink80,
    errorTrailingIconColor     = Pink80
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false
) {
    var name                by rememberSaveable { mutableStateOf("") }
    var email               by rememberSaveable { mutableStateOf("") }
    var password            by rememberSaveable { mutableStateOf("") }
    var confirmPassword     by rememberSaveable { mutableStateOf("") }
    var passwordMismatchError by rememberSaveable { mutableStateOf(false) }
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Focus chain: Name → Email → Password → ConfirmPassword → Done
    val emailFocusRequester          = remember { FocusRequester() }
    val passwordFocusRequester       = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Brand-gradient register button: same two-stop mono purple as Login — see Color.kt
    val registerButtonGradient = Brush.horizontalGradient(
        colors = listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
    )

    val fieldColors = regFieldColors

    // Single source of truth for register validation — called from both keyboard onDone and button onClick
    fun attemptRegister() {
        focusManager.clearFocus()
        when {
            name.isBlank() || email.isBlank() ||
            password.isBlank() || confirmPassword.isBlank() -> Toast.makeText(
                context, "Please fill in all fields.", Toast.LENGTH_SHORT
            ).show()
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> Toast.makeText(
                context, "Please enter a valid email address.", Toast.LENGTH_SHORT
            ).show()
            password.length < 6 -> Toast.makeText(
                context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT
            ).show()
            password == confirmPassword ->
                onRegisterClick(name.trim(), email.trim(), password, confirmPassword)
            else -> passwordMismatchError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AuthBgTop, AuthBgBottom))
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Dumbbell icon inside glowing circle ──────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Purple40.copy(alpha = 0.22f))
                        .border(1.dp, Purple80.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dumbbell),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Shredzilla",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Name field ───────────────────────────────────────────
                TextFormField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = "Name Icon")
                    },
                    keyboardActions = KeyboardActions(
                        onNext = { emailFocusRequester.requestFocus() }
                    ),
                    customColors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Email field ──────────────────────────────────────────
                TextFormField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.focusRequester(emailFocusRequester),
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = "Email Icon")
                    },
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    customColors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Password field ───────────────────────────────────────
                PasswordFormField(
                    value = password,
                    onValueChange = { password = it; passwordMismatchError = false },
                    label = "Password",
                    enabled = !isLoading,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.focusRequester(passwordFocusRequester),
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Password Icon")
                    },
                    keyboardActions = KeyboardActions(
                        onNext = { confirmPasswordFocusRequester.requestFocus() }
                    ),
                    isError = passwordMismatchError,
                    errorMessage = if (passwordMismatchError) "Passwords do not match" else null,
                    customColors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Confirm password field ───────────────────────────────
                PasswordFormField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordMismatchError = false },
                    label = "Confirm Password",
                    enabled = !isLoading,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Confirm Password Icon")
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isLoading) return@KeyboardActions
                            attemptRegister()
                        }
                    ),
                    isError = passwordMismatchError,
                    errorMessage = if (passwordMismatchError) "Passwords do not match" else null,
                    customColors = fieldColors
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Register button (brand gradient) ─────────────────────
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Purple80,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(registerButtonGradient)
                    ) {
                        Button(
                            onClick = { attemptRegister() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor   = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(
                                "Register",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Already have an account? Login",
                        color = Purple80,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Register Preview")
@Composable
fun RegisterScreenPreview() {
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
