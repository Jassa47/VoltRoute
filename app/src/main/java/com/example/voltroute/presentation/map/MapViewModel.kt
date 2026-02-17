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
import com.example.voltroute.domain.usecase.PlanChargingStopsUseCase
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
    private val planChargingStopsUseCase: PlanChargingStopsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _vehicle = MutableStateFlow(Vehicle())
    val vehicle: StateFlow<Vehicle> = _vehicle.asStateFlow()

    init {
        checkLocationPermission()
        calculateInitialBatteryState()
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

    /**
     * Cancel charger swap operation
     * Clears the swappingStop to exit swap mode
     */
    fun cancelSwap() {
        _uiState.update { it.copy(swappingStop = null) }
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
    val swappingStop: ChargingStop? = null
)

