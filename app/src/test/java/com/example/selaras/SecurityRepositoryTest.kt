package com.example.selaras

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.security.KeyManager
import com.example.selaras.core.security.PinManager
import com.example.selaras.core.security.RecoveryManager
import com.example.selaras.data.repository.SecurityRepositoryImpl
import com.example.selaras.domain.repository.SecurityRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.SecureRandom

class TestKeyManager(context: Context) : KeyManager(context) {
    private var dek: ByteArray? = null

    override fun getOrCreateDatabaseKey(): ByteArray {
        if (dek == null) {
            dek = ByteArray(32).also { SecureRandom().nextBytes(it) }
        }
        return dek!!
    }

    override fun saveDekProtected(dek: ByteArray) {
        this.dek = dek
    }

    override fun hasDatabaseKey(): Boolean {
        return dek != null
    }
}

@RunWith(RobolectricTestRunner::class)
class SecurityRepositoryTest {

    private lateinit var context: Context
    private lateinit var pinManager: PinManager
    private lateinit var recoveryManager: RecoveryManager
    private lateinit var keyManager: KeyManager
    private lateinit var securityRepository: SecurityRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinManager = PinManager(context)
        recoveryManager = RecoveryManager(context)
        keyManager = TestKeyManager(context)

        securityRepository = SecurityRepositoryImpl(
            pinManager = pinManager,
            recoveryManager = recoveryManager,
            keyManager = keyManager
        )

        // Ensure clean state before each test
        securityRepository.clearSecurity()
    }

    @Test
    fun testSetupPinAndRecoverySuccess() {
        assertFalse(securityRepository.isPinSet())
        assertFalse(securityRepository.isRecoveryConfigured())

        val result = securityRepository.setupPinAndRecovery("123456")
        assertTrue(result.isSuccess)

        val recoveryCode = result.getOrNull()
        assertNotNull(recoveryCode)

        // Code format must be 4 groups of 4 characters: XXXX-XXXX-XXXX-XXXX
        val segments = recoveryCode!!.split("-")
        assertEquals(4, segments.size)
        segments.forEach { assertEquals(4, it.length) }

        assertTrue(securityRepository.isPinSet())
        assertTrue(securityRepository.isRecoveryConfigured())
        assertTrue(securityRepository.verifyPin("123456"))
    }

    @Test
    fun testSetupPinRejectsInvalidLengthOrNonDigits() {
        val shortResult = securityRepository.setupPinAndRecovery("123")
        assertTrue(shortResult.isFailure)

        val longResult = securityRepository.setupPinAndRecovery("1234567")
        assertTrue(longResult.isFailure)

        val lettersResult = securityRepository.setupPinAndRecovery("abcdef")
        assertTrue(lettersResult.isFailure)

        assertFalse(securityRepository.isPinSet())
    }

    @Test
    fun testVerifyPin() {
        securityRepository.setupPinAndRecovery("654321")

        assertTrue(securityRepository.verifyPin("654321"))
        assertFalse(securityRepository.verifyPin("123456"))
        assertFalse(securityRepository.verifyPin("000000"))
        assertFalse(securityRepository.verifyPin("65432"))
    }

    @Test
    fun testVerifyRecoveryCode() {
        val setupResult = securityRepository.setupPinAndRecovery("987654")
        val recoveryCode = setupResult.getOrThrow()

        // Valid with exact format
        assertTrue(securityRepository.verifyRecoveryCode(recoveryCode))

        // Valid when normalized (lowercase, spaces instead of hyphens)
        val normalized = recoveryCode.lowercase().replace("-", " ")
        assertTrue(securityRepository.verifyRecoveryCode(normalized))

        // Invalid code
        assertFalse(securityRepository.verifyRecoveryCode("AAAA-BBBB-CCCC-DDDD"))
    }

    @Test
    fun testResetPinWithRecoverySuccess() {
        val setupResult = securityRepository.setupPinAndRecovery("112233")
        val recoveryCode = setupResult.getOrThrow()

        // Reset to new PIN
        val resetResult = securityRepository.resetPinWithRecovery(recoveryCode, "998877")
        assertTrue(resetResult.isSuccess)

        // Old PIN no longer valid
        assertFalse(securityRepository.verifyPin("112233"))

        // New PIN is active
        assertTrue(securityRepository.verifyPin("998877"))

        // DEK is preserved
        assertTrue(keyManager.hasDatabaseKey())
    }

    @Test
    fun testResetPinWithInvalidRecoveryCodeFails() {
        securityRepository.setupPinAndRecovery("112233")

        val resetResult = securityRepository.resetPinWithRecovery("INVALID-CODE-1234", "998877")
        assertTrue(resetResult.isFailure)

        // Original PIN still intact
        assertTrue(securityRepository.verifyPin("112233"))
        assertFalse(securityRepository.verifyPin("998877"))
    }

    @Test
    fun testClearSecurity() {
        securityRepository.setupPinAndRecovery("123456")
        assertTrue(securityRepository.isPinSet())

        securityRepository.clearSecurity()
        assertFalse(securityRepository.isPinSet())
        assertFalse(securityRepository.isRecoveryConfigured())
    }
}
