package com.example.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.greenrobot.eventbus.EventBus

class LocationSensor(
    context: Context,
    private val onLocationChanged: (LocationMessage) -> Unit = {}
) {

    private val context = context.applicationContext

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this.context)

    private var currentLocation: LocationMessage? = null

    private var isListening = false

    private val locationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).build()

    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {

            val location = result.lastLocation ?: return

            val message = LocationMessage(
                latitude = location.latitude,
                longitude = location.longitude
            )

            currentLocation = message

            // Notify Repository
            onLocationChanged(message)

            // Keep existing EventBus compatibility
            EventBus.getDefault().post(message)
        }
    }

    @SuppressLint("MissingPermission")
    fun enableLocation() {

        if (isListening) return

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            Log.d("LocationSensor", "Location permission NOT granted")
            return
        }

        Log.d("LocationSensor", "Starting location updates")

        isListening = true

        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { exception ->
            isListening = false
            Log.e("LocationSensor", "Location updates failed", exception)
        }
    }

    fun getCurrentLocation(): LocationMessage? {
        return currentLocation
    }

    fun disableLocation() {
        isListening = false
        client.removeLocationUpdates(locationCallback)
    }
}