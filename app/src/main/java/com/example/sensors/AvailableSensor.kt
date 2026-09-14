package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log

fun testSensorList(context: Context){
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

    for (sensor in sensors) {
        Log.d(
            "SensorTest",
            "name=${sensor.name}, type=${sensor.type}"
        )
    }
}


