package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.model.BreathingPattern

enum class BreathingRunState { READY, RUNNING, PAUSED }

enum class BreathingPhase { INHALE, HOLD_INHALE, EXHALE, HOLD_EXHALE }

data class BreathingSessionSnapshot(
    val runState: BreathingRunState = BreathingRunState.READY,
    val phase: BreathingPhase = BreathingPhase.INHALE,
    val secondsRemaining: Int = 0,
    val phaseProgress: Float = 0f,
    val activeMillis: Long = 0L
)

/**
 * Pure Kotlin clock-independent state machine. The ViewModel supplies elapsed time from
 * SystemClock.elapsedRealtime(), so the countdown cannot drift when a frame is delayed.
 * Pausing preserves the exact phase position and stopping never persists session history.
 */
class BreathingSessionEngine(private val pattern: BreathingPattern) {
    private data class Step(val phase: BreathingPhase, val durationMs: Long)

    private val steps = buildList {
        add(Step(BreathingPhase.INHALE, pattern.inhaleSeconds * 1_000L))
        if (pattern.holdAfterInhaleSeconds > 0) {
            add(Step(BreathingPhase.HOLD_INHALE, pattern.holdAfterInhaleSeconds * 1_000L))
        }
        add(Step(BreathingPhase.EXHALE, pattern.exhaleSeconds * 1_000L))
        if (pattern.holdAfterExhaleSeconds > 0) {
            add(Step(BreathingPhase.HOLD_EXHALE, pattern.holdAfterExhaleSeconds * 1_000L))
        }
    }

    private val cycleDurationMs = steps.sumOf { it.durationMs }
    private var stepIndex = 0
    private var elapsedInStepMs = 0L
    private var activeMillis = 0L
    private var runState = BreathingRunState.READY

    fun start() {
        if (runState == BreathingRunState.READY) runState = BreathingRunState.RUNNING
    }

    fun pause() {
        if (runState == BreathingRunState.RUNNING) runState = BreathingRunState.PAUSED
    }

    fun resume() {
        if (runState == BreathingRunState.PAUSED) runState = BreathingRunState.RUNNING
    }

    fun stop() {
        runState = BreathingRunState.READY
        stepIndex = 0
        elapsedInStepMs = 0L
        activeMillis = 0L
    }

    fun advanceBy(elapsedMs: Long) {
        if (runState != BreathingRunState.RUNNING || elapsedMs <= 0L) return
        activeMillis += elapsedMs

        // Whole additional cycles leave the current phase unchanged.
        var toConsume = elapsedMs % cycleDurationMs
        while (toConsume > 0L) {
            val remainingInStep = steps[stepIndex].durationMs - elapsedInStepMs
            if (toConsume < remainingInStep) {
                elapsedInStepMs += toConsume
                toConsume = 0L
            } else {
                toConsume -= remainingInStep
                stepIndex = (stepIndex + 1) % steps.size
                elapsedInStepMs = 0L
            }
        }
    }

    fun snapshot(): BreathingSessionSnapshot {
        val step = steps[stepIndex]
        val remainingMs = step.durationMs - elapsedInStepMs
        return BreathingSessionSnapshot(
            runState = runState,
            phase = step.phase,
            secondsRemaining = ((remainingMs + 999L) / 1_000L).toInt(),
            phaseProgress = (elapsedInStepMs.toFloat() / step.durationMs).coerceIn(0f, 1f),
            activeMillis = activeMillis
        )
    }
}
