package com.FreeRave.shredzilla

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController // Import NavHostController
import androidx.navigation.compose.rememberNavController
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.auth.FirebaseGoogleAuth
import com.FreeRave.shredzilla.navigation.AppNavigationHost
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.utils.NotificationUtils // Import NotificationUtils
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import kotlinx.coroutines.launch

import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.objects.Update
import com.github.javiersantos.appupdater.enums.AppUpdaterError // Added for AppUpdater Listener



class MainActivity : ComponentActivity() {

    private val firebaseEmailAuthManager = FirebaseEmailPasswordAuth()
    private lateinit var firebaseGoogleAuthManager: FirebaseGoogleAuth
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private var navController: NavHostController? = null // Member variable for NavController

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, object : OnInitializationCompleteListener {
            override fun onInitializationComplete(initializationStatus: InitializationStatus) {
                Log.d(TAG, "MobileAds.initialize onInitializationComplete: $initializationStatus")
                // SDK is initialized. It's now safe to load ads.
                // You could potentially trigger a global flag or an event here
                // if other parts of the app need to know.
                // For now, RewardedAdManager will attempt to load ads when needed,
                // and this initialization ensures the SDK is ready.
            }
        })

        // Create Notification Channel
        NotificationUtils.createNotificationChannel(applicationContext)

        // Initialize App Updater
        AppUpdaterUtils(this)
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON("https://fitnessapp-9b198.web.app/update-info.json")
            .withListener(object : AppUpdaterUtils.UpdateListener {
                override fun onSuccess(update: Update, isUpdateAvailable: Boolean) {
                    Log.d("AppUpdater", "Check success. Is update available? $isUpdateAvailable")
                    if (isUpdateAvailable) {
                        // Show update UI
                        AppUpdater(this@MainActivity)
                            .setUpdateFrom(UpdateFrom.JSON)
                            .setUpdateJSON("https://fitnessapp-9b198.web.app/update-info.json")
                            .setDisplay(Display.DIALOG)
                            .setCancelable(false)
                            .start()
                    }
                }

                override fun onFailed(error: AppUpdaterError) {
                    Log.e("AppUpdater", "Error checking for update: $error")
                }
            })
            .start()


        firebaseGoogleAuthManager = FirebaseGoogleAuth(this)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "GoogleSignInLauncher result: resultCode=${result.resultCode}")
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val signInResult = firebaseGoogleAuthManager.handleGoogleSignInResult(result.data)
                    if (signInResult.isSuccess) {
                        val googleUser = signInResult.getOrNull()
                        if (googleUser != null) {
                             Log.d(TAG, "Google Sign-In success for user: ${googleUser.uid}, checking onboarding.")
                             checkOnboardingStatusAndNavigate(googleUser.uid)
                        } else {
                            Log.e(TAG, "Google Sign-In successful but FirebaseUser is null")
                            setContentToAppNavigation(AppRoutes.AUTH) // Fallback
                        }
                    } else {
                        Log.e(TAG, "Google Sign-In failed: ${signInResult.exceptionOrNull()?.message}")
                        Toast.makeText(this@MainActivity, "Google Sign-In failed: ${signInResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        setContentToAppNavigation(AppRoutes.AUTH) // Fallback
                    }
                }
            } else {
                Log.w(TAG, "Google Sign-In cancelled or failed by user.")
                Toast.makeText(this@MainActivity, "Google Sign-In cancelled or failed.", Toast.LENGTH_LONG).show()
                 // Potentially setContentToAppNavigation(AppRoutes.AUTH) here if needed after user cancels
            }
        }

        val currentUser = firebaseEmailAuthManager.getCurrentUser()
        if (currentUser != null) {
            Log.d(TAG, "Current user found: ${currentUser.uid}. Checking onboarding status.")
            checkOnboardingStatusAndNavigate(currentUser.uid)
        } else {
            Log.d(TAG, "No current user. Navigating to Auth flow.")
            setContentToAppNavigation(AppRoutes.AUTH)
        }
    }

    private fun checkOnboardingStatusAndNavigate(userId: String) {
        Log.d(TAG, "checkOnboardingStatusAndNavigate for user: $userId")
        lifecycleScope.launch {
            val userDataResult = firebaseEmailAuthManager.getUserData(userId)
            if (userDataResult.isSuccess) {
                val userData = userDataResult.getOrNull()
                ThemeManager.currentGenderTheme = userData?.get("gender") as? String
                Log.d(TAG, "Firestore userData for $userId: $userData. Gender theme set to: ${ThemeManager.currentGenderTheme}")

                val determinedStartDestination = if (userData == null) {
                    Log.d(TAG, "User $userId has no data in Firestore, starting ONBOARDING (gender_selection)")
                    AppRoutes.ONBOARDING
                } else if (userData.containsKey("weeklyGoalFrequency")) { // Check for weekly goal first (last step)
                    Log.d(TAG, "User $userId has weeklyGoalFrequency, navigating to FITNESS_MAIN")
                    AppRoutes.FITNESS_MAIN
                } else if (userData.containsKey("initialExercises")) {
                    Log.d(TAG, "User $userId has initialExercises, navigating to WEEKLY_GOAL_SELECTION")
                    AppRoutes.WEEKLY_GOAL_SELECTION
                } else if (userData.containsKey("restTimePreference")) {
                    Log.d(TAG, "User $userId has restTimePreference, navigating to INITIAL_EXERCISES_SELECTION")
                    AppRoutes.INITIAL_EXERCISES_SELECTION
                } else if (userData.containsKey("age") || userData.containsKey("height") || userData.containsKey("weight")) {
                    Log.d(TAG, "User $userId has physical details, navigating to REST_TIME_SELECTION")
                    AppRoutes.REST_TIME_SELECTION
                } else if (userData.containsKey("gender")) {
                    Log.d(TAG, "User $userId has gender, navigating to PHYSICAL_DETAILS")
                    AppRoutes.PHYSICAL_DETAILS
                } else {
                    Log.d(TAG, "User $userId has no specific onboarding data, starting ONBOARDING (gender_selection)")
                    AppRoutes.ONBOARDING
                }
                
                // Navigate using existing controller if available, else set initial content
                val currentNavController = this@MainActivity.navController
                if (currentNavController != null && currentNavController.currentBackStackEntry?.destination?.route != determinedStartDestination) {
                    Log.d(TAG, "Navigating with existing controller to: $determinedStartDestination")
                    currentNavController.navigate(determinedStartDestination) {
                        // Pop up to the start of the graph to clear back stack,
                        // effectively making the new destination the new root.
                        popUpTo(currentNavController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true // Avoid multiple copies of the same destination
                    }
                } else if (currentNavController == null) {
                    Log.d(TAG, "Setting initial content with start destination: $determinedStartDestination")
                    setContentToAppNavigation(determinedStartDestination)
                } else {
                    Log.d(TAG, "Already at destination or controller not ready for navigation: $determinedStartDestination")
                }

            } else {
                Log.e(TAG, "Error fetching user data for $userId: ${userDataResult.exceptionOrNull()?.message}")
                Toast.makeText(this@MainActivity, "Error fetching user data.", Toast.LENGTH_SHORT).show()
                // Navigate to Auth if controller exists, else set initial content to Auth
                val currentNavController = this@MainActivity.navController
                if (currentNavController != null) {
                     currentNavController.navigate(AppRoutes.AUTH) {
                        popUpTo(currentNavController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                     }
                } else {
                    setContentToAppNavigation(AppRoutes.AUTH)
                }
            }
        }
    }

    private fun setContentToAppNavigation(startDestinationRoute: String) {
        Log.d(TAG, "setContentToAppNavigation called with startDestination: $startDestinationRoute")
        setContent {
            val localNavController = rememberNavController()
            this.navController = localNavController // Assign to member variable
            AppNavigationHost(
                navController = localNavController,
                startDestination = startDestinationRoute,
                firebaseEmailAuthManager = firebaseEmailAuthManager,
                firebaseGoogleAuthManager = firebaseGoogleAuthManager,
                googleSignInLauncher = googleSignInLauncher,
                activityContext = this
            )
        }
    }
}
