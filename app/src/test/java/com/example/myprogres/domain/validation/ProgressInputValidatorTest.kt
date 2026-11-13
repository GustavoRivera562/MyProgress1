package com.example.myprogres.domain.validation

import com.example.myprogres.core.error.ValidationException
import com.example.myprogres.domain.model.ExerciseLibrary
import com.example.myprogres.testing.TestErrorLogRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class ProgressInputValidatorTest {
    @get:Rule
    val testErrorLogRule = TestErrorLogRule()

    @Test
    fun validSet_isAccepted() {
        val exercise = ExerciseLibrary.exercises.first()

        ProgressInputValidator.validateSet(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            weightKg = 85.5,
            repetitions = 8
        )
    }

    @Test
    fun negativeWeight_isRejected() {
        val exercise = ExerciseLibrary.exercises.first()

        val error = assertThrows(ValidationException::class.java) {
            ProgressInputValidator.validateSet(exercise.id, exercise.name, -1.0, 8)
        }

        assertEquals("El peso debe estar entre 0 y 1.000 kg.", error.message)
    }

    @Test
    fun nonFiniteWeight_isRejected() {
        val exercise = ExerciseLibrary.exercises.first()

        assertThrows(ValidationException::class.java) {
            ProgressInputValidator.validateSet(exercise.id, exercise.name, Double.NaN, 8)
        }
    }

    @Test
    fun zeroRepetitions_isRejected() {
        val exercise = ExerciseLibrary.exercises.first()

        assertThrows(ValidationException::class.java) {
            ProgressInputValidator.validateSet(exercise.id, exercise.name, 20.0, 0)
        }
    }

    @Test
    fun exerciseOutsideCatalog_isRejected() {
        assertThrows(ValidationException::class.java) {
            ProgressInputValidator.validateSet("custom", "Ejercicio externo", 20.0, 8)
        }
    }

    @Test
    fun waterAmountOutsideAllowedRange_isRejected() {
        val error = assertThrows(ValidationException::class.java) {
            ProgressInputValidator.validateWater(0)
        }

        assertEquals("Registra una cantidad entre 1 y 2.000 ml.", error.message)
    }

    @Test
    fun validWaterAmount_isAccepted() {
        ProgressInputValidator.validateWater(500)
    }
}
