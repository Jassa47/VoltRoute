@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.voltroute.presentation.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voltroute.presentation.map.components.DestinationInput
import com.example.voltroute.presentation.map.components.RouteInfoCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()

    // Request location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Request permission on first composition if not granted
    LaunchedEffect(uiState.hasLocationPermission) {
        if (!uiState.hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Camera position for map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(uiState.currentLocation.latitude, uiState.currentLocation.longitude),
            12f
        )
    }

    // Update camera when location changes (only if no route is displayed)
    LaunchedEffect(uiState.currentLocation) {
        if (uiState.route == null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(uiState.currentLocation.latitude, uiState.currentLocation.longitude),
                12f
            )
        }
    }

    // Update camera to show full route when calculated
    LaunchedEffect(uiState.route) {
        uiState.route?.let { route ->
            if (uiState.routePoints.isNotEmpty()) {
                // Create bounds that include all route points
                val boundsBuilder = LatLngBounds.Builder()
                uiState.routePoints.forEach { point ->
                    boundsBuilder.include(point)
                }
                val bounds = boundsBuilder.build()

                // Animate camera to show full route with padding
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                cameraPositionState.animate(cameraUpdate, 1000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoltRoute") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = uiState.hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true
                )
            ) {
                // Current location marker
                if (uiState.hasLocationPermission) {
                    Marker(
                        state = rememberMarkerState(
                            position = LatLng(
                                uiState.currentLocation.latitude,
                                uiState.currentLocation.longitude
                            )
                        ),
                        title = "Current Location"
                    )
                }

                // Route polyline
                if (uiState.routePoints.isNotEmpty()) {
                    Polyline(
                        points = uiState.routePoints,
                        color = Color(0xFF2196F3), // Material Blue
                        width = 12f
                    )
                }

                // Destination marker
                uiState.route?.let { route ->
                    Marker(
                        state = rememberMarkerState(
                            position = LatLng(
                                route.endLocation.latitude,
                                route.endLocation.longitude
                            )
                        ),
                        title = "Destination",
                        snippet = route.endLocation.name,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            // Destination input overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        DestinationInput(
                            value = uiState.destinationAddress,
                            onValueChange = viewModel::onDestinationChanged,
                            enabled = uiState.hasLocationPermission
                        )

                        // Calculate Route button
                        if (uiState.destinationAddress.isNotBlank() && uiState.route == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.calculateRoute() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                enabled = !uiState.isCalculatingRoute
                            ) {
                                if (uiState.isCalculatingRoute) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Calculating...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Calculate Route"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Calculate Route")
                                }
                            }
                        }

                        // Clear Route button
                        if (uiState.route != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.clearRoute() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text("Clear Route")
                            }
                        }
                    }
                }
            }

            // Route Info Card at bottom
            if (uiState.route != null) {
                RouteInfoCard(
                    route = uiState.route!!,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Loading indicator for location
            if (uiState.isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Location error snackbar
            uiState.locationError?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Route error snackbar
            uiState.routeError?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearRouteError) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}