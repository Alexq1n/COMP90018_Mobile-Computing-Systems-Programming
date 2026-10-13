package com.group5.roammate.weather.data

import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.HourlyWeather
import com.group5.roammate.weather.model.WeatherStatus
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** Converts Meteosource JSON into RoamMate weather models. */
object MeteosourceWeatherMapper {

    fun parseCurrent(json: JSONObject): CurrentWeather {
        val current = json.getJSONObject("current")

        return CurrentWeather(
            status = parseStatus(current),
            temperature = current
                .optDouble("temperature", Double.NaN)
                .takeUnless { it.isNaN() }
        )
    }

    fun parseDailyWeatherByDate(
        json: JSONObject
    ): Map<LocalDate, WeatherStatus> {
        val data = json
            .getJSONObject("daily")
            .getJSONArray("data")

        val weatherByDate = mutableMapOf<LocalDate, WeatherStatus>()

        for (i in 0 until data.length()) {
            val day = data.getJSONObject(i)

            val date = try {
                LocalDate.parse(day.getString("day"))
            } catch (_: Exception) {
                continue
            }

            parseStatus(day)?.let { status ->
                weatherByDate[date] = status
            }
        }

        return weatherByDate
    }

    fun parseHourly(json: JSONObject): List<HourlyWeather> {
        val data = json
            .getJSONObject("hourly")
            .getJSONArray("data")

        val result = mutableListOf<HourlyWeather>()

        for (i in 0 until data.length()) {
            val hour = data.getJSONObject(i)

            val time = try {
                LocalDateTime.parse(hour.getString("date"))
            } catch (_: Exception) {
                continue
            }

            val precipitation = hour.optJSONObject("precipitation")

            val precipitationType = precipitation
                ?.optString("type", "none")
                ?.takeIf { it.isNotBlank() }

            val precipitationTotal = precipitation
                ?.optDouble("total", Double.NaN)
                ?.takeUnless { it.isNaN() }

            result += HourlyWeather(
                time = time,
                status = parseStatus(hour),
                precipitationType = precipitationType,
                precipitationTotal = precipitationTotal
            )
        }

        return result
    }

    /** Convert a Meteosource icon number into the app's WeatherStatus. */
    private fun parseStatus(json: JSONObject): WeatherStatus? {
        // Current weather uses "icon_num",
        // while hourly and daily forecasts use "icon".
        val icon =
            if (json.has("icon_num")) {
                json.optInt("icon_num")
            } else {
                json.optInt("icon")
            }

        return when (icon) {
            2, 3, 4, 26, 27, 28 ->
                WeatherStatus.SUNNY

            5, 6, 7, 8, 9, 29, 30, 31 ->
                WeatherStatus.CLOUDY

            10, 11, 12, 13, 23, 24, 32, 36 ->
                WeatherStatus.RAINY

            14, 15, 25, 33 ->
                WeatherStatus.STORMY

            16, 17, 18, 19, 20, 21, 22, 34, 35 ->
                WeatherStatus.SNOWY

            else -> null
        }
    }
}
