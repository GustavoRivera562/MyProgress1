package com.example.myprogres

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.myprogres.data.local.AppDatabase
import com.example.myprogres.data.local.MIGRATION_1_2
import com.example.myprogres.data.repository.OfflineProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class RoomMigrationAndIsolationTest {
    @Test
    fun v1Data_isPreservedUnowned_hiddenUntilExplicitClaim() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-v1-v2-test.db"
        context.deleteDatabase(databaseName)

        createVersionOneDatabase(context, databaseName)

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = OfflineProgressRepository(
                database = database,
                workoutDao = database.workoutDao(),
                waterDao = database.waterDao(),
                errorLogDao = database.errorLogDao()
            )

            assertEquals(2, repository.observeUnownedRecordCount().first())
            assertTrue(repository.observeHistory("uid-a").first().isEmpty())
            assertEquals(0, repository.observeTodayWater("uid-a").first())
            assertNull(database.workoutDao().getWorkout("legacy-workout", "uid-a"))

            val preservedSets = database.openHelper.writableDatabase.query(
                "SELECT COUNT(*) FROM workout_sets WHERE workoutId = 'legacy-workout'"
            )
            assertTrue(preservedSets.moveToFirst())
            assertEquals(1, preservedSets.getInt(0))
            preservedSets.close()
            assertEquals(1, database.errorLogDao().observeRecent().first().size)

            assertEquals(2, repository.claimUnownedRecords("uid-a"))
            assertEquals(0, repository.observeUnownedRecordCount().first())
            assertEquals(1, repository.observeHistory("uid-a").first().size)
            assertEquals(1, repository.observeHistory("uid-a").first().first().setCount)
            assertEquals(250, repository.observeTodayWater("uid-a").first())

            assertTrue(repository.observeHistory("uid-b").first().isEmpty())
            assertEquals(0, repository.observeTodayWater("uid-b").first())
            assertTrue(database.workoutDao().observeSets("legacy-workout", "uid-b").first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionOneDatabase(context: Context, databaseName: String) {
        val db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        try {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS workouts (" +
                    "id TEXT NOT NULL, startedAt INTEGER NOT NULL, finishedAt INTEGER, " +
                    "syncState TEXT NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS workout_sets (" +
                    "id TEXT NOT NULL, workoutId TEXT NOT NULL, exerciseId TEXT NOT NULL, " +
                    "exerciseName TEXT NOT NULL, setNumber INTEGER NOT NULL, weightKg REAL NOT NULL, " +
                    "repetitions INTEGER NOT NULL, recordedAt INTEGER NOT NULL, syncState TEXT NOT NULL, " +
                    "PRIMARY KEY(id), FOREIGN KEY(workoutId) REFERENCES workouts(id) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_workout_sets_workoutId ON workout_sets(workoutId)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_workout_sets_recordedAt ON workout_sets(recordedAt)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS water_intake (" +
                    "id TEXT NOT NULL, amountMl INTEGER NOT NULL, loggedAt INTEGER NOT NULL, " +
                    "syncState TEXT NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_water_intake_loggedAt ON water_intake(loggedAt)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS error_logs (" +
                    "errorId TEXT NOT NULL, createdAt INTEGER NOT NULL, module TEXT NOT NULL, " +
                    "type TEXT NOT NULL, code TEXT NOT NULL, severity TEXT NOT NULL, " +
                    "operation TEXT NOT NULL, safeMessage TEXT NOT NULL, appVersion TEXT NOT NULL, " +
                    "syncState TEXT NOT NULL, attempts INTEGER NOT NULL, PRIMARY KEY(errorId))"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_error_logs_createdAt ON error_logs(createdAt)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_error_logs_severity ON error_logs(severity)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES(42, '0f778d550af9e26d40d8966781a91962')"
            )
            db.execSQL(
                "INSERT INTO workouts(id, startedAt, finishedAt, syncState) " +
                    "VALUES('legacy-workout', 1700000000000, 1700003600000, 'PENDING')"
            )
            db.execSQL(
                "INSERT INTO workout_sets(id, workoutId, exerciseId, exerciseName, setNumber, " +
                    "weightKg, repetitions, recordedAt, syncState) " +
                    "VALUES('legacy-set', 'legacy-workout', 'squat', 'Sentadilla', 1, 50, 8, 1700000100000, 'PENDING')"
            )
            db.execSQL(
                "INSERT INTO water_intake(id, amountMl, loggedAt, syncState) " +
                    "VALUES('legacy-water', 250, ${System.currentTimeMillis()}, 'PENDING')"
            )
            db.execSQL(
                "INSERT INTO error_logs(errorId, createdAt, module, type, code, severity, " +
                    "operation, safeMessage, appVersion, syncState, attempts) " +
                    "VALUES('legacy-error', 1700000000000, 'DATABASE', 'DATABASE', 'DB_001', " +
                    "'LOW', 'READ', 'Error local saneado', '1.0', 'PENDING', 0)"
            )
            db.version = 1
        } finally {
            db.close()
        }
    }
}
