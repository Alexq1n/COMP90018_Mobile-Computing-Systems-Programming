package com.example.weathertest.data.weather

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Periodically gets the user's latest GPS location
 * and requests the current weather.
 */
class CurrentWeatherMonitor(
    private val weatherApiManager: WeatherApiManager,

    // This will later be connected to the GPS function
    // provided by the sensor/GPS team.
    private val getCurrentLocation:
        ((Double, Double) -> Unit) -> Unit
) {

    private val handler =
        Handler(Looper.getMainLooper())

    private var monitoring = false

    // 10 minutes
    private val updateInterval =
        10 * 60 * 1000L

    private val updateRunnable =
        object : Runnable {

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

    /**
     * Start current-weather monitoring.
     *
     * Weather is requested immediately,
     * then every 10 minutes.
     */
    fun start() {

        if (monitoring) {
            return
        }

        monitoring = true

        // Run immediately.
        handler.post(updateRunnable)
    }

    /**
     * Stop current-weather monitoring.
     */
    fun stop() {

        monitoring = false

        handler.removeCallbacks(
            updateRunnable
        )
    }

    /**
     * 1. Ask GPS module for the latest location.
     * 2. Use that location to request current weather.
     */
    private fun updateWeather() {

        getCurrentLocation { latitude, longitude ->

            Log.d(
                "CurrentWeatherMonitor",
                "GPS: $latitude, $longitude"
            )

            weatherApiManager.getCurrentWeather(
                latitude = latitude,
                longitude = longitude
            )
        }
    }
}