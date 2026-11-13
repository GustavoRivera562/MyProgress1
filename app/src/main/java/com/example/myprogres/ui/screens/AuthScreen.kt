package com.example.myprogres.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myprogres.feature.auth.AuthUiState

private enum class AuthFormMode { SIGN_IN, REGISTER, RESET_PASSWORD }

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onSendPasswordReset: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    var mode by remember { mutableStateOf(AuthFormMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isRegister = mode == AuthFormMode.REGISTER
    val isReset = mode == AuthFormMode.RESET_PASSWORD
    val title = when (mode) {
        AuthFormMode.SIGN_IN -> "Bienvenido"
        AuthFormMode.REGISTER -> "Crear cuenta"
        AuthFormMode.RESET_PASSWORD -> "Recuperar acceso"
    }
    val subtitle = when (mode) {
        AuthFormMode.SIGN_IN -> "Inicia sesión para continuar a MyProgres."
        AuthFormMode.REGISTER -> "Tus datos locales permanecerán en este dispositivo."
        AuthFormMode.RESET_PASSWORD -> "Te enviaremos instrucciones al correo indicado."
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "MYPROGRES  /  CUENTA",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null; onDismissMessage() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !state.working,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        if (!isReset) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localError = null; onDismissMessage() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !state.working,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
        if (isRegister) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it; localError = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Repite la contraseña") },
                singleLine = true,
                enabled = !state.working,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        Spacer(Modifier.height(14.dp))
        (localError ?: state.message)?.let { message ->
            Text(
                message,
                color = if (localError != null || state.messageIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val normalizedEmail = email.trim()
                localError = when {
                    !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                        "Ingresa un correo válido."
                    isRegister && password.length < 6 ->
                        "La contraseña debe tener al menos 6 caracteres."
                    isRegister && password != confirmation ->
                        "Las contraseñas no coinciden."
                    !isReset && password.isBlank() ->
                        "Ingresa tu contraseña."
                    else -> null
                }
                if (localError == null) {
                    when (mode) {
                        AuthFormMode.SIGN_IN -> {
                            val submittedPassword = password
                            password = ""
                            confirmation = ""
                            onSignIn(normalizedEmail, submittedPassword)
                        }
                        AuthFormMode.REGISTER -> {
                            val submittedPassword = password
                            password = ""
                            confirmation = ""
                            onRegister(normalizedEmail, submittedPassword)
                        }
                        AuthFormMode.RESET_PASSWORD -> onSendPasswordReset(normalizedEmail)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.working
        ) {
            if (state.working) CircularProgressIndicator()
            else Text(
                when (mode) {
                    AuthFormMode.SIGN_IN -> "INICIAR SESIÓN"
                    AuthFormMode.REGISTER -> "CREAR CUENTA"
                    AuthFormMode.RESET_PASSWORD -> "ENVIAR INSTRUCCIONES"
                }
            )
        }

        Spacer(Modifier.height(8.dp))
        when (mode) {
            AuthFormMode.SIGN_IN -> {
                TextButton(
                    onClick = {
                        mode = AuthFormMode.RESET_PASSWORD
                        password = ""
                        confirmation = ""
                        localError = null
                        onDismissMessage()
                    },
                    enabled = !state.working
                ) { Text("Olvidé mi contraseña") }
                TextButton(
                    onClick = {
                        mode = AuthFormMode.REGISTER
                        password = ""
                        confirmation = ""
                        localError = null
                        onDismissMessage()
                    },
                    enabled = !state.working
                ) { Text("Crear una cuenta") }
            }
            AuthFormMode.REGISTER, AuthFormMode.RESET_PASSWORD -> TextButton(
                onClick = {
                    mode = AuthFormMode.SIGN_IN
                    password = ""
                    confirmation = ""
                    localError = null
                    onDismissMessage()
                },
                enabled = !state.working
            ) { Text("Volver a iniciar sesión") }
        }
    }
}
