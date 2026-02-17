package com.example.voltroute.presentation.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.BatteryState

/**
 * Warning card displayed when battery is insufficient for the route
 *
 * Shows:
 * - Warning header with icon
 * - Energy comparison (available vs required)
 * - Number of charging stops needed
 *
 * Only visible when:
 * - Destination cannot be reached on current charge
 * - A route has been calculated (requiredEnergyKWh is not null)
 *
 * Uses AnimatedVisibility for smooth expand/collapse transitions
 *
 * @param batteryState Battery state with route energy requirements
 * @param modifier Optional modifier for positioning/styling
 */
@Composable
fun BatteryWarningCard(
    batteryState: BatteryState,
    modifier: Modifier = Modifier
) {
    // Only show warning when destination cannot be reached and route exists
    val shouldShow = !batteryState.canReachDestination &&
                     batteryState.requiredEnergyKWh != null

    AnimatedVisibility(
        visible = shouldShow,
        modifier = modifier,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)  // Light orange background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Warning header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFE65100),  // Deep orange
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Insufficient Battery Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)  // Deep orange
                    )
                }

                // 2. Divider
                HorizontalDivider(color = Color(0xFFFFCC80))  // Light orange

                // 3. Energy comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Available energy
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = batteryState.currentEnergyText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)  // Green
                        )
                        Text(
                            text = "Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Center: Arrow
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Right: Required energy
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = batteryState.requiredEnergyText ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)  // Deep orange
                        )
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Divider
                HorizontalDivider(color = Color(0xFFFFCC80))  // Light orange

                // 5. Charging stops information
                batteryState.warningMessage?.let { message ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EvStation,
                            contentDescription = "Charging station",
                            tint = Color(0xFF1565C0),  // Blue
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1565C0)  // Blue
                        )
                    }
                }
            }
        }
    }
}

