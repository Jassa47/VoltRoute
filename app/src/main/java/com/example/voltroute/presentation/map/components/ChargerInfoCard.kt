package com.example.voltroute.presentation.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.Charger
import com.example.voltroute.domain.model.PowerLevel

/**
 * Get color for power level visualization
 * Used for markers and UI indicators
 *
 * @param powerLevel The charging power level category
 * @return Color for the power level (Blue/Yellow/Red)
 */
fun getPowerLevelColor(powerLevel: PowerLevel): Color =
    when (powerLevel) {
        PowerLevel.ULTRA_FAST -> Color(0xFF2196F3) // Blue - 150kW+
        PowerLevel.FAST -> Color(0xFFFFEB3B)       // Yellow - 50-149kW
        PowerLevel.STANDARD -> Color(0xFFF44336)   // Red - <50kW
    }

/**
 * Charging station information card with animated slide-in/out
 *
 * Displays detailed information about a selected charging station:
 * - Station name with close button
 * - Power output, connector types, number of ports
 * - Distance from current location
 *
 * Uses AnimatedVisibility for smooth entrance/exit animations
 *
 * @param charger Selected charging station (null = hidden)
 * @param onDismiss Callback when user closes the card
 * @param modifier Optional modifier for positioning/styling
 */
@Composable
fun ChargerInfoCard(
    charger: Charger?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = charger != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        // Only render content if charger is not null
        charger?.let { station ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Station name + close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Icon + Name
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EvStation,
                                contentDescription = "Charging Station",
                                tint = getPowerLevelColor(station.powerLevel),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Right: Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Divider
                    HorizontalDivider()

                    // Stats row: Power, Connector, Ports
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Power stat
                        ChargerStatItem(
                            icon = Icons.Default.FlashOn,
                            label = "Power",
                            value = station.powerText,
                            tint = getPowerLevelColor(station.powerLevel)
                        )

                        // Divider
                        VerticalDivider(
                            modifier = Modifier.height(40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Connector stat (truncate long text)
                        val connectorDisplay = if (station.connectorText.length > 10) {
                            station.connectorText.take(10) + "..."
                        } else {
                            station.connectorText
                        }
                        ChargerStatItem(
                            icon = Icons.Default.Cable,
                            label = "Connector",
                            value = connectorDisplay,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        // Divider
                        VerticalDivider(
                            modifier = Modifier.height(40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Ports stat
                        ChargerStatItem(
                            icon = Icons.Default.Power,
                            label = "Ports",
                            value = station.portsText,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Distance row (only if distance available)
                    station.distanceText?.let { distance ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Distance",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = distance,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single stat item for charger information
 * Shows icon, value, and label in a vertical column
 *
 * @param icon Icon representing the stat type
 * @param label Description text (e.g., "Power", "Connector")
 * @param value Main value to display (e.g., "150 kW", "CCS")
 * @param tint Color for the icon
 * @param modifier Optional modifier for positioning/styling
 */
@Composable
private fun ChargerStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )

        // Value (bold)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Label (subtle)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

