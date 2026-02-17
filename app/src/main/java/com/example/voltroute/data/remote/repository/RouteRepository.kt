package com.example.voltroute.data.remote.repository

import com.example.voltroute.data.remote.api.DirectionsApi
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repository for fetching and managing route data
 *
 * Responsibilities:
 * - Call Google Directions API via Retrofit
 * - Transform API DTOs to domain models
 * - Handle API errors and edge cases
 * - Switch to IO dispatcher for network calls
 *
 * Uses Result<T> for functional error handling
 */
@Singleton
class RouteRepository @Inject constructor(
    private val directionsApi: DirectionsApi,
    @Named("maps_api_key") private val apiKey: String
) {

    /**
     * Calculate route from origin to destination
     *
     * @param origin Starting location (will be converted to "lat,lng" format)
     * @param destination Destination address or coordinates
     * @return Result.success with Route on success, Result.failure on error
     */
    suspend fun getRoute(origin: Location, destination: String): Result<Route> =
        withContext(Dispatchers.IO) {
            try {
                // Convert origin location to "lat,lng" format required by API
                val originString = "${origin.latitude},${origin.longitude}"

                // Call Directions API
                val response = directionsApi.getDirections(
                    origin = originString,
                    destination = destination,
                    key = apiKey,
                    mode = "driving"
                )

                // Check API response status
                when (response.status) {
                    "OK" -> {
                        // Extract first route and first leg (typical for single destination)
                        val routeDto = response.routes.firstOrNull()
                            ?: return@withContext Result.failure(
                                Exception("No routes found in response")
                            )

                        val legDto = routeDto.legs.firstOrNull()
                            ?: return@withContext Result.failure(
                                Exception("No legs found in route")
                            )

                        // Map DTO to domain model
                        val route = Route(
                            distanceMeters = legDto.distance.value,
                            durationSeconds = legDto.duration.value,
                            polylinePoints = routeDto.overviewPolyline.points,
                            startLocation = Location(
                                latitude = legDto.startLocation.lat,
                                longitude = legDto.startLocation.lng,
                                name = origin.name
                            ),
                            endLocation = Location(
                                latitude = legDto.endLocation.lat,
                                longitude = legDto.endLocation.lng,
                                name = destination
                            )
                        )

                        Result.success(route)
                    }

                    "ZERO_RESULTS" -> Result.failure(
                        Exception("No route found to destination")
                    )

                    "NOT_FOUND" -> Result.failure(
                        Exception("Destination not found")
                    )

                    "INVALID_REQUEST" -> Result.failure(
                        Exception("Invalid request. Please check your destination")
                    )

                    "REQUEST_DENIED" -> Result.failure(
                        Exception("API request denied. Please check API key")
                    )

                    "OVER_QUERY_LIMIT" -> Result.failure(
                        Exception("API query limit exceeded. Please try again later")
                    )

                    else -> Result.failure(
                        Exception("Could not calculate route. Status: ${response.status}")
                    )
                }

            } catch (e: Exception) {
                // Handle network errors, JSON parsing errors, etc.
                Result.failure(
                    Exception("Could not calculate route. Please try again", e)
                )
            }
        }
}

