package com.example.fitnessrepcounter.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark-only theme — fitness apps live in dark mode
private val DarkColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = Charcoal,
    primaryContainer = ElectricLimeDim,
    onPrimaryContainer = Charcoal,
    secondary = InfoBlue,
    onSecondary = Charcoal,
    tertiary = ValidGreen,
    onTertiary = Charcoal,
    error = RejectRed,
    onError = Charcoal,
    background = Charcoal,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidGray,
    onSurfaceVariant = TextSecondary,
    outline = SubtleGray,
    outlineVariant = SubtleGray,
)

@Composable
fun FitnessRepCounterTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
