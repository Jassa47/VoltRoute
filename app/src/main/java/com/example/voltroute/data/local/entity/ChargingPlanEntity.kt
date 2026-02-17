package com.example.voltroute.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching charging plan data locally
 *
 * Stores the complete calculated charging plan for offline access.
 * Uses id = 1 always (single plan cache strategy).
 *
 * Design decisions:
 * - Entire plan stored as JSON string rather than separate tables
 *   Reason: ChargingPlan contains nested objects:
 *     ChargingPlan → List<ChargingStop> → each stop contains Charger → Location
 *   This deep nesting is complex to model in Room with foreign keys
 *   JSON serialization is simpler and preserves the complete object graph
 *
 * Serialization example:
 *   val json = gson.toJson(chargingPlan)
 *   val plan = gson.fromJson(json, ChargingPlan::class.java)
 *
 * Trade-offs:
 * - Pro: Simple to implement, preserves complete structure
 * - Pro: No need for complex Room relationships/foreign keys
 * - Con: Cannot query individual stops (acceptable for cache use case)
 *
 * @param id Always 1 (single plan cache)
 * @param planJson Complete ChargingPlan serialized as JSON
 * @param cachedAt Timestamp when plan was cached
 */
@Entity(tableName = "cached_charging_plan")
data class ChargingPlanEntity(
    @PrimaryKey
    val id: Int = 1,

    val planJson: String,  // Serialized ChargingPlan with all nested objects
    val cachedAt: Long = System.currentTimeMillis()
)

