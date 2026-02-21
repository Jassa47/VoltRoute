package com.example.voltroute.data.remote.firestore

import com.google.firebase.Timestamp

/**
 * UserData - Firestore user document model
 *
 * Represents user settings and preferences stored in Firestore.
 * Separate from Firebase Auth (which only stores uid/email/password).
 *
 * Firestore structure:
 * users (collection)
 * └── {userId} (document)
 *     ├── userId: String
 *     ├── email: String?
 *     ├── phoneNumber: String?
 *     ├── selectedVehiclePresetId: String
 *     ├── currentBatteryPercent: Double
 *     ├── electricityCostPerKWh: Double
 *     ├── isDarkMode: Boolean
 *     ├── createdAt: Timestamp
 *     └── lastSyncedAt: Timestamp
 *
 * @property userId Firebase Auth UID
 * @property email User's email (null for phone auth users)
 * @property phoneNumber User's phone (null for email auth users)
 * @property selectedVehiclePresetId Vehicle preset ID ("rivian_r1t" default)
 * @property currentBatteryPercent Current battery level (80% default)
 * @property electricityCostPerKWh Cost per kWh for charging calculations ($0.15 default)
 * @property isDarkMode Dark theme preference (false default)
 * @property createdAt Account creation timestamp
 * @property lastSyncedAt Last update timestamp (updated on each login)
 */
data class UserData(
    val userId: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val selectedVehiclePresetId: String = "rivian_r1t",
    val currentBatteryPercent: Double = 80.0,
    val electricityCostPerKWh: Double = 0.15,
    val isDarkMode: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val lastSyncedAt: Timestamp = Timestamp.now()
)

