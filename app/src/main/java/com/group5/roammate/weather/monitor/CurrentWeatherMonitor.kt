package com.group5.roammate.weather.monitor

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.group5.roammate.weather.data.WeatherService
import com.group5.roammate.weather.model.CurrentWeather

/**
 * Periodically gets the user's latest GPS location and requests current weather.
 * Current weather is requested immediately and then every 10 minutes.
 */
class CurrentWeatherMonitor(
    private val weatherService: WeatherService,

    // Connected to the GPS function provided by the sensor/GPS module.
    private val getCurrentLocation:
        () -> Pair<Double, Double>?
){

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false

    private val updateInterval = 10 * 60 * 1000L

    /** Optional consumer for context-awareness logic. Called on the main thread. */
    var onWeatherUpdated: ((CurrentWeather) -> Unit)? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!monitoring) {
                return
            }

            updateWeather()

            handler.postDelayed(
                this,
                updateInterval
            )
        }
    }

    fun start() {
        if (monitoring) {
            return
        }

        monitoring = true
        handler.post(updateRunnable)
    }

    fun stop() {
        monitoring = false
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateWeather() {
        val (latitude, longitude) =
            getCurrentLocation() ?: return

        Log.d(
            "CurrentWeatherMonitor",
            "GPS: $latitude, $longitude"
        )

        weatherService.getCurrentWeather(
            latitude = latitude,
            longitude = longitude
        ) { weather ->

            if (!monitoring) {
                return@getCurrentWeather
            }

            if (weather != null) {
                onWeatherUpdated?.invoke(weather)
            }
        }
    }
}
