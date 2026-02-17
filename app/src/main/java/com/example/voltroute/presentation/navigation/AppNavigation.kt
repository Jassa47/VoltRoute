package com.example.voltroute.presentation.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voltroute.domain.model.VehiclePreset
import com.example.voltroute.presentation.history.TripHistoryScreen
import com.example.voltroute.presentation.map.MapScreen
import com.example.voltroute.presentation.settings.SettingsScreen
import com.example.voltroute.presentation.splash.SplashScreen

/**
 * AppRoutes - Navigation destination constants
 * Uses object pattern for compile-time constant access
 */
object AppRoutes {
    const val SPLASH = "splash"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

/**
 * AppNavigation - Top-level navigation graph
 *
 * Navigation flow:
 * SPLASH -> MAP <-> SETTINGS
 *
 * Transitions:
 * - SPLASH exits with fade out (removed from back stack)
 * - MAP enters with fade in
 * - MAP <-> SETTINGS use horizontal slide animations (Android standard)
 *   Forward: Current slides left, new slides in from right
 *   Back: Current slides right, previous slides in from left
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    selectedPresetId: String,
    onVehicleSelected: (VehiclePreset) -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
        // SPLASH SCREEN
        // Shows once on app launch, then removed from back stack
        composable(
            route = AppRoutes.SPLASH,
            exitTransition = {
                fadeOut(animationSpec = tween(500))
            }
        ) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(AppRoutes.MAP) {
                        // Remove splash from back stack so back button doesn't return to it
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // MAP SCREEN (Main screen)
        // Entry point after splash, can navigate to settings
        composable(
            route = AppRoutes.MAP,
            enterTransition = {
                fadeIn(animationSpec = tween(500))
            },
            // Exit when navigating forward to settings: slide left
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            // Re-enter when returning from settings: slide in from left
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                )
            }
        ) {
            MapScreen(
                onNavigateToSettings = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
                onNavigateToHistory = {
                    navController.navigate(AppRoutes.HISTORY)
                }
            )
        }

        // SETTINGS SCREEN
        // Slide in from right, slide out to right on back
        composable(
            route = AppRoutes.SETTINGS,
            // Enter when navigating forward: slide in from right
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            // Exit when navigating back: slide out to right
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            SettingsScreen(
                selectedPresetId = selectedPresetId,
                isDarkMode = isDarkMode,
                onVehicleSelected = onVehicleSelected,
                onThemeChanged = onThemeChanged,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // TRIP HISTORY SCREEN
        // Slide in from right (same as Settings), slide out to right on back
        composable(
            route = AppRoutes.HISTORY,
            // Enter when navigating forward: slide in from right
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            // Exit when navigating back: slide out to right
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            TripHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

