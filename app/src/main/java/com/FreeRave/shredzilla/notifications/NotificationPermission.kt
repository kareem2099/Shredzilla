package com.FreeRave.shredzilla.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.FreeRave.shredzilla.pro.findActivity

/** Notification permission state that refreshes when the user comes back from system settings. */
class NotificationPermissionState(private val context: Context, private val requestSystem: () -> Unit) {
    var granted by mutableStateOf(AppNotifications.enabled(context))
        internal set

    /** Android 13+ shows its dialog; after a refusal (or on older versions) open the app's notification settings. */
    fun request() {
        val store = ReminderStore(context)
        val activity = context.findActivity()
        val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
            (!store.permissionPrompted || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS))
        store.permissionPrompted = true
        if (canAsk) requestSystem() else openSettings()
    }

    fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
fun rememberNotificationPermission(onResult: (Boolean) -> Unit = {}): NotificationPermissionState {
    val context = LocalContext.current
    var state: NotificationPermissionState? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state?.granted = granted
        onResult(granted)
    }
    val remembered = remember(context) {
        NotificationPermissionState(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    state = remembered
    LifecycleResumeEffect(remembered) {
        remembered.granted = AppNotifications.enabled(context)
        onPauseOrDispose { }
    }
    return remembered
}
