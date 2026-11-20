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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.features.security.AppLockUiState
import com.example.selaras.features.security.PinStep
import com.example.selaras.features.security.components.PinDots
import com.example.selaras.features.security.components.PinKeypad
import com.example.selaras.features.security.components.SecurityIcons

/**
 * Screen for establishing and confirming 6-digit PIN (Figma 173:2307, 173:2313, 168:2346).
 */
@Composable
fun PinSetupScreen(
    state: AppLockUiState,
    onDigitPressed: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val errorRed = Color(0xFFB3261E)

    val isConfirmStep = state.pinStep == PinStep.CONFIRM_NEW_PIN

    val title = if (isConfirmStep) "Konfirmasi PIN" else "Buat PIN Keamanan"
    val subtitle = if (isConfirmStep) {
        "Masukkan kembali 6 digit PIN yang baru saja kamu buat"
    } else {
        "Buat 6 digit PIN untuk mengunci dan melindungi privasi catatan kesehatan mentalmu"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConfirmStep) SecurityIcons.CheckCircle else SecurityIcons.Lock,
                    contentDescription = null,
                    tint = pineGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 6-dot visual indicator
            PinDots(
                length = 6,
                filledCount = state.currentPinLength,
                isError = state.isError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error feedback text
            if (state.isError && state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    fontSize = 14.sp,
                    color = errorRed,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Placeholder to prevent layout jump
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Keypad Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            PinKeypad(
                onDigitPressed = onDigitPressed,
                onBackspace = onBackspace,
                enabled = !state.isLoading
            )
        }
    }
}
