package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository

class UpdateCustomBreathingPatternUseCase(private val repository: BreathingRepository) {
    suspend operator fun invoke(pattern: BreathingPattern): Result<Unit> =
        repository.updateCustomPattern(pattern)

    suspend operator fun invoke(id: Long, name: String, inhaleSeconds: Int, exhaleSeconds: Int): Result<Unit> =
        repository.updateCustomPattern(
            BreathingPattern(
                key = "custom:$id",
                id = id,
                name = name,
                inhaleSeconds = inhaleSeconds,
                exhaleSeconds = exhaleSeconds
            )
        )
}
