package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StepDetector(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _stepEvents = MutableSharedFlow<Long>(
        extraBufferCapacity = 64
    )

    val stepEvents = _stepEvents.asSharedFlow()

    private var listening = false

    private val listener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR &&
                event.values[0] > 0f
            ) {
                val eventMillis = event.timestamp / 1_000_000L
                _stepEvents.tryEmit(eventMillis)
            }
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int
        ) = Unit
    }

    fun start() {
        if (listening || stepSensor == null) return

        listening = sensorManager.registerListener(
            listener,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        if (!listening) return

        sensorManager.unregisterListener(listener)
        listening = false
    }

    fun isAvailable(): Boolean = stepSensor != null
}