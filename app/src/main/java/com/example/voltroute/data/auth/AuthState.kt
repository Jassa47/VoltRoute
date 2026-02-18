package com.example.voltroute.data.auth

import com.google.firebase.auth.FirebaseUser

/**
 * AuthState - Sealed class representing authentication states
 *
 * Used throughout the app to represent the current authentication status.
 * Observed by UI components to show appropriate screens:
 * - Loading: Initial state or auth operation in progress
 * - Unauthenticated: No user logged in (show login screen)
 * - Authenticated: User logged in (show main app)
 * - Error: Auth operation failed (show error message)
 *
 * This pattern provides type-safe state management and makes it
 * easy to handle all possible auth states in the UI with when expressions.
 */
sealed class AuthState {

    /**
     * Loading state - Auth operation in progress
     *
     * Used when:
     * - Checking initial auth state on app start
     * - Sign in/sign up operation in progress
     * - Waiting for Firebase response
     *
     * UI should show loading indicator
     */
    object Loading : AuthState()

    /**
     * Unauthenticated state - No user logged in
     *
     * Used when:
     * - User has signed out
     * - No previous session exists
     * - Session expired/invalidated
     *
     * UI should show login/signup screens
     */
    object Unauthenticated : AuthState()

    /**
     * Authenticated state - User successfully logged in
     *
     * Contains user information from FirebaseUser:
     * - userId: Unique Firebase user ID
     * - email: User's email (null for phone auth)
     * - phoneNumber: User's phone (null for email auth)
     * - isEmailVerified: Whether email is verified
     *
     * UI should show main app content
     *
     * @param user The Firebase user object
     * @param userId Unique user ID (defaults to user.uid)
     * @param email User's email address (defaults to user.email)
     * @param phoneNumber User's phone number (defaults to user.phoneNumber)
     * @param isEmailVerified Email verification status (defaults to user.isEmailVerified)
     */
    data class Authenticated(
        val user: FirebaseUser,
        val userId: String = user.uid,
        val email: String? = user.email,
        val phoneNumber: String? = user.phoneNumber,
        val isEmailVerified: Boolean = user.isEmailVerified
    ) : AuthState()

    /**
     * Error state - Auth operation failed
     *
     * Used when:
     * - Invalid credentials
     * - Network error
     * - Firebase service unavailable
     * - Other auth-related errors
     *
     * UI should show error message to user
     *
     * @param message Human-readable error message
     */
    data class Error(val message: String) : AuthState()
}

