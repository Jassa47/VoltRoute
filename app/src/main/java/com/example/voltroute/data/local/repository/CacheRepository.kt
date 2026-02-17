package com.example.voltroute.data.local.repository

import com.example.voltroute.data.local.dao.ChargerDao
import com.example.voltroute.data.local.dao.ChargingPlanDao
import com.example.voltroute.data.local.dao.RouteDao
import com.example.voltroute.data.local.entity.ChargerEntity
import com.example.voltroute.data.local.entity.ChargingPlanEntity
import com.example.voltroute.data.local.entity.RouteEntity
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper class for cached data with metadata
 *
 * @param T The type of data being cached
 * @param data The actual cached data
 * @param cachedAt Timestamp when data was cached (milliseconds)
 * @param destinationAddress The destination address (for routes)
 */
data class CachedData<T>(
    val data: T,
    val cachedAt: Long,
    val destinationAddress: String = ""
)

/**
 * Repository for managing local data cache
 *
 * Handles conversion between domain models and Room entities.
 * All database operations run on IO dispatcher for optimal performance.
 *
 * Key responsibilities:
 * - Save/load routes with location flattening
 * - Save/load chargers with JSON serialization of connector types
 * - Save/load charging plans with full JSON serialization
 * - Provide cache age formatting for UI
 *
 * Design: Singleton to ensure single source of truth for cache
 */
