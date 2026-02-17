package com.example.voltroute.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.voltroute.data.local.entity.ChargingPlanEntity

/**
 * Data Access Object for ChargingPlan caching operations
 *
 * Provides database operations for storing and retrieving the cached charging plan.
 * Uses single-plan cache strategy (id = 1).
 *
 * The plan is stored as serialized JSON containing all stops and their details.
 */
@Dao
interface ChargingPlanDao {

    /**
     * Save a charging plan to cache
     *
     * OnConflictStrategy.REPLACE means if a plan with id=1 already exists,
     * it will be replaced with the new one.
     * This implements our single-plan cache strategy.
     *
     * @param plan The charging plan entity (containing JSON) to cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlan(plan: ChargingPlanEntity)

    /**
     * Get the cached charging plan
     *
     * Returns null if no plan is cached.
     * The plan JSON will need to be deserialized by the repository.
     *
     * @return Cached plan entity or null if none exists
     */
    @Query("SELECT * FROM cached_charging_plan WHERE id = 1")
    suspend fun getCachedPlan(): ChargingPlanEntity?

    /**
     * Delete the cached charging plan
     *
     * Clears the charging plan from cache.
     * Useful for cache invalidation scenarios.
     */
    @Query("DELETE FROM cached_charging_plan")
    suspend fun clearPlan()
}

