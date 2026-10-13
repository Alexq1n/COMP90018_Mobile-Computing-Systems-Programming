package com.group5.roammate.weather.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.DailyWeatherForecast
import com.group5.roammate.weather.model.HourlyWeather
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * App-facing weather data source.
 *
 * UI/monitor classes ask this repository for weather models and do not need to
 * know about Meteosource URLs or JSON structure.
 */
class WeatherRepository(
    private val apiClient: MeteosourceApiClient = MeteosourceApiClient()
) {

    private val _currentWeather = MutableLiveData<CurrentWeather>()
    val currentWeather: LiveData<CurrentWeather> get() = _currentWeather

    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        callback: (CurrentWeather?) -> Unit = {}
    ) {
        apiClient.request(
            latitude = latitude,
            longitude = longitude,
            section = "current"
        ) { json ->
            if (json == null) {
                callback(null)
                return@request
            }

            try {
                val weather = MeteosourceWeatherMapper.parseCurrent(json)

                _currentWeather.value = weather
                callback(weather)
            } catch (e: Exception) {
                Log.e(
                    "WeatherRepository",
                    "Current weather parse error",
                    e
                )
                callback(null)
            }
        }
    }

    fun getPlannedWeather(
        latitude: Double,
        longitude: Double,
        startDate: LocalDate,
        endDate: LocalDate,
        callback: (List<DailyWeatherForecast>?) -> Unit
    ) {
        if (endDate.isBefore(startDate)) {
            Log.e(
                "WeatherRepository",
                "Invalid trip dates: endDate is before startDate"
            )
            callback(null)
            return
        }

        val tripDays = ChronoUnit.DAYS
            .between(startDate, endDate)
            .toInt() + 1

        val dates = List(tripDays) { dayIndex ->
            startDate.plusDays(dayIndex.toLong())
        }

        apiClient.request(
            latitude = latitude,
            longitude = longitude,
            section = "daily"
        ) { json ->
            if (json == null) {
                callback(null)
                return@request
            }

            try {
                val weatherByDate =
                    MeteosourceWeatherMapper.parseDailyWeatherByDate(json)

                callback(
                    dates.map { date ->
                        DailyWeatherForecast(
                            date = date,
                            status = weatherByDate[date]
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(
                    "WeatherRepository",
                    "Daily weather parse error",
                    e
                )
                callback(null)
            }
        }
    }

    fun getHourlyWeather(
        latitude: Double,
        longitude: Double,
        callback: (List<HourlyWeather>?) -> Unit
    ) {
        apiClient.request(
            latitude = latitude,
            longitude = longitude,
            section = "hourly"
        ) { json ->
            if (json == null) {
                callback(null)
                return@request
            }

            try {
                callback(MeteosourceWeatherMapper.parseHourly(json))
            } catch (e: Exception) {
                Log.e(
                    "WeatherRepository",
                    "Hourly weather parse error",
                    e
                )
                callback(null)
            }
        }
    }
}
