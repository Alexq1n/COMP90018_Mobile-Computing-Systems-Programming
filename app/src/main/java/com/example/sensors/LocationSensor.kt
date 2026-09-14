package com.example.sensors
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.greenrobot.eventbus.EventBus

class LocationSensor(context: Context) {

    private val context = context

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest =
        LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).build()

    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {

            val location = result.lastLocation ?: return

            EventBus.getDefault().post(
                LocationMessage(
                    location.latitude,
                    location.longitude
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun enableLocation() {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationSensor", "Location permission NOT granted")
            return
        }
        Log.d("LocationSensor", "Starting location updates")


        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun disableLocation() {

        client.removeLocationUpdates(locationCallback)
    }
}