package com.example.selaras.features.breathing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object BreathingColors {
    val Primary = Color(0xFF356859)
    val Background = Color(0xFFF7F7F2)
    val Text = Color(0xFF243B35)
    val Muted = Color(0xFF52675F)
    val Outline = Color(0xFF7B9187)
    val PaleGreen = Color(0xFFDCECE3)
    val Disabled = Color(0xFFE2E8E3)
    val Error = Color(0xFFB3261E)
}

@Composable
internal fun BreathingHeader(
    title: String,
    onBack: () -> Unit,
    onHelp: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(54.dp).clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("←", fontSize = 27.sp, color = BreathingColors.Primary)
        }
        Text(
            title,
            color = BreathingColors.Text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
        Spacer(Modifier.weight(1f))
        if (onHelp != null) {
            Box(
                modifier = Modifier.size(50.dp).clickable(role = Role.Button, onClick = onHelp),
                contentAlignment = Alignment.Center
            ) {
                Text("ⓘ", color = BreathingColors.Primary, fontSize = 24.sp)
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
internal fun BreathingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BreathingColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = BreathingColors.Disabled,
            disabledContentColor = BreathingColors.Muted
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun BreathingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, BreathingColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BreathingColors.Primary)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
