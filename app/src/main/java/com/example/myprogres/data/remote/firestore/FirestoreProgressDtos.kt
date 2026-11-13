package com.example.myprogres.data.remote.firestore

import com.example.myprogres.data.local.entity.WaterIntakeEntity
import com.example.myprogres.data.local.entity.WorkoutEntity
import com.example.myprogres.data.local.entity.WorkoutSetEntity

/**
 * Firestore payloads intentionally contain only fields permitted by firestore.rules.
 * Sync metadata stays local in Room.
 */
internal data class FirestoreWorkoutDto(
    val id: String,
    val ownerUid: String,
    val startedAt: Long,
    val finishedAt: Long?
) {
    fun toDocument() = mapOf(
        "id" to id,
        "ownerUid" to ownerUid,
        "startedAt" to startedAt,
        "finishedAt" to finishedAt
    )
}

internal data class FirestoreWorkoutSetDto(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val repetitions: Int,
    val recordedAt: Long
) {
    fun toDocument() = mapOf(
        "id" to id,
        "workoutId" to workoutId,
        "exerciseId" to exerciseId,
        "exerciseName" to exerciseName,
        "setNumber" to setNumber,
        "weightKg" to weightKg,
        "repetitions" to repetitions,
        "recordedAt" to recordedAt
    )
}

internal data class FirestoreWaterIntakeDto(
    val id: String,
    val ownerUid: String,
    val amountMl: Int,
    val loggedAt: Long
) {
    fun toDocument() = mapOf(
        "id" to id,
        "ownerUid" to ownerUid,
        "amountMl" to amountMl,
        "loggedAt" to loggedAt
    )
}

internal fun WorkoutEntity.toFirestoreDto(uid: String): FirestoreWorkoutDto {
    require(ownerUid == uid && uid.isNotBlank()) { "Workout owner does not match the active account." }
    return FirestoreWorkoutDto(
        id = id,
        ownerUid = uid,
        startedAt = startedAt,
        finishedAt = finishedAt
    )
}

internal fun WorkoutSetEntity.toFirestoreDto(): FirestoreWorkoutSetDto =
    FirestoreWorkoutSetDto(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        setNumber = setNumber,
        weightKg = weightKg,
        repetitions = repetitions,
        recordedAt = recordedAt
    )

internal fun WaterIntakeEntity.toFirestoreDto(uid: String): FirestoreWaterIntakeDto {
    require(ownerUid == uid && uid.isNotBlank()) { "Water intake owner does not match the active account." }
    return FirestoreWaterIntakeDto(
        id = id,
        ownerUid = uid,
        amountMl = amountMl,
        loggedAt = loggedAt
    )
}
