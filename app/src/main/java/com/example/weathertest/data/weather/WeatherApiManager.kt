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
import java.time.temporal.ChronoUnit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/** Real-time weather returned for the user's current location. */
data class CurrentWeather(
    val status: WeatherStatus?,
    val temperature: Double?
)

/** Hourly forecast used for weather-change detection. */
data class HourlyWeather(
    val time: LocalDateTime,
    val status: WeatherStatus?,
    val precipitationType: String?,
    val precipitationTotal: Double?
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

    private val _currentWeather = MutableLiveData<CurrentWeather>()

    val currentWeather: LiveData<CurrentWeather> get() = _currentWeather

    private val handler = Handler(Looper.getMainLooper())
    private var monitoring = false
    private val notifiedDates = mutableSetOf<LocalDate>()

    var onWeatherChange: ((WeatherChangeEvent) -> Unit)? = null

    /**
     * Fetch current weather for the given GPS location.
     *
     * Every successful request updates currentWeather LiveData,
     * allowing the UI to automatically receive the latest weather.
     */
    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        callback: (CurrentWeather?) -> Unit = {}
    ) {

        request(
            latitude,
            longitude,
            "current"
        ) { json ->

            if (json == null) {
                callback(null)
                return@request
            }

            try {

                val current =
                    json.getJSONObject("current")

                val weather =
                    CurrentWeather(
                        status = getStatus(current),

                        temperature = current
                            .optDouble(
                                "temperature",
                                Double.NaN
                            )
                            .takeUnless {
                                it.isNaN()
                            }
                    )

                // Update LiveData for UI / downstream components.
                _currentWeather.postValue(weather)

                // Still support callback if someone wants
                // the result directly.
                callback(weather)

            } catch (e: Exception) {

                Log.e(
                    "WeatherAPI",
                    "Current parse error",
                    e
                )

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
        endDate: LocalDate,
        callback: (List<DailyWeatherForecast>?) -> Unit
    ) {

        val tripDays =
            ChronoUnit.DAYS
                .between(startDate, endDate)
                .toInt() + 1

        // Invalid date range
        if (tripDays < 0) {
            Log.e(
                "WeatherAPI",
                "Invalid trip dates: endDate is before startDate"
            )
            callback(null)
            return
        }

        // Same start/end date -> duration = 0
        if (tripDays == 0) {
            callback(emptyList())
            return
        }

        val dates = List(tripDays) { dayIndex ->
            startDate.plusDays(dayIndex.toLong())
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
                        LocalDate.parse(
                            day.getString("day")
                        )
                    } catch (_: Exception) {
                        continue
                    }

                    getStatus(day)?.let { status ->
                        weatherByDate[date] = status
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

                Log.e(
                    "WeatherAPI",
                    "Daily parse error",
                    e
                )

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

                    val precipitation =
                        hour.optJSONObject("precipitation")

                    val precipitationType =
                        precipitation
                            ?.optString("type", "none")
                            ?.takeIf { it.isNotBlank() }

                    val precipitationTotal =
                        precipitation
                            ?.optDouble("total", Double.NaN)
                            ?.takeUnless { it.isNaN() }

                    result.add(
                        HourlyWeather(
                            time = time,
                            status = getStatus(hour),
                            precipitationType = precipitationType,
                            precipitationTotal = precipitationTotal
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
    fun getRainChangeToday(
        latitude: Double,
        longitude: Double,
        callback: (WeatherChangeEvent?) -> Unit
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

            val today = currentHour.toLocalDate()

            val todayWeather = hourly
                .filter {
                    it.time.toLocalDate() == today &&
                            !it.time.isBefore(currentHour)
                }
                .sortedBy { it.time }

            for (i in 1 until todayWeather.size) {

                val previous = todayWeather[i - 1]
                val current = todayWeather[i]

                val previousIsRain =
                    previous.precipitationType.equals(
                        "rain",
                        ignoreCase = true
                    )

                val currentIsRain =
                    current.precipitationType.equals(
                        "rain",
                        ignoreCase = true
                    )

                if (!previousIsRain && currentIsRain) {

                    callback(
                        WeatherChangeEvent(
                            time = current.time,
                            from = previous.status ?: WeatherStatus.CLOUDY,
                            to = WeatherStatus.RAINY
                        )
                    )

                    // once today's first rain transition is found,
                    // ignore all later changes
                    return@getHourlyWeather
                }
            }

            // No non-rain -> rain transition today
            callback(null)
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

        val runnable = object : Runnable {

            override fun run() {

                getRainChangeToday(
                    latitude,
                    longitude
                ) { event ->

                    if (event != null) {

                        val date =
                            event.time.toLocalDate()

                        if (notifiedDates.add(date)) {

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

        handler.post(runnable)
    }

    /** Stop weather monitoring and clear previously reported changes. */
    fun stopWeatherMonitoring() {
        monitoring = false
        handler.removeCallbacksAndMessages(null)
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
