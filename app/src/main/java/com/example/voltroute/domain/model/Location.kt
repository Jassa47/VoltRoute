package com.example.voltroute.domain.model
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
) {
    companion object {
        // Default location (San Francisco)
        val DEFAULT = Location(
            latitude = 37.7749,
            longitude = -122.4194,
            name = "San Francisco, CA"
        )
    }
}