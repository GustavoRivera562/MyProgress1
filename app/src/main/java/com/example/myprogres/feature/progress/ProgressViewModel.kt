package com.example.myprogres.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myprogres.core.error.AppResult
import com.example.myprogres.core.error.ErrorManager
import com.example.myprogres.data.sync.SyncCoordinator
import com.example.myprogres.domain.model.ActiveWorkout
import com.example.myprogres.domain.model.AuthState
import com.example.myprogres.domain.model.DiagnosticSnapshot
import com.example.myprogres.domain.model.WorkoutSummary
import com.example.myprogres.domain.repository.AuthRepository
import com.example.myprogres.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val activeWorkout: ActiveWorkout? = null,
    val history: List<WorkoutSummary> = emptyList(),
    val todayWaterMl: Int = 0,
    val diagnostics: DiagnosticSnapshot = DiagnosticSnapshot(0, emptyList()),
    val unownedRecordCount: Int = 0,
    val loading: Boolean = true,
    val working: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: ProgressRepository,
    private val errorManager: ErrorManager,
    private val syncCoordinator: SyncCoordinator,
    authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    private var ownerUid: String? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthState()
                .distinctUntilChanged()
                .collectLatest { authState ->
                    val uid = (authState as? AuthState.SignedIn)?.user?.uid
                    ownerUid = uid
                    if (uid == null) {
                        _uiState.value = ProgressUiState(loading = false)
                    } else {
                        requestSync()
                        observeUserData(uid)
                    }
                }
        }
    }

    private suspend fun observeUserData(uid: String) {
        _uiState.update { it.copy(loading = true) }
        combine(
            repository.observeActiveWorkout(uid),
            repository.observeHistory(uid),
            repository.observeTodayWater(uid),
            repository.observeDiagnostics(uid),
            repository.observeUnownedRecordCount()
        ) { active, history, water, diagnostics, unownedCount ->
            UserProgressSnapshot(active, history, water, diagnostics, unownedCount)
        }
            .catch { handleStreamError("DATABASE", "READ_USER_DATA", it) }
            .collect { snapshot ->
                _uiState.update {
                    it.copy(
                        activeWorkout = snapshot.activeWorkout,
                        history = snapshot.history,
                        todayWaterMl = snapshot.todayWaterMl,
                        diagnostics = snapshot.diagnostics,
                        unownedRecordCount = snapshot.unownedRecordCount,
                        loading = false
                    )
                }
            }
    }

    fun startWorkout() = perform("TRAINING", "START_SESSION", "Sesión iniciada.") { uid ->
        repository.startWorkout(uid)
    }

    fun finishWorkout(workoutId: String) =
        perform("TRAINING", "FINISH_SESSION", "Entrenamiento guardado.") { uid ->
            repository.finishWorkout(uid, workoutId)
        }

    fun addSet(
        workoutId: String,
        exerciseId: String,
        exerciseName: String,
        weightText: String,
        repetitionsText: String
    ) {
        val weight = weightText.trim().replace(',', '.').toDoubleOrNull()
        val repetitions = repetitionsText.trim().toIntOrNull()
        val validationMessage = when {
            weight == null || !weight.isFinite() || weight !in 0.0..1_000.0 ->
                "El peso debe estar entre 0 y 1.000 kg."
            repetitions == null || repetitions !in 1..1_000 ->
                "Las repeticiones deben ser un entero entre 1 y 1.000."
            else -> null
        }
        if (validationMessage != null) {
            showMessage(validationMessage, isError = true)
            return
        }
        perform("TRAINING", "SAVE_SET", "Serie guardada en este dispositivo.") { uid ->
            repository.addSet(
                ownerUid = uid,
                workoutId = workoutId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                weightKg = weight!!,
                repetitions = repetitions!!
            )
        }
    }

    fun addWater(amountText: String) {
        val amount = amountText.trim().toIntOrNull()
        if (amount == null || amount !in 1..2_000) {
            showMessage("Ingresa una cantidad entre 1 y 2.000 ml.", isError = true)
            return
        }
        perform("WATER", "SAVE_INTAKE", "$amount ml guardados.") { uid ->
            repository.addWater(uid, amount)
        }
    }

    fun claimUnownedRecords() {
        perform("ACCOUNT", "CLAIM_LEGACY_LOCAL_DATA", "Los registros anteriores se asignaron a esta cuenta.") { uid ->
            repository.claimUnownedRecords(uid)
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    private fun showMessage(message: String, isError: Boolean) {
        _uiState.update { it.copy(message = message, messageIsError = isError) }
    }

    private fun perform(
        module: String,
        operation: String,
        successMessage: String,
        action: suspend (ownerUid: String) -> Any?
    ) {
        val uid = ownerUid
        if (uid.isNullOrBlank()) {
            showMessage("Inicia sesión para continuar.", isError = true)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(working = true, message = null) }
            when (val result = errorManager.execute(module, operation) { action(uid) }) {
                is AppResult.Success -> {
                    showMessage(successMessage, isError = false)
                    requestSync()
                }
                is AppResult.Failure -> showMessage(result.error.safeMessage, isError = true)
            }
            _uiState.update { it.copy(working = false) }
        }
    }

    private fun requestSync() {
        viewModelScope.launch {
            errorManager.execute("SYNC", "UPLOAD_PENDING_PROGRESS") {
                syncCoordinator.syncPendingForCurrentUser()
            }
        }
    }

    private suspend fun handleStreamError(module: String, operation: String, cause: Throwable) {
        val exception = cause as? Exception ?: IllegalStateException()
        val failure = errorManager.execute<Unit>(module, operation) {
            throw exception
        }
        if (failure is AppResult.Failure) {
            _uiState.update {
                it.copy(
                    loading = false,
                    message = failure.error.safeMessage,
                    messageIsError = true
                )
            }
        }
    }

    private data class UserProgressSnapshot(
        val activeWorkout: ActiveWorkout?,
        val history: List<WorkoutSummary>,
        val todayWaterMl: Int,
        val diagnostics: DiagnosticSnapshot,
        val unownedRecordCount: Int
    )
}
