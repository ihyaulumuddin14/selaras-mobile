package com.example.selaras.features.security.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasHeader
import com.example.selaras.features.security.AppLockMode
import com.example.selaras.features.security.AppLockUiState
import com.example.selaras.features.security.PinStep
import com.example.selaras.features.security.components.PinDots
import com.example.selaras.features.security.components.PinKeypad
import com.example.selaras.features.security.components.SecurityIcons

/**
 * Screen for unlocking application with 6-digit PIN or verifying current PIN during PIN change.
 *
 * Requirements:
 * - 6-dot indicator.
 * - Numeric keypad.
 * - Immediate verification on 6th digit.
 * - Red dots & feedback on incorrect PIN.
 * - "Lupa PIN?" button leading to Recovery Code flow.
 * - Optional top header with back button when navigated from Settings.
 */
@Composable
fun AppLockScreen(
    state: AppLockUiState,
    onDigitPressed: (Int) -> Unit,
    onBackspace: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val background = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val errorRed = Color(0xFFB3261E)

    val isCurrentPinStep = state.pinStep == PinStep.ENTER_CURRENT_PIN || state.mode == AppLockMode.CHANGE_PIN

    val titleText = if (isCurrentPinStep) "Masukkan PIN Saat Ini" else "Buka Kunci Selaras"
    val subtitleText = if (isCurrentPinStep) {
        "Masukkan 6 digit PIN lama untuk melanjutkan perubahan PIN"
    } else {
        "Masukkan 6 digit PIN untuk membuka catatanmu"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section with optional SelarasHeader
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack != null) {
                SelarasHeader(
                    title = if (isCurrentPinStep) "Ubah PIN" else "Keamanan",
                    onBack = onBack,
                    showBackButton = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SecurityIcons.Lock,
                        contentDescription = null,
                        tint = pineGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = titleText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 6-dot visual indicator
                PinDots(
                    length = 6,
                    filledCount = state.currentPinLength,
                    isError = state.isError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error feedback
                if (state.isError && state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        fontSize = 14.sp,
                        color = errorRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Bottom Keypad & Forgot PIN
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PinKeypad(
                onDigitPressed = onDigitPressed,
                onBackspace = onBackspace,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Lupa PIN?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = pineGreen
                )
            }
        }
    }
}
