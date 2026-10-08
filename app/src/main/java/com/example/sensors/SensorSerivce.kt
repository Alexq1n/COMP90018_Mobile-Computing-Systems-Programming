package com.example.sensors

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class SensorService : Service() {

    private val repository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    override fun onCreate() {
        super.onCreate()

        // Do not automatically start every sensor here.
        // Each sensor has its own lifecycle owner.
    }

    fun getCurrentLocation(): LocationMessage? {
        return repository.getCurrentLocation()
    }

    fun getStepsLastHour(): Int? {
        return repository.getStepsLastHour()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        // Do not stop shared sensors here.
        // They may still be used by MainActivity or other screens.
        super.onDestroy()
    }
}