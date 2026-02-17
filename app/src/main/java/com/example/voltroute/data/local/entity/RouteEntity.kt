package com.example.voltroute.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching route data locally
 *
 * Stores a single calculated route for offline access.
 * Uses id = 1 always (single route cache strategy).
 * When a new route is saved, it replaces the old one (OnConflictStrategy.REPLACE).
 *
 * Design decisions:
 * - Locations broken into separate lat/lng fields (Room doesn't support nested objects directly)
 * - destinationAddress stored to restore search field
 * - cachedAt timestamp for cache age display
 *
 * @param id Always 1 (single route cache)
 * @param distanceMeters Route distance in meters
 * @param durationSeconds Route duration in seconds
 * @param polylinePoints Encoded polyline string for map display
 * @param startLat Starting location latitude
 * @param startLng Starting location longitude
 * @param endLat Destination latitude
 * @param endLng Destination longitude
 * @param destinationAddress User-entered destination text
 * @param cachedAt Timestamp when route was cached (milliseconds since epoch)
 */
@Entity(tableName = "cached_routes")
data class RouteEntity(
    @PrimaryKey
    val id: Int = 1,

    val distanceMeters: Int,
    val durationSeconds: Int,
    val polylinePoints: String,

    // Start location (flattened from Location object)
    val startLat: Double,
    val startLng: Double,

    // End location (flattened from Location object)
    val endLat: Double,
    val endLng: Double,

    // Metadata
    val destinationAddress: String,
    val cachedAt: Long = System.currentTimeMillis()
)

