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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.features.security.AppLockUiState
import com.example.selaras.features.security.components.SecurityIcons

/**
 * Screen for inputting emergency recovery code to reset PIN (Figma 168:2348, 168:2349).
 */
@Composable
fun PinRecoveryScreen(
    state: AppLockUiState,
    onCodeChanged: (String) -> Unit,
    onVerifyCode: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val errorRed = Color(0xFFB3261E)
    val disabledColor = Color(0xFFE2E8E3)
    val disabledText = Color(0xFF596B62)
    val outlineColor = Color(0xFF7B9187)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Key Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SecurityIcons.Key,
                    contentDescription = null,
                    tint = pineGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Pemulihan Akses",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Masukkan 16 karakter kode pemulihan darurat yang telah kamu simpan saat pembuatan PIN.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Formatted Recovery Code TextField
            OutlinedTextField(
                value = state.inputRecoveryCode,
                onValueChange = onCodeChanged,
                placeholder = {
                    Text(
                        text = "XXXX-XXXX-XXXX-XXXX",
                        color = outlineColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = TextStyle(
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                ),
                isError = state.isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = pineGreen,
                    unfocusedBorderColor = outlineColor,
                    errorBorderColor = errorRed,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Error feedback
            if (state.isError && state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    fontSize = 13.sp,
                    color = errorRed,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Contoh: ABCD-EFGH-JKLM-NPQR",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onVerifyCode,
                enabled = state.inputRecoveryCode.replace("-", "").length == 16 && !state.isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pineGreen,
                    contentColor = Color.White,
                    disabledContainerColor = disabledColor,
                    disabledContentColor = disabledText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Verifikasi Kode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Kembali ke Layar Kunci",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }
        }
    }
}
