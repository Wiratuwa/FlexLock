package com.example.fitnessrepcounter.domain.model

/**
 * Supported exercise types.
 * Each exercise has specific angle thresholds and landmark requirements.
 */
enum class ExerciseType(
    val displayName: String,
    val description: String,
    val recommendedView: String,
) {
    SQUAT(
        displayName = "Squat",
        description = "Hip-knee-ankle flexion/extension",
        recommendedView = "Side view",
    ),
    PUSHUP(
        displayName = "Push-Up",
        description = "Shoulder-elbow-wrist flexion/extension",
        recommendedView = "Side view",
    ),
    JUMPING_JACK(
        displayName = "Jumping Jack",
        description = "Shoulder abduction/adduction",
        recommendedView = "Front view",
    ),
}
