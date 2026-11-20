package com.example.selaras.domain.repository

import com.example.selaras.domain.model.BreathingPattern
import kotlinx.coroutines.flow.Flow

interface BreathingRepository {
    fun observeCustomPatterns(): Flow<List<BreathingPattern>>
    suspend fun saveCustomPattern(name: String, inhaleSeconds: Int, exhaleSeconds: Int): Long
    suspend fun updateCustomPattern(id: Long, name: String, inhaleSeconds: Int, exhaleSeconds: Int)
    suspend fun deleteCustomPattern(id: Long)
}
