package com.example.selaras.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasIcons

/**
 * Top contextual greeting and date header for Selaras Dashboard (Figma 12:717).
 */
@Composable
fun DashboardHeader(
    greeting: String,
    formattedDate: String,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val primaryContainer = Color(0xFFDCECE3)
    val pineGreen = Color(0xFF356859)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Bagaimana harimu?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date pill badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = pineGreen
                )
            }
        }

        // Settings Shortcut Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onNavigateToSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = SelarasIcons.Settings,
                contentDescription = "Pengaturan",
                tint = textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
