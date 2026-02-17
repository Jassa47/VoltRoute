package com.example.voltroute.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.voltroute.data.local.dao.ChargerDao
import com.example.voltroute.data.local.dao.ChargingPlanDao
import com.example.voltroute.data.local.dao.RouteDao
import com.example.voltroute.data.local.entity.ChargerEntity
import com.example.voltroute.data.local.entity.ChargingPlanEntity
import com.example.voltroute.data.local.entity.RouteEntity

/**
 * Room database for VoltRoute offline caching
 *
 * Provides local storage for routes, chargers, and charging plans
 * to enable offline mode functionality.
 *
 * Database schema:
 * - cached_routes: Single route cache (id=1 always)
 * - cached_chargers: Multiple chargers (along route)
 * - cached_charging_plan: Single plan cache (id=1 always)
 *
 * Version 1: Initial schema
 * exportSchema = false: Don't export schema to file (not needed for this app)
 *
 * DAOs:
 * - RouteDao: Route CRUD operations
 * - ChargerDao: Charger CRUD operations
 * - ChargingPlanDao: Plan CRUD operations
 */
@Database(
    entities = [
        RouteEntity::class,
        ChargerEntity::class,
        ChargingPlanEntity::class
    ],
    version = 1,
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
    abstract fun chargerDao(): ChargerDao

    /**
     * Get DAO for charging plan operations
     * Room generates the implementation automatically
     */
    abstract fun chargingPlanDao(): ChargingPlanDao
}

