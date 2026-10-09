package com.group5.roammate.sensor

import android.app.Application

class RoamMateApp : Application() {

    val sensorRepository: SensorRepository by lazy {
        SensorRepository(applicationContext)
    }
}