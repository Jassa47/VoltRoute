package com.example.voltroute.data.remote.sync

import android.util.Log
import com.example.voltroute.data.auth.AuthRepository
import com.example.voltroute.data.local.repository.TripHistoryRepository
import com.example.voltroute.data.remote.firestore.CloudTrip
import com.example.voltroute.data.remote.firestore.FirestoreRepository
import com.example.voltroute.data.remote.firestore.UserData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SyncManager - Orchestrates cloud synchronization between local Room database and Firestore
 *
 * Responsibilities:
 * - Upload pending trips from Room to Firestore
 * - Download new/updated trips from Firestore to Room (real-time)
 * - Upload user settings (vehicle, battery, preferences) to Firestore
 * - Track sync state (Idle, Syncing, Success, Error)
 * - Track last sync timestamp
 *
 * Sync Strategy:
 * 1. Check user authentication
 * 2. Get all unsynced trips from local database
 * 3. Convert each to CloudTrip format
 * 4. Upload to Firestore
 * 5. Mark as synced in local database
 * 6. Listen for real-time changes from Firestore
 *
 * Real-time Sync:
 * - Firestore snapshot listener monitors user's trips collection
 * - Automatically downloads new trips from other devices
 * - Handles updates and deletions
 * - Conflict resolution using lastModified timestamp
 *
 * Error Handling:
 * - Continues uploading remaining trips if one fails
 * - Updates sync state with error message
 * - Logs all operations for debugging
 *
 * @Singleton ensures single instance manages all sync operations
 */
