package com.example.sensors
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
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

    private var showCamera by mutableStateOf(false)

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                locationSensor.enableLocation()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                showCamera = true
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accelerometer = Accelerometer(this)
        lightSensor = LightSensor(this)
        locationSensor = LocationSensor(this)

        setContent {
            if (showCamera) {

                CameraScreen(
                    onBack = {
                        showCamera = false
                    }
                )

            } else {

                SensorScreen(
                    x = x,
                    y = y,
                    z = z,
                    light = light,
                    latitude = latitude,
                    longitude = longitude,

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
        EventBus.getDefault().unregister(this)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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


            Button(
                onClick = onCameraClick
            ) {

                Text("Open Camera")
            }
        }
    }
}