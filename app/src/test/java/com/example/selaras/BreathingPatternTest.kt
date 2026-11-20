package com.example.selaras

import com.example.selaras.domain.model.BreathingPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingPatternTest {
    @Test fun `approved default patterns have the right rhythm`() {
        assertEquals(4, BreathingPattern.defaults.size)
        assertEquals(
            listOf(4 to 6, 5 to 5, 3 to 5, 5 to 7),
            BreathingPattern.defaults.map { it.inhaleSeconds to it.exhaleSeconds }
        )
        assertFalse(BreathingPattern.defaults.first().isCustom)
    }

    @Test
    fun `custom pattern has isCustom true and uuid generated`() {
        val custom = BreathingPattern(
            key = "custom:1",
            name = "Pola Kustom",
            inhaleSeconds = 4,
            exhaleSeconds = 4
        )
        assertTrue(custom.isCustom)
        assertTrue(custom.uuid.isNotBlank())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `inhale duration cannot be zero`() {
        BreathingPattern("invalid", "Terlalu Cepat", 0, 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `exhale duration cannot exceed sixty seconds`() {
        BreathingPattern("invalid", "Terlalu Lama", 4, 61)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `name cannot be blank`() {
        BreathingPattern("custom:0", "   ", 4, 6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `name cannot exceed forty characters`() {
        BreathingPattern("custom:0", "A".repeat(41), 4, 6)
    }
}
