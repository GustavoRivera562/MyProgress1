package com.example.myprogres.domain.model

data class ExerciseOption(
    val id: String,
    val name: String,
    val muscleGroup: String
)

object ExerciseLibrary {
    val exercises = listOf(
        ExerciseOption("squat", "Sentadilla", "Piernas"),
        ExerciseOption("deadlift", "Peso muerto", "Espalda y piernas"),
        ExerciseOption("bench_press", "Press banca", "Pecho"),
        ExerciseOption("overhead_press", "Press militar", "Hombros"),
        ExerciseOption("barbell_row", "Remo con barra", "Espalda"),
        ExerciseOption("lat_pulldown", "Jalón al pecho", "Espalda"),
        ExerciseOption("bicep_curl", "Curl de bíceps", "Brazos"),
        ExerciseOption("triceps_pushdown", "Extensión de tríceps", "Brazos")
    )
}

data class Workout(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?
)

data class WorkoutSet(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val repetitions: Int,
    val recordedAt: Long
)

data class ActiveWorkout(
    val workout: Workout,
    val sets: List<WorkoutSet>
)

data class WorkoutSummary(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val setCount: Int,
    val totalVolumeKg: Double
)

data class DiagnosticSnapshot(
    val pendingSyncCount: Int,
    val recentErrors: List<DiagnosticError>,
    val lastSuccessfulSyncAt: Long? = null
)

data class DiagnosticError(
    val code: String,
    val module: String,
    val safeMessage: String,
    val createdAt: Long
)
