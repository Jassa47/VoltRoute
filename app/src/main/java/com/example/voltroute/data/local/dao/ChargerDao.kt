package com.example.voltroute.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.voltroute.data.local.entity.ChargerEntity

/**
 * Data Access Object for Charger caching operations
 *
 * Provides database operations for storing and retrieving cached charging stations.
 * Unlike routes, we cache multiple chargers (all stations found along a route).
 *
 * OnConflictStrategy.REPLACE handles updates to existing chargers
 * (same id = update the entry).
 */
@Dao
interface ChargerDao {

    /**
     * Save a list of chargers to cache
     *
     * OnConflictStrategy.REPLACE means if a charger with the same id exists,
     * it will be updated with new data.
     *
     * Note: This adds to existing chargers. To replace all chargers,
     * call clearChargers() first (which CacheRepository does).
     *
     * @param chargers List of charger entities to cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChargers(chargers: List<ChargerEntity>)

    /**
     * Get all cached chargers
     *
     * Returns all chargers stored in cache (typically those found along last route).
     * Returns empty list if no chargers are cached.
     *
     * @return List of all cached chargers
     */
    @Query("SELECT * FROM cached_chargers")
    suspend fun getCachedChargers(): List<ChargerEntity>

    /**
     * Delete all cached chargers
     *
     * Clears entire charger cache.
     * Called before saving new charger list to prevent mixing old and new data.
     */
    @Query("DELETE FROM cached_chargers")
    suspend fun clearChargers()

    /**
     * Count cached chargers
     *
     * Useful for checking if any chargers are cached without loading all data.
     * Returns 0 if cache is empty.
     *
     * @return Number of chargers in cache
     */
    @Query("SELECT COUNT(*) FROM cached_chargers")
    suspend fun getChargerCount(): Int
}

