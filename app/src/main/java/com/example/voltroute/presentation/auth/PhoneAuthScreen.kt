package com.example.voltroute.presentation.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voltroute.R
import com.example.voltroute.data.auth.AuthState

/**
 * Extension function to find Activity from Context
 *
 * Firebase Phone Auth requires Activity context for SMS auto-retrieval.
 * This function walks up the context chain to find the Activity.
 *
 * @throws IllegalStateException if no Activity is found
 */
fun Context.findActivity(): android.app.Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("No activity found")
}

/**
 * PhoneAuthScreen - Phone number authentication screen
 *
 * Two-step phone authentication flow:
 *
 * STEP 1: Phone Number Entry
 * - User enters phone number with country code
 * - Tap "Send Code" to receive SMS
 * - Firebase sends 6-digit verification code
 *
 * STEP 2: Code Verification
 * - User enters 6-digit code from SMS
 * - Tap "Verify Code" to complete auth
 * - Can go back to use different number
 *
 * UI automatically switches between steps based on uiState.isCodeSent.
 *
 * Features:
 * - Back button to return to login
 * - Loading states in buttons
 * - Error messages via Snackbar
 * - Auto-navigation on success
 * - "Use different number" to restart flow
 *
 * Technical requirements:
 * - Requires Activity context for Firebase SMS retrieval
 * - Phone number must include country code (e.g., +14155552671)
 * - Verification code is always 6 digits
 *
 * @param onNavigateBack Navigate back to login screen
 * @param onAuthSuccess Navigate to main app after auth
 * @param viewModel AuthViewModel (injected by Hilt)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onNavigateBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // State from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Local UI state
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    // Get Activity context for Firebase Phone Auth
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    // Snackbar for error messages
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * LaunchedEffect: Navigate on successful authentication
     *
     * Key: authState - Re-runs when auth state changes
     *
     * When phone number is verified and user is authenticated:
     * - Navigate to main app
     * - Clear auth back stack
     */
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    /**
     * LaunchedEffect: Show error messages
     *
     * Key: uiState.error - Re-runs when error changes
     *
     * Shows errors like:
     * - Invalid phone number format
     * - SMS sending failed
     * - Invalid verification code
     * - Network errors
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
        topBar = {
            TopAppBar(
                title = { Text("Phone Sign In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
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
                /**
                 * Conditional UI based on flow step
                 *
                 * STEP 1: !uiState.isCodeSent
                 * - Show phone number entry
                 * - "Send Code" button
                 *
                 * STEP 2: uiState.isCodeSent
                 * - Show code entry
                 * - "Verify Code" button
                 * - "Use different number" to go back
                 */
                if (!uiState.isCodeSent) {
                    // STEP 1: Phone Number Entry

                    // VoltRoute Logo (PNG)
                    Image(
                        painter = painterResource(id = R.drawable.voltroutelogo),
                        contentDescription = "VoltRoute Logo",
                        modifier = Modifier.size(200.dp)
                    )

                    // Title
                    Text(
                        text = "Enter Phone Number",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Description
                    Text(
                        text = "We'll send you a verification code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone number field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        },
                        placeholder = { Text("+1 234 567 8900", color = Color.White.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.sendPhoneCode(phoneNumber, activity)
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Include country code (e.g., +1)", color = Color.White.copy(alpha = 0.7f)) },
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

                    // Send Code button
                    Button(
                        onClick = {
                            viewModel.sendPhoneCode(phoneNumber, activity)
                        },
                        enabled = !uiState.isLoading && phoneNumber.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send Code")
                        }
                    }
                } else {
                    // STEP 2: Code Verification

                    // SMS icon
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    // Title
                    Text(
                        text = "Enter Verification Code",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Description with phone number
                    Text(
                        text = "Code sent to $phoneNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verification code field
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = { Text("Verification Code", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = Color.White)
                        },
                        placeholder = { Text("123456", color = Color.White.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.verifyPhoneCode(verificationCode) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("6-digit code from SMS", color = Color.White.copy(alpha = 0.7f)) },
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

                    // Verify Code button
                    Button(
                        onClick = { viewModel.verifyPhoneCode(verificationCode) },
                        enabled = !uiState.isLoading && verificationCode.length == 6,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Verify Code")
                        }
                    }

                    // Use different number button
                    TextButton(
                        onClick = {
                            // Reset flow to phone number entry
                            viewModel.clearError()
                            viewModel.clearSuccess()
                            phoneNumber = ""
                            verificationCode = ""
                        }
                    ) {
                        Text("Use different number", color = Color.White)
                    }
                }
            }
        }
    }
}

