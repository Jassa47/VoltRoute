package com.example.voltroute.domain.model

/**
 * Domain model representing a calculated route between two locations
 *
 * This is the clean domain representation, independent of any API response structure.
 * All computed properties provide convenient access to formatted data.
 *
 * @param distanceMeters Total route distance in meters
 * @param durationSeconds Estimated travel time in seconds
 * @param polylinePoints Encoded polyline string for map rendering
 * @param startLocation Starting point of the route
 * @param endLocation Destination point of the route
 */
data class Route(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polylinePoints: String,
    val startLocation: Location,
    val endLocation: Location
) {
    /**
     * Distance in kilometers (more user-friendly than meters)
     */
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    /**
     * Duration in minutes (more user-friendly than seconds)
     */
    val durationMinutes: Int
        get() = durationSeconds / 60

    /**
     * Formatted distance string for display (e.g., "15.2 km")
     */
    val distanceText: String
        get() = "%.1f km".format(distanceKm)

    /**
     * Formatted duration string for display (e.g., "25 min")
     */
    val durationText: String
        get() = "$durationMinutes min"
}

