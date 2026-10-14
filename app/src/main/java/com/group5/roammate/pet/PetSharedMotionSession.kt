package com.group5.roammate.pet

/** Adapts one foreground visit to shared events using only the monotonic sensor clock. */
class PetSharedMotionSession(
    private val source: PetMotionSource,
    private val startedAtMillis: Long,
) {
    private val engine = PetMotionEngine()

    fun onStepEvent(eventMillis: Long, receivedAtMillis: Long) {
        if (source == PetMotionSource.StepDetector &&
            eventMillis in startedAtMillis..receivedAtMillis
        ) engine.onStepDetected(eventMillis)
    }

    fun onCounter(total: Int?, receivedAtMillis: Long) {
        if (source == PetMotionSource.StepCounter && receivedAtMillis >= startedAtMillis && total != null) {
            engine.onStepCounter(total.toFloat(), receivedAtMillis)
        }
    }

    fun isMoving(nowMillis: Long): Boolean = engine.isMoving(nowMillis)
}
