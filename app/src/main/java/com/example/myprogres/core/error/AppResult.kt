package com.example.myprogres.core.error

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(
    val code: String,
    val type: String,
    val severity: String,
    val safeMessage: String
) {
    class Validation(message: String) :
        AppError("INPUT_001", "VALIDATION", "LOW", message)

    data object Database :
        AppError("DB_001", "DATABASE", "HIGH", "No se pudo guardar el cambio. Tus datos guardados siguen disponibles.")

    data object Connectivity :
        AppError("NET_001", "NETWORK", "MEDIUM", "No se pudo sincronizar tu información. Tus datos siguen guardados en este dispositivo y volveremos a intentarlo cuando haya conexión.")

    data object Authentication :
        AppError("AUTH_001", "AUTHENTICATION", "MEDIUM", "No se pudo verificar la sesión. Inicia sesión nuevamente e inténtalo otra vez.")

    data object Firestore :
        AppError("FS_001", "FIRESTORE", "MEDIUM", "No se pudo sincronizar tu información. Tus datos siguen guardados en este dispositivo y volveremos a intentarlo.")

    data object Unexpected :
        AppError("APP_001", "APPLICATION", "HIGH", "Ocurrió un problema. Inténtalo nuevamente.")

    data object NotFound :
        AppError("DATA_001", "APPLICATION", "MEDIUM", "No encontramos ese registro. Actualiza la pantalla e inténtalo otra vez.")
}

class ValidationException(message: String) : Exception(message)
