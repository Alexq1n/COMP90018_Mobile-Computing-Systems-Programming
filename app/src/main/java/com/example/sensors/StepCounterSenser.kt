package com.example.sensors

import android.util.Log
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.pow
data class StepData(
    val steps: Float,
    val timestamp: Long
)

class StepCounterSensor(
    context: Context,
    private val intervalMillis: Long = 100L,
    private val maxPoints: Int = 5
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val stepHistory = mutableListOf<StepData>()

    private var lastRecordedTime = 0L

    fun enableStepCounter() {

        if (stepSensor != null) {

            val registered = sensorManager.registerListener(
                this,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            Log.d(
                "STEP_SENSOR",
                "registerListener result = $registered"
            )

        } else {

            Log.d(
                "STEP_SENSOR",
                "TYPE_STEP_COUNTER not found"
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) {
            return
        }

        val totalSteps = event.values[0]
        val currentTime = System.currentTimeMillis()

        Log.d(
            "STEP_SENSOR",
            "Total steps = $totalSteps"
        )

        if (currentTime - lastRecordedTime >= intervalMillis) {

            stepHistory.add(
                StepData(
                    steps = totalSteps,
                    timestamp = currentTime
                )
            )

            lastRecordedTime = currentTime

            if (stepHistory.size > maxPoints) {
                stepHistory.removeAt(0)
            }
        }
    }

    fun getStepsLastHour(): Int? {

        if (stepHistory.size < 2) return 0

        val last = stepHistory.last()

        val currentTime = System.currentTimeMillis()

        val targetTime = currentTime - 3_600_000L

        val first = stepHistory
            .filter { it.timestamp < currentTime }
            .minByOrNull {
                kotlin.math.abs(it.timestamp - targetTime)
            } ?: return null

        val stepDifference = last.steps  - first.steps


        // Filter out small step counts to prevent overestimation from short-duration data.
        if (stepDifference <= 1f) return 0


        val timeDifferenceMinutes =
            (currentTime - first.timestamp) / 60_000f

        if (timeDifferenceMinutes <= 0f) return 0

        // Multiplier：0.3 → 1.0
        val t = timeDifferenceMinutes.coerceIn(0f, 60f)

        val multiplier = if (timeDifferenceMinutes >= 60f) {
            1.0f
        } else {
            val t = timeDifferenceMinutes.coerceIn(0f, 60f)
            0.5f + 0.5f * (t / 60f).pow(1.5f)
        }

        val estimatedSteps = stepDifference *
                (1f + multiplier * (60f / t - 1f))

        Log.d(
            "STEP_SENSOR",
            "ActualSteps=$stepDifference, " +
                    "Minutes=$timeDifferenceMinutes, " +
                    "Multiplier=$multiplier, " +
                    "EstimatedSteps=$estimatedSteps"
        )



        Log.d("STEP_SENSOR", "===== Step History (${stepHistory.size} points) =====")

        stepHistory.forEachIndexed { index, data ->
            Log.d(
                "STEP_SENSOR",
                "[$index] Steps=${data.steps}, Timestamp=${data.timestamp}"
            )
        }

        Log.d("STEP_SENSOR", "===== End Step History =====")

        return estimatedSteps.toInt()
    }






    fun disableStepCounter() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}



