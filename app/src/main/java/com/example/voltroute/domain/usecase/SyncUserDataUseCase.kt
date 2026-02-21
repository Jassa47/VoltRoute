package com.example.voltroute.domain.usecase

import com.example.voltroute.data.remote.sync.SyncManager
import javax.inject.Inject

/**
 * SyncUserDataUseCase - Use case for triggering manual cloud synchronization
 *
 * Clean Architecture layer between ViewModel and data layer.
 *
 * Responsibilities:
 * - Provide simple interface for triggering sync
 * - Delegate actual sync logic to SyncManager
 *
 * Usage in ViewModel:
 * ```kotlin
 * viewModelScope.launch {
 *     syncUserDataUseCase()
 *         .onSuccess { /* Show success message */ }
 *         .onFailure { /* Show error */ }
 * }
 * ```
 *
 * Why Use Case pattern:
 * - Separates business logic from presentation layer
 * - Makes testing easier (can mock use case)
 * - Provides clear API for domain operations
 * - Single responsibility (just sync)
 */
class SyncUserDataUseCase @Inject constructor(
    private val syncManager: SyncManager
) {
    /**
     * Trigger manual sync of trips and settings
     *
     * Uploads:
     * - All unsynced trips to Firestore
     * - Current user settings (handled separately)
     *
     * @return Result.success if sync completes, Result.failure if error occurs
     */
    suspend operator fun invoke(): Result<Unit> {
        return syncManager.syncNow()
    }
}

