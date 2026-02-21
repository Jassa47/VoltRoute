package com.example.voltroute.domain.usecase

import com.example.voltroute.data.remote.firestore.FirestoreRepository
import com.example.voltroute.data.remote.firestore.UserData
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

/**
 * CreateUserDocumentUseCase - Creates or updates Firestore user document
 *
 * Called after successful authentication (sign up or sign in).
 * Creates a Firestore document with user settings and preferences.
 *
 * Uses SetOptions.merge() in repository, so:
 * - First call (signup): Creates new document with defaults
 * - Subsequent calls (signin): Updates lastSyncedAt only
 * - Never overwrites existing user settings
 *
 * Why separate from auth:
 * - Firebase Auth: Identity (uid, email, password)
 * - Firestore: App data (settings, preferences, trips)
 * - Separation of concerns
 *
 * @param firestoreRepository Repository for Firestore operations
 */
class CreateUserDocumentUseCase @Inject constructor(
    private val firestoreRepository: FirestoreRepository
) {
    /**
     * Create or update user document in Firestore
     *
     * Extracts user info from FirebaseUser and creates UserData object
     * with sensible defaults for new users.
     *
     * @param user Firebase authenticated user
     * @return Result with Unit on success, Exception on failure
     */
    suspend operator fun invoke(user: FirebaseUser): Result<Unit> {
        // Build UserData from FirebaseUser with default app settings
        val userData = UserData(
            userId = user.uid,
            email = user.email,
            phoneNumber = user.phoneNumber,
            selectedVehiclePresetId = "rivian_r1t", // Default vehicle
            currentBatteryPercent = 80.0,           // Default battery level
            electricityCostPerKWh = 0.15,           // Default electricity cost ($0.15/kWh)
            isDarkMode = false                      // Default to light theme
            // createdAt and lastSyncedAt use defaults from UserData (Timestamp.now())
        )

        // Save to Firestore (merge mode - won't overwrite existing data)
        return firestoreRepository.saveUserData(userData)
    }
}
