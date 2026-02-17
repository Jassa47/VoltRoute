package com.example.voltroute.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connectivity monitor
 *
 * Provides real-time network status updates using Android's ConnectivityManager.
 * Uses callbackFlow to emit connectivity changes as a Flow.
 *
 * Features:
 * - Emits true when internet is available, false when offline
 * - Registers NetworkCallback for real-time updates
 * - Automatically cleans up callback on Flow cancellation
 * - Uses distinctUntilChanged to avoid duplicate emissions
 *
 * Design: Singleton to share network state across app
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Flow that emits network connectivity status
     *
     * Emits true when device has internet access, false when offline.
     * Uses callbackFlow to bridge Android's callback-based API to Kotlin Flow.
     *
     * The flow:
     * 1. Immediately emits current network state
     * 2. Registers callback for future changes
     * 3. Emits on network available/lost/changed
     * 4. Unregisters callback when flow is cancelled (awaitClose)
     *
     * distinctUntilChanged prevents duplicate consecutive emissions
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        // Emit current state immediately when flow is collected
        trySend(isCurrentlyOnline())

        // Create callback to listen for network changes
        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                // Network became available - we're online
                trySend(true)
            }

            override fun onLost(network: Network) {
                // Network lost - check if any other network is still available
                trySend(isCurrentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // Network capabilities changed - check if internet is available
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                trySend(hasInternet)
            }
        }

        // Build network request that matches any network with internet capability
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Register callback to receive network updates
        connectivityManager.registerNetworkCallback(networkRequest, callback)

        // Cleanup: Unregister callback when flow is cancelled
        // This prevents memory leaks
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()  // Only emit when value actually changes

    /**
     * Check current network connectivity state synchronously
     *
     * Returns true if device has an active network with internet capability.
     * This is useful for one-time checks without observing the flow.
     *
     * @return true if online, false if offline
     */
    fun isCurrentlyOnline(): Boolean {
        // Get active network (returns null if no active network)
        val network = connectivityManager.activeNetwork ?: return false

        // Get capabilities of active network (returns null if unavailable)
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return false

        // Check if network has internet capability
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

