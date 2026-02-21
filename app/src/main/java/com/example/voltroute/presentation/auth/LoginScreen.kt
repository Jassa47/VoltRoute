package com.example.voltroute.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.painterResource
import com.example.voltroute.R
import com.example.voltroute.data.auth.AuthState

/**
 * LoginScreen - Email/password sign-in screen
 *
 * Entry point for authentication flow (no back button).
 * Users can:
 * - Sign in with email/password
 * - Navigate to sign up screen
 * - Navigate to phone auth screen
 *
 * Features:
 * - Email and password fields with validation
 * - Password visibility toggle
 * - Keyboard navigation (Next → Done)
 * - Loading state in button
 * - Error messages via Snackbar
 * - Auto-navigation on successful auth
 *
 * @param onNavigateToSignUp Navigate to sign-up screen
 * @param onNavigateToPhoneAuth Navigate to phone auth screen
 * @param onLoginSuccess Navigate to main app after login
 * @param viewModel AuthViewModel (injected by Hilt)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToPhoneAuth: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // State from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Local UI state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Snackbar for error messages
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * LaunchedEffect: Navigate on successful authentication
     *
     * Key: authState - Re-runs when auth state changes
     *
     * When authState becomes Authenticated:
     * - Navigate to main app
     * - This handles navigation for both email and phone auth
     *
     * Why separate from button onClick:
     * - Auth state can change from multiple sources
     * - Keeps navigation logic centralized
     * - Works for both sign-in and sign-up flows
     */
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    /**
     * LaunchedEffect: Show error messages
     *
     * Key: uiState.error - Re-runs when error changes
     *
     * When error is not null:
     * - Show Snackbar with error message
     * - Auto-dismiss after short duration
     * - Clear error from state (prevents re-showing)
     *
     * Why this pattern:
     * - Snackbar handles dismiss timing automatically
     * - clearError() prevents error persisting across recompositions
     * - User sees error once, then it's gone
     */
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // VoltRoute Logo (PNG)
                Image(
                    painter = painterResource(id = R.drawable.voltroutelogo),
                    contentDescription = "VoltRoute Logo",
                    modifier = Modifier.size(160.dp)
                )

                // Welcome text
                Text(
                    text = "Welcome back!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.White) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible)
                                    "Hide password"
                                else
                                    "Show password",
                                tint = Color.White
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signInWithEmail(email, password)
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                    )
                )

                // Sign In button
                Button(
                    onClick = { viewModel.signInWithEmail(email, password) },
                    enabled = !uiState.isLoading &&
                            email.isNotBlank() &&
                            password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Sign In")
                    }
                }

                // Divider with "OR"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Phone sign-in button
                OutlinedButton(
                    onClick = onNavigateToPhoneAuth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Phone", color = Color.White)
                }

                // Sign up link
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    TextButton(onClick = onNavigateToSignUp) {
                        Text("Sign Up")
                    }
                }
            }
        }
    }
}

