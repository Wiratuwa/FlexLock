package com.example.fitnessrepcounter.data.pose

import com.example.fitnessrepcounter.domain.model.ExerciseType
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Result of pose angle calculation for a specific exercise.
 * @param angle The computed joint angle in degrees (0-180)
 * @param isInFlexion True if the angle indicates the "down" phase of the exercise
 * @param confidence Average confidence of the landmarks used
 */
data class PoseAngleResult(
    val angle: Float,
    val isInFlexion: Boolean,
    val confidence: Float,
)

/**
 * Calculates joint angles from ML Kit Pose landmarks for each exercise type.
 * Uses the law of cosines to compute angles between three body points.
 *
 * Angle thresholds define when a rep phase transition occurs:
 * - Flexion (down): angle goes below flexion threshold
 * - Extension (up): angle goes above extension threshold
 */
object PoseAngleCalculator {

    // Angle thresholds per exercise (degrees)
    private const val SQUAT_FLEXION_THRESHOLD = 100f    // knee bend threshold
    private const val SQUAT_EXTENSION_THRESHOLD = 160f  // standing straight
    private const val PUSHUP_FLEXION_THRESHOLD = 90f    // elbow bent
    private const val PUSHUP_EXTENSION_THRESHOLD = 160f // arms straight
    private const val JACK_FLEXION_THRESHOLD = 30f      // arms down
    private const val JACK_EXTENSION_THRESHOLD = 120f   // arms up

    /** Minimum landmark confidence to consider the pose usable */
    const val MIN_CONFIDENCE = 0.5f

    /**
     * Calculate the relevant angle for the given exercise from ML Kit Pose.
     * Returns null if required landmarks are not detected with sufficient confidence.
     */
    fun calculate(pose: Pose, exerciseType: ExerciseType): PoseAngleResult? {
        return when (exerciseType) {
            ExerciseType.SQUAT -> calculateSquatAngle(pose)
            ExerciseType.PUSHUP -> calculatePushupAngle(pose)
            ExerciseType.JUMPING_JACK -> calculateJumpingJackAngle(pose)
        }
    }

    /**
     * Check if critical landmarks for the exercise are visible.
     * Used during calibration.
     */
    fun areLandmarksVisible(pose: Pose, exerciseType: ExerciseType): Boolean {
        val requiredLandmarks = when (exerciseType) {
            ExerciseType.SQUAT -> listOf(
                PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE,
            )
            ExerciseType.PUSHUP -> listOf(
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST,
            )
            ExerciseType.JUMPING_JACK -> listOf(
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_WRIST,
                PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_WRIST,
            )
        }

        return requiredLandmarks.all { landmarkType ->
            val landmark = pose.getPoseLandmark(landmarkType)
            landmark != null && landmark.inFrameLikelihood >= MIN_CONFIDENCE
        }
    }

    // ── Squat: hip-knee-ankle angle (side view) ──

    private fun calculateSquatAngle(pose: Pose): PoseAngleResult? {
        // Use whichever side has better confidence
        val leftAngle = computeAngle(
            pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE
        )
        val rightAngle = computeAngle(
            pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        )

        val result = pickBestSide(leftAngle, rightAngle) ?: return null

        return PoseAngleResult(
            angle = result.first,
            isInFlexion = result.first < SQUAT_FLEXION_THRESHOLD,
            confidence = result.second,
        )
    }

    // ── Push-up: shoulder-elbow-wrist angle (side view) ──

    private fun calculatePushupAngle(pose: Pose): PoseAngleResult? {
        val leftAngle = computeAngle(
            pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST
        )
        val rightAngle = computeAngle(
            pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )

        val result = pickBestSide(leftAngle, rightAngle) ?: return null

        return PoseAngleResult(
            angle = result.first,
            isInFlexion = result.first < PUSHUP_FLEXION_THRESHOLD,
            confidence = result.second,
        )
    }

    // ── Jumping Jack: shoulder abduction angle (front view) ──

    private fun calculateJumpingJackAngle(pose: Pose): PoseAngleResult? {
        // Angle between hip-shoulder-wrist
        val leftAngle = computeAngle(
            pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_WRIST
        )
        val rightAngle = computeAngle(
            pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_WRIST
        )

        val result = pickBestSide(leftAngle, rightAngle) ?: return null

        return PoseAngleResult(
            angle = result.first,
            isInFlexion = result.first < JACK_FLEXION_THRESHOLD,
            confidence = result.second,
        )
    }

    // ── Helpers ──

    /**
     * Compute the angle at point B formed by points A-B-C using atan2.
     * Returns Pair(angle, avgConfidence) or null if landmarks are missing.
     */
    private fun computeAngle(
        pose: Pose,
        landmarkA: Int,
        landmarkB: Int,
        landmarkC: Int,
    ): Pair<Float, Float>? {
        val a = pose.getPoseLandmark(landmarkA) ?: return null
        val b = pose.getPoseLandmark(landmarkB) ?: return null
        val c = pose.getPoseLandmark(landmarkC) ?: return null

        val avgConfidence = (a.inFrameLikelihood + b.inFrameLikelihood + c.inFrameLikelihood) / 3f
        if (avgConfidence < MIN_CONFIDENCE) return null

        val radians = atan2(
            (c.position.y - b.position.y).toDouble(),
            (c.position.x - b.position.x).toDouble(),
        ) - atan2(
            (a.position.y - b.position.y).toDouble(),
            (a.position.x - b.position.x).toDouble(),
        )

        var angle = abs(Math.toDegrees(radians)).toFloat()
        if (angle > 180f) angle = 360f - angle

        return Pair(angle, avgConfidence)
    }

    /** Pick whichever side has higher confidence */
    private fun pickBestSide(
        left: Pair<Float, Float>?,
        right: Pair<Float, Float>?,
    ): Pair<Float, Float>? {
        return when {
            left != null && right != null -> if (left.second >= right.second) left else right
            left != null -> left
            right != null -> right
            else -> null
        }
    }
}
