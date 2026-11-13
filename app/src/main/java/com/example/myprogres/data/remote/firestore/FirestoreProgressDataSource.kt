package com.example.myprogres.data.remote.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProgressDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    internal suspend fun putWorkout(uid: String, workout: FirestoreWorkoutDto) {
        require(uid.isNotBlank() && workout.ownerUid == uid)
        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .document(workout.id)
            .set(workout.toDocument())
            .awaitAck()
    }

    internal suspend fun putWorkoutSet(uid: String, set: FirestoreWorkoutSetDto) {
        require(uid.isNotBlank())
        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .document(set.workoutId)
            .collection("sets")
            .document(set.id)
            .set(set.toDocument())
            .awaitAck()
    }

    internal suspend fun putWaterIntake(uid: String, intake: FirestoreWaterIntakeDto) {
        require(uid.isNotBlank() && intake.ownerUid == uid)
        firestore.collection("users")
            .document(uid)
            .collection("water_intake")
            .document(intake.id)
            .set(intake.toDocument())
            .awaitAck()
    }
}

private suspend fun Task<Void>.awaitAck(): Unit =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firestore write failed.")
                )
            }
        }
    }
