package com.example.voltroute.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) for Google Directions API response
 *
 * These classes map directly to the JSON structure returned by Google's API.
 * We use @SerializedName to handle snake_case field names from the API.
 *
 * Example API response structure:
 * {
 *   "routes": [ { "legs": [...], "overview_polyline": {...} } ],
 *   "status": "OK"
 * }
 */

/**
 * Root response from Directions API
 *
 * @param routes List of possible routes (usually only one for driving)
 * @param status API response status (OK, ZERO_RESULTS, NOT_FOUND, etc.)
 */
data class DirectionsResponse(
    @SerializedName("routes")
    val routes: List<RouteDto>,

    @SerializedName("status")
    val status: String
)

/**
 * Single route option between origin and destination
 *
 * @param legs List of route segments (usually one leg for single destination)
 * @param overviewPolyline Encoded polyline covering entire route
 */
data class RouteDto(
    @SerializedName("legs")
    val legs: List<LegDto>,

    @SerializedName("overview_polyline")
    val overviewPolyline: PolylineDto
)

/**
 * A leg represents travel between two waypoints
 * For origin -> destination routes, there's typically just one leg
 *
 * @param distance Total distance of this leg
 * @param duration Total travel time for this leg
 * @param startLocation Geographic coordinates where leg begins
 * @param endLocation Geographic coordinates where leg ends
 */
data class LegDto(
    @SerializedName("distance")
    val distance: DistanceDto,

    @SerializedName("duration")
    val duration: DurationDto,

    @SerializedName("start_location")
    val startLocation: LocationDto,

    @SerializedName("end_location")
    val endLocation: LocationDto
)

/**
 * Distance information
 *
 * @param text Human-readable distance (e.g., "5.2 km")
 * @param value Distance in meters (for calculations)
 */
data class DistanceDto(
    @SerializedName("text")
    val text: String,

    @SerializedName("value")
    val value: Int  // meters
)

/**
 * Duration/time information
 *
 * @param text Human-readable duration (e.g., "15 mins")
 * @param value Duration in seconds (for calculations)
 */
data class DurationDto(
    @SerializedName("text")
    val text: String,

    @SerializedName("value")
    val value: Int  // seconds
)

/**
 * Encoded polyline string
 * Uses Google's polyline encoding algorithm to compress coordinates
 *
 * @param points Encoded polyline string (decode with PolyUtil.decode())
 */
data class PolylineDto(
    @SerializedName("points")
    val points: String
)

/**
 * Geographic coordinates
 *
 * @param lat Latitude in degrees
 * @param lng Longitude in degrees (note: Google uses "lng" not "lon")
 */
data class LocationDto(
    @SerializedName("lat")
    val lat: Double,

    @SerializedName("lng")
    val lng: Double
)

