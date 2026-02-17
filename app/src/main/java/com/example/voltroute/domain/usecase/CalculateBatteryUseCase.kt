package com.example.voltroute.domain.usecase

import com.example.voltroute.domain.model.BatteryState
import com.example.voltroute.domain.model.Route
import com.example.voltroute.domain.model.Vehicle
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Use case for calculating battery state and energy requirements
 *
 * Pure domain logic with no Android dependencies or async operations.
 * Calculates:
 * - Current battery status and remaining range
 * - Energy required for a specific route
 * - Whether destination is reachable
 * - Number of charging stops needed
 *
 * Assumptions:
 * - Each charging stop provides 80% charge (realistic fast charging)
 * - Efficiency remains constant (no terrain/weather adjustments yet)
 *
 * @param vehicle Current vehicle configuration and battery status
 * @param route Optional route to calculate energy requirements for
 * @return BatteryState with all current and projected battery information
 */
class CalculateBatteryUseCase @Inject constructor() {

    /**
     * Calculate battery state with optional route energy requirements
     *
     * This is NOT a suspend function - it's pure math that runs instantly.
     * No async operations, no network calls, no database access.
     */
    operator fun invoke(vehicle: Vehicle, route: Route? = null): BatteryState {
        // Step 1: Calculate current energy available (kWh)
        val currentEnergyKWh = vehicle.batteryCapacityKWh * (vehicle.currentBatteryPercent / 100.0)

        // Step 2: Calculate how far vehicle can go on current charge (km)
        val remainingRangeKm = currentEnergyKWh / vehicle.efficiencyKWhPerKm

        // Step 3: If no route, return basic battery state (no route-specific fields)
        if (route == null) {
            return BatteryState(
                batteryCapacityKWh = vehicle.batteryCapacityKWh,
                currentBatteryPercent = vehicle.currentBatteryPercent,
                efficiencyKWhPerKm = vehicle.efficiencyKWhPerKm,
                currentEnergyKWh = currentEnergyKWh,
                requiredEnergyKWh = null,
                routeDistanceKm = null,
                remainingRangeKm = remainingRangeKm,
                canReachDestination = false,
                energyDeficitKWh = null,
                numberOfChargesNeeded = 0,
                percentageUsedForRoute = null
            )
        }

        // Step 4: Calculate energy required for the route (kWh)
        val requiredEnergyKWh = route.distanceKm * vehicle.efficiencyKWhPerKm

        // Step 5: Check if we can reach destination on current charge
        val canReachDestination = currentEnergyKWh >= requiredEnergyKWh

        // Step 6: Calculate what percentage of battery the route will use
        val percentageUsedForRoute = (requiredEnergyKWh / vehicle.batteryCapacityKWh) * 100.0

        // Step 7: If we can reach, return success state
        if (canReachDestination) {
            return BatteryState(
                batteryCapacityKWh = vehicle.batteryCapacityKWh,
                currentBatteryPercent = vehicle.currentBatteryPercent,
                efficiencyKWhPerKm = vehicle.efficiencyKWhPerKm,
                currentEnergyKWh = currentEnergyKWh,
                requiredEnergyKWh = requiredEnergyKWh,
                routeDistanceKm = route.distanceKm,
                remainingRangeKm = remainingRangeKm,
                canReachDestination = true,
                energyDeficitKWh = null,
                numberOfChargesNeeded = 0,
                percentageUsedForRoute = percentageUsedForRoute
            )
        }

        // Step 8: Calculate energy deficit (how much more energy we need)
        val energyDeficitKWh = requiredEnergyKWh - currentEnergyKWh

        // Step 9: Calculate energy gained per charging stop
        // Assumption: Fast charging to 80% (realistic EV behavior)
        // Going from empty to 80% charge gives us 80% of capacity
        val energyPerCharge = vehicle.batteryCapacityKWh * 0.80

        // Step 10: Calculate number of charging stops needed
        // Use ceil to round up (partial charge still requires a stop)
        val numberOfChargesNeeded = ceil(energyDeficitKWh / energyPerCharge).toInt()

        // Step 11: Return battery state with warning information
        return BatteryState(
            batteryCapacityKWh = vehicle.batteryCapacityKWh,
            currentBatteryPercent = vehicle.currentBatteryPercent,
            efficiencyKWhPerKm = vehicle.efficiencyKWhPerKm,
            currentEnergyKWh = currentEnergyKWh,
            requiredEnergyKWh = requiredEnergyKWh,
            routeDistanceKm = route.distanceKm,
            remainingRangeKm = remainingRangeKm,
            canReachDestination = false,
            energyDeficitKWh = energyDeficitKWh,
            numberOfChargesNeeded = numberOfChargesNeeded,
            percentageUsedForRoute = percentageUsedForRoute
        )
    }
}

