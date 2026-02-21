package com.example.voltroute

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.voltroute.data.remote.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * VoltRouteApplication - Custom Application class
 *
 * Responsibilities:
 * - Enable Hilt dependency injection (@HiltAndroidApp)
 * - Configure WorkManager with HiltWorkerFactory
 * - Schedule periodic background sync
 *
 * WorkManager Configuration:
 * - Uses HiltWorkerFactory for dependency injection in Workers
 * - Enables constructor injection in SyncWorker
 *
 * Periodic Sync:
 * - Runs every 1 hour
 * - Only when device is online
 * - Only when battery is not low
 * - Retries with exponential backoff on failure
 */
@HiltAndroidApp
class VoltRouteApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSync()
    }

    /**
     * Provide WorkManager configuration
     *
     * Required by Configuration.Provider interface.
     * Must be implemented to use HiltWorkerFactory.
     *
     * HiltWorkerFactory enables:
     * - @Inject constructor in Workers
     * - Automatic dependency injection
     * - Access to repositories, managers, etc.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Schedule periodic background sync
     *
     * Creates PeriodicWorkRequest that runs every 1 hour.
     *
     * Constraints:
     * - CONNECTED: Only run when device has network
     * - BATTERY_NOT_LOW: Respect battery optimization
     *
     * Backoff Policy:
     * - EXPONENTIAL: Retry delays increase (15s, 30s, 1m, 2m, etc.)
     * - Prevents excessive retries
     * - Gives network/auth time to recover
     *
     * KEEP Policy:
     * - If work already scheduled, keep existing
     * - Prevents duplicate periodic tasks
     * - Survives app restarts
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                SyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
}
