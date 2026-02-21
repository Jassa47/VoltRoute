package com.example.voltroute.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WorkManagerModule - Hilt module for WorkManager dependency injection
 *
 * Provides WorkManager instance for scheduling background work.
 *
 * WorkManager is used for:
 * - Periodic sync (every 1 hour)
 * - Background uploads when app is closed
 * - Reliable execution with constraints
 *
 * @Singleton ensures single WorkManager instance across app
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provide WorkManager instance
     *
     * WorkManager.getInstance() returns the system-configured
     * WorkManager with proper initialization.
     *
     * Configuration is provided by VoltRouteApplication which
     * implements Configuration.Provider interface.
     *
     * @param context Application context
     * @return WorkManager singleton instance
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}

