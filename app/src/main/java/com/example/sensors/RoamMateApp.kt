package com.example.sensors

import android.app.Application

class RoamMateApp : Application() {

    val sensorRepository: SensorRepository by lazy {
        SensorRepository(applicationContext)
    }
}