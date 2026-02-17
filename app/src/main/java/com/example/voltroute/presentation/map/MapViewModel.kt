package com.example.voltroute.presentation.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voltroute.data.location.LocationClient
import com.example.voltroute.domain.model.BatteryState
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Route
import com.example.voltroute.domain.model.Vehicle
import com.example.voltroute.domain.usecase.CalculateBatteryUseCase
import com.example.voltroute.domain.usecase.CalculateRouteUseCase
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
    private val calculateBatteryUseCase: CalculateBatteryUseCase
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
    val batteryState: BatteryState? = null
)

