package com.example.voltroute.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voltroute.data.auth.AuthRepository
import com.example.voltroute.data.auth.AuthState
import com.example.voltroute.data.auth.PhoneAuthResult
import com.example.voltroute.domain.usecase.CreateUserDocumentUseCase
import com.example.voltroute.domain.usecase.SignInWithEmailUseCase
import com.example.voltroute.domain.usecase.SignOutUseCase
import com.example.voltroute.domain.usecase.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuthUiState - UI state for authentication screens
 *
 * Separate from AuthState (which represents auth status).
 * This represents UI-specific state like loading, errors, messages.
 *
 * @param isLoading Whether an auth operation is in progress
 * @param error Error message to display (null if no error)
 * @param successMessage Success message to display (null if none)
 * @param phoneVerificationId ID from Firebase for phone verification
 * @param isCodeSent Whether SMS code has been sent
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val phoneVerificationId: String? = null,
    val isCodeSent: Boolean = false
)

/**
 * AuthViewModel - ViewModel for authentication screens
 *
 * Manages authentication operations and UI state:
 * - Email/Password sign in and sign up
 * - Phone number authentication
 * - Firestore user document creation
 * - Auth state observation
 * - Error and success message handling
 *
 * Two StateFlows:
 * 1. authState: Firebase auth status (from repository)
 * 2. uiState: UI-specific state (loading, errors, messages)
 *
 * UI screens observe both flows:
 * - authState → Navigate when Authenticated
 * - uiState → Show loading, errors, success messages
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val createUserDocumentUseCase: CreateUserDocumentUseCase
) : ViewModel() {

    /**
     * Auth state from repository
     *
     * Converted to StateFlow using stateIn() for:
     * - Immediate value (initialValue = Loading)
     * - Survives configuration changes
     * - 5 second buffer for rotation handling
     *
     * UI observes this to navigate between screens:
     * - Loading → Show splash/loading screen
     * - Unauthenticated → Show login screen
     * - Authenticated → Show main app
     * - Error → Show error message
     */
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )

    /**
     * UI state for auth screens
     *
     * Mutable internally, exposed as read-only StateFlow.
     * Updated by auth operations to show:
     * - Loading indicators
     * - Error messages
     * - Success messages
     * - Phone verification state
     */
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Sign in with email and password
     *
     * Process:
     * 1. Set loading state
     * 2. Call use case (validates + calls repository)
     * 3. On success: Create/update Firestore document, show welcome message
     * 4. On failure: Show error message
     *
     * AuthState will automatically update to Authenticated on success,
     * triggering navigation in UI.
     *
     * Firestore document creation:
     * - Creates user document if doesn't exist
     * - Updates lastSyncedAt if exists (using merge)
     * - Safe to call on every login
     *
     * @param email User's email address
     * @param password User's password
     */
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signInWithEmailUseCase(email, password)
                .onSuccess { firebaseUser ->
                    // Create/update Firestore document (safe with merge)
                    createUserDocumentUseCase(firebaseUser)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Welcome back!"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Sign in failed"
                        )
                    }
                }
        }
    }

    /**
     * Sign up (create account) with email and password
     *
     * Process:
     * 1. Set loading state
     * 2. Call use case (validates passwords match + calls repository)
     * 3. On success: Create Firestore document, show success message
     * 4. On failure: Show error message
     *
     * Email verification is sent automatically by repository.
     * User can sign in even without verification, but some features
     * may be restricted.
     *
     * Firestore document creation:
     * - Creates user document with default settings
     * - If Firestore fails, auth still succeeds (logged but not blocking)
     * - User can still use app, document will be created on next login
     *
     * @param email User's email address
     * @param password User's password
     * @param confirmPassword Password confirmation (must match)
     */
    fun signUpWithEmail(
        email: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signUpWithEmailUseCase(email, password, confirmPassword)
                .onSuccess { firebaseUser ->
                    // Create Firestore document for new user
                    createUserDocumentUseCase(firebaseUser)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Account created! Please verify your email."
                                )
                            }
                        }
                        .onFailure { firestoreError ->
                            // Auth succeeded but Firestore failed
                            // User can still use app, just log the error
                            android.util.Log.e(
                                "AuthViewModel",
                                "Failed to create Firestore document: ${firestoreError.message}"
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Account created!"
                                )
                            }
                        }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Sign up failed"
                        )
                    }
                }
        }
    }

    /**
     * Send phone verification code via SMS
     *
     * Process:
     * 1. Set loading state
     * 2. Call repository (returns Flow of PhoneAuthResult)
     * 3. Collect results:
     *    - CodeSent: Save verificationId, show success
     *    - AutoVerified: Show success (rare - instant verification)
     *    - Failed: Show error
     *
     * After CodeSent, UI should show code input field.
     * Call verifyPhoneCode() when user enters code.
     *
     * @param phoneNumber Phone in E.164 format (e.g., "+14155552671")
     * @param activity Android Activity for SMS auto-retrieval
     */
    fun sendPhoneCode(phoneNumber: String, activity: android.app.Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.sendPhoneVerificationCode(phoneNumber, activity)
                .collect { result ->
                    when (result) {
                        is PhoneAuthResult.CodeSent -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    phoneVerificationId = result.verificationId,
                                    isCodeSent = true,
                                    successMessage = "Code sent to $phoneNumber"
                                )
                            }
                        }
                        is PhoneAuthResult.AutoVerified -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = "Phone verified!"
                                )
                            }
                        }
                        is PhoneAuthResult.Failed -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                        }
                    }
                }
        }
    }

    /**
     * Verify phone code entered by user
     *
     * Process:
     * 1. Check if verificationId exists (from sendPhoneCode)
     * 2. Set loading state
     * 3. Call repository to verify code
     * 4. On success: Create Firestore document, show success message
     * 5. On failure: Show error message
     *
     * Firestore document creation:
     * - Creates user document for phone auth users
     * - Email will be null (phone-only account)
     * - Uses same default settings as email signup
     *
     * @param code 6-digit SMS code entered by user
     */
    fun verifyPhoneCode(code: String) {
        val verificationId = _uiState.value.phoneVerificationId
        if (verificationId == null) {
            _uiState.update { it.copy(error = "No verification ID found") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.verifyPhoneCode(verificationId, code)
                .onSuccess { firebaseUser ->
                    // Create Firestore document for phone user
                    createUserDocumentUseCase(firebaseUser)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Phone verified successfully!"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Verification failed"
                        )
                    }
                }
        }
    }

    /**
     * Sign out current user
     *
     * Synchronous operation (no loading state needed).
     * AuthState will automatically update to Unauthenticated,
     * triggering navigation to login screen.
     */
    fun signOut() {
        signOutUseCase()
    }

    /**
     * Clear error message
     *
     * Called after error is shown to user (e.g., in Snackbar).
     * Prevents error from showing again on recomposition.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message
     *
     * Called after success message is shown to user.
     * Prevents message from showing again on recomposition.
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

