package com.example.sensors

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorRepository(context: Context) {

    private val appContext = context.applicationContext

    private val stepCounter = StepCounterSensor(appContext)

    private val _locationFlow =
        MutableStateFlow<LocationMessage?>(null)

    val locationFlow = _locationFlow.asStateFlow()

    private val locationSensor = LocationSensor(
        context = appContext,
        onLocationChanged = { location ->
            _locationFlow.value = location
        }
    )

    private var stepCounterStarted = false
    private var locationStarted = false

    // =========================
    // Step Counter
    // =========================

    fun startStepCounter() {
        if (stepCounterStarted) return

        stepCounter.enableStepCounter()
        stepCounterStarted = true
    }

    fun stopStepCounter() {
        if (!stepCounterStarted) return

        stepCounter.disableStepCounter()
        stepCounterStarted = false
    }

    fun getStepsLastHour(): Int? {
        return stepCounter.getStepsLastHour()
    }

    // =========================
    // GPS
    // =========================

    fun startLocationTracking() {
        if (locationStarted) return

        locationSensor.enableLocation()
        locationStarted = true
    }

    fun stopLocationTracking() {
        if (!locationStarted) return

        locationSensor.disableLocation()
        locationStarted = false
    }

    fun getCurrentLocation(): LocationMessage? {
        return locationFlow.value
    }

    // =========================
    // All Sensors
    // =========================

    fun stopAllSensors() {
        stopStepCounter()
        stopLocationTracking()
    }
}