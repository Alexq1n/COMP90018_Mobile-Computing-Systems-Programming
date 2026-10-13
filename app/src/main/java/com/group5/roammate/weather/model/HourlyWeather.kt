package com.group5.roammate.weather.model

import java.time.LocalDateTime

/** Hourly forecast used by the weather context-awareness component. */
data class HourlyWeather(
    val time: LocalDateTime,
    val status: WeatherStatus?,
    val precipitationType: String?,
    val precipitationTotal: Double?
)
