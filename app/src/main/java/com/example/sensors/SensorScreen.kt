package com.example.sensors

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SensorScreen(
    x: Float,
    y: Float,
    z: Float,
    light: Float,
    latitude: Double,
    longitude: Double,
    places: List<Place>,
    repository: SensorRepository,
    hasLocationPermission: Boolean,
    requestLocationPermission: () -> Unit,
    onCameraClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Ask only while this screen is present; permission callback updates MainActivity state.
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) requestLocationPermission()
    }

    // GPS runs only while this screen is composed AND its lifecycle is STARTED.
    // onDispose also stops it when navigating to CameraScreen.
    DisposableEffect(lifecycleOwner, repository, hasLocationPermission) {
        var tracking = false

        fun startIfAllowed() {
            if (hasLocationPermission && !tracking) {
                repository.startLocationTracking()
                tracking = true
                Log.d("GPS_TEST", "SensorScreen GPS start requested")
            }
        }
        fun stopIfRunning() {
            if (tracking) {
                repository.stopLocationTracking()
                tracking = false
                Log.d("GPS_TEST", "SensorScreen GPS stopped")
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startIfAllowed()
                Lifecycle.Event.ON_STOP -> stopIfRunning()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startIfAllowed()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopIfRunning()
        }
    }

    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("RoamMate Sensors")
        Text("X: %.2f".format(x))
        Text("Y: %.2f".format(y))
        Text("Z: %.2f".format(z))
        Text("Light: %.2f lux".format(light))
        Text("Latitude: %.6f".format(latitude))
        Text("Longitude: %.6f".format(longitude))

        Spacer(modifier = Modifier.height(10.dp))
        Text("Search Places")
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Enter place name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { searchResults = searchPlaces(query = query, places = places) },
            enabled = query.isNotBlank()
        ) { Text("Search") }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { result ->
                Column {
                    Text(result.place.name)
                    Text("Score: %.2f".format(result.score))
                }
            }
        }

        Button(onClick = {
            val location = repository.getCurrentLocation()
            if (location != null) {
                Log.d("GPS_TEST", "Latitude = ${location.latitude}, type = ${location.latitude::class.simpleName}")
                Log.d("GPS_TEST", "Longitude = ${location.longitude}, type = ${location.longitude::class.simpleName}")
            } else {
                Log.d("GPS_TEST", "No GPS fix received yet")
            }
        }) { Text("Test GPS") }

        Button(onClick = {
            val steps = repository.getStepsLastHour()
            Log.d("STEP_SENSOR", "Steps last hour = $steps, type = ${steps?.let { it::class.simpleName }}")
        }) { Text("Test Steps") }

        Button(onClick = onCameraClick) { Text("Open Camera") }
    }
}
