package com.example.selaras.features.security.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.selaras.features.security.components.RecoveryCodeCard
import com.example.selaras.features.security.components.SecurityIcons

/**
 * Screen displaying the emergency 16-character recovery code after PIN setup (Figma 168:2348).
 *
 * Requirements:
 * - Card displaying formatted recovery code with "Salin Kode" action.
 * - Checkbox: "Saya telah mencatat dan menyimpan kode pemulihan ini dengan aman".
 * - "Selesai & Masuk" button (enabled only when checkbox is checked).
 */
@Composable
fun RecoveryCodeDisplayScreen(
    state: AppLockUiState,
    onCopyCode: () -> Unit,
    onCheckSaved: (Boolean) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val disabledColor = Color(0xFFE2E8E3)
    val disabledText = Color(0xFF596B62)
    val warningCardBg = Color(0xFFFFF0E5)
    val warningText = Color(0xFF9E4A00)

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

            // Shield Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SecurityIcons.Shield,
                    contentDescription = null,
                    tint = pineGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Simpan Kode Pemulihan",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Catat atau salin kode 16-karakter ini di tempat aman. Kode ini adalah satu-satunya cara memulihkan catatanmu jika lupa PIN.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recovery Code Display Card
            RecoveryCodeCard(
                recoveryCode = state.recoveryCode,
                onCopied = onCopyCode,
                isCopied = state.isCopiedToClipboard
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Disclaimer Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(warningCardBg, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔒 Selaras tidak menyimpan kode ini di server mana pun demi privasi totalmu. Jika kode hilang dan kamu lupa PIN, catatan tidak dapat dipulihkan.",
                    fontSize = 13.sp,
                    color = warningText,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirmation Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isRecoverySavedChecked,
                    onCheckedChange = onCheckSaved,
                    colors = CheckboxDefaults.colors(
                        checkedColor = pineGreen,
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    text = "Saya telah mencatat dan menyimpan kode pemulihan ini dengan aman.",
                    fontSize = 14.sp,
                    color = textPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Bottom Action Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        ) {
            Button(
                onClick = onComplete,
                enabled = state.isRecoverySavedChecked,
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
                Text(
                    text = "Selesai & Masuk",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
