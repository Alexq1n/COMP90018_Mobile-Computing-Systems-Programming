package com.group5.roammate.pet

import com.group5.roammate.weather.model.CurrentWeather
import com.group5.roammate.weather.model.WeatherStatus
import org.junit.Assert.*
import org.junit.Test

class PetSharedWeatherAdapterTest {
    @Test fun mapsEverySharedWeatherStatus() {
        val expected = listOf(PetWeatherCondition.Clear, PetWeatherCondition.Cloudy,
            PetWeatherCondition.Rain, PetWeatherCondition.Storm, PetWeatherCondition.Snow)
        WeatherStatus.entries.zip(expected).forEach { (status, condition) ->
            assertEquals(condition, PetSharedWeatherAdapter.fromCurrentWeather(CurrentWeather(status, 18.0), 1_000L).condition)
        }
    }

    @Test fun missingWindDoesNotDisableRainProtection() {
        val weather = PetSharedWeatherAdapter.fromCurrentWeather(CurrentWeather(WeatherStatus.RAINY, 18.0), System.currentTimeMillis())
        assertTrue(weather.windSpeedKmh.isNaN())
        assertTrue(weather.isCurrent)
        assertEquals(PetOutfit.Raincoat, PetStateEngine.resolveOutfit(weather, PetWardrobeChoice.Festival))
    }

    @Test fun absentDaylightAndWindDoNotForceThoseOutfits() {
        val sunny = PetSharedWeatherAdapter.fromCurrentWeather(CurrentWeather(WeatherStatus.SUNNY, 29.0), System.currentTimeMillis())
        assertFalse(sunny.daylightKnown)
        assertEquals(PetOutfit.Explorer, PetStateEngine.resolveOutfit(sunny, PetWardrobeChoice.Explorer))
        assertEquals(PetOutfit.Winter, PetStateEngine.resolveOutfit(sunny.copy(temperatureC = 5.0), PetWardrobeChoice.Explorer))
    }

    @Test fun incompleteOrInvalidWeatherCannotForceClothing() {
        listOf(CurrentWeather(null, 18.0), CurrentWeather(WeatherStatus.RAINY, null),
            CurrentWeather(WeatherStatus.RAINY, Double.NaN), CurrentWeather(WeatherStatus.RAINY, 100.0)).forEach {
            val weather = PetSharedWeatherAdapter.fromCurrentWeather(it, System.currentTimeMillis())
            assertFalse(weather.isCurrent)
            assertEquals(PetOutfit.Pajamas, PetStateEngine.resolveOutfit(weather, PetWardrobeChoice.Pajamas))
        }
    }

    @Test fun receiptTimeIsDistinctFromUnknownObservationTimeAndExpires() {
        val weather = PetSharedWeatherAdapter.fromCurrentWeather(CurrentWeather(WeatherStatus.RAINY, 18.0), 1_000_000L)
        assertEquals(0L, weather.observedAtMillis)
        assertEquals(1_000_000L, weather.receivedAtMillis)
        assertTrue(weather.isFreshAt(1_000_000L))
        assertFalse(weather.isFreshAt(999_999L))
        assertFalse(weather.isFreshAt(1_000_000L + 46 * 60_000L))
    }

    @Test fun replayCannotRenewAnOldReadingButANewResponseCan() {
        val state = PetSharedWeatherState()
        val response = CurrentWeather(WeatherStatus.RAINY, 18.0)
        val initial = state.accept(response, 1_000_000L)
        val replay = state.accept(response, 1_000_000L + 46 * 60_000L)
        assertSame(initial, replay)
        assertFalse(replay.isFreshAt(1_000_000L + 46 * 60_000L))
        val refreshed = state.accept(response.copy(), 1_000_000L + 46 * 60_000L)
        assertTrue(refreshed.isFreshAt(1_000_000L + 46 * 60_000L))
    }

    @Test fun invalidNewResponseReplacesPreviousRain() {
        val state = PetSharedWeatherState()
        state.accept(CurrentWeather(WeatherStatus.RAINY, 18.0), 1_000L)
        assertFalse(state.accept(CurrentWeather(null, null), 2_000L).isFreshAt(2_000L))
    }
}
