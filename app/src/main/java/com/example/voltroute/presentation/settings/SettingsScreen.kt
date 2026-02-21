package com.example.voltroute.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voltroute.data.remote.sync.SyncManager
import com.example.voltroute.data.remote.sync.SyncState
import com.example.voltroute.domain.model.VehiclePreset
import com.example.voltroute.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch

/**
 * SettingsScreen - Application settings and preferences
 *
 * STATELESS component that receives all state from MainActivity:
 * - selectedPresetId: Currently selected vehicle
 * - isDarkMode: Current theme preference
 * - syncManager: For cloud synchronization
 *
 * Sections:
 * 1. Vehicle Selection - List of 10 EV presets with specs
 * 2. Theme Toggle - Light/Dark mode selector
 * 3. Cloud Sync - Manual sync trigger with status
 * 4. About - App version and build info
 * 5. Account - Sign out
 *
 * UI/UX Design:
 * - Uses RadioButton for vehicle selection (single choice pattern)
 * - Selected vehicle row has subtle background highlight (primaryContainer)
 * - Theme selector uses equal-width Cards with border highlight
 * - All interactions call callbacks immediately (no "Save" button needed)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedPresetId: String,
    isDarkMode: Boolean,
    onVehicleSelected: (VehiclePreset) -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    syncManager: SyncManager
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = spacedBy(0.dp)
        ) {
            // ==================== SECTION 1: VEHICLE ====================
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.DirectionsCar,
                    title = "Vehicle"
                )
            }

            // List of all 10 vehicle presets
            items(VehiclePreset.ALL) { preset ->
                VehiclePresetItem(
                    preset = preset,
                    isSelected = preset.id == selectedPresetId,
                    onClick = { onVehicleSelected(preset) }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ==================== SECTION 2: THEME ====================
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Palette,
                    title = "Theme"
                )
            }

            item {
                ThemeSelector(
                    isDarkMode = isDarkMode,
                    onThemeChanged = onThemeChanged
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ==================== SECTION 3: CLOUD SYNC ====================
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Cloud,
                    title = "Cloud Sync"
                )
            }

            item {
                val syncState by syncManager.syncState.collectAsState()
                val lastSyncTime by syncManager.lastSyncTime.collectAsState()
                val scope = rememberCoroutineScope()

                ListItem(
                    modifier = Modifier.clickable {
                        // Trigger manual sync
                        scope.launch {
                            syncManager.syncNow()
                        }
                    },
                    headlineContent = { Text("Sync Now") },
                    supportingContent = {
                        when (syncState) {
                            is SyncState.Idle -> {
                                lastSyncTime?.let { time ->
                                    val minutes = (System.currentTimeMillis() - time) / 60000
                                    Text("Last synced: ${if (minutes < 1) "Just now" else "$minutes min ago"}")
                                } ?: Text("Not synced yet")
                            }
                            is SyncState.Syncing -> Text("Syncing...")
                            is SyncState.Success -> Text(
                                (syncState as SyncState.Success).message,
                                color = MaterialTheme.colorScheme.primary
                            )
                            is SyncState.Error -> Text(
                                (syncState as SyncState.Error).message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    leadingContent = {
                        when (syncState) {
                            is SyncState.Syncing -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            else -> Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ==================== SECTION 4: ABOUT ====================
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Info,
                    title = "About"
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text("App Version")
                    },
                    trailingContent = {
                        Text(
                            text = "1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text("Built with")
                    },
                    supportingContent = {
                        Text("Kotlin • Jetpack Compose • Google Maps")
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ==================== SECTION 5: ACCOUNT ====================
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Logout,
                    title = "Account"
                )
            }

            item {
                val authViewModel: AuthViewModel = hiltViewModel()

                ListItem(
                    modifier = Modifier.clickable {
                        authViewModel.signOut()
                    },
                    headlineContent = {
                        Text("Sign Out")
                    },
                    supportingContent = {
                        Text("Log out of your account")
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}

/**
 * SettingsSectionHeader - Visual separator for settings sections
 *
 * Displays an icon + title in primary color to group related settings
 */
@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * VehiclePresetItem - Single vehicle option in the list
 *
 * UI/UX Details:
 * - RadioButton on left for single-choice pattern
 * - Vehicle name is bold when selected
 * - Specs shown in supporting text (capacity + efficiency)
 * - Range displayed on right, colored primary when selected
 * - Entire row is clickable (not just the radio button)
 * - Selected row has subtle background tint (primaryContainer @ 30% alpha)
 *
 * @param preset The vehicle preset to display
 * @param isSelected Whether this preset is currently selected
 * @param onClick Callback when the row is clicked
 */
@Composable
private fun VehiclePresetItem(
    preset: VehiclePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Text(
                text = preset.displayName,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                text = preset.displaySpecs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        },
        trailingContent = {
            Text(
                text = preset.displayRange,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * ThemeSelector - Light/Dark mode toggle
 *
 * Two equal-width cards side-by-side:
 * - Selected card has primary background + border
 * - Unselected card has neutral background
 * - Icons and text update based on selection
 */
@Composable
private fun ThemeSelector(
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = spacedBy(12.dp)
    ) {
        ThemeOption(
            label = "Light",
            icon = Icons.Default.LightMode,
            isSelected = !isDarkMode,
            onClick = { onThemeChanged(false) },
            modifier = Modifier.weight(1f)
        )
        ThemeOption(
            label = "Dark",
            icon = Icons.Default.DarkMode,
            isSelected = isDarkMode,
            onClick = { onThemeChanged(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * ThemeOption - Single theme choice card
 *
 * UI/UX Details:
 * - Card background changes to primaryContainer when selected
 * - 2dp primary-colored border when selected
 * - Icon and text are bolder/colored when selected
 * - Entire card is clickable
 *
 * @param label "Light" or "Dark"
 * @param icon Sun or Moon icon
 * @param isSelected Whether this theme is currently active
 * @param onClick Callback to change theme
 * @param modifier Applied to the card (includes weight for equal widths)
 */
@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

