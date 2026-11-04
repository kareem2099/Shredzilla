package com.FreeRave.shredzilla.viewmodels

import com.FreeRave.shredzilla.models.LoggedSet
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.data.UserDataManager
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.FreeRave.shredzilla.utils.ImageStorageUtils
import com.FreeRave.shredzilla.navigation.AppRoutes
import com.FreeRave.shredzilla.auth.UserFirestoreHelper
import com.FreeRave.shredzilla.ui.theme.ThemeManager
import com.FreeRave.shredzilla.utils.TimerManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.SetOptions
import com.FreeRave.shredzilla.utils.dateKeysBetween

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
        private const val HISTORY_DAYS = 90
        private const val RECENT_LOGIN_WINDOW_MS = 5 * 60 * 1000L // Firebase's own limit for sensitive actions
    }

    // Process Death secure destination state
    val startDestinationFlow = savedStateHandle.getStateFlow<String?>("startDestination", null)
    
    fun updateStartDestination(dest: String?) {
        savedStateHandle["startDestination"] = dest
    }

    /**
     * Where a signed-in user should land: the first unfinished onboarding step, or the main app.
     * Also applies their gender theme. Fails if the user document can't be read.
     */
    suspend fun resolveStartDestination(userId: String): Result<String> =
        UserFirestoreHelper.getUserData(userId).map { userData ->
            ThemeManager.currentGenderTheme = userData?.get("gender") as? String
            when {
                userData == null -> AppRoutes.ONBOARDING
                userData.containsKey("weeklyGoalFrequency") -> AppRoutes.FITNESS_MAIN
                userData.containsKey("initialExercises") -> AppRoutes.WEEKLY_GOAL_SELECTION
                userData.containsKey("restTimePreference") -> AppRoutes.INITIAL_EXERCISES_SELECTION
                userData.containsKey("age") || userData.containsKey("height") || userData.containsKey("weight") -> AppRoutes.REST_TIME_SELECTION
                userData.containsKey("gender") -> AppRoutes.PHYSICAL_DETAILS
                else -> AppRoutes.ONBOARDING
            }
        }

    /** Forget the signed-out user's cached data so the next account never sees it. */
    fun clearSession() {
        _analyticsGraphData.value = emptyMap()
        _exerciseHistory.value = emptyMap()
        lastAnalyticsLoadTimeMillis = 0
        userDataManager.resetForSignOut()
        ThemeManager.currentGenderTheme = null
    }

    val userDataManager = UserDataManager(
        context = application.applicationContext,
        onThemePreferenceChanged = { malePref, femalePref ->
            ThemeManager.updateThemePreferenceForGender("Male", malePref)
            ThemeManager.updateThemePreferenceForGender("Female", femalePref)
        }
    )

    val timerManager = TimerManager(
        context = application.applicationContext,
        coroutineScope = viewModelScope,
        userRestTimePreferenceProvider = { userDataManager.userRestTimePreference }
    )

    // State Hoisting: Search Query & Debouncing for Exercises
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _debouncedSearchQuery = MutableStateFlow("")
    val debouncedSearchQuery = _debouncedSearchQuery.asStateFlow()

    init {
        _searchQuery
            .debounce(300L)
            .onEach { _debouncedSearchQuery.value = it }
            .launchIn(viewModelScope)
    }

    data class ChartDataPoint(val timestampDate: Long, val totalVolume: Double)

    private val _analyticsGraphData = MutableStateFlow<Map<String, List<ChartDataPoint>>>(emptyMap())
    val analyticsGraphData = _analyticsGraphData.asStateFlow()

    /** Every set from the last 90 days, keyed by exercise id, newest first. Feeds the exercise detail page. */
    private val _exerciseHistory = MutableStateFlow<Map<String, List<LoggedSet>>>(emptyMap())
    val exerciseHistory = _exerciseHistory.asStateFlow()

    private val _isAnalyticsLoading = MutableStateFlow(false)
    val isAnalyticsLoading = _isAnalyticsLoading.asStateFlow()

    private var lastAnalyticsLoadTimeMillis: Long = 0

    /** [force] skips the cache, e.g. right after a set was added, edited or deleted. */
    fun loadHistoricalAnalytics(userId: String, force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        val isCacheExpired = currentTime - lastAnalyticsLoadTimeMillis > CACHE_EXPIRY_MS
        if (!force && !isCacheExpired && (_analyticsGraphData.value.isNotEmpty() || _isAnalyticsLoading.value)) return // Cached and valid!

        _isAnalyticsLoading.value = true
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                
                val daysRef = db.collection("users").document(userId).collection("dailyActivity")
                // Day documents were never written before (sets go straight into their subcollection),
                // and Firestore can't list documents that don't exist. So ask for each of the last
                // 90 days by key instead of listing the collection.
                val now = System.currentTimeMillis()
                val dayKeys = dateKeysBetween(now - TimeUnit.DAYS.toMillis(HISTORY_DAYS - 1L), now)

                val tasks = dayKeys.map { dateKey ->
                    this@launch.async(Dispatchers.IO) {
                        try {
                            val dayRef = daysRef.document(dateKey)
                            val setsSnapshot = dayRef.collection("recordedSets").get().await()
                            // Backfill the day document so the day is listable from now on (e.g. for account deletion)
                            if (!setsSnapshot.isEmpty) dayRef.set(mapOf("date" to dateKey), SetOptions.merge())
                            setsSnapshot.documents.mapNotNull { setDoc ->
                                LoggedSet(
                                    docId = setDoc.id,
                                    dateKey = dateKey,
                                    exerciseId = setDoc.getString("exerciseId") ?: return@mapNotNull null,
                                    reps = setDoc.getLong("reps")?.toInt() ?: 0,
                                    weightKg = setDoc.getDouble("weight") ?: 0.0,
                                    notes = setDoc.getString("notes").orEmpty(),
                                    timestampMillis = (setDoc.getTimestamp("timestamp")?.seconds ?: 0L) * 1000L
                                )
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                val allSets = tasks.awaitAll().flatten()

                // 3. Mathematical Agression: Process timeline grouping on Default dispatcher
                val chartPoints = withContext(Dispatchers.Default) {
                    val groupedByExercise = allSets.groupBy { it.exerciseId }
                    
                    val result = mutableMapOf<String, List<ChartDataPoint>>()
                    groupedByExercise.forEach { (exId, sets) ->
                        val dailyAggregated = sets.groupBy { 
                            val cal = Calendar.getInstance().apply { timeInMillis = it.timestampMillis }
                            cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                            cal.timeInMillis
                        }.map { (dayStartMillis, daySets) ->
                            ChartDataPoint(
                                timestampDate = dayStartMillis,
                                totalVolume = daySets.sumOf { it.reps * it.weightKg }
                            )
                        }.sortedBy { it.timestampDate }
                        result[exId] = dailyAggregated
                    }
                    result
                }

                _analyticsGraphData.value = chartPoints
                _exerciseHistory.value = allSets.sortedByDescending { it.timestampMillis }.groupBy { it.exerciseId }
                lastAnalyticsLoadTimeMillis = System.currentTimeMillis()
            } catch (e: Exception) {
                // Handle silently
            } finally {
                _isAnalyticsLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    var isUpdatingUsername by mutableStateOf(false)
        private set

    fun updateUsername(
        newName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (newName.isBlank()) {
            onError(IllegalArgumentException("Username cannot be empty."))
            return
        }
        isUpdatingUsername = true
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    
                    userDataManager.updateUserSetting(user.uid, "name", newName)
                    onSuccess()
                } else {
                    onError(Exception("Not logged in."))
                }
            } catch (e: Exception) {
                onError(e)
            } finally {
                isUpdatingUsername = false
            }
        }
    }

    var isDeletingAccount by mutableStateOf(false)
        private set

    fun deleteUserAccount(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            isDeletingAccount = true
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Firebase refuses to delete an account without a recent sign-in. Check that up front,
                    // otherwise the data would be wiped while the account itself survives.
                    val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
                    if (System.currentTimeMillis() - lastSignIn > RECENT_LOGIN_WINDOW_MS) {
                        throw FirebaseAuthRecentLoginRequiredException(
                            "ERROR_REQUIRES_RECENT_LOGIN",
                            "Please sign out and sign back in, then delete your account."
                        )
                    }

                    userDataManager.userProfileImageLocalPath?.let(ImageStorageUtils::deleteImageFromInternalStorage)
                    userDataManager.deleteAllUserData(user.uid, accountCreatedMillis = user.metadata?.creationTimestamp ?: lastSignIn)
                    user.delete().await()

                    onSuccess()
                } else {
                    onError(Exception("Not logged in."))
                }
            } catch (e: Exception) {
                onError(e)
            } finally {
                isDeletingAccount = false
            }
        }
    }
}
