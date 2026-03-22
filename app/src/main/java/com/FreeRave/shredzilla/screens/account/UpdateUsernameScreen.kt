package com.FreeRave.shredzilla.screens.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateUsernameScreen(
    onNavigateBack: () -> Unit,
    currentUsername: String?,
    isUpdatingUsername: Boolean,
    onUpdateUsername: (String, () -> Unit, (Exception) -> Unit) -> Unit
) {
    var username by rememberSaveable { mutableStateOf(currentUsername ?: "") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Username") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (username.isBlank()) {
                        Toast.makeText(context, "Username cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onUpdateUsername(
                        username,
                        { // onSuccess
                            Toast.makeText(context, "Username updated successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        { e -> // onError
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdatingUsername
            ) {
                if (isUpdatingUsername) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Username")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateUsernameScreenPreview() {
    ShredzillaTheme {
        UpdateUsernameScreen(onNavigateBack = {}, currentUsername = "Old Username", isUpdatingUsername = false, onUpdateUsername = { _, _, _ -> })
    }
}