@Singleton
class SyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val tripHistoryRepository: TripHistoryRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    // CoroutineScope for real-time listener callbacks
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Firestore snapshot listener registration
    private var tripsListener: ListenerRegistration? = null

    // Sync state exposed as StateFlow for reactive UI updates
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Last sync timestamp (null if never synced)
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    /**
     * Manual sync - uploads pending trips and user settings
     *
     * Called when:
     * - User taps "Sync Now" button in settings
     * - App starts (auto-sync on launch)
     * - New trip is saved (auto-upload)
     *
     * Steps:
     * 1. Verify user is logged in
     * 2. Set state to Syncing
     * 3. Upload all unsynced trips
     * 4. Update sync state and timestamp
     *
     * @return Result.success if all uploads succeed, Result.failure if any error occurs
     */
    suspend fun syncNow(): Result<Unit> {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _syncState.value = SyncState.Error("Not logged in")
            return Result.failure(Exception("Not logged in"))
        }

        _syncState.value = SyncState.Syncing
        Log.d(TAG, "Starting manual sync for user: ${currentUser.uid}")

        return try {
            // Upload pending trips
            uploadPendingTrips(currentUser.uid)

            Log.d(TAG, "Sync completed successfully")
            _syncState.value = SyncState.Success("Synced successfully")
            _lastSyncTime.value = System.currentTimeMillis()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    /**
     * Upload trips that haven't been synced yet
     *
     * Process:
     * 1. Get all trips with isSynced = false from Room
     * 2. For each trip:
     *    a. Convert TripHistoryEntity to CloudTrip
     *    b. Upload to Firestore
     *    c. Mark as synced in Room with Firestore ID
     * 3. Continue with remaining trips even if one fails
     *
     * Conflict Resolution:
     * - Uses generateSyncId() to create unique ID if trip has no syncId yet
     * - Firestore document ID is stored in local syncId field
     * - This links local and cloud copies of the same trip
     *
     * @param userId Firebase user ID (owner of the trips)
     */
    private suspend fun uploadPendingTrips(userId: String) {
        Log.d(TAG, "Uploading pending trips...")

        val unsyncedTrips = tripHistoryRepository.getUnsyncedTrips()
        Log.d(TAG, "Found ${unsyncedTrips.size} unsynced trips")

        unsyncedTrips.forEach { localTrip ->
            try {
                // Convert to CloudTrip
                val cloudTrip = CloudTrip(
                    id = localTrip.syncId ?: generateSyncId(localTrip.id),
                    destinationAddress = localTrip.destinationAddress,
                    distanceKm = localTrip.distanceKm,
                    durationMinutes = localTrip.durationMinutes,
                    startBatteryPercent = localTrip.startBatteryPercent,
                    vehicleName = localTrip.vehicleName,
                    chargingStopsCount = localTrip.chargingStopsCount,
                    totalChargingTimeMinutes = localTrip.totalChargingTimeMinutes,
                    estimatedCostDollars = localTrip.estimatedCostDollars,
                    energyUsedKWh = localTrip.energyUsedKWh,
                    tripDate = Timestamp(java.util.Date(localTrip.tripDate))
                )

                // Upload to Firestore
                firestoreRepository.uploadTrip(userId, cloudTrip)
                    .onSuccess {
                        // Mark as synced in local database
                        tripHistoryRepository.markAsSynced(
                            localId = localTrip.id,
                            syncId = cloudTrip.id
                        )
                        Log.d(TAG, "Uploaded trip: ${cloudTrip.id}")
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Failed to upload trip ${localTrip.id}", e)
                        throw e
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading trip ${localTrip.id}", e)
                // Continue with other trips even if one fails
            }
        }
    }

    /**
     * Generate unique sync ID for a trip
     *
     * Format: "trip_{currentTimestamp}_{localId}"
     * Example: "trip_1708534800000_42"
     *
     * This ensures:
     * - Uniqueness across all users and devices
     * - Sortable by creation time
     * - Traceable to original local ID
     *
     * @param localId The local Room database ID
     * @return Unique sync ID for Firestore
     */
    private fun generateSyncId(localId: Long): String {
        return "trip_${System.currentTimeMillis()}_$localId"
    }

    /**
     * Upload user settings to Firestore
     *
     * Called when user changes:
     * - Vehicle selection
     * - Battery percentage
     * - Electricity cost
     * - Dark mode preference
     *
     * Creates UserData object with current settings and uploads to Firestore.
     * These settings will sync across devices when user signs in.
     *
     * @param userId Firebase user ID
     * @param selectedVehiclePresetId Vehicle preset ID (e.g., "rivian_r1t")
     * @param currentBatteryPercent Current battery level (0-100)
     * @param electricityCostPerKWh Cost per kWh in dollars
     * @param isDarkMode Dark theme enabled flag
     * @return Result indicating success or failure
     */
    suspend fun uploadUserSettings(
        userId: String,
        selectedVehiclePresetId: String,
        currentBatteryPercent: Double,
        electricityCostPerKWh: Double,
        isDarkMode: Boolean
    ): Result<Unit> {
        val currentUser = authRepository.currentUser ?: return Result.failure(
            Exception("Not logged in")
        )

        val userData = UserData(
            userId = userId,
            email = currentUser.email,
            phoneNumber = currentUser.phoneNumber,
            selectedVehiclePresetId = selectedVehiclePresetId,
            currentBatteryPercent = currentBatteryPercent,
            electricityCostPerKWh = electricityCostPerKWh,
            isDarkMode = isDarkMode,
            lastSyncedAt = Timestamp.now()
        )

        return firestoreRepository.saveUserData(userData)
    }

    /**
     * Clear sync state back to Idle
     *
     * Called after user dismisses success/error message.
     */
    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * Start listening for changes in Firestore
     *
     * Sets up real-time snapshot listener on user's trips collection.
     * Automatically downloads new/updated trips from other devices.
     *
     * Process:
     * 1. Stop any existing listener
     * 2. Create snapshot listener on user's trips collection
     * 3. On each document change:
     *    - ADDED/MODIFIED: Download and merge trip
     *    - REMOVED: Delete from local database
     *
     * Listener remains active until stopRealtimeSync() is called.
     *
     * @param userId Firebase user ID to listen for
     */
    fun startRealtimeSync(userId: String) {
        Log.d(TAG, "Starting realtime sync for user: $userId")

        // Stop existing listener if any
        stopRealtimeSync()

        // Listen to user's trips collection
        tripsListener = firestore.collection("users")
            .document(userId)
            .collection("trips")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Realtime sync error", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    viewModelScope.launch {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                downloadAndMergeTrip(change.document.toObject(CloudTrip::class.java))
                            }
                            DocumentChange.Type.REMOVED -> {
                                deleteLocalTrip(change.document.id)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Stop real-time sync listener
     *
     * Removes Firestore snapshot listener to stop receiving updates.
     * Called when user signs out or app is destroyed.
     */
    fun stopRealtimeSync() {
        tripsListener?.remove()
        tripsListener = null
        Log.d(TAG, "Stopped realtime sync")
    }

    /**
     * Download and merge trip from Firestore into local database
     *
     * Conflict Resolution Strategy (Last-Write-Wins):
     * 1. Check if trip exists locally by syncId
     * 2. If exists:
     *    - Compare lastModified timestamps
     *    - If cloud version is newer: update local
     *    - If local version is newer: skip (keep local)
     * 3. If doesn't exist: insert as new trip
     *
     * This ensures:
     * - No duplicate trips
     * - Most recent version always wins
     * - Changes from any device propagate to all devices
     *
     * @param cloudTrip The trip downloaded from Firestore
     */
    private suspend fun downloadAndMergeTrip(cloudTrip: CloudTrip) {
        try {
            // Convert to local entity
            val localTrip = TripHistoryEntity(
                id = 0, // Room will assign or use existing
                destinationAddress = cloudTrip.destinationAddress,
                distanceKm = cloudTrip.distanceKm,
                durationMinutes = cloudTrip.durationMinutes,
                startBatteryPercent = cloudTrip.startBatteryPercent,
                vehicleName = cloudTrip.vehicleName,
                chargingStopsCount = cloudTrip.chargingStopsCount,
                totalChargingTimeMinutes = cloudTrip.totalChargingTimeMinutes,
                estimatedCostDollars = cloudTrip.estimatedCostDollars,
                energyUsedKWh = cloudTrip.energyUsedKWh,
                tripDate = cloudTrip.tripDate.toDate().time,
                syncId = cloudTrip.id,
                lastModified = cloudTrip.tripDate.toDate().time,
                isSynced = true
            )

            // Check if exists locally
            val existing = tripHistoryRepository.getTripBySyncId(cloudTrip.id)

            if (existing != null) {
                // Conflict resolution: last-write-wins
                if (localTrip.lastModified > existing.lastModified) {
                    tripHistoryRepository.updateTrip(localTrip.copy(id = existing.id))
                    Log.d(TAG, "Updated trip from cloud: ${cloudTrip.id}")
                } else {
                    Log.d(TAG, "Local trip is newer, skipping: ${cloudTrip.id}")
                }
            } else {
                // New trip from cloud
                tripHistoryRepository.insertTrip(localTrip)
                Log.d(TAG, "Downloaded new trip: ${cloudTrip.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading trip ${cloudTrip.id}", e)
        }
    }

    /**
     * Delete local trip when removed from Firestore
     *
     * Called when snapshot listener detects REMOVED event.
     * Keeps local database in sync with Firestore deletions.
     *
     * @param syncId Firestore document ID of deleted trip
     */
    private suspend fun deleteLocalTrip(syncId: String) {
        try {
            val localTrip = tripHistoryRepository.getTripBySyncId(syncId)
            if (localTrip != null) {
                tripHistoryRepository.deleteTrip(localTrip)
                Log.d(TAG, "Deleted trip from cloud sync: $syncId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting trip $syncId", e)
        }
    }

    /**
     * Delete trip from Firestore
     *
     * Called when user deletes a synced trip from local database.
     * Removes trip from Firestore so it's deleted on all devices.
     *
     * @param userId Firebase user ID
     * @param syncId Firestore document ID of trip to delete
     */
    suspend fun deleteTrip(userId: String, syncId: String) {
        try {
            firestoreRepository.deleteTrip(userId, syncId)
                .onSuccess {
                    Log.d(TAG, "Deleted trip from Firestore: $syncId")
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to delete trip from Firestore: $syncId", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting trip $syncId from Firestore", e)
        }
    }
}

