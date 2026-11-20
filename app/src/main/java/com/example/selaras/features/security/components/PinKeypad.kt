package com.example.selaras.features.security.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 3x4 numeric keypad for PIN entry (Figma 173:2307, 173:2337).
 *
 * Design Specs (Section 6.9):
 * - Grid: 1..9, bottom row: empty placeholder, 0, backspace.
 * - Minimum touch target >= 64dp x 64dp, circular shape with tactile ripple.
 * - Text: Roboto Regular 24sp #243B35.
 */
@Composable
fun PinKeypad(
    onDigitPressed: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val textPrimary = Color(0xFF243B35)
    val buttonBackground = Color(0xFFF7F7F2)

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    when (item) {
                        "" -> {
                            // Empty placeholder to balance the grid
                            Box(modifier = Modifier.size(72.dp))
                        }
                        "DEL" -> {
                            // Backspace button
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(buttonBackground)
                                    .clickable(
                                        enabled = enabled,
                                        role = Role.Button,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = textPrimary),
                                        onClick = onBackspace
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = SecurityIcons.Backspace,
                                    contentDescription = "Hapus digit",
                                    tint = textPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        else -> {
                            // Numeric button (0-9)
                            val digit = item.toInt()
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(buttonBackground)
                                    .clickable(
                                        enabled = enabled,
                                        role = Role.Button,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = textPrimary),
                                        onClick = { onDigitPressed(digit) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
