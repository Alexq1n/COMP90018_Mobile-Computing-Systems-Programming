package com.example.sensors

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/** Manual test screen for the four shared sensors and the bound SensorService. */
class MainActivity : ComponentActivity() {

    private val repository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    // UI state. These are updated by Flow collectors and button callbacks.
    private var totalSteps by mutableStateOf<Int?>(null)
    private var hourlySteps by mutableStateOf<Int?>(null)
    private var latitude by mutableStateOf<Double?>(null)
    private var longitude by mutableStateOf<Double?>(null)
    private var stepEventsReceived by mutableIntStateOf(0)
    private var lastStepTime by mutableStateOf<Long?>(null)
    private var shakesReceived by mutableIntStateOf(0)
    private var stepCounterStatus by mutableStateOf("Not started")
    private var stepDetectorStatus by mutableStateOf("Not started")
    private var gpsStatus by mutableStateOf("Not started")
    private var shakeStatus by mutableStateOf("Not started")
    private var serviceStatus by mutableStateOf("Not bound")
    private var functionResult by mutableStateOf("No function calls yet")
    private var isServiceBound by mutableStateOf(false)
    private var sensorService: SensorService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? SensorService.LocalBinder
            sensorService = localBinder?.getService()
            isServiceBound = sensorService != null
            serviceStatus = if (isServiceBound) "Bound" else "Binding failed"
            Log.d("SENSOR_SERVICE_TEST", serviceStatus)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sensorService = null
            isServiceBound = false
            serviceStatus = "Disconnected"
        }
    }

    // Android 10+ requires ACTIVITY_RECOGNITION for step sensors.
    private val activityPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            functionResult = "Activity recognition permission: $granted"
            if (granted) {
                // Permission only: use the relevant Start button again.
                Log.d("SENSOR_PERMISSION", "ACTIVITY_RECOGNITION granted")
            }
        }

    // Request both foreground location permissions together.
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            functionResult = "Foreground location permission: $granted"
            if (!granted) gpsStatus = "Location permission denied"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // StateFlow: new collectors immediately receive the latest values.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repository.stepCountFlow.collect { totalSteps = it }
                }
                launch {
                    repository.stepsLastHourFlow.collect { hourlySteps = it }
                }
                launch {
                    repository.locationFlow.collect { location ->
                        latitude = location?.latitude
                        longitude = location?.longitude
                    }
            }
            // SharedFlow: receive new events while STARTED.
            launch {
                repository.stepEvents.collect { time ->
                    stepEventsReceived++
                    lastStepTime = time
                    Log.d("STEP_DETECTOR_TEST", "Step event at $time")
                }
            }
            launch {
                repository.shakeEvents.collect {
                    shakesReceived++
                    Log.d("SHAKE_TEST", "Shake event #$shakesReceived")
                }
            }
        }
    }

    setContent {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("RoamMate Sensor Test", style = MaterialTheme.typography.headlineSmall)
                    Text("StateFlow values update automatically; SharedFlow counters update on events.")

                    SectionTitle("1. Permissions")
                    TestButton("Request activity recognition") { requestActivityPermission() }
                    TestButton("Request GPS location") { requestLocationPermission() }
                    Text("Step permission: ${hasActivityPermission()}")
                    Text("GPS permission: ${hasLocationPermission()}")

                    SectionTitle("2. SensorService (Bound Service)")
                    Text("Service: $serviceStatus")
                    ButtonRow(
                        "Bind Service", { bindSensorService() },
                        "Unbind Service", { unbindSensorService() }
                    )
                    ButtonRow(
                        "Start Step Counter via Service", { startStepCounterViaService() },
                        "Stop Step Counter via Service", { stopStepCounterViaService() }
                    )
                    Text("Step Counter: $stepCounterStatus")

                    SectionTitle("3. Step Counter — StateFlow + Function")
                    Text("Live cumulative steps (StateFlow): ${totalSteps ?: "Waiting for sensor"}")
                    Text("Live last-hour estimate (StateFlow): ${hourlySteps ?: "Waiting for sensor"}")
                    ButtonRow(
                        "Get Current Steps", {
                            functionResult = "getCurrentSteps() = ${repository.getCurrentSteps()}"
                        },
                        "Get Steps Last Hour", {
                            functionResult = "getStepsLastHour() = ${repository.getStepsLastHour()}"
                        }
                    )
                    TestButton("Get Steps via Bound Service") {
                        val service = sensorService
                        functionResult = if (service == null) "Bind Service first" else
                            "Service: current=${service.getCurrentSteps()}, lastHour=${service.getStepsLastHour()}"
                    }

                    SectionTitle("4. Step Detector — SharedFlow")
                    Text("Status: $stepDetectorStatus")
                    Text("Available: ${repository.isStepDetectorAvailable()}")
                    Text("Events received: $stepEventsReceived")
                    Text("Last event timestamp (elapsed realtime ms): ${lastStepTime ?: "None"}")
                    ButtonRow(
                        "Start Step Detector", { startStepDetector() },
                        "Stop Step Detector", {
                            repository.stopStepDetection()
                            stepDetectorStatus = "Stopped"
                        }
                    )
                    TestButton("Reset Step Event Display") { stepEventsReceived = 0; lastStepTime = null }

                    SectionTitle("5. GPS — StateFlow + Function")
                    Text("Status: $gpsStatus")
                    Text("Live latitude (StateFlow): ${latitude ?: "Waiting for GPS"}")
                    Text("Live longitude (StateFlow): ${longitude ?: "Waiting for GPS"}")
                    ButtonRow(
                        "Start GPS", { startGps() },
                        "Stop GPS", { repository.stopLocationTracking(); gpsStatus = "Stopped" }
                    )
                    TestButton("Get Current Location (Function)") {
                        val loc = repository.getCurrentLocation()
                        functionResult = if (loc == null) "getCurrentLocation() = null" else
                            "getCurrentLocation() = ${loc.latitude}, ${loc.longitude}"
                    }
                    Text("Note: stopping GPS does not clear its last cached location.")

                    SectionTitle("6. Shake — SharedFlow")
                    Text("Status: $shakeStatus")
                    Text("Shake events received: $shakesReceived")
                    ButtonRow(
                        "Start Shake", {
                            shakeStatus = if (repository.startShakeDetection()) "Listening" else "Failed to start"
                        },
                        "Stop Shake", { repository.stopShakeDetection(); shakeStatus = "Stopped" }
                    )
                    TestButton("Reset Shake Display") { shakesReceived = 0 }

                    SectionTitle("7. Function Call Result")
                    Text(functionResult)
                }
            }
        }
    }
}

