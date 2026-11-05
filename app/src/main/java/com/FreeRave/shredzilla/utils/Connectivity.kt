package com.FreeRave.shredzilla.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Validated means the internet really works, not just "joined a Wi-Fi" (e.g. a gym captive portal). */
private fun NetworkCapabilities?.hasWorkingInternet(): Boolean =
    this != null && hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

/** Live online/offline state of the default network, for UI hints. Firestore syncs on its own either way. */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val connectivity = remember { context.getSystemService(ConnectivityManager::class.java) }
    var isOnline by remember {
        mutableStateOf(connectivity.getNetworkCapabilities(connectivity.activeNetwork).hasWorkingInternet())
    }
    DisposableEffect(connectivity) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                isOnline = capabilities.hasWorkingInternet()
            }

            override fun onLost(network: Network) {
                isOnline = false
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    return isOnline
}
