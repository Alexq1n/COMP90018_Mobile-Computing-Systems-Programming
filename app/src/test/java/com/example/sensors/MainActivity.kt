
package com.example.sensors

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.core.content.ContextCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ==================== Repository ====================

    private val sensorRepository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    // ==================== UI State ====================

    private var latitude by mutableDoubleStateOf(0.0)
    private var longitude by mutableDoubleStateOf(0.0)

    private var showCamera by mutableStateOf(false)
    private var hasLocationPermission by mutableStateOf(false)

    // NEW: Step Detector test count
    private var detectedSteps by mutableIntStateOf(0)

    private lateinit var placeRepository: MockPOI

    // ==================== Location Permission ====================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasLocationPermission = granted

            Log.d(
                "GPS_TEST",
                "Location permission granted = $granted"
            )
        }

    // ==================== Camera Permission ====================

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                showCamera = true
            }
        }

    // ==================== Activity Recognition Permission ====================

    private val activityPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (
                granted &&
                lifecycle.currentState.isAtLeast(
                    Lifecycle.State.STARTED
                )
            ) {
                startStepSensors()

            } else if (!granted) {

                Log.d(
                    "STEP_SENSOR",
                    "Activity recognition permission denied"
                )
            }
        }

    // ==================== Step Sensor Startup ====================

    private fun startStepSensors() {

        // Existing Step Counter
        sensorRepository.startStepCounter()

        Log.d(
            "STEP_SENSOR",
            "Step Counter start requested"
        )

        // NEW: Step Detector
        if (sensorRepository.isStepDetectorAvailable()) {

            sensorRepository.startStepDetection()

            Log.d(
                "STEP_DETECTOR",
                "Step Detector start requested"
            )

        } else {

            Log.d(
                "STEP_DETECTOR",
                "Step Detector is not available on this device"
            )
        }
    }

    // ==================== onCreate ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Print available sensors
        testSensorList(this)

        // Check existing GPS permission
        hasLocationPermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // ==================== GPS Flow ====================

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                sensorRepository.locationFlow.collect { location ->

                    if (location != null) {

                        latitude = location.latitude
                        longitude = location.longitude

                        Log.d(
                            "GPS_TEST",
                            "Latitude=$latitude, Longitude=$longitude"
                        )
                    }
                }
            }
        }

        // ==================== Shake Detector Flow ====================

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.RESUMED) {

                sensorRepository.shakeEvents.collect {

                    Log.d(
                        "SHAKE_SENSOR",
                        "MainActivity received shake event"
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Shake detected!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // ==================== NEW: Step Detector Flow ====================

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                sensorRepository.stepEvents.collect { eventMillis ->

                    detectedSteps += 1

                    Log.d(
                        "STEP_DETECTOR",
                        "Step detected! Count=$detectedSteps, " +
                                "time=$eventMillis"
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Step detected! Count=$detectedSteps",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // ==================== POI ====================

        placeRepository = MockPOI(this)

        val places = placeRepository.getPlaces()

        // ==================== Compose UI ====================

        setContent {

            if (showCamera) {

                CameraScreen(
                    onBack = {
                        showCamera = false
                    }
                )

            } else {

                SensorScreen(
                    x = 3.14f,
                    y = 3.14f,
                    z = 3.14f,
                    light = 3.14f,

                    latitude = latitude,
                    longitude = longitude,

                    places = places,
                    repository = sensorRepository,

                    hasLocationPermission = hasLocationPermission,

                    requestLocationPermission = {

                        locationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    },

                    onCameraClick = {

                        if (
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {

                            showCamera = true

                        } else {

                            cameraPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                        }
                    }
                )
            }
        }
    }

    // ==================== onStart ====================

    override fun onStart() {
        super.onStart()

        // Android 10+ requires ACTIVITY_RECOGNITION permission
        val hasActivityPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED

        if (hasActivityPermission) {

            // Start both Step Counter and Step Detector
            startStepSensors()

        } else {

            activityPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }
    }

    // ==================== onResume ====================

    override fun onResume() {
        super.onResume()

        // Temporary Shake Detector test
        val started =
            sensorRepository.startShakeDetection()

        Log.d(
            "SHAKE_SENSOR",
            "MainActivity shake listener started = $started"
        )
    }

    // ==================== onPause ====================

    override fun onPause() {

        sensorRepository.stopShakeDetection()

        Log.d(
            "SHAKE_SENSOR",
            "MainActivity shake listener stopped"
        )

        super.onPause()
    }

    // ==================== onStop ====================

    override fun onStop() {

        // NEW: Stop Step Detector
        sensorRepository.stopStepDetection()

        Log.d(
            "STEP_DETECTOR",
            "Step Detector stopped"
        )

        // Existing Step Counter
        sensorRepository.stopStepCounter()

        Log.d(
            "STEP_SENSOR",
            "Step Counter stopped"
        )

        super.onStop()
    }
}
