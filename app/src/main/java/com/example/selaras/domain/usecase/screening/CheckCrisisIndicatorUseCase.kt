
package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.ScreeningAnswer

class CheckCrisisIndicatorUseCase {

    operator fun invoke(
        answers: List<ScreeningAnswer>,
        totalScore: Int
    ): Boolean {

        require(answers.size == QUESTION_COUNT) {
            "PHQ-9 requires exactly 9 answers"
        }

        require(totalScore in MIN_SCORE..MAX_SCORE) {
            "PHQ-9 score must be between 0 and 27"
        }

        val questionNine = answers.singleOrNull {
            it.questionNumber == 9
        } ?: throw IllegalArgumentException(
            "Question 9 answer is missing"
        )

        return questionNine.answerValue > 0 ||
                totalScore >= CRISIS_SCORE_THRESHOLD
    }

    companion object {
        private const val QUESTION_COUNT = 9
        private const val MIN_SCORE = 0
        private const val MAX_SCORE = 27
        private const val CRISIS_SCORE_THRESHOLD = 20
    }
}
