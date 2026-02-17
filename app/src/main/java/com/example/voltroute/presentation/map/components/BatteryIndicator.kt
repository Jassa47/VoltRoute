package com.example.voltroute.presentation.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltroute.domain.model.BatteryLevel
import com.example.voltroute.domain.model.BatteryState

/**
 * Battery indicator with animated progress bar and color-coded status
 *
 * Shows current battery percentage with smooth animations:
 * - Progress bar animates when battery state changes
 * - Color transitions based on battery level (green → yellow → orange → red)
 *
 * Colors:
 * - HIGH (≥60%): Green - Safe for long trips
 * - MEDIUM (30-59%): Yellow - Moderate range
 * - LOW (15-29%): Orange - Consider charging soon
 * - CRITICAL (<15%): Red - Charge immediately
 *
 * @param batteryState Current battery state with charge level
 * @param modifier Optional modifier for positioning/styling
 */
@Composable
fun BatteryIndicator(
    batteryState: BatteryState,
    modifier: Modifier = Modifier
) {
    // Animate progress bar smoothly when battery percentage changes
    val animatedProgress by animateFloatAsState(
        targetValue = (batteryState.currentBatteryPercent / 100f).toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "battery_progress_animation"
    )

    // Get color based on battery level
    val targetColor = when (batteryState.batteryLevel) {
        BatteryLevel.HIGH -> Color(0xFF4CAF50)      // Green
        BatteryLevel.MEDIUM -> Color(0xFFFFEB3B)    // Yellow
        BatteryLevel.LOW -> Color(0xFFFF9800)       // Orange
        BatteryLevel.CRITICAL -> Color(0xFFF44336)  // Red
    }

    // Animate color transitions smoothly
    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "battery_color_animation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header row: Battery label and percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Battery emoji + label
            Text(
                text = "🔋 Battery",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Right: Percentage with animated color
            Text(
                text = batteryState.batteryPercentText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }

        // Progress bar with rounded corners
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = animatedColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

