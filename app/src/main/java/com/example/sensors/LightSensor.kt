package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.greenrobot.eventbus.EventBus

class LightSensor(context: Context) : SensorEventListener {


    private val manager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val primarySensor = Sensor.TYPE_LIGHT

    private var foundSensor: Sensor? = null

    fun enableSensor() {

        foundSensor = manager.getDefaultSensor(primarySensor)

        foundSensor?.let { sensor ->

            manager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun disableSensor() {

        manager.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        if (sensorEvent == null) return

        val light = sensorEvent.values[0]

        EventBus.getDefault().post(
            LightSensorMessage(
                light
            )
        )
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Nothing needed here
    }

}
