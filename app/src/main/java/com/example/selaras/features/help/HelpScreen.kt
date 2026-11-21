package com.example.selaras.features.help

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasHeader
import com.example.selaras.core.ui.components.SelarasIcons

/**
 * Priority Help Path: Bantuan & Kontak Krisis (Figma 12:720, 43:998).
 *
 * Always accessible, non-blocking, and provides direct dialing for emergency hotlines.
 */
@Composable
fun HelpScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val backgroundCanvas = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val crisisRed = Color(0xFFB3261E)
    val cardBg = Color(0xFFFFFFFF)
    val cardBorder = Color(0xFFE2E8E3)
    val warmBg = Color(0xFFFFF0E5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundCanvas)
    ) {
        SelarasHeader(
            title = "Bantuan & Kontak Krisis"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kamu tidak sendiri. Bantuan profesional dan saluran krisis siap mendampingimu kapan saja.",
                fontSize = 14.sp,
                color = textSecondary,
                lineHeight = 20.sp
            )

        Spacer(modifier = Modifier.height(20.dp))

        // Hotline Utama Sejiwa 119
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(warmBg)
                .border(1.5.dp, Color(0xFFFFD8BF), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(crisisRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SelarasIcons.Phone,
                        contentDescription = null,
                        tint = crisisRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Hotline Sejiwa 119 ext. 8",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Kemenkes RI · 24 Jam Bebas Pulsa",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Layanan konseling krisis pertama untuk individu yang mengalami tekanan psikologis berat atau pikiran menyakiti diri.",
                fontSize = 13.sp,
                color = textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = SelarasIcons.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Panggil Hotline 119",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Lembaga Layanan Psikologis Lainnya",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Directory Card 1: Yayasan Pulih
        DirectoryItem(
            name = "Yayasan Pulih",
            desc = "Penanganan trauma dan pemulihan kesehatan psikososial.",
            contact = "(021) 7884 2580",
            phone = "02178842580"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Directory Card 2: LSM Jangan Bunuh Diri
        DirectoryItem(
            name = "LSM Jangan Bunuh Diri",
            desc = "Konsultasi krisis dan pencegahan bunuh diri.",
            contact = "(021) 9696 9293",
            phone = "02196969293"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pertolongan Pertama Emosional
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Tips Pertolongan Pertama Saat Cemas",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Teknik Grounding 5-4-3-2-1:\n" +
                        "   • 5 hal yang bisa kamu lihat\n" +
                        "   • 4 hal yang bisa kamu sentuh\n" +
                        "   • 3 hal yang bisa kamu dengar\n" +
                        "   • 2 hal yang bisa kamu cium\n" +
                        "   • 1 hal yang bisa kamu rasakan\n\n" +
                        "2. Latihan napas lambat 4-7-8 untuk meredakan detak jantung cepat.",
                fontSize = 13.sp,
                color = textSecondary,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DirectoryItem(
    name: String,
    desc: String,
    contact: String,
    phone: String
) {
    val context = LocalContext.current
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val cardBorder = Color(0xFFE2E8E3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDCECE3),
                    contentColor = pineGreen
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Hubungi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = desc,
            fontSize = 13.sp,
            color = textSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Telp: $contact",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = pineGreen
        )
    }
}
