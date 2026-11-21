package com.example.selaras.features.counselor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen for Counselor Summary generation and local PDF export (Figma 168:2333 - 168:2343).
 */
@Composable
fun CounselorSummaryScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundCanvas = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundCanvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = "‹ Kembali ke Beranda", color = pineGreen, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ringkasan untuk Konselor",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Kompilasi data observasi 7 hari suasana hati dan tidur menjadi dokumen PDF lokal terstruktur untuk dibawa ke sesi konsultasi profesional.",
            fontSize = 14.sp,
            color = textSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateBack,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = pineGreen,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Kembali ke Beranda")
        }
    }
}
