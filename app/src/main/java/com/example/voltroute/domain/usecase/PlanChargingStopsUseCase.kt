package com.example.voltroute.domain.usecase

import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.ChargingStop
import com.example.voltroute.domain.model.Route
import com.example.voltroute.domain.model.Vehicle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Use case for planning optimal charging stops along a route
 *
 * HOW THE GREEDY ALGORITHM WORKS:
 *
 * Given: Route 1931km, Battery 108kWh (600km range)
 *
 * Iteration 1:
 *   currentPosition = 0km, range = 600km
 *   lookWindow = 420km to 540km (70%-90% of range)
 *   idealStop = 540km (90% mark)
 *   Best charger near 540km? Tesla Salem OR ✅
 *   → Add stop, reset battery to 80%, advance to 540km
 *
 * Iteration 2:
 *   currentPosition = 540km, range = 600km
 *   lookWindow = 960km to 1080km
 *   Best charger near 1080km? Electrify Redding CA ✅
 *   → Add stop, reset battery to 80%, advance to 1080km
 *
 * Iteration 3:
 *   currentPosition = 1080km, range = 600km
 *   lookWindow = 1500km to 1620km
 *   Best charger near 1620km? ChargePoint Sacramento ✅
 *   → Add stop, reset battery to 80%, advance to 1620km
 *
 * Iteration 4:
 *   currentPosition = 1620km, range = 600km
 *   remainingKm = 1931 - 1620 = 311km
 *   currentRange (600km) >= remaining (311km) → DONE! ✅
 *
 * SCORING: 60% power + 40% proximity to ideal point
 * Why 60/40? Power matters more (faster charge = less wait time)
 * But proximity prevents backtracking or big detours
 *
 * Design: Pure domain logic with no async operations (instant calculation)
 */
class PlanChargingStopsUseCase @Inject constructor() {

    companion object {
        // Always charge to 80% (protects battery health)
        private const val CHARGE_TO_PERCENT = 80.0

        // Start looking for chargers at 70% of remaining range
        private const val START_LOOKING_AT_PERCENT = 0.70

        // Must stop by 10% battery (safety margin)
        private const val MUST_STOP_AT_PERCENT = 0.10

        // Convert charger power (kW) to charge rate (kWh per minute)
        private fun chargeRateKWhPerMin(powerKw: Double) = powerKw / 60.0
    }

