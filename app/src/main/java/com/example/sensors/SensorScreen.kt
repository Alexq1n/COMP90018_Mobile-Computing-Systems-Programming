package com.example.sensors

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


@Composable
fun SensorScreen(
    x: Float,
    y: Float,
    z: Float,
    light: Float,
    latitude: Double,
    longitude: Double,
    onCameraClick: () -> Unit
) {
    Column {

        Text("RoamMate Sensors")

        Text("X: %.2f".format(x))
        Text("Y: %.2f".format(y))
        Text("Z: %.2f".format(z))

        Text("Light: %.2f lux".format(light))

        Text("Latitude: %.6f".format(latitude))
        Text("Longitude: %.6f".format(longitude))

        Button(
            onClick = onCameraClick
        ) {
            Text("Open Camera")
        }
    }
}