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
import com.example.weathertest.data.weather.CurrentWeatherMonitor

class MainActivity : ComponentActivity() {

    private val weatherApiManager = WeatherApiManager()
    private val geoapifyRepository = GeoapifyRepository()

    private lateinit var currentWeatherMonitor: CurrentWeatherMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("TEST_START", "MainActivity onCreate reached")

        // =========================
        // POI TEST
        // =========================

        lifecycleScope.launch {
            try {

                val allPois =
                    geoapifyRepository.fetchAllCategories(
                        city = "Melbourne",
                        latitude = -37.8136,
                        longitude = 144.9631,
                        radiusMeters = 20_000,
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

                val cache =
                    PoiJsonCache(this@MainActivity)

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


        // =========================
        // PLANNED WEATHER TEST
        // =========================

        // Later these values come from the UI.
        loadPlannedWeather(
            destinationName = "Melbourne",
            startDate = LocalDate.of(2026, 10, 12),
            endDate = LocalDate.of(2026, 10, 22)
        )

        // =========================
        // CURRENT WEATHER TEST
        // =========================

        currentWeatherMonitor =
            CurrentWeatherMonitor(
                weatherApiManager = weatherApiManager,

                // TEMPORARY GPS
                // Replace this part with the sensor team's GPS function later.
                getCurrentLocation = { callback ->

                    val latitude = -37.8136
                    val longitude = 144.9631

                    callback(
                        latitude,
                        longitude
                    )
                }
            )
        weatherApiManager.currentWeather.observe(this) { weather ->

            Log.d(
                "CURRENT_WEATHER",
                "Weather = ${weather.status}, " +
                        "Temperature = ${weather.temperature}"
            )
        }

        Log.e("TEST_START", "About to start weather monitor")

        currentWeatherMonitor.start()

        Log.e("TEST_START", "Weather monitor start called")
    }



    private fun loadPlannedWeather(
        destinationName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ) {

        lifecycleScope.launch {

            val coordinates =
                geoapifyRepository.geocodeDestination(
                    destinationName
                )

            if (coordinates == null) {

                Log.e(
                    "PlannedWeather",
                    "Could not find destination: $destinationName"
                )

                return@launch
            }

            Log.d(
                "PlannedWeather",
                "$destinationName -> " +
                        "${coordinates.latitude}, " +
                        "${coordinates.longitude}"
            )

            weatherApiManager.getPlannedWeather(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                startDate = startDate,
                endDate = endDate
            ) { forecasts ->

                if (forecasts == null) {

                    Log.e(
                        "PlannedWeather",
                        "Failed to get planned weather"
                    )

                    return@getPlannedWeather
                }

                forecasts.forEach { forecast ->

                    Log.d(
                        "PlannedWeather",
                        "${forecast.date} -> ${forecast.status}"
                    )
                }
            }
        }
    }


    override fun onDestroy() {

        if (::currentWeatherMonitor.isInitialized) {
            currentWeatherMonitor.stop()
        }

        weatherApiManager.stopWeatherMonitoring()

        super.onDestroy()
    }
}