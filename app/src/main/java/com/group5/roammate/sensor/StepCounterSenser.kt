package com.group5.roammate.sensor


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.pow

data class StepData(val steps: Float, val timestamp: Long)

class StepCounterSensor(
    context: Context,
    private val intervalMillis: Long = 100L,
    private val maxPoints: Int = 100,
    private val onStepsUpdated: (Int, Int?) -> Unit = { _, _ -> },
    private val onHistoryRecorded: (StepData) -> Unit = {}
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepHistory = mutableListOf<StepData>()
    private var lastRecordedTime = 0L
    private var listening = false

    fun restoreHistory(savedHistory: List<StepData>) {
        check(!listening) {
            "Cannot restore history while sensor is listening"
        }

        stepHistory.clear()
        stepHistory.addAll(
            savedHistory.sortedBy { it.timestamp }.takeLast(maxPoints)
        )
        lastRecordedTime = stepHistory.lastOrNull()?.timestamp ?: 0L
    }

    fun enableStepCounter(): Boolean {
        if (listening) return true
        val sensor = stepSensor ?: run {
            Log.d("STEP_SENSOR", "TYPE_STEP_COUNTER not found")
            return false
        }
        listening = sensorManager.registerListener(
            this, sensor, SensorManager.SENSOR_DELAY_NORMAL
        )
        Log.d("STEP_SENSOR", "registerListener result = $listening")
        return listening
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val totalSteps = event.values[0].toInt()
        val currentTime = System.currentTimeMillis()


        // Detect step counter reset, clean step history(e.g. device reboot)
        val lastSteps = stepHistory.lastOrNull()?.steps

        if (lastSteps != null && totalSteps.toFloat() < lastSteps) {
            Log.d("STEP_SENSOR", "Step counter reset detected")

            stepHistory.clear()
            lastRecordedTime = 0L
        }

        if (currentTime - lastRecordedTime >= intervalMillis) {
            val record = StepData(totalSteps.toFloat(), currentTime)

            stepHistory.add(record)
            lastRecordedTime = currentTime
            if (stepHistory.size > maxPoints) stepHistory.removeAt(0)
            onHistoryRecorded(record)
        }


        // Send the latest cumulative count and the existing hourly estimate.
        onStepsUpdated(totalSteps, getStepsLastHour())
    }

    // Preserves the original short-window estimation algorithm.
    // With maxPoints = 5 this is an estimate, not an exact one-hour count.
    fun getStepsLastHour(): Int? {

        Log.d(
            "STEP_SENSOR",
            "getStepsLastHour() CALLED! currentTime=${System.currentTimeMillis()}"
        )

        if (stepHistory.size < 2) return null
        val last = stepHistory.last()
        val currentTime = System.currentTimeMillis()
        val targetTime = currentTime - 3_600_000L
        val first = stepHistory
            .filter { it.timestamp < currentTime }
            .minByOrNull { abs(it.timestamp - targetTime) } ?: return null

        val stepDifference = last.steps - first.steps
        if (stepDifference < 0f) return null
        if (stepDifference == 0f) return 0
        val timeDifferenceMinutes = (currentTime - first.timestamp) / 60_000f
        if (timeDifferenceMinutes <= 0f) return null
        val t = timeDifferenceMinutes.coerceIn(0f, 60f)
        if (t <= 0f) return null
        val multiplier = if (timeDifferenceMinutes >= 60f) {
            1f
        } else {
            0.5f + 0.5f * (t / 60f).pow(1.5f)
        }
        val estimatedSteps = stepDifference * (1f + multiplier * (60f / t - 1f))
        Log.d("STEP_SENSOR", "EstimatedSteps=$estimatedSteps")
        return estimatedSteps.toInt()
    }

    fun disableStepCounter() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
