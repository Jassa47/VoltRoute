package com.example.voltroute.presentation.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voltroute.data.location.LocationClient
import com.example.voltroute.domain.model.BatteryState
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.ChargingStop
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import com.example.voltroute.domain.model.Vehicle
import com.example.voltroute.domain.usecase.CalculateBatteryUseCase
import com.example.voltroute.domain.usecase.CalculateRouteUseCase
import com.example.voltroute.domain.usecase.FindChargersUseCase
import com.example.voltroute.domain.usecase.LoadFromCacheUseCase
import com.example.voltroute.domain.usecase.PlanChargingStopsUseCase
import com.example.voltroute.domain.usecase.SaveToCacheUseCase
import com.example.voltroute.utils.NetworkMonitor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationClient: LocationClient,
    private val calculateRouteUseCase: CalculateRouteUseCase,
    private val calculateBatteryUseCase: CalculateBatteryUseCase,
    private val findChargersUseCase: FindChargersUseCase,
    private val planChargingStopsUseCase: PlanChargingStopsUseCase,
    private val saveToCacheUseCase: SaveToCacheUseCase,
    private val loadFromCacheUseCase: LoadFromCacheUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _vehicle = MutableStateFlow(Vehicle())
    val vehicle: StateFlow<Vehicle> = _vehicle.asStateFlow()

    init {
        checkLocationPermission()
        calculateInitialBatteryState()
        observeNetworkStatus()
        checkForCachedData()
    }

    private fun checkLocationPermission() {
        val hasPermission = locationClient.hasLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }

        if (hasPermission) {
            getCurrentLocation()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            getCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        viewModelScope.launch  {
            _uiState.update { it.copy(isLoadingLocation = true) }

            val location = locationClient.getCurrentLocation()

            _uiState.update {
                it.copy(
                    currentLocation = location ?: Location.DEFAULT,
                    isLoadingLocation = false,
                    locationError = if (location == null) "Could not get location" else null
                )
            }
        }
    }

    fun onDestinationChanged(destination: String) {
        _uiState.update { it.copy(destinationAddress = destination) }
    }

    fun clearError() {
        _uiState.update { it.copy(locationError = null) }
    }

    /**
     * Calculate initial battery state without a route
     * Called on ViewModel initialization to show current battery status
     */
    private fun calculateInitialBatteryState() {
        val batteryState = calculateBatteryUseCase(
            vehicle = _vehicle.value,
            route = null
        )
        _uiState.update { it.copy(batteryState = batteryState) }
    }

    /**
     * Calculate route from current location to destination
     *
     * Validates destination, calls use case, decodes polyline, and updates UI state
     */
    fun calculateRoute() {
        viewModelScope.launch {
            val destination = _uiState.value.destinationAddress

            // Validate destination is not blank
            if (destination.isBlank()) {
                _uiState.update {
                    it.copy(routeError = "Please enter a destination")
                }
                return@launch
            }

            // Show loading state
            _uiState.update { it.copy(isCalculatingRoute = true, routeError = null) }

            // Call use case to calculate route
            val result = calculateRouteUseCase(
                origin = _uiState.value.currentLocation,
                destination = destination
            )

            // Handle result
            result.fold(
                onSuccess = { route ->
                    // Decode polyline to LatLng points for map display
                    val routePoints = try {
                        PolyUtil.decode(route.polylinePoints)
                    } catch (e: Exception) {
                        emptyList<LatLng>()
                    }

                    // Calculate battery state for this route
                    val batteryState = calculateBatteryUseCase(
                        vehicle = _vehicle.value,
                        route = route
                    )

                    _uiState.update {
                        it.copy(
                            route = route,
                            routePoints = routePoints,
                            isCalculatingRoute = false,
                            routeError = null,
                            batteryState = batteryState
                        )
                    }

                    // Auto-find charging stations along the route
                    findChargers()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            route = null,
                            routePoints = emptyList(),
                            isCalculatingRoute = false,
                            routeError = exception.message ?: "Could not calculate route"
                        )
                    }
                }
            )
        }
    }

    /**
     * Clear the current route and reset route-related state
     */
    fun clearRoute() {
        // Recalculate battery state without route
        val batteryState = calculateBatteryUseCase(
            vehicle = _vehicle.value,
            route = null
        )

        _uiState.update {
            it.copy(
                route = null,
                routePoints = emptyList(),
                routeError = null,
                batteryState = batteryState
            )
        }
    }

    /**
     * Clear route error message
     */
    fun clearRouteError() {
        _uiState.update { it.copy(routeError = null) }
    }

    /**
     * Find charging stations near current location or along the route
     *
     * Uses smart search logic:
     * - Without route: Searches near current location
     * - With route: Searches near route midpoint for better coverage
     */
    fun findChargers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChargers = true) }

            val result = findChargersUseCase(
                currentLocation = _uiState.value.currentLocation,
                route = _uiState.value.route
            )

            result
                .onSuccess { chargers ->
                    // Calculate charging plan if route exists
                    val plan = _uiState.value.route?.let { route ->
                        planChargingStopsUseCase(
                            route = route,
                            vehicle = _vehicle.value,
                            availableChargers = chargers
                        )
                    }

                    _uiState.update {
                        it.copy(
                            chargers = chargers,
                            isLoadingChargers = false,
                            showChargers = true,
                            chargerError = null,
                            chargingPlan = plan
                        )
                    }

                    // Auto-save to cache after successful charger search
                    _uiState.value.route?.let { route ->
                        viewModelScope.launch {
                            saveToCacheUseCase(
                                route = route,
                                destinationAddress = _uiState.value.destinationAddress,
                                chargers = chargers,
                                chargingPlan = plan
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingChargers = false,
                            chargerError = error.message
                        )
                    }
                }
        }
    }

    /**
     * Handle charger marker selection
     * Updates UI to show charger details
     */
    fun onChargerSelected(charger: Charger) {
        _uiState.update { it.copy(selectedCharger = charger) }
    }

    /**
     * Dismiss charger details
     * Closes the charger info panel
     */
    fun onChargerDismissed() {
        _uiState.update { it.copy(selectedCharger = null) }
    }

    /**
     * Clear charger error message
     */
    fun clearChargerError() {
        _uiState.update { it.copy(chargerError = null) }
    }

    /**
     * Toggle charger markers visibility on map
     * Automatically finds chargers if not already loaded
     */
    fun toggleChargers() {
        val currentlyShowing = _uiState.value.showChargers

        if (!currentlyShowing && _uiState.value.chargers.isEmpty()) {
            // First time showing - find chargers
            findChargers()
        } else {
            // Just toggle visibility
            _uiState.update { it.copy(showChargers = !currentlyShowing) }
        }
    }

    /**
     * Start the process of swapping a charging stop
     * Sets the stop to be swapped and shows chargers for selection
     */
    fun onSwapStop(stop: ChargingStop) {
        _uiState.update {
            it.copy(
                swappingStop = stop,
                showChargers = true
            )
        }
    }

    /**
     * Replace a charging stop with a different charger
     * Recalculates the plan with the new charger
     */
    fun onChargerSelectedAsSwap(charger: Charger) {
        val swapping = _uiState.value.swappingStop ?: return
        val currentPlan = _uiState.value.chargingPlan ?: return

        // Update the stop with the new charger
        val updatedStops = currentPlan.stops.map { stop ->
            if (stop.stopNumber == swapping.stopNumber) {
                stop.copy(charger = charger)
            } else {
                stop
            }
        }

        // Recalculate total times
        val totalChargingMins = updatedStops.sumOf { it.estimatedChargeTimeMinutes }
        val updatedPlan = currentPlan.copy(
            stops = updatedStops,
            totalChargingTimeMinutes = totalChargingMins,
            totalTripTimeMinutes = currentPlan.routeDurationMinutes + totalChargingMins
        )

        _uiState.update {
            it.copy(
                chargingPlan = updatedPlan,
                swappingStop = null
            )
        }
    }

    // ==================== OFFLINE MODE FUNCTIONS ====================

    /**
     * Observe network connectivity status
     *
     * Collects from NetworkMonitor's isOnline flow and updates UI state.
     * Updates both isOnline (network status) and isOfflineMode (user-facing) flags.
     *
     * Runs continuously for the lifetime of the ViewModel.
     */
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update {
                    it.copy(
                        isOnline = online,
                        isOfflineMode = !online
                    )
                }
            }
        }
    }

    /**
     * Check for cached data on app start
     *
     * Loads cache metadata (not the actual data) to:
     * - Show if cache exists
     * - Display cache age
     * - Enable "Load from Cache" button
     *
     * Called in init block.
     */
    private fun checkForCachedData() {
        viewModelScope.launch {
            val cached = loadFromCacheUseCase()
            _uiState.update {
                it.copy(
                    hasCachedRoute = cached.routeData != null,
                    hasCachedChargers = cached.chargers.isNotEmpty(),
                    cacheAgeText = cached.cacheAgeText
                )
            }
        }
    }

    /**
     * Load cached data and restore app state
     *
     * Called when user taps "Load from Cache" or app is in offline mode.
     * Restores:
     * - Last calculated route with polyline
     * - Destination address (search field)
     * - Battery state for that route
     * - Found chargers
     * - Charging plan (if existed)
     *
     * After loading, exits offline mode (data is now available).
     */
    fun loadCachedData() {
        viewModelScope.launch {
            val cached = loadFromCacheUseCase()

            // Restore route data
            cached.routeData?.let { routeData ->
                // Decode polyline for map display
                val points = PolyUtil.decode(routeData.data.polylinePoints)

                // Recalculate battery state for cached route
                val batteryState = calculateBatteryUseCase(
                    vehicle = _vehicle.value,
                    route = routeData.data
                )

                _uiState.update {
                    it.copy(
                        route = routeData.data,
                        routePoints = points,
                        destinationAddress = routeData.destinationAddress,
                        batteryState = batteryState,
                        isOfflineMode = false  // Exit offline mode after loading
                    )
                }
            }

            // Restore chargers
            if (cached.chargers.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        chargers = cached.chargers,
                        showChargers = true
                    )
                }
            }

            // Restore charging plan
            cached.chargingPlan?.let { plan ->
                _uiState.update {
                    it.copy(chargingPlan = plan)
                }
            }
        }
    }

    /**
     * Retry network connection check
     *
     * Called when user taps "Retry" in offline mode banner.
     * Synchronously checks current network status and updates UI.
     *
     * Uses isCurrentlyOnline() for immediate check (not flow-based).
     */
    fun retryConnection() {
        val isOnline = networkMonitor.isCurrentlyOnline()
        _uiState.update {
            it.copy(
                isOnline = isOnline,
                isOfflineMode = !isOnline
            )
        }
    }

    /**
     * Cancel charger swap operation
     * Clears the swappingStop to exit swap mode
     */
    fun cancelSwap() {
        _uiState.update { it.copy(swappingStop = null) }
    }

    /**
     * Update vehicle from preset selection
     *
     * Preserves current battery percentage when switching vehicles.
     * Recalculates battery state with new vehicle specs.
     *
     * @param preset The selected vehicle preset
     */
    fun updateVehicle(preset: com.example.voltroute.domain.model.VehiclePreset) {
        // Update vehicle while preserving current battery percentage
        _vehicle.update { preset.toVehicle(it.currentBatteryPercent) }

        // Recalculate battery state with new vehicle specs
        val batteryState = calculateBatteryUseCase(
            vehicle = _vehicle.value,
            route = _uiState.value.route
        )

        // Update UI state with new battery state and preset ID
        _uiState.update {
            it.copy(
                batteryState = batteryState,
                selectedPresetId = preset.id
            )
        }
    }
}

data class MapUiState(
    val currentLocation: Location = Location.DEFAULT,
    val destinationAddress: String = "",
    val hasLocationPermission: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null,
    // Phase 2: Route calculation fields
    val route: Route? = null,
    val isCalculatingRoute: Boolean = false,
    val routeError: String? = null,
    val routePoints: List<LatLng> = emptyList(),
    // Phase 3: Battery state
    val batteryState: BatteryState? = null,
    // Phase 4: Charging stations
    val chargers: List<Charger> = emptyList(),
    val selectedCharger: Charger? = null,
    val isLoadingChargers: Boolean = false,
    val chargerError: String? = null,
    val showChargers: Boolean = false,
    // Phase 5: Charging plan
    val chargingPlan: ChargingPlan? = null,
    val swappingStop: ChargingStop? = null,
    // Phase 6: Offline mode
    val isOnline: Boolean = true,
    val isOfflineMode: Boolean = false,
    val cacheAgeText: String = "",
    val hasCachedRoute: Boolean = false,
    val hasCachedChargers: Boolean = false,
    // Phase 6: Vehicle preset selection
    val selectedPresetId: String = com.example.voltroute.domain.model.VehiclePreset.DEFAULT.id
)

