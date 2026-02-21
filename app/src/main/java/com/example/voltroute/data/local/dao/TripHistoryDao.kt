package com.example.voltroute.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.voltroute.data.local.entity.TripHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * TripHistoryDao - Data Access Object for trip history operations
 *
 * IMPORTANT CONCEPT - Flow vs suspend:
 *
 * Flow functions (NO suspend keyword):
 * - Return Flow<T> that emits data whenever database changes
 * - Room automatically observes database and emits new values
 * - Used for queries that need to update UI in real-time
 * - Example: getAllTrips() - UI auto-updates when trips are added/deleted
 *
 * Suspend functions (WITH suspend keyword):
 * - Execute once and return result
 * - Used for one-time operations (insert, update, delete)
 * - Example: insertTrip() - runs once, returns ID, then completes
 */
@Dao
interface TripHistoryDao {

    /**
     * Insert a new trip into history
     * @return The ID of the newly inserted trip
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripHistoryEntity): Long

    /**
     * Get all trips, ordered by date (newest first)
     *
     * Returns Flow - NO suspend keyword!
     * Flow automatically emits new list whenever:
     * - A trip is inserted
     * - A trip is deleted
     * - The table is cleared
     *
     * UI observing this Flow will automatically refresh.
     */
    @Query("SELECT * FROM trip_history ORDER BY tripDate DESC")
    fun getAllTrips(): Flow<List<TripHistoryEntity>>

    /**
     * Delete a specific trip from history
     */
    @Delete
    suspend fun deleteTrip(trip: TripHistoryEntity)

    /**
     * Clear all trip history
     */
    @Query("DELETE FROM trip_history")
    suspend fun clearAllTrips()

    /**
     * Get total count of trips
     * Useful for showing stats like "45 trips recorded"
     */
    @Query("SELECT COUNT(*) FROM trip_history")
    suspend fun getTripCount(): Int

    /**
     * Get trips from the last 30 days
     *
     * @param thirtyDaysAgo Timestamp 30 days ago (default calculated automatically)
     * @return Flow of recent trips, ordered by date (newest first)
     *
     * Note: 2_592_000_000L milliseconds = 30 days
     */
    @Query("""
        SELECT * FROM trip_history
        WHERE tripDate > :thirtyDaysAgo
        ORDER BY tripDate DESC
    """)
    fun getRecentTrips(
        thirtyDaysAgo: Long = System.currentTimeMillis() - 2_592_000_000L
    ): Flow<List<TripHistoryEntity>>

    // NEW SYNC METHODS - For cloud synchronization

    /**
     * Get all trips that haven't been synced to cloud yet
     *
     * Returns trips where isSynced = 0 (false).
     * These trips need to be uploaded to Firestore.
     *
     * @return List of unsynced trips
     */
    @Query("SELECT * FROM trip_history WHERE isSynced = 0")
    suspend fun getUnsyncedTrips(): List<TripHistoryEntity>

    /**
     * Mark trip as synced after successful upload
     *
     * Updates isSynced flag and stores Firestore document ID.
     * Called after successful upload to Firestore.
     *
     * @param localId The local database ID of the trip
     * @param syncId The Firestore document ID
     */
    @Query("UPDATE trip_history SET isSynced = 1, syncId = :syncId WHERE id = :localId")
    suspend fun markAsSynced(localId: Long, syncId: String)

    /**
     * Update trip's last modified timestamp
     *
     * Used for conflict resolution when syncing changes.
     * The trip with the newest timestamp wins.
     *
     * @param localId The local database ID
     * @param timestamp The new modification timestamp
     */
    @Query("UPDATE trip_history SET lastModified = :timestamp WHERE id = :localId")
    suspend fun updateLastModified(localId: Long, timestamp: Long)

    /**
     * Find trip by Firestore sync ID
     *
     * Used when downloading trips from cloud to avoid duplicates.
     * Returns null if no local trip has this syncId.
     *
     * @param syncId The Firestore document ID
     * @return Trip entity or null if not found
     */
    @Query("SELECT * FROM trip_history WHERE syncId = :syncId LIMIT 1")
    suspend fun getTripBySyncId(syncId: String): TripHistoryEntity?

    /**
     * Insert or update trip from cloud (for syncing down)
     *
     * Uses REPLACE strategy: if trip exists (same id), it's replaced.
     * Used when downloading trips from Firestore.
     *
     * @param trip Trip entity to insert or update
     * @return The ID of the inserted/updated trip
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripHistoryEntity): Long
}

