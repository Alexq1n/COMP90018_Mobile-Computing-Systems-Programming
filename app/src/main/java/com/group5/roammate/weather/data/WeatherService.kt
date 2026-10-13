package com.group5.roammate.weather.data

import com.group5.roammate.poi.data.geoapify.GeoapifyRepository
import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.DailyWeatherForecast
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WeatherService(
    private val weatherRepository: WeatherRepository = WeatherRepository(),
    private val geoapifyRepository: GeoapifyRepository = GeoapifyRepository()
) {

    // Current Weather
    val currentWeather
        get() = weatherRepository.currentWeather

    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        onResult: (CurrentWeather?) -> Unit
    ) {
        weatherRepository.getCurrentWeather(
            latitude = latitude,
            longitude = longitude,
            callback = onResult
        )
    }


    // Planned Weather
    suspend fun getPlannedWeather(
        destination: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyWeatherForecast>? {

        if (
            destination.isBlank() ||
            endDate.isBefore(startDate)
        ) {
            return null
        }

        val coordinates =
            geoapifyRepository
                .geocodeDestination(destination)
                ?: return null

        return suspendCoroutine { continuation ->

            weatherRepository.getPlannedWeather(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                startDate = startDate,
                endDate = endDate
            ) { result ->

                continuation.resume(result)
            }
        }
    }
}