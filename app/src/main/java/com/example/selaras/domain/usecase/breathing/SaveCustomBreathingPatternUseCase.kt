package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository

class SaveCustomBreathingPatternUseCase(private val repository: BreathingRepository) {
    suspend operator fun invoke(pattern: BreathingPattern): Result<BreathingPattern> =
        repository.saveCustomPattern(pattern)

    suspend operator fun invoke(name: String, inhaleSeconds: Int, exhaleSeconds: Int): Result<BreathingPattern> =
        repository.saveCustomPattern(
            BreathingPattern(
                key = "custom:0",
                name = name,
                inhaleSeconds = inhaleSeconds,
                exhaleSeconds = exhaleSeconds
            )
        )
}
