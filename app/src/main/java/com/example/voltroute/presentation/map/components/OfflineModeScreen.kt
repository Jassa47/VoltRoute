package com.example.voltroute.presentation.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Offline mode screen component
 *
 * Displays when the app detects no internet connection.
 * Provides options to:
 * - View cached route data
 * - View cached chargers
 * - Retry internet connection
 *
 * UX Design decisions:
 * - Centered card makes the offline state clear and focused
 * - Shows cache age to indicate data freshness
 * - Conditional buttons only show if data is available
 * - Retry button always visible for easy reconnection
 * - Error color scheme communicates the offline state
 *
 * This component is stateless - all state managed by MapViewModel
 *
 * @param cacheAgeText Human-readable cache age ("5 minutes ago")
 * @param hasCachedRoute Whether a route is cached
 * @param hasCachedChargers Whether chargers are cached
 * @param onViewCachedRoute Callback to load cached route
 * @param onViewCachedChargers Callback to load cached chargers
 * @param onRetry Callback to retry connection
 * @param modifier Optional modifier for positioning
 */
@Composable
fun OfflineModeScreen(
    cacheAgeText: String,
    hasCachedRoute: Boolean,
    hasCachedChargers: Boolean,
    onViewCachedRoute: () -> Unit,
    onViewCachedChargers: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ITEM 1: Large offline icon
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "No internet connection",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )

                // ITEM 2: Title
                Text(
                    text = "You're Offline",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // ITEM 3: Subtitle with cache age
                Text(
                    text = "No internet connection detected.\nShowing data cached $cacheAgeText.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // ITEM 4: Divider
                HorizontalDivider()

                // ITEM 5: Section title
                Text(
                    text = "Available Offline Data",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // ITEM 6: Cached route button (only if data exists)
                if (hasCachedRoute) {
                    OutlinedButton(
                        onClick = onViewCachedRoute,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = "View route",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Cached Route")
                    }
                }

                // ITEM 7: Cached chargers button (only if data exists)
                if (hasCachedChargers) {
                    OutlinedButton(
                        onClick = onViewCachedChargers,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.EvStation,
                            contentDescription = "View chargers",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Cached Chargers")
                    }
                }

                // ITEM 8: No cache message (only if no data cached)
                if (!hasCachedRoute && !hasCachedChargers) {
                    Text(
                        text = "No cached data available.\nCalculate a route while online first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // ITEM 9: Divider
                HorizontalDivider()

                // ITEM 10: Retry connection button (always show)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry connection",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Connection")
                }
            }
        }
    }
}

