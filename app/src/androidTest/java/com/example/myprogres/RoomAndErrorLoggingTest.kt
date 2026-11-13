package com.example.myprogres

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.myprogres.core.error.AppResult
import com.example.myprogres.core.error.ErrorManager
import com.example.myprogres.data.local.AppDatabase
import com.example.myprogres.data.repository.OfflineProgressRepository
import com.example.myprogres.domain.model.ExerciseLibrary
import com.example.myprogres.testing.TestErrorLogRule
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class RoomAndErrorLoggingTest {
    @get:Rule
    val testErrorLogRule = TestErrorLogRule()

    private val ownerA = "uid-test-a"
    private val ownerB = "uid-test-b"
    private lateinit var database: AppDatabase
    private lateinit var repository: OfflineProgressRepository
    private lateinit var errorManager: ErrorManager

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = OfflineProgressRepository(
            database = database,
            workoutDao = database.workoutDao(),
            waterDao = database.waterDao(),
            errorLogDao = database.errorLogDao()
        )
        errorManager = ErrorManager(database.errorLogDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun workoutAndSet_arePersistedAndReadableOnlyByOwner() = runBlocking {
        val workoutId = repository.startWorkout(ownerA)
        val exercise = ExerciseLibrary.exercises.first()
        repository.addSet(ownerA, workoutId, exercise.id, exercise.name, 60.0, 10)
        repository.addWater(ownerA, 300)

        val activeA = repository.observeActiveWorkout(ownerA).first()
        assertEquals(1, activeA?.sets?.size)
        assertEquals(60.0, activeA?.sets?.first()?.weightKg ?: 0.0, 0.0)
        assertEquals(10, activeA?.sets?.first()?.repetitions)

        assertEquals(null, repository.observeActiveWorkout(ownerB).first())
        assertTrue(repository.observeHistory(ownerB).first().isEmpty())
        assertEquals(0, repository.observeTodayWater(ownerB).first())
        assertTrue(database.workoutDao().observeSets(workoutId, ownerB).first().isEmpty())

        var otherOwnerCouldFinish = true
        try {
            repository.finishWorkout(ownerB, workoutId)
        } catch (_: Exception) {
            otherOwnerCouldFinish = false
        }
        assertFalse(otherOwnerCouldFinish)

        repository.finishWorkout(ownerA, workoutId)
        val historyA = repository.observeHistory(ownerA).first()
        assertEquals(1, historyA.size)
        assertEquals(1, historyA.first().setCount)
        assertEquals(300, repository.observeTodayWater(ownerA).first())
    }

    @Test
    fun emptyOwnerUid_isRejected() = runBlocking {
        var rejected = false
        try {
            repository.startWorkout(" ")
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun firebaseNetworkFailure_isClassifiedAndSanitized() = runBlocking {
        val result = errorManager.execute("SYNC", "UPLOAD") {
            throw FirebaseNetworkException("private network details")
        }

        assertTrue(result is AppResult.Failure)
        assertEquals("NETWORK", (result as AppResult.Failure).error.type)
        assertTrue(result.error.safeMessage.contains("guardados en este dispositivo"))
        val log = database.errorLogDao().observeRecent().first().single()
        assertEquals("NET_001", log.code)
        assertFalse(log.safeMessage.contains("private network details"))
    }

    @Test
    fun firebaseAuthAndFirestoreFailures_haveSafeCategories() = runBlocking {
        val auth = errorManager.execute("AUTH", "LOGIN") {
            throw FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIALS", "private auth details")
        }
        val firestore = errorManager.execute("SYNC", "UPLOAD") {
            throw FirebaseFirestoreException(
                "private firestore details",
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                null
            )
        }

        assertTrue(auth is AppResult.Failure)
        assertEquals("AUTHENTICATION", (auth as AppResult.Failure).error.type)
        assertTrue(firestore is AppResult.Failure)
        assertEquals("FIRESTORE", (firestore as AppResult.Failure).error.type)
        val logs = database.errorLogDao().observeRecent().first()
        assertEquals(2, logs.size)
        assertTrue(logs.none { it.safeMessage.contains("private") })
    }

    @Test
    fun failedOperation_writesSanitizedErrorLog() = runBlocking {
        val result = errorManager.execute("TRAINING", "SAVE_SET") {
            throw IllegalStateException("private test payload")
        }

        assertTrue(result is AppResult.Failure)
        val logs = database.errorLogDao().observeRecent().first()
        assertEquals(1, logs.size)
        assertEquals("APP_001", logs.first().code)
        assertEquals("TRAINING", logs.first().module)
        assertFalse(logs.first().safeMessage.contains("private test payload"))
    }
}
