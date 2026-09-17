package com.group5.roammate.pet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Lightweight, key-free weather fallback for the pet demo. The production app can inject Yan's
 * weather payload through [PetWeatherResult] and remove this network call without touching UI.
 */
class OpenMeteoPetWeatherRepository(
    private val endpoint: String = "https://api.open-meteo.com/v1/forecast",
) {
    suspend fun fetchCurrent(
        latitude: Double,
        longitude: Double,
    ): Result<PetWeatherResult> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(buildUrl(latitude, longitude)).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.setRequestProperty("Accept", "application/json")

                check(connection.responseCode in 200..299) {
                    "Weather request failed with HTTP ${connection.responseCode}"
                }

                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                parseCurrentWeather(payload)
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

        fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        return "$endpoint?latitude=$latitude&longitude=$longitude" +
            "&current=${encode(currentFields)}&timezone=auto"
    }

    internal fun parseCurrentWeather(payload: String): PetWeatherResult {
        val current = JSONObject(payload).getJSONObject("current")
        val code = current.getInt("weather_code")
        val condition = PetWeatherAdapter.fromWmoCode(code)

        return PetWeatherResult(
            condition = condition,
            conditionLabel = PetWeatherAdapter.label(condition),
            temperatureC = current.getDouble("temperature_2m"),
            windSpeedKmh = current.getDouble("wind_speed_10m"),
            source = "Open-Meteo",
        )
    }
}
