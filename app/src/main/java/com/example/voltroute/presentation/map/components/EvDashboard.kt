package com.example.voltroute.presentation.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.BatteryState

/**
 * Collapsible EV Dashboard component
 *
 * IMPORTANT: Starts COLLAPSED by default to maximize map visibility
 * and allow users to see charging station markers.
 *
 * Collapsed state: Shows only header with vehicle name, battery %, and range
 * Expanded state: Shows full dashboard with battery indicator, stats, and warnings
 *
 * Tap the header to toggle between collapsed and expanded states
 *
 * @param batteryState Current battery state and route energy calculations
 * @param modifier Optional modifier for positioning/styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvDashboard(
    batteryState: BatteryState,
    modifier: Modifier = Modifier
) {
    // Internal state for collapse/expand - starts COLLAPSED (false)
    // This ensures the map is fully visible on initial load
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // SECTION 1: Header (ALWAYS VISIBLE, clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Vehicle info + battery + range
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricCar,
                        contentDescription = "Electric Vehicle",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "⚡ Rivian R1T",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = batteryState.batteryPercentText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = batteryState.remainingRangeText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right side: Expand/collapse arrow
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.KeyboardArrowDown
                    else
                        Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SECTION 2: Expanded Content (animated visibility)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Divider
                    HorizontalDivider()

                    // Battery indicator
                    BatteryIndicator(batteryState = batteryState)

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Energy stat
                        StatItem(
                            icon = Icons.Default.BatteryChargingFull,
                            label = "Energy",
                            value = batteryState.currentEnergyText
                        )

                        VerticalDivider(
                            modifier = Modifier.height(40.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Range stat
                        StatItem(
                            icon = Icons.Default.Route,
                            label = "Range",
                            value = batteryState.remainingRangeText
                        )

                        // Needed stat (only when route calculated)
                        if (batteryState.requiredEnergyKWh != null) {
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            StatItem(
                                icon = Icons.Default.Speed,
                                label = "Needed",
                                value = "%.1f kWh".format(batteryState.requiredEnergyKWh)
                            )
                        }
                    }

                    // Route battery usage (only when route exists)
                    batteryState.percentageUsedForRoute?.let { percentage ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Route Battery Usage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val percentageColor = if (batteryState.canReachDestination) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }

                                Text(
                                    text = "%.0f%%".format(percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = percentageColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { ((percentage / 100.0).coerceAtMost(1.0)).toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (batteryState.canReachDestination) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Battery warning card
                    BatteryWarningCard(batteryState = batteryState)
                }
            }
        }
    }
}

/**
 * Single stat item for dashboard
 * Shows icon, value, and label in a vertical column
 *
 * @param icon Icon representing the stat type
 * @param label Description text (e.g., "Energy", "Range")
 * @param value Main value to display (e.g., "108.0 kWh", "600 km")
 * @param modifier Optional modifier for positioning/styling
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

