package com.example.myprogres.core.error

import android.database.sqlite.SQLiteException
import com.example.myprogres.data.local.dao.ErrorLogDao
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.myprogres.data.local.entity.ErrorLogEntity
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.UUID

class ErrorManager(
    private val errorLogDao: ErrorLogDao,
    private val crashReporter: CrashReporter = NoOpCrashReporter
) {
    suspend fun <T> execute(
        module: String,
        operation: String,
        action: suspend () -> T
    ): AppResult<T> {
        return try {
            AppResult.Success(action())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = when (exception) {
                is ValidationException -> AppError.Validation(exception.message.orEmpty())
                is SQLiteException -> AppError.Database
                is FirebaseNetworkException, is IOException -> AppError.Connectivity
                is FirebaseAuthException -> AppError.Authentication
                is FirebaseFirestoreException -> when (exception.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Connectivity
                    else -> AppError.Firestore
                }
                else -> AppError.Unexpected
            }
            if (error !is AppError.Validation) {
                persistSanitizedError(error, module, operation)
                runCatching { crashReporter.record(error.code) }
            }
            AppResult.Failure(error)
        }
    }

    private suspend fun persistSanitizedError(
        error: AppError,
        module: String,
        operation: String
    ) {
        try {
            errorLogDao.insert(
                ErrorLogEntity(
                    errorId = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    module = module.take(24),
                    type = error.type,
                    code = error.code,
                    severity = error.severity,
                    operation = operation.take(32),
                    safeMessage = error.safeMessage,
                    appVersion = "1.0"
                )
            )
        } catch (_: Exception) {
            // Logging must never trigger a second failure or hide the original result.
        }
    }
}
