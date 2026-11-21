package com.example.selaras

import com.example.selaras.domain.repository.SecurityRepository
import com.example.selaras.domain.usecase.security.CheckAppLockStatusUseCase
import com.example.selaras.domain.usecase.security.ResetPinWithRecoveryUseCase
import com.example.selaras.domain.usecase.security.SetupPinAndRecoveryUseCase
import com.example.selaras.domain.usecase.security.VerifyPinUseCase
import com.example.selaras.domain.usecase.security.VerifyRecoveryCodeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSecurityRepository : SecurityRepository {
    var storedPin: String? = null
    var storedRecoveryCode: String? = null

    override fun isPinSet(): Boolean = storedPin != null
    override fun isRecoveryConfigured(): Boolean = storedRecoveryCode != null

    override fun setupPinAndRecovery(pin: String): Result<String> {
        storedPin = pin
        val fakeCode = "TEST-RECO-VERY-CODE"
        storedRecoveryCode = fakeCode
        return Result.success(fakeCode)
    }

    override fun verifyPin(pin: String): Boolean = storedPin == pin

    override fun verifyRecoveryCode(code: String): Boolean {
        val sanitized = code.replace("-", "").replace(" ", "").trim()
        val storedSanitized = storedRecoveryCode?.replace("-", "")?.replace(" ", "")?.trim()
        return storedSanitized.equals(sanitized, ignoreCase = true)
    }

    override fun resetPinWithRecovery(recoveryCode: String, newPin: String): Result<Unit> {
        if (!verifyRecoveryCode(recoveryCode)) {
            return Result.failure(IllegalArgumentException("Kode tidak valid"))
        }
        storedPin = newPin
        return Result.success(Unit)
    }

    override fun clearSecurity(): Result<Unit> {
        storedPin = null
        storedRecoveryCode = null
        return Result.success(Unit)
    }
}

class SecurityUseCasesTest {

    private lateinit var fakeRepository: FakeSecurityRepository
    private lateinit var setupPinUseCase: SetupPinAndRecoveryUseCase
    private lateinit var verifyPinUseCase: VerifyPinUseCase
    private lateinit var verifyRecoveryCodeUseCase: VerifyRecoveryCodeUseCase
    private lateinit var resetPinUseCase: ResetPinWithRecoveryUseCase
    private lateinit var checkAppLockStatusUseCase: CheckAppLockStatusUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSecurityRepository()
        setupPinUseCase = SetupPinAndRecoveryUseCase(fakeRepository)
        verifyPinUseCase = VerifyPinUseCase(fakeRepository)
        verifyRecoveryCodeUseCase = VerifyRecoveryCodeUseCase(fakeRepository)
        resetPinUseCase = ResetPinWithRecoveryUseCase(fakeRepository)
        checkAppLockStatusUseCase = CheckAppLockStatusUseCase(fakeRepository)
    }

    @Test
    fun testSetupPinUseCaseValidation() {
        // Less than 6 digits fails
        val shortResult = setupPinUseCase("12345")
        assertTrue(shortResult.isFailure)

        // Non-digits fails
        val lettersResult = setupPinUseCase("12345a")
        assertTrue(lettersResult.isFailure)

        // Valid 6 digits succeeds
        val successResult = setupPinUseCase("123456")
        assertTrue(successResult.isSuccess)
        assertEquals("TEST-RECO-VERY-CODE", successResult.getOrNull())
        assertTrue(checkAppLockStatusUseCase())
    }

    @Test
    fun testVerifyPinUseCaseValidation() {
        setupPinUseCase("123456")

        // Incomplete PIN returns false without crashing
        assertFalse(verifyPinUseCase("12345"))
        assertFalse(verifyPinUseCase("1234567"))
        assertFalse(verifyPinUseCase("abcdef"))

        // Correct PIN
        assertTrue(verifyPinUseCase("123456"))
        assertFalse(verifyPinUseCase("654321"))
    }

    @Test
    fun testVerifyRecoveryCodeUseCaseValidation() {
        setupPinUseCase("123456")

        // Incomplete code returns false
        assertFalse(verifyRecoveryCodeUseCase("TEST-RECO"))

        // Valid 16-char code
        assertTrue(verifyRecoveryCodeUseCase("TEST-RECO-VERY-CODE"))
        assertTrue(verifyRecoveryCodeUseCase("test-reco-very-code"))
        assertFalse(verifyRecoveryCodeUseCase("WRON-GREC-OVER-YCOD"))
    }

    @Test
    fun testResetPinUseCaseValidation() {
        setupPinUseCase("123456")

        // Invalid code format fails
        val badCodeResult = resetPinUseCase("SHORT", "654321")
        assertTrue(badCodeResult.isFailure)

        // Invalid new PIN format fails
        val badPinResult = resetPinUseCase("TEST-RECO-VERY-CODE", "123")
        assertTrue(badPinResult.isFailure)

        // Valid reset
        val successResult = resetPinUseCase("TEST-RECO-VERY-CODE", "654321")
        assertTrue(successResult.isSuccess)
        assertTrue(verifyPinUseCase("654321"))
        assertFalse(verifyPinUseCase("123456"))
    }
}
