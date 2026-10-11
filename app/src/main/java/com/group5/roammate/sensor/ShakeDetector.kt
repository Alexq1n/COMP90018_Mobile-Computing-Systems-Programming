package com.group5.roammate.sensor


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.sqrt

/** Accelerometer-based shake detector. The screen/repository owns its lifetime. */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {
    private val manager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var listening = false
    private var lastShakeAt = 0L

    fun start(): Boolean {
        if (listening) return true
        if (accelerometer == null) {
            Log.w(TAG, "Accelerometer unavailable")
            return false
        }
        // Avoid inheriting a cooldown from a previous visit to the screen.
        lastShakeAt = 0L
        val registered = try {
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to register accelerometer", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid accelerometer registration", e)
            false
        }
        listening = registered
        return registered
    }

    fun stop() {
        if (!listening) return
        listening = false
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!listening || event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(x * x + y * y + z * z)
        val now = SystemClock.elapsedRealtime()
        if (gForce > SHAKE_THRESHOLD_G && now - lastShakeAt > COOLDOWN_MS) {
            lastShakeAt = now
            Log.d(TAG, "Shake detected: gForce=$gForce")
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "SHAKE_SENSOR"
        private const val SHAKE_THRESHOLD_G = 2.35f
        private const val COOLDOWN_MS = 3_000L
    }
}
