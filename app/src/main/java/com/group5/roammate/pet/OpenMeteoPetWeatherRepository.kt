package com.group5.roammate.pet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.SimpleTimeZone
import kotlin.coroutines.coroutineContext

/** Key-free, non-commercial weather source. Attribution: https://open-meteo.com/ */
class OpenMeteoPetWeatherRepository(
    private val endpoint: String = "https://api.open-meteo.com/v1/forecast",
) {
    suspend fun fetchCurrent(
        latitude: Double,
        longitude: Double,
        locationLabel: String = "Melbourne · selected city",
    ): Result<PetWeatherSnapshot> = withContext(Dispatchers.IO) {
        try {
            coroutineContext.ensureActive()
            val connection = URL(buildUrl(latitude, longitude)).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.setRequestProperty("Accept", "application/json")
                check(connection.responseCode in 200..299) {
                    "Weather request failed (HTTP ${connection.responseCode})"
                }
                val payload = connection.inputStream.bufferedReader().use { reader ->
                    val text = StringBuilder()
                    val buffer = CharArray(4_096)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = reader.read(buffer)
                        if (count < 0) break
                        check(text.length + count <= 128_000) { "Weather response is too large" }
                        text.append(buffer, 0, count)
                    }
                    text.toString()
                }
                coroutineContext.ensureActive()
                Result.success(parseCurrentWeather(payload, locationLabel))
            } finally {
                connection.disconnect()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    internal fun buildUrl(latitude: Double, longitude: Double): String {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid longitude" }
        require(endpoint.startsWith("https://")) { "Weather endpoint must use HTTPS" }
        fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        return "$endpoint?latitude=$latitude&longitude=$longitude" +
            "&current=${encode("temperature_2m,weather_code,wind_speed_10m,is_day")}" +
            "&daily=${encode("temperature_2m_max,temperature_2m_min,precipitation_probability_max")}" +
            "&temperature_unit=celsius&wind_speed_unit=kmh&forecast_days=1&timezone=auto"
    }

    internal fun parseCurrentWeather(
        payload: String,
        locationLabel: String,
        fetchedAtMillis: Long = System.currentTimeMillis(),
    ): PetWeatherSnapshot {
        val root = JSONObject(payload)
        check(!root.optBoolean("error", false)) { "Weather service rejected the request" }
        val current = root.getJSONObject("current")
        val offsetSeconds = root.getInt("utc_offset_seconds")
        require(offsetSeconds in -14 * 3_600..14 * 3_600) { "Invalid weather time zone" }
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            isLenient = false
            timeZone = SimpleTimeZone(offsetSeconds * 1_000, "Weather location")
        }
        val currentTime = current.getString("time")
        val parsePosition = ParsePosition(0)
        val validTimeMillis = formatter.parse(currentTime, parsePosition)?.time
        require(validTimeMillis != null && parsePosition.index == currentTime.length) {
            "Invalid weather timestamp"
        }
        require(fetchedAtMillis - validTimeMillis in -15 * 60_000L..2 * 60 * 60_000L) {
            "Weather response is out of date"
        }
        val temperature = current.getDouble("temperature_2m")
        val wind = current.getDouble("wind_speed_10m")
        require(temperature.isFinite() && temperature in -100.0..70.0) { "Invalid temperature" }
        require(wind.isFinite() && wind in 0.0..500.0) { "Invalid wind speed" }
        val condition = PetWeatherAdapter.fromWmoCode(current.getInt("weather_code"))
        val day = current.getInt("is_day")
        require(day == 0 || day == 1) { "Invalid daylight flag" }

        val daily = root.optJSONObject("daily")
        val date = daily?.optJSONArray("time")?.optString(0)?.takeIf {
            it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                current.optString("time").startsWith(it)
        }
        fun dailyValue(key: String, range: ClosedFloatingPointRange<Double>): Double? {
            if (date == null) return null
            val value = daily?.optJSONArray(key)?.optDouble(0) ?: return null
            return value.takeIf { it.isFinite() && it in range }
        }
        return PetWeatherSnapshot(
            condition = condition,
            label = PetWeatherAdapter.label(condition),
            temperatureC = temperature,
            windSpeedKmh = wind,
            locationLabel = locationLabel,
            source = "Open-Meteo",
            // Preserve the feed time so re-fetching an old payload cannot renew freshness.
            observedAtMillis = minOf(validTimeMillis, fetchedAtMillis),
            todayHighC = dailyValue("temperature_2m_max", -100.0..70.0),
            todayLowC = dailyValue("temperature_2m_min", -100.0..70.0),
            todayRainChancePercent = dailyValue("precipitation_probability_max", 0.0..100.0)?.toInt(),
            forecastDate = date,
            isDay = day == 1,
        )
    }
}
