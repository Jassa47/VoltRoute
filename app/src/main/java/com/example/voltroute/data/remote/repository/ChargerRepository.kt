package com.example.voltroute.data.remote.repository

import com.example.voltroute.data.remote.api.OpenChargeMapApi
import com.example.voltroute.data.remote.dto.ChargerResponseItem
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repository for fetching and managing EV charging station data
 *
 * Responsibilities:
 * - Call Open Charge Map API via Retrofit
 * - Transform API DTOs to domain models
 * - Handle API errors and edge cases
 * - Switch to IO dispatcher for network calls
 *
 * Uses Result<T> for functional error handling
 */
@Singleton
class ChargerRepository @Inject constructor(
    private val openChargeMapApi: OpenChargeMapApi,
    @Named("ocm_api_key") private val apiKey: String
) {

    /**
     * Get charging stations near a specific location
     *
     * Searches Open Charge Map for EV charging stations within a radius.
     * Converts API response to domain Charger models.
     *
     * @param location Center point for search
     * @param radiusKm Search radius in kilometers (default 50 km)
     * @param maxResults Maximum number of stations to return (default 20)
     * @return Result.success with List<Charger> on success, Result.failure on error
     */
    suspend fun getChargersNearLocation(
        location: Location,
        radiusKm: Int = 50,
        maxResults: Int = 20
    ): Result<List<Charger>> = withContext(Dispatchers.IO) {
        try {
            // Call Open Charge Map API
            val response = openChargeMapApi.getChargers(
                latitude = location.latitude,
                longitude = location.longitude,
                distance = radiusKm,
                distanceUnit = "km",
                maxResults = maxResults,
                compact = true,
                verbose = false,
                key = apiKey
            )

            // Convert DTOs to domain models, filtering out invalid entries
            val chargers = response
                .mapNotNull { item -> convertToCharger(item) }
                .sortedBy { it.distanceKm ?: Double.MAX_VALUE } // Sort by distance

            Result.success(chargers)

        } catch (e: Exception) {
            // Handle network errors, JSON parsing errors, etc.
            Result.failure(
                Exception("Could not load charging stations. Please try again.", e)
            )
        }
    }

    /**
     * Convert API DTO to domain Charger model
     *
     * Handles missing/invalid data gracefully:
     * - Returns null if no valid connections available
     * - Uses default values for missing fields
     * - Aggregates data from multiple connections
     *
     * @param item Raw API response item
     * @return Charger domain model, or null if data is invalid
     */
    private fun convertToCharger(item: ChargerResponseItem): Charger? {
        // Skip if no connections data available
        if (item.connections.isNullOrEmpty()) {
            return null
        }

        // Find maximum charging power across all connections
        val maxPowerKw = item.connections
            .mapNotNull { it.powerKw }
            .maxOrNull() ?: 0.0

        // Collect all unique connector types, filtering out blanks
        val connectorTypes = item.connections
            .mapNotNull { it.connectionType?.title }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("Unknown") } // Default if no valid types found

        // Sum up total number of charging ports
        var totalPorts = item.connections
            .mapNotNull { it.quantity }
            .sum()

        // If no quantities specified, use number of connection types as fallback
        if (totalPorts == 0) {
            totalPorts = item.connections.size
        }

        // Build domain Charger model
        return Charger(
            id = item.id.toString(),
            name = item.addressInfo.title ?: "Charging Station",
            location = Location(
                latitude = item.addressInfo.latitude,
                longitude = item.addressInfo.longitude
            ),
            powerKw = maxPowerKw,
            connectorTypes = connectorTypes,
            numberOfPoints = totalPorts,
            distanceKm = item.addressInfo.distance
        )
    }
}

