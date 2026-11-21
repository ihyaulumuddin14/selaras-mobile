package com.example.selaras.features.dashboard.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasIcons

/**
 * Priority Help Path: Crisis & Emergency Hotline Banner (FR-DSH-03 & ADR-013).
 *
 * Always accessible, non-blocking, and provides direct one-tap dialing to Sejiwa (119 ext. 8).
 */
@Composable
fun CrisisBanner(
    onNavigateToHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val warmBg = Color(0xFFFFF0E5)
    val warmBorder = Color(0xFFFFD8BF)
    val textPrimary = Color(0xFF243B35)
    val crisisRed = Color(0xFFB3261E)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(warmBg)
            .border(1.dp, warmBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = SelarasIcons.Phone,
                contentDescription = null,
                tint = crisisRed,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = "Butuh Bantuan Segera?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Hotline Kesehatan Jiwa 119 ext. 8 (Sejiwa) siap mendampingi kamu 24 jam bebas pulsa.",
            fontSize = 13.sp,
            color = Color(0xFF52675F),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:119")
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = crisisRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Icon(
                    imageVector = SelarasIcons.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Telepon 119",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onNavigateToHelp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = textPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Text(
                    text = "Layanan Lainnya",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
