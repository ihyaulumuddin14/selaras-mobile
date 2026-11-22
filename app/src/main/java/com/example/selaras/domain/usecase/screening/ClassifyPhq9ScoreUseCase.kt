
package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.ScreeningCategory

class ClassifyPhq9ScoreUseCase {

    operator fun invoke(totalScore: Int): ScreeningCategory {

        require(totalScore in MIN_SCORE..MAX_SCORE) {
            "PHQ-9 score must be between 0 and 27"
        }

        return when (totalScore) {
            in 0..4 -> ScreeningCategory.MINIMAL
            in 5..9 -> ScreeningCategory.RINGAN
            in 10..14 -> ScreeningCategory.SEDANG
            in 15..19 -> ScreeningCategory.SEDANG_BERAT
            in 20..27 -> ScreeningCategory.BERAT

            else -> throw IllegalArgumentException(
                "Invalid PHQ-9 score: $totalScore"
            )
        }
    }

    companion object {
        private const val MIN_SCORE = 0
        private const val MAX_SCORE = 27
    }
}
