package com.example.voltroute.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.voltroute.data.local.dao.ChargerDao
import com.example.voltroute.data.local.dao.ChargingPlanDao
import com.example.voltroute.data.local.dao.RouteDao
import com.example.voltroute.data.local.dao.TripHistoryDao
import com.example.voltroute.data.local.entity.ChargerEntity
import com.example.voltroute.data.local.entity.ChargingPlanEntity
import com.example.voltroute.data.local.entity.RouteEntity
import com.example.voltroute.data.local.entity.TripHistoryEntity

/**
 * Room database for VoltRoute offline caching and trip history
 *
 * Provides local storage for:
 * - Routes, chargers, and charging plans (offline mode)
 * - Trip history (historical record of completed trips)
 *
 * Database schema:
 * - cached_routes: Single route cache (id=1 always)
 * - cached_chargers: Multiple chargers (along route)
 * - cached_charging_plan: Single plan cache (id=1 always)
 * - trip_history: Historical trips (multiple rows)
 *
 * Version 1: Initial schema (cache only)
 * Version 2: Added trip_history table
 * Version 3: Added sync fields to trip_history (syncId, lastModified, isSynced)
 * exportSchema = false: Don't export schema to file (not needed for this app)
 *
 * DAOs:
 * - RouteDao: Route CRUD operations
 * - ChargerDao: Charger CRUD operations
 * - ChargingPlanDao: Plan CRUD operations
 * - TripHistoryDao: Trip history CRUD operations
 */
@Database(
    entities = [
        RouteEntity::class,
        ChargerEntity::class,
        ChargingPlanEntity::class,
        TripHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VoltRouteDatabase : RoomDatabase() {

    /**
     * Get DAO for route operations
     * Room generates the implementation automatically
     */
    abstract fun routeDao(): RouteDao

    /**
     * Get DAO for charger operations
     * Room generates the implementation automatically
     */

    /**
     * Get DAO for trip history operations
     * Room generates the implementation automatically
     */
    abstract fun tripHistoryDao(): TripHistoryDao
    abstract fun chargerDao(): ChargerDao

    /**
     * Get DAO for charging plan operations
     * Room generates the implementation automatically
     */
    abstract fun chargingPlanDao(): ChargingPlanDao
}

