package com.example.myprogres.data.repository

import androidx.room.withTransaction
import com.example.myprogres.core.error.ValidationException
import com.example.myprogres.data.local.AppDatabase
import com.example.myprogres.data.local.dao.ErrorLogDao
import com.example.myprogres.data.local.dao.WaterDao
import com.example.myprogres.data.local.dao.WorkoutDao
import com.example.myprogres.data.local.entity.ErrorLogEntity
import com.example.myprogres.data.local.entity.WaterIntakeEntity
import com.example.myprogres.data.local.entity.WorkoutEntity
import com.example.myprogres.data.local.entity.WorkoutSetEntity
import com.example.myprogres.domain.model.ActiveWorkout
import com.example.myprogres.domain.model.DiagnosticError
import com.example.myprogres.domain.model.DiagnosticSnapshot
import com.example.myprogres.domain.model.Workout
import com.example.myprogres.domain.model.WorkoutSet
import com.example.myprogres.domain.model.WorkoutSummary
import com.example.myprogres.domain.repository.ProgressRepository
import com.example.myprogres.domain.validation.ProgressInputValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineProgressRepository(
    private val database: AppDatabase,
    private val workoutDao: WorkoutDao,
    private val waterDao: WaterDao,
    private val errorLogDao: ErrorLogDao
) : ProgressRepository {
    override fun observeActiveWorkout(ownerUid: String): Flow<ActiveWorkout?> {
        requireOwner(ownerUid)
        return workoutDao.observeActiveWorkout(ownerUid).flatMapLatest { workout ->
            if (workout == null) {
                flowOf(null)
            } else {
                workoutDao.observeSets(workout.id, ownerUid).map { sets ->
                    ActiveWorkout(
                        workout = workout.toDomain(),
                        sets = sets.map(WorkoutSetEntity::toDomain)
                    )
                }
            }
        }
    }

    override fun observeHistory(ownerUid: String): Flow<List<WorkoutSummary>> {
        requireOwner(ownerUid)
        return workoutDao.observeHistory(ownerUid).map { rows ->
            rows.map { row ->
                WorkoutSummary(
                    id = row.id,
                    startedAt = row.startedAt,
                    finishedAt = row.finishedAt,
                    setCount = row.setCount,
                    totalVolumeKg = row.totalVolumeKg
                )
            }
        }
    }

    override fun observeTodayWater(ownerUid: String): Flow<Int> {
        requireOwner(ownerUid)
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return waterDao.observeTotalBetween(ownerUid, start.timeInMillis, end.timeInMillis)
    }

    override fun observeDiagnostics(ownerUid: String): Flow<DiagnosticSnapshot> {
        requireOwner(ownerUid)
        return combine(
            workoutDao.observePendingSetCount(ownerUid),
            workoutDao.observePendingWorkoutCount(ownerUid),
            waterDao.observePendingCount(ownerUid),
            errorLogDao.observeRecent()
        ) { pendingSets, pendingWorkouts, pendingWater, errors ->
            DiagnosticSnapshot(
                pendingSyncCount = pendingSets + pendingWorkouts + pendingWater,
                recentErrors = errors.map(ErrorLogEntity::toDiagnostic)
            )
        }
    }

    override fun observeUnownedRecordCount(): Flow<Int> =
        combine(workoutDao.observeUnownedWorkoutCount(), waterDao.observeUnownedIntakeCount()) { workouts, water ->
            workouts + water
        }

    override suspend fun startWorkout(ownerUid: String): String {
        requireOwner(ownerUid)
        return database.withTransaction {
            val active = workoutDao.getActiveWorkout(ownerUid)
            if (active != null) return@withTransaction active.id

            val id = UUID.randomUUID().toString()
            workoutDao.insertWorkout(
                WorkoutEntity(
                    id = id,
                    startedAt = System.currentTimeMillis(),
                    ownerUid = ownerUid
                )
            )
            id
        }
    }

    override suspend fun finishWorkout(ownerUid: String, workoutId: String) {
        requireOwner(ownerUid)
        database.withTransaction {
            val workout = workoutDao.getWorkout(workoutId, ownerUid)
                ?: throw ValidationException("La sesión ya no está disponible.")
            if (workout.finishedAt != null) return@withTransaction
            workoutDao.finishWorkout(workoutId, ownerUid, System.currentTimeMillis())
        }
    }

    override suspend fun addSet(
        ownerUid: String,
        workoutId: String,
        exerciseId: String,
        exerciseName: String,
        weightKg: Double,
        repetitions: Int
    ) {
        requireOwner(ownerUid)
        ProgressInputValidator.validateSet(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            weightKg = weightKg,
            repetitions = repetitions
        )
        database.withTransaction {
            val workout = workoutDao.getWorkout(workoutId, ownerUid)
                ?: throw ValidationException("La sesión ya no está disponible. Inicia otra para registrar series.")
            if (workout.finishedAt != null) {
                throw ValidationException("Esta sesión ya terminó. Inicia otra para registrar más series.")
            }
            val nextNumber = workoutDao.countSetsForExercise(workoutId, exerciseId, ownerUid) + 1
            workoutDao.insertSet(
                WorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName.trim(),
                    setNumber = nextNumber,
                    weightKg = weightKg,
                    repetitions = repetitions,
                    recordedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun addWater(ownerUid: String, amountMl: Int) {
        requireOwner(ownerUid)
        ProgressInputValidator.validateWater(amountMl)
        waterDao.insert(
            WaterIntakeEntity(
                id = UUID.randomUUID().toString(),
                amountMl = amountMl,
                loggedAt = System.currentTimeMillis(),
                ownerUid = ownerUid
            )
        )
    }

    override suspend fun claimUnownedRecords(ownerUid: String): Int {
        requireOwner(ownerUid)
        return database.withTransaction {
            workoutDao.claimUnownedWorkouts(ownerUid) +
                waterDao.claimUnownedIntakes(ownerUid)
        }
    }

    private fun requireOwner(ownerUid: String) {
        if (ownerUid.isBlank()) throw ValidationException("Se requiere una sesión autenticada.")
    }
}

private fun WorkoutEntity.toDomain() = Workout(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    setNumber = setNumber,
    weightKg = weightKg,
    repetitions = repetitions,
    recordedAt = recordedAt
)

private fun ErrorLogEntity.toDiagnostic() = DiagnosticError(
    code = code,
    module = module,
    safeMessage = safeMessage,
    createdAt = createdAt
)
