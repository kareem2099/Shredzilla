package com.FreeRave.shredzilla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.auth.FirebaseGoogleAuth
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import androidx.navigation.NavHostController
import androidx.activity.ComponentActivity


// This file now only contains the Composable UI for the fitness screen.
// The Activity part is removed as MainActivity handles navigation.

@Composable
fun FitnessAppScreen(modifier: Modifier = Modifier, onSignOut: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fitness App",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground // Ensure text color adapts
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FitnessAppScreenPreview() {
    ShredzillaTheme { // Preview with default theme
        FitnessAppScreen {}
    }
}

@Preview(showBackground = true)
@Composable
fun FitnessAppScreenMaleDarkPreview() {
    // To preview specific gender/dark mode combinations, 
    // you might need to temporarily set ThemeManager properties here for the preview,
    // or create a more complex preview setup.
    // For now, just calling ShredzillaTheme which will use defaults or system settings.
    ThemeManager.currentGenderTheme = "Male" // Temporary for preview
    // ThemeManager.themePreferenceMale = ThemeSetting.DARK // If you want to force dark for this preview
    ShredzillaTheme {
        FitnessAppScreen {}
    }
}

@Preview(showBackground = true)
@Composable
fun FitnessAppScreenFemaleDarkPreview() {
    ThemeManager.currentGenderTheme = "Female" // Temporary for preview
    // ThemeManager.themePreferenceFemale = ThemeSetting.DARK // If you want to force dark for this preview
    ShredzillaTheme {
        FitnessAppScreen {}
    }
}

@Composable
fun FitnessActivityContent(
    activityContext: ComponentActivity,
    firebaseEmailAuthManager: FirebaseEmailPasswordAuth,
    firebaseGoogleAuthManager: FirebaseGoogleAuth,
    navController: NavHostController
) {
    FitnessAppScreen {
        firebaseEmailAuthManager.signOut()
        firebaseGoogleAuthManager.signOut() // Also sign out from Google
        ThemeManager.currentGenderTheme = null
        navController.navigate(AppRoutes.AUTH) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true } // Clears entire back stack
            launchSingleTop = true
        }
    }
}
