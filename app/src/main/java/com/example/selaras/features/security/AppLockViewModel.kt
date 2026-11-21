package com.example.selaras.features.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.repository.SecurityRepository
import com.example.selaras.domain.usecase.security.ChangePinUseCase
import com.example.selaras.domain.usecase.security.CheckAppLockStatusUseCase
import com.example.selaras.domain.usecase.security.ResetPinWithRecoveryUseCase
import com.example.selaras.domain.usecase.security.SetupPinAndRecoveryUseCase
import com.example.selaras.domain.usecase.security.VerifyPinUseCase
import com.example.selaras.domain.usecase.security.VerifyRecoveryCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the state of App Lock, PIN authentication, and Recovery flows.
 */
class AppLockViewModel(
    private val setupPinAndRecoveryUseCase: SetupPinAndRecoveryUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val verifyRecoveryCodeUseCase: VerifyRecoveryCodeUseCase,
    private val resetPinWithRecoveryUseCase: ResetPinWithRecoveryUseCase,
    private val checkAppLockStatusUseCase: CheckAppLockStatusUseCase,
    private val securityRepository: SecurityRepository,
    private val changePinUseCase: ChangePinUseCase = ChangePinUseCase(securityRepository),
    initialMode: AppLockMode? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        createInitialState(initialMode ?: if (checkAppLockStatusUseCase()) AppLockMode.UNLOCK else AppLockMode.SETUP)
    )
    val uiState = _uiState.asStateFlow()

    private fun createInitialState(mode: AppLockMode): AppLockUiState {
        return when (mode) {
            AppLockMode.SETUP -> AppLockUiState(
                mode = AppLockMode.SETUP,
                pinStep = PinStep.ENTER_NEW_PIN
            )
            AppLockMode.UNLOCK -> AppLockUiState(
                mode = AppLockMode.UNLOCK,
                pinStep = PinStep.ENTER_PIN
            )
            AppLockMode.RESET -> AppLockUiState(
                mode = AppLockMode.RESET,
                pinStep = PinStep.ENTER_RECOVERY_CODE
            )
            AppLockMode.CHANGE_PIN -> AppLockUiState(
                mode = AppLockMode.CHANGE_PIN,
                pinStep = PinStep.ENTER_CURRENT_PIN
            )
        }
    }

    /**
     * Handles numeric keypad input (0..9). Automatically processes when reaching 6 digits.
     */
    fun onDigitPressed(digit: Int) {
        val currentInput = _uiState.value.inputPin
        if (currentInput.length >= 6) return

        val newInput = currentInput + digit.toString()
        _uiState.update { it.copy(inputPin = newInput, isError = false, errorMessage = null) }

        if (newInput.length == 6) {
            processPinEntry(newInput)
        }
    }

    /**
     * Handles backspace button.
     */
    fun onBackspace() {
        val currentInput = _uiState.value.inputPin
        if (currentInput.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    inputPin = currentInput.dropLast(1),
                    isError = false,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Clears entered PIN.
     */
    fun onClearPin() {
        _uiState.update { it.copy(inputPin = "", isError = false, errorMessage = null) }
    }

    private fun processPinEntry(pin: String) {
        when (_uiState.value.pinStep) {
            PinStep.ENTER_PIN -> {
                // Unlock mode verification
                val isValid = verifyPinUseCase(pin)
                if (isValid) {
                    _uiState.update {
                        it.copy(
                            pinStep = PinStep.UNLOCK_SUCCESS,
                            isError = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            inputPin = "",
                            isError = true,
                            errorMessage = "PIN salah. Coba lagi"
                        )
                    }
                }
            }

            PinStep.ENTER_CURRENT_PIN -> {
                // Change PIN mode: verify current PIN first
                val isValid = verifyPinUseCase(pin)
                if (isValid) {
                    _uiState.update {
                        it.copy(
                            verifiedCurrentPin = pin,
                            inputPin = "",
                            pinStep = PinStep.ENTER_NEW_PIN,
                            isError = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            inputPin = "",
                            isError = true,
                            errorMessage = "PIN saat ini salah. Coba lagi"
                        )
                    }
                }
            }

            PinStep.ENTER_NEW_PIN -> {
                // Setup, Reset, or Change PIN mode initial PIN entry -> advance to confirmation
                _uiState.update {
                    it.copy(
                        firstEnteredPin = pin,
                        inputPin = "",
                        pinStep = PinStep.CONFIRM_NEW_PIN,
                        isError = false,
                        errorMessage = null
                    )
                }
            }

            PinStep.CONFIRM_NEW_PIN -> {
                // Confirm matching PIN
                val firstPin = _uiState.value.firstEnteredPin
                if (pin == firstPin) {
                    when (_uiState.value.mode) {
                        AppLockMode.SETUP -> completePinSetup(pin)
                        AppLockMode.RESET -> completePinReset(pin)
                        AppLockMode.CHANGE_PIN -> completePinChange(pin)
                        else -> Unit
                    }
                } else {
                    // Mismatch: reset back to ENTER_NEW_PIN (Figma 173:2361)
                    _uiState.update {
                        it.copy(
                            inputPin = "",
                            firstEnteredPin = "",
                            pinStep = PinStep.ENTER_NEW_PIN,
                            isError = true,
                            errorMessage = "PIN tidak cocok, silakan ulangi"
                        )
                    }
                }
            }

            else -> Unit
        }
    }

    private fun completePinSetup(pin: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = setupPinAndRecoveryUseCase(pin)
            result.fold(
                onSuccess = { recoveryCode ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recoveryCode = recoveryCode,
                            inputPin = "",
                            pinStep = PinStep.SHOW_RECOVERY_CODE,
                            isError = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inputPin = "",
                            firstEnteredPin = "",
                            pinStep = PinStep.ENTER_NEW_PIN,
                            isError = true,
                            errorMessage = error.message ?: "Gagal menyimpan PIN. Silakan coba lagi."
                        )
                    }
                }
            )
        }
    }

    private fun completePinReset(newPin: String) {
        val recoveryCode = _uiState.value.inputRecoveryCode
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = resetPinWithRecoveryUseCase(recoveryCode, newPin)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inputPin = "",
                            pinStep = PinStep.RESET_SUCCESS,
                            isError = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inputPin = "",
                            firstEnteredPin = "",
                            pinStep = PinStep.ENTER_NEW_PIN,
                            isError = true,
                            errorMessage = error.message ?: "Gagal memperbarui PIN"
                        )
                    }
                }
            )
        }
    }

    private fun completePinChange(newPin: String) {
        val currentPin = _uiState.value.verifiedCurrentPin
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = changePinUseCase(currentPin, newPin)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inputPin = "",
                            firstEnteredPin = "",
                            verifiedCurrentPin = "",
                            pinStep = PinStep.RESET_SUCCESS,
                            isError = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inputPin = "",
                            firstEnteredPin = "",
                            pinStep = PinStep.ENTER_NEW_PIN,
                            isError = true,
                            errorMessage = error.message ?: "Gagal memperbarui PIN"
                        )
                    }
                }
            )
        }
    }

    /**
     * Handles text input for the 16-character recovery code in RESET mode.
     * Automatically strips invalid characters and formats with hyphens: XXXX-XXXX-XXXX-XXXX.
     */
    fun onRecoveryCodeInputChanged(rawText: String) {
        val clean = rawText.filter { it.isLetterOrDigit() }.uppercase().take(16)
        val formatted = clean.chunked(4).joinToString("-")
        _uiState.update {
            it.copy(
                inputRecoveryCode = formatted,
                isError = false,
                errorMessage = null
            )
        }
    }

    /**
     * Verifies the entered recovery code.
     */
    fun verifyEnteredRecoveryCode() {
        val inputCode = _uiState.value.inputRecoveryCode
        val clean = inputCode.replace("-", "").trim()
        if (clean.length != 16) {
            _uiState.update {
                it.copy(
                    isError = true,
                    errorMessage = "Kode pemulihan harus terdiri dari 16 karakter"
                )
            }
            return
        }

        val isValid = verifyRecoveryCodeUseCase(inputCode)
        if (isValid) {
            _uiState.update {
                it.copy(
                    pinStep = PinStep.ENTER_NEW_PIN,
                    inputPin = "",
                    firstEnteredPin = "",
                    isError = false,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isError = true,
                    errorMessage = "Kode pemulihan tidak valid atau tidak cocok"
                )
            }
        }
    }

    /**
     * Toggles checkbox confirmation on the recovery display screen.
     */
    fun onRecoverySavedChecked(checked: Boolean) {
        _uiState.update { it.copy(isRecoverySavedChecked = checked) }
    }

    /**
     * Marks recovery code as copied to clipboard.
     */
    fun onCopyRecoveryCode() {
        _uiState.update { it.copy(isCopiedToClipboard = true) }
    }

    /**
     * Resets copy feedback state.
     */
    fun dismissCopyFeedback() {
        _uiState.update { it.copy(isCopiedToClipboard = false) }
    }

    /**
     * Completes setup and triggers transition to home/dashboard.
     */
    fun completeSetup() {
        _uiState.update { it.copy(pinStep = PinStep.SETUP_COMPLETED) }
    }

    /**
     * Switches to emergency recovery mode when user forgets PIN.
     */
    fun switchToRecoveryMode() {
        val currentMode = _uiState.value.mode
        _uiState.update {
            it.copy(
                mode = AppLockMode.RESET,
                previousMode = if (currentMode != AppLockMode.RESET) currentMode else it.previousMode,
                pinStep = PinStep.ENTER_RECOVERY_CODE,
                inputPin = "",
                firstEnteredPin = "",
                inputRecoveryCode = "",
                isError = false,
                errorMessage = null
            )
        }
    }

    /**
     * Switches to normal unlock mode.
     */
    fun switchToUnlockMode() {
        _uiState.update {
            it.copy(
                mode = AppLockMode.UNLOCK,
                previousMode = null,
                pinStep = PinStep.ENTER_PIN,
                inputPin = "",
                firstEnteredPin = "",
                isError = false,
                errorMessage = null
            )
        }
    }

    /**
     * Switches to setup mode.
     */
    fun switchToSetupMode() {
        _uiState.update {
            it.copy(
                mode = AppLockMode.SETUP,
                previousMode = null,
                pinStep = PinStep.ENTER_NEW_PIN,
                inputPin = "",
                firstEnteredPin = "",
                isError = false,
                errorMessage = null
            )
        }
    }

    /**
     * Switches to Change PIN mode (from Settings).
     * Prompts user to verify their current PIN first, with an option to recover if forgotten.
     */
    fun switchToChangePinMode() {
        _uiState.update {
            it.copy(
                mode = AppLockMode.CHANGE_PIN,
                previousMode = null,
                pinStep = PinStep.ENTER_CURRENT_PIN,
                inputPin = "",
                firstEnteredPin = "",
                verifiedCurrentPin = "",
                inputRecoveryCode = "",
                isError = false,
                errorMessage = null
            )
        }
    }

    /**
     * Cancels recovery flow and returns to either Change PIN mode or Unlock mode.
     */
    fun onCancelRecovery() {
        if (_uiState.value.previousMode == AppLockMode.CHANGE_PIN) {
            switchToChangePinMode()
        } else {
            switchToUnlockMode()
        }
    }

    /**
     * Handles system back or UI back button based on current step.
     * @return True if back was consumed internally, false if it should exit/propagate.
     */
    fun handleBack(): Boolean {
        val state = _uiState.value
        return when (state.pinStep) {
            PinStep.CONFIRM_NEW_PIN -> {
                _uiState.update {
                    it.copy(
                        pinStep = PinStep.ENTER_NEW_PIN,
                        inputPin = "",
                        firstEnteredPin = "",
                        isError = false,
                        errorMessage = null
                    )
                }
                true
            }
            PinStep.ENTER_RECOVERY_CODE -> {
                onCancelRecovery()
                true
            }
            PinStep.ENTER_NEW_PIN -> {
                when (state.mode) {
                    AppLockMode.RESET -> {
                        _uiState.update {
                            it.copy(
                                pinStep = PinStep.ENTER_RECOVERY_CODE,
                                inputPin = "",
                                firstEnteredPin = "",
                                isError = false,
                                errorMessage = null
                            )
                        }
                        true
                    }
                    AppLockMode.CHANGE_PIN -> {
                        switchToChangePinMode()
                        true
                    }
                    else -> false
                }
            }
            PinStep.ENTER_CURRENT_PIN -> false
            else -> false
        }
    }

    /**
     * Clears security preferences (for testing purposes).
     */
    fun restartSecurityForTesting() {
        securityRepository.clearSecurity()
        _uiState.update {
            AppLockUiState(
                mode = AppLockMode.SETUP,
                pinStep = PinStep.ENTER_NEW_PIN
            )
        }
    }

    class Factory(
        private val securityRepository: SecurityRepository,
        private val initialMode: AppLockMode? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppLockViewModel::class.java))
            val setupPin = SetupPinAndRecoveryUseCase(securityRepository)
            val verifyPin = VerifyPinUseCase(securityRepository)
            val verifyRecovery = VerifyRecoveryCodeUseCase(securityRepository)
            val resetPin = ResetPinWithRecoveryUseCase(securityRepository)
            val checkStatus = CheckAppLockStatusUseCase(securityRepository)
            val changePin = ChangePinUseCase(securityRepository)
            @Suppress("UNCHECKED_CAST")
            return AppLockViewModel(
                setupPinAndRecoveryUseCase = setupPin,
                verifyPinUseCase = verifyPin,
                verifyRecoveryCodeUseCase = verifyRecovery,
                resetPinWithRecoveryUseCase = resetPin,
                checkAppLockStatusUseCase = checkStatus,
                securityRepository = securityRepository,
                changePinUseCase = changePin,
                initialMode = initialMode
            ) as T
        }
    }
}
