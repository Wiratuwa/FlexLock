package com.example.fitnessrepcounter.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessrepcounter.domain.model.ExerciseType
import com.example.fitnessrepcounter.theme.*

@Composable
fun HomeScreen(
    onExerciseSelected: (ExerciseType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Charcoal)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Header ──
        Column {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "REP",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    color = ElectricLime,
                    letterSpacing = (-4).sp,
                ),
            )
            Text(
                text = "COUNTER",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = TextSecondary,
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Anti-cheat dual-source verification",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled),
            )
        }

        // ── Exercise Selection ──
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SELECT EXERCISE",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextDisabled,
                    letterSpacing = 3.sp,
                ),
            )

            ExerciseType.entries.forEach { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onClick = { onExerciseSelected(exercise) },
                )
            }
        }

        // ── Footer ──
        Text(
            text = "Position camera at ${ExerciseType.entries.first().recommendedView.lowercase()}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextDisabled,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseType,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "cardScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) ElectricLime else SubtleGray,
        animationSpec = tween(200),
        label = "borderColor",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(DarkSurface, MidGray.copy(alpha = 0.3f)),
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    text = exercise.displayName,
                    style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${exercise.description} • ${exercise.recommendedView}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextDisabled),
                )
            }
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = ElectricLime,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
