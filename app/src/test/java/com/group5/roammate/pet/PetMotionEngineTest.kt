package com.group5.roammate.pet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetMotionEngineTest {
    @Test fun twoStepsStartWalkingThenQuietReturnsToIdle() {
        val engine = PetMotionEngine()
        engine.onStepDetected(1_000)
        assertFalse(engine.isMoving(1_000))
        engine.onStepDetected(1_600)
        assertTrue(engine.isMoving(1_600))
        assertTrue(engine.isMoving(8_099))
        assertFalse(engine.isMoving(8_100))
        engine.onStepDetected(9_000)
        assertFalse(engine.isMoving(9_000))
    }

    @Test fun cumulativeCounterNeedsNewStepsAndHandlesDeviceReboot() {
        val engine = PetMotionEngine()
        engine.onStepCounter(12_000f, 1_000)
        assertFalse(engine.isMoving(1_000))
        engine.onStepCounter(12_001f, 1_600)
        assertFalse(engine.isMoving(1_600))
        engine.onStepCounter(12_002f, 2_200)
        assertTrue(engine.isMoving(2_200))
        engine.reset()
        engine.onStepCounter(0f, 3_000)
        assertFalse(engine.isMoving(3_000))
        engine.onStepCounter(2f, 4_000)
        assertTrue(engine.isMoving(4_000))
    }

    @Test fun counterDecreasingOrInvalidNeverManufacturesMovement() {
        val engine = PetMotionEngine()
        engine.onStepCounter(500f, 1_000)
        engine.onStepCounter(0f, 1_500)
        engine.onStepCounter(Float.NaN, 2_000)
        engine.onStepCounter(-4f, 2_500)
        engine.onStepCounter(1f, 3_000)
        assertFalse(engine.isMoving(3_000))
    }

    @Test fun smoothRotationAndSingleShakeDoNotCountAsWalking() {
        val engine = PetMotionEngine()
        engine.onAcceleration(0f, 0f, 9.80665f, 1_000)
        engine.onAcceleration(9.80665f, 0f, 0f, 1_100)
        engine.onAcceleration(0f, 9.80665f, 0f, 1_200)
        engine.onAcceleration(0f, 0f, 30f, 1_300)
        engine.onAcceleration(0f, 0f, 9.80665f, 1_400)
        assertFalse(engine.isMoving(1_500))
    }

    @Test fun sustainedGaitStartsEstimateAndPauseClearsIt() {
        val engine = PetMotionEngine()
        for (index in 0..3) {
            val time = 1_000L + index * 500L
            engine.onAcceleration(0f, 0f, 9.80665f, time)
            engine.onAcceleration(0f, 0f, 12f, time + 100L)
        }
        assertTrue(engine.isMoving(2_600))
        engine.reset()
        assertFalse(engine.isMoving(2_601))
    }

    @Test fun rapidShakingAndWidelySpacedHandlingNeverBecomeGait() {
        val engine = PetMotionEngine()
        for (index in 0..12) {
            val time = 1_000L + index * 120L
            engine.onAcceleration(0f, 0f, 9.80665f, time)
            engine.onAcceleration(0f, 0f, 13f, time + 50L)
        }
        assertFalse(engine.isMoving(3_000))
        engine.reset()
        for (index in 0..5) {
            val time = 4_000L + index * 3_000L
            engine.onAcceleration(0f, 0f, 9.80665f, time)
            engine.onAcceleration(0f, 0f, 12f, time + 100L)
        }
        assertFalse(engine.isMoving(20_000))
    }

    @Test fun oldStepTimestampsDoNotExtendWalkingAndResetRequiresFreshEvidence() {
        val engine = PetMotionEngine()
        engine.onStepDetected(1_000)
        engine.onStepDetected(1_500)
        engine.onStepDetected(1_100)
        assertFalse(engine.isMoving(8_000))
        engine.reset()
        engine.onStepDetected(8_100)
        assertFalse(engine.isMoving(8_100))
    }
}
