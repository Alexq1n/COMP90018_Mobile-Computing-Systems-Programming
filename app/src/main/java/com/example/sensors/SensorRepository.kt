package com.example.sensors

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** One repository shared by the app; individual screens control sensor registration. */
class SensorRepository(context: Context) {
    private val appContext = context.applicationContext

    // Existing step counter functionality.
    private val stepCounter = StepCounterSensor(appContext)
    private var stepStarted = false

    fun startStepCounter() {
        if (stepStarted) return
        stepCounter.enableStepCounter()
        stepStarted = true
    }

    fun stopStepCounter() {
        if (!stepStarted) return
        stepCounter.disableStepCounter()
        stepStarted = false
    }

    fun getStepsLastHour(): Int? = stepCounter.getStepsLastHour()

    // Existing GPS functionality.
    private val _locationFlow = MutableStateFlow<LocationMessage?>(null)
    val locationFlow = _locationFlow.asStateFlow()
    private val locationSensor = LocationSensor(appContext) { location ->
        _locationFlow.value = location
    }

    fun startLocationTracking() = locationSensor.enableLocation()
    fun stopLocationTracking() = locationSensor.disableLocation()
    fun getCurrentLocation(): LocationMessage? = _locationFlow.value

    // New shake event stream: each event represents one detected shake.
    private val _shakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val shakeEvents = _shakeEvents.asSharedFlow()
    private val shakeDetector = ShakeDetector(appContext) {
        _shakeEvents.tryEmit(Unit)
    }

    fun startShakeDetection(): Boolean = shakeDetector.start()
    fun stopShakeDetection() = shakeDetector.stop()
}
