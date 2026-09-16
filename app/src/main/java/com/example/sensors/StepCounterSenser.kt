package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager


class StepCounterSensor(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val stepHistory = mutableListOf<Float>()

    private var lastRecordedTime = 0L

    fun enableStepCounter() {


        if (stepSensor != null) {
            sensorManager.registerListener(
                this,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) {
            return
        }

        val totalSteps = event.values[0]
        val currentTime = System.currentTimeMillis()

        // Only save one value every minute
        if (currentTime - lastRecordedTime >= 60_000L) {

            stepHistory.add(totalSteps)

            lastRecordedTime = currentTime

            // Keep only the latest 60 points
            if (stepHistory.size > 60) {
                stepHistory.removeAt(0)
            }
        }
    }

    fun getStepsLastHour(): Float? {

        if (stepHistory.size < 2) {
            return null
        }

        return stepHistory.last() - stepHistory.first()
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