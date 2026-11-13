package com.example.myprogres.domain.repository

import com.example.myprogres.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun register(email: String, password: String)
    suspend fun signIn(email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    suspend fun signOut()
}
