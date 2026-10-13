package com.group5.roammate.weather.monitor

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.group5.roammate.weather.context.WeatherContextDetector
import com.group5.roammate.weather.context.WeatherContextEvent
import com.group5.roammate.weather.data.WeatherRepository

/**
 * Periodically checks hourly forecasts for context-aware upcoming-rain events.
 *
 * It intentionally runs less often than CurrentWeatherMonitor because hourly
 * forecasts do not need to be refreshed every 10 minutes.
 */
class WeatherForecastMonitor(
    private val weatherRepository: WeatherRepository,
    private val contextDetector: WeatherContextDetector,
    private val getCurrentLocation:
        () -> Pair<Double, Double>?,
    private val forecastHoursAhead: Long = 6
) {

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false

    private val updateInterval = 30 * 60 * 1000L
    private var upcomingRainAlreadyNotified = false

    var onContextEvent: ((WeatherContextEvent) -> Unit)? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!monitoring) {
                return
            }

            checkForecast()

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

    private fun checkForecast() {
        val (latitude, longitude) =
            getCurrentLocation() ?: return

        Log.d(
            "WeatherForecastMonitor",
            "Checking forecast at GPS: $latitude, $longitude"
        )

        weatherRepository.getHourlyWeather(
            latitude = latitude,
            longitude = longitude
        ) { hourly ->

            if (!monitoring) {
                return@getHourlyWeather
            }

            if (hourly == null) {
                return@getHourlyWeather
            }

            val event = contextDetector.detectUpcomingRain(
                current = weatherRepository.currentWeather.value,
                hourly = hourly,
                hoursAhead = forecastHoursAhead
            )

            if (event is WeatherContextEvent.UpcomingRain) {

                if (!upcomingRainAlreadyNotified) {
                    upcomingRainAlreadyNotified = true

                    onContextEvent?.invoke(event)
                }

            } else {
                upcomingRainAlreadyNotified = false
            }
        }
    }
}
