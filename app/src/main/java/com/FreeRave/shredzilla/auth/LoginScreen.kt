package com.FreeRave.shredzilla.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.FreeRave.shredzilla.ui.theme.GoogleButtonBg
import com.FreeRave.shredzilla.ui.theme.GoogleButtonContent
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.screens.settings.ThemeSetting

// Auth screen background colors — dark purple gradient
// Auth background colors are defined in Color.kt (AuthBgTop / AuthBgBottom)

// WCAG AA-compliant field colors for dark background (ratio > 4.5:1)
@OptIn(ExperimentalMaterial3Api::class)
internal val authFieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
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
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    isLoading: Boolean = false
) {
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Focus chain: Email → Password
    val passwordFocusRequester = remember { FocusRequester() }

    // Brand-gradient login button: two stops from the same purple hue — see Color.kt
    val loginButtonGradient = Brush.horizontalGradient(
        colors = listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
    )

    // Google button: white background per Google branding guidelines — see Color.kt
    val googleButtonBg      = GoogleButtonBg
    val googleButtonContent = GoogleButtonContent

    val fieldColors = authFieldColors

    // Single source of truth for login validation — called from both keyboard onDone and button onClick
    fun attemptLogin() {
        focusManager.clearFocus()
        when {
            email.isBlank() || password.isBlank() -> Toast.makeText(
                context, "Please enter both email and password.", Toast.LENGTH_SHORT
            ).show()
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> Toast.makeText(
                context, "Please enter a valid email address.", Toast.LENGTH_SHORT
            ).show()
            else -> onLoginClick(email.trim(), password)
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
                    .padding(horizontal = 28.dp, vertical = 24.dp),
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
                    text = "Welcome Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // ── Email field ──────────────────────────────────────────
                TextFormField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
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
                    onValueChange = { password = it },
                    label = "Password",
                    enabled = !isLoading,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.focusRequester(passwordFocusRequester),
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Password Icon")
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isLoading) return@KeyboardActions
                            attemptLogin()
                        }
                    ),
                    customColors = fieldColors
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Login button (brand gradient) ────────────────────────
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
                            .background(loginButtonGradient)
                    ) {
                        Button(
                            onClick = { attemptLogin() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor   = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(
                                "Login",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Google button (white bg — Google branding compliant) ──
                Button(
                    onClick = onGoogleSignInClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp), // Google spec: 4dp corner radius
                    colors = ButtonDefaults.buttonColors(
                        containerColor = googleButtonBg,
                        contentColor   = googleButtonContent,
                        disabledContainerColor = googleButtonBg.copy(alpha = 0.6f),
                        disabledContentColor   = googleButtonContent.copy(alpha = 0.4f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = "Google logo",
                            tint = Color.Unspecified, // preserve the 4-color logo
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Sign in with Google",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(onClick = onNavigateToForgotPassword) {
                    Text(
                        "Forgot Password?",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Don't have an account? Register",
                        color = Purple80,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Preview")
@Composable
fun LoginScreenPreview() {
    ShredzillaTheme(dynamicColor = false) {
        LoginScreen(
            onLoginClick = { _, _ -> },
            onGoogleSignInClick = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {},
            isLoading = false
        )
    }
}

@Preview(showBackground = true, name = "Login Loading")
@Composable
fun LoginScreenLoadingPreview() {
    ShredzillaTheme(dynamicColor = false) {
        LoginScreen(
            onLoginClick = { _, _ -> },
            onGoogleSignInClick = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {},
            isLoading = true
        )
    }
}
