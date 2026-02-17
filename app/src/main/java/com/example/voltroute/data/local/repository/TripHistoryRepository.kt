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
}

