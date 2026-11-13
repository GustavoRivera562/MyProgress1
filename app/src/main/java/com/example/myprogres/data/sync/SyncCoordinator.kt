package com.example.myprogres.data.sync

import com.example.myprogres.data.local.AppDatabase
import com.example.myprogres.data.local.entity.SYNC_PENDING
import com.example.myprogres.data.local.entity.SYNCED
import com.example.myprogres.data.remote.firestore.FirestoreProgressDataSource
import com.example.myprogres.data.remote.firestore.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: AppDatabase,
    private val remote: FirestoreProgressDataSource
) {
    private val syncMutex = Mutex()

    /**
     * Reads the account directly from FirebaseAuth. No UID is accepted from UI or callers.
     * Room remains the source of truth; this only uploads pending local snapshots.
     */
    suspend fun syncPendingForCurrentUser() {
        syncMutex.withLock {
            val uid = firebaseAuth.currentUser?.uid
                ?.takeIf(String::isNotBlank)
                ?: return
            syncForUid(uid)
        }
    }

    private suspend fun syncForUid(uid: String) {
        val workoutDao = database.workoutDao()

        // Parent documents are acknowledged before any pending child sets are sent.
        val pendingWorkouts = workoutDao.getPendingWorkouts(uid)
        for (snapshot in pendingWorkouts) {
            ensureSameSession(uid)
            remote.putWorkout(uid, snapshot.toFirestoreDto(uid))
            ensureSameSession(uid)
            val marked = workoutDao.markWorkoutSynced(
                workoutId = snapshot.id,
                ownerUid = uid,
                expectedStartedAt = snapshot.startedAt,
                expectedFinishedAt = snapshot.finishedAt
            )
            if (marked == 1) {
                syncSetsForWorkout(uid, snapshot.id)
            }
        }

        // Sets may be added later to a parent that was already acknowledged.
        val workoutIdsWithPendingSets = workoutDao.getWorkoutIdsWithPendingSets(uid)
        for (workoutId in workoutIdsWithPendingSets) {
            ensureSameSession(uid)
            val parent = workoutDao.getWorkout(workoutId, uid) ?: continue
            if (parent.syncState == SYNC_PENDING) {
                remote.putWorkout(uid, parent.toFirestoreDto(uid))
                ensureSameSession(uid)
                val marked = workoutDao.markWorkoutSynced(
                    workoutId = parent.id,
                    ownerUid = uid,
                    expectedStartedAt = parent.startedAt,
                    expectedFinishedAt = parent.finishedAt
                )
                if (marked != 1) continue
            } else if (parent.syncState != SYNCED) {
                continue
            }
            syncSetsForWorkout(uid, workoutId)
        }

        val pendingWater = database.waterDao().getPendingIntakes(uid)
        for (snapshot in pendingWater) {
            ensureSameSession(uid)
            remote.putWaterIntake(uid, snapshot.toFirestoreDto(uid))
            ensureSameSession(uid)
            database.waterDao().markIntakeSynced(
                intakeId = snapshot.id,
                ownerUid = uid,
                expectedAmountMl = snapshot.amountMl,
                expectedLoggedAt = snapshot.loggedAt
            )
        }
    }

    private suspend fun syncSetsForWorkout(uid: String, workoutId: String) {
        val workoutDao = database.workoutDao()
        val snapshots = workoutDao.getPendingSets(workoutId, uid)
        for (snapshot in snapshots) {
            ensureSameSession(uid)
            remote.putWorkoutSet(uid, snapshot.toFirestoreDto())
            ensureSameSession(uid)
            workoutDao.markSetSynced(
                setId = snapshot.id,
                workoutId = snapshot.workoutId,
                ownerUid = uid,
                expectedExerciseId = snapshot.exerciseId,
                expectedExerciseName = snapshot.exerciseName,
                expectedSetNumber = snapshot.setNumber,
                expectedWeightKg = snapshot.weightKg,
                expectedRepetitions = snapshot.repetitions,
                expectedRecordedAt = snapshot.recordedAt
            )
        }
    }

    private suspend fun ensureSameSession(expectedUid: String) {
        currentCoroutineContext().ensureActive()
        if (firebaseAuth.currentUser?.uid != expectedUid) {
            throw CancellationException("Firebase account changed during synchronization.")
        }
    }
}
