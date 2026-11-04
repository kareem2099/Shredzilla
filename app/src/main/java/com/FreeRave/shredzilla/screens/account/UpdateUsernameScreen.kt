package com.FreeRave.shredzilla.screens.account

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.FreeRave.shredzilla.auth.composables.TextFormField
import com.FreeRave.shredzilla.ui.glass.GlassCard
import com.FreeRave.shredzilla.ui.glass.GlassFootnote
import com.FreeRave.shredzilla.ui.glass.GlassPrimaryButton
import com.FreeRave.shredzilla.ui.glass.GlassSubpageScaffold
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

@Composable
fun UpdateUsernameScreen(
    onNavigateBack: () -> Unit,
    currentUsername: String?,
    isUpdatingUsername: Boolean,
    onUpdateUsername: (String, () -> Unit, (Exception) -> Unit) -> Unit
) {
    var username by rememberSaveable { mutableStateOf(currentUsername.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val unchanged = username.trim() == currentUsername.orEmpty().trim()

    fun save() {
        val name = username.trim()
        if (name.isEmpty()) {
            error = "Name can't be empty"
            return
        }
        onUpdateUsername(
            name,
            {
                Toast.makeText(context, "Name updated.", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            },
            { e -> Toast.makeText(context, "Couldn't update name: ${e.message}", Toast.LENGTH_LONG).show() }
        )
    }

    GlassSubpageScaffold(
        title = "Your name",
        eyebrow = "Account",
        onBack = onNavigateBack,
        bottomBar = {
            GlassPrimaryButton(text = "Save", onClick = ::save, enabled = !unchanged, loading = isUpdatingUsername)
        }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            TextFormField(
                value = username,
                onValueChange = { username = it; error = null },
                label = "Name",
                enabled = !isUpdatingUsername,
                imeAction = ImeAction.Done,
                isError = error != null,
                errorMessage = error,
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                keyboardActions = KeyboardActions(onDone = { if (!unchanged && !isUpdatingUsername) save() })
            )
        }
        GlassFootnote("This is the name we greet you with on the Sets tab.")
    }
}

@Preview
@Composable
private fun UpdateUsernameScreenPreview() {
    ShredzillaTheme {
        UpdateUsernameScreen(onNavigateBack = {}, currentUsername = "Alex", isUpdatingUsername = false, onUpdateUsername = { _, _, _ -> })
    }
}
