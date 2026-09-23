package com.example.weathertest.data.weather

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.weathertest.model.DailyWeatherForecast
import com.example.weathertest.model.WeatherStatus
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

/** Real-time weather returned for the user's current location. */
data class CurrentWeather(
    val status: WeatherStatus?,
    val temperature: Double?
)

/** Hourly forecast used internally for weather-change detection. */
data class HourlyWeather(
    val time: LocalDateTime,
    val status: WeatherStatus?
)

/**
 * Represents a detected weather transition.
 *
 * Example: 15:00 SUNNY -> RAINY.
 */
data class WeatherChangeEvent(
    val time: LocalDateTime,
    val from: WeatherStatus,
    val to: WeatherStatus
)

class WeatherApiManager {

    private val client = OkHttpClient()
    private val apiKey = "vnbcxa1687l84gf6lxlwm2saghv1lam998r8zc4v"

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false
    private val notified = mutableSetOf<String>()

    var onWeatherChange: ((WeatherChangeEvent) -> Unit)? = null

    /** Fetch current weather and temperature for the given location. */
    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        callback: (CurrentWeather?) -> Unit
    ) {
        request(latitude, longitude, "current") { json ->

            if (json == null) {
                callback(null)
                return@request
            }

            try {
                val current = json.getJSONObject("current")

                callback(
                    CurrentWeather(
                        status = getStatus(current),
                        temperature = current
                            .optDouble("temperature", Double.NaN)
                            .takeUnless { it.isNaN() }
                    )
                )
            } catch (e: Exception) {
                Log.e("WeatherAPI", "Current parse error", e)
                callback(null)
            }
        }
    }

    /**
     * Fetch daily weather for the requested trip dates.
     *
     * Dates outside the API forecast range are returned with a null status.
     */
    fun getPlannedWeather(
        latitude: Double,
        longitude: Double,
        startDate: LocalDate,
        tripDays: Int,
        callback: (List<DailyWeatherForecast>?) -> Unit
    ) {
        if (tripDays <= 0) {
            callback(emptyList())
            return
        }

        val dates = List(tripDays) {
            startDate.plusDays(it.toLong())
        }

        request(latitude, longitude, "daily") { json ->

            if (json == null) {
                callback(null)
                return@request
            }

            try {
                val data = json
                    .getJSONObject("daily")
                    .getJSONArray("data")

                val weatherByDate =
                    mutableMapOf<LocalDate, WeatherStatus>()

                for (i in 0 until data.length()) {
                    val day = data.getJSONObject(i)

                    val date = try {
                        LocalDate.parse(day.getString("day"))
                    } catch (_: Exception) {
                        continue
                    }

                    getStatus(day)?.let {
                        weatherByDate[date] = it
                    }
                }

                callback(
                    dates.map { date ->
                        DailyWeatherForecast(
                            date = date,
                            status = weatherByDate[date]
                        )
                    }
                )

            } catch (e: Exception) {
                Log.e("WeatherAPI", "Daily parse error", e)
                callback(null)
            }
        }
    }

    /** Fetch hourly weather for weather-change detection. */
    private fun getHourlyWeather(
        latitude: Double,
        longitude: Double,
        callback: (List<HourlyWeather>?) -> Unit
    ) {
        request(latitude, longitude, "hourly") { json ->

            if (json == null) {
                callback(null)
                return@request
            }

            try {
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

                    result.add(
                        HourlyWeather(
                            time = time,
                            status = getStatus(hour)
                        )
                    )
                }

                callback(result.sortedBy { it.time })

            } catch (e: Exception) {
                Log.e("WeatherAPI", "Hourly parse error", e)
                callback(null)
            }
        }
    }

    /**
     * Detect weather changes from the current hour through the next six hours.
     *
     * Example: SUNNY -> RAINY at 15:00.
     */
    fun getWeatherChangesNext6Hours(
        latitude: Double,
        longitude: Double,
        callback: (List<WeatherChangeEvent>?) -> Unit
    ) {
        getHourlyWeather(latitude, longitude) { hourly ->

            if (hourly == null) {
                callback(null)
                return@getHourlyWeather
            }

            val currentHour = LocalDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            val endTime = currentHour.plusHours(6)

            val next = hourly
                .filter {
                    !it.time.isBefore(currentHour) &&
                            !it.time.isAfter(endTime)
                }
                .sortedBy { it.time }

            val changes = mutableListOf<WeatherChangeEvent>()

            for (i in 1 until next.size) {
                val oldStatus = next[i - 1].status ?: continue
                val newStatus = next[i].status ?: continue

                if (oldStatus != newStatus) {
                    changes.add(
                        WeatherChangeEvent(
                            time = next[i].time,
                            from = oldStatus,
                            to = newStatus
                        )
                    )
                }
            }

            callback(changes)
        }
    }

    /**
     * Start weather monitoring.
     *
     * Weather changes are checked every 30 minutes and new changes are reported
     * through onWeatherChange.
     */
    fun startWeatherMonitoring(
        latitude: Double,
        longitude: Double
    ) {
        if (monitoring) return

        monitoring = true

        val task = object : Runnable {
            override fun run() {
                if (!monitoring) return

                getWeatherChangesNext6Hours(
                    latitude,
                    longitude
                ) { changes ->

                    changes?.forEach { event ->
                        val id =
                            "${event.time}-${event.from}-${event.to}"

                        // Avoid sending the same weather change more than once.
                        if (notified.add(id)) {
                            handler.post {
                                onWeatherChange?.invoke(event)
                            }
                        }
                    }
                }

                handler.postDelayed(
                    this,
                    30 * 60 * 1000L
                )
            }
        }

        handler.post(task)
    }

    /** Stop weather monitoring and clear previously reported changes. */
    fun stopWeatherMonitoring() {
        monitoring = false
        handler.removeCallbacksAndMessages(null)
        notified.clear()
    }

    /** Send a Meteosource request for the requested section. */
    private fun request(
        latitude: Double,
        longitude: Double,
        section: String,
        callback: (JSONObject?) -> Unit
    ) {
        val url =
            "https://www.meteosource.com/api/v1/free/point" +
                    "?lat=$latitude" +
                    "&lon=$longitude" +
                    "&sections=$section" +
                    "&timezone=auto" +
                    "&language=en" +
                    "&units=metric" +
                    "&key=$apiKey"

        client.newCall(
            Request.Builder().url(url).build()
        ).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {
                Log.e(
                    "WeatherAPI",
                    "$section request failed",
                    e
                )
                callback(null)
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(
                            "WeatherAPI",
                            "$section HTTP ${it.code}: ${it.body?.string()}"
                        )
                        callback(null)
                        return
                    }

                    callback(
                        try {
                            JSONObject(it.body?.string().orEmpty())
                        } catch (_: Exception) {
                            null
                        }
                    )
                }
            }
        })
    }

    /** Convert a Meteosource weather icon number into WeatherStatus. */
    private fun getStatus(
        json: JSONObject
    ): WeatherStatus? {

        val icon =
            if (json.has("icon_num")) {
                json.optInt("icon_num", 1)
            } else {
                json.optInt("icon", 1)
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

            else ->
                null
        }
    }
}
