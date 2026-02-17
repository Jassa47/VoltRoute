package com.example.voltroute.domain.usecase

import com.example.voltroute.data.local.repository.CacheRepository
import com.example.voltroute.data.local.repository.CachedData
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.Route
import javax.inject.Inject

/**
 * Aggregated cached data for app state restoration
 *
 * Contains all cached data needed to restore the app to its last state.
 *
 * @param routeData Cached route with metadata (null if no cache)
 * @param chargers List of cached chargers (empty if no cache)
 * @param chargingPlan Cached charging plan (null if no cache or not needed)
 * @param cacheAgeText Human-readable cache age ("5 minutes ago")
 */
data class CachedAppData(
    val routeData: CachedData<Route>?,
    val chargers: List<Charger>,
    val chargingPlan: ChargingPlan?,
    val cacheAgeText: String
)

/**
 * Use case for loading all cached data
 *
 * Loads complete cached state:
 * - Last calculated route
 * - Chargers found along that route
 * - Charging plan (if stops were needed)
 * - Cache age for display
 *
 * Used for:
 * - Checking if cache exists on app start
 * - Restoring last route in offline mode
 * - Displaying cache availability to user
 *
 * Design: Aggregates all cache loading into single use case
 */
class LoadFromCacheUseCase @Inject constructor(
    private val cacheRepository: CacheRepository
) {

    /**
     * Load all cached data
     *
     * Retrieves all cached components and calculates cache age.
     * Returns aggregated data structure for easy consumption by ViewModel.
     *
     * All components may be null/empty if:
     * - App is running for first time
     * - Cache was cleared
     * - No route has been calculated yet
     *
     * @return CachedAppData with all cached information
     */
    suspend operator fun invoke(): CachedAppData {
        // Load route data (includes timestamp and destination)
        val routeData = cacheRepository.loadCachedRoute()

        // Load chargers (empty list if none cached)
        val chargers = cacheRepository.loadCachedChargers()

        // Load charging plan (null if none cached)
        val chargingPlan = cacheRepository.loadCachedChargingPlan()

        // Get cache timestamp for age calculation
        val cachedAt = cacheRepository.getLastCachedTime()

        // Format cache age for display
        // Example: "5 minutes ago", "2 hours ago", "just now"
        val cacheAgeText = if (cachedAt != null) {
            cacheRepository.formatCacheAge(cachedAt)
        } else {
            "unknown"
        }

        return CachedAppData(
            routeData = routeData,
            chargers = chargers,
            chargingPlan = chargingPlan,
            cacheAgeText = cacheAgeText
        )
    }
}