private fun hasActivityPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

private fun hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

private fun requestActivityPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission()) {
        activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    } else functionResult = "Activity recognition already granted / not required"
}

private fun requestLocationPermission() {
    if (!hasLocationPermission()) {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    } else functionResult = "Foreground location already granted"
}

private fun bindSensorService() {
    if (isServiceBound) { serviceStatus = "Already bound"; return }
    serviceStatus = "Binding..."
    val requested = bindService(
        Intent(this, SensorService::class.java),
        serviceConnection,
        Context.BIND_AUTO_CREATE
    )
    if (!requested) serviceStatus = "bindService() returned false"
}

private fun unbindSensorService() {
    if (!isServiceBound) { serviceStatus = "Not bound"; return }
    // Stop before unbinding: this test Activity is the service's only owner.
    sensorService?.stopStepCounting()
    unbindService(serviceConnection)
    sensorService = null
    isServiceBound = false
    serviceStatus = "Unbound"
    stepCounterStatus = "Stopped (service unbound)"
}

private fun startStepCounterViaService() {
    if (!hasActivityPermission()) {
        stepCounterStatus = "Grant activity recognition first"
        requestActivityPermission()
        return
    }
    val service = sensorService
    stepCounterStatus = when {
        service == null -> "Bind Service first"
        service.startStepCounting() -> "Listening (via Service)"
        else -> "Failed (sensor unavailable or registration failed)"
    }
}

private fun stopStepCounterViaService() {
    if (sensorService == null) { stepCounterStatus = "Bind Service first"; return }
    sensorService?.stopStepCounting()
    stepCounterStatus = "Stopped"
}

private fun startStepDetector() {
    if (!hasActivityPermission()) {
        stepDetectorStatus = "Grant activity recognition first"
        requestActivityPermission()
        return
    }
    if (!repository.isStepDetectorAvailable()) {
        stepDetectorStatus = "TYPE_STEP_DETECTOR unavailable"
        return
    }
    repository.startStepDetection()
    stepDetectorStatus = "Start requested; walk to generate events"
}

private fun startGps() {
    if (!hasLocationPermission()) {
        gpsStatus = "Grant location permission first"
        requestLocationPermission()
        return
    }
    repository.startLocationTracking()
    gpsStatus = "Start requested; waiting for location"
}

override fun onDestroy() {
    // This screen owns Step Detector, GPS and Shake test registrations.
    repository.stopStepDetection()
    repository.stopLocationTracking()
    repository.stopShakeDetection()
    if (isServiceBound) unbindSensorService()
    super.onDestroy()
}
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun TestButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun ButtonRow(
    leftLabel: String,
    leftAction: () -> Unit,
    rightLabel: String,
    rightAction: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = leftAction, modifier = Modifier.weight(1f)) { Text(leftLabel) }
        OutlinedButton(onClick = rightAction, modifier = Modifier.weight(1f)) { Text(rightLabel) }
    }
}
