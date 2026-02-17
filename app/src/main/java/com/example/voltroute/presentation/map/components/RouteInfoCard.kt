package com.example.voltroute.presentation.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.Route

/**
 * Card displaying route information (distance and duration)
 *
 * Shows key route metrics in a clean, Material Design 3 styled card:
 * - Distance with car icon
 * - Duration with clock icon
 *
 * Positioned at bottom of map screen to provide at-a-glance route info
 * without obstructing the map view.
 *
 * @param route The calculated route to display
 * @param modifier Optional modifier for positioning/styling
 */
@Composable
fun RouteInfoCard(
    route: Route,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distance section
            RouteInfoItem(
                icon = Icons.Default.DirectionsCar,
                label = "Distance",
                value = route.distanceText,
                modifier = Modifier.weight(1f)
            )

            // Vertical divider
            VerticalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
            )

            // Duration section
            RouteInfoItem(
                icon = Icons.Default.Schedule,
                label = "Duration",
                value = route.durationText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Single info item within the route card
 * Displays icon, label, and value in a column
 */
@Composable
private fun RouteInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Simple vertical divider for separating info items
 */
@Composable
private fun VerticalDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier.width(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

