package com.FreeRave.shredzilla.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.FreeRave.shredzilla.auth.composables.AuthScaffold
import com.FreeRave.shredzilla.auth.composables.AuthSwitchPrompt
import com.FreeRave.shredzilla.auth.composables.PasswordFormField
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassDefaults
import com.FreeRave.shredzilla.ui.glass.GlassDividerWithText
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.theme.GoogleButtonBg
import com.FreeRave.shredzilla.ui.theme.GoogleButtonContent
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

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
    val palette      = MaterialTheme.glass

    // Focus chain: Email → Password
    val passwordFocusRequester = remember { FocusRequester() }

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

    AuthScaffold(title = "Shredzilla", subtitle = "Welcome back, let's get to work") {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TextFormField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                enabled = !isLoading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
            )

            PasswordFormField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                enabled = !isLoading,
                imeAction = ImeAction.Done,
                modifier = Modifier.focusRequester(passwordFocusRequester),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                keyboardActions = KeyboardActions(onDone = { if (!isLoading) attemptLogin() })
            )

            TextButton(
                onClick = onNavigateToForgotPassword,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password?", color = palette.accent, style = MaterialTheme.typography.labelLarge)
            }

            GlassPrimaryButton(text = "Login", onClick = ::attemptLogin, loading = isLoading)
        }

        Spacer(Modifier.height(24.dp))
        GlassDividerWithText("or continue with")
        Spacer(Modifier.height(24.dp))

        GoogleSignInButton(onClick = onGoogleSignInClick, enabled = !isLoading)

        Spacer(Modifier.height(20.dp))

        AuthSwitchPrompt(
            prompt = "Don't have an account?",
            action = "Register",
            onClick = onNavigateToRegister,
            enabled = !isLoading
        )
    }
}

/** White Google button — colours and logo follow Google's sign-in branding guidelines. */
@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(GlassDefaults.ButtonHeight),
        shape = GlassDefaults.ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = GoogleButtonBg,
            contentColor = GoogleButtonContent,
            disabledContainerColor = GoogleButtonBg.copy(alpha = 0.6f),
            disabledContentColor = GoogleButtonContent.copy(alpha = 0.4f)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                tint = Color.Unspecified, // preserve the 4-color logo
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Sign in with Google", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(name = "Login")
@Composable
private fun LoginScreenPreview() {
    ShredzillaTheme(dynamicColor = false) {
        LoginScreen(
            onLoginClick = { _, _ -> },
            onGoogleSignInClick = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}

@Preview(name = "Login Loading")
@Composable
private fun LoginScreenLoadingPreview() {
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
