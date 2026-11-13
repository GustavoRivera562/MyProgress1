package com.example.myprogres.data.repository

import com.example.myprogres.domain.model.AuthFailure
import com.example.myprogres.domain.model.AuthFailureReason
import com.example.myprogres.domain.model.AuthState
import com.example.myprogres.domain.model.AuthUser
import com.example.myprogres.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {
    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        var isInitialEvent = true
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val state = if (user == null) {
                AuthState.SignedOut
            } else {
                AuthState.SignedIn(
                    user = user.toDomain(),
                    restoredFromPreviousRun = isInitialEvent
                )
            }
            isInitialEvent = false
            trySend(state)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun register(email: String, password: String) {
        performAuthOperation {
            firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
        }
    }

    override suspend fun signIn(email: String, password: String) {
        performAuthOperation {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
        }
    }

    override suspend fun sendPasswordReset(email: String) {
        try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).awaitResult()
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            val reason = exception.toAuthFailureReason()
            if (reason == AuthFailureReason.NETWORK) throw AuthFailure(reason)
            // Do not reveal whether an email address is registered.
            if (reason == AuthFailureReason.INVALID_EMAIL) throw AuthFailure(reason)
            if (reason == AuthFailureReason.TOO_MANY_REQUESTS) throw AuthFailure(reason)
            if (reason == AuthFailureReason.UNKNOWN) throw AuthFailure(reason)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {
            throw AuthFailure(AuthFailureReason.UNKNOWN)
        }
    }

    private suspend fun performAuthOperation(operation: suspend () -> Unit) {
        try {
            operation()
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) throw exception
            throw AuthFailure(exception.toAuthFailureReason())
        }
    }
}

private fun FirebaseUser.toDomain() = AuthUser(
    uid = uid,
    email = email.orEmpty()
)

private fun Exception.toAuthFailureReason(): AuthFailureReason = when {
    this is FirebaseNetworkException -> AuthFailureReason.NETWORK
    this is FirebaseAuthException && errorCode.contains("TOO_MANY_REQUESTS", ignoreCase = true) ->
        AuthFailureReason.TOO_MANY_REQUESTS
    this is FirebaseAuthWeakPasswordException -> AuthFailureReason.WEAK_PASSWORD
    this is FirebaseAuthUserCollisionException -> AuthFailureReason.EMAIL_ALREADY_IN_USE
    this is FirebaseAuthInvalidUserException || this is FirebaseAuthInvalidCredentialsException ->
        AuthFailureReason.INVALID_CREDENTIALS
    this is FirebaseAuthException && errorCode.contains("INVALID_EMAIL", ignoreCase = true) ->
        AuthFailureReason.INVALID_EMAIL
    this is FirebaseAuthException && errorCode.contains("WEAK_PASSWORD", ignoreCase = true) ->
        AuthFailureReason.WEAK_PASSWORD
    this is java.io.IOException -> AuthFailureReason.NETWORK
    else -> AuthFailureReason.UNKNOWN
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException())
            }
        }
    }
