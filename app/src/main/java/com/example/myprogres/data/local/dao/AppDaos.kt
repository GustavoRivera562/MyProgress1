package com.example.myprogres.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myprogres.data.local.entity.ErrorLogEntity
import com.example.myprogres.data.local.entity.WaterIntakeEntity
import com.example.myprogres.data.local.entity.WorkoutEntity
import com.example.myprogres.data.local.entity.WorkoutSetEntity
import com.example.myprogres.data.local.entity.WorkoutSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE ownerUid = :ownerUid AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(ownerUid: String): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE ownerUid = :ownerUid AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveWorkout(ownerUid: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :workoutId AND ownerUid = :ownerUid LIMIT 1")
    suspend fun getWorkout(workoutId: String, ownerUid: String): WorkoutEntity?

    @Query("""
        SELECT * FROM workouts
        WHERE ownerUid = :ownerUid AND syncState = 'PENDING'
        ORDER BY startedAt ASC
    """)
    suspend fun getPendingWorkouts(ownerUid: String): List<WorkoutEntity>

    @Query("""
        SELECT DISTINCT w.id
        FROM workouts AS w
        INNER JOIN workout_sets AS s ON s.workoutId = w.id
        WHERE w.ownerUid = :ownerUid AND s.syncState = 'PENDING'
        ORDER BY w.startedAt ASC
    """)
    suspend fun getWorkoutIdsWithPendingSets(ownerUid: String): List<String>

    @Query("""
        SELECT s.*
        FROM workout_sets AS s
        INNER JOIN workouts AS w ON w.id = s.workoutId
        WHERE w.id = :workoutId
          AND w.ownerUid = :ownerUid
          AND s.syncState = 'PENDING'
        ORDER BY s.recordedAt ASC
    """)
    suspend fun getPendingSets(workoutId: String, ownerUid: String): List<WorkoutSetEntity>

    @Query("""
        UPDATE workouts SET syncState = 'SYNCED'
        WHERE id = :workoutId
          AND ownerUid = :ownerUid
          AND syncState = 'PENDING'
          AND startedAt = :expectedStartedAt
          AND finishedAt IS :expectedFinishedAt
    """)
    suspend fun markWorkoutSynced(
        workoutId: String,
        ownerUid: String,
        expectedStartedAt: Long,
        expectedFinishedAt: Long?
    ): Int

    @Query("""
        UPDATE workout_sets SET syncState = 'SYNCED'
        WHERE id = :setId
          AND workoutId = :workoutId
          AND syncState = 'PENDING'
          AND exerciseId = :expectedExerciseId
          AND exerciseName = :expectedExerciseName
          AND setNumber = :expectedSetNumber
          AND weightKg = :expectedWeightKg
          AND repetitions = :expectedRepetitions
          AND recordedAt = :expectedRecordedAt
          AND EXISTS (
              SELECT 1 FROM workouts AS w
              WHERE w.id = workout_sets.workoutId AND w.ownerUid = :ownerUid
          )
    """)
    suspend fun markSetSynced(
        setId: String,
        workoutId: String,
        ownerUid: String,
        expectedExerciseId: String,
        expectedExerciseName: String,
        expectedSetNumber: Int,
        expectedWeightKg: Double,
        expectedRepetitions: Int,
        expectedRecordedAt: Long
    ): Int

    @Query("""
        SELECT COUNT(s.id)
        FROM workout_sets AS s
        INNER JOIN workouts AS w ON w.id = s.workoutId
        WHERE s.workoutId = :workoutId
          AND s.exerciseId = :exerciseId
          AND w.ownerUid = :ownerUid
    """)
    suspend fun countSetsForExercise(workoutId: String, exerciseId: String, ownerUid: String): Int

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("""
        UPDATE workouts
        SET finishedAt = :finishedAt, syncState = 'PENDING'
        WHERE id = :workoutId AND ownerUid = :ownerUid AND finishedAt IS NULL
    """)
    suspend fun finishWorkout(workoutId: String, ownerUid: String, finishedAt: Long): Int

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity)

    @Query("""
        SELECT s.*
        FROM workout_sets AS s
        INNER JOIN workouts AS w ON w.id = s.workoutId
        WHERE s.workoutId = :workoutId AND w.ownerUid = :ownerUid
        ORDER BY s.recordedAt ASC
    """)
    fun observeSets(workoutId: String, ownerUid: String): Flow<List<WorkoutSetEntity>>

    @Query("""
        SELECT w.id AS id,
               w.startedAt AS startedAt,
               w.finishedAt AS finishedAt,
               COUNT(s.id) AS setCount,
               COALESCE(SUM(s.weightKg * s.repetitions), 0.0) AS totalVolumeKg
        FROM workouts AS w
        LEFT JOIN workout_sets AS s ON s.workoutId = w.id
        WHERE w.ownerUid = :ownerUid
        GROUP BY w.id
        ORDER BY w.startedAt DESC
        LIMIT 50
    """)
    fun observeHistory(ownerUid: String): Flow<List<WorkoutSummaryRow>>

    @Query("""
        SELECT COUNT(s.id)
        FROM workout_sets AS s
        INNER JOIN workouts AS w ON w.id = s.workoutId
        WHERE w.ownerUid = :ownerUid AND s.syncState = 'PENDING'
    """)
    fun observePendingSetCount(ownerUid: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM workouts WHERE ownerUid = :ownerUid AND syncState = 'PENDING'")
    fun observePendingWorkoutCount(ownerUid: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM workouts WHERE ownerUid = :ownerUid")
    fun observeWorkoutCount(ownerUid: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM workouts WHERE ownerUid IS NULL")
    fun observeUnownedWorkoutCount(): Flow<Int>

    @Query("UPDATE workouts SET ownerUid = :ownerUid WHERE ownerUid IS NULL")
    suspend fun claimUnownedWorkouts(ownerUid: String): Int
}

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(entry: WaterIntakeEntity)

    @Query("""
        SELECT * FROM water_intake
        WHERE ownerUid = :ownerUid AND syncState = 'PENDING'
        ORDER BY loggedAt ASC
    """)
    suspend fun getPendingIntakes(ownerUid: String): List<WaterIntakeEntity>

    @Query("""
        UPDATE water_intake SET syncState = 'SYNCED'
        WHERE id = :intakeId
          AND ownerUid = :ownerUid
          AND syncState = 'PENDING'
          AND amountMl = :expectedAmountMl
          AND loggedAt = :expectedLoggedAt
    """)
    suspend fun markIntakeSynced(
        intakeId: String,
        ownerUid: String,
        expectedAmountMl: Int,
        expectedLoggedAt: Long
    ): Int

    @Query("""
        SELECT COALESCE(SUM(amountMl), 0)
        FROM water_intake
        WHERE ownerUid = :ownerUid AND loggedAt >= :startAt AND loggedAt < :endAt
    """)
    fun observeTotalBetween(ownerUid: String, startAt: Long, endAt: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM water_intake WHERE ownerUid = :ownerUid AND syncState = 'PENDING'")
    fun observePendingCount(ownerUid: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM water_intake WHERE ownerUid IS NULL")
    fun observeUnownedIntakeCount(): Flow<Int>

    @Query("UPDATE water_intake SET ownerUid = :ownerUid WHERE ownerUid IS NULL")
    suspend fun claimUnownedIntakes(ownerUid: String): Int
}

@Dao
interface ErrorLogDao {
    @Insert
    suspend fun insert(error: ErrorLogEntity)

    @Query("SELECT * FROM error_logs ORDER BY createdAt DESC LIMIT 10")
    fun observeRecent(): Flow<List<ErrorLogEntity>>

    @Query("SELECT COUNT(*) FROM error_logs WHERE syncState = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}
