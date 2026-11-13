package com.example.myprogres.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myprogres.domain.model.AuthState
import com.example.myprogres.feature.auth.AuthViewModel
import com.example.myprogres.feature.progress.ProgressUiState
import com.example.myprogres.feature.progress.ProgressViewModel
import com.example.myprogres.ui.screens.AuthScreen
import com.example.myprogres.ui.screens.DiagnosticsScreen
import com.example.myprogres.ui.screens.HomeScreen
import com.example.myprogres.ui.screens.ProfileScreen
import com.example.myprogres.ui.screens.ProgressScreen
import com.example.myprogres.ui.screens.TrainingScreen
import com.example.myprogres.ui.screens.WaterScreen

private enum class Destination(val label: String, val glyph: String) {
    HOME("Inicio", "⌂"),
    TRAINING("Entrenar", "＋"),
    PROGRESS("Progreso", "↗"),
    WATER("Agua", "◉"),
    PROFILE("Perfil", "○"),
    DIAGNOSTICS("", "");

    companion object {
        val main = listOf(HOME, TRAINING, PROGRESS, WATER, PROFILE)
    }
}

@Composable
fun ProgressApp(
    authViewModel: AuthViewModel,
    progressViewModel: ProgressViewModel
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val progressState by progressViewModel.uiState.collectAsStateWithLifecycle()
    var legacyPromptDismissedForUid by rememberSaveable { mutableStateOf<String?>(null) }

    when (val auth = authUiState.authState) {
        AuthState.Initializing -> LoadingScreen("Comprobando tu sesión…")
        AuthState.SignedOut -> AuthScreen(
            state = authUiState,
            onSignIn = authViewModel::signIn,
            onRegister = authViewModel::register,
            onSendPasswordReset = authViewModel::sendPasswordReset,
            onDismissMessage = authViewModel::dismissMessage
        )
        is AuthState.SignedIn -> {
            if (progressState.loading) {
                LoadingScreen("Preparando tus datos locales…")
            } else if (
                progressState.unownedRecordCount > 0 &&
                legacyPromptDismissedForUid != auth.user.uid
            ) {
                LegacyDataClaimScreen(
                    email = auth.user.email,
                    recordCount = progressState.unownedRecordCount,
                    working = progressState.working,
                    message = progressState.message,
                    isError = progressState.messageIsError,
                    onClaim = progressViewModel::claimUnownedRecords,
                    onKeepHidden = { legacyPromptDismissedForUid = auth.user.uid },
                    onDismissMessage = progressViewModel::dismissMessage
                )
            } else {
                AuthenticatedProgressApp(
                    state = progressState,
                    email = auth.user.email,
                    sessionRestored = auth.restoredFromPreviousRun,
                    authMessage = authUiState.message,
                    onDismissAuthMessage = authViewModel::dismissMessage,
                    progressViewModel = progressViewModel,
                    onSignOut = authViewModel::signOut
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegacyDataClaimScreen(
    email: String,
    recordCount: Int,
    working: Boolean,
    message: String?,
    isError: Boolean,
    onClaim: () -> Unit,
    onKeepHidden: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "DATOS LOCALES  /  REVISIÓN",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Registros anteriores",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Encontramos $recordCount registros creados antes de iniciar sesión. Están ocultos hasta que confirmes si pertenecen a $email.",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Si confirmas, se asignarán a esta cuenta en una única operación local. La decisión no enviará datos a Firebase.",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        message?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 12.dp),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        Button(
            onClick = onClaim,
            enabled = !working,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            if (working) CircularProgressIndicator()
            else Text("CONFIRMAR Y ASIGNAR A ESTA CUENTA")
        }
        OutlinedButton(
            onClick = {
                onDismissMessage()
                onKeepHidden()
            },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("MANTENER OCULTOS Y CONTINUAR") }
    }
}

@Composable
private fun AuthenticatedProgressApp(
    state: ProgressUiState,
    email: String,
    sessionRestored: Boolean,
    authMessage: String?,
    onDismissAuthMessage: () -> Unit,
    progressViewModel: ProgressViewModel,
    onSignOut: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    val currentDestination = Destination.valueOf(destination)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            progressViewModel.dismissMessage()
        }
    }
    LaunchedEffect(authMessage) {
        authMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onDismissAuthMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentDestination != Destination.DIAGNOSTICS) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    Destination.main.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination == item,
                            onClick = { destination = item.name },
                            icon = {
                                Text(
                                    item.glyph,
                                    modifier = Modifier.clearAndSetSemantics { },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues)
        ) {
            when (currentDestination) {
                Destination.HOME -> HomeScreen(
                    state = state,
                    onStartTraining = { destination = Destination.TRAINING.name },
                    onOpenWater = { destination = Destination.WATER.name }
                )
                Destination.TRAINING -> TrainingScreen(
                    state = state,
                    onStartWorkout = progressViewModel::startWorkout,
                    onFinishWorkout = progressViewModel::finishWorkout,
                    onAddSet = progressViewModel::addSet
                )
                Destination.PROGRESS -> ProgressScreen(state = state)
                Destination.WATER -> WaterScreen(state = state, onAddWater = progressViewModel::addWater)
                Destination.PROFILE -> ProfileScreen(
                    state = state,
                    email = email,
                    sessionRestored = sessionRestored,
                    onOpenDiagnostics = { destination = Destination.DIAGNOSTICS.name },
                    onSignOut = onSignOut
                )
                Destination.DIAGNOSTICS -> DiagnosticsScreen(
                    state = state,
                    onBack = { destination = Destination.PROFILE.name }
                )
            }
        }
    }
}
