package com.example.voltroute.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirestoreRepository - Manages Firestore operations for user data
 *
 * Handles CRUD operations for user documents in Firestore.
 * Uses SetOptions.merge() to safely update documents without overwriting.
 *
 * Collection structure:
 * users/
 * ├── {userId}/
 * │   └── UserData fields
 *
 * @param firestore Firebase Firestore instance (injected by Hilt)
 */
@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Save or update user data in Firestore
     *
     * Uses SetOptions.merge() which:
     * - Creates document if it doesn't exist
     * - Updates only provided fields if document exists
     * - Doesn't overwrite existing fields not in userData
     *
     * Safe to call on every login to update lastSyncedAt.
     *
     * @param userData User data to save
     * @return Result with Unit on success, Exception on failure
     */
    suspend fun saveUserData(userData: UserData): Result<Unit> = try {
        firestore.collection("users")
            .document(userData.userId)
            .set(userData, SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get user data from Firestore
     *
     * @param userId Firebase Auth UID
     * @return Result with UserData on success, Exception on failure
     */
    suspend fun getUserData(userId: String): Result<UserData> = try {
        val document = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        val userData = document.toObject(UserData::class.java)
        if (userData != null) {
            Result.success(userData)
        } else {
            Result.failure(Exception("User data not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Delete user data from Firestore
     *
     * @param userId Firebase Auth UID
     * @return Result with Unit on success, Exception on failure
     */
    suspend fun deleteUserData(userId: String): Result<Unit> = try {
        firestore.collection("users")
            .document(userId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Upload trip to Firestore
     *
     * Stores trip in users/{userId}/trips/{tripId} collection.
     * Uses trip.id as document ID for idempotent uploads.
     *
     * @param userId Firebase Auth UID (trip owner)
     * @param trip CloudTrip to upload
     * @return Result with Unit on success, Exception on failure
     */
    suspend fun uploadTrip(userId: String, trip: CloudTrip): Result<Unit> = try {
        firestore.collection("users")
            .document(userId)
            .collection("trips")
            .document(trip.id)
            .set(trip, SetOptions.merge())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Get all trips for a user from Firestore
     *
     * @param userId Firebase Auth UID
     * @return Result with List<CloudTrip> on success, Exception on failure
     */
    suspend fun getAllTrips(userId: String): Result<List<CloudTrip>> = try {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("trips")
            .get()
            .await()

        val trips = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CloudTrip::class.java)
        }
        Result.success(trips)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Delete trip from Firestore
     *
     * Removes trip document from users/{userId}/trips/{tripId}.
     * This triggers REMOVED event in snapshot listeners on other devices.
     *
     * @param userId Firebase Auth UID (trip owner)
     * @param tripId Cloud trip ID (document ID)
     * @return Result with Unit on success, Exception on failure
     */
    suspend fun deleteTrip(userId: String, tripId: String): Result<Unit> = try {
        firestore.collection("users")
            .document(userId)
            .collection("trips")
            .document(tripId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

