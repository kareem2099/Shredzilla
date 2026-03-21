package com.FreeRave.shredzilla

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.FreeRave.shredzilla.auth.FirebaseEmailPasswordAuth
import com.FreeRave.shredzilla.auth.FirebaseGoogleAuth
import com.FreeRave.shredzilla.navigation.AppNavigationHost
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.utils.NotificationUtils
import com.FreeRave.shredzilla.viewmodels.MainViewModel
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val firebaseEmailAuthManager = FirebaseEmailPasswordAuth()
    private lateinit var firebaseGoogleAuthManager: FirebaseGoogleAuth
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // ✅ إزلنا navController كـ member variable — كان بيسبب memory leak
    // بدلاً منه بنستخدم state داخل setContent مباشرةً
    private val mainViewModel: MainViewModel by viewModels()

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        // ✅ MobileAds.initialize اتحذفت من هنا — موجودة في MyApplication بالفعل

        NotificationUtils.createNotificationChannel(applicationContext)
        // ✅ AppUpdater checks internally so no need for AppUpdaterUtils
        AppUpdater(this)
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON("https://fitnessapp-9b198.web.app/update-info.json")
            .setDisplay(Display.DIALOG)
            .setCancelable(false)
            .start()

        firebaseGoogleAuthManager = FirebaseGoogleAuth(this)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "GoogleSignInLauncher result: resultCode=${result.resultCode}")
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    val signInResult = firebaseGoogleAuthManager.handleGoogleSignInResult(result.data)
                    if (signInResult.isSuccess) {
                        val googleUser = signInResult.getOrNull()
                        if (googleUser != null) {
                            Log.d(TAG, "Google Sign-In success: ${googleUser.uid}")
                            checkOnboardingAndSetDestination(googleUser.uid)
                        } else {
                            Log.e(TAG, "Google Sign-In successful but FirebaseUser is null")
                            mainViewModel.updateStartDestination(AppRoutes.AUTH)
                        }
                    } else {
                        Log.e(TAG, "Google Sign-In failed: ${signInResult.exceptionOrNull()?.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "Google Sign-In failed: ${signInResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        mainViewModel.updateStartDestination(AppRoutes.AUTH)
                    }
                }
            } else {
                Log.w(TAG, "Google Sign-In cancelled.")
                Toast.makeText(this@MainActivity, "Google Sign-In cancelled.", Toast.LENGTH_LONG).show()
            }
        }

        // ✅ setContent مرة واحدة بس — بيراقب startDestination
        setContent {
            val navController = rememberNavController()
            val startDestination by mainViewModel.startDestinationFlow.collectAsState()

            // لما startDestination يتغير → navigate
            LaunchedEffect(startDestination) {
                val dest = startDestination ?: return@LaunchedEffect
                navController.navigate(dest) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }

            // ✅ نبدأ بـ AUTH دايماً — والـ LaunchedEffect هيعمل redirect لو لزم
            AppNavigationHost(
                navController = navController,
                startDestination = AppRoutes.AUTH,
                firebaseEmailAuthManager = firebaseEmailAuthManager,
                firebaseGoogleAuthManager = firebaseGoogleAuthManager,
                googleSignInLauncher = googleSignInLauncher
            )
        }

        // تشيك على اليوزر الحالي
        val currentUser = firebaseEmailAuthManager.getCurrentUser()
        if (savedInstanceState == null) {
            if (currentUser != null) {
                Log.d(TAG, "Current user found: ${currentUser.uid}")
                lifecycleScope.launch {
                    checkOnboardingAndSetDestination(currentUser.uid)
                }
            } else {
                Log.d(TAG, "No current user.")
                mainViewModel.updateStartDestination(AppRoutes.AUTH)
            }
        }
    }

    // ✅ الدالة دي بتحدد الـ destination بس — مش بتعمل navigate مباشرةً
    private suspend fun checkOnboardingAndSetDestination(userId: String) {
        Log.d(TAG, "checkOnboardingAndSetDestination for: $userId")
        val userDataResult = firebaseEmailAuthManager.getUserData(userId)

        if (userDataResult.isSuccess) {
            val userData = userDataResult.getOrNull()
            ThemeManager.currentGenderTheme = userData?.get("gender") as? String

            val dest = when {
                userData == null -> AppRoutes.ONBOARDING
                userData.containsKey("weeklyGoalFrequency") -> AppRoutes.FITNESS_MAIN
                userData.containsKey("initialExercises") -> AppRoutes.WEEKLY_GOAL_SELECTION
                userData.containsKey("restTimePreference") -> AppRoutes.INITIAL_EXERCISES_SELECTION
                userData.containsKey("age") || userData.containsKey("height") || userData.containsKey("weight") -> AppRoutes.REST_TIME_SELECTION
                userData.containsKey("gender") -> AppRoutes.PHYSICAL_DETAILS
                else -> AppRoutes.ONBOARDING
            }
            mainViewModel.updateStartDestination(dest)
            Log.d(TAG, "Destination set to: $dest")
        } else {
            Log.e(TAG, "Error fetching user data: ${userDataResult.exceptionOrNull()?.message}")
            if (!isNetworkAvailable(this)) {
                Toast.makeText(this, "Network error. Working offline.", Toast.LENGTH_SHORT).show()
                mainViewModel.updateStartDestination(AppRoutes.FITNESS_MAIN)
            } else {
                Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                mainViewModel.updateStartDestination(AppRoutes.AUTH)
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