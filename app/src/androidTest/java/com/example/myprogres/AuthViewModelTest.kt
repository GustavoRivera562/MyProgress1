package com.example.myprogres

import com.example.myprogres.domain.model.AuthFailure
import com.example.myprogres.domain.model.AuthFailureReason
import com.example.myprogres.domain.model.AuthState
import com.example.myprogres.domain.model.AuthUser
import com.example.myprogres.domain.repository.AuthRepository
import com.example.myprogres.feature.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AuthViewModelTest {
    @Test
    fun registrationAndSignOut_updateTheObservedSession() = runBlocking {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.register("member@example.com", "Test-password-123")
        val registered = withTimeout(5_000) {
            viewModel.uiState.first {
                it.authState is AuthState.SignedIn &&
                    it.message == "Cuenta creada." &&
                    !it.working
            }
        }
        assertEquals("member@example.com", (registered.authState as AuthState.SignedIn).user.email)
        assertFalse(registered.message!!.contains("Test-password-123"))

        viewModel.signOut()
        val signedOut = withTimeout(5_000) {
            viewModel.uiState.first {
                it.authState == AuthState.SignedOut &&
                    it.message == "Sesión cerrada." &&
                    !it.working
            }
        }
        assertEquals(AuthState.SignedOut, signedOut.authState)
    }

    @Test
    fun invalidCredentials_arePresentedWithoutBackendDetails() = runBlocking {
        val repository = FakeAuthRepository().apply {
            nextFailure = AuthFailure(AuthFailureReason.INVALID_CREDENTIALS)
        }
        val viewModel = AuthViewModel(repository)

        viewModel.signIn("member@example.com", "wrong-password")
        val result = withTimeout(5_000) {
            viewModel.uiState.first { !it.working && it.messageIsError }
        }

        assertEquals(
            "No se pudo validar el acceso. Revisa los datos e inténtalo de nuevo.",
            result.message
        )
        assertFalse(result.message!!.contains("wrong-password"))
        assertFalse(result.message!!.contains("member@example.com"))
    }

    private class FakeAuthRepository : AuthRepository {
        private val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
        var nextFailure: AuthFailure? = null

        override fun observeAuthState() = state

        override suspend fun register(email: String, password: String) {
            checkFailure()
            state.value = AuthState.SignedIn(AuthUser("uid-test", email))
        }

        override suspend fun signIn(email: String, password: String) {
            checkFailure()
            state.value = AuthState.SignedIn(AuthUser("uid-test", email))
        }

        override suspend fun sendPasswordReset(email: String) {
            checkFailure()
        }

        override suspend fun signOut() {
            checkFailure()
            state.value = AuthState.SignedOut
        }

        private fun checkFailure() {
            nextFailure?.let {
                nextFailure = null
                throw it
            }
        }
    }
}
