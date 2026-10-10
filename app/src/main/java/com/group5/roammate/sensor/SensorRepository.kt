
package com.group5.roammate.sensor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface provided by the database module.
 * Sensor does not need to know how data is stored.
 */
interface StepHistoryStorage {

    // Read history from the database
    suspend fun loadStepHistory(): List<StepData>

    // Save record to their database
    suspend fun saveStepHistory(record: StepData)
}

/** One shared repository for sensor data and sensor control. */
class SensorRepository(
    context: Context,
    private val stepHistoryStorage: StepHistoryStorage
) {
    private val appContext = context.applicationContext

    // ==================== Step Counter ====================

    private val repositoryScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val historyMutex = Mutex()
    private var historyRestored = false

    // Latest device cumulative steps (since reboot).
    private val _stepCountFlow = MutableStateFlow<Int?>(null)
    val stepCountFlow = _stepCountFlow.asStateFlow()

    // Existing last-hour estimate.
    private val _stepsLastHourFlow = MutableStateFlow<Int?>(null)
    val stepsLastHourFlow = _stepsLastHourFlow.asStateFlow()

    private val stepCounter = StepCounterSensor(
        context = appContext,

        onStepsUpdated = { total, hourly ->
            _stepCountFlow.value = total
            _stepsLastHourFlow.value = hourly
        },

        // [NEW] Sensor -> Database
        onHistoryRecorded = { record ->
            repositoryScope.launch {
                stepHistoryStorage.saveStepHistory(record)
            }
        }
    )

    private var stepStarted = false

    // Restore history before starting Step Counter.
    suspend fun startStepCounter(): Boolean =
        historyMutex.withLock {

            if (!historyRestored) {

                // Database -> Sensor
                val savedHistory =
                    stepHistoryStorage.loadStepHistory()

                // Restore the previous stepHistory
                stepCounter.restoreHistory(savedHistory)

                historyRestored = true
            }

            if (stepStarted) {
                true
            } else {
                stepStarted = stepCounter.enableStepCounter()
                stepStarted
            }
        }

    fun stopStepCounter() {
        if (!stepStarted) return
        stepCounter.disableStepCounter()
        stepStarted = false
    }

    fun getCurrentSteps(): Int? = _stepCountFlow.value

    fun getStepsLastHour(): Int? =
        stepCounter.getStepsLastHour()

    // ==================== Step Detector ====================

    private val stepDetector = StepDetector(appContext)
    val stepEvents = stepDetector.stepEvents

    fun startStepDetection() = stepDetector.start()
    fun stopStepDetection() = stepDetector.stop()
    fun isStepDetectorAvailable(): Boolean =
        stepDetector.isAvailable()

    // ==================== GPS Location ====================

    private val _locationFlow =
        MutableStateFlow<LocationMessage?>(null)

    val locationFlow = _locationFlow.asStateFlow()

    private val locationSensor = LocationSensor(appContext) { location ->
        _locationFlow.value = location
    }

    fun startLocationTracking() =
        locationSensor.enableLocation()

    fun stopLocationTracking() =
        locationSensor.disableLocation()

    fun getCurrentLocation(): LocationMessage? =
        _locationFlow.value

    // ==================== Shake Detector ====================

    private val _shakeEvents =
        MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    val shakeEvents = _shakeEvents.asSharedFlow()

    private val shakeDetector = ShakeDetector(appContext) {
        _shakeEvents.tryEmit(Unit)
    }

    fun startShakeDetection(): Boolean =
        shakeDetector.start()

    fun stopShakeDetection() =
        shakeDetector.stop()
}
