
package com.group5.roammate.ui.sensor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group5.roammate.sensor.SensorRepository

private const val TAG = "RoamMateSensor"

@Composable
fun SensorTestScreen(
    sensorRepository: SensorRepository,
    onBack: () -> Unit
) {

    // Step Counter: automatically receive step count updates.
    val steps by sensorRepository.stepCountFlow
        .collectAsStateWithLifecycle()

    // Last Hour Steps: receive the latest published hourly estimate.
    val lastHourSteps by sensorRepository.stepsLastHourFlow
        .collectAsStateWithLifecycle()

    // GPS: automatically receive location updates.
    val location by sensorRepository.locationFlow
        .collectAsStateWithLifecycle()

    // Manual results: store values returned by sensor functions.
    var manualSteps by remember {
        mutableStateOf<String?>(null)
    }

    var manualLastHour by remember {
        mutableStateOf<String?>(null)
    }

    var manualLocation by remember {
        mutableStateOf<String?>(null)
    }

    // Step Detector: count each detected step event.
    var stepEvents by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(sensorRepository) {
        sensorRepository.stepEvents.collect { timestamp ->
            stepEvents++
            Log.d(TAG, "Step detected: $timestamp")
        }
    }

    // Shake Detector: count each detected shake event.
    var shakeEvents by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(sensorRepository) {
        sensorRepository.shakeEvents.collect {
            shakeEvents++
            Log.d(TAG, "Shake detected!")
        }
    }

    // Sensor Test UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Sensor Test",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = onBack) {
            Text("Back")
        }

        HorizontalDivider()

        // 1. Step Counter: test automatic updates and manual step retrieval.
        Text(
            text = "1. Step Counter",
            style = MaterialTheme.typography.titleLarge
        )

        Text("StateFlow: ${steps ?: "Waiting..."}")

        Button(
            onClick = {
                val result = sensorRepository.getCurrentSteps()
                manualSteps = result.toString()
                Log.d(TAG, "getCurrentSteps(): $result")
            }
        ) {
            Text("Get Current Steps")
        }

        Text("Manual: ${manualSteps ?: "Not requested"}")

        HorizontalDivider()

        // 2. Last Hour Steps: compare StateFlow with manual calculation.
        Text(
            text = "2. Steps Last Hour",
            style = MaterialTheme.typography.titleLarge
        )

        Text("StateFlow: ${lastHourSteps ?: "Waiting..."}")

        Button(
            onClick = {
                val result = sensorRepository.getStepsLastHour()
                manualLastHour = result?.toString() ?: "No data"
                Log.d(TAG, "getStepsLastHour(): $result")
            }
        ) {
            Text("Get Last Hour Steps")
        }

        Text("Manual: ${manualLastHour ?: "Not requested"}")

        HorizontalDivider()

        // 3. Step Detector: test individual step detection events.
        Text(
            text = "3. Step Detector",
            style = MaterialTheme.typography.titleLarge
        )

        Text("Step Events: $stepEvents")
        Text("Walk with your phone to test.")

        HorizontalDivider()

        // 4. GPS: test automatic location updates and manual retrieval.
        Text(
            text = "4. GPS",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "StateFlow: " +
                    (location?.let {
                        "${it.latitude}, ${it.longitude}"
                    } ?: "Waiting...")
        )

        Button(
            onClick = {
                val result = sensorRepository.getCurrentLocation()

                manualLocation = result?.let {
                    "${it.latitude}, ${it.longitude}"
                } ?: "No location"

                Log.d(TAG, "getCurrentLocation(): $result")
            }
        ) {
            Text("Get Current Location")
        }

        Text("Manual: ${manualLocation ?: "Not requested"}")

        HorizontalDivider()

        // 5. Shake Detector: test phone shake detection events.
        Text(
            text = "5. Shake Detector",
            style = MaterialTheme.typography.titleLarge
        )

        Text("Shake Events: $shakeEvents")
        Text("Shake your phone to test.")
    }
}
