package com.example.voltroute.domain.usecase

import com.example.voltroute.data.remote.repository.ChargerRepository
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Use case for finding EV charging stations near current location or route
 *
 * Smart search logic:
 * - Without route: Searches near current location with 25km radius
 * - With route: Searches near route midpoint with adaptive radius based on route length
 *
 * This provides better coverage for long routes by searching along the route
 * rather than just at start/end points.
 *
 * Design: Pure domain logic following Clean Architecture
 */
class FindChargersUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {

    /**
     * Find charging stations near current location or along a route
     *
     * Search strategy:
     * - No route: Search 25km around current location
     * - With route: Search around route midpoint with adaptive radius
     *   - Radius = route distance / 4 (to cover route corridor)
     *   - Capped at 200km maximum (API limit consideration)
     *   - Minimum 25km (ensure some results)
     *
     * @param currentLocation User's current geographic position
     * @param route Optional calculated route to destination
     * @return Result.success with List<Charger> on success, Result.failure on error
     */
    suspend operator fun invoke(
        currentLocation: Location,
        route: Route? = null
    ): Result<List<Charger>> {

        // Determine search location based on route availability
        val searchLocation = if (route != null) {
            // Calculate midpoint of route for better coverage along the path
            Location(
                latitude = (route.startLocation.latitude + route.endLocation.latitude) / 2.0,
                longitude = (route.startLocation.longitude + route.endLocation.longitude) / 2.0,
                name = "Route Midpoint"
            )
        } else {
            // Use current location when no route planned
            currentLocation
        }

        // Calculate appropriate search radius
        val searchRadius = if (route != null) {
            // Adaptive radius based on route length
            // Use 1/4 of route distance to create a search corridor
            val routeBasedRadius = route.distanceKm / 4.0

            // Cap at 200km max (API performance and relevance)
            val cappedRadius = min(routeBasedRadius, 200.0).toInt()

            // Ensure minimum 25km for short routes
            max(cappedRadius, 25)
        } else {
            // Default 25km radius for local search
            25
        }

        // Call repository to fetch chargers
        return chargerRepository.getChargersNearLocation(
            location = searchLocation,
            radiusKm = searchRadius,
            maxResults = 20
        )
    }
}

