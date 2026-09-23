package com.example.weathertest.model

import java.time.LocalDate

data class DailyWeatherForecast(
    val date: LocalDate,
    val status: WeatherStatus?
)