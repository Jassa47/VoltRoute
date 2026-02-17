package com.example.voltroute.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching charging station data locally
 *
 * Stores chargers found along the route for offline access.
 * Multiple chargers can be cached (list of available stations).
 *
 * Design decisions:
 * - connectorTypes stored as JSON string because Room doesn't directly support List<String>
 *   Example: "[\"CCS\",\"CHAdeMO\",\"Type 2\"]"
 *   Use Gson to serialize/deserialize: gson.toJson() and gson.fromJson()
 * - Location broken into separate lat/lng fields
 * - distanceKm nullable (may not be calculated for all chargers)
 * - id from Open Charge Map API (unique identifier for deduplication)
 *
 * @param id Unique charger identifier (from API)
 * @param name Charging station name
 * @param latitude Station location latitude
 * @param longitude Station location longitude
 * @param powerKw Maximum charging power in kilowatts
 * @param connectorTypes JSON string of connector type list
 * @param numberOfPoints Number of charging ports available
 * @param distanceKm Distance from origin (nullable)
 * @param cachedAt Timestamp when charger was cached
 */
@Entity(tableName = "cached_chargers")
data class ChargerEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    // Location (flattened)
    val latitude: Double,
    val longitude: Double,

    // Charger specifications
    val powerKw: Double,
    val connectorTypes: String,  // JSON string: ["CCS", "CHAdeMO"]
    val numberOfPoints: Int,

    // Optional metadata
    val distanceKm: Double?,
    val cachedAt: Long = System.currentTimeMillis()
)

