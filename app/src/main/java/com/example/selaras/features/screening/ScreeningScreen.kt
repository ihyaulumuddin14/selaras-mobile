package com.example.selaras.features.screening

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Screen for initiating PHQ-9 Depression Screening (Figma 41:999).
 *
 * Supports choosing between Routine and Manual screening modes within weekly quota.
 */
@Composable
fun ScreeningScreen(
    onStartScreening: (isRoutine: Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val backgroundCanvas = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val cardBorder = Color(0xFFE2E8E3)

    var selectedMode by remember { mutableStateOf("ROUTINE") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundCanvas)
    ) {
        SelarasHeader(
            title = "Skrining PHQ-9"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kuesioner terstandarisasi 9 butir untuk mengevaluasi derajat kesehatan emosional dalam 2 minggu terakhir.",
                fontSize = 14.sp,
                color = textSecondary,
                lineHeight = 20.sp
            )

        Spacer(modifier = Modifier.height(24.dp))

        // Mode 1: Skrining Rutin Mingguan
        ScreeningModeCard(
            title = "Skrining Rutin",
            subtitle = "Penapisan berkala otomatis yang dibuka setiap awal minggu. Kuota: 1x per minggu.",
            isSelected = selectedMode == "ROUTINE",
            onSelect = { selectedMode = "ROUTINE" }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mode 2: Skrining Manual / Fleksibel
        ScreeningModeCard(
            title = "Skrining Mandiri",
            subtitle = "Lakukan penapisan tambahan sewaktu-waktu saat kamu merasakan perubahan emosi mendadak. Kuota: 1x per minggu.",
            isSelected = selectedMode == "MANUAL",
            onSelect = { selectedMode = "MANUAL" }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Button
        Button(
            onClick = { onStartScreening(selectedMode == "ROUTINE") },
            colors = ButtonDefaults.buttonColors(
                containerColor = pineGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Mulai Kuesioner (9 Pertanyaan)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScreeningModeCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val cardBorder = Color(0xFFE2E8E3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) primaryContainer.copy(alpha = 0.4f) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) pineGreen else cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isSelected) pineGreen else Color(0xFF7B9187), CircleShape)
                    .background(if (isSelected) pineGreen else Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = textSecondary,
            lineHeight = 18.sp
        )
    }
}
