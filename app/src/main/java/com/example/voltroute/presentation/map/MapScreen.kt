@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.voltroute.presentation.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.PowerLevel
import com.example.voltroute.presentation.map.components.ChargerInfoCard
import com.example.voltroute.presentation.map.components.ChargingPlanCard
import com.example.voltroute.presentation.map.components.DestinationInput
import com.example.voltroute.presentation.map.components.EvDashboard
import com.example.voltroute.presentation.map.components.OfflineModeScreen
import com.example.voltroute.presentation.map.components.RouteInfoCard
import com.example.voltroute.presentation.map.components.getPowerLevelColor
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
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

    // Offline mode detection - show special screen when no internet
    if (uiState.isOfflineMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VoltRoute")
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        ) { padding ->
            OfflineModeScreen(
                cacheAgeText = uiState.cacheAgeText,
                hasCachedRoute = uiState.hasCachedRoute,
                hasCachedChargers = uiState.hasCachedChargers,
                onViewCachedRoute = { viewModel.loadCachedData() },
                onViewCachedChargers = { viewModel.loadCachedData() },
                onRetry = { viewModel.retryConnection() },
                modifier = Modifier.padding(padding)
            )
        }
    } else {
        // Normal online mode - show map and all features
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VoltRoute",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Trip History"
                        )
                    }
                },
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

                // Charger markers (color-coded by power level)
                if (uiState.showChargers) {
                    uiState.chargers.forEach { charger ->
                        ChargerMarker(
                            charger = charger,
                            onClick = {
                                if (uiState.swappingStop != null) {
                                    // In swap mode - replace the stop!
                                    viewModel.onChargerSelectedAsSwap(charger)
                                } else {
                                    // Normal mode - show info card
                                    viewModel.onChargerSelected(charger)
                                }
                                true
                            }
                        )
                    }
                }

                // Charging stop markers (green, numbered - these are the recommended stops)
                uiState.chargingPlan?.stops?.forEach { stop ->
                    Marker(
                        state = rememberMarkerState(
                            position = LatLng(
                                stop.charger.location.latitude,
                                stop.charger.location.longitude
                            )
                        ),
                        title = "Stop ${stop.stopNumber}: ${stop.charger.name}",
                        snippet = "${stop.charger.powerText} • ${stop.chargeTimeText}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }
            }

            // Destination input overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Swap mode banner (appears when user is selecting replacement charger)
                uiState.swappingStop?.let { swappingStop ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tap a charger to replace Stop ${swappingStop.stopNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = { viewModel.cancelSwap() }
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

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

                        // Toggle Chargers button
                        if (uiState.route != null || uiState.chargers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.toggleChargers() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.showChargers)
                                        MaterialTheme.colorScheme.secondary
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (uiState.isLoadingChargers) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.EvStation,
                                        contentDescription = "Charging Stations",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.showChargers)
                                        "Hide Chargers (${uiState.chargers.size})"
                                    else
                                        "Show Chargers"
                                )
                            }
                        }
                    }
                }
            }
            // Bottom content: Charger Info, Charging Plan, EV Dashboard, and Route Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Charger Info Card (slides up when marker tapped)
                ChargerInfoCard(
                    charger = uiState.selectedCharger,
                    onDismiss = viewModel::onChargerDismissed,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Charging Plan Card (expanded by default, collapsible)
                ChargingPlanCard(
                    chargingPlan = uiState.chargingPlan,
                    onSwapStop = viewModel::onSwapStop,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. EV Dashboard (collapsed by default)
                uiState.batteryState?.let { batteryState: com.example.voltroute.domain.model.BatteryState ->
                    EvDashboard(
                        batteryState = batteryState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Route Info Card (shown when route calculated)
                uiState.route?.let { route: com.example.voltroute.domain.model.Route ->
                    RouteInfoCard(
                        route = route,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

            // Charger error snackbar
            uiState.chargerError?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 200.dp, start = 16.dp, end = 16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearChargerError) {
                            Text("Retry")
                        }
                    }
                ) {
                    Text("Charger load failed: $error")
                }
            }
        }
    }
    } // End of else block (online mode)
}

/**
 * Charger marker with color-coded icon based on power level
 *
 * Marker colors:
 * - ULTRA_FAST (150kW+): Blue (HUE_AZURE)
 * - FAST (50-149kW): Yellow (HUE_YELLOW)
 * - STANDARD (<50kW): Red (HUE_RED)
 *
 * @param charger Charging station to display
 * @param onClick Callback when marker is tapped (returns true to consume event)
 */
@Composable
private fun ChargerMarker(
    charger: Charger,
    onClick: () -> Boolean
) {
    // Determine marker color based on power level
    val markerHue = when (charger.powerLevel) {
        PowerLevel.ULTRA_FAST -> BitmapDescriptorFactory.HUE_AZURE   // Blue
        PowerLevel.FAST -> BitmapDescriptorFactory.HUE_YELLOW        // Yellow
        PowerLevel.STANDARD -> BitmapDescriptorFactory.HUE_RED       // Red
    }

    Marker(
        state = rememberMarkerState(
            position = LatLng(
                charger.location.latitude,
                charger.location.longitude
            )
        ),
        title = charger.name,
        snippet = "${charger.powerText} • ${charger.portsText}",
        onClick = { onClick() },
        icon = BitmapDescriptorFactory.defaultMarker(markerHue)
    )
}
