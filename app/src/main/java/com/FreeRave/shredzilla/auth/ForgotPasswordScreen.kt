package com.FreeRave.shredzilla.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.R
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.ui.theme.AuthBgBottom
import com.FreeRave.shredzilla.ui.theme.AuthBgTop
import com.FreeRave.shredzilla.ui.theme.AuthBtnGradientEnd
import com.FreeRave.shredzilla.ui.theme.AuthBtnGradientStart
import com.FreeRave.shredzilla.ui.theme.Pink80
import com.FreeRave.shredzilla.ui.theme.Purple40
import com.FreeRave.shredzilla.ui.theme.Purple80
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onSendResetEmail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    isEmailSent: Boolean = false
) {
    var email by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val buttonGradient = Brush.horizontalGradient(
        listOf(AuthBtnGradientStart, AuthBtnGradientEnd)
    )

    // Reuse the same WCAG-AA compliant field colors as LoginScreen
    val fieldColors = authFieldColors

    fun attemptReset() {
        focusManager.clearFocus()
        emailError = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Please enter a valid email address"
            else -> null
        }
        if (emailError == null) {
            onSendResetEmail(email.trim())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuthBgTop, AuthBgBottom)))
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

                // ── Icon ─────────────────────────────────────────────────
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
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Success state ─────────────────────────────────────────
                AnimatedVisibility(
                    visible = isEmailSent,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Email sent",
                            tint = Purple80,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Reset email sent!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple80
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check your inbox and follow the link to reset your password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        TextButton(onClick = onNavigateBack) {
                            Text(
                                "Back to Login",
                                color = Purple80,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // ── Input form (hidden after success) ─────────────────────
                AnimatedVisibility(
                    visible = !isEmailSent,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Enter the email linked to your account. We'll send you a link to reset your password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // ── Email field ───────────────────────────────────
                        TextFormField(
                            value = email,
                            onValueChange = { email = it; emailError = null },
                            label = "Email",
                            enabled = !isLoading,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            isError = emailError != null,
                            errorMessage = emailError,
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = "Email Icon")
                            },
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (!isLoading) attemptReset()
                                }
                            ),
                            customColors = fieldColors
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // ── Send button ───────────────────────────────────
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
                                    .background(buttonGradient)
                            ) {
                                Button(
                                    onClick = { attemptReset() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Text(
                                        "Send Reset Link",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = onNavigateBack) {
                            Text(
                                "Back to Login",
                                color = Purple80,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Forgot Password — Input")
@Composable
fun ForgotPasswordPreview() {
    ShredzillaTheme(dynamicColor = false) {
        ForgotPasswordScreen(onSendResetEmail = {}, onNavigateBack = {})
    }
}

@Preview(showBackground = true, name = "Forgot Password — Sent")
@Composable
fun ForgotPasswordSentPreview() {
    ShredzillaTheme(dynamicColor = false) {
        ForgotPasswordScreen(onSendResetEmail = {}, onNavigateBack = {}, isEmailSent = true)
    }
}
