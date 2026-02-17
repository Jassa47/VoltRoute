package com.example.voltroute.domain.model

/**
 * Power level categories for EV charging stations
 * Used for UI visualization and marker colors
 *
 * - ULTRA_FAST: 150kW+ (DC Fast Charging) - Blue marker
 * - FAST: 50-149kW (DC Fast Charging) - Yellow marker
 * - STANDARD: <50kW (Level 2 AC) - Red marker
 */
enum class PowerLevel {
    ULTRA_FAST,  // >= 150kW
    FAST,        // 50-149kW
    STANDARD     // < 50kW
}

/**
 * Represents an EV charging station
 *
 * Pure domain model with no Android dependencies.
 * Contains station information, location, power specifications,
 * and connector availability.
 *
 * Design: Clean domain model following separation of concerns
 *
 * @param id Unique identifier for the charging station
 * @param name Station name (e.g., "Tesla Supercharger", "ChargePoint")
 * @param location Geographic location using our Location domain model
 * @param powerKw Maximum charging power in kilowatts
 * @param connectorTypes List of available connector types (CCS, CHAdeMO, Type 2, etc.)
 * @param numberOfPoints Total number of charging ports/points available
 * @param distanceKm Distance from current location in kilometers (null if not calculated)
 */
data class Charger(
    val id: String,
    val name: String,
    val location: Location,
    val powerKw: Double,
    val connectorTypes: List<String>,
    val numberOfPoints: Int,
    val distanceKm: Double? = null
) {

    /**
     * Power level category based on maximum charging power
     * Used for color-coded markers and filtering
     */
    val powerLevel: PowerLevel
        get() = when {
            powerKw >= 150 -> PowerLevel.ULTRA_FAST
            powerKw >= 50 -> PowerLevel.FAST
            else -> PowerLevel.STANDARD
        }

    /**
     * Formatted power output for display
     * Shows kW for standard chargers, W for very low power
     *
     * Examples: "150 kW", "50 kW", "7 kW", "500 W"
     */
    val powerText: String
        get() = if (powerKw >= 1) {
            "${powerKw.toInt()} kW"
        } else {
            "${(powerKw * 1000).toInt()} W"
        }

    /**
     * Formatted distance for display
     * Returns null if distance not calculated
     *
     * Examples: "2.3 km away", "15.7 km away", null
     */
    val distanceText: String?
        get() = distanceKm?.let { "%.1f km away".format(it) }

    /**
     * Comma-separated list of connector types
     * Shows all available connector types at this station
     *
     * Examples: "CCS, CHAdeMO", "Type 2", "Tesla, CCS"
     */
    val connectorText: String
        get() = connectorTypes.joinToString(", ")

    /**
     * Formatted number of ports for display
     * Handles singular/plural properly
     *
     * Examples: "1 port", "8 ports", "4 ports"
     */
    val portsText: String
        get() = "$numberOfPoints ${if (numberOfPoints == 1) "port" else "ports"}"
}

