package com.example.fitnessrepcounter.domain.engine

import com.example.fitnessrepcounter.domain.model.RepState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Temporal sliding-window state machine for cross-verifying sensor and pose events.
 *
 * The core anti-cheat mechanism: a rep is only counted when BOTH the sensor
 * pipeline (accelerometer peak) and pose pipeline (angle threshold crossing)
 * fire within a configurable time window.
 *
 * Either source can trigger first — the state machine handles both orderings.
 *
 * Thread-safety: Must be called from a single coroutine (e.g., viewModelScope).
 */
class FusionEngine(
    /** Maximum time (ms) between sensor and pose events to validate a rep */
    private val windowDurationMs: Long = 1500L,
    /** Cooldown (ms) after a validated rep before accepting new events */
    private val cooldownMs: Long = 500L,
) {
    private val _state = MutableStateFlow(RepState.IDLE)
    val state: StateFlow<RepState> = _state.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private var windowStartMs: Long = 0L
    private var lastValidationMs: Long = 0L

    /**
     * Called when the sensor pipeline detects a movement peak.
     * @param timestampMs current system time in milliseconds
     * @return true if this event triggered a rep validation
     */
    fun onSensorPeak(timestampMs: Long): Boolean {
        // Cooldown check
        if (timestampMs - lastValidationMs < cooldownMs) return false

        return when (_state.value) {
            RepState.IDLE -> {
                // Sensor fires first — open validation window
                _state.value = RepState.SENSOR_TRIGGERED
                windowStartMs = timestampMs
                false
            }
            RepState.POSE_TRIGGERED -> {
                // Pose already fired — check if within window
                if (timestampMs - windowStartMs <= windowDurationMs) {
                    validateRep(timestampMs)
                    true
                } else {
                    // Window expired, start fresh with sensor
                    _state.value = RepState.SENSOR_TRIGGERED
                    windowStartMs = timestampMs
                    false
                }
            }
            else -> false
        }
    }

    /**
     * Called when the pose pipeline confirms a rep-qualifying movement.
     * @param timestampMs current system time in milliseconds
     * @return true if this event triggered a rep validation
     */
    fun onPoseConfirmed(timestampMs: Long): Boolean {
        // Cooldown check
        if (timestampMs - lastValidationMs < cooldownMs) return false

        return when (_state.value) {
            RepState.IDLE -> {
                // Pose fires first — open validation window
                _state.value = RepState.POSE_TRIGGERED
                windowStartMs = timestampMs
                false
            }
            RepState.SENSOR_TRIGGERED -> {
                // Sensor already fired — check if within window
                if (timestampMs - windowStartMs <= windowDurationMs) {
                    validateRep(timestampMs)
                    true
                } else {
                    // Window expired, start fresh with pose
                    _state.value = RepState.POSE_TRIGGERED
                    windowStartMs = timestampMs
                    false
                }
            }
            else -> false
        }
    }

    /**
     * Call periodically to expire stale windows.
     * Prevents the state machine from getting stuck in a triggered state.
     */
    fun tick(timestampMs: Long) {
        val currentState = _state.value
        if (currentState == RepState.SENSOR_TRIGGERED || currentState == RepState.POSE_TRIGGERED) {
            if (timestampMs - windowStartMs > windowDurationMs) {
                _state.value = RepState.REJECTED
                // Immediately return to IDLE after rejection
                _state.value = RepState.IDLE
            }
        }
    }

    private fun validateRep(timestampMs: Long) {
        _state.value = RepState.VALIDATED
        _repCount.value += 1
        lastValidationMs = timestampMs
        // Return to IDLE after validation
        _state.value = RepState.IDLE
    }

    /** Reset everything. Call when starting a new set. */
    fun reset() {
        _state.value = RepState.IDLE
        _repCount.value = 0
        windowStartMs = 0L
        lastValidationMs = 0L
    }
}
