package com.example.voltroute.data.remote.api

import com.example.voltroute.data.remote.dto.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Google Directions API
 *
 * This API calculates routes between locations and returns:
 * - Distance and duration
 * - Encoded polyline for map display
 * - Step-by-step directions
 *
 * API Documentation: https://developers.google.com/maps/documentation/directions/overview
 */
interface DirectionsApi {

    /**
     * Get directions between origin and destination
     *
     * @param origin Starting point as "latitude,longitude" or address string
     * @param destination End point as "latitude,longitude" or address string
     * @param key Google Maps API key for authentication
     * @param mode Travel mode (driving, walking, bicycling, transit). Default: "driving"
     * @return DirectionsResponse containing route information
     */
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String,
        @Query("mode") mode: String = "driving"
    ): DirectionsResponse
}

