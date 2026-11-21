package com.example.selaras.features.checkin.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private enum class ConfettiShape {
    CIRCLE, RECTANGLE, DIAMOND
}

private data class ConfettiParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val velocityX: Float,
    val velocityY: Float,
    val gravity: Float,
    val size: Float,
    val color: Color,
    val shape: ConfettiShape,
    val rotationSpeed: Float
)

/**
 * Satisfying pure-Compose celebration confetti animation triggered when saving a daily check-in.
 */
@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 42,
    durationMillis: Int = 2400
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
        )
    }

    val palette = remember {
        listOf(
            Color(0xFF356859), // Pine Green
            Color(0xFF88B39B), // Soft Sage
            Color(0xFFE57373), // Warm Coral
            Color(0xFFF5B041), // Golden Amber
            Color(0xFF9B82C1), // Soft Lavender
            Color(0xFF80CBC4), // Soft Mint
            Color(0xFF4FA3D1)  // Sky Blue
        )
    }

    val particles = remember {
        val random = Random(42)
        List(particleCount) {
            val angle = random.nextDouble(Math.PI * 0.15, Math.PI * 0.85) // Upward arc
            val speed = random.nextDouble(380.0, 780.0).toFloat()
            val vx = (Math.cos(angle) * speed * if (random.nextBoolean()) 1 else -1).toFloat()
            val vy = (-Math.sin(angle) * speed).toFloat()
            ConfettiParticle(
                initialXRatio = 0.5f + random.nextDouble(-0.08, 0.08).toFloat(),
                initialYRatio = 0.35f + random.nextDouble(-0.04, 0.04).toFloat(),
                velocityX = vx,
                velocityY = vy,
                gravity = random.nextDouble(750.0, 1100.0).toFloat(),
                size = random.nextDouble(14.0, 26.0).toFloat(),
                color = palette[random.nextInt(palette.size)],
                shape = ConfettiShape.values()[random.nextInt(ConfettiShape.values().size)],
                rotationSpeed = random.nextDouble(-540.0, 540.0).toFloat()
            )
        }
    }

    val currentProgress = progress.value
    if (currentProgress < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val timeSec = currentProgress * (durationMillis / 1000f)
            val alpha = (1f - (currentProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)

            particles.forEach { p ->
                val x = p.initialXRatio * size.width + p.velocityX * timeSec
                val y = p.initialYRatio * size.height + p.velocityY * timeSec + 0.5f * p.gravity * timeSec * timeSec
                val rotation = p.rotationSpeed * timeSec

                val drawColor = p.color.copy(alpha = alpha)

                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    when (p.shape) {
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = drawColor,
                                radius = p.size / 2f,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = drawColor,
                                topLeft = Offset(x - p.size / 2f, y - p.size / 4f),
                                size = Size(p.size, p.size / 2f)
                            )
                        }
                        ConfettiShape.DIAMOND -> {
                            val path = Path().apply {
                                moveTo(x, y - p.size / 2f)
                                lineTo(x + p.size / 2f, y)
                                lineTo(x, y + p.size / 2f)
                                lineTo(x - p.size / 2f, y)
                                close()
                            }
                            drawPath(path = path, color = drawColor)
                        }
                    }
                }
            }
        }
    }
}
