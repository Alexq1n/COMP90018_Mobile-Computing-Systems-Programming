package com.group5.roammate.pet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Android's real org.json parser is used; these tests make no network calls. */
@RunWith(AndroidJUnit4::class)
class OpenMeteoPetWeatherRepositoryTest {
    private val repository = OpenMeteoPetWeatherRepository()
    private val fetchedAt = 1_789_988_700_000L // 2026-09-21 11:05 UTC / 21:05 Melbourne
    private val payload = """
        {
          "utc_offset_seconds": 36000,
          "current": {
            "time": "2026-09-21T21:00",
            "temperature_2m": 12.5,
            "weather_code": 61,
            "wind_speed_10m": 18.2,
            "is_day": 0
          },
          "daily": {
            "time": ["2026-09-21"],
            "temperature_2m_max": [17.3],
            "temperature_2m_min": [8.4],
            "precipitation_probability_max": [80]
          }
        }
    """.trimIndent()

    @Test
    fun mapsCurrentAndDailyForecastWithoutPretendingNightIsDay() {
        val actual = repository.parseCurrentWeather(payload, "Your location", fetchedAt)
        assertEquals(PetWeatherCondition.Rain, actual.condition)
        assertEquals(12.5, actual.temperatureC, 0.001)
        assertEquals(18.2, actual.windSpeedKmh, 0.001)
        assertEquals(17.3, actual.todayHighC!!, 0.001)
        assertEquals(8.4, actual.todayLowC!!, 0.001)
        assertEquals(80, actual.todayRainChancePercent)
        assertEquals("2026-09-21", actual.forecastDate)
        assertEquals(fetchedAt - 5 * 60_000L, actual.observedAtMillis)
        assertEquals("Your location", actual.locationLabel)
        assertFalse(actual.isDay)
    }

    @Test
    fun refusesToDescribeAnotherDatesDailyForecastAsToday() {
        val differentDay = payload.replace("\"time\": [\"2026-09-21\"]", "\"time\": [\"2026-09-20\"]")
        val actual = repository.parseCurrentWeather(differentDay, "Melbourne", fetchedAt)
        assertNull(actual.forecastDate)
        assertNull(actual.todayRainChancePercent)
        assertNull(actual.todayHighC)
    }

    @Test
    fun missingDailyValueStaysUnknownRatherThanZero() {
        val actual = repository.parseCurrentWeather(
            payload.replace("\"precipitation_probability_max\": [80]", "\"precipitation_probability_max\": [null]"),
            "Melbourne",
            fetchedAt,
        )
        assertNull(actual.todayRainChancePercent)
        assertEquals(12.5, actual.temperatureC, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDaylightFlag() {
        repository.parseCurrentWeather(payload.replace("\"is_day\": 0", "\"is_day\": 2"), "Melbourne", fetchedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsImplausibleTemperatureRatherThanProvidingAdvice() {
        repository.parseCurrentWeather(payload.replace("12.5", "125.0"), "Melbourne", fetchedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidCoordinatesBeforeMakingAnyRequest() {
        repository.buildUrl(Double.NaN, 144.96)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCachedServerResponseOlderThanTwoHours() {
        repository.parseCurrentWeather(payload, "Melbourne", fetchedAt + 3 * 60 * 60_000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedTimestampWithoutAcceptingPartialDate() {
        repository.parseCurrentWeather(payload.replace("2026-09-21T21:00", "2026-09-21T21:00garbage"), "Melbourne", fetchedAt)
    }
}
