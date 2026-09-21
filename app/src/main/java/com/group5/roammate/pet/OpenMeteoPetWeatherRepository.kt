package com.group5.roammate.pet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Key-free current-weather source used by the pet experience.
 *
 * Keeping the network and JSON mapping behind this repository makes it straightforward to replace
 * Open-Meteo with the team's shared weather payload later without changing the pet UI or state
 * engine.
 */
class OpenMeteoPetWeatherRepository(
    private val endpoint: String = "https://api.open-meteo.com/v1/forecast",
) {
    suspend fun fetchCurrent(
        latitude: Double,
        longitude: Double,
        locationLabel: String = "Melbourne",
    ): Result<PetWeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(buildUrl(latitude, longitude)).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.setRequestProperty("Accept", "application/json")

                val responseCode = connection.responseCode
                check(responseCode in 200..299) {
                    val detail = connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?.take(MAX_ERROR_DETAIL_LENGTH)
                        .orEmpty()
                    buildString {
                        append("Weather request failed with HTTP ")
                        append(responseCode)
                        if (detail.isNotBlank()) append(": ").append(detail)
                    }
                }

                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                parseCurrentWeather(payload, locationLabel)
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun buildUrl(latitude: Double, longitude: Double): String {
        val currentFields = listOf(
            "temperature_2m",
            "weather_code",
            "wind_speed_10m",
        ).joinToString(",")

        fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        return "$endpoint?latitude=$latitude&longitude=$longitude" +
            "&current=${encode(currentFields)}&timezone=auto"
    }

    internal fun parseCurrentWeather(
        payload: String,
        locationLabel: String,
    ): PetWeatherSnapshot {
        val current = JSONObject(payload).getJSONObject("current")
        val condition = PetWeatherAdapter.fromWmoCode(current.getInt("weather_code"))

        return PetWeatherSnapshot(
            condition = condition,
            label = PetWeatherAdapter.label(condition),
            temperatureC = current.getDouble("temperature_2m"),
            windSpeedKmh = current.getDouble("wind_speed_10m"),
            locationLabel = locationLabel,
            source = SOURCE_NAME,
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 5_000
        const val MAX_ERROR_DETAIL_LENGTH = 300
        const val SOURCE_NAME = "Open-Meteo"
    }
}