@Singleton
class CacheRepository @Inject constructor(
    private val routeDao: RouteDao,
    private val chargerDao: ChargerDao,
    private val chargingPlanDao: ChargingPlanDao,
    private val gson: Gson
) {

    // ==================== ROUTE OPERATIONS ====================

    /**
     * Save route to local cache
     *
     * Converts domain Route model to Room entity, flattening nested Location objects.
     * Runs on IO dispatcher for database operations.
     *
     * @param route The route to cache
     * @param destinationAddress User-entered destination text (for search field restoration)
     */
    suspend fun saveRoute(route: Route, destinationAddress: String) =
        withContext(Dispatchers.IO) {
            val entity = RouteEntity(
                id = 1,  // Always use id=1 (single route cache)
                distanceMeters = route.distanceMeters,
                durationSeconds = route.durationSeconds,
                polylinePoints = route.polylinePoints,
                startLat = route.startLocation.latitude,
                startLng = route.startLocation.longitude,
                endLat = route.endLocation.latitude,
                endLng = route.endLocation.longitude,
                destinationAddress = destinationAddress,
                cachedAt = System.currentTimeMillis()
            )
            routeDao.saveRoute(entity)
        }

    /**
     * Load cached route from local database
     *
     * Converts Room entity back to domain Route model, reconstructing Location objects.
     * Returns wrapped in CachedData to provide cache metadata.
     *
     * @return CachedData<Route> with route and metadata, or null if no cache
     */
    suspend fun loadCachedRoute(): CachedData<Route>? =
        withContext(Dispatchers.IO) {
            val entity = routeDao.getCachedRoute() ?: return@withContext null

            // Reconstruct Location objects from flattened fields
            val startLocation = Location(
                latitude = entity.startLat,
                longitude = entity.startLng
            )
            val endLocation = Location(
                latitude = entity.endLat,
                longitude = entity.endLng
            )

            // Reconstruct Route domain model
            val route = Route(
                distanceMeters = entity.distanceMeters,
                durationSeconds = entity.durationSeconds,
                polylinePoints = entity.polylinePoints,
                startLocation = startLocation,
                endLocation = endLocation
            )

            CachedData(
                data = route,
                cachedAt = entity.cachedAt,
                destinationAddress = entity.destinationAddress
            )
        }

    // ==================== CHARGER OPERATIONS ====================

    /**
     * Save chargers to local cache
     *
     * Clears old charger cache first to prevent mixing data from different searches.
     * Converts domain Charger models to Room entities, serializing connector types to JSON.
     *
     * Why clear first: Each route search finds different chargers, so we want to
     * replace the entire set rather than append.
     *
     * @param chargers List of chargers to cache
     */
    suspend fun saveChargers(chargers: List<Charger>) =
        withContext(Dispatchers.IO) {
            // Clear old chargers first
            chargerDao.clearChargers()

            // Convert chargers to entities
            val entities = chargers.map { charger ->
                ChargerEntity(
                    id = charger.id,
                    name = charger.name,
                    latitude = charger.location.latitude,
                    longitude = charger.location.longitude,
                    powerKw = charger.powerKw,
                    // Serialize connector types list to JSON string
                    // Example: ["CCS", "CHAdeMO"] → "[\"CCS\",\"CHAdeMO\"]"
                    connectorTypes = gson.toJson(charger.connectorTypes),
                    numberOfPoints = charger.numberOfPoints,
                    distanceKm = charger.distanceKm,
                    cachedAt = System.currentTimeMillis()
                )
            }

            chargerDao.saveChargers(entities)
        }

    /**
     * Load cached chargers from local database
     *
     * Converts Room entities back to domain Charger models,
     * deserializing JSON connector types back to List<String>.
     *
     * @return List of cached chargers (empty if no cache)
     */
    suspend fun loadCachedChargers(): List<Charger> =
        withContext(Dispatchers.IO) {
            val entities = chargerDao.getCachedChargers()

            entities.map { entity ->
                // Deserialize connector types from JSON string
                // Example: "[\"CCS\",\"CHAdeMO\"]" → ["CCS", "CHAdeMO"]
                val connectorTypes = gson.fromJson(
                    entity.connectorTypes,
                    Array<String>::class.java
                ).toList()

                // Reconstruct Location object
                val location = Location(
                    latitude = entity.latitude,
                    longitude = entity.longitude
                )

                // Reconstruct Charger domain model
                Charger(
                    id = entity.id,
                    name = entity.name,
                    location = location,
                    powerKw = entity.powerKw,
                    connectorTypes = connectorTypes,
                    numberOfPoints = entity.numberOfPoints,
                    distanceKm = entity.distanceKm
                )
            }
        }

    // ==================== CHARGING PLAN OPERATIONS ====================

    /**
     * Save charging plan to local cache
     *
     * Serializes entire ChargingPlan to JSON (including nested ChargingStop and Charger objects).
     * This approach handles deep nesting without complex Room relationships.
     *
     * @param plan The charging plan to cache
     */
    suspend fun saveChargingPlan(plan: ChargingPlan) =
        withContext(Dispatchers.IO) {
            // Serialize entire plan to JSON
            val planJson = gson.toJson(plan)

            val entity = ChargingPlanEntity(
                id = 1,  // Always use id=1 (single plan cache)
                planJson = planJson,
                cachedAt = System.currentTimeMillis()
            )

            chargingPlanDao.savePlan(entity)
        }

    /**
     * Load cached charging plan from local database
     *
     * Deserializes JSON back to complete ChargingPlan with all nested objects.
     * Gson automatically reconstructs the entire object graph.
     *
     * @return Cached charging plan or null if no cache
     */
    suspend fun loadCachedChargingPlan(): ChargingPlan? =
        withContext(Dispatchers.IO) {
            val entity = chargingPlanDao.getCachedPlan() ?: return@withContext null

            // Deserialize JSON back to ChargingPlan
            gson.fromJson(entity.planJson, ChargingPlan::class.java)
        }

    // ==================== UTILITY FUNCTIONS ====================

    /**
     * Get timestamp of last cached route
     *
     * Used for displaying cache age to user.
     *
     * @return Timestamp in milliseconds, or null if no cache
     */
    suspend fun getLastCachedTime(): Long? = routeDao.getLastCachedTime()

    /**
     * Format cache age for display
     *
     * Converts millisecond timestamp to human-readable age string.
     *
     * Examples:
     * - "just now" (< 1 minute)
     * - "5 minutes ago"
     * - "2 hours ago"
     * - "3 days ago"
     *
     * @param cachedAt Timestamp when data was cached (milliseconds)
     * @return Human-readable age string
     */
    fun formatCacheAge(cachedAt: Long): String {
        val ageMs = System.currentTimeMillis() - cachedAt
        val minutes = ageMs / 60_000
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            else -> "$days days ago"
        }
    }
}

