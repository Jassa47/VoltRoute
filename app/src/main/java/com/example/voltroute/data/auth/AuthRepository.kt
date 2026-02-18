package com.example.voltroute.data.auth

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class PhoneAuthResult {
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken
    ) : PhoneAuthResult()

    data class AutoVerified(
        val credential: PhoneAuthCredential
    ) : PhoneAuthResult()

    data class Failed(val message: String) : PhoneAuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    val authState: Flow<AuthState> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
        trySend(
            if (currentUser != null)
                AuthState.Authenticated(currentUser)
            else
                AuthState.Unauthenticated
        )

        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(
                if (user != null)
                    AuthState.Authenticated(user)
                else
                    AuthState.Unauthenticated
            )
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = try {
        val result = firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = try {
        val result = firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .await()

        result.user?.sendEmailVerification()?.await()

        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: android.app.Activity
    ): Flow<PhoneAuthResult> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                trySend(PhoneAuthResult.AutoVerified(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(PhoneAuthResult.Failed(e.message ?: "Verification failed"))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                trySend(PhoneAuthResult.CodeSent(verificationId, token))
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        awaitClose { }
    }

    suspend fun verifyPhoneCode(
        verificationId: String,
        code: String
    ): Result<FirebaseUser> = try {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        val result = firebaseAuth.signInWithCredential(credential).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

