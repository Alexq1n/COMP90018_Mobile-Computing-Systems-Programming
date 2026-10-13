package com.group5.roammate.ui.sensor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.group5.roammate.sensor.SensorRepository
import com.group5.roammate.ui.screens.AddStopPlace


private const val TAG = "RoamMateSensor"
private const val SEARCH_TAG = "SearchTest"

// =====================================================
// Display API values in a more visible way
// =====================================================

@Composable
private fun SensorValue(value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "VALUE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}







// =====================================================
// Sensor Test Screen
// =====================================================

@Composable
fun SensorTestScreen(
    sensorRepository: SensorRepository,
    onBack: () -> Unit,
    searchPlacesByName: (
        query: String,
        latitude: Double?,
        longitude: Double?
    ) -> List<AddStopPlace>
) {

    // ==================== StateFlow APIs ====================

    val steps by sensorRepository.stepCountFlow
        .collectAsStateWithLifecycle()

    val lastHourSteps by sensorRepository.stepsLastHourFlow
        .collectAsStateWithLifecycle()

    val location by sensorRepository.locationFlow
        .collectAsStateWithLifecycle()


    // ==================== Manual Function Results ====================

    var manualSteps by remember {
        mutableStateOf<String?>(null)
    }

    var manualLastHour by remember {
        mutableStateOf<String?>(null)
    }

    var manualLocation by remember {
        mutableStateOf<String?>(null)
    }


    // ==================== Step Detector ====================

    var stepEvents by remember {
        mutableIntStateOf(0)
    }

    var lastStepTimestamp by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(sensorRepository) {
        sensorRepository.stepEvents.collect { timestamp ->
            stepEvents++
            lastStepTimestamp = timestamp

            Log.d(TAG, "Step detected: $timestamp")
        }
    }


    // ==================== Shake Detector ====================

    var shakeEvents by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(sensorRepository) {
        sensorRepository.shakeEvents.collect {
            shakeEvents++

            Log.d(TAG, "Shake detected!")
        }
    }


    // ==================== POI Search Test ====================

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember {
        mutableStateOf<List<AddStopPlace>>(emptyList())
    }
    var searchStatus by remember { mutableStateOf("Not searched") }
    var searchLocationText by remember { mutableStateOf("Not searched") }


    // ==================== Sensor Test UI ====================

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


        // =====================================================
        // 1. Step Counter
        // =====================================================

        Text(
            text = "1. Step Counter",
            style = MaterialTheme.typography.titleLarge
        )

        Text("API: stepCountFlow")
        Text("Type: StateFlow<Int?>")

        SensorValue(
            value = steps?.toString() ?: "null"
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("API: getCurrentSteps()")
        Text("Type: Function -> Int?")

        Button(
            onClick = {
                val result = sensorRepository.getCurrentSteps()

                manualSteps = result?.toString() ?: "null"

                Log.d(TAG, "getCurrentSteps(): $result")
            }
        ) {
            Text("Get Current Steps")
        }

        SensorValue(
            value = manualSteps ?: "Not requested"
        )

        HorizontalDivider()


        // =====================================================
        // 2. Steps Last Hour
        // =====================================================

        Text(
            text = "2. Steps Last Hour",
            style = MaterialTheme.typography.titleLarge
        )

        Text("API: stepsLastHourFlow")
        Text("Type: StateFlow<Int?>")

        SensorValue(
            value = lastHourSteps?.toString() ?: "null"
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("API: getStepsLastHour()")
        Text("Type: Function -> Int?")

        Button(
            onClick = {
                val result = sensorRepository.getStepsLastHour()

                manualLastHour = result?.toString() ?: "null"

                Log.d(TAG, "getStepsLastHour(): $result")
            }
        ) {
            Text("Get Last Hour Steps")
        }

        SensorValue(
            value = manualLastHour ?: "Not requested"
        )

        HorizontalDivider()


        // =====================================================
        // 3. Step Detector
        // =====================================================

        Text(
            text = "3. Step Detector",
            style = MaterialTheme.typography.titleLarge
        )

        Text("API: isStepDetectorAvailable()")
        Text("Type: Function -> Boolean")

        SensorValue(
            value = sensorRepository
                .isStepDetectorAvailable()
                .toString()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("API: stepEvents")
        Text("Type: SharedFlow<Long>")

        SensorValue(
            value = lastStepTimestamp?.toString()
                ?: "No event received"
        )

        Text("Events Received: $stepEvents")
        Text("Walk with your phone to test.")

        HorizontalDivider()


        // =====================================================
        // 4. GPS
        // =====================================================

        Text(
            text = "4. GPS",
            style = MaterialTheme.typography.titleLarge
        )

        Text("API: locationFlow")
        Text("Type: StateFlow<LocationMessage?>")

        SensorValue(
            value = location?.let {
                "Latitude: ${it.latitude}\nLongitude: ${it.longitude}"
            } ?: "null"
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("API: getCurrentLocation()")
        Text("Type: Function -> LocationMessage?")

        Button(
            onClick = {
                val result = sensorRepository.getCurrentLocation()

                manualLocation = result?.let {
                    "Latitude: ${it.latitude}\nLongitude: ${it.longitude}"
                } ?: "null"

                Log.d(TAG, "getCurrentLocation(): $result")
            }
        ) {
            Text("Get Current Location")
        }

        SensorValue(
            value = manualLocation ?: "Not requested"
        )

        HorizontalDivider()


        // =====================================================
        // 5. Shake Detector
        // =====================================================

        Text(
            text = "5. Shake Detector",
            style = MaterialTheme.typography.titleLarge
        )

        Text("API: shakeEvents")
        Text("Type: SharedFlow<Unit>")

        SensorValue(
            value = if (shakeEvents > 0) {
                "Unit"
            } else {
                "No event received"
            }
        )

        Text("Events Received: $shakeEvents")
        Text("Shake your phone to test.")

        HorizontalDivider()
























        // =====================================================
        // 6. POI Search Test
        // =====================================================

        Text(
            text = "6. POI Search Test",
            style = MaterialTheme.typography.titleLarge
        )

        Text("API: uiSearchPlacesByName()")
        Text("Output: List<AddStopPlace>")

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search POI") },
            placeholder = { Text("e.g. museum, park, cafe") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                try {
                    val latitude = location?.latitude ?: -37.8136
                    val longitude = location?.longitude ?: 144.9631

                    searchLocationText = if (location != null) {
                        "Real GPS: $latitude, $longitude"
                    } else {
                        "Mock GPS (Melbourne CBD): $latitude, $longitude"
                    }

                    // Test MainActivity's existing UI integration function.
                    val results = searchPlacesByName(
                        searchQuery,
                        latitude,
                        longitude
                    )

                    searchResults = results
                    searchStatus = "Found ${results.size} results"

                    Log.d(SEARCH_TAG, "Query: '$searchQuery'")
                    Log.d(SEARCH_TAG, "Location: $searchLocationText")
                    Log.d(SEARCH_TAG, "Total results: ${results.size}")

                    results.forEachIndexed { index, place ->
                        Log.d(
                            SEARCH_TAG,
                            "${index + 1}. Name: ${place.name} | " +
                                    "Environment: ${place.environmentType}"
                        )
                    }
                } catch (e: Exception) {
                    searchResults = emptyList()
                    searchStatus = "Search Error: ${e.message}"
                    Log.e(SEARCH_TAG, "POI UI search failed", e)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Text(
            text = searchStatus,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Search Location: $searchLocationText",
            style = MaterialTheme.typography.bodySmall
        )

        searchResults.forEachIndexed { index, place ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${place.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Environment: ${place.environmentType}")
                }
            }
        }

        if (searchResults.isEmpty() && searchStatus == "Found 0 results") {
            Text(
                text = "No matching POIs found.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(300.dp))
    }
}
