package com.example.voltroute.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TripHistoryEntity - Database representation of a completed trip
 *
 * Stores historical data about past navigation routes including:
 * - Route details (distance, duration, destination)
 * - Vehicle state (battery level, vehicle name)
 * - Charging information (stops count, charging time)
 * - Energy and cost metrics
 * - Trip timestamp
 *
 * Computed properties provide formatted display strings for UI.
 */
@Entity(tableName = "trip_history")
data class TripHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val destinationAddress: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val startBatteryPercent: Double,
    val vehicleName: String,
    val chargingStopsCount: Int,
    val totalChargingTimeMinutes: Int,
    val estimatedCostDollars: Double,
    val energyUsedKWh: Double,
    val tripDate: Long = System.currentTimeMillis()
) {
    /**
     * Formatted date string for display
     * Example: "Feb 17, 2026"
     */
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            .format(Date(tripDate))

    /**
     * Formatted time string for display
     * Example: "3:45 PM"
     */
    val formattedTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault())
            .format(Date(tripDate))

    /**
     * Formatted distance string
     * Example: "245 km"
     */
    val distanceText: String
        get() = "%.0f km".format(distanceKm)

    /**
     * Formatted duration string
     * Example: "45 min" or "2h 30min"
     */
    val durationText: String
        get() = when {
            durationMinutes < 60 -> "$durationMinutes min"
            else -> {
                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                "${hours}h ${mins}min"
            }
        }

    /**
     * Formatted cost string
     * Example: "$12.50"
     */
    val costText: String
        get() = "$%.2f".format(estimatedCostDollars)

    /**
     * Formatted energy usage string
     * Example: "44.1 kWh"
     */
    val energyText: String
        get() = "%.1f kWh".format(energyUsedKWh)
}

