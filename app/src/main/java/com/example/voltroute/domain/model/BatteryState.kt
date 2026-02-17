package com.example.voltroute.domain.model

/**
 * Battery level indicator for UI color coding
 * - HIGH: >= 60% (Green) - Safe for long trips
 * - MEDIUM: 30-59% (Yellow) - Moderate range
 * - LOW: 15-29% (Orange) - Consider charging soon
 * - CRITICAL: < 15% (Red) - Charge immediately
 */
enum class BatteryLevel {
    HIGH,      // >= 60%
    MEDIUM,    // 30-59%
    LOW,       // 15-29%
    CRITICAL   // < 15%
}

/**
 * Represents the complete battery state for an EV
 *
 * Tracks both current battery status and route-specific energy requirements.
 * Used to calculate if a destination is reachable and how many charging stops needed.
 *
 * Design: Pure domain model with no Android dependencies
 *
 * @param batteryCapacityKWh Total battery capacity in kilowatt-hours
 * @param currentBatteryPercent Current charge level (0-100)
 * @param efficiencyKWhPerKm Vehicle efficiency (energy per km)
 * @param currentEnergyKWh Available energy right now
 * @param requiredEnergyKWh Energy needed for planned route (null if no route)
 * @param routeDistanceKm Distance of planned route (null if no route)
 * @param remainingRangeKm How far vehicle can travel on current charge
 * @param canReachDestination Whether current charge is sufficient for route
 * @param energyDeficitKWh How much more energy needed (null if can reach)
 * @param numberOfChargesNeeded Charging stops required to complete route
 * @param percentageUsedForRoute What % of battery the route will consume
 */
data class BatteryState(
    val batteryCapacityKWh: Double,
    val currentBatteryPercent: Double,
    val efficiencyKWhPerKm: Double,
    val currentEnergyKWh: Double,
    val requiredEnergyKWh: Double? = null,
    val routeDistanceKm: Double? = null,
    val remainingRangeKm: Double,
    val canReachDestination: Boolean = false,
    val energyDeficitKWh: Double? = null,
    val numberOfChargesNeeded: Int = 0,
    val percentageUsedForRoute: Double? = null
) {

    /**
     * Battery level category based on current charge percentage
     * Used for color-coded UI indicators
     */
    val batteryLevel: BatteryLevel
        get() = when {
            currentBatteryPercent >= 60 -> BatteryLevel.HIGH
            currentBatteryPercent >= 30 -> BatteryLevel.MEDIUM
            currentBatteryPercent >= 15 -> BatteryLevel.LOW
            else -> BatteryLevel.CRITICAL
        }

    /**
     * Formatted battery percentage for display
     * Example: "80%", "45%"
     */
    val batteryPercentText: String
        get() = "${currentBatteryPercent.toInt()}%"

    /**
     * Formatted remaining range for display
     * Example: "450 km", "125 km"
     */
    val remainingRangeText: String
        get() = "%.0f km".format(remainingRangeKm)

    /**
     * Formatted current energy for display
     * Example: "108.0 kWh", "54.5 kWh"
     */
    val currentEnergyText: String
        get() = "%.1f kWh".format(currentEnergyKWh)

    /**
     * Formatted required energy for display
     * Returns null if no route planned
     * Example: "45.2 kWh", null
     */
    val requiredEnergyText: String?
        get() = requiredEnergyKWh?.let { "%.1f kWh".format(it) }

    /**
     * User-friendly warning message when destination is unreachable
     * Returns null if destination is reachable or no route planned
     *
     * Examples:
     * - "Need 1 charging stop to reach destination"
     * - "Need 2 charging stops to reach destination"
     * - null (if can reach)
     */
    val warningMessage: String?
        get() = if (!canReachDestination && requiredEnergyKWh != null) {
            val stopWord = if (numberOfChargesNeeded == 1) "stop" else "stops"
            "Need $numberOfChargesNeeded charging $stopWord to reach destination"
        } else {
            null
        }
}

