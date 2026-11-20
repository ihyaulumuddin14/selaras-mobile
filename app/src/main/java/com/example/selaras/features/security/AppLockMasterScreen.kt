package com.example.selaras.features.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.selaras.features.security.screens.AppLockScreen
import com.example.selaras.features.security.screens.PinRecoveryScreen
import com.example.selaras.features.security.screens.PinResetSuccessScreen
import com.example.selaras.features.security.screens.PinSetupScreen
import com.example.selaras.features.security.screens.RecoveryCodeDisplayScreen

/**
 * Master container orchestrating App Lock, PIN Setup, and Emergency Recovery screens.
 */
@Composable
fun AppLockMasterScreen(
    viewModel: AppLockViewModel,
    onUnlockSuccess: () -> Unit,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onBackToPrevious: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    // Handle system back gestures
    BackHandler {
        val handled = viewModel.handleBack()
        if (!handled && onBackToPrevious != null) {
            onBackToPrevious()
        }
    }

    // Trigger success callback when unlocked
    LaunchedEffect(state.pinStep) {
        if (state.pinStep == PinStep.UNLOCK_SUCCESS) {
            onUnlockSuccess()
        } else if (state.pinStep == PinStep.SETUP_COMPLETED) {
            onSetupComplete()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state.pinStep) {
            PinStep.ENTER_PIN -> {
                AppLockScreen(
                    state = state,
                    onDigitPressed = viewModel::onDigitPressed,
                    onBackspace = viewModel::onBackspace,
                    onForgotPassword = viewModel::switchToRecoveryMode
                )
            }

            PinStep.ENTER_NEW_PIN,
            PinStep.CONFIRM_NEW_PIN -> {
                PinSetupScreen(
                    state = state,
                    onDigitPressed = viewModel::onDigitPressed,
                    onBackspace = viewModel::onBackspace
                )
            }

            PinStep.SHOW_RECOVERY_CODE -> {
                RecoveryCodeDisplayScreen(
                    state = state,
                    onCopyCode = viewModel::onCopyRecoveryCode,
                    onCheckSaved = viewModel::onRecoverySavedChecked,
                    onComplete = viewModel::completeSetup
                )
            }

            PinStep.ENTER_RECOVERY_CODE -> {
                PinRecoveryScreen(
                    state = state,
                    onCodeChanged = viewModel::onRecoveryCodeInputChanged,
                    onVerifyCode = viewModel::verifyEnteredRecoveryCode,
                    onCancel = viewModel::switchToUnlockMode
                )
            }

            PinStep.RESET_SUCCESS -> {
                PinResetSuccessScreen(
                    onContinue = onUnlockSuccess
                )
            }

            PinStep.UNLOCK_SUCCESS -> {
                // Screen transition handled by LaunchedEffect
            }

            PinStep.SETUP_COMPLETED -> {
                // Screen transition handled by LaunchedEffect
            }
        }
    }
}
