package com.example.voltroute.domain.usecase

import com.example.voltroute.data.local.repository.CacheRepository
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.Route
import javax.inject.Inject

/**
 * Use case for saving route data to local cache
 *
 * Handles caching of:
 * - Calculated route
 * - Found chargers along route
 * - Charging plan (if available)
 *
 * This enables offline mode by storing last successful search results.
 * Called automatically after successful route calculation + charger search.
 *
 * Design: Encapsulates cache save logic in domain layer
 */
class SaveToCacheUseCase @Inject constructor(
    private val cacheRepository: CacheRepository
) {

    /**
     * Save route data to local cache
     *
     * Saves all components of a successful route search:
     * 1. Route details (distance, duration, polyline)
     * 2. Chargers found along route
     * 3. Charging plan (if battery requires charging stops)
     *
     * All operations are suspend functions that run on IO dispatcher internally.
     *
     * @param route The calculated route
     * @param destinationAddress User-entered destination text
     * @param chargers List of chargers found along route
     * @param chargingPlan Optional charging plan (null if no stops needed)
     */
    suspend operator fun invoke(
        route: Route,
        destinationAddress: String,
        chargers: List<Charger>,
        chargingPlan: ChargingPlan?
    ) {
        // Save route with destination address
        cacheRepository.saveRoute(route, destinationAddress)

        // Save all chargers found
        cacheRepository.saveChargers(chargers)

        // Save charging plan if it exists
        // (plan is null when current battery can reach destination)
        chargingPlan?.let { plan ->
            cacheRepository.saveChargingPlan(plan)
        }
    }
}

