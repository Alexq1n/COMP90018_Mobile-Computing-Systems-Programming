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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sensorRepository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    private var latitude by mutableDoubleStateOf(0.0)
    private var longitude by mutableDoubleStateOf(0.0)
    private var showCamera by mutableStateOf(false)
    private var hasLocationPermission by mutableStateOf(false)
    private lateinit var placeRepository: MockPOI

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // SensorScreen observes this state and starts GPS only while visible.
        hasLocationPermission = granted
        Log.d("GPS_TEST", "Location permission granted = $granted")
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true
    }

    private val activityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && lifecycle.currentState.isAtLeast(
                androidx.lifecycle.Lifecycle.State.STARTED
            )) {
            sensorRepository.startStepCounter()
        } else if (!granted) {
            Log.d("STEP_SENSOR", "Activity recognition permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testSensorList(this)

        hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        lifecycleScope.launch {
            sensorRepository.locationFlow.collect { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                }
            }
        }

        // Observe shake events only while this Activity is resumed.
        // ShakeDetector is owned by the shared SensorRepository.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sensorRepository.shakeEvents.collect {
                    Log.d("SHAKE_SENSOR", "MainActivity received shake event")
                    Toast.makeText(this@MainActivity, "Shake detected!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        placeRepository = MockPOI(this)
        val places = placeRepository.getPlaces()

        setContent {
            if (showCamera) {
                CameraScreen(onBack = { showCamera = false })
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
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onCameraClick = {
                        if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            showCamera = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Step Counter remains owned by MainActivity.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sensorRepository.startStepCounter()
        } else {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    override fun onResume() {
        super.onResume()
        // Temporary test: enable shake while MainActivity is interactive.
        val started = sensorRepository.startShakeDetection()
        Log.d("SHAKE_SENSOR", "MainActivity shake listener started = $started")
    }

    override fun onPause() {
        // Stop accelerometer sampling while the Activity is not resumed.
        sensorRepository.stopShakeDetection()
        Log.d("SHAKE_SENSOR", "MainActivity shake listener stopped")
        super.onPause()
    }

    override fun onStop() {
        sensorRepository.stopStepCounter()
        super.onStop()
    }
}
