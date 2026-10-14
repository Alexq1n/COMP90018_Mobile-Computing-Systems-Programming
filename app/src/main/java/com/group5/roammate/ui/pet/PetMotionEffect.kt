package com.group5.roammate.ui.pet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.group5.roammate.pet.PetSharedMotionSession
import com.group5.roammate.pet.PetMotionSource
import com.group5.roammate.pet.PetMotionState
import com.group5.roammate.sensor.SensorRepository
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PetMotionController(
    val state: PetMotionState,
    val requestActivityPermission: () -> Unit,
)

/** Consumes Alex's foreground streams; MainActivity/SensorService retain sensor ownership. */
@Composable
fun rememberPetMotion(
    sensorRepository: SensorRepository?,
    onRequestActivityPermission: () -> Unit,
): PetMotionController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requestPermission by rememberUpdatedState(onRequestActivityPermission)
    var motion by remember(sensorRepository) { mutableStateOf(PetMotionState()) }

    LaunchedEffect(sensorRepository, lifecycleOwner) {
        val repository = sensorRepository ?: return@LaunchedEffect // Preview has no device inputs.
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val startedAt = SystemClock.elapsedRealtime()
                val permissionGranted = hasPetActivityPermission(context)
                val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                // Hardware discovery is read-only; this module never registers a sensor listener.
                val detectorAvailable = repository.isStepDetectorAvailable()
                val counterAvailable = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
                val source = when {
                    !permissionGranted -> PetMotionSource.Unavailable
                    detectorAvailable -> PetMotionSource.StepDetector
                    counterAvailable -> PetMotionSource.StepCounter
                    else -> PetMotionSource.Unavailable
                }
                val session = PetSharedMotionSession(source, startedAt)
                motion = PetMotionState(
                    source = source,
                    isActive = source != PetMotionSource.Unavailable,
                    needsActivityPermission = !permissionGranted && (detectorAvailable || counterAvailable),
                )

                fun publish() {
                    motion = motion.copy(isMoving = motion.isActive && session.isMoving(SystemClock.elapsedRealtime()))
                }

                try {
                    coroutineScope {
                        val subscription = when (source) {
                            PetMotionSource.StepDetector -> launch {
                                repository.stepEvents.collect { eventMillis ->
                                    session.onStepEvent(eventMillis, SystemClock.elapsedRealtime())
                                    publish()
                                }
                            }
                            PetMotionSource.StepCounter -> launch {
                                repository.stepCountFlow.collect { total ->
                                    // The first replay establishes a baseline, never a walking action.
                                    session.onCounter(total, SystemClock.elapsedRealtime())
                                    publish()
                                }
                            }
                            else -> null
                        }
                        while (hasPetActivityPermission(context) == permissionGranted) {
                            publish() // Stopping produces no step event, so expire movement on a timer.
                            delay(500L)
                        }
                        subscription?.cancelAndJoin()
                    }
                } finally {
                    motion = motion.copy(isMoving = false, isActive = false)
                }
            }
        }
    }
    return PetMotionController(
        state = motion,
        requestActivityPermission = { if (motion.needsActivityPermission) requestPermission() },
    )
}

private fun hasPetActivityPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED
