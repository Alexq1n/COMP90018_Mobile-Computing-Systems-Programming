package com.group5.roammate.pet

import kotlin.math.sqrt

enum class PetMotionSource(val label: String) {
    StepDetector("Step sensor"),
    StepCounter("Step counter"),
    Accelerometer("Motion estimate"),
    Unavailable("Motion unavailable"),
}

data class PetMotionState(
    val isMoving: Boolean = false,
    val source: PetMotionSource = PetMotionSource.Unavailable,
    val needsActivityPermission: Boolean = false,
    val isActive: Boolean = false,
)

/**
 * Foreground animation signal, not a distance, fitness or fatigue measurement.
 * All times must use the same monotonic clock (SensorEvent.timestamp / elapsedRealtime).
 * Step sensors are authoritative. The accelerometer is a deliberately conservative fallback:
 * sustained walking-like pulses qualify; rotating the phone or one shake does not.
 */
class PetMotionEngine(private val stationaryAfterMillis: Long = 6_500L) {
    private var lastMovementAt: Long? = null
    private var lastDetectorStepAt: Long? = null
    private var previousCounter: Float? = null
    private var pendingCounterSteps = 0
    private var previousCounterChangeAt: Long? = null
    private var lastAccelerationAt: Long? = null
    private var lastPeakAt: Long? = null
    private var firstPeakAt: Long? = null
    private var peakCount = 0
    private var peakArmed = true
    private var rejectAccelerationUntil = Long.MIN_VALUE

    fun onStepDetected(atMillis: Long) {
        val previous = lastDetectorStepAt
        if (previous != null && atMillis <= previous) return
        // Two nearby steps avoid switching the loop for a single incidental movement.
        if (previous != null && atMillis - previous <= 2_500L) {
            lastMovementAt = atMillis
        }
        lastDetectorStepAt = atMillis
    }

    fun onStepCounter(totalSteps: Float, atMillis: Long) {
        if (!totalSteps.isFinite() || totalSteps < 0f) return
        if (previousCounterChangeAt?.let { atMillis < it } == true) return
        val previous = previousCounter
        previousCounter = totalSteps
        // Android reports a cumulative value since reboot. Opening the screen is not walking.
        if (previous == null || totalSteps < previous) {
            pendingCounterSteps = 0
            previousCounterChangeAt = null
            return
        }
        val increase = (totalSteps - previous).toInt()
        if (increase <= 0) return
        val previousChange = previousCounterChangeAt
        if (previousChange == null || atMillis - previousChange > 3_000L) {
            pendingCounterSteps = 0
        }
        pendingCounterSteps = (pendingCounterSteps + increase).coerceAtMost(2)
        previousCounterChangeAt = atMillis
        if (pendingCounterSteps >= 2) lastMovementAt = atMillis
    }

    fun onAcceleration(x: Float, y: Float, z: Float, atMillis: Long) {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return
        val previousSample = lastAccelerationAt
        if (previousSample != null && atMillis <= previousSample) return
        if (previousSample != null && atMillis - previousSample > 1_500L) resetPeaks()
        lastAccelerationAt = atMillis
        // The norm is orientation independent: slowly turning the phone preserves about 1 g.
        val magnitudeG = sqrt(x * x + y * y + z * z) / 9.80665f
        if (!magnitudeG.isFinite()) return
        if (magnitudeG > 2.1f || magnitudeG < 0.25f) {
            resetPeaks()
            rejectAccelerationUntil = atMillis + 1_200L
            return
        }
        if (atMillis < rejectAccelerationUntil) return
        val aboveGravity = magnitudeG - 1f
        if (aboveGravity < 0.045f) peakArmed = true
        if (!peakArmed || aboveGravity < 0.14f) return
        peakArmed = false
        val previousPeak = lastPeakAt
        val interval = previousPeak?.let { atMillis - it }
        if (interval == null || interval !in 300L..1_400L) {
            // Fast repetitive shaking and widely spaced handling gestures cannot accumulate.
            peakCount = 1
            firstPeakAt = atMillis
        } else {
            peakCount += 1
        }
        lastPeakAt = atMillis
        if (peakCount >= 4 && atMillis - (firstPeakAt ?: atMillis) >= 1_300L) {
            lastMovementAt = atMillis
        }
    }

    fun isMoving(atMillis: Long): Boolean = lastMovementAt?.let {
        atMillis >= it && atMillis - it < stationaryAfterMillis
    } ?: false

    fun reset() {
        lastMovementAt = null
        lastDetectorStepAt = null
        previousCounter = null
        pendingCounterSteps = 0
        previousCounterChangeAt = null
        lastAccelerationAt = null
        rejectAccelerationUntil = Long.MIN_VALUE
        resetPeaks()
    }

    private fun resetPeaks() {
        lastPeakAt = null
        firstPeakAt = null
        peakCount = 0
        peakArmed = true
    }
}
