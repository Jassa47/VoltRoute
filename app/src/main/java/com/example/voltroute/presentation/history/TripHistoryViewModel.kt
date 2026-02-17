package com.example.voltroute.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voltroute.data.local.entity.TripHistoryEntity
import com.example.voltroute.data.local.repository.TripHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TripHistoryViewModel - ViewModel for Trip History screen
 *
 * Manages trip history data and exposes it as StateFlow for UI.
 *
 * KEY CONCEPT - Flow to StateFlow conversion with stateIn():
 *
 * Repository returns Flow<List<TripHistoryEntity>>:
 * - Flow is "cold" - only active when collected
 * - Multiple collectors = multiple database queries
 * - No initial value (emits when DB ready)
 *
 * We convert to StateFlow with stateIn():
 * - StateFlow is "hot" - always has a value
 * - Multiple collectors share same Flow (efficient)
 * - Has initialValue (shows immediately)
 * - Survives configuration changes (screen rotation)
 *
 * SharingStarted.WhileSubscribed(5_000):
 * - Starts Flow collection when first subscriber appears
 * - Keeps running for 5 seconds after last subscriber leaves
 * - 5 second buffer handles screen rotation gracefully
 * - Prevents unnecessary restarts during quick config changes
 *
 * Without the 5 second buffer:
 * - Rotate screen → old screen unsubscribes
 * - New screen subscribes → Flow restarts from scratch
 * - Result: Unnecessary work, UI flicker
 *
 * With 5 second buffer:
 * - Rotate screen → old screen unsubscribes
 * - New screen subscribes within 5 seconds
 * - Flow is still running → instant data!
 * - Result: Smooth rotation, no reload
 */
@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    private val tripHistoryRepository: TripHistoryRepository
) : ViewModel() {

    /**
     * All trips from history, newest first
     *
     * StateFlow auto-updates when:
     * - New trip is saved (from MapViewModel)
     * - User deletes a trip
     * - User clears all history
     *
     * UI collects this and recomposes automatically.
     *
     * Flow → StateFlow conversion breakdown:
     * 1. tripHistoryRepository.getAllTrips() returns Flow<List<TripHistoryEntity>>
     * 2. .stateIn() converts it to StateFlow<List<TripHistoryEntity>>
     * 3. scope = viewModelScope: Lives as long as ViewModel
     * 4. started = WhileSubscribed(5000): Smart lifecycle management
     * 5. initialValue = emptyList(): Shows empty list before DB loads
     */
    val trips: StateFlow<List<TripHistoryEntity>> =
        tripHistoryRepository.getAllTrips()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * Delete a specific trip from history
     *
     * Launched in viewModelScope so it:
     * - Runs on background thread (via repository's withContext)
     * - Cancels if ViewModel is cleared
     * - Doesn't block UI thread
     *
     * After deletion, trips StateFlow auto-emits updated list.
     * No manual refresh needed!
     */
    fun deleteTrip(trip: TripHistoryEntity) {
        viewModelScope.launch {
            tripHistoryRepository.deleteTrip(trip)
            // trips StateFlow automatically updates via Room's Flow!
        }
    }

    /**
     * Clear all trip history
     *
     * Useful for "Clear All History" button in UI.
     * After clearing, trips StateFlow emits empty list.
     */
    fun clearAllTrips() {
        viewModelScope.launch {
            tripHistoryRepository.clearAllTrips()
        }
    }
}

