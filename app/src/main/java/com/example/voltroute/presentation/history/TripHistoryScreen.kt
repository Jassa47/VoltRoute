package com.example.voltroute.presentation.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voltroute.data.local.entity.TripHistoryEntity

/**
 * TripHistoryScreen - Display list of saved trips with swipe-to-delete
 *
 * Features:
 * - Empty state when no trips exist
 * - LazyColumn list with swipe-to-delete gesture
 * - Clear all trips confirmation dialog
 * - Automatic updates via Room Flow → StateFlow
 *
 * Key Technical Details:
 * - Uses key = { trip.id } in LazyColumn for stable item identity
 * - SwipeToDismissBox for left-swipe delete gesture
 * - StateFlow automatically emits updates when Room data changes
 * - No manual list refresh needed - Room handles it!
 *
 * UI/UX:
 * - Shows DeleteSweep icon only when trips exist
 * - Empty state provides helpful guidance
 * - Destructive actions (delete/clear) use confirmation
 * - Swipe reveals "Delete" label before confirming
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TripHistoryViewModel = hiltViewModel()
) {
    // Collect trips from StateFlow
    // Updates automatically when Room database changes
    val trips by viewModel.trips.collectAsState()

    // Dialog state for clear all confirmation
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trip History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                // Only show clear all button when trips exist
                actions = {
                    if (trips.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all history"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        // BRANCH 1: Empty state
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No trips yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Calculate a route to save\nyour first trip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // BRANCH 2: List of trips
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = spacedBy(8.dp)
            ) {
                items(
                    items = trips,
                    // CRITICAL: Use stable ID as key for proper animations
                    // Without this, swipe animations break when items are deleted
                    key = { trip -> trip.id }
                ) { trip ->
                    SwipeableTripItem(
                        trip = trip,
                        onDelete = { viewModel.deleteTrip(trip) }
                    )
                }
            }
        }

        // Clear all confirmation dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = {
                    Text("Clear History")
                },
                text = {
                    Text("Delete all ${trips.size} saved trips? This cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllTrips()
                            showClearDialog = false
                        }
                    ) {
                        Text(
                            text = "Clear All",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * SwipeableTripItem - Trip card with left-swipe to delete
 *
 * KEY CONCEPT - SwipeToDismissBox:
 *
 * SwipeToDismissBox provides the swipe gesture handling:
 * - User swipes left → reveals "Delete" background
 * - User releases → confirmValueChange is called
 * - If returns true → dismiss animation plays, item removed
 * - If returns false → item snaps back to original position
 *
 * enableDismissFromStartToEnd = false:
 * - Disables right swipe (StartToEnd direction)
 * - Only left swipe (EndToStart) triggers delete
 * - Prevents accidental swipes in wrong direction
 *
 * confirmValueChange flow:
 * 1. User swipes left past threshold
 * 2. confirmValueChange called with EndToStart value
 * 3. onDelete() triggers Room delete operation
 * 4. Returns true to confirm dismiss animation
 * 5. Item slides out while animating
 * 6. Room emits updated list via Flow
 * 7. LazyColumn automatically updates (key helps here!)
 *
 * @param trip The trip data to display
 * @param onDelete Callback when user confirms delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTripItem(
    trip: TripHistoryEntity,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // User swiped left past threshold
                onDelete()
                true // Confirm dismiss - play animation and remove item
            } else {
                // User swiped in other direction (shouldn't happen with our config)
                false // Cancel dismiss - snap back to original position
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false, // No right swipe
        enableDismissFromEndToStart = true,   // Only left swipe
        backgroundContent = {
            // What's revealed behind the card when swiping
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    horizontalArrangement = spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete trip",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) {
        // The actual card content
        TripHistoryCard(trip = trip)
    }
}

/**
 * TripHistoryCard - Display trip details in a card
 *
 * Layout structure:
 * 1. Header: Destination + Date
 * 2. Divider
 * 3. Stats row: Distance, Duration, Energy, Cost
 * 4. Charging stops (if any)
 * 5. Footer: Vehicle name + Time
 *
 * Uses animateContentSize() for smooth layout transitions.
 */
@Composable
private fun TripHistoryCard(
    trip: TripHistoryEntity
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // Smooth size changes
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = spacedBy(8.dp)
        ) {
            // SECTION 1: Header - Destination + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = trip.destinationAddress,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = trip.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SECTION 2: Divider
            HorizontalDivider()

            // SECTION 3: Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripStatChip(
                    emoji = "📍",
                    value = trip.distanceText,
                    label = "Distance"
                )
                TripStatChip(
                    emoji = "⏱️",
                    value = trip.durationText,
                    label = "Drive time"
                )
                TripStatChip(
                    emoji = "⚡",
                    value = trip.energyText,
                    label = "Energy"
                )
                TripStatChip(
                    emoji = "💰",
                    value = trip.costText,
                    label = "Est. cost"
                )
            }

            // SECTION 4: Charging stops (only if needed)
            if (trip.chargingStopsCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EvStation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${trip.chargingStopsCount} charging " +
                                        if (trip.chargingStopsCount == 1) "stop" else "stops",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "+${trip.totalChargingTimeMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SECTION 5: Footer - Vehicle + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = trip.vehicleName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = trip.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * TripStatChip - Display a single stat with emoji, value, and label
 *
 * Used for the 4 stats in each trip card:
 * - Distance (📍)
 * - Duration (⏱️)
 * - Energy (⚡)
 * - Cost (💰)
 *
 * Vertical layout: emoji on top, value (bold), label on bottom
 */
@Composable
private fun TripStatChip(
    emoji: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(2.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

