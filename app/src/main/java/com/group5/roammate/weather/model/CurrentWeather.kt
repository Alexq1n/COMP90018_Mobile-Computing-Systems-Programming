package com.group5.roammate.weather.model

/** Real-time weather returned for the user's current location. */
data class CurrentWeather(
    val status: WeatherStatus?,
    val temperature: Double?
)
