package com.example.selaras

import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.usecase.breathing.BreathingPhase
import com.example.selaras.domain.usecase.breathing.BreathingRunState
import com.example.selaras.domain.usecase.breathing.BreathingSessionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingSessionEngineTest {
    private val pattern = BreathingPattern("test", "Relaksasi Santai", 4, 6)

    @Test fun `cycles inhale and exhale without timer drift`() {
        val engine = BreathingSessionEngine(pattern)
        engine.start()
        engine.advanceBy(3_200)
        assertEquals(BreathingPhase.INHALE, engine.snapshot().phase)
        assertEquals(1, engine.snapshot().secondsRemaining)

        engine.advanceBy(800)
        assertEquals(BreathingPhase.EXHALE, engine.snapshot().phase)
        assertEquals(6, engine.snapshot().secondsRemaining)

        engine.advanceBy(6_000)
        assertEquals(BreathingPhase.INHALE, engine.snapshot().phase)
        assertEquals(10_000L, engine.snapshot().activeMillis)
    }

    @Test fun `pause freezes phase and elapsed active time`() {
        val engine = BreathingSessionEngine(pattern)
        engine.start()
        engine.advanceBy(2_000)
        engine.pause()
        engine.advanceBy(20_000)
        assertEquals(BreathingRunState.PAUSED, engine.snapshot().runState)
        assertEquals(2_000L, engine.snapshot().activeMillis)
        assertEquals(2, engine.snapshot().secondsRemaining)

        engine.resume()
        engine.advanceBy(2_000)
        assertEquals(BreathingPhase.EXHALE, engine.snapshot().phase)
    }

    @Test fun `stop resets all session data`() {
        val engine = BreathingSessionEngine(pattern)
        engine.start()
        engine.advanceBy(42_000)
        engine.stop()
        assertEquals(BreathingRunState.READY, engine.snapshot().runState)
        assertEquals(0L, engine.snapshot().activeMillis)
        assertEquals(BreathingPhase.INHALE, engine.snapshot().phase)
    }

    @Test fun `optional holds are distinct phases`() {
        val withHold = pattern.copy(holdAfterInhaleSeconds = 2, holdAfterExhaleSeconds = 1)
        val engine = BreathingSessionEngine(withHold)
        engine.start()
        engine.advanceBy(4_000)
        assertEquals(BreathingPhase.HOLD_INHALE, engine.snapshot().phase)
        engine.advanceBy(2_000)
        assertEquals(BreathingPhase.EXHALE, engine.snapshot().phase)
        engine.advanceBy(6_000)
        assertEquals(BreathingPhase.HOLD_EXHALE, engine.snapshot().phase)
    }
}
