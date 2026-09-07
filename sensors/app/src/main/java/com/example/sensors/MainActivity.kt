package com.example.sensors

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensors.AccelerometerMessage
import com.example.sensors.Accelerometer
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : ComponentActivity() {

    private lateinit var accelerometer: Accelerometer
    private lateinit var lightSensor: LightSensor
    private lateinit var locationSensor: LocationSensor

    private var x by mutableFloatStateOf(0f)
    private var y by mutableFloatStateOf(0f)
    private var z by mutableFloatStateOf(0f)

    private var light by mutableFloatStateOf(0f)

    private var latitude by mutableDoubleStateOf(0.0)
    private var longitude by mutableDoubleStateOf(0.0)


    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                locationSensor.enableLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accelerometer = Accelerometer(this)
        lightSensor = LightSensor(this)
        locationSensor = LocationSensor(this)
        setContent {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp)
            ) {
                Text("RoamMate Sensors")

                Text("Accelerometer")

                Text("X: %.2f".format(x))

                Text("Y: %.2f".format(y))

                Text("Z: %.2f".format(z))

                Text("Light Sensor")

                Text("Light: %.2f lux".format(light))

                Text("Location")
                Text("Latitude: %.6f".format(latitude))
                Text("Longitude: %.6f".format(longitude))
            }
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)

        accelerometer.enableSensor()
        lightSensor.enableSensor()
        locationSensor.enableLocation()
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationSensor.enableLocation()
        } else {
            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onStop() {

        EventBus.getDefault().unregister(this)

        accelerometer.disableSensor()
        lightSensor.disableSensor()
        locationSensor.disableLocation()
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccelerometerMessage(
        message: AccelerometerMessage
    ) {

        x = message.x
        y = message.y
        z = message.z
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLightSensorMessage(
        message: LightSensorMessage
    ) {

        light = message.light
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocationMessage(
        message: LocationMessage
    ) {

        latitude = message.latitude
        longitude = message.longitude
    }
}