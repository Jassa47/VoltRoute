package com.example.voltroute.data.remote.api

import com.example.voltroute.data.remote.dto.ChargerResponseItem
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Open Charge Map API
 *
 * Open Charge Map is a global public registry of EV charging stations.
 * This API allows searching for charging stations near a location.
 *
 * API Documentation: https://openchargemap.org/site/develop/api
 * Base URL: https://api.openchargemap.io/
 */
interface OpenChargeMapApi {

    /**
     * Get charging stations near a specific location
     *
     * Searches for EV charging stations within a radius of the given coordinates.
     * Results are sorted by distance from the search point.
     *
     * @param latitude Search center latitude
     * @param longitude Search center longitude
     * @param distance Search radius (default 50 km)
     * @param distanceUnit Unit for distance parameter (default "km")
     * @param maxResults Maximum number of results to return (default 20)
     * @param compact Return compact response format (default true)
     * @param verbose Include verbose details (default false)
     * @param key Open Charge Map API key
     * @return List of charging stations near the location
     */
    @GET("v3/poi/")
    suspend fun getChargers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") distance: Int = 50,
        @Query("distanceunit") distanceUnit: String = "km",
        @Query("maxresults") maxResults: Int = 20,
        @Query("compact") compact: Boolean = true,
        @Query("verbose") verbose: Boolean = false,
        @Query("key") key: String
    ): List<ChargerResponseItem>
}