    /**
     * Plan charging stops for a route
     *
     * NOT a suspend function - this is pure calculation that runs instantly
     * Uses greedy algorithm to select optimal charging stops
     *
     * @param route The calculated route to destination
     * @param vehicle Current vehicle configuration and battery state
     * @param availableChargers List of chargers found along route
     * @return ChargingPlan with all stops and time calculations
     */
    operator fun invoke(
        route: Route,
        vehicle: Vehicle,
        availableChargers: List<Charger>
    ): ChargingPlan {

        // STEP 1: Check if charging is needed
        val currentEnergyKWh = vehicle.batteryCapacityKWh *
            (vehicle.currentBatteryPercent / 100.0)
        val energyNeededKWh = route.distanceKm * vehicle.efficiencyKWhPerKm

        // Can reach destination on current charge - no stops needed!
        if (currentEnergyKWh >= energyNeededKWh) {
            return ChargingPlan(
                stops = emptyList(),
                totalChargingTimeMinutes = 0,
                totalTripTimeMinutes = route.durationMinutes,
                routeDurationMinutes = route.durationMinutes
            )
        }

        // STEP 2: Decode polyline into GPS points
        val routePoints = PolyUtil.decode(route.polylinePoints)

        // STEP 3: Calculate distance of each charger along the route
        val chargersWithDistance = availableChargers.mapNotNull { charger ->
            val distanceFromStart = calculateDistanceAlongRoute(
                charger, routePoints, route.distanceKm
            )
            if (distanceFromStart != null) {
                Pair(charger, distanceFromStart)
            } else {
                null
            }
        }.sortedBy { it.second }  // Sort by distance from start

        // STEP 4: Greedy selection loop - pick best chargers iteratively
        val selectedStops = mutableListOf<ChargingStop>()
        var currentBatteryPercent = vehicle.currentBatteryPercent
        var currentPositionKm = 0.0
        var maxAttempts = 10  // Safety limit to prevent infinite loop

        while (currentPositionKm < route.distanceKm && maxAttempts > 0) {
            maxAttempts--

            // Calculate current range based on battery level
            val currentEnergyKWh2 = vehicle.batteryCapacityKWh *
                (currentBatteryPercent / 100.0)
            val currentRangeKm = currentEnergyKWh2 / vehicle.efficiencyKWhPerKm
            val remainingKm = route.distanceKm - currentPositionKm

            // Can reach destination? We're done!
            if (currentRangeKm >= remainingKm) {
                break
            }

            // Define search window for next charging stop
            val lookStartKm = currentPositionKm +
                (currentRangeKm * START_LOOKING_AT_PERCENT)  // Start at 70% range
            val lookEndKm = currentPositionKm +
                (currentRangeKm * (1.0 - MUST_STOP_AT_PERCENT))  // Must stop by 90%
            val idealStopKm = currentPositionKm +
                (currentRangeKm * 0.90)  // Ideal = 90% of range

            // Find best charger in the window
            val bestCharger = findBestCharger(
                chargersWithDistance,
                lookStartKm,
                lookEndKm,
                idealStopKm,
                alreadySelected = selectedStops.map { it.charger.id }
            )

            if (bestCharger == null) {
                // No charger in ideal window - find nearest available charger
                val nearestCharger = chargersWithDistance.filter { (charger, dist) ->
                    dist > currentPositionKm &&
                    dist <= currentPositionKm + currentRangeKm &&
                    charger.id !in selectedStops.map { it.charger.id }
                }.maxByOrNull { (charger, _) -> charger.powerKw }

                if (nearestCharger == null) {
                    // No chargers reachable - stop planning
                    break
                }

                // Create stop for nearest charger
                val stop = createChargingStop(
                    charger = nearestCharger.first,
                    stopNumber = selectedStops.size + 1,
                    currentPositionKm = currentPositionKm,
                    stopDistanceKm = nearestCharger.second,
                    currentBatteryPercent = currentBatteryPercent,
                    vehicle = vehicle
                )
                selectedStops.add(stop)

                // Update position and battery for next iteration
                currentPositionKm = nearestCharger.second
                currentBatteryPercent = CHARGE_TO_PERCENT
                continue
            }

            // Create stop for best charger
            val stop = createChargingStop(
                charger = bestCharger.first,
                stopNumber = selectedStops.size + 1,
                currentPositionKm = currentPositionKm,
                stopDistanceKm = bestCharger.second,
                currentBatteryPercent = currentBatteryPercent,
                vehicle = vehicle
            )
            selectedStops.add(stop)

            // Update position and battery for next iteration
            currentPositionKm = bestCharger.second
            currentBatteryPercent = CHARGE_TO_PERCENT
        }

        // STEP 5: Calculate totals and return plan
        val totalChargingMins = selectedStops.sumOf { it.estimatedChargeTimeMinutes }
        val totalTripMins = route.durationMinutes + totalChargingMins

        return ChargingPlan(
            stops = selectedStops,
            totalChargingTimeMinutes = totalChargingMins,
            totalTripTimeMinutes = totalTripMins,
            routeDurationMinutes = route.durationMinutes
        )
    }

    /**
     * Find the best charger in a given search window
     *
     * Scoring algorithm:
     * - 60% weight on charging power (faster = better)
     * - 40% weight on proximity to ideal stop point
     *
     * @param chargersWithDistance All chargers sorted by distance
     * @param lookStartKm Start of search window
     * @param lookEndKm End of search window
     * @param idealStopKm Ideal stop location (90% of range)
     * @param alreadySelected IDs of chargers already in plan
     * @return Best charger and its distance, or null if none found
     */
    private fun findBestCharger(
        chargersWithDistance: List<Pair<Charger, Double>>,
        lookStartKm: Double,
        lookEndKm: Double,
        idealStopKm: Double,
        alreadySelected: List<String>
    ): Pair<Charger, Double>? {

        // Filter candidates in the search window
        val candidates = chargersWithDistance.filter { (charger, dist) ->
            dist in lookStartKm..lookEndKm &&
            charger.id !in alreadySelected
        }

        if (candidates.isEmpty()) {
            return null
        }

        // Find maximum power for normalization
        val maxPower = candidates.maxOf { it.first.powerKw }

        // Score each candidate and pick the best
        return candidates.maxByOrNull { (charger, dist) ->
            // Power score: 0-100 based on charging power
            val powerScore = (charger.powerKw / maxPower) * 100.0

            // Proximity score: 0-100 based on distance from ideal point
            val maxDeviation = lookEndKm - lookStartKm
            val deviation = abs(dist - idealStopKm)
            val proximityScore = ((maxDeviation - deviation) / maxDeviation) * 100.0

            // Final score: 60% power + 40% proximity
            (powerScore * 0.6) + (proximityScore * 0.4)
        }
    }

