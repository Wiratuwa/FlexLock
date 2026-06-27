package com.example.fitnessrepcounter.domain.model

/**
 * Immutable snapshot of the current workout session state.
 * Updated by the WorkoutViewModel on each rep event.
 */
data class WorkoutSession(
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val repCount: Int = 0,
    val setCount: Int = 1,
    val targetReps: Int = 10,
    val isActive: Boolean = false,
    val isSoftLocked: Boolean = false,
    val sensorActive: Boolean = false,
    val cameraActive: Boolean = false,
    val poseConfidence: Float = 0f,
    val lastRepState: RepState = RepState.IDLE,
    val calibrated: Boolean = false,
)
