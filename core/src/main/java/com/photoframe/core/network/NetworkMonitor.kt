package com.photoframe.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity state for the application.
 *
 * Provides reactive network state updates via StateFlow.
 * Useful for detecting when SMB connections may be lost or restored.
 *
 * Thread Safety: All operations are thread-safe.
 * StateFlow updates are atomic and can be observed from any coroutine.
 *
 * Use Case:
 * - Pause slideshow when network is lost
 * - Auto-reconnect to SMB when network is restored
 * - Show network error UI when offline
 *
 * Per Senior Dev 3 requirement: Auto-recovery from network failures.
 *
 * @param context Application context
 */
@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Context
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    private val _isNetworkAvailable = MutableStateFlow(checkInitialNetworkState())

    /**
     * StateFlow that emits true when network is available, false when offline.
     */
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
        }

        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            // Check if network has internet capability
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isNetworkAvailable.value = hasInternet && hasValidated
        }
    }

    init {
        registerNetworkCallback()
    }

    /**
     * Checks the initial network state synchronously.
     * Called once during initialization.
     */
    private fun checkInitialNetworkState(): Boolean {
        val connectivityManager = this.connectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Registers the network callback to listen for network changes.
     */
    private fun registerNetworkCallback() {
        val connectivityManager = this.connectivityManager ?: return

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            // Registration failed - device may not support network callbacks
            // State will remain at initial value
        }
    }

    /**
     * Unregisters the network callback.
     * Should be called when the monitor is no longer needed (e.g., app shutdown).
     */
    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Callback may not have been registered
        }
    }

    /**
     * Returns true if the device is currently connected to a WiFi network.
     * Useful for SMB connections which typically require WiFi.
     */
    fun isWifiConnected(): Boolean {
        val connectivityManager = this.connectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Returns true if the device is currently connected to an Ethernet network.
     * Useful for tablets with wired connections.
     */
    fun isEthernetConnected(): Boolean {
        val connectivityManager = this.connectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Flow that emits network state changes.
     * Use this for one-time observations or collecting in a coroutine.
     */
    fun networkAvailabilityFlow(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager != null) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, callback)

            // Emit initial state
            trySend(checkInitialNetworkState())

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        } else {
            // No connectivity manager - emit false
            trySend(false)
            awaitClose { }
        }
    }
}
