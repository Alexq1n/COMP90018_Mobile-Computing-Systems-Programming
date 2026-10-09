package com.example.sensors

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** One shared repository for sensor data and sensor control. */
class SensorRepository(context: Context) {
    private val appContext = context.applicationContext

    // ==================== Step Counter ====================

    // Latest device cumulative steps (since reboot), null until first reading.
    private val _stepCountFlow = MutableStateFlow<Int?>(null)
    val stepCountFlow = _stepCountFlow.asStateFlow()

    // Existing last-hour estimate, updated when a new sensor reading arrives.
    private val _stepsLastHourFlow = MutableStateFlow<Int?>(null)
    val stepsLastHourFlow = _stepsLastHourFlow.asStateFlow()

    private val stepCounter = StepCounterSensor(appContext) { total, hourly ->
        _stepCountFlow.value = total
        _stepsLastHourFlow.value = hourly
    }
    private var stepStarted = false

    fun startStepCounter(): Boolean {
        if (stepStarted) return true
        stepStarted = stepCounter.enableStepCounter()
        return stepStarted
    }

    fun stopStepCounter() {
        if (!stepStarted) return
        stepCounter.disableStepCounter()
        stepStarted = false
    }

    fun getCurrentSteps(): Int? = _stepCountFlow.value

    // Keeps the original function-call API.
    fun getStepsLastHour(): Int? = stepCounter.getStepsLastHour()

    // ==================== Step Detector ====================

    private val stepDetector = StepDetector(appContext)
    val stepEvents = stepDetector.stepEvents // SharedFlow: one event per step.

    fun startStepDetection() = stepDetector.start()
    fun stopStepDetection() = stepDetector.stop()
    fun isStepDetectorAvailable(): Boolean = stepDetector.isAvailable()

    // ==================== GPS Location ====================

    private val _locationFlow = MutableStateFlow<LocationMessage?>(null)
    val locationFlow = _locationFlow.asStateFlow() // Latest cached location.

    private val locationSensor = LocationSensor(appContext) { location ->
        _locationFlow.value = location
    }

    fun startLocationTracking() = locationSensor.enableLocation()
    fun stopLocationTracking() = locationSensor.disableLocation()
    fun getCurrentLocation(): LocationMessage? = _locationFlow.value

    // ==================== Shake Detector ====================

    private val _shakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val shakeEvents = _shakeEvents.asSharedFlow() // One event per shake.

    private val shakeDetector = ShakeDetector(appContext) {
        _shakeEvents.tryEmit(Unit)
    }

    fun startShakeDetection(): Boolean = shakeDetector.start()
    fun stopShakeDetection() = shakeDetector.stop()
}