    /**
     * Calculate how far along the route a charger is located
     *
     * Uses SphericalUtil to find the closest point on the route polyline
     * Filters out chargers that are too far from the route (>30km)
     *
     * @param charger The charging station to locate
     * @param routePoints Decoded polyline points
     * @param totalRouteKm Total route distance in kilometers
     * @return Distance from start in km, or null if charger too far off route
     */
    private fun calculateDistanceAlongRoute(
        charger: Charger,
        routePoints: List<LatLng>,
        totalRouteKm: Double
    ): Double? {
        if (routePoints.isEmpty()) {
            return null
        }

        val chargerLatLng = LatLng(
            charger.location.latitude,
            charger.location.longitude
        )

        // Find closest point on route
        var minDistance = Double.MAX_VALUE
        var closestIndex = 0

        routePoints.forEachIndexed { index, routePoint ->
            val distance = SphericalUtil.computeDistanceBetween(
                chargerLatLng,
                routePoint
            )  // Returns distance in meters

            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        // Filter out chargers too far from route (>30km)
        if (minDistance > 30_000) {  // 30,000 meters = 30km
            return null
        }

        // Calculate distance from start based on position in polyline
        val fractionAlongRoute = closestIndex.toDouble() / routePoints.size
        return fractionAlongRoute * totalRouteKm
    }

    /**
     * Create a charging stop with all calculated fields
     *
     * Calculates:
     * - Battery level on arrival
     * - Energy needed to charge
     * - Time required to charge
     *
     * @param charger The charging station
     * @param stopNumber Sequential stop number
     * @param currentPositionKm Current position on route
     * @param stopDistanceKm Position of this charger on route
     * @param currentBatteryPercent Current battery percentage
     * @param vehicle Vehicle configuration
     * @return Complete ChargingStop with all fields calculated
     */
    private fun createChargingStop(
        charger: Charger,
        stopNumber: Int,
        currentPositionKm: Double,
        stopDistanceKm: Double,
        currentBatteryPercent: Double,
        vehicle: Vehicle
    ): ChargingStop {

        // Calculate energy consumed driving to this stop
        val distanceToStop = stopDistanceKm - currentPositionKm
        val energyUsedKWh = distanceToStop * vehicle.efficiencyKWhPerKm

        // Calculate battery state on arrival
        val energyAtArrival = (vehicle.batteryCapacityKWh *
            currentBatteryPercent / 100.0) - energyUsedKWh
        val arrivalBatteryPercent = (energyAtArrival /
            vehicle.batteryCapacityKWh * 100.0).coerceAtLeast(1.0)

        // Calculate how much energy to add
        val currentEnergyKWh = energyAtArrival.coerceAtLeast(0.0)
        val targetEnergyKWh = vehicle.batteryCapacityKWh *
            (CHARGE_TO_PERCENT / 100.0)
        val energyToAddKWh = (targetEnergyKWh - currentEnergyKWh).coerceAtLeast(0.0)

        // Calculate charging time based on charger power
        val chargeRate = chargeRateKWhPerMin(charger.powerKw)
        val chargeTimeMinutes = if (chargeRate > 0) {
            ceil(energyToAddKWh / chargeRate).roundToInt()
        } else {
            60  // Default 60 minutes if power unknown
        }

        return ChargingStop(
            charger = charger,
            stopNumber = stopNumber,
            arrivalBatteryPercent = arrivalBatteryPercent,
            targetBatteryPercent = CHARGE_TO_PERCENT,
            estimatedChargeTimeMinutes = chargeTimeMinutes,
            distanceFromStartKm = stopDistanceKm
        )
    }
}

