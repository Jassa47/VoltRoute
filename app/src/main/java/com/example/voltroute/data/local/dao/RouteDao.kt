package com.example.voltroute.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.voltroute.data.local.entity.RouteEntity

/**
 * Data Access Object for Route caching operations
 *
 * Provides database operations for storing and retrieving cached routes.
 * Uses OnConflictStrategy.REPLACE to automatically overwrite old routes
 * since we only cache one route at a time (id = 1).
 */
@Dao
interface RouteDao {

    /**
     * Save a route to cache
     *
     * OnConflictStrategy.REPLACE means if a route with id=1 already exists,
     * it will be replaced with the new one (no error thrown).
     * This implements our single-route cache strategy.
     *
     * @param route The route entity to cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoute(route: RouteEntity)

    /**
     * Get the cached route
     *
     * Returns null if no route is cached.
     * Since we always use id=1, this query is straightforward.
     *
     * @return Cached route or null if none exists
     */
    @Query("SELECT * FROM cached_routes WHERE id = 1")
    suspend fun getCachedRoute(): RouteEntity?

    /**
     * Delete the cached route
     *
     * Clears all routes from cache (in practice, just the one with id=1).
     * Useful for cache invalidation or logout scenarios.
     */
    @Query("DELETE FROM cached_routes")
    suspend fun clearRoute()

    /**
     * Get timestamp of last cached route
     *
     * Used for displaying cache age to user ("cached 5 minutes ago").
     * Returns null if no route is cached.
     *
     * @return Timestamp in milliseconds, or null if no cache
     */
    @Query("SELECT cachedAt FROM cached_routes WHERE id = 1")
    suspend fun getLastCachedTime(): Long?
}

