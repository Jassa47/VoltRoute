package com.example.voltroute.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) for Open Charge Map API response
 *
 * These classes map directly to the JSON structure returned by the API.
 * We use @SerializedName to handle the API's naming conventions.
 *
 * API Documentation: https://openchargemap.org/site/develop/api
 *
 * Example API response:
 * [
 *   {
 *     "ID": 12345,
 *     "AddressInfo": {
 *       "Title": "Tesla Supercharger",
 *       "Latitude": 49.2827,
 *       "Longitude": -123.1207,
 *       "Distance": 2.3
 *     },
 *     "Connections": [
 *       {
 *         "ConnectionType": {"Title": "CCS"},
 *         "PowerKW": 150.0,
 *         "Quantity": 8
 *       }
 *     ]
 *   }
 * ]
 */

/**
 * Root response item for a single charging station
 *
 * @param id Unique identifier from Open Charge Map
 * @param addressInfo Location and address details
 * @param connections List of available charging connections (can be null/empty)
 */
data class ChargerResponseItem(
    @SerializedName("ID")
    val id: Int,

    @SerializedName("AddressInfo")
    val addressInfo: AddressInfoDto,

    @SerializedName("Connections")
    val connections: List<ConnectionDto>?
)

/**
 * Address and location information for a charging station
 *
 * @param title Station name/title
 * @param latitude Geographic latitude
 * @param longitude Geographic longitude
 * @param distance Distance from search point in specified units (optional)
 */
data class AddressInfoDto(
    @SerializedName("Title")
    val title: String?,

    @SerializedName("Latitude")
    val latitude: Double,

    @SerializedName("Longitude")
    val longitude: Double,

    @SerializedName("Distance")
    val distance: Double?
)

/**
 * Individual charging connection/port information
 *
 * @param connectionType Type of connector (CCS, CHAdeMO, Type 2, etc.)
 * @param powerKw Charging power in kilowatts
 * @param quantity Number of ports of this type
 */
data class ConnectionDto(
    @SerializedName("ConnectionType")
    val connectionType: ConnectionTypeDto?,

    @SerializedName("PowerKW")
    val powerKw: Double?,

    @SerializedName("Quantity")
    val quantity: Int?
)

/**
 * Connection type details
 *
 * @param title Name of the connector type (e.g., "CCS", "CHAdeMO", "Type 2")
 */
data class ConnectionTypeDto(
    @SerializedName("Title")
    val title: String?
)

