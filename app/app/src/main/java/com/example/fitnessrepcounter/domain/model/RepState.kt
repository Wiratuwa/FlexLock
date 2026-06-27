package com.example.fitnessrepcounter.domain.model

/**
 * State machine states for the fusion engine.
 * Tracks the lifecycle of a single rep detection attempt.
 */
enum class RepState {
    /** No rep in progress. Waiting for either source to trigger. */
    IDLE,
    /** Sensor detected a potential rep. Waiting for pose confirmation within window. */
    SENSOR_TRIGGERED,
    /** Pose detected movement first. Waiting for sensor confirmation within window. */
    POSE_TRIGGERED,
    /** Both sources confirmed. Rep is valid. Brief cooldown before returning to IDLE. */
    VALIDATED,
    /** Window expired without cross-confirmation. Rep rejected. */
    REJECTED,
}
