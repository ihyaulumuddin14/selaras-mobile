package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository
import kotlinx.coroutines.flow.Flow

class GetBreathingPatternUseCase(private val repository: BreathingRepository) {
    operator fun invoke(): Flow<List<BreathingPattern>> = repository.observeCustomPatterns()
}
