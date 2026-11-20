package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.repository.BreathingRepository

class UpdateCustomBreathingPatternUseCase(private val repository: BreathingRepository) {
    suspend operator fun invoke(id: Long, name: String, inhaleSeconds: Int, exhaleSeconds: Int) =
        repository.updateCustomPattern(id, name, inhaleSeconds, exhaleSeconds)
}
