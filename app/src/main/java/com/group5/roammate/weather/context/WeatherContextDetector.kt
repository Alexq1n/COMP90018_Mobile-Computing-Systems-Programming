package com.group5.roammate.weather.context

import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.HourlyWeather
import com.group5.roammate.weather.model.WeatherStatus
import java.time.LocalDateTime

/**
 * Pure weather context-awareness logic.
 *
 * It does not perform network requests or scheduling. It only decides whether
 * the supplied weather data represents a context change worth reporting.
 */
class WeatherContextDetector {

    private var previousCurrentStatus: WeatherStatus? = null

    /**
     * Detect a real current-weather transition from non-rain to rain.
     *
     * The first observation only establishes the baseline and does not trigger
     * an event because there is no previous context to compare against yet.
     */

    fun detectCurrentWeatherChange(
        current: CurrentWeather,
        now: LocalDateTime = LocalDateTime.now()
    ): WeatherContextEvent? {
        val currentStatus = current.status ?: return null
        val previousStatus = previousCurrentStatus

        previousCurrentStatus = currentStatus

        if (
            previousStatus != null &&
            !isRainCondition(previousStatus) &&
            isRainCondition(currentStatus)
        ) {
            return WeatherContextEvent.SuddenRain(
                detectedAt = now,
                from = previousStatus,
                to = currentStatus
            )
        }

        return null
    }

    /**
     * Detect rain forecast within the next [hoursAhead] hours while the current
     * weather is still non-rainy.
     */
    fun detectUpcomingRain(
        current: CurrentWeather?,
        hourly: List<HourlyWeather>,
        hoursAhead: Long = 3,
        now: LocalDateTime = LocalDateTime.now()
    ): WeatherContextEvent? {
        val currentStatus = current?.status ?: return null

        // If it is already raining, sudden-rain handling is more relevant than
        // an "upcoming rain" forecast event.
        if (isRainCondition(currentStatus)) {
            return null
        }

        val windowEnd = now.plusHours(hoursAhead)

        val firstRain = hourly
            .filter { forecast ->
                val inWindow =
                    !forecast.time.isBefore(now) &&
                            !forecast.time.isAfter(windowEnd)

                inWindow && isRainForecast(forecast)
            }
            .minByOrNull { it.time }
            ?: return null

        return WeatherContextEvent.UpcomingRain(
            detectedAt = now,
            expectedStartTime = firstRain.time
        )
    }

    private fun isRainForecast(hourly: HourlyWeather): Boolean {
        val precipitationSaysRain =
            hourly.precipitationType.equals(
                "rain",
                ignoreCase = true
            )

        return precipitationSaysRain ||
                hourly.status?.let(::isRainCondition) == true
    }

    private fun isRainCondition(status: WeatherStatus): Boolean {
        return status == WeatherStatus.RAINY ||
                status == WeatherStatus.STORMY
    }
}
