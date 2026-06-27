package com.example.fitnessrepcounter.domain.engine

import com.example.fitnessrepcounter.data.pose.PoseAngleResult
import com.example.fitnessrepcounter.domain.model.ExerciseType

/**
 * Tracks pose angle transitions to detect rep phases (flexion → extension).
 * A rep's pose component is confirmed when the angle transitions from
 * the flexion zone back to the extension zone (i.e., one full rep cycle).
 *
 * Thread-safety: Must be called from a single coroutine.
 */
class ExerciseValidator {

    /**
     * Tracking state for rep phase transitions.
     * A full rep cycle: WAITING → IN_FLEXION → COMPLETED → WAITING
     */
    private enum class Phase {
        /** Waiting for the user to enter the flexion (down) phase */
        WAITING,
        /** User is in the flexion (down) phase */
        IN_FLEXION,
        /** Full cycle completed — rep pose confirmed */
        COMPLETED,
    }

    private var phase: Phase = Phase.WAITING

    // Angle thresholds per exercise
    private val flexionThresholds = mapOf(
        ExerciseType.SQUAT to 100f,
        ExerciseType.PUSHUP to 90f,
        ExerciseType.JUMPING_JACK to 30f,
    )

    private val extensionThresholds = mapOf(
        ExerciseType.SQUAT to 160f,
        ExerciseType.PUSHUP to 160f,
        ExerciseType.JUMPING_JACK to 120f,
    )

    /**
     * Process a pose angle result and determine if a full rep cycle occurred.
     * @return true if a complete flexion → extension cycle was detected
     */
    fun processAngle(exerciseType: ExerciseType, angleResult: PoseAngleResult): Boolean {
        val flexThreshold = flexionThresholds[exerciseType] ?: return false
        val extThreshold = extensionThresholds[exerciseType] ?: return false

        return when (phase) {
            Phase.WAITING -> {
                // Enter flexion phase
                if (angleResult.angle < flexThreshold) {
                    phase = Phase.IN_FLEXION
                }
                false
            }
            Phase.IN_FLEXION -> {
                // Exit flexion → extension = rep cycle complete
                if (angleResult.angle > extThreshold) {
                    phase = Phase.COMPLETED
                    // Immediately reset for next rep
                    phase = Phase.WAITING
                    true
                } else {
                    false
                }
            }
            Phase.COMPLETED -> {
                phase = Phase.WAITING
                false
            }
        }
    }

    /** Reset phase tracking. Call when starting a new set. */
    fun reset() {
        phase = Phase.WAITING
    }
}
