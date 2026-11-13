package com.example.myprogres.data.di

import android.content.Context
import androidx.room.Room
import com.example.myprogres.BuildConfig
import com.example.myprogres.core.error.ErrorManager
import com.example.myprogres.data.local.AppDatabase
import com.example.myprogres.data.local.MIGRATION_1_2
import com.example.myprogres.data.repository.FirebaseAuthRepository
import com.example.myprogres.data.repository.OfflineProgressRepository
import com.example.myprogres.domain.repository.AuthRepository
import com.example.myprogres.domain.repository.ProgressRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        if (BuildConfig.DEBUG) {
            val emulatorHost = BuildConfig.FIRESTORE_EMULATOR_HOST.trim()
            check(emulatorHost.isNotEmpty()) {
                "Debug builds require host in firestore-emulator.local.properties."
            }
            firestore.useEmulator(emulatorHost, 8080)
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository =
        FirebaseAuthRepository(firebaseAuth)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "myprogres.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideProgressRepository(database: AppDatabase): ProgressRepository =
        OfflineProgressRepository(
            database = database,
            workoutDao = database.workoutDao(),
            waterDao = database.waterDao(),
            errorLogDao = database.errorLogDao()
        )

    @Provides
    @Singleton
    fun provideErrorManager(database: AppDatabase): ErrorManager =
        ErrorManager(database.errorLogDao())
}
