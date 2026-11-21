package com.example.selaras.features.security.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 6-dot visual indicator for PIN entry (Figma 173:2307, 173:2361, 173:2367).
 *
 * Design Specs (Section 6.9):
 * - 6 dots, each 16dp x 16dp, 12dp spacing, radius/full.
 * - Empty dot: 2dp outline border of #7B9187 (Outline).
 * - Filled dot: solid fill of #356859 (Primary).
 * - Error state: solid/border of #B3261E (Error) with horizontal shake animation.
 */
@Composable
fun PinDots(
    length: Int = 6,
    filledCount: Int,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pineGreen = Color(0xFF356859)
    val outlineGray = Color(0xFF7B9187)
    val errorRed = Color(0xFFB3261E)

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-14f) at 50
                    14f at 100
                    (-10f) at 150
                    10f at 200
                    (-6f) at 250
                    6f at 300
                    0f at 400
                }
            )
        }
    }

    Row(
        modifier = modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until length) {
            val isFilled = i < filledCount
            val dotColor = when {
                isError -> errorRed
                isFilled -> pineGreen
                else -> outlineGray
            }

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (isFilled || isError) {
                            Modifier.background(dotColor, CircleShape)
                        } else {
                            Modifier.border(2.dp, dotColor, CircleShape)
                        }
                    )
            )
        }
    }
}
