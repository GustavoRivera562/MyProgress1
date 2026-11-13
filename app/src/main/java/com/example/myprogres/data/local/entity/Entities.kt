package com.example.myprogres.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    indices = [Index(value = ["ownerUid", "startedAt"])]
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val syncState: String = SYNC_PENDING,
    val ownerUid: String?
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId"), Index("recordedAt")]
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val repetitions: Int,
    val recordedAt: Long,
    val syncState: String = SYNC_PENDING
)

@Entity(
    tableName = "water_intake",
    indices = [Index("loggedAt"), Index(value = ["ownerUid", "loggedAt"])]
)
data class WaterIntakeEntity(
    @PrimaryKey val id: String,
    val amountMl: Int,
    val loggedAt: Long,
    val syncState: String = SYNC_PENDING,
    val ownerUid: String?
)

@Entity(
    tableName = "error_logs",
    indices = [Index("createdAt"), Index("severity")]
)
data class ErrorLogEntity(
    @PrimaryKey val errorId: String,
    val createdAt: Long,
    val module: String,
    val type: String,
    val code: String,
    val severity: String,
    val operation: String,
    val safeMessage: String,
    val appVersion: String,
    val syncState: String = "PENDING",
    val attempts: Int = 0
)

data class WorkoutSummaryRow(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val setCount: Int,
    @ColumnInfo(name = "totalVolumeKg") val totalVolumeKg: Double
)

const val SYNC_PENDING = "PENDING"
const val SYNCED = "SYNCED"
const val SYNC_FAILED = "FAILED"
const val SYNC_CONFLICT = "CONFLICT"
