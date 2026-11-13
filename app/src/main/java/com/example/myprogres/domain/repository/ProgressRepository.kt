package com.example.myprogres.domain.repository

import com.example.myprogres.domain.model.ActiveWorkout
import com.example.myprogres.domain.model.DiagnosticSnapshot
import com.example.myprogres.domain.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeActiveWorkout(ownerUid: String): Flow<ActiveWorkout?>
    fun observeHistory(ownerUid: String): Flow<List<WorkoutSummary>>
    fun observeTodayWater(ownerUid: String): Flow<Int>
    fun observeDiagnostics(ownerUid: String): Flow<DiagnosticSnapshot>
    fun observeUnownedRecordCount(): Flow<Int>

    suspend fun startWorkout(ownerUid: String): String
    suspend fun finishWorkout(ownerUid: String, workoutId: String)
    suspend fun addSet(
        ownerUid: String,
        workoutId: String,
        exerciseId: String,
        exerciseName: String,
        weightKg: Double,
        repetitions: Int
    )
    suspend fun addWater(ownerUid: String, amountMl: Int)
    suspend fun claimUnownedRecords(ownerUid: String): Int
}
