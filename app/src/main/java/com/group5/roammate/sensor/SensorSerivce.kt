
package com.group5.roammate.sensor

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow

class SensorService : Service() {

    companion object {
        const val ACTION_START = "com.group5.roammate.sensor.START_STEPS"
        const val ACTION_STOP = "com.group5.roammate.sensor.STOP_STEPS"

        private const val CHANNEL_ID = "roammate_step_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "RoamMateSensor"
    }

    private val repository: SensorRepository
        get() = (application as RoamMateApp).sensorRepository

    private val binder = LocalBinder()
    private var ownsStepCounter = false

    inner class LocalBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // [NEW] Service can now start independently of Activity binding.
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            stopStepCounting()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasActivityRecognitionPermission()) {
            Log.w(TAG, "Activity Recognition permission missing")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Promote to Foreground Service.
        createNotificationChannel()

        val notification = buildNotification()

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // The Service itself owns Step Counter startup.
        if (!startStepCounting()) {
            Log.e(TAG, "Step Counter unavailable or registration failed")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Log.d(TAG, "Foreground Step Counter running")

        // System may recreate the service after process termination.
        return START_STICKY
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
    }

    // Required for Android 8.0+ notifications.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RoamMate Step Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks walking steps in the background"
            }

            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(channel)
        }
    }

    // Persistent foreground-service notification.
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle("RoamMate step tracking")
            .setContentText("Tracking your steps in the background")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // Existing API, now owned by this service.
    fun startStepCounting(): Boolean {
        if (ownsStepCounter) return true

        ownsStepCounter = repository.startStepCounter()
        return ownsStepCounter
    }

    fun stopStepCounting() {
        if (!ownsStepCounter) return

        repository.stopStepCounter()
        ownsStepCounter = false
    }

    // Preserve your existing repository-backed APIs.
    val stepCountFlow: StateFlow<Int?>
        get() = repository.stepCountFlow

    val stepsLastHourFlow: StateFlow<Int?>
        get() = repository.stepsLastHourFlow

    fun getCurrentSteps(): Int? = repository.getCurrentSteps()

    fun getStepsLastHour(): Int? = repository.getStepsLastHour()

    fun getCurrentLocation(): LocationMessage? =
        repository.getCurrentLocation()

    override fun onDestroy() {
        stopStepCounting()
        Log.d(TAG, "SensorService destroyed")
        super.onDestroy()
    }
}
