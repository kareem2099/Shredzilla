package com.FreeRave.shredzilla.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.FreeRave.shredzilla.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import java.util.concurrent.TimeUnit

/** What the update UI should show. */
sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    /** A newer version is live on Google Play; the user may update now or later. */
    data class Available(val versionCode: Int) : UpdateStatus
    data class Downloading(val percent: Int) : UpdateStatus
    /** Downloaded in the background; a restart installs it. */
    data object ReadyToInstall : UpdateStatus
    /** This version is below the minimum set in Remote Config: the app can't be used until it's updated. */
    data object Required : UpdateStatus
}

/**
 * Keeps the app current through Google Play:
 * - Remote Config's [MIN_VERSION_KEY] (changed from the Firebase console, no release needed) blocks
 *   versions below it with a full-screen update.
 * - Otherwise a newer Play version is offered and downloaded in the background (flexible update).
 * Play only reports updates for installs that came from Play, so debug builds simply stay up to date.
 */
class AppUpdateController(private val activity: ComponentActivity) {

    companion object {
        const val MIN_VERSION_KEY = "min_supported_version_code"
        const val MESSAGE_KEY = "update_message"
        /** 1.4.0: everything before it is the old app. */
        private const val DEFAULT_MIN_VERSION_CODE = 7L
        private const val PREFS = "app_update"
        private const val PREF_PROMPTED_VERSION = "prompted_version"
        private const val PREF_PROMPTED_AT = "prompted_at"
        private val PROMPT_AGAIN_AFTER_MS = TimeUnit.DAYS.toMillis(1)
    }

    var status by mutableStateOf<UpdateStatus>(UpdateStatus.UpToDate)
        private set
    /** Optional text from Remote Config, e.g. what's new; empty when not set. */
    var message by mutableStateOf("")
        private set

    private val updateManager = AppUpdateManagerFactory.create(activity)
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private var latestInfo: AppUpdateInfo? = null
    private val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Registered in the constructor, so the controller must be created in onCreate
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // Nothing to do: backing out of a required update leaves the blocking screen up,
            // and a flexible one reports its progress through installListener
        }

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                status = UpdateStatus.Downloading(if (total > 0) (state.bytesDownloaded() * 100 / total).toInt() else 0)
            }
            InstallStatus.DOWNLOADED -> status = UpdateStatus.ReadyToInstall
            InstallStatus.FAILED, InstallStatus.CANCELED -> latestInfo?.let { status = UpdateStatus.Available(it.availableVersionCode()) }
            else -> Unit
        }
    }

    init {
        remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            // Debug builds fetch every time so console changes can be tried right away
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else TimeUnit.HOURS.toSeconds(1)
        })
        remoteConfig.setDefaultsAsync(mapOf(MIN_VERSION_KEY to DEFAULT_MIN_VERSION_CODE, MESSAGE_KEY to ""))
        updateManager.registerListener(installListener)
    }

    /** On launch: read the minimum version (cached values work offline), then ask Play. */
    fun check() {
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            message = remoteConfig.getString(MESSAGE_KEY)
            val required = BuildConfig.VERSION_CODE < remoteConfig.getLong(MIN_VERSION_KEY)
            if (required) status = UpdateStatus.Required
            updateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    latestInfo = info
                    when {
                        info.installStatus() == InstallStatus.DOWNLOADED -> status = UpdateStatus.ReadyToInstall
                        info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> Unit
                        required && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> startUpdate(AppUpdateType.IMMEDIATE)
                        !required && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && shouldPrompt(info.availableVersionCode()) ->
                            status = UpdateStatus.Available(info.availableVersionCode())
                    }
                }
                .addOnFailureListener { e -> Log.w("AppUpdate", "Play update check failed (not installed from Play?)", e) }
        }
    }

    /** When coming back to the app: finish a download that completed, or reopen an interrupted required update. */
    fun onResume() {
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            latestInfo = info
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> status = UpdateStatus.ReadyToInstall
                status == UpdateStatus.Required &&
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> startUpdate(AppUpdateType.IMMEDIATE)
            }
        }
    }

    /** "Update" on the prompt: download in the background while the user keeps training. */
    fun startFlexibleUpdate() {
        status = UpdateStatus.Downloading(0)
        startUpdate(AppUpdateType.FLEXIBLE)
    }

    /** "Update" on the blocking screen: Play's full-screen flow, or the store page when Play can't run it. */
    fun startRequiredUpdate() {
        val info = latestInfo
        if (info != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            startUpdate(AppUpdateType.IMMEDIATE)
        } else {
            openStorePage()
        }
    }

    /** "Later": hidden until tomorrow for this version. */
    fun postpone() {
        (status as? UpdateStatus.Available)?.let { available ->
            prefs.edit().putInt(PREF_PROMPTED_VERSION, available.versionCode).putLong(PREF_PROMPTED_AT, System.currentTimeMillis()).apply()
        }
        status = UpdateStatus.UpToDate
    }

    /** Restarts the app into the downloaded version. */
    fun installDownloaded() = updateManager.completeUpdate()

    fun release() = updateManager.unregisterListener(installListener)

    private fun shouldPrompt(versionCode: Int): Boolean =
        prefs.getInt(PREF_PROMPTED_VERSION, -1) != versionCode ||
            System.currentTimeMillis() - prefs.getLong(PREF_PROMPTED_AT, 0) > PROMPT_AGAIN_AFTER_MS

    private fun startUpdate(type: Int) {
        val info = latestInfo ?: return
        updateManager.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.defaultOptions(type))
    }

    private fun openStorePage() {
        val packageName = activity.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { activity.startActivity(market) }.onFailure {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
