package com.example.voltroute.presentation.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voltroute.data.location.LocationClient
import com.example.voltroute.domain.model.Location
import com.example.voltroute.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationClient: LocationClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _vehicle = MutableStateFlow(Vehicle())
    val vehicle: StateFlow<Vehicle> = _vehicle.asStateFlow()

    init {
        checkLocationPermission()
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
}

data class MapUiState(
    val currentLocation: Location = Location.DEFAULT,
    val destinationAddress: String = "",
    val hasLocationPermission: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null
)