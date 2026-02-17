package com.example.voltroute.presentation.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.BatteryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvDashboard(
    batteryState: BatteryState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricCar,
                    contentDescription = "Electric Vehicle",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "⚡ EV Dashboard - Rivian R1T",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            BatteryIndicator(batteryState = batteryState)

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Energy",
                    value = batteryState.currentEnergyText
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                StatItem(
                    icon = Icons.Default.Route,
                    label = "Range",
                    value = batteryState.remainingRangeText
                )

                if (batteryState.requiredEnergyKWh != null) {
                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    StatItem(
                        icon = Icons.Default.Speed,
                        label = "Needed",
                        value = "%.1f kWh".format(batteryState.requiredEnergyKWh)
                    )
                }
            }

            // Route battery usage
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val percentageColor = if (batteryState.canReachDestination) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }

                        Text(
                            text = "%.0f%%".format(percentage),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = percentageColor
                        )
                    }

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

            BatteryWarningCard(batteryState = batteryState)
        }
    }
}

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
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

