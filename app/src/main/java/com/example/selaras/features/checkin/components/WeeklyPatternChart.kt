package com.example.selaras.features.checkin.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.WeeklyCheckInAnalysis
import java.time.DayOfWeek
import java.time.LocalDate

enum class ChartMetric(val label: String, val unitLabel: String) {
    MOOD("Suasana Hati", "Skala 1-5"),
    ENERGY("Tingkat Energi", "Skala 1-10"),
    SLEEP("Durasi Tidur", "Jam Istirahat")
}

data class DayChartPoint(
    val date: LocalDate,
    val dayName: String,
    val dayNumber: String,
    val checkIn: CheckIn?,
    val value: Float,
    val displayValue: String
)

/**
 * 7-Day Trend Chart composable inspired by [grafik_pola.png] adapted for Selaras.
 * Features line curve, gradient fill under the line, point markers, horizontal grid lines,
 * Y-axis markers, and interactive day selection.
 */
@Composable
fun WeeklyPatternChart(
    analysis: WeeklyCheckInAnalysis,
    modifier: Modifier = Modifier
) {
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val cardBorder = Color(0xFFE2E8E3)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val surfaceWhite = Color(0xFFFFFFFF)

    var selectedMetric by remember { mutableStateOf(ChartMetric.MOOD) }
    var selectedDayIndex by remember { mutableIntStateOf(6) } // Default to latest day

    // Prepare exactly 7 consecutive days: from startDate to endDate
    val days = remember(analysis, selectedMetric) {
        val startDate = analysis.startDate
        (0..6).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val entry = analysis.entries.find { it.checkInDate == date }
            val dayName = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Sen"
                DayOfWeek.TUESDAY -> "Sel"
                DayOfWeek.WEDNESDAY -> "Rab"
                DayOfWeek.THURSDAY -> "Kam"
                DayOfWeek.FRIDAY -> "Jum"
                DayOfWeek.SATURDAY -> "Sab"
                DayOfWeek.SUNDAY -> "Min"
                null -> ""
            }
            val (value, displayVal) = when (selectedMetric) {
                ChartMetric.MOOD -> {
                    val m = entry?.mood?.toFloat() ?: 0f
                    val label = when (entry?.mood) {
                        5 -> "Sangat Baik (5)"
                        4 -> "Baik (4)"
                        3 -> "Biasa (3)"
                        2 -> "Kurang (2)"
                        1 -> "Buruk (1)"
                        else -> "Belum Tercatat"
                    }
                    m to label
                }
                ChartMetric.ENERGY -> {
                    val e = entry?.energy?.toFloat() ?: 0f
                    val label = if (entry != null) "${entry.energy}/10" else "Belum Tercatat"
                    e to label
                }
                ChartMetric.SLEEP -> {
                    val h = entry?.let { it.sleepDurationMinutes / 60f } ?: 0f
                    val label = if (entry != null) {
                        val hours = entry.sleepDurationMinutes / 60
                        val mins = entry.sleepDurationMinutes % 60
                        "${hours}j ${mins}m"
                    } else {
                        "Belum Tercatat"
                    }
                    h to label
                }
            }
            DayChartPoint(
                date = date,
                dayName = dayName,
                dayNumber = date.dayOfMonth.toString(),
                checkIn = entry,
                value = value,
                displayValue = displayVal
            )
        }
    }

    val (minY, maxY, yLabels) = when (selectedMetric) {
        ChartMetric.MOOD -> Triple(1f, 5f, listOf("5", "3", "1"))
        ChartMetric.ENERGY -> Triple(0f, 10f, listOf("10", "5", "0"))
        ChartMetric.SLEEP -> Triple(0f, 12f, listOf("12j", "6j", "0j"))
    }

    // Animation progress for chart entry
    val animProgress = remember(selectedMetric) { Animatable(0f) }
    LaunchedEffect(selectedMetric) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val selectedPoint = days.getOrNull(selectedDayIndex) ?: days.last()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceWhite)
            .border(1.5.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Metric Switcher Chips (matching top segment selector in grafik_pola.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartMetric.values().forEach { metric ->
                val isSelected = metric == selectedMetric
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) pineGreen else primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable { selectedMetric = metric }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = metric.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else pineGreen,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Pola ${selectedMetric.label}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "${selectedPoint.dayName}, ${selectedPoint.dayNumber} ${formatMonthName(selectedPoint.date.monthValue)}",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = selectedPoint.displayValue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = pineGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas Chart Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(days, selectedMetric) {
                        detectTapGestures { offset ->
                            val leftPad = 24.dp.toPx()
                            val rightPad = 36.dp.toPx()
                            val chartW = size.width - leftPad - rightPad
                            val stepX = chartW / 6f
                            val clickedIndex = ((offset.x - leftPad + stepX / 2f) / stepX).toInt().coerceIn(0, 6)
                            selectedDayIndex = clickedIndex
                        }
                    }
            ) {
                val leftPad = 24.dp.toPx()
                val rightPad = 36.dp.toPx()
                val topPad = 14.dp.toPx()
                val bottomPad = 20.dp.toPx()

                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad
                val stepX = chartW / 6f

                // 1. Draw 3 Horizontal Dashed Grid Lines
                val gridYLevels = listOf(topPad, topPad + chartH / 2f, topPad + chartH)
                gridYLevels.forEach { y ->
                    drawLine(
                        color = cardBorder,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // 2. Draw Vertical Guide Line for Selected Day (matching grafik_pola.png)
                val selectedX = leftPad + selectedDayIndex * stepX
                drawLine(
                    color = pineGreen.copy(alpha = 0.25f),
                    start = Offset(selectedX, topPad),
                    end = Offset(selectedX, topPad + chartH),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // 3. Compute Points Coordinates
                val points = days.mapIndexed { index, item ->
                    val x = leftPad + index * stepX
                    val y = if (item.checkIn != null) {
                        val ratio = ((item.value - minY) / (maxY - minY)).coerceIn(0f, 1f)
                        topPad + (1f - ratio * animProgress.value) * chartH
                    } else {
                        topPad + chartH // Baseline
                    }
                    Offset(x, y)
                }

                // 4. Build Smooth Curved Path and Gradient Fill Path
                val recordedIndices = days.indices.filter { days[it].checkIn != null }
                if (recordedIndices.size >= 2) {
                    val linePath = Path()
                    val fillPath = Path()

                    val firstIdx = recordedIndices.first()
                    val lastIdx = recordedIndices.last()

                    linePath.moveTo(points[firstIdx].x, points[firstIdx].y)
                    fillPath.moveTo(points[firstIdx].x, topPad + chartH)
                    fillPath.lineTo(points[firstIdx].x, points[firstIdx].y)

                    for (k in 0 until recordedIndices.size - 1) {
                        val curIdx = recordedIndices[k]
                        val nextIdx = recordedIndices[k + 1]
                        val p1 = points[curIdx]
                        val p2 = points[nextIdx]
                        val dx = (p2.x - p1.x) / 2f
                        linePath.cubicTo(
                            p1.x + dx, p1.y,
                            p1.x + dx, p2.y,
                            p2.x, p2.y
                        )
                        fillPath.cubicTo(
                            p1.x + dx, p1.y,
                            p1.x + dx, p2.y,
                            p2.x, p2.y
                        )
                    }

                    fillPath.lineTo(points[lastIdx].x, topPad + chartH)
                    fillPath.close()

                    // Draw Gradient Area below Line
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                pineGreen.copy(alpha = 0.30f),
                                pineGreen.copy(alpha = 0.02f)
                            ),
                            startY = topPad,
                            endY = topPad + chartH
                        )
                    )

                    // Draw Solid Smooth Connecting Line
                    drawPath(
                        path = linePath,
                        color = pineGreen,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 5. Draw Data Dots / Markers
                points.forEachIndexed { i, pt ->
                    val hasEntry = days[i].checkIn != null
                    val isSelected = i == selectedDayIndex

                    if (hasEntry) {
                        // Outer glowing halo if selected
                        if (isSelected) {
                            drawCircle(
                                color = pineGreen.copy(alpha = 0.25f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                        }

                        // Outer border ring
                        drawCircle(
                            color = pineGreen,
                            radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                            center = pt
                        )

                        // Inner white center
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 3.5.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    } else {
                        // Subtle marker on baseline for unrecorded day
                        drawCircle(
                            color = if (isSelected) pineGreen.copy(alpha = 0.5f) else cardBorder,
                            radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Y-Axis Labels overlay on the right (matching right axis in grafik_pola.png)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 6.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis Day Labels Row with exact point centering via BoxWithConstraints
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val totalWidth = maxWidth
            val leftPadDp = 24.dp
            val rightPadDp = 36.dp
            val chartWidthDp = totalWidth - leftPadDp - rightPadDp
            val stepXDp = chartWidthDp / 6f

            days.forEachIndexed { i, item ->
                val xCenter = leftPadDp + stepXDp * i
                val isSelected = i == selectedDayIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = xCenter - 18.dp)
                        .width(36.dp)
                        .clickable { selectedDayIndex = i }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = item.dayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) pineGreen else textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.dayNumber,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) pineGreen else textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatMonthName(month: Int): String {
    return when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "Mei"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Agu"
        9 -> "Sep"
        10 -> "Okt"
        11 -> "Nov"
        12 -> "Des"
        else -> ""
    }
}
