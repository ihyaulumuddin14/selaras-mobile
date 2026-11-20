package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.repository.BreathingRepository

class SaveCustomBreathingPatternUseCase(private val repository: BreathingRepository) {
    suspend operator fun invoke(name: String, inhaleSeconds: Int, exhaleSeconds: Int): Long =
        repository.saveCustomPattern(name, inhaleSeconds, exhaleSeconds)
}
