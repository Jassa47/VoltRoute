package com.example.voltroute.domain.usecase

import com.example.voltroute.data.local.repository.TripHistoryRepository
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.Route
import com.example.voltroute.domain.model.Vehicle
import javax.inject.Inject

/**
 * SaveTripHistoryUseCase - Domain layer use case for saving trip history
 *
 * Part of Clean Architecture's domain layer.
 * Provides simple interface for MapViewModel to save trips
 * without knowing about repository implementation details.
 *
 * Called automatically after successful route calculation
 * to build a historical record of user's trips.
 */
class SaveTripHistoryUseCase @Inject constructor(
    private val tripHistoryRepository: TripHistoryRepository
) {

    /**
     * Save a completed trip to history
     *
     * Invokable as a function: saveTripHistoryUseCase(...)
     *
     * @param route The calculated route
     * @param vehicle Current vehicle state
     * @param vehicleName Display name of vehicle (e.g., "Rivian R1T")
     * @param destinationAddress User-entered destination
     * @param chargingPlan Charging plan if needed (null otherwise)
     * @param estimatedCostDollars Cost estimate (default 0.0 for now)
     */
    suspend operator fun invoke(
        route: Route,
        vehicle: Vehicle,
        vehicleName: String,
        destinationAddress: String,
        chargingPlan: ChargingPlan?,
        estimatedCostDollars: Double = 0.0
    ) {
        tripHistoryRepository.saveTrip(
            route = route,
            vehicle = vehicle,
            vehicleName = vehicleName,
            destinationAddress = destinationAddress,
            chargingPlan = chargingPlan,
            estimatedCostDollars = estimatedCostDollars
        )
    }
}

