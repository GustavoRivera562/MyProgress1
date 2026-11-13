package com.example.myprogres.domain.validation

import com.example.myprogres.core.error.ValidationException
import com.example.myprogres.domain.model.ExerciseLibrary

object ProgressInputValidator {
    fun validateSet(
        exerciseId: String,
        exerciseName: String,
        weightKg: Double,
        repetitions: Int
    ) {
        val name = exerciseName.trim()
        val exercise = ExerciseLibrary.exercises.firstOrNull { it.id == exerciseId }
        if (
            name.isEmpty() ||
            name.length > 60 ||
            name.any(Char::isISOControl) ||
            exercise == null ||
            exercise.name != name
        ) {
            throw ValidationException("Selecciona un ejercicio válido.")
        }
        if (!weightKg.isFinite() || weightKg !in 0.0..1_000.0) {
            throw ValidationException("El peso debe estar entre 0 y 1.000 kg.")
        }
        if (repetitions !in 1..1_000) {
            throw ValidationException("Las repeticiones deben ser un número entero entre 1 y 1.000.")
        }
    }

    fun validateWater(amountMl: Int) {
        if (amountMl !in 1..2_000) {
            throw ValidationException("Registra una cantidad entre 1 y 2.000 ml.")
        }
    }
}
