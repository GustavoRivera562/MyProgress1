package com.example.myprogres.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprogres.domain.model.AuthFailure
import com.example.myprogres.domain.model.AuthFailureReason
import com.example.myprogres.domain.model.AuthState
import com.example.myprogres.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val authState: AuthState = AuthState.Initializing,
    val working: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAuthState()
                .catch {
                    _uiState.update {
                        it.copy(
                            authState = AuthState.SignedOut,
                            message = "No se pudo comprobar la sesión. Revisa tu conexión e inténtalo de nuevo.",
                            messageIsError = true
                        )
                    }
                }
                .collect { authState ->
                    _uiState.update { it.copy(authState = authState) }
                }
        }
    }

    fun register(email: String, password: String) = perform {
        repository.register(email.trim(), password)
        "Cuenta creada."
    }

    fun signIn(email: String, password: String) = perform {
        repository.signIn(email.trim(), password)
        "Sesión iniciada."
    }

    fun sendPasswordReset(email: String) = perform {
        repository.sendPasswordReset(email.trim())
        "Si el correo corresponde a una cuenta, recibirás instrucciones para recuperar el acceso."
    }

    fun signOut() = perform {
        repository.signOut()
        "Sesión cerrada."
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    private fun perform(action: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update { it.copy(working = true, message = null, messageIsError = false) }
            try {
                val message = action()
                _uiState.update { it.copy(message = message, messageIsError = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: AuthFailure) {
                _uiState.update {
                    it.copy(
                        message = failure.reason.toSafeMessage(),
                        messageIsError = true
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        message = "No se pudo completar la solicitud. Inténtalo de nuevo.",
                        messageIsError = true
                    )
                }
            } finally {
                _uiState.update { it.copy(working = false) }
            }
        }
    }
}

private fun AuthFailureReason.toSafeMessage(): String = when (this) {
    AuthFailureReason.INVALID_EMAIL -> "Revisa el formato del correo."
    AuthFailureReason.INVALID_CREDENTIALS ->
        "No se pudo validar el acceso. Revisa los datos e inténtalo de nuevo."
    AuthFailureReason.EMAIL_ALREADY_IN_USE ->
        "No se pudo crear la cuenta con esos datos. Prueba iniciar sesión o usa otro correo."
    AuthFailureReason.WEAK_PASSWORD ->
        "La contraseña no cumple los requisitos configurados."
    AuthFailureReason.NETWORK ->
        "No hay conexión. Para iniciar sesión o crear una cuenta necesitas Internet."
    AuthFailureReason.TOO_MANY_REQUESTS ->
        "Se hicieron demasiados intentos. Espera un momento y vuelve a intentarlo."
    AuthFailureReason.UNKNOWN ->
        "No se pudo completar la solicitud. Inténtalo de nuevo."
}
