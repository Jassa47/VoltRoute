package com.example.voltroute.domain.usecase

import com.example.voltroute.data.remote.repository.ChargerRepository
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import javax.inject.Inject

/**
 * Use case for finding EV charging stations along a route or near current location
 *
 * ALGORITHM VISUALIZATION:
 *
 * OLD approach (WRONG - searches midpoint only):
 * Vancouver ──────────── Midpoint ──────────── LA
 * ↑
 * Only searched here
 * (may be in desert!)
 *
 * NEW approach (CORRECT - searches every 100km along actual route):
 * Vancouver ──●──────●────────●──────●────────●── LA
 * ↑ ↑ ↑ ↑ ↑
 * Search Search Search Search Search
 * 25km 25km 25km 25km 25km radius
 *
 * Result: Chargers spread along entire route! ✅
 * Deduplication: No repeated stations! ✅
 *
 * Example: Vancouver → Los Angeles (1,931 km route)
 * - Decodes polyline into ~500 GPS points
 * - Searches every ~100km (every 25 points)
 * - ~20 search locations along route
 * - Finds ~60-80 unique chargers after deduplication
 *
 * Design: Pure domain logic following Clean Architecture
 */
class FindChargersUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {

    companion object {
        // Search every 100km along the route
        private const val SEARCH_INTERVAL_KM = 100.0

        // Search 25km radius around each point
        private const val SEARCH_RADIUS_KM = 25

        // Max chargers to fetch per search point
        private const val MAX_RESULTS_PER_POINT = 10

        // Default radius when no route
        private const val DEFAULT_RADIUS_KM = 25
    }

    /**
     * Find charging stations near current location or along a route
     *
     * Search strategy:
     * - No route: Search 25km around current location
     * - With route: Decode polyline, search every 100km along actual path
     *
     * @param currentLocation User's current geographic position
     * @param route Optional calculated route to destination
     * @return Result.success with List<Charger> on success, Result.failure on error
     */
    suspend operator fun invoke(
        currentLocation: Location,
        route: Route? = null
    ): Result<List<Charger>> {

        // No route: Simple local search
        if (route == null) {
            return chargerRepository.getChargersNearLocation(
                location = currentLocation,
                radiusKm = DEFAULT_RADIUS_KM,
                maxResults = 20
            )
        }

        // Route exists: Search along polyline path
        return try {
            searchAlongRoute(currentLocation, route)
        } catch (e: Exception) {
            // If polyline decode or search fails, return error
            Result.failure(Exception("Could not find chargers along route: ${e.message}", e))
        }
    }

    /**
     * Search for chargers along the actual route polyline path
     *
     * Algorithm:
     * 1. Decode encoded polyline into GPS points
     * 2. Calculate interval (how many points = 100km)
     * 3. Build list of search points every 100km
     * 4. Search each point with 25km radius
     * 5. Deduplicate results by charger ID
     *
     * @param currentLocation Starting location
     * @param route Route with encoded polyline
     * @return Result with deduplicated list of chargers
     */
    private suspend fun searchAlongRoute(
        currentLocation: Location,
        route: Route
    ): Result<List<Charger>> {

        // STEP 1: Decode polyline into GPS coordinates
        val routePoints: List<LatLng> = PolyUtil.decode(route.polylinePoints)
        val totalPoints = routePoints.size
        val totalKm = route.distanceKm

        // STEP 2: Calculate how many polyline points represent 100km
        val pointsPerInterval = if (totalKm > 0) {
            ((SEARCH_INTERVAL_KM / totalKm) * totalPoints)
                .toInt()
                .coerceAtLeast(1)  // Never less than 1 to prevent infinite loop
        } else {
            totalPoints  // If distance unknown, use all points
        }

        // STEP 3: Build list of search points along the route
        val searchPoints = mutableListOf<Location>()

        // Always search at start location
        searchPoints.add(currentLocation)

        // Add points every ~100km along route
        var index = pointsPerInterval
        while (index < totalPoints) {
            val point = routePoints[index]
            searchPoints.add(
                Location(
                    latitude = point.latitude,
                    longitude = point.longitude
                )
            )
            index += pointsPerInterval
        }

        // Always search near destination
        if (routePoints.isNotEmpty()) {
            val lastPoint = routePoints.last()
            searchPoints.add(
                Location(
                    latitude = lastPoint.latitude,
                    longitude = lastPoint.longitude
                )
            )
        }

        // STEP 4: Search each point and deduplicate results
        val allChargers = mutableListOf<Charger>()
        val seenIds = mutableSetOf<String>()  // For O(1) duplicate checking

        for (searchPoint in searchPoints) {
            val result = chargerRepository.getChargersNearLocation(
                location = searchPoint,
                radiusKm = SEARCH_RADIUS_KM,
                maxResults = MAX_RESULTS_PER_POINT
            )

            // Process results from this search point
            result.onSuccess { chargers ->
                chargers.forEach { charger ->
                    // seenIds.add() returns true if newly added, false if duplicate
                    // This efficiently deduplicates chargers found in multiple searches
                    if (seenIds.add(charger.id)) {
                        allChargers.add(charger)
                    }
                }
            }
            // Continue even if one search fails (resilient to partial failures)
        }

        // STEP 5: Return all combined, deduplicated chargers
        return Result.success(allChargers)
    }
}
