package com.example.myprogres.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workouts ADD COLUMN ownerUid TEXT")
        db.execSQL("ALTER TABLE water_intake ADD COLUMN ownerUid TEXT")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_workouts_ownerUid_startedAt " +
                "ON workouts(ownerUid, startedAt)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_water_intake_ownerUid_loggedAt " +
                "ON water_intake(ownerUid, loggedAt)"
        )
    }
}
