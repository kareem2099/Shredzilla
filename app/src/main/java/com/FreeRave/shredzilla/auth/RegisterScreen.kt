package com.FreeRave.shredzilla.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.auth.composables.AuthScaffold
import com.FreeRave.shredzilla.auth.composables.AuthSwitchPrompt
import com.FreeRave.shredzilla.auth.composables.PasswordFormField
import com.FreeRave.shredzilla.auth.composables.PasswordStrengthIndicator
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.glass

private const val TERMS_URL = "https://shredzilla.app/terms"
private const val PRIVACY_URL = "https://shredzilla.app/privacy"
private const val MIN_PASSWORD_LENGTH = 6

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false
) {
    var name                  by rememberSaveable { mutableStateOf("") }
    var email                 by rememberSaveable { mutableStateOf("") }
    var password              by rememberSaveable { mutableStateOf("") }
    var confirmPassword       by rememberSaveable { mutableStateOf("") }
    var tosAccepted           by rememberSaveable { mutableStateOf(false) }
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Live check: flag as soon as the confirmation diverges, not only after pressing the button
    val passwordsMismatch = confirmPassword.isNotEmpty() && confirmPassword != password

    // Focus chain: Name → Email → Password → ConfirmPassword → Done
    val emailFocusRequester           = remember { FocusRequester() }
    val passwordFocusRequester        = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Single source of truth for register validation — called from both keyboard onDone and button onClick
    fun attemptRegister() {
        focusManager.clearFocus()
        val message = when {
            !tosAccepted -> "Please accept the Terms of Service to continue."
            name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                "Please fill in all fields."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Please enter a valid email address."
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
            passwordsMismatch -> "Passwords do not match."
            else -> null
        }
        if (message != null) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        else onRegisterClick(name.trim(), email.trim(), password, confirmPassword)
    }

    AuthScaffold(title = "Create Account", subtitle = "Start your transformation today") {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TextFormField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                enabled = !isLoading,
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() })
            )

            TextFormField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                enabled = !isLoading,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.focusRequester(emailFocusRequester),
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() })
            )

            PasswordFormField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                enabled = !isLoading,
                imeAction = ImeAction.Next,
                modifier = Modifier.focusRequester(passwordFocusRequester),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() })
            )

            PasswordStrengthIndicator(password = password)

            PasswordFormField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                enabled = !isLoading,
                modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                keyboardActions = KeyboardActions(onDone = { if (!isLoading) attemptRegister() }),
                isError = passwordsMismatch,
                errorMessage = "Passwords do not match"
            )

            TermsCheckbox(checked = tosAccepted, onCheckedChange = { tosAccepted = it }, enabled = !isLoading)

            GlassPrimaryButton(
                text = "Create Account",
                onClick = ::attemptRegister,
                enabled = tosAccepted,
                loading = isLoading
            )
        }

        Spacer(Modifier.height(20.dp))

        AuthSwitchPrompt(
            prompt = "Already have an account?",
            action = "Login",
            onClick = onNavigateToLogin,
            enabled = !isLoading
        )
    }
}

@Composable
private fun TermsCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean) {
    val palette = MaterialTheme.glass
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = palette.accent, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
    )
    val termsText = buildAnnotatedString {
        append("I agree to the ")
        withLink(LinkAnnotation.Url(TERMS_URL, linkStyle)) { append("Terms of Service") }
        append(" and ")
        withLink(LinkAnnotation.Url(PRIVACY_URL, linkStyle)) { append("Privacy Policy") }
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = palette.accent,
                uncheckedColor = palette.contentMuted,
                checkmarkColor = palette.onAccent,
                disabledCheckedColor = palette.accent.copy(alpha = 0.4f),
                disabledUncheckedColor = palette.content.copy(alpha = 0.2f)
            )
        )
        Text(
            text = termsText,
            color = palette.contentMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Preview(name = "Register")
@Composable
private fun RegisterScreenPreview() {
    ShredzillaTheme(dynamicColor = false) {
        RegisterScreen(onRegisterClick = { _, _, _, _ -> }, onNavigateToLogin = {})
    }
}

@Preview(name = "Register Loading")
@Composable
private fun RegisterScreenLoadingPreview() {
    ShredzillaTheme(dynamicColor = false) {
        RegisterScreen(onRegisterClick = { _, _, _, _ -> }, onNavigateToLogin = {}, isLoading = true)
    }
}
