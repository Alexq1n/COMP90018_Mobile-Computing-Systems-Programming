package com.example.weathertest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.weathertest.data.geoapify.GeoapifyRepository
import com.example.weathertest.data.geoapify.PoiJsonCache
import com.example.weathertest.data.weather.WeatherApiManager
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val weatherApiManager = WeatherApiManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // POI TEST
        lifecycleScope.launch {
            try {
                val repository = GeoapifyRepository()

                val allPois = repository.fetchAllCategories(
                    city = "Melbourne",
                    latitude = -37.8136,
                    longitude = 144.9631,
                    radiusMeters = 20_000,
                    perCategory = 10,
                    enrichWithDetails = false
                )

                Log.d(
                    "POI_TEST",
                    "Total POIs = ${allPois.size}"
                )

                allPois
                    .groupBy { it.category }
                    .forEach { (category, pois) ->
                        Log.d(
                            "POI_TEST",
                            "$category = ${pois.size}"
                        )
                    }

                // Cache POIs in a local JSON file.
                val cache = PoiJsonCache(this@MainActivity)

                cache.save(
                    pois = allPois,
                    fileName = "pois.json"
                )

            } catch (e: Exception) {
                Log.e(
                    "POI_TEST",
                    "ERROR",
                    e
                )
            }
        }

        // FAKE SENSOR TEST
        // Replace with the real sensor/location module later.
        getFakeSensorLocation { latitude, longitude ->

            Log.d(
                "SensorTest",
                "Fake location = $latitude, $longitude"
            )

            // Fetch current weather using the current sensor location.
            weatherApiManager.getCurrentWeather(
                latitude = latitude,
                longitude = longitude
            ) { current ->

                if (current != null) {
                    Log.d(
                        "WeatherTest",
                        "Current weather = ${current.status}, " +
                                "temperature = ${current.temperature}°C"
                    )
                } else {
                    Log.e(
                        "WeatherTest",
                        "Failed to get current weather"
                    )
                }
            }

            // Receive detected weather changes.
            weatherApiManager.onWeatherChange = { event ->

                Log.d(
                    "WeatherChange",
                    "${event.time}: ${event.from} -> ${event.to}"
                )

                // Zewen can handle replanning from this event.
            }

            // Monitor weather changes using the current sensor location.
            weatherApiManager.startWeatherMonitoring(
                latitude = latitude,
                longitude = longitude
            )
        }

        // PLANNED WEATHER TEST
        // Destination coordinates will later come from the UI / itinerary.
        val destinationLatitude = -37.8136
        val destinationLongitude = 144.9631

        weatherApiManager.getPlannedWeather(
            latitude = destinationLatitude,
            longitude = destinationLongitude,
            startDate = LocalDate.of(2026, 9, 24),
            tripDays = 10
        ) { forecasts ->

            if (forecasts == null) {
                Log.e(
                    "WeatherTest",
                    "Failed to get planned weather"
                )
                return@getPlannedWeather
            }

            forecasts.forEach { forecast ->
                Log.d(
                    "WeatherTest",
                    "${forecast.date} -> ${forecast.status}"
                )
            }
        }
    }

    /**
     * Temporary fake location provider for testing.
     * Replace with the real sensor/location module later.
     */
    private fun getFakeSensorLocation(
        callback: (
            latitude: Double,
            longitude: Double
        ) -> Unit
    ) {
        val fakeLatitude = -37.8136
        val fakeLongitude = 144.9631

        callback(
            fakeLatitude,
            fakeLongitude
        )
    }

    override fun onDestroy() {
        weatherApiManager.stopWeatherMonitoring()
        super.onDestroy()
    }
}