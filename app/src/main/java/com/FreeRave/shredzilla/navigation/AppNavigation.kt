package com.FreeRave.shredzilla.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.FreeRave.shredzilla.auth.*
import com.FreeRave.shredzilla.onboarding.*
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import com.FreeRave.shredzilla.viewmodels.MainViewModel

private val onboardingSteps = setOf(
    AppRoutes.GENDER_SELECTION,
    AppRoutes.PHYSICAL_DETAILS,
    AppRoutes.REST_TIME_SELECTION,
    AppRoutes.INITIAL_EXERCISES_SELECTION,
    AppRoutes.WEEKLY_GOAL_SELECTION
)

@Composable
fun AppNavigationHost(
    navController: NavHostController,
    startDestination: String,
    firebaseEmailAuthManager: FirebaseEmailPasswordAuth,
    firebaseGoogleAuthManager: FirebaseGoogleAuth,
    mainViewModel: MainViewModel = viewModel()
) {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Email and Google sign-in finish the same way: find where the user belongs, then go there.
    // The spinner stays up until routing is done, and a session we can't load is signed out again
    // so the user isn't left signed in on the login screen.
    fun signIn(silentFailure: (Throwable) -> Boolean = { false }, attempt: suspend () -> Result<FirebaseUser>) {
        if (isLoading) return // double-tap guard
        isLoading = true
        mainViewModel.viewModelScope.launch {
            val destination = attempt().mapCatching { user -> mainViewModel.resolveStartDestination(user.uid).getOrThrow() }
            destination
                .onSuccess { dest ->
                    navController.navigate(dest) {
                        popUpTo(AppRoutes.AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                .onFailure { e ->
                    if (firebaseEmailAuthManager.getCurrentUser() != null) firebaseGoogleAuthManager.signOut()
                    if (!silentFailure(e)) Toast.makeText(context, AuthErrorMapper.getMessage(e), Toast.LENGTH_LONG).show()
                }
            isLoading = false
        }
    }

    // Every onboarding step saves its answers the same way: guard double taps, write, then advance.
    // On failure the user stays on the step so they can retry.
    fun saveOnboardingStep(data: Map<String, Any>, onSaved: () -> Unit) {
        val userId = firebaseEmailAuthManager.getCurrentUser()?.uid ?: return
        if (isLoading) return
        isLoading = true
        mainViewModel.viewModelScope.launch {
            val result = firebaseEmailAuthManager.updateUserOnboardingData(userId, data)
            isLoading = false
            if (result.isSuccess) onSaved()
            else Toast.makeText(context, AuthErrorMapper.getMessage(result.exceptionOrNull()), Toast.LENGTH_SHORT).show()
        }
    }

    // ShredzillaTheme no longer takes genderTheme or darkTheme directly
    ShredzillaTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // NavHost can only start on a direct child, so resuming onboarding mid-way (e.g. after the app
            // was closed on step 3) starts the onboarding graph itself at that step
            val resumedOnboardingStep = startDestination.takeIf { it in onboardingSteps }
            NavHost(
                navController = navController,
                startDestination = if (resumedOnboardingStep != null) AppRoutes.ONBOARDING else startDestination
            ) {
                navigation(startDestination = AppRoutes.LOGIN, route = AppRoutes.AUTH) {
                    composable(AppRoutes.LOGIN) {
                        LoginScreen(
                            onLoginClick = { email, password ->
                                signIn { firebaseEmailAuthManager.signInUser(email, password) }
                            },
                            // Cancelling the Google sheet isn't an error worth a toast
                            onGoogleSignInClick = { signIn(silentFailure = { it.message?.contains("cancelled", true) == true }) { firebaseGoogleAuthManager.signInWithGoogle() } },
                            onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER) },
                            onNavigateToForgotPassword = { navController.navigate(AppRoutes.FORGOT_PASSWORD) },
                            isLoading = isLoading
                        )
                    }
                    composable(AppRoutes.REGISTER) {
                        RegisterScreen(
                            onRegisterClick = { name, email, password, _ ->
                                if (isLoading) return@RegisterScreen  // double-tap guard
                                isLoading = true                      // set synchronously before launch
                                mainViewModel.viewModelScope.launch {
                                    val result = firebaseEmailAuthManager.createUser(name, email, password)
                                    if (result.isSuccess) {
                                        ThemeManager.currentGenderTheme = null
                                        navController.navigate(AppRoutes.ONBOARDING) { popUpTo(AppRoutes.AUTH) { inclusive = true } }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            AuthErrorMapper.getMessage(result.exceptionOrNull()),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() },
                            isLoading = isLoading
                        )
                    }
                    composable(AppRoutes.FORGOT_PASSWORD) {
                        var isEmailSent by rememberSaveable { mutableStateOf(false) }
                        ForgotPasswordScreen(
                            onSendResetEmail = { email ->
                                if (isLoading) return@ForgotPasswordScreen  // double-tap guard
                                isLoading = true                             // set synchronously before launch
                                mainViewModel.viewModelScope.launch {
                                    val result = firebaseEmailAuthManager.sendPasswordResetEmail(email)
                                    if (result.isSuccess) {
                                        isEmailSent = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            AuthErrorMapper.getMessage(result.exceptionOrNull()),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    isLoading = false
                                }
                            },
                            onNavigateBack = { navController.popBackStack() },
                            isLoading = isLoading,
                            isEmailSent = isEmailSent
                        )
                    }
                } // end auth navigation

                navigation(startDestination = resumedOnboardingStep ?: AppRoutes.GENDER_SELECTION, route = AppRoutes.ONBOARDING) {
                    composable(AppRoutes.GENDER_SELECTION) {
                        GenderSelectionScreen(
                            isLoading = isLoading,
                            onGenderSelected = { gender ->
                                ThemeManager.currentGenderTheme = gender
                                saveOnboardingStep(mapOf("gender" to gender)) {
                                    navController.navigate(AppRoutes.PHYSICAL_DETAILS)
                                }
                            }
                        )
                    }
                    composable(AppRoutes.PHYSICAL_DETAILS) {
                        PhysicalDetailsScreen(
                            isLoading = isLoading,
                            onBack = { navController.popBackStack() },
                            onDetailsSubmitted = { age, heightCm, weightKg ->
                                // Height/weight have always been stored as doubles
                                val details = mapOf("age" to age, "height" to heightCm.toDouble(), "weight" to weightKg.toDouble())
                                saveOnboardingStep(details) { navController.navigate(AppRoutes.REST_TIME_SELECTION) }
                            }
                        )
                    }
                    composable(AppRoutes.REST_TIME_SELECTION) {
                        RestTimeScreen(
                            isLoading = isLoading,
                            onBack = { navController.popBackStack() },
                            onRestTimeSelected = { restTime ->
                                saveOnboardingStep(mapOf("restTimePreference" to restTime)) {
                                    navController.navigate(AppRoutes.INITIAL_EXERCISES_SELECTION)
                                }
                            }
                        )
                    }
                    composable(AppRoutes.INITIAL_EXERCISES_SELECTION) {
                        InitialExercisesScreen(
                            isLoading = isLoading,
                            onBack = { navController.popBackStack() },
                            onExercisesSelected = { exerciseIds ->
                                saveOnboardingStep(mapOf("initialExercises" to exerciseIds)) {
                                    navController.navigate(AppRoutes.WEEKLY_GOAL_SELECTION)
                                }
                            }
                        )
                    }
                    composable(AppRoutes.WEEKLY_GOAL_SELECTION) {
                        WeeklyGoalScreen(
                            isLoading = isLoading,
                            onBack = { navController.popBackStack() },
                            onWeeklyGoalSelected = { frequency, nudgeDays ->
                                saveOnboardingStep(mapOf("weeklyGoalFrequency" to frequency, "nudgeDays" to nudgeDays)) {
                                    navController.navigate(AppRoutes.FITNESS_MAIN) {
                                        popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }

                composable(AppRoutes.FITNESS_MAIN) {
                    MainAppContainer(
                        mainNavController = navController,
                        firebaseEmailAuthManager = firebaseEmailAuthManager, // Explicitly matching param name
                        firebaseGoogleAuthManager = firebaseGoogleAuthManager // Explicitly matching param name
                    )
                }
            }  // end NavHost
        }
    }
}

// MainAppScaffold has been moved to MainAppContainer.kt
// Ensure MainAppContainer is imported and used in AppNavigationHost where MainAppScaffold was.
