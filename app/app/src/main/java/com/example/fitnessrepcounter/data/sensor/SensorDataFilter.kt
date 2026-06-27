package com.example.fitnessrepcounter.data.sensor

import kotlin.math.sqrt

/**
 * Processes raw accelerometer data into peak detection events.
 *
 * Pipeline:
 * 1. Compute magnitude from xyz
 * 2. Apply low-pass filter to smooth noise
 * 3. Sliding window peak detection with refractory period
 *
 * Thread-safe: designed to be called from a single coroutine.
 */
class SensorDataFilter(
    /** Smoothing factor for low-pass filter. Lower = smoother. Range: 0.0-1.0 */
    private val alpha: Float = 0.15f,
    /** Minimum acceleration magnitude delta to consider as a peak */
    private val peakThreshold: Float = 2.5f,
    /** Minimum time (nanos) between two peaks. Prevents double-counting. */
    private val refractoryPeriodNanos: Long = 300_000_000L, // 300ms
    /** Number of samples in the sliding window for peak detection */
    private val windowSize: Int = 15,
) {
    // Low-pass filter state
    private var filteredMagnitude: Float = 0f
    private var initialized: Boolean = false

    // Sliding window buffer
    private val windowBuffer = FloatArray(windowSize)
    private var windowIndex = 0
    private var windowFilled = false

    // Refractory tracking
    private var lastPeakTimestamp: Long = 0L

    // Derivative tracking for sign-change detection
    private var previousValue: Float = 0f
    private var previousDerivative: Float = 0f

    /**
     * Sealed class representing the result of processing a sensor sample.
     */
    sealed class FilterResult {
        /** A valid peak was detected at this timestamp */
        data class PeakDetected(val timestamp: Long, val magnitude: Float) : FilterResult()
        /** No peak — just a regular sample */
        data object NoEvent : FilterResult()
    }

    /**
     * Process a single accelerometer reading.
     * @return PeakDetected if this sample represents a movement peak, NoEvent otherwise
     */
    fun process(data: SensorData): FilterResult {
        // Step 1: Compute magnitude (removes gravity direction dependency)
        val magnitude = sqrt(data.x * data.x + data.y * data.y + data.z * data.z)

        // Step 2: Low-pass filter
        if (!initialized) {
            filteredMagnitude = magnitude
            initialized = true
            previousValue = magnitude
            return FilterResult.NoEvent
        }
        filteredMagnitude = alpha * magnitude + (1f - alpha) * filteredMagnitude

        // Step 3: Add to sliding window
        windowBuffer[windowIndex] = filteredMagnitude
        windowIndex = (windowIndex + 1) % windowSize
        if (windowIndex == 0) windowFilled = true

        // Need full window before detecting peaks
        if (!windowFilled) {
            previousValue = filteredMagnitude
            return FilterResult.NoEvent
        }

        // Step 4: Peak detection via derivative sign change
        val derivative = filteredMagnitude - previousValue
        val isPeak = previousDerivative > 0f && derivative <= 0f // positive → negative = peak

        previousDerivative = derivative
        previousValue = filteredMagnitude

        if (!isPeak) return FilterResult.NoEvent

        // Step 5: Check if peak exceeds threshold
        val windowMin = windowBuffer.min()
        val peakDelta = filteredMagnitude - windowMin

        if (peakDelta < peakThreshold) return FilterResult.NoEvent

        // Step 6: Refractory period check
        if (data.timestamp - lastPeakTimestamp < refractoryPeriodNanos) return FilterResult.NoEvent

        lastPeakTimestamp = data.timestamp
        return FilterResult.PeakDetected(
            timestamp = data.timestamp,
            magnitude = filteredMagnitude,
        )
    }

    /** Reset all state. Call when starting a new set. */
    fun reset() {
        filteredMagnitude = 0f
        initialized = false
        windowIndex = 0
        windowFilled = false
        lastPeakTimestamp = 0L
        previousValue = 0f
        previousDerivative = 0f
        windowBuffer.fill(0f)
    }
}
