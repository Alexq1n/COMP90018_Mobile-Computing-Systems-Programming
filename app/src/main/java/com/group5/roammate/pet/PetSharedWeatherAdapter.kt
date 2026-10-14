package com.group5.roammate.pet

import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.WeatherStatus

/** Converts Leyan's shared feed without inventing wind, daylight or an observation timestamp. */
object PetSharedWeatherAdapter {
    fun fromCurrentWeather(weather: CurrentWeather, receivedAtMillis: Long): PetWeatherSnapshot {
        val condition = when (weather.status) {
            WeatherStatus.SUNNY -> PetWeatherCondition.Clear
            WeatherStatus.CLOUDY -> PetWeatherCondition.Cloudy
            WeatherStatus.RAINY -> PetWeatherCondition.Rain
            WeatherStatus.STORMY -> PetWeatherCondition.Storm
            WeatherStatus.SNOWY -> PetWeatherCondition.Snow
            null -> PetWeatherCondition.Unknown
        }
        return PetWeatherSnapshot(
            condition = condition,
            label = weather.status?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                ?: "Weather unavailable",
            temperatureC = weather.temperature?.takeIf { it.isFinite() && it in -100.0..70.0 }
                ?: Double.NaN,
            windSpeedKmh = Double.NaN,
            locationLabel = "Current location",
            source = "Meteosource · shared weather",
            receivedAtMillis = receivedAtMillis,
            daylightKnown = false,
        )
    }
}

/** A LiveData replay is the same response, so returning to the app must not renew its age. */
class PetSharedWeatherState {
    private var lastResponse: CurrentWeather? = null
    private var lastSnapshot: PetWeatherSnapshot? = null

    fun accept(weather: CurrentWeather, receivedAtMillis: Long): PetWeatherSnapshot {
        if (weather === lastResponse) return checkNotNull(lastSnapshot)
        return PetSharedWeatherAdapter.fromCurrentWeather(weather, receivedAtMillis).also {
            lastResponse = weather
            lastSnapshot = it
        }
    }
}
