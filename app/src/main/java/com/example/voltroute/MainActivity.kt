package com.example.voltroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.voltroute.data.auth.AuthRepository
import com.example.voltroute.data.auth.AuthState
import com.example.voltroute.domain.model.VehiclePreset
import com.example.voltroute.presentation.navigation.AppNavigation
import com.example.voltroute.ui.theme.VoltRouteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity - Entry point for VoltRoute app
 *
 * Manages app-level state:
 * - Authentication state (from Firebase)
 * - Theme mode (light/dark)
 * - Selected vehicle preset
 *
 * These are hoisted here so they persist across navigation
 * and can be shared between screens.
 *
 * AuthRepository is injected by Hilt to observe authentication state.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // System dark mode as initial value
            val systemDark = isSystemInDarkTheme()

            // Theme state - owned at app level
            var isDarkMode by remember { mutableStateOf(systemDark) }

            // Selected vehicle preset - owned at app level
            var selectedPresetId by remember {
                mutableStateOf(VehiclePreset.DEFAULT.id)
            }

            // Collect auth state from repository
            // This determines navigation (LOGIN vs MAP)
            val authState by authRepository.authState
                .collectAsState(initial = AuthState.Loading)

            VoltRouteTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    isDarkMode = isDarkMode,
                    selectedPresetId = selectedPresetId,
                    authState = authState,
                    onVehicleSelected = { preset: VehiclePreset ->
                        selectedPresetId = preset.id
                    },
                    onThemeChanged = { darkMode: Boolean ->
                        isDarkMode = darkMode
                    }
                )
            }
        }
    }
}