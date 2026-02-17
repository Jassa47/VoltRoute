package com.example.voltroute.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.voltroute.domain.model.VehiclePreset

/**
 * SettingsScreen - STUB for Part 1
 *
 * This is a placeholder to allow compilation and navigation setup.
 * Full implementation will be added in Part 2.
 */
@Composable
fun SettingsScreen(
    selectedPresetId: String,
    isDarkMode: Boolean,
    onVehicleSelected: (VehiclePreset) -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Settings Screen - Coming in Part 2")
    }
}

