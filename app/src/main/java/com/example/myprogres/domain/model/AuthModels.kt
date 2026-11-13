package com.example.myprogres.domain.model

data class AuthUser(
    val uid: String,
    val email: String
)

sealed interface AuthState {
    data object Initializing : AuthState
    data object SignedOut : AuthState
    data class SignedIn(
        val user: AuthUser,
        val restoredFromPreviousRun: Boolean = false
    ) : AuthState
}

enum class AuthFailureReason {
    INVALID_EMAIL,
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    NETWORK,
    TOO_MANY_REQUESTS,
    UNKNOWN
}

class AuthFailure(val reason: AuthFailureReason) : Exception()
