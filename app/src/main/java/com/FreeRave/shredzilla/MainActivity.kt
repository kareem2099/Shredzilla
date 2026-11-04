package com.FreeRave.shredzilla

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
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
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val firebaseEmailAuthManager = FirebaseEmailPasswordAuth()
    private lateinit var firebaseGoogleAuthManager: FirebaseGoogleAuth

    // Removed navController as member variable — it was causing memory leaks
    // Instead we use state inside setContent directly
    private val mainViewModel: MainViewModel by viewModels()

    private val TAG = "MainActivity"
    private val EXTRA_DEBUG_ROUTE = "debugRoute"
    private val EXTRA_DEBUG_GENDER = "debugGender"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        // UX Polish: If the app was killed while the timer was running, the notification gets stuck.
        // Clearing it on fresh launch ensures it doesn't persist zombie-like.
        NotificationUtils.cancelTimerRunningNotification(this)

        // MobileAds.initialize removed from here — it's already in MyApplication

        NotificationUtils.createNotificationChannel(applicationContext)
        // AppUpdater checks internally so no need for AppUpdaterUtils
        AppUpdater(this)
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON("https://fitnessapp-9b198.web.app/update-info.json")
            .setDisplay(Display.DIALOG)
            .setCancelable(false)
            .start()

        firebaseGoogleAuthManager = FirebaseGoogleAuth(this)

        // setContent once only — monitors startDestination
        setContent {
            val navController = rememberNavController()
            val startDestination by mainViewModel.startDestinationFlow.collectAsState()

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
    private fun checkOnboardingAndSetDestination(userId: String) {
        lifecycleScope.launch {
            mainViewModel.resolveStartDestination(userId)
                .onSuccess { dest -> mainViewModel.updateStartDestination(dest) }
                .onFailure { e ->
                    Log.e(TAG, "Error fetching user data: ${e.message}")
                    if (!isNetworkAvailable(this@MainActivity)) {
                        // Offline with a saved session: let them train; Firestore syncs later
                        Toast.makeText(this@MainActivity, "Network error. Working offline.", Toast.LENGTH_SHORT).show()
                        mainViewModel.updateStartDestination(AppRoutes.FITNESS_MAIN)
                    } else {
                        // Don't keep a session we can't load; send them to a clean login
                        firebaseEmailAuthManager.signOut()
                        Toast.makeText(this@MainActivity, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                        mainViewModel.updateStartDestination(AppRoutes.AUTH)
                    }
                }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}