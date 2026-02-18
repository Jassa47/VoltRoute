package com.example.voltroute.presentation.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voltroute.data.auth.AuthState
import com.example.voltroute.domain.model.VehiclePreset
import com.example.voltroute.presentation.auth.LoginScreen
import com.example.voltroute.presentation.auth.PhoneAuthScreen
import com.example.voltroute.presentation.auth.SignUpScreen
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
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PHONE_AUTH = "phone_auth"
    const val MAP = "map"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

/**
 * AppNavigation - Top-level navigation graph
 *
 * Navigation flow with authentication:
 * SPLASH -> LOGIN/MAP (based on authState)
 * LOGIN <-> SIGNUP <-> PHONE_AUTH
 * MAP <-> SETTINGS <-> HISTORY
 *
 * Auth flow:
 * - If authenticated: SPLASH -> MAP
 * - If unauthenticated: SPLASH -> LOGIN
 * - After sign in/up: LOGIN -> MAP (clear auth backstack)
 *
 * Transitions:
 * - SPLASH exits with fade out (removed from back stack)
 * - LOGIN enters/exits with fade
 * - SIGNUP slides vertically (modal feel)
 * - PHONE_AUTH slides horizontally (forward navigation)
 * - MAP <-> SETTINGS use horizontal slide animations (Android standard)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    selectedPresetId: String,
    authState: AuthState,
    onVehicleSelected: (VehiclePreset) -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    // Determine start destination based on auth state
    val startDestination = when (authState) {
        is AuthState.Authenticated -> AppRoutes.MAP
        AuthState.Unauthenticated -> AppRoutes.LOGIN
        else -> AppRoutes.SPLASH
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // SPLASH SCREEN
        // Shows once on app launch, then navigates based on auth state
        composable(
            route = AppRoutes.SPLASH,
            exitTransition = {
                fadeOut(animationSpec = tween(500))
            }
        ) {
            SplashScreen(
                onSplashComplete = {
                    // Navigate to LOGIN or MAP based on auth state
                    val destination = when (authState) {
                        is AuthState.Authenticated -> AppRoutes.MAP
                        else -> AppRoutes.LOGIN
                    }
                    navController.navigate(destination) {
                        // Remove splash from back stack so back button doesn't return to it
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // LOGIN SCREEN
        // Entry point for unauthenticated users
        // Fade in/out for smooth entry
        composable(
            route = AppRoutes.LOGIN,
            enterTransition = {
                fadeIn(animationSpec = tween(500))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(AppRoutes.SIGNUP)
                },
                onNavigateToPhoneAuth = {
                    navController.navigate(AppRoutes.PHONE_AUTH)
                },
                onLoginSuccess = {
                    navController.navigate(AppRoutes.MAP) {
                        // Clear auth screens from back stack
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // SIGN UP SCREEN
        // Modal-style slide up from bottom
        composable(
            route = AppRoutes.SIGNUP,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(AppRoutes.MAP) {
                        // Clear auth screens from back stack
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // PHONE AUTH SCREEN
        // Standard forward navigation slide from right
        composable(
            route = AppRoutes.PHONE_AUTH,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            PhoneAuthScreen(
                onNavigateBack = { navController.popBackStack() },
                onAuthSuccess = {
                    navController.navigate(AppRoutes.MAP) {
                        // Clear auth screens from back stack
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
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

