package com.example.voltroute.domain.usecase

import com.example.voltroute.data.remote.repository.RouteRepository
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import javax.inject.Inject

/**
 * Use case for calculating a route between origin and destination
 *
 * This follows Clean Architecture principles by:
 * - Encapsulating business logic
 * - Validating input before calling repository
 * - Providing a single, clear responsibility
 * - Being independent of Android framework
 *
 * Use cases are the entry points to the domain layer from the presentation layer.
 */
class CalculateRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {

    /**
     * Calculate route from origin to destination
     *
     * Validates inputs and delegates to repository for actual API call.
     *
     * @param origin Starting location (current user location)
     * @param destination Destination address or place name
     * @return Result.success with Route if successful, Result.failure if validation fails or API error occurs
     */
    suspend operator fun invoke(origin: Location, destination: String): Result<Route> {
        // Validate destination is not blank
        if (destination.isBlank()) {
            return Result.failure(
                Exception("Please enter a destination")
            )
        }

        // Trim whitespace to handle edge cases
        val trimmedDestination = destination.trim()

        if (trimmedDestination.isEmpty()) {
            return Result.failure(
                Exception("Please enter a destination")
            )
        }

        // Delegate to repository for route calculation
        return routeRepository.getRoute(origin, trimmedDestination)
    }
}

