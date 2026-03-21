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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

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
