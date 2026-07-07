package com.FreeRave.shredzilla.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.data.UserDataManager
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

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
    }

    // Process Death secure destination state
    val startDestinationFlow = savedStateHandle.getStateFlow<String?>("startDestination", null)
    
    fun updateStartDestination(dest: String?) {
        savedStateHandle["startDestination"] = dest
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

    private val _isAnalyticsLoading = MutableStateFlow(false)
    val isAnalyticsLoading = _isAnalyticsLoading.asStateFlow()

    private var lastAnalyticsLoadTimeMillis: Long = 0

    fun loadHistoricalAnalytics(userId: String) {
        val currentTime = System.currentTimeMillis()
        val isCacheExpired = currentTime - lastAnalyticsLoadTimeMillis > CACHE_EXPIRY_MS
        if (!isCacheExpired && (_analyticsGraphData.value.isNotEmpty() || _isAnalyticsLoading.value)) return // Cached and valid!

        _isAnalyticsLoading.value = true
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                
                // 1. Fetch available dailyActivity nodes
                val daysSnapshot = db.collection("users").document(userId)
                    .collection("dailyActivity")
                    .orderBy(com.google.firebase.firestore.FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(90) // Pro-Tip: Limit historical data to 3 months to save memory & costs
                    .get().await()

                if (daysSnapshot.isEmpty) {
                    _isAnalyticsLoading.value = false
                    return@launch
                }

                // 2. Sequential extraction: Safely fetch sets avoiding API limits
                val allSets = mutableListOf<Triple<String, Double, Long>>()
                for (dayDoc in daysSnapshot.documents) {
                    try {
                        val setsSnapshot = dayDoc.reference.collection("recordedSets").get().await()
                        setsSnapshot.documents.forEach { setDoc ->
                            val exId = setDoc.getString("exerciseId") ?: return@forEach
                            val reps = setDoc.getLong("reps")?.toInt() ?: 0
                            val weight = setDoc.getDouble("weight") ?: 0.0
                            val ts = setDoc.getTimestamp("timestamp")?.seconds ?: 0L
                            allSets.add(Triple(exId, reps * weight, ts * 1000L)) // (Id, Volume, Time)
                        }
                    } catch(e: Exception) {
                        // Handle silently
                    }
                }

                // 3. Mathematical Agression: Process timeline grouping on Default dispatcher
                val chartPoints = withContext(Dispatchers.Default) {
                    val groupedByExercise = allSets.groupBy { it.first }
                    
                    val result = mutableMapOf<String, List<ChartDataPoint>>()
                    groupedByExercise.forEach { (exId, sets) ->
                        val dailyAggregated = sets.groupBy { 
                            val cal = Calendar.getInstance().apply { timeInMillis = it.third }
                            cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                            cal.timeInMillis
                        }.map { (dayStartMillis, daySets) ->
                            ChartDataPoint(
                                timestampDate = dayStartMillis,
                                totalVolume = daySets.sumOf { it.second }
                            )
                        }.sortedBy { it.timestampDate }
                        result[exId] = dailyAggregated
                    }
                    result
                }

                _analyticsGraphData.value = chartPoints
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
                    val userId = user.uid

                    // Clean up local profile image
                    userDataManager.userProfileImageLocalPath?.let { path ->
                        com.FreeRave.shredzilla.utils.ImageStorageUtils.deleteImageFromInternalStorage(path)
                    }

                    // 1. Delete data from Firestore
                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userId).delete().await()
                    
                    // 2. Delete Auth User
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
