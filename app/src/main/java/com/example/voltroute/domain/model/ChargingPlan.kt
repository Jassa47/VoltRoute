package com.example.voltroute.domain.model

/**
 * Represents a complete charging plan for a route
 *
 * Contains all charging stops needed to complete the journey,
 * along with time calculations for total trip duration.
 *
 * Design: Pure domain model with no Android dependencies
 *
 * @param stops List of planned charging stops in order
 * @param totalChargingTimeMinutes Total time spent charging (sum of all stops)
 * @param totalTripTimeMinutes Total journey time (driving + charging)
 * @param routeDurationMinutes Original route driving time without charging
 */
data class ChargingPlan(
    val stops: List<ChargingStop>,
    val totalChargingTimeMinutes: Int,
    val totalTripTimeMinutes: Int,
    val routeDurationMinutes: Int
) {

    /**
     * Formatted charging time for display
     * Examples: "45 min", "2h 15min"
     */
    val totalChargingTimeText: String
        get() = when {
            totalChargingTimeMinutes < 60 -> "$totalChargingTimeMinutes min"
            else -> {
                val hours = totalChargingTimeMinutes / 60
                val mins = totalChargingTimeMinutes % 60
                "${hours}h ${mins}min"
            }
        }

    /**
     * Formatted total trip time for display
     * Examples: "17h 42min", "5h 30min"
     */
    val totalTripTimeText: String
        get() = when {
            totalTripTimeMinutes < 60 -> "$totalTripTimeMinutes min"
            else -> {
                val hours = totalTripTimeMinutes / 60
                val mins = totalTripTimeMinutes % 60
                "${hours}h ${mins}min"
            }
        }

    /**
     * Whether this plan requires any charging stops
     * Returns true if stops are needed, false if can reach on current charge
     */
    val needsCharging: Boolean
        get() = stops.isNotEmpty()
}

/**
 * Represents a single planned charging stop along the route
 *
 * Contains all information about when to stop, what state the battery
 * will be in, and how long to charge.
 *
 * Design: Pure domain model with no Android dependencies
 *
 * @param charger The charging station to stop at
 * @param stopNumber Sequential number (1, 2, 3...) for this stop
 * @param arrivalBatteryPercent Battery percentage when arriving at this charger
 * @param targetBatteryPercent Target charge level (default 80% for battery health)
 * @param estimatedChargeTimeMinutes How many minutes to charge at this stop
 * @param distanceFromStartKm Distance from trip origin to this charging stop
 */
data class ChargingStop(
    val charger: Charger,
    val stopNumber: Int,
    val arrivalBatteryPercent: Double,
    val targetBatteryPercent: Double = 80.0,
    val estimatedChargeTimeMinutes: Int,
    val distanceFromStartKm: Double
) {

    /**
     * Formatted arrival battery for display
     * Example: "Arrive: 15%"
     */
    val arrivalBatteryText: String
        get() = "Arrive: ${arrivalBatteryPercent.toInt()}%"

    /**
     * Formatted charge time for display
     * Example: "45min charge"
     */
    val chargeTimeText: String
        get() = "${estimatedChargeTimeMinutes}min charge"

    /**
     * Formatted distance from start for display
     * Example: "540 km from start"
     */
    val distanceText: String
        get() = "%.0f km from start".format(distanceFromStartKm)
}

