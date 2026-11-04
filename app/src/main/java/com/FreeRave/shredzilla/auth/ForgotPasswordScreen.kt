package com.FreeRave.shredzilla.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.auth.composables.AuthScaffold
import com.FreeRave.shredzilla.auth.composables.AuthSwitchPrompt
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

@Composable
fun ForgotPasswordScreen(
    onSendResetEmail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    isEmailSent: Boolean = false
) {
    var email      by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    fun attemptReset() {
        focusManager.clearFocus()
        emailError = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Please enter a valid email address"
            else -> null
        }
        if (emailError == null) onSendResetEmail(email.trim())
    }

    AuthScaffold(
        title = if (isEmailSent) "Check your inbox" else "Forgot Password?",
        subtitle = if (isEmailSent) {
            "We sent a reset link to ${email.trim()}. Follow it to set a new password."
        } else {
            "Enter the email linked to your account and we'll send you a reset link."
        },
        icon = rememberVectorPainter(if (isEmailSent) Icons.Filled.MarkEmailRead else Icons.Filled.LockReset)
    ) {
        AnimatedContent(
            targetState = isEmailSent,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
            label = "forgotPasswordState"
        ) { sent ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                if (sent) {
                    GlassPrimaryButton(text = "Back to Login", onClick = onNavigateBack)
                } else {
                    TextFormField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = "Email",
                        enabled = !isLoading,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        isError = emailError != null,
                        errorMessage = emailError,
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardActions = KeyboardActions(onDone = { if (!isLoading) attemptReset() })
                    )
                    GlassPrimaryButton(text = "Send Reset Link", onClick = ::attemptReset, loading = isLoading)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (isEmailSent) {
            AuthSwitchPrompt(
                prompt = "Didn't get the email?",
                action = "Resend",
                onClick = { onSendResetEmail(email.trim()) },
                enabled = !isLoading
            )
        } else {
            AuthSwitchPrompt(
                prompt = "Remembered it?",
                action = "Back to Login",
                onClick = onNavigateBack,
                enabled = !isLoading
            )
        }
    }
}

@Preview(name = "Forgot Password — Input")
@Composable
private fun ForgotPasswordPreview() {
    ShredzillaTheme(dynamicColor = false) {
        ForgotPasswordScreen(onSendResetEmail = {}, onNavigateBack = {})
    }
}

@Preview(name = "Forgot Password — Sent")
@Composable
private fun ForgotPasswordSentPreview() {
    ShredzillaTheme(dynamicColor = false) {
        ForgotPasswordScreen(onSendResetEmail = {}, onNavigateBack = {}, isEmailSent = true)
    }
}
