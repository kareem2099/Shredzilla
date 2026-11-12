package com.FreeRave.shredzilla.viewmodels

import com.FreeRave.shredzilla.models.LoggedSet
import com.FreeRave.shredzilla.models.Side
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.data.SetDocFields
import com.FreeRave.shredzilla.data.TrainingHistoryStore
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.DocumentSnapshot
import com.FreeRave.shredzilla.utils.dateKeysBetween
import com.FreeRave.shredzilla.utils.startOfDay

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

    val sessionStore = com.FreeRave.shredzilla.auth.SessionStore(application)

    /**
     * Where a signed-in user should land: the first unfinished onboarding step, or the main app.
     * Also applies their gender theme.
     *
     * Never needs the network for a returning user: once an account finished onboarding on this
     * device the answer is kept locally. Otherwise the profile comes from Firestore's cache first,
     * then the server. Fails only when neither answers; callers must not sign out because of that.
     */
    suspend fun resolveStartDestination(userId: String): Result<String> {
        if (sessionStore.isOnboarded(userId)) {
            ThemeManager.currentGenderTheme = sessionStore.gender(userId)
            return Result.success(AppRoutes.FITNESS_MAIN)
        }
        return UserFirestoreHelper.getUserData(userId).map { userData ->
            val gender = userData?.get("gender") as? String
            ThemeManager.currentGenderTheme = gender
            val destination = when {
                userData == null -> AppRoutes.ONBOARDING
                userData.containsKey("weeklyGoalFrequency") -> AppRoutes.FITNESS_MAIN
                userData.containsKey("initialExercises") -> AppRoutes.WEEKLY_GOAL_SELECTION
                userData.containsKey("restTimePreference") -> AppRoutes.INITIAL_EXERCISES_SELECTION
                userData.containsKey("age") || userData.containsKey("height") || userData.containsKey("weight") -> AppRoutes.REST_TIME_SELECTION
                userData.containsKey("gender") -> AppRoutes.PHYSICAL_DETAILS
                else -> AppRoutes.ONBOARDING
            }
            sessionStore.remember(userId, onboarded = destination == AppRoutes.FITNESS_MAIN, gender = gender)
            destination
        }
    }

    /** Keeps the local copy of the profile in step after onboarding finishes or the gender changes. */
    fun rememberOnboarded(userId: String, gender: String?) = sessionStore.remember(userId, onboarded = true, gender = gender)

    /** Forget the signed-out user's cached data so the next account never sees it. */
    fun clearSession() {
        _analyticsGraphData.value = emptyMap()
        _exerciseHistory.value = emptyMap()
        lastAnalyticsLoadTimeMillis = 0
        userDataManager.resetForSignOut()
        trainingHistory.reset()
        ThemeManager.currentGenderTheme = null
        ThemeManager.accentTheme = null
        workout.clear()
        sessionStore.clear()
        // The next account must not get this one's reminders
        com.FreeRave.shredzilla.notifications.ReminderStore(getApplication()).clear()
        com.FreeRave.shredzilla.notifications.ReminderScheduler.cancelAll(getApplication())
    }

    val userDataManager = UserDataManager(
        context = application.applicationContext,
        onThemePreferenceChanged = { malePref, femalePref ->
            ThemeManager.updateThemePreferenceForGender("Male", malePref)
            ThemeManager.updateThemePreferenceForGender("Female", femalePref)
        }
    )

    val trainingHistory = TrainingHistoryStore()

    /** Pro lifetime and tips through Google Play; connected once for the app's lifetime. */
    val billing = com.FreeRave.shredzilla.pro.BillingRepository(application.applicationContext).also { it.connect() }

    val timerManager = TimerManager(
        context = application.applicationContext,
        coroutineScope = viewModelScope,
        userRestTimePreferenceProvider = { userDataManager.userRestTimePreference }
    )

    /** Set logging and the guided workout, shared by the screens and the workout notification. */
    val workout by lazy {
        com.FreeRave.shredzilla.workout.WorkoutController(
            context = application.applicationContext,
            scope = viewModelScope,
            userDataManager = userDataManager,
            trainingHistory = trainingHistory,
            timerManager = timerManager,
            billing = billing,
            history = exerciseHistory,
            refreshHistory = { FirebaseAuth.getInstance().currentUser?.uid?.let { loadHistoricalAnalytics(it, force = true) } },
        )
    }

    /** Badges, judged in the background from the same data the screens use. */
    val badges by lazy {
        com.FreeRave.shredzilla.badges.BadgeTracker(
            scope = viewModelScope,
            userDataManager = userDataManager,
            trainingHistory = trainingHistory,
            billing = billing,
            workout = workout,
            history = exerciseHistory,
        )
    }

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

    private var historyLoadJob: Job? = null

    /**
     * Two passes over the last [HISTORY_DAYS] days: the device cache first, so history shows up
     * instantly and works offline (it includes sets that haven't synced yet), then the server.
     * [force] skips the 30-minute freshness check, e.g. right after a set was added, edited or deleted.
     */
    fun loadHistoricalAnalytics(userId: String, force: Boolean = false) {
        val isCacheExpired = System.currentTimeMillis() - lastAnalyticsLoadTimeMillis > CACHE_EXPIRY_MS
        if (!force && !isCacheExpired && (_analyticsGraphData.value.isNotEmpty() || _isAnalyticsLoading.value)) return

        historyLoadJob?.cancel()
        _isAnalyticsLoading.value = true
        historyLoadJob = viewModelScope.launch {
            try {
                val cached = fetchHistory(userId, Source.CACHE)
                publishHistory(cached)
                // Only keep the spinner up when there's nothing to show yet (first launch on a device)
                if (cached.isNotEmpty()) _isAnalyticsLoading.value = false
                publishHistory(fetchHistory(userId, Source.DEFAULT))
                lastAnalyticsLoadTimeMillis = System.currentTimeMillis()
            } finally {
                _isAnalyticsLoading.value = false
            }
        }
    }

    private suspend fun fetchHistory(userId: String, source: Source): List<LoggedSet> = coroutineScope {
        val daysRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("dailyActivity")
        // Day documents were never written before (sets go straight into their subcollection),
        // and Firestore can't list documents that don't exist. So ask for each of the last
        // 90 days by key instead of listing the collection.
        val now = System.currentTimeMillis()
        val dayKeys = dateKeysBetween(now - TimeUnit.DAYS.toMillis(HISTORY_DAYS - 1L), now)
        dayKeys.map { dateKey ->
            async(Dispatchers.IO) {
                val dayRef = daysRef.document(dateKey)
                val setsSnapshot = try {
                    dayRef.collection("recordedSets").get(source).await()
                } catch (e: Exception) {
                    // Server unreachable mid-load: fall back to what the device already has
                    if (source == Source.CACHE) null
                    else runCatching { dayRef.collection("recordedSets").get(Source.CACHE).await() }.getOrNull()
                } ?: return@async emptyList()
                // Backfill the day document so the day is listable from now on (e.g. for account deletion)
                if (source != Source.CACHE && !setsSnapshot.isEmpty) dayRef.set(mapOf("date" to dateKey), SetOptions.merge())
                setsSnapshot.documents.mapNotNull { setDoc ->
                    LoggedSet(
                        docId = setDoc.id,
                        dateKey = dateKey,
                        exerciseId = setDoc.getString("exerciseId") ?: return@mapNotNull null,
                        reps = setDoc.getLong("reps")?.toInt() ?: 0,
                        weightKg = setDoc.getDouble("weight") ?: 0.0,
                        notes = setDoc.getString("notes").orEmpty(),
                        plates = setDoc.getLong("plates")?.toInt(),
                        side = Side.fromStored(setDoc.getString("side")),
                        bodyweight = setDoc.getBoolean("bodyweight") ?: false,
                        setType = SetDocFields.setTypeOf(setDoc),
                        tempo = SetDocFields.tempoOf(setDoc),
                        perSide = SetDocFields.perSideOf(setDoc),
                        // Pending (unsynced) sets have no server timestamp yet; estimate it locally
                        timestampMillis = (setDoc.getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.seconds ?: 0L) * 1000L
                    )
                }
            }
        }.awaitAll().flatten()
    }

    private suspend fun publishHistory(allSets: List<LoggedSet>) {
        val chartPoints = withContext(Dispatchers.Default) {
            allSets.groupBy { it.exerciseId }.mapValues { (_, sets) ->
                sets.groupBy { startOfDay(Date(it.timestampMillis)) }
                    .map { (dayStartMillis, daySets) ->
                        ChartDataPoint(timestampDate = dayStartMillis, totalVolume = daySets.sumOf { it.volume })
                    }
                    .sortedBy { it.timestampDate }
            }
        }
        _analyticsGraphData.value = chartPoints
        _exerciseHistory.value = allSets.sortedByDescending { it.timestampMillis }.groupBy { it.exerciseId }
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
