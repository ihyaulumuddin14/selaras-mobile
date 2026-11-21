package com.example.selaras.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.selaras.core.ui.components.SelarasHeader
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasIcons

/**
 * Screen displaying history of records, check-in reflections, and counselor PDF summaries.
 */
@Composable
fun HistoryScreen(
    onNavigateToCheckInHistory: () -> Unit = {},
    onNavigateToCounselorPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val backgroundCanvas = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val cardBg = Color(0xFFFFFFFF)
    val cardBorder = Color(0xFFE2E8E3)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundCanvas)
    ) {
        SelarasHeader(
            title = "Riwayat Catatan"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Lihat rekapitulasi suasana hati, catatan harian, dan ringkasan konselor yang pernah kamu buat.",
                fontSize = 14.sp,
                color = textSecondary,
                lineHeight = 20.sp
            )

        Spacer(modifier = Modifier.height(20.dp))

        // History Menu Card 1: Check-in & Pola Mingguan
        HistoryMenuCard(
            title = "Riwayat Check-in & Pola",
            subtitle = "Pantau perkembangan mood, energi, dan catatan harian 7 hari terakhir.",
            icon = SelarasIcons.EditNote,
            onClick = onNavigateToCheckInHistory
        )

        Spacer(modifier = Modifier.height(12.dp))

        // History Menu Card 2: Dokumen Ringkasan Konselor
        HistoryMenuCard(
            title = "Dokumen Ringkasan PDF",
            subtitle = "Arsip berkas PDF rekapitulasi data yang siap dibawa ke sesi psikolog.",
            icon = SelarasIcons.Description,
            onClick = onNavigateToCounselorPdf
        )

        Spacer(modifier = Modifier.height(12.dp))

        // History Menu Card 3: Riwayat Skrining PHQ-9
        HistoryMenuCard(
            title = "Riwayat Skor Skrining",
            subtitle = "Catatan skor penapisan depresi rutin dan berkala mingguan.",
            icon = SelarasIcons.FactCheck,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val cardBorder = Color(0xFFE2E8E3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = pineGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryContainer,
                contentColor = pineGreen
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = "Buka",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
