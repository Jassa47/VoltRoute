package com.example.voltroute.presentation.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.ChargingPlan
import com.example.voltroute.domain.model.ChargingStop

/**
 * Charging plan card showing all recommended stops
 *
 * IMPORTANT: Starts EXPANDED by default (opposite of EvDashboard)
 * Reason: Charging plan is critical information users need to see immediately
 *
 * Features:
 * - Collapsible header with trip summary
 * - List of charging stops with swap buttons
 * - Total trip time calculation
 * - Color-coded power levels
 *
 * Only visible when a charging plan exists and has stops
 *
 * @param chargingPlan The calculated charging plan (null if no plan)
 * @param onSwapStop Callback when user wants to swap a charging stop
 * @param modifier Optional modifier for positioning/styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingPlanCard(
    chargingPlan: ChargingPlan?,
    onSwapStop: (ChargingStop) -> Unit,
    modifier: Modifier = Modifier
) {
    // Early return if no plan or no stops needed
    if (chargingPlan == null || !chargingPlan.needsCharging) {
        return
    }

    // Internal state for collapse/expand - starts EXPANDED (true)
    // This ensures charging plan is visible immediately
    var isExpanded by remember { mutableStateOf(true) }

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
            // HEADER ROW (always visible, clickable to toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Charging plan summary
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EvStation,
                        contentDescription = "Charging Plan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "🛑 Charging Plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${chargingPlan.stops.size} stops",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = chargingPlan.totalChargingTimeText,
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

            // EXPANDED CONTENT (animated visibility)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Divider before stops list
                    HorizontalDivider()

                    // List of charging stops
                    chargingPlan.stops.forEach { stop ->
                        ChargingStopItem(
                            stop = stop,
                            onSwap = { onSwapStop(stop) }
                        )

                        // Divider between stops (but not after last one)
                        if (stop.stopNumber < chargingPlan.stops.size) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Divider before total
                    HorizontalDivider()

                    // Total trip time summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Total time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Total trip time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = chargingPlan.totalTripTimeText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual charging stop item
 *
 * Shows:
 * - Stop number badge
 * - Charger name
 * - Swap button
 * - Color-coded power indicator (dot)
 * - Power, arrival battery, charge time
 *
 * @param stop The charging stop details
 * @param onSwap Callback when user taps swap button
 */
@Composable
private fun ChargingStopItem(
    stop: ChargingStop,
    onSwap: () -> Unit
) {
    // Get color based on charger power level
    val powerColor = getPowerLevelColor(stop.charger.powerLevel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ROW 1: Stop header with name and swap button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Stop number badge + charger name
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop number badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = " ${stop.stopNumber} ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Charger name
                Text(
                    text = stop.charger.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: Swap button
            TextButton(
                onClick = onSwap,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap charger",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "Swap",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // ROW 2: Stats with colored power indicator dot
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored dot indicator (represents power level)
            Surface(
                color = powerColor,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(8.dp)
            ) {}

            // Power rating
            Text(
                text = stop.charger.powerText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = powerColor
            )

            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Arrival battery percentage
            Text(
                text = stop.arrivalBatteryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Estimated charge time
            Text(
                text = stop.chargeTimeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

