package com.FreeRave.shredzilla.auth.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.FreeRave.shredzilla.screens.settings.ThemeSetting
// Import specific purple colors
import com.FreeRave.shredzilla.ui.theme.Purple40
import com.FreeRave.shredzilla.ui.theme.Purple80
import com.FreeRave.shredzilla.ui.theme.PurpleGrey40
import com.FreeRave.shredzilla.ui.theme.PurpleGrey80
import com.FreeRave.shredzilla.ui.theme.Pink40 // For error states in light mode
import com.FreeRave.shredzilla.ui.theme.Pink80 // For error states in dark mode
import com.FreeRave.shredzilla.ui.theme.ThemeManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null // New parameter for leading icon
) {
    // Determine active theme
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

    // val GreenColor = TextFieldGreen // Custom green is no longer used

    val finalTextFieldColors = if (useDarkTheme) {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Purple80,
            unfocusedTextColor = Purple80,
            focusedLabelColor = Purple80.copy(alpha = 0.75f),
            unfocusedLabelColor = Purple80.copy(alpha = 0.5f),
            cursorColor = Purple80,
            focusedBorderColor = Purple80,
            unfocusedBorderColor = Purple80.copy(alpha = 0.5f),
            disabledTextColor = Purple80.copy(alpha = 0.5f),
            disabledLabelColor = Purple80.copy(alpha = 0.38f),
            disabledBorderColor = Purple80.copy(alpha = 0.38f),
            errorTextColor = Pink80, // Using Pink for errors in purple theme
            errorLabelColor = Pink80,
            errorBorderColor = Pink80,
            errorCursorColor = Pink80,
            focusedSupportingTextColor = Pink80.copy(alpha = 0.75f),
            unfocusedSupportingTextColor = Pink80.copy(alpha = 0.5f),
            focusedContainerColor = PurpleGrey40.copy(alpha = 0.7f), 
            unfocusedContainerColor = PurpleGrey40.copy(alpha = 0.7f),
            disabledContainerColor = PurpleGrey40.copy(alpha = 0.5f),
            errorContainerColor = PurpleGrey40.copy(alpha = 0.7f) 
        )
    } else { // Light Mode
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Purple40,
            unfocusedTextColor = Purple40,
            focusedLabelColor = Purple40.copy(alpha = 0.75f),
            unfocusedLabelColor = Purple40.copy(alpha = 0.5f),
            cursorColor = Purple40,
            focusedBorderColor = Purple40,
            unfocusedBorderColor = Purple40.copy(alpha = 0.5f),
            disabledTextColor = Purple40.copy(alpha = 0.5f),
            disabledLabelColor = Purple40.copy(alpha = 0.38f),
            disabledBorderColor = Purple40.copy(alpha = 0.38f),
            errorTextColor = Pink40, // Using Pink for errors in purple theme
            errorLabelColor = Pink40,
            errorBorderColor = Pink40,
            errorCursorColor = Pink40,
            focusedSupportingTextColor = Pink40.copy(alpha = 0.75f),
            unfocusedSupportingTextColor = Pink40.copy(alpha = 0.5f),
            focusedContainerColor = PurpleGrey80.copy(alpha = 0.7f), 
            unfocusedContainerColor = PurpleGrey80.copy(alpha = 0.7f),
            disabledContainerColor = PurpleGrey80.copy(alpha = 0.5f),
            errorContainerColor = PurpleGrey80.copy(alpha = 0.7f) 
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = leadingIcon, // Pass leadingIcon to OutlinedTextField
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        isError = isError,
        supportingText = {
            if (isError && errorMessage != null) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error) // Ensure error message uses error color
            }
        },
        colors = finalTextFieldColors // Apply the determined colors
    )
}
