package com.example.selaras.features.breathing

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.domain.usecase.breathing.BreathingPhase
import com.example.selaras.domain.usecase.breathing.BreathingRunState
import com.example.selaras.domain.usecase.breathing.BreathingSessionSnapshot

/**
 * A layered breathing orb driven by the session clock (not an independent infinite animation).
 * Both the outline and its visible outer glow stop precisely when the session is paused.
 */
@Composable
internal fun BreathingOrb(
    snapshot: BreathingSessionSnapshot,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }
    val eased = snapshot.phaseProgress.let { it * it * (3f - 2f * it) }
    val expansion = when {
        snapshot.runState == BreathingRunState.READY -> 0f
        !animationsEnabled -> when (snapshot.phase) {
            BreathingPhase.INHALE, BreathingPhase.HOLD_INHALE -> 1f
            else -> 0f
        }
        snapshot.phase == BreathingPhase.INHALE -> eased
        snapshot.phase == BreathingPhase.HOLD_INHALE -> 1f
        snapshot.phase == BreathingPhase.EXHALE -> 1f - eased
        else -> 0f
    }
    val scale = 0.57f + expansion * 0.43f
    val isReady = snapshot.runState == BreathingRunState.READY
    val orbLabel = if (isReady) "Siap" else snapshot.secondsRemaining.toString()
    val stateLabel = when (snapshot.runState) {
        BreathingRunState.READY -> "Siap memulai"
        BreathingRunState.PAUSED -> "Dijeda"
        BreathingRunState.RUNNING -> when (snapshot.phase) {
            BreathingPhase.INHALE -> "Tarik napas"
            BreathingPhase.HOLD_INHALE -> "Tahan napas"
            BreathingPhase.EXHALE -> "Embuskan napas"
            BreathingPhase.HOLD_EXHALE -> "Jeda sejenak"
        }
    }

    Box(
        modifier = modifier.semantics {
            contentDescription = if (isReady) stateLabel else "$stateLabel, $orbLabel detik"
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Leave enough room for the halo. A larger solid circle would clip it at the
            // Canvas edges, particularly at the end of the inhale phase.
            val radius = size.minDimension * 0.355f * scale
            val outerHaloRadius = radius * 1.40f
            val innerHaloRadius = radius * 1.24f
            val glowStrength = when (snapshot.runState) {
                BreathingRunState.READY -> 0.45f
                BreathingRunState.PAUSED -> 0.72f
                BreathingRunState.RUNNING -> 0.70f + expansion * 0.30f
            }
            val glowColor = lerp(Color(0xFF91C9AE), Color(0xFF5FAF8C), expansion)

            // The glow must peak OUTSIDE the filled disc. Previously its brightest
            // part was underneath an opaque fill and was almost entirely hidden.
            drawCircle(
                brush = Brush.radialGradient(
                    0.00f to Color.Transparent,
                    0.62f to glowColor.copy(alpha = 0.06f * glowStrength),
                    0.77f to glowColor.copy(alpha = 0.33f * glowStrength),
                    0.92f to glowColor.copy(alpha = 0.13f * glowStrength),
                    1.00f to Color.Transparent,
                    center = center,
                    radius = outerHaloRadius
                ),
                radius = outerHaloRadius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.00f to Color.Transparent,
                    0.70f to glowColor.copy(alpha = 0.12f * glowStrength),
                    0.84f to glowColor.copy(alpha = 0.34f * glowStrength),
                    1.00f to Color.Transparent,
                    center = center,
                    radius = innerHaloRadius
                ),
                radius = innerHaloRadius
            )

            // A very fine secondary ring follows the breath without flashing.
            drawCircle(
                color = glowColor.copy(alpha = (0.12f + expansion * 0.10f) * glowStrength),
                radius = radius * 1.11f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Calm color transition: lighter on exhale, richer sage on inhale.
            val centerColor = lerp(Color(0xFFE9F3EC), Color(0xFFDDF1E5), expansion)
            val edgeColor = lerp(Color(0xFFDCECE3), Color(0xFFC6E5D5), expansion)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(centerColor, edgeColor),
                    center = center - Offset(radius * 0.20f, radius * 0.22f),
                    radius = radius * 1.7f
                ),
                radius = radius
            )
            drawCircle(
                color = Color(0xFF356859),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.56f),
                radius = radius * 0.90f,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = orbLabel,
                color = BreathingColors.Primary,
                fontSize = if (isReady) 28.sp else 38.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            Text(
                text = "RITME",
                color = BreathingColors.Primary.copy(alpha = 0.70f),
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}
