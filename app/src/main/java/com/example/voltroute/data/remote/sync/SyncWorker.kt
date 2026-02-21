package com.example.voltroute.data.remote.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * SyncWorker - Background periodic sync worker using WorkManager
 *
 * Runs every 1 hour when:
 * - Device is online (has network connection)
 * - Battery is not low
 * - Android system allows background work
 *
 * WorkManager Benefits:
 * - Guaranteed execution (survives app restart)
 * - Respects battery optimization
 * - Retries automatically on failure
 * - Runs even when app is closed
 *
 * This ensures trips are synced even if user doesn't open app:
 * - Uploads unsynced trips from local database
 * - Works alongside real-time listeners (when app is open)
 * - Provides reliability for offline-first architecture
 *
 * @HiltWorker enables Hilt dependency injection in Worker
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "periodic_sync"
    }

    /**
     * Background work execution
     *
     * Called by WorkManager when:
     * - Scheduled time arrives (every 1 hour)
     * - Constraints are met (online + battery not low)
     *
     * Returns:
     * - Result.success(): Work completed, schedule next run
     * - Result.retry(): Work failed, retry with exponential backoff
     * - Result.failure(): Work failed permanently, don't retry
     *
     * We always return success() because:
     * - Sync failures are logged but not critical
     * - Next scheduled run will try again
     * - Prevents excessive retries
     */
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic sync...")

        return try {
            syncManager.syncNow()
                .onSuccess {
                    Log.d(TAG, "Periodic sync completed successfully")
                }
                .onFailure { e ->
                    Log.e(TAG, "Periodic sync failed", e)
                }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker error", e)
            Result.retry()  // Retry on failure with exponential backoff
        }
    }
}

