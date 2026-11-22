
package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.ScreeningAnswer

class CalculatePhq9ScoreUseCase {

    operator fun invoke(
        answers: List<ScreeningAnswer>
    ): Int {

        require(answers.size == QUESTION_COUNT) {
            "PHQ-9 requires exactly 9 answers"
        }

        val questionNumbers = answers
            .map { it.questionNumber }
            .sorted()

        require(questionNumbers == (1..QUESTION_COUNT).toList()) {
            "All questions from 1 to 9 must be answered exactly once"
        }

        require(answers.all { it.answerValue in MIN_ANSWER..MAX_ANSWER }) {
            "Answer values must be between 0 and 3"
        }

        return answers.sumOf { it.answerValue }
    }

    companion object {
        private const val QUESTION_COUNT = 9
        private const val MIN_ANSWER = 0
        private const val MAX_ANSWER = 3
    }
}
