package com.example.selaras.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Single Unified Theme for Selaras (ADR-013).
 *
 * Designed to provide a calming, non-threatening atmosphere using warm neutrals and pine green.
 * Dark theme is intentionally omitted to maintain therapeutic contrast and uniform counselor reports.
 */
private val SelarasLightColorScheme = lightColorScheme(
    primary = SelarasPrimary,
    onPrimary = SelarasOnPrimary,
    primaryContainer = SelarasPrimaryContainer,
    onPrimaryContainer = SelarasOnPrimaryContainer,
    surface = SelarasSurface,
    onSurface = SelarasOnSurface,
    surfaceVariant = SelarasSurfaceVariant,
    onSurfaceVariant = SelarasOnSurfaceVariant,
    background = SelarasBackground,
    onBackground = SelarasOnBackground,
    error = SelarasError,
    onError = SelarasOnError,
    errorContainer = SelarasErrorContainer,
    onErrorContainer = SelarasOnErrorContainer,
    outline = SelarasOutline
)

@Composable
fun SelarasTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SelarasLightColorScheme,
        typography = Typography,
        content = content
    )
}
