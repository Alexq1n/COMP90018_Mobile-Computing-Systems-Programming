package com.example.sensors

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.flow.StateFlow

/** Bound service: one owner can start/stop shared step counting. */
class SensorService : Service() {

    private val repository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    private val binder = LocalBinder()
    private var ownsStepCounter = false

    inner class LocalBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // Call after binding, with ACTIVITY_RECOGNITION permission granted.
    fun startStepCounting(): Boolean {
        if (ownsStepCounter) return true
        ownsStepCounter = repository.startStepCounter()
        return ownsStepCounter
    }

    fun stopStepCounting() {
        if (!ownsStepCounter) return
        repository.stopStepCounter()
        ownsStepCounter = false
    }

    // Multiple screens can subscribe to the same repository-backed StateFlow.
    val stepCountFlow: StateFlow<Int?>
        get() = repository.stepCountFlow

    val stepsLastHourFlow: StateFlow<Int?>
        get() = repository.stepsLastHourFlow

    fun getCurrentSteps(): Int? = repository.getCurrentSteps()
    fun getStepsLastHour(): Int? = repository.getStepsLastHour()
    fun getCurrentLocation(): LocationMessage? = repository.getCurrentLocation()

    override fun onDestroy() {
        // Release only the sensor this service explicitly started.
        stopStepCounting()
        super.onDestroy()
    }
}
