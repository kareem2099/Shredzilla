package com.FreeRave.shredzilla

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.auth.FirebaseGoogleAuth
import com.FreeRave.shredzilla.navigation.AppNavigationHost
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.ui.glass.GlassBackground
import com.FreeRave.shredzilla.ui.theme.ShredzillaTheme
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.ui.theme.glass
import com.FreeRave.shredzilla.utils.NotificationUtils
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import androidx.compose.foundation.layout.Box
import com.FreeRave.shredzilla.update.AppUpdateController
import com.FreeRave.shredzilla.update.AppUpdateOverlay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class MainActivity : ComponentActivity() {

    private val firebaseEmailAuthManager = FirebaseEmailPasswordAuth()
    private lateinit var firebaseGoogleAuthManager: FirebaseGoogleAuth

    // Removed navController as member variable — it was causing memory leaks
    // Instead we use state inside setContent directly
    private val mainViewModel: MainViewModel by viewModels()

    private val TAG = "MainActivity"
    private lateinit var appUpdateController: AppUpdateController
    private val EXTRA_DEBUG_ROUTE = "debugRoute"
    private val EXTRA_DEBUG_GENDER = "debugGender"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        // MobileAds.initialize removed from here — it's already in MyApplication

        NotificationUtils.createNotificationChannels(applicationContext)
        com.FreeRave.shredzilla.notifications.AppNotifications.createChannels(applicationContext)
        readNotificationRoute(intent)
        // Google Play updates plus the Remote Config minimum version; once per launch, not per rotation
        appUpdateController = AppUpdateController(this)
        if (savedInstanceState == null) appUpdateController.check()

        firebaseGoogleAuthManager = FirebaseGoogleAuth(this)

        // setContent once only — monitors startDestination
        setContent {
            val navController = rememberNavController()
            val startDestination by mainViewModel.startDestinationFlow.collectAsState()

            Box {
                if (startDestination != null) {
                    AppNavigationHost(
                        navController = navController,
                        startDestination = startDestination!!,
                        firebaseEmailAuthManager = firebaseEmailAuthManager,
                        firebaseGoogleAuthManager = firebaseGoogleAuthManager
                    )
                } else {
                    // Brief wait while we work out where to send the user; same backdrop as what follows
                    ShredzillaTheme {
                        GlassBackground(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.glass.accent)
                        }
                    }
                }
                // Above everything: a required update blocks the app, otherwise a prompt or download progress
                AppUpdateOverlay(appUpdateController)
            }
        }

        // Check current user
        val currentUser = firebaseEmailAuthManager.getCurrentUser()
        if (savedInstanceState == null) {
            val debugRoute = debugStartRoute()
            if (debugRoute != null) {
                Log.d(TAG, "Debug start route: $debugRoute")
                mainViewModel.updateStartDestination(debugRoute)
            } else if (currentUser != null) {
                Log.d(TAG, "Current user found: ${currentUser.uid}")
                checkOnboardingAndSetDestination(currentUser.uid)
            } else {
                Log.d(TAG, "No current user.")
                mainViewModel.updateStartDestination(AppRoutes.AUTH)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readNotificationRoute(intent)
    }

    /** A tapped reminder carries the screen to open; the main app picks it up. */
    private fun readNotificationRoute(intent: Intent?) {
        if (intent?.getBooleanExtra(com.FreeRave.shredzilla.workout.WorkoutNotifier.EXTRA_FINISH, false) == true) {
            com.FreeRave.shredzilla.notifications.PendingNavigation.finishWorkout = true
            intent.removeExtra(com.FreeRave.shredzilla.workout.WorkoutNotifier.EXTRA_FINISH)
        }
        val route = intent?.getStringExtra(com.FreeRave.shredzilla.notifications.AppNotifications.EXTRA_OPEN_ROUTE) ?: return
        com.FreeRave.shredzilla.notifications.PendingNavigation.route = route
        intent.removeExtra(com.FreeRave.shredzilla.notifications.AppNotifications.EXTRA_OPEN_ROUTE)
    }

    override fun onResume() {
        super.onResume()
        appUpdateController.onResume()
    }

    override fun onDestroy() {
        appUpdateController.release()
        super.onDestroy()
    }

    /**
     * Debug builds only: open a screen directly so UI normally hidden behind onboarding can be checked
     * without a fresh account, e.g.
     * `adb shell am start -n com.FreeRave.shredzilla/.MainActivity --es debugRoute onboarding_flow --es debugGender Male`
     */
    private fun debugStartRoute(): String? {
        if (!BuildConfig.DEBUG) return null
        intent.getStringExtra(EXTRA_DEBUG_GENDER)?.let { ThemeManager.currentGenderTheme = it }
        return intent.getStringExtra(EXTRA_DEBUG_ROUTE)
    }

    // This function determines destination only — doesn't navigate directly
    /**
     * A saved sign-in always opens the app: Firebase Auth keeps it on the device, and the start
     * screen comes from a local copy or Firestore's cache. A profile that can't be read (offline,
     * gym Wi-Fi without internet, a slow server) never signs anyone out; the app opens and syncs
     * later. Only an account Firebase reports as deleted or disabled is signed out.
     */
    private fun checkOnboardingAndSetDestination(userId: String) {
        lifecycleScope.launch {
            mainViewModel.resolveStartDestination(userId)
                .onSuccess { dest -> mainViewModel.updateStartDestination(dest) }
                .onFailure { e ->
                    Log.w(TAG, "Profile not readable yet, opening the app: ${e.message}")
                    mainViewModel.updateStartDestination(AppRoutes.FITNESS_MAIN)
                }
            verifyAccountStillExists()
        }
    }

    /** Refreshes the sign-in in the background; network errors are ignored, a removed account signs out. */
    private suspend fun verifyAccountStillExists() {
        val user = firebaseEmailAuthManager.getCurrentUser() ?: return
        try {
            user.reload().await()
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w(TAG, "Account was deleted or disabled; signing out", e)
            firebaseEmailAuthManager.signOut()
            mainViewModel.clearSession()
            Toast.makeText(this, "This account is no longer available. Please sign in again.", Toast.LENGTH_LONG).show()
            mainViewModel.updateStartDestination(AppRoutes.AUTH)
        } catch (e: Exception) {
            Log.d(TAG, "Couldn't refresh the sign-in right now (offline?): ${e.message}")
        }
    }
}