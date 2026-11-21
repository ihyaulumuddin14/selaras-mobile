package com.example.selaras.features.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasIcons
import com.example.selaras.domain.model.CheckIn

/**
 * Primary card on the dashboard showing today's Check-in status.
 *
 * Implements 2 distinct Figma states:
 * 1. Unrecorded state (`F3/Beranda` / Node 86:1195) with optional gentle pulse animation
 * 2. Recorded state (`F3/Beranda tercatat` / Node 86:1203)
 */
@Composable
fun TodayCheckInCard(
    todayCheckIn: CheckIn?,
    onStartCheckIn: () -> Unit,
    onViewWeeklyAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false
) {
    val surfaceWhite = Color(0xFFFFFFFF)
    val cardBorder = Color(0xFFE2E8E3)
    val pineGreen = Color(0xFF356859)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val primaryContainer = Color(0xFFDCECE3)

    if (todayCheckIn == null) {
        val infiniteTransition = rememberInfiniteTransition(label = "CheckInPulseAnimation")
        val pulseScale by if (isPulsing) {
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.018f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )
        } else {
            remember { mutableFloatStateOf(1f) }
        }

        val borderAlpha by if (isPulsing) {
            infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseBorderAlpha"
            )
        } else {
            remember { mutableFloatStateOf(0.35f) }
        }

        val animatedBorderColor = pineGreen.copy(alpha = borderAlpha)

        // State A: Belum Check-in (Figma 86:1195)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceWhite)
                .border(
                    width = if (isPulsing) 1.8.dp else 1.5.dp,
                    color = if (isPulsing) animatedBorderColor else cardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SelarasIcons.EditNote,
                        contentDescription = null,
                        tint = pineGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Check-in Hari Ini Belum Diisi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Luangkan 1 menit untuk mencatat kondisimu",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartCheckIn,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pineGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = SelarasIcons.EditNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Catat Hari Ini",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        // State B: Sudah Check-in (Figma 86:1203)
        val moodLabel = when (todayCheckIn.mood) {
            5 -> "😄 Sangat Baik"
            4 -> "🙂 Baik"
            3 -> "😐 Cukup / Netral"
            2 -> "🙁 Kurang Baik"
            else -> "😞 Sangat Buruk"
        }

        val hours = todayCheckIn.sleepDurationMinutes / 60
        val minutes = todayCheckIn.sleepDurationMinutes % 60
        val sleepFormatted = "${hours}j ${minutes}m"

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceWhite)
                .border(1.5.dp, primaryContainer, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pineGreen)
                    )
                    Text(
                        text = "Check-in Hari Ini Tercatat",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pineGreen
                    )
                }

                Text(
                    text = "✓ Lengkap",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = pineGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Metric Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mood Badge
                MetricPill(
                    label = "Suasana Hati",
                    value = moodLabel,
                    modifier = Modifier.weight(1.3f)
                )

                // Energy Badge
                MetricPill(
                    label = "Energi",
                    value = "⚡ ${todayCheckIn.energy}/10",
                    modifier = Modifier.weight(0.9f)
                )

                // Sleep Badge
                MetricPill(
                    label = "Tidur",
                    value = "🌙 $sleepFormatted",
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Note snippet if available
            if (!todayCheckIn.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF7F7F2))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "\"${todayCheckIn.note}\"",
                        fontSize = 13.sp,
                        color = textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onStartCheckIn,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(
                        text = "Ubah Catatan",
                        fontSize = 13.sp,
                        color = pineGreen
                    )
                }

                Button(
                    onClick = onViewWeeklyAnalysis,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryContainer,
                        contentColor = pineGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(
                        text = "Pola 7 Hari",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val pillBg = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(pillBg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
