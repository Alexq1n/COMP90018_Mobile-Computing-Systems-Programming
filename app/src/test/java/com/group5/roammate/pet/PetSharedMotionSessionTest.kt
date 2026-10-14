package com.group5.roammate.pet

import org.junit.Assert.*
import org.junit.Test

class PetSharedMotionSessionTest {
    @Test fun sharedStepEventsStartAndThenExpireWalking() {
        val session = PetSharedMotionSession(PetMotionSource.StepDetector, 1_000L)
        session.onStepEvent(1_100L, 1_100L)
        assertFalse(session.isMoving(1_100L))
        session.onStepEvent(1_700L, 1_700L)
        assertTrue(session.isMoving(1_700L))
        assertFalse(session.isMoving(8_200L))
    }

    @Test fun previousVisitAndFutureEventsAreIgnored() {
        val session = PetSharedMotionSession(PetMotionSource.StepDetector, 5_000L)
        session.onStepEvent(4_000L, 5_100L)
        session.onStepEvent(4_600L, 5_200L)
        session.onStepEvent(9_000L, 5_300L)
        session.onStepEvent(9_600L, 5_400L)
        assertFalse(session.isMoving(9_600L))
    }

    @Test fun replayedCumulativeCountIsABaselineRatherThanMovement() {
        val session = PetSharedMotionSession(PetMotionSource.StepCounter, 1_000L)
        session.onCounter(12_000, 1_000L)
        session.onCounter(12_000, 1_500L)
        assertFalse(session.isMoving(1_500L))
        session.onCounter(12_001, 1_800L)
        session.onCounter(12_002, 2_400L)
        assertTrue(session.isMoving(2_400L))
    }

    @Test fun returningToTheScreenNeedsFreshEvidence() {
        val old = PetSharedMotionSession(PetMotionSource.StepCounter, 1_000L)
        old.onCounter(100, 1_000L)
        old.onCounter(102, 1_600L)
        assertTrue(old.isMoving(1_600L))
        val resumed = PetSharedMotionSession(PetMotionSource.StepCounter, 2_000L)
        resumed.onCounter(102, 2_000L)
        assertFalse(resumed.isMoving(2_000L))
    }

    @Test fun unavailableOrWrongSourcesCannotCreateMovement() {
        val unavailable = PetSharedMotionSession(PetMotionSource.Unavailable, 1_000L)
        unavailable.onCounter(10, 1_100L)
        unavailable.onCounter(12, 1_600L)
        unavailable.onStepEvent(1_100L, 1_100L)
        unavailable.onStepEvent(1_600L, 1_600L)
        assertFalse(unavailable.isMoving(1_600L))
        val detector = PetSharedMotionSession(PetMotionSource.StepDetector, 1_000L)
        detector.onCounter(10, 1_100L)
        detector.onCounter(12, 1_600L)
        assertFalse(detector.isMoving(1_600L))
    }
}
