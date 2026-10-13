package com.group5.roammate.weather.context

import com.group5.roammate.weather.model.WeatherStatus
import java.time.LocalDateTime

/** Context-awareness events that are meaningful to the UI/itinerary planner. */
sealed class WeatherContextEvent {

    /** Current weather changed from a non-rain state to a rain state. */
    data class SuddenRain(
        val detectedAt: LocalDateTime,
        val from: WeatherStatus,
        val to: WeatherStatus
    ) : WeatherContextEvent()

    /** Current weather is non-rainy, but rain is forecast within the look-ahead window. */
    data class UpcomingRain(
        val detectedAt: LocalDateTime,
        val expectedStartTime: LocalDateTime
    ) : WeatherContextEvent()
}
