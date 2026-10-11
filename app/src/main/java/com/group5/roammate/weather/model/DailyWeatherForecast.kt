package com.group5.roammate.weather.model

import java.time.LocalDate

data class DailyWeatherForecast(
    val date: LocalDate,
    val status: WeatherStatus?
)

