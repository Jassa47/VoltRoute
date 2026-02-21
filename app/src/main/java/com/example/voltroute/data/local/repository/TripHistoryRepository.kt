package com.example.voltroute.data.local.repository

import com.example.voltroute.data.local.dao.TripHistoryDao
import com.example.voltroute.data.local.entity.TripHistoryEntity
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.Route
import com.example.voltroute.domain.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TripHistoryRepository - Repository layer for trip history data
 *
 * Provides clean abstraction over database operations.
 * Handles:
 * - Converting domain models to database entities
 * - Thread management (withContext for IO operations)
 * - Exposing Flow for reactive UI updates
 *
 * @Singleton ensures single instance across app
 */
@Singleton
class TripHistoryRepository @Inject constructor(
    private val tripHistoryDao: TripHistoryDao
) {

    /**
     * Get all trips as a Flow
     *
     * NO suspend keyword - just returns the Flow from DAO.
     * Flow is lazy and doesn't need suspend.
     *
     * Emits new list whenever database changes:
     * - Trip inserted → emits updated list
     * - Trip deleted → emits updated list
     * - All cleared → emits empty list
     *
     * ViewModel converts this to StateFlow for UI consumption.
     */
    fun getAllTrips(): Flow<List<TripHistoryEntity>> {
        return tripHistoryDao.getAllTrips()
    }

    /**
     * Save a completed trip to history
     *
     * Converts domain models (Route, Vehicle, ChargingPlan) into
     * a database entity and persists it.
     *
     * Called automatically after successful route calculation.
     *
     * @param route The calculated route
     * @param vehicle Current vehicle state (for battery and efficiency)
     * @param vehicleName Display name of the vehicle
     * @param destinationAddress User-entered destination
     * @param chargingPlan Charging plan (null if no charging needed)
     * @param estimatedCostDollars Cost estimate for the trip
     */
    suspend fun saveTrip(
        route: Route,
        vehicle: Vehicle,
        vehicleName: String,
        destinationAddress: String,
        chargingPlan: ChargingPlan?,
        estimatedCostDollars: Double
    ) {
        // Switch to IO thread for database operation
        withContext(Dispatchers.IO) {
            val entity = TripHistoryEntity(
                destinationAddress = destinationAddress,
                distanceKm = route.distanceKm,
                durationMinutes = route.durationMinutes,
                startBatteryPercent = vehicle.currentBatteryPercent,
                vehicleName = vehicleName,
                chargingStopsCount = chargingPlan?.stops?.size ?: 0,
                totalChargingTimeMinutes = chargingPlan?.totalChargingTimeMinutes ?: 0,
                estimatedCostDollars = estimatedCostDollars,
                energyUsedKWh = route.distanceKm * vehicle.efficiencyKWhPerKm
            )

            tripHistoryDao.insertTrip(entity)
        }
    }

    /**
     * Delete a specific trip from history
     *
     * @param trip The trip entity to delete
     */
    suspend fun deleteTrip(trip: TripHistoryEntity) {
        withContext(Dispatchers.IO) {
            tripHistoryDao.deleteTrip(trip)
        }
    }

    /**
     * Clear all trip history
     *
     * Useful for "Clear History" feature in settings.
     */
    suspend fun clearAllTrips() {
        withContext(Dispatchers.IO) {
            tripHistoryDao.clearAllTrips()
        }
    }

    // NEW SYNC METHODS - For cloud synchronization

    /**
     * Get trips that haven't been synced to cloud yet
     *
     * Returns all trips where isSynced = false.
     * These trips need to be uploaded to Firestore.
     *
     * @return List of unsynced trips
     */
    suspend fun getUnsyncedTrips(): List<TripHistoryEntity> =
        withContext(Dispatchers.IO) {
            tripHistoryDao.getUnsyncedTrips()
        }

    /**
     * Mark trip as synced after successful upload
     *
     * Updates the trip to:
     * - Set isSynced = true
     * - Store Firestore document ID in syncId
     *
     * Called after successfully uploading trip to Firestore.
     *
     * @param localId The local database ID of the trip
     * @param syncId The Firestore document ID
     */
    suspend fun markAsSynced(localId: Long, syncId: String) =
        withContext(Dispatchers.IO) {
            tripHistoryDao.markAsSynced(localId, syncId)
        }

    /**
     * Update trip's last modified timestamp
     *
     * Used for conflict resolution when syncing changes.
     * The trip with the newest lastModified timestamp wins.
     *
     * @param localId The local database ID
     * @param timestamp The new modification timestamp in milliseconds
     */
    suspend fun updateLastModified(localId: Long, timestamp: Long) =
        withContext(Dispatchers.IO) {
            tripHistoryDao.updateLastModified(localId, timestamp)
        }

    /**
     * Find trip by Firestore sync ID
     *
     * Used when downloading trips from cloud to check if trip already exists locally.
     * Helps avoid duplicate trips when syncing down.
     *
     * @param syncId The Firestore document ID
     * @return Trip entity or null if not found
     */
    suspend fun getTripBySyncId(syncId: String): TripHistoryEntity? =
        withContext(Dispatchers.IO) {
            tripHistoryDao.getTripBySyncId(syncId)
        }

    /**
     * Update existing trip (for sync merge)
     *
     * Used when downloading trip from cloud that already exists locally.
     * Replaces local trip data with cloud version after conflict resolution.
     *
     * @param trip Trip entity with updated data
     */
    suspend fun updateTrip(trip: TripHistoryEntity) =
        withContext(Dispatchers.IO) {
            tripHistoryDao.upsertTrip(trip)
        }

    /**
     * Insert trip (wrapper for clarity)
     *
     * Used when downloading new trip from cloud that doesn't exist locally.
     * Also used when saving local trips.
     *
     * @param trip Trip entity to insert
     * @return The ID of the inserted trip
     */
    suspend fun insertTrip(trip: TripHistoryEntity): Long =
        withContext(Dispatchers.IO) {
            tripHistoryDao.insertTrip(trip)
        }
}

