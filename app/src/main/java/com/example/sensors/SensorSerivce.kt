package com.example.sensors

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class SensorService : Service() {

    private lateinit var locationSensor: LocationSensor
    private lateinit var stepCounter: StepCounterSensor

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SensorService {
            return this@SensorService
        }
    }

    override fun onCreate() {
        super.onCreate()

        locationSensor = LocationSensor(this)
        stepCounter = StepCounterSensor(this)

        locationSensor.enableLocation()
        stepCounter.enableStepCounter()
    }

    fun getCurrentLocation(): LocationMessage? {
        return locationSensor.getCurrentLocation()
    }

    fun getStepsLastHour(): Float? {
        return stepCounter.getStepsLastHour()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        locationSensor.disableLocation()
        stepCounter.disableStepCounter()

        super.onDestroy()
    }
}