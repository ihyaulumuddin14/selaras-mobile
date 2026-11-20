package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.repository.BreathingRepository

class DeleteCustomBreathingPatternUseCase(private val repository: BreathingRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteCustomPattern(id)
}
