
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
import com.group5.roammate.sensor.SensorService.Companion.TAG
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel


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


    // Coroutine scope for asynchronous Step Counter startup
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )
    // Track the Step Counter startup coroutine
    private var startJob: Job? = null

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
        startStepCounting()

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
    fun startStepCounting() {
        if (ownsStepCounter || startJob?.isActive == true) return

        startJob = serviceScope.launch {
            try {
                // Wait until Room history has been restored
                val started = repository.startStepCounter()

                if (started) {
                    ownsStepCounter = true
                    Log.d(TAG, "Step Counter started successfully")
                } else {
                    Log.e(TAG, "Step Counter unavailable or registration failed")
                    stopSelf()
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e(TAG, "Failed to restore history or start Step Counter", e)
                stopSelf()
            }
        }
    }

    fun stopStepCounting() {
        startJob?.cancel()
        startJob = null

        if (ownsStepCounter) {
            repository.stopStepCounter()
            ownsStepCounter = false
        }
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
        startJob?.cancel()
        serviceScope.cancel()
        stopStepCounting()
        Log.d(TAG, "SensorService destroyed")
        super.onDestroy()
    }



}
