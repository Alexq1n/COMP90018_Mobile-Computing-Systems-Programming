package com.example.sensors

import android.util.Log
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

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

        Log.d(
            "STEP_SENSOR",
            "Total recorded points = ${stepHistory.size}"
        )

        if (stepHistory.size < 2) {

            Log.d(
                "STEP_SENSOR",
                "You need more data"
            )

            return null
        }

        val first = stepHistory.first()
        val last = stepHistory.last()

        Log.d(
            "STEP_SENSOR",
            "First: steps=${first.steps}, timestamp=${first.timestamp}"
        )

        Log.d(
            "STEP_SENSOR",
            "Last: steps=${last.steps}, timestamp=${last.timestamp}"
        )

        val stepDifference =
            last.steps - first.steps

        val timeDifferenceHours =
            (last.timestamp - first.timestamp) / 3_600_000f


        val stepsPerHour =
            stepDifference / timeDifferenceHours

        Log.d(
            "STEP_SENSOR",
            "steps/hour=$stepsPerHour"
        )

        return stepsPerHour.toInt()
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



