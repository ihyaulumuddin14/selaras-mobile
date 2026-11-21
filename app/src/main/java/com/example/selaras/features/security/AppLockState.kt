package com.example.selaras.features.security

/**
 * Operating mode for the App Lock screen.
 */
enum class AppLockMode {
    /** First-time setup: Create 6-digit PIN -> Confirm PIN -> Display Recovery Code */
    SETUP,

    /** Regular verification when launching or returning to the app */
    UNLOCK,

    /** Emergency recovery when user forgot PIN: Input Recovery Code -> Create New PIN -> Confirm -> Success */
    RESET
}

/**
 * Detailed step in the App Lock authentication lifecycle.
 */
enum class PinStep {
    /** Inputting 6-digit PIN to unlock */
    ENTER_PIN,

    /** Inputting initial 6-digit PIN during setup or reset */
    ENTER_NEW_PIN,

    /** Re-entering 6-digit PIN to confirm matching */
    CONFIRM_NEW_PIN,

    /** Displaying 16-character recovery code with copy & confirmation */
    SHOW_RECOVERY_CODE,

    /** Entering 16-character recovery code after forgetting PIN */
    ENTER_RECOVERY_CODE,

    /** PIN successfully verified */
    UNLOCK_SUCCESS,

    /** Setup successfully completed */
    SETUP_COMPLETED,

    /** PIN successfully reset via recovery code */
    RESET_SUCCESS
}

/**
 * UI State for App Lock, PIN authentication, and Recovery.
 */
data class AppLockUiState(
    val mode: AppLockMode = AppLockMode.UNLOCK,
    val pinStep: PinStep = PinStep.ENTER_PIN,
    val inputPin: String = "",
    val firstEnteredPin: String = "",
    val recoveryCode: String = "",
    val inputRecoveryCode: String = "",
    val isRecoverySavedChecked: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isCopiedToClipboard: Boolean = false
) {
    val isPinFull: Boolean get() = inputPin.length >= 6
    val currentPinLength: Int get() = inputPin.length
}
