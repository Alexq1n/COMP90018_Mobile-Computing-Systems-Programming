package com.group5.roammate.ui.pet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.group5.roammate.pet.PetMotionEngine
import com.group5.roammate.pet.PetMotionSource
import com.group5.roammate.pet.PetMotionState

data class PetMotionController(
    val state: PetMotionState,
    val requestActivityPermission: () -> Unit,
)

/**
 * Call only while the Pet screen is composed. Registration stops on pause or navigation away.
 * Permission is requested only by the caller's explicit user action; denial still permits the
 * accelerometer estimate. No foreground service, GPS, activity history or EventBus is required.
 */
@Composable
fun rememberPetMotion(): PetMotionController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRevision by remember { mutableIntStateOf(0) }
    var motion by remember { mutableStateOf(PetMotionState()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionRevision += 1 }

    DisposableEffect(context, lifecycleOwner, permissionRevision) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val engine = PetMotionEngine()
        val handler = Handler(Looper.getMainLooper())
        var active = false
        var source = PetMotionSource.Unavailable
        var startedAt = 0L

        fun publish() {
            motion = motion.copy(
                isMoving = active && engine.isMoving(SystemClock.elapsedRealtime()),
                source = source,
                isActive = active,
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!active || event.values.isEmpty()) return
                val eventMillis = event.timestamp / 1_000_000L
                if (eventMillis < startedAt) return // Ignore a queued sample from a previous visit.
                when (event.sensor.type) {
                    Sensor.TYPE_STEP_DETECTOR -> if (event.values[0] > 0f) {
                        engine.onStepDetected(eventMillis)
                    }
                    Sensor.TYPE_STEP_COUNTER -> engine.onStepCounter(event.values[0], eventMillis)
                    Sensor.TYPE_ACCELEROMETER -> if (event.values.size >= 3) {
                        engine.onAcceleration(
                            event.values[0], event.values[1], event.values[2], eventMillis,
                        )
                    }
                }
                publish()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val tick = object : Runnable {
            override fun run() {
                if (!active) return
                publish() // Step sensors send no event when the user stops walking.
                handler.postDelayed(this, 500L)
            }
        }

        fun stop() {
            active = false
            manager?.unregisterListener(listener)
            handler.removeCallbacks(tick)
            engine.reset()
            publish()
        }

        fun trySensor(type: Int): Boolean {
            val sensorManager = manager ?: return false
            val sensor = sensorManager.getDefaultSensor(type) ?: return false
            return try {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            } catch (_: SecurityException) {
                false // Permission can change between checking it and registering.
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        fun start() {
            stop()
            val stepHardware = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_SENSOR_STEP_DETECTOR,
            ) || context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
            val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION,
                ) == PackageManager.PERMISSION_GRANTED
            motion = motion.copy(needsActivityPermission = stepHardware && !permissionGranted)
            startedAt = SystemClock.elapsedRealtime()
            source = when {
                permissionGranted && trySensor(Sensor.TYPE_STEP_DETECTOR) -> PetMotionSource.StepDetector
                permissionGranted && trySensor(Sensor.TYPE_STEP_COUNTER) -> PetMotionSource.StepCounter
                trySensor(Sensor.TYPE_ACCELEROMETER) -> PetMotionSource.Accelerometer
                else -> PetMotionSource.Unavailable
            }
            active = source != PetMotionSource.Unavailable
            publish()
            if (active) handler.post(tick)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    return PetMotionController(
        state = motion,
        requestActivityPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && motion.needsActivityPermission) {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        },
    )
}
