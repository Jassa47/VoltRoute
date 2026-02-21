package com.example.voltroute.data.remote.firestore

import com.google.firebase.Timestamp

/**
 * CloudTrip - Firestore data model for trip history
 *
 * Represents a trip stored in Firestore cloud database.
 *
 * Firestore document structure:
 * users/{userId}/trips/{tripId}
 *
 * Used for:
 * - Cross-device synchronization
 * - Cloud backup of trip history
 * - Real-time sync via snapshot listeners
 *
 * Mirrors TripHistoryEntity but uses Firestore-friendly types:
 * - Timestamp instead of Long for dates
 * - String ID instead of auto-generated Long
 */
data class CloudTrip(
    val id: String = "",
    val destinationAddress: String = "",
    val distanceKm: Double = 0.0,
    val durationMinutes: Int = 0,
    val startBatteryPercent: Double = 0.0,
    val vehicleName: String = "",
    val chargingStopsCount: Int = 0,
    val totalChargingTimeMinutes: Int = 0,
    val estimatedCostDollars: Double = 0.0,
    val energyUsedKWh: Double = 0.0,
    val tripDate: Timestamp = Timestamp.now()
)

