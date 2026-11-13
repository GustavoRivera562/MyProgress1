package com.example.myprogres

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myprogres.data.local.AppDatabase
import com.example.myprogres.data.local.entity.SYNCED
import com.example.myprogres.data.local.entity.SYNC_PENDING
import com.example.myprogres.data.local.entity.WaterIntakeEntity
import com.example.myprogres.data.local.entity.WorkoutEntity
import com.example.myprogres.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pendingQueries_areScopedToOwnerAndExcludeUnownedRows() = runBlocking {
        val workouts = database.workoutDao()
        workouts.insertWorkout(workout("workout-a", "uid-a"))
        workouts.insertWorkout(workout("workout-b", "uid-b"))
        workouts.insertWorkout(workout("workout-unowned", null))
        workouts.insertSet(set("set-a", "workout-a"))
        workouts.insertSet(set("set-b", "workout-b"))
        workouts.insertSet(set("set-unowned", "workout-unowned"))

        assertEquals(listOf("workout-a"), workouts.getPendingWorkouts("uid-a").map { it.id })
        assertEquals(listOf("workout-a"), workouts.getWorkoutIdsWithPendingSets("uid-a"))
        assertEquals(listOf("set-a"), workouts.getPendingSets("workout-a", "uid-a").map { it.id })
        assertTrue(workouts.getPendingWorkouts("uid-b").none { it.id == "workout-a" })
        assertTrue(workouts.getPendingSets("workout-unowned", "uid-a").isEmpty())
    }

    @Test
    fun workoutSyncAck_requiresMatchingUidStateAndSnapshot() = runBlocking {
        val dao = database.workoutDao()
        dao.insertWorkout(workout("workout-a", "uid-a"))

        assertEquals(0, dao.markWorkoutSynced("workout-a", "uid-b", 100L, null))
        assertEquals(0, dao.markWorkoutSynced("workout-a", "uid-a", 101L, null))
        assertEquals(SYNC_PENDING, dao.getWorkout("workout-a", "uid-a")?.syncState)

        assertEquals(1, dao.markWorkoutSynced("workout-a", "uid-a", 100L, null))
        assertEquals(SYNCED, dao.getWorkout("workout-a", "uid-a")?.syncState)
        assertEquals(0, dao.markWorkoutSynced("workout-a", "uid-a", 100L, null))
    }

    @Test
    fun finishingPreviouslySyncedWorkout_marksItPendingAgain() = runBlocking {
        val dao = database.workoutDao()
        dao.insertWorkout(workout("workout-a", "uid-a"))
        assertEquals(1, dao.markWorkoutSynced("workout-a", "uid-a", 100L, null))

        assertEquals(1, dao.finishWorkout("workout-a", "uid-a", 200L))
        val updated = dao.getWorkout("workout-a", "uid-a")
        assertEquals(200L, updated?.finishedAt)
        assertEquals(SYNC_PENDING, updated?.syncState)
    }

    @Test
    fun setAndWaterAck_requireExactOwnerAndLocalSnapshot() = runBlocking {
        val workouts = database.workoutDao()
        val water = database.waterDao()
        workouts.insertWorkout(workout("workout-a", "uid-a"))
        workouts.insertSet(set("set-a", "workout-a"))
        water.insert(
            WaterIntakeEntity(
                id = "water-a",
                amountMl = 500,
                loggedAt = 300L,
                ownerUid = "uid-a"
            )
        )
        water.insert(
            WaterIntakeEntity(
                id = "water-b",
                amountMl = 250,
                loggedAt = 301L,
                ownerUid = "uid-b"
            )
        )

        assertEquals(
            0,
            workouts.markSetSynced(
                setId = "set-a",
                workoutId = "workout-a",
                ownerUid = "uid-b",
                expectedExerciseId = "squat",
                expectedExerciseName = "Sentadilla",
                expectedSetNumber = 1,
                expectedWeightKg = 80.0,
                expectedRepetitions = 8,
                expectedRecordedAt = 150L
            )
        )
        assertEquals(
            0,
            workouts.markSetSynced(
                setId = "set-a",
                workoutId = "workout-a",
                ownerUid = "uid-a",
                expectedExerciseId = "squat",
                expectedExerciseName = "Sentadilla",
                expectedSetNumber = 1,
                expectedWeightKg = 81.0,
                expectedRepetitions = 8,
                expectedRecordedAt = 150L
            )
        )
        assertEquals(
            1,
            workouts.markSetSynced(
                setId = "set-a",
                workoutId = "workout-a",
                ownerUid = "uid-a",
                expectedExerciseId = "squat",
                expectedExerciseName = "Sentadilla",
                expectedSetNumber = 1,
                expectedWeightKg = 80.0,
                expectedRepetitions = 8,
                expectedRecordedAt = 150L
            )
        )

        assertEquals(0, water.markIntakeSynced("water-a", "uid-b", 500, 300L))
        assertEquals(0, water.markIntakeSynced("water-a", "uid-a", 501, 300L))
        assertEquals(1, water.markIntakeSynced("water-a", "uid-a", 500, 300L))
        assertTrue(water.getPendingIntakes("uid-a").isEmpty())
        assertEquals(1, water.getPendingIntakes("uid-b").size)
    }

    private fun workout(id: String, ownerUid: String?) = WorkoutEntity(
        id = id,
        startedAt = 100L,
        finishedAt = null,
        ownerUid = ownerUid
    )

    private fun set(id: String, workoutId: String) = WorkoutSetEntity(
        id = id,
        workoutId = workoutId,
        exerciseId = "squat",
        exerciseName = "Sentadilla",
        setNumber = 1,
        weightKg = 80.0,
        repetitions = 8,
        recordedAt = 150L
    )
}
