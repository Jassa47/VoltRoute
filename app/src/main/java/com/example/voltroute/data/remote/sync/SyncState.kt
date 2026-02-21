package com.example.voltroute.data.remote.sync

/**
 * SyncState - Represents the current state of cloud synchronization
 *
 * Used to show sync status in UI:
 * - Idle: No sync in progress
 * - Syncing: Upload/download in progress
 * - Success: Sync completed successfully
 * - Error: Sync failed with error message
 *
 * Exposed as StateFlow from SyncManager for reactive UI updates.
 */
sealed class SyncState {
    /**
     * No sync operation in progress
     */
    object Idle : SyncState()

    /**
     * Sync operation currently in progress
     */
    object Syncing : SyncState()

    /**
     * Sync completed successfully
     * @param message Success message to display to user
     */
    data class Success(val message: String) : SyncState()

    /**
     * Sync failed with error
     * @param message Error message to display to user
     */
    data class Error(val message: String) : SyncState()
}

