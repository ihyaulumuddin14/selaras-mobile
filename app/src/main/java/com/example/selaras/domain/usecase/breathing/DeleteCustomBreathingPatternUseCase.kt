package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository

class DeleteCustomBreathingPatternUseCase(private val repository: BreathingRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.deleteCustomPatternById(id)
    suspend operator fun invoke(pattern: BreathingPattern): Result<Unit> = repository.deleteCustomPattern(pattern)
}
