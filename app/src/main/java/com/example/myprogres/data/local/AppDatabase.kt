package com.example.myprogres.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myprogres.data.local.dao.ErrorLogDao
import com.example.myprogres.data.local.dao.WaterDao
import com.example.myprogres.data.local.dao.WorkoutDao
import com.example.myprogres.data.local.entity.ErrorLogEntity
import com.example.myprogres.data.local.entity.WaterIntakeEntity
import com.example.myprogres.data.local.entity.WorkoutEntity
import com.example.myprogres.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        WaterIntakeEntity::class,
        ErrorLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun waterDao(): WaterDao
    abstract fun errorLogDao(): ErrorLogDao
}
