package com.example.selaras

import com.example.selaras.domain.usecase.security.CheckAppLockStatusUseCase
import com.example.selaras.domain.usecase.security.ResetPinWithRecoveryUseCase
import com.example.selaras.domain.usecase.security.SetupPinAndRecoveryUseCase
import com.example.selaras.domain.usecase.security.VerifyPinUseCase
import com.example.selaras.domain.usecase.security.VerifyRecoveryCodeUseCase
import com.example.selaras.features.security.AppLockMode
import com.example.selaras.features.security.AppLockViewModel
import com.example.selaras.features.security.PinStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSecurityRepository
    private lateinit var viewModel: AppLockViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSecurityRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(initialMode: AppLockMode? = null): AppLockViewModel {
        return AppLockViewModel(
            setupPinAndRecoveryUseCase = SetupPinAndRecoveryUseCase(fakeRepository),
            verifyPinUseCase = VerifyPinUseCase(fakeRepository),
            verifyRecoveryCodeUseCase = VerifyRecoveryCodeUseCase(fakeRepository),
            resetPinWithRecoveryUseCase = ResetPinWithRecoveryUseCase(fakeRepository),
            checkAppLockStatusUseCase = CheckAppLockStatusUseCase(fakeRepository),
            securityRepository = fakeRepository,
            initialMode = initialMode
        )
    }

    @Test
    fun testInitialStateWhenPinNotSetDefaultsToSetupMode() {
        viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(AppLockMode.SETUP, state.mode)
        assertEquals(PinStep.ENTER_NEW_PIN, state.pinStep)
        assertEquals("", state.inputPin)
        assertFalse(state.isError)
    }

    @Test
    fun testInitialStateWhenPinAlreadySetDefaultsToUnlockMode() {
        fakeRepository.storedPin = "123456"
        viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(AppLockMode.UNLOCK, state.mode)
        assertEquals(PinStep.ENTER_PIN, state.pinStep)
    }

    @Test
    fun testSetupPinFlowSuccess() = runTest {
        viewModel = createViewModel(AppLockMode.SETUP)

        // 1. Enter 6 digits for new PIN: 1, 2, 3, 4, 5, 6
        listOf(1, 2, 3, 4, 5, 6).forEach { viewModel.onDigitPressed(it) }

        // Automatically transitions to CONFIRM_NEW_PIN
        assertEquals(PinStep.CONFIRM_NEW_PIN, viewModel.uiState.value.pinStep)
        assertEquals("", viewModel.uiState.value.inputPin)
        assertEquals("123456", viewModel.uiState.value.firstEnteredPin)

        // 2. Enter matching 6 digits for confirmation: 1, 2, 3, 4, 5, 6
        listOf(1, 2, 3, 4, 5, 6).forEach { viewModel.onDigitPressed(it) }
        advanceUntilIdle()

        // Automatically saves and displays Recovery Code
        assertEquals(PinStep.SHOW_RECOVERY_CODE, viewModel.uiState.value.pinStep)
        assertEquals("TEST-RECO-VERY-CODE", viewModel.uiState.value.recoveryCode)
        assertFalse(viewModel.uiState.value.isError)

        // 3. User copies code and checks agreement
        viewModel.onCopyRecoveryCode()
        assertTrue(viewModel.uiState.value.isCopiedToClipboard)

        viewModel.onRecoverySavedChecked(true)
        assertTrue(viewModel.uiState.value.isRecoverySavedChecked)

        // 4. User completes setup
        viewModel.completeSetup()
        assertEquals(PinStep.SETUP_COMPLETED, viewModel.uiState.value.pinStep)
    }

    @Test
    fun testSetupPinMismatchResetsToEnterNewPin() = runTest {
        viewModel = createViewModel(AppLockMode.SETUP)

        // Enter initial PIN: 1, 2, 3, 4, 5, 6
        listOf(1, 2, 3, 4, 5, 6).forEach { viewModel.onDigitPressed(it) }
        assertEquals(PinStep.CONFIRM_NEW_PIN, viewModel.uiState.value.pinStep)

        // Enter mismatching PIN: 6, 5, 4, 3, 2, 1
        listOf(6, 5, 4, 3, 2, 1).forEach { viewModel.onDigitPressed(it) }

        // Mismatch sets error and resets back to ENTER_NEW_PIN
        assertEquals(PinStep.ENTER_NEW_PIN, viewModel.uiState.value.pinStep)
        assertTrue(viewModel.uiState.value.isError)
        assertEquals("PIN tidak cocok, silakan ulangi", viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.inputPin)
        assertEquals("", viewModel.uiState.value.firstEnteredPin)
    }

    @Test
    fun testUnlockPinSuccess() = runTest {
        fakeRepository.storedPin = "112233"
        viewModel = createViewModel(AppLockMode.UNLOCK)

        // Enter correct PIN: 1, 1, 2, 2, 3, 3
        listOf(1, 1, 2, 2, 3, 3).forEach { viewModel.onDigitPressed(it) }

        assertEquals(PinStep.UNLOCK_SUCCESS, viewModel.uiState.value.pinStep)
        assertFalse(viewModel.uiState.value.isError)
    }

    @Test
    fun testUnlockPinFailure() = runTest {
        fakeRepository.storedPin = "112233"
        viewModel = createViewModel(AppLockMode.UNLOCK)

        // Enter wrong PIN: 9, 9, 9, 9, 9, 9
        listOf(9, 9, 9, 9, 9, 9).forEach { viewModel.onDigitPressed(it) }

        assertEquals(PinStep.ENTER_PIN, viewModel.uiState.value.pinStep)
        assertTrue(viewModel.uiState.value.isError)
        assertEquals("PIN salah. Coba lagi", viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.inputPin)
    }

    @Test
    fun testRecoveryFlowAndResetPin() = runTest {
        fakeRepository.storedPin = "112233"
        fakeRepository.storedRecoveryCode = "ABCD-EFGH-JKLM-NPQR"
        viewModel = createViewModel(AppLockMode.UNLOCK)

        // 1. User clicks "Lupa PIN?"
        viewModel.switchToRecoveryMode()
        assertEquals(AppLockMode.RESET, viewModel.uiState.value.mode)
        assertEquals(PinStep.ENTER_RECOVERY_CODE, viewModel.uiState.value.pinStep)

        // 2. Input invalid recovery code
        viewModel.onRecoveryCodeInputChanged("WRO1-NGCO-DE12-3456")
        viewModel.verifyEnteredRecoveryCode()
        assertTrue(viewModel.uiState.value.isError)
        assertEquals(PinStep.ENTER_RECOVERY_CODE, viewModel.uiState.value.pinStep)

        // 3. Input valid recovery code
        viewModel.onRecoveryCodeInputChanged("ABCD-EFGH-JKLM-NPQR")
        viewModel.verifyEnteredRecoveryCode()
        assertFalse(viewModel.uiState.value.isError)
        assertEquals(PinStep.ENTER_NEW_PIN, viewModel.uiState.value.pinStep)

        // 4. Enter new PIN: 7, 7, 8, 8, 9, 9
        listOf(7, 7, 8, 8, 9, 9).forEach { viewModel.onDigitPressed(it) }
        assertEquals(PinStep.CONFIRM_NEW_PIN, viewModel.uiState.value.pinStep)

        // 5. Confirm new PIN: 7, 7, 8, 8, 9, 9
        listOf(7, 7, 8, 8, 9, 9).forEach { viewModel.onDigitPressed(it) }
        advanceUntilIdle()

        // Successfully updated
        assertEquals(PinStep.RESET_SUCCESS, viewModel.uiState.value.pinStep)
        assertEquals("778899", fakeRepository.storedPin)
    }

    @Test
    fun testKeypadOperations() {
        viewModel = createViewModel(AppLockMode.SETUP)

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        assertEquals("123", viewModel.uiState.value.inputPin)

        viewModel.onBackspace()
        assertEquals("12", viewModel.uiState.value.inputPin)

        viewModel.onClearPin()
        assertEquals("", viewModel.uiState.value.inputPin)
    }

    @Test
    fun testHandleBack() {
        viewModel = createViewModel(AppLockMode.SETUP)

        // Enter 6 digits to go to confirm step
        listOf(1, 2, 3, 4, 5, 6).forEach { viewModel.onDigitPressed(it) }
        assertEquals(PinStep.CONFIRM_NEW_PIN, viewModel.uiState.value.pinStep)

        // Handle back returns to ENTER_NEW_PIN
        val handled = viewModel.handleBack()
        assertTrue(handled)
        assertEquals(PinStep.ENTER_NEW_PIN, viewModel.uiState.value.pinStep)
        assertEquals("", viewModel.uiState.value.inputPin)
    }

    @Test
    fun testChangePinFlowSuccess() = runTest {
        fakeRepository.storedPin = "112233"
        viewModel = createViewModel(AppLockMode.CHANGE_PIN)

        // 1. Initial state is ENTER_CURRENT_PIN
        val state = viewModel.uiState.value
        assertEquals(AppLockMode.CHANGE_PIN, state.mode)
        assertEquals(PinStep.ENTER_CURRENT_PIN, state.pinStep)

        // 2. Input wrong current PIN
        listOf(9, 9, 9, 9, 9, 9).forEach { viewModel.onDigitPressed(it) }
        assertTrue(viewModel.uiState.value.isError)
        assertEquals("PIN saat ini salah. Coba lagi", viewModel.uiState.value.errorMessage)
        assertEquals(PinStep.ENTER_CURRENT_PIN, viewModel.uiState.value.pinStep)

        // 3. Input correct current PIN
        listOf(1, 1, 2, 2, 3, 3).forEach { viewModel.onDigitPressed(it) }
        assertFalse(viewModel.uiState.value.isError)
        assertEquals(PinStep.ENTER_NEW_PIN, viewModel.uiState.value.pinStep)
        assertEquals("112233", viewModel.uiState.value.verifiedCurrentPin)

        // 4. Enter new PIN
        listOf(6, 5, 4, 3, 2, 1).forEach { viewModel.onDigitPressed(it) }
        assertEquals(PinStep.CONFIRM_NEW_PIN, viewModel.uiState.value.pinStep)
        assertEquals("654321", viewModel.uiState.value.firstEnteredPin)

        // 5. Confirm matching new PIN
        listOf(6, 5, 4, 3, 2, 1).forEach { viewModel.onDigitPressed(it) }
        advanceUntilIdle()

        // 6. Successfully updated
        assertEquals(PinStep.RESET_SUCCESS, viewModel.uiState.value.pinStep)
        assertEquals("654321", fakeRepository.storedPin)
    }

    @Test
    fun testChangePinForgotPasswordRecoveryAndCancel() = runTest {
        fakeRepository.storedPin = "112233"
        fakeRepository.storedRecoveryCode = "ABCD-EFGH-JKLM-NPQR"
        viewModel = createViewModel(AppLockMode.CHANGE_PIN)

        // User is at ENTER_CURRENT_PIN, clicks "Lupa PIN?"
        viewModel.switchToRecoveryMode()
        assertEquals(AppLockMode.RESET, viewModel.uiState.value.mode)
        assertEquals(PinStep.ENTER_RECOVERY_CODE, viewModel.uiState.value.pinStep)
        assertEquals(AppLockMode.CHANGE_PIN, viewModel.uiState.value.previousMode)

        // Cancel returns to CHANGE_PIN mode
        viewModel.onCancelRecovery()
        assertEquals(AppLockMode.CHANGE_PIN, viewModel.uiState.value.mode)
        assertEquals(PinStep.ENTER_CURRENT_PIN, viewModel.uiState.value.pinStep)

        // Click "Lupa PIN?" again and complete recovery
        viewModel.switchToRecoveryMode()
        viewModel.onRecoveryCodeInputChanged("ABCD-EFGH-JKLM-NPQR")
        viewModel.verifyEnteredRecoveryCode()
        assertEquals(PinStep.ENTER_NEW_PIN, viewModel.uiState.value.pinStep)

        listOf(7, 7, 8, 8, 9, 9).forEach { viewModel.onDigitPressed(it) }
        assertEquals(PinStep.CONFIRM_NEW_PIN, viewModel.uiState.value.pinStep)

        listOf(7, 7, 8, 8, 9, 9).forEach { viewModel.onDigitPressed(it) }
        advanceUntilIdle()

        assertEquals(PinStep.RESET_SUCCESS, viewModel.uiState.value.pinStep)
        assertEquals("778899", fakeRepository.storedPin)
    }

    @Test
    fun testChangePinHandleBack() {
        fakeRepository.storedPin = "112233"
        viewModel = createViewModel(AppLockMode.CHANGE_PIN)

        // At ENTER_CURRENT_PIN: not consumed internally -> false (exits to Settings)
        assertFalse(viewModel.handleBack())

        // Move to ENTER_NEW_PIN
        listOf(1, 1, 2, 2, 3, 3).forEach { viewModel.onDigitPressed(it) }
        assertEquals(PinStep.ENTER_NEW_PIN, viewModel.uiState.value.pinStep)

        // At ENTER_NEW_PIN: handleBack returns to ENTER_CURRENT_PIN
        assertTrue(viewModel.handleBack())
        assertEquals(PinStep.ENTER_CURRENT_PIN, viewModel.uiState.value.pinStep)
    }
}
