package com.example.selaras.features.security.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card displaying the formatted 16-character Emergency Recovery Code (Figma 168:2348).
 *
 * Design Specs (Section 6.10):
 * - Surface #FFFFFF, radius 16dp, padding 16dp, border #E2E8E3.
 * - Formatted into 4 segments (XXXX-XXXX-XXXX-XXXX) with monospace font & letter-spacing.
 * - Outlined button "Salin Kode" with content_copy icon.
 */
@Composable
fun RecoveryCodeCard(
    recoveryCode: String,
    onCopied: () -> Unit,
    isCopied: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val surfaceWhite = Color(0xFFFFFFFF)
    val cardBorder = Color(0xFFE2E8E3)
    val pineGreen = Color(0xFF356859)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val primaryContainer = Color(0xFFDCECE3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceWhite, RoundedCornerShape(16.dp))
            .border(1.5.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = SecurityIcons.Key,
                contentDescription = null,
                tint = pineGreen,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "KODE PEMULIHAN DARURAT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = pineGreen,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Formatted 16-character code (XXXX-XXXX-XXXX-XXXX)
        Text(
            text = recoveryCode,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            color = textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(vertical = 14.dp, horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(recoveryCode))
                onCopied()
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = pineGreen
            ),
            border = BorderStroke(1.dp, pineGreen),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                imageVector = if (isCopied) SecurityIcons.CheckCircle else SecurityIcons.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = if (isCopied) "Kode Tersalin!" else "Salin Kode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (isCopied) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tersimpan di papan klip ponsel",
                fontSize = 12.sp,
                color = pineGreen
            )
        }
    }
}
