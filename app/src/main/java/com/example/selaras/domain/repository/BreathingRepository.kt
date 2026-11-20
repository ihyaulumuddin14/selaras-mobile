package com.example.selaras.domain.repository

import com.example.selaras.domain.model.BreathingPattern
import kotlinx.coroutines.flow.Flow

interface BreathingRepository {
    fun observeCustomPatterns(): Flow<List<BreathingPattern>>
    suspend fun saveCustomPattern(pattern: BreathingPattern): Result<BreathingPattern>
    suspend fun updateCustomPattern(pattern: BreathingPattern): Result<Unit>
    suspend fun deleteCustomPattern(pattern: BreathingPattern): Result<Unit>
    suspend fun deleteCustomPatternById(id: Long): Result<Unit>
    suspend fun getPatternById(id: Long): BreathingPattern?
    suspend fun getPatternByUuid(uuid: String): BreathingPattern?
}
