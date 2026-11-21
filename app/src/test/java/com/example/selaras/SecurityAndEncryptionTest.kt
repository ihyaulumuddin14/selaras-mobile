package com.example.selaras

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.security.PinManager
import com.example.selaras.core.security.RecoveryManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
class SecurityAndEncryptionTest {

    private lateinit var context: Context
    private lateinit var pinManager: PinManager
    private lateinit var recoveryManager: RecoveryManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinManager = PinManager(context)
        recoveryManager = RecoveryManager(context)
    }

    @Test
    fun testPinManagerLifecycle() {
        // Initially no PIN is set
        assertFalse(pinManager.isPinSet())

        // Set valid 6-digit PIN
        val success = pinManager.setPin("123456")
        assertTrue(success)
        assertTrue(pinManager.isPinSet())

        // Verify with correct PIN
        assertTrue(pinManager.verifyPin("123456"))

        // Verify with incorrect PIN
        assertFalse(pinManager.verifyPin("654321"))
        assertFalse(pinManager.verifyPin("000000"))

        // Reject short PIN
        assertFalse(pinManager.setPin("12"))

        // Clear PIN
        pinManager.clearPin()
        assertFalse(pinManager.isPinSet())
    }

    @Test
    fun testRecoveryManagerSetupAndRecover() {
        val originalDek = ByteArray(32).also { SecureRandom().nextBytes(it) }

        // Setup recovery
        val recoveryCode = recoveryManager.setupRecovery(originalDek)
        assertTrue(recoveryManager.isRecoveryConfigured())
        assertNotNull(recoveryCode)

        // Code format must be 4 groups of 4 characters: XXXX-XXXX-XXXX-XXXX
        val parts = recoveryCode.split("-")
        assertEquals(4, parts.size)
        parts.forEach { assertEquals(4, it.length) }

        // Recover with correct code
        val recoveredDek = recoveryManager.recoverDatabaseKey(recoveryCode)
        assertNotNull(recoveredDek)
        assertArrayEquals(originalDek, recoveredDek)

        // Recover with normalized code (lowercase, with spaces)
        val messyInput = recoveryCode.lowercase().replace("-", " ")
        val recoveredMessy = recoveryManager.recoverDatabaseKey(messyInput)
        assertNotNull(recoveredMessy)
        assertArrayEquals(originalDek, recoveredMessy)

        // Fails with wrong code
        val wrongCode = "AAAA-BBBB-CCCC-DDDD"
        val failedRecovery = recoveryManager.recoverDatabaseKey(wrongCode)
        assertNull(failedRecovery)

        // Clear recovery
        recoveryManager.clearRecovery()
        assertFalse(recoveryManager.isRecoveryConfigured())
    }
}
